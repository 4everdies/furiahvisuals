package meowtils.furiahvisuals.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import meowtils.furiahvisuals.render.ColorUtil;

public class ItemRulesSetting extends Setting {
   private final PresetSetting presets;
   private final List<ItemRulesSetting.Rule> rules = new ArrayList<>();
   private final Runnable colorChanged = new Runnable() {
      @Override
      public void run() {
         ItemRulesSetting.this.fireChanged();
      }
   };

   public ItemRulesSetting(String id, String name, String description, PresetSetting presets) {
      super(id, name, description);
      this.presets = presets;
   }

   public PresetSetting getPresets() {
      return this.presets;
   }

   public List<ItemRulesSetting.Rule> get() {
      return Collections.unmodifiableList(this.rules);
   }

   public ItemRulesSetting.Rule find(String item) {
      if (item == null) {
         return null;
      } else {
         for (int i = 0; i < this.rules.size(); i++) {
            if (this.rules.get(i).item.equals(item)) {
               return this.rules.get(i);
            }
         }

         return null;
      }
   }

   public ItemRulesSetting.Rule add(String item) {
      ItemRulesSetting.Rule r = this.find(item);
      if (r == null) {
         r = new ItemRulesSetting.Rule(item);
         this.rules.add(r);
         this.fireChanged();
      }

      return r;
   }

   public void remove(ItemRulesSetting.Rule rule) {
      if (this.rules.remove(rule)) {
         this.fireChanged();
      }
   }

   public void setPreset(ItemRulesSetting.Rule rule, String preset) {
      if (preset == null ? rule.preset != null : !preset.equals(rule.preset)) {
         rule.preset = preset;
         this.fireChanged();
      }
   }

   public void cyclePreset(ItemRulesSetting.Rule rule, int dir) {
      List<PresetSetting.Chip> chips = this.presets.chips();
      int index = -1;

      for (int i = 0; i < chips.size(); i++) {
         if (chips.get(i).id.equals(rule.preset)) {
            index = i;
         }
      }

      int next = Math.floorMod(index + 1 + dir, chips.size() + 1) - 1;
      this.setPreset(rule, next < 0 ? null : chips.get(next).id);
   }

   public void setColor(ItemRulesSetting.Rule rule, boolean on, int start) {
      if (on && rule.color == null) {
         rule.color = this.newColor(start);
         this.fireChanged();
      } else if (!on && rule.color != null) {
         rule.color = null;
         this.fireChanged();
      }
   }

   private ColorSetting newColor(int start) {
      ColorSetting c = new ColorSetting("color", "Color", "The glow color for this item.", ColorSetting.Mode.STATIC, start, ColorUtil.core(start), 1.0);
      c.addListener(this.colorChanged);
      return c;
   }

   @Override
   public void reset() {
      if (!this.rules.isEmpty()) {
         this.rules.clear();
         this.fireChanged();
      }
   }

   @Override
   public JsonElement toJson() {
      JsonArray a = new JsonArray();

      for (ItemRulesSetting.Rule r : this.rules) {
         JsonObject o = new JsonObject();
         o.addProperty("item", r.item);
         if (r.preset != null) {
            o.addProperty("preset", r.preset);
         }

         if (r.color != null) {
            o.add("color", r.color.toJson());
         }

         a.add(o);
      }

      return a;
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json != null && json.isJsonArray()) {
         this.rules.clear();

         for (JsonElement e : json.getAsJsonArray()) {
            try {
               JsonObject o;
               String item;
               if (e.isJsonObject()
                  && e.getAsJsonObject().has("item")
                  && !(item = (o = e.getAsJsonObject()).get("item").getAsString().trim()).isEmpty()
                  && this.find(item) == null) {
                  ItemRulesSetting.Rule r = new ItemRulesSetting.Rule(item);
                  if (o.has("preset")) {
                     r.preset = o.get("preset").getAsString();
                  }

                  if (o.has("color") && o.get("color").isJsonObject()) {
                     r.color = this.newColor(-1);
                     r.color.fromJson(o.get("color"));
                  }

                  this.rules.add(r);
               }
            } catch (RuntimeException var7) {
            }
         }
      }
   }

   public static final class Rule {
      public final String item;
      private String preset;
      private ColorSetting color;

      private Rule(String item) {
         this.item = item;
      }

      public String getPreset() {
         return this.preset;
      }

      public ColorSetting getColor() {
         return this.color;
      }
   }
}
