package meowtils.furiahvisuals.module.kill;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.core.TestDummy;
import meowtils.furiahvisuals.fx.EffectEngine;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.FxParticles;
import meowtils.furiahvisuals.fx.Rng;
import meowtils.furiahvisuals.fx.Terrain;
import meowtils.furiahvisuals.fx.look.WorldLight;
import meowtils.furiahvisuals.fx.pass.FxPasses;
import meowtils.furiahvisuals.fx.style.Signature;
import meowtils.furiahvisuals.gui.widget.SignatureColorsWidget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.hit.HitContext;
import meowtils.furiahvisuals.module.hit.HitEffectModule;
import meowtils.furiahvisuals.module.misc.skins.SkinHooks;
import meowtils.furiahvisuals.module.particles.LibraryImageSetting;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.SkinSampler;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public final class KillEffectsModule extends Module {
   static final String[] DEFAULT_PATTERNS = new String[]{
      "{victim} was slain by {me}",
      "{victim} was shot by {me}",
      "{victim} was killed by {me}",
      "{victim} was knocked into the void by {me}",
      "{victim} was thrown into the void by {me}",
      "{victim} was blown up by {me}",
      "{victim} was doomed to fall by {me}",
      "{victim} * by {me}. FINAL KILL!",
      "{victim} has been killed by {me}",
      "{me} killed {victim}"
   };
   private static final double LOD_DISTANCE = 32.0;
   private static final long STREAK_NANOS = 10000000000L;
   private static final long REPLACE_HIT_NANOS = 150000000L;
   private static final double HIDE_RADIUS = 3.0;
   private static final float MARKER_SECONDS = 1.6F;
   private static final float PULSE_SECONDS = 0.38F;
   public final ModeSetting<KillStyle> style;
   public final BooleanSetting randomStyle;
   final StyleSetSetting randomSet;
   public final ModeSetting<KillEffectsModule.ShowFor> showFor;
   public final SliderSetting credit;
   public final BooleanSetting detectDeath;
   public final BooleanSetting detectChat;
   final KillPatternSetting chatPatterns;
   public final BooleanSetting detectVanish;
   public final SliderSetting vanishHealth;
   public final SliderSetting size;
   public final SliderSetting speed;
   public final SliderSetting intensity;
   public final SliderSetting opacity;
   public final BooleanSetting glow;
   public final SliderSetting glowIntensity;
   public final BooleanSetting critBoost;
   public final ModeSetting<KillEffectsModule.ParticleStyle> particleStyle;
   public final BooleanSetting showArmor;
   public final BooleanSetting showItem;
   public final BooleanSetting showCape;
   public final ModeSetting<KillEffectsModule.BodyEnd> bodyEnd;
   public final SliderSetting linger;
   public final BooleanSetting replaceHit;
   public final BooleanSetting marker;
   public final ModeSetting<KillEffectsModule.MarkerIcon> markerIcon;
   public final LibraryImageSetting markerImage;
   public final ColorSetting markerColor;
   public final BooleanSetting islandNotice;
   public final BooleanSetting slowmo;
   public final BooleanSetting pulse;
   public final SliderSetting maxEffects;
   public final BooleanSetting testCrit;
   public final WorldLight lighting;
   private final KillStyleBase[] styles;
   private final KillDetector detector;
   private final EffectEngine<KillFx> engine = new EffectEngine<>("Kill Effects");
   private final FxParticles particles = new FxParticles(6144);
   private final Camera camera = new Camera();
   private final Camera previewCamera = new Camera();
   private final FxFrame frame = new FxFrame();
   private final Rng rng = new Rng();
   private int seedCounter = 1;
   private long lastFrameNanos;
   private float renderMillis;
   private final List<KillEffectsModule.Hide> hidden = new ArrayList<>();
   private final List<KillEffectsModule.Marker> markers = new ArrayList<>();
   private float pulseAge = -1.0F;
   private int pulseColor;
   private int streak;
   private long lastKillNanos;
   private final List<ResourceLocation> skinQueue = new ArrayList<>();
   private long testAt;
   private long dummyKillAt;
   private boolean dummyKillCrit;
   private int kills;
   private KillContext lastKill;
   private float lastSetupMillis;
   private int hiddenFrames;
   private final EffectEngine<KillFx> previewEngine = new EffectEngine<>("Kill Effects preview");
   private final FxParticles previewParticles = new FxParticles(2048, true);
   private final FxFrame previewFrame = new FxFrame();
   private final BodyProxy.Snapshot previewBody = new BodyProxy.Snapshot();
   private final BodyProxy.Part[] previewParts = BodyProxy.newParts();
   private final BodyProxy.Look previewLook = new BodyProxy.Look();
   private float previewClock;
   private float previewNext = 0.8F;
   private float previewStand;
   private int previewIndex;
   private final Module.StartingLooks looks = styleLooks(new Supplier<List<SliderSetting>>() {
      public List<SliderSetting> get() {
         return KillEffectsModule.this.getSelectedStyle().lookTargets();
      }
   });
   private static final List<KillEffectsModule.KillListener> KILL_LISTENERS = new ArrayList<>();
   private float frameDt;
   private long frameNow;
   private final FxPasses.Clock passClock = new FxPasses.Clock();
   private double costSum;
   private int costFrames;
   private float costPeak;
   private long lastPulseNanos;
   private int warmFrames;
   private int warmIndex;
   private final FxParticles warmParticles = new FxParticles(512);
   private int pulseFrames;
   private static final double[][] PREVIEW_EYES = new double[][]{{0.0, 1.62, 3.0}, {-35.0, 1.6, 2.8}, {8.0, 3.4, 2.2}, {35.0, 1.6, 2.8}};
   private double previewFocusX;
   private double previewFocusZ;
   private final double[] focus = new double[2];

   public KillEffectsModule() {
      super("kill_effects", "Kill Effects", "An effect when you kill a player, played on a copy of their body.", true);
      this.style = this.add(
         new ModeSetting<>("style", "Style", "The look of the effect.", KillStyle.class, KillStyle.RAGDOLL).style(ModeSetting.Style.CARDS).group("Style")
      );
      KillStyle[] types = KillStyle.values();
      this.styles = new KillStyleBase[types.length];

      for (KillStyle type : types) {
         KillStyleBase s;
         this.styles[type.ordinal()] = s = type.create(this);

         for (Setting setting : s.getSettings()) {
            this.add(setting);
         }
      }

      BooleanSupplier random = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return KillEffectsModule.this.randomStyle.get();
         }
      };
      BooleanSupplier chatOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return KillEffectsModule.this.detectChat.get();
         }
      };
      BooleanSupplier vanishOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return KillEffectsModule.this.detectVanish.get();
         }
      };
      BooleanSupplier glowOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return KillEffectsModule.this.glow.get();
         }
      };
      BooleanSupplier markerOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return KillEffectsModule.this.marker.get();
         }
      };
      BooleanSupplier customMarker = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return KillEffectsModule.this.marker.get() && KillEffectsModule.this.markerIcon.get() == KillEffectsModule.MarkerIcon.CUSTOM;
         }
      };
      this.randomStyle = this.add(
         new BooleanSetting("randomStyle", "Random Style Each Kill", "Every kill picks one of the styles below at random.", false).group("Random Style")
      );
      this.randomSet = this.add(
         new StyleSetSetting("randomSet", "Pick From", "The styles a kill may pick. Click one to put it in or take it out.")
            .<StyleSetSetting>group("Random Style")
            .visibleWhen(random)
      );
      this.showFor = this.add(
         new ModeSetting<>("showFor", "Show For", "Whose deaths get an effect.", KillEffectsModule.ShowFor.class, KillEffectsModule.ShowFor.MY_KILLS)
            .group("Detection")
      );
      this.credit = this.add(
         new SliderSetting("credit", "Credit Window", "A death counts as your kill when you hit that player within this time before it.", 2.0, 20.0, 8.0, 1.0)
            .unit(" s")
            .group("Detection")
      );
      this.detectDeath = this.add(
         new BooleanSetting("detectDeath", "Death Status", "The server marks the player dead (vanilla servers).", true).group("Detection")
      );
      this.detectChat = this.add(
         new BooleanSetting("detectChat", "Chat Messages", "A kill message in chat names the victim and you (servers that respawn players at once).", true)
            .group("Detection")
      );
      this.chatPatterns = this.add(
         new KillPatternSetting(
               "chatPatterns",
               "Kill Messages",
               "Chat messages that mean you killed someone. {victim} is the player who died, {me} is you, * is any text.",
               DEFAULT_PATTERNS
            )
            .<KillPatternSetting>group("Detection")
            .visibleWhen(chatOn)
      );
      this.detectVanish = this.add(
         new BooleanSetting(
               "detectVanish", "Vanished At Low Health", "The player you were fighting disappears within a second of your hit while low on health.", true
            )
            .group("Detection")
      );
      this.vanishHealth = this.add(
         new SliderSetting(
               "vanishHealth",
               "Low Health",
               "At or below this many hearts, vanishing counts as a kill. At full health it is only a teleport.",
               0.5,
               10.0,
               4.0,
               0.5
            )
            .unit(" hearts")
            .<SliderSetting>group("Detection")
            .visibleWhen(vanishOn)
      );
      this.size = this.add(new SliderSetting("size", "Size", "Scales the whole effect.", 0.5, 2.0, 1.0, 0.05).unit("x").group("Look"));
      this.speed = this.add(
         new SliderSetting("speed", "Speed", "How fast the animation plays. It always plays to the end.", 0.5, 2.0, 1.0, 0.05).unit("x").group("Look")
      );
      this.intensity = this.add(
         new SliderSetting("intensity", "Intensity", "How many particles, pieces and specks.", 0.25, 2.0, 1.0, 0.05).unit("x").group("Look")
      );
      this.opacity = this.add(new SliderSetting("opacity", "Opacity", "How strong the effect is.", 0.1, 1.0, 1.0, 0.05).group("Look"));
      this.glow = this.add(new BooleanSetting("glow", "Glow", "Soft light around the bright parts.", true).group("Look"));
      this.glowIntensity = this.add(
         new SliderSetting("glowIntensity", "Glow Strength", "How bright the glow is.", 0.1, 2.0, 1.0, 0.05)
            .unit("x")
            .<SliderSetting>group("Look")
            .visibleWhen(glowOn)
      );
      this.critBoost = this.add(
         new BooleanSetting("critBoost", "Critical Boost", "A critical killing blow (while falling) plays a bigger, stronger version.", true).group("Look")
      );
      BooleanSupplier goreStyles = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return KillEffectsModule.this.randomStyle.get() || !KillEffectsModule.this.style.get().isNew();
         }
      };
      BooleanSupplier leavesBody = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return KillEffectsModule.this.randomStyle.get()
               || !KillEffectsModule.this.style.get().isNew()
               || KillEffectsModule.this.getSelectedStyle().leavesBody();
         }
      };
      this.particleStyle = this.add(
         new ModeSetting<>(
               "particleStyle",
               "Particle Style",
               "For the gorier styles: sparks and cubes in the effect color, or classic red.",
               KillEffectsModule.ParticleStyle.class,
               KillEffectsModule.ParticleStyle.STYLIZED
            )
            .<ModeSetting>group("Look")
            .visibleWhen(goreStyles)
      );
      this.showArmor = this.add(new BooleanSetting("showArmor", "Armor", "The copy wears the victim's armor.", true).group("Body"));
      this.showItem = this.add(new BooleanSetting("showItem", "Held Item", "The copy holds the victim's item.", true).group("Body"));
      this.showCape = this.add(new BooleanSetting("showCape", "Cape", "The copy wears the victim's cape.", true).group("Body"));
      this.bodyEnd = this.add(
         new ModeSetting<>(
               "bodyEnd",
               "Body End",
               "For styles that leave a body: how it goes after Linger.",
               KillEffectsModule.BodyEnd.class,
               KillEffectsModule.BodyEnd.FADE
            )
            .<ModeSetting>group("Body")
            .visibleWhen(leavesBody)
      );
      this.linger = this.add(
         new SliderSetting("linger", "Linger", "How long a body stays before it goes.", 0.0, 10.0, 3.0, 0.5)
            .unit(" s")
            .<SliderSetting>group("Body")
            .visibleWhen(leavesBody)
      );
      this.replaceHit = this.add(
         new BooleanSetting(
               "replaceHit",
               "Replace Hit Effect On The Killing Blow",
               "When the kill follows your hit at once, that hit's effect fades out quickly so the kill plays alone.",
               true
            )
            .group("Body")
      );
      this.marker = this.add(
         new BooleanSetting("marker", "Kill Marker", "An icon pops up over the death spot, floats up and fades.", true)
            .<BooleanSetting>group("Add-ons")
            .keywords(new String[]{"skull", "icon", "hitmarker"})
      );
      this.markerIcon = this.add(
         new ModeSetting<>("markerIcon", "Marker Icon", "The marker's icon.", KillEffectsModule.MarkerIcon.class, KillEffectsModule.MarkerIcon.SKULL)
            .<ModeSetting>group("Add-ons")
            .visibleWhen(markerOn)
      );
      this.markerImage = this.add(
         new LibraryImageSetting("markerImage", "Marker Image", "The image (from Particles › Custom Images).")
            .<LibraryImageSetting>group("Add-ons")
            .visibleWhen(customMarker)
      );
      this.markerColor = this.add(
         new ColorSetting("markerColor", "Marker Color", "Color of the marker.", -45206).<ColorSetting>group("Add-ons").visibleWhen(markerOn)
      );
      this.islandNotice = this.add(
         new BooleanSetting(
               "islandNotice",
               "Island Notification",
               "The WaterMark island shows the name of the player you killed, with a streak (×3) when kills chain within 10 s.",
               true
            )
            .<BooleanSetting>group("Add-ons")
            .keywords(new String[]{"watermark", "killstreak", "streak"})
      );
      this.slowmo = this.add(
         new BooleanSetting("slowmo", "Slow-Mo Start", "The effect's first 0.3 s plays slowed down, for impact. Only the effect slows, never the game.", true)
            .<BooleanSetting>group("Add-ons")
            .keywords(new String[]{"slow motion", "slowmotion"})
      );
      this.pulse = this.add(
         new BooleanSetting(
               "pulse", "Screen Pulse", "A short, subtle flash at the screen's edges in the effect color. Off while Settings › Reduce Flashing is on.", false
            )
            .<BooleanSetting>group("Add-ons")
            .keywords(new String[]{"vignette", "flash"})
      );
      this.maxEffects = this.add(
         new SliderSetting(
               "maxEffects",
               "Max Effects At Once",
               "Kill effects playing at the same time; more kills wait for none (they get no effect). Past 32 blocks a simpler version plays.",
               1.0,
               8.0,
               4.0,
               1.0
            )
            .<SliderSetting>group("Performance")
            .keywords(new String[]{"limit", "fps"})
      );
      this.testCrit = this.add(new BooleanSetting("testCrit", "Test As Critical Hit", "Test kill plays the critical-hit version.", false).group("Testing"));
      this.lighting = new WorldLight(
         "Solid materials, paper, drawn strokes and the body copies' looks", "Overgrowth, Ink Wash, Consumed, and Erased with Pencil"
      );
      this.add(this.lighting.match);
      this.add(this.lighting.minimum);
      this.add(this.lighting.night);
      this.add(SignatureColorsWidget.setting(new SignatureColorsWidget.Source() {
         @Override
         public Signature selectedSignature() {
            return KillEffectsModule.this.getSelectedStyle().signature();
         }

         @Override
         public String selectedStyleName() {
            return KillEffectsModule.this.style.get().displayName();
         }
      }));
      this.style.addListener(new Runnable() {
         @Override
         public void run() {
            KillEffectsModule.this.restartPreview();
         }
      });
      this.detector = new KillDetector(this);
   }

   public KillStyleBase getStyle(KillStyle type) {
      return this.styles[type.ordinal()];
   }

   @Override
   public Module.StartingLooks startingLooks() {
      return this.looks;
   }

   public KillStyleBase getSelectedStyle() {
      return this.styles[this.style.get().ordinal()];
   }

   @Override
   public void onAttack(EntityPlayer attacker, Entity target) {
      Minecraft mc = Minecraft.getMinecraft();
      if (attacker == mc.thePlayer && mc.thePlayer != null && target != null) {
         this.detector.onAttack(mc, mc.thePlayer, target);
      }
   }

   public void onDeath(EntityPlayer victim) {
      if (this.isEnabled()) {
         this.detector.onDeath(Minecraft.getMinecraft(), victim);
      }
   }

   @Override
   public void onChat(String text) {
      this.detector.onChat(Minecraft.getMinecraft(), text);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getMinecraft();
      long nowMs = GameClock.millis();
      if (this.testAt != 0L && nowMs >= this.testAt && mc.currentScreen == null) {
         this.testAt = 0L;
         this.testKill(mc, this.testCrit.get());
      }

      if (this.dummyKillAt != 0L && nowMs >= this.dummyKillAt) {
         this.dummyKillAt = 0L;
         this.killDummy(mc, this.dummyKillCrit);
      }

      this.detector.tick(mc);
      long now = GameClock.nanos();

      for (int i = this.hidden.size() - 1; i >= 0; i--) {
         if (now > this.hidden.get(i).until) {
            this.hidden.remove(i);
         }
      }
   }

   public static void addKillListener(KillEffectsModule.KillListener listener) {
      KILL_LISTENERS.add(listener);
   }

   void onKill(KillContext ctx, EntityPlayer victim) {
      if (ctx.mine) {
         for (int i = 0; i < KILL_LISTENERS.size(); i++) {
            try {
               KILL_LISTENERS.get(i).onMyKill(ctx);
            } catch (RuntimeException | LinkageError var10) {
               FuriahsVisuals.LOG.error("A kill listener failed", var10);
            }
         }
      }

      long start = System.nanoTime();
      Minecraft mc = Minecraft.getMinecraft();
      KillFx fx = this.startEffect(ctx, false);
      if (fx != null) {
         this.kills++;
         this.lastKill = ctx;
         if (victim != null && victim.getHealth() <= 0.0F) {
            this.hide(victim, ctx, fx);
         }

         Module hit;
         if (this.replaceHit.get()
            && ctx.hitTime != 0L
            && ctx.time - ctx.hitTime <= 150000000L
            && (hit = FuriahsVisuals.instance.getModules().get("hit_effect")) instanceof HitEffectModule
            && hit.isEnabled()) {
            ((HitEffectModule)hit).cutShort(ctx.body.entityId);
         }

         int color = fx.style.color.getColor(0.5F) | 0xFF000000;
         if (this.marker.get()) {
            this.addMarker(ctx);
         }

         if (ctx.mine) {
            this.streak = ctx.time - this.lastKillNanos <= 10000000000L && this.lastKillNanos != 0L ? this.streak + 1 : 1;
            this.lastKillNanos = ctx.time;
            WaterMarkModule wm;
            if (this.islandNotice.get() && (wm = FuriahsVisuals.instance.getWaterMark()) != null) {
               wm.postKill(ctx.victimName + (this.streak > 1 ? "  ×" + this.streak : ""), this.streak);
            }

            if (this.pulse.get() && !FuriahsVisuals.instance.isReduceFlashing() && !fx.lod) {
               this.pulseAge = 0.0F;
               this.pulseColor = color;
            }
         }

         this.lastSetupMillis = (float)(System.nanoTime() - start) / 1000000.0F;
         if (mc.gameSettings.showDebugInfo && this.lastSetupMillis > 2.0F) {
            FuriahsVisuals.LOG.debug("Kill effect setup took {} ms", new Object[]{this.lastSetupMillis});
         }
      }
   }

   private KillFx startEffect(KillContext ctx, boolean preview) {
      EffectEngine<KillFx> target = preview ? this.previewEngine : this.engine;
      target.setLimits(preview ? 1 : this.maxEffects.getInt(), 2);
      if (!target.hasRoom(ctx.body.entityId)) {
         this.detector.lastSkip = ctx.victimName + ": max effects at once reached";
         return null;
      } else {
         KillStyle type = this.style.get();
         if (this.randomStyle.get() && !preview) {
            type = this.pickRandom(type);
         }

         KillStyleBase s = this.styles[type.ordinal()];
         int seed = Rng.mix(this.seedCounter++ * -1640531535 ^ (int)ctx.time);
         KillFx fx = new KillFx(s, ctx, seed, preview ? this.previewParticles : this.particles, this, preview);
         if (!preview) {
            Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
            fx.lod = viewer != null && viewer.getDistanceSq(ctx.feetX, ctx.feetY, ctx.feetZ) > 1024.0;
         }

         return target.add(fx) ? fx : null;
      }
   }

   private KillStyle pickRandom(KillStyle fallback) {
      KillStyle[] all = KillStyle.values();
      int n = 0;

      for (KillStyle s : all) {
         n += this.randomSet.includes(s) ? 1 : 0;
      }

      if (n == 0) {
         return fallback;
      } else {
         this.rng.seed(this.seedCounter * 31, (int)System.nanoTime());
         int pick = Math.min(n - 1, (int)(this.rng.next() * n));

         for (KillStyle s : all) {
            if (this.randomSet.includes(s) && pick-- == 0) {
               return s;
            }
         }

         return fallback;
      }
   }

   private void hide(EntityPlayer victim, KillContext ctx, KillFx fx) {
      long until = GameClock.nanos() + (long)((fx.realSeconds() + 0.5F) * 1.0E9);

      for (int i = this.hidden.size() - 1; i >= 0; i--) {
         if (this.hidden.get(i).entityId == victim.getEntityId()) {
            this.hidden.remove(i);
         }
      }

      this.hidden.add(new KillEffectsModule.Hide(victim.getEntityId(), victim.posX, victim.posY, victim.posZ, until));
   }

   private KillEffectsModule.Hide hideOf(int entityId) {
      for (int i = 0; i < this.hidden.size(); i++) {
         if (this.hidden.get(i).entityId == entityId) {
            return this.hidden.get(i);
         }
      }

      return null;
   }

   @Override
   public boolean hidesPlayer(EntityPlayer p) {
      if (!this.hidden.isEmpty() && !Minecraft.getMinecraft().gameSettings.hideGUI) {
         KillEffectsModule.Hide h = this.hideOf(p.getEntityId());
         if (h == null) {
            return false;
         } else {
            double dx = p.posX - h.x;
            double dy = p.posY - h.y;
            double dz = p.posZ - h.z;
            boolean alive = p.getHealth() > 0.0F && p.deathTime == 0;
            if (!alive && !(dx * dx + dy * dy + dz * dz > 9.0) && GameClock.nanos() <= h.until) {
               this.hiddenFrames++;
               return true;
            } else {
               this.hidden.remove(h);
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean skipsPoof(EntityLivingBase e) {
      return this.isEnabled() && !this.hidden.isEmpty() && e instanceof EntityPlayer && !Minecraft.getMinecraft().gameSettings.hideGUI
         ? this.hideOf(e.getEntityId()) != null && e.getHealth() <= 0.0F
         : false;
   }

   @Override
   public void onPrepareWorld(float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      long now = GameClock.nanos();
      float dt = this.lastFrameNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = now;
      this.frameDt = dt;
      this.frameNow = now;
      this.prefetchOne();
      this.warmUp(mc);
      this.engine.update(dt, partialTicks, mc.theWorld);
      this.particles.update(dt);

      for (int i = this.markers.size() - 1; i >= 0; i--) {
         KillEffectsModule.Marker m = this.markers.get(i);
         m.age += dt;
         if (m.age >= 1.6F) {
            this.markers.remove(i);
         }
      }

      boolean passes = false;

      for (int ix = 0; ix < this.engine.size() && !passes; ix++) {
         passes = this.engine.list().get(ix).passes() != 0;
      }

      if (passes) {
         long start = System.nanoTime();
         this.camera.captureWorld();
         this.fillFrame(mc, partialTicks, dt, now);
         this.lighting.frame(mc.theWorld, partialTicks);

         for (int ix = 0; ix < this.engine.size(); ix++) {
            KillFx fx = this.engine.list().get(ix);
            if (fx.passes() != 0) {
               fx.passFrame = this.frame;
               float level = this.lighting.level(mc.theWorld, fx.ctx.feetX, fx.ctx.feetY + 1.0, fx.ctx.feetZ);
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
      long now = this.frameNow;
      float dt = this.frameDt;
      float passMillis = this.passClock.takeMillis();
      if (!this.engine.isEmpty() || !this.particles.isEmpty() || !this.markers.isEmpty()) {
         long start = System.nanoTime();
         Textures.ensureKillLoaded();
         RenderState.begin();

         try {
            this.camera.captureModelView();
            Vfx.setCamera(this.camera);
            this.fillFrame(mc, partialTicks, dt, now);
            this.engine.render(this.frame);
            this.particles.render(this.frame.viewX, this.frame.viewY, this.frame.viewZ);
            this.drawMarkers(this.frame);
         } finally {
            RenderState.end();
         }

         float ms = (float)(System.nanoTime() - start) / 1000000.0F + passMillis;
         float f = this.renderMillis = this.renderMillis == 0.0F ? ms : this.renderMillis + (ms - this.renderMillis) * 0.05F;
         if (!this.engine.isEmpty()) {
            this.costSum += ms;
            this.costFrames++;
            this.costPeak = Math.max(this.costPeak, this.renderMillis);
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

   private void fillFrame(Minecraft mc, float pt, float dt, long now) {
      FxFrame f = this.frame;
      f.cam = this.camera;
      f.viewX = mc.getRenderManager().viewerPosX;
      f.viewY = mc.getRenderManager().viewerPosY;
      f.viewZ = mc.getRenderManager().viewerPosZ;
      Entity v = (Entity)(mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.thePlayer);
      if (v != null) {
         f.eyeX = v.lastTickPosX + (v.posX - v.lastTickPosX) * pt;
         f.eyeY = v.lastTickPosY + (v.posY - v.lastTickPosY) * pt + v.getEyeHeight();
         f.eyeZ = v.lastTickPosZ + (v.posZ - v.lastTickPosZ) * pt;
      }

      f.partialTicks = pt;
      f.dt = dt;
      f.now = now;
      f.frameId = FxFrame.currentFrame();
      f.preview = false;
      f.world = mc.theWorld;
      f.firstPerson = mc.gameSettings.thirdPersonView == 0 && v == mc.thePlayer;
   }

   private void addMarker(KillContext ctx) {
      if (this.markers.size() >= 8) {
         this.markers.remove(0);
      }

      KillEffectsModule.Marker m = new KillEffectsModule.Marker();
      m.x = ctx.feetX;
      m.y = ctx.feetY + 2.35;
      m.z = ctx.feetZ;
      m.color = this.markerColor.getColor(0.5F);
      m.icon = this.markerIcon.get();
      this.markers.add(m);
   }

   private int markerTexture(KillEffectsModule.MarkerIcon icon) {
      switch (icon) {
         case X:
            return Textures.KILL_X;
         case CROWN:
            return Textures.KILL_CROWN;
         case CUSTOM:
            int tex = this.markerImage.texture();
            return tex > 0 ? tex : Textures.SKULL;
         default:
            return Textures.SKULL;
      }
   }

   private void drawMarkers(FxFrame f) {
      if (!this.markers.isEmpty()) {
         RenderState.additive();

         for (int i = 0; i < this.markers.size(); i++) {
            KillEffectsModule.Marker m = this.markers.get(i);
            float t = m.age / 1.6F;
            float a = Easing.smoothstep(0.0F, 0.06F, t) * (1.0F - Easing.smoothstep(0.62F, 1.0F, t)) * this.opacity.getFloat();
            double y = m.y + 0.55 * Easing.outCubic(t);
            Vfx.sprite(Textures.GLOW, f.rx(m.x), f.ry(y), f.rz(m.z), 1.25 * this.size.getFloat(), 0.0F, ColorUtil.mulAlpha(m.color, a * 0.45F));
         }

         RenderState.translucent(false);

         for (int var16 = 0; var16 < this.markers.size(); var16++) {
            KillEffectsModule.Marker m = this.markers.get(var16);
            float t = m.age / 1.6F;
            float a = Easing.smoothstep(0.0F, 0.06F, t) * (1.0F - Easing.smoothstep(0.62F, 1.0F, t)) * this.opacity.getFloat();
            double y = m.y + 0.55 * Easing.outCubic(t);
            float pop = Easing.outBack(Easing.clamp01(m.age / 0.28F));
            int tex = this.markerTexture(m.icon);
            boolean custom = m.icon == KillEffectsModule.MarkerIcon.CUSTOM && tex != Textures.SKULL;
            int c = custom ? ColorUtil.mulAlpha(-1, a) : ColorUtil.mulAlpha(ColorUtil.lerp(m.color, -1, 0.25F), a);
            Vfx.sprite(tex, f.rx(m.x), f.ry(y), f.rz(m.z), 0.62 * this.size.getFloat() * pop, 0.0F, c);
         }

         Vfx.flush();
         RenderState.additive();
      }
   }

   @Override
   public void onRenderHud(float partialTicks) {
      if (!(this.pulseAge < 0.0F)) {
         long now = ClientClock.nanos();
         this.pulseAge = this.pulseAge + Math.min(0.1F, this.lastPulseNanos == 0L ? 0.0F : (float)(now - this.lastPulseNanos) / 1.0E9F);
         this.lastPulseNanos = now;
         if (!(this.pulseAge >= 0.38F) && !FuriahsVisuals.instance.isReduceFlashing() && this.pulse.get()) {
            float t = this.pulseAge / 0.38F;
            float a = (t < 0.15F ? t / 0.15F : (float)Math.pow(1.0F - (t - 0.15F) / 0.85F, 2.2)) * 0.42F * this.opacity.getFloat();
            Minecraft mc = Minecraft.getMinecraft();
            ScaledResolution sr = new ScaledResolution(mc);
            double w = sr.getScaledWidth_double();
            double h = sr.getScaledHeight_double();
            Textures.ensureKillLoaded();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 1, 1, 771);
            GlStateManager.disableAlpha();
            GlStateManager.depthMask(false);
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(Textures.VIGNETTE);
            Vfx.resetTextureBinding();
            int c = ColorUtil.mulAlpha(this.pulseColor, a);
            float r = (c >> 16 & 0xFF) / 255.0F;
            float g = (c >> 8 & 0xFF) / 255.0F;
            float b = (c & 0xFF) / 255.0F;
            float al = (c >>> 24) / 255.0F;
            Tessellator tes = Tessellator.getInstance();
            WorldRenderer wr = tes.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            wr.pos(0.0, h, 0.0).tex(0.0, 1.0).color(r, g, b, al).endVertex();
            wr.pos(w, h, 0.0).tex(1.0, 1.0).color(r, g, b, al).endVertex();
            wr.pos(w, 0.0, 0.0).tex(1.0, 0.0).color(r, g, b, al).endVertex();
            wr.pos(0.0, 0.0, 0.0).tex(0.0, 0.0).color(r, g, b, al).endVertex();
            tes.draw();
            this.pulseFrames++;
            GlStateManager.depthMask(true);
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.enableAlpha();
            GlStateManager.resetColor();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         } else {
            this.pulseAge = -1.0F;
            this.lastPulseNanos = 0L;
         }
      }
   }

   private void warmUp(Minecraft mc) {
      if (this.warmIndex <= this.styles.length && mc.thePlayer != null && this.engine.isEmpty() && ++this.warmFrames >= 40) {
         if (this.warmIndex == 0) {
            if (Textures.warmKill()) {
               this.warmIndex = 1;
            }
         } else {
            KillStyleBase s = this.styles[this.warmIndex - 1];
            this.warmIndex++;

            try {
               EntityPlayerSP me = mc.thePlayer;
               KillContext ctx = new KillContext();
               ctx.body.capture(me, 1.0F);
               ctx.body.entityId = -7;
               ctx.feetX = me.posX;
               ctx.feetY = me.posY;
               ctx.feetZ = me.posZ;
               ctx.groundY = me.posY;
               ctx.time = GameClock.nanos();
               KillDetector.fromViewer(ctx, me.posX, me.posY + 1.6, me.posZ + 2.0);
               KillFx fx = new KillFx(s, ctx, 1, this.warmParticles, this, false);
               fx.lod = true;
               fx.start();
               fx.update(0.05F);
            } catch (RuntimeException | LinkageError var6) {
            }

            this.warmParticles.clear();
         }
      }
   }

   void prefetchSkin(EntityPlayer p) {
      if (p instanceof AbstractClientPlayer) {
         ResourceLocation skin = ((AbstractClientPlayer)p).getLocationSkin();
         if (skin != null && !this.skinQueue.contains(skin) && this.skinQueue.size() < 16) {
            this.skinQueue.add(skin);
         }
      }
   }

   private void prefetchOne() {
      if (!this.skinQueue.isEmpty()) {
         ResourceLocation skin = this.skinQueue.remove(0);
         if (Minecraft.getMinecraft().getTextureManager().getTexture(skin) != null) {
            SkinSampler.image(skin);
         }
      }
   }

   @Override
   public boolean hasTestAction() {
      return true;
   }

   @Override
   public String getTestLabel() {
      return "Test kill";
   }

   @Override
   public String getTestTooltip() {
      return "Closes the menu, then kills your test dummy from where you look (singleplayer; it comes back after 3 s). On a server: a copy of you in front of you.";
   }

   @Override
   public void runTest() {
      this.testAt = GameClock.millis() + 250L;
   }

   public void testKill(Minecraft mc, boolean crit) {
      if (mc.thePlayer != null && mc.theWorld != null) {
         if (!mc.isSingleplayer()) {
            this.playOnCopyOfYou(mc, crit);
         } else if (TestDummy.get() != null) {
            this.killDummy(mc, crit);
         } else if (TestDummy.spawnQuietly(mc)) {
            this.dummyKillAt = GameClock.millis() + 700L;
            this.dummyKillCrit = crit;
         }
      }
   }

   private void killDummy(Minecraft mc, boolean crit) {
      EntityOtherPlayerMP d = TestDummy.get();
      EntityPlayerSP me = mc.thePlayer;
      if (d != null && me != null) {
         long now = GameClock.nanos();
         HitContext hit = this.detector.measurer().measure(mc, me, d, now);
         hit.crit = crit;
         this.detector.credit(d, hit, now);
         TestDummy.kill(mc);
      }
   }

   private void playOnCopyOfYou(Minecraft mc, boolean crit) {
      EntityPlayerSP me = mc.thePlayer;
      double yaw = Math.toRadians(me.rotationYaw);
      double x = me.posX - Math.sin(yaw) * 3.0;
      double z;
      double ground = Terrain.surfaceAt(mc.theWorld, x, z = me.posZ + Math.cos(yaw) * 3.0, me.posY + 1.0, 1.5, 6.0);
      double y = Double.isNaN(ground) ? me.posY : ground;
      this.playOnCopyOfYou(mc, x, y, z, me.rotationYaw + 180.0F, -5, crit);
   }

   public void copyKillForTest(double x, double y, double z, float facing, int id, boolean crit) {
      this.playOnCopyOfYou(Minecraft.getMinecraft(), x, y, z, facing, id, crit);
   }

   private void playOnCopyOfYou(Minecraft mc, double x, double y, double z, float facing, int id, boolean crit) {
      EntityPlayerSP me = mc.thePlayer;
      KillContext ctx = new KillContext();
      ctx.body.capture(me, 1.0F);
      ctx.body.entityId = id;
      ctx.body.pose.x = x;
      ctx.body.pose.y = y;
      ctx.body.pose.z = z;
      ctx.body.pose.bodyYaw = facing;
      ctx.body.pose.netHeadYaw = 0.0F;
      ctx.body.pose.headPitch = 0.0F;
      ctx.body.pose.limbSwingAmount = 0.0F;
      ctx.body.pose.swingProgress = 0.0F;
      ctx.body.pose.sneaking = false;
      ctx.body.pose.deathAngle = 0.0F;
      ctx.body.vz = 0.0;
      ctx.body.vy = 0.0;
      ctx.body.vx = 0.0;
      ctx.feetX = x;
      ctx.feetY = y;
      ctx.feetZ = z;
      ctx.groundY = y;
      ctx.victimName = me.getName();
      ctx.method = "test";
      ctx.mine = true;
      ctx.time = GameClock.nanos();
      KillDetector.fromViewer(ctx, me.posX, me.posY + me.getEyeHeight(), me.posZ);
      ctx.crit = crit;
      this.onKill(ctx, null);
   }

   @Override
   public void onWorldChange() {
      this.engine.clear();
      this.particles.clear();
      this.detector.clear();
      this.hidden.clear();
      this.markers.clear();
      this.skinQueue.clear();
      this.pulseAge = -1.0F;
      this.lastPulseNanos = 0L;
      this.streak = 0;
      this.lastKillNanos = 0L;
      this.lastFrameNanos = 0L;
      this.dummyKillAt = 0L;
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

   public int getKillCount() {
      return this.kills;
   }

   public KillContext getLastKill() {
      return this.lastKill;
   }

   public float getLastSetupMillis() {
      return this.lastSetupMillis;
   }

   public String getLastSkip() {
      return this.detector.lastSkip;
   }

   public String getLastChatMatch() {
      return this.detector.lastChatMatch;
   }

   public int getStreak() {
      return this.streak;
   }

   public int getHiddenFrames() {
      return this.hiddenFrames;
   }

   public int getMarkerCount() {
      return this.markers.size();
   }

   public boolean isPulsing() {
      return this.pulseAge >= 0.0F;
   }

   public int getPulseFrames() {
      return this.pulseFrames;
   }

   public List<KillFx> getEffectsForTest() {
      return this.engine.list();
   }

   public void creditForTest(EntityPlayer victim, HitContext hit) {
      this.detector.credit(victim, hit, GameClock.nanos());
   }

   public void chatForTest(String line) {
      this.detector.onChat(Minecraft.getMinecraft(), line);
   }

   @Override
   public int getAccentColor(double time) {
      return this.getSelectedStyle().color.getDisplayColor();
   }

   @Override
   public int getOptionColor(Setting setting, Enum<?> option, double time) {
      return setting == this.style && option instanceof KillStyle ? this.getStyle((KillStyle)option).color.getDisplayColor() : 0;
   }

   @Override
   public boolean isGroupCollapsible(String group) {
      return "Detection".equals(group) || "Performance".equals(group) || "Testing".equals(group);
   }

   @Override
   public boolean isGroupCollapsedByDefault(String group) {
      return "Detection".equals(group);
   }

   @Override
   public boolean hasPreview() {
      return true;
   }

   @Override
   public boolean showsPreviewMannequin() {
      return false;
   }

   @Override
   public String getPreviewLabel() {
      return this.style.get().displayName();
   }

   @Override
   public String getPreviewInfo() {
      return this.renderMillis <= 0.0F ? "Kills come from changing sides" : String.format(Locale.ROOT, "%.2f ms / frame", this.renderMillis);
   }

   @Override
   public void restartPreview() {
      this.previewEngine.clear();
      this.previewParticles.clear();
      this.previewClock = 0.0F;
      this.previewNext = 0.8F;
      this.previewStand = 1.0F;
      this.previewFocusZ = 0.0;
      this.previewFocusX = 0.0;
   }

   private void buildPreviewBody(float ageTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      UUID id = mc.thePlayer != null ? mc.thePlayer.getUniqueID() : mc.getSession().getProfile().getId();
      ResourceLocation skin = SkinHooks.ownSkin();
      boolean slim = SkinHooks.ownSlim();
      BodyProxy.Snapshot b = this.previewBody;
      b.pose.mannequin(ageTicks, slim, 0.0);
      b.skin = skin;
      b.cape = null;
      b.slim = slim;

      for (int i = 0; i < 4; i++) {
         b.armor[i] = null;
      }

      b.held = null;
      b.entity = null;
      b.name = mc.getSession().getUsername();
      b.id = id;
      b.entityId = -2;
      b.vz = 0.0;
      b.vy = 0.0;
      b.vx = 0.0;
      b.light = 15728880;
      b.valid = true;
   }

   private void spawnPreviewKill() {
      int k = this.previewIndex++ % PREVIEW_EYES.length;
      this.buildPreviewBody(this.previewClock * 20.0F);
      KillContext ctx = new KillContext();
      ctx.body.copy(this.previewBody);
      ctx.feetZ = 0.0;
      ctx.feetY = 0.0;
      ctx.feetX = 0.0;
      ctx.groundY = 0.0;
      ctx.victimName = this.previewBody.name;
      ctx.method = "preview";
      ctx.mine = true;
      ctx.time = ClientClock.nanos();
      double cx = this.previewCamera.posX;
      double cz = this.previewCamera.posZ;
      double cl = Math.sqrt(cx * cx + cz * cz);
      if (cl < 0.001) {
         cx = 0.0;
         cz = 1.0;
         cl = 1.0;
      }

      double a = Math.toRadians(PREVIEW_EYES[k][0]);
      double co = Math.cos(a);
      double si = Math.sin(a);
      double ux = cx / cl;
      double uz = cz / cl;
      KillDetector.fromViewer(ctx, (ux * co - uz * si) * PREVIEW_EYES[k][2], PREVIEW_EYES[k][1], (ux * si + uz * co) * PREVIEW_EYES[k][2]);
      ctx.crit = k == 2;
      this.startEffect(ctx, true);
   }

   @Override
   public double getPreviewFocusX() {
      return this.previewFocusX;
   }

   @Override
   public double getPreviewFocusZ() {
      return this.previewFocusZ;
   }

   @Override
   public void renderPreview(float dt) {
      this.previewClock += dt;
      boolean wasRunning = !this.previewEngine.isEmpty();
      if (!wasRunning && this.previewClock >= this.previewNext) {
         this.spawnPreviewKill();
      }

      this.previewEngine.update(dt, 1.0F, null);
      this.previewParticles.update(dt);
      double tx = 0.0;
      double tz = 0.0;
      List<KillFx> running = this.previewEngine.list();
      if (!running.isEmpty() && running.get(running.size() - 1).focus(this.focus)) {
         tx = this.focus[0] * 0.9;
         tz = this.focus[1] * 0.9;
      }

      double k = 1.0 - Math.exp(-dt * 3.0);
      this.previewFocusX = this.previewFocusX + (tx - this.previewFocusX) * k;
      this.previewFocusZ = this.previewFocusZ + (tz - this.previewFocusZ) * k;
      if (wasRunning && this.previewEngine.isEmpty()) {
         this.previewNext = this.previewClock + 1.1F;
         this.previewStand = 0.0F;
      }

      Textures.ensureKillLoaded();
      RenderState.begin();

      try {
         this.previewCamera.captureModelView();
         Vfx.setCamera(this.previewCamera);
         FxFrame f = this.previewFrame;
         f.cam = this.previewCamera;
         f.viewZ = 0.0;
         f.viewY = 0.0;
         f.viewX = 0.0;
         f.eyeX = this.previewCamera.posX;
         f.eyeY = this.previewCamera.posY;
         f.eyeZ = this.previewCamera.posZ;
         f.partialTicks = 1.0F;
         f.dt = dt;
         f.now = ClientClock.nanos();
         f.frameId = FxFrame.currentFrame();
         f.preview = true;
         f.world = null;
         f.firstPerson = false;
         if (this.previewEngine.isEmpty()) {
            this.previewStand = Math.min(1.0F, this.previewStand + dt / 0.35F);
            this.buildPreviewBody(this.previewClock * 20.0F);
            BodyProxy.rest(this.previewBody, this.previewParts);
            this.previewLook.reset();
            this.previewLook.lightmap = false;
            this.previewLook.alpha = Easing.smoothstep(0.0F, 1.0F, this.previewStand);
            BodyProxy.draw(this.previewBody, this.previewParts, this.previewLook, 0.0, 0.0, 0.0);
            RenderState.additive();
         }

         this.previewEngine.render(f);

         for (int i = 0; i < this.previewEngine.size(); i++) {
            KillFx fx = this.previewEngine.list().get(i);
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

   public static enum BodyEnd implements Displayable {
      FADE("Fade", "The body fades away"),
      SINK("Sink", "The body sinks into the ground"),
      DUST("Dust", "The body crumbles to dust");

      private final String displayName;
      private final String description;

      private BodyEnd(String displayName, String description) {
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

   private static final class Hide {
      final int entityId;
      final double x;
      final double y;
      final double z;
      final long until;

      Hide(int entityId, double x, double y, double z, long until) {
         this.entityId = entityId;
         this.x = x;
         this.y = y;
         this.z = z;
         this.until = until;
      }
   }

   public interface KillListener {
      void onMyKill(KillContext var1);
   }

   private static final class Marker {
      double x;
      double y;
      double z;
      float age;
      int color;
      KillEffectsModule.MarkerIcon icon;

      private Marker() {
      }
   }

   public static enum MarkerIcon implements Displayable {
      SKULL("Skull", "A skull"),
      X("X", "A bold X"),
      CROWN("Crown", "A crown"),
      CUSTOM("Custom", "An image from Particles › Custom Images");

      private final String displayName;
      private final String description;

      private MarkerIcon(String displayName, String description) {
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

   public static enum ParticleStyle implements Displayable {
      STYLIZED("Stylized", "Sparks and cubes in the effect color"),
      CLASSIC_RED("Classic Red", "Shades of red, for the gorier styles");

      private final String displayName;
      private final String description;

      private ParticleStyle(String displayName, String description) {
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

   public static enum ShowFor implements Displayable {
      MY_KILLS("My Kills", "Only players you hit before they died"),
      EVERY_DEATH("Every Death", "Every player death you can see");

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
}
