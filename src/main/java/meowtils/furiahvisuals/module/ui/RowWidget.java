package meowtils.furiahvisuals.module.ui;

import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.Setting;

public abstract class RowWidget extends Widget {
   public Setting row;

   @Override
   public boolean isVisible() {
      return this.row == null || this.row.isVisible();
   }
}
