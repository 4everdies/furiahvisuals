package meowtils.furiahvisuals.render;

public final class PreviewBackdrop {
   public static final int SLATE_TOP = -12959412;
   public static final int SLATE_BOTTOM = -14012360;
   public static final int GRID = 8;
   private static final int BOTTOM = ColorUtil.shade(-15723493, 0.44F);
   private static final int EDGE = ColorUtil.lerp(-15065305, -1, 0.1F);
   private static final float[] LIFT = new float[]{9.0F, 11.0F, 14.0F};
   private static final float[] SLATE_LIFT = new float[]{12.0F, 13.0F, 15.0F};
   private static final int[][] CORNERS = new int[2][];

   private PreviewBackdrop() {
   }

   public static int[] corners(boolean slate) {
      int k = slate ? 1 : 0;
      int[] c = CORNERS[k];
      if (c == null) {
         c = new int[81];

         for (int j = 0; j <= 8; j++) {
            for (int i = 0; i <= 8; i++) {
               c[j * 9 + i] = color(-1.0F + 2.0F * i / 8.0F, -1.0F + 2.0F * j / 8.0F, slate);
            }
         }

         CORNERS[k] = c;
      }

      return c;
   }

   public static int color(float x, float y, boolean slate) {
      int top = slate ? -12959412 : -15723493;
      int bottom = slate ? -14012360 : BOTTOM;
      int c = ColorUtil.lerp(bottom, top, Math.max(0.0F, Math.min(1.0F, (y + 1.0F) * 0.5F)));
      float r = (float)Math.sqrt(x * x + (y - 0.1F) * (y - 0.1F) * 0.9F);
      float k = 1.0F - smooth(0.05F, 1.25F, r);
      float[] lift = slate ? SLATE_LIFT : LIFT;
      int red = Math.min(255, (c >> 16 & 0xFF) + Math.round(lift[0] * k));
      int green = Math.min(255, (c >> 8 & 0xFF) + Math.round(lift[1] * k));
      int blue = Math.min(255, (c & 0xFF) + Math.round(lift[2] * k));
      return 0xFF000000 | red << 16 | green << 8 | blue;
   }

   public static int groundEdge() {
      return EDGE;
   }

   private static float smooth(float a, float b, float x) {
      float t = Math.max(0.0F, Math.min(1.0F, (x - a) / (b - a)));
      return t * t * (3.0F - 2.0F * t);
   }
}
