package meowtils.furiahvisuals.module.sky;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class SkySettings {
   static final double SMOOTH_SECONDS = 1.5;
   public final SkyStyle sky;
   public final SliderSetting speed;
   public final SliderSetting scale;
   public final SliderSetting rotation;
   public final SliderSetting parallax;
   public final SliderSetting fadeIn;
   public final ModeSetting<SkyModule.ColorSource> colorSource;
   public final ColorSetting color;
   public final SliderSetting brightness;
   public final SliderSetting saturation;
   public final SliderSetting contrast;
   public final BooleanSetting matchFog;
   public final SliderSetting fogBlend;
   public final ModeSetting<SkyModule.Show> show;
   public final SliderSetting dayBrightness;
   public final BooleanSetting weather;
   public final BooleanSetting reactMusic;
   public final SliderSetting musicAmount;
   public final ModeSetting<SkyModule.Element> sunMoon;
   public final ModeSetting<SkyModule.Clouds> clouds;
   public final BooleanSetting stars;
   public final SliderSetting starDensity;
   public final SliderSetting twinkle;
   public final ColorSetting starColor;
   public final SliderSetting shootingStars;
   public final BooleanSetting overworld;
   public final BooleanSetting nether;
   public final BooleanSetting end;
   public final ModeSetting<SkyModule.Blend> blend;
   public final SliderSetting opacity;
   public final ModeSetting<SkyModule.Quality> quality;
   public final BooleanSetting offWithShaders;
   public final ModeSetting<SkyModule.OptiFineSkies> optifineSkies;
   final List<Setting> common = new ArrayList<>();
   private Setting[] own = new Setting[0];
   private final Map<Setting, JsonElement> defaults = new IdentityHashMap<>();
   private boolean modifiedKnown;
   private boolean modified;

   SkySettings(SkyStyle sky, SettingStyle owner, Supplier<String> noMusic) {
      this.sky = sky;
      BooleanSupplier custom = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return SkySettings.this.colorSource.get() == SkyModule.ColorSource.CUSTOM;
         }
      };
      BooleanSupplier starsOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return SkySettings.this.stars.get();
         }
      };
      this.speed = this.add(
         new SliderSetting(this.id("speed"), "Animation Speed", "How fast the sky moves. 0 freezes it.", 0.0, 3.0, 1.0, 0.05).unit("x").group("Motion"), owner
      );
      this.scale = this.add(
         new SliderSetting(this.id("scale"), "Pattern Size", "Size of the shapes in the sky.", 0.5, 2.0, 1.0, 0.05).unit("x").group("Motion"), owner
      );
      this.rotation = this.add(
         new SliderSetting(this.id("rotation"), "Rotation", "Slowly turns the whole sky; below 0 it turns the other way.", -6.0, 6.0, 0.0, 0.1)
            .unit("°/s")
            .group("Motion"),
         owner
      );
      this.parallax = this.add(
         new SliderSetting(this.id("parallax"), "Parallax", "The sky shifts a little as you move around, for depth.", 0.0, 1.0, 0.3, 0.05).group("Motion"),
         owner
      );
      this.fadeIn = this.add(
         new SliderSetting(
               this.id("fadeIn"), "Fade-In", "How long this sky takes to crossfade in when you switch to it. 0 switches at once.", 0.0, 5.0, 1.5, 0.1
            )
            .unit("s")
            .<SliderSetting>group("Motion")
            .keywords(new String[]{"crossfade", "fade", "transition", "smooth switching"}),
         owner
      );
      this.colorSource = this.add(
         new ModeSetting<>(this.id("colorSource"), "Colors", "Where the sky's colors come from.", SkyModule.ColorSource.class, SkyModule.ColorSource.STYLE)
            .group("Color"),
         owner
      );
      this.color = this.add(
         new ColorSetting(
               this.id("color"), "Sky Color", "Your colors for the sky: one, a gradient of two, or chroma.", ColorSetting.Mode.GRADIENT, -50291, -9806593, 1.0
            )
            .<ColorSetting>group("Color")
            .visibleWhen(custom),
         owner
      );
      this.brightness = this.add(new SliderSetting(this.id("brightness"), "Brightness", "How bright the sky is.", 0.2, 1.5, 1.0, 0.05).group("Color"), owner);
      this.saturation = this.add(
         new SliderSetting(this.id("saturation"), "Saturation", "How vivid its colors are. 0 is black and white.", 0.0, 1.5, 1.0, 0.05).group("Color"), owner
      );
      this.contrast = this.add(
         new SliderSetting(this.id("contrast"), "Contrast", "Deeper darks and brighter lights (below 1: softer).", 0.5, 1.5, 1.0, 0.05).group("Color"), owner
      );
      this.matchFog = this.add(
         new BooleanSetting(
               this.id("matchFog"),
               "Match Fog",
               "The fog takes the sky's horizon color, so the world melts into the sky (Ambience's Atmosphere tint wins where it is on).",
               true
            )
            .<BooleanSetting>group("Color")
            .keywords(new String[]{"fog", "horizon", "tint"}),
         owner
      );
      this.fogBlend = this.add(
         new SliderSetting(this.id("fogBlend"), "Fog Blend", "How far the fog's color goes towards the sky's.", 0.0, 1.0, 0.6, 0.05)
            .<SliderSetting>group("Color")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return SkySettings.this.matchFog.get();
               }
            }),
         owner
      );
      this.show = this.add(
         new ModeSetting<>(this.id("show"), "Show", "When this sky shows.", SkyModule.Show.class, SkyModule.Show.ALWAYS)
            .<ModeSetting>group("When")
            .keywords(new String[]{"night", "day", "dusk"}),
         owner
      );
      this.dayBrightness = this.add(
         new SliderSetting(this.id("dayBrightness"), "Day Brightness", "How bright this sky is in the daytime (1: as at night).", 0.2, 1.5, 1.0, 0.05)
            .<SliderSetting>group("When")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return SkySettings.this.show.get() == SkyModule.Show.ALWAYS;
               }
            }),
         owner
      );
      this.reactMusic = this.add(
         new BooleanSetting(
               this.id("reactMusic"),
               "React to Music",
               "The sky pulses brighter and moves faster on the bass of the music you play (heard by the WaterMark's Audio module; game sounds and ignored apps never count).",
               false
            )
            .<BooleanSetting>group("Music")
            .<BooleanSetting>keywords(new String[]{"audio", "beat", "bass", "pulse", "visualizer"})
            .unavailableWhen(noMusic),
         owner
      );
      this.musicAmount = this.add(
         new SliderSetting(this.id("musicAmount"), "Music Amount", "How strongly it reacts.", 0.0, 1.0, 0.6, 0.05)
            .<SliderSetting>group("Music")
            .<SliderSetting>visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return SkySettings.this.reactMusic.get();
               }
            })
            .unavailableWhen(noMusic),
         owner
      );
      this.sunMoon = this.add(
         new ModeSetting<>(
               this.id("sunMoon"), "Sun & Moon", "Minecraft's sun and moon, none, or the sky's own.", SkyModule.Element.class, SkyModule.Element.STYLE
            )
            .group("Sky Elements"),
         owner
      );
      this.clouds = this.add(
         new ModeSetting<>(
               this.id("clouds"),
               "Clouds",
               "Minecraft's clouds (as they are, or tinted to the sky), none, or the sky's own.",
               SkyModule.Clouds.class,
               SkyModule.Clouds.STYLE
            )
            .<ModeSetting>group("Sky Elements")
            .keywords(new String[]{"vanilla clouds"}),
         owner
      );
      this.stars = this.add(new BooleanSetting(this.id("stars"), "Stars", "Stars wherever the sky is dark.", true).group("Sky Elements"), owner);
      this.starDensity = this.add(
         new SliderSetting(this.id("starDensity"), "Star Density", "How many stars there are.", 0.05, 1.0, 0.5, 0.05)
            .<SliderSetting>group("Sky Elements")
            .visibleWhen(starsOn),
         owner
      );
      this.twinkle = this.add(
         new SliderSetting(this.id("twinkle"), "Twinkle", "How much the stars flicker.", 0.0, 1.0, 0.5, 0.05)
            .<SliderSetting>group("Sky Elements")
            .visibleWhen(starsOn),
         owner
      );
      this.starColor = this.add(
         new ColorSetting(this.id("starColor"), "Star Color", "Color of the stars and shooting stars.", ColorSetting.Mode.STATIC, -1, -2562817, 1.0)
            .skipSetAll()
            .<ColorSetting>group("Sky Elements")
            .visibleWhen(starsOn),
         owner
      );
      this.shootingStars = this.add(
         new SliderSetting(
               this.id("shootingStars"), "Shooting Stars", "How often they fall; now and then there is a meteor shower. 0 = never.", 0.0, 1.0, 0.3, 0.05
            )
            .group("Sky Elements"),
         owner
      );
      this.weather = this.add(
         new BooleanSetting(this.id("weather"), "Dim During Rain", "Rain greys and dims the sky; lightning lights it up.", true)
            .<BooleanSetting>group("Sky Elements")
            .keywords(new String[]{"weather", "react to weather", "lightning", "storm"}),
         owner
      );
      this.overworld = this.add(
         new BooleanSetting(this.id("overworld"), "Overworld", "Show it in the Overworld (and other normal worlds).", true)
            .<BooleanSetting>group("Where")
            .keywords(new String[]{"dimension", "dimensions"}),
         owner
      );
      this.nether = this.add(
         new BooleanSetting(this.id("nether"), "Nether", "Show it in the Nether, where there is normally no sky.", false)
            .<BooleanSetting>group("Where")
            .keywords(new String[]{"dimension", "dimensions"}),
         owner
      );
      this.end = this.add(
         new BooleanSetting(this.id("end"), "The End", "Show it in the End instead of its dark sky.", false)
            .<BooleanSetting>group("Where")
            .keywords(new String[]{"dimension", "dimensions"}),
         owner
      );
      this.blend = this.add(
         new ModeSetting<>(this.id("blend"), "Blend", "Replace the normal sky, or lay this one over it.", SkyModule.Blend.class, SkyModule.Blend.REPLACE)
            .group("Where"),
         owner
      );
      this.opacity = this.add(
         new SliderSetting(this.id("opacity"), "Opacity", "How much of this sky shows over the normal one.", 0.1, 1.0, 0.6, 0.05)
            .<SliderSetting>group("Where")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return SkySettings.this.blend.get() == SkyModule.Blend.OVERLAY;
               }
            }),
         owner
      );
      this.quality = this.add(
         new ModeSetting<>(
               this.id("quality"),
               "Quality",
               "Lower is faster: fewer details, and Low and Medium draw at a lower resolution. Soft skies (clouds, plasma, cells) draw at half resolution below Ultra, smoothly scaled.",
               SkyModule.Quality.class,
               SkyModule.Quality.HIGH
            )
            .group("Performance"),
         owner
      );
      this.offWithShaders = this.add(
         new BooleanSetting(
               this.id("offWithShaders"),
               "Off With Shader Packs",
               "Steps aside while an OptiFine shader pack is on (it draws its own sky). The skies with their own options always do.",
               true
            )
            .group("Performance"),
         owner
      );
      this.optifineSkies = this.add(
         new ModeSetting<>(
               this.id("optifineSkies"),
               "OptiFine Skies",
               "When a resource pack has its own OptiFine sky: draw this one anyway, or show the pack's.",
               SkyModule.OptiFineSkies.class,
               SkyModule.OptiFineSkies.OVERRIDE
            )
            .group("Performance"),
         owner
      );
   }

   private String id(String name) {
      return this.sky.name().toLowerCase(Locale.ROOT) + "." + name;
   }

   private <S extends Setting> S add(S setting, SettingStyle owner) {
      setting.ownedBy(owner);
      this.common.add(setting);
      return setting;
   }

   void setOwn(Setting[] settings) {
      this.own = (Setting[])settings.clone();
   }

   List<Setting> all() {
      ArrayList<Setting> out = new ArrayList<>(this.common);
      out.addAll(Arrays.asList(this.own));
      return out;
   }

   void recordDefaults() {
      Runnable changed = new Runnable() {
         @Override
         public void run() {
            SkySettings.this.modifiedKnown = false;
         }
      };

      for (Setting s : this.all()) {
         this.defaults.put(s, s.toJson());
         s.addListener(changed);
      }
   }

   boolean isModified() {
      if (!this.modifiedKnown) {
         this.modified = false;

         for (Setting s : this.all()) {
            JsonElement d = this.defaults.get(s);
            if (d != null && !d.equals(s.toJson())) {
               this.modified = true;
               break;
            }
         }

         this.modifiedKnown = true;
      }

      return this.modified;
   }

   void copyFrom(SkySettings other) {
      for (int i = 0; i < this.common.size(); i++) {
         this.common.get(i).fromJson(other.common.get(i).toJson());
      }
   }

   void reset() {
      for (Setting s : this.all()) {
         s.reset();
      }
   }

   static String sharedId(Setting s) {
      String id = s.getId();
      return id.substring(id.indexOf(46) + 1);
   }

   static boolean hasSharedSettings(JsonObject json, SkySettings first) {
      if (json.has(first.speed.getId())) {
         return false;
      } else {
         for (Setting s : first.common) {
            String old = s == first.fadeIn ? "smooth" : sharedId(s);
            if (json.has(old)) {
               return true;
            }
         }

         return json.has("whenPerSky");
      }
   }

   void carryOver(JsonObject json, String when) {
      for (Setting s : this.common) {
         if (s == this.fadeIn) {
            JsonElement smooth = json.get("smooth");
            if (smooth != null && smooth.isJsonPrimitive()) {
               this.fadeIn.set(smooth.getAsBoolean() ? 1.5 : 0.0);
            }
         } else {
            JsonElement value;
            if (s != this.show && s != this.dayBrightness && (value = json.get(sharedId(s))) != null) {
               s.fromJson(value);
            }
         }
      }

      SkyModule.Show showValue = SkyModule.Show.ALWAYS;
      double day = 1.0;
      String[] parts;
      if (when != null && (parts = when.split(":")).length == 2) {
         showValue = "NIGHT".equals(parts[0]) ? SkyModule.Show.NIGHT : SkyModule.Show.ALWAYS;

         try {
            day = Double.parseDouble(parts[1]);
         } catch (NumberFormatException var8) {
            day = 1.0;
         }
      }

      this.show.set(showValue);
      this.dayBrightness.set(day);
   }
}
