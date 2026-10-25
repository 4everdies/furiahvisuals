package meowtils.furiahvisuals.module.ui;

import java.util.function.Supplier;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.WidgetSetting;

final class UiRow extends WidgetSetting {
   private final Supplier<Widget> widget;

   UiRow(String id, String name, String description, Supplier<Widget> widget) {
      super(id, name, description);
      this.widget = widget;
   }

   @Override
   public Widget createWidget() {
      Widget w = this.widget.get();
      if (w instanceof RowWidget) {
         ((RowWidget)w).row = this;
      }

      return w;
   }
}
