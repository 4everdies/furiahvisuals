package meowtils.furiahvisuals.module.watermark;

import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.TextSetting;

final class ItemLabel {
   static final int MAX_LENGTH = 16;
   final BooleanSetting show;
   final TextSetting text;
   final ModeSetting<ItemLabel.Place> place;
   private String shownText = "";
   private String measuredText;
   private ItemLabel.Place measuredPlace;
   private int measuredPx;
   private float width;

   ItemLabel(IslandItem item, String showId, String prefix, String defaultText, boolean on, ItemLabel.Place defaultPlace) {
      this.show = item.option(new BooleanSetting(showId, "Show Label", "Writes \"" + defaultText + "\" (or your own text) next to the value.", on));
      BooleanSupplier shown = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ItemLabel.this.show.get();
         }
      };
      this.text = item.option(
         new TextSetting(prefix + ".labelText", "Label", "What the label says (at most 16 characters).", defaultText, 16).visibleWhen(shown)
      );
      this.place = item.option(
         new ModeSetting<>(prefix + ".labelPlace", "Label Side", "Before or after the value.", ItemLabel.Place.class, defaultPlace).visibleWhen(shown)
      );
   }

   boolean visible() {
      return this.show.get() && !this.text.get().trim().isEmpty();
   }

   boolean before() {
      return this.visible() && this.place.get() == ItemLabel.Place.BEFORE;
   }

   boolean after() {
      return this.visible() && this.place.get() == ItemLabel.Place.AFTER;
   }

   float width(IslandDraw d, IslandStyle s) {
      if (!this.visible()) {
         return 0.0F;
      } else {
         String t = this.text.get();
         ItemLabel.Place p = this.place.get();
         int px = d.pixels(s.small);
         if (px != this.measuredPx || !t.equals(this.measuredText) || p != this.measuredPlace) {
            this.measuredPx = px;
            this.measuredText = t;
            this.measuredPlace = p;
            this.shownText = p == ItemLabel.Place.BEFORE ? t.trim() + " " : " " + t.trim();
            this.width = d.width(false, s.small, this.shownText);
         }

         return this.width;
      }
   }

   float draw(IslandDraw d, IslandStyle s, float x, float cy) {
      float w = this.width(d, s);
      if (w <= 0.0F) {
         return x;
      } else {
         float y = this.measuredPlace == ItemLabel.Place.BEFORE
            ? d.middleTop(false, s.small, cy)
            : d.sameBaseline(false, s.font, d.middleTop(false, s.font, cy), s.small);
         d.text(false, s.small, this.shownText, x, y, s.dim);
         return x + w;
      }
   }

   static enum Place implements Displayable {
      BEFORE("Before", "Before the value: \"fps 144\""),
      AFTER("After", "After the value: \"144 fps\"");

      private final String displayName;
      private final String description;

      private Place(String displayName, String description) {
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
