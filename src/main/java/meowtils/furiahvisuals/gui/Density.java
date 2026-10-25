package meowtils.furiahvisuals.gui;

import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;

public final class Density {
   private static ModeSetting<Density.Mode> setting;

   private Density() {
   }

   public static void attach(ModeSetting<Density.Mode> s) {
      setting = s;
   }

   public static Density.Mode get() {
      return setting == null ? Density.Mode.COMPACT : setting.get();
   }

   public static boolean compact() {
      return get() != Density.Mode.COMFORTABLE;
   }

   public static boolean dense() {
      return get() == Density.Mode.DENSE;
   }

   public static float text() {
      switch (get()) {
         case COMPACT:
            return 8.8F;
         case DENSE:
            return 8.0F;
         default:
            return 10.0F;
      }
   }

   public static float small() {
      switch (get()) {
         case COMPACT:
            return 7.8F;
         case DENSE:
            return 7.2F;
         default:
            return 8.5F;
      }
   }

   public static float row() {
      switch (get()) {
         case COMPACT:
            return 16.0F;
         case DENSE:
            return 14.0F;
         default:
            return 26.0F;
      }
   }

   public static float control() {
      switch (get()) {
         case COMPACT:
            return 12.5F;
         case DENSE:
            return 11.5F;
         default:
            return 20.0F;
      }
   }

   public static float header() {
      switch (get()) {
         case COMPACT:
            return 15.0F;
         case DENSE:
            return 13.5F;
         default:
            return 26.0F;
      }
   }

   public static float headerText() {
      switch (get()) {
         case COMPACT:
            return 7.3F;
         case DENSE:
            return 6.8F;
         default:
            return 8.2F;
      }
   }

   public static float gap() {
      switch (get()) {
         case COMPACT:
            return 1.0F;
         case DENSE:
            return 1.0F;
         default:
            return 0.0F;
      }
   }

   public static float cardScale() {
      switch (get()) {
         case COMPACT:
            return 0.6F;
         case DENSE:
            return 0.46F;
         default:
            return 1.0F;
      }
   }

   public static float textCardH(float comfortable) {
      switch (get()) {
         case COMPACT:
            return Math.round(comfortable * 0.72F);
         case DENSE:
            return 17.0F;
         default:
            return comfortable;
      }
   }

   public static int togglesPerRow(float width) {
      switch (get()) {
         case COMPACT:
            return width >= 230.0F ? 2 : 1;
         case DENSE:
            return width >= 420.0F ? 3 : (width >= 210.0F ? 2 : 1);
         default:
            return 1;
      }
   }

   public static enum Mode implements Displayable {
      COMFORTABLE("Comfortable", "Roomy rows with the full controls: the original look"),
      COMPACT("Compact", "Thin rows, two toggles to a row: 15 to 20 settings on one screen"),
      DENSE("Dense", "Tighter still, three toggles to a row on a wide panel");

      private final String displayName;
      private final String description;

      private Mode(String displayName, String description) {
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
}
