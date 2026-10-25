package meowtils.furiahvisuals.module.sky;

import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.ModeSetting;

public final class SkyStyleSetting extends ModeSetting<SkyStyle> implements HasWidget {
   private final SkyModule module;

   SkyStyleSetting(SkyModule module) {
      super("style", "Sky", "The sky you see: click a card (it crossfades in).", SkyStyle.class, SkyStyle.MOLTEN_MARBLE);
      this.module = module;
      this.style(ModeSetting.Style.CARDS);
   }

   @Override
   public Widget createWidget() {
      return new SkyCardsWidget(this.module, this);
   }
}
