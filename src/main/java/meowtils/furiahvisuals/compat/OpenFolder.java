package meowtils.furiahvisuals.compat;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;

public final class OpenFolder {
   private OpenFolder() {
   }

   public static void open(File dir) throws IOException {
      if (!dir.isDirectory()) {
         dir.mkdirs();
      }

      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.OPEN)) {
         Desktop.getDesktop().open(dir.getAbsoluteFile());
      } else {
         throw new IOException("opening folders is not supported here");
      }
   }
}
