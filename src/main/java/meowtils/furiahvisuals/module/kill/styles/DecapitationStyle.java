package meowtils.furiahvisuals.module.kill.styles;

import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.physics.Ragdoll;
import meowtils.furiahvisuals.fx.physics.RigidBox;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class DecapitationStyle extends KillStyleBase {
   private static final float BEAT = 0.45F;
   private final SliderSetting launch;
   private final SliderSetting spin;
   private final SliderSetting burstSize;
   private final ModeSetting<DecapitationStyle.Body> body;
   private final double[] neck = new double[3];

   public DecapitationStyle(KillEffectsModule module) {
      super(module, KillStyle.DECAPITATION, new ColorSetting("decap.color", "Color", "Color of the burst and the neck.", -50340));
      this.launch = this.setting(new SliderSetting("decap.launch", "Launch Strength", "How hard the head flies off.", 0.3, 2.0, 1.0, 0.05).unit("x"));
      this.spin = this.setting(new SliderSetting("decap.spin", "Spin", "How fast the head spins.", 0.0, 2.0, 1.0, 0.05).unit("x"));
      this.body = this.setting(
         new ModeSetting<>("decap.body", "Body", "What the body does after a beat.", DecapitationStyle.Body.class, DecapitationStyle.Body.COLLAPSE)
      );
      this.burstSize = this.setting(new SliderSetting("decap.burst", "Burst Size", "How much comes out of the neck.", 0.0, 2.0, 1.0, 0.05).unit("x"));
   }

   @Override
   public float seconds(KillFx fx) {
      return 2.6F;
   }

   @Override
   public boolean leavesBody() {
      return true;
   }

   @Override
   public void start(KillFx fx) {
      fx.data = new DecapitationStyle.State();
      Ragdoll r = fx.ragdoll();
      r.detach(0);
      r.hold();
      RigidBox head = r.parts[0];
      head.restitution = 0.42F;
      head.friction = 0.3F;
      double dx = fx.ctx.dirX;
      double dz = fx.ctx.dirZ;
      double hl = Math.sqrt(dx * dx + dz * dz);
      if (hl < 1.0E-4) {
         dx = 0.0;
         dz = 1.0;
         hl = 1.0;
      }

      double speed = 4.2 * this.launch.getFloat() * (fx.crit ? 1.35 : 1.0) * Math.sqrt(fx.size) * (fx.preview ? 0.4 : 1.0);
      double up = 3.6 * this.launch.getFloat() * (fx.crit ? 1.2 : 1.0) + Math.max(0.0, -fx.ctx.dirY) * 1.5;
      double w = 11.0 * this.spin.getFloat();
      double var16;
      double var17;
      head.setVelocity(
         (var16 = dx / hl) * speed + fx.ctx.body.vx * 0.5,
         up,
         (var17 = dz / hl) * speed + fx.ctx.body.vz * 0.5,
         var17 * w + fx.rng.signed() * 2.0F,
         fx.rng.signed() * 3.0F,
         -var16 * w + fx.rng.signed() * 2.0F,
         0.008333333333333333
      );
      this.neckBurst(fx, 1.0F);
   }

   private void neckPoint(KillFx fx) {
      BodyProxy.Part b = fx.parts[1];
      double k = 0.3515625;
      this.neck[0] = b.x - b.r[1] * k;
      this.neck[1] = b.y - b.r[4] * k;
      this.neck[2] = b.z - b.r[7] * k;
   }

   private void neckBurst(KillFx fx, float amount) {
      if (!(this.burstSize.getFloat() <= 0.0F) && (!fx.lod || !(amount < 1.0F))) {
         fx.applyRagdoll();
         this.neckPoint(fx);
         BodyProxy.Part b = fx.parts[1];
         double ux = -b.r[1];
         double uy = -b.r[4];
         double uz = -b.r[7];
         int n = fx.count(22.0F * this.burstSize.getFloat() * amount, true);
         fx.burst(this.neck[0], this.neck[1], this.neck[2], ux + fx.ctx.dirX * 0.4, uy, uz + fx.ctx.dirZ * 0.4, n, 3.8F * (0.6F + 0.4F * amount), 0.45F);
      }
   }

   @Override
   public void update(KillFx fx, float dt) {
      DecapitationStyle.State s = (DecapitationStyle.State)fx.data;
      Ragdoll r = fx.ragdoll();
      if (s.spurts < 3 && fx.t >= s.nextSpurt) {
         s.spurts++;
         s.nextSpurt += 0.11F;
         this.neckBurst(fx, 0.35F);
      }

      if (!s.let && fx.t >= 0.45F && this.body.get() != DecapitationStyle.Body.STAY) {
         s.let = true;
         if (this.body.get() == DecapitationStyle.Body.FALL_OVER) {
            r.stiffness = 1.0F;
            r.wake();
            BodyProxy.Part b = fx.parts[1];
            r.fling(b.x, b.y + 0.4, b.z, 0.0, 0.0, 0.0, fx.ctx.dirX, 0.0, fx.ctx.dirZ, 0.6, 2.2);
         } else {
            r.stiffness = 0.0F;
            r.wake();
            BodyProxy.Part b = fx.parts[1];
            r.fling(b.x, b.y, b.z, 0.0, 0.0, 0.0, fx.ctx.dirX * 0.4, -1.0, fx.ctx.dirZ * 0.4, 1.2, 1.2);
         }
      }
   }

   @Override
   public void render(KillFx fx, FxFrame f) {
      fx.look.glowAmount = 0.0F;
      fx.drawBody(f);
      float a = fx.alpha() * (1.0F - fx.endPhase());
      if (!(a <= 0.01F)) {
         this.neckPoint(fx);
         BodyProxy.Part b = fx.parts[1];
         double hx = 0.234375;
         double hz = 0.1171875;
         double rx = b.r[0] * hx;
         double ry = b.r[3] * hx;
         double rz = b.r[6] * hx;
         double fx2 = b.r[2] * hz;
         double fy2 = b.r[5] * hz;
         double fz2 = b.r[8] * hz;
         double ox = -b.r[1] * 0.004;
         double oy = -b.r[4] * 0.004;
         double oz = -b.r[7] * 0.004;
         double cx = f.rx(this.neck[0]) + ox;
         double cy = f.ry(this.neck[1]) + oy;
         double cz = f.rz(this.neck[2]) + oz;
         if (fx.classicRed) {
            RenderState.translucent(false);
            int c = alpha(-9565429, a);
            Vfx.quad(
               cx - rx - fx2,
               cy - ry - fy2,
               cz - rz - fz2,
               cx + rx - fx2,
               cy + ry - fy2,
               cz + rz - fz2,
               cx + rx + fx2,
               cy + ry + fy2,
               cz + rz + fz2,
               cx - rx + fx2,
               cy - ry + fy2,
               cz - rz + fz2,
               c,
               c,
               c,
               c
            );
            Vfx.flush();
            RenderState.additive();
         } else {
            int c = alpha(this.color.getColor(0.5F), a * 0.9F);
            int hot = core(this.color.getColor(0.5F), a);
            Vfx.quadUV(
               Textures.GLOW,
               cx - rx - fx2,
               cy - ry - fy2,
               cz - rz - fz2,
               0.0,
               0.0,
               c,
               cx + rx - fx2,
               cy + ry - fy2,
               cz + rz - fz2,
               1.0,
               0.0,
               c,
               cx + rx + fx2,
               cy + ry + fy2,
               cz + rz + fz2,
               1.0,
               1.0,
               hot,
               cx - rx + fx2,
               cy - ry + fy2,
               cz - rz + fz2,
               0.0,
               1.0,
               c
            );
            float glow = Math.max(0.3F, fx.glow) * (1.0F - Easing.smoothstep(0.3F, 1.4F, fx.t));
            Vfx.sprite(Textures.GLOW, cx, cy, cz, 0.7 * fx.size, 0.0F, alpha(ColorUtil.core(this.color.getColor(0.5F)), 0.5F * a * glow));
         }
      }
   }

   static enum Body implements Displayable {
      COLLAPSE("Collapse", "Crumples to the ground"),
      FALL_OVER("Fall Over", "Topples like a plank"),
      STAY("Stay Standing", "Stays on its feet");

      private final String displayName;
      private final String description;

      private Body(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   private static final class State {
      boolean let;
      boolean spurted;
      float nextSpurt = 0.08F;
      int spurts;

      private State() {
      }
   }
}
