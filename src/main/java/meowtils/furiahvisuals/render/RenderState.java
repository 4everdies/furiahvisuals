package meowtils.furiahvisuals.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;

public final class RenderState {
   private static int depth;

   private RenderState() {
   }

   public static void begin() {
      if (depth++ <= 0) {
         apply();
      }
   }

   public static void suspend() {
      if (depth > 0) {
         Vfx.flush();
         restore();
      }
   }

   public static void resume() {
      if (depth > 0) {
         apply();
      }
   }

   private static void apply() {
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GlStateManager.disableTexture2D();
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      GlStateManager.disableLighting();
      GlStateManager.disableFog();
      GlStateManager.disableAlpha();
      GlStateManager.enableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.enableDepth();
      GlStateManager.depthFunc(515);
      GlStateManager.disableCull();
      GlStateManager.shadeModel(7425);
      additive();
      Vfx.resetTextureBinding();
   }

   public static void end() {
      if (depth != 0 && --depth <= 0) {
         Vfx.flush();
         restore();
      }
   }

   private static void restore() {
      GlStateManager.disablePolygonOffset();
      GlStateManager.doPolygonOffset(0.0F, 0.0F);
      GlStateManager.depthMask(true);
      GlStateManager.depthFunc(515);
      GlStateManager.enableDepth();
      GlStateManager.enableCull();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.alphaFunc(516, 0.1F);
      GlStateManager.shadeModel(7424);
      GlStateManager.enableTexture2D();
      GlStateManager.resetColor();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void additive() {
      Vfx.flush();
      GlStateManager.tryBlendFuncSeparate(770, 1, 1, 771);
      GlStateManager.depthMask(false);
   }

   public static void translucent(boolean writeDepth) {
      Vfx.flush();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
      GlStateManager.depthMask(writeDepth);
   }

   public static void decal(boolean on) {
      Vfx.flush();
      if (on) {
         GlStateManager.enablePolygonOffset();
         GlStateManager.doPolygonOffset(-2.0F, -8.0F);
      } else {
         GlStateManager.disablePolygonOffset();
         GlStateManager.doPolygonOffset(0.0F, 0.0F);
      }
   }
}
