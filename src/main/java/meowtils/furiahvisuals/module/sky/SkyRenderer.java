package meowtils.furiahvisuals.module.sky;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import meowtils.furiahvisuals.render.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.Project;

final class SkyRenderer {
   private static final ResourceLocation SUN = new ResourceLocation("textures/environment/sun.png");
   private static final ResourceLocation MOON = new ResourceLocation("textures/environment/moon_phases.png");
   private static final float BOX = 10.0F;
   private static final float[] NO_OPTIONS = new float[8];
   private final SkyShaders shaders = new SkyShaders();
   private final IntBuffer ints = BufferUtils.createIntBuffer(16);
   private final FloatBuffer floats = BufferUtils.createFloatBuffer(16);
   private Framebuffer lowRes;
   private int savedProgram;
   private boolean savedCull;
   private static final int THUMB_COLS = 8;
   private Framebuffer thumbs;
   private int thumbW;
   private int thumbH;
   private int thumbRows;

   SkyShaders shaders() {
      return this.shaders;
   }

   void begin(boolean blend) {
      this.savedProgram = GL11.glGetInteger(35725);
      this.savedCull = GL11.glIsEnabled(2884);
      GlStateManager.disableCull();
      GlStateManager.disableAlpha();
      GlStateManager.depthMask(false);
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      blend(blend);
   }

   void end() {
      GL20.glUseProgram(this.savedProgram);
      if (this.savedCull) {
         GlStateManager.enableCull();
      } else {
         GlStateManager.disableCull();
      }

      GlStateManager.enableAlpha();
      GlStateManager.depthMask(true);
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.enableFog();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   void restoreProgram() {
      GL20.glUseProgram(this.savedProgram);
   }

   static void blend(boolean on) {
      if (on) {
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      } else {
         GlStateManager.disableBlend();
      }
   }

   boolean draw(SkyStyle style, SkyRenderer.Frame f, float alpha) {
      SkyShaders.Program p = this.shaders.get(style);
      if (p == null) {
         return false;
      } else {
         if (f.prepare != null) {
            f.prepare.apply(style, f);
         }

         int texture = style == SkyStyle.CUSTOM_IMAGE && f.image >= 0 ? f.image : this.shaders.noiseTexture();
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         GlStateManager.bindTexture(texture);
         GL20.glUseProgram(p.id);
         GL20.glUniform1i(p.noise, 0);
         GL20.glUniform1i(p.image, 0);
         GL20.glUniform1f(p.time, f.time);
         GL20.glUniform1f(p.detail, f.detail);
         GL20.glUniform1f(p.scale, f.scale);
         color3(p.colA, f.useStyleColors ? style.colorA : f.colA);
         color3(p.colB, f.useStyleColors ? style.colorB : f.colB);
         GL20.glUniform3f(p.parallax, f.parallaxX, f.parallaxY, f.parallaxZ);
         GL20.glUniform1f(p.clouds, f.clouds);
         GL20.glUniform1f(p.sunOwn, f.sunOwn);
         GL20.glUniform1f(p.rain, f.rain);
         GL20.glUniform1f(p.flash, f.flash);
         GL20.glUniform3f(p.flashDir, f.flashX, f.flashY, f.flashZ);
         GL20.glUniform1f(p.stars, f.stars);
         GL20.glUniform1f(p.twinkle, f.twinkle);
         color3(p.starCol, f.starColor);
         if (f.meteorHeads != null) {
            float[] h = f.meteorHeads;
            float[] t = f.meteorTails;

            for (int i = 0; i < 4; i++) {
               GL20.glUniform4f(p.meteorA[i], h[i * 4], h[i * 4 + 1], h[i * 4 + 2], h[i * 4 + 3]);
               GL20.glUniform4f(p.meteorB[i], t[i * 4], t[i * 4 + 1], t[i * 4 + 2], t[i * 4 + 3]);
            }
         }

         GL20.glUniform1f(p.bright, f.brightness);
         GL20.glUniform1f(p.sat, f.saturation);
         GL20.glUniform1f(p.alpha, alpha);
         GL20.glUniform2f(p.spin, (float)Math.cos(f.spin), (float)Math.sin(f.spin));
         GL20.glUniform1f(p.custom, f.custom);
         GL20.glUniform1f(p.hasImage, f.image >= 0 ? 1.0F : 0.0F);
         float[] o = f.options != null ? f.options.of(style) : NO_OPTIONS;
         GL20.glUniform4f(p.optA, o[0], o[1], o[2], o[3]);
         GL20.glUniform4f(p.optB, o[4], o[5], o[6], o[7]);
         GL20.glUniform1f(p.beat, f.beat);
         GL20.glUniform3f(p.look, f.lookX, f.lookY, f.lookZ);
         GL20.glUniform1f(p.contrast, f.contrast);
         drawBox();
         return true;
      }
   }

   private static void color3(int location, int argb) {
      GL20.glUniform3f(location, ColorUtil.red(argb) / 255.0F, ColorUtil.green(argb) / 255.0F, ColorUtil.blue(argb) / 255.0F);
   }

   private static void drawBox() {
      float b = 10.0F;
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION);
      wr.pos(b, -b, -b).endVertex();
      wr.pos(b, b, -b).endVertex();
      wr.pos(b, b, b).endVertex();
      wr.pos(b, -b, b).endVertex();
      wr.pos(-b, -b, b).endVertex();
      wr.pos(-b, b, b).endVertex();
      wr.pos(-b, b, -b).endVertex();
      wr.pos(-b, -b, -b).endVertex();
      wr.pos(-b, b, -b).endVertex();
      wr.pos(-b, b, b).endVertex();
      wr.pos(b, b, b).endVertex();
      wr.pos(b, b, -b).endVertex();
      wr.pos(-b, -b, b).endVertex();
      wr.pos(-b, -b, -b).endVertex();
      wr.pos(b, -b, -b).endVertex();
      wr.pos(b, -b, b).endVertex();
      wr.pos(b, -b, b).endVertex();
      wr.pos(b, b, b).endVertex();
      wr.pos(-b, b, b).endVertex();
      wr.pos(-b, -b, b).endVertex();
      wr.pos(-b, -b, -b).endVertex();
      wr.pos(-b, b, -b).endVertex();
      wr.pos(b, b, -b).endVertex();
      wr.pos(b, -b, -b).endVertex();
      tess.draw();
   }

   boolean drawLowRes(SkyStyle from, SkyStyle to, float mix, SkyRenderer.Frame f, float alpha, float scale) {
      if (OpenGlHelper.isFramebufferEnabled() && this.shaders.composite() != 0) {
         ((Buffer)this.ints).clear();
         GL11.glGetInteger(2978, this.ints);
         int vx = this.ints.get(0);
         int vy = this.ints.get(1);
         int vw = this.ints.get(2);
         int vh = this.ints.get(3);
         if (vw >= 16 && vh >= 16) {
            int w = Math.max(16, Math.round(vw * scale));
            int h = Math.max(16, Math.round(vh * scale));
            boolean gl30 = GLContext.getCapabilities().OpenGL30;
            int drawFb = GL11.glGetInteger(36006);
            int readFb = gl30 ? GL11.glGetInteger(36010) : drawFb;

            boolean drawn;
            try {
               this.ensureLowRes(w, h);
               this.lowRes.bindFramebuffer(true);
               blend(false);
               drawn = this.draw(from != null ? from : to, f, 1.0F);
               if (drawn && from != null && mix > 0.0F) {
                  blend(true);
                  this.draw(to, f, mix);
               }
            } finally {
               bindFramebuffers(drawFb, readFb, gl30);
               GlStateManager.viewport(vx, vy, vw, vh);
            }

            if (!drawn) {
               return false;
            } else {
               blend(alpha < 1.0F);
               GL20.glUseProgram(this.shaders.composite());
               GL20.glUniform1i(this.shaders.compositeTexLocation(), 0);
               GL20.glUniform1f(this.shaders.compositeAlphaLocation(), alpha);
               GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
               GlStateManager.bindTexture(this.lowRes.framebufferTexture);
               float u = (float)this.lowRes.framebufferWidth / this.lowRes.framebufferTextureWidth;
               float v = (float)this.lowRes.framebufferHeight / this.lowRes.framebufferTextureHeight;
               Tessellator tess = Tessellator.getInstance();
               WorldRenderer wr = tess.getWorldRenderer();
               wr.begin(7, DefaultVertexFormats.POSITION_TEX);
               wr.pos(-1.0, -1.0, 0.0).tex(0.0, 0.0).endVertex();
               wr.pos(1.0, -1.0, 0.0).tex(u, 0.0).endVertex();
               wr.pos(1.0, 1.0, 0.0).tex(u, v).endVertex();
               wr.pos(-1.0, 1.0, 0.0).tex(0.0, v).endVertex();
               tess.draw();
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void ensureLowRes(int w, int h) {
      if (this.lowRes == null || this.lowRes.framebufferWidth != w || this.lowRes.framebufferHeight != h) {
         ((Buffer)this.floats).clear();
         GL11.glGetFloat(3106, this.floats);
         boolean depth = GL11.glIsEnabled(2929);

         try {
            if (this.lowRes == null) {
               this.lowRes = new Framebuffer(w, h, false);
            } else {
               this.lowRes.createBindFramebuffer(w, h);
            }

            this.lowRes.setFramebufferFilter(9729);
         } finally {
            GlStateManager.clearColor(this.floats.get(0), this.floats.get(1), this.floats.get(2), this.floats.get(3));
            if (depth) {
               GlStateManager.enableDepth();
            } else {
               GlStateManager.disableDepth();
            }
         }
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

   void drawVanillaSunMoon(Minecraft mc, WorldClient world, float partialTicks) {
      GlStateManager.enableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
      GlStateManager.pushMatrix();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F - world.getRainStrength(partialTicks));
      GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(world.getCelestialAngle(partialTicks) * 360.0F, 1.0F, 0.0F, 0.0F);
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      float size = 30.0F;
      mc.getTextureManager().bindTexture(SUN);
      wr.begin(7, DefaultVertexFormats.POSITION_TEX);
      wr.pos(-size, 100.0, -size).tex(0.0, 0.0).endVertex();
      wr.pos(size, 100.0, -size).tex(1.0, 0.0).endVertex();
      wr.pos(size, 100.0, size).tex(1.0, 1.0).endVertex();
      wr.pos(-size, 100.0, size).tex(0.0, 1.0).endVertex();
      tess.draw();
      size = 20.0F;
      mc.getTextureManager().bindTexture(MOON);
      int phase = world.getMoonPhase();
      int col = phase % 4;
      int row = phase / 4 % 2;
      double u0 = col / 4.0;
      double v0 = row / 2.0;
      double u1 = (col + 1) / 4.0;
      double v1 = (row + 1) / 2.0;
      wr.begin(7, DefaultVertexFormats.POSITION_TEX);
      wr.pos(-size, -100.0, size).tex(u1, v1).endVertex();
      wr.pos(size, -100.0, size).tex(u0, v1).endVertex();
      wr.pos(size, -100.0, -size).tex(u0, v0).endVertex();
      wr.pos(-size, -100.0, -size).tex(u1, v0).endVertex();
      tess.draw();
      GlStateManager.popMatrix();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   boolean drawPreview(SkyStyle style, SkyRenderer.Frame f, float yaw, float pitch) {
      ((Buffer)this.ints).clear();
      GL11.glGetInteger(2978, this.ints);
      float aspect = (float)this.ints.get(2) / Math.max(1, this.ints.get(3));
      int program = GL11.glGetInteger(35725);
      GlStateManager.matrixMode(5889);
      GlStateManager.pushMatrix();
      GlStateManager.loadIdentity();
      Project.gluPerspective(75.0F, aspect, 0.05F, 100.0F);
      GlStateManager.matrixMode(5888);
      GlStateManager.pushMatrix();
      GlStateManager.loadIdentity();
      GlStateManager.rotate(pitch - 22.0F, 1.0F, 0.0F, 0.0F);
      GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
      GlStateManager.depthMask(false);

      boolean drawn;
      try {
         drawn = this.draw(style, f, 1.0F);
      } finally {
         GL20.glUseProgram(program);
         GlStateManager.depthMask(true);
         GlStateManager.matrixMode(5889);
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.popMatrix();
      }

      return drawn;
   }

   boolean drawThumbnails(SkyStyle[] styles, float[] views, int w, int h, SkyRenderer.Frame f) {
      if (OpenGlHelper.isFramebufferEnabled() && w >= 4 && h >= 4) {
         int rows = (styles.length + 8 - 1) / 8;
         ((Buffer)this.ints).clear();
         GL11.glGetInteger(2978, this.ints);
         int vx = this.ints.get(0);
         int vy = this.ints.get(1);
         int vw = this.ints.get(2);
         int vh = this.ints.get(3);
         boolean gl30 = GLContext.getCapabilities().OpenGL30;
         int drawFb = GL11.glGetInteger(36006);
         int readFb = gl30 ? GL11.glGetInteger(36010) : drawFb;
         int program = GL11.glGetInteger(35725);
         boolean scissor = GL11.glIsEnabled(3089);
         boolean depth = GL11.glIsEnabled(2929);
         boolean cull = GL11.glIsEnabled(2884);
         boolean alphaTest = GL11.glIsEnabled(3008);
         GlStateManager.matrixMode(5889);
         GlStateManager.pushMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();

         try {
            if (scissor) {
               GL11.glDisable(3089);
            }

            this.ensureThumbs(w, h, rows);
            this.thumbs.bindFramebuffer(false);
            GlStateManager.disableBlend();
            GlStateManager.disableCull();
            GlStateManager.disableAlpha();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);

            for (int i = 0; i < styles.length; i++) {
               GlStateManager.viewport(i % 8 * w, i / 8 * h, w, h);
               GlStateManager.matrixMode(5889);
               GlStateManager.loadIdentity();
               Project.gluPerspective(78.0F, (float)w / h, 0.05F, 100.0F);
               GlStateManager.matrixMode(5888);
               GlStateManager.loadIdentity();
               GlStateManager.rotate(-views[i * 2 + 1], 1.0F, 0.0F, 0.0F);
               GlStateManager.rotate(views[i * 2], 0.0F, 1.0F, 0.0F);
               float yaw = (float)Math.toRadians(views[i * 2]);
               float pitch = (float)Math.toRadians(views[i * 2 + 1]);
               f.lookX = (float)(Math.sin(yaw) * Math.cos(pitch));
               f.lookY = (float)Math.sin(pitch);
               f.lookZ = (float)(-Math.cos(yaw) * Math.cos(pitch));
               if (!this.draw(styles[i], f, 1.0F)) {
                  GL20.glUseProgram(program);
               }
            }
         } finally {
            GL20.glUseProgram(program);
            bindFramebuffers(drawFb, readFb, gl30);
            GlStateManager.viewport(vx, vy, vw, vh);
            if (scissor) {
               GL11.glEnable(3089);
            }

            GlStateManager.matrixMode(5889);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            if (depth) {
               GlStateManager.enableDepth();
            } else {
               GlStateManager.disableDepth();
            }

            if (cull) {
               GlStateManager.enableCull();
            } else {
               GlStateManager.disableCull();
            }

            if (alphaTest) {
               GlStateManager.enableAlpha();
            } else {
               GlStateManager.disableAlpha();
            }

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         }

         return true;
      } else {
         return false;
      }
   }

   private void ensureThumbs(int w, int h, int rows) {
      if (this.thumbs == null || this.thumbW != w || this.thumbH != h || this.thumbRows != rows) {
         ((Buffer)this.floats).clear();
         GL11.glGetFloat(3106, this.floats);
         boolean depth = GL11.glIsEnabled(2929);

         try {
            if (this.thumbs == null) {
               this.thumbs = new Framebuffer(w * 8, h * rows, false);
            } else {
               this.thumbs.createBindFramebuffer(w * 8, h * rows);
            }

            this.thumbs.setFramebufferFilter(9729);
            this.thumbW = w;
            this.thumbH = h;
            this.thumbRows = rows;
         } finally {
            GlStateManager.clearColor(this.floats.get(0), this.floats.get(1), this.floats.get(2), this.floats.get(3));
            if (depth) {
               GlStateManager.enableDepth();
            } else {
               GlStateManager.disableDepth();
            }
         }
      }
   }

   int thumbTexture() {
      return this.thumbs == null ? -1 : this.thumbs.framebufferTexture;
   }

   float[] thumbUv(int i) {
      float tw = this.thumbs.framebufferTextureWidth;
      float th = this.thumbs.framebufferTextureHeight;
      float u0 = i % 8 * this.thumbW / tw;
      float vBottom = i / 8 * this.thumbH / th;
      return new float[]{u0, vBottom + this.thumbH / th, u0 + this.thumbW / tw, vBottom};
   }

   void releaseLowRes() {
      if (this.lowRes != null) {
         boolean gl30 = GLContext.getCapabilities().OpenGL30;
         int drawFb = GL11.glGetInteger(36006);
         int readFb = gl30 ? GL11.glGetInteger(36010) : drawFb;

         try {
            this.lowRes.deleteFramebuffer();
            this.lowRes = null;
         } finally {
            bindFramebuffers(drawFb, readFb, gl30);
         }
      }
   }

   static final class Frame {
      float time;
      float detail = 5.0F;
      float scale = 1.0F;
      int colA = -1;
      int colB = -1;
      float parallaxX;
      float parallaxY;
      float parallaxZ;
      float clouds = 1.0F;
      float sunOwn = 1.0F;
      float rain;
      float flash;
      float flashX;
      float flashY = 1.0F;
      float flashZ;
      float stars;
      float twinkle;
      int starColor = -1;
      float[] meteorHeads;
      float[] meteorTails;
      float brightness = 1.0F;
      float saturation = 1.0F;
      float spin;
      float custom;
      int image = -1;
      boolean useStyleColors;
      float beat;
      float contrast = 1.0F;
      float lookX;
      float lookY;
      float lookZ = -1.0F;
      SkyRenderer.Options options;
      SkyRenderer.Prepare prepare;
   }

   interface Options {
      float[] of(SkyStyle var1);
   }

   interface Prepare {
      void apply(SkyStyle var1, SkyRenderer.Frame var2);
   }
}
