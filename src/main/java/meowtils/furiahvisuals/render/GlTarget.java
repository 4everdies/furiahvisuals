package meowtils.furiahvisuals.render;

import java.nio.ByteBuffer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.opengl.GL11;

public final class GlTarget {
   public final int width;
   public final int height;
   public final int texture;
   private final int fbo;
   private final int depth;

   private GlTarget(int width, int height, int texture, int fbo, int depth) {
      this.width = width;
      this.height = height;
      this.texture = texture;
      this.fbo = fbo;
      this.depth = depth;
   }

   public static GlTarget create(int width, int height, int internalFormat, int format, boolean linear, boolean withDepth) {
      int tex = TextureUtil.glGenTextures();
      GlStateManager.bindTexture(tex);
      int filter = linear ? 9729 : 9728;
      GL11.glTexParameteri(3553, 10241, filter);
      GL11.glTexParameteri(3553, 10240, filter);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL11.glTexImage2D(3553, 0, internalFormat, width, height, 0, format, 5121, (ByteBuffer)null);
      int fb = OpenGlHelper.glGenFramebuffers();
      OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, fb);
      OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0, 3553, tex, 0);
      int rb = -1;
      if (withDepth) {
         rb = OpenGlHelper.glGenRenderbuffers();
         OpenGlHelper.glBindRenderbuffer(OpenGlHelper.GL_RENDERBUFFER, rb);
         OpenGlHelper.glRenderbufferStorage(OpenGlHelper.GL_RENDERBUFFER, 33190, width, height);
         OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_DEPTH_ATTACHMENT, OpenGlHelper.GL_RENDERBUFFER, rb);
      }

      GlTarget target = new GlTarget(width, height, tex, fb, rb);
      if (OpenGlHelper.glCheckFramebufferStatus(OpenGlHelper.GL_FRAMEBUFFER) != OpenGlHelper.GL_FRAMEBUFFER_COMPLETE) {
         target.delete();
         return null;
      } else {
         return target;
      }
   }

   public void bind() {
      OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.fbo);
      GlStateManager.viewport(0, 0, this.width, this.height);
   }

   public void delete() {
      if (this.depth >= 0) {
         OpenGlHelper.glDeleteRenderbuffers(this.depth);
      }

      OpenGlHelper.glDeleteFramebuffers(this.fbo);
      TextureUtil.deleteTexture(this.texture);
   }

   public static void delete(GlTarget target) {
      if (target != null) {
         target.delete();
      }
   }
}
