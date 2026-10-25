package meowtils.furiahvisuals.module.weapons;

import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.asset.ShaderFiles;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.module.watermark.IslandGlass;
import meowtils.furiahvisuals.render.ColorUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

final class WheelShapes {
   static final int FILL = 0;
   static final int BORDER = 1;
   static final int GLOW = 2;
   static final int GLASS = 3;
   static final int GLINT = 4;
   static final int FILL_BORDER = 5;
   private static final String VERTEX = "weapons/wheel.vsh";
   private static final String FRAGMENT = "weapons/wheel.fsh";
   private static int program = -1;
   private static int filesGeneration = -1;
   private static int uRing;
   private static int uDir;
   private static int uSinCos;
   private static int uLift;
   private static int uRound;
   private static int uMode;
   private static int uGlint;
   private static int uTrail;
   private static int uBg;
   private static int uViewport;
   private static int uUvScale;
   private static int uUvMax;
   private static int uTint;
   private static int uBorderColor;
   private static final float[] BLUR = new float[9];

   private WheelShapes() {
   }

   static boolean ready() {
      if (program < 0 || filesGeneration != AssetSource.generation()) {
         if (program > 0) {
            GL20.glDeleteProgram(program);
         }

         filesGeneration = AssetSource.generation();
         program = compile();
      }

      return program > 0;
   }

   static void draw(
      float cx, float cy, float r0, float r1, double mid, double half, float gap, float round, float liftX, float liftY, int mode, float extra, int color
   ) {
      int c = Ui.alpha() >= 1.0F ? color : ColorUtil.mulAlpha(color, Ui.alpha());
      if (c >>> 24 != 0 && !(r1 <= 0.0F) && ready()) {
         float px = Ui.px;
         boolean whole = half <= 0.0 || half >= 3.141492653589793;
         GL20.glUseProgram(program);
         GL20.glUniform4f(uRing, Math.max(0.0F, r0) * px, r1 * px, whole ? 0.0F : (float)half, gap / 2.0F * px);
         float dx = (float)Math.sin(mid);
         float dy = (float)(-Math.cos(mid));
         GL20.glUniform2f(uDir, dx, dy);
         GL20.glUniform2f(uSinCos, (float)Math.sin(half), (float)Math.cos(half));
         GL20.glUniform2f(uLift, liftX * px, liftY * px);
         float maxRound = whole ? (r1 - Math.max(0.0F, r0)) / 2.0F : Math.min((r1 - Math.max(0.0F, r0)) / 2.0F, r1 * (float)Math.sin(half));
         GL20.glUniform1f(uRound, Math.max(0.0F, Math.min(round, maxRound - 0.5F)) * px);
         GL20.glUniform4f(uMode, mode, Math.max(0.5F, extra) * px, 0.0F, 0.0F);
         float m = (mode == 2 ? extra : 0.0F) + 2.0F / px;
         float y1;
         float x1;
         float y0;
         float x0;
         if (whole) {
            x0 = -r1;
            y0 = -r1;
            x1 = r1;
            y1 = r1;
         } else {
            x0 = Float.MAX_VALUE;
            y0 = Float.MAX_VALUE;
            x1 = -Float.MAX_VALUE;
            y1 = -Float.MAX_VALUE;

            for (int k = 0; k <= 8; k++) {
               double a = mid - half + 2.0 * half * k / 8.0;
               float sx = (float)Math.sin(a);
               float sy = (float)(-Math.cos(a));

               for (float r : new float[]{Math.max(0.0F, r0), r1}) {
                  x0 = Math.min(x0, sx * r);
                  x1 = Math.max(x1, sx * r);
                  y0 = Math.min(y0, sy * r);
                  y1 = Math.max(y1, sy * r);
               }
            }

            float bulge = r1 * (1.0F - (float)Math.cos(half / 8.0)) + 1.0F;
            x0 -= bulge;
            y0 -= bulge;
            x1 += bulge;
            y1 += bulge;
         }

         WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
         wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         float var37;
         float var43;
         vertex(wr, cx, cy, var43 = x0 + (liftX - m), var37 = y1 + (liftY + m), px, c);
         float var39;
         vertex(wr, cx, cy, var39 = x1 + (liftX + m), var37, px, c);
         float var41;
         vertex(wr, cx, cy, var39, var41 = y0 + (liftY - m), px, c);
         vertex(wr, cx, cy, var43, var41, px, c);
         Tessellator.getInstance().draw();
         GL20.glUseProgram(Ui.outerProgram());
      }
   }

   private static void vertex(WorldRenderer wr, float cx, float cy, float lx, float ly, float px, int c) {
      wr.pos(cx + lx, cy + ly, 0.0).tex(lx * px, ly * px).color(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, c >>> 24).endVertex();
   }

   static void fillBorder(
      float cx, float cy, float r0, float r1, double mid, double half, float gap, float round, float liftX, float liftY, int fill, float width, int border
   ) {
      if (ready()) {
         int b = Ui.alpha() >= 1.0F ? border : ColorUtil.mulAlpha(border, Ui.alpha());
         if (b >>> 24 == 0) {
            draw(cx, cy, r0, r1, mid, half, gap, round, liftX, liftY, 0, 0.0F, fill);
         } else {
            GL20.glUseProgram(program);
            GL20.glUniform4f(uBorderColor, ColorUtil.red(b) / 255.0F, ColorUtil.green(b) / 255.0F, ColorUtil.blue(b) / 255.0F, ColorUtil.alpha(b) / 255.0F);
            int f = fill >>> 24 == 0 ? fill & 16777215 | 16777216 : fill;
            draw(cx, cy, r0, r1, mid, half, gap, round, liftX, liftY, 5, width, f);
         }
      }
   }

   static void glint(
      float cx, float cy, float r0, float r1, double mid, double half, float gap, float round, float band, float halfWidth, float angle, float trail, int color
   ) {
      if (ready() && !(halfWidth <= 0.0F)) {
         GL20.glUseProgram(program);
         GL20.glUniform4f(uGlint, (float)Math.cos(angle), (float)Math.sin(angle), band, halfWidth);
         GL20.glUniform1f(uTrail, trail);
         draw(cx, cy, r0, r1, mid, half, gap, round, 0.0F, 0.0F, 4, 0.0F, color);
      }
   }

   static boolean glass(
      float cx, float cy, float r0, float r1, double mid, double half, float gap, float round, float liftX, float liftY, int tint, float alpha
   ) {
      if (ready() && IslandGlass.blurSource(BLUR)) {
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         GlStateManager.enableTexture2D();
         GL20.glUseProgram(program);
         GlStateManager.bindTexture((int)BLUR[0]);
         GL20.glUniform4f(uViewport, BLUR[1], BLUR[2], BLUR[3], BLUR[4]);
         GL20.glUniform2f(uUvScale, BLUR[5], BLUR[6]);
         GL20.glUniform2f(uUvMax, BLUR[7], BLUR[8]);
         GL20.glUniform4f(uTint, ColorUtil.red(tint) / 255.0F, ColorUtil.green(tint) / 255.0F, ColorUtil.blue(tint) / 255.0F, ColorUtil.alpha(tint) / 255.0F);
         draw(cx, cy, r0, r1, mid, half, gap, round, liftX, liftY, 3, 0.0F, ColorUtil.withAlpha(-1, Math.max(0.0F, Math.min(1.0F, alpha))));
         return true;
      } else {
         return false;
      }
   }

   private static int compile() {
      try {
         if (!GLContext.getCapabilities().OpenGL20) {
            return 0;
         } else {
            String vertex = ShaderFiles.text("weapons/wheel.vsh");
            String fragment = ShaderFiles.text("weapons/wheel.fsh");
            if (vertex != null && fragment != null) {
               int vs = shader(35633, vertex);
               int fs = shader(35632, fragment);
               if (vs != 0 && fs != 0) {
                  int p = GL20.glCreateProgram();
                  GL20.glAttachShader(p, vs);
                  GL20.glAttachShader(p, fs);
                  GL20.glLinkProgram(p);
                  if (GL20.glGetProgrami(p, 35714) == 0) {
                     FuriahsVisuals.LOG.warn("Weapon wheel shader failed to link: {}", new Object[]{GL20.glGetProgramInfoLog(p, 2048)});
                     return 0;
                  } else {
                     uRing = GL20.glGetUniformLocation(p, "uRing");
                     uDir = GL20.glGetUniformLocation(p, "uDir");
                     uSinCos = GL20.glGetUniformLocation(p, "uSinCos");
                     uLift = GL20.glGetUniformLocation(p, "uLift");
                     uRound = GL20.glGetUniformLocation(p, "uRound");
                     uMode = GL20.glGetUniformLocation(p, "uMode");
                     uGlint = GL20.glGetUniformLocation(p, "uGlint");
                     uTrail = GL20.glGetUniformLocation(p, "uTrail");
                     uBg = GL20.glGetUniformLocation(p, "uBg");
                     uViewport = GL20.glGetUniformLocation(p, "uViewport");
                     uUvScale = GL20.glGetUniformLocation(p, "uUvScale");
                     uUvMax = GL20.glGetUniformLocation(p, "uUvMax");
                     uTint = GL20.glGetUniformLocation(p, "uTint");
                     uBorderColor = GL20.glGetUniformLocation(p, "uBorderColor");
                     int previous = GL11.glGetInteger(35725);
                     GL20.glUseProgram(p);
                     GL20.glUniform1i(uBg, 0);
                     GL20.glUseProgram(previous);
                     return p;
                  }
               } else {
                  return 0;
               }
            } else {
               return 0;
            }
         }
      } catch (RuntimeException var6) {
         FuriahsVisuals.LOG.warn("Weapon wheel shader unavailable", var6);
         return 0;
      }
   }

   private static int shader(int type, String source) {
      int s = GL20.glCreateShader(type);
      GL20.glShaderSource(s, source);
      GL20.glCompileShader(s);
      if (GL20.glGetShaderi(s, 35713) == 0) {
         FuriahsVisuals.LOG.warn("Weapon wheel shader failed to compile: {}", new Object[]{GL20.glGetShaderInfoLog(s, 2048)});
         return 0;
      } else {
         return s;
      }
   }
}
