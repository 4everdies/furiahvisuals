package meowtils.furiahvisuals.module.trails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.search.SearchList;
import meowtils.furiahvisuals.gui.widget.ToggleWidget;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class TrailCardsWidget extends Widget implements SearchList {
   private static final float GAP = 6.0F;
   private static final float TILE_W = 126.0F;
   private static final float RADIUS = 7.0F;
   private static final int DIMMED = -6645086;
   private final TrailsModule module;
   private final TrailStyle[] values = TrailStyle.values();
   private final float[] hover = new float[this.values.length];
   private final float[] knob = new float[this.values.length];
   private final int[] cells = new int[this.values.length];
   private final List<TrailStyle> onScreen = new ArrayList<>();
   private final float[] uv = new float[4];
   private float selX = -1.0F;
   private float selY = -1.0F;
   private float combineShown = -1.0F;
   private int[] shown;
   private long refusedAt;

   TrailCardsWidget(TrailsModule module) {
      this.module = module;

      for (int i = 0; i < this.knob.length; i++) {
         this.knob[i] = -1.0F;
      }
   }

   private int columns() {
      if (Density.dense()) {
         return this.w >= 420.0F ? 4 : (this.w >= 280.0F ? 3 : 2);
      } else {
         float min = 126.0F * Density.cardScale();
         int most = Density.compact() ? 4 : 3;
         return Math.max(2, Math.min(most, (int)((this.w + 6.0F) / (min + 6.0F))));
      }
   }

   private float tileW() {
      int cols = this.columns();
      return (this.w - 6.0F * (cols - 1)) / cols;
   }

   private float tileH() {
      return Density.dense() ? Density.textCardH(44.0F) : Math.round(this.tileW() * (Density.compact() ? 0.6F : 0.66F));
   }

   private static float thumbH() {
      return Density.textCardH(44.0F) - 4.0F;
   }

   private static float thumbW() {
      return Math.round(thumbH() * 1.8F);
   }

   private static float switchW() {
      return Density.compact() ? 20.0F : 24.0F;
   }

   private static float switchH() {
      return Density.compact() ? 11.0F : 13.0F;
   }

   private int count() {
      return this.shown == null ? this.values.length : this.shown.length;
   }

   private int valueAt(int slot) {
      return this.shown == null ? slot : this.shown[slot];
   }

   private int slotOf(int value) {
      if (this.shown == null) {
         return value;
      } else {
         for (int i = 0; i < this.shown.length; i++) {
            if (this.shown[i] == value) {
               return i;
            }
         }

         return -1;
      }
   }

   @Override
   public float height() {
      int rows = (this.count() + this.columns() - 1) / this.columns();
      return rows == 0 ? 0.0F : rows * this.tileH() + (rows - 1) * 6.0F + 4.0F;
   }

   @Override
   public boolean isVisible() {
      return this.module.style.isVisible();
   }

   private float tileX(int slot) {
      return this.x + slot % this.columns() * (this.tileW() + 6.0F);
   }

   private float tileY(int slot) {
      return this.y + slot / this.columns() * (this.tileH() + 6.0F);
   }

   private float switchX(int slot) {
      return this.tileX(slot) + this.tileW() - (Density.dense() ? 6.0F : 7.0F) - switchW();
   }

   private float switchY(int slot) {
      return Density.dense() ? this.tileY(slot) + (this.tileH() - switchH()) / 2.0F : this.tileY(slot) + 7.0F;
   }

   private boolean onSwitch(int slot, float mx, float my) {
      if (!this.module.combine.get()) {
         return false;
      } else {
         float sx = this.switchX(slot);
         float sy = this.switchY(slot);
         return mx >= sx - 4.0F && mx < sx + switchW() + 4.0F && my >= sy - 4.0F && my < sy + switchH() + 4.0F;
      }
   }

   private boolean runs(TrailStyle t) {
      return this.module.combine.get() ? this.module.getStyle(t).on.get() : this.module.style.get() == t;
   }

   private static boolean isNew(TrailStyle t) {
      return t.isNew() && !MenuMemory.seen("card.trails." + t.name());
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      boolean dense = Density.dense();
      float tw = this.tileW();
      float th = this.tileH();
      boolean combine = this.module.combine.get();
      TrailStyle pickedStyle = this.module.style.get();
      if (pickedStyle.isNew()) {
         MenuMemory.markSeen("card.trails." + pickedStyle.name());
      }

      this.combineShown = this.combineShown < 0.0F ? (combine ? 1.0F : 0.0F) : Easing.approach(this.combineShown, combine ? 1.0F : 0.0F, 14.0F, dt);
      int selectedSlot = this.slotOf(this.module.style.getIndex());
      if (selectedSlot >= 0) {
         float targetX = this.tileX(selectedSlot);
         float targetY = this.tileY(selectedSlot) - this.y;
         this.selX = this.selX < 0.0F ? targetX : Easing.approach(this.selX, targetX, 18.0F, dt);
         this.selY = this.selY < 0.0F ? targetY : Easing.approach(this.selY, targetY, 18.0F, dt);
      }

      float iw = dense ? thumbW() : tw;
      float ih = dense ? thumbH() : th;
      this.onScreen.clear();

      for (int s = 0; s < this.count(); s++) {
         float ix = dense ? this.tileX(s) + 3.0F : this.tileX(s);
         float iy = dense ? this.tileY(s) + 2.0F : this.tileY(s);
         if (!Ui.clippedOut(ix, iy, iw, ih)) {
            this.onScreen.add(this.values[this.valueAt(s)]);
         }
      }

      int atlas = this.module.previews().drawCards(this.onScreen, Math.max(16, Math.round(iw * Ui.px)), Math.max(16, Math.round(ih * Ui.px)), this.cells);
      if (dense) {
         this.renderList(mx, my, dt, accent, selectedSlot, atlas);
      } else {
         this.renderTiles(mx, my, dt, accent, selectedSlot, atlas);
      }
   }

   private void renderTiles(float mx, float my, float dt, int accent, int selectedSlot, int atlas) {
      boolean compact = Density.compact();
      float tw = this.tileW();
      float th = this.tileH();
      if (selectedSlot >= 0) {
         Ui.glow(this.selX + tw / 2.0F, this.y + this.selY + th / 2.0F, tw * 1.35F, ColorUtil.withAlpha(accent, 0.24F));
      }

      int picked = this.module.style.getIndex();

      for (int s = 0; s < this.count(); s++) {
         int i = this.valueAt(s);
         TrailStyle t = this.values[i];
         TrailStyleBase st = this.module.getStyle(t);
         float tx = this.tileX(s);
         float ty = this.tileY(s);
         boolean over = mx >= tx && mx < tx + tw && my >= ty && my < ty + th;
         this.hover[i] = Easing.approach(this.hover[i], over ? 1.0F : 0.0F, 16.0F, dt);
         boolean runs = this.runs(t);
         this.knob[i] = this.knob[i] < 0.0F ? (st.on.get() ? 1.0F : 0.0F) : Easing.approach(this.knob[i], st.on.get() ? 1.0F : 0.0F, 18.0F, dt);
         int tint = this.module.combine.get() && !runs ? -6645086 : -1;
         if (atlas > 0 && this.cells[i] >= 0) {
            this.module.previews().cellUv(this.cells[i], this.uv);
            Ui.roundedImage(atlas, tx, ty, tw, th, 7.0F, this.uv[0], this.uv[1], this.uv[2], this.uv[3], tint);
         } else {
            Ui.gradientV(tx, ty, tw, th, 7.0F, -15723493, -15065305);
         }

         if (this.hover[i] > 0.01F) {
            Ui.roundRect(tx, ty, tw, th, 7.0F, ColorUtil.withAlpha(-1, 0.07F * this.hover[i]));
         }

         String tag = t.tag();
         float ts = compact ? 5.4F : 6.0F;
         float pw = Ui.widthBold(tag, ts) + 8.0F;
         float ph = compact ? 9.5F : 11.0F;
         Ui.roundRect(tx + 6.0F, ty + 6.0F, pw, ph, ph / 2.0F, -1728053248);
         Ui.textMiddle(tag, tx + 10.0F, ty + 6.0F + ph / 2.0F, ts, i == picked ? -1 : -788529153, true);
         if (isNew(t)) {
            float nw = Ui.widthBold("NEW", ts) + 8.0F;
            float nx = tx + 6.0F + pw + 4.0F;
            Ui.roundRect(nx, ty + 6.0F, nw, ph, ph / 2.0F, ColorUtil.withAlpha(accent, 0.92F));
            Ui.textMiddle("NEW", nx + 4.0F, ty + 6.0F + ph / 2.0F, ts, -1, true);
         }

         float shadeTop = ty + th * 0.48F;
         Ui.gradientV(tx, shadeTop, tw, ty + th - shadeTop, 7.0F, 0, -805306368);
         int dot = st.color.getDisplayColor() | 0xFF000000;
         float ns = compact ? 7.8F : 9.2F;
         float ds = 7.0F;
         float nameY = compact ? ty + th - ns - 5.0F : ty + th - ns - ds - 8.0F;
         float dx = tx + 9.0F;
         float dy = nameY + ns / 2.0F + 0.5F;
         if (runs) {
            Ui.glow(dx, dy, 14.0F, ColorUtil.withAlpha(dot, 0.55F));
         }

         Ui.circle(dx, dy, compact ? 2.4F : 2.8F, runs ? dot : ColorUtil.withAlpha(dot, 0.5F));
         int nameColor = i == picked ? -1 : (runs ? ColorUtil.lerp(-1907990, -1, this.hover[i]) : -4934466);
         Ui.textBold(Ui.ellipsize(t.displayName(), ns, tw - 24.0F, true), tx + 16.0F, nameY, ns, nameColor);
         if (!compact) {
            Ui.text(Ui.ellipsize(t.description(), ds, tw - 14.0F, false), tx + 7.0F, ty + th - ds - 5.0F, ds, i == picked ? -419430401 : -1325400065);
         }

         if (this.combineShown > 0.01F) {
            float sw = switchW();
            float sh = switchH();
            float sx = this.switchX(s);
            float sy = this.switchY(s);
            Ui.pushAlpha(this.combineShown);
            Ui.roundRect(sx - 3.0F, sy - 3.0F, sw + 6.0F, sh + 6.0F, (sh + 6.0F) / 2.0F, -1946157056);
            ToggleWidget.drawSwitch(sx, sy, sw, sh, this.knob[i], dot);
            Ui.popAlpha();
         }

         Ui.outline(tx, ty, tw, th, 7.0F, 0.8F, i == picked ? 0 : 620756991);
      }

      if (selectedSlot >= 0) {
         Ui.accentOutline(this.selX - 1.5F, this.y + this.selY - 1.5F, tw + 3.0F, th + 3.0F, 8.5F, 1.4F, accent);
      }
   }

   private void renderList(float mx, float my, float dt, int accent, int selectedSlot, int atlas) {
      float tw = this.tileW();
      float th = this.tileH();

      for (int s = 0; s < this.count(); s++) {
         int i = this.valueAt(s);
         float cx = this.tileX(s);
         float cy = this.tileY(s);
         boolean over = mx >= cx && mx < cx + tw && my >= cy && my < cy + th;
         this.hover[i] = Easing.approach(this.hover[i], over ? 1.0F : 0.0F, 16.0F, dt);
         Ui.surface(cx, cy, tw, th, 7.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hover[i]));
      }

      if (selectedSlot >= 0) {
         float hy = this.y + this.selY;
         Ui.roundRect(this.selX, hy, tw, th, 7.0F, Theme.ACCENT_SOFT);
         Ui.accentOutline(this.selX, hy, tw, th, 7.0F, 1.1F, accent);
      }

      int picked = this.module.style.getIndex();
      boolean combine = this.module.combine.get();
      float pw = thumbW();
      float ph = thumbH();

      for (int s = 0; s < this.count(); s++) {
         int i = this.valueAt(s);
         TrailStyle t = this.values[i];
         TrailStyleBase st = this.module.getStyle(t);
         float cx = this.tileX(s);
         float cy = this.tileY(s);
         boolean runs = this.runs(t);
         this.knob[i] = this.knob[i] < 0.0F ? (st.on.get() ? 1.0F : 0.0F) : Easing.approach(this.knob[i], st.on.get() ? 1.0F : 0.0F, 18.0F, dt);
         float px = cx + 3.0F;
         float py = cy + 2.0F;
         if (atlas > 0 && this.cells[i] >= 0) {
            this.module.previews().cellUv(this.cells[i], this.uv);
            Ui.roundedImage(atlas, px, py, pw, ph, 3.0F, this.uv[0], this.uv[1], this.uv[2], this.uv[3], combine && !runs ? -6645086 : -1);
         } else {
            Ui.roundRect(px, py, pw, ph, 3.0F, -15723493);
         }

         int dot = st.color.getDisplayColor() | 0xFF000000;
         float sw = switchW();
         float sh = switchH();
         float sx = this.switchX(s);
         float sy = this.switchY(s);
         float dx = px + pw + 6.0F;
         float dy = cy + th / 2.0F;
         Ui.circle(dx, dy, 2.4F, runs ? dot : ColorUtil.withAlpha(dot, 0.45F));
         float right = combine ? sx - 5.0F : cx + tw - 6.0F;
         float ts = 5.6F;
         float tagW = Ui.widthBold(t.tag(), ts);
         Ui.textMiddle(t.tag(), right - tagW, dy, ts, Theme.TEXT_MUTED, true);
         if (combine) {
            ToggleWidget.drawSwitch(sx, sy, sw, sh, this.knob[i], dot);
         }

         float textX = dx + 5.0F;
         float ns = 7.8F;
         float nts = 5.4F;
         float nw = Ui.widthBold("NEW", nts) + 6.0F;
         float nh = 9.0F;
         if (isNew(t) && right - tagW - nw - 5.0F - 6.0F - textX >= Ui.widthBold("Ww…", ns)) {
            float nx = right - tagW - 5.0F - nw;
            Ui.roundRect(nx, dy - nh / 2.0F, nw, nh, nh / 2.0F, ColorUtil.withAlpha(accent, 0.92F));
            Ui.textMiddle("NEW", nx + 3.0F, dy, nts, -1, true);
            tagW += nw + 5.0F;
         }

         float room;
         if ((room = right - tagW - 6.0F - textX) >= Ui.widthBold("…", ns)) {
            Ui.textBold(
               Ui.ellipsize(t.displayName(), ns, room, true),
               textX,
               dy - ns / 2.0F - 0.5F,
               ns,
               i == picked ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, 0.3F + 0.7F * this.hover[i])
            );
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         float tw = this.tileW();
         float th = this.tileH();

         for (int s = 0; s < this.count(); s++) {
            int i = this.valueAt(s);
            float cx = this.tileX(s);
            float cy = this.tileY(s);
            if (mx >= cx && mx < cx + tw && my >= cy && my < cy + th) {
               if (this.onSwitch(s, mx, my)) {
                  boolean was = this.module.getStyle(this.values[i]).on.get();
                  if (this.module.togglePill(this.values[i])) {
                     ModSounds.toggle(!was);
                  } else {
                     this.refusedAt = ClientClock.millis();
                     ModSounds.gui(Sfx.ERROR);
                  }
               } else {
                  this.module.style.setIndex(i);
                  ModSounds.gui(Sfx.SELECT);
               }

               return true;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else {
         float tw = this.tileW();
         float th = this.tileH();

         for (int s = 0; s < this.count(); s++) {
            int i = this.valueAt(s);
            TrailStyle t = this.values[i];
            float cx = this.tileX(s);
            float cy = this.tileY(s);
            if (mx >= cx && mx < cx + tw && my >= cy && my < cy + th) {
               if (this.onSwitch(s, mx, my)) {
                  boolean on = this.module.getStyle(t).on.get();
                  if (this.refusedAt != 0L && ClientClock.millis() - this.refusedAt < 2500L) {
                     return on ? "At least one style runs: switch another one on first" : "Up to 3 styles at once: switch one off first";
                  }

                  return on ? "Stop " + t.displayName() + " (the others go on)" : "Run " + t.displayName() + " with the others (up to " + 3 + " at once)";
               }

               String type = t.isPath() ? "Path: one shape along your path" : "Emitter: drops pieces along the way";
               return t.description() + ". " + type + ". Click for its settings.";
            }
         }

         return null;
      }
   }

   @Override
   public void searchItems(SearchList.Items out) {
   }

   @Override
   public void searchFilter(Set<Object> keys) {
      if (keys == null) {
         this.shown = null;
      } else {
         int n = 0;
         int[] idx = new int[this.values.length];

         for (int i = 0; i < this.values.length; i++) {
            if (keys.contains(this.values[i])) {
               idx[n++] = i;
            }
         }

         this.shown = Arrays.copyOf(idx, n);
      }
   }

   @Override
   public float reveal(Object key) {
      if (key instanceof TrailStyle) {
         int slot = this.slotOf(((TrailStyle)key).ordinal());
         return slot < 0 ? -1.0F : slot / this.columns() * (this.tileH() + 6.0F);
      } else {
         return -1.0F;
      }
   }

   @Override
   public float revealHeight(Object key) {
      return this.tileH();
   }

   @Override
   public int searchVersion() {
      return 0;
   }
}
