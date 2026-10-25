package meowtils.furiahvisuals.module.misc.skins;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.GlSave;
import meowtils.furiahvisuals.render.GlTarget;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class SkinPreviews {
   private static final long FRAME = 33000000L;
   private static final int TOP = -13881285;
   private static final int BOTTOM = -15460836;
   private static final float FOV = 30.0F;
   private static final int PER_FRAME = 2;
   private static final int KEEP = 160;
   private final GlSave save = new GlSave();
   private final Camera cam = new Camera();
   private final BodyProxy.Snapshot body = new BodyProxy.Snapshot();
   private final BodyProxy.Part[] parts = BodyProxy.newParts();
   private final BodyProxy.Look look = new BodyProxy.Look();
   private final Map<String, SkinPreviews.Picture> pictures = new HashMap<>();
   private GlTarget bigTarget;
   private GlTarget cardTarget;
   private long bigDrawn;
   private long frameNanos;
   private int drawnThisFrame;
   private boolean failed;
   private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
   static final int HOVER = 0;
   static final int SEARCH = 1;
   private final GlTarget[] liveTargets = new GlTarget[2];
   private final String[] liveKeys = new String[2];
   private final long[] liveDrawn = new long[2];
   static final float CARD_YAW = 22.0F;

   boolean failed() {
      return this.failed;
   }

   int drawBig(ResourceLocation skin, boolean slim, int w, int h, float yaw, boolean walk, double time) {
      if (!this.failed && skin != null) {
         w = Math.max(16, Math.min(2048, w));
         h = Math.max(16, Math.min(2048, h));
         if (this.bigTarget != null && (this.bigTarget.width != w || this.bigTarget.height != h)) {
            GlTarget.delete(this.bigTarget);
            this.bigTarget = null;
         }

         long now = ClientClock.nanos();
         if (this.bigTarget != null && now - this.bigDrawn < 33000000L) {
            return this.bigTarget.texture;
         } else {
            this.bigDrawn = now;
            this.begin();

            try {
               if (this.bigTarget == null) {
                  this.bigTarget = GlTarget.create(w, h, 32856, 6408, true, true);
                  if (this.bigTarget == null) {
                     this.fail(null);
                     return -1;
                  }
               }

               this.bigTarget.bind();
               this.scene(skin, slim, w, h, yaw, walk, time, 4.6F, true);
               return this.bigTarget.texture;
            } catch (RuntimeException | LinkageError var17) {
               this.fail(var17);
               return -1;
            } finally {
               this.end();
            }
         }
      } else {
         return -1;
      }
   }

   int drawLive(int slot, String key, ResourceLocation skin, boolean slim, int w, int h, double time) {
      if (!this.failed && skin != null) {
         w = Math.max(16, Math.min(1024, w));
         h = Math.max(16, Math.min(1024, h));
         GlTarget t = this.liveTargets[slot];
         if (t != null && (t.width != w || t.height != h)) {
            GlTarget.delete(t);
            this.liveTargets[slot] = null;
            t = null;
         }

         long now = ClientClock.nanos();
         if (t != null && key.equals(this.liveKeys[slot]) && now - this.liveDrawn[slot] < 33000000L) {
            return t.texture;
         } else {
            this.liveDrawn[slot] = now;
            this.liveKeys[slot] = key;
            this.begin();

            int var23;
            try {
               if (t != null || (t = this.liveTargets[slot] = GlTarget.create(w, h, 32856, 6408, true, true)) != null) {
                  t.bind();
                  this.scene(skin, slim, w, h, 22.0F + (float)(time * 40.0 % 360.0), false, time, 4.4F, false);
                  return t.texture;
               }

               this.fail(null);
               int n = -1;
               var23 = n;
            } catch (RuntimeException | LinkageError var18) {
               this.fail(var18);
               return -1;
            } finally {
               this.end();
            }

            return var23;
         }
      } else {
         return -1;
      }
   }

   int card(String key, ResourceLocation skin, boolean slim, int version, boolean ready, int w, int h) {
      w = Math.max(16, Math.min(512, w));
      h = Math.max(16, Math.min(512, h));
      SkinPreviews.Picture p = this.pictures.get(key);
      long now = System.nanoTime();
      if (p != null) {
         p.usedAt = now;
         if (p.w == w && p.h == h && p.version == version && p.slim == slim) {
            return p.texture;
         }
      }

      if (!this.failed && ready && skin != null) {
         if (now - this.frameNanos > 8000000L) {
            this.frameNanos = now;
            this.drawnThisFrame = 0;
         }

         if (this.drawnThisFrame >= 2) {
            return p != null ? p.texture : -1;
         } else {
            this.drawnThisFrame++;
            if (p == null) {
               if (this.pictures.size() >= 160) {
                  this.forgetOldest();
               }

               p = new SkinPreviews.Picture();
               this.pictures.put(key, p);
            }

            this.begin();

            try {
               if (this.cardTarget != null && (this.cardTarget.width != w || this.cardTarget.height != h)) {
                  GlTarget.delete(this.cardTarget);
                  this.cardTarget = null;
               }

               if (this.cardTarget == null) {
                  this.cardTarget = GlTarget.create(w, h, 32856, 6408, true, true);
                  if (this.cardTarget == null) {
                     this.fail(null);
                     return -1;
                  }
               }

               this.cardTarget.bind();
               this.scene(skin, slim, w, h, 22.0F, false, 0.0, 4.4F, false);
               if (p.texture < 0) {
                  p.texture = TextureUtil.glGenTextures();
               }

               GlStateManager.bindTexture(p.texture);
               GL11.glTexParameteri(3553, 10241, 9729);
               GL11.glTexParameteri(3553, 10240, 9728);
               GL11.glTexParameteri(3553, 10242, 33071);
               GL11.glTexParameteri(3553, 10243, 33071);
               GL11.glCopyTexImage2D(3553, 0, 32856, 0, 0, w, h, 0);
               p.w = w;
               p.h = h;
               p.version = version;
               p.slim = slim;
               p.usedAt = now;
               return p.texture;
            } catch (RuntimeException | LinkageError var17) {
               this.fail(var17);
               return -1;
            } finally {
               this.end();
            }
         }
      } else {
         return p != null ? p.texture : -1;
      }
   }

   boolean cardFresh(String key, int version, boolean slim, int w, int h) {
      SkinPreviews.Picture p = this.pictures.get(key);
      return p != null && p.w == Math.max(16, Math.min(512, w)) && p.h == Math.max(16, Math.min(512, h)) && p.version == version && p.slim == slim;
   }

   void forget(String key) {
      SkinPreviews.Picture p = this.pictures.remove(key);
      if (p != null && p.texture > 0) {
         TextureUtil.deleteTexture(p.texture);
      }
   }

   private void forgetOldest() {
      String oldest = null;
      long at = Long.MAX_VALUE;

      for (Entry<String, SkinPreviews.Picture> e : this.pictures.entrySet()) {
         if (e.getValue().usedAt < at) {
            at = e.getValue().usedAt;
            oldest = e.getKey();
         }
      }

      if (oldest != null) {
         this.forget(oldest);
      }
   }

   void trim() {
      long now = System.nanoTime();
      Iterator<Entry<String, SkinPreviews.Picture>> it = this.pictures.entrySet().iterator();

      while (it.hasNext()) {
         Entry<String, SkinPreviews.Picture> e = it.next();
         if (now - e.getValue().usedAt > 60000000000L) {
            if (e.getValue().texture > 0) {
               TextureUtil.deleteTexture(e.getValue().texture);
            }

            it.remove();
         }
      }
   }

   private void scene(ResourceLocation skin, boolean slim, int w, int h, float yaw, boolean walk, double time, float dist, boolean floor) {
      backdrop();
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      perspective(30.0F, (float)w / h, 0.05F, 50.0F);
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.translate(0.0F, 0.0F, -dist);
      GlStateManager.rotate(7.0F, 1.0F, 0.0F, 0.0F);
      GlStateManager.translate(0.0F, -0.93F, 0.0F);
      RenderState.begin();

      try {
         Vfx.setCamera(this.cam);
         this.cam.captureModelView();
         if (floor) {
            RenderState.translucent(false);
            Vfx.flatSprite(Textures.GLOW, 0.0, 0.004, 0.0, 0.62, 0.0, 1845493760);
            Vfx.flush();
         }

         BodyProxy.Snapshot b = this.body;
         b.skin = skin;
         b.cape = null;
         b.slim = slim;

         for (int i = 0; i < 4; i++) {
            b.armor[i] = null;
         }

         b.held = null;
         b.entity = null;
         b.name = "";
         b.id = null;
         b.entityId = -5;
         b.vz = 0.0;
         b.vy = 0.0;
         b.vx = 0.0;
         b.light = 15728880;
         b.valid = true;
         b.pose.mannequin((float)(time * 20.0), slim, 0.0);
         b.pose.bodyYaw = yaw;
         if (walk) {
            b.pose.limbSwing = (float)(time * 13.0);
            b.pose.limbSwingAmount = 0.75F;
         }

         BodyProxy.rest(b, this.parts);
         this.look.reset();
         this.look.lightmap = false;
         this.look.cape = false;
         BodyProxy.draw(b, this.parts, this.look, 0.0, 0.0, 0.0);
         Vfx.flush();
      } finally {
         RenderState.end();
      }
   }

   private static void backdrop() {
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.disableCull();
      GlStateManager.disableBlend();
      GlStateManager.disableAlpha();
      GlStateManager.disableLighting();
      GlStateManager.disableFog();
      GlStateManager.enableTexture2D();
      GlStateManager.enableDepth();
      GlStateManager.depthFunc(519);
      GlStateManager.depthMask(true);
      GlStateManager.colorMask(true, true, true, true);
      GlStateManager.shadeModel(7425);
      Vfx.resetTextureBinding();
      Vfx.quad(-1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, -1.0, 1.0, -1.0, -1.0, 1.0, -13881285, -13881285, -15460836, -15460836);
      Vfx.flush();
      GlStateManager.depthFunc(515);
   }

   private static void perspective(float fovY, float aspect, float near, float far) {
      float f = (float)(1.0 / Math.tan(Math.toRadians(fovY) / 2.0));
      ((Buffer)MATRIX).clear();
      MATRIX.put(f / aspect).put(0.0F).put(0.0F).put(0.0F);
      MATRIX.put(0.0F).put(f).put(0.0F).put(0.0F);
      MATRIX.put(0.0F).put(0.0F).put((far + near) / (near - far)).put(-1.0F);
      MATRIX.put(0.0F).put(0.0F).put(2.0F * far * near / (near - far)).put(0.0F);
      ((Buffer)MATRIX).flip();
      GlStateManager.multMatrix(MATRIX);
   }

   private void begin() {
      GlStateManager.matrixMode(5889);
      GlStateManager.pushMatrix();
      GlStateManager.matrixMode(5888);
      GlStateManager.pushMatrix();
      this.save.save();
      GL20.glUseProgram(0);
      GL11.glDisable(3089);
      Textures.ensureLoaded();
   }

   private void end() {
      this.save.restore();
      GlStateManager.matrixMode(5889);
      GlStateManager.popMatrix();
      GlStateManager.matrixMode(5888);
      GlStateManager.popMatrix();
   }

   private void fail(Throwable e) {
      this.failed = true;
      if (e != null) {
         FuriahsVisuals.LOG.warn("Skins' previews are off: they could not be drawn", e);
      } else {
         FuriahsVisuals.LOG.warn("Skins' previews are off: no offscreen target could be made here");
      }
   }

   static final class Picture {
      int texture = -1;
      int w;
      int h;
      int version;
      boolean slim;
      long usedAt;
   }
}
