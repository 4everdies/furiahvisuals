package meowtils.furiahvisuals.module.kill.styles;

import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.Rng;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.module.kill.SkinDust;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.DissolveShader;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class DerezzStyle extends KillStyleBase {
   private static final int BANDS = 7;
   private static final float BREAK = 0.45F;
   private static final int MAX_VOXELS = 700;
   private final SliderSetting glitch = this.setting(
      new SliderSetting("derezz.glitch", "Glitch Time", "How long the body glitches before it breaks up.", 0.2, 1.5, 0.7, 0.05).unit(" s")
   );
   private final SliderSetting voxel = this.setting(new SliderSetting("derezz.voxel", "Voxel Size", "How big the cubes are.", 0.5, 2.0, 1.0, 0.05).unit("x"));
   private final SliderSetting floatSpeed = this.setting(
      new SliderSetting("derezz.float", "Float Speed", "How fast the cubes rise.", 0.5, 2.0, 1.0, 0.05).unit("x")
   );

   public DerezzStyle(KillEffectsModule module) {
      super(module, KillStyle.DEREZZ, new ColorSetting("derezz.color", "Color", "Color of the glitch lines and the edge.", -12782849));
   }

   private float glitchTime(KillFx fx) {
      return this.glitch.getFloat();
   }

   @Override
   public float seconds(KillFx fx) {
      return this.glitchTime(fx) + 0.45F + 1.6F;
   }

   @Override
   public void start(final KillFx fx) {
      DerezzStyle.State s = new DerezzStyle.State();
      fx.data = s;
      float v = this.voxel.getFloat();
      int wanted = fx.count(380.0F / (v * v) * (fx.crit ? 1.3F : 1.0F), false);
      s.voxels.sample(fx, Math.min(700, wanted), new SkinDust.Threshold() {
         @Override
         public float at(double x, double y, double z) {
            float h = Easing.clamp01((float)((y - fx.ctx.feetY) / 2.0));
            float noise = DissolveShader.noiseAt(x - fx.ctx.body.pose.x, y - fx.ctx.body.pose.y, z - fx.ctx.body.pose.z);
            return DissolveShader.thresholdAt(h, noise, 0.25F, 0.06F);
         }
      });
   }

   private float breaking(KillFx fx) {
      return Easing.clamp01((fx.t - this.glitchTime(fx)) / 0.45F);
   }

   @Override
   public void update(KillFx fx, float dt) {
      DerezzStyle.State s = (DerezzStyle.State)fx.data;
      SkinDust d = s.voxels;
      float p = this.breaking(fx);
      Rng rng = fx.rng;
      float size = 0.07F * this.voxel.getFloat() * fx.size;

      while (d.next < d.count && d.at[d.next] <= p) {
         int i = d.next++;
         double x = fx.ctx.feetX + d.x[i];
         double y = fx.ctx.feetY + d.y[i];
         double z = fx.ctx.feetZ + d.z[i];
         x = Math.floor(x / size) * size + size * 0.5;
         y = Math.floor(y / size) * size + size * 0.5;
         z = Math.floor(z / size) * size + size * 0.5;
         float up = (0.8F + rng.next() * 0.9F) * this.floatSpeed.getFloat();
         fx.particles
            .spawn(10, x, y, z, rng.signed() * 0.25F, up, rng.signed() * 0.25F, 1.1F + rng.next() * 0.8F, size)
            .end(0.0F)
            .drag(0.6F)
            .color(d.color[i])
            .alpha(fx.opacity)
            .flags(8)
            .spin(0.0F, rng.signed() * 2.0F)
            .axis(0.0F, 1.0F, 0.0F)
            .fade(0.0F, 0.55F);
      }
   }

   @Override
   public void render(KillFx fx, FxFrame f) {
      DerezzStyle.State s = (DerezzStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      float p = this.breaking(fx);
      if (!(p >= 1.0F)) {
         int bucket = (int)(t / 0.055F);
         float strength = Easing.smoothstep(0.0F, 0.12F, t) * (fx.crit ? 1.4F : 1.0F);
         double rx = Vfx.camera().rightX;
         double rz = Vfx.camera().rightZ;
         double rl = Math.sqrt(rx * rx + rz * rz);
         if (rl > 1.0E-4) {
            rx /= rl;
            rz /= rl;
         }

         s.jitter.seed(fx.seed, bucket);
         boolean flicker = s.jitter.next() < 0.18F;
         float bodyA = flicker ? 0.45F : 1.0F;
         double bottom = fx.ctx.feetY - 0.05;
         double height = 0.3;
         boolean shader = DissolveShader.available() && p > 0.0F;

         for (int b = 0; b < 7; b++) {
            double y0 = bottom + b * height;
            double y1 = y0 + height;
            boolean glitched = s.jitter.next() < 0.45F * strength;
            double shove = glitched ? s.jitter.signed() * 0.16 * strength : 0.0;
            BodyProxy.Part[] band = s.bands[b];

            for (int i = 0; i < 6; i++) {
               band[i].copy(fx.rest[i]);
               band[i].x += rx * shove;
               band[i].z += rz * shove;
               band[i].clip = true;
               band[i].cnx = 0.0;
               band[i].cny = 1.0;
               band[i].cnz = 0.0;
               band[i].cd = y0;
               band[i].clip2 = true;
               band[i].c2x = 0.0;
               band[i].c2y = -1.0;
               band[i].c2z = 0.0;
               band[i].c2d = -y1;
               band[i].capColor = 0;
            }

            fx.look.tintAmount = 0.0F;
            fx.look.dissolve = -1.0F;
            fx.look.alpha = bodyA;
            fx.look.glow = this.color.getColor(0.5F);
            float f2 = fx.look.glowAmount = glitched ? 0.35F : 0.08F;
            if (shader) {
               fx.look.dissolve = p;
               fx.look.sweepX = 0.0;
               fx.look.sweepY = 1.0;
               fx.look.sweepZ = 0.0;
               fx.look.sweepFrom = fx.ctx.feetY;
               fx.look.sweepLength = 2.0;
               fx.look.dissolveNoise = 0.25F;
               fx.look.edgeColor = this.color.getColor(0.5F) | 0xFF000000;
               fx.look.edgeWidth = 0.06F;
            } else if (p > 0.0F) {
               fx.look.alpha = bodyA * (1.0F - p);
            }

            fx.drawParts(f, band);
            if (glitched && p <= 0.0F) {
               for (int g = 0; g < 2; g++) {
                  double side = (g == 0 ? 1 : -1) * 0.045 * strength;

                  for (int i = 0; i < 6; i++) {
                     band[i].x += rx * side;
                     band[i].z += rz * side;
                  }

                  fx.look.tint = g == 0 ? -54742 : -13959169;
                  fx.look.tintAmount = 1.0F;
                  fx.look.alpha = 0.5F * bodyA;
                  fx.look.glowAmount = 0.0F;
                  fx.drawParts(f, band);

                  for (int var40 = 0; var40 < 6; var40++) {
                     band[var40].x -= rx * side;
                     band[var40].z -= rz * side;
                  }
               }
            }
         }

         fx.look.tintAmount = 0.0F;
         fx.look.dissolve = -1.0F;
         fx.look.alpha = 1.0F;
         fx.look.glowAmount = 0.0F;
         RenderState.additive();
         if (p <= 0.0F && !fx.lod) {
            for (int k = 0; k < 3; k++) {
               double y = fx.ctx.feetY + (t * 1.3 + k * 0.37) % 1.0 * 2.0;
               double hw = 0.45 * fx.size;
               Vfx.line(
                  Textures.LINE_GLOW,
                  f.rx(fx.ctx.feetX - rx * hw),
                  f.ry(y),
                  f.rz(fx.ctx.feetZ - rz * hw),
                  f.rx(fx.ctx.feetX + rx * hw),
                  f.ry(y),
                  f.rz(fx.ctx.feetZ + rz * hw),
                  0.05 * fx.size,
                  alpha(this.color.getColor(0.5F), 0.55F * a * strength),
                  alpha(this.color.getColor(0.5F), 0.55F * a * strength)
               );
            }
         }
      }
   }

   private static final class State {
      final BodyProxy.Part[][] bands = new BodyProxy.Part[7][];
      final SkinDust voxels = new SkinDust(700);
      final Rng jitter = new Rng();

      State() {
         for (int i = 0; i < 7; i++) {
            this.bands[i] = BodyProxy.newParts();
         }
      }
   }
}
