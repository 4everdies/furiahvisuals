package meowtils.furiahvisuals.hud;

import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.module.watermark.IslandGlass;
import meowtils.furiahvisuals.render.ColorUtil;
import net.minecraft.client.Minecraft;

public final class HudPanel {
   private static final HudGlint.Shine SHINE = new HudGlint.Shine();

   private HudPanel() {
   }

   private static float units1080(float pixels1080) {
      return pixels1080 * Minecraft.getMinecraft().displayHeight / 1080.0F / Ui.px;
   }

   public static void background(float x, float y, float w, float h, float r, HudLook look, float alpha) {
      if (!(alpha <= 0.002F) && !(w <= 0.0F) && !(h <= 0.0F) && look.background) {
         r = Math.min(r, Math.min(w, h) / 2.0F);
         if (look.shadow) {
            Ui.shadow(x, y + units1080(2.5F), w, h, r, units1080(9.0F), ColorUtil.mulAlpha(look.colors.shadow, (look.colors.light ? 0.35F : 0.6F) * alpha));
         }

         boolean glassDrawn = look.glass && IslandGlass.hasScreen() && IslandGlass.draw(x, y, w, h, r, look.colors.glassTint, look.colors.glassRim, alpha);
         if (!glassDrawn) {
            Ui.roundRect(x, y, w, h, r, look.bg(alpha));
         }
      }
   }

   public static void foreground(float x, float y, float w, float h, float r, HudLook look, float alpha) {
      if (!(alpha <= 0.002F) && !(w <= 0.0F) && !(h <= 0.0F)) {
         r = Math.min(r, Math.min(w, h) / 2.0F);
         if (look.outline) {
            Ui.outline(x, y, w, h, r, 1.0F / Ui.px, ColorUtil.mulAlpha(look.colors.outline, alpha));
         }

         if (look.glint && look.background) {
            glint(x, y, w, h, r, look.colors.accent, look.global, look.colors.light, alpha);
         }
      }
   }

   public static void glint(float x, float y, float w, float h, float r, int accent, int global, boolean light, float alpha) {
      glint(x, y, w, h, r, accent, global, light, alpha, 1.0F);
   }

   public static void glint(float x, float y, float w, float h, float r, int accent, int global, boolean light, float alpha, float widthScale) {
      float px = Ui.px;
      float cx = (x + w / 2.0F) * px;
      float cy = (y + h / 2.0F + Ui.offsetY) * px;
      if (HudGlint.shine(cx, cy, w * px, h * px, accent, global, light, widthScale, SHINE)) {
         int color = ColorUtil.mulAlpha(SHINE.color, alpha);
         Ui.glint(x, y, w, h, r, color, SHINE.band / px, SHINE.halfWidth / px, SHINE.angle, SHINE.trail);
      }
   }
}
