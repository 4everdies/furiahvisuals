package meowtils.furiahvisuals.module.targetaura;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.misc.skins.SkinHooks;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.GlSave;
import meowtils.furiahvisuals.render.GlTarget;
import meowtils.furiahvisuals.render.ParticleSystem;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class AuraPreviews {
   private static final int COLS = 5;
   private static final int ROWS = 4;
   private static final int CELLS = 20;
   private static final long FRAME = 33000000L;
   private static final float FOV = 34.0F;
   private static final int STAGE_TOP = -15394262;
   private static final int STAGE_HORIZON = -14011568;
   private static final int PLATFORM = -14275784;
   private static final int PLATFORM_RIM = -12959406;
   private final TargetAuraModule module;
   private final AuraPreviews.Card[] cards = new AuraPreviews.Card[EffectType.values().length];
   private final GlSave save = new GlSave();
   private GlTarget atlas;
   private int cellW;
   private int cellH;
   private final EffectType[] cellOwner = new EffectType[20];
   private final boolean[] cellTaken = new boolean[20];
   private final boolean[] fresh = new boolean[20];
   private long drawn;
   private long drawnClock;
   private boolean failed;
   private float drawMillis;
   private final BodyProxy.Snapshot body = new BodyProxy.Snapshot();
   private final BodyProxy.Part[] parts = BodyProxy.newParts();
   private final BodyProxy.Look look = new BodyProxy.Look();
   private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);

   AuraPreviews(TargetAuraModule module) {
      this.module = module;
   }

   int drawCards(List<EffectType> shown, int w, int h, int[] cells) {
      Arrays.fill(cells, -1);
      if (!this.failed && !shown.isEmpty()) {
         w = Math.max(16, Math.min(512, w));
         h = Math.max(16, Math.min(512, h));
         if (this.atlas != null && (this.cellW != w || this.cellH != h)) {
            GlTarget.delete(this.atlas);
            this.atlas = null;
         }

         if (this.atlas == null) {
            Arrays.fill(this.cellOwner, null);
         }

         this.cellW = w;
         this.cellH = h;
         Arrays.fill(this.cellTaken, false);
         int n = Math.min(shown.size(), 20);

         for (int i2 = 0; i2 < n; i2++) {
            EffectType t = shown.get(i2);

            for (int k = 0; k < 20; k++) {
               if (!this.cellTaken[k] && this.cellOwner[k] == t) {
                  cells[t.ordinal()] = k;
                  this.cellTaken[k] = true;
                  break;
               }
            }

            this.fresh[i2] = cells[t.ordinal()] < 0;
         }

         for (int var34 = 0; var34 < n; var34++) {
            EffectType t = shown.get(var34);
            if (cells[t.ordinal()] < 0) {
               for (int kx = 0; kx < 20; kx++) {
                  if (!this.cellTaken[kx]) {
                     cells[t.ordinal()] = kx;
                     this.cellTaken[kx] = true;
                     break;
                  }
               }
            }
         }

         long wall = System.nanoTime();
         boolean due = wall - this.drawn >= 33000000L;
         boolean anyFresh = false;

         for (int i3 = 0; i3 < n; i3++) {
            anyFresh |= this.fresh[i3];
         }

         if (!due && !anyFresh && this.atlas != null) {
            return this.atlas.texture;
         } else {
            long now = ClientClock.nanos();
            float dt = this.drawn == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.drawnClock) / 1.0E9F);
            if (due) {
               this.drawn = wall;
               this.drawnClock = now;
            }

            long start = System.nanoTime();
            this.begin();
            if (this.atlas == null) {
               this.atlas = GlTarget.create(w * 5, h * 4, 32856, 6408, true, true);
               if (this.atlas == null) {
                  this.fail(null);
                  int n2 = -1;
                  this.end();
                  float ms = (float)(System.nanoTime() - start) / 1000000.0F;
                  this.drawMillis = this.drawMillis == 0.0F ? ms : this.drawMillis + (ms - this.drawMillis) * 0.1F;
                  return n2;
               }
            }

            int var26;
            try {
               this.atlas.bind();
               this.prepareBody();

               for (int i = 0; i < n; i++) {
                  EffectType t = shown.get(i);
                  int cell = cells[t.ordinal()];
                  if (cell >= 0 && (due || this.fresh[i])) {
                     AuraPreviews.Card card = this.cards[t.ordinal()];
                     if (card == null) {
                        AuraPreviews.Card card2 = new AuraPreviews.Card();
                        this.cards[t.ordinal()] = card2;
                        card = card2;
                     }

                     GlStateManager.viewport(cell % 5 * w, cell / 5 * h, w, h);
                     this.scene(t, card, w, h, dt, now);
                     this.cellOwner[cell] = t;
                  }
               }

               var26 = this.atlas.texture;
            } catch (RuntimeException | LinkageError var23) {
               Throwable e = var23;

               int n3;
               try {
                  this.fail(e);
                  n3 = -1;
                  this.end();
               } catch (Throwable var22) {
                  this.end();
                  float ms = (float)(System.nanoTime() - start) / 1000000.0F;
                  this.drawMillis = this.drawMillis == 0.0F ? ms : this.drawMillis + (ms - this.drawMillis) * 0.1F;
                  throw var22;
               }

               float ms = (float)(System.nanoTime() - start) / 1000000.0F;
               this.drawMillis = this.drawMillis == 0.0F ? ms : this.drawMillis + (ms - this.drawMillis) * 0.1F;
               return n3;
            }

            this.end();
            float ms = (float)(System.nanoTime() - start) / 1000000.0F;
            this.drawMillis = this.drawMillis == 0.0F ? ms : this.drawMillis + (ms - this.drawMillis) * 0.1F;
            return var26;
         }
      } else {
         return -1;
      }
   }

   float[] cellUv(int cell, float[] out) {
      float aw = this.cellW * 5;
      float ah = this.cellH * 4;
      int cx = cell % 5 * this.cellW;
      int cy = cell / 5 * this.cellH;
      out[0] = cx / aw;
      out[2] = (cx + this.cellW) / aw;
      out[1] = (cy + this.cellH) / ah;
      out[3] = cy / ah;
      return out;
   }

   float drawMillisForTest() {
      return this.drawMillis;
   }

   private void scene(EffectType type, AuraPreviews.Card card, int vw, int vh, float dt, long now) {
      backdrop();
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      perspective(34.0F, (float)vw / vh, 0.05F, 100.0F);
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.translate(0.0F, 0.0F, -5.6F);
      GlStateManager.rotate(10.0F, 1.0F, 0.0F, 0.0F);
      GlStateManager.rotate(-28.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.translate(0.0F, -1.12F, 0.0F);
      RenderState.begin();

      try {
         card.cam.captureModelView();
         Vfx.setCamera(card.cam);
         platform();
         this.figure(card.state.age(now));
      } finally {
         RenderState.end();
      }

      card.particles.update(dt);
      this.module.drawCard(type, card.ctx, card.state, card.particles, card.cam, dt, now);
   }

   private void figure(double time) {
      BodyProxy.Snapshot b = this.body;
      b.pose.mannequin((float)(time * 20.0), b.slim, 0.0);
      b.pose.x = 0.0;
      b.pose.z = 0.0;
      b.pose.bodyYaw = 0.0F;
      b.pose.limbSwing = 0.0F;
      b.pose.limbSwingAmount = 0.0F;
      b.pose.sneaking = false;
      BodyProxy.rest(b, this.parts);
      this.look.reset();
      this.look.lightmap = false;
      this.look.cape = false;
      BodyProxy.draw(b, this.parts, this.look, 0.0, 0.0, 0.0);
      Vfx.flush();
   }

   private void prepareBody() {
      Minecraft mc = Minecraft.getMinecraft();
      UUID id = mc.thePlayer != null ? mc.thePlayer.getUniqueID() : mc.getSession().getProfile().getId();
      BodyProxy.Snapshot b = this.body;
      b.skin = SkinHooks.ownSkin();
      b.slim = SkinHooks.ownSlim();
      b.cape = null;

      for (int i = 0; i < 4; i++) {
         b.armor[i] = null;
      }

      b.held = null;
      b.entity = null;
      b.name = mc.getSession().getUsername();
      b.id = id;
      b.entityId = -5;
      b.vz = 0.0;
      b.vy = 0.0;
      b.vx = 0.0;
      b.light = 15728880;
      b.valid = true;
   }

   private static void platform() {
      RenderState.translucent(true);
      Vfx.flatSprite(Textures.RADIAL_FILL, 0.0, 0.001, 0.0, 1.15, 0.0, -1339675848);
      Vfx.flatRing(Textures.LINE_CORE, 0.0, 0.002, 0.0, 1.12, 0.03, 64, 1614430546);
      Vfx.flush();
      RenderState.additive();
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
      Vfx.quad(-1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, -1.0, 1.0, -1.0, -1.0, 1.0, -15394262, -15394262, -14011568, -14011568);
      Vfx.flush();
      GlStateManager.depthFunc(515);
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
         FuriahsVisuals.LOG.warn("Target Aura's card previews are off: they could not be drawn", e);
      } else {
         FuriahsVisuals.LOG.warn("Target Aura's card previews are off: no offscreen target could be made here");
      }
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

   private static final class Card {
      final AuraState state;
      final EffectContext[] ctx = new EffectContext[]{new EffectContext()};
      final ParticleSystem particles = new ParticleSystem(192);
      final Camera cam = new Camera();

      Card() {
         this.state = new AuraState(null, ClientClock.nanos() - 2000000000L);
         this.state.aimed = true;
         this.state.fade = 1.0F;
         this.state.alpha = 1.0F;
      }
   }
}
