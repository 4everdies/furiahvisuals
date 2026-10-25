package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

public final class WeaponRule {
   static final String BOW_ITEM = "minecraft:bow";
   public static final String VANILLA = "vanilla";
   WeaponRule.Kind kind;
   WeaponRule.Group group = WeaponRule.Group.SWORDS;
   String item;
   int damage = -1;
   String text = "";
   String weapon = "vanilla";
   String finish;
   String style;

   public boolean takesBows() {
      return this.kind == WeaponRule.Kind.GROUP && this.group == WeaponRule.Group.BOWS
         || this.kind == WeaponRule.Kind.ITEM && "minecraft:bow".equals(this.item)
         || this.kind == WeaponRule.Kind.NAME;
   }

   static boolean isBow(ItemStack stack) {
      return stack != null && stack.getItem() instanceof ItemBow;
   }

   WeaponRule(WeaponRule.Kind kind) {
      this.kind = kind;
   }

   static WeaponRule group(WeaponRule.Group g, String weapon) {
      WeaponRule r = new WeaponRule(WeaponRule.Kind.GROUP);
      r.group = g;
      r.weapon = weapon;
      return r;
   }

   static WeaponRule item(String item, int damage, String weapon) {
      WeaponRule r = new WeaponRule(WeaponRule.Kind.ITEM);
      r.item = item;
      r.damage = damage;
      r.weapon = weapon;
      return r;
   }

   static WeaponRule name(String text, String weapon) {
      WeaponRule r = new WeaponRule(WeaponRule.Kind.NAME);
      r.text = text;
      r.weapon = weapon;
      return r;
   }

   public WeaponRule.Kind kind() {
      return this.kind;
   }

   public String weapon() {
      return this.weapon;
   }

   public String finish() {
      return this.finish;
   }

   public String style() {
      return this.style;
   }

   public boolean matches(ItemStack stack) {
      if (stack != null && stack.getItem() != null) {
         switch (this.kind) {
            case GROUP:
               return this.group.contains(stack.getItem());
            case ITEM:
               if (this.item != null && (this.damage < 0 || stack.getItemDamage() == this.damage)) {
                  Object name = Item.itemRegistry.getNameForObject(stack.getItem());
                  return name != null && name.toString().equals(this.item);
               }

               return false;
            default:
               String t = this.text == null ? "" : this.text.trim().toLowerCase(Locale.ROOT);
               if (t.isEmpty()) {
                  return false;
               } else {
                  String n = EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName());
                  return n != null && n.toLowerCase(Locale.ROOT).contains(t);
               }
         }
      } else {
         return false;
      }
   }

   public String targetLabel() {
      switch (this.kind) {
         case GROUP:
            return this.group.displayName;
         case ITEM:
            ItemStack s = this.stack();
            String n = s != null ? s.getDisplayName() : this.item;
            return this.damage >= 0 ? n + " (damage " + this.damage + ")" : n;
         default:
            return "Name contains “" + this.text + "”";
      }
   }

   public ItemStack stack() {
      if (this.kind == WeaponRule.Kind.ITEM && this.item != null) {
         Item i = (Item)Item.itemRegistry.getObject(new ResourceLocation(this.item));
         return i == null ? null : new ItemStack(i, 1, Math.max(0, this.damage));
      } else {
         return null;
      }
   }

   JsonObject toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("target", this.kind.name().toLowerCase(Locale.ROOT));
      switch (this.kind) {
         case GROUP:
            o.addProperty("group", this.group.name().toLowerCase(Locale.ROOT));
            break;
         case ITEM:
            o.addProperty("item", this.item);
            o.addProperty("damage", this.damage);
            break;
         default:
            o.addProperty("text", this.text);
      }

      o.addProperty("weapon", this.weapon);
      if (this.finish != null) {
         o.addProperty("finish", this.finish);
      }

      if (this.style != null) {
         o.addProperty("style", this.style);
      }

      return o;
   }

   static WeaponRule fromJson(JsonElement e) {
      if (e != null && e.isJsonObject()) {
         JsonObject o = e.getAsJsonObject();
         String target = WeaponDef.str(o, "target", "group");
         WeaponRule r;
         if ("item".equalsIgnoreCase(target)) {
            r = new WeaponRule(WeaponRule.Kind.ITEM);
            r.item = WeaponDef.str(o, "item", null);
            r.damage = (int)WeaponDef.num(o, "damage", -1.0);
            if (r.item == null) {
               return null;
            }
         } else if ("name".equalsIgnoreCase(target)) {
            r = new WeaponRule(WeaponRule.Kind.NAME);
            r.text = WeaponDef.str(o, "text", "");
         } else {
            r = new WeaponRule(WeaponRule.Kind.GROUP);
            String g = WeaponDef.str(o, "group", "swords");

            for (WeaponRule.Group x : WeaponRule.Group.values()) {
               if (x.name().equalsIgnoreCase(g)) {
                  r.group = x;
               }
            }
         }

         r.weapon = WeaponDef.str(o, "weapon", "vanilla").toLowerCase(Locale.ROOT);
         r.finish = WeaponDef.str(o, "finish", null);
         r.style = WeaponOptions.StyleChoice.of(WeaponDef.str(o, "style", null)).json();
         return r;
      } else {
         return null;
      }
   }

   WeaponRule copy() {
      return fromJson(this.toJson());
   }

   public static enum Group {
      SWORDS("All swords"),
      AXES("All axes"),
      TOOLS("All pickaxes, shovels and hoes"),
      BOWS("All bows");

      public final String displayName;

      private Group(String displayName) {
         this.displayName = displayName;
      }

      boolean contains(Item item) {
         switch (this) {
            case SWORDS:
               return item instanceof ItemSword;
            case AXES:
               return item instanceof ItemAxe;
            case BOWS:
               return item instanceof ItemBow;
            default:
               return item instanceof ItemPickaxe || item instanceof ItemSpade || item instanceof ItemHoe;
         }
      }
   }

   public static enum Kind {
      GROUP,
      ITEM,
      NAME;
   }
}
