package meowtils.furiahvisuals.module.kill.styles;

import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.physics.Ragdoll;
import meowtils.furiahvisuals.fx.physics.RigidBox;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class RagdollStyle extends KillStyleBase {
   private final SliderSetting fling = this.setting(
      new SliderSetting("ragdoll.fling", "Fling Strength", "How hard the body is thrown.", 0.2, 3.0, 1.0, 0.05).unit("x")
   );
   private final SliderSetting stiffness = this.setting(
      new SliderSetting("ragdoll.stiffness", "Stiffness", "Limp (0) to stiff (1): how much the body keeps the pose it died in.", 0.0, 1.0, 0.15, 0.05)
   );
   private final SliderSetting bounce = this.setting(
      new SliderSetting("ragdoll.bounce", "Bounciness", "How much it bounces off the ground and walls.", 0.0, 0.8, 0.3, 0.05)
   );
   private final SliderSetting gravity = this.setting(
      new SliderSetting("ragdoll.gravity", "Gravity", "How heavy the body falls.", 0.4, 2.0, 1.0, 0.05).unit("x")
   );

   public RagdollStyle(KillEffectsModule module) {
      super(module, KillStyle.RAGDOLL, new ColorSetting("ragdoll.color", "Color", "Color of the impact sparks and the flash.", -45206));
   }

   @Override
   public float seconds(KillFx fx) {
      return 2.4F;
   }

   @Override
   public boolean leavesBody() {
      return true;
   }

   @Override
   public void start(KillFx fx) {
      Ragdoll r = fx.ragdoll();
      r.gravity = 26.0F * this.gravity.getFloat();
      r.stiffness = this.stiffness.getFloat();

      for (RigidBox b : r.parts) {
         b.restitution = this.bounce.getFloat();
         b.friction = 0.55F;
      }

      double dx = fx.ctx.dirX;
      double dy = fx.ctx.dirY;
      double dz = fx.ctx.dirZ;
      if (dy < -0.45) {
         double h = Math.sqrt(dx * dx + dz * dz);
         double k = h < 1.0E-4 ? 0.0 : Math.sqrt(0.7975) / h;
         dx *= k;
         dz *= k;
         dy = -0.45;
      }

      double strength = 6.5 * this.fling.getFloat() * (fx.crit ? 1.55 : 1.0) * Math.sqrt(fx.size) * (fx.preview ? 0.35 : 1.0);
      r.fling(
         fx.ctx.impactX,
         fx.ctx.impactY,
         fx.ctx.impactZ,
         fx.ctx.body.vx * 0.6,
         fx.ctx.body.vy * 0.3,
         fx.ctx.body.vz * 0.6,
         dx,
         dy,
         dz,
         strength,
         5.0 * this.fling.getFloat() * (fx.crit ? 1.4 : 1.0)
      );
      if (!fx.lod) {
         fx.burst(fx.ctx.impactX, fx.ctx.impactY, fx.ctx.impactZ, dx, dy, dz, fx.count(fx.crit ? 26.0F : 16.0F, false), 5.5F, 0.55F);
      }
   }

   @Override
   public void render(KillFx fx, FxFrame f) {
      float flash = 1.0F - Easing.smoothstep(0.0F, 0.18F, fx.t);
      fx.look.glow = fx.color(0.5F);
      fx.look.glowAmount = flash * 0.85F * Math.min(1.0F, 0.4F + fx.glow);
      fx.drawBody(f);
   }
}
