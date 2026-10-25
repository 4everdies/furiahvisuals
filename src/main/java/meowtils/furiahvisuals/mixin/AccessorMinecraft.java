package meowtils.furiahvisuals.mixin;

import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

public final class AccessorMinecraft {
   private static final Field TIMER = timerField();

   private AccessorMinecraft() {
   }

   private static Field timerField() {
      Field f = Access.field(Minecraft.class, "timer", "timer");
      if (f != null) {
         return f;
      }

      for (Field c : Minecraft.class.getDeclaredFields()) {
         if (c.getType() == Timer.class && !java.lang.reflect.Modifier.isStatic(c.getModifiers())) {
            c.setAccessible(true);
            return c;
         }
      }

      return null;
   }

   public static boolean available() {
      return TIMER != null;
   }

   public static Timer getFvTimer(Minecraft mc) {
      return TIMER == null ? null : (Timer)Access.get(TIMER, mc);
   }
}
