package meowtils.furiahvisuals.module.weapons;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

final class WheelSlotsWidget extends Widget {
   private static final float TITLE = 24.0F;
   private static final int MAX = 48;
   private static final ItemStack SWORD = new ItemStack(Items.diamond_sword);
   private final WeaponsModule module;
   private final WheelSlotsSetting setting;
   private final Pill add = new Pill();
   private final float[] rowY = new float[48];
   private final float[] rowHover = new float[48];
   private int rows;
   private int editing = -1;
   private float editorY;
   private int dragging = -1;
   private final ChipRow weapons = new ChipRow();
   private final ChipRow finishes = new ChipRow();
   private final List<String> weaponIds = new ArrayList<>();
   private final List<String> finishIds = new ArrayList<>();
   private String[] weaponLabels = new String[0];
   private String[] finishLabels = new String[0];
   private boolean[] weaponLit = new boolean[0];
   private boolean[] finishLit = new boolean[0];

   WheelSlotsWidget(WeaponsModule module, WheelSlotsSetting setting) {
      this.module = module;
      this.setting = setting;
   }

   private static float row() {
      return Density.compact() ? 22.0F : 28.0F;
   }

   private static float pageLine() {
      return 14.0F;
   }

   private boolean pageStarts(int index) {
      int per = this.module.wheel.perPage();
      return index > 0 && index % per == 0;
   }

   @Override
   public float height() {
      List<WheelSlotsSetting.Slot> slots = this.setting.get();
      if (this.editing >= slots.size()) {
         this.editing = -1;
      }

      float h = 24.0F + (slots.isEmpty() ? 20.0F : 0.0F);

      for (int i = 0; i < Math.min(48, slots.size()); i++) {
         h += row() + (this.pageStarts(i) ? pageLine() : 0.0F);
         if (i == this.editing) {
            h += this.editorHeight() + 6.0F;
         }
      }

      return h + 4.0F;
   }

   private float editorHeight() {
      this.layoutEditor(this.x + 10.0F, 0.0F, this.w - 20.0F);
      return 22.0F + this.weapons.height + 8.0F + (this.finishLabels.length > 0 ? 14.0F + this.finishes.height + 8.0F : 0.0F);
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private float removeX() {
      return this.x + this.w - 18.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      WeaponsModule.itemsAsThemselves = true;

      try {
         this.draw(mx, my, dt, accent);
      } finally {
         WeaponsModule.itemsAsThemselves = false;
      }
   }

   private void draw(float mx, float my, float dt, int accent) {
      List<WheelSlotsSetting.Slot> slots = this.setting.get();
      float ts = Density.text();
      Ui.textMiddle(this.setting.getLabel(), this.x, this.y + 10.0F, ts, Theme.TEXT, false);
      float nw = Ui.width(this.setting.getLabel(), ts);
      String label = "Add";
      this.add.layout(this.x + this.w - (Ui.width(label, 8.8F) + 20.0F), this.y, label);
      this.add.draw(label, mx, my, dt, accent, true, slots.size() < 48);
      String hint = slots.size() + " · clockwise from the top · drag to reorder";
      float room = this.add.x - this.x - nw - 18.0F;
      if (room > 40.0F) {
         Ui.textMiddle(Ui.ellipsize(hint, Density.small(), room, false), this.x + nw + 8.0F, this.y + 10.0F, Density.small(), Theme.TEXT_MUTED, false);
      }

      float ry = this.y + 24.0F;
      this.rows = Math.min(48, slots.size());
      if (slots.isEmpty()) {
         Ui.text("No slots: Add puts weapons on the wheel.", this.x, ry + 3.0F, 8.2F, Theme.TEXT_MUTED);
      }

      for (int i = 0; i < this.rows; i++) {
         if (this.pageStarts(i)) {
            int page = i / this.module.wheel.perPage() + 1;
            String p = "Page " + page;
            float pw = Ui.width(p, 7.2F);
            Ui.rect(this.x, ry + pageLine() / 2.0F, this.w / 2.0F - pw / 2.0F - 6.0F, 0.8F, Theme.OUTLINE);
            Ui.textMiddle(p, this.x + this.w / 2.0F - pw / 2.0F, ry + pageLine() / 2.0F, 7.2F, Theme.TEXT_MUTED, false);
            Ui.rect(this.x + this.w / 2.0F + pw / 2.0F + 6.0F, ry + pageLine() / 2.0F, this.w / 2.0F - pw / 2.0F - 6.0F, 0.8F, Theme.OUTLINE);
            ry += pageLine();
         }

         this.rowY[i] = ry;
         this.drawRow(slots.get(i), i, ry, mx, my, dt, accent);
         ry += row();
         if (i == this.editing) {
            this.editorY = ry;
            this.drawEditor(slots.get(i), ry, mx, my, dt, accent);
            ry += this.editorHeight() + 6.0F;
         }
      }
   }

   private void drawRow(WheelSlotsSetting.Slot slot, int i, float ry, float mx, float my, float dt, int accent) {
      float rh = row() - 4.0F;
      boolean over = mx >= this.x && mx < this.x + this.w && my >= ry && my < ry + rh;
      this.rowHover[i] = Easing.approach(this.rowHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
      boolean open = i == this.editing;
      if (!open && this.dragging != i) {
         Ui.surface(this.x, ry, this.w, rh, 7.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.rowHover[i]));
      } else {
         Ui.roundRect(this.x, ry, this.w, rh, 7.0F, ColorUtil.lerp(Theme.ACCENT_FAINT, Theme.ACCENT_SOFT, this.rowHover[i]));
         Ui.accentOutline(this.x, ry, this.w, rh, 7.0F, this.dragging == i ? 1.4F : 0.9F, accent);
      }

      float cy = ry + rh / 2.0F;
      int dots = ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT_DIM, this.rowHover[i]);

      for (int k = 0; k < 6; k++) {
         Ui.circle(this.x + 8.0F + k % 2 * 3.6F, cy - 3.6F + k / 2 * 3.6F, 0.9F, dots);
      }

      int per = this.module.wheel.perPage();
      String num = String.valueOf(i % per + 1);
      Ui.textMiddle(num, this.x + 22.0F, cy, 7.4F, Theme.TEXT_MUTED, false);
      float ix = this.x + 34.0F;
      WeaponDef d = this.module.library.get(slot.weapon);
      float icon = Math.min(18.0F, rh - 2.0F);
      if (d == null) {
         Ui.circle(ix + icon / 2.0F, cy, icon * 0.35F, Theme.PILL);
      } else if (d.isVanilla()) {
         Ui.item(SWORD, ix + (icon - 16.0F) / 2.0F, cy - 8.0F, 16.0F);
      } else {
         float[] uv = this.module.iconOf(d, slot.finishOrNull());
         if (uv != null) {
            Ui.image((int)uv[0], ix, cy - icon / 2.0F, icon, icon, uv[1], uv[2], uv[3], uv[4], -1);
         } else {
            Ui.circle(ix + icon / 2.0F, cy, icon * 0.35F, Theme.PILL);
         }
      }

      String name = d == null ? "Missing: " + slot.weapon : d.name;
      WeaponOptions.Finish f = slot.finishOrNull();
      String finish = d != null && !d.isVanilla() ? (f != null ? f.displayName : "its own finish") : null;
      float tx = ix + icon + 8.0F;
      float end = this.removeX() - 10.0F;
      float size = Density.compact() ? 8.6F : 9.0F;
      String shown = Ui.ellipsize(name, size, end - tx, true);
      Ui.textMiddle(shown, tx, cy, size, d == null ? Theme.DANGER : Theme.TEXT, true);
      float fx;
      if (finish != null && end - (fx = tx + Ui.widthBold(shown, size) + 7.0F) > 30.0F) {
         Ui.textMiddle(
            Ui.ellipsize("· " + finish, Density.small(), end - fx, false), fx, cy, Density.small(), f != null ? Theme.TEXT_DIM : Theme.TEXT_MUTED, false
         );
      }

      boolean overRemove = over && mx >= this.removeX();
      Ui.textMiddle("×", this.removeX() + 5.0F, cy - 0.5F, 9.4F, overRemove ? Theme.DANGER : Theme.TEXT_MUTED, false);
   }

   private void layoutEditor(float ex, float ey, float ew) {
      List<WheelSlotsSetting.Slot> slots = this.setting.get();
      if (this.editing >= 0 && this.editing < slots.size()) {
         WheelSlotsSetting.Slot slot = slots.get(this.editing);
         this.weaponIds.clear();
         ArrayList<String> labels = new ArrayList<>();

         for (WeaponDef d : this.module.library.all()) {
            if (this.weaponIds.size() >= 47) {
               break;
            }

            this.weaponIds.add(d.id);
            labels.add(d.name);
         }

         this.weaponLabels = labels.toArray(new String[0]);
         this.weaponLit = new boolean[this.weaponLabels.length];

         for (int i = 0; i < this.weaponIds.size(); i++) {
            this.weaponLit[i] = this.weaponIds.get(i).equals(slot.weapon);
         }

         this.weapons.layout(ex, ey + 8.0F + 14.0F, ew, this.weaponLabels, null);
         this.finishIds.clear();
         WeaponDef d = this.module.library.get(slot.weapon);
         if (d != null && !d.isVanilla()) {
            ArrayList<String> fl = new ArrayList<>();
            this.finishIds.add(null);
            fl.add("The Weapon's Own");

            for (String s : d.finishes) {
               WeaponOptions.Finish fin = WeaponOptions.Finish.of(s);
               if (fin != null && fin != WeaponOptions.Finish.CUSTOM && !this.finishIds.contains(fin.name().toLowerCase(Locale.ROOT))) {
                  this.finishIds.add(fin.name().toLowerCase(Locale.ROOT));
                  fl.add(fin.displayName);
               }
            }

            this.finishLabels = fl.toArray(new String[0]);
            this.finishLit = new boolean[this.finishLabels.length];

            for (int i = 0; i < this.finishIds.size(); i++) {
               String id = this.finishIds.get(i);
               this.finishLit[i] = id == null ? slot.finish == null : id.equalsIgnoreCase(slot.finish);
            }

            this.finishes.layout(ex, ey + 8.0F + 14.0F + this.weapons.height + 8.0F + 14.0F, ew, this.finishLabels, null);
         } else {
            this.finishLabels = new String[0];
            this.finishLit = new boolean[0];
         }
      } else {
         this.weaponLabels = new String[0];
         this.finishLabels = new String[0];
      }
   }

   private void drawEditor(WheelSlotsSetting.Slot slot, float ey, float mx, float my, float dt, int accent) {
      float ex = this.x + 10.0F;
      float ew = this.w - 20.0F;
      this.layoutEditor(ex, ey, ew);
      float eh = this.editorHeight();
      this.layoutEditor(ex, ey, ew);
      Ui.roundRect(this.x + 4.0F, ey - 2.0F, this.w - 8.0F, eh, 7.0F, ColorUtil.mulAlpha(Theme.SURFACE, 0.7F));
      Ui.outline(this.x + 4.0F, ey - 2.0F, this.w - 8.0F, eh, 7.0F, 0.8F, Theme.OUTLINE);
      Ui.text("Weapon", ex, ey + 8.0F, 7.8F, Theme.TEXT_DIM);
      this.weapons.draw(this.weaponLabels, this.weaponLit, null, null, mx, my, dt, accent);
      if (this.finishLabels.length > 0) {
         Ui.text("Finish", ex, ey + 8.0F + 14.0F + this.weapons.height + 8.0F, 7.8F, Theme.TEXT_DIM);
         this.finishes.draw(this.finishLabels, this.finishLit, null, null, mx, my, dt, accent);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         return false;
      } else {
         List<WheelSlotsSetting.Slot> slots = this.setting.get();
         if (button == 0 && this.add.contains(mx, my)) {
            if (slots.size() >= 48) {
               ModSounds.gui(Sfx.ERROR);
               return true;
            } else {
               this.setting.add(new WheelSlotsSetting.Slot(this.module.selected(), null));
               this.editing = this.setting.size() - 1;
               ModSounds.gui(Sfx.CONFIRM);
               return true;
            }
         } else {
            for (int i = 0; i < this.rows && i < slots.size(); i++) {
               if (!(my < this.rowY[i]) && !(my >= this.rowY[i] + row() - 4.0F)) {
                  if (button != 0) {
                     return true;
                  }

                  if (mx >= this.removeX()) {
                     this.setting.remove(i);
                     if (this.editing == i) {
                        this.editing = -1;
                     } else if (this.editing > i) {
                        this.editing--;
                     }

                     ModSounds.gui(Sfx.CLICK);
                     return true;
                  }

                  if (mx < this.x + 18.0F) {
                     this.dragging = i;
                     ModSounds.gui(Sfx.SELECT);
                     return true;
                  }

                  this.editing = this.editing == i ? -1 : i;
                  ModSounds.gui(this.editing >= 0 ? Sfx.DROPDOWN : Sfx.CLICK);
                  return true;
               }
            }

            if (this.editing >= 0 && this.editing < slots.size() && my >= this.editorY && my < this.editorY + this.editorHeight() && button == 0) {
               this.layoutEditor(this.x + 10.0F, this.editorY, this.w - 20.0F);
               WheelSlotsSetting.Slot slot = slots.get(this.editing);
               int k = this.weapons.at(mx, my);
               if (k >= 0 && k < this.weaponIds.size()) {
                  String weapon = this.weaponIds.get(k);
                  String finish = slot.finish;
                  WeaponDef d = this.module.library.get(weapon);
                  if (finish != null && (d == null || d.isVanilla() || !d.finishes.contains(finish.toLowerCase(Locale.ROOT)))) {
                     finish = null;
                  }

                  this.setting.set(this.editing, new WheelSlotsSetting.Slot(weapon, finish));
                  ModSounds.gui(Sfx.SELECT);
                  return true;
               }

               int f = this.finishes.at(mx, my);
               if (f >= 0 && f < this.finishIds.size()) {
                  this.setting.set(this.editing, new WheelSlotsSetting.Slot(slot.weapon, this.finishIds.get(f)));
                  ModSounds.gui(Sfx.SELECT);
                  return true;
               }
            }

            return true;
         }
      }
   }

   @Override
   public void mouseDrag(float mx, float my) {
      if (this.dragging >= 0) {
         List<WheelSlotsSetting.Slot> slots = this.setting.get();
         int to = this.dragging;

         for (int i = 0; i < this.rows && i < slots.size(); i++) {
            if (my >= this.rowY[i] && my < this.rowY[i] + row()) {
               to = i;
            }
         }

         if (my < this.y + 24.0F) {
            to = 0;
         }

         if (to != this.dragging) {
            if (this.editing == this.dragging) {
               this.editing = to;
            }

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
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else if (this.add.contains(mx, my)) {
         return "A new slot at the end: the weapon whose card is selected (click it to change its weapon and finish)";
      } else {
         for (int i = 0; i < this.rows && i < this.setting.size(); i++) {
            if (!(my < this.rowY[i]) && !(my >= this.rowY[i] + row() - 4.0F)) {
               if (mx >= this.removeX()) {
                  return "Remove this slot";
               }

               if (mx < this.x + 18.0F) {
                  return "Drag to move it round the wheel";
               }

               return i == this.editing ? "Close its weapon and finish" : "Change its weapon and finish";
            }
         }

         return null;
      }
   }

   void editForTest(int index) {
      this.editing = index;
   }
}
