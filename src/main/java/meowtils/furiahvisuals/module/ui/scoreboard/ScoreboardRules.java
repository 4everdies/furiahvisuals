package meowtils.furiahvisuals.module.ui.scoreboard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import meowtils.furiahvisuals.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.EnumChatFormatting;

public final class ScoreboardRules extends Setting {
   public static final String ALL = "*";
   private static final Pattern NUMBERS = Pattern.compile("[0-9]+");
   private static final Pattern SPACES = Pattern.compile("\\s+");
   private static final Pattern ADDRESS = Pattern.compile(
      "(?i)^[^a-z0-9]*([a-z0-9-]+\\.)+(net|com|org|gg|io|me|co|eu|us|uk|de|fr|pl|ru|br|nl|es|it|xyz|club|tv|cc|pro|fun|world|network|games?|live|land|host|mc|online|cloud|biz|info|to)[^a-z0-9]*$"
   );
   private final Map<String, LinkedHashSet<String>> rules = new LinkedHashMap<>();

   public ScoreboardRules(String id) {
      super(id, "Hidden Lines", "The scoreboard lines you hid.");
   }

   public static String normalize(String text) {
      String plain = EnumChatFormatting.getTextWithoutFormattingCodes(text == null ? "" : text);
      if (plain == null) {
         plain = "";
      }

      plain = NUMBERS.matcher(plain).replaceAll("#");
      return SPACES.matcher(plain).replaceAll(" ").trim();
   }

   public static boolean isAddress(String normalized) {
      return normalized.length() >= 4 && normalized.length() <= 48 && ADDRESS.matcher(normalized).matches();
   }

   public static String scope(String title) {
      Minecraft mc = Minecraft.getMinecraft();
      ServerData server = mc.getCurrentServerData();
      String host = server != null && server.serverIP != null ? server.serverIP.toLowerCase(Locale.ROOT).trim() : "singleplayer";
      int colon = host.lastIndexOf(58);
      if (colon > 0 && host.indexOf(58) == colon) {
         host = host.substring(0, colon);
      }

      return host + "|" + normalize(title).toUpperCase(Locale.ROOT);
   }

   public boolean hidden(String scope, String normalized) {
      Set all = this.rules.get("*");
      if (all != null && all.contains(normalized)) {
         return true;
      } else {
         Set here = this.rules.get(scope);
         return here != null && here.contains(normalized);
      }
   }

   public void set(String scope, String normalized, boolean hide) {
      if (hide) {
         LinkedHashSet<String> s = this.rules.get(scope);
         if (s == null) {
            s = new LinkedHashSet<>();
            this.rules.put(scope, s);
         }

         if (s.add(normalized)) {
            this.fireChanged();
         }
      } else {
         boolean changed = false;

         for (Set set : this.rules.values()) {
            changed |= set.remove(normalized);
         }

         if (changed) {
            this.fireChanged();
         }
      }
   }

   public int count(String scope) {
      Set all = this.rules.get("*");
      Set here = this.rules.get(scope);
      return (all == null ? 0 : all.size()) + (here != null && !"*".equals(scope) ? here.size() : 0);
   }

   @Override
   public boolean showInMenu() {
      return false;
   }

   @Override
   public void reset() {
      if (!this.rules.isEmpty()) {
         this.rules.clear();
         this.fireChanged();
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject o = new JsonObject();

      for (Entry<String, LinkedHashSet<String>> e : this.rules.entrySet()) {
         if (!e.getValue().isEmpty()) {
            JsonArray a = new JsonArray();

            for (String s : e.getValue()) {
               a.add(new JsonPrimitive(s));
            }

            o.add(e.getKey(), a);
         }
      }

      return o;
   }

   @Override
   public void fromJson(JsonElement json) {
      this.rules.clear();
      if (json != null && json.isJsonObject()) {
         for (Entry e : json.getAsJsonObject().entrySet()) {
            if (((JsonElement)e.getValue()).isJsonArray()) {
               LinkedHashSet<String> s = new LinkedHashSet<>();

               for (JsonElement v : ((JsonElement)e.getValue()).getAsJsonArray()) {
                  if (v.isJsonPrimitive()) {
                     s.add(v.getAsString());
                  }
               }

               if (!s.isEmpty()) {
                  this.rules.put((String)e.getKey(), s);
               }
            }
         }
      }
   }
}
