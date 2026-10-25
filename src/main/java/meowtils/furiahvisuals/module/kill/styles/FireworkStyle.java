package meowtils.furiahvisuals.module.kill.styles;

import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.Rng;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.module.kill.SkinDust;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class FireworkStyle extends KillStyleBase {
   private static final float LAUNCH = 0.85F;
   private final SliderSetting height;
   private final SliderSetting burstSize;
   private final ModeSetting<FireworkStyle.Shape> shape;
   private final BooleanSetting trail;
   private final double[] flat = new double[2];

   public FireworkStyle(KillEffectsModule module) {
      super(module, KillStyle.FIREWORK, new ColorSetting("firework.color", "Color", "Colors of the burst (a gradient spreads over it).", -41054));
      this.height = this.setting(
         new SliderSetting("firework.height", "Height", "How high the body flies before it bursts.", 2.0, 20.0, 3.0, 0.5).unit(" blocks")
      );
      this.burstSize = this.setting(new SliderSetting("firework.size", "Burst Size", "How big the burst is.", 0.5, 2.0, 1.0, 0.05).unit("x"));
      this.shape = this.setting(
         new ModeSetting<>("firework.shape", "Burst Shape", "The shape of the burst.", FireworkStyle.Shape.class, FireworkStyle.Shape.SPHERE)
            .style(ModeSetting.Style.CHIPS)
      );
      this.trail = this.setting(new BooleanSetting("firework.trail", "Trails", "Every spark leaves a streak of light.", true));
   }

   @Override
   public float seconds(KillFx fx) {
      return 2.65F;
   }

   @Override
   public void start(KillFx fx) {
      FireworkStyle.State s = new FireworkStyle.State();
      fx.data = s;
      s.skin.sample(fx, 36, null);
   }

   private double rise(KillFx fx) {
      float u = Easing.clamp01(fx.t / 0.85F);
      return (fx.preview ? 1.4 : this.height.getFloat()) * u * u * (fx.crit ? 1.15 : 1.0);
   }

   @Override
   public void update(KillFx fx, float dt) {
      FireworkStyle.State s = (FireworkStyle.State)fx.data;
      Rng rng = fx.rng;
      double y = fx.ctx.feetY + this.rise(fx);
      if (!s.burst && fx.t < 0.85F) {
         s.trailed = s.trailed + dt * 140.0F * (fx.lod ? 0.4F : 1.0F);

         while (s.trailed >= 1.0F) {
            s.trailed--;
            fx.particles
               .spawn(
                  rng.next() < 0.3F ? 5 : 1,
                  fx.ctx.feetX + rng.signed() * 0.15,
                  y,
                  fx.ctx.feetZ + rng.signed() * 0.15,
                  rng.signed() * 0.8F,
                  -1.5F - rng.next() * 2.0F,
                  rng.signed() * 0.8F,
                  0.4F + rng.next() * 0.5F,
                  (0.05F + rng.next() * 0.04F) * fx.size
               )
               .gravity(3.0F)
               .drag(1.5F)
               .color(ColorUtil.lerp(-5720, this.color.getColor(rng.next()), rng.next() * 0.6F))
               .alpha(fx.opacity)
               .hot(0.9F)
               .flags(2);
         }
      }

      if (!s.burst && fx.t >= 0.85F) {
         s.burst = true;
         this.burst(fx, s, fx.ctx.feetX, fx.ctx.feetY + this.rise(fx) + 1.0, fx.ctx.feetZ);
      }
   }

   private void burst(KillFx fx, FireworkStyle.State s, double x, double y, double z) {
      Rng rng = fx.rng;
      float k = this.burstSize.getFloat() * fx.size * (fx.crit ? 1.3F : 1.0F) * (fx.preview ? 0.4F : 1.0F);
      int n = fx.count(170.0F * this.burstSize.getFloat(), true);
      double rx = Vfx.camera().rightX;
      double ry = Vfx.camera().rightY;
      double rz = Vfx.camera().rightZ;
      double ux = Vfx.camera().upX;
      double uy = Vfx.camera().upY;
      double uz = Vfx.camera().upZ;
      boolean trails = this.trail.get() && !fx.lod;

      for (int i = 0; i < n; i++) {
         float u = (float)i / n;
         FireworkStyle.Shape sh = this.shape.get();
         double dz;
         double dy;
         double dx;
         if (sh == FireworkStyle.Shape.SPHERE) {
            double yy = 1.0 - 2.0 * (i + 0.5) / n;
            double r = Math.sqrt(1.0 - yy * yy);
            double a2 = i * 2.39996;
            dx = Math.cos(a2) * r;
            dy = yy;
            dz = Math.sin(a2) * r;
         } else {
            double[] p = this.flat(sh, u, rng);
            dx = rx * p[0] + ux * p[1];
            dy = ry * p[0] + uy * p[1];
            dz = rz * p[0] + uz * p[1];
            double fx2 = uy * rz - uz * ry;
            double fy2 = uz * rx - ux * rz;
            double fz2 = ux * ry - uy * rx;
            double d = rng.signed() * 0.12;
            dx += fx2 * d;
            dy += fy2 * d;
            dz += fz2 * d;
         }

         float speed = 9.0F * k * (0.92F + rng.next() * 0.16F);
         int kind = trails ? 2 : 1;
         fx.particles
            .spawn(kind, x, y, z, (float)dx * speed, (float)dy * speed, (float)dz * speed, 1.3F + rng.next() * 0.5F, (trails ? 0.06F : 0.09F) * fx.size)
            .gravity(2.2F)
            .drag(2.3F)
            .color(this.color, u)
            .alpha(fx.opacity)
            .hot(0.8F)
            .fade(0.0F, 0.6F)
            .flags(2);
      }

      for (int var49 = 0; var49 < s.skin.count && !fx.lod; var49++) {
         double a = rng.next() * 6.2832;
         double e = rng.signed() * 1.4;
         float sp = (3.0F + rng.next() * 3.0F) * k;
         fx.particles
            .spawn(
               10,
               x,
               y,
               z,
               (float)(Math.cos(a) * Math.cos(e)) * sp,
               (float)Math.sin(e) * sp,
               (float)(Math.sin(a) * Math.cos(e)) * sp,
               1.0F + rng.next() * 0.6F,
               0.07F * fx.size
            )
            .gravity(6.0F)
            .drag(1.4F)
            .color(s.skin.color[var49])
            .alpha(fx.opacity)
            .hot(0.2F)
            .spin(rng.next() * 6.0F, rng.signed() * 8.0F)
            .axis(rng.signed(), rng.signed(), rng.signed())
            .end(0.02F);
      }

      for (int var50 = 0; var50 < fx.count(24.0F, false) && !fx.lod; var50++) {
         double a = rng.next() * 6.2832;
         double e = rng.signed() * 1.4;
         double r = (2.0F + rng.next() * 2.5F) * k * 0.55;
         fx.particles
            .spawn(5, x + Math.cos(a) * Math.cos(e) * r, y + Math.sin(e) * r, z + Math.sin(a) * Math.cos(e) * r, 0.0F, -0.3F, 0.0F, 0.35F, 0.12F * fx.size)
            .delay(0.45F + rng.next() * 0.6F)
            .color(-3128)
            .alpha(fx.opacity)
            .flags(2);
      }
   }

   private double[] flat(FireworkStyle.Shape sh, float u, Rng rng) {
      double t = u * Math.PI * 2.0;
      switch (sh) {
         case RING:
            this.flat[0] = Math.cos(t);
            this.flat[1] = Math.sin(t);
            break;
         case STAR:
            double seg = u * 10.0F;
            int i = (int)seg;
            double f = seg - i;
            double a0 = (Math.PI / 2) + i * Math.PI / 5.0;
            double a1 = (Math.PI / 2) + (i + 1) * Math.PI / 5.0;
            double r0 = i % 2 == 0 ? 1.0 : 0.42;
            double r1 = i % 2 == 0 ? 0.42 : 1.0;
            this.flat[0] = Math.cos(a0) * r0 + (Math.cos(a1) * r1 - Math.cos(a0) * r0) * f;
            this.flat[1] = Math.sin(a0) * r0 + (Math.sin(a1) * r1 - Math.sin(a0) * r0) * f;
            break;
         case HEART:
            double hx = 16.0 * Math.pow(Math.sin(t), 3.0);
            double hy = 13.0 * Math.cos(t) - 5.0 * Math.cos(2.0 * t) - 2.0 * Math.cos(3.0 * t) - Math.cos(4.0 * t);
            this.flat[0] = hx / 17.0;
            this.flat[1] = hy / 17.0 + 0.1;
            break;
         case SKULL:
            if (u < 0.5F) {
               double a = Math.PI * (-0.2 + 1.4 * (u / 0.5));
               this.flat[0] = Math.cos(a) * 0.95;
               this.flat[1] = Math.sin(a) * 0.95 + 0.15;
            } else if (u < 0.66F) {
               double v = (u - 0.5F) / 0.16F;
               double[] jaw = new double[]{-0.62, -0.45, -0.42, -0.95, 0.42, -0.95, 0.62, -0.45};
               int shapeSegment = Math.min(2, (int)(v * 3.0));
               double jawFraction = v * 3.0 - shapeSegment;
               this.flat[0] = jaw[shapeSegment * 2] + (jaw[shapeSegment * 2 + 2] - jaw[shapeSegment * 2]) * jawFraction;
               this.flat[1] = jaw[shapeSegment * 2 + 1] + (jaw[shapeSegment * 2 + 3] - jaw[shapeSegment * 2 + 1]) * jawFraction;
            } else if (u < 0.9F) {
               double v = (u - 0.66F) / 0.24F;
               boolean left = v < 0.5;
               double a = (left ? v * 2.0 : v * 2.0 - 1.0) * Math.PI * 2.0;
               this.flat[0] = (left ? -0.36 : 0.36) + Math.cos(a) * 0.22;
               this.flat[1] = 0.05 + Math.sin(a) * 0.24;
            } else {
               double v = (u - 0.9F) / 0.1F;
               double[] nose = new double[]{0.0, -0.18, -0.12, -0.42, 0.12, -0.42, 0.0, -0.18};
               int shapeSegment = Math.min(2, (int)(v * 3.0));
               double noseFraction = v * 3.0 - shapeSegment;
               this.flat[0] = nose[shapeSegment * 2] + (nose[shapeSegment * 2 + 2] - nose[shapeSegment * 2]) * noseFraction;
               this.flat[1] = nose[shapeSegment * 2 + 1] + (nose[shapeSegment * 2 + 3] - nose[shapeSegment * 2 + 1]) * noseFraction;
            }
            break;
         default:
            this.flat[0] = Math.cos(t);
            this.flat[1] = Math.sin(t);
      }

      return this.flat;
   }

   @Override
   public void render(KillFx fx, FxFrame f) {
      FireworkStyle.State s = (FireworkStyle.State)fx.data;
      float a = fx.alpha();
      if (s.burst) {
         float ft = (fx.t - 0.85F) / 0.4F;
         if (ft < 1.0F) {
            double y = f.ry(fx.ctx.feetY + this.rise(fx) + 1.0);
            float fl = 1.0F - ft;
            Vfx.sprite(
               Textures.GLOW,
               f.rx(fx.ctx.feetX),
               y,
               f.rz(fx.ctx.feetZ),
               6.0 * this.burstSize.getFloat() * fx.size,
               0.0F,
               alpha(this.color.getColor(0.5F), 0.7F * a * fl * fl * Math.max(0.4F, fx.glow))
            );
            Vfx.sprite(Textures.FLARE, f.rx(fx.ctx.feetX), y, f.rz(fx.ctx.feetZ), 2.5 * fx.size, ft, core(this.color.getColor(0.5F), a * fl));
         }
      } else {
         double lift = this.rise(fx);
         float turn = fx.t * 4.0F;
         double c = Math.cos(turn);
         double sn = Math.sin(turn);

         for (int i = 0; i < 6; i++) {
            BodyProxy.Part p = fx.parts[i];
            BodyProxy.Part r = fx.rest[i];
            p.copy(r);
            double ox = r.x - fx.ctx.feetX;
            double oz = r.z - fx.ctx.feetZ;
            p.x = fx.ctx.feetX + ox * c - oz * sn;
            p.z = fx.ctx.feetZ + ox * sn + oz * c;
            p.y = r.y + lift;

            for (int row = 0; row < 3; row++) {
               double r0 = r.r[row];
               double r2 = r.r[6 + row];
               p.r[row] = r0 * c - r2 * sn;
               p.r[6 + row] = r0 * sn + r2 * c;
            }
         }

         fx.look.glow = this.color.getColor(0.5F);
         fx.look.glowAmount = 0.6F * Easing.smoothstep(0.2F, 0.85F, fx.t);
         fx.drawBody(f);
         fx.look.glowAmount = 0.0F;
         double y = f.ry(fx.ctx.feetY + lift);
         Vfx.sprite(Textures.GLOW, f.rx(fx.ctx.feetX), y, f.rz(fx.ctx.feetZ), 1.1 * fx.size, 0.0F, alpha(-11654, 0.7F * a * Math.max(0.4F, fx.glow)));
         Vfx.sprite(Textures.SPARK, f.rx(fx.ctx.feetX), y, f.rz(fx.ctx.feetZ), 0.5 * fx.size, 0.0F, alpha(-1, 0.9F * a));
      }
   }

   static enum Shape implements Displayable {
      SPHERE("Sphere", "A round burst"),
      RING("Ring", "A ring facing you"),
      STAR("Star", "A five-pointed star"),
      HEART("Heart", "A heart"),
      SKULL("Skull", "A skull");

      private final String displayName;
      private final String description;

      private Shape(String displayName, String description) {
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
      boolean burst;
      float trailed;
      final SkinDust skin = new SkinDust(48);

      private State() {
      }
   }
}
