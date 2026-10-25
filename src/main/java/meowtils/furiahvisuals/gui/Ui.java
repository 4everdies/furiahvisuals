package meowtils.furiahvisuals.gui;

import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.asset.ShaderFiles;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.hud.HudGl;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Textures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

public final class Ui {
   public static float px = 2.0F;
   public static float offsetY;
   public static SmoothFont font;
   public static SmoothFont bold;
   public static SmoothFont heavy;
   private static float alpha = 1.0F;
   private static final float[] ALPHA_STACK = new float[32];
   private static int alphaDepth;
   private static final float[] SCISSOR = new float[64];
   private static int scissorDepth;
   private static int program = -1;
   private static int uHalf;
   private static int uRadius;
   private static int uBorder;
   private static int uSoft;
   private static int uChecker;
   private static int uFade;
   private static int imageProgram = -1;
   private static int uImageHalf;
   private static int uImageRadius;
   private static int uImageCenter;
   private static int uImagePx;
   private static int uImageFade;
   private static int glintProgram = -1;
   private static int uGlintHalf;
   private static int uGlintRadius;
   private static int uGlintAxis;
   private static int uGlintBand;
   private static int uGlintFade;
   private static int texProgram = -1;
   private static int uTexFade;
   private static int fadeXProgram = -1;
   private static int uFadeX;
   private static int fadeXOuter;
   private static boolean fadeXOn;
   private static final float[] FADE_OFF = new float[]{-2000000.0F, -1000000.0F, 1000000.0F, 2000000.0F};
   private static final float[] FADE = (float[])FADE_OFF.clone();
   private static boolean fadeOn;
   private static int fadeVersion;
   private static int shapeFadeSent = -1;
   private static int imageFadeSent = -1;
   private static int glintFadeSent = -1;
   private static int texFadeSent = -1;
   private static Boolean shadersAvailable;
   private static int outerProgram;
   private static boolean outerScissor;
   private static final IntBuffer OUTER_SCISSOR_BOX = BufferUtils.createIntBuffer(16);
   private static final String VERTEX = "gui/ui.vsh";
   private static final String FRAGMENT = "gui/ui_shape.fsh";
   private static final String IMAGE_VERTEX = "gui/ui_image.vsh";
   private static final String IMAGE_FRAGMENT = "gui/ui_image.fsh";
   private static final String TEX_FRAGMENT = "gui/ui_tex.fsh";
   private static final String FADE_X_FRAGMENT = "gui/ui_fade_x.fsh";
   private static final String GLINT_FRAGMENT = "gui/ui_glint.fsh";
   private static final String ARC_FRAGMENT = "gui/ui_arc.fsh";
   private static int arcProgram = -1;
   private static int uArcArc;
   private static int uArcAngles;
   private static int uArcRound;
   private static int uArcFade;
   private static int uArcPremul;
   private static int arcFadeSent = -1;
   private static int uPremul;
   private static boolean premul;
   private static float menuPx = -1.0F;
   private static float menuTextPx = -1.0F;
   private static final float[][] seenSizes = new float[3][64];
   private static final int[] seenCount = new int[3];
   private static int filesGeneration = AssetSource.generation();
   private static final int[] SHADE_STACK = new int[8];
   private static int shadeDepth;
   private static boolean quickFrame;
   private static boolean imageHeld;
   private static boolean imageBound;

   public static void premultiplied(boolean on) {
      premul = on;
   }

   private Ui() {
   }

   public static void arc(float cx, float cy, float radius, float thickness, float startDeg, float sweepDeg, boolean roundEnds, int color) {
      int c = fade(color);
      if (c >>> 24 != 0 && !(thickness <= 0.0F) && !(radius <= 0.0F) && !(sweepDeg <= 0.0F)) {
         if (arcProgram < 0 && (arcProgram = compileWith("gui/ui_arc.fsh", "Arc")) > 0) {
            uArcArc = GL20.glGetUniformLocation(arcProgram, "uArc");
            uArcAngles = GL20.glGetUniformLocation(arcProgram, "uAngles");
            uArcRound = GL20.glGetUniformLocation(arcProgram, "uRound");
            uArcFade = GL20.glGetUniformLocation(arcProgram, "uFade");
            uArcPremul = GL20.glGetUniformLocation(arcProgram, "uPremul");
            arcFadeSent = -1;
         }

         if (arcProgram > 0) {
            GL20.glUseProgram(arcProgram);
            arcFadeSent = sendFade(uArcFade, arcFadeSent);
            GL20.glUniform1f(uArcPremul, premul ? 1.0F : 0.0F);
            GL20.glUniform2f(uArcArc, radius * px, thickness * px / 2.0F);
            float start = (float)Math.toRadians((startDeg % 360.0F + 360.0F) % 360.0F);
            GL20.glUniform2f(uArcAngles, start, (float)Math.toRadians(Math.min(sweepDeg, 360.0F)));
            GL20.glUniform1f(uArcRound, roundEnds ? 1.0F : 0.0F);
            float m = radius + thickness / 2.0F + 1.5F / px;
            WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            quad(wr, cx, cy, cx - m, cy - m, cx + m, cy + m, c, c, c, c);
            Tessellator.getInstance().draw();
            GL20.glUseProgram(outerProgram);
         }
      }
   }

   public static void line(float x0, float y0, float x1, float y1, float thickness, boolean roundEnds, int color) {
      float dx = x1 - x0;
      float dy = y1 - y0;
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      if (!(len <= 0.0F) && !(thickness <= 0.0F)) {
         float cx = (x0 + x1) / 2.0F;
         float cy = (y0 + y1) / 2.0F;
         float w = roundEnds ? len + thickness : len;
         float deg = (float)Math.toDegrees(Math.atan2(dy, dx));
         GlStateManager.pushMatrix();
         GlStateManager.translate(cx, cy, 0.0F);
         if (deg != 0.0F) {
            GlStateManager.rotate(deg, 0.0F, 0.0F, 1.0F);
         }

         roundRect(-w / 2.0F, -thickness / 2.0F, w, thickness, roundEnds ? thickness / 2.0F : 0.0F, color);
         GlStateManager.popMatrix();
      }
   }

   private static int compileWith(String fragment, String what) {
      try {
         if (!GLContext.getCapabilities().OpenGL20) {
            return 0;
         } else {
            int vs = shader(35633, "gui/ui.vsh");
            int fs = shader(35632, fragment);
            if (vs != 0 && fs != 0) {
               int p = GL20.glCreateProgram();
               GL20.glAttachShader(p, vs);
               GL20.glAttachShader(p, fs);
               GL20.glLinkProgram(p);
               if (GL20.glGetProgrami(p, 35714) == 0) {
                  FuriahsVisuals.LOG.warn("{} shader failed to link: {}", new Object[]{what, GL20.glGetProgramInfoLog(p, 2048)});
                  return 0;
               } else {
                  return p;
               }
            } else {
               return 0;
            }
         }
      } catch (RuntimeException var5) {
         FuriahsVisuals.LOG.warn("{} shader unavailable", new Object[]{what, var5});
         return 0;
      }
   }

   public static void ensureFonts() {
      if (font == null) {
         SmoothFont[] fonts = SmoothFont.createDefaults();
         font = fonts[0];
         bold = fonts[1];
         heavy = fonts[2];
      }
   }

   static void setScale(float pixelsPerUnit, float textPixelsPerUnit) {
      px = pixelsPerUnit;
      menuPx = pixelsPerUnit;
      if (!(menuTextPx < 0.0F) && font != null) {
         if (!(Math.abs(textPixelsPerUnit - menuTextPx) <= 0.001F)) {
            SmoothFont[] fonts = new SmoothFont[]{font, bold, heavy};
            boolean built = false;
            boolean ready = true;

            for (int f = 0; f < 3; f++) {
               for (int i = 0; i < seenCount[f]; i++) {
                  int p = SmoothFont.pixelSize(seenSizes[f][i], textPixelsPerUnit);
                  if (!fonts[f].has(p)) {
                     if (!built) {
                        fonts[f].warm(p);
                        built = true;
                     } else {
                        ready = false;
                     }
                  }
               }
            }

            if (ready) {
               float old = menuTextPx;
               menuTextPx = textPixelsPerUnit;

               for (int f = 0; f < 3; f++) {
                  HashSet<Integer> keep = new HashSet<>();

                  for (int ix = 0; ix < seenCount[f]; ix++) {
                     keep.add(SmoothFont.pixelSize(seenSizes[f][ix], textPixelsPerUnit));
                     keep.add(SmoothFont.pixelSize(seenSizes[f][ix], old));
                  }

                  fonts[f].retain(keep);
               }
            }
         }
      } else {
         menuTextPx = textPixelsPerUnit;
      }
   }

   static void endMenuScale() {
      menuPx = -1.0F;
      menuTextPx = -1.0F;
      seenCount[2] = 0;
      seenCount[1] = 0;
      seenCount[0] = 0;
   }

   private static float textPx() {
      return px == menuPx ? menuTextPx : px;
   }

   private static void noteSize(SmoothFont f, float size) {
      if (px == menuPx) {
         int fi = f == font ? 0 : (f == bold ? 1 : 2);
         float[] arr = seenSizes[fi];
         int n = seenCount[fi];

         for (int i = 0; i < n; i++) {
            if (arr[i] == size) {
               return;
            }
         }

         if (n < arr.length) {
            arr[n] = size;
            seenCount[fi] = n + 1;
         }
      }
   }

   public static void pushAlpha(float a) {
      ALPHA_STACK[alphaDepth++] = alpha;
      alpha = alpha * Math.max(0.0F, Math.min(1.0F, a));
   }

   public static void popAlpha() {
      alpha = ALPHA_STACK[--alphaDepth];
   }

   public static void pushAlphaAbsolute(float a) {
      ALPHA_STACK[alphaDepth++] = alpha;
      alpha = Math.max(0.0F, Math.min(1.0F, a));
   }

   public static float alpha() {
      return alpha;
   }

   static void resetStacks() {
      alpha = 1.0F;
      alphaDepth = 0;
      scissorDepth = 0;
      if (fadeOn) {
         popFadeV();
      }
   }

   public static void pushFadeV(float top, float fadeTop, float bottom, float fadeBottom) {
      int displayHeight = Minecraft.getMinecraft().displayHeight;
      float t = displayHeight - (top + offsetY) * px;
      float b;
      FADE[0] = b = displayHeight - (bottom + offsetY) * px;
      FADE[1] = Math.max(b + 0.01F, b + Math.max(0.0F, fadeBottom) * px);
      FADE[3] = t;
      FADE[2] = Math.min(t - 0.01F, t - Math.max(0.0F, fadeTop) * px);
      fadeOn = true;
      fadeVersion++;
   }

   public static void popFadeV() {
      System.arraycopy(FADE_OFF, 0, FADE, 0, 4);
      fadeOn = false;
      fadeVersion++;
   }

   private static int sendFade(int location, int sent) {
      if (sent != fadeVersion) {
         GL20.glUniform4f(location, FADE[0], FADE[1], FADE[2], FADE[3]);
      }

      return fadeVersion;
   }

   private static boolean beginTextured() {
      if (!fadeOn) {
         return false;
      } else {
         if (texProgram < 0) {
            texProgram = compileTextured();
         }

         if (texProgram == 0) {
            return false;
         } else {
            GL20.glUseProgram(texProgram);
            texFadeSent = sendFade(uTexFade, texFadeSent);
            return true;
         }
      }
   }

   private static void endTextured(boolean used) {
      if (used) {
         GL20.glUseProgram(outerProgram);
      }
   }

   private static void checkFiles() {
      int g = AssetSource.generation();
      if (g != filesGeneration) {
         filesGeneration = g;

         for (int p : new int[]{program, imageProgram, glintProgram, texProgram, fadeXProgram, arcProgram}) {
            if (p > 0) {
               GL20.glDeleteProgram(p);
            }
         }

         arcProgram = -1;
         fadeXProgram = -1;
         texProgram = -1;
         glintProgram = -1;
         imageProgram = -1;
         program = -1;
         arcFadeSent = -1;
         texFadeSent = -1;
         glintFadeSent = -1;
         imageFadeSent = -1;
         shapeFadeSent = -1;
      }
   }

   public static void beginFrame() {
      resetStacks();
      checkFiles();
      if (shadersAvailable == null) {
         shadersAvailable = GLContext.getCapabilities().OpenGL20;
      }

      outerProgram = shadersAvailable ? GL11.glGetInteger(35725) : 0;
      outerScissor = GL11.glIsEnabled(3089);
      ((Buffer)OUTER_SCISSOR_BOX).clear();
      GL11.glGetInteger(3088, OUTER_SCISSOR_BOX);
      quickFrame = false;
      smoothShading();
   }

   private static void smoothShading() {
      int outer = HudGl.recordedShadeModel();
      if (shadeDepth < SHADE_STACK.length) {
         SHADE_STACK[shadeDepth] = outer;
      }

      shadeDepth++;
      if (outer != 7425) {
         GlStateManager.shadeModel(7425);
      }
   }

   private static void restoreShading() {
      if (shadeDepth > 0) {
         int outer = --shadeDepth < SHADE_STACK.length ? SHADE_STACK[shadeDepth] : 7425;
         if (outer != 7425) {
            GlStateManager.shadeModel(outer);
         }
      }
   }

   public static void beginFrameQuick(int programInUse) {
      resetStacks();
      checkFiles();
      if (shadersAvailable == null) {
         shadersAvailable = GLContext.getCapabilities().OpenGL20;
      }

      outerProgram = shadersAvailable ? programInUse : 0;
      quickFrame = true;
      smoothShading();
   }

   public static void endFrame() {
      resetStacks();
      if (!quickFrame) {
         restoreOuterScissor();
      }

      quickFrame = false;
      if (shadersAvailable != null && shadersAvailable) {
         GL20.glUseProgram(outerProgram);
      }

      restoreShading();
   }

   public static int outerProgram() {
      return outerProgram;
   }

   public static boolean shadersWork() {
      if (program < 0) {
         program = compile();
      }

      return program != 0;
   }

   private static void restoreOuterScissor() {
      if (outerScissor) {
         GL11.glEnable(3089);
         GL11.glScissor(OUTER_SCISSOR_BOX.get(0), OUTER_SCISSOR_BOX.get(1), OUTER_SCISSOR_BOX.get(2), OUTER_SCISSOR_BOX.get(3));
      } else {
         GL11.glDisable(3089);
      }
   }

   private static int fade(int color) {
      return alpha >= 1.0F ? color : ColorUtil.mulAlpha(color, alpha);
   }

   public static void rect(float x, float y, float w, float h, int color) {
      shape(x, y, w, h, 0.0F, 0.0F, 0.0F, 0.0F, color, color, color, color);
   }

   public static void roundRect(float x, float y, float w, float h, float radius, int color) {
      shape(x, y, w, h, radius, 0.0F, 0.0F, 0.0F, color, color, color, color);
   }

   public static void gradientV(float x, float y, float w, float h, float radius, int top, int bottom) {
      shape(x, y, w, h, radius, 0.0F, 0.0F, 0.0F, top, top, bottom, bottom);
   }

   public static void gradientH(float x, float y, float w, float h, float radius, int left, int right) {
      shape(x, y, w, h, radius, 0.0F, 0.0F, 0.0F, left, right, right, left);
   }

   public static void outline(float x, float y, float w, float h, float radius, float thickness, int color) {
      shape(x, y, w, h, radius, thickness, 0.0F, 0.0F, color, color, color, color);
   }

   public static void accentOutline(float x, float y, float w, float h, float radius, float thickness, int color) {
      float x0 = Math.round(x * px) / px;
      float x1 = Math.round((x + w) * px) / px;
      float y0 = Math.round((y + offsetY) * px) / px - offsetY;
      float y1 = Math.round((y + h + offsetY) * px) / px - offsetY;
      float t = Math.max(2.0F, (float)Math.round(thickness * px)) / px;
      outline(x0, y0, x1 - x0, y1 - y0, radius, t, color | 0xFF000000);
   }

   public static void outlineV(float x, float y, float w, float h, float radius, float thickness, int top, int bottom) {
      shape(x, y, w, h, radius, thickness, 0.0F, 0.0F, top, top, bottom, bottom);
   }

   public static void surface(float x, float y, float w, float h, float radius, int fill) {
      roundRect(x, y, w, h, radius, fill);
      if (Theme.GLASS) {
         outlineV(x, y, w, h, radius, 0.8F, ColorUtil.withAlpha(Theme.GLASS_RIM, 0.26F), ColorUtil.withAlpha(Theme.GLASS_RIM, 0.05F));
      }
   }

   public static void shadow(float x, float y, float w, float h, float radius, float blur, int color) {
      shape(x, y, w, h, radius, 0.0F, blur, 0.0F, color, color, color, color);
   }

   public static void circle(float cx, float cy, float radius, int color) {
      roundRect(cx - radius, cy - radius, radius * 2.0F, radius * 2.0F, radius, color);
   }

   public static void checker(float x, float y, float w, float h, float radius, float cell) {
      shape(x, y, w, h, radius, 0.0F, 0.0F, cell, -1, -1, -1, -1);
   }

   public static void hueStrip(float x, float y, float w, float h, float radius, boolean vertical) {
      if (beginShape(x, y, w, h, radius, 0.0F, 0.0F, 0.0F)) {
         WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
         wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         float cx = x + w / 2.0F;
         float cy = y + h / 2.0F;

         for (int i = 0; i < 6; i++) {
            int c0 = fade(ColorUtil.hsv(i / 6.0F, 1.0F, 1.0F));
            int c1 = fade(ColorUtil.hsv((i + 1) / 6.0F, 1.0F, 1.0F));
            if (vertical) {
               float y0 = y + h * i / 6.0F;
               float y1 = y + h * (i + 1) / 6.0F;
               quad(wr, cx, cy, x, y0, x + w, y1, c0, c0, c1, c1);
            } else {
               float x0 = x + w * i / 6.0F;
               float x1 = x + w * (i + 1) / 6.0F;
               quad(wr, cx, cy, x0, y, x1, y + h, c0, c1, c1, c0);
            }
         }

         Tessellator.getInstance().draw();
         endShape();
      }
   }

   public static void svSquare(float x, float y, float w, float h, float radius, float hue) {
      int pure = ColorUtil.hsv(hue, 1.0F, 1.0F);
      gradientH(x, y, w, h, radius, -1, pure);
      gradientV(x, y, w, h, radius, 0, -16777216);
   }

   public static void glow(float cx, float cy, float size, int color) {
      if (fade(color) >>> 24 != 0) {
         int c = fade(color);
         GlStateManager.blendFunc(770, 1);
         GlStateManager.bindTexture(Textures.GLOW);
         boolean masked = beginTextured();
         WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
         wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         float h = size / 2.0F;
         int a = c >>> 24;
         int r = c >> 16 & 0xFF;
         int g = c >> 8 & 0xFF;
         int b = c & 0xFF;
         wr.pos(cx - h, cy + h, 0.0).tex(0.0, 1.0).color(r, g, b, a).endVertex();
         wr.pos(cx + h, cy + h, 0.0).tex(1.0, 1.0).color(r, g, b, a).endVertex();
         wr.pos(cx + h, cy - h, 0.0).tex(1.0, 0.0).color(r, g, b, a).endVertex();
         wr.pos(cx - h, cy - h, 0.0).tex(0.0, 0.0).color(r, g, b, a).endVertex();
         Tessellator.getInstance().draw();
         endTextured(masked);
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
      }
   }

   public static void image(int texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int color) {
      int c = fade(color);
      if (c >>> 24 != 0 && texture > 0) {
         GlStateManager.enableTexture2D();
         GlStateManager.bindTexture(texture);
         boolean masked = beginTextured();
         WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
         wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         int a = c >>> 24;
         int r = c >> 16 & 0xFF;
         int g = c >> 8 & 0xFF;
         int b = c & 0xFF;
         wr.pos(x, y + h, 0.0).tex(u0, v1).color(r, g, b, a).endVertex();
         wr.pos(x + w, y + h, 0.0).tex(u1, v1).color(r, g, b, a).endVertex();
         wr.pos(x + w, y, 0.0).tex(u1, v0).color(r, g, b, a).endVertex();
         wr.pos(x, y, 0.0).tex(u0, v0).color(r, g, b, a).endVertex();
         Tessellator.getInstance().draw();
         endTextured(masked);
      }
   }

   public static void roundedImage(int texture, float x, float y, float w, float h, float radius, float u0, float v0, float u1, float v1, int color) {
      roundedImagePair(texture, x, y, w, h, radius, u0, v0, u1, v1, false, 0.0F, 0.0F, 0.0F, 0.0F, color);
   }

   public static void beginImageBatch() {
      imageHeld = true;
      imageBound = false;
   }

   public static void endImageBatch() {
      if (imageBound) {
         GL20.glUseProgram(outerProgram);
      }

      imageHeld = false;
      imageBound = false;
   }

   public static void roundedImagePair(
      int texture,
      float x,
      float y,
      float w,
      float h,
      float radius,
      float u0,
      float v0,
      float u1,
      float v1,
      boolean second,
      float su0,
      float sv0,
      float su1,
      float sv1,
      int color
   ) {
      int c = fade(color);
      if (c >>> 24 != 0 && texture > 0 && !(w <= 0.0F) && !(h <= 0.0F)) {
         if (imageProgram < 0) {
            imageProgram = compileImage();
         }

         if (imageProgram == 0) {
            image(texture, x, y, w, h, u0, v0, u1, v1, color);
            if (second) {
               image(texture, x, y, w, h, su0, sv0, su1, sv1, color);
            }
         } else {
            if (!imageHeld || !imageBound) {
               GL20.glUseProgram(imageProgram);
               imageBound = imageHeld;
            }

            imageFadeSent = sendFade(uImageFade, imageFadeSent);
            GL20.glUniform2f(uImageCenter, x + w / 2.0F, y + h / 2.0F);
            GL20.glUniform1f(uImagePx, px);
            GL20.glUniform2f(uImageHalf, w * px / 2.0F, h * px / 2.0F);
            GL20.glUniform1f(uImageRadius, Math.min(radius * px, Math.min(w, h) * px / 2.0F));
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(texture);
            WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            int a = c >>> 24;
            int r = c >> 16 & 0xFF;
            int g = c >> 8 & 0xFF;
            int b = c & 0xFF;
            wr.pos(x, y + h, 0.0).tex(u0, v1).color(r, g, b, a).endVertex();
            wr.pos(x + w, y + h, 0.0).tex(u1, v1).color(r, g, b, a).endVertex();
            wr.pos(x + w, y, 0.0).tex(u1, v0).color(r, g, b, a).endVertex();
            wr.pos(x, y, 0.0).tex(u0, v0).color(r, g, b, a).endVertex();
            if (second) {
               wr.pos(x, y + h, 0.0).tex(su0, sv1).color(r, g, b, a).endVertex();
               wr.pos(x + w, y + h, 0.0).tex(su1, sv1).color(r, g, b, a).endVertex();
               wr.pos(x + w, y, 0.0).tex(su1, sv0).color(r, g, b, a).endVertex();
               wr.pos(x, y, 0.0).tex(su0, sv0).color(r, g, b, a).endVertex();
            }

            Tessellator.getInstance().draw();
            if (!imageHeld) {
               GL20.glUseProgram(outerProgram);
            }
         }
      }
   }

   public static void glint(float x, float y, float w, float h, float radius, int color, float band, float halfWidth, float angle, float trail) {
      int c = fade(color);
      if (c >>> 24 != 0 && !(w <= 0.0F) && !(h <= 0.0F) && !(halfWidth <= 0.0F)) {
         if (glintProgram < 0) {
            glintProgram = compileGlint();
         }

         if (glintProgram != 0) {
            GL20.glUseProgram(glintProgram);
            glintFadeSent = sendFade(uGlintFade, glintFadeSent);
            GL20.glUniform2f(uGlintHalf, w * px / 2.0F, h * px / 2.0F);
            GL20.glUniform1f(uGlintRadius, Math.min(radius * px, Math.min(w, h) * px / 2.0F));
            GL20.glUniform2f(uGlintAxis, (float)Math.cos(angle), (float)Math.sin(angle));
            GL20.glUniform3f(uGlintBand, band * px, halfWidth * px, trail);
            float m = 1.5F / px;
            WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            quad(wr, x + w / 2.0F, y + h / 2.0F, x - m, y - m, x + w + m, y + h + m, c, c, c, c);
            Tessellator.getInstance().draw();
            GL20.glUseProgram(outerProgram);
         }
      }
   }

   private static int compileGlint() {
      try {
         if (!GLContext.getCapabilities().OpenGL20) {
            return 0;
         } else {
            int vs = shader(35633, "gui/ui.vsh");
            int fs = shader(35632, "gui/ui_glint.fsh");
            if (vs != 0 && fs != 0) {
               int p = GL20.glCreateProgram();
               GL20.glAttachShader(p, vs);
               GL20.glAttachShader(p, fs);
               GL20.glLinkProgram(p);
               if (GL20.glGetProgrami(p, 35714) == 0) {
                  FuriahsVisuals.LOG.warn("Glint shader failed to link: {}", new Object[]{GL20.glGetProgramInfoLog(p, 2048)});
                  return 0;
               } else {
                  uGlintHalf = GL20.glGetUniformLocation(p, "uHalf");
                  uGlintRadius = GL20.glGetUniformLocation(p, "uRadius");
                  uGlintAxis = GL20.glGetUniformLocation(p, "uAxis");
                  uGlintBand = GL20.glGetUniformLocation(p, "uBand");
                  uGlintFade = GL20.glGetUniformLocation(p, "uFade");
                  glintFadeSent = -1;
                  return p;
               }
            } else {
               return 0;
            }
         }
      } catch (RuntimeException var3) {
         FuriahsVisuals.LOG.warn("Glint shader unavailable, no glint", var3);
         return 0;
      }
   }

   private static int compileImage() {
      try {
         if (!GLContext.getCapabilities().OpenGL20) {
            return 0;
         } else {
            int vs = shader(35633, "gui/ui_image.vsh");
            int fs = shader(35632, "gui/ui_image.fsh");
            if (vs != 0 && fs != 0) {
               int p = GL20.glCreateProgram();
               GL20.glAttachShader(p, vs);
               GL20.glAttachShader(p, fs);
               GL20.glLinkProgram(p);
               if (GL20.glGetProgrami(p, 35714) == 0) {
                  FuriahsVisuals.LOG.warn("Image shader failed to link: {}", new Object[]{GL20.glGetProgramInfoLog(p, 2048)});
                  return 0;
               } else {
                  uImageCenter = GL20.glGetUniformLocation(p, "uCenter");
                  uImagePx = GL20.glGetUniformLocation(p, "uPx");
                  uImageHalf = GL20.glGetUniformLocation(p, "uHalf");
                  uImageRadius = GL20.glGetUniformLocation(p, "uRadius");
                  uImageFade = GL20.glGetUniformLocation(p, "uFade");
                  imageFadeSent = -1;
                  int previous = GL11.glGetInteger(35725);
                  GL20.glUseProgram(p);
                  GL20.glUniform1i(GL20.glGetUniformLocation(p, "uTex"), 0);
                  GL20.glUseProgram(previous);
                  return p;
               }
            } else {
               return 0;
            }
         }
      } catch (RuntimeException var4) {
         FuriahsVisuals.LOG.warn("Image shader unavailable, using square corners", var4);
         return 0;
      }
   }

   public static boolean beginFadeX(float left, float fadeLeft, float right, float fadeRight) {
      if (fadeXProgram < 0) {
         fadeXProgram = compileFadeX();
      }

      if (fadeXProgram == 0) {
         return false;
      } else {
         fadeXOuter = GL11.glGetInteger(35725);
         GL20.glUseProgram(fadeXProgram);
         float a = fadeLeft > 0.0F ? left : -2000000.0F;
         float b = fadeLeft > 0.0F ? left + fadeLeft : -1000000.0F;
         float c = fadeRight > 0.0F ? right - fadeRight : 1000000.0F;
         float d = fadeRight > 0.0F ? right : 2000000.0F;
         GL20.glUniform4f(uFadeX, a, b, c, d);
         fadeXOn = true;
         return true;
      }
   }

   public static void endFadeX() {
      if (fadeXOn) {
         GL20.glUseProgram(fadeXOuter);
         fadeXOn = false;
      }
   }

   private static int compileFadeX() {
      try {
         if (!GLContext.getCapabilities().OpenGL20) {
            return 0;
         } else {
            int vs = shader(35633, "gui/ui.vsh");
            int fs = shader(35632, "gui/ui_fade_x.fsh");
            if (vs != 0 && fs != 0) {
               int p = GL20.glCreateProgram();
               GL20.glAttachShader(p, vs);
               GL20.glAttachShader(p, fs);
               GL20.glLinkProgram(p);
               if (GL20.glGetProgrami(p, 35714) == 0) {
                  FuriahsVisuals.LOG.warn("Side fade shader failed to link: {}", new Object[]{GL20.glGetProgramInfoLog(p, 2048)});
                  return 0;
               } else {
                  uFadeX = GL20.glGetUniformLocation(p, "uFadeX");
                  int previous = GL11.glGetInteger(35725);
                  GL20.glUseProgram(p);
                  GL20.glUniform1i(GL20.glGetUniformLocation(p, "uTex"), 0);
                  GL20.glUseProgram(previous);
                  return p;
               }
            } else {
               return 0;
            }
         }
      } catch (RuntimeException var4) {
         FuriahsVisuals.LOG.warn("Side fade shader unavailable, long text in the chat's typing bar is cut without fading", var4);
         return 0;
      }
   }

   private static int compileTextured() {
      try {
         if (!GLContext.getCapabilities().OpenGL20) {
            return 0;
         } else {
            int vs = shader(35633, "gui/ui.vsh");
            int fs = shader(35632, "gui/ui_tex.fsh");
            if (vs != 0 && fs != 0) {
               int p = GL20.glCreateProgram();
               GL20.glAttachShader(p, vs);
               GL20.glAttachShader(p, fs);
               GL20.glLinkProgram(p);
               if (GL20.glGetProgrami(p, 35714) == 0) {
                  FuriahsVisuals.LOG.warn("Fade shader failed to link: {}", new Object[]{GL20.glGetProgramInfoLog(p, 2048)});
                  return 0;
               } else {
                  uTexFade = GL20.glGetUniformLocation(p, "uFade");
                  texFadeSent = -1;
                  int previous = GL11.glGetInteger(35725);
                  GL20.glUseProgram(p);
                  GL20.glUniform1i(GL20.glGetUniformLocation(p, "uTex"), 0);
                  GL20.glUseProgram(previous);
                  return p;
               }
            } else {
               return 0;
            }
         }
      } catch (RuntimeException var4) {
         FuriahsVisuals.LOG.warn("Fade shader unavailable, lists' text is cut without fading", var4);
         return 0;
      }
   }

   private static void shape(float x, float y, float w, float h, float radius, float border, float soft, float checker, int c00, int c10, int c11, int c01) {
      if (!(w <= 0.0F) && !(h <= 0.0F)) {
         if (!beginShape(x, y, w, h, radius, border, soft, checker)) {
            fallback(x, y, w, h, border, soft, c00, c10, c11, c01);
         } else {
            float m = soft * 2.0F + 1.5F / px;
            WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            quad(wr, x + w / 2.0F, y + h / 2.0F, x - m, y - m, x + w + m, y + h + m, fade(c00), fade(c10), fade(c11), fade(c01));
            Tessellator.getInstance().draw();
            endShape();
         }
      }
   }

   private static void quad(WorldRenderer wr, float cx, float cy, float x0, float y0, float x1, float y1, int c00, int c10, int c11, int c01) {
      vertex(wr, x0, y1, (x0 - cx) * px, (y1 - cy) * px, c01);
      vertex(wr, x1, y1, (x1 - cx) * px, (y1 - cy) * px, c11);
      vertex(wr, x1, y0, (x1 - cx) * px, (y0 - cy) * px, c10);
      vertex(wr, x0, y0, (x0 - cx) * px, (y0 - cy) * px, c00);
   }

   private static void vertex(WorldRenderer wr, float x, float y, float u, float v, int c) {
      wr.pos(x, y, 0.0).tex(u, v).color(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, c >>> 24).endVertex();
   }

   private static boolean beginShape(float x, float y, float w, float h, float radius, float border, float soft, float checker) {
      if (program < 0) {
         program = compile();
      }

      if (program == 0) {
         return false;
      } else {
         GL20.glUseProgram(program);
         shapeFadeSent = sendFade(uFade, shapeFadeSent);
         GL20.glUniform1f(uPremul, premul ? 1.0F : 0.0F);
         GL20.glUniform2f(uHalf, w * px / 2.0F, h * px / 2.0F);
         GL20.glUniform1f(uRadius, Math.min(radius * px, Math.min(w, h) * px / 2.0F));
         GL20.glUniform1f(uBorder, border * px);
         GL20.glUniform1f(uSoft, soft * px);
         GL20.glUniform1f(uChecker, checker * px);
         return true;
      }
   }

   private static void endShape() {
      GL20.glUseProgram(outerProgram);
   }

   private static void fallback(float x, float y, float w, float h, float border, float soft, int c00, int c10, int c11, int c01) {
      if (!(soft > 0.0F)) {
         GlStateManager.disableTexture2D();
         WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
         wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
         if (border > 0.0F) {
            int c = fade(c00);
            plain(wr, x, y, w, border, c);
            plain(wr, x, y + h - border, w, border, c);
            plain(wr, x, y, border, h, c);
            plain(wr, x + w - border, y, border, h, c);
         } else {
            wr.pos(x, y + h, 0.0).color(fade(c01) >> 16 & 0xFF, fade(c01) >> 8 & 0xFF, fade(c01) & 0xFF, fade(c01) >>> 24).endVertex();
            wr.pos(x + w, y + h, 0.0)
               .color(fade(c11) >> 16 & 0xFF, fade(c11) >> 8 & 0xFF, fade(c11) & 0xFF, fade(c11) >>> 24)
               .endVertex();
            wr.pos(x + w, y, 0.0).color(fade(c10) >> 16 & 0xFF, fade(c10) >> 8 & 0xFF, fade(c10) & 0xFF, fade(c10) >>> 24).endVertex();
            wr.pos(x, y, 0.0).color(fade(c00) >> 16 & 0xFF, fade(c00) >> 8 & 0xFF, fade(c00) & 0xFF, fade(c00) >>> 24).endVertex();
         }

         Tessellator.getInstance().draw();
         GlStateManager.enableTexture2D();
      }
   }

   private static void plain(WorldRenderer wr, float x, float y, float w, float h, int c) {
      int r = c >> 16 & 0xFF;
      int g = c >> 8 & 0xFF;
      int b = c & 0xFF;
      int a = c >>> 24;
      wr.pos(x, y + h, 0.0).color(r, g, b, a).endVertex();
      wr.pos(x + w, y + h, 0.0).color(r, g, b, a).endVertex();
      wr.pos(x + w, y, 0.0).color(r, g, b, a).endVertex();
      wr.pos(x, y, 0.0).color(r, g, b, a).endVertex();
   }

   private static int compile() {
      try {
         if (!GLContext.getCapabilities().OpenGL20) {
            FuriahsVisuals.LOG.warn("OpenGL 2.0 not available, GUI uses square corners");
            return 0;
         } else {
            int vs = shader(35633, "gui/ui.vsh");
            int fs = shader(35632, "gui/ui_shape.fsh");
            if (vs != 0 && fs != 0) {
               int p = GL20.glCreateProgram();
               GL20.glAttachShader(p, vs);
               GL20.glAttachShader(p, fs);
               GL20.glLinkProgram(p);
               if (GL20.glGetProgrami(p, 35714) == 0) {
                  FuriahsVisuals.LOG.warn("GUI shader failed to link: {}", new Object[]{GL20.glGetProgramInfoLog(p, 2048)});
                  return 0;
               } else {
                  uHalf = GL20.glGetUniformLocation(p, "uHalf");
                  uRadius = GL20.glGetUniformLocation(p, "uRadius");
                  uBorder = GL20.glGetUniformLocation(p, "uBorder");
                  uSoft = GL20.glGetUniformLocation(p, "uSoft");
                  uChecker = GL20.glGetUniformLocation(p, "uChecker");
                  uFade = GL20.glGetUniformLocation(p, "uFade");
                  uPremul = GL20.glGetUniformLocation(p, "uPremul");
                  shapeFadeSent = -1;
                  return p;
               }
            } else {
               return 0;
            }
         }
      } catch (RuntimeException var3) {
         FuriahsVisuals.LOG.warn("GUI shader unavailable, using square corners", var3);
         return 0;
      }
   }

   private static int shader(int type, String file) {
      String source = ShaderFiles.text(file);
      if (source == null) {
         return 0;
      } else {
         int s = GL20.glCreateShader(type);
         GL20.glShaderSource(s, source);
         GL20.glCompileShader(s);
         if (GL20.glGetShaderi(s, 35713) == 0) {
            FuriahsVisuals.LOG.warn("GUI shader failed to compile: {}", new Object[]{GL20.glGetShaderInfoLog(s, 2048)});
            return 0;
         } else {
            return s;
         }
      }
   }

   public static void item(ItemStack stack, float x, float y, float size) {
      if (stack != null && !(alpha < 0.6F)) {
         GlStateManager.pushMatrix();
         GlStateManager.translate(x, y, 0.0F);
         GlStateManager.scale(size / 16.0F, size / 16.0F, 1.0F);
         GlStateManager.enableDepth();
         GlStateManager.enableRescaleNormal();
         RenderHelper.enableGUIStandardItemLighting();
         Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
         RenderHelper.disableStandardItemLighting();
         GlStateManager.disableRescaleNormal();
         GlStateManager.disableDepth();
         GlStateManager.popMatrix();
         GlStateManager.disableLighting();
         GlStateManager.disableAlpha();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
         GlStateManager.enableTexture2D();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public static void text(String s, float x, float y, float size, int color) {
      draw(font, s, x, y, size, color);
   }

   public static void textBold(String s, float x, float y, float size, int color) {
      draw(bold, s, x, y, size, color);
   }

   private static void draw(SmoothFont f, String s, float x, float y, float size, int color) {
      noteSize(f, size);
      if (UiProbe.recording() && !s.trim().isEmpty()) {
         float lh = f.lineHeight(size, textPx());
         UiProbe.add(x, y + lh * 0.15F, f.width(s.replaceAll("\\s+$", ""), size, textPx()), lh * 0.7F, s, true);
      }

      boolean masked = beginTextured();
      f.draw(s, x, y, size, textPx(), fade(color));
      endTextured(masked);
   }

   public static void registerControl(float x, float y, float w, float h, String label) {
      if (UiProbe.recording()) {
         UiProbe.add(x, y, w, h, label, false);
      }
   }

   public static void registerText(float x, float y, float w, float h, String label) {
      if (UiProbe.recording()) {
         UiProbe.add(x, y, w, h, label, true);
      }
   }

   static float[] currentClip() {
      if (scissorDepth == 0) {
         return null;
      } else {
         int i = (scissorDepth - 1) * 4;
         return new float[]{SCISSOR[i], SCISSOR[i + 1], SCISSOR[i + 2], SCISSOR[i + 3]};
      }
   }

   public static float width(String s, float size) {
      noteSize(font, size);
      return font.width(s, size, textPx());
   }

   public static float widthBold(String s, float size) {
      noteSize(bold, size);
      return bold.width(s, size, textPx());
   }

   public static float widthHeavy(String s, float size) {
      SmoothFont f = heavy != null ? heavy : bold;
      noteSize(f, size);
      return f.width(s, size, textPx());
   }

   public static float lineHeight(float size) {
      noteSize(font, size);
      return font.lineHeight(size, textPx());
   }

   public static void textMiddle(String s, float x, float cy, float size, int color, boolean isBold) {
      SmoothFont f = isBold ? bold : font;
      draw(f, s, x, cy - f.lineHeight(size, textPx()) / 2.0F, size, color);
   }

   public static void textMiddleHeavy(String s, float x, float cy, float size, int color) {
      SmoothFont f = heavy != null ? heavy : bold;
      draw(f, s, x, cy - f.lineHeight(size, textPx()) / 2.0F, size, color);
   }

   public static void textHeavy(String s, float x, float y, float size, int color) {
      draw(heavy != null ? heavy : bold, s, x, y, size, color);
   }

   public static String ellipsizeHeavy(String s, float size, float maxWidth) {
      SmoothFont f = heavy != null ? heavy : bold;
      noteSize(f, size);
      float tp = textPx();
      if (f.width(s, size, tp) <= maxWidth) {
         return s;
      } else {
         for (int n = s.length() - 1; n > 0; n--) {
            String t = s.substring(0, n).trim() + "…";
            if (f.width(t, size, tp) <= maxWidth) {
               return t;
            }
         }

         return "…";
      }
   }

   public static String ellipsize(String s, float size, float maxWidth, boolean isBold) {
      SmoothFont f = isBold ? bold : font;
      noteSize(f, size);
      float tp = textPx();
      if (f.width(s, size, tp) <= maxWidth) {
         return s;
      } else {
         String dots = "…";

         for (int n = s.length() - 1; n > 0; n--) {
            String t = s.substring(0, n).trim() + dots;
            if (f.width(t, size, tp) <= maxWidth) {
               return t;
            }
         }

         return dots;
      }
   }

   public static void pushScissor(float x, float y, float w, float h) {
      if (scissorDepth > 0) {
         int p = (scissorDepth - 1) * 4;
         float x1 = Math.max(x, SCISSOR[p]);
         float y1 = Math.max(y, SCISSOR[p + 1]);
         float x2 = Math.min(x + w, SCISSOR[p] + SCISSOR[p + 2]);
         float y2 = Math.min(y + h, SCISSOR[p + 1] + SCISSOR[p + 3]);
         x = x1;
         y = y1;
         w = Math.max(0.0F, x2 - x1);
         h = Math.max(0.0F, y2 - y1);
      }

      int i = scissorDepth * 4;
      SCISSOR[i] = x;
      SCISSOR[i + 1] = y;
      SCISSOR[i + 2] = w;
      SCISSOR[i + 3] = h;
      scissorDepth++;
      applyScissor();
   }

   public static void popScissor() {
      if (scissorDepth > 0) {
         scissorDepth--;
      }

      applyScissor();
   }

   private static void applyScissor() {
      if (scissorDepth == 0) {
         restoreOuterScissor();
      } else {
         int i = (scissorDepth - 1) * 4;
         int displayHeight = Minecraft.getMinecraft().displayHeight;
         int x0 = (int)Math.floor(SCISSOR[i] * px);
         int y1 = (int)Math.ceil((SCISSOR[i + 1] + SCISSOR[i + 3] + offsetY) * px);
         int x1 = (int)Math.ceil((SCISSOR[i] + SCISSOR[i + 2]) * px);
         int y0 = (int)Math.floor((SCISSOR[i + 1] + offsetY) * px);
         GL11.glEnable(3089);
         GL11.glScissor(x0, displayHeight - y1, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
      }
   }

   public static boolean clippedOut(float x, float y, float w, float h) {
      if (scissorDepth == 0) {
         return false;
      } else {
         int i = (scissorDepth - 1) * 4;
         return x + w <= SCISSOR[i] || x >= SCISSOR[i] + SCISSOR[i + 2] || y + h <= SCISSOR[i + 1] || y >= SCISSOR[i + 1] + SCISSOR[i + 3];
      }
   }

   static int[] currentScissorPixels() {
      if (scissorDepth == 0) {
         return null;
      } else {
         int i = (scissorDepth - 1) * 4;
         int displayHeight = Minecraft.getMinecraft().displayHeight;
         int x0 = (int)Math.floor(SCISSOR[i] * px);
         int y1 = (int)Math.ceil((SCISSOR[i + 1] + SCISSOR[i + 3] + offsetY) * px);
         int x1 = (int)Math.ceil((SCISSOR[i] + SCISSOR[i + 2]) * px);
         int y0 = (int)Math.floor((SCISSOR[i + 1] + offsetY) * px);
         return new int[]{x0, displayHeight - y1, Math.max(0, x1 - x0), Math.max(0, y1 - y0)};
      }
   }
}
