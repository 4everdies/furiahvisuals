package meowtils.furiahvisuals.module.weapons;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;

final class ChipRow {
   static final float H = 20.0F;
   static final float GAP = 5.0F;
   static final float TEXT = 8.8F;
   private static final int MAX = 48;
   final float[] cx = new float[48];
   final float[] cy = new float[48];
   final float[] cw = new float[48];
   final float[] hover = new float[48];
   int count;
   float height = 20.0F;

   float layout(float x, float y, float w, String[] labels, boolean[] dots) {
      this.count = Math.min(48, labels.length);
      float px = x;
      float py = y;

      for (int i = 0; i < this.count; i++) {
         float pw = Ui.width(labels[i], 8.8F) + 18.0F + (dots != null && dots[i] ? 12.0F : 0.0F);
         if (px > x && px + pw > x + w) {
            px = x;
            py += 25.0F;
         }

         this.cx[i] = px;
         this.cy[i] = py;
         this.cw[i] = pw;
         px += pw + 5.0F;
      }

      this.height = this.count == 0 ? 0.0F : py - y + 20.0F;
      return this.height;
   }

   float layoutFixed(float x, float y, int n, float pw) {
      this.count = Math.min(48, n);

      for (int i = 0; i < this.count; i++) {
         this.cx[i] = x + i * (pw + 5.0F);
         this.cy[i] = y;
         this.cw[i] = pw;
      }

      this.height = this.count == 0 ? 0.0F : 20.0F;
      return this.height;
   }

   int at(float mx, float my) {
      for (int i = 0; i < this.count; i++) {
         if (mx >= this.cx[i] && mx < this.cx[i] + this.cw[i] && my >= this.cy[i] && my < this.cy[i] + 20.0F) {
            return i;
         }
      }

      return -1;
   }

   void draw(String[] labels, boolean[] selected, boolean[] enabled, int[] dotColors, float mx, float my, float dt, int accent) {
      for (int i = 0; i < this.count; i++) {
         boolean on = enabled == null || enabled[i];
         boolean over = on && mx >= this.cx[i] && mx < this.cx[i] + this.cw[i] && my >= this.cy[i] && my < this.cy[i] + 20.0F;
         this.hover[i] = Easing.approach(this.hover[i], over ? 1.0F : 0.0F, 16.0F, dt);
         boolean sel = selected != null && selected[i];
         Ui.registerControl(this.cx[i], this.cy[i], this.cw[i], 20.0F, "chip " + labels[i]);
         Ui.pushAlpha(on ? 1.0F : 0.4F);
         if (sel) {
            Ui.roundRect(this.cx[i], this.cy[i], this.cw[i], 20.0F, 10.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, this.hover[i]));
            Ui.accentOutline(this.cx[i], this.cy[i], this.cw[i], 20.0F, 10.0F, 1.0F, accent);
         } else {
            Ui.surface(this.cx[i], this.cy[i], this.cw[i], 20.0F, 10.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hover[i]));
         }

         float tx = this.cx[i] + 9.0F;
         if (dotColors != null && dotColors[i] != 0) {
            Ui.circle(tx + 3.5F, this.cy[i] + 10.0F, 3.6F, dotColors[i] | 0xFF000000);
            Ui.outline(tx, this.cy[i] + 10.0F - 3.6F, 7.2F, 7.2F, 3.6F, 0.6F, Theme.OUTLINE_HOVER);
            tx += 12.0F;
         }

         int text = sel ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hover[i]);
         Ui.textMiddle(labels[i], tx, this.cy[i] + 10.0F, 8.8F, text, sel);
         Ui.popAlpha();
      }
   }
}
