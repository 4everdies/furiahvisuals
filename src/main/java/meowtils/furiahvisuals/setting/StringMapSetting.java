package meowtils.furiahvisuals.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class StringMapSetting extends Setting {
   private final Map<String, String> values = new LinkedHashMap<>();

   public StringMapSetting(String id) {
      super(id, id, "");
   }

   public String get(String key) {
      return this.values.get(key);
   }

   public void put(String key, String value) {
      String old = this.values.put(key, value);
      if (old == null || !old.equals(value)) {
         this.fireChanged();
      }
   }

   public void remove(String key) {
      if (this.values.remove(key) != null) {
         this.fireChanged();
      }
   }

   public Map<String, String> asMap() {
      return Collections.unmodifiableMap(this.values);
   }

   @Override
   public boolean showInMenu() {
      return false;
   }

   @Override
   public void reset() {
      if (!this.values.isEmpty()) {
         this.values.clear();
         this.fireChanged();
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject o = new JsonObject();

      for (Entry<String, String> e : this.values.entrySet()) {
         o.addProperty(e.getKey(), e.getValue());
      }

      return o;
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json != null && json.isJsonObject()) {
         this.values.clear();

         for (Entry e : json.getAsJsonObject().entrySet()) {
            if (((JsonElement)e.getValue()).isJsonPrimitive()) {
               this.values.put((String)e.getKey(), ((JsonElement)e.getValue()).getAsString());
            }
         }
      }
   }
}
