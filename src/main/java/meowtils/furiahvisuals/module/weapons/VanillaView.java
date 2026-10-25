package meowtils.furiahvisuals.module.weapons;

import meowtils.furiahvisuals.fx.FxFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

final class VanillaView {
   static final float PIVOT_X = 0.5F;
   static final float PIVOT_Y = -0.38F;
   static final float PIVOT_Z = -0.72F;
   private final WeaponsModule module;
   private final ViewModels views;
   final ViewModels.Hand hand;
   private final Pose pose = new Pose();
   private long frame = -1L;
   private long lastNanos;
   private int lastSlot = -1;
   private Item lastItem;
   private boolean lastSwinging;
   private int lastSwingInt;
   private int depth;
   private final boolean[] pushedProjection = new boolean[4];
   private final boolean[] pushedModelview = new boolean[4];
   private boolean copy;
   private final Mat4 m = new Mat4();
   private final Mat4 proj = new Mat4();
   private final float[] q = new float[4];

   VanillaView(WeaponsModule module, ViewModels views) {
      this.module = module;
      this.views = views;
      this.hand = views.new Hand();
   }

   private boolean active(WeaponSettings s) {
      return this.hand.anim.inspecting()
         || this.hand.anim.inspectClip() != null
         || s.placement.get() != WeaponOptions.Placement.DEFAULT
         || s.fov.getFloat() != 70.0F
         || s.leftHanded.get()
         || s.weaponMotion != null && s.weaponMotion.get()
         || this.module.dualWield.get();
   }

   void head(Minecraft mc, ItemStack held, WeaponDef def, WeaponSettings s, float pt) {
      EntityPlayerSP p = mc.thePlayer;
      int level;
      if ((level = this.depth++) < this.pushedProjection.length) {
         this.pushedProjection[level] = false;
         this.pushedModelview[level] = false;
         long id;
         if (!this.copy && (id = FxFrame.currentFrame()) != this.frame) {
            this.frame = id;
            long now = this.views.nanos();
            float dt = this.lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastNanos) / 1.0E9F);
            this.lastNanos = now;
            this.update(p, held, def, s, pt, dt);
         }

         if (this.active(s)) {
            if (s.fov.getFloat() != 70.0F) {
               GlStateManager.matrixMode(5889);
               GlStateManager.pushMatrix();
               GlStateManager.loadIdentity();
               this.proj.perspective(s.fov.getFloat(), (float)mc.displayWidth / Math.max(1, mc.displayHeight), 0.05F, 400.0F);
               GlStateManager.multMatrix(this.proj.buffer());
               GlStateManager.matrixMode(5888);
               this.pushedProjection[level] = true;
            }

            GlStateManager.pushMatrix();
            this.pushedModelview[level] = true;
            this.transform(s, this.copy, this.m);
            GlStateManager.multMatrix(this.m.buffer());
            if (this.m.mirrored()) {
               GL11.glFrontFace(2304);
            }
         }
      }
   }

   void transform(WeaponSettings s, boolean copy, Mat4 out) {
      float[] o = ViewModels.placement(s);
      Motion mo = this.hand.motion;
      boolean motion = s.weaponMotion != null && s.weaponMotion.get();
      out.identity();
      if (s.leftHanded.get() != copy) {
         out.scale(-1.0F, 1.0F, 1.0F);
      }

      if (copy) {
         out.translate(0.06F, -0.03F, 0.0F);
      }

      float dx = o[0] + this.pose.rPos[0] + this.pose.wPos[0] + (motion ? mo.offset[0] : 0.0F);
      float dy = o[1] + this.pose.rPos[1] + this.pose.wPos[1] + (motion ? mo.offset[1] : 0.0F);
      float dz = o[2] + this.pose.rPos[2] + this.pose.wPos[2] + (motion ? mo.offset[2] : 0.0F);
      out.translate(0.5F + dx, -0.38F + dy, -0.72F + dz);
      out.rotate(this.pose.rRot);
      if (motion) {
         Mat4.euler(mo.pitch, mo.yaw, mo.roll, this.q);
         out.rotate(this.q);
      }

      out.rotate(o[4], 0.0F, 1.0F, 0.0F).rotate(o[3], 1.0F, 0.0F, 0.0F).rotate(o[5], 0.0F, 0.0F, 1.0F);
      out.rotate(this.pose.wRot);
      out.scale(o[6], o[6], o[6]);
      out.translate(-0.5F, 0.38F, 0.72F);
   }

   void previewStep(WeaponDef def, WeaponSettings s, float dt) {
      this.hand.set(def, s);
      if (s.weaponMotion != null && s.weaponMotion.get()) {
         this.hand.motion.update(null, 1.0F, dt, s, def.mass, false);
      }

      this.hand.anim.setHold(new float[]{0.5F, -0.38F, -0.72F}, new float[]{0.0F, 0.0F, 0.0F, 1.0F});
      this.hand.anim.update(dt, false, true, this.pose);
   }

   void tail(Minecraft mc, WeaponSettings s, float pt) {
      if (this.depth > 0) {
         int level;
         if ((level = --this.depth) < this.pushedProjection.length) {
            if (this.pushedModelview[level]) {
               GL11.glFrontFace(2305);
               GlStateManager.popMatrix();
               this.pushedModelview[level] = false;
            }

            if (this.pushedProjection[level]) {
               GlStateManager.matrixMode(5889);
               GlStateManager.popMatrix();
               GlStateManager.matrixMode(5888);
               this.pushedProjection[level] = false;
            }

            if (level == 0 && !this.copy && this.module.dualWield.get() && s.leftWeapon.get().equals("same")) {
               this.copy = true;

               try {
                  GlStateManager.pushMatrix();
                  mc.getItemRenderer().renderItemInFirstPerson(pt);
               } finally {
                  GlStateManager.popMatrix();
                  this.copy = false;
               }
            }
         }
      }
   }

   boolean drawingCopy() {
      return this.copy;
   }

   void updateOnly(Minecraft mc, ItemStack held, WeaponDef def, WeaponSettings s, float pt) {
      long id = FxFrame.currentFrame();
      if (id != this.frame && mc.thePlayer != null) {
         this.frame = id;
         long now = this.views.nanos();
         float dt = this.lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastNanos) / 1.0E9F);
         this.lastNanos = now;
         this.update(mc.thePlayer, held, def, s, pt, dt);
      }
   }

   Pose pose() {
      return this.pose;
   }

   private void update(EntityPlayerSP p, ItemStack held, WeaponDef def, WeaponSettings s, float pt, float dt) {
      this.hand.set(def, s);
      int slot = p.inventory.currentItem;
      Item item = held == null ? null : held.getItem();
      if (slot != this.lastSlot || item != this.lastItem) {
         this.hand.anim.reset();
         this.module.inspects.cancel(this.hand);
      }

      this.lastSlot = slot;
      this.lastItem = item;
      boolean swinging = p.isSwingInProgress;
      int si = p.swingProgressInt;
      if (swinging && (!this.lastSwinging || si < this.lastSwingInt)) {
         this.module.inspects.cancel(this.hand);
         this.hand.motion.attacked();
      }

      this.lastSwinging = swinging;
      this.lastSwingInt = si;
      if (p.isUsingItem()) {
         this.module.inspects.cancel(this.hand);
      }

      if (s.weaponMotion != null && s.weaponMotion.get()) {
         this.hand.motion.update(p, pt, dt, s, def.mass, false);
      }

      this.hand.anim.setHold(new float[]{0.5F, -0.38F, -0.72F}, new float[]{0.0F, 0.0F, 0.0F, 1.0F});
      this.hand.anim.update(dt, false, true, this.pose);
   }

   void reset() {
      this.hand.anim.reset();
      this.hand.motion.reset();
      this.lastItem = null;
      this.lastSlot = -1;
   }
}
