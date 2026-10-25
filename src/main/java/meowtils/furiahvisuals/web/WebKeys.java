package meowtils.furiahvisuals.web;

import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.setting.Setting;

public final class WebKeys {
   public static final String GENERAL = "settings.";
   public static final String ACTIVE = "active";

   private WebKeys() {
   }

   public static String section(Module m) {
      return WebValues.id(m.getId());
   }

   public static String key(Module m, Setting s) {
      return (m == null ? "settings." : section(m) + ".") + s.getId();
   }
}
