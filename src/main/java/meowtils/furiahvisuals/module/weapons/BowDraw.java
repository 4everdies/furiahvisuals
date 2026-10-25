package meowtils.furiahvisuals.module.weapons;

final class BowDraw {
   static final float NOCK = 0.12F;
   static final float SNAP = 0.15F;
   static final float LET_DOWN = 0.2F;
   static final float TREMBLE_AFTER = 1.0F;
   static final int MAX_USE = 72000;
   boolean drawing;
   float drawT;
   float fullT;
   float releaseT = -1.0F;
   float releasedPull;
   boolean fired;
   int lastCount;
   float charge;
   float pluckT = -1.0F;

   static float charge(float useTicks) {
      float f = Math.max(0.0F, useTicks) / 20.0F;
      f = (f * f + f * 2.0F) / 3.0F;
      return Math.min(1.0F, f);
   }

   static boolean fires(int useTicks) {
      return charge(useTicks) >= 0.1F;
   }

   void update(boolean using, int count, float pt, float dt) {
      if (using) {
         if (!this.drawing) {
            this.drawing = true;
            this.drawT = 0.0F;
            this.fullT = 0.0F;
            this.releaseT = -1.0F;
         } else {
            this.drawT += dt;
         }

         this.lastCount = count;
         this.charge = charge(72000.0F - (count - pt + 1.0F));
         this.fullT = this.charge >= 1.0F ? this.fullT + dt : 0.0F;
      } else {
         if (this.drawing) {
            this.drawing = false;
            this.releasedPull = this.charge;
            this.fired = fires(72000 - this.lastCount);
            this.releaseT = 0.0F;
         } else if (this.releaseT >= 0.0F) {
            this.releaseT += dt;
            if (this.releaseT > Math.max(0.15F, 0.2F) + 0.4F) {
               this.releaseT = -1.0F;
            }
         }

         this.charge = 0.0F;
      }
   }

   void reset() {
      this.drawing = false;
      this.releaseT = -1.0F;
      this.pluckT = -1.0F;
      this.charge = 0.0F;
      this.drawT = 0.0F;
      this.fullT = 0.0F;
   }

   void pluck() {
      this.pluckT = 0.0F;
   }

   void stepPluck(float dt) {
      if (this.pluckT >= 0.0F) {
         this.pluckT += dt;
         if (this.pluckT > 0.4F) {
            this.pluckT = -1.0F;
         }
      }
   }

   float pluckShake() {
      return this.pluckT < 0.0F ? 0.0F : 0.035F * (float)Math.exp(-this.pluckT / 0.09F) * (float)Math.sin(this.pluckT * 2.0 * Math.PI / 0.028F);
   }

   float pull() {
      if (this.drawing) {
         return this.charge;
      } else if (this.releaseT < 0.0F) {
         return 0.0F;
      } else if (this.fired) {
         float t = this.releaseT;
         return t >= 0.15F ? 0.0F : -0.05F * (float)Math.exp(-t / 0.045F) * (float)Math.sin(t * 2.0 * Math.PI / 0.032F);
      } else {
         return this.releasedPull * (1.0F - smooth(this.releaseT / 0.2F));
      }
   }

   float bend() {
      if (this.drawing) {
         return this.charge;
      } else if (this.releaseT < 0.0F) {
         return 0.0F;
      } else if (this.fired) {
         float t = this.releaseT;
         if (t >= 0.15F) {
            return 0.0F;
         } else {
            float b = this.releasedPull * (float)Math.exp(-t / 0.018F) * (float)Math.cos(t * 2.0 * Math.PI / 0.06F);
            return Math.max(-0.06F, b);
         }
      } else {
         return this.pull();
      }
   }

   boolean nocked() {
      return this.drawing ? this.drawT >= 0.06F : !this.fired && this.releaseT >= 0.0F && this.releaseT < 0.120000005F;
   }

   float handOn() {
      if (this.drawing) {
         return smooth(this.drawT / 0.12F);
      } else {
         return this.releaseT < 0.0F ? 0.0F : 1.0F - smooth(this.releaseT / (this.fired ? 0.25F : 0.2F));
      }
   }

   float dip() {
      return this.drawing && this.drawT < 0.12F ? (float)Math.sin(Math.PI * this.drawT / 0.12F) : 0.0F;
   }

   float aim() {
      if (this.drawing) {
         return smooth(Math.min(1.0F, this.charge * 1.25F));
      } else if (this.releaseT < 0.0F) {
         return 0.0F;
      } else {
         float from = smooth(Math.min(1.0F, this.releasedPull * 1.25F));
         float hold = this.fired ? 0.12F : 0.0F;
         return from * (1.0F - smooth((this.releaseT - hold) / 0.3F));
      }
   }

   float tremble() {
      return this.drawing ? Math.min(1.0F, Math.max(0.0F, (this.fullT - 1.0F) / 1.5F)) : 0.0F;
   }

   private static float smooth(float u) {
      u = Math.max(0.0F, Math.min(1.0F, u));
      return u * u * (3.0F - 2.0F * u);
   }
}
