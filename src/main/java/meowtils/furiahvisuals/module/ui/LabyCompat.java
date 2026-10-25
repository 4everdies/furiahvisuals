package meowtils.furiahvisuals.module.ui;

import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

public final class LabyCompat {
   private static boolean looked;
   private static boolean present;
   private static Method labyApi;
   private static final LabyCompat.Check ADVANCED_CHAT = new LabyCompat.Check();
   private static final LabyCompat.Check TAB_LIST = new LabyCompat.Check();
   private static final LabyCompat.Check SCOREBOARD = new LabyCompat.Check();
   private static Boolean testChat;
   private static Boolean testTab;
   private static Boolean testScoreboard;
   private static boolean cameraLooked;
   private static Method cameraYaw;
   private static Method cameraPitch;
   private static final float[] CAMERA = new float[2];

   private LabyCompat() {
   }

   public static boolean present() {
      look();
      return present;
   }

   public static boolean advancedChat() {
      return testChat != null ? testChat : ask(ADVANCED_CHAT, "config", "ingame", "advancedChat", "enabled", "get");
   }

   public static boolean customTabList() {
      return testTab != null ? testTab : ask(TAB_LIST, "config", "multiplayer", "customPlayerList", "get");
   }

   public static boolean scoreboardWidget() {
      if (testScoreboard != null) {
         return testScoreboard;
      } else {
         look();
         if (!present) {
            return false;
         } else {
            LabyCompat.Check c = SCOREBOARD;
            long now = System.nanoTime();
            if (c.at != 0L && now - c.at < 1000000000L) {
               return c.value;
            } else {
               c.at = now;

               try {
                  Object registry = call(labyApi.invoke(null), "hudWidgetRegistry");
                  Method byId = registry.getClass().getMethod("getById", String.class);
                  byId.setAccessible(true);
                  Object widget = byId.invoke(registry, "scoreboard");
                  c.value = widget != null && Boolean.TRUE.equals(call(widget, "isEnabled"));
               } catch (Throwable var6) {
                  c.value = false;
               }

               return c.value;
            }
         }
      }
   }

   public static float[] cameraAngles() {
      if (!cameraLooked) {
         cameraLooked = true;
         look();
         if (present) {
            try {
               Class<?> accessor = Class.forName("net.labymod.v1_8_9.client.renderer.EntityRendererAccessor", false, LabyCompat.class.getClassLoader());
               cameraYaw = accessor.getMethod("getCameraYaw");
               cameraPitch = accessor.getMethod("getCameraPitch");
            } catch (Throwable var3) {
               cameraPitch = null;
               cameraYaw = null;
            }
         }
      }

      if (cameraYaw == null) {
         return null;
      } else {
         EntityRenderer renderer = Minecraft.getMinecraft().entityRenderer;
         if (!cameraYaw.getDeclaringClass().isInstance(renderer)) {
            return null;
         } else {
            try {
               CAMERA[0] = ((Number)cameraYaw.invoke(renderer)).floatValue();
               CAMERA[1] = ((Number)cameraPitch.invoke(renderer)).floatValue();
               return CAMERA;
            } catch (Throwable var2) {
               cameraPitch = null;
               cameraYaw = null;
               return null;
            }
         }
      }
   }

   public static void overrideForTest(Boolean chat, Boolean tab, Boolean scoreboard) {
      testChat = chat;
      testTab = tab;
      testScoreboard = scoreboard;
   }

   private static boolean ask(LabyCompat.Check c, String... steps) {
      look();
      if (!present) {
         return false;
      } else {
         long now = System.nanoTime();
         if (c.at != 0L && now - c.at < 1000000000L) {
            return c.value;
         } else {
            c.at = now;

            try {
               Object o = labyApi.invoke(null);

               for (String step : steps) {
                  o = call(o, step);
               }

               c.value = Boolean.TRUE.equals(o);
            } catch (Throwable var9) {
               c.value = false;
            }

            return c.value;
         }
      }
   }

   private static void look() {
      if (!looked) {
         looked = true;

         try {
            Class<?> laby = Class.forName("net.labymod.api.Laby", false, LabyCompat.class.getClassLoader());
            labyApi = laby.getMethod("labyAPI");
            present = true;
         } catch (Throwable var1) {
            present = false;
         }
      }
   }

   private static Object call(Object target, String name) throws Exception {
      Method m = target.getClass().getMethod(name);
      m.setAccessible(true);
      return m.invoke(target);
   }

   private static final class Check {
      long at;
      boolean value;

      private Check() {
      }
   }
}
