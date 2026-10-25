package meowtils.furiahvisuals.sound;

import java.util.Locale;
import meowtils.furiahvisuals.setting.Displayable;

public enum SoundTheme implements Displayable {
   SOFT("Soft", "Warm, rounded plops and chimes"),
   GLASS("Glass", "Bright crystal pings and bells"),
   MINIMAL("Minimal", "Tiny dry clicks"),
   CREAMY("Creamy", "Deep, soft keystrokes, like creamy mechanical switches");

   private final String displayName;
   private final String description;

   private SoundTheme(String displayName, String description) {
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

   public String dir() {
      return this.name().toLowerCase(Locale.ROOT);
   }
}
