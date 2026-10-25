package meowtils.furiahvisuals.module.weapons;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

final class WeaponCheck {
   static final int MAX_TRIANGLES = 10000;

   private WeaponCheck() {
   }

   static WeaponCheck.Result check(WeaponDef def) {
      WeaponCheck.Result r = new WeaponCheck.Result(def.id);
      HashMap<String, List<float[]>> hdTris = new HashMap<>();
      Set<String> hdParts = hdParts(def, r, def.pivots.keySet(), hdTris);

      PixelModel m;
      try {
         m = PixelModel.read(def);
      } catch (RuntimeException | IOException var19) {
         r.problems.add("Pixelated: " + var19.getMessage());
         return r;
      }

      if (m == null) {
         r.problems.add("Pixelated: missing (no pixel/pixel.json)");
         return r;
      } else {
         r.hasPixel = true;
         long t0 = System.nanoTime();
         PixelMesh near = PixelMesh.build(m, false);
         PixelMesh far = PixelMesh.build(m, true);
         r.buildMs = (float)(System.nanoTime() - t0) / 1000000.0F;
         r.triangles = near.triangles;
         r.farTriangles = far.triangles;
         if (near.triangles > 10000) {
            r.problems.add("Pixelated: " + near.triangles + " triangles after merging (at most " + 10000 + ")");
         }

         LinkedHashSet<String> pxParts = new LinkedHashSet<>();

         for (PixelModel.Layer l : m.layers) {
            pxParts.add(l.part);
            if (!hdParts.isEmpty() && !hdParts.contains(l.part)) {
               r.problems.add("Pixelated: part \"" + l.part + "\" is not one of the HD model's " + hdParts);
            }
         }

         for (String moving : def.pivots.keySet()) {
            if (!pxParts.contains(moving)) {
               r.problems.add("Pixelated: the moving part \"" + moving + "\" has no pixels");
            }
         }

         for (String stretched : m.stretch.keySet()) {
            if (!def.pivots.containsKey(stretched) || !pxParts.contains(stretched)) {
               r.problems.add("Pixelated: \"stretch\" names \"" + stretched + "\", which is not a moving part with pixels");
            }
         }

         float px = m.pixel;
         float len = near.max[1] - near.min[1];
         float hdLen = def.length();
         if (Math.abs(len - hdLen) > hdLen * 0.03F) {
            r.problems
               .add(
                  String.format(
                     Locale.ROOT, "Pixelated: %.1f pixels long, the HD model %.1f (%.1f%%; at most 3%%)", len / px, hdLen / px, (len - hdLen) / hdLen * 100.0F
                  )
               );
         }

         LinkedHashSet<String> handles = new LinkedHashSet<>();

         for (Entry<String, WeaponDef.Role> entry : def.parts.entrySet()) {
            if (entry.getValue() == WeaponDef.Role.HANDLE) {
               handles.add(entry.getKey());
            }
         }

         checkGrip(r, m, "right", def.right, handles, px);
         if (def.left != null && !def.isBow()) {
            checkGrip(r, m, "left", def.left, handles, px);
         }

         for (Entry<String, WeaponDef.Pivot> entryx : def.pivots.entrySet()) {
            float[] o = entryx.getValue().origin;
            float d = nearest(m, entryx.getKey(), o[0] / px, o[1] / px, o[2] / px);
            float hdD;
            if (d > 1.0F && d < Float.MAX_VALUE && Math.abs(d - (hdD = hdDistance(hdTris.get(entryx.getKey()), o) / px)) > 1.0F) {
               r.problems.add(String.format(Locale.ROOT, "Pixelated: the pivot of \"%s\" is %.1f pixels from its pixels (HD %.1f)", entryx.getKey(), d, hdD));
            }
         }

         if (m.icon == null) {
            r.problems.add("Pixelated: no 32 x 32 icon");
         }

         if (m.icon16 == null) {
            r.problems.add("Pixelated: no 16 x 16 icon");
         }

         return r;
      }
   }

   private static Set<String> hdParts(WeaponDef def, WeaponCheck.Result r, Set<String> wanted, Map<String, List<float[]>> tris) {
      LinkedHashSet<String> parts = new LinkedHashSet<>();
      if (def.isVanilla()) {
         return parts;
      } else {
         try {
            InputStream in = def.open("model.obj");
            if (in == null) {
               r.problems.add("HD: model.obj is missing");
               return parts;
            }

            try {
               BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
               ArrayList<float[]> verts = new ArrayList<>();
               List<float[]> into = null;

               String line;
               while ((line = br.readLine()) != null) {
                  if (line.startsWith("g ") || line.startsWith("o ")) {
                     String g = line.substring(2).trim();
                     parts.add(g);
                     into = null;
                     if (wanted.contains(g) && (into = tris.get(g)) == null) {
                        into = new ArrayList<>();
                        tris.put(g, into);
                     }
                  } else if (line.startsWith("v ")) {
                     String[] t = line.trim().split("\\s+");
                     if (t.length >= 4) {
                        verts.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                     }
                  } else if (into != null && line.startsWith("f ")) {
                     String[] t = line.trim().split("\\s+");
                     int[] idx = new int[t.length - 1];

                     for (int i = 1; i < t.length; i++) {
                        int k = Integer.parseInt(t[i].split("/")[0]);
                        idx[i - 1] = k < 0 ? verts.size() + k : k - 1;
                     }

                     for (int var33 = 1; var33 + 1 < idx.length; var33++) {
                        float[] a = verts.get(idx[0]);
                        float[] b = verts.get(idx[var33]);
                        float[] c = verts.get(idx[var33 + 1]);
                        into.add(new float[]{a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]});
                     }
                  }
               }
            } catch (RuntimeException var20) {
               r.problems.add("HD: model.obj can't be read (" + var20.getMessage() + ")");
            } finally {
               in.close();
            }

            for (String lod : new String[]{"model_lod1.obj", "model_lod2.obj"}) {
               InputStream l = def.open(lod);
               if (l == null) {
                  r.problems.add("HD: " + lod + " is missing");
               } else {
                  l.close();
               }
            }

            for (String kind : new String[]{"albedo", "material", "normal"}) {
               String name = def.texture(kind);
               InputStream t = name == null ? null : def.open(name);
               if (t == null) {
                  r.problems.add("HD: its " + kind + " map is missing");
               } else {
                  t.close();
               }
            }
         } catch (IOException var22) {
            r.problems.add("HD: " + var22.getMessage());
         }

         for (String part : def.parts.keySet()) {
            if (!parts.contains(part)) {
               r.problems.add("HD: weapon.json names a part \"" + part + "\" the model doesn't have");
            }
         }

         return parts;
      }
   }

   private static void checkGrip(WeaponCheck.Result r, PixelModel m, String which, WeaponDef.GripPoint g, Set<String> handles, float px) {
      float[] p = new float[]{g.x / px, g.y / px, g.z / px};
      int a = Math.abs(g.ax) > Math.abs(g.ay) && Math.abs(g.ax) > Math.abs(g.az) ? 0 : (Math.abs(g.az) > Math.abs(g.ay) ? 2 : 1);
      int u = (a + 1) % 3;
      int w = (a + 2) % 3;
      float reach = 2.5F;
      float u0 = Float.MAX_VALUE;
      float u1 = -Float.MAX_VALUE;
      float w0 = Float.MAX_VALUE;
      float w1 = -Float.MAX_VALUE;

      for (PixelModel.Layer l : m.layers) {
         if (handles.contains(l.part)) {
            for (int row = 0; row < l.h; row++) {
               for (int c = 0; c < l.w; c++) {
                  float[] b;
                  if (l.slot(c, row) != 0
                     && !(p[a] < (b = box(l, c, row))[a])
                     && !(p[a] > b[3 + a])
                     && !((b[u] + b[3 + u]) / 2.0F - p[u] > reach)
                     && !(p[u] - (b[u] + b[3 + u]) / 2.0F > reach)
                     && !((b[w] + b[3 + w]) / 2.0F - p[w] > reach)
                     && !(p[w] - (b[w] + b[3 + w]) / 2.0F > reach)) {
                     u0 = Math.min(u0, b[u]);
                     u1 = Math.max(u1, b[3 + u]);
                     w0 = Math.min(w0, b[w]);
                     w1 = Math.max(w1, b[3 + w]);
                  }
               }
            }
         }
      }

      if (u0 > u1) {
         r.problems.add("Pixelated: no handle pixels at the " + which + " grip");
      } else {
         float du = (u0 + u1) / 2.0F - p[u];
         float dw = (w0 + w1) / 2.0F - p[w];
         if (Math.abs(du) > 1.0F || Math.abs(dw) > 1.0F) {
            r.problems.add(String.format(Locale.ROOT, "Pixelated: the %s grip is %.1f, %.1f pixels off the handle's middle", which, du, dw));
         }
      }
   }

   private static float hdDistance(List<float[]> tris, float[] p) {
      float best = Float.MAX_VALUE;
      if (tris == null) {
         return best;
      } else {
         for (float[] t : tris) {
            best = Math.min(best, pointTriangle(p, t));
         }

         return best;
      }
   }

   private static float pointTriangle(float[] p, float[] t) {
      float abx = t[3] - t[0];
      float aby = t[4] - t[1];
      float abz = t[5] - t[2];
      float acx = t[6] - t[0];
      float acy = t[7] - t[1];
      float acz = t[8] - t[2];
      float d1 = abx * (p[0] - t[0]) + aby * (p[1] - t[1]) + abz * (p[2] - t[2]);
      float d2 = acx * (p[0] - t[0]) + acy * (p[1] - t[1]) + acz * (p[2] - t[2]);
      float d3 = abx * (p[0] - t[3]) + aby * (p[1] - t[4]) + abz * (p[2] - t[5]);
      float d4 = acx * (p[0] - t[3]) + acy * (p[1] - t[4]) + acz * (p[2] - t[5]);
      float d5 = abx * (p[0] - t[6]) + aby * (p[1] - t[7]) + abz * (p[2] - t[8]);
      float d6 = acx * (p[0] - t[6]) + acy * (p[1] - t[7]) + acz * (p[2] - t[8]);
      float vc = d1 * d4 - d3 * d2;
      float vb = d5 * d2 - d1 * d6;
      float va = d3 * d6 - d5 * d4;
      float w;
      float v;
      if (d1 <= 0.0F && d2 <= 0.0F) {
         v = 0.0F;
         w = 0.0F;
      } else if (d3 >= 0.0F && d4 <= d3) {
         v = 1.0F;
         w = 0.0F;
      } else if (vc <= 0.0F && d1 >= 0.0F && d3 <= 0.0F) {
         v = d1 / (d1 - d3);
         w = 0.0F;
      } else if (d6 >= 0.0F && d5 <= d6) {
         v = 0.0F;
         w = 1.0F;
      } else if (vb <= 0.0F && d2 >= 0.0F && d6 <= 0.0F) {
         v = 0.0F;
         w = d2 / (d2 - d6);
      } else if (va <= 0.0F && d4 - d3 >= 0.0F && d5 - d6 >= 0.0F) {
         w = (d4 - d3) / (d4 - d3 + (d5 - d6));
         v = 1.0F - w;
      } else {
         float den = 1.0F / (va + vb + vc);
         v = vb * den;
         w = vc * den;
      }

      float qx = t[0] + abx * v + acx * w - p[0];
      float qy = t[1] + aby * v + acy * w - p[1];
      float qz = t[2] + abz * v + acz * w - p[2];
      return (float)Math.sqrt(qx * qx + qy * qy + qz * qz);
   }

   private static float nearest(PixelModel m, String part, float x, float y, float z) {
      float best = Float.MAX_VALUE;

      for (PixelModel.Layer l : m.layers) {
         if (l.part.equals(part)) {
            for (int row = 0; row < l.h; row++) {
               for (int c = 0; c < l.w; c++) {
                  if (l.slot(c, row) != 0) {
                     float[] b = box(l, c, row);
                     float dx = Math.max(0.0F, Math.max(b[0] - x, x - b[3]));
                     float dy = Math.max(0.0F, Math.max(b[1] - y, y - b[4]));
                     float dz = Math.max(0.0F, Math.max(b[2] - z, z - b[5]));
                     best = Math.min(best, (float)Math.sqrt(dx * dx + dy * dy + dz * dz));
                  }
               }
            }
         }
      }

      return best;
   }

   private static float[] box(PixelModel.Layer l, int c, int r) {
      int d = l.depth(c, r);
      switch (l.plane) {
         case FRONT:
            return new float[]{l.x + c, l.y - r - 1.0F, l.z - d / 2.0F, l.x + c + 1.0F, l.y - r, l.z + d / 2.0F};
         case SIDE:
            return new float[]{l.x - d / 2.0F, l.y - r - 1.0F, l.z + c, l.x + d / 2.0F, l.y - r, l.z + c + 1.0F};
         default:
            return new float[]{l.x + c, l.y - d / 2.0F, l.z + r, l.x + c + 1.0F, l.y + d / 2.0F, l.z + r + 1.0F};
      }
   }

   static final class Result {
      final String id;
      final List<String> problems = new ArrayList<>();
      int triangles;
      int farTriangles;
      float buildMs;
      boolean hasPixel;

      Result(String id) {
         this.id = id;
      }
   }
}
