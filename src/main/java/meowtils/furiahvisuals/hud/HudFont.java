package meowtils.furiahvisuals.hud;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import meowtils.furiahvisuals.gui.SmoothFont;
import meowtils.furiahvisuals.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public final class HudFont {
   public static final float LINE_HEIGHT = 9.0F;
   public static final float BASELINE = 7.0F;
   private static final float CAP_HEIGHT = 7.0F;
   public static final int[] COLORS = new int[]{
      0, 170, 43520, 43690, 11141120, 11141290, 16755200, 11184810, 5592405, 5592575, 5635925, 5636095, 16733525, 16733695, 16777045, 16777215
   };
   private static final int[] COLORS_ON_LIGHT = new int[]{
      0, 170, 35328, 35471, 11141120, 10485920, 13072384, 7040629, 4868682, 4013536, 1874460, 691866, 14235190, 12726210, 10519552, 1711397
   };
   private static boolean onLight;
   private static final String CODES = "0123456789abcdefklmnor";
   private static final int FLAG_ITALIC = 1;
   private static final int FLAG_UNDERLINE = 2;
   private static final int FLAG_STRIKE = 4;
   private static final int FLAG_OBFUSCATED = 8;
   private static final byte FACE_REGULAR = 0;
   private static final byte FACE_BOLD = 1;
   private static final byte FACE_MC = 2;
   private static final byte FACE_MC_BOLD = 3;
   private static final float ITALIC = 0.2F;
   private static final String OBFUSCATED_POOL = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ0123456789#$%&?";
   private static final int CACHE_SIZE = 2048;
   private static final Map<Integer, Map<String, HudFont.Layout>> CACHES = new LinkedHashMap<Integer, Map<String, HudFont.Layout>>(16, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<Integer, Map<String, HudFont.Layout>> eldest) {
         return this.size() > 12;
      }
   };
   private static int[] tint;
   private static int cachePxBits;
   private static Map<String, HudFont.Layout> cache;
   private static float sizedFor = -1.0F;
   private static int sizedPixels;
   private static boolean batching;
   private static final int BATCH_INTS = 262144;
   private static final WorldRenderer[] BATCH = new WorldRenderer[2];
   private static final int[] BATCH_TEX = new int[2];
   private static final boolean[] BATCH_OPEN = new boolean[2];
   private static final WorldVertexBufferUploader UPLOADER = new WorldVertexBufferUploader();

   public static void setOnLight(boolean light) {
      onLight = light;
   }

   public static void setTint(int[] colors) {
      tint = colors;
   }

   public static String plain(HudFont.Layout l) {
      return new String(l.chars, 0, l.n);
   }

   private static Map<String, HudFont.Layout> newCache() {
      return new LinkedHashMap<String, HudFont.Layout>(256, 0.75F, true) {
         @Override
         protected boolean removeEldestEntry(Entry<String, HudFont.Layout> eldest) {
            return this.size() > 2048;
         }
      };
   }

   private HudFont() {
   }

   public static int pixelSize(float px) {
      if (px == sizedFor) {
         return sizedPixels;
      } else {
         Ui.ensureFonts();
         float want = 7.0F * px;
         int best = Math.max(6, Math.round(px * 10.0F));
         float bestErr = Float.MAX_VALUE;

         for (int p = Math.max(6, best - 4); p <= best + 4; p++) {
            float err = Math.abs(Ui.font.capHeightPixels(p) - want);
            if (err < bestErr) {
               bestErr = err;
               best = p;
            }
         }

         sizedFor = px;
         sizedPixels = best;
         return best;
      }
   }

   public static HudFont.Layout layout(String text) {
      float px = Ui.px;
      int bits = Float.floatToIntBits(px);
      if (cache == null || bits != cachePxBits) {
         cache = CACHES.get(bits);
         if (cache == null) {
            cache = newCache();
            CACHES.put(bits, cache);
         }

         cachePxBits = bits;
      }

      HudFont.Layout l;
      if ((l = cache.get(text)) == null) {
         l = build(text, px);
         cache.put(text, l);
      }

      return l;
   }

   public static float width(String text) {
      return text != null && !text.isEmpty() ? layout(text).width() : 0.0F;
   }

   public static String trimToWidth(String text, float maxWidth) {
      if (width(text) <= maxWidth) {
         return text;
      } else {
         HudFont.Layout l = layout(text);
         float limitPx = maxWidth * l.px;
         int keep = 0;

         for (int i = 0; i < l.n && !(l.pen[i] + l.advance[i] > limitPx + 0.01F); i++) {
            keep = i + 1;
         }

         int seen = 0;

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 167 && i + 1 < text.length()) {
               i++;
            } else {
               if (seen == keep) {
                  return text.substring(0, i);
               }

               seen++;
            }
         }

         return text;
      }
   }

   private static HudFont.Layout build(String text, float px) {
      Ui.ensureFonts();
      int pixelSize = pixelSize(px);
      HudFont.Layout l = new HudFont.Layout(px, pixelSize);
      FontRenderer mcFont = Minecraft.getMinecraft().fontRendererObj;
      int color = -1;
      boolean bold = false;
      int flags = 0;
      float pen = 0.0F;

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == 167 && i + 1 < text.length()) {
            int code = "0123456789abcdefklmnor".indexOf(Character.toLowerCase(text.charAt(i + 1)));
            i++;
            if (code >= 0) {
               if (code < 16) {
                  color = code;
                  bold = false;
                  flags = 0;
               } else if (code == 16) {
                  flags |= 8;
               } else if (code == 17) {
                  bold = true;
               } else if (code == 18) {
                  flags |= 4;
               } else if (code == 19) {
                  flags |= 2;
               } else if (code == 20) {
                  flags |= 1;
               } else {
                  color = -1;
                  bold = false;
                  flags = 0;
               }
            }
         } else {
            SmoothFont f = bold ? Ui.bold : Ui.font;
            float adv;
            byte face;
            if (c != ' ' && !f.canDisplay(c)) {
               face = (byte)(bold ? 3 : 2);
               adv = (mcFont.getCharWidth(c) + (bold ? 1 : 0)) * px;
               l.fallback = true;
            } else {
               SmoothFont.Glyph g = f.glyph(c, pixelSize);
               if (g == null) {
                  continue;
               }

               face = (byte)(bold ? 1 : 0);
               adv = g.advance;
            }

            l.add(c, pen, adv, face, (byte)color, (byte)flags);
            if ((flags & 6) != 0) {
               l.decorated = true;
            }

            if ((flags & 8) != 0) {
               l.obfuscated = true;
            }

            if (bold) {
               l.bold = true;
            }

            pen += adv;
         }
      }

      l.widthPx = pen;
      return l;
   }

   public static void draw(String text, float x, float y, int color, boolean shadow) {
      if (text != null && !text.isEmpty() && color >>> 24 >= 4) {
         draw(layout(text), x, y, color, shadow);
      }
   }

   public static void drawCentered(String text, float cx, float y, int color, boolean shadow) {
      if (text != null && !text.isEmpty()) {
         HudFont.Layout l = layout(text);
         draw(l, cx - l.width() / 2.0F, y, color, shadow);
      }
   }

   static float shadowPixels(float px) {
      return Math.max(1.0F, Math.min(2.0F, (float)Math.round(px * 0.6F)));
   }

   static int shadowOf(int argb) {
      return argb & 0xFF000000 | (argb & 16579836) >> 2;
   }

   public static void draw(HudFont.Layout l, float x, float y, int color, boolean shadow) {
      if (l.n != 0 && color >>> 24 >= 4) {
         float px = Ui.px;
         if (px == l.px) {
            if (shadow) {
               float s = shadowPixels(px) / px;
               pass(l, x + s, y + s, color, true);
            }

            pass(l, x, y, color, false);
         }
      }
   }

   private static void pass(HudFont.Layout l, float x, float y, int base, boolean shadow) {
      float px = l.px;
      int alpha = base >>> 24;
      float originPx = x * px;
      float baselinePx = Math.round((y + 7.0F) * px);
      Tessellator tess = Tessellator.getInstance();

      for (int pass = 0; pass < 2; pass++) {
         byte want = (byte)(pass == 0 ? 0 : 1);
         if (pass == 1 && !l.bold) {
            break;
         }

         SmoothFont f = pass == 0 ? Ui.font : Ui.bold;
         boolean allKnown = true;
         if (batching) {
            for (int i = 0; i < l.n; i++) {
               if (l.face[i] == want && !f.known(glyphChar(l, i), l.pixelSize)) {
                  flushBatch();
                  allKnown = false;
                  break;
               }
            }
         }

         if (!batching || !allKnown) {
            for (int ix = 0; ix < l.n; ix++) {
               if (l.face[ix] == want) {
                  f.glyph(glyphChar(l, ix), l.pixelSize);
               }
            }
         }

         int tex = f.atlasTexture(l.pixelSize);
         float iw = 1.0F / f.atlasWidth(l.pixelSize);
         float ih = 1.0F / f.atlasHeight(l.pixelSize);
         WorldRenderer wr;
         if (batching) {
            if (BATCH_OPEN[pass] && BATCH_TEX[pass] != tex) {
               flushBatch();
            }

            if (BATCH[pass] == null) {
               BATCH[pass] = new WorldRenderer(262144);
            }

            wr = BATCH[pass];
         } else {
            wr = tess.getWorldRenderer();
         }

         boolean open = false;

         for (int i2 = 0; i2 < l.n; i2++) {
            char c;
            SmoothFont.Glyph g;
            if (l.face[i2] == want && (g = f.glyph(c = glyphChar(l, i2), l.pixelSize)) != null && g.w > 4) {
               if (batching && BATCH_OPEN[pass] && (wr.getVertexCount() + 4) * 24 > wr.getByteBuffer().capacity()) {
                  flushBatch();
               }

               if (!open || batching && !BATCH_OPEN[pass]) {
                  if (batching) {
                     if (!BATCH_OPEN[pass]) {
                        wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                        BATCH_OPEN[pass] = true;
                        BATCH_TEX[pass] = tex;
                     }
                  } else {
                     HudGl.bindTexture(tex);
                     wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                  }

                  open = true;
               }

               int col = colorOf(l, i2, base, alpha, shadow);
               float centering = c != l.chars[i2] ? (l.advance[i2] - g.advance) / 2.0F : 0.0F;
               float gx = Math.round(originPx + l.pen[i2] + centering) + g.offX;
               float gy = baselinePx + g.offY;
               float x0 = gx / px;
               float y0 = gy / px;
               float x1 = (gx + g.w) / px;
               float y1 = (gy + g.h) / px;
               float lean0 = 0.0F;
               float lean1 = 0.0F;
               if ((l.flags[i2] & 1) != 0) {
                  lean0 = (baselinePx - gy) / px * 0.2F;
                  lean1 = (baselinePx - gy - g.h) / px * 0.2F;
               }

               float u0 = g.x * iw;
               float v0 = g.y * ih;
               float u1 = (g.x + g.w) * iw;
               float v1 = (g.y + g.h) * ih;
               int r = col >> 16 & 0xFF;
               int gr = col >> 8 & 0xFF;
               int b = col & 0xFF;
               int a = col >>> 24;
               wr.pos(x0 + lean1, y1, 0.0).tex(u0, v1).color(r, gr, b, a).endVertex();
               wr.pos(x1 + lean1, y1, 0.0).tex(u1, v1).color(r, gr, b, a).endVertex();
               wr.pos(x1 + lean0, y0, 0.0).tex(u1, v0).color(r, gr, b, a).endVertex();
               wr.pos(x0 + lean0, y0, 0.0).tex(u0, v0).color(r, gr, b, a).endVertex();
            }
         }

         if (open && !batching) {
            tess.draw();
         }
      }

      if (l.fallback) {
         drawFallback(l, x, baselinePx, base, alpha, shadow);
      }

      if (l.decorated) {
         drawLines(l, x, y, base, alpha, shadow);
      }
   }

   private static char glyphChar(HudFont.Layout l, int i) {
      return (l.flags[i] & '\b') != 0 && l.chars[i] != ' '
         ? "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ0123456789#$%&?"
            .charAt(ThreadLocalRandom.current().nextInt("abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNOPQRSTUVWXYZ0123456789#$%&?".length()))
         : l.chars[i];
   }

   private static int colorOf(HudFont.Layout l, int i, int base, int alpha, boolean shadow) {
      int rgb = tint != null && i < tint.length && tint[i] != 0
         ? tint[i] & 16777215
         : (l.color[i] < 0 ? base & 16777215 : (onLight ? COLORS_ON_LIGHT : COLORS)[l.color[i]]);
      int c = alpha << 24 | rgb;
      return shadow ? shadowOf(c) : c;
   }

   private static void drawFallback(HudFont.Layout l, float x, float baselinePx, int base, int alpha, boolean shadow) {
      FontRenderer mcFont = Minecraft.getMinecraft().fontRendererObj;
      float px = l.px;
      float top = baselinePx / px - 7.0F;

      for (int i = 0; i < l.n; i++) {
         if (l.face[i] == 2 || l.face[i] == 3) {
            int col = colorOf(l, i, base, alpha, shadow);
            String s = (l.face[i] == 3 ? "§l" : "") + ((l.flags[i] & 1) != 0 ? "§o" : "") + l.chars[i];
            mcFont.drawString(s, x + l.pen[i] / px, top, col, false);
         }
      }

      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableBlend();
   }

   private static void drawLines(HudFont.Layout l, float x, float y, int base, int alpha, boolean shadow) {
      float px = l.px;
      float thick = Math.max(1.0F, (float)Math.round(px * 0.75F)) / px;
      float underY = (Math.round((y + 7.0F) * px) + Math.max(1.0F, (float)Math.round(px * 0.8F))) / px;
      float strikeY = Math.round((y + 7.0F - 2.9399998F) * px) / px;
      GlStateManager.disableTexture2D();
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

      for (int i = 0; i < l.n; i++) {
         byte fl = l.flags[i];
         if ((fl & 6) != 0) {
            int col = colorOf(l, i, base, alpha, shadow);
            float x0 = x + l.pen[i] / px;
            float x1 = x + (l.pen[i] + l.advance[i]) / px;
            if ((fl & 2) != 0) {
               quad(wr, x0, underY, x1, underY + thick, col);
            }

            if ((fl & 4) != 0) {
               quad(wr, x0, strikeY, x1, strikeY + thick, col);
            }
         }
      }

      tess.draw();
      GlStateManager.enableTexture2D();
   }

   private static void quad(WorldRenderer wr, float x0, float y0, float x1, float y1, int c) {
      int r = c >> 16 & 0xFF;
      int g = c >> 8 & 0xFF;
      int b = c & 0xFF;
      int a = c >>> 24;
      wr.pos(x0, y1, 0.0).color(r, g, b, a).endVertex();
      wr.pos(x1, y1, 0.0).color(r, g, b, a).endVertex();
      wr.pos(x1, y0, 0.0).color(r, g, b, a).endVertex();
      wr.pos(x0, y0, 0.0).color(r, g, b, a).endVertex();
   }

   public static void beginBatch() {
      batching = true;
   }

   public static void endBatch() {
      flushBatch();
      batching = false;
   }

   private static void flushBatch() {
      for (int f = 0; f < 2; f++) {
         if (BATCH_OPEN[f]) {
            BATCH[f].finishDrawing();
            GlStateManager.enableTexture2D();
            HudGl.bindTexture(BATCH_TEX[f]);
            UPLOADER.draw(BATCH[f]);
            BATCH_OPEN[f] = false;
         }
      }
   }

   public static void clearCache() {
      CACHES.clear();
      cache = null;
      sizedFor = -1.0F;
   }

   public static final class Layout {
      final float px;
      final int pixelSize;
      int n;
      char[] chars = new char[16];
      float[] pen = new float[16];
      float[] advance = new float[16];
      byte[] face = new byte[16];
      byte[] color = new byte[16];
      byte[] flags = new byte[16];
      float widthPx;
      boolean decorated;
      boolean fallback;
      boolean obfuscated;
      boolean bold;

      Layout(float px, int pixelSize) {
         this.px = px;
         this.pixelSize = pixelSize;
      }

      public float width() {
         return this.widthPx / this.px;
      }

      void add(char c, float penPx, float adv, byte f, byte col, byte fl) {
         if (this.n == this.chars.length) {
            int m = this.n * 2;
            this.chars = Arrays.copyOf(this.chars, m);
            this.pen = Arrays.copyOf(this.pen, m);
            this.advance = Arrays.copyOf(this.advance, m);
            this.face = Arrays.copyOf(this.face, m);
            this.color = Arrays.copyOf(this.color, m);
            this.flags = Arrays.copyOf(this.flags, m);
         }

         this.chars[this.n] = c;
         this.pen[this.n] = penPx;
         this.advance[this.n] = adv;
         this.face[this.n] = f;
         this.color[this.n] = col;
         this.flags[this.n] = fl;
         this.n++;
      }
   }
}
