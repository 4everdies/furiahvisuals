package meowtils.furiahvisuals.module.ambience;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.GlSave;
import meowtils.furiahvisuals.render.GlTarget;
import meowtils.furiahvisuals.render.PreviewBackdrop;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.render.ViewFrustum;
import meowtils.furiahvisuals.setting.Setting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class AmbiencePreviews {
   private static final int COLS = 4;
   private static final int ROWS = 4;
   private static final int CELLS = 16;
   private static final long FRAME = 33000000L;
   private static final double GROUND = 64.0;
   private static final double EYE = 65.62;
   private static final double CAM_X = 0.5;
   private static final double CAM_Z = 0.5;
   private static final float PITCH = 12.0F;
   private static final float FOV = 48.0F;
   private static final float CARD_RADIUS = 11.0F;
   private static final float BIG_RADIUS = 16.0F;
   private static final int CARD_CAP = 260;
   private static final int BIG_CAP = 1400;
   private static final float CARD_DENSITY = 2.8F;
   private static final float BIG_DENSITY = 3.2F;
   private static final int WARM_TICKS = 70;
   private static final int DAY_TOP = -13873268;
   private static final int DAY_HORIZON = -6440486;
   private static final int DAY_GROUND = -12820940;
   private static final int DAY_FAR = -7428724;
   private final AmbienceModule module;
   private final Wind wind = new Wind();
   private final AmbientEnv cardEnv = previewEnv(11.0F, 260);
   private final AmbientEnv bigEnv = previewEnv(16.0F, 1400);
   private final AmbiencePreviews.Scene[] cards = new AmbiencePreviews.Scene[AmbientStyle.values().length];
   private AmbiencePreviews.Scene big;
   private final int[] versions = new int[AmbientStyle.values().length];
   private long lastNanos;
   private float tickAccum;
   private float frameDt;
   private double time;
   private final GlSave save = new GlSave();
   private final Camera camera = new Camera();
   private final ViewFrustum frustum = new ViewFrustum();
   private GlTarget atlas;
   private GlTarget bigTarget;
   private int cellW;
   private int cellH;
   private final AmbientStyle[] cellOwner = new AmbientStyle[16];
   private final boolean[] cellTaken = new boolean[16];
   private final boolean[] fresh = new boolean[16];
   private long cardsDrawn;
   private long bigDrawn;
   private boolean failed;
   private float drawMillis;
   private long draws;
   private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
   private static final float EDGE_Y = edgeY();

   AmbiencePreviews(AmbienceModule module) {
      this.module = module;
   }

   private static AmbientEnv previewEnv(float radius, int cap) {
      AmbientEnv env = new AmbientEnv();
      env.preview = true;
      env.flatGround = 64.0;
      env.radius = radius;
      env.previewCap = cap;
      env.previewFar = 64.0F;
      env.camX = 0.5;
      env.camY = 65.62;
      env.camZ = 0.5;
      env.outdoorsOnly = true;
      env.dayCycle = true;
      env.globalAlpha = 1.0F;
      env.fogStart = 1.0E9F;
      env.fogEnd = 2.0E9F;
      return env;
   }

   void settingsChanged(AmbientStyle type) {
      int n = type.ordinal();
      this.versions[n]++;
   }

   void gust() {
      this.wind.gustNow(this.module.gustStrength.getFloat() / 100.0F);
   }

   boolean nightOnCard(AmbientStyle type) {
      return this.module.style(type).time.get() == AmbientStyleBase.TimeRule.NIGHT;
   }

   private int advance(long now) {
      float dt = this.lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastNanos) / 1.0E9F);
      this.lastNanos = now;
      this.frameDt = dt;
      this.time += dt;
      this.tickAccum += dt * 20.0F;
      int n = (int)this.tickAccum;
      this.tickAccum -= n;
      return Math.min(3, n);
   }

   private void tickWind() {
      this.wind
         .tick(
            0.05F,
            this.module.windMode.get() == AmbienceModule.WindMode.FIXED ? this.module.windDirection.getFloat() : Float.NaN,
            this.module.windStrength.getFloat() / 100.0F,
            this.module.gustFrequency.getFloat() / 100.0F,
            this.module.gustStrength.getFloat() / 100.0F,
            this.module.turbulence.getFloat() / 100.0F
         );
   }

   private void sync(AmbiencePreviews.Scene sc) {
      int v = this.versions[sc.type.ordinal()];
      if (sc.version != v) {
         sc.version = v;
         AmbientStyleBase real = this.module.style(sc.type);
         List<Setting> from = real.getSettings();
         List<Setting> to = sc.twin.getSettings();

         for (int i = 0; i < from.size() && i < to.size(); i++) {
            Setting a = from.get(i);
            Setting b = to.get(i);
            if (a.isSaved() && a.getId().equals(b.getId())) {
               b.fromJson(a.toJson());
            }
         }

         sc.twin.on.fromJson(real.on.toJson());
      }
   }

   private static void light(AmbiencePreviews.Scene sc) {
      sc.env.daylight = sc.night ? 0.0F : 1.0F;
      sc.env.light = sc.night ? 0.36F : 1.0F;
   }

   private void tickScene(AmbiencePreviews.Scene sc, boolean reseed, boolean refill) {
      light(sc);
      sc.twin.tick(sc.env, sc.env == this.cardEnv ? 2.8F : 3.2F, reseed, refill);
   }

   private void warm(AmbiencePreviews.Scene sc) {
      if (!sc.warmed) {
         sc.warmed = true;
         this.sync(sc);

         for (int i = 0; i < 70; i++) {
            sc.env.tickCount++;
            this.tickScene(sc, i == 0, true);
         }
      }
   }

   int drawCards(List<AmbientStyle> shown, int w, int h, int[] cells) {
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
         int n = Math.min(shown.size(), 16);

         for (int i2 = 0; i2 < n; i2++) {
            AmbientStyle t = shown.get(i2);

            for (int k = 0; k < 16; k++) {
               if (!this.cellTaken[k] && this.cellOwner[k] == t) {
                  cells[t.ordinal()] = k;
                  this.cellTaken[k] = true;
                  break;
               }
            }

            this.fresh[i2] = cells[t.ordinal()] < 0;
         }

         for (int var30 = 0; var30 < n; var30++) {
            AmbientStyle t = shown.get(var30);
            if (cells[t.ordinal()] < 0) {
               for (int kx = 0; kx < 16; kx++) {
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

         for (int i = 0; i < n; i++) {
            anyFresh |= this.fresh[i];
         }

         if (!due && !anyFresh && this.atlas != null) {
            return this.atlas.texture;
         } else {
            for (int var27 = 0; var27 < n; var27++) {
               AmbientStyle t2 = shown.get(var27);
               AmbiencePreviews.Scene sc = this.cards[t2.ordinal()];
               if (sc == null) {
                  AmbiencePreviews.Scene scene = new AmbiencePreviews.Scene(t2, this.cardEnv);
                  this.cards[t2.ordinal()] = scene;
                  sc = scene;
               }

               sc.dark = true;
               sc.night = this.nightOnCard(t2);
               this.sync(sc);
               this.warm(sc);
            }

            if (due) {
               int ticks = this.advance(now);

               for (int k2 = 0; k2 < ticks; k2++) {
                  this.tickWind();
                  this.cardEnv.tickCount++;

                  for (int i3 = 0; i3 < n; i3++) {
                     this.tickScene(this.cards[shown.get(i3).ordinal()], false, false);
                  }
               }

               this.cardsDrawn = now;
            }

            long start = System.nanoTime();
            if (!this.begin()) {
               return -1;
            } else {
               try {
                  if (this.atlas == null) {
                     this.atlas = GlTarget.create(w * 4, h * 4, 32856, 6408, true, false);
                     if (this.atlas == null) {
                        this.fail(null);
                        return -1;
                     }
                  }

                  this.atlas.bind();
                  boolean captured = false;

                  for (int i4 = 0; i4 < n; i4++) {
                     AmbientStyle t3 = shown.get(i4);
                     int cell = cells[t3.ordinal()];
                     if (cell >= 0 && (due || this.fresh[i4])) {
                        captured = this.render(this.cards[t3.ordinal()], cell % 4 * w, cell / 4 * h, w, h, captured);
                        this.cellOwner[cell] = t3;
                     }
                  }

                  return this.atlas.texture;
               } catch (RuntimeException | LinkageError var23) {
                  this.fail(var23);
                  return -1;
               } finally {
                  this.end();
                  this.timed(start);
               }
            }
         }
      } else {
         return -1;
      }
   }

   private void timed(long start) {
      float ms = (float)(System.nanoTime() - start) / 1000000.0F;
      this.drawMillis = this.drawMillis == 0.0F ? ms : this.drawMillis + (ms - this.drawMillis) * 0.1F;
      this.draws++;
   }

   float drawMillisForTest() {
      return this.drawMillis;
   }

   long drawsForTest() {
      return this.draws;
   }

   float[] cellUv(int cell, float[] out) {
      float aw = this.cellW * 4;
      float ah = this.cellH * 4;
      int cx = cell % 4 * this.cellW;
      int cy = cell / 4 * this.cellH;
      out[0] = cx / aw;
      out[2] = (cx + this.cellW) / aw;
      out[1] = (cy + this.cellH) / ah;
      out[3] = cy / ah;
      return out;
   }

   int drawBig(AmbientStyle type, int w, int h, boolean day) {
      if (this.failed) {
         return -1;
      } else {
         w = Math.max(16, Math.min(2048, w));
         h = Math.max(16, Math.min(2048, h));
         boolean fresh = false;
         if (this.big == null || this.big.type != type) {
            this.big = new AmbiencePreviews.Scene(type, this.bigEnv);
            fresh = true;
         }

         this.big.dark = !day;
         this.big.night = !day && this.nightOnCard(type);
         this.sync(this.big);
         this.warm(this.big);
         if (this.bigTarget != null && (this.bigTarget.width != w || this.bigTarget.height != h)) {
            GlTarget.delete(this.bigTarget);
            this.bigTarget = null;
         }

         long now;
         boolean due = (now = ClientClock.nanos()) - this.bigDrawn >= 33000000L;
         if (!due && !fresh && this.bigTarget != null) {
            return this.bigTarget.texture;
         } else {
            if (due) {
               int ticks = this.advance(now);

               for (int k = 0; k < ticks; k++) {
                  this.tickWind();
                  this.bigEnv.tickCount++;
                  this.tickScene(this.big, false, false);
               }

               this.bigDrawn = now;
            }

            long start = System.nanoTime();
            if (!this.begin()) {
               return -1;
            } else {
               try {
                  if (this.bigTarget == null) {
                     this.bigTarget = GlTarget.create(w, h, 32856, 6408, true, false);
                     if (this.bigTarget == null) {
                        this.fail(null);
                        return -1;
                     }
                  }

                  this.bigTarget.bind();
                  this.render(this.big, 0, 0, w, h, false);
                  return this.bigTarget.texture;
               } catch (RuntimeException | LinkageError var18) {
                  this.fail(var18);
                  return -1;
               } finally {
                  this.end();
                  this.timed(start);
               }
            }
         }
      }
   }

   int bigCountForTest() {
      return this.big == null ? 0 : this.big.twin.count();
   }

   float bigPresenceForTest() {
      return this.big == null ? 0.0F : this.big.twin.presence;
   }

   int cardCountForTest(AmbientStyle type) {
      AmbiencePreviews.Scene sc = this.cards[type.ordinal()];
      return sc == null ? 0 : sc.twin.count();
   }

   String twinSettingForTest(AmbientStyle type, boolean bigOne, String id) {
      AmbiencePreviews.Scene sc = bigOne ? (this.big != null && this.big.type == type ? this.big : null) : this.cards[type.ordinal()];
      if (sc == null) {
         return null;
      } else {
         for (Setting s : sc.twin.getSettings()) {
            if (s.getId().equals(id)) {
               return s.toJson().toString();
            }
         }

         return null;
      }
   }

   float windSpeedForTest() {
      return this.wind.speed;
   }

   private boolean begin() {
      GlStateManager.matrixMode(5889);
      GlStateManager.pushMatrix();
      GlStateManager.matrixMode(5888);
      GlStateManager.pushMatrix();
      this.save.save();
      GL20.glUseProgram(0);
      GL11.glDisable(3089);
      Textures.ensureAmbienceLoaded();
      return true;
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
         FuriahsVisuals.LOG.warn("Ambience's previews are off: they could not be drawn", e);
      } else {
         FuriahsVisuals.LOG.warn("Ambience's previews are off: no offscreen target could be made here");
      }
   }

   private boolean render(AmbiencePreviews.Scene sc, int vx, int vy, int vw, int vh, boolean captured) {
      GlStateManager.viewport(vx, vy, vw, vh);
      backdrop(sc.dark);
      lens(vw, vh);
      ground(sc.dark);
      if (sc.dark) {
         groundEdge(vh);
         lens(vw, vh);
      }

      if (!captured) {
         this.camera.captureModelView();
         this.frustum.capture();
      }

      AmbientEnv env = sc.env;
      light(sc);
      env.cam = this.camera;
      env.frustum = this.frustum;
      env.partialTicks = this.tickAccum;
      env.eyeX = 0.5;
      env.viewX = 0.5;
      env.eyeY = 65.62;
      env.viewY = 65.62;
      env.eyeZ = 0.5;
      env.viewZ = 0.5;
      env.frameDt = this.frameDt;
      env.time = this.time;
      env.skyPass = true;
      env.fogColor = sc.dark ? -15723493 : -6440486;
      RenderState.begin();

      try {
         Vfx.setCamera(this.camera);
         AmbientStyleBase s = sc.twin;
         if (!s.spec.volume) {
            if (s.presence > 0.0F) {
               s.drawSky(env);
            }
         } else if (s.count() > 0) {
            s.draw(env);
         }

         Vfx.flush();
      } finally {
         RenderState.end();
      }

      return true;
   }

   private static void lens(int vw, int vh) {
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      perspective(48.0F, (float)vw / vh, 0.05F, 400.0F);
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.rotate(-12.0F, 1.0F, 0.0F, 0.0F);
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

   private static void backdrop(boolean dark) {
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      GlStateManager.disableTexture2D();
      GlStateManager.disableCull();
      GlStateManager.disableAlpha();
      GlStateManager.disableLighting();
      GlStateManager.disableFog();
      GlStateManager.shadeModel(7425);
      GlStateManager.disableBlend();
      Tessellator t = Tessellator.getInstance();
      WorldRenderer wr = t.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
      if (dark) {
         int n = 8;
         int row = n + 1;
         int[] c = PreviewBackdrop.corners(false);

         for (int j = 0; j < n; j++) {
            float y0 = -1.0F + 2.0F * j / n;
            float y1 = -1.0F + 2.0F * (j + 1) / n;

            for (int i = 0; i < n; i++) {
               float x0 = -1.0F + 2.0F * i / n;
               float x1 = -1.0F + 2.0F * (i + 1) / n;
               color(wr.pos(x0, y0, 0.0), c[j * row + i]);
               color(wr.pos(x1, y0, 0.0), c[j * row + i + 1]);
               color(wr.pos(x1, y1, 0.0), c[(j + 1) * row + i + 1]);
               color(wr.pos(x0, y1, 0.0), c[(j + 1) * row + i]);
            }
         }
      } else {
         float hy = (float)(-Math.tan(Math.toRadians(12.0)) / Math.tan(Math.toRadians(24.0)));
         color(wr.pos(-1.0, hy, 0.0), -6440486);
         color(wr.pos(1.0, hy, 0.0), -6440486);
         color(wr.pos(1.0, 1.0, 0.0), -13873268);
         color(wr.pos(-1.0, 1.0, 0.0), -13873268);
         color(wr.pos(-1.0, -1.0, 0.0), -6440486);
         color(wr.pos(1.0, -1.0, 0.0), -6440486);
         color(wr.pos(1.0, hy, 0.0), -6440486);
         color(wr.pos(-1.0, hy, 0.0), -6440486);
      }

      t.draw();
      GlStateManager.enableTexture2D();
   }

   private static void ground(boolean dark) {
      int mid;
      int far;
      int near;
      if (dark) {
         near = ColorUtil.shade(-15065305, 0.85F);
         far = -15065305;
         mid = -15065305;
      } else {
         near = -12820940;
         far = -7428724;
         mid = ColorUtil.lerp(near, far, 0.35F);
      }

      float y = -1.62F;
      GlStateManager.disableTexture2D();
      GlStateManager.disableBlend();
      Tessellator t = Tessellator.getInstance();
      WorldRenderer wr = t.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
      color(wr.pos(-400.0, y, 2.0), near);
      color(wr.pos(400.0, y, 2.0), near);
      color(wr.pos(400.0, y, -30.0), mid);
      color(wr.pos(-400.0, y, -30.0), mid);
      color(wr.pos(-400.0, y, -30.0), mid);
      color(wr.pos(400.0, y, -30.0), mid);
      color(wr.pos(400.0, y, -390.0), far);
      color(wr.pos(-400.0, y, -390.0), far);
      t.draw();
      GlStateManager.enableTexture2D();
   }

   private static float edgeY() {
      double p = Math.toRadians(12.0);
      double y = -1.6200000000000045;
      double z = -390.0;
      double ry = y * Math.cos(p) + z * Math.sin(p);
      double rz = -y * Math.sin(p) + z * Math.cos(p);
      return (float)(ry / -rz / Math.tan(Math.toRadians(24.0)));
   }

   private static void groundEdge(int vh) {
      float px = 2.0F / vh;
      float top = EDGE_Y;
      float line = top - px;
      float bottom = top - 3.0F * px;
      int edge = PreviewBackdrop.groundEdge();
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.disableTexture2D();
      GlStateManager.disableBlend();
      Tessellator t = Tessellator.getInstance();
      WorldRenderer wr = t.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
      color(wr.pos(-1.0, line, 0.0), edge);
      color(wr.pos(1.0, line, 0.0), edge);
      color(wr.pos(1.0, top, 0.0), edge);
      color(wr.pos(-1.0, top, 0.0), edge);
      color(wr.pos(-1.0, bottom, 0.0), -15065305);
      color(wr.pos(1.0, bottom, 0.0), -15065305);
      color(wr.pos(1.0, line, 0.0), edge);
      color(wr.pos(-1.0, line, 0.0), edge);
      t.draw();
      GlStateManager.enableTexture2D();
   }

   private static void color(WorldRenderer wr, int argb) {
      wr.color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24 & 0xFF).endVertex();
   }

   private final class Scene {
      final AmbientStyle type;
      final AmbientStyleBase twin;
      final AmbientEnv env;
      int version = -1;
      boolean dark = true;
      boolean night;
      boolean warmed;

      Scene(AmbientStyle type, AmbientEnv env) {
         this.type = type;
         this.env = env;
         this.twin = type.create(AmbiencePreviews.this.module);
         this.twin.getSettings();
      }
   }
}
