package meowtils.furiahvisuals.asset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import meowtils.furiahvisuals.core.FuriahsVisuals;

public final class AssetSource {
   private static final String ROOT = "/assets/furiahsvisuals/";
   private static final int MAX_BYTES = 33554432;

   private AssetSource() {
   }

   public static InputStream open(String path) {
      return safe(path) ? ExtensionResources.open("/assets/furiahsvisuals/" + path) : null;
   }

   public static byte[] bytes(String path) {
      try (InputStream in = open(path)) {
         if (in != null) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];

            int n;
            while ((n = in.read(buffer)) != -1) {
               if (out.size() > 33554432 - n) {
                  throw new IOException("Resource too large");
               }

               out.write(buffer, 0, n);
            }

            return out.toByteArray();
         } else {
            return null;
         }
      } catch (IOException var19) {
         FuriahsVisuals.LOG.warn("Could not read resource {}", new Object[]{path});
         return null;
      }
   }

   public static String text(String path) {
      byte[] value = bytes(path);
      return value == null ? null : new String(value, StandardCharsets.UTF_8);
   }

   public static boolean ready() {
      return true;
   }

   public static int generation() {
      return 0;
   }

   public static void addListener(Runnable listener) {
   }

   public static boolean safe(String path) {
      if (path != null && !path.isEmpty() && !path.startsWith("/") && path.indexOf(92) < 0 && path.indexOf(58) < 0 && path.indexOf(0) < 0) {
         for (String part : path.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }
}
