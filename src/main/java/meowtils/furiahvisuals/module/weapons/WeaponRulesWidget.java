package meowtils.furiahvisuals.module.weapons;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

final class WeaponRulesWidget extends Widget {
   private static final float TITLE = 26.0F;
   private static final float ROW = 30.0F;
   private static final float CELL = 21.0F;
   private static final int GRID_ROWS = 4;
   private static final int MAX = 32;
   private static final ItemStack SWORD = new ItemStack(Items.diamond_sword);
   private static final ItemStack AXE = new ItemStack(Items.diamond_axe);
   private static final ItemStack PICKAXE = new ItemStack(Items.diamond_pickaxe);
   private static final ItemStack BOW = new ItemStack(Items.bow);
   private static final ItemStack NAME_TAG = new ItemStack(Items.name_tag);
   private final WeaponsModule module;
   private final WeaponRulesSetting setting;
   private final Pill add = new Pill();
   private final float[] rowY = new float[32];
   private final float[] rowHover = new float[32];
   private int rows;
   private WeaponRule editing;
   private float editorY;
   private float editorH;
   private int dragging = -1;
   private String status;
   private long statusUntil;
   private final ChipRow kinds = new ChipRow();
   private final ChipRow groups = new ChipRow();
   private final ChipRow damage = new ChipRow();
   private final ChipRow finishes = new ChipRow();
   private static final String[] KIND_LABELS = new String[]{"Item Group", "Specific Item", "Name Contains"};
   private static final String[] GROUP_LABELS = new String[]{
      WeaponRule.Group.SWORDS.displayName, WeaponRule.Group.AXES.displayName, WeaponRule.Group.TOOLS.displayName, WeaponRule.Group.BOWS.displayName
   };
   private final boolean[] kindLit = new boolean[3];
   private final boolean[] groupLit = new boolean[GROUP_LABELS.length];
   private String[] damageLabels = new String[0];
   private String[] finishLabels = new String[0];
   private boolean[] damageLit = new boolean[0];
   private boolean[] finishLit = new boolean[0];
   private final List<String> finishIds = new ArrayList<>();
   private final WeaponRulesWidget.Field search = new WeaponRulesWidget.Field(40);
   private final WeaponRulesWidget.Field name = new WeaponRulesWidget.Field(48);
   private WeaponRulesWidget.Field focused;
   private List<ItemStack> everything;
   private final List<ItemStack> found = new ArrayList<>();
   private String foundFor;
   private float gridX;
   private float gridY;
   private float gridW;
   private int gridCols;
   private float gridScroll;
   private ItemStack gridHover;
   static String testEditor;
   private static final String BOW_DROPPED = "Bows only replace bows: this rule is back to Vanilla";

   WeaponRulesWidget(WeaponsModule module, WeaponRulesSetting setting) {
      this.module = module;
      this.setting = setting;
   }

   @Override
   public float height() {
      List<WeaponRule> rules = this.setting.get();
      if (this.editing != null && !rules.contains(this.editing)) {
         this.editing = null;
         this.focused = null;
      }

      float h = 26.0F + (rules.isEmpty() ? 20.0F : Math.min(32, rules.size()) * 30.0F);
      if (this.editing != null) {
         h += this.editorHeight() + 6.0F;
      }

      return h + 4.0F;
   }

   private float editorHeight() {
      float h = 36.0F;
      switch (this.editing.kind) {
         case GROUP:
            h += this.groups.height + 8.0F;
            break;
         case ITEM:
            h += Math.max(18.0F, this.damage.height - 1.0F) + 8.0F + 84.0F + 8.0F;
            break;
         default:
            h += 26.0F;
      }

      WeaponDef d = this.module.library.get(this.editing.weapon);
      if (d != null && !d.isVanilla()) {
         h += 16.0F + this.finishes.height + 8.0F;
      }

      return h;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible() && !this.module.editing();
   }

   private float removeX() {
      return this.x + this.w - 20.0F;
   }

   private float weaponW() {
      return Math.min(180.0F, this.w * 0.38F);
   }

   private float weaponX() {
      return this.removeX() - 6.0F - this.weaponW();
   }

   private float targetX() {
      return this.x + 42.0F;
   }

   private float targetEnd() {
      return this.weaponX() - 18.0F;
   }

   private void applyTestEditor() {
      String kind = testEditor;
      testEditor = null;
      WeaponRule r = this.setting.selectedRule();
      if (!kind.equals("close") && r != null) {
         if (kind.equals("item")) {
            this.setting.setItem(r, "minecraft:diamond_sword", -1);
            this.search.text = "sword";
            this.foundFor = null;
         } else if (kind.equals("name")) {
            this.setting.setName(r, "katana");
         } else if (r.kind != WeaponRule.Kind.GROUP) {
            this.setting.setGroup(r, WeaponRule.Group.SWORDS);
         }

         this.editing = r;
      } else {
         this.editing = null;
         this.unfocus();
      }
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      if (testEditor != null) {
         this.applyTestEditor();
      }

      WeaponsModule.itemsAsThemselves = true;

      try {
         this.draw(mx, my, dt, accent);
      } finally {
         WeaponsModule.itemsAsThemselves = false;
      }
   }

   private void draw(float mx, float my, float dt, int accent) {
      List<WeaponRule> rules = this.setting.get();
      Ui.textMiddle(this.setting.getName(), this.x, this.y + 11.0F, 10.0F, Theme.TEXT, false);
      float nw = Ui.width(this.setting.getName(), 10.0F);
      Ui.textMiddle("the first rule that matches wins · drag to reorder", this.x + nw + 8.0F, this.y + 11.0F, 7.4F, Theme.TEXT_MUTED, false);
      String label = "Add Rule";
      this.add.layout(this.x + this.w - (Ui.width(label, 8.8F) + 20.0F), this.y + 1.0F, label);
      this.add.draw(label, mx, my, dt, accent, true, rules.size() < 32);
      if (this.status != null && ClientClock.millis() < this.statusUntil) {
         float sw = Ui.widthHeavy(this.status, 7.8F);
         Ui.textMiddleHeavy(this.status, this.add.x - 10.0F - sw, this.y + 11.0F, 7.8F, accent | 0xFF000000);
      }

      float ry = this.y + 26.0F;
      this.rows = Math.min(32, rules.size());
      if (rules.isEmpty()) {
         Ui.text("No rules: every item looks as your resource pack draws it. Add Rule to replace some.", this.x, ry + 3.0F, 8.2F, Theme.TEXT_MUTED);
      }

      int selected = this.setting.selected();

      for (int i = 0; i < this.rows; i++) {
         WeaponRule r = rules.get(i);
         this.rowY[i] = ry;
         this.drawRow(r, i, i == selected, ry, mx, my, dt, accent);
         ry += 30.0F;
         if (r == this.editing) {
            this.editorY = ry;
            this.drawEditor(r, ry, mx, my, dt, accent);
            ry += this.editorHeight() + 6.0F;
         }
      }
   }

   private void drawRow(WeaponRule r, int i, boolean selected, float ry, float mx, float my, float dt, int accent) {
      float rh = 26.0F;
      boolean over = mx >= this.x && mx < this.x + this.w && my >= ry && my < ry + rh;
      this.rowHover[i] = Easing.approach(this.rowHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
      if (selected) {
         Ui.roundRect(this.x, ry, this.w, rh, 7.0F, ColorUtil.lerp(Theme.ACCENT_FAINT, Theme.ACCENT_SOFT, this.rowHover[i]));
         Ui.accentOutline(this.x, ry, this.w, rh, 7.0F, this.dragging == i ? 1.4F : 0.9F, accent);
      } else {
         Ui.surface(this.x, ry, this.w, rh, 7.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.rowHover[i]));
      }

      float cy = ry + rh / 2.0F;
      int dots = ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT_DIM, this.rowHover[i]);

      for (int k = 0; k < 6; k++) {
         Ui.circle(this.x + 8.0F + k % 2 * 3.6F, cy - 3.6F + k / 2 * 3.6F, 0.9F, dots);
      }

      Ui.item(this.targetIcon(r), this.x + 20.0F, cy - 8.0F, 16.0F);
      boolean overTarget = over && mx >= this.targetX() && mx < this.targetEnd();
      String target = r.targetLabel();
      if (r.kind == WeaponRule.Kind.NAME && r.text.trim().isEmpty()) {
         target = "Name contains… (type the text)";
      }

      Ui.textMiddle(
         Ui.ellipsize(target, 8.8F, this.targetEnd() - this.targetX() - 14.0F, false),
         this.targetX(),
         cy,
         8.8F,
         !overTarget && r != this.editing ? ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, 0.6F) : Theme.TEXT,
         false
      );
      Ui.textMiddle(
         r == this.editing ? "↓" : "›", this.targetEnd() - 8.0F, cy - 0.5F, 9.0F, r != this.editing && !overTarget ? Theme.TEXT_MUTED : accent, false
      );
      Ui.textMiddle("→", this.weaponX() - 13.0F, cy, 8.8F, Theme.TEXT_MUTED, false);
      WeaponDef d = this.module.library.get(r.weapon);
      float wx = this.weaponX();
      if (d == null) {
         Ui.textMiddle(Ui.ellipsize("Missing: " + r.weapon + " (Vanilla)", 8.4F, this.weaponW(), false), wx, cy, 8.4F, Theme.DANGER, false);
      } else {
         if (d.isVanilla()) {
            Ui.item(this.targetIcon(r), wx, cy - 8.0F, 16.0F);
         } else {
            float[] icon = this.module.iconOf(d);
            if (icon != null) {
               Ui.image((int)icon[0], wx - 1.0F, cy - 9.0F, 18.0F, 18.0F, icon[1], icon[2], icon[3], icon[4], -1);
            } else {
               Ui.circle(wx + 8.0F, cy, 6.0F, Theme.PILL);
            }
         }

         String wn = d.name;
         WeaponOptions.Finish f;
         if (r.finish != null && (f = WeaponOptions.Finish.of(r.finish)) != null) {
            wn = wn + " · " + f.displayName;
         }

         Ui.textMiddle(Ui.ellipsize(wn, 8.8F, this.weaponW() - 22.0F, true), wx + 21.0F, cy, 8.8F, Theme.TEXT, true);
      }

      boolean overRemove = over && mx >= this.removeX();
      Ui.textMiddle("×", this.removeX() + 6.0F, cy - 0.5F, 9.4F, overRemove ? Theme.DANGER : Theme.TEXT_MUTED, false);
   }

   private ItemStack targetIcon(WeaponRule r) {
      switch (r.kind) {
         case GROUP:
            return r.group == WeaponRule.Group.SWORDS ? SWORD : (r.group == WeaponRule.Group.AXES ? AXE : (r.group == WeaponRule.Group.BOWS ? BOW : PICKAXE));
         case ITEM:
            ItemStack s = r.stack();
            return s != null ? s : NAME_TAG;
         default:
            return NAME_TAG;
      }
   }

   private void drawEditor(WeaponRule r, float ey, float mx, float my, float dt, int accent) {
      float ex = this.x + 10.0F;
      float ew = this.w - 20.0F;
      Ui.roundRect(this.x + 4.0F, ey - 2.0F, this.w - 8.0F, this.editorHeight(), 7.0F, ColorUtil.mulAlpha(Theme.SURFACE, 0.7F));
      Ui.outline(this.x + 4.0F, ey - 2.0F, this.w - 8.0F, this.editorHeight(), 7.0F, 0.8F, Theme.OUTLINE);
      float cy = ey + 8.0F;
      this.kindLit[0] = r.kind == WeaponRule.Kind.GROUP;
      this.kindLit[1] = r.kind == WeaponRule.Kind.ITEM;
      this.kindLit[2] = r.kind == WeaponRule.Kind.NAME;
      this.kinds.layout(ex, cy, ew, KIND_LABELS, null);
      this.kinds.draw(KIND_LABELS, this.kindLit, null, null, mx, my, dt, accent);
      cy += this.kinds.height + 8.0F;
      switch (r.kind) {
         case GROUP:
            for (int i = 0; i < GROUP_LABELS.length; i++) {
               this.groupLit[i] = r.group == WeaponRule.Group.values()[i];
            }

            this.groups.layout(ex, cy, ew, GROUP_LABELS, null);
            this.groups.draw(GROUP_LABELS, this.groupLit, null, null, mx, my, dt, accent);
            cy += this.groups.height + 8.0F;
            break;
         case ITEM:
            float fieldW = Math.min(220.0F, ew * 0.45F);
            this.search.layout(ex, cy, fieldW);
            this.search.draw("Search items…", mx, my, dt, accent);
            this.layoutDamage(r, ex + fieldW + 8.0F, cy - 1.0F, ew - fieldW - 8.0F);
            this.damage.draw(this.damageLabels, this.damageLit, null, null, mx, my, dt, accent);
            float var17;
            this.drawGrid(r, ex, var17 = cy + (Math.max(18.0F, this.damage.height - 1.0F) + 8.0F), ew, mx, my, accent);
            cy = var17 + 92.0F;
            break;
         default:
            this.name.layout(ex, cy, Math.min(300.0F, ew));
            if (!this.name.focused) {
               this.name.text = r.text;
            }

            this.name.draw("Text in the item's name (e.g. katana)", mx, my, dt, accent);
            Ui.textMiddle("case and color codes don't matter", ex + Math.min(300.0F, ew) + 8.0F, cy + 9.0F, 7.2F, Theme.TEXT_MUTED, false);
            cy += 26.0F;
      }

      WeaponDef d = this.module.library.get(r.weapon);
      if (d != null && !d.isVanilla()) {
         Ui.text("Finish for this rule", ex, cy + 2.0F, 7.8F, Theme.TEXT_DIM);
         cy += 16.0F;
         this.finishIds.clear();
         ArrayList<String> labels = new ArrayList<>();
         this.finishIds.add(null);
         labels.add("The Weapon's Own");

         for (String f : d.finishes) {
            WeaponOptions.Finish fin = WeaponOptions.Finish.of(f);
            if (fin != null && fin != WeaponOptions.Finish.CUSTOM) {
               this.finishIds.add(fin.name().toLowerCase(Locale.ROOT));
               labels.add(fin.displayName);
            }
         }

         this.finishLabels = labels.toArray(new String[0]);
         this.finishLit = new boolean[this.finishLabels.length];

         for (int i = 0; i < this.finishIds.size(); i++) {
            String id = this.finishIds.get(i);
            this.finishLit[i] = id == null ? r.finish == null : id.equalsIgnoreCase(r.finish);
         }

         this.finishes.layout(ex, cy, ew, this.finishLabels, null);
         this.finishes.draw(this.finishLabels, this.finishLit, null, null, mx, my, dt, accent);
      }
   }

   private void layoutDamage(WeaponRule r, float dx, float dy, float dw) {
      if (r.damage >= 0) {
         this.damageLabels = new String[]{"Any Damage", "Only Damage " + r.damage, "−", "+"};
         this.damageLit = new boolean[]{false, true, false, false};
      } else {
         this.damageLabels = new String[]{"Any Damage", "Only One Damage"};
         this.damageLit = new boolean[]{true, false};
      }

      this.damage.layout(dx, dy, dw, this.damageLabels, null);
   }

   private List<ItemStack> everything() {
      if (this.everything == null) {
         this.everything = new ArrayList<>();

         for (Item item : Item.itemRegistry) {
            ArrayList<ItemStack> subs = new ArrayList<>();

            try {
               item.getSubItems(item, null, subs);
            } catch (RuntimeException var5) {
               subs.clear();
            }

            if (subs.isEmpty()) {
               subs.add(new ItemStack(item));
            }

            this.everything.addAll(subs);
         }
      }

      return this.everything;
   }

   private void filter() {
      String q = this.search.text.trim().toLowerCase(Locale.ROOT);
      if (!q.equals(this.foundFor)) {
         this.foundFor = q;
         this.found.clear();
         this.gridScroll = 0.0F;

         for (ItemStack s : this.everything()) {
            if (q.isEmpty()) {
               this.found.add(s);
            } else {
               String n;
               try {
                  n = EnumChatFormatting.getTextWithoutFormattingCodes(s.getDisplayName());
               } catch (RuntimeException var6) {
                  n = null;
               }

               Object id = Item.itemRegistry.getNameForObject(s.getItem());
               if (n != null && n.toLowerCase(Locale.ROOT).contains(q) || id != null && id.toString().contains(q)) {
                  this.found.add(s);
               }
            }
         }
      }
   }

   private void drawGrid(WeaponRule r, float gx, float gy, float gw, float mx, float my, int accent) {
      this.filter();
      this.gridX = gx;
      this.gridY = gy;
      this.gridW = gw;
      this.gridCols = Math.max(4, (int)(gw / 21.0F));
      int total = (this.found.size() + this.gridCols - 1) / this.gridCols;
      this.gridScroll = Math.max(0.0F, Math.min(this.gridScroll, (float)Math.max(0, total - 4)));
      int first = (int)this.gridScroll;
      float gh = 84.0F;
      Ui.roundRect(gx - 2.0F, gy - 2.0F, this.gridCols * 21.0F + 4.0F, gh + 4.0F, 5.0F, Theme.SURFACE);
      this.gridHover = null;
      if (this.found.isEmpty()) {
         Ui.text("No item matches.", gx + 4.0F, gy + 4.0F, 8.2F, Theme.TEXT_MUTED);
      } else {
         ItemStack current = r.kind == WeaponRule.Kind.ITEM ? r.stack() : null;

         int i;
         for (int row = 0; row < 4; row++) {
            for (int col = 0; col < this.gridCols && (i = (first + row) * this.gridCols + col) < this.found.size(); col++) {
               ItemStack s = this.found.get(i);
               float cx = gx + col * 21.0F;
               float cy = gy + row * 21.0F;
               boolean over = mx >= cx && mx < cx + 21.0F && my >= cy && my < cy + 21.0F;
               boolean isCurrent = current != null && current.getItem() == s.getItem() && (r.damage < 0 || r.damage == s.getItemDamage());
               if (isCurrent) {
                  Ui.roundRect(cx, cy, 20.0F, 20.0F, 4.0F, Theme.ACCENT_SOFT);
                  Ui.accentOutline(cx, cy, 20.0F, 20.0F, 4.0F, 0.9F, accent);
               } else if (over) {
                  Ui.roundRect(cx, cy, 20.0F, 20.0F, 4.0F, Theme.SURFACE_HOVER);
               }

               if (over) {
                  this.gridHover = s;
               }

               Ui.item(s, cx + 2.0F, cy + 2.0F, 16.0F);
            }
         }

         if (total > 4) {
            float bx = gx + this.gridCols * 21.0F + 5.0F;
            float frac = 4.0F / total;
            float by = gy + gh * (this.gridScroll / total);
            Ui.roundRect(bx, gy, 2.5F, gh, 1.25F, Theme.TRACK);
            Ui.roundRect(bx, by, 2.5F, Math.max(8.0F, gh * frac), 1.25F, Theme.ACCENT_LINE);
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         if (this.focused != null) {
            this.unfocus();
         }

         return false;
      } else {
         List<WeaponRule> rules = this.setting.get();
         if (button == 0 && this.add.contains(mx, my)) {
            this.addRule();
            return true;
         } else {
            for (int i = 0; i < this.rows && i < rules.size(); i++) {
               if (!(my < this.rowY[i]) && !(my >= this.rowY[i] + 30.0F - 4.0F)) {
                  WeaponRule r = rules.get(i);
                  this.unfocus();
                  if (button == 1) {
                     return true;
                  }

                  if (mx >= this.removeX()) {
                     this.setting.remove(i);
                     if (r == this.editing) {
                        this.editing = null;
                     }

                     ModSounds.gui(Sfx.CLICK);
                     return true;
                  }

                  this.setting.select(i);
                  if (mx < this.x + 18.0F) {
                     this.dragging = i;
                     ModSounds.gui(Sfx.SELECT);
                     return true;
                  }

                  if (mx >= this.targetX() && mx < this.targetEnd()) {
                     this.editing = this.editing == r ? null : r;
                     this.search.text = "";
                     this.foundFor = null;
                     ModSounds.gui(Sfx.DROPDOWN);
                     return true;
                  }

                  if (mx >= this.weaponX() - 18.0F && this.module.library.get(r.weapon) != null) {
                     this.module.select(r.weapon);
                  }

                  if (this.editing != null && this.editing != r) {
                     this.editing = null;
                  }

                  ModSounds.gui(Sfx.SELECT);
                  return true;
               }
            }

            if (this.editing != null && my >= this.editorY && my < this.editorY + this.editorHeight()) {
               this.editorDown(this.editing, mx, my, button);
               return true;
            } else {
               this.unfocus();
               return true;
            }
         }
      }
   }

   private void editorDown(WeaponRule r, float mx, float my, int button) {
      if (button == 0) {
         int k = this.kinds.at(mx, my);
         if (k >= 0) {
            this.unfocus();
            if (k == 0 && r.kind != WeaponRule.Kind.GROUP) {
               if (this.setting.setGroup(r, r.group)) {
                  this.say("Bows only replace bows: this rule is back to Vanilla");
               }
            } else if (k == 1 && r.kind != WeaponRule.Kind.ITEM) {
               ItemStack held = Minecraft.getMinecraft().thePlayer == null ? null : Minecraft.getMinecraft().thePlayer.getHeldItem();
               ItemStack pick = held != null ? held : SWORD;
               Object id = Item.itemRegistry.getNameForObject(pick.getItem());
               if (this.setting.setItem(r, id == null ? "minecraft:diamond_sword" : id.toString(), -1)) {
                  this.say("Bows only replace bows: this rule is back to Vanilla");
               }
            } else if (k == 2 && r.kind != WeaponRule.Kind.NAME) {
               this.setting.setName(r, r.text == null ? "" : r.text);
               this.focus(this.name);
               this.name.text = r.text;
            }

            ModSounds.gui(Sfx.SELECT);
         } else {
            int f = this.finishes.at(mx, my);
            if (f >= 0 && f < this.finishIds.size() && this.module.library.get(r.weapon) != null && !this.module.library.get(r.weapon).isVanilla()) {
               this.setting.setFinish(r, this.finishIds.get(f));
               ModSounds.gui(Sfx.SELECT);
            } else {
               switch (r.kind) {
                  case GROUP:
                     int g = this.groups.at(mx, my);
                     if (g >= 0) {
                        if (this.setting.setGroup(r, WeaponRule.Group.values()[g])) {
                           this.say("Bows only replace bows: this rule is back to Vanilla");
                        }

                        ModSounds.gui(Sfx.SELECT);
                     }

                     return;
                  case ITEM:
                     if (this.search.contains(mx, my)) {
                        this.focus(this.search);
                        return;
                     } else {
                        int d = this.damage.at(mx, my);
                        if (d < 0) {
                           if (this.gridHover != null
                              && mx >= this.gridX
                              && mx < this.gridX + this.gridCols * 21.0F
                              && my >= this.gridY
                              && my < this.gridY + 84.0F) {
                              Object id = Item.itemRegistry.getNameForObject(this.gridHover.getItem());
                              if (id != null) {
                                 int value = this.gridHover.getHasSubtypes() ? this.gridHover.getItemDamage() : -1;
                                 if (this.setting.setItem(r, id.toString(), value)) {
                                    this.say("Bows only replace bows: this rule is back to Vanilla");
                                 }

                                 ModSounds.gui(Sfx.SELECT);
                              }

                              return;
                           } else {
                              this.unfocus();
                              return;
                           }
                        } else {
                           ItemStack s = r.stack();
                           int value = r.damage;
                           if (d == 0) {
                              value = -1;
                           } else if (d == 1) {
                              value = r.damage >= 0 ? r.damage : (s != null ? s.getItemDamage() : 0);
                           } else if (d == 2 && r.damage > 0) {
                              value = r.damage - 1;
                           } else if (d == 3 && r.damage >= 0) {
                              value = Math.min(32767, r.damage + 1);
                           }

                           this.setting.setItem(r, r.item, value);
                           ModSounds.gui(Sfx.CLICK);
                           return;
                        }
                     }
                  default:
                     if (this.name.contains(mx, my)) {
                        this.focus(this.name);
                        this.name.text = r.text;
                     } else {
                        this.unfocus();
                     }
               }
            }
         }
      }
   }

   private void focus(WeaponRulesWidget.Field f) {
      if (this.focused != f) {
         ModSounds.gui(Sfx.CLICK);
      }

      this.focused = f;
      this.search.focused = f == this.search;
      this.name.focused = f == this.name;
      f.caret = 0.0F;
      f.all = false;
   }

   private void addRule() {
      if (this.setting.get().size() >= 32) {
         ModSounds.gui(Sfx.ERROR);
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         ItemStack held = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
         WeaponRule r;
         if (held != null && this.setting.match(held) == null) {
            Object id = Item.itemRegistry.getNameForObject(held.getItem());
            r = WeaponRule.item(id == null ? "minecraft:diamond_sword" : id.toString(), held.getHasSubtypes() ? held.getItemDamage() : -1, "vanilla");
            this.say("Added " + EnumChatFormatting.getTextWithoutFormattingCodes(held.getDisplayName()) + ": now Equip a weapon for it");
         } else {
            r = WeaponRule.name("", "vanilla");
            this.say("Added a rule: pick what it matches, then Equip a weapon");
         }

         this.setting.add(r);
         this.editing = r;
         this.search.text = "";
         this.foundFor = null;
         if (r.kind == WeaponRule.Kind.NAME) {
            this.name.text = "";
            this.focus(this.name);
         }

         ModSounds.gui(Sfx.CONFIRM);
      }
   }

   private void say(String message) {
      this.status = message;
      this.statusUntil = ClientClock.millis() + 3500L;
   }

   @Override
   public void mouseDrag(float mx, float my) {
      if (this.dragging >= 0) {
         List<WeaponRule> rules = this.setting.get();
         int to = this.dragging;

         for (int i = 0; i < this.rows && i < rules.size(); i++) {
            if (my >= this.rowY[i] && my < this.rowY[i] + 30.0F) {
               to = i;
            }
         }

         if (my < this.y + 26.0F) {
            to = 0;
         }

         if (to != this.dragging) {
            this.setting.move(this.dragging, to);
            this.dragging = to;
            ModSounds.gui(Sfx.SLIDER_TICK);
         }
      }
   }

   @Override
   public void mouseUp(float mx, float my) {
      this.dragging = -1;
   }

   @Override
   public boolean mouseWheel(float mx, float my, int wheel) {
      if (this.editing != null
         && this.editing.kind == WeaponRule.Kind.ITEM
         && mx >= this.gridX
         && mx < this.gridX + this.gridW
         && my >= this.gridY
         && my < this.gridY + 84.0F) {
         this.gridScroll -= wheel / 120.0F;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyTyped(char c, int key) {
      if (this.focused == null || this.editing == null) {
         return false;
      } else if (key != 1 && key != 28 && key != 156) {
         if (this.focused.key(c, key) && this.focused == this.name && this.editing.kind == WeaponRule.Kind.NAME) {
            this.setting.setText(this.editing, this.name.text);
         }

         return true;
      } else {
         this.unfocus();
         return true;
      }
   }

   @Override
   public boolean isFocused() {
      return this.focused != null;
   }

   @Override
   public void unfocus() {
      this.focused = null;
      this.search.focused = false;
      this.name.focused = false;
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else if (this.add.contains(mx, my)) {
         return "A new rule: for the item you hold if no rule covers it, else one to fill in";
      } else {
         List<WeaponRule> rules = this.setting.get();

         for (int i = 0; i < this.rows && i < rules.size(); i++) {
            if (!(my < this.rowY[i]) && !(my >= this.rowY[i] + 30.0F - 4.0F)) {
               if (mx >= this.removeX()) {
                  return "Remove this rule";
               }

               if (mx < this.x + 18.0F) {
                  return "Drag to reorder: the first rule that matches wins";
               }

               if (mx >= this.targetX() && mx < this.targetEnd()) {
                  return "What it matches: click to change it";
               }

               if (mx >= this.weaponX() - 18.0F) {
                  return "The weapon it turns into: select a rule, then Equip a weapon card (click here to see this one's card)";
               }

               return i == this.setting.selected() ? "The selected rule: a card's Equip changes its weapon" : "Select this rule";
            }
         }

         if (this.editing != null && this.gridHover != null) {
            Object id = Item.itemRegistry.getNameForObject(this.gridHover.getItem());
            return EnumChatFormatting.getTextWithoutFormattingCodes(this.gridHover.getDisplayName())
               + "  ("
               + id
               + (this.gridHover.getHasSubtypes() ? ", damage " + this.gridHover.getItemDamage() : "")
               + ")";
         } else {
            return this.editing != null && this.editing.kind == WeaponRule.Kind.ITEM && this.damage.at(mx, my) >= 0
               ? "Any damage value, or only one (colors of wool, potion kinds, custom items)"
               : null;
         }
      }
   }

   private static final class Field {
      static final float H = 18.0F;
      final int max;
      String text = "";
      boolean focused;
      boolean all;
      float caret;
      float fx;
      float fy;
      float fw;
      float focus;

      Field(int max) {
         this.max = max;
      }

      void layout(float x, float y, float w) {
         this.fx = x;
         this.fy = y;
         this.fw = w;
      }

      boolean contains(float mx, float my) {
         return mx >= this.fx && mx < this.fx + this.fw && my >= this.fy && my < this.fy + 18.0F;
      }

      void draw(String placeholder, float mx, float my, float dt, int accent) {
         this.caret += dt;
         this.focus = Easing.approach(this.focus, this.focused ? 1.0F : 0.0F, 16.0F, dt);
         Ui.surface(
            this.fx,
            this.fy,
            this.fw,
            18.0F,
            6.0F,
            ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, Math.max(this.focus, this.contains(mx, my) ? 0.6F : 0.0F))
         );
         if (this.focus > 0.99F) {
            Ui.accentOutline(this.fx, this.fy, this.fw, 18.0F, 6.0F, 0.9F, accent);
         } else {
            Ui.outline(this.fx, this.fy, this.fw, 18.0F, 6.0F, 0.9F, ColorUtil.lerp(Theme.OUTLINE, accent | 0xFF000000, this.focus));
         }

         if (this.text.isEmpty() && !this.focused) {
            Ui.textMiddle(Ui.ellipsize(placeholder, 8.4F, this.fw - 14.0F, false), this.fx + 7.0F, this.fy + 9.0F, 8.4F, Theme.TEXT_MUTED, false);
         } else {
            String shown = this.text;

            while (!shown.isEmpty() && Ui.width(shown, 8.8F) > this.fw - 16.0F) {
               shown = shown.substring(1);
            }

            if (this.all && this.focused && !this.text.isEmpty()) {
               Ui.roundRect(this.fx + 6.0F, this.fy + 3.5F, Ui.width(shown, 8.8F) + 2.0F, 11.0F, 2.0F, Theme.ACCENT_SELECTION);
            }

            Ui.textMiddle(shown, this.fx + 7.0F, this.fy + 9.0F, 8.8F, Theme.TEXT, false);
            if (this.focused && !this.all && this.caret % 1.0F < 0.55F) {
               Ui.rect(this.fx + 7.0F + Ui.width(shown, 8.8F) + 1.0F, this.fy + 4.0F, 0.8F, 10.0F, Theme.TEXT);
            }
         }
      }

      boolean key(char c, int key) {
         this.caret = 0.0F;
         if (GuiScreen.isKeyComboCtrlA(key)) {
            this.all = !this.text.isEmpty();
            return false;
         } else if (key == 14) {
            this.text = !this.all && !this.text.isEmpty() ? this.text.substring(0, this.text.length() - 1) : "";
            this.all = false;
            ModSounds.key();
            return true;
         } else {
            String add;
            if (GuiScreen.isKeyComboCtrlV(key)) {
               add = GuiScreen.getClipboardString();
            } else {
               if (c < ' ' || c == 127 || c == 167) {
                  return false;
               }

               add = String.valueOf(c);
            }

            add = add.replace("\n", " ").replace("\r", "").replace("§", "");
            String next = (this.all ? "" : this.text) + add;
            this.text = next.length() > this.max ? next.substring(0, this.max) : next;
            this.all = false;
            ModSounds.key();
            return true;
         }
      }
   }
}
