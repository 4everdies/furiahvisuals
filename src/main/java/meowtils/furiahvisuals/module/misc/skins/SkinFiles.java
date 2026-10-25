package meowtils.furiahvisuals.module.misc.skins;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;
import meowtils.furiahvisuals.compat.OpenFolder;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import net.minecraft.client.Minecraft;

final class SkinFiles {
   private static volatile boolean picking;

   private SkinFiles() {
   }

   static boolean isPicking() {
      return picking;
   }

   static void pick(final SkinLibrary library) {
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
                     FileDialog dialog = new FileDialog(owner, "Upload skins (PNG: 64×64, 64×32 or HD)", 0);
                     dialog.setMultipleMode(true);
                     dialog.setFilenameFilter(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                           return name.toLowerCase(Locale.ROOT).endsWith(".png");
                        }
                     });
                     if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
                        dialog.setFile("*.png");
                     }

                     dialog.setVisible(true);
                     File[] files = dialog.getFiles();
                     dialog.dispose();
                     if (files != null) {
                        for (File f : files) {
                           library.importFile(f);
                        }

                        return;
                     }
                  } catch (Throwable var11) {
                     FuriahsVisuals.LOG.warn("Skins: the file picker failed, opening the folder instead", var11);
                     SkinFiles.openFolder(library.getDir());
                     library.post(new Runnable() {
                        @Override
                        public void run() {
                           library.say("The file picker could not open: drop your skins into the folder that just opened, then press Reload.", true);
                        }
                     });
                     return;
                  } finally {
                     if (owner != null) {
                        owner.dispose();
                     }

                     SkinFiles.picking = false;
                  }
               }
            }, "Furiah's Visuals skin picker");
            t.setDaemon(true);
            t.start();
         } else {
            openFolder(library.getDir());
            library.say("The file picker can't open next to the game here: drop your skins into the folder that just opened, then press Reload.", false);
         }
      }
   }

   static void openFolder(final File dir) {
      run(new Runnable() {
         @Override
         public void run() {
            try {
               OpenFolder.open(dir);
            } catch (Throwable var2) {
               FuriahsVisuals.LOG.warn("Skins: could not open {}", new Object[]{dir, var2});
            }
         }
      }, "Furiah's Visuals open folder");
   }

   static void showInFolder(final File file) {
      run(new Runnable() {
         @Override
         public void run() {
            try {
               OpenFolder.open(file.getAbsoluteFile().getParentFile());
            } catch (Throwable var2) {
               FuriahsVisuals.LOG.warn("Skins: could not show {}", new Object[]{file, var2});
            }
         }
      }, "Furiah's Visuals show in folder");
   }

   private static void run(Runnable r, String name) {
      Thread t = new Thread(r, name);
      t.setDaemon(true);
      t.start();
   }
}
