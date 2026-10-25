package meowtils.furiahvisuals.hud;

import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.render.Easing;
import net.minecraft.client.Minecraft;

public final class HudGlint {
   public static HudGlint.Sync sync = HudGlint.Sync.TOGETHER;
   private static final float WAVE_SLOWER = 2.2F;
   public static float progressOverride = Float.NaN;

   private HudGlint() {
   }

   public static boolean shine(float cx, float cy, float w, float h, int accent, int global, boolean light, HudGlint.Shine out) {
      return shine(cx, cy, w, h, accent, global, light, 1.0F, out);
   }

   public static boolean shine(float cx, float cy, float w, float h, int accent, int global, boolean light, float widthScale, HudGlint.Shine out) {
      WaterMarkModule wm = FuriahsVisuals.instance == null ? null : FuriahsVisuals.instance.getWaterMark();
      if (wm != null && wm.glintOn()) {
         Minecraft mc = Minecraft.getMinecraft();
         float k = mc.displayHeight / 1080.0F;
         boolean wave = sync == HudGlint.Sync.WAVE;
         float sweep = wm.glintSweep() * (wave ? 2.2F : 1.0F);
         float period = Math.max(wm.glintEvery(), sweep + 0.25F);
         double now = ClientClock.seconds();
         long cycle = (long)Math.floor(now / period);
         float t = (float)(now - (double)cycle * period) / sweep;
         if (!Float.isNaN(progressOverride)) {
            t = progressOverride;
            cycle = 0L;
         }

         if (!(t <= 0.0F) && !(t >= 1.0F)) {
            WaterMarkModule.GlintDirection dir = wm.glintDirection();
            boolean reverse = dir == WaterMarkModule.GlintDirection.RIGHT_TO_LEFT || dir == WaterMarkModule.GlintDirection.BACK_AND_FORTH && (cycle & 1L) == 1L;
            float progress = Easing.inOutSine(t);
            if (reverse) {
               progress = 1.0F - progress;
            }

            float angle = (float)Math.toRadians(wm.glintTilt());
            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);
            float half = wm.glintWidth() / 2.0F * k * widthScale;
            float band;
            if (wave) {
               float sw = mc.displayWidth;
               float sh = mc.displayHeight;
               float a0 = Math.min(Math.min(0.0F, sw * cos), Math.min(sh * sin, sw * cos + sh * sin));
               float pos = a0 - half * 3.2F + (Math.max(Math.max(0.0F, sw * cos), Math.max(sh * sin, sw * cos + sh * sin)) - a0 + half * 6.4F) * progress;
               band = pos - (cx * cos + cy * sin);
               float reach;
               if (band < -(reach = w / 2.0F * Math.abs(cos) + h / 2.0F * Math.abs(sin) + half * 3.2F) || band > reach) {
                  return false;
               }
            } else {
               float reach = w / 2.0F * Math.abs(cos) + h / 2.0F * Math.abs(sin) + half * 3.2F;
               band = -reach + 2.0F * reach * progress;
            }

            int color;
            switch (wm.glintColor()) {
               case WHITE:
                  color = -1;
                  break;
               case ACCENT:
                  color = accent;
                  break;
               case GLOBAL:
                  color = global;
                  break;
               default:
                  color = light ? accent : -1;
            }

            float strength = wm.glintStrength();
            if (FuriahsVisuals.instance.isReduceFlashing()) {
               strength *= 0.5F;
            }

            out.band = band;
            out.halfWidth = half;
            out.angle = angle;
            out.trail = reverse ? -1.0F : 1.0F;
            out.color = Math.round(Math.max(0.0F, Math.min(1.0F, strength)) * 255.0F) << 24 | color & 16777215;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static final class Shine {
      public float band;
      public float halfWidth;
      public float angle;
      public float trail;
      public int color;
   }

   public static enum Sync {
      TOGETHER,
      WAVE;
   }
}
