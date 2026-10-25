package meowtils.furiahvisuals.module.particles;

import java.io.File;
import meowtils.furiahvisuals.image.ImageLoader;

final class ImageProcessor {
   static final int MAX_SIDE = 4096;

   private ImageProcessor() {
   }

   static ImageProcessor.Result process(File file, ImageProcessor.Square square, int maxSize) throws ImageLoader.LoadException {
      ImageLoader.Image image = ImageLoader.load(file, 4096, true);
      int w = image.width;
      int h = image.height;
      int[] argb = image.argb;
      ImageProcessor.Result result = new ImageProcessor.Result();
      result.originalWidth = image.fileWidth;
      result.originalHeight = image.fileHeight;
      int longer = Math.max(w, h);
      int shorter = Math.min(w, h);
      float[] level0;
      int size;
      if (w == h) {
         size = Math.min(maxSize, w);
         level0 = resample(argb, w, 0, 0, w, h, size, size);
         result.how = size == w ? "kept" : "scaled";
      } else if (square == ImageProcessor.Square.CROP) {
         size = Math.min(maxSize, shorter);
         level0 = resample(argb, w, (w - shorter) / 2, (h - shorter) / 2, shorter, shorter, size, size);
         result.how = "cropped";
      } else if (square == ImageProcessor.Square.FIT) {
         size = Math.min(maxSize, longer);
         int fw = Math.max(1, Math.round(w * ((float)size / longer)));
         int fh = Math.max(1, Math.round(h * ((float)size / longer)));
         float[] inner = resample(argb, w, 0, 0, w, h, fw, fh);
         level0 = new float[size * size * 4];
         int ox = (size - fw) / 2;
         int oy = (size - fh) / 2;

         for (int y = 0; y < fh; y++) {
            System.arraycopy(inner, y * fw * 4, level0, ((oy + y) * size + ox) * 4, fw * 4);
         }

         result.how = "fitted";
      } else {
         size = Math.min(maxSize, longer);
         level0 = resample(argb, w, 0, 0, w, h, size, size);
         result.how = "stretched";
      }

      argb = null;
      result.size = size;
      int levels = 1;

      for (int s = size; s > 1; s = Math.max(1, s / 2)) {
         levels++;
      }

      result.levels = new int[levels][];
      result.levelSizes = new int[levels];
      float[] current = level0;
      int s2 = size;

      for (int l = 0; l < levels; l++) {
         int[] packed = unpremultiply(current, s2 * s2);
         ImageOps.bleed(packed, s2, s2);
         result.levels[l] = packed;
         result.levelSizes[l] = s2;
         if (l == 0) {
            result.average = averageOf(current, s2 * s2);
         }

         if (l + 1 < levels) {
            int ns = Math.max(1, s2 / 2);
            current = resize(current, s2, s2, ns, ns);
            s2 = ns;
         }
      }

      return result;
   }

   private static float[] resample(int[] argb, int stride, int x0, int y0, int cw, int ch, int dw, int dh) {
      ImageProcessor.Kernel kx = ImageProcessor.Kernel.of(cw, dw);
      float[] rows = new float[dw * ch * 4];
      float[] line = new float[cw * 4];

      for (int y = 0; y < ch; y++) {
         int src = (y0 + y) * stride + x0;

         for (int x = 0; x < cw; x++) {
            int c = argb[src + x];
            float a = (c >>> 24) / 255.0F;
            line[x * 4] = (c >> 16 & 0xFF) / 255.0F * a;
            line[x * 4 + 1] = (c >> 8 & 0xFF) / 255.0F * a;
            line[x * 4 + 2] = (c & 0xFF) / 255.0F * a;
            line[x * 4 + 3] = a;
         }

         kx.apply(line, 0, 4, rows, y * dw * 4, 4, dw);
      }

      return resizeVertical(rows, dw, ch, dh);
   }

   private static float[] resize(float[] src, int w, int h, int dw, int dh) {
      ImageProcessor.Kernel kx = ImageProcessor.Kernel.of(w, dw);
      float[] rows = new float[dw * h * 4];

      for (int y = 0; y < h; y++) {
         kx.apply(src, y * w * 4, 4, rows, y * dw * 4, 4, dw);
      }

      return resizeVertical(rows, dw, h, dh);
   }

   private static float[] resizeVertical(float[] rows, int w, int h, int dh) {
      ImageProcessor.Kernel ky = ImageProcessor.Kernel.of(h, dh);
      float[] out = new float[w * dh * 4];

      for (int x = 0; x < w; x++) {
         ky.apply(rows, x * 4, w * 4, out, x * 4, w * 4, dh);
      }

      for (int i = 0; i < out.length; i += 4) {
         float a;
         out[i + 3] = a = Math.max(0.0F, Math.min(1.0F, out[i + 3]));
         out[i] = Math.max(0.0F, Math.min(a, out[i]));
         out[i + 1] = Math.max(0.0F, Math.min(a, out[i + 1]));
         out[i + 2] = Math.max(0.0F, Math.min(a, out[i + 2]));
      }

      return out;
   }

   private static int[] unpremultiply(float[] p, int n) {
      int[] out = new int[n];

      for (int i = 0; i < n; i++) {
         float a = p[i * 4 + 3];
         int ai = Math.round(a * 255.0F);
         if (ai <= 0) {
            out[i] = 0;
         } else {
            int r = Math.min(255, Math.round(p[i * 4] / a * 255.0F));
            int g = Math.min(255, Math.round(p[i * 4 + 1] / a * 255.0F));
            int b = Math.min(255, Math.round(p[i * 4 + 2] / a * 255.0F));
            out[i] = ai << 24 | r << 16 | g << 8 | b;
         }
      }

      return out;
   }

   private static int averageOf(float[] p, int n) {
      double r = 0.0;
      double g = 0.0;
      double b = 0.0;
      double a = 0.0;

      for (int i = 0; i < n; i++) {
         r += p[i * 4];
         g += p[i * 4 + 1];
         b += p[i * 4 + 2];
         a += p[i * 4 + 3];
      }

      if (a <= 0.0) {
         return 16777215;
      } else {
         int ai = (int)Math.min(255L, Math.round(a / n * 255.0));
         return ai << 24
            | (int)Math.min(255L, Math.round(r / a * 255.0)) << 16
            | (int)Math.min(255L, Math.round(g / a * 255.0)) << 8
            | (int)Math.min(255L, Math.round(b / a * 255.0));
      }
   }

   private static final class Kernel {
      final int srcLen;
      final int[] start;
      final int[] count;
      final int[] offset;
      final float[] weights;
      final boolean identity;

      private Kernel(int srcLen, int[] start, int[] count, int[] offset, float[] weights, boolean identity) {
         this.srcLen = srcLen;
         this.start = start;
         this.count = count;
         this.offset = offset;
         this.weights = weights;
         this.identity = identity;
      }

      static ImageProcessor.Kernel of(int src, int dst) {
         if (src == dst) {
            return new ImageProcessor.Kernel(src, null, null, null, null, true);
         } else {
            int[] start = new int[dst];
            int[] count = new int[dst];
            int[] offset = new int[dst];
            float[] weights;
            if (dst < src) {
               double scale = (double)src / dst;
               int taps = (int)Math.ceil(scale) + 2;
               weights = new float[dst * taps];

               for (int i = 0; i < dst; i++) {
                  double s0 = i * scale;
                  double s1 = (i + 1) * scale;
                  int j0 = (int)Math.floor(s0);
                  int j1 = Math.min(src - 1, (int)Math.ceil(s1) - 1);
                  start[i] = j0;
                  offset[i] = i * taps;
                  int n = 0;

                  for (int j = j0; j <= j1; j++) {
                     double overlap = Math.min(s1, (double)(j + 1)) - Math.max(s0, (double)j);
                     weights[i * taps + n++] = (float)(Math.max(0.0, overlap) / scale);
                  }

                  count[i] = n;
               }
            } else {
               weights = new float[dst * 4];
               double scale = (double)src / dst;

               for (int i = 0; i < dst; i++) {
                  double x = (i + 0.5) * scale - 0.5;
                  int ix = (int)Math.floor(x);
                  float t = (float)(x - ix);
                  start[i] = ix - 1;
                  count[i] = 4;
                  offset[i] = i * 4;
                  weights[i * 4] = ((-0.5F * t + 1.0F) * t - 0.5F) * t;
                  weights[i * 4 + 1] = (1.5F * t - 2.5F) * t * t + 1.0F;
                  weights[i * 4 + 2] = ((-1.5F * t + 2.0F) * t + 0.5F) * t;
                  weights[i * 4 + 3] = (0.5F * t - 0.5F) * t * t;
               }
            }

            return new ImageProcessor.Kernel(src, start, count, offset, weights, false);
         }
      }

      void apply(float[] in, int inBase, int inStep, float[] out, int outBase, int outStep, int dstLen) {
         if (this.identity) {
            for (int i = 0; i < dstLen; i++) {
               System.arraycopy(in, inBase + i * inStep, out, outBase + i * outStep, 4);
            }
         } else {
            int last = this.srcLen - 1;

            for (int i = 0; i < dstLen; i++) {
               float r = 0.0F;
               float g = 0.0F;
               float b = 0.0F;
               float a = 0.0F;
               int s = this.start[i];
               int n = this.count[i];
               int o = this.offset[i];

               for (int k = 0; k < n; k++) {
                  int j = s + k;
                  if (j < 0) {
                     j = 0;
                  } else if (j > last) {
                     j = last;
                  }

                  float w = this.weights[o + k];
                  int p = inBase + j * inStep;
                  r += in[p] * w;
                  g += in[p + 1] * w;
                  b += in[p + 2] * w;
                  a += in[p + 3] * w;
               }

               int q = outBase + i * outStep;
               out[q] = r;
               out[q + 1] = g;
               out[q + 2] = b;
               out[q + 3] = a;
            }
         }
      }
   }

   static final class Result {
      int originalWidth;
      int originalHeight;
      int size;
      int[][] levels;
      int[] levelSizes;
      int average;
      String how;
   }

   static enum Square {
      STRETCH,
      FIT,
      CROP;
   }
}
