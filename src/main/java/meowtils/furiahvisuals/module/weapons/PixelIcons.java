package meowtils.furiahvisuals.module.weapons;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import meowtils.furiahvisuals.fx.FxFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class PixelIcons {
   private static final int CELL = 32;
   private static final int COLS = 16;
   private static final int CELLS = 256;
   private static final int SIZE = 512;
   private int texture = -1;
   private final Map<String, Integer> cells = new LinkedHashMap<>(16, 0.75F, true);
   private final ByteBuffer upload = BufferUtils.createByteBuffer(4096);

   boolean draw(WeaponShader.Program p, WeaponDef def, WeaponSettings s, WeaponOptions.Finish ruleFinish, int x, int y, float zLevel, int enchant, float time) {
      Minecraft mc = Minecraft.getMinecraft();
      int scale = guiScale(mc);
      int slotPx = 16 * scale;
      boolean small = slotPx % 32 != 0;
      int n = small ? 16 : 32;
      int k = Math.max(1, slotPx / n);
      int cell = this.cellOf(def, s, ruleFinish, small);
      if (cell < 0) {
         return false;
      } else {
         float size = (float)(n * k) / scale;
         float x0 = x + (16.0F - size) / 2.0F;
         float y0 = y + (16.0F - size) / 2.0F;
         int cx = cell % 16 * 32;
         int cy = cell / 16 * 32;
         float u0 = cx / 512.0F;
         float v0 = cy / 512.0F;
         float u1 = (cx + n) / 512.0F;
         float v1 = (cy + n) / 512.0F;
         int previous = GL11.glGetInteger(35725);
         GlStateManager.bindTexture(this.texture);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
         GlStateManager.disableLighting();
         GlStateManager.disableAlpha();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         p.use();
         p.set4f("uCell", u0, v0, u1, v1);
         p.set1f("uSheen", enchant);
         p.set1f("uTime", time);
         p.set3f("uSheenColor", 0.55F, 0.32F, 1.0F);
         p.set1f("uPixels", n);

         try {
            Tessellator t = Tessellator.getInstance();
            WorldRenderer wr = t.getWorldRenderer();
            float z = zLevel + 50.0F;
            wr.begin(7, DefaultVertexFormats.POSITION_TEX);
            wr.pos(x0, y0 + size, z).tex(u0, v1).endVertex();
            wr.pos(x0 + size, y0 + size, z).tex(u1, v1).endVertex();
            wr.pos(x0 + size, y0, z).tex(u1, v0).endVertex();
            wr.pos(x0, y0, z).tex(u0, v0).endVertex();
            t.draw();
         } finally {
            p.set1f("uPixels", 0.0F);
            GL20.glUseProgram(previous);
            GlStateManager.blendFunc(770, 771);
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
         }

         return true;
      }
   }

   float[] iconOf(WeaponDef def, WeaponSettings s, WeaponOptions.Finish finish) {
      int cell = this.cellOf(def, s, finish, false);
      if (cell < 0) {
         return null;
      } else {
         int cx = cell % 16 * 32;
         int cy = cell / 16 * 32;
         return new float[]{this.texture, cx / 512.0F, cy / 512.0F, (cx + 32) / 512.0F, (cy + 32) / 512.0F};
      }
   }

   private int cellOf(WeaponDef def, WeaponSettings s, WeaponOptions.Finish ruleFinish, boolean small) {
      WeaponAssets px = WeaponAssets.of(def, WeaponAssets.Tier.PIXEL);
      px.request(0, true);
      PixelModel m = px.pixelModel;
      if (px.ready() && m != null) {
         byte[] icon = small ? m.icon16 : m.icon;
         if (icon == null) {
            return -1;
         } else {
            WeaponOptions.Finish finish = ruleFinish != null ? ruleFinish : (s != null ? s.effectiveFinish() : WeaponOptions.Finish.of(def.finish));
            if (finish == null) {
               finish = WeaponOptions.Finish.STEEL_POLISHED;
            }

            String key;
            Integer cell;
            if ((cell = this.cells.get(key = def.id + "|" + System.identityHashCode(m) + "|" + (s != null ? s.version() : 0) + "|" + finish + "|" + small))
               != null) {
               return cell;
            } else {
               int c;
               if (this.cells.size() >= 256) {
                  Iterator<Entry<String, Integer>> it = this.cells.entrySet().iterator();
                  c = it.next().getValue();
                  it.remove();
               } else {
                  c = this.cells.size();
               }

               PixelPalette pal = px.palette(finish);
               pal.update(def, m, s, finish, FxFrame.currentFrame());
               int n = small ? 16 : 32;
               float wear = s != null && s.wear != null ? s.wear.getFloat() / 100.0F : 0.0F;
               int seed = def.id.hashCode() & 1023;
               ((Buffer)this.upload).clear();

               for (int y = 0; y < 32; y++) {
                  for (int x = 0; x < 32; x++) {
                     int argb = 0;
                     int slot;
                     if (x < n && y < n && (slot = icon[y * n + x] & 255) != 0) {
                        boolean edge = empty(icon, n, x - 1, y) || empty(icon, n, x + 1, y) || empty(icon, n, x, y - 1) || empty(icon, n, x, y + 1);
                        float along = Math.max(0.0F, Math.min(0.999F, (x + (n - 1 - y)) / (2.0F * (n - 1))));
                        argb = 0xFF000000 | pal.color(slot, x, n - 1 - y, 0, 2, edge, true, along, wear, seed);
                     }

                     this.upload.put((byte)(argb >> 16)).put((byte)(argb >> 8)).put((byte)argb).put((byte)(argb >>> 24));
                  }
               }

               ((Buffer)this.upload).flip();
               if (this.texture < 0) {
                  this.texture = TextureUtil.glGenTextures();
                  GlStateManager.bindTexture(this.texture);
                  GL11.glTexParameteri(3553, 10241, 9728);
                  GL11.glTexParameteri(3553, 10240, 9728);
                  GL11.glTexParameteri(3553, 10242, 10496);
                  GL11.glTexParameteri(3553, 10243, 10496);
                  GL11.glTexImage2D(3553, 0, 32856, 512, 512, 0, 6408, 5121, (ByteBuffer)null);
               }

               GlStateManager.bindTexture(this.texture);
               GL11.glTexSubImage2D(3553, 0, c % 16 * 32, c / 16 * 32, 32, 32, 6408, 5121, this.upload);
               this.cells.put(key, c);
               return c;
            }
         }
      } else {
         return -1;
      }
   }

   private static boolean empty(byte[] icon, int n, int x, int y) {
      return x < 0 || y < 0 || x >= n || y >= n || icon[y * n + x] == 0;
   }

   static int guiScale(Minecraft mc) {
      int want = mc.gameSettings.guiScale == 0 ? 1000 : mc.gameSettings.guiScale;
      int scale = 1;

      while (scale < want && mc.displayWidth / (scale + 1) >= 320 && mc.displayHeight / (scale + 1) >= 240) {
         scale++;
      }

      if (mc.isUnicode() && scale % 2 != 0 && scale != 1) {
         scale--;
      }

      return scale;
   }

   void clear() {
      this.cells.clear();
   }

   void release() {
      if (this.texture >= 0) {
         TextureUtil.deleteTexture(this.texture);
         this.texture = -1;
      }

      this.cells.clear();
   }
}
