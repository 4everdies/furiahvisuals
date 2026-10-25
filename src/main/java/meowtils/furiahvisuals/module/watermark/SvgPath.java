package meowtils.furiahvisuals.module.watermark;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Path2D.Double;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import meowtils.furiahvisuals.asset.ExtensionResources;

final class SvgPath {
   private static final Pattern VIEW_BOX = Pattern.compile("viewBox\\s*=\\s*\"([^\"]+)\"");
   private static final Pattern D = Pattern.compile("\\sd\\s*=\\s*\"([^\"]+)\"");
   private static final Pattern NUMBER = Pattern.compile("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?");
   final Double path;
   final double[] viewBox;

   private SvgPath(Double path, double[] viewBox) {
      this.path = path;
      this.viewBox = viewBox;
   }

   static SvgPath resource(String name) throws IOException {
      InputStream in = ExtensionResources.open(name);
      if (in == null) {
         throw new IOException("missing " + name);
      } else {
         SvgPath var6;
         try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];

            int r;
            while ((r = in.read(buf)) > 0) {
               out.write(buf, 0, r);
            }

            SvgPath svgPath = of(new String(out.toByteArray(), StandardCharsets.UTF_8));
            var6 = svgPath;
         } finally {
            in.close();
         }

         return var6;
      }
   }

   static SvgPath of(String svg) throws IOException {
      Matcher d = D.matcher(svg);
      if (!d.find()) {
         throw new IOException("no path");
      } else {
         double[] box = null;
         Matcher v = VIEW_BOX.matcher(svg);
         double[] n;
         if (v.find() && (n = numbers(v.group(1))).length == 4) {
            box = n;
         }

         Double p = parse(d.group(1));
         if (box == null) {
            Rectangle2D b = p.getBounds2D();
            box = new double[]{b.getX(), b.getY(), b.getWidth(), b.getHeight()};
         }

         return new SvgPath(p, box);
      }
   }

   private static double[] numbers(String s) {
      Matcher m = NUMBER.matcher(s);
      double[] out = new double[16];

      int n;
      for (n = 0; m.find(); out[n++] = java.lang.Double.parseDouble(m.group())) {
         if (n == out.length) {
            out = Arrays.copyOf(out, n * 2);
         }
      }

      return Arrays.copyOf(out, n);
   }

   static Double parse(String data) throws IOException {
      Double p = new Double(1);
      int i = 0;
      int len = data.length();
      int cmd = 0;
      double x = 0.0;
      double y = 0.0;
      double startX = 0.0;
      double startY = 0.0;
      double lastCx = 0.0;
      double lastCy = 0.0;
      int lastCmd = 0;
      Matcher m = NUMBER.matcher(data);
      int[] at = new int[1];

      while (true) {
         while (i >= len || !Character.isWhitespace(data.charAt(i)) && data.charAt(i) != ',') {
            if (i >= len) {
               return p;
            }

            int c = data.charAt(i);
            if (Character.isLetter((char)c) && c != 101 && c != 69) {
               cmd = c;
               i++;
               if (c == 90 || c == 122) {
                  p.closePath();
                  x = startX;
                  y = startY;
                  lastCmd = c;
                  continue;
               }
            } else if (cmd == 0) {
               throw new IOException("path data starts without a command");
            }

            boolean rel;
            double ox = (rel = Character.isLowerCase((char)cmd)) ? x : 0.0;
            double oy = rel ? y : 0.0;
            switch (Character.toUpperCase((char)cmd)) {
               case 'C': {
                  double[] a = read(m, data, i, at, 6);
                  i = at[0];
                  p.curveTo(ox + a[0], oy + a[1], ox + a[2], oy + a[3], ox + a[4], oy + a[5]);
                  lastCx = ox + a[2];
                  lastCy = oy + a[3];
                  x = ox + a[4];
                  y = oy + a[5];
                  break;
               }
               case 'D':
               case 'E':
               case 'F':
               case 'G':
               case 'I':
               case 'J':
               case 'K':
               case 'N':
               case 'O':
               case 'P':
               case 'R':
               case 'U':
               default:
                  throw new IOException("unsupported path command " + (char)cmd);
               case 'H': {
                  double[] a = read(m, data, i, at, 1);
                  i = at[0];
                  x = ox + a[0];
                  p.lineTo(x, y);
                  break;
               }
               case 'L': {
                  double[] a = read(m, data, i, at, 2);
                  i = at[0];
                  x = ox + a[0];
                  y = oy + a[1];
                  p.lineTo(x, y);
                  break;
               }
               case 'M': {
                  double[] a = read(m, data, i, at, 2);
                  i = at[0];
                  x = ox + a[0];
                  y = oy + a[1];
                  p.moveTo(x, y);
                  startX = x;
                  startY = y;
                  cmd = rel ? 108 : 76;
                  break;
               }
               case 'Q': {
                  double[] a = read(m, data, i, at, 4);
                  i = at[0];
                  p.quadTo(ox + a[0], oy + a[1], ox + a[2], oy + a[3]);
                  lastCx = ox + a[0];
                  lastCy = oy + a[1];
                  x = ox + a[2];
                  y = oy + a[3];
                  break;
               }
               case 'S': {
                  double[] a = read(m, data, i, at, 4);
                  i = at[0];
                  boolean smooth = "CcSs".indexOf(lastCmd) >= 0;
                  double c1x = smooth ? 2.0 * x - lastCx : x;
                  double c1y = smooth ? 2.0 * y - lastCy : y;
                  p.curveTo(c1x, c1y, ox + a[0], oy + a[1], ox + a[2], oy + a[3]);
                  lastCx = ox + a[0];
                  lastCy = oy + a[1];
                  x = ox + a[2];
                  y = oy + a[3];
                  break;
               }
               case 'T': {
                  double[] a = read(m, data, i, at, 2);
                  i = at[0];
                  boolean smooth = "QqTt".indexOf(lastCmd) >= 0;
                  double cx = smooth ? 2.0 * x - lastCx : x;
                  double cy = smooth ? 2.0 * y - lastCy : y;
                  p.quadTo(cx, cy, ox + a[0], oy + a[1]);
                  lastCx = cx;
                  lastCy = cy;
                  x = ox + a[0];
                  y = oy + a[1];
                  break;
               }
               case 'V': {
                  double[] a = read(m, data, i, at, 1);
                  i = at[0];
                  y = oy + a[0];
                  p.lineTo(x, y);
               }
            }

            lastCmd = cmd;
         }

         i++;
      }
   }

   private static double[] read(Matcher m, String data, int from, int[] endAt, int n) throws IOException {
      double[] out = new double[n];
      int at = from;

      for (int k = 0; k < n; k++) {
         while (at < data.length() && (Character.isWhitespace(data.charAt(at)) || data.charAt(at) == ',')) {
            at++;
         }

         if (!m.find(at) || m.start() != at) {
            throw new IOException("expected a number at " + at);
         }

         out[k] = java.lang.Double.parseDouble(m.group());
         at = m.end();
      }

      endAt[0] = at;
      return out;
   }
}
