package meowtils.furiahvisuals.module.glow;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;

final class PresetCodec {
   static final String PREFIX = "FVG1-";
   private static final int VERSION = 1;

   private PresetCodec() {
   }

   static int stepOf(Setting s) {
      if (s instanceof SliderSetting) {
         SliderSetting slider = (SliderSetting)s;
         return (int)Math.round((slider.get() - slider.getMin()) / slider.getStep());
      } else if (s instanceof ModeSetting) {
         return ((ModeSetting)s).getIndex();
      } else if (s instanceof BooleanSetting) {
         return ((BooleanSetting)s).get() ? 1 : 0;
      } else {
         throw new IllegalArgumentException("not a preset setting: " + s.getId());
      }
   }

   static int defaultStepOf(Setting s) {
      if (s instanceof SliderSetting) {
         SliderSetting slider = (SliderSetting)s;
         return (int)Math.round((slider.getDefault() - slider.getMin()) / slider.getStep());
      } else if (s instanceof ModeSetting) {
         return ((ModeSetting)s).getDefault().ordinal();
      } else {
         return ((BooleanSetting)s).getDefault() ? 1 : 0;
      }
   }

   static double valueOf(Setting s, int step) {
      if (s instanceof SliderSetting) {
         SliderSetting slider = (SliderSetting)s;
         return Math.max(slider.getMin(), Math.min(slider.getMax(), slider.getMin() + step * slider.getStep()));
      } else if (s instanceof ModeSetting) {
         return Math.max(0, Math.min(((ModeSetting)s).getValues().length - 1, step));
      } else {
         return step != 0 ? 1.0 : 0.0;
      }
   }

   static void apply(Setting s, int step) {
      if (s instanceof SliderSetting) {
         ((SliderSetting)s).set(valueOf(s, step));
      } else if (s instanceof ModeSetting) {
         ((ModeSetting)s).setIndex((int)valueOf(s, step));
      } else if (s instanceof BooleanSetting) {
         ((BooleanSetting)s).set(step != 0);
      }
   }

   static String encode(PresetCodec.Preset p) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      out.write(1);
      out.write(p.steps.length);

      for (int step : p.steps) {
         out.write(Math.max(0, Math.min(255, step)));
      }

      out.write(p.hasColor ? 1 : 0);
      if (p.hasColor) {
         out.write(modeCode(p.mode));
         rgb(out, p.colorA);
         rgb(out, p.colorB);
         out.write(p.direction);
      }

      byte[] name = p.name.getBytes(StandardCharsets.UTF_8);
      int len = Math.min(40, name.length);
      out.write(len);
      out.write(name, 0, len);
      byte[] body = out.toByteArray();
      out.write(checksum(body, body.length));
      return "FVG1-" + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
   }

   static PresetCodec.Preset decode(String text) {
      if (text == null) {
         return null;
      } else {
         String t = text.replaceAll("\\s+", "");
         if (!t.startsWith("FVG1-")) {
            return null;
         } else {
            byte[] b;
            try {
               b = Base64.getUrlDecoder().decode(t.substring("FVG1-".length()));
            } catch (IllegalArgumentException var9) {
               return null;
            }

            try {
               if (b.length >= 5 && (b[b.length - 1] & 255) == checksum(b, b.length - 1) && (b[0] & 255) == 1) {
                  PresetCodec.Preset p = new PresetCodec.Preset();
                  int i = 1;
                  int n = b[i++] & 255;
                  p.steps = new int[n];

                  for (int k = 0; k < n; k++) {
                     p.steps[k] = b[i++] & 255;
                  }

                  if ((b[i++] & 255 & 1) != 0) {
                     p.hasColor = true;
                     p.mode = mode(b[i++] & 255);
                     p.colorA = 0xFF000000 | (b[i] & 255) << 16 | (b[i + 1] & 255) << 8 | b[i + 2] & 255;
                     int var14 = i + 3;
                     p.colorB = 0xFF000000 | (b[var14] & 255) << 16 | (b[var14 + 1] & 255) << 8 | b[var14 + 2] & 255;
                     i = var14 + 3;
                     p.direction = b[i++] & 255;
                  }

                  int len;
                  if (i + (len = b[i++] & 255) != b.length - 1) {
                     return null;
                  } else {
                     p.name = new String(b, i, len, StandardCharsets.UTF_8).trim();
                     return p;
                  }
               } else {
                  return null;
               }
            } catch (ArrayIndexOutOfBoundsException var10) {
               return null;
            }
         }
      }
   }

   private static void rgb(ByteArrayOutputStream out, int c) {
      out.write(c >> 16 & 0xFF);
      out.write(c >> 8 & 0xFF);
      out.write(c & 0xFF);
   }

   private static int checksum(byte[] b, int n) {
      int sum = 0;

      for (int i = 0; i < n; i++) {
         sum += (b[i] & 255) * (i + 1);
      }

      return sum & 0xFF;
   }

   private static int modeCode(ColorSetting.Mode mode) {
      switch (mode) {
         case GRADIENT:
            return 1;
         case CHROMA:
            return 2;
         case GLOBAL:
            return 3;
         default:
            return 0;
      }
   }

   private static ColorSetting.Mode mode(int code) {
      switch (code) {
         case 1:
            return ColorSetting.Mode.GRADIENT;
         case 2:
            return ColorSetting.Mode.CHROMA;
         case 3:
            return ColorSetting.Mode.GLOBAL;
         default:
            return ColorSetting.Mode.STATIC;
      }
   }

   static final class Preset {
      String name = "";
      int[] steps = new int[0];
      boolean hasColor;
      ColorSetting.Mode mode = ColorSetting.Mode.STATIC;
      int colorA = -1;
      int colorB = -1;
      int direction;
   }
}
