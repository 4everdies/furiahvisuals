package meowtils.furiahvisuals.module.sky;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.image.ImageLoader;
import meowtils.furiahvisuals.render.ColorUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;

final class SkyImage {
   private static final int MAX_WIDTH = 4096;
   private static final int LOAD_SIDE = 8192;
   private static final long CHECK_EVERY_MS = 2000L;
   private final File file;
   private int texture = -1;
   private long loadedModified = -1L;
   private long lastCheck;
   private int horizon = -13420476;
   private volatile SkyImage.Loaded ready;
   private volatile boolean loading;

   SkyImage(File file) {
      this.file = file;
   }

   File getFile() {
      return this.file;
   }

   int texture() {
      SkyImage.Loaded done = this.ready;
      if (done != null) {
         this.ready = null;
         this.upload(done);
      }

      long now = System.currentTimeMillis();
      if (!this.loading && (now - this.lastCheck >= 2000L || this.texture < 0 && this.lastCheck == 0L)) {
         this.lastCheck = now;
         long modified = this.file.isFile() ? this.file.lastModified() : -1L;
         if (modified != this.loadedModified) {
            this.loadedModified = modified;
            if (modified < 0L) {
               this.delete();
            } else {
               this.load(modified);
            }
         }
      }

      return this.texture;
   }

   int horizonColor() {
      return this.horizon;
   }

   private void load(final long modified) {
      this.loading = true;
      Thread t = new Thread(
         new Runnable() {
            @Override
            public void run() {
               try {
                  ImageLoader.Image loaded = ImageLoader.load(SkyImage.this.file, 8192, true);
                  BufferedImage image = loaded.toBufferedImage();
                  if (image.getWidth() > 4096) {
                     int w = 4096;
                     int h = Math.max(1, image.getHeight() * w / image.getWidth());
                     BufferedImage scaled = new BufferedImage(w, h, 2);
                     Graphics2D g = scaled.createGraphics();
                     g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                     g.drawImage(image, 0, 0, w, h, null);
                     g.dispose();
                     image = scaled;
                  }

                  SkyImage.this.ready = new SkyImage.Loaded(image, SkyImage.averageMiddle(image), modified);
                  FuriahsVisuals.LOG
                     .info(
                        "Loaded sky panorama {} ({}x{}, from {}x{})",
                        new Object[]{SkyImage.this.file, image.getWidth(), image.getHeight(), loaded.fileWidth, loaded.fileHeight}
                     );
               } catch (ImageLoader.LoadException var11) {
                  SkyImage.this.ready = new SkyImage.Loaded(null, 0, modified);
               } catch (RuntimeException | OutOfMemoryError var12) {
                  FuriahsVisuals.LOG.warn("Could not load the sky panorama {}", new Object[]{SkyImage.this.file, var12});
                  SkyImage.this.ready = new SkyImage.Loaded(null, 0, modified);
               } finally {
                  SkyImage.this.loading = false;
               }
            }
         },
         "Furiah's Visuals sky image"
      );
      t.setDaemon(true);
      t.setPriority(1);
      t.start();
   }

   private void upload(SkyImage.Loaded done) {
      if (done.modified == this.loadedModified) {
         this.delete();
         if (done.image != null) {
            try {
               this.horizon = done.horizon;
               this.texture = TextureUtil.glGenTextures();
               TextureUtil.uploadTextureImageAllocate(this.texture, done.image, true, false);
               GlStateManager.bindTexture(this.texture);
               GL11.glTexParameteri(3553, 10243, 33071);
            } catch (RuntimeException var3) {
               FuriahsVisuals.LOG.warn("Could not upload the sky panorama {}", new Object[]{this.file, var3});
               this.delete();
            }
         }
      }
   }

   private void delete() {
      if (this.texture >= 0) {
         TextureUtil.deleteTexture(this.texture);
         this.texture = -1;
      }
   }

   private static int averageMiddle(BufferedImage image) {
      int w = image.getWidth();
      int h = image.getHeight();
      long r = 0L;
      long g = 0L;
      long b = 0L;
      long n = 0L;
      int y0 = (int)(h * 0.45);
      int y1 = Math.max(y0 + 1, (int)(h * 0.55));
      int step = Math.max(1, w / 256);

      for (int y = y0; y < y1; y++) {
         for (int x = 0; x < w; x += step) {
            int c = image.getRGB(x, y);
            r += c >> 16 & 0xFF;
            g += c >> 8 & 0xFF;
            b += c & 0xFF;
            n++;
         }
      }

      return n == 0L ? -13420476 : ColorUtil.argb(255, (int)(r / n), (int)(g / n), (int)(b / n));
   }

   void release() {
      this.delete();
      this.ready = null;
      this.loadedModified = -1L;
      this.lastCheck = 0L;
   }

   private static final class Loaded {
      final BufferedImage image;
      final int horizon;
      final long modified;

      Loaded(BufferedImage image, int horizon, long modified) {
         this.image = image;
         this.horizon = horizon;
         this.modified = modified;
      }
   }
}
