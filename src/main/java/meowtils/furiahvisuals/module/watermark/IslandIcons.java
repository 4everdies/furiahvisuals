package meowtils.furiahvisuals.module.watermark;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Ellipse2D.Float;
import java.awt.geom.Line2D.Double;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.Map;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Ui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public final class IslandIcons {
   static final String DISCORD_SVG = "/assets/furiahsvisuals/discord/Discord-Symbol-Blurple.svg";
   static final int DISCORD_WHITE = -1;
   static final int DISCORD_BLURPLE = -10983950;
   static final int CELL = 128;
   static final int COLS = 8;
   static final int ROWS = 10;
   private static final int W = 1024;
   private static final int H = 1280;
   private static int texture;
   private static boolean failed;

   private IslandIcons() {
   }

   public static int texture() {
      if (texture == 0 && !failed) {
         try {
            texture = build();
         } catch (Throwable var1) {
            failed = true;
            FuriahsVisuals.LOG.warn("WaterMark icons could not be made", var1);
         }
      }

      return texture;
   }

   public static float u0(IslandIcons.Icon icon) {
      return icon.ordinal() % 8 * 128 / 1024.0F;
   }

   public static float v0(IslandIcons.Icon icon) {
      return icon.ordinal() / 8 * 128 / 1280.0F;
   }

   public static float du() {
      return 0.125F;
   }

   public static float dv() {
      return 0.1F;
   }

   public static void draw(IslandIcons.Icon icon, float x, float y, float size, int color) {
      int tex = texture();
      if (tex != 0) {
         Ui.image(tex, x, y, size, size, u0(icon), v0(icon), u0(icon) + du(), v0(icon) + dv(), color);
      }
   }

   public static void drawPlay(float cx, float cy, float size, int color) {
      int tex = texture();
      if (tex != 0) {
         Ui.image(
            tex,
            cx - size / 2.0F,
            cy - size / 2.0F,
            size,
            size,
            u0(IslandIcons.Icon.PLAY),
            v0(IslandIcons.Icon.PLAY),
            u0(IslandIcons.Icon.PLAY) + du(),
            v0(IslandIcons.Icon.PLAY) + dv(),
            color
         );
      }
   }

   public static Map<IslandIcons.Icon, BufferedImage> images() {
      BufferedImage all = paint();
      EnumMap<IslandIcons.Icon, BufferedImage> out = new EnumMap<>(IslandIcons.Icon.class);

      for (IslandIcons.Icon icon : IslandIcons.Icon.values()) {
         out.put(icon, all.getSubimage(icon.ordinal() % 8 * 128, icon.ordinal() / 8 * 128, 128, 128));
      }

      return out;
   }

   static BufferedImage paint() {
      BufferedImage img = new BufferedImage(1024, 1280, 2);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setColor(Color.WHITE);

      for (IslandIcons.Icon icon : IslandIcons.Icon.values()) {
         AffineTransform saved = g.getTransform();
         g.translate(icon.ordinal() % 8 * 128, icon.ordinal() / 8 * 128);
         g.setClip(0, 0, 128, 128);
         draw(g, icon);
         g.setClip(null);
         g.setTransform(saved);
      }

      g.dispose();
      return img;
   }

   private static int build() {
      BufferedImage img = paint();
      int levels = 5;
      int[][] data = new int[levels][];
      int[] alpha = new int[1310720];
      int[] argb = img.getRGB(0, 0, 1024, 1280, null, 0, 1024);

      for (int i = 0; i < argb.length; i++) {
         alpha[i] = argb[i] >>> 24;
      }

      int w = 1024;
      int h = 1280;

      for (int l = 0; l < levels; l++) {
         int[] level = new int[w * h];

         for (int i = 0; i < level.length; i++) {
            level[i] = alpha[i] << 24 | 16777215;
         }

         data[l] = level;
         if (l + 1 < levels) {
            int nw = w / 2;
            int nh = h / 2;
            int[] next = new int[nw * nh];

            for (int y = 0; y < nh; y++) {
               for (int x = 0; x < nw; x++) {
                  int i = y * 2 * w + x * 2;
                  next[y * nw + x] = (alpha[i] + alpha[i + 1] + alpha[i + w] + alpha[i + w + 1] + 2) / 4;
               }
            }

            alpha = next;
            w = nw;
            h = nh;
         }
      }

      int id = TextureUtil.glGenTextures();
      GlStateManager.bindTexture(id);
      GL11.glTexParameteri(3553, 33085, levels - 1);
      GL11.glTexParameteri(3553, 10241, 9987);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      int var16 = 1024;
      int var17 = 1280;

      for (int l = 0; l < levels; l++) {
         IntBuffer buffer = BufferUtils.createIntBuffer(var16 * var17);
         ((Buffer)buffer.put(data[l])).flip();
         GL11.glTexImage2D(3553, l, 32856, var16, var17, 0, 32993, 33639, buffer);
         var16 /= 2;
         var17 /= 2;
      }

      return id;
   }

   private static BasicStroke stroke(float width) {
      return new BasicStroke(width, 1, 1);
   }

   private static void draw(Graphics2D g, IslandIcons.Icon icon) {
      switch (icon) {
         case GLOBE:
            g.setStroke(stroke(11.0F));
            g.draw(new Float(18.0F, 18.0F, 92.0F, 92.0F));
            g.draw(new Float(44.0F, 18.0F, 40.0F, 92.0F));
            g.draw(new java.awt.geom.Line2D.Float(20.0F, 64.0F, 108.0F, 64.0F));
            break;
         case MONITOR:
            g.setStroke(stroke(11.0F));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(16.0F, 22.0F, 96.0F, 66.0F, 22.0F, 22.0F));
            g.draw(new java.awt.geom.Line2D.Float(64.0F, 90.0F, 64.0F, 104.0F));
            g.draw(new java.awt.geom.Line2D.Float(42.0F, 106.0F, 86.0F, 106.0F));
            break;
         case CLOCK:
            g.setStroke(stroke(11.0F));
            g.draw(new Float(16.0F, 16.0F, 96.0F, 96.0F));
            g.draw(new java.awt.geom.Line2D.Float(64.0F, 64.0F, 64.0F, 34.0F));
            g.draw(new java.awt.geom.Line2D.Float(64.0F, 64.0F, 86.0F, 76.0F));
            break;
         case SPEED:
            g.setStroke(stroke(11.0F));
            g.draw(new java.awt.geom.Arc2D.Float(18.0F, 26.0F, 92.0F, 92.0F, -25.0F, 230.0F, 0));
            g.draw(new java.awt.geom.Line2D.Float(64.0F, 72.0F, 90.0F, 46.0F));
            g.fill(new Float(55.0F, 63.0F, 18.0F, 18.0F));
            break;
         case CHIP:
            g.setStroke(stroke(10.0F));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(32.0F, 32.0F, 64.0F, 64.0F, 18.0F, 18.0F));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(50.0F, 50.0F, 28.0F, 28.0F, 8.0F, 8.0F));

            for (int ixxx = 0; ixxx < 3; ixxx++) {
               float px = 46 + ixxx * 18;
               g.draw(new java.awt.geom.Line2D.Float(px, 16.0F, px, 28.0F));
               g.draw(new java.awt.geom.Line2D.Float(px, 100.0F, px, 112.0F));
               g.draw(new java.awt.geom.Line2D.Float(16.0F, px, 28.0F, px));
               g.draw(new java.awt.geom.Line2D.Float(100.0F, px, 112.0F, px));
            }
            break;
         case SUN:
            g.fill(new Float(40.0F, 40.0F, 48.0F, 48.0F));
            g.setStroke(stroke(9.0F));

            for (int ixxx = 0; ixxx < 8; ixxx++) {
               double ax = ixxx * Math.PI / 4.0;
               g.draw(new Double(64.0 + Math.cos(ax) * 36.0, 64.0 + Math.sin(ax) * 36.0, 64.0 + Math.cos(ax) * 50.0, 64.0 + Math.sin(ax) * 50.0));
            }
            break;
         case MOON: {
            Area moon = new Area(new Float(18.0F, 18.0F, 90.0F, 90.0F));
            moon.subtract(new Area(new Float(44.0F, 8.0F, 82.0F, 82.0F)));
            g.fill(moon);
            break;
         }
         case CLOUD:
            g.fill(cloud(0.0F, 0.0F, 1.0F));
            break;
         case RAIN:
            g.fill(cloud(0.0F, -16.0F, 0.9F));
            g.setStroke(stroke(8.0F));

            for (int ixxx = 0; ixxx < 3; ixxx++) {
               float x = 40 + ixxx * 22;
               g.draw(new java.awt.geom.Line2D.Float(x + 6.0F, 90.0F, x - 2.0F, 110.0F));
            }
            break;
         case SNOW:
            g.fill(cloud(0.0F, -16.0F, 0.9F));

            for (int ixxx = 0; ixxx < 3; ixxx++) {
               float x = 38 + ixxx * 24;
               g.fill(new Float(x - 6.0F, 92 + ixxx % 2 * 8, 12.0F, 12.0F));
            }
            break;
         case STORM: {
            Area a = new Area(cloud(0.0F, -18.0F, 0.9F));
            java.awt.geom.Path2D.Float bolt = new java.awt.geom.Path2D.Float();
            bolt.moveTo(66.0, 56.0);
            bolt.lineTo(46.0, 90.0);
            bolt.lineTo(62.0, 90.0);
            bolt.lineTo(54.0, 116.0);
            bolt.lineTo(84.0, 78.0);
            bolt.lineTo(68.0, 78.0);
            bolt.lineTo(78.0, 56.0);
            bolt.closePath();
            Area gap = new Area(new BasicStroke(10.0F, 1, 1).createStrokedShape(bolt));
            a.subtract(gap);
            g.fill(a);
            g.fill(bolt);
            break;
         }
         case NOTE:
            AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians(-20.0), 46.0, 92.0);
            g.fill(at.createTransformedShape(new Float(26.0F, 78.0F, 40.0F, 28.0F)));
            g.setStroke(stroke(10.0F));
            g.draw(new java.awt.geom.Line2D.Float(63.0F, 88.0F, 63.0F, 20.0F));
            g.draw(new java.awt.geom.QuadCurve2D.Float(63.0F, 20.0F, 100.0F, 30.0F, 92.0F, 64.0F));
            break;
         case PLAY: {
            java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
            p.moveTo(40.0, 26.0);
            p.lineTo(102.0, 64.0);
            p.lineTo(40.0, 102.0);
            p.closePath();
            g.setStroke(stroke(10.0F));
            g.draw(p);
            g.fill(p);
            break;
         }
         case PAUSE:
            g.fill(new java.awt.geom.RoundRectangle2D.Float(32.0F, 24.0F, 22.0F, 80.0F, 12.0F, 12.0F));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(74.0F, 24.0F, 22.0F, 80.0F, 12.0F, 12.0F));
            break;
         case PREV:
         case NEXT: {
            java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
            p.moveTo(100.0, 30.0);
            p.lineTo(46.0, 64.0);
            p.lineTo(100.0, 98.0);
            p.closePath();
            Area a = new Area(p);
            a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(26.0F, 28.0F, 16.0F, 72.0F, 10.0F, 10.0F)));
            Shape s = a;
            if (icon == IslandIcons.Icon.NEXT) {
               s = new AffineTransform(-1.0F, 0.0F, 0.0F, 1.0F, 128.0F, 0.0F).createTransformedShape(a);
            }

            g.setStroke(stroke(6.0F));
            g.draw(s);
            g.fill(s);
            break;
         }
         case APP:
            g.setStroke(stroke(11.0F));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(18.0F, 24.0F, 92.0F, 80.0F, 22.0F, 22.0F));
            g.draw(new java.awt.geom.Line2D.Float(20.0F, 48.0F, 108.0F, 48.0F));
            g.fill(new Float(30.0F, 32.0F, 8.0F, 8.0F));
            g.fill(new Float(42.0F, 32.0F, 8.0F, 8.0F));
            break;
         case AURA:
            g.setStroke(stroke(8.0F));
            g.draw(new Float(22.0F, 22.0F, 84.0F, 84.0F));
            g.fill(new Float(50.0F, 50.0F, 28.0F, 28.0F));
            g.draw(new java.awt.geom.Line2D.Float(64.0F, 10.0F, 64.0F, 22.0F));
            g.draw(new java.awt.geom.Line2D.Float(64.0F, 106.0F, 64.0F, 118.0F));
            g.draw(new java.awt.geom.Line2D.Float(10.0F, 64.0F, 22.0F, 64.0F));
            g.draw(new java.awt.geom.Line2D.Float(106.0F, 64.0F, 118.0F, 64.0F));
            break;
         case HIT: {
            GeneralPath p = new GeneralPath();

            for (int ixx = 0; ixx < 16; ixx++) {
               double ax = ixx * Math.PI / 8.0 - (Math.PI / 2);
               double r = ixx % 2 == 0 ? 52.0 : 24.0;
               double x = 64.0 + Math.cos(ax) * r;
               double y = 64.0 + Math.sin(ax) * r;
               if (ixx == 0) {
                  p.moveTo(x, y);
               } else {
                  p.lineTo(x, y);
               }
            }

            p.closePath();
            g.fill(p);
            break;
         }
         case LANDING:
            g.setStroke(stroke(9.0F));
            g.draw(new java.awt.geom.Line2D.Float(64.0F, 16.0F, 64.0F, 70.0F));
            java.awt.geom.Path2D.Float head = new java.awt.geom.Path2D.Float();
            head.moveTo(44.0, 54.0);
            head.lineTo(64.0, 76.0);
            head.lineTo(84.0, 54.0);
            g.draw(head);
            g.draw(new java.awt.geom.Arc2D.Float(18.0F, 80.0F, 92.0F, 34.0F, 0.0F, -180.0F, 0));
            g.draw(new java.awt.geom.Line2D.Float(26.0F, 88.0F, 16.0F, 80.0F));
            g.draw(new java.awt.geom.Line2D.Float(102.0F, 88.0F, 112.0F, 80.0F));
            break;
         case GLOW:
            g.fill(sparkle(58.0F, 66.0F, 46.0F, 12.0F));
            g.fill(sparkle(96.0F, 30.0F, 18.0F, 5.0F));
            break;
         case PARTICLES:
            g.fill(new Float(24.0F, 60.0F, 26.0F, 26.0F));
            g.fill(new Float(60.0F, 28.0F, 20.0F, 20.0F));
            g.fill(new Float(78.0F, 70.0F, 30.0F, 30.0F));
            g.fill(new Float(40.0F, 96.0F, 14.0F, 14.0F));
            g.fill(new Float(92.0F, 34.0F, 12.0F, 12.0F));
            break;
         case SKY: {
            Area moon = new Area(new Float(18.0F, 34.0F, 70.0F, 70.0F));
            moon.subtract(new Area(new Float(40.0F, 26.0F, 64.0F, 64.0F)));
            g.fill(moon);
            g.fill(sparkle(92.0F, 36.0F, 20.0F, 5.0F));
            g.fill(sparkle(104.0F, 80.0F, 13.0F, 3.5F));
            break;
         }
         case GEAR:
            Area gear = new Area(new Float(28.0F, 28.0F, 72.0F, 72.0F));

            for (int ixx = 0; ixx < 8; ixx++) {
               AffineTransform atx = AffineTransform.getRotateInstance(ixx * Math.PI / 4.0, 64.0, 64.0);
               gear.add(new Area(atx.createTransformedShape(new java.awt.geom.RoundRectangle2D.Float(55.0F, 12.0F, 18.0F, 30.0F, 8.0F, 8.0F))));
            }

            gear.subtract(new Area(new Float(47.0F, 47.0F, 34.0F, 34.0F)));
            g.fill(gear);
            break;
         case LAYERS:
            g.fill(new java.awt.geom.RoundRectangle2D.Float(22.0F, 22.0F, 84.0F, 22.0F, 20.0F, 20.0F));
            g.setStroke(stroke(8.0F));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(26.0F, 58.0F, 76.0F, 16.0F, 16.0F, 16.0F));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(32.0F, 90.0F, 64.0F, 14.0F, 14.0F, 14.0F));
            break;
         case COMPASS: {
            g.setStroke(stroke(10.0F));
            g.draw(new Float(18.0F, 18.0F, 92.0F, 92.0F));
            java.awt.geom.Path2D.Float n = new java.awt.geom.Path2D.Float();
            n.moveTo(64.0, 30.0);
            n.lineTo(78.0, 64.0);
            n.lineTo(50.0, 64.0);
            n.closePath();
            g.fill(n);
            java.awt.geom.Path2D.Float s = new java.awt.geom.Path2D.Float();
            s.moveTo(64.0, 98.0);
            s.lineTo(78.0, 64.0);
            s.lineTo(50.0, 64.0);
            s.closePath();
            g.draw(s);
            break;
         }
         case SIGNAL:
            for (int ixx = 0; ixx < 4; ixx++) {
               float hx = 26 + ixx * 22;
               g.fill(new java.awt.geom.RoundRectangle2D.Float(20 + ixx * 26, 110.0F - hx, 18.0F, hx, 8.0F, 8.0F));
            }
            break;
         case WIFI:
            g.fill(new Float(54.0F, 90.0F, 20.0F, 20.0F));
            g.setStroke(stroke(11.0F));

            for (int ixx = 0; ixx < 3; ixx++) {
               float r = 30 + ixx * 24;
               g.draw(new java.awt.geom.Arc2D.Float(64.0F - r, 100.0F - r, 2.0F * r, 2.0F * r, 45.0F, 90.0F, 0));
            }
            break;
         case CHART:
            g.fill(new java.awt.geom.RoundRectangle2D.Float(20.0F, 66.0F, 24.0F, 44.0F, 9.0F, 9.0F));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(52.0F, 42.0F, 24.0F, 68.0F, 9.0F, 9.0F));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(84.0F, 18.0F, 24.0F, 92.0F, 9.0F, 9.0F));
            break;
         case BOLT: {
            java.awt.geom.Path2D.Float b = new java.awt.geom.Path2D.Float();
            b.moveTo(72.0, 10.0);
            b.lineTo(32.0, 70.0);
            b.lineTo(60.0, 70.0);
            b.lineTo(50.0, 118.0);
            b.lineTo(96.0, 54.0);
            b.lineTo(68.0, 54.0);
            b.lineTo(84.0, 10.0);
            b.closePath();
            g.setStroke(stroke(5.0F));
            g.draw(b);
            g.fill(b);
            break;
         }
         case SERVER:
            g.setStroke(stroke(10.0F));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(20.0F, 20.0F, 88.0F, 38.0F, 16.0F, 16.0F));
            g.draw(new java.awt.geom.RoundRectangle2D.Float(20.0F, 70.0F, 88.0F, 38.0F, 16.0F, 16.0F));
            g.fill(new Float(32.0F, 33.0F, 13.0F, 13.0F));
            g.fill(new Float(32.0F, 83.0F, 13.0F, 13.0F));
            g.draw(new java.awt.geom.Line2D.Float(60.0F, 39.0F, 92.0F, 39.0F));
            g.draw(new java.awt.geom.Line2D.Float(60.0F, 89.0F, 92.0F, 89.0F));
            break;
         case HOURGLASS:
            g.setStroke(stroke(10.0F));
            g.draw(new java.awt.geom.Line2D.Float(28.0F, 16.0F, 100.0F, 16.0F));
            g.draw(new java.awt.geom.Line2D.Float(28.0F, 112.0F, 100.0F, 112.0F));
            java.awt.geom.Path2D.Float glass = new java.awt.geom.Path2D.Float();
            glass.moveTo(38.0, 18.0);
            glass.lineTo(90.0, 18.0);
            glass.lineTo(69.0, 64.0);
            glass.lineTo(90.0, 110.0);
            glass.lineTo(38.0, 110.0);
            glass.lineTo(59.0, 64.0);
            glass.closePath();
            g.setStroke(stroke(8.0F));
            g.draw(glass);
            java.awt.geom.Path2D.Float sand = new java.awt.geom.Path2D.Float();
            sand.moveTo(47.0, 104.0);
            sand.lineTo(81.0, 104.0);
            sand.lineTo(64.0, 80.0);
            sand.closePath();
            g.fill(sand);
            break;
         case THERMO:
            Area t = new Area(new java.awt.geom.RoundRectangle2D.Float(50.0F, 12.0F, 28.0F, 72.0F, 28.0F, 28.0F));
            t.add(new Area(new Float(40.0F, 66.0F, 48.0F, 48.0F)));
            g.setStroke(stroke(9.0F));
            g.draw(t);
            g.fill(new Float(52.0F, 78.0F, 24.0F, 24.0F));
            g.fill(new java.awt.geom.RoundRectangle2D.Float(60.0F, 36.0F, 8.0F, 50.0F, 8.0F, 8.0F));
            break;
         case PIN:
            Area pin = new Area(new Float(28.0F, 12.0F, 72.0F, 72.0F));
            java.awt.geom.Path2D.Float tip = new java.awt.geom.Path2D.Float();
            tip.moveTo(32.0, 60.0);
            tip.lineTo(64.0, 118.0);
            tip.lineTo(96.0, 60.0);
            tip.closePath();
            pin.add(new Area(tip));
            pin.subtract(new Area(new Float(49.0F, 33.0F, 30.0F, 30.0F)));
            g.fill(pin);
            break;
         case HEART: {
            java.awt.geom.Path2D.Float h = new java.awt.geom.Path2D.Float();
            h.moveTo(64.0, 112.0);
            h.curveTo(22.0, 82.0, 10.0, 56.0, 24.0, 34.0);
            h.curveTo(38.0, 14.0, 58.0, 18.0, 64.0, 38.0);
            h.curveTo(70.0, 18.0, 90.0, 14.0, 104.0, 34.0);
            h.curveTo(118.0, 56.0, 106.0, 82.0, 64.0, 112.0);
            h.closePath();
            g.fill(h);
            break;
         }
         case STAR: {
            GeneralPath p = new GeneralPath();

            for (int ix = 0; ix < 10; ix++) {
               double ax = (-Math.PI / 2) + ix * Math.PI / 5.0;
               double r = ix % 2 == 0 ? 56.0 : 23.0;
               double x = 64.0 + Math.cos(ax) * r;
               double y = 68.0 + Math.sin(ax) * r;
               if (ix == 0) {
                  p.moveTo(x, y);
               } else {
                  p.lineTo(x, y);
               }
            }

            p.closePath();
            g.setStroke(stroke(6.0F));
            g.draw(p);
            g.fill(p);
            break;
         }
         case CROWN:
            java.awt.geom.Path2D.Float c = new java.awt.geom.Path2D.Float();
            c.moveTo(20.0, 94.0);
            c.lineTo(22.0, 40.0);
            c.lineTo(45.0, 66.0);
            c.lineTo(64.0, 30.0);
            c.lineTo(83.0, 66.0);
            c.lineTo(106.0, 40.0);
            c.lineTo(108.0, 94.0);
            c.closePath();
            g.setStroke(stroke(6.0F));
            g.draw(c);
            g.fill(c);
            g.fill(new java.awt.geom.RoundRectangle2D.Float(20.0F, 102.0F, 88.0F, 12.0F, 6.0F, 6.0F));
            g.fill(new Float(15.0F, 31.0F, 14.0F, 14.0F));
            g.fill(new Float(57.0F, 20.0F, 14.0F, 14.0F));
            g.fill(new Float(99.0F, 31.0F, 14.0F, 14.0F));
            break;
         case FLAME:
            java.awt.geom.Path2D.Float f = new java.awt.geom.Path2D.Float();
            f.moveTo(64.0, 10.0);
            f.curveTo(100.0, 44.0, 108.0, 68.0, 101.0, 88.0);
            f.curveTo(95.0, 108.0, 80.0, 118.0, 64.0, 118.0);
            f.curveTo(46.0, 118.0, 32.0, 106.0, 27.0, 88.0);
            f.curveTo(22.0, 70.0, 34.0, 56.0, 45.0, 40.0);
            f.curveTo(47.0, 56.0, 53.0, 62.0, 59.0, 64.0);
            f.curveTo(57.0, 44.0, 58.0, 24.0, 64.0, 10.0);
            f.closePath();
            Area flame = new Area(f);
            java.awt.geom.Path2D.Float core = new java.awt.geom.Path2D.Float();
            core.moveTo(64.0, 72.0);
            core.curveTo(80.0, 88.0, 82.0, 98.0, 76.0, 106.0);
            core.curveTo(71.0, 112.0, 57.0, 112.0, 52.0, 106.0);
            core.curveTo(46.0, 98.0, 50.0, 86.0, 64.0, 72.0);
            core.closePath();
            flame.subtract(new Area(core));
            g.fill(flame);
            break;
         case SWORD: {
            Area s = new Area();
            java.awt.geom.Path2D.Float blade = new java.awt.geom.Path2D.Float();
            blade.moveTo(64.0, 8.0);
            blade.lineTo(73.0, 20.0);
            blade.lineTo(73.0, 80.0);
            blade.lineTo(55.0, 80.0);
            blade.lineTo(55.0, 20.0);
            blade.closePath();
            s.add(new Area(blade));
            s.add(new Area(new java.awt.geom.RoundRectangle2D.Float(38.0F, 80.0F, 52.0F, 11.0F, 6.0F, 6.0F)));
            s.add(new Area(new java.awt.geom.RoundRectangle2D.Float(58.0F, 90.0F, 12.0F, 18.0F, 4.0F, 4.0F)));
            s.add(new Area(new Float(56.0F, 104.0F, 16.0F, 16.0F)));
            g.fill(AffineTransform.getRotateInstance(Math.toRadians(45.0), 64.0, 64.0).createTransformedShape(s));
            break;
         }
         case GEM:
            java.awt.geom.Path2D.Float d = new java.awt.geom.Path2D.Float();
            d.moveTo(36.0, 22.0);
            d.lineTo(92.0, 22.0);
            d.lineTo(114.0, 48.0);
            d.lineTo(64.0, 112.0);
            d.lineTo(14.0, 48.0);
            d.closePath();
            Area gem = new Area(d);
            java.awt.geom.Path2D.Float facets = new java.awt.geom.Path2D.Float();
            facets.moveTo(14.0, 48.0);
            facets.lineTo(114.0, 48.0);
            facets.moveTo(48.0, 24.0);
            facets.lineTo(40.0, 48.0);
            facets.lineTo(64.0, 110.0);
            facets.moveTo(80.0, 24.0);
            facets.lineTo(88.0, 48.0);
            facets.lineTo(64.0, 110.0);
            gem.subtract(new Area(new BasicStroke(5.0F, 0, 1).createStrokedShape(facets)));
            g.fill(gem);
            break;
         case ARROWS: {
            g.setStroke(stroke(14.0F));
            java.awt.geom.Path2D.Float a = new java.awt.geom.Path2D.Float();
            a.moveTo(24.0, 30.0);
            a.lineTo(56.0, 64.0);
            a.lineTo(24.0, 98.0);
            a.moveTo(66.0, 30.0);
            a.lineTo(98.0, 64.0);
            a.lineTo(66.0, 98.0);
            g.draw(a);
            break;
         }
         case PARTLY:
            Shape cloud = cloud(12.0F, 18.0F, 0.8F);
            Area sun = new Area(new Float(16.0F, 16.0F, 52.0F, 52.0F));
            sun.subtract(new Area(new BasicStroke(12.0F, 1, 1).createStrokedShape(cloud)));
            sun.subtract(new Area(cloud));
            g.fill(sun);
            g.fill(cloud);
            break;
         case WIND:
            g.setStroke(stroke(10.0F));
            java.awt.geom.Path2D.Float top = new java.awt.geom.Path2D.Float();
            top.moveTo(18.0, 50.0);
            top.lineTo(78.0, 50.0);
            top.curveTo(96.0, 50.0, 102.0, 28.0, 86.0, 22.0);
            top.curveTo(76.0, 18.0, 68.0, 26.0, 72.0, 34.0);
            g.draw(top);
            java.awt.geom.Path2D.Float middle = new java.awt.geom.Path2D.Float();
            middle.moveTo(18.0, 72.0);
            middle.lineTo(94.0, 72.0);
            middle.curveTo(110.0, 72.0, 114.0, 94.0, 100.0, 100.0);
            middle.curveTo(90.0, 104.0, 82.0, 96.0, 86.0, 88.0);
            g.draw(middle);
            g.draw(new java.awt.geom.Line2D.Float(30.0F, 94.0F, 64.0F, 94.0F));
            break;
         case SKULL:
            Area skull = new Area(new Float(20.0F, 14.0F, 88.0F, 80.0F));
            skull.add(new Area(new java.awt.geom.RoundRectangle2D.Float(38.0F, 70.0F, 52.0F, 40.0F, 18.0F, 18.0F)));
            skull.subtract(new Area(new Float(34.0F, 44.0F, 24.0F, 26.0F)));
            skull.subtract(new Area(new Float(70.0F, 44.0F, 24.0F, 26.0F)));
            java.awt.geom.Path2D.Float nose = new java.awt.geom.Path2D.Float();
            nose.moveTo(64.0, 70.0);
            nose.lineTo(56.0, 84.0);
            nose.lineTo(72.0, 84.0);
            nose.closePath();
            skull.subtract(new Area(nose));

            for (int ix = 0; ix < 2; ix++) {
               float x = 54 + ix * 14;
               skull.subtract(new Area(new java.awt.geom.RoundRectangle2D.Float(x, 94.0F, 6.0F, 18.0F, 4.0F, 4.0F)));
            }

            g.fill(skull);
            break;
         case DISCORD:
            g.fill(discordLogo());
            break;
         case MIC:
            g.fill(mic());
            break;
         case MIC_OFF:
            g.fill(slashed(mic()));
            break;
         case HEADSET:
            g.fill(headset());
            break;
         case HEADSET_OFF:
            g.fill(slashed(headset()));
            break;
         case HANGUP: {
            Area h = new Area(stroke(20.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(18.0F, 38.0F, 92.0F, 64.0F, 28.0F, 124.0F, 0)));
            h.add(
               new Area(
                  AffineTransform.getRotateInstance(Math.toRadians(-28.0), 28.0, 76.0)
                     .createTransformedShape(new java.awt.geom.RoundRectangle2D.Float(12.0F, 64.0F, 32.0F, 26.0F, 12.0F, 12.0F))
               )
            );
            h.add(
               new Area(
                  AffineTransform.getRotateInstance(Math.toRadians(28.0), 100.0, 76.0)
                     .createTransformedShape(new java.awt.geom.RoundRectangle2D.Float(84.0F, 64.0F, 32.0F, 26.0F, 12.0F, 12.0F))
               )
            );
            g.fill(h);
            break;
         }
         case CHAT: {
            Area b = new Area(new java.awt.geom.RoundRectangle2D.Float(14.0F, 18.0F, 100.0F, 74.0F, 40.0F, 40.0F));
            java.awt.geom.Path2D.Float tail = new java.awt.geom.Path2D.Float();
            tail.moveTo(30.0, 80.0);
            tail.lineTo(22.0, 114.0);
            tail.lineTo(60.0, 88.0);
            tail.closePath();
            b.add(new Area(tail));

            for (int ix = 0; ix < 3; ix++) {
               b.subtract(new Area(new Float(35 + ix * 23, 46.0F, 14.0F, 14.0F)));
            }

            g.fill(b);
            break;
         }
         case AT: {
            Area a = new Area(stroke(11.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(16.0F, 16.0F, 96.0F, 96.0F, -38.0F, 318.0F, 0)));
            a.add(new Area(stroke(11.0F).createStrokedShape(new Float(44.0F, 42.0F, 40.0F, 44.0F))));
            a.add(new Area(stroke(11.0F).createStrokedShape(new java.awt.geom.Line2D.Float(84.0F, 46.0F, 84.0F, 76.0F))));
            a.add(new Area(stroke(11.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(84.0F, 58.0F, 24.0F, 36.0F, 180.0F, 150.0F, 0))));
            g.fill(a);
            break;
         }
         case SPEAKER:
            g.fill(speaker(true));
            break;
         case SPEAKER_OFF:
            g.fill(slashed(new Area(AffineTransform.getTranslateInstance(20.0, 0.0).createTransformedShape(speaker(false)))));
            break;
         case UI: {
            Area a = new Area(stroke(10.0F).createStrokedShape(new java.awt.geom.RoundRectangle2D.Float(16.0F, 22.0F, 96.0F, 84.0F, 24.0F, 24.0F)));
            a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(32.0F, 84.0F, 64.0F, 10.0F, 10.0F, 10.0F)));
            a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(72.0F, 36.0F, 26.0F, 30.0F, 10.0F, 10.0F)));
            g.fill(a);
            break;
         }
         case CROSSHAIR:
            g.setStroke(stroke(12.0F));
            g.draw(new java.awt.geom.Line2D.Float(64.0F, 16.0F, 64.0F, 44.0F));
            g.draw(new java.awt.geom.Line2D.Float(64.0F, 84.0F, 64.0F, 112.0F));
            g.draw(new java.awt.geom.Line2D.Float(16.0F, 64.0F, 44.0F, 64.0F));
            g.draw(new java.awt.geom.Line2D.Float(84.0F, 64.0F, 112.0F, 64.0F));
            g.fill(new Float(56.0F, 56.0F, 16.0F, 16.0F));
            break;
         case SCOREBOARD: {
            Area a = new Area(stroke(9.0F).createStrokedShape(new java.awt.geom.RoundRectangle2D.Float(22.0F, 16.0F, 84.0F, 96.0F, 20.0F, 20.0F)));
            a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(22.0F, 16.0F, 84.0F, 24.0F, 20.0F, 20.0F)));

            for (int ix = 0; ix < 3; ix++) {
               float y = 54 + ix * 18;
               a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(36.0F, y, 40.0F, 9.0F, 9.0F, 9.0F)));
               a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(82.0F, y, 12.0F, 9.0F, 9.0F, 9.0F)));
            }

            g.fill(a);
            break;
         }
         case TABLIST:
            Area back = new Area(new Float(70.0F, 28.0F, 30.0F, 30.0F));
            back.add(new Area(new java.awt.geom.Arc2D.Float(58.0F, 64.0F, 54.0F, 56.0F, 0.0F, 180.0F, 2)));
            Area front = new Area(new Float(26.0F, 22.0F, 36.0F, 36.0F));
            front.add(new Area(new java.awt.geom.Arc2D.Float(14.0F, 64.0F, 60.0F, 64.0F, 0.0F, 180.0F, 2)));
            back.subtract(new Area(stroke(12.0F).createStrokedShape(front)));
            back.add(front);
            g.fill(back);
            break;
         case NAMETAG: {
            java.awt.geom.Path2D.Float tag = new java.awt.geom.Path2D.Float();
            tag.moveTo(18.0, 40.0);
            tag.lineTo(78.0, 40.0);
            tag.lineTo(112.0, 64.0);
            tag.lineTo(78.0, 88.0);
            tag.lineTo(18.0, 88.0);
            tag.closePath();
            Area a = new Area(stroke(10.0F).createStrokedShape(tag));
            a.add(new Area(tag));
            a.subtract(new Area(new Float(76.0F, 56.0F, 16.0F, 16.0F)));
            a.subtract(new Area(new java.awt.geom.RoundRectangle2D.Float(30.0F, 58.0F, 34.0F, 12.0F, 12.0F, 12.0F)));
            g.fill(a);
            break;
         }
         case HOTBAR: {
            Area a = new Area();

            for (int i = 0; i < 4; i++) {
               float x = 14 + i * 26;
               if (i == 1) {
                  a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(x - 3.0F, 61.0F, 30.0F, 44.0F, 12.0F, 12.0F)));
               } else {
                  a.add(new Area(stroke(7.0F).createStrokedShape(new java.awt.geom.RoundRectangle2D.Float(x, 64.0F, 24.0F, 38.0F, 10.0F, 10.0F))));
               }
            }

            for (int var19 = 0; var19 < 3; var19++) {
               a.add(new Area(heart(30 + var19 * 34, 34.0F, 14.0F)));
            }

            g.fill(a);
            break;
         }
         case BACK:
         case FORWARD: {
            java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
            p.moveTo(80.0, 26.0);
            p.lineTo(42.0, 64.0);
            p.lineTo(80.0, 102.0);
            Shape s = (Shape)(icon == IslandIcons.Icon.FORWARD ? new AffineTransform(-1.0F, 0.0F, 0.0F, 1.0F, 128.0F, 0.0F).createTransformedShape(p) : p);
            g.setStroke(stroke(14.0F));
            g.draw(s);
            break;
         }
         case ARMOR: {
            java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
            p.moveTo(32.0, 20.0);
            p.lineTo(48.0, 20.0);
            p.curveTo(50.0, 34.0, 78.0, 34.0, 80.0, 20.0);
            p.lineTo(96.0, 20.0);
            p.lineTo(116.0, 44.0);
            p.lineTo(98.0, 58.0);
            p.lineTo(94.0, 52.0);
            p.lineTo(94.0, 104.0);
            p.quadTo(94.0, 110.0, 88.0, 110.0);
            p.lineTo(40.0, 110.0);
            p.quadTo(34.0, 110.0, 34.0, 104.0);
            p.lineTo(34.0, 52.0);
            p.lineTo(30.0, 58.0);
            p.lineTo(12.0, 44.0);
            p.closePath();
            Area a = new Area(p);
            a.subtract(new Area(new java.awt.geom.RoundRectangle2D.Float(60.0F, 46.0F, 8.0F, 52.0F, 8.0F, 8.0F)));
            g.fill(a);
            break;
         }
         case SHAKE: {
            Area a = new Area(stroke(9.0F).createStrokedShape(new java.awt.geom.RoundRectangle2D.Float(32.0F, 40.0F, 64.0F, 52.0F, 16.0F, 16.0F)));
            a.add(new Area(stroke(9.0F).createStrokedShape(new Float(53.0F, 55.0F, 22.0F, 22.0F))));
            a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(44.0F, 30.0F, 20.0F, 14.0F, 6.0F, 6.0F)));
            a.add(new Area(stroke(8.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(6.0F, 42.0F, 26.0F, 48.0F, 120.0F, 120.0F, 0))));
            a.add(new Area(stroke(8.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(96.0F, 42.0F, 26.0F, 48.0F, -60.0F, 120.0F, 0))));
            g.fill(a);
            break;
         }
         case SKIN: {
            Area a = new Area(new java.awt.geom.RoundRectangle2D.Float(42.0F, 12.0F, 44.0F, 44.0F, 10.0F, 10.0F));
            a.subtract(new Area(new java.awt.geom.RoundRectangle2D.Float(50.0F, 30.0F, 9.0F, 9.0F, 2.0F, 2.0F)));
            a.subtract(new Area(new java.awt.geom.RoundRectangle2D.Float(69.0F, 30.0F, 9.0F, 9.0F, 2.0F, 2.0F)));
            a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(22.0F, 62.0F, 84.0F, 50.0F, 24.0F, 24.0F)));
            g.fill(a);
            break;
         }
         case PALETTE: {
            java.awt.geom.Path2D.Float drop = new java.awt.geom.Path2D.Float();
            drop.moveTo(64.0, 12.0);
            drop.curveTo(64.0, 12.0, 100.0, 52.0, 100.0, 78.0);
            drop.curveTo(100.0, 98.0, 84.0, 114.0, 64.0, 114.0);
            drop.curveTo(44.0, 114.0, 28.0, 98.0, 28.0, 78.0);
            drop.curveTo(28.0, 52.0, 64.0, 12.0, 64.0, 12.0);
            drop.closePath();
            Area a = new Area(stroke(9.0F).createStrokedShape(drop));
            Area half = new Area(drop);
            half.intersect(new Area(new java.awt.geom.RoundRectangle2D.Float(0.0F, 0.0F, 64.0F, 128.0F, 0.0F, 0.0F)));
            a.add(half);
            g.fill(a);
            break;
         }
         case LIST:
            g.setStroke(stroke(12.0F));

            for (int i = 0; i < 3; i++) {
               float y = 34 + i * 30;
               g.fill(new Float(16.0F, y - 9.0F, 18.0F, 18.0F));
               g.draw(new java.awt.geom.Line2D.Float(50.0F, y, 110.0F, y));
            }
            break;
         case MOTION:
            g.fill(new Float(66.0F, 38.0F, 50.0F, 50.0F));
            g.setStroke(stroke(10.0F));
            g.draw(new java.awt.geom.Line2D.Float(14.0F, 46.0F, 50.0F, 46.0F));
            g.draw(new java.awt.geom.Line2D.Float(26.0F, 63.0F, 56.0F, 63.0F));
            g.draw(new java.awt.geom.Line2D.Float(14.0F, 80.0F, 50.0F, 80.0F));
            break;
         case TRAIL: {
            java.awt.geom.Path2D.Float band = new java.awt.geom.Path2D.Float();
            band.moveTo(12.0, 112.0);
            band.curveTo(40.0, 108.0, 66.0, 86.0, 84.0, 50.0);
            band.lineTo(96.0, 58.0);
            band.curveTo(76.0, 96.0, 46.0, 114.0, 12.0, 112.0);
            band.closePath();
            Area a = new Area(band);
            a.add(new Area(new Float(78.0F, 18.0F, 34.0F, 34.0F)));
            a.add(new Area(stroke(7.0F).createStrokedShape(new java.awt.geom.QuadCurve2D.Float(18.0F, 88.0F, 44.0F, 80.0F, 62.0F, 54.0F))));
            g.fill(a);
         }
      }
   }

   private static Shape heart(float cx, float cy, float r) {
      java.awt.geom.Path2D.Float p = new java.awt.geom.Path2D.Float();
      p.moveTo(cx, cy + r);
      p.curveTo(cx - r * 1.6F, cy - r * 0.1F, cx - r * 0.9F, cy - r * 1.3F, cx, cy - r * 0.45F);
      p.curveTo(cx + r * 0.9F, cy - r * 1.3F, cx + r * 1.6F, cy - r * 0.1F, cx, cy + r);
      p.closePath();
      return p;
   }

   private static Shape discordLogo() {
      try {
         SvgPath svg = SvgPath.resource("/assets/furiahsvisuals/discord/Discord-Symbol-Blurple.svg");
         double[] box = svg.viewBox;
         double scale = Math.min(100.0 / box[2], 100.0 / box[3]);
         AffineTransform at = new AffineTransform();
         at.translate(64.0 - box[2] * scale / 2.0, 64.0 - box[3] * scale / 2.0);
         at.scale(scale, scale);
         at.translate(-box[0], -box[1]);
         return at.createTransformedShape(svg.path);
      } catch (IOException var5) {
         FuriahsVisuals.LOG.warn("WaterMark: Discord's logo could not be read", var5);
         return new java.awt.geom.RoundRectangle2D.Float(24.0F, 34.0F, 80.0F, 60.0F, 24.0F, 24.0F);
      }
   }

   private static Area mic() {
      Area a = new Area(new java.awt.geom.RoundRectangle2D.Float(45.0F, 12.0F, 38.0F, 68.0F, 38.0F, 38.0F));
      a.add(new Area(stroke(10.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(29.0F, 30.0F, 70.0F, 68.0F, 180.0F, 180.0F, 0))));
      a.add(new Area(stroke(10.0F).createStrokedShape(new java.awt.geom.Line2D.Float(64.0F, 98.0F, 64.0F, 112.0F))));
      a.add(new Area(stroke(10.0F).createStrokedShape(new java.awt.geom.Line2D.Float(46.0F, 114.0F, 82.0F, 114.0F))));
      return a;
   }

   private static Area headset() {
      Area a = new Area(stroke(11.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(20.0F, 20.0F, 88.0F, 88.0F, 0.0F, 180.0F, 0)));
      a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(14.0F, 62.0F, 28.0F, 46.0F, 14.0F, 14.0F)));
      a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(86.0F, 62.0F, 28.0F, 46.0F, 14.0F, 14.0F)));
      return a;
   }

   private static Area speaker(boolean waves) {
      java.awt.geom.Path2D.Float cone = new java.awt.geom.Path2D.Float();
      cone.moveTo(18.0, 48.0);
      cone.lineTo(40.0, 48.0);
      cone.lineTo(66.0, 22.0);
      cone.lineTo(66.0, 106.0);
      cone.lineTo(40.0, 80.0);
      cone.lineTo(18.0, 80.0);
      cone.closePath();
      Area a = new Area(stroke(6.0F).createStrokedShape(cone));
      a.add(new Area(cone));
      if (waves) {
         a.add(new Area(stroke(9.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(58.0F, 42.0F, 32.0F, 44.0F, -55.0F, 110.0F, 0))));
         a.add(new Area(stroke(9.0F).createStrokedShape(new java.awt.geom.Arc2D.Float(56.0F, 24.0F, 58.0F, 80.0F, -55.0F, 110.0F, 0))));
      }

      return a;
   }

   private static Area slashed(Area icon) {
      java.awt.geom.Line2D.Float line = new java.awt.geom.Line2D.Float(22.0F, 16.0F, 106.0F, 112.0F);
      Area a = new Area(icon);
      a.subtract(new Area(stroke(26.0F).createStrokedShape(line)));
      a.add(new Area(stroke(11.0F).createStrokedShape(line)));
      return a;
   }

   private static Shape cloud(float dx, float dy, float s) {
      Area a = new Area(new Float(20.0F, 58.0F, 40.0F, 40.0F));
      a.add(new Area(new Float(38.0F, 34.0F, 54.0F, 54.0F)));
      a.add(new Area(new Float(70.0F, 52.0F, 40.0F, 40.0F)));
      a.add(new Area(new java.awt.geom.RoundRectangle2D.Float(24.0F, 66.0F, 82.0F, 32.0F, 30.0F, 30.0F)));
      AffineTransform at = new AffineTransform();
      at.translate(64.0F + dx, 66.0F + dy);
      at.scale(s, s);
      at.translate(-64.0, -66.0);
      return at.createTransformedShape(a);
   }

   private static Shape sparkle(float cx, float cy, float outer, float inner) {
      GeneralPath p = new GeneralPath();

      for (int i = 0; i < 8; i++) {
         double a = i * Math.PI / 4.0 - (Math.PI / 2);
         double r = i % 2 == 0 ? outer : inner;
         double x = cx + Math.cos(a) * r;
         double y = cy + Math.sin(a) * r;
         if (i == 0) {
            p.moveTo(x, y);
         } else {
            p.lineTo(x, y);
         }
      }

      p.closePath();
      return p;
   }

   public static enum Icon {
      GLOBE,
      MONITOR,
      CLOCK,
      SPEED,
      CHIP,
      SUN,
      MOON,
      CLOUD,
      RAIN,
      SNOW,
      STORM,
      NOTE,
      PLAY,
      PAUSE,
      PREV,
      NEXT,
      APP,
      AURA,
      HIT,
      LANDING,
      GLOW,
      PARTICLES,
      SKY,
      GEAR,
      LAYERS,
      COMPASS,
      SIGNAL,
      WIFI,
      CHART,
      BOLT,
      SERVER,
      HOURGLASS,
      THERMO,
      PIN,
      HEART,
      STAR,
      CROWN,
      FLAME,
      SWORD,
      GEM,
      ARROWS,
      PARTLY,
      WIND,
      SKULL,
      DISCORD,
      MIC,
      MIC_OFF,
      HEADSET,
      HEADSET_OFF,
      HANGUP,
      CHAT,
      AT,
      SPEAKER,
      SPEAKER_OFF,
      UI,
      CROSSHAIR,
      SCOREBOARD,
      TABLIST,
      NAMETAG,
      HOTBAR,
      BACK,
      FORWARD,
      ARMOR,
      SHAKE,
      SKIN,
      PALETTE,
      LIST,
      MOTION,
      TRAIL;
   }
}
