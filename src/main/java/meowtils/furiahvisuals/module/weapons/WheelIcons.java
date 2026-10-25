package meowtils.furiahvisuals.module.weapons;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.GlSave;
import meowtils.furiahvisuals.render.GlTarget;
import meowtils.furiahvisuals.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class WheelIcons {
   static final int COLS = 8;
   static final int ROWS = 4;
   static final int CELLS = 32;
   static final float LIVE_FPS = 12.0F;
   static final float LIT_FPS = 30.0F;
   private static final int PER_FRAME = 2;
   private static final float FOV = 18.0F;
   private static final float[] ITEM_BOX = new float[]{-0.12F, -0.62F, -0.12F, 0.12F, 0.62F, 0.12F};
   private final WeaponsModule module;
   private final WeaponStudio studio;
   private final GlSave save = new GlSave();
   private GlTarget atlas;
   private GlTarget big;
   private int cell;
   private int bigSize;
   private final WeaponDef[] cellDef = new WeaponDef[32];
   private final WeaponOptions.Finish[] cellFinish = new WeaponOptions.Finish[32];
   private final boolean[] cellPixel = new boolean[32];
   private final int[] cellLook = new int[32];
   private final long[] cellDrawn = new long[32];
   private WeaponDef bigDef;
   private WeaponOptions.Finish bigFinish;
   private boolean bigPixel;
   private int bigLook;
   private long bigDrawn;
   private boolean failed;
   private final Mat4 turn = new Mat4();
   private final Mat4 model = new Mat4();
   private final Mat4 proj = new Mat4();
   private final float[] box = new float[6];
   private final float[] frame = new float[6];
   private final boolean[] due = new boolean[32];
   private final Map<WeaponSettings, int[]> looks = new IdentityHashMap<>();
   long lastNanos;
   long lastSaveNanos;
   int lastDrawn;
   private static final ItemStack DIAMOND_SWORD = new ItemStack(Items.diamond_sword);

   WheelIcons(WeaponsModule module, WeaponRenderer renderer) {
      this.module = module;
      this.studio = new WeaponStudio(renderer);
   }

   boolean failed() {
      return this.failed;
   }

   int atlasTexture() {
      return this.atlas == null ? -1 : this.atlas.texture;
   }

   int bigTexture() {
      return this.big == null ? -1 : this.big.texture;
   }

   float[] cellUv(int c, float[] out) {
      float aw = this.cell * 8;
      float ah = this.cell * 4;
      int cx = c % 8 * this.cell;
      int cy = c / 8 * this.cell;
      out[0] = cx / aw;
      out[2] = (cx + this.cell) / aw;
      out[1] = (cy + this.cell) / ah;
      out[3] = cy / ah;
      return out;
   }

   private int look(WeaponDef def) {
      WeaponSettings s = this.module.settingsOf(def);
      int[] v = this.looks.get(s);
      if (v != null && def.isVanilla()) {
         return v[0] * 31 + System.identityHashCode(this.vanillaStack().getItem());
      } else {
         if (v == null) {
            final int[] counter = new int[]{0};

            for (Setting x : s.all) {
               if ("Look".equals(x.getGroup())) {
                  x.addListener(new Runnable() {
                     @Override
                     public void run() {
                        counter[0]++;
                     }
                  });
               }
            }

            this.looks.put(s, counter);
            v = counter;
         }

         return v[0];
      }
   }

   private boolean holds(int c, WheelIcons.Request r, int look) {
      return this.cellDef[c] == r.def && this.cellFinish[c] == r.finish && this.cellPixel[c] == r.pixel && this.cellLook[c] == look;
   }

   void prepare(WheelIcons.Request[] slots, int count, WheelIcons.Request center, int cellPx, int bigPx, boolean live) {
      long start = System.nanoTime();
      this.lastDrawn = 0;
      this.lastSaveNanos = 0L;
      if (!this.failed) {
         cellPx = Math.max(48, Math.min(256, (cellPx + 15) / 16 * 16));
         bigPx = Math.max(64, Math.min(512, (bigPx + 15) / 16 * 16));
         long now = ClientClock.nanos();
         long unlit = 83333333L;
         long lit = 33333333L;
         boolean resized = this.atlas != null && this.cell != cellPx;
         boolean any = false;
         int fresh = 4;

         for (int i = 0; i < count; i++) {
            WheelIcons.Request r = slots[i];
            this.due[i] = false;
            if (r.def == null) {
               r.ready = false;
            } else {
               int look = this.look(r.def);
               boolean same;
               r.ready = same = !resized && this.atlas != null && this.holds(r.cell, r, look);
               if (!same && fresh > 0) {
                  this.due[i] = true;
                  fresh--;
                  any = true;
               } else if (same && live && r.lit && now - this.cellDrawn[r.cell] >= lit) {
                  this.due[i] = true;
                  any = true;
               }
            }
         }

         if (live) {
            for (int budget = 2; budget > 0; budget--) {
               int oldest = -1;
               long age = unlit;

               for (int ix = 0; ix < count; ix++) {
                  WheelIcons.Request r = slots[ix];
                  if (!this.due[ix] && r.ready && !r.lit && now - this.cellDrawn[r.cell] >= age) {
                     age = now - this.cellDrawn[r.cell];
                     oldest = ix;
                  }
               }

               if (oldest < 0) {
                  break;
               }

               this.due[oldest] = true;
               any = true;
            }
         }

         boolean centerDue = false;
         if (center != null && center.def != null) {
            int look = this.look(center.def);
            boolean same;
            center.ready = same = this.big != null
               && this.bigSize == bigPx
               && this.bigDef == center.def
               && this.bigFinish == center.finish
               && this.bigPixel == center.pixel
               && this.bigLook == look;
            centerDue = !same || now - this.bigDrawn >= lit;
         }

         if (!any && !centerDue) {
            this.lastNanos = System.nanoTime() - start;
         } else {
            GlStateManager.matrixMode(5889);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.pushMatrix();
            long saving = System.nanoTime();
            this.save.save();
            this.lastSaveNanos = this.lastSaveNanos + (System.nanoTime() - saving);

            try {
               try {
                  GL20.glUseProgram(0);
                  GL11.glDisable(3089);
                  if (resized) {
                     GlTarget.delete(this.atlas);
                     this.atlas = null;
                  }

                  if (this.atlas == null) {
                     this.atlas = GlTarget.create(cellPx * 8, cellPx * 4, 32856, 6408, true, false);
                     if (this.atlas == null) {
                        this.failed = true;
                        return;
                     }

                     this.cell = cellPx;
                     Arrays.fill(this.cellDef, null);
                     this.atlas.bind();
                     GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
                     GlStateManager.clear(16384);
                  }

                  for (int ixx = 0; ixx < count; ixx++) {
                     if (this.due[ixx]) {
                        WheelIcons.Request r = slots[ixx];
                        if (this.drawOne(r, live ? r.angle : 28.0F, cellPx, this.atlas, r.cell % 8 * cellPx, r.cell / 8 * cellPx)) {
                           this.cellDef[r.cell] = r.def;
                           this.cellFinish[r.cell] = r.finish;
                           this.cellPixel[r.cell] = r.pixel;
                           this.cellLook[r.cell] = this.look(r.def);
                           this.cellDrawn[r.cell] = now;
                           r.ready = true;
                           this.lastDrawn++;
                        }
                     }
                  }

                  if (centerDue) {
                     if (this.big != null && this.bigSize != bigPx) {
                        GlTarget.delete(this.big);
                        this.big = null;
                     }

                     if (this.big == null) {
                        this.big = GlTarget.create(bigPx, bigPx, 32856, 6408, true, false);
                        this.bigSize = bigPx;
                        this.bigDef = null;
                     }

                     if (this.big != null && this.drawOne(center, center.angle, bigPx, this.big, 0, 0)) {
                        this.bigDef = center.def;
                        this.bigFinish = center.finish;
                        this.bigPixel = center.pixel;
                        this.bigLook = this.look(center.def);
                        this.bigDrawn = now;
                        center.ready = true;
                        this.lastDrawn++;
                     }

                     return;
                  }
               } catch (RuntimeException | LinkageError var27) {
                  this.failed = true;
                  FuriahsVisuals.LOG.warn("The weapon wheel's pictures are off: they could not be drawn", var27);
               }
            } finally {
               saving = System.nanoTime();
               this.save.restore();
               GlStateManager.matrixMode(5889);
               GlStateManager.popMatrix();
               GlStateManager.matrixMode(5888);
               GlStateManager.popMatrix();
               this.lastSaveNanos = this.lastSaveNanos + (System.nanoTime() - saving);
               this.lastNanos = System.nanoTime() - start;
            }
         }
      }
   }

   private boolean drawOne(WheelIcons.Request r, float angle, int px, GlTarget dest, int x, int y) {
      int w = px * 2;
      if (r.def.isVanilla()) {
         if (!this.studio.begin(w, w)) {
            return false;
         } else {
            float rock = (float)Math.sin(Math.toRadians(angle)) * 38.0F;
            this.turn.identity().rotate(-45.0F, 0.0F, 0.0F, 1.0F);
            this.studio.fit(ITEM_BOX, this.turn, 18.0F, 1.0F, 0.95F, this.model);
            this.model.rotate(rock, 0.0F, 1.0F, 0.0F);
            this.model.rotate(45.0F, 0.0F, 0.0F, 1.0F);
            this.drawItem(this.vanillaStack(), 18.0F);
            this.studio.finish(dest, x, y, px, px, w, w);
            return true;
         }
      } else {
         WeaponAssets a = WeaponAssets.of(r.def, WeaponAssets.Tier.CARD);
         a.request(256);
         if (!a.ready()) {
            return false;
         } else if (r.pixel && a.pixelTwin().loading()) {
            a.pixelTwin().request(0);
            return false;
         } else if (a.frame(1, this.frame) && this.studio.begin(w, w)) {
            float cx = (this.frame[0] + this.frame[3]) / 2.0F;
            float cy = (this.frame[1] + this.frame[4]) / 2.0F;
            float cz = (this.frame[2] + this.frame[5]) / 2.0F;
            float rx = (this.frame[3] - this.frame[0]) / 2.0F;
            float rz = (this.frame[5] - this.frame[2]) / 2.0F;
            float rad = (float)Math.sqrt(rx * rx + rz * rz);
            this.box[0] = -rad;
            this.box[1] = this.frame[1] - cy;
            this.box[2] = -rad;
            this.box[3] = rad;
            this.box[4] = this.frame[4] - cy;
            this.box[5] = rad;
            this.turn.identity().rotate(-45.0F, 0.0F, 0.0F, 1.0F);
            this.studio.fit(this.box, this.turn, 18.0F, 1.0F, 0.97F, this.model);
            this.model.rotate(angle, 0.0F, 1.0F, 0.0F);
            this.model.translate(-cx, -cy, -cz);
            this.studio.light.studio(0.0F, 180.0F);
            WeaponRenderer.Look look = new WeaponRenderer.Look().set(this.module.settingsOf(r.def));
            look.finish = r.finish;
            look.pixel = r.pixel;
            look.chainPhase = this.module.clientTime() * 0.12F;
            this.studio.draw(a, look, 1, this.model, 18.0F, w, w, this.module.clientTime());
            this.studio.finish(dest, x, y, px, px, w, w);
            return true;
         } else {
            return false;
         }
      }
   }

   private ItemStack vanillaStack() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      ItemStack held = p == null ? null : p.getHeldItem();
      WeaponRule r = this.module.wheelContent.targetRule();
      return held != null && r != null && r.matches(held) ? held : DIAMOND_SWORD;
   }

   private void drawItem(ItemStack stack, float fov) {
      Minecraft mc = Minecraft.getMinecraft();
      GL20.glUseProgram(0);
      this.proj.perspective(fov, 1.0F, 0.05F, 50.0F);
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.multMatrix(this.proj.buffer());
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      RenderHelper.enableGUIStandardItemLighting();
      GlStateManager.multMatrix(this.model.buffer());
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GlStateManager.disableTexture2D();
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      GlStateManager.enableRescaleNormal();
      GlStateManager.enableDepth();
      GlStateManager.depthMask(true);
      GlStateManager.enableAlpha();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      boolean before = WeaponsModule.itemsAsThemselves;
      WeaponsModule.itemsAsThemselves = true;

      try {
         mc.getRenderItem().renderItem(stack, TransformType.NONE);
      } finally {
         WeaponsModule.itemsAsThemselves = before;
         RenderHelper.disableStandardItemLighting();
      }
   }

   void idle() {
   }

   void release() {
      GlTarget.delete(this.atlas);
      GlTarget.delete(this.big);
      this.atlas = null;
      this.big = null;
      Arrays.fill(this.cellDef, null);
      this.bigDef = null;
      this.studio.release();
   }

   static final class Request {
      WeaponDef def;
      WeaponOptions.Finish finish;
      boolean pixel;
      float angle;
      boolean lit;
      int cell;
      boolean ready;
   }
}
