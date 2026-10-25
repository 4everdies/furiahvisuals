package meowtils.furiahvisuals.module.kill.styles;

import java.util.Locale;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.kit.Vines;
import meowtils.furiahvisuals.fx.pass.FxPass;
import meowtils.furiahvisuals.module.ambience.AmbienceModule;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.render.look.BodyLook;
import meowtils.furiahvisuals.render.material.Finish;
import meowtils.furiahvisuals.render.material.Material;
import meowtils.furiahvisuals.render.material.MeshBatch;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class OvergrowthStyle extends KillStyleBase {
   private static final float GROWN = 0.7F;
   private static final float BLOOMED = 1.0F;
   private static final float WITHERED = 1.7F;
   private static final float COLLAPSED = 2.4F;
   private static final int POINTS = 26;
   private static final int MAX_VINES = 8;
   private static final int MAX_ROSES = 10;
   private static final int MAX_PETALS = 48;
   private static final double TURNS = 2.0;
   private static final double R_FOOT = 0.5;
   private static final double R_HEAD = 0.3;
   private static final double TOP = 2.05;
   private static final double LEAN = 0.18;
   private static final double PETAL_GRAVITY = 1.6;
   private static final double PETAL_DRAG = 2.4;
   private static final int NIGHT_RIM = -6309633;
   public final ColorSetting thornTips;
   public final ColorSetting roses;
   public final SliderSetting vineCount;
   public final SliderSetting roseCount;
   public final SliderSetting thornDensity;
   public final ModeSetting<OvergrowthStyle.Ending> ending;
   public final BooleanSetting petals;
   private final Vines kit = new Vines(208);
   private final double[] point = new double[3];
   private final float[] wind = new float[3];

   public OvergrowthStyle(KillEffectsModule module) {
      super(
         module,
         KillStyle.OVERGROWTH,
         new ColorSetting("overgrowth.color", "Vines", "The brambles' bark.", ColorSetting.Mode.STATIC, -14734816, -12965346, 1.0)
      );
      this.signature(this.color, -14734816);
      this.thornTips = this.setting(
         new ColorSetting("overgrowth.thornTips", "Thorn Tips", "The thorns' pale tips.", ColorSetting.Mode.STATIC, -1452088, -1, 1.0).secondary()
      );
      this.signature(this.thornTips, -1452088);
      this.roses = this.setting(
         new ColorSetting("overgrowth.roses", "Roses", "The roses and their petals.", ColorSetting.Mode.STATIC, -5041618, -9760982, 1.0).secondary()
      );
      this.signature(this.roses, -5041618);
      this.vineCount = this.look(this.setting(new SliderSetting("overgrowth.vines", "Vines", "How many brambles climb the body.", 3.0, 8.0, 5.0, 1.0)));
      this.roseCount = this.setting(new SliderSetting("overgrowth.roseCount", "Roses", "How many roses bloom on the vines.", 0.0, 10.0, 6.0, 1.0));
      this.thornDensity = this.setting(
         new SliderSetting("overgrowth.thornDensity", "Thorn Density", "How thick with thorns the vines are.", 0.0, 100.0, 60.0, 5.0).unit("%")
      );
      this.ending = this.setting(
         new ModeSetting<>(
               "overgrowth.ending", "Ending", "Withering into petals, or sinking into the ground.", OvergrowthStyle.Ending.class, OvergrowthStyle.Ending.WITHER
            )
            .style(ModeSetting.Style.CHIPS)
      );
      this.petals = this.setting(new BooleanSetting("overgrowth.petals", "Petals", "Dark petals blow away as it collapses.", true));
   }

   @Override
   public float seconds(KillFx fx) {
      return 2.4F;
   }

   @Override
   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | (fx.night > 0.01F ? FxPass.GLOW.bit() : 0);
   }

   @Override
   public void start(KillFx fx) {
      OvergrowthStyle.State s = new OvergrowthStyle.State();
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
      int n = this.vines(fx);

      for (int k = 0; k < n; k++) {
         s.start[k] = (Math.PI * 2) * (k + 0.35 * hash(fx.seed, k * 3 + 1)) / n;
         s.delay[k] = 0.12 * hash(fx.seed, k * 3 + 2);
      }

      s.roses = Math.min(10, fx.lod ? this.roseCount.getInt() / 2 : this.roseCount.getInt());

      for (int i = 0; i < s.roses; i++) {
         s.roseVine[i] = i % n;
         s.roseAt[i] = 0.25 + 0.65 * hash(fx.seed, 40 + i * 3);
         s.roseSize[i] = (0.1 + 0.05 * hash(fx.seed, 41 + i * 3)) * fx.size;
      }

      fx.data = s;
   }

   private int vines(KillFx fx) {
      int n = Math.min(8, this.vineCount.getInt());
      return fx.lod ? Math.max(2, n / 2) : n;
   }

   private static double grown(KillFx fx, OvergrowthStyle.State s, int k) {
      return Easing.outCubic(Easing.clamp01((fx.t - s.delay[k]) / (0.7F - s.delay[k])));
   }

   private static float withered(KillFx fx) {
      return Easing.inOutCubic(fx.phase(1.0F, 1.7F));
   }

   private static float collapsed(KillFx fx) {
      return fx.phase(1.7F, 2.4F);
   }

   private float sunk(KillFx fx) {
      return this.ending.get() == OvergrowthStyle.Ending.SINK ? Easing.inCubic(fx.phase(1.2F, 2.4F)) : 0.0F;
   }

   private void vinePoint(KillFx fx, OvergrowthStyle.State s, int k, double u, double sink) {
      double size = fx.size;
      double a = s.start[k] + (Math.PI * 4) * u * (k % 2 == 0 ? 1 : -1);
      double slump = this.ending.get() == OvergrowthStyle.Ending.WITHER ? Easing.smoothstep(0.0F, 0.7F, collapsed(fx)) : 0.0;
      double r = (0.5 + -0.2 * u) * size * (1.0 + 0.6 * slump);
      double lean = 0.18 * u * size;
      this.point[0] = fx.ctx.feetX + Math.cos(a) * r + s.awayX * lean;
      this.point[1] = fx.ctx.groundY + 2.05 * u * size * (1.0 - 0.9 * slump) - sink;
      this.point[2] = fx.ctx.feetZ + Math.sin(a) * r + s.awayZ * lean;
   }

   @Override
   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      OvergrowthStyle.State s = (OvergrowthStyle.State)fx.data;
      if (s != null) {
         if (pass == FxPass.GLOW) {
            this.night(fx, f);
         } else {
            this.body(fx, f);
            Camera cam = Vfx.camera();
            float alpha = fx.alpha();
            float c = collapsed(fx);
            double sink = this.sunk(fx) * 2.05 * fx.size;
            int n = this.vines(fx);
            float vineAlpha = alpha * (this.ending.get() == OvergrowthStyle.Ending.SINK ? 1.0F : 1.0F - Easing.smoothstep(0.45F, 0.95F, c));
            MeshBatch.begin(Material.CUSTOM, Finish.MATTE, fx.light, 0.08F + 0.5F * fx.night, -6309633, true, true);
            if (vineAlpha > 0.003F) {
               for (int k = 0; k < n; k++) {
                  this.vine(fx, f, s, cam, k, n, sink, vineAlpha);
               }
            }

            MeshBatch.end();
            MeshBatch.begin(Material.CUSTOM, Finish.SATIN, fx.light, 0.1F + 0.25F * fx.night, -6309633, false, true);
            if (vineAlpha > 0.003F) {
               this.thorns(fx, s, n, vineAlpha);
               this.roses(fx, f, s, sink, vineAlpha);
            }

            if (this.petals.get() && this.ending.get() == OvergrowthStyle.Ending.WITHER) {
               this.petals(fx, f, s, alpha);
            }

            MeshBatch.end();
            RenderState.translucent(true);
         }
      }
   }

   private void vine(KillFx fx, FxFrame f, OvergrowthStyle.State s, Camera cam, int k, int n, double sink, float alpha) {
      double g = grown(fx, s, k);
      if (!(g <= 0.01)) {
         float w = withered(fx);
         double thick = 0.046 * fx.size * (1.0 - 0.35 * w);
         int bark = ColorUtil.lerp(this.color.getColor((float)k / Math.max(1, n - 1)), -15856630, 0.6F * w);
         int at = k * 26;

         for (int j = 0; j < 26; j++) {
            double u = g * j / 25.0;
            this.vinePoint(fx, s, k, u, sink);
            int i = at + j;
            this.kit.px[i] = f.rx(this.point[0]);
            this.kit.py[i] = f.ry(this.point[1]);
            this.kit.pz[i] = f.rz(this.point[2]);
            double tip = Easing.clamp01((g - u) / 0.08 + (g >= 1.0 ? 0.35 : 0.0));
            double knots = Math.sin(u * 37.0 + k * 3.1) * Math.sin(u * 15.0 + k * 1.7);
            double under = this.point[1] < fx.ctx.groundY - 0.02 ? 0.0 : 1.0;
            this.kit.rad[i] = thick * (1.0 - 0.3 * u) * (1.0 + 0.16 * knots) * Math.max(0.15, tip) * under;
            int cc = ColorUtil.shade(bark, (float)(0.86 + 0.18 * Math.sin(u * 31.0 + k * 2.3)));
            this.kit.col[i] = 0xFF000000 | cc & 16777215;
            this.kit.keep[i] = 1.0F;
         }

         this.kit.frames(at, 26, false, f.rx(fx.ctx.feetX), f.rz(fx.ctx.feetZ));
         this.kit.tube(at, 26, false, cam, alpha);
      }
   }

   private void thorns(KillFx fx, OvergrowthStyle.State s, int n, float alpha) {
      float density = this.thornDensity.getFloat() / 100.0F;
      if (!(density <= 0.0F)) {
         int tip = this.thornTips.getColor(0.5F);
         float w = withered(fx);

         for (int k = 0; k < n; k++) {
            double g = grown(fx, s, k);
            int count = Math.round(density * 22.0F);

            for (int q = 0; q < count; q++) {
               double u = (q + 0.5 + 0.6 * (hash(k * 31 + q, 3) - 0.5)) / count;
               if (!(u < 0.05) && !(u > g - 0.04)) {
                  double at = u / Math.max(1.0E-6, g) * 25.0;
                  double len = 0.07 * fx.size * (0.8 + 0.4 * hash(k * 17 + q, 23)) * (1.0 - 0.3 * w);
                  this.kit.thorn(k * 26, 26, at, (Math.PI * 2) * hash(k * 13 + q, 7), len, tip, w, alpha);
               }
            }
         }
      }
   }

   private void roses(KillFx fx, FxFrame f, OvergrowthStyle.State s, double sink, float alpha) {
      float bloom = Easing.outCubic(fx.phase(0.65F, 1.0F));
      float w = withered(fx);
      if (!(bloom <= 0.0F)) {
         int rose = this.roses.getColor(0.5F);
         int wilted = ColorUtil.lerp(rose, -15856630, 0.75F * w) | 0xFF000000;
         double open = bloom * (1.0 - 0.55 * w);

         for (int i = 0; i < s.roses; i++) {
            int k = s.roseVine[i];
            double g = grown(fx, s, k);
            if (!(s.roseAt[i] > g)) {
               this.vinePoint(fx, s, k, s.roseAt[i], sink);
               double x = this.point[0];
               double y = this.point[1];
               double z = this.point[2];
               if (!(y < fx.ctx.groundY)) {
                  double ox = x - fx.ctx.feetX;
                  double oz = z - fx.ctx.feetZ;
                  double ol = Math.max(1.0E-6, Math.sqrt(ox * ox + oz * oz));
                  this.kit
                     .rose(
                        fx.seed + i & 7,
                        f.rx(x),
                        f.ry(y),
                        f.rz(z),
                        ox / ol,
                        0.25,
                        oz / ol,
                        s.roseSize[i] * (0.4 + 0.6 * bloom) * (1.0 - 0.25 * w),
                        open,
                        wilted,
                        alpha
                     );
               }
            }
         }

         float big;
         if (fx.crit && (big = Easing.outBack(fx.phase(0.85F, 1.1F), 1.4F)) > 0.0F) {
            BodyProxy.Part b = fx.rest[1];
            double tx = -s.awayX;
            double tz = -s.awayZ;
            double x = b.x + tx * 0.2 * fx.size;
            double y = b.y + 0.15 * fx.size - sink;
            double z = b.z + tz * 0.2 * fx.size;
            this.kit
               .rose(
                  fx.seed & 7,
                  f.rx(x),
                  f.ry(y),
                  f.rz(z),
                  tx,
                  0.1,
                  tz,
                  0.2 * fx.size * big * (1.0 - 0.2 * w),
                  Math.min(1.0, (double)big) * (1.0 - 0.5 * w),
                  wilted,
                  alpha
               );
         }
      }
   }

   private void body(KillFx fx, FxFrame f) {
      BodyProxy.Look look = fx.look;
      look.kind = BodyLook.SKIN;
      look.sweepTo = null;
      BodyProxy.Part[] parts = fx.parts;

      for (int i = 0; i < 6; i++) {
         parts[i].copy(fx.rest[i]);
      }

      double sway = Math.sin(fx.t * 7.0) * 0.012 * (1.0 - fx.phase(0.7F, 1.0F)) * fx.size;
      double sink = this.sunk(fx) * 2.05 * fx.size;

      for (BodyProxy.Part p : parts) {
         double h = p.y - fx.ctx.feetY;
         p.x += sway * h;
         p.y -= sink;
         if (sink > 0.0) {
            p.clip = true;
            p.cnx = 0.0;
            p.cny = 1.0;
            p.cnz = 0.0;
            p.cd = fx.ctx.groundY + 0.002;
         }
      }

      float d = this.ending.get() == OvergrowthStyle.Ending.SINK ? 0.0F : fx.phase(1.1F, 1.85F);
      if (!(d >= 1.0F)) {
         if (d > 0.0F) {
            look.dissolve = d;
            look.dissolveNoise = 0.6F;
            look.sweepX = 0.0;
            look.sweepY = -1.0;
            look.sweepZ = 0.0;
            look.sweepFrom = -(fx.ctx.feetY + 2.0);
            look.sweepLength = 2.0;
            look.edgeColor = ColorUtil.shade(this.color.getColor(0.5F), 0.7F) | 0xFF000000;
            look.edgeWidth = 0.1F;
         } else {
            look.dissolve = -1.0F;
         }

         fx.drawBody(f);
         RenderState.translucent(true);
      }
   }

   private void petals(KillFx fx, FxFrame f, OvergrowthStyle.State s, float alpha) {
      float c = collapsed(fx);
      if (!(c <= 0.0F)) {
         if (!s.blown) {
            this.blow(fx, s);
         }

         double dt = Math.max(0.0, Math.min(0.1, (double)(fx.t - s.lastT)));
         s.lastT = fx.t;
         double wx = fx.ctx.dirX * 1.4;
         double wz = fx.ctx.dirZ * 1.4;
         AmbienceModule ambience = !fx.preview && FuriahsVisuals.instance != null ? FuriahsVisuals.instance.getAmbience() : null;
         if (ambience != null && ambience.windAt(fx.ctx.feetX, fx.ctx.feetY + 1.0, fx.ctx.feetZ, this.wind)) {
            wx = this.wind[0];
            wz = this.wind[2];
         }

         int petal = ColorUtil.lerp(this.roses.getColor(0.5F), -15856630, 0.45F) | 0xFF000000;
         float a = alpha * (1.0F - Easing.smoothstep(0.6F, 1.0F, c));
         double groundKeep = Math.pow(0.9, dt * 60.0);

         for (int i = 0; i < s.petals; i++) {
            double flutter = Math.sin(fx.t * 9.0 + i * 1.7) * 0.9;
            s.vx[i] = s.vx[i] + ((wx - s.vx[i]) * 2.4 + flutter * Math.cos(s.spin[i])) * dt;
            s.vz[i] = s.vz[i] + ((wz - s.vz[i]) * 2.4 + flutter * Math.sin(s.spin[i])) * dt;
            s.vy[i] = s.vy[i] + (-1.6 - s.vy[i] * 2.4 * 0.6) * dt;
            s.px[i] = s.px[i] + s.vx[i] * dt;
            s.py[i] = s.py[i] + s.vy[i] * dt;
            s.pz[i] = s.pz[i] + s.vz[i] * dt;
            if (s.py[i] < fx.ctx.groundY + 0.02) {
               s.py[i] = fx.ctx.groundY + 0.02;
               s.vy[i] = 0.0;
               s.vx[i] = s.vx[i] * groundKeep;
               s.vz[i] = s.vz[i] * groundKeep;
            }

            s.spin[i] = s.spin[i] + 3.0 * dt;
            s.tumble[i] = s.tumble[i] + (5.0 + 3.0 * Math.sin(i * 2.1)) * dt;
            this.kit.tumblingPetal(f.rx(s.px[i]), f.ry(s.py[i]), f.rz(s.pz[i]), s.spin[i], s.tumble[i], s.size[i], petal, a);
         }
      }
   }

   private void blow(KillFx fx, OvergrowthStyle.State s) {
      s.blown = true;
      s.lastT = fx.t;
      int n = this.vines(fx);
      s.petals = Math.min(48, Math.round((fx.lod ? 14 : 28) * fx.intensity) + s.roses * 2);

      for (int i = 0; i < s.petals; i++) {
         int k = i % n;
         double u = i < s.roses * 2 ? s.roseAt[i / 2] : 0.1 + 0.85 * hash(fx.seed, 200 + i);
         if (i < s.roses * 2) {
            k = s.roseVine[i / 2];
         }

         this.vinePoint(fx, s, k, u, 0.0);
         s.px[i] = this.point[0];
         s.py[i] = this.point[1];
         s.pz[i] = this.point[2];
         double ox = this.point[0] - fx.ctx.feetX;
         double oz = this.point[2] - fx.ctx.feetZ;
         double ol = Math.max(1.0E-6, Math.sqrt(ox * ox + oz * oz));
         double out = 0.6 + 0.8 * hash(fx.seed, 300 + i);
         s.vx[i] = ox / ol * out + fx.ctx.dirX * 0.4;
         s.vy[i] = 0.4 + 0.8 * hash(fx.seed, 400 + i);
         s.vz[i] = oz / ol * out + fx.ctx.dirZ * 0.4;
         s.spin[i] = (Math.PI * 2) * hash(fx.seed, 500 + i);
         s.tumble[i] = (Math.PI * 2) * hash(fx.seed, 600 + i);
         s.size[i] = (0.045 + 0.03 * hash(fx.seed, 700 + i)) * fx.size;
      }
   }

   private void night(KillFx fx, FxFrame f) {
      float a = fx.alpha() * fx.night * 0.14F * (1.0F - collapsed(fx));
      if (!(a <= 0.003F)) {
         BodyProxy.Part b = fx.rest[1];
         Vfx.sprite(Textures.GLOW, f.rx(b.x + fx.ctx.dirX * 0.3), f.ry(b.y + 0.2), f.rz(b.z + fx.ctx.dirZ * 0.3), 2.4 * fx.size, 0.0F, alpha(-6309633, a));
      }
   }

   @Override
   public String stateForTest(KillFx fx) {
      OvergrowthStyle.State s = (OvergrowthStyle.State)fx.data;
      if (s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.7F
            ? "growing and wrapping"
            : (t < 1.0F ? "blooming" : (t < 1.7F ? "withering" : (this.ending.get() == OvergrowthStyle.Ending.SINK ? "sinking" : "collapsing")));
         return String.format(
            Locale.ROOT,
            "%s; %d vines, %d roses, thorns %d%%, %s, %d petals out; light %.2f, night %.2f%s",
            phase,
            this.vines(fx),
            s.roses,
            this.thornDensity.getInt(),
            this.ending.get().displayName(),
            s.petals,
            fx.light,
            fx.night,
            fx.crit ? ", crit rose" : ""
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

   public static enum Ending implements Displayable {
      WITHER("Wither", "It dries out and collapses into petals"),
      SINK("Sink into the ground", "The thicket draws the body down into the ground");

      private final String displayName;
      private final String description;

      private Ending(String displayName, String description) {
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
      double awayX;
      double awayZ;
      final double[] start = new double[8];
      final double[] delay = new double[8];
      final int[] roseVine = new int[10];
      final double[] roseAt = new double[10];
      final double[] roseSize = new double[10];
      int roses;
      int petals;
      boolean blown;
      final double[] px = new double[48];
      final double[] py = new double[48];
      final double[] pz = new double[48];
      final double[] vx = new double[48];
      final double[] vy = new double[48];
      final double[] vz = new double[48];
      final double[] spin = new double[48];
      final double[] tumble = new double[48];
      final double[] size = new double[48];
      float lastT;

      private State() {
      }
   }
}
