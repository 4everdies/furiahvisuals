package meowtils.furiahvisuals.module.trails;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.misc.skins.SkinHooks;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.GlSave;
import meowtils.furiahvisuals.render.GlTarget;
import meowtils.furiahvisuals.render.PreviewBackdrop;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class TrailPreviews {
   private static final int COLS = 6;
   private static final int ROWS = 4;
   private static final int CELLS = 24;
   private static final long FRAME = 33000000L;
   private static final float CARD_RADIUS = 1.1F;
   private static final float BIG_SIZE = 2.4F;
   private static final float CARD_FOV = 36.0F;
   private static final float BIG_FOV = 34.0F;
   private static final int DAY_TOP = -13740654;
   private static final int DAY_HORIZON = -5651230;
   private static final int DAY_PLATFORM = -9801604;
   private static final int DAY_RIM = -7696228;
   private final TrailsModule module;
   private final TrailPreview[] cards = new TrailPreview[TrailStyle.values().length];
   private TrailPreview big;
   private final TrailStyleBase[] one = new TrailStyleBase[1];
   private final TrailStyleBase[] bigStyles = new TrailStyleBase[3];
   TrailPreviews.View view = TrailPreviews.View.ORBIT;
   boolean day;
   boolean showOther;
   private TrailPreview bigOther;
   static final int DEMO_TEAM = -43691;
   float orbitYaw = 35.0F;
   float orbitPitch = 24.0F;
   long draggedAt;
   private float behindYaw = Float.NaN;
   private final GlSave save = new GlSave();
   private GlTarget atlas;
   private GlTarget bigTarget;
   private int cellW;
   private int cellH;
   private final TrailStyle[] cellOwner = new TrailStyle[24];
   private final boolean[] cellTaken = new boolean[24];
   private final boolean[] fresh = new boolean[24];
   private long cardsDrawn;
   private long bigDrawn;
   private boolean failed;
   private float drawMillis;
   private final BodyProxy.Snapshot body = new BodyProxy.Snapshot();
   private final BodyProxy.Snapshot otherBody = new BodyProxy.Snapshot();
   private final BodyProxy.Part[] parts = BodyProxy.newParts();
   private final BodyProxy.Look look = new BodyProxy.Look();
   private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
   private static final ResourceLocation ALEX = new ResourceLocation("textures/entity/alex.png");

   TrailPreviews(TrailsModule module) {
      this.module = module;
   }

   TrailPreview big() {
      if (this.big == null) {
         this.big = new TrailPreview(this.module, TrailPreview.Path.FIGURE_EIGHT, 2.4F);
      }

      return this.big;
   }

   int drawCards(List<TrailStyle> shown, int w, int h, int[] cells) {
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
         int n = Math.min(shown.size(), 24);

         for (int i = 0; i < n; i++) {
            TrailStyle t = shown.get(i);

            for (int k = 0; k < 24; k++) {
               if (!this.cellTaken[k] && this.cellOwner[k] == t) {
                  cells[t.ordinal()] = k;
                  this.cellTaken[k] = true;
                  break;
               }
            }

            this.fresh[i] = cells[t.ordinal()] < 0;
         }

         for (int var28 = 0; var28 < n; var28++) {
            TrailStyle t = shown.get(var28);
            if (cells[t.ordinal()] < 0) {
               for (int kx = 0; kx < 24; kx++) {
                  if (!this.cellTaken[kx]) {
                     cells[t.ordinal()] = kx;
                     this.cellTaken[kx] = true;
                     break;
                  }
               }
            }
         }

         long now = ClientClock.nanos();
         boolean due = now - this.cardsDrawn >= 33000000L;
         boolean anyFresh = false;

         for (int i2 = 0; i2 < n; i2++) {
            anyFresh |= this.fresh[i2];
         }

         if (!due && !anyFresh && this.atlas != null) {
            return this.atlas.texture;
         } else {
            float dt = this.cardsDrawn == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.cardsDrawn) / 1.0E9F);

            for (int i3 = 0; i3 < n; i3++) {
               TrailStyle t2 = shown.get(i3);
               TrailPreview sc = this.cards[t2.ordinal()];
               this.one[0] = this.module.getStyle(t2);
               if (sc == null) {
                  TrailPreview trailPreview = new TrailPreview(this.module, TrailPreview.Path.LOOP, 1.1F);
                  this.cards[t2.ordinal()] = trailPreview;
                  sc = trailPreview;
                  if (this.one[0].sprintOnly()) {
                     trailPreview.move = TrailPreview.Move.SPRINT;
                  }

                  warm(trailPreview, this.one, 1);
               }

               if (due) {
                  sc.step(dt, this.one, 1);
               }
            }

            if (due) {
               this.cardsDrawn = now;
            }

            long start = System.nanoTime();
            this.begin();

            try {
               if (this.atlas == null) {
                  this.atlas = GlTarget.create(w * 6, h * 4, 32856, 6408, true, true);
                  if (this.atlas == null) {
                     this.fail(null);
                     return -1;
                  }
               }

               this.atlas.bind();
               this.prepareBody();

               for (int i4 = 0; i4 < n; i4++) {
                  TrailStyle t3 = shown.get(i4);
                  int cell = cells[t3.ordinal()];
                  if (cell >= 0 && (due || this.fresh[i4])) {
                     this.one[0] = this.module.getStyle(t3);
                     GlStateManager.viewport(cell % 6 * w, cell / 6 * h, w, h);
                     this.scene(
                        this.cards[t3.ordinal()], null, this.one, 1, w, h, false, this.module.drawsDark(this.one[0]), 36.0F, 6.6F, -35.0F, 30.0F, 0.0, 0.6, 0.0
                     );
                     this.cellOwner[cell] = t3;
                  }
               }

               return this.atlas.texture;
            } catch (RuntimeException | LinkageError var22) {
               this.fail(var22);
               return -1;
            } finally {
               this.end();
               this.timed(start);
            }
         }
      } else {
         return -1;
      }
   }

   private static void warm(TrailPreview sc, TrailStyleBase[] styles, int count) {
      for (int i = 0; i < 45; i++) {
         sc.step(0.033333335F, styles, count);
      }
   }

   float[] cellUv(int cell, float[] out) {
      float aw = this.cellW * 6;
      float ah = this.cellH * 4;
      int cx = cell % 6 * this.cellW;
      int cy = cell / 6 * this.cellH;
      out[0] = cx / aw;
      out[2] = (cx + this.cellW) / aw;
      out[1] = (cy + this.cellH) / ah;
      out[3] = cy / ah;
      return out;
   }

   TrailPreview cardForTest(TrailStyle type) {
      return this.cards[type.ordinal()];
   }

   float drawMillisForTest() {
      return this.drawMillis;
   }

   int drawBig(int w, int h) {
      if (this.failed) {
         return -1;
      } else {
         w = Math.max(16, Math.min(2048, w));
         h = Math.max(16, Math.min(2048, h));
         TrailPreview sc = this.big();
         int count = this.module.runningStyles(this.bigStyles);
         if (this.bigTarget != null && (this.bigTarget.width != w || this.bigTarget.height != h)) {
            GlTarget.delete(this.bigTarget);
            this.bigTarget = null;
         }

         long now = ClientClock.nanos();
         boolean fresh = this.bigTarget == null;
         boolean due = now - this.bigDrawn >= 33000000L;
         if (!due && !fresh) {
            return this.bigTarget.texture;
         } else {
            float dt = this.bigDrawn == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.bigDrawn) / 1.0E9F);
            if (sc.clock() == 0.0) {
               warm(sc, this.bigStyles, count);
            } else {
               sc.step(dt, this.bigStyles, count);
            }

            TrailPreview other = null;
            if (this.showOther) {
               other = this.bigOther;
               if (other == null) {
                  other = this.bigOther = new TrailPreview(this.module, TrailPreview.Path.FIGURE_EIGHT, 2.4F, true, Math.PI);
               }

               other.move = sc.move;
               int n = other.trail.teamColor = this.module.otherColor.get() == TrailsModule.OtherColor.TEAM ? -43691 : 0;
               if (other.clock() == 0.0) {
                  warm(other, this.bigStyles, count);
               } else {
                  other.step(dt, this.bigStyles, count);
               }
            }

            this.bigDrawn = now;
            double fx = 0.0;
            double fy = 0.85;
            double fz = 0.0;
            float dist;
            float pitch;
            float yaw;
            switch (this.view) {
               case SIDE:
                  yaw = 0.0F;
                  pitch = 18.0F;
                  dist = 9.6F;
                  break;
               case BEHIND:
                  float target = sc.bodyYaw + 180.0F;
                  if (Float.isNaN(this.behindYaw)) {
                     this.behindYaw = target;
                  }

                  float d = wrap(target - this.behindYaw);
                  this.behindYaw = this.behindYaw + d * Math.min(1.0F, dt * 4.0F);
                  yaw = this.behindYaw;
                  pitch = 18.0F;
                  dist = 5.4F;
                  fx = sc.x;
                  fy = 1.0 + sc.y * 0.5;
                  fz = sc.z;
                  break;
               default:
                  if (now - this.draggedAt > 2500000000L) {
                     this.orbitYaw += dt * 12.0F;
                  }

                  yaw = this.orbitYaw;
                  pitch = this.orbitPitch;
                  dist = 10.0F;
            }

            if (this.view != TrailPreviews.View.BEHIND) {
               this.behindYaw = Float.NaN;
            }

            long start = System.nanoTime();
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
               this.prepareBody();
               boolean slate = false;

               for (int i = 0; i < count; i++) {
                  slate |= this.module.drawsDark(this.bigStyles[i]);
               }

               this.scene(sc, other, this.bigStyles, count, w, h, this.day, slate, 34.0F, dist, yaw, pitch, fx, fy, fz);
               return this.bigTarget.texture;
            } catch (RuntimeException | LinkageError var30) {
               this.fail(var30);
               return -1;
            } finally {
               this.end();
               this.timed(start);
            }
         }
      }
   }

   private static float wrap(float degrees) {
      float d = degrees % 360.0F;
      if (d > 180.0F) {
         d -= 360.0F;
      } else if (d < -180.0F) {
         d += 360.0F;
      }

      return d;
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
         FuriahsVisuals.LOG.warn("Trails' previews are off: they could not be drawn", e);
      } else {
         FuriahsVisuals.LOG.warn("Trails' previews are off: no offscreen target could be made here");
      }
   }

   private void timed(long start) {
      float ms = (float)(System.nanoTime() - start) / 1000000.0F;
      this.drawMillis = this.drawMillis == 0.0F ? ms : this.drawMillis + (ms - this.drawMillis) * 0.1F;
   }

   private void prepareBody() {
      Minecraft mc = Minecraft.getMinecraft();
      UUID id = mc.thePlayer != null ? mc.thePlayer.getUniqueID() : mc.getSession().getProfile().getId();
      ResourceLocation skin = SkinHooks.ownSkin();
      boolean slim = SkinHooks.ownSlim();
      BodyProxy.Snapshot b = this.body;
      b.skin = skin;
      b.cape = null;
      b.slim = slim;

      for (int i = 0; i < 4; i++) {
         b.armor[i] = null;
      }

      b.held = null;
      b.entity = null;
      b.name = mc.getSession().getUsername();
      b.id = id;
      b.entityId = -3;
      b.vz = 0.0;
      b.vy = 0.0;
      b.vx = 0.0;
      b.light = 15728880;
      b.valid = true;
      BodyProxy.Snapshot o = this.otherBody;
      o.slim = !slim;
      o.skin = o.slim ? ALEX : DefaultPlayerSkin.getDefaultSkinLegacy();
      o.cape = null;

      for (int i = 0; i < 4; i++) {
         o.armor[i] = null;
      }

      o.held = null;
      o.entity = null;
      o.name = "Other player";
      o.id = null;
      o.entityId = -4;
      o.vz = 0.0;
      o.vy = 0.0;
      o.vx = 0.0;
      o.light = 15728880;
      o.valid = true;
   }

   private void scene(
      TrailPreview sc,
      TrailPreview other,
      TrailStyleBase[] styles,
      int count,
      int vw,
      int vh,
      boolean day,
      boolean slate,
      float fov,
      float dist,
      float yaw,
      float pitch,
      double fx,
      double fy,
      double fz
   ) {
      backdrop(day, slate);
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      perspective(fov, (float)vw / vh, 0.05F, 100.0F);
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.translate(0.0F, 0.0F, -dist);
      GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
      GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
      GlStateManager.translate(-fx, -fy, -fz);
      RenderState.begin();

      try {
         Vfx.setCamera(sc.frame.cam);
         sc.frame.cam.captureModelView();
         platform(sc, day);
         if (other != null) {
            shadow(other);
         }

         this.figure(this.body, sc);
         if (other != null) {
            this.figure(this.otherBody, other);
         }
      } finally {
         RenderState.end();
      }

      double scale = 1.0 / Math.tan(Math.toRadians(fov) / 2.0);
      sc.render(styles, count, vh, scale);
      if (other != null) {
         other.render(styles, count, vh, scale);
      }
   }

   private void figure(BodyProxy.Snapshot b, TrailPreview sc) {
      b.pose.mannequin((float)(sc.clock() * 20.0), b.slim, sc.y);
      b.pose.x = sc.x;
      b.pose.z = sc.z;
      b.pose.bodyYaw = sc.bodyYaw;
      b.pose.limbSwing = sc.limbSwing;
      b.pose.limbSwingAmount = sc.limbAmount;
      b.pose.sneaking = sc.sneaking;
      BodyProxy.rest(b, this.parts);
      this.look.reset();
      this.look.lightmap = false;
      this.look.cape = false;
      BodyProxy.draw(b, this.parts, this.look, 0.0, 0.0, 0.0);
      Vfx.flush();
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

   private static void backdrop(boolean day, boolean slate) {
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
      if (day) {
         Vfx.quad(-1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, -1.0, 1.0, -1.0, -1.0, 1.0, -13740654, -13740654, -5651230, -5651230);
      } else {
         int n = 8;
         int row = n + 1;
         int[] c = PreviewBackdrop.corners(slate);

         for (int j = 0; j < n; j++) {
            float y0 = -1.0F + 2.0F * j / n;
            float y1 = -1.0F + 2.0F * (j + 1) / n;

            for (int i = 0; i < n; i++) {
               float x0 = -1.0F + 2.0F * i / n;
               float x1 = -1.0F + 2.0F * (i + 1) / n;
               Vfx.quad(x0, y1, 1.0, x1, y1, 1.0, x1, y0, 1.0, x0, y0, 1.0, c[(j + 1) * row + i], c[(j + 1) * row + i + 1], c[j * row + i + 1], c[j * row + i]);
            }
         }
      }

      Vfx.flush();
      GlStateManager.depthFunc(515);
   }

   private static void platform(TrailPreview sc, boolean day) {
      RenderState.translucent(true);
      int fill = day ? -9801604 : -15065305;
      int rim = day ? -7696228 : PreviewBackdrop.groundEdge();
      double radius = sc.pathSize() + 0.9;
      double rimIn = radius - 0.12;
      int seg = 40;

      for (int i = 0; i < seg; i++) {
         double a0 = i * Math.PI * 2.0 / seg;
         double a1 = (i + 1) * Math.PI * 2.0 / seg;
         double c0 = Math.cos(a0);
         double s0 = Math.sin(a0);
         double c1 = Math.cos(a1);
         double s1 = Math.sin(a1);
         if (sc.figureEight()) {
            Vfx.triangle(0.0, 0.0, 0.0, c0 * radius, 0.0, s0 * radius * 0.6, c1 * radius, 0.0, s1 * radius * 0.6, fill, fill, fill);
            Vfx.quad(
               c0 * rimIn,
               0.001,
               s0 * rimIn * 0.6,
               c0 * radius,
               0.001,
               s0 * radius * 0.6,
               c1 * radius,
               0.001,
               s1 * radius * 0.6,
               c1 * rimIn,
               0.001,
               s1 * rimIn * 0.6,
               rim,
               rim,
               rim,
               rim
            );
         } else {
            Vfx.triangle(0.0, 0.0, 0.0, c0 * radius, 0.0, s0 * radius, c1 * radius, 0.0, s1 * radius, fill, fill, fill);
            Vfx.quad(
               c0 * rimIn,
               0.001,
               s0 * rimIn,
               c0 * radius,
               0.001,
               s0 * radius,
               c1 * radius,
               0.001,
               s1 * radius,
               c1 * rimIn,
               0.001,
               s1 * rimIn,
               rim,
               rim,
               rim,
               rim
            );
         }
      }

      Vfx.flush();
      shadow(sc);
   }

   private static void shadow(TrailPreview sc) {
      RenderState.translucent(false);
      float lift = (float)Math.max(0.0, sc.y);
      Vfx.flatSprite(Textures.GLOW, sc.x, 0.004, sc.z, 0.55 * (1.0 - lift * 0.25), 0.0, (int)(120.0F * (1.0F - lift * 0.4F)) << 24);
      Vfx.flush();
   }

   static enum View {
      SIDE,
      BEHIND,
      ORBIT;
   }
}
