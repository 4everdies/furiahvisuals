package meowtils.furiahvisuals.module.watermark;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;

final class IconPickerWidget extends Widget {
   private static final float BTN_W = 28.0F;
   private static final float BTN_H = 22.0F;
   private static final float GAP = 4.0F;
   private static final float ROW = 30.0F;
   private final IconSetting setting;
   private final float[] hovers;
   private final float[] bx;
   private final float[] by;
   private int hovered = -1;
   private float lastHeight = 30.0F;

   IconPickerWidget(IconSetting setting) {
      this.setting = setting;
      int n = setting.icons().length + 1;
      this.hovers = new float[n];
      this.bx = new float[n];
      this.by = new float[n];
   }

   @Override
   public float height() {
      return this.lastHeight;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private IslandIcons.Icon icon(int i) {
      return i == 0 ? null : this.setting.icons()[i - 1];
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      int n = this.hovers.length;
      float labelW = Ui.width(this.setting.getName(), 10.0F) + 16.0F;
      float room = Math.max(28.0F, this.w - labelW);
      int perRow = Math.max(1, (int)((room + 4.0F) / 32.0F));
      int rows = (n + perRow - 1) / perRow;
      this.lastHeight = 30.0F + (rows - 1) * 26.0F;
      Ui.textMiddle(this.setting.getName(), this.x, this.y + 15.0F, 10.0F, Theme.TEXT, false);
      this.hovered = -1;
      IslandIcons.Icon chosen = this.setting.get();

      for (int i = 0; i < n; i++) {
         int row = i / perRow;
         int col = i % perRow;
         int inRow = Math.min(perRow, n - row * perRow);
         float rowW = inRow * 28.0F + (inRow - 1) * 4.0F;
         float cx = this.x + this.w - rowW + col * 32.0F;
         float cy = this.y + 4.0F + row * 26.0F;
         this.bx[i] = cx;
         this.by[i] = cy;
         boolean over = mx >= cx && mx < cx + 28.0F && my >= cy && my < cy + 22.0F;
         if (over) {
            this.hovered = i;
         }

         this.hovers[i] = Easing.approach(this.hovers[i], over ? 1.0F : 0.0F, 16.0F, dt);
         IslandIcons.Icon icon = this.icon(i);
         boolean selected = icon == chosen;
         if (selected) {
            Ui.roundRect(cx, cy, 28.0F, 22.0F, 7.0F, Theme.ACCENT_SOFT);
            Ui.accentOutline(cx, cy, 28.0F, 22.0F, 7.0F, 0.9F, accent);
         } else {
            Ui.surface(cx, cy, 28.0F, 22.0F, 7.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hovers[i]));
         }

         int color = selected ? accent : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hovers[i]);
         if (icon == null) {
            String off = "Off";
            Ui.textMiddle(off, cx + (28.0F - Ui.width(off, 7.8F)) / 2.0F, cy + 11.0F, 7.8F, color, selected);
         } else if (IslandIcons.texture() != 0) {
            float s = 13.0F;
            Ui.image(
               IslandIcons.texture(),
               cx + (28.0F - s) / 2.0F,
               cy + (22.0F - s) / 2.0F,
               s,
               s,
               IslandIcons.u0(icon),
               IslandIcons.v0(icon),
               IslandIcons.u0(icon) + IslandIcons.du(),
               IslandIcons.v0(icon) + IslandIcons.dv(),
               color
            );
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         for (int i = 0; i < this.hovers.length; i++) {
            if (mx >= this.bx[i] && mx < this.bx[i] + 28.0F && my >= this.by[i] && my < this.by[i] + 22.0F) {
               this.setting.set(this.icon(i));
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (this.hovered >= 0) {
         return this.setting.describe(this.icon(this.hovered));
      } else {
         return this.contains(mx, my) ? this.setting.getDescription() : null;
      }
   }
}
