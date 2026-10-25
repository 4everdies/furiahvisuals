package meowtils.furiahvisuals.fx.look;

import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.Easing;

public final class NearCamera {
   public static final float OUTER = 1.2F;
   public static final float INNER = 0.48F;
   public static final float DARK_WITHIN = 1.5F;
   public static final float DARK_CAP = 0.35F;

   private NearCamera() {
   }

   public static float fade(Camera cam, double x, double y, double z) {
      if (cam == null) {
         return 1.0F;
      } else {
         float d = (float)Math.sqrt(cam.distanceSq(x, y, z));
         return Easing.smoothstep(0.48F, 1.2F, d);
      }
   }

   public static float dark(Camera cam, double x, double y, double z, float alpha) {
      if (cam != null && !(alpha <= 0.35F)) {
         float d = (float)Math.sqrt(cam.distanceSq(x, y, z));
         float k = Easing.smoothstep(1.5F, 2.0F, d);
         return 0.35F + (alpha - 0.35F) * k;
      } else {
         return alpha;
      }
   }
}
