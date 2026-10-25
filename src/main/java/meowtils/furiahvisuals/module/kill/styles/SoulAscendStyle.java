package meowtils.furiahvisuals.module.kill.styles;

import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.PoseSnapshot;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Silhouette;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class SoulAscendStyle extends KillStyleBase {
   private final SliderSetting rise;
   private final SliderSetting riseSpeed;
   private final BooleanSetting halo;
   private final Silhouette.Look ghost = new Silhouette.Look();
   private final double[] point = new double[3];

   public SoulAscendStyle(KillEffectsModule module) {
      super(module, KillStyle.SOUL_ASCEND, new ColorSetting("soul.color", "Color", "Color of the soul, its halo and sparkles.", -6297601));
      this.rise = this.setting(
         new SliderSetting("soul.rise", "Rise Height", "How high the soul rises before it dissolves.", 1.5, 5.0, 3.0, 0.25).unit(" blocks")
      );
      this.riseSpeed = this.setting(new SliderSetting("soul.speed", "Rise Speed", "How fast it rises.", 0.5, 2.0, 1.0, 0.05).unit("x"));
      this.halo = this.setting(new BooleanSetting("soul.halo", "Halo", "A thin ring floating over the soul's head.", true));
   }

   private float riseSeconds() {
      return 2.2F / this.riseSpeed.getFloat();
   }

   @Override
   public float seconds(KillFx fx) {
      return 0.3F + this.riseSeconds() + 0.9F;
   }

   @Override
   public void start(KillFx fx) {
      SoulAscendStyle.State s = new SoulAscendStyle.State();
      s.pose.copy(fx.ctx.body.pose);
      s.pose.deathAngle = 0.0F;
      s.pose.limbSwingAmount = 0.0F;
      s.pose.swingProgress = 0.0F;
      s.pose.sneaking = false;
      s.pose.netHeadYaw = 0.0F;
      s.pose.headPitch = -8.0F;
      s.pose.heldItemRight = 0;
      fx.data = s;
   }

   @Override
   public void render(KillFx fx, FxFrame f) {
      SoulAscendStyle.State s = (SoulAscendStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      float up = Easing.inOutSine(Easing.clamp01((t - 0.3F) / this.riseSeconds()));
      double height = Math.min(fx.preview ? 1.6 : 1.0E9, this.rise.getFloat() * fx.size * (fx.crit ? 1.25 : 1.0));
      float leave = Easing.clamp01((t - 0.3F - this.riseSeconds() * 0.85F) / 0.9F);
      fx.look.alpha = 1.0F - Easing.smoothstep(0.35F, 0.3F + this.riseSeconds() * 0.8F, t);
      fx.look.glow = this.color.getColor(0.3F);
      fx.look.glowAmount = 0.35F * (1.0F - Easing.smoothstep(0.2F, 1.2F, t)) * Easing.smoothstep(0.0F, 0.25F, t);
      fx.drawBody(f);
      fx.look.alpha = 1.0F;
      fx.look.glowAmount = 0.0F;
      RenderState.additive();
      double sway = Math.sin(t * 2.1) * 0.12 * up;
      double swayZ = Math.sin(t * 1.6 + 1.0) * 0.07 * up;
      double dy = height * up;
      float emerge = Easing.smoothstep(0.05F, 0.45F, t);
      float ga = a * emerge * (1.0F - Easing.smoothstep(0.6F, 1.0F, leave));
      if (ga > 0.003F) {
         this.ghost.reset();
         this.ghost.color = this.color;
         this.ghost.alpha = ga;
         this.ghost.fill = 0.24F;
         this.ghost.rim = 0.75F;
         this.ghost.inflate = 0.35F;
         this.ghost.rimWidth = 0.9F;
         this.ghost.additive = true;
         this.ghost.shiftX = sway;
         this.ghost.shiftY = dy;
         this.ghost.shiftZ = swayZ;
         this.ghost.dissolve = leave > 0.0F ? leave * 1.1F : -1.0F;
         Silhouette.draw(s.pose, f.viewX, f.viewY, f.viewZ, this.ghost);
         float rate = leave > 0.0F ? 120.0F : 10.0F;
         s.sparkled = s.sparkled + rate * f.dt * (fx.lod ? 0.4F : 1.0F) * fx.intensity;

         while (s.sparkled >= 1.0F) {
            s.sparkled--;
            Silhouette.randomPoint(fx.rng.next(), fx.rng.next(), fx.rng.next(), this.point);
            fx.particles
               .spawn(
                  5,
                  this.point[0],
                  this.point[1],
                  this.point[2],
                  fx.rng.signed() * 0.3F,
                  0.4F + fx.rng.next() * 0.6F,
                  fx.rng.signed() * 0.3F,
                  0.7F + fx.rng.next() * 0.5F,
                  (0.06F + fx.rng.next() * 0.05F) * fx.size
               )
               .drag(0.8F)
               .color(this.color, fx.rng.next())
               .alpha(fx.opacity)
               .flags(2);
         }
      }

      if (this.halo.get() && ga > 0.01F) {
         double hx = f.rx(fx.ctx.feetX + sway);
         double hy = f.ry(fx.ctx.feetY + 2.15 + dy);
         double hz = f.rz(fx.ctx.feetZ + swayZ);
         float ha = ga * Easing.smoothstep(0.25F, 0.7F, t);
         double r = 0.3 * fx.size;
         int c = this.color.getColor(0.9F);
         Vfx.flatRing(Textures.LINE_GLOW, hx, hy, hz, r, 0.16 * fx.size, 48, alpha(c, 0.55F * ha));
         Vfx.flatRing(Textures.LINE_CORE, hx, hy, hz, r, 0.035 * fx.size, 48, core(c, 0.95F * ha));
         Vfx.sprite(Textures.GLOW, hx, hy, hz, 0.9 * fx.size, 0.0F, alpha(ColorUtil.core(c), 0.25F * ha * Math.max(0.3F, fx.glow)));
      }

      float lift;
      if ((lift = a * Easing.smoothstep(0.1F, 0.4F, t) * (1.0F - Easing.smoothstep(0.6F, 1.6F, t))) > 0.01F) {
         Vfx.sprite(
            Textures.GLOW,
            f.rx(fx.ctx.midX()),
            f.ry(fx.ctx.midY() + 0.3),
            f.rz(fx.ctx.midZ()),
            2.2 * fx.size,
            0.0F,
            alpha(this.color.getColor(0.4F), 0.3F * lift * Math.max(0.3F, fx.glow))
         );
      }
   }

   private static final class State {
      final PoseSnapshot pose = new PoseSnapshot();
      float sparkled;

      private State() {
      }
   }
}
