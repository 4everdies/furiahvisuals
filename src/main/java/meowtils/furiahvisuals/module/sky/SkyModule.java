package meowtils.furiahvisuals.module.sky;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.compat.EntityViewRenderEvent;
import meowtils.furiahvisuals.compat.IRenderHandler;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.ambience.AmbienceModule;
import meowtils.furiahvisuals.module.targetaura.TargetAuraModule;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.GpuTimer;
import meowtils.furiahvisuals.render.OptiFineCompat;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.StringMapSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldProvider;

public final class SkyModule extends Module {
   private static final float HOVER_FADE_SECONDS = 0.6F;
   private static final long HOVER_DELAY_MS = 300L;
   private static final long HOVER_TIMEOUT_MS = 150L;
   public final SkyStyleSetting style;
   private final Map<SkyStyle, SkySettings> skies = new EnumMap<>(SkyStyle.class);
   public final BooleanSetting perServer;
   private final StringMapSetting serverStyles;
   public final BooleanSetting hoverPreview;
   private final Map<SkyStyle, Setting[]> options = new EnumMap<>(SkyStyle.class);
   private final Map<SkyStyle, float[]> optionValues = new EnumMap<>(SkyStyle.class);
   public final ModeSetting<SkyModule.Center> kaleidoCenter;
   public final BooleanSetting watcherInvert;
   private final SkyRenderer renderer = new SkyRenderer();
   private final SkyRenderer.Frame frame = new SkyRenderer.Frame();
   private final Meteors meteors = new Meteors();
   private final GpuTimer gpuTimer = new GpuTimer();
   private final SkyImage image = new SkyImage(new File(new File(Minecraft.getMinecraft().mcDataDir, "config"), "furiahsvisuals/sky.png"));
   private final IRenderHandler skyHandler = new IRenderHandler() {
      @Override
      public void render(float partialTicks, WorldClient world, Minecraft mc) {
         SkyModule.this.renderSky(partialTicks, world, mc);
      }
   };
   private final IRenderHandler noClouds = new IRenderHandler() {
      @Override
      public void render(float partialTicks, WorldClient world, Minecraft mc) {
      }
   };
   private WorldProvider skyProvider;
   private IRenderHandler previousSky;
   private WorldProvider cloudProvider;
   private IRenderHandler previousClouds;
   private boolean broken;
   private String status;
   private long movedFrame = -1L;
   private long lastGameNanos;
   private long lastMenuNanos;
   private final double[] skyTimes = new double[SkyStyle.values().length];
   private final double[] spinAngles = new double[SkyStyle.values().length];
   private SkyStyle shown;
   private SkyStyle fadingFrom;
   private float fade = 1.0F;
   private float fadeSeconds = 1.5F;
   private boolean shownByHover;
   private SkyStyle hoverCandidate;
   private long hoverSince;
   private long hoverLastCall;
   private long lastMenuFrame;
   private boolean measuring;
   private int measureRepeat = 1;
   private float flash;
   private String lastServer;
   private boolean applyingServerStyle;
   private float night = 1.0F;
   private float visibility = 1.0F;
   private float worldRain;
   private boolean inWorld;
   private double viewX;
   private double viewY;
   private double viewZ;
   private boolean forCards;
   private float bassAverage;
   private float beat;
   private float musicForTest = -1.0F;
   private long thumbsDrawnNanos;
   private final float[] cardViews = new float[SkyStyle.values().length * 2];
   private final float[] fogIn = new float[3];
   private final float[] fogOut = new float[3];
   SkyToolsWidget tools;
   private boolean pickStyle;
   private float pickCustom;
   private int pickA;
   private int pickB;
   private static final float[] NO_OPTIONS = new float[8];

   public SkyModule() {
      super("dynamic_skies", "Dynamic Skies", "Living, animated skies, from liquid marble to code rain.", true);
      Supplier<String> noMusic = new Supplier<String>() {
         public String get() {
            WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
            return wm == null ? "Needs the WaterMark's Audio module" : wm.musicUnavailableReason();
         }
      };
      this.style = this.add(
         new SkyStyleSetting(this)
            .<SkyStyleSetting>group("Sky")
            .keywords(new String[]{"animated sky", "sky", "skybox", "dynamic skies", "style", "background"})
      );
      this.add(
         (new WidgetSetting("skyTools", "This Sky's Settings", "Copy another sky's settings, give every sky this one's, or put this sky back to its defaults.") {
               @Override
               public Widget createWidget() {
                  return new SkyToolsWidget(SkyModule.this);
               }
            })
            .<WidgetSetting>group("Sky")
            .keywords(new String[]{"copy from", "apply to all skies", "reset this sky", "per sky"})
      );
      this.registerOptions();

      for (SkyStyle s : SkyStyle.values()) {
         SettingStyle own = this.owner(s);
         Setting[] opts = this.options.get(s);
         if (opts != null) {
            for (Setting x : opts) {
               this.add(x.<Setting>group(s.displayName()).ownedBy(own));
            }
         }

         SkySettings k = new SkySettings(s, own, noMusic);

         for (Setting x : k.common) {
            this.add(x);
         }

         if (opts != null) {
            k.setOwn(opts);
         }

         k.recordDefaults();
         this.skies.put(s, k);
      }

      this.kaleidoCenter = this.mode(SkyStyle.KALEIDOSCOPE, 5);
      this.watcherInvert = this.bool(SkyStyle.THE_WATCHER, 5);
      this.perServer = this.add(
         new BooleanSetting("perServer", "Remember Per Server", "Each server (and singleplayer) keeps the sky you picked there.", false).group("Switching")
      );
      this.serverStyles = this.add(new StringMapSetting("serverStyles"));
      this.hoverPreview = this.add(
         new BooleanSetting("hoverPreview", "Preview On Hover", "Hovering a card shows its sky on the real sky behind this menu.", true).group("Switching")
      );
      this.style.addListener(new Runnable() {
         @Override
         public void run() {
            SkyModule.this.rememberForServer();
            MenuMemory.markSeen(SkyModule.this.style.get().seenKey());
         }
      });
      this.perServer.addListener(new Runnable() {
         @Override
         public void run() {
            SkyModule.this.rememberForServer();
         }
      });
      this.frame.options = new SkyRenderer.Options() {
         @Override
         public float[] of(SkyStyle s) {
            return SkyModule.this.optionsOf(s);
         }
      };
      this.frame.prepare = new SkyRenderer.Prepare() {
         @Override
         public void apply(SkyStyle s, SkyRenderer.Frame f) {
            SkyModule.this.applySky(s, f);
         }
      };
   }

   @Override
   public String[] getLegacyIds() {
      return new String[]{"sky"};
   }

   @Override
   public String[] getLegacyNames() {
      return new String[]{"Animated Sky"};
   }

   public SkySettings of(SkyStyle s) {
      return this.skies.get(s);
   }

   public SkySettings current() {
      return this.skies.get(this.style.get());
   }

   boolean isModified(SkyStyle s) {
      return this.skies.get(s).isModified();
   }

   void copyInto(SkyStyle from, SkyStyle to) {
      if (from != to) {
         this.skies.get(to).copyFrom(this.skies.get(from));
      }
   }

   void applyToAll(SkyStyle from) {
      for (SkyStyle s : SkyStyle.values()) {
         this.copyInto(from, s);
      }
   }

   void resetSky(SkyStyle s) {
      this.skies.get(s).reset();
   }

   @Override
   public boolean afterLoad(JsonObject json) {
      MenuMemory.markSeen(this.style.get().seenKey());
      if (!SkySettings.hasSharedSettings(json, this.skies.get(SkyStyle.values()[0]))) {
         return false;
      } else {
         JsonObject when = json.has("whenPerSky") && json.get("whenPerSky").isJsonObject() ? json.getAsJsonObject("whenPerSky") : new JsonObject();

         for (SkyStyle s : SkyStyle.values()) {
            JsonElement w = when.get(s.name());
            this.skies.get(s).carryOver(json, w != null && w.isJsonPrimitive() ? w.getAsString() : null);
         }

         return true;
      }
   }

   private SettingStyle owner(final SkyStyle s) {
      return new SettingStyle() {
         @Override
         public String styleName() {
            return s.displayName();
         }

         @Override
         public boolean isShown() {
            return SkyModule.this.style.get() == s;
         }

         @Override
         public boolean isActive() {
            return SkyModule.this.style.get() == s;
         }

         @Override
         public boolean isMultiple() {
            return false;
         }

         @Override
         public void show() {
            SkyModule.this.style.set(s);
         }

         @Override
         public String kind() {
            return "sky";
         }

         @Override
         public String kinds() {
            return "skies";
         }

         @Override
         public String searchGroup(String group) {
            return group != null && !group.equals(s.displayName()) ? s.displayName() + " › " + group : s.displayName();
         }

         @Override
         public boolean foundElsewhereByNameOnly() {
            return true;
         }
      };
   }

   private static SliderSetting slider(String id, String name, String description, double min, double max, double def, double step, String unit) {
      SliderSetting x = new SliderSetting(id, name, description, min, max, def, step);
      if (unit != null) {
         x.unit(unit);
      }

      return x;
   }

   private void options(SkyStyle s, Setting... settings) {
      this.options.put(s, settings);
      this.optionValues.put(s, new float[8]);
   }

   private void registerOptions() {
      this.options(
         SkyStyle.CIRCUIT_MAZE,
         slider("circuit.density", "Trace Density", "How tightly packed the traces are.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("circuit.pulses", "Pulses", "Bright pulses running along the traces.", 0.0, 1.0, 0.6, 0.05, null),
         slider("circuit.flicker", "Flicker", "How much the traces flicker.", 0.0, 1.0, 0.5, 0.05, null)
      );
      final ModeSetting<SkyModule.Center> center = new ModeSetting<>(
         "kaleido.center", "Center", "Where the tunnel opens.", SkyModule.Center.class, SkyModule.Center.ZENITH
      );
      this.options(
         SkyStyle.KALEIDOSCOPE,
         slider("kaleido.petals", "Petals", "Points of each star.", 4.0, 12.0, 8.0, 1.0, null),
         slider("kaleido.spacing", "Ring Spacing", "Room between the nested rings.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("kaleido.zoom", "Zoom Speed", "How fast the rings flow outwards.", 0.0, 2.0, 1.0, 0.05, "x"),
         slider("kaleido.spin", "Rotation", "How fast the stars turn (below 0: the other way).", -1.0, 1.0, 0.2, 0.05, null),
         slider("kaleido.blur", "Outer Blur", "How soft the big outer rings get.", 0.0, 1.0, 0.6, 0.05, null),
         center,
         slider("kaleido.direction", "Direction", "Where on the horizon (0 north, 90 east).", 0.0, 359.0, 0.0, 1.0, "°").visibleWhen(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return center.get() == SkyModule.Center.HORIZON;
            }
         })
      );
      this.options(
         SkyStyle.THE_WATCHER,
         slider("watcher.direction", "Direction", "Where the eye is (0 north, 90 east).", 0.0, 359.0, 0.0, 1.0, "°"),
         slider("watcher.height", "Height", "How high above the horizon.", 5.0, 80.0, 28.0, 1.0, "°"),
         slider("watcher.size", "Size", "How big the eye is.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("watcher.blink", "Blink Rate", "How often it blinks.", 0.0, 1.0, 0.5, 0.05, null),
         new BooleanSetting("watcher.follow", "Follow Me", "The pupil follows where you look.", true),
         new BooleanSetting("watcher.invert", "Invert", "A black sky with white dots.", false),
         slider("watcher.dots", "Dot Size", "Size of the halftone dots.", 0.5, 2.0, 1.0, 0.05, "x")
      );
      this.options(
         SkyStyle.MEGASTRUCTURE,
         slider("mega.lights", "Light Density", "How many lights there are.", 0.0, 1.0, 0.6, 0.05, null),
         slider("mega.drift", "Drift Speed", "How fast you drift through it.", 0.0, 2.0, 1.0, 0.05, "x"),
         slider("mega.direction", "Drift Direction", "Which way you drift (0 north, 90 east).", 0.0, 359.0, 90.0, 1.0, "°"),
         slider("mega.dark", "Panel Darkness", "How dark the metal is.", 0.0, 1.0, 0.6, 0.05, null),
         slider("mega.twinkle", "Twinkle", "How much the lights twinkle.", 0.0, 1.0, 0.5, 0.05, null)
      );
      this.options(
         SkyStyle.CODE_RAIN,
         slider("rain.columns", "Column Density", "How many columns fall.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("rain.speed", "Fall Speed", "How fast they fall.", 0.2, 3.0, 1.0, 0.05, "x"),
         slider("rain.tail", "Tail Length", "How long each trail is.", 0.2, 2.0, 1.0, 0.05, "x"),
         slider("rain.glyph", "Glyph Size", "How big the characters are.", 0.5, 2.0, 1.0, 0.05, "x")
      );
      this.options(
         SkyStyle.PLASMA_VEINS,
         slider("plasma.density", "Vein Density", "How many veins there are.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("plasma.thickness", "Thickness", "How thick the veins are.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("plasma.flow", "Flow Speed", "How fast they move.", 0.0, 2.0, 1.0, 0.05, "x"),
         slider("plasma.sparks", "Sparks", "Sparks running along the veins.", 0.0, 1.0, 0.5, 0.05, null)
      );
      this.options(
         SkyStyle.NEON_FRACTURE,
         slider("fracture.density", "Crack Density", "How many cracks there are.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("fracture.flashes", "Flash Rate", "How often flashes run along the cracks.", 0.0, 1.0, 0.5, 0.05, null),
         slider("fracture.band", "Band Height", "Where the brightest band is, above the horizon.", 0.0, 60.0, 12.0, 1.0, "°"),
         slider("fracture.bandBright", "Band Brightness", "How bright that band is.", 0.0, 2.0, 1.0, 0.05, "x")
      );
      this.options(
         SkyStyle.VIOLET_STORM,
         slider("storm.turbulence", "Turbulence", "How wildly the clouds churn.", 0.0, 2.0, 1.0, 0.05, "x"),
         slider("storm.flow", "Flow Speed", "How fast they move.", 0.0, 2.0, 1.0, 0.05, "x"),
         slider("storm.direction", "Flow Direction", "Which way they flow (0 north, 90 east).", 0.0, 359.0, 90.0, 1.0, "°"),
         slider("storm.contrast", "Cloud Contrast", "Darker streaks and brighter hearts.", 0.0, 2.0, 1.0, 0.05, "x")
      );
      this.options(
         SkyStyle.SKY_BEAMS,
         slider("beams.count", "Beams", "How many searchlights.", 2.0, 12.0, 8.0, 1.0, null),
         slider("beams.sway", "Sway Speed", "How fast they sweep.", 0.0, 2.0, 1.0, 0.05, "x"),
         slider("beams.width", "Width", "How wide each beam is.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("beams.height", "Height", "How high they reach.", 0.2, 1.0, 0.7, 0.05, null),
         slider("beams.haze", "Haze", "The glow along the horizon.", 0.0, 1.0, 0.6, 0.05, null)
      );
      this.options(
         SkyStyle.AMETHYST_CELLS,
         slider("cells.size", "Cell Size", "How big the cells are.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("cells.border", "Border Width", "How thick the walls are.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("cells.glow", "Border Glow", "How brightly the walls glow.", 0.0, 2.0, 1.0, 0.05, "x"),
         slider("cells.drift", "Drift Speed", "How fast the cells shift.", 0.0, 2.0, 1.0, 0.05, "x"),
         slider("cells.ripple", "Ripple", "Light rippling across the walls.", 0.0, 1.0, 0.5, 0.05, null)
      );
      final ModeSetting<SkyModule.RiftPlacement> placement = new ModeSetting<>(
         "rift.placement", "Placement", "Where the rift hangs.", SkyModule.RiftPlacement.class, SkyModule.RiftPlacement.NORTH
      );
      this.options(
         SkyStyle.VOID_RIFT,
         slider("rift.size", "Rift Size", "How big the rift is.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("rift.energy", "Energy", "The violet energy boiling round it.", 0.0, 1.0, 0.7, 0.05, null),
         slider("rift.streaks", "Streaks", "Arcs of light curving round it and drifting outward.", 0.0, 1.0, 0.6, 0.05, null),
         slider("rift.sparks", "Sparks", "Sparks flying off its edge.", 0.0, 1.0, 0.5, 0.05, null),
         slider("rift.breathing", "Breathing", "How much it slowly swells and shrinks.", 0.0, 1.0, 0.5, 0.05, null),
         placement,
         slider("rift.direction", "Direction", "Which way it hangs (0 north, 90 east).", 0.0, 359.0, 0.0, 1.0, "°").visibleWhen(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return placement.get() == SkyModule.RiftPlacement.DIRECTION;
            }
         })
      );
      this.options(
         SkyStyle.FROZEN_TIDE,
         slider("tide.size", "Wave Size", "How broad the wave is.", 0.5, 2.0, 1.0, 0.05, "x"),
         slider("tide.speed", "Wave Speed", "How fast it rolls along its length.", 0.0, 2.0, 1.0, 0.05, "x"),
         slider("tide.spires", "Spires", "Needles of ice round the horizon.", 0.0, 24.0, 12.0, 1.0, null),
         slider("tide.height", "Spire Height", "How tall the needles are.", 0.5, 1.5, 1.0, 0.05, "x"),
         slider("tide.sparkles", "Sparkles", "Sparkles drifting off the needles' tips.", 0.0, 1.0, 0.6, 0.05, null),
         new BooleanSetting("tide.stars", "Starfield", "Faint stars above the wave.", true)
      );
      final BooleanSetting greatBlade = new BooleanSetting("firefall.greatBlade", "Great Blade", "One colossal burning blade towering into the smoke.", true);
      this.options(
         SkyStyle.FIREFALL,
         slider("firefall.rate", "Streak Rate", "How often streaks of fire fall.", 0.0, 1.0, 0.5, 0.05, null),
         slider("firefall.angle", "Fall Angle", "How slanted they fall (0: straight down).", 0.0, 60.0, 25.0, 1.0, "°"),
         slider("firefall.blades", "Horizon Blades", "Blades of fire standing along the horizon.", 0.0, 16.0, 8.0, 1.0, null),
         greatBlade,
         slider("firefall.direction", "Great Blade Direction", "Where it stands (0 north, 90 east).", 0.0, 359.0, 20.0, 1.0, "°")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return greatBlade.get();
               }
            }),
         slider("firefall.embers", "Embers", "Embers drifting upward.", 0.0, 1.0, 0.6, 0.05, null),
         slider("firefall.smoke", "Smoke", "How thick the smoke is.", 0.0, 1.0, 0.8, 0.05, null)
      );
      this.options(
         SkyStyle.GILDED_STORM,
         slider("gilded.cover", "Cloud Cover", "How much of the sky the storm clouds cover.", 0.0, 1.0, 0.6, 0.05, null),
         slider("gilded.rays", "Ray Strength", "How bright the rays of light through the gaps are.", 0.0, 1.0, 0.7, 0.05, null),
         new BooleanSetting("gilded.sigil", "Sigil", "A radiant golden star in a ring of light, standing on the horizon.", true),
         slider("gilded.direction", "Direction", "Where the sigil stands and the light comes from (0 north, 90 east).", 0.0, 359.0, 60.0, 1.0, "°"),
         slider("gilded.motes", "Motes", "Golden motes drifting in the light.", 0.0, 1.0, 0.5, 0.05, null),
         slider("gilded.haze", "Haze", "The warm golden haze along the horizon.", 0.0, 1.0, 0.6, 0.05, null)
      );
   }

   private ModeSetting<SkyModule.Center> mode(SkyStyle s, int slot) {
      return (ModeSetting<SkyModule.Center>)((Setting[])this.options.get(s))[slot];
   }

   private BooleanSetting bool(SkyStyle s, int slot) {
      return (BooleanSetting)this.options.get(s)[slot];
   }

   float[] optionsOf(SkyStyle s) {
      float[] out = this.optionValues.get(s);
      Setting[] settings = this.options.get(s);
      if (out == null) {
         return NO_OPTIONS;
      } else {
         for (int i = 0; i < settings.length && i < 8; i++) {
            Setting x = settings[i];
            if (x instanceof SliderSetting) {
               out[i] = ((SliderSetting)x).getFloat();
            } else if (x instanceof BooleanSetting) {
               out[i] = ((BooleanSetting)x).get() ? 1.0F : 0.0F;
            } else if (x instanceof ModeSetting) {
               out[i] = ((ModeSetting)x).getIndex();
            }
         }

         return out;
      }
   }

   private static float night(WorldClient world, float partialTicks) {
      if (world != null && world.provider.isSurfaceWorld()) {
         double sun = Math.cos(world.getCelestialAngle(partialTicks) * Math.PI * 2.0);
         double k = (sun - -0.06) / 0.28;
         k = Math.max(0.0, Math.min(1.0, k));
         return (float)(1.0 - k * k * (3.0 - 2.0 * k));
      } else {
         return 1.0F;
      }
   }

   private float visibilityOf(SkyStyle s) {
      return this.inWorld && this.skies.get(s).show.get() == SkyModule.Show.NIGHT ? this.night : 1.0F;
   }

   private float alphaOf(SkyStyle s) {
      SkySettings k = this.skies.get(s);
      return (k.blend.get() == SkyModule.Blend.OVERLAY ? k.opacity.getFloat() : 1.0F) * this.visibilityOf(s);
   }

   private float brightnessOf(SkyStyle s) {
      SkySettings k = this.skies.get(s);
      if (!this.forCards && k.show.get() == SkyModule.Show.ALWAYS) {
         float day = k.dayBrightness.getFloat();
         return k.brightness.getFloat() * (day + (1.0F - day) * this.night);
      } else {
         return k.brightness.getFloat();
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getMinecraft();
      WorldClient world = mc.theWorld;
      WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
      if (wm != null) {
         wm.wantLevelsForSky(this.skies.get(this.activeStyle()).reactMusic.get());
      }

      if (world != null) {
         this.applyServerStyle();
         WorldProvider provider = world.provider;
         boolean draw = this.shouldDraw(world);
         if (draw) {
            if (IRenderHandler.getSkyRenderer(provider) != this.skyHandler) {
               this.previousSky = IRenderHandler.getSkyRenderer(provider);
               IRenderHandler.setSkyRenderer(provider, this.skyHandler);
               this.skyProvider = provider;
            }
         } else {
            this.uninstallSky();
         }

         SkyModule.Clouds c = this.skies.get(this.activeStyle()).clouds.get();
         boolean hide = draw && (c == SkyModule.Clouds.HIDDEN || c == SkyModule.Clouds.STYLE) && this.visibility > 0.5F;
         if (hide) {
            if (IRenderHandler.getCloudRenderer(provider) != this.noClouds) {
               this.previousClouds = IRenderHandler.getCloudRenderer(provider);
               IRenderHandler.setCloudRenderer(provider, this.noClouds);
               this.cloudProvider = provider;
            }
         } else {
            this.uninstallClouds();
         }
      }
   }

   private boolean shouldDraw(WorldClient world) {
      if (this.broken) {
         this.status = "Switched off after a graphics error";
         return false;
      } else {
         SkyStyle s = this.activeStyle();
         SkySettings k = this.skies.get(s);
         int dim = world.provider.getDimensionId();
         boolean here = dim == -1 ? k.nether.get() : (dim == 1 ? k.end.get() : k.overworld.get());
         if (!here) {
            this.status = "Off in this dimension";
            return false;
         } else if (!this.renderer.shaders().supported()) {
            this.status = "No OpenGL 2.0 shaders here";
            return false;
         } else if (s.isDynamic() && OptiFineCompat.shadersActive()) {
            this.status = "Skies with their own options need the shader pack off";
            return false;
         } else if (k.offWithShaders.get() && OptiFineCompat.shadersActive()) {
            this.status = "Off while a shader pack is on";
            return false;
         } else if (k.optifineSkies.get() == SkyModule.OptiFineSkies.STEP_ASIDE && OptiFineCompat.customSkyActive(world)) {
            this.status = "Showing the resource pack's sky";
            return false;
         } else if (!this.renderer.shaders().isAvailable(s)) {
            this.status = "This sky could not load";
            return false;
         } else {
            this.status = null;
            return true;
         }
      }
   }

   private void uninstallSky() {
      if (this.skyProvider != null && IRenderHandler.getSkyRenderer(this.skyProvider) == this.skyHandler) {
         IRenderHandler.setSkyRenderer(this.skyProvider, this.previousSky);
      }

      this.skyProvider = null;
      this.previousSky = null;
   }

   private void uninstallClouds() {
      if (this.cloudProvider != null && IRenderHandler.getCloudRenderer(this.cloudProvider) == this.noClouds) {
         IRenderHandler.setCloudRenderer(this.cloudProvider, this.previousClouds);
      }

      this.cloudProvider = null;
      this.previousClouds = null;
   }

   private boolean isDrawing() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc.theWorld != null && IRenderHandler.getSkyRenderer(mc.theWorld.provider) == this.skyHandler;
   }

   @Override
   public void onWorldChange() {
      this.skyProvider = null;
      this.previousSky = null;
      this.cloudProvider = null;
      this.previousClouds = null;
      this.meteors.clear();
      this.flash = 0.0F;
   }

   @Override
   protected void onDisable() {
      this.uninstallSky();
      this.uninstallClouds();
      this.meteors.clear();
      WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
      if (wm != null) {
         wm.wantLevelsForSky(false);
      }
   }

   private SkyStyle activeStyle() {
      long now = ClientClock.millis();
      return !this.hoverPreview.get()
            || this.hoverCandidate == null
            || now - this.hoverLastCall >= 150L
            || now - this.hoverSince < 300L
            || !this.renderer.shaders().isAvailable(this.hoverCandidate)
            || this.hoverCandidate.isDynamic() && OptiFineCompat.shadersActive()
         ? this.style.get()
         : this.hoverCandidate;
   }

   private void prepareFrame(float partialTicks, WorldClient world, boolean menu) {
      long gameNow = GameClock.nanos();
      long menuNow = ClientClock.nanos();
      float dt = 0.0F;
      long frameNumber = GameClock.frame();
      if (frameNumber != this.movedFrame) {
         this.movedFrame = frameNumber;
         long now = menu ? menuNow : gameNow;
         long last = menu ? this.lastMenuNanos : this.lastGameNanos;
         dt = last == 0L ? 0.0F : Math.min(0.1F, (float)(now - last) / 1.0E9F);
      }

      this.lastGameNanos = gameNow;
      this.lastMenuNanos = menuNow;
      SkyStyle target = this.activeStyle();
      if (this.shown != target) {
         boolean hovering = target != this.style.get() || this.shownByHover;
         this.fadingFrom = this.shown;
         this.shown = target;
         this.shownByHover = target != this.style.get();
         this.fadeSeconds = hovering ? 0.6F : this.skies.get(target).fadeIn.getFloat();
         float f = this.fade = this.fadingFrom != null && !(this.fadeSeconds <= 0.0F) ? 0.0F : 1.0F;
         if (this.fade >= 1.0F) {
            this.fadingFrom = null;
         }
      }

      if (this.fade < 1.0F) {
         this.fade = Math.min(1.0F, this.fade + dt / this.fadeSeconds);
         if (this.fade >= 1.0F) {
            this.fadingFrom = null;
         }
      }

      SkySettings on = this.skies.get(this.shown);
      boolean react = on.reactMusic.get() || this.fadingFrom != null && this.skies.get(this.fadingFrom).reactMusic.get();
      float bass = this.musicForTest >= 0.0F ? this.musicForTest : -1.0F;
      if (bass < 0.0F && react) {
         WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
         bass = wm == null ? -1.0F : wm.musicBass();
      }

      if (dt > 0.0F) {
         if (react && bass >= 0.0F) {
            this.bassAverage = this.bassAverage + (bass - this.bassAverage) * (1.0F - (float)Math.exp(-dt / 0.6F));
            float hit = Math.max(0.0F, Math.min(1.0F, (bass - this.bassAverage * 1.08F) * 3.5F + bass * 0.25F));
            this.beat = Math.max(this.beat * (float)Math.exp(-dt / 0.18F), hit);
         } else {
            this.beat = this.beat * (float)Math.exp(-dt / 0.18F);
         }
      }

      SkyStyle[] all = SkyStyle.values();

      for (int i = 0; i < all.length; i++) {
         SkySettings k = this.skies.get(all[i]);
         float pulse = this.pulseOf(k);
         this.skyTimes[i] = this.skyTimes[i] + dt * k.speed.get() * (1.0 + pulse * 1.5);
         this.spinAngles[i] = (this.spinAngles[i] + dt * Math.toRadians(k.rotation.get())) % (Math.PI * 2);
      }

      this.inWorld = world != null;
      this.night = night(world, partialTicks);
      this.visibility = this.visibilityOf(this.shown);
      this.meteors.update(dt, on.shootingStars.getFloat());
      this.frame.meteorHeads = this.meteors.heads();
      this.frame.meteorTails = this.meteors.tails();
      boolean imageShown = this.shown == SkyStyle.CUSTOM_IMAGE || this.fadingFrom == SkyStyle.CUSTOM_IMAGE;
      this.frame.image = imageShown ? this.image.texture() : -1;
      Minecraft mc = Minecraft.getMinecraft();
      Entity viewer = mc.getRenderViewEntity();
      if (viewer != null) {
         this.viewX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
         this.viewY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
         this.viewZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;
         Vec3 look = viewer.getLook(partialTicks);
         this.frame.lookX = (float)look.xCoord;
         this.frame.lookY = (float)look.yCoord;
         this.frame.lookZ = (float)look.zCoord;
      }

      if (world != null) {
         this.worldRain = world.getRainStrength(partialTicks);
         int bolt = world.getLastLightningBolt();
         if (bolt > 0) {
            this.flash = Math.max(this.flash, Math.min(1.0F, bolt - partialTicks) * 0.55F);
            this.aimFlash(world, viewer);
         }
      } else {
         this.worldRain = 0.0F;
      }

      this.flash = Math.max(0.0F, this.flash - dt * 3.0F);
   }

   private float pulseOf(SkySettings k) {
      return k.reactMusic.get() ? this.beat * k.musicAmount.getFloat() : 0.0F;
   }

   private void applySky(SkyStyle s, SkyRenderer.Frame f) {
      SkySettings k = this.skies.get(s);
      int i = s.ordinal();
      f.time = (float)(this.skyTimes[i] % 100000.0);
      f.detail = k.quality.get().detail;
      f.scale = k.scale.getFloat();
      this.pickColors(k, s);
      f.useStyleColors = this.pickStyle;
      f.custom = this.pickCustom;
      f.colA = this.pickA;
      f.colB = this.pickB;
      f.brightness = this.brightnessOf(s);
      f.saturation = k.saturation.getFloat();
      f.contrast = k.contrast.getFloat();
      f.beat = this.pulseOf(k);
      f.spin = (float)this.spinAngles[i];
      f.clouds = k.clouds.get() == SkyModule.Clouds.STYLE ? 1.0F : 0.0F;
      f.sunOwn = k.sunMoon.get() == SkyModule.Element.STYLE ? 1.0F : 0.0F;
      f.stars = k.stars.get() ? k.starDensity.getFloat() : 0.0F;
      f.twinkle = k.twinkle.getFloat();
      f.starColor = k.starColor.getColor(0.0F);
      double p = 0.01 * k.parallax.get();
      f.parallaxX = (float)(this.viewX * p);
      f.parallaxY = (float)(this.viewY * p);
      f.parallaxZ = (float)(this.viewZ * p);
      boolean weather = k.weather.get() && !this.forCards;
      f.rain = weather ? this.worldRain : 0.0F;
      f.flash = weather ? this.flash : 0.0F;
   }

   private void aimFlash(WorldClient world, Entity viewer) {
      if (viewer != null) {
         for (int i = world.weatherEffects.size() - 1; i >= 0; i--) {
            Entity e = (Entity)world.weatherEffects.get(i);
            if (e instanceof EntityLightningBolt) {
               double dx = e.posX - viewer.posX;
               double dy = e.posY + 60.0 - viewer.posY;
               double dz = e.posZ - viewer.posZ;
               double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
               if (len > 0.001) {
                  this.frame.flashX = (float)(dx / len);
                  this.frame.flashY = (float)(dy / len);
                  this.frame.flashZ = (float)(dz / len);
               }

               return;
            }
         }
      }
   }

   private void pickColors(SkySettings k, SkyStyle s) {
      this.pickStyle = false;
      if (k.colorSource.get() == SkyModule.ColorSource.CUSTOM) {
         this.fromSetting(k.color);
      } else if (k.colorSource.get() == SkyModule.ColorSource.GLOBAL && ColorSetting.getGlobal() != null) {
         this.fromSetting(ColorSetting.getGlobal());
      } else {
         Module m;
         if (k.colorSource.get() == SkyModule.ColorSource.AURA && (m = FuriahsVisuals.instance.getModules().get("target_aura")) instanceof TargetAuraModule) {
            this.fromSetting(((TargetAuraModule)m).getSelectedEffect().color);
         } else {
            this.pickStyle = true;
            this.pickCustom = 0.0F;
            this.pickA = s.colorA;
            this.pickB = s.colorB;
         }
      }
   }

   private void fromSetting(ColorSetting c) {
      this.pickCustom = 1.0F;
      this.pickA = c.getColor(0.0F) | 0xFF000000;
      this.pickB = c.getEffectiveMode() == ColorSetting.Mode.STATIC ? ColorUtil.shade(this.pickA, 0.5F) : c.getColor(1.0F) | 0xFF000000;
   }

   private void renderSky(float partialTicks, WorldClient world, Minecraft mc) {
      if (this.broken) {
         this.drawOriginalSky(partialTicks, world, mc);
      } else {
         this.prepareFrame(partialTicks, world, false);
         float alphaTo = this.alphaOf(this.shown);
         float a2 = 0.0F;
         float a3 = alphaTo;
         if (this.fadingFrom != null) {
            float alphaFrom = this.alphaOf(this.fadingFrom);
            a3 = this.fade * alphaTo;
            a2 = a3 >= 0.9999F ? 0.0F : Math.min(1.0F, (1.0F - this.fade) * alphaFrom / (1.0F - a3));
         }

         float total;
         if ((total = a2 * (1.0F - a3) + a3) <= 0.002F) {
            this.drawOriginalSky(partialTicks, world, mc);
         } else {
            boolean overlay = total < 0.998F;
            if (overlay) {
               this.drawOriginalSky(partialTicks, world, mc);
            }

            boolean drawn = false;

            try {
               drawn = this.drawSky(mc, world, partialTicks, a2, a3, total);
            } catch (RuntimeException | LinkageError var12) {
               this.fail(var12);
            }

            if (!drawn && !overlay) {
               this.drawOriginalSky(partialTicks, world, mc);
            }
         }
      }
   }

   private boolean drawSky(Minecraft mc, WorldClient world, float partialTicks, float alphaFrom, float alphaTo, float total) {
      boolean fading = this.fadingFrom != null && alphaFrom > 0.001F;
      boolean measure = this.measuring || System.currentTimeMillis() - this.lastMenuFrame < 500L;
      if (measure) {
         this.gpuTimer.begin();
      }

      boolean drawn = false;
      this.renderer.begin(total < 1.0F || fading);

      try {
         float res = this.resolution(this.shown, fading ? this.fadingFrom : null);
         if (res < 1.0F) {
            drawn = this.renderer.drawLowRes(fading ? this.fadingFrom : null, this.shown, fading ? alphaTo / total : 1.0F, this.frame, total, res);

            for (int i = 1; i < this.measureRepeat && this.measuring && drawn && !fading && total >= 1.0F; i++) {
               this.renderer.drawLowRes(null, this.shown, 1.0F, this.frame, total, res);
            }
         }

         if (!drawn) {
            if (fading) {
               SkyRenderer.blend(alphaFrom < 1.0F);
               drawn = this.renderer.draw(this.fadingFrom, this.frame, alphaFrom);
               SkyRenderer.blend(true);
               drawn |= this.renderer.draw(this.shown, this.frame, alphaTo);
            } else {
               SkyRenderer.blend(alphaTo < 1.0F);
               drawn = this.renderer.draw(this.shown, this.frame, alphaTo);

               for (int i = 1; i < this.measureRepeat && this.measuring && drawn && alphaTo >= 1.0F; i++) {
                  this.renderer.draw(this.shown, this.frame, alphaTo);
               }
            }
         }

         if (drawn && this.skies.get(this.shown).sunMoon.get() == SkyModule.Element.VANILLA) {
            this.renderer.restoreProgram();
            this.renderer.drawVanillaSunMoon(mc, world, partialTicks);
         }
      } finally {
         this.renderer.end();
         if (measure) {
            this.gpuTimer.end();
         }
      }

      return drawn;
   }

   private float resolution(SkyStyle a, SkyStyle b) {
      return Math.max(this.resolutionOf(a), b == null ? 0.0F : this.resolutionOf(b));
   }

   private float resolutionOf(SkyStyle s) {
      SkyModule.Quality q = this.skies.get(s).quality.get();
      return s.kind == SkyStyle.Kind.SOFT && q != SkyModule.Quality.ULTRA ? Math.min(q.resolution, 0.5F) : q.resolution;
   }

   private void drawOriginalSky(float partialTicks, WorldClient world, Minecraft mc) {
      WorldProvider provider = world.provider;
      IRenderHandler ours = IRenderHandler.getSkyRenderer(provider);
      IRenderHandler.setSkyRenderer(provider, this.previousSky);

      try {
         mc.renderGlobal.renderSky(partialTicks, 2);
      } finally {
         IRenderHandler.setSkyRenderer(provider, ours);
      }
   }

   private void fail(Throwable e) {
      if (!this.broken) {
         this.broken = true;
         FuriahsVisuals.LOG.error("Dynamic Skies failed while drawing and is switched off for this session", e);
      }
   }

   private int horizonNow() {
      this.pickColors(this.skies.get(this.shown), this.shown);
      int a = this.pickStyle ? this.shown.colorA : this.pickA;
      int b = this.pickStyle ? this.shown.colorB : this.pickB;
      if (this.shown == SkyStyle.THE_WATCHER && this.watcherInvert.get()) {
         int t = a;
         a = b;
         b = t;
      }

      return this.shown == SkyStyle.CUSTOM_IMAGE ? this.image.horizonColor() : this.shown.horizonColor(a, b, this.pickCustom > 0.5F);
   }

   @Override
   public void onFogColors(EntityViewRenderEvent.FogColors event) {
      this.fogIn[0] = event.red;
      this.fogIn[1] = event.green;
      this.fogIn[2] = event.blue;
      this.matchFog(event);
      this.fogOut[0] = event.red;
      this.fogOut[1] = event.green;
      this.fogOut[2] = event.blue;
   }

   private void matchFog(EntityViewRenderEvent.FogColors event) {
      if (this.isDrawing() && this.shown != null) {
         SkySettings k = this.skies.get(this.shown);
         if (k.matchFog.get() && !(k.fogBlend.get() <= 0.0)) {
            AmbienceModule ambience = FuriahsVisuals.instance.getAmbience();
            if (ambience == null || !ambience.tintsFog()) {
               Material m;
               if (event.block == null || (m = event.block.getMaterial()) != Material.water && m != Material.lava) {
                  int horizon = this.horizonNow();
                  float kf = k.fogBlend.getFloat() * (k.blend.get() == SkyModule.Blend.OVERLAY ? k.opacity.getFloat() : 1.0F) * this.visibility;
                  float bright = this.brightnessOf(this.shown);
                  event.red = event.red + (ColorUtil.red(horizon) / 255.0F * bright - event.red) * kf;
                  event.green = event.green + (ColorUtil.green(horizon) / 255.0F * bright - event.green) * kf;
                  event.blue = event.blue + (ColorUtil.blue(horizon) / 255.0F * bright - event.blue) * kf;
               }
            }
         }
      }
   }

   public float reflectionColors(float[] out) {
      if (this.isEnabled() && this.shown != null && this.isDrawing()) {
         this.pickColors(this.skies.get(this.shown), this.shown);
         int a = this.pickStyle ? this.shown.colorA : this.pickA;
         int horizon = this.horizonNow();
         float bright = this.brightnessOf(this.shown);
         out[0] = ColorUtil.red(a) / 255.0F * bright;
         out[1] = ColorUtil.green(a) / 255.0F * bright;
         out[2] = ColorUtil.blue(a) / 255.0F * bright;
         out[3] = ColorUtil.red(horizon) / 255.0F * bright;
         out[4] = ColorUtil.green(horizon) / 255.0F * bright;
         out[5] = ColorUtil.blue(horizon) / 255.0F * bright;
         SkySettings k = this.skies.get(this.shown);
         return this.visibility * (k.blend.get() == SkyModule.Blend.OVERLAY ? k.opacity.getFloat() : 1.0F);
      } else {
         return 0.0F;
      }
   }

   public Vec3 tintClouds(Vec3 c) {
      if (this.isEnabled() && this.shown != null && this.skies.get(this.shown).clouds.get() == SkyModule.Clouds.TINTED && this.isDrawing()) {
         int horizon = this.horizonNow();
         float bright = Math.max(0.35F, this.brightnessOf(this.shown));
         float k = 0.65F * this.visibility;
         double r = ColorUtil.red(horizon) / 255.0 * bright;
         double g = ColorUtil.green(horizon) / 255.0 * bright;
         double b = ColorUtil.blue(horizon) / 255.0 * bright;
         r = r * 0.6 + 0.4 * Math.max(r, 0.5);
         g = g * 0.6 + 0.4 * Math.max(g, 0.5);
         b = b * 0.6 + 0.4 * Math.max(b, 0.5);
         return new Vec3(c.xCoord + (r - c.xCoord) * k, c.yCoord + (g - c.yCoord) * k, c.zCoord + (b - c.zCoord) * k);
      } else {
         return c;
      }
   }

   private static String serverKey() {
      Minecraft mc = Minecraft.getMinecraft();
      ServerData data = mc.getCurrentServerData();
      if (data != null && data.serverIP != null && !data.serverIP.isEmpty()) {
         return data.serverIP.toLowerCase(Locale.ROOT);
      } else {
         return mc.isSingleplayer() ? "singleplayer" : null;
      }
   }

   private void rememberForServer() {
      if (this.perServer.get() && !this.applyingServerStyle) {
         String key = serverKey();
         if (key != null && Minecraft.getMinecraft().theWorld != null) {
            this.serverStyles.put(key, this.style.get().name());
         }
      }
   }

   private void applyServerStyle() {
      String key = serverKey();
      if (key != null && !key.equals(this.lastServer)) {
         this.lastServer = key;
         if (this.perServer.get()) {
            String saved = this.serverStyles.get(key);
            if (saved == null) {
               this.rememberForServer();
            } else {
               for (SkyStyle s : SkyStyle.values()) {
                  if (s.name().equals(saved)) {
                     this.applyingServerStyle = true;

                     try {
                        this.style.set(s);
                     } finally {
                        this.applyingServerStyle = false;
                     }

                     return;
                  }
               }
            }
         }
      }
   }

   @Override
   public Module.Notice getNotice() {
      if (!this.renderer.shaders().supported()) {
         return new Module.Notice("This graphics driver has no OpenGL 2.0 shaders, which the skies are drawn with, so Minecraft's own sky stays.", true);
      } else if (OptiFineCompat.shadersActive()) {
         return this.style.get().isDynamic()
            ? new Module.Notice(
               "The skies with their own options need OptiFine's shader pack off (it draws its own sky); the classic skies still work. Turn the pack off, or pick a classic sky.",
               false
            )
            : new Module.Notice(
               "An OptiFine shader pack is on: the skies with their own options are unavailable until it is off"
                  + (this.current().offWithShaders.get() ? ", and this sky steps aside for the pack's own (Off With Shader Packs)." : "."),
               false
            );
      } else {
         return null;
      }
   }

   boolean isUsable(SkyStyle s) {
      return this.unusableReason(s) == null;
   }

   String unusableReason(SkyStyle s) {
      if (this.broken) {
         return "Dynamic Skies was switched off after a graphics error";
      } else if (!this.renderer.shaders().supported()) {
         return "Needs OpenGL 2.0 shaders";
      } else {
         return s.isDynamic() && OptiFineCompat.shadersActive() ? "Needs OptiFine's shader pack off" : null;
      }
   }

   int drawCardViews(int w, int h) {
      this.lastMenuFrame = System.currentTimeMillis();
      if (this.broken) {
         return -1;
      } else {
         long now = ClientClock.nanos();
         int tex = this.renderer.thumbTexture();
         if (tex >= 0 && now - this.thumbsDrawnNanos < 33000000L) {
            return tex;
         } else {
            this.thumbsDrawnNanos = now;

            try {
               this.prepareFrame(1.0F, Minecraft.getMinecraft().theWorld, true);
               SkyStyle[] all = SkyStyle.values();

               for (int i = 0; i < all.length; i++) {
                  this.cardViews[i * 2] = all[i].previewYaw;
                  this.cardViews[i * 2 + 1] = all[i].previewPitch;
               }

               if (this.kaleidoCenter.get() == SkyModule.Center.HORIZON) {
                  this.cardViews[SkyStyle.KALEIDOSCOPE.ordinal() * 2] = this.optionsOf(SkyStyle.KALEIDOSCOPE)[6];
                  this.cardViews[SkyStyle.KALEIDOSCOPE.ordinal() * 2 + 1] = 12.0F;
               }

               float[] wt = this.optionsOf(SkyStyle.THE_WATCHER);
               this.cardViews[SkyStyle.THE_WATCHER.ordinal() * 2] = wt[0];
               this.cardViews[SkyStyle.THE_WATCHER.ordinal() * 2 + 1] = Math.max(8.0F, wt[1] - 2.0F);
               float[] vr = this.optionsOf(SkyStyle.VOID_RIFT);
               int rift = SkyStyle.VOID_RIFT.ordinal() * 2;
               this.cardViews[rift] = vr[5] > 1.5F ? vr[6] : 0.0F;
               this.cardViews[rift + 1] = vr[5] > 0.5F && vr[5] < 1.5F ? 75.0F : 40.0F;
               float[] ff = this.optionsOf(SkyStyle.FIREFALL);
               if (ff[3] > 0.5F) {
                  this.cardViews[SkyStyle.FIREFALL.ordinal() * 2] = ff[4];
               }

               this.cardViews[SkyStyle.GILDED_STORM.ordinal() * 2] = this.optionsOf(SkyStyle.GILDED_STORM)[3];
               this.forCards = true;
               if (!this.renderer.drawThumbnails(all, this.cardViews, w, h, this.frame)) {
                  return -1;
               }
            } catch (RuntimeException | LinkageError var16) {
               this.fail(var16);
               return -1;
            } finally {
               this.forCards = false;
            }

            return this.renderer.thumbTexture();
         }
      }
   }

   float[] cardViewUv(SkyStyle s) {
      return this.renderer.thumbUv(s.ordinal());
   }

   @Override
   public int getAccentColor(double time) {
      SkySettings k = this.skies.get(this.activeStyle());
      switch ((SkyModule.ColorSource)k.colorSource.get()) {
         case CUSTOM:
            return k.color.getDisplayColor();
         case GLOBAL:
            return ColorSetting.getGlobal() != null ? ColorSetting.getGlobal().getDisplayColor() : this.activeStyle().colorA;
         case AURA:
            Module m = FuriahsVisuals.instance.getModules().get("target_aura");
            return m != null ? m.getAccentColor(time) : this.activeStyle().colorA;
         default:
            SkyStyle s = this.activeStyle();
            float lum = ColorUtil.luminance(s.colorA);
            return !(lum > 0.85F) && !(lum < 0.08F) ? s.colorA : s.colorB;
      }
   }

   @Override
   public int getOptionColor(Setting setting, Enum<?> option, double time) {
      return setting == this.style && option instanceof SkyStyle ? ((SkyStyle)option).colorA : 0;
   }

   @Override
   public void onOptionHover(Setting setting, Enum<?> option) {
      if (setting == this.style) {
         this.lastMenuFrame = System.currentTimeMillis();
         long now = ClientClock.millis();
         SkyStyle s = option instanceof SkyStyle ? (SkyStyle)option : null;
         if (s != this.hoverCandidate) {
            this.hoverCandidate = s;
            this.hoverSince = now;
         }

         if (s != null) {
            this.hoverLastCall = now;
         }
      }
   }

   @Override
   public boolean hasPreview() {
      return true;
   }

   @Override
   public String getPreviewLabel() {
      return this.activeStyle().displayName();
   }

   @Override
   public String getPreviewInfo() {
      if (!this.isEnabled()) {
         return "Off";
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.theWorld != null && this.status != null) {
            return this.status;
         } else {
            SkyModule.Quality q = this.skies.get(this.activeStyle()).quality.get();
            float ms = this.gpuTimer.millis();
            return ms > 0.0F && mc.theWorld != null ? String.format(Locale.ROOT, "%.2f ms GPU  ·  %s", ms, q.displayName()) : q.displayName() + " quality";
         }
      }
   }

   @Override
   public void renderPreviewBackdrop(float dt, float yaw, float pitch) {
      this.lastMenuFrame = System.currentTimeMillis();
      if (!this.broken) {
         try {
            this.prepareFrame(1.0F, Minecraft.getMinecraft().theWorld, true);
            float y = (float)Math.toRadians(yaw);
            float p = (float)Math.toRadians(22.0F - pitch);
            this.frame.lookX = (float)(Math.sin(y) * Math.cos(p));
            this.frame.lookY = (float)Math.sin(p);
            this.frame.lookZ = (float)(-Math.cos(y) * Math.cos(p));
            this.renderer.drawPreview(this.shown, this.frame, yaw, pitch);
         } catch (RuntimeException | LinkageError var6) {
            this.fail(var6);
         }
      }
   }

   public void setMeasuring(boolean on) {
      this.measuring = on;
      if (on) {
         this.gpuTimer.reset();
      }
   }

   public float getGpuMillis() {
      return this.gpuTimer.millis() / (this.measuring ? this.measureRepeat : 1);
   }

   public float getGpuLeastMillis() {
      return this.gpuTimer.least() / (this.measuring ? this.measureRepeat : 1);
   }

   public void setMeasureRepeatForTest(int n) {
      this.measureRepeat = Math.max(1, n);
   }

   public void setSkyTimeForTest(double seconds) {
      if (this.shown != null) {
         this.skyTimes[this.shown.ordinal()] = seconds;
      }
   }

   public void musicForTest(float bass) {
      this.musicForTest = bass;
   }

   public float[] stateForTest() {
      SkyStyle s = this.shown != null ? this.shown : this.style.get();
      SkySettings k = this.skies.get(s);
      return new float[]{
         this.visibility,
         this.beat,
         this.fade,
         this.fadingFrom != null ? 1.0F : 0.0F,
         this.brightnessOf(s),
         k.weather.get() ? this.worldRain : 0.0F,
         (float)this.skyTimes[s.ordinal()]
      };
   }

   public float[] fogForTest() {
      return new float[]{this.fogIn[0], this.fogIn[1], this.fogIn[2], this.fogOut[0], this.fogOut[1], this.fogOut[2]};
   }

   public SkyStyle shownForTest() {
      return this.shown;
   }

   public SkyStyle fadingFromForTest() {
      return this.fadingFrom;
   }

   public boolean cardViewsForTest() {
      return this.renderer.thumbTexture() >= 0;
   }

   public int horizonForTest() {
      return this.shown == null ? 0 : this.horizonNow();
   }

   public String statusForTest() {
      return this.status;
   }

   public boolean modifiedForTest(SkyStyle s) {
      return this.isModified(s);
   }

   public void toolsForTest(boolean copyList, boolean armApply) {
      if (this.tools != null) {
         this.tools.showForTest(copyList, armApply);
      }
   }

   public void copyForTest(SkyStyle from, SkyStyle to) {
      this.copyInto(from, to);
   }

   public void applyToAllForTest(SkyStyle from) {
      this.applyToAll(from);
   }

   public void resetSkyForTest(SkyStyle s) {
      this.resetSky(s);
   }

   public static enum Blend implements Displayable {
      REPLACE("Replace", "Only this sky"),
      OVERLAY("Overlay", "Over the normal sky, see-through");

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

   public static enum Center implements Displayable {
      ZENITH("Overhead", "Straight up"),
      HORIZON("Horizon", "On the horizon, in a compass direction");

      private final String displayName;
      private final String description;

      private Center(String displayName, String description) {
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

   public static enum Clouds implements Displayable {
      VANILLA("Vanilla", "Minecraft's own clouds, as they are"),
      TINTED("Tinted", "Minecraft's clouds, tinted to the sky"),
      HIDDEN("Hidden", "No clouds at all"),
      STYLE("Sky's Own", "The sky's own clouds, if it has some");

      private final String displayName;
      private final String description;

      private Clouds(String displayName, String description) {
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

   public static enum ColorSource implements Displayable {
      STYLE("Sky", "The sky's own colors"),
      GLOBAL("Global", "The Global Color from Settings"),
      CUSTOM("Custom", "Your colors, below (one, a gradient of two, or chroma)"),
      AURA("Target Aura", "The color of your Target Aura effect");

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

   public static enum Element implements Displayable {
      VANILLA("Vanilla", "Minecraft's own"),
      HIDDEN("Hidden", "None at all"),
      STYLE("Sky's Own", "The sky's own, if it has one");

      private final String displayName;
      private final String description;

      private Element(String displayName, String description) {
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

   public static enum OptiFineSkies implements Displayable {
      OVERRIDE("Override", "Draw this sky anyway"),
      STEP_ASIDE("Step Aside", "Show the resource pack's sky");

      private final String displayName;
      private final String description;

      private OptiFineSkies(String displayName, String description) {
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
      LOW("Low", "Half resolution, least detail", 3.0F, 0.5F),
      MEDIUM("Medium", "Three-quarter resolution", 4.0F, 0.75F),
      HIGH("High", "Full resolution (soft skies at half, smoothly scaled)", 5.0F, 1.0F),
      ULTRA("Ultra", "Full resolution for every sky, finest detail", 6.0F, 1.0F);

      private final String displayName;
      private final String description;
      final float detail;
      final float resolution;

      private Quality(String displayName, String description, float detail, float resolution) {
         this.displayName = displayName;
         this.description = description;
         this.detail = detail;
         this.resolution = resolution;
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

   public static enum RiftPlacement implements Displayable {
      NORTH("High in the North", "High in the northern sky"),
      ZENITH("Zenith", "Straight overhead"),
      DIRECTION("Direction", "As high, in a compass direction");

      private final String displayName;
      private final String description;

      private RiftPlacement(String displayName, String description) {
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

   public static enum Show implements Displayable {
      ALWAYS("Always", "Day and night (Day Brightness sets how bright it is by day)"),
      NIGHT("Night Only", "Minecraft's sky by day; this one fades in at dusk and out at dawn");

      private final String displayName;
      private final String description;

      private Show(String displayName, String description) {
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
