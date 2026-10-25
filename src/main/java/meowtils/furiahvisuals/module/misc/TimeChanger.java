package meowtils.furiahvisuals.module.misc;

import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.module.ui.UiHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.World;

final class TimeChanger {
   private static final double DAY = 24000.0;
   private static final long GLIDE_NANOS = 1000000000L;
   private final MiscModule misc;
   private Boolean hooked;
   private double shown = Double.NaN;
   private double offset;
   private long glideFrom;
   private boolean wasOn;
   private int wasMinute = -1;
   private World wasWorld;

   TimeChanger(MiscModule misc) {
      this.misc = misc;
   }

   boolean hooked() {
      if (this.hooked == null) {
         this.hooked = UiHooks.installed("celestialAngle", World.class);
      }

      return this.hooked;
   }

   static double ticksOf(int minute) {
      return (minute - 360 + 1440) % 1440 * 1000.0 / 60.0;
   }

   static int minuteOf(double ticks) {
      int m = (int)Math.round(wrap(ticks) * 60.0 / 1000.0) + 360;
      return (m % 1440 + 1440) % 1440;
   }

   private static double wrap(double t) {
      double r = t % 24000.0;
      return r < 0.0 ? r + 24000.0 : r;
   }

   private static double delta(double a, double b) {
      double d = wrap(b - a);
      return d > 12000.0 ? d - 24000.0 : d;
   }

   void frame(Minecraft mc, float partialTicks) {
      WorldClient w = mc.theWorld;
      boolean on = this.misc.timeActive();
      if (w != null && w.provider != null && w.provider.getDimensionId() == 0) {
         double real = wrap((double)w.getWorldTime() + partialTicks);
         int minute = this.misc.timeMinute();
         double target = on ? ticksOf(minute) : real;
         if (Double.isNaN(this.shown)) {
            if (!on) {
               this.wasOn = false;
               this.wasWorld = w;
               return;
            }

            this.shown = w == this.wasWorld ? real : target;
            this.wasOn = false;
         }

         long now = GameClock.nanos();
         if (on != this.wasOn || on && minute != this.wasMinute || w != this.wasWorld) {
            this.offset = w == this.wasWorld && this.misc.timeSmooth() ? delta(target, this.shown) : 0.0;
            this.glideFrom = now;
            this.wasOn = on;
            this.wasMinute = minute;
            this.wasWorld = w;
         }

         double k = this.misc.timeSmooth() ? Math.min(1.0, (now - this.glideFrom) / 1.0E9) : 1.0;
         double ease = k * k * (3.0 - 2.0 * k);
         this.shown = wrap(target + this.offset * (1.0 - ease));
         if (!on && k >= 1.0) {
            this.shown = Double.NaN;
         }
      } else {
         this.shown = Double.NaN;
         this.wasWorld = w;
         this.wasOn = on;
      }
   }

   float angle(World w) {
      double s = this.shown;
      if (!Double.isNaN(s) && w == Minecraft.getMinecraft().theWorld) {
         long t = (long)Math.floor(s);
         return w.provider.calculateCelestialAngle(t, (float)(s - t));
      } else {
         return Float.NaN;
      }
   }

   double shownForTest() {
      return this.shown;
   }
}
