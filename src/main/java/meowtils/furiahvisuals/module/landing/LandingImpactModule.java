package meowtils.furiahvisuals.module.landing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.core.TestDummy;
import meowtils.furiahvisuals.fx.Anchor;
import meowtils.furiahvisuals.fx.EffectEngine;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.FxParticles;
import meowtils.furiahvisuals.fx.Lifecycle;
import meowtils.furiahvisuals.fx.Rng;
import meowtils.furiahvisuals.fx.Terrain;
import meowtils.furiahvisuals.fx.look.WorldLight;
import meowtils.furiahvisuals.fx.pass.FxPasses;
import meowtils.furiahvisuals.fx.style.Signature;
import meowtils.furiahvisuals.gui.widget.SignatureColorsWidget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.PoseSnapshot;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public final class LandingImpactModule extends Module {
   private static final long WAIT_FOR_MODEL_NANOS = 200000000L;
   private static final double LOD_DISTANCE = 16.0;
   private static final float NORMAL_JUMP = 1.25F;
   private static final int KNOCKBACK_TICKS = 3;
   private static final long BUNNY_HOP_NANOS = 150000000L;
   private static final float SPRINT_BOOST = 1.25F;
   private static final double TELEPORT_XZ = 4.0;
   private static final double TELEPORT_UP = 2.5;
   private static final double TELEPORT_DOWN = 8.0;
   public final ModeSetting<LandingStyle> style;
   public final ModeSetting<LandingImpactModule.Trigger> trigger;
   public final BooleanSetting edgeJumps;
   public final BooleanSetting ignoreKnockback;
   public final SliderSetting jumpStrength;
   public final BooleanSetting sprintBoost;
   public final SliderSetting jumpCooldown;
   public final ModeSetting<LandingImpactModule.ShowFor> showFor;
   public final SliderSetting maxDistance;
   public final BooleanSetting firstPerson;
   public final SliderSetting minAirTime;
   public final SliderSetting minFallHeight;
   public final BooleanSetting scaleWithFall;
   public final SliderSetting maxScale;
   public final SliderSetting size;
   public final SliderSetting duration;
   public final SliderSetting speed;
   public final SliderSetting opacity;
   public final SliderSetting density;
   public final BooleanSetting conform;
   public final SliderSetting maxEffects;
   public final WorldLight lighting;
   public final BooleanSetting dummyJumps;
   private final LandingStyleBase[] styles;
   private final EffectEngine<LandingFx> engine = new EffectEngine<>("Landing Impact");
   private final FxParticles particles = new FxParticles(2048);
   private final Camera camera = new Camera();
   private final FxFrame frame = new FxFrame();
   private long lastFrameNanos;
   private float renderMillis;
   private int seedCounter = 1;
   private long tickCounter;
   private long testAt;
   private int spawnCount;
   private int otherSpawnCount;
   private int jumpCount;
   private int otherJumpCount;
   private float lastFall;
   private float lastScale;
   private float lastJumpScale;
   private double lastModelGap;
   private final LandingImpactModule.Tracker me = new LandingImpactModule.Tracker();
   private final Map<Integer, LandingImpactModule.Tracker> others = new HashMap<>();
   private final List<LandingImpactModule.Pending> pending = new ArrayList<>();
   private final EffectEngine<LandingFx> previewEngine = new EffectEngine<>("Landing Impact preview");
   private final FxParticles previewParticles = new FxParticles(768, true);
   private final FxFrame previewFrame = new FxFrame();
   private float previewClock;
   private boolean previewLanded;
   private boolean previewJumped;
   private int previewForce;
   private static final float PREVIEW_JUMP_AT = 0.35F;
   private static final float PREVIEW_AIR = 0.62F;
   private static final float PREVIEW_HEIGHT = 1.1F;
   private final Module.StartingLooks looks = styleLooks(new Supplier<List<SliderSetting>>() {
      public List<SliderSetting> get() {
         return LandingImpactModule.this.getSelectedStyle().lookTargets();
      }
   });
   private float frameDt;
   private long frameNow;
   private final FxPasses.Clock passClock = new FxPasses.Clock();
   private double costSum;
   private int costFrames;
   private float costPeak;
   private float lastSetupMillis;
   public static boolean farForTest;

   public LandingImpactModule() {
      super("landing_impact", "Landing Impact", "An effect on the ground where you (or others) land or jump.", true);
      this.style = this.add(
         new ModeSetting<>("style", "Style", "The look of the effect.", LandingStyle.class, LandingStyle.JUMP_CIRCLES)
            .style(ModeSetting.Style.CARDS)
            .group("Style")
      );
      this.trigger = this.add(
         new ModeSetting<>(
               "trigger",
               "Trigger",
               "When the effect plays: where you land, where you jump (the moment your feet leave the ground), or both.",
               LandingImpactModule.Trigger.class,
               LandingImpactModule.Trigger.LANDING
            )
            .<ModeSetting>group("Style")
            .keywords(new String[]{"jump", "on jump", "takeoff", "when"})
      );
      LandingStyle[] types = LandingStyle.values();
      this.styles = new LandingStyleBase[types.length];

      for (LandingStyle type : types) {
         LandingStyleBase s;
         this.styles[type.ordinal()] = s = type.create(this);

         for (Setting setting : s.getSettings()) {
            this.add(setting);
         }
      }

      BooleanSupplier fade = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return LandingImpactModule.this.style.get().lifecycle() == Lifecycle.FADE;
         }
      };
      BooleanSupplier animation = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return LandingImpactModule.this.style.get().lifecycle() == Lifecycle.ANIMATION;
         }
      };
      BooleanSupplier everyone = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return LandingImpactModule.this.showFor.get() == LandingImpactModule.ShowFor.ALL_PLAYERS;
         }
      };
      BooleanSupplier scaling = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return LandingImpactModule.this.scaleWithFall.get();
         }
      };
      this.showFor = this.add(
         new ModeSetting<>(
               "showFor", "Show For", "Whose landings (and jumps) get an effect.", LandingImpactModule.ShowFor.class, LandingImpactModule.ShowFor.ONLY_ME
            )
            .group("Who")
      );
      this.maxDistance = this.add(
         new SliderSetting("maxDistance", "Max Distance", "Other players farther away than this get no effect.", 8.0, 64.0, 32.0, 1.0)
            .unit(" blocks")
            .<SliderSetting>group("Who")
            .visibleWhen(everyone)
      );
      this.firstPerson = this.add(
         new BooleanSetting("firstPerson", "Show Mine In First Person", "Your own landings while you look through your own eyes.", true).group("Who")
      );
      BooleanSupplier landings = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return LandingImpactModule.this.trigger.get().landing();
         }
      };
      BooleanSupplier jumps = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return LandingImpactModule.this.trigger.get().jump();
         }
      };
      this.minAirTime = this.add(
         new SliderSetting(
               "minAirTime",
               "Min Air Time",
               "Ticks in the air before a landing counts. 5 skips stairs and slabs but keeps jumps and 1-block drops.",
               1.0,
               20.0,
               5.0,
               1.0
            )
            .unit(" ticks")
            .<SliderSetting>group("Detection")
            .visibleWhen(landings)
      );
      this.minFallHeight = this.add(
         new SliderSetting("minFallHeight", "Min Fall Height", "Only landings from at least this high.", 0.0, 10.0, 0.0, 0.25)
            .unit(" blocks")
            .<SliderSetting>group("Detection")
            .visibleWhen(landings)
      );
      this.edgeJumps = this.add(
         new BooleanSetting(
               "edgeJumps",
               "Also When Walking Off Edges",
               "Walking off a ledge counts as a jump too (a step down a stair or slab never does). Off: only real jumps, leaving the ground moving up.",
               false
            )
            .<BooleanSetting>group("Jump")
            .<BooleanSetting>visibleWhen(jumps)
            .keywords(new String[]{"ledge", "edge", "fall off"})
      );
      this.ignoreKnockback = this.add(
         new BooleanSetting("ignoreKnockback", "Ignore Knockback", "No jump effect within about 150 ms of being hit (a hit can knock you into the air).", true)
            .<BooleanSetting>group("Jump")
            .<BooleanSetting>visibleWhen(jumps)
            .keywords(new String[]{"hit", "knocked", "kb"})
      );
      this.jumpStrength = this.add(
         new SliderSetting("jumpStrength", "Jump Strength", "How big and strong a jump's effect is, compared with a landing's.", 10.0, 200.0, 70.0, 5.0)
            .unit("%")
            .<SliderSetting>group("Jump")
            .visibleWhen(jumps)
      );
      this.sprintBoost = this.add(
         new BooleanSetting("sprintBoost", "Bigger When Sprint-Jumping", "A sprint-jump's effect is a quarter bigger.", true)
            .<BooleanSetting>group("Jump")
            .<BooleanSetting>visibleWhen(jumps)
            .keywords(new String[]{"sprint"})
      );
      this.jumpCooldown = this.add(
         new SliderSetting(
               "jumpCooldown",
               "Cooldown",
               "The least time between two jump effects of the same player, so bunny hopping doesn't spam them.",
               0.0,
               2.0,
               0.3,
               0.05
            )
            .unit(" s")
            .<SliderSetting>group("Jump")
            .<SliderSetting>visibleWhen(jumps)
            .keywords(new String[]{"bunny hop", "spam"})
      );
      this.scaleWithFall = this.add(
         new BooleanSetting("scaleWithFall", "Scale With Fall Height", "Bigger and brighter the higher the fall (about x0.85 for a jump).", true)
            .group("Detection")
      );
      this.maxScale = this.add(
         new SliderSetting("maxScale", "Max Scale", "The size a fall of 10 blocks or more reaches.", 1.0, 2.5, 1.6, 0.05)
            .unit("x")
            .<SliderSetting>group("Detection")
            .visibleWhen(scaling)
      );
      this.size = this.add(new SliderSetting("size", "Size", "Scales the whole effect.", 0.5, 2.0, 1.0, 0.05).unit("x").group("Look"));
      this.duration = this.add(
         new SliderSetting("duration", "Duration", "How long the effect lasts before it fades.", 0.5, 2.0, 1.0, 0.05)
            .unit("x")
            .<SliderSetting>group("Look")
            .visibleWhen(fade)
      );
      this.speed = this.add(
         new SliderSetting("speed", "Speed", "How fast the animation plays. It always plays to the end.", 0.5, 2.0, 1.0, 0.05)
            .unit("x")
            .<SliderSetting>group("Look")
            .visibleWhen(animation)
      );
      this.opacity = this.add(new SliderSetting("opacity", "Opacity", "How strong the effect is.", 0.1, 1.0, 1.0, 0.05).group("Look"));
      this.density = this.add(new SliderSetting("density", "Density", "How many particles and pieces.", 0.25, 2.0, 1.0, 0.05).unit("x").group("Look"));
      this.conform = this.add(
         new BooleanSetting("conform", "Conform To Terrain", "Ground effects bend over steps and slabs. Off: a flat plane at the landing height.", true)
            .group("Look")
      );
      this.maxEffects = this.add(
         new SliderSetting("maxEffects", "Max Effects At Once", "Landing effects on screen at the same time.", 4.0, 64.0, 32.0, 1.0).group("Limits")
      );
      this.lighting = new WorldLight("Solid materials, paper and drawn strokes", "Bramble Bloom, Ink Blot, Shadow Pool, and Sketch Rings with Pencil");
      this.add(this.lighting.match);
      this.add(this.lighting.minimum);
      this.add(this.lighting.night);
      this.add(SignatureColorsWidget.setting(new SignatureColorsWidget.Source() {
         @Override
         public Signature selectedSignature() {
            return LandingImpactModule.this.getSelectedStyle().signature();
         }

         @Override
         public String selectedStyleName() {
            return LandingImpactModule.this.style.get().displayName();
         }
      }));
      this.dummyJumps = this.add(
         new BooleanSetting(
               "dummyJumps",
               "Test Dummy Jumps",
               "Your test dummy (/furiah dummy) jumps every 2 seconds. Set Show For to All Players to see its effects.",
               false
            )
            .group("Testing")
      );
      this.dummyJumps.addListener(new Runnable() {
         @Override
         public void run() {
            TestDummy.setJumping(LandingImpactModule.this.dummyJumps.get());
         }
      });
      Runnable restart = new Runnable() {
         @Override
         public void run() {
            LandingImpactModule.this.restartPreview();
         }
      };
      this.style.addListener(restart);
      this.trigger.addListener(restart);
   }

   public LandingStyleBase getStyle(LandingStyle type) {
      return this.styles[type.ordinal()];
   }

   @Override
   public Module.StartingLooks startingLooks() {
      return this.looks;
   }

   public LandingStyleBase getSelectedStyle() {
      return this.styles[this.style.get().ordinal()];
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getMinecraft();
      EntityPlayerSP self = mc.thePlayer;
      WorldClient world = mc.theWorld;
      if (self != null && world != null) {
         this.tickCounter++;
         if (this.testAt != 0L && GameClock.millis() >= this.testAt && mc.currentScreen == null) {
            this.testAt = 0L;
            if (this.trigger.get() == LandingImpactModule.Trigger.JUMP) {
               this.playJumpTest(mc);
            } else {
               this.playTest(mc);
            }
         }

         this.track(world, self, this.me, true);
         if (this.showFor.get() == LandingImpactModule.ShowFor.ALL_PLAYERS) {
            double max = this.maxDistance.get();

            for (int i = 0; i < world.playerEntities.size(); i++) {
               EntityPlayer p = (EntityPlayer)world.playerEntities.get(i);
               if (p instanceof EntityOtherPlayerMP && !(p.getDistanceSqToEntity(self) > max * max)) {
                  LandingImpactModule.Tracker t = this.others.get(p.getEntityId());
                  if (t == null) {
                     t = new LandingImpactModule.Tracker();
                     this.others.put(p.getEntityId(), t);
                  }

                  t.seen = this.tickCounter;
                  this.track(world, p, t, false);
               }
            }

            Iterator<LandingImpactModule.Tracker> it = this.others.values().iterator();

            while (it.hasNext()) {
               if (it.next().seen != this.tickCounter) {
                  it.remove();
               }
            }
         } else if (!this.others.isEmpty()) {
            this.others.clear();
         }
      }
   }

   private static boolean inWeb(World world, EntityPlayer p) {
      BlockPos feet = new BlockPos(p.posX, p.posY + 0.1, p.posZ);
      return world.getBlockState(feet).getBlock() == Blocks.web
         || world.getBlockState(feet.up()).getBlock() == Blocks.web;
   }

   private void track(World world, EntityPlayer p, LandingImpactModule.Tracker t, boolean self) {
      boolean ground = p.onGround;
      double y = self ? p.posY : p.serverPosY / 32.0;
      double x = self ? p.posX : p.serverPosX / 32.0;
      double z = self ? p.posZ : p.serverPosZ / 32.0;
      boolean spoil = p.isInWater()
         || p.isInLava()
         || p.isOnLadder()
         || p.ridingEntity != null
         || p.isSpectator()
         || p.capabilities.isFlying
         || inWeb(world, p);
      boolean teleported = !Double.isNaN(t.lastY) && (Math.abs(x - t.lastX) > 4.0 || Math.abs(z - t.lastZ) > 4.0 || y - t.lastY > 2.5 || t.lastY - y > 8.0);
      if (!Double.isNaN(t.lastY) && !teleported) {
         double moved = y - t.lastY;
         if (!self) {
            if (ground) {
               t.sawGround = true;
            } else if (!t.sawGround) {
               ground = resting(world, p, t, x, y, z, moved);
            }
         }

         boolean wasFalling = t.falling;
         if (moved < -1.0E-4) {
            t.falling = true;
         } else if (moved > 1.0E-4) {
            t.falling = false;
         }

         this.trackTakeoff(world, p, t, self, ground, x, y, z, spoil);
         if (!ground) {
            if (t.wasOnGround) {
               t.airTicks = 0;
               t.peakY = Math.max(t.groundY, y);
               t.spoiled = false;
            }

            t.airTicks++;
            t.peakY = Math.max(t.peakY, y);
            if (spoil) {
               t.spoiled = true;
            }

            double half;
            double surface;
            if (!self
               && wasFalling
               && Math.abs(moved) < 1.0E-4
               && t.airTicks >= 3
               && !Double.isNaN(surface = Terrain.standingSurface(world, x - (half = p.width / 2.0F), z - half, x + half, z + half, y))
               && Math.abs(y - surface) < 0.05) {
               this.landed(world, p, t, self, y, spoil);
               ground = true;
            }
         } else if (!t.wasOnGround) {
            this.landed(world, p, t, self, y, spoil);
         }

         if (ground) {
            t.groundY = y;
            t.falling = false;
         }

         t.lastY = y;
         t.lastX = x;
         t.lastZ = z;
         t.wasOnGround = ground;
      } else {
         t.lastY = y;
         t.lastX = x;
         t.lastZ = z;
         t.groundY = y;
         t.peakY = y;
         t.airTicks = 0;
         t.falling = false;
         t.spoiled = false;
         t.takeoffWait = 0;
         t.wasOnGround = ground && !teleported;
      }
   }

   private void trackTakeoff(
      World world, EntityPlayer p, LandingImpactModule.Tracker t, boolean self, boolean ground, double x, double y, double z, boolean spoil
   ) {
      if (t.wasOnGround && !ground) {
         t.takeoffX = t.lastX;
         t.takeoffZ = t.lastZ;
         t.takeoffY = t.groundY;
         t.takeoffSprint = p.isSprinting();
         t.takeoffKnocked = knocked(p);
         t.takeoffSpoiled = spoil;
         t.takeoffWait = 3;
      }

      if (t.takeoffWait > 0) {
         t.takeoffSpoiled |= spoil;
         t.takeoffKnocked = t.takeoffKnocked | knocked(p);
         double rise = y - t.takeoffY;
         if (rise > 0.05) {
            t.takeoffWait = 0;
            this.jumped(world, p, t, self);
         } else if (rise < -0.05) {
            t.takeoffWait = 0;
            if (this.edgeJumps.get() && !stepDown(world, p, t, x, z)) {
               this.jumped(world, p, t, self);
            }
         } else {
            t.takeoffWait--;
         }
      }
   }

   private static boolean stepDown(World world, EntityPlayer p, LandingImpactModule.Tracker t, double x, double z) {
      double half = p.width / 2.0F;
      double below = Terrain.standingSurface(world, x - half, z - half, x + half, z + half, t.takeoffY - 0.1);
      return !Double.isNaN(below) && below >= t.takeoffY - 0.6;
   }

   private static boolean resting(World world, EntityPlayer p, LandingImpactModule.Tracker t, double x, double y, double z, double moved) {
      if (!(moved > 1.0E-4) || t.wasOnGround && moved <= 0.6) {
         double half = p.width / 2.0F;
         double surface = Terrain.standingSurface(world, x - half, z - half, x + half, z + half, y);
         return !Double.isNaN(surface) && Math.abs(y - surface) < 0.05;
      } else {
         return false;
      }
   }

   private static boolean knocked(EntityPlayer p) {
      return p.hurtTime > 0 && p.maxHurtTime > 0 && p.maxHurtTime - p.hurtTime <= 3;
   }

   private void jumped(World world, EntityPlayer p, LandingImpactModule.Tracker t, boolean self) {
      LandingImpactModule.Trigger tr = this.trigger.get();
      if (tr.jump() && !t.takeoffSpoiled && (!this.ignoreKnockback.get() || !t.takeoffKnocked) && !p.isInvisible() && !p.isSpectator()) {
         long now = GameClock.nanos();
         if (t.lastJumpEffect == 0L || now - t.lastJumpEffect >= (long)(this.jumpCooldown.get() * 1.0E9)) {
            if (tr != LandingImpactModule.Trigger.BOTH || t.lastLandingEffect == 0L || now - t.lastLandingEffect >= 150000000L) {
               t.lastJumpEffect = now;
               double half = p.width / 2.0F;
               double surface = Terrain.standingSurface(world, t.takeoffX - half, t.takeoffZ - half, t.takeoffX + half, t.takeoffZ + half, t.takeoffY);
               if (Double.isNaN(surface)) {
                  surface = t.takeoffY;
               }

               float strength = this.jumpStrength.getFloat() / 100.0F * (this.sprintBoost.get() && t.takeoffSprint ? 1.25F : 1.0F);
               this.pending.add(new LandingImpactModule.Pending(p, self, surface, 1.25F, now, true, strength, t.takeoffX, t.takeoffZ));
            }
         }
      }
   }

   private void landed(World world, EntityPlayer p, LandingImpactModule.Tracker t, boolean self, double y, boolean spoilNow) {
      int airTime = t.airTicks + 1;
      double fall = t.peakY - y;
      boolean rose = t.peakY > t.groundY + 0.2;
      if (!t.spoiled && !spoilNow && !p.isInvisible() && !p.isSpectator()) {
         if (airTime >= this.minAirTime.getInt() && !(fall < this.minFallHeight.get()) && (rose || !(fall < 0.6))) {
            double x = self ? p.posX : p.serverPosX / 32.0;
            double z;
            double half;
            double surface = Terrain.standingSurface(
               world, x - (half = p.width / 2.0F), (z = self ? p.posZ : p.serverPosZ / 32.0) - half, x + half, z + half, y
            );
            if (Double.isNaN(surface)) {
               surface = y;
            }

            if (this.trigger.get().landing()) {
               t.lastLandingEffect = GameClock.nanos();
               this.pending.add(new LandingImpactModule.Pending(p, self, surface, (float)Math.max(0.0, fall), GameClock.nanos()));
            }
         }
      }
   }

   private float fallScale(float fall) {
      if (!this.scaleWithFall.get()) {
         return 1.0F;
      } else {
         float t = Easing.clamp01((fall - 1.25F) / 8.75F);
         return 0.85F + (this.maxScale.getFloat() - 0.85F) * (float)Math.sqrt(t);
      }
   }

   private boolean spawn(LandingContext land, boolean preview) {
      long started = System.nanoTime();
      boolean added = this.spawnNow(land, preview);
      if (added && !preview) {
         this.lastSetupMillis = (float)(System.nanoTime() - started) / 1000000.0F;
      }

      return added;
   }

   private boolean spawnNow(LandingContext land, boolean preview) {
      EffectEngine<LandingFx> target = preview ? this.previewEngine : this.engine;
      FxParticles pool = preview ? this.previewParticles : this.particles;
      LandingStyleBase s = this.getSelectedStyle();
      target.setLimits(this.maxEffects.getInt(), 4);
      if (!target.hasRoom(land.playerId)) {
         return false;
      } else {
         float timeScale = s.lifecycle() == Lifecycle.FADE ? this.duration.getFloat() : 1.0F / this.speed.getFloat();
         float k = land.fallScale = land.jump ? this.fallScale(1.25F) * land.strength : this.fallScale(land.fallHeight);
         float opacityK = Math.min(1.0F, 0.75F + 0.18F * k);
         int seed = Rng.mix(this.seedCounter++ * -1640531535 ^ (int)System.nanoTime());
         LandingFx fx = new LandingFx(
            s,
            land,
            Anchor.world(land.x, land.y, land.z),
            s.baseSeconds() * timeScale,
            timeScale,
            seed,
            pool,
            this.size.getFloat() * k,
            this.opacity.getFloat() * opacityK,
            this.density.getFloat() * Math.max(1.0F, k),
            preview
         );
         Minecraft mc = Minecraft.getMinecraft();
         WorldClient world = preview ? null : mc.theWorld;
         double reach = s.reach() * fx.size + 0.6;
         Terrain.HeightField heightField = fx.ground = this.conform.get() && world != null
            ? Terrain.HeightField.sample(world, land.x, land.y, land.z, reach, 2)
            : Terrain.HeightField.flat(land.y);
         if (!preview) {
            Entity viewer = mc.getRenderViewEntity();
            if (farForTest || viewer != null && viewer.getDistanceSq(land.x, land.y, land.z) > 256.0) {
               fx.lod = true;
            }
         }

         return target.add(fx);
      }
   }

   private LandingContext context(World world, EntityPlayer p, boolean self, double surface, float fall, float pt) {
      return this.context(world, p, self, surface, fall, pt, Double.NaN, Double.NaN);
   }

   private LandingContext context(World world, EntityPlayer p, boolean self, double surface, float fall, float pt, double atX, double atZ) {
      LandingContext land = new LandingContext();
      land.player = p;
      land.playerId = p.getEntityId();
      land.self = self;
      land.x = Double.isNaN(atX) ? p.lastTickPosX + (p.posX - p.lastTickPosX) * pt : atX;
      land.z = Double.isNaN(atZ) ? p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt : atZ;
      land.y = surface;
      land.fallHeight = fall;
      land.pose.capture(p, pt);
      land.pose.y = surface;
      land.yaw = land.pose.bodyYaw;
      land.blockColor = Terrain.mapColor(world, land.x, surface, land.z, -7697782);
      return land;
   }

   @Override
   public void onPrepareWorld(float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      long now = GameClock.nanos();
      float dt = this.lastFrameNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = now;
      this.frameDt = dt;
      this.frameNow = now;
      WorldClient world = mc.theWorld;

      for (int i = this.pending.size() - 1; i >= 0; i--) {
         LandingImpactModule.Pending p = this.pending.get(i);
         EntityPlayer pl = p.player;
         if (!pl.isDead && pl.worldObj == world) {
            double drawnY = pl.lastTickPosY + (pl.posY - pl.lastTickPosY) * partialTicks;
            boolean ready = p.jump ? drawnY >= p.surface + 0.05 : drawnY <= p.surface + 0.03;
            if (ready || now - p.since > 200000000L) {
               this.pending.remove(i);
               if (!p.self || mc.gameSettings.thirdPersonView != 0 || mc.getRenderViewEntity() != pl || this.firstPerson.get()) {
                  LandingContext land = this.context(world, pl, p.self, p.surface, p.fall, partialTicks, p.x, p.z);
                  land.jump = p.jump;
                  land.strength = p.strength;
                  if (this.spawn(land, false)) {
                     if (p.jump) {
                        this.jumpCount++;
                        this.lastJumpScale = land.fallScale;
                        if (!p.self) {
                           this.otherJumpCount++;
                        }
                     } else {
                        this.spawnCount++;
                        this.lastFall = p.fall;
                        this.lastScale = this.fallScale(p.fall);
                        if (!p.self) {
                           this.otherSpawnCount++;
                           this.lastModelGap = drawnY - p.surface;
                        }
                     }
                  }
               }
            }
         } else {
            this.pending.remove(i);
         }
      }

      this.engine.update(dt, partialTicks, world);
      this.particles.update(dt);
      boolean passes = false;

      for (int ix = 0; ix < this.engine.size() && !passes; ix++) {
         passes = this.engine.list().get(ix).passes() != 0;
      }

      if (passes) {
         long start = System.nanoTime();
         this.camera.captureWorld();
         this.fillFrame(mc, partialTicks, dt, now, world);
         this.lighting.frame(world, partialTicks);

         for (int ix = 0; ix < this.engine.size(); ix++) {
            LandingFx fx = this.engine.list().get(ix);
            if (fx.passes() != 0) {
               fx.passFrame = this.frame;
               float level = this.lighting.level(world, fx.x(), fx.y() + 0.5, fx.z());
               fx.light = this.lighting.light(level);
               fx.night = this.lighting.night(level);
               FxPasses.submit(fx, this.passClock);
            }
         }

         this.passClock.nanos = this.passClock.nanos + (System.nanoTime() - start);
      }
   }

   @Override
   public void onRenderWorld(float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      float passMillis = this.passClock.takeMillis();
      if (!this.engine.isEmpty() || !this.particles.isEmpty()) {
         long start = System.nanoTime();
         Textures.ensureLoaded();
         RenderState.begin();

         try {
            this.camera.captureModelView();
            Vfx.setCamera(this.camera);
            FxFrame f = this.frame;
            this.fillFrame(mc, partialTicks, this.frameDt, this.frameNow, mc.theWorld);
            this.engine.render(f);
            this.particles.render(f.viewX, f.viewY, f.viewZ);
         } finally {
            RenderState.end();
         }

         float var10 = (float)(System.nanoTime() - start) / 1000000.0F + passMillis;
         float f = this.renderMillis = this.renderMillis == 0.0F ? var10 : this.renderMillis + (var10 - this.renderMillis) * 0.05F;
         if (!this.engine.isEmpty()) {
            this.costSum += var10;
            this.costFrames++;
            this.costPeak = Math.max(this.costPeak, var10);
         }
      }
   }

   public void resetCostForTest() {
      this.costSum = 0.0;
      this.costFrames = 0;
      this.costPeak = 0.0F;
   }

   public float averageCostForTest() {
      return this.costFrames == 0 ? 0.0F : (float)(this.costSum / this.costFrames);
   }

   public float peakCostForTest() {
      return this.costPeak;
   }

   public float getLastSetupMillis() {
      return this.lastSetupMillis;
   }

   public List<LandingFx> getEffectsForTest() {
      return this.engine.list();
   }

   public boolean landForTest(double x, double y, double z, float fall, boolean jump, float strength, int id, boolean self) {
      Minecraft mc = Minecraft.getMinecraft();
      EntityPlayerSP p = mc.thePlayer;
      if (p != null && mc.theWorld != null) {
         double surface = Terrain.standingSurface(mc.theWorld, x - 0.3, z - 0.3, x + 0.3, z + 0.3, y);
         if (Double.isNaN(surface)) {
            surface = y;
         }

         LandingContext land = this.context(mc.theWorld, p, self, surface, fall, 1.0F, x, z);
         land.playerId = id;
         land.jump = jump;
         land.strength = jump ? strength : 1.0F;
         return this.spawn(land, false);
      } else {
         return false;
      }
   }

   private void fillFrame(Minecraft mc, float partialTicks, float dt, long now, World world) {
      FxFrame f = this.frame;
      f.cam = this.camera;
      f.viewX = mc.getRenderManager().viewerPosX;
      f.viewY = mc.getRenderManager().viewerPosY;
      f.viewZ = mc.getRenderManager().viewerPosZ;
      Entity v = (Entity)(mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.thePlayer);
      if (v != null) {
         f.eyeX = v.lastTickPosX + (v.posX - v.lastTickPosX) * partialTicks;
         f.eyeY = v.lastTickPosY + (v.posY - v.lastTickPosY) * partialTicks + v.getEyeHeight();
         f.eyeZ = v.lastTickPosZ + (v.posZ - v.lastTickPosZ) * partialTicks;
      }

      f.partialTicks = partialTicks;
      f.dt = dt;
      f.now = now;
      f.frameId = FxFrame.currentFrame();
      f.preview = false;
      f.world = world;
      f.firstPerson = mc.gameSettings.thirdPersonView == 0 && v == mc.thePlayer;
   }

   @Override
   public void onWorldChange() {
      this.engine.clear();
      this.particles.clear();
      this.pending.clear();
      this.others.clear();
      this.me.lastY = Double.NaN;
      this.me.wasOnGround = true;
      this.lastFrameNanos = 0L;
   }

   @Override
   protected void onDisable() {
      this.onWorldChange();
   }

   public float getRenderMillis() {
      return this.renderMillis;
   }

   public int getActiveEffectCount() {
      return this.engine.size();
   }

   public int getSpawnCount() {
      return this.spawnCount;
   }

   public int getOtherSpawnCount() {
      return this.otherSpawnCount;
   }

   public float getLastFall() {
      return this.lastFall;
   }

   public float getLastScale() {
      return this.lastScale;
   }

   public double getLastModelGap() {
      return this.lastModelGap;
   }

   public int getJumpCount() {
      return this.jumpCount;
   }

   public int getOtherJumpCount() {
      return this.otherJumpCount;
   }

   public float getLastJumpScale() {
      return this.lastJumpScale;
   }

   public float normalJumpScaleForTest() {
      return this.fallScale(1.25F);
   }

   public int previewEffectsForTest() {
      return this.previewEngine.size();
   }

   @Override
   public boolean hasTestAction() {
      return true;
   }

   @Override
   public String getTestTooltip() {
      return "Closes the menu, then plays the style at your feet";
   }

   @Override
   public void runTest() {
      this.testAt = GameClock.millis() + 250L;
   }

   public void playTest(Minecraft mc) {
      EntityPlayerSP p = mc.thePlayer;
      if (p != null && mc.theWorld != null) {
         double half = p.width / 2.0F;
         double surface = Terrain.standingSurface(
            mc.theWorld, p.posX - half, p.posZ - half, p.posX + half, p.posZ + half, p.posY
         );
         if (Double.isNaN(surface)) {
            surface = p.posY;
         }

         this.spawn(this.context(mc.theWorld, p, true, surface, 1.25F, 1.0F), false);
      }
   }

   public void playJumpTest(Minecraft mc) {
      EntityPlayerSP p = mc.thePlayer;
      if (p != null && mc.theWorld != null) {
         double half = p.width / 2.0F;
         double surface = Terrain.standingSurface(
            mc.theWorld, p.posX - half, p.posZ - half, p.posX + half, p.posZ + half, p.posY
         );
         if (Double.isNaN(surface)) {
            surface = p.posY;
         }

         LandingContext land = this.context(mc.theWorld, p, true, surface, 1.25F, 1.0F);
         land.jump = true;
         land.strength = this.jumpStrength.getFloat() / 100.0F;
         this.spawn(land, false);
      }
   }

   @Override
   public int getAccentColor(double time) {
      return this.getSelectedStyle().color.getDisplayColor();
   }

   @Override
   public int getOptionColor(Setting setting, Enum<?> option, double time) {
      return setting == this.style && option instanceof LandingStyle ? this.getStyle((LandingStyle)option).color.getDisplayColor() : 0;
   }

   @Override
   public boolean hasPreview() {
      return true;
   }

   @Override
   public String getPreviewLabel() {
      return this.style.get().displayName();
   }

   @Override
   public String getPreviewInfo() {
      return this.renderMillis <= 0.0F ? null : String.format(Locale.ROOT, "%.2f ms / frame", this.renderMillis);
   }

   @Override
   public void restartPreview() {
      this.previewEngine.clear();
      this.previewParticles.clear();
      this.previewClock = 0.0F;
      this.previewLanded = false;
      this.previewJumped = false;
      this.previewForce = 0;
   }

   @Override
   public String[] getPreviewTests() {
      return new String[]{"Test landing", "Test jump"};
   }

   @Override
   public String getPreviewTestTooltip(int index) {
      return index == 0
         ? "The mannequin jumps, and the style plays where it lands"
         : "The mannequin jumps, and the style plays where it leaves the ground (at Jump Strength)";
   }

   @Override
   public void runPreviewTest(int index) {
      this.restartPreview();
      this.previewForce = index == 0 ? 1 : 2;
      this.previewClock = 0.29999998F;
   }

   private LandingContext previewContext(boolean jump) {
      LandingContext land = new LandingContext();
      land.playerId = -2;
      land.fallHeight = 1.25F;
      land.pose.copy(PoseSnapshot.PREVIEW_MANNEQUIN);
      land.pose.y = 0.0;
      land.blockColor = -8408520;
      land.jump = jump;
      land.strength = jump ? this.jumpStrength.getFloat() / 100.0F : 1.0F;
      return land;
   }

   private float previewCycle() {
      LandingStyleBase s = this.getSelectedStyle();
      float timeScale = s.lifecycle() == Lifecycle.FADE ? this.duration.getFloat() : 1.0F / this.speed.getFloat();
      return 0.97F + Math.max(1.0F, s.baseSeconds() * timeScale + 0.45F);
   }

   @Override
   public double getPreviewModelOffsetY() {
      float u = (this.previewClock - 0.35F) / 0.62F;
      return !(u <= 0.0F) && !(u >= 1.0F) ? 4.4F * u * (1.0 - u) : 0.0;
   }

   @Override
   public void renderPreview(float dt) {
      this.previewClock += dt;
      if (this.previewClock >= this.previewCycle()) {
         this.previewClock = 0.0F;
         this.previewLanded = false;
         this.previewJumped = false;
         this.previewForce = 0;
      }

      LandingImpactModule.Trigger tr = this.trigger.get();
      boolean jumpFx = this.previewForce == 2 || this.previewForce == 0 && tr.jump();
      boolean landFx = this.previewForce == 1 || this.previewForce == 0 && tr.landing();
      if (!this.previewJumped && this.previewClock >= 0.35F) {
         this.previewJumped = true;
         if (jumpFx) {
            this.spawn(this.previewContext(true), true);
         }
      }

      if (!this.previewLanded && this.previewClock >= 0.97F) {
         this.previewLanded = true;
         if (landFx) {
            this.spawn(this.previewContext(false), true);
         }
      }

      this.previewEngine.update(dt, 1.0F, null);
      this.previewParticles.update(dt);
      Textures.ensureLoaded();
      RenderState.begin();

      try {
         this.camera.captureModelView();
         Vfx.setCamera(this.camera);
         FxFrame f = this.previewFrame;
         f.cam = this.camera;
         f.viewZ = 0.0;
         f.viewY = 0.0;
         f.viewX = 0.0;
         f.eyeX = this.camera.posX;
         f.eyeY = this.camera.posY;
         f.eyeZ = this.camera.posZ;
         f.partialTicks = 1.0F;
         f.dt = dt;
         f.now = ClientClock.nanos();
         f.frameId = FxFrame.currentFrame();
         f.preview = true;
         f.world = null;
         f.firstPerson = false;
         this.previewEngine.render(f);

         for (int i = 0; i < this.previewEngine.size(); i++) {
            LandingFx fx = this.previewEngine.list().get(i);
            fx.passFrame = f;
            fx.light = 1.0F;
            fx.night = this.lighting.previewNight();
         }

         FxPasses.drawNow(this.previewEngine.list());
         this.previewParticles.render(0.0, 0.0, 0.0);
      } finally {
         RenderState.end();
      }
   }

   private static final class Pending {
      final EntityPlayer player;
      final boolean self;
      final double surface;
      final float fall;
      final long since;
      final boolean jump;
      final float strength;
      final double x;
      final double z;

      Pending(EntityPlayer player, boolean self, double surface, float fall, long since) {
         this(player, self, surface, fall, since, false, 1.0F, Double.NaN, Double.NaN);
      }

      Pending(EntityPlayer player, boolean self, double surface, float fall, long since, boolean jump, float strength, double x, double z) {
         this.player = player;
         this.self = self;
         this.surface = surface;
         this.fall = fall;
         this.since = since;
         this.jump = jump;
         this.strength = strength;
         this.x = x;
         this.z = z;
      }
   }

   public static enum ShowFor implements Displayable {
      ONLY_ME("Only Me", "Just your own landings"),
      ALL_PLAYERS("All Players", "Everyone around you");

      private final String displayName;
      private final String description;

      private ShowFor(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   private static final class Tracker {
      boolean wasOnGround = true;
      int airTicks;
      double groundY;
      double peakY;
      double lastY = Double.NaN;
      double lastX;
      double lastZ;
      boolean falling;
      boolean spoiled;
      long seen;
      int takeoffWait;
      double takeoffX;
      double takeoffY;
      double takeoffZ;
      boolean takeoffSprint;
      boolean takeoffKnocked;
      boolean takeoffSpoiled;
      long lastJumpEffect;
      long lastLandingEffect;
      boolean sawGround;

      private Tracker() {
      }
   }

   public static enum Trigger implements Displayable {
      LANDING("On landing", "Where you land"),
      JUMP("On jump", "Where you jump, the moment your feet leave the ground"),
      BOTH("Both", "Where you jump and where you land");

      private final String displayName;
      private final String description;

      private Trigger(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }

      public boolean landing() {
         return this != JUMP;
      }

      public boolean jump() {
         return this != LANDING;
      }
   }
}
