package meowtils.furiahvisuals.web;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import meowtils.furiahvisuals.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

final class IconExport {
   private IconExport() {
   }

   static BufferedImage render(int size, IconExport.Painter painter) {
      if (!OpenGlHelper.isFramebufferEnabled()) {
         return null;
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         Framebuffer fb = new Framebuffer(size, size, false);
         float oldPx = Ui.px;
         GlStateManager.matrixMode(5889);
         GlStateManager.pushMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();

         BufferedImage var17;
         try {
            fb.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
            fb.framebufferClear();
            fb.bindFramebuffer(true);
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0, size, size, 0.0, 1000.0, 3000.0);
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.0F, 0.0F, -2000.0F);
            GlStateManager.disableDepth();
            GlStateManager.disableAlpha();
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Ui.px = 1.0F;
            Ui.beginFrame();

            try {
               painter.paint(size);
            } finally {
               Ui.endFrame();
            }

            ByteBuffer pixels = BufferUtils.createByteBuffer(size * size * 4);
            GL11.glPixelStorei(3333, 1);
            GL11.glReadPixels(0, 0, size, size, 6408, 5121, pixels);
            BufferedImage img = new BufferedImage(size, size, 2);

            for (int bufferedImage = 0; bufferedImage < size; bufferedImage++) {
               for (int x = 0; x < size; x++) {
                  int cover = pixels.get((bufferedImage * size + x) * 4) & 255;
                  img.setRGB(x, size - 1 - bufferedImage, cover << 24 | 16777215);
               }
            }

            var17 = img;
         } finally {
            Ui.px = oldPx;
            GlStateManager.matrixMode(5889);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.enableDepth();
            fb.unbindFramebuffer();
            fb.deleteFramebuffer();
            mc.getFramebuffer().bindFramebuffer(true);
         }

         return var17;
      }
   }

   interface Painter {
      void paint(float var1);
   }
}
