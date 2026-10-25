package meowtils.furiahvisuals.gui;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.asset.ShaderFiles;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.ColorUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

final class GlassRenderer {
   private static final String PASS_VERT = "gui/glass_pass.vsh";
   private static final String DOWN_FRAG = "gui/glass_down.fsh";
   private static final String BLUR_FRAG = "gui/glass_blur.fsh";
   private static final String SHAPE_VERT = "gui/glass_shape.vsh";
   private static final String WINDOW_FRAG = "gui/glass_window.fsh";
   private static final String GLASS_FRAG = "gui/glass_glass.fsh";
   private static final int CHECKED_FRAMES = 3;
   private static final IntBuffer INTS = BufferUtils.createIntBuffer(16);
   private static final FloatBuffer FLOATS = BufferUtils.createFloatBuffer(16);
   private static boolean compiled;
   private static boolean disabled;
   private static boolean ready;
   private static int checkFrames;
   private static int filesGeneration = -1;
   private static boolean missingFiles;
   private static int downProgram;
   private static int blurProgram;
   private static int downTexel;
   private static int blurDir;
   private static GlassRenderer.ShapeProgram window;
   private static GlassRenderer.ShapeProgram glass;
   private static int uSharpScale;
   private static int uBevel;
   private static int uRefract;
   private static int uHair;
   private static int uTint;
   private static int uRim;
   private static int uLight;
   private static Framebuffer copy;
   private static Framebuffer half;
   private static Framebuffer pong;
   private static int viewX;
   private static int viewY;
   private static int viewW;
   private static int viewH;
   private static float lightX = -0.55F;
   private static float lightY = -0.83F;

   private GlassRenderer() {
   }

   static boolean isReady() {
      return ready;
   }

   static void skip() {
      ready = false;
   }

   static void setLight(float x, float y) {
      float len = (float)Math.sqrt(x * x + y * y);
      if (len > 1.0E-4F) {
         lightX = x / len;
         lightY = y / len;
      }
   }

   static void capture(float px) {
      ready = false;
      if (!disabled && OpenGlHelper.isFramebufferEnabled()) {
         ContextCapabilities caps = GLContext.getCapabilities();
         if (caps.OpenGL20) {
            if (!compiled || filesGeneration != AssetSource.generation()) {
               compile();
               if (disabled) {
                  return;
               }
            }

            if (!missingFiles) {
               ((Buffer)INTS).clear();
               GL11.glGetInteger(2978, INTS);
               viewX = INTS.get(0);
               viewY = INTS.get(1);
               viewW = INTS.get(2);
               viewH = INTS.get(3);
               if (viewW >= 16 && viewH >= 16) {
                  boolean gl30 = caps.OpenGL30;
                  int drawFb = GL11.glGetInteger(36006);
                  int readFb = gl30 ? GL11.glGetInteger(36010) : drawFb;
                  int program = GL11.glGetInteger(35725);
                  int activeUnit = GL11.glGetInteger(34016);
                  boolean scissor = GL11.glIsEnabled(3089);
                  boolean check = checkFrames > 0 || copy == null || copy.framebufferWidth != viewW || copy.framebufferHeight != viewH;
                  if (check) {
                     int i = 0;

                     while (i < 16 && GL11.glGetError() != 0) {
                        i++;
                     }
                  }

                  boolean ok = false;

                  try {
                     if (scissor) {
                        GL11.glDisable(3089);
                     }

                     GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
                     GlStateManager.disableBlend();
                     ensureTargets(viewW, viewH);
                     if (gl30) {
                        GL30.glBindFramebuffer(36008, readFb);
                        GL30.glBindFramebuffer(36009, copy.framebufferObject);
                        GL30.glBlitFramebuffer(viewX, viewY, viewX + viewW, viewY + viewH, 0, 0, viewW, viewH, 16384, 9728);
                     } else {
                        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, drawFb);
                        GlStateManager.bindTexture(copy.framebufferTexture);
                        GL11.glCopyTexSubImage2D(3553, 0, 0, 0, viewX, viewY, viewW, viewH);
                     }

                     float spread = Math.max(0.6F, px / 1.9F);
                     GL20.glUseProgram(downProgram);
                     GL20.glUniform2f(downTexel, 1.0F / copy.framebufferTextureWidth, 1.0F / copy.framebufferTextureHeight);
                     pass(half, copy);
                     GL20.glUseProgram(blurProgram);
                     blur(spread);
                     blur(spread * 2.0F);
                     ok = true;
                  } catch (RuntimeException var15) {
                     disable("glass setup failed", var15);
                  } finally {
                     bindFramebuffers(drawFb, readFb, gl30);
                     GlStateManager.viewport(viewX, viewY, viewW, viewH);
                     GL20.glUseProgram(program);
                     GlStateManager.setActiveTexture(activeUnit);
                     if (scissor) {
                        GL11.glEnable(3089);
                     }

                     GlStateManager.enableBlend();
                     GlStateManager.disableDepth();
                  }

                  if (ok) {
                     if (check) {
                        int error = GL11.glGetError();
                        if (error != 0) {
                           disable("OpenGL error " + error + " while capturing the screen", null);
                           return;
                        }

                        checkFrames--;
                     }

                     ready = true;
                  }
               }
            }
         }
      }
   }

   private static void blur(float spread) {
      GL20.glUniform2f(blurDir, spread / half.framebufferTextureWidth, 0.0F);
      pass(pong, half);
      GL20.glUniform2f(blurDir, 0.0F, spread / pong.framebufferTextureHeight);
      pass(half, pong);
   }

   static boolean drawGlass(float x, float y, float w, float h, float radius, int tint, int rim, float bevel, float refract, float alpha) {
      if (ready && !(w <= 0.0F) && !(h <= 0.0F) && !(alpha <= 0.0F)) {
         float px = Ui.px;
         float r = Math.max(0.5F, Math.min(radius, Math.min(w, h) / 2.0F));
         int activeUnit = GL11.glGetInteger(34016);
         GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
         int unit1 = GL11.glGetInteger(32873);
         GlStateManager.bindTexture(copy.framebufferTexture);
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         glass.use(half, x, y, w, h, r, alpha);
         GL20.glUniform2f(uSharpScale, uScale(copy), vScale(copy));
         GL20.glUniform1f(uBevel, Math.max(1.0F, Math.min(bevel, r) * px));
         GL20.glUniform1f(uRefract, refract * px);
         GL20.glUniform1f(uHair, Math.max(1.0F, 0.85F * px));
         GL20.glUniform4f(uTint, ColorUtil.red(tint) / 255.0F, ColorUtil.green(tint) / 255.0F, ColorUtil.blue(tint) / 255.0F, ColorUtil.alpha(tint) / 255.0F);
         GL20.glUniform4f(uRim, ColorUtil.red(rim) / 255.0F, ColorUtil.green(rim) / 255.0F, ColorUtil.blue(rim) / 255.0F, ColorUtil.alpha(rim) / 255.0F);
         GL20.glUniform2f(uLight, lightX, lightY);
         quad(x, y, w, h, px);
         GL20.glUseProgram(Ui.outerProgram());
         GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
         GlStateManager.bindTexture(unit1);
         GlStateManager.setActiveTexture(activeUnit);
         return true;
      } else {
         return false;
      }
   }

   static boolean drawWindow(float x, float y, float w, float h, float radius, float alpha) {
      if (ready && !(w <= 0.0F) && !(h <= 0.0F) && !(alpha <= 0.0F)) {
         int activeUnit = GL11.glGetInteger(34016);
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         window.use(copy, x, y, w, h, Math.min(radius, Math.min(w, h) / 2.0F), alpha);
         quad(x, y, w, h, Ui.px);
         GL20.glUseProgram(Ui.outerProgram());
         GlStateManager.setActiveTexture(activeUnit);
         return true;
      } else {
         return false;
      }
   }

   private static void quad(float x, float y, float w, float h, float px) {
      float m = 1.5F / px;
      float cx = x + w / 2.0F;
      float cy = y + h / 2.0F;
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_TEX);
      vertex(wr, x - m, y + h + m, cx, cy, px);
      vertex(wr, x + w + m, y + h + m, cx, cy, px);
      vertex(wr, x + w + m, y - m, cx, cy, px);
      vertex(wr, x - m, y - m, cx, cy, px);
      tess.draw();
   }

   private static void vertex(WorldRenderer wr, float x, float y, float cx, float cy, float px) {
      wr.pos(x, y, 0.0).tex((x - cx) * px, (y - cy) * px).endVertex();
   }

   private static void pass(Framebuffer target, Framebuffer source) {
      target.bindFramebuffer(true);
      GlStateManager.bindTexture(source.framebufferTexture);
      float u = uScale(source);
      float v = vScale(source);
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_TEX);
      wr.pos(0.0, 0.0, 0.0).tex(0.0, 0.0).endVertex();
      wr.pos(1.0, 0.0, 0.0).tex(u, 0.0).endVertex();
      wr.pos(1.0, 1.0, 0.0).tex(u, v).endVertex();
      wr.pos(0.0, 1.0, 0.0).tex(0.0, v).endVertex();
      tess.draw();
   }

   private static float uScale(Framebuffer fb) {
      return (float)fb.framebufferWidth / fb.framebufferTextureWidth;
   }

   private static float vScale(Framebuffer fb) {
      return (float)fb.framebufferHeight / fb.framebufferTextureHeight;
   }

   private static void bindFramebuffers(int drawFb, int readFb, boolean gl30) {
      if (gl30 && readFb != drawFb) {
         GL30.glBindFramebuffer(36008, readFb);
         GL30.glBindFramebuffer(36009, drawFb);
      } else {
         OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, drawFb);
      }
   }

   private static void ensureTargets(int w, int h) {
      if (copy == null || copy.framebufferWidth != w || copy.framebufferHeight != h) {
         deleteTargets();
         ((Buffer)FLOATS).clear();
         GL11.glGetFloat(3106, FLOATS);

         try {
            copy = target(w, h);
            half = target(Math.max(1, w / 2), Math.max(1, h / 2));
            pong = target(Math.max(1, w / 2), Math.max(1, h / 2));
         } finally {
            GlStateManager.clearColor(FLOATS.get(0), FLOATS.get(1), FLOATS.get(2), FLOATS.get(3));
            GlStateManager.disableDepth();
         }

         checkFrames = 3;
      }
   }

   private static Framebuffer target(int w, int h) {
      Framebuffer fb = new Framebuffer(w, h, false);
      fb.setFramebufferFilter(9729);
      GlStateManager.bindTexture(fb.framebufferTexture);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      return fb;
   }

   private static void deleteTargets() {
      for (Framebuffer fb : new Framebuffer[]{copy, half, pong}) {
         if (fb != null) {
            fb.deleteFramebuffer();
         }
      }

      pong = null;
      half = null;
      copy = null;
   }

   static void release() {
      ready = false;
      if (copy != null || half != null || pong != null) {
         boolean gl30 = GLContext.getCapabilities().OpenGL30;
         int drawFb = GL11.glGetInteger(36006);
         int readFb = gl30 ? GL11.glGetInteger(36010) : drawFb;

         try {
            deleteTargets();
         } finally {
            bindFramebuffers(drawFb, readFb, gl30);
         }
      }
   }

   private static void compile() {
      if (compiled) {
         for (int p : new int[]{downProgram, blurProgram, window == null ? 0 : window.id, glass == null ? 0 : glass.id}) {
            if (p > 0) {
               GL20.glDeleteProgram(p);
            }
         }

         blurProgram = 0;
         downProgram = 0;
         glass = null;
         window = null;
         disabled = false;
      }

      compiled = true;
      filesGeneration = AssetSource.generation();
      missingFiles = ShaderFiles.text("gui/glass_pass.vsh") == null || ShaderFiles.text("gui/glass_glass.fsh") == null;
      if (!missingFiles) {
         int previousProgram = GL11.glGetInteger(35725);

         try {
            downProgram = link("gui/glass_pass.vsh", "gui/glass_down.fsh");
            blurProgram = link("gui/glass_pass.vsh", "gui/glass_blur.fsh");
            int windowProgram = link("gui/glass_shape.vsh", "gui/glass_window.fsh");
            int glassProgram = link("gui/glass_shape.vsh", "gui/glass_glass.fsh");
            if (downProgram != 0 && blurProgram != 0 && windowProgram != 0 && glassProgram != 0) {
               downTexel = GL20.glGetUniformLocation(downProgram, "uTexel");
               blurDir = GL20.glGetUniformLocation(blurProgram, "uDir");
               window = new GlassRenderer.ShapeProgram(windowProgram);
               glass = new GlassRenderer.ShapeProgram(glassProgram);
               uSharpScale = GL20.glGetUniformLocation(glassProgram, "uSharpScale");
               uBevel = GL20.glGetUniformLocation(glassProgram, "uBevel");
               uRefract = GL20.glGetUniformLocation(glassProgram, "uRefract");
               uHair = GL20.glGetUniformLocation(glassProgram, "uHair");
               uTint = GL20.glGetUniformLocation(glassProgram, "uTint");
               uRim = GL20.glGetUniformLocation(glassProgram, "uRim");
               uLight = GL20.glGetUniformLocation(glassProgram, "uLight");
               GL20.glUseProgram(downProgram);
               GL20.glUniform1i(GL20.glGetUniformLocation(downProgram, "uTex"), 0);
               GL20.glUseProgram(blurProgram);
               GL20.glUniform1i(GL20.glGetUniformLocation(blurProgram, "uTex"), 0);
               GL20.glUseProgram(windowProgram);
               GL20.glUniform1i(GL20.glGetUniformLocation(windowProgram, "uBg"), 0);
               GL20.glUseProgram(glassProgram);
               GL20.glUniform1i(GL20.glGetUniformLocation(glassProgram, "uBg"), 0);
               GL20.glUniform1i(GL20.glGetUniformLocation(glassProgram, "uSharp"), 1);
               return;
            }

            disable("glass shaders did not compile", null);
         } catch (RuntimeException var7) {
            disable("glass shaders failed", var7);
            return;
         } finally {
            GL20.glUseProgram(previousProgram);
         }
      }
   }

   private static int link(String vertex, String fragment) {
      int vs = shader(35633, vertex);
      int fs = shader(35632, fragment);
      if (vs != 0 && fs != 0) {
         int p = GL20.glCreateProgram();
         GL20.glAttachShader(p, vs);
         GL20.glAttachShader(p, fs);
         GL20.glLinkProgram(p);
         if (GL20.glGetProgrami(p, 35714) == 0) {
            FuriahsVisuals.LOG.warn("Glass shader failed to link: {}", new Object[]{GL20.glGetProgramInfoLog(p, 2048)});
            return 0;
         } else {
            return p;
         }
      } else {
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
            FuriahsVisuals.LOG.warn("Glass shader failed to compile: {}", new Object[]{GL20.glGetShaderInfoLog(s, 2048)});
            return 0;
         } else {
            return s;
         }
      }
   }

   private static void disable(String reason, Throwable error) {
      if (!disabled) {
         disabled = true;
         if (error != null) {
            FuriahsVisuals.LOG.warn("Liquid glass disabled ({}); glass themes use plain translucent panels", new Object[]{reason, error});
         } else {
            FuriahsVisuals.LOG.warn("Liquid glass disabled ({}); glass themes use plain translucent panels", new Object[]{reason});
         }
      }

      ready = false;
   }

   private static final class ShapeProgram {
      final int id;
      final int viewport;
      final int uvScale;
      final int alpha;
      final int halfSize;
      final int radius;

      ShapeProgram(int id) {
         this.id = id;
         this.viewport = GL20.glGetUniformLocation(id, "uViewport");
         this.uvScale = GL20.glGetUniformLocation(id, "uUvScale");
         this.alpha = GL20.glGetUniformLocation(id, "uAlpha");
         this.halfSize = GL20.glGetUniformLocation(id, "uHalf");
         this.radius = GL20.glGetUniformLocation(id, "uRadius");
      }

      void use(Framebuffer source, float x, float y, float w, float h, float r, float a) {
         float px = Ui.px;
         GL20.glUseProgram(this.id);
         GL20.glUniform4f(this.viewport, GlassRenderer.viewX, GlassRenderer.viewY, GlassRenderer.viewW, GlassRenderer.viewH);
         GL20.glUniform2f(this.uvScale, GlassRenderer.uScale(source), GlassRenderer.vScale(source));
         GL20.glUniform1f(this.alpha, a);
         GL20.glUniform2f(this.halfSize, w * px / 2.0F, h * px / 2.0F);
         GL20.glUniform1f(this.radius, r * px);
         GlStateManager.bindTexture(source.framebufferTexture);
      }
   }
}
