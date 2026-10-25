package meowtils.furiahvisuals.module.targetaura;

import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.ModeSetting;

final class AuraEffectSetting extends ModeSetting<EffectType> implements HasWidget {
   private final TargetAuraModule module;

   AuraEffectSetting(TargetAuraModule module) {
      super("effect", "Effect", "The look of the aura.", EffectType.class, EffectType.ORBIT_RINGS);
      this.module = module;
      this.style(ModeSetting.Style.CARDS);
   }

   @Override
   public Widget createWidget() {
      return new AuraCardsWidget(this.module);
   }
}
