package meowtils.furiahvisuals.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.IdentityHashMap;
import java.util.Map;

public final class SettingDefaults {
   private static final Map<Setting, JsonElement> DEFAULTS = new IdentityHashMap<>();

   private SettingDefaults() {
   }

   public static void capture(Iterable<? extends Setting> settings) {
      for (Setting setting : settings) {
         try {
            JsonElement json = setting.toJson();
            if (json != null) {
               DEFAULTS.put(setting, json);
            }
         } catch (RuntimeException var4) {
         }
      }
   }

   public static boolean known(Setting s) {
      return DEFAULTS.containsKey(s);
   }

   public static JsonElement get(Setting s) {
      JsonElement d = DEFAULTS.get(s);
      return d == null ? null : new JsonParser().parse(d.toString());
   }

   public static boolean isDefault(Setting s) {
      JsonElement d = DEFAULTS.get(s);
      if (d == null) {
         return true;
      } else {
         try {
            return d.equals(s.toJson());
         } catch (RuntimeException var3) {
            return true;
         }
      }
   }
}
