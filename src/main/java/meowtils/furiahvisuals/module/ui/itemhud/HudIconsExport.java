package meowtils.furiahvisuals.module.ui.itemhud;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import javax.imageio.ImageIO;

public final class HudIconsExport {
   private static final String[] NAMES = new String[]{
      "Heart", "Rounded square", "Hexagon", "Circle", "Drumstick", "Energy bolt", "Food circle", "Shield", "Chestplate", "Bubble", "Popping bubble"
   };
   private static final int[] TINT = new int[]{-50354, -50354, -50354, -50354, -875952, -875952, -875952, -2235414, -2235414, -6301441, -6301441};
   private static final int[] STATE = new int[]{-8535477, -11908526, -10934, -50354, -7617718, -875952, -875952, -2235414, -2235414, -6301441, -6301441};
   private static final String[] STATE_NAME = new String[]{"poison", "wither", "absorption", "half", "hunger effect", "half", "half", "half", "half", "", ""};
   private static final boolean[] HALF = new boolean[]{false, false, false, true, false, true, true, true, true, false, false};
   private static final HudIconsExport.Look DARK = new HudIconsExport.Look(
      "Dark", -15395043, -1642850272, 952749526, 0.45F, 0.28F, 0.42F, false, false, -5656643
   );
   private static final HudIconsExport.Look LIGHT = new HudIconsExport.Look(
      "Light", -1446670, -1191905542, 855638016, 0.5F, 0.25F, 0.22F, false, true, -11906976
   );
   private static final HudIconsExport.Look GLASS = new HudIconsExport.Look(
      "Liquid Glass", -14011568, 1476395007, -1711276033, 0.6F, 0.3F, 0.4F, true, false, -2695958
   );

   private HudIconsExport() {
   }

   public static String export(File dir) throws IOException {
      if (!dir.isDirectory() && !dir.mkdirs()) {
         throw new IOException("could not make " + dir);
      } else {
         File png = new File(dir, "hud-icons.png");
         ImageIO.write(sheet(108), "png", png);
         File svg = new File(dir, "hud-icons.svg");

         try (OutputStreamWriter w = new OutputStreamWriter(Files.newOutputStream(svg.toPath()), StandardCharsets.UTF_8)) {
            w.write(svg());
         }

         return png.getName() + " (" + png.length() / 1024L + " KB), " + svg.getName() + " (" + svg.length() / 1024L + " KB)";
      }
   }

   private static BufferedImage sheet(int px) {
      HudIcons.Icon[] icons = HudIcons.Icon.values();
      int cell = px + 44;
      int left = 170;
      int top = 60;
      HudIconsExport.Look[] looks = new HudIconsExport.Look[]{DARK, LIGHT, GLASS};
      int rows = 2 + looks.length;
      int w = left + cell * icons.length + 20;
      int h = top + cell * rows + 30;
      BufferedImage img = new BufferedImage(w, h, 2);
      Graphics2D g = img.createGraphics();
      hints(g);
      g.setColor(new Color(DARK.background, true));
      g.fillRect(0, 0, w, h);

      for (int i = 0; i < icons.length; i++) {
         g.setColor(new Color(15922424));
         g.setFont(new Font("SansSerif", 1, 13));
         center(g, NAMES[i], left + i * cell + 22 + px / 2, top - 22);
      }

      for (int r = 0; r < rows; r++) {
         int y = top + r * cell;
         HudIconsExport.Look look = r == 0 ? DARK : (r <= looks.length ? looks[r - 1] : DARK);
         if (r >= 1 && r <= looks.length) {
            g.setPaint(
               (Paint)(look == GLASS
                  ? new GradientPaint(0.0F, y, new Color(-10798452, true), w, y + cell, new Color(-14913649, true))
                  : new Color(look.background, true))
            );
            g.fillRect(0, y + 4, w, cell - 8);
         }

         String label = r == 0 ? "Shape" : (r <= looks.length ? look.name : "States");
         g.setColor(new Color(r >= 1 && r <= looks.length ? look.label : -5656643, true));
         g.setFont(new Font("SansSerif", 1, 15));
         g.drawString(label, 20, y + cell / 2 + 5);

         for (int i = 0; i < icons.length; i++) {
            HudIcons.Icon icon = icons[i];
            boolean bubble = icon == HudIcons.Icon.BUBBLE || icon == HudIcons.Icon.BUBBLE_POP;
            int x = left + i * cell + 22;
            int iy = y + 22;
            if (r == 0) {
               put(img, HudIcons.mask(icon, HudIcons.Layer.FILL, px), x, iy, px, -1, -1, 0.0F, 1.0F, false);
            } else {
               boolean states = r > looks.length;
               if (!bubble) {
                  put(img, HudIcons.mask(icon, HudIcons.Layer.FILL, px), x, iy, px, look.slot, look.slot, 0.0F, 1.0F, false);
                  put(img, HudIcons.mask(icon, HudIcons.Layer.RIM, px), x, iy, px, look.slotRim, look.slotRim, 0.0F, 1.0F, false);
               }

               int c = states ? STATE[i] : TINT[i];
               if (look.light && c == -2235414) {
                  c = -7363649;
               }

               boolean half = states && HALF[i];
               boolean food = icon.name().startsWith("FOOD");
               float from = half && food ? 0.5F : 0.0F;
               float to = half && !food ? 0.5F : 1.0F;
               filled(img, icon, x, iy, px, look, c, from, to);
               if (states) {
                  g.setColor(new Color(8423066));
                  g.setFont(new Font("SansSerif", 0, 12));
                  center(g, STATE_NAME[i], x + px / 2, y + cell + 6);
               }
            }
         }
      }

      g.dispose();
      return img;
   }

   private static void filled(BufferedImage img, HudIcons.Icon icon, int x, int y, int px, HudIconsExport.Look look, int color, float from, float to) {
      int c = color | 0xFF000000;
      if (look.glow > 0.0F) {
         put(img, HudIcons.mask(icon, HudIcons.Layer.GLOW, px), x, y, px, alpha(c, look.glow), alpha(c, look.glow), from, to, true);
      }

      put(img, HudIcons.mask(icon, HudIcons.Layer.FILL, px), x, y, px, lerp(c, -1, look.lighten), shade(c, 0.8F), from, to, false);
      if (HudIcons.hasDetail(icon)) {
         boolean bone = icon == HudIcons.Icon.FOOD_DRUMSTICK;
         put(
            img,
            HudIcons.mask(icon, HudIcons.Layer.DETAIL, px),
            x,
            y,
            px,
            bone ? -265242 : alpha(shade(c, 0.7F), 0.8F),
            bone ? -1517116 : alpha(shade(c, 0.62F), 0.8F),
            from,
            to,
            false
         );
      }

      put(img, HudIcons.mask(icon, HudIcons.Layer.GLOSS, px), x, y, px, alpha(-1, look.gloss), alpha(-1, look.gloss), from, to, false);
      int rim = look.glassEdge ? -1275068417 : alpha(lerp(c, -1, 0.65F), 0.75F);
      put(img, HudIcons.mask(icon, HudIcons.Layer.RIM, px), x, y, px, rim, rim, from, to, false);
   }

   private static void hints(Graphics2D g) {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
   }

   private static void center(Graphics2D g, String s, int cx, int y) {
      g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, y);
   }

   private static void put(BufferedImage dst, float[] mask, int x, int y, int px, int top, int bottom, float from, float to, boolean glow) {
      int pad = HudIcons.pad(px);
      int n = px + pad * 2;
      float m = glow ? 1.6F * px / 9.0F : 0.0F;
      float l = from <= 0.0F ? -m : px * from;
      float r = to >= 1.0F ? px + m : px * to;

      for (int j = 0; j < n; j++) {
         float yy = j - pad;
         if (!(yy < -m) && !(yy >= px + m)) {
            int c = lerp(top, bottom, (yy + m) / (px + 2.0F * m));
            float ca = (c >>> 24) / 255.0F;

            for (int i = 0; i < n; i++) {
               float xx = i - pad;
               float a = mask[j * n + i] * ca;
               if (!(xx < l) && !(xx >= r) && !(a <= 0.0F)) {
                  int dx = x + i - pad;
                  int dy = y + j - pad;
                  if (dx >= 0 && dy >= 0 && dx < dst.getWidth() && dy < dst.getHeight()) {
                     int d = dst.getRGB(dx, dy);
                     int rr = Math.round((c >> 16 & 0xFF) * a + (d >> 16 & 0xFF) * (1.0F - a));
                     int gg = Math.round((c >> 8 & 0xFF) * a + (d >> 8 & 0xFF) * (1.0F - a));
                     int bb = Math.round((c & 0xFF) * a + (d & 0xFF) * (1.0F - a));
                     dst.setRGB(dx, dy, 0xFF000000 | rr << 16 | gg << 8 | bb);
                  }
               }
            }
         }
      }
   }

   private static int lerp(int a, int b, float t) {
      int aa = a >>> 24;
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int ba = b >>> 24;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return Math.round(aa + (ba - aa) * t) << 24 | Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
   }

   private static int alpha(int c, float a) {
      return Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, a))) << 24 | c & 16777215;
   }

   private static int shade(int c, float k) {
      int r = Math.round((c >> 16 & 0xFF) * k);
      int g = Math.round((c >> 8 & 0xFF) * k);
      int b = Math.round((c & 0xFF) * k);
      return c & 0xFF000000 | r << 16 | g << 8 | b;
   }

   private static String svg() {
      HudIcons.Icon[] icons = HudIcons.Icon.values();
      int cell = 14;
      int w = cell * icons.length + 2;
      int h = cell * 2 + 2;
      StringBuilder sb = new StringBuilder();
      sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      sb.append("<!-- Furiah's Visuals: the Item HUD's themed icon set. Each shape is on vanilla's 9 x 9 icon grid. Row 1: the shapes;\n");
      sb.append("     row 2: as in game on the Dark theme: the empty slot (the theme's panel color, its edge lit from above), then\n");
      sb.append("     the filled icon (a soft glow, a gradient of its color, the drumstick's bone, a gloss across the top with a\n");
      sb.append("     highlight spot, and a light edge just inside, lit from above). -->\n");
      sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" viewBox=\"0 0 ")
         .append(w)
         .append(' ')
         .append(h)
         .append("\" width=\"")
         .append(w * 40)
         .append("\" height=\"")
         .append(h * 40)
         .append("\">\n");
      sb.append("  <defs>\n");
      sb.append("    <filter id=\"glow\" x=\"-50%\" y=\"-50%\" width=\"200%\" height=\"200%\"><feGaussianBlur stdDeviation=\"0.8\"/></filter>\n");
      sb.append("    <linearGradient id=\"gloss\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"0.55\">")
         .append("<stop offset=\"0\" stop-color=\"#ffffff\" stop-opacity=\"0.59\"/><stop offset=\"1\" stop-color=\"#ffffff\" stop-opacity=\"0\"/>")
         .append("</linearGradient>\n");
      sb.append("    <linearGradient id=\"bone\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">")
         .append("<stop offset=\"0\" stop-color=\"#")
         .append(hex(-265242))
         .append("\"/><stop offset=\"1\" stop-color=\"#")
         .append(hex(-1517116))
         .append("\"/></linearGradient>\n");

      for (int i = 0; i < icons.length; i++) {
         HudIcons.Icon icon = icons[i];
         String id = id(icon);
         Shape s = HudIcons.shape(icon);
         Rectangle2D b = s.getBounds2D();
         String rule = s.getPathIterator(null).getWindingRule() == 0 ? "evenodd" : "nonzero";
         sb.append("    <path id=\"").append(id).append("\" fill-rule=\"").append(rule).append("\" d=\"").append(path(s)).append("\"/>\n");
         sb.append("    <clipPath id=\"clip-").append(id).append("\"><use xlink:href=\"#").append(id).append("\"/></clipPath>\n");
         int c = TINT[i];
         sb.append("    <linearGradient id=\"fill-")
            .append(id)
            .append("\" gradientUnits=\"userSpaceOnUse\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"9\">")
            .append("<stop offset=\"0\" stop-color=\"#")
            .append(hex(lerp(c, -1, DARK.lighten)))
            .append("\"/>")
            .append("<stop offset=\"1\" stop-color=\"#")
            .append(hex(shade(c, 0.8F)))
            .append("\"/></linearGradient>\n");
         sb.append("    <linearGradient id=\"rim-")
            .append(id)
            .append("\" gradientUnits=\"userSpaceOnUse\" x1=\"0\" y1=\"")
            .append(f(b.getY()))
            .append("\" x2=\"0\" y2=\"")
            .append(f(b.getMaxY()))
            .append("\">")
            .append("<stop offset=\"0\" stop-color=\"#")
            .append(hex(lerp(c, -1, 0.65F)))
            .append("\" stop-opacity=\"0.75\"/>")
            .append("<stop offset=\"1\" stop-color=\"#")
            .append(hex(lerp(c, -1, 0.65F)))
            .append("\" stop-opacity=\"0.21\"/></linearGradient>\n");
         sb.append("    <linearGradient id=\"slotrim-")
            .append(id)
            .append("\" gradientUnits=\"userSpaceOnUse\" x1=\"0\" y1=\"")
            .append(f(b.getY()))
            .append("\" x2=\"0\" y2=\"")
            .append(f(b.getMaxY()))
            .append("\">")
            .append("<stop offset=\"0\" stop-color=\"#")
            .append(hex(DARK.slotRim))
            .append("\" stop-opacity=\"")
            .append(f((DARK.slotRim >>> 24) / 255.0))
            .append("\"/><stop offset=\"1\" stop-color=\"#")
            .append(hex(DARK.slotRim))
            .append("\" stop-opacity=\"")
            .append(f((DARK.slotRim >>> 24) / 255.0 * 0.27))
            .append("\"/></linearGradient>\n");
      }

      sb.append("  </defs>\n");
      sb.append("  <rect width=\"").append(w).append("\" height=\"").append(h).append("\" fill=\"#15171d\"/>\n");

      for (int var19 = 0; var19 < icons.length; var19++) {
         HudIcons.Icon icon = icons[var19];
         String id = id(icon);
         boolean bubble = icon == HudIcons.Icon.BUBBLE;
         boolean pop = icon == HudIcons.Icon.BUBBLE_POP;
         Rectangle2D b = HudIcons.shape(icon).getBounds2D();
         double x = 2.5 + var19 * cell;
         sb.append("  <g transform=\"translate(").append(f(x)).append(" 2.5)\"><use xlink:href=\"#").append(id).append("\" fill=\"#ffffff\"/></g>\n");
         sb.append("  <g transform=\"translate(").append(f(x)).append(' ').append(f(2.5 + cell)).append(")\">");
         String fill = "url(#fill-" + id + ")";
         String clip = "url(#clip-" + id + ")";
         String color = "#" + hex(TINT[var19]);
         if (!pop) {
            sb.append("<use xlink:href=\"#")
               .append(id)
               .append("\" fill=\"")
               .append(color)
               .append("\" opacity=\"")
               .append(f(DARK.glow))
               .append("\" filter=\"url(#glow)\"/>");
         }

         if (bubble) {
            sb.append("<use xlink:href=\"#")
               .append(id)
               .append("\" fill=\"")
               .append(fill)
               .append("\" fill-opacity=\"0.31\" stroke=\"")
               .append(fill)
               .append("\" stroke-width=\"0.9\"/><ellipse cx=\"3.5\" cy=\"3.1\" rx=\"0.95\" ry=\"0.65\" fill=\"#ffffff\" fill-opacity=\"0.85\"/>");
         } else if (pop) {
            sb.append("<use xlink:href=\"#").append(id).append("\" fill=\"").append(fill).append("\"/>");
         } else {
            sb.append("<use xlink:href=\"#")
               .append(id)
               .append("\" fill=\"#")
               .append(hex(DARK.slot))
               .append("\" fill-opacity=\"")
               .append(f((DARK.slot >>> 24) / 255.0))
               .append("\"/>");
            sb.append("<use xlink:href=\"#").append(id).append("\" fill=\"").append(fill).append("\"/>");
            Shape detail = HudIcons.detailShape(icon);
            if (detail != null) {
               sb.append("<path d=\"")
                  .append(path(detail))
                  .append("\" fill=\"")
                  .append(icon == HudIcons.Icon.FOOD_DRUMSTICK ? "url(#bone)" : "#" + hex(shade(TINT[var19], 0.66F)) + "\" fill-opacity=\"0.8")
                  .append("\"/>");
            }

            sb.append("<rect x=\"")
               .append(f(b.getX()))
               .append("\" y=\"")
               .append(f(b.getY()))
               .append("\" width=\"")
               .append(f(b.getWidth()))
               .append("\" height=\"")
               .append(f(b.getHeight()))
               .append("\" fill=\"url(#gloss)\" opacity=\"")
               .append(f(DARK.gloss))
               .append("\" clip-path=\"")
               .append(clip)
               .append("\"/>");
            sb.append("<path d=\"").append(path(HudIcons.spotShape(icon))).append("\" fill=\"#ffffff\" fill-opacity=\"").append(f(DARK.gloss)).append("\"/>");
            sb.append("<use xlink:href=\"#")
               .append(id)
               .append("\" fill=\"none\" stroke=\"url(#rim-")
               .append(id)
               .append(")\" stroke-width=\"1\" clip-path=\"")
               .append(clip)
               .append("\"/>");
         }

         sb.append("</g>\n");
      }

      sb.append("</svg>\n");
      return sb.toString();
   }

   private static String id(HudIcons.Icon icon) {
      return icon.name().toLowerCase(Locale.ROOT).replace('_', '-');
   }

   private static String path(Shape s) {
      StringBuilder sb = new StringBuilder();
      double[] c = new double[6];

      for (PathIterator it = s.getPathIterator(new AffineTransform()); !it.isDone(); it.next()) {
         switch (it.currentSegment(c)) {
            case 0:
               sb.append('M').append(f(c[0])).append(' ').append(f(c[1]));
               break;
            case 1:
               sb.append('L').append(f(c[0])).append(' ').append(f(c[1]));
               break;
            case 2:
               sb.append('Q').append(f(c[0])).append(' ').append(f(c[1])).append(' ').append(f(c[2])).append(' ').append(f(c[3]));
               break;
            case 3:
               sb.append('C')
                  .append(f(c[0]))
                  .append(' ')
                  .append(f(c[1]))
                  .append(' ')
                  .append(f(c[2]))
                  .append(' ')
                  .append(f(c[3]))
                  .append(' ')
                  .append(f(c[4]))
                  .append(' ')
                  .append(f(c[5]));
               break;
            default:
               sb.append('Z');
         }
      }

      return sb.toString();
   }

   private static String f(double v) {
      String s = String.format(Locale.ROOT, "%.4f", v);
      String var3;
      return (var3 = s.replaceAll("0+$", "")).endsWith(".") ? var3.substring(0, var3.length() - 1) : var3;
   }

   private static String hex(int rgb) {
      return String.format(Locale.ROOT, "%06x", rgb & 16777215);
   }

   private static final class Look {
      final String name;
      final int background;
      final int slot;
      final int slotRim;
      final int label;
      final float gloss;
      final float lighten;
      final float glow;
      final boolean glassEdge;
      final boolean light;

      Look(String name, int background, int slot, int slotRim, float gloss, float lighten, float glow, boolean glassEdge, boolean light, int label) {
         this.name = name;
         this.background = background;
         this.slot = slot;
         this.slotRim = slotRim;
         this.gloss = gloss;
         this.lighten = lighten;
         this.glow = glow;
         this.glassEdge = glassEdge;
         this.light = light;
         this.label = label;
      }
   }
}
