package meowtils.furiahvisuals.module.misc.skins;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import meowtils.furiahvisuals.image.ImageLoader;

public final class SkinImage {
   public static final long MAX_FILE = 8388608L;
   private static final int[] WIDTHS = new int[]{64, 128, 256, 512, 1024};
   public final int size;
   public final int[] argb;
   public final boolean slim;
   public final boolean legacy;
   public final String hash;

   private SkinImage(int size, int[] argb, boolean slim, boolean legacy, String hash) {
      this.size = size;
      this.argb = argb;
      this.slim = slim;
      this.legacy = legacy;
      this.hash = hash;
   }

   public boolean hd() {
      return this.size > 64;
   }

   public static SkinImage read(byte[] bytes, String name) throws ImageLoader.LoadException {
      if (bytes.length > 8388608L) {
         throw new ImageLoader.LoadException(String.format(Locale.ROOT, "This file is %.1f MB; skins can be up to 8 MB.", bytes.length / 1048576.0));
      } else {
         ImageLoader.Format format = ImageLoader.detect(bytes);
         if (format != ImageLoader.Format.PNG && format.readable) {
            throw new ImageLoader.LoadException(
               "Skins are PNG images (they need see-through parts); this file is " + format.description + ". Save it as PNG and try again."
            );
         } else {
            int[] wh = ImageLoader.dimensions(bytes, name);
            check(wh[0], wh[1]);
            ImageLoader.Image image = ImageLoader.decodeBytes(bytes, name, 1024, false);
            check(image.width, image.height);
            return process(image.width, image.height, image.argb);
         }
      }
   }

   static void check(int w, int h) throws ImageLoader.LoadException {
      for (int s : WIDTHS) {
         if (w == s && (h == s || h == s / 2)) {
            return;
         }
      }

      throw new ImageLoader.LoadException("Skins must be 64×64, 64×32, or an HD size like 128×128. This image is " + w + "×" + h + ".");
   }

   public static SkinImage process(int w, int h, int[] src) {
      int s = w / 64;
      boolean legacy = h == w / 2;
      int[] px = new int[w * w];
      System.arraycopy(src, 0, px, 0, w * h);
      if (legacy) {
         mirrorLimb(px, w, s, 0, 16, 16, 48);
         mirrorLimb(px, w, s, 40, 16, 32, 48);
      }

      boolean slim = seeThrough(px, w, s, 54, 20, 56, 32);
      solid(px, w, s, 0, 0, 32, 16);
      emptyIfSolid(px, w, s, 32, 0, 64, 32);
      solid(px, w, s, 0, 16, 64, 32);
      emptyIfSolid(px, w, s, 0, 32, 16, 48);
      emptyIfSolid(px, w, s, 16, 32, 40, 48);
      emptyIfSolid(px, w, s, 40, 32, 56, 48);
      emptyIfSolid(px, w, s, 0, 48, 16, 64);
      solid(px, w, s, 16, 48, 48, 64);
      emptyIfSolid(px, w, s, 48, 48, 64, 64);
      return new SkinImage(w, px, slim, legacy, hash(w, px));
   }

   private static void mirrorLimb(int[] px, int w, int s, int sx, int sy, int dx, int dy) {
      flipCopy(px, w, s, sx + 4, sy, dx + 4, dy, 4, 4);
      flipCopy(px, w, s, sx + 8, sy, dx + 8, dy, 4, 4);
      flipCopy(px, w, s, sx + 8, sy + 4, dx, dy + 4, 4, 12);
      flipCopy(px, w, s, sx + 4, sy + 4, dx + 4, dy + 4, 4, 12);
      flipCopy(px, w, s, sx, sy + 4, dx + 8, dy + 4, 4, 12);
      flipCopy(px, w, s, sx + 12, sy + 4, dx + 12, dy + 4, 4, 12);
   }

   private static void flipCopy(int[] px, int w, int s, int sx, int sy, int dx, int dy, int fw, int fh) {
      int cw = fw * s;
      int ch = fh * s;
      int x0 = sx * s;
      int y0 = sy * s;
      int x1 = dx * s;
      int y1 = dy * s;

      for (int y = 0; y < ch; y++) {
         int from = (y0 + y) * w + x0;
         int to = (y1 + y) * w + x1 + cw - 1;

         for (int x = 0; x < cw; x++) {
            px[to - x] = px[from + x];
         }
      }
   }

   private static boolean seeThrough(int[] px, int w, int s, int x0, int y0, int x1, int y1) {
      for (int y = y0 * s; y < y1 * s; y++) {
         for (int x = x0 * s; x < x1 * s; x++) {
            if (px[y * w + x] >>> 24 != 0) {
               return false;
            }
         }
      }

      return true;
   }

   private static void solid(int[] px, int w, int s, int x0, int y0, int x1, int y1) {
      for (int y = y0 * s; y < y1 * s; y++) {
         for (int x = x0 * s; x < x1 * s; x++) {
            int n = y * w + x;
            px[n] |= -16777216;
         }
      }
   }

   private static void emptyIfSolid(int[] px, int w, int s, int x0, int y0, int x1, int y1) {
      for (int y = y0 * s; y < y1 * s; y++) {
         for (int x = x0 * s; x < x1 * s; x++) {
            if (px[y * w + x] >>> 24 < 128) {
               return;
            }
         }
      }

      for (int var11 = y0 * s; var11 < y1 * s; var11++) {
         for (int xx = x0 * s; xx < x1 * s; xx++) {
            int n = var11 * w + xx;
            px[n] &= 16777215;
         }
      }
   }

   static String hash(int size, int[] px) {
      try {
         MessageDigest md = MessageDigest.getInstance("SHA-1");
         ByteBuffer b = ByteBuffer.allocate(4 + px.length * 4);
         b.putInt(size);
         b.asIntBuffer().put(px);
         md.update(b.array());
         byte[] d = md.digest();
         StringBuilder sb = new StringBuilder(d.length * 2);

         for (byte v : d) {
            sb.append(Character.forDigit(v >> 4 & 15, 16)).append(Character.forDigit(v & 15, 16));
         }

         return sb.toString();
      } catch (NoSuchAlgorithmException var10) {
         return Integer.toHexString(Arrays.hashCode(px)) + Integer.toHexString(size);
      }
   }

   public int[] face(int[] sideOut) {
      int s = this.size / 64;
      int n = 8 * s;
      int step = Math.max(1, n / 32);
      int side = n / step;
      int[] out = new int[side * side];

      for (int y = 0; y < side; y++) {
         for (int x = 0; x < side; x++) {
            int fx = x * step;
            int fy = y * step;
            int base = this.argb[(8 * s + fy) * this.size + 8 * s + fx] | 0xFF000000;
            int hat = this.argb[(8 * s + fy) * this.size + 40 * s + fx];
            out[y * side + x] = over(hat, base);
         }
      }

      sideOut[0] = side;
      return out;
   }

   private static int over(int top, int bottom) {
      int a = top >>> 24;
      if (a == 0) {
         return bottom;
      } else if (a == 255) {
         return top;
      } else {
         int r = ((top >> 16 & 0xFF) * a + (bottom >> 16 & 0xFF) * (255 - a)) / 255;
         int g = ((top >> 8 & 0xFF) * a + (bottom >> 8 & 0xFF) * (255 - a)) / 255;
         int bl = ((top & 0xFF) * a + (bottom & 0xFF) * (255 - a)) / 255;
         return 0xFF000000 | r << 16 | g << 8 | bl;
      }
   }
}
