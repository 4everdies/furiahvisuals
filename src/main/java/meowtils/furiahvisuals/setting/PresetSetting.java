package meowtils.furiahvisuals.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class PresetSetting extends Setting {
   public static final String CUSTOM = "custom";
   public static final String USER = "user:";
   private final String defaultSelected;
   private String selected;
   private final List<String[]> user = new ArrayList<>();

   protected PresetSetting(String id, String name, String description, String defaultSelected) {
      super(id, name, description);
      this.defaultSelected = defaultSelected;
      this.selected = defaultSelected;
   }

   public abstract List<PresetSetting.Chip> chips();

   public abstract String current();

   public abstract void apply(String var1);

   public abstract void applySuggestedColor(String var1);

   public abstract String saveCurrent();

   public abstract String exportCode();

   public abstract String codeOf(String var1);

   public abstract String importCode(String var1);

   public String getSelected() {
      return this.selected;
   }

   protected void select(String id) {
      if (id != null && !id.equals(this.selected)) {
         this.selected = id;
         this.fireChanged();
      }
   }

   public List<String[]> userPresets() {
      return Collections.unmodifiableList(this.user);
   }

   protected String userCode(String id) {
      if (id != null && id.startsWith("user:")) {
         String name = id.substring("user:".length());

         for (String[] p : this.user) {
            if (p[0].equals(name)) {
               return p[1];
            }
         }

         return null;
      } else {
         return null;
      }
   }

   protected String putUser(String name, String code) {
      for (String[] p : this.user) {
         if (p[0].equals(name)) {
            p[1] = code;
            this.fireChanged();
            return "user:" + name;
         }
      }

      this.user.add(new String[]{name, code});
      this.fireChanged();
      return "user:" + name;
   }

   protected String freeName(String base) {
      String name = base;

      for (int n = 2; this.userCode("user:" + name) != null; n++) {
         name = base + " " + n;
      }

      return name;
   }

   public void remove(String id) {
      if (id != null && id.startsWith("user:")) {
         String name = id.substring("user:".length());

         for (int i = 0; i < this.user.size(); i++) {
            if (this.user.get(i)[0].equals(name)) {
               this.user.remove(i);
               if (id.equals(this.selected)) {
                  this.selected = "custom";
               }

               this.fireChanged();
               return;
            }
         }
      }
   }

   @Override
   public void reset() {
      this.select(this.defaultSelected);
   }

   @Override
   public JsonElement toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("selected", this.selected);
      JsonArray list = new JsonArray();

      for (String[] p : this.user) {
         JsonObject e = new JsonObject();
         e.addProperty("name", p[0]);
         e.addProperty("code", p[1]);
         list.add(e);
      }

      o.add("user", list);
      return o;
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json != null && json.isJsonObject()) {
         JsonObject o = json.getAsJsonObject();

         try {
            if (o.has("user") && o.get("user").isJsonArray()) {
               this.user.clear();

               for (JsonElement e : o.getAsJsonArray("user")) {
                  if (e.isJsonObject() && e.getAsJsonObject().has("name") && e.getAsJsonObject().has("code")) {
                     String name = e.getAsJsonObject().get("name").getAsString().trim();
                     String code = e.getAsJsonObject().get("code").getAsString().trim();
                     if (!name.isEmpty() && !code.isEmpty() && this.userCode("user:" + name) == null) {
                        this.user.add(new String[]{name, code});
                     }
                  }
               }
            }

            if (o.has("selected")) {
               this.selected = o.get("selected").getAsString();
            }
         } catch (RuntimeException var7) {
         }
      }
   }

   public static final class Chip {
      public final String id;
      public final String name;
      public final String description;
      public final int colorA;
      public final int colorB;
      public final ColorSetting.Mode mode;
      public final boolean removable;

      public Chip(String id, String name, String description, int colorA, int colorB, ColorSetting.Mode mode, boolean removable) {
         this.id = id;
         this.name = name;
         this.description = description;
         this.colorA = colorA;
         this.colorB = colorB;
         this.mode = mode;
         this.removable = removable;
      }
   }
}
