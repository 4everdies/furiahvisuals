package meowtils.furiahvisuals.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Choices;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.EnumChoices;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.PresetSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.TextSetting;

public final class WebValues {
   public static final String TOGGLE = "toggle";
   public static final String SLIDER = "slider";
   public static final String CHOICE = "choice";
   public static final String MULTI_CHOICE = "multiChoice";
   public static final String COLOR = "color";
   public static final String TEXT = "text";

   private WebValues() {
   }

   public static String type(Setting s) {
      if (s instanceof BooleanSetting) {
         return "toggle";
      } else if (s instanceof SliderSetting) {
         return "slider";
      } else if (s instanceof ModeSetting) {
         return "choice";
      } else if (s instanceof ColorSetting) {
         return "color";
      } else if (s instanceof EnumChoices) {
         return "multiChoice";
      } else if (s instanceof TextSetting) {
         return "text";
      } else if (!(s instanceof PresetSetting) && !(s instanceof Choices)) {
         String name = s.getClass().getSimpleName();
         if (name.isEmpty()) {
            name = s.getClass().getSuperclass().getSimpleName();
         }

         name = name.endsWith("Setting") && name.length() > 7 ? name.substring(0, name.length() - 7) : name;
         return Character.toLowerCase(name.charAt(0)) + name.substring(1);
      } else {
         return "choice";
      }
   }

   public static String id(String name) {
      return name.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
   }

   public static JsonElement toWeb(Setting s, JsonElement mod) {
      if (mod != null && !mod.isJsonNull()) {
         try {
            if (s instanceof BooleanSetting) {
               return new JsonPrimitive(mod.getAsBoolean());
            }

            if (s instanceof SliderSetting) {
               return new JsonPrimitive(number(mod.getAsDouble()));
            }

            if (s instanceof ModeSetting) {
               Enum<?> c = constant((ModeSetting<?>)s, mod.getAsString());
               return c == null ? null : new JsonPrimitive(id(c.name()));
            }

            if (s instanceof ColorSetting) {
               return color(mod.getAsJsonObject());
            }

            if (s instanceof EnumChoices) {
               JsonArray out = new JsonArray();

               for (JsonElement e : mod.getAsJsonArray()) {
                  Enum<?> c = constant(((EnumChoices)s).choices(), e.getAsString());
                  if (c != null) {
                     out.add(new JsonPrimitive(id(c.name())));
                  }
               }

               return out;
            }

            if (s instanceof TextSetting) {
               return new JsonPrimitive(mod.getAsString());
            }

            if (s instanceof PresetSetting) {
               String selected = mod.getAsJsonObject().has("selected") ? mod.getAsJsonObject().get("selected").getAsString() : "";

               for (PresetSetting.Chip chip : builtIn((PresetSetting)s)) {
                  if (chip.id.equals(selected)) {
                     return new JsonPrimitive(id(chip.id));
                  }
               }

               return new JsonPrimitive("custom");
            }
         } catch (RuntimeException var6) {
            return null;
         }

         return copy(mod);
      } else {
         return null;
      }
   }

   public static List<PresetSetting.Chip> builtIn(PresetSetting s) {
      ArrayList<PresetSetting.Chip> out = new ArrayList<>();

      for (PresetSetting.Chip chip : s.chips()) {
         if (!chip.removable && !"custom".equals(chip.id)) {
            out.add(chip);
         }
      }

      return out;
   }

   private static JsonObject color(JsonObject o) {
      String mode = o.has("mode") ? o.get("mode").getAsString().toLowerCase(Locale.ROOT) : "static";
      String a = rgba(o.has("a") ? o.get("a").getAsString() : "#FFFFFFFF");
      String b = rgba(o.has("b") ? o.get("b").getAsString() : "#FFFFFFFF");
      double chroma = o.has("chromaSpeed") ? o.get("chromaSpeed").getAsDouble() : 1.0;
      double scroll = o.has("scrollSpeed") ? o.get("scrollSpeed").getAsDouble() : 0.0;
      JsonObject web = new JsonObject();
      web.addProperty("mode", mode);
      web.addProperty("color", a);
      JsonArray stops = new JsonArray();
      stops.add(new JsonPrimitive(a));
      stops.add(new JsonPrimitive(b));
      web.add("stops", stops);
      web.addProperty("speed", number("gradient".equals(mode) ? scroll : chroma));
      JsonObject extra = new JsonObject();
      extra.addProperty("chromaSpeed", number(chroma));
      extra.addProperty("scrollSpeed", number(scroll));
      extra.addProperty("saturation", number(o.has("chromaSaturation") ? o.get("chromaSaturation").getAsDouble() : 0.85));
      extra.addProperty("brightness", number(o.has("chromaBrightness") ? o.get("chromaBrightness").getAsDouble() : 1.0));
      extra.addProperty("spread", number(o.has("chromaSpread") ? o.get("chromaSpread").getAsDouble() : 0.25));
      web.add("extra", extra);
      return web;
   }

   public static JsonElement toMod(Setting s, JsonElement web) {
      if (web != null && !web.isJsonNull()) {
         try {
            if (s instanceof BooleanSetting) {
               return web.isJsonPrimitive() && web.getAsJsonPrimitive().isBoolean() ? new JsonPrimitive(web.getAsBoolean()) : null;
            }

            if (s instanceof SliderSetting) {
               if (web.isJsonPrimitive() && web.getAsJsonPrimitive().isNumber()) {
                  SliderSetting sl = (SliderSetting)s;
                  return new JsonPrimitive(Math.max(sl.getMin(), Math.min(sl.getMax(), web.getAsDouble())));
               }

               return null;
            }

            if (s instanceof ModeSetting) {
               Enum<?> c = constant((ModeSetting<?>)s, web.getAsString());
               return c == null ? null : new JsonPrimitive(c.name());
            }

            if (s instanceof ColorSetting) {
               return web.isJsonObject() ? modColor((ColorSetting)s, web.getAsJsonObject()) : null;
            }

            if (s instanceof EnumChoices) {
               JsonArray out = new JsonArray();

               for (JsonElement e : web.getAsJsonArray()) {
                  Enum<?> c = constant(((EnumChoices)s).choices(), e.getAsString());
                  if (c != null) {
                     out.add(new JsonPrimitive(c.name()));
                  }
               }

               return out;
            }

            if (s instanceof TextSetting) {
               return web.isJsonPrimitive() ? new JsonPrimitive(web.getAsString()) : null;
            }

            if (s instanceof Choices) {
               String value = web.getAsString();

               for (String[] c : ((Choices)s).choices()) {
                  if (c[0].equals(value)) {
                     return new JsonPrimitive(value);
                  }
               }

               return null;
            }

            if (s instanceof PresetSetting) {
               String value = web.getAsString();
               JsonObject o = new JsonObject();
               o.addProperty("selected", "custom");

               for (PresetSetting.Chip chip : builtIn((PresetSetting)s)) {
                  if (id(chip.id).equals(value) || chip.id.equals(value)) {
                     o.addProperty("selected", chip.id);
                  }
               }

               return o;
            }
         } catch (RuntimeException var6) {
            return null;
         }

         return copy(web);
      } else {
         return null;
      }
   }

   private static JsonObject modColor(ColorSetting s, JsonObject web) {
      String mode = web.has("mode") ? web.get("mode").getAsString().toUpperCase(Locale.ROOT) : "STATIC";
      ColorSetting.Mode m = null;

      for (ColorSetting.Mode each : ColorSetting.Mode.values()) {
         if (each.name().equals(mode)) {
            m = each;
         }
      }

      if (m != null && (m != ColorSetting.Mode.GLOBAL || s.allowsGlobal())) {
         JsonArray stops = web.has("stops") && web.get("stops").isJsonArray() ? web.getAsJsonArray("stops") : new JsonArray();
         String a = web.has("color") ? web.get("color").getAsString() : null;
         String b = stops.size() >= 2 ? stops.get(stops.size() - 1).getAsString() : null;
         if (m == ColorSetting.Mode.GRADIENT && stops.size() >= 2) {
            a = stops.get(0).getAsString();
         }

         JsonObject extra = web.has("extra") && web.get("extra").isJsonObject() ? web.getAsJsonObject("extra") : new JsonObject();
         JsonObject o = new JsonObject();
         o.addProperty("mode", m.name());
         if (a != null) {
            o.addProperty("a", argb(a));
         }

         if (b != null) {
            o.addProperty("b", argb(b));
         }

         Double speed = web.has("speed") && web.get("speed").isJsonPrimitive() ? web.get("speed").getAsDouble() : null;
         Double chroma = m == ColorSetting.Mode.CHROMA && speed != null ? speed : num(extra, "chromaSpeed");
         Double scroll = m == ColorSetting.Mode.GRADIENT && speed != null ? speed : num(extra, "scrollSpeed");
         if (chroma != null) {
            o.addProperty("chromaSpeed", chroma);
         }

         if (scroll != null) {
            o.addProperty("scrollSpeed", scroll);
         }

         Double saturation;
         if ((saturation = num(extra, "saturation")) != null) {
            o.addProperty("chromaSaturation", saturation);
            Double brightness = num(extra, "brightness");
            o.addProperty("chromaBrightness", brightness != null ? brightness : 1.0);
         }

         Double spread;
         if ((spread = num(extra, "spread")) != null) {
            o.addProperty("chromaSpread", spread);
         }

         return o;
      } else {
         return null;
      }
   }

   private static Enum<?> constant(ModeSetting<?> s, String value) {
      return constant(s.getValues(), value);
   }

   private static Enum<?> constant(Enum<?>[] constants, String value) {
      for (Enum<?> c : constants) {
         if (c.name().equals(value)) {
            return c;
         }
      }

      for (Enum<?> cx : constants) {
         if (id(cx.name()).equals(value) || cx.name().equalsIgnoreCase(value)) {
            return cx;
         }
      }

      return null;
   }

   private static Double num(JsonObject o, String key) {
      return o.has(key) && o.get(key).isJsonPrimitive() && o.get(key).getAsJsonPrimitive().isNumber() ? o.get(key).getAsDouble() : null;
   }

   public static String rgba(String argb) {
      int c = ColorUtil.parseHex(argb, -1);
      return String.format(Locale.ROOT, "#%06X%02X", c & 16777215, c >>> 24 & 0xFF);
   }

   public static String rgba(int c) {
      return String.format(Locale.ROOT, "#%06X%02X", c & 16777215, c >>> 24 & 0xFF);
   }

   private static String argb(String rgba) {
      String s = rgba.trim().startsWith("#") ? rgba.trim().substring(1) : rgba.trim();
      if (s.length() == 6) {
         s = s + "FF";
      }

      if (s.length() != 8) {
         throw new IllegalArgumentException(rgba);
      } else {
         long v = Long.parseLong(s, 16);
         int c = (int)((v & 255L) << 24 | v >>> 8);
         return ColorUtil.toHexArgb(c);
      }
   }

   public static double number(double v) {
      if (!Double.isNaN(v) && !Double.isInfinite(v)) {
         double f = (float)v;
         return f == v ? Double.parseDouble(Float.toString((float)v)) : new BigDecimal(Double.toString(v)).doubleValue();
      } else {
         return 0.0;
      }
   }

   private static JsonElement copy(JsonElement e) {
      return new JsonParser().parse(e.toString());
   }
}
