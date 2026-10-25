package meowtils.furiahvisuals.module.sky;

import java.util.Random;

final class Meteors {
   private static final int MAX = 4;
   private final Random random = new Random();
   private final float[] start = new float[12];
   private final float[] travel = new float[12];
   private final float[] speed = new float[4];
   private final float[] length = new float[4];
   private final float[] age = new float[4];
   private final float[] life = new float[4];
   private final boolean[] alive = new boolean[4];
   private float showerLeft;
   private float nextShower = 180.0F + this.random.nextFloat() * 240.0F;
   private final float[] heads = new float[16];
   private final float[] tails = new float[16];

   void update(float dt, float frequency) {
      for (int i = 0; i < 4; i++) {
         if (this.alive[i]) {
            this.age[i] = this.age[i] + dt;
            if (this.age[i] >= this.life[i]) {
               this.alive[i] = false;
            }
         }
      }

      if (!(frequency <= 0.0F) && !(dt <= 0.0F)) {
         this.nextShower -= dt;
         if (this.nextShower <= 0.0F) {
            this.showerLeft = 25.0F;
            this.nextShower = 240.0F + this.random.nextFloat() * 360.0F;
         }

         this.showerLeft = Math.max(0.0F, this.showerLeft - dt);
         float perSecond = frequency * 0.3F * (this.showerLeft > 0.0F ? 7.0F : 1.0F);
         if (this.random.nextFloat() < perSecond * dt) {
            this.spawn();
         }
      }
   }

   private void spawn() {
      for (int i = 0; i < 4; i++) {
         if (!this.alive[i]) {
            double az = this.random.nextDouble() * Math.PI * 2.0;
            double el = Math.toRadians(20.0 + this.random.nextDouble() * 50.0);
            float sx = (float)(Math.sin(az) * Math.cos(el));
            float sy = (float)Math.sin(el);
            float sz = (float)(-Math.cos(az) * Math.cos(el));
            float l = (float)Math.sqrt(sx * sx + sz * sz);
            float ex = -sz / l;
            float ez = sx / l;
            float ux = -ez * sy;
            float uy = ez * sx - ex * sz;
            float uz = ex * sy;
            double a = Math.toRadians(-60.0 - this.random.nextDouble() * 60.0);
            if (this.random.nextBoolean()) {
               a = Math.PI - a;
            }

            float tx = (float)(Math.cos(a) * ex + Math.sin(a) * ux);
            float ty = (float)(Math.sin(a) * uy);
            float tz = (float)(Math.cos(a) * ez + Math.sin(a) * uz);
            float tl = (float)Math.sqrt(tx * tx + ty * ty + tz * tz);
            this.start[i * 3] = sx;
            this.start[i * 3 + 1] = sy;
            this.start[i * 3 + 2] = sz;
            this.travel[i * 3] = tx / tl;
            this.travel[i * 3 + 1] = ty / tl;
            this.travel[i * 3 + 2] = tz / tl;
            this.speed[i] = (float)Math.toRadians(28.0 + this.random.nextDouble() * 22.0);
            this.length[i] = (float)Math.toRadians(5.0 + this.random.nextDouble() * 8.0);
            this.life[i] = 0.45F + this.random.nextFloat() * 0.6F;
            this.age[i] = 0.0F;
            this.alive[i] = true;
            return;
         }
      }
   }

   float[] heads() {
      this.fill();
      return this.heads;
   }

   float[] tails() {
      return this.tails;
   }

   private void fill() {
      for (int i = 0; i < 4; i++) {
         if (!this.alive[i]) {
            put(this.heads, i, 0.0F, 1.0F, 0.0F, 0.0F);
            put(this.tails, i, 1.0F, 0.0F, 0.0F, 0.1F);
         } else {
            float s = this.speed[i] * this.age[i];
            float c = (float)Math.cos(s);
            float n = (float)Math.sin(s);
            float hx = this.start[i * 3] * c + this.travel[i * 3] * n;
            float hy = this.start[i * 3 + 1] * c + this.travel[i * 3 + 1] * n;
            float hz = this.start[i * 3 + 2] * c + this.travel[i * 3 + 2] * n;
            float vx = this.travel[i * 3] * c - this.start[i * 3] * n;
            float vy = this.travel[i * 3 + 1] * c - this.start[i * 3 + 1] * n;
            float vz = this.travel[i * 3 + 2] * c - this.start[i * 3 + 2] * n;
            float t = this.age[i] / this.life[i];
            float brightness = Math.min(1.0F, t * 8.0F) * (1.0F - smooth(0.55F, 1.0F, t));
            put(this.heads, i, hx, hy, hz, brightness);
            put(this.tails, i, vx, vy, vz, Math.max(0.01F, Math.min(this.length[i], s)));
         }
      }
   }

   private static void put(float[] a, int i, float x, float y, float z, float w) {
      a[i * 4] = x;
      a[i * 4 + 1] = y;
      a[i * 4 + 2] = z;
      a[i * 4 + 3] = w;
   }

   private static float smooth(float a, float b, float x) {
      float t = Math.max(0.0F, Math.min(1.0F, (x - a) / (b - a)));
      return t * t * (3.0F - 2.0F * t);
   }

   void clear() {
      for (int i = 0; i < 4; i++) {
         this.alive[i] = false;
      }
   }
}
