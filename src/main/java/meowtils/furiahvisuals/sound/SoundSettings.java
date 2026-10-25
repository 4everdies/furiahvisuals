package meowtils.furiahvisuals.sound;

import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.NoteSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class SoundSettings {
   private static final String[] KEYWORDS = new String[]{"sound", "sounds", "audio", "volume", "mute", "sfx"};
   public final BooleanSetting mute = new BooleanSetting(
         "soundMute",
         "Mute Mod Sounds",
         "Silences every sound of this mod, the menu's and the island's (Minecraft's own sounds stay). Also on a key (Options > Controls) and on the island's quick settings.",
         false
      )
      .keywords(KEYWORDS);
   public final SliderSetting volume = new SliderSetting(
         "soundVolume", "Volume", "How loud all of the mod's sounds are (Minecraft's Master volume applies on top).", 0.0, 100.0, 60.0, 1.0
      )
      .unit("%")
      .keywords(KEYWORDS);
   public final ModeSetting<SoundTheme> theme = new ModeSetting<>(
         "soundTheme",
         "Sound Theme",
         "The same sounds in four voices: Soft (warm plops and chimes), Glass (bright pings and bells), Minimal (tiny dry clicks) or Creamy (deep, soft keystrokes, like creamy mechanical keyboard switches).",
         SoundTheme.class,
         SoundTheme.SOFT
      )
      .keywords(KEYWORDS);
   public final NoteSetting creamyTip = new NoteSetting("soundCreamyTip", "Tip: try Typing sounds with this theme.");
   public final BooleanSetting gui = new BooleanSetting(
         "guiSounds", "Menu Sounds", "Sounds for this menu: opening it, pages, switches, sliders, picks, saving and errors.", true
      )
      .keywords(KEYWORDS);
   public final SliderSetting guiVolume = new SliderSetting("guiVolume", "Menu Volume", "How loud the menu's sounds are.", 0.0, 100.0, 100.0, 1.0)
      .unit("%")
      .keywords(KEYWORDS);
   public final BooleanSetting sliderTicks = new BooleanSetting(
         "sliderTicks", "Slider Ticks", "Soft ticks while a slider or color moves, higher as its value rises.", true
      )
      .keywords(KEYWORDS);
   public final BooleanSetting noResults = new BooleanSetting(
         "noResultsSound", "No-Results Sound", "A soft low tone when a search stops finding anything.", true
      )
      .keywords(KEYWORDS);
   public final BooleanSetting hover = new BooleanSetting(
         "hoverSounds", "Hover Sounds", "A very faint tick when the mouse comes onto something you can click.", false
      )
      .keywords(KEYWORDS);
   public final BooleanSetting typing = new BooleanSetting(
         "typingSounds",
         "Typing Sounds",
         "A soft tick for each key typed into a text field or the search (Creamy: a keystroke, never the same twice, Space, Enter and Backspace their own, and the key coming back up).",
         false
      )
      .keywords(new String[]{"sound", "sounds", "audio", "keyboard", "keystroke", "typing"});

   public SoundSettings() {
      BooleanSupplier menuOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return SoundSettings.this.gui.get();
         }
      };
      this.creamyTip.visibleWhen(new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return SoundSettings.this.theme.get() == SoundTheme.CREAMY;
         }
      });
      this.guiVolume.visibleWhen(menuOn);
      this.sliderTicks.visibleWhen(menuOn);
      this.noResults.visibleWhen(menuOn);
      this.hover.visibleWhen(menuOn);
      this.typing.visibleWhen(menuOn);
      this.volume.preview(previewOf(Sfx.SELECT));
      this.theme.preview(new Runnable() {
         @Override
         public void run() {
            ModSounds.previewTheme();
         }
      });
      this.gui.preview(previewOf(Sfx.CLICK));
      this.guiVolume.preview(previewOf(Sfx.CLICK));
      this.sliderTicks.preview(previewOf(Sfx.SLIDER_TICK));
      this.noResults.preview(previewOf(Sfx.EMPTY));
      this.hover.preview(previewOf(Sfx.HOVER));
      this.typing.preview(previewOf(Sfx.KEY));
   }

   private static Runnable previewOf(final Sfx sfx) {
      return new Runnable() {
         @Override
         public void run() {
            ModSounds.previewGui(sfx);
         }
      };
   }

   public Setting[] all() {
      return new Setting[]{
         this.mute, this.volume, this.theme, this.creamyTip, this.gui, this.guiVolume, this.sliderTicks, this.noResults, this.hover, this.typing
      };
   }
}
