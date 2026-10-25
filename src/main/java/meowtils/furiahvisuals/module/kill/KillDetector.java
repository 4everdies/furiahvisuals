package meowtils.furiahvisuals.module.kill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.core.HitConfirmer;
import meowtils.furiahvisuals.fx.Terrain;
import meowtils.furiahvisuals.module.hit.HitContext;
import meowtils.furiahvisuals.module.hit.HitMeasurer;
import meowtils.furiahvisuals.render.BodyProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

final class KillDetector {
   private static final long DEDUPE_NANOS = 3000000000L;
   private static final long VANISH_WINDOW_NANOS = 1000000000L;
   private static final int HISTORY = 40;
   private static final int SNAPSHOTS = 4;
   private static final int SNAPSHOT_EVERY = 10;
   private static final double JUMP = 8.0;
   static final double MAX_DISTANCE = 48.0;
   private final KillEffectsModule module;
   private final HitMeasurer measurer = new HitMeasurer();
   private final Map<HitConfirmer.Attack, HitContext> waiting = new IdentityHashMap<>();
   private final Map<UUID, KillDetector.Hit> hits = new HashMap<>();
   private final Map<UUID, KillDetector.Track> tracks = new HashMap<>();
   /** Your latest attack on each player (entity id), kept even when the hit is never "confirmed". */
   private final Map<Integer, KillDetector.Hit> attacked = new HashMap<>();
   private final Map<String, Long> recent = new HashMap<>();
   private long lastTickNanos;
   private List<Pattern> patterns = new ArrayList<>();
   private int patternsVersion = -1;
   private String patternsName = "";
   String lastSkip = "";
   String lastChatMatch = "";

   KillDetector(KillEffectsModule module) {
      this.module = module;
      FuriahsVisuals.instance.getHitConfirmer().addListener(new HitConfirmer.Listener() {
         @Override
         public void onConfirmedHit(HitConfirmer.Attack attack) {
            KillDetector.this.confirmed(attack);
         }
      });
   }

   void onAttack(Minecraft mc, EntityPlayerSP me, Entity target) {
      if (target instanceof EntityPlayer && target != me) {
         long when = GameClock.nanos();
         HitContext measured = this.measurer.measure(mc, me, target, when);
         HitConfirmer.Attack attack = FuriahsVisuals.instance.getHitConfirmer().lastAttack();
         if (attack != null && attack.target == target) {
            this.waiting.put(attack, measured);
         }

         KillDetector.Hit h = new KillDetector.Hit();
         h.nanos = when;
         h.ctx = measured;
         this.attacked.put(target.getEntityId(), h);
      }
   }

   private void confirmed(HitConfirmer.Attack attack) {
      if (this.module.isEnabled()) {
         HitContext ctx = this.waiting.remove(attack);
         Iterator<HitConfirmer.Attack> it = this.waiting.keySet().iterator();

         while (it.hasNext()) {
            if (it.next().targetId == attack.targetId) {
               it.remove();
            }
         }

         if (attack.target instanceof EntityPlayer && ctx != null) {
            this.credit((EntityPlayer)attack.target, ctx, GameClock.nanos());
         }
      }
   }

   void onDeath(Minecraft mc, EntityPlayer victim) {
      if (victim == null || victim == mc.thePlayer || mc.theWorld == null || victim.worldObj != mc.theWorld) {
         return;
      }

      long now = Math.max(GameClock.nanos(), this.lastTickNanos + 1L);
      long window = (long)(this.module.credit.getFloat() * 1.0E9);
      HitConfirmer.Attack best = null;
      HitContext context = null;
      Iterator<Entry<HitConfirmer.Attack, HitContext>> it = this.waiting.entrySet().iterator();
      while (it.hasNext()) {
         Entry<HitConfirmer.Attack, HitContext> entry = it.next();
         HitConfirmer.Attack attack = entry.getKey();
         if (attack.targetId == victim.getEntityId()) {
            if (now >= attack.nanos && now - attack.nanos <= window && (best == null || attack.nanos > best.nanos)) {
               best = attack;
               context = entry.getValue();
            }
            it.remove();
         }
      }
      if (best != null && context != null) {
         this.credit(victim, context, best.nanos);
      }

      UUID id = victim.getUniqueID();
      KillDetector.Track track = this.tracks.get(id);
      if (track == null) {
         track = new KillDetector.Track(id);
         this.tracks.put(id, track);
      }
      track.name = victim.getName();
      track.entity = victim;
      track.missing = 0;
      track.dead = true;
      track.invisible = victim.isInvisible();
      track.spectator = victim.isSpectator();
      this.candidate(mc, victim, track, "death", victim.posX, victim.posY, victim.posZ, null, now);
   }

   void credit(EntityPlayer target, HitContext ctx, long nanos) {
      KillDetector.Hit h = this.hits.get(target.getUniqueID());
      if (h == null) {
         h = new KillDetector.Hit();
         this.hits.put(target.getUniqueID(), h);
      }

      h.nanos = nanos;
      h.ctx = ctx;
      this.module.prefetchSkin(target);
   }

   HitMeasurer measurer() {
      return this.measurer;
   }

   private KillDetector.Hit creditFor(UUID id, long now) {
      KillDetector.Hit h = id == null ? null : this.hits.get(id);
      if (h == null) {
         return null;
      } else {
         long window = (long)(this.module.credit.getFloat() * 1.0E9);
         return now - h.nanos <= window ? h : null;
      }
   }

   void tick(Minecraft mc) {
      EntityPlayerSP me = mc.thePlayer;
      if (me != null && mc.theWorld != null) {
         long now;
         this.lastTickNanos = now = Math.max(GameClock.nanos(), this.lastTickNanos + 1L);
         this.measurer.recordLook(me, now);

         for (KillDetector.Track t : this.tracks.values()) {
            t.missing++;
         }

         List players = mc.theWorld.playerEntities;
         double range = 64.0;

         for (int i = 0; i < players.size(); i++) {
            EntityPlayer p = (EntityPlayer)players.get(i);
            if (p != me && !(p.getDistanceSqToEntity(me) > range * range)) {
               KillDetector.Track t = this.tracks.get(p.getUniqueID());
               if (t == null) {
                  t = new KillDetector.Track(p.getUniqueID());
                  this.tracks.put(p.getUniqueID(), t);
               }

               boolean dead = p.getHealth() <= 0.0F || p.deathTime > 0;
               boolean spectator = p.isSpectator();
               if (dead && !t.dead && this.module.detectDeath.get()) {
                  this.candidate(mc, p, t, "death", p.posX, p.posY, p.posZ, null, now);
               }

               if (!dead && this.module.detectVanish.get()) {
                  if (p.isInvisible() && !t.invisible) {
                     this.vanished(mc, t, p, now, "invisible");
                  } else if (spectator && !t.spectator) {
                     this.vanished(mc, t, p, now, "spectator");
                  } else {
                     double dz;
                     double dy;
                     double dx;
                     if (t.filled > 0
                        && t.missing == 1
                        && (dx = p.posX - t.x[t.head]) * dx + (dy = p.posY - t.y[t.head]) * dy + (dz = p.posZ - t.z[t.head]) * dz
                           > 64.0) {
                        this.vanished(mc, t, p, now, "teleported");
                     }
                  }
               }

               t.dead = dead;
               t.invisible = p.isInvisible();
               t.spectator = spectator;
               t.name = p.getName();
               t.entity = p;
               t.missing = 0;
               t.record(p, now);
            }
         }

         Iterator<Entry<UUID, KillDetector.Track>> it = this.tracks.entrySet().iterator();

         while (it.hasNext()) {
            KillDetector.Track tx = it.next().getValue();
            if (tx.missing == 1 && !tx.dead && this.module.detectVanish.get() && tx.entity != null && tx.entity.isDead) {
               this.vanished(mc, tx, tx.entity, now, "removed");
            }

            if (tx.missing > 50) {
               it.remove();
            }
         }

         long keep = (long)(this.module.credit.getFloat() * 1.0E9) + 1000000000L;
         Iterator<KillDetector.Hit> it2 = this.hits.values().iterator();

         while (it2.hasNext()) {
            if (now - it2.next().nanos > keep) {
               it2.remove();
            }
         }

         Iterator<KillDetector.Hit> it5 = this.attacked.values().iterator();

         while (it5.hasNext()) {
            if (now - it5.next().nanos > keep) {
               it5.remove();
            }
         }

         Iterator<Long> it4 = this.recent.values().iterator();

         while (it4.hasNext()) {
            if (now - it4.next() > 3000000000L) {
               it4.remove();
            }
         }

         if (!this.waiting.isEmpty()) {
            HitConfirmer confirmer = FuriahsVisuals.instance.getHitConfirmer();
            Iterator<HitConfirmer.Attack> it3 = this.waiting.keySet().iterator();

            while (it3.hasNext()) {
               if (!confirmer.isPending(it3.next())) {
                  it3.remove();
               }
            }
         }
      }
   }

   private void vanished(Minecraft mc, KillDetector.Track t, EntityPlayer p, long now, String how) {
      KillDetector.Hit h = this.hits.get(t.id);
      if (h != null && now - h.nanos <= 1000000000L && t.filled != 0) {
         float lastHealth = t.health[t.head];
         float limit = this.module.vanishHealth.getFloat() * 2.0F;
         if (lastHealth > limit) {
            this.lastSkip = t.name + " vanished (" + how + ") at " + fmt(lastHealth / 2.0F) + " hearts: not a kill";
         } else {
            int at = t.head;
            this.candidate(mc, p, t, "vanished", t.x[at], t.y[at], t.z[at], t.snapshotBefore(now), now);
         }
      }
   }

   private static String fmt(float v) {
      return String.format(Locale.ROOT, "%.1f", v);
   }

   void onChat(Minecraft mc, String text) {
      if (this.module.detectChat.get() && mc.thePlayer != null && text != null) {
         String clean = EnumChatFormatting.getTextWithoutFormattingCodes(text);
         if (clean != null && !clean.isEmpty()) {
            String me = mc.thePlayer.getName();
            this.compile(me);

            for (int i = 0; i < this.patterns.size(); i++) {
               Matcher m = this.patterns.get(i).matcher(clean);
               String victim;
               if (m.find() && !(victim = m.group(1)).equalsIgnoreCase(me)) {
                  this.lastChatMatch = clean;
                  this.chatKill(mc, victim, GameClock.nanos());
                  return;
               }
            }
         }
      }
   }

   private void compile(String me) {
      int v = this.module.chatPatterns.version();
      if (v != this.patternsVersion || !me.equals(this.patternsName)) {
         this.patternsVersion = v;
         this.patternsName = me;
         ArrayList<Pattern> out = new ArrayList<>();

         for (String s : this.module.chatPatterns.get()) {
            Pattern p = toRegex(s, me);
            if (p != null) {
               out.add(p);
            }
         }

         this.patterns = out;
      }
   }

   static Pattern toRegex(String pattern, String me) {
      if (pattern != null && KillPatternSetting.problem(pattern) == null) {
         String p = pattern.trim();
         StringBuilder sb = new StringBuilder();
         int i = 0;

         while (i < p.length()) {
            if (p.startsWith("{me}", i)) {
               sb.append("(?<![A-Za-z0-9_])").append(Pattern.quote(me)).append("(?![A-Za-z0-9_])");
               i += 4;
            } else if (p.startsWith("{victim}", i)) {
               sb.append("(?<![A-Za-z0-9_])([A-Za-z0-9_]{1,16})(?![A-Za-z0-9_])");
               i += 8;
            } else if (p.charAt(i) == '*') {
               sb.append(".*?");
               i++;
            } else {
               int next = p.length();
               int a = p.indexOf("{me}", i);
               int b = p.indexOf("{victim}", i);
               int c = p.indexOf(42, i);
               if (a >= 0) {
                  next = Math.min(next, a);
               }

               if (b >= 0) {
                  next = Math.min(next, b);
               }

               if (c >= 0) {
                  next = Math.min(next, c);
               }

               sb.append(Pattern.quote(p.substring(i, next)));
               i = next;
            }
         }

         try {
            return Pattern.compile(sb.toString(), 2);
         } catch (RuntimeException var9) {
            return null;
         }
      } else {
         return null;
      }
   }

   private void chatKill(Minecraft mc, String victim, long now) {
      KillDetector.Track t = null;

      for (KillDetector.Track c : this.tracks.values()) {
         if (c.name != null && c.name.equalsIgnoreCase(victim)) {
            t = c;
            break;
         }
      }

      if (t != null && t.entity != null && t.filled != 0) {
         int at = t.lastBeforeJump();
         BodyProxy.Snapshot body = at == t.head && !t.dead ? null : t.snapshotBefore(t.time[at]);
         this.candidate(mc, t.entity, t, "chat", t.x[at], t.y[at], t.z[at], body, now);
      } else {
         this.lastSkip = "chat kill of " + victim + ": not seen nearby";
      }
   }

   private void candidate(Minecraft mc, EntityPlayer p, KillDetector.Track t, String method, double x, double y, double z, BodyProxy.Snapshot body, long now) {
      EntityPlayerSP me = mc.thePlayer;
      String name = t.name != null ? t.name : p.getName();
      KillDetector.Hit hit = this.creditFor(t.id, now);
      if (hit == null) {
         // The hurt-time confirmation can miss the killing blow (the victim dies or is removed first), so a recent
         // attack of yours on this very player still counts as your kill.
         KillDetector.Hit swing = this.attacked.get(p.getEntityId());
         long window = (long)(this.module.credit.getFloat() * 1.0E9);
         if (swing != null && now >= swing.nanos && now - swing.nanos <= window) {
            hit = swing;
         }
      }

      boolean chat = "chat".equals(method);
      boolean mine = hit != null || chat;
      if (!mine && this.module.showFor.get() != KillEffectsModule.ShowFor.EVERY_DEATH) {
         this.lastSkip = method + " of " + name + ": not your kill";
      } else {
         String key = t.id != null ? t.id.toString() : name.toLowerCase(Locale.ROOT);
         Long last = this.recent.get(key);
         if (last != null && now - last < 3000000000L) {
            this.lastSkip = method + " of " + name + ": already played";
         } else {
            double dx = x - me.posX;
            double dy = y - me.posY;
            double dz = z - me.posZ;
            if (dx * dx + dy * dy + dz * dz > 2304.0) {
               this.lastSkip = method + " of " + name + ": too far away";
            } else if (y < 0.0) {
               this.lastSkip = method + " of " + name + ": into the void";
            } else {
               this.recent.put(key, now);
               KillContext ctx = new KillContext();
               if (body != null) {
                  ctx.body.copy(body);
               } else {
                  ctx.body.capture(p, 1.0F);
               }

               double sneak = ctx.body.pose.sneaking && body == null ? -0.125 : 0.0;
               ctx.body.pose.x = x;
               ctx.body.pose.y = y + (body == null ? sneak : ctx.body.pose.y - body.pose.y);
               ctx.body.pose.z = z;
               ctx.body.pose.deathAngle = 0.0F;
               if (body == null && !chat && !"vanished".equals(method)) {
                  double v = Math.sqrt(ctx.body.vx * ctx.body.vx + ctx.body.vy * ctx.body.vy + ctx.body.vz * ctx.body.vz);
                  if (v > 12.0) {
                     ctx.body.vx *= 12.0 / v;
                     ctx.body.vy *= 12.0 / v;
                     ctx.body.vz *= 12.0 / v;
                  }
               } else {
                  ctx.body.vz = 0.0;
                  ctx.body.vy = 0.0;
                  ctx.body.vx = 0.0;
               }

               ctx.feetX = x;
               ctx.feetY = y;
               ctx.feetZ = z;
               ctx.victimName = name;
               ctx.method = method;
               ctx.mine = mine;
               ctx.time = now;
               double ground = Terrain.surfaceAt(mc.theWorld, x, z, y + 0.5, 1.5, 8.0);
               double d = ctx.groundY = Double.isNaN(ground) ? y : ground;
               if (hit != null && hit.ctx != null) {
                  HitContext h = hit.ctx;
                  ctx.impactX = x + h.offX;
                  ctx.impactY = y + h.offY;
                  ctx.impactZ = z + h.offZ;
                  ctx.dirX = h.dirX;
                  ctx.dirY = h.dirY;
                  ctx.dirZ = h.dirZ;
                  ctx.swipeX = h.swipeX;
                  ctx.swipeY = h.swipeY;
                  ctx.swipeZ = h.swipeZ;
                  ctx.crit = h.crit;
                  ctx.hitTime = hit.nanos;
               } else {
                  fromViewer(ctx, me.posX, me.posY + me.getEyeHeight(), me.posZ);
               }

               this.lastSkip = "";
               this.module.onKill(ctx, p);
            }
         }
      }
   }

   static void fromViewer(KillContext ctx, double ex, double ey, double ez) {
      double cx = ctx.feetX - ex;
      double cy = ctx.feetY + 1.1 - ey;
      double cz = ctx.feetZ - ez;
      double len = Math.sqrt(cx * cx + cy * cy + cz * cz);
      if (len < 1.0E-4) {
         cx = 0.0;
         cy = 0.0;
         cz = 1.0;
         len = 1.0;
      }

      ctx.dirX = cx / len;
      ctx.dirY = cy / len;
      ctx.dirZ = cz / len;
      double hl = Math.sqrt(ctx.dirX * ctx.dirX + ctx.dirZ * ctx.dirZ);
      double hx = hl < 1.0E-4 ? 0.0 : ctx.dirX / hl;
      double hz = hl < 1.0E-4 ? 1.0 : ctx.dirZ / hl;
      ctx.impactX = ctx.feetX - hx * 0.3;
      ctx.impactY = ctx.feetY + 1.1;
      ctx.impactZ = ctx.feetZ - hz * 0.3;
      ctx.swipeX = -hz;
      ctx.swipeY = 0.0;
      ctx.swipeZ = hx;
   }

   void clear() {
      this.waiting.clear();
      this.hits.clear();
      this.tracks.clear();
      this.recent.clear();
   }

   static final class Hit {
      long nanos;
      HitContext ctx;
   }

   static final class Track {
      final UUID id;
      String name;
      EntityPlayer entity;
      final double[] x = new double[40];
      final double[] y = new double[40];
      final double[] z = new double[40];
      final float[] health = new float[40];
      final long[] time = new long[40];
      int head = -1;
      int filled;
      final BodyProxy.Snapshot[] snaps = new BodyProxy.Snapshot[4];
      final long[] snapTime = new long[4];
      int snapHead = -1;
      int ticks;
      boolean dead;
      boolean invisible;
      boolean spectator;
      int missing;

      Track(UUID id) {
         this.id = id;

         for (int i = 0; i < 4; i++) {
            this.snaps[i] = new BodyProxy.Snapshot();
         }
      }

      void record(EntityPlayer p, long now) {
         this.head = (this.head + 1) % 40;
         this.x[this.head] = p.posX;
         this.y[this.head] = p.posY;
         this.z[this.head] = p.posZ;
         this.health[this.head] = p.getHealth();
         this.time[this.head] = now;
         if (this.filled < 40) {
            this.filled++;
         }

         if (this.ticks++ % 10 == 0 && p.getHealth() > 0.0F && !p.isInvisible() && p.deathTime == 0) {
            this.snapHead = (this.snapHead + 1) % 4;
            this.snaps[this.snapHead].capture(p, 1.0F);
            this.snapTime[this.snapHead] = now;
         }
      }

      int lastBeforeJump() {
         for (int k = 0; k + 1 < this.filled; k++) {
            int i = (this.head - k + 40) % 40;
            int j = (i - 1 + 40) % 40;
            double dx = this.x[i] - this.x[j];
            double dy = this.y[i] - this.y[j];
            double dz = this.z[i] - this.z[j];
            if (dx * dx + dy * dy + dz * dz > 64.0) {
               return j;
            }
         }

         return this.head;
      }

      BodyProxy.Snapshot snapshotBefore(long nanos) {
         BodyProxy.Snapshot best = null;
         long bestTime = Long.MIN_VALUE;

         for (int i = 0; i < 4; i++) {
            if (this.snaps[i].valid && this.snapTime[i] <= nanos && this.snapTime[i] > bestTime) {
               best = this.snaps[i];
               bestTime = this.snapTime[i];
            }
         }

         return best;
      }
   }
}
