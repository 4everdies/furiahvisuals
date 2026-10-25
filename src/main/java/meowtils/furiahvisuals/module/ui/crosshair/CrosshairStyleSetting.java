package meowtils.furiahvisuals.module.ui.crosshair;

import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.ModeSetting;

final class CrosshairStyleSetting extends ModeSetting<CrosshairStyle> implements HasWidget {
   private final CrosshairElement element;

   CrosshairStyleSetting(CrosshairElement element) {
      super("crosshair.style", "Style", "What the crosshair looks like: click a card.", CrosshairStyle.class, CrosshairStyle.CLASSIC);
      this.element = element;
      this.style(ModeSetting.Style.CARDS);
   }

   @Override
   public Widget createWidget() {
      return new CrosshairCardsWidget(this.element, this);
   }
}
