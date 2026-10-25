package meowtils.furiahvisuals.core;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import meowtils.furiahvisuals.fx.Terrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

public final class TestDummy {
   static final int ENTITY_ID = -4343;
   private static final UUID PROFILE_ID = UUID.nameUUIDFromBytes("furiahsvisuals:test-dummy".getBytes(StandardCharsets.UTF_8));
   private static final int JUMP_EVERY_TICKS = 40;
   private static EntityOtherPlayerMP dummy;
   private static volatile boolean jumping;
   private static double x;
   private static double y;
   private static double z;
   private static double vx;
   private static double vy;
   private static double vz;
   private static float yaw;
   private static boolean onGround;
   private static boolean sendsGround = true;
   private static int ticks;
   private static long runUntilMs;
   private static double runCx;
   private static double runCz;
   private static double runRadius;
   private static double runAngle;
   private static boolean runJumps;
   private static final double RUN_SPEED = 0.27999999999999997;
   private static final int RUN_JUMP_EVERY_TICKS = 60;
   private static long respawnAtMs;
   private static final long RESPAWN_MS = 3000L;
   private static long respawnMs = 3000L;
   private static String name = "Dummy";
   private static double sentX;
   private static double sentY;
   private static double sentZ;
   private static boolean sentOnGround;

   private TestDummy() {
   }

   public static EntityOtherPlayerMP get() {
      Minecraft mc = Minecraft.getMinecraft();
      EntityOtherPlayerMP d = dummy;
      return d != null && !d.isDead && mc.theWorld != null && d.worldObj == mc.theWorld && mc.theWorld.getEntityByID(-4343) == d
         ? d
         : null;
   }

   public static void setJumping(boolean on) {
      jumping = on;
   }

   public static boolean isJumping() {
      return jumping;
   }

   static void setSendsGround(boolean on) {
      sendsGround = on;
      EntityOtherPlayerMP d = get();
      if (d != null) {
         d.onGround = onGround && sendsGround;
      }
   }

   static void spawn(Minecraft mc) {
      if (place(mc)) {
         FuriahsVisuals.chat("Test dummy spawned. Hit it to try the effects; §f/furiah dummy remove§7 removes it.");
      }
   }

   public static boolean spawnQuietly(Minecraft mc) {
      return place(mc);
   }

   private static boolean place(Minecraft mc) {
      EntityPlayerSP p = mc.thePlayer;
      if (p == null || mc.theWorld == null) {
         return false;
      } else if (!mc.isSingleplayer()) {
         FuriahsVisuals.chat("The test dummy only works in singleplayer.");
         return false;
      } else {
         remove(mc, false);
         double yawRad = Math.toRadians(p.rotationYaw);
         x = p.posX - Math.sin(yawRad) * 3.0;
         z = p.posZ + Math.cos(yawRad) * 3.0;
         double ground = Terrain.surfaceAt(mc.theWorld, x, z, p.posY, 1.5, 6.0);
         y = Double.isNaN(ground) ? p.posY : ground;
         yaw = MathHelper.wrapAngleTo180_float(p.rotationYaw + 180.0F);
         create(mc);
         return true;
      }
   }

   private static void create(Minecraft mc) {
      vz = 0.0;
      vy = 0.0;
      vx = 0.0;
      onGround = true;
      ticks = 0;
      EntityOtherPlayerMP d = new EntityOtherPlayerMP(mc.theWorld, new GameProfile(PROFILE_ID, name));
      d.setPositionAndRotation(x, y, z, yaw, 0.0F);
      d.rotationYawHead = d.prevRotationYawHead = yaw;
      d.renderYawOffset = d.prevRenderYawOffset = yaw;
      d.onGround = sendsGround;
      setServerPosition(d);
      sentX = x;
      sentY = y;
      sentZ = z;
      sentOnGround = true;
      mc.theWorld.addEntityToWorld(-4343, d);
      dummy = d;
   }

   public static boolean runAround(Minecraft mc, long millis, boolean jumps) {
      EntityPlayerSP p = mc.thePlayer;
      if (p == null || mc.theWorld == null) {
         return false;
      } else if (!mc.isSingleplayer()) {
         FuriahsVisuals.chat("The test dummy only works in singleplayer.");
         return false;
      } else if (get() == null && !place(mc)) {
         return false;
      } else {
         runAround(p.posX, p.posZ, 4.0, millis, jumps);
         return true;
      }
   }

   static void runAround(double cx, double cz, double radius, long millis, boolean jumps) {
      runCx = cx;
      runCz = cz;
      runRadius = radius;
      runAngle = Math.atan2(x - cx, z - cz);
      runUntilMs = millis == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(1L, GameClock.millis() + millis);
      runJumps = jumps;
   }

   public static boolean isRunning() {
      return runUntilMs != 0L;
   }

   static void remove(Minecraft mc, boolean tell) {
      boolean wasDead = respawnAtMs != 0L;
      respawnAtMs = 0L;
      runUntilMs = 0L;
      if (dummy != null && mc.theWorld != null && mc.theWorld.getEntityByID(-4343) == dummy) {
         mc.theWorld.removeEntityFromWorld(-4343);
         if (tell) {
            FuriahsVisuals.chat("Test dummy removed.");
         }
      } else if (tell) {
         FuriahsVisuals.chat(wasDead ? "Test dummy removed (it will not come back)." : "There is no test dummy.");
      }

      dummy = null;
   }

   static void onWorldChange() {
      dummy = null;
      respawnAtMs = 0L;
      runUntilMs = 0L;
   }

   static void respawnAfterForTest(long ms) {
      respawnMs = ms > 0L ? ms : 3000L;
   }

   static void respawnNowForTest() {
      if (respawnAtMs != 0L) {
         respawnAtMs = Math.max(1L, GameClock.millis());
      }
   }

   static UUID profileId() {
      return PROFILE_ID;
   }

   static void nameForTest(String n) {
      name = n != null && !n.isEmpty() ? n : "Dummy";
   }

   static int ticksForTest() {
      return ticks;
   }

   public static boolean kill(Minecraft mc) {
      EntityOtherPlayerMP d = get();
      if (d != null && respawnAtMs == 0L) {
         d.setHealth(0.0F);
         vz = 0.0;
         vy = 0.0;
         vx = 0.0;
         respawnAtMs = Math.max(1L, GameClock.millis() + respawnMs);
         return true;
      } else {
         return false;
      }
   }

   public static boolean isDead() {
      return respawnAtMs != 0L;
   }

   private static void setServerPosition(EntityOtherPlayerMP d) {
      d.serverPosX = MathHelper.floor_double(x * 32.0);
      d.serverPosY = MathHelper.floor_double(y * 32.0);
      d.serverPosZ = MathHelper.floor_double(z * 32.0);
   }

   static void tick(Minecraft mc) {
      if (!mc.isGamePaused()) {
         if (respawnAtMs != 0L) {
            if (GameClock.millis() >= respawnAtMs) {
               respawnAtMs = 0L;
               if (mc.isSingleplayer() && mc.theWorld != null) {
                  if (mc.theWorld.getEntityByID(-4343) != null) {
                     mc.theWorld.removeEntityFromWorld(-4343);
                  }

                  create(mc);
               }
            }
         } else {
            EntityOtherPlayerMP d = get();
            if (d == null) {
               dummy = null;
            } else {
               if (jumping && onGround && ++ticks % 40 == 0) {
                  vy = 0.42;
                  onGround = false;
               }

               if (runUntilMs != 0L) {
                  if (GameClock.millis() >= runUntilMs) {
                     runUntilMs = 0L;
                     d.setSprinting(false);
                  } else {
                     double tx = runCx + Math.sin(runAngle = runAngle + 0.27999999999999997 / runRadius) * runRadius;
                     double tz = runCz + Math.cos(runAngle) * runRadius;
                     double dx = tx - x;
                     double dz = tz - z;
                     double l = Math.sqrt(dx * dx + dz * dz);
                     double step = Math.min(l, 0.44799999999999995);
                     vx = l > 1.0E-6 ? dx / l * step : 0.0;
                     vz = l > 1.0E-6 ? dz / l * step : 0.0;
                     if (l > 1.0E-6) {
                        yaw = (float)Math.toDegrees(Math.atan2(-vx, vz));
                        d.setRotationYawHead(yaw);
                     }

                     d.setSprinting(true);
                     if (runJumps && onGround && ticks % 60 == 0) {
                        vy = 0.42;
                        onGround = false;
                     }
                  }
               }

               double previousY = y;
               if (!onGround || vy > 0.0) {
                  y = y + vy;
                  vy = (vy - 0.08) * 0.98;
               }

               double ground;
               if (!Double.isNaN(ground = Terrain.surfaceAt(mc.theWorld, x = x + vx, z = z + vz, previousY + 0.05, 0.1, 40.0)) && y <= ground) {
                  y = ground;
                  vy = 0.0;
                  onGround = true;
               } else {
                  onGround = false;
               }

               float friction = onGround ? 0.546F : 0.91F;
               vx *= friction;
               vz *= friction;
               if (y < -64.0) {
                  remove(mc, false);
               } else {
                  boolean moved = Math.abs(x - sentX) > 0.001 || Math.abs(y - sentY) > 0.001 || Math.abs(z - sentZ) > 0.001;
                  if (moved || onGround != sentOnGround) {
                     d.setPositionAndRotation2(x, y, z, yaw, 0.0F, 3, false);
                     d.onGround = onGround && sendsGround;
                     setServerPosition(d);
                     sentX = x;
                     sentY = y;
                     sentZ = z;
                     sentOnGround = onGround;
                  }
               }
            }
         }
      }
   }

   static void simulateClick(Minecraft mc) {
      EntityOtherPlayerMP d = get();
      if (d != null && mc.thePlayer != null) {
         hit(mc, d);
      }
   }

   static void place(double px, double py, double pz, float facing) {
      EntityOtherPlayerMP d = get();
      if (d != null) {
         x = px;
         y = py;
         z = pz;
         yaw = facing;
         vz = 0.0;
         vy = 0.0;
         vx = 0.0;
         onGround = true;
         d.setPositionAndRotation(x, y, z, yaw, 0.0F);
         d.rotationYawHead = d.prevRotationYawHead = yaw;
         d.renderYawOffset = d.prevRenderYawOffset = yaw;
         d.setPositionAndRotation2(x, y, z, yaw, 0.0F, 1, false);
         setServerPosition(d);
      }
   }

   static boolean onMouse(Minecraft mc, int button, boolean buttonstate) {
      if (button == 0 && buttonstate && mc.currentScreen == null && mc.thePlayer != null) {
         EntityOtherPlayerMP d = get();
         MovingObjectPosition over = mc.objectMouseOver;
         if (d != null && over != null && over.entityHit == d) {
            hit(mc, d);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void hit(Minecraft mc, EntityOtherPlayerMP d) {
      if (respawnAtMs == 0L) {
         EntityPlayerSP p = mc.thePlayer;
         if (!p.isSwingInProgress || p.swingProgressInt >= 3 || p.swingProgressInt < 0) {
            p.swingProgressInt = -1;
            p.isSwingInProgress = true;
         }

         FuriahsVisuals.instance.onLocalAttack(p, d);
         if (d.hurtResistantTime <= d.maxHurtResistantTime / 2) {
            d.hurtResistantTime = d.maxHurtResistantTime;
            d.maxHurtTime = 10;
            d.hurtTime = 10;
            d.attackedAtYaw = 0.0F;
            d.limbSwingAmount = 1.5F;
            mc.theWorld
               .playSound(
                  d.posX,
                  d.posY,
                  d.posZ,
                  "game.player.hurt",
                  1.0F,
                  (d.getRNG().nextFloat() - d.getRNG().nextFloat()) * 0.2F + 1.0F,
                  false
               );
            double dx = p.posX - x;
            double dz = p.posZ - z;
            double len = Math.max(1.0E-4, Math.sqrt(dx * dx + dz * dz));
            vx = vx / 2.0 - dx / len * 0.3;
            vz = vz / 2.0 - dz / len * 0.3;
            vy = Math.min(0.36, vy / 2.0 + 0.36);
            if (p.isSprinting()) {
               double yawRad = Math.toRadians(p.rotationYaw);
               vx = vx + -Math.sin(yawRad) * 0.35;
               vz = vz + Math.cos(yawRad) * 0.35;
               vy += 0.05;
            }

            onGround = false;
         }
      }
   }
}
