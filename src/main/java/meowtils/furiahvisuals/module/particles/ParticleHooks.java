package meowtils.furiahvisuals.module.particles;

import meowtils.furiahvisuals.core.GameClock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

public final class ParticleHooks {
   public static final int CRIT_ID = EnumParticleTypes.CRIT.getParticleID();
   public static final int MAGIC_ID = EnumParticleTypes.CRIT_MAGIC.getParticleID();
   static final int EMITTER_SEEN = 64;
   static ParticlesModule module;
   private static boolean nextEmitMine;
   private static int pendingEmitter;
   private static int emitterContext;
   private static boolean inMultiplier;
   private static boolean inReplace;
   private static boolean inBypass;
   private static EntityFX spawnResult;
   private static int rng = (int)System.nanoTime() | 1;
   private static boolean seenSpawnHead;
   private static boolean seenSpawnTail;
   private static boolean seenEmitHead;
   private static boolean seenEmitter;
   private static boolean seenDraw;
   private static boolean seenStart;
   private static boolean seenEnd;
   private static boolean seenBypass;
   private static boolean seenPrediction;
   private static boolean seenPacket;
   static final int[] BURSTS = new int[3];
   static final int[] SPAWNED = new int[3];
   static final int[] MINE_SPAWNED = new int[3];

   private ParticleHooks() {
   }

   static boolean active() {
      ParticlesModule m = module;
      return m != null && m.isActive();
   }

   static int kindOf(int particleId) {
      return particleId == CRIT_ID ? 1 : (particleId == MAGIC_ID ? 2 : 0);
   }

   static int kindOf(EnumParticleTypes type) {
      return type == EnumParticleTypes.CRIT ? 1 : (type == EnumParticleTypes.CRIT_MAGIC ? 2 : 0);
   }

   static int nextInt() {
      int x = rng;
      x ^= x << 13;
      x ^= x >>> 17;
      x ^= x << 5;
      rng = x;
      return x;
   }

   static float nextFloat() {
      return (nextInt() >>> 8) * 5.9604645E-8F;
   }

   public static boolean spawnHead(
      EffectRenderer self, int particleId, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, int[] parameters
   ) {
      if (!seenSpawnHead) {
         seenSpawnHead = true;
         HookStatus.seen("spawnHead");
      }

      int kind;
      if ((kind = kindOf(particleId)) != 0 && !inReplace) {
         ParticlesModule m = module;
         if (m == null || !m.isActive()) {
            return false;
         } else if (!m.shows(kind)) {
            spawnResult = null;
            return true;
         } else {
            float amount = m.amount(kind);
            if (amount < 0.999F && nextFloat() >= amount) {
               spawnResult = null;
               return true;
            } else {
               ParticlesModule.Replacement r = m.replacement.get();
               if (r == ParticlesModule.Replacement.NONE) {
                  return false;
               } else {
                  inReplace = true;

                  EntityFX fx;
                  try {
                     fx = self.spawnEffectParticle(r.particleId(), x, y, z, r.speedX(xSpeed), r.speedY(ySpeed), r.speedZ(zSpeed), r.parameters(parameters));
                  } finally {
                     inReplace = false;
                  }

                  if (fx != null) {
                     tag(fx, kind | 8);
                  }

                  spawnResult = fx;
                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   public static EntityFX takeSpawnResult() {
      EntityFX fx = spawnResult;
      spawnResult = null;
      return fx;
   }

   public static void spawnTail(int particleId, EntityFX fx) {
      if (!seenSpawnTail) {
         seenSpawnTail = true;
         HookStatus.seen("spawnTail");
      }

      if (fx != null && !inReplace) {
         int kind = kindOf(particleId);
         if (kind != 0 && active() && ParticleTag.tagOf(fx) == 0) {
            tag(fx, kind);
         }
      }
   }

   private static void tag(EntityFX fx, int bits) {
      ParticleTag t = ParticleTag.of(fx);
      int seed = nextInt();
      int tag = bits | emitterContext & 4;
      int var8;
      t.setFvTag(var8 = tag | module.pickImage(bits & 3, seed));
      t.setFvSeed(seed);
      t.setFvBorn(GameClock.nanos());
      int kind;
      int n = kind = bits & 3;
      SPAWNED[n]++;
      if ((var8 & 4) != 0) {
         MINE_SPAWNED[kind]++;
      }
   }

   public static boolean emitHead(EffectRenderer self, Entity entity, EnumParticleTypes type) {
      if (!seenEmitHead) {
         seenEmitHead = true;
         HookStatus.seen("emitHead");
      }

      boolean mine = nextEmitMine;
      nextEmitMine = false;
      int kind = kindOf(type);
      ParticlesModule m = module;
      if (kind == 0 || entity == null || m == null || !m.isActive()) {
         pendingEmitter = 0;
         return false;
      } else if (!m.shows(kind)) {
         pendingEmitter = 0;
         return true;
      } else {
         int tag = kind | (mine ? 4 : 0);
         BURSTS[kind]++;
         int extra;
         if (!inMultiplier && (extra = m.multiplier(kind, mine) - 1) > 0) {
            inMultiplier = true;

            try {
               for (int i = 0; i < extra; i++) {
                  nextEmitMine = mine;
                  self.emitParticleAtEntity(entity, type);
               }
            } finally {
               inMultiplier = false;
               nextEmitMine = false;
            }
         }

         pendingEmitter = tag;
         return false;
      }
   }

   public static void emitterSpawn(
      Object emitter,
      World world,
      EnumParticleTypes type,
      boolean force,
      double x,
      double y,
      double z,
      double xOffset,
      double yOffset,
      double zOffset,
      int[] parameters
   ) {
      if (!seenEmitter) {
         seenEmitter = true;
         HookStatus.seen("emitterSpawn");
      }

      int saved = emitterContext;
      if (emitter != null) {
         ParticleTag e = ParticleTag.of(emitter);
         int tag = e.getFvTag();
         if (tag == 0) {
            tag = pendingEmitter | 64;
            pendingEmitter = 0;
            e.setFvTag(tag);
         }

         emitterContext = tag;
      }

      try {
         world.spawnParticle(type, force, x, y, z, xOffset, yOffset, zOffset, parameters);
      } finally {
         emitterContext = saved;
      }
   }

   static void emitMine(Entity target, EnumParticleTypes type) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.effectRenderer != null && target != null) {
         nextEmitMine = true;

         try {
            mc.effectRenderer.emitParticleAtEntity(target, type);
         } finally {
            nextEmitMine = false;
         }
      }
   }

   public static boolean prediction(Entity target) {
      if (!seenPrediction) {
         seenPrediction = true;
         HookStatus.seen("critPrediction");
         HookStatus.seen("sharpPrediction");
      }

      ParticlesModule m = module;
      if (module == null || !m.isActive()) {
         return false;
      } else if (m.realHitsOnly()) {
         return true;
      } else {
         nextEmitMine = true;
         return false;
      }
   }

   public static void critPacket(EffectRenderer renderer, Entity entity, EnumParticleTypes type) {
      if (!seenPacket) {
         seenPacket = true;
         HookStatus.seen("critPacket");
      }

      ParticlesModule m = module;
      if (module != null && m.isActive() && entity != null) {
         boolean mine = m.attackedRecently(entity);
         if (!mine || !m.realHitsOnly()) {
            nextEmitMine = mine;

            try {
               renderer.emitParticleAtEntity(entity, type);
            } finally {
               nextEmitMine = false;
            }
         }
      } else {
         renderer.emitParticleAtEntity(entity, type);
      }
   }

   public static boolean bypassSetting(
      int particleId, boolean ignoreRange, double x, double y, double z, double xOffset, double yOffset, double zOffset, int[] parameters
   ) {
      if (!seenBypass) {
         seenBypass = true;
         HookStatus.seen("bypassSetting");
      }

      if (!inBypass && kindOf(particleId) != 0) {
         ParticlesModule m = module;
         if (m != null && m.isActive() && m.ignoreParticleSetting.get()) {
            Minecraft mc = Minecraft.getMinecraft();
            Entity view = mc.getRenderViewEntity();
            if (mc.gameSettings.particleSetting != 0 && view != null && mc.effectRenderer != null) {
               double dz;
               double dy;
               double dx;
               if (!ignoreRange && (dx = view.posX - x) * dx + (dy = view.posY - y) * dy + (dz = view.posZ - z) * dz > 256.0) {
                  spawnResult = null;
                  return true;
               } else {
                  inBypass = true;

                  try {
                     spawnResult = mc.effectRenderer.spawnEffectParticle(particleId, x, y, z, xOffset, yOffset, zOffset, parameters);
                  } finally {
                     inBypass = false;
                  }

                  return true;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void drawStart(float partialTicks) {
      if (!seenStart) {
         seenStart = true;
         HookStatus.seen("drawStart");
      }

      ParticlesModule m = module;
      if (module != null) {
         m.renderer.begin(m, partialTicks);
      }
   }

   public static void drawParticle(
      EntityFX particle,
      WorldRenderer wr,
      Entity entity,
      float partialTicks,
      float rotationX,
      float rotationZ,
      float rotationYZ,
      float rotationXY,
      float rotationXZ
   ) {
      if (!seenDraw) {
         seenDraw = true;
         HookStatus.seen("drawParticle");
      }

      ParticlesModule m = module;
      if (module != null && m.renderer.isFrameActive() && (ParticleTag.tagOf(particle) & 3) != 0) {
         m.renderer.draw(particle, wr, entity, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
      } else {
         particle.renderParticle(wr, entity, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
      }
   }

   public static void drawEnd() {
      if (!seenEnd) {
         seenEnd = true;
         HookStatus.seen("drawEnd");
      }

      ParticlesModule m = module;
      if (module != null) {
         m.renderer.end();
      }
   }

   static void reset() {
      nextEmitMine = false;
      pendingEmitter = 0;
      emitterContext = 0;
   }
}
