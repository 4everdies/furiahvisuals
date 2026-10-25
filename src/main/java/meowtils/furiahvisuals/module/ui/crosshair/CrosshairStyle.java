package meowtils.furiahvisuals.module.ui.crosshair;

import meowtils.furiahvisuals.setting.Displayable;

public enum CrosshairStyle implements Displayable {
   VANILLA("Vanilla", "The resource pack's own crosshair, unchanged (hit markers and the charge ring can still show)"),
   CLASSIC("Classic", "Four lines with a gap and an optional dot, built line by line with the FPS builder"),
   DOT("Dot", "A center dot"),
   CIRCLE("Circle", "A thin ring, with an optional dot"),
   CIRCLE_CROSS("Circle + Cross", "A ring with four short lines crossing it"),
   BRACKETS("Brackets", "Four corner brackets framing the center; they snap in on a target"),
   CHEVRON("Chevron", "An upward chevron whose tip sits on the aim point"),
   TRIANGLE("Triangle", "Three short ticks at 120°, pointing inward"),
   TRI_DOT("Tri-dot", "Three small dots in a triangle around the center"),
   DIAMOND("Diamond", "A rotated square outline with a dot"),
   HEX("Hex", "A hexagon outline with a dot, slowly turning"),
   SHOTGUN("Shotgun", "A wide ring with four small ticks on it"),
   RED_DOT("Red Dot", "A bright, softly glowing dot inside a faint ring, like a red-dot sight"),
   SPINNER("Spinner", "A segmented ring that spins, faster on a target"),
   ORBIT("Orbit", "A dot with small dots orbiting it; the orbit tightens on a target"),
   PULSE("Pulse", "A dot with a ring that keeps expanding and fading, like a sonar ping"),
   RETICLE("Reticle", "Long thin lines with a small gap and a thin circle, like a scope");

   public static final String REMOVED_IMAGE = "IMAGE";
   private final String displayName;
   private final String description;

   private CrosshairStyle(String displayName, String description) {
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

   public boolean usesBasics() {
      return this != CLASSIC && this != VANILLA;
   }

   public boolean hasOwnDot() {
      return this == DOT || this == RED_DOT || this == ORBIT || this == PULSE || this == DIAMOND || this == HEX;
   }
}
