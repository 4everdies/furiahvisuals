package meowtils.furiahvisuals.gui.widget;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;

public final class Segmented {
   private float highlight = -1.0F;
   private float shown = -1.0F;
   private float[] hover = new float[0];

   public void render(float x, float y, float w, float h, String[] labels, int selected, float mx, float my, float dt, int accent) {
      int n = labels.length;
      if (this.hover.length != n) {
         this.hover = new float[n];
      }

      if (selected >= 0) {
         float ow = this.highlight = this.highlight < 0.0F ? selected : Easing.approach(this.highlight, selected, 16.0F, dt);
      }

      this.shown = this.shown < 0.0F ? (selected >= 0 ? 1.0F : 0.0F) : Easing.approach(this.shown, selected >= 0 ? 1.0F : 0.0F, 16.0F, dt);
      float ow = w / n;
      Ui.registerControl(x, y, w, h, "segmented " + (n > 0 ? labels[0] : ""));
      Ui.surface(x, y, w, h, 6.0F, Theme.SURFACE);
      float hx = x + Math.max(0.0F, this.highlight) * ow;
      if (this.shown > 0.01F) {
         Ui.roundRect(hx + 2.0F, y + 2.0F, ow - 4.0F, h - 4.0F, 4.5F, ColorUtil.withAlpha(Theme.ACCENT_SOFT, this.shown));
         if (this.shown > 0.99F) {
            Ui.accentOutline(hx + 2.0F, y + 2.0F, ow - 4.0F, h - 4.0F, 4.5F, 1.0F, accent);
         } else {
            Ui.outline(hx + 2.0F, y + 2.0F, ow - 4.0F, h - 4.0F, 4.5F, 1.0F, ColorUtil.withAlpha(accent, this.shown));
         }
      }

      for (int i = 0; i < n; i++) {
         float ox = x + i * ow;
         boolean over = mx >= ox && mx < ox + ow && my >= y && my < y + h;
         this.hover[i] = Easing.approach(this.hover[i], over ? 1.0F : 0.0F, 16.0F, dt);
         float closeness = Math.max(0.0F, 1.0F - Math.abs(this.highlight - i)) * this.shown;
         int color = ColorUtil.lerp(ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hover[i] * 0.6F), Theme.TEXT, closeness);
         String label = Ui.ellipsize(labels[i], 9.0F, ow - 6.0F, closeness > 0.5F);
         float tw = closeness > 0.5F ? Ui.widthBold(label, 9.0F) : Ui.width(label, 9.0F);
         Ui.textMiddle(label, ox + (ow - tw) / 2.0F, y + h / 2.0F, 9.0F, color, closeness > 0.5F);
      }
   }

   public static int hit(float x, float y, float w, float h, int count, float mx, float my) {
      return !(mx < x) && !(mx >= x + w) && !(my < y) && !(my >= y + h) ? Math.min(count - 1, (int)((mx - x) / (w / count))) : -1;
   }

   public static float preferredWidth(String[] labels) {
      float max = 0.0F;

      for (String l : labels) {
         max = Math.max(max, Ui.widthBold(l, 9.0F));
      }

      return labels.length * (max + 16.0F);
   }
}
