package meowtils.furiahvisuals.module.misc;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.Keybinds;
import meowtils.furiahvisuals.gui.FuriahScreen;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.MiniSections;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.misc.skins.SkinLibrary;
import meowtils.furiahvisuals.module.misc.skins.Skins;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.module.weapons.RadialWheel;
import meowtils.furiahvisuals.render.OptiFineCompat;
import meowtils.furiahvisuals.render.post.PostChain;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.KeyBindSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.TextSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.BlockPos;
import net.minecraft.world.biome.BiomeGenBase;

public final class MiscModule extends Module {
   static final String[] PIECES = new String[]{"Helmet", "Chestplate", "Leggings", "Boots"};
   private static final String[] KEYS = new String[]{"helmet", "chestplate", "leggings", "boots"};
   static final String[] TIME_PRESET_NAMES = new String[]{"Sunrise", "Noon", "Sunset", "Midnight"};
   static final int[] TIME_PRESETS = new int[]{360, 720, 1080, 0};
   static final String[] LOOK_NAMES = new String[]{"Vanilla", "Vivid", "Punchy", "Soft", "Cinematic", "Black & white"};
   static final int[][] LOOKS = new int[][]{{100, 100, 0}, {135, 105, 20}, {115, 125, 10}, {90, 90, 0}, {85, 115, -10}, {0, 115, 0}};
   private static final String[] SET_IDS = new String[]{"sat", "sat.overworld", "sat.nether", "sat.end"};
   private static final String[] SET_NAMES = new String[]{null, "in the Overworld", "in the Nether", "in the End"};
   private final MiniSections tabs = new MiniSections(this);
   private final MiniSection skinTab;
   private final MiniSection satTab;
   private final MiniSection fullbrightTab;
   private final MiniSection timeTab;
   private final MiniSection weatherTab;
   private final MiniSection armorTab;
   private final MiniSection hurtTab;
   public final Skins skins;
   final BooleanSetting[][] hide = new BooleanSetting[4][2];
   public final BooleanSetting armorOn;
   public final BooleanSetting hurtOn;
   public final BooleanSetting heads;
   public final SliderSetting hurtShake;
   public final BooleanSetting satOn;
   public final ModeSetting<MiscModule.Dimension> satDimension;
   final SliderSetting[][] satValues = new SliderSetting[4][3];
   public final ModeSetting<MiscModule.ApplyTo> satApplyTo;
   public final ModeSetting<MiscModule.PerDimension> satPerDimension;
   public final KeyBindSetting satToggleKey;
   public final SliderSetting satTransition;
   final Saturation saturation;
   public final BooleanSetting fullbrightOn;
   public final BooleanSetting timeOn;
   public final BooleanSetting timeSmooth;
   public final BooleanSetting weatherOn;
   public final BooleanSetting weatherSmooth;
   public final SliderSetting fullbrightStrength;
   public final SliderSetting timeClock;
   public final SliderSetting weatherStrength;
   public final KeyBindSetting fullbrightToggleKey;
   public final ModeSetting<MiscModule.Weather> weather;
   final Fullbright fullbright;
   final TimeChanger timeChanger;
   final WeatherChanger weatherChanger;
   private int dimensionForTest = -1;

   public MiscModule() {
      super(
         "misc",
         "Misc",
         "Small options that don't belong anywhere else: skins, saturation, fullbright, the time and weather you see, hide armor, the hurt camera.",
         true
      );
      this.skinTab = this.tabs.add("skins", "Skins", IslandIcons.Icon.SKIN);
      this.satTab = this.tabs.add("saturation", "Saturation", IslandIcons.Icon.PALETTE);
      this.fullbrightTab = this.tabs.add("fullbright", "Fullbright", IslandIcons.Icon.SUN);
      this.timeTab = this.tabs.add("time", "Time Changer", IslandIcons.Icon.CLOCK);
      this.weatherTab = this.tabs.add("weather", "Weather Changer", IslandIcons.Icon.CLOUD);
      this.armorTab = this.tabs.add("armor", "Hide armor", IslandIcons.Icon.ARMOR);
      this.hurtTab = this.tabs.add("hurt", "Hurtcam", IslandIcons.Icon.SHAKE);
      BooleanSetting skinOn = this.tabs
         .attachSwitch(
            this.skinTab,
            this.own(
               this.skinTab,
               new BooleanSetting(
                     "skin.enabled",
                     "Use a Custom Skin",
                     "Wears the skin you picked, on your screen only: other players keep seeing your account's skin. Picking a skin turns this on.",
                     false
                  )
                  .keywords(new String[]{"skin", "skins", "custom skin", "change skin", "enabled"})
            )
         );
      this.own(
         this.skinTab,
         (new WidgetSetting("skin.current", "Current Skin", "The skin you wear now, on a figure: drag it to turn it; Idle or Walk, Front or Back.") {
            @Override
            public Widget createWidget() {
               return MiscModule.this.skins.currentWidget();
            }
         }).keywords(new String[]{"preview", "current skin", "model", "slim", "classic", "alex", "steve", "hd skin"})
      );
      this.own(
         this.skinTab,
         (new WidgetSetting(
               "skin.back",
               "Back to My Account Skin",
               "Turns Use a Custom Skin off: your account's skin shows again (the skin you picked stays in the library)."
            ) {
               @Override
               public Widget createWidget() {
                  return MiscModule.this.skins.backWidget();
               }
            })
            .<WidgetSetting>visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return MiscModule.this.skins.active();
               }
            })
            .keywords(new String[]{"account skin", "default skin", "reset skin", "real skin"})
      );
      this.own(
         this.skinTab,
         (new WidgetSetting("skin.add", "Add Skins", "Upload your own PNG skins (HD too), or drop them into the skins folder and press Reload.") {
            @Override
            public Widget createWidget() {
               return MiscModule.this.skins.addWidget();
            }
         }).keywords(new String[]{"upload", "png", "import", "folder", "reload", "hd skin", "file"})
      );
      this.own(
         this.skinTab,
         (new WidgetSetting(
               "skin.library", "Your Skins", "Your skins: click one to wear it; ⋯ or right-click for Rename, Model, Favorite, Show in folder and Delete."
            ) {
               @Override
               public Widget createWidget() {
                  return MiscModule.this.skins.libraryWidget();
               }
            })
            .keywords(new String[]{"library", "skins", "wear", "favorite", "rename", "delete", "slim", "classic", "hd skin"})
      );
      TextSetting skinWorn = this.own(
         this.skinTab, new TextSetting("skin.worn", "Skin Picked", "The skin picked in the library (its hash and name).", "", 80).nested()
      );
      ModeSetting skinSort = this.own(
         this.skinTab, new ModeSetting<>("skin.sort", "Sort", "The library's order.", SkinLibrary.Sort.class, SkinLibrary.Sort.RECENT).nested()
      );
      BooleanSetting skinFavorites = this.own(
         this.skinTab, new BooleanSetting("skin.favoritesFirst", "Favorites First", "Your favorite skins come first in the library.", true).nested()
      );
      BooleanSetting skinAutoModel = this.own(
         this.skinTab,
         new BooleanSetting(
               "skin.autoModel",
               "Auto-Detect Model for Uploads",
               "New skins get the Slim model (3-pixel arms) when the outer strip of the right arm is see-through, else Classic. Off: Classic (change it on the skin's card).",
               true
            )
            .<BooleanSetting>group("Advanced")
            .keywords(new String[]{"slim", "classic", "alex", "steve", "model", "arms"})
      );
      BooleanSetting skinRemember = new BooleanSetting("skin.rememberSearches", "Remember Recent Searches", "", true);
      this.own(
         this.skinTab,
         new KeyBindSetting(
               "skin.wheelKey",
               "Skin Wheel",
               "Hold (or tap, with the weapon wheel's Toggle) to pick one of your favorite skins on a wheel: the weapon wheel's, with its look and controls. Unbound at first (also in Options › Controls).",
               new Supplier<KeyBinding>() {
                  public KeyBinding get() {
                     return Keybinds.skinWheel;
                  }
               }
            )
            .<KeyBindSetting>group("Quick switch")
            .keywords(new String[]{"wheel", "keybind", "hotkey", "favorite", "quick"})
      );
      this.own(
         this.skinTab,
         new KeyBindSetting(
               "skin.nextKey",
               "Next Favorite",
               "Wears your next favorite skin (by name, the skin wheel's order). Unbound at first.",
               new Supplier<KeyBinding>() {
                  public KeyBinding get() {
                     return Keybinds.nextSkin;
                  }
               }
            )
            .<KeyBindSetting>group("Quick switch")
            .keywords(new String[]{"keybind", "hotkey", "cycle", "favorite"})
      );
      this.own(
         this.skinTab,
         new KeyBindSetting("skin.prevKey", "Previous Favorite", "Wears your previous favorite skin. Unbound at first.", new Supplier<KeyBinding>() {
            public KeyBinding get() {
               return Keybinds.previousSkin;
            }
         }).<KeyBindSetting>group("Quick switch").keywords(new String[]{"keybind", "hotkey", "cycle", "favorite"})
      );
      BooleanSetting skinNotify = this.own(
         this.skinTab,
         new BooleanSetting("skin.notify", "Notification", "The island shows \"Skin: Notch\" when a key or the wheel changes your skin.", true)
            .<BooleanSetting>group("Quick switch")
            .keywords(new String[]{"island", "notice", "toast"})
      );
      BooleanSetting skinAsk = new BooleanSetting("skin.askAccount", "Ask Before Changing My Account Skin", "", true);
      this.skins = new Skins(this, skinOn, skinWorn, skinSort, skinFavorites, skinAutoModel, skinRemember, skinNotify, skinAsk);
      this.satOn = this.tabs
         .attachSwitch(
            this.satTab,
            this.own(
               this.satTab,
               new BooleanSetting("sat.enabled", "Enabled", "Changes how colorful the world looks. Off: vanilla's colors.", false)
                  .keywords(new String[]{"saturation", "colors", "colours", "color grading", "filter"})
            )
         );
      this.own(
         this.satTab,
         (new WidgetSetting(
               "sat.preview",
               "Before / After",
               "Your view as it is (left) and with these values (right): drag the line between them. Hold Peek to see the whole screen."
            ) {
               @Override
               public Widget createWidget() {
                  return new SaturationPreviewWidget(MiscModule.this);
               }
            })
            .keywords(new String[]{"preview", "before", "after", "peek"})
      );
      this.own(
         this.satTab,
         (new WidgetSetting(
               "sat.looks",
               "Looks",
               "One-click starting points: they set Saturation, Contrast and Vibrance (with Per dimension, the dimension's shown), and turn Saturation on."
            ) {
               @Override
               public Widget createWidget() {
                  return new SaturationLooksWidget(MiscModule.this);
               }
            })
            .keywords(new String[]{"vivid", "punchy", "soft", "cinematic", "black and white", "grayscale", "greyscale", "look", "preset"})
      );
      BooleanSupplier perDim = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return MiscModule.this.satPerDimension.get() == MiscModule.PerDimension.PER_DIMENSION;
         }
      };
      this.satDimension = this.own(
         this.satTab,
         new ModeSetting<>(
               "sat.dimension",
               "Dimension",
               "Whose values the sliders below show: it follows the dimension you are in.",
               MiscModule.Dimension.class,
               MiscModule.Dimension.OVERWORLD
            )
            .<ModeSetting>notSaved()
            .<ModeSetting>visibleWhen(perDim)
            .keywords(new String[]{"overworld", "nether", "end", "dimension"})
      );

      for (int set = 0; set < 4; set++) {
         final int which = set;
         BooleanSupplier shows = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return which == 0
                  ? MiscModule.this.satPerDimension.get() == MiscModule.PerDimension.SAME
                  : MiscModule.this.satPerDimension.get() == MiscModule.PerDimension.PER_DIMENSION && MiscModule.this.satDimension.get().ordinal() == which - 1;
            }
         };
         String in = SET_NAMES[set] == null ? "" : " " + SET_NAMES[set];
         this.satValues[set][0] = this.own(
            this.satTab,
            new SliderSetting(
                  SET_IDS[set] + ".saturation",
                  "Saturation",
                  "How colorful the world is" + in + ": 0% is black and white, 100% unchanged, 200% very vivid.",
                  0.0,
                  200.0,
                  100.0,
                  1.0
               )
               .unit("%")
               .<SliderSetting>visibleWhen(shows)
               .keywords(new String[]{"saturation", "colors", "grayscale", "black and white"})
         );
         this.satValues[set][1] = this.own(
            this.satTab,
            new SliderSetting(
                  SET_IDS[set] + ".contrast",
                  "Contrast",
                  "Darks darker and lights lighter" + in + ", around mid-grey: 100% unchanged.",
                  50.0,
                  200.0,
                  100.0,
                  1.0
               )
               .unit("%")
               .<SliderSetting>visibleWhen(shows)
               .keywords(new String[]{"contrast"})
         );
         this.satValues[set][2] = this.own(
            this.satTab,
            new SliderSetting(
                  SET_IDS[set] + ".vibrance",
                  "Vibrance",
                  "Like Saturation"
                     + in
                     + ", but muted colors move more than strong ones, so the sky, wool and skins don't turn garish; below 0% it mutes them.",
                  -100.0,
                  100.0,
                  0.0,
                  1.0
               )
               .unit("%")
               .<SliderSetting>visibleWhen(shows)
               .keywords(new String[]{"vibrance", "vibrancy"})
         );
      }

      this.satApplyTo = this.own(
         this.satTab,
         new ModeSetting<>(
               "sat.applyTo",
               "Apply To",
               "World only: the world, your hand and the held item, the HUD and menus keep their colors. World and HUD: the HUD too (never this mod's menu).",
               MiscModule.ApplyTo.class,
               MiscModule.ApplyTo.WORLD
            )
            .keywords(new String[]{"hud"})
      );
      this.satPerDimension = this.own(
         this.satTab,
         new ModeSetting<>(
               "sat.perDimension",
               "Per Dimension",
               "Same everywhere, or the Overworld, the Nether and the End each with their own values (a dimension change blends in about a second).",
               MiscModule.PerDimension.class,
               MiscModule.PerDimension.SAME
            )
            .keywords(new String[]{"nether", "end", "overworld", "dimension"})
      );
      this.satToggleKey = this.own(
         this.satTab,
         new KeyBindSetting(
               "sat.toggleKey",
               "Toggle Key",
               "A key that turns Saturation on and off, with the island's notice (also in Options › Controls).",
               new Supplier<KeyBinding>() {
                  public KeyBinding get() {
                     return Keybinds.toggleSaturation;
                  }
               }
            )
            .keywords(new String[]{"keybind", "hotkey"})
      );
      this.satTransition = this.own(
         this.satTab,
         new SliderSetting("sat.transition", "Transition", "How long changes and switching on and off take to blend in; 0 s snaps.", 0.0, 1.0, 0.25, 0.05)
            .unit("s")
            .keywords(new String[]{"fade", "smooth"})
      );
      this.saturation = new Saturation(this);
      PostChain.register(this.saturation);
      this.fullbrightOn = this.tabs
         .attachSwitch(
            this.fullbrightTab,
            this.own(
               this.fullbrightTab,
               new BooleanSetting(
                     "fullbright.enabled",
                     "Enabled",
                     "Lights up everything, at night and in caves, only on your screen. Off: vanilla's light. Your Brightness setting is never changed.",
                     false
                  )
                  .keywords(new String[]{"fullbright", "full bright", "brightness", "gamma", "night vision", "light", "dark", "caves"})
            )
         );
      this.fullbrightStrength = this.own(
         this.fullbrightTab,
         new SliderSetting(
               "fullbright.strength",
               "Strength",
               "100% lights everything fully; lower values brighten the world but keep some shading.",
               10.0,
               100.0,
               100.0,
               5.0
            )
            .unit("%")
            .keywords(new String[]{"brightness", "gamma", "night vision", "light"})
      );
      this.fullbrightToggleKey = this.own(
         this.fullbrightTab,
         new KeyBindSetting(
               "fullbright.toggleKey",
               "Toggle Key",
               "A key that turns Fullbright on and off, with the island's notice (also in Options › Controls).",
               new Supplier<KeyBinding>() {
                  public KeyBinding get() {
                     return Keybinds.toggleFullbright;
                  }
               }
            )
            .keywords(new String[]{"keybind", "hotkey", "brightness", "gamma"})
      );
      this.fullbright = new Fullbright(this);
      this.timeOn = this.tabs
         .attachSwitch(
            this.timeTab,
            this.own(
               this.timeTab,
               new BooleanSetting(
                     "time.enabled",
                     "Enabled",
                     "Sets the time of day you see, only on your screen: the server's time keeps going underneath. Overworld only.",
                     false
                  )
                  .keywords(new String[]{"time", "time changer", "day", "night", "clock", "daytime", "nighttime"})
            )
         );
      this.timeClock = this.own(
         this.timeTab,
         new SliderSetting(
               "time.clock",
               "Time",
               "The time of day you see, on Minecraft's clock: sunrise is 06:00, noon 12:00, sunset 18:00 and midnight 00:00. The sky, the sun, the moon, the stars and the light follow it.",
               0.0,
               1439.0,
               720.0,
               1.0
            )
            .clock()
            .keywords(new String[]{"time", "clock", "hour", "day", "night"})
      );
      this.own(
         this.timeTab,
         (new WidgetSetting(
               "time.presets", "Presets", "One click sets the Time: Sunrise 06:00, Noon 12:00, Sunset 18:00 or Midnight 00:00 (and turns the Time Changer on)."
            ) {
               @Override
               public Widget createWidget() {
                  return new TimePresetsWidget(MiscModule.this);
               }
            })
            .keywords(new String[]{"sunrise", "noon", "sunset", "midnight", "dawn", "dusk", "day", "night"})
      );
      this.timeSmooth = this.own(
         this.timeTab,
         new BooleanSetting(
               "time.smooth",
               "Smooth",
               "A change glides to the new time over about a second instead of jumping; turning the Time Changer off glides back to the real time.",
               true
            )
            .keywords(new String[]{"glide", "fade", "transition"})
      );
      this.timeChanger = new TimeChanger(this);
      this.weatherOn = this.tabs
         .attachSwitch(
            this.weatherTab,
            this.own(
               this.weatherTab,
               new BooleanSetting(
                     "weather.enabled", "Enabled", "Sets the weather you see, only on your screen: the server's weather is unchanged. Overworld only.", false
                  )
                  .keywords(new String[]{"weather", "weather changer", "rain", "snow", "thunder", "storm", "clear"})
            )
         );
      this.weather = this.own(
         this.weatherTab,
         new ModeSetting<>(
               "weather.type",
               "Weather",
               "Clear hides the server's rain and snow; Rain falls where vanilla rain would; Thunderstorm darkens the sky like a vanilla storm; Snow turns all of it into snow, without a sound.",
               MiscModule.Weather.class,
               MiscModule.Weather.CLEAR
            )
            .keywords(new String[]{"clear", "rain", "thunder", "thunderstorm", "storm", "snow", "sunny"})
      );
      this.weatherStrength = this.own(
         this.weatherTab,
         new SliderSetting("weather.strength", "Strength", "How heavy the rain or snow is.", 10.0, 100.0, 100.0, 5.0)
            .unit("%")
            .<SliderSetting>visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return MiscModule.this.weather.get() != MiscModule.Weather.CLEAR;
               }
            })
            .keywords(new String[]{"rain", "snow", "heavy", "intensity"})
      );
      this.weatherSmooth = this.own(
         this.weatherTab,
         new BooleanSetting(
               "weather.smooth", "Smooth", "Changes fade over about two seconds, like vanilla weather (rain and snow trade places through nothing).", true
            )
            .keywords(new String[]{"fade", "transition"})
      );
      this.weatherChanger = new WeatherChanger(this);
      this.armorOn = this.tabs
         .attachSwitch(
            this.armorTab,
            this.own(
               this.armorTab,
               new BooleanSetting("armor.enabled", "Enabled", "Hides the pieces of armor picked below. Off: every piece shows, as in vanilla.", false)
            )
         );
      this.own(
         this.armorTab,
         (new WidgetSetting(
               "armor",
               "Hide Armor",
               "Hide pieces of armor: yours (in third person and the inventory) and other players' (on your screen only). Only the look: the armor still counts."
            ) {
               @Override
               public Widget createWidget() {
                  return new ArmorGridWidget(MiscModule.this);
               }
            })
            .keywords(new String[]{"armor", "armour", "helmet", "chestplate", "leggings", "boots", "hide armor", "invisible armor"})
      );

      for (int i = 0; i < PIECES.length; i++) {
         String piece = PIECES[i].toLowerCase(Locale.ROOT);
         this.hide[i][0] = this.own(
            this.armorTab,
            new BooleanSetting("armor.me." + KEYS[i], "Hide My " + PIECES[i], "Your " + piece + " is not drawn (in third person and the inventory).", false)
               .nested()
         );
         this.hide[i][1] = this.own(
            this.armorTab,
            new BooleanSetting("armor.others." + KEYS[i], "Hide Others' " + PIECES[i], "Other players' " + piece + " is not drawn, on your screen only.", false)
               .nested()
         );
      }

      this.heads = this.own(
         this.armorTab,
         new BooleanSetting("armor.heads", "Heads and Pumpkins", "Where the helmet is hidden, heads and pumpkins worn on the head are hidden too.", true)
            .keywords(new String[]{"skull", "pumpkin", "head"})
      );
      this.hurtOn = this.tabs
         .attachSwitch(
            this.hurtTab,
            this.own(
               this.hurtTab,
               new BooleanSetting("hurt.enabled", "Enabled", "Changes how much the camera shakes when you take damage. Off: vanilla's shake.", false)
                  .keywords(new String[]{"hurtcam", "hurt cam"})
            )
         );
      this.hurtShake = this.own(
         this.hurtTab,
         new SliderSetting(
               "hurtShake",
               "Hurt Shake",
               "How much the camera shakes and tilts when you take damage: 100% is vanilla, 0% turns it off. Only the camera: the hearts' flash, the sounds and the Weapons' hurt jolt stay.",
               0.0,
               100.0,
               100.0,
               5.0
            )
            .unit("%")
            .keywords(new String[]{"hurtcam", "hurt cam", "camera shake", "damage tilt", "screen shake"})
      );
      this.hurtShake.info(new Supplier<String>() {
         public String get() {
            return MiscHooks.hurtHooked() ? null : "its game hook could not be installed here";
         }
      }, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return !MiscHooks.hurtHooked();
         }
      });
      MiscHooks.module = this;
   }

   private <S extends Setting> S own(MiniSection tab, S setting) {
      return this.add(setting.ownedBy(tab));
   }

   @Override
   public void onClientTick() {
      this.skins.update();
      this.skins.wheel();
   }

   @Override
   public void onTick() {
      RadialWheel w = this.skins.wheel();
      if (w != null) {
         w.tick();
      }
   }

   @Override
   public void onRenderOverlay(float partialTicks) {
      RadialWheel w = this.skins.wheel();
      if (w != null) {
         w.drawBackdrop();
      }
   }

   @Override
   public boolean onOverlayElement(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      RadialWheel w = this.skins.wheel();
      return type == RenderGameOverlayEvent.ElementType.CROSSHAIRS && w != null && w.showing();
   }

   @Override
   public void onRenderHud(float partialTicks) {
      RadialWheel w = this.skins.wheel();
      if (w != null) {
         w.drawHud();
      }
   }

   @Override
   protected void onDisable() {
      this.skins.stop();
   }

   @Override
   public MiniSections getTabs() {
      return this.tabs;
   }

   boolean hidden(int piece, boolean me) {
      return this.armorTab.on() && this.hide[piece][me ? 0 : 1].get();
   }

   boolean cell(int piece, boolean me) {
      return this.hide[piece][me ? 0 : 1].get();
   }

   boolean hurtActive() {
      return this.hurtTab.on();
   }

   public void dimensionForTest(int dim) {
      this.dimensionForTest = dim;
   }

   int dimensionIndex(Minecraft mc) {
      if (this.dimensionForTest >= 0) {
         return this.dimensionForTest;
      } else if (mc.theWorld != null && mc.theWorld.provider != null) {
         int id = mc.theWorld.provider.getDimensionId();
         return id == -1 ? 1 : (id == 1 ? 2 : 0);
      } else {
         return 0;
      }
   }

   void followDimension(Minecraft mc, int dim) {
      if (!(mc.currentScreen instanceof FuriahScreen) && this.satDimension.get().ordinal() != dim) {
         this.satDimension.set(MiscModule.Dimension.values()[dim]);
      }
   }

   float[] satValues(int dim) {
      SliderSetting[] set = this.satValues[this.satPerDimension.get() == MiscModule.PerDimension.PER_DIMENSION ? 1 + dim : 0];
      return new float[]{set[0].getFloat() / 100.0F, set[1].getFloat() / 100.0F, set[2].getFloat() / 100.0F};
   }

   SliderSetting[] shownSet() {
      return this.satValues[this.satPerDimension.get() == MiscModule.PerDimension.PER_DIMENSION ? 1 + this.satDimension.get().ordinal() : 0];
   }

   void applyLook(int look) {
      SliderSetting[] set = this.shownSet();

      for (int i = 0; i < 3; i++) {
         set[i].set(LOOKS[look][i]);
      }

      this.satOn.set(true);
   }

   int currentLook() {
      SliderSetting[] set = this.shownSet();

      for (int look = 0; look < LOOKS.length; look++) {
         if (set[0].getInt() == LOOKS[look][0] && set[1].getInt() == LOOKS[look][1] && set[2].getInt() == LOOKS[look][2]) {
            return look;
         }
      }

      return -1;
   }

   public boolean toggleSaturation() {
      boolean on = !this.isEnabled() || !this.satOn.get();
      if (on && !this.isEnabled()) {
         this.setEnabled(true);
      }

      this.satOn.set(on);
      return on;
   }

   @Override
   public Module.Notice getNotice() {
      MiniSection open = this.tabs.current();
      if (open == this.fullbrightTab) {
         return this.fullbrightNotice();
      } else if (open == this.timeTab) {
         return this.timeNotice();
      } else if (open == this.weatherTab) {
         return this.weatherNotice();
      } else if (open != this.satTab) {
         return null;
      } else {
         String failure = this.saturation.failure();
         if (failure != null) {
            return new Module.Notice(
               "Saturation stopped working on this graphics driver (" + failure + ") and is off until the game restarts. The log has the details.", true
            );
         } else if (!OpenGlHelper.shadersSupported) {
            return new Module.Notice("Saturation needs OpenGL shaders, which this graphics driver doesn't offer, so its options are off.", true);
         } else {
            Minecraft mc = Minecraft.getMinecraft();
            return mc.gameSettings != null && mc.gameSettings.anaglyph
               ? new Module.Notice("Saturation is off while 3D Anaglyph is on (Options › Video Settings).", false)
               : null;
         }
      }
   }

   public String saturationStateForTest() {
      return this.saturation.stateForTest();
   }

   public float[] saturationBlendedForTest() {
      return this.saturation.blendedForTest();
   }

   public void saturationForTest(int sat, int con, int vib) {
      SliderSetting[] set = this.shownSet();
      set[0].set(sat);
      set[1].set(con);
      set[2].set(vib);
   }

   public void saturationSetForTest(int which, int sat, int con, int vib) {
      this.satValues[which][0].set(sat);
      this.satValues[which][1].set(con);
      this.satValues[which][2].set(vib);
   }

   public void resetSaturationForTest() {
      for (Setting s : this.getSettings()) {
         if (s.getId().startsWith("sat.") && !(s instanceof KeyBindSetting)) {
            s.reset();
         }
      }

      this.dimensionForTest = -1;
   }

   public int lookForTest(int look) {
      this.applyLook(look);
      return this.currentLook();
   }

   public String saturationTimesForTest(boolean on) {
      Saturation.timingForTest = on;
      return this.saturation.timesForTest();
   }

   public void saturationPointForTest(PostChain.Point at) {
      Saturation.pointForTest = at;
      this.saturation.resetTimesForTest();
   }

   public PostChain.Pass saturationPassForTest() {
      return this.saturation;
   }

   boolean fullbrightActive() {
      return this.fullbrightTab.on() && this.fullbright.hooked() && !MiscHooks.fullbrightBroken;
   }

   float fullbrightStrength() {
      return this.fullbrightStrength.getFloat() / 100.0F;
   }

   boolean timeActive() {
      return this.timeTab.on() && this.timeChanger.hooked() && !MiscHooks.timeBroken;
   }

   int timeMinute() {
      return this.timeClock.getInt();
   }

   boolean timeSmooth() {
      return this.timeSmooth.get();
   }

   boolean weatherActive() {
      return this.weatherTab.on() && this.weatherChanger.hooked() && !MiscHooks.weatherBroken;
   }

   MiscModule.Weather weatherKind() {
      return this.weather.get();
   }

   float weatherStrength() {
      return this.weatherStrength.getFloat() / 100.0F;
   }

   boolean weatherSmooth() {
      return this.weatherSmooth.get();
   }

   public boolean fullbrightKey() {
      boolean on = !this.isEnabled() || !this.fullbrightOn.get();
      if (on && !this.isEnabled()) {
         this.setEnabled(true);
      }

      this.fullbrightOn.set(on);
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.ingameGUI != null) {
         mc.ingameGUI.setRecordPlaying("§dFuriah's Visuals §8» §fFullbright " + (on ? "§aON" : "§cOFF"), false);
      }

      WaterMarkModule wm;
      if ((wm = FuriahsVisuals.instance.getWaterMark()) != null) {
         wm.partOnOff("fullbright", "Fullbright", on);
      }

      return on;
   }

   void applyTimePreset(int i) {
      this.timeClock.set(TIME_PRESETS[i]);
      this.timeOn.set(true);
   }

   int currentTimePreset() {
      for (int i = 0; i < TIME_PRESETS.length; i++) {
         if (this.timeClock.getInt() == TIME_PRESETS[i]) {
            return i;
         }
      }

      return -1;
   }

   private Module.Notice fullbrightNotice() {
      if (!this.fullbright.hooked() || MiscHooks.fullbrightBroken) {
         return new Module.Notice("Fullbright couldn't hook into the game's lighting here, so it stays off.", true);
      } else {
         return OptiFineCompat.shadersActive() ? new Module.Notice("A shader pack's own lighting may limit Fullbright.", false) : null;
      }
   }

   private Module.Notice timeNotice() {
      if (this.timeChanger.hooked() && !MiscHooks.timeBroken) {
         return elsewhere() ? new Module.Notice("The Time Changer works in the Overworld only: the Nether and the End have no day cycle.", false) : null;
      } else {
         return new Module.Notice("The Time Changer couldn't hook into the game here, so it stays off.", true);
      }
   }

   private Module.Notice weatherNotice() {
      if (this.weatherChanger.hooked() && !MiscHooks.weatherBroken) {
         if (this.weather.get() == MiscModule.Weather.SNOW && !this.weatherChanger.snowHooked()) {
            return new Module.Notice("Snow couldn't hook into the game's rain here, so it shows as rain.", false);
         } else {
            return elsewhere() ? new Module.Notice("The Weather Changer works in the Overworld only.", false) : null;
         }
      } else {
         return new Module.Notice("The Weather Changer couldn't hook into the game here, so it stays off.", true);
      }
   }

   private static boolean elsewhere() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc.theWorld != null && mc.theWorld.provider != null && mc.theWorld.provider.getDimensionId() != 0;
   }

   public void worldViewOffForTest() {
      for (Setting s : this.getSettings()) {
         String id = s.getId();
         if ((id.startsWith("fullbright.") || id.startsWith("time.") || id.startsWith("weather.")) && !(s instanceof KeyBindSetting)) {
            s.reset();
         }
      }

      this.fullbright.aroundForTest(false);
   }

   public boolean[] worldHooksForTest() {
      return new boolean[]{this.fullbright.hooked(), this.timeChanger.hooked(), this.weatherChanger.hooked(), this.weatherChanger.snowHooked()};
   }

   public void fullbrightForTest(boolean on, int strength) {
      this.fullbrightStrength.set(strength);
      this.fullbrightOn.set(on);
   }

   public float[] fullbrightStateForTest() {
      return this.fullbright.stateForTest();
   }

   public void fullbrightAroundForTest(boolean on) {
      this.fullbright.aroundForTest(on);
   }

   public int lightmapForTest(int index) {
      return MiscHooks.lightmapColor(index);
   }

   public void timeForTest(boolean on, int minute, boolean smooth) {
      this.timeClock.set(minute);
      this.timeSmooth.set(smooth);
      this.timeOn.set(on);
   }

   public double timeShownForTest() {
      return this.timeChanger.shownForTest();
   }

   public int currentTimePresetForTest() {
      return this.currentTimePreset();
   }

   public void applyTimePresetForTest(int i) {
      this.applyTimePreset(i);
   }

   public void weatherForTest(boolean on, MiscModule.Weather kind, int strength, boolean smooth) {
      this.weather.set(kind);
      this.weatherStrength.set(strength);
      this.weatherSmooth.set(smooth);
      this.weatherOn.set(on);
   }

   public float[] weatherStateForTest() {
      return this.weatherChanger.stateForTest();
   }

   public float realRainForTest() {
      final float[] out = new float[1];
      MiscHooks.real(new Runnable() {
         @Override
         public void run() {
            WorldClient w = Minecraft.getMinecraft().theWorld;
            out[0] = w == null ? 0.0F : w.getRainStrength(1.0F);
         }
      });
      return out[0];
   }

   public float temperatureForTest(BiomeGenBase biome, BlockPos pos) {
      return MiscHooks.temperature(biome, pos);
   }

   @Override
   public boolean afterLoad(JsonObject json) {
      boolean changed = false;
      if (json != null && !json.has("armor.enabled")) {
         boolean any = false;
         BooleanSetting[][] booleanSettingArray = this.hide;
         int n = booleanSettingArray.length;

         for (int i = 0; i < n; i++) {
            BooleanSetting[] row;
            for (BooleanSetting cell : row = booleanSettingArray[i]) {
               any |= cell.get();
            }
         }

         if (any != this.armorOn.get()) {
            this.armorOn.set(any);
         }

         changed |= any;
      }

      if (json != null && !json.has("hurt.enabled")) {
         boolean shaken = this.hurtShake.getInt() != 100;
         if (shaken != this.hurtOn.get()) {
            this.hurtOn.set(shaken);
         }

         changed |= shaken;
      }

      return changed;
   }

   @Override
   public String summary() {
      int n = 0;
      if (this.armorOn.get()) {
         BooleanSetting[][] booleanSettingArray = this.hide;
         int n2 = booleanSettingArray.length;

         for (int i = 0; i < n2; i++) {
            BooleanSetting[] row;
            for (BooleanSetting cell : row = booleanSettingArray[i]) {
               n += cell.get() ? 1 : 0;
            }
         }
      }

      ArrayList<String> parts = new ArrayList<>();
      if (this.skins.active() && this.skins.wornEntry() != null) {
         parts.add("Skin " + this.skins.wornEntry().name);
      }

      if (this.satOn.get()) {
         int look = this.currentLook();
         parts.add(look > 0 ? LOOK_NAMES[look] : "Saturation " + this.shownSet()[0].getInt() + "%");
      }

      if (this.fullbrightOn.get()) {
         parts.add(this.fullbrightStrength.getInt() < 100 ? "Fullbright " + this.fullbrightStrength.getInt() + "%" : "Fullbright");
      }

      if (this.timeOn.get()) {
         parts.add("Time " + TimePresetsWidget.clock(this.timeClock.getInt()));
      }

      if (this.weatherOn.get()) {
         parts.add(this.weather.get().displayName());
      }

      if (n > 0) {
         parts.add("Hides " + n + (n == 1 ? " piece" : " pieces"));
      }

      if (this.hurtOn.get() && this.hurtShake.getInt() != 100) {
         parts.add("Hurt shake " + this.hurtShake.getInt() + "%");
      }

      if (parts.isEmpty()) {
         return "Vanilla";
      } else {
         return parts.size() == 1 ? parts.get(0) : parts.get(0) + ", " + parts.get(1);
      }
   }

   public float lastTiltForTest() {
      return MiscHooks.lastTilt;
   }

   public void hideForTest(int piece, boolean me, boolean on) {
      this.hide[piece][me ? 0 : 1].set(on);
      if (on) {
         this.armorOn.set(true);
      }
   }

   public boolean[] hooksForTest() {
      return new boolean[]{MiscHooks.armorHooked(), MiscHooks.hurtHooked()};
   }

   public static enum ApplyTo implements Displayable {
      WORLD("World only", "The world, your hand and the held item; the HUD, chat, the island and every menu keep their colors"),
      WORLD_AND_HUD("World and HUD", "The HUD too: hotbar, chat, scoreboard, tab list, the island (never this mod's menu)");

      private final String name;
      private final String description;

      private ApplyTo(String name, String description) {
         this.name = name;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.name;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   public static enum Dimension implements Displayable {
      OVERWORLD("Overworld"),
      NETHER("Nether"),
      END("End");

      private final String name;

      private Dimension(String name) {
         this.name = name;
      }

      @Override
      public String displayName() {
         return this.name;
      }

      @Override
      public String description() {
         return "Its own Saturation, Contrast and Vibrance";
      }
   }

   public static enum PerDimension implements Displayable {
      SAME("Same everywhere", "One set of values in every dimension"),
      PER_DIMENSION("Per dimension", "The Overworld, the Nether and the End each keep their own values");

      private final String name;
      private final String description;

      private PerDimension(String name, String description) {
         this.name = name;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.name;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   public static enum Weather implements Displayable {
      CLEAR("Clear", "No rain or snow, even while the server's weather has some"),
      RAIN("Rain", "Rain where vanilla rain falls (snow in cold biomes; deserts and other dry biomes stay dry)"),
      THUNDERSTORM("Thunderstorm", "Rain under a dark stormy sky, like a vanilla storm; lightning only when the server sends it"),
      SNOW("Snow", "Snow everywhere rain would fall, even in warm biomes (dry biomes stay dry); silent");

      private final String name;
      private final String description;

      private Weather(String name, String description) {
         this.name = name;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.name;
      }

      @Override
      public String description() {
         return this.description;
      }
   }
}
