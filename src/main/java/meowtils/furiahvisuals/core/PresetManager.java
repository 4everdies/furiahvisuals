package meowtils.furiahvisuals.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.TextSetting;
import meowtils.furiahvisuals.web.WebCodes;
import meowtils.furiahvisuals.web.WebPresets;

public final class PresetManager {
   public static final int FORMAT = 1;
   public static final int MAX_NAME = 32;
   public static final String CODE_PREFIX = "FVP1-";
   private static final String[] RESERVED = new String[]{
      "con",
      "prn",
      "aux",
      "nul",
      "com1",
      "com2",
      "com3",
      "com4",
      "com5",
      "com6",
      "com7",
      "com8",
      "com9",
      "lpt1",
      "lpt2",
      "lpt3",
      "lpt4",
      "lpt5",
      "lpt6",
      "lpt7",
      "lpt8",
      "lpt9"
   };
   private final File dir;
   private final ConfigManager config;
   private final ModuleManager modules;
   private final TextSetting active;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private final List<PresetManager.Preset> presets = new ArrayList<>();
   private boolean scanned;
   private PresetManager.Listener listener;
   private boolean applying;
   private int modifiedAt = -1;
   private final List<PresetManager.Preset> unmodified = new ArrayList<>();

   PresetManager(File dir, ConfigManager config, ModuleManager modules, TextSetting active) {
      this.dir = dir;
      this.config = config;
      this.modules = modules;
      this.active = active;
   }

   public void setListener(PresetManager.Listener listener) {
      this.listener = listener;
   }

   public boolean isApplying() {
      return this.applying;
   }

   public File getDir() {
      return this.dir;
   }

   public List<PresetManager.Preset> list() {
      if (!this.scanned) {
         this.refresh();
      }

      return Collections.unmodifiableList(this.presets);
   }

   public void refresh() {
      this.scanned = true;
      this.presets.clear();
      this.modifiedAt = -1;
      File[] files = this.dir.listFiles();
      if (files != null) {
         for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
               this.presets.add(this.read(f));
            }
         }

         Collections.sort(this.presets, new Comparator<PresetManager.Preset>() {
            public int compare(PresetManager.Preset a, PresetManager.Preset b) {
               return a.name.compareToIgnoreCase(b.name);
            }
         });
      }
   }

   private PresetManager.Preset read(File f) {
      String fileName = f.getName().substring(0, f.getName().length() - 5);

      try {
         JsonElement root = new JsonParser().parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
         if (root != null && root.isJsonObject() && root.getAsJsonObject().has("config") && root.getAsJsonObject().get("config").isJsonObject()) {
            JsonObject o = root.getAsJsonObject();
            int format = o.has("format") && o.get("format").isJsonPrimitive() ? o.get("format").getAsInt() : 1;
            if (format > 1) {
               return new PresetManager.Preset(fileName, f, f.lastModified(), null, "Made by a newer version of the mod");
            } else {
               long saved = o.has("saved") && o.get("saved").isJsonPrimitive() ? o.get("saved").getAsLong() : f.lastModified();
               return new PresetManager.Preset(fileName, f, saved, migrate(o), null);
            }
         } else {
            return new PresetManager.Preset(fileName, f, f.lastModified(), null, "Not a Furiah's Visuals preset");
         }
      } catch (Exception var9) {
         return new PresetManager.Preset(fileName, f, f.lastModified(), null, "Damaged file");
      }
   }

   private static JsonObject migrate(JsonObject file) {
      return file.getAsJsonObject("config");
   }

   public PresetManager.Preset find(String name) {
      for (PresetManager.Preset p : this.list()) {
         if (p.name.equalsIgnoreCase(name)) {
            return p;
         }
      }

      return null;
   }

   public PresetManager.Preset getActive() {
      return this.active.get().isEmpty() ? null : this.find(this.active.get());
   }

   public boolean isModified(PresetManager.Preset p) {
      if (p != null && !p.isBroken()) {
         if (this.modifiedAt != this.config.changeCount()) {
            this.modifiedAt = this.config.changeCount();
            this.unmodified.clear();
            JsonObject now = this.config.snapshot();

            for (PresetManager.Preset each : this.presets) {
               if (!each.isBroken() && this.matches(each.config, now)) {
                  this.unmodified.add(each);
               }
            }
         }

         return !this.unmodified.contains(p);
      } else {
         return false;
      }
   }

   private boolean matches(JsonObject preset, JsonObject now) {
      return sectionMatches(preset, now, "general") && this.modulesMatch(preset, now);
   }

   private boolean modulesMatch(JsonObject preset, JsonObject now) {
      if (preset.has("modules") && preset.get("modules").isJsonObject()) {
         for (Entry e : preset.getAsJsonObject("modules").entrySet()) {
            Module m = this.modules.get((String)e.getKey());
            JsonObject mine;
            if (m != null && ((JsonElement)e.getValue()).isJsonObject() && (mine = now.getAsJsonObject("modules").getAsJsonObject((String)e.getKey())) != null) {
               for (Entry s : ((JsonElement)e.getValue()).getAsJsonObject().entrySet()) {
                  Setting setting = settingOf(m, (String)s.getKey());
                  if (setting != null && !setting.isPersonal() && !((JsonElement)s.getValue()).equals(mine.get((String)s.getKey()))) {
                     return false;
                  }
               }
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private static boolean sectionMatches(JsonObject preset, JsonObject now, String key) {
      if (preset.has(key) && preset.get(key).isJsonObject()) {
         JsonObject mine = now.getAsJsonObject(key);

         for (Entry e : preset.getAsJsonObject(key).entrySet()) {
            if (mine.has((String)e.getKey()) && !((JsonElement)e.getValue()).equals(mine.get((String)e.getKey()))) {
               return false;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private static Setting settingOf(Module m, String id) {
      for (Setting s : m.getSettings()) {
         if (s.getId().equals(id)) {
            return s;
         }
      }

      return null;
   }

   public String checkName(String name, PresetManager.Preset except) {
      String n = name == null ? "" : name.trim();
      if (n.isEmpty()) {
         return "Give the preset a name.";
      } else if (n.length() > 32) {
         return "Names can be at most 32 characters.";
      } else {
         for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (c < ' ' || "\\/:*?\"<>|".indexOf(c) >= 0) {
               return "Names cannot contain \\ / : * ? \" < > |";
            }
         }

         if (n.endsWith(".")) {
            return "Names cannot end with a dot.";
         } else {
            String base = n.toLowerCase(Locale.ROOT);
            int dot = base.indexOf(46);
            if (dot >= 0) {
               base = base.substring(0, dot);
            }

            for (String r : RESERVED) {
               if (r.equals(base)) {
                  return "\"" + n + "\" cannot be used as a file name.";
               }
            }

            PresetManager.Preset same = this.find(n);
            return same != null && same != except ? "A preset called \"" + same.name + "\" already exists." : null;
         }
      }
   }

   public String create(String name) {
      String problem = this.checkName(name, null);
      if (problem != null) {
         return problem;
      } else {
         String n = name.trim();
         String error = this.write(n, this.config.snapshot());
         if (error != null) {
            return error;
         } else {
            this.active.set(n);
            this.refresh();
            return null;
         }
      }
   }

   public String update(PresetManager.Preset p) {
      String error = this.write(p.name, this.config.snapshot());
      if (error != null) {
         return error;
      } else {
         this.active.set(p.name);
         this.refresh();
         return null;
      }
   }

   public String rename(PresetManager.Preset p, String newName) {
      String problem = this.checkName(newName, p);
      if (problem != null) {
         return problem;
      } else {
         String n = newName.trim();
         if (n.equals(p.name)) {
            return null;
         } else if (p.isBroken()) {
            return "This file cannot be read, so it cannot be renamed here.";
         } else {
            if (p.name.equalsIgnoreCase(n)) {
               File aside = new File(this.dir, p.name + ".json.rename");

               try {
                  Files.move(p.file.toPath(), aside.toPath(), StandardCopyOption.REPLACE_EXISTING);
               } catch (IOException var7) {
                  return "Could not rename " + p.file.getName() + ".";
               }

               String error = this.write(n, p.config, p.saved);
               if (error != null) {
                  aside.renameTo(p.file);
                  return error;
               }

               if (!aside.delete()) {
                  FuriahsVisuals.LOG.warn("Could not remove {}", new Object[]{aside});
               }
            } else {
               String errorx = this.write(n, p.config, p.saved);
               if (errorx != null) {
                  return errorx;
               }

               if (!p.file.delete()) {
                  FuriahsVisuals.LOG.warn("Could not remove {} after renaming it", new Object[]{p.file});
               }
            }

            if (this.active.get().equalsIgnoreCase(p.name)) {
               this.active.set(n);
            }

            this.refresh();
            return null;
         }
      }
   }

   public String duplicate(PresetManager.Preset p) {
      if (p.isBroken()) {
         return "This file cannot be read.";
      } else {
         String n = this.freeName(p.name);
         String error = this.write(n, p.config, System.currentTimeMillis());
         this.refresh();
         return error;
      }
   }

   public String delete(PresetManager.Preset p) {
      if (p.file.exists() && !p.file.delete()) {
         return "Could not delete " + p.file.getName() + " (is it open somewhere?)";
      } else {
         if (this.active.get().equalsIgnoreCase(p.name)) {
            this.active.set("");
         }

         this.refresh();
         return null;
      }
   }

   public String load(PresetManager.Preset p) {
      if (p.isBroken()) {
         return p.error;
      } else {
         this.applying = true;

         try {
            this.config.apply(p.config);
         } finally {
            this.applying = false;
         }

         this.active.set(p.name);
         StringBuilder warning = null;

         for (Module m : this.modules.getModules()) {
            String w2 = m.getLoadWarning();
            if (w2 != null) {
               warning = warning == null ? new StringBuilder(w2) : warning.append(' ').append(w2);
            }
         }

         String w = warning == null ? null : warning.toString();
         if (this.listener != null) {
            this.listener.presetLoaded(p.name, w);
         }

         return w;
      }
   }

   public String exportCode(PresetManager.Preset p) {
      return p.isBroken() ? null : WebCodes.encode(WebPresets.values(this.withoutPersonal(p.config), p.name));
   }

   public String importCode(String code) {
      if (WebCodes.isCode(code)) {
         JsonObject values = WebCodes.decode(code);
         if (values == null) {
            return "!That code is damaged.";
         } else {
            String name = WebPresets.name(values);
            return this.addImported(name != null ? name : "Imported", WebPresets.config(values));
         }
      } else if (code != null && code.trim().startsWith("FVP1-")) {
         JsonObject body;
         try {
            byte[] packed = Base64.getUrlDecoder().decode(code.trim().substring("FVP1-".length()));
            Inflater inflater = new Inflater();
            inflater.setInput(packed);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];

            int n;
            while (!inflater.finished() && ((n = inflater.inflate(buf)) != 0 || !inflater.needsInput() && !inflater.needsDictionary())) {
               out.write(buf, 0, n);
               if (out.size() > 4194304) {
                  break;
               }
            }

            inflater.end();
            JsonElement root = new JsonParser().parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
            body = root.getAsJsonObject();
            if (!body.has("config") || !body.get("config").isJsonObject()) {
               return "!That code is damaged.";
            }
         } catch (IllegalArgumentException | IllegalStateException | DataFormatException | JsonParseException var9) {
            return "!That code is damaged.";
         }

         String wanted = body.has("name") && body.get("name").isJsonPrimitive() ? body.get("name").getAsString() : "Imported";
         return this.addImported(wanted, body.getAsJsonObject("config"));
      } else {
         return "!The clipboard does not hold a preset code (they start with FVP2-).";
      }
   }

   private String addImported(String name, JsonObject config) {
      String wanted = TextSetting.clean(name, 32).trim();
      if (this.checkName(wanted, null) != null && this.find(wanted) == null) {
         wanted = "Imported";
      }

      String n = this.freeName(wanted);
      String error = this.write(n, this.withoutPersonal(config), System.currentTimeMillis());
      this.refresh();
      return error != null ? "!" + error : n;
   }

   private String freeName(String name) {
      if (this.find(name) == null && this.checkName(name, null) == null) {
         return name;
      } else {
         for (int i = 2; i < 1000; i++) {
            String suffix = " (" + i + ")";
            String base = name.length() + suffix.length() > 32 ? name.substring(0, 32 - suffix.length()).trim() : name;
            String n = base + suffix;
            if (this.find(n) == null && this.checkName(n, null) == null) {
               return n;
            }
         }

         return name + " " + System.currentTimeMillis();
      }
   }

   private JsonObject withoutPersonal(JsonObject cfg) {
      JsonObject copy = new JsonParser().parse(cfg.toString()).getAsJsonObject();
      if (copy.has("modules") && copy.get("modules").isJsonObject()) {
         for (Entry e : copy.getAsJsonObject("modules").entrySet()) {
            Module m = this.modules.get((String)e.getKey());
            if (m != null && ((JsonElement)e.getValue()).isJsonObject()) {
               for (Setting s : m.getSettings()) {
                  if (s.isPersonal()) {
                     ((JsonElement)e.getValue()).getAsJsonObject().remove(s.getId());
                  }
               }
            }
         }
      }

      return copy;
   }

   private File fileFor(String name) {
      return new File(this.dir, name + ".json");
   }

   private String write(String name, JsonObject cfg) {
      return this.write(name, cfg, System.currentTimeMillis());
   }

   private String write(String name, JsonObject cfg, long saved) {
      JsonObject root = new JsonObject();
      root.addProperty("format", 1);
      root.addProperty("name", name);
      root.addProperty("saved", saved);
      root.addProperty("configVersion", ConfigManager.version());
      root.add("config", cfg);

      try {
         if (!this.dir.isDirectory() && !this.dir.mkdirs()) {
            throw new IOException("cannot create " + this.dir);
         } else {
            File target = this.fileFor(name);
            File tmp = new File(this.dir, name + ".json.tmp");
            Files.write(tmp.toPath(), this.gson.toJson(root).getBytes(StandardCharsets.UTF_8));

            try {
               Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException var9) {
               Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            return null;
         }
      } catch (IOException var10) {
         FuriahsVisuals.LOG.error("Could not save preset {}", new Object[]{name, var10});
         return "Could not save the preset file.";
      }
   }

   public interface Listener {
      void presetLoaded(String var1, String var2);
   }

   public static final class Preset {
      public final String name;
      public final File file;
      public final long saved;
      final JsonObject config;
      public final String error;

      Preset(String name, File file, long saved, JsonObject config, String error) {
         this.name = name;
         this.file = file;
         this.saved = saved;
         this.config = config;
         this.error = error;
      }

      public boolean isBroken() {
         return this.config == null;
      }

      public JsonObject settings() {
         return this.config == null ? null : new JsonParser().parse(this.config.toString()).getAsJsonObject();
      }
   }
}
