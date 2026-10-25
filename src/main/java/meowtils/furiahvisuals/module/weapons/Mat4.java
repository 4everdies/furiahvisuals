package meowtils.furiahvisuals.module.weapons;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

final class Mat4 {
   final float[] m = new float[16];
   private static final float[] T = new float[16];
   private static final FloatBuffer BUF = BufferUtils.createFloatBuffer(16);
   private static final float[] R = new float[16];

   Mat4() {
      this.identity();
   }

   Mat4 identity() {
      for (int i = 0; i < 16; i++) {
         this.m[i] = i % 5 == 0 ? 1.0F : 0.0F;
      }

      return this;
   }

   Mat4 set(Mat4 o) {
      System.arraycopy(o.m, 0, this.m, 0, 16);
      return this;
   }

   Mat4 mul(Mat4 o) {
      return this.mul(o.m);
   }

   Mat4 mul(float[] b) {
      float[] a = this.m;

      for (int c = 0; c < 4; c++) {
         for (int r = 0; r < 4; r++) {
            T[c * 4 + r] = a[r] * b[c * 4] + a[4 + r] * b[c * 4 + 1] + a[8 + r] * b[c * 4 + 2] + a[12 + r] * b[c * 4 + 3];
         }
      }

      System.arraycopy(T, 0, this.m, 0, 16);
      return this;
   }

   Mat4 premul(Mat4 o) {
      float[] a = o.m;
      float[] b = this.m;

      for (int c = 0; c < 4; c++) {
         for (int r = 0; r < 4; r++) {
            T[c * 4 + r] = a[r] * b[c * 4] + a[4 + r] * b[c * 4 + 1] + a[8 + r] * b[c * 4 + 2] + a[12 + r] * b[c * 4 + 3];
         }
      }

      System.arraycopy(T, 0, this.m, 0, 16);
      return this;
   }

   Mat4 translate(float x, float y, float z) {
      this.m[12] = this.m[12] + (this.m[0] * x + this.m[4] * y + this.m[8] * z);
      this.m[13] = this.m[13] + (this.m[1] * x + this.m[5] * y + this.m[9] * z);
      this.m[14] = this.m[14] + (this.m[2] * x + this.m[6] * y + this.m[10] * z);
      this.m[15] = this.m[15] + (this.m[3] * x + this.m[7] * y + this.m[11] * z);
      return this;
   }

   Mat4 scale(float x, float y, float z) {
      for (int r = 0; r < 4; r++) {
         this.m[r] = this.m[r] * x;
         int n2 = 4 + r;
         this.m[n2] = this.m[n2] * y;
         int n3 = 8 + r;
         this.m[n3] = this.m[n3] * z;
      }

      return this;
   }

   Mat4 rotate(float deg, float ax, float ay, float az) {
      if (deg == 0.0F) {
         return this;
      } else {
         float a = (float)Math.toRadians(deg);
         float c = (float)Math.cos(a);
         float s = (float)Math.sin(a);
         float t = 1.0F - c;
         float l = (float)Math.sqrt(ax * ax + ay * ay + az * az);
         if (l < 1.0E-9F) {
            return this;
         } else {
            float[] r = R;
            float var11;
            r[0] = t * (var11 = ax / l) * var11 + c;
            float var12;
            float var13;
            r[1] = t * var11 * (var12 = ay / l) + s * (var13 = az / l);
            r[2] = t * var11 * var13 - s * var12;
            r[3] = 0.0F;
            r[4] = t * var11 * var12 - s * var13;
            r[5] = t * var12 * var12 + c;
            r[6] = t * var12 * var13 + s * var11;
            r[7] = 0.0F;
            r[8] = t * var11 * var13 + s * var12;
            r[9] = t * var12 * var13 - s * var11;
            r[10] = t * var13 * var13 + c;
            r[11] = 0.0F;
            r[14] = 0.0F;
            r[13] = 0.0F;
            r[12] = 0.0F;
            r[15] = 1.0F;
            return this.mul(r);
         }
      }
   }

   Mat4 rotate(float[] q) {
      float x = q[0];
      float y = q[1];
      float z = q[2];
      float w = q[3];
      float[] r = R;
      r[0] = 1.0F - 2.0F * (y * y + z * z);
      r[1] = 2.0F * (x * y + w * z);
      r[2] = 2.0F * (x * z - w * y);
      r[3] = 0.0F;
      r[4] = 2.0F * (x * y - w * z);
      r[5] = 1.0F - 2.0F * (x * x + z * z);
      r[6] = 2.0F * (y * z + w * x);
      r[7] = 0.0F;
      r[8] = 2.0F * (x * z + w * y);
      r[9] = 2.0F * (y * z - w * x);
      r[10] = 1.0F - 2.0F * (x * x + y * y);
      r[11] = 0.0F;
      r[14] = 0.0F;
      r[13] = 0.0F;
      r[12] = 0.0F;
      r[15] = 1.0F;
      return this.mul(r);
   }

   Mat4 frame(float[] x, float[] y, float[] z, float px, float py, float pz) {
      this.m[0] = x[0];
      this.m[1] = x[1];
      this.m[2] = x[2];
      this.m[3] = 0.0F;
      this.m[4] = y[0];
      this.m[5] = y[1];
      this.m[6] = y[2];
      this.m[7] = 0.0F;
      this.m[8] = z[0];
      this.m[9] = z[1];
      this.m[10] = z[2];
      this.m[11] = 0.0F;
      this.m[12] = px;
      this.m[13] = py;
      this.m[14] = pz;
      this.m[15] = 1.0F;
      return this;
   }

   Mat4 mirrorOuterX() {
      this.m[0] = -this.m[0];
      this.m[4] = -this.m[4];
      this.m[8] = -this.m[8];
      this.m[12] = -this.m[12];
      return this;
   }

   void transform(float x, float y, float z, float[] out) {
      out[0] = this.m[0] * x + this.m[4] * y + this.m[8] * z + this.m[12];
      out[1] = this.m[1] * x + this.m[5] * y + this.m[9] * z + this.m[13];
      out[2] = this.m[2] * x + this.m[6] * y + this.m[10] * z + this.m[14];
      out[3] = this.m[3] * x + this.m[7] * y + this.m[11] * z + this.m[15];
   }

   void direction(float x, float y, float z, float[] out) {
      out[0] = this.m[0] * x + this.m[4] * y + this.m[8] * z;
      out[1] = this.m[1] * x + this.m[5] * y + this.m[9] * z;
      out[2] = this.m[2] * x + this.m[6] * y + this.m[10] * z;
   }

   float tx() {
      return this.m[12];
   }

   float ty() {
      return this.m[13];
   }

   float tz() {
      return this.m[14];
   }

   boolean mirrored() {
      float d = this.m[0] * (this.m[5] * this.m[10] - this.m[9] * this.m[6])
         - this.m[4] * (this.m[1] * this.m[10] - this.m[9] * this.m[2])
         + this.m[8] * (this.m[1] * this.m[6] - this.m[5] * this.m[2]);
      return d < 0.0F;
   }

   FloatBuffer buffer() {
      ((Buffer)BUF).clear();
      BUF.put(this.m);
      ((Buffer)BUF).flip();
      return BUF;
   }

   Mat4 perspective(float fovY, float aspect, float near, float far) {
      float f = (float)(1.0 / Math.tan(Math.toRadians(fovY) / 2.0));
      this.identity();
      this.m[0] = f / aspect;
      this.m[5] = f;
      this.m[10] = (far + near) / (near - far);
      this.m[11] = -1.0F;
      this.m[14] = 2.0F * far * near / (near - far);
      this.m[15] = 0.0F;
      return this;
   }

   Mat4 screenAdjust(float dx, float dy, float sx, float sy) {
      for (int c = 0; c < 4; c++) {
         float w = this.m[c * 4 + 3];
         this.m[c * 4] = sx * (this.m[c * 4] + dx * w);
         this.m[c * 4 + 1] = sy * (this.m[c * 4 + 1] + dy * w);
      }

      return this;
   }

   static void euler(float xDeg, float yDeg, float zDeg, float[] out) {
      double hx = Math.toRadians(xDeg) / 2.0;
      double hy = Math.toRadians(yDeg) / 2.0;
      double hz = Math.toRadians(zDeg) / 2.0;
      double cx = Math.cos(hx);
      double sx = Math.sin(hx);
      double cy = Math.cos(hy);
      double sy = Math.sin(hy);
      double cz = Math.cos(hz);
      double sz = Math.sin(hz);
      out[0] = (float)(cz * cy * sx - sz * sy * cx);
      out[1] = (float)(cz * sy * cx + sz * cy * sx);
      out[2] = (float)(sz * cy * cx - cz * sy * sx);
      out[3] = (float)(cz * cy * cx + sz * sy * sx);
   }

   static void qmul(float[] a, float[] b, float[] out) {
      float x = a[3] * b[0] + a[0] * b[3] + a[1] * b[2] - a[2] * b[1];
      float y = a[3] * b[1] - a[0] * b[2] + a[1] * b[3] + a[2] * b[0];
      float z = a[3] * b[2] + a[0] * b[1] - a[1] * b[0] + a[2] * b[3];
      float w = a[3] * b[3] - a[0] * b[0] - a[1] * b[1] - a[2] * b[2];
      out[0] = x;
      out[1] = y;
      out[2] = z;
      out[3] = w;
   }

   static void slerp(float[] a, float[] b, float t, float[] out) {
      float dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
      float sign = 1.0F;
      if (dot < 0.0F) {
         dot = -dot;
         sign = -1.0F;
      }

      float kb;
      float ka;
      if (dot > 0.9995F) {
         ka = 1.0F - t;
         kb = t * sign;
      } else {
         double th = Math.acos(dot);
         double s = Math.sin(th);
         ka = (float)(Math.sin((1.0F - t) * th) / s);
         kb = (float)(Math.sin(t * th) / s) * sign;
      }

      out[0] = a[0] * ka + b[0] * kb;
      out[1] = a[1] * ka + b[1] * kb;
      out[2] = a[2] * ka + b[2] * kb;
      out[3] = a[3] * ka + b[3] * kb;
      qnormalize(out);
   }

   static void qnormalize(float[] q) {
      float l = (float)Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
      if (l < 1.0E-9F) {
         q[2] = 0.0F;
         q[1] = 0.0F;
         q[0] = 0.0F;
         q[3] = 1.0F;
      } else {
         q[0] /= l;
         q[1] /= l;
         q[2] /= l;
         q[3] /= l;
      }
   }

   static void fromBasis(float[] x, float[] y, float[] z, float[] out) {
      float m00 = x[0];
      float m10 = x[1];
      float m20 = x[2];
      float m01 = y[0];
      float m11 = y[1];
      float m21 = y[2];
      float m02 = z[0];
      float m12 = z[1];
      float m22 = z[2];
      float trace = m00 + m11 + m22;
      if (trace > 0.0F) {
         float s = (float)Math.sqrt(trace + 1.0F) * 2.0F;
         out[3] = 0.25F * s;
         out[0] = (m21 - m12) / s;
         out[1] = (m02 - m20) / s;
         out[2] = (m10 - m01) / s;
      } else if (m00 > m11 && m00 > m22) {
         float s = (float)Math.sqrt(1.0F + m00 - m11 - m22) * 2.0F;
         out[3] = (m21 - m12) / s;
         out[0] = 0.25F * s;
         out[1] = (m01 + m10) / s;
         out[2] = (m02 + m20) / s;
      } else if (m11 > m22) {
         float s = (float)Math.sqrt(1.0F + m11 - m00 - m22) * 2.0F;
         out[3] = (m02 - m20) / s;
         out[0] = (m01 + m10) / s;
         out[1] = 0.25F * s;
         out[2] = (m12 + m21) / s;
      } else {
         float s = (float)Math.sqrt(1.0F + m22 - m00 - m11) * 2.0F;
         out[3] = (m10 - m01) / s;
         out[0] = (m02 + m20) / s;
         out[1] = (m12 + m21) / s;
         out[2] = 0.25F * s;
      }

      qnormalize(out);
   }

   static void qconj(float[] q, float[] out) {
      out[0] = -q[0];
      out[1] = -q[1];
      out[2] = -q[2];
      out[3] = q[3];
   }

   static void qidentity(float[] q) {
      q[2] = 0.0F;
      q[1] = 0.0F;
      q[0] = 0.0F;
      q[3] = 1.0F;
   }

   static void axisAngle(float deg, float ax, float ay, float az, float[] out) {
      double h = Math.toRadians(deg) / 2.0;
      float s = (float)Math.sin(h);
      float l = (float)Math.sqrt(ax * ax + ay * ay + az * az);
      if (l < 1.0E-9F) {
         qidentity(out);
      } else {
         out[0] = ax / l * s;
         out[1] = ay / l * s;
         out[2] = az / l * s;
         out[3] = (float)Math.cos(h);
      }
   }

   static float dot(float[] a, float[] b) {
      return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
   }

   static void cross(float[] a, float[] b, float[] out) {
      float x = a[1] * b[2] - a[2] * b[1];
      float y = a[2] * b[0] - a[0] * b[2];
      float z = a[0] * b[1] - a[1] * b[0];
      out[0] = x;
      out[1] = y;
      out[2] = z;
   }

   static float normalize(float[] v) {
      float l = (float)Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
      if (l > 1.0E-9F) {
         v[0] /= l;
         v[1] /= l;
         v[2] /= l;
      }

      return l;
   }

   static void set(float[] v, float x, float y, float z) {
      v[0] = x;
      v[1] = y;
      v[2] = z;
   }
}
