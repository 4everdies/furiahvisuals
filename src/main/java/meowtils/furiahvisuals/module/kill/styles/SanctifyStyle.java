package meowtils.furiahvisuals.module.kill.styles;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.kit.Glass;
import meowtils.furiahvisuals.fx.look.NearCamera;
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
import meowtils.furiahvisuals.render.SkinSampler;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.render.look.BodyLook;
import meowtils.furiahvisuals.render.look.GlassCells;
import meowtils.furiahvisuals.render.look.Portrait;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class SanctifyStyle extends KillStyleBase {
   private static final float GLASSED = 0.5F;
   private static final float WINDOWED = 1.0F;
   private static final float BRIGHT = 1.6F;
   private static final float END = 2.4F;
   private static final float WINDOW_FROM = 0.68F;
   private static final float WINDOW_TO = 0.95F;
   private static final float BODY_GONE_FROM = 0.8F;
   private static final double ARCH_W = 1.05;
   private static final double ROUND_R = 1.0;
   private static final double BOTTOM = 0.25;
   private static final double ARCH_FIG = 0.85;
   private static final double ROUND_FIG = 0.78;
   private static final double ARCH_FEET = 0.1;
   private static final double CELL = 0.1;
   private static final float LEAD_W = 0.075F;
   private static final float OPACITY = 0.86F;
   private static final double BEHIND = 0.03;
   private static final double FRAME = 0.065;
   private static final double TRACERY = 0.045;
   private static final int MAX_OUTLINE = 52;
   private static final int SHAFTS = 5;
   private static final float MOTE_WIND = 0.5F;
   public final ColorSetting lead;
   public final ModeSetting<SanctifyStyle.Palette> glassColors;
   public final ColorSetting glass1;
   public final ColorSetting glass2;
   public final ColorSetting glass3;
   public final ModeSetting<SanctifyStyle.Window> window;
   public final SliderSetting cellSize;
   public final SliderSetting shafts;
   public final BooleanSetting groundLight;
   public final BooleanSetting motes;
   private final double[] tu = new double[64];
   private final double[] tv = new double[64];
   private final Glass.Pane pane = new Glass.Pane();
   private final float[] hsv = new float[3];
   private double wx;
   private double wy;
   private double wz;

   public SanctifyStyle(KillEffectsModule module) {
      super(
         module,
         KillStyle.SANCTIFY,
         new ColorSetting("sanctify.color", "Light", "The light pouring through the glass.", ColorSetting.Mode.STATIC, -3639, -9846, 1.0)
      );
      this.signature(this.color, -3639);
      this.lead = this.setting(
         new ColorSetting("sanctify.lead", "Lead", "The lead between the cells and round the window.", ColorSetting.Mode.STATIC, -15066594, -12961212, 1.0)
            .secondary()
      );
      this.signature(this.lead, -15066594);
      this.glassColors = this.setting(
         new ModeSetting<>(
               "sanctify.glassColors",
               "Glass Colors",
               "From the skin: the figure's own colors as glass. Cathedral: ruby, sapphire, emerald and gold. Tinted: the three glass colors.",
               SanctifyStyle.Palette.class,
               SanctifyStyle.Palette.SKIN
            )
            .style(ModeSetting.Style.CHIPS)
      );
      BooleanSupplier tinted = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return SanctifyStyle.this.glassColors.get() == SanctifyStyle.Palette.TINTED;
         }
      };
      this.glass1 = this.setting(
         new ColorSetting(
               "sanctify.glass1", "Glass 1", "The first glass color (Tinted): the figure's darkest parts.", ColorSetting.Mode.STATIC, -13935399, -9794305, 1.0
            )
            .secondary()
            .visibleWhen(tinted)
      );
      this.glass2 = this.setting(
         new ColorSetting("sanctify.glass2", "Glass 2", "The second glass color (Tinted).", ColorSetting.Mode.STATIC, -4184513, -38273, 1.0)
            .secondary()
            .visibleWhen(tinted)
      );
      this.glass3 = this.setting(
         new ColorSetting(
               "sanctify.glass3", "Glass 3", "The third glass color (Tinted): the figure's brightest parts.", ColorSetting.Mode.STATIC, -868018, -6496, 1.0
            )
            .secondary()
            .visibleWhen(tinted)
      );
      this.signature(this.glass1, -13935399);
      this.signature(this.glass2, -4184513);
      this.signature(this.glass3, -868018);
      this.window = this.setting(
         new ModeSetting<>(
               "sanctify.window",
               "Window",
               "The window's shape: a tall arch, a circle, or a rose window.",
               SanctifyStyle.Window.class,
               SanctifyStyle.Window.ARCHED
            )
            .style(ModeSetting.Style.CHIPS)
      );
      this.cellSize = this.look(
         this.setting(new SliderSetting("sanctify.cellSize", "Cell Size", "How big the glass cells are.", 50.0, 150.0, 100.0, 5.0).unit("%"))
      );
      this.shafts = this.look(
         this.setting(
            new SliderSetting("sanctify.shafts", "Light Shafts", "How strong the shafts of light through the window are.", 0.0, 100.0, 60.0, 5.0).unit("%")
         )
      );
      this.groundLight = this.setting(new BooleanSetting("sanctify.groundLight", "Ground Light", "The glass casts colored light on the ground.", true));
      this.motes = this.setting(new BooleanSetting("sanctify.motes", "Motes", "The glass goes into rising motes of colored light.", true));
   }

   @Override
   public float seconds(KillFx fx) {
      return 2.4F;
   }

   @Override
   public int passes(KillFx fx) {
      return FxPass.SOLID.bit() | FxPass.TRANSLUCENT.bit() | FxPass.GLOW.bit();
   }

   @Override
   public void start(KillFx fx) {
      SanctifyStyle.State s = new SanctifyStyle.State();
      aim(s, -fx.ctx.dirX, -fx.ctx.dirZ);
      double size = fx.size;
      SanctifyStyle.Window w = this.window.get();
      if (w == SanctifyStyle.Window.ARCHED) {
         double[][] o = Glass.outlineOf(1);
         s.width = 1.05 * size;
         s.bottom = 0.25 * size;
         s.outline = Math.min(52, o[0].length);
         s.top = s.bottom;

         for (int i = 0; i < s.outline; i++) {
            s.oa[i] = o[0][i] * s.width;
            s.oh[i] = s.bottom + (o[1][i] + 1.0) * s.width;
            s.top = Math.max(s.top, s.oh[i]);
         }

         s.ma = 0.0;
         s.mh = s.bottom + (s.top - s.bottom) * 0.45;
         s.fig = 0.85 * size;
         s.feet = s.bottom + 0.1 * size;
      } else {
         s.round = true;
         s.radius = 1.0 * size;
         s.bottom = 0.25 * size;
         s.outline = 48;

         for (int i = 0; i < s.outline; i++) {
            s.oa[i] = Glass.RING_COS[i] * s.radius;
            s.oh[i] = s.bottom + s.radius + Glass.RING_SIN[i] * s.radius;
         }

         s.top = s.bottom + 2.0 * s.radius;
         s.width = 2.0 * s.radius;
         s.ma = 0.0;
         s.mh = s.bottom + s.radius;
         s.fig = 0.78 * size;
         s.feet = s.mh - 0.9 * s.fig;
      }

      this.palette(fx, s);
      fx.data = s;
   }

   private void palette(KillFx fx, SanctifyStyle.State s) {
      SanctifyStyle.Palette p = this.glassColors.get();
      s.palette = p.ordinal();
      if (p == SanctifyStyle.Palette.TINTED) {
         int g1 = this.glass1.getColor(0.5F);
         int g2 = this.glass2.getColor(0.5F);
         int g3 = this.glass3.getColor(0.5F);
         s.cells[0] = g1;
         s.cells[1] = g2;
         s.cells[2] = g3;
         s.cells[3] = ColorUtil.lerpOklab(g1, g3, 0.5F);
         System.arraycopy(s.cells, 0, s.light, 0, 4);
         backdrop(s, g1, g2);
      } else {
         System.arraycopy(Glass.CATHEDRAL_SET, 0, s.cells, 0, 4);
         backdrop(s, Glass.CATHEDRAL_SET[1], Glass.CATHEDRAL_SET[0]);
         if (p == SanctifyStyle.Palette.CATHEDRAL) {
            System.arraycopy(Glass.CATHEDRAL_SET, 0, s.light, 0, 4);
         } else {
            int[] parts = new int[]{1, 0, 4, 2};

            for (int i = 0; i < 4; i++) {
               int c = SkinSampler.average(fx.ctx.body, parts[i], Glass.CATHEDRAL_SET[i]);
               ColorUtil.toHsv(c, this.hsv);
               s.light[i] = ColorUtil.hsv(this.hsv[0], Math.min(1.0F, this.hsv[1] * 1.25F + 0.12F), 0.5F + 0.5F * this.hsv[2]) | 0xFF000000;
            }
         }
      }
   }

   private static void backdrop(SanctifyStyle.State s, int main, int accent) {
      s.back[0] = ColorUtil.shade(main, 0.55F) | 0xFF000000;
      s.back[1] = ColorUtil.shade(main, 0.64F) | 0xFF000000;
      s.back[2] = ColorUtil.shade(main, 0.47F) | 0xFF000000;
      s.back[3] = ColorUtil.shade(accent, 0.55F) | 0xFF000000;
   }

   private static void aim(SanctifyStyle.State s, double dx, double dz) {
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

   private static void place(KillFx fx, FxFrame f, SanctifyStyle.State s, Camera cam) {
      s.placed = true;
      aim(s, f.viewX + cam.posX - fx.ctx.feetX, f.viewZ + cam.posZ - fx.ctx.feetZ);
   }

   private static void capture(KillFx fx, SanctifyStyle.State s) {
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

   private static float windowIn(KillFx fx) {
      return Easing.smoothstep(0.68F, 0.95F, fx.t);
   }

   private static float swell(KillFx fx) {
      return Easing.inOutCubic(fx.phase(1.0F, 1.6F));
   }

   private static float gone(KillFx fx) {
      return fx.phase(1.6F, 2.3500001F) * 1.08F;
   }

   private static float roseOpen(KillFx fx) {
      return fx.crit ? Easing.outBack(fx.phase(0.85F, 1.25F), 1.4F) : 0.0F;
   }

   @Override
   public void update(KillFx fx, float dt) {
      SanctifyStyle.State s = (SanctifyStyle.State)fx.data;
      if (s != null && s.placed && this.motes.get() && !(fx.t < 1.5500001F) && !(fx.t > 2.4F)) {
         s.motesDue = s.motesDue + dt / 0.8000001F * fx.count(36.0F, true);

         while (s.motesDue >= 1.0F) {
            s.motesDue--;
            this.mote(fx, s);
         }
      }
   }

   private void mote(KillFx fx, SanctifyStyle.State s) {
      double a = 0.0;
      double h = 0.0;
      boolean found = false;

      for (int tries = 0; tries < 8 && !found; tries++) {
         a = (fx.rng.next() - 0.5) * s.width;
         h = s.bottom + fx.rng.next() * (s.top - s.bottom);
         found = inside(s, a, h);
      }

      if (found) {
         double x = fx.ctx.feetX + s.rX * a + s.nX * 0.04;
         double y = fx.ctx.groundY + h;
         double z = fx.ctx.feetZ + s.rZ * a + s.nZ * 0.04;
         int col = s.light[fx.rng.nextInt(4)];
         float drift = fx.rng.signed() * 0.18F;
         fx.particles
            .spawn(
               3,
               x,
               y,
               z,
               (float)(s.rX * drift + s.nX * 0.12),
               0.45F + fx.rng.next() * 0.6F,
               (float)(s.rZ * drift + s.nZ * 0.12),
               1.0F + fx.rng.next() * 0.6F,
               (0.05F + fx.rng.next() * 0.04F) * fx.size
            )
            .drag(0.6F)
            .color(ColorUtil.lerp(col, -1, 0.25F))
            .alpha(fx.alpha())
            .fade(0.1F, 0.55F)
            .end(0.01F)
            .wind(0.5F);
         s.motes++;
      }
   }

   private static boolean inside(SanctifyStyle.State s, double a, double h) {
      if (s.round) {
         double da = a - s.ma;
         double dh = h - s.mh;
         return da * da + dh * dh <= s.radius * s.radius * 0.94;
      } else {
         for (int i = 0; i < s.outline; i++) {
            int j = (i + 1) % s.outline;
            double ea = s.oa[j] - s.oa[i];
            double ch = h - s.oh[i];
            double eh = s.oh[j] - s.oh[i];
            double ca = a - s.oa[i];
            if (ea * ch - eh * ca < 0.0) {
               return false;
            }
         }

         return true;
      }
   }

   @Override
   public void renderPass(KillFx fx, FxFrame f, FxPass pass) {
      SanctifyStyle.State s = (SanctifyStyle.State)fx.data;
      if (s != null) {
         Camera cam = Vfx.camera();
         if (pass == FxPass.SOLID) {
            if (!s.placed) {
               place(fx, f, s, cam);
            }

            if (!s.tried && !fx.lod) {
               capture(fx, s);
            }

            this.body(fx, f, s);
         } else {
            if (!s.placed) {
               return;
            }

            if (pass == FxPass.TRANSLUCENT) {
               this.window(fx, f, s, cam);
               if (fx.crit) {
                  this.rose(fx, f, s, false);
               }
            } else if (pass == FxPass.GLOW) {
               this.light(fx, f, s, cam);
            }
         }
      }
   }

   private void body(KillFx fx, FxFrame f, SanctifyStyle.State s) {
      if (!(fx.t >= 1.0F)) {
         BodyProxy.Look look = fx.look;
         float t = fx.t;
         look.params[3] = s.palette == SanctifyStyle.Palette.SKIN.ordinal() ? 0 : (s.palette == SanctifyStyle.Palette.CATHEDRAL.ordinal() ? 1 : 2);
         look.params[4] = this.cellSize.getInt();
         look.params[5] = this.glass1.getColor(0.5F);
         look.params[6] = this.glass2.getColor(0.5F);
         look.params[7] = this.glass3.getColor(0.5F);
         look.params[8] = this.lead.getColor(0.5F) | 0xFF000000;
         look.params[9] = 80;
         look.worldLight = fx.light;
         if (t < 0.5F) {
            look.kind = BodyLook.SKIN;
            look.sweepTo = BodyLook.GLASS;
            look.sweep = Easing.inOutCubic(t / 0.5F);
            look.sweepMode = 0;
            look.sweepOX = fx.ctx.feetX;
            look.sweepOY = fx.ctx.feetY;
            look.sweepOZ = fx.ctx.feetZ;
            look.sweepSpan = 2.0F * fx.size;
            look.sweepEdgeColor = this.color.getColor(0.5F) | 0xFF000000;
         } else {
            look.kind = BodyLook.GLASS;
            look.sweepTo = null;
         }

         float before = look.alpha;
         look.alpha = 1.0F - Easing.smoothstep(0.8F, 1.0F, t);
         float k = Easing.inOutCubic(fx.phase(0.5F, 1.0F));
         double px = fx.ctx.feetX - s.nX * 0.03;
         double pz = fx.ctx.feetZ - s.nZ * 0.03;

         for (int i = 0; i < 6; i++) {
            BodyProxy.Part q = fx.rest[i];
            BodyProxy.Part p = fx.parts[i];
            p.copy(q);
            if (!(k <= 0.0F)) {
               double dx = q.x - fx.ctx.feetX;
               double dy = q.y - fx.ctx.feetY;
               double dz = q.z - fx.ctx.feetZ;
               double across = dx * s.rX + dz * s.rZ;
               double tx = px + s.rX * across * s.fig;
               double ty = fx.ctx.groundY + s.feet + dy * s.fig;
               double tz = pz + s.rZ * across * s.fig;
               p.x = q.x + (tx - q.x) * k;
               p.y = q.y + (ty - q.y) * k;
               p.z = q.z + (tz - q.z) * k;
               p.scale = (float)(1.0 + (s.fig - 1.0) * k);
               p.stretch = (float)(1.0 - 0.97 * k);
               p.sx = s.nX;
               p.sy = 0.0;
               p.sz = s.nZ;
               p.ox = px;
               p.oy = fx.ctx.groundY + s.feet;
               p.oz = pz;
            }
         }

         fx.drawBody(f);
         look.alpha = before;
         RenderState.translucent(true);
      }
   }

   private void at(KillFx fx, FxFrame f, SanctifyStyle.State s, double a, double h, double out) {
      this.wx = f.rx(fx.ctx.feetX + s.rX * a + s.nX * out);
      this.wy = f.ry(fx.ctx.groundY + h);
      this.wz = f.rz(fx.ctx.feetZ + s.rZ * a + s.nZ * out);
   }

   private static double pu(SanctifyStyle.State s, double a) {
      return (a / s.fig + 0.7F) / 1.4F;
   }

   private static double pv(SanctifyStyle.State s, double h) {
      return ((h - s.feet) / s.fig + 0.08F) / 2.1F;
   }

   private float windowAlpha(KillFx fx, FxFrame f, SanctifyStyle.State s, Camera cam) {
      float a = windowIn(fx) * fx.alpha();
      if (a <= 0.003F) {
         return 0.0F;
      } else {
         this.at(fx, f, s, 0.0, (s.bottom + s.top) * 0.5, 0.0);
         return a * NearCamera.fade(cam, this.wx, this.wy, this.wz);
      }
   }

   private void window(KillFx fx, FxFrame f, SanctifyStyle.State s, Camera cam) {
      float alpha = this.windowAlpha(fx, f, s, cam);
      if (!(alpha <= 0.003F)) {
         float light = swell(fx);
         float gone = gone(fx);
         float dim = fx.light;
         boolean portrait = s.picture >= 0 && !fx.lod;
         RenderState.translucent(false);

         try {
            if (GlassCells.available()) {
               this.glassCells(fx, f, s, alpha, light, gone, dim, portrait, 0.0F);
            } else {
               this.fallbackGlass(fx, f, s, alpha * (1.0F - Easing.clamp01(gone)), light, dim, portrait);
            }

            float leadAlpha = alpha * (1.0F - Easing.smoothstep(0.6F, 1.05F, gone));
            if (leadAlpha > 0.003F) {
               int col = alpha(ColorUtil.shade(this.lead.getColor(0.5F), dim * (1.0F + 0.25F * light)) | 0xFF000000, leadAlpha);
               RenderState.decal(true);
               this.frame(fx, f, s, col);
               if (this.window.get() == SanctifyStyle.Window.ROSE) {
                  this.tracery(fx, f, s, col);
               }

               RenderState.decal(false);
            }
         } finally {
            Vfx.flush();
            RenderState.translucent(true);
         }
      }
   }

   private void glassCells(KillFx fx, FxFrame f, SanctifyStyle.State s, float alpha, float light, float gone, float dim, boolean portrait, float glow) {
      Vfx.flush();
      float cell = (float)(0.1 * this.cellSize.getFloat() / 100.0 * fx.size);
      GlassCells.begin(
         (float)(1.4F * s.fig),
         (float)(2.1F * s.fig),
         (float)(-0.7F * s.fig),
         (float)(-0.08F * s.fig + s.feet),
         cell,
         0.075F,
         this.lead.getColor(0.5F),
         s.cells,
         s.back,
         s.palette,
         portrait,
         light,
         this.color.getColor(0.5F),
         gone,
         (float)s.bottom,
         (float)(s.top - s.bottom),
         glow,
         dim,
         0.86F
      );

      try {
         this.fan(fx, f, s, portrait ? s.picture : Textures.WHITE, alpha(-1, alpha), false);
         Vfx.flush();
      } finally {
         GlassCells.end();
      }
   }

   private void fallbackGlass(KillFx fx, FxFrame f, SanctifyStyle.State s, float alpha, float light, float dim, boolean portrait) {
      if (!(alpha <= 0.003F)) {
         float k = dim * (1.0F + 0.6F * light);
         int low = alpha(ColorUtil.shade(s.back[0], k) | 0xFF000000, alpha * 0.86F);
         int high = alpha(ColorUtil.shade(s.back[2], k) | 0xFF000000, alpha * 0.86F);
         this.gradientFan(fx, f, s, Glass.texture(), low, high, 1.2);
         if (portrait) {
            this.fan(fx, f, s, s.picture, alpha(ColorUtil.shade(-1, k) | 0xFF000000, alpha), false);
         }

         double cell = 0.1 * this.cellSize.getFloat() / 100.0 * fx.size;
         this.gradientFan(
            fx, f, s, GlassCells.leadTexture(), alpha(this.lead.getColor(0.5F), alpha), alpha(this.lead.getColor(0.5F), alpha), 1.0 / (cell * 4.0)
         );
      }
   }

   private void fan(KillFx fx, FxFrame f, SanctifyStyle.State s, int texture, int col, boolean tiled) {
      this.at(fx, f, s, s.ma, s.mh, 0.0);
      double cx = this.wx;
      double cy = this.wy;
      double cz = this.wz;
      double cu = pu(s, s.ma);
      double cv = pv(s, s.mh);

      for (int i = 0; i < s.outline; i++) {
         int j = (i + 1) % s.outline;
         this.at(fx, f, s, s.oa[i], s.oh[i], 0.0);
         double ax = this.wx;
         double ay = this.wy;
         double az = this.wz;
         this.at(fx, f, s, s.oa[j], s.oh[j], 0.0);
         Vfx.quadUV(
            texture,
            cx,
            cy,
            cz,
            cu,
            cv,
            col,
            ax,
            ay,
            az,
            pu(s, s.oa[i]),
            pv(s, s.oh[i]),
            col,
            this.wx,
            this.wy,
            this.wz,
            pu(s, s.oa[j]),
            pv(s, s.oh[j]),
            col,
            this.wx,
            this.wy,
            this.wz,
            pu(s, s.oa[j]),
            pv(s, s.oh[j]),
            col
         );
      }
   }

   private void gradientFan(KillFx fx, FxFrame f, SanctifyStyle.State s, int texture, int low, int high, double repeat) {
      double span = Math.max(0.01, s.top - s.bottom);
      this.at(fx, f, s, s.ma, s.mh, 0.0);
      double cx = this.wx;
      double cy = this.wy;
      double cz = this.wz;
      int cc = ColorUtil.lerp(low, high, (float)((s.mh - s.bottom) / span));

      for (int i = 0; i < s.outline; i++) {
         int j = (i + 1) % s.outline;
         this.at(fx, f, s, s.oa[i], s.oh[i], 0.0);
         double ax = this.wx;
         double ay = this.wy;
         double az = this.wz;
         int ca = ColorUtil.lerp(low, high, (float)((s.oh[i] - s.bottom) / span));
         this.at(fx, f, s, s.oa[j], s.oh[j], 0.0);
         int cb = ColorUtil.lerp(low, high, (float)((s.oh[j] - s.bottom) / span));
         Vfx.quadUV(
            texture,
            cx,
            cy,
            cz,
            s.ma * repeat,
            s.mh * repeat,
            cc,
            ax,
            ay,
            az,
            s.oa[i] * repeat,
            s.oh[i] * repeat,
            ca,
            this.wx,
            this.wy,
            this.wz,
            s.oa[j] * repeat,
            s.oh[j] * repeat,
            cb,
            this.wx,
            this.wy,
            this.wz,
            s.oa[j] * repeat,
            s.oh[j] * repeat,
            cb
         );
      }
   }

   private void strip(KillFx fx, FxFrame f, SanctifyStyle.State s, double a0, double h0, double a1, double h1, double width, int col) {
      double ea = a1 - a0;
      double eh = h1 - h0;
      double l = Math.sqrt(ea * ea + eh * eh);
      if (!(l < 1.0E-6)) {
         double hw = width * 0.5;
         double ua = ea / l * hw;
         double uh = eh / l * hw;
         double na = -uh;
         double var49;
         double var50;
         this.at(fx, f, s, (var49 = a0 - ua) - na, (var50 = h0 - uh) - ua, 0.004);
         double x0 = this.wx;
         double y0 = this.wy;
         double z0 = this.wz;
         double var51;
         double var52;
         this.at(fx, f, s, (var51 = a1 + ua) - na, (var52 = h1 + uh) - ua, 0.004);
         double x1 = this.wx;
         double y1 = this.wy;
         double z1 = this.wz;
         this.at(fx, f, s, var51 + na, var52 + ua, 0.004);
         double x2 = this.wx;
         double y2 = this.wy;
         double z2 = this.wz;
         this.at(fx, f, s, var49 + na, var50 + ua, 0.004);
         Vfx.quadUV(Textures.WHITE, x0, y0, z0, 0.5, 0.5, col, x1, y1, z1, 0.5, 0.5, col, x2, y2, z2, 0.5, 0.5, col, this.wx, this.wy, this.wz, 0.5, 0.5, col);
      }
   }

   private void frame(KillFx fx, FxFrame f, SanctifyStyle.State s, int col) {
      double w = 0.065 * fx.size;

      for (int i = 0; i < s.outline; i++) {
         int j = (i + 1) % s.outline;
         this.strip(fx, f, s, s.oa[i], s.oh[i], s.oa[j], s.oh[j], w, col);
      }
   }

   private void tracery(KillFx fx, FxFrame f, SanctifyStyle.State s, int col) {
      double r = s.radius;
      double w = 0.045 * fx.size;
      int segs = 96;
      double prevA = 0.0;
      double prevH = 0.0;

      for (int i = 0; i <= segs; i++) {
         double th = i * (Math.PI * 2) / segs;
         double x = th / (Math.PI / 6) % 1.0;
         double bump = Math.pow(1.0 - Math.abs(2.0 * x - 1.0), 1.4);
         double pr = r * (0.3 + 0.38 * bump);
         double a = s.ma + Math.cos(th) * pr;
         double h = s.mh + Math.sin(th) * pr;
         if (i > 0) {
            this.strip(fx, f, s, prevA, prevH, a, h, w, col);
         }

         prevA = a;
         prevH = h;
      }

      this.circle(fx, f, s, r * 0.3, w, col);
      this.circle(fx, f, s, r * 0.12, w, col);

      for (int k = 0; k < 12; k++) {
         double th = (k + 0.5) * (Math.PI * 2) / 12.0;
         this.strip(fx, f, s, s.ma + Math.cos(th) * r * 0.68, s.mh + Math.sin(th) * r * 0.68, s.ma + Math.cos(th) * r, s.mh + Math.sin(th) * r, w, col);
      }
   }

   private void circle(KillFx fx, FxFrame f, SanctifyStyle.State s, double r, double w, int col) {
      for (int i = 0; i < 48; i++) {
         this.strip(
            fx, f, s, s.ma + Glass.RING_COS[i] * r, s.mh + Glass.RING_SIN[i] * r, s.ma + Glass.RING_COS[i + 1] * r, s.mh + Glass.RING_SIN[i + 1] * r, w, col
         );
      }
   }

   private void rose(KillFx fx, FxFrame f, SanctifyStyle.State s, boolean glow) {
      float open = roseOpen(fx);
      float alpha = fx.alpha() * Easing.clamp01(open * 3.0F) * (1.0F - Easing.smoothstep(0.2F, 0.85F, gone(fx)));
      if (!(open <= 0.01F) && !(alpha <= 0.003F)) {
         double sc = 0.3 * fx.size * open;
         double th = 0.5 * fx.t;
         double c = Math.cos(th);
         double sn = Math.sin(th);
         this.at(fx, f, s, s.ma, s.round ? s.top + 0.15 * fx.size : s.top - 0.45 * fx.size, 0.02);
         this.pane.ox = this.wx;
         this.pane.oy = this.wy;
         this.pane.oz = this.wz;
         this.pane.rx = s.rX * c;
         this.pane.ry = sn;
         this.pane.rz = s.rZ * c;
         this.pane.ux = -s.rX * sn;
         this.pane.uy = c;
         this.pane.uz = -s.rZ * sn;
         this.pane.nx = s.nX;
         this.pane.ny = 0.0;
         this.pane.nz = s.nZ;
         this.pane.scale = sc;
         this.pane.offZ = 0.0;
         this.pane.offY = 0.0;
         this.pane.offX = 0.0;
         Glass.Cells cells = Glass.rose();
         float light = swell(fx);
         float dim = fx.light;
         if (glow) {
            for (int i = 0; i < cells.n; i++) {
               int n = Glass.shard(cells, i, 0.0, 1.0, cells.cu[i], cells.cv[i], this.tu, this.tv);
               int col = s.light[cells.color[i]];
               Glass.glass(
                  this.pane, this.tu, this.tv, n, cells.cu[i], cells.cv[i], alpha(col, 0.35F * (0.3F + 0.7F * light) * alpha), alpha(col, 0.08F * alpha)
               );
            }

            Vfx.sprite(
               Textures.GLOW,
               this.pane.ox - s.nX * 0.03,
               this.pane.oy,
               this.pane.oz - s.nZ * 0.03,
               sc * 2.6,
               0.0F,
               alpha(this.color.getColor(0.5F), 0.3F * (0.4F + 0.6F * light) * alpha)
            );
         } else {
            RenderState.translucent(false);
            RenderState.decal(true);

            try {
               float k = dim * (1.0F + 0.6F * light);

               for (int i = 0; i < cells.n; i++) {
                  int n = Glass.shard(cells, i, 0.0, 1.0, cells.cu[i], cells.cv[i], this.tu, this.tv);
                  int base = ColorUtil.shade(s.light[cells.color[i]], k) | 0xFF000000;
                  Glass.glass(
                     this.pane,
                     this.tu,
                     this.tv,
                     n,
                     cells.cu[i],
                     cells.cv[i],
                     alpha(ColorUtil.lerp(base, -1, 0.15F), alpha * 0.86F),
                     alpha(ColorUtil.shade(base, 0.8F), alpha * 0.86F)
                  );
               }

               int col = alpha(ColorUtil.shade(this.lead.getColor(0.5F), dim) | 0xFF000000, alpha);

               for (int i = 0; i < cells.n; i++) {
                  int n = Glass.shard(cells, i, 0.0, 1.0, cells.cu[i], cells.cv[i], this.tu, this.tv);
                  Glass.lead(this.pane, this.tu, this.tv, n, 0.028, col);
               }

               Glass.ringLead(this.pane, 0.875, 1.0, col);
            } finally {
               RenderState.decal(false);
               RenderState.translucent(true);
            }
         }
      }
   }

   private void light(KillFx fx, FxFrame f, SanctifyStyle.State s, Camera cam) {
      float alpha = fx.alpha();
      float glassed = fx.phase(0.1F, 0.5F);
      float light = swell(fx);
      float gone = Easing.clamp01(gone(fx));
      float fade = 1.0F - Easing.smoothstep(0.4F, 1.0F, gone);
      float amount;
      if (this.groundLight.get() && (amount = (0.14F * glassed + 0.3F * light) * fade * alpha) > 0.003F) {
         double gy = f.ry(fx.ctx.groundY + 0.02);

         for (int i = 0; i < 4; i++) {
            double side = (i - 1.5) * 0.42 * fx.size;
            double out = (0.35 + 0.25 * (i % 2)) * fx.size + light * (0.9 + 0.35 * (i % 2)) * fx.size;
            double x = fx.ctx.feetX + s.rX * side + s.nX * out;
            double z = fx.ctx.feetZ + s.rZ * side + s.nZ * out;
            Glass.patch(f.rx(x), gy, f.rz(z), (float)((0.8 + 0.5 * light) * fx.size), alpha(s.light[i], amount));
         }
      }

      float win;
      if ((win = this.windowAlpha(fx, f, s, cam)) > 0.003F && light > 0.01F) {
         if (!GlassCells.available()) {
            this.gradientFan(
               fx,
               f,
               s,
               Glass.texture(),
               alpha(this.color.getColor(0.5F), 0.3F * light * win * fade),
               alpha(this.color.getColor(0.5F), 0.3F * light * win * fade),
               1.2
            );
         } else {
            this.glassCells(fx, f, s, win, light, gone(fx), fx.light, s.picture >= 0 && !fx.lod, 0.35F * light);
         }

         this.at(fx, f, s, s.ma, (s.bottom + s.top) * 0.5, -0.08);
         Vfx.sprite(Textures.GLOW, this.wx, this.wy, this.wz, (s.top - s.bottom) * 1.5, 0.0F, alpha(this.color.getColor(0.5F), 0.26F * light * fade * alpha));
         float strength = this.shafts.getFloat() / 100.0F * (fx.crit ? 1.5F : 1.0F);
         float a = 0.75F * strength * light * fade * win;
         if (a > 0.003F) {
            for (int k = 0; k < 5; k++) {
               double u = (k - 2.0) / 4.0;
               double a0 = u * s.width * 0.6;
               double h0 = s.bottom + (s.top - s.bottom) * (0.55 + 0.15 * Math.cos(k * 1.7));
               double reach = (1.0 + 0.35 * (k % 2)) * fx.size;
               this.at(fx, f, s, a0, h0, 0.02);
               double x0 = this.wx;
               double y0 = this.wy;
               double z0 = this.wz;
               this.at(fx, f, s, a0 * 2.2, 0.03, reach);
               Glass.shaft(cam, x0, y0, z0, this.wx, this.wy, this.wz, 0.3 * fx.size, 0.7 * fx.size, alpha(s.light[k % 4], a));
            }
         }
      }

      if (fx.crit) {
         this.rose(fx, f, s, true);
      }
   }

   @Override
   public String stateForTest(KillFx fx) {
      SanctifyStyle.State s = (SanctifyStyle.State)fx.data;
      if (s == null) {
         return null;
      } else {
         float t = fx.t;
         String phase = t < 0.5F ? "turning to glass" : (t < 1.0F ? "rising into the window" : (t < 1.6F ? "the light swelling" : "dissolving"));
         String pic = fx.lod ? "plain glass (far)" : (!s.tried ? "not yet" : (s.picture < 0 ? "plain glass (no portrait)" : "the figure"));
         double cell = 0.1 * this.cellSize.getFloat() / 100.0 * fx.size;
         int cells = (int)Math.round((s.round ? Math.PI * s.radius * s.radius : Glass.outlineArea(1) * s.width * s.width) / (cell * cell));
         return String.format(
            Locale.ROOT,
            "%s; %s window %.2f wide, %.2f high, %s, about %d cells of %.2f (%s), portrait %s (drawn in %.2f ms); window %.2f, light %.2f, gone %.2f, %d motes; crit rose %.2f; light %.2f",
            phase,
            this.window.get().displayName().toLowerCase(Locale.ROOT),
            s.width,
            s.top - s.bottom,
            this.glassColors.get().displayName(),
            cells,
            cell,
            GlassCells.stateForTest(),
            pic,
            s.pictureMillis,
            windowIn(fx),
            swell(fx),
            gone(fx),
            s.motes,
            roseOpen(fx),
            fx.light
         );
      }
   }

   public static enum Palette implements Displayable {
      SKIN("From the skin", "The figure's own colors, as glass"),
      CATHEDRAL("Cathedral", "Ruby, sapphire, emerald and gold"),
      TINTED("Tinted", "The three glass colors");

      private final String displayName;
      private final String description;

      private Palette(String displayName, String description) {
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
      final double[] oa = new double[52];
      final double[] oh = new double[52];
      int outline;
      double ma;
      double mh;
      double bottom;
      double top;
      double width;
      double radius;
      boolean round;
      double fig;
      double feet;
      int picture = -1;
      boolean tried;
      float pictureMillis;
      final int[] cells = new int[4];
      final int[] light = new int[4];
      final int[] back = new int[4];
      int palette;
      float motesDue;
      int motes;

      private State() {
      }
   }

   public static enum Window implements Displayable {
      ARCHED("Arched", "A tall pointed arch"),
      ROUND("Round", "A circle"),
      ROSE("Rose", "A circle with a rose window's tracery over it");

      private final String displayName;
      private final String description;

      private Window(String displayName, String description) {
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
