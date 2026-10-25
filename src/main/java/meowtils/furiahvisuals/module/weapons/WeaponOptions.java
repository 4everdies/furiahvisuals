package meowtils.furiahvisuals.module.weapons;

import java.util.Locale;
import meowtils.furiahvisuals.setting.Displayable;

public final class WeaponOptions {
   private WeaponOptions() {
   }

   public static enum AntiAliasing implements Displayable {
      OFF("Off", "Drawn straight onto the screen"),
      X2("2x", "Drawn at twice the resolution, then smoothed down"),
      X4("4x", "Four times the resolution: the smoothest, the most work");

      private final String name;
      private final String description;

      private AntiAliasing(String name, String description) {
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

      int factor() {
         return this == OFF ? 1 : (this == X2 ? 2 : 4);
      }
   }

   public static enum AutoInspect implements Displayable {
      OFF("Off", "Only with the key"),
      EVERY("Every", "At a fixed interval"),
      RANDOM("Random", "At random times between a minimum and a maximum");

      private final String name;
      private final String description;

      private AutoInspect(String name, String description) {
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

   public static enum BladeMaterial implements Displayable {
      STEEL_POLISHED(WeaponOptions.Finish.STEEL_POLISHED),
      STEEL_BRUSHED(WeaponOptions.Finish.STEEL_BRUSHED),
      DAMASCUS(WeaponOptions.Finish.DAMASCUS),
      BLACKENED(WeaponOptions.Finish.BLACKENED),
      GOLD(WeaponOptions.Finish.GOLD),
      NEBULA(WeaponOptions.Finish.NEBULA),
      PRISM(WeaponOptions.Finish.PRISM),
      OBSIDIAN(WeaponOptions.Finish.OBSIDIAN),
      PAINTED(WeaponOptions.Finish.PAINTED),
      BONE(WeaponOptions.Finish.BONE),
      CRIMSON(WeaponOptions.Finish.CRIMSON),
      SAPPHIRE(WeaponOptions.Finish.SAPPHIRE);

      final WeaponOptions.Finish finish;

      private BladeMaterial(WeaponOptions.Finish finish) {
         this.finish = finish;
      }

      @Override
      public String displayName() {
         return this.finish.displayName;
      }

      @Override
      public String description() {
         return this.finish.description;
      }
   }

   public static enum Draw implements Displayable {
      FULL("Full", "Each weapon's own draw"),
      QUICK("Quick", "Twice as fast"),
      OFF("Off", "Straight into the hand");

      private final String name;
      private final String description;

      private Draw(String name, String description) {
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

   public static enum DualSwings implements Displayable {
      ALTERNATE("Alternate Hands", "Right, left, right…"),
      RIGHT_ONLY("Right Only", "Only the right hand swings"),
      BOTH("Both", "Both hands at once");

      private final String name;
      private final String description;

      private DualSwings(String name, String description) {
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

   public static enum EmptyHand implements Displayable {
      OFF("Off", "Never shown"),
      SPRINT_INSPECT("Sprinting & Inspecting", "Shows while you sprint and during inspects"),
      ALWAYS("Always", "Always in the lower-left corner");

      private final String name;
      private final String description;

      private EmptyHand(String name, String description) {
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

   public static enum Enchant implements Displayable {
      SHEEN("Sheen", "A smooth animated sheen sweeping over the model"),
      VANILLA("Vanilla-Style", "Minecraft's glint stripes, on the model"),
      OFF("Off", "No glint");

      private final String name;
      private final String description;

      private Enchant(String name, String description) {
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

   public static enum Finish implements Displayable {
      STEEL_POLISHED("Polished Steel", "Mirror-bright steel", 1, -3551789),
      STEEL_BRUSHED("Brushed Steel", "Satin steel with fine lines along the blade", 2, -6380630),
      DAMASCUS("Damascus", "Wavy layers of light and dark folded steel", 3, -9539467),
      BLACKENED("Blackened", "Matte black, with steel showing on the worn edges", 4, -14934752),
      GOLD("Gold", "Polished gold", 5, -1525682),
      NEBULA("Nebula", "Indigo to violet to magenta, with nebula clouds and a shifting sheen", 6, -6605600),
      PRISM("Prism", "A holographic rainbow that moves as it turns", 7, -10299672),
      OBSIDIAN("Obsidian", "Glossy black glass with a purple sheen", 8, -14018752),
      PAINTED("Painted", "Your color, under a satin coat", 9, -45096),
      VOID("Void", "Dark energy with a glowing edge and drifting motes", 10, -12971936),
      HARD_LIGHT("Hard Light", "A blade of light: a white-hot core in its energy color", 11, -1),
      ICE("Ice", "Translucent ice with inner cracks and facets", 12, -4200705),
      YEW("Yew", "Honey-brown wood with a fine grain, waxed (bows)", 13, -4158902),
      INFERNAL("Infernal", "Molten red-orange under a cracked crust, with slowly flowing heat lines", 14, -42470),
      BONE("Bone", "Ivory with fine cracks and darker grooves, a soft waxy sheen", 15, -1647420),
      CRIMSON("Crimson", "Deep red lacquered steel, darker toward the spine, bright red at the edges", 16, -5236702),
      SAPPHIRE("Sapphire", "Deep blue steel with a frosty pale-blue edge", 17, -14399320),
      DRAGONSCALE("Dragonscale", "Overlapping deep red scales with gold trim", 18, -6677988),
      MOONSILVER("Moonsilver", "Pale silver with fine star engravings and a soft glow in the accent color", 19, -3615514),
      LIVING_WOOD("Living Wood", "Dark bark with moss and small leaves, a faint glow of the accent in its cracks", 26, -11912670),
      CUSTOM("Custom", "Your own blade material, handle and colors", 0, -8355712);

      public final String displayName;
      public final String description;
      public final int code;
      public final int swatch;

      private Finish(String displayName, String description, int code, int swatch) {
         this.displayName = displayName;
         this.description = description;
         this.code = code;
         this.swatch = swatch;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }

      static WeaponOptions.Finish of(String s) {
         if (s == null) {
            return null;
         } else {
            for (WeaponOptions.Finish f : values()) {
               if (f.name().equalsIgnoreCase(s)) {
                  return f;
               }
            }

            return null;
         }
      }
   }

   public static enum GlowStyle implements Displayable {
      PIXEL_HALO("Pixel Halo", "A blocky halo one pixel wide round glowing pixels, at half strength"),
      SOFT("Soft", "The HD weapons' soft glow"),
      OFF("Off", "No glow round them");

      public final String displayName;
      public final String description;

      private GlowStyle(String displayName, String description) {
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

   public static enum HandleMaterial implements Displayable {
      LEATHER("Leather Wrap", "Overlapping leather bands", 1),
      WOOD("Wood", "Oiled wood with a fine grain", 2),
      RUBBER("Rubber Grip", "Grippy textured rubber", 3),
      CORD("Cord Wrap", "Two crossing cords", 4),
      METAL("Metal", "Knurled metal", 5);

      public final String displayName;
      public final String description;
      public final int code;

      private HandleMaterial(String displayName, String description, int code) {
         this.displayName = displayName;
         this.description = description;
         this.code = code;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }

      static WeaponOptions.HandleMaterial of(String s) {
         if (s != null) {
            String k = s.toLowerCase(Locale.ROOT);
            if (k.startsWith("leather")) {
               return LEATHER;
            }

            if (k.startsWith("wood")) {
               return WOOD;
            }

            if (k.startsWith("cord")) {
               return CORD;
            }

            if (k.startsWith("metal")) {
               return METAL;
            }
         }

         return RUBBER;
      }
   }

   public static enum InspectOrder implements Displayable {
      RANDOM("Random", "Any, never the same twice in a row"),
      IN_ORDER("In Order", "One after the other"),
      ALWAYS_SAME("Always the Same", "Always the first one checked");

      private final String name;
      private final String description;

      private InspectOrder(String name, String description) {
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

   public static enum KeyAgain implements Displayable {
      RESTART("Restart", "Starts it again"),
      NEXT("Next Inspect", "Goes on to another one"),
      CANCEL("Cancel", "Stops it");

      private final String name;
      private final String description;

      private KeyAgain(String name, String description) {
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

   public static enum PixelShading implements Displayable {
      VANILLA("Vanilla Item Lighting", "Lit like vanilla's held items: two fixed lights, so the faces turned away are darker"),
      FLAT("Flat", "Every face as bright as the light where you are: only the pixels' own shading");

      public final String displayName;
      public final String description;

      private PixelShading(String displayName, String description) {
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

   public static enum Placement implements Displayable {
      DEFAULT("Default", "Hand-tuned for each weapon"),
      MINIMAL("Minimal", "Smaller and lower"),
      CENTERED("Centered", "Closer to the middle of the screen"),
      CUSTOM("Custom", "Your own position, turn and size");

      private final String name;
      private final String description;

      private Placement(String name, String description) {
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

   public static enum StyleChoice implements Displayable {
      SECTION("Section Style", "Follows Weapon Style at the top of the section"),
      HD("HD", "Always its smooth, detailed model"),
      PIXELATED("Pixelated", "Always its 3D pixel art");

      public final String displayName;
      public final String description;

      private StyleChoice(String displayName, String description) {
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

      static WeaponOptions.StyleChoice of(String s) {
         if ("hd".equalsIgnoreCase(s)) {
            return HD;
         } else {
            return "pixelated".equalsIgnoreCase(s) ? PIXELATED : SECTION;
         }
      }

      String json() {
         return this == SECTION ? null : this.name().toLowerCase(Locale.ROOT);
      }
   }

   public static enum SwingPattern implements Displayable {
      ALTERNATE("Alternate", "Takes turns through the weapon's swings"),
      RANDOM("Random", "Any of the weapon's swings"),
      DIRECTIONAL("Directional", "Swings the way your mouse moves (like Hit Effect's slash)");

      private final String name;
      private final String description;

      private SwingPattern(String name, String description) {
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

   public static enum TextureSize implements Displayable {
      K1("1K", "1024 pixels: sharp, light on memory"),
      K2("2K", "2048 pixels: sharpest up close, four times the memory");

      private final String name;
      private final String description;

      private TextureSize(String name, String description) {
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

      int pixels() {
         return this == K1 ? 1024 : 2048;
      }
   }

   public static enum Trail implements Displayable {
      DEFAULT("Weapon Default", "Energy and magic weapons leave a trail, the others don't"),
      OFF("Off", "No trail"),
      SUBTLE("Subtle", "A faint ribbon from the tip"),
      STRONG("Strong", "A bright ribbon from the tip");

      private final String name;
      private final String description;

      private Trail(String name, String description) {
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

   public static enum WeaponStyle implements Displayable {
      HD("HD", "Smooth, detailed models"),
      PIXELATED("Pixelated", "3D pixel art that fits Minecraft");

      public final String displayName;
      public final String description;

      private WeaponStyle(String displayName, String description) {
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

   public static enum WheelIcons implements Displayable {
      LIVE("Live 3D Models", "Each weapon turns slowly in its finish; the one lit up turns faster and grows"),
      STATIC("Static Icons", "Drawn once, so they cost less");

      private final String name;
      private final String description;

      private WheelIcons(String name, String description) {
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

   public static enum WheelLabels implements Displayable {
      CENTER("Center", "The lit weapon's name, kind and finish in the middle, with a bigger live picture of it"),
      UNDER("Under Every Slot", "Each weapon's name under its picture"),
      NONE("None", "Only the pictures");

      private final String name;
      private final String description;

      private WheelLabels(String name, String description) {
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

   public static enum WheelMode implements Displayable {
      HOLD("Hold", "Hold the key to open it; let go to pick the weapon lit up"),
      TOGGLE("Toggle", "Press to open it; click (or press again) to pick");

      private final String name;
      private final String description;

      private WheelMode(String name, String description) {
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

   public static enum WheelTheme implements Displayable {
      MATCH("Match Mod Theme", "The menu's and the island's look: background, border, accent, glass and the glint"),
      CUSTOM("Custom", "Your own background, slot and highlight colors, border and glass"),
      MINIMAL("Minimal", "Thin rings and dividers, no fill");

      private final String name;
      private final String description;

      private WheelTheme(String name, String description) {
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
