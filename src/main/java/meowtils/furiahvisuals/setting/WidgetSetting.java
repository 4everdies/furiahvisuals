package meowtils.furiahvisuals.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public abstract class WidgetSetting extends Setting implements HasWidget {
   protected WidgetSetting(String id, String name, String description) {
      super(id, name, description);
   }

   @Override
   public boolean isSaved() {
      return false;
   }

   @Override
   public void reset() {
   }

   @Override
   public JsonElement toJson() {
      return JsonNull.INSTANCE;
   }

   @Override
   public void fromJson(JsonElement json) {
   }
}
