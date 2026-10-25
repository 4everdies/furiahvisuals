package meowtils.furiahvisuals.module.sky;

import java.util.Locale;
import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.Displayable;

public enum SkyStyle implements Displayable {
   CIRCUIT_MAZE("Circuit Maze", "A maze of tiny glowing circuits", -54314, -14293, SkyStyle.Kind.CRISP, 0.0F, 30.0F),
   KALEIDOSCOPE("Kaleidoscope", "Neon stars blooming out of the sky", -4174849, -42279, SkyStyle.Kind.CRISP, 0.0F, 62.0F),
   THE_WATCHER("The Watcher", "A giant halftone eye that watches you", -15461356, -856348, SkyStyle.Kind.CRISP, 0.0F, 26.0F),
   MEGASTRUCTURE("Megastructure", "Drifting through a vast lit structure", -49736, -1, SkyStyle.Kind.CRISP, 90.0F, 25.0F),
   CODE_RAIN("Code Rain", "Glowing glyphs raining down", -12714130, -3604520, SkyStyle.Kind.CRISP, 0.0F, 22.0F),
   PLASMA_VEINS("Plasma Veins", "A living web of plasma", -13703425, -8758017, SkyStyle.Kind.SOFT, 0.0F, 35.0F),
   NEON_FRACTURE("Neon Fracture", "Lightning cracks webbing the sky", -45128, -1, SkyStyle.Kind.CRISP, 0.0F, 16.0F),
   VIOLET_STORM("Violet Storm", "Bright clouds churning like liquid", -2867969, -1, SkyStyle.Kind.SOFT, 90.0F, 22.0F),
   SKY_BEAMS("Sky Beams", "Searchlights sweeping up from the horizon", -5878273, -38187, SkyStyle.Kind.SOFT, 0.0F, 20.0F),
   AMETHYST_CELLS("Amethyst Cells", "Glowing cell walls, slowly shifting", -6464257, -2047233, SkyStyle.Kind.SOFT, 0.0F, 35.0F),
   VOID_RIFT("Void Rift", "A black rift in boiling violet energy", -10805295, -2080517, SkyStyle.Kind.CRISP, 0.0F, 40.0F),
   FROZEN_TIDE("Frozen Tide", "A wave of frost frozen mid-crash", -8402689, -853249, SkyStyle.Kind.CRISP, 0.0F, 32.0F),
   FIREFALL("Firefall", "Fire raining from a smoldering sky", -5038066, -34278, SkyStyle.Kind.CRISP, 0.0F, 18.0F),
   GILDED_STORM("Gilded Storm", "Golden light breaking through a storm", -3566037, -3656, SkyStyle.Kind.CRISP, 0.0F, 16.0F),
   MOLTEN_MARBLE("Molten Marble", "Red liquid marble flowing in waves", -60396, -7733248),
   CHROME_INK("Chrome Ink", "Silver liquid curling through black", -854276, -10720382),
   CRIMSON_VEINS("Crimson Veins", "Glowing threads of red liquid", -56284, -10747896),
   NEON_LOTUS("Neon Lotus", "A bloom of neon light on the horizon", -4043777, -12755969),
   SILK("Silk", "Glossy silk folding into the horizon", -6140427, -8787894),
   ACRYLIC_POUR("Acrylic Pour", "Pearl cells and pink lace on black", -2079362, -792084),
   LIQUID_VORTEX("Liquid Vortex", "Glossy bands twisting overhead", -44316, -9148673),
   AURORA_NIGHT("Aurora Night", "Northern lights over a starry night", -13041764, -5222913),
   NEBULA("Nebula", "Glowing gas clouds of deep space", -2999156, -13987888),
   BLACK_HOLE("Black Hole", "A black hole bending the light", -26052, -5699),
   SYNTHWAVE("Synthwave", "A neon sunset over a racing grid", -50004, -14494721),
   GOLDEN_HOUR("Golden Hour", "A sunset that never ends", -30147, -2072433),
   ANIME_SKY("Anime Sky", "Vivid blue with cel-shaded clouds", -12743681, -5916438),
   STORM_FRONT("Storm Front", "Churning clouds and lightning", -12298118, -3483393),
   BLOOD_MOON("Blood Moon", "A giant red moon in crimson haze", -5238758, -46546),
   ECLIPSE("Eclipse", "A black sun with a blazing corona", -6472, -14998718),
   SEA_OF_CLOUDS("Sea of Clouds", "High above an ocean of clouds", -11563521, -11352),
   CHERRY_BLOSSOM("Cherry Blossom", "Pink sky with drifting petals", -19503, -3950849),
   SWIRLING_NIGHT("Swirling Night", "A painted night of swirling strokes", -14729319, -864434),
   CHROMA_SKY("Chroma Sky", "Colors flowing through the rainbow", -42406, -10813480),
   VOID("Void", "Almost black, nothing to distract", -7562300, -13752768),
   CUSTOM_IMAGE("Custom Image", "Your own panorama: sky.png", -1, -3616026);

   private final String displayName;
   private final String description;
   public final int colorA;
   public final int colorB;
   final SkyStyle.Kind kind;
   final float previewYaw;
   final float previewPitch;

   private SkyStyle(String displayName, String description, int colorA, int colorB) {
      this(displayName, description, colorA, colorB, SkyStyle.Kind.CLASSIC, 0.0F, 20.0F);
   }

   private SkyStyle(String displayName, String description, int colorA, int colorB, SkyStyle.Kind kind, float previewYaw, float previewPitch) {
      this.displayName = displayName;
      this.description = description;
      this.colorA = colorA;
      this.colorB = colorB;
      this.kind = kind;
      this.previewYaw = previewYaw;
      this.previewPitch = previewPitch;
   }

   @Override
   public String displayName() {
      return this.displayName;
   }

   @Override
   public String description() {
      return this.description;
   }

   @Override
   public String tag() {
      return this.isDynamic() && !MenuMemory.seen(this.seenKey()) ? "NEW" : null;
   }

   public String seenKey() {
      return "sky." + this.shaderName();
   }

   @Override
   public String[] keywords() {
      switch (this) {
         case VOID_RIFT:
            return new String[]{"void", "rift", "purple", "violet", "magenta", "portal", "energy", "black", "space"};
         case FROZEN_TIDE:
            return new String[]{"frost", "ice", "frozen", "wave", "tide", "winter", "cold", "snow", "spires", "blue"};
         case FIREFALL:
            return new String[]{"fire", "swords", "blades", "embers", "flames", "burning", "red", "smoke"};
         case GILDED_STORM:
            return new String[]{"gold", "golden", "holy", "rays", "light", "sunbeams", "sigil", "star", "divine", "clouds"};
         default:
            return null;
      }
   }

   public boolean isDynamic() {
      return this.kind != SkyStyle.Kind.CLASSIC;
   }

   public String shaderName() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public int horizonColor(int a, int b, boolean custom) {
      switch (this) {
         case VOID_RIFT:
            return ColorUtil.lerp(-16252145, a, 0.2F);
         case FROZEN_TIDE:
            return ColorUtil.lerp(-15064791, a, 0.3F);
         case FIREFALL:
            return ColorUtil.lerp(-15727870, a, 0.25F);
         case GILDED_STORM:
            return ColorUtil.lerp(-15068407, a, 0.35F);
         case MOLTEN_MARBLE:
            return ColorUtil.lerp(-16777216, a, 0.3F);
         case CHROME_INK:
            return ColorUtil.lerp(-16777216, b, 0.15F);
         case CRIMSON_VEINS:
            return ColorUtil.lerp(-16252928, a, 0.12F);
         case NEON_LOTUS:
            return ColorUtil.lerp(-16448756, ColorUtil.lerp(a, b, 0.5F), 0.12F);
         case SILK:
            return ColorUtil.lerp(-16443105, ColorUtil.lerp(a, b, 0.5F), 0.35F);
         case ACRYLIC_POUR:
            return ColorUtil.lerp(-16580350, a, 0.08F);
         case LIQUID_VORTEX:
            return ColorUtil.lerp(-16580348, ColorUtil.lerp(a, b, 0.5F), 0.15F);
         case AURORA_NIGHT:
            return ColorUtil.lerp(-16445412, a, 0.06F);
         case NEBULA:
            return ColorUtil.lerp(-16645627, ColorUtil.lerp(a, b, 0.5F), 0.12F);
         case BLACK_HOLE:
            return -16580092;
         case SYNTHWAVE:
            return ColorUtil.shade(ColorUtil.lerp(-32688, a, 0.5F), 0.7F);
         case GOLDEN_HOUR:
            return ColorUtil.lerp(-24999, a, 0.4F);
         case ANIME_SKY:
            return -6236929;
         case STORM_FRONT:
            return ColorUtil.lerp(-16447734, a, 0.45F);
         case BLOOD_MOON:
            return ColorUtil.lerp(-16777216, a, 0.5F);
         case ECLIPSE:
            return -11717825;
         case SEA_OF_CLOUDS:
            return -4006923;
         case CHERRY_BLOSSOM:
            return -10552;
         case SWIRLING_NIGHT:
            return ColorUtil.lerp(-14596801, a, 0.4F);
         case CHROMA_SKY:
            return custom ? ColorUtil.shade(ColorUtil.lerp(a, b, 0.5F), 0.7F) : -7701878;
         case VOID:
            return -16250868;
         case CIRCUIT_MAZE:
            return ColorUtil.lerp(-16514554, a, 0.1F);
         case KALEIDOSCOPE:
            return ColorUtil.lerp(-16383216, ColorUtil.lerp(a, b, 0.5F), 0.16F);
         case THE_WATCHER:
            return ColorUtil.shade(b | 0xFF000000, 0.86F);
         case MEGASTRUCTURE:
            return ColorUtil.lerp(-16317172, a, 0.14F);
         case CODE_RAIN:
            return ColorUtil.lerp(-16710654, a, 0.1F);
         case PLASMA_VEINS:
            return ColorUtil.lerp(-16710648, a, 0.1F);
         case NEON_FRACTURE:
            return ColorUtil.lerp(-16252144, a, 0.22F);
         case VIOLET_STORM:
            return ColorUtil.lerp(-15071706, a, 0.45F);
         case SKY_BEAMS:
            return ColorUtil.lerp(-16055018, a, 0.3F);
         case AMETHYST_CELLS:
            return ColorUtil.lerp(-15923431, a, 0.25F);
         default:
            return -13420476;
      }
   }

   static enum Kind {
      CLASSIC,
      CRISP,
      SOFT;
   }
}
