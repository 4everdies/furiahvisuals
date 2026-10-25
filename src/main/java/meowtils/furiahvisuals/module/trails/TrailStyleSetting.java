package meowtils.furiahvisuals.module.trails;

import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.ModeSetting;

final class TrailStyleSetting extends ModeSetting<TrailStyle> implements HasWidget {
   private final TrailsModule module;

   TrailStyleSetting(TrailsModule module) {
      super(
         "style",
         "Style",
         "The look of the trail: click a card for its settings (with Combine Styles, its switch runs it).",
         TrailStyle.class,
         TrailStyle.RIBBON
      );
      this.module = module;
      this.style(ModeSetting.Style.CARDS);
   }

   @Override
   public Widget createWidget() {
      return new TrailCardsWidget(this.module);
   }
}
