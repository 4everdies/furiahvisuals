package meowtils.furiahvisuals.module.watermark;

import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

final class FvItem extends IslandItem {
   private static final char[] LETTERS = new char[]{'F', 'V'};
   private static final int STRIPS = 3;
   private static final int GLITCH_RED = -50325;
   private static final int GLITCH_CYAN = -13244673;
   final ModeSetting<FvItem.Effect> effect;
   final SliderSetting speed;
   final SliderSetting depth;
   private float widthF = -1.0F;
   private float widthV;
   private int measuredPx;
   private double clock;
   private double clockOverride = Double.NaN;
   int lastColor;
   private final float[] box = new float[4];
   private final float[] projected = new float[2];
   static boolean colorCheck;

   FvItem(WaterMarkModule wm) {
      super(wm, "fv", "FV", "The mod's initials, always first.", null);
      this.shared(wm.fvColor);
      this.effect = this.option(
         new ModeSetting<>("fv.effect", "Effect", "An animation for the initials.", FvItem.Effect.class, FvItem.Effect.NONE).style(ModeSetting.Style.CHIPS)
      );
      this.speed = this.option(
         new SliderSetting("fv.speed", "Effect Speed", "How fast the effect runs.", 0.25, 3.0, 1.0, 0.05).unit("×").visibleWhen(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return FvItem.this.effect.get().moves();
            }
         })
      );
      this.depth = this.option(
         new SliderSetting("fv.depth", "3D Depth", "How thick the 3D letters are (at font size 13).", 1.0, 10.0, 4.0, 0.5).visibleWhen(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return FvItem.this.effect.get().is3d();
            }
         })
      );
   }

   @Override
   void frame(float dt, IslandDraw d, IslandStyle s) {
      this.clock = this.clock + dt * this.speed.get();
   }

   void effectTimeForTest(double seconds) {
      this.clockOverride = seconds;
   }

   private double time() {
      return Double.isNaN(this.clockOverride) ? this.clock : this.clockOverride;
   }

   @Override
   float measure(IslandDraw d, IslandStyle s) {
      int px = d.pixels(s.font);
      if (px != this.measuredPx) {
         this.measuredPx = px;
         this.widthF = d.heavyCharWidth(s.font, 'F');
         this.widthV = d.heavyCharWidth(s.font, 'V');
      }

      return this.widthF + this.widthV;
   }

   private int color(IslandStyle s, float x) {
      return s.paint(this.wm.fvColor.get().paint, x);
   }

   @Override
   void draw(IslandDraw d, IslandStyle s, float x, float cy) {
      this.measure(d, s);
      int cF = this.color(s, x + this.widthF / 2.0F);
      int cV = this.color(s, x + this.widthF + this.widthV / 2.0F);
      this.lastColor = cF;
      double t = this.time();
      switch (colorCheck ? FvItem.Effect.NONE : (FvItem.Effect)this.effect.get()) {
         case SPIN:
            this.solid(d, s, x, cy, (float)(t % 6.0 / 6.0 * Math.PI * 2.0), -0.18F, cF, cV);
            break;
         case SWING:
            this.solid(d, s, x, cy, 0.62F * (float)Math.sin(t * Math.PI * 2.0 / 4.0), -0.14F, cF, cV);
            break;
         case FLIP:
            double phase = t % 4.0;
            float flip = phase < 0.8 ? (float)(Easing.inOutCubic((float)(phase / 0.8)) * Math.PI * 2.0) : 0.0F;
            this.solid(d, s, x, cy, 0.34F, -0.14F + flip, cF, cV);
            break;
         case TILT:
            this.solid(d, s, x, cy, 0.38F, -0.16F, cF, cV);
            break;
         case WAVE:
            this.wave(d, s, x, cy, t, cF, cV);
            break;
         case PULSE:
            this.pulse(d, s, x, cy, t, cF, cV);
            break;
         case NEON:
            this.neon(d, s, x, cy, t, cF, cV);
            break;
         case GLITCH:
            this.glitch(d, s, x, cy, t, cF, cV);
            break;
         default:
            this.plain(d, s, x, cy, cF, cV);
      }
   }

   private void plain(IslandDraw d, IslandStyle s, float x, float cy, int cF, int cV) {
      float top = d.heavyMiddleTop(s.font, cy);
      d.heavyGlyph(s.font, 'F', x, top, cF);
      d.heavyGlyph(s.font, 'V', x + this.widthF, top, cV);
   }

   private float pen(int i) {
      return i == 0 ? 0.0F : this.widthF;
   }

   private void letter(IslandDraw d, IslandStyle s, int i, float x, float baseline, float k, float px, float py, float dx, float dy, int color) {
      if (d.heavyGlyphBox(s.font, LETTERS[i], this.box)) {
         float x0 = px + (x + this.pen(i) + this.box[0] - px) * k + dx;
         float x1 = px + (x + this.pen(i) + this.box[2] - px) * k + dx;
         float y0 = py + (baseline + this.box[1] - py) * k + dy;
         float y1 = py + (baseline + this.box[3] - py) * k + dy;
         d.heavyQuad(s.font, LETTERS[i], 0.0F, 1.0F, x0, y0, x1, y0, x1, y1, x0, y1, color);
      }
   }

   private void solid(IslandDraw d, IslandStyle s, float x, float cy, float yaw, float pitch, int cF, int cV) {
      float k = s.font / 13.0F;
      float thick = this.depth.getFloat() * k;
      float baseline = d.heavyMiddleTop(s.font, cy) + d.heavyAscent(s.font);
      float ox = x + (this.widthF + this.widthV) / 2.0F;
      int layers = Math.max(4, Math.min(28, (int)Math.ceil(thick * d.pixelsPerUnit() / 0.8F) + 1));
      float cosY = (float)Math.cos(yaw);
      float sinY = (float)Math.sin(yaw);
      float cosP = (float)Math.cos(pitch);
      float sinP = (float)Math.sin(pitch);
      float focal = 4.9F * s.font;
      boolean frontNearest = cosY * cosP >= 0.0F;

      for (int n = 0; n < layers; n++) {
         int layer = frontNearest ? layers - 1 - n : n;
         float z = thick / 2.0F - thick * layer / (layers - 1.0F);
         boolean face = n == layers - 1;
         float shade = 1.0F - (n + 1.0F) / layers;

         for (int i = 0; i < 2; i++) {
            int base = i == 0 ? cF : cV;
            int color = face ? base : ColorUtil.lerp(ColorUtil.shade(base, 0.74F), ColorUtil.shade(base, 0.4F), shade);
            this.slab(d, s, i, x, baseline, ox, cy, z, cosY, sinY, cosP, sinP, focal, color);
         }
      }
   }

   private void slab(
      IslandDraw d,
      IslandStyle s,
      int i,
      float x,
      float baseline,
      float ox,
      float oy,
      float z,
      float cosY,
      float sinY,
      float cosP,
      float sinP,
      float focal,
      int color
   ) {
      if (d.heavyGlyphBox(s.font, LETTERS[i], this.box)) {
         float lx0 = x + this.pen(i) + this.box[0] - ox;
         float lx1 = x + this.pen(i) + this.box[2] - ox;
         float ly0 = baseline + this.box[1] - oy;
         float ly1 = baseline + this.box[3] - oy;

         for (int j = 0; j < 3; j++) {
            float f0 = j / 3.0F;
            float f1 = (j + 1) / 3.0F;
            float a = lx0 + (lx1 - lx0) * f0;
            float b = lx0 + (lx1 - lx0) * f1;
            this.project(a, ly0, z, cosY, sinY, cosP, sinP, focal);
            float tlx = ox + this.projected[0];
            float tly = oy + this.projected[1];
            this.project(b, ly0, z, cosY, sinY, cosP, sinP, focal);
            float trx = ox + this.projected[0];
            float try0 = oy + this.projected[1];
            this.project(b, ly1, z, cosY, sinY, cosP, sinP, focal);
            float brx = ox + this.projected[0];
            float bry = oy + this.projected[1];
            this.project(a, ly1, z, cosY, sinY, cosP, sinP, focal);
            float blx = ox + this.projected[0];
            float bly = oy + this.projected[1];
            d.heavyQuad(s.font, LETTERS[i], f0, f1, tlx, tly, trx, try0, brx, bry, blx, bly, color);
         }
      }
   }

   private void project(float lx, float ly, float z, float cosY, float sinY, float cosP, float sinP, float focal) {
      float x1 = lx * cosY + z * sinY;
      float z1 = -lx * sinY + z * cosY;
      float y2 = ly * cosP - z1 * sinP;
      float z2 = ly * sinP + z1 * cosP;
      float k = focal / Math.max(focal * 0.25F, focal - z2);
      this.projected[0] = x1 * k;
      this.projected[1] = y2 * k;
   }

   private void wave(IslandDraw d, IslandStyle s, float x, float cy, double t, int cF, int cV) {
      float baseline = d.heavyMiddleTop(s.font, cy) + d.heavyAscent(s.font);
      float amp = 0.13F * s.font;

      for (int i = 0; i < 2; i++) {
         float dy = -amp * (float)Math.sin(t * Math.PI * 2.0 / 1.5 - i * 1.15);
         this.letter(d, s, i, x, baseline, 1.0F, 0.0F, 0.0F, 0.0F, dy, i == 0 ? cF : cV);
      }
   }

   private void pulse(IslandDraw d, IslandStyle s, float x, float cy, double t, int cF, int cV) {
      float beat = 0.5F + 0.5F * (float)Math.sin(t * Math.PI * 2.0 / 2.2);
      float k = 1.0F + 0.08F * beat;
      float cx = x + (this.widthF + this.widthV) / 2.0F;
      float glowA = 0.14F + 0.22F * beat;
      d.glow(x + this.widthF / 2.0F, cy, s.font * 2.4F * k, ColorUtil.withAlpha(cF, glowA));
      d.glow(x + this.widthF + this.widthV / 2.0F, cy, s.font * 2.4F * k, ColorUtil.withAlpha(cV, glowA));
      float baseline = d.heavyMiddleTop(s.font, cy) + d.heavyAscent(s.font);

      for (int i = 0; i < 2; i++) {
         this.letter(d, s, i, x, baseline, k, cx, cy, 0.0F, 0.0F, i == 0 ? cF : cV);
      }
   }

   private void neon(IslandDraw d, IslandStyle s, float x, float cy, double t, int cF, int cV) {
      long burst = (long)Math.floor(t / 4.5);
      double in = t - burst * 4.5;
      float on = 1.0F;

      for (int k = 0; k < 3; k++) {
         double at = 0.15 + hash(burst * 7L + k) * 0.9;
         double len = 0.04 + hash(burst * 13L + k) * 0.07;
         if (in >= at && in < at + len) {
            on = 0.28F;
         }
      }

      float top = d.heavyMiddleTop(s.font, cy);

      for (int i = 0; i < 2; i++) {
         int c = i == 0 ? cF : cV;
         float lx = x + this.pen(i) + (i == 0 ? this.widthF : this.widthV) / 2.0F;
         d.glow(lx, cy, s.font * 3.2F, ColorUtil.withAlpha(c, 0.34F * on));
         d.glow(lx, cy, s.font * 1.7F, ColorUtil.withAlpha(c, 0.3F * on));
         int core = ColorUtil.withAlpha(ColorUtil.lerp(c, -1, 0.3F * on), 0.45F + 0.55F * on);
         d.heavyGlyph(s.font, LETTERS[i], x + this.pen(i), top, core);
      }
   }

   private void glitch(IslandDraw d, IslandStyle s, float x, float cy, double t, int cF, int cV) {
      long burst = (long)Math.floor(t / 2.8);
      double in = t - burst * 2.8;
      float top = d.heavyMiddleTop(s.font, cy);
      if (in > 0.3) {
         this.plain(d, s, x, cy, cF, cV);
      } else {
         long step = burst * 16L + (long)(in / 0.05);
         float k = s.font / 13.0F;
         float split = (0.8F + 1.4F * hash(step * 3L)) * k;
         float lh = d.heavyLineHeight(s.font);

         for (int i = 0; i < 2; i++) {
            float px = x + this.pen(i);
            d.heavyGlyph(s.font, LETTERS[i], px - split, top, ColorUtil.withAlpha(-50325, 0.7F));
            d.heavyGlyph(s.font, LETTERS[i], px + split, top, ColorUtil.withAlpha(-13244673, 0.7F));
            int c = i == 0 ? cF : cV;

            for (int band = 0; band < 3; band++) {
               float b0 = top + lh * band / 3.0F;
               float b1 = top + lh * (band + 1) / 3.0F;
               float slip = (hash(step * 5L + band * 17L + i) - 0.5F) * 3.2F * k;
               d.heavyGlyphClipped(s.font, LETTERS[i], px + slip, top, c, b0, b1);
            }
         }
      }
   }

   private static float hash(long n) {
      long h = n * -7046029254386353131L;
      h ^= h >>> 29;
      h *= -4658895280553007687L;
      h ^= h >>> 32;
      return (float)(h >>> 40) / 1.6777216E7F;
   }

   @Override
   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      hit.set(this, x - s.pad, cy - s.height / 2.0F, this.widthF + this.widthV + s.pad * 2.0F, s.height, 0, "Furiah's Visuals", false);
      return true;
   }

   static enum Effect implements Displayable {
      NONE("None", "Plain letters"),
      SPIN("3D Spin", "Solid 3D letters turning slowly"),
      SWING("3D Swing", "Solid 3D letters rocking from side to side"),
      FLIP("3D Flip", "Solid 3D letters that flip over now and then"),
      TILT("3D Tilt", "Solid 3D letters at a slight angle, standing still"),
      WAVE("Wave", "The letters bob up and down in turn"),
      PULSE("Pulse", "The letters breathe, with a soft glow"),
      NEON("Neon", "A glowing sign that flickers now and then"),
      GLITCH("Glitch", "Now and then the letters glitch: a color split and shifted slices");

      private final String displayName;
      private final String description;

      private Effect(String displayName, String description) {
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

      boolean is3d() {
         return this == SPIN || this == SWING || this == FLIP || this == TILT;
      }

      boolean moves() {
         return this != NONE && this != TILT;
      }
   }
}
