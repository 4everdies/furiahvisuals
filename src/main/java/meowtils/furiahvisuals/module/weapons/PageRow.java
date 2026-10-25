package meowtils.furiahvisuals.module.weapons;

import java.util.function.Supplier;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.WidgetSetting;

final class PageRow extends WidgetSetting {
   private final Supplier<Widget> widget;

   PageRow(String id, String name, String description, Supplier<Widget> widget) {
      super(id, name, description);
      this.widget = widget;
   }

   @Override
   public boolean showInMenu() {
      return true;
   }

   @Override
   public Widget createWidget() {
      return this.widget.get();
   }
}
