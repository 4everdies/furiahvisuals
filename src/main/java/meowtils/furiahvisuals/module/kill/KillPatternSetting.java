package meowtils.furiahvisuals.module.kill;

import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.StringListSetting;

final class KillPatternSetting extends StringListSetting implements HasWidget {
   static final int MAX_LENGTH = 120;

   KillPatternSetting(String id, String name, String description, String... defaults) {
      super(id, name, description, defaults);
   }

   @Override
   public boolean showInMenu() {
      return true;
   }

   @Override
   public Widget createWidget() {
      return new KillPatternWidget(this);
   }

   static String problem(String pattern) {
      String p = pattern.trim();
      if (p.isEmpty()) {
         return "Type a message first";
      } else if (!p.contains("{victim}")) {
         return "A pattern needs {victim} where the name of the player who died appears";
      } else if (p.indexOf("{victim}") != p.lastIndexOf("{victim}")) {
         return "Use {victim} only once";
      } else {
         return p.replace("{victim}", "").replace("{me}", "").replace("*", "").trim().length() < 3
            ? "Add some of the message's words, or it matches everything"
            : null;
      }
   }
}
