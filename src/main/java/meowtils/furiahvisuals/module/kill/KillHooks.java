package meowtils.furiahvisuals.module.kill;

import meowtils.furiahvisuals.core.FuriahsVisuals;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public final class KillHooks {
   private static boolean failed;
   private static int poofsSkipped;

   private KillHooks() {
   }

   public static int poofsSkipped() {
      return poofsSkipped;
   }

   private static KillEffectsModule module() {
      FuriahsVisuals mod = FuriahsVisuals.instance;
      return mod == null ? null : mod.getKillEffects();
   }

   public static boolean skipPoof(EntityLivingBase entity) {
      if (!failed && entity instanceof EntityPlayer && entity.worldObj != null && entity.worldObj.isRemote) {
         try {
            KillEffectsModule m = module();
            if (m != null && m.skipsPoof(entity)) {
               poofsSkipped++;
               return true;
            } else {
               return false;
            }
         } catch (Throwable var2) {
            fail(var2);
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean hideShadow(Entity entity) {
      if (!failed && entity instanceof EntityPlayer) {
         try {
            KillEffectsModule m = module();
            return m != null && m.isEnabled() && m.hidesPlayer((EntityPlayer)entity);
         } catch (Throwable var2) {
            fail(var2);
            return false;
         }
      } else {
         return false;
      }
   }

   private static void fail(Throwable t) {
      if (!failed) {
         failed = true;
         FuriahsVisuals.LOG.error("Kill Effects' death hooks failed and are switched off for this session", t);
      }
   }
}
