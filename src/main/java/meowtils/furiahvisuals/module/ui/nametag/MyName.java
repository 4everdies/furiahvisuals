package meowtils.furiahvisuals.module.ui.nametag;

import java.util.Arrays;
import meowtils.furiahvisuals.hud.HudFont;
import meowtils.furiahvisuals.render.ColorUtil;
import net.minecraft.client.gui.FontRenderer;

public final class MyName {
   private MyName() {
   }

   public static String plain(String formatted) {
      StringBuilder sb = new StringBuilder(formatted.length());

      for (int i = 0; i < formatted.length(); i++) {
         char c = formatted.charAt(i);
         if (c == 167 && i + 1 < formatted.length()) {
            i++;
         } else {
            sb.append(c);
         }
      }

      return sb.toString();
   }

   static int[] ownColors(String formatted, int base) {
      int[] out = new int[formatted.length()];
      int n = 0;
      int color = base & 16777215;

      for (int i = 0; i < formatted.length(); i++) {
         char c = formatted.charAt(i);
         if (c == 167 && i + 1 < formatted.length()) {
            int code = "0123456789abcdef".indexOf(Character.toLowerCase(formatted.charAt(i + 1)));
            if (code >= 0) {
               color = HudFont.COLORS[code];
            } else if (Character.toLowerCase(formatted.charAt(i + 1)) == 'r') {
               color = base & 16777215;
            }

            i++;
         } else {
            out[n++] = color;
         }
      }

      return Arrays.copyOf(out, n);
   }

   static int[] tint(NametagElement e, String plain, String name, double time, int[] own) {
      if (name != null && !name.isEmpty()) {
         int at = plain.indexOf(name);
         if (at < 0) {
            return null;
         } else {
            int[] out = new int[plain.length()];
            int n = name.length();

            boolean any;
            for (any = false; at >= 0; at = plain.indexOf(name, at + n)) {
               for (int k = 0; k < n; k++) {
                  float t = n == 1 ? 0.5F : (float)k / (n - 1);
                  int c = e.nameColorAt(t, time);
                  float shine = e.shimmerAt(t, time);
                  if (c == 0) {
                     if (shine <= 0.0F) {
                        continue;
                     }

                     c = own != null && at + k < own.length ? own[at + k] | 0xFF000000 : -1;
                  }

                  if (shine > 0.0F) {
                     c = ColorUtil.lerp(c | 0xFF000000, -1, shine);
                  }

                  out[at + k] = c | 0xFF000000;
                  any = true;
               }
            }

            return any ? out : null;
         }
      } else {
         return null;
      }
   }

   static int[] tintFormatted(NametagElement e, String formatted, String name, double time, int base) {
      String plain = plain(formatted);
      return name != null && plain.indexOf(name) >= 0 ? tint(e, plain, name, time, e.shimmer.get() ? ownColors(formatted, base) : null) : null;
   }

   public static void drawVanilla(FontRenderer fr, String formatted, float x, float y, int color, boolean shadow, int[] tint) {
      if (tint == null) {
         fr.drawString(formatted, x, y, color, shadow);
      } else {
         int alpha = color & 0xFF000000;
         StringBuilder run = new StringBuilder();
         float runX = x;
         char activeColor = 0;
         StringBuilder styles = new StringBuilder();
         int glyph = 0;

         for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (c == 167 && i + 1 < formatted.length()) {
               char code = Character.toLowerCase(formatted.charAt(i + 1));
               if ("0123456789abcdef".indexOf(code) >= 0) {
                  activeColor = code;
                  styles.setLength(0);
               } else if (code == 'r') {
                  activeColor = 0;
                  styles.setLength(0);
               } else if ("klmno".indexOf(code) >= 0) {
                  styles.append('§').append(code);
               }

               run.append(c).append(formatted.charAt(i + 1));
               i++;
            } else {
               if (glyph < tint.length && tint[glyph] != 0) {
                  if (run.length() > 0) {
                     String s = run.toString();
                     fr.drawString(s, runX, y, color, shadow);
                     runX += fr.getStringWidth(s);
                     run.setLength(0);
                  }

                  String s = styles + String.valueOf(c);
                  fr.drawString(s, runX, y, alpha | tint[glyph] & 16777215, shadow);
                  runX += fr.getStringWidth(s);
                  if (activeColor != 0) {
                     run.append('§').append(activeColor);
                  }

                  run.append((CharSequence)styles);
               } else {
                  run.append(c);
               }

               glyph++;
            }
         }

         if (run.length() > 0) {
            fr.drawString(run.toString(), runX, y, color, shadow);
         }
      }
   }
}
