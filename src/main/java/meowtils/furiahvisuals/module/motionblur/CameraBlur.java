package meowtils.furiahvisuals.module.motionblur;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import meowtils.furiahvisuals.asset.ShaderFiles;
import meowtils.furiahvisuals.compat.ReflectionHelper;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.post.Mat4f;
import meowtils.furiahvisuals.render.post.PostFrame;
import meowtils.furiahvisuals.render.post.PostProgram;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class CameraBlur {
   static final int[] SAMPLES = new int[]{6, 10, 16, 24};
   static final double WINDOW = 0.01;
   private static final int KEPT = 64;
   private static final String FRAGMENT = "motionblur/camera.fsh";
   private final PostProgram[] programs = new PostProgram[SAMPLES.length];
   private final int[][] uniforms = new int[SAMPLES.length][];
   private final float[] proj = new float[16];
   private final float[] view = new float[16];
   private final float[] vp = new float[16];
   private final float[] inv = new float[16];
   private final float[] tmp = new float[16];
   private final float[] prevVp = new float[16];
   private final float[] reproj = new float[16];
   private final float[] v4 = new float[4];
   private final float[][] keptProj = new float[64][16];
   private final float[][] keptView = new float[64][16];
   private final double[] keptX = new double[64];
   private final double[] keptY = new double[64];
   private final double[] keptZ = new double[64];
   private final double[] keptTime = new double[64];
   private int keptHead;
   private int keptCount;
   private float span;
   private int frameNumber;
   private boolean still = true;
   private float fallbackX;
   private float fallbackY;
   private static boolean looked;
   private static Field modelview;
   private static Field projection;
   private static final FloatBuffer READ = BufferUtils.createFloatBuffer(16);
   private static boolean toldDriver;

   boolean prepare(double x, double y, double z, double time, float movement, boolean skip, double window) {
      this.readMatrices();
      if (skip) {
         this.keptCount = 0;
      }

      int from;
      boolean ok = (from = this.earlier(time, window)) >= 0;
      if (ok) {
         this.span = (float)(time - this.keptTime[from]);
         ok = window <= 0.0 || this.span > 0.0F;
      }

      if (ok) {
         Mat4f.mul(this.proj, this.view, this.vp);
         ok = Mat4f.invert(this.vp, this.inv, this.tmp);
      }

      if (ok) {
         Mat4f.mul(this.keptProj[from], this.keptView[from], this.prevVp);
         Mat4f.translate(
            this.prevVp, (float)((x - this.keptX[from]) * movement), (float)((y - this.keptY[from]) * movement), (float)((z - this.keptZ[from]) * movement)
         );
         Mat4f.mul(this.prevVp, this.inv, this.reproj);
         this.still = Mat4f.offIdentity(this.reproj) < 1.0E-5F;
         Mat4f.transform(this.reproj, 0.0F, 0.0F, 1.0F, 1.0F, this.v4);
         if (this.v4[3] > 1.0E-6F) {
            this.fallbackX = 0.5F - (this.v4[0] / this.v4[3] * 0.5F + 0.5F);
            this.fallbackY = 0.5F - (this.v4[1] / this.v4[3] * 0.5F + 0.5F);
            if (!(Math.abs(this.fallbackX) < 4.0F) || !(Math.abs(this.fallbackY) < 4.0F)) {
               this.fallbackX = 0.0F;
               this.fallbackY = 0.0F;
            }
         } else {
            this.fallbackX = 0.0F;
            this.fallbackY = 0.0F;
         }
      } else {
         this.still = true;
      }

      this.keep(x, y, z, time);
      return ok && !this.still;
   }

   private int earlier(double time, double window) {
      int found = -1;

      for (int i = 0; i < this.keptCount; i++) {
         int at;
         found = at = (this.keptHead - i + 64) % 64;
         if (time - this.keptTime[at] >= window) {
            break;
         }
      }

      return found;
   }

   private void keep(double x, double y, double z, double time) {
      if (this.keptCount == 0 || this.keptTime[this.keptHead] != time) {
         this.keptHead = (this.keptHead + 1) % 64;
         this.keptCount = Math.min(64, this.keptCount + 1);
      }

      System.arraycopy(this.proj, 0, this.keptProj[this.keptHead], 0, 16);
      System.arraycopy(this.view, 0, this.keptView[this.keptHead], 0, 16);
      this.keptX[this.keptHead] = x;
      this.keptY[this.keptHead] = y;
      this.keptZ[this.keptHead] = z;
      this.keptTime[this.keptHead] = time;
   }

   float span() {
      return this.span;
   }

   void reset() {
      this.keptCount = 0;
   }

   void run(PostFrame frame, float scale, float maxLength, int quality, boolean linear) {
      int q = Math.max(0, Math.min(SAMPLES.length - 1, quality));
      if (this.programs[q] == null) {
         String source = ShaderFiles.require("motionblur/camera.fsh").replace("SAMPLES", String.valueOf(SAMPLES[q]));
         PostProgram p;
         this.programs[q] = p = PostProgram.link("motion blur camera (" + SAMPLES[q] + " samples)", source, "uColor", "uDepth");
         this.uniforms[q] = new int[]{
            p.uniform("uSize"),
            p.uniform("uR0"),
            p.uniform("uR1"),
            p.uniform("uR2"),
            p.uniform("uR3"),
            p.uniform("uBlur"),
            p.uniform("uFallback"),
            p.uniform("uLinear")
         };
      }

      int depth = frame.depth();
      int color = frame.color();
      frame.bindTarget();
      PostProgram p = this.programs[q];
      int[] u = this.uniforms[q];
      p.use();
      PostProgram.set(u[0], frame.width(), frame.height());

      for (int c = 0; c < 4; c++) {
         PostProgram.set(u[1 + c], this.reproj[c * 4], this.reproj[c * 4 + 1], this.reproj[c * 4 + 2], this.reproj[c * 4 + 3]);
      }

      this.frameNumber = this.frameNumber + 1 & 63;
      PostProgram.set(u[5], scale, maxLength, (float)frame.width() / Math.max(1, frame.height()), this.frameNumber);
      PostProgram.set(u[6], this.fallbackX * scale, this.fallbackY * scale);
      PostProgram.set(u[7], linear ? 1.0F : 0.0F);
      PostFrame.bind(1, depth);
      PostFrame.bind(0, color);
      GlStateManager.colorMask(true, true, true, false);
      PostFrame.quad();
      GlStateManager.colorMask(true, true, true, true);
   }

   private void readMatrices() {
      if (!looked) {
         looked = true;

         try {
            modelview = ReflectionHelper.findField(ActiveRenderInfo.class, new String[]{"MODELVIEW", "MODELVIEW"});
            projection = ReflectionHelper.findField(ActiveRenderInfo.class, new String[]{"PROJECTION", "PROJECTION"});
         } catch (RuntimeException var2) {
            modelview = null;
            projection = null;
         }
      }

      if (modelview != null && projection != null) {
         try {
            copy((FloatBuffer)modelview.get(null), this.view);
            copy((FloatBuffer)projection.get(null), this.proj);
            return;
         } catch (RuntimeException | IllegalAccessException var3) {
            modelview = null;
            projection = null;
         }
      }

      if (!toldDriver) {
         toldDriver = true;
         FuriahsVisuals.LOG.info("Motion Blur: ActiveRenderInfo's matrices can't be read here; the driver's are used");
      }

      ((Buffer)READ).clear();
      GL11.glGetFloat(2982, READ);
      copy(READ, this.view);
      ((Buffer)READ).clear();
      GL11.glGetFloat(2983, READ);
      copy(READ, this.proj);
   }

   private static void copy(FloatBuffer from, float[] to) {
      for (int i = 0; i < 16; i++) {
         to[i] = from.get(i);
      }
   }

   boolean allocated() {
      for (PostProgram p : this.programs) {
         if (p != null) {
            return true;
         }
      }

      return false;
   }

   float farMotionForTest(float aspect) {
      return this.still ? 0.0F : (float)Math.sqrt(this.fallbackX * aspect * this.fallbackX * aspect + this.fallbackY * this.fallbackY);
   }
}
