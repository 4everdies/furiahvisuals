package meowtils.furiahvisuals.module.weapons;

import meowtils.furiahvisuals.render.GlTarget;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL20;

final class WeaponStudio {
   private final WeaponRenderer renderer;
   private GlTarget work;
   final WeaponLight light = new WeaponLight();
   private final Mat4 proj = new Mat4();
   private final float[] c = new float[4];

   WeaponStudio(WeaponRenderer renderer) {
      this.renderer = renderer;
   }

   boolean begin(int w, int h) {
      if (this.work != null && (this.work.width < w || this.work.height < h)) {
         GlTarget.delete(this.work);
         this.work = null;
      }

      if (this.work == null) {
         this.work = GlTarget.create(Math.max(w, 256), Math.max(h, 256), 32856, 6408, true, true);
         if (this.work == null) {
            return false;
         }
      }

      this.work.bind();
      GlStateManager.viewport(0, 0, w, h);
      GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
      GlStateManager.depthMask(true);
      GlStateManager.clear(16640);
      return true;
   }

   void draw(WeaponAssets a, WeaponRenderer.Look look, int lod, Mat4 model, float fov, int w, int h, float time) {
      this.proj.perspective(fov, (float)w / Math.max(1, h), 0.05F, 50.0F);
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.multMatrix(this.proj.buffer());
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.multMatrix(model.buffer());
      GlStateManager.enableDepth();
      GlStateManager.depthFunc(515);
      GlStateManager.depthMask(true);
      GlStateManager.disableCull();
      GlStateManager.disableAlpha();
      GlStateManager.disableLighting();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
      if (this.renderer.beginShaded(this.light, false, time)) {
         this.renderer.draw(a, lod, look, WeaponRenderer.Mode.SHADED, model.mirrored());
         this.renderer.endShaded();
      }

      if (a.def.glow != null && (this.renderer.module == null || this.renderer.module.softGlow(look))) {
         GL20.glUseProgram(0);
         WeaponSettings s = look.settings;
         WeaponFx.drawGlowHere(
            a.def, s != null && s.accent != null ? s.accent.getColor(0.5F) : a.def.accentColor, s != null && s.glow != null ? s.glow.getFloat() : 1.0F
         );
      }

      if (a.def.arcs != null && time > 0.0F) {
         GL20.glUseProgram(0);
         WeaponSettings s = look.settings;
         WeaponFx.drawArcsHere(
            a.arcs(look.pixel),
            s != null && s.accent != null ? s.accent.getColor(0.5F) : a.def.accentColor,
            s != null && s.glow != null ? s.glow.getFloat() : 1.0F,
            2,
            (long)(time * 1000.0)
         );
      }
   }

   void finish(GlTarget dest, int x, int y, int w, int h, int workW, int workH) {
      this.finish(dest, x, y, w, h, workW, workH, 0.0F);
   }

   void finish(GlTarget dest, int x, int y, int w, int h, int workW, int workH, float outline) {
      WeaponShader.Program p = this.renderer.shaders().composite();
      if (p != null && dest != null) {
         dest.bind();
         GlStateManager.viewport(x, y, w, h);
         p.use();
         p.set2f("uTexel", 1.0F / this.work.width, 1.0F / this.work.height);
         p.set1f("uTaps", 1.0F);
         p.set1f("uBloom", 0.0F);
         p.set1f("uOutline", outline);
         p.set1f("uMosaic", 0.0F);
         GlStateManager.bindTexture(this.work.texture);
         GlStateManager.disableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.disableBlend();
         GlStateManager.matrixMode(5889);
         GlStateManager.loadIdentity();
         GlStateManager.matrixMode(5888);
         GlStateManager.loadIdentity();
         float u1 = (float)workW / this.work.width;
         float v1 = (float)workH / this.work.height;
         Tessellator t = Tessellator.getInstance();
         WorldRenderer wr = t.getWorldRenderer();
         wr.begin(7, DefaultVertexFormats.POSITION_TEX);
         wr.pos(-1.0, -1.0, 0.0).tex(0.0, 0.0).endVertex();
         wr.pos(1.0, -1.0, 0.0).tex(u1, 0.0).endVertex();
         wr.pos(1.0, 1.0, 0.0).tex(u1, v1).endVertex();
         wr.pos(-1.0, 1.0, 0.0).tex(0.0, v1).endVertex();
         t.draw();
      }
   }

   Mat4 fit(WeaponDef def, WeaponMesh mesh, Mat4 turn, float fov, float fill, Mat4 out) {
      float[] fArray;
      if (mesh != null) {
         float[] fArray2 = new float[]{mesh.min[0], mesh.min[1], mesh.min[2], mesh.max[0], mesh.max[1], 0.0F};
         fArray = fArray2;
         fArray2[5] = mesh.max[2];
      } else {
         fArray = def.bounds;
      }

      return this.fit(fArray, turn, fov, 1.0F, fill, out);
   }

   Mat4 fit(float[] b, Mat4 turn, float fov, float aspect, float fill, Mat4 out) {
      float minX = Float.MAX_VALUE;
      float minY = Float.MAX_VALUE;
      float maxX = -Float.MAX_VALUE;
      float maxY = -Float.MAX_VALUE;
      float minZ = Float.MAX_VALUE;
      float maxZ = -Float.MAX_VALUE;

      for (int k = 0; k < 8; k++) {
         float x = (k & 1) == 0 ? b[0] : b[3];
         float y = (k & 2) == 0 ? b[1] : b[4];
         float z = (k & 4) == 0 ? b[2] : b[5];
         turn.transform(x, y, z, this.c);
         minX = Math.min(minX, this.c[0]);
         maxX = Math.max(maxX, this.c[0]);
         minY = Math.min(minY, this.c[1]);
         maxY = Math.max(maxY, this.c[1]);
         minZ = Math.min(minZ, this.c[2]);
         maxZ = Math.max(maxZ, this.c[2]);
      }

      float half = Math.max((maxX - minX) / Math.max(0.1F, aspect), maxY - minY) / 2.0F / fill;
      float dist = half / (float)Math.tan(Math.toRadians(fov) / 2.0) + (maxZ - minZ) / 2.0F;
      out.identity();
      out.translate(-(minX + maxX) / 2.0F, -(minY + maxY) / 2.0F, -dist - (minZ + maxZ) / 2.0F);
      out.mul(turn);
      return out;
   }

   void release() {
      GlTarget.delete(this.work);
      this.work = null;
   }
}
