package meowtils.furiahvisuals.module.hit;

import meowtils.furiahvisuals.fx.Basis;
import meowtils.furiahvisuals.fx.Rng;
import meowtils.furiahvisuals.fx.Terrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public final class HitMeasurer {
   private static final long SWIPE_NANOS = 60000000L;
   private static final int SAMPLES = 32;
   private final long[] lookTime = new long[32];
   private final float[] lookYaw = new float[32];
   private final float[] lookPitch = new float[32];
   private int head;
   private final Basis basis = new Basis();
   private final Rng rng = new Rng();
   public static final double MIN_SWIPE = 0.35;
   private final float[] swipe = new float[2];

   public void recordLook(EntityPlayerSP me, long now) {
      if (now - this.lookTime[this.head] < 4000000L) {
         this.lookYaw[this.head] = me.rotationYaw;
         this.lookPitch[this.head] = me.rotationPitch;
      } else {
         this.head = (this.head + 1) % 32;
         this.lookTime[this.head] = now;
         this.lookYaw[this.head] = me.rotationYaw;
         this.lookPitch[this.head] = me.rotationPitch;
      }
   }

   public HitContext measure(Minecraft mc, EntityPlayerSP me, Entity target, long now) {
      this.recordLook(me, now);
      HitContext hit = new HitContext();
      hit.target = target;
      hit.targetId = target.getEntityId();
      hit.time = now;
      hit.feetX = target.posX;
      hit.feetY = target.posY;
      hit.feetZ = target.posZ;
      hit.width = target.width;
      hit.height = target.height;
      Vec3 eye = me.getPositionEyes(1.0F);
      Vec3 look = me.getLook(1.0F);
      double border = target.getCollisionBorderSize();
      AxisAlignedBB box = target.getEntityBoundingBox().expand(border, border, border);
      Vec3 impact = null;
      MovingObjectPosition over = mc.objectMouseOver;
      if (over != null && over.entityHit == target && over.hitVec != null && !box.isVecInside(eye)) {
         impact = over.hitVec;
      }

      if (impact == null) {
         MovingObjectPosition m = box.calculateIntercept(eye, eye.addVector(look.xCoord * 8.0, look.yCoord * 8.0, look.zCoord * 8.0));
         impact = m != null && m.hitVec != null ? m.hitVec : closestPointOnBox(box, eye, look);
      }

      hit.offX = impact.xCoord - hit.feetX;
      hit.offY = impact.yCoord - hit.feetY;
      hit.offZ = impact.zCoord - hit.feetZ;
      double dx = impact.xCoord - eye.xCoord;
      double dy = impact.yCoord - eye.yCoord;
      double dz = impact.zCoord - eye.zCoord;
      double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (len < 1.0E-4) {
         dx = look.xCoord;
         dy = look.yCoord;
         dz = look.zCoord;
         len = Math.sqrt(dx * dx + dy * dy + dz * dz);
      }

      hit.dirX = dx / len;
      hit.dirY = dy / len;
      hit.dirZ = dz / len;
      Basis b = this.basis.set(hit.dirX, hit.dirY, hit.dirZ);
      hit.side = hit.offX * b.rx + hit.offZ * b.rz >= 0.0 ? 1 : -1;
      this.swipe(hit, now, me.ticksExisted);
      hit.crit = isCrit(me, target);
      double ground = Terrain.surfaceAt(mc.theWorld, target.posX, target.posZ, target.posY, 0.3, 6.0);
      hit.groundY = Double.isNaN(ground) ? target.posY - 6.0 : ground;
      return hit;
   }

   private static Vec3 closestPointOnBox(AxisAlignedBB box, Vec3 eye, Vec3 look) {
      double cx = (box.minX + box.maxX) / 2.0;
      double cy = (box.minY + box.maxY) / 2.0;
      double cz = (box.minZ + box.maxZ) / 2.0;
      double t = Math.max(
         0.0, (cx - eye.xCoord) * look.xCoord + (cy - eye.yCoord) * look.yCoord + (cz - eye.zCoord) * look.zCoord
      );
      double px = eye.xCoord + look.xCoord * t;
      double py = eye.yCoord + look.yCoord * t;
      double pz = eye.zCoord + look.zCoord * t;
      return new Vec3(
         Math.max(box.minX, Math.min(box.maxX, px)),
         Math.max(box.minY, Math.min(box.maxY, py)),
         Math.max(box.minZ, Math.min(box.maxZ, pz))
      );
   }

   public static boolean isCrit(EntityPlayerSP me, Entity target) {
      return me.fallDistance > 0.0F
         && !me.onGround
         && !me.isOnLadder()
         && !me.isInWater()
         && !me.isPotionActive(Potion.blindness)
         && me.ridingEntity == null
         && target instanceof EntityLivingBase;
   }

   public void screenSwipe(long now, float[] out) {
      float yawNow = this.lookYaw[this.head];
      float pitchNow = this.lookPitch[this.head];
      float dYaw = 0.0F;
      float dPitch = 0.0F;

      int j;
      for (int k = 1; k < 32 && this.lookTime[j = (this.head - k + 32) % 32] != 0L; k++) {
         dYaw = MathHelper.wrapAngleTo180_float(yawNow - this.lookYaw[j]);
         dPitch = pitchNow - this.lookPitch[j];
         if (now - this.lookTime[j] >= 60000000L) {
            break;
         }
      }

      out[0] = dYaw;
      out[1] = -dPitch;
   }

   private void swipe(HitContext hit, long now, int tick) {
      this.screenSwipe(now, this.swipe);
      double sx = this.swipe[0];
      double sy = this.swipe[1];
      double len = Math.sqrt(sx * sx + sy * sy);
      if (len < 0.35) {
         this.rng.seed(tick, hit.targetId);
         sx = this.rng.next() < 0.5F ? 1.0 : -1.0;
         sy = this.rng.next() < 0.5F ? 0.75 : -0.75;
         len = Math.sqrt(sx * sx + sy * sy);
      }

      double var12;
      hit.screenSwipeX = (float)(var12 = sx / len);
      double var13;
      hit.screenSwipeY = (float)(var13 = sy / len);
      Basis b = this.basis.set(hit.dirX, hit.dirY, hit.dirZ);
      hit.swipeX = b.rx * var12 + b.ux * var13;
      hit.swipeY = b.ry * var12 + b.uy * var13;
      hit.swipeZ = b.rz * var12 + b.uz * var13;
   }
}
