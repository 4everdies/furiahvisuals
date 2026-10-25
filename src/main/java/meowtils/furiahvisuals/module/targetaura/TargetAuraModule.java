package meowtils.furiahvisuals.module.targetaura;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.fx.look.NightVisibility;
import meowtils.furiahvisuals.fx.look.WorldLight;
import meowtils.furiahvisuals.fx.pass.FxPass;
import meowtils.furiahvisuals.fx.pass.FxPasses;
import meowtils.furiahvisuals.fx.pass.PassItem;
import meowtils.furiahvisuals.fx.style.Signature;
import meowtils.furiahvisuals.fx.trail.LightCache;
import meowtils.furiahvisuals.gui.widget.SignatureColorsWidget;
import meowtils.furiahvisuals.gui.widget.StartingLookWidget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.ParticleSystem;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.Team.EnumVisible;
import net.minecraft.world.World;

public final class TargetAuraModule extends Module {
   public static final float INTRO_SECONDS = 0.6F;
   public final ModeSetting<EffectType> effect;
   public final SliderSetting range;
   public final SliderSetting aimTolerance;
   public final BooleanSetting ignoreTeammates;
   public final BooleanSetting ignoreNpcs;
   public final SliderSetting linger;
   public final SliderSetting fadeIn;
   public final SliderSetting fadeOut;
   public final BooleanSetting keepVisible;
   public final SliderSetting inWayOpacity;
   public final SliderSetting darkCap;
   public final BooleanSetting matchLighting;
   public final SliderSetting minBrightness;
   public final ModeSetting<NightVisibility> nightVisibility;
   private final AuraEffect[] effects;
   private final List<AuraState> auras = new ArrayList<>();
   private final Map<Integer, AuraState> byEntity = new HashMap<>();
   private final TargetFinder finder = new TargetFinder();
   private final ParticleSystem particles = new ParticleSystem(768);
   private final Camera camera = new Camera();
   private EffectContext[] contexts = new EffectContext[]{new EffectContext(), new EffectContext(), new EffectContext(), new EffectContext()};
   private int[] order = new int[4];
   private final LightCache lights = new LightCache();
   private long lastFrameNanos;
   private float renderMillis;
   private final FxPasses.Clock passClock = new FxPasses.Clock();
   private float preparedMillis = -1.0F;
   private TargetAuraModule.AuraItem[] items = new TargetAuraModule.AuraItem[4];
   private final TargetAuraModule.ParticlesItem particlesItem = new TargetAuraModule.ParticlesItem();
   private final ParticleSystem previewParticles = new ParticleSystem(256);
   private AuraState previewState;
   private final EffectContext[] previewContext = new EffectContext[]{new EffectContext()};
   private final AuraPreviews previews = new AuraPreviews(this);
   private final Module.StartingLooks looks = new Module.StartingLooks() {
      @Override
      public List<SliderSetting> targets() {
         return TargetAuraModule.this.getSelectedEffect().lookTargets();
      }

      @Override
      public double value(SliderSetting slider, int look) {
         return StartingLookWidget.value(slider, look);
      }

      @Override
      public String tooltip(int look) {
         return look == 0
            ? "Smaller, softer, calmer: the style's size, strength, density and motion a third of the way down (colors stay as they are)"
            : (
               look == 1
                  ? "The style's size, strength, density and motion at their defaults (colors stay as they are)"
                  : "Bigger, stronger, livelier: the style's size, strength, density and motion a third of the way up (colors stay as they are)"
            );
      }

      @Override
      public String after() {
         return "effect";
      }

      @Override
      public String rowTooltip() {
         return "A starting point for the picked style's size, strength, density and motion; colors, range and times are never changed";
      }
   };
   private final TargetFinder offFinder = new TargetFinder();
   private long offTick = Long.MIN_VALUE;
   private long offLastAim;

   public TargetAuraModule() {
      super("target_aura", "Target Aura", "A glowing effect around the player you are aiming at.", true);
      this.effect = this.add(new AuraEffectSetting(this).group("Effect"));
      EffectType[] types = EffectType.values();
      this.effects = new AuraEffect[types.length];

      for (EffectType type : types) {
         AuraEffect fx;
         this.effects[type.ordinal()] = fx = type.create(this);

         for (Setting s : fx.getSettings()) {
            this.add(s);
         }
      }

      this.range = this.add(
         new SliderSetting("range", "Range", "How far away a player can be. 3.0 is vanilla reach.", 3.0, 8.0, 3.0, 0.1).unit(" blocks").group("Targeting")
      );
      this.aimTolerance = this.add(
         new SliderSetting("aimTolerance", "Aim Tolerance", "Expands hitboxes a little so fast flicks still count.", 0.0, 0.5, 0.1, 0.01)
            .unit(" blocks")
            .group("Targeting")
      );
      this.ignoreTeammates = this.add(
         new BooleanSetting("ignoreTeammates", "Ignore Teammates", "No aura on players of your scoreboard team or name color.", false).group("Targeting")
      );
      this.ignoreNpcs = this.add(new BooleanSetting("ignoreNpcs", "Ignore NPCs", "No aura on server NPCs and bots.", true).group("Targeting"));
      this.linger = this.add(
         new SliderSetting("linger", "Linger", "How long the aura stays after you look away.", 0.0, 3.0, 1.0, 0.05).unit("s").group("Timing")
      );
      this.fadeIn = this.add(new SliderSetting("fadeIn", "Fade In", "Fade-in duration.", 0.05, 1.0, 0.15, 0.01).unit("s").group("Timing"));
      this.fadeOut = this.add(new SliderSetting("fadeOut", "Fade Out", "Fade-out duration.", 0.05, 1.5, 0.3, 0.01).unit("s").group("Timing"));
      this.add(SignatureColorsWidget.setting(new SignatureColorsWidget.Source() {
         @Override
         public Signature selectedSignature() {
            return TargetAuraModule.this.getSelectedEffect().signature();
         }

         @Override
         public String selectedStyleName() {
            return TargetAuraModule.this.effect.get().displayName();
         }
      }));
      this.keepVisible = this.add(
         new BooleanSetting(
               "keepVisible",
               "Keep the Target Visible",
               "The aura frames the player and never hides them: solid parts that pass between you and the target fade while they're in the way, and dark effects stay see-through in front of their body.",
               true
            )
            .<BooleanSetting>group("Advanced")
            .keywords(new String[]{"visible", "hide", "hidden", "see", "fade", "readability", "transparent", "block view"})
      );
      this.inWayOpacity = this.add(
         new SliderSetting(
               "keepVisible.opacity",
               "Opacity in the Way",
               "How much of a solid part still shows while it is between you and the target.",
               0.0,
               100.0,
               30.0,
               5.0
            )
            .unit("%")
            .<SliderSetting>group("Advanced")
            .visibleWhen(on(this.keepVisible))
      );
      this.darkCap = this.add(
         new SliderSetting(
               "keepVisible.dark",
               "Dark Effects in Front",
               "The most a darkening effect (Shadow Veil's smoke) covers the target's body.",
               10.0,
               100.0,
               35.0,
               5.0
            )
            .unit("%")
            .<SliderSetting>group("Advanced")
            .visibleWhen(on(this.keepVisible))
      );
      this.matchLighting = this.add(
         new BooleanSetting(
               "matchLighting",
               "Match World Lighting",
               "Solid materials and drawn strokes take the light where the target stands (block light and daylight), so a gold aura doesn't glow in a dark cave. Glowing parts stay as bright as ever.",
               true
            )
            .<BooleanSetting>group("Advanced")
            .keywords(new String[]{"light", "lighting", "dark", "cave", "night", "brightness", "lit"})
      );
      this.minBrightness = this.add(
         new SliderSetting(
               "minBrightness", "Minimum Brightness", "How dark materials and strokes may get in the dark, so they always stay visible.", 0.0, 100.0, 25.0, 5.0
            )
            .unit("%")
            .<SliderSetting>group("Advanced")
            .visibleWhen(on(this.matchLighting))
      );
      this.nightVisibility = this.add(
         new ModeSetting<>(
               "nightVisibility",
               "Night Visibility for Dark Styles",
               "Dark styles (Ink Enso, Thorn Crown, Shadow Veil, and Doodle with Pencil) get a faint light halo or rim so they read in the dark. Auto: only while the target stands in the dark.",
               NightVisibility.class,
               NightVisibility.AUTO
            )
            .<ModeSetting>group("Advanced")
            .keywords(new String[]{"night", "dark", "halo", "rim", "visible", "visibility"})
      );
      this.effect.addListener(new Runnable() {
         @Override
         public void run() {
            TargetAuraModule.this.restartPreview();
         }
      });
   }

   public AuraEffect getEffect(EffectType type) {
      return this.effects[type.ordinal()];
   }

   public AuraEffect getSelectedEffect() {
      return this.effects[this.effect.get().ordinal()];
   }

   AuraPreviews previews() {
      return this.previews;
   }

   @Override
   public Module.StartingLooks startingLooks() {
      return this.looks;
   }

   void drawCard(EffectType type, EffectContext[] list, AuraState s, ParticleSystem cardParticles, Camera cardCamera, float dt, long now) {
      EffectContext c = list[0];
      c.preview = true;
      this.fill(c, s, now, dt, ClientClock.seconds());
      c.cam = cardCamera;
      c.z = 0.0;
      c.y = 0.0;
      c.x = 0.0;
      c.groundY = 0.0;
      c.groundAlpha = 1.0F;
      c.worldGroundY = 0.0;
      c.worldZ = 0.0;
      c.worldY = 0.0;
      c.worldX = 0.0;
      c.width = 0.6F;
      c.height = 1.8F;
      c.nameTop = 1.875F;
      c.nameBottom = 1.875F;
      c.headTop = 1.875F;
      c.bodyYaw = 0.0F;
      c.particles = cardParticles;
      c.light = 1.0F;
      c.lightLevel = 1.0F;
      c.night = this.nightVisibility.get() == NightVisibility.ON ? 1.0F : 0.0F;
      c.keep.set(cardCamera, 0.0, 0.0, 0.0, 0.6F, 1.8F, this.keepVisible.get(), this.inWayOpacity.getFloat() / 100.0F, this.darkCap.getFloat() / 100.0F);
      c.distanceSq = 0.0;
      Textures.ensureLoaded();
      RenderState.begin();

      try {
         Vfx.setCamera(cardCamera);
         this.drawPasses(this.getEffect(type), list, 1);
         cardParticles.render(0.0, 0.0, 0.0);
      } finally {
         RenderState.end();
         c.preview = false;
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.theWorld != null && mc.thePlayer != null) {
         long now = GameClock.nanos();
         EntityOtherPlayerMP target = this.finder.find(mc, this.range.get(), this.aimTolerance.get(), this.ignoreTeammates.get(), this.ignoreNpcs.get());

         for (int i = 0; i < this.auras.size(); i++) {
            this.auras.get(i).aimed = false;
         }

         if (target != null) {
            AuraState s = this.byEntity.get(target.getEntityId());
            if (s == null || s.entity != target || s.released) {
               if (s != null) {
                  s.released = true;
               }

               s = new AuraState(target, now);
               this.byEntity.put(target.getEntityId(), s);
               this.auras.add(s);
            }

            s.aimed = true;
            s.lastAimNanos = now;
         }

         for (int var11 = 0; var11 < this.auras.size(); var11++) {
            AuraState s = this.auras.get(var11);
            EntityOtherPlayerMP e = s.entity;
            boolean gone = e.isDead || e.worldObj != mc.theWorld || mc.theWorld.getEntityByID(e.getEntityId()) != e;
            boolean hidden = e.isInvisible() || e.isSpectator();
            if ((gone || hidden) && !s.released) {
               s.released = true;
               s.aimed = false;
            }

            if (!s.released) {
               s.updateGround(mc.theWorld);
            }
         }
      }
   }

   @Override
   public void onPrepareWorld(float partialTicks) {
      long now = GameClock.nanos();
      float dt = this.lastFrameNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = now;
      float passMillis = this.passClock.takeMillis();
      if (this.preparedMillis >= 0.0F) {
         float ms = this.preparedMillis + passMillis;
         this.renderMillis = this.renderMillis == 0.0F ? ms : this.renderMillis + (ms - this.renderMillis) * 0.05F;
         this.preparedMillis = -1.0F;
      }

      this.updateFades(now, dt);
      this.particles.update(dt);
      if (!this.auras.isEmpty() || !this.particles.isEmpty()) {
         long start = System.nanoTime();
         Minecraft mc = Minecraft.getMinecraft();
         RenderManager rm = mc.getRenderManager();
         double viewX = rm.viewerPosX;
         double viewY = rm.viewerPosY;
         double viewZ = rm.viewerPosZ;
         this.camera.captureWorld();
         this.lights.frame(mc.theWorld, partialTicks);
         AuraEffect fx = this.getSelectedEffect();
         double globalTime = GameClock.seconds();
         int n = this.auras.size();
         this.ensureContexts(n);
         float inWay = this.inWayOpacity.getFloat() / 100.0F;
         float dark = this.darkCap.getFloat() / 100.0F;

         for (int i = 0; i < n; i++) {
            AuraState s = this.auras.get(i);
            EffectContext ctx = this.contexts[i];
            EntityOtherPlayerMP e = s.entity;
            double wz;
            double wy;
            double wx;
            if (s.released && s.hasLastPosition) {
               wx = s.lastX;
               wy = s.lastY;
               wz = s.lastZ;
            } else {
               wx = e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks;
               wy = e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks;
               wz = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks;
               s.lastX = wx;
               s.lastY = wy;
               s.lastZ = wz;
               s.hasLastPosition = true;
            }

            if (Double.isNaN(s.groundSmooth)) {
               s.groundSmooth = Double.isNaN(s.groundTarget) ? wy : s.groundTarget;
            } else if (!Double.isNaN(s.groundTarget)) {
               s.groundSmooth = s.groundSmooth + (s.groundTarget - s.groundSmooth) * (1.0 - Math.exp(-14.0 * dt));
            }

            double ground = Math.min(s.groundSmooth, wy);
            this.fill(ctx, s, now, dt, globalTime);
            ctx.x = wx - viewX;
            ctx.y = wy - viewY;
            ctx.z = wz - viewZ;
            ctx.groundY = ground - viewY;
            ctx.groundAlpha = 1.0F - Easing.smoothstep(0.6F, 3.0F, (float)(wy - ground));
            ctx.worldX = wx;
            ctx.worldY = wy;
            ctx.worldZ = wz;
            ctx.worldGroundY = ground;
            ctx.width = e.width;
            ctx.height = e.height;
            ctx.bodyYaw = e.prevRenderYawOffset + (e.renderYawOffset - e.prevRenderYawOffset) * partialTicks;
            heads(ctx, s, mc, e, dt);
            ctx.particles = this.particles;
            ctx.preview = false;
            this.light(ctx, s, mc.theWorld, wx, wy, wz, e.height, dt);
            ctx.keep.set(this.camera, ctx.x, ctx.y, ctx.z, ctx.width, ctx.height, this.keepVisible.get(), inWay, dark);
            ctx.distanceSq = this.camera.distanceSq(ctx.x, ctx.y + ctx.height * 0.5, ctx.z);
            FxPasses.submit(this.item(i, fx, ctx), this.passClock);
         }

         this.particlesItem.set(viewX, viewY, viewZ);
         FxPasses.submit(this.particlesItem, this.passClock);
         this.preparedMillis = (float)(System.nanoTime() - start) / 1000000.0F;
      }
   }

   private TargetAuraModule.AuraItem item(int i, AuraEffect fx, EffectContext ctx) {
      if (this.items.length <= i) {
         this.items = Arrays.copyOf(this.items, Math.max(i + 1, this.items.length * 2));
      }

      TargetAuraModule.AuraItem it;
      if ((it = this.items[i]) == null) {
         it = this.items[i] = new TargetAuraModule.AuraItem();
      }

      it.fx = fx;
      it.ctx = ctx;
      return it;
   }

   private void drawPasses(AuraEffect fx, EffectContext[] list, int n) {
      FxPasses.state(FxPass.SOLID);

      for (int i = 0; i < n; i++) {
         fx.render(list[i], FxPass.SOLID);
         Vfx.flush();
      }

      FxPasses.state(FxPass.TRANSLUCENT);
      this.sortFarToNear(list, n);

      for (int k = 0; k < n; k++) {
         fx.render(list[this.order[k]], FxPass.TRANSLUCENT);
         Vfx.flush();
      }

      FxPasses.state(FxPass.GLOW);

      for (int var6 = 0; var6 < n; var6++) {
         fx.render(list[var6], FxPass.GLOW);
         resetBetweenAuras();
      }
   }

   private void sortFarToNear(EffectContext[] list, int n) {
      if (this.order.length < n) {
         this.order = new int[Math.max(n, this.order.length * 2)];
      }

      int i = 0;

      while (i < n) {
         int k;
         for (k = i; k > 0 && list[this.order[k - 1]].distanceSq < list[i].distanceSq; k--) {
            this.order[k] = this.order[k - 1];
         }

         this.order[k] = i++;
      }
   }

   private void ensureContexts(int n) {
      if (this.contexts.length < n) {
         EffectContext[] more = Arrays.copyOf(this.contexts, Math.max(n, this.contexts.length * 2));

         for (int i = this.contexts.length; i < more.length; i++) {
            more[i] = new EffectContext();
         }

         this.contexts = more;
      }
   }

   private void light(EffectContext c, AuraState s, World w, double wx, double wy, double wz, float height, float dt) {
      float level = WorldLight.levelOf(this.lights.at(w, wx, wy + height * 0.6, wz));
      boolean first = Float.isNaN(s.lightLevel);
      float k = first ? 1.0F : (dt <= 0.0F ? 0.0F : 1.0F - (float)Math.exp(-dt / 0.25F));
      c.lightLevel = s.lightLevel = first ? level : s.lightLevel + (level - s.lightLevel) * k;
      c.light = WorldLight.light(s.lightLevel, this.matchLighting.get(), this.minBrightness.getFloat() / 100.0F);
      float night = WorldLight.night(s.lightLevel, this.nightVisibility.get());
      s.night = s.night + (night - s.night) * k;
      c.night = s.night;
   }

   private static void heads(EffectContext c, AuraState s, Minecraft mc, EntityOtherPlayerMP e, float dt) {
      if (!s.released || Float.isNaN(s.headTop)) {
         boolean sneaking = e.isSneaking();
         float head;
         float bottom = head = e.height * 1.0416667F - (sneaking ? 0.18F : 0.0F);
         float top = head;
         Entity viewer = mc.getRenderViewEntity();
         if (viewer != null && nameTagShows(mc, e, viewer)) {
            double d = e.getDistanceSqToEntity(viewer);
            float range = sneaking ? 32.0F : 64.0F;
            if (d < range * range) {
               bottom = e.height + 0.287F - (sneaking ? 0.25F : 0.0F);
               top = bottom + 0.24F;
               if (!sneaking && d < 100.0 && e.getWorldScoreboard().getObjectiveInDisplaySlot(2) != null) {
                  top += 0.276F;
               }
            }
         }

         if (Float.isNaN(s.headTop)) {
            s.headTop = head;
            s.nameBottom = bottom;
            s.nameTop = top;
         } else {
            float k = dt <= 0.0F ? 0.0F : 1.0F - (float)Math.exp(-12.0 * dt);
            s.headTop = s.headTop + (head - s.headTop) * k;
            s.nameBottom = s.nameBottom + (bottom - s.nameBottom) * k;
            s.nameTop = s.nameTop + (top - s.nameTop) * k;
         }
      }

      c.headTop = s.headTop;
      c.nameBottom = s.nameBottom;
      c.nameTop = s.nameTop;
   }

   private static boolean nameTagShows(Minecraft mc, EntityOtherPlayerMP e, Entity viewer) {
      Team team = e.getTeam();
      if (team != null && mc.thePlayer != null) {
         Team mine = mc.thePlayer.getTeam();
         EnumVisible v = team.getNameTagVisibility();
         if (v == EnumVisible.NEVER) {
            return false;
         } else if (v == EnumVisible.HIDE_FOR_OTHER_TEAMS) {
            return mine == null || team.isSameTeam(mine);
         } else {
            return v != EnumVisible.HIDE_FOR_OWN_TEAM ? true : mine == null || !team.isSameTeam(mine);
         }
      } else {
         return Minecraft.isGuiEnabled() && e != viewer && (mc.thePlayer == null || !e.isInvisibleToPlayer(mc.thePlayer)) && e.riddenByEntity == null;
      }
   }

   private void fill(EffectContext c, AuraState s, long now, float dt, double globalTime) {
      c.state = s;
      c.cam = this.camera;
      c.alpha = s.alpha;
      c.scale = 0.55F + 0.45F * s.alpha;
      c.time = s.age(now);
      c.intro = Easing.clamp01(c.time / 0.6F);
      c.globalTime = globalTime;
      c.dt = dt;
      c.seed = s.seed;
      c.spawnParticles = !s.isFadingOut() || c.preview;
      c.fadingOut = s.isFadingOut();
      c.outro = s.outroActive ? 1.0F - s.fade : 0.0F;
   }

   private static BooleanSupplier on(final BooleanSetting setting) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return setting.get();
         }
      };
   }

   private static void resetBetweenAuras() {
      Vfx.flush();
      RenderState.decal(false);
      RenderState.additive();
   }

   private void updateFades(long now, float dt) {
      long lingerNanos = (long)(this.linger.get() * 1.0E9);
      float inStep = dt / Math.max(0.01F, this.fadeIn.getFloat());
      float outStep = dt / Math.max(0.01F, this.fadeOut.getFloat());

      for (int i = this.auras.size() - 1; i >= 0; i--) {
         AuraState s = this.auras.get(i);
         boolean visible;
         s.visible = visible = !s.released && (s.aimed || now - s.lastAimNanos <= lingerNanos);
         if (visible) {
            s.fade = Math.min(1.0F, s.fade + inStep);
            if (s.fade >= 1.0F) {
               s.outroActive = false;
            }
         } else {
            s.outroActive = true;
            s.fade = Math.max(0.0F, s.fade - outStep);
            if (s.fade <= 0.0F) {
               this.auras.remove(i);
               if (this.byEntity.get(s.entity.getEntityId()) == s) {
                  this.byEntity.remove(s.entity.getEntityId());
               }
               continue;
            }
         }

         s.alpha = Easing.outCubic(s.fade);
      }
   }

   @Override
   public void onWorldChange() {
      this.auras.clear();
      this.byEntity.clear();
      this.particles.clear();
      this.lastFrameNanos = 0L;
      this.offLastAim = 0L;
      this.offTick = Long.MIN_VALUE;
   }

   @Override
   protected void onDisable() {
      this.onWorldChange();
   }

   public boolean hasTarget() {
      if (this.isEnabled()) {
         for (int i = 0; i < this.auras.size(); i++) {
            AuraState s = this.auras.get(i);
            if (s.visible && !s.released) {
               return true;
            }
         }

         return false;
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.theWorld != null && mc.thePlayer != null) {
            long now = GameClock.nanos();
            long tick = mc.theWorld.getTotalWorldTime();
            if (tick != this.offTick) {
               this.offTick = tick;
               if (this.finderFind(mc) != null) {
                  this.offLastAim = now;
               }
            }

            return this.offLastAim != 0L && now - this.offLastAim <= (long)(this.linger.get() * 1.0E9) + 60000000L;
         } else {
            return false;
         }
      }
   }

   private EntityOtherPlayerMP finderFind(Minecraft mc) {
      return this.offFinder.find(mc, this.range.get(), this.aimTolerance.get(), this.ignoreTeammates.get(), this.ignoreNpcs.get());
   }

   public float getRenderMillis() {
      return this.renderMillis;
   }

   public int getActiveAuraCount() {
      return this.auras.size();
   }

   public float cardDrawMillisForTest() {
      return this.previews.drawMillisForTest();
   }

   public String lightForTest() {
      if (this.auras.isEmpty()) {
         return "no aura";
      } else {
         EffectContext c = this.contexts[0];
         return String.format(Locale.ROOT, "level %.2f, light %.2f, night %.2f", c.lightLevel, c.light, c.night);
      }
   }

   public String outroForTest() {
      if (this.auras.isEmpty()) {
         return "no aura";
      } else {
         AuraState s = this.auras.get(0);
         return String.format(Locale.ROOT, "fade %.2f, fading out %s, outro %.2f", s.fade, s.isFadingOut(), s.outroActive ? 1.0F - s.fade : 0.0F);
      }
   }

   public EffectContext contextForTest() {
      return this.auras.isEmpty() ? null : this.contexts[0];
   }

   @Override
   public int getAccentColor(double time) {
      return this.getSelectedEffect().color.getDisplayColor();
   }

   @Override
   public int getOptionColor(Setting setting, Enum<?> option, double time) {
      return setting == this.effect && option instanceof EffectType ? this.getEffect((EffectType)option).color.getDisplayColor() : 0;
   }

   @Override
   public boolean hasPreview() {
      return true;
   }

   @Override
   public String getPreviewLabel() {
      return this.effect.get().displayName();
   }

   @Override
   public String getPreviewInfo() {
      return this.renderMillis <= 0.0F ? null : String.format(Locale.ROOT, "%.2f ms / frame", this.renderMillis);
   }

   @Override
   public void restartPreview() {
      this.previewState = new AuraState(null, ClientClock.nanos());
      this.previewState.aimed = true;
      this.previewParticles.clear();
   }

   @Override
   public void renderPreview(float dt) {
      if (this.previewState == null) {
         this.restartPreview();
      }

      long now = ClientClock.nanos();
      this.previewState.fade = Math.min(1.0F, this.previewState.fade + dt / Math.max(0.01F, this.fadeIn.getFloat()));
      this.previewState.alpha = Easing.outCubic(this.previewState.fade);
      this.previewParticles.update(dt);
      Textures.ensureLoaded();
      RenderState.begin();
      EffectContext ctx = this.previewContext[0];

      try {
         this.camera.captureModelView();
         Vfx.setCamera(this.camera);
         ctx.preview = true;
         this.fill(ctx, this.previewState, now, dt, ClientClock.seconds());
         ctx.z = 0.0;
         ctx.y = 0.0;
         ctx.x = 0.0;
         ctx.groundY = 0.0;
         ctx.groundAlpha = 1.0F;
         ctx.worldGroundY = 0.0;
         ctx.worldZ = 0.0;
         ctx.worldY = 0.0;
         ctx.worldX = 0.0;
         ctx.width = 0.6F;
         ctx.height = 1.8F;
         ctx.nameTop = 1.875F;
         ctx.nameBottom = 1.875F;
         ctx.headTop = 1.875F;
         ctx.bodyYaw = 0.0F;
         ctx.particles = this.previewParticles;
         ctx.light = 1.0F;
         ctx.lightLevel = 1.0F;
         ctx.night = this.nightVisibility.get() == NightVisibility.ON ? 1.0F : 0.0F;
         ctx.keep.set(this.camera, 0.0, 0.0, 0.0, 0.6F, 1.8F, this.keepVisible.get(), this.inWayOpacity.getFloat() / 100.0F, this.darkCap.getFloat() / 100.0F);
         ctx.distanceSq = 0.0;
         this.drawPasses(this.getSelectedEffect(), this.previewContext, 1);
         this.previewParticles.render(0.0, 0.0, 0.0);
      } finally {
         RenderState.end();
         ctx.preview = false;
      }
   }

   private static final class AuraItem implements PassItem {
      AuraEffect fx;
      EffectContext ctx;

      private AuraItem() {
      }

      @Override
      public int passes() {
         return 7;
      }

      @Override
      public void drawPass(FxPass pass) {
         this.fx.render(this.ctx, pass);
      }

      @Override
      public double passDistanceSq() {
         return this.ctx.distanceSq;
      }
   }

   private final class ParticlesItem implements PassItem {
      double viewX;
      double viewY;
      double viewZ;

      private ParticlesItem() {
      }

      void set(double x, double y, double z) {
         this.viewX = x;
         this.viewY = y;
         this.viewZ = z;
      }

      @Override
      public int passes() {
         return FxPass.GLOW.bit();
      }

      @Override
      public void drawPass(FxPass pass) {
         TargetAuraModule.this.particles.render(this.viewX, this.viewY, this.viewZ);
      }

      @Override
      public double passDistanceSq() {
         return 0.0;
      }
   }
}
