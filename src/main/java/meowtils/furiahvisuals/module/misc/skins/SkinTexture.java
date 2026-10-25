package meowtils.furiahvisuals.module.misc.skins;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;

final class SkinTexture extends AbstractTexture {
   private int[] pixels;
   private final int size;

   SkinTexture(int[] pixels, int size) {
      this.pixels = pixels;
      this.size = size;
   }

   public void loadTexture(IResourceManager manager) {
      int[] p = this.pixels;
      if (p != null) {
         int id = this.getGlTextureId();
         TextureUtil.allocateTexture(id, this.size, this.size);
         TextureUtil.uploadTexture(id, p, this.size, this.size);
         this.pixels = null;
      }
   }
}
