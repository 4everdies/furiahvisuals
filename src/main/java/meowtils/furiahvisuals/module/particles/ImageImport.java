package meowtils.furiahvisuals.module.particles;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;
import meowtils.furiahvisuals.compat.OpenFolder;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.image.ImageLoader;
import net.minecraft.client.Minecraft;

final class ImageImport {
   private static volatile boolean picking;

   private ImageImport() {
   }

   static boolean isPicking() {
      return picking;
   }

   static void pick(final ImageLibrary library) {
      if (!picking) {
         if (!Minecraft.isRunningOnMac && !GraphicsEnvironment.isHeadless()) {
            picking = true;
            Thread t = new Thread(new Runnable() {
               @Override
               public void run() {
                  Frame owner = null;

                  try {
                     owner = new Frame();
                     owner.setUndecorated(true);
                     owner.setAlwaysOnTop(true);
                     owner.setSize(1, 1);
                     owner.setLocationRelativeTo(null);
                     owner.setVisible(true);
                     FileDialog dialog = new FileDialog(owner, "Import particle images (PNG, JPG, GIF or BMP)", 0);
                     dialog.setMultipleMode(true);
                     dialog.setFilenameFilter(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                           return ImageLoader.isImageName(name);
                        }
                     });
                     if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
                        dialog.setFile("*.png;*.jpg;*.jpeg;*.jfif;*.gif;*.bmp;*.webp;*.avif;*.heic");
                     }

                     dialog.setVisible(true);
                     File[] files = dialog.getFiles();
                     dialog.dispose();
                     if (files != null && files.length != 0) {
                        for (File f : files) {
                           library.importFile(f);
                        }

                        return;
                     }
                  } catch (Throwable var11) {
                     FuriahsVisuals.LOG.warn("Particles: the file picker failed, opening the folder instead", var11);
                     ImageImport.openFolder(library);
                     library.post(new Runnable() {
                        @Override
                        public void run() {
                           library.say("The file picker could not open: drop your images into the folder that just opened, then press Reload.", true);
                        }
                     });
                     return;
                  } finally {
                     if (owner != null) {
                        owner.dispose();
                     }

                     ImageImport.picking = false;
                  }
               }
            }, "Furiah's Visuals file picker");
            t.setDaemon(true);
            t.start();
         } else {
            openFolder(library);
            library.say("The file picker cannot open next to the game here: drop your images into the folder that just opened, then press Reload.", false);
         }
      }
   }

   static void openFolder(ImageLibrary library) {
      final File dir = library.getDir();
      Thread t = new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               OpenFolder.open(dir);
            } catch (Throwable var2) {
               FuriahsVisuals.LOG.warn("Particles: could not open {}", new Object[]{dir, var2});
            }
         }
      }, "Furiah's Visuals open folder");
      t.setDaemon(true);
      t.start();
   }
}
