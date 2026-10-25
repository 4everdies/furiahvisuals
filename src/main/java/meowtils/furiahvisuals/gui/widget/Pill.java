package meowtils.furiahvisuals.gui.widget;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ColorSetting;

public final class Pill {
   public static final float H = 20.0F;
   public static final float TEXT = 8.8F;
   public float x;
   public float y;
   public float w;
   float hover;

   public float layout(float x, float y, String label) {
      this.x = x;
      this.y = y;
      this.w = Ui.width(label, 8.8F) + 20.0F;
      return this.w;
   }

   public boolean contains(float mx, float my) {
      return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + 20.0F;
   }

   public void draw(String label, float mx, float my, float dt, int accent, boolean accentStyle, boolean enabled) {
      this.hover = Easing.approach(this.hover, enabled && this.contains(mx, my) ? 1.0F : 0.0F, 16.0F, dt);
      Ui.registerControl(this.x, this.y, this.w, 20.0F, "button " + label);
      if (accentStyle) {
         Ui.roundRect(this.x, this.y, this.w, 20.0F, 10.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, this.hover));
         Ui.accentOutline(this.x, this.y, this.w, 20.0F, 10.0F, 1.0F, this.hover > 0.5F ? Theme.ACCENT_HOVER : Theme.ACCENT);
      } else {
         Ui.surface(this.x, this.y, this.w, 20.0F, 10.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hover));
      }

      int text = enabled ? ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, 0.35F + 0.65F * this.hover) : Theme.TEXT_MUTED;
      Ui.textMiddle(label, this.x + 10.0F, this.y + 10.0F, 8.8F, text, false);
   }

   public static void dot(float cx, float cy, float r, int colorA, int colorB, ColorSetting.Mode mode) {
      ColorSetting.Mode m = mode;
      int a = colorA;
      int b = colorB;
      if (mode == ColorSetting.Mode.GLOBAL && ColorSetting.getGlobal() != null) {
         ColorSetting g = ColorSetting.getGlobal();
         m = g.getEffectiveMode();
         a = g.getColor(0.0F);
         b = g.getColor(1.0F);
      }

      if (m != ColorSetting.Mode.CHROMA) {
         if (m == ColorSetting.Mode.GRADIENT) {
            Ui.gradientH(cx - r, cy - r, r * 2.0F, r * 2.0F, r, a | 0xFF000000, b | 0xFF000000);
         } else {
            Ui.circle(cx, cy, r, a | 0xFF000000);
         }

         Ui.outline(cx - r, cy - r, r * 2.0F, r * 2.0F, r, 0.7F, Theme.OUTLINE_HOVER);
      } else {
         for (int i = 0; i < 12; i++) {
            double ang = i / 12.0 * Math.PI * 2.0 + ColorSetting.clock() * 1.2;
            Ui.circle(cx + (float)Math.cos(ang) * r * 0.62F, cy + (float)Math.sin(ang) * r * 0.62F, r * 0.42F, ColorUtil.hsv(i / 12.0F, 0.85F, 1.0F));
         }

         Ui.circle(cx, cy, r * 0.32F, -1);
      }
   }
}
