package meowtils.furiahvisuals.module.weapons;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

final class ObjModel {
   final Map<String, ObjModel.Part> parts = new LinkedHashMap<>();
   final float[] min = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
   final float[] max = new float[]{-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
   static final String STATIC = "#static";

   int triangles() {
      int n = 0;

      for (ObjModel.Part p : this.parts.values()) {
         n += p.triangles();
      }

      return n;
   }

   static ObjModel read(InputStream in, int maxTriangles) throws IOException {
      ObjModel m = new ObjModel();
      ArrayList<float[]> v = new ArrayList<>();
      ArrayList<float[]> vn = new ArrayList<>();
      ArrayList<float[]> vt = new ArrayList<>();
      ObjModel.Part part = null;
      int tris = 0;
      BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      int[] vi = new int[64];
      int[] ti = new int[64];
      int[] ni = new int[64];
      float[] zero2 = new float[]{0.0F, 0.0F};

      String line;
      while ((line = r.readLine()) != null) {
         if (!line.isEmpty() && line.charAt(0) != '#') {
            String[] t = line.trim().split("\\s+");
            String var15 = t[0];
            switch (var15) {
               case "v":
                  float[] p2 = new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])};
                  v.add(p2);

                  for (int k = 0; k < 3; k++) {
                     m.min[k] = Math.min(m.min[k], p2[k]);
                     m.max[k] = Math.max(m.max[k], p2[k]);
                  }
                  break;
               case "vn":
                  vn.add(new float[]{Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])});
                  break;
               case "vt":
                  vt.add(new float[]{Float.parseFloat(t[1]), 1.0F - (t.length > 2 ? Float.parseFloat(t[2]) : 0.0F)});
                  break;
               case "g":
               case "o":
                  String name = t.length > 1 ? t[1] : "default";
                  part = m.parts.get(name);
                  if (part == null) {
                     part = new ObjModel.Part(name);
                     m.parts.put(name, part);
                  }
                  break;
               case "f":
                  int n = Math.min(64, t.length - 1);
                  if (n >= 3) {
                     for (int k = 0; k < n; k++) {
                        String[] c = t[k + 1].split("/", -1);
                        vi[k] = index(c[0], v.size());
                        ti[k] = c.length > 1 && !c[1].isEmpty() ? index(c[1], vt.size()) : -1;
                        ni[k] = c.length > 2 && !c[2].isEmpty() ? index(c[2], vn.size()) : -1;
                     }

                     if (part == null) {
                        part = new ObjModel.Part("default");
                        m.parts.put("default", part);
                     }

                     for (int var26 = 1; var26 + 1 < n; var26++) {
                        int a = 0;
                        int c = var26 + 1;
                        float[] pa = v.get(vi[a]);
                        float[] pb = v.get(vi[var26]);
                        float[] pc = v.get(vi[c]);
                        float[] flat = null;
                        if (ni[a] < 0 || ni[var26] < 0 || ni[c] < 0) {
                           flat = flatNormal(pa, pb, pc);
                        }

                        part.corner(pa, ni[a] >= 0 ? vn.get(ni[a]) : flat, ti[a] >= 0 ? vt.get(ti[a]) : zero2);
                        part.corner(pb, ni[var26] >= 0 ? vn.get(ni[var26]) : flat, ti[var26] >= 0 ? vt.get(ti[var26]) : zero2);
                        part.corner(pc, ni[c] >= 0 ? vn.get(ni[c]) : flat, ti[c] >= 0 ? vt.get(ti[c]) : zero2);
                        if (++tris > maxTriangles) {
                           throw new IOException("The model has more than " + maxTriangles + " triangles");
                        }
                     }
                  }
            }
         }
      }

      m.parts.values().removeIf(p -> p.floats == 0);
      return m;
   }

   void bakeChain(String name, float[] path) {
      ObjModel.Part src = this.parts.get(name);
      if (src != null && path != null) {
         ObjModel.Part out = new ObjModel.Part(name + "#static");
         float len = 2.0F * (path[2] - path[1]) + (float) (Math.PI * 2) * path[3];
         float[] c = new float[3];
         float[] t = new float[3];
         float[] o = new float[3];
         float[] p = new float[3];
         float[] n = new float[3];
         float[] uv = new float[2];

         for (int i = 0; i + 8 <= src.floats; i += 8) {
            float[] d = src.data;
            chainPoint(d[i + 6] * len, path, c, t, o);

            for (int k = 0; k < 3; k++) {
               p[k] = c[k] + t[k] * d[i] + o[k] * d[i + 1];
               n[k] = t[k] * d[i + 3] + o[k] * d[i + 4];
            }

            p[2] += d[i + 2];
            n[2] += d[i + 5];
            uv[0] = d[i + 6];
            uv[1] = d[i + 7];
            out.corner(p, n, uv);
         }

         this.parts.put(out.name, out);
      }
   }

   private static void chainPoint(float s, float[] path, float[] c, float[] t, float[] o) {
      float x0 = path[0];
      float y1 = path[2];
      float y0 = path[1];
      float d = y1 - y0;
      float r = path[3];
      float half = (float) Math.PI * r;
      float len = 2.0F * d + 2.0F * half;
      if ((s = (s % len + len) % len) < d) {
         set(t, 0.0F, 1.0F, 0.0F);
         set(o, 1.0F, 0.0F, 0.0F);
         set(c, x0 + r, y0 + s, 0.0F);
      } else if ((s = s - d) < half) {
         float a = s / r;
         set(t, -((float)Math.sin(a)), (float)Math.cos(a), 0.0F);
         set(o, (float)Math.cos(a), (float)Math.sin(a), 0.0F);
         set(c, x0 + r * (float)Math.cos(a), y1 + r * (float)Math.sin(a), 0.0F);
      } else if ((s = s - half) < d) {
         set(t, 0.0F, -1.0F, 0.0F);
         set(o, -1.0F, 0.0F, 0.0F);
         set(c, x0 - r, y1 - s, 0.0F);
      } else {
         float var16;
         float a = (var16 = s - d) / r;
         set(t, (float)Math.sin(a), -((float)Math.cos(a)), 0.0F);
         set(o, -((float)Math.cos(a)), -((float)Math.sin(a)), 0.0F);
         set(c, x0 - r * (float)Math.cos(a), y0 - r * (float)Math.sin(a), 0.0F);
      }
   }

   private static void set(float[] v, float x, float y, float z) {
      v[0] = x;
      v[1] = y;
      v[2] = z;
   }

   private static int index(String s, int count) {
      int i = Integer.parseInt(s);
      int k = i < 0 ? count + i : i - 1;
      if (k >= 0 && k < count) {
         return k;
      } else {
         throw new NumberFormatException("index " + i + " out of range");
      }
   }

   private static float[] flatNormal(float[] a, float[] b, float[] c) {
      float uy = b[1] - a[1];
      float vz = c[2] - a[2];
      float uz = b[2] - a[2];
      float vy = c[1] - a[1];
      float nx = uy * vz - uz * vy;
      float vx = c[0] - a[0];
      float ux = b[0] - a[0];
      float ny = uz * vx - ux * vz;
      float nz = ux * vy - uy * vx;
      float l = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
      float[] fArray;
      if (l < 1.0E-12F) {
         float[] fArray2 = new float[]{0.0F, 1.0F, 0.0F};
         fArray = fArray2;
         fArray2[2] = 0.0F;
      } else {
         float[] fArray3 = new float[]{nx / l, ny / l, 0.0F};
         fArray = fArray3;
         fArray3[2] = nz / l;
      }

      return fArray;
   }

   static final class Part {
      final String name;
      float[] data = new float[1024];
      int floats;

      Part(String name) {
         this.name = name;
      }

      void corner(float[] p, float[] n, float[] t) {
         if (this.floats + 8 > this.data.length) {
            float[] bigger = new float[this.data.length * 2];
            System.arraycopy(this.data, 0, bigger, 0, this.floats);
            this.data = bigger;
         }

         this.data[this.floats++] = p[0];
         this.data[this.floats++] = p[1];
         this.data[this.floats++] = p[2];
         this.data[this.floats++] = n[0];
         this.data[this.floats++] = n[1];
         this.data[this.floats++] = n[2];
         this.data[this.floats++] = t[0];
         this.data[this.floats++] = t[1];
      }

      int triangles() {
         return this.floats / 24;
      }
   }
}
