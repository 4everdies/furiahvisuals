package meowtils.furiahvisuals.module.weapons;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import meowtils.furiahvisuals.setting.ColorSetting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class PixelPalette {
   static final int W = 128;
   static final int H = 48;
   static final int ROW_FLAGS = 1;
   static final int ROW_HUES = 2;
   static final int ROW_PURPLE = 12;
   static final int ROW_STAR = 13;
   static final int ROW_COLD = 14;
   static final int ROW_MOON = 15;
   static final int ROW_ALONG = 16;
   static final int ALONG = 32;
   static final int PLAIN = 0;
   static final int BRUSHED = 1;
   static final int DAMASCUS = 2;
   static final int NEBULA = 3;
   static final int PRISM = 4;
   static final int OBSIDIAN = 5;
   static final int VOID = 6;
   static final int HARD_LIGHT = 7;
   static final int ICE = 8;
   static final int INFERNAL = 9;
   static final int BONE = 10;
   static final int DRAGONSCALE = 11;
   static final int MOONSILVER = 12;
   static final int LIVING_WOOD = 13;
   static final int[] STEEL = new int[]{1975088, 4871270, 9082278, 12831702, 15922416};
   static final int[] STEEL_EDGE = new int[]{4871270, 9082278, 12831702, 15133422, 16777215};
   static final int[] BRUSHED_RAMP = new int[]{2304051, 5266026, 9213604, 12371150, 14870246};
   static final int[] BRUSHED_EDGE = new int[]{5266026, 9213604, 12371150, 14541542, 16054006};
   static final int[] DAMASCUS_RAMP = new int[]{1842982, 4080976, 6910591, 10134447, 13226200};
   static final int[] DAMASCUS_EDGE = new int[]{4080976, 6910591, 10134447, 13226200, 15659508};
   static final int[] BLACKENED_RAMP = new int[]{658190, 1382171, 2237739, 3224893, 4541011};
   static final int[] BLACKENED_EDGE = new int[]{4871270, 8160918, 11450308, 14015459, 15791093};
   static final int[] GOLD_RAMP = new int[]{3874566, 8145424, 13143592, 15581779, 16773286};
   static final int[] GOLD_EDGE = new int[]{8145424, 13143592, 15581779, 16770442, 16776160};
   static final int[][] NEBULA_STEPS = new int[][]{
      {854566, 2168920, 3746698, 5851326, 9208036},
      {1313326, 3282538, 5448860, 8013002, 11306220},
      {1837106, 4723310, 7479460, 10372814, 13404910},
      {2623530, 6557792, 10362510, 13518516, 15764186}
   };
   static final int[] NEBULA_STAR = new int[]{10259696, 13154559, 15787263, 16774396, 16777215};
   static final int[] PRISM_HUES = new int[]{15216714, 15764004, 15258682, 3984234, 3065056, 8019184};
   static final int[] OBSIDIAN_RAMP = new int[]{328712, 854802, 1578271, 2498865, 3682375};
   static final int[] OBSIDIAN_EDGE = new int[]{1578271, 3024960, 4865894, 8023200, 12101864};
   static final int[] OBSIDIAN_PURPLE = new int[]{2034234, 3806310, 6038176, 9193174, 12486911};
   static final int[] VOID_RAMP = new int[]{393996, 1050656, 1838646, 2889296, 4333678};
   static final int[] YEW_RAMP = new int[]{3808272, 6961692, 10641964, 13604432, 15781004};
   static final int[] YEW_EDGE = new int[]{6961692, 10641964, 13604432, 15582590, 16772548};
   static final int[] INFERNAL_RAMP = new int[]{3803654, 8001032, 12728846, 15757340, 16756800};
   static final int[] INFERNAL_EDGE = new int[]{12728846, 15757340, 16752688, 16765040, 16773312};
   static final int[] BONE_RAMP = new int[]{5917238, 9206366, 12759182, 14866104, 16183518};
   static final int[] BONE_EDGE = new int[]{9206366, 12759182, 14866104, 16183518, 16776692};
   static final int[] CRIMSON_RAMP = new int[]{2753544, 5506830, 9047576, 12065828, 14696506};
   static final int[] CRIMSON_EDGE = new int[]{9047576, 12852256, 15740970, 16738898, 16756890};
   static final int[] SAPPHIRE_RAMP = new int[]{397358, 925784, 1718926, 3038400, 5932774};
   static final int[] SAPPHIRE_EDGE = new int[]{5932774, 9352434, 12375802, 14478079, 16055039};
   static final int[] DRAGONSCALE_RAMP = new int[]{2885638, 6033424, 9575446, 12858910, 15493684};
   static final int[] MOONSILVER_RAMP = new int[]{2765896, 6057094, 10267328, 13424356, 15922940};
   static final int[] MOONSILVER_EDGE = new int[]{6057094, 10267328, 13424356, 15265528, 16777215};
   static final float MOON_STARS = 0.94F;
   static final int[] LIVING_RAMP = new int[]{1970696, 3810324, 5913122, 8016948, 10121288};
   static final int[] LIVING_MOSS = new int[]{1980942, 3432474, 5147174, 7254068, 10146906};
   static final float LIVING_MOSS_AT = 0.86F;
   static final float LIVING_CRACKS = 0.965F;
   static final int[] CHAIN = new int[]{1447963, 2764082, 4540751, 6843764, 9541280};
   final int[] texels = new int[6144];
   private int texture = -1;
   private final ByteBuffer upload = BufferUtils.createByteBuffer(24576);
   private boolean dirty = true;
   private int builtVersion = Integer.MIN_VALUE;
   private long builtFrame = Long.MIN_VALUE;
   private WeaponOptions.Finish builtFinish;
   boolean animated;
   int pattern;
   int handle;
   boolean edgeGlows;

   void update(WeaponDef def, PixelModel model, WeaponSettings s, WeaponOptions.Finish finish, long frame) {
      int version = s != null ? s.version() : 0;
      if (version != this.builtVersion || finish != this.builtFinish || this.animated && frame != this.builtFrame) {
         this.build(def, model, s, finish, ColorSetting.clock());
         this.builtVersion = version;
         this.builtFinish = finish;
         this.builtFrame = frame;
      }
   }

   void build(WeaponDef def, PixelModel model, WeaponSettings s, WeaponOptions.Finish finish, double time) {
      Arrays.fill(this.texels, 0);
      this.animated = false;
      this.pattern = patternOf(finish);
      this.edgeGlows = finish == WeaponOptions.Finish.VOID || finish == WeaponOptions.Finish.HARD_LIGHT || finish == WeaponOptions.Finish.INFERNAL;
      WeaponOptions.HandleMaterial handleMat = s != null && s.handleMaterial != null
         ? s.handleMaterial.get()
         : WeaponOptions.HandleMaterial.of(def.handleMaterial);
      this.handle = handleMat.code;
      int handleColor = s != null && s.handleColor != null ? s.handleColor.getColor(0.5F, time) : def.handleColor;
      if (handleMat == WeaponOptions.HandleMaterial.METAL) {
         handleColor = desaturate(handleColor, 0.75F);
      }

      int[] metal = PixelPalette.Ramps.around(def.metalColor);
      int[] handleRamp = PixelPalette.Ramps.around(handleColor);
      ColorSetting accent = s != null ? s.accent : null;
      ColorSetting paint = s != null ? s.paint : null;
      this.animated = moving(accent) || finish == WeaponOptions.Finish.PAINTED && moving(paint) || s != null && s.handleColor != null && moving(s.handleColor);

      for (int a = 0; a < 32; a++) {
         float pos = a / 31.0F;
         int row = 16 + a;
         int acc = accent != null ? accent.getColor(pos, time) : def.accentColor;
         int[] accentRamp = PixelPalette.Ramps.around(acc);
         int[][] blade = bladeRamps(finish, pos, paint, time, accentRamp);
         this.put(row, 0, blade[0]);
         this.put(row, 1, blade[1]);
         this.put(row, 2, metal);
         this.put(row, 3, handleRamp);
         this.put(row, 4, accentRamp);
         this.put(row, 5, PixelPalette.Ramps.energy(acc));
         this.put(row, 6, accentRamp);
         this.put(row, 7, CHAIN);

         for (int f = 0; f < 16; f++) {
            PixelModel.Fixed x = model.fixed[f];
            if (x != null) {
               this.put(row, PixelModel.MATERIAL_NAMES.length + f, x.shades != null ? x.shades : PixelPalette.Ramps.around(x.color));
            }
         }
      }

      System.arraycopy(this.texels, 4096, this.texels, 0, 128);

      for (int h = 0; h < 6; h++) {
         int[] r = PixelPalette.Ramps.around(PRISM_HUES[h]);
         this.put(2 + h, 0, r);
         this.put(2 + h, 1, shiftUp(r, PRISM_HUES[(h + 1) % 6]));
      }

      this.put(12, 0, OBSIDIAN_PURPLE);
      this.put(12, 1, OBSIDIAN_PURPLE);
      this.put(13, 0, NEBULA_STAR);
      this.put(13, 1, NEBULA_STAR);
      this.put(14, 0, BLACKENED_RAMP);
      this.put(14, 1, BLACKENED_EDGE);
      int[] moon = PixelPalette.Ramps.energy(accent != null ? accent.getColor(0.5F, time) : def.accentColor);
      this.put(15, 0, moon);
      this.put(15, 1, moon);

      for (int slot = 1; slot < PixelModel.SLOTS && slot < 128; slot++) {
         int mat = PixelModel.material(slot);
         int shade = PixelModel.shade(slot);
         PixelModel.Fixed fx = mat >= PixelModel.MATERIAL_NAMES.length ? model.fixed[mat - PixelModel.MATERIAL_NAMES.length] : null;
         boolean glow = mat == 5
            || fx != null && fx.glow
            || mat == 1 && this.edgeGlows
            || (finish == WeaponOptions.Finish.HARD_LIGHT || finish == WeaponOptions.Finish.INFERNAL) && mat == 0;
         float alpha = fx != null ? fx.alpha : (finish != WeaponOptions.Finish.ICE || mat != 0 && mat != 1 ? 1.0F : 0.62F);
         this.texels[128 + slot] = (glow ? 255 : 0) << 24 | Math.round(alpha * 255.0F) << 16 | mat << 8 | shade;
      }

      this.dirty = true;
   }

   private static int[][] bladeRamps(WeaponOptions.Finish finish, float pos, ColorSetting paint, double time, int[] accentRamp) {
      switch (finish) {
         case STEEL_BRUSHED:
            return new int[][]{BRUSHED_RAMP, BRUSHED_EDGE};
         case DAMASCUS:
            return new int[][]{DAMASCUS_RAMP, DAMASCUS_EDGE};
         case BLACKENED:
            return new int[][]{BLACKENED_RAMP, BLACKENED_EDGE};
         case GOLD:
            return new int[][]{GOLD_RAMP, GOLD_EDGE};
         case NEBULA:
            int[] step = NEBULA_STEPS[Math.min(3, (int)(pos * 4.0F))];
            return new int[][]{step, shiftUp(step, 16771322)};
         case PRISM:
            return new int[][]{PixelPalette.Ramps.around(PRISM_HUES[0]), PixelPalette.Ramps.around(PRISM_HUES[1])};
         case OBSIDIAN:
            return new int[][]{OBSIDIAN_RAMP, OBSIDIAN_EDGE};
         case PAINTED:
            int c = paint != null ? paint.getColor(pos, time) : 16732120;
            int[] r = PixelPalette.Ramps.around(c);
            return new int[][]{r, shiftUp(r, 16777215)};
         case VOID:
            return new int[][]{VOID_RAMP, accentRamp};
         case HARD_LIGHT:
            return new int[][]{PixelPalette.Ramps.energy(accentRamp[2]), PixelPalette.Ramps.energy(accentRamp[2])};
         case ICE:
            return new int[][]{{4090506, 7316680, 11064558, 13954810, 16055551}, {7316680, 11064558, 13954810, 16055551, 16777215}};
         case YEW:
            return new int[][]{YEW_RAMP, YEW_EDGE};
         case INFERNAL:
            return new int[][]{INFERNAL_RAMP, INFERNAL_EDGE};
         case BONE:
            return new int[][]{BONE_RAMP, BONE_EDGE};
         case CRIMSON:
            return new int[][]{CRIMSON_RAMP, CRIMSON_EDGE};
         case SAPPHIRE:
            return new int[][]{SAPPHIRE_RAMP, SAPPHIRE_EDGE};
         case DRAGONSCALE:
            return new int[][]{DRAGONSCALE_RAMP, GOLD_EDGE};
         case MOONSILVER:
            return new int[][]{MOONSILVER_RAMP, MOONSILVER_EDGE};
         case LIVING_WOOD:
            return new int[][]{LIVING_RAMP, LIVING_MOSS};
         default:
            return new int[][]{STEEL, STEEL_EDGE};
      }
   }

   static int patternOf(WeaponOptions.Finish f) {
      switch (f) {
         case STEEL_BRUSHED:
         case YEW:
            return 1;
         case DAMASCUS:
            return 2;
         case BLACKENED:
         case GOLD:
         case PAINTED:
         case CRIMSON:
         case SAPPHIRE:
         default:
            return 0;
         case NEBULA:
            return 3;
         case PRISM:
            return 4;
         case OBSIDIAN:
            return 5;
         case VOID:
            return 6;
         case HARD_LIGHT:
            return 7;
         case ICE:
            return 8;
         case INFERNAL:
            return 9;
         case BONE:
            return 10;
         case DRAGONSCALE:
            return 11;
         case MOONSILVER:
            return 12;
         case LIVING_WOOD:
            return 13;
      }
   }

   private static boolean moving(ColorSetting c) {
      return c != null && c.getEffectiveMode() != ColorSetting.Mode.STATIC;
   }

   private static int[] shiftUp(int[] r, int top) {
      return new int[]{r[1], r[2], r[3], r[4], top & 16777215};
   }

   private void put(int row, int material, int[] ramp) {
      for (int k = 0; k < 5; k++) {
         int slot = PixelModel.slot(material, k);
         if (slot < 128) {
            this.texels[row * 128 + slot] = 0xFF000000 | ramp[k];
         }
      }
   }

   private static int desaturate(int c, float by) {
      int r = c >> 16 & 0xFF;
      int g = c >> 8 & 0xFF;
      int b = c & 0xFF;
      int l = (r * 3 + g * 6 + b) / 10;
      return (int)(r + (l - r) * by) << 16 | (int)(g + (l - g) * by) << 8 | (int)(b + (l - b) * by);
   }

   int texture() {
      if (this.texture < 0) {
         this.texture = TextureUtil.glGenTextures();
         GlStateManager.bindTexture(this.texture);
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glTexParameteri(3553, 10242, 10496);
         GL11.glTexParameteri(3553, 10243, 10496);
         this.fill();
         GL11.glTexImage2D(3553, 0, 32856, 128, 48, 0, 6408, 5121, this.upload);
         this.dirty = false;
      } else if (this.dirty) {
         GlStateManager.bindTexture(this.texture);
         this.fill();
         GL11.glTexSubImage2D(3553, 0, 0, 0, 128, 48, 6408, 5121, this.upload);
         this.dirty = false;
      }

      return this.texture;
   }

   private void fill() {
      ((Buffer)this.upload).clear();

      for (int c : this.texels) {
         this.upload.put((byte)(c >> 16)).put((byte)(c >> 8)).put((byte)c).put((byte)(c >>> 24));
      }

      ((Buffer)this.upload).flip();
   }

   void delete() {
      if (this.texture >= 0) {
         TextureUtil.deleteTexture(this.texture);
         this.texture = -1;
      }

      this.builtVersion = Integer.MIN_VALUE;
   }

   int color(int slot, int px, int py, int pz, int axis, boolean edge, boolean front, float along, float wear, int seed) {
      if (slot > 0 && slot < 128) {
         int flags = this.texels[128 + slot];
         int mat = flags >> 8 & 0xFF;
         int shade;
         int k = shade = flags & 0xFF;
         int row = 16 + Math.max(0, Math.min(31, (int)(along * 32.0F)));
         int u = axis == 2 ? px : (axis == 0 ? pz : px);
         int v = axis == 1 ? pz : py;
         if (mat == 0 || mat == 1) {
            switch (this.pattern) {
               case 1:
                  if (hash(px * 7 + pz, floorDiv(py, 4), 11) > 0.72F) {
                     k++;
                  }
                  break;
               case 2:
                  int wave = py + Math.round(1.6F * (float)Math.sin(px * 0.9 + py * 0.25));
                  if (floorMod(floorDiv(wave, 2), 2) == 1) {
                     k--;
                  }
                  break;
               case 3:
                  if (mat == 0 && front && hash(px, py * 31 + pz, 23) > 0.965F) {
                     row = 13;
                     k = 4;
                  }
                  break;
               case 4:
                  row = 2 + floorMod(floorDiv(u + v, 3), 6);
                  break;
               case 5:
                  if (k >= 3 || hash(px, py * 31 + pz, 29) > 0.93F) {
                     row = 12;
                  }
               case 6:
               case 7:
               case 8:
               default:
                  break;
               case 9:
                  if (floorMod(py + floorDiv(px, 2) + (int)(hash(px, 0, 83) * 3.0F), 7) == 0) {
                     k++;
                  }
                  break;
               case 10:
                  if (mat == 0 && hash(px - py, floorDiv(px + py, 4), 91) > 0.93F) {
                     k--;
                  }
                  break;
               case 11:
                  if (mat == 0) {
                     k += scaleShade(px, py);
                  }
                  break;
               case 12:
                  float h = hash(px, py * 31 + pz, 37);
                  if (mat == 0 && front && h > 0.94F) {
                     row = 15;
                     k = h > 0.975F ? 4 : 3;
                  }
                  break;
               case 13:
                  if (mat == 0) {
                     if (hash(px + pz - py, floorDiv(py, 3), 53) > 0.78F) {
                        k++;
                     }

                     float clump;
                     if ((clump = hash(floorDiv(px, 2) + pz * 3, floorDiv(py, 2), 61)) > 0.86F) {
                        return this.texels[row * 128 + PixelModel.slot(1, clump > 0.93F ? 3 : 2)];
                     }

                     if (front && hash(px, py * 31 + pz, 67) > 0.965F) {
                        row = 15;
                        k = 2;
                     }
                  }
            }
         } else if (mat == 3) {
            k += handleShade(this.handle, u, v);
         }

         if (edge && wear > 0.0F && (mat == 0 || mat == 1 || mat == 2)) {
            float hx = hash(px * 13 + pz, py, 41 + seed);
            if (hx < wear * 0.35F) {
               k++;
            } else if (hx > 1.0F - wear * 0.12F) {
               k = 0;
            }
         }

         k = Math.max(0, Math.min(4, k));
         return this.texels[row * 128 + slot - shade + k];
      } else {
         return 0;
      }
   }

   static int scaleShade(int px, int py) {
      int row = floorDiv(py, 2);
      int lx = floorMod(px + (floorMod(row, 2) == 1 ? 2 : 0), 4);
      if (floorMod(py, 2) != 0) {
         return lx == 0 ? -1 : 0;
      } else {
         return lx != 1 && lx != 2 ? 0 : 1;
      }
   }

   static int handleShade(int material, int u, int v) {
      switch (material) {
         case 1:
            return floorMod(u + v, 3) == 0 ? -1 : 0;
         case 2:
            return hash(u, floorDiv(v, 3), 5) > 0.68F ? -1 : 0;
         case 3:
            return floorMod(v, 2) == 0 ? -1 : 0;
         case 4:
            return floorMod(u + v, 4) != 0 && floorMod(u - v, 4) != 0 ? 1 : -1;
         default:
            return floorMod(u + v, 2) == 0 ? 0 : -1;
      }
   }

   static int floorDiv(int a, int b) {
      return Math.floorDiv(a, b);
   }

   static int floorMod(int a, int b) {
      return Math.floorMod(a, b);
   }

   static float hash(int x, int y, int seed) {
      float px = fract(x * 443.8975F);
      float py = fract(y * 397.2973F);
      float pz = fract(seed * 491.1871F);
      float d = px * (py + 19.19F) + py * (pz + 19.19F) + pz * (px + 19.19F);
      px = fract(fract(px + d) * 443.8975F);
      py = fract(fract(py + d) * 397.2973F);
      pz = fract(fract(pz + d) * 491.1871F);
      d = px * (py + 19.19F) + py * (pz + 19.19F) + pz * (px + 19.19F);
      float var8;
      float var10;
      float var12;
      return fract(((var8 = px + d) + (var10 = py + d)) * (var12 = pz + d));
   }

   private static float fract(float v) {
      return v - (float)Math.floor(v);
   }

   static final class Ramps {
      private static final double COOL = Math.atan2(-0.312, -0.032);
      private static final double WARM = Math.atan2(0.199, -0.071);
      private static final double[] DL = new double[]{-0.22, -0.11, 0.0, 0.1, 0.2};
      private static final double[] CHROMA = new double[]{0.85, 1.05, 1.0, 0.9, 0.62};
      private static final Map<Integer, int[]> CACHE = new HashMap<>();

      static int[] around(int rgb) {
         int var2;
         int[] c = CACHE.get(var2 = rgb & 16777215);
         if (c == null) {
            if (CACHE.size() > 4096) {
               CACHE.clear();
            }

            c = compute(var2);
            CACHE.put(var2, c);
         }

         return c;
      }

      static int[] energy(int rgb) {
         int[] r = (int[])around(rgb).clone();
         r[4] = mix(r[4], 16777215, 0.7F);
         r[3] = mix(r[3], 16777215, 0.35F);
         return r;
      }

      private static int[] compute(int rgb) {
         double[] lab = lab(rgb);
         double l = lab[0];
         double c = Math.hypot(lab[1], lab[2]);
         double h = Math.atan2(lab[2], lab[1]);
         double contrast = 0.5 + l;
         int[] out = new int[5];

         for (int k = 0; k < 5; k++) {
            double lk = Math.max(0.05, Math.min(0.97, l + DL[k] * contrast));
            double shift = k < 2 ? (2 - k) / 2.0 : (k - 2) / 2.0;
            double target = k < 2 ? COOL : WARM;
            double ck;
            double hk;
            if (c < 0.02) {
               hk = target;
               ck = c + 0.012 * shift;
            } else {
               double d2 = Math.atan2(Math.sin(target - h), Math.cos(target - h));
               hk = h + Math.max(-0.26 * shift, Math.min(0.26 * shift, d2));
               ck = c * CHROMA[k];
            }

            out[k] = fromLab(lk, ck * Math.cos(hk), ck * Math.sin(hk));
         }

         out[2] = rgb;
         return out;
      }

      static int mix(int a, int b, float t) {
         int r = (int)((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
         int g = (int)((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
         int bl = (int)((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
         return r << 16 | g << 8 | bl;
      }

      static double[] lab(int rgb) {
         double r = lin((rgb >> 16 & 0xFF) / 255.0);
         double g = lin((rgb >> 8 & 0xFF) / 255.0);
         double b = lin((rgb & 0xFF) / 255.0);
         double l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
         double m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
         double s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
         return new double[]{
            0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s
         };
      }

      static int fromLab(double ll, double a, double bb) {
         double l = ll + 0.3963377774 * a + 0.2158037573 * bb;
         double m = ll - 0.1055613458 * a - 0.0638541728 * bb;
         double s = ll - 0.0894841775 * a - 1.291485548 * bb;
         l = l * l * l;
         m = m * m * m;
         s = s * s * s;
         return ch(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s) << 16
            | ch(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s) << 8
            | ch(-0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s);
      }

      private static double lin(double c) {
         return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
      }

      private static int ch(double c) {
         double var2;
         return (int)Math.round(
            ((var2 = Math.max(0.0, Math.min(1.0, c))) <= 0.0031308 ? var2 * 12.92 : 1.055 * Math.pow(var2, 0.4166666666666667) - 0.055) * 255.0
         );
      }
   }
}
