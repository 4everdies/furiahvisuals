package meowtils.furiahvisuals.module.watermark;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;

final class ChatDetect {
   private static final Map<Class<?>, Method> LABY_SCREEN = new HashMap<>();
   private static GuiScreen lastScreen;
   private static boolean lastResult;
   private static long lastCheckNanos;

   private ChatDetect() {
   }

   static boolean isChat(GuiScreen screen) {
      if (screen == null) {
         return false;
      } else if (screen instanceof GuiChat) {
         return true;
      } else {
         long now = System.nanoTime();
         if (screen == lastScreen && now - lastCheckNanos < 50000000L) {
            return lastResult;
         } else {
            boolean result = false;
            Object inner = labyScreen(screen);

            for (Class<?> c = inner == null ? null : inner.getClass(); c != null; c = c.getSuperclass()) {
               if (c.getSimpleName().contains("ChatInput")) {
                  result = true;
                  break;
               }
            }

            lastScreen = screen;
            lastResult = result;
            lastCheckNanos = now;
            return result;
         }
      }
   }

   private static Object labyScreen(GuiScreen screen) {
      Class<?> type = screen.getClass();
      Method method;
      if (LABY_SCREEN.containsKey(type)) {
         method = LABY_SCREEN.get(type);
      } else {
         method = null;
         if ("LabyScreenRenderer".equals(type.getSimpleName())) {
            try {
               method = type.getMethod("screen");
               method.setAccessible(true);
            } catch (Exception var5) {
               method = null;
            }
         }

         LABY_SCREEN.put(type, method);
      }

      if (method == null) {
         return null;
      } else {
         try {
            return method.invoke(screen);
         } catch (Exception var4) {
            return null;
         }
      }
   }
}
