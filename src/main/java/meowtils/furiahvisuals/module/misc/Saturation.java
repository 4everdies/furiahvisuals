package meowtils.furiahvisuals.module.misc;

import java.util.Locale;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.gui.GuiColors;
import meowtils.furiahvisuals.render.GlSave;
import meowtils.furiahvisuals.render.GlTarget;
import meowtils.furiahvisuals.render.GpuTimer;
import meowtils.furiahvisuals.render.post.PostChain;
import meowtils.furiahvisuals.render.post.PostFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

final class Saturation implements PostChain.Pass {
   static final int PREVIEW_W = 480;
   private static final long CAPTURE_MS = 100L;
   private final MiscModule misc;
   private float sat = 1.0F;
   private float con = 1.0F;
   private float vib = 0.0F;
   private float amount;
   private long updatedFrame = -1L;
   private long lastUpdateNanos;
   private int dimension = Integer.MIN_VALUE;
   private long dimensionBlendUntilMs;
   private boolean active;
   private PostChain.Point point = PostChain.Point.BEFORE_HUD;
   private GlTarget before;
   private GlTarget after;
   private long previewWantedUntil;
   private long lastCaptureMs;
   private boolean captureDue;
   private boolean captured;
   private int captures;
   private String failure;
   static boolean timingForTest;
   static PostChain.Point pointForTest;
   private final GpuTimer copyTimer = new GpuTimer();
   private final GpuTimer gradeTimer = new GpuTimer();

   Saturation(MiscModule misc) {
      this.misc = misc;
   }

   @Override
   public String name() {
      return "saturation";
   }

   static boolean allowedHere(Minecraft mc) {
      return !mc.gameSettings.anaglyph && OpenGlHelper.shadersSupported && !GuiColors.isOpen();
   }

   private void update() {
      long frame = GameClock.frame();
      if (frame != this.updatedFrame) {
         this.updatedFrame = frame;
         Minecraft mc = Minecraft.getMinecraft();
         long now = ClientClock.nanos();
         float clientDt = this.lastUpdateNanos == 0L ? 0.0F : Math.min(0.5F, (float)(now - this.lastUpdateNanos) / 1.0E9F);
         this.lastUpdateNanos = now;
         float dt = GameClock.paused() ? clientDt : GameClock.dt();
         long nowMs = ClientClock.millis();
         int dim = this.misc.dimensionIndex(mc);
         if (dim != this.dimension) {
            if (this.dimension != Integer.MIN_VALUE) {
               this.dimensionBlendUntilMs = nowMs + 1200L;
            }

            this.dimension = dim;
         }

         this.misc.followDimension(mc, dim);
         boolean on = this.misc.isEnabled() && this.misc.satOn.get() && allowedHere(mc);
         float[] target = this.misc.satValues(dim);
         float tau = nowMs < this.dimensionBlendUntilMs ? 0.33333334F : this.misc.satTransition.getFloat() / 3.0F;
         float k = tau <= 1.0E-4F ? 1.0F : 1.0F - (float)Math.exp(-dt / tau);
         this.sat = this.sat + (target[0] - this.sat) * k;
         this.con = this.con + (target[1] - this.con) * k;
         this.vib = this.vib + (target[2] - this.vib) * k;
         this.amount = this.amount + ((on ? 1.0F : 0.0F) - this.amount) * k;
         if (Math.abs(target[0] - this.sat) < 5.0E-4F) {
            this.sat = target[0];
         }

         if (Math.abs(target[1] - this.con) < 5.0E-4F) {
            this.con = target[1];
         }

         if (Math.abs(target[2] - this.vib) < 5.0E-4F) {
            this.vib = target[2];
         }

         float f = on ? 1.0F : 0.0F;
         if (Math.abs(f - this.amount) < 0.001F) {
            this.amount = on ? 1.0F : 0.0F;
         }

         boolean neutral = this.sat == 1.0F && this.con == 1.0F && this.vib == 0.0F;
         this.active = this.amount > 0.0F && !neutral && allowedHere(mc) && this.failure == null;
         boolean hud = this.misc.satApplyTo.get() == MiscModule.ApplyTo.WORLD_AND_HUD;
         boolean hudDrawn = !mc.gameSettings.hideGUI || mc.currentScreen != null;
         PostChain.Point point = this.point = hud && hudDrawn ? PostChain.Point.AFTER_HUD : PostChain.Point.BEFORE_HUD;
         if (pointForTest != null) {
            this.point = pointForTest;
         }

         this.captureDue = nowMs < this.previewWantedUntil && nowMs - this.lastCaptureMs >= 100L && allowedHere(mc);
      }
   }

   @Override
   public boolean wants(PostChain.Point at) {
      this.update();
      return this.active && at == this.point || this.captureDue && at == PostChain.Point.BEFORE_HUD;
   }

   @Override
   public void run(PostChain.Point at, PostFrame frame) {
      if (this.captureDue && at == PostChain.Point.BEFORE_HUD) {
         this.capture(frame);
      }

      if (this.active && at == this.point) {
         if (timingForTest) {
            GL11.glFinish();
            this.copyTimer.begin();
         }

         int source = frame.color();
         if (timingForTest) {
            this.copyTimer.end();
            this.gradeTimer.begin();
         }

         frame.bindTarget();
         SaturationShader.grade(source, frame.width(), frame.height(), this.sat, this.con, this.vib, this.amount, true);
         if (timingForTest) {
            this.gradeTimer.end();
         }
      }
   }

   String timesForTest() {
      return String.format(Locale.ROOT, "copy %.3f ms, grade %.3f ms", this.copyTimer.millis(), this.gradeTimer.millis());
   }

   void resetTimesForTest() {
      this.copyTimer.reset();
      this.gradeTimer.reset();
   }

   private void capture(PostFrame frame) {
      int w = 480;
      int h = Math.max(1, Math.round((float)(480 * frame.height()) / Math.max(1, frame.width())));
      if (this.before == null || this.before.width != w || this.before.height != h) {
         GlTarget.delete(this.before);
         this.before = GlTarget.create(w, h, 32856, 6408, true, false);
         if (this.before == null) {
            throw new IllegalStateException("the preview's framebuffer is incomplete");
         }
      }

      int source = frame.color();
      this.before.bind();
      SaturationShader.downscale(source, frame.width(), frame.height(), w, h);
      frame.bindTarget();
      this.captureDue = false;
      this.captured = true;
      this.captures++;
      this.lastCaptureMs = ClientClock.millis();
   }

   void previewShown() {
      this.previewWantedUntil = ClientClock.millis() + 300L;
   }

   int beforeTexture() {
      return this.captured && this.before != null ? this.before.texture : 0;
   }

   int beforeWidth() {
      return this.before == null ? 480 : this.before.width;
   }

   int beforeHeight() {
      return this.before == null ? 270 : this.before.height;
   }

   int renderAfter(int source, int w, int h, float saturation, float contrast, float vibrance) {
      if (source > 0 && this.failure == null) {
         GlSave save = new GlSave().save();

         try {
            if (this.after == null || this.after.width != w || this.after.height != h) {
               GlTarget.delete(this.after);
               this.after = GlTarget.create(w, h, 32856, 6408, true, false);
               if (this.after == null) {
                  return 0;
               }
            }

            this.after.bind();
            GlStateManager.disableBlend();
            GlStateManager.disableDepth();
            GlStateManager.disableAlpha();
            GlStateManager.disableCull();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            SaturationShader.grade(source, w, h, saturation, contrast, vibrance, 1.0F, false);
            return this.after.texture;
         } catch (RuntimeException | LinkageError var14) {
            this.failed(var14.getClass().getSimpleName() + (var14.getMessage() == null ? "" : ": " + var14.getMessage()));
            return 0;
         } finally {
            save.restore();
         }
      } else {
         return 0;
      }
   }

   @Override
   public void failed(String why) {
      this.failure = why;
   }

   String failure() {
      return this.failure;
   }

   @Override
   public void release() {
      GlTarget.delete(this.before);
      GlTarget.delete(this.after);
      this.before = null;
      this.after = null;
      this.captured = false;
   }

   String stateForTest() {
      Object[] objectArray = new Object[]{
         this.active ? (this.point == PostChain.Point.AFTER_HUD ? "grades after the HUD" : "grades before the HUD") : "off",
         this.sat,
         this.con,
         this.vib,
         this.amount,
         this.captures,
         this.failure == null ? "" : ", failed: " + this.failure
      };
      return String.format(Locale.ROOT, "%s: saturation %.2f contrast %.2f vibrance %.2f, amount %.2f, %d pictures%s", objectArray);
   }

   float[] blendedForTest() {
      return new float[]{this.sat, this.con, this.vib, this.amount};
   }
}
