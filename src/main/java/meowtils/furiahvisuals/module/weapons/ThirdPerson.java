package meowtils.furiahvisuals.module.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.render.GlSave;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class ThirdPerson {
   private static final float PI = (float) Math.PI;
   private final WeaponsModule module;
   private final WeaponRenderer renderer;
   private final WeaponLight light = new WeaponLight();
   private final GlSave save = new GlSave();
   private final Mat4 fist = new Mat4();
   private final Mat4 weapon = new Mat4();
   private final Mat4 arm = new Mat4();
   private final Mat4 tmp = new Mat4();
   private final float[] v = new float[4];
   private final float[] d = new float[3];
   private final float[] y = new float[3];
   private final float[] x = new float[3];
   private final float[] z = new float[3];
   private boolean inCopy;
   private final Map<String, float[]> eyePoses = new HashMap<>();
   private final float[] eyePixel = new float[4];
   private final float[] eyeDir = new float[3];
   private final WeakHashMap<Entity, float[]> chains = new WeakHashMap<>();
   private static final float BOW_ARM_IN = 0.2F;
   private static final float BOW_OUT = 2.0F;
   private static final float NOCK_FRONT = 4.0F;
   private float posedBowPull = -1.0F;
   private Entity posedBowFor;
   private final Mat4 bowPx = new Mat4();
   private final Mat4 armTurn = new Mat4();
   private final float[] nock = new float[4];
   private final float[] arrowV = new float[3];
   private final float[] lookV = new float[3];
   private final float[] upV = new float[3];
   private final float[] bowTip = new float[3];
   private final float[] bowEdge = new float[3];
   private final float[] aimV = new float[3];
   private static final float[] RIGHT_FIST_2H = new float[]{-1.2F, 6.6F, -7.2F};
   private static final float SLACK = 2.2F;
   private final Mat4 stance = new Mat4();
   private final Mat4 turn = new Mat4();
   private WeaponDef stanceDef;
   private final float[] tipV = new float[3];
   private final float[] edgeV = new float[3];
   private final float[] grip = new float[4];

   ThirdPerson(WeaponsModule module, WeaponRenderer renderer) {
      this.module = module;
      this.renderer = renderer;
   }

   WeaponDef weaponOf(Entity entity) {
      if (!(entity instanceof EntityPlayer)) {
         return null;
      } else {
         EntityPlayer p = (EntityPlayer)entity;
         Minecraft mc = Minecraft.getMinecraft();
         if (p == mc.thePlayer
            || this.module.otherPlayers.get()
               && mc.getRenderViewEntity() != null
               && !(p.getDistanceSqToEntity(mc.getRenderViewEntity()) > this.module.otherDistance.get() * this.module.otherDistance.get())) {
            ItemStack held = p.getHeldItem();
            if (held == null) {
               return null;
            } else {
               WeaponDef def = this.module.weaponFor(held);
               return p.isUsingItem() && WeaponsModule.ownUseAnimation(held, def) ? null : def;
            }
         } else {
            return null;
         }
      }
   }

   private void eyeOf(WeaponRenderer.Look look, WeaponDef def, AbstractClientPlayer player, float pt) {
      float head = player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * pt;
      float body = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * pt;
      float yaw = (float)Math.toRadians(MathHelper.wrapAngleTo180_float(head - body));
      float pitch = (float)Math.toRadians(player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * pt);
      float bx = -MathHelper.cos(pitch) * MathHelper.sin(yaw);
      float bz;
      float by;
      float mx = this.weapon.m[0] * bx
         + this.weapon.m[1] * (by = MathHelper.sin(pitch))
         + this.weapon.m[2] * (bz = -MathHelper.cos(pitch) * MathHelper.cos(yaw));
      float mz;
      float my;
      float l = (float)Math.sqrt(
         mx * mx
            + (my = this.weapon.m[4] * bx + this.weapon.m[5] * by + this.weapon.m[6] * bz) * my
            + (mz = this.weapon.m[8] * bx + this.weapon.m[9] * by + this.weapon.m[10] * bz) * mz
      );
      this.eyeDir[0] = l < 1.0E-6F ? 0.0F : mx / l;
      this.eyeDir[1] = l < 1.0E-6F ? 0.0F : my / l;
      this.eyeDir[2] = l < 1.0E-6F ? 1.0F : mz / l;
      this.eyePoses.clear();
      EyeMotion.still(def.eye, this.eyeDir, this.module.time(), player.getEntityId() * 31, this.eyePoses, this.eyePixel);
      look.poses = this.eyePoses;
      look.eye = this.eyePixel;
   }

   private float chainPhase(EntityPlayer p) {
      float now = this.module.time();
      float[] c = this.chains.get(p);
      if (c == null) {
         c = new float[]{p.getEntityId() * 0.37F % 1.0F, 0.0F, now};
         this.chains.put(p, c);
      }

      float dt = Math.max(0.0F, Math.min(0.1F, now - c[2]));
      c[2] = now;
      c[1] = p.isSwingInProgress ? 1.0F : Math.max(0.0F, c[1] - dt / 0.6F);
      c[0] = (c[0] + (0.12F + 4.48F * c[1]) * dt) % 1000.0F;
      return c[0];
   }

   private boolean dual(Entity entity, WeaponDef def) {
      return def != null && this.module.dualWield.get() && !def.twoHanded;
   }

   void pose(ModelBiped model, Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks) {
      WeaponDef def = this.weaponOf(entity);
      if (def != null) {
         EntityPlayer p = (EntityPlayer)entity;
         boolean me = p == Minecraft.getMinecraft().thePlayer;
         float inspect = me && this.module.inspecting3p() ? this.module.inspectWeight3p() : 0.0F;
         float bowPull = def.isBow() ? bowPull(p, ageInTicks - (float)Math.floor(ageInTicks)) : -1.0F;
         this.poseArms(
            model,
            def,
            model.heldItemRight == 3,
            entity.isSprinting(),
            this.dual(entity, def),
            "slim".equals(((AbstractClientPlayer)entity).getSkinType()),
            limbSwing,
            limbSwingAmount,
            inspect,
            bowPull
         );
         this.posedBowFor = entity;
      }
   }

   static float bowPull(EntityPlayer p, float pt) {
      return p.isUsingItem() && WeaponRule.isBow(p.getItemInUse()) ? BowDraw.charge(72000.0F - (p.getItemInUseCount() - pt + 1.0F)) : -1.0F;
   }

   void poseArms(
      ModelBiped model,
      WeaponDef def,
      boolean blocking,
      boolean sprint,
      boolean dual,
      boolean slim,
      float limbSwing,
      float limbSwingAmount,
      float inspect,
      float bowPull
   ) {
      boolean vanilla = def.isVanilla();
      ModelRenderer r = model.bipedRightArm;
      ModelRenderer l = model.bipedLeftArm;
      this.posedBowPull = -1.0F;
      if (def.isBow()) {
         this.bowArms(model, def, slim, bowPull, sprint, limbSwing, limbSwingAmount);
      } else if (!vanilla) {
         if (def.twoHanded) {
            this.stance(model, def, slim, blocking, sprint, model.swingProgress, limbSwing, limbSwingAmount, inspect);
         } else {
            r.rotateAngleX -= 0.12F;
            if (blocking) {
               r.rotateAngleY = -0.55F;
            }

            if (inspect > 0.0F) {
               r.rotateAngleX = r.rotateAngleX + (-1.15F - r.rotateAngleX) * inspect;
               r.rotateAngleY = r.rotateAngleY + (-0.35F - r.rotateAngleY) * inspect;
            }
         }
      }

      if (dual) {
         l.rotateAngleX = l.rotateAngleX * 0.5F - (float) (Math.PI / 10) - 0.12F;
      }

      if (model instanceof ModelPlayer) {
         ModelBase.copyModelAngles(r, ((ModelPlayer)model).bipedRightArmwear);
         ModelBase.copyModelAngles(l, ((ModelPlayer)model).bipedLeftArmwear);
      }
   }

   void copyPose(ModelBiped armor, Entity entity) {
      if (this.weaponOf(entity) != null) {
         Render r = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(entity);
         if (r instanceof RenderPlayer) {
            ModelPlayer main = ((RenderPlayer)r).getMainModel();
            ModelBase.copyModelAngles(main.bipedRightArm, armor.bipedRightArm);
            ModelBase.copyModelAngles(main.bipedLeftArm, armor.bipedLeftArm);
            armor.bipedBody.rotateAngleY = main.bipedBody.rotateAngleY;
         }
      }
   }

   static float bowAim(float pull) {
      return pull <= 0.0F ? 0.0F : smooth01(Math.min(1.0F, pull * 4.0F));
   }

   private void bowArms(ModelBiped model, WeaponDef def, boolean slim, float pull, boolean sprint, float limbSwing, float limbSwingAmount) {
      ModelRenderer r = model.bipedRightArm;
      ModelRenderer l = model.bipedLeftArm;
      ModelRenderer head = model.bipedHead;
      if (model.aimedBow) {
         float aimX = (float) (-Math.PI / 2) + head.rotateAngleX;
         r.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount * 0.5F
            - (float) (Math.PI / 10)
            + (r.rotateAngleX - aimX);
         l.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * limbSwingAmount + (l.rotateAngleX - aimX);
         r.rotateAngleY = 0.0F;
         l.rotateAngleY = 0.0F;
      }

      l.rotateAngleX = l.rotateAngleX * 0.5F - (float) (Math.PI / 10) * (sprint ? 0.3F : 1.0F);
      float k = bowAim(pull);
      if (!(k <= 0.0F)) {
         float lx = (float) (-Math.PI / 2) + head.rotateAngleX;
         float ly = 0.2F + head.rotateAngleY;
         l.rotateAngleX = l.rotateAngleX + (lx - l.rotateAngleX) * k;
         l.rotateAngleY = l.rotateAngleY + (ly - l.rotateAngleY) * k;
         l.rotateAngleZ = l.rotateAngleZ + (0.0F - l.rotateAngleZ) * k;
         WeaponDef.Bow bow = def.bow;
         this.bowMatrix(model, def, this.module.settingsOf(def), slim, k, this.bowPx);
         this.bowPx.transform(bow.nock[0], bow.nock[1], bow.nock[2], this.nock);
         this.bowPx.direction(1.0F, 0.0F, 0.0F, this.arrowV);
         lookOf(head, this.lookV);
         float along = Mat4.dot(this.arrowV, this.lookV) * bow.draw;
         float front = this.nock[0] * this.lookV[0] + this.nock[1] * this.lookV[1] + this.nock[2] * this.lookV[2];
         float shown;
         this.posedBowPull = shown = along > 0.5F ? Math.max(0.0F, Math.min(Math.max(0.0F, pull), (front - 4.0F) / along)) : 0.0F;
         float back = bow.draw * shown;
         float nx = this.nock[0] - this.arrowV[0] * back;
         float ny = this.nock[1] - this.arrowV[1] * back;
         float nz = this.nock[2] - this.arrowV[2] * back;
         float ax = r.rotateAngleX;
         float ay = r.rotateAngleY;
         float az = r.rotateAngleZ;
         float sx = r.rotationPointX;
         float sy = r.rotationPointY;
         float sz = r.rotationPointZ;
         this.reach(r, true, slim, sx, sy, sz, nx, ny, nz);
         r.rotateAngleX = ax + (r.rotateAngleX - ax) * k;
         r.rotateAngleY = ay + MathHelper.wrapAngleTo180_float((float)Math.toDegrees(r.rotateAngleY - ay)) / 180.0F * (float) Math.PI * k;
         r.rotateAngleZ = az + (r.rotateAngleZ - az) * k;
         r.rotationPointX = sx + (r.rotationPointX - sx) * k;
         r.rotationPointY = sy + (r.rotationPointY - sy) * k;
         r.rotationPointZ = sz + (r.rotationPointZ - sz) * k;
      }
   }

   private static void lookOf(ModelRenderer head, float[] out) {
      float cx = MathHelper.cos(head.rotateAngleX);
      float sxv = MathHelper.sin(head.rotateAngleX);
      float cy = MathHelper.cos(head.rotateAngleY);
      float syv = MathHelper.sin(head.rotateAngleY);
      out[0] = -syv * cx;
      out[1] = sxv;
      out[2] = -cy * cx;
   }

   private static void upOf(ModelRenderer head, float[] out) {
      float cx = MathHelper.cos(head.rotateAngleX);
      float sxv = MathHelper.sin(head.rotateAngleX);
      float cy = MathHelper.cos(head.rotateAngleY);
      float syv = MathHelper.sin(head.rotateAngleY);
      out[0] = -syv * sxv;
      out[1] = -cx;
      out[2] = -cy * sxv;
   }

   private void bowMatrix(ModelBiped model, WeaponDef def, WeaponSettings s, boolean slim, float aim, Mat4 out) {
      armMatrix(model.bipedLeftArm, out);
      if (model.isSneak) {
         out.translate(0.0F, 3.25F, 0.0F);
      }

      out.scale(16.0F, 16.0F, 16.0F);
      this.placeBow(model, def, s, slim, aim);
      out.mul(this.weapon);
   }

   static float shoulderY(boolean slim) {
      return slim ? 2.5F : 2.0F;
   }

   static void resetShoulders(ModelBiped model, boolean slim) {
      model.bipedRightArm.rotationPointY = shoulderY(slim);
      model.bipedLeftArm.rotationPointY = shoulderY(slim);
   }

   private void stance(
      ModelBiped model, WeaponDef def, boolean slim, boolean blocking, boolean sprint, float swing, float limbSwing, float limbSwingAmount, float inspect
   ) {
      WeaponSettings s = this.module.settingsOf(def);
      WeaponDef.Hold hold = def.thirdPerson;
      System.arraycopy(hold.tip, 0, this.tipV, 0, 3);
      System.arraycopy(hold.edge, 0, this.edgeV, 0, 3);
      float fx = RIGHT_FIST_2H[0] + hold.pos[0] * 16.0F;
      float fy = RIGHT_FIST_2H[1] + hold.pos[1] * 16.0F;
      float fz = RIGHT_FIST_2H[2] + hold.pos[2] * 16.0F;
      float twist = (float)Math.toRadians(hold.twist);
      boolean onShoulder = hold.shoulderRest && !blocking;
      if (onShoulder) {
         sprint = false;
         twist = 0.0F;
      }

      if (blocking) {
         set(this.tipV, 1.0F, -0.2F, -0.28F);
         set(this.edgeV, 0.12F, -0.72F, 0.68F);
         fx = -3.4F;
         fy = 4.8F;
         fz = -7.6F;
         twist *= 0.5F;
      } else if (sprint) {
         set(this.tipV, 0.42F, -0.3F, -0.86F);
         set(this.edgeV, 0.0F, 0.95F, -0.3F);
         fx = -2.6F;
         fy = 8.8F;
         fz = -5.4F;
         twist *= 0.6F;
      }

      if (model.isSneak) {
         fy++;
         fz--;
      }

      fy += MathHelper.cos(limbSwing * 0.6662F * 2.0F) * 0.35F * limbSwingAmount;
      if (inspect > 0.0F) {
         fx += (-1.6F - fx) * inspect;
         fy += (3.6F - fy) * inspect;
         fz += (-6.8F - fz) * inspect;
         this.tipV[0] = this.tipV[0] + (0.3F - this.tipV[0]) * inspect;
         this.tipV[1] = this.tipV[1] + (-0.92F - this.tipV[1]) * inspect;
         this.tipV[2] = this.tipV[2] + (-0.25F - this.tipV[2]) * inspect;
      }

      float pitch = 0.0F;
      float yaw = 0.0F;
      if (swing > 0.0F) {
         float wind = smooth01(swing / 0.25F);
         float strike = smooth01((swing - 0.22F) / 0.3F);
         float back = smooth01((swing - 0.55F) / 0.45F);
         pitch = -0.75F * wind + 1.5F * strike - 0.75F * back;
         yaw = -0.45F * (strike - back);
         twist += 0.3F * (strike - back);
      }

      float sy = shoulderY(slim);
      this.stance.identity();
      if (pitch != 0.0F || yaw != 0.0F) {
         this.stance.translate(0.0F, sy, 0.0F);
         this.stance.rotate((float)Math.toDegrees(yaw), 0.0F, 1.0F, 0.0F);
         this.stance.rotate((float)Math.toDegrees(pitch), 1.0F, 0.0F, 0.0F);
         this.stance.translate(0.0F, -sy, 0.0F);
      }

      this.stance.translate(fx, fy, fz);
      Rig.basis(this.tipV, this.edgeV, this.y, this.x, this.z);
      this.turn.frame(this.x, this.y, this.z, 0.0F, 0.0F, 0.0F);
      this.stance.mul(this.turn);
      Rig.gripToFist(def.right, 16.0F * this.tpScale(def, s), this.stance);
      this.stanceDef = def;
      model.bipedBody.rotateAngleY = twist;
      float c = MathHelper.cos(twist) * 5.0F;
      float sn = MathHelper.sin(twist) * 5.0F;
      this.stance.transform(def.right.x, def.right.y, def.right.z, this.grip);
      this.reach(model.bipedRightArm, true, slim, -c, sy, sn, this.grip[0], this.grip[1], this.grip[2]);
      float mx = this.v[0] - this.grip[0];
      float my = this.v[1] - this.grip[1];
      float mz = this.v[2] - this.grip[2];
      if (mx != 0.0F || my != 0.0F || mz != 0.0F) {
         this.tmp.identity().translate(mx, my, mz);
         this.stance.premul(this.tmp);
      }

      if (def.left != null && (!onShoulder || inspect > 0.5F)) {
         this.stance.transform(def.left.x, def.left.y, def.left.z, this.grip);
         this.reach(model.bipedLeftArm, false, slim, c, sy, -sn, this.grip[0], this.grip[1], this.grip[2]);
      }
   }

   private void reach(ModelRenderer part, boolean right, boolean slim, float sx, float sy, float sz, float tx, float ty, float tz) {
      part.rotationPointX = sx;
      part.rotationPointY = sy;
      part.rotationPointZ = sz;
      this.d[0] = tx - sx;
      this.d[1] = ty - sy;
      this.d[2] = tz - sz;
      Mat4.normalize(this.d);
      float ax = -((float)Math.acos(Math.max(-1.0F, Math.min(1.0F, this.d[1]))));
      float sa = MathHelper.sin(ax);
      float ay = Math.abs(sa) < 1.0E-4F ? 0.0F : (float)Math.atan2(this.d[0] / sa, this.d[2] / sa);
      part.rotateAngleX = ax;
      part.rotateAngleY = ay;
      part.rotateAngleZ = 0.0F;
      armMatrix(part, this.arm);
      this.arm.transform(ArmRenderer.fistX(slim, right), 8.0F, 0.0F, this.v);
      float mx = tx - this.v[0];
      float my = ty - this.v[1];
      float mz = tz - this.v[2];
      float m = (float)Math.sqrt(mx * mx + my * my + mz * mz);
      if (m > 2.2F) {
         float k = 2.2F / m;
         mx *= k;
         my *= k;
         mz *= k;
      }

      part.rotationPointX += mx;
      part.rotationPointY += my;
      part.rotationPointZ += mz;
      this.v[0] = this.v[0] + mx;
      this.v[1] = this.v[1] + my;
      this.v[2] = this.v[2] + mz;
   }

   private static void set(float[] a, float x, float y, float z) {
      a[0] = x;
      a[1] = y;
      a[2] = z;
   }

   private static float smooth01(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return t * t * (3.0F - 2.0F * t);
   }

   private boolean stanceMatrix(WeaponDef def, boolean sneaking, Mat4 out) {
      if (this.stanceDef != def) {
         return false;
      } else {
         out.identity();
         if (sneaking) {
            out.translate(0.0F, 0.2F, 0.0F);
         }

         out.scale(0.0625F, 0.0625F, 0.0625F);
         out.mul(this.stance);
         return true;
      }
   }

   static void armMatrix(ModelRenderer part, Mat4 out) {
      out.identity();
      out.translate(part.rotationPointX, part.rotationPointY, part.rotationPointZ);
      out.rotate((float)Math.toDegrees(part.rotateAngleZ), 0.0F, 0.0F, 1.0F);
      out.rotate((float)Math.toDegrees(part.rotateAngleY), 0.0F, 1.0F, 0.0F);
      out.rotate((float)Math.toDegrees(part.rotateAngleX), 1.0F, 0.0F, 0.0F);
   }

   private void fistMatrix(WeaponDef def, WeaponSettings s, boolean slim, boolean right, boolean blocks, Mat4 arm, Mat4 out) {
      WeaponDef.Hold hold = holdOf(def, false);
      float unit = blocks ? 0.0625F : 1.0F;
      out.set(arm);
      out.translate((ArmRenderer.fistX(slim, right) + hold.pos[0] * 16.0F) * unit, (8.0F + hold.pos[1] * 16.0F) * unit, hold.pos[2] * 16.0F * unit);
      Rig.basis(hold.tip, hold.edge, this.y, this.x, this.z);
      this.tmp.frame(this.x, this.y, this.z, 0.0F, 0.0F, 0.0F);
      out.mul(this.tmp);
   }

   private static WeaponDef.Hold holdOf(WeaponDef def, boolean guard) {
      return def.thirdPerson;
   }

   private float tpScale(WeaponDef def, WeaponSettings s) {
      float k = s != null && s.sizeThird != null ? s.sizeThird.getFloat() : 1.0F;
      return holdOf(def, false).scale * k;
   }

   boolean drawHeld(ModelBiped model, EntityLivingBase entity, float pt) {
      WeaponDef def = this.weaponOf(entity);
      if (def == null) {
         return false;
      } else if (def.isVanilla()) {
         return false;
      } else {
         WeaponAssets a = WeaponAssets.of(def, WeaponAssets.Tier.FULL);
         a.request(this.module.textureSize());
         if (!a.ready()) {
            return false;
         } else {
            boolean timing = WeaponsModule.timingForTest;
            long started = 0L;
            if (timing) {
               started = System.nanoTime();
            }

            this.drawWeapon(model, (AbstractClientPlayer)entity, def, a, pt, !def.isBow());
            if (timing) {
               WeaponsModule.noteThirdPerson(System.nanoTime() - started);
            }

            WeaponDef left;
            if (this.dual(entity, def) && (left = this.module.leftWeaponOf(def)) != null && !left.isVanilla()) {
               WeaponAssets la = WeaponAssets.of(left, WeaponAssets.Tier.FULL);
               la.request(this.module.textureSize());
               if (la.ready()) {
                  this.drawWeapon(model, (AbstractClientPlayer)entity, left, la, pt, false);
               }
            }

            return true;
         }
      }
   }

   void drawVanillaCopy(ModelBiped model, EntityLivingBase entity) {
      if (!this.inCopy) {
         WeaponDef def = this.weaponOf(entity);
         if (def != null && def.isVanilla() && this.dual(entity, def)) {
            ItemStack stack = entity.getHeldItem();
            if (stack != null) {
               this.inCopy = true;
               GlStateManager.pushMatrix();

               try {
                  model.bipedLeftArm.postRender(0.0625F);
                  GlStateManager.translate(0.0625F, 0.4375F, 0.0625F);
                  if (entity.isSneaking()) {
                     GlStateManager.translate(0.0F, 0.203125F, 0.0F);
                  }

                  GlStateManager.scale(-1.0F, 1.0F, 1.0F);
                  GL11.glFrontFace(2304);
                  Minecraft.getMinecraft().getItemRenderer().renderItem(entity, stack, TransformType.THIRD_PERSON);
               } finally {
                  GL11.glFrontFace(2305);
                  GlStateManager.popMatrix();
                  this.inCopy = false;
               }
            }
         }
      }
   }

   private WeaponShader.Program beamProgram() {
      return !WeaponsModule.simpleShading() && this.renderer.shaders().supported() ? this.renderer.shaders().beam() : null;
   }

   private void drawWeapon(ModelBiped model, AbstractClientPlayer player, WeaponDef def, WeaponAssets a, float pt, boolean right) {
      Minecraft mc = Minecraft.getMinecraft();
      WeaponSettings s = this.module.settingsOf(def);
      boolean slim = "slim".equals(player.getSkinType());
      boolean me = player == mc.thePlayer;
      ModelRenderer part = right ? model.bipedRightArm : model.bipedLeftArm;
      Entity viewer = mc.getRenderViewEntity();
      double dist = viewer == null ? 0.0 : Math.sqrt(player.getDistanceSqToEntity(viewer));
      float near;
      int lod = dist < (near = this.module.lodDistance.getFloat()) ? 0 : (dist < near * 2.5 ? 1 : 2);
      if (me && mc.gameSettings.thirdPersonView == 0) {
         lod = 1;
      }

      GlStateManager.pushMatrix();
      this.save.saveQuick();
      float bowPull = def.isBow() ? bowPull(player, pt) : -1.0F;

      try {
         if (def.isBow()) {
            part.postRender(0.0625F);
            if (player.isSneaking()) {
               GlStateManager.translate(0.0F, 0.203125F, 0.0F);
            }

            this.placeBow(model, def, s, slim, bowAim(bowPull));
         } else if (!def.twoHanded || !this.stanceMatrix(def, player.isSneaking(), this.weapon)) {
            part.postRender(0.0625F);
            if (player.isSneaking()) {
               GlStateManager.translate(0.0F, 0.203125F, 0.0F);
            }

            this.placeWeapon(def, s, slim, right, me && right && this.module.inspecting3p() ? this.module.inspectPose3p() : null);
         }

         GlStateManager.multMatrix(this.weapon.buffer());
         WeaponRenderer.Look look = new WeaponRenderer.Look().set(s);
         look.finish = this.module.ruleFinish(player.getHeldItem(), def);
         look.pixel = this.module.pixelFor(player.getHeldItem(), def);
         look.enchant = this.module.enchantMode(player.getHeldItem());
         if (bowPull >= 0.0F) {
            look.bowPull = this.posedBowFor == player ? Math.max(0.0F, this.posedBowPull) : bowPull;
            look.bowBend = bowPull;
            boolean mirrored = look.bowNocked = bowPull >= 0.06F;
         }

         if (def.heatIdle > 0.0F) {
            look.heat = Math.min(1.0F, def.heatIdle + def.heatSwing * (float)Math.sin(Math.PI * player.getSwingProgress(pt)));
         }

         if (def.engine) {
            float var26 = look.chainPhase = me ? this.module.myChainPhase() : this.chainPhase(player);
         }

         if (def.eye != null) {
            this.eyeOf(look, def, player, pt);
         }

         boolean mirrored = this.weapon.mirrored();
         if (!WeaponsModule.simpleShading() && this.renderer.shaders().supported() && this.renderer.shaders().weapon() != null) {
            float pitch = viewer == null ? 0.0F : viewer.prevRotationPitch + (viewer.rotationPitch - viewer.prevRotationPitch) * pt;
            float yaw = viewer == null ? 0.0F : viewer.prevRotationYaw + (viewer.rotationYaw - viewer.prevRotationYaw) * pt;
            if (mc.gameSettings.thirdPersonView == 2) {
               yaw += 180.0F;
               pitch = -pitch;
            }

            this.light.updateEntity(mc, player, pt, pitch, yaw);
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
            if (this.renderer.beginShaded(this.light, this.save.fog(), this.module.time())) {
               this.renderer.draw(a, lod, look, WeaponRenderer.Mode.SHADED, mirrored);
               this.renderer.endShaded();
            }
         } else {
            this.renderer.setTime(this.module.time());
            this.renderer.draw(a, Math.max(1, lod), look, WeaponRenderer.Mode.SIMPLE, mirrored);
         }

         if (def.glow != null && this.module.softGlow(look)) {
            GL20.glUseProgram(0);
            WeaponFx.drawGlowHere(
               def, s.accent != null ? s.accent.getColor(0.5F) : def.accentColor, s.glow != null ? s.glow.getFloat() : 1.0F, this.beamProgram()
            );
         }

         if (def.arcs != null) {
            GL20.glUseProgram(0);
            WeaponFx.drawArcsHere(
               a.arcs(look.pixel),
               s.accent != null ? s.accent.getColor(0.5F) : def.accentColor,
               s.glow != null ? s.glow.getFloat() : 1.0F,
               player.getEntityId() * 7 + (right ? 0 : 3),
               this.beamProgram(),
               GameClock.millis()
            );
         }
      } finally {
         this.save.restore();
         GlStateManager.popMatrix();
      }
   }

   boolean drawWorldItem(Entity entity, ItemStack stack, boolean frame, float pt) {
      if (!this.module.dropped.get()) {
         return false;
      } else {
         WeaponDef def = this.module.weaponFor(stack);
         if (def != null && !def.isVanilla()) {
            WeaponAssets a = WeaponAssets.of(def, WeaponAssets.Tier.FULL);
            a.request(this.module.textureSize());
            if (!a.ready()) {
               return false;
            } else {
               Minecraft mc = Minecraft.getMinecraft();
               WeaponSettings s = this.module.settingsOf(def);
               Entity viewer = mc.getRenderViewEntity();
               double dist = viewer == null ? 0.0 : Math.sqrt(entity.getDistanceSqToEntity(viewer));
               int lod = dist < this.module.lodDistance.getFloat() ? 1 : 2;
               float[] b = def.bounds;
               float longest = Math.max(b[3] - b[0], Math.max(b[4] - b[1], b[5] - b[2]));
               float k = 0.95F / Math.max(0.001F, longest);
               GlStateManager.pushMatrix();
               this.save.saveQuick();

               try {
                  if (frame) {
                     GlStateManager.rotate(-45.0F, 0.0F, 0.0F, 1.0F);
                  }

                  GlStateManager.scale(k, k, k);
                  GlStateManager.translate(-(b[0] + b[3]) / 2.0F, -(b[1] + b[4]) / 2.0F, -(b[2] + b[5]) / 2.0F);
                  WeaponRenderer.Look look = new WeaponRenderer.Look().set(s);
                  look.finish = this.module.ruleFinish(stack, def);
                  look.pixel = this.module.pixelFor(stack, def);
                  look.enchant = this.module.enchantMode(stack);
                  if (!WeaponsModule.simpleShading() && this.renderer.shaders().supported() && this.renderer.shaders().weapon() != null) {
                     float pitch = viewer == null ? 0.0F : viewer.prevRotationPitch + (viewer.rotationPitch - viewer.prevRotationPitch) * pt;
                     float yaw = viewer == null ? 0.0F : viewer.prevRotationYaw + (viewer.rotationYaw - viewer.prevRotationYaw) * pt;
                     this.light.updateEntity(mc, entity, pt, pitch, yaw);
                     GlStateManager.disableLighting();
                     GlStateManager.enableDepth();
                     GlStateManager.depthMask(true);
                     GlStateManager.disableCull();
                     GlStateManager.enableBlend();
                     GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
                     if (this.renderer.beginShaded(this.light, this.save.fog(), this.module.time())) {
                        this.renderer.draw(a, lod, look, WeaponRenderer.Mode.SHADED, false);
                        this.renderer.endShaded();
                     }
                  } else {
                     GlStateManager.enableLighting();
                     this.renderer.setTime(this.module.time());
                     this.renderer.draw(a, 2, look, WeaponRenderer.Mode.SIMPLE, false);
                  }

                  if (def.glow != null && this.module.softGlow(look)) {
                     GL20.glUseProgram(0);
                     WeaponFx.drawGlowHere(
                        def, s.accent != null ? s.accent.getColor(0.5F) : def.accentColor, s.glow != null ? s.glow.getFloat() : 1.0F, this.beamProgram()
                     );
                  }

                  if (def.arcs != null) {
                     GL20.glUseProgram(0);
                     WeaponFx.drawArcsHere(
                        a.arcs(look.pixel),
                        s.accent != null ? s.accent.getColor(0.5F) : def.accentColor,
                        s.glow != null ? s.glow.getFloat() : 1.0F,
                        entity.getEntityId() * 13,
                        this.beamProgram(),
                        GameClock.millis()
                     );
                  }
               } finally {
                  this.save.restore();
                  GlStateManager.popMatrix();
               }

               return true;
            }
         } else {
            return false;
         }
      }
   }

   private void placeBow(ModelBiped model, WeaponDef def, WeaponSettings s, boolean slim, float aim) {
      set(this.bowTip, 0.0F, -0.82F, -0.57F);
      set(this.bowEdge, 0.0F, 0.57F, -0.82F);
      if (aim > 0.0F) {
         ModelRenderer l = model.bipedLeftArm;
         this.armTurn.identity();
         this.armTurn.rotate((float)Math.toDegrees(l.rotateAngleZ), 0.0F, 0.0F, 1.0F);
         this.armTurn.rotate((float)Math.toDegrees(l.rotateAngleY), 0.0F, 1.0F, 0.0F);
         this.armTurn.rotate((float)Math.toDegrees(l.rotateAngleX), 1.0F, 0.0F, 0.0F);
         lookOf(model.bipedHead, this.aimV);
         this.toArm(this.aimV);

         for (int i = 0; i < 3; i++) {
            this.bowEdge[i] = this.bowEdge[i] + (this.aimV[i] - this.bowEdge[i]) * aim;
         }

         upOf(model.bipedHead, this.aimV);
         this.toArm(this.aimV);

         for (int var9 = 0; var9 < 3; var9++) {
            this.bowTip[var9] = this.bowTip[var9] + (this.aimV[var9] - this.bowTip[var9]) * aim;
         }
      }

      WeaponDef.Hold hold = holdOf(def, false);
      this.fist.identity();
      this.fist.translate((ArmRenderer.fistX(slim, false) - hold.pos[0] * 16.0F) / 16.0F, (8.0F + hold.pos[1] * 16.0F + 2.0F * aim) / 16.0F, hold.pos[2]);
      Rig.basis(this.bowTip, this.bowEdge, this.y, this.x, this.z);
      this.z[0] = -this.z[0];
      this.z[1] = -this.z[1];
      this.z[2] = -this.z[2];
      this.tmp.frame(this.x, this.y, this.z, 0.0F, 0.0F, 0.0F);
      this.fist.mul(this.tmp);
      this.weapon.set(this.fist);
      Rig.gripToFist(def.right, this.tpScale(def, s), this.weapon);
   }

   private void toArm(float[] v3) {
      float[] m = this.armTurn.m;
      float a = m[0] * v3[0] + m[1] * v3[1] + m[2] * v3[2];
      float b = m[4] * v3[0] + m[5] * v3[1] + m[6] * v3[2];
      float c = m[8] * v3[0] + m[9] * v3[1] + m[10] * v3[2];
      v3[0] = a;
      v3[1] = b;
      v3[2] = c;
   }

   private void placeWeapon(WeaponDef def, WeaponSettings s, boolean slim, boolean right, Pose pose) {
      this.arm.identity();
      this.fistMatrix(def, s, slim, true, true, this.arm, this.fist);
      this.weapon.set(this.fist);
      if (pose != null) {
         this.weapon.translate(pose.wPos[0], pose.wPos[1], pose.wPos[2]);
         this.weapon.rotate(pose.wRot);
      }

      Rig.gripToFist(def.right, this.tpScale(def, s), this.weapon);
      if (!right) {
         float cx = ArmRenderer.fistX(slim, true) / 16.0F;
         this.weapon.m[12] = this.weapon.m[12] - cx;
         Mat4 mirror = new Mat4().scale(-1.0F, 1.0F, 1.0F);
         this.weapon.premul(mirror);
         this.weapon.mul(mirror);
         this.weapon.m[12] = this.weapon.m[12] + ArmRenderer.fistX(slim, false) / 16.0F;
      }
   }

   void drawStudio(
      ModelBiped model,
      WeaponDef def,
      WeaponAssets a,
      boolean slim,
      boolean sneaking,
      boolean right,
      WeaponLight studio,
      float time,
      Boolean pixel,
      float bowPull
   ) {
      WeaponSettings s = this.module.settingsOf(def);
      ModelRenderer part = right && !def.isBow() ? model.bipedRightArm : model.bipedLeftArm;
      GlStateManager.pushMatrix();

      try {
         if (def.isBow()) {
            part.postRender(0.0625F);
            if (sneaking) {
               GlStateManager.translate(0.0F, 0.203125F, 0.0F);
            }

            this.placeBow(model, def, s, slim, bowAim(bowPull));
         } else if (!def.twoHanded || !this.stanceMatrix(def, sneaking, this.weapon)) {
            part.postRender(0.0625F);
            if (sneaking) {
               GlStateManager.translate(0.0F, 0.203125F, 0.0F);
            }

            this.placeWeapon(def, s, slim, right, null);
         }

         GlStateManager.multMatrix(this.weapon.buffer());
         WeaponRenderer.Look look = new WeaponRenderer.Look().set(s);
         look.chainPhase = time * 0.12F;
         look.pixel = this.module.pixelOf(def, pixel);
         if (bowPull >= 0.0F) {
            look.bowPull = Math.max(0.0F, this.posedBowPull >= 0.0F ? this.posedBowPull : bowPull);
            look.bowBend = bowPull;
            look.bowNocked = bowPull >= 0.06F;
         }

         boolean mirrored = this.weapon.mirrored();
         if (!WeaponsModule.simpleShading() && this.renderer.shaders().supported() && this.renderer.shaders().weapon() != null) {
            GlStateManager.disableLighting();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
            if (this.renderer.beginShaded(studio, false, time)) {
               this.renderer.draw(a, 0, look, WeaponRenderer.Mode.SHADED, mirrored);
               this.renderer.endShaded();
            }

            GL20.glUseProgram(0);
         } else {
            this.renderer.setTime(time);
            this.renderer.draw(a, 0, look, WeaponRenderer.Mode.SIMPLE, mirrored);
         }

         if (def.glow != null && this.module.softGlow(look)) {
            WeaponFx.drawGlowHere(def, s.accent != null ? s.accent.getColor(0.5F) : def.accentColor, s.glow != null ? s.glow.getFloat() : 1.0F);
         }

         if (def.arcs != null) {
            GL20.glUseProgram(0);
            WeaponFx.drawArcsHere(
               a.arcs(look.pixel),
               s.accent != null ? s.accent.getColor(0.5F) : def.accentColor,
               s.glow != null ? s.glow.getFloat() : 1.0F,
               1,
               (long)(time * 1000.0)
            );
         }
      } finally {
         GlStateManager.popMatrix();
      }
   }
}
