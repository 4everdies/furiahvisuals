package meowtils.furiahvisuals.module.weapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.core.Keybinds;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.KeyBindSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.settings.KeyBinding;

final class WheelSettings {
   static final String GROUP = "Weapon Wheel";
   final KeyBindSetting key = new KeyBindSetting(
         "wheel.key", "Weapon Wheel Key", "The key that opens the wheel (in Options › Controls too).", new Supplier<KeyBinding>() {
            public KeyBinding get() {
               return Keybinds.weaponWheel;
            }
         }
      )
      .<Setting>group("Weapon Wheel")
      .keywords("keybind", "hotkey", "bind", "key", "wheel", "weapon wheel", "open");
   final PageRow tryIt;
   final WheelSlotsSetting slots;
   final SliderSetting perPage;
   final ModeSetting<WeaponOptions.WheelMode> mode;
   final BooleanSetting quickSwap;
   final BooleanSetting freezeCamera;
   final BooleanSetting closeOnDamage;
   final ModeSetting<WeaponOptions.WheelTheme> theme;
   final ColorSetting background;
   final SliderSetting backgroundOpacity;
   final ColorSetting slotColor;
   final ColorSetting highlight;
   final BooleanSetting border;
   final BooleanSetting glass;
   final SliderSetting size;
   final SliderSetting hole;
   final SliderSetting gap;
   final ModeSetting<WeaponOptions.WheelIcons> icons;
   final ModeSetting<WeaponOptions.WheelLabels> labels;
   final BooleanSetting blur;
   final SliderSetting blurStrength;
   final List<Setting> all;

   WheelSettings(final WeaponsModule module) {
      this.tryIt = new PageRow(
            "wheel.try",
            "Try It",
            "Opens the wheel here in the menu: move the mouse or scroll to light a weapon, click to pick it, right-click or Esc to close.",
            new Supplier<Widget>() {
               public Widget get() {
                  return new WheelTryWidget(module);
               }
            }
         )
         .<PageRow>group("Weapon Wheel")
         .keywords(new String[]{"preview", "test", "try", "weapon wheel"});
      this.slots = new WheelSlotsSetting(module)
         .<Setting>group("Weapon Wheel")
         .keywords("slots", "weapons", "finish", "add", "order", "reorder", "weapon wheel");
      this.perPage = new SliderSetting(
            "wheel.perPage",
            "Slots Per Page",
            "With more slots than this, the wheel gets pages (dots under it); scrolling past the last slot turns the page.",
            2.0,
            12.0,
            8.0,
            1.0
         )
         .<SliderSetting>group("Weapon Wheel")
         .keywords(new String[]{"pages", "page", "slots"});
      this.mode = new ModeSetting<>("wheel.mode", "Opens With", "Hold the key, or press it once.", WeaponOptions.WheelMode.class, WeaponOptions.WheelMode.HOLD)
         .<ModeSetting>group("Weapon Wheel")
         .keywords(new String[]{"hold", "toggle", "press"});
      this.quickSwap = new BooleanSetting(
            "wheel.quickSwap", "Quick Swap", "A short tap of the key (under 200 ms) goes straight back to your previous weapon, without the wheel.", true
         )
         .<BooleanSetting>group("Weapon Wheel")
         .<BooleanSetting>keywords(new String[]{"tap", "previous", "last weapon", "swap back", "quick switch"})
         .visibleWhen(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return WheelSettings.this.mode.get() == WeaponOptions.WheelMode.HOLD;
            }
         });
      this.freezeCamera = new BooleanSetting(
            "wheel.freezeCamera",
            "Freeze Camera While Open",
            "The mouse moves the wheel's selector instead of the camera. Off: the camera keeps turning and only the scroll wheel picks.",
            true
         )
         .<BooleanSetting>group("Weapon Wheel")
         .keywords(new String[]{"camera", "mouse", "look", "freeze", "selector"});
      this.closeOnDamage = new BooleanSetting("wheel.closeOnDamage", "Close When Hurt", "Taking damage closes the wheel without changing anything.", false)
         .<BooleanSetting>group("Weapon Wheel")
         .keywords(new String[]{"damage", "hurt", "hit"});
      this.theme = new ModeSetting<>("wheel.theme", "Look", "How the wheel looks.", WeaponOptions.WheelTheme.class, WeaponOptions.WheelTheme.MATCH)
         .<ModeSetting>group("Weapon Wheel")
         .keywords(new String[]{"theme", "style", "glass", "minimal", "custom"});
      BooleanSupplier custom = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return WheelSettings.this.theme.get() == WeaponOptions.WheelTheme.CUSTOM;
         }
      };
      this.background = new ColorSetting(
            "wheel.background", "Background", "The wheel's middle and the ring behind its slots.", ColorSetting.Mode.STATIC, -15395300, -14408146, 1.0
         )
         .skipSetAll()
         .<ColorSetting>group("Weapon Wheel")
         .<ColorSetting>keywords(new String[]{"background", "fill"})
         .visibleWhen(custom);
      this.backgroundOpacity = new SliderSetting("wheel.backgroundOpacity", "Background Opacity", "How solid the background is.", 0.0, 100.0, 72.0, 1.0)
         .unit("%")
         .<SliderSetting>group("Weapon Wheel")
         .<SliderSetting>keywords(new String[]{"transparency", "alpha"})
         .visibleWhen(custom);
      this.slotColor = new ColorSetting("wheel.slotColor", "Slots", "Each slot's fill.", ColorSetting.Mode.STATIC, -14013128, -12960180, 1.0)
         .skipSetAll()
         .<ColorSetting>group("Weapon Wheel")
         .<ColorSetting>keywords(new String[]{"slot color", "fill"})
         .visibleWhen(custom);
      this.highlight = new ColorSetting("wheel.highlight", "Highlight", "The lit slot's glow and edge.", -45096)
         .<ColorSetting>group("Weapon Wheel")
         .<ColorSetting>keywords(new String[]{"highlight", "accent", "selected", "glow"})
         .visibleWhen(custom);
      this.border = new BooleanSetting("wheel.border", "Border", "A thin edge around the wheel and its slots.", true)
         .<BooleanSetting>group("Weapon Wheel")
         .<BooleanSetting>keywords(new String[]{"outline", "edge"})
         .visibleWhen(custom);
      this.glass = new BooleanSetting("wheel.glass", "Glass", "Frosted glass: the world shows through it, blurred (the island's glass).", false)
         .<BooleanSetting>group("Weapon Wheel")
         .<BooleanSetting>keywords(new String[]{"frosted", "blur", "transparent"})
         .visibleWhen(custom);
      this.size = new SliderSetting(
            "wheel.size",
            "Size",
            "How tall the wheel is, against the screen's height (it shrinks to keep clear of the hotbar and the island).",
            20.0,
            60.0,
            36.0,
            1.0
         )
         .unit("%")
         .<SliderSetting>group("Weapon Wheel")
         .keywords(new String[]{"scale", "big", "small", "radius"});
      this.hole = new SliderSetting("wheel.hole", "Inner Hole", "How big its middle is, against the whole wheel.", 25.0, 65.0, 44.0, 1.0)
         .unit("%")
         .<SliderSetting>group("Weapon Wheel")
         .keywords(new String[]{"center", "middle", "inner"});
      this.gap = new SliderSetting("wheel.gap", "Gap Between Slots", "The space between two slots (pixels at 1080p).", 0.0, 16.0, 5.0, 1.0)
         .unit(" px")
         .<SliderSetting>group("Weapon Wheel")
         .keywords(new String[]{"spacing", "gap", "space"});
      this.icons = new ModeSetting<>(
            "wheel.icons", "Pictures", "How the wheel shows its weapons.", WeaponOptions.WheelIcons.class, WeaponOptions.WheelIcons.LIVE
         )
         .<ModeSetting>group("Weapon Wheel")
         .keywords(new String[]{"icons", "3d", "models", "static", "live"});
      this.labels = new ModeSetting<>(
            "wheel.labels", "Names", "Where the weapons' names show.", WeaponOptions.WheelLabels.class, WeaponOptions.WheelLabels.CENTER
         )
         .<ModeSetting>group("Weapon Wheel")
         .keywords(new String[]{"labels", "names", "text", "center"});
      this.blur = new BooleanSetting("wheel.blur", "Blur Behind", "Blurs and dims the world behind the wheel (the island's glass blur).", true)
         .<BooleanSetting>group("Weapon Wheel")
         .keywords(new String[]{"blur", "dim", "background", "darken"});
      this.blurStrength = new SliderSetting("wheel.blurStrength", "Blur Strength", "How much the world behind is blurred and dimmed.", 5.0, 100.0, 60.0, 5.0)
         .unit("%")
         .<SliderSetting>group("Weapon Wheel")
         .<SliderSetting>keywords(new String[]{"blur", "dim", "strength"})
         .visibleWhen(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return WheelSettings.this.blur.get();
            }
         });
      ArrayList list = new ArrayList();
      Collections.addAll(
         list,
         this.key,
         this.tryIt,
         this.slots,
         this.perPage,
         this.mode,
         this.quickSwap,
         this.freezeCamera,
         this.closeOnDamage,
         this.theme,
         this.background,
         this.backgroundOpacity,
         this.slotColor,
         this.highlight,
         this.border,
         this.glass,
         this.size,
         this.hole,
         this.gap,
         this.icons,
         this.labels,
         this.blur,
         this.blurStrength
      );
      this.all = Collections.unmodifiableList(list);
   }
}
