package meowtils.furiahvisuals.render.post;

public final class Mat4f {
   private Mat4f() {
   }

   public static void identity(float[] m) {
      for (int i = 0; i < 16; i++) {
         m[i] = i % 5 == 0 ? 1.0F : 0.0F;
      }
   }

   public static void mul(float[] a, float[] b, float[] out) {
      for (int c = 0; c < 4; c++) {
         for (int r = 0; r < 4; r++) {
            float s = 0.0F;

            for (int k = 0; k < 4; k++) {
               s += a[k * 4 + r] * b[c * 4 + k];
            }

            out[c * 4 + r] = s;
         }
      }
   }

   public static void translate(float[] m, float x, float y, float z) {
      for (int r = 0; r < 4; r++) {
         int n = 12 + r;
         m[n] += m[r] * x + m[4 + r] * y + m[8 + r] * z;
      }
   }

   public static boolean invert(float[] m, float[] out, float[] tmp) {
      float[] inv = tmp;
      tmp[0] = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
      tmp[4] = -m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10];
      tmp[8] = m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9];
      tmp[12] = -m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9];
      tmp[1] = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
      tmp[5] = m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10];
      tmp[9] = -m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9];
      tmp[13] = m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9];
      tmp[2] = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
      tmp[6] = -m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6];
      tmp[10] = m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5];
      tmp[14] = -m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5];
      tmp[3] = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
      tmp[7] = m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6];
      tmp[11] = -m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5];
      tmp[15] = m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5];
      float det = m[0] * tmp[0] + m[1] * tmp[4] + m[2] * tmp[8] + m[3] * tmp[12];
      if (det != 0.0F && !Float.isNaN(det) && !Float.isInfinite(det)) {
         float d = 1.0F / det;

         for (int i = 0; i < 16; i++) {
            out[i] = inv[i] * d;
         }

         return true;
      } else {
         return false;
      }
   }

   public static void transform(float[] m, float x, float y, float z, float w, float[] out) {
      for (int r = 0; r < 4; r++) {
         out[r] = m[r] * x + m[4 + r] * y + m[8 + r] * z + m[12 + r] * w;
      }
   }

   public static float offIdentity(float[] m) {
      float max = 0.0F;

      for (int i = 0; i < 16; i++) {
         max = Math.max(max, Math.abs(m[i] - (i % 5 == 0 ? 1.0F : 0.0F)));
      }

      return max;
   }
}
