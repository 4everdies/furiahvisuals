package meowtils.furiahvisuals.gui;

public final class MenuIcons {
   private MenuIcons() {
   }

   public static void section(String id, float cx, float cy, float s, int c) {
      float r = s / 2.0F;
      float t = Math.max(0.9F, s * 0.1F);
      if ("target_aura".equals(id)) {
         Ui.arc(cx, cy, r * 0.62F, t, 0.0F, 360.0F, false, c);
         Ui.circle(cx, cy, r * 0.2F, c);
         Ui.line(cx, cy - r, cx, cy - r * 0.78F, t, true, c);
         Ui.line(cx, cy + r * 0.78F, cx, cy + r, t, true, c);
         Ui.line(cx - r, cy, cx - r * 0.78F, cy, t, true, c);
         Ui.line(cx + r * 0.78F, cy, cx + r, cy, t, true, c);
      } else if ("hit_effect".equals(id)) {
         for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0 + (Math.PI / 8);
            float in = r * 0.32F;
            float out = i % 2 == 0 ? r : r * 0.68F;
            Ui.line(cx + (float)Math.cos(a) * in, cy + (float)Math.sin(a) * in, cx + (float)Math.cos(a) * out, cy + (float)Math.sin(a) * out, t, true, c);
         }

         Ui.circle(cx, cy, r * 0.16F, c);
      } else if ("kill_effects".equals(id)) {
         Ui.arc(cx, cy - r * 0.12F, r * 0.62F, t, 0.0F, 360.0F, false, c);
         Ui.circle(cx - r * 0.25F, cy - r * 0.12F, r * 0.15F, c);
         Ui.circle(cx + r * 0.25F, cy - r * 0.12F, r * 0.15F, c);
         Ui.line(cx - r * 0.3F, cy + r * 0.5F, cx - r * 0.3F, cy + r * 0.9F, t, true, c);
         Ui.line(cx, cy + r * 0.52F, cx, cy + r * 0.9F, t, true, c);
         Ui.line(cx + r * 0.3F, cy + r * 0.5F, cx + r * 0.3F, cy + r * 0.9F, t, true, c);
      } else if ("landing_impact".equals(id)) {
         Ui.line(cx, cy - r, cx, cy + r * 0.25F, t, true, c);
         Ui.line(cx, cy + r * 0.3F, cx - r * 0.38F, cy - r * 0.08F, t, true, c);
         Ui.line(cx, cy + r * 0.3F, cx + r * 0.38F, cy - r * 0.08F, t, true, c);
         Ui.line(cx - r, cy + r * 0.82F, cx + r, cy + r * 0.82F, t, true, c);
      } else if ("trails".equals(id)) {
         Ui.arc(cx + r * 0.45F, cy + r * 0.45F, r * 1.05F, t, 270.0F, 90.0F, true, c);
         Ui.arc(cx + r * 0.45F, cy + r * 0.45F, r * 0.6F, t * 0.85F, 285.0F, 70.0F, true, c);
         Ui.circle(cx + r * 0.45F, cy - r * 0.6F, r * 0.26F, c);
      } else if ("particles".equals(id)) {
         sparkle(cx - r * 0.3F, cy + r * 0.2F, r * 0.62F, t, c);
         sparkle(cx + r * 0.5F, cy - r * 0.45F, r * 0.38F, t * 0.85F, c);
         sparkle(cx + r * 0.55F, cy + r * 0.6F, r * 0.26F, t * 0.8F, c);
      } else if ("weapons".equals(id)) {
         Ui.line(cx - r * 0.55F, cy + r * 0.55F, cx + r * 0.9F, cy - r * 0.9F, t * 1.15F, true, c);
         Ui.line(cx - r * 0.75F, cy + r * 0.05F, cx - r * 0.05F, cy + r * 0.75F, t, true, c);
         Ui.circle(cx - r * 0.8F, cy + r * 0.8F, r * 0.17F, c);
      } else if ("glow".equals(id)) {
         Ui.circle(cx, cy, r * 0.36F, c);

         for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            Ui.line(
               cx + (float)Math.cos(a) * r * 0.6F,
               cy + (float)Math.sin(a) * r * 0.6F,
               cx + (float)Math.cos(a) * r * 0.92F,
               cy + (float)Math.sin(a) * r * 0.92F,
               t,
               true,
               c
            );
         }
      } else if ("ambience".equals(id)) {
         float[] ys = new float[]{-0.5F, 0.05F, 0.6F};
         float[] ends = new float[]{0.55F, 0.85F, 0.35F};

         for (int i = 0; i < 3; i++) {
            float y = cy + r * ys[i];
            float end = cx + r * ends[i];
            Ui.line(cx - r, y, end, y, t, true, c);
            Ui.arc(end, y - r * 0.2F, r * 0.2F, t, 90.0F, 180.0F, true, c);
         }
      } else if ("dynamic_skies".equals(id)) {
         Ui.arc(cx - r * 0.12F, cy + r * 0.05F, r * 0.62F, t * 1.9F, 180.0F, 230.0F, true, c);
         sparkle(cx + r * 0.55F, cy - r * 0.55F, r * 0.32F, t * 0.8F, c);
      } else if ("motion_blur".equals(id)) {
         Ui.circle(cx + r * 0.45F, cy, r * 0.42F, c);
         Ui.line(cx - r, cy - r * 0.42F, cx - r * 0.1F, cy - r * 0.42F, t, true, c);
         Ui.line(cx - r * 0.7F, cy, cx - r * 0.1F, cy, t, true, c);
         Ui.line(cx - r, cy + r * 0.42F, cx - r * 0.1F, cy + r * 0.42F, t, true, c);
      } else if ("watermark".equals(id)) {
         Ui.outline(cx - r, cy - r * 0.42F, s, r * 0.84F, r * 0.42F, t, c);
         Ui.circle(cx - r * 0.5F, cy, r * 0.16F, c);
         Ui.line(cx - r * 0.15F, cy, cx + r * 0.55F, cy, t, true, c);
      } else if ("ui".equals(id)) {
         Ui.outline(cx - r, cy - r * 0.75F, s, r * 1.5F, r * 0.25F, t, c);
         Ui.line(cx, cy - r * 0.35F, cx, cy + r * 0.05F, t * 0.9F, true, c);
         Ui.line(cx - r * 0.2F, cy - r * 0.15F, cx + r * 0.2F, cy - r * 0.15F, t * 0.9F, true, c);
         Ui.line(cx - r * 0.55F, cy + r * 0.42F, cx + r * 0.55F, cy + r * 0.42F, t, true, c);
      } else if ("misc".equals(id)) {
         float[] ys = new float[]{-0.55F, 0.0F, 0.55F};
         float[] knobs = new float[]{-0.35F, 0.4F, -0.05F};

         for (int i = 0; i < 3; i++) {
            float y = cy + r * ys[i];
            Ui.line(cx - r, y, cx + r, y, t, true, c);
            Ui.circle(cx + r * knobs[i], y, t * 1.7F, c);
         }
      } else {
         Ui.circle(cx, cy, r * 0.45F, c);
      }
   }

   private static void sparkle(float cx, float cy, float r, float t, int c) {
      Ui.line(cx, cy - r, cx, cy + r, t, true, c);
      Ui.line(cx - r, cy, cx + r, cy, t, true, c);
      Ui.circle(cx, cy, t * 0.9F, c);
   }

   public static void search(float cx, float cy, float s, int c) {
      float r = s * 0.3F;
      float t = Math.max(0.9F, s * 0.11F);
      float ox = cx - s * 0.08F;
      float oy = cy - s * 0.08F;
      Ui.outline(ox - r, oy - r, r * 2.0F, r * 2.0F, r, t, c);
      float k = 0.7071F;
      Ui.line(ox + r * k + t * 0.3F, oy + r * k + t * 0.3F, cx + s * 0.42F, cy + s * 0.42F, t * 1.15F, true, c);
   }

   public static void sections(float cx, float cy, float s, int c) {
      float t = Math.max(0.9F, s * 0.11F);
      float x = cx - s / 2.0F;

      for (int i = -1; i <= 1; i++) {
         float y = cy + i * s * 0.32F;
         Ui.circle(x + t, y, t * 0.95F, c);
         Ui.line(x + s * 0.3F, y, x + s, y, t, true, c);
      }
   }

   public static void reset(float cx, float cy, float r, int c) {
      float t = Math.max(0.8F, r * 0.26F);
      Ui.arc(cx, cy, r, t, 70.0F, 310.0F, true, c);
      double end = Math.toRadians(20.0);
      float ex = cx + (float)Math.sin(end) * r;
      float ey = cy - (float)Math.cos(end) * r;
      double dir = Math.atan2(Math.sin(end), Math.cos(end));

      for (int k = -1; k <= 1; k += 2) {
         double a = dir + Math.PI + k * Math.toRadians(40.0);
         Ui.line(ex, ey, ex + (float)Math.cos(a) * r * 0.75F, ey + (float)Math.sin(a) * r * 0.75F, t, true, c);
      }
   }

   public static void jump(float cx, float cy, int c) {
      for (int i = 0; i <= 6; i++) {
         float t = i / 6.0F;
         Ui.circle(cx - 3.2F + t * 6.4F, cy + 3.2F - t * 6.4F, 0.7F, c);
      }

      for (int var6 = 1; var6 <= 4; var6++) {
         float t = var6 * 0.9F;
         Ui.circle(cx + 3.2F - t, cy - 3.2F, 0.7F, c);
         Ui.circle(cx + 3.2F, cy - 3.2F + t, 0.7F, c);
      }
   }

   public static void back(float cx, float cy, float s, int c) {
      float r = s / 2.0F;
      float t = Math.max(0.8F, s * 0.12F);
      Ui.line(cx - r, cy, cx + r, cy, t, true, c);
      Ui.line(cx - r, cy, cx - r * 0.35F, cy - r * 0.62F, t, true, c);
      Ui.line(cx - r, cy, cx - r * 0.35F, cy + r * 0.62F, t, true, c);
   }

   public static void chevron(float cx, float cy, float size, float t, int c) {
      double angle = t * Math.PI / 2.0;
      double dx = Math.cos(angle);
      double dy = Math.sin(angle);
      float th = Math.max(0.9F, size * 0.42F);
      float tipX = (float)(cx + dx * size * 0.5);
      float tipY = (float)(cy + dy * size * 0.5);

      for (int s = -1; s <= 1; s += 2) {
         float ex = (float)(cx - dx * size * 0.5 - dy * size * s);
         float ey = (float)(cy - dy * size * 0.5 + dx * size * s);
         Ui.line(tipX, tipY, ex, ey, th, true, c);
      }
   }

   public static void move(float cx, float cy, float s, int c) {
      float r = s / 2.0F;
      float t = Math.max(0.8F, s * 0.11F);
      float h = r * 0.34F;
      Ui.line(cx - r, cy, cx + r, cy, t, true, c);
      Ui.line(cx, cy - r, cx, cy + r, t, true, c);

      for (int k = -1; k <= 1; k += 2) {
         float ex = cx + k * r;
         float ey = cy + k * r;
         Ui.line(ex, cy, ex - k * h, cy - h, t, true, c);
         Ui.line(ex, cy, ex - k * h, cy + h, t, true, c);
         Ui.line(cx, ey, cx - h, ey - k * h, t, true, c);
         Ui.line(cx, ey, cx + h, ey - k * h, t, true, c);
      }
   }

   public static void play(float cx, float cy, float s, int c) {
      int steps = 8;
      float w = s * 0.85F;
      float x0 = cx - w * 0.4F;
      float step = w / steps;

      for (int i = 0; i < steps; i++) {
         float h = s / 2.0F * (1.0F - (float)i / steps);
         Ui.rect(x0 + i * step, cy - h, step + 0.05F, h * 2.0F, c);
      }
   }
}
