package meowtils.furiahvisuals.module.misc;

import java.lang.reflect.Field;
import meowtils.furiahvisuals.compat.ReflectionHelper;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.mixin.AccessorEntityRenderer;
import meowtils.furiahvisuals.module.ui.UiHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public final class MiscHooks {
   static MiscModule module;
   static float lastTilt = Float.NaN;
   private static boolean real;
   static boolean fullbrightBroken;
   static boolean timeBroken;
   static boolean weatherBroken;
   private static long lastFrame;

   private MiscHooks() {
   }

   private static void broke(String what, Throwable e) {
      FuriahsVisuals.LOG.warn("Misc: {} failed and is off until the game restarts", new Object[]{what, e});
   }

   public static void frame(Minecraft mc, float partialTicks) {
      MiscModule m = module;
      long now = GameClock.nanos();
      float dt = lastFrame == 0L ? 0.0F : Math.min(0.25F, (float)(now - lastFrame) / 1.0E9F);
      lastFrame = now;
      if (m != null) {
         if (!fullbrightBroken) {
            try {
               m.fullbright.frame(dt);
            } catch (RuntimeException var15) {
               fullbrightBroken = true;
               broke("Fullbright", var15);
            }
         }

         boolean was = real;
         real = true;

         try {
            if (!timeBroken) {
               try {
                  m.timeChanger.frame(mc, partialTicks);
               } catch (RuntimeException var14) {
                  timeBroken = true;
                  broke("The Time Changer", var14);
               }
            }

            if (!weatherBroken) {
               try {
                  m.weatherChanger.frame(mc, partialTicks, dt);
               } catch (RuntimeException var13) {
                  weatherBroken = true;
                  broke("The Weather Changer", var13);
               }
            }
         } finally {
            real = was;
         }
      }
   }

   public static void real(Runnable read) {
      boolean was = real;
      real = true;

      try {
         read.run();
      } finally {
         real = was;
      }
   }

   public static void lightmapStart(EntityRenderer r) {
      MiscModule m = module;
      if (m != null && !fullbrightBroken) {
         try {
            AccessorEntityRenderer a = AccessorEntityRenderer.of(r);
            Boolean needed = a == null ? null : a.getFvLightmapUpdateNeeded();
            if (m.fullbright.lightmapStart(needed) && a != null) {
               a.setFvLightmapUpdateNeeded(true);
            }
         } catch (RuntimeException | LinkageError var5) {
            fullbrightBroken = true;
            broke("Fullbright", var5);
         }
      }
   }

   public static float gamma(float value) {
      MiscModule m = module;
      if (m != null && !fullbrightBroken) {
         try {
            return m.fullbright.gammaRead(value);
         } catch (RuntimeException var3) {
            fullbrightBroken = true;
            broke("Fullbright", var3);
            return value;
         }
      } else {
         return value;
      }
   }

   public static void lightmapEnd() {
      MiscModule m = module;
      if (m != null) {
         try {
            m.fullbright.lightmapEnd();
         } catch (RuntimeException var2) {
            fullbrightBroken = true;
            broke("Fullbright", var2);
         }
      }
   }

   public static float celestialAngle(World w) {
      MiscModule m = module;
      if (m != null && !real && !timeBroken) {
         try {
            return m.timeChanger.angle(w);
         } catch (RuntimeException var3) {
            timeBroken = true;
            broke("The Time Changer", var3);
            return Float.NaN;
         }
      } else {
         return Float.NaN;
      }
   }

   public static float rain(World w) {
      MiscModule m = module;
      if (m != null && !real && !weatherBroken) {
         try {
            return m.weatherChanger.rain(w);
         } catch (RuntimeException var3) {
            weatherBroken = true;
            broke("The Weather Changer", var3);
            return Float.NaN;
         }
      } else {
         return Float.NaN;
      }
   }

   public static float thunder(World w) {
      MiscModule m = module;
      if (m != null && !real && !weatherBroken) {
         try {
            return m.weatherChanger.thunder(w);
         } catch (RuntimeException var3) {
            weatherBroken = true;
            broke("The Weather Changer", var3);
            return Float.NaN;
         }
      } else {
         return Float.NaN;
      }
   }

   public static float temperature(BiomeGenBase biome, BlockPos pos) {
      MiscModule m = module;
      if (m != null && !weatherBroken) {
         try {
            return m.weatherChanger.temperature(biome, pos);
         } catch (RuntimeException var4) {
            weatherBroken = true;
            broke("The Weather Changer", var4);
         }
      }

      return biome.getFloatTemperature(pos);
   }

   public static boolean hideArmor(EntityLivingBase entity, int armorSlot) {
      MiscModule m = module;
      return m != null && m.isEnabled() && entity instanceof EntityPlayer && armorSlot >= 1 && armorSlot <= 4
         ? m.hidden(4 - armorSlot, entity == Minecraft.getMinecraft().thePlayer)
         : false;
   }

   public static boolean hideHead(EntityLivingBase entity) {
      MiscModule m = module;
      return m != null && m.heads.get() && hideArmor(entity, 4);
   }

   public static float hurtShake(float tilt) {
      MiscModule m = module;
      float t;
      lastTilt = t = m != null && m.hurtActive() ? tilt * m.hurtShake.getFloat() / 100.0F : tilt;
      return t;
   }

   static boolean armorHooked() {
      return UiHooks.installed("hideArmor", LayerArmorBase.class) && UiHooks.installed("hideHead", LayerCustomHead.class);
   }

   static boolean hurtHooked() {
      return UiHooks.installed("hurtShake", EntityRenderer.class);
   }

   static int lightmapColor(int index) {
      try {
         Field f = ReflectionHelper.findField(EntityRenderer.class, new String[]{"lightmapColors", "lightmapColors"});
         return ((int[])f.get(Minecraft.getMinecraft().entityRenderer))[index];
      } catch (LinkageError | Exception var2) {
         return -1;
      }
   }
}
