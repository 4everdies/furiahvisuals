package meowtils.furiahvisuals.gui;

import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.Displayable;

public final class Theme {
   public static int ACCENT;
   public static int ACCENT_HOVER;
   public static int ACCENT_SOFT;
   public static int ACCENT_SOFT_HOVER;
   public static int ACCENT_FAINT;
   public static int ACCENT_SELECTION;
   public static int ACCENT_LINE;
   public static final int ON = -12723068;
   public static int OFF;
   public static int BACKDROP_TOP;
   public static int BACKDROP_BOTTOM;
   public static int SHADOW;
   public static int PANEL;
   public static int PANEL_SHEEN;
   public static int PANEL_BORDER;
   public static int PANEL_RIM;
   public static int SIDEBAR;
   public static int SURFACE;
   public static int SURFACE_HOVER;
   public static int TRACK;
   public static int DIVIDER;
   public static int PILL;
   public static int SCROLL_TRACK;
   public static int TEXT;
   public static int TEXT_DIM;
   public static int TEXT_MUTED;
   public static int DANGER;
   public static int KNOB;
   public static int OUTLINE;
   public static int OUTLINE_HOVER;
   public static int TOOLTIP;
   public static int TOOLTIP_BORDER;
   public static int PREVIEW_STAGE;
   public static boolean LIGHT;
   public static boolean GLASS;
   public static float PANEL_RADIUS;
   public static int GLASS_TINT;
   public static int GLASS_RIM;
   public static float GLASS_BEVEL;
   public static float GLASS_REFRACT;
   public static final int STAGE_TEXT = -920584;
   public static final int STAGE_TEXT_DIM = -6050627;
   public static final int PREVIEW_BACKDROP = -15723493;
   public static final int PREVIEW_GROUND = -15065305;
   public static final float RADIUS = 7.0F;
   public static final float TEXT_SIZE = 10.0F;
   public static final float SMALL = 8.5F;
   private static final Theme.Palette ACTIVE = new Theme.Palette();

   private Theme() {
   }

   public static void apply(Theme.Style style, int accent) {
      Theme.Palette p = fill(style, accent, ACTIVE);
      BACKDROP_TOP = p.backdropTop;
      BACKDROP_BOTTOM = p.backdropBottom;
      SHADOW = p.shadow;
      PANEL = p.panel;
      PANEL_SHEEN = p.panelSheen;
      PANEL_BORDER = p.panelBorder;
      PANEL_RIM = p.panelRim;
      SIDEBAR = p.sidebar;
      SURFACE = p.surface;
      SURFACE_HOVER = p.surfaceHover;
      TRACK = p.track;
      DIVIDER = p.divider;
      PILL = p.pill;
      SCROLL_TRACK = p.scrollTrack;
      TEXT = p.text;
      TEXT_DIM = p.textDim;
      TEXT_MUTED = p.textMuted;
      DANGER = p.danger;
      KNOB = p.knob;
      OUTLINE = p.outline;
      OUTLINE_HOVER = p.outlineHover;
      TOOLTIP = p.tooltip;
      TOOLTIP_BORDER = p.tooltipBorder;
      PREVIEW_STAGE = p.previewStage;
      LIGHT = p.light;
      GLASS = p.glass;
      PANEL_RADIUS = p.panelRadius;
      GLASS_TINT = p.glassTint;
      GLASS_RIM = p.glassRim;
      GLASS_BEVEL = p.glassBevel;
      GLASS_REFRACT = p.glassRefract;
      OFF = DANGER;
      ACCENT = accent | 0xFF000000;
      ACCENT_HOVER = hover(ACCENT);
      ACCENT_SOFT = tint(ACCENT, 0.2F);
      ACCENT_SOFT_HOVER = tint(ACCENT, 0.3F);
      ACCENT_FAINT = tint(ACCENT, 0.1F);
      ACCENT_SELECTION = tint(ACCENT, 0.35F);
      ACCENT_LINE = tint(ACCENT, 0.5F);
   }

   public static int tint(int accent, float amount) {
      return amount >= 1.0F ? accent | 0xFF000000 : ColorUtil.lerp(PANEL | 0xFF000000, accent | 0xFF000000, Math.max(0.0F, amount)) | 0xFF000000;
   }

   public static int hover(int accent) {
      return ColorUtil.lerp(accent | 0xFF000000, LIGHT ? -16777216 : -1, 0.12F) | 0xFF000000;
   }

   public static Theme.Palette fill(Theme.Style style, int accent, Theme.Palette p) {
      p.danger = -43674;
      p.knob = -526085;
      p.light = false;
      p.glass = false;
      p.panelRim = 0;
      p.panelRadius = 14.0F;
      p.glassTint = 0;
      p.glassRim = -1;
      p.glassBevel = 0.0F;
      p.glassRefract = 0.0F;
      switch (style) {
         case LIGHT:
            p.light = true;
            p.backdropTop = 1073741824;
            p.backdropBottom = 1929379840;
            p.shadow = 1711276032;
            p.panel = -118229511;
            p.panelSheen = -1711276033;
            p.panelBorder = 520093696;
            p.sidebar = 253436467;
            p.surface = 184549376;
            p.surfaceHover = 369098752;
            p.track = 553648128;
            p.divider = 369098752;
            p.pill = 268435456;
            p.scrollTrack = 301989888;
            p.text = -15065819;
            p.textDim = -10919565;
            p.textMuted = -7103832;
            p.danger = -2083766;
            p.knob = -1;
            p.outline = 469762048;
            p.outlineHover = 939524096;
            p.tooltip = -117440513;
            p.tooltipBorder = 603979776;
            p.previewStage = -350807519;
            break;
         case LIQUID_GLASS:
            glassCommon(p);
            p.backdropTop = 335544320;
            p.backdropBottom = 1191182336;
            p.panel = -1944840160;
            p.panelSheen = 620756991;
            p.panelBorder = 872415231;
            p.glassTint = ColorUtil.withAlpha(-15986922, 0.4F);
            p.glassRim = -1;
            p.text = -1;
            p.textDim = -1972497;
            p.textMuted = -5195578;
            p.tooltip = -1038475736;
            p.tooltipBorder = 1728053247;
            break;
         case TINTED_GLASS:
            glassCommon(p);
            int tint = accent | 0xFF000000;
            int rim = ColorUtil.brighten(tint, 0.55F);
            p.backdropTop = 520093696;
            p.backdropBottom = 1543503872;
            p.panel = ColorUtil.withAlpha(ColorUtil.lerp(-15723750, tint, 0.3F), 0.8F);
            p.panelSheen = ColorUtil.withAlpha(ColorUtil.brighten(tint, 0.7F), 0.18F);
            p.panelBorder = ColorUtil.withAlpha(rim, 0.4F);
            p.glassTint = ColorUtil.withAlpha(ColorUtil.lerp(-15986922, tint, 0.42F), 0.52F);
            p.glassRim = rim;
            p.sidebar = ColorUtil.withAlpha(ColorUtil.brighten(tint, 0.5F), 0.07F);
            p.surface = ColorUtil.withAlpha(ColorUtil.brighten(tint, 0.65F), 0.08F);
            p.surfaceHover = ColorUtil.withAlpha(ColorUtil.brighten(tint, 0.65F), 0.15F);
            p.divider = ColorUtil.withAlpha(rim, 0.2F);
            p.pill = ColorUtil.withAlpha(rim, 0.14F);
            p.text = -1;
            p.textDim = ColorUtil.lerp(-1972497, rim, 0.2F);
            p.textMuted = ColorUtil.lerp(-5590334, rim, 0.3F);
            p.outline = ColorUtil.withAlpha(rim, 0.28F);
            p.outlineHover = ColorUtil.withAlpha(rim, 0.55F);
            p.tooltip = ColorUtil.withAlpha(ColorUtil.lerp(-15328992, tint, 0.22F), 0.9F);
            p.tooltipBorder = ColorUtil.withAlpha(rim, 0.5F);
            break;
         default:
            p.backdropTop = 1929379840;
            p.backdropBottom = -1342177280;
            p.shadow = -1946157056;
            p.panel = -200404970;
            p.panelSheen = 218103807;
            p.panelBorder = 486539263;
            p.sidebar = 1493172224;
            p.surface = 218103807;
            p.surfaceHover = 385875967;
            p.track = 654311423;
            p.divider = 352321535;
            p.pill = 318767103;
            p.scrollTrack = 234881023;
            p.text = -920584;
            p.textDim = -6642763;
            p.textMuted = -10656653;
            p.outline = 536870911;
            p.outlineHover = 1174405119;
            p.tooltip = -233301212;
            p.tooltipBorder = 536870911;
            p.previewStage = 771751936;
      }

      return p;
   }

   private static void glassCommon(Theme.Palette p) {
      p.glass = true;
      p.panelRadius = 28.0F;
      p.glassBevel = 28.0F;
      p.glassRefract = 20.0F;
      p.shadow = 1493172224;
      p.panelRim = 452984831;
      p.sidebar = 234881023;
      p.surface = 318767103;
      p.surfaceHover = 620756991;
      p.track = 956301311;
      p.divider = 654311423;
      p.pill = 486539263;
      p.scrollTrack = 452984831;
      p.knob = -1;
      p.outline = 872415231;
      p.outlineHover = 1811939327;
      p.previewStage = 1795491595;
   }

   public static int readableAccent(int color) {
      return color | 0xFF000000;
   }

   static {
      apply(Theme.Style.DARK, -7635713);
   }

   public static final class Palette {
      public int backdropTop;
      public int backdropBottom;
      public int shadow;
      public int panel;
      public int panelSheen;
      public int panelBorder;
      public int panelRim;
      public int sidebar;
      public int surface;
      public int surfaceHover;
      public int track;
      public int divider;
      public int pill;
      public int scrollTrack;
      public int text;
      public int textDim;
      public int textMuted;
      public int danger;
      public int knob;
      public int outline;
      public int outlineHover;
      public int tooltip;
      public int tooltipBorder;
      public int previewStage;
      public boolean light;
      public boolean glass;
      public float panelRadius;
      public int glassTint;
      public int glassRim;
      public float glassBevel;
      public float glassRefract;
   }

   public static enum Style implements Displayable {
      DARK("Dark Mode", "Deep, solid and calm"),
      LIGHT("Light Mode", "Bright paper-white panels"),
      LIQUID_GLASS("Liquid Glass", "Frosted glass that bends the light"),
      TINTED_GLASS("Tinted Glass", "Liquid glass in your effect color");

      private final String displayName;
      private final String description;

      private Style(String displayName, String description) {
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
