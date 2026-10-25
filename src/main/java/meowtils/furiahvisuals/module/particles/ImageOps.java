package meowtils.furiahvisuals.module.particles;

import java.nio.Buffer;
import java.nio.IntBuffer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class ImageOps {
   private ImageOps() {
   }

   static void bleed(int[] p, int w, int h) {
      int n = w * h;
      byte[] state = new byte[n];
      int[] queue = new int[n];
      int tail = 0;
      boolean any = false;

      for (int i = 0; i < n; i++) {
         if (p[i] >>> 24 != 0) {
            state[i] = 2;
            any = true;
         }
      }

      if (any) {
         for (int var25 = 0; var25 < n; var25++) {
            if (state[var25] == 0 && touchesColored(state, var25 % w, var25 / w, w, h)) {
               state[var25] = 1;
               queue[tail++] = var25;
            }
         }

         int head = 0;

         while (head < tail) {
            int ringEnd = tail;

            for (int q = head; q < ringEnd; q++) {
               int i2 = queue[q];
               int x = i2 % w;
               int y = i2 / w;
               int r = 0;
               int g = 0;
               int b = 0;
               int count = 0;

               for (int dy = -1; dy <= 1; dy++) {
                  int yy = y + dy;
                  if (yy >= 0 && yy < h) {
                     for (int dx = -1; dx <= 1; dx++) {
                        int xx = x + dx;
                        int j;
                        if ((dx != 0 || dy != 0) && xx >= 0 && xx < w && state[j = yy * w + xx] == 2) {
                           int c = p[j];
                           r += c >> 16 & 0xFF;
                           g += c >> 8 & 0xFF;
                           b += c & 0xFF;
                           count++;
                        }
                     }
                  }
               }

               if (count > 0) {
                  p[i2] = r / count << 16 | g / count << 8 | b / count;
               }
            }

            for (int var29 = head; var29 < ringEnd; var29++) {
               state[queue[var29]] = 2;
            }

            for (int var30 = head; var30 < ringEnd; var30++) {
               int i2 = queue[var30];
               int x = i2 % w;
               int y = i2 / w;
               if (x > 0 && state[i2 - 1] == 0) {
                  state[i2 - 1] = 1;
                  queue[tail++] = i2 - 1;
               }

               if (x < w - 1 && state[i2 + 1] == 0) {
                  state[i2 + 1] = 1;
                  queue[tail++] = i2 + 1;
               }

               if (y > 0 && state[i2 - w] == 0) {
                  state[i2 - w] = 1;
                  queue[tail++] = i2 - w;
               }

               if (y < h - 1 && state[i2 + w] == 0) {
                  state[i2 + w] = 1;
                  queue[tail++] = i2 + w;
               }
            }

            head = ringEnd;
         }
      }
   }

   private static boolean touchesColored(byte[] state, int x, int y, int w, int h) {
      return x > 0 && state[y * w + x - 1] == 2
         || x < w - 1 && state[y * w + x + 1] == 2
         || y > 0 && state[(y - 1) * w + x] == 2
         || y < h - 1 && state[(y + 1) * w + x] == 2;
   }

   static int[] halve(int[] src, int w, int h) {
      int nw = Math.max(1, w / 2);
      int nh = Math.max(1, h / 2);
      int[] dst = new int[nw * nh];

      for (int y = 0; y < nh; y++) {
         for (int x = 0; x < nw; x++) {
            long a = 0L;
            long r = 0L;
            long g = 0L;
            long b = 0L;
            int n = 0;

            for (int yy = y * 2; yy < Math.min(h, y * 2 + 2); yy++) {
               for (int xx = x * 2; xx < Math.min(w, x * 2 + 2); xx++) {
                  int c = src[yy * w + xx];
                  int ca = c >>> 24;
                  a += ca;
                  r += (c >> 16 & 0xFF) * ca;
                  g += (c >> 8 & 0xFF) * ca;
                  b += (c & 0xFF) * ca;
                  n++;
               }
            }

            dst[y * nw + x] = a > 0L ? (int)((a + n / 2) / n << 24 | r / a << 16 | g / a << 8 | b / a) : 0;
         }
      }

      return dst;
   }

   static int average(int[] p, int w, int x0, int y0, int cw, int ch) {
      long a = 0L;
      long r = 0L;
      long g = 0L;
      long b = 0L;

      for (int y = y0; y < y0 + ch; y++) {
         for (int x = x0; x < x0 + cw; x++) {
            int c = p[y * w + x];
            int ca = c >>> 24;
            a += ca;
            r += (c >> 16 & 0xFF) * ca;
            g += (c >> 8 & 0xFF) * ca;
            b += (c & 0xFF) * ca;
         }
      }

      if (a == 0L) {
         return 16777215;
      } else {
         int coverage = (int)Math.min(255L, a / Math.max(1L, (long)cw * ch));
         return coverage << 24 | (int)(r / a) << 16 | (int)(g / a) << 8 | (int)(b / a);
      }
   }

   static int upload(int[][] levels, int[] widths, int[] heights, boolean smooth) {
      int id = TextureUtil.glGenTextures();
      GlStateManager.bindTexture(id);
      GL11.glTexParameteri(3553, 33085, levels.length - 1);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      applyFilter(smooth);

      for (int l = 0; l < levels.length; l++) {
         IntBuffer buffer = BufferUtils.createIntBuffer(widths[l] * heights[l]);
         buffer.put(levels[l], 0, widths[l] * heights[l]);
         ((Buffer)buffer).flip();
         GL11.glTexImage2D(3553, l, 32856, widths[l], heights[l], 0, 32993, 33639, buffer);
      }

      return id;
   }

   static void setFilter(int texture, boolean smooth) {
      GlStateManager.bindTexture(texture);
      applyFilter(smooth);
   }

   private static void applyFilter(boolean smooth) {
      GL11.glTexParameteri(3553, 10241, smooth ? 9987 : 9984);
      GL11.glTexParameteri(3553, 10240, smooth ? 9729 : 9728);
   }

   static void delete(int texture) {
      if (texture > 0) {
         TextureUtil.deleteTexture(texture);
      }
   }
}
