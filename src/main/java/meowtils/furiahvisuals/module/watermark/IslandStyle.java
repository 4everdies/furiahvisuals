package meowtils.furiahvisuals.module.watermark;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.ColorSetting;

public final class IslandStyle {
   private static final float FADE_SECONDS = 0.25F;
   private static final int LUT = 33;
   float font = 13.0F;
   float small = 10.5F;
   float height = 26.0F;
   float pad = 10.0F;
   float icon = 12.0F;
   float gap = 5.0F;
   float endPad = 3.0F;
   float radius = 13.0F;
   public int bg;
   public int outline;
   public int text;
   public int dim;
   public int muted;
   public int separator;
   public int accent;
   public int shadow;
   public int hover;
   public int glassTint;
   public int glassRim;
   public int danger;
   public int iconColor;
   public boolean light;
   public boolean glass;
   private Theme.Style shownStyle;
   private float fade = 1.0F;
   private final Theme.Palette target = new Theme.Palette();
   private final int[] from = new int[10];
   private final int[] to = new int[10];
   private boolean across;
   private float spanX;
   private float spanW = 1.0F;
   private int global;
   private final int[] accentLut = new int[33];
   private final int[] globalLut = new int[33];

   void size(float fontSize) {
      this.font = fontSize;
      float k = fontSize / 13.0F;
      this.small = 10.5F * k;
      this.height = Math.round(26.0F * k);
      this.pad = 10.0F * k;
      this.icon = 12.0F * k;
      this.gap = 5.0F * k;
      this.endPad = 3.0F * k;
      this.radius = this.height / 2.0F;
   }

   public void colors(Theme.Style style, int accentColor, float opacity, float dt) {
      Theme.fill(style, accentColor, this.target);
      boolean targetLight = this.target.light;
      this.to[0] = ColorUtil.mulAlpha(this.target.panel, opacity);
      this.to[1] = this.target.glass ? ColorUtil.withAlpha(this.target.glassRim, 0.28F) : this.target.panelBorder;
      this.to[2] = this.target.text;
      this.to[3] = this.target.textDim;
      this.to[4] = this.target.textMuted;
      this.to[5] = ColorUtil.withAlpha(this.target.text, targetLight ? 0.16F : 0.18F);
      this.to[6] = readable(accentColor, targetLight, this.target.glass);
      this.to[7] = this.target.shadow;
      this.to[8] = this.target.glass
         ? ColorUtil.mulAlpha(this.target.glassTint, Math.max(0.35F, opacity))
         : ColorUtil.withAlpha(this.target.panel, 0.15F + 0.5F * opacity);
      int n = this.to[9] = this.target.glass ? this.target.glassRim : -1711276033;
      if (this.shownStyle == null) {
         this.shownStyle = style;
         System.arraycopy(this.to, 0, this.from, 0, this.to.length);
      } else if (style != this.shownStyle) {
         this.from[0] = this.bg;
         this.from[1] = this.outline;
         this.from[2] = this.text;
         this.from[3] = this.dim;
         this.from[4] = this.muted;
         this.from[5] = this.separator;
         this.from[6] = this.accent;
         this.from[7] = this.shadow;
         this.from[8] = this.glassTint;
         this.from[9] = this.glassRim;
         this.shownStyle = style;
         this.fade = 0.0F;
      }

      this.fade = Math.min(1.0F, this.fade + dt / 0.25F);
      float t = this.fade * this.fade * (3.0F - 2.0F * this.fade);
      this.bg = ColorUtil.lerp(this.from[0], this.to[0], t);
      this.outline = ColorUtil.lerp(this.from[1], this.to[1], t);
      this.text = ColorUtil.lerp(this.from[2], this.to[2], t);
      this.dim = ColorUtil.lerp(this.from[3], this.to[3], t);
      this.muted = ColorUtil.lerp(this.from[4], this.to[4], t);
      this.separator = ColorUtil.lerp(this.from[5], this.to[5], t);
      this.accent = this.fade >= 1.0F ? this.to[6] : ColorUtil.lerp(this.from[6], this.to[6], t);
      this.shadow = ColorUtil.lerp(this.from[7], this.to[7], t);
      this.glassTint = ColorUtil.lerp(this.from[8], this.to[8], t);
      this.glassRim = ColorUtil.lerp(this.from[9], this.to[9], t);
      this.light = targetLight;
      this.glass = this.target.glass;
      this.hover = ColorUtil.withAlpha(this.accent, this.light ? 0.14F : 0.2F);
      this.iconColor = ColorUtil.lerp(this.dim, this.accent, 0.55F);
      this.danger = this.target.danger;
      if (this.fade >= 1.0F) {
         System.arraycopy(this.to, 0, this.from, 0, this.to.length);
      }
   }

   public boolean fading() {
      return this.fade < 1.0F;
   }

   void palette(ColorSetting accentSetting, boolean across, double time) {
      this.across = across;
      ColorSetting g = ColorSetting.getGlobal();
      int n = this.global = g == null ? this.accent : readable(g.getColor(0.0F, time), this.light, this.glass);
      if (across) {
         for (int i = 0; i < 33; i++) {
            float p = i / 32.0F;
            this.accentLut[i] = readable(accentSetting.getColor(p, time), this.light, this.glass);
            this.globalLut[i] = g == null ? this.accentLut[i] : readable(g.getColor(p, time), this.light, this.glass);
         }
      }
   }

   void span(float x, float w) {
      this.spanX = x;
      this.spanW = Math.max(1.0F, w);
   }

   boolean uniform() {
      return !this.across;
   }

   int accentAt(float x) {
      return this.across ? this.lut(this.accentLut, x) : this.accent;
   }

   int globalAt(float x) {
      return this.across ? this.lut(this.globalLut, x) : this.global;
   }

   int paint(IslandStyle.Paint p, float x) {
      switch (p) {
         case TEXT:
            return this.text;
         case ACCENT:
            return this.accentAt(x);
         case GLOBAL:
            return this.globalAt(x);
         default:
            return this.across ? ColorUtil.lerp(this.dim, this.accentAt(x), 0.55F) : this.iconColor;
      }
   }

   private int lut(int[] lut, float x) {
      float f = (x - this.spanX) / this.spanW * 32.0F;
      if (f <= 0.0F) {
         return lut[0];
      } else if (f >= 32.0F) {
         return lut[32];
      } else {
         int i = (int)f;
         return ColorUtil.lerp(lut[i], lut[i + 1], f - i);
      }
   }

   public static int readable(int color, boolean light, boolean glass) {
      return color | 0xFF000000;
   }

   static enum Paint {
      TEXT,
      SOFT,
      ACCENT,
      GLOBAL;
   }
}
