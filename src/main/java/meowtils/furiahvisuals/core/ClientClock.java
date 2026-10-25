package meowtils.furiahvisuals.core;

import meowtils.furiahvisuals.mixin.AccessorMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

public final class ClientClock {
   private static final long TICK_NANOS = 50000000L;
   private static long ticks;
   private static long nanos;
   private static boolean timerChecked;
   private static boolean timerReadable;

   private ClientClock() {
   }

   static void tick() {
      ticks++;
   }

   static void frame(Minecraft mc, float partialTicks) {
      float partial = partialTicks;
      Timer timer = timer(mc);
      if (timer != null) {
         partial = timer.elapsedPartialTicks;
      }

      long t;
      if ((t = ticks * 50000000L + (long)(Math.max(0.0F, Math.min(1.0F, partial)) * 5.0E7F)) > nanos) {
         nanos = t;
      }
   }

   private static Timer timer(Minecraft mc) {
      if (!timerChecked) {
         timerChecked = true;
         timerReadable = AccessorMinecraft.available();
         if (!timerReadable) {
            FuriahsVisuals.LOG.warn("Minecraft's timer is out of reach: while the game is paused, the menus' animations move once a tick");
         }
      }

      return timerReadable ? AccessorMinecraft.getFvTimer(mc) : null;
   }

   public static long nanos() {
      return nanos;
   }

   public static long millis() {
      return nanos / 1000000L;
   }

   public static double seconds() {
      return nanos / 1.0E9;
   }
}
