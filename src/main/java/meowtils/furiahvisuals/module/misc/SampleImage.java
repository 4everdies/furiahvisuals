package meowtils.furiahvisuals.module.misc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D.Float;
import java.awt.image.BufferedImage;
import java.util.Random;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

final class SampleImage {
   static final int W = 480;
   static final int H = 270;
   private static DynamicTexture texture;

   private SampleImage() {
   }

   static int texture() {
      if (texture == null) {
         texture = new DynamicTexture(draw());
         GlStateManager.bindTexture(texture.getGlTextureId());
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
      }

      return texture.getGlTextureId();
   }

   private static BufferedImage draw() {
      BufferedImage img = new BufferedImage(480, 270, 2);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setPaint(new GradientPaint(0.0F, 0.0F, new Color(3105736), 0.0F, 140.0F, new Color(11064575)));
      g.fillRect(0, 0, 480, 150);
      g.setPaint(
         new RadialGradientPaint(
            372.0F,
            52.0F,
            60.0F,
            new float[]{0.0F, 0.35F, 1.0F},
            new Color[]{new Color(255, 236, 160, 255), new Color(255, 214, 120, 130), new Color(255, 214, 120, 0)}
         )
      );
      g.fill(new Float(312.0F, -8.0F, 120.0F, 120.0F));
      g.setColor(new Color(16773816));
      g.fill(new Float(354.0F, 34.0F, 36.0F, 36.0F));
      g.setColor(new Color(255, 255, 255, 225));
      cloud(g, 70.0F, 46.0F, 1.0F);
      cloud(g, 210.0F, 70.0F, 0.75F);
      hills(g, 118, 16, 0.021, new Color(7248816), new Color(8366524));
      hills(g, 140, 22, 0.013, new Color(4098613), new Color(5810502));
      g.setPaint(new GradientPaint(0.0F, 150.0F, new Color(5216826), 0.0F, 270.0F, new Color(3108390)));
      g.fillRect(0, 168, 480, 102);
      g.setPaint(new GradientPaint(0.0F, 190.0F, new Color(3049176), 0.0F, 250.0F, new Color(1789845)));
      g.fill(new Float(-40.0F, 188.0F, 220.0F, 70.0F));
      g.setColor(new Color(255, 255, 255, 70));
      g.setStroke(new BasicStroke(2.0F));
      g.drawLine(20, 214, 60, 214);
      g.drawLine(70, 226, 120, 226);
      java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
      path.moveTo(300.0, 270.0);
      path.curveTo(300.0, 230.0, 255.0, 205.0, 262.0, 172.0);
      path.lineTo(276.0, 172.0);
      path.curveTo(282.0, 205.0, 350.0, 230.0, 372.0, 270.0);
      path.closePath();
      g.setColor(new Color(10262672));
      g.fill(path);
      g.setColor(new Color(9067058));
      g.fillRect(330, 140, 70, 46);
      g.setColor(new Color(7029283));

      for (int y = 148; y < 186; y += 8) {
         g.drawLine(330, y, 400, y);
      }

      g.setColor(new Color(11740702));
      java.awt.geom.Path2D.Float roof = new java.awt.geom.Path2D.Float();
      roof.moveTo(322.0, 142.0);
      roof.lineTo(365.0, 112.0);
      roof.lineTo(408.0, 142.0);
      roof.closePath();
      g.fill(roof);
      g.setColor(new Color(3810322));
      g.fillRect(358, 160, 14, 26);
      g.setColor(new Color(16767352));
      g.fillRect(338, 152, 12, 10);
      g.setColor(new Color(5978654));
      g.fillRect(438, 128, 8, 50);
      g.setColor(new Color(2914858));
      g.fill(new Float(414.0F, 92.0F, 56.0F, 52.0F));
      g.setColor(new Color(4036152));
      g.fill(new Float(422.0F, 98.0F, 34.0F, 30.0F));
      int[] colors = new int[]{15020075, 16041008, 9323721, 16777215, 16743080};
      Random r = new Random(7L);

      for (int i = 0; i < 46; i++) {
         int x = 190 + r.nextInt(280);
         int y = 196 + r.nextInt(70);
         if (x <= 286 || x >= 352 || y <= 200) {
            g.setColor(new Color(colors[i % colors.length]));
            g.fill(new Float(x, y, 5.0F, 5.0F));
         }
      }

      g.dispose();
      return img;
   }

   private static void cloud(Graphics2D g, float x, float y, float s) {
      g.fill(new Float(x, y, 46.0F * s, 22.0F * s));
      g.fill(new Float(x + 22.0F * s, y - 10.0F * s, 40.0F * s, 28.0F * s));
      g.fill(new Float(x + 48.0F * s, y, 40.0F * s, 20.0F * s));
   }

   private static void hills(Graphics2D g, int base, int height, double freq, Color dark, Color light) {
      java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
      p.moveTo(0.0, 270.0);

      for (int x = 0; x <= 480; x += 4) {
         double y = base - height * (0.6 * Math.sin(x * freq) + 0.4 * Math.sin(x * freq * 2.7 + 1.3));
         p.lineTo(x, y);
      }

      p.lineTo(480.0, 270.0);
      p.closePath();
      g.setPaint(new GradientPaint(0.0F, base - height, light, 0.0F, base + height, dark));
      g.fill(p);
   }
}
