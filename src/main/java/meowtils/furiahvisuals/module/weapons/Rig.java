package meowtils.furiahvisuals.module.weapons;

final class Rig {
   static float maxLean = -0.62F;
   private static final float MIN_LEAN = 0.12F;
   private static final float PIXEL = 0.0625F;
   private static final float[] X = new float[3];
   private static final float[] Y = new float[3];
   private static final float[] Z = new float[3];
   private static final float[] T = new float[3];
   private static final float[] E = new float[3];
   private static final Mat4 TMP = new Mat4();

   private Rig() {
   }

   static void fist(float[] pos, float[] tip, float[] edge, Mat4 out) {
      basis(tip, edge, Y, X, Z);
      out.frame(X, Y, Z, pos[0], pos[1], pos[2]);
   }

   static void gripToFist(WeaponDef.GripPoint grip, float scale, Mat4 out) {
      out.scale(scale, scale, scale);
      T[0] = grip.ax;
      T[1] = grip.ay;
      T[2] = grip.az;
      E[0] = grip.ex;
      E[1] = grip.ey;
      E[2] = grip.ez;
      float[] my = new float[3];
      float[] mx = new float[3];
      float[] mz = new float[3];
      basis(T, E, my, mx, mz);
      Mat4 inv = TMP;
      inv.identity();
      inv.m[0] = mx[0];
      inv.m[4] = mx[1];
      inv.m[8] = mx[2];
      inv.m[1] = my[0];
      inv.m[5] = my[1];
      inv.m[9] = my[2];
      inv.m[2] = mz[0];
      inv.m[6] = mz[1];
      inv.m[10] = mz[2];
      out.mul(inv);
      out.translate(-grip.x, -grip.y, -grip.z);
   }

   static void basis(float[] main, float[] second, float[] y, float[] x, float[] z) {
      y[0] = main[0];
      y[1] = main[1];
      y[2] = main[2];
      Mat4.normalize(y);
      float d = Mat4.dot(second, y);
      x[0] = second[0] - y[0] * d;
      x[1] = second[1] - y[1] * d;
      x[2] = second[2] - y[2] * d;
      if (Mat4.normalize(x) < 1.0E-5F) {
         float[] fArray;
         if (Math.abs(y[1]) < 0.9F) {
            float[] fArray2 = new float[]{0.0F, 1.0F, 0.0F};
            fArray = fArray2;
            fArray2[2] = 0.0F;
         } else {
            float[] fArray3 = new float[]{1.0F, 0.0F, 0.0F};
            fArray = fArray3;
            fArray3[2] = 0.0F;
         }

         Mat4.cross(fArray, y, x);
         Mat4.normalize(x);
      }

      Mat4.cross(x, y, z);
      Mat4.normalize(z);
   }

   static void arm(float[] fist, float[] handle, float[] shoulder, boolean slim, boolean right, float scale, Mat4 out) {
      arm(fist, handle, shoulder, slim, right, scale, false, out);
   }

   static void arm(float[] fist, float[] handle, float[] shoulder, boolean slim, boolean right, float scale, boolean exact, Mat4 out) {
      float[] h = new float[]{handle[0], handle[1], handle[2]};
      Mat4.normalize(h);
      float[] d = new float[]{shoulder[0] - fist[0], shoulder[1] - fist[1], shoulder[2] - fist[2]};
      Mat4.normalize(d);
      float c = Mat4.dot(d, h);
      float cc = exact ? c : Math.max(maxLean, Math.min(0.12F, c));
      float[] a = new float[3];
      if (cc != c) {
         float[] fArray = new float[]{d[0] - h[0] * c, d[1] - h[1] * c, d[2] - h[2] * c};
         float[] perp = fArray;
         if (Mat4.normalize(fArray) < 1.0E-5F) {
            float[] fArray2;
            if (Math.abs(h[1]) < 0.9F) {
               float[] fArray3 = new float[]{0.0F, -1.0F, 0.0F};
               fArray2 = fArray3;
               fArray3[2] = 0.0F;
            } else {
               float[] fArray4 = new float[]{1.0F, 0.0F, 0.0F};
               fArray2 = fArray4;
               fArray4[2] = 0.0F;
            }

            perp = fArray2;
         }

         float s = (float)Math.sqrt(Math.max(0.0F, 1.0F - cc * cc));
         a[0] = perp[0] * s + h[0] * cc;
         a[1] = perp[1] * s + h[1] * cc;
         a[2] = perp[2] * s + h[2] * cc;
      } else {
         a[0] = d[0];
         a[1] = d[1];
         a[2] = d[2];
      }

      Mat4.normalize(a);
      float ha = Mat4.dot(h, a);
      float[] p = new float[]{h[0] - a[0] * ha, h[1] - a[1] * ha, h[2] - a[2] * ha};
      Mat4.normalize(p);
      Y[0] = -a[0];
      Y[1] = -a[1];
      Y[2] = -a[2];
      Z[0] = -p[0];
      Z[1] = -p[1];
      Z[2] = -p[2];
      Mat4.cross(Y, Z, X);
      Mat4.normalize(X);
      out.frame(X, Y, Z, fist[0], fist[1], fist[2]);
      float k = 0.0625F * scale;
      out.scale(k, k, k);
      out.translate(-ArmRenderer.fistX(slim, right), -ArmRenderer.FIST_Y, -0.0F);
   }

   static void point(Mat4 m, float x, float y, float z, float[] out) {
      float[] r = new float[4];
      m.transform(x, y, z, r);
      out[0] = r[0];
      out[1] = r[1];
      out[2] = r[2];
   }
}
