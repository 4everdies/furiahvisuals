package meowtils.furiahvisuals.module.kill.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.kit.Liquid;
import meowtils.furiahvisuals.fx.pass.FxPass;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.render.look.BodyLook;
import meowtils.furiahvisuals.render.material.Finish;
import meowtils.furiahvisuals.render.material.Material;
import meowtils.furiahvisuals.render.material.MeshBatch;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class MeltdownStyle extends KillStyleBase {
   private static final float SWEPT = 0.4F;
   private static final float STATUE = 0.7F;
   private static final float MELT = 0.7F;
   private static final float RIPPLE = 0.4F;
   private static final float SPHERE = 0.4F;
   private static final float FADE = 0.08F;
   private static final float GLINT_FROM = 0.45F;
   private static final int BALLS = 11;
   private static final int MAX_DROPLETS = 8;
   private static final int CROWN = 8;
   private static final int MAX = 28;
   private static final double PUDDLE = 0.85;
   private static final double SPHERE_R = 0.3;
   private static final double SAG = 0.3;
   private static final float RIM = 0.22F;
   public final ModeSetting<Material> material = this.materialSetting(
      "meltdown", Material.CHROME, "What the body melts into. Custom takes the Tint, with a finish."
   );
   public final ModeSetting<Finish> finish = this.finishSetting("meltdown", Finish.METALLIC, this.material);
   public final SliderSetting meltSpeed;
   public final SliderSetting puddleSize;
   public final SliderSetting droplets;
   public final ModeSetting<MeltdownStyle.Quality> quality;
   private static final int[] PER_PART = new int[]{1, 2, 2, 2, 2, 2};
   private static final double[] SPACING = new double[]{0.0, 0.18, 0.17, 0.17, 0.17, 0.17};
   private static final double[] RADIUS = new double[]{0.26, 0.21, 0.12, 0.12, 0.13, 0.13};

   public MeltdownStyle(KillEffectsModule module) {
      super(
         module,
         KillStyle.MELTDOWN,
         new ColorSetting("meltdown.color", "Tint", "The metal's color with the Custom material.", ColorSetting.Mode.STATIC, -2300950, -6310712, 1.0)
      );
      this.color.visibleWhen(new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return MeltdownStyle.this.material.get() == Material.CUSTOM;
         }
      });
      this.signature(this.color, -2300950);
      this.meltSpeed = this.look(
         this.setting(new SliderSetting("meltdown.meltSpeed", "Melt Speed", "How fast the statue melts into a puddle.", 50.0, 200.0, 100.0, 5.0).unit("%"))
      );
      this.puddleSize = this.look(
         this.setting(new SliderSetting("meltdown.puddleSize", "Puddle Size", "How far the puddle spreads.", 50.0, 150.0, 100.0, 5.0).unit("%"))
      );
      this.droplets = this.setting(new SliderSetting("meltdown.droplets", "Droplets", "How many droplets jump out of the puddle.", 0.0, 8.0, 4.0, 1.0));
      this.quality = this.setting(
         new ModeSetting<>(
            "meltdown.quality",
            "Quality",
            "How fine the liquid's surface is: Low, Medium or High (a little more work each frame).",
            MeltdownStyle.Quality.class,
            MeltdownStyle.Quality.MEDIUM
         )
      );
   }

   private float melt() {
      return 70.0F / this.meltSpeed.getFloat();
   }

   @Override
   public float seconds(KillFx fx) {
      return 0.7F + this.melt() + 0.4F + 0.4F;
   }

   @Override
   public int passes(KillFx fx) {
      int body = this.material.get().translucent(this.finish.get()) ? FxPass.TRANSLUCENT.bit() : FxPass.SOLID.bit();
      return body | FxPass.SOLID.bit() | FxPass.GLOW.bit();
   }

   @Override
   public void start(KillFx fx) {
      MeltdownStyle.State s = new MeltdownStyle.State();
      double ax = fx.ctx.dirX;
      double az = fx.ctx.dirZ;
      double l = Math.sqrt(ax * ax + az * az);
      if (l < 1.0E-4) {
         ax = 0.0;
         az = 1.0;
         l = 1.0;
      }

      s.awayX = ax / l;
      s.awayZ = az / l;
      double size = fx.size;
      double puddle = 0.85 * this.puddleSize.getFloat() / 100.0 * size;
      s.cx = fx.ctx.feetX + s.awayX * 0.3 * size;
      s.cz = fx.ctx.feetZ + s.awayZ * 0.3 * size;
      int b = 0;

      for (int part = 0; part < 6; part++) {
         BodyProxy.Part q = fx.rest[part];

         for (int k = 0; k < PER_PART[part]; k++) {
            double along = PER_PART[part] == 1 ? 0.0 : (k == 0 ? -1 : 1) * SPACING[part];
            s.sx[b] = q.x + q.r[1] * along;
            s.sy[b] = q.y + q.r[4] * along;
            s.sz[b] = q.z + q.r[7] * along;
            s.r[b] = RADIUS[part] * size;
            double ox = s.sx[b] - fx.ctx.feetX;
            double oz = s.sz[b] - fx.ctx.feetZ;
            double angle = Math.atan2(oz, ox) + (hash(fx.seed, b * 3 + 1) - 0.5) * 1.2;
            if (ox * ox + oz * oz < 1.0E-4) {
               angle = (Math.PI * 2) * hash(fx.seed, b * 3 + 2);
            }

            double reach = puddle * (0.25 + 0.6 * hash(fx.seed, b * 3 + 3));
            s.px[b] = s.cx + Math.cos(angle) * reach;
            s.pz[b] = s.cz + Math.sin(angle) * reach;
            b++;
         }
      }

      s.droplets = fx.lod ? this.droplets.getInt() / 2 : this.droplets.getInt();

      for (int i = 0; i < s.droplets; i++) {
         double a = (Math.PI * 2) * hash(fx.seed, 100 + i);
         double r = puddle * (0.2 + 0.55 * hash(fx.seed, 101 + i));
         s.dx[i] = s.cx + Math.cos(a) * r;
         s.dz[i] = s.cz + Math.sin(a) * r;
         s.dUp[i] = (1.1 + 0.7 * hash(fx.seed, 102 + i)) * Math.sqrt(size);
         s.dAt[i] = (float)(0.05 + 0.5 * hash(fx.seed, 103 + i));
      }

      fx.data = s;
   }

   private float meltEnd() {
      return 0.7F + this.melt();
   }

   private float rippleEnd() {
      return this.meltEnd() + 0.4F;
   }

   private void balls(KillFx fx, FxFrame f, MeltdownStyle.State s) {
      Liquid.Field fl = s.field;
      float t = fx.t;
      fl.begin(1.9);
      if (!(t < 0.68F)) {
         double ground = fx.ctx.groundY;
         float melt = fx.phase(0.7F, this.meltEnd());
         float ripple = fx.phase(this.meltEnd(), this.rippleEnd());
         float sphere = fx.phase(this.rippleEnd(), this.rippleEnd() + 0.4F);
         double size = fx.size;
         float gather = Easing.inOutCubic(Easing.clamp01(sphere / 0.4F));
         double sphereR = 0.3 * Math.cbrt(this.puddleSize.getFloat() / 100.0) * size;
         double hop = sphere > 0.4F && sphere < 0.8F ? Math.sin(Math.PI * (sphere - 0.4F) / 0.4F) * 0.35 * size : 0.0;
         double vanish = 1.0 - Easing.inCubic(Easing.clamp01((sphere - 0.75F) / 0.25F));

         for (int b = 0; b < 11; b++) {
            double h = s.sy[b] - ground;
            double q = Easing.inQuad(melt);
            double flat = 1.0 + 1.6 * q;
            double r = s.r[b] * (1.0 + 0.45 * q);
            double y = ground + r / flat * 0.9 + (h - r / flat * 0.9) * (1.0 - q);
            double spread = Easing.inOutCubic(melt);
            double x = s.sx[b] + (s.px[b] - s.sx[b]) * spread + s.awayX * 0.3 * size * 0.3 * Math.sin(Math.PI * melt);
            double z = s.sz[b] + (s.pz[b] - s.sz[b]) * spread + s.awayZ * 0.3 * size * 0.3 * Math.sin(Math.PI * melt);
            if (ripple > 0.0F && sphere <= 0.0F) {
               double d = Math.sqrt((x - s.cx) * (x - s.cx) + (z - s.cz) * (z - s.cz));
               y += 0.012 * size * Math.sin(d * 18.0 - ripple * 20.0) * (1.0 - ripple);
            }

            if (sphere > 0.0F) {
               x += (s.cx - x) * gather;
               z += (s.cz - z) * gather;
               y += (ground + sphereR + hop - y) * gather;
               flat += (1.0 - flat) * gather;
               r += (sphereR * 0.62 - r) * gather;
               r *= vanish;
            }

            if (!(r < 0.004)) {
               fl.ball(f.rx(x), f.ry(y), f.rz(z), r, 1.0, 0.0, 1.0, 0.0, flat);
            }
         }

         if (ripple > 0.0F && sphere < 0.3F) {
            double radius = 0.85 * this.puddleSize.getFloat() / 100.0 * size * 0.75 * (1.0 - gather);
            double w = ripple * 30.0;
            fl.ring(
               f.rx(s.cx),
               f.ry(ground + 0.03 * size),
               f.rz(s.cz),
               radius,
               0.05 * size,
               0.8 * (1.0 - gather),
               0.025 * size * (1.0 - ripple),
               Math.cos(w),
               Math.sin(w)
            );
         }

         float rt = t - this.meltEnd();

         for (int i = 0; i < s.droplets; i++) {
            double air = rt - s.dAt[i] * 0.4F;
            double up = s.dUp[i] * air - 4.5 * air * air;
            if (!(air <= 0.0) && !(up <= 0.0)) {
               fl.ball(f.rx(s.dx[i]), f.ry(ground + 0.04 + up), f.rz(s.dz[i]), 0.045 * size, 1.0, 0.0, 1.0, 0.0, 0.85);
            }
         }

         float c;
         if (fx.crit && (c = fx.phase(this.meltEnd() - 0.05F, this.meltEnd() + 0.3F)) > 0.0F && c < 1.0F) {
            double radius = 0.85 * this.puddleSize.getFloat() / 100.0 * size * 0.55;
            double up = Math.sin(Math.PI * c) * 0.28 * size;

            for (int ix = 0; ix < 8; ix++) {
               double a = (Math.PI * 2) * ix / 8.0 + fx.seed;
               fl.ball(
                  f.rx(s.cx + Math.cos(a) * radius * (1.0 + 0.3 * c)),
                  f.ry(ground + 0.05 + up),
                  f.rz(s.cz + Math.sin(a) * radius * (1.0 + 0.3 * c)),
                  0.04 * size,
                  1.0,
                  0.0,
                  1.0,
                  0.0,
                  0.7
               );
            }
         }
      }
   }

   @Override
   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      MeltdownStyle.State s = (MeltdownStyle.State)fx.data;
      if (s != null) {
         if (pass == FxPass.GLOW) {
            this.glow(fx, f, s);
         } else {
            boolean translucent = this.material.get().translucent(this.finish.get());
            if (pass == FxPass.SOLID) {
               this.statue(fx, f);
            }

            if (pass == FxPass.TRANSLUCENT == translucent) {
               this.balls(fx, f, s);
               if (s.field.balls() != 0) {
                  s.field.plain = fx.lod;
                  boolean custom = this.material.get() == Material.CUSTOM;
                  int tint = custom ? this.color.getColor(0.5F) : -1;
                  float a = (custom ? fx.alpha() * (tint >>> 24) / 255.0F : fx.alpha()) * fx.phase(0.68F, 0.74F);
                  double cells = this.quality.get().cells;
                  double cell = 1.9 * fx.size / cells;
                  MeshBatch.begin(this.material.get(), this.finish.get(), fx.light, 0.22F, -1, true, pass == FxPass.SOLID);
                  s.field.mesh(cell, f.rx(s.cx), f.ry(fx.ctx.groundY), f.rz(s.cz), f.distanceSqToEye(s.cx, fx.ctx.groundY, s.cz), a, tint, null);
                  MeshBatch.end();
               }
            }
         }
      }
   }

   private void statue(KillFx fx, FxFrame f) {
      float t = fx.t;
      float gone = fx.phase(0.7F, 0.78F);
      if (!(gone >= 1.0F)) {
         BodyProxy.Look look = fx.look;
         look.material = this.material.get();
         look.finish = this.finish.get();
         look.materialTint = this.color.getColor(0.5F) | 0xFF000000;
         look.worldLight = fx.light;
         look.glint = fx.crit ? 0.1F + 0.45F * (float)Math.sin(Math.PI * fx.phase(0.45F, 0.7F)) : 0.1F;
         look.roundness = 0.9F;
         look.reflectTilt = 0.35F;
         if (t < 0.4F) {
            look.kind = BodyLook.SKIN;
            look.sweepTo = BodyLook.MATERIAL;
            look.sweep = Easing.inOutCubic(t / 0.4F);
            look.sweepMode = 0;
            look.sweepOX = fx.ctx.feetX;
            look.sweepOY = fx.ctx.feetY;
            look.sweepOZ = fx.ctx.feetZ;
            look.sweepSpan = 1.95F;
            look.sweepEdgeColor = -1;
         } else {
            look.kind = BodyLook.MATERIAL;
            look.sweepTo = null;
         }

         look.dissolve = -1.0F;
         float before = look.alpha;
         look.alpha = 1.0F - gone;
         fx.drawBody(f);
         look.alpha = before;
         RenderState.translucent(true);
      }
   }

   private void glow(KillFx fx, FxFrame f, MeltdownStyle.State s) {
      float alpha = fx.alpha();
      boolean calm = FuriahsVisuals.instance.isReduceFlashing();
      BodyProxy.Part b = fx.rest[1];
      float g;
      if (fx.crit && (g = fx.phase(0.45F, 0.7F)) > 0.0F && g < 1.0F) {
         Camera cam = Vfx.camera();
         double y = fx.ctx.feetY + 1.95 * fx.size * g;
         double x = f.rx(b.x);
         double yy = f.ry(y);
         double z = f.rz(b.z);
         double tx = cam.posX - x;
         double ty = cam.posY - yy;
         double tz = cam.posZ - z;
         double tl = Math.max(0.001, Math.sqrt(tx * tx + ty * ty + tz * tz));
         double pull = Math.min(0.4, tl * 0.5) / tl;
         Vfx.sprite(Textures.FLARE, x + tx * pull, yy + ty * pull, z + tz * pull, 0.9 * fx.size, 0.2F, core(-1, (calm ? 0.35F : 0.75F) * alpha));
      }

      float flash;
      if ((flash = 1.0F - Easing.clamp01(Math.abs(fx.t - 0.74F) / 0.09F)) > 0.0F) {
         Vfx.sprite(Textures.GLOW, f.rx(b.x), f.ry(b.y), f.rz(b.z), 2.4 * fx.size, 0.0F, alpha(-1, (calm ? 0.2F : 0.55F) * flash * alpha));
      }

      float sphere;
      if ((sphere = fx.phase(this.rippleEnd(), this.rippleEnd() + 0.4F)) > 0.75F && sphere < 1.0F) {
         float k = (float)Math.sin(Math.PI * (sphere - 0.75F) / 0.25);
         double y = fx.ctx.groundY + 0.3 * fx.size;
         Vfx.sprite(Textures.SPARKLE, f.rx(s.cx), f.ry(y), f.rz(s.cz), 0.6 * fx.size, 0.1F, core(-1, 0.6F * k * alpha));
      }
   }

   @Override
   public String stateForTest(KillFx fx) {
      MeltdownStyle.State s = (MeltdownStyle.State)fx.data;
      if (s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.4F
            ? "turning to metal"
            : (t < 0.7F ? "a statue" : (t < this.meltEnd() ? "melting" : (t < this.rippleEnd() ? "the puddle ripples" : "the sphere")));
         Liquid.Field fl = s.field;
         String how = fl.lastMode == 0 ? "meshed" : (fl.lastMode == 1 ? "its last mesh" : (fl.lastMode == 2 ? "spheres" : "nothing"));
         return String.format(
            Locale.ROOT,
            "%s; %s, %d balls (%s: %d vertices, %d points), %d droplets, %s quality%s",
            phase,
            this.material.get().displayName(),
            fl.balls(),
            how,
            fl.lastVertices,
            fl.lastPoints,
            s.droplets,
            this.quality.get().displayName(),
            fx.crit ? ", crit" : ""
         );
      }
   }

   private static double hash(int a, int b) {
      int h = a * 668265261 ^ (b + 374761393) * -1640531535;
      h ^= h >>> 15;
      h *= -2048144789;
      h ^= h >>> 13;
      return (h >>> 8 & 16777215) / 1.6777216E7;
   }

   public static enum Quality implements Displayable {
      LOW("Low", "16 cells across the body", 16),
      MEDIUM("Medium", "24 cells across the body", 24),
      HIGH("High", "32 cells across the body", 32);

      private final String displayName;
      private final String description;
      final int cells;

      private Quality(String displayName, String description, int cells) {
         this.displayName = displayName;
         this.description = description;
         this.cells = cells;
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
      final Liquid.Field field = new Liquid.Field(28);
      final double[] sx = new double[11];
      final double[] sy = new double[11];
      final double[] sz = new double[11];
      final double[] r = new double[11];
      final double[] px = new double[11];
      final double[] pz = new double[11];
      double cx;
      double cz;
      double awayX;
      double awayZ;
      int droplets;
      final double[] dx = new double[8];
      final double[] dz = new double[8];
      final double[] dUp = new double[8];
      final float[] dAt = new float[8];

      private State() {
      }
   }
}
