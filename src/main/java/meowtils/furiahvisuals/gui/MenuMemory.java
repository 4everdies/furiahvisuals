package meowtils.furiahvisuals.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import meowtils.furiahvisuals.setting.StringMapSetting;

public final class MenuMemory {
   public static final String SETTINGS = "#settings";
   public static final String PRESETS = "#presets";
   public static final String OVERVIEW = "#overview";
   private static StringMapSetting store;
   private static final Map<String, String> LOCAL = new HashMap<>();

   private MenuMemory() {
   }

   public static void attach(StringMapSetting setting) {
      store = setting;
   }

   private static String get(String key) {
      return store != null ? store.get(key) : LOCAL.get(key);
   }

   private static void put(String key, String value) {
      if (!value.equals(get(key))) {
         if (store != null) {
            store.put(key, value);
         } else {
            LOCAL.put(key, value);
         }
      }
   }

   static String page() {
      return get("page");
   }

   static void page(String key) {
      put("page", key);
   }

   public static void pageForTest(String key) {
      page(key);
   }

   static float scroll(String page) {
      String v = get("scroll." + page);
      if (v == null) {
         return -1.0F;
      } else {
         try {
            float f = Float.parseFloat(v);
            return f >= 0.0F && !Float.isInfinite(f) ? f : -1.0F;
         } catch (NumberFormatException var3) {
            return -1.0F;
         }
      }
   }

   static void scroll(String page, float units) {
      put("scroll." + page, String.format(Locale.ROOT, "%.1f", Math.max(0.0F, units)));
   }

   public static Boolean folded(String key) {
      String v = get("fold." + key);
      return v == null ? null : "1".equals(v);
   }

   public static void folded(String key, boolean folded) {
      put("fold." + key, folded ? "1" : "0");
   }

   static String subPage(String module) {
      return get("sub." + module);
   }

   static void subPage(String module, String page) {
      put("sub." + module, page);
   }

   static List<String> pins(String module) {
      String v = get("pin." + module);
      ArrayList<String> out = new ArrayList<>();
      if (v != null) {
         for (String id : v.split("\n")) {
            if (!id.isEmpty()) {
               out.add(id);
            }
         }
      }

      return out;
   }

   static void pins(String module, List<String> ids) {
      StringBuilder sb = new StringBuilder();

      for (String id : ids) {
         if (sb.length() > 0) {
            sb.append('\n');
         }

         sb.append(id);
      }

      put("pin." + module, sb.toString());
   }

   static boolean optionsOpen(String key) {
      return "1".equals(get("opt." + key));
   }

   static void optionsOpen(String key, boolean open) {
      put("opt." + key, open ? "1" : "0");
   }

   public static boolean rowOpen(String key) {
      return "1".equals(get("row." + key));
   }

   public static void rowOpen(String key, boolean open) {
      put("row." + key, open ? "1" : "0");
   }

   public static boolean opened(String module) {
      return get("opened." + module) != null || get("scroll." + module) != null;
   }

   static void markOpened(String module) {
      put("opened." + module, "1");
   }

   public static boolean previewDay(String module) {
      return "day".equals(get("backdrop." + module));
   }

   public static void previewDay(String module, boolean day) {
      put("backdrop." + module, day ? "day" : "dark");
   }

   public static boolean seen(String key) {
      return get("seen." + key) != null;
   }

   public static void markSeen(String key) {
      put("seen." + key, "1");
   }

   public static void forgetSeenForTest(String key) {
      if (store != null) {
         store.remove("seen." + key);
      } else {
         LOCAL.remove("seen." + key);
      }
   }

   static boolean hintDue(String module) {
      return get("hint." + module) == null && get("scroll." + module) == null;
   }

   static void hintDone(String module) {
      put("hint." + module, "1");
   }

   static boolean guideDone() {
      return "1".equals(get("guide"));
   }

   static void guideDone(boolean done) {
      put("guide", done ? "1" : "0");
   }

   static JsonElement snapshot() {
      if (store != null) {
         return store.toJson();
      } else {
         JsonObject o = new JsonObject();

         for (Entry<String, String> e : LOCAL.entrySet()) {
            o.addProperty(e.getKey(), e.getValue());
         }

         return o;
      }
   }

   static void restore(JsonElement saved) {
      if (saved != null) {
         if (store != null) {
            store.fromJson(saved);
         } else if (saved.isJsonObject()) {
            LOCAL.clear();

            for (Entry e : saved.getAsJsonObject().entrySet()) {
               LOCAL.put((String)e.getKey(), ((JsonElement)e.getValue()).getAsString());
            }
         }
      }
   }

   public static void forget() {
      LOCAL.clear();
      if (store != null) {
         store.reset();
      }
   }

   public static String describe() {
      return store != null ? store.toJson().toString() : LOCAL.toString();
   }
}
