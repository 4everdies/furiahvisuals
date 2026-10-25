package meowtils.furiahvisuals.module.weapons;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import meowtils.furiahvisuals.render.PlayerModel;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

final class ArmRenderer {
   private final int[][][] first = new int[2][2][4];
   private static final int VERTS_PER_BOX = 36;
   static final float EXTENSION = 28.0F;
   private int vbo = -1;
   static float FIST_Y = 9.0F;
   static final float FIST_Z = 0.0F;

   static float fistX(boolean slim, boolean right) {
      PlayerModel.Box b = PlayerModel.base(right ? 2 : 3, slim);
      return b.cx();
   }

   private void build() {
      ByteBuffer data = ByteBuffer.allocateDirect(16128).order(ByteOrder.nativeOrder());
      int vertex = 0;
      float[] c = new float[3];
      float[] uv = new float[4];

      for (int slim = 0; slim < 2; slim++) {
         for (int side = 0; side < 2; side++) {
            int part = side == 0 ? 2 : 3;

            for (int range = 0; range < 4; range++) {
               int layer = range % 2;
               boolean extension = range >= 2;
               PlayerModel.Box b = layer == 0 ? PlayerModel.base(part, slim == 1) : PlayerModel.wear(part, slim == 1);
               this.first[slim][side][range] = vertex;
               float top = b.y - b.inflate;

               for (int face = 0; face < 6; face++) {
                  int[] corners = PlayerModel.FACE_CORNERS[face];
                  PlayerModel.faceUV(b, face, uv);
                  if (extension && face != 2 && face != 3) {
                     uv[3] = uv[1] + 1.0F;
                  }

                  float nx = face == 0 ? 1.0F : (face == 1 ? -1.0F : 0.0F);
                  float ny = face == 2 ? -1.0F : (face == 3 ? 1.0F : 0.0F);
                  float nz = face == 4 ? -1.0F : (face == 5 ? 1.0F : 0.0F);
                  float[][] tex = new float[][]{{uv[2], uv[1]}, {uv[0], uv[1]}, {uv[0], uv[3]}, {uv[2], uv[3]}};

                  for (int k : new int[]{0, 1, 2, 0, 2, 3}) {
                     int corner = corners[k];
                     PlayerModel.corner(b, corner, c);
                     if (extension) {
                        boolean handEnd = corner == 2 || corner == 3 || corner == 6 || corner == 7;
                        c[1] = handEnd ? top : top - 28.0F;
                     }

                     data.putFloat(c[0]).putFloat(c[1]).putFloat(c[2]);
                     data.putShort((short)(nx * 32767.0F)).putShort((short)(ny * 32767.0F)).putShort((short)(nz * 32767.0F)).putShort((short)0);
                     data.putFloat(tex[k][0] / b.texW).putFloat(tex[k][1] / b.texH);
                     vertex++;
                  }
               }
            }
         }
      }

      ((Buffer)data).flip();
      this.vbo = OpenGlHelper.glGenBuffers();
      OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.vbo);
      OpenGlHelper.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, data, OpenGlHelper.GL_STATIC_DRAW);
      OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
   }

   void draw(boolean slim, boolean right, boolean sleeve) {
      if (this.vbo < 0) {
         this.build();
      }

      OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, this.vbo);
      GL11.glVertexPointer(3, 5126, 28, 0L);
      GL11.glNormalPointer(5122, 28, 12L);
      OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
      GL11.glTexCoordPointer(2, 5126, 28, 20L);
      GL11.glEnableClientState(32884);
      GL11.glEnableClientState(32885);
      GL11.glEnableClientState(32888);

      try {
         int s = slim ? 1 : 0;
         int side = right ? 0 : 1;
         GL11.glDrawArrays(4, this.first[s][side][0], 36);
         GL11.glDrawArrays(4, this.first[s][side][2], 36);
         if (sleeve) {
            GL11.glDrawArrays(4, this.first[s][side][1], 36);
            GL11.glDrawArrays(4, this.first[s][side][3], 36);
         }
      } finally {
         GL11.glDisableClientState(32884);
         GL11.glDisableClientState(32885);
         GL11.glDisableClientState(32888);
         OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
      }
   }
}
