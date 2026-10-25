package meowtils.furiahvisuals.module.weapons;

import meowtils.furiahvisuals.render.GlTarget;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

final class SsaaBuffer {
   private static final long MAX_PIXELS = 16000000L;
   private static final long SHRINK_NANOS = 5000000000L;
   private GlTarget target;
   private int maxSize = -1;
   private long smallSince;
   int factor;
   int rx;
   int ry;
   int rw;
   int rh;
   int bw;
   int bh;

   boolean begin(int x, int y, int w, int h, int wantFactor) {
      if (this.maxSize < 0) {
         this.maxSize = Math.max(1024, GL11.glGetInteger(3379));
      }

      int f = wantFactor;

      while (f > 1 && ((long)w * f * h * f > 16000000L || w * f > this.maxSize || h * f > this.maxSize)) {
         f /= 2;
      }

      if (f < 2) {
         return false;
      } else {
         int needW = w * f;
         int needH = h * f;
         long now = System.nanoTime();
         if (this.target != null && (this.target.width < needW || this.target.height < needH)) {
            GlTarget.delete(this.target);
            this.target = null;
         }

         if (this.target == null || needW >= this.target.width / 2 || needH >= this.target.height / 2) {
            this.smallSince = 0L;
         } else if (this.smallSince == 0L) {
            this.smallSince = now;
         } else if (now - this.smallSince > 5000000000L) {
            GlTarget.delete(this.target);
            this.target = null;
         }

         if (this.target == null) {
            int tw = Math.min(this.maxSize, (int)Math.ceil(needW * 1.2) + 8);
            int th = Math.min(this.maxSize, (int)Math.ceil(needH * 1.2) + 8);
            this.target = GlTarget.create(tw, th, 32856, 6408, true, true);
            if (this.target == null) {
               return false;
            }

            this.smallSince = 0L;
         }

         this.factor = f;
         this.rx = x;
         this.ry = y;
         this.rw = w;
         this.rh = h;
         this.bw = needW;
         this.bh = needH;
         this.target.bind();
         GlStateManager.viewport(0, 0, this.bw, this.bh);
         GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GlStateManager.depthMask(true);
         GlStateManager.clear(16640);
         return true;
      }
   }

   void composite(WeaponShader.Program p, int screenW, int screenH, int bloomTexture, float bloom) {
      this.composite(p, screenW, screenH, bloomTexture, bloom, 0.0F);
   }

   void composite(WeaponShader.Program p, int screenW, int screenH, int bloomTexture, float bloom, float block) {
      p.use();
      p.set2f("uTexel", 1.0F / this.target.width, 1.0F / this.target.height);
      p.set1f("uTaps", this.factor >= 4 ? 2.0F : 1.0F);
      p.set1f("uMosaic", block > 1.0F ? block * this.factor : 0.0F);
      p.set2f("uUsed", (float)this.bw / this.target.width, (float)this.bh / this.target.height);
      p.set1f("uBloom", bloomTexture >= 0 ? bloom : 0.0F);
      p.set1f("uOutline", 0.0F);
      p.set4f("uBloomRect", 0.0F, 0.0F, 1.0F, 1.0F);
      if (bloomTexture >= 0) {
         GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
         GlStateManager.bindTexture(bloomTexture);
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      }

      GlStateManager.bindTexture(this.target.texture);
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      GlStateManager.disableCull();
      GlStateManager.disableAlpha();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
      float x0 = (float)this.rx / screenW * 2.0F - 1.0F;
      float x1 = (float)(this.rx + this.rw) / screenW * 2.0F - 1.0F;
      float y0 = (float)this.ry / screenH * 2.0F - 1.0F;
      float y1 = (float)(this.ry + this.rh) / screenH * 2.0F - 1.0F;
      float u1 = (float)this.bw / this.target.width;
      float v1 = (float)this.bh / this.target.height;
      Tessellator t = Tessellator.getInstance();
      WorldRenderer wr = t.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_TEX);
      wr.pos(x0, y0, 0.0).tex(0.0, 0.0).endVertex();
      wr.pos(x1, y0, 0.0).tex(u1, 0.0).endVertex();
      wr.pos(x1, y1, 0.0).tex(u1, v1).endVertex();
      wr.pos(x0, y1, 0.0).tex(0.0, v1).endVertex();
      t.draw();
   }

   int texture() {
      return this.target == null ? -1 : this.target.texture;
   }

   void delete() {
      GlTarget.delete(this.target);
      this.target = null;
   }
}
