package meowtils.furiahvisuals.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import sun.misc.Unsafe;

public final class Swap {
   private static final Unsafe UNSAFE;

   private Swap() {
   }

   public static <T, S extends T> S upgrade(Object current, Class<T> base, Class<S> sub) {
      if (current != null && UNSAFE != null) {
         Class<?> c = current.getClass();
         if (c == sub) {
            return sub.cast(current);
         } else if (c == base || c.getSuperclass() == base && c.getName().equals(sub.getName())) {
            return copy(current, base, sub);
         } else {
            FuriahsVisuals.LOG.warn("Hooks: {} is a {}, not a plain {}; that hook stays off", new Object[]{base.getSimpleName(), c.getName(), base.getName()});
            return null;
         }
      } else {
         return null;
      }
   }

   public static <T> T downgrade(Object current, Class<T> base) {
      return current != null && UNSAFE != null && current.getClass() != base ? copy(current, base, base) : base.cast(current);
   }

   public static boolean ours(Object o, Class<?> sub) {
      return o != null && o.getClass().getName().equals(sub.getName());
   }

   private static <S> S copy(Object from, Class<?> base, Class<S> to) {
      try {
         Object o = UNSAFE.allocateInstance(to);

         for (Class<?> k = base; k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
               if (!Modifier.isStatic(f.getModifiers())) {
                  long off = UNSAFE.objectFieldOffset(f);
                  Class<?> t = f.getType();
                  if (!t.isPrimitive()) {
                     UNSAFE.putObject(o, off, UNSAFE.getObject(from, off));
                  } else if (t == int.class) {
                     UNSAFE.putInt(o, off, UNSAFE.getInt(from, off));
                  } else if (t == float.class) {
                     UNSAFE.putFloat(o, off, UNSAFE.getFloat(from, off));
                  } else if (t == double.class) {
                     UNSAFE.putDouble(o, off, UNSAFE.getDouble(from, off));
                  } else if (t == long.class) {
                     UNSAFE.putLong(o, off, UNSAFE.getLong(from, off));
                  } else if (t == boolean.class) {
                     UNSAFE.putBoolean(o, off, UNSAFE.getBoolean(from, off));
                  } else if (t == byte.class) {
                     UNSAFE.putByte(o, off, UNSAFE.getByte(from, off));
                  } else if (t == short.class) {
                     UNSAFE.putShort(o, off, UNSAFE.getShort(from, off));
                  } else if (t == char.class) {
                     UNSAFE.putChar(o, off, UNSAFE.getChar(from, off));
                  }
               }
            }
         }

         return to.cast(o);
      } catch (Throwable var12) {
         FuriahsVisuals.LOG.error("Hooks: could not copy a {}", new Object[]{base.getName(), var12});
         return null;
      }
   }

   public static void set(Field f, Object owner, Object value) {
      if (f != null && UNSAFE != null) {
         UNSAFE.putObject(owner, UNSAFE.objectFieldOffset(f), value);
      }
   }

   public static int getInt(Field f, Object owner) {
      return f != null && UNSAFE != null ? UNSAFE.getInt(owner, UNSAFE.objectFieldOffset(f)) : 0;
   }

   public static void setInt(Field f, Object owner, int value) {
      if (f != null && UNSAFE != null) {
         UNSAFE.putInt(owner, UNSAFE.objectFieldOffset(f), value);
      }
   }

   public static void rebind(Object holder, Object from, Object to) {
      if (holder != null && from != null && UNSAFE != null) {
         for (Class<?> k = holder.getClass(); k != null && k != Object.class; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
               if (!Modifier.isStatic(f.getModifiers()) && !f.getType().isPrimitive()) {
                  long off = UNSAFE.objectFieldOffset(f);
                  if (UNSAFE.getObject(holder, off) == from) {
                     UNSAFE.putObject(holder, off, to);
                  }
               }
            }
         }
      }
   }

   public static Object get(Field f, Object owner) {
      return f != null && UNSAFE != null ? UNSAFE.getObject(owner, UNSAFE.objectFieldOffset(f)) : null;
   }

   static {
      Unsafe u = null;

      try {
         Field f = Unsafe.class.getDeclaredField("theUnsafe");
         f.setAccessible(true);
         u = (Unsafe)f.get(null);
      } catch (Throwable var2) {
         FuriahsVisuals.LOG.error("Hooks: no Unsafe, so none of the swapped-in hooks can install", var2);
      }

      UNSAFE = u;
   }
}
