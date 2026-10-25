package meowtils.furiahvisuals.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import meowtils.furiahvisuals.core.ConfigManager;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.ambience.AmbienceModule;
import meowtils.furiahvisuals.module.trails.TrailsModule;
import meowtils.furiahvisuals.module.ui.UiModule;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.KeyBindSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.PresetSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingDefaults;
import meowtils.furiahvisuals.setting.WidgetSetting;

public final class WebPresets {
   public static final String NAME = "$name";
   private static final String CROSSHAIR = "crosshair.";

   private WebPresets() {
   }

   static List<WebPresets.Binding> bindings() {
      ArrayList<WebPresets.Binding> out = new ArrayList<>();
      ConfigManager config = FuriahsVisuals.instance.getConfig();

      for (Setting s : config.presetGeneral()) {
         if (s.isSaved() && !s.isPersonal()) {
            out.add(new WebPresets.Plain(null, s));
         }
      }

      for (Module m : FuriahsVisuals.instance.getModules().getModules()) {
         Set covered = Collections.newSetFromMap(new IdentityHashMap());
         ModeSetting<?> picker = m instanceof TrailsModule ? mode(m, "style") : (m instanceof AmbienceModule ? mode(m, "editing") : null);
         if (picker != null) {
            WebPresets.Active a = new WebPresets.Active(m, picker);
            out.add(a);
            covered.addAll(a.switches);
         }

         for (Setting sx : m.getSettings()) {
            if (!covered.contains(sx) && sx.isSaved() && !sx.isPersonal()) {
               out.add(new WebPresets.Plain(m, sx));
               if (sx instanceof PresetSetting) {
                  out.add(new WebPresets.PresetList(m, sx));
               }
            }
         }

         if (m instanceof UiModule) {
            Setting store = byId(m, "crosshair.defs");

            for (Setting sxx : m.getSettings()) {
               if (store != null
                  && sxx.getOwner() instanceof MiniSection
                  && ((MiniSection)sxx.getOwner()).getId().equals("crosshair")
                  && !sxx.isSaved()
                  && !(sxx instanceof WidgetSetting)
                  && !(sxx instanceof KeyBindSetting)
                  && sxx.getId().startsWith("crosshair.")) {
                  out.add(new WebPresets.CrosshairField(m, store, sxx));
               }
            }
         }
      }

      return out;
   }

   public static JsonObject values(JsonObject config, String name) {
      JsonObject out = new JsonObject();
      if (name != null) {
         out.addProperty("$name", name);
      }

      for (WebPresets.Binding b : bindings()) {
         JsonElement v;
         try {
            v = b.read(config);
         } catch (RuntimeException var7) {
            continue;
         }

         if (v != null && !v.equals(b.defaults())) {
            out.add(b.key, v);
         }
      }

      return out;
   }

   public static JsonObject config(JsonObject values) {
      JsonObject config = new JsonObject();
      config.add("general", new JsonObject());
      config.add("modules", new JsonObject());
      LinkedHashMap byKey = new LinkedHashMap();

      for (Entry e : values.entrySet()) {
         int at = ((String)e.getKey()).indexOf(64);
         byKey.put(at > 0 ? ((String)e.getKey()).substring(0, at) : (String)e.getKey(), e.getValue());
      }

      for (WebPresets.Binding b : bindings()) {
         JsonElement v = (JsonElement)byKey.get(b.key);
         if ((v == null || v.isJsonNull()) && b instanceof WebPresets.Plain && ((WebPresets.Plain)b).setting instanceof PresetSetting) {
            v = b.defaults();
         }

         if (v != null && !v.isJsonNull()) {
            try {
               b.write(config, v);
            } catch (RuntimeException var7) {
            }
         }
      }

      return config;
   }

   public static String name(JsonObject values) {
      JsonElement n = values.get("$name");
      return n != null && n.isJsonPrimitive() ? n.getAsString() : null;
   }

   static JsonObject section(JsonObject config, Module m, boolean create) {
      if (m == null) {
         return child(config, "general", create);
      } else {
         JsonObject modules = child(config, "modules", create);
         if (modules == null) {
            return null;
         } else {
            JsonObject found = ConfigManager.section(modules, m);
            if (found == null && create) {
               found = new JsonObject();
               modules.add(m.getId(), found);
            }

            return found;
         }
      }
   }

   private static JsonObject child(JsonObject o, String key, boolean create) {
      JsonElement e = o.get(key);
      if (e != null && e.isJsonObject()) {
         return e.getAsJsonObject();
      } else if (!create) {
         return null;
      } else {
         JsonObject c = new JsonObject();
         o.add(key, c);
         return c;
      }
   }

   private static ModeSetting<?> mode(Module m, String id) {
      Setting s = byId(m, id);
      return s instanceof ModeSetting ? (ModeSetting)s : null;
   }

   private static Setting byId(Module m, String id) {
      for (Setting s : m.getSettings()) {
         if (s.getId().equals(id)) {
            return s;
         }
      }

      return null;
   }

   static final class Active extends WebPresets.Binding {
      final Module module;
      final List<Enum<?>> styles = new ArrayList<>();
      final List<BooleanSetting> switches = new ArrayList<>();

      Active(Module module, ModeSetting<?> picker) {
         super(WebKeys.section(module) + ".active");
         this.module = module;

         for (Enum c : picker.getValues()) {
            BooleanSetting sw = WebSchema.styleSwitch(module, c);
            if (sw != null) {
               this.styles.add(c);
               this.switches.add(sw);
            }
         }
      }

      @Override
      JsonElement read(JsonObject config) {
         JsonObject sec = WebPresets.section(config, this.module, false);
         if (sec == null) {
            return null;
         } else {
            JsonArray on = new JsonArray();

            for (int i = 0; i < this.styles.size(); i++) {
               BooleanSetting sw = this.switches.get(i);
               JsonElement v = sec.get(sw.getId());
               if (v != null && v.isJsonPrimitive() ? v.getAsBoolean() : sw.getDefault()) {
                  on.add(new JsonPrimitive(WebValues.id(this.styles.get(i).name())));
               }
            }

            return on;
         }
      }

      @Override
      void write(JsonObject config, JsonElement web) {
         if (web.isJsonArray()) {
            HashSet<String> on = new HashSet<>();

            for (JsonElement e : web.getAsJsonArray()) {
               if (e.isJsonPrimitive()) {
                  on.add(e.getAsString());
               }
            }

            JsonObject sec = WebPresets.section(config, this.module, true);

            for (int i = 0; i < this.styles.size(); i++) {
               sec.addProperty(this.switches.get(i).getId(), on.contains(WebValues.id(this.styles.get(i).name())));
            }
         }
      }

      @Override
      JsonElement defaults() {
         JsonArray on = new JsonArray();

         for (int i = 0; i < this.styles.size(); i++) {
            if (this.switches.get(i).getDefault()) {
               on.add(new JsonPrimitive(WebValues.id(this.styles.get(i).name())));
            }
         }

         return on;
      }
   }

   abstract static class Binding {
      final String key;

      Binding(String key) {
         this.key = key;
      }

      abstract JsonElement read(JsonObject var1);

      abstract void write(JsonObject var1, JsonElement var2);

      abstract JsonElement defaults();
   }

   static final class CrosshairField extends WebPresets.Binding {
      final Module module;
      final Setting store;
      final Setting field;
      final String name;

      CrosshairField(Module module, Setting store, Setting field) {
         super(WebKeys.key(module, field));
         this.module = module;
         this.store = store;
         this.field = field;
         this.name = field.getId().substring("crosshair.".length());
      }

      @Override
      JsonElement read(JsonObject config) {
         JsonObject main = this.main(WebPresets.section(config, this.module, false), false);
         return main != null && main.has(this.name) ? WebValues.toWeb(this.field, main.get(this.name)) : null;
      }

      @Override
      void write(JsonObject config, JsonElement web) {
         JsonElement mod = WebValues.toMod(this.field, web);
         if (mod != null) {
            this.main(WebPresets.section(config, this.module, true), true).add(this.name, mod);
         }
      }

      @Override
      JsonElement defaults() {
         JsonElement d = SettingDefaults.get(this.store);
         JsonObject main = d != null && d.isJsonObject() && d.getAsJsonObject().has("main") ? d.getAsJsonObject().getAsJsonObject("main") : null;
         return main != null && main.has(this.name) ? WebValues.toWeb(this.field, main.get(this.name)) : null;
      }

      private JsonObject main(JsonObject sec, boolean create) {
         if (sec == null) {
            return null;
         } else {
            JsonElement defs = sec.get(this.store.getId());
            if (defs == null || !defs.isJsonObject()) {
               if (!create) {
                  return null;
               }

               JsonElement d = SettingDefaults.get(this.store);
               defs = (JsonElement)(d != null && d.isJsonObject() ? d : new JsonObject());
               sec.add(this.store.getId(), defs);
            }

            JsonElement main;
            if ((main = defs.getAsJsonObject().get("main")) == null || !main.isJsonObject()) {
               if (!create) {
                  return null;
               }

               main = new JsonObject();
               defs.getAsJsonObject().add("main", main);
            }

            return main.getAsJsonObject();
         }
      }
   }

   static final class Plain extends WebPresets.Binding {
      final Module module;
      final Setting setting;

      Plain(Module module, Setting setting) {
         super(WebKeys.key(module, setting));
         this.module = module;
         this.setting = setting;
      }

      @Override
      JsonElement read(JsonObject config) {
         JsonObject sec = WebPresets.section(config, this.module, false);
         return sec != null && sec.has(this.setting.getId()) ? WebValues.toWeb(this.setting, sec.get(this.setting.getId())) : null;
      }

      @Override
      void write(JsonObject config, JsonElement web) {
         JsonElement mod = WebValues.toMod(this.setting, web);
         if (mod != null) {
            JsonElement def = SettingDefaults.get(this.setting);
            if (this.setting instanceof ColorSetting && mod.isJsonObject() && def != null && def.isJsonObject()) {
               JsonObject full = def.getAsJsonObject();

               for (Entry e : mod.getAsJsonObject().entrySet()) {
                  full.add((String)e.getKey(), (JsonElement)e.getValue());
               }

               mod = full;
            }

            WebPresets.section(config, this.module, true).add(this.setting.getId(), mod);
         }
      }

      @Override
      JsonElement defaults() {
         JsonElement def = SettingDefaults.get(this.setting);
         return WebValues.toWeb(this.setting, def != null ? def : this.setting.toJson());
      }
   }

   static final class PresetList extends WebPresets.Binding {
      final Module module;
      final Setting setting;

      PresetList(Module module, Setting setting) {
         super(WebKeys.key(module, setting) + ".user");
         this.module = module;
         this.setting = setting;
      }

      @Override
      JsonElement read(JsonObject config) {
         JsonObject sec = WebPresets.section(config, this.module, false);
         JsonElement v = sec == null ? null : sec.get(this.setting.getId());
         return v != null && v.isJsonObject() && v.getAsJsonObject().has("user") ? v.getAsJsonObject().get("user") : null;
      }

      @Override
      void write(JsonObject config, JsonElement web) {
         if (web.isJsonArray()) {
            JsonObject sec = WebPresets.section(config, this.module, true);
            JsonElement v = sec.get(this.setting.getId());
            JsonObject o = v != null && v.isJsonObject() ? v.getAsJsonObject() : new JsonObject();
            o.add("user", web);
            sec.add(this.setting.getId(), o);
         }
      }

      @Override
      JsonElement defaults() {
         return new JsonArray();
      }
   }
}
