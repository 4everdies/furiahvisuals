package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.core.HitConfirmer;
import meowtils.furiahvisuals.core.Keybinds;
import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.gui.popup.Popover;
import meowtils.furiahvisuals.gui.popup.Popovers;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.hit.HitContext;
import meowtils.furiahvisuals.module.hit.HitMeasurer;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.render.OptiFineCompat;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.KeyBindSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;

public final class WeaponsModule extends Module {
   static final boolean CUSTOM_WIDGETS = true;
   final WeaponLibrary library;
   final WeaponRulesSetting rules;
   private final Map<String, WeaponSettings> settings = new LinkedHashMap<>();
   private WeaponSettings importedSettings;
   private String selected = "vanilla";
   WeaponDef.Category category;
   final WeaponViews.Stage stage = new WeaponViews.Stage();
   private WeaponViews views;
   private boolean editing;
   private long editorSeen;
   final ModeSetting<WeaponOptions.WeaponStyle> style;
   final BooleanSetting pixelEffects;
   final ModeSetting<WeaponOptions.GlowStyle> glowStyle;
   final ModeSetting<WeaponOptions.PixelShading> pixelShading;
   final BooleanSetting switchAnimation;
   final BooleanSetting autoPixelate;
   final BooleanSetting otherPlayers;
   final SliderSetting otherDistance;
   final BooleanSetting icons;
   final BooleanSetting dropped;
   final ModeSetting<WeaponOptions.Enchant> enchant;
   final ModeSetting<WeaponOptions.AutoInspect> autoInspect;
   final SliderSetting autoEvery;
   final SliderSetting autoMin;
   final SliderSetting autoMax;
   final BooleanSetting autoNotSprinting;
   final BooleanSetting autoFirstPersonOnly;
   final BooleanSetting afterEquip;
   final SliderSetting afterEquipChance;
   final ModeSetting<WeaponOptions.KeyAgain> keyAgain;
   final SliderSetting inspectSpeed;
   final BooleanSetting cancelOnDamage;
   final BooleanSetting motionBlur;
   final BooleanSetting dualWield;
   final ModeSetting<WeaponOptions.DualSwings> dualSwings;
   final ModeSetting<WeaponOptions.Draw> draw;
   final BooleanSetting blockHit;
   final ModeSetting<WeaponOptions.EmptyHand> emptyHand;
   final BooleanSetting hideArms;
   final ModeSetting<WeaponOptions.AntiAliasing> antiAliasing;
   final ModeSetting<WeaponOptions.TextureSize> textureSize;
   final SliderSetting lodDistance;
   final WheelSettings wheelSettings;
   final WeaponWheelContent wheelContent;
   final WeaponWheel wheel;
   private WheelIcons wheelIcons;
   final ClipLibrary clips = new ClipLibrary(new File(FuriahsVisuals.instance.getDataDir(), "weapons/animations"));
   final Inspects inspects = new Inspects(this);
   final ChainsawSound chainsawSound = new ChainsawSound(this);
   private final HitMeasurer measurer = new HitMeasurer();
   private HitContext lastHit;
   private int lastHitTick = -1;
   private WeaponShader shaders;
   private WeaponRenderer renderer;
   private FirstPersonRenderer firstPerson;
   private ViewModels viewModels;
   private VanillaView vanillaView;
   private ThirdPerson thirdPerson;
   private WeaponIcons weaponIcons;
   private int ticks;
   private boolean pixelatedOnce;
   static boolean maskPass;
   private final Map<WeaponDef, boolean[]> importFiles = new WeakHashMap<>();
   static boolean itemsAsThemselves;
   private int vanillaDepth;
   private volatile String importMessage;
   private volatile boolean importError;
   static boolean forceSimpleForTest;
   static boolean timingForTest;
   private static long fpNanos;
   private static long fpMax;
   private static long tpNanos;
   private static long tpMax;
   private static int fpFrames;
   private static int tpDraws;
   static String tagHoverForTest;
   private String compareFrom;

   public WeaponsModule() {
      super("weapons", "Weapons", "High-quality 3D weapons in place of swords (or any item), with inspects and dual wield.", true);
      this.library = new WeaponLibrary(new File(FuriahsVisuals.instance.getDataDir(), "weapons"));
      AssetSource.addListener(new Runnable() {
         @Override
         public void run() {
            WeaponsModule.this.clips.reload();
            WeaponAssets.releaseAll();
         }
      });
      this.style = this.add(
         new WeaponStyleSetting(
               this,
               "style",
               "Weapon Style",
               "Every weapon in its smooth HD model or as 3D pixel art that fits Minecraft (a weapon can also have its own Style)."
            )
            .keywords(new String[]{"pixel", "pixelated", "voxel", "8-bit", "retro", "minecraft style", "hd", "pixel art", "style"})
      );
      this.add(new PageRow("categories", "Categories", "Shows the weapons of one kind.", new Supplier<Widget>() {
         public Widget get() {
            return new WeaponCategoryWidget(WeaponsModule.this);
         }
      }).keywords(new String[]{"category", "knives", "swords", "polearms", "axes", "hammers", "bows", "modern", "magic", "imported"}));
      this.rules = this.add(new WeaponRulesSetting(this).keywords(new String[]{"replace", "rules", "swords", "axes", "item", "name contains", "equip"}));
      this.add(new PageRow("cards", "Weapons", "Every weapon: click one to see it and its settings, Equip to use it.", new Supplier<Widget>() {
         public Widget get() {
            return new WeaponCardsWidget(WeaponsModule.this);
         }
      }).keywords(new String[]{"weapon", "cards", "equip", "pick"}));
      this.add(new PageRow("preview", "Preview", "The selected weapon in first person, third person, or alone.", new Supplier<Widget>() {
         public Widget get() {
            return new WeaponPreviewWidget(WeaponsModule.this);
         }
      }).keywords(new String[]{"preview", "first person", "third person", "model", "orbit", "zoom"}));
      this.add(
         new PageRow(
               "gripSetup",
               "Grip Setup",
               "Your imported weapon: where the fist holds it, how it is turned, its size, one or two hands, its animations.",
               new Supplier<Widget>() {
                  public Widget get() {
                     return new GripSetupWidget(WeaponsModule.this);
                  }
               }
            )
            .<PageRow>keywords(new String[]{"grip", "import", "imported", "obj", "custom weapon", "hands", "animations"})
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return WeaponsModule.this.selectedDef().isImported();
               }
            })
      );

      for (WeaponDef weaponDef : this.library.all()) {
         if (!weaponDef.isImported()) {
            WeaponSettings s = new WeaponSettings(this, weaponDef, WeaponSettings.prefixOf(weaponDef), this.owner(weaponDef.id, weaponDef.name));

            for (Setting x : s.all) {
               this.add(x);
            }

            this.settings.put(weaponDef.id, s);
         }
      }

      this.importedSettings = new WeaponSettings(this, importedStandIn(), "imported", this.owner("imported", "Imported weapons"));

      for (Setting setting : this.importedSettings.all) {
         this.add(setting);
      }

      this.pixelEffects = this.add(
         new BooleanSetting(
               "pixel.effects", "Pixel Effects", "Pixelated weapons' own effects (swing trails, sparks, smoke, motes, glows) turn to pixel art too.", true
            )
            .<BooleanSetting>group("Pixelated")
            .keywords(new String[]{"pixel", "effects", "particles", "trail"})
      );
      this.glowStyle = this.add(
         new ModeSetting<>(
               "pixel.glowStyle",
               "Glow Style",
               "How a Pixelated weapon's glowing pixels glow.",
               WeaponOptions.GlowStyle.class,
               WeaponOptions.GlowStyle.PIXEL_HALO
            )
            .<ModeSetting>group("Pixelated")
            .keywords(new String[]{"pixel", "glow", "halo", "bloom"})
      );
      this.pixelShading = this.add(
         new ModeSetting<>(
               "pixel.shading",
               "Shading",
               "How light falls on a Pixelated weapon's faces.",
               WeaponOptions.PixelShading.class,
               WeaponOptions.PixelShading.VANILLA
            )
            .<ModeSetting>group("Pixelated")
            .keywords(new String[]{"pixel", "lighting", "flat", "shading"})
      );
      this.switchAnimation = this.add(
         new BooleanSetting(
               "pixel.switchAnimation",
               "Switch Animation",
               "In first person, a quick mosaic as the weapon changes style: its pixels grow, then the new style resolves.",
               true
            )
            .<BooleanSetting>group("Pixelated")
            .keywords(new String[]{"pixel", "mosaic", "transition", "switch"})
      );
      this.add(
         new KeyBindSetting(
               "pixel.key",
               "Toggle Weapon Style",
               "A key that switches Weapon Style between HD and Pixelated, with the island's notice (also in Options › Controls).",
               new Supplier<KeyBinding>() {
                  public KeyBinding get() {
                     return Keybinds.toggleWeaponStyle;
                  }
               }
            )
            .<KeyBindSetting>group("Pixelated")
            .keywords(new String[]{"pixel", "keybind", "hd", "style", "toggle"})
      );
      this.autoPixelate = this.add(
         new BooleanSetting(
               "pixel.autoPixelate",
               "Auto-Pixelate Imported Models",
               "An imported OBJ model gets a Pixelated version of its own: voxels a thirty-second of a block, up to 32 colors in ramps, made once at import and kept in its folder (made again on Reload if the model or its size changed). Off: it stays HD in Pixelated (\"HD only\" on its card).",
               true
            )
            .<BooleanSetting>group("Pixelated")
            .keywords(new String[]{"pixel", "pixelate", "import", "obj", "voxel", "voxelize", "auto"})
      );
      this.autoPixelate.addListener(new Runnable() {
         @Override
         public void run() {
            WeaponsModule.this.pixelatedOnce = false;
         }
      });
      this.otherPlayers = this.add(
         new BooleanSetting("otherPlayers", "Other Players", "Shows weapons in other players' hands too, by your rules (only on your screen).", false)
            .<BooleanSetting>group("Show")
            .keywords(new String[]{"others", "players", "multiplayer"})
      );
      this.otherDistance = this.add(
         new SliderSetting("otherDistance", "Other Players' Distance", "Up to how far away.", 8.0, 64.0, 32.0, 1.0)
            .unit(" blocks")
            .<SliderSetting>group("Show")
            .visibleWhen(on(this.otherPlayers))
      );
      this.icons = this.add(
         new BooleanSetting(
               "icons", "Hotbar & Inventory Icons", "Items that turn into a 3D weapon show an icon of it (the count and durability bar stay).", true
            )
            .<BooleanSetting>group("Show")
            .keywords(new String[]{"hotbar", "inventory", "icon"})
      );
      this.dropped = this.add(
         new BooleanSetting("dropped", "Dropped Items & Item Frames", "The 3D weapon on the ground and in item frames too.", false)
            .<BooleanSetting>group("Show")
            .keywords(new String[]{"item frame", "ground"})
      );
      this.enchant = this.add(
         new ModeSetting<>("enchant", "Enchanted Weapons", "How an enchanted 3D weapon shimmers.", WeaponOptions.Enchant.class, WeaponOptions.Enchant.SHEEN)
            .<ModeSetting>group("Show")
            .keywords(new String[]{"glint", "enchantment"})
      );
      this.autoInspect = this.add(
         new ModeSetting<>(
               "autoInspect",
               "Auto Inspect",
               "Inspects on their own now and then, when you're idle.",
               WeaponOptions.AutoInspect.class,
               WeaponOptions.AutoInspect.OFF
            )
            .<ModeSetting>group("Inspect")
            .keywords(new String[]{"automatic", "idle"})
      );
      this.autoEvery = this.add(
         new SliderSetting("autoEvery", "Every", "How often.", 5.0, 300.0, 45.0, 5.0)
            .unit(" s")
            .<SliderSetting>group("Inspect")
            .visibleWhen(is(this.autoInspect, WeaponOptions.AutoInspect.EVERY))
      );
      this.autoMin = this.add(
         new SliderSetting("autoMin", "At Least", "The shortest wait.", 20.0, 90.0, 20.0, 1.0)
            .unit(" s")
            .<SliderSetting>group("Inspect")
            .visibleWhen(is(this.autoInspect, WeaponOptions.AutoInspect.RANDOM))
      );
      this.autoMax = this.add(
         new SliderSetting("autoMax", "At Most", "The longest wait.", 20.0, 90.0, 90.0, 1.0)
            .unit(" s")
            .<SliderSetting>group("Inspect")
            .visibleWhen(is(this.autoInspect, WeaponOptions.AutoInspect.RANDOM))
      );
      BooleanSupplier auto = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return WeaponsModule.this.autoInspect.get() != WeaponOptions.AutoInspect.OFF;
         }
      };
      this.autoNotSprinting = this.add(
         new BooleanSetting("autoNotSprinting", "Not While Sprinting", "Auto inspects wait until you stop sprinting.", true)
            .<BooleanSetting>group("Inspect")
            .visibleWhen(auto)
      );
      this.autoFirstPersonOnly = this.add(
         new BooleanSetting("autoFirstPersonOnly", "Only In First Person", "Auto inspects only in first person.", true)
            .<BooleanSetting>group("Inspect")
            .visibleWhen(auto)
      );
      this.afterEquip = this.add(
         new BooleanSetting("afterEquip", "After Equipping", "A chance to inspect right after drawing a weapon.", false).group("Inspect")
      );
      this.afterEquipChance = this.add(
         new SliderSetting("afterEquipChance", "Chance", "How likely.", 0.0, 100.0, 30.0, 5.0)
            .unit("%")
            .<SliderSetting>group("Inspect")
            .visibleWhen(on(this.afterEquip))
      );
      this.keyAgain = this.add(
         new ModeSetting<>(
               "keyAgain", "Key During An Inspect", "Pressing the inspect key while one plays.", WeaponOptions.KeyAgain.class, WeaponOptions.KeyAgain.RESTART
            )
            .<ModeSetting>group("Inspect")
            .keywords(new String[]{"inspect key", "keybind", "f"})
      );
      this.inspectSpeed = this.add(
         new SliderSetting("inspectSpeed", "Inspect Speed", "How fast inspects play.", 0.5, 1.5, 1.0, 0.05).unit("x").group("Inspect")
      );
      this.cancelOnDamage = this.add(new BooleanSetting("cancelOnDamage", "Cancel When Hurt", "Taking damage stops an inspect.", true).group("Inspect"));
      this.motionBlur = this.add(
         new BooleanSetting("motionBlur", "Motion Blur", "A soft afterimage on fast spins and flips.", true)
            .<BooleanSetting>group("Inspect")
            .keywords(new String[]{"afterimage", "blur"})
      );
      this.dualWield = this.add(
         new BooleanSetting("dualWield", "Dual Wield", "One-handed weapons in both hands (the left one is only for show: 1.8.9 has no off-hand).", false)
            .<BooleanSetting>group("Hands")
            .keywords(new String[]{"dual", "two weapons", "off hand", "akimbo"})
      );
      this.dualSwings = this.add(
         new ModeSetting<>("dualSwings", "Dual Swings", "Which hand swings.", WeaponOptions.DualSwings.class, WeaponOptions.DualSwings.ALTERNATE)
            .<ModeSetting>group("Hands")
            .visibleWhen(on(this.dualWield))
      );
      this.draw = this.add(
         new ModeSetting<>("draw", "Draw", "The draw when you switch to a weapon.", WeaponOptions.Draw.class, WeaponOptions.Draw.FULL)
            .<ModeSetting>group("Hands")
            .keywords(new String[]{"equip", "switch"})
      );
      this.blockHit = this.add(
         new BooleanSetting("blockHit", "1.7-Style Block-Hit", "Swings still play over the guard while you block.", true)
            .<BooleanSetting>group("Hands")
            .keywords(new String[]{"block hit", "blockhit", "1.7", "old animations"})
      );
      this.emptyHand = this.add(
         new ModeSetting<>(
               "emptyHand",
               "Empty Left Hand",
               "The free left fist with one-handed weapons.",
               WeaponOptions.EmptyHand.class,
               WeaponOptions.EmptyHand.SPRINT_INSPECT
            )
            .<ModeSetting>group("Hands")
            .keywords(new String[]{"left hand", "fist"})
      );
      this.hideArms = this.add(
         new BooleanSetting("hideArms", "Hide Arms", "Only the weapons, floating.", false)
            .<BooleanSetting>group("Hands")
            .keywords(new String[]{"arms", "hands", "floating"})
      );
      this.antiAliasing = this.add(
         new ModeSetting<>(
               "antiAliasing",
               "Anti-Aliasing",
               "First-person weapons are drawn at a higher resolution and smoothed down, so thin blades never shimmer.",
               WeaponOptions.AntiAliasing.class,
               WeaponOptions.AntiAliasing.X2
            )
            .<ModeSetting>group("Quality")
            .keywords(new String[]{"aa", "smooth", "jagged"})
      );
      this.textureSize = this.add(
         new ModeSetting<>(
               "textureSize",
               "Texture Size",
               "HD weapons' texture resolution (Pixelated ones have none).",
               WeaponOptions.TextureSize.class,
               WeaponOptions.TextureSize.K1
            )
            .<ModeSetting>group("Quality")
            .keywords(new String[]{"resolution", "textures"})
      );
      this.lodDistance = this.add(
         new SliderSetting("lodDistance", "Full Detail Within", "Past this distance, lighter models are used.", 4.0, 24.0, 8.0, 1.0)
            .unit(" blocks")
            .<SliderSetting>group("Quality")
            .keywords(new String[]{"lod", "level of detail"})
      );
      this.add(
         new PageRow(
               "import",
               "Import Your Own",
               "A model of your own: an OBJ (up to 150,000 triangles, textures up to 4096 pixels), or pixel art from Blockbench, a Java item model or MagicaVoxel: Import, Open Folder, Reload.",
               new Supplier<Widget>() {
                  public Widget get() {
                     return new WeaponImportWidget(WeaponsModule.this);
                  }
               }
            )
            .<PageRow>group("Import")
            .keywords(
               new String[]{
                  "import",
                  "obj",
                  "model",
                  "custom weapon",
                  "folder",
                  "reload",
                  "own weapon",
                  "blockbench",
                  "bbmodel",
                  "item model",
                  "magicavoxel",
                  "vox",
                  "voxel",
                  "pixel art"
               }
            )
      );
      this.wheelSettings = new WheelSettings(this);

      for (Setting x : this.wheelSettings.all) {
         this.add(x);
      }

      this.wheelContent = new WeaponWheelContent(this, this.wheelSettings);
      this.wheel = new WeaponWheel(this.wheelSettings, this.wheelContent);
      this.wheelSettings
         .freezeCamera
         .unavailableWhen(
            new Supplier<String>() {
               public String get() {
                  return WeaponHooks.turnHooked()
                     ? null
                     : "Another mod changes how the camera turns, so the mouse can't move the wheel's selector here: the scroll wheel picks";
               }
            }
         );
      BooleanSupplier booleanSupplier = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return !WeaponsModule.this.editing();
         }
      };

      for (Setting s : this.getSettings()) {
         if (s != this.getEnabledSetting() && !"View Model".equals(s.getGroup())) {
            s.visibleWhen(booleanSupplier);
         }
      }
   }

   private static BooleanSupplier on(final BooleanSetting b) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return b.get();
         }
      };
   }

   private static <E extends Enum<E>> BooleanSupplier is(final ModeSetting<E> m, final E value) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return m.get() == value;
         }
      };
   }

   private static WeaponDef importedStandIn() {
      JsonObject o = new JsonObject();
      o.addProperty("id", "imported");
      o.addProperty("name", "Imported");
      return WeaponDef.bundled(o, "weapons/imported/");
   }

   private SettingStyle owner(final String id, final String name) {
      return new SettingStyle() {
         @Override
         public String styleName() {
            return name;
         }

         @Override
         public boolean isShown() {
            return id.equals(WeaponsModule.this.selectedSettingsId());
         }

         @Override
         public boolean isActive() {
            return this.isShown();
         }

         @Override
         public boolean isMultiple() {
            return false;
         }

         @Override
         public void show() {
            WeaponsModule.this.select(id.equals("imported") ? WeaponsModule.this.firstImportedId() : id);
         }

         @Override
         public String kind() {
            return "weapon";
         }

         @Override
         public String kinds() {
            return "weapons";
         }

         @Override
         public String searchGroup(String group) {
            return group == null ? name : name + " › " + group;
         }

         @Override
         public boolean foundElsewhereByNameOnly() {
            return true;
         }
      };
   }

   private String firstImportedId() {
      List<WeaponDef> imported = this.library.imported();
      return imported.isEmpty() ? this.selected : imported.get(0).id;
   }

   private String selectedSettingsId() {
      WeaponDef d = this.library.get(this.selected);
      return d != null && d.isImported() ? "imported" : this.selected;
   }

   void select(String id) {
      if (this.library.get(id) != null) {
         this.selected = id;
      }
   }

   String selected() {
      return this.selected;
   }

   public String swingTrailSettingId() {
      WeaponDef def = this.library.get(this.selected);
      WeaponSettings shown = def == null ? null : this.settingsOf(def);
      if (shown != null && shown.trail != null) {
         return shown.trail.getId();
      } else {
         for (Entry<String, WeaponSettings> e : this.settings.entrySet()) {
            if (e.getValue().trail != null && this.library.get(e.getKey()) != null) {
               this.select(e.getKey());
               return e.getValue().trail.getId();
            }
         }

         return null;
      }
   }

   WeaponDef selectedDef() {
      WeaponDef d = this.library.get(this.selected);
      return d != null ? d : this.library.vanilla();
   }

   boolean equip(String id) {
      WeaponRule r = this.rules.selectedRule();
      boolean bow = this.isBowWeapon(id);
      if (r == null) {
         this.rules.add(WeaponRule.group(bow ? WeaponRule.Group.BOWS : WeaponRule.Group.SWORDS, id));
      } else {
         if (bow && !r.takesBows()) {
            return false;
         }

         if (!id.equals(r.weapon)) {
            this.wheelContent.remember(r);
         }

         this.rules.setWeapon(r, id);
      }

      this.select(id);
      return true;
   }

   boolean bowBlocked(String id) {
      WeaponRule r = this.rules.selectedRule();
      return r != null && this.isBowWeapon(id) && !r.takesBows();
   }

   static String seenKey(WeaponDef def) {
      return "card.weapon." + def.id;
   }

   static boolean showsNew(WeaponDef def) {
      return def.isNew && !MenuMemory.seen(seenKey(def));
   }

   void playDraw(WeaponDef def) {
      if (def != null) {
         if (def.isVanilla()) {
            Minecraft.getMinecraft().getItemRenderer().resetEquippedProgress();
            if (this.vanillaView != null) {
               this.vanillaView.reset();
            }
         } else if (this.viewModels != null) {
            this.viewModels.forceDraw();
         }
      }
   }

   void preloadWheel() {
      for (WheelSlotsSetting.Slot slot : this.wheelSettings.slots.get()) {
         WeaponDef d = this.library.get(slot.weapon);
         if (d != null && !d.isVanilla()) {
            WeaponAssets.of(d, WeaponAssets.Tier.CARD).request(256);
         }
      }
   }

   WheelIcons wheelIcons() {
      this.ensureRenderers();
      if (this.wheelIcons == null) {
         this.wheelIcons = new WheelIcons(this, this.renderer);
      }

      return this.wheelIcons;
   }

   @Override
   public void onRenderOverlay(float partialTicks) {
      if (this.wheel.showing() && !this.wheel.inMenu) {
         this.wheel.renderer.drawBackdropHud();
      }
   }

   @Override
   public boolean onOverlayElement(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      return type == RenderGameOverlayEvent.ElementType.CROSSHAIRS && this.wheel.showing() && !this.wheel.inMenu;
   }

   @Override
   public void onRenderHud(float partialTicks) {
      if (this.wheel.showing() && !this.wheel.inMenu) {
         this.wheel.renderer.drawHud();
      }
   }

   String equippedId() {
      WeaponRule r = this.rules.selectedRule();
      return r == null ? null : r.weapon;
   }

   @Override
   public String summary() {
      String id = this.equippedId();
      WeaponDef d = id == null ? null : this.library.get(id);
      return d != null ? d.name : this.selectedDef().name;
   }

   String selectedRuleLabel(WeaponDef def) {
      WeaponRule r = this.rules.selectedRule();
      return r != null ? r.targetLabel() : (def.isBow() ? "a new rule for all bows" : "a new rule for all swords");
   }

   public List<WeaponDef> cardWeapons() {
      ArrayList<WeaponDef> out = new ArrayList<>();
      out.add(this.library.vanilla());
      out.addAll(this.bundledWeapons());
      return out;
   }

   int countIn(WeaponDef.Category c) {
      int n = 0;

      for (WeaponDef d : this.library.all()) {
         if (d.category == c) {
            n++;
         }
      }

      return n;
   }

   WeaponViews views() {
      this.ensureRenderers();
      if (this.views == null) {
         this.views = new WeaponViews(this, this.renderer);
      }

      return this.views;
   }

   float[] iconOf(WeaponDef def) {
      this.ensureRenderers();
      return this.weaponIcons.iconOf(def);
   }

   float[] iconOf(WeaponDef def, WeaponOptions.Finish finish) {
      this.ensureRenderers();
      return this.weaponIcons.iconOf(def, finish);
   }

   String previewProblem(WeaponDef def) {
      WeaponAssets a;
      return !def.isVanilla() && (a = WeaponAssets.of(def, WeaponAssets.Tier.FULL)).state() == WeaponAssets.State.FAILED
         ? def.name + " could not be loaded: " + a.error()
         : "The preview can't be drawn here";
   }

   boolean previewInspect(WeaponDef def, String inspect) {
      this.stage.tab = WeaponViews.Tab.FIRST_PERSON;
      return this.views().previewInspect(this.stage, def, inspect);
   }

   boolean editing() {
      return this.editing && System.nanoTime() - this.editorSeen < 500000000L;
   }

   void setEditing(boolean on) {
      this.editing = on;
      this.editorSeen = System.nanoTime();
   }

   void editorShown() {
      long now = System.nanoTime();
      if (this.editing && now - this.editorSeen > 500000000L) {
         this.editing = false;
      }

      this.editorSeen = now;
   }

   WeaponDef shownWeapon(ItemStack held) {
      WeaponDef d;
      return this.editing() && !(d = this.selectedDef()).isVanilla() ? d : this.weaponFor(held);
   }

   boolean editorShowsOnScreen(WeaponDef def) {
      if (!def.isVanilla()) {
         return true;
      } else {
         EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
         return p != null && p.getHeldItem() != null && this.weaponFor(p.getHeldItem()) == def;
      }
   }

   void editorSwing() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      if (p != null) {
         p.swingProgressInt = -1;
         p.isSwingInProgress = true;
      }
   }

   void editorBlock() {
      if (this.viewModels != null) {
         this.viewModels.guardUntil = this.viewModels.nanos() + 1200000000L;
      }
   }

   void editorDraw(WeaponDef def) {
      if (def.isVanilla()) {
         Minecraft.getMinecraft().getItemRenderer().resetEquippedProgress();
      } else if (this.viewModels != null) {
         this.viewModels.forceDraw();
      }
   }

   boolean editorInspect(String inspect) {
      ViewModels.Hand hand = this.activeHand();
      return hand != null && this.inspects.play(hand, inspect);
   }

   int copyPlacementToAll(WeaponDef def) {
      WeaponSettings from = this.settingsOf(def);
      int n = 0;

      for (WeaponSettings s : this.settings.values()) {
         if (s != from) {
            s.copyPlacementFrom(from);
            n++;
         }
      }

      if (this.importedSettings != from && !this.library.imported().isEmpty()) {
         this.importedSettings.copyPlacementFrom(from);
         n += this.library.imported().size();
      }

      return n;
   }

   @Override
   public boolean previewsInWorld() {
      return true;
   }

   @Override
   public String getLoadWarning() {
      for (WeaponRule r : this.rules.get()) {
         if (this.library.get(r.weapon) == null) {
            String folder = r.weapon.startsWith("import:") ? r.weapon.substring("import:".length()) : r.weapon;
            return "Weapons: the preset uses an imported weapon you don't have (\""
               + folder
               + "\" in config/furiahsvisuals/weapons), so its rule shows Vanilla";
         }
      }

      return null;
   }

   WeaponSettings settingsOf(WeaponDef def) {
      if (def.isImported()) {
         return this.importedSettings;
      } else {
         WeaponSettings s = this.settings.get(def.id);
         return s != null ? s : this.settings.get("vanilla");
      }
   }

   int textureSize() {
      return this.textureSize.get().pixels();
   }

   float time() {
      return (float)(GameClock.seconds() % 3600.0);
   }

   float clientTime() {
      return (float)(ClientClock.seconds() % 3600.0);
   }

   WeaponDef weaponFor(ItemStack stack) {
      WeaponRule r = this.rules.match(stack);
      if (r == null) {
         return null;
      } else {
         WeaponDef d = this.library.get(r.weapon);
         return d != null ? d : this.library.vanilla();
      }
   }

   WeaponOptions.Finish ruleFinish(ItemStack stack, WeaponDef def) {
      WeaponRule r = this.rules.match(stack);
      return r != null && r.finish != null && def != null && def.id.equals(r.weapon) ? WeaponOptions.Finish.of(r.finish) : null;
   }

   boolean softGlow(WeaponRenderer.Look look) {
      return look.pixel && this.glowStyle.get() != WeaponOptions.GlowStyle.SOFT ? look.settings == null || this.drawnAsHd(look.settings.def) : true;
   }

   boolean drawnAsHd(WeaponDef def) {
      WeaponAssets px = WeaponAssets.of(def, WeaponAssets.Tier.PIXEL);
      return !px.ready() || px.pixelModel == null;
   }

   int importStyles(WeaponDef d) {
      if (d != null && d.isImported() && d.folder != null) {
         boolean[] f = this.importFiles.get(d);
         if (f == null) {
            File px = new File(d.folder, "pixel" + File.separator + "pixel.json");
            String made = px.isFile() ? PixelImport.madeFrom(px) : null;
            f = new boolean[]{new File(d.folder, "model.obj").isFile(), px.isFile(), made != null && made.startsWith("model.obj ")};
            this.importFiles.put(d, f);
         }

         boolean pixel = f[1] && (!f[2] || this.autoPixelate.get());
         return f[0] && !pixel ? 2 : (!f[0] && pixel ? 1 : 0);
      } else {
         return 0;
      }
   }

   String styleTag(WeaponDef d) {
      int only = this.importStyles(d);
      return only == 1 ? "Pixel only" : (only == 2 ? "HD only" : (this.pixelOf(d, WeaponOptions.StyleChoice.SECTION) ? "PX" : "HD"));
   }

   public void toggleStyle() {
      WeaponOptions.WeaponStyle next = this.style.get() == WeaponOptions.WeaponStyle.HD ? WeaponOptions.WeaponStyle.PIXELATED : WeaponOptions.WeaponStyle.HD;
      this.style.set(next);
      WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
      if (wm != null) {
         wm.settingNotice("weapons.style", "Weapons: " + next.displayName);
      }
   }

   boolean pixelFor(ItemStack stack, WeaponDef def) {
      if (def != null && !def.isVanilla()) {
         WeaponRule r = this.rules.match(stack);
         String own = r != null && def.id.equals(r.weapon) ? r.style : null;
         return this.pixelOf(def, WeaponOptions.StyleChoice.of(own));
      } else {
         return false;
      }
   }

   boolean pixelOf(WeaponDef def, Boolean forced) {
      return this.pixelOf(
         def, forced == null ? WeaponOptions.StyleChoice.SECTION : (forced ? WeaponOptions.StyleChoice.PIXELATED : WeaponOptions.StyleChoice.HD)
      );
   }

   boolean pixelOf(WeaponDef def, WeaponOptions.StyleChoice own) {
      if (def != null && !def.isVanilla()) {
         int only = this.importStyles(def);
         if (only != 0) {
            return only == 1;
         } else if (own == WeaponOptions.StyleChoice.HD) {
            return false;
         } else if (own == WeaponOptions.StyleChoice.PIXELATED) {
            return true;
         } else {
            WeaponSettings s = this.settingsOf(def);
            WeaponOptions.StyleChoice mine = s.style != null ? s.style.get() : WeaponOptions.StyleChoice.SECTION;
            return mine != WeaponOptions.StyleChoice.SECTION
               ? mine == WeaponOptions.StyleChoice.PIXELATED
               : this.style.get() == WeaponOptions.WeaponStyle.PIXELATED;
         }
      } else {
         return false;
      }
   }

   static boolean ownUseAnimation(ItemStack stack, WeaponDef def) {
      EnumAction a = stack.getItemUseAction();
      return a == EnumAction.EAT || a == EnumAction.DRINK || a == EnumAction.BOW && (def == null || !def.isBow());
   }

   WeaponDef leftWeaponOf(WeaponDef def) {
      String id = this.settingsOf(def).leftWeapon.get();
      if ("same".equals(id)) {
         return def;
      } else {
         WeaponDef d = this.library.get(id);
         return d != null && !d.twoHanded ? d : def;
      }
   }

   public String command(String[] args) {
      String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
      if (action.equals("reload")) {
         int n = this.clips.reload();
         WeaponImport.reload(this);
         String problems = this.clips.problems().isEmpty() ? "" : " Problems: " + String.join("; ", this.clips.problems());
         return "Weapons: " + n + " animation clips read; reading the weapons folder again." + problems;
      } else if (action.equals("play") && args.length > 1) {
         ViewModels.Hand hand = this.activeHand();
         if (hand == null) {
            return "Hold an item that a Weapons rule covers first.";
         } else {
            return this.inspects.playClip(hand, args[1]) ? "Playing " + args[1] + "." : "There is no clip called " + args[1] + ".";
         }
      } else if (action.equals("check")) {
         final List<WeaponDef> defs = this.bundledWeapons();
         WeaponAssets.runOnLoader(new Runnable() {
            @Override
            public void run() {
               final List<String> lines = WeaponsModule.checkReport(defs);
               Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                  @Override
                  public void run() {
                     for (String l : lines) {
                        FuriahsVisuals.chat(l);
                     }
                  }
               });
            }
         });
         return "Weapons: checking " + defs.size() + " weapons in both styles...";
      } else {
         return "Usage: /furiah weapon reload | /furiah weapon play <clip> | /furiah weapon check";
      }
   }

   List<WeaponDef> bundledWeapons() {
      ArrayList<WeaponDef> out = new ArrayList<>();

      for (WeaponDef d : this.library.all()) {
         if (!d.isVanilla() && !d.isImported()) {
            out.add(d);
         }
      }

      return out;
   }

   static List<String> checkReport(List<WeaponDef> defs) {
      ArrayList<String> lines = new ArrayList<>();
      int ok = 0;
      int noPixel = 0;

      for (WeaponDef d : defs) {
         WeaponCheck.Result r = WeaponCheck.check(d);
         if (r.problems.isEmpty()) {
            ok++;
            lines.add(
               String.format(
                  Locale.ROOT, "§a✔ §f%s§7: HD and Pixelated OK (%d / %d triangles, built in %.1f ms)", d.name, r.triangles, r.farTriangles, r.buildMs
               )
            );
         } else {
            if (!r.hasPixel) {
               noPixel++;
            }

            lines.add("§c✘ §f" + d.name + "§7: " + String.join("; ", r.problems));
         }
      }

      lines.add(
         ok == defs.size()
            ? "§aWeapons: " + ok + " weapons x 2 styles OK"
            : "§eWeapons: " + ok + " of " + defs.size() + " weapons OK in both styles" + (noPixel > 0 ? " (" + noPixel + " without a Pixelated variant)" : "")
      );
      return lines;
   }

   public List<String> clipNames() {
      return this.clips.names();
   }

   ViewModels.Hand dualLeftHand() {
      return this.viewModels == null ? null : this.viewModels.dualLeft();
   }

   boolean inspecting3p() {
      ViewModels.Hand h = this.activeHand();
      return h != null && h.anim.inspectWeight() > 0.0F;
   }

   float inspectWeight3p() {
      ViewModels.Hand h = this.activeHand();
      return h == null ? 0.0F : Clip.Ease.apply(3, h.anim.inspectWeight());
   }

   Pose inspectPose3p() {
      ViewModels.Hand h = this.activeHand();
      if (h == null) {
         return null;
      } else {
         return h.def != null && h.def.isVanilla() ? this.vanillaView.pose() : h.pose;
      }
   }

   ThirdPerson thirdPerson() {
      this.ensureRenderers();
      return this.thirdPerson;
   }

   boolean icon(ItemStack stack, int x, int y, float zLevel) {
      if (this.icons.get() && !itemsAsThemselves) {
         this.ensureRenderers();
         return this.weaponIcons.draw(stack, x, y, zLevel);
      } else {
         return false;
      }
   }

   static boolean usingOwnAnimation(EntityPlayerSP p, ItemStack stack, WeaponDef def) {
      if (stack == null) {
         return false;
      } else if (stack.getItem() instanceof ItemFishingRod && p.fishEntity != null) {
         return true;
      } else if (p.getItemInUseCount() <= 0) {
         return false;
      } else {
         EnumAction a = stack.getItemUseAction();
         return a == EnumAction.EAT || a == EnumAction.DRINK || a == EnumAction.BOW && (def == null || !def.isBow());
      }
   }

   boolean isBowWeapon(String id) {
      WeaponDef d = this.library.get(id);
      return d != null && d.isBow();
   }

   private void ensureRenderers() {
      if (this.renderer == null) {
         this.shaders = new WeaponShader();
         this.renderer = new WeaponRenderer(this.shaders);
         this.renderer.module = this;
         this.firstPerson = new FirstPersonRenderer(this.renderer);
         this.viewModels = new ViewModels(this, this.firstPerson);
         this.viewModels.game = true;
         this.vanillaView = new VanillaView(this, this.viewModels);
         this.thirdPerson = new ThirdPerson(this, this.renderer);
         this.weaponIcons = new WeaponIcons(this, this.renderer);
         HitConfirmer confirmer = FuriahsVisuals.instance.getHitConfirmer();
         confirmer.addListener(new HitConfirmer.Listener() {
            @Override
            public void onConfirmedHit(HitConfirmer.Attack attack) {
               WeaponsModule.this.confirmedHit();
            }
         });
      }
   }

   boolean firstPerson(ItemStack itemToRender, float pt) {
      Minecraft mc = Minecraft.getMinecraft();
      EntityPlayerSP p = mc.thePlayer;
      if (p == null) {
         return false;
      } else {
         this.ensureRenderers();
         ItemStack held = p.getHeldItem();
         WeaponDef def = this.shownWeapon(held);
         if (this.vanillaView.drawingCopy()) {
            this.vanillaView.head(mc, held, this.library.vanilla(), this.settingsOf(this.library.vanilla()), pt);
            this.vanillaDepth++;
            return false;
         } else if (def != null && def.isVanilla() && itemToRender != null && this.weaponFor(itemToRender) == def) {
            this.vanillaView.head(mc, held, def, this.settingsOf(def), pt);
            this.vanillaDepth++;
            return false;
         } else if (def == null || def.isVanilla()) {
            WeaponDef leaving = this.weaponFor(itemToRender);
            if (leaving != null && !leaving.isVanilla()) {
               return this.viewModels.render(mc, held, leaving, this.settingsOf(leaving), pt, maskPass, true);
            } else {
               this.viewModels.reset();
               this.vanillaView.reset();
               return false;
            }
         } else {
            return usingOwnAnimation(p, held, def) ? false : this.viewModels.render(mc, held, def, this.settingsOf(def), pt, maskPass, false);
         }
      }
   }

   void firstPersonEnd(float pt) {
      if (this.vanillaDepth > 0 && this.vanillaView != null) {
         this.vanillaDepth--;
         this.vanillaView.tail(Minecraft.getMinecraft(), this.settingsOf(this.library.vanilla()), pt);
      }
   }

   /** The first-person guard is up (also covers items vanilla would not block with). */
   boolean guarding() {
      return this.viewModels != null && this.viewModels.guardActive;
   }

   ViewModels.Hand activeHand() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      if (p != null && this.viewModels != null) {
         WeaponDef def = this.shownWeapon(p.getHeldItem());
         if (def == null) {
            return null;
         } else if (def.isVanilla()) {
            return this.vanillaView.hand.def == def ? this.vanillaView.hand : null;
         } else {
            return this.viewModels.right.def == def ? this.viewModels.right : null;
         }
      } else {
         return null;
      }
   }

   @Override
   public void onAttack(EntityPlayer attacker, Entity target) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.thePlayer != null) {
         this.lastHit = this.measurer.measure(mc, mc.thePlayer, target, GameClock.nanos());
         this.lastHitTick = mc.thePlayer.ticksExisted;
      }
   }

   boolean swipeFromHit(float[] out) {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      if (this.lastHit != null && p != null && p.ticksExisted == this.lastHitTick) {
         out[0] = this.lastHit.screenSwipeX;
         out[1] = this.lastHit.screenSwipeY;
         return true;
      } else {
         return false;
      }
   }

   HitMeasurer measurer() {
      return this.measurer;
   }

   private void confirmedHit() {
      if (this.isEnabled() && this.viewModels != null) {
         this.chainsawSound.bite();
         ViewModels.Hand hand = this.activeHand();
         if (hand != null) {
            hand.hit();
         }

         if (hand != null && hand.settings != null && hand.settings.hitReaction.get()) {
            hand.motion.hit();
         }
      }
   }

   int enchantMode(ItemStack stack) {
      if (stack != null && stack.hasEffect()) {
         switch ((WeaponOptions.Enchant)this.enchant.get()) {
            case SHEEN:
               return 1;
            case VANILLA:
               return 2;
            default:
               return 0;
         }
      } else {
         return 0;
      }
   }

   @Override
   public void onWorldChange() {
      if (this.viewModels != null) {
         this.viewModels.reset();
      }

      this.chainsawSound.stopAll();
      this.lastHit = null;
   }

   @Override
   public void onClientTick() {
      if (!this.pixelatedOnce && this.autoPixelate.get()) {
         this.pixelatedOnce = true;
         WeaponImport.pixelateMissing(this);
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.thePlayer != null) {
         this.measurer.recordLook(mc.thePlayer, GameClock.nanos());
         this.inspects.tick(mc);
      }

      this.chainsawSound.tick(mc);
      this.wheel.tick();
      if (++this.ticks % 20 == 0) {
         WeaponAssets.unpinAll();

         for (WeaponRule r : this.rules.get()) {
            WeaponDef d = this.library.get(r.weapon);
            if (d != null && !d.isVanilla()) {
               WeaponAssets a = WeaponAssets.of(d, WeaponAssets.Tier.FULL);
               a.pinned = true;
               a.request(this.textureSize(), false);
            }
         }

         if (this.wheel.recentlyUsed()) {
            this.preloadWheel();
         }
      }

      if (this.ticks % 100 == 0) {
         WeaponAssets.collect();
      }
   }

   @Override
   public void onRenderWorld(float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      EntityPlayerSP p = mc.thePlayer;
      if (p != null) {
         this.measurer.recordLook(p, GameClock.nanos());
         if (mc.gameSettings.thirdPersonView != 0) {
            this.ensureRenderers();
            ItemStack held = p.getHeldItem();
            WeaponDef def = this.shownWeapon(held);
            if (def != null && def.isVanilla()) {
               this.vanillaView.updateOnly(mc, held, def, this.settingsOf(def), partialTicks);
            } else if (def != null) {
               this.viewModels.updateOnly(mc, held, def, this.settingsOf(def), partialTicks);
            }
         }
      }

      WeaponAssets.pump();
   }

   @Override
   public boolean drawsWithHudHidden() {
      return true;
   }

   @Override
   public Module.Notice getNotice() {
      if (WeaponHooks.broken()) {
         return new Module.Notice(WeaponHooks.brokenReason(), true);
      } else if (!WeaponHooks.hooksIn()) {
         return new Module.Notice(
            "Weapons can't hook into how Minecraft draws held items here (another mod changed it: "
               + WeaponHooks.missingHooks()
               + "), so it stays off and every item looks vanilla.",
            true
         );
      } else {
         return this.firstPerson != null && this.firstPerson.failure() != null ? new Module.Notice(this.firstPerson.failure(), false) : null;
      }
   }

   @Override
   protected void onDisable() {
      this.wheel.cancel(false);
      this.chainsawSound.stopAll();
      if (this.firstPerson != null) {
         this.firstPerson.release();
      }
   }

   float myChainPhase() {
      ViewModels.Hand h = this.activeHand();
      return h != null ? h.engine.phase : this.time() * 0.12F;
   }

   public void ruleWeaponForTest(String weapon) {
      this.rules.setWeapon(this.rules.get().get(0), weapon);
   }

   public void bowRuleForTest(String weapon) {
      for (WeaponRule r : this.rules.get()) {
         if (r.kind == WeaponRule.Kind.GROUP && r.group == WeaponRule.Group.BOWS) {
            this.rules.setWeapon(r, weapon);
            return;
         }
      }

      this.rules.add(WeaponRule.group(WeaponRule.Group.BOWS, weapon));
   }

   public String bowForTest() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      if (this.viewModels != null && p != null) {
         BowDraw d = this.viewModels.right.bow;
         boolean using = p.isUsingItem();
         int count = p.getItemInUseCount();
         Object[] objectArray = new Object[]{
            using ? "using" : "not using",
            count,
            using ? BowDraw.charge(72000 - count) : 0.0F,
            d.pull(),
            d.bend(),
            d.nocked() ? "nocked" : "no arrow",
            d.handOn(),
            d.releaseT >= 0.0F ? (d.fired ? ", shot" : ", let down") : ""
         };
         return String.format(Locale.ROOT, "%s, use count %d (vanilla's charge %.2f), pull %.2f, bend %.2f, %s, hand on %.2f%s", objectArray);
      } else {
         return "no view model";
      }
   }

   public String otherBowForTest(EntityPlayer other) {
      WeaponDef def = this.weaponFor(other.getHeldItem());
      float pull = ThirdPerson.bowPull(other, 0.0F);
      return String.format(
         Locale.ROOT,
         "%s, %s, use count %d, pull %.2f",
         def == null ? "no weapon" : def.id,
         other.isUsingItem() ? "using" : "not using",
         other.getItemInUseCount(),
         pull
      );
   }

   public void wheelKeyForTest(Boolean down) {
      this.wheel.testKey = down;
   }

   public String wheelForTest() {
      return this.wheel.describe();
   }

   public boolean wheelOpenForTest() {
      return this.wheel.isOpen();
   }

   public boolean wheelShowingForTest() {
      return this.wheel.showing();
   }

   public int wheelLitForTest() {
      return this.wheel.selected;
   }

   public int wheelPageForTest() {
      return this.wheel.page;
   }

   public String wheelRuleForTest() {
      WeaponRule r = this.wheelContent.targetRule();
      return r == null ? "none" : r.targetLabel() + " -> " + r.weapon + (r.finish != null ? " (" + r.finish + ")" : "");
   }

   public String wheelTargetForTest() {
      return this.wheelContent.target();
   }

   public String rulesForTest() {
      StringBuilder b = new StringBuilder();

      for (WeaponRule r : this.rules.get()) {
         b.append(b.length() == 0 ? "" : "; ").append(r.targetLabel()).append(" -> ").append(r.weapon);
      }

      return b.toString();
   }

   public boolean wheelMouseForTest(int button, boolean down, int wheelTurn) {
      return WeaponHooks.wheelMouse(button, down, wheelTurn);
   }

   public void wheelKeyInputForTest() {
      WeaponHooks.wheelKeyInput();
   }

   public boolean wheelTurnHookedForTest() {
      return WeaponHooks.turnHooked();
   }

   public void wheelSlotsForTest(String spec) {
      ArrayList<WheelSlotsSetting.Slot> list = new ArrayList<>();

      for (String part : spec.split(",")) {
         String[] p = part.trim().split(":");
         list.add(new WheelSlotsSetting.Slot(p[0], p.length > 1 ? p[1] : null));
      }

      this.wheelSettings.slots.setAll(list);
   }

   public String wheelSlotsJsonForTest() {
      return this.wheelSettings.slots.toJson().toString();
   }

   public String wheelLayoutForTest() {
      return this.wheel.renderer.layoutForTest();
   }

   public String wheelCostForTest() {
      return this.wheel.renderer.costForTest();
   }

   public String wheelPicturesForTest() {
      WheelIcons ic = this.wheelIcons();
      return String.format(Locale.ROOT, "%d drawn, %.3f ms", ic.lastDrawn, ic.lastNanos / 1000000.0);
   }

   public boolean drawingForTest() {
      ViewModels.Hand h = this.activeHand();
      return h != null && h.anim.drawing();
   }

   public void wheelForgetForTest() {
      this.wheelContent.forgetForTest();
   }

   public void tryWheelForTest() {
      this.wheel.openInMenu();
      Popovers.open(new WheelTryWidget.TryPopover(this), 0.0F, 0.0F, 10.0F, 10.0F);
   }

   public void tryWheelPointForTest(float x, float y) {
      this.wheel.pointSelector(x, y);
   }

   public void tryWheelClickForTest(int button) {
      Popover p = Popovers.current();
      if (p != null) {
         p.mouseDown(0.0F, 0.0F, button);
      }
   }

   public KeyBindSetting wheelKeySettingForTest() {
      return this.wheelSettings.key;
   }

   public boolean readyForTest(String weapon) {
      WeaponDef d = this.library.get(weapon);
      if (d != null && !d.isVanilla()) {
         WeaponAssets a = WeaponAssets.of(d, WeaponAssets.Tier.FULL);
         a.request(this.textureSize());
         if (a.state() == WeaponAssets.State.FAILED) {
            throw new IllegalStateException("weapon " + weapon + " failed to load: " + a.error());
         } else {
            return a.ready();
         }
      } else {
         return true;
      }
   }

   public boolean pixelReadyForTest(String weapon) {
      WeaponDef d = this.library.get(weapon);
      if (d != null && !d.isVanilla()) {
         WeaponAssets a = WeaponAssets.of(d, WeaponAssets.Tier.PIXEL);
         a.request(0, true);
         if (a.state() != WeaponAssets.State.FAILED) {
            return a.ready();
         } else if (!"it has no Pixelated variant".equals(a.error())) {
            throw new IllegalStateException("weapon " + weapon + "'s Pixelated variant failed to load: " + a.error());
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   public String pixelStatsForTest(String weapon) {
      WeaponDef d = this.library.get(weapon);
      WeaponAssets a = d == null ? null : WeaponAssets.of(d, WeaponAssets.Tier.PIXEL);
      if (a != null && a.ready() && a.pixelLods[0] != null) {
         PixelMesh near = a.pixelLods[0];
         PixelMesh far = a.pixelLods[1];
         float px = near.pixel;
         return String.format(
            Locale.ROOT,
            "%s: %d / %d triangles (%d without the shader), built in %.2f + %.2f ms, %.0f x %.0f x %.0f pixels, %d parts, icons %s/%s, %d KB on the graphics card",
            weapon,
            near.triangles,
            far == null ? 0 : far.triangles,
            near.bakedTriangles,
            near.buildNanos / 1000000.0,
            far == null ? 0.0 : far.buildNanos / 1000000.0,
            (near.max[0] - near.min[0]) / px,
            (near.max[1] - near.min[1]) / px,
            (near.max[2] - near.min[2]) / px,
            near.parts.size(),
            a.pixelModel.icon != null ? "32" : "-",
            a.pixelModel.icon16 != null ? "16" : "-",
            a.gpuBytes() / 1024L
         );
      } else {
         return weapon + ": no pixel model" + (a != null && a.error() != null ? " (" + a.error() + ")" : "");
      }
   }

   public List<String> weaponCheckForTest() {
      return checkReport(this.bundledWeapons());
   }

   public List<String> finishesForTest(String id) {
      WeaponDef d = this.library.get(id);
      ArrayList<String> out = new ArrayList<>();
      if (d != null) {
         for (String f : d.finishes) {
            WeaponOptions.Finish x = WeaponOptions.Finish.of(f);
            if (x != null) {
               out.add(x.name());
            }
         }
      }

      return out;
   }

   public void resetSettingForTest(String id) {
      for (Setting s : this.getSettings()) {
         if (s.getId().equals(id)) {
            s.reset();
            return;
         }
      }

      throw new IllegalArgumentException("no setting " + id);
   }

   public String heldWeaponForTest() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      WeaponDef d = p == null ? null : this.weaponFor(p.getHeldItem());
      return d == null ? "none" : d.id;
   }

   public void setForTest(String id, String value) {
      JsonPrimitive v = !value.equals("true") && !value.equals("false")
         ? (value.matches("-?[0-9.]+") ? new JsonPrimitive(Double.parseDouble(value)) : new JsonPrimitive(value))
         : new JsonPrimitive(Boolean.parseBoolean(value));

      for (Setting s : this.getSettings()) {
         if (s.getId().equals(id)) {
            s.fromJson(v);
            return;
         }
      }

      throw new IllegalArgumentException("no setting " + id);
   }

   public void holdForTest(float[] pos, float[] tip, float[] edge, float[] shoulder, float[] scale) {
      this.holdForTest(pos, tip, edge, shoulder, scale, null);
   }

   public void holdForTest(float[] pos, float[] tip, float[] edge, float[] shoulder, float[] scale, float[] leftShoulder) {
      ViewModels.testHold = pos == null ? null : new WeaponDef.Hold(pos, tip, edge, scale == null ? 1.0F : scale[0], null, leftShoulder, 18.0F, false);
      ViewModels.testShoulder = shoulder;
      if (scale != null && scale.length > 2) {
         Rig.maxLean = -scale[1];
         ArmRenderer.FIST_Y = scale[2];
      }
   }

   public void resetForTest() {
      this.resetToDefaults();
   }

   public boolean inspectForTest(String inspect) {
      ViewModels.Hand hand = this.activeHand();
      if (hand != null && inspect.startsWith("#") && hand.def != null) {
         int i = Integer.parseInt(inspect.substring(1));
         if (i >= hand.def.inspects.size()) {
            return false;
         }

         inspect = hand.def.inspects.get(i);
      }

      return hand != null && this.inspects.play(hand, inspect);
   }

   public void swingForTest() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      p.swingProgressInt = -1;
      p.isSwingInProgress = true;
   }

   public void blockForTest(boolean on) {
      ViewModels.testBlocking = on;
   }

   public boolean inspectingForTest() {
      ViewModels.Hand hand = this.activeHand();
      return hand != null && hand.anim.inspecting();
   }

   public void saveIconsForTest(File file) {
      try {
         if (this.weaponIcons != null) {
            this.weaponIcons.saveForTest(file);
         }
      } catch (IOException var3) {
         FuriahsVisuals.LOG.warn("Could not save the icon atlas", var3);
      }
   }

   public List<String> inspectsForTest(String id) {
      WeaponDef d = this.library.get(id);
      return d == null ? Collections.emptyList() : d.inspects;
   }

   public void selectForTest(String id) {
      this.select(id);
   }

   public void selectRuleForTest(int index) {
      this.rules.select(index);
   }

   public void previewBlockForTest(boolean on) {
      this.stage.blocking = on;
   }

   public boolean equipForTest(String id) {
      return this.equip(id);
   }

   public void previewForTest(String tab, String stance, float zoom) {
      if (tab != null) {
         this.stage.tab = WeaponViews.Tab.valueOf(tab);
      }

      if (stance != null) {
         this.stage.stance = WeaponViews.Stance.valueOf(stance);
      }

      if (zoom > 0.0F) {
         this.stage.zoom = zoom;
      }
   }

   public boolean previewInspectForTest(String inspect) {
      return this.previewInspect(this.selectedDef(), inspect);
   }

   public void editorForTest(boolean on) {
      this.setEditing(on);
   }

   public void categoryForTest(String name) {
      this.category = "ALL".equals(name) ? null : WeaponDef.Category.valueOf(name);
   }

   public void ruleEditorForTest(String kind) {
      WeaponRulesWidget.testEditor = kind;
   }

   public String clipsForTest() {
      return this.clips.names().size() + " clips" + (this.clips.problems().isEmpty() ? "" : ", problems: " + this.clips.problems());
   }

   public int fxCountForTest() {
      return this.firstPerson == null ? -1 : this.firstPerson.fx.count();
   }

   public int chainsawPlayingForTest() {
      return this.chainsawSound.playingForTest();
   }

   public String fxForTest() {
      return this.firstPerson == null ? "" : this.firstPerson.fx.describe();
   }

   public void smokeForTest() {
      if (this.firstPerson != null) {
         for (int i = 0; i < 4; i++) {
            this.firstPerson.fx.smoke(new float[]{0.0F, 0.0F, -1.2F}, new float[]{0.0F, 0.2F, 0.0F}, 1.0F, 2.5F);
         }
      }
   }

   void sayImport(String message, boolean error) {
      this.importError = error;
      this.importMessage = message;
   }

   String importMessage() {
      return this.importMessage;
   }

   boolean importError() {
      return this.importError;
   }

   static void noteFirstPerson(long nanos) {
      fpNanos += nanos;
      fpMax = Math.max(fpMax, nanos);
      fpFrames++;
   }

   static void noteThirdPerson(long nanos) {
      tpNanos += nanos;
      tpMax = Math.max(tpMax, nanos);
      tpDraws++;
   }

   public void timingForTest(boolean on) {
      timingForTest = on;
      tpMax = 0L;
      tpNanos = 0L;
      fpMax = 0L;
      fpNanos = 0L;
      tpDraws = 0;
      fpFrames = 0;
   }

   public String timingReportForTest() {
      return String.format(
         Locale.ROOT,
         "first person %.3f ms a frame (max %.3f, %d frames); third person %.3f ms a weapon (max %.3f, %d)",
         fpFrames == 0 ? 0.0 : fpNanos / 1000000.0 / fpFrames,
         fpMax / 1000000.0,
         fpFrames,
         tpDraws == 0 ? 0.0 : tpNanos / 1000000.0 / tpDraws,
         tpMax / 1000000.0,
         tpDraws
      );
   }

   static boolean simpleShading() {
      return OptiFineCompat.shadersActive() || forceSimpleForTest;
   }

   public void simpleForTest(boolean on) {
      forceSimpleForTest = on;
   }

   public void arcsForTest(boolean on) {
      ViewModels.testArcs = on;
   }

   public void tagHoverForTest(String id) {
      tagHoverForTest = id != null && !id.isEmpty() ? id : null;
   }

   public void compareForTest(boolean on, float split) {
      if (on && this.selectedDef().isVanilla()) {
         for (WeaponDef d : this.library.all()) {
            if (!d.isVanilla() && !d.isImported()) {
               this.compareFrom = this.selected;
               this.select(d.id);
               break;
            }
         }
      } else if (!on && this.compareFrom != null) {
         this.select(this.compareFrom);
         this.compareFrom = null;
      }

      this.stage.compare = on;
      if (split >= 0.0F) {
         this.stage.split(split);
      }
   }

   public float[] drawCompareHandleForColorCheck(float x, float y, float h, int accent) {
      return WeaponPreviewWidget.handleForColorCheck(x, y, h, accent);
   }

   public float[] drawStyleRingForColorCheck(float x, float y, float w, float h, int accent) {
      return StylePickerWidget.ringForColorCheck(x, y, w, h, accent);
   }

   public float[] drawStyleTagForColorCheck(float x, float y, int accent) {
      return WeaponCardsWidget.tagForColorCheck(x, y, accent);
   }

   public float[] drawNewPillForColorCheck(float x, float y, int accent) {
      return WeaponCardsWidget.newForColorCheck(x, y, accent);
   }

   public void importFileForTest(String path) {
      this.importMessage = null;
      String[] names = path.split("\\|");
      File[] files = new File[names.length];

      for (int i = 0; i < names.length; i++) {
         files[i] = new File(names[i]);
      }

      WeaponImport.importFiles(this, files);
   }

   public void importReloadForTest() {
      this.importMessage = null;
      WeaponImport.reload(this);
   }

   public String lastImportForTest() {
      return WeaponImport.lastId;
   }

   public String importStylesForTest(String id) {
      WeaponDef d = this.library.get(id);
      return d == null ? "no weapon " + id : d.name + ": " + this.styleTag(d);
   }

   public String touchImportForTest(String id) {
      WeaponDef d = this.library.get(id);
      if (d != null && d.folder != null) {
         for (String k : new String[]{"bbmodel", "json", "vox", "obj"}) {
            File f = new File(d.folder, k.equals("obj") ? "model.obj" : "pixel." + k);
            if (f.isFile()) {
               f.setLastModified(System.currentTimeMillis());
               return "touched " + f.getName();
            }
         }

         return "nothing to touch in " + d.folder.getName();
      } else {
         return "no weapon " + id;
      }
   }

   public String nameForTest(String id) {
      WeaponDef d = this.library.get(id);
      return d == null ? id : d.name;
   }

   public String categoryNameForTest(String id) {
      WeaponDef d = this.library.get(id);
      return d == null ? "" : d.categoryName();
   }

   public void hitForTest() {
      ViewModels.Hand h = this.activeHand();
      if (h != null) {
         h.hit();
      }
   }

   public float flareForTest() {
      return ViewModels.lastFlareForTest;
   }

   public String importPartsForTest(String id) {
      WeaponDef d = this.library.get(id);
      if (d != null && d.folder != null) {
         try {
            JsonObject o = new JsonParser()
               .parse(new String(Files.readAllBytes(new File(d.folder, "weapon.json").toPath()), StandardCharsets.UTF_8))
               .getAsJsonObject();
            StringBuilder sb = new StringBuilder("parts");

            for (String key : new String[]{"parts", "pivots"}) {
               if (key.equals("pivots")) {
                  sb.append("; pivots");
               }

               JsonElement e;
               if ((e = o.get(key)) != null && e.isJsonObject()) {
                  for (Entry p : e.getAsJsonObject().entrySet()) {
                     sb.append(' ').append((String)p.getKey());
                     if (key.equals("pivots") && ((JsonElement)p.getValue()).isJsonObject() && ((JsonElement)p.getValue()).getAsJsonObject().has("origin")) {
                        sb.append(((JsonElement)p.getValue()).getAsJsonObject().get("origin"));
                     }
                  }
               }
            }

            return sb.toString();
         } catch (RuntimeException | IOException var12) {
            return "can't read its weapon.json: " + var12;
         }
      } else {
         return "no weapon " + id;
      }
   }

   public String replaceImportForTest(String id, String path) {
      WeaponDef d = this.library.get(id);
      File from = new File(path);
      String kind = WeaponImport.kind(from.getName());
      if (d != null && d.folder != null && kind != null && from.isFile()) {
         try {
            File to = new File(d.folder, "pixel." + kind);
            Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
            to.setLastModified(System.currentTimeMillis());
            return "replaced " + to.getName() + " of " + d.folder.getName() + " with " + from.getName();
         } catch (IOException var7) {
            return "could not replace: " + var7;
         }
      } else {
         return "can't replace " + id + " with " + path;
      }
   }

   public String importMessageForTest() {
      return (this.importError ? "[error] " : "[ok] ") + this.importMessage;
   }

   public void importGripForTest(String id, String spec) {
      WeaponDef def = this.library.get(id);
      if (def != null && def.folder != null) {
         try {
            File f = new File(def.folder, "weapon.json");

            JsonObject o;
            try (InputStreamReader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
               o = new JsonParser().parse(r).getAsJsonObject();
            }

            JsonObject g = o.getAsJsonObject("import");
            float[] turn = WeaponImport.floats(g, "turn", 3);

            for (String kv : spec.split(",")) {
               String[] p = kv.split("=", 2);
               String var13 = p[0];
               switch (var13) {
                  case "along":
                  case "size":
                     g.addProperty(p[0], Float.parseFloat(p[1]));
                     break;
                  case "pitch":
                     turn[0] = Float.parseFloat(p[1]);
                     break;
                  case "spin":
                     turn[1] = Float.parseFloat(p[1]);
                     break;
                  case "roll":
                     turn[2] = Float.parseFloat(p[1]);
                     break;
                  case "hands":
                     g.addProperty("hands", Integer.parseInt(p[1]));
                     break;
                  case "set":
                     g.addProperty("set", p[1]);
               }
            }

            g.add("turn", WeaponImport.array(turn));
            WeaponImport.applyGrip(o);
            WeaponImport.write(f, o);
            this.library.reloadImported();
            this.sayImport("Saved the grip of " + def.name + " (" + spec + ")", false);
         } catch (RuntimeException | IOException var23) {
            this.sayImport("Could not set the grip: " + var23, true);
         }
      } else {
         this.sayImport("No imported weapon " + id, true);
      }
   }

   public void shimmerForTest(float k) {
      WeaponFx.testShimmer = k;
   }

   public String memoryForTest() {
      return String.format(
         Locale.ROOT, "%d weapon asset set(s) loaded, %.1f MB on the graphics card", WeaponAssets.loadedCount(), WeaponAssets.totalGpuBytes() / 1048576.0
      );
   }
}
