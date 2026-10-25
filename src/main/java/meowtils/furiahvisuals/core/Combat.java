package meowtils.furiahvisuals.core;

import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.targetaura.TargetAuraModule;
import net.minecraft.client.Minecraft;

public final class Combat {
   private static final long WINDOW_MS = 5000L;
   private static final long NEVER = -2305843009213693952L;
   private static long lastFightMs = -2305843009213693952L;
   private static int lastHurtTime;

   private Combat() {
   }

   static void attacked() {
      lastFightMs = GameClock.millis();
   }

   static void tick(Minecraft mc) {
      if (mc.thePlayer == null) {
         lastHurtTime = 0;
      } else {
         int hurt = mc.thePlayer.hurtTime;
         if (hurt > lastHurtTime) {
            lastFightMs = GameClock.millis();
         }

         lastHurtTime = hurt;
      }
   }

   public static boolean fighting() {
      if (GameClock.millis() - lastFightMs < 5000L) {
         return true;
      } else {
         Module aura = FuriahsVisuals.instance.getModules().get("target_aura");
         return aura instanceof TargetAuraModule && ((TargetAuraModule)aura).hasTarget();
      }
   }

   public static void fightForTest(boolean fighting) {
      lastFightMs = fighting ? GameClock.millis() : -2305843009213693952L;
   }
}
