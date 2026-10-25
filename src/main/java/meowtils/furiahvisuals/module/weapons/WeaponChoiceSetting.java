package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.Choices;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.Setting;

public final class WeaponChoiceSetting extends Setting implements HasWidget, Choices {
   public static final String SAME = "same";
   private final String defaultValue;
   private String value;
   WeaponsModule module;
   WeaponDef def;

   WeaponChoiceSetting(String id, String name, String description, String defaultValue) {
      super(id, name, description);
      this.defaultValue = defaultValue;
      this.value = defaultValue;
   }

   public String get() {
      return this.value;
   }

   public void set(String v) {
      if (v != null && !v.equals(this.value)) {
         this.value = v;
         this.fireChanged();
      }
   }

   @Override
   public List<String[]> choices() {
      ArrayList<String[]> out = new ArrayList<>();
      out.add(new String[]{"same", "Same as the right hand"});
      if (this.module != null) {
         for (WeaponDef d : this.module.cardWeapons()) {
            if (!d.twoHanded) {
               out.add(new String[]{d.id, d.name});
            }
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
      return new WeaponChoiceWidget(this.module, this, this.def);
   }

   @Override
   public void reset() {
      this.set(this.defaultValue);
   }

   @Override
   public JsonElement toJson() {
      return new JsonPrimitive(this.value);
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json != null && json.isJsonPrimitive()) {
         this.set(json.getAsString());
      }
   }
}
