package meowtils.furiahvisuals.module.kill.styles;

import java.util.Locale;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.fx.Breeze;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.Ground;
import meowtils.furiahvisuals.fx.Terrain;
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
import meowtils.furiahvisuals.render.SkinSampler;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.render.look.BodyLook;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.Minecraft;

public final class ConsumedStyle extends KillStyleBase {
   private static final float RISEN = 0.4F;
   private static final float CHARRED = 0.8F;
   private static final float CRUMBLE = 0.7F;
   private static final float MIST = 0.5F;
   private static final int SWIRL = 30;
   private static final int BITS = 26;
   private static final int POOL = 56;
   private static final float NOISE = 0.5F;
   private static final float EDGE = 0.1F;
   private static final float SMOKE_WIND = 0.4F;
   private static final float EMBER_WIND = 0.5F;
   public final ColorSetting embers;
   public final ColorSetting edge;
   public final SliderSetting density;
   public final SliderSetting emberAmount;
   public final SliderSetting crumbleSpeed;
   public final BooleanSetting groundMist;
   public final ModeSetting<Smoke.EdgeLight> edgeLight;
   private static final float[] SX = new float[26];
   private static final float[] SY = new float[26];
   private static final float[] SZ = new float[26];
   private static final int[] SC = new int[26];
   private static final double[] FLOW = new double[3];

   public ConsumedStyle(KillEffectsModule module) {
      super(
         module,
         KillStyle.CONSUMED,
         new ColorSetting("consumed.color", "Smoke", "The smoke, the mist and the charred body.", ColorSetting.Mode.STATIC, -15593964, -14017490, 1.0)
      );
      this.signature(this.color, -15593964);
      this.embers = this.setting(
         new ColorSetting(
               "consumed.embers",
               "Embers",
               "The cracks glowing across the body, the crumbling edge and the sparks.",
               ColorSetting.Mode.STATIC,
               -45778,
               -19641,
               1.0
            )
            .secondary()
      );
      this.signature(this.embers, -45778);
      this.edge = this.setting(
         new ColorSetting("consumed.edge", "Edge Light", "The thin light round the smoke.", ColorSetting.Mode.STATIC, -4124129, -50373, 1.0).secondary()
      );
      this.signature(this.edge, -4124129);
      this.density = this.look(this.setting(new SliderSetting("consumed.density", "Smoke Density", "How thick the smoke is.", 0.0, 100.0, 60.0, 5.0).unit("%")));
      this.emberAmount = this.look(
         this.setting(
            new SliderSetting("consumed.emberAmount", "Embers", "How bright the ember cracks are, and how many sparks fly.", 0.0, 100.0, 50.0, 5.0).unit("%")
         )
      );
      this.crumbleSpeed = this.look(
         this.setting(new SliderSetting("consumed.crumbleSpeed", "Crumble Speed", "How fast the body crumbles away.", 50.0, 200.0, 100.0, 5.0).unit("%"))
      );
      this.groundMist = this.setting(new BooleanSetting("consumed.groundMist", "Ground Mist", "Dark mist pools on the ground, then thins away.", true));
      this.edgeLight = this.setting(
         new ModeSetting<>(
               "consumed.edgeLight",
               "Edge Light",
               "A thin red light round the smoke: where it is dark (Auto), always, or never.",
               Smoke.EdgeLight.class,
               Smoke.EdgeLight.AUTO
            )
            .style(ModeSetting.Style.CHIPS)
      );
   }

   private float crumble() {
      return 70.0F / this.crumbleSpeed.getFloat();
   }

   @Override
   public float seconds(KillFx fx) {
      return 0.8F + this.crumble() + 0.5F;
   }

   @Override
   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit();
   }

   @Override
   public void start(KillFx fx) {
      ConsumedStyle.State s = new ConsumedStyle.State();
      float d = this.density.getFloat() / 100.0F;
      s.swirl = Math.min(30, Math.max(4, Math.round(30.0F * (0.35F + 0.65F * d) * (fx.lod ? 0.5F : 1.0F))));

      for (int i = 0; i < s.swirl; i++) {
         s.born[i] = (float)(0.28 * hash(fx.seed, i * 4 + 1));
         s.angle[i] = (float)((Math.PI * 2) * hash(fx.seed, i * 4 + 2));
         s.radius[i] = (float)(0.42 + 0.25 * hash(fx.seed, i * 4 + 3));
         s.climb[i] = (float)(2.0 + 1.2 * hash(fx.seed, i * 4 + 4));
      }

      int wanted = Math.min(26, fx.count(20.800001F, false));
      int per = Math.max(1, wanted / 6);
      int n = 0;

      for (int part = 0; part < 6 && n < 26; part++) {
         int got = SkinSampler.sample(fx.ctx.body, part, Math.min(per + (part == 1 ? 2 : 0), 26 - n), fx.rng, SX, SY, SZ, SC, n, -16777216);
         BodyProxy.Part q = fx.rest[part];

         for (int i = n; i < n + got; i++) {
            s.bx[i] = q.x + q.r[0] * SX[i] + q.r[1] * SY[i] + q.r[2] * SZ[i];
            s.by[i] = q.y + q.r[3] * SX[i] + q.r[4] * SY[i] + q.r[5] * SZ[i];
            s.bz[i] = q.z + q.r[6] * SX[i] + q.r[7] * SY[i] + q.r[8] * SZ[i];
            float sweep = Easing.clamp01((fx.ctx.feetY + 2.0 - s.by[i]) / 2.0);
            float noise = DissolveShader.noiseAt(s.bx[i] - fx.ctx.feetX, s.by[i] - fx.ctx.feetY, s.bz[i] - fx.ctx.feetZ);
            s.at[i] = DissolveShader.thresholdAt(sweep, noise, 0.5F, 0.1F);
         }

         n += got;
      }

      s.bits = n;
      s.ground = fx.preview
         ? Terrain.HeightField.flat(fx.ctx.groundY)
         : Terrain.HeightField.sample(Minecraft.getMinecraft().theWorld, fx.ctx.feetX, fx.ctx.groundY, fx.ctx.feetZ, 1.6, 2);
      Breeze.at(fx.ctx.feetX, fx.ctx.feetY + 1.0, fx.ctx.feetZ, fx.preview, s.wind);
      int k = 0;

      while (k < 3) {
         int n2 = k++;
         s.wind[n2] = s.wind[n2] * 0.4F;
      }

      fx.data = s;
   }

   private float crumbled(KillFx fx) {
      return fx.phase(0.8F, 0.8F + this.crumble());
   }

   private static float flare(KillFx fx) {
      return fx.crit ? (float)Math.exp(-Math.abs(fx.t - 0.75F) * 14.0) : 0.0F;
   }

   @Override
   public void update(KillFx fx, float dt) {
      ConsumedStyle.State s = (ConsumedStyle.State)fx.data;
      if (s != null && !fx.lod) {
         float c = this.crumbled(fx);
         float amount = this.emberAmount.getFloat() / 100.0F;
         if (!(c <= 0.0F) && !(amount <= 0.0F)) {
            int col = this.embers.getColor(0.5F);

            for (int i = 0; i < s.bits; i++) {
               if (!s.sparked[i] && !(c < s.at[i])) {
                  s.sparked[i] = true;
                  int n = 1 + Math.round(2.0F * amount);

                  for (int k = 0; k < n; k++) {
                     float vx = (float)(fx.ctx.dirX * 0.4) + fx.rng.signed() * 0.5F;
                     float vz = (float)(fx.ctx.dirZ * 0.4) + fx.rng.signed() * 0.5F;
                     fx.particles
                        .spawn(
                           7,
                           s.bx[i],
                           s.by[i],
                           s.bz[i],
                           vx,
                           0.8F + fx.rng.next() * 1.2F,
                           vz,
                           0.7F + fx.rng.next() * 0.6F,
                           (0.035F + fx.rng.next() * 0.03F) * fx.size
                        )
                        .gravity(-0.6F)
                        .drag(1.6F)
                        .color(col)
                        .alpha(fx.alpha())
                        .end(0.005F)
                        .wind(0.5F);
                  }
               }
            }
         }
      }
   }

   @Override
   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      ConsumedStyle.State s = (ConsumedStyle.State)fx.data;
      if (s != null) {
         float alpha = fx.alpha();
         if (pass == FxPass.SOLID) {
            this.body(fx, f);
         } else if (pass == FxPass.TRANSLUCENT) {
            Camera cam = Vfx.camera();
            if (this.groundMist.get()) {
               this.mist(fx, f, s, alpha);
            }

            this.smoke(fx, f, s, cam, alpha);
         } else if (pass == FxPass.GLOW) {
            this.light(fx, f, s, alpha);
         }
      }
   }

   private void body(KillFx fx, FxFrame f) {
      float c = this.crumbled(fx);
      if (!(c >= 1.0F)) {
         BodyProxy.Look look = fx.look;
         float t = fx.t;
         int ember = this.embers.getColor(0.5F) | 0xFF000000;
         float flare = flare(fx);
         look.emberColor = ColorUtil.lerp(ember, ColorUtil.core(ember), 0.85F * flare);
         look.embers = Math.min(1.0F, this.emberAmount.getFloat() / 100.0F * 1.4F + 1.6F * flare);
         look.worldLight = fx.light;
         if (t < 0.4F) {
            look.kind = BodyLook.SKIN;
            look.sweepTo = null;
         } else if (t < 0.8F) {
            look.kind = BodyLook.SKIN;
            look.sweepTo = BodyLook.SHADOW;
            look.sweep = Easing.inOutCubic(fx.phase(0.4F, 0.8F));
            look.sweepMode = 0;
            look.sweepOX = fx.ctx.feetX;
            look.sweepOY = fx.ctx.feetY;
            look.sweepOZ = fx.ctx.feetZ;
            look.sweepSpan = 1.95F;
            look.sweepEdgeColor = ember;
         } else {
            look.kind = BodyLook.SHADOW;
            look.sweepTo = null;
         }

         if (c > 0.0F) {
            look.dissolve = c;
            look.dissolveNoise = 0.5F;
            look.sweepX = 0.0;
            look.sweepY = -1.0;
            look.sweepZ = 0.0;
            look.sweepFrom = -(fx.ctx.feetY + 2.0);
            look.sweepLength = 2.0;
            look.edgeColor = ember;
            look.edgeWidth = 0.1F;
         } else {
            look.dissolve = -1.0F;
         }

         fx.drawBody(f);
         RenderState.translucent(true);
      }
   }

   private void smoke(KillFx fx, FxFrame f, ConsumedStyle.State s, Camera cam, float alpha) {
      Smoke.Puffs puffs = s.puffs;
      puffs.count = 0;
      float t = fx.t;
      float end = this.seconds(fx);
      float fadeAll = 1.0F - Easing.smoothstep(end - 0.5F, end, t);
      double bx = f.rx(fx.ctx.feetX);
      double bz = f.rz(fx.ctx.feetZ);
      double tx = cam.posX - bx;
      double tz = cam.posZ - bz;
      double tl = Math.max(1.0E-4, Math.sqrt(tx * tx + tz * tz));
      tx /= tl;
      tz /= tl;
      double[] flow = FLOW;

      for (int i = 0; i < s.swirl; i++) {
         float age = t - s.born[i];
         if (!(age <= 0.0F)) {
            double a = s.angle[i] + age * 2.6;
            double rise = Math.min(2.4, s.climb[i] * age * (1.0 - 0.2 * age));
            double r = s.radius[i] * fx.size * (1.0 + 0.25 * age);
            Smoke.curl(fx.ctx.feetX + Math.cos(a) * r, fx.ctx.feetY + rise, fx.ctx.feetZ + Math.sin(a) * r, age, 1.4, flow);
            double drift = Math.max(0.0, (double)(t - 0.8F)) * 0.5;
            double x = fx.ctx.feetX + Math.cos(a) * r + flow[0] * 0.15 + fx.ctx.dirX * drift + s.wind[0] * age;
            double y = fx.ctx.feetY + rise + flow[1] * 0.1 + s.wind[1] * age;
            double z = fx.ctx.feetZ + Math.sin(a) * r + flow[2] * 0.15 + fx.ctx.dirZ * drift + s.wind[2] * age;
            float k = Easing.smoothstep(0.0F, 0.12F, age) * (1.0F - Easing.smoothstep(1.3F, 1.9F, age));
            add(puffs, f, cam, x, y, z, (float)((0.6 + 0.4 * age) * fx.size), 0.52F * k * fadeAll, (float)(a * 0.5), i, bx, bz, tx, tz);
         }
      }

      float span = this.crumble();

      for (int ix = 0; ix < s.bits; ix++) {
         float age = t - (0.8F + s.at[ix] * span);
         if (!(age <= 0.0F)) {
            Smoke.curl(s.bx[ix], s.by[ix] + age * 0.4, s.bz[ix], age, 1.7, flow);
            double x = s.bx[ix] + fx.ctx.dirX * 0.45 * age + flow[0] * 0.25 * age + s.wind[0] * age;
            double y = s.by[ix] + 0.7 * age + 0.2 * age * age + s.wind[1] * age;
            double z = s.bz[ix] + fx.ctx.dirZ * 0.45 * age + flow[2] * 0.25 * age + s.wind[2] * age;
            float k = Easing.smoothstep(0.0F, 0.1F, age) * (1.0F - Easing.smoothstep(0.4F, 1.1F, age));
            add(puffs, f, cam, x, y, z, (float)((0.35 + 0.6 * age) * fx.size), 0.5F * k, (float)(age * 1.3 + ix), ix, bx, bz, tx, tz);
         }
      }

      if (puffs.count != 0) {
         puffs.sort(cam);
         puffs.draw(this.color.getColor(0.5F), alpha, null, cam, bx, bz, 1.0);
      }
   }

   private static void add(
      Smoke.Puffs puffs,
      FxFrame f,
      Camera cam,
      double x,
      double y,
      double z,
      float size,
      float a,
      float turn,
      int cell,
      double bx,
      double bz,
      double tx,
      double tz
   ) {
      if (!(a <= 0.004F) && puffs.count < puffs.capacity()) {
         int j = puffs.count++;
         puffs.x[j] = f.rx(x);
         puffs.y[j] = f.ry(y);
         puffs.z[j] = f.rz(z);
         puffs.size[j] = size;
         double front = Easing.clamp01(((puffs.x[j] - bx) * tx + (puffs.z[j] - bz) * tz) / 0.6);
         puffs.alpha[j] = NearCamera.dark(cam, puffs.x[j], puffs.y[j], puffs.z[j], a * (1.0F - 0.35F * (float)front));
         puffs.turn[j] = turn;
         puffs.cell[j] = cell % 4;
      }
   }

   private void mist(KillFx fx, FxFrame f, ConsumedStyle.State s, float alpha) {
      float t = fx.t;
      float end = this.seconds(fx);
      float k = Easing.smoothstep(0.8F, 1.2F, t) * (1.0F - Easing.smoothstep(end - 0.5F, end, t));
      if (!(k <= 0.003F)) {
         int col = this.color.getColor(0.5F);
         int tex = Smoke.sheet();
         RenderState.decal(true);

         try {
            for (int i = 0; i < 4; i++) {
               double a = (Math.PI * 2) * i / 4.0 + fx.seed;
               double r = 0.35 * fx.size;
               double half = (0.55 + 0.25 * k) * fx.size;
               Ground.decal(
                  f,
                  s.ground,
                  tex,
                  i / 4.0F,
                  0.0F,
                  (i + 1) / 4.0F,
                  1.0F,
                  fx.ctx.feetX + Math.cos(a) * r,
                  fx.ctx.feetZ + Math.sin(a) * r,
                  half,
                  a + t * 0.3,
                  Smoke.alphaOf(col, 0.4F * k * alpha),
                  4
               );
            }
         } finally {
            RenderState.decal(false);
         }
      }
   }

   private void light(KillFx fx, FxFrame f, ConsumedStyle.State s, float alpha) {
      int ember = this.embers.getColor(0.5F);
      boolean calm = FuriahsVisuals.instance.isReduceFlashing();
      float flare = flare(fx);
      if (flare > 0.02F) {
         Camera cam = Vfx.camera();
         BodyProxy.Part b = fx.parts[1];
         float k = (calm ? 0.25F : 0.7F) * flare * alpha;
         double bx = f.rx(b.x);
         double by = f.ry(b.y + 0.2);
         double bz = f.rz(b.z);
         double tx = cam.posX - bx;
         double ty = cam.posY - by;
         double tz = cam.posZ - bz;
         double tl = Math.max(0.001, Math.sqrt(tx * tx + ty * ty + tz * tz));
         double pull = Math.min(0.45, tl * 0.5) / tl;
         Vfx.sprite(Textures.GLOW, bx + tx * pull, by + ty * pull, bz + tz * pull, 2.4 * fx.size, 0.0F, alpha(ember, k));

         for (int i = 0; i < 6; i++) {
            BodyProxy.Part p = fx.parts[i];
            Vfx.sprite(Textures.GLOW, f.rx(p.x) + tx * pull, f.ry(p.y) + ty * pull, f.rz(p.z) + tz * pull, 0.9 * fx.size, 0.0F, core(ember, 0.6F * k));
         }
      }

      Smoke.EdgeLight mode;
      float on = (mode = this.edgeLight.get()) == Smoke.EdgeLight.ON ? 1.0F : (mode == Smoke.EdgeLight.AUTO ? fx.night : 0.0F);
      if (!(on <= 0.01F)) {
         int col = this.edge.getColor(0.5F);
         Smoke.Puffs puffs = s.puffs;

         for (int i = 0; i < puffs.count; i += 2) {
            int k = puffs.order(i);
            Vfx.sprite(Textures.GLOW, puffs.x[k], puffs.y[k], puffs.z[k], puffs.size[k] * 1.1, 0.0F, alpha(col, 0.12F * on * puffs.alpha[k] * alpha));
         }
      }
   }

   @Override
   public String stateForTest(KillFx fx) {
      ConsumedStyle.State s = (ConsumedStyle.State)fx.data;
      if (s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.4F ? "the smoke rises" : (t < 0.8F ? "charring" : (this.crumbled(fx) < 1.0F ? "crumbling" : "the mist thins"));
         int sparked = 0;

         for (int i = 0; i < s.bits; i++) {
            sparked += s.sparked[i] ? 1 : 0;
         }

         return String.format(
            Locale.ROOT,
            "%s; %d swirl puffs, %d of %d places on the skin gone, %d puffs drawn, crumbled %.2f (%s), embers %.2f%s; light %.2f, night %.2f",
            phase,
            s.swirl,
            sparked,
            s.bits,
            s.puffs.count,
            this.crumbled(fx),
            DissolveShader.available() ? "shader" : "fade",
            fx.look.embers,
            flare(fx) > 0.05F ? " (crit flare)" : "",
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
      final Smoke.Puffs puffs = new Smoke.Puffs(56);
      int swirl;
      int bits;
      final float[] born = new float[30];
      final float[] angle = new float[30];
      final float[] radius = new float[30];
      final float[] climb = new float[30];
      final double[] bx = new double[26];
      final double[] by = new double[26];
      final double[] bz = new double[26];
      final float[] at = new float[26];
      final boolean[] sparked = new boolean[26];
      Terrain.HeightField ground;
      final float[] wind = new float[3];

      private State() {
      }
   }
}
