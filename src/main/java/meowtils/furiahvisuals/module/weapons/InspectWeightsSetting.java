package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.Setting;

public final class InspectWeightsSetting extends Setting implements HasWidget {
   public static final int MAX_WEIGHT = 3;
   private final List<String> inspects;
   private final Map<String, Integer> weights = new LinkedHashMap<>();
   WeaponsModule module;
   WeaponDef def;

   InspectWeightsSetting(String id, String name, String description, List<String> inspects) {
      super(id, name, description);
      this.inspects = Collections.unmodifiableList(new ArrayList<>(inspects));

      for (String s : inspects) {
         this.weights.put(s, 1);
      }
   }

   public List<String> inspects() {
      return this.inspects;
   }

   public int weight(String inspect) {
      Integer w = this.weights.get(inspect);
      return w == null ? 0 : w;
   }

   public void setWeight(String inspect, int weight) {
      if (this.weights.containsKey(inspect)) {
         int w = Math.max(0, Math.min(3, weight));
         if (w != this.weights.get(inspect)) {
            this.weights.put(inspect, w);
            this.fireChanged();
         }
      }
   }

   public List<String> enabled() {
      ArrayList<String> out = new ArrayList<>();

      for (String s : this.inspects) {
         if (this.weight(s) > 0) {
            out.add(s);
         }
      }

      return out;
   }

   @Override
   public boolean showInMenu() {
      return true;
   }

   @Override
   public Widget createWidget() {
      return new InspectWeightsWidget(this.module, this, this.def);
   }

   @Override
   public void reset() {
      boolean changed = false;

      for (String s : this.inspects) {
         if (this.weights.put(s, 1) != 1) {
            changed = true;
         }
      }

      if (changed) {
         this.fireChanged();
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject o = new JsonObject();

      for (String s : this.inspects) {
         o.addProperty(s, this.weight(s));
      }

      return o;
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json != null && json.isJsonObject()) {
         boolean changed = false;

         for (Entry e : json.getAsJsonObject().entrySet()) {
            if (this.weights.containsKey(e.getKey())) {
               try {
                  int w = Math.max(0, Math.min(3, ((JsonElement)e.getValue()).getAsInt()));
                  if (w != this.weights.get(e.getKey())) {
                     this.weights.put((String)e.getKey(), w);
                     changed = true;
                  }
               } catch (RuntimeException var6) {
               }
            }
         }

         if (changed) {
            this.fireChanged();
         }
      }
   }
}
