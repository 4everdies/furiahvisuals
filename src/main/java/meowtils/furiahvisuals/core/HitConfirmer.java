package meowtils.furiahvisuals.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

public final class HitConfirmer {
   private static final long CONFIRM_WINDOW_NANOS = 400000000L;
   private final List<HitConfirmer.Attack> pending = new ArrayList<>();
   private final Map<Integer, Integer> seenHurt = new HashMap<>();
   private final List<HitConfirmer.Listener> listeners = new ArrayList<>();
   private HitConfirmer.Attack last;

   public void addListener(HitConfirmer.Listener listener) {
      this.listeners.add(listener);
   }

   HitConfirmer.Attack attack(Entity target) {
      if (!(target instanceof EntityLivingBase)) {
         this.last = null;
         return null;
      } else {
         int id = target.getEntityId();
         if (!this.seenHurt.containsKey(id)) {
            this.seenHurt.put(id, ((EntityLivingBase)target).hurtTime);
         }

         this.last = new HitConfirmer.Attack(target, GameClock.nanos());
         this.pending.add(this.last);
         return this.last;
      }
   }

   public HitConfirmer.Attack lastAttack() {
      return this.last;
   }

   public boolean isPending(HitConfirmer.Attack attack) {
      return this.pending.contains(attack);
   }

   void tick(Minecraft mc) {
      if (this.pending.isEmpty()) {
         this.seenHurt.clear();
      } else {
         WorldClient world = mc.theWorld;
         long now = GameClock.nanos();
         long ping = ping(mc);
         long window = 400000000L + Math.min(600L, ping) * 1000000L;
         Iterator<Entry<Integer, Integer>> it = this.seenHurt.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Integer, Integer> entry = it.next();
            int id = entry.getKey();
            Entity e = world == null ? null : world.getEntityByID(id);
            if (!(e instanceof EntityLivingBase)) {
               this.drop(id);
               it.remove();
            } else {
               int hurt = ((EntityLivingBase)e).hurtTime;
               if (hurt > entry.getValue()) {
                  this.confirm(id, now, ping);
               }

               entry.setValue(hurt);
            }
         }

         for (int i = this.pending.size() - 1; i >= 0; i--) {
            if (now - this.pending.get(i).nanos > window) {
               this.pending.remove(i);
            }
         }

         Iterator<Integer> it2 = this.seenHurt.keySet().iterator();

         while (it2.hasNext()) {
            int id = it2.next();
            boolean any = false;

            for (int ix = 0; ix < this.pending.size() && !any; ix++) {
               any = this.pending.get(ix).targetId == id;
            }

            if (!any) {
               it2.remove();
            }
         }
      }
   }

   private void confirm(int id, long now, long pingMs) {
      long expected = now - pingMs * 1000000L;
      HitConfirmer.Attack best = null;

      for (int i = 0; i < this.pending.size(); i++) {
         HitConfirmer.Attack a = this.pending.get(i);
         if (a.targetId == id && (best == null || Math.abs(a.nanos - expected) < Math.abs(best.nanos - expected))) {
            best = a;
         }
      }

      if (best != null) {
         this.drop(id);

         for (int var12 = 0; var12 < this.listeners.size(); var12++) {
            try {
               this.listeners.get(var12).onConfirmedHit(best);
            } catch (RuntimeException | LinkageError var11) {
               FuriahsVisuals.LOG.error("A confirmed-hit listener failed", var11);
            }
         }
      }
   }

   private void drop(int id) {
      for (int i = this.pending.size() - 1; i >= 0; i--) {
         if (this.pending.get(i).targetId == id) {
            this.pending.remove(i);
         }
      }
   }

   void clear() {
      this.pending.clear();
      this.seenHurt.clear();
      this.last = null;
   }

   private static long ping(Minecraft mc) {
      if (mc.getNetHandler() != null && mc.thePlayer != null) {
         NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
         return info == null ? 0L : Math.max(0, Math.min(1000, info.getResponseTime()));
      } else {
         return 0L;
      }
   }

   public static final class Attack {
      public final Entity target;
      public final int targetId;
      public final long nanos;

      Attack(Entity target, long nanos) {
         this.target = target;
         this.targetId = target.getEntityId();
         this.nanos = nanos;
      }
   }

   public interface Listener {
      void onConfirmedHit(HitConfirmer.Attack var1);
   }
}
