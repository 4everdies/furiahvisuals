package meowtils.furiahvisuals.module.kill.styles;

import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.look.LightBoost;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.BlockPos;

public final class TombstoneStyle extends KillStyleBase {
   private static final float RISE = 1.1F;
   private static final float SINK = 1.0F;
   private final ModeSetting<TombstoneStyle.Stone> stone = this.setting(
      new ModeSetting<>("tomb.stone", "Stone", "The gravestone's shape.", TombstoneStyle.Stone.class, TombstoneStyle.Stone.ROUNDED)
   );
   private final BooleanSetting showName = this.setting(new BooleanSetting("tomb.name", "Show Name", "The victim's name engraved on the front.", true));
   private final SliderSetting stay = this.setting(
      new SliderSetting("tomb.stay", "Stay Time", "How long the stone stays before it sinks back.", 1.0, 10.0, 4.0, 0.5).unit(" s")
   );
   private double ox;
   private double oy;
   private double oz;
   private double ground;
   private TombstoneStyle.State st;
   private float alpha;
   private FxFrame frame;

   public TombstoneStyle(KillEffectsModule module) {
      super(module, KillStyle.TOMBSTONE, new ColorSetting("tomb.color", "Glow", "Color of the soft glow around the stone.", -4205569));
   }

   @Override
   public float seconds(KillFx fx) {
      return 1.1F + this.stayTime(fx) + 1.0F;
   }

   private float stayTime(KillFx fx) {
      return fx.preview ? Math.min(1.5F, this.stay.getFloat()) : this.stay.getFloat();
   }

   @Override
   public void start(KillFx fx) {
      TombstoneStyle.State s = new TombstoneStyle.State();
      fx.data = s;
      double fx0 = -fx.ctx.dirX;
      double fz0 = -fx.ctx.dirZ;
      double l = Math.sqrt(fx0 * fx0 + fz0 * fz0);
      if (l < 1.0E-4) {
         fx0 = 0.0;
         fz0 = 1.0;
         l = 1.0;
      }

      s.fx = fx0 / l;
      s.rx = s.fz = fz0 / l;
      s.rz = -s.fx;
      if (!fx.preview && Minecraft.getMinecraft().theWorld != null) {
         BlockPos pos = new BlockPos(fx.ctx.feetX, fx.ctx.groundY + 0.5, fx.ctx.feetZ);
         s.brightness = Math.max(0.25F, LightBoost.lift(Minecraft.getMinecraft().theWorld.getLightBrightness(pos)));
      }
   }

   private float out(KillFx fx) {
      float t = fx.t;
      float up = Easing.outBack(Easing.clamp01((t - 0.2F) / 0.90000004F), 1.2F);
      float down = Easing.inCubic(Easing.clamp01((t - 1.1F - this.stayTime(fx)) / 1.0F));
      return Math.max(0.0F, Math.min(1.05F, up)) * (1.0F - down);
   }

   private double stoneHeight() {
      switch ((TombstoneStyle.Stone)this.stone.get()) {
         case CROSS:
            return 1.55;
         case OBELISK:
            return 1.9;
         default:
            return 1.05;
      }
   }

   @Override
   public void update(KillFx fx, float dt) {
      TombstoneStyle.State s = (TombstoneStyle.State)fx.data;
      float t = fx.t;
      boolean moving = t > 0.15F && t < 1.1F || t > 1.1F + this.stayTime(fx) && t < 1.1F + this.stayTime(fx) + 0.9F;
      if (moving && !fx.lod) {
         s.puffed += dt * 26.0F;

         while (s.puffed >= 1.0F) {
            s.puffed--;
            double a = fx.rng.next() * 6.2832;
            double r = 0.35 + fx.rng.next() * 0.3;
            fx.particles
               .spawn(
                  8,
                  fx.ctx.feetX + Math.cos(a) * r,
                  fx.ctx.groundY + 0.05,
                  fx.ctx.feetZ + Math.sin(a) * r,
                  (float)Math.cos(a) * 0.8F,
                  0.35F + fx.rng.next() * 0.3F,
                  (float)Math.sin(a) * 0.8F,
                  0.9F + fx.rng.next() * 0.5F,
                  (0.25F + fx.rng.next() * 0.2F) * fx.size
               )
               .end(0.6F * fx.size)
               .drag(1.6F)
               .color(-7569556)
               .alpha(0.55F * fx.opacity);
         }
      }
   }

   @Override
   public void render(KillFx fx, FxFrame f) {
      TombstoneStyle.State s = (TombstoneStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      float sink = Easing.inCubic(Easing.clamp01(t / 0.95F));
      if (sink < 1.0F) {
         double down = sink * 2.1;

         for (int i = 0; i < 6; i++) {
            BodyProxy.Part p = fx.parts[i];
            p.copy(fx.rest[i]);
            p.y -= down;
            p.clip = true;
            p.cnx = 0.0;
            p.cny = 1.0;
            p.cnz = 0.0;
            p.cd = fx.ctx.groundY + 0.002;
            p.capColor = 0;
         }

         fx.drawBody(f);
      }

      float o;
      if (!((o = this.out(fx)) <= 0.002F)) {
         double lift = (o - 1.0) * (this.stoneHeight() + 0.2);
         this.drawStone(fx, f, s, lift, a);
         RenderState.additive();
         float glow = a * Math.max(0.3F, fx.glow) * Easing.smoothstep(0.4F, 1.2F, t) * o;
         Vfx.sprite(
            Textures.GLOW,
            f.rx(fx.ctx.feetX),
            f.ry(fx.ctx.groundY + lift + this.stoneHeight() * 0.55),
            f.rz(fx.ctx.feetZ),
            this.stoneHeight() * 1.6 * fx.size,
            0.0F,
            alpha(this.color.getColor(0.5F), 0.14F * glow)
         );
         if (this.showName.get()) {
            this.engrave(fx, f, s, lift, a);
         }
      }
   }

   private void drawStone(KillFx fx, FxFrame f, TombstoneStyle.State s, double lift, float a) {
      this.st = s;
      this.frame = f;
      this.alpha = a;
      this.ox = fx.ctx.feetX;
      this.oy = fx.ctx.groundY + lift;
      this.oz = fx.ctx.feetZ;
      this.ground = fx.ctx.groundY;
      double k = fx.size;
      RenderState.translucent(true);
      switch ((TombstoneStyle.Stone)this.stone.get()) {
         case CROSS:
            this.box(-0.3 * k, 0.0, -0.2 * k, 0.3 * k, 0.18 * k, 0.2 * k);
            this.box(-0.11 * k, 0.18 * k, -0.09 * k, 0.11 * k, 1.55 * k, 0.09 * k);
            this.box(-0.42 * k, 0.95 * k, -0.09 * k, 0.42 * k, 1.17 * k, 0.09 * k);
            break;
         case OBELISK:
            this.box(-0.36 * k, 0.0, -0.36 * k, 0.36 * k, 0.16 * k, 0.36 * k);
            this.taper(0.16 * k, 1.55 * k, 0.23 * k, 0.14 * k);
            this.pyramid(1.55 * k, 1.9 * k, 0.14 * k);
            break;
         default:
            this.box(-0.5 * k, 0.0, -0.22 * k, 0.5 * k, 0.14 * k, 0.22 * k);
            this.rounded(0.14 * k, 0.62 * k, 0.4 * k, 0.1 * k);
      }

      Vfx.flush();
   }

   private double wx(double x, double z) {
      return this.ox + this.st.rx * x + this.st.fx * z;
   }

   private double wz(double x, double z) {
      return this.oz + this.st.rz * x + this.st.fz * z;
   }

   private void face(
      double x0,
      double y0,
      double z0,
      double x1,
      double y1,
      double z1,
      double x2,
      double y2,
      double z2,
      double x3,
      double y3,
      double z3,
      double nx,
      double ny,
      double nz
   ) {
      double l = 0.35 * nx + 0.8 * ny + 0.45 * nz;
      float shade = (float)(0.55 + 0.45 * Math.max(0.0, l)) * this.st.brightness;
      int g = Math.min(255, Math.round(255.0F * shade));
      int c = Math.max(0, Math.min(255, Math.round(this.alpha * 255.0F))) << 24 | g << 16 | g << 8 | g;
      double ax = Math.abs(nx);
      double ay = Math.abs(ny);
      Vfx.quadUV(
         Textures.STONE,
         this.frame.rx(this.wx(x0, z0)),
         this.frame.ry(Math.max(this.ground, this.oy + y0)),
         this.frame.rz(this.wz(x0, z0)),
         uv(ax, ay, x0, y0, z0, true),
         uv(ax, ay, x0, y0, z0, false),
         c,
         this.frame.rx(this.wx(x1, z1)),
         this.frame.ry(Math.max(this.ground, this.oy + y1)),
         this.frame.rz(this.wz(x1, z1)),
         uv(ax, ay, x1, y1, z1, true),
         uv(ax, ay, x1, y1, z1, false),
         c,
         this.frame.rx(this.wx(x2, z2)),
         this.frame.ry(Math.max(this.ground, this.oy + y2)),
         this.frame.rz(this.wz(x2, z2)),
         uv(ax, ay, x2, y2, z2, true),
         uv(ax, ay, x2, y2, z2, false),
         c,
         this.frame.rx(this.wx(x3, z3)),
         this.frame.ry(Math.max(this.ground, this.oy + y3)),
         this.frame.rz(this.wz(x3, z3)),
         uv(ax, ay, x3, y3, z3, true),
         uv(ax, ay, x3, y3, z3, false),
         c
      );
   }

   private static double uv(double ax, double ay, double x, double y, double z, boolean u) {
      if (ay > 0.7) {
         return (u ? x : z) * 0.9;
      } else {
         return ax > 0.7 ? (u ? z : -y) * 0.9 : (u ? x : -y) * 0.9;
      }
   }

   private void box(double x0, double y0, double z0, double x1, double y1, double z1) {
      this.face(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0.0, 0.0, 1.0);
      this.face(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0.0, 0.0, -1.0);
      this.face(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0.0, 1.0, 0.0);
      this.face(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1.0, 0.0, 0.0);
      this.face(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1.0, 0.0, 0.0);
   }

   private void taper(double y0, double y1, double b, double t) {
      double slope = (b - t) / (y1 - y0);
      this.face(-b, y0, b, b, y0, b, t, y1, t, -t, y1, t, 0.0, slope, 1.0);
      this.face(b, y0, -b, -b, y0, -b, -t, y1, -t, t, y1, -t, 0.0, slope, -1.0);
      this.face(b, y0, b, b, y0, -b, t, y1, -t, t, y1, t, 1.0, slope, 0.0);
      this.face(-b, y0, -b, -b, y0, b, -t, y1, t, -t, y1, -t, -1.0, slope, 0.0);
   }

   private void pyramid(double y0, double y1, double h) {
      this.face(-h, y0, h, h, y0, h, 0.0, y1, 0.0, 0.0, y1, 0.0, 0.0, 0.6, 1.0);
      this.face(h, y0, -h, -h, y0, -h, 0.0, y1, 0.0, 0.0, y1, 0.0, 0.0, 0.6, -1.0);
      this.face(h, y0, h, h, y0, -h, 0.0, y1, 0.0, 0.0, y1, 0.0, 1.0, 0.6, 0.0);
      this.face(-h, y0, -h, -h, y0, h, 0.0, y1, 0.0, 0.0, y1, 0.0, -1.0, 0.6, 0.0);
   }

   private void rounded(double y0, double y1, double r, double d) {
      int n = 12;
      this.face(-r, y0, d, r, y0, d, r, y1, d, -r, y1, d, 0.0, 0.0, 1.0);
      this.face(r, y0, -d, -r, y0, -d, -r, y1, -d, r, y1, -d, 0.0, 0.0, -1.0);
      this.face(r, y0, d, r, y0, -d, r, y1, -d, r, y1, d, 1.0, 0.0, 0.0);
      this.face(-r, y0, -d, -r, y0, d, -r, y1, d, -r, y1, -d, -1.0, 0.0, 0.0);

      for (int i = 0; i < n; i++) {
         double a0 = Math.PI * i / n;
         double a1 = Math.PI * (i + 1) / n;
         double x0 = Math.cos(a0) * r;
         double h0 = Math.sin(a0) * r;
         double x1 = Math.cos(a1) * r;
         double h1 = Math.sin(a1) * r;
         this.face(0.0, y1, d, x0, y1 + h0, d, x1, y1 + h1, d, x1, y1 + h1, d, 0.0, 0.0, 1.0);
         this.face(0.0, y1, -d, x1, y1 + h1, -d, x0, y1 + h0, -d, x0, y1 + h0, -d, 0.0, 0.0, -1.0);
         double nx = Math.cos((a0 + a1) / 2.0);
         double ny = Math.sin((a0 + a1) / 2.0);
         this.face(x0, y1 + h0, -d, x0, y1 + h0, d, x1, y1 + h1, d, x1, y1 + h1, -d, nx, ny, 0.0);
      }
   }

   private void engrave(KillFx fx, FxFrame f, TombstoneStyle.State s, double lift, float a) {
      if (!(a <= 0.05F)) {
         Minecraft mc = Minecraft.getMinecraft();
         FontRenderer font = mc.fontRendererObj;
         if (font != null) {
            double k = fx.size;
            double room;
            double textY;
            double faceZ;
            switch ((TombstoneStyle.Stone)this.stone.get()) {
               case CROSS:
                  faceZ = 0.09 * k;
                  textY = 1.06 * k;
                  room = 0.78 * k;
                  break;
               case OBELISK:
                  faceZ = 0.2 * k;
                  textY = 0.7 * k;
                  room = 0.36 * k;
                  break;
               default:
                  faceZ = 0.1 * k;
                  textY = 0.58 * k;
                  room = 0.7 * k;
            }

            String name = fx.ctx.victimName != null && !fx.ctx.victimName.isEmpty() ? fx.ctx.victimName : "?";
            int w = font.getStringWidth(name);
            double scale = Math.min(0.0125 * k, room / Math.max(1, w));
            double yaw = Math.toDegrees(Math.atan2(s.fx, -s.fz));
            float bright = s.brightness;
            int dark = shade(2894376, bright, a);
            int light = shade(12170412, bright, a);
            Vfx.flush();
            RenderState.suspend();
            GlStateManager.pushMatrix();

            try {
               GlStateManager.enableDepth();
               GlStateManager.depthMask(true);
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
               GlStateManager.disableLighting();
               double y = f.ry(fx.ctx.groundY + lift + textY);
               if (fx.ctx.groundY + lift + textY - 0.06 > fx.ctx.groundY) {
                  this.line(font, f, s, faceZ + 0.004, y, yaw, scale, name, dark, light);
                  if (this.stone.get() != TombstoneStyle.Stone.CROSS) {
                     double ry = f.ry(fx.ctx.groundY + lift + textY + (this.stone.get() == TombstoneStyle.Stone.OBELISK ? 0.28 : 0.2) * k);
                     if (fx.ctx.groundY + lift + textY + 0.14 > fx.ctx.groundY) {
                        this.line(font, f, s, faceZ + 0.004, ry, yaw, Math.min(scale * 1.1, 0.012 * k), "R.I.P.", dark, light);
                     }
                  }
               }
            } finally {
               GlStateManager.popMatrix();
               GlStateManager.disableBlend();
               RenderState.resume();
            }
         }
      }
   }

   private void line(FontRenderer font, FxFrame f, TombstoneStyle.State s, double faceZ, double y, double yaw, double scale, String text, int dark, int light) {
      GlStateManager.pushMatrix();
      GlStateManager.translate(f.rx(this.ox + s.fx * faceZ), y, f.rz(this.oz + s.fz * faceZ));
      GlStateManager.rotate((float)(-yaw), 0.0F, 1.0F, 0.0F);
      GlStateManager.scale((float)(-scale), (float)(-scale), (float)scale);
      int w = font.getStringWidth(text);
      font.drawString(text, -w / 2 + 0.6F, -3.4F, light, false);
      GlStateManager.translate(0.0F, 0.0F, -0.002F / (float)scale);
      font.drawString(text, -w / 2, -4.0F, dark, false);
      GlStateManager.popMatrix();
   }

   private static int shade(int rgb, float bright, float a) {
      int r = Math.round((rgb >> 16 & 0xFF) * bright);
      int g = Math.round((rgb >> 8 & 0xFF) * bright);
      int b = Math.round((rgb & 0xFF) * bright);
      int al = Math.max(8, Math.min(255, Math.round(a * 255.0F)));
      return al << 24 | r << 16 | g << 8 | b;
   }

   private static final class State {
      double rx;
      double rz;
      double fx;
      double fz;
      float brightness = 1.0F;
      float puffed;

      private State() {
      }
   }

   static enum Stone implements Displayable {
      ROUNDED("Rounded", "A classic headstone"),
      CROSS("Cross", "A stone cross"),
      OBELISK("Obelisk", "A tall, tapering pillar");

      private final String displayName;
      private final String description;

      private Stone(String displayName, String description) {
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
