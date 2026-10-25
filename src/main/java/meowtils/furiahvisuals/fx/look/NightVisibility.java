package meowtils.furiahvisuals.fx.look;

import meowtils.furiahvisuals.setting.Displayable;

public enum NightVisibility implements Displayable {
   AUTO("Auto", "Only where it's dark"),
   ON("On", "Always"),
   OFF("Off", "Never");

   private final String displayName;
   private final String description;

   private NightVisibility(String displayName, String description) {
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
