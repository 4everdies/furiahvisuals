package meowtils.furiahvisuals.module.ui.crosshair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.TextSetting;

final class CrosshairProfiles {
   static final String PREFIX = "FVX1:";
   private static final int MAX_JSON = 262144;
   private static final String ID_PREFIX = "crosshair.";
   private final CrosshairElement e;
   private final TextSetting name;
   private final CrosshairProfiles.Store store = new CrosshairProfiles.Store();
   private final List<String> names = new ArrayList<>();
   private final List<JsonObject> data = new ArrayList<>();
   private int active = -1;
   private final Map<String, JsonElement> defaults = new HashMap<>();
   private final Map<CrosshairDef.Kind, JsonObject> defDefaults = new HashMap<>();
   private String message;
   private boolean messageError;
   private long messageAt;
   private int checkedAt = -1;
   private boolean modified;
   private static final String NOW_CLASSIC = " (custom image crosshairs were removed: its image is the Classic crosshair now)";

   CrosshairProfiles(CrosshairElement e) {
      this.e = e;
      this.name = new TextSetting("crosshair.profileName", "Profile Name", "The name for Save (empty: \"Profile 1\", \"Profile 2\"...).", "", 24)
         .<TextSetting>notSaved()
         .keywords(new String[]{"profile", "name"});
   }

   TextSetting nameSetting() {
      return this.name;
   }

   Setting store() {
      return this.store;
   }

   void captureDefaults() {
      for (Setting s : this.e.profileSettings()) {
         this.defaults.put(s.getId(), s.toJson());
      }
   }

   JsonObject snapshot() {
      JsonObject o = new JsonObject();
      JsonObject set = new JsonObject();

      for (Setting s : this.e.profileSettings()) {
         JsonElement v = s.toJson();
         if (!v.equals(this.defaults.get(s.getId()))) {
            set.add(shortId(s.getId()), v);
         }
      }

      o.add("s", set);
      JsonObject defs = new JsonObject();

      for (int i = 0; i < CrosshairElement.COUNT; i++) {
         if (this.e.inUse(i)) {
            JsonObject all = this.e.defs[i].toJson();
            JsonObject def = this.defaultsOf(this.e.defs[i].kind);
            JsonObject diff = new JsonObject();

            for (Entry en : all.entrySet()) {
               if (!((JsonElement)en.getValue()).equals(def.get((String)en.getKey()))) {
                  diff.add((String)en.getKey(), (JsonElement)en.getValue());
               }
            }

            defs.add(CrosshairElement.key(i), diff);
         }
      }

      o.add("d", defs);
      return o;
   }

   private JsonObject defaultsOf(CrosshairDef.Kind kind) {
      JsonObject d = this.defDefaults.get(kind);
      if (d == null) {
         d = CrosshairDef.stored(kind).toJson();
         this.defDefaults.put(kind, d);
      }

      return d;
   }

   private static String shortId(String id) {
      return id.startsWith("crosshair.") ? id.substring("crosshair.".length()) : id;
   }

   boolean apply(JsonObject o) {
      boolean image = false;
      JsonObject set = o.has("s") && o.get("s").isJsonObject() ? o.getAsJsonObject("s") : new JsonObject();

      for (Setting s : this.e.profileSettings()) {
         JsonElement v = set.get(shortId(s.getId()));

         try {
            if (v != null) {
               s.fromJson(v);
            } else {
               s.reset();
            }
         } catch (RuntimeException var8) {
            s.reset();
         }
      }

      JsonObject defs = o.has("d") && o.get("d").isJsonObject() ? o.getAsJsonObject("d") : new JsonObject();

      for (int i = 0; i < CrosshairElement.COUNT; i++) {
         JsonElement d = defs.get(CrosshairElement.key(i));

         try {
            image |= this.e.defs[i].fromJson(d != null && d.isJsonObject() ? d.getAsJsonObject() : null);
         } catch (RuntimeException var9) {
            this.e.defs[i].reset();
         }
      }

      this.e.defsReplaced();
      if (image) {
         this.e.imageRemoved();
      }

      return image;
   }

   List<String> names() {
      return this.names;
   }

   int active() {
      return this.active;
   }

   boolean modified() {
      if (this.active < 0) {
         return false;
      } else {
         int changes = FuriahsVisuals.instance.getConfig().changeCount();
         if (changes != this.checkedAt) {
            this.checkedAt = changes;
            this.modified = !this.snapshot().equals(this.data.get(this.active));
         }

         return this.modified;
      }
   }

   String save(String wanted) {
      String n = wanted == null ? "" : wanted.trim();
      if (n.isEmpty()) {
         int k = this.names.size() + 1;

         while (this.names.contains("Profile " + k)) {
            k++;
         }

         n = "Profile " + k;
      }

      JsonObject snap = this.snapshot();
      int at = this.names.indexOf(n);
      if (at >= 0) {
         this.data.set(at, snap);
         this.active = at;
         this.say("Updated \"" + n + "\"", false);
      } else {
         this.names.add(n);
         this.data.add(snap);
         this.active = this.names.size() - 1;
         this.say("Saved \"" + n + "\"", false);
      }

      this.checkedAt = -1;
      this.name.set("");
      this.store.changed();
      return this.message;
   }

   void load(int index) {
      if (index >= 0 && index < this.names.size()) {
         boolean image = this.apply(this.data.get(index));
         if (image) {
            this.data.set(index, this.snapshot());
         }

         this.active = index;
         this.checkedAt = -1;
         this.say(
            "Loaded \"" + this.names.get(index) + "\"" + (image ? " (custom image crosshairs were removed: its image is the Classic crosshair now)" : ""),
            false
         );
         this.store.changed();
      }
   }

   void delete(int index) {
      if (index >= 0 && index < this.names.size()) {
         String n = this.names.remove(index);
         this.data.remove(index);
         if (this.active == index) {
            this.active = -1;
         } else if (this.active > index) {
            this.active--;
         }

         this.say("Deleted \"" + n + "\"", false);
         this.store.changed();
      }
   }

   String next() {
      if (this.names.isEmpty()) {
         return null;
      } else {
         this.load((this.active + 1) % this.names.size());
         return this.names.get(this.active);
      }
   }

   void tick() {
      if (this.message != null && ClientClock.millis() - this.messageAt > 8000L) {
         this.message = null;
      }
   }

   String message() {
      return this.message;
   }

   boolean messageIsError() {
      return this.messageError;
   }

   void sayCopied(int length) {
      this.say("Code copied (" + length + " characters): paste it anywhere to share", false);
   }

   private void say(String text, boolean error) {
      this.message = text;
      this.messageError = error;
      this.messageAt = ClientClock.millis();
   }

   String describe() {
      return this.names + (this.active >= 0 ? " active " + this.names.get(this.active) : " none active");
   }

   String loadOldForTest(String profileName, JsonObject setup) {
      int at = this.names.indexOf(profileName);
      if (at >= 0) {
         this.names.remove(at);
         this.data.remove(at);
      }

      this.names.add(profileName);
      this.data.add(setup);
      this.load(this.names.size() - 1);
      return this.message + (this.modified() ? ", counts as changed (FAIL)" : ", not changed since loaded (OK)");
   }

   String exportCode() {
      return encode(this.snapshot());
   }

   static String encode(JsonObject setup) {
      byte[] json = setup.toString().getBytes(StandardCharsets.UTF_8);
      Deflater d = new Deflater(9);
      d.setInput(json);
      d.finish();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[1024];

      while (!d.finished()) {
         out.write(buf, 0, d.deflate(buf));
      }

      d.end();
      return "FVX1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
   }

   String importCode(String text, boolean unused) {
      String code = text == null ? "" : text.replaceAll("\\s+", "");
      if (!code.regionMatches(true, 0, "FVX1:", 0, "FVX1:".length())) {
         this.say(code.isEmpty() ? "The clipboard is empty: copy a crosshair code first" : "That is not a crosshair code (they start with FVX1:)", true);
         return this.message;
      } else {
         code = code.substring("FVX1:".length());

         JsonObject o;
         try {
            byte[] packed = Base64.getUrlDecoder().decode(code);
            Inflater inf = new Inflater();
            inf.setInput(packed);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];

            int n;
            while (!inf.finished() && ((n = inf.inflate(buf)) != 0 || !inf.needsInput() && !inf.needsDictionary())) {
               out.write(buf, 0, n);
               if (out.size() > 262144) {
                  throw new DataFormatException("too long");
               }
            }

            inf.end();
            JsonElement root = new JsonParser().parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
            if (!root.isJsonObject() || !root.getAsJsonObject().has("d")) {
               throw new DataFormatException("not a crosshair setup");
            }

            o = root.getAsJsonObject();
         } catch (IllegalArgumentException | IllegalStateException | DataFormatException | JsonParseException var12) {
            this.say("That code is broken or cut short (copy all of it)", true);
            return this.message;
         }

         boolean image = this.apply(o);
         this.active = -1;
         this.checkedAt = -1;
         this.say(
            image
               ? "Code loaded (custom image crosshairs were removed: its image is the Classic crosshair now): Save keeps it as a profile"
               : "Code loaded: Save keeps it as a profile",
            false
         );
         this.store.changed();
         return this.message;
      }
   }

   private final class Store extends Setting {
      Store() {
         super("crosshair.profiles", "Crosshair Profiles", "Your crosshair profiles.");
      }

      void changed() {
         this.fireChanged();
      }

      @Override
      public boolean showInMenu() {
         return false;
      }

      @Override
      public void reset() {
         CrosshairProfiles.this.names.clear();
         CrosshairProfiles.this.data.clear();
         CrosshairProfiles.this.active = -1;
         this.fireChanged();
      }

      @Override
      public JsonElement toJson() {
         JsonObject o = new JsonObject();
         o.addProperty("active", CrosshairProfiles.this.active);
         JsonArray list = new JsonArray();

         for (int i = 0; i < CrosshairProfiles.this.names.size(); i++) {
            JsonObject p = new JsonObject();
            p.addProperty("name", CrosshairProfiles.this.names.get(i));
            p.add("data", (JsonElement)CrosshairProfiles.this.data.get(i));
            list.add(p);
         }

         o.add("list", list);
         return o;
      }

      @Override
      public void fromJson(JsonElement json) {
         CrosshairProfiles.this.names.clear();
         CrosshairProfiles.this.data.clear();
         CrosshairProfiles.this.active = -1;
         if (json != null && json.isJsonObject()) {
            JsonObject o = json.getAsJsonObject();

            try {
               if (o.has("list") && o.get("list").isJsonArray()) {
                  for (JsonElement el : o.getAsJsonArray("list")) {
                     String n;
                     if (el.isJsonObject()
                        && el.getAsJsonObject().has("name")
                        && el.getAsJsonObject().has("data")
                        && el.getAsJsonObject().get("data").isJsonObject()
                        && !(n = el.getAsJsonObject().get("name").getAsString().trim()).isEmpty()
                        && !CrosshairProfiles.this.names.contains(n)) {
                        CrosshairProfiles.this.names.add(n);
                        CrosshairProfiles.this.data.add(el.getAsJsonObject().getAsJsonObject("data"));
                     }
                  }
               }

               int a = o.has("active") ? o.get("active").getAsInt() : -1;
               CrosshairProfiles.this.active = a >= 0 && a < CrosshairProfiles.this.names.size() ? a : -1;
            } catch (RuntimeException var6) {
            }

            CrosshairProfiles.this.checkedAt = -1;
         }
      }
   }
}
