package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.Setting;

public final class WheelSlotsSetting extends Setting implements HasWidget {
   static final int MAX = 48;
   private static final String[] DEFAULTS = new String[]{"vanilla", "tactical_bayonet", "dagger", "katana", "scythe", "halberd", "chainsaw", "photon_edge"};
   private final WeaponsModule module;
   private final List<WheelSlotsSetting.Slot> slots = new ArrayList<>();
   private int version;

   WheelSlotsSetting(WeaponsModule module) {
      super("wheel.slots", "Slots", "The weapons on the wheel, each in a finish of its own: drag to reorder; the same weapon may be in it twice.");
      this.module = module;
      this.fillDefaults();
   }

   private void fillDefaults() {
      this.slots.clear();

      for (String id : DEFAULTS) {
         this.slots.add(new WheelSlotsSetting.Slot(id, null));
      }
   }

   @Override
   public Widget createWidget() {
      return new WheelSlotsWidget(this.module, this);
   }

   public List<WheelSlotsSetting.Slot> get() {
      return Collections.unmodifiableList(this.slots);
   }

   public int size() {
      return this.slots.size();
   }

   public int version() {
      return this.version;
   }

   void add(WheelSlotsSetting.Slot s) {
      if (this.slots.size() < 48) {
         this.slots.add(s);
         this.changed();
      }
   }

   void set(int index, WheelSlotsSetting.Slot s) {
      if (index >= 0 && index < this.slots.size()) {
         this.slots.set(index, s);
         this.changed();
      }
   }

   void remove(int index) {
      if (index >= 0 && index < this.slots.size()) {
         this.slots.remove(index);
         this.changed();
      }
   }

   void move(int from, int to) {
      if (from >= 0 && from < this.slots.size() && to >= 0 && to < this.slots.size() && from != to) {
         this.slots.add(to, this.slots.remove(from));
         this.changed();
      }
   }

   void setAll(List<WheelSlotsSetting.Slot> list) {
      this.slots.clear();

      for (WheelSlotsSetting.Slot s : list) {
         if (this.slots.size() < 48) {
            this.slots.add(s);
         }
      }

      this.changed();
   }

   private void changed() {
      this.version++;
      this.fireChanged();
   }

   @Override
   public boolean showInMenu() {
      return true;
   }

   @Override
   public void reset() {
      this.fillDefaults();
      this.changed();
   }

   @Override
   public JsonElement toJson() {
      JsonArray a = new JsonArray();

      for (WheelSlotsSetting.Slot s : this.slots) {
         a.add(s.toJson());
      }

      return a;
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json != null && json.isJsonArray()) {
         ArrayList<WheelSlotsSetting.Slot> read = new ArrayList<>();

         for (JsonElement e : json.getAsJsonArray()) {
            try {
               WheelSlotsSetting.Slot s = WheelSlotsSetting.Slot.fromJson(e);
               if (s != null && read.size() < 48) {
                  read.add(s);
               }
            } catch (RuntimeException var6) {
            }
         }

         this.slots.clear();
         this.slots.addAll(read);
         this.changed();
      }
   }

   static final class Slot {
      final String weapon;
      final String finish;
      final String style;

      Slot(String weapon, String finish) {
         this(weapon, finish, null);
      }

      Slot(String weapon, String finish, String style) {
         this.weapon = weapon;
         this.finish = finish;
         this.style = style;
      }

      boolean same(String weapon, String finish) {
         return this.weapon.equals(weapon) && (this.finish == null ? finish == null : this.finish.equalsIgnoreCase(finish));
      }

      boolean same(String weapon, String finish, String style) {
         return this.same(weapon, finish) && (this.style == null ? style == null : this.style.equals(style));
      }

      WeaponOptions.Finish finishOrNull() {
         return WeaponOptions.Finish.of(this.finish);
      }

      WeaponOptions.StyleChoice styleChoice() {
         return WeaponOptions.StyleChoice.of(this.style);
      }

      JsonObject toJson() {
         JsonObject o = new JsonObject();
         o.addProperty("weapon", this.weapon);
         if (this.finish != null) {
            o.addProperty("finish", this.finish);
         }

         if (this.style != null) {
            o.addProperty("style", this.style);
         }

         return o;
      }

      static WheelSlotsSetting.Slot fromJson(JsonElement e) {
         if (e != null && e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            String weapon = WeaponDef.str(o, "weapon", null);
            if (weapon != null && !weapon.trim().isEmpty()) {
               String finish = WeaponDef.str(o, "finish", null);
               WeaponOptions.Finish f = WeaponOptions.Finish.of(finish);
               return new WheelSlotsSetting.Slot(
                  weapon.trim().toLowerCase(Locale.ROOT),
                  f == null ? null : f.name().toLowerCase(Locale.ROOT),
                  WeaponOptions.StyleChoice.of(WeaponDef.str(o, "style", null)).json()
               );
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }
}
