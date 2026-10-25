package meowtils.furiahvisuals.module.glow;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.asset.ShaderFiles;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.weapons.WeaponHooks;
import meowtils.furiahvisuals.render.GlTarget;
import meowtils.furiahvisuals.render.GpuTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

final class GlowRenderer {
   private static final String ROOT = "glow/";
   private static final int LEVELS = 7;
   private static final float MAX_SIGMA = 8.0F;
   private static final long MAX_MASK_PIXELS = 16777216L;
   private static final IntBuffer INTS = BufferUtils.createIntBuffer(16);
   private static final FloatBuffer FLOATS = BufferUtils.createFloatBuffer(16);
   private static final ByteBuffer LUT_BYTES = BufferUtils.createByteBuffer(512);
   private static final int SHAPE_LEVEL = 5;
   private static final int SHAPE_W = 16;
   private static final int SHAPE_H = 8;
   private boolean compiled;
   private int filesGeneration = -1;
   private boolean broken;
   private String brokenReason;
   private GlowRenderer.Program maskP;
   private GlowRenderer.Program downP;
   private GlowRenderer.Program blurP;
   private GlowRenderer.Program rowsP;
   private GlowRenderer.Program colsP;
   private GlowRenderer.Program trailP;
   private GlowRenderer.Program compositeP;
   private GlowRenderer.Program tintP;
   private GlowRenderer.Program momentsP;
   private GlowRenderer.Program totalP;
   private boolean rg;
   private int fmtR;
   private int pixR;
   private int fmtRG;
   private int pixRG;
   private int maxTexture;
   private int screenW;
   private int screenH;
   private int maskScale = 1;
   private GlTarget mask;
   private GlTarget rows;
   private GlTarget dist;
   private final GlTarget[] levels = new GlTarget[7];
   private GlTarget haloA;
   private GlTarget haloB;
   private int haloLevel = -1;
   private GlTarget trailA;
   private GlTarget trailB;
   private int trailLevel = -1;
   private boolean trailOnA;
   private boolean trailClean = true;
   private GlTarget shapeA;
   private GlTarget shapeB;
   private GlTarget shapeTotal;
   private boolean shapeFailed;
   private int lutTexture = -1;
   private int noiseTexture = -1;
   private boolean maskDrawn;
   private boolean maskFilled;
   private boolean hasShape;
   private float centerX;
   private float centerY;
   private float motionX;
   private float motionY;
   private boolean hadCenter;
   private final float[] modelView = new float[16];
   private final float[] projection = new float[16];
   private float smokeClock;
   private final GpuTimer maskTimer = new GpuTimer();
   private final GpuTimer glowTimer = new GpuTimer();
   private boolean measuring;
   private float cpuMillis;
   private static final String[] PARTS = new String[]{"mask", "levels", "rim", "halo", "trail", "shape", "composite"};
   private final GpuTimer[] partTimers = new GpuTimer[PARTS.length];
   private boolean profiling;
   private final GlowRenderer.State passState = new GlowRenderer.State();
   private final GlowRenderer.State utilState = new GlowRenderer.State();

   String unavailableReason() {
      if (!OpenGlHelper.framebufferSupported) {
         return "Your graphics driver has no framebuffer support, which the glow needs.";
      } else if (!OpenGlHelper.shadersSupported || !openGl20()) {
         return "Your graphics driver has no shader support (OpenGL 2.0), which the glow needs.";
      } else {
         return this.broken ? "The glow could not start on this graphics driver (" + this.brokenReason + "; see the log)." : null;
      }
   }

   private static boolean openGl20() {
      try {
         return GLContext.getCapabilities().OpenGL20;
      } catch (RuntimeException | LinkageError var1) {
         return false;
      }
   }

   boolean isBroken() {
      return this.broken;
   }

   void renderMask(Minecraft mc, float partialTicks, boolean fill, float cutoff, int quality) {
      this.maskDrawn = false;
      if (!this.broken && this.prepare(mc.displayWidth, mc.displayHeight, quality)) {
         long start = System.nanoTime();
         if (this.measuring) {
            this.maskTimer.begin();
         }

         this.partBegin(0);
         GlowRenderer.State saved = this.passState.save(false);

         try {
            ((Buffer)FLOATS).clear();
            GL11.glGetFloat(3106, FLOATS);
            float cr = FLOATS.get(0);
            float cg = FLOATS.get(1);
            float cb = FLOATS.get(2);
            float ca = FLOATS.get(3);
            this.mask.bind();
            GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GlStateManager.depthMask(true);
            GlStateManager.clear(16640);
            GlStateManager.clearColor(cr, cg, cb, ca);
            boolean drawn = false;
            if (fill && ViewModel.pushHandCamera(mc, partialTicks)) {
               try {
                  this.maskP.use();
                  this.maskP.set1f("uCutoff", cutoff);
                  WeaponHooks.maskPass(true);

                  try {
                     mc.getItemRenderer().renderItemInFirstPerson(partialTicks);
                  } finally {
                     WeaponHooks.maskPass(false);
                  }

                  float lastX = this.centerX;
                  float lastY = this.centerY;
                  this.findCenter(mc, partialTicks);
                  float limit = 0.15F * this.screenH;
                  this.motionX = this.hadCenter && Math.abs(lastX - this.centerX) < limit ? lastX - this.centerX : 0.0F;
                  this.motionY = this.hadCenter && Math.abs(lastY - this.centerY) < limit ? lastY - this.centerY : 0.0F;
                  this.hadCenter = true;
                  drawn = true;
               } finally {
                  ViewModel.popHandCamera();
               }
            }

            if (!drawn) {
               this.hadCenter = false;
               this.motionY = 0.0F;
               this.motionX = 0.0F;
            }

            this.maskDrawn = true;
            this.maskFilled = drawn;
         } catch (RuntimeException | LinkageError var33) {
            this.fail("the mask pass failed", var33);
         } finally {
            saved.restore();
            this.partEnd(0);
            if (this.measuring) {
               this.maskTimer.end();
            }
         }

         this.cpuMillis = (float)(System.nanoTime() - start) / 1000000.0F;
      }
   }

   private void findCenter(Minecraft mc, float partialTicks) {
      ((Buffer)FLOATS).clear();
      GL11.glGetFloat(2982, FLOATS);
      FLOATS.get(this.modelView);
      ((Buffer)FLOATS).clear();
      GL11.glGetFloat(2983, FLOATS);
      FLOATS.get(this.projection);
      double[] p = ViewModel.center(mc, partialTicks);
      float[] m = this.modelView;
      double ex = m[0] * p[0] + m[4] * p[1] + m[8] * p[2] + m[12];
      double ey = m[1] * p[0] + m[5] * p[1] + m[9] * p[2] + m[13];
      double ez = m[2] * p[0] + m[6] * p[1] + m[10] * p[2] + m[14];
      double ew = m[3] * p[0] + m[7] * p[1] + m[11] * p[2] + m[15];
      float[] q = this.projection;
      double cx = q[0] * ex + q[4] * ey + q[8] * ez + q[12] * ew;
      double cy = q[1] * ex + q[5] * ey + q[9] * ez + q[13] * ew;
      double cw = q[3] * ex + q[7] * ey + q[11] * ez + q[15] * ew;
      if (cw > 1.0E-4) {
         this.centerX = (float)((cx / cw * 0.5 + 0.5) * this.screenW);
         this.centerY = (float)((cy / cw * 0.5 + 0.5) * this.screenH);
      }
   }

   boolean takeMask() {
      boolean drawn = this.maskDrawn;
      this.maskDrawn = false;
      return drawn;
   }

   boolean maskFilled() {
      return this.maskFilled;
   }

   float centerX() {
      return this.centerX;
   }

   float centerY() {
      return this.centerY;
   }

   void renderGlow(GlowParams p, float dt, int quality) {
      if (!this.broken && this.mask != null) {
         long start = System.nanoTime();
         boolean filled = this.maskFilled;
         boolean trailOn = p.trailSeconds > 0.0F && (filled || !this.trailClean);
         if (filled || trailOn) {
            if (this.measuring) {
               this.glowTimer.begin();
            }

            GlowRenderer.State saved = this.passState.save(true);

            try {
               GlStateManager.disableBlend();
               GlStateManager.disableDepth();
               GlStateManager.depthMask(false);
               GlStateManager.disableCull();
               GlStateManager.disableAlpha();
               GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
               int base = quality == 0 ? 2 : (quality == 1 ? 1 : 0);
               float sigma = p.haloRadius * 0.5F;
               int halo = base;

               while (halo < 6 && sigma / (1 << halo) > 8.0F) {
                  halo++;
               }

               float rimReach = p.rimWidth * (1.0F + 1.2F * p.rimSoftness) + 2.0F;
               int flag = Math.max(2, Math.min(6, 32 - Integer.numberOfLeadingZeros((int)Math.ceil(rimReach))));
               boolean shape = this.rg && !this.shapeFailed && (p.direction != 0 || p.flowAmount > 0.0F && p.flowDirection == 1 || p.shimmerStrength > 0.0F);
               int haloTexture = -1;
               this.hasShape = false;
               if (filled) {
                  int deepest = Math.max(halo, p.rimWidth > 0.0F ? flag : 0);
                  if (trailOn) {
                     deepest = Math.max(deepest, base);
                  }

                  if (shape) {
                     deepest = Math.max(deepest, 5);
                  }

                  this.partBegin(1);
                  this.buildLevels(deepest);
                  this.partEnd(1);
                  if (p.rimWidth > 0.0F) {
                     this.partBegin(2);
                     this.distanceField(flag, rimReach);
                     this.partEnd(2);
                  }

                  this.partBegin(3);
                  haloTexture = sigma > 0.05F ? this.blurHalo(halo, sigma) : this.coverage(halo);
                  this.partEnd(3);
                  if (shape) {
                     this.partBegin(5);
                     this.measureShape();
                     this.partEnd(5);
                  }
               }

               if (trailOn) {
                  this.partBegin(4);
                  this.updateTrail(p, dt, base, filled ? haloTexture : -1);
                  this.partEnd(4);
               }

               this.partBegin(6);
               this.uploadLut(p);
               saved.bindTarget();
               if (filled && p.itemTint > 0.001F) {
                  this.tint(p);
               }

               this.composite(p, filled, haloTexture, trailOn);
               this.partEnd(6);
            } catch (RuntimeException | LinkageError var21) {
               this.fail("the glow pass failed", var21);
            } finally {
               saved.restore();
               if (this.measuring) {
                  this.glowTimer.end();
               }
            }

            this.cpuMillis = this.cpuMillis + (float)(System.nanoTime() - start) / 1000000.0F;
         }
      }
   }

   private void buildLevels(int deepest) {
      this.downP.use();

      for (int k = this.maskScale == 2 ? 0 : 1; k <= deepest; k++) {
         GlTarget target = this.level(k);
         int source = k == 0 ? this.mask.texture : this.coverage(k - 1);
         int sw = k == 0 ? this.mask.width : (k == 1 && this.maskScale == 1 ? this.mask.width : this.levels[k - 1].width);
         int sh = k == 0 ? this.mask.height : (k == 1 && this.maskScale == 1 ? this.mask.height : this.levels[k - 1].height);
         boolean fromMask = k == 0 || k == 1 && this.maskScale == 1;
         this.downP.set2f("uTexel", 1.0F / sw, 1.0F / sh);
         this.downP.set1f("uFromMask", fromMask ? 1.0F : 0.0F);
         target.bind();
         bindTexture(0, source);
         quad();
      }
   }

   private int coverage(int k) {
      return k == 0 && this.maskScale == 1 ? this.mask.texture : this.levels[k].texture;
   }

   private GlTarget level(int k) {
      if (this.levels[k] == null) {
         this.levels[k] = this.target(Math.max(1, this.screenW + (1 << k) - 1 >> k), Math.max(1, this.screenH + (1 << k) - 1 >> k), true);
      }

      return this.levels[k];
   }

   private void measureShape() {
      if (this.shapeTotal == null) {
         this.shapeA = GlTarget.create(16, 8, 34836, 6408, false, false);
         this.shapeB = GlTarget.create(16, 8, 34836, 6408, false, false);
         this.shapeTotal = GlTarget.create(2, 1, 34836, 6408, false, false);
         if (this.shapeA == null || this.shapeB == null || this.shapeTotal == null) {
            GlTarget.delete(this.shapeA);
            GlTarget.delete(this.shapeB);
            GlTarget.delete(this.shapeTotal);
            this.shapeTotal = null;
            this.shapeB = null;
            this.shapeA = null;
            this.shapeFailed = true;
            return;
         }
      }

      GlTarget source = this.levels[5];
      float bw = (float)Math.ceil(source.width / 16.0F);
      float bh = (float)Math.ceil(source.height / 8.0F);
      this.momentsP.use();
      this.momentsP.set2f("uLevelSize", source.width, source.height);
      this.momentsP.set2f("uBlock", Math.min(16.0F, bw), Math.min(16.0F, bh));
      bindTexture(0, source.texture);
      this.momentsP.set1f("uSecond", 0.0F);
      this.shapeA.bind();
      quad();
      this.momentsP.set1f("uSecond", 1.0F);
      this.shapeB.bind();
      quad();
      this.totalP.use();
      this.totalP.set2f("uSize", 16.0F, 8.0F);
      this.shapeTotal.bind();
      bindTexture(0, this.shapeA.texture);
      bindTexture(1, this.shapeB.texture);
      quad();
      this.hasShape = true;
   }

   private void distanceField(int flag, float reachPx) {
      float radius = Math.min(32.0F, (float)Math.ceil(reachPx * this.maskScale));
      int near = this.levels[flag] != null ? this.levels[flag].texture : this.coverage(flag);
      this.rowsP.use();
      this.rowsP.set2f("uTexel", 1.0F / this.mask.width, 1.0F / this.mask.height);
      this.rowsP.set1f("uRadius", radius);
      this.rows.bind();
      bindTexture(0, this.mask.texture);
      bindTexture(1, near);
      quad();
      this.colsP.use();
      this.colsP.set2f("uTexel", 1.0F / this.mask.width, 1.0F / this.mask.height);
      this.colsP.set1f("uRadius", radius);
      this.dist.bind();
      bindTexture(0, this.rows.texture);
      bindTexture(1, near);
      quad();
   }

   private int blurHalo(int k, float sigmaPx) {
      int w = k == 0 && this.maskScale == 1 ? this.screenW : this.level(k).width;
      int h = k == 0 && this.maskScale == 1 ? this.screenH : this.level(k).height;
      if (this.haloLevel != k || this.haloA == null || this.haloA.width != w || this.haloA.height != h) {
         GlTarget.delete(this.haloA);
         GlTarget.delete(this.haloB);
         this.haloA = this.target(w, h, false);
         this.haloB = this.target(w, h, false);
         this.haloLevel = k;
      }

      float sigma = Math.max(0.3F, sigmaPx / (1 << k));
      int pairs = Math.min(16, (int)Math.ceil(sigma * 1.25F));
      this.blurP.use();
      this.blurP.set1f("uSigma", sigma);
      this.blurP.set1i("uPairs", pairs);
      this.blurP.set2f("uStep", 1.0F / w, 0.0F);
      this.haloA.bind();
      bindTexture(0, this.coverage(k));
      quad();
      this.blurP.set2f("uStep", 0.0F, 1.0F / h);
      this.haloB.bind();
      bindTexture(0, this.haloA.texture);
      quad();
      return this.haloB.texture;
   }

   private void updateTrail(GlowParams p, float dt, int k, int haloTexture) {
      int w = k == 0 && this.maskScale == 1 ? this.screenW : this.level(k).width;
      int h = k == 0 && this.maskScale == 1 ? this.screenH : this.level(k).height;
      if (this.trailLevel != k || this.trailA == null || this.trailA.width != w || this.trailA.height != h) {
         GlTarget.delete(this.trailA);
         GlTarget.delete(this.trailB);
         this.trailA = this.target(w, h, true);
         this.trailB = this.target(w, h, true);
         this.trailLevel = k;
         this.clearTrailTargets();
      }

      boolean capture = p.trailCapture && haloTexture >= 0;
      GlTarget from = this.trailOnA ? this.trailA : this.trailB;
      GlTarget to = this.trailOnA ? this.trailB : this.trailA;
      float tau = Math.max(0.01F, p.trailSeconds / 3.0F);
      float spread = 0.0F;
      float grow = 0.0F;
      if (!p.trailEcho && p.trailSpread > 0.0F) {
         this.smokeClock += dt;
         int steps = Math.min(3, (int)(this.smokeClock * 60.0F));
         this.smokeClock = Math.min(0.016666668F, this.smokeClock - steps / 60.0F);
         if (steps > 0) {
            float stepPx = 5.0F * p.trailSpread * (this.screenH / 1080.0F);
            spread = stepPx / (1 << k) * (float)Math.sqrt(steps);
            grow = Math.min(1.0F, 0.45F * p.trailSpread * steps);
         }
      }

      this.trailP.use();
      this.trailP.set2f("uTexel", 1.0F / w, 1.0F / h);
      this.trailP.set1f("uFade", (float)Math.exp(-dt / tau));
      this.trailP.set1f("uCapture", capture ? 1.0F : 0.0F);
      this.trailP.set1f("uSpread", spread);
      this.trailP.set1f("uGrow", grow);
      this.trailP.set1f("uEcho", p.trailEcho ? 1.0F : 0.0F);
      this.trailP.set2f("uMotion", p.trailEcho ? 0.0F : this.motionX / this.screenW, p.trailEcho ? 0.0F : this.motionY / this.screenH);
      to.bind();
      bindTexture(0, from.texture);
      bindTexture(1, capture ? this.coverage(k) : from.texture);
      bindTexture(2, capture ? haloTexture : from.texture);
      quad();
      this.trailOnA = !this.trailOnA;
      this.trailClean = false;
   }

   void clearTrail() {
      if (!this.trailClean && this.trailA != null) {
         GlowRenderer.State saved = this.utilState.save(false);

         try {
            this.clearTrailTargets();
         } catch (RuntimeException | LinkageError var6) {
            this.fail("clearing the trail failed", var6);
         } finally {
            saved.restore();
         }
      } else {
         this.trailClean = true;
      }
   }

   private void clearTrailTargets() {
      ((Buffer)FLOATS).clear();
      GL11.glGetFloat(3106, FLOATS);
      float r = FLOATS.get(0);
      float g = FLOATS.get(1);
      float b = FLOATS.get(2);
      float a = FLOATS.get(3);
      GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.trailA.bind();
      GlStateManager.clear(16384);
      this.trailB.bind();
      GlStateManager.clear(16384);
      GlStateManager.clearColor(r, g, b, a);
      this.trailClean = true;
      this.smokeClock = 0.0F;
   }

   boolean isTrailClean() {
      return this.trailClean;
   }

   private void tint(GlowParams p) {
      this.tintP.use();
      this.common(this.tintP, p);
      this.tintP.set1f("uTint", p.itemTint);
      this.tintP.set1f("uOpacity", p.opacity);
      bindTexture(0, this.mask.texture);
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(774, 771, 0, 1);
      quad();
      GlStateManager.disableBlend();
   }

   private void composite(GlowParams p, boolean filled, int haloTexture, boolean trailOn) {
      GlowRenderer.Program c = this.compositeP;
      c.use();
      this.common(c, p);
      c.set1f("uHasMask", filled ? 1.0F : 0.0F);
      c.set1f("uMaskScale", this.maskScale);
      c.set1f("uPx", this.screenH / 1080.0F);
      c.set4f("uRim", p.rimWidth, p.rimSoftness, p.rimIntensity, p.hotCore);
      c.set3f("uHaloP", p.haloIntensity, p.haloFalloff, Math.max(0.05F, p.haloCurve));
      c.set2f("uHaloSize", p.haloRadius, p.haloRadius * 0.5F);
      c.set1f("uHaloShape", p.haloRadius > 0.1F ? 1.0F : 0.0F);
      float rimReach = p.rimWidth * (1.0F + 1.2F * p.rimSoftness) + 2.0F;
      c.set1f("uHaloCovers", filled && p.haloRadius > 0.1F && p.haloRadius >= rimReach ? 1.0F : 0.0F);
      c.set1f("uReach", Math.max(1.0F, p.gradientReach));
      c.set2f("uInside", p.innerGlow, p.itemTint);
      c.set3f("uTrailP", p.trailIntensity, trailOn ? 1.0F : 0.0F, p.trailColorByAge ? 1.0F : 0.0F);
      c.set1f("uEcho", p.trailEcho ? 1.0F : 0.0F);
      c.set4f("uFlow", p.flowAmount, Math.max(1.0F, p.flowCell), p.flowTravel, p.flowDirection);
      c.set4f("uShimmer", p.shimmerPos, Math.max(0.01F, p.shimmerWidth), p.shimmerStrength, 0.0F);
      c.set3f("uLook", p.gain, p.flash, p.opacity);
      c.set1i("uBlend", p.blend);
      GlTarget trail = this.trailOnA ? this.trailA : this.trailB;
      bindTexture(0, this.mask.texture);
      bindTexture(1, filled && p.rimWidth > 0.0F ? this.dist.texture : this.noise());
      bindTexture(2, filled ? haloTexture : this.mask.texture);
      bindTexture(3, trailOn && trail != null ? trail.texture : this.noise());
      bindTexture(5, this.noise());
      if (filled && p.rimWidth <= 0.0F) {
         c.set4f("uRim", 0.0F, p.rimSoftness, 0.0F, 0.0F);
      }

      if (!trailOn) {
         c.set3f("uTrailP", 0.0F, 0.0F, 0.0F);
      }

      GlStateManager.enableBlend();
      if (p.blend == 1) {
         GlStateManager.tryBlendFuncSeparate(1, 769, 0, 1);
      } else {
         GlStateManager.tryBlendFuncSeparate(1, 771, 0, 1);
      }

      quad();
      GlStateManager.disableBlend();
   }

   private void common(GlowRenderer.Program prog, GlowParams p) {
      prog.set2f("uScreen", this.screenW, this.screenH);
      prog.set2f("uCenter", this.centerX, this.centerY);
      prog.set1f("uHasShape", this.hasShape ? 1.0F : 0.0F);
      prog.set1i("uDirection", p.direction);
      prog.set1f("uMirror", p.mirror ? 1.0F : 0.0F);
      bindTexture(4, this.lutTexture);
      bindTexture(6, this.hasShape ? this.shapeTotal.texture : this.lutTexture);
   }

   private void uploadLut(GlowParams p) {
      if (this.lutTexture < 0) {
         this.lutTexture = TextureUtil.glGenTextures();
         GlStateManager.bindTexture(this.lutTexture);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glTexImage2D(3553, 0, 32856, 64, 2, 0, 6408, 5121, (ByteBuffer)null);
      }

      ((Buffer)LUT_BYTES).clear();
      put(p.glowColors);
      put(p.rimColors);
      ((Buffer)LUT_BYTES).flip();
      GlStateManager.bindTexture(this.lutTexture);
      GL11.glTexSubImage2D(3553, 0, 0, 0, 64, 2, 6408, 5121, LUT_BYTES);
   }

   private static void put(int[] colors) {
      for (int c : colors) {
         LUT_BYTES.put((byte)(c >> 16)).put((byte)(c >> 8)).put((byte)c).put((byte)(c >>> 24));
      }
   }

   private int noise() {
      if (this.noiseTexture >= 0) {
         return this.noiseTexture;
      } else {
         int n = 128;
         ByteBuffer data = BufferUtils.createByteBuffer(65536);
         int x = 625341585;

         for (int i = 0; i < 16384; i++) {
            int var5 = x ^ x << 13;
            int var6 = var5 ^ var5 >>> 17;
            x = var6 ^ var6 << 5;
            data.put((byte)x).put((byte)(x >>> 8)).put((byte)0).put((byte)-1);
         }

         ((Buffer)data).flip();
         this.noiseTexture = TextureUtil.glGenTextures();
         GlStateManager.bindTexture(this.noiseTexture);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 10497);
         GL11.glTexParameteri(3553, 10243, 10497);
         GL11.glTexImage2D(3553, 0, 32856, 128, 128, 0, 6408, 5121, data);
         return this.noiseTexture;
      }
   }

   private static void bindTexture(int unit, int texture) {
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
      GlStateManager.bindTexture(texture);
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
   }

   private static void quad() {
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_TEX);
      wr.pos(0.0, 0.0, 0.0).tex(0.0, 0.0).endVertex();
      wr.pos(1.0, 0.0, 0.0).tex(1.0, 0.0).endVertex();
      wr.pos(1.0, 1.0, 0.0).tex(1.0, 1.0).endVertex();
      wr.pos(0.0, 1.0, 0.0).tex(0.0, 1.0).endVertex();
      tess.draw();
   }

   private boolean prepare(int w, int h, int quality) {
      if (w >= 16 && h >= 16) {
         if (!this.compiled || this.filesGeneration != AssetSource.generation()) {
            if (this.compiled) {
               this.deletePrograms();
               this.broken = false;
               this.brokenReason = null;
            }

            this.compile();
         }

         if (this.broken) {
            return false;
         } else {
            int scale = quality == 2 && (long)w * h * 4L <= 16777216L && 2 * Math.max(w, h) <= this.maxTexture ? 2 : 1;
            if (this.mask != null && w == this.screenW && h == this.screenH && scale == this.maskScale) {
               return true;
            } else {
               GlowRenderer.State saved = this.utilState.save(false);

               boolean var10;
               try {
                  this.deleteTargets();
                  this.screenW = w;
                  this.screenH = h;
                  this.maskScale = scale;
                  int mw = w * scale;
                  int mh = h * scale;
                  this.mask = GlTarget.create(mw, mh, this.fmtR, this.pixR, true, true);
                  this.rows = this.target(mw, mh, true);
                  this.dist = this.target(mw, mh, true);
                  if (this.mask != null) {
                     return true;
                  }

                  this.fail("the mask framebuffer is incomplete", null);
                  boolean bl = false;
                  var10 = bl;
               } catch (RuntimeException | LinkageError var14) {
                  this.fail("making the framebuffers failed", var14);
                  return false;
               } finally {
                  saved.restore();
               }

               return var10;
            }
         }
      } else {
         return false;
      }
   }

   private GlTarget target(int w, int h, boolean twoChannels) {
      GlTarget t = GlTarget.create(w, h, twoChannels ? this.fmtRG : this.fmtR, twoChannels ? this.pixRG : this.pixR, true, false);
      if (t == null) {
         throw new IllegalStateException("framebuffer " + w + "x" + h + " is incomplete");
      } else {
         return t;
      }
   }

   private void deletePrograms() {
      for (GlowRenderer.Program p : new GlowRenderer.Program[]{
         this.maskP, this.downP, this.blurP, this.rowsP, this.colsP, this.trailP, this.compositeP, this.tintP, this.momentsP, this.totalP
      }) {
         if (p != null) {
            GL20.glDeleteProgram(p.id);
         }
      }

      this.totalP = null;
      this.momentsP = null;
      this.tintP = null;
      this.compositeP = null;
      this.trailP = null;
      this.colsP = null;
      this.rowsP = null;
      this.blurP = null;
      this.downP = null;
      this.maskP = null;
   }

   private void compile() {
      this.compiled = true;
      this.filesGeneration = AssetSource.generation();
      if (!AssetSource.ready()) {
         this.broken = true;
         this.brokenReason = "its files aren't here yet (the visuals' pack)";
      } else {
         int previous = GL11.glGetInteger(35725);

         try {
            ContextCapabilities caps = GLContext.getCapabilities();
            this.rg = caps.OpenGL30;
            this.fmtR = this.rg ? '舩' : '聘';
            this.pixR = this.rg ? 6403 : 6408;
            this.fmtRG = this.rg ? '舫' : '聘';
            this.pixRG = this.rg ? '舧' : 6408;
            this.maxTexture = GL11.glGetInteger(3379);
            String common = read("common.glsl");
            String pass = read("pass.vsh");
            this.maskP = GlowRenderer.Program.link("mask", read("mask.vsh"), read("mask.fsh"), "uTex");
            this.downP = GlowRenderer.Program.link("down", pass, read("down.fsh"), "uTex");
            this.blurP = GlowRenderer.Program.link("blur", pass, read("blur.fsh"), "uTex");
            this.rowsP = GlowRenderer.Program.link("edt_h", pass, read("edt_h.fsh"), "uMask", "uNear");
            this.colsP = GlowRenderer.Program.link("edt_v", pass, read("edt_v.fsh"), "uRows", "uNear");
            this.trailP = GlowRenderer.Program.link("trail", pass, read("trail.fsh"), "uPrev", "uCover", "uHalo");
            this.compositeP = GlowRenderer.Program.link(
               "composite", pass, common + read("composite.fsh"), "sMask", "sDist", "sHalo", "sTrail", "sLut", "sNoise", "sShape"
            );
            this.tintP = GlowRenderer.Program.link("tint", pass, common + read("tint.fsh"), "sMask", null, null, null, "sLut", null, "sShape");
            if (this.rg) {
               this.momentsP = GlowRenderer.Program.link("moments", pass, read("moments.fsh"), "uTex");
               this.totalP = GlowRenderer.Program.link("total", pass, read("total.fsh"), "uA", "uB");
            } else {
               this.shapeFailed = true;
            }
         } catch (RuntimeException | LinkageError var8) {
            this.fail(var8.getMessage() == null ? "the shaders did not compile" : var8.getMessage(), var8);
         } finally {
            GL20.glUseProgram(previous);
         }
      }
   }

   private static String read(String name) {
      return ShaderFiles.require("glow/" + name);
   }

   private void deleteTargets() {
      GlTarget.delete(this.mask);
      GlTarget.delete(this.rows);
      GlTarget.delete(this.dist);

      for (int k = 0; k < 7; k++) {
         GlTarget.delete(this.levels[k]);
         this.levels[k] = null;
      }

      GlTarget.delete(this.haloA);
      GlTarget.delete(this.haloB);
      GlTarget.delete(this.trailA);
      GlTarget.delete(this.trailB);
      GlTarget.delete(this.shapeA);
      GlTarget.delete(this.shapeB);
      GlTarget.delete(this.shapeTotal);
      this.shapeTotal = null;
      this.shapeB = null;
      this.shapeA = null;
      this.trailB = null;
      this.trailA = null;
      this.haloB = null;
      this.haloA = null;
      this.dist = null;
      this.rows = null;
      this.mask = null;
      this.hasShape = false;
      this.trailLevel = -1;
      this.haloLevel = -1;
      this.trailClean = true;
      this.maskDrawn = false;
   }

   void release() {
      if (this.mask != null || this.lutTexture >= 0 || this.noiseTexture >= 0) {
         GlowRenderer.State saved = this.utilState.save(false);

         try {
            this.deleteTargets();
            if (this.lutTexture >= 0) {
               TextureUtil.deleteTexture(this.lutTexture);
               this.lutTexture = -1;
            }

            if (this.noiseTexture >= 0) {
               TextureUtil.deleteTexture(this.noiseTexture);
               this.noiseTexture = -1;
            }
         } catch (RuntimeException | LinkageError var6) {
            FuriahsVisuals.LOG.warn("Freeing the glow's framebuffers failed", var6);
         } finally {
            saved.restore();
         }
      }
   }

   private void fail(String reason, Throwable e) {
      if (!this.broken) {
         this.broken = true;
         this.brokenReason = reason;
         if (e != null) {
            FuriahsVisuals.LOG.warn("Glow switched off ({})", new Object[]{reason, e});
         } else {
            FuriahsVisuals.LOG.warn("Glow switched off ({})", new Object[]{reason});
         }
      }

      this.maskDrawn = false;
   }

   void setMeasuring(boolean on) {
      if (on && !this.measuring) {
         this.maskTimer.reset();
         this.glowTimer.reset();
      }

      this.measuring = on && !this.profiling;
   }

   void setProfiling(boolean on) {
      this.profiling = on;
      if (on) {
         this.measuring = false;

         for (int i = 0; i < PARTS.length; i++) {
            if (this.partTimers[i] == null) {
               this.partTimers[i] = new GpuTimer();
            }

            this.partTimers[i].reset();
         }
      }
   }

   String partReport() {
      StringBuilder sb = new StringBuilder();
      float total = 0.0F;

      for (int i = 0; i < PARTS.length; i++) {
         float ms = this.partTimers[i] == null ? 0.0F : this.partTimers[i].millis();
         total += ms;
         sb.append(PARTS[i]).append(String.format(Locale.ROOT, " %.3f  ", ms));
      }

      return sb.append(String.format(Locale.ROOT, "= %.3f ms GPU", total)).toString();
   }

   private void partBegin(int i) {
      if (this.profiling) {
         this.partTimers[i].begin();
      }
   }

   private void partEnd(int i) {
      if (this.profiling) {
         this.partTimers[i].end();
      }
   }

   float gpuMillis() {
      return this.maskTimer.millis() + this.glowTimer.millis();
   }

   float cpuMillis() {
      return this.cpuMillis;
   }

   int maskScale() {
      return this.maskScale;
   }

   String dump(File dir) {
      StringBuilder sb = new StringBuilder();
      GlTarget trail = this.trailOnA ? this.trailA : this.trailB;
      GlTarget[] targets = new GlTarget[]{this.mask, this.dist, this.haloB, this.levels[1], this.levels[3], trail};
      String[] names = new String[]{"mask", "dist", "halo", "level1", "level3", "trail"};
      int bound = GL11.glGetInteger(32873);

      for (int i = 0; i < targets.length; i++) {
         GlTarget t = targets[i];
         if (t == null) {
            sb.append(names[i]).append(": none  ");
         } else {
            ByteBuffer buf = BufferUtils.createByteBuffer(t.width * t.height * 4);
            GlStateManager.bindTexture(t.texture);
            GL11.glGetTexImage(3553, 0, 6408, 5121, buf);
            BufferedImage img = new BufferedImage(t.width, t.height, 1);
            int nonZero = 0;
            int maxR = 0;

            for (int y = 0; y < t.height; y++) {
               for (int x = 0; x < t.width; x++) {
                  int o = (y * t.width + x) * 4;
                  int r = buf.get(o) & 255;
                  int g = buf.get(o + 1) & 255;
                  int b = buf.get(o + 2) & 255;
                  if (r > 0) {
                     nonZero++;
                  }

                  maxR = Math.max(maxR, r);
                  img.setRGB(x, t.height - 1 - y, r << 16 | g << 8 | b);
               }
            }

            try {
               ImageIO.write(img, "png", new File(dir, "glowdump_" + names[i] + ".png"));
            } catch (IOException var19) {
               FuriahsVisuals.LOG.warn("dump failed", var19);
            }

            sb.append(names[i])
               .append(' ')
               .append(t.width)
               .append('x')
               .append(t.height)
               .append(": ")
               .append(nonZero)
               .append(" px with r>0, max r ")
               .append(maxR)
               .append("  ");
         }
      }

      sb.append("projected center ").append((int)this.centerX).append(',').append((int)this.centerY).append(" filled ").append(this.maskFilled);
      if (this.hasShape && this.shapeTotal != null) {
         FloatBuffer f = BufferUtils.createFloatBuffer(8);
         GlStateManager.bindTexture(this.shapeTotal.texture);
         GL11.glGetTexImage(3553, 0, 6408, 5126, f);
         float m = f.get(0);
         sb.append(String.format(Locale.ROOT, ", measured center %.0f,%.0f (coverage %.1f)", f.get(1) / m * this.screenW, f.get(2) / m * this.screenH, m));
      }

      GlStateManager.bindTexture(bound);
      return sb.toString();
   }

   int screenWidth() {
      return this.screenW;
   }

   int screenHeight() {
      return this.screenH;
   }

   private static final class Program {
      final int id;
      private final Map<String, Integer> uniforms = new HashMap<>();

      private Program(int id) {
         this.id = id;
      }

      static GlowRenderer.Program link(String name, String vertex, String fragment, String... samplers) {
         int vs = compile(name, 35633, vertex);
         int fs = compile(name, 35632, fragment);
         int p = GL20.glCreateProgram();
         GL20.glAttachShader(p, vs);
         GL20.glAttachShader(p, fs);
         GL20.glLinkProgram(p);
         GL20.glDeleteShader(vs);
         GL20.glDeleteShader(fs);
         if (GL20.glGetProgrami(p, 35714) == 0) {
            FuriahsVisuals.LOG.warn("Glow shader '{}' failed to link: {}", new Object[]{name, GL20.glGetProgramInfoLog(p, 4096)});
            GL20.glDeleteProgram(p);
            throw new IllegalStateException("shader " + name + " did not link");
         } else {
            GlowRenderer.Program program = new GlowRenderer.Program(p);
            GL20.glUseProgram(p);

            for (int i = 0; i < samplers.length; i++) {
               if (samplers[i] != null) {
                  GL20.glUniform1i(program.u(samplers[i]), i);
               }
            }

            return program;
         }
      }

      private static int compile(String name, int type, String source) {
         int s = GL20.glCreateShader(type);
         GL20.glShaderSource(s, source);
         GL20.glCompileShader(s);
         if (GL20.glGetShaderi(s, 35713) == 0) {
            FuriahsVisuals.LOG.warn("Glow shader '{}' failed to compile: {}", new Object[]{name, GL20.glGetShaderInfoLog(s, 4096)});
            GL20.glDeleteShader(s);
            throw new IllegalStateException("shader " + name + " did not compile");
         } else {
            return s;
         }
      }

      int u(String name) {
         Integer loc = this.uniforms.get(name);
         if (loc == null) {
            loc = GL20.glGetUniformLocation(this.id, name);
            this.uniforms.put(name, loc);
         }

         return loc;
      }

      void use() {
         GL20.glUseProgram(this.id);
      }

      void set1i(String name, int v) {
         GL20.glUniform1i(this.u(name), v);
      }

      void set1f(String name, float v) {
         GL20.glUniform1f(this.u(name), v);
      }

      void set2f(String name, float a, float b) {
         GL20.glUniform2f(this.u(name), a, b);
      }

      void set3f(String name, float a, float b, float c) {
         GL20.glUniform3f(this.u(name), a, b, c);
      }

      void set4f(String name, float a, float b, float c, float d) {
         GL20.glUniform4f(this.u(name), a, b, c, d);
      }
   }

   private static final class State {
      private static final int UNITS = 7;
      private boolean gl30;
      private int drawFb;
      private int readFb;
      private int program;
      private int activeUnit;
      private boolean scissor;
      private final int[] viewport = new int[4];
      private final int[] bindings = new int[7];
      private boolean withTextures;

      private State() {
      }

      GlowRenderer.State save(boolean textures) {
         GlowRenderer.State s = this;
         this.gl30 = GLContext.getCapabilities().OpenGL30;
         this.drawFb = GL11.glGetInteger(36006);
         this.readFb = this.gl30 ? GL11.glGetInteger(36010) : this.drawFb;
         this.program = GL11.glGetInteger(35725);
         this.activeUnit = GL11.glGetInteger(34016);
         this.scissor = GL11.glIsEnabled(3089);
         ((Buffer)GlowRenderer.INTS).clear();
         GL11.glGetInteger(2978, GlowRenderer.INTS);

         for (int i = 0; i < 4; i++) {
            s.viewport[i] = GlowRenderer.INTS.get(i);
         }

         s.withTextures = textures;
         if (textures) {
            for (int var4 = 0; var4 < 7; var4++) {
               GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + var4);
               s.bindings[var4] = GL11.glGetInteger(32873);
            }

            GlStateManager.setActiveTexture(s.activeUnit);
         }

         if (s.scissor) {
            GL11.glDisable(3089);
         }

         return s;
      }

      void bindTarget() {
         this.bindFramebuffers();
         GlStateManager.viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
      }

      void restore() {
         this.bindFramebuffers();
         GlStateManager.viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
         GL20.glUseProgram(this.program);
         if (this.withTextures) {
            for (int i = 6; i >= 0; i--) {
               GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + i);
               GlStateManager.bindTexture(this.bindings[i]);
            }

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         }

         GlStateManager.setActiveTexture(this.activeUnit);
         if (this.scissor) {
            GL11.glEnable(3089);
         }
      }

      private void bindFramebuffers() {
         if (this.gl30 && this.readFb != this.drawFb) {
            GL30.glBindFramebuffer(36008, this.readFb);
            GL30.glBindFramebuffer(36009, this.drawFb);
         } else {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.drawFb);
         }
      }
   }
}
