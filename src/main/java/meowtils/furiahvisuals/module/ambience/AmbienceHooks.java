package meowtils.furiahvisuals.module.ambience;

import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.sky.SkyModule;
import net.minecraft.util.Vec3;

public final class AmbienceHooks {
   private static boolean failed;

   private AmbienceHooks() {
   }

   private static AmbienceModule module() {
      FuriahsVisuals mod = FuriahsVisuals.instance;
      return mod == null ? null : mod.getAmbience();
   }

   public static Vec3 skyColor(Vec3 sky) {
      if (failed) {
         return sky;
      } else {
         try {
            AmbienceModule m = module();
            return m == null ? sky : m.tintSky(sky);
         } catch (Throwable var2) {
            fail(var2);
            return sky;
         }
      }
   }

   public static Vec3 cloudColor(Vec3 clouds) {
      if (failed) {
         return clouds;
      } else {
         try {
            FuriahsVisuals mod = FuriahsVisuals.instance;
            SkyModule sky = mod == null ? null : mod.getSky();
            Vec3 c = sky == null ? clouds : sky.tintClouds(clouds);
            AmbienceModule m = module();
            return m == null ? c : m.tintClouds(c);
         } catch (Throwable var5) {
            fail(var5);
            return clouds;
         }
      }
   }

   public static void afterSky(float partialTicks) {
      if (!failed) {
         try {
            AmbienceModule m = module();
            if (m != null) {
               m.renderSkyPass(partialTicks);
            }
         } catch (Throwable var2) {
            fail(var2);
         }
      }
   }

   private static void fail(Throwable t) {
      if (!failed) {
         failed = true;
         FuriahsVisuals.LOG.error("Ambience's sky hooks failed and are switched off for this session", t);
      }
   }
}
