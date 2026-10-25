package meowtils.furiahvisuals.module.weapons;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import javax.imageio.ImageIO;
import meowtils.furiahvisuals.image.ImageLoader;

final class PixelImport {
   static final int MAX_ELEMENTS = 4096;
   static final int MAX_TEXTURE = 1024;
   static final int MAX_VOX_SIDE = 256;
   static final int MAX_VOXELS = 200000;
   static final int MAX_GRID = 256;
   static final int MAX_COLORS = 80;
   static final int AUTO_COLORS = 32;
   static final String[] PART_NAMES = new String[]{"blade", "guard", "handle", "accent", "energy"};
   static final String BODY = "body";
   static final String MADE_FROM = "madeFrom";
   static final long MAX_FILE = 16777216L;
   static final String[] FACES = new String[]{"north", "east", "south", "west", "up", "down"};

   private PixelImport() {
   }

   static String partName(String name) {
      if (name == null) {
         return null;
      } else {
         String n = name.trim().toLowerCase(Locale.ROOT);

         for (String p : PART_NAMES) {
            if (n.equals(p)) {
               return p;
            }
         }

         return null;
      }
   }

   static PixelImport.Grid readVox(byte[] b, List<String> notes) throws PixelImport.ImportException {
      if (b.length >= 20 && b[0] == 86 && b[1] == 79 && b[2] == 88 && b[3] == 32) {
         int pos = 8;
         if (!"MAIN".equals(id(b, pos))) {
            throw new PixelImport.ImportException("it has no MAIN chunk");
         } else {
            int end = (int)Math.min((long)b.length, pos + 12L + int32(b, pos + 4) + int32(b, pos + 8));
            pos += 12 + int32(b, pos + 4);
            ArrayList<int[]> sizes = new ArrayList<>();
            ArrayList<byte[]> models = new ArrayList<>();
            int[] palette = null;
            HashMap<Integer, Object[]> nodes = new HashMap<>();
            int voxels = 0;

            while (pos + 12 <= end) {
               String id = id(b, pos);
               int n = int32(b, pos + 4);
               int m = int32(b, pos + 8);
               if (n < 0 || m < 0 || pos + 12L + n > end) {
                  throw new PixelImport.ImportException("a " + id + " chunk runs past the end of the file");
               }

               int c = pos + 12;
               if ("SIZE".equals(id) && n >= 12) {
                  int sx = int32(b, c);
                  int sy = int32(b, c + 4);
                  int sz = int32(b, c + 8);
                  if (sx <= 0 || sy <= 0 || sz <= 0 || sx > 256 || sy > 256 || sz > 256) {
                     throw new PixelImport.ImportException("a model is " + sx + " x " + sy + " x " + sz + " (at most " + 256 + " on a side)");
                  }

                  sizes.add(new int[]{sx, sy, sz});
               } else if ("XYZI".equals(id) && n >= 4) {
                  int count = int32(b, c);
                  if (count < 0 || 4L + 4L * count > n) {
                     throw new PixelImport.ImportException("a model's voxels run past its chunk");
                  }

                  if ((voxels += count) > 200000) {
                     throw new PixelImport.ImportException("it has more than " + String.format(Locale.ROOT, "%,d", 200000) + " voxels");
                  }

                  models.add(Arrays.copyOfRange(b, c + 4, c + 4 + 4 * count));
               } else if ("RGBA".equals(id) && n >= 1024) {
                  palette = new int[256];

                  for (int i = 0; i < 256; i++) {
                     int r = b[c + i * 4] & 255;
                     int g = b[c + i * 4 + 1] & 255;
                     int bl = b[c + i * 4 + 2] & 255;
                     int a = b[c + i * 4 + 3] & 255;
                     palette[i] = a << 24 | r << 16 | g << 8 | bl;
                  }
               } else if ("nTRN".equals(id) || "nGRP".equals(id) || "nSHP".equals(id)) {
                  readNode(b, c, c + n, id, nodes);
               }

               pos = c + n + m;
            }

            if (!models.isEmpty() && sizes.size() >= models.size()) {
               if (palette == null) {
                  palette = madeUpPalette();
                  notes.add("it has no palette of its own: its colors are made up");
               }

               ArrayList<double[]> places = new ArrayList<>();
               ArrayList<String> names = new ArrayList<>();

               for (int i = 0; i < models.size(); i++) {
                  places.add(null);
                  names.add(null);
               }

               if (nodes.containsKey(0)) {
                  walk(nodes, 0, new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0}, null, places, names, 0, new int[]{100000});
               }

               boolean placed = false;

               for (double[] p2 : places) {
                  placed |= p2 != null;
               }

               if (!placed) {
                  places.set(0, new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0});
                  if (models.size() > 1) {
                     notes.add("only its first model is used (it has no scene to place the others)");
                  }
               }

               ArrayList<int[]> all = new ArrayList<>();
               int minX = Integer.MAX_VALUE;
               int minY = Integer.MAX_VALUE;
               int minZ = Integer.MAX_VALUE;
               int maxX = Integer.MIN_VALUE;
               int maxY = Integer.MIN_VALUE;
               int maxZ = Integer.MIN_VALUE;
               int[][] box = new int[models.size()][];

               for (int mi = 0; mi < models.size(); mi++) {
                  double[] t = places.get(mi);
                  if (t != null) {
                     int[] size = sizes.get(mi);
                     byte[] v = models.get(mi);

                     for (int k = 0; k + 3 < v.length; k += 4) {
                        int x = v[k] & 255;
                        int y = v[k + 1] & 255;
                        int z = v[k + 2] & 255;
                        int ci = v[k + 3] & 255;
                        if (ci != 0 && x < size[0] && y < size[1] && z < size[2]) {
                           double lx = x + 0.5 - size[0] / 2.0;
                           double ly = y + 0.5 - size[1] / 2.0;
                           double lz = z + 0.5 - size[2] / 2.0;
                           int wx = (int)Math.floor(t[0] * lx + t[1] * ly + t[2] * lz + t[9]);
                           int wy = (int)Math.floor(t[3] * lx + t[4] * ly + t[5] * lz + t[10]);
                           int wz = (int)Math.floor(t[6] * lx + t[7] * ly + t[8] * lz + t[11]);
                           all.add(new int[]{wx, wy, wz, palette[ci - 1], mi});
                           int[] mb = box[mi];
                           if (mb == null) {
                              mb = new int[]{wx, wy, wz, wx, wy, wz};
                              box[mi] = mb;
                           }

                           mb[0] = Math.min(mb[0], wx);
                           mb[1] = Math.min(mb[1], wy);
                           mb[2] = Math.min(mb[2], wz);
                           mb[3] = Math.max(mb[3], wx);
                           mb[4] = Math.max(mb[4], wy);
                           mb[5] = Math.max(mb[5], wz);
                           minX = Math.min(minX, wx);
                           minY = Math.min(minY, wy);
                           minZ = Math.min(minZ, wz);
                           maxX = Math.max(maxX, wx);
                           maxY = Math.max(maxY, wy);
                           maxZ = Math.max(maxZ, wz);
                        }
                     }
                  }
               }

               if (all.isEmpty()) {
                  throw new PixelImport.ImportException("it has no voxels");
               } else {
                  PixelImport.Grid g = new PixelImport.Grid(maxX - minX + 1, maxZ - minZ + 1, maxY - minY + 1);
                  int[] partOfModel = new int[models.size()];

                  for (int mix = 0; mix < models.size(); mix++) {
                     String p = partName(names.get(mix));
                     partOfModel[mix] = p != null ? g.partOf(p) : 0;
                  }

                  for (int[] v : all) {
                     g.put(v[0] - minX, v[2] - minZ, maxY - v[1], v[3], partOfModel[v[4]]);
                  }

                  center(g);

                  for (int mi2 = 0; mi2 < models.size(); mi2++) {
                     String p = partName(names.get(mi2));
                     int[] mb = box[mi2];
                     if (p != null && mb != null && !g.pivots.containsKey(p)) {
                        double cx = (mb[0] + mb[3] + 1) / 2.0;
                        double cy = (mb[1] + mb[4] + 1) / 2.0;
                        double cz = (mb[2] + mb[5] + 1) / 2.0;
                        g.pivots.put(p, new float[]{(float)(cx - minX + g.ox), (float)(cz - minZ + g.oy), (float)(maxY + 1 - cy + g.oz)});
                     }
                  }

                  return g;
               }
            } else {
               throw new PixelImport.ImportException("it has no voxels");
            }
         }
      } else {
         throw new PixelImport.ImportException("it is not a MagicaVoxel file (no \"VOX \" at its start)");
      }
   }

   private static void readNode(byte[] b, int c, int end, String id, Map<Integer, Object[]> nodes) throws PixelImport.ImportException {
      int[] p = new int[]{c};
      int node = int32(b, p[0]);
      p[0] += 4;
      Map<String, String> attrs = dict(b, p, end);
      if ("nTRN".equals(id)) {
         int child = int32(b, p[0]);
         p[0] += 12;
         int frames = int32(b, p[0]);
         p[0] += 4;
         double[] t = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0};
         if (frames > 0) {
            Map<String, String> f = dict(b, p, end);
            String r = f.get("_r");
            String tr = f.get("_t");
            if (r != null) {
               rotation(Integer.parseInt(r.trim()), t);
            }

            if (tr != null) {
               String[] s = tr.trim().split("\\s+");

               for (int i = 0; i < 3 && i < s.length; i++) {
                  t[9 + i] = Double.parseDouble(s[i]);
               }
            }
         }

         nodes.put(node, new Object[]{0, new int[]{child}, null, t, attrs.get("_name")});
      } else if ("nGRP".equals(id)) {
         int n = int32(b, p[0]);
         p[0] += 4;
         if (n < 0 || p[0] + 4L * n > end) {
            throw new PixelImport.ImportException("a group in its scene runs past its chunk");
         }

         int[] kids = new int[n];

         for (int i = 0; i < n; i++) {
            kids[i] = int32(b, p[0]);
            p[0] += 4;
         }

         nodes.put(node, new Object[]{1, kids, null, null, attrs.get("_name")});
      } else {
         int n = int32(b, p[0]);
         p[0] += 4;
         if (n < 0 || n > 4096) {
            throw new PixelImport.ImportException("a shape in its scene has " + n + " models");
         }

         int[] ms = new int[n];

         for (int i = 0; i < n; i++) {
            ms[i] = int32(b, p[0]);
            p[0] += 4;
            dict(b, p, end);
         }

         nodes.put(node, new Object[]{2, null, ms, null, attrs.get("_name")});
      }
   }

   private static void walk(Map<Integer, Object[]> nodes, int id, double[] t, String name, List<double[]> places, List<String> names, int depth, int[] steps) {
      Object[] n = nodes.get(id);
      if (n != null && depth <= 64 && --steps[0] >= 0) {
         int type = (Integer)n[0];
         String own = (String)n[4];
         String nm = own != null ? own : name;
         if (type == 0) {
            double[] here = multiply(t, (double[])n[3]);

            for (int k : (int[])n[1]) {
               walk(nodes, k, here, nm, places, names, depth + 1, steps);
            }
         } else if (type == 1) {
            for (int k : (int[])n[1]) {
               walk(nodes, k, t, nm, places, names, depth + 1, steps);
            }
         } else {
            for (int m : (int[])n[2]) {
               if (m >= 0 && m < places.size() && places.get(m) == null) {
                  places.set(m, t);
                  names.set(m, nm);
               }
            }
         }
      }
   }

   private static double[] multiply(double[] a, double[] b) {
      double[] o = new double[12];

      for (int r = 0; r < 3; r++) {
         for (int c = 0; c < 3; c++) {
            o[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c];
         }

         o[9 + r] = a[r * 3] * b[9] + a[r * 3 + 1] * b[10] + a[r * 3 + 2] * b[11] + a[9 + r];
      }

      return o;
   }

   private static void rotation(int bits, double[] t) {
      int c0 = bits & 3;
      int c1 = bits >> 2 & 3;
      int c2 = 3 - c0 - c1;
      if (c0 <= 2 && c1 <= 2 && c0 != c1 && c2 >= 0 && c2 <= 2) {
         Arrays.fill(t, 0, 9, 0.0);
         t[c0] = (bits & 16) != 0 ? -1.0 : 1.0;
         t[3 + c1] = (bits & 32) != 0 ? -1.0 : 1.0;
         t[6 + c2] = (bits & 64) != 0 ? -1.0 : 1.0;
      }
   }

   private static Map<String, String> dict(byte[] b, int[] p, int end) throws PixelImport.ImportException {
      int n = int32(b, p[0]);
      p[0] += 4;
      if (n >= 0 && n <= 256) {
         HashMap<String, String> m = new HashMap<>();

         for (int i = 0; i < n; i++) {
            String k = string(b, p, end);
            String v = string(b, p, end);
            m.put(k, v);
         }

         return m;
      } else {
         throw new PixelImport.ImportException("its scene has a broken entry");
      }
   }

   private static String string(byte[] b, int[] p, int end) throws PixelImport.ImportException {
      int n = int32(b, p[0]);
      p[0] += 4;
      if (n >= 0 && n <= 4096 && (long)p[0] + n <= end) {
         String s = new String(b, p[0], n, StandardCharsets.UTF_8);
         p[0] += n;
         return s;
      } else {
         throw new PixelImport.ImportException("its scene has a broken name");
      }
   }

   private static String id(byte[] b, int at) {
      return at + 4 <= b.length ? new String(b, at, 4, StandardCharsets.US_ASCII) : "";
   }

   private static int int32(byte[] b, int at) {
      return at >= 0 && at + 4 <= b.length ? b[at] & 0xFF | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24 : -1;
   }

   private static int[] madeUpPalette() {
      int[] p = new int[256];

      for (int i = 0; i < 256; i++) {
         double h = i * 137.508 % 360.0;
         double l = 0.45 + 0.35 * (i / 7 % 3) / 2.0;
         double a = 0.11 * Math.cos(Math.toRadians(h));
         double bb = 0.11 * Math.sin(Math.toRadians(h));
         p[i] = 0xFF000000 | PixelPalette.Ramps.fromLab(l, a, bb);
      }

      return p;
   }

   static PixelImport.Grid readBlockbench(byte[] json, Map<String, byte[]> picked, List<String> notes) throws PixelImport.ImportException {
      JsonObject o = parse(json);
      double resW = 16.0;
      double resH = 16.0;
      JsonObject res = obj(o, "resolution");
      if (res != null) {
         resW = Math.max(1.0, WeaponDef.num(res, "width", 16.0));
         resH = Math.max(1.0, WeaponDef.num(res, "height", 16.0));
      }

      ArrayList<PixelImport.Texture> textures = new ArrayList<>();
      HashMap<String, Integer> byUuid = new HashMap<>();
      JsonArray tex = arr(o, "textures");
      if (tex != null) {
         for (JsonElement te : tex) {
            if (!te.isJsonObject()) {
               textures.add(null);
            } else {
               JsonObject t = te.getAsJsonObject();
               String name = WeaponDef.str(t, "name", "texture " + (textures.size() + 1));
               String source = WeaponDef.str(t, "source", null);
               byte[] png = null;
               if (source != null && source.startsWith("data:")) {
                  int comma = source.indexOf(44);
                  if (!source.startsWith("data:image/png;base64,") || comma < 0) {
                     throw new PixelImport.ImportException("its texture \"" + name + "\" is not a PNG");
                  }

                  try {
                     png = Base64.getDecoder().decode(source.substring(comma + 1).replaceAll("\\s", ""));
                  } catch (IllegalArgumentException var36) {
                     throw new PixelImport.ImportException("its texture \"" + name + "\" can't be read");
                  }
               } else {
                  png = pickedImage(picked, name, WeaponDef.str(t, "path", null));
                  if (png == null) {
                     throw new PixelImport.ImportException(
                        "its texture \"" + name + "\" isn't in the file: save the model with its textures embedded (or pick the texture too)"
                     );
                  }
               }

               PixelImport.Texture x = image(png, name);
               double uw = WeaponDef.num(t, "uv_width", resW);
               double uh = WeaponDef.num(t, "uv_height", resH);
               x.perU = x.w / Math.max(1.0, uw);
               x.perV = x.h / Math.max(1.0, uh);
               textures.add(x);
               String uuid = WeaponDef.str(t, "uuid", null);
               if (uuid != null) {
                  byUuid.put(uuid, textures.size() - 1);
               }
            }
         }
      }

      HashMap<String, String> partOfElement = new HashMap<>();
      LinkedHashMap<String, double[]> pivots = new LinkedHashMap<>();
      HashMap<String, JsonObject> groups = new HashMap<>();
      JsonArray groupList = arr(o, "groups");
      if (groupList != null) {
         for (JsonElement ge : groupList) {
            if (ge.isJsonObject() && ge.getAsJsonObject().has("uuid")) {
               groups.put(ge.getAsJsonObject().get("uuid").getAsString(), ge.getAsJsonObject());
            }
         }
      }

      JsonArray outliner;
      if ((outliner = arr(o, "outliner")) != null) {
         for (JsonElement n : outliner) {
            outline(n, "body", groups, partOfElement, pivots, 0);
         }
      }

      JsonArray elements;
      if ((elements = arr(o, "elements")) != null && elements.size() != 0) {
         if (elements.size() > 4096) {
            throw new PixelImport.ImportException("it has " + elements.size() + " elements (at most " + 4096 + ")");
         } else {
            ArrayList<PixelImport.Cube> cubes = new ArrayList<>();
            LinkedHashMap<String, Integer> partIndex = new LinkedHashMap<>();
            partIndex.put("body", 0);
            int meshes = 0;

            for (JsonElement ee : elements) {
               if (ee.isJsonObject()) {
                  JsonObject e = ee.getAsJsonObject();
                  String type = WeaponDef.str(e, "type", "cube");
                  if (!"cube".equals(type)) {
                     meshes++;
                  } else if (!e.has("visibility") || !e.get("visibility").isJsonPrimitive() || e.get("visibility").getAsBoolean()) {
                     PixelImport.Cube c = new PixelImport.Cube();
                     vec(e, "from", c.from);
                     vec(e, "to", c.to);
                     vec(e, "origin", c.origin);
                     double inflate = WeaponDef.num(e, "inflate", 0.0);

                     for (int i = 0; i < 3; i++) {
                        double lo = Math.min(c.from[i], c.to[i]) - inflate;
                        double hi = Math.max(c.from[i], c.to[i]) + inflate;
                        c.from[i] = lo;
                        c.to[i] = hi;
                     }

                     double[] r = new double[3];
                     if (e.has("rotation") && e.get("rotation").isJsonArray()) {
                        vec(e, "rotation", r);
                     }

                     c.rot = euler(r[0], r[1], r[2]);
                     JsonObject faces = obj(e, "faces");

                     for (int f = 0; f < 6 && faces != null; f++) {
                        JsonObject fo = obj(faces, FACES[f]);
                        if (fo != null && fo.has("texture") && !fo.get("texture").isJsonNull()) {
                           JsonElement texx = fo.get("texture");
                           int ti = texx.isJsonPrimitive() && texx.getAsJsonPrimitive().isNumber()
                              ? texx.getAsInt()
                              : (
                                 texx.isJsonPrimitive() && texx.getAsJsonPrimitive().isString() && byUuid.containsKey(texx.getAsString())
                                    ? byUuid.get(texx.getAsString())
                                    : -1
                              );
                           if (ti >= 0 && ti < textures.size() && textures.get(ti) != null) {
                              PixelImport.Face face = new PixelImport.Face();
                              face.texture = textures.get(ti);
                              face.uv = uv(fo, c, f, 16.0);
                              face.rotation = (int)WeaponDef.num(fo, "rotation", 0.0);
                              c.faces[f] = face;
                           }
                        }
                     }

                     String uuid = WeaponDef.str(e, "uuid", null);
                     String p = uuid != null && partOfElement.containsKey(uuid) ? partOfElement.get(uuid) : "body";
                     if (!partIndex.containsKey(p)) {
                        partIndex.put(p, partIndex.size());
                     }

                     c.part = partIndex.get(p);
                     cubes.add(c);
                  }
               }
            }

            if (cubes.isEmpty()) {
               throw new PixelImport.ImportException(meshes > 0 ? "it is made of meshes, not cubes: export it as OBJ and import that" : "it has no cubes");
            } else {
               if (meshes > 0) {
                  notes.add(meshes + " mesh element" + (meshes == 1 ? " was" : "s were") + " left out (cubes only: export meshes as OBJ)");
               }

               return voxelize(cubes, textures, new ArrayList<>(partIndex.keySet()), pivots);
            }
         }
      } else {
         throw new PixelImport.ImportException("it has no elements");
      }
   }

   private static void outline(
      JsonElement n, String part, Map<String, JsonObject> groups, Map<String, String> partOfElement, Map<String, double[]> pivots, int depth
   ) {
      if (depth <= 64) {
         if (n.isJsonPrimitive()) {
            partOfElement.put(n.getAsString(), part);
         } else if (n.isJsonObject()) {
            JsonObject g = n.getAsJsonObject();
            JsonObject def = g.has("name") ? g : (g.has("uuid") ? groups.get(g.get("uuid").getAsString()) : null);
            String here = part;
            String named;
            if (def != null && (named = partName(WeaponDef.str(def, "name", null))) != null) {
               here = named;
               if (!pivots.containsKey(named)) {
                  double[] origin = new double[3];
                  vec(def, "origin", origin);
                  pivots.put(named, origin);
               }
            }

            JsonArray kids;
            if ((kids = arr(g, "children")) != null) {
               for (JsonElement k : kids) {
                  outline(k, here, groups, partOfElement, pivots, depth + 1);
               }
            }
         }
      }
   }

   static PixelImport.Grid readItemModel(byte[] json, Map<String, byte[]> picked, List<String> notes) throws PixelImport.ImportException {
      JsonObject o = parse(json);
      JsonObject tj = obj(o, "textures");
      HashMap<String, String> names = new HashMap<>();
      if (tj != null) {
         for (Entry e : tj.entrySet()) {
            if (((JsonElement)e.getValue()).isJsonPrimitive()) {
               names.put((String)e.getKey(), ((JsonElement)e.getValue()).getAsString());
            }
         }
      }

      HashMap<String, PixelImport.Texture> loaded = new HashMap<>();
      JsonArray elements = arr(o, "elements");
      if (elements != null && elements.size() != 0) {
         if (elements.size() > 4096) {
            throw new PixelImport.ImportException("it has " + elements.size() + " elements (at most " + 4096 + ")");
         } else {
            ArrayList<PixelImport.Cube> cubes = new ArrayList<>();
            ArrayList<PixelImport.Texture> all = new ArrayList<>();

            for (JsonElement ee : elements) {
               if (ee.isJsonObject()) {
                  JsonObject ex = ee.getAsJsonObject();
                  PixelImport.Cube c = new PixelImport.Cube();
                  vec(ex, "from", c.from);
                  vec(ex, "to", c.to);

                  for (int i = 0; i < 3; i++) {
                     double lo = Math.min(c.from[i], c.to[i]);
                     double hi = Math.max(c.from[i], c.to[i]);
                     c.from[i] = lo;
                     c.to[i] = hi;
                  }

                  JsonObject r = obj(ex, "rotation");
                  if (r != null) {
                     vec(r, "origin", c.origin);
                     double angle = WeaponDef.num(r, "angle", 0.0);
                     String axis = WeaponDef.str(r, "axis", "y");
                     c.rot = euler("x".equals(axis) ? angle : 0.0, "y".equals(axis) ? angle : 0.0, "z".equals(axis) ? angle : 0.0);
                  }

                  JsonObject faces = obj(ex, "faces");

                  for (int f = 0; f < 6 && faces != null; f++) {
                     JsonObject fo = obj(faces, FACES[f]);
                     if (fo != null) {
                        String ref = WeaponDef.str(fo, "texture", null);
                        String path = ref == null ? null : (ref.startsWith("#") ? resolve(names, ref.substring(1)) : ref);
                        if (path != null) {
                           PixelImport.Texture t = itemTexture(path, picked, loaded);
                           if (!all.contains(t)) {
                              all.add(t);
                           }

                           t.perU = t.w / 16.0;
                           t.perV = t.h / 16.0;
                           PixelImport.Face face = new PixelImport.Face();
                           face.texture = t;
                           face.uv = uv(fo, c, f, 16.0);
                           face.rotation = (int)WeaponDef.num(fo, "rotation", 0.0);
                           c.faces[f] = face;
                        }
                     }
                  }

                  cubes.add(c);
               }
            }

            if (cubes.isEmpty()) {
               throw new PixelImport.ImportException("it has no elements");
            } else {
               return voxelize(cubes, all, Collections.singletonList("body"), new LinkedHashMap<>());
            }
         }
      } else {
         ArrayList<PixelImport.Texture> layers = new ArrayList<>();

         for (int i = 0; i < 8; i++) {
            String path = resolve(names, "layer" + i);
            if (path != null) {
               layers.add(itemTexture(path, picked, loaded));
            }
         }

         if (layers.isEmpty()) {
            throw new PixelImport.ImportException(
               "it has no elements and no layer0 texture (a model with a parent other than item/generated needs that parent's elements)"
            );
         } else {
            int w = layers.get(0).w;
            int h = layers.get(0).h;
            PixelImport.Grid g = new PixelImport.Grid(w, h, 1);

            for (PixelImport.Texture t : layers) {
               for (int y = 0; y < h; y++) {
                  for (int x = 0; x < w; x++) {
                     int ty = y * t.h / h;
                     int tx = x * t.w / w;
                     int c = t.argb[ty * t.w + tx];
                     if (c >>> 24 >= 128) {
                        g.put(x, h - 1 - y, 0, c, 0);
                     }
                  }
               }
            }

            center(g);
            g.oz = -0.5F;
            g.unit = 1.0F / w;
            return g;
         }
      }
   }

   private static String resolve(Map<String, String> names, String key) {
      String v = names.get(key);

      for (int i = 0; v != null && v.startsWith("#") && i < 16; i++) {
         v = names.get(v.substring(1));
      }

      return v != null && v.startsWith("#") ? null : v;
   }

   private static PixelImport.Texture itemTexture(String path, Map<String, byte[]> picked, Map<String, PixelImport.Texture> loaded) throws PixelImport.ImportException {
      PixelImport.Texture t = loaded.get(path);
      if (t != null) {
         return t;
      } else {
         String base = path.substring(path.lastIndexOf(47) + 1);
         String var7;
         byte[] png = pickedImage(picked, var7 = base.substring(base.lastIndexOf(58) + 1), null);
         if (png == null) {
            throw new PixelImport.ImportException("pick its texture too: " + var7 + ".png");
         } else {
            t = image(png, var7 + ".png");
            loaded.put(path, t);
            return t;
         }
      }
   }

   private static byte[] pickedImage(Map<String, byte[]> picked, String name, String path) {
      ArrayList<String> tries = new ArrayList<>();

      for (String s : new String[]{name, path}) {
         if (s != null) {
            String f = s.replace('\\', '/');
            f = f.substring(f.lastIndexOf(47) + 1).toLowerCase(Locale.ROOT);
            tries.add(f);
            tries.add(f.endsWith(".png") ? f : f + ".png");
         }
      }

      for (Entry entry : picked.entrySet()) {
         if (tries.contains(((String)entry.getKey()).toLowerCase(Locale.ROOT))) {
            return (byte[])entry.getValue();
         }
      }

      return null;
   }

   private static double[] uv(JsonObject face, PixelImport.Cube c, int f, double size) {
      double[] uv = new double[4];
      JsonElement e = face.get("uv");
      if (e != null && e.isJsonArray() && e.getAsJsonArray().size() >= 4) {
         for (int i = 0; i < 4; i++) {
            uv[i] = e.getAsJsonArray().get(i).getAsDouble();
         }

         return uv;
      } else {
         double[] a = c.from;
         double[] b = c.to;
         switch (f) {
            case 0:
               return new double[]{size - b[0], size - b[1], size - a[0], size - a[1]};
            case 1:
               return new double[]{size - b[2], size - b[1], size - a[2], size - a[1]};
            case 2:
               return new double[]{a[0], size - b[1], b[0], size - a[1]};
            case 3:
               return new double[]{a[2], size - b[1], b[2], size - a[1]};
            case 4:
               return new double[]{a[0], a[2], b[0], b[2]};
            default:
               return new double[]{a[0], size - b[2], b[0], size - a[2]};
         }
      }
   }

   private static PixelImport.Grid voxelize(
      List<PixelImport.Cube> cubes, List<PixelImport.Texture> textures, List<String> partNames, Map<String, double[]> pivots
   ) throws PixelImport.ImportException {
      int k = 1;

      for (PixelImport.Texture t : textures) {
         if (t != null) {
            k = Math.max(k, (int)Math.round(Math.max(t.perU, t.perV)));
         }
      }

      k = Math.max(1, Math.min(4, k));
      double[] lo = new double[]{1.0E9, 1.0E9, 1.0E9};
      double[] hi = new double[]{-1.0E9, -1.0E9, -1.0E9};

      for (PixelImport.Cube c : cubes) {
         for (int corner = 0; corner < 8; corner++) {
            Object p = new double[]{(corner & 1) == 0 ? c.from[0] : c.to[0], (corner & 2) == 0 ? c.from[1] : c.to[1], (corner & 4) == 0 ? c.from[2] : c.to[2]};
            double[] w = toModel(c, (double[])p);

            for (int i = 0; i < 3; i++) {
               lo[i] = Math.min(lo[i], w[i]);
               hi[i] = Math.max(hi[i], w[i]);
            }
         }
      }

      while (k < 4 && !onGrid(cubes, lo, k) && (hi[0] - lo[0]) * k * 2.0 <= 256.0 && (hi[1] - lo[1]) * k * 2.0 <= 256.0 && (hi[2] - lo[2]) * k * 2.0 <= 256.0) {
         k *= 2;
      }

      double[] base = new double[3];
      int[] n = new int[3];

      for (int i = 0; i < 3; i++) {
         base[i] = gridStart(lo[i], k);
         n[i] = Math.max(1, (int)Math.ceil(hi[i] * k - base[i] - 1.0E-6));
      }

      PixelImport.Grid g = new PixelImport.Grid(n[0], n[1], n[2]);

      for (String p : partNames) {
         g.partOf(p);
      }

      double[][] probes = new double[9][];
      probes[0] = new double[]{0.5, 0.5, 0.5};

      for (int s = 0; s < 8; s++) {
         probes[1 + s] = new double[]{(s & 1) == 0 ? 0.2 : 0.8, (s & 2) == 0 ? 0.2 : 0.8, (s & 4) == 0 ? 0.2 : 0.8};
      }

      for (PixelImport.Cube cube : cubes) {
         int[] a = new int[3];
         int[] b = new int[3];
         double[] clo = new double[]{1.0E9, 1.0E9, 1.0E9};
         double[] chi = new double[]{-1.0E9, -1.0E9, -1.0E9};

         for (int corner = 0; corner < 8; corner++) {
            double[] p = new double[]{
               (corner & 1) == 0 ? cube.from[0] : cube.to[0], (corner & 2) == 0 ? cube.from[1] : cube.to[1], (corner & 4) == 0 ? cube.from[2] : cube.to[2]
            };
            double[] w = toModel(cube, p);

            for (int i = 0; i < 3; i++) {
               clo[i] = Math.min(clo[i], w[i]);
               chi[i] = Math.max(chi[i], w[i]);
            }
         }

         for (int i = 0; i < 3; i++) {
            a[i] = Math.max(0, (int)Math.floor(clo[i] * k - base[i] + 1.0E-6));
            b[i] = Math.min(n[i] - 1, (int)Math.ceil(chi[i] * k - base[i] - 1.0E-6) - 1);
         }

         int part = g.partOf(partNames.get(Math.min(cube.part, partNames.size() - 1)));
         double[] q = new double[3];

         for (int z = a[2]; z <= b[2]; z++) {
            for (int y = a[1]; y <= b[1]; y++) {
               for (int x = a[0]; x <= b[0]; x++) {
                  for (double[] pr : probes) {
                     double[] p = new double[]{(base[0] + x + pr[0]) / k, (base[1] + y + pr[1]) / k, (base[2] + z + pr[2]) / k};
                     toLocal(cube, p, q);
                     if (!(q[0] < cube.from[0])
                        && !(q[0] >= cube.to[0])
                        && !(q[1] < cube.from[1])
                        && !(q[1] >= cube.to[1])
                        && !(q[2] < cube.from[2])
                        && !(q[2] >= cube.to[2])) {
                        int color = texel(cube, q);
                        if (color != 0) {
                           g.put(x, y, z, color, part);
                        }
                        break;
                     }
                  }
               }
            }
         }
      }

      if (g.count() == 0) {
         throw new PixelImport.ImportException("none of its faces has a texture to color it");
      } else {
         g.ox = (float)base[0];
         g.oy = (float)base[1];
         g.oz = (float)base[2];
         g.unit = 1.0F / (16.0F * k);

         for (Entry entry : pivots.entrySet()) {
            double[] p = (double[])entry.getValue();
            g.pivots.put((String)entry.getKey(), new float[]{(float)(p[0] * k), (float)(p[1] * k), (float)(p[2] * k)});
         }

         return g;
      }
   }

   private static double gridStart(double lo, int k) {
      return Math.round(lo * k * 2.0) / 2.0;
   }

   private static boolean onGrid(List<PixelImport.Cube> cubes, double[] lo, int k) {
      for (PixelImport.Cube c : cubes) {
         boolean square = true;

         for (double r : c.rot) {
            square &= Math.abs(r) < 1.0E-6 || Math.abs(Math.abs(r) - 1.0) < 1.0E-6;
         }

         if (square) {
            double[] a = toModel(c, c.from);
            double[] b = toModel(c, c.to);

            for (int i = 0; i < 3; i++) {
               double base = gridStart(lo[i], k);

               for (double v : new double[]{a[i], b[i]}) {
                  double at = v * k - base;
                  if (Math.abs(at - Math.round(at)) > 0.001) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   private static int texel(PixelImport.Cube c, double[] q) {
      double[] d = new double[]{q[2] - c.from[2], c.to[0] - q[0], c.to[2] - q[2], q[0] - c.from[0], c.to[1] - q[1], q[1] - c.from[1]};
      int best = -1;

      for (int f = 0; f < 6; f++) {
         if (c.faces[f] != null && (best < 0 || d[f] < d[best])) {
            best = f;
         }
      }

      if (best < 0) {
         return 0;
      } else {
         double w = Math.max(1.0E-9, c.to[0] - c.from[0]);
         double h = Math.max(1.0E-9, c.to[1] - c.from[1]);
         double dz = Math.max(1.0E-9, c.to[2] - c.from[2]);
         double t;
         double s;
         switch (best) {
            case 0:
               s = (c.to[0] - q[0]) / w;
               t = (c.to[1] - q[1]) / h;
               break;
            case 1:
               s = (c.to[2] - q[2]) / dz;
               t = (c.to[1] - q[1]) / h;
               break;
            case 2:
               s = (q[0] - c.from[0]) / w;
               t = (c.to[1] - q[1]) / h;
               break;
            case 3:
               s = (q[2] - c.from[2]) / dz;
               t = (c.to[1] - q[1]) / h;
               break;
            case 4:
               s = (q[0] - c.from[0]) / w;
               t = (q[2] - c.from[2]) / dz;
               break;
            default:
               s = (q[0] - c.from[0]) / w;
               t = (c.to[2] - q[2]) / dz;
         }

         PixelImport.Face face = c.faces[best];
         s = Math.max(1.0E-4, Math.min(0.9999, s));
         t = Math.max(1.0E-4, Math.min(0.9999, t));

         for (int r = (face.rotation % 360 + 360) % 360; r >= 90; r -= 90) {
            double nt = 1.0 - s;
            s = t;
            t = nt;
         }

         double u = face.uv[0] + s * (face.uv[2] - face.uv[0]);
         double v = face.uv[1] + t * (face.uv[3] - face.uv[1]);
         PixelImport.Texture tx = face.texture;
         int px = (int)Math.floor(u * tx.perU);
         int py = (int)Math.floor(v * tx.perV);
         px = Math.max(0, Math.min(tx.w - 1, px));
         int color = tx.argb[Math.max(0, Math.min(tx.h - 1, py)) * tx.w + px];
         return color >>> 24 < 128 ? 0 : color;
      }
   }

   private static double[] toModel(PixelImport.Cube c, double[] p) {
      double x = p[0] - c.origin[0];
      double y = p[1] - c.origin[1];
      double z = p[2] - c.origin[2];
      double[] r = c.rot;
      return new double[]{
         r[0] * x + r[1] * y + r[2] * z + c.origin[0], r[3] * x + r[4] * y + r[5] * z + c.origin[1], r[6] * x + r[7] * y + r[8] * z + c.origin[2]
      };
   }

   private static void toLocal(PixelImport.Cube c, double[] p, double[] out) {
      double x = p[0] - c.origin[0];
      double y = p[1] - c.origin[1];
      double z = p[2] - c.origin[2];
      double[] r = c.rot;
      out[0] = r[0] * x + r[3] * y + r[6] * z + c.origin[0];
      out[1] = r[1] * x + r[4] * y + r[7] * z + c.origin[1];
      out[2] = r[2] * x + r[5] * y + r[8] * z + c.origin[2];
   }

   private static double[] euler(double xd, double yd, double zd) {
      double x = Math.toRadians(xd);
      double y = Math.toRadians(yd);
      double z = Math.toRadians(zd);
      double cx = Math.cos(x);
      double sx = Math.sin(x);
      double cy = Math.cos(y);
      double sy = Math.sin(y);
      double cz = Math.cos(z);
      double sz = Math.sin(z);
      double[] rx = new double[]{1.0, 0.0, 0.0, 0.0, cx, -sx, 0.0, sx, cx};
      double[] ry = new double[]{cy, 0.0, sy, 0.0, 1.0, 0.0, -sy, 0.0, cy};
      double[] rz = new double[]{cz, -sz, 0.0, sz, cz, 0.0, 0.0, 0.0, 1.0};
      return mul3(rz, mul3(ry, rx));
   }

   private static double[] mul3(double[] a, double[] b) {
      double[] o = new double[9];

      for (int r = 0; r < 3; r++) {
         for (int c = 0; c < 3; c++) {
            o[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c];
         }
      }

      return o;
   }

   static PixelImport.Texture image(byte[] png, String name) throws PixelImport.ImportException {
      if (png.length >= 33 && (png[0] & 255) == 137 && png[1] == 80 && png[2] == 78 && png[3] == 71) {
         int w = (png[16] & 255) << 24 | (png[17] & 255) << 16 | (png[18] & 255) << 8 | png[19] & 255;
         int h = (png[20] & 255) << 24 | (png[21] & 255) << 16 | (png[22] & 255) << 8 | png[23] & 255;
         if (w > 0 && h > 0 && w <= 1024 && h <= 1024) {
            BufferedImage img;
            try {
               img = ImageIO.read(new ByteArrayInputStream(png));
            } catch (RuntimeException | IOException var6) {
               throw new PixelImport.ImportException("the texture \"" + name + "\" can't be read");
            }

            if (img != null && img.getWidth() == w && img.getHeight() == h) {
               int[] argb = img.getRGB(0, 0, w, h, null, 0, w);
               return new PixelImport.Texture(argb, w, h);
            } else {
               throw new PixelImport.ImportException("the texture \"" + name + "\" can't be read");
            }
         } else {
            throw new PixelImport.ImportException("the texture \"" + name + "\" is " + w + " x " + h + " pixels (at most " + 1024 + " on a side)");
         }
      } else {
         throw new PixelImport.ImportException("the texture \"" + name + "\" is not a PNG image");
      }
   }

   private static JsonObject parse(byte[] json) throws PixelImport.ImportException {
      try {
         JsonElement e = new JsonParser().parse(new String(json, StandardCharsets.UTF_8));
         if (!e.isJsonObject()) {
            throw new PixelImport.ImportException("it is not a JSON object");
         } else {
            return e.getAsJsonObject();
         }
      } catch (RuntimeException var2) {
         throw new PixelImport.ImportException("it is not valid JSON");
      }
   }

   private static JsonObject obj(JsonObject o, String key) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
   }

   private static JsonArray arr(JsonObject o, String key) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
   }

   private static void vec(JsonObject o, String key, double[] out) {
      JsonElement e = o.get(key);
      if (e != null && e.isJsonArray()) {
         JsonArray a = e.getAsJsonArray();

         for (int i = 0; i < 3 && i < a.size(); i++) {
            try {
               double v = a.get(i).getAsDouble();
               out[i] = !Double.isNaN(v) && !Double.isInfinite(v) ? Math.max(-4096.0, Math.min(4096.0, v)) : 0.0;
            } catch (RuntimeException var8) {
               out[i] = 0.0;
            }
         }
      }
   }

   static void center(PixelImport.Grid g) {
      g.ox = -g.nx / 2;
      g.oy = 0.0F;
      g.oz = -g.nz / 2;
   }

   static float[] handleLine(PixelImport.Grid g) {
      double[][] dirs = new double[][]{
         {1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}, {Math.sqrt(0.5), Math.sqrt(0.5), 0.0}, {-Math.sqrt(0.5), Math.sqrt(0.5), 0.0}
      };
      double[] lo = new double[5];
      double[] hi = new double[5];
      Arrays.fill(lo, 1.0E9);
      Arrays.fill(hi, -1.0E9);
      int handle = g.parts.indexOf("handle");
      double[] all = new double[3];
      double[] held = new double[3];
      int count = 0;
      int heldCount = 0;

      for (int z = 0; z < g.nz; z++) {
         for (int y = 0; y < g.ny; y++) {
            for (int x = 0; x < g.nx; x++) {
               int i = g.index(x, y, z);
               if (g.color[i] != 0) {
                  double[] p = new double[]{x + g.ox + 0.5, y + g.oy + 0.5, z + g.oz + 0.5};

                  for (int d2 = 0; d2 < 5; d2++) {
                     double s = p[0] * dirs[d2][0] + p[1] * dirs[d2][1] + p[2] * dirs[d2][2];
                     lo[d2] = Math.min(lo[d2], s);
                     hi[d2] = Math.max(hi[d2], s);
                  }

                  for (int k = 0; k < 3; k++) {
                     all[k] += p[k];
                  }

                  count++;
                  if (handle >= 0 && g.part[i] == handle) {
                     for (int var32 = 0; var32 < 3; var32++) {
                        held[var32] += p[var32];
                     }

                     heldCount++;
                  }
               }
            }
         }
      }

      int best = 1;

      for (int d = 0; d < 3; d++) {
         if (hi[d] - lo[d] > hi[best] - lo[best] + 1.0E-9) {
            best = d;
         }
      }

      for (int var31 = 3; var31 < 5; var31++) {
         double span = hi[var31] - lo[var31];
         if (span > 1.15 * Math.max(hi[0] - lo[0], hi[1] - lo[1]) && span > hi[2] - lo[2] && span > hi[best] - lo[best]) {
            best = var31;
         }
      }

      double[] dir = (double[])dirs[best].clone();
      double[] at = new double[3];
      if (heldCount > 0) {
         double toRest = 0.0;

         for (int k = 0; k < 3; k++) {
            at[k] = held[k] / heldCount;
            toRest += (all[k] / count - at[k]) * dir[k];
         }

         if (toRest < 0.0) {
            for (int var34 = 0; var34 < 3; var34++) {
               dir[var34] = -dir[var34];
            }
         }
      } else {
         double span = hi[best] - lo[best];
         double from = lo[best] + 0.06 * span - 0.5;
         double to = lo[best] + 0.18 * span + 0.5;
         double[] sum = new double[3];
         int n = 0;

         for (int z = 0; z < g.nz; z++) {
            for (int y = 0; y < g.ny; y++) {
               for (int xx = 0; xx < g.nx; xx++) {
                  if (g.color[g.index(xx, y, z)] != 0) {
                     double[] dArray = new double[]{xx + g.ox + 0.5, y + g.oy + 0.5, z + g.oz + 0.5};
                     double[] p = dArray;
                     double s = dArray[0] * dir[0] + dArray[1] * dir[1] + dArray[2] * dir[2];
                     if (s >= from && s <= to) {
                        for (int k2 = 0; k2 < 3; k2++) {
                           sum[k2] += p[k2];
                        }

                        n++;
                     }
                  }
               }
            }
         }

         for (int k3 = 0; k3 < 3; k3++) {
            at[k3] = n > 0 ? sum[k3] / n : all[k3] / count;
         }

         double s = at[0] * dir[0] + at[1] * dir[1] + at[2] * dir[2];
         double want = lo[best] + 0.12 * span;

         for (int k4 = 0; k4 < 3; k4++) {
            at[k4] += (want - s) * dir[k4];
         }
      }

      float u = g.unit;
      return new float[]{(float)at[0] * u, (float)at[1] * u, (float)at[2] * u, (float)dir[0], (float)dir[1], (float)dir[2]};
   }

   static PixelImport.Grid voxelizeObj(ObjModel m, int[] tex, int tw, int th, float voxel) throws PixelImport.ImportException {
      if (!(voxel > 1.0E-6F)) {
         throw new PixelImport.ImportException("its size is too small to make pixels of");
      } else {
         int[] base = new int[3];
         int[] n = new int[3];

         for (int i = 0; i < 3; i++) {
            base[i] = (int)Math.floor(m.min[i] / voxel);
            n[i] = (int)Math.floor(m.max[i] / voxel) - base[i] + 1;
         }

         if (n[0] <= 256 && n[1] <= 256 && n[2] <= 256) {
            PixelImport.Grid g = new PixelImport.Grid(n[0], n[1], n[2]);
            float[] a = new float[8];
            float[] b = new float[8];
            float[] c = new float[8];

            for (ObjModel.Part part : m.parts.values()) {
               String named = partName(part.name);
               int pi = named != null ? g.partOf(named) : 0;
               float[] d = part.data;

               for (int t = 0; t + 24 <= part.floats; t += 24) {
                  System.arraycopy(d, t, a, 0, 8);
                  System.arraycopy(d, t + 8, b, 0, 8);
                  System.arraycopy(d, t + 16, c, 0, 8);
                  double edge = Math.max(len(a, b), Math.max(len(b, c), len(c, a)));
                  int steps = Math.max(1, Math.min(1024, (int)Math.ceil(edge / (voxel * 0.5))));

                  for (int i = 0; i <= steps; i++) {
                     for (int j = 0; i + j <= steps; j++) {
                        float u = (float)i / steps;
                        float v = (float)j / steps;
                        float w = 1.0F - u - v;
                        float x = w * a[0] + u * b[0] + v * c[0];
                        float y = w * a[1] + u * b[1] + v * c[1];
                        float z = w * a[2] + u * b[2] + v * c[2];
                        int vx = (int)Math.floor(x / voxel) - base[0];
                        int vz;
                        int vy;
                        if (g.at(vx, vy = (int)Math.floor(y / voxel) - base[1], vz = (int)Math.floor(z / voxel) - base[2]) == 0) {
                           int color = -4934476;
                           if (tex != null) {
                              float s = w * a[6] + u * b[6] + v * c[6];
                              float tt = w * a[7] + u * b[7] + v * c[7];
                              s -= (float)Math.floor(s);
                              if ((color = tex[Math.min(th - 1, (int)((tt - (float)Math.floor(tt)) * th)) * tw + Math.min(tw - 1, (int)(s * tw))]) >>> 24 < 128
                                 )
                               {
                                 continue;
                              }
                           }

                           g.put(vx, vy, vz, color, pi);
                        }
                     }
                  }
               }
            }

            if (g.count() == 0) {
               throw new PixelImport.ImportException("it has no faces to make pixels of");
            } else {
               g.ox = base[0];
               g.oy = base[1];
               g.oz = base[2];
               g.unit = voxel;
               return g;
            }
         } else {
            throw new PixelImport.ImportException(
               "at its size it would be " + n[0] + " x " + n[1] + " x " + n[2] + " pixels (at most " + 256 + " on a side): make it smaller under Grip Setup"
            );
         }
      }
   }

   private static double len(float[] p, float[] q) {
      double x = p[0] - q[0];
      double y = p[1] - q[1];
      double z = p[2] - q[2];
      return Math.sqrt(x * x + y * y + z * z);
   }

   static boolean pixelateObj(File dir) throws IOException, PixelImport.ImportException {
      File obj = new File(dir, "model.obj");
      File wj = new File(dir, "weapon.json");
      if (obj.isFile() && wj.isFile() && wj.length() <= 1048576L) {
         for (String k : new String[]{"bbmodel", "json", "vox"}) {
            if (new File(dir, "pixel." + k).isFile()) {
               return false;
            }
         }

         JsonElement je = new JsonParser().parse(new String(Files.readAllBytes(wj.toPath()), StandardCharsets.UTF_8));
         if (!je.isJsonObject()) {
            return false;
         } else {
            JsonObject o = je.getAsJsonObject();
            JsonObject fp = obj(o, "firstPerson");
            double scale = fp != null ? WeaponDef.num(fp, "scale", 1.0) : 1.0;
            float voxel = (float)(0.03125 / Math.max(1.0E-4, scale));
            JsonObject tj = obj(o, "textures");
            String texName = tj != null ? WeaponDef.str(tj, "albedo", null) : null;
            File tex = texName != null ? new File(dir, texName) : null;
            if (tex != null && (!tex.isFile() || !tex.getCanonicalFile().getParentFile().equals(dir.getCanonicalFile()))) {
               tex = null;
            }

            String sig = signature(obj) + (tex != null ? " " + signature(tex) : "") + String.format(Locale.ROOT, " voxel %.6f", voxel);
            File pixelJson = new File(dir, "pixel" + File.separator + "pixel.json");
            String made;
            if (!pixelJson.isFile() || (made = madeFrom(pixelJson)) != null && !made.equals(sig)) {
               ObjModel model;
               try (InputStream in = Files.newInputStream(obj.toPath())) {
                  model = ObjModel.read(in, 150000);
               }

               int[] px = null;
               int tw = 0;
               int th = 0;
               if (tex != null) {
                  try {
                     ImageLoader.Image img = ImageLoader.load(tex, 4096, false);
                     px = img.argb;
                     tw = img.width;
                     th = img.height;
                  } catch (ImageLoader.LoadException var28) {
                     throw new PixelImport.ImportException("its texture can't be read (" + var28.getMessage() + ")");
                  }
               }

               write(voxelizeObj(model, px, tw, th, voxel), dir, voxel, 32, sig);
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   static float[] write(PixelImport.Grid g, File dir, float pixel, int maxColors, String madeFrom) throws IOException, PixelImport.ImportException {
      int energy = g.parts.indexOf("energy");
      LinkedHashMap<Integer, int[]> counts = new LinkedHashMap<>();

      for (int i = 0; i < g.color.length; i++) {
         if (g.color[i] != 0) {
            int key = g.color[i] & 16777215 | (energy >= 0 && g.part[i] == energy ? 16777216 : 0);
            int[] c = counts.get(key);
            if (c == null) {
               c = new int[1];
               counts.put(key, c);
            }

            c[0]++;
         }
      }

      if (counts.isEmpty()) {
         throw new PixelImport.ImportException("it has no voxels to draw");
      } else {
         Map<Integer, Integer> slotOf = null;
         ArrayList<int[]> ramps = null;
         ArrayList<Boolean> glows = null;

         for (int limit = Math.min(maxColors, 80); limit >= 8 && slotOf == null; limit -= 8) {
            Map<Integer, Integer> to = quantize(counts, limit);
            ramps = new ArrayList<>();
            glows = new ArrayList<>();
            slotOf = ramps(to, ramps, glows);
         }

         if (slotOf == null) {
            throw new PixelImport.ImportException("its colors could not be laid out in ramps");
         } else {
            File out = new File(dir, "pixel.new");
            deleteGenerated(out);
            Files.createDirectories(out.toPath());
            int[] previewPalette = new int[256];

            for (int f = 0; f < ramps.size(); f++) {
               for (int s = 0; s < 5; s++) {
                  previewPalette[PixelModel.slot(PixelModel.MATERIAL_NAMES.length + f, s)] = ramps.get(f)[s];
               }
            }

            JsonObject json = new JsonObject();
            json.addProperty("pixel", pixel);
            json.addProperty("madeFrom", madeFrom);
            JsonArray fixed = new JsonArray();

            for (int f = 0; f < ramps.size(); f++) {
               JsonObject fo = new JsonObject();
               fo.addProperty("name", "color " + (f + 1));
               fo.addProperty("color", hex(ramps.get(f)[2]));
               JsonArray shades = new JsonArray();
               JsonArray slots = new JsonArray();

               for (int s = 0; s < 5; s++) {
                  shades.add(new JsonPrimitive(hex(ramps.get(f)[s])));
                  slots.add(new JsonPrimitive(PixelModel.slot(PixelModel.MATERIAL_NAMES.length + f, s)));
               }

               fo.add("shades", shades);
               fo.add("slots", slots);
               if (glows.get(f)) {
                  fo.addProperty("glow", true);
               }

               fixed.add(fo);
            }

            json.add("fixed", fixed);
            JsonArray layers = new JsonArray();
            int sprites = 0;

            for (int p = 0; p < g.parts.size(); p++) {
               for (int z = 0; z < g.nz; z++) {
                  int x0 = g.nx;
                  int x1 = -1;
                  int y0 = g.ny;
                  int y1 = -1;

                  for (int y = 0; y < g.ny; y++) {
                     for (int x = 0; x < g.nx; x++) {
                        int ix = g.index(x, y, z);
                        if (g.color[ix] != 0 && g.part[ix] == p) {
                           x0 = Math.min(x0, x);
                           x1 = Math.max(x1, x);
                           y0 = Math.min(y0, y);
                           y1 = Math.max(y1, y);
                        }
                     }
                  }

                  if (x1 >= 0) {
                     int w = x1 - x0 + 1;
                     int h = y1 - y0 + 1;
                     byte[] slots = new byte[w * h];

                     for (int r = 0; r < h; r++) {
                        for (int c = 0; c < w; c++) {
                           int ix = g.index(x0 + c, y1 - r, z);
                           if (g.color[ix] != 0 && g.part[ix] == p) {
                              int key = g.color[ix] & 16777215 | (energy >= 0 && g.part[ix] == energy ? 16777216 : 0);
                              slots[r * w + c] = (byte)slotOf.get(key).intValue();
                           }
                        }
                     }

                     String name = String.format(Locale.ROOT, "%s_%03d.png", g.parts.get(p).replace(' ', '_'), z);
                     writeIndexed(new File(out, name), slots, w, h, previewPalette);
                     JsonObject lo = new JsonObject();
                     lo.addProperty("part", g.parts.get(p));
                     lo.addProperty("sprite", name);
                     lo.addProperty("plane", "front");
                     JsonArray at = new JsonArray();
                     at.add(new JsonPrimitive(g.ox + x0));
                     at.add(new JsonPrimitive(g.oy + y1 + 1.0F));
                     at.add(new JsonPrimitive(g.oz + z + 0.5F));
                     lo.add("at", at);
                     lo.addProperty("depth", 1);
                     layers.add(lo);
                     sprites++;
                  }
               }
            }

            if (sprites == 0) {
               throw new PixelImport.ImportException("it has no voxels to draw");
            } else {
               json.add("layers", layers);
               byte[] icon = icon(g, slotOf, energy, 32);
               byte[] icon16 = icon(g, slotOf, energy, 16);
               writeIndexed(new File(out, "icon.png"), icon, 32, 32, previewPalette);
               writeIndexed(new File(out, "icon16.png"), icon16, 16, 16, previewPalette);
               json.addProperty("icon", "icon.png");
               json.addProperty("icon16", "icon16.png");

               try (OutputStreamWriter wr = new OutputStreamWriter(Files.newOutputStream(new File(out, "pixel.json").toPath()), StandardCharsets.UTF_8)) {
                  new GsonBuilder().setPrettyPrinting().create().toJson(json, wr);
               }

               File old = new File(dir, "pixel");
               File oldJson = new File(old, "pixel.json");
               if (oldJson.isFile() && !madeHere(oldJson)) {
                  deleteGenerated(out);
                  throw new PixelImport.ImportException("its folder already has pixel files of its own (pixel/pixel.json): those are kept");
               } else {
                  deleteGenerated(old);
                  if (!out.renameTo(old)) {
                     throw new IOException("could not put the pixel files in place");
                  } else {
                     return new float[]{g.ox * pixel, g.oy * pixel, g.oz * pixel, (g.ox + g.nx) * pixel, (g.oy + g.ny) * pixel, (g.oz + g.nz) * pixel};
                  }
               }
            }
         }
      }
   }

   static Map<String, String> refreshAll(File importFolder, boolean autoPixelate, List<String> made) {
      LinkedHashMap<String, String> problems = new LinkedHashMap<>();
      File[] dirs = importFolder.listFiles();
      if (dirs == null) {
         return problems;
      } else {
         for (File dir : dirs) {
            if (dir.isDirectory()) {
               try {
                  if (refresh(dir) || autoPixelate && pixelateObj(dir)) {
                     made.add(dir.getName());
                  }
               } catch (PixelImport.ImportException var10) {
                  problems.put(dir.getName(), var10.getMessage());
               } catch (RuntimeException | IOException var11) {
                  problems.put(dir.getName(), "its pixel files could not be made (" + var11.getMessage() + ")");
               }
            }
         }

         return problems;
      }
   }

   static boolean refresh(File dir) throws IOException, PixelImport.ImportException {
      File source = null;
      String kind = null;

      for (String k : new String[]{"bbmodel", "json", "vox"}) {
         File f = new File(dir, "pixel." + k);
         if (f.isFile()) {
            source = f;
            kind = k;
            break;
         }
      }

      if (source == null) {
         return false;
      } else {
         String sig = signature(source);
         File pixelJson = new File(dir, "pixel" + File.separator + "pixel.json");
         String made;
         if (!pixelJson.isFile() || (made = madeFrom(pixelJson)) != null && !made.equals(sig)) {
            if (source.length() > 16777216L) {
               throw new PixelImport.ImportException("pixel." + kind + " is bigger than " + 16L + " MB");
            } else {
               byte[] bytes = Files.readAllBytes(source.toPath());
               LinkedHashMap<String, byte[]> images = new LinkedHashMap<>();
               File[] files = dir.listFiles();
               if (files != null) {
                  for (File f : files) {
                     if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".png") && f.length() <= 4194304L) {
                        images.put(f.getName(), Files.readAllBytes(f.toPath()));
                     }
                  }
               }

               ArrayList<String> notes = new ArrayList<>();
               PixelImport.Grid g = "vox".equals(kind)
                  ? readVox(bytes, notes)
                  : ("bbmodel".equals(kind) ? readBlockbench(bytes, images, notes) : readItemModel(bytes, images, notes));
               float[] bounds = write(g, dir, g.unit, 80, sig);
               File wj = new File(dir, "weapon.json");
               JsonElement e;
               if (wj.isFile()
                  && wj.length() < 1048576L
                  && (e = new JsonParser().parse(new String(Files.readAllBytes(wj.toPath()), StandardCharsets.UTF_8))).isJsonObject()
                  && e.getAsJsonObject().has("import")
                  && e.getAsJsonObject().get("import").isJsonObject()) {
                  JsonObject o = e.getAsJsonObject();
                  JsonObject imp = o.getAsJsonObject("import");
                  imp.add("min", WeaponImport.array(bounds[0], bounds[1], bounds[2]));
                  imp.add("max", WeaponImport.array(bounds[3], bounds[4], bounds[5]));
                  WeaponImport.applyGrip(o);
                  WeaponImport.write(wj, o);
               }

               return true;
            }
         } else {
            return false;
         }
      }
   }

   static String signature(File source) {
      return source.getName() + " " + source.length() + " " + source.lastModified();
   }

   static boolean madeHere(File pixelJson) {
      try {
         byte[] b = Files.readAllBytes(pixelJson.toPath());
         if (b.length > 524288) {
            return false;
         } else {
            JsonElement e = new JsonParser().parse(new String(b, StandardCharsets.UTF_8));
            return e.isJsonObject() && e.getAsJsonObject().has("madeFrom");
         }
      } catch (RuntimeException | IOException var3) {
         return false;
      }
   }

   static String madeFrom(File pixelJson) {
      try {
         byte[] b = Files.readAllBytes(pixelJson.toPath());
         if (b.length > 524288) {
            return null;
         } else {
            JsonElement e = new JsonParser().parse(new String(b, StandardCharsets.UTF_8));
            return e.isJsonObject() && e.getAsJsonObject().has("madeFrom") ? e.getAsJsonObject().get("madeFrom").getAsString() : null;
         }
      } catch (RuntimeException | IOException var3) {
         return null;
      }
   }

   static void deleteGenerated(File folder) {
      File[] files = folder.listFiles();
      if (files != null) {
         for (File f : files) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (f.isFile() && (n.endsWith(".png") || n.equals("pixel.json"))) {
               f.delete();
            }
         }

         String[] left = folder.list();
         if (left != null && left.length == 0) {
            folder.delete();
         }
      }
   }

   private static Map<Integer, Integer> quantize(Map<Integer, int[]> counts, int limit) {
      LinkedHashMap<Integer, Integer> out = new LinkedHashMap<>();
      ArrayList<Integer> keys = new ArrayList<>(counts.keySet());
      if (keys.size() <= limit) {
         for (Integer k : keys) {
            out.put(k, k);
         }

         return out;
      } else {
         for (int glow = 0; glow < 2; glow++) {
            ArrayList<Integer> set = new ArrayList<>();

            for (Integer k : keys) {
               if (k >>> 24 == glow) {
                  set.add(k);
               }
            }

            if (!set.isEmpty()) {
               int n = Math.max(1, Math.round((float)(limit * set.size()) / keys.size()));
               double[][] lab = new double[set.size()][];
               double[] weight = new double[set.size()];

               for (int i2 = 0; i2 < set.size(); i2++) {
                  lab[i2] = PixelPalette.Ramps.lab(set.get(i2) & 16777215);
                  weight[i2] = counts.get(set.get(i2))[0];
               }

               int[] assign = kMeans(lab, weight, Math.min(n, set.size()));
               HashMap<Integer, Integer> best = new HashMap<>();

               for (int i = 0; i < set.size(); i++) {
                  Integer b = best.get(assign[i]);
                  if (b == null || weight[i] > counts.get(set.get(b))[0]) {
                     best.put(assign[i], i);
                  }
               }

               for (int var14 = 0; var14 < set.size(); var14++) {
                  out.put(set.get(var14), set.get(best.get(assign[var14])));
               }
            }
         }

         return out;
      }
   }

   private static int[] kMeans(double[][] p, double[] w, int k) {
      Random random = new Random(20261006L);
      int n = p.length;
      double[][] c = new double[k][];
      c[0] = (double[])p[0].clone();
      double[] d = new double[n];

      for (int j = 1; j < k; j++) {
         double sum = 0.0;

         for (int i = 0; i < n; i++) {
            double m = Double.MAX_VALUE;

            for (int q = 0; q < j; q++) {
               m = Math.min(m, dist(p[i], c[q]));
            }

            d[i] = m * w[i];
            sum += d[i];
         }

         double r = random.nextDouble() * sum;
         int pick = n - 1;

         for (int i = 0; i < n; i++) {
            if ((r -= d[i]) <= 0.0) {
               pick = i;
               break;
            }
         }

         c[j] = (double[])p[pick].clone();
      }

      int[] assign = new int[n];

      for (int it = 0; it < 12; it++) {
         for (int ix = 0; ix < n; ix++) {
            int bi = 0;
            double bd = Double.MAX_VALUE;

            for (int q = 0; q < k; q++) {
               double dd = dist(p[ix], c[q]);
               if (dd < bd) {
                  bd = dd;
                  bi = q;
               }
            }

            assign[ix] = bi;
         }

         double[][] s = new double[k][3];
         double[] ws = new double[k];

         for (int ix = 0; ix < n; ix++) {
            for (int a = 0; a < 3; a++) {
               double[] dArray = s[assign[ix]];
               dArray[a] += p[ix][a] * w[ix];
            }

            int n3 = assign[ix];
            ws[n3] += w[ix];
         }

         for (int q2 = 0; q2 < k; q2++) {
            if (ws[q2] > 0.0) {
               for (int a = 0; a < 3; a++) {
                  c[q2][a] = s[q2][a] / ws[q2];
               }
            }
         }
      }

      return assign;
   }

   private static double dist(double[] a, double[] b) {
      double l = a[0] - b[0];
      double x = a[1] - b[1];
      double y = a[2] - b[2];
      return l * l + x * x + y * y;
   }

   private static Map<Integer, Integer> ramps(Map<Integer, Integer> to, List<int[]> ramps, List<Boolean> glows) {
      ArrayList<Integer> kept = new ArrayList<>(new LinkedHashSet<>(to.values()));
      HashMap<Integer, Integer> slotOfKept = new HashMap<>();

      for (int glow = 0; glow < 2; glow++) {
         for (int grey = 1; grey >= 0; grey--) {
            ArrayList<double[]> set = new ArrayList<>();

            for (Integer k : kept) {
               double[] lab;
               if (k >>> 24 == glow && Math.hypot((lab = PixelPalette.Ramps.lab(k & 16777215))[1], lab[2]) < 0.04 == (grey == 1)) {
                  set.add(new double[]{k.intValue(), lab[0], Math.atan2(lab[2], lab[1])});
               }
            }

            if (!set.isEmpty()) {
               final boolean byLightness = grey == 1;
               Collections.sort(set, new Comparator<double[]>() {
                  public int compare(double[] a, double[] b) {
                     return byLightness ? Double.compare(a[1], b[1]) : Double.compare(a[2], b[2]);
                  }
               });
               ArrayList<double[]> ramp = new ArrayList<>();

               for (int i = 0; i <= set.size(); i++) {
                  double[] c = i < set.size() ? set.get(i) : null;
                  boolean fits = c != null
                     && ramp.size() < 5
                     && (
                        byLightness
                           || ramp.isEmpty()
                           || Math.abs(Math.atan2(Math.sin(c[2] - ramp.get(0)[2]), Math.cos(c[2] - ramp.get(0)[2]))) < Math.toRadians(30.0)
                     );
                  if (!fits && !ramp.isEmpty()) {
                     if (ramps.size() >= 16) {
                        return null;
                     }

                     Collections.sort(ramp, new Comparator<double[]>() {
                        public int compare(double[] a, double[] b) {
                           return Double.compare(a[1], b[1]);
                        }
                     });
                     int[] shades = new int[5];

                     for (int s = 0; s < 5; s++) {
                        shades[s] = (int)ramp.get(Math.min(s, ramp.size() - 1))[0] & 16777215;
                     }

                     int f = ramps.size();
                     ramps.add(shades);
                     glows.add(glow == 1);

                     for (int s = 0; s < ramp.size(); s++) {
                        slotOfKept.put((int)ramp.get(s)[0], PixelModel.slot(PixelModel.MATERIAL_NAMES.length + f, s));
                     }

                     ramp.clear();
                  }

                  if (c != null) {
                     ramp.add(c);
                  }
               }
            }
         }
      }

      HashMap<Integer, Integer> out = new HashMap<>();

      for (Entry<Integer, Integer> e : to.entrySet()) {
         out.put(e.getKey(), slotOfKept.get(e.getValue()));
      }

      return out;
   }

   private static byte[] icon(PixelImport.Grid g, Map<Integer, Integer> slotOf, int energy, int size) {
      float[] line = handleLine(g);
      double[] up = new double[]{line[3], line[4], line[5]};
      boolean fromSide = Math.abs(up[2]) > 0.9;
      double[] dArray;
      if (fromSide) {
         double[] dArray2 = new double[]{1.0, 0.0, 0.0};
         dArray = dArray2;
         dArray2[2] = 0.0;
      } else {
         double[] dArray3 = new double[]{0.0, 0.0, 0.0};
         dArray = dArray3;
         dArray3[2] = 1.0;
      }

      double[] right = new double[]{up[1] * dArray[2] - up[2] * dArray[1], up[2] * dArray[0] - up[0] * dArray[2], up[0] * dArray[1] - up[1] * dArray[0]};
      boolean square = Math.abs(up[0]) > 0.5 && Math.abs(up[1]) > 0.5;
      double r2 = Math.sqrt(0.5);
      double du0 = 1.0E9;
      double du1 = -1.0E9;
      double dv0 = 1.0E9;
      double dv1 = -1.0E9;

      for (int z = 0; z < g.nz; z++) {
         for (int y = 0; y < g.ny; y++) {
            for (int x = 0; x < g.nx; x++) {
               if (g.color[g.index(x, y, z)] != 0) {
                  double pu = (x + 0.5) * up[0] + (y + 0.5) * up[1] + (z + 0.5) * up[2];
                  double pw = (x + 0.5) * right[0] + (y + 0.5) * right[1] + (z + 0.5) * right[2];
                  double du = (pu + pw) * r2;
                  double dv = (pw - pu) * r2;
                  du0 = Math.min(du0, du);
                  du1 = Math.max(du1, du);
                  dv0 = Math.min(dv0, dv);
                  dv1 = Math.max(dv1, dv);
               }
            }
         }
      }

      double half = square ? 0.5 : r2;
      double var54;
      double var55;
      double wd = (var55 = du1 + half) - (var54 = du0 - half);
      double var56;
      double var57;
      double hd = (var57 = dv1 + half) - (var56 = dv0 - half);
      double s = (size - 2) / Math.max(wd, hd);
      double left = (size - s * wd) / 2.0;
      double top = (size - s * hd) / 2.0;
      if (square && s >= 1.0) {
         s = Math.floor(s);
         left = Math.floor((size - s * wd) / 2.0);
         top = Math.floor((size - s * hd) / 2.0);
      }

      byte[] out = new byte[size * size];

      for (int j = 0; j < size; j++) {
         for (int i = 0; i < size; i++) {
            double du = var54 + (i + 0.5 - left) / s;
            double dv = var56 + (j + 0.5 - top) / s;
            double pu = (du - dv) * r2;
            double pw = (du + dv) * r2;
            int xx = (int)Math.floor(pu * up[0] + pw * right[0]);
            int y = (int)Math.floor(pu * up[1] + pw * right[1]);
            int z = (int)Math.floor(pu * up[2] + pw * right[2]);

            for (int d = (fromSide ? g.nx : g.nz) - 1; d >= 0; d--) {
               int vx = fromSide ? d : xx;
               int vz;
               int c = g.at(vx, y, vz = fromSide ? z : d);
               if (c != 0) {
                  byte p = g.part[g.index(vx, y, vz)];
                  out[j * size + i] = (byte)slotOf.get(c & 16777215 | (energy >= 0 && p == energy ? 16777216 : 0)).intValue();
                  break;
               }
            }
         }
      }

      byte[] lined = (byte[])out.clone();

      for (int j = 0; j < size; j++) {
         for (int i = 0; i < size; i++) {
            if (out[j * size + i] == 0) {
               for (int[] nb : new int[][]{{i - 1, j}, {i + 1, j}, {i, j - 1}, {i, j + 1}}) {
                  if (nb[0] >= 0 && nb[1] >= 0 && nb[0] < size && nb[1] < size && out[nb[1] * size + nb[0]] != 0) {
                     int slot = out[nb[1] * size + nb[0]] & 255;
                     lined[j * size + i] = (byte)PixelModel.slot(PixelModel.material(slot), 0);
                     break;
                  }
               }
            }
         }
      }

      return lined;
   }

   private static void writeIndexed(File f, byte[] slots, int w, int h, int[] palette) throws IOException {
      byte[] r = new byte[256];
      byte[] gr = new byte[256];
      byte[] b = new byte[256];
      byte[] a = new byte[256];

      for (int i = 1; i < 256; i++) {
         r[i] = (byte)(palette[i] >> 16);
         gr[i] = (byte)(palette[i] >> 8);
         b[i] = (byte)palette[i];
         a[i] = -1;
      }

      IndexColorModel cm = new IndexColorModel(8, 256, r, gr, b, a);
      BufferedImage img = new BufferedImage(w, h, 13, cm);
      img.getRaster().setDataElements(0, 0, w, h, slots);
      if (!ImageIO.write(img, "png", f)) {
         throw new IOException("could not write " + f.getName());
      }
   }

   private static String hex(int rgb) {
      return String.format(Locale.ROOT, "#%06X", rgb & 16777215);
   }

   static final class Cube {
      final double[] from = new double[3];
      final double[] to = new double[3];
      final double[] origin = new double[3];
      double[] rot = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0};
      final PixelImport.Face[] faces = new PixelImport.Face[6];
      int part;
   }

   static final class Face {
      PixelImport.Texture texture;
      double[] uv;
      int rotation;
   }

   static final class Grid {
      final int nx;
      final int ny;
      final int nz;
      final int[] color;
      final byte[] part;
      final List<String> parts = new ArrayList<>();
      final Map<String, float[]> pivots = new LinkedHashMap<>();
      float ox;
      float oy;
      float oz;
      float unit = 0.03125F;

      Grid(int nx, int ny, int nz) throws PixelImport.ImportException {
         if (nx <= 0 || ny <= 0 || nz <= 0) {
            throw new PixelImport.ImportException("it has nothing in it");
         } else if (nx <= 256 && ny <= 256 && nz <= 256) {
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.color = new int[nx * ny * nz];
            this.part = new byte[nx * ny * nz];
            this.parts.add("body");
         } else {
            throw new PixelImport.ImportException("it is " + nx + " x " + ny + " x " + nz + " voxels (at most " + 256 + " on a side)");
         }
      }

      int index(int x, int y, int z) {
         return (z * this.ny + y) * this.nx + x;
      }

      int at(int x, int y, int z) {
         return x >= 0 && y >= 0 && z >= 0 && x < this.nx && y < this.ny && z < this.nz ? this.color[this.index(x, y, z)] : 0;
      }

      int partOf(String name) {
         int i = this.parts.indexOf(name);
         if (i < 0 && this.parts.size() < 16) {
            this.parts.add(name);
            i = this.parts.size() - 1;
         }

         return Math.max(0, i);
      }

      void put(int x, int y, int z, int argb, int p) {
         if (x >= 0 && y >= 0 && z >= 0 && x < this.nx && y < this.ny && z < this.nz) {
            int i = this.index(x, y, z);
            this.color[i] = argb >>> 24 < 128 ? 0 : 0xFF000000 | argb;
            this.part[i] = (byte)p;
         }
      }

      int count() {
         int n = 0;

         for (int c : this.color) {
            if (c != 0) {
               n++;
            }
         }

         return n;
      }
   }

   static final class ImportException extends Exception {
      ImportException(String message) {
         super(message);
      }
   }

   static final class Texture {
      final int[] argb;
      final int w;
      final int h;
      double perU = 1.0;
      double perV = 1.0;

      Texture(int[] argb, int w, int h) {
         this.argb = argb;
         this.w = w;
         this.h = h;
      }
   }
}
