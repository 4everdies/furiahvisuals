package meowtils.furiahvisuals.module.ui.itemhud;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D.Double;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

final class HudIcons {
   static final float GLOW = 1.6F;
   private static final int COLUMNS = HudIcons.Icon.values().length;
   private static final int ROWS = HudIcons.Layer.values().length;
   private static DynamicTexture texture;
   private static int builtPx = -1;
   private static int builtPad;
   private static int wantedPx = -1;
   private static volatile int[] pending;
   private static volatile int pendingPx = -1;
   private static volatile int pendingW;
   private static volatile int pendingH;
   private static int atlasW;
   private static int atlasH;
   private static ExecutorService worker;

   private HudIcons() {
   }

   static int pad(int px) {
      return 2 + (int)Math.ceil(px * 1.6F / 9.0F);
   }

   static boolean ready(int sizePx) {
      final int px = Math.max(9, Math.min(160, sizePx));
      int[] made = pending;
      if (made != null && pendingPx == px) {
         upload(made, pendingW, pendingH, px);
         pending = null;
      }

      if (texture != null && builtPx == px) {
         return true;
      } else {
         if (wantedPx != px) {
            wantedPx = px;
            worker().execute(new Runnable() {
               @Override
               public void run() {
                  HudIcons.build(px);
               }
            });
         }

         return texture != null;
      }
   }

   private static ExecutorService worker() {
      if (worker == null) {
         worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
               Thread t = new Thread(r, "Furiah's Visuals HUD icons");
               t.setDaemon(true);
               return t;
            }
         });
      }

      return worker;
   }

   static int texture() {
      return texture.getGlTextureId();
   }

   static boolean hasDetail(HudIcons.Icon icon) {
      return icon == HudIcons.Icon.FOOD_DRUMSTICK || icon == HudIcons.Icon.ARMOR_CHEST;
   }

   static void quad(WorldRenderer wr, HudIcons.Icon icon, HudIcons.Layer layer, float x, float y, float size, float from, float to, int top, int bottom) {
      int cell = builtPx + builtPad * 2;
      float m = layer == HudIcons.Layer.GLOW ? 1.6F : 0.0F;
      float l = from <= 0.0F ? -m : 9.0F * from;
      float r = to >= 1.0F ? 9.0F + m : 9.0F * to;
      float pu = builtPx / 9.0F;
      float sc = size / 9.0F;
      float ox = icon.ordinal() * cell + builtPad;
      float oy = layer.ordinal() * cell + builtPad;
      float u0 = (ox + l * pu) / atlasW;
      float u1 = (ox + r * pu) / atlasW;
      float v0 = (oy - m * pu) / atlasH;
      float v1 = (oy + (9.0F + m) * pu) / atlasH;
      float x0 = x + l * sc;
      float x1 = x + r * sc;
      float y0 = y - m * sc;
      float y1 = y + (9.0F + m) * sc;
      int tr = top >> 16 & 0xFF;
      int tg = top >> 8 & 0xFF;
      int tb = top & 0xFF;
      int ta = top >>> 24;
      int br = bottom >> 16 & 0xFF;
      int bg = bottom >> 8 & 0xFF;
      int bb = bottom & 0xFF;
      int ba = bottom >>> 24;
      wr.pos(x0, y1, 0.0).tex(u0, v1).color(br, bg, bb, ba).endVertex();
      wr.pos(x1, y1, 0.0).tex(u1, v1).color(br, bg, bb, ba).endVertex();
      wr.pos(x1, y0, 0.0).tex(u1, v0).color(tr, tg, tb, ta).endVertex();
      wr.pos(x0, y0, 0.0).tex(u0, v0).color(tr, tg, tb, ta).endVertex();
   }

   static void quad(WorldRenderer wr, HudIcons.Icon icon, HudIcons.Layer layer, float x, float y, float size, float from, float to, int color) {
      quad(wr, icon, layer, x, y, size, from, to, color, color);
   }

   private static void upload(int[] argb, int w, int h, int px) {
      if (texture != null) {
         texture.deleteGlTexture();
      }

      DynamicTexture t = new DynamicTexture(w, h);
      System.arraycopy(argb, 0, t.getTextureData(), 0, argb.length);
      t.updateDynamicTexture();
      GlStateManager.bindTexture(t.getGlTextureId());
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      texture = t;
      builtPx = px;
      builtPad = pad(px);
      atlasW = w;
      atlasH = h;
   }

   private static BasicStroke round(float width) {
      return new BasicStroke(width, 1, 1);
   }

   private static Shape rounded(Shape sharp, float r) {
      Area a = new Area(sharp);
      a.add(new Area(round(r * 2.0F).createStrokedShape(sharp)));
      return a;
   }

   private static Path2D polygon(double... xy) {
      Double p = new Double();
      p.moveTo(xy[0], xy[1]);

      for (int i = 2; i < xy.length; i += 2) {
         p.lineTo(xy[i], xy[i + 1]);
      }

      p.closePath();
      return p;
   }

   private static Shape scaled(Shape s, double k, double cx, double cy) {
      AffineTransform t = new AffineTransform();
      t.translate(cx, cy);
      t.scale(k, k);
      t.translate(-cx, -cy);
      return t.createTransformedShape(s);
   }

   private static Shape meat() {
      return AffineTransform.getRotateInstance(Math.PI / 4, 3.5, 3.5).createTransformedShape(new java.awt.geom.Ellipse2D.Double(0.45, 1.15, 6.1, 4.7));
   }

   private static Shape bone() {
      Area a = new Area(new BasicStroke(1.4F, 0, 1).createStrokedShape(new java.awt.geom.Line2D.Double(5.0, 5.0, 6.85, 6.85)));
      a.add(new Area(new java.awt.geom.Ellipse2D.Double(6.5, 5.45, 1.7, 1.7)));
      a.add(new Area(new java.awt.geom.Ellipse2D.Double(5.45, 6.5, 1.7, 1.7)));
      return a;
   }

   static Shape shape(HudIcons.Icon icon) {
      switch (icon) {
         case HEART: {
            Double p = new Double();
            p.moveTo(4.5, 8.25);
            p.curveTo(3.2, 7.2, 0.55, 5.4, 0.55, 3.15);
            p.curveTo(0.55, 1.65, 1.65, 0.75, 2.85, 0.75);
            p.curveTo(3.65, 0.75, 4.2, 1.2, 4.5, 1.85);
            p.curveTo(4.8, 1.2, 5.35, 0.75, 6.15, 0.75);
            p.curveTo(7.35, 0.75, 8.45, 1.65, 8.45, 3.15);
            p.curveTo(8.45, 5.4, 5.8, 7.2, 4.5, 8.25);
            p.closePath();
            return scaled(p, 0.9, 4.5, 4.5);
         }
         case HEART_SQUARE:
            return new java.awt.geom.RoundRectangle2D.Double(1.1, 1.1, 6.8, 6.8, 3.2, 3.2);
         case HEART_HEX: {
            Double p = new Double();

            for (int i = 0; i < 6; i++) {
               double ax = Math.toRadians(90 + 60 * i);
               double x = 4.5 + Math.cos(ax) * 3.4;
               double y = 4.5 - Math.sin(ax) * 3.4;
               if (i == 0) {
                  p.moveTo(x, y);
               } else {
                  p.lineTo(x, y);
               }
            }

            p.closePath();
            return rounded(p, 0.45F);
         }
         case HEART_CIRCLE:
            return new java.awt.geom.Ellipse2D.Double(1.0, 1.0, 7.0, 7.0);
         case FOOD_DRUMSTICK: {
            Area a = new Area(meat());
            a.add(new Area(bone()));
            return a;
         }
         case FOOD_BOLT:
            return rounded(polygon(5.55, 0.85, 1.95, 5.0, 4.25, 5.0, 3.45, 8.15, 7.05, 3.85, 4.75, 3.85), 0.3F);
         case FOOD_CIRCLE:
            return new java.awt.geom.Ellipse2D.Double(1.0, 1.0, 7.0, 7.0);
         case ARMOR_SHIELD: {
            Double p = new Double();
            p.moveTo(4.5, 0.8);
            p.curveTo(5.55, 1.4, 6.75, 1.7, 7.9, 1.75);
            p.curveTo(7.9, 5.2, 6.5, 7.3, 4.5, 8.3);
            p.curveTo(2.5, 7.3, 1.1, 5.2, 1.1, 1.75);
            p.curveTo(2.25, 1.7, 3.45, 1.4, 4.5, 0.8);
            p.closePath();
            return p;
         }
         case ARMOR_CHEST: {
            Double p = new Double();
            p.moveTo(3.15, 0.95);
            p.curveTo(3.45, 2.25, 5.55, 2.25, 5.85, 0.95);
            p.lineTo(7.3, 1.45);
            p.curveTo(8.2, 1.75, 8.35, 2.85, 7.7, 3.45);
            p.curveTo(6.95, 4.15, 6.75, 5.2, 6.8, 6.55);
            p.lineTo(6.75, 7.5);
            p.curveTo(5.6, 8.3, 3.4, 8.3, 2.25, 7.5);
            p.lineTo(2.2, 6.55);
            p.curveTo(2.25, 5.2, 2.05, 4.15, 1.3, 3.45);
            p.curveTo(0.65, 2.85, 0.8, 1.75, 1.7, 1.45);
            p.closePath();
            return p;
         }
         case BUBBLE:
            return new java.awt.geom.Ellipse2D.Double(1.3, 1.3, 6.4, 6.4);
         default: {
            Area a = new Area();

            for (int i = 0; i < 4; i++) {
               java.awt.geom.Arc2D.Double arc = new java.awt.geom.Arc2D.Double(1.1, 1.1, 6.8, 6.8, 20 + 90 * i, 50.0, 0);
               a.add(new Area(round(1.1F).createStrokedShape(arc)));
            }

            return a;
         }
      }
   }

   private static Shape spot(HudIcons.Icon icon, Shape s) {
      switch (icon) {
         case HEART:
            return AffineTransform.getRotateInstance(Math.toRadians(-38.0), 2.75, 2.85)
               .createTransformedShape(new java.awt.geom.Ellipse2D.Double(1.8, 2.3, 1.9, 1.1));
         case FOOD_DRUMSTICK:
            return AffineTransform.getRotateInstance(Math.toRadians(-45.0), 2.6, 2.6)
               .createTransformedShape(new java.awt.geom.Ellipse2D.Double(1.5, 2.05, 2.2, 1.1));
         default:
            Rectangle2D b = s.getBounds2D();
            double w = b.getWidth() * 0.34;
            double h = b.getHeight() * 0.17;
            double cx = b.getX() + b.getWidth() * 0.34;
            double cy = b.getY() + b.getHeight() * 0.24;
            return new java.awt.geom.Ellipse2D.Double(cx - w / 2.0, cy - h / 2.0, w, h);
      }
   }

   private static void build(int px) {
      try {
         int pad = pad(px);
         int cell = px + pad * 2;
         int w = cell * COLUMNS;
         int h = cell * ROWS;
         int[] argb = new int[w * h];

         for (HudIcons.Icon icon : HudIcons.Icon.values()) {
            for (HudIcons.Layer layer : HudIcons.Layer.values()) {
               float[] m = mask(icon, layer, px);
               int ox = icon.ordinal() * cell;
               int oy = layer.ordinal() * cell;

               for (int y = 0; y < cell; y++) {
                  for (int x = 0; x < cell; x++) {
                     int a = Math.round(Math.max(0.0F, Math.min(1.0F, m[y * cell + x])) * 255.0F);
                     argb[(oy + y) * w + ox + x] = a << 24 | 16777215;
                  }
               }
            }
         }

         pendingW = w;
         pendingH = h;
         pendingPx = px;
         pending = argb;
      } catch (Throwable var20) {
         FuriahsVisuals.LOG.warn("UI: the HUD icons could not be drawn", var20);
      }
   }

   static float[] mask(HudIcons.Icon icon, HudIcons.Layer layer, int px) {
      int pad = pad(px);
      int n = px + pad * 2;
      BufferedImage img = new BufferedImage(n, n, 2);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.translate(pad, pad);
      g.scale(px / 9.0, px / 9.0);
      Shape s = shape(icon);
      draw(g, icon, s, layer);
      g.dispose();
      float[] a = new float[n * n];
      int[] row = img.getRGB(0, 0, n, n, null, 0, n);

      for (int i = 0; i < a.length; i++) {
         a[i] = (row[i] >>> 24) / 255.0F;
      }

      if (layer == HudIcons.Layer.GLOW && icon != HudIcons.Icon.BUBBLE_POP) {
         float[] blur = blur(a, n, 0.8F * px / 9.0F);

         for (int i = 0; i < a.length; i++) {
            a[i] = Math.min(1.0F, blur[i] * 1.6F) * (1.0F - a[i]);
         }
      }

      return a;
   }

   private static void draw(Graphics2D g, HudIcons.Icon icon, Shape s, HudIcons.Layer layer) {
      boolean bubble = icon == HudIcons.Icon.BUBBLE;
      boolean pop = icon == HudIcons.Icon.BUBBLE_POP;
      switch (layer) {
         case FILL:
            if (bubble) {
               g.setColor(new Color(255, 255, 255, 80));
               g.fill(s);
               g.setStroke(round(0.9F));
               g.setColor(Color.WHITE);
               g.draw(s);
            } else {
               g.setColor(Color.WHITE);
               g.fill(s);
            }
            break;
         case GLOSS:
            if (!pop) {
               if (bubble) {
                  g.setColor(new Color(255, 255, 255, 240));
                  g.fill(new java.awt.geom.Ellipse2D.Double(2.55, 2.45, 1.9, 1.3));
               } else {
                  Rectangle2D b = s.getBounds2D();
                  Area inset = new Area(s);
                  inset.subtract(new Area(round(1.3F).createStrokedShape(s)));
                  g.clip(inset);
                  g.setPaint(
                     new GradientPaint(
                        0.0F, (float)b.getY(), new Color(255, 255, 255, 150), 0.0F, (float)(b.getY() + b.getHeight() * 0.55), new Color(255, 255, 255, 0)
                     )
                  );
                  g.fill(b);
                  g.setClip(null);
                  Area spot = new Area(spot(icon, s));
                  spot.intersect(new Area(s));
                  g.setColor(Color.WHITE);
                  g.fill(spot);
               }
            }
            break;
         case RIM:
            if (!bubble && !pop) {
               Rectangle2D b = s.getBounds2D();
               g.clip(s);
               g.setStroke(round(1.0F));
               g.setPaint(
                  new GradientPaint(0.0F, (float)b.getY(), new Color(255, 255, 255, 255), 0.0F, (float)(b.getY() + b.getHeight()), new Color(255, 255, 255, 70))
               );
               g.draw(s);
            }
            break;
         case DETAIL:
            Shape detail = detailShape(icon);
            if (detail != null) {
               g.setColor(Color.WHITE);
               g.fill(detail);
            }
            break;
         default:
            if (!pop) {
               g.setColor(Color.WHITE);
               g.fill(s);
            }
      }
   }

   private static float[] blur(float[] a, int n, float sigma) {
      int r = (int)Math.ceil(sigma * 3.0F);
      float[] k = new float[r * 2 + 1];
      float sum = 0.0F;

      for (int i2 = -r; i2 <= r; i2++) {
         k[i2 + r] = (float)Math.exp(-i2 * i2 / (2.0F * sigma * sigma));
         sum += k[i2 + r];
      }

      int var18 = 0;

      while (var18 < k.length) {
         int n2 = var18++;
         k[n2] /= sum;
      }

      float[] t = new float[n * n];
      float[] o = new float[n * n];

      for (int y = 0; y < n; y++) {
         for (int x = 0; x < n; x++) {
            float v = 0.0F;

            for (int i = -r; i <= r; i++) {
               int xx = x + i;
               if (xx >= 0 && xx < n) {
                  v += a[y * n + xx] * k[i + r];
               }
            }

            t[y * n + x] = v;
         }
      }

      for (int var17 = 0; var17 < n; var17++) {
         for (int x = 0; x < n; x++) {
            float v = 0.0F;

            for (int ix = -r; ix <= r; ix++) {
               int yy = var17 + ix;
               if (yy >= 0 && yy < n) {
                  v += t[yy * n + x] * k[ix + r];
               }
            }

            o[var17 * n + x] = v;
         }
      }

      return o;
   }

   static Shape detailShape(HudIcons.Icon icon) {
      if (icon == HudIcons.Icon.FOOD_DRUMSTICK) {
         Area bone = new Area(bone());
         bone.subtract(new Area(meat()));
         return bone;
      } else if (icon == HudIcons.Icon.ARMOR_CHEST) {
         Area ridge = new Area(round(0.5F).createStrokedShape(new java.awt.geom.Line2D.Double(4.5, 2.6, 4.5, 7.7)));
         ridge.intersect(new Area(shape(icon)));
         return ridge;
      } else {
         return null;
      }
   }

   static Shape spotShape(HudIcons.Icon icon) {
      Shape s = shape(icon);
      Area spot = new Area(spot(icon, s));
      spot.intersect(new Area(s));
      return spot;
   }

   static enum Icon {
      HEART,
      HEART_SQUARE,
      HEART_HEX,
      HEART_CIRCLE,
      FOOD_DRUMSTICK,
      FOOD_BOLT,
      FOOD_CIRCLE,
      ARMOR_SHIELD,
      ARMOR_CHEST,
      BUBBLE,
      BUBBLE_POP;
   }

   static enum Layer {
      FILL,
      GLOSS,
      RIM,
      GLOW,
      DETAIL;
   }
}
