package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.Choices;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;

public final class FinishSetting extends Setting implements HasWidget, Choices {
   private final List<WeaponOptions.Finish> options;
   private final WeaponOptions.Finish defaultValue;
   private WeaponOptions.Finish value;

   FinishSetting(String id, String name, String description, List<String> finishes, String start) {
      super(id, name, description);
      ArrayList<WeaponOptions.Finish> list = new ArrayList<>();

      for (String s : finishes) {
         WeaponOptions.Finish f = WeaponOptions.Finish.of(s);
         if (f != null && f != WeaponOptions.Finish.CUSTOM && !list.contains(f)) {
            list.add(f);
         }
      }

      if (list.isEmpty()) {
         list.add(WeaponOptions.Finish.STEEL_POLISHED);
      }

      list.add(WeaponOptions.Finish.CUSTOM);
      this.options = Collections.unmodifiableList(list);
      WeaponOptions.Finish d = WeaponOptions.Finish.of(start);
      this.value = this.defaultValue = d != null && list.contains(d) ? d : list.get(0);
   }

   public List<WeaponOptions.Finish> options() {
      return this.options;
   }

   @Override
   public List<String[]> choices() {
      ArrayList<String[]> out = new ArrayList<>();

      for (WeaponOptions.Finish f : this.options) {
         out.add(new String[]{f.name(), ModeSetting.displayName(f)});
      }

      return out;
   }

   public WeaponOptions.Finish get() {
      return this.value;
   }

   public WeaponOptions.Finish getDefault() {
      return this.defaultValue;
   }

   public void set(WeaponOptions.Finish f) {
      if (f != null && f != this.value && this.options.contains(f)) {
         this.value = f;
         this.fireChanged();
      }
   }

   @Override
   public boolean showInMenu() {
      return true;
   }

   @Override
   public Widget createWidget() {
      return new FinishWidget(this);
   }

   @Override
   public void reset() {
      this.set(this.defaultValue);
   }

   @Override
   public JsonElement toJson() {
      return new JsonPrimitive(this.value.name());
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json != null && json.isJsonPrimitive()) {
         this.set(WeaponOptions.Finish.of(json.getAsString()));
      }
   }
}
