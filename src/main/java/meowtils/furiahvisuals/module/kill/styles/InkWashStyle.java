package meowtils.furiahvisuals.module.kill.styles;

import java.util.Locale;
import meowtils.furiahvisuals.fx.Breeze;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.kit.Ink;
import meowtils.furiahvisuals.fx.kit.Smoke;
import meowtils.furiahvisuals.fx.look.NearCamera;
import meowtils.furiahvisuals.fx.pass.FxPass;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.DissolveShader;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.render.look.BodyLook;
import meowtils.furiahvisuals.render.stroke.Strokes;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class InkWashStyle extends KillStyleBase {
   private static final float SWEEP = 0.4F;
   private static final float STILL = 0.7F;
   private static final float STAMPED = 0.85F;
   private static final float DIFFUSE = 1.1F;
   private static final float AFTER = 0.05F;
   private static final int CURLS = 28;
   private static final int NIGHT = -3549953;
   private static final float STROKE_AT = 0.62F;
   private static final float STROKE_PAINT = 0.08F;
   private static final int STROKE_POINTS = 40;
   public final ColorSetting seal;
   public final SliderSetting levels;
   public final SliderSetting outline;
   public final BooleanSetting sealOn;
   public final ModeSetting<Ink.SealDesign> sealDesign;
   public final SliderSetting diffuseSpeed;
   public final BooleanSetting grain;
   private final Strokes.Style style = new Strokes.Style();
   private static final float NOISE = 0.55F;
   private static final float EDGE = 0.14F;
   private static final float CURL_WIND = 0.4F;
   private static final double[] FLOW = new double[3];

   public InkWashStyle(KillEffectsModule module) {
      super(
         module,
         KillStyle.INK_WASH,
         new ColorSetting("inkwash.color", "Ink", "The painting's ink, its outlines and the curls.", ColorSetting.Mode.STATIC, -15461356, -14013906, 1.0)
      );
      this.signature(this.color, -15461356);
      this.seal = this.setting(new ColorSetting("inkwash.seal", "Seal", "The red seal's color.", ColorSetting.Mode.STATIC, -3660241, -2078886, 1.0).secondary());
      this.signature(this.seal, -3660241);
      this.levels = this.setting(new SliderSetting("inkwash.levels", "Wash Levels", "How many shades of ink the painting has.", 2.0, 5.0, 3.0, 1.0));
      this.outline = this.look(
         this.setting(new SliderSetting("inkwash.outline", "Outline Width", "How bold the brush outlines are.", 50.0, 200.0, 100.0, 5.0).unit("%"))
      );
      this.sealOn = this.setting(new BooleanSetting("inkwash.sealOn", "Seal", "A red seal stamps onto the painting's side.", true));
      this.sealDesign = this.setting(
         new ModeSetting<>("inkwash.sealDesign", "Seal Design", "The crest cut into the seal.", Ink.SealDesign.class, Ink.SealDesign.SUN)
            .style(ModeSetting.Style.CHIPS)
      );
      this.diffuseSpeed = this.look(
         this.setting(new SliderSetting("inkwash.diffuseSpeed", "Diffuse Speed", "How fast the painting diffuses away.", 50.0, 200.0, 100.0, 5.0).unit("%"))
      );
      this.grain = this.setting(new BooleanSetting("inkwash.grain", "Paper Grain", "The grain of rice paper in the painting.", true));
   }

   private float diffuse() {
      return 110.0F / this.diffuseSpeed.getFloat();
   }

   @Override
   public float seconds(KillFx fx) {
      return 0.85F + this.diffuse() + 0.05F;
   }

   @Override
   public void start(KillFx fx) {
      InkWashStyle.State s = new InkWashStyle.State();
      s.n = Math.min(28, fx.count(22.4F, false));
      BodyProxy.Part[] p = fx.rest;

      for (int i = 0; i < s.n; i++) {
         int part = pick(hash(fx.seed, i * 3 + 1));
         BodyProxy.Part q = p[part];
         double ox = (hash(fx.seed, i * 3 + 2) - 0.5) * 0.4;
         double oy = (hash(fx.seed, i * 3 + 3) - 0.5) * 0.6;
         double oz = (hash(fx.seed, i * 3 + 4) - 0.5) * 0.2;
         s.x[i] = q.x + q.r[0] * ox + q.r[1] * oy + q.r[2] * oz;
         s.y[i] = q.y + q.r[3] * ox + q.r[4] * oy + q.r[5] * oz;
         s.z[i] = q.z + q.r[6] * ox + q.r[7] * oy + q.r[8] * oz;
         float sweep = Easing.clamp01((fx.ctx.feetY + 2.0 - s.y[i]) / 2.0);
         float noise = DissolveShader.noiseAt(s.x[i] - fx.ctx.feetX, s.y[i] - fx.ctx.feetY, s.z[i] - fx.ctx.feetZ);
         s.at[i] = DissolveShader.thresholdAt(sweep, noise, 0.55F, 0.14F);
         s.size[i] = (float)((0.35 + 0.3 * hash(fx.seed, i * 3 + 5)) * fx.size);
         s.spin[i] = (float)((hash(fx.seed, i * 3 + 6) - 0.5) * 2.0);
      }

      Breeze.at(fx.ctx.feetX, fx.ctx.feetY + 1.0, fx.ctx.feetZ, fx.preview, s.wind);
      int k = 0;

      while (k < 3) {
         int n = k++;
         s.wind[n] = s.wind[n] * 0.4F;
      }

      fx.data = s;
   }

   private static int pick(double h) {
      if (h < 0.35) {
         return 1;
      } else {
         return h < 0.55 ? 0 : 2 + (int)((h - 0.55) / 0.45 * 4.0);
      }
   }

   @Override
   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | FxPass.TRANSLUCENT.bit() | (fx.night > 0.01F ? FxPass.GLOW.bit() : 0);
   }

   private float diffused(KillFx fx) {
      return fx.phase(0.85F, 0.85F + this.diffuse());
   }

   @Override
   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      InkWashStyle.State s = (InkWashStyle.State)fx.data;
      if (s != null) {
         float alpha = fx.alpha();
         if (pass == FxPass.SOLID) {
            this.body(fx, f);
         } else if (pass == FxPass.TRANSLUCENT) {
            Camera cam = Vfx.camera();
            if (this.sealOn.get()) {
               this.seal(fx, f, alpha);
            }

            if (fx.crit) {
               this.stroke(fx, f, cam, alpha);
            }

            this.curls(fx, f, s, cam, alpha);
         } else if (pass == FxPass.GLOW) {
            this.night(fx, f, alpha);
         }
      }
   }

   private void body(KillFx fx, FxFrame f) {
      BodyProxy.Look look = fx.look;
      float t = fx.t;
      int ink = this.color.getColor(0.5F);
      look.params[1] = this.levels.getInt();
      look.params[2] = this.grain.get() ? 1 : 0;
      look.outlineColor = ink | 0xFF000000;
      look.outlineWidth = 2.0F * this.outline.getFloat() / 100.0F;
      look.worldLight = fx.light;
      if (t < 0.4F) {
         look.kind = BodyLook.SKIN;
         look.sweepTo = BodyLook.INK;
         look.sweep = Easing.inOutCubic(t / 0.4F);
         look.sweepMode = 1;
         look.sweepOX = fx.ctx.feetX;
         look.sweepOY = fx.ctx.feetY;
         look.sweepOZ = fx.ctx.feetZ;
         look.sweepSpan = 1.95F;
         look.sweepEdgeColor = -790814;
      } else {
         look.kind = BodyLook.INK;
         look.sweepTo = null;
      }

      float d = this.diffused(fx);
      if (d > 0.0F) {
         look.dissolve = d;
         look.dissolveNoise = 0.55F;
         look.sweepX = 0.0;
         look.sweepY = -1.0;
         look.sweepZ = 0.0;
         look.sweepFrom = -(fx.ctx.feetY + 2.0);
         look.sweepLength = 2.0;
         look.edgeColor = ink | 0xFF000000;
         look.edgeWidth = 0.14F;
      } else {
         look.dissolve = -1.0F;
      }

      fx.drawBody(f);
      RenderState.translucent(true);
   }

   private void seal(KillFx fx, FxFrame f, float alpha) {
      float stamp = fx.phase(0.7F, 0.85F);
      if (!(stamp <= 0.0F)) {
         BodyProxy.Part b = fx.parts[1];
         double tx = -fx.ctx.dirX;
         double tz = -fx.ctx.dirZ;
         double lx = b.r[0];
         double lz = b.r[6];
         double bx = b.r[2];
         double bz = b.r[8];
         double dl = lx * tx + lz * tz;
         double db = bx * tx + bz * tz;
         double az;
         double ay;
         double ax;
         double half;
         double nz;
         double ny;
         double nx;
         if (Math.abs(db) >= Math.abs(dl)) {
            double sg = Math.signum(db);
            nx = b.r[2] * sg;
            ny = b.r[5] * sg;
            nz = b.r[8] * sg;
            half = 0.1171875;
            ax = b.r[0];
            ay = b.r[3];
            az = b.r[6];
         } else {
            double sg = Math.signum(dl);
            nx = b.r[0] * sg;
            ny = b.r[3] * sg;
            nz = b.r[6] * sg;
            half = 0.234375;
            ax = b.r[2];
            ay = b.r[5];
            az = b.r[8];
         }

         double ux = -b.r[1];
         double uy = -b.r[4];
         double uz = -b.r[7];
         double lift = half + 0.004;
         double cx = b.x + nx * lift + ux * -0.12 + ax * 0.06;
         double cy = b.y + ny * lift + uy * -0.12 + ay * 0.06;
         double cz = b.z + nz * lift + uz * -0.12 + az * 0.06;
         float k = Easing.outBack(stamp, 1.6F);
         double w = 0.22 * fx.size * (1.35 - 0.35 * k);
         double h = w * (0.8 + 0.2 * Easing.outCubic(stamp));
         float sweep = Easing.clamp01((fx.ctx.feetY + 2.0 - cy) / 2.0);
         float gone = Easing.smoothstep(sweep * 0.7F, sweep * 0.7F + 0.15F, this.diffused(fx));
         float a = alpha * Easing.clamp01(stamp * 3.0F) * (1.0F - gone);
         if (!(a <= 0.003F)) {
            int col = ColorUtil.shade(this.seal.getColor(0.5F), fx.light);
            RenderState.decal(true);
            Ink.quad(Ink.seals(), this.sealDesign.get().ordinal(), 4, f.rx(cx), f.ry(cy), f.rz(cz), ax, ay, az, ux, uy, uz, w, h, 0.0F, alpha(col, a));
            RenderState.decal(false);
         }
      }
   }

   private void stroke(KillFx fx, FxFrame f, Camera cam, float alpha) {
      float painted = Easing.outCubic(fx.phase(0.62F, 0.7F));
      if (!(painted <= 0.0F)) {
         double sx = fx.ctx.swipeX;
         double sy = fx.ctx.swipeY;
         double sz = fx.ctx.swipeZ;
         double len = 1.5 * fx.size;
         double cx = fx.ctx.feetX - fx.ctx.dirX * 0.4;
         double cy = fx.ctx.feetY + 1.05;
         double cz = fx.ctx.feetZ - fx.ctx.dirZ * 0.4;

         for (int i = 0; i < 40; i++) {
            double u = (i / 39.0 - 0.5) * len;
            double bow = 0.08 * len * Math.sin(Math.PI * i / 39.0);
            double x = cx + sx * u;
            double y = cy + sy * u + bow;
            double z = cz + sz * u;
            Strokes.X[i] = f.rx(x);
            Strokes.Y[i] = f.ry(y);
            Strokes.Z[i] = f.rz(z);
            Strokes.A[i] = NearCamera.dark(cam, Strokes.X[i], Strokes.Y[i], Strokes.Z[i], 1.0F);
         }

         Strokes.wobble(40, 0.012 * len, fx.seed, fx.ctx.dirX, fx.ctx.dirY, fx.ctx.dirZ);
         this.style.reset();
         this.style.tool = Strokes.Tool.BRUSH;
         this.style.color = this.color.getColor(0.5F);
         this.style.width = 0.22 * fx.size;
         this.style.alpha = alpha;
         this.style.progress = painted;
         this.style.heavyStart = 0.6F;
         this.style.dryness = 0.55F;
         this.style.diffuse = this.diffused(fx);
         this.style.light = fx.light;
         this.style.flat = true;
         this.style.normalX = fx.ctx.dirX;
         this.style.normalY = fx.ctx.dirY;
         this.style.normalZ = fx.ctx.dirZ;
         this.style.pointAlpha = true;
         Strokes.draw(40, this.style);
      }
   }

   private void curls(KillFx fx, FxFrame f, InkWashStyle.State s, Camera cam, float alpha) {
      float d = this.diffused(fx);
      if (!(d <= 0.0F)) {
         Smoke.Puffs puffs = s.puffs;
         puffs.count = 0;
         double[] flow = FLOW;
         float span = this.diffuse();

         for (int i = 0; i < s.n; i++) {
            float born = 0.85F + s.at[i] * span;
            float age = fx.t - born;
            float k;
            if (!(age <= 0.0F) && !((k = age / Math.min(1.4F, span)) >= 1.0F)) {
               Smoke.curl(s.x[i], s.y[i] + age * 0.3, s.z[i], age, 1.6, flow);
               double rise = 0.55 * age + 0.25 * age * age;
               double x = s.x[i] + fx.ctx.dirX * 0.35 * age + flow[0] * 0.25 * age + s.wind[0] * age;
               double y = s.y[i] + rise + flow[1] * 0.15 * age + s.wind[1] * age;
               double z = s.z[i] + fx.ctx.dirZ * 0.35 * age + flow[2] * 0.25 * age + s.wind[2] * age;
               int j = puffs.count++;
               puffs.x[j] = f.rx(x);
               puffs.y[j] = f.ry(y);
               puffs.z[j] = f.rz(z);
               puffs.size[j] = s.size[i] * (0.6F + 1.2F * k);
               float a = 0.55F * Easing.smoothstep(0.0F, 0.12F, k) * (1.0F - Easing.smoothstep(0.35F, 1.0F, k));
               puffs.alpha[j] = NearCamera.dark(cam, puffs.x[j], puffs.y[j], puffs.z[j], a);
               puffs.turn[j] = s.spin[i] * age;
               puffs.cell[j] = i % 4;
            }
         }

         if (puffs.count != 0) {
            puffs.sort(cam);
            puffs.draw(this.color.getColor(0.5F), alpha, null, cam, 0.0, 0.0, 1.0);
         }
      }
   }

   private void night(KillFx fx, FxFrame f, float alpha) {
      float a = alpha * fx.night * 0.12F * (1.0F - this.diffused(fx));
      if (!(a <= 0.003F)) {
         BodyProxy.Part b = fx.parts[1];
         Vfx.sprite(Textures.GLOW, f.rx(b.x + fx.ctx.dirX * 0.3), f.ry(b.y + 0.2), f.rz(b.z + fx.ctx.dirZ * 0.3), 2.2 * fx.size, 0.0F, alpha(-3549953, a));
      }
   }

   @Override
   public String stateForTest(KillFx fx) {
      InkWashStyle.State s = (InkWashStyle.State)fx.data;
      if (s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.4F ? "sweeping into ink" : (t < 0.7F ? "a still painting" : (t < 0.85F ? "the seal stamps" : "diffusing"));
         int out = 0;
         float span = this.diffuse();

         for (int i = 0; i < s.n; i++) {
            if (t > 0.85F + s.at[i] * span) {
               out++;
            }
         }

         return String.format(
            Locale.ROOT,
            "%s; %d levels, outline %.1f px, dissolved %.2f (%s), %d of %d curls out%s; light %.2f, night %.2f",
            phase,
            this.levels.getInt(),
            2.0F * this.outline.getFloat() / 100.0F,
            this.diffused(fx),
            DissolveShader.available() ? "shader" : "fade",
            out,
            s.n,
            fx.crit ? ", crit stroke" : "",
            fx.light,
            fx.night
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

   private static final class State {
      final Smoke.Puffs puffs = new Smoke.Puffs(28);
      final double[] x = new double[28];
      final double[] y = new double[28];
      final double[] z = new double[28];
      final float[] at = new float[28];
      final float[] size = new float[28];
      final float[] spin = new float[28];
      int n;
      final float[] wind = new float[3];

      private State() {
      }
   }
}
