package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class WeaponSettings {
   public final WeaponDef def;
   public final String prefix;
   public ModeSetting<WeaponOptions.StyleChoice> style;
   public FinishSetting finish;
   public ModeSetting<WeaponOptions.BladeMaterial> customBlade;
   public ColorSetting paint;
   public ModeSetting<WeaponOptions.HandleMaterial> handleMaterial;
   public ColorSetting handleColor;
   public ColorSetting accent;
   public SliderSetting glow;
   public SliderSetting wear;
   public SliderSetting sizeFirst;
   public SliderSetting sizeThird;
   public final ModeSetting<WeaponOptions.Placement> placement;
   public final SliderSetting offsetX;
   public final SliderSetting offsetY;
   public final SliderSetting offsetZ;
   public final SliderSetting pitch;
   public final SliderSetting yaw;
   public final SliderSetting roll;
   public final SliderSetting size;
   public final SliderSetting fov;
   public final BooleanSetting leftHanded;
   public BooleanSetting weaponMotion;
   public final SliderSetting breathing;
   public final SliderSetting walkBob;
   public final SliderSetting lookSway;
   public final SliderSetting strafeTilt;
   public final SliderSetting jumpLand;
   public final SliderSetting crouch;
   public final SliderSetting sprintPose;
   public final SliderSetting hurt;
   public final BooleanSetting hitReaction;
   public final BooleanSetting fidgets;
   public BooleanSetting vibration;
   public ModeSetting<WeaponOptions.SwingPattern> swingPattern;
   public ModeSetting<WeaponOptions.Trail> trail;
   public ColorSetting trailColor;
   public final InspectWeightsSetting inspects;
   public final ModeSetting<WeaponOptions.InspectOrder> inspectOrder;
   public final WeaponChoiceSetting leftWeapon;
   public BooleanSetting sound;
   public BooleanSetting soundIdle;
   public BooleanSetting soundBite;
   public BooleanSetting soundOthers;
   public SliderSetting soundVolume;
   public BooleanSetting heatShimmer;
   final List<Setting> all = new ArrayList<>();
   private final Map<Setting, JsonElement> defaults = new IdentityHashMap<>();
   private boolean modifiedKnown;
   private boolean modified;
   private int version;

   WeaponSettings(final WeaponsModule module, final WeaponDef def, String prefix, SettingStyle owner) {
      final PageRow[] editorRow = new PageRow[1];
      this.def = def;
      this.prefix = prefix;
      boolean vanilla = def.isVanilla();
      String name = def.name;
      if (!vanilla) {
         this.style = this.add(
            new ModeSetting<>(
                  this.id("style"),
                  "Style",
                  "This weapon's own style: the section's Weapon Style, or always HD or Pixelated.",
                  WeaponOptions.StyleChoice.class,
                  WeaponOptions.StyleChoice.SECTION
               )
               .<ModeSetting>group("Look")
               .keywords(new String[]{"pixel", "pixelated", "hd", "style", "voxel"}),
            owner
         );
         final FinishSetting f = this.finish = this.add(
            new FinishSetting(this.id("finish"), "Finish", "The blade's material and look.", def.finishes, def.finish)
               .<FinishSetting>group("Look")
               .keywords(new String[]{"skin", "material", "finish", "steel", "damascus", "gold", "nebula", "prism", "obsidian", "painted", "blackened"}),
            owner
         );
         BooleanSupplier custom = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return f.get() == WeaponOptions.Finish.CUSTOM;
            }
         };
         this.customBlade = this.add(
            new ModeSetting<>(
                  this.id("customBlade"), "Blade Material", "Custom finish: what the blade is.", WeaponOptions.BladeMaterial.class, bladeFor(def.finish)
               )
               .style(ModeSetting.Style.CHIPS)
               .<ModeSetting>group("Look")
               .visibleWhen(custom),
            owner
         );
         this.paint = this.add(
            new ColorSetting(
                  this.id("paint"),
                  "Paint Color",
                  "The Painted finish's color (a gradient runs from the guard to the tip).",
                  ColorSetting.Mode.STATIC,
                  -45096,
                  -9806593,
                  1.0
               )
               .<ColorSetting>group("Look")
               .visibleWhen(
                  new BooleanSupplier() {
                     @Override
                     public boolean getAsBoolean() {
                        return f.get() == WeaponOptions.Finish.PAINTED
                           || f.get() == WeaponOptions.Finish.CUSTOM && WeaponSettings.this.customBlade.get() == WeaponOptions.BladeMaterial.PAINTED;
                     }
                  }
               ),
            owner
         );
         this.handleMaterial = this.add(
            new ModeSetting<>(
                  this.id("handleMaterial"),
                  "Handle",
                  "What the handle is made of.",
                  WeaponOptions.HandleMaterial.class,
                  WeaponOptions.HandleMaterial.of(def.handleMaterial)
               )
               .style(ModeSetting.Style.CHIPS)
               .<ModeSetting>group("Look")
               .keywords(new String[]{"grip", "wrap", "leather", "wood", "rubber", "cord"}),
            owner
         );
         this.handleColor = this.add(
            new ColorSetting(this.id("handleColor"), "Handle Color", "The handle's color.", ColorSetting.Mode.STATIC, def.handleColor, def.handleColor, 1.0)
               .skipSetAll()
               .<ColorSetting>group("Look")
               .keywords(new String[]{"grip color"}),
            owner
         );
         this.accent = this.add(
            new ColorSetting(
                  this.id("accent"),
                  "Accent / Energy Color",
                  "Glowing parts (energy, runes, crystals) and accents.",
                  def.accentStatic ? ColorSetting.Mode.STATIC : ColorSetting.Mode.GLOBAL,
                  def.accentColor,
                  def.accentColor,
                  1.0
               )
               .<ColorSetting>group("Look")
               .keywords(new String[]{"glow color", "energy", "accent"}),
            owner
         );
         this.glow = this.add(
            new SliderSetting(this.id("glow"), "Glow Strength", "How brightly glowing parts glow.", 0.0, 2.0, 1.0, 0.05)
               .unit("x")
               .<SliderSetting>group("Look")
               .keywords(new String[]{"bloom", "emissive", "brightness"}),
            owner
         );
         this.wear = this.add(
            new SliderSetting(this.id("wear"), "Wear", "Scratches, polished edges and dirt in the grooves.", 0.0, 100.0, 10.0, 5.0)
               .unit("%")
               .<SliderSetting>group("Look")
               .keywords(new String[]{"scratches", "battle worn", "dirt"}),
            owner
         );
         if (def.shimmer != null) {
            this.heatShimmer = this.add(
               new BooleanSetting(
                     this.id("heatShimmer"),
                     "Heat Shimmer",
                     "HD only: the air above the hot edge wavers (not with a shader pack; Pixelated flickers its edge instead).",
                     true
                  )
                  .<BooleanSetting>group("Look")
                  .<BooleanSetting>keywords(new String[]{"distortion", "haze", "heat", "refraction"})
                  .visibleWhen(new BooleanSupplier() {
                     @Override
                     public boolean getAsBoolean() {
                        return !module.pixelOf(def, WeaponOptions.StyleChoice.SECTION);
                     }
                  }),
               owner
            );
         }

         this.sizeFirst = this.add(
            new SliderSetting(this.id("sizeFirst"), "Size (First Person)", "The weapon's size in your hand.", 0.6, 1.4, 1.0, 0.05)
               .unit("x")
               .<SliderSetting>group("Size")
               .keywords(new String[]{"scale"}),
            owner
         );
         this.sizeThird = this.add(
            new SliderSetting(this.id("sizeThird"), "Size (Third Person)", "Its size in F5 and in other players' hands.", 0.6, 1.4, 1.0, 0.05)
               .unit("x")
               .<SliderSetting>group("Size")
               .keywords(new String[]{"scale"}),
            owner
         );
      }

      editorRow[0] = this.add(new PageRow(this.id("editor"), "View Model Editor", "Move, turn and size it on your real screen.", new Supplier<Widget>() {
         public Widget get() {
            return new ViewModelEditorWidget(module, editorRow[0], def);
         }
      }).<PageRow>group("View Model").keywords(new String[]{"view model editor", "viewmodel", "split view"}), owner);
      this.placement = this.add(
         new ModeSetting<>(
               this.id("placement"), "Placement", "Where " + name + " sits in first person.", WeaponOptions.Placement.class, WeaponOptions.Placement.DEFAULT
            )
            .<ModeSetting>group("View Model")
            .keywords(new String[]{"view model editor", "position", "viewmodel"}),
         owner
      );
      BooleanSupplier customPlacement = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return WeaponSettings.this.placement.get() == WeaponOptions.Placement.CUSTOM;
         }
      };
      this.offsetX = this.add(this.slider("offsetX", "X Offset", "Right (+) or left (-).", -0.3, 0.3, 0.0, 0.01, "", "View Model", customPlacement), owner);
      this.offsetY = this.add(this.slider("offsetY", "Y Offset", "Up (+) or down (-).", -0.3, 0.3, 0.0, 0.01, "", "View Model", customPlacement), owner);
      this.offsetZ = this.add(this.slider("offsetZ", "Z Offset", "Closer (+) or further (-).", -0.3, 0.3, 0.0, 0.01, "", "View Model", customPlacement), owner);
      this.pitch = this.add(
         this.slider("pitch", "Pitch", "Tilts the tip up (+) or down (-).", -45.0, 45.0, 0.0, 1.0, "°", "View Model", customPlacement), owner
      );
      this.yaw = this.add(this.slider("yaw", "Yaw", "Turns it left (+) or right (-).", -45.0, 45.0, 0.0, 1.0, "°", "View Model", customPlacement), owner);
      this.roll = this.add(this.slider("roll", "Roll", "Rolls it about its length.", -45.0, 45.0, 0.0, 1.0, "°", "View Model", customPlacement), owner);
      this.size = this.add(this.slider("size", "Size", "The whole view model's size.", 0.5, 1.5, 1.0, 0.05, "x", "View Model", customPlacement), owner);
      this.fov = this.add(
         new SliderSetting(
               this.id("fov"),
               "View Model FOV",
               "Its own field of view, apart from the game's (sprinting and potions never stretch it).",
               50.0,
               90.0,
               70.0,
               1.0
            )
            .unit("°")
            .<SliderSetting>group("View Model")
            .keywords(new String[]{"field of view", "fov"}),
         owner
      );
      this.leftHanded = this.add(
         new BooleanSetting(this.id("leftHanded"), "Left-Handed", "Mirrors everything to the left.", false)
            .<BooleanSetting>group("View Model")
            .keywords(new String[]{"left hand", "mirror"}),
         owner
      );
      BooleanSupplier motionOn;
      if (vanilla) {
         this.weaponMotion = this.add(
            new BooleanSetting(this.id("weaponMotion"), "Weapon Motion", "Breathing, bob, sway, the sprint pose and the rest on top of vanilla's hold.", false)
               .<BooleanSetting>group("Motion")
               .keywords(new String[]{"sway", "bob", "breathing", "motion"}),
            owner
         );
         motionOn = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return WeaponSettings.this.weaponMotion.get();
            }
         };
      } else {
         motionOn = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return true;
            }
         };
      }

      this.breathing = this.add(this.slider("breathing", "Idle Breathing", "A slow drift and tilt.", 0.0, 2.0, 1.0, 0.05, "x", "Motion", motionOn), owner);
      this.walkBob = this.add(
         this.slider("walkBob", "Walk Bob", "A figure-eight in step with your walking.", 0.0, 2.0, 1.0, 0.05, "x", "Motion", motionOn), owner
      );
      this.lookSway = this.add(
         this.slider("lookSway", "Look Sway", "Lags behind when you turn, then settles.", 0.0, 2.0, 0.6, 0.05, "x", "Motion", motionOn), owner
      );
      this.strafeTilt = this.add(
         this.slider("strafeTilt", "Strafe Tilt", "Rolls toward the way you strafe.", 0.0, 2.0, 1.0, 0.05, "x", "Motion", motionOn), owner
      );
      this.jumpLand = this.add(
         this.slider("jumpLand", "Jump & Land", "Dips when you jump, bounces when you land.", 0.0, 2.0, 1.0, 0.05, "x", "Motion", motionOn), owner
      );
      this.crouch = this.add(this.slider("crouch", "Crouch", "Lower and closer while sneaking.", 0.0, 2.0, 1.0, 0.05, "x", "Motion", motionOn), owner);
      this.sprintPose = this.add(
         this.slider("sprintPose", "Sprint Pose", "Tilts down and to the side while sprinting.", 0.0, 2.0, 1.0, 0.05, "x", "Motion", motionOn), owner
      );
      this.hurt = this.add(this.slider("hurt", "Hurt Jolt", "A small jolt when you take damage.", 0.0, 2.0, 1.0, 0.05, "x", "Motion", motionOn), owner);
      this.hitReaction = this.add(
         new BooleanSetting(this.id("hitReaction"), "Hit Reaction", "A small jolt back when your hit connects.", true)
            .<BooleanSetting>group("Motion")
            .visibleWhen(motionOn),
         owner
      );
      this.fidgets = this.add(
         new BooleanSetting(this.id("fidgets"), "Idle Fidgets", "After a while standing still, a small grip adjustment now and then.", true)
            .<BooleanSetting>group("Motion")
            .visibleWhen(motionOn),
         owner
      );
      if (def.engine) {
         this.vibration = this.add(
            new BooleanSetting(
                  this.id("vibration"), "Engine Vibration", "The saw shivers with its running engine: softly at idle, hard at full throttle.", true
               )
               .<BooleanSetting>group("Motion")
               .<BooleanSetting>keywords(new String[]{"shake", "rumble", "engine", "vibrate"})
               .foundAnywhere(),
            owner
         );
      }

      if (!vanilla) {
         this.swingPattern = this.add(
            new ModeSetting<>(
                  this.id("swingPattern"),
                  "Swing Pattern",
                  "Which swing plays when you attack.",
                  WeaponOptions.SwingPattern.class,
                  WeaponOptions.SwingPattern.ALTERNATE
               )
               .<ModeSetting>group("Swings")
               .keywords(new String[]{"attack", "swing", "directional"}),
            owner
         );
         this.trail = this.add(
            new ModeSetting<>(this.id("trail"), "Swing Trail", "A ribbon from the tip while swinging.", WeaponOptions.Trail.class, WeaponOptions.Trail.DEFAULT)
               .<ModeSetting>group("Swings")
               .keywords(new String[]{"streak", "ribbon"}),
            owner
         );
         this.trailColor = this.add(
            new ColorSetting(this.id("trailColor"), "Trail Color", "The swing trail's color.", def.accentColor)
               .<ColorSetting>group("Swings")
               .visibleWhen(new BooleanSupplier() {
                  @Override
                  public boolean getAsBoolean() {
                     return WeaponSettings.this.trail.get() != WeaponOptions.Trail.OFF;
                  }
               }),
            owner
         );
      }

      this.inspects = this.add(
         new InspectWeightsSetting(this.id("inspects"), "Inspects", "Which inspects play, and how often.", def.inspects)
            .<InspectWeightsSetting>group("Inspects")
            .keywords(new String[]{"inspect", "animation", "look over", "flip"}),
         owner
      );
      this.inspects.module = module;
      this.inspects.def = def;
      this.inspectOrder = this.add(
         new ModeSetting<>(this.id("inspectOrder"), "Order", "Which inspect comes next.", WeaponOptions.InspectOrder.class, WeaponOptions.InspectOrder.RANDOM)
            .group("Inspects"),
         owner
      );
      this.leftWeapon = this.add(
         new WeaponChoiceSetting(this.id("leftWeapon"), "Left-Hand Weapon", "Dual Wield: a mirrored copy, or any other one-handed weapon.", "same")
            .<WeaponChoiceSetting>group("Dual Wield")
            .keywords(new String[]{"dual wield", "off hand", "offhand"}),
         owner
      );
      this.leftWeapon.module = module;
      this.leftWeapon.def = def;
      if (def.engine) {
         this.sound = this.add(
            new BooleanSetting(this.id("sound"), "Engine Sound", "The engine: the only weapon that makes a sound.", true)
               .<BooleanSetting>group("Sound")
               .<BooleanSetting>keywords(new String[]{"audio", "engine", "noise", "sounds", "chainsaw"})
               .<BooleanSetting>foundAnywhere()
               .preview(new Runnable() {
                  @Override
                  public void run() {
                     module.chainsawSound.preview(WeaponSettings.this);
                  }
               }),
            owner
         );
         BooleanSupplier soundOn = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return WeaponSettings.this.sound.get();
            }
         };
         this.soundVolume = this.add(
            this.slider("soundVolume", "Volume", "How loud it is.", 0.0, 100.0, 60.0, 5.0, "%", "Sound", soundOn)
               .<SliderSetting>keywords(new String[]{"chainsaw sound", "loudness"})
               .foundAnywhere(),
            owner
         );
         this.soundIdle = this.add(
            new BooleanSetting(this.id("soundIdle"), "Engine Idle", "A low putter while you hold it.", true)
               .<BooleanSetting>group("Sound")
               .<BooleanSetting>visibleWhen(soundOn)
               .<BooleanSetting>keywords(new String[]{"chainsaw sound"})
               .foundAnywhere(),
            owner
         );
         this.soundBite = this.add(
            new BooleanSetting(this.id("soundBite"), "Bite", "A short grinding bite when your hit connects.", true)
               .<BooleanSetting>group("Sound")
               .<BooleanSetting>visibleWhen(soundOn)
               .<BooleanSetting>keywords(new String[]{"chainsaw sound", "hit sound"})
               .foundAnywhere(),
            owner
         );
         this.soundOthers = this.add(
            new BooleanSetting(
                  this.id("soundOthers"),
                  "Other Players' Chainsaws",
                  "Hear other players' chainsaws (needs Other Players on): up to 3, the closest first.",
                  false
               )
               .<BooleanSetting>group("Sound")
               .<BooleanSetting>visibleWhen(soundOn)
               .<BooleanSetting>keywords(new String[]{"chainsaw sound", "multiplayer"})
               .foundAnywhere(),
            owner
         );
      }

      for (Setting s : this.all) {
         this.defaults.put(s, s.toJson());
         s.addListener(new Runnable() {
            @Override
            public void run() {
               WeaponSettings.this.modifiedKnown = false;
               WeaponSettings.this.version++;
            }
         });
      }
   }

   int version() {
      return this.version;
   }

   private static WeaponOptions.BladeMaterial bladeFor(String finish) {
      WeaponOptions.Finish f = WeaponOptions.Finish.of(finish);

      for (WeaponOptions.BladeMaterial b : WeaponOptions.BladeMaterial.values()) {
         if (b.finish == f) {
            return b;
         }
      }

      return WeaponOptions.BladeMaterial.STEEL_POLISHED;
   }

   private String id(String name) {
      return this.prefix + "." + name;
   }

   private SliderSetting slider(
      String name, String label, String description, double min, double max, double def, double step, String unit, String group, BooleanSupplier visible
   ) {
      SliderSetting s = new SliderSetting(this.id(name), label, description, min, max, def, step);
      if (!unit.isEmpty()) {
         s.unit(unit);
      }

      return s.<SliderSetting>group(group).visibleWhen(visible);
   }

   private <S extends Setting> S add(S setting, SettingStyle owner) {
      setting.ownedBy(owner);
      this.all.add(setting);
      return setting;
   }

   boolean isModified() {
      if (!this.modifiedKnown) {
         this.modified = false;

         for (Setting s : this.all) {
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

   List<Setting> placementSettings() {
      ArrayList<Setting> out = new ArrayList<>();
      out.add(this.placement);
      out.add(this.offsetX);
      out.add(this.offsetY);
      out.add(this.offsetZ);
      out.add(this.pitch);
      out.add(this.yaw);
      out.add(this.roll);
      out.add(this.size);
      out.add(this.fov);
      out.add(this.leftHanded);
      return out;
   }

   void copyPlacementFrom(WeaponSettings other) {
      List<Setting> mine = this.placementSettings();
      List<Setting> theirs = other.placementSettings();

      for (int i = 0; i < mine.size(); i++) {
         mine.get(i).fromJson(theirs.get(i).toJson());
      }
   }

   void resetPlacement() {
      for (Setting s : this.placementSettings()) {
         s.reset();
      }
   }

   WeaponOptions.Finish effectiveFinish() {
      if (this.finish == null) {
         return WeaponOptions.Finish.STEEL_POLISHED;
      } else {
         WeaponOptions.Finish f = this.finish.get();
         return f == WeaponOptions.Finish.CUSTOM ? this.customBlade.get().finish : f;
      }
   }

   static String prefixOf(WeaponDef def) {
      return def.isImported() ? "imported" : def.id.toLowerCase(Locale.ROOT);
   }
}
