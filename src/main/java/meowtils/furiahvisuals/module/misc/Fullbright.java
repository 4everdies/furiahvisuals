package meowtils.furiahvisuals.module.misc;

import meowtils.furiahvisuals.fx.look.LightBoost;
import meowtils.furiahvisuals.module.ui.UiHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;

final class Fullbright {
   static final float FULL_GAMMA = 12.0F;
   private static final float FADE_SECONDS = 0.25F;
   private final MiscModule misc;
   private float fade;
   private float built = Float.NaN;
   private boolean around;
   private boolean endSeen;
   private Boolean hooked;
   private boolean rebuilding;
   private boolean known;
   private boolean redirected;
   private boolean raised;
   private float saved;
   private int rebuilds;
   private int raisedRebuilds;

   Fullbright(MiscModule misc) {
      this.misc = misc;
   }

   boolean hooked() {
      if (this.hooked == null) {
         boolean redirect = UiHooks.installed("gamma", EntityRenderer.class);
         boolean startEnd = UiHooks.installed("lightmapStart", EntityRenderer.class) && UiHooks.installed("lightmapEnd", EntityRenderer.class);
         this.around |= !redirect;
         this.hooked = redirect || startEnd;
      }

      return this.hooked;
   }

   void frame(float dt) {
      if (this.raised) {
         Minecraft.getMinecraft().gameSettings.gammaSetting = this.saved;
         this.raised = false;
         this.endSeen = false;
         this.around = false;
      }

      float target = this.misc.fullbrightActive() ? 1.0F : 0.0F;
      if (this.fade != target) {
         float step = dt / 0.25F;
         if (dt > 0.0F) {
            this.fade = this.fade < target ? Math.min(target, this.fade + step) : Math.max(target, this.fade - step);
         } else if (Minecraft.getMinecraft().theWorld == null) {
            this.fade = target;
         }
      }

      LightBoost.set(this.amount());
   }

   float amount() {
      return this.fade <= 0.0F ? 0.0F : this.fade * this.misc.fullbrightStrength();
   }

   float gamma(float real) {
      float a = this.amount();
      return a <= 0.0F ? real : real + (12.0F - real) * a;
   }

   boolean lightmapStart(Boolean needed) {
      float a = this.amount();
      boolean force = this.built != a;
      this.known = needed != null;
      this.rebuilding = !this.known || needed || force;
      this.redirected = false;
      if (this.rebuilding && a > 0.0F && this.around && this.endSeen) {
         GameSettings gs = Minecraft.getMinecraft().gameSettings;
         this.saved = gs.gammaSetting;
         this.raised = true;
         gs.gammaSetting = this.gamma(this.saved);
      }

      return force && this.known && !needed;
   }

   float gammaRead(float value) {
      this.redirected = true;
      return this.raised ? value : this.gamma(value);
   }

   void lightmapEnd() {
      this.endSeen = true;
      boolean wasRaised = this.raised;
      if (this.raised) {
         Minecraft.getMinecraft().gameSettings.gammaSetting = this.saved;
         this.raised = false;
      }

      if (this.rebuilding) {
         this.rebuilding = false;
         if (Minecraft.getMinecraft().theWorld != null) {
            this.rebuilds++;
            if (wasRaised) {
               this.raisedRebuilds++;
            }

            float a;
            if ((a = this.amount()) > 0.0F && this.known && !this.around && !this.redirected) {
               this.around = true;
               this.built = Float.NaN;
            } else {
               this.built = a;
            }
         }
      }
   }

   float[] stateForTest() {
      return new float[]{this.fade, this.amount(), this.gamma(0.5F), this.rebuilds, this.raisedRebuilds, this.around ? 1.0F : 0.0F};
   }

   void aroundForTest(boolean on) {
      this.around = on || !UiHooks.installed("gamma", EntityRenderer.class);
      this.built = Float.NaN;
   }
}
