package meowtils.furiahvisuals.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectionHelper {
   private ReflectionHelper() {
   }

   public static Field findField(Class<?> clazz, String... fieldNames) {
      Exception failed = null;

      for (String name : meowtils.furiahvisuals.hook.Srg.names(clazz, false, fieldNames)) {
         try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
         } catch (Exception var8) {
            failed = var8;
         }
      }

      throw new ReflectionHelper.UnableToFindFieldException(fieldNames, failed);
   }

   public static Method findMethod(Class<?> clazz, Object instance, String[] methodNames, Class<?>... methodTypes) {
      Exception failed = null;

      for (String name : meowtils.furiahvisuals.hook.Srg.names(clazz, true, methodNames)) {
         try {
            Method m = clazz.getDeclaredMethod(name, methodTypes);
            m.setAccessible(true);
            return m;
         } catch (Exception var10) {
            failed = var10;
         }
      }

      throw new ReflectionHelper.UnableToFindMethodException(methodNames, failed);
   }

   public static class UnableToFindFieldException extends RuntimeException {
      public UnableToFindFieldException(String[] names, Exception cause) {
         super(String.join("/", names), cause);
      }
   }

   public static class UnableToFindMethodException extends RuntimeException {
      public UnableToFindMethodException(String[] names, Exception cause) {
         super(String.join("/", names), cause);
      }
   }
}
