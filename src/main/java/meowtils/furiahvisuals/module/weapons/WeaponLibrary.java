package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.core.FuriahsVisuals;

final class WeaponLibrary {
   static final String ROOT = "weapons/";
   static final String IMPORT_PREFIX = "import:";
   private final Map<String, WeaponDef> bundled = new LinkedHashMap<>();
   private final Map<String, WeaponDef> imported = new LinkedHashMap<>();
   private final WeaponDef vanilla = WeaponDef.vanilla();
   private final File importFolder;
   private final Map<String, String> importProblems = new LinkedHashMap<>();
   private int version;

   WeaponLibrary(File importFolder) {
      this.importFolder = importFolder;
      this.loadBundled();
      this.reloadImported();
   }

   private void loadBundled() {
      JsonObject index = readJson("weapons/index.json");
      if (index != null && index.has("weapons") && index.get("weapons").isJsonArray()) {
         for (JsonElement e : index.getAsJsonArray("weapons")) {
            String id = e.getAsString();
            JsonObject json = readJson("weapons/" + id + "/weapon.json");
            if (json == null) {
               FuriahsVisuals.LOG.warn("Weapon {} has no weapon.json in the mod's files", new Object[]{id});
            } else {
               try {
                  WeaponDef def = WeaponDef.bundled(json, "weapons/" + id + "/");
                  this.bundled.put(def.id, def);
               } catch (RuntimeException var7) {
                  FuriahsVisuals.LOG.warn("Weapon {} has an invalid weapon.json", new Object[]{id, var7});
               }
            }
         }
      } else {
         if (AssetSource.ready()) {
            FuriahsVisuals.LOG.warn("The weapon index is missing from the mod's files; only Vanilla is available");
         }
      }
   }

   void reloadBundled() {
      this.bundled.clear();
      this.loadBundled();
      this.version++;
   }

   void reloadImported() {
      this.imported.clear();
      this.importProblems.clear();
      File[] dirs = this.importFolder.listFiles();
      if (dirs != null) {
         Arrays.sort(dirs, new Comparator<File>() {
            public int compare(File a, File b) {
               return a.getName().compareToIgnoreCase(b.getName());
            }
         });

         for (File dir : dirs) {
            File json;
            if (dir.isDirectory() && (json = new File(dir, "weapon.json")).isFile()) {
               try (InputStreamReader r = new InputStreamReader(new FileInputStream(json), StandardCharsets.UTF_8)) {
                  JsonObject o = new JsonParser().parse(r).getAsJsonObject();
                  o.addProperty("id", importId(dir));
                  WeaponDef def = WeaponDef.imported(o, dir);
                  this.imported.put(def.id, def);
               } catch (RuntimeException | IOException var21) {
                  this.importProblems.put(dir.getName(), "its weapon.json can't be read (" + var21.getMessage() + ")");
                  FuriahsVisuals.LOG.warn("Imported weapon {} not loaded: {}", new Object[]{dir.getName(), var21.toString()});
               }
            }
         }
      }

      WeaponAssets.dropChanged(this.imported);
      this.version++;
   }

   void replaceImported(WeaponDef def) {
      if (this.imported.containsKey(def.id)) {
         this.imported.put(def.id, def);
      }
   }

   static String importId(File dir) {
      return "import:" + dir.getName().toLowerCase(Locale.ROOT);
   }

   File importFolder() {
      return this.importFolder;
   }

   Map<String, String> importProblems() {
      return Collections.unmodifiableMap(this.importProblems);
   }

   int version() {
      return this.version;
   }

   WeaponDef vanilla() {
      return this.vanilla;
   }

   WeaponDef get(String id) {
      if (id == null) {
         return null;
      } else if ("vanilla".equals(id)) {
         return this.vanilla;
      } else {
         WeaponDef d = this.bundled.get(id);
         return d != null ? d : this.imported.get(id);
      }
   }

   List<WeaponDef> all() {
      ArrayList<WeaponDef> out = new ArrayList<>(1 + this.bundled.size() + this.imported.size());
      out.add(this.vanilla);
      out.addAll(this.bundled.values());
      out.addAll(this.imported.values());
      return out;
   }

   List<WeaponDef> bundled() {
      return new ArrayList<>(this.bundled.values());
   }

   List<WeaponDef> imported() {
      return new ArrayList<>(this.imported.values());
   }

   static JsonObject readJson(String resource) {
      InputStream in = AssetSource.open(resource);
      if (in == null) {
         return null;
      } else {
         JsonObject jsonObject;
         try {
            InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8);
            JsonElement e = new JsonParser().parse(r);
            return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
         } catch (RuntimeException var15) {
            FuriahsVisuals.LOG.warn("Could not read {}", new Object[]{resource, var15});
            JsonObject jsonObjectx = null;
            jsonObject = jsonObjectx;
         } finally {
            try {
               in.close();
            } catch (IOException var14) {
            }
         }

         return jsonObject;
      }
   }
}
