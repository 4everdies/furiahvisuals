package meowtils.furiahvisuals.module.ui.nametag;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.hud.HudGl;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

final class NameGlow {
   private static final int PX = 3;
   static final float PAD = 5.0F;
   private static final int MAX = 96;
   private final Map<String, NameGlow.Entry> cache = new LinkedHashMap<String, NameGlow.Entry>(64, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(java.util.Map.Entry<String, NameGlow.Entry> eldest) {
         if (this.size() > 96) {
            NameGlow.release(eldest.getValue());
            return true;
         } else {
            return false;
         }
      }
   };
   private ExecutorService worker;

   NameGlow.Entry get(final String plain, boolean bold) {
      if (plain.isEmpty()) {
         return null;
      } else {
         String key = (bold ? 49 : 48) + plain;
         NameGlow.Entry e = this.cache.get(key);
         if (e == null) {
            final NameGlow.Entry made = new NameGlow.Entry();
            this.cache.put(key, made);
            Ui.ensureFonts();
            final Font font = (bold ? Ui.bold : Ui.font).awt(30.0F);
            this.worker().execute(new Runnable() {
               @Override
               public void run() {
                  NameGlow.build(made, plain, font);
               }
            });
            return null;
         } else if (e.failed) {
            return null;
         } else {
            if (e.texture == null && e.pixels != null) {
               upload(e);
            }

            return e.texture != null ? e : null;
         }
      }
   }

   private ExecutorService worker() {
      if (this.worker == null) {
         this.worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
               Thread t = new Thread(r, "Furiah's Visuals name glow");
               t.setDaemon(true);
               t.setPriority(1);
               return t;
            }
         });
      }

      return this.worker;
   }

   private static void build(NameGlow.Entry e, String text, Font face) {
      try {
         BufferedImage probe = new BufferedImage(1, 1, 2);
         Graphics2D pg = probe.createGraphics();
         Rectangle caps = face.createGlyphVector(pg.getFontRenderContext(), "H").getPixelBounds(pg.getFontRenderContext(), 0.0F, 0.0F);
         Font font = caps.height > 0 ? face.deriveFont(face.getSize2D() * 7.0F * 3.0F / caps.height) : face;
         pg.setFont(font);
         FontMetrics fm = pg.getFontMetrics();
         int textPx = Math.max(1, fm.stringWidth(text));
         pg.dispose();
         int pad = Math.round(15.0F);
         int w = textPx + pad * 2;
         int h = 27 + pad * 2;
         BufferedImage img = new BufferedImage(w, h, 2);
         Graphics2D g = img.createGraphics();
         g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         g.setFont(font);
         g.setColor(Color.WHITE);
         g.drawString(text, pad, pad + 21);
         g.dispose();
         int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
         float[] a = new float[w * h];

         for (int i = 0; i < a.length; i++) {
            a[i] = (argb[i] >>> 24) / 255.0F;
         }

         int r = Math.max(1, Math.round(3.6000001F));
         float[] tmp = new float[a.length];

         for (int pass = 0; pass < 3; pass++) {
            boxH(a, tmp, w, h, r);
            boxV(tmp, a, w, h, r);
         }

         int[] out = new int[w * h];

         for (int i = 0; i < out.length; i++) {
            int alpha = Math.min(255, Math.round(a[i] * 255.0F * 1.7F));
            out[i] = alpha << 24 | 16777215;
         }

         e.w = w;
         e.h = h;
         e.textW = textPx / 3.0F;
         e.pixels = out;
      } catch (Throwable var21) {
         FuriahsVisuals.LOG.warn("UI: a name's glow could not be made", var21);
         e.failed = true;
      }
   }

   private static void boxH(float[] src, float[] dst, int w, int h, int r) {
      float k = 1.0F / (2 * r + 1);

      for (int y = 0; y < h; y++) {
         int row = y * w;
         float sum = 0.0F;

         for (int x = -r; x <= r; x++) {
            sum += src[row + Math.max(0, Math.min(w - 1, x))];
         }

         for (int var10 = 0; var10 < w; var10++) {
            dst[row + var10] = sum * k;
            sum += src[row + Math.min(w - 1, var10 + r + 1)] - src[row + Math.max(0, var10 - r)];
         }
      }
   }

   private static void boxV(float[] src, float[] dst, int w, int h, int r) {
      float k = 1.0F / (2 * r + 1);

      for (int x = 0; x < w; x++) {
         float sum = 0.0F;

         for (int y = -r; y <= r; y++) {
            sum += src[Math.max(0, Math.min(h - 1, y)) * w + x];
         }

         for (int var9 = 0; var9 < h; var9++) {
            dst[var9 * w + x] = sum * k;
            sum += src[Math.min(h - 1, var9 + r + 1) * w + x] - src[Math.max(0, var9 - r) * w + x];
         }
      }
   }

   private static void upload(NameGlow.Entry e) {
      DynamicTexture t = new DynamicTexture(e.w, e.h);
      System.arraycopy(e.pixels, 0, t.getTextureData(), 0, e.pixels.length);
      t.updateDynamicTexture();
      GlStateManager.bindTexture(t.getGlTextureId());
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      e.texture = t;
      e.pixels = null;
   }

   private static void release(NameGlow.Entry e) {
      if (e.texture != null) {
         e.texture.deleteGlTexture();
         e.texture = null;
      }
   }

   static void draw(NameGlow.Entry e, float left, float top, float width, NameGlow.Colors colors, float alpha) {
      float scale = e.textW > 0.0F ? width / e.textW : 1.0F;
      float x0 = left - 5.0F * scale;
      float x1 = left + width + 5.0F * scale;
      float y0 = top - 5.0F;
      float y1 = top + 9.0F + 5.0F;
      HudGl.set(3553, true);
      HudGl.bindTexture(e.texture.getGlTextureId());
      HudGl.blendFunc(770, 1, 770, 1);
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      int segments = 12;

      for (int i = 0; i < segments; i++) {
         float ua = (float)i / segments;
         float ub = (float)(i + 1) / segments;
         float xa = x0 + (x1 - x0) * ua;
         float xb = x0 + (x1 - x0) * ub;
         int ca = colors.at(Math.max(0.0F, Math.min(1.0F, (xa - left) / width)));
         int cb = colors.at(Math.max(0.0F, Math.min(1.0F, (xb - left) / width)));
         int aa;
         int ab = aa = Math.round(255.0F * alpha);
         wr.pos(xa, y1, 0.0).tex(ua, 1.0).color(ca >> 16 & 0xFF, ca >> 8 & 0xFF, ca & 0xFF, aa).endVertex();
         wr.pos(xb, y1, 0.0).tex(ub, 1.0).color(cb >> 16 & 0xFF, cb >> 8 & 0xFF, cb & 0xFF, ab).endVertex();
         wr.pos(xb, y0, 0.0).tex(ub, 0.0).color(cb >> 16 & 0xFF, cb >> 8 & 0xFF, cb & 0xFF, ab).endVertex();
         wr.pos(xa, y0, 0.0).tex(ua, 0.0).color(ca >> 16 & 0xFF, ca >> 8 & 0xFF, ca & 0xFF, aa).endVertex();
      }

      tess.draw();
      HudGl.blendFunc(770, 771, 1, 0);
   }

   void clear() {
      Iterator<NameGlow.Entry> it = this.cache.values().iterator();

      while (it.hasNext()) {
         release(it.next());
         it.remove();
      }
   }

   int size() {
      return this.cache.size();
   }

   interface Colors {
      int at(float var1);
   }

   static final class Entry {
      volatile int[] pixels;
      volatile boolean failed;
      int w;
      int h;
      float textW;
      DynamicTexture texture;
   }
}
