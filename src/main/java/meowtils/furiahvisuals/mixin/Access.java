package meowtils.furiahvisuals.mixin;

import java.lang.reflect.Field;
import meowtils.furiahvisuals.hook.Hooks;

final class Access {
   private Access() {
   }

   static Field field(Class<?> owner, String mcp, String srg) {
      return Hooks.field(owner, mcp, srg);
   }

   static int getInt(Field f, Object o) {
      try {
         return f.getInt(o);
      } catch (Exception var3) {
         return 0;
      }
   }

   static void setInt(Field f, Object o, int v) {
      try {
         f.setInt(o, v);
      } catch (Exception var4) {
      }
   }

   static Object get(Field f, Object o) {
      try {
         return f.get(o);
      } catch (Exception var3) {
         return null;
      }
   }
}
