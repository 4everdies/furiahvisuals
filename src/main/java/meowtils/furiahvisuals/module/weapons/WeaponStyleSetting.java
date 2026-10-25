package meowtils.furiahvisuals.module.weapons;

import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.ModeSetting;

public final class WeaponStyleSetting extends ModeSetting<WeaponOptions.WeaponStyle> implements HasWidget {
   private final WeaponsModule module;

   WeaponStyleSetting(WeaponsModule module, String id, String name, String description) {
      super(id, name, description, WeaponOptions.WeaponStyle.class, WeaponOptions.WeaponStyle.HD);
      this.module = module;
   }

   @Override
   public Widget createWidget() {
      return new StylePickerWidget(this.module, this);
   }
}
