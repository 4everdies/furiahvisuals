package meowtils.furiahvisuals.render.look;

import java.util.Random;
import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.asset.ShaderFiles;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.GlSave;
import meowtils.furiahvisuals.render.OptiFineCompat;
import meowtils.furiahvisuals.render.Textures;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL20;

public final class GlassCells {
   private static final String VERT = "look/glasscells.vsh";
   private static final String FRAG = "look/glasscells.fsh";
   private static boolean compiled;
   private static boolean broken;
   private static boolean forceFallback;
   private static int filesGeneration = -1;
   private static int program;
   private static int uSpan;
   private static int uOff;
   private static int uCell;
   private static int uLeadW;
   private static int uLead;
   private static int uPalette;
   private static int uPortrait;
   private static int uLight;
   private static int uLightColor;
   private static int uGone;
   private static int uRange;
   private static int uGlow;
   private static int uDim;
   private static int uOpacity;
   private static final int[] uSet = new int[4];
   private static final int[] uBack = new int[4];
   private static int previous;
   private static int leadTexture;

   private GlassCells() {
   }

   public static boolean available() {
      if (!forceFallback && !OptiFineCompat.shadersActive()) {
         if (!compiled || filesGeneration != AssetSource.generation()) {
            compile();
         }

         return program != 0 && !broken;
      } else {
         return false;
      }
   }

   public static void begin(
      float spanU,
      float spanV,
      float offU,
      float offV,
      float cell,
      float leadW,
      int lead,
      int[] set,
      int[] back,
      int palette,
      boolean portrait,
      float light,
      int lightColor,
      float gone,
      float bottom,
      float height,
      float glow,
      float dim,
      float opacity
   ) {
      previous = GlSave.boundProgram();
      GL20.glUseProgram(program);
      GL20.glUniform2f(uSpan, spanU, spanV);
      GL20.glUniform2f(uOff, offU, offV);
      GL20.glUniform1f(uCell, Math.max(0.01F, cell));
      GL20.glUniform1f(uLeadW, leadW);
      rgb(uLead, lead);

      for (int i = 0; i < 4; i++) {
         rgb(uSet[i], set[i]);
         rgb(uBack[i], back[i]);
      }

      GL20.glUniform1f(uPalette, palette);
      GL20.glUniform1f(uPortrait, portrait ? 1.0F : 0.0F);
      GL20.glUniform1f(uLight, light);
      rgb(uLightColor, lightColor);
      GL20.glUniform1f(uGone, gone);
      GL20.glUniform2f(uRange, bottom, height);
      GL20.glUniform1f(uGlow, glow);
      GL20.glUniform1f(uDim, dim);
      GL20.glUniform1f(uOpacity, opacity);
   }

   private static void rgb(int location, int color) {
      GL20.glUniform3f(location, (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
   }

   public static void end() {
      GL20.glUseProgram(previous);
   }

   private static void compile() {
      compiled = true;
      filesGeneration = AssetSource.generation();
      if (program != 0) {
         GL20.glDeleteProgram(program);
         program = 0;
      }

      broken = false;
      if (!OpenGlHelper.shadersSupported) {
         broken = true;
      } else {
         String vertex = ShaderFiles.text("look/glasscells.vsh");
         String fragment = ShaderFiles.text("look/glasscells.fsh");
         if (vertex != null && fragment != null) {
            int before = GlSave.boundProgram();

            try {
               int vs = shader(35633, vertex);
               int fs = shader(35632, fragment);
               if (vs == 0 || fs == 0) {
                  fail("the shader did not compile");
                  return;
               }

               int p = GL20.glCreateProgram();
               GL20.glAttachShader(p, vs);
               GL20.glAttachShader(p, fs);
               GL20.glLinkProgram(p);
               if (GL20.glGetProgrami(p, 35714) != 0) {
                  program = p;
                  uSpan = GL20.glGetUniformLocation(p, "uSpan");
                  uOff = GL20.glGetUniformLocation(p, "uOff");
                  uCell = GL20.glGetUniformLocation(p, "uCell");
                  uLeadW = GL20.glGetUniformLocation(p, "uLeadW");
                  uLead = GL20.glGetUniformLocation(p, "uLead");

                  for (int i = 0; i < 4; i++) {
                     uSet[i] = GL20.glGetUniformLocation(p, "uSet" + i);
                     uBack[i] = GL20.glGetUniformLocation(p, "uBack" + i);
                  }

                  uPalette = GL20.glGetUniformLocation(p, "uPalette");
                  uPortrait = GL20.glGetUniformLocation(p, "uPortrait");
                  uLight = GL20.glGetUniformLocation(p, "uLight");
                  uLightColor = GL20.glGetUniformLocation(p, "uLightColor");
                  uGone = GL20.glGetUniformLocation(p, "uGone");
                  uRange = GL20.glGetUniformLocation(p, "uRange");
                  uGlow = GL20.glGetUniformLocation(p, "uGlow");
                  uDim = GL20.glGetUniformLocation(p, "uDim");
                  uOpacity = GL20.glGetUniformLocation(p, "uOpacity");
                  GL20.glUseProgram(p);
                  GL20.glUniform1i(GL20.glGetUniformLocation(p, "uTex"), 0);
                  return;
               }

               FuriahsVisuals.LOG.warn("Glass cells shader failed to link: {}", new Object[]{GL20.glGetProgramInfoLog(p, 2048)});
               fail("the shader did not link");
            } catch (RuntimeException | LinkageError var10) {
               FuriahsVisuals.LOG.warn("Glass cells shader setup failed", var10);
               fail("shader setup failed");
               return;
            } finally {
               GL20.glUseProgram(before);
            }
         }
      }
   }

   private static int shader(int type, String source) {
      int s = GL20.glCreateShader(type);
      GL20.glShaderSource(s, source);
      GL20.glCompileShader(s);
      if (GL20.glGetShaderi(s, 35713) == 0) {
         FuriahsVisuals.LOG.warn("Glass cells shader failed to compile: {}", new Object[]{GL20.glGetShaderInfoLog(s, 2048)});
         return 0;
      } else {
         return s;
      }
   }

   private static void fail(String reason) {
      if (!broken) {
         broken = true;
         program = 0;
         FuriahsVisuals.LOG.warn("Glass cells shader switched off ({}); windows show their figure under a lead pattern instead", new Object[]{reason});
      }
   }

   public static int leadTexture() {
      if (leadTexture == 0) {
         int size = 128;
         int cells = 4;
         float cell = (float)size / cells;
         Random r = new Random(370631L);
         float[] jx = new float[cells * cells];
         float[] jy = new float[cells * cells];

         for (int k = 0; k < jx.length; k++) {
            jx[k] = 0.15F + 0.7F * r.nextFloat();
            jy[k] = 0.15F + 0.7F * r.nextFloat();
         }

         int[] px = new int[size * size];

         for (int y = 0; y < size; y++) {
            int cj = (int)(y / cell);

            for (int x = 0; x < size; x++) {
               int ci = (int)(x / cell);
               float d1 = Float.MAX_VALUE;
               float d2 = Float.MAX_VALUE;

               for (int dj = -1; dj <= 1; dj++) {
                  for (int di = -1; di <= 1; di++) {
                     int k = (cj + dj + cells) % cells * cells + (ci + di + cells) % cells;
                     float dx = x + 0.5F - (ci + di + jx[k]) * cell;
                     float dy = y + 0.5F - (cj + dj + jy[k]) * cell;
                     float d = (float)Math.sqrt(dx * dx + dy * dy);
                     if (d < d1) {
                        d2 = d1;
                        d1 = d;
                     } else if (d < d2) {
                        d2 = d;
                     }
                  }
               }

               float edge = (d2 - d1) * 0.5F;
               float a = Math.max(0.0F, Math.min(1.0F, (2.6F - edge) / 1.2F));
               px[y * size + x] = Math.round(a * 255.0F) << 24 | 16777215;
            }
         }

         leadTexture = Textures.uploadImage(px, size, size, true, true);
      }

      return leadTexture;
   }

   public static String stateForTest() {
      if (forceFallback) {
         return "fallback (asked)";
      } else if (OptiFineCompat.shadersActive()) {
         return "fallback (shader pack)";
      } else {
         if (!compiled || filesGeneration != AssetSource.generation()) {
            compile();
         }

         return program != 0 && !broken ? "shader" : "fallback (no shader)";
      }
   }

   public static void forceFallbackForTest(boolean on) {
      forceFallback = on;
   }
}
