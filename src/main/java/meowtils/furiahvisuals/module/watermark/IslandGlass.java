package meowtils.furiahvisuals.module.watermark;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.asset.ShaderFiles;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import net.minecraft.client.Minecraft;
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

public final class IslandGlass {
   private static final String PASS_VERT = "gui/island_pass.vsh";
   private static final String DOWN_FRAG = "gui/island_down.fsh";
   private static final String BLUR_FRAG = "gui/island_blur.fsh";
   private static final String SHAPE_VERT = "gui/island_shape.vsh";
   private static final String GLASS_FRAG = "gui/island_glass.fsh";
   private static final int MARGIN = 16;
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
   private static int glassProgram;
   private static int uDownTexel;
   private static int uDownMax;
   private static int uBlurDir;
   private static int uBlurMax;
   private static int uViewport;
   private static int uUvScale;
   private static int uUvMax;
   private static int uHalf;
   private static int uRadius;
   private static int uTint;
   private static int uRim;
   private static int uAlpha;
   private static int uBevel;
   private static int uRefract;
   private static int uHair;
   private static Framebuffer copy;
   private static Framebuffer half;
   private static Framebuffer pong;
   private static int capW;
   private static int capH;
   private static int regionX;
   private static int regionY;
   private static int regionW;
   private static int regionH;
   private static int halfW;
   private static int halfH;
   private static int lastLeft = -1;
   private static int lastTop = -1;
   private static long capturedNanos;
   private static long screenFrame = -1L;

   private IslandGlass() {
   }

   public static boolean captureScreen() {
      Minecraft mc = Minecraft.getMinecraft();
      boolean ok = captureRegion(0, 0, mc.displayWidth, mc.displayHeight, mc.displayHeight / 1080.0F);
      screenFrame = ok ? FxFrame.currentFrame() : -1L;
      return ok;
   }

   public static boolean hasScreen() {
      return ready && screenFrame == FxFrame.currentFrame();
   }

   public static boolean blurSource(float[] out) {
      if (hasScreen() && !disabled && half != null) {
         out[0] = half.framebufferTexture;
         out[1] = regionX;
         out[2] = regionY;
         out[3] = regionW;
         out[4] = regionH;
         out[5] = (float)halfW / half.framebufferTextureWidth;
         out[6] = (float)halfH / half.framebufferTextureHeight;
         out[7] = (halfW - 0.5F) / half.framebufferTextureWidth;
         out[8] = (halfH - 0.5F) / half.framebufferTextureHeight;
         return true;
      } else {
         return false;
      }
   }

   static boolean capture(float x, float y, float w, float h) {
      if (hasScreen() && Minecraft.getMinecraft().currentScreen == null) {
         return true;
      } else {
         screenFrame = -1L;
         float px = Ui.px;
         int left = (int)Math.floor(x * px) - 16;
         int right = (int)Math.ceil((x + w) * px) + 16;
         int top = (int)Math.floor((y + Ui.offsetY) * px) - 16;
         int bottom = (int)Math.ceil((y + h + Ui.offsetY) * px) + 16;
         return captureRegion(left, top, right, bottom, px);
      }
   }

   private static boolean captureRegion(int left, int top, int right, int bottom, float px) {
      if (!disabled && OpenGlHelper.isFramebufferEnabled()) {
         Minecraft mc = Minecraft.getMinecraft();
         left = Math.max(0, left);
         top = Math.max(0, top);
         right = Math.min(mc.displayWidth, right);
         bottom = Math.min(mc.displayHeight, bottom);
         int rw = right - left;
         int rh = bottom - top;
         if (rw >= 8 && rh >= 8) {
            long now = System.nanoTime();
            if (ready && left == lastLeft && top == lastTop && rw == regionW && rh == regionH && now - capturedNanos < 16000000L) {
               return true;
            } else {
               ready = false;
               ContextCapabilities caps = GLContext.getCapabilities();
               if (!caps.OpenGL20) {
                  return false;
               } else {
                  if (!compiled || filesGeneration != AssetSource.generation()) {
                     compile();
                     if (disabled) {
                        return false;
                     }
                  }

                  if (missingFiles) {
                     return false;
                  } else {
                     ((Buffer)INTS).clear();
                     GL11.glGetInteger(2978, INTS);
                     int viewX = INTS.get(0);
                     int viewY = INTS.get(1);
                     int viewW = INTS.get(2);
                     int viewH = INTS.get(3);
                     boolean gl30 = caps.OpenGL30;
                     int drawFb = GL11.glGetInteger(36006);
                     int readFb = gl30 ? GL11.glGetInteger(36010) : drawFb;
                     int program = GL11.glGetInteger(35725);
                     int activeUnit = GL11.glGetInteger(34016);
                     boolean scissor = GL11.glIsEnabled(3089);
                     boolean check = checkFrames > 0 || copy == null || rw > capW || rh > capH;
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
                        ensureTargets(rw, rh);
                        int glY = viewY + viewH - bottom;
                        int glX = viewX + left;
                        if (gl30) {
                           GL30.glBindFramebuffer(36008, readFb);
                           GL30.glBindFramebuffer(36009, copy.framebufferObject);
                           GL30.glBlitFramebuffer(glX, glY, glX + rw, glY + rh, 0, 0, rw, rh, 16384, 9728);
                        } else {
                           OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, drawFb);
                           GlStateManager.bindTexture(copy.framebufferTexture);
                           GL11.glCopyTexSubImage2D(3553, 0, 0, 0, glX, glY, rw, rh);
                        }

                        halfW = Math.max(1, rw / 2);
                        halfH = Math.max(1, rh / 2);
                        GL20.glUseProgram(downProgram);
                        GL20.glUniform2f(uDownTexel, 1.0F / copy.framebufferTextureWidth, 1.0F / copy.framebufferTextureHeight);
                        GL20.glUniform2f(uDownMax, (rw - 0.5F) / copy.framebufferTextureWidth, (rh - 0.5F) / copy.framebufferTextureHeight);
                        pass(half, halfW, halfH, copy, rw, rh);
                        GL20.glUseProgram(blurProgram);
                        GL20.glUniform2f(uBlurMax, (halfW - 0.5F) / half.framebufferTextureWidth, (halfH - 0.5F) / half.framebufferTextureHeight);
                        float spread = Math.max(0.8F, px * 0.9F);
                        blur(spread);
                        blur(spread * 2.0F);
                        ok = true;
                     } catch (RuntimeException | LinkageError var30) {
                        disable("capture failed", var30);
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

                     if (!ok) {
                        return false;
                     } else {
                        if (check) {
                           int error = GL11.glGetError();
                           if (error != 0) {
                              disable("OpenGL error " + error + " while capturing", null);
                              return false;
                           }

                           checkFrames--;
                        }

                        regionX = viewX + left;
                        regionY = viewY + viewH - bottom;
                        regionW = rw;
                        regionH = rh;
                        lastLeft = left;
                        lastTop = top;
                        capturedNanos = now;
                        ready = true;
                        return true;
                     }
                  }
               }
            }
         } else {
            ready = false;
            return false;
         }
      } else {
         ready = false;
         return false;
      }
   }

   private static void blur(float spread) {
      GL20.glUniform2f(uBlurDir, spread / half.framebufferTextureWidth, 0.0F);
      pass(pong, halfW, halfH, half, halfW, halfH);
      GL20.glUniform2f(uBlurDir, 0.0F, spread / pong.framebufferTextureHeight);
      pass(half, halfW, halfH, pong, halfW, halfH);
   }

   private static void pass(Framebuffer target, int tw, int th, Framebuffer source, int sw, int sh) {
      target.bindFramebuffer(false);
      GlStateManager.viewport(0, 0, tw, th);
      GlStateManager.bindTexture(source.framebufferTexture);
      float u = (float)sw / source.framebufferTextureWidth;
      float v = (float)sh / source.framebufferTextureHeight;
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_TEX);
      wr.pos(0.0, 0.0, 0.0).tex(0.0, 0.0).endVertex();
      wr.pos(1.0, 0.0, 0.0).tex(u, 0.0).endVertex();
      wr.pos(1.0, 1.0, 0.0).tex(u, v).endVertex();
      wr.pos(0.0, 1.0, 0.0).tex(0.0, v).endVertex();
      tess.draw();
   }

   public static boolean draw(float x, float y, float w, float h, float radius, int tint, int rim, float alpha) {
      if (ready && !disabled && !(w <= 0.0F) && !(h <= 0.0F) && !(alpha <= 0.0F)) {
         float px = Ui.px;
         float r = Math.max(0.5F, Math.min(radius, Math.min(w, h) / 2.0F));
         int activeUnit = GL11.glGetInteger(34016);
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         GL20.glUseProgram(glassProgram);
         GlStateManager.bindTexture(half.framebufferTexture);
         GL20.glUniform4f(uViewport, regionX, regionY, regionW, regionH);
         GL20.glUniform2f(uUvScale, (float)halfW / half.framebufferTextureWidth, (float)halfH / half.framebufferTextureHeight);
         GL20.glUniform2f(uUvMax, (halfW - 0.5F) / half.framebufferTextureWidth, (halfH - 0.5F) / half.framebufferTextureHeight);
         GL20.glUniform2f(uHalf, w * px / 2.0F, h * px / 2.0F);
         GL20.glUniform1f(uRadius, r * px);
         GL20.glUniform4f(uTint, ColorUtil.red(tint) / 255.0F, ColorUtil.green(tint) / 255.0F, ColorUtil.blue(tint) / 255.0F, ColorUtil.alpha(tint) / 255.0F);
         GL20.glUniform4f(uRim, ColorUtil.red(rim) / 255.0F, ColorUtil.green(rim) / 255.0F, ColorUtil.blue(rim) / 255.0F, ColorUtil.alpha(rim) / 255.0F);
         GL20.glUniform1f(uAlpha, alpha * Ui.alpha());
         GL20.glUniform1f(uBevel, Math.max(1.0F, r * px * 0.8F));
         GL20.glUniform1f(uRefract, Math.max(1.0F, r * px * 0.35F));
         GL20.glUniform1f(uHair, Math.max(1.0F, 0.85F * px));
         float m = 1.5F / px;
         float cx = x + w / 2.0F;
         float cy = y + h / 2.0F;
         Tessellator tess = Tessellator.getInstance();
         WorldRenderer wr = tess.getWorldRenderer();
         wr.begin(7, DefaultVertexFormats.POSITION_TEX);
         wr.pos(x - m, y + h + m, 0.0).tex((x - m - cx) * px, (y + h + m - cy) * px).endVertex();
         wr.pos(x + w + m, y + h + m, 0.0).tex((x + w + m - cx) * px, (y + h + m - cy) * px).endVertex();
         wr.pos(x + w + m, y - m, 0.0).tex((x + w + m - cx) * px, (y - m - cy) * px).endVertex();
         wr.pos(x - m, y - m, 0.0).tex((x - m - cx) * px, (y - m - cy) * px).endVertex();
         tess.draw();
         GL20.glUseProgram(Ui.outerProgram());
         GlStateManager.setActiveTexture(activeUnit);
         return true;
      } else {
         return false;
      }
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
      if (copy == null || w > capW || h > capH) {
         int nw = Math.max(capW, (w + 127) / 128 * 128);
         int nh = Math.max(capH, (h + 31) / 32 * 32);
         deleteTargets();
         ((Buffer)FLOATS).clear();
         GL11.glGetFloat(3106, FLOATS);

         try {
            copy = target(nw, nh);
            half = target(Math.max(1, nw / 2), Math.max(1, nh / 2));
            pong = target(Math.max(1, nw / 2), Math.max(1, nh / 2));
            capW = nw;
            capH = nh;
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
      capH = 0;
      capW = 0;
   }

   private static void compile() {
      if (compiled) {
         for (int p : new int[]{downProgram, blurProgram, glassProgram}) {
            if (p > 0) {
               GL20.glDeleteProgram(p);
            }
         }

         glassProgram = 0;
         blurProgram = 0;
         downProgram = 0;
         disabled = false;
      }

      compiled = true;
      filesGeneration = AssetSource.generation();
      missingFiles = ShaderFiles.text("gui/island_pass.vsh") == null || ShaderFiles.text("gui/island_glass.fsh") == null;
      if (!missingFiles) {
         int previous = GL11.glGetInteger(35725);

         try {
            downProgram = link("gui/island_pass.vsh", "gui/island_down.fsh");
            blurProgram = link("gui/island_pass.vsh", "gui/island_blur.fsh");
            glassProgram = link("gui/island_shape.vsh", "gui/island_glass.fsh");
            if (downProgram != 0 && blurProgram != 0 && glassProgram != 0) {
               uDownTexel = GL20.glGetUniformLocation(downProgram, "uTexel");
               uDownMax = GL20.glGetUniformLocation(downProgram, "uMax");
               uBlurDir = GL20.glGetUniformLocation(blurProgram, "uDir");
               uBlurMax = GL20.glGetUniformLocation(blurProgram, "uMax");
               uViewport = GL20.glGetUniformLocation(glassProgram, "uViewport");
               uUvScale = GL20.glGetUniformLocation(glassProgram, "uUvScale");
               uUvMax = GL20.glGetUniformLocation(glassProgram, "uUvMax");
               uHalf = GL20.glGetUniformLocation(glassProgram, "uHalf");
               uRadius = GL20.glGetUniformLocation(glassProgram, "uRadius");
               uTint = GL20.glGetUniformLocation(glassProgram, "uTint");
               uRim = GL20.glGetUniformLocation(glassProgram, "uRim");
               uAlpha = GL20.glGetUniformLocation(glassProgram, "uAlpha");
               uBevel = GL20.glGetUniformLocation(glassProgram, "uBevel");
               uRefract = GL20.glGetUniformLocation(glassProgram, "uRefract");
               uHair = GL20.glGetUniformLocation(glassProgram, "uHair");
               GL20.glUseProgram(downProgram);
               GL20.glUniform1i(GL20.glGetUniformLocation(downProgram, "uTex"), 0);
               GL20.glUseProgram(blurProgram);
               GL20.glUniform1i(GL20.glGetUniformLocation(blurProgram, "uTex"), 0);
               GL20.glUseProgram(glassProgram);
               GL20.glUniform1i(GL20.glGetUniformLocation(glassProgram, "uBg"), 0);
               return;
            }

            disable("shaders did not compile", null);
         } catch (RuntimeException | LinkageError var7) {
            disable("shaders failed", var7);
            return;
         } finally {
            GL20.glUseProgram(previous);
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
            FuriahsVisuals.LOG.warn("Island glass shader failed to link: {}", new Object[]{GL20.glGetProgramInfoLog(p, 2048)});
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
            FuriahsVisuals.LOG.warn("Island glass shader failed to compile: {}", new Object[]{GL20.glGetShaderInfoLog(s, 2048)});
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
            FuriahsVisuals.LOG.warn("WaterMark glass disabled ({}); the island is drawn solid", new Object[]{reason, error});
         } else {
            FuriahsVisuals.LOG.warn("WaterMark glass disabled ({}); the island is drawn solid", new Object[]{reason});
         }
      }

      ready = false;
   }

   static boolean isAvailable() {
      return !disabled;
   }
}
