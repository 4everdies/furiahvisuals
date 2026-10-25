package meowtils.furiahvisuals.module.particles;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;
import meowtils.furiahvisuals.hook.Hooks;
import net.minecraft.client.particle.EntityFX;

public final class ParticleTag {
   public static final int CRIT = 1;
   public static final int SHARP = 2;
   public static final int KIND_MASK = 3;
   public static final int MINE = 4;
   public static final int REPLACED = 8;
   public static final int HAS_IMAGE = 16;
   public static final int IMAGE_SHIFT = 8;
   public static final int IMAGE_MASK = 65280;
   public static final int SILENT = 32;
   private static final Map<Object, ParticleTag> TAGS = new WeakHashMap<>();
   private static final Field AGE = Hooks.field(EntityFX.class, "particleAge", "particleAge");
   private static final Field MAX_AGE = Hooks.field(EntityFX.class, "particleMaxAge", "particleMaxAge");
   private final Object owner;
   private int tag;
   private int seed;
   private long born;

   private ParticleTag(Object owner) {
      this.owner = owner;
   }

   public static ParticleTag of(Object o) {
      ParticleTag t = TAGS.get(o);
      if (t == null) {
         t = new ParticleTag(o);
         TAGS.put(o, t);
      }

      return t;
   }

   public static int tagOf(Object o) {
      ParticleTag t = TAGS.get(o);
      return t == null ? 0 : t.tag;
   }

   public static void clear() {
      TAGS.clear();
   }

   public int getFvTag() {
      return this.tag;
   }

   public void setFvTag(int tag) {
      this.tag = tag;
   }

   public int getFvSeed() {
      return this.seed;
   }

   public void setFvSeed(int seed) {
      this.seed = seed;
   }

   public long getFvBorn() {
      return this.born;
   }

   public void setFvBorn(long nanos) {
      this.born = nanos;
   }

   public int getFvAge() {
      return read(AGE, this.owner);
   }

   public int getFvMaxAge() {
      return read(MAX_AGE, this.owner);
   }

   private static int read(Field f, Object o) {
      if (f != null && o instanceof EntityFX) {
         try {
            return f.getInt(o);
         } catch (IllegalAccessException var3) {
            return 0;
         }
      } else {
         return 0;
      }
   }
}
