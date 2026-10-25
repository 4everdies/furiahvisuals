package meowtils.furiahvisuals.render.look;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.GlSave;
import meowtils.furiahvisuals.render.GlTarget;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class Portrait {
   public static final int W = 512;
   public static final int H = 768;
   public static final float VIEW_W = 1.4F;
   public static final float VIEW_H = 2.1F;
   public static final float FEET_AT = 0.08F;
   private static final int POOL = 4;
   private static final GlTarget[] TARGETS = new GlTarget[4];
   private static int next;
   private static boolean failed;
   private static final GlSave SAVE = new GlSave();
   private static final Camera CAM = new Camera();
   private static float lastMillis;

   private Portrait() {
   }

   public static int capture(BodyProxy.Snapshot s, BodyProxy.Part[] parts, BodyProxy.Look look, double feetX, double feetY, double feetZ, float yaw) {
      if (!failed && OpenGlHelper.framebufferSupported) {
         long start = System.nanoTime();
         Vfx.flush();
         Camera before = Vfx.camera();
         GlStateManager.matrixMode(5889);
         GlStateManager.pushMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();
         SAVE.save();

         int var35;
         try {
            GlTarget t = TARGETS[next];
            if (t != null || (t = TARGETS[next] = GlTarget.create(512, 768, 32856, 6408, true, true)) != null) {
               next = (next + 1) % 4;
               t.bind();
               GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
               Vfx.resetTextureBinding();
               GL20.glUseProgram(0);
               GL11.glDisable(3089);
               GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
               GlStateManager.clear(16640);
               GlStateManager.matrixMode(5889);
               GlStateManager.loadIdentity();
               GlStateManager.ortho(-0.7F, 0.7F, -0.08F, 2.02F, -20.0, 20.0);
               GlStateManager.matrixMode(5888);
               GlStateManager.loadIdentity();
               GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
               double rad = Math.toRadians(yaw);
               double fx = -Math.sin(rad);
               double fz = Math.cos(rad);
               CAM.posX = fx * 12.0;
               CAM.posY = 0.95;
               CAM.posZ = fz * 12.0;
               CAM.forwardX = -fx;
               CAM.forwardY = 0.0;
               CAM.forwardZ = -fz;
               CAM.rightX = -fz;
               CAM.rightY = 0.0;
               CAM.rightZ = fx;
               CAM.upX = 0.0;
               CAM.upY = 1.0;
               CAM.upZ = 0.0;
               Vfx.setCamera(CAM);
               Textures.ensureLoaded();
               RenderState.begin();

               try {
                  GlStateManager.enableDepth();
                  boolean lit = look.lit;
                  boolean lightmap = look.lightmap;
                  look.lit = false;
                  look.lightmap = false;
                  BodyProxy.draw(s, parts, look, feetX, feetY, feetZ);
                  look.lit = lit;
                  look.lightmap = lightmap;
                  Vfx.flush();
               } finally {
                  RenderState.end();
               }

               return t.texture;
            }

            failed = true;
            FuriahsVisuals.LOG.warn("Portraits are off: no offscreen target could be made here");
            int n = -1;
            var35 = n;
         } catch (RuntimeException | LinkageError var31) {
            failed = true;
            FuriahsVisuals.LOG.warn("Portraits are off: drawing one failed", var31);
            return -1;
         } finally {
            SAVE.restore();
            Vfx.resetTextureBinding();
            GlStateManager.matrixMode(5889);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            Vfx.setCamera(before);
            lastMillis = (float)(System.nanoTime() - start) / 1000000.0F;
         }

         return var35;
      } else {
         return -1;
      }
   }

   public static BufferedImage readForTest(int texture) {
      if (texture < 0) {
         return null;
      } else {
         GlStateManager.bindTexture(texture);
         Vfx.resetTextureBinding();
         IntBuffer buf = BufferUtils.createIntBuffer(393216);
         GL11.glGetTexImage(3553, 0, 32993, 33639, buf);
         int[] px = new int[393216];
         buf.get(px);
         BufferedImage img = new BufferedImage(512, 768, 2);

         for (int y = 0; y < 768; y++) {
            img.setRGB(0, 767 - y, 512, 1, px, y * 512, 512);
         }

         return img;
      }
   }

   public static float lastMillisForTest() {
      return lastMillis;
   }

   public static boolean available() {
      return !failed && OpenGlHelper.framebufferSupported;
   }

   public static void failForTest(boolean on) {
      failed = on;
   }
}
