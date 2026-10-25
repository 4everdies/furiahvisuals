package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.imageio.ImageIO;

final class PixelModel {
   static final int BLADE = 0;
   static final int EDGE = 1;
   static final int METAL = 2;
   static final int HANDLE = 3;
   static final int ACCENT = 4;
   static final int ENERGY = 5;
   static final int PAINT = 6;
   static final int CHAIN = 7;
   static final String[] MATERIAL_NAMES = new String[]{"blade", "edge", "metal", "handle", "accent", "energy", "paint", "chain"};
   static final int MAX_FIXED = 16;
   static final int MATERIALS = MATERIAL_NAMES.length + 16;
   static final int SLOTS = 1 + MATERIALS * 5;
   static final int MAX_SIDE = 1024;
   static final long MAX_PIXELS = 1048576L;
   private static final int MAX_FILE = 4194304;
   float pixel = 0.03125F;
   final List<PixelModel.Layer> layers = new ArrayList<>();
   final PixelModel.Fixed[] fixed = new PixelModel.Fixed[16];
   byte[] icon;
   byte[] icon16;
   Map<String, Float> stretch = Collections.emptyMap();
   float[] arcs;

   static int slot(int material, int shade) {
      return 1 + material * 5 + shade;
   }

   static int material(int slot) {
      return (slot - 1) / 5;
   }

   static int shade(int slot) {
      return (slot - 1) % 5;
   }

   static PixelModel read(WeaponDef def) throws IOException {
      byte[] json = bytes(def, "pixel/pixel.json", 262144);
      if (json == null) {
         return null;
      } else {
         JsonObject o;
         try {
            JsonElement e = new JsonParser().parse(new String(json, StandardCharsets.UTF_8));
            if (!e.isJsonObject()) {
               throw new IOException("pixel/pixel.json is not a JSON object");
            }

            o = e.getAsJsonObject();
         } catch (RuntimeException var19) {
            throw new IOException("pixel/pixel.json is not valid JSON: " + var19.getMessage());
         }

         PixelModel m = new PixelModel();
         m.pixel = (float)WeaponDef.num(o, "pixel", 0.03125);
         if (m.pixel > 1.0E-5F && m.pixel < 10000.0F) {
            int[] map = new int[256];
            Arrays.fill(map, -1);
            map[0] = 0;
            if (o.has("materials") && o.get("materials").isJsonObject()) {
               JsonObject mats = o.getAsJsonObject("materials");

               for (int mi = 0; mi < MATERIAL_NAMES.length; mi++) {
                  if (mats.has(MATERIAL_NAMES[mi])) {
                     mapRamp(map, mats.get(MATERIAL_NAMES[mi]), mi, MATERIAL_NAMES[mi]);
                  }
               }

               for (Entry e : mats.entrySet()) {
                  if (indexOf(MATERIAL_NAMES, (String)e.getKey()) < 0) {
                     throw new IOException("pixel/pixel.json: unknown material \"" + (String)e.getKey() + "\"");
                  }
               }
            }

            if (o.has("fixed") && o.get("fixed").isJsonArray()) {
               JsonArray fx = o.getAsJsonArray("fixed");
               if (fx.size() > 16) {
                  throw new IOException("pixel/pixel.json: more than 16 fixed colors");
               }

               for (int i = 0; i < fx.size(); i++) {
                  if (fx.get(i).isJsonObject()) {
                     JsonObject f = fx.get(i).getAsJsonObject();
                     PixelModel.Fixed x = new PixelModel.Fixed();
                     x.name = WeaponDef.str(f, "name", "fixed " + (i + 1));
                     x.color = color(WeaponDef.str(f, "color", "#808080"));
                     x.glow = f.has("glow") && f.get("glow").isJsonPrimitive() && f.get("glow").getAsBoolean();
                     x.alpha = (float)Math.max(0.05, Math.min(1.0, WeaponDef.num(f, "alpha", 1.0)));
                     if (f.has("shades")) {
                        JsonElement se = f.get("shades");
                        if (!se.isJsonArray() || se.getAsJsonArray().size() != 5) {
                           throw new IOException("pixel/pixel.json: the shades of \"" + x.name + "\" must be five colors (darkest first)");
                        }

                        x.shades = new int[5];

                        for (int s = 0; s < 5; s++) {
                           x.shades[s] = color(se.getAsJsonArray().get(s).getAsString());
                        }
                     }

                     m.fixed[i] = x;
                     if (f.has("slots")) {
                        mapRamp(map, f.get("slots"), MATERIAL_NAMES.length + i, x.name);
                     }
                  }
               }
            }

            if (o.has("layers") && o.get("layers").isJsonArray()) {
               long pixels = 0L;

               for (JsonElement le : o.getAsJsonArray("layers")) {
                  if (le.isJsonObject()) {
                     JsonObject lo = le.getAsJsonObject();
                     PixelModel.Layer l = new PixelModel.Layer();
                     l.part = WeaponDef.str(lo, "part", null);
                     String sprite = WeaponDef.str(lo, "sprite", null);
                     if (l.part == null || sprite == null) {
                        throw new IOException("pixel/pixel.json: a layer needs a \"part\" and a \"sprite\"");
                     }

                     String plane = WeaponDef.str(lo, "plane", "front").toLowerCase(Locale.ROOT);
                     l.plane = plane.equals("side") ? PixelModel.Plane.SIDE : (plane.equals("top") ? PixelModel.Plane.TOP : PixelModel.Plane.FRONT);
                     float[] at = WeaponDef.floats(lo, "at", 3);
                     if (at == null) {
                        at = new float[3];
                     }

                     l.x = half(at[0], sprite);
                     l.y = half(at[1], sprite);
                     l.z = half(at[2], sprite);
                     l.depth = (int)Math.max(1.0, Math.min(32.0, WeaponDef.num(lo, "depth", 1.0)));
                     int[] dims = new int[2];
                     int[] idx = indexed(def, "pixel/" + sprite, dims, true);
                     l.w = dims[0];
                     l.h = dims[1];
                     if ((pixels += (long)l.w * l.h) > 1048576L) {
                        throw new IOException("the pixel sprites are too big together");
                     }

                     l.slot = new byte[l.w * l.h];

                     for (int ix = 0; ix < idx.length; ix++) {
                        int s = map[idx[ix] & 0xFF];
                        if (s < 0) {
                           throw new IOException("pixel/" + sprite + ": palette slot " + idx[ix] + " belongs to no material in pixel.json");
                        }

                        l.slot[ix] = (byte)s;
                     }

                     String depthMap = WeaponDef.str(lo, "depthMap", null);
                     if (depthMap != null) {
                        int[] d = indexed(def, "pixel/" + depthMap, dims, false);
                        if (dims[0] != l.w || dims[1] != l.h) {
                           throw new IOException("pixel/" + depthMap + " is not the size of " + sprite);
                        }

                        l.depths = new byte[d.length];

                        for (int ix = 0; ix < d.length; ix++) {
                           l.depths[ix] = (byte)Math.max(0, Math.min(32, d[ix]));
                        }
                     }

                     m.layers.add(l);
                  }
               }

               if (m.layers.isEmpty()) {
                  throw new IOException("pixel/pixel.json has no layers");
               } else {
                  if (o.has("stretch") && o.get("stretch").isJsonObject()) {
                     HashMap<String, Float> st = new HashMap<>();

                     for (Entry ex : o.getAsJsonObject("stretch").entrySet()) {
                        if (!((JsonElement)ex.getValue()).isJsonPrimitive() || !((JsonElement)ex.getValue()).getAsJsonPrimitive().isNumber()) {
                           throw new IOException("pixel/pixel.json: the stretch of \"" + (String)ex.getKey() + "\" is not a number");
                        }

                        float fx = ((JsonElement)ex.getValue()).getAsFloat();
                        if (!(fx > 0.001F) || !(fx <= 100.0F)) {
                           throw new IOException("pixel/pixel.json: the stretch of \"" + (String)ex.getKey() + "\" must be between 0.001 and 100");
                        }

                        st.put((String)ex.getKey(), fx);
                     }

                     m.stretch = st;
                  }

                  if (def.arcs != null && o.has("arcs") && o.get("arcs").isJsonObject()) {
                     JsonObject ao = o.getAsJsonObject("arcs");
                     float[] from = WeaponDef.floats(ao, "from", 3);
                     float[] to = WeaponDef.floats(ao, "to", 3);
                     if (from == null || to == null) {
                        throw new IOException("pixel/pixel.json: \"arcs\" needs \"from\" and \"to\" (3 numbers each)");
                     }

                     m.arcs = new float[8];

                     for (int ix = 0; ix < 3; ix++) {
                        if (!(Math.abs(from[ix]) <= 4096.0F) || !(Math.abs(to[ix]) <= 4096.0F)) {
                           throw new IOException("pixel/pixel.json: the arcs' ends must be within 4096 pixels");
                        }

                        m.arcs[ix] = from[ix] * m.pixel;
                        m.arcs[3 + ix] = to[ix] * m.pixel;
                     }

                     float width = (float)WeaponDef.num(ao, "width", def.arcs[6] / m.pixel);
                     if (!(width >= 0.01F) || !(width <= 4.0F)) {
                        throw new IOException("pixel/pixel.json: the arcs' width must be between 0.01 and 4 pixels");
                     }

                     m.arcs[6] = width * m.pixel;
                     m.arcs[7] = def.arcs[7];
                  }

                  m.icon = icon(def, WeaponDef.str(o, "icon", null), 32, map);
                  m.icon16 = icon(def, WeaponDef.str(o, "icon16", null), 16, map);
                  return m;
               }
            } else {
               throw new IOException("pixel/pixel.json has no \"layers\"");
            }
         } else {
            throw new IOException("pixel/pixel.json: \"pixel\" must be between 0.00001 and 10000 model units");
         }
      }
   }

   private static float half(float v, String sprite) throws IOException {
      float h = v * 2.0F;
      if (!(Math.abs(h - Math.round(h)) > 0.001F) && !(Math.abs(v) > 4096.0F)) {
         return Math.round(h) / 2.0F;
      } else {
         throw new IOException("pixel/pixel.json: " + sprite + " is placed off the half-pixel grid (" + v + ")");
      }
   }

   private static byte[] icon(WeaponDef def, String name, int size, int[] map) throws IOException {
      if (name == null) {
         return null;
      } else {
         int[] dims = new int[2];
         int[] idx = indexed(def, "pixel/" + name, dims, true);
         if (dims[0] == size && dims[1] == size) {
            byte[] out = new byte[idx.length];

            for (int i = 0; i < idx.length; i++) {
               int s = map[idx[i] & 0xFF];
               out[i] = (byte)Math.max(0, s);
            }

            return out;
         } else {
            throw new IOException("pixel/" + name + " must be " + size + " x " + size + " pixels");
         }
      }
   }

   private static void mapRamp(int[] map, JsonElement e, int material, String what) throws IOException {
      if (e != null && e.isJsonArray() && e.getAsJsonArray().size() != 0 && e.getAsJsonArray().size() <= 5) {
         JsonArray a = e.getAsJsonArray();

         for (int k = 0; k < a.size(); k++) {
            int s = a.get(k).getAsInt();
            if (s < 1 || s > 255) {
               throw new IOException("pixel/pixel.json: slot " + s + " of \"" + what + "\" is not 1 to 255");
            }

            if (map[s] >= 0) {
               throw new IOException("pixel/pixel.json: slot " + s + " is in two materials");
            }

            int shade = a.size() == 1 ? 2 : Math.round(k * 4.0F / (a.size() - 1));
            map[s] = slot(material, shade);
         }
      } else {
         throw new IOException("pixel/pixel.json: \"" + what + "\" needs 1 to 5 slots (darkest first)");
      }
   }

   private static int indexOf(String[] a, String s) {
      for (int i = 0; i < a.length; i++) {
         if (a[i].equals(s)) {
            return i;
         }
      }

      return -1;
   }

   static int color(String hex) throws IOException {
      try {
         String h = hex.startsWith("#") ? hex.substring(1) : hex;
         return Integer.parseInt(h, 16) & 16777215;
      } catch (NumberFormatException var2) {
         throw new IOException("pixel/pixel.json: \"" + hex + "\" is not a color like #E8DCC0");
      }
   }

   static int[] indexed(WeaponDef def, String name, int[] dims, boolean palette) throws IOException {
      byte[] b = bytes(def, name, 4194304);
      if (b == null) {
         throw new IOException(name + " is missing");
      } else {
         return indexed(b, name, dims, palette);
      }
   }

   static int[] indexed(byte[] b, String name, int[] dims, boolean palette) throws IOException {
      if (b.length >= 33 && (b[0] & 255) == 137 && b[1] == 80 && b[2] == 78 && b[3] == 71 && b[12] == 73 && b[13] == 72) {
         int w = (b[16] & 255) << 24 | (b[17] & 255) << 16 | (b[18] & 255) << 8 | b[19] & 255;
         int h = (b[20] & 255) << 24 | (b[21] & 255) << 16 | (b[22] & 255) << 8 | b[23] & 255;
         int type = b[25] & 255;
         if (w > 0 && h > 0 && w <= 1024 && h <= 1024) {
            if (type == 3 || !palette && type == 0) {
               BufferedImage img;
               try {
                  img = ImageIO.read(new ByteArrayInputStream(b));
               } catch (RuntimeException | IOException var12) {
                  throw new IOException(name + " could not be read: " + var12.getMessage());
               }

               if (img != null && img.getWidth() == w && img.getHeight() == h) {
                  if (type == 3 && !(img.getColorModel() instanceof IndexColorModel)) {
                     throw new IOException(name + " lost its palette while loading");
                  } else {
                     WritableRaster r = img.getRaster();
                     int[] out = new int[w * h];

                     for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                           out[y * w + x] = r.getSample(x, y, 0);
                        }
                     }

                     dims[0] = w;
                     dims[1] = h;
                     return out;
                  }
               } else {
                  throw new IOException(name + " could not be read");
               }
            } else {
               throw new IOException(
                  name
                     + " must be a palette PNG (each pixel a palette slot), not "
                     + (type == 0 ? "grey" : (type == 2 ? "RGB" : (type == 6 ? "RGBA" : "type " + type)))
               );
            }
         } else {
            throw new IOException(name + " is " + w + " x " + h + " pixels (at most " + 1024 + " on a side)");
         }
      } else {
         throw new IOException(name + " is not a PNG image");
      }
   }

   static byte[] bytes(WeaponDef def, String name, int max) throws IOException {
      InputStream in = def.open(name);
      if (in == null) {
         return null;
      } else {
         byte[] var8;
         try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];

            int n;
            while ((n = in.read(buf)) > 0) {
               out.write(buf, 0, n);
               if (out.size() > max) {
                  throw new IOException(name + " is too big");
               }
            }

            byte[] byArray = out.toByteArray();
            var8 = byArray;
         } finally {
            in.close();
         }

         return var8;
      }
   }

   static final class Fixed {
      String name;
      int color;
      boolean glow;
      float alpha = 1.0F;
      int[] shades;
   }

   static final class Layer {
      String part;
      PixelModel.Plane plane;
      float x;
      float y;
      float z;
      int w;
      int h;
      int depth = 1;
      byte[] slot;
      byte[] depths;

      int slot(int c, int r) {
         return c >= 0 && r >= 0 && c < this.w && r < this.h ? this.slot[r * this.w + c] & 0xFF : 0;
      }

      int depth(int c, int r) {
         int d = this.depths == null ? 0 : this.depths[r * this.w + c] & 255;
         return d > 0 ? d : this.depth;
      }
   }

   static enum Plane {
      FRONT,
      SIDE,
      TOP;
   }
}
