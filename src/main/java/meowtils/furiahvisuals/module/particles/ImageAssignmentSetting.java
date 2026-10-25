package meowtils.furiahvisuals.module.particles;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.Setting;

final class ImageAssignmentSetting extends Setting implements HasWidget {
   private final ParticlesModule module;
   private final int kind;
   private ImageAssignmentSetting.Mode mode = ImageAssignmentSetting.Mode.VANILLA;
   private String single;
   private final List<String> set = new ArrayList<>();
   private int version;

   ImageAssignmentSetting(ParticlesModule module, int kind, String id, String name, String description) {
      super(id, name, description);
      this.module = module;
      this.kind = kind;
   }

   @Override
   public Widget createWidget() {
      return new AssignmentWidget(this.module, this, this.kind);
   }

   ImageAssignmentSetting.Mode getMode() {
      return this.mode;
   }

   void setMode(ImageAssignmentSetting.Mode mode) {
      if (mode != null && mode != this.mode) {
         this.mode = mode;
         this.changed();
      }
   }

   String getSingle() {
      return this.single;
   }

   List<String> getSet() {
      return Collections.unmodifiableList(this.set);
   }

   List<String> active() {
      if (this.mode == ImageAssignmentSetting.Mode.IMAGE) {
         return this.single == null ? Collections.emptyList() : Collections.singletonList(this.single);
      } else {
         return this.mode == ImageAssignmentSetting.Mode.RANDOM ? this.getSet() : Collections.emptyList();
      }
   }

   int activeCount() {
      if (this.mode == ImageAssignmentSetting.Mode.IMAGE) {
         return this.single == null ? 0 : 1;
      } else {
         return this.mode == ImageAssignmentSetting.Mode.RANDOM ? this.set.size() : 0;
      }
   }

   boolean uses(String name) {
      return this.active().contains(name);
   }

   void click(String name) {
      if (this.mode == ImageAssignmentSetting.Mode.IMAGE) {
         if (!name.equals(this.single)) {
            this.single = name;
            this.changed();
         }
      } else if (this.mode == ImageAssignmentSetting.Mode.RANDOM) {
         if (!this.set.remove(name)) {
            this.set.add(name);
         }

         this.changed();
      }
   }

   void rename(String from, String to) {
      boolean c = false;
      if (from.equals(this.single)) {
         this.single = to;
         c = true;
      }

      for (int i = 0; i < this.set.size(); i++) {
         if (this.set.get(i).equals(from)) {
            this.set.set(i, to);
            c = true;
         }
      }

      if (c) {
         this.changed();
      }
   }

   void remove(String name) {
      boolean c = this.set.remove(name);
      if (name.equals(this.single)) {
         this.single = null;
         c = true;
      }

      if (c) {
         this.changed();
      }
   }

   int version() {
      return this.version;
   }

   private void changed() {
      this.version++;
      this.fireChanged();
   }

   @Override
   public void reset() {
      this.mode = ImageAssignmentSetting.Mode.VANILLA;
      this.single = null;
      this.set.clear();
      this.changed();
   }

   @Override
   public JsonElement toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("mode", this.mode.name());
      if (this.single != null) {
         o.addProperty("image", this.single);
      }

      JsonArray list = new JsonArray();

      for (String s : this.set) {
         list.add(new JsonPrimitive(s));
      }

      o.add("set", list);
      return o;
   }

   @Override
   public void fromJson(JsonElement json) {
      if (json != null && json.isJsonObject()) {
         JsonObject o = json.getAsJsonObject();

         try {
            if (o.has("mode")) {
               for (ImageAssignmentSetting.Mode m : ImageAssignmentSetting.Mode.values()) {
                  if (m.name().equalsIgnoreCase(o.get("mode").getAsString())) {
                     this.mode = m;
                  }
               }
            }

            this.single = o.has("image") && o.get("image").isJsonPrimitive() ? o.get("image").getAsString() : null;
            this.set.clear();
            if (o.has("set") && o.get("set").isJsonArray()) {
               for (JsonElement e : o.getAsJsonArray("set")) {
                  if (e.isJsonPrimitive() && !this.set.contains(e.getAsString())) {
                     this.set.add(e.getAsString());
                  }
               }
            }

            this.changed();
         } catch (RuntimeException var7) {
         }
      }
   }

   static enum Mode implements Displayable {
      VANILLA("Vanilla sprite", "The particle's own sprite"),
      IMAGE("One image", "Every particle uses the chosen image"),
      RANDOM("Random from set", "Each particle picks one of the chosen images when it spawns");

      private final String displayName;
      private final String description;

      private Mode(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }
}
