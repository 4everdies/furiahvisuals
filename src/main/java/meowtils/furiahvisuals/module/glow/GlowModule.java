package meowtils.furiahvisuals.module.glow;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.core.HitConfirmer;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.targetaura.TargetAuraModule;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.OptiFineCompat;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ItemListSetting;
import meowtils.furiahvisuals.setting.ItemRulesSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

public final class GlowModule extends Module {
   static final GlowModule.BuiltIn[] BUILT_IN = new GlowModule.BuiltIn[]{
      new GlowModule.BuiltIn(
         "emerald_edge",
         "Emerald Edge",
         "A thin mint rim, a soft halo and a smoky trail",
         ColorSetting.Mode.STATIC,
         -8519782,
         -14756192,
         GlowModule.Direction.EDGE
      ),
      new GlowModule.BuiltIn(
            "white_aura", "White Aura", "An overexposed rim in a wide, milky halo and trail", ColorSetting.Mode.STATIC, -1, -2694401, GlowModule.Direction.EDGE
         )
         .set("rim.width", 3.0)
         .set("rim.intensity", 1.2)
         .set("rim.hotCore", 0.9)
         .set("halo.radius", 32.0)
         .set("halo.intensity", 1.0)
         .set("trail.length", 280.0)
         .set("trail.intensity", 0.9)
         .set("trail.spread", 0.6),
      new GlowModule.BuiltIn(
            "neon_outline",
            "Neon Outline",
            "A crisp neon tube around the item, no trail",
            ColorSetting.Mode.CHROMA,
            -53905,
            -13773569,
            GlowModule.Direction.AROUND
         )
         .set("rim.width", 3.0)
         .set("rim.softness", 0.1)
         .set("rim.intensity", 1.3)
         .set("rim.hotCore", 0.35)
         .set("halo.radius", 5.0)
         .set("trail.length", 0.0),
      new GlowModule.BuiltIn(
            "soft_bloom", "Soft Bloom", "No outline: a wide, dreamy bloom", ColorSetting.Mode.STATIC, -19495, -10516, GlowModule.Direction.EDGE
         )
         .set("rim.width", 0.0)
         .set("halo.radius", 45.0)
         .set("halo.intensity", 0.55)
         .set("halo.falloff", GlowModule.Falloff.EXPONENTIAL.ordinal())
         .set("trail.length", 150.0),
      new GlowModule.BuiltIn(
            "ghost_echo", "Ghost Echo", "Swings leave crisp ghost copies behind", ColorSetting.Mode.STATIC, -11668481, -8745729, GlowModule.Direction.EDGE
         )
         .set("rim.width", 2.0)
         .set("halo.radius", 12.0)
         .set("trail.length", 450.0)
         .set("trail.intensity", 0.9)
         .set("trail.style", GlowModule.TrailStyle.ECHO.ordinal()),
      new GlowModule.BuiltIn(
            "ember", "Ember", "Flickering flames rising off the item", ColorSetting.Mode.GRADIENT, -24023, -54758, GlowModule.Direction.VERTICAL
         )
         .set("rim.width", 2.0)
         .set("halo.radius", 22.0)
         .set("halo.intensity", 1.0)
         .set("flow.amount", 0.6)
         .set("flow.direction", GlowModule.FlowDirection.UP.ordinal())
         .set("flow.scale", 1.2)
         .set("flow.speed", 1.3)
         .set("flicker.amount", 0.3)
         .set("flicker.speed", 9.0),
      new GlowModule.BuiltIn(
            "heartbeat", "Heartbeat", "Beats like a heart, flashes on every hit", ColorSetting.Mode.STATIC, -53941, -34164, GlowModule.Direction.EDGE
         )
         .set("rim.width", 2.5)
         .set("halo.radius", 16.0)
         .set("pulse.depth", 0.6)
         .set("pulse.speed", 1.1)
         .set("pulse.shape", GlowModule.PulseShape.HEARTBEAT.ordinal())
         .set("hitFlash", 1.0),
      new GlowModule.BuiltIn(
            "inner_light", "Inner Light", "Lit from within, with a golden touch", ColorSetting.Mode.STATIC, -11702, -3920, GlowModule.Direction.EDGE
         )
         .set("rim.width", 1.5)
         .set("halo.radius", 8.0)
         .set("inner.glow", 0.6)
         .set("inner.tint", 0.15),
      new GlowModule.BuiltIn(
            "electric",
            "Electric",
            "Fast crackling flicker and a light sweeping along it",
            ColorSetting.Mode.STATIC,
            -12737025,
            -4659969,
            GlowModule.Direction.EDGE
         )
         .set("rim.width", 2.0)
         .set("halo.radius", 10.0)
         .set("flicker.amount", 0.6)
         .set("flicker.speed", 22.0)
         .set("shimmer", 1.0)
         .set("shimmer.speed", 1.6)
         .set("shimmer.width", 0.12)
         .set("shimmer.pause", 0.5)
   };
   private static final long SWING_TRAIL_TAIL_NANOS = 150000000L;
   private static final float ECHO_INTERVAL = 0.022222223F;
   public final GlowPresets presets;
   public final BooleanSetting heldItems;
   public final BooleanSetting bareHand;
   public final ModeSetting<GlowModule.OnlyFor> onlyFor;
   public final ItemListSetting customItems;
   public final ItemRulesSetting overrides;
   public final SliderSetting rimWidth;
   public final SliderSetting rimIntensity;
   public final SliderSetting rimSoftness;
   public final SliderSetting hotCore;
   public final ModeSetting<GlowModule.RimColor> rimColorMode;
   public final ColorSetting rimColor;
   public final SliderSetting haloRadius;
   public final SliderSetting haloIntensity;
   public final SliderSetting haloCurve;
   public final ModeSetting<GlowModule.Falloff> haloFalloff;
   public final SliderSetting innerGlow;
   public final SliderSetting itemTint;
   public final SliderSetting trailLength;
   public final SliderSetting trailIntensity;
   public final SliderSetting trailSpread;
   public final ModeSetting<GlowModule.TrailStyle> trailStyle;
   public final BooleanSetting trailOnlySwings;
   public final BooleanSetting trailColorByAge;
   public final SliderSetting pulseDepth;
   public final SliderSetting pulseSpeed;
   public final ModeSetting<GlowModule.PulseShape> pulseShape;
   public final SliderSetting flickerAmount;
   public final SliderSetting flickerSpeed;
   public final SliderSetting flowAmount;
   public final SliderSetting flowScale;
   public final SliderSetting flowSpeed;
   public final ModeSetting<GlowModule.FlowDirection> flowDirection;
   public final BooleanSetting shimmer;
   public final SliderSetting shimmerSpeed;
   public final SliderSetting shimmerWidth;
   public final SliderSetting shimmerPause;
   public final BooleanSetting hitFlash;
   public final SliderSetting hitFlashAmount;
   public final SliderSetting hitFlashDuration;
   public final SliderSetting targetBoost;
   public final ColorSetting color;
   public final ModeSetting<GlowModule.Direction> direction;
   public final ModeSetting<GlowModule.Blend> blend;
   public final SliderSetting opacity;
   public final ModeSetting<GlowModule.Quality> quality;
   public final SliderSetting alphaCutoff;
   private final List<Setting> presetSettings = new ArrayList<>();
   private final int rimWidthI;
   private final int rimIntensityI;
   private final int rimSoftnessI;
   private final int hotCoreI;
   private final int haloRadiusI;
   private final int haloIntensityI;
   private final int haloFalloffI;
   private final int haloCurveI;
   private final int innerGlowI;
   private final int itemTintI;
   private final int trailLengthI;
   private final int trailIntensityI;
   private final int trailSpreadI;
   private final int trailStyleI;
   private final int pulseDepthI;
   private final int pulseSpeedI;
   private final int pulseShapeI;
   private final int flickerAmountI;
   private final int flickerSpeedI;
   private final int flowAmountI;
   private final int flowScaleI;
   private final int flowSpeedI;
   private final int flowDirectionI;
   private final int shimmerI;
   private final int shimmerSpeedI;
   private final int shimmerWidthI;
   private final int shimmerPauseI;
   private final int hitFlashI;
   private final int hitFlashAmountI;
   private final int hitFlashDurationI;
   private final int targetBoostI;
   private final GlowRenderer renderer = new GlowRenderer();
   private final GlowParams params = new GlowParams();
   private final int[] steps;
   private int[] lookOverride;
   private long lastFrameNanos;
   private long frames;
   private long lastFilledNanos;
   private long lastActiveNanos;
   private float echoClock;
   private float pulseClock;
   private float flickerClock;
   private float flowClock;
   private float shimmerClock;
   private float boostLevel;
   private long flashStart;
   private int flashCount;
   private long infoRequested;
   private boolean forceMeasuring;
   private TargetAuraModule targetAura;
   private final float[] hsv = new float[3];
   private final Module.Notice noWorld = new Module.Notice("Join a world to see the glow live on your hand while you change it.", false);
   private final Module.Notice shaderPack = new Module.Notice(
      "Glow is paused while an OptiFine shader pack is on (shader packs draw the hand their own way). Turn the shader pack off to use it.", true
   );
   private Module.Notice unsupported;
   private Module.Notice simulatedNotice;

   static GlowModule.BuiltIn builtIn(String id) {
      for (GlowModule.BuiltIn b : BUILT_IN) {
         if (b.id.equals(id)) {
            return b;
         }
      }

      return null;
   }

   public GlowModule() {
      super("glow", "Glow", "Your held item or bare hand glows.", true);
      this.presets = this.add(new GlowPresets(this));
      this.heldItems = this.add(new BooleanSetting("heldItems", "Glow Held Items", "The item in your hand glows.", true).group("Target"));
      this.bareHand = this.add(new BooleanSetting("bareHand", "Glow Bare Hand", "Your arm glows when you hold nothing.", true).group("Target"));
      this.onlyFor = this.add(
         new ModeSetting<>("onlyFor", "Only For", "Which held items glow.", GlowModule.OnlyFor.class, GlowModule.OnlyFor.ALL)
            .<ModeSetting>group("Target")
            .visibleWhen(on(this.heldItems))
      );
      this.customItems = this.add(
         new ItemListSetting("customItems", "Items", "The items that glow with Custom list.")
            .<ItemListSetting>group("Target")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return GlowModule.this.heldItems.get() && GlowModule.this.onlyFor.get() == GlowModule.OnlyFor.CUSTOM;
               }
            })
      );
      this.overrides = this.add(
         new ItemRulesSetting("overrides", "Per-Item Overrides", "Give single items their own preset or colors (a bow blue, a sword red).", this.presets)
            .group("Target")
      );
      this.rimWidth = this.preset(
         new SliderSetting("rim.width", "Width", "How wide the bright line around the item is (0: no rim).", 0.0, 6.0, 2.5, 0.1).unit(" px").group("Rim")
      );
      this.rimIntensity = this.preset(new SliderSetting("rim.intensity", "Intensity", "How bright the rim is.", 0.0, 2.0, 1.0, 0.05).group("Rim"));
      this.rimSoftness = this.preset(new SliderSetting("rim.softness", "Softness", "How softly the rim's outer edge fades.", 0.0, 1.0, 0.3, 0.05).group("Rim"));
      this.hotCore = this.preset(
         new SliderSetting("rim.hotCore", "Hot Core", "Pushes the rim towards white right at the outline.", 0.0, 1.0, 0.5, 0.05).group("Rim")
      );
      this.rimColorMode = this.add(
         new ModeSetting<>("rim.colorMode", "Rim Color", "The rim's color.", GlowModule.RimColor.class, GlowModule.RimColor.SAME).group("Rim")
      );
      this.rimColor = this.add(
         new ColorSetting("rim.color", "Rim Own Color", "The rim's own color.", ColorSetting.Mode.STATIC, -1, -4194346, 1.0)
            .secondary()
            .<ColorSetting>group("Rim")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return GlowModule.this.rimColorMode.get() == GlowModule.RimColor.OWN;
               }
            })
      );
      this.haloRadius = this.preset(
         new SliderSetting("halo.radius", "Radius", "How far the soft glow reaches (0: no halo).", 0.0, 64.0, 14.0, 1.0).unit(" px").group("Halo")
      );
      this.haloIntensity = this.preset(new SliderSetting("halo.intensity", "Intensity", "How bright the halo is.", 0.0, 2.0, 0.8, 0.05).group("Halo"));
      this.haloFalloff = this.preset(
         new ModeSetting<>("halo.falloff", "Falloff", "How the halo fades with distance.", GlowModule.Falloff.class, GlowModule.Falloff.SMOOTH).group("Halo")
      );
      this.haloCurve = this.preset(
         new SliderSetting("halo.curve", "Curve", "Below 1 a fuller halo, above 1 a tighter one.", 0.3, 3.0, 1.0, 0.05).unit("x").group("Halo")
      );
      this.innerGlow = this.preset(
         new SliderSetting("inner.glow", "Inner Glow", "The glow also covers the item itself.", 0.0, 1.0, 0.0, 0.05).group("Inside the item")
      );
      this.itemTint = this.preset(
         new SliderSetting("inner.tint", "Item Tint", "Tints the item's own texture (0 keeps it as it is).", 0.0, 1.0, 0.0, 0.05).group("Inside the item")
      );
      this.trailLength = this.preset(
         new SliderSetting("trail.length", "Length", "How long the trail takes to fade (0: no trail).", 0.0, 800.0, 200.0, 10.0).unit(" ms").group("Trail")
      );
      BooleanSupplier trailOn = above(this.trailLength);
      this.trailIntensity = this.preset(
         new SliderSetting("trail.intensity", "Intensity", "How bright the trail is.", 0.0, 2.0, 0.8, 0.05).<SliderSetting>group("Trail").visibleWhen(trailOn)
      );
      this.trailSpread = this.preset(
         new SliderSetting("trail.spread", "Spread", "How much the smoke spreads and swells as it fades.", 0.0, 1.0, 0.4, 0.05)
            .<SliderSetting>group("Trail")
            .visibleWhen(trailOn)
      );
      this.trailStyle = this.preset(
         new ModeSetting<>("trail.style", "Style", "Smoke smears or crisp echoes.", GlowModule.TrailStyle.class, GlowModule.TrailStyle.SMOKE)
            .<ModeSetting>group("Trail")
            .visibleWhen(trailOn)
      );
      this.trailOnlySwings = this.add(
         new BooleanSetting(
               "trail.onlySwings", "Only During Swings And Switches", "No trail from walking or turning: only from swings and item switches.", false
            )
            .<BooleanSetting>group("Trail")
            .visibleWhen(trailOn)
      );
      this.trailColorByAge = this.add(
         new BooleanSetting("trail.colorByAge", "Color By Age", "With a gradient: a fresh trail has color A and turns into color B as it fades.", false)
            .<BooleanSetting>group("Trail")
            .visibleWhen(trailOn)
      );
      this.pulseDepth = this.preset(new SliderSetting("pulse.depth", "Pulse", "How strongly the glow pulses (0: off).", 0.0, 1.0, 0.0, 0.05).group("Animation"));
      BooleanSupplier pulseOn = above(this.pulseDepth);
      this.pulseSpeed = this.preset(
         new SliderSetting("pulse.speed", "Pulse Speed", "Pulses per second.", 0.1, 4.0, 1.0, 0.05)
            .unit(" Hz")
            .<SliderSetting>group("Animation")
            .visibleWhen(pulseOn)
      );
      this.pulseShape = this.preset(
         new ModeSetting<>("pulse.shape", "Pulse Shape", "Breathing or a heartbeat.", GlowModule.PulseShape.class, GlowModule.PulseShape.SINE)
            .<ModeSetting>group("Animation")
            .visibleWhen(pulseOn)
      );
      this.flickerAmount = this.preset(
         new SliderSetting("flicker.amount", "Flicker", "Random flicker, like a flame or a faulty neon (0: off).", 0.0, 1.0, 0.0, 0.05).group("Animation")
      );
      this.flickerSpeed = this.preset(
         new SliderSetting("flicker.speed", "Flicker Speed", "How fast it flickers.", 1.0, 30.0, 12.0, 0.5)
            .unit(" Hz")
            .<SliderSetting>group("Animation")
            .visibleWhen(above(this.flickerAmount))
      );
      this.flowAmount = this.preset(
         new SliderSetting("flow.amount", "Flow Noise", "The glow drifts through moving noise (0: off).", 0.0, 1.0, 0.0, 0.05).group("Animation")
      );
      BooleanSupplier flowOn = above(this.flowAmount);
      this.flowScale = this.preset(
         new SliderSetting("flow.scale", "Flow Scale", "Size of the noise.", 0.25, 4.0, 1.0, 0.05)
            .unit("x")
            .<SliderSetting>group("Animation")
            .visibleWhen(flowOn)
      );
      this.flowSpeed = this.preset(
         new SliderSetting("flow.speed", "Flow Speed", "How fast the noise moves.", 0.1, 3.0, 1.0, 0.05)
            .unit("x")
            .<SliderSetting>group("Animation")
            .visibleWhen(flowOn)
      );
      this.flowDirection = this.preset(
         new ModeSetting<>("flow.direction", "Flow Direction", "Where the noise moves.", GlowModule.FlowDirection.class, GlowModule.FlowDirection.UP)
            .<ModeSetting>group("Animation")
            .visibleWhen(flowOn)
      );
      this.shimmer = this.preset(new BooleanSetting("shimmer", "Shimmer", "A highlight sweeps along the rim now and then.", false).group("Animation"));
      BooleanSupplier shimmerOn = on(this.shimmer);
      this.shimmerSpeed = this.preset(
         new SliderSetting("shimmer.speed", "Shimmer Speed", "How fast the highlight sweeps.", 0.2, 3.0, 1.0, 0.05)
            .unit("x")
            .<SliderSetting>group("Animation")
            .visibleWhen(shimmerOn)
      );
      this.shimmerWidth = this.preset(
         new SliderSetting("shimmer.width", "Shimmer Width", "How wide the highlight is.", 0.05, 0.5, 0.15, 0.01)
            .<SliderSetting>group("Animation")
            .visibleWhen(shimmerOn)
      );
      this.shimmerPause = this.preset(
         new SliderSetting("shimmer.pause", "Shimmer Pause", "Rest between sweeps.", 0.0, 3.0, 0.8, 0.05)
            .unit("s")
            .<SliderSetting>group("Animation")
            .visibleWhen(shimmerOn)
      );
      this.hitFlash = this.preset(
         new BooleanSetting("hitFlash", "Hit Flash", "Flashes brighter and whiter on every confirmed hit (works while Hit Effect is off too).", false)
            .group("Reactive")
      );
      this.hitFlashAmount = this.preset(
         new SliderSetting("hitFlash.amount", "Flash Amount", "How strong the flash is.", 0.1, 2.0, 1.0, 0.05)
            .<SliderSetting>group("Reactive")
            .visibleWhen(on(this.hitFlash))
      );
      this.hitFlashDuration = this.preset(
         new SliderSetting("hitFlash.duration", "Flash Duration", "How long the flash takes to fade.", 50.0, 800.0, 250.0, 10.0)
            .unit(" ms")
            .<SliderSetting>group("Reactive")
            .visibleWhen(on(this.hitFlash))
      );
      this.targetBoost = this.preset(
         new SliderSetting(
               "targetBoost",
               "Target Boost",
               "Brighter and bigger while Target Aura has a target (works while Target Aura is off too; 0: off).",
               0.0,
               1.0,
               0.0,
               0.05
            )
            .group("Reactive")
      );
      this.color = this.add(new ColorSetting("color", "Glow Color", "The glow's color (Global follows the Global Color).", -8519782).group("Color"));
      this.direction = this.add(
         new ModeSetting<>(
               "color.direction",
               "Gradient Direction",
               "Where a gradient's colors run (chroma's spread uses it too).",
               GlowModule.Direction.class,
               GlowModule.Direction.EDGE
            )
            .<ModeSetting>group("Color")
            .visibleWhen(
               new BooleanSupplier() {
                  @Override
                  public boolean getAsBoolean() {
                     return GlowModule.this.color.getEffectiveMode() != ColorSetting.Mode.STATIC
                        || GlowModule.this.rimColorMode.get() == GlowModule.RimColor.OWN
                           && GlowModule.this.rimColor.getEffectiveMode() != ColorSetting.Mode.STATIC;
                  }
               }
            )
      );
      this.blend = this.add(
         new ModeSetting<>("render.blend", "Blend Mode", "How the glow mixes with the scene.", GlowModule.Blend.class, GlowModule.Blend.ADDITIVE)
            .group("Rendering")
      );
      this.opacity = this.add(new SliderSetting("render.opacity", "Opacity", "Fades the whole glow.", 0.0, 1.0, 1.0, 0.05).group("Rendering"));
      this.quality = this.add(
         new ModeSetting<>(
               "render.quality", "Quality", "Resolution of the blur (and of the mask on High).", GlowModule.Quality.class, GlowModule.Quality.MEDIUM
            )
            .group("Rendering")
      );
      this.alphaCutoff = this.add(
         new SliderSetting(
               "render.alphaCutoff",
               "Alpha Cutoff (Advanced)",
               "How opaque a pixel of the item's texture must be to count as part of it.",
               0.01,
               0.9,
               0.1,
               0.01
            )
            .group("Rendering")
      );
      this.rimWidthI = this.index(this.rimWidth);
      this.rimIntensityI = this.index(this.rimIntensity);
      this.rimSoftnessI = this.index(this.rimSoftness);
      this.hotCoreI = this.index(this.hotCore);
      this.haloRadiusI = this.index(this.haloRadius);
      this.haloIntensityI = this.index(this.haloIntensity);
      this.haloFalloffI = this.index(this.haloFalloff);
      this.haloCurveI = this.index(this.haloCurve);
      this.innerGlowI = this.index(this.innerGlow);
      this.itemTintI = this.index(this.itemTint);
      this.trailLengthI = this.index(this.trailLength);
      this.trailIntensityI = this.index(this.trailIntensity);
      this.trailSpreadI = this.index(this.trailSpread);
      this.trailStyleI = this.index(this.trailStyle);
      this.pulseDepthI = this.index(this.pulseDepth);
      this.pulseSpeedI = this.index(this.pulseSpeed);
      this.pulseShapeI = this.index(this.pulseShape);
      this.flickerAmountI = this.index(this.flickerAmount);
      this.flickerSpeedI = this.index(this.flickerSpeed);
      this.flowAmountI = this.index(this.flowAmount);
      this.flowScaleI = this.index(this.flowScale);
      this.flowSpeedI = this.index(this.flowSpeed);
      this.flowDirectionI = this.index(this.flowDirection);
      this.shimmerI = this.index(this.shimmer);
      this.shimmerSpeedI = this.index(this.shimmerSpeed);
      this.shimmerWidthI = this.index(this.shimmerWidth);
      this.shimmerPauseI = this.index(this.shimmerPause);
      this.hitFlashI = this.index(this.hitFlash);
      this.hitFlashAmountI = this.index(this.hitFlashAmount);
      this.hitFlashDurationI = this.index(this.hitFlashDuration);
      this.targetBoostI = this.index(this.targetBoost);
      this.steps = new int[this.presetSettings.size()];

      for (GlowModule.BuiltIn b : BUILT_IN) {
         b.steps = new int[this.presetSettings.size()];

         for (int i = 0; i < this.presetSettings.size(); i++) {
            Setting s = this.presetSettings.get(i);
            Double v = b.values.get(s.getId());
            b.steps[i] = v == null ? PresetCodec.defaultStepOf(s) : valueToStep(s, v);
         }
      }

      FuriahsVisuals.instance.getHitConfirmer().addListener(new HitConfirmer.Listener() {
         @Override
         public void onConfirmedHit(HitConfirmer.Attack attack) {
            if (GlowModule.this.isEnabled() && GlowModule.this.look(GlowModule.this.hitFlashI) > 0.5) {
               GlowModule.this.flashStart = GameClock.nanos();
               GlowModule.this.flashCount++;
            }
         }
      });
      this.quality.addListener(new Runnable() {
         @Override
         public void run() {
            GlowModule.this.renderer.clearTrail();
         }
      });
   }

   private <S extends Setting> S preset(S setting) {
      this.presetSettings.add(setting);
      return this.add(setting);
   }

   private int index(Setting s) {
      return this.presetSettings.indexOf(s);
   }

   private static int valueToStep(Setting s, double v) {
      if (s instanceof SliderSetting) {
         SliderSetting slider = (SliderSetting)s;
         return (int)Math.round((v - slider.getMin()) / slider.getStep());
      } else {
         return (int)Math.round(v);
      }
   }

   private static BooleanSupplier on(final BooleanSetting b) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return b.get();
         }
      };
   }

   private static BooleanSupplier above(final SliderSetting s) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return s.get() > s.getMin();
         }
      };
   }

   int[] currentSteps() {
      for (int i = 0; i < this.steps.length; i++) {
         this.steps[i] = PresetCodec.stepOf(this.presetSettings.get(i));
      }

      return this.steps;
   }

   int[] completeSteps(int[] code) {
      int[] full = new int[this.presetSettings.size()];

      for (int i = 0; i < full.length; i++) {
         full[i] = i < code.length ? code[i] : PresetCodec.defaultStepOf(this.presetSettings.get(i));
      }

      return full;
   }

   void applySteps(int[] values) {
      for (int i = 0; i < this.presetSettings.size() && i < values.length; i++) {
         PresetCodec.apply(this.presetSettings.get(i), values[i]);
      }
   }

   void applyColor(ColorSetting.Mode mode, int a, int b, int dir) {
      this.color.setMode(mode);
      if (mode != ColorSetting.Mode.GLOBAL) {
         this.color.setColorA(a | 0xFF000000);
         this.color.setColorB(b | 0xFF000000);
      }

      this.direction.setIndex(Math.max(0, Math.min(GlowModule.Direction.values().length - 1, dir)));
   }

   String capture(String name) {
      PresetCodec.Preset p = new PresetCodec.Preset();
      p.name = name;
      p.steps = (int[])this.currentSteps().clone();
      p.hasColor = true;
      p.mode = this.color.getMode();
      p.colorA = this.color.getColorA();
      p.colorB = this.color.getColorB();
      p.direction = this.direction.getIndex();
      return PresetCodec.encode(p);
   }

   String encodeBuiltIn(GlowModule.BuiltIn b) {
      PresetCodec.Preset p = new PresetCodec.Preset();
      p.name = b.name;
      p.steps = b.steps;
      p.hasColor = true;
      p.mode = b.mode;
      p.colorA = b.colorA;
      p.colorB = b.colorB;
      p.direction = b.direction.ordinal();
      return PresetCodec.encode(p);
   }

   private double look(int i) {
      Setting s = this.presetSettings.get(i);
      if (this.lookOverride != null) {
         return PresetCodec.valueOf(s, this.lookOverride[i]);
      } else if (s instanceof SliderSetting) {
         return ((SliderSetting)s).get();
      } else if (s instanceof ModeSetting) {
         return ((ModeSetting)s).getIndex();
      } else {
         return ((BooleanSetting)s).get() ? 1.0 : 0.0;
      }
   }

   private float lookF(int i) {
      return (float)this.look(i);
   }

   public static String idOf(ItemStack stack) {
      return ItemListSetting.idOf(stack);
   }

   private boolean shouldGlow(Minecraft mc, ItemStack stack) {
      if (stack == null) {
         return this.bareHand.get() && !mc.thePlayer.isInvisible();
      } else if (!this.heldItems.get()) {
         return false;
      } else {
         switch ((GlowModule.OnlyFor)this.onlyFor.get()) {
            case WEAPONS:
               Item item = stack.getItem();
               return item instanceof ItemSword || item instanceof ItemAxe || item instanceof ItemBow || item instanceof ItemFishingRod;
            case CUSTOM:
               return this.customItems.contains(idOf(stack));
            default:
               return true;
         }
      }
   }

   private boolean available() {
      return this.renderer.unavailableReason() == null && !OptiFineCompat.shadersActive();
   }

   @Override
   public void onRenderHand(float partialTicks, int renderPass) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.thePlayer != null
         && !mc.gameSettings.anaglyph
         && mc.gameSettings.thirdPersonView == 0
         && !mc.gameSettings.hideGUI
         && mc.playerController != null
         && !mc.playerController.isSpectator()
         && this.available()
         && ViewModel.handEnabled(mc)) {
         Entity view = mc.getRenderViewEntity();
         if (!(view instanceof EntityLivingBase) || !((EntityLivingBase)view).isPlayerSleeping()) {
            ItemStack stack = ViewModel.stack(mc);
            this.renderer.renderMask(mc, partialTicks, this.shouldGlow(mc, stack), this.alphaCutoff.getFloat(), this.quality.get().ordinal());
         }
      }
   }

   @Override
   public void onAfterHand(float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      long now = GameClock.nanos();
      this.frames++;
      float dt = this.lastFrameNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = now;
      if (!this.renderer.takeMask()) {
         this.renderer.clearTrail();
      } else {
         boolean filled = this.renderer.maskFilled();
         if (filled) {
            this.lastFilledNanos = now;
         }

         float trailSeconds = this.trailLength.getFloat() / 1000.0F;
         if (!filled) {
            if (this.renderer.isTrailClean()) {
               return;
            }

            if (trailSeconds <= 0.0F || now - this.lastFilledNanos > (long)((trailSeconds * 1.3F + 0.1F) * 1.0E9F)) {
               this.renderer.clearTrail();
               return;
            }
         }

         this.renderer.setMeasuring(this.forceMeasuring || System.nanoTime() - this.infoRequested < 500000000L);
         this.fillParams(mc, dt, now, filled);
         this.renderer.renderGlow(this.params, dt, this.quality.get().ordinal());
      }
   }

   private void fillParams(Minecraft mc, float dt, long now, boolean filled) {
      ItemStack stack = ViewModel.stack(mc);
      ItemRulesSetting.Rule rule = stack == null ? null : this.overrides.find(idOf(stack));
      this.lookOverride = rule != null && rule.getPreset() != null ? this.presets.stepsOf(rule.getPreset()) : null;
      GlowParams p = this.params;
      float px = mc.displayHeight / 1080.0F;
      float pulseGain = 1.0F;
      float pulseSize = 1.0F;
      float depth = this.lookF(this.pulseDepthI);
      this.pulseClock = (this.pulseClock + dt * this.lookF(this.pulseSpeedI)) % 1000.0F;
      if (depth > 0.0F) {
         float phase = this.pulseClock - (float)Math.floor(this.pulseClock);
         float shape = this.look(this.pulseShapeI) >= GlowModule.PulseShape.HEARTBEAT.ordinal()
            ? heartbeat(phase)
            : 0.5F + 0.5F * (float)Math.cos(phase * Math.PI * 2.0);
         pulseGain = 1.0F - depth + depth * shape;
         pulseSize = 1.0F + 0.3F * depth * (shape - 0.5F);
      }

      float flickerGain = 1.0F;
      float flicker = this.lookF(this.flickerAmountI);
      this.flickerClock = (this.flickerClock + dt * this.lookF(this.flickerSpeedI)) % 4096.0F;
      if (flicker > 0.0F) {
         flickerGain = 1.0F - flicker * 0.75F * smoothNoise(this.flickerClock);
      }

      float flash = this.flashLevel(now);
      boolean target = this.lookF(this.targetBoostI) > 0.0F && this.hasTarget();
      this.boostLevel = this.boostLevel + ((target ? 1.0F : 0.0F) - this.boostLevel) * (1.0F - (float)Math.exp(-7.0F * dt));
      float boost = this.boostLevel * this.lookF(this.targetBoostI);
      p.rimWidth = this.lookF(this.rimWidthI) * px * (1.0F + 0.25F * boost + 0.15F * Math.min(1.0F, flash));
      p.rimSoftness = this.lookF(this.rimSoftnessI);
      p.rimIntensity = this.lookF(this.rimIntensityI);
      p.hotCore = this.lookF(this.hotCoreI);
      p.haloRadius = this.lookF(this.haloRadiusI) * px * pulseSize * (1.0F + 0.5F * boost + 0.2F * Math.min(1.0F, flash));
      p.haloIntensity = this.lookF(this.haloIntensityI);
      p.haloFalloff = (int)this.look(this.haloFalloffI);
      p.haloCurve = this.lookF(this.haloCurveI);
      p.innerGlow = this.lookF(this.innerGlowI);
      p.itemTint = this.lookF(this.itemTintI);
      p.trailSeconds = this.lookF(this.trailLengthI) / 1000.0F;
      p.trailIntensity = this.lookF(this.trailIntensityI);
      p.trailSpread = this.lookF(this.trailSpreadI);
      p.trailEcho = this.look(this.trailStyleI) >= GlowModule.TrailStyle.ECHO.ordinal();
      p.trailColorByAge = this.trailColorByAge.get();
      EntityPlayerSP me = mc.thePlayer;
      boolean active = me != null && (me.isSwingInProgress || ViewModel.switching(mc));
      if (active) {
         this.lastActiveNanos = now;
      }

      boolean allowed = filled && (!this.trailOnlySwings.get() || now - this.lastActiveNanos < 150000000L);
      if (p.trailEcho) {
         this.echoClock += dt;
         boolean due = this.echoClock >= 0.022222223F;
         if (due) {
            this.echoClock = Math.min(0.022222223F, this.echoClock - 0.022222223F);
         }

         p.trailCapture = allowed && due;
      } else {
         p.trailCapture = allowed;
      }

      p.flowAmount = this.lookF(this.flowAmountI);
      p.flowCell = 38.0F * px * this.lookF(this.flowScaleI);
      p.flowTravel = this.flowClock = (this.flowClock + dt * this.lookF(this.flowSpeedI) * 0.9F) % 4096.0F;
      p.flowDirection = (int)this.look(this.flowDirectionI);
      p.shimmerStrength = 0.0F;
      if (this.look(this.shimmerI) > 0.5) {
         float sweep = 0.7F / Math.max(0.05F, this.lookF(this.shimmerSpeedI));
         float cycle = sweep + this.lookF(this.shimmerPauseI);
         this.shimmerClock = (this.shimmerClock + dt) % Math.max(0.05F, cycle);
         if (this.shimmerClock < sweep) {
            p.shimmerPos = -0.25F + 1.5F * (this.shimmerClock / sweep);
            p.shimmerStrength = 1.0F;
         }

         p.shimmerWidth = this.lookF(this.shimmerWidthI);
      }

      p.gain = pulseGain * flickerGain * (1.0F + 0.8F * boost) * (1.0F + 0.9F * flash);
      p.flash = Math.min(1.0F, 0.6F * flash);
      p.opacity = this.opacity.getFloat();
      p.blend = this.blend.get().ordinal();
      ColorSetting glow = rule != null && rule.getColor() != null ? rule.getColor() : this.color;
      ColorSetting rim = this.rimColorMode.get() == GlowModule.RimColor.OWN ? this.rimColor : glow;
      GlowModule.Direction dir = this.direction.get();
      this.sample(glow, dir, p.glowColors);
      this.sample(rim, dir, p.rimColors);
      p.direction = dir.ordinal();
      p.mirror = dir == GlowModule.Direction.AROUND && effective(glow).getMode() != ColorSetting.Mode.CHROMA;
      p.gradientReach = p.rimWidth * (1.0F + p.rimSoftness) + 1.0F;
   }

   private void sample(ColorSetting c, GlowModule.Direction dir, int[] out) {
      ColorSetting e = effective(c);
      boolean rainbow = dir == GlowModule.Direction.AROUND && e.getMode() == ColorSetting.Mode.CHROMA;

      for (int i = 0; i < out.length; i++) {
         float t = (float)i / (out.length - 1);
         if (rainbow) {
            ColorUtil.toHsv(e.getColorA(), this.hsv);
            double hue = this.hsv[0] + ColorSetting.clock() * e.getChromaSpeed() * 0.2 + t;
            out[i] = ColorUtil.withAlpha(
               ColorUtil.hsv((float)(hue - Math.floor(hue)), e.getChromaSaturation(), e.getChromaBrightness()), ColorUtil.alpha(e.getColorA())
            );
         } else {
            out[i] = c.getColor(t);
         }
      }
   }

   private static ColorSetting effective(ColorSetting c) {
      return c.getMode() == ColorSetting.Mode.GLOBAL && ColorSetting.getGlobal() != null ? ColorSetting.getGlobal() : c;
   }

   private static float heartbeat(float phase) {
      float a = (phase - 0.1F) / 0.05F;
      float b = (phase - 0.3F) / 0.06F;
      return Math.min(1.0F, Math.max((float)Math.exp(-a * a), 0.75F * (float)Math.exp(-b * b)));
   }

   private static float smoothNoise(float t) {
      int i = (int)Math.floor(t);
      float f = t - i;
      float u = f * f * (3.0F - 2.0F * f);
      return hash(i) + (hash(i + 1) - hash(i)) * u;
   }

   private static float hash(int n) {
      int x = n * 668265261;
      x ^= x >>> 15;
      x *= 739982445;
      x ^= x >>> 12;
      return (x & 65535) / 65535.0F;
   }

   private float flashLevel(long now) {
      if (this.flashStart != 0L && !(this.look(this.hitFlashI) < 0.5)) {
         float t = (float)(now - this.flashStart) / 1.0E9F;
         float duration = Math.max(0.05F, this.lookF(this.hitFlashDurationI) / 1000.0F);
         if (t >= duration) {
            this.flashStart = 0L;
            return 0.0F;
         } else {
            float attack = Math.min(1.0F, t / 0.03F);
            float decay = 1.0F - t / duration;
            return this.lookF(this.hitFlashAmountI) * attack * decay * decay;
         }
      } else {
         return 0.0F;
      }
   }

   private boolean hasTarget() {
      if (this.targetAura == null) {
         Module m = FuriahsVisuals.instance.getModules().get("target_aura");
         if (!(m instanceof TargetAuraModule)) {
            return false;
         }

         this.targetAura = (TargetAuraModule)m;
      }

      return this.targetAura.hasTarget();
   }

   @Override
   public void onWorldChange() {
      this.renderer.clearTrail();
      this.flashStart = 0L;
      this.boostLevel = 0.0F;
      this.lastFrameNanos = 0L;
   }

   @Override
   protected void onDisable() {
      this.renderer.release();
      this.flashStart = 0L;
      this.boostLevel = 0.0F;
      this.lastFrameNanos = 0L;
   }

   @Override
   public int getAccentColor(double time) {
      return this.color.getDisplayColor();
   }

   @Override
   public boolean previewsInWorld() {
      return true;
   }

   @Override
   public boolean isGroupCollapsible(String group) {
      return true;
   }

   @Override
   public boolean isGroupCollapsedByDefault(String group) {
      return "Inside the item".equals(group) || "Animation".equals(group) || "Reactive".equals(group) || "Rendering".equals(group);
   }

   @Override
   public Module.Notice getNotice() {
      if (this.simulatedNotice != null) {
         return this.simulatedNotice;
      } else {
         String reason = this.renderer.unavailableReason();
         if (reason == null) {
            if (OptiFineCompat.shadersActive()) {
               return this.shaderPack;
            } else {
               return Minecraft.getMinecraft().theWorld == null ? this.noWorld : null;
            }
         } else {
            if (this.unsupported == null || !this.unsupported.text.equals(reason)) {
               this.unsupported = new Module.Notice(reason, true);
            }

            return this.unsupported;
         }
      }
   }

   @Override
   public boolean hasTestAction() {
      return true;
   }

   @Override
   public String getTestLabel() {
      return "Swing";
   }

   @Override
   public boolean testClosesMenu() {
      return false;
   }

   @Override
   public String getTestTooltip() {
      return "Swings your arm to show the trail (on your screen only: nothing is sent)";
   }

   @Override
   public void runTest() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      if (p != null && (!p.isSwingInProgress || p.swingProgressInt >= 3 || p.swingProgressInt < 0)) {
         p.swingProgressInt = -1;
         p.isSwingInProgress = true;
      }
   }

   @Override
   public String getPreviewInfo() {
      this.infoRequested = System.nanoTime();
      float gpu = this.renderer.gpuMillis();
      float cpu = this.renderer.cpuMillis();
      if (gpu > 0.0F) {
         return String.format(Locale.ROOT, "%.2f ms GPU  ·  %.2f ms CPU", gpu, cpu);
      } else {
         return cpu > 0.0F ? String.format(Locale.ROOT, "%.2f ms CPU", cpu) : null;
      }
   }

   public void setMeasuring(boolean on) {
      this.forceMeasuring = on;
   }

   public void setProfiling(boolean on) {
      this.renderer.setProfiling(on);
   }

   public String getPartReport() {
      return this.renderer.partReport();
   }

   public float getGpuMillis() {
      return this.renderer.gpuMillis();
   }

   public float getCpuMillis() {
      return this.renderer.cpuMillis();
   }

   public float[] getCenter() {
      return new float[]{this.renderer.centerX(), this.renderer.centerY()};
   }

   public int getMaskScale() {
      return this.renderer.maskScale();
   }

   public String dumpForTest(File dir) {
      return this.renderer.dump(dir);
   }

   public void simulateNoticeForTest(String text) {
      this.simulatedNotice = text == null ? null : new Module.Notice(text, true);
   }

   public long getFramesForTest() {
      return this.frames;
   }

   public int getFlashCountForTest() {
      return this.flashCount;
   }

   public float getBoostForTest() {
      return this.boostLevel;
   }

   public String getRenderSize() {
      return this.renderer.screenWidth() + "x" + this.renderer.screenHeight();
   }

   public void applyPresetForTest(String id) {
      this.presets.apply(id);
      this.presets.applySuggestedColor(id);
   }

   public String checkPresetRow() {
      StringBuilder sb = new StringBuilder();
      this.presets.apply("ghost_echo");
      sb.append("after Ghost Echo the row shows ").append(this.presets.current());
      this.rimWidth.set(this.rimWidth.get() + 0.5);
      sb.append(", after a change ").append(this.presets.current());
      String id = this.presets.saveCurrent();
      sb.append(", saved as ").append(id).append(" (shows ").append(this.presets.current()).append(")");
      int[] saved = (int[])this.currentSteps().clone();
      String code = this.presets.exportCode();
      this.presets.apply("emerald_edge");
      String message = this.presets.importCode(code);
      sb.append(", pasting its code: \"").append(message).append("\", same settings ").append(Arrays.equals(saved, this.currentSteps()));
      this.presets.remove(id);
      this.presets.remove(this.presets.getSelected());
      this.presets.apply("emerald_edge");
      sb.append(", own presets left ").append(this.presets.userPresets().size());
      return sb.toString();
   }

   public String checkPresetCodes() {
      StringBuilder sb = new StringBuilder();

      for (GlowModule.BuiltIn b : BUILT_IN) {
         String code = this.encodeBuiltIn(b);
         PresetCodec.Preset p = PresetCodec.decode(code);
         boolean same = p != null
            && Arrays.equals(this.completeSteps(p.steps), b.steps)
            && p.name.equals(b.name)
            && p.mode == b.mode
            && (p.colorA & 16777215) == (b.colorA & 16777215);
         sb.append(b.name).append(same ? " ok" : " MISMATCH").append(" (").append(code.length()).append(" chars)  ");
      }

      String broken = PresetCodec.encode(new PresetCodec.Preset()).replace('A', 'B');
      sb.append("| a damaged code is ").append(PresetCodec.decode(broken) == null ? "rejected" : "ACCEPTED");
      return sb.toString();
   }

   public static enum Blend implements Displayable {
      ADDITIVE("Additive", "Light added to the scene"),
      SCREEN("Screen", "Softer light, never harsh white"),
      ALPHA("Alpha", "Painted over: dark colors work too");

      private final String displayName;
      private final String description;

      private Blend(String displayName, String description) {
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

   static final class BuiltIn {
      final String id;
      final String name;
      final String description;
      final ColorSetting.Mode mode;
      final int colorA;
      final int colorB;
      final GlowModule.Direction direction;
      final Map<String, Double> values = new HashMap<>();
      int[] steps;

      BuiltIn(String id, String name, String description, ColorSetting.Mode mode, int colorA, int colorB, GlowModule.Direction direction) {
         this.id = id;
         this.name = name;
         this.description = description;
         this.mode = mode;
         this.colorA = colorA;
         this.colorB = colorB;
         this.direction = direction;
      }

      GlowModule.BuiltIn set(String setting, double value) {
         this.values.put(setting, value);
         return this;
      }
   }

   public static enum Direction implements Displayable {
      EDGE("Edge", "From the outline outwards"),
      VERTICAL("Vertical", "Bottom to top"),
      HORIZONTAL("Horizontal", "Left to right"),
      DIAGONAL("Diagonal", "Bottom left to top right"),
      RADIAL("Radial", "From the item's center"),
      AROUND("Around", "Around the item (a chroma rainbow circles it)");

      private final String displayName;
      private final String description;

      private Direction(String displayName, String description) {
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

   public static enum Falloff implements Displayable {
      LINEAR("Linear", "Fades evenly"),
      SMOOTH("Smooth", "Full near the item, soft at the end"),
      EXPONENTIAL("Exponential", "A long, faint tail");

      private final String displayName;
      private final String description;

      private Falloff(String displayName, String description) {
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

   public static enum FlowDirection implements Displayable {
      UP("Up", "Rises like flames"),
      OUTWARD("Outward", "Streams away from the item");

      private final String displayName;
      private final String description;

      private FlowDirection(String displayName, String description) {
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

   public static enum OnlyFor implements Displayable {
      ALL("All items", "Every item you hold"),
      WEAPONS("Weapons", "Swords, axes, bows and fishing rods"),
      CUSTOM("Custom list", "Only the items in the list");

      private final String displayName;
      private final String description;

      private OnlyFor(String displayName, String description) {
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

   public static enum PulseShape implements Displayable {
      SINE("Sine", "Breathes in and out"),
      HEARTBEAT("Heartbeat", "Two quick beats, then rest");

      private final String displayName;
      private final String description;

      private PulseShape(String displayName, String description) {
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

   public static enum Quality implements Displayable {
      LOW("Low", "Quarter-resolution blur"),
      MEDIUM("Medium", "Half-resolution blur"),
      HIGH("High", "Full-resolution blur, 2x mask");

      private final String displayName;
      private final String description;

      private Quality(String displayName, String description) {
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

   public static enum RimColor implements Displayable {
      SAME("Same as glow", "The rim uses the glow color"),
      OWN("Own color", "The rim has a color of its own");

      private final String displayName;
      private final String description;

      private RimColor(String displayName, String description) {
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

   public static enum TrailStyle implements Displayable {
      SMOKE("Smoke", "Soft smears that spread as they fade"),
      ECHO("Echo", "Crisp ghost copies");

      private final String displayName;
      private final String description;

      private TrailStyle(String displayName, String description) {
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
