package meowtils.furiahvisuals.module.weapons;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

final class PixelMesh {
   static final int STRIDE = 20;
   private static final int GL_ELEMENT_ARRAY_BUFFER = 34963;
   private static final int GL_DYNAMIC_DRAW = 35048;
   static final int EDGE = 1;
   static final int HALF_X = 2;
   static final int HALF_Y = 4;
   static final int HALF_Z = 8;
   static final int HALO = 16;
   static final int FRONT = 32;
   final List<PixelMesh.Part> parts = new ArrayList<>();
   final float[] min = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
   final float[] max = new float[]{-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
   float alongMin;
   float alongMax;
   float pixel;
   Map<String, Float> stretch = Collections.emptyMap();
   int triangles;
   int bakedTriangles;
   long buildNanos;
   final boolean far;
   private ByteBuffer vertexData;
   private ByteBuffer indexData;
   private ByteBuffer bakedVertexData;
   private ByteBuffer bakedIndexData;
   private int vbo = -1;
   private int ibo = -1;
   private int bvbo = -1;
   private int bibo = -1;
   long bytes;
   int[] bakedQuads;
   int bakedQuadCount;
   private ByteBuffer bakedVertices;
   long bakedKey = Long.MIN_VALUE;

   private PixelMesh(boolean far) {
      this.far = far;
   }

   static PixelMesh build(PixelModel m, boolean far) {
      long t0 = System.nanoTime();
      PixelMesh mesh = new PixelMesh(far);
      mesh.pixel = m.pixel;
      mesh.stretch = m.stretch;
      ArrayList<String> partNames = new ArrayList<>();

      for (PixelModel.Layer l : m.layers) {
         if (!partNames.contains(l.part) && (!far || l.plane == PixelModel.Plane.FRONT)) {
            partNames.add(l.part);
         }
      }

      boolean[] clear = new boolean[PixelModel.SLOTS];
      boolean[] glow = new boolean[PixelModel.SLOTS];

      for (int s = 1; s < PixelModel.SLOTS; s++) {
         int mat = PixelModel.material(s);
         PixelModel.Fixed f = mat >= PixelModel.MATERIAL_NAMES.length ? m.fixed[mat - PixelModel.MATERIAL_NAMES.length] : null;
         clear[s] = f != null && f.alpha < 0.999F;
         glow[s] = mat == 5 || f != null && f.glow;
      }

      PixelMesh.Grid g = grid(m, far, partNames);

      for (int i = 0; i < partNames.size(); i++) {
         mesh.parts.add(new PixelMesh.Part(partNames.get(i)));
      }

      PixelMesh.Builder merged = new PixelMesh.Builder();
      PixelMesh.Builder baked = far ? null : new PixelMesh.Builder();
      int[] kind = new int[PixelModel.SLOTS];

      for (int s = 1; s < PixelModel.SLOTS; s++) {
         int mat = PixelModel.material(s);
         kind[s] = clear[s] ? 2 : (mat != 0 && mat != 1 ? 0 : 1);
      }

      PixelMesh.Builder[] byKind = new PixelMesh.Builder[]{new PixelMesh.Builder(), new PixelMesh.Builder(), new PixelMesh.Builder()};
      PixelMesh.Builder[] builderArray;
      if (baked != null) {
         PixelMesh.Builder[] builderArray2 = new PixelMesh.Builder[]{new PixelMesh.Builder(), new PixelMesh.Builder(), null};
         builderArray = builderArray2;
         builderArray2[2] = new PixelMesh.Builder();
      } else {
         builderArray = null;
      }

      PixelMesh.Builder[] bakedByKind = builderArray;

      for (int pi = 0; pi < partNames.size(); pi++) {
         PixelMesh.Part part = mesh.parts.get(pi);

         for (PixelMesh.Builder b : byKind) {
            b.reset();
         }

         faces(g, pi, kind, clear, byKind, m.pixel, part);
         part.first = merged.indexCount;
         merged.append(byKind[0]);
         part.count = merged.indexCount - part.first;
         part.bladeFirst = merged.indexCount;
         merged.append(byKind[1]);
         part.bladeCount = merged.indexCount - part.bladeFirst;
         part.clearFirst = merged.indexCount;
         merged.append(byKind[2]);
         part.clearCount = merged.indexCount - part.clearFirst;
         if (baked != null) {
            for (PixelMesh.Builder b : bakedByKind) {
               b.reset();
            }

            bakedFaces(g, pi, clear, glow, bakedByKind, m.pixel);
            part.bFirst = baked.indexCount;
            baked.append(bakedByKind[0]);
            part.bCount = baked.indexCount - part.bFirst;
            part.bGlowFirst = baked.indexCount;
            baked.append(bakedByKind[1]);
            part.bGlowCount = baked.indexCount - part.bGlowFirst;
            part.bClearFirst = baked.indexCount;
            baked.append(bakedByKind[2]);
            part.bClearCount = baked.indexCount - part.bClearFirst;
         }
      }

      for (int var22 = 0; var22 < partNames.size(); var22++) {
         PixelMesh.Part part = mesh.parts.get(var22);
         int start = merged.indexCount;
         halos(m, far, partNames.get(var22), glow, false, merged, m.pixel);
         part.haloFirst = start;
         part.haloCount = merged.indexCount - start;
         start = merged.indexCount;
         halos(m, far, partNames.get(var22), glow, true, merged, m.pixel);
         part.edgeHaloFirst = start;
         part.edgeHaloCount = merged.indexCount - start;
      }

      for (PixelMesh.Part p : mesh.parts) {
         for (int k = 0; k < 3; k++) {
            mesh.min[k] = Math.min(mesh.min[k], p.min[k]);
            mesh.max[k] = Math.max(mesh.max[k], p.max[k]);
         }
      }

      mesh.alongMin = mesh.min[1] / m.pixel;
      mesh.alongMax = mesh.max[1] / m.pixel;
      mesh.vertexData = merged.vertices();
      mesh.indexData = merged.indices();
      mesh.triangles = merged.indexCount / 3;
      if (baked != null) {
         mesh.bakedVertices = baked.vertices();
         mesh.bakedIndexData = baked.indices();
         mesh.bakedQuads = baked.quads;
         mesh.bakedQuadCount = baked.quadCount;
         mesh.bakedTriangles = baked.indexCount / 3;
      }

      mesh.buildNanos = System.nanoTime() - t0;
      return mesh;
   }

   private static PixelMesh.Grid grid(PixelModel m, boolean far, List<String> partNames) {
      int[] lo = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
      int[] hi = new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
      int[] box = new int[6];

      for (PixelModel.Layer l : m.layers) {
         if (!far || l.plane == PixelModel.Plane.FRONT) {
            for (int r = 0; r < l.h; r++) {
               for (int c = 0; c < l.w; c++) {
                  if (l.slot(c, r) != 0) {
                     box(l, c, r, box);

                     for (int i = 0; i < 3; i++) {
                        lo[i] = Math.min(lo[i], box[i]);
                        hi[i] = Math.max(hi[i], box[3 + i]);
                     }
                  }
               }
            }
         }
      }

      PixelMesh.Grid g = new PixelMesh.Grid();
      g.boxes = new int[partNames.size()][];
      if (lo[0] > hi[0]) {
         g.nz = 1;
         g.ny = 1;
         g.nx = 1;
         g.slot = new byte[1];
         g.part = new byte[1];
         g.info = new byte[1];
         return g;
      } else {
         g.minX = lo[0];
         g.minY = lo[1];
         g.minZ = lo[2];
         g.nx = hi[0] - lo[0];
         g.ny = hi[1] - lo[1];
         g.nz = hi[2] - lo[2];
         int n = g.nx * g.ny * g.nz;
         g.slot = new byte[n];
         g.part = new byte[n];
         g.info = new byte[n];

         for (PixelModel.Layer lx : m.layers) {
            if (!far || lx.plane == PixelModel.Plane.FRONT) {
               int pi = partNames.indexOf(lx.part);
               int axis = lx.plane == PixelModel.Plane.FRONT ? 2 : (lx.plane == PixelModel.Plane.SIDE ? 0 : 1);
               int thick = 1 << axis;
               int parity = parity(lx) & ~thick;
               boolean planeHalf = (parity(lx) & thick) != 0;

               for (int r = 0; r < lx.h; r++) {
                  for (int cx = 0; cx < lx.w; cx++) {
                     int s = lx.slot(cx, r);
                     if (s != 0) {
                        box(lx, cx, r, box);
                        int[] pb = g.boxes[pi];
                        if (pb == null) {
                           g.boxes[pi] = new int[]{
                              Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE
                           };
                           pb = g.boxes[pi];
                        }

                        for (int i = 0; i < 3; i++) {
                           pb[i] = Math.min(pb[i], box[i]);
                           pb[3 + i] = Math.max(pb[3 + i], box[3 + i]);
                        }

                        boolean edge = lx.slot(cx - 1, r) == 0 || lx.slot(cx + 1, r) == 0 || lx.slot(cx, r - 1) == 0 || lx.slot(cx, r + 1) == 0;
                        boolean halfThick = (lx.depth(cx, r) & 1) == 1 != planeHalf;
                        byte info = (byte)(axis | (edge ? 4 : 0) | (parity | (halfThick ? thick : 0)) << 3);

                        for (int x = box[0]; x < box[3]; x++) {
                           for (int y = box[1]; y < box[4]; y++) {
                              for (int z = box[2]; z < box[5]; z++) {
                                 int i = g.index(x, y, z);
                                 g.slot[i] = (byte)s;
                                 g.part[i] = (byte)pi;
                                 g.info[i] = info;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return g;
      }
   }

   private static int parity(PixelModel.Layer l) {
      int p = 0;
      if (Math.abs(l.x - Math.floor(l.x)) > 0.25) {
         p |= 1;
      }

      if (Math.abs(l.y - Math.floor(l.y)) > 0.25) {
         p |= 2;
      }

      if (Math.abs(l.z - Math.floor(l.z)) > 0.25) {
         p |= 4;
      }

      return p;
   }

   private static void box(PixelModel.Layer l, int c, int r, int[] out) {
      int d = l.depth(c, r);
      float z1;
      float z0;
      float y0;
      float y1;
      float x1;
      float x0;
      switch (l.plane) {
         case FRONT:
            x0 = l.x + c;
            x1 = x0 + 1.0F;
            y1 = l.y - r;
            y0 = y1 - 1.0F;
            z0 = l.z - d / 2.0F;
            z1 = l.z + d / 2.0F;
            break;
         case SIDE:
            z0 = l.z + c;
            z1 = z0 + 1.0F;
            y1 = l.y - r;
            y0 = y1 - 1.0F;
            x0 = l.x - d / 2.0F;
            x1 = l.x + d / 2.0F;
            break;
         default:
            x0 = l.x + c;
            x1 = x0 + 1.0F;
            z0 = l.z + r;
            z1 = z0 + 1.0F;
            y0 = l.y - d / 2.0F;
            y1 = l.y + d / 2.0F;
      }

      out[0] = Math.round(x0 * 2.0F);
      out[1] = Math.round(y0 * 2.0F);
      out[2] = Math.round(z0 * 2.0F);
      out[3] = Math.round(x1 * 2.0F);
      out[4] = Math.round(y1 * 2.0F);
      out[5] = Math.round(z1 * 2.0F);
   }

   private static int faceSlot(int slot, int info, int axis) {
      return (info & 3) != axis && PixelModel.shade(slot) != 0 ? slot - 1 : slot;
   }

   private static void sweep(
      PixelMesh.Grid g, int pi, int[] kind, boolean[] clear, boolean[] glow, boolean baked, PixelMesh.Builder[] out, float pixel, PixelMesh.Part part
   ) {
      int[] box = g.boxes[pi];
      if (box != null) {
         int[] gmin = new int[]{g.minX, g.minY, g.minZ};
         int[] gn = new int[]{g.nx, g.ny, g.nz};
         int[] stride = new int[]{g.ny * g.nz, g.nz, 1};
         int[] p = new int[3];

         for (int a = 0; a < 3; a++) {
            int u = (a + 1) % 3;
            int w = (a + 2) % 3;
            int nu = box[3 + u] - box[u];
            int nw = box[3 + w] - box[w];
            int[] mask = new int[nu * nw];

            for (int sign = -1; sign <= 1; sign += 2) {
               for (int s = box[a]; s < box[3 + a]; s++) {
                  boolean any = false;
                  boolean beyondInside = s + sign >= gmin[a] && s + sign < gmin[a] + gn[a];
                  int sliceBase = (s - gmin[a]) * stride[a];

                  for (int i = 0; i < nu; i++) {
                     int rowBase = sliceBase + (box[u] + i - gmin[u]) * stride[u] + (box[w] - gmin[w]) * stride[w];

                     for (int j = 0; j < nw; j++) {
                        int idx = rowBase + j * stride[w];
                        int key = 0;
                        int slot = g.slot[idx] & 255;
                        if (slot != 0 && g.part[idx] == pi) {
                           int k = baked ? (clear[slot] ? 2 : (glow[slot] ? 1 : 0)) : kind[slot];
                           boolean hidden = false;
                           if (beyondInside) {
                              int nIdx = idx + sign * stride[a];
                              int nb = g.slot[nIdx] & 255;
                              hidden = nb != 0 && g.part[nIdx] == pi && (!clear[nb] || k == 2);
                           }

                           if (!hidden) {
                              byte info = g.info[idx];
                              int fs = faceSlot(slot, info, a);
                              int flags = ((info & 4) != 0 ? 1 : 0) | (info >> 3 & 7) << 1 | ((info & 3) == a ? 32 : 0);
                              if (baked) {
                                 p[a] = s;
                                 p[u] = box[u] + i;
                                 p[w] = box[w] + j;
                                 int pu = pixelOf(p[u], info >> 3 >> u & 1);
                                 int pw = pixelOf(p[w], info >> 3 >> w & 1);
                                 key = fs | flags << 8 | ((pu & 15) << 4 | pw & 15) << 14 | k << 22 | 16777216;
                              } else {
                                 key = fs | flags << 8 | k << 14 | 65536;
                              }

                              any = true;
                           }
                        }

                        mask[i * nw + j] = key;
                     }
                  }

                  if (any) {
                     int plane = s + (sign > 0 ? 1 : 0);
                     if (baked) {
                        mergeBaked(mask, nu, nw, a, u, w, sign, plane, box[u], box[w], out, pixel);
                     } else {
                        merge(mask, nu, nw, a, u, w, sign, plane, box[u], box[w], out, pixel, part);
                     }
                  }
               }
            }
         }
      }
   }

   private static void faces(PixelMesh.Grid g, int pi, int[] kind, boolean[] clear, PixelMesh.Builder[] out, float pixel, PixelMesh.Part part) {
      sweep(g, pi, kind, clear, null, false, out, pixel, part);
   }

   private static void merge(
      int[] mask, int nu, int nw, int a, int u, int w, int sign, int plane, int lu, int lw, PixelMesh.Builder[] out, float pixel, PixelMesh.Part part
   ) {
      for (int i = 0; i < nu; i++) {
         int j = 0;

         while (j < nw) {
            int k = mask[i * nw + j];
            if (k == 0) {
               j++;
            } else {
               int len = 1;

               while (j + len < nw && mask[i * nw + j + len] == k) {
                  len++;
               }

               int rows;
               label58:
               for (rows = 1; i + rows < nu; rows++) {
                  for (int t = 0; t < len; t++) {
                     if (mask[(i + rows) * nw + j + t] != k) {
                        break label58;
                     }
                  }
               }

               for (int r = 0; r < rows; r++) {
                  for (int tx = 0; tx < len; tx++) {
                     mask[(i + r) * nw + j + tx] = 0;
                  }
               }

               out[k >> 14 & 3].quad(a, u, w, sign, plane, lu + i, lu + i + rows, lw + j, lw + j + len, k & 0xFF, k >> 8 & 63, pixel, part);
               j += len;
            }
         }
      }
   }

   private static void bakedFaces(PixelMesh.Grid g, int pi, boolean[] clear, boolean[] glow, PixelMesh.Builder[] out, float pixel) {
      sweep(g, pi, null, clear, glow, true, out, pixel, null);
   }

   private static int pixelOf(int cell, int half) {
      return (int)Math.floor((cell - half) / 2.0);
   }

   private static void mergeBaked(int[] mask, int nu, int nw, int a, int u, int w, int sign, int plane, int lu, int lw, PixelMesh.Builder[] kinds, float pixel) {
      for (int i = 0; i < nu; i++) {
         int j = 0;

         while (j < nw) {
            int k = mask[i * nw + j];
            if (k == 0) {
               j++;
            } else {
               int len = 1;

               while (j + len < nw && mask[i * nw + j + len] == k) {
                  len++;
               }

               int rows;
               label68:
               for (rows = 1; i + rows < nu; rows++) {
                  for (int t = 0; t < len; t++) {
                     if (mask[(i + rows) * nw + j + t] != k) {
                        break label68;
                     }
                  }
               }

               for (int r = 0; r < rows; r++) {
                  for (int tx = 0; tx < len; tx++) {
                     mask[(i + r) * nw + j + tx] = 0;
                  }
               }

               int slot = k & 0xFF;
               int flags = k >> 8 & 63;
               PixelMesh.Builder out = kinds[k >> 22 & 3];
               out.quad(a, u, w, sign, plane, lu + i, lu + i + rows, lw + j, lw + j + len, slot, flags, pixel, null);
               int[] cell = new int[3];
               cell[a] = plane - (sign > 0 ? 1 : 0);
               cell[u] = lu + i;
               cell[w] = lw + j;
               int half = flags >> 1;
               out.bakedQuad(
                  slot, pixelOf(cell[0], half & 1), pixelOf(cell[1], half >> 1 & 1), pixelOf(cell[2], half >> 2 & 1), a * 2 + (sign > 0 ? 1 : 0), flags
               );
               j += len;
            }
         }
      }
   }

   private static void halos(PixelModel m, boolean far, String part, boolean[] glow, boolean edges, PixelMesh.Builder out, float pixel) {
      for (PixelModel.Layer l : m.layers) {
         if (l.part.equals(part) && (!far || l.plane == PixelModel.Plane.FRONT)) {
            boolean[] done = new boolean[(l.w + 2) * (l.h + 2)];

            for (int r = 0; r < l.h; r++) {
               for (int c = 0; c < l.w; c++) {
                  int s = l.slot(c, r);
                  boolean lit = s != 0 && (edges ? PixelModel.material(s) == 1 : glow[s]);
                  if (lit) {
                     for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                           int nc = c + dc;
                           int nr = r + dr;
                           int ns = l.slot(nc, nr);
                           boolean nLit = ns != 0 && (edges ? PixelModel.material(ns) == 1 : glow[ns]);
                           int key = (nr + 1) * (l.w + 2) + nc + 1;
                           if (!nLit && !done[key]) {
                              done[key] = true;
                              float depth = ns != 0 ? l.depth(nc, nr) / 2.0F + 0.03F : 0.0F;
                              out.halo(l, nc, nr, depth, s, pixel);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   boolean uploaded() {
      return this.vbo >= 0;
   }

   boolean hasBaked() {
      return this.bvbo >= 0;
   }

   void upload() {
      if (this.vbo < 0 && this.vertexData != null) {
         this.vbo = buffer(OpenGlHelper.GL_ARRAY_BUFFER, this.vertexData, OpenGlHelper.GL_STATIC_DRAW);
         this.ibo = buffer(34963, this.indexData, OpenGlHelper.GL_STATIC_DRAW);
         this.bytes = this.vertexData.limit() + this.indexData.limit();
         if (this.bakedVertices != null) {
            this.bvbo = buffer(OpenGlHelper.GL_ARRAY_BUFFER, this.bakedVertices, 35048);
            this.bibo = buffer(34963, this.bakedIndexData, OpenGlHelper.GL_STATIC_DRAW);
            this.bytes = this.bytes + (this.bakedVertices.limit() + this.bakedIndexData.limit());
            this.bakedIndexData = null;
         }

         this.vertexData = null;
         this.indexData = null;
      }
   }

   private static int buffer(int target, ByteBuffer data, int usage) {
      int id = OpenGlHelper.glGenBuffers();
      OpenGlHelper.glBindBuffer(target, id);
      OpenGlHelper.glBufferData(target, data, usage);
      OpenGlHelper.glBindBuffer(target, 0);
      return id;
   }

   void delete() {
      for (int id : new int[]{this.vbo, this.ibo, this.bvbo, this.bibo}) {
         if (id >= 0) {
            OpenGlHelper.glDeleteBuffers(id);
         }
      }

      this.bibo = -1;
      this.bvbo = -1;
      this.ibo = -1;
      this.vbo = -1;
      this.bakedIndexData = null;
      this.indexData = null;
      this.vertexData = null;
      this.bakedVertices = null;
   }

   void bake(int[] colors, long key) {
      if (this.bvbo >= 0 && this.bakedVertices != null) {
         for (int q = 0; q < this.bakedQuadCount; q++) {
            int c = colors[q];

            for (int k = 0; k < 4; k++) {
               int at = (q * 4 + k) * 20 + 16;
               this.bakedVertices.put(at, (byte)(c >> 16)).put(at + 1, (byte)(c >> 8)).put(at + 2, (byte)c).put(at + 3, (byte)(c >>> 24));
            }
         }

         ((Buffer)this.bakedVertices).position(0);
         OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.bvbo);
         OpenGlHelper.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, this.bakedVertices, 35048);
         OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
         this.bakedKey = key;
      }
   }

   void begin() {
      OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.vbo);
      OpenGlHelper.glBindBuffer(34963, this.ibo);
      GL11.glVertexPointer(3, 5126, 20, 0L);
      GL11.glNormalPointer(5120, 20, 12L);
      OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
      GL11.glTexCoordPointer(2, 5122, 20, 16L);
      GL11.glEnableClientState(32884);
      GL11.glEnableClientState(32885);
      GL11.glEnableClientState(32888);
   }

   void beginShape() {
      OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.vbo);
      OpenGlHelper.glBindBuffer(34963, this.ibo);
      GL11.glVertexPointer(3, 5126, 20, 0L);
      GL11.glNormalPointer(5120, 20, 12L);
      GL11.glEnableClientState(32884);
      GL11.glEnableClientState(32885);
   }

   void beginBaked() {
      OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.bvbo);
      OpenGlHelper.glBindBuffer(34963, this.bibo);
      GL11.glVertexPointer(3, 5126, 20, 0L);
      GL11.glNormalPointer(5120, 20, 12L);
      GL11.glColorPointer(4, 5121, 20, 16L);
      GL11.glEnableClientState(32884);
      GL11.glEnableClientState(32885);
      GL11.glEnableClientState(32886);
   }

   static void draw(int first, int count) {
      if (count > 0) {
         GL11.glDrawElements(4, count, 5125, first * 4L);
      }
   }

   static void end() {
      GL11.glDisableClientState(32884);
      GL11.glDisableClientState(32885);
      GL11.glDisableClientState(32888);
      GL11.glDisableClientState(32886);
      OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
      OpenGlHelper.glBindBuffer(34963, 0);
   }

   PixelMesh.Part part(String name) {
      for (PixelMesh.Part p : this.parts) {
         if (p.name.equals(name)) {
            return p;
         }
      }

      return null;
   }

   private static final class Builder {
      ByteBuffer v = ByteBuffer.allocate(20480).order(ByteOrder.nativeOrder());
      int[] idx = new int[1536];
      int vertexCount;
      int indexCount;
      int[] quads = new int[1536];
      int quadCount;

      private Builder() {
      }

      void reset() {
         ((Buffer)this.v).clear();
         this.quadCount = 0;
         this.indexCount = 0;
         this.vertexCount = 0;
      }

      void append(PixelMesh.Builder o) {
         this.ensure(o.vertexCount, o.indexCount);
         int base = this.vertexCount;
         ByteBuffer src = o.v.duplicate();
         ((Buffer)src).flip();
         this.v.put(src);

         for (int i = 0; i < o.indexCount; i++) {
            this.idx[this.indexCount++] = o.idx[i] + base;
         }

         this.vertexCount = this.vertexCount + o.vertexCount;
         if (o.quadCount > 0) {
            if ((this.quadCount + o.quadCount) * 6 > this.quads.length) {
               this.quads = Arrays.copyOf(this.quads, Math.max(this.quads.length * 2, (this.quadCount + o.quadCount) * 6));
            }

            System.arraycopy(o.quads, 0, this.quads, this.quadCount * 6, o.quadCount * 6);
            this.quadCount = this.quadCount + o.quadCount;
         }
      }

      void ensure(int moreVertices, int moreIndices) {
         if (this.v.remaining() < moreVertices * 20) {
            ByteBuffer bigger = ByteBuffer.allocate(Math.max(this.v.capacity() * 2, this.v.position() + moreVertices * 20)).order(ByteOrder.nativeOrder());
            ((Buffer)this.v).flip();
            bigger.put(this.v);
            this.v = bigger;
         }

         if (this.indexCount + moreIndices > this.idx.length) {
            this.idx = Arrays.copyOf(this.idx, Math.max(this.idx.length * 2, this.indexCount + moreIndices));
         }
      }

      void vertex(float x, float y, float z, int nx, int ny, int nz, int slot, int flags) {
         this.v.putFloat(x).putFloat(y).putFloat(z);
         this.v.put((byte)(nx * 127)).put((byte)(ny * 127)).put((byte)(nz * 127)).put((byte)0);
         this.v.putShort((short)slot).putShort((short)flags);
         this.vertexCount++;
      }

      void quad(int a, int u, int w, int sign, int plane, int u0, int u1, int w0, int w1, int slot, int flags, float pixel, PixelMesh.Part part) {
         this.ensure(4, 6);
         float h = pixel * 0.5F;
         float[][] c = new float[4][3];
         int[][] nArrayArray;
         if (sign > 0) {
            int[][] nArrayArray2 = new int[][]{{u0, w0}, {u1, w0}, {u1, w1}, null};
            nArrayArray = nArrayArray2;
            nArrayArray2[3] = new int[]{u0, w1};
         } else {
            int[][] nArrayArray3 = new int[][]{{u0, w0}, {u0, w1}, {u1, w1}, null};
            nArrayArray = nArrayArray3;
            nArrayArray3[3] = new int[]{u1, w0};
         }

         int[][] uw = nArrayArray;
         int base = this.vertexCount;
         int[] nrm = new int[3];
         nrm[a] = sign;

         for (int k = 0; k < 4; k++) {
            c[k][a] = plane * h;
            c[k][u] = uw[k][0] * h;
            c[k][w] = uw[k][1] * h;
            this.vertex(c[k][0], c[k][1], c[k][2], nrm[0], nrm[1], nrm[2], slot, flags);
            if (part != null) {
               for (int i = 0; i < 3; i++) {
                  part.min[i] = Math.min(part.min[i], c[k][i]);
                  part.max[i] = Math.max(part.max[i], c[k][i]);
               }
            }
         }

         this.idx[this.indexCount++] = base;
         this.idx[this.indexCount++] = base + 1;
         this.idx[this.indexCount++] = base + 2;
         this.idx[this.indexCount++] = base;
         this.idx[this.indexCount++] = base + 2;
         this.idx[this.indexCount++] = base + 3;
      }

      void bakedQuad(int slot, int px, int py, int pz, int dir, int flags) {
         if ((this.quadCount + 1) * 6 > this.quads.length) {
            this.quads = Arrays.copyOf(this.quads, this.quads.length * 2);
         }

         int q = this.quadCount * 6;
         this.quads[q] = slot;
         this.quads[q + 1] = px;
         this.quads[q + 2] = py;
         this.quads[q + 3] = pz;
         this.quads[q + 4] = dir;
         this.quads[q + 5] = flags;
         this.quadCount++;
      }

      void halo(PixelModel.Layer l, int c, int r, float depth, int slot, float pixel) {
         int sides = depth > 0.0F ? 2 : 1;

         for (int side = 0; side < sides; side++) {
            this.ensure(4, 6);
            float off = side == 0 ? depth : -depth;
            float[][] q = new float[4][3];
            int a;
            switch (l.plane) {
               case FRONT: {
                  a = 2;
                  float x0 = l.x + c;
                  float y1 = l.y - r;
                  float[][] corners = new float[][]{{x0, y1 - 1.0F}, {x0 + 1.0F, y1 - 1.0F}, {x0 + 1.0F, y1}, {x0, y1}};

                  for (int k = 0; k < 4; k++) {
                     q[k][0] = corners[k][0];
                     q[k][1] = corners[k][1];
                     q[k][2] = l.z + off;
                  }
                  break;
               }
               case SIDE: {
                  a = 0;
                  float z0 = l.z + c;
                  float y1 = l.y - r;
                  float[][] corners = new float[][]{{z0, y1 - 1.0F}, {z0 + 1.0F, y1 - 1.0F}, {z0 + 1.0F, y1}, {z0, y1}};

                  for (int k = 0; k < 4; k++) {
                     q[k][0] = l.x + off;
                     q[k][1] = corners[k][1];
                     q[k][2] = corners[k][0];
                  }
                  break;
               }
               default: {
                  a = 1;
                  float x0 = l.x + c;
                  float z0 = l.z + r;
                  float[][] corners = new float[][]{{x0, z0}, {x0 + 1.0F, z0}, {x0 + 1.0F, z0 + 1.0F}, {x0, z0 + 1.0F}};

                  for (int k = 0; k < 4; k++) {
                     q[k][0] = corners[k][0];
                     q[k][1] = l.y + off;
                     q[k][2] = corners[k][1];
                  }
               }
            }

            int base = this.vertexCount;
            int[] nrm = new int[3];
            nrm[a] = side == 0 ? 1 : -1;

            for (int k = 0; k < 4; k++) {
               this.vertex(q[k][0] * pixel, q[k][1] * pixel, q[k][2] * pixel, nrm[0], nrm[1], nrm[2], slot, 16);
            }

            this.idx[this.indexCount++] = base;
            this.idx[this.indexCount++] = base + 1;
            this.idx[this.indexCount++] = base + 2;
            this.idx[this.indexCount++] = base;
            this.idx[this.indexCount++] = base + 2;
            this.idx[this.indexCount++] = base + 3;
         }
      }

      ByteBuffer vertices() {
         ByteBuffer out = ByteBuffer.allocateDirect(Math.max(20, this.v.position())).order(ByteOrder.nativeOrder());
         ((Buffer)this.v).flip();
         out.put(this.v);
         ((Buffer)out).flip();
         return out;
      }

      ByteBuffer indices() {
         ByteBuffer out = ByteBuffer.allocateDirect(Math.max(4, this.indexCount * 4)).order(ByteOrder.nativeOrder());

         for (int i = 0; i < this.indexCount; i++) {
            out.putInt(this.idx[i]);
         }

         ((Buffer)out).flip();
         return out;
      }
   }

   private static final class Grid {
      int minX;
      int minY;
      int minZ;
      int nx;
      int ny;
      int nz;
      byte[] slot;
      byte[] part;
      byte[] info;
      int[][] boxes;

      private Grid() {
      }

      int index(int x, int y, int z) {
         return ((x - this.minX) * this.ny + (y - this.minY)) * this.nz + (z - this.minZ);
      }
   }

   static final class Part {
      final String name;
      int first;
      int count;
      int bladeFirst;
      int bladeCount;
      int clearFirst;
      int clearCount;
      int haloFirst;
      int haloCount;
      int edgeHaloFirst;
      int edgeHaloCount;
      int bFirst;
      int bCount;
      int bGlowFirst;
      int bGlowCount;
      int bClearFirst;
      int bClearCount;
      final float[] min = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
      final float[] max = new float[]{-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};

      Part(String name) {
         this.name = name;
      }
   }
}
