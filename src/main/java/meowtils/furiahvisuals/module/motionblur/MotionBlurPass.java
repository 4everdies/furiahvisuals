package meowtils.furiahvisuals.module.motionblur;

import java.util.Locale;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.gui.FuriahScreen;
import meowtils.furiahvisuals.render.GpuTimer;
import meowtils.furiahvisuals.render.OptiFineCompat;
import meowtils.furiahvisuals.render.post.PostChain;
import meowtils.furiahvisuals.render.post.PostFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

final class MotionBlurPass implements PostChain.Pass {
   private static final float TURN_SMOOTH = 0.06F;
   private static final long GAP_NANOS = 250000000L;
   private static final double JUMP = 4.0;
   private final MotionBlurModule module;
   private final TrailLayer trails = new TrailLayer();
   private final CameraBlur camera = new CameraBlur();
   private long decidedFrame = -1L;
   private boolean trailRuns;
   private boolean cameraRuns;
   private PostChain.Point trailPoint = PostChain.Point.BEFORE_HAND;
   private float keep;
   private boolean hold;
   private float cameraScale;
   private Entity lastView;
   private int lastPerspective = -1;
   private double lastX;
   private double lastY;
   private double lastZ;
   private boolean haveOrigin;
   private long lastRunNanos;
   private long lastDecideNanos;
   private long lastDecideGameNanos;
   private long lastRunGameNanos;
   private boolean trailReset = true;
   private boolean cameraReset = true;
   private float lastYaw;
   private float lastPitch;
   private boolean haveAngles;
   private float turnAngle;
   private float turnFactor;
   private float lastDt = 0.016666668F;
   private String failure;
   private float lastKeep;
   private final GpuTimer handTimer = new GpuTimer();
   private final GpuTimer hudTimer = new GpuTimer();
   private float cpuHand;
   private float cpuHud;
   private float frameMs = 16.666666F;
   private long handUsedMs;
   private long hudUsedMs;
   private int lastGlError;
   private int resets;
   private String resetReason = "none";

   MotionBlurPass(MotionBlurModule module) {
      this.module = module;
   }

   @Override
   public String name() {
      return "motion_blur";
   }

   @Override
   public boolean wants(PostChain.Point at) {
      this.decide();
      return this.cameraRuns && at == PostChain.Point.BEFORE_HAND || this.trailRuns && at == this.trailPoint;
   }

   private void decide() {
      long frame = GameClock.frame();
      if (frame != this.decidedFrame) {
         this.decidedFrame = frame;
         this.trailRuns = false;
         this.cameraRuns = false;
         Minecraft mc = Minecraft.getMinecraft();
         Entity view = mc.getRenderViewEntity();
         long now = System.nanoTime();
         float realDt = this.lastDecideNanos == 0L ? 0.0F : (float)(now - this.lastDecideNanos) / 1.0E9F;
         this.lastDecideNanos = now;
         long game = GameClock.nanos();
         long gameGap = this.lastDecideGameNanos == 0L ? 0L : game - this.lastDecideGameNanos;
         this.lastDecideGameNanos = game;
         if (this.module.isEnabled() && mc.theWorld != null && view != null && this.allowedHere(mc)) {
            if (gameGap > 250000000L) {
               this.reset("a gap of " + gameGap / 1000000L + " ms");
            }

            float pt = GameClock.partialTicks();
            double x = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
            double y = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
            double z = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;
            int perspective = mc.gameSettings.thirdPersonView;
            if (view != this.lastView || perspective != this.lastPerspective) {
               this.reset(view != this.lastView ? "another camera entity" : "the F5 view changed");
               this.haveAngles = false;
            } else if (this.haveOrigin
               && (x - this.lastX) * (x - this.lastX) + (y - this.lastY) * (y - this.lastY) + (z - this.lastZ) * (z - this.lastZ) > 16.0) {
               this.reset("the camera jumped");
            }

            this.lastView = view;
            this.lastPerspective = perspective;
            this.lastX = x;
            this.lastY = y;
            this.lastZ = z;
            this.haveOrigin = true;
            float dt = GameClock.dt();
            boolean paused = GameClock.paused();
            if (dt > 0.0F) {
               this.lastDt = dt;
            }

            if (realDt > 0.0F && realDt < 1.0F) {
               this.frameMs = this.frameMs + (realDt * 1000.0F - this.frameMs) * 0.05F;
            }

            float step = !paused && !(dt <= 0.0F) ? dt : this.lastDt;
            float turn = this.turnFactor(view, pt, dt, paused);
            BlurStyle style = this.module.style.get();
            boolean independent = this.module.fpsIndependent.get();
            if (style.camera()) {
               float s = this.module.cameraStrength() / 100.0F;
               boolean skip = this.cameraReset;
               this.cameraReset = false;
               float movement = view.ridingEntity != null ? 0.0F : this.module.movement();
               boolean motion = this.camera.prepare(x, y, z, GameClock.seconds(), movement, skip, independent ? 0.01 : 0.0);
               if (s > 0.0F && motion) {
                  this.cameraScale = independent ? s * (1.0F / this.module.referenceHz()) / clamp(this.camera.span(), 5.0E-4F, 0.1F) : s;
                  this.cameraRuns = true;
               }
            } else {
               this.camera.reset();
            }

            float s = this.module.trailStrength() / 100.0F;
            if (style.trails() && s > 0.0F) {
               float tau = 0.3F * s * s * 60.0F / this.module.referenceHz();
               float k = independent ? (float)Math.exp(-clamp(step, 5.0E-4F, 0.1F) / tau) : (float)Math.exp(-0.016666668F / tau);
               if (this.module.onlyTurning.get()) {
                  k *= turn;
               }

               this.keep = k;
               this.lastKeep = k;
               if (k >= 0.004F) {
                  this.hold = independent && !paused && dt <= 0.0F;
                  this.trailPoint = this.module.blurHand.get() ? PostChain.Point.BEFORE_HUD : PostChain.Point.BEFORE_HAND;
                  this.trailRuns = true;
               } else {
                  this.trailReset = true;
               }
            } else {
               this.trailReset = true;
            }
         } else {
            this.trailReset = true;
            this.cameraReset = true;
            this.haveAngles = false;
         }
      }
   }

   private boolean allowedHere(Minecraft mc) {
      if (!mc.gameSettings.anaglyph && !OptiFineCompat.shadersActive() && !PostChain.broken(this)) {
         boolean third = mc.gameSettings.thirdPersonView != 0;
         if (third ? this.module.thirdPerson.get() : this.module.firstPerson.get()) {
            GuiScreen screen = mc.currentScreen;
            return screen == null || screen instanceof FuriahScreen || this.module.inMenus.get();
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private float turnFactor(Entity view, float pt, float dt, boolean paused) {
      float yaw = view.prevRotationYaw + (view.rotationYaw - view.prevRotationYaw) * pt;
      float pitch = view.prevRotationPitch + (view.rotationPitch - view.prevRotationPitch) * pt;
      if (this.haveAngles) {
         float dy = wrap(yaw - this.lastYaw);
         float dp = pitch - this.lastPitch;
         this.turnAngle = this.turnAngle + (float)Math.sqrt(dy * dy + dp * dp);
      } else {
         this.turnAngle = 0.0F;
      }

      this.lastYaw = yaw;
      this.lastPitch = pitch;
      this.haveAngles = true;
      if (dt > 0.0F && !paused) {
         float speed = this.turnAngle / dt;
         this.turnAngle = 0.0F;
         float target = clamp(speed / this.module.turnSpeed.getFloat(), 0.0F, 1.0F);
         this.turnFactor = this.turnFactor + (target - this.turnFactor) * (1.0F - (float)Math.exp(-dt / 0.06F));
      }

      return this.turnFactor;
   }

   @Override
   public void run(PostChain.Point at, PostFrame frame) {
      boolean debug = this.module.debug.get();
      if (!debug) {
         this.draw(at, frame);
      } else {
         boolean hand = at == PostChain.Point.BEFORE_HAND;
         GpuTimer timer = hand ? this.handTimer : this.hudTimer;
         long start = System.nanoTime();
         timer.begin();
         this.draw(at, frame);
         timer.end();
         float ms = (float)(System.nanoTime() - start) / 1000000.0F;
         if (hand) {
            this.cpuHand = this.cpuHand + (ms - this.cpuHand) * 0.1F;
            this.handUsedMs = System.currentTimeMillis();
         } else {
            this.cpuHud = this.cpuHud + (ms - this.cpuHud) * 0.1F;
            this.hudUsedMs = System.currentTimeMillis();
         }

         int error = GL11.glGetError();
         if (error != 0) {
            this.lastGlError = error;
         }
      }
   }

   String debugLine() {
      Minecraft mc = Minecraft.getMinecraft();
      BlurStyle style = this.module.style.get();
      StringBuilder sb = new StringBuilder("Motion Blur · ")
         .append(style.displayName())
         .append(" · ")
         .append(Minecraft.getDebugFPS())
         .append(" FPS · ")
         .append(String.format(Locale.ROOT, "%.2f ms", this.frameMs));
      if (style.trails()) {
         Object[] objectArray = new Object[]{this.lastKeep, this.trails.allocated() ? (this.trails.high() ? "16-bit" : "8-bit") : "no history"};
         sb.append(String.format(Locale.ROOT, " · keep %.2f · %s", objectArray));
      }

      String hand = PostChain.handHooked() ? "hand hook" : "hand event";
      if (!this.cameraRuns && !this.trailRuns) {
         sb.append(" · idle");
      } else {
         sb.append(" · ");
         if (this.cameraRuns) {
            sb.append("camera ").append(hand);
         }

         if (this.trailRuns) {
            sb.append(this.cameraRuns ? ", trails " : "trails ")
               .append(this.trailPoint == PostChain.Point.BEFORE_HUD ? (PostChain.hudHooked() ? "HUD hook" : "HUD event") : hand);
         }
      }

      boolean hudNow;
      long now;
      boolean handNow;
      float gpu;
      if ((
            gpu = ((handNow = (now = System.currentTimeMillis()) - this.handUsedMs < 500L) ? this.handTimer.millis() : 0.0F)
               + ((hudNow = now - this.hudUsedMs < 500L) ? this.hudTimer.millis() : 0.0F)
         )
         > 0.0F) {
         sb.append(String.format(Locale.ROOT, " · %.3f ms GPU", gpu));
      } else {
         sb.append(String.format(Locale.ROOT, " · %.3f ms CPU", (handNow ? this.cpuHand : 0.0F) + (hudNow ? this.cpuHud : 0.0F)));
      }

      if (this.lastGlError != 0) {
         sb.append(" · GL error ").append(this.lastGlError);
      }

      if (this.failure != null) {
         sb.append(" · off: ").append(this.failure);
      }

      return sb.toString();
   }

   private void draw(PostChain.Point at, PostFrame frame) {
      if (this.cameraRuns && at == PostChain.Point.BEFORE_HAND) {
         this.camera.run(frame, this.cameraScale, this.module.maxLength(), this.module.quality(), this.module.naturalLight.get());
         frame.frameWritten();
      }

      if (this.trailRuns && at == this.trailPoint) {
         long game = GameClock.nanos();
         if (this.lastRunGameNanos != 0L && game - this.lastRunGameNanos > 250000000L) {
            this.reset("a gap of " + (game - this.lastRunGameNanos) / 1000000L + " ms without trails");
         }

         this.lastRunGameNanos = game;
         this.lastRunNanos = System.nanoTime();
         if (this.trailReset) {
            this.trailReset = false;
            this.resets++;
            this.trails.reset();
         }

         this.trails
            .run(
               frame,
               this.keep,
               this.module.trailLook.get() == MotionBlurModule.TrailLook.PHOSPHOR,
               this.module.naturalLight.get(),
               this.module.precision.get() != MotionBlurModule.Precision.STANDARD,
               this.hold
            );
      }
   }

   void reset() {
      this.reset("asked");
   }

   private void reset(String why) {
      this.trailReset = true;
      this.cameraReset = true;
      this.resetReason = why;
   }

   String resetsForTest() {
      return this.resets + " resets, last: " + this.resetReason;
   }

   @Override
   public void failed(String why) {
      this.failure = why;
   }

   @Override
   public void release() {
      this.trails.release();
      this.trailReset = true;
      this.cameraReset = true;
   }

   void releaseIfIdle() {
      if (this.trails.allocated() && System.nanoTime() - this.lastRunNanos > 3000000000L) {
         this.trails.release();
         this.trailReset = true;
      }
   }

   String failure() {
      return this.failure;
   }

   boolean highRefused() {
      return this.trails.highRefused();
   }

   boolean high() {
      return this.trails.high();
   }

   String stateForTest() {
      String what = this.cameraRuns && this.trailRuns ? "camera + trails" : (this.cameraRuns ? "camera" : (this.trailRuns ? "trails" : null));
      what = what == null
         ? "off"
         : "runs (" + what + ") " + (this.trailRuns && this.trailPoint == PostChain.Point.BEFORE_HUD ? "before the HUD" : "before the hand");
      Object[] objectArray = new Object[]{
         this.lastKeep,
         this.hold,
         this.turnFactor,
         this.cameraScale,
         this.camera.span() * 1000.0F,
         this.trails.allocated() ? (this.trails.high() ? "16-bit float" : "8-bit") : "none",
         this.failure == null ? "" : ", failed: " + this.failure
      };
      return what + String.format(Locale.ROOT, ", keep %.3f, hold %s, turn %.2f, camera scale %.2f over %.1f ms, history %s%s", objectArray);
   }

   boolean allocatedForTest() {
      return this.trails.allocated();
   }

   CameraBlur cameraForTest() {
      return this.camera;
   }

   private static float wrap(float degrees) {
      float d = degrees % 360.0F;
      if (d >= 180.0F) {
         d -= 360.0F;
      } else if (d < -180.0F) {
         d += 360.0F;
      }

      return d;
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }
}
