package meowtils.furiahvisuals.module.kill.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.kit.Chains;
import meowtils.furiahvisuals.fx.kit.Clockwork;
import meowtils.furiahvisuals.fx.kit.Surface;
import meowtils.furiahvisuals.fx.pass.FxPass;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.PlayerModel;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.render.material.Finish;
import meowtils.furiahvisuals.render.material.Material;
import meowtils.furiahvisuals.render.material.MeshBatch;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class ChainedStyle extends KillStyleBase {
   private static final float BURST = 0.25F;
   private static final float TAUT = 0.6F;
   private static final float KNELT = 0.9F;
   private static final float OPEN = 1.1F;
   private static final float DRAG = 0.6F;
   private static final float CLOSE = 0.3F;
   private static final float AFTER = 0.2F;
   private static final int MAX_CHAINS = 12;
   private static final int MAX_ROPE = 40;
   private static final int HELIX_LINKS_MAX = 40;
   private static final double LINK = 0.15;
   private static final double WRAP = 0.36;
   private static final double ANCHOR = 0.95;
   private static final double RING = 0.78;
   private static final float RIM = 0.16F;
   public final ModeSetting<Material> material;
   public final ModeSetting<Finish> finish;
   public final ColorSetting tint;
   public final SliderSetting chains;
   public final BooleanSetting kneel;
   public final SliderSetting pullSpeed;
   public final BooleanSetting groundRing;
   public final SliderSetting linkSize;
   private static final double[] ROT = new double[9];
   private static final double[] V = new double[3];
   private final double[] a = new double[3];
   private final double[] b = new double[3];

   public ChainedStyle(KillEffectsModule module) {
      super(
         module,
         KillStyle.CHAINED,
         new ColorSetting("chained.color", "Ring Glow", "The ring's glow and its closing flash.", ColorSetting.Mode.STATIC, -9864, -19641, 1.0)
      );
      this.signature(this.color, -9864);
      this.material = this.materialSetting("chained", Material.GOLD, "What the chains and the ring are made of. Custom takes the Tint, with a finish.");
      this.finish = this.finishSetting("chained", Finish.METALLIC, this.material);
      this.tint = this.setting(
         new ColorSetting("chained.tint", "Tint", "The metal's color with the Custom material.", ColorSetting.Mode.STATIC, -1525686, -6237, 1.0).secondary()
      );
      this.tint.visibleWhen(new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ChainedStyle.this.material.get() == Material.CUSTOM;
         }
      });
      this.signature(this.tint, -1525686);
      this.chains = this.look(this.setting(new SliderSetting("chained.chains", "Chains", "How many chains burst out of the ground.", 2.0, 6.0, 4.0, 1.0)));
      this.kneel = this.setting(new BooleanSetting("chained.kneel", "Kneel", "The chains force the body down to its knees before the drag.", true));
      this.pullSpeed = this.look(
         this.setting(new SliderSetting("chained.pullSpeed", "Pull Speed", "How fast the chains drag the body under.", 50.0, 200.0, 100.0, 5.0).unit("%"))
      );
      this.groundRing = this.setting(
         new BooleanSetting("chained.groundRing", "Ground Ring", "A gilded ring opens on the ground and swallows the body (off: the ground itself).", true)
      );
      this.linkSize = this.look(this.setting(new SliderSetting("chained.linkSize", "Link Size", "How big the links are.", 50.0, 150.0, 100.0, 5.0).unit("%")));
   }

   private float drag() {
      return 60.000004F / this.pullSpeed.getFloat();
   }

   @Override
   public float seconds(KillFx fx) {
      return 1.1F + this.drag() + 0.3F + 0.2F;
   }

   @Override
   public void start(KillFx fx) {
      ChainedStyle.State s = new ChainedStyle.State();
      int n = Math.min(12, Math.round(this.chains.getInt() * (fx.crit ? 2.0F : 1.0F) * (fx.lod ? 0.5F : 1.0F)));
      s.n = Math.max(2, n);
      s.link = 0.15 * fx.size * this.linkSize.getFloat() / 100.0;
      s.floor.y = fx.ctx.groundY;
      s.floor.cx = fx.ctx.feetX;
      s.floor.cz = fx.ctx.feetZ;
      double toYou = Math.atan2(-fx.ctx.dirZ, -fx.ctx.dirX);

      for (int k = 0; k < s.n; k++) {
         double a = toYou + (Math.PI * 2) * k / s.n + (hash(fx.seed, k * 5 + 1) - 0.5) * 0.4;
         double r = 0.95 * fx.size * (0.9 + 0.2 * hash(fx.seed, k * 5 + 2));
         s.ax[k] = fx.ctx.feetX + Math.cos(a) * r;
         s.az[k] = fx.ctx.feetZ + Math.sin(a) * r;
         int rank = Math.min(k, s.n - k);
         s.delay[k] = 0.05 * rank;
         s.height[k] = 0.15 + 0.5 * (k * 0.618 % 1.0);
         s.turn[k] = (k & 1) == 0 ? 1 : -1;
      }

      fx.data = s;
   }

   @Override
   public int passes(KillFx fx) {
      int body = this.material.get().translucent(this.finish.get()) ? FxPass.TRANSLUCENT.bit() : 0;
      return FxPass.SOLID.bit() | body | FxPass.TRANSLUCENT.bit() | (fx.lod ? 0 : FxPass.GLOW.bit());
   }

   private void pose(KillFx fx, double k, double sink) {
      BodyProxy.Part[] p = fx.parts;

      for (int i = 0; i < 6; i++) {
         p[i].copy(fx.rest[i]);
      }

      if (k > 0.0) {
         BodyProxy.Part body = fx.rest[1];
         double lx = body.r[0];
         double ly = body.r[3];
         double lz = body.r[6];
         double dx = body.r[1];
         double dy = body.r[4];
         double dz = body.r[7];

         for (int leg = 4; leg <= 5; leg++) {
            BodyProxy.Part q = p[leg];
            double px = q.x - q.r[1] * 6.0 * 0.05859375;
            double py = q.y - q.r[4] * 6.0 * 0.05859375;
            double pz = q.z - q.r[7] * 6.0 * 0.05859375;
            rotateAbout(q, px, py, pz, lx, ly, lz, Math.toRadians(82.0) * k);
         }

         for (int arm = 2; arm <= 3; arm++) {
            BodyProxy.Part q = p[arm];
            double ay = q.r[4];
            double az = q.r[7];
            double cx = ay * 0.0 - az * -1.0;
            double ax = q.r[1];
            double cy = az * 0.0 - ax * 0.0;
            double cz = ax * -1.0 - ay * 0.0;
            double cl = Math.sqrt(cx * cx + cy * cy + cz * cz);
            if (cl > 1.0E-6) {
               double ang = Math.acos(Math.max(-1.0, Math.min(1.0, -ay))) * k;
               double px = q.x - q.r[1] * 4.0 * 0.05859375;
               double py = q.y - q.r[4] * 4.0 * 0.05859375;
               double pz = q.z - q.r[7] * 4.0 * 0.05859375;
               rotateAbout(q, px, py, pz, cx / cl, cy / cl, cz / cl, ang);
            }
         }

         BodyProxy.Part head = p[0];
         rotateAbout(
            head,
            head.x + head.r[1] * 4.0 * 0.05859375,
            head.y + head.r[4] * 4.0 * 0.05859375,
            head.z + head.r[7] * 4.0 * 0.05859375,
            lx,
            ly,
            lz,
            Math.toRadians(12.0) * k
         );
         double hx = fx.ctx.dirX;
         double hz = fx.ctx.dirZ;
         double hl = Math.sqrt(hx * hx + hz * hz);
         if (hl > 1.0E-6) {
            double var55;
            double kx = var55 = hz / hl;
            double var52;
            double kz = -(var52 = hx / hl);
            double bx = body.x + dx * 6.0 * 0.05859375;
            double by = body.y + dy * 6.0 * 0.05859375;
            double bz = body.z + dz * 6.0 * 0.05859375;

            for (int part = 0; part <= 3; part++) {
               rotateAbout(p[part], bx, by, bz, kx, 0.0, kz, Math.toRadians(14.0) * k);
            }
         }
      }

      double down = 0.5 * k + sink;

      for (int i = 0; i < 6; i++) {
         BodyProxy.Part q = p[i];
         q.y -= down;
         q.clip = true;
         q.cnx = 0.0;
         q.cny = 1.0;
         q.cnz = 0.0;
         q.cd = fx.ctx.groundY + 0.002;
      }
   }

   private static void rotateAbout(BodyProxy.Part q, double px, double py, double pz, double kx, double ky, double kz, double a) {
      double c = Math.cos(a);
      double s = Math.sin(a);
      double t = 1.0 - c;
      ROT[0] = c + kx * kx * t;
      ROT[1] = kx * ky * t - kz * s;
      ROT[2] = kx * kz * t + ky * s;
      ROT[3] = ky * kx * t + kz * s;
      ROT[4] = c + ky * ky * t;
      ROT[5] = ky * kz * t - kx * s;
      ROT[6] = kz * kx * t - ky * s;
      ROT[7] = kz * ky * t + kx * s;
      ROT[8] = c + kz * kz * t;
      double ox = q.x - px;
      double oy = q.y - py;
      double oz = q.z - pz;
      q.x = px + ROT[0] * ox + ROT[1] * oy + ROT[2] * oz;
      q.y = py + ROT[3] * ox + ROT[4] * oy + ROT[5] * oz;
      q.z = pz + ROT[6] * ox + ROT[7] * oy + ROT[8] * oz;
      PlayerModel.mul9(ROT, q.r, q.r);
   }

   @Override
   public void update(KillFx fx, float dt) {
      ChainedStyle.State s = (ChainedStyle.State)fx.data;
      if (s != null) {
         float t = fx.t;
         double k = this.kneel.get() ? Easing.inOutCubic(fx.phase(0.6F, 0.9F)) : 0.0;
         float drag = this.drag();
         double under = Easing.inCubic(fx.phase(1.1F, 1.1F + drag));
         s.sink = under * 2.3;
         this.pose(fx, k, s.sink);
         double r = this.groundRing.get() ? 0.78 * fx.size : 0.55 * fx.size;
         float open = Easing.outBack(fx.phase(0.9F, 1.1F), 1.4F);
         float shut = Easing.inCubic(fx.phase(1.1F + drag, 1.1F + drag + 0.3F));
         s.ring = r * open * (1.0 - shut);
         s.floor.hole = Math.max(0.0, s.ring * 0.88);
         if (t >= 0.6F && !s.physics) {
            this.toPhysics(fx, s);
         }

         if (s.physics) {
            this.ropes(fx, s, dt, under, shut);
         }
      }
   }

   private void wrapStart(KillFx fx, ChainedStyle.State s, int k) {
      this.helix(fx, s, k, 0.0, V);
   }

   private void helix(KillFx fx, ChainedStyle.State s, int k, double u, double[] out) {
      BodyProxy.Part b = fx.parts[1];
      double lx = b.r[0];
      double ly = b.r[3];
      double lz = b.r[6];
      double dx = b.r[1];
      double dy = b.r[4];
      double dz = b.r[7];
      double bx = b.r[2];
      double by = b.r[5];
      double bz = b.r[8];
      double ax = s.ax[k] - b.x;
      double az = s.az[k] - b.z;
      double a0 = Math.atan2(ax * bx + az * bz, ax * lx + az * lz);
      double a = a0 + s.turn[k] * Math.PI * 1.25 * u;
      double rr = 0.36 * fx.size * (1.0 + 0.04 * Math.sin(u * 9.0));
      double h = (s.height[k] - 0.12 * u) * fx.size;
      double ca = Math.cos(a) * rr;
      double sa = Math.sin(a) * rr;
      out[0] = b.x + lx * ca + bx * sa - dx * h;
      out[1] = b.y + ly * ca + by * sa - dy * h;
      out[2] = b.z + lz * ca + bz * sa - dz * h;
   }

   private void run(KillFx fx, ChainedStyle.State s, int k, double u, float t, double[] out) {
      this.wrapStart(fx, s, k);
      double hx = V[0];
      double hy = V[1];
      double hz = V[2];
      double gx = s.ax[k];
      double gy = fx.ctx.groundY;
      double gz = s.az[k];
      double local = Math.max(0.0, t - s.delay[k]);
      double straight = Easing.smoothstep(0.25F, 0.6F, (float)local);
      double mx = (gx + hx) * 0.5;
      double my = Math.max(gy, hy) + 0.6 * fx.size * (1.0 - straight);
      double mz = (gz + hz) * 0.5;
      double w = 1.0 - u;
      double x = w * w * gx + 2.0 * w * u * mx + u * u * hx;
      double y = w * w * gy + 2.0 * w * u * my + u * u * hy;
      double z = w * w * gz + 2.0 * w * u * mz + u * u * hz;
      double wave = 0.18 * fx.size * Math.sin(u * Math.PI * 2.0 - local * 22.0) * Math.exp(-local * 6.0) * Math.sin(u * Math.PI);
      double sx = -(hz - gz);
      double sz = hx - gx;
      double sl = Math.sqrt(sx * sx + sz * sz) + 1.0E-9;
      out[0] = x + sx / sl * wave;
      out[1] = y;
      out[2] = z + sz / sl * wave;
   }

   private void toPhysics(KillFx fx, ChainedStyle.State s) {
      s.physics = true;

      for (int k = 0; k < s.n; k++) {
         this.wrapStart(fx, s, k);
         double dx = V[0] - s.ax[k];
         double dy = V[1] - fx.ctx.groundY;
         double dz = V[2] - s.az[k];
         double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
         double spacing = s.link * 0.72;
         int pts;
         s.points[k] = pts = Math.max(3, Math.min(40, (int)Math.ceil(len * 1.06 / spacing) + 1));
         Chains.Rope r = s.rope[k];
         r.reset(pts, spacing, s.ax[k], fx.ctx.groundY, s.az[k], dx, dy, dz);

         for (int i = 0; i < pts; i++) {
            this.run(fx, s, k, i / (pts - 1.0), 1.6F, V);
            r.place(i, V[0], V[1], V[2]);
         }
      }
   }

   private void ropes(KillFx fx, ChainedStyle.State s, float dt, double under, double shut) {
      for (int k = 0; k < s.n; k++) {
         Chains.Rope r = s.rope[k];
         int last = s.points[k] - 1;
         double toward = Easing.smoothstep(0.1F, 0.9F, (float)under);
         double ex = fx.ctx.feetX + (s.ax[k] - fx.ctx.feetX) * 0.35;
         double ez = fx.ctx.feetZ + (s.az[k] - fx.ctx.feetZ) * 0.35;
         double px = s.ax[k] + (ex - s.ax[k]) * toward;
         double pz = s.az[k] + (ez - s.az[k]) * toward;
         double py = fx.ctx.groundY - 1.2 * Math.max(0.0, shut) - 0.6 * Easing.smoothstep(0.7F, 1.0F, (float)under);
         r.pin(0, true, px, py, pz);
         this.wrapStart(fx, s, k);
         r.pin(last, true, V[0], V[1], V[2]);
         r.step(dt, s.floor);
      }
   }

   @Override
   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      ChainedStyle.State s = (ChainedStyle.State)fx.data;
      if (s != null) {
         float alpha = fx.alpha();
         if (pass == FxPass.GLOW) {
            this.glow(fx, f, s, alpha);
         } else {
            boolean glass = this.material.get().translucent(this.finish.get());
            if (pass == FxPass.TRANSLUCENT) {
               this.hole(fx, f, s, alpha);
               if (!glass) {
                  return;
               }
            }

            if (pass == FxPass.SOLID) {
               this.body(fx, f);
               if (glass) {
                  return;
               }
            }

            boolean custom = this.material.get() == Material.CUSTOM;
            int col = custom ? this.tint.getColor(0.5F) : -1;
            int metal = Math.round(255.0F * alpha * (custom ? (col >>> 24) / 255.0F : 1.0F)) << 24 | col & 16777215;
            float glint = 0.06F + 0.8F * (float)Math.exp(-Math.abs(fx.t - 0.55F) * 25.0);
            MeshBatch.begin(this.material.get(), this.finish.get(), fx.light, 0.16F, -1, true, pass == FxPass.SOLID);

            for (int k = 0; k < s.n; k++) {
               this.chain(fx, f, s, k, metal, glint);
            }

            if (s.ring > 0.01) {
               double cx = f.rx(fx.ctx.feetX);
               double cy = f.ry(fx.ctx.groundY + 0.025);
               double cz = f.rz(fx.ctx.feetZ);
               Clockwork.ring(cx, cy, cz, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, s.ring * 0.92, s.ring * 1.06, 0.05 * fx.size, 40, metal, 0.12F);
               if (fx.crit) {
                  Clockwork.ring(
                     cx, cy - 0.005, cz, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, s.ring * 1.08, s.ring * 1.26, 0.035 * fx.size, 48, metal, 0.12F
                  );
               }
            }

            MeshBatch.end();
            if (fx.crit && s.ring > 0.01) {
               double cx = f.rx(fx.ctx.feetX);
               double cy = f.ry(fx.ctx.groundY + 0.025 + 0.02);
               double cz = f.rz(fx.ctx.feetZ);
               RenderState.decal(true);
               Clockwork.ticks(
                  cx, cy, cz, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.001, 0.0, s.ring * 1.17, s.ring * 0.14, fx.t * 3.0, alpha(-14018040, 0.8F * alpha), null
               );
               RenderState.decal(false);
            }
         }
      }
   }

   private void body(KillFx fx, FxFrame f) {
      if (!(((ChainedStyle.State)fx.data).sink > 2.25)) {
         fx.drawBody(f);
         RenderState.translucent(true);
      }
   }

   private void chain(KillFx fx, FxFrame f, ChainedStyle.State s, int k, int col, float glint) {
      float local = (float)(fx.t - s.delay[k]);
      if (!(local <= 0.0F)) {
         double ground = fx.ctx.groundY;
         double spacing = s.link * 0.72;
         if (!s.physics) {
            double grown = Easing.outCubic(Easing.clamp01(local / 0.25F));
            this.run(fx, s, k, 0.0, fx.t, this.a);
            int steps = Math.max(2, (int)Math.ceil(this.runLength(fx, s, k) / spacing));

            double u2;
            for (int i = 1; i <= steps && !((u2 = (double)i / steps) > grown); i++) {
               this.run(fx, s, k, u2, fx.t, this.b);
               link(f, this.a, this.b, i, s.link, col, glint, ground);
               this.a[0] = this.b[0];
               this.a[1] = this.b[1];
               this.a[2] = this.b[2];
            }

            if (grown < 0.999) {
               return;
            }
         } else {
            Chains.Rope r = s.rope[k];

            for (int i = 1; i < s.points[k]; i++) {
               this.a[0] = r.x(i - 1);
               this.a[1] = r.y(i - 1);
               this.a[2] = r.z(i - 1);
               this.b[0] = r.x(i);
               this.b[1] = r.y(i);
               this.b[2] = r.z(i);
               link(f, this.a, this.b, i, s.link, col, glint, ground);
            }
         }

         float wrapped;
         if (!((wrapped = Easing.outCubic(Easing.clamp01((local - 0.25F) / 0.3F))) <= 0.0F)) {
            int links = Math.min(40, Math.max(4, (int)Math.ceil(0.36 * fx.size * Math.PI * 1.25 / spacing)));
            this.helix(fx, s, k, 0.0, this.a);

            double u;
            for (int i = 1; i <= links && !((u = (double)i / links) > wrapped); i++) {
               this.helix(fx, s, k, u, this.b);
               link(f, this.a, this.b, i + 1, s.link, col, glint, ground);
               this.a[0] = this.b[0];
               this.a[1] = this.b[1];
               this.a[2] = this.b[2];
            }
         }
      }
   }

   private double runLength(KillFx fx, ChainedStyle.State s, int k) {
      this.wrapStart(fx, s, k);
      double dx = V[0] - s.ax[k];
      double dy = V[1] - fx.ctx.groundY;
      double dz = V[2] - s.az[k];
      return Math.sqrt(dx * dx + dy * dy + dz * dz) * 1.15;
   }

   private static void link(FxFrame f, double[] p, double[] q, int i, double size, int col, float glint, double ground) {
      if (!(p[1] < ground - 0.01) || !(q[1] < ground - 0.01)) {
         Chains.linkBetween(f.rx(p[0]), f.ry(p[1]), f.rz(p[2]), f.rx(q[0]), f.ry(q[1]), f.rz(q[2]), 0.0, 1.0, 0.0, (i & 1) == 1, size, col, glint);
      }
   }

   private void hole(KillFx fx, FxFrame f, ChainedStyle.State s, float alpha) {
      if (!(s.ring <= 0.01) && this.groundRing.get()) {
         double cx = f.rx(fx.ctx.feetX);
         double cy = f.ry(fx.ctx.groundY + 0.012);
         double cz = f.rz(fx.ctx.feetZ);
         int dark = alpha(-16448766, alpha);
         RenderState.decal(true);

         for (int pass = 0; pass < 2; pass++) {
            double h = s.ring * (pass == 0 ? 1.15 : 0.8);
            Vfx.quadUV(
               Textures.GLOW,
               cx - h,
               cy,
               cz - h,
               0.0,
               0.0,
               dark,
               cx + h,
               cy,
               cz - h,
               1.0,
               0.0,
               dark,
               cx + h,
               cy,
               cz + h,
               1.0,
               1.0,
               dark,
               cx - h,
               cy,
               cz + h,
               0.0,
               1.0,
               dark
            );
         }

         RenderState.decal(false);
      }
   }

   private void glow(KillFx fx, FxFrame f, ChainedStyle.State s, float alpha) {
      int col = this.color.getColor(0.5F);
      boolean calm = FuriahsVisuals.instance.isReduceFlashing();
      float taut = (float)Math.exp(-Math.abs(fx.t - 0.55F) * 18.0);
      if (taut > 0.02F) {
         for (int k = 0; k < s.n; k++) {
            this.wrapStart(fx, s, k);
            Vfx.sprite(Textures.SPARK, f.rx(V[0]), f.ry(V[1]), f.rz(V[2]), 0.35 * fx.size, fx.t * 4.0F + k, core(col, (calm ? 0.35F : 0.8F) * taut * alpha));
         }
      }

      double cx = f.rx(fx.ctx.feetX);
      double cy = f.ry(fx.ctx.groundY + 0.06);
      double cz = f.rz(fx.ctx.feetZ);
      if (s.ring > 0.01) {
         Vfx.sprite(Textures.GLOW, cx, cy, cz, s.ring * 2.6, 0.0F, alpha(col, 0.35F * alpha));
      }

      float flash;
      if ((flash = Easing.clamp01(1.0F - Math.abs(fx.t - (1.1F + this.drag() + 0.3F)) / 0.18F)) > 0.0F) {
         double size = (calm ? 1.2 : 2.4) * fx.size;
         Vfx.sprite(Textures.GLOW, cx, cy + 0.1, cz, size, 0.0F, alpha(col, (calm ? 0.3F : 0.75F) * flash * alpha));
         if (!calm) {
            Vfx.sprite(Textures.FLARE, cx, cy + 0.1, cz, size * 0.8, 0.4F, core(col, 0.8F * flash * alpha));
         }
      }
   }

   @Override
   public String stateForTest(KillFx fx) {
      ChainedStyle.State s = (ChainedStyle.State)fx.data;
      if (s == null) {
         return null;
      } else {
         float t = fx.t;
         float drag = this.drag();
         String phase = t < 0.25F
            ? "bursting out"
            : (
               t < 0.6F
                  ? "wrapping"
                  : (
                     t < 0.9F
                        ? "kneeling"
                        : (t < 1.1F ? "ring opening" : (t < 1.1F + drag ? "dragged under" : (t < 1.1F + drag + 0.3F ? "ring closing" : "gone")))
                  )
            );
         double stretch = 0.0;
         if (s.physics) {
            for (int k = 0; k < s.n; k++) {
               stretch = Math.max(stretch, s.rope[k].stretchForTest());
            }
         }

         return String.format(
            Locale.ROOT,
            "%s, %s; %d chains (link %.3f), %s, ring %.2f, sunk %.2f%s",
            this.material.get().displayName(),
            phase,
            s.n,
            s.link,
            s.physics ? String.format(Locale.ROOT, "ropes (stretch %.4f)", stretch) : "whipping",
            s.ring,
            s.sink,
            this.kneel.get() ? "" : ", no kneel"
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

   private static final class Floor implements Surface {
      double y;
      double cx;
      double cz;
      double hole;

      private Floor() {
      }

      @Override
      public double at(double x, double yy, double z) {
         double dx = x - this.cx;
         double dz = z - this.cz;
         return dx * dx + dz * dz < this.hole * this.hole ? Double.NaN : this.y;
      }
   }

   private static final class State {
      int n;
      final double[] ax = new double[12];
      final double[] az = new double[12];
      final double[] delay = new double[12];
      final double[] height = new double[12];
      final double[] start = new double[12];
      final int[] turn = new int[12];
      final Chains.Rope[] rope = new Chains.Rope[12];
      final int[] points = new int[12];
      boolean physics;
      final ChainedStyle.Floor floor = new ChainedStyle.Floor();
      double link;
      double sink;
      double ring;

      State() {
         for (int i = 0; i < 12; i++) {
            this.rope[i] = new Chains.Rope();
         }
      }
   }
}
