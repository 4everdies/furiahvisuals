package meowtils.furiahvisuals.module.kill.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.kit.Comic;
import meowtils.furiahvisuals.fx.kit.Halftone;
import meowtils.furiahvisuals.fx.pass.FxPass;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.PlayerModel;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.render.look.BodyLook;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public final class KnockoutStyle extends KillStyleBase {
   private static final float SWEPT = 0.2F;
   private static final float SLAMMED = 0.35F;
   private static final float SHAKEN = 0.65F;
   private static final float LAUNCHED = 1.45F;
   private static final float TWINKLED = 1.65F;
   private static final float END = 1.8F;
   private static final float FLOPPED = 1.0F;
   private static final double AWAY = 6.5;
   private static final double UP = 8.5;
   private static final double GRAVITY = 5.0;
   private static final double TURNS = 3.0;
   private static final double SMALLEST = 0.08;
   private static final double BURST_W = 1.25;
   private static final double BURST_H = 1.05;
   private static final double BESIDE = 0.95;
   private static final int MAX_WORD = 8;
   private static final int RING_STARS = 6;
   private static final int DIZZY_STARS = 4;
   public final ColorSetting lettering;
   public final ColorSetting outline;
   public final ModeSetting<KnockoutStyle.Word> word;
   public final TextSetting custom;
   public final BooleanSetting launch;
   public final BooleanSetting halftone;
   public final SliderSetting outlineWidth;
   public final BooleanSetting twinkle;
   private static final double[] ROT = new double[9];
   private final double[] at = new double[3];

   public KnockoutStyle(KillEffectsModule module) {
      super(
         module,
         KillStyle.KNOCKOUT,
         new ColorSetting("knockout.color", "Burst", "The starburst's fill (its dots a darker shade).", ColorSetting.Mode.STATIC, -11713, -30147, 1.0)
      );
      this.signature(this.color, -11713);
      this.lettering = this.setting(
         new ColorSetting("knockout.lettering", "Lettering", "The word's letters.", ColorSetting.Mode.STATIC, -1689274, -1, 1.0).secondary()
      );
      this.signature(this.lettering, -1689274);
      this.outline = this.setting(
         new ColorSetting(
               "knockout.outline",
               "Outline",
               "The body's and the burst's outlines, and the word's shadow.",
               ColorSetting.Mode.STATIC,
               -15658735,
               -14017974,
               1.0
            )
            .secondary()
      );
      this.signature(this.outline, -15658735);
      this.word = this.setting(
         new ModeSetting<>("knockout.word", "Word", "What the burst says: K.O.! or your own word.", KnockoutStyle.Word.class, KnockoutStyle.Word.KO)
            .style(ModeSetting.Style.CHIPS)
      );
      this.custom = this.setting(
         new TextSetting("knockout.custom", "Custom Word", "Your own word, up to 8 characters.", "BOOM!", 8).visibleWhen(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return KnockoutStyle.this.word.get() == KnockoutStyle.Word.CUSTOM;
            }
         })
      );
      this.launch = this.setting(
         new BooleanSetting("knockout.launch", "Launch", "The body is launched into the sky (off: it flops flat, seeing stars, and stays like any body).", true)
      );
      this.halftone = this.setting(new BooleanSetting("knockout.halftone", "Halftone", "Printed halftone dots in the burst's shade.", true));
      this.outlineWidth = this.look(
         this.setting(new SliderSetting("knockout.outlineWidth", "Outline Width", "How bold the body's outlines are.", 50.0, 200.0, 100.0, 5.0).unit("%"))
      );
      this.twinkle = this.setting(new BooleanSetting("knockout.twinkle", "Twinkle", "The tiny star twinkles once before it's gone.", true));
   }

   @Override
   public float seconds(KillFx fx) {
      return 1.8F;
   }

   @Override
   public boolean leavesBody() {
      return !this.launch.get();
   }

   @Override
   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit();
   }

   @Override
   public void start(KillFx fx) {
      KnockoutStyle.State s = new KnockoutStyle.State();
      s.launch = this.launch.get();
      String text = this.word.get() == KnockoutStyle.Word.CUSTOM ? this.custom.get().trim() : "K.O.!";
      if (text.isEmpty()) {
         text = "K.O.!";
      }

      if (text.length() > 8) {
         text = text.substring(0, 8);
      }

      s.text = fx.crit ? text + "!" : text;
      Comic.word(s.text);
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
      double sx = -s.awayZ;
      double sz = s.awayX;
      double tilt = (hash(fx.seed, 1) - 0.5) * 0.6;
      s.spinX = sx + s.awayX * tilt;
      s.spinY = 0.25 * (hash(fx.seed, 2) - 0.5);
      s.spinZ = sz + s.awayZ * tilt;
      double sl = Math.sqrt(s.spinX * s.spinX + s.spinY * s.spinY + s.spinZ * s.spinZ);
      s.spinX /= sl;
      s.spinY /= sl;
      s.spinZ /= sl;
      s.side = hash(fx.seed, 3) < 0.5 ? -1 : 1;
      BodyProxy.Part b = fx.rest[1];
      s.pivotX = b.x;
      s.pivotY = b.y;
      s.pivotZ = b.z;
      s.ceiling = !fx.preview && ceilingAbove(Minecraft.getMinecraft().theWorld, fx.ctx.feetX, fx.ctx.feetY, fx.ctx.feetZ);
      fx.data = s;
   }

   private static boolean ceilingAbove(World w, double x, double y, double z) {
      if (w == null) {
         return false;
      } else {
         for (int dy = 2; dy <= 5; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  BlockPos p = new BlockPos(Math.floor(x + dx * 0.3), Math.floor(y) + dy, Math.floor(z + dz * 0.3));
                  if (w.getBlockState(p).getBlock().getMaterial().blocksMovement()) {
                     return true;
                  }
               }
            }
         }

         return false;
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

   private static double flight(KillFx fx) {
      return Math.max(0.0, (double)(Math.min(fx.t, 1.45F) - 0.65F));
   }

   private double flightAt(KillFx fx, KnockoutStyle.State s, double tau, double[] out) {
      float k = (float)(tau / 0.8000001F);
      double scale = 1.0 - 0.92 * Easing.inCubic(k) - 0.0;
      if (s.ceiling) {
         out[0] = s.pivotX;
         out[1] = s.pivotY + 0.35 * Math.sin(Math.PI * Math.min(1.0, k * 1.6));
         out[2] = s.pivotZ;
         return Math.max(0.02, 1.0 - 0.98 * Easing.inCubic(k));
      } else {
         double size = fx.size;
         out[0] = s.pivotX + s.awayX * 6.5 * tau * size;
         out[1] = s.pivotY + (8.5 * tau - 2.5 * tau * tau) * size;
         out[2] = s.pivotZ + s.awayZ * 6.5 * tau * size;
         return scale;
      }
   }

   private void body(KillFx fx, FxFrame f, KnockoutStyle.State s) {
      float t = fx.t;
      if (!s.launch || !(t >= 1.45F)) {
         BodyProxy.Look look = fx.look;
         look.outlineColor = this.outline.getColor(0.5F) | 0xFF000000;
         look.outlineWidth = 3.0F * this.outlineWidth.getFloat() / 100.0F;
         look.worldLight = fx.light;
         if (t < 0.2F) {
            look.kind = BodyLook.SKIN;
            look.sweepTo = BodyLook.TOON;
            look.sweep = Easing.inOutCubic(t / 0.2F);
            look.sweepMode = 2;
            look.sweepOX = fx.ctx.impactX;
            look.sweepOY = fx.ctx.impactY;
            look.sweepOZ = fx.ctx.impactZ;
            look.sweepSpan = 1.4F;
            look.sweepEdgeColor = -1;
         } else {
            look.kind = BodyLook.TOON;
            look.sweepTo = null;
         }

         look.dissolve = -1.0F;
         BodyProxy.Part[] parts = fx.parts;

         for (int i = 0; i < 6; i++) {
            parts[i].copy(fx.rest[i]);
         }

         if (s.launch) {
            double tau = flight(fx);
            if (tau > 0.0) {
               double scale = this.flightAt(fx, s, tau, this.at);
               double turn = (Math.PI * 6) * Easing.inQuad((float)(tau / 0.8000001F)) * (fx.crit ? 1.3 : 1.0);

               for (int i = 0; i < 6; i++) {
                  BodyProxy.Part p = parts[i];
                  if (s.ceiling) {
                     rotateAbout(p, s.pivotX, s.pivotY, s.pivotZ, 0.0, 1.0, 0.0, turn);
                  } else {
                     rotateAbout(p, s.pivotX, s.pivotY, s.pivotZ, s.spinX, s.spinY, s.spinZ, turn);
                  }

                  p.x = this.at[0] + (p.x - s.pivotX) * scale;
                  p.y = this.at[1] + (p.y - s.pivotY) * scale;
                  p.z = this.at[2] + (p.z - s.pivotZ) * scale;
                  p.scale = (float)scale;
               }
            }
         } else {
            float k = fx.phase(0.65F, 1.0F);
            if (k > 0.0F) {
               double fall = (Math.PI / 2) * bounce(k);
               double ax = -s.awayZ;
               double az = s.awayX;

               for (int i = 0; i < 6; i++) {
                  rotateAbout(parts[i], fx.ctx.feetX, fx.ctx.feetY + 0.12, fx.ctx.feetZ, ax, 0.0, az, -fall);
               }
            }
         }

         fx.drawBody(f);
         RenderState.translucent(true);
      }
   }

   private static double bounce(float k) {
      if (k < 0.7F) {
         float q = k / 0.7F;
         return q * q;
      } else {
         float q = (k - 0.7F) / 0.3F;
         return 1.0 - 0.08 * Math.sin(Math.PI * q);
      }
   }

   @Override
   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      KnockoutStyle.State s = (KnockoutStyle.State)fx.data;
      if (s != null) {
         if (pass == FxPass.SOLID) {
            this.body(fx, f, s);
         } else if (pass == FxPass.TRANSLUCENT) {
            Camera cam = Vfx.camera();
            this.burst(fx, f, s, cam);
            this.stars(fx, f, s, cam);
         } else if (pass == FxPass.GLOW) {
            this.glow(fx, f, s);
         }
      }
   }

   private void burst(KillFx fx, FxFrame f, KnockoutStyle.State s, Camera cam) {
      float t = fx.t;
      float alpha = fx.alpha();
      float in = fx.phase(0.2F, 0.35F);
      float away = fx.phase(0.9F, 1.0999999F);
      if (!(in <= 0.0F) && !(away >= 1.0F)) {
         double rx = cam.rightX;
         double ry = cam.rightY;
         double rz = cam.rightZ;
         double ux = cam.upX;
         double uy = cam.upY;
         double uz = cam.upZ;
         double sc = fx.size * (fx.crit ? 1.15 : 1.0);
         BodyProxy.Part b = fx.rest[1];
         double side = 0.95 * sc * s.side;
         double x = f.rx(b.x) + rx * side;
         double y = f.ry(b.y + 0.45) + ry * side;
         double z = f.rz(b.z) + rz * side;
         double by;
         double bx;
         if (t < 0.35F) {
            double e = Easing.outBack(in, 2.4F);
            bx = e * (1.0 - 0.25 * Math.sin(Math.PI * in));
            by = e * (1.0 + 0.3 * Math.sin(Math.PI * in));
         } else if (away <= 0.0F) {
            double wob = 0.05 * Math.exp(-(t - 0.35F) * 9.0) * Math.sin((t - 0.35F) * 70.0);
            bx = 1.0 + wob;
            by = 1.0 - wob;
         } else if (away < 0.45F) {
            double q = Easing.outCubic(away / 0.45F);
            bx = 1.0 + 0.35 * q;
            by = 1.0 - 0.55 * q;
         } else {
            double q = Easing.inCubic((away - 0.45F) / 0.55F);
            bx = 1.35 * (1.0 + 0.3 * q) * (1.0 - q);
            by = 0.45 * (1.0 - q);
         }

         double w = 1.25 * sc * bx;
         double h = 1.05 * sc * by;
         if (!(w <= 0.001) && !(h <= 0.001)) {
            int fill = this.color.getColor(0.5F);
            int line = this.outline.getColor(0.5F);
            int lineCol = ColorUtil.mulAlpha(line, alpha);
            int variant = (int)(hash(fx.seed, 7) * 4.0) % 4;
            double turn = s.side * Math.toRadians(-7.0);
            double shake = t >= 0.35F && t < 0.65F ? 0.05 * sc * Math.exp(-(t - 0.35F) * 7.0) : 0.0;
            double sx = shake * Math.sin(t * 210.0 + fx.seed);
            double sy = shake * Math.cos(t * 170.0 + fx.seed * 0.7);
            float lines = alpha * Easing.clamp01(in * 2.0F) * (1.0F - away);
            double var65;
            double var66;
            double var67;
            Comic.actionLines(
               var65 = x + (rx * sx + ux * sy),
               var66 = y + (ry * sx + uy * sy),
               var67 = z + (rz * sx + uz * sy),
               rx,
               ry,
               rz,
               ux,
               uy,
               uz,
               0.62 * sc,
               1.0 * sc,
               14,
               fx.seed,
               0.02 * sc,
               ColorUtil.mulAlpha(line, lines)
            );
            if (fx.crit) {
               Comic.balloon(
                  0, variant, var65, var66, var67, rx, ry, rz, ux, uy, uz, w * 1.14, h * 1.14, turn, ColorUtil.mulAlpha(-1, alpha), 0, false, lineCol, 1.6F
               );
            }

            Comic.balloon(
               0,
               variant,
               var65,
               var66,
               var67,
               rx,
               ry,
               rz,
               ux,
               uy,
               uz,
               w,
               h,
               turn,
               ColorUtil.mulAlpha(fill, alpha * 0.92F),
               ColorUtil.mulAlpha(ColorUtil.shade(fill, 0.7F), alpha),
               this.halftone.get(),
               lineCol,
               1.4F
            );
            Comic.Word wd = Comic.word(s.text);
            float slam = fx.phase(0.25F, 0.39F);
            if (slam > 0.0F) {
               double grow = 1.0 + 1.1 * (1.0 - Easing.outCubic(slam));
               double wordH = 0.32 * sc * grow * Math.min(bx, 1.2) * Math.min(by * 1.2, 1.0);
               double tilt = s.side * Math.toRadians(-10.0) + (t < 0.65F ? Math.toRadians(4.0) * Math.sin(t * 80.0) * Math.exp(-(t - 0.35F) * 6.0) : 0.0);
               float wa = alpha * Easing.clamp01(slam * 3.0F) * (1.0F - away);
               Comic.lettering(
                  wd,
                  var65,
                  var66,
                  var67,
                  rx,
                  ry,
                  rz,
                  ux,
                  uy,
                  uz,
                  wordH,
                  tilt,
                  ColorUtil.mulAlpha(this.lettering.getColor(0.5F), wa),
                  ColorUtil.mulAlpha(line, wa),
                  fx.crit ? 1.6F : 1.0F
               );
            }
         }
      }
   }

   private void stars(KillFx fx, FxFrame f, KnockoutStyle.State s, Camera cam) {
      float t = fx.t;
      float alpha = fx.alpha();
      int fill = this.color.getColor(0.5F);
      int line = this.outline.getColor(0.5F);
      float k;
      if (fx.crit && s.launch && (k = fx.phase(0.2F, 0.35F) * (1.0F - fx.phase(0.65F, 0.77F))) > 0.01F) {
         BodyProxy.Part b = fx.rest[1];

         for (int i = 0; i < 6; i++) {
            double a = (Math.PI * 2) * i / 6.0 + t * 5.0;
            double r = 0.75 * fx.size;
            star(
               f.rx(b.x + Math.cos(a) * r),
               f.ry(b.y + 0.15 + 0.1 * Math.sin(a * 2.0)),
               f.rz(b.z + Math.sin(a) * r),
               0.16 * fx.size,
               (float)(a * 2.0),
               fill,
               line,
               alpha * k
            );
         }
      }

      if (!s.launch && (k = fx.phase(0.9F, 1.1F) * (1.0F - fx.endPhase())) > 0.01F) {
         BodyProxy.Part h = fx.parts[0];

         for (int i = 0; i < 4; i++) {
            double a = (Math.PI * 2) * i / 4.0 + t * 4.0;
            double r = 0.3 * fx.size;
            star(
               f.rx(h.x + Math.cos(a) * r),
               f.ry(h.y + 0.32 + 0.05 * Math.sin(a * 3.0)),
               f.rz(h.z + Math.sin(a) * r),
               0.11 * fx.size,
               (float)(a * 1.5),
               fill,
               line,
               alpha * k
            );
         }
      }
   }

   private static void star(double x, double y, double z, double size, float spin, int fill, int line, float a) {
      Vfx.sprite(Textures.STAR, x, y, z, size * 1.3, spin, ColorUtil.mulAlpha(line, a));
      Vfx.sprite(Textures.STAR, x, y, z, size, spin, ColorUtil.mulAlpha(fill, a));
   }

   private void glow(KillFx fx, FxFrame f, KnockoutStyle.State s) {
      float alpha = fx.alpha();
      boolean calm = FuriahsVisuals.instance.isReduceFlashing();
      int fill = this.color.getColor(0.5F);
      float flash;
      if (!calm && (flash = 1.0F - Easing.clamp01(Math.abs(fx.t - 0.35F) / 0.08F)) > 0.0F) {
         BodyProxy.Part b = fx.rest[1];
         Vfx.sprite(Textures.GLOW, f.rx(b.x), f.ry(b.y + 0.4), f.rz(b.z), 2.4 * fx.size, 0.0F, alpha(fill, 0.35F * flash * alpha));
      }

      if (s.launch && this.twinkle.get()) {
         float k = fx.phase(1.4000001F, 1.65F);
         if (!(k <= 0.0F) && !(k >= 1.0F)) {
            this.flightAt(fx, s, 0.8000001F, this.at);
            double x = f.rx(this.at[0]);
            double y = f.ry(this.at[1]);
            double z = f.rz(this.at[2]);
            float bloom = (float)Math.sin(Math.PI * k);
            double size = (s.ceiling ? 0.45 : 0.9) * fx.size * bloom;
            float a = alpha * bloom * (calm ? 0.55F : 1.0F);
            Vfx.sprite(Textures.STAR, x, y, z, size * 0.55, (float)(k * 2.0), core(fill, a));
            Vfx.sprite(Textures.FLARE, x, y, z, size * 1.4, 0.3F, alpha(fill, 0.7F * a));
            Vfx.sprite(Textures.GLOW, x, y, z, size * 1.8, 0.0F, alpha(fill, 0.35F * a));
         }
      }
   }

   @Override
   public String stateForTest(KillFx fx) {
      KnockoutStyle.State s = (KnockoutStyle.State)fx.data;
      if (s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.2F
            ? "turning toon"
            : (
               t < 0.35F
                  ? "the burst slams in"
                  : (
                     t < 0.65F
                        ? "shaking"
                        : (
                           !s.launch
                              ? (t < 1.0F ? "flopping" : "flat, seeing stars")
                              : (t < 1.45F ? (s.ceiling ? "spinning under a ceiling" : "launched") : (t < 1.65F ? "twinkling" : "gone"))
                        )
                  )
            );
         Comic.Word w = Comic.word(s.text);
         Object[] objectArray = new Object[]{
            phase,
            s.text,
            w.ready() ? "ready" : "painting",
            3.0F * this.outlineWidth.getFloat() / 100.0F,
            this.halftone.get() ? (Halftone.usable() ? "shader" : "printed") : "off",
            s.launch ? "on" : "off",
            s.ceiling ? ", ceiling above" : ""
         };
         return String.format(Locale.ROOT, "%s; \"%s\" (%s), outline %.1f px, halftone %s, launch %s%s", objectArray);
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
      String text;
      boolean ceiling;
      boolean launch;
      double awayX;
      double awayZ;
      double spinX;
      double spinY;
      double spinZ;
      int side;
      double pivotX;
      double pivotY;
      double pivotZ;

      private State() {
      }
   }

   public static enum Word implements Displayable {
      KO("K.O.!", "The classic"),
      CUSTOM("Custom", "Your own word, up to 8 characters");

      private final String displayName;
      private final String description;

      private Word(String displayName, String description) {
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
}
