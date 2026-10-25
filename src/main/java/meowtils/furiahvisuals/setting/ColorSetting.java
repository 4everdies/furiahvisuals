package meowtils.furiahvisuals.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.render.ColorUtil;

public class ColorSetting extends Setting {
   public static final double MIN_SPEED = 0.1;
   public static final double MAX_SPEED = 3.0;
   public static final double MAX_SCROLL = 3.0;
   private static final int LUT_SIZE = 64;
   private static ColorSetting global;
   private final boolean allowGlobal;
   private boolean secondary;
   private boolean skipSetAll;
   private final ColorSetting.Mode defaultMode;
   private final int defaultA;
   private final int defaultB;
   private final double defaultSpeed;
   private double defaultScroll;
   private float defaultSaturation = 0.85F;
   private float defaultBrightness = 1.0F;
   private float defaultSpread = 0.25F;
   private ColorSetting.Mode mode;
   private int colorA;
   private int colorB;
   private double chromaSpeed;
   private double scrollSpeed;
   private float chromaSaturation;
   private float chromaBrightness;
   private float chromaSpread;
   private final int[] gradientLut = new int[64];
   private boolean lutDirty = true;
   private final float[] hsvA = new float[3];

   public ColorSetting(String id, String name, String description, int defaultColor) {
      this(id, name, description, ColorSetting.Mode.GLOBAL, defaultColor, ColorUtil.core(defaultColor), 1.0);
   }

   public ColorSetting(String id, String name, String description, ColorSetting.Mode mode, int colorA, int colorB, double chromaSpeed) {
      this(id, name, description, mode, colorA, colorB, chromaSpeed, true);
   }

   private ColorSetting(String id, String name, String description, ColorSetting.Mode mode, int colorA, int colorB, double chromaSpeed, boolean allowGlobal) {
      super(id, name, description);
      this.allowGlobal = allowGlobal;
      ColorSetting.Mode start;
      this.defaultMode = start = !allowGlobal && mode == ColorSetting.Mode.GLOBAL ? ColorSetting.Mode.STATIC : mode;
      this.defaultA = colorA;
      this.defaultB = colorB;
      this.defaultSpeed = chromaSpeed;
      this.mode = start;
      this.colorA = colorA;
      this.colorB = colorB;
      this.chromaSpeed = chromaSpeed;
      this.chromaSaturation = this.defaultSaturation;
      this.chromaBrightness = this.defaultBrightness;
      this.chromaSpread = this.defaultSpread;
      ColorUtil.toHsv(colorA, this.hsvA);
   }

   public static ColorSetting global(String id, String name, String description, ColorSetting.Mode mode, int colorA, int colorB, double chromaSpeed) {
      return new ColorSetting(id, name, description, mode, colorA, colorB, chromaSpeed, false);
   }

   public static void setGlobal(ColorSetting setting) {
      global = setting;
   }

   public static ColorSetting getGlobal() {
      return global;
   }

   public static double clock() {
      return ClientClock.seconds();
   }

   public ColorSetting secondary() {
      this.secondary = true;
      return this;
   }

   public boolean allowsGlobal() {
      return this.allowGlobal;
   }

   public ColorSetting skipSetAll() {
      this.skipSetAll = true;
      return this;
   }

   public boolean followsSetAll() {
      return this.allowGlobal && !this.skipSetAll;
   }

   public int getColor(float position) {
      return this.getColor(position, clock());
   }

   public int getColor(float position, double time) {
      switch (this.mode) {
         case GLOBAL:
            if (global != null && global != this) {
               int c = global.getColor(position, time);
               return this.secondary && global.getMode() == ColorSetting.Mode.STATIC ? ColorUtil.core(c) : c;
            } else {
               return this.colorA;
            }
         case GRADIENT:
            return this.gradient(this.scrolled(position, time));
         case CHROMA:
            return this.chroma(position, time);
         default:
            return this.colorA;
      }
   }

   public int getDisplayColor() {
      switch (this.mode) {
         case GLOBAL:
            return global != null && global != this ? global.getDisplayColor() : this.colorA;
         case GRADIENT:
            return this.gradient(this.scrolled(0.0F, clock()));
         case CHROMA:
            return this.chroma(0.0F, clock());
         default:
            return this.colorA;
      }
   }

   public ColorSetting.Mode getEffectiveMode() {
      return this.mode == ColorSetting.Mode.GLOBAL && global != null && global != this ? global.getMode() : this.mode;
   }

   private float scrolled(float position, double time) {
      if (this.scrollSpeed <= 0.0) {
         return position;
      } else {
         double p = position + time * this.scrollSpeed * 0.25;
         double var6;
         return (float)((var6 = p - Math.floor(p / 2.0) * 2.0) > 1.0 ? 2.0 - var6 : var6);
      }
   }

   private int gradient(float position) {
      if (this.lutDirty) {
         for (int i2 = 0; i2 < 64; i2++) {
            this.gradientLut[i2] = ColorUtil.lerpOklab(this.colorA, this.colorB, i2 / 63.0F);
         }

         this.lutDirty = false;
      }

      float f;
      int i;
      return (i = (int)(f = (position < 0.0F ? 0.0F : (position > 1.0F ? 1.0F : position)) * 63.0F)) >= 63
         ? this.gradientLut[63]
         : ColorUtil.lerp(this.gradientLut[i], this.gradientLut[i + 1], f - i);
   }

   private int chroma(float position, double time) {
      double hue = this.hsvA[0] + time * this.chromaSpeed * 0.2 + position * this.chromaSpread;
      return ColorUtil.withAlpha(ColorUtil.hsv((float)(hue - Math.floor(hue)), this.chromaSaturation, this.chromaBrightness), ColorUtil.alpha(this.colorA));
   }

   public ColorSetting.Mode getMode() {
      return this.mode;
   }

   public void setMode(ColorSetting.Mode mode) {
      if (mode != null && mode != this.mode && (mode != ColorSetting.Mode.GLOBAL || this.allowGlobal)) {
         this.mode = mode;
         this.fireChanged();
      }
   }

   void setModeQuietly(ColorSetting.Mode mode) {
      this.mode = mode;
   }

   public int getColorA() {
      return this.colorA;
   }

   public int getDefaultColorA() {
      return this.defaultA;
   }

   public int getColorB() {
      return this.colorB;
   }

   public void setColorA(int color) {
      if (color != this.colorA) {
         this.colorA = color;
         ColorUtil.toHsv(this.colorA, this.hsvA);
         this.lutDirty = true;
         this.fireChanged();
      }
   }

   public void setColorB(int color) {
      if (color != this.colorB) {
         this.colorB = color;
         this.lutDirty = true;
         this.fireChanged();
      }
   }

   public double getChromaSpeed() {
      return this.chromaSpeed;
   }

   public void setChromaSpeed(double speed) {
      double v = Math.max(0.1, Math.min(3.0, Math.round(speed * 20.0) / 20.0));
      if (v != this.chromaSpeed) {
         this.chromaSpeed = v;
         this.fireChanged();
      }
   }

   public double getScrollSpeed() {
      return this.scrollSpeed;
   }

   public void setScrollSpeed(double speed) {
      double v = Math.max(0.0, Math.min(3.0, Math.round(speed * 20.0) / 20.0));
      if (v != this.scrollSpeed) {
         this.scrollSpeed = v;
         this.fireChanged();
      }
   }

   public float getChromaSaturation() {
      return this.chromaSaturation;
   }

   public void setChromaSaturation(float v) {
      float c = clamp01(Math.round(v * 100.0F) / 100.0F);
      if (c != this.chromaSaturation) {
         this.chromaSaturation = c;
         this.fireChanged();
      }
   }

   public float getChromaBrightness() {
      return this.chromaBrightness;
   }

   public void setChromaBrightness(float v) {
      float c = Math.max(0.1F, clamp01(Math.round(v * 100.0F) / 100.0F));
      if (c != this.chromaBrightness) {
         this.chromaBrightness = c;
         this.fireChanged();
      }
   }

   public float getChromaSpread() {
      return this.chromaSpread;
   }

   public void setChromaSpread(float v) {
      float c = clamp01(Math.round(v * 100.0F) / 100.0F);
      if (c != this.chromaSpread) {
         this.chromaSpread = c;
         this.fireChanged();
      }
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   @Override
   public void reset() {
      this.setMode(this.defaultMode);
      this.setColorA(this.defaultA);
      this.setColorB(this.defaultB);
      this.setChromaSpeed(this.defaultSpeed);
      this.setScrollSpeed(this.defaultScroll);
      this.setChromaSaturation(this.defaultSaturation);
      this.setChromaBrightness(this.defaultBrightness);
      this.setChromaSpread(this.defaultSpread);
   }

   @Override
   public JsonElement toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("mode", this.mode.name());
      o.addProperty("a", ColorUtil.toHexArgb(this.colorA));
      o.addProperty("b", ColorUtil.toHexArgb(this.colorB));
      o.addProperty("chromaSpeed", this.chromaSpeed);
      o.addProperty("scrollSpeed", this.scrollSpeed);
      o.addProperty("chromaSaturation", this.chromaSaturation);
      o.addProperty("chromaBrightness", this.chromaBrightness);
      o.addProperty("chromaSpread", this.chromaSpread);
      return o;
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json != null && json.isJsonObject()) {
         JsonObject o = json.getAsJsonObject();

         try {
            if (o.has("mode")) {
               String name = o.get("mode").getAsString();

               for (ColorSetting.Mode m : ColorSetting.Mode.values()) {
                  if (m.name().equalsIgnoreCase(name)) {
                     this.setMode(m);
                  }
               }
            }

            if (o.has("a")) {
               this.setColorA(ColorUtil.parseHex(o.get("a").getAsString(), this.colorA));
            }

            if (o.has("b")) {
               this.setColorB(ColorUtil.parseHex(o.get("b").getAsString(), this.colorB));
            }

            if (o.has("chromaSpeed")) {
               this.setChromaSpeed(o.get("chromaSpeed").getAsDouble());
            }

            if (o.has("scrollSpeed")) {
               this.setScrollSpeed(o.get("scrollSpeed").getAsDouble());
            }

            if (o.has("chromaSaturation")) {
               this.setChromaSaturation(o.get("chromaSaturation").getAsFloat());
               this.setChromaBrightness(o.has("chromaBrightness") ? o.get("chromaBrightness").getAsFloat() : this.chromaBrightness);
            } else {
               this.setChromaSaturation(Math.max(this.hsvA[1], 0.55F));
               this.setChromaBrightness(Math.max(this.hsvA[2], 0.75F));
            }

            if (o.has("chromaSpread")) {
               this.setChromaSpread(o.get("chromaSpread").getAsFloat());
            }
         } catch (RuntimeException var8) {
         }
      }
   }

   public static enum Mode implements Displayable {
      GLOBAL("Global", "Follows the Global Color in Settings"),
      STATIC("Static", "One color"),
      GRADIENT("Gradient", "Blend two colors across the effect"),
      CHROMA("Chroma", "Hue cycles through the rainbow");

      private final String displayName;
      private final String description;

      private Mode(String displayName, String description) {
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
