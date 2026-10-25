package meowtils.furiahvisuals.fx.physics;

import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.PlayerModel;

public final class Ragdoll {
   public final RigidBox[] parts = new RigidBox[6];
   public float gravity = 26.0F;
   public float damping = 0.0035F;
   public float stiffness = 0.15F;
   private static final int JOINTS = 5;
   private static final int[] CHILD = new int[]{0, 2, 3, 4, 5};
   private static final double[] LIMIT = new double[]{50.0, 150.0, 150.0, 95.0, 95.0};
   public final boolean[] attached = new boolean[]{true, true, true, true, true};
   private final double[][] wChild = new double[5][8];
   private final double[][] wParent = new double[5][8];
   private final double[][] wFar = new double[5][8];
   private final double[][] restDir = new double[5][3];
   private final double[] boneLen = new double[5];
   private final double[] cosLimit = new double[5];
   private final double[] pa = new double[3];
   private final double[] pb = new double[3];
   private final double[] pf = new double[3];
   private static final double STILL = 6.25E-6;
   private static final int STILL_STEPS = 36;
   private int still;
   private boolean groupAsleep;
   private final double[] anchor = new double[54];
   private static final double DRIFT = 0.02;
   private static final int TORSO_ONLY_AFTER = 120;
   private int steps;
   private static double SX;
   private static double SY;
   private static double SZ;
   private final double[] ic = new double[3];

   public Ragdoll() {
      for (int i = 0; i < this.parts.length; i++) {
         this.parts[i] = new RigidBox();
      }
   }

   public void build(BodyProxy.Part[] proxy, boolean slim) {
      this.groupAsleep = false;
      this.still = 0;
      this.steps = 0;

      for (int i = 0; i < 6; i++) {
         PlayerModel.Box b = PlayerModel.base(i, slim);
         BodyProxy.Part p = proxy[i];
         double s = 0.05859375 * p.scale;
         RigidBox box = this.parts[i];
         box.place(p.x, p.y, p.z, p.r, b.w / 2.0 * s, b.h / 2.0 * s, b.d / 2.0 * s);
         box.invMass = i == 1 ? 0.45 : (i == 0 ? 0.9 : 0.8);
      }

      RigidBox body = this.parts[1];

      for (int j = 0; j < 5; j++) {
         this.attached[j] = true;
         int c = CHILD[j];
         RigidBox child = this.parts[c];
         PlayerModel.Box b = PlayerModel.base(c, slim);
         double s = 0.05859375 * proxy[c].scale;
         double pxl = -b.cx() * s;
         double pyl = -b.cy() * s;
         double pzl = -b.cz() * s;
         double fyl = (c == 0 ? b.y : b.y + b.h) * s - b.cy() * s;
         child.weights(pxl, pyl, pzl, this.wChild[j]);
         child.weights(0.0, fyl, 0.0, this.wFar[j]);
         double wx = child.cx + child.rot[0] * pxl + child.rot[1] * pyl + child.rot[2] * pzl;
         double wy = child.cy + child.rot[3] * pxl + child.rot[4] * pyl + child.rot[5] * pzl;
         double wz = child.cz + child.rot[6] * pxl + child.rot[7] * pyl + child.rot[8] * pzl;
         double dx = wx - body.cx;
         double dy = wy - body.cy;
         double dz = wz - body.cz;
         double lx = body.rot[0] * dx + body.rot[3] * dy + body.rot[6] * dz;
         double ly = body.rot[1] * dx + body.rot[4] * dy + body.rot[7] * dz;
         double lz = body.rot[2] * dx + body.rot[5] * dy + body.rot[8] * dz;
         body.weights(lx, ly, lz, this.wParent[j]);
         double bx = 0.0 - pxl;
         double by = fyl - pyl;
         double bz = 0.0 - pzl;
         double ex = child.rot[0] * bx + child.rot[1] * by + child.rot[2] * bz;
         double ey = child.rot[3] * bx + child.rot[4] * by + child.rot[5] * bz;
         double ez = child.rot[6] * bx + child.rot[7] * by + child.rot[8] * bz;
         double len;
         this.boneLen[j] = len = Math.sqrt(ex * ex + ey * ey + ez * ez);
         this.restDir[j][0] = (body.rot[0] * ex + body.rot[3] * ey + body.rot[6] * ez) / len;
         this.restDir[j][1] = (body.rot[1] * ex + body.rot[4] * ey + body.rot[7] * ez) / len;
         this.restDir[j][2] = (body.rot[2] * ex + body.rot[5] * ey + body.rot[8] * ez) / len;
         this.cosLimit[j] = Math.cos(Math.toRadians(LIMIT[j]));
      }
   }

   public void detach(int part) {
      for (int j = 0; j < 5; j++) {
         if (CHILD[j] == part) {
            this.attached[j] = false;
         }
      }

      this.parts[part].sleeping = false;
   }

   public boolean isAttached(int part) {
      for (int j = 0; j < 5; j++) {
         if (CHILD[j] == part) {
            return this.attached[j];
         }
      }

      return true;
   }

   public void fling(double ix, double iy, double iz, double vx, double vy, double vz, double dx, double dy, double dz, double strength, double spin) {
      double ax = dz;
      double az = -dx;
      double al = Math.sqrt(dz * dz + az * az);
      if (al < 1.0E-6) {
         ax = 1.0;
         az = 0.0;
         al = 1.0;
      }

      ax /= al;
      az /= al;
      double mx = 0.0;
      double my = 0.0;
      double mz = 0.0;
      double mass = 0.0;

      for (int i = 0; i < this.parts.length; i++) {
         if (this.inGroup(i)) {
            RigidBox b = this.parts[i];
            double m = b.invMass > 0.0 ? 1.0 / b.invMass : 0.0;
            mx += b.cx * m;
            my += b.cy * m;
            mz += b.cz * m;
            mass += m;
         }
      }

      if (mass > 0.0) {
         mx /= mass;
         my /= mass;
         mz /= mass;
      }

      double wx = ax * spin;
      double wz = az * spin;

      for (int ixx = 0; ixx < this.parts.length; ixx++) {
         if (this.inGroup(ixx)) {
            RigidBox b = this.parts[ixx];
            double ex = b.cx - ix;
            double ey = b.cy - iy;
            double ez = b.cz - iz;
            double d = Math.sqrt(ex * ex + ey * ey + ez * ez);
            double k = strength * (0.55 + 0.45 / (1.0 + d * 2.2));
            double rx = b.cx - mx;
            double ry = b.cy - my;
            double rz = b.cz - mz;
            double tx = -wz * ry;
            double ty = wz * rx - wx * rz;
            double tz = wx * ry;
            b.setVelocity(vx + dx * k + tx, vy + dy * k + strength * 0.22 + ty, vz + dz * k + tz, wx, 0.0, wz, 0.008333333333333333);
         }
      }
   }

   public void step(BlockCollider world) {
      double dt = 0.008333333333333333;
      RigidBox body = this.parts[1];

      for (int i = 0; i < this.parts.length; i++) {
         RigidBox b = this.parts[i];
         if (!this.groupAsleep || !this.inGroup(i)) {
            b.integrate(dt, this.gravity, this.damping);
            world.collide(b);
         }
      }

      if (!this.groupAsleep) {
         for (int it = 0; it < 4; it++) {
            for (int j = 0; j < 5; j++) {
               if (this.attached[j]) {
                  this.joint(j, body, this.parts[CHILD[j]]);
               }
            }

            for (int i2 = 0; i2 < this.parts.length; i2++) {
               if (this.inGroup(i2)) {
                  this.parts[i2].matchShape();
                  world.separate(this.parts[i2]);
               }
            }
         }
      }

      for (int var8 = 0; var8 < this.parts.length; var8++) {
         RigidBox b = this.parts[var8];
         if (!this.groupAsleep || !this.inGroup(var8)) {
            b.matchShape();
            world.separate(b);
            b.matchShape();
            if (!this.inGroup(var8)) {
               settle(b);
               b.updateSleep();
            }
         }
      }

      if (!this.groupAsleep) {
         this.sleepGroup();
      }
   }

   private boolean inGroup(int part) {
      return part == 1 || this.isAttached(part);
   }

   private static void settle(RigidBox b) {
      b.settle();
   }

   private void sleepGroup() {
      this.steps++;
      double drift = 0.0;

      for (int i = 0; i < this.parts.length; i++) {
         if (this.inGroup(i)) {
            settle(this.parts[i]);
            if (this.steps < 120 || i == 1) {
               drift = Math.max(drift, this.drift(i));
            }
         }
      }

      if (drift < 0.02 && this.still > 0) {
         if (++this.still > 36) {
            this.groupAsleep = true;

            for (int var5 = 0; var5 < this.parts.length; var5++) {
               if (this.inGroup(var5)) {
                  this.parts[var5].sleep();
               }
            }
         }
      } else {
         this.still = 1;

         for (int var4 = 0; var4 < this.parts.length; var4++) {
            this.mark(var4);
         }
      }
   }

   private void mark(int i) {
      RigidBox b = this.parts[i];
      double s = Math.max(b.hx, Math.max(b.hy, b.hz));
      int o = i * 9;
      this.anchor[o] = b.cx;
      this.anchor[o + 1] = b.cy;
      this.anchor[o + 2] = b.cz;
      this.anchor[o + 3] = b.cx + b.rot[0] * s;
      this.anchor[o + 4] = b.cy + b.rot[3] * s;
      this.anchor[o + 5] = b.cz + b.rot[6] * s;
      this.anchor[o + 6] = b.cx + b.rot[1] * s;
      this.anchor[o + 7] = b.cy + b.rot[4] * s;
      this.anchor[o + 8] = b.cz + b.rot[7] * s;
   }

   private double drift(int i) {
      RigidBox b = this.parts[i];
      double s = Math.max(b.hx, Math.max(b.hy, b.hz));
      int o = i * 9;
      double d = dist(b.cx, b.cy, b.cz, this.anchor[o], this.anchor[o + 1], this.anchor[o + 2]);
      d = Math.max(d, dist(b.cx + b.rot[0] * s, b.cy + b.rot[3] * s, b.cz + b.rot[6] * s, this.anchor[o + 3], this.anchor[o + 4], this.anchor[o + 5]));
      return Math.max(d, dist(b.cx + b.rot[1] * s, b.cy + b.rot[4] * s, b.cz + b.rot[7] * s, this.anchor[o + 6], this.anchor[o + 7], this.anchor[o + 8]));
   }

   private static double dist(double ax, double ay, double az, double bx, double by, double bz) {
      double dx = ax - bx;
      double dy = ay - by;
      double dz = az - bz;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private void joint(int j, RigidBox body, RigidBox child) {
      if (!body.sleeping || !child.sleeping) {
         child.point(this.wChild[j], this.pa);
         body.point(this.wParent[j], this.pb);
         double dx = this.pb[0] - this.pa[0];
         double dy = this.pb[1] - this.pa[1];
         double dz = this.pb[2] - this.pa[2];
         double mc = child.invMass;
         double mp = body.invMass;
         double sum = mc + mp;
         if (sum > 0.0) {
            child.movePoint(this.wChild[j], dx, dy, dz, mc / sum);
            body.movePoint(this.wParent[j], -dx, -dy, -dz, mp / sum);
         }

         child.point(this.wChild[j], this.pa);
         child.point(this.wFar[j], this.pf);
         double bx = this.pf[0] - this.pa[0];
         double by = this.pf[1] - this.pa[1];
         double bz = this.pf[2] - this.pa[2];
         double bl = Math.sqrt(bx * bx + by * by + bz * bz);
         if (!(bl < 1.0E-9)) {
            double[] r = this.restDir[j];
            double rx = body.rot[0] * r[0] + body.rot[1] * r[1] + body.rot[2] * r[2];
            double ry = body.rot[3] * r[0] + body.rot[4] * r[1] + body.rot[5] * r[2];
            double rz = body.rot[6] * r[0] + body.rot[7] * r[1] + body.rot[8] * r[2];
            double var52;
            double var53;
            double var54;
            double cos = (var52 = bx / bl) * rx + (var53 = by / bl) * ry + (var54 = bz / bl) * rz;
            double tx = var52;
            double ty = var53;
            double tz = var54;
            boolean move = false;
            if (cos < this.cosLimit[j]) {
               slerpDirection(rx, ry, rz, var52, var53, var54, Math.acos(this.cosLimit[j]));
               tx = SX;
               ty = SY;
               tz = SZ;
               move = true;
            }

            double k = child.touched ? 0.0 : this.stiffness * this.stiffness * 0.06;
            if (k > 1.0E-4) {
               tx += (rx - tx) * k;
               ty += (ry - ty) * k;
               tz += (rz - tz) * k;
               move = true;
            }

            double tl;
            if (move && (tl = Math.sqrt(tx * tx + ty * ty + tz * tz)) > 1.0E-9) {
               double fx = this.pa[0] + tx / tl * this.boneLen[j];
               double fy = this.pa[1] + ty / tl * this.boneLen[j];
               double fz = this.pa[2] + tz / tl * this.boneLen[j];
               child.movePoint(this.wFar[j], fx - this.pf[0], fy - this.pf[1], fz - this.pf[2], 0.6);
            }
         }
      }
   }

   private static void slerpDirection(double ax, double ay, double az, double bx, double by, double bz, double angle) {
      double d = ax * bx + ay * by + az * bz;
      double px = bx - ax * d;
      double py = by - ay * d;
      double pz = bz - az * d;
      double pl = Math.sqrt(px * px + py * py + pz * pz);
      if (pl < 1.0E-9) {
         px = -ay;
         py = ax;
         pz = 0.0;
         pl = Math.sqrt(px * px + ax * ax);
         if (pl < 1.0E-9) {
            px = 0.0;
            py = -az;
            pz = ay;
            pl = Math.sqrt(py * py + ay * ay);
         }
      }

      double c = Math.cos(angle);
      double s = Math.sin(angle);
      double var28;
      SX = ax * c + (var28 = px / pl) * s;
      double var29;
      SY = ay * c + (var29 = py / pl) * s;
      double var30;
      SZ = az * c + (var30 = pz / pl) * s;
   }

   public boolean asleep() {
      for (int i = 0; i < this.parts.length; i++) {
         if (this.inGroup(i) ? !this.groupAsleep : !this.parts[i].sleeping) {
            return false;
         }
      }

      return true;
   }

   public void hold() {
      this.groupAsleep = true;

      for (int i = 0; i < this.parts.length; i++) {
         if (this.inGroup(i)) {
            this.parts[i].sleep();
         }
      }
   }

   public boolean isHeld() {
      return this.groupAsleep;
   }

   public void wake() {
      this.groupAsleep = false;
      this.still = 0;
      this.steps = 0;

      for (RigidBox b : this.parts) {
         b.sleeping = false;
      }
   }

   public void apply(BodyProxy.Part[] proxy, double alpha) {
      for (int i = 0; i < this.parts.length; i++) {
         this.parts[i].interpolate(alpha, this.ic, proxy[i].r);
         proxy[i].x = this.ic[0];
         proxy[i].y = this.ic[1];
         proxy[i].z = this.ic[2];
      }
   }
}
