package meowtils.furiahvisuals.module.watermark;

import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.module.misc.MiscHooks;
import meowtils.furiahvisuals.module.watermark.data.WeatherService;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

final class WeatherItem extends IslandItem {
   final ModeSetting<WeatherItem.WeatherTint> tint;
   final ModeSetting<WeatherItem.UnitText> unitText;
   private final RollingText temp = new RollingText();
   private IslandIcons.Icon icon = IslandIcons.Icon.CLOUD;
   private WeatherService.Kind kind = WeatherService.Kind.CLOUDS;
   private float celsius;
   private int shownTemp = Integer.MIN_VALUE;
   private boolean hasValue;
   private String condition = "";
   private float unitW;
   private int measuredPx;
   private String unitShown = "";
   private String measuredUnit = "";

   WeatherItem(WaterMarkModule wm) {
      super(
         wm,
         "weather",
         "Local Weather",
         "The weather: your city's real weather, or the world's. (This offline build shows the world's.)",
         IslandIcons.Icon.PARTLY
      );
      this.iconChoice(IslandIcons.Icon.PARTLY, new IslandIcons.Icon[]{IslandIcons.Icon.PARTLY, IslandIcons.Icon.THERMO})
         .tip(IslandIcons.Icon.PARTLY, "The weather's own icon: sun or moon, clouds, rain, snow or storm")
         .tip(IslandIcons.Icon.THERMO, "A thermometer");
      this.tint = this.option(
         new ModeSetting<>("weather.iconColor", "Icon Color", "The icon's color.", WeatherItem.WeatherTint.class, WeatherItem.WeatherTint.ACCENT)
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return WeatherItem.this.iconChoice.get() != null;
               }
            })
      );
      this.unitText = this.option(
         new ModeSetting<>("weather.unitText", "Unit Text", "What follows the temperature.", WeatherItem.UnitText.class, WeatherItem.UnitText.FULL)
      );
      this.shared(wm.weatherUnits);
   }

   @Override
   void tick() {
      Minecraft mc = Minecraft.getMinecraft();
      WorldClient w = mc.theWorld;
      if (w != null && mc.thePlayer != null) {
         BlockPos pos = new BlockPos(mc.thePlayer);
         BiomeGenBase biome = w.getBiomeGenForCoords(pos);
         float t = biome.getFloatTemperature(pos);
         this.celsius = t * 25.0F - 5.0F;
         final boolean cold = t < 0.15F;
         final World world = w;
         MiscHooks.real(new Runnable() {
            @Override
            public void run() {
               float angle = world.getCelestialAngle(1.0F);
               boolean day = angle < 0.26F || angle > 0.74F;
               if (world.isThundering()) {
                  WeatherItem.this.kind = WeatherService.Kind.STORM;
               } else if (world.isRaining()) {
                  WeatherItem.this.kind = cold ? WeatherService.Kind.SNOW : WeatherService.Kind.RAIN;
               } else {
                  WeatherItem.this.kind = day ? WeatherService.Kind.SUN : WeatherService.Kind.MOON;
               }
            }
         });
         this.condition = describe(this.kind) + " in this world (" + biome.biomeName + ")";
         this.hasValue = true;
         this.icon = iconOf(this.kind);
         int shown = Math.round(this.wm.weatherUnits.get() == WaterMarkModule.Units.FAHRENHEIT ? this.celsius * 9.0F / 5.0F + 32.0F : this.celsius);
         this.setTemp(shown);
      } else {
         this.hasValue = false;
         this.setTemp(Integer.MIN_VALUE);
      }
   }

   @Override
   String panelValue() {
      return !this.hasValue
         ? "No weather yet"
         : this.shownTemp + (this.wm.weatherUnits.get() == WaterMarkModule.Units.FAHRENHEIT ? "°F " : "°C ") + describe(this.kind);
   }

   @Override
   String panelDescription() {
      return "Local weather";
   }

   private void setTemp(int t) {
      if (t != this.shownTemp || this.temp.get().isEmpty()) {
         this.shownTemp = t;
         this.temp.set(t == Integer.MIN_VALUE ? "—" : Integer.toString(t), t, this.wm.rolling());
      }
   }

   static IslandIcons.Icon iconOf(WeatherService.Kind kind) {
      switch (kind) {
         case SUN:
            return IslandIcons.Icon.SUN;
         case MOON:
            return IslandIcons.Icon.MOON;
         case RAIN:
            return IslandIcons.Icon.RAIN;
         case SNOW:
            return IslandIcons.Icon.SNOW;
         case STORM:
            return IslandIcons.Icon.STORM;
         default:
            return IslandIcons.Icon.CLOUD;
      }
   }

   private static String describe(WeatherService.Kind kind) {
      switch (kind) {
         case SUN:
            return "Clear";
         case MOON:
            return "Clear night";
         case RAIN:
            return "Rain";
         case SNOW:
            return "Snow";
         case STORM:
            return "Thunderstorm";
         default:
            return "Clouds";
      }
   }

   private int weatherColor(IslandStyle s) {
      int c;
      if (this.iconChoice.get() == IslandIcons.Icon.THERMO) {
         c = !this.hasValue ? -3550240 : (this.celsius < 0.0F ? -8402689 : (this.celsius < 15.0F ? -7347264 : (this.celsius < 26.0F ? -15043 : -38070)));
      } else {
         switch (this.kind) {
            case SUN:
               c = -15043;
               break;
            case MOON:
               c = -2695425;
               break;
            case RAIN:
               c = -10833665;
               break;
            case SNOW:
               c = -1838337;
               break;
            case STORM:
               c = -5010177;
               break;
            default:
               c = -3550240;
         }
      }

      return IslandStyle.readable(c, s.light, s.glass);
   }

   private int color(IslandStyle s, float x) {
      switch ((WeatherItem.WeatherTint)this.tint.get()) {
         case WEATHER:
            return this.weatherColor(s);
         case SOFT:
            return s.paint(IslandStyle.Paint.SOFT, x);
         case TEXT:
            return s.paint(IslandStyle.Paint.TEXT, x);
         case GLOBAL:
            return s.paint(IslandStyle.Paint.GLOBAL, x);
         default:
            return s.paint(IslandStyle.Paint.ACCENT, x);
      }
   }

   @Override
   void frame(float dt, IslandDraw d, IslandStyle s) {
      this.temp.update(dt);
   }

   private float iconSize(IslandStyle s) {
      return s.icon * 1.15F;
   }

   @Override
   float iconSpace(IslandStyle s) {
      return this.showsIcon() ? this.iconSize(s) + s.gap : 0.0F;
   }

   @Override
   float measure(IslandDraw d, IslandStyle s) {
      boolean f = this.wm.weatherUnits.get() == WaterMarkModule.Units.FAHRENHEIT;
      String unit = this.unitText.get() == WeatherItem.UnitText.OFF ? "" : (this.unitText.get() == WeatherItem.UnitText.DEGREE ? "°" : (f ? "°F" : "°C"));
      int px = d.pixels(s.small);
      if (px != this.measuredPx || !unit.equals(this.measuredUnit)) {
         this.measuredPx = px;
         this.measuredUnit = unit;
         this.unitShown = unit;
         this.unitW = unit.isEmpty() ? 0.0F : d.width(false, s.small, unit);
      }

      return this.iconSpace(s) + this.temp.width(d, false, s.font) + (this.hasValue ? this.unitW : 0.0F);
   }

   @Override
   void draw(IslandDraw d, IslandStyle s, float x, float cy) {
      if (this.showsIcon()) {
         float size = this.iconSize(s);
         IslandIcons.Icon shown = this.iconChoice.get() == IslandIcons.Icon.PARTLY ? this.icon : this.iconChoice.get();
         this.lastIconColor = this.color(s, x + size / 2.0F);
         d.icon(shown, x, cy - size / 2.0F, size, this.lastIconColor);
         x += size + s.gap;
      }

      this.temp.draw(d, false, s.font, x, cy, this.hasValue ? s.text : s.muted);
      if (this.hasValue && this.unitW > 0.0F) {
         d.text(false, s.small, this.unitShown, x + this.temp.width(d, false, s.font), d.middleTop(false, s.font, cy) + 1.0F, s.dim);
      }
   }

   @Override
   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      hit.set(this, x - s.pad, cy - s.height / 2.0F, this.measure(this.wm.draw(), s) + s.pad * 2.0F, s.height, 0, this.condition, false);
      return true;
   }

   static enum UnitText implements Displayable {
      FULL("°C / °F", "The degree sign and the unit: 21°C"),
      DEGREE("° only", "Only the degree sign: 21°"),
      OFF("Off", "Only the number: 21");

      private final String displayName;
      private final String description;

      private UnitText(String displayName, String description) {
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

   static enum WeatherTint implements Displayable {
      WEATHER("Weather", "Sun yellow, rain blue, snow white... (a thermometer shows how warm it is)"),
      ACCENT("Accent", "The island's accent"),
      SOFT("Soft", "The text color with a touch of the accent"),
      TEXT("Text", "The text color"),
      GLOBAL("Global", "The Global Color");

      private final String displayName;
      private final String description;

      private WeatherTint(String displayName, String description) {
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
