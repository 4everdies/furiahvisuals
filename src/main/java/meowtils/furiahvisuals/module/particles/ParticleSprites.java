package meowtils.furiahvisuals.module.particles;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

final class ParticleSprites implements IResourceManagerReloadListener {
   static final ResourceLocation PARTICLES = new ResourceLocation("textures/particle/particles.png");
   private static final ResourceLocation SNOWBALL = new ResourceLocation("textures/items/snowball.png");
   static final int CRIT_SPRITE = 65;
   static final int SHARP_SPRITE = 66;
   private final int[] cellAverage = new int[256];
   private final float[] cellGlow = new float[256];
   private int[] pixels;
   private int width = 128;
   private int height = 128;
   private int snowballAverage = -854278;
   private boolean critPlain = true;
   private boolean sharpPlain = true;
   private int smoothTexture = -1;
   private boolean smoothDirty = true;
   private int version;

   ParticleSprites() {
      Arrays.fill(this.cellAverage, -1);
      Arrays.fill(this.cellGlow, 1.0F);
   }

   void register() {
      try {
         if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this);
         }
      } catch (RuntimeException var2) {
         FuriahsVisuals.LOG.warn("Particles: could not listen to resource reloads", var2);
      }
   }

   public void onResourceManagerReload(IResourceManager manager) {
      try {
         BufferedImage image = read(manager, PARTICLES);
         int w = image.getWidth();
         int h = image.getHeight();
         int[] p = new int[w * h];
         image.getRGB(0, 0, w, h, p, 0, w);
         this.pixels = p;
         this.width = w;
         this.height = h;
         int cw = Math.max(1, w / 16);
         int ch = Math.max(1, h / 16);

         for (int i = 0; i < 256; i++) {
            int x0 = i % 16 * cw;
            int y0 = i / 16 * ch;
            this.cellAverage[i] = x0 + cw <= w && y0 + ch <= h ? ImageOps.average(p, w, x0, y0, cw, ch) : -1;
            this.cellGlow[i] = ParticleRenderer.glowStrength(this.cellAverage[i]);
         }

         this.critPlain = isPlain(p, w, 65, cw, ch);
         this.sharpPlain = isPlain(p, w, 66, cw, ch);
      } catch (Exception var12) {
         FuriahsVisuals.LOG.warn("Particles: could not read particles.png: {}", new Object[]{var12.toString()});
         this.pixels = null;
      }

      try {
         BufferedImage snow = read(manager, SNOWBALL);
         int[] p = new int[snow.getWidth() * snow.getHeight()];
         snow.getRGB(0, 0, snow.getWidth(), snow.getHeight(), p, 0, snow.getWidth());
         this.snowballAverage = ImageOps.average(p, snow.getWidth(), 0, 0, snow.getWidth(), snow.getHeight()) | 0xFF000000;
      } catch (Exception var11) {
         this.snowballAverage = -854278;
      }

      this.smoothDirty = true;
      this.version++;
   }

   private static BufferedImage read(IResourceManager manager, ResourceLocation location) throws Exception {
      InputStream in = manager.getResource(location).getInputStream();
      return TextureUtil.readBufferedImage(in);
   }

   private static boolean isPlain(int[] p, int w, int sprite, int cw, int ch) {
      int x0 = sprite % 16 * cw;
      int y0 = sprite / 16 * ch;
      long chroma = 0L;
      long weight = 0L;
      int max = 0;

      for (int y = y0; y < y0 + ch; y++) {
         for (int x = x0; x < x0 + cw; x++) {
            int c = p[y * w + x];
            int a = c >>> 24;
            if (a >= 32) {
               int r = c >> 16 & 0xFF;
               int g = c >> 8 & 0xFF;
               int b = c & 0xFF;
               int spread = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
               chroma += (long)spread * a;
               weight += a;
               max = Math.max(max, spread);
            }
         }
      }

      return weight == 0L || chroma / weight < 14L && max < 48;
   }

   int width() {
      return this.width;
   }

   int height() {
      return this.height;
   }

   int version() {
      return this.version;
   }

   static int cell(float u, float v) {
      int x = Math.max(0, Math.min(15, (int)(u * 16.0F + 0.01F)));
      int y = Math.max(0, Math.min(15, (int)(v * 16.0F + 0.01F)));
      return y * 16 + x;
   }

   int cellAverage(int cell) {
      return this.cellAverage[cell];
   }

   float cellGlowStrength(int cell) {
      return this.cellGlow[cell];
   }

   int spriteAverage(int sprite) {
      return this.cellAverage[sprite & 0xFF];
   }

   int snowballAverage() {
      return this.snowballAverage;
   }

   boolean isPlain(boolean sharp) {
      return sharp ? this.sharpPlain : this.critPlain;
   }

   int smoothTexture() {
      if (!this.smoothDirty) {
         return this.smoothTexture;
      } else {
         this.smoothDirty = false;
         ImageOps.delete(this.smoothTexture);
         this.smoothTexture = -1;
         int[] p = this.pixels;
         if (p != null && this.width % 16 == 0 && this.height % 16 == 0) {
            try {
               int cw = this.width / 16;
               int ch = this.height / 16;
               int levels = 1;

               for (int c = Math.min(cw, ch); c >= 4; c >>= 1) {
                  levels++;
               }

               int[][] data = new int[levels][];
               int[] ws = new int[levels];
               int[] hs = new int[levels];
               int[] level = (int[])p.clone();
               int w = this.width;
               int h = this.height;

               for (int l = 0; l < levels; l++) {
                  bleedCells(level, w, h);
                  data[l] = level;
                  ws[l] = w;
                  hs[l] = h;
                  if (l + 1 < levels) {
                     level = ImageOps.halve(level, w, h);
                     w = Math.max(1, w / 2);
                     h = Math.max(1, h / 2);
                  }
               }

               this.smoothTexture = ImageOps.upload(data, ws, hs, true);
            } catch (RuntimeException | LinkageError var12) {
               FuriahsVisuals.LOG.warn("Particles: could not make the smooth particle texture", var12);
               this.smoothTexture = -1;
            }

            return this.smoothTexture;
         } else {
            return -1;
         }
      }
   }

   private static void bleedCells(int[] p, int w, int h) {
      int cw = w / 16;
      int ch = h / 16;
      if (cw >= 1 && ch >= 1) {
         int[] cell = new int[cw * ch];

         for (int cy = 0; cy < 16; cy++) {
            for (int cx = 0; cx < 16; cx++) {
               for (int y = 0; y < ch; y++) {
                  System.arraycopy(p, (cy * ch + y) * w + cx * cw, cell, y * cw, cw);
               }

               ImageOps.bleed(cell, cw, ch);

               for (int var9 = 0; var9 < ch; var9++) {
                  System.arraycopy(cell, var9 * cw, p, (cy * ch + var9) * w + cx * cw, cw);
               }
            }
         }
      }
   }
}
