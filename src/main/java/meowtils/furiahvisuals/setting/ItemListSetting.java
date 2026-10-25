package meowtils.furiahvisuals.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ItemListSetting extends Setting {
   private final List<String> items = new ArrayList<>();

   public ItemListSetting(String id, String name, String description) {
      super(id, name, description);
   }

   public static String idOf(ItemStack stack) {
      if (stack != null && stack.getItem() != null) {
         ResourceLocation name = (ResourceLocation)Item.itemRegistry.getNameForObject(stack.getItem());
         return name == null ? null : name.toString();
      } else {
         return null;
      }
   }

   public static ItemStack stackOf(String item) {
      Item i = item == null ? null : Item.getByNameOrId(item);
      return i == null ? null : new ItemStack(i);
   }

   public List<String> get() {
      return Collections.unmodifiableList(this.items);
   }

   public boolean contains(String item) {
      return item != null && this.items.contains(item);
   }

   public boolean add(String item) {
      if (item != null && !this.items.contains(item)) {
         this.items.add(item);
         this.fireChanged();
         return true;
      } else {
         return false;
      }
   }

   public void remove(String item) {
      if (this.items.remove(item)) {
         this.fireChanged();
      }
   }

   @Override
   public void reset() {
      if (!this.items.isEmpty()) {
         this.items.clear();
         this.fireChanged();
      }
   }

   @Override
   public JsonElement toJson() {
      JsonArray a = new JsonArray();

      for (String item : this.items) {
         a.add(new JsonPrimitive(item));
      }

      return a;
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json != null && json.isJsonArray()) {
         this.items.clear();

         for (JsonElement e : json.getAsJsonArray()) {
            String item;
            if (e.isJsonPrimitive() && !(item = e.getAsString().trim()).isEmpty() && !this.items.contains(item)) {
               this.items.add(item);
            }
         }
      }
   }
}
