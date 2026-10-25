package meowtils.furiahvisuals.module.watermark;

public final class Spring {
   public float value;
   public float velocity;
   private boolean started;

   public float update(float target, float omega, float dt) {
      if (!this.started) {
         this.started = true;
         this.value = target;
         this.velocity = 0.0F;
         return this.value;
      } else if (dt <= 0.0F) {
         return this.value;
      } else {
         float d = this.value - target;
         float e = (float)Math.exp(-omega * dt);
         float k = this.velocity + omega * d;
         this.value = target + (d + k * dt) * e;
         this.velocity = (this.velocity - omega * k * dt) * e;
         if (Math.abs(this.value - target) < 0.001F && Math.abs(this.velocity) < 0.01F) {
            this.value = target;
            this.velocity = 0.0F;
         }

         return this.value;
      }
   }

   public void snap(float v) {
      this.started = true;
      this.value = v;
      this.velocity = 0.0F;
   }

   public boolean isStarted() {
      return this.started;
   }

   public boolean isSettled(float target) {
      return Math.abs(this.value - target) < 0.01F && Math.abs(this.velocity) < 0.05F;
   }
}
