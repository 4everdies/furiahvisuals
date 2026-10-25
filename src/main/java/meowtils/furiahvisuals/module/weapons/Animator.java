package meowtils.furiahvisuals.module.weapons;

final class Animator {
   static final float GUARD_SECONDS = 0.06F;
   static final float SWING_BLEND = 0.06F;
   static final float CANCEL_SECONDS = 0.09F;
   private final Pose tmp = new Pose();
   private final Pose from = new Pose();
   private final float[] holdPos = new float[3];
   private final float[] holdRot = new float[]{0.0F, 0.0F, 0.0F, 1.0F};
   private Clip draw;
   private float drawTime;
   private float drawLength;
   private Clip swing;
   private float swingTime;
   private float swingLength;
   private float swingFade;
   private float fromFade;
   private boolean hasFrom;
   private Clip guard;
   private float guardWeight;
   private Clip inspect;
   private float inspectTime;
   private float inspectSpeed = 1.0F;
   private float inspectWeight;
   private boolean inspectCancelled;
   private float leaving = -1.0F;
   Animator.Events events;

   boolean drawing() {
      return this.draw != null;
   }

   boolean swinging() {
      return this.swing != null && this.swingTime < this.swingLength;
   }

   boolean inspecting() {
      return this.inspect != null && !this.inspectCancelled;
   }

   Clip inspectClip() {
      return this.inspect;
   }

   float inspectWeight() {
      return this.inspect == null ? 0.0F : this.inspectWeight;
   }

   float inspectProgress() {
      return this.inspect == null ? 0.0F : Math.min(1.0F, this.inspectTime / this.inspect.length);
   }

   void setHold(float[] pos, float[] rot) {
      System.arraycopy(pos, 0, this.holdPos, 0, 3);
      System.arraycopy(rot, 0, this.holdRot, 0, 4);
   }

   void startDraw(Clip clip, float speed) {
      this.draw = clip;
      this.drawTime = 0.0F;
      this.drawLength = clip == null ? 0.0F : clip.length / Math.max(0.1F, speed);
      this.leaving = -1.0F;
   }

   void startSwing(Clip clip, float seconds, Pose current) {
      if (this.swing != null && this.swingTime < this.swingLength + 0.06F) {
         this.sampleSwing(this.from);
         this.hasFrom = true;
         this.fromFade = 1.0F - Math.min(1.0F, Math.max(0.0F, this.swingTime - this.swingLength) / 0.06F);
      } else {
         this.hasFrom = false;
      }

      this.swing = clip;
      this.swingTime = 0.0F;
      this.swingLength = Math.max(0.05F, seconds);
      this.swingFade = 1.0F;
   }

   void setGuard(Clip clip) {
      this.guard = clip;
   }

   boolean hasGuardClip() {
      return this.guard != null;
   }

   void startInspect(Clip clip, float speed) {
      this.inspect = clip;
      this.inspectTime = 0.0F;
      this.inspectSpeed = Math.max(0.1F, speed);
      this.inspectCancelled = false;
      if (this.inspectWeight <= 0.0F) {
         this.inspectWeight = 0.001F;
      }
   }

   void cancelInspect() {
      if (this.inspect != null) {
         this.inspectCancelled = true;
      }
   }

   void leave() {
      if (this.leaving < 0.0F) {
         this.leaving = 0.0F;
      }
   }

   boolean left() {
      return this.leaving >= 1.0F;
   }

   float leaveProgress() {
      return this.leaving;
   }

   void reset() {
      this.draw = null;
      this.swing = null;
      this.inspect = null;
      this.inspectWeight = 0.0F;
      this.guardWeight = 0.0F;
      this.hasFrom = false;
      this.leaving = -1.0F;
   }

   void update(float dt, boolean blocking, boolean swingsOverGuard, Pose out) {
      out.identity();
      if (this.draw != null) {
         float before2 = this.drawTime;
         this.drawTime += dt;
         float u = this.drawLength <= 0.0F ? 1.0F : this.drawTime / this.drawLength;
         this.fire(this.draw, before2 / Math.max(1.0E-4F, this.drawLength), u);
         if (u >= 1.0F) {
            this.draw = null;
         } else {
            this.draw.sample(u, this.tmp, this.holdPos, this.holdRot);
            out.add(this.tmp, 1.0F);
         }
      }

      float target = blocking && this.guard != null ? 1.0F : 0.0F;
      // Only move when the weight is away from the target. Falling into the "else" branch at target == weight
      // (1.0 while guarding) used to drop it a little every other frame: the guard pose kept twitching.
      if (target > this.guardWeight) {
         this.guardWeight = Math.min(target, this.guardWeight + dt / 0.06F);
      } else if (target < this.guardWeight) {
         this.guardWeight = Math.max(target, this.guardWeight - dt / 0.12F);
      }
      if (this.guardWeight > 0.0F && this.guard != null) {
         this.guard.sample(1.0F, this.tmp, this.holdPos, this.holdRot);
         out.add(this.tmp, Clip.Ease.apply(6, this.guardWeight));
      }

      if (this.hasFrom) {
         this.fromFade -= dt / 0.06F;
         if (this.fromFade <= 0.0F) {
            this.hasFrom = false;
         }
      }

      if (this.swing != null) {
         float before = this.swingTime;
         this.swingTime += dt;
         this.fire(this.swing, before / this.swingLength, this.swingTime / this.swingLength);
         float over = this.swingTime - this.swingLength;
         this.swingFade = over <= 0.0F ? 1.0F : Math.max(0.0F, 1.0F - over / 0.06F);
         float w = this.swingFade * (this.guardWeight > 0.5F && !swingsOverGuard ? 0.0F : 1.0F);
         if (w > 0.0F) {
            this.sampleSwing(this.tmp);
            if (this.hasFrom) {
               this.tmp.blend(this.from, this.fromFade);
            }
            out.add(this.tmp, w);
         }

         if (this.swingFade <= 0.0F) {
            this.swing = null;
         }
      }

      if (this.inspect != null) {
         float beforex = this.inspectTime;
         this.inspectTime = this.inspectTime + dt * this.inspectSpeed;
         float u = this.inspectTime / this.inspect.length;
         if (!this.inspectCancelled) {
            this.fire(this.inspect, beforex / this.inspect.length, u);
            this.inspectWeight = Math.min(1.0F, this.inspectWeight + dt / 0.15F);
            if (u >= 1.0F) {
               this.inspectCancelled = true;
            }
         } else {
            this.inspectWeight -= dt / 0.09F;
         }

         if (this.inspectWeight > 0.0F) {
            this.inspect.sample(Math.min(1.0F, u), this.tmp, this.holdPos, this.holdRot);
            out.add(this.tmp, Clip.Ease.apply(3, this.inspectWeight));
         } else {
            this.inspect = null;
            this.inspectWeight = 0.0F;
         }
      }

      if (this.leaving >= 0.0F) {
         this.leaving = Math.min(1.0F, this.leaving + dt / 0.1F);
         float k = this.leaving * this.leaving;
         out.rPos[1] = out.rPos[1] - 0.55F * k;
         out.rPos[0] = out.rPos[0] + 0.08F * k;
         float[] q = new float[4];
         Mat4.axisAngle(-25.0F * k, 1.0F, 0.0F, 0.0F, q);
         Mat4.qmul(q, out.rRot, out.rRot);
      }
   }

   private void sampleSwing(Pose into) {
      float u = Math.min(1.0F, this.swingTime / this.swingLength);
      this.swing.sample(u, into, this.holdPos, this.holdRot);
   }

   private void fire(Clip c, float from, float to) {
      if (this.events != null && !c.events.isEmpty()) {
         for (Clip.Event e : c.events) {
            if (e.t > from && e.t <= to) {
               this.events.event(c, e);
            }
         }
      }
   }

   interface Events {
      void event(Clip var1, Clip.Event var2);
   }
}
