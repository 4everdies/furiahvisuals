package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.Setting;
import net.minecraft.item.ItemStack;

public final class WeaponRulesSetting extends Setting implements HasWidget {
   private final WeaponsModule module;
   private final List<WeaponRule> rules = new ArrayList<>();
   private int selected;
   private int version;

   WeaponRulesSetting(WeaponsModule module) {
      super("rules", "Replaces", "Which held items turn into which weapon: the first rule that matches wins; drag to reorder.");
      this.module = module;
      this.rules.add(defaultRule());
   }

   @Override
   public Widget createWidget() {
      return new WeaponRulesWidget(this.module, this);
   }

   private static WeaponRule defaultRule() {
      return WeaponRule.group(WeaponRule.Group.SWORDS, "vanilla");
   }

   public List<WeaponRule> get() {
      return Collections.unmodifiableList(this.rules);
   }

   public int version() {
      return this.version;
   }

   public WeaponRule match(ItemStack stack) {
      if (stack == null) {
         return null;
      } else {
         boolean bow = WeaponRule.isBow(stack);

         for (int i = 0; i < this.rules.size(); i++) {
            WeaponRule r = this.rules.get(i);
            if (r.matches(stack) && (bow || !this.module.isBowWeapon(r.weapon))) {
               return r;
            }
         }

         return null;
      }
   }

   public int selected() {
      return Math.max(0, Math.min(this.selected, this.rules.size() - 1));
   }

   public WeaponRule selectedRule() {
      return this.rules.isEmpty() ? null : this.rules.get(this.selected());
   }

   public void select(int index) {
      this.selected = Math.max(0, Math.min(index, this.rules.size() - 1));
   }

   public void add(WeaponRule r) {
      this.rules.add(r);
      this.selected = this.rules.size() - 1;
      this.changed();
   }

   public void remove(int index) {
      if (index >= 0 && index < this.rules.size()) {
         this.rules.remove(index);
         this.selected = Math.max(0, Math.min(this.selected, this.rules.size() - 1));
         this.changed();
      }
   }

   public void move(int from, int to) {
      if (from >= 0 && from < this.rules.size() && to >= 0 && to < this.rules.size() && from != to) {
         WeaponRule r = this.rules.remove(from);
         this.rules.add(to, r);
         if (this.selected == from) {
            this.selected = to;
         }

         this.changed();
      }
   }

   public void setWeapon(WeaponRule r, String weapon) {
      if (!weapon.equals(r.weapon)) {
         r.weapon = weapon;
         this.changed();
      }
   }

   public void setFinish(WeaponRule r, String finish) {
      if (finish == null ? r.finish != null : !finish.equals(r.finish)) {
         r.finish = finish;
         this.changed();
      }
   }

   public void setStyle(WeaponRule r, String style) {
      if (style == null ? r.style != null : !style.equals(r.style)) {
         r.style = style;
         this.changed();
      }
   }

   public void setText(WeaponRule r, String text) {
      if (!text.equals(r.text)) {
         r.text = text;
         this.changed();
      }
   }

   public boolean setGroup(WeaponRule r, WeaponRule.Group g) {
      r.kind = WeaponRule.Kind.GROUP;
      r.group = g;
      boolean dropped = this.dropBow(r);
      this.changed();
      return dropped;
   }

   public boolean setItem(WeaponRule r, String item, int damage) {
      r.kind = WeaponRule.Kind.ITEM;
      r.item = item;
      r.damage = damage;
      boolean dropped = this.dropBow(r);
      this.changed();
      return dropped;
   }

   private boolean dropBow(WeaponRule r) {
      if (!r.takesBows() && this.module.isBowWeapon(r.weapon)) {
         r.weapon = "vanilla";
         return true;
      } else {
         return false;
      }
   }

   public void setName(WeaponRule r, String text) {
      r.kind = WeaponRule.Kind.NAME;
      r.text = text;
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
      this.rules.clear();
      this.rules.add(defaultRule());
      this.selected = 0;
      this.changed();
   }

   @Override
   public JsonElement toJson() {
      JsonArray a = new JsonArray();

      for (WeaponRule r : this.rules) {
         a.add(r.toJson());
      }

      return a;
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json != null && json.isJsonArray()) {
         ArrayList<WeaponRule> read = new ArrayList<>();

         for (JsonElement e : json.getAsJsonArray()) {
            try {
               WeaponRule r = WeaponRule.fromJson(e);
               if (r != null) {
                  read.add(r);
               }
            } catch (RuntimeException var6) {
            }
         }

         this.rules.clear();
         this.rules.addAll(read);
         this.selected = 0;
         this.changed();
      }
   }
}
