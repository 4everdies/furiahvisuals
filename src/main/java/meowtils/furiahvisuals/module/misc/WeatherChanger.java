package meowtils.furiahvisuals.module.misc;

import meowtils.furiahvisuals.module.ui.UiHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

final class WeatherChanger {
   private static final float FADE_SECONDS = 2.0F;
   private final MiscModule misc;
   private Boolean hooked;
   private Boolean snowHooked;
   private boolean overriding;
   private float rain;
   private float thunder;
   private boolean snow;

   WeatherChanger(MiscModule misc) {
      this.misc = misc;
   }

   boolean hooked() {
      if (this.hooked == null) {
         this.hooked = UiHooks.installed("rainStrength", World.class) && UiHooks.installed("thunderStrength", World.class);
      }

      return this.hooked;
   }

   boolean snowHooked() {
      if (this.snowHooked == null) {
         this.snowHooked = UiHooks.installed("snowFall", EntityRenderer.class);
      }

      return this.snowHooked;
   }

   void frame(Minecraft mc, float partialTicks, float dt) {
      WorldClient w = mc.theWorld;
      boolean on = this.misc.weatherActive();
      if (w != null && w.provider != null && w.provider.getDimensionId() == 0) {
         float realRain = w.getRainStrength(partialTicks);
         float realThunder = w.getThunderStrength(partialTicks);
         float targetRain = realRain;
         float targetThunder = realThunder;
         boolean targetSnow = false;
         if (on) {
            float s = this.misc.weatherStrength();
            MiscModule.Weather kind = this.misc.weatherKind();
            targetRain = kind == MiscModule.Weather.CLEAR ? 0.0F : s;
            targetThunder = kind == MiscModule.Weather.THUNDERSTORM ? s : 0.0F;
            targetSnow = kind == MiscModule.Weather.SNOW && this.snowHooked();
         }

         if (!this.overriding) {
            if (!on) {
               return;
            }

            this.overriding = true;
            this.rain = realRain;
            this.thunder = realThunder;
            this.snow = false;
         }

         if (!this.misc.weatherSmooth()) {
            this.rain = targetRain;
            this.thunder = targetThunder;
            this.snow = targetSnow;
         } else if (dt > 0.0F) {
            float step = dt / 2.0F;
            if (this.snow != targetSnow && this.rain > 0.0F) {
               this.rain = Math.max(0.0F, this.rain - step);
            } else {
               this.snow = targetSnow;
               this.rain = toward(this.rain, targetRain, step);
            }

            this.thunder = toward(this.thunder, targetThunder, step);
         }

         if (!on && !this.snow && Math.abs(this.rain - realRain) < 0.002F && Math.abs(this.thunder - realThunder) < 0.002F) {
            this.overriding = false;
         }
      } else {
         this.overriding = false;
         this.snow = false;
      }
   }

   private static float toward(float v, float target, float step) {
      return v < target ? Math.min(target, v + step) : Math.max(target, v - step);
   }

   float rain(World w) {
      return this.overriding && w == Minecraft.getMinecraft().theWorld ? this.rain : Float.NaN;
   }

   float thunder(World w) {
      return this.overriding && w == Minecraft.getMinecraft().theWorld ? Math.min(this.thunder, this.rain) : Float.NaN;
   }

   float temperature(BiomeGenBase biome, BlockPos pos) {
      return this.overriding && this.snow ? 0.0F : biome.getFloatTemperature(pos);
   }

   float[] stateForTest() {
      return new float[]{this.overriding ? 1.0F : 0.0F, this.rain, this.thunder, this.snow ? 1.0F : 0.0F};
   }
}
