package meowtils.furiahvisuals.module.weapons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.search.SearchList;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class WeaponCardsWidget extends Widget implements SearchList {
   private static final float GAP = 6.0F;
   private static final float MIN_CARD_W = 126.0F;
   private static final float ASPECT = 0.66F;
   private static final float RADIUS = 7.0F;
   private static final float NAME_SIZE = 8.8F;
   private static final float SUB_SIZE = 6.6F;
   private static final float BADGE_SIZE = 5.6F;
   private static final long DOUBLE_CLICK_MS = 350L;
   private static final float IDLE_SPEED = 26.0F;
   private static final float HOVER_SPEED = 190.0F;
   private final WeaponsModule module;
   private final List<WeaponDef> shown = new ArrayList<>();
   private final List<WeaponViews.Card> cards = new ArrayList<>();
   private final List<WeaponViews.Card> pool = new ArrayList<>();
   private final Map<String, float[]> anim = new HashMap<>();
   private final float[] uv = new float[4];
   private Set<Object> searchKeys;
   private float selX = -1.0F;
   private float selY = -1.0F;
   private String lastClickId;
   private long lastClickAt;

   WeaponCardsWidget(WeaponsModule module) {
      this.module = module;
   }

   private List<WeaponDef> shown() {
      this.shown.clear();

      for (WeaponDef d : this.module.library.all()) {
         if (this.searchKeys != null) {
            if (this.searchKeys.contains(d.id)) {
               this.shown.add(d);
            }
         } else if (d.isVanilla() || this.module.category == null || d.category == this.module.category) {
            this.shown.add(d);
         }
      }

      return this.shown;
   }

   private int columns() {
      float min = 126.0F * Density.cardScale();
      int most = Density.dense() ? 6 : (Density.compact() ? 5 : 4);
      return Math.max(2, Math.min(most, (int)((this.w + 6.0F) / (min + 6.0F))));
   }

   private float nameSize() {
      return Density.dense() ? 7.0F : (Density.compact() ? 7.8F : 8.8F);
   }

   private boolean showsSub() {
      return !Density.dense();
   }

   private float cardW() {
      int cols = this.columns();
      return (this.w - 6.0F * (cols - 1)) / cols;
   }

   private float cardH() {
      return Math.round(this.cardW() * 0.66F);
   }

   private float cardX(int slot) {
      return this.x + slot % this.columns() * (this.cardW() + 6.0F);
   }

   private float cardY(int slot) {
      return this.y + slot / this.columns() * (this.cardH() + 6.0F);
   }

   private int slotAt(float mx, float my) {
      float cw = this.cardW();
      float ch = this.cardH();

      for (int s = 0; s < this.shown.size(); s++) {
         float cx = this.cardX(s);
         float cy = this.cardY(s);
         if (mx >= cx && mx < cx + cw && my >= cy && my < cy + ch) {
            return s;
         }
      }

      return -1;
   }

   private float[] equipButton(float cx, float cy) {
      float bw = Ui.width("Equip", 7.6F) + 16.0F;
      float bh = 15.0F;
      return new float[]{cx + this.cardW() - bw - 6.0F, cy + 6.0F, bw, bh};
   }

   private float[] styleTag(float cx, float cy, String tag) {
      float tw = badgeW(tag);
      float th = 10.0F;
      return new float[]{cx + this.cardW() - tw - 6.0F, cy + this.cardH() - th - 4.0F, tw, th};
   }

   private boolean overTag(WeaponDef d, float cx, float cy, float mx, float my) {
      if (d.isVanilla() || this.module.importStyles(d) != 0) {
         return false;
      } else if (d.id.equals(WeaponsModule.tagHoverForTest)) {
         return true;
      } else {
         float[] t = this.styleTag(cx, cy, "PX");
         return mx >= t[0] && mx < t[0] + t[2] && my >= t[1] && my < t[1] + t[3];
      }
   }

   private float[] anim(WeaponDef d) {
      float[] a = this.anim.get(d.id);
      if (a == null) {
         a = new float[]{24.0F, 0.0F, 0.0F};
         this.anim.put(d.id, a);
      }

      return a;
   }

   @Override
   public float height() {
      int n = this.shown().size();
      int rows = (n + this.columns() - 1) / this.columns();
      return rows == 0 ? 22.0F : rows * this.cardH() + (rows - 1) * 6.0F + 4.0F;
   }

   @Override
   public boolean isVisible() {
      return !this.module.editing();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      List<WeaponDef> list = this.shown();
      if (list.isEmpty()) {
         Ui.text("No weapon matches.", this.x, this.y + 4.0F, 8.4F, Theme.TEXT_MUTED);
      } else {
         int cols = this.columns();
         float cw = this.cardW();
         float ch = this.cardH();
         String selected = this.module.selected();
         String equipped = this.module.equippedId();
         int selSlot = -1;

         for (int s = 0; s < list.size(); s++) {
            if (list.get(s).id.equals(selected)) {
               selSlot = s;
            }
         }

         if (selSlot >= 0) {
            float tx = this.x + selSlot % cols * (cw + 6.0F);
            float ty = selSlot / cols * (ch + 6.0F);
            this.selX = this.selX < 0.0F ? tx : Easing.approach(this.selX, tx, 18.0F, dt);
            this.selY = this.selY < 0.0F ? ty : Easing.approach(this.selY, ty, 18.0F, dt);
         }

         for (WeaponViews.Card c : this.cards) {
            this.pool.add(c);
         }

         this.cards.clear();

         for (int sx = 0; sx < list.size(); sx++) {
            WeaponDef d = list.get(sx);
            float cx = this.cardX(sx);
            float cy = this.cardY(sx);
            boolean over = mx >= cx && mx < cx + cw && my >= cy && my < cy + ch;
            float[] a = this.anim(d);
            a[1] = Easing.approach(a[1], over ? 1.0F : 0.0F, 10.0F, dt);
            a[0] = (a[0] + dt * (26.0F + 164.0F * a[1])) % 360.0F;
            if (!Ui.clippedOut(cx, cy, cw, ch)) {
               WeaponViews.Card c = this.pool.isEmpty() ? new WeaponViews.Card() : this.pool.remove(this.pool.size() - 1);
               c.def = d;
               c.angle = a[0];
               c.style = this.overTag(d, cx, cy, mx, my) ? (this.module.pixelOf(d, WeaponOptions.StyleChoice.SECTION) ? 0 : 1) : -1;
               c.cell = -1;
               this.cards.add(c);
            }
         }

         int atlas = this.module.views().drawCards(this.cards, Math.round(cw * Ui.px), Math.round(ch * Ui.px));
         if (selSlot >= 0) {
            Ui.glow(this.selX + cw / 2.0F, this.y + this.selY + ch / 2.0F, cw * 1.35F, ColorUtil.withAlpha(accent, 0.22F));
         }

         for (int sxx = 0; sxx < list.size(); sxx++) {
            WeaponDef d = list.get(sxx);
            float cx = this.cardX(sxx);
            float cy;
            if (!Ui.clippedOut(cx, cy = this.cardY(sxx), cw, ch)) {
               float[] a = this.anim(d);
               boolean over = mx >= cx && mx < cx + cw && my >= cy && my < cy + ch;
               boolean isSelected = d.id.equals(selected);
               boolean isEquipped = d.id.equals(equipped);
               int cell = -1;

               for (WeaponViews.Card c : this.cards) {
                  if (c.def == d) {
                     cell = c.cell;
                  }
               }

               float ns = this.nameSize();
               float nameY = this.showsSub() ? cy + ch - ns - 6.6F - 11.0F : cy + ch - ns - 5.0F;
               if (atlas > 0 && cell >= 0) {
                  this.module.views().cellUv(cell, this.uv);
                  Ui.roundedImage(atlas, cx, cy, cw, ch, 7.0F, this.uv[0], this.uv[1], this.uv[2], this.uv[3], -1);
               } else {
                  Ui.gradientV(cx, cy, cw, ch, 7.0F, -13749699, -15461095);
                  float top = cy + 18.0F;
                  float bottom = d.isVanilla() ? nameY : Math.min(nameY, this.styleTag(cx, cy, "PX")[1] - 1.0F);
                  if (bottom - top >= 9.0F) {
                     String loading = Ui.ellipsize("Loading…", 7.4F, cw - 12.0F, false);
                     Ui.textMiddle(loading, cx + cw / 2.0F - Ui.width(loading, 7.4F) / 2.0F, top + (bottom - top) / 2.0F, 7.4F, -1711276033, false);
                  }
               }

               if (a[1] > 0.01F) {
                  Ui.roundRect(cx, cy, cw, ch, 7.0F, ColorUtil.withAlpha(-1, 0.06F * a[1]));
               }

               boolean blocked;
               if (blocked = this.module.bowBlocked(d.id)) {
                  Ui.roundRect(cx, cy, cw, ch, 7.0F, ColorUtil.withAlpha(Theme.PANEL, 0.62F));
               }

               float shadeTop = cy + ch * 0.5F;
               Ui.gradientV(cx, shadeTop, cw, cy + ch - shadeTop, 7.0F, 0, -805306368);
               String tag = d.isVanilla() ? null : this.module.styleTag(d);
               float textW = cw - 12.0F - (tag == null ? 0.0F : badgeW(tag) + 4.0F);
               int nameColor = blocked ? -6050627 : (isSelected ? -1 : ColorUtil.lerp(-1907990, -1, a[1]));
               Ui.textBold(Ui.ellipsize(d.name, ns, this.showsSub() ? cw - 12.0F : textW, true), cx + 7.0F, nameY, ns, nameColor);
               if (this.showsSub()) {
                  String sub = d.categoryName() + (d.isVanilla() ? " · your resource pack" : "");
                  Ui.text(Ui.ellipsize(sub, 6.6F, textW, false), cx + 7.0F, cy + ch - 6.6F - 6.0F, 6.6F, -1191182337);
               }

               if (tag != null) {
                  float[] t = this.styleTag(cx, cy, tag);
                  boolean onTag = this.overTag(d, cx, cy, mx, my);
                  badge(tag, t[0], t[1], onTag ? accent | 0xFF000000 : -1342177280, -1);
                  Ui.registerControl(t[0], t[1], t[2], t[3], tag);
               }

               float leftLimit = cx + cw - 6.0F;
               if (isEquipped) {
                  float pw = Ui.widthBold("EQUIPPED", 5.6F) + 8.0F;
                  badge("EQUIPPED", cx + cw - pw - 6.0F, cy + 6.0F, accent | 0xFF000000, -1);
                  leftLimit = cx + cw - pw - 9.0F;
               } else if (a[1] > 0.02F && !blocked) {
                  float[] b = this.equipButton(cx, cy);
                  boolean overButton = mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3];
                  a[2] = Easing.approach(a[2], overButton ? 1.0F : 0.0F, 16.0F, dt);
                  Ui.pushAlpha(a[1]);
                  Ui.registerControl(b[0], b[1], b[2], b[3], "Equip");
                  Ui.roundRect(b[0], b[1], b[2], b[3], b[3] / 2.0F, overButton ? Theme.ACCENT_HOVER : Theme.ACCENT);
                  Ui.textMiddle("Equip", b[0] + 8.0F, b[1] + b[3] / 2.0F, 7.6F, -1, true);
                  Ui.popAlpha();
                  leftLimit = b[0] - 3.0F;
               }

               float bx = cx + 6.0F;
               if (WeaponsModule.showsNew(d) && bx + badgeW("NEW") <= leftLimit) {
                  bx = badge("NEW", bx, cy + 6.0F, accent | 0xFF000000, -1) + 3.0F;
               }

               String hand = d.twoHanded ? "2H" : "1H";
               if (bx + badgeW(hand) <= leftLimit) {
                  bx = badge(hand, bx, cy + 6.0F, -1342177280, -1) + 3.0F;
                  if (d.dualWield && !d.twoHanded && bx + badgeW("DUAL") <= leftLimit) {
                     badge("DUAL", bx, cy + 6.0F, accent | 0xFF000000, -1);
                  }
               }

               if (this.module.settingsOf(d).isModified()) {
                  float nw = Math.min(cw - 20.0F, Ui.widthBold(d.name, ns));
                  Ui.circle(cx + 7.0F + nw + 6.0F, nameY + ns / 2.0F + 0.5F, 2.2F, accent | 0xFF000000);
               }

               Ui.outline(cx, cy, cw, ch, 7.0F, 0.8F, isSelected ? 0 : 620756991);
               if (over && isSelected) {
                  Ui.roundRect(cx, cy, cw, ch, 7.0F, 150994943);
               }
            }
         }

         if (selSlot >= 0) {
            Ui.accentOutline(this.selX - 1.5F, this.y + this.selY - 1.5F, cw + 3.0F, ch + 3.0F, 8.5F, 1.4F, accent);
         }
      }
   }

   private static float badgeW(String text) {
      return Ui.widthBold(text, 5.6F) + 8.0F;
   }

   private static float badge(String text, float bx, float by, int fill, int color) {
      float pw = Ui.widthBold(text, 5.6F) + 8.0F;
      Ui.roundRect(bx, by, pw, 10.0F, 5.0F, fill);
      Ui.textMiddle(text, bx + 4.0F, by + 5.0F, 5.6F, color, true);
      return bx + pw;
   }

   static float[] tagForColorCheck(float bx, float by, int accent) {
      badge("HD", bx, by, accent | 0xFF000000, -1);
      return new float[]{bx + 1.5F, by + 3.5F, 2.5F, 3.0F};
   }

   static float[] newForColorCheck(float bx, float by, int accent) {
      badge("NEW", bx, by, accent | 0xFF000000, -1);
      return new float[]{bx + 1.5F, by + 3.5F, 2.5F, 3.0F};
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         int s = this.slotAt(mx, my);
         if (s < 0) {
            return true;
         } else {
            WeaponDef d = this.shown.get(s);
            float[] b = this.equipButton(this.cardX(s), this.cardY(s));
            boolean onEquip = !d.id.equals(this.module.equippedId()) && mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3];
            long now = System.currentTimeMillis();
            boolean doubleClick = d.id.equals(this.lastClickId) && now - this.lastClickAt < 350L;
            this.lastClickId = d.id;
            this.lastClickAt = now;
            this.module.select(d.id);
            if (d.isNew) {
               MenuMemory.markSeen(WeaponsModule.seenKey(d));
            }

            if (!onEquip && !doubleClick) {
               ModSounds.gui(Sfx.SELECT);
            } else {
               ModSounds.gui(this.module.equip(d.id) ? Sfx.CONFIRM : Sfx.ERROR);
               this.lastClickId = null;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else {
         int s = this.slotAt(mx, my);
         if (s < 0) {
            return null;
         } else {
            WeaponDef d = this.shown.get(s);
            if (this.overTag(d, this.cardX(s), this.cardY(s), mx, my)) {
               return this.module.pixelOf(d, WeaponOptions.StyleChoice.SECTION)
                  ? "Pixelated here (its own Style or the section's Weapon Style): the picture shows it HD while you point at this"
                  : "HD here (its own Style or the section's Weapon Style): the picture shows it Pixelated while you point at this";
            } else if (this.module.bowBlocked(d.id)) {
               return "Bows only replace bows.";
            } else {
               StringBuilder t = new StringBuilder(d.description.isEmpty() ? d.name : d.description);
               if (d.isBow()) {
                  t.append(". A bow: for bow rules (All bows, the bow item, or a name)");
               } else if (d.twoHanded) {
                  t.append(". Two-handed");
               } else if (!d.dualWield) {
                  t.append(". Can't be dual-wielded");
               }

               if (d.id.equals(this.module.equippedId())) {
                  t.append(". Used by the selected rule (").append(this.module.selectedRuleLabel(d)).append(")");
               } else {
                  t.append(". Double-click or Equip: use it for the selected rule (").append(this.module.selectedRuleLabel(d)).append(")");
               }

               if (this.module.settingsOf(d).isModified()) {
                  t.append(". Its settings differ from its defaults");
               }

               return t.toString();
            }
         }
      }
   }

   @Override
   public void searchItems(SearchList.Items out) {
      for (WeaponDef d : this.module.library.all()) {
         ArrayList<String> words = new ArrayList<>();
         words.add(d.categoryName());
         words.addAll(d.tags);

         for (String f : d.finishes) {
            WeaponOptions.Finish finish = WeaponOptions.Finish.of(f);
            if (finish != null) {
               words.add(finish.displayName);
            }
         }

         words.add(d.twoHanded ? "two-handed" : "one-handed");
         words.add(d.twoHanded ? "2h" : "1h");
         if (d.dualWield && !d.twoHanded) {
            words.add("dual wield");
         }

         out.item(d.id, d.name, d.description, words.toArray(new String[0]));
      }
   }

   @Override
   public void searchFilter(Set<Object> keys) {
      this.searchKeys = keys;
   }

   @Override
   public float reveal(Object key) {
      List<WeaponDef> list = this.shown();

      for (int s = 0; s < list.size(); s++) {
         if (list.get(s).id.equals(key)) {
            this.module.select(list.get(s).id);
            return s / this.columns() * (this.cardH() + 6.0F);
         }
      }

      return -1.0F;
   }

   @Override
   public float revealHeight(Object key) {
      return this.cardH();
   }

   @Override
   public int searchVersion() {
      return this.module.library.version();
   }
}
