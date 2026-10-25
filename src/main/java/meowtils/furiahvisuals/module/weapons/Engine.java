package meowtils.furiahvisuals.module.weapons;

import java.util.Random;

final class Engine {
   private static final float HOLD = 0.25F;
   private static final float FALL = 0.35F;
   static final float CRAWL = 0.12F;
   static final float RACE = 4.6F;
   boolean running = true;
   float throttle;
   float phase;
   private float sinceRev = 10.0F;
   private float sinceStart = 10.0F;
   final float[] shake = new float[3];
   private final Random random = new Random();
   boolean started;
   boolean cut;

   void rev() {
      if (this.running) {
         this.sinceRev = 0.0F;
      }
   }

   void draw() {
      this.reset();
      this.started = true;
      this.sinceStart = 0.0F;
   }

   void start() {
      if (!this.running) {
         this.running = true;
         this.started = true;
         this.sinceStart = 0.0F;
      }
   }

   void caught() {
      if (!this.running) {
         this.running = true;
         this.sinceStart = -0.3F;
      }
   }

   void stop() {
      if (this.running) {
         this.running = false;
         this.cut = true;
         this.throttle = 0.0F;
      }
   }

   float idleShare() {
      float t = Math.max(0.0F, Math.min(1.0F, (this.sinceStart - 0.35F) / 0.4F));
      return t * t * (3.0F - 2.0F * t);
   }

   void update(float dt, boolean vibration) {
      this.sinceRev += dt;
      this.sinceStart = Math.min(10.0F, this.sinceStart + dt);
      float target = !this.running ? 0.0F : (this.sinceRev < 0.25F ? 1.0F : Math.max(0.0F, 1.0F - (this.sinceRev - 0.25F) / 0.35F));
      this.throttle = target > this.throttle ? Math.min(target, this.throttle + dt / 0.06F) : target;
      float speed = this.running ? 0.12F + 4.48F * this.throttle : 0.0F;
      this.phase = (this.phase + speed * dt) % 1000.0F;
      float amp = this.running && vibration ? 9.0E-4F + 0.0048F * this.throttle : 0.0F;
      if (dt > 0.0F || amp == 0.0F) {
         this.shake[0] = (this.random.nextFloat() - 0.5F) * 2.0F * amp;
         this.shake[1] = (this.random.nextFloat() - 0.5F) * 2.0F * amp;
         this.shake[2] = (this.random.nextFloat() - 0.5F) * amp;
      }
   }

   void reset() {
      this.running = true;
      this.throttle = 0.0F;
      this.sinceRev = 10.0F;
      this.sinceStart = 10.0F;
      this.started = false;
      this.cut = false;
      this.shake[2] = 0.0F;
      this.shake[1] = 0.0F;
      this.shake[0] = 0.0F;
   }
}
