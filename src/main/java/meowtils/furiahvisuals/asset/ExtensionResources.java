package meowtils.furiahvisuals.asset;

import java.io.InputStream;
import java.lang.reflect.Field;

public final class ExtensionResources {
   private ExtensionResources() {
   }

   public static InputStream open(String path) {
      String relative = path.startsWith("/") ? path.substring(1) : path;
      InputStream in = ExtensionResources.class.getResourceAsStream(path.startsWith("/") ? path : "/" + path);
      if (in != null) {
         return in;
      } else {
         ClassLoader own = ExtensionResources.class.getClassLoader();
         if (own == null) {
            return null;
         } else {
            in = own.getResourceAsStream(relative);
            return in != null ? in : fromDelegate(own, relative);
         }
      }
   }

   private static InputStream fromDelegate(ClassLoader loader, String relative) {
      for (Class<?> type = loader.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
         for (Field field : type.getDeclaredFields()) {
            if (ClassLoader.class.isAssignableFrom(field.getType())) {
               try {
                  field.setAccessible(true);
                  ClassLoader delegate = (ClassLoader)field.get(loader);
                  if (delegate != null && delegate != loader) {
                     InputStream in = delegate.getResourceAsStream(relative);
                     if (in != null) {
                        return in;
                     }
                  }
               } catch (Throwable var9) {
               }
            }
         }
      }

      return null;
   }
}
