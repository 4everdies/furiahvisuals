package meowtils.furiahvisuals.module.weapons;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

final class WeaponMesh {
   static final int STRIDE = 28;
   private static final int GL_ELEMENT_ARRAY_BUFFER = 34963;
   final List<WeaponMesh.Part> parts = new ArrayList<>();
   final float[] min = new float[3];
   final float[] max = new float[3];
   int triangles;
   private ByteBuffer vertexData;
   private ByteBuffer indexData;
   private int vbo = -1;
   private int ibo = -1;
   private int indexCount;
   long bytes;

   private WeaponMesh() {
   }

   static WeaponMesh build(ObjModel model) {
      WeaponMesh mesh = new WeaponMesh();
      int corners = model.triangles() * 3;
      ByteBuffer verts = ByteBuffer.allocateDirect(Math.max(1792, corners * 28 / 2)).order(ByteOrder.nativeOrder());
      int[] indices = new int[corners];
      int vertexCount = 0;
      int n = 0;

      for (int k = 0; k < 3; k++) {
         mesh.min[k] = model.min[k];
         mesh.max[k] = model.max[k];
      }

      for (ObjModel.Part p : model.parts.values()) {
         HashMap<WeaponMesh.Key, Integer> seen = new HashMap<>(p.floats / 8);
         int first = n;
         WeaponMesh.Part part = new WeaponMesh.Part(p.name, n, 0);
         float[] pmin = part.min;
         float[] pmax = part.max;
         pmin[2] = Float.MAX_VALUE;
         pmin[1] = Float.MAX_VALUE;
         pmin[0] = Float.MAX_VALUE;
         pmax[2] = -Float.MAX_VALUE;
         pmax[1] = -Float.MAX_VALUE;
         pmax[0] = -Float.MAX_VALUE;
         float u0 = Float.MAX_VALUE;
         float v0 = Float.MAX_VALUE;
         float u1 = -Float.MAX_VALUE;
         float v1 = -Float.MAX_VALUE;

         for (int i = 0; i < p.floats; i += 8) {
            WeaponMesh.Key key = new WeaponMesh.Key(p.data, i);
            Integer index = seen.get(key);
            if (index == null) {
               index = vertexCount++;
               seen.put(key, index);
               if (verts.remaining() < 28) {
                  ByteBuffer bigger = ByteBuffer.allocateDirect(verts.capacity() * 2).order(ByteOrder.nativeOrder());
                  ((Buffer)verts).flip();
                  bigger.put(verts);
                  verts = bigger;
               }

               float x = p.data[i];
               float y = p.data[i + 1];
               float z = p.data[i + 2];
               verts.putFloat(x).putFloat(y).putFloat(z);
               float nx = p.data[i + 3];
               float ny = p.data[i + 4];
               float nz = p.data[i + 5];
               float l = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
               if (l < 1.0E-6F) {
                  nx = 0.0F;
                  ny = 1.0F;
                  nz = 0.0F;
                  l = 1.0F;
               }

               verts.putShort(toShort(nx / l)).putShort(toShort(ny / l)).putShort(toShort(nz / l)).putShort((short)0);
               float u = p.data[i + 6];
               float v = p.data[i + 7];
               verts.putFloat(u).putFloat(v);
               pmin[0] = Math.min(pmin[0], x);
               pmin[1] = Math.min(pmin[1], y);
               pmin[2] = Math.min(pmin[2], z);
               pmax[0] = Math.max(pmax[0], x);
               pmax[1] = Math.max(pmax[1], y);
               pmax[2] = Math.max(pmax[2], z);
               u0 = Math.min(u0, u);
               v0 = Math.min(v0, v);
               u1 = Math.max(u1, u);
               v1 = Math.max(v1, v);
            }

            indices[n++] = index;
         }

         WeaponMesh.Part done = new WeaponMesh.Part(p.name, first, n - first);
         System.arraycopy(pmin, 0, done.min, 0, 3);
         System.arraycopy(pmax, 0, done.max, 0, 3);
         done.uvRect[0] = u0;
         done.uvRect[1] = v0;
         done.uvRect[2] = u1;
         done.uvRect[3] = v1;
         mesh.parts.add(done);
      }

      ((Buffer)verts).flip();
      mesh.vertexData = verts;
      ByteBuffer idx = ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder());

      for (int i = 0; i < n; i++) {
         idx.putInt(indices[i]);
      }

      ((Buffer)idx).flip();
      mesh.indexData = idx;
      mesh.indexCount = n;
      mesh.triangles = n / 3;
      return mesh;
   }

   private static short toShort(float v) {
      return (short)Math.round(Math.max(-1.0F, Math.min(1.0F, v)) * 32767.0F);
   }

   boolean uploaded() {
      return this.vbo >= 0;
   }

   void upload() {
      if (this.vbo < 0 && this.vertexData != null) {
         this.vbo = OpenGlHelper.glGenBuffers();
         OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.vbo);
         OpenGlHelper.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, this.vertexData, OpenGlHelper.GL_STATIC_DRAW);
         OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
         this.ibo = OpenGlHelper.glGenBuffers();
         OpenGlHelper.glBindBuffer(34963, this.ibo);
         OpenGlHelper.glBufferData(34963, this.indexData, OpenGlHelper.GL_STATIC_DRAW);
         OpenGlHelper.glBindBuffer(34963, 0);
         this.bytes = this.vertexData.limit() + this.indexData.limit();
         this.vertexData = null;
         this.indexData = null;
      }
   }

   void delete() {
      if (this.vbo >= 0) {
         OpenGlHelper.glDeleteBuffers(this.vbo);
         OpenGlHelper.glDeleteBuffers(this.ibo);
         this.ibo = -1;
         this.vbo = -1;
      }

      this.vertexData = null;
      this.indexData = null;
   }

   void begin() {
      OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.vbo);
      OpenGlHelper.glBindBuffer(34963, this.ibo);
      GL11.glVertexPointer(3, 5126, 28, 0L);
      GL11.glNormalPointer(5122, 28, 12L);
      OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
      GL11.glTexCoordPointer(2, 5126, 28, 20L);
      GL11.glEnableClientState(32884);
      GL11.glEnableClientState(32885);
      GL11.glEnableClientState(32888);
   }

   void draw(WeaponMesh.Part p) {
      if (p.count > 0) {
         GL11.glDrawElements(4, p.count, 5125, p.first * 4L);
      }
   }

   void drawAll() {
      GL11.glDrawElements(4, this.indexCount, 5125, 0L);
   }

   static void end() {
      GL11.glDisableClientState(32884);
      GL11.glDisableClientState(32885);
      GL11.glDisableClientState(32888);
      OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
      OpenGlHelper.glBindBuffer(34963, 0);
   }

   WeaponMesh.Part part(String name) {
      for (WeaponMesh.Part p : this.parts) {
         if (p.name.equals(name)) {
            return p;
         }
      }

      return null;
   }

   private static final class Key {
      private final float[] d = new float[8];
      private final int hash;

      Key(float[] src, int at) {
         System.arraycopy(src, at, this.d, 0, 8);
         int h = 1;

         for (float f : this.d) {
            h = 31 * h + Float.floatToIntBits(f);
         }

         this.hash = h;
      }

      @Override
      public int hashCode() {
         return this.hash;
      }

      @Override
      public boolean equals(Object o) {
         if (!(o instanceof WeaponMesh.Key)) {
            return false;
         } else {
            float[] e = ((WeaponMesh.Key)o).d;

            for (int i = 0; i < 8; i++) {
               if (Float.floatToIntBits(this.d[i]) != Float.floatToIntBits(e[i])) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   static final class Part {
      final String name;
      final int first;
      final int count;
      final float[] min = new float[3];
      final float[] max = new float[3];
      final float[] uvRect = new float[4];

      Part(String name, int first, int count) {
         this.name = name;
         this.first = first;
         this.count = count;
      }
   }
}
