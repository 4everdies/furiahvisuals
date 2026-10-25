package meowtils.furiahvisuals.module.ui.crosshair;

import meowtils.furiahvisuals.gui.Ui;
import net.minecraft.client.renderer.GlStateManager;

final class MenuPixels {
   private static float savedPx;
   private static int depth;

   private MenuPixels() {
   }

   static void begin(float x, float y) {
      if (depth++ > 0) {
         throw new IllegalStateException("MenuPixels.begin twice");
      } else {
         savedPx = Ui.px;
         GlStateManager.pushMatrix();
         GlStateManager.translate(x, y, 0.0F);
         GlStateManager.scale(1.0F / savedPx, 1.0F / savedPx, 1.0F);
         Ui.px = 1.0F;
      }
   }

   static float menuPx() {
      return depth > 0 ? savedPx : Ui.px;
   }

   static void end() {
      depth = 0;
      Ui.px = savedPx;
      GlStateManager.popMatrix();
      Ui.premultiplied(false);
      GlStateManager.disableAlpha();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
