package meowtils.furiahvisuals.module.targetaura;

import java.util.List;
import meowtils.furiahvisuals.core.PlayerFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

final class TargetFinder {
   EntityOtherPlayerMP find(Minecraft mc, double range, double tolerance, boolean ignoreTeammates, boolean ignoreNpcs) {
      WorldClient world = mc.theWorld;
      Entity viewer = (Entity)(mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.thePlayer);
      if (world != null && viewer != null && mc.thePlayer != null) {
         Vec3 eye = viewer.getPositionEyes(1.0F);
         Vec3 look = viewer.getLook(1.0F);
         Vec3 end = eye.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range);
         double maxDistance = range;
         MovingObjectPosition block = world.rayTraceBlocks(eye, end, false, true, false);
         if (block != null && block.hitVec != null) {
            maxDistance = Math.min(range, eye.distanceTo(block.hitVec));
         }

         EntityOtherPlayerMP best = null;
         double bestDistance = maxDistance;
         List players = world.playerEntities;

         for (int i = 0; i < players.size(); i++) {
            EntityPlayer p = (EntityPlayer)players.get(i);
            EntityOtherPlayerMP other;
            if (p instanceof EntityOtherPlayerMP
               && p != viewer
               && isValidTarget(mc, other = (EntityOtherPlayerMP)p, ignoreTeammates, ignoreNpcs)
               && !(other.getDistanceSq(eye.xCoord, eye.yCoord, eye.zCoord) > (range + 4.0) * (range + 4.0))) {
               AxisAlignedBB box = other.getEntityBoundingBox().expand(tolerance, tolerance, tolerance);
               if (box.isVecInside(eye)) {
                  return other;
               }

               MovingObjectPosition hit = box.calculateIntercept(eye, end);
               double distance;
               if (hit != null && hit.hitVec != null && (distance = eye.distanceTo(hit.hitVec)) <= bestDistance) {
                  best = other;
                  bestDistance = distance;
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   static boolean isValidTarget(Minecraft mc, EntityOtherPlayerMP p, boolean ignoreTeammates, boolean ignoreNpcs) {
      if (p.isDead || p.getHealth() <= 0.0F || p.isInvisible() || p.isSpectator()) {
         return false;
      } else {
         return ignoreNpcs && isNpc(p) ? false : !ignoreTeammates || !isTeammate(mc, p);
      }
   }

   static boolean isNpc(EntityPlayer p) {
      return PlayerFilter.isNpc(p);
   }

   static boolean isTeammate(Minecraft mc, EntityPlayer other) {
      return PlayerFilter.isTeammate(mc, other);
   }
}
