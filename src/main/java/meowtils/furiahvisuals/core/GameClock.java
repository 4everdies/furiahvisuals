package meowtils.furiahvisuals.core;

import net.minecraft.client.Minecraft;

public final class GameClock {
   private static long ticks;
   private static double now = Double.NaN;
   private static float dt;
   private static boolean paused = true;
   private static long frame;
   private static float partialTicks;

   private GameClock() {
   }

   static void tick(Minecraft mc) {
      if (mc.theWorld != null && !mc.isGamePaused()) {
         ticks++;
      }
   }

   static void frame(Minecraft mc, float partialTicks) {
      frame++;
      GameClock.partialTicks = partialTicks;
      paused = mc.theWorld == null || mc.isGamePaused();
      if (paused) {
         dt = 0.0F;
      } else {
         double t = ((float)ticks + Math.max(0.0F, Math.min(1.0F, partialTicks))) / 20.0;
         if (Double.isNaN(now)) {
            now = t;
            dt = 0.0F;
         } else {
            double d = t - now;
            if (d > 0.0) {
               dt = (float)Math.min(0.5, d);
               now = t;
            } else {
               dt = 0.0F;
            }
         }
      }
   }

   public static double seconds() {
      return Double.isNaN(now) ? 0.0 : now;
   }

   public static long nanos() {
      return Double.isNaN(now) ? 0L : Math.round(now * 1.0E9);
   }

   public static long millis() {
      return Double.isNaN(now) ? 0L : Math.round(now * 1000.0);
   }

   public static float dt() {
      return dt;
   }

   public static boolean paused() {
      return paused;
   }

   public static long frame() {
      return frame;
   }

   public static float partialTicks() {
      return partialTicks;
   }
}
