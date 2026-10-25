package meowtils.furiahvisuals.gui.popup;

import java.util.List;
import meowtils.furiahvisuals.gui.MenuPrefs;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.ColorWidget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

public final class ColorPopover extends Popover {
   private static final float PAD = 8.0F;
   private static final float SW = 13.0F;
   private static final float SG = 3.0F;
   private static final float LABEL_H = 12.0F;
   private final ColorSetting setting;
   private final ColorWidget picker;
   private final int startA;
   private final int startB;
   private String hoverTip;

   public ColorPopover(ColorSetting setting, int slot) {
      this.setting = setting;
      this.picker = new ColorWidget(setting).alwaysExpanded().inPopover();
      this.picker.appear = 1.0F;
      this.picker.editSlot(slot);
      this.startA = setting.getColorA();
      this.startB = setting.getColorB();
   }

   private int perRow() {
      return Math.max(1, (int)((this.preferredW() - 16.0F + 3.0F) / 16.0F));
   }

   private float stripH(int count) {
      int rows = Math.max(1, (count + this.perRow() - 1) / this.perRow());
      return 12.0F + rows * 16.0F;
   }

   @Override
   protected float preferredW() {
      return 236.0F;
   }

   @Override
   protected float preferredH() {
      this.picker.w = this.preferredW() - 16.0F;
      return 8.0F + this.stripH(MenuPrefs.favorites().size()) + 4.0F + this.stripH(MenuPrefs.recents().size()) + 6.0F + this.picker.height() + 8.0F;
   }

   @Override
   protected void renderContent(float mx, float my, float dt, int accent) {
      this.hoverTip = null;
      float cy = this.y + 8.0F;
      cy = this.strip("FAVORITES", MenuPrefs.favorites(), "★ a color in its row to keep it here", cy, mx, my, true);
      cy += 4.0F;
      cy = this.strip("RECENT", MenuPrefs.recents(), "Colors you pick show up here", cy, mx, my, false);
      float var9;
      Ui.rect(this.x + 8.0F, var9 = cy + 2.0F, this.w - 16.0F, 0.6F, Theme.DIVIDER);
      this.picker.x = this.x + 8.0F;
      this.picker.y = cy = var9 + 4.0F;
      this.picker.w = this.w - 16.0F;
      this.picker.render(mx, my, dt, accent);
   }

   private float strip(String label, List<Integer> colors, String empty, float top, float mx, float my, boolean favorites) {
      Ui.textMiddle(label, this.x + 8.0F, top + 5.0F, 6.6F, Theme.TEXT_MUTED, true);
      float sy = top + 12.0F;
      if (colors.isEmpty()) {
         Ui.textMiddle(empty, this.x + 8.0F, sy + 6.5F, 7.4F, Theme.TEXT_MUTED, false);
         return sy + 13.0F + 3.0F;
      } else {
         int per = this.perRow();

         for (int i = 0; i < colors.size(); i++) {
            float sx = this.x + 8.0F + i % per * 16.0F;
            float yy = sy + i / per * 16.0F;
            int c = colors.get(i);
            if (c >>> 24 < 255) {
               Ui.checker(sx, yy, 13.0F, 13.0F, 3.5F, 3.0F);
            }

            Ui.roundRect(sx, yy, 13.0F, 13.0F, 3.5F, c);
            boolean over = mx >= sx && mx < sx + 13.0F && my >= yy && my < yy + 13.0F;
            Ui.outline(sx, yy, 13.0F, 13.0F, 3.5F, over ? 1.2F : 0.8F, over ? ColorUtil.withAlpha(-1, 0.9F) : Theme.OUTLINE_HOVER);
            if (over) {
               this.hoverTip = ColorUtil.toHex(c) + (favorites ? "  (click: use it, right-click: remove)" : "  (click: use it)");
            }
         }

         int rows = (colors.size() + per - 1) / per;
         return sy + rows * 16.0F;
      }
   }

   private int swatchAt(List<Integer> colors, float top, float mx, float my) {
      float sy = top + 12.0F;
      int per = this.perRow();

      for (int i = 0; i < colors.size(); i++) {
         float sx = this.x + 8.0F + i % per * 16.0F;
         float yy = sy + i / per * 16.0F;
         if (mx >= sx && mx < sx + 13.0F && my >= yy && my < yy + 13.0F) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      float cy = this.y + 8.0F;
      List<Integer> fav = MenuPrefs.favorites();
      int f = this.swatchAt(fav, cy, mx, my);
      if (f >= 0) {
         if (button == 1) {
            MenuPrefs.toggleFavorite(fav.get(f));
            ModSounds.gui(Sfx.CLICK);
         } else {
            this.picker.applyColor(fav.get(f));
            ModSounds.gui(Sfx.CLICK);
         }

         return true;
      } else {
         List<Integer> rec = MenuPrefs.recents();
         float var9;
         int r = this.swatchAt(rec, var9 = cy + (this.stripH(fav.size()) + 4.0F), mx, my);
         if (r >= 0) {
            this.picker.applyColor(rec.get(r));
            ModSounds.gui(Sfx.CLICK);
            return true;
         } else {
            if (this.picker.contains(mx, my)) {
               this.picker.mouseDown(mx, my, button);
            }

            return true;
         }
      }
   }

   @Override
   public void mouseDrag(float mx, float my) {
      this.picker.mouseDrag(mx, my);
   }

   @Override
   public void mouseUp(float mx, float my) {
      this.picker.mouseUp(mx, my);
   }

   @Override
   public boolean isDragging() {
      return this.picker.isDragging();
   }

   @Override
   public boolean wheel(float mx, float my, int wheel) {
      this.picker.mouseWheel(mx, my, wheel);
      return this.contains(mx, my);
   }

   @Override
   public boolean wantsKeys() {
      return this.picker.isFocused();
   }

   @Override
   public boolean keyTyped(char c, int key) {
      return this.picker.keyTyped(c, key);
   }

   @Override
   public String tooltip(float mx, float my) {
      return this.hoverTip != null ? this.hoverTip : this.picker.tooltip(mx, my);
   }

   @Override
   protected void closed() {
      this.picker.unfocus();
      if (this.setting.getColorA() != this.startA) {
         MenuPrefs.used(this.setting.getColorA());
      }

      if (this.setting.getColorB() != this.startB && this.setting.getMode() == ColorSetting.Mode.GRADIENT) {
         MenuPrefs.used(this.setting.getColorB());
      }
   }
}
