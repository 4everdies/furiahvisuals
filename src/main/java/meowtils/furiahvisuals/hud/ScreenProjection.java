package meowtils.furiahvisuals.hud;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import meowtils.furiahvisuals.compat.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.entity.RenderManager;

public final class ScreenProjection {
   private static boolean looked;
   private static boolean ok;
   private static Field modelview;
   private static Field projection;
   private static Field viewport;
   private static final float[] MV = new float[16];
   private static final float[] P = new float[16];

   private ScreenProjection() {
   }

   private static boolean lookUp() {
      if (!looked) {
         looked = true;

         try {
            modelview = ReflectionHelper.findField(ActiveRenderInfo.class, new String[]{"MODELVIEW", "MODELVIEW"});
            projection = ReflectionHelper.findField(ActiveRenderInfo.class, new String[]{"PROJECTION", "PROJECTION"});
            viewport = ReflectionHelper.findField(ActiveRenderInfo.class, new String[]{"VIEWPORT", "VIEWPORT"});
            ok = true;
         } catch (RuntimeException var1) {
            ok = false;
         }
      }

      return ok;
   }

   public static boolean project(double x, double y, double z, float[] out) {
      if (!lookUp()) {
         return false;
      } else {
         try {
            FloatBuffer mv = (FloatBuffer)modelview.get(null);
            FloatBuffer pr = (FloatBuffer)projection.get(null);
            IntBuffer vp = (IntBuffer)viewport.get(null);

            for (int i = 0; i < 16; i++) {
               MV[i] = mv.get(i);
               P[i] = pr.get(i);
            }

            RenderManager rm = Minecraft.getMinecraft().getRenderManager();
            float rx = (float)(x - rm.viewerPosX);
            float ry = (float)(y - rm.viewerPosY);
            float rz = (float)(z - rm.viewerPosZ);
            float ex = MV[0] * rx + MV[4] * ry + MV[8] * rz + MV[12];
            float ey = MV[1] * rx + MV[5] * ry + MV[9] * rz + MV[13];
            float ez = MV[2] * rx + MV[6] * ry + MV[10] * rz + MV[14];
            float ew = MV[3] * rx + MV[7] * ry + MV[11] * rz + MV[15];
            float cx = P[0] * ex + P[4] * ey + P[8] * ez + P[12] * ew;
            float cy = P[1] * ex + P[5] * ey + P[9] * ez + P[13] * ew;
            float cw = P[3] * ex + P[7] * ey + P[11] * ez + P[15] * ew;
            if (cw <= 1.0E-4F) {
               return false;
            } else {
               float nx = cx / cw;
               float ny = cy / cw;
               int vx = vp.get(0);
               int vy = vp.get(1);
               int vw = vp.get(2);
               int vh = vp.get(3);
               int height = Minecraft.getMinecraft().displayHeight;
               out[0] = vx + (nx + 1.0F) / 2.0F * vw;
               out[1] = height - (vy + (ny + 1.0F) / 2.0F * vh);
               return true;
            }
         } catch (RuntimeException | IllegalAccessException var28) {
            ok = false;
            return false;
         }
      }
   }

   public static float fieldOfView() {
      if (!lookUp()) {
         return Float.NaN;
      } else {
         try {
            FloatBuffer pr = (FloatBuffer)projection.get(null);
            float p5 = pr.get(5);
            return p5 <= 1.0E-6F ? Float.NaN : (float)Math.toDegrees(2.0 * Math.atan(1.0 / p5));
         } catch (RuntimeException | IllegalAccessException var2) {
            return Float.NaN;
         }
      }
   }
}
