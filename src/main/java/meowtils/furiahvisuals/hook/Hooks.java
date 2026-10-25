package meowtils.furiahvisuals.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import meowtils.furiahvisuals.core.FuriahsVisuals;

public final class Hooks {
   public static final Set<String> INSTALLED = Collections.synchronizedSet(new HashSet<>());

   private Hooks() {
   }

   public static boolean has(String hook) {
      return INSTALLED.contains(hook);
   }

   static void on(String... hooks) {
      Collections.addAll(INSTALLED, hooks);
   }

   static void off(String... hooks) {
      for (String h : hooks) {
         INSTALLED.remove(h);
      }
   }

   public static Field field(Class<?> owner, String... names) {
      for (String n : Srg.names(owner, false, names)) {
         try {
            Field f = owner.getDeclaredField(n);
            f.setAccessible(true);
            return f;
         } catch (NoSuchFieldException var7) {
         }
      }

      FuriahsVisuals.LOG.warn("Hooks: field {}.{} was not found; whatever needs it stays off", new Object[]{owner.getSimpleName(), names.length == 0 ? "?" : names[0]});
      return null;
   }

   public static Method method(Class<?> owner, String[] names, Class<?>... params) {
      for (String n : Srg.names(owner, true, names)) {
         try {
            Method m = owner.getDeclaredMethod(n, params);
            m.setAccessible(true);
            return m;
         } catch (NoSuchMethodException var8) {
         }
      }

      FuriahsVisuals.LOG.warn("Hooks: method {}.{} was not found; whatever needs it stays off", new Object[]{owner.getSimpleName(), names.length == 0 ? "?" : names[0]});
      return null;
   }
}
