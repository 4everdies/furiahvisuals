package meowtils.furiahvisuals.module.weapons;

import java.util.Random;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;

final class Motion {
   final float[] offset = new float[3];
   float pitch;
   float yaw;
   float roll;
   float sprint;
   float sprintHand;
   private final Motion.Spring lagYaw = new Motion.Spring();
   private final Motion.Spring lagPitch = new Motion.Spring();
   private final Motion.Spring strafe = new Motion.Spring();
   private final Motion.Spring bounce = new Motion.Spring();
   private final Motion.Spring crouch = new Motion.Spring();
   private final Motion.Spring jolt = new Motion.Spring();
   private final Motion.Spring joltRoll = new Motion.Spring();
   private final Motion.Spring hitBack = new Motion.Spring();
   private final Motion.Spring bobAmount = new Motion.Spring();
   private final Motion.Spring fidget = new Motion.Spring();
   private float lastYaw;
   private float lastPitch;
   private boolean started;
   private boolean wasOnGround = true;
   private float lastMotionY;
   private int lastHurt;
   private float idle;
   private float nextFidget = 12.0F;
   private float fidgetTime = -1.0F;
   private float fidgetSign = 1.0F;
   private double time;
   private final Random random = new Random();

   void hit() {
      this.hitBack.v++;
   }

   void attacked() {
      this.sprint = 0.0F;
      this.idle = 0.0F;
   }

   void reset() {
      this.lagYaw.reset();
      this.lagPitch.reset();
      this.strafe.reset();
      this.bounce.reset();
      this.crouch.reset();
      this.jolt.reset();
      this.joltRoll.reset();
      this.hitBack.reset();
      this.bobAmount.reset();
      this.fidget.reset();
      this.started = false;
      this.sprint = 0.0F;
      this.idle = 0.0F;
   }

   void update(EntityPlayerSP p, float pt, float dt, WeaponSettings s, float mass, boolean twoHanded) {
      dt = Math.max(0.0F, Math.min(0.1F, dt));
      this.time += dt;
      float heavy = (float)Math.sqrt(Math.max(0.2F, mass) / 0.5F);
      float omega = 16.0F / heavy;
      Random rng = p != null ? p.getRNG() : this.random;
      float yawNow = p == null ? 0.0F : p.prevRotationYaw + (p.rotationYaw - p.prevRotationYaw) * pt;
      float pitchNow = p == null ? 0.0F : p.prevRotationPitch + (p.rotationPitch - p.prevRotationPitch) * pt;
      if (!this.started) {
         this.lastYaw = yawNow;
         this.lastPitch = pitchNow;
         this.started = true;
      }

      float yawRate = dt > 0.0F ? MathHelper.wrapAngleTo180_float(yawNow - this.lastYaw) / dt : 0.0F;
      float pitchRate = dt > 0.0F ? (pitchNow - this.lastPitch) / dt : 0.0F;
      this.lastYaw = yawNow;
      this.lastPitch = pitchNow;
      float sway = s.lookSway.getFloat();
      float maxLag = 5.0F * sway;
      this.lagYaw.step(clamp(-yawRate * 0.012F * sway * heavy, maxLag), omega, 0.72F, dt);
      this.lagPitch.step(clamp(-pitchRate * 0.01F * sway * heavy, maxLag * 0.8F), omega, 0.72F, dt);
      float lateral = 0.0F;
      if (p != null) {
         double yawRad = Math.toRadians(p.rotationYaw);
         double rightX = -Math.cos(yawRad);
         double rightZ = -Math.sin(yawRad);
         lateral = (float)(p.motionX * rightX + p.motionZ * rightZ);
      }

      this.strafe.step(clamp(lateral * 38.0F * s.strafeTilt.getFloat(), 7.0F), omega * 0.8F, 0.8F, dt);
      boolean onGround = p == null || p.onGround;
      float motionY = p == null ? 0.0F : (float)p.motionY;
      if (this.wasOnGround && !onGround && motionY > 0.1F) {
         this.bounce.v = this.bounce.v - 0.5F * s.jumpLand.getFloat();
      } else if (!this.wasOnGround && onGround) {
         float fall = Math.min(1.5F, Math.max(0.0F, -this.lastMotionY) * 1.6F);
         this.bounce.v = this.bounce.v - (0.35F + fall) * s.jumpLand.getFloat();
      }

      this.wasOnGround = onGround;
      this.lastMotionY = motionY;
      this.bounce.step(0.0F, omega * 0.75F, 0.42F, dt);
      this.crouch.step(p != null && p.isSneaking() ? s.crouch.getFloat() : 0.0F, omega * 0.7F, 1.0F, dt);
      int hurtTime = p == null ? 0 : p.hurtTime;
      if (hurtTime > this.lastHurt && hurtTime > 0) {
         float k = s.hurt.getFloat();
         this.jolt.v -= 1.2F * k;
         this.joltRoll.v = this.joltRoll.v + (rng.nextBoolean() ? 1.0F : -1.0F) * 60.0F * k;
      }

      this.lastHurt = hurtTime;
      this.jolt.step(0.0F, omega * 1.2F, 0.5F, dt);
      this.joltRoll.step(0.0F, omega * 1.2F, 0.5F, dt);
      this.hitBack.step(0.0F, omega * 1.3F, 0.55F, dt);
      float want = p != null && p.isSprinting() ? 1.0F : 0.0F;
      float rate = dt / 0.2F;
      this.sprintHand = this.sprint = want > this.sprint ? Math.min(want, this.sprint + rate) : Math.max(want, this.sprint - rate * 1.4F);
      float bob = p == null ? 0.0F : p.prevCameraYaw + (p.cameraYaw - p.prevCameraYaw) * pt;
      this.bobAmount.step(Math.min(1.2F, bob * 10.0F), 10.0F / heavy, 1.0F, dt);
      float phase = 0.0F;
      if (p != null) {
         float walked = p.distanceWalkedModified - p.prevDistanceWalkedModified;
         phase = -(p.distanceWalkedModified + walked * pt) * (float) Math.PI;
      }

      float a = this.bobAmount.x * s.walkBob.getFloat() * (0.8F + 0.2F * heavy);
      boolean still = Math.abs(yawRate) < 2.0F && Math.abs(pitchRate) < 2.0F && bob < 0.005F && (p == null || !p.isSwingInProgress);
      float f3 = this.idle = still ? this.idle + dt : 0.0F;
      if (!s.fidgets.get()) {
         this.fidgetTime = -1.0F;
      } else if (this.fidgetTime < 0.0F && this.idle > 10.0F && this.idle > this.nextFidget) {
         this.fidgetTime = 0.0F;
         this.fidgetSign = rng.nextBoolean() ? 1.0F : -1.0F;
         this.nextFidget = this.idle + 6.0F + rng.nextFloat() * 6.0F;
      }

      float fid = 0.0F;
      if (this.fidgetTime >= 0.0F) {
         this.fidgetTime += dt;
         float u = this.fidgetTime / 0.8F;
         if (!(u >= 1.0F) && still) {
            fid = (float)Math.sin(u * Math.PI) * this.fidgetSign;
         } else {
            this.fidgetTime = -1.0F;
         }
      }

      this.fidget.step(fid, 12.0F, 1.0F, dt);
      float br = s.breathing.getFloat();
      double t = this.time * Math.PI * 2.0 / 3.6;
      float sp = s.sprintPose.getFloat() * this.sprint;
      this.offset[0] = (float)(Math.sin(phase) * 0.013F * a) - this.lagYaw.x * 0.0035F - this.strafe.x * 0.0022F + (twoHanded ? -0.1F : 0.05F) * sp;
      this.offset[1] = (float)(Math.sin(t) * 0.0045F * br)
         - (float)Math.abs(Math.cos(phase)) * 0.016F * a
         + this.bounce.x * 0.03F
         - this.crouch.x * 0.04F
         + this.jolt.x * 0.012F
         + this.lagPitch.x * 0.003F
         - (twoHanded ? 0.06F : 0.09F) * sp;
      this.offset[2] = this.crouch.x * 0.03F + this.hitBack.x * 0.02F + 0.05F * sp;
      this.pitch = (float)(Math.sin(t + 0.9) * 0.7F * br)
         + this.lagPitch.x * 0.9F
         + this.bounce.x * 1.6F
         + (float)Math.abs(Math.cos(phase)) * 1.2F * a
         + this.hitBack.x * 2.2F
         - (twoHanded ? 12.0F : 24.0F) * sp
         + this.fidget.x * 4.0F;
      this.yaw = (float)(Math.sin(t * 0.5) * 0.5 * br) + this.lagYaw.x * 0.9F + (twoHanded ? -34.0F : 10.0F) * sp;
      this.roll = (float)(Math.sin(phase) * 1.6F * a) - this.strafe.x + this.joltRoll.x * 0.06F + (twoHanded ? -18.0F : 16.0F) * sp + this.fidget.x * 7.0F;
   }

   private static float clamp(float v, float max) {
      return v < -max ? -max : (v > max ? max : v);
   }

   static final class Spring {
      float x;
      float v;

      void step(float target, float omega, float zeta, float dt) {
         int n = Math.max(1, (int)Math.ceil(dt * 240.0F));
         float h = dt / n;

         for (int i = 0; i < n; i++) {
            float a = -2.0F * zeta * omega * this.v - omega * omega * (this.x - target);
            this.v += a * h;
            this.x = this.x + this.v * h;
         }
      }

      void reset() {
         this.v = 0.0F;
         this.x = 0.0F;
      }
   }
}
