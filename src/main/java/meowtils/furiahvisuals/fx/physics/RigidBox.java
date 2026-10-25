package meowtils.furiahvisuals.fx.physics;

public final class RigidBox {
   public static final int N = 8;
   final double[] x = new double[8];
   final double[] y = new double[8];
   final double[] z = new double[8];
   final double[] ox = new double[8];
   final double[] oy = new double[8];
   final double[] oz = new double[8];
   final double[] qx = new double[8];
   final double[] qy = new double[8];
   final double[] qz = new double[8];
   public double hx;
   public double hy;
   public double hz;
   public double invMass = 1.0;
   public float friction = 0.6F;
   public float restitution = 0.3F;
   public double cx;
   public double cy;
   public double cz;
   public final double[] rot = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0};
   double pcx;
   double pcy;
   double pcz;
   final double[] q = new double[]{0.0, 0.0, 0.0, 1.0};
   final double[] pq = new double[]{0.0, 0.0, 0.0, 1.0};
   public boolean sleeping;
   int still;
   public boolean touched;
   private static final double SETTLE_SPEED = 0.35;
   private static final double SETTLE_SQ = 8.506944444444442E-6;
   private static final double[] W = new double[8];
   private static final double[] A = new double[9];
   private static final double[] QI = new double[4];

   public void step(BlockCollider world, double gravity, double damping) {
      this.integrate(0.008333333333333333, gravity, damping);
      world.collide(this);
      this.matchShape();
      world.separate(this);
      this.matchShape();
      this.settle();
      this.updateSleep();
   }

   public void settle() {
      if (this.touched && this.rigidStepSq() < 8.506944444444442E-6) {
         this.damp(0.25);
      }
   }

   public double corner(int i, int axis) {
      return axis == 0 ? this.x[i] : (axis == 1 ? this.y[i] : this.z[i]);
   }

   public void place(double cx, double cy, double cz, double[] r, double hx, double hy, double hz) {
      this.hx = hx;
      this.hy = hy;
      this.hz = hz;

      for (int i = 0; i < 8; i++) {
         this.qx[i] = ((i & 1) != 0 ? 1 : -1) * hx;
         this.qy[i] = ((i & 2) != 0 ? 1 : -1) * hy;
         this.qz[i] = ((i & 4) != 0 ? 1 : -1) * hz;
         this.x[i] = this.ox[i] = cx + r[0] * this.qx[i] + r[1] * this.qy[i] + r[2] * this.qz[i];
         this.y[i] = this.oy[i] = cy + r[3] * this.qx[i] + r[4] * this.qy[i] + r[5] * this.qz[i];
         this.z[i] = this.oz[i] = cz + r[6] * this.qx[i] + r[7] * this.qy[i] + r[8] * this.qz[i];
      }

      this.cx = this.pcx = cx;
      this.cy = this.pcy = cy;
      this.cz = this.pcz = cz;
      System.arraycopy(r, 0, this.rot, 0, 9);
      Quat.fromMatrix(r, this.q);
      System.arraycopy(this.q, 0, this.pq, 0, 4);
      this.sleeping = false;
      this.still = 0;
   }

   public void setVelocity(double vx, double vy, double vz, double wx, double wy, double wz, double dt) {
      for (int i = 0; i < 8; i++) {
         double rx = this.x[i] - this.cx;
         double ry = this.y[i] - this.cy;
         double rz = this.z[i] - this.cz;
         double px = vx + wy * rz - wz * ry;
         double py = vy + wz * rx - wx * rz;
         double pz = vz + wx * ry - wy * rx;
         this.ox[i] = this.x[i] - px * dt;
         this.oy[i] = this.y[i] - py * dt;
         this.oz[i] = this.z[i] - pz * dt;
      }

      this.sleeping = false;
      this.still = 0;
   }

   public void push(double px, double py, double pz, double vx, double vy, double vz, double dt) {
      double sum = 0.0;
      double[] w = W;

      for (int i = 0; i < 8; i++) {
         double dx = this.x[i] - px;
         double dy = this.y[i] - py;
         double dz = this.z[i] - pz;
         w[i] = 1.0 / (0.15 + Math.sqrt(dx * dx + dy * dy + dz * dz));
         sum += w[i];
      }

      int var25 = 0;

      while (var25 < 8) {
         double k = w[var25] / sum * 8.0 * dt;
         this.ox[var25] = this.ox[var25] - vx * k;
         this.oy[var25] = this.oy[var25] - vy * k;
         int n3 = var25++;
         this.oz[n3] = this.oz[n3] - vz * k;
      }

      this.sleeping = false;
      this.still = 0;
   }

   void integrate(double dt, double gravity, double damping) {
      this.pcx = this.cx;
      this.pcy = this.cy;
      this.pcz = this.cz;
      System.arraycopy(this.q, 0, this.pq, 0, 4);
      if (this.invMass != 0.0 && !this.sleeping) {
         double keep = 1.0 - damping;
         double g = -gravity * dt * dt;

         for (int i = 0; i < 8; i++) {
            double nx = this.x[i] + (this.x[i] - this.ox[i]) * keep;
            double ny = this.y[i] + (this.y[i] - this.oy[i]) * keep + g;
            double nz = this.z[i] + (this.z[i] - this.oz[i]) * keep;
            this.ox[i] = this.x[i];
            this.oy[i] = this.y[i];
            this.oz[i] = this.z[i];
            this.x[i] = nx;
            this.y[i] = ny;
            this.z[i] = nz;
         }

         this.touched = false;
      }
   }

   void matchShape() {
      if (this.invMass != 0.0 && !this.sleeping) {
         double mx = 0.0;
         double my = 0.0;
         double mz = 0.0;

         for (int i = 0; i < 8; i++) {
            mx += this.x[i];
            my += this.y[i];
            mz += this.z[i];
         }

         mx /= 8.0;
         my /= 8.0;
         mz /= 8.0;

         for (int k = 0; k < 9; k++) {
            A[k] = 0.0;
         }

         for (int var14 = 0; var14 < 8; var14++) {
            double px = this.x[var14] - mx;
            double py = this.y[var14] - my;
            double pz = this.z[var14] - mz;
            A[0] = A[0] + px * this.qx[var14];
            A[1] = A[1] + px * this.qy[var14];
            A[2] = A[2] + px * this.qz[var14];
            A[3] = A[3] + py * this.qx[var14];
            A[4] = A[4] + py * this.qy[var14];
            A[5] = A[5] + py * this.qz[var14];
            A[6] = A[6] + pz * this.qx[var14];
            A[7] = A[7] + pz * this.qy[var14];
            A[8] = A[8] + pz * this.qz[var14];
         }

         Quat.extractRotation(A, this.q, 6);
         Quat.toMatrix(this.q, this.rot);
         this.cx = mx;
         this.cy = my;
         this.cz = mz;

         for (int var15 = 0; var15 < 8; var15++) {
            this.x[var15] = mx + this.rot[0] * this.qx[var15] + this.rot[1] * this.qy[var15] + this.rot[2] * this.qz[var15];
            this.y[var15] = my + this.rot[3] * this.qx[var15] + this.rot[4] * this.qy[var15] + this.rot[5] * this.qz[var15];
            this.z[var15] = mz + this.rot[6] * this.qx[var15] + this.rot[7] * this.qy[var15] + this.rot[8] * this.qz[var15];
         }
      }
   }

   public double maxStepSq() {
      double m = 0.0;

      for (int i = 0; i < 8; i++) {
         double dx = this.x[i] - this.ox[i];
         double dy = this.y[i] - this.oy[i];
         double dz = this.z[i] - this.oz[i];
         m = Math.max(m, dx * dx + dy * dy + dz * dz);
      }

      return m;
   }

   public double rigidStepSq() {
      double dx = this.cx - this.pcx;
      double dy = this.cy - this.pcy;
      double dz = this.cz - this.pcz;
      double dot = Math.abs(this.q[0] * this.pq[0] + this.q[1] * this.pq[1] + this.q[2] * this.pq[2] + this.q[3] * this.pq[3]);
      double theta2 = 8.0 * Math.max(0.0, 1.0 - dot);
      return dx * dx + dy * dy + dz * dz + theta2 * (this.hx * this.hx + this.hy * this.hy + this.hz * this.hz);
   }

   void damp(double k) {
      double keep = 1.0 - k;

      for (int i = 0; i < 8; i++) {
         this.ox[i] = this.x[i] - (this.x[i] - this.ox[i]) * keep;
         this.oy[i] = this.y[i] - (this.y[i] - this.oy[i]) * keep;
         this.oz[i] = this.z[i] - (this.z[i] - this.oz[i]) * keep;
      }
   }

   void sleep() {
      this.sleeping = true;
      this.still = 0;

      for (int i = 0; i < 8; i++) {
         this.ox[i] = this.x[i];
         this.oy[i] = this.y[i];
         this.oz[i] = this.z[i];
      }

      this.pcx = this.cx;
      this.pcy = this.cy;
      this.pcz = this.cz;
      System.arraycopy(this.q, 0, this.pq, 0, 4);
   }

   void updateSleep() {
      if (this.invMass != 0.0 && !this.sleeping) {
         if (this.rigidStepSq() < 6.25E-6) {
            if (++this.still > 36) {
               this.sleep();
            }
         } else {
            this.still = 0;
         }
      }
   }

   public void interpolate(double alpha, double[] c, double[] r) {
      c[0] = this.pcx + (this.cx - this.pcx) * alpha;
      c[1] = this.pcy + (this.cy - this.pcy) * alpha;
      c[2] = this.pcz + (this.cz - this.pcz) * alpha;
      Quat.nlerp(this.pq, this.q, alpha, QI);
      Quat.toMatrix(QI, r);
   }

   public double speed(double dt) {
      double dx = this.cx - this.pcx;
      double dy = this.cy - this.pcy;
      double dz = this.cz - this.pcz;
      return Math.sqrt(dx * dx + dy * dy + dz * dz) / dt;
   }

   public void weights(double lx, double ly, double lz, double[] w) {
      double u = this.hx == 0.0 ? 0.5 : (lx / this.hx + 1.0) * 0.5;
      double v = this.hy == 0.0 ? 0.5 : (ly / this.hy + 1.0) * 0.5;
      double s = this.hz == 0.0 ? 0.5 : (lz / this.hz + 1.0) * 0.5;

      for (int i = 0; i < 8; i++) {
         w[i] = ((i & 1) != 0 ? u : 1.0 - u) * ((i & 2) != 0 ? v : 1.0 - v) * ((i & 4) != 0 ? s : 1.0 - s);
      }
   }

   public void point(double[] w, double[] out) {
      double px = 0.0;
      double py = 0.0;
      double pz = 0.0;

      for (int i = 0; i < 8; i++) {
         px += w[i] * this.x[i];
         py += w[i] * this.y[i];
         pz += w[i] * this.z[i];
      }

      out[0] = px;
      out[1] = py;
      out[2] = pz;
   }

   void movePoint(double[] w, double dx, double dy, double dz, double share) {
      double sum = 0.0;

      for (int i = 0; i < 8; i++) {
         sum += w[i] * w[i];
      }

      if (!(sum < 1.0E-12)) {
         double k = share / sum;

         for (int i = 0; i < 8; i++) {
            this.x[i] = this.x[i] + w[i] * dx * k;
            this.y[i] = this.y[i] + w[i] * dy * k;
            this.z[i] = this.z[i] + w[i] * dz * k;
         }

         if (this.sleeping && dx * dx + dy * dy + dz * dz > 1.0E-8) {
            this.sleeping = false;
            this.still = 0;
         }
      }
   }

   public double lowest() {
      double m = Double.MAX_VALUE;

      for (int i = 0; i < 8; i++) {
         m = Math.min(m, this.y[i]);
      }

      return m;
   }
}
