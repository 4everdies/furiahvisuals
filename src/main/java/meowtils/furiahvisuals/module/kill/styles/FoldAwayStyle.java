package meowtils.furiahvisuals.module.kill.styles;

import java.util.Locale;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.kit.Paper;
import meowtils.furiahvisuals.fx.pass.FxPass;
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
import meowtils.furiahvisuals.render.look.LineArt;
import meowtils.furiahvisuals.render.look.Portrait;
import meowtils.furiahvisuals.render.material.Finish;
import meowtils.furiahvisuals.render.material.Material;
import meowtils.furiahvisuals.render.material.MeshBatch;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.renderer.GlStateManager;

public final class FoldAwayStyle extends KillStyleBase {
   private static final float PAPERED = 0.3F;
   private static final float FLAT = 0.5F;
   private static final float FOLD = 0.8F;
   private static final float FLY = 0.9F;
   private static final float DROP = 0.6F;
   private static final float SHEET_FROM = 0.36F;
   private static final float SHEET_TO = 0.48F;
   private static final float BODY_GONE_FROM = 0.42F;
   private static final double[] SHEET = new double[]{1.05, 1.21, 1.15, 1.25};
   private static final double[] MIDDLE = new double[]{1.05, 1.24, 1.0, 1.28};
   private static final double[] FIGURE_Y = new double[]{0.0, -0.2, 0.0, 0.0};
   private static final double[] REST = new double[]{0.3, 0.28, 0.24, 0.2};
   private static final Paper.Shape[] SHAPES = new Paper.Shape[]{Paper.CRANE, Paper.PLANE, Paper.STAR, Paper.BOAT};
   private static final int[][] STEPS = new int[][]{
      {0, 0, 0, 0, 0, 1, 1, 3, 3, 1, 1, 4, 0, 0, 5, 5, 2, 2}, {0, 0, 0, 2, 3, 1}, {5, 5, 0, 1, 2, 3, 4, 5, 5, 5, 5, 5}, {3, 3, 2, 2, 0, 1, 4, 4, 4}
   };
   private static final int[] STEP_COUNT = new int[]{6, 4, 6, 5};
   private static final double FIG = 0.9;
   private static final double FOLDED = 0.7;
   private static final double ARC = 0.35;
   private static final double AWAY = 4.5;
   private static final double RISE = 2.6;
   private static final int FRONT_PAPER = -988191;
   private static final float PAPER_WIND = 0.6F;
   private static final double BODY_OUT = 0.012;
   private static final float RIM = 0.06F;
   public final ModeSetting<FoldAwayStyle.FinalShape> shape;
   public final BooleanSetting flyAway;
   public final BooleanSetting keepSkin;
   public final BooleanSetting paperTexture;
   public final SliderSetting foldSpeed;
   private final Paper.Pose pose = new Paper.Pose();
   private final float[] fold = new float[32];
   private final float[] lift = new float[32];
   private final double[] map = new double[6];
   private final double[] at = new double[3];
   private final float[] hsb = new float[3];

   public FoldAwayStyle(KillEffectsModule module) {
      super(
         module,
         KillStyle.FOLD_AWAY,
         new ColorSetting(
            "foldaway.color", "Paper", "The paper's back, and both its sides with Keep Skin Colors off.", ColorSetting.Mode.STATIC, -19294, -10587, 1.0
         )
      );
      this.signature(this.color, -19294);
      this.shape = this.setting(
         new ModeSetting<>("foldaway.shape", "Final Shape", "What the sheet folds into.", FoldAwayStyle.FinalShape.class, FoldAwayStyle.FinalShape.CRANE)
            .style(ModeSetting.Style.CHIPS)
      );
      this.flyAway = this.setting(
         new BooleanSetting(
            "foldaway.flyAway", "Fly Away", "It flies off into the sky. Off: it drops, settles on the ground and stays like any body (Linger, Body End).", true
         )
      );
      this.keepSkin = this.setting(
         new BooleanSetting("foldaway.keepSkin", "Keep Skin Colors", "The print in the skin's colors on white paper. Off: all in the paper's color.", true)
      );
      this.paperTexture = this.setting(new BooleanSetting("foldaway.texture", "Paper Texture", "The paper's fibers.", true));
      this.foldSpeed = this.look(this.setting(new SliderSetting("foldaway.foldSpeed", "Fold Speed", "How fast it folds.", 50.0, 200.0, 100.0, 5.0).unit("%")));
   }

   @Override
   public float seconds(KillFx fx) {
      return 0.5F + 0.8F / Math.max(0.5F, this.foldSpeed.getFloat() / 100.0F) + (this.flyAway.get() ? 0.9F : 0.6F);
   }

   @Override
   public boolean leavesBody() {
      return !this.flyAway.get();
   }

   @Override
   public int passes(KillFx fx) {
      return FxPass.SOLID.bit();
   }

   @Override
   public void start(KillFx fx) {
      FoldAwayStyle.State s = new FoldAwayStyle.State();
      aim(s, -fx.ctx.dirX, -fx.ctx.dirZ);
      s.kind = this.shape.get().ordinal();
      s.shape = SHAPES[s.kind];
      s.folded = 0.5F + 0.8F / Math.max(0.5F, this.foldSpeed.getFloat() / 100.0F);
      s.fly = this.flyAway.get();
      fx.data = s;
   }

   private static void aim(FoldAwayStyle.State s, double dx, double dz) {
      double l = Math.sqrt(dx * dx + dz * dz);
      if (l < 1.0E-4) {
         if (s.nX == 0.0 && s.nZ == 0.0) {
            s.nZ = 1.0;
            s.rX = 1.0;
         }
      } else {
         s.nX = dx / l;
         s.rX = s.nZ = dz / l;
         s.rZ = -s.nX;
      }
   }

   private static void capture(KillFx fx, FoldAwayStyle.State s) {
      s.tried = true;
      if (Portrait.available()) {
         long t0 = System.nanoTime();
         BodyProxy.Look look = new BodyProxy.Look();
         look.reset();
         look.armor = fx.look.armor;
         look.heldItem = fx.look.heldItem;
         look.cape = fx.look.cape;
         float yaw = (float)Math.toDegrees(Math.atan2(-s.nX, s.nZ));
         s.picture = Portrait.capture(fx.ctx.body, fx.rest, look, fx.ctx.feetX, fx.ctx.feetY, fx.ctx.feetZ, yaw);
         s.pictureMillis = (float)(System.nanoTime() - t0) / 1000000.0F;
         RenderState.translucent(true);
      }
   }

   private static float folding(KillFx fx, FoldAwayStyle.State s) {
      return fx.phase(0.5F, s.folded);
   }

   private static float after(KillFx fx, FoldAwayStyle.State s) {
      return fx.phase(s.folded, s.folded + (s.fly ? 0.9F : 0.6F));
   }

   private void folds(FoldAwayStyle.State s, float progress) {
      int n = STEP_COUNT[s.kind];
      int[] steps = STEPS[s.kind];
      int points = s.shape.points();

      for (int q = 0; q < points; q++) {
         float local = Easing.clamp01((progress * n - steps[q]) / 0.78F);
         this.fold[q] = Easing.inOutCubic(local);
         this.lift[q] = 0.0F;
      }

      if (s.shape == Paper.STAR) {
         for (int k = 0; k < 5; k++) {
            float local = Easing.clamp01((progress * n - k) / 0.78F);
            this.lift[2 + k] = (float)(Math.sin(Math.PI * local) * 0.5);
         }
      }
   }

   private double scale(KillFx fx, FoldAwayStyle.State s, float progress) {
      return SHEET[s.kind] * fx.size * (1.0 - 0.30000000000000004 * Easing.smoothstep(0.0F, 1.0F, progress));
   }

   private double foldedScale(KillFx fx, FoldAwayStyle.State s) {
      return SHEET[s.kind] * fx.size * 0.7;
   }

   private double middleY(KillFx fx, FoldAwayStyle.State s) {
      return fx.ctx.groundY + MIDDLE[s.kind] * fx.size;
   }

   private double figure(KillFx fx) {
      return 0.9 * fx.size;
   }

   private double figureFeet(KillFx fx, FoldAwayStyle.State s) {
      return (MIDDLE[s.kind] + FIGURE_Y[s.kind]) * fx.size - 0.9 * this.figure(fx);
   }

   private void printMap(KillFx fx, FoldAwayStyle.State s) {
      double sheet = SHEET[s.kind] * fx.size;
      double fig = this.figure(fx);
      double mid = MIDDLE[s.kind] * fx.size;
      double feet = this.figureFeet(fx, s);
      this.map[0] = 0.0;
      this.map[1] = sheet / (fig * 1.4F);
      this.map[2] = 0.5;
      this.map[3] = sheet / (fig * 2.1F);
      this.map[4] = 0.0;
      this.map[5] = ((mid - feet) / fig + 0.08F) / 2.1F;
   }

   private void flightAt(KillFx fx, FoldAwayStyle.State s, float e) {
      double away = 4.5 * fx.size * Math.pow(e, 1.5);
      double up = 2.6 * fx.size * Math.pow(e, 1.2);
      this.at[0] = fx.ctx.feetX - s.nX * away;
      this.at[1] = this.middleY(fx, s) + up;
      this.at[2] = fx.ctx.feetZ - s.nZ * away;
   }

   @Override
   public void update(KillFx fx, float dt) {
      FoldAwayStyle.State s = (FoldAwayStyle.State)fx.data;
      if (s != null && s.placed && !(fx.t < s.folded)) {
         float e = after(fx, s);
         if (s.fly) {
            if (fx.crit && e < 0.6F) {
               for (s.confettiDue = s.confettiDue + dt * 40.0F * fx.intensity * (fx.lod ? 0.5F : 1.0F); s.confettiDue >= 1.0F; s.confetti++) {
                  s.confettiDue--;
                  this.flightAt(fx, s, e);
                  this.confetti(fx, this.at[0], this.at[1], this.at[2], Paper.PASTEL_SET[fx.rng.nextInt(Paper.PASTEL_SET.length)], 0.6F);
               }
            }
         } else {
            double rest = fx.ctx.groundY + REST[s.kind] * this.foldedScale(fx, s);
            if (!s.dropping) {
               s.dropping = true;
               s.dropY = this.middleY(fx, s);
               s.dropVy = 0.6;
            }

            if (!s.settled) {
               s.dropVy -= 14.0 * dt;
               s.dropY = s.dropY + s.dropVy * dt;
               if (s.dropY <= rest) {
                  s.dropY = rest;
                  if (!s.bounced && s.dropVy < -1.0) {
                     s.bounced = true;
                     s.dropVy = -s.dropVy * 0.22;
                  } else {
                     s.dropVy = 0.0;
                     s.settled = true;
                  }
               }
            }

            if (fx.endPhase() > 0.0F && fx.bodyEnd == KillEffectsModule.BodyEnd.DUST && !s.scrapped) {
               s.scrapped = true;
               int n = fx.count(16.0F, false);
               int back = this.color.getColor(0.5F);
               int front = this.keepSkin.get() ? -988191 : back;

               for (int i = 0; i < n; i++) {
                  this.confetti(fx, fx.ctx.feetX + fx.rng.signed() * 0.3, s.dropY + 0.05, fx.ctx.feetZ + fx.rng.signed() * 0.3, i % 2 == 0 ? front : back, 1.4F);
               }
            }
         }
      }
   }

   private void confetti(KillFx fx, double x, double y, double z, int col, float lift) {
      double a = fx.rng.next() * Math.PI * 2.0;
      float out = 0.3F + fx.rng.next() * 0.5F;
      fx.particles
         .spawn(
            9,
            x,
            y,
            z,
            (float)Math.cos(a) * out,
            lift * (0.5F + fx.rng.next()),
            (float)Math.sin(a) * out,
            1.1F + fx.rng.next() * 0.5F,
            (0.06F + fx.rng.next() * 0.03F) * fx.size
         )
         .gravity(2.6F)
         .drag(2.9F)
         .axis((float)(-Math.sin(a)), 0.0F, (float)Math.cos(a))
         .spin(fx.rng.next() * 6.28F, fx.rng.signed() * 4.0F)
         .sway(0.4F, fx.rng.next() * 6.28F)
         .texture(Textures.WHITE)
         .color(ColorUtil.shade(col, fx.light) | 0xFF000000)
         .alpha(fx.alpha())
         .fade(0.02F, 0.7F)
         .flags(4)
         .ground(fx.ctx.groundY + 0.02, 0, 0.0F)
         .wind(0.6F);
   }

   @Override
   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      FoldAwayStyle.State s = (FoldAwayStyle.State)fx.data;
      if (s != null && pass == FxPass.SOLID) {
         Camera cam = Vfx.camera();
         if (!s.placed) {
            s.placed = true;
            aim(s, f.viewX + cam.posX - fx.ctx.feetX, f.viewZ + cam.posZ - fx.ctx.feetZ);
         }

         if (!s.tried && !fx.lod) {
            capture(fx, s);
         }

         if (fx.t >= 0.36F) {
            this.paper(fx, f, s);
         }

         if (fx.t < 0.5F) {
            this.body(fx, f, s);
         }
      }
   }

   private void body(KillFx fx, FxFrame f, FoldAwayStyle.State s) {
      BodyProxy.Look look = fx.look;
      float t = fx.t;
      boolean skin = this.keepSkin.get();
      look.params[2] = (this.paperTexture.get() ? 1 : 0) | (skin ? 0 : 2);
      look.tint = this.color.getColor(0.5F) | 0xFF000000;
      look.tintAmount = skin ? 0.0F : 1.0F;
      look.worldLight = fx.light;
      if (t < 0.3F) {
         look.kind = BodyLook.SKIN;
         look.sweepTo = BodyLook.PAPER;
         look.sweep = Easing.inOutCubic(t / 0.3F);
         look.sweepMode = 2;
         look.sweepOX = fx.ctx.impactX;
         look.sweepOY = fx.ctx.impactY;
         look.sweepOZ = fx.ctx.impactZ;
         look.sweepSpan = 2.2F * fx.size;
         look.sweepEdgeColor = -2326;
      } else {
         look.kind = BodyLook.PAPER;
         look.sweepTo = null;
      }

      float before = look.alpha;
      look.alpha = 1.0F - Easing.smoothstep(0.42F, 0.5F, t);
      float k = Easing.inOutCubic(fx.phase(0.3F, 0.5F));
      double fig = this.figure(fx);
      double feet = this.figureFeet(fx, s);
      double px = fx.ctx.feetX + s.nX * 0.012;
      double pz = fx.ctx.feetZ + s.nZ * 0.012;

      for (int i = 0; i < 6; i++) {
         BodyProxy.Part q = fx.rest[i];
         BodyProxy.Part p = fx.parts[i];
         p.copy(q);
         if (!(k <= 0.0F)) {
            double dx = q.x - fx.ctx.feetX;
            double dy = q.y - fx.ctx.feetY;
            double dz = q.z - fx.ctx.feetZ;
            double across = dx * s.rX + dz * s.rZ;
            double tx = px + s.rX * across * fig;
            double ty = fx.ctx.groundY + feet + dy * fig;
            double tz = pz + s.rZ * across * fig;
            p.x = q.x + (tx - q.x) * k;
            p.y = q.y + (ty - q.y) * k;
            p.z = q.z + (tz - q.z) * k;
            p.scale = (float)(1.0 + (fig - 1.0) * k);
            p.stretch = (float)(1.0 - 0.97 * k);
            p.sx = s.nX;
            p.sy = 0.0;
            p.sz = s.nZ;
            p.ox = px;
            p.oy = fx.ctx.groundY + feet;
            p.oz = pz;
         }
      }

      fx.drawBody(f);
      look.alpha = before;
      look.tintAmount = 0.0F;
      RenderState.translucent(true);
   }

   private void paper(KillFx fx, FxFrame f, FoldAwayStyle.State s) {
      float alpha = fx.alpha() * Easing.smoothstep(0.36F, 0.48F, fx.t);
      float progress = folding(fx, s);
      this.folds(s, progress);
      Paper.Pose p = this.pose;
      p.pointFold = this.fold;
      p.pointLift = this.lift;
      p.arc = 0.35;
      p.fold = progress;
      p.flap = 0.0;
      double phi = 0.0;
      double roll = 0.0;
      double spin = 0.0;
      double cx = fx.ctx.feetX;
      double cy = this.middleY(fx, s);
      double cz = fx.ctx.feetZ;
      double sc = this.scale(fx, s, progress);
      if (fx.t >= s.folded) {
         float e = after(fx, s);
         sc = this.foldedScale(fx, s);
         if (s.fly) {
            this.flightAt(fx, s, e);
            cx = this.at[0];
            cy = this.at[1];
            cz = this.at[2];
            double dAway = 6.75 * Math.sqrt(Math.max((double)e, 0.001));
            double dUp = 3.12 * Math.pow(Math.max((double)e, 0.001), 0.2);
            double climb = Math.atan2(dUp, dAway);
            if (s.shape == Paper.STAR) {
               spin = (Math.PI * 5.0 / 2.0) * Math.pow(e, 1.2);
            } else {
               phi = ((Math.PI / 2) - climb) * Easing.smoothstep(0.0F, 0.3F, e);
               roll = (s.shape == Paper.PLANE ? 0.12 : 0.2) * Math.sin(8.79645943005142 * e) * Easing.smoothstep(0.2F, 0.45F, e);
            }

            if (s.shape == Paper.CRANE) {
               p.flap = e < 0.45F ? 0.6 * Math.sin((Math.PI * 4) * e / 0.45) : 0.25 * Math.sin((Math.PI * 6) * e);
            }

            sc *= 1.0 - 0.7 * Math.pow(e, 1.3);
            alpha *= 1.0F - Easing.smoothstep(0.7F, 1.0F, e);
         } else {
            phi = (Math.PI / 2) * Easing.smoothstep(0.0F, 0.35F, e);
            cy = s.dropping ? s.dropY : cy;
            float end = fx.endPhase();
            if (end > 0.0F) {
               switch (fx.bodyEnd) {
                  case SINK:
                     cy -= Easing.inCubic(end) * 0.5 * fx.size;
                     alpha *= 1.0F - Easing.smoothstep(0.7F, 1.0F, end);
                     break;
                  case DUST:
                     alpha *= 1.0F - Easing.smoothstep(0.0F, 0.5F, end);
                     break;
                  default:
                     alpha *= 1.0F - Easing.smoothstep(0.0F, 1.0F, end);
               }
            }
         }
      }

      if (!(alpha <= 0.003F) && !(sc <= 1.0E-4)) {
         axes(p, s, phi, roll, spin);
         p.x = f.rx(cx);
         p.y = f.ry(cy);
         p.z = f.rz(cz);
         p.scale = sc;
         boolean skin = this.keepSkin.get();
         int back = ColorUtil.mulAlpha(this.color.getColor(0.5F) | 0xFF000000, alpha);
         int front = skin ? ColorUtil.mulAlpha(-988191, alpha) : back;
         MeshBatch.begin(Material.CUSTOM, Finish.MATTE, fx.light, 0.06F, -1, true, alpha >= 0.999F);
         Paper.sides(s.shape, p, front, back);
         MeshBatch.end();
         RenderState.translucent(true);
         RenderState.decal(true);
         RenderState.translucent(false);

         try {
            if (s.picture >= 0 && !fx.lod) {
               this.printMap(fx, s);
               this.print(fx, s, p, skin, alpha);
            }

            if (this.paperTexture.get()) {
               int ink = ColorUtil.mulAlpha(ColorUtil.shade(this.color.getColor(0.5F), 0.45F * fx.light) | 0xFF000000, 0.32F * alpha);
               Paper.fibers(s.shape, p, ink);
            }

            Vfx.flush();
         } finally {
            RenderState.decal(false);
            RenderState.translucent(true);
         }
      }
   }

   private void print(KillFx fx, FoldAwayStyle.State s, Paper.Pose p, boolean skin, float alpha) {
      boolean lineArt = !skin && LineArt.usable();
      int col = skin
         ? ColorUtil.shade(-1, fx.light) | 0xFF000000
         : ColorUtil.shade(this.color.getColor(0.5F), (lineArt ? 0.42F : 0.85F) * fx.light) | 0xFF000000;
      Vfx.flush();
      GlStateManager.enableCull();
      if (lineArt) {
         LineArt.begin(3, 0.3F, 0.45F);
      }

      try {
         Paper.print(s.shape, p, s.picture, this.map, ColorUtil.mulAlpha(col, alpha));
         Vfx.flush();
      } finally {
         if (lineArt) {
            LineArt.end();
         }

         GlStateManager.disableCull();
      }
   }

   private static void axes(Paper.Pose p, FoldAwayStyle.State s, double phi, double roll, double spin) {
      double cp = Math.cos(phi);
      double sp = Math.sin(phi);
      double fx = -s.nX * sp;
      double fy = cp;
      double fz = -s.nZ * sp;
      double ux = s.nX * cp;
      double uy = sp;
      double uz = s.nZ * cp;
      double rx = s.rX;
      double ry = 0.0;
      double rz = s.rZ;
      if (spin != 0.0) {
         double c = Math.cos(spin);
         double sn = Math.sin(spin);
         double nfx = fx * c + rx * sn;
         double nfy = cp * c + ry * sn;
         double nfz = fz * c + rz * sn;
         double nrx = rx * c - fx * sn;
         double nry = ry * c - cp * sn;
         double nrz = rz * c - fz * sn;
         fx = nfx;
         fy = nfy;
         fz = nfz;
         rx = nrx;
         ry = nry;
         rz = nrz;
      }

      if (roll != 0.0) {
         double c = Math.cos(roll);
         double sn = Math.sin(roll);
         double nux = ux * c + rx * sn;
         double nuy = sp * c + ry * sn;
         double nuz = uz * c + rz * sn;
         double nrx = rx * c - ux * sn;
         double nry = ry * c - sp * sn;
         double nrz = rz * c - uz * sn;
         ux = nux;
         uy = nuy;
         uz = nuz;
         rx = nrx;
         ry = nry;
         rz = nrz;
      }

      p.fx = fx;
      p.fy = fy;
      p.fz = fz;
      p.ux = ux;
      p.uy = uy;
      p.uz = uz;
      p.rx = rx;
      p.ry = ry;
      p.rz = rz;
   }

   @Override
   public boolean focus(KillFx fx, double[] out) {
      FoldAwayStyle.State s = (FoldAwayStyle.State)fx.data;
      if (s != null && s.fly && !(fx.t < s.folded)) {
         this.flightAt(fx, s, after(fx, s));
         out[0] = this.at[0];
         out[1] = this.at[2];
         return true;
      } else {
         return false;
      }
   }

   @Override
   public String stateForTest(KillFx fx) {
      FoldAwayStyle.State s = (FoldAwayStyle.State)fx.data;
      if (s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.3F
            ? "turning to paper"
            : (
               t < 0.5F
                  ? "flattening into the sheet"
                  : (t < s.folded ? "folding" : (s.fly ? "flying away" : (s.settled ? (fx.endPhase() > 0.0F ? "body end" : "lying there") : "dropping")))
            );
         String pic = fx.lod
            ? "plain paper (far)"
            : (
               !s.tried
                  ? "not yet"
                  : (
                     s.picture < 0
                        ? "plain paper (no portrait)"
                        : (this.keepSkin.get() ? "in color" : (LineArt.usable() ? "line art in the paper's color" : "tinted with the paper's color"))
                  )
            );
         float progress = folding(fx, s);
         int step = Math.min(STEP_COUNT[s.kind], (int)Math.floor(progress * STEP_COUNT[s.kind]) + (progress > 0.0F ? 1 : 0));
         return String.format(
            Locale.ROOT,
            "%s; %s, fold %.2f (crease %d of %d), %s at %.0f %%; print %s (drawn in %.2f ms); after %.2f%s; %d confetti; light %.2f",
            phase,
            this.shape.get().displayName(),
            progress,
            step,
            STEP_COUNT[s.kind],
            s.fly ? "flies" : "drops",
            this.foldSpeed.getFloat(),
            pic,
            s.pictureMillis,
            fx.t >= s.folded ? after(fx, s) : 0.0F,
            s.fly ? "" : String.format(Locale.ROOT, ", at %.2f above the ground", s.dropY - fx.ctx.groundY),
            s.confetti,
            fx.light
         );
      }
   }

   public static enum FinalShape implements Displayable {
      CRANE("Crane", "A paper crane that flaps twice and flies off"),
      PLANE("Paper plane", "A paper dart that glides away"),
      STAR("Star", "A puffy folded star that spins away"),
      BOAT("Boat", "A paper boat that sails up into the sky");

      private final String displayName;
      private final String description;

      private FinalShape(String displayName, String description) {
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
      boolean placed;
      double nX;
      double nZ;
      double rX;
      double rZ;
      int kind;
      Paper.Shape shape;
      float folded;
      boolean fly;
      int picture = -1;
      boolean tried;
      float pictureMillis;
      double dropY;
      double dropVy;
      boolean dropping;
      boolean settled;
      boolean bounced;
      float confettiDue;
      int confetti;
      boolean scrapped;

      private State() {
      }
   }
}
