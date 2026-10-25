package meowtils.furiahvisuals.module.particles;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.core.HitConfirmer;
import meowtils.furiahvisuals.core.TestDummy;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.render.OptiFineCompat;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

public final class ParticlesModule extends Module {
   private static final String G_HIT = "Hit Particles";
   private static final String G_SPAWN = "Always Show & Multipliers";
   private static final String G_LOOK = "Look";
   private static final String G_GLOW = "Glow";
   private static final String G_VISIBILITY = "Visibility";
   private static final String G_IMAGES = "Custom Images";
   private static final String G_PERFORMANCE = "Performance";
   private static final long RECENT_NANOS = 500000000L;
   public final ModeSetting<ParticlesModule.ColorSource> critColor;
   public final ColorSetting critCustom;
   public final BooleanSetting critRandomPoint;
   public final ModeSetting<ParticlesModule.ColorSource> sharpColor;
   public final ColorSetting sharpCustom;
   public final BooleanSetting sharpRandomPoint;
   public final ModeSetting<ParticlesModule.Scope> colorScope;
   public final ModeSetting<ParticlesModule.AppearOn> appearOn;
   public final BooleanSetting alwaysSharp;
   public final BooleanSetting alwaysCrit;
   public final ModeSetting<ParticlesModule.Targets> alwaysTargets;
   public final SliderSetting critMultiplier;
   public final SliderSetting sharpMultiplier;
   public final ModeSetting<ParticlesModule.MultiplierScope> multiplierScope;
   public final ModeSetting<ParticlesModule.Replacement> replacement;
   public final BooleanSetting ignoreParticleSetting;
   public final SliderSetting scale;
   public final ModeSetting<ParticlesModule.AppliesTo> scaleApplies;
   public final SliderSetting pulseAmount;
   public final SliderSetting pulseSpeed;
   public final BooleanSetting pulseRandom;
   public final ModeSetting<ParticlesModule.AppliesTo> pulseApplies;
   public final BooleanSetting spin;
   public final SliderSetting spinSpeed;
   public final SliderSetting spinVariance;
   public final ModeSetting<ParticlesModule.SpinDirection> spinDirection;
   public final BooleanSetting spinRandomStart;
   public final ModeSetting<ParticlesModule.AppliesTo> spinApplies;
   public final SliderSetting flickerAmount;
   public final SliderSetting flickerSpeed;
   public final ModeSetting<ParticlesModule.FlickerStyle> flickerStyle;
   public final ModeSetting<ParticlesModule.FlickerTarget> flickerAffects;
   public final BooleanSetting flickerRandom;
   public final ModeSetting<ParticlesModule.AppliesTo> flickerApplies;
   public final SliderSetting fadeOut;
   public final ModeSetting<ParticlesModule.AppliesTo> fadeApplies;
   public final SliderSetting closeFade;
   public final ModeSetting<ParticlesModule.AppliesTo> closeApplies;
   public final BooleanSetting fullbright;
   public final ModeSetting<ParticlesModule.AppliesTo> fullbrightApplies;
   public final BooleanSetting smoothFiltering;
   public final ModeSetting<ParticlesModule.AppliesTo> smoothApplies;
   public final BooleanSetting glow;
   public final SliderSetting glowSize;
   public final SliderSetting glowIntensity;
   public final ModeSetting<ParticlesModule.GlowColor> glowColorMode;
   public final ColorSetting glowColor;
   public final ModeSetting<ParticlesModule.AppliesTo> glowApplies;
   public final BooleanSetting showCrit;
   public final SliderSetting critAmount;
   public final BooleanSetting showSharp;
   public final SliderSetting sharpAmount;
   final ImageAssignmentSetting critImages;
   public final SliderSetting critImageScale;
   final ImageAssignmentSetting sharpImages;
   public final SliderSetting sharpImageScale;
   public final ModeSetting<ParticlesModule.SquareMode> squareMode;
   public final SliderSetting maxSize;
   public final ModeSetting<ParticlesModule.Filtering> filtering;
   public final SliderSetting maxGlowing;
   final ParticleSprites sprites = new ParticleSprites();
   final ParticleRenderer renderer = new ParticleRenderer(this.sprites);
   final ImageLibrary library = new ImageLibrary(new File(Minecraft.getMinecraft().mcDataDir, "config/furiahsvisuals/particles"));
   private final boolean mixinOk;
   private final Map<Integer, Long> recentAttacks = new HashMap<>();
   private final Map<HitConfirmer.Attack, Integer> waiting = new IdentityHashMap<>();
   private boolean repeat;
   private long pageSeenAt;
   private int repeatTicks;
   private int burstCount;
   private final ParticleRenderer.ImageSet[] imageSets = new ParticleRenderer.ImageSet[]{
      ParticleRenderer.ImageSet.EMPTY, ParticleRenderer.ImageSet.EMPTY, ParticleRenderer.ImageSet.EMPTY
   };
   private final int[] imageSetKey = new int[]{-1, -1, -1};
   private final int[] imageSetLibrary = new int[]{-1, -1, -1};
   private final float[] imageSetScale = new float[]{0.0F, 0.0F, 0.0F};
   private final Map<String, Module.Notice> notices = new HashMap<>();
   private static volatile String renderError;

   public ParticlesModule() {
      super("particles", "Particles", "Your crit and sharpness particles: colors, how many, and how they look.", true);
      this.mixinOk = HookStatus.mixinActive();
      this.add(new WidgetSetting("burstTest", "Burst Test", "A crit and a sharpness burst on your screen only.") {
         @Override
         public Widget createWidget() {
            return new BurstTestWidget(ParticlesModule.this);
         }
      });
      this.add((new WidgetSetting("swatches", "Colors", "How each color source looks with your pack's sprites.") {
         @Override
         public Widget createWidget() {
            return new SwatchWidget(ParticlesModule.this);
         }
      }).group("Hit Particles"));
      this.critColor = this.add(
         new ModeSetting<>(
               "crit.color", "Crit Color", "Where crit particles get their color.", ParticlesModule.ColorSource.class, ParticlesModule.ColorSource.NATIVE
            )
            .group("Hit Particles")
      );
      this.critCustom = this.add(
         new ColorSetting(
               "crit.custom", "Crit Custom Color", "The color of crit particles with Custom (gradients and chroma run over each particle's life).", -19641
            )
            .<ColorSetting>group("Hit Particles")
            .visibleWhen(is(this.critColor, ParticlesModule.ColorSource.CUSTOM))
      );
      this.critRandomPoint = this.add(
         new BooleanSetting(
               "crit.randomPoint",
               "Random Point on Gradient",
               "Each crit particle takes one random point of the gradient (or chroma) for its whole life.",
               false
            )
            .<BooleanSetting>group("Hit Particles")
            .visibleWhen(is(this.critColor, ParticlesModule.ColorSource.CUSTOM))
      );
      this.sharpColor = this.add(
         new ModeSetting<>(
               "sharp.color",
               "Sharpness Color",
               "Where sharpness particles get their color.",
               ParticlesModule.ColorSource.class,
               ParticlesModule.ColorSource.NATIVE
            )
            .group("Hit Particles")
      );
      this.sharpCustom = this.add(
         new ColorSetting(
               "sharp.custom",
               "Sharpness Custom Color",
               "The color of sharpness particles with Custom (gradients and chroma run over each particle's life).",
               -11671297
            )
            .<ColorSetting>group("Hit Particles")
            .visibleWhen(is(this.sharpColor, ParticlesModule.ColorSource.CUSTOM))
      );
      this.sharpRandomPoint = this.add(
         new BooleanSetting(
               "sharp.randomPoint",
               "Random Point on Gradient",
               "Each sharpness particle takes one random point of the gradient (or chroma) for its whole life.",
               false
            )
            .<BooleanSetting>group("Hit Particles")
            .visibleWhen(is(this.sharpColor, ParticlesModule.ColorSource.CUSTOM))
      );
      this.colorScope = this.add(
         new ModeSetting<>(
               "colorScope",
               "Colors Apply To",
               "Recolor every crit and sharpness particle you see, or only those of your own hits.",
               ParticlesModule.Scope.class,
               ParticlesModule.Scope.ALL
            )
            .group("Hit Particles")
      );
      this.appearOn = this.add(
         new ModeSetting<>(
               "appearOn",
               "Hit Particles Appear On",
               "Every attack (vanilla timing) or only hits the server confirmed.",
               ParticlesModule.AppearOn.class,
               ParticlesModule.AppearOn.EVERY_ATTACK
            )
            .group("Always Show & Multipliers")
      );
      this.alwaysSharp = this.add(
         new BooleanSetting(
               "alwaysSharp", "Always Show Sharpness", "Sharpness particles on your hits even without a Sharpness weapon (a fist or a plain sword too).", false
            )
            .group("Always Show & Multipliers")
      );
      this.alwaysCrit = this.add(
         new BooleanSetting(
               "alwaysCrit",
               "Always Show Criticals",
               "Crit particles on your hits even when they are not critical (standing still too). Only the particles: nothing counts as a crit.",
               false
            )
            .group("Always Show & Multipliers")
      );
      this.alwaysTargets = this.add(
         new ModeSetting<>(
               "alwaysTargets",
               "Always Show On",
               "Which targets get the always-shown particles.",
               ParticlesModule.Targets.class,
               ParticlesModule.Targets.PLAYERS
            )
            .<ModeSetting>group("Always Show & Multipliers")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return ParticlesModule.this.alwaysCrit.get() || ParticlesModule.this.alwaysSharp.get();
               }
            })
      );
      this.critMultiplier = this.add(
         new SliderSetting("critMultiplier", "Crit Multiplier", "Extra crit bursts per burst (x1 = vanilla).", 1.0, 10.0, 1.0, 1.0)
            .unit("×")
            .group("Always Show & Multipliers")
      );
      this.sharpMultiplier = this.add(
         new SliderSetting("sharpMultiplier", "Sharpness Multiplier", "Extra sharpness bursts per burst (x1 = vanilla).", 1.0, 10.0, 1.0, 1.0)
            .unit("×")
            .group("Always Show & Multipliers")
      );
      this.multiplierScope = this.add(
         new ModeSetting<>(
               "multiplierScope",
               "Multipliers Apply To",
               "Your hits only, or every burst you see.",
               ParticlesModule.MultiplierScope.class,
               ParticlesModule.MultiplierScope.MINE
            )
            .<ModeSetting>group("Always Show & Multipliers")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return ParticlesModule.this.critMultiplier.getInt() > 1 || ParticlesModule.this.sharpMultiplier.getInt() > 1;
               }
            })
      );
      this.replacement = this.add(
         new ModeSetting<>(
               "replaceWith",
               "Replace With",
               "Another vanilla particle in place of every crit and sharpness particle.",
               ParticlesModule.Replacement.class,
               ParticlesModule.Replacement.NONE
            )
            .style(ModeSetting.Style.CHIPS)
            .group("Always Show & Multipliers")
      );
      this.ignoreParticleSetting = this.add(
         new BooleanSetting(
               "ignoreParticleSetting",
               "Ignore Minecraft's Particles Setting",
               "Crit and sharpness particles show even with Particles set to Decreased or Minimal.",
               false
            )
            .group("Always Show & Multipliers")
      );
      this.scale = this.add(
         new SliderSetting("scale", "Scale", "Draw size of the particles (their motion stays vanilla).", 0.25, 4.0, 1.0, 0.05).unit("×").group("Look")
      );
      this.scaleApplies = this.applies("scale.applies", "Scale", new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ParticlesModule.this.scale.get() != 1.0;
         }
      });
      this.pulseAmount = this.add(
         new SliderSetting("pulse.amount", "Pulse", "How much the size breathes (0: off).", 0.0, 100.0, 0.0, 1.0).unit("%").group("Look")
      );
      BooleanSupplier pulsing = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ParticlesModule.this.pulseAmount.get() > 0.0;
         }
      };
      this.pulseSpeed = this.add(
         new SliderSetting("pulse.speed", "Pulse Speed", "Pulses per second.", 0.2, 8.0, 2.0, 0.1)
            .unit(" Hz")
            .<SliderSetting>group("Look")
            .visibleWhen(pulsing)
      );
      this.pulseRandom = this.add(
         new BooleanSetting("pulse.random", "Random Pulse Phase", "Each particle pulses at its own moment instead of all together.", true)
            .<BooleanSetting>group("Look")
            .visibleWhen(pulsing)
      );
      this.pulseApplies = this.applies("pulse.applies", "Pulse", pulsing);
      this.spin = this.add(new BooleanSetting("spin", "Spin", "The particles turn in place, still facing you.", false).group("Look"));
      BooleanSupplier spinning = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ParticlesModule.this.spin.get();
         }
      };
      this.spinSpeed = this.add(
         new SliderSetting("spin.speed", "Spin Speed", "Degrees per second (0: a fixed random tilt).", 0.0, 1080.0, 180.0, 5.0)
            .unit("°/s")
            .<SliderSetting>group("Look")
            .visibleWhen(spinning)
      );
      this.spinVariance = this.add(
         new SliderSetting("spin.variance", "Spin Speed Variance", "How much each particle's speed differs.", 0.0, 100.0, 30.0, 1.0)
            .unit("%")
            .<SliderSetting>group("Look")
            .visibleWhen(spinning)
      );
      this.spinDirection = this.add(
         new ModeSetting<>(
               "spin.direction", "Spin Direction", "Which way they turn.", ParticlesModule.SpinDirection.class, ParticlesModule.SpinDirection.RANDOM
            )
            .<ModeSetting>group("Look")
            .visibleWhen(spinning)
      );
      this.spinRandomStart = this.add(
         new BooleanSetting("spin.randomStart", "Random Start Angle", "Each particle starts at its own angle.", true)
            .<BooleanSetting>group("Look")
            .visibleWhen(spinning)
      );
      this.spinApplies = this.applies("spin.applies", "Spin", spinning);
      this.flickerAmount = this.add(
         new SliderSetting("flicker.amount", "Flicker", "How strongly they flicker (0: off).", 0.0, 100.0, 0.0, 1.0).unit("%").group("Look")
      );
      BooleanSupplier flickering = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ParticlesModule.this.flickerAmount.get() > 0.0;
         }
      };
      this.flickerSpeed = this.add(
         new SliderSetting("flicker.speed", "Flicker Speed", "Flickers per second.", 1.0, 20.0, 8.0, 0.5)
            .unit(" Hz")
            .<SliderSetting>group("Look")
            .visibleWhen(flickering)
      );
      this.flickerStyle = this.add(
         new ModeSetting<>(
               "flicker.style", "Flicker Style", "Smooth twinkling or a hard blink.", ParticlesModule.FlickerStyle.class, ParticlesModule.FlickerStyle.TWINKLE
            )
            .<ModeSetting>group("Look")
            .visibleWhen(flickering)
      );
      this.flickerAffects = this.add(
         new ModeSetting<>("flicker.affects", "Flicker Affects", "What flickers.", ParticlesModule.FlickerTarget.class, ParticlesModule.FlickerTarget.OPACITY)
            .<ModeSetting>group("Look")
            .visibleWhen(flickering)
      );
      this.flickerRandom = this.add(
         new BooleanSetting("flicker.random", "Random Per Particle", "Each particle flickers on its own instead of all together.", true)
            .<BooleanSetting>group("Look")
            .visibleWhen(flickering)
      );
      this.flickerApplies = this.applies("flicker.applies", "Flicker", flickering);
      this.fadeOut = this.add(
         new SliderSetting("fadeOut", "Smooth Fade-Out", "The last part of each particle's life fades out instead of vanishing (0: off).", 0.0, 100.0, 0.0, 1.0)
            .unit("% of life")
            .group("Look")
      );
      this.fadeApplies = this.applies("fadeOut.applies", "Fade-Out", new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ParticlesModule.this.fadeOut.get() > 0.0;
         }
      });
      this.closeFade = this.add(
         new SliderSetting(
               "closeFade", "Close-Range Fade", "Particles this close to your camera fade out, so they never cover the screen (0: off).", 0.0, 3.0, 0.0, 0.1
            )
            .unit(" blocks")
            .group("Look")
      );
      this.closeApplies = this.applies("closeFade.applies", "Close-Range Fade", new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ParticlesModule.this.closeFade.get() > 0.0;
         }
      });
      this.fullbright = this.add(new BooleanSetting("fullbright", "Fullbright", "Full brightness in the dark too (no world lighting).", false).group("Look"));
      this.fullbrightApplies = this.applies("fullbright.applies", "Fullbright", new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ParticlesModule.this.fullbright.get();
         }
      });
      this.smoothFiltering = this.add(
         new BooleanSetting(
               "smoothFiltering", "Smooth Texture Filtering", "Soft, filtered sprites instead of hard pixels (a mipmapped copy of particles.png).", false
            )
            .group("Look")
      );
      this.smoothApplies = this.applies("smoothFiltering.applies", "Smooth Filtering", new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ParticlesModule.this.smoothFiltering.get();
         }
      });
      this.glow = this.add(
         new BooleanSetting(
               "glow",
               "Particle Glow",
               "A soft, round glow behind each particle (following its size, pulse, flicker and fade). Glowing particles give light: the night does not darken them.",
               false
            )
            .group("Glow")
      );
      BooleanSupplier glowing = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ParticlesModule.this.glow.get();
         }
      };
      this.glowSize = this.add(
         new SliderSetting("glow.size", "Glow Size", "Size of the glow, times the particle's size.", 1.0, 6.0, 2.5, 0.1)
            .unit("×")
            .<SliderSetting>group("Glow")
            .visibleWhen(glowing)
      );
      this.glowIntensity = this.add(
         new SliderSetting("glow.intensity", "Glow Intensity", "How bright the glow is.", 0.0, 2.0, 0.8, 0.05)
            .<SliderSetting>group("Glow")
            .visibleWhen(glowing)
      );
      this.glowColorMode = this.add(
         new ModeSetting<>(
               "glow.colorMode", "Glow Color", "Where the glow gets its color.", ParticlesModule.GlowColor.class, ParticlesModule.GlowColor.PARTICLE
            )
            .<ModeSetting>group("Glow")
            .visibleWhen(glowing)
      );
      this.glowColor = this.add(
         new ColorSetting("glow.color", "Glow Custom Color", "The glow's own color.", -11654)
            .<ColorSetting>group("Glow")
            .<ColorSetting>visibleWhen(glowing)
            .visibleWhen(is(this.glowColorMode, ParticlesModule.GlowColor.CUSTOM))
      );
      this.glowApplies = this.applies("glow.applies", "Glow", glowing);
      this.glowApplies.group("Glow");
      this.showCrit = this.add(new BooleanSetting("showCrit", "Show Crits", "Crit particles appear at all.", true).group("Visibility"));
      this.critAmount = this.add(
         new SliderSetting("critAmount", "Crit Amount", "How many of them spawn.", 0.0, 100.0, 100.0, 1.0)
            .unit("%")
            .<SliderSetting>group("Visibility")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return ParticlesModule.this.showCrit.get();
               }
            })
      );
      this.showSharp = this.add(new BooleanSetting("showSharp", "Show Sharpness", "Sharpness particles appear at all.", true).group("Visibility"));
      this.sharpAmount = this.add(
         new SliderSetting("sharpAmount", "Sharpness Amount", "How many of them spawn.", 0.0, 100.0, 100.0, 1.0)
            .unit("%")
            .<SliderSetting>group("Visibility")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return ParticlesModule.this.showSharp.get();
               }
            })
      );
      this.add((new WidgetSetting("library", "Image Library", "Your particle images.") {
         @Override
         public Widget createWidget() {
            return new LibraryWidget(ParticlesModule.this);
         }
      }).group("Custom Images"));
      this.critImages = this.add(new ImageAssignmentSetting(this, 1, "crit.images", "Crit Image", "What crit particles are drawn with.").group("Custom Images"));
      this.critImageScale = this.add(
         new SliderSetting("crit.imageScale", "Crit Image Scale", "Size of the image, times Scale.", 0.25, 4.0, 1.0, 0.05)
            .unit("×")
            .<SliderSetting>group("Custom Images")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return ParticlesModule.this.critImages.getMode() != ImageAssignmentSetting.Mode.VANILLA;
               }
            })
      );
      this.sharpImages = this.add(
         new ImageAssignmentSetting(this, 2, "sharp.images", "Sharpness Image", "What sharpness particles are drawn with.").group("Custom Images")
      );
      this.sharpImageScale = this.add(
         new SliderSetting("sharp.imageScale", "Sharpness Image Scale", "Size of the image, times Scale.", 0.25, 4.0, 1.0, 0.05)
            .unit("×")
            .<SliderSetting>group("Custom Images")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return ParticlesModule.this.sharpImages.getMode() != ImageAssignmentSetting.Mode.VANILLA;
               }
            })
      );
      this.squareMode = this.add(
         new ModeSetting<>(
               "images.square",
               "Square Mode",
               "How a non-square image becomes square (an image that already is square stays as it is).",
               ParticlesModule.SquareMode.class,
               ParticlesModule.SquareMode.STRETCH
            )
            .group("Custom Images")
      );
      this.maxSize = this.add(
         new SliderSetting("images.maxSize", "Max Size", "Bigger images are scaled down to this.", 64.0, 1024.0, 512.0, 32.0)
            .unit(" px")
            .group("Custom Images")
      );
      this.filtering = this.add(
         new ModeSetting<>("images.filtering", "Filtering", "How images are filtered.", ParticlesModule.Filtering.class, ParticlesModule.Filtering.AUTO)
            .group("Custom Images")
      );
      this.maxGlowing = this.add(
         new SliderSetting(
               "maxGlowing", "Max Glowing Per Frame", "More glowing particles than this: the farthest skip their glow.", 100.0, 4000.0, 1000.0, 100.0
            )
            .group("Performance")
      );
      Runnable processing = new Runnable() {
         @Override
         public void run() {
            ParticlesModule.this.library.setProcessing(ParticlesModule.this.square(), ParticlesModule.this.maxSize.getInt());
         }
      };
      this.squareMode.addListener(processing);
      this.maxSize.addListener(processing);
      this.filtering.addListener(new Runnable() {
         @Override
         public void run() {
            ParticlesModule.this.library.setFiltering(ParticlesModule.this.filtering.get());
         }
      });
      FuriahsVisuals.instance.getHitConfirmer().addListener(new HitConfirmer.Listener() {
         @Override
         public void onConfirmedHit(HitConfirmer.Attack attack) {
            ParticlesModule.this.confirmed(attack);
         }
      });
      this.sprites.register();
      ParticleHooks.module = this;
   }

   private static <E extends Enum<E>> BooleanSupplier is(final ModeSetting<E> setting, final E value) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return setting.get() == value;
         }
      };
   }

   private ModeSetting<ParticlesModule.AppliesTo> applies(String id, String what, BooleanSupplier on) {
      return this.add(
         new ModeSetting<>(
               id,
               what + " Applies To",
               "Which hit particles get " + what.toLowerCase(Locale.ROOT) + ".",
               ParticlesModule.AppliesTo.class,
               ParticlesModule.AppliesTo.BOTH
            )
            .<ModeSetting>group("Look")
            .visibleWhen(on)
      );
   }

   private ImageProcessor.Square square() {
      switch ((ParticlesModule.SquareMode)this.squareMode.get()) {
         case FIT:
            return ImageProcessor.Square.FIT;
         case CROP:
            return ImageProcessor.Square.CROP;
         default:
            return ImageProcessor.Square.STRETCH;
      }
   }

   boolean isActive() {
      return this.mixinOk && this.isEnabled();
   }

   ModeSetting<ParticlesModule.ColorSource> colorSource(int kind) {
      return kind == 2 ? this.sharpColor : this.critColor;
   }

   ColorSetting customColor(int kind) {
      return kind == 2 ? this.sharpCustom : this.critCustom;
   }

   BooleanSetting randomPoint(int kind) {
      return kind == 2 ? this.sharpRandomPoint : this.critRandomPoint;
   }

   ImageAssignmentSetting images(int kind) {
      return kind == 2 ? this.sharpImages : this.critImages;
   }

   boolean shows(int kind) {
      return kind == 2 ? this.showSharp.get() : this.showCrit.get();
   }

   float amount(int kind) {
      return (kind == 2 ? this.sharpAmount : this.critAmount).getFloat() / 100.0F;
   }

   int multiplier(int kind, boolean mine) {
      return !mine && this.multiplierScope.get() == ParticlesModule.MultiplierScope.MINE
         ? 1
         : (kind == 2 ? this.sharpMultiplier : this.critMultiplier).getInt();
   }

   boolean realHitsOnly() {
      return this.appearOn.get() == ParticlesModule.AppearOn.REAL_HITS && realHitsHooks();
   }

   private static boolean realHitsHooks() {
      return HookStatus.has("critPrediction") && HookStatus.has("sharpPrediction") && HookStatus.has("critPacket") && HookStatus.has("emitHead");
   }

   boolean attackedRecently(Entity entity) {
      Long at = this.recentAttacks.get(entity.getEntityId());
      return at != null && GameClock.nanos() - at < 500000000L;
   }

   int pickImage(int kind, int seed) {
      int n = this.images(kind).activeCount();
      if (n == 0) {
         return 0;
      } else {
         int slot = n == 1 ? 0 : (seed >>> 1) % n;
         return 16 | Math.min(255, slot) << 8;
      }
   }

   ParticleRenderer.ImageSet imageSet(int kind) {
      ImageAssignmentSetting a = this.images(kind);
      float scale = (kind == 2 ? this.sharpImageScale : this.critImageScale).getFloat();
      int key = a.version() * 31 + a.getMode().ordinal();
      if (this.imageSetKey[kind] == key && this.imageSetLibrary[kind] == this.library.version() && this.imageSetScale[kind] == scale) {
         return this.imageSets[kind];
      } else {
         List<String> names = a.active();
         int[] textures = new int[names.size()];
         int[] averages = new int[names.size()];

         for (int i = 0; i < names.size(); i++) {
            ImageLibrary.Entry e = this.library.find(names.get(i));
            boolean ready = e != null && e.state == ImageLibrary.State.READY && e.texture > 0;
            textures[i] = ready ? e.texture : -1;
            averages[i] = ready ? e.average : -1;
         }

         this.imageSets[kind] = names.isEmpty() ? ParticleRenderer.ImageSet.EMPTY : new ParticleRenderer.ImageSet(textures, averages, scale);
         this.imageSetKey[kind] = key;
         this.imageSetLibrary[kind] = this.library.version();
         this.imageSetScale[kind] = scale;
         return this.imageSets[kind];
      }
   }

   static void reportRenderError(Throwable e) {
      if (renderError == null) {
         renderError = e.toString();
         FuriahsVisuals.LOG.error("Particles: drawing failed and is off until the game restarts", e);
      }
   }

   @Override
   public void onAttack(EntityPlayer attacker, Entity target) {
      Minecraft mc = Minecraft.getMinecraft();
      EntityPlayerSP me = mc.thePlayer;
      if (attacker == me && target != null && this.mixinOk) {
         this.recentAttacks.put(target.getEntityId(), GameClock.nanos());
         boolean living = target instanceof EntityLivingBase;
         float damage = (float)me.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
         float bonus = EnchantmentHelper.getModifierForCreature(
            me.getHeldItem(), living ? ((EntityLivingBase)target).getCreatureAttribute() : EnumCreatureAttribute.UNDEFINED
         );
         boolean hurts = damage > 0.0F || bonus > 0.0F;
         boolean vanillaCrit = hurts
            && me.fallDistance > 0.0F
            && !me.onGround
            && !me.isOnLadder()
            && !me.isInWater()
            && !me.isPotionActive(Potion.blindness)
            && me.ridingEntity == null
            && living;
         boolean vanillaSharp = hurts && bonus > 0.0F;
         boolean eligible = this.alwaysTargets.get() == ParticlesModule.Targets.PLAYERS ? target instanceof EntityPlayer : living;
         boolean forceCrit = this.alwaysCrit.get() && eligible;
         boolean forceSharp = this.alwaysSharp.get() && eligible;
         if (this.realHitsOnly()) {
            HitConfirmer.Attack attack = FuriahsVisuals.instance.getHitConfirmer().lastAttack();
            int bursts = (!vanillaCrit && !forceCrit ? 0 : 1) | (!vanillaSharp && !forceSharp ? 0 : 2);
            if (attack != null && attack.target == target && bursts != 0) {
               this.waiting.put(attack, bursts);
            }
         } else if (target != TestDummy.get()) {
            if (forceCrit && !vanillaCrit) {
               ParticleHooks.emitMine(target, EnumParticleTypes.CRIT);
            }

            if (forceSharp && !vanillaSharp) {
               ParticleHooks.emitMine(target, EnumParticleTypes.CRIT_MAGIC);
            }
         } else {
            if (vanillaCrit || forceCrit) {
               ParticleHooks.emitMine(target, EnumParticleTypes.CRIT);
            }

            if (vanillaSharp || forceSharp) {
               ParticleHooks.emitMine(target, EnumParticleTypes.CRIT_MAGIC);
            }
         }
      }
   }

   private void confirmed(HitConfirmer.Attack attack) {
      Integer bursts = this.waiting.remove(attack);
      if (bursts != null && this.isEnabled() && this.realHitsOnly()) {
         Entity target = attack.target;
         if (target != null && !target.isDead && target.worldObj == Minecraft.getMinecraft().theWorld) {
            if ((bursts & 1) != 0) {
               ParticleHooks.emitMine(target, EnumParticleTypes.CRIT);
            }

            if ((bursts & 2) != 0) {
               ParticleHooks.emitMine(target, EnumParticleTypes.CRIT_MAGIC);
            }
         }
      }
   }

   @Override
   public void onTick() {
      if (!this.library.hasScanned()) {
         this.library.refresh();
      }

      this.library.update();
      long now = GameClock.nanos();
      Iterator<Long> it = this.recentAttacks.values().iterator();

      while (it.hasNext()) {
         if (now - it.next() > 2000000000L) {
            it.remove();
         }
      }

      if (!this.waiting.isEmpty()) {
         HitConfirmer confirmer = FuriahsVisuals.instance.getHitConfirmer();
         Iterator<HitConfirmer.Attack> it2 = this.waiting.keySet().iterator();

         while (it2.hasNext()) {
            if (!confirmer.isPending(it2.next())) {
               it2.remove();
            }
         }
      }

      if (this.repeat) {
         if (ClientClock.millis() - this.pageSeenAt > 500L) {
            this.repeat = false;
         } else if (++this.repeatTicks >= 20) {
            this.repeatTicks = 0;
            this.burst();
         }
      }
   }

   @Override
   public void onWorldChange() {
      this.recentAttacks.clear();
      this.waiting.clear();
      ParticleHooks.reset();
   }

   @Override
   protected void onDisable() {
      this.recentAttacks.clear();
      this.waiting.clear();
      this.repeat = false;
   }

   public void burst() {
      Minecraft mc = Minecraft.getMinecraft();
      EntityPlayerSP p = mc.thePlayer;
      if (p != null && mc.theWorld != null && this.isActive()) {
         Object target = TestDummy.get();
         if (target == null) {
            double yaw = Math.toRadians(p.rotationYaw + 26.0F);
            double x = p.posX - Math.sin(yaw) * 2.4;
            double z = p.posZ + Math.cos(yaw) * 2.4;
            double y = p.posY + p.getEyeHeight() - 0.25 - 0.9;
            ParticlesModule.BurstMarker marker = new ParticlesModule.BurstMarker(mc.theWorld);
            marker.setPosition(x, y, z);
            target = marker;
         }

         ParticleHooks.emitMine((Entity)target, EnumParticleTypes.CRIT);
         ParticleHooks.emitMine((Entity)target, EnumParticleTypes.CRIT_MAGIC);
         this.burstCount++;
      }
   }

   boolean isRepeating() {
      return this.repeat;
   }

   void setRepeating(boolean on) {
      this.repeat = on;
      this.repeatTicks = 0;
      this.pageSeenAt = ClientClock.millis();
   }

   void pageShown() {
      this.pageSeenAt = ClientClock.millis();
      this.library.update();
   }

   @Override
   public int getAccentColor(double time) {
      return this.critColor.get() == ParticlesModule.ColorSource.CUSTOM ? this.critCustom.getDisplayColor() : -18355;
   }

   @Override
   public boolean hasTestAction() {
      return true;
   }

   @Override
   public String getTestLabel() {
      return "Burst";
   }

   @Override
   public boolean testClosesMenu() {
      return false;
   }

   @Override
   public String getTestTooltip() {
      return "A crit and a sharpness burst on the test dummy, or next to you (your screen only)";
   }

   @Override
   public void runTest() {
      this.burst();
   }

   @Override
   public boolean previewsInWorld() {
      return true;
   }

   @Override
   public String summary() {
      return this.replacement.get() != ParticlesModule.Replacement.NONE ? ModeSetting.displayName(this.replacement.get()) : "Crits & sharpness";
   }

   @Override
   public boolean isGroupCollapsible(String group) {
      return true;
   }

   @Override
   public boolean isGroupCollapsedByDefault(String group) {
      return "Performance".equals(group);
   }

   @Override
   public String getPreviewInfo() {
      this.renderer.requestMeasuring();
      int n = this.renderer.getParticleCount();
      return n == 0
         ? null
         : String.format(Locale.ROOT, "%d hit particles  ·  %d glowing  ·  %.2f ms", n, this.renderer.getGlowCount(), this.renderer.getMillis());
   }

   @Override
   public Module.Notice getNotice() {
      if (!this.mixinOk) {
         return this.notice(
            "Particles needs Mixin, which did not load here, so this section is off. (Keep the mod's jar in the mods folder as it is: its manifest starts Mixin.)",
            true
         );
      } else {
         StringBuilder sb = new StringBuilder();
         String error = renderError;
         if (error != null) {
            sb.append("Drawing the particles failed, so the section's look is off until the game restarts (").append(error).append("). ");
         }

         ArrayList<String> off = new ArrayList<>();
         if (!HookStatus.has("spawnTail") || !HookStatus.has("drawParticle") || !HookStatus.has("drawStart")) {
            off.add("colors and look");
         }

         if (!HookStatus.has("spawnHead")) {
            off.add("Show, Amount and Replace with");
         }

         if (!HookStatus.has("emitHead") || !HookStatus.has("emitterSpawn")) {
            off.add("multipliers and \"my hits\"");
         }

         if (!HookStatus.has("bypassSetting")) {
            off.add("ignoring the Particles setting");
         }

         if (!realHitsHooks()) {
            off.add("Real hits only");
         }

         if (!this.renderer.canUseBatches()) {
            off.add("custom images, smooth filtering and glow");
         }

         if (!off.isEmpty()) {
            sb.append("Off here, because another mod or OptiFine changed that part of Minecraft: ");

            for (int i = 0; i < off.size(); i++) {
               sb.append(i == 0 ? "" : (i == off.size() - 1 ? " and " : ", ")).append(off.get(i));
            }

            sb.append(". ");
         }

         String missing;
         if ((missing = this.missingImages()) != null) {
            sb.append(missing).append(' ');
         }

         if (OptiFineCompat.shadersActive()) {
            sb.append("A shader pack is on: it draws particles its own way, so glow and images may look different. ");
         }

         String text;
         return (text = sb.toString().trim()).isEmpty() ? null : this.notice(text, false);
      }
   }

   @Override
   public String getLoadWarning() {
      this.library.refresh();
      return this.missingImages();
   }

   private String missingImages() {
      if (!this.library.hasScanned()) {
         return null;
      } else {
         StringBuilder sb = null;

         for (int kind = 1; kind <= 2; kind++) {
            for (String name : this.images(kind).active()) {
               ImageLibrary.Entry e = this.library.find(name);
               if (e == null || e.state == ImageLibrary.State.ERROR) {
                  if (sb == null) {
                     sb = new StringBuilder();
                  } else {
                     sb.append(' ');
                  }

                  sb.append(e == null ? "Missing image: " : "Unusable image: ")
                     .append(name)
                     .append(" (")
                     .append(kind == 1 ? "crits" : "sharpness")
                     .append(" use the vanilla sprite instead).");
               }
            }
         }

         return sb == null ? null : sb.toString();
      }
   }

   public void ensureLibrary() {
      if (!this.library.hasScanned()) {
         this.library.refresh();
      }

      this.library.update();
   }

   public int imageTexture(String name) {
      if (name != null && !name.isEmpty()) {
         ImageLibrary.Entry e = this.library.find(name);
         return e != null && e.state == ImageLibrary.State.READY && e.texture > 0 ? e.texture : -1;
      } else {
         return -1;
      }
   }

   public List<String> imageNames() {
      ArrayList<String> names = new ArrayList<>();

      for (ImageLibrary.Entry e : this.library.entries()) {
         if (e.state != ImageLibrary.State.ERROR) {
            names.add(e.name);
         }
      }

      return names;
   }

   public int libraryVersion() {
      return this.library.version();
   }

   public File libraryFolder() {
      return this.library.getDir();
   }

   private Module.Notice notice(String text, boolean blocking) {
      String key = (blocking ? "!" : "") + text;
      Module.Notice n = this.notices.get(key);
      if (n == null) {
         if (this.notices.size() > 32) {
            this.notices.clear();
         }

         n = new Module.Notice(text, blocking);
         this.notices.put(key, n);
      }

      return n;
   }

   public int getBurstCountForTest() {
      return this.burstCount;
   }

   public int[] countsForTest() {
      return new int[]{
         ParticleHooks.BURSTS[1],
         ParticleHooks.BURSTS[2],
         ParticleHooks.SPAWNED[1],
         ParticleHooks.SPAWNED[2],
         ParticleHooks.MINE_SPAWNED[1] + ParticleHooks.MINE_SPAWNED[2]
      };
   }

   public void foreignBurstForTest(Entity target, boolean sharp) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.effectRenderer != null) {
         mc.effectRenderer.emitParticleAtEntity(target, sharp ? EnumParticleTypes.CRIT_MAGIC : EnumParticleTypes.CRIT);
      }
   }

   public boolean realHitsActiveForTest() {
      return this.realHitsOnly();
   }

   public void burstAtForTest(double x, double y, double z) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.theWorld != null && this.isActive()) {
         ParticlesModule.BurstMarker marker = new ParticlesModule.BurstMarker(mc.theWorld);
         marker.setPosition(x, y - 0.9, z);
         ParticleHooks.emitMine(marker, EnumParticleTypes.CRIT);
         ParticleHooks.emitMine(marker, EnumParticleTypes.CRIT_MAGIC);
      }
   }

   public int getDrawnForTest() {
      return this.renderer.getParticleCount();
   }

   public int getGlowsForTest() {
      return this.renderer.getGlowCount();
   }

   public int getBatchesForTest() {
      return this.renderer.getBatchCount();
   }

   public float getMillisForTest() {
      return this.renderer.getMillis();
   }

   public float getFrameMillisForTest() {
      return this.renderer.getFrameMillis();
   }

   public void setMeasuringForTest(boolean on) {
      this.renderer.setMeasuring(on);
   }

   public void setProfilingForTest(boolean on) {
      this.renderer.setProfiling(on);
   }

   public String profileForTest() {
      return this.renderer.profileReport() + String.format(Locale.ROOT, "; GPU %.3f ms", this.renderer.getGpuMillis());
   }

   public String hooksForTest() {
      return HookStatus.describe();
   }

   public boolean isPlainSpriteForTest(boolean sharp) {
      return this.sprites.isPlain(sharp);
   }

   public void assignImagesForTest(boolean sharp, String... names) {
      ImageAssignmentSetting a = this.images(sharp ? 2 : 1);
      a.reset();
      if (names.length != 0 && (names.length != 1 || !names[0].isEmpty())) {
         a.setMode(names.length == 1 ? ImageAssignmentSetting.Mode.IMAGE : ImageAssignmentSetting.Mode.RANDOM);

         for (String n : names) {
            a.click(n);
         }
      }
   }

   public File getLibraryDirForTest() {
      return this.library.getDir();
   }

   public void refreshLibraryForTest() {
      this.library.refresh();
   }

   public String describeLibraryForTest() {
      this.library.update();
      StringBuilder sb = new StringBuilder();

      for (ImageLibrary.Entry e : this.library.entries()) {
         sb.append(e.name)
            .append(": ")
            .append(e.state == ImageLibrary.State.READY ? e.sizes() : (e.state == ImageLibrary.State.ERROR ? "ERROR " + e.error : "waiting"))
            .append("; ");
      }

      return sb.toString();
   }

   public boolean libraryReadyForTest() {
      this.library.update();

      for (ImageLibrary.Entry e : this.library.entries()) {
         if (e.state == ImageLibrary.State.WAITING) {
            return false;
         }
      }

      return true;
   }

   public void pickForTest() {
      ImageImport.pick(this.library);
   }

   public boolean pickingForTest() {
      return ImageImport.isPicking();
   }

   public String libraryMessageForTest() {
      this.library.update();
      return this.library.message();
   }

   public void deleteImageForTest(String name) {
      if (this.library.delete(name)) {
         this.critImages.remove(name);
         this.sharpImages.remove(name);
      }
   }

   public static enum AppearOn implements Displayable {
      EVERY_ATTACK("Every attack", "Vanilla timing: whenever you attack"),
      REAL_HITS("Real hits only", "One burst per hit the server confirmed; none while the target cannot take damage");

      private final String displayName;
      private final String description;

      private AppearOn(String displayName, String description) {
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

   public static enum AppliesTo implements Displayable {
      BOTH("Crits & sharpness", "Both kinds of hit particles", 6),
      CRITS("Crits", "Only critical hit particles", 2),
      SHARPNESS("Sharpness", "Only sharpness particles", 4);

      private final String displayName;
      private final String description;
      final int mask;

      private AppliesTo(String displayName, String description, int mask) {
         this.displayName = displayName;
         this.description = description;
         this.mask = mask;
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

   private static final class BurstMarker extends Entity {
      BurstMarker(World world) {
         super(world);
         this.setSize(0.6F, 1.8F);
      }

      protected void entityInit() {
      }

      protected void readEntityFromNBT(NBTTagCompound tag) {
      }

      protected void writeEntityToNBT(NBTTagCompound tag) {
      }
   }

   public static enum ColorSource implements Displayable {
      VANILLA("Vanilla", "Minecraft's tint: grey drifting to orange (sharpness: cyan)"),
      NATIVE("Native", "The sprite exactly as drawn in the texture, for its whole life"),
      CUSTOM("Custom", "A color of your own");

      private final String displayName;
      private final String description;

      private ColorSource(String displayName, String description) {
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

   public static enum Filtering implements Displayable {
      AUTO("Auto", "Pixel for images of 64 px or less, Smooth for bigger ones"),
      SMOOTH("Smooth", "Linear filtering with mipmaps"),
      PIXEL("Pixel", "Nearest: crisp pixels");

      private final String displayName;
      private final String description;

      private Filtering(String displayName, String description) {
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

   public static enum FlickerStyle implements Displayable {
      TWINKLE("Twinkle", "Smooth, random wavering"),
      STROBE("Strobe", "Hard on / off blinking");

      private final String displayName;
      private final String description;

      private FlickerStyle(String displayName, String description) {
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

   public static enum FlickerTarget implements Displayable {
      OPACITY("Opacity", "The particles fade in and out"),
      BRIGHTNESS("Brightness", "The particles dim and brighten");

      private final String displayName;
      private final String description;

      private FlickerTarget(String displayName, String description) {
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

   public static enum GlowColor implements Displayable {
      PARTICLE("Particle color", "The sprite's average color times its tint"),
      CUSTOM("Custom", "A color of your own");

      private final String displayName;
      private final String description;

      private GlowColor(String displayName, String description) {
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

   public static enum MultiplierScope implements Displayable {
      MINE("My hits", "Only your own hits get more particles"),
      ALL("All hits I see", "Every crit and sharpness burst you see");

      private final String displayName;
      private final String description;

      private MultiplierScope(String displayName, String description) {
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

   public static enum Replacement implements Displayable {
      NONE("None", "Crits and sharpness stay themselves", null, 0.0),
      HEARTS("Hearts", "Hearts float up", EnumParticleTypes.HEART, 0.0),
      FLAMES("Flames", "Small flames drift out", EnumParticleTypes.FLAME, 0.05),
      REDSTONE("Redstone dust", "Red dust", EnumParticleTypes.REDSTONE, 0.0),
      NOTES("Notes", "Music notes in every color", EnumParticleTypes.NOTE, 0.0),
      HAPPY_VILLAGER("Happy villager", "Green sparkles", EnumParticleTypes.VILLAGER_HAPPY, 1.0),
      ANGRY_VILLAGER("Angry villager", "Angry clouds", EnumParticleTypes.VILLAGER_ANGRY, 0.0),
      WITCH_MAGIC("Witch magic", "Purple swirls", EnumParticleTypes.SPELL_WITCH, 1.0),
      SPELL("Spell", "White swirls", EnumParticleTypes.SPELL, 1.0),
      LAVA("Lava", "Popping lava drops", EnumParticleTypes.LAVA, 0.0),
      SNOWBALL("Snowball poof", "Bits of snowball", EnumParticleTypes.SNOWBALL, 0.0),
      CLOUD("Cloud", "Puffs of cloud", EnumParticleTypes.CLOUD, 0.1),
      FIREWORK_SPARK("Firework spark", "Firework sparks fly out", EnumParticleTypes.FIREWORKS_SPARK, 0.15),
      PORTAL("Portal", "Portal specks pulled into the hit", EnumParticleTypes.PORTAL, 0.8);

      private final String displayName;
      private final String description;
      private final EnumParticleTypes type;
      private final double speed;

      private Replacement(String displayName, String description, EnumParticleTypes type, double speed) {
         this.displayName = displayName;
         this.description = description;
         this.type = type;
         this.speed = speed;
      }

      int particleId() {
         return this.type.getParticleID();
      }

      double speedX(double v) {
         return this == NOTES ? ParticleHooks.nextFloat() : v * this.speed;
      }

      double speedY(double v) {
         return this == NOTES ? 0.0 : v * this.speed;
      }

      double speedZ(double v) {
         return this == NOTES ? 0.0 : v * this.speed;
      }

      int[] parameters(int[] original) {
         return original;
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

   public static enum Scope implements Displayable {
      ALL("All I see", "Every crit and sharpness particle on your screen"),
      MINE("My hits only", "Only the particles of your own hits");

      private final String displayName;
      private final String description;

      private Scope(String displayName, String description) {
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

   public static enum SpinDirection implements Displayable {
      RANDOM("Random", "Each particle picks a direction"),
      CLOCKWISE("Clockwise", "All turn clockwise"),
      COUNTER_CLOCKWISE("Counter-clockwise", "All turn counter-clockwise");

      private final String displayName;
      private final String description;

      private SpinDirection(String displayName, String description) {
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

   public static enum SquareMode implements Displayable {
      STRETCH("Stretch", "To a square as wide as the longer side (300×200 becomes 300×300, nothing cut)"),
      FIT("Fit", "The whole image, padded with transparency"),
      CROP("Crop center", "The middle square of the image");

      private final String displayName;
      private final String description;

      private SquareMode(String displayName, String description) {
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

   public static enum Targets implements Displayable {
      PLAYERS("Players", "Only when you hit players"),
      PLAYERS_AND_MOBS("Players + mobs", "Players and every other living target");

      private final String displayName;
      private final String description;

      private Targets(String displayName, String description) {
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
