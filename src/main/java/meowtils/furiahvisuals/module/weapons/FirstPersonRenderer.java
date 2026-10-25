package meowtils.furiahvisuals.module.weapons;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.GlSave;
import meowtils.furiahvisuals.render.GlTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class FirstPersonRenderer {
   static final float REFERENCE_ASPECT = 1.7777778F;
   private static final float NEAR = 0.02F;
   private static final float FAR = 8.0F;
   private final WeaponRenderer renderer;
   private final ArmRenderer arms = new ArmRenderer();
   private final SsaaBuffer ssaa = new SsaaBuffer();
   final WeaponLight light = new WeaponLight();
   final WeaponFx fx = new WeaponFx();
   private final GlSave save = new GlSave();
   private final Mat4 projection = new Mat4();
   private final Mat4 projRight = new Mat4();
   private final Mat4 projLeft = new Mat4();
   private final Mat4 drawProj = new Mat4();
   private final FirstPersonRenderer.Item[] items = new FirstPersonRenderer.Item[12];
   private int count;
   private final float[] corner = new float[4];
   private long lastNanos;
   private boolean failed;
   private String failure;
   float mosaic;
   boolean pixelDrawn;
   private GlTarget pixelFx;
   private int pixelPx = 8;
   private int fxW;
   private int fxH;
   private long pixelFxSmallSince;
   private final Mat4 camera = new Mat4();
   private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
   private static final Mat4 RECT = new Mat4();
   private static final Mat4 TMP = new Mat4();

   FirstPersonRenderer(WeaponRenderer renderer) {
      this.renderer = renderer;

      for (int i = 0; i < this.items.length; i++) {
         this.items[i] = new FirstPersonRenderer.Item();
      }
   }

   void clear() {
      this.count = 0;
      this.mosaic = 0.0F;
      this.pixelDrawn = false;
      this.fx.drawSide = 0;
   }

   void studioStyle(boolean pixel) {
      for (int i = 0; i < this.count; i++) {
         if (!this.items[i].arm) {
            this.items[i].look.pixel = pixel || this.items[i].assets != null && this.items[i].assets.pixelImport();
         }
      }

      this.fx.drawSide = pixel ? 2 : 1;
   }

   FirstPersonRenderer.Item addWeapon(WeaponAssets a, boolean leftAnchored, boolean mirrored) {
      FirstPersonRenderer.Item it = this.next();
      if (it == null) {
         return null;
      } else {
         it.arm = false;
         it.ghost = false;
         it.assets = a;
         it.left = leftAnchored;
         it.mirrored = mirrored;
         float[] b = a.def.bounds;
         WeaponMesh m = a.mesh(0);
         if (m != null) {
            System.arraycopy(m.min, 0, it.bounds, 0, 3);
            System.arraycopy(m.max, 0, it.bounds, 3, 3);
         } else {
            System.arraycopy(b, 0, it.bounds, 0, 6);
         }

         return it;
      }
   }

   FirstPersonRenderer.Item addArm(boolean slim, boolean right, boolean sleeve, boolean leftAnchored, boolean mirrored) {
      FirstPersonRenderer.Item it = this.next();
      if (it == null) {
         return null;
      } else {
         it.arm = true;
         it.ghost = false;
         it.assets = null;
         it.slim = slim;
         it.rightArm = right;
         it.sleeve = sleeve;
         it.left = leftAnchored;
         it.mirrored = mirrored;
         float x0 = right ? (slim ? -2.0F : -3.0F) : -1.0F;
         float x1 = right ? 1.0F : (slim ? 2.0F : 3.0F);
         it.bounds[0] = x0 - 0.3F;
         it.bounds[1] = -30.3F;
         it.bounds[2] = -2.3F;
         it.bounds[3] = x1 + 0.3F;
         it.bounds[4] = 10.3F;
         it.bounds[5] = 2.3F;
         return it;
      }
   }

   private FirstPersonRenderer.Item next() {
      return this.count < this.items.length ? this.items[this.count++] : null;
   }

   String failure() {
      return this.failure;
   }

   void draw(Minecraft mc, float pt, float fov, WeaponOptions.AntiAliasing aa, boolean mask, float time, long now) {
      float dt = this.lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastNanos) / 1.0E9F);
      if (!mask) {
         this.lastNanos = now;
      }

      this.renderer.setTime(time);
      WeaponAssets.pump();
      int w = mc.displayWidth;
      int h = mc.displayHeight;
      float aspect = (float)w / Math.max(1, h);
      this.projection.perspective(fov, aspect, 0.02F, 8.0F);
      float shift = 1.0F - 1.7777778F / aspect;
      this.projRight.set(this.projection).screenAdjust(shift, 0.0F, 1.0F, 1.0F);
      this.projLeft.set(this.projection).screenAdjust(-shift, 0.0F, 1.0F, 1.0F);
      if (!mask) {
         this.light.updateFirstPerson(mc, pt, dt);
         this.fx.update(dt);
      }

      boolean simple = this.failed || WeaponsModule.simpleShading() || !this.renderer.shaders().supported() || this.renderer.shaders().weapon() == null;
      GlStateManager.matrixMode(5889);
      GlStateManager.pushMatrix();
      GlStateManager.matrixMode(5888);
      GlStateManager.pushMatrix();
      this.save.save();

      try {
         if (mask) {
            this.drawMask(mc);
         } else if (simple) {
            this.drawSimple(mc);
         } else {
            this.drawShaded(mc, w, h, aa, time);
         }
      } catch (RuntimeException | LinkageError var20) {
         if (!this.failed) {
            this.failed = true;
            this.failure = "The weapons' shader failed on this graphics driver (" + var20 + "); they are drawn the simple way.";
            FuriahsVisuals.LOG.error("The first-person weapon renderer failed; switching to the simple path", var20);
         }
      } finally {
         this.save.restore();
         GlStateManager.matrixMode(5889);
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.popMatrix();
      }
   }

   private void load(FirstPersonRenderer.Item it, Mat4 rect, Mat4 camera) {
      this.drawProj.set(it.left ? this.projLeft : this.projRight);
      if (rect != null) {
         this.drawProj.premul(rect);
      }

      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.multMatrix(this.drawProj.buffer());
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.multMatrix(camera.buffer());
      GlStateManager.multMatrix(it.matrix.buffer());
   }

   private void captureCamera() {
      ((Buffer)MATRIX).clear();
      GL11.glGetFloat(2982, MATRIX);
      MATRIX.get(this.camera.m);
   }

   private void drawShaded(Minecraft mc, int w, int h, WeaponOptions.AntiAliasing aa, float time) {
      this.captureCamera();
      int[] r = this.screenRect(w, h, 3);
      boolean buffered = false;
      Mat4 rect = null;
      boolean useBuffer = aa != WeaponOptions.AntiAliasing.OFF || this.mosaic > 0.0F;
      int factor = aa == WeaponOptions.AntiAliasing.OFF ? 2 : aa.factor();
      boolean pixelFx = this.pixelDrawn
         && this.fx.hasAnything()
         && this.renderer.module != null
         && this.renderer.module.pixelEffects.get()
         && this.renderer.shaders().pixelFx() != null;
      if (r != null && useBuffer && this.ssaa.begin(r[0], r[1], r[2], r[3], factor)) {
         buffered = true;
         float x0 = (float)r[0] / w * 2.0F - 1.0F;
         float x1 = (float)(r[0] + r[2]) / w * 2.0F - 1.0F;
         float y0 = (float)r[1] / h * 2.0F - 1.0F;
         float y1 = (float)(r[1] + r[3]) / h * 2.0F - 1.0F;
         rect = RECT.identity().screenAdjust(-(x0 + x1) / 2.0F, -(y0 + y1) / 2.0F, 2.0F / (x1 - x0), 2.0F / (y1 - y0));
      } else if (r == null) {
         return;
      }

      if (!buffered) {
         this.drawShimmer();
      }

      GlStateManager.enableDepth();
      GlStateManager.depthFunc(515);
      GlStateManager.depthMask(true);
      GlStateManager.disableCull();
      GlStateManager.disableAlpha();
      GlStateManager.disableLighting();
      GlStateManager.shadeModel(7425);
      if (buffered) {
         GlStateManager.disableBlend();
      } else {
         GlStateManager.clear(256);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
      }

      if (!this.renderer.beginShaded(this.light, false, time)) {
         throw new IllegalStateException("no weapon shader");
      } else {
         this.renderer.setBaseBlend(!buffered);
         this.renderer.setClearStrings(true);
         WeaponShader.Program p = this.renderer.program();
         EntityPlayerSP player = mc.thePlayer;

         for (int i = 0; i < this.count; i++) {
            FirstPersonRenderer.Item it = this.items[i];
            this.load(it, rect, this.camera);
            if (it.arm) {
               p.set1f("uMat", 30.0F);
               p.set1f("uAlpha", 1.0F);
               mc.getTextureManager().bindTexture(player.getLocationSkin());
               if (it.mirrored) {
                  GL11.glFrontFace(2304);
               }

               this.arms.draw(it.slim, it.rightArm, it.sleeve);
               if (it.mirrored) {
                  GL11.glFrontFace(2305);
               }
            } else if (it.ghost) {
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
               GlStateManager.depthMask(false);
               this.renderer.draw(it.assets, 1, it.look, WeaponRenderer.Mode.SHADED, it.mirrored);
               GlStateManager.depthMask(true);
               if (buffered) {
                  GlStateManager.disableBlend();
               }
            } else {
               this.renderer.draw(it.assets, 0, it.look, WeaponRenderer.Mode.SHADED, it.mirrored);
            }
         }

         this.renderer.setClearStrings(false);
         this.renderer.endShaded();
         if (!pixelFx) {
            this.drawFx(rect);
         }

         if (buffered) {
            this.save.bindTarget();
            this.drawShimmer();
            WeaponShader.Program c = this.renderer.shaders().composite();
            if (c != null) {
               GlStateManager.matrixMode(5889);
               GlStateManager.loadIdentity();
               GlStateManager.matrixMode(5888);
               GlStateManager.loadIdentity();
               int[] vp = this.save.viewport();
               float block = this.mosaic > 0.0F ? 1.0F + this.mosaic * Math.max(this.pixelPx * 1.6F, h / 40.0F) : 0.0F;
               this.ssaa.composite(c, vp[2], vp[3], -1, 0.0F, block);
            }
         }

         if (pixelFx) {
            this.drawPixelFx(mc, w, h);
         }
      }
   }

   private void drawPixelFx(Minecraft mc, int w, int h) {
      int k = this.pixelPx = this.weaponPixelOnScreen(w, h);
      int[] r = this.screenRect(w, h, 3);
      if (r != null) {
         int lw = Math.max(1, (r[2] + k - 1) / k);
         int lh = Math.max(1, (r[3] + k - 1) / k);
         long now = System.nanoTime();
         if (this.pixelFx != null && (this.pixelFx.width < lw || this.pixelFx.height < lh)) {
            GlTarget.delete(this.pixelFx);
            this.pixelFx = null;
         }

         if (this.pixelFx == null || lw * 2 >= this.pixelFx.width || lh * 2 >= this.pixelFx.height) {
            this.pixelFxSmallSince = 0L;
         } else if (this.pixelFxSmallSince == 0L) {
            this.pixelFxSmallSince = now;
         } else if (now - this.pixelFxSmallSince > 5000000000L) {
            GlTarget.delete(this.pixelFx);
            this.pixelFx = null;
         }

         if (this.pixelFx == null) {
            this.pixelFx = GlTarget.create(lw + 8, lh + 8, 32856, 6408, false, true);
            this.pixelFxSmallSince = 0L;
            if (this.pixelFx == null) {
               return;
            }
         }

         this.fxW = lw;
         this.fxH = lh;
         int rw = lw * k;
         int rh = lh * k;
         float x0 = (float)r[0] / w * 2.0F - 1.0F;
         float x1 = (float)(r[0] + rw) / w * 2.0F - 1.0F;
         float y0 = (float)r[1] / h * 2.0F - 1.0F;
         float y1 = (float)(r[1] + rh) / h * 2.0F - 1.0F;
         Mat4 rect = RECT.identity().screenAdjust(-(x0 + x1) / 2.0F, -(y0 + y1) / 2.0F, 2.0F / (x1 - x0), 2.0F / (y1 - y0));
         this.pixelFx.bind();
         GlStateManager.viewport(0, 0, lw, lh);
         GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GlStateManager.depthMask(true);
         GlStateManager.clear(16640);
         GL20.glUseProgram(0);
         GlStateManager.enableDepth();
         GlStateManager.depthFunc(515);
         GlStateManager.colorMask(false, false, false, false);

         for (int i = 0; i < this.count; i++) {
            FirstPersonRenderer.Item it = this.items[i];
            if (!it.ghost) {
               this.load(it, rect, this.camera);
               if (it.arm) {
                  this.arms.draw(it.slim, it.rightArm, false);
               } else {
                  this.renderer.draw(it.assets, 0, it.look, WeaponRenderer.Mode.MASK, it.mirrored);
               }
            }
         }

         GlStateManager.colorMask(true, true, true, true);
         this.drawFx(rect);
         this.save.bindTarget();
         int[] vp = this.save.viewport();
         GlStateManager.viewport(vp[0], vp[1], vp[2], vp[3]);
         WeaponShader.Program c = this.renderer.shaders().pixelFx();
         if (c != null) {
            c.use();
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(this.pixelFx.texture);
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.disableCull();
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
            float u1 = (float)lw / this.pixelFx.width;
            float v1 = (float)lh / this.pixelFx.height;
            Tessellator t = Tessellator.getInstance();
            WorldRenderer wr = t.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX);
            wr.pos(x0, y0, 0.0).tex(0.0, 0.0).endVertex();
            wr.pos(x1, y0, 0.0).tex(u1, 0.0).endVertex();
            wr.pos(x1, y1, 0.0).tex(u1, v1).endVertex();
            wr.pos(x0, y1, 0.0).tex(0.0, v1).endVertex();
            t.draw();
            GL20.glUseProgram(0);
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
         }
      }
   }

   private int weaponPixelOnScreen(int w, int h) {
      for (int i = 0; i < this.count; i++) {
         FirstPersonRenderer.Item it = this.items[i];
         if (!it.arm && !it.ghost) {
            Mat4 full = TMP.set(it.left ? this.projLeft : this.projRight).mul(this.camera).mul(it.matrix);
            full.transform(0.0F, 0.0F, 0.0F, this.corner);
            if (!(this.corner[3] < 0.01F)) {
               float ax = this.corner[0] / this.corner[3] * 0.5F * w;
               float ay = this.corner[1] / this.corner[3] * 0.5F * h;
               full.transform(0.0F, 0.03125F, 0.0F, this.corner);
               if (!(this.corner[3] < 0.01F)) {
                  float bx = this.corner[0] / this.corner[3] * 0.5F * w;
                  float by = this.corner[1] / this.corner[3] * 0.5F * h;
                  float d = (float)Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay));
                  return Math.max(2, Math.min(64, Math.round(d)));
               }
            }
         }
      }

      return this.pixelPx;
   }

   private void drawFx(Mat4 rect) {
      if (this.fx.hasAnything()) {
         GL20.glUseProgram(0);
         this.drawProj.set(this.projRight);
         if (rect != null) {
            this.drawProj.premul(rect);
         }

         GlStateManager.matrixMode(5889);
         GlStateManager.loadIdentity();
         GlStateManager.multMatrix(this.drawProj.buffer());
         GlStateManager.matrixMode(5888);
         GlStateManager.loadIdentity();
         GlStateManager.multMatrix(this.camera.buffer());
         this.fx.draw(this.light.light);
      }
   }

   private void drawShimmer() {
      if (this.fx.hasShimmer()) {
         GL20.glUseProgram(0);
         GlStateManager.matrixMode(5889);
         GlStateManager.loadIdentity();
         GlStateManager.multMatrix(this.projRight.buffer());
         GlStateManager.matrixMode(5888);
         GlStateManager.loadIdentity();
         GlStateManager.multMatrix(this.camera.buffer());
         GlStateManager.disableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.disableCull();
         GlStateManager.disableAlpha();
         GlStateManager.disableFog();
         GlStateManager.disableLighting();
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         GlStateManager.enableTexture2D();
         GlStateManager.enableBlend();
         this.fx.drawShimmer();
         GlStateManager.enableDepth();
         GlStateManager.depthMask(true);
      }
   }

   private int[] screenRect(int w, int h, int pad) {
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      if (this.fx.hasAnything()) {
         float[] box = new float[]{minX, minY, maxX, maxY};
         this.fx.bounds(TMP.set(this.projRight).mul(this.camera), box);
         minX = box[0];
         minY = box[1];
         maxX = box[2];
         maxY = box[3];
      }

      Mat4 full = TMP;

      for (int i = 0; i < this.count; i++) {
         FirstPersonRenderer.Item it = this.items[i];
         full.set(it.left ? this.projLeft : this.projRight).mul(this.camera).mul(it.matrix);
         float[] b = it.bounds;

         for (int k = 0; k < 8; k++) {
            float x = (k & 1) == 0 ? b[0] : b[3];
            float y = (k & 2) == 0 ? b[1] : b[4];
            float z = (k & 4) == 0 ? b[2] : b[5];
            full.transform(x, y, z, this.corner);
            float cw = this.corner[3];
            if (cw < 0.01F) {
               minX = -1.0F;
               minY = -1.0F;
               maxX = 1.0F;
               maxY = 1.0F;
            } else {
               float nx = this.corner[0] / cw;
               float ny = this.corner[1] / cw;
               minX = Math.min(minX, nx);
               minY = Math.min(minY, ny);
               maxX = Math.max(maxX, nx);
               maxY = Math.max(maxY, ny);
            }
         }
      }

      if (minX > maxX) {
         return null;
      } else {
         int x0 = (int)Math.floor((minX * 0.5F + 0.5F) * w) - pad;
         int x1 = (int)Math.ceil((maxX * 0.5F + 0.5F) * w) + pad;
         int y0 = (int)Math.floor((minY * 0.5F + 0.5F) * h) - pad;
         int y1 = (int)Math.ceil((maxY * 0.5F + 0.5F) * h) + pad;
         x0 = Math.max(0, x0);
         y0 = Math.max(0, y0);
         x1 = Math.min(w, x1);
         y1 = Math.min(h, y1);
         return x1 - x0 >= 2 && y1 - y0 >= 2 ? new int[]{x0, y0, x1 - x0, y1 - y0} : null;
      }
   }

   private void drawMask(Minecraft mc) {
      this.captureCamera();
      GlStateManager.enableDepth();
      GlStateManager.depthMask(true);
      GlStateManager.depthFunc(515);
      GlStateManager.disableCull();

      for (int pass = 0; pass < 2; pass++) {
         for (int i = 0; i < this.count; i++) {
            FirstPersonRenderer.Item it = this.items[i];
            if (it.arm == (pass == 0) && !it.ghost) {
               this.load(it, null, this.camera);
               if (it.arm) {
                  GlStateManager.colorMask(false, false, false, false);
                  this.arms.draw(it.slim, it.rightArm, false);
                  GlStateManager.colorMask(true, true, true, true);
               } else {
                  this.renderer.draw(it.assets, 0, it.look, WeaponRenderer.Mode.MASK, it.mirrored);
               }
            }
         }
      }
   }

   private void drawSimple(Minecraft mc) {
      this.captureCamera();
      GlStateManager.clear(256);
      GlStateManager.enableDepth();
      GlStateManager.depthMask(true);
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.disableCull();
      GlStateManager.enableRescaleNormal();
      int combined = mc.theWorld
         .getCombinedLight(
            new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ), 0
         );
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, combined & 65535, combined >> 16);
      EntityPlayerSP player = mc.thePlayer;

      for (int i = 0; i < this.count; i++) {
         FirstPersonRenderer.Item it = this.items[i];
         if (!it.ghost) {
            this.load(it, null, this.camera);
            if (it.arm) {
               mc.getTextureManager().bindTexture(player.getLocationSkin());
               RenderHelper.enableStandardItemLighting();
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               this.arms.draw(it.slim, it.rightArm, it.sleeve);
               RenderHelper.disableStandardItemLighting();
            } else {
               this.renderer.draw(it.assets, 1, it.look, WeaponRenderer.Mode.SIMPLE, it.mirrored);
            }
         }
      }

      this.drawFx(null);
   }

   void drawStudio(Minecraft mc, ResourceLocation skin, float fov, int w, int h, float time) {
      this.renderer.setTime(time);
      float aspect = (float)w / Math.max(1, h);
      this.projection.perspective(fov, aspect, 0.02F, 8.0F);
      float shift = 1.0F - 1.7777778F / aspect;
      this.projRight.set(this.projection).screenAdjust(shift, 0.0F, 1.0F, 1.0F);
      this.projLeft.set(this.projection).screenAdjust(-shift, 0.0F, 1.0F, 1.0F);
      this.camera.identity();
      this.light.studio(0.0F, 180.0F);
      boolean simple = this.failed || WeaponsModule.simpleShading() || !this.renderer.shaders().supported() || this.renderer.shaders().weapon() == null;
      GlStateManager.enableDepth();
      GlStateManager.depthFunc(515);
      GlStateManager.depthMask(true);
      GlStateManager.disableCull();
      GlStateManager.disableLighting();
      GlStateManager.shadeModel(7425);
      if (simple) {
         GlStateManager.disableBlend();
         GlStateManager.enableAlpha();
         GlStateManager.enableRescaleNormal();
         OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

         for (int i = 0; i < this.count; i++) {
            FirstPersonRenderer.Item it = this.items[i];
            if (!it.ghost) {
               this.load(it, null, this.camera);
               if (it.arm) {
                  mc.getTextureManager().bindTexture(skin);
                  RenderHelper.enableStandardItemLighting();
                  GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                  this.arms.draw(it.slim, it.rightArm, it.sleeve);
                  RenderHelper.disableStandardItemLighting();
               } else {
                  this.renderer.draw(it.assets, 1, it.look, WeaponRenderer.Mode.SIMPLE, it.mirrored);
               }
            }
         }

         this.drawFx(null);
      } else {
         GlStateManager.disableAlpha();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
         if (this.renderer.beginShaded(this.light, false, time)) {
            WeaponShader.Program p = this.renderer.program();

            for (int ix = 0; ix < this.count; ix++) {
               FirstPersonRenderer.Item it = this.items[ix];
               this.load(it, null, this.camera);
               if (it.arm) {
                  p.set1f("uMat", 30.0F);
                  p.set1f("uAlpha", 1.0F);
                  mc.getTextureManager().bindTexture(skin);
                  this.arms.draw(it.slim, it.rightArm, it.sleeve);
               } else if (it.ghost) {
                  GlStateManager.depthMask(false);
                  this.renderer.draw(it.assets, 1, it.look, WeaponRenderer.Mode.SHADED, it.mirrored);
                  GlStateManager.depthMask(true);
               } else {
                  this.renderer.draw(it.assets, 0, it.look, WeaponRenderer.Mode.SHADED, it.mirrored);
               }
            }

            this.renderer.endShaded();
            this.drawFx(null);
         }
      }
   }

   void loadStudioProjection() {
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.multMatrix(this.projRight.buffer());
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
   }

   static boolean sleeve(AbstractClientPlayer p, boolean right) {
      return p.isWearing(right ? EnumPlayerModelParts.RIGHT_SLEEVE : EnumPlayerModelParts.LEFT_SLEEVE);
   }

   void release() {
      this.ssaa.delete();
      this.fx.release();
      this.fx.clear();
   }

   static final class Item {
      final Mat4 matrix = new Mat4();
      boolean left;
      boolean arm;
      boolean slim;
      boolean rightArm;
      boolean sleeve;
      WeaponAssets assets;
      WeaponRenderer.Look look = new WeaponRenderer.Look();
      boolean mirrored;
      boolean ghost;
      final float[] bounds = new float[6];
   }
}
