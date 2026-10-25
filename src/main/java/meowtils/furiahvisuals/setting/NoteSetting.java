package meowtils.furiahvisuals.setting;

import meowtils.furiahvisuals.gui.widget.NoteWidget;
import meowtils.furiahvisuals.gui.widget.Widget;

public final class NoteSetting extends WidgetSetting {
   public NoteSetting(String id, String text) {
      super(id, "", text);
   }

   @Override
   public Widget createWidget() {
      return new NoteWidget(this);
   }
}
