package meowtils.furiahvisuals.module.weapons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import meowtils.furiahvisuals.asset.AssetSounds;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.SoundSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

final class ChainsawSound {
   private static final ResourceLocation IDLE = sound("chainsaw_idle");
   private static final ResourceLocation FULL = sound("chainsaw_full");
   private static final ResourceLocation START = sound("chainsaw_start");
   private static final ResourceLocation PULL = sound("chainsaw_pull");
   private static final ResourceLocation STOP = sound("chainsaw_stop");
   private static final ResourceLocation BITE = sound("chainsaw_bite");
   private static final ResourceLocation OTHERS = sound("chainsaw_others");
   private static final int START_TICKS = 15;
   private static final int PULL_TICKS = 30;
   private static final int STOP_TICKS = 6;
   private static final int BITE_TICKS = 7;
   private static final int MAX_OTHERS = 3;
   private static final double OTHERS_RANGE = 16.0;
   private final WeaponsModule module;
   private ChainsawSound.Voice idle;
   private ChainsawSound.Voice full;
   private ChainsawSound.Voice shot;
   private int ticks;
   private int shotUntil;
   private WorldClient world;
   private boolean wasRunning;
   private float lastVolume;
   private Engine demo;
   private int demoTicks;
   private float demoVolume;
   private final Map<EntityPlayer, ChainsawSound.Voice> others = new HashMap<>();
   private final Map<EntityPlayer, float[]> otherThrottle = new HashMap<>();

   private static ResourceLocation sound(String name) {
      return AssetSounds.event(name);
   }

   ChainsawSound(WeaponsModule module) {
      this.module = module;
   }

   private static float volume(WeaponSettings s) {
      SoundSettings ss = ModSounds.settings();
      return s != null && s.sound != null && s.sound.get() && ss != null && !ss.mute.get()
         ? s.soundVolume.getFloat() / 100.0F * ss.volume.getFloat() / 100.0F
         : 0.0F;
   }

   void tick(Minecraft mc) {
      this.ticks++;
      EntityPlayerSP p = mc.thePlayer;
      if (mc.theWorld != this.world) {
         this.world = mc.theWorld;
         this.stopAll();
      }

      if (p != null && mc.theWorld != null && this.module.isEnabled() && !p.isDead && !(p.getHealth() <= 0.0F)) {
         boolean idleOn;
         float vol;
         Engine e;
         if (this.demo != null) {
            this.stepDemo();
            e = this.demo;
            vol = this.demoVolume;
            idleOn = true;
         } else {
            ViewModels.Hand hand = this.module.activeHand();
            e = hand != null && hand.def != null && hand.def.engine ? hand.engine : null;
            WeaponSettings s = e == null ? null : this.module.settingsOf(hand.def);
            vol = volume(s);
            idleOn = s != null && s.soundIdle != null && s.soundIdle.get();
         }

         if (e == null) {
            this.stopLoops();
            if (this.wasRunning) {
               this.oneShot(STOP, this.lastVolume * 0.9F, 1.0F, 6);
            }

            this.wasRunning = false;
         } else {
            if (e.cut) {
               e.cut = false;
               this.stopLoops();
               if (vol > 0.0F) {
                  this.oneShot(STOP, vol * 0.9F, 1.0F, 6);
               }
            }

            if (e.started) {
               e.started = false;
               if (vol > 0.0F) {
                  this.oneShot(START, vol, 1.0F, 15);
               }
            }

            if (e.running && vol > 0.0F) {
               float thr = e.throttle;
               float idleVol = idleOn ? vol * 0.8F * (1.0F - thr) * e.idleShare() : 0.0F;
               float fullVol = vol * thr;
               if (this.ticks < this.shotUntil) {
                  if (idleVol >= fullVol) {
                     fullVol = 0.0F;
                  } else {
                     idleVol = 0.0F;
                  }
               }

               this.idle = this.keep(this.idle, IDLE, idleVol, 0.92F + 0.22F * thr);
               this.full = this.keep(this.full, FULL, fullVol, 0.86F + 0.16F * thr);
            } else {
               this.stopLoops();
            }

            this.wasRunning = this.idle != null || this.full != null;
            this.lastVolume = vol;
         }

         this.tickOthers(mc, p);
      } else {
         this.stopAll();
      }
   }

   private ChainsawSound.Voice keep(ChainsawSound.Voice v, ResourceLocation location, float volume, float pitch) {
      if (volume <= 0.002F) {
         if (v != null) {
            v.end();
         }

         return null;
      } else {
         if (v == null || v.isDonePlaying() || this.ticks - v.started > 10 && !Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(v)) {
            if (v != null) {
               v.end();
            }

            v = new ChainsawSound.Voice(location, true, null, volume, pitch, this.ticks);
            play(v, pitch, volume);
         }

         v.targetVolume = volume;
         v.targetPitch = pitch;
         return v;
      }
   }

   private void oneShot(ResourceLocation location, float volume, float pitch, int length) {
      if (this.shot != null) {
         this.shot.end();
      }

      this.shot = new ChainsawSound.Voice(location, false, null, volume, pitch, this.ticks);
      play(this.shot, pitch, volume);
      this.shotUntil = this.ticks + length;
   }

   private static void play(ChainsawSound.Voice v, float pitch, float volume) {
      ModSounds.noteForTest(v.getSoundLocation().getResourcePath(), pitch, volume);
      if (AssetSounds.ensure()) {
         try {
            Minecraft.getMinecraft().getSoundHandler().playSound(v);
         } catch (RuntimeException var4) {
            FuriahsVisuals.LOG.debug("Could not play {}", new Object[]{v.getSoundLocation(), var4});
         }
      }
   }

   void pull() {
      ViewModels.Hand hand = this.module.activeHand();
      float vol = hand != null && hand.def != null && hand.def.engine ? volume(this.module.settingsOf(hand.def)) : 0.0F;
      if (vol > 0.0F) {
         this.oneShot(PULL, vol, 1.0F, 30);
      }
   }

   void bite() {
      ViewModels.Hand hand = this.module.activeHand();
      if (hand != null && hand.def != null && hand.def.engine && hand.engine.running) {
         WeaponSettings s = this.module.settingsOf(hand.def);
         float vol = volume(s);
         if (vol > 0.0F && s.soundBite != null && s.soundBite.get()) {
            this.oneShot(BITE, vol, 0.97F + (float)Math.random() * 0.06F, 7);
         }
      }
   }

   void preview(WeaponSettings s) {
      SoundSettings ss = ModSounds.settings();
      if (ss != null && !ss.mute.get() && s.soundVolume != null) {
         this.stopLoops();
         this.demo = new Engine();
         this.demo.draw();
         this.demoTicks = 0;
         this.demoVolume = s.soundVolume.getFloat() / 100.0F * ss.volume.getFloat() / 100.0F;
      }
   }

   private void stepDemo() {
      this.demoTicks++;
      if (this.demoTicks == 22 || this.demoTicks == 27 || this.demoTicks == 31) {
         this.demo.rev();
      }

      if (this.demoTicks == 58) {
         this.demo.stop();
      }

      this.demo.update(0.05F, false);
      if (this.demoTicks > 64) {
         this.demo = null;
      }
   }

   boolean demoPlaying() {
      return this.demo != null;
   }

   int playingForTest() {
      int n = 0;

      for (ChainsawSound.Voice v : new ChainsawSound.Voice[]{this.idle, this.full, this.shot}) {
         if (v != null && Minecraft.getMinecraft().getSoundHandler().isSoundPlaying(v)) {
            n++;
         }
      }

      return n;
   }

   private void tickOthers(Minecraft mc, EntityPlayerSP me) {
      WeaponDef chainsaw = this.module.library.get("chainsaw");
      WeaponSettings s = chainsaw == null ? null : this.module.settingsOf(chainsaw);
      float vol = s != null && s.soundOthers != null && s.soundOthers.get() && this.module.otherPlayers.get() ? volume(s) : 0.0F;
      ArrayList<EntityPlayer> near = new ArrayList<>();
      if (vol > 0.0F) {
         for (EntityPlayer p : mc.theWorld.playerEntities) {
            WeaponDef d;
            if (p != me && !p.isDead && !(p.getDistanceToEntity(me) > 16.0) && (d = this.module.thirdPerson().weaponOf(p)) != null && d.engine) {
               near.add(p);
            }
         }

         final EntityPlayerSP self = me;
         Collections.sort(near, new Comparator<EntityPlayer>() {
            public int compare(EntityPlayer a, EntityPlayer b) {
               return Double.compare(a.getDistanceSqToEntity(self), b.getDistanceSqToEntity(self));
            }
         });
         if (near.size() > 3) {
            near = new ArrayList<>(near.subList(0, 3));
         }
      }

      Iterator<Entry<EntityPlayer, ChainsawSound.Voice>> it = this.others.entrySet().iterator();

      while (it.hasNext()) {
         Entry<EntityPlayer, ChainsawSound.Voice> e = it.next();
         if (!near.contains(e.getKey())) {
            e.getValue().end();
            it.remove();
            this.otherThrottle.remove(e.getKey());
         }
      }

      for (EntityPlayer px : near) {
         float[] thr = this.otherThrottle.get(px);
         if (thr == null) {
            thr = new float[1];
            this.otherThrottle.put(px, thr);
         }

         thr[0] = px.isSwingInProgress ? 1.0F : Math.max(0.0F, thr[0] - 0.08333333F);
         float volume = Math.min(1.0F, vol * (0.75F + 0.25F * thr[0]));
         float pitch = 0.95F + 0.6F * thr[0];
         ChainsawSound.Voice v = this.others.get(px);
         if (v == null || v.isDonePlaying() || this.ticks - v.started > 10 && !mc.getSoundHandler().isSoundPlaying(v)) {
            if (v != null) {
               v.end();
            }

            v = new ChainsawSound.Voice(OTHERS, true, px, volume, pitch, this.ticks);
            play(v, pitch, volume);
            this.others.put(px, v);
         }

         v.targetVolume = volume;
         v.targetPitch = pitch;
      }
   }

   private void stopLoops() {
      if (this.idle != null) {
         this.idle.end();
         this.idle = null;
      }

      if (this.full != null) {
         this.full.end();
         this.full = null;
      }
   }

   void stopAll() {
      this.stopLoops();
      if (this.shot != null) {
         this.shot.end();
         this.shot = null;
      }

      for (ChainsawSound.Voice v : this.others.values()) {
         v.end();
      }

      this.others.clear();
      this.otherThrottle.clear();
      this.wasRunning = false;
      this.demo = null;
   }

   static final class Voice extends MovingSound {
      final EntityPlayer at;
      final int started;
      float targetVolume;
      float targetPitch;
      private boolean stop;

      Voice(ResourceLocation location, boolean loop, EntityPlayer at, float volume, float pitch, int tick) {
         super(location);
         this.at = at;
         this.started = tick;
         this.repeat = loop;
         this.repeatDelay = 0;
         this.attenuationType = at == null ? AttenuationType.NONE : AttenuationType.LINEAR;
         this.targetVolume = volume;
         this.targetPitch = pitch;
         this.volume = Math.max(0.001F, volume);
         this.pitch = pitch;
         this.place();
      }

      private void place() {
         EntityPlayer p = (EntityPlayer)(this.at != null ? this.at : Minecraft.getMinecraft().thePlayer);
         if (p != null) {
            this.xPosF = (float)p.posX;
            this.yPosF = (float)(p.posY + (this.at != null ? p.height * 0.6 : p.getEyeHeight()));
            this.zPosF = (float)p.posZ;
         }
      }

      void end() {
         this.stop = true;

         try {
            Minecraft.getMinecraft().getSoundHandler().stopSound(this);
         } catch (RuntimeException var2) {
            FuriahsVisuals.LOG.debug("Could not stop {}", new Object[]{this.getSoundLocation(), var2});
         }
      }

      public void update() {
         if (!this.stop && (this.at == null || !this.at.isDead && this.at.worldObj == Minecraft.getMinecraft().theWorld)) {
            this.volume = Math.max(5.0E-4F, Math.min(1.0F, this.targetVolume));
            this.pitch = Math.max(0.5F, Math.min(2.0F, this.targetPitch));
            this.place();
         } else {
            this.donePlaying = true;
         }
      }
   }
}
