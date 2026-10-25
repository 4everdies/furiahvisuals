package meowtils.furiahvisuals.render.post;

import java.nio.ByteBuffer;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.GlSave;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

public final class PostFrame {
   private static final long CHECK_MS = 2000L;
   private static final int FIRST_CHECKS = 20;
   private final GlSave save = new GlSave();
   private int fbo;
   private int width;
   private int height;
   private int vanillaTexture;
   private boolean running;
   private int colorTex;
   private int depthTex;
   private int texW;
   private int texH;
   private boolean colorValid;
   private boolean depthValid;
   private final int[] checks = new int[PostChain.Point.values().length];
   private final long[] nextCheck = new long[PostChain.Point.values().length];
   private final boolean[] askDriver = new boolean[PostChain.Point.values().length];
   private int point;
   private int sizeVersion;
   private int lastW = -1;
   private int lastH = -1;
   private static Boolean floatTextures;

   boolean begin(Minecraft mc, PostChain.Point at) {
      int w = mc.displayWidth;
      int h = mc.displayHeight;
      if (w > 0 && h > 0) {
         this.point = at.ordinal();
         Framebuffer main = mc.getFramebuffer();
         boolean vanilla = OpenGlHelper.isFramebufferEnabled() && main != null && main.framebufferObject >= 0;
         int expectFbo = vanilla ? main.framebufferObject : 0;
         int expectW = vanilla ? main.framebufferWidth : w;
         int expectH = vanilla ? main.framebufferHeight : h;
         long now = System.currentTimeMillis();
         if (!this.askDriver[this.point] && this.checks[this.point] >= 20 && now < this.nextCheck[this.point]) {
            this.save.saveQuick();
            this.fbo = expectFbo;
            this.width = expectW;
            this.height = expectH;
         } else {
            this.save.save();
            int bound = this.save.drawFramebuffer();
            int[] vp = this.save.viewport();
            if (!this.askDriver[this.point] && (bound != expectFbo || vp[0] != 0 || vp[1] != 0 || vp[2] != expectW || vp[3] != expectH)) {
               this.askDriver[this.point] = true;
               FuriahsVisuals.LOG
                  .info(
                     "Post passes: the framebuffer {} is {} ({}x{} at {},{}), not the expected {} ({}x{}): asking the driver there from now on",
                     new Object[]{at, bound, vp[2], vp[3], vp[0], vp[1], expectFbo, expectW, expectH}
                  );
            }

            this.fbo = bound;
            this.width = Math.max(1, vp[2]);
            this.height = Math.max(1, vp[3]);
            int n = this.point;
            this.checks[n]++;
            this.nextCheck[this.point] = now + 2000L;
         }

         int n = this.vanillaTexture = vanilla
               && this.fbo == main.framebufferObject
               && main.framebufferTexture > 0
               && main.framebufferTextureWidth == this.width
               && main.framebufferTextureHeight == this.height
            ? main.framebufferTexture
            : 0;
         if (this.width != this.lastW || this.height != this.lastH) {
            this.lastW = this.width;
            this.lastH = this.height;
            this.releaseCopies();
            this.sizeVersion++;
         }

         this.colorValid = false;
         this.depthValid = false;
         this.running = true;
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         GlStateManager.disableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.disableBlend();
         GlStateManager.disableAlpha();
         GlStateManager.disableCull();
         GlStateManager.disableFog();
         GlStateManager.disableLighting();
         GlStateManager.colorMask(true, true, true, true);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         return true;
      } else {
         return false;
      }
   }

   void end() {
      this.running = false;
      this.bindTarget();
      this.save.restore();
   }

   public void frameWritten() {
      this.colorValid = false;
   }

   void passDone() {
      this.colorValid = false;
      this.bindTarget();
      GlStateManager.colorMask(true, true, true, true);
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
   }

   public int framebuffer() {
      return this.fbo;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public int sizeVersion() {
      return this.sizeVersion;
   }

   public void bindTarget() {
      OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.fbo);
      GlStateManager.viewport(0, 0, this.width, this.height);
   }

   public int scene() {
      return this.vanillaTexture != 0 ? this.vanillaTexture : this.color();
   }

   public int color() {
      if (!this.colorValid) {
         if (this.colorTex == 0) {
            this.colorTex = this.texture(32856, 6408, 5121, true);
         }

         this.bindTarget();
         GlStateManager.bindTexture(this.colorTex);
         GL11.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, this.width, this.height);
         this.colorValid = true;
      }

      return this.colorTex;
   }

   public int depth() {
      if (!this.depthValid) {
         if (this.depthTex == 0) {
            this.depthTex = this.texture(33190, 6402, 5125, false);
            GL11.glTexParameteri(3553, 34892, 0);
         }

         this.bindTarget();
         GlStateManager.bindTexture(this.depthTex);
         GL11.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, this.width, this.height);
         this.depthValid = true;
      }

      return this.depthTex;
   }

   private int texture(int internalFormat, int format, int type, boolean linear) {
      int tex = TextureUtil.glGenTextures();
      GlStateManager.bindTexture(tex);
      int filter = linear ? 9729 : 9728;
      GL11.glTexParameteri(3553, 10241, filter);
      GL11.glTexParameteri(3553, 10240, filter);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL11.glTexImage2D(3553, 0, internalFormat, this.width, this.height, 0, format, type, (ByteBuffer)null);
      this.texW = this.width;
      this.texH = this.height;
      return tex;
   }

   public static void bind(int unit, int texture) {
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
      GlStateManager.bindTexture(texture);
      if (unit != 0) {
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      }
   }

   public static void quad() {
      Tessellator t = Tessellator.getInstance();
      WorldRenderer wr = t.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION);
      wr.pos(-1.0, -1.0, 0.0).endVertex();
      wr.pos(1.0, -1.0, 0.0).endVertex();
      wr.pos(1.0, 1.0, 0.0).endVertex();
      wr.pos(-1.0, 1.0, 0.0).endVertex();
      t.draw();
   }

   boolean allocated() {
      return this.colorTex != 0 || this.depthTex != 0;
   }

   private void releaseCopies() {
      if (this.colorTex != 0) {
         TextureUtil.deleteTexture(this.colorTex);
         this.colorTex = 0;
      }

      if (this.depthTex != 0) {
         TextureUtil.deleteTexture(this.depthTex);
         this.depthTex = 0;
      }

      this.colorValid = false;
      this.depthValid = false;
   }

   void release() {
      if (!this.running) {
         this.releaseCopies();
      }
   }

   int[] copiesSize() {
      return new int[]{this.colorTex == 0 && this.depthTex == 0 ? 0 : this.texW, this.colorTex == 0 && this.depthTex == 0 ? 0 : this.texH};
   }

   public boolean direct() {
      return this.vanillaTexture != 0;
   }

   public boolean asksDriver(PostChain.Point at) {
      return this.askDriver[at.ordinal()];
   }

   public static boolean floatTextures() {
      if (floatTextures == null) {
         try {
            ContextCapabilities caps = GLContext.getCapabilities();
            floatTextures = caps.OpenGL30 || caps.GL_ARB_texture_float;
         } catch (RuntimeException | LinkageError var1) {
            floatTextures = false;
         }
      }

      return floatTextures;
   }
}
