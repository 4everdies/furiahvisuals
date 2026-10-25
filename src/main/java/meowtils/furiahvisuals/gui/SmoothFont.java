package meowtils.furiahvisuals.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public final class SmoothFont {
   private static final String CHARSET;
   private static final String[] PREFERRED;
   private static final String[] FALLBACKS;
   private static final int MAX_HEIGHT = 4096;
   private static final SmoothFont.Glyph MISSING;
   private final Font baseFont;
   private final Map<Integer, SmoothFont.Atlas> atlases = new HashMap<>();
   private static List<Font> fallbackFonts;

   private SmoothFont(Font baseFont) {
      this.baseFont = baseFont;
   }

   public Font awt(float pixelSize) {
      return this.baseFont.deriveFont(pixelSize);
   }

   static SmoothFont[] createDefaults() {
      HashSet<String> available = new HashSet<>();

      try {
         available.addAll(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
      } catch (Throwable var6) {
         FuriahsVisuals.LOG.warn("Could not list system fonts, using SansSerif", var6);
      }

      String family = "SansSerif";

      for (String name : PREFERRED) {
         if (available.contains(name)) {
            family = name;
            break;
         }
      }

      Font regular = new Font(family, 0, 12);
      Font bold = available.contains(family + " Semibold") ? new Font(family + " Semibold", 0, 12) : new Font(family, 1, 12);
      Font heavy = new Font(family, 1, 12);
      FuriahsVisuals.LOG.info("GUI font: {}", new Object[]{family});
      return new SmoothFont[]{new SmoothFont(regular), new SmoothFont(bold), new SmoothFont(heavy)};
   }

   private SmoothFont.Atlas atlas(int pixelSize) {
      SmoothFont.Atlas atlas = this.atlases.get(pixelSize);
      if (atlas == null) {
         atlas = this.build(pixelSize);
         this.atlases.put(pixelSize, atlas);
      }

      return atlas;
   }

   private SmoothFont.Glyph lookup(SmoothFont.Atlas atlas, char c) {
      if (c < 128) {
         return atlas.ascii[c];
      } else {
         SmoothFont.Glyph g = atlas.other.get(c);
         if (g == null) {
            g = this.add(atlas, c);
            atlas.other.put(c, g == null ? MISSING : g);
         }

         return g == MISSING ? null : g;
      }
   }

   private SmoothFont.Atlas build(int pixelSize) {
      Font font = this.baseFont.deriveFont((float)pixelSize);
      BufferedImage scratch = new BufferedImage(1, 1, 2);
      Graphics2D sg = scratch.createGraphics();
      hints(sg);
      sg.setFont(font);
      FontRenderContext frc = sg.getFontRenderContext();
      SmoothFont.Atlas atlas = new SmoothFont.Atlas();
      atlas.pixelSize = pixelSize;
      atlas.font = font;
      atlas.frc = frc;
      atlas.ascent = sg.getFontMetrics().getAscent();
      atlas.descent = sg.getFontMetrics().getDescent();
      sg.dispose();
      Rectangle caps = font.createGlyphVector(frc, "H").getPixelBounds(frc, 0.0F, 0.0F);
      if (caps.height > 0) {
         atlas.capTop = caps.y;
         atlas.capBottom = caps.y + caps.height;
      } else {
         atlas.capTop = -Math.round(pixelSize * 0.7F);
         atlas.capBottom = 0;
      }

      int pad = 2;
      int width = pixelSize > 40 ? 1024 : 512;
      int penX = 0;
      int penY = 0;
      int rowHeight = 0;
      HashMap<Character, SmoothFont.Glyph> glyphs = new HashMap<>();

      for (int i = 0; i < CHARSET.length(); i++) {
         char ch = CHARSET.charAt(i);
         if (font.canDisplay(ch)) {
            GlyphVector gv = font.createGlyphVector(frc, String.valueOf(ch));
            Rectangle bounds = gv.getPixelBounds(frc, 0.0F, 0.0F);
            SmoothFont.Glyph glyph = new SmoothFont.Glyph();
            glyph.advance = gv.getGlyphMetrics(0).getAdvanceX();
            glyph.w = Math.max(1, bounds.width) + pad * 2;
            glyph.h = Math.max(1, bounds.height) + pad * 2;
            glyph.offX = bounds.x - pad;
            glyph.offY = bounds.y - pad;
            if (penX + glyph.w > width) {
               penX = 0;
               penY += rowHeight;
               rowHeight = 0;
            }

            glyph.x = penX;
            glyph.y = penY;
            penX += glyph.w;
            rowHeight = Math.max(rowHeight, glyph.h);
            glyphs.put(ch, glyph);
         }
      }

      int height = 1;

      while (height < penY + rowHeight + Math.max(rowHeight * 2, (penY + rowHeight) / 4)) {
         height <<= 1;
      }

      atlas.penX = penX;
      atlas.penY = penY;
      atlas.rowHeight = rowHeight;
      BufferedImage image = new BufferedImage(width, height, 2);
      Graphics2D g2 = image.createGraphics();
      hints(g2);
      g2.setFont(font);
      g2.setColor(Color.WHITE);

      for (Entry entry : glyphs.entrySet()) {
         SmoothFont.Glyph g = (SmoothFont.Glyph)entry.getValue();
         if ((Character)entry.getKey() != ' ') {
            g2.drawString(String.valueOf(entry.getKey()), g.x - g.offX, g.y - g.offY);
         }

         char ch;
         if ((ch = (Character)entry.getKey()) < 128) {
            atlas.ascii[ch] = g;
         } else {
            atlas.other.put(ch, g);
         }
      }

      g2.dispose();
      int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
      whiten(pixels);
      IntBuffer intBuffer = BufferUtils.createIntBuffer(pixels.length);
      ((Buffer)intBuffer.put(pixels)).flip();
      atlas.texture = TextureUtil.glGenTextures();
      GlStateManager.bindTexture(atlas.texture);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL11.glTexParameteri(3553, 33085, 0);
      GL11.glTexImage2D(3553, 0, 32856, width, height, 0, 32993, 33639, intBuffer);
      atlas.width = width;
      atlas.height = height;
      return atlas;
   }

   private static void whiten(int[] pixels) {
      for (int i = 0; i < pixels.length; i++) {
         float a = (pixels[i] >>> 24) / 255.0F;
         int boosted = Math.round((float)Math.pow(a, 0.85) * 255.0F);
         pixels[i] = boosted << 24 | 16777215;
      }
   }

   private SmoothFont.Glyph add(SmoothFont.Atlas atlas, char c) {
      if (c >= ' ' && !Character.isSurrogate(c) && !Character.isISOControl(c)) {
         Font font = atlas.font.canDisplay(c) ? atlas.font : fallback(c, atlas.pixelSize);
         if (font == null) {
            return null;
         } else {
            try {
               GlyphVector gv = font.createGlyphVector(atlas.frc, String.valueOf(c));
               Rectangle bounds = gv.getPixelBounds(atlas.frc, 0.0F, 0.0F);
               int pad = 2;
               SmoothFont.Glyph g = new SmoothFont.Glyph();
               g.advance = gv.getGlyphMetrics(0).getAdvanceX();
               g.w = Math.max(1, bounds.width) + pad * 2;
               g.h = Math.max(1, bounds.height) + pad * 2;
               g.offX = bounds.x - pad;
               g.offY = bounds.y - pad;
               if (g.w > atlas.width) {
                  return null;
               } else {
                  if (atlas.penX + g.w > atlas.width) {
                     atlas.penX = 0;
                     atlas.penY = atlas.penY + atlas.rowHeight;
                     atlas.rowHeight = 0;
                  }

                  while (atlas.penY + g.h > atlas.height) {
                     if (atlas.height * 2 > 4096 || !grow(atlas)) {
                        return null;
                     }
                  }

                  g.x = atlas.penX;
                  g.y = atlas.penY;
                  atlas.penX = atlas.penX + g.w;
                  atlas.rowHeight = Math.max(atlas.rowHeight, g.h);
                  BufferedImage image = new BufferedImage(g.w, g.h, 2);
                  Graphics2D g2 = image.createGraphics();
                  hints(g2);
                  g2.setFont(font);
                  g2.setColor(Color.WHITE);
                  if (!Character.isWhitespace(c)) {
                     g2.drawString(String.valueOf(c), -g.offX, -g.offY);
                  }

                  g2.dispose();
                  int[] pixels = image.getRGB(0, 0, g.w, g.h, null, 0, g.w);
                  whiten(pixels);
                  IntBuffer buffer = BufferUtils.createIntBuffer(pixels.length);
                  ((Buffer)buffer.put(pixels)).flip();
                  GlStateManager.bindTexture(atlas.texture);
                  GL11.glTexSubImage2D(3553, 0, g.x, g.y, g.w, g.h, 32993, 33639, buffer);
                  return g;
               }
            } catch (RuntimeException var13) {
               FuriahsVisuals.LOG.debug("Could not add glyph U+{}", new Object[]{Integer.toHexString(c), var13});
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static boolean grow(SmoothFont.Atlas atlas) {
      int newHeight = atlas.height * 2;
      IntBuffer old = BufferUtils.createIntBuffer(atlas.width * atlas.height);
      GlStateManager.bindTexture(atlas.texture);
      GL11.glGetTexImage(3553, 0, 32993, 33639, old);
      IntBuffer grown = BufferUtils.createIntBuffer(atlas.width * newHeight);
      grown.put(old);
      ((Buffer)grown).position(0);
      GL11.glTexImage2D(3553, 0, 32856, atlas.width, newHeight, 0, 32993, 33639, grown);
      atlas.height = newHeight;
      return true;
   }

   private static Font fallback(char c, int pixelSize) {
      if (fallbackFonts == null) {
         HashSet<String> available = new HashSet<>();

         try {
            available.addAll(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
         } catch (Throwable var8) {
         }

         ArrayList<Font> list = new ArrayList<>();

         for (String name : FALLBACKS) {
            if ("Dialog".equals(name) || available.contains(name)) {
               list.add(new Font(name, 0, 12));
            }
         }

         fallbackFonts = list;
      }

      for (Font f : fallbackFonts) {
         if (f.canDisplay(c)) {
            return f.deriveFont((float)pixelSize);
         }
      }

      return null;
   }

   private static void hints(Graphics2D g) {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
   }

   public static int pixelSize(float size, float px) {
      return Math.max(6, Math.round(size * px));
   }

   public SmoothFont.Glyph glyph(char c, int pixelSize) {
      return this.lookup(this.atlas(pixelSize), c);
   }

   public boolean known(char c, int pixelSize) {
      SmoothFont.Atlas atlas = this.atlases.get(pixelSize);
      return atlas != null && (c < 128 || atlas.other.get(c) != null);
   }

   public int atlasTexture(int pixelSize) {
      return this.atlas(pixelSize).texture;
   }

   public int atlasWidth(int pixelSize) {
      return this.atlas(pixelSize).width;
   }

   public int atlasHeight(int pixelSize) {
      return this.atlas(pixelSize).height;
   }

   public int ascentPixels(int pixelSize) {
      return this.atlas(pixelSize).ascent;
   }

   public float capMiddlePixels(int pixelSize) {
      SmoothFont.Atlas a = this.atlas(pixelSize);
      return a.ascent + (a.capTop + a.capBottom) / 2.0F;
   }

   public boolean canDisplay(char c) {
      return this.baseFont.canDisplay(c);
   }

   public int capHeightPixels(int pixelSize) {
      SmoothFont.Atlas a = this.atlas(pixelSize);
      return a.capBottom - a.capTop;
   }

   public int linePixels(int pixelSize) {
      SmoothFont.Atlas a = this.atlas(pixelSize);
      return a.ascent + a.descent;
   }

   public float width(String text, float size, float px) {
      SmoothFont.Atlas atlas = this.atlas(pixelSize(size, px));
      float w = 0.0F;

      for (int i = 0; i < text.length(); i++) {
         SmoothFont.Glyph g = this.lookup(atlas, text.charAt(i));
         if (g != null) {
            w += g.advance;
         }
      }

      return w / px;
   }

   public float lineHeight(float size, float px) {
      SmoothFont.Atlas atlas = this.atlas(pixelSize(size, px));
      return (atlas.ascent + atlas.descent) / px;
   }

   public float ascent(float size, float px) {
      return this.atlas(pixelSize(size, px)).ascent / px;
   }

   public void draw(String text, float x, float y, float size, float px, int color) {
      if (color >>> 24 != 0 && !text.isEmpty()) {
         SmoothFont.Atlas atlas = this.atlas(pixelSize(size, px));

         for (int i = 0; i < text.length(); i++) {
            this.lookup(atlas, text.charAt(i));
         }

         GlStateManager.bindTexture(atlas.texture);
         Tessellator tess = Tessellator.getInstance();
         WorldRenderer wr = tess.getWorldRenderer();
         wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         int a = color >>> 24;
         int r = color >> 16 & 0xFF;
         int gC = color >> 8 & 0xFF;
         int b = color & 0xFF;
         float penPx = x * px;
         float baselinePx = Math.round(y * px) + atlas.ascent;
         float iw = 1.0F / atlas.width;
         float ih = 1.0F / atlas.height;

         for (int i = 0; i < text.length(); i++) {
            SmoothFont.Glyph g = this.lookup(atlas, text.charAt(i));
            if (g != null) {
               if (g.w > 4) {
                  float gx = Math.round(penPx) + g.offX;
                  float gy = baselinePx + g.offY;
                  float x0 = gx / px;
                  float y0 = gy / px;
                  float x1 = (gx + g.w) / px;
                  float y1 = (gy + g.h) / px;
                  float u0 = g.x * iw;
                  float v0 = g.y * ih;
                  float u1 = (g.x + g.w) * iw;
                  float v1 = (g.y + g.h) * ih;
                  wr.pos(x0, y1, 0.0).tex(u0, v1).color(r, gC, b, a).endVertex();
                  wr.pos(x1, y1, 0.0).tex(u1, v1).color(r, gC, b, a).endVertex();
                  wr.pos(x1, y0, 0.0).tex(u1, v0).color(r, gC, b, a).endVertex();
                  wr.pos(x0, y0, 0.0).tex(u0, v0).color(r, gC, b, a).endVertex();
               }

               penPx += g.advance;
            }
         }

         tess.draw();
      }
   }

   public void clear() {
      for (SmoothFont.Atlas atlas : this.atlases.values()) {
         TextureUtil.deleteTexture(atlas.texture);
      }

      this.atlases.clear();
   }

   public boolean has(int pixelSize) {
      return this.atlases.containsKey(pixelSize);
   }

   public void warm(int pixelSize) {
      this.atlas(pixelSize);
   }

   public void retain(Set<Integer> keep) {
      Iterator<Entry<Integer, SmoothFont.Atlas>> it = this.atlases.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Integer, SmoothFont.Atlas> e = it.next();
         if (!keep.contains(e.getKey())) {
            TextureUtil.deleteTexture(e.getValue().texture);
            it.remove();
         }
      }
   }

   public int cachedSizes() {
      return this.atlases.size();
   }

   static {
      StringBuilder sb = new StringBuilder();

      for (char c = ' '; c < 127; c++) {
         sb.append(c);
      }

      sb.append("°·×•–—…←→↑↓✓⇄");
      CHARSET = sb.toString();
      PREFERRED = new String[]{"Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Roboto", "Noto Sans", "Ubuntu", "DejaVu Sans", "Arial"};
      FALLBACKS = new String[]{
         "Segoe UI Symbol",
         "Segoe UI Historic",
         "Nirmala UI",
         "Microsoft YaHei UI",
         "Microsoft YaHei",
         "Yu Gothic UI",
         "Meiryo",
         "Malgun Gothic",
         "Microsoft JhengHei UI",
         "Noto Sans CJK JP",
         "Noto Sans",
         "Arial Unicode MS",
         "Apple SD Gothic Neo",
         "PingFang SC",
         "Hiragino Sans",
         "Dialog"
      };
      MISSING = new SmoothFont.Glyph();
   }

   private static final class Atlas {
      int texture;
      int width;
      int height;
      int ascent;
      int descent;
      int capTop;
      int capBottom;
      int pixelSize;
      Font font;
      FontRenderContext frc;
      int penX;
      int penY;
      int rowHeight;
      final SmoothFont.Glyph[] ascii = new SmoothFont.Glyph[128];
      final SmoothFont.CharMap other = new SmoothFont.CharMap();

      private Atlas() {
      }
   }

   private static final class CharMap {
      private char[] keys = new char[64];
      private SmoothFont.Glyph[] values = new SmoothFont.Glyph[64];
      private int size;

      private CharMap() {
      }

      SmoothFont.Glyph get(char c) {
         int mask = this.keys.length - 1;

         SmoothFont.Glyph v;
         for (int i = c * '鸷' & mask; (v = this.values[i]) != null; i = i + 1 & mask) {
            if (this.keys[i] == c) {
               return v;
            }
         }

         return null;
      }

      void put(char c, SmoothFont.Glyph g) {
         if ((this.size + 1) * 2 > this.keys.length) {
            char[] ok = this.keys;
            SmoothFont.Glyph[] ov = this.values;
            this.keys = new char[ok.length * 2];
            this.values = new SmoothFont.Glyph[ov.length * 2];
            this.size = 0;

            for (int i = 0; i < ok.length; i++) {
               if (ov[i] != null) {
                  this.put(ok[i], ov[i]);
               }
            }
         }

         int mask = this.keys.length - 1;

         int ix;
         for (ix = c * '鸷' & mask; this.values[ix] != null; ix = ix + 1 & mask) {
            if (this.keys[ix] == c) {
               this.values[ix] = g;
               return;
            }
         }

         this.keys[ix] = c;
         this.values[ix] = g;
         this.size++;
      }
   }

   public static final class Glyph {
      public float advance;
      public int x;
      public int y;
      public int w;
      public int h;
      public int offX;
      public int offY;
   }
}
