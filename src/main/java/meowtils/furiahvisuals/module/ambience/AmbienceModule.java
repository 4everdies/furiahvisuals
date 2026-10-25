package meowtils.furiahvisuals.module.ambience;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.compat.EntityViewRenderEvent;
import meowtils.furiahvisuals.core.Combat;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.gui.MenuNav;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.render.ViewFrustum;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public final class AmbienceModule extends Module {
   public final SliderSetting renderDistance;
   public final ModeSetting<AmbienceModule.WindMode> windMode;
   public final SliderSetting windDirection;
   public final SliderSetting windStrength;
   public final SliderSetting gustFrequency;
   public final SliderSetting gustStrength;
   public final SliderSetting turbulence;
   public final BooleanSetting overworld;
   public final BooleanSetting nether;
   public final BooleanSetting end;
   public final ModeSetting<AmbienceModule.Location> location;
   public final BooleanSetting hideUnderwater;
   public final BooleanSetting calmInCombat;
   public final SliderSetting calmOpacity;
   public final BooleanSetting atmosphere;
   public final ModeSetting<AmbienceModule.Dimension> atmosphereEditing;
   final ColorSetting[] tintColor = new ColorSetting[3];
   final SliderSetting[] tintStrength = new SliderSetting[3];
   final SliderSetting[] fogDistance = new SliderSetting[3];
   public final BooleanSetting wake;
   public final BooleanSetting wakeOthers;
   public final SliderSetting wakeStrength;
   public final SliderSetting totalCap;
   public final BooleanSetting adaptive;
   public final SliderSetting adaptiveFps;
   public final ModeSetting<AmbientStyle> editing;
   private final AmbientStyleBase[] styles;
   private final AmbientStyleBase[] drawOrder;
   final AmbientEnv env = new AmbientEnv();
   final AmbiencePreviews previews = new AmbiencePreviews(this);
   private final Atmosphere air = new Atmosphere();
   private final Camera camera = new Camera();
   private final ViewFrustum frustum = new ViewFrustum();
   private double lastCamX = Double.NaN;
   private double lastCamY;
   private double lastCamZ;
   private boolean reseed;
   private int refillTicks;
   private float quality = 1.0F;
   private long lastFrameNanos;
   private float renderMillis;
   private float tickMillis;
   private boolean skyDrawnThisFrame;
   private int lastDimension = Integer.MIN_VALUE;
   private static final int MAX_WAKE = 16;
   private final double[] wakeX = new double[16];
   private final double[] wakeY = new double[16];
   private final double[] wakeZ = new double[16];
   private final float[] wakeVX = new float[16];
   private final float[] wakeVZ = new float[16];
   private int wakeCount;
   private final float[] fogTint = new float[3];

   public AmbienceModule() {
      super("ambience", "Ambience", "Atmospheric effects that fill the air around you: wind, snow, crystals, fireflies and more.", true);
      BooleanSupplier fixed = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AmbienceModule.this.windMode.get() == AmbienceModule.WindMode.FIXED;
         }
      };
      this.renderDistance = this.add(
         new SliderSetting(
               "distance",
               "Render Distance",
               "Ambience only exists this far around you (never beyond Minecraft's own render distance). It fades out over the last fifth and into the fog, so there is never an edge.",
               8.0,
               96.0,
               32.0,
               1.0
            )
            .unit(" blocks")
            .<SliderSetting>group("Render Distance")
            .keywords(new String[]{"radius", "view distance"})
      );
      this.renderDistance.info(new Supplier<String>() {
         public String get() {
            int n = AmbienceModule.this.totalEstimate();
            return n == 0 ? "no style is on" : "≈ " + AmbienceModule.this.formatCount(n) + " particles";
         }
      }, new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AmbienceModule.this.totalEstimate() >= Math.min(4000, AmbienceModule.this.totalCap.getInt());
         }
      });
      this.windMode = this.add(
         new ModeSetting<>("windMode", "Direction", "Where the wind blows.", AmbienceModule.WindMode.class, AmbienceModule.WindMode.AUTO).group("Wind")
      );
      this.windDirection = this.add(
         new SliderSetting(
               "windDirection",
               "Blows Toward",
               "The compass direction the wind blows toward: 0° north, 90° east, 180° south, 270° west.",
               0.0,
               355.0,
               90.0,
               5.0
            )
            .unit("°")
            .<SliderSetting>group("Wind")
            .visibleWhen(fixed)
      );
      this.windStrength = this.add(new SliderSetting("windStrength", "Strength", "How hard the wind blows.", 0.0, 100.0, 40.0, 5.0).unit("%").group("Wind"));
      this.gustFrequency = this.add(
         new SliderSetting("gustFrequency", "Gust Frequency", "How often gusts come.", 0.0, 100.0, 35.0, 5.0).unit("%").group("Wind")
      );
      this.gustStrength = this.add(
         new SliderSetting(
               "gustStrength",
               "Gust Strength",
               "How much a gust speeds everything up (the Wind style sends a bunch of streaks with it).",
               0.0,
               100.0,
               50.0,
               5.0
            )
            .unit("%")
            .group("Wind")
      );
      this.turbulence = this.add(
         new SliderSetting("turbulence", "Turbulence", "Small swirls, so nothing moves in straight lines.", 0.0, 100.0, 30.0, 5.0).unit("%").group("Wind")
      );
      this.overworld = this.add(new BooleanSetting("overworld", "Overworld", "Shows in the overworld.", true).group("Where & When"));
      this.nether = this.add(
         new BooleanSetting("nether", "Nether", "Shows in the Nether (it has no sky: Outdoors Only is ignored there).", true).group("Where & When")
      );
      this.end = this.add(new BooleanSetting("end", "End", "Shows in the End.", true).group("Where & When"));
      this.location = this.add(
         new ModeSetting<>("location", "Location", "Where it may show.", AmbienceModule.Location.class, AmbienceModule.Location.OUTDOORS).group("Where & When")
      );
      this.hideUnderwater = this.add(
         new BooleanSetting("hideUnderwater", "Hide Underwater", "Fades away while your head is under water.", true).group("Where & When")
      );
      this.calmInCombat = this.add(
         new BooleanSetting(
               "calm",
               "Calm in Combat",
               "While you fight (Target Aura has a target, or you hit someone or were hit in the last 5 s), Ambience fades down so it never gets in the way.",
               false
            )
            .group("Where & When")
      );
      this.calmOpacity = this.add(
         new SliderSetting("calmOpacity", "Opacity in Combat", "How much of it stays while you fight.", 0.0, 100.0, 25.0, 5.0)
            .unit("%")
            .<SliderSetting>group("Where & When")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return AmbienceModule.this.calmInCombat.get();
               }
            })
      );
      BooleanSupplier atmosphereOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AmbienceModule.this.atmosphere.get();
         }
      };
      this.atmosphere = this.add(
         new BooleanSetting(
               "atmosphere",
               "Atmosphere",
               "Tints the fog, the sky and the clouds (like a red or a cyan world) and changes how far you see. Off: vanilla.",
               false
            )
            .group("Atmosphere")
      );
      this.atmosphereEditing = this.add(
         new ModeSetting<>(
               "atmosphereDimension",
               "Dimension",
               "Each dimension has its own tint and fog; pick the one to change.",
               AmbienceModule.Dimension.class,
               AmbienceModule.Dimension.OVERWORLD
            )
            .<ModeSetting>group("Atmosphere")
            .visibleWhen(atmosphereOn)
      );
      String[] dimKeys = new String[]{"overworld", "nether", "end"};
      float[] defaultTint = new float[]{45.0F, 40.0F, 40.0F};
      int[] defaultColors = new int[]{-11544321, -50373, -5219073};

      for (int d = 0; d < 3; d++) {
         final AmbienceModule.Dimension dim = AmbienceModule.Dimension.values()[d];
         BooleanSupplier shown = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return AmbienceModule.this.atmosphere.get() && AmbienceModule.this.atmosphereEditing.get() == dim;
            }
         };
         this.tintColor[d] = this.add(
            new ColorSetting(
                  "atmo." + dimKeys[d] + ".color",
                  "Tint",
                  "The color the air shifts toward.",
                  ColorSetting.Mode.STATIC,
                  defaultColors[d],
                  defaultColors[d],
                  1.0
               )
               .<ColorSetting>group("Atmosphere")
               .visibleWhen(shown)
         );
         this.tintStrength[d] = this.add(
            new SliderSetting(
                  "atmo." + dimKeys[d] + ".strength",
                  "Tint Strength",
                  "How far fog, sky and clouds shift toward the tint (their brightness stays).",
                  0.0,
                  100.0,
                  defaultTint[d],
                  5.0
               )
               .unit("%")
               .<SliderSetting>group("Atmosphere")
               .visibleWhen(shown)
         );
         this.fogDistance[d] = this.add(
            new SliderSetting(
                  "atmo." + dimKeys[d] + ".fog",
                  "Fog Distance",
                  "Below 1× the air is thicker (the fog comes closer); above, it is clearer.",
                  0.25,
                  3.0,
                  1.0,
                  0.05
               )
               .unit("×")
               .<SliderSetting>group("Atmosphere")
               .visibleWhen(shown)
         );
      }

      this.wake = this.add(
         new BooleanSetting(
               "wake",
               "Player Wake",
               "Light particles (snow, fireflies, petals, sparks, mist) are pushed aside and swirl behind you when you walk or sprint through them.",
               true
            )
            .group("Player Wake")
      );
      BooleanSupplier wakeOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return AmbienceModule.this.wake.get();
         }
      };
      this.wakeStrength = this.add(
         new SliderSetting("wakeStrength", "Strength", "How hard they are pushed.", 0.0, 100.0, 60.0, 5.0)
            .unit("%")
            .<SliderSetting>group("Player Wake")
            .visibleWhen(wakeOn)
      );
      this.wakeOthers = this.add(
         new BooleanSetting("wakeOthers", "Other Players Too", "Other players leave a wake as well.", false)
            .<BooleanSetting>group("Player Wake")
            .visibleWhen(wakeOn)
      );
      this.totalCap = this.add(
         new SliderSetting("totalCap", "Total Cap", "The most particles all styles together ever show.", 1000.0, 12000.0, 6000.0, 250.0)
            .<SliderSetting>group("Performance")
            .keywords(new String[]{"limit", "fps", "lag"})
      );
      this.adaptive = this.add(
         new BooleanSetting(
               "adaptive", "Adaptive Quality", "When your FPS drops below the value below, density goes down smoothly, and comes back later.", true
            )
            .group("Performance")
      );
      this.adaptiveFps = this.add(
         new SliderSetting("adaptiveFps", "Keep FPS Above", "The frame rate Adaptive Quality protects.", 20.0, 144.0, 50.0, 1.0)
            .<SliderSetting>group("Performance")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return AmbienceModule.this.adaptive.get();
               }
            })
      );
      this.editing = new ModeSetting<>("editing", "Open Style", "The style whose settings are shown.", AmbientStyle.class, AmbientStyle.WIND).nested();
      this.add(this.editing);
      final AmbienceModule self = this;
      this.add((new WidgetSetting("styles", "Styles", "Click a card for its settings; its switch turns it on or off. Any number can be on at once.") {
         @Override
         public Widget createWidget() {
            return new AmbienceCardsWidget(self);
         }
      }).group("Styles"));
      AmbientStyle[] types = AmbientStyle.values();
      this.styles = new AmbientStyleBase[types.length];

      for (final AmbientStyle type : types) {
         AmbientStyleBase s;
         this.styles[type.ordinal()] = s = type.create(this);
         this.add(s.on);
         Runnable changed = new Runnable() {
            @Override
            public void run() {
               AmbienceModule.this.previews.settingsChanged(type);
            }
         };
         s.on.addListener(changed);

         for (Setting setting : s.getSettings()) {
            this.add(setting);
            setting.addListener(changed);
         }
      }

      AmbientStyle[] order = new AmbientStyle[]{
         AmbientStyle.GROUND_MIST,
         AmbientStyle.BOKEH,
         AmbientStyle.PETALS,
         AmbientStyle.SNOWFALL,
         AmbientStyle.WIRE_CRYSTALS,
         AmbientStyle.PLEXUS,
         AmbientStyle.CIRCUIT_LINES,
         AmbientStyle.FIREFLIES,
         AmbientStyle.EMBERS,
         AmbientStyle.COMET_SPARKS,
         AmbientStyle.WIND
      };
      ArrayList<Object> list = new ArrayList<>();

      for (AmbientStyle t : order) {
         list.add(this.style(t));
      }

      for (AmbientStyleBase s : this.styles) {
         if (!list.contains(s) && s.spec.volume) {
            list.add(s);
         }
      }

      this.drawOrder = list.toArray(new AmbientStyleBase[0]);
   }

   public AmbientStyleBase style(AmbientStyle type) {
      return this.styles[type.ordinal()];
   }

   AmbientStyleBase[] styles() {
      return this.styles;
   }

   float minecraftDistance() {
      return Math.max(32, Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16);
   }

   int totalEstimate() {
      int sum = 0;

      for (AmbientStyleBase s : this.styles) {
         if (s.isOn() && s.spec.volume) {
            sum += s.estimate(s.radius(this.env));
         }
      }

      return Math.min(sum, this.totalCap.getInt());
   }

   String formatCount(int n) {
      return n >= 1000 ? String.format(Locale.ROOT, "%.1fk", n / 1000.0F) : Integer.toString(n);
   }

   @Override
   public void onTick() {
      long start = System.nanoTime();
      Minecraft mc = Minecraft.getMinecraft();
      Entity view = (Entity)(mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.thePlayer);
      if (mc.theWorld != null && view != null) {
         this.env.world = mc.theWorld;
         this.env.paused = mc.isGamePaused();
         this.env.tickCount++;
         this.env.camX = view.posX;
         this.env.camY = view.posY + view.getEyeHeight();
         this.env.camZ = view.posZ;
         int dim = mc.theWorld.provider.getDimensionId();
         if (dim != this.lastDimension) {
            this.lastDimension = dim;
            this.reseed = true;
         }

         double dz;
         double dy;
         double dx;
         if (!Double.isNaN(this.lastCamX)
            && (dx = this.env.camX - this.lastCamX) * dx + (dy = this.env.camY - this.lastCamY) * dy + (dz = this.env.camZ - this.lastCamZ) * dz
               > Math.pow(Math.max(12.0, this.renderDistance.get() * 0.5), 2.0)) {
            this.reseed = true;
            this.env.forgetGround();
         }

         this.lastCamX = this.env.camX;
         this.lastCamY = this.env.camY;
         this.lastCamZ = this.env.camZ;
         this.env.radius = Math.min(this.renderDistance.getFloat(), this.minecraftDistance());
         boolean sky = !mc.theWorld.provider.getHasNoSky();
         this.env.dayCycle = dim == 0 || sky && dim != 1 && dim != -1;
         this.env.outdoorsOnly = this.location.get() == AmbienceModule.Location.OUTDOORS && sky && dim != -1;
         float sun = MathHelper.cos(mc.theWorld.getCelestialAngle(1.0F) * (float) Math.PI * 2.0F);
         this.env.daylight = smooth(-0.18F, 0.18F, sun);
         this.env.light = this.env.dayCycle
            ? (0.36F + 0.64F * this.env.daylight) * (1.0F - 0.3F * mc.theWorld.getRainStrength(1.0F))
            : (dim == -1 ? 0.75F : 0.6F);
         this.env.tickCache();
         boolean allowed = this.dimensionAllowed(dim);
         boolean underwater = this.hideUnderwater.get() && view.isInsideOfMaterial(Material.water);
         float target = allowed && !underwater ? 1.0F : 0.0F;
         if (this.calmInCombat.get() && this.fighting(mc)) {
            target = Math.min(target, this.calmOpacity.getFloat() / 100.0F);
         }

         this.env.globalAlpha = this.env.globalAlpha + (target - this.env.globalAlpha) * (target < this.env.globalAlpha ? 0.14F : 0.07F);
         if (Math.abs(target - this.env.globalAlpha) < 0.003F) {
            this.env.globalAlpha = target;
         }

         if (!this.env.paused) {
            this.env
               .wind
               .tick(
                  0.05F,
                  this.windMode.get() == AmbienceModule.WindMode.FIXED ? this.windDirection.getFloat() : Float.NaN,
                  this.windStrength.getFloat() / 100.0F,
                  this.gustFrequency.getFloat() / 100.0F,
                  this.gustStrength.getFloat() / 100.0F,
                  this.turbulence.getFloat() / 100.0F
               );
            this.collectWake(mc);
            this.updateQuality(mc);
            float scale = this.quality * this.capScale();
            boolean again = this.reseed;
            this.reseed = false;
            this.refillTicks = again ? 10 : Math.max(0, this.refillTicks - 1);

            for (AmbientStyleBase s : this.styles) {
               s.tick(this.env, scale, again, this.refillTicks > 0);
            }
         }

         float ms = (float)(System.nanoTime() - start) / 1000000.0F;
         this.tickMillis = this.tickMillis == 0.0F ? ms : this.tickMillis + (ms - this.tickMillis) * 0.05F;
      }
   }

   private static float smooth(float e0, float e1, float x) {
      float t = Math.max(0.0F, Math.min(1.0F, (x - e0) / (e1 - e0)));
      return t * t * (3.0F - 2.0F * t);
   }

   private boolean dimensionAllowed(int dim) {
      return dim == -1 ? this.nether.get() : (dim == 1 ? this.end.get() : this.overworld.get());
   }

   private float capScale() {
      int sum = 0;

      for (AmbientStyleBase s : this.styles) {
         if (s.isOn() && s.spec.volume) {
            sum += s.estimate(s.radius(this.env));
         }
      }

      return sum <= this.totalCap.getInt() ? 1.0F : (float)this.totalCap.getInt() / sum;
   }

   private void updateQuality(Minecraft mc) {
      if (!this.adaptive.get()) {
         this.quality = 1.0F;
      } else {
         int fps = Minecraft.getDebugFPS();
         float want = this.adaptiveFps.getFloat();
         if (fps > 0 && fps < want) {
            this.quality = Math.max(0.3F, this.quality - 0.015F);
         } else if (fps > want * 1.15F) {
            this.quality = Math.min(1.0F, this.quality + 0.004F);
         }
      }
   }

   private boolean fighting(Minecraft mc) {
      return Combat.fighting();
   }

   private void collectWake(Minecraft mc) {
      this.wakeCount = 0;
      if (this.wake.get() && mc.theWorld != null) {
         List players = mc.theWorld.playerEntities;
         float r = this.env.radius + 2.0F;

         for (int i = 0; i < players.size() && this.wakeCount < 16; i++) {
            EntityPlayer p = (EntityPlayer)players.get(i);
            double dz;
            double dx;
            double vz;
            double vx;
            if ((p == mc.thePlayer || this.wakeOthers.get())
               && !p.isSpectator()
               && !p.isInvisible()
               && !((vx = (p.posX - p.prevPosX) * 20.0) * vx + (vz = (p.posZ - p.prevPosZ) * 20.0) * vz < 1.2)
               && !((dx = p.posX - this.env.camX) * dx + (dz = p.posZ - this.env.camZ) * dz > r * r)) {
               this.wakeX[this.wakeCount] = p.posX;
               this.wakeY[this.wakeCount] = p.posY + 0.9;
               this.wakeZ[this.wakeCount] = p.posZ;
               this.wakeVX[this.wakeCount] = (float)vx;
               this.wakeVZ[this.wakeCount] = (float)vz;
               this.wakeCount++;
            }
         }
      }
   }

   boolean wakeActive() {
      return this.wakeCount > 0;
   }

   void wake(AmbientPool p, int i, float dt) {
      float strength = this.wakeStrength.getFloat() / 100.0F;

      for (int s = 0; s < this.wakeCount; s++) {
         double dy = p.y[i] - this.wakeY[s];
         double dz;
         double dx;
         double d2;
         if (!(dy < -1.4) && !(dy > 1.4) && !((d2 = (dx = p.x[i] - this.wakeX[s]) * dx + (dz = p.z[i] - this.wakeZ[s]) * dz) > 3.2) && !(d2 < 1.0E-6)) {
            double d = Math.sqrt(d2);
            float speed = (float)Math.sqrt(this.wakeVX[s] * this.wakeVX[s] + this.wakeVZ[s] * this.wakeVZ[s]);
            float fall = (float)(1.0 - d / 1.8);
            float push = strength * fall * Math.min(1.0F, speed / 5.6F) * 9.0F;
            double nx = dx / d;
            double nz = dz / d;
            double side = nx * this.wakeVZ[s] - nz * this.wakeVX[s] >= 0.0 ? 1.0 : -1.0;
            float mx = this.wakeVX[s] / Math.max(0.1F, speed);
            float mz = this.wakeVZ[s] / Math.max(0.1F, speed);
            p.kx[i] = p.kx[i] + (float)((nx * 0.8 + mx * 0.5 - nz * side * 0.5) * push * dt * 4.0);
            p.kz[i] = p.kz[i] + (float)((nz * 0.8 + mz * 0.5 + nx * side * 0.5) * push * dt * 4.0);
            p.ky[i] = p.ky[i] + (float)(push * dt * (0.8 - dy * 0.3));
         }
      }
   }

   private boolean anyParticles() {
      for (AmbientStyleBase s : this.styles) {
         if (s.count() > 0 || s.presence > 0.0F && !s.spec.volume) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void onRenderWorld(float partialTicks) {
      long now = GameClock.nanos();
      float dt = this.lastFrameNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrameNanos) / 1.0E9F);
      this.lastFrameNanos = now;
      Minecraft mc = Minecraft.getMinecraft();
      this.updateAtmosphere(dt, mc);
      if (this.anyParticles()) {
         long start = System.nanoTime();
         Textures.ensureAmbienceLoaded();
         RenderState.begin();

         try {
            this.frame(mc, partialTicks, dt);
            if (!this.skyDrawnThisFrame) {
               this.env.skyPass = false;

               for (AmbientStyleBase s : this.styles) {
                  if (!s.spec.volume && s.presence > 0.0F) {
                     s.drawSky(this.env);
                     Vfx.flush();
                  }
               }
            }

            this.skyDrawnThisFrame = false;

            for (AmbientStyleBase sx : this.drawOrder) {
               if (sx.count() > 0) {
                  sx.draw(this.env);
                  Vfx.flush();
                  RenderState.additive();
               }
            }
         } finally {
            RenderState.end();
         }

         float var16 = (float)(System.nanoTime() - start) / 1000000.0F;
         this.renderMillis = this.renderMillis == 0.0F ? var16 : this.renderMillis + (var16 - this.renderMillis) * 0.05F;
      }
   }

   private void frame(Minecraft mc, float partialTicks, float dt) {
      this.camera.captureModelView();
      Vfx.setCamera(this.camera);
      this.frustum.capture();
      this.env.cam = this.camera;
      this.env.frustum = this.frustum;
      this.env.partialTicks = partialTicks;
      this.env.viewX = mc.getRenderManager().viewerPosX;
      this.env.viewY = mc.getRenderManager().viewerPosY;
      this.env.viewZ = mc.getRenderManager().viewerPosZ;
      Entity v = (Entity)(mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.thePlayer);
      if (v != null) {
         this.env.eyeX = v.lastTickPosX + (v.posX - v.lastTickPosX) * partialTicks;
         this.env.eyeY = v.lastTickPosY + (v.posY - v.lastTickPosY) * partialTicks + v.getEyeHeight();
         this.env.eyeZ = v.lastTickPosZ + (v.posZ - v.lastTickPosZ) * partialTicks;
      }

      this.env.frameDt = this.env.paused ? 0.0F : dt;
      this.env.time = this.env.time + this.env.frameDt;
      FogCapture.apply(this.env, this.minecraftDistance());
   }

   public void renderSkyPass(float partialTicks) {
      if (this.isEnabled()) {
         boolean any = false;

         for (AmbientStyleBase s : this.styles) {
            any |= !s.spec.volume && s.presence > 0.0F;
         }

         if (any) {
            Minecraft mc = Minecraft.getMinecraft();
            Textures.ensureAmbienceLoaded();
            RenderState.begin();

            try {
               this.camera.captureModelView();
               Vfx.setCamera(this.camera);
               this.env.cam = this.camera;
               this.env.partialTicks = partialTicks;
               this.env.skyPass = true;

               for (AmbientStyleBase s : this.styles) {
                  if (!s.spec.volume && s.presence > 0.0F) {
                     s.drawSky(this.env);
                     Vfx.flush();
                  }
               }
            } finally {
               RenderState.end();
            }

            this.skyDrawnThisFrame = true;
         }
      }
   }

   private int dimensionIndex(int dim) {
      return dim == -1 ? 1 : (dim == 1 ? 2 : 0);
   }

   private void updateAtmosphere(float dt, Minecraft mc) {
      if (mc.theWorld != null) {
         int d = this.dimensionIndex(mc.theWorld.provider.getDimensionId());
         boolean on = this.atmosphere.get();
         ColorSetting tint = this.tintColor[d];
         this.air
            .update(
               dt,
               tint.getColor(0.0F) | 0xFF000000,
               tint.getColor(0.5F) | 0xFF000000,
               tint.getColor(1.0F) | 0xFF000000,
               on ? this.tintStrength[d].getFloat() / 100.0F : 0.0F,
               on ? this.fogDistance[d].getFloat() : 1.0F
            );
      }
   }

   public Vec3 tintSky(Vec3 sky) {
      return this.isEnabled() ? this.air.tint(2, sky, 1.0F) : sky;
   }

   public Vec3 tintClouds(Vec3 clouds) {
      return this.isEnabled() ? this.air.tint(1, clouds, 0.85F) : clouds;
   }

   public boolean tintsFog() {
      return this.isEnabled() && this.air.active();
   }

   @Override
   public void onFogColors(EntityViewRenderEvent.FogColors event) {
      if (this.air.active()) {
         Material m;
         if (event.block == null || (m = event.block.getMaterial()) != Material.water && m != Material.lava) {
            this.air.tint(0, event.red, event.green, event.blue, this.fogTint, 1.0F);
            event.red = this.fogTint[0];
            event.green = this.fogTint[1];
            event.blue = this.fogTint[2];
         }
      }
   }

   @Override
   public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
      if (this.air.fogChanged()) {
         FogCapture.scaleFog(event, this.air.fogFactor());
      }
   }

   public float fogFactor() {
      return this.isEnabled() && this.air.fogChanged() ? this.air.fogFactor() : 1.0F;
   }

   @Override
   public void onWorldChange() {
      for (AmbientStyleBase s : this.styles) {
         s.clear();
      }

      this.lastCamX = Double.NaN;
      this.env.world = null;
      this.env.forgetGround();
      this.wakeCount = 0;
      this.reseed = true;
   }

   @Override
   protected void onDisable() {
      this.onWorldChange();
      this.air.reset();
      this.lastFrameNanos = 0L;
   }

   @Override
   public int getAccentColor(double time) {
      return this.style(this.editing.get()).color.getDisplayColor();
   }

   @Override
   public boolean previewsInWorld() {
      return true;
   }

   @Override
   public boolean drawsWithHudHidden() {
      return true;
   }

   @Override
   public boolean isGroupCollapsible(String group) {
      return "Wind".equals(group) || "Where & When".equals(group) || "Atmosphere".equals(group) || "Player Wake".equals(group) || "Performance".equals(group);
   }

   @Override
   public boolean isGroupCollapsedByDefault(String group) {
      return "Player Wake".equals(group) || "Performance".equals(group);
   }

   @Override
   public String summary() {
      StringBuilder sb = new StringBuilder();
      int n = 0;

      for (AmbientStyleBase s : this.styles) {
         if (s.isOn() && n++ < 2) {
            sb.append(sb.length() > 0 ? ", " : "").append(s.styleName());
         }
      }

      if (n == 0) {
         return "No style on";
      } else {
         return n > 2 ? sb + " +" + (n - 2) : sb.toString();
      }
   }

   @Override
   public String getPreviewInfo() {
      int n = 0;

      for (AmbientStyleBase s : this.styles) {
         n += s.count();
      }

      return n == 0 && this.renderMillis <= 0.0F
         ? null
         : String.format(Locale.ROOT, "Ambience: %s particles, %.2f ms / frame", this.formatCount(n), this.renderMillis + this.tickMillis / 3.0F);
   }

   public boolean openStylePageForTest(AmbientStyle type) {
      this.editing.set(type);
      return MenuNav.openStylePage(this, this.style(type));
   }

   public int cardPreviewCountForTest(AmbientStyle type) {
      return this.previews.cardCountForTest(type);
   }

   public int bigPreviewCountForTest() {
      return this.previews.bigCountForTest();
   }

   public float bigPreviewPresenceForTest() {
      return this.previews.bigPresenceForTest();
   }

   public String previewSettingForTest(AmbientStyle type, boolean big, String id) {
      return this.previews.twinSettingForTest(type, big, id);
   }

   public float previewWindForTest() {
      return this.previews.windSpeedForTest();
   }

   public void previewPressForTest(int button) {
      AmbiencePreviewWidget w = AmbiencePreviewWidget.lastShown;
      if (w != null) {
         w.press(button);
      }
   }

   public Boolean previewDayForTest() {
      AmbiencePreviewWidget w = AmbiencePreviewWidget.lastShown;
      return w == null ? null : w.dayForTest();
   }

   public float[] previewDrawsForTest() {
      return new float[]{(float)this.previews.drawsForTest(), this.previews.drawMillisForTest()};
   }

   public String countsForTest() {
      StringBuilder sb = new StringBuilder();

      for (AmbientStyleBase s : this.styles) {
         if (s.count() > 0) {
            if (sb.length() > 0) {
               sb.append(", ");
            }

            sb.append(s.describe());
         }
      }

      return sb.length() == 0 ? "none" : sb.toString();
   }

   public int countForTest(AmbientStyle type) {
      return this.style(type).count();
   }

   public float renderMillisForTest() {
      return this.renderMillis;
   }

   public float tickMillisForTest() {
      return this.tickMillis;
   }

   public float globalAlphaForTest() {
      return this.env.globalAlpha;
   }

   public float windDegreesForTest() {
      return this.env.wind.compassDegrees();
   }

   public boolean windAt(double x, double y, double z, float[] out) {
      if (!this.isEnabled()) {
         return false;
      } else {
         this.env.wind.velocity(x, y, z, 1.0F, out);
         return true;
      }
   }

   public float windSpeedForTest() {
      return this.env.wind.speed;
   }

   public double farthestForTest(AmbientStyle type) {
      AmbientStyleBase s = this.style(type);
      double far = 0.0;
      if (s.pool != null) {
         for (int i = 0; i < s.pool.count; i++) {
            double dx = s.pool.x[i] - this.env.camX;
            double dz = s.pool.z[i] - this.env.camZ;
            far = Math.max(far, Math.sqrt(dx * dx + dz * dz));
         }
      }

      return far;
   }

   public int coveredForTest(AmbientStyle type) {
      AmbientStyleBase s = this.style(type);
      int n = 0;
      if (s.pool != null) {
         for (int i = 0; i < s.pool.count; i++) {
            double g = this.env.groundAt(s.pool.x[i], s.pool.z[i]);
            if (!Double.isNaN(g) && s.pool.y[i] < g - 0.05) {
               n++;
            }
         }
      }

      return n;
   }

   public int insideBlocksForTest(AmbientStyle type) {
      AmbientStyleBase s = this.style(type);
      int n = 0;
      if (s.pool != null) {
         for (int i = 0; i < s.pool.count; i++) {
            if (this.env.solidAt(s.pool.x[i], s.pool.y[i], s.pool.z[i])) {
               n++;
            }
         }
      }

      return n;
   }

   public boolean atmosphereActiveForTest() {
      return this.air.active();
   }

   public int estimateForTest() {
      return this.totalEstimate();
   }

   public int windBlockedForTest() {
      return ((WindStyle)this.style(AmbientStyle.WIND)).blockedForTest();
   }

   public double checksumForTest() {
      double sum = this.env.time;

      for (AmbientStyleBase s : this.styles) {
         if (s.pool != null) {
            for (int i = 0; i < s.pool.count; i++) {
               sum += s.pool.x[i] * 3.0 + s.pool.y[i] * 5.0 + s.pool.z[i] * 7.0;
            }
         }
      }

      return sum;
   }

   public float presenceForTest(AmbientStyle type) {
      return this.style(type).presence;
   }

   public float[] fogForTest() {
      return new float[]{this.env.fogStart, this.env.fogEnd};
   }

   public int[] balanceForTest(AmbientStyle type, double dirX, double dirZ) {
      AmbientStyleBase s = this.style(type);
      int ahead = 0;
      int behind = 0;
      int kicked = 0;
      int near = 0;
      if (s.pool != null) {
         for (int i = 0; i < s.pool.count; i++) {
            double dx = s.pool.x[i] - this.env.camX;
            double dz = s.pool.z[i] - this.env.camZ;
            if (dx * dirX + dz * dirZ >= 0.0) {
               ahead++;
            } else {
               behind++;
            }

            if (dx * dx + dz * dz < 100.0) {
               near++;
            }

            if (s.pool.kx[i] != 0.0F || s.pool.ky[i] != 0.0F || s.pool.kz[i] != 0.0F) {
               kicked++;
            }
         }
      }

      return new int[]{ahead, behind, kicked, near};
   }

   public int totalCountForTest() {
      int n = 0;

      for (AmbientStyleBase s : this.styles) {
         n += s.count();
      }

      return n;
   }

   public static enum Dimension implements Displayable {
      OVERWORLD("Overworld", "The overworld's air"),
      NETHER("Nether", "The Nether's air"),
      END("End", "The End's air");

      private final String displayName;
      private final String description;

      private Dimension(String displayName, String description) {
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

   public static enum Location implements Displayable {
      OUTDOORS("Outdoors Only", "Only where the sky is visible: nothing under roofs or in caves"),
      EVERYWHERE("Everywhere", "Indoors and in caves too");

      private final String displayName;
      private final String description;

      private Location(String displayName, String description) {
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

   public static enum WindMode implements Displayable {
      AUTO("Auto", "Turns slowly by itself"),
      FIXED("Fixed", "Always blows the same way");

      private final String displayName;
      private final String description;

      private WindMode(String displayName, String description) {
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
