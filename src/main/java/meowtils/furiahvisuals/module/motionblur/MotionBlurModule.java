package meowtils.furiahvisuals.module.motionblur;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.core.Keybinds;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.render.OptiFineCompat;
import meowtils.furiahvisuals.render.post.PostChain;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.KeyBindSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

public final class MotionBlurModule extends Module {
   public final ModeSetting<BlurStyle> style;
   private final MotionBlurModule.Owner[] owners = new MotionBlurModule.Owner[BlurStyle.values().length];
   public final SliderSetting classicStrength;
   public final SliderSetting realisticStrength;
   public final SliderSetting realisticMovement;
   public final SliderSetting realisticMaxLength;
   public final ModeSetting<MotionBlurModule.Quality> realisticQuality;
   public final SliderSetting hybridStrength;
   public final SliderSetting hybridTrails;
   public final SliderSetting hybridMovement;
   public final SliderSetting hybridMaxLength;
   public final ModeSetting<MotionBlurModule.Quality> hybridQuality;
   public final BooleanSetting debug;
   public final BooleanSetting fpsIndependent;
   public final BooleanSetting onlyTurning;
   public final SliderSetting turnSpeed;
   public final BooleanSetting firstPerson;
   public final BooleanSetting thirdPerson;
   public final BooleanSetting inMenus;
   public final BooleanSetting blurHand;
   public final KeyBindSetting toggleKey;
   public final ModeSetting<MotionBlurModule.TrailLook> trailLook;
   public final BooleanSetting naturalLight;
   public final BooleanSetting matchRefresh;
   public final ModeSetting<MotionBlurModule.Precision> precision;
   private final MotionBlurPass pass;
   private final Map<SliderSetting, double[]> looks = new IdentityHashMap<>();
   private static float refresh = -1.0F;
   private static long refreshCheckedMs;
   private final Module.StartingLooks startingLooks = new Module.StartingLooks() {
      @Override
      public List<SliderSetting> targets() {
         ArrayList<SliderSetting> out = new ArrayList<>();

         for (SliderSetting s : MotionBlurModule.this.looks.keySet()) {
            if (s.isVisible()) {
               out.add(s);
            }
         }

         if (out.size() > 1) {
            Collections.sort(out, new Comparator<SliderSetting>() {
               public int compare(SliderSetting a, SliderSetting b) {
                  return MotionBlurModule.this.getSettings().indexOf(a) - MotionBlurModule.this.getSettings().indexOf(b);
               }
            });
         }

         return out;
      }

      @Override
      public double value(SliderSetting slider, int look) {
         double[] v = MotionBlurModule.this.looks.get(slider);
         return v == null ? slider.getDefault() : v[Math.max(0, Math.min(2, look))];
      }

      @Override
      public String tooltip(int look) {
         StringBuilder sb = new StringBuilder(look == 0 ? "A lighter blur" : (look == 1 ? "The default blur" : "A stronger blur"));
         String sep = " (";

         for (SliderSetting s : this.targets()) {
            sb.append(sep).append(s.getLabel()).append(' ').append(MotionBlurModule.format(this.value(s, look))).append(s.getUnit());
            sep = ", ";
         }

         return sb.append(sep.equals(", ") ? ")" : "").toString();
      }

      @Override
      public String after() {
         return "style";
      }

      @Override
      public String rowTooltip() {
         return "A starting point for this style's strength and blur; you can fine-tune it after";
      }
   };
   private String debugText = "";
   private float debugScale = 1.0F;
   private long debugNextMs;

   public MotionBlurModule() {
      super("motion_blur", "Motion Blur", "Smooth camera motion: Lunar-style trails or shader-pack camera blur.", false);

      for (BlurStyle s : BlurStyle.values()) {
         this.owners[s.ordinal()] = new MotionBlurModule.Owner(s);
      }

      this.style = this.add(
         new ModeSetting<>(
               "style",
               "Style",
               "Classic blends each frame with the ones before it, so motion leaves trails, also on moving players. Realistic blurs the camera's motion like a shader pack: turning and moving blur the world, standing still is sharp. Hybrid is Realistic with a light Classic trail on top, so moving players leave a faint trail too.",
               BlurStyle.class,
               BlurStyle.CLASSIC
            )
            .style(ModeSetting.Style.CARDS)
            .keywords(
               new String[]{"motion blur", "blur", "lunar", "badlion", "frame blending", "trails", "smear", "ghosting", "shader", "camera blur", "realistic"}
            )
      );
      this.classicStrength = this.add(
         this.own(
            BlurStyle.CLASSIC,
            new SliderSetting("classic.strength", "Strength", "How long the trails last: 0% turns the blur off (it then costs nothing).", 0.0, 100.0, 40.0, 1.0)
               .unit("%")
               .keywords(new String[]{"blur amount", "intensity", "trail length"})
         )
      );
      this.looks.put(this.classicStrength, new double[]{25.0, 40.0, 65.0});
      this.realisticStrength = this.add(
         this.own(
            BlurStyle.REALISTIC,
            new SliderSetting(
                  "realistic.strength",
                  "Strength",
                  "How long the camera's shutter stays open: 50% is a 180° shutter, like film; 0% turns the blur off (it then costs nothing). Realistic blurs only the camera's motion: players who move while you stand still stay sharp (Hybrid adds trails for them), see-through blocks and particles take the blur of what is behind them, and in third person your own model blurs as you turn.",
                  0.0,
                  100.0,
                  50.0,
                  1.0
               )
               .unit("%")
               .keywords(new String[]{"blur amount", "intensity", "shutter"})
         )
      );
      this.looks.put(this.realisticStrength, new double[]{30.0, 50.0, 80.0});
      this.realisticMovement = this.add(
         this.own(
            BlurStyle.REALISTIC,
            new SliderSetting(
                  "realistic.movement", "Movement Blur", "How much walking, strafing and falling blur the world; 0%: only turning does.", 0.0, 100.0, 50.0, 1.0
               )
               .unit("%")
               .keywords(new String[]{"walking", "strafing", "moving", "falling"})
         )
      );
      this.looks.put(this.realisticMovement, new double[]{25.0, 50.0, 75.0});
      this.realisticMaxLength = this.add(
         this.own(
            BlurStyle.REALISTIC,
            new SliderSetting(
                  "realistic.maxLength",
                  "Max Blur Length",
                  "The longest a streak gets, in % of the screen's height: caps the blur of a fast flick.",
                  1.0,
                  15.0,
                  5.0,
                  0.5
               )
               .unit("%")
               .keywords(new String[]{"flick", "streak", "length", "cap"})
         )
      );
      this.looks.put(this.realisticMaxLength, new double[]{3.0, 5.0, 8.0});
      this.realisticQuality = this.add(
         this.own(
            BlurStyle.REALISTIC,
            new ModeSetting<>(
                  "realistic.quality",
                  "Quality",
                  "How many samples each pixel's streak takes: more are smoother and cost more.",
                  MotionBlurModule.Quality.class,
                  MotionBlurModule.Quality.MEDIUM
               )
               .keywords(new String[]{"samples", "performance"})
         )
      );
      this.hybridStrength = this.add(
         this.own(
            BlurStyle.HYBRID,
            new SliderSetting(
                  "hybrid.strength",
                  "Strength",
                  "How long the camera's shutter stays open (Realistic's blur): 50% is a 180° shutter; 0% leaves only the trails.",
                  0.0,
                  100.0,
                  45.0,
                  1.0
               )
               .unit("%")
               .keywords(new String[]{"blur amount", "intensity", "shutter"})
         )
      );
      this.looks.put(this.hybridStrength, new double[]{30.0, 45.0, 70.0});
      this.hybridTrails = this.add(
         this.own(
            BlurStyle.HYBRID,
            new SliderSetting(
                  "hybrid.trails",
                  "Trails",
                  "How strong the light Classic trail on top is, so moving players leave a faint trail too; 0% leaves only the camera blur.",
                  0.0,
                  100.0,
                  25.0,
                  1.0
               )
               .unit("%")
               .keywords(new String[]{"trail", "ghosting", "players"})
         )
      );
      this.looks.put(this.hybridTrails, new double[]{15.0, 25.0, 40.0});
      this.hybridMovement = this.add(
         this.own(
            BlurStyle.HYBRID,
            new SliderSetting(
                  "hybrid.movement", "Movement Blur", "How much walking, strafing and falling blur the world; 0%: only turning does.", 0.0, 100.0, 50.0, 1.0
               )
               .unit("%")
               .keywords(new String[]{"walking", "strafing", "moving", "falling"})
         )
      );
      this.looks.put(this.hybridMovement, new double[]{25.0, 50.0, 75.0});
      this.hybridMaxLength = this.add(
         this.own(
            BlurStyle.HYBRID,
            new SliderSetting(
                  "hybrid.maxLength",
                  "Max Blur Length",
                  "The longest a streak gets, in % of the screen's height: caps the blur of a fast flick.",
                  1.0,
                  15.0,
                  5.0,
                  0.5
               )
               .unit("%")
               .keywords(new String[]{"flick", "streak", "length", "cap"})
         )
      );
      this.looks.put(this.hybridMaxLength, new double[]{3.0, 5.0, 8.0});
      this.hybridQuality = this.add(
         this.own(
            BlurStyle.HYBRID,
            new ModeSetting<>(
                  "hybrid.quality",
                  "Quality",
                  "How many samples each pixel's streak takes: more are smoother and cost more.",
                  MotionBlurModule.Quality.class,
                  MotionBlurModule.Quality.MEDIUM
               )
               .keywords(new String[]{"samples", "performance"})
         )
      );
      BooleanSupplier trails = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return MotionBlurModule.this.style.get().trails();
         }
      };
      this.fpsIndependent = this.add(
         new BooleanSetting(
               "fpsIndependent", "FPS-Independent", "The same blur at 60 or 600 FPS. Off: the old behaviour, where more FPS means less blur.", true
            )
            .keywords(new String[]{"fps", "frame rate", "framerate"})
      );
      this.onlyTurning = this.add(
         new BooleanSetting("onlyTurning", "Only While Turning", "Trails appear only while the camera turns, so standing still and walking stay sharp.", false)
            .<BooleanSetting>visibleWhen(trails)
            .keywords(new String[]{"turning", "camera", "flick"})
      );
      BooleanSupplier turningOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return MotionBlurModule.this.onlyTurning.get();
         }
      };
      this.turnSpeed = this.add(
         new SliderSetting(
               "turnSpeed", "Turn Speed for Full Blur", "How fast the camera has to turn for the full blur; slower turns blur less.", 30.0, 720.0, 180.0, 10.0
            )
            .unit("°/s")
            .<SliderSetting>visibleWhen(trails)
            .<SliderSetting>visibleWhen(turningOn)
            .keywords(new String[]{"turn speed", "sensitivity"})
      );
      this.firstPerson = this.add(new BooleanSetting("firstPerson", "First Person", "Blur in first person.", true));
      this.thirdPerson = this.add(
         new BooleanSetting("thirdPerson", "Third Person", "Blur in both third-person views (F5).", true).keywords(new String[]{"f5", "third person"})
      );
      this.inMenus = this.add(
         new BooleanSetting(
               "inMenus",
               "While a Menu Is Open",
               "Keep blurring the world behind chat and inventories. This mod's menu always keeps it running, so changes show live.",
               true
            )
            .keywords(new String[]{"chat", "inventory", "gui", "menu"})
      );
      this.blurHand = this.add(
         new BooleanSetting(
               "blurHand",
               "Blur Hand & Held Item",
               "Your hand and the item you hold (Weapons' models and Glow's halo too) trail with the world. Off: they stay sharp.",
               false
            )
            .<BooleanSetting>visibleWhen(trails)
            .keywords(new String[]{"hand", "held item", "sword", "item"})
      );
      this.toggleKey = this.add(
         new KeyBindSetting(
               "toggleKey",
               "Toggle Key",
               "A key that turns Motion Blur on and off, with the island's notice (also in Options › Controls).",
               new Supplier<KeyBinding>() {
                  public KeyBinding get() {
                     return Keybinds.toggleOf(MotionBlurModule.this);
                  }
               }
            )
            .keywords(new String[]{"keybind", "hotkey", "toggle"})
      );
      this.trailLook = this.add(
         new ModeSetting<>(
               "trailLook",
               "Trail Look",
               "Smooth fades old frames evenly; Phosphor keeps only their bright parts, like the old Super Secret Settings shader.",
               MotionBlurModule.TrailLook.class,
               MotionBlurModule.TrailLook.SMOOTH
            )
            .<ModeSetting>visibleWhen(trails)
            .keywords(new String[]{"phosphor", "super secret settings"})
      );
      this.naturalLight = this.add(
         new BooleanSetting(
               "naturalLight",
               "Natural Light Blending",
               "Blends in linear light, like a real camera, for cleaner, brighter trails. Trails need High precision for it.",
               true
            )
            .keywords(new String[]{"linear", "gamma", "camera"})
      );
      this.matchRefresh = this.add(
         new BooleanSetting(
               "matchRefresh",
               "Match Monitor Refresh Rate",
               "Uses your monitor's refresh rate instead of 60 FPS as the reference, so high-refresh monitors get proportionally less blur.",
               false
            )
            .keywords(new String[]{"hz", "refresh rate", "144hz", "240hz"})
      );
      this.precision = this.add(
         new ModeSetting<>(
               "precision",
               "Buffer Precision",
               "Auto picks High where the graphics card supports it. Standard uses less memory; faint trails then fade in small steps.",
               MotionBlurModule.Precision.class,
               MotionBlurModule.Precision.AUTO
            )
            .keywords(new String[]{"hdr", "float", "16-bit", "banding"})
      );
      this.debug = this.add(
         new BooleanSetting(
               "debug",
               "Debug Overlay",
               "One small line on the HUD: the style, FPS, frame time, the trails' keep factor, the precision, where it hooks in, and what it costs (GPU time where the graphics card can measure it).",
               false
            )
            .keywords(new String[]{"debug", "performance", "cost", "fps"})
      );
      group("Main", this.style, this.classicStrength, this.realisticStrength, this.hybridStrength, this.hybridTrails);
      group(
         "Motion",
         this.fpsIndependent,
         this.realisticMovement,
         this.realisticMaxLength,
         this.hybridMovement,
         this.hybridMaxLength,
         this.onlyTurning,
         this.turnSpeed
      );
      group("When it shows", this.firstPerson, this.thirdPerson, this.inMenus, this.blurHand, this.toggleKey);
      group("Advanced", this.realisticQuality, this.hybridQuality, this.trailLook, this.naturalLight, this.matchRefresh, this.precision, this.debug);
      this.pass = new MotionBlurPass(this);
      PostChain.register(this.pass);
      Runnable restart = new Runnable() {
         @Override
         public void run() {
            MotionBlurModule.this.pass.reset();
         }
      };
      this.style.addListener(restart);
      this.blurHand.addListener(restart);
   }

   private <S extends Setting> S own(BlurStyle s, S setting) {
      return setting.ownedBy(this.owners[s.ordinal()]);
   }

   private static void group(String group, Setting... settings) {
      for (Setting s : settings) {
         s.group(group);
      }
   }

   float strength() {
      switch ((BlurStyle)this.style.get()) {
         case REALISTIC:
            return this.realisticStrength.getFloat();
         case HYBRID:
            return this.hybridStrength.getFloat();
         default:
            return this.classicStrength.getFloat();
      }
   }

   float cameraStrength() {
      BlurStyle s = this.style.get();
      return s == BlurStyle.REALISTIC ? this.realisticStrength.getFloat() : (s == BlurStyle.HYBRID ? this.hybridStrength.getFloat() : 0.0F);
   }

   float trailStrength() {
      BlurStyle s = this.style.get();
      return s == BlurStyle.CLASSIC ? this.classicStrength.getFloat() : (s == BlurStyle.HYBRID ? this.hybridTrails.getFloat() : 0.0F);
   }

   private boolean hybrid() {
      return this.style.get() == BlurStyle.HYBRID;
   }

   float movement() {
      return (this.hybrid() ? this.hybridMovement : this.realisticMovement).getFloat() / 100.0F;
   }

   float maxLength() {
      return (this.hybrid() ? this.hybridMaxLength : this.realisticMaxLength).getFloat() / 100.0F;
   }

   int quality() {
      return (this.hybrid() ? this.hybridQuality : this.realisticQuality).get().ordinal();
   }

   float referenceHz() {
      return this.matchRefresh.get() ? refreshRate() : 60.0F;
   }

   static float refreshRate() {
      long now = System.currentTimeMillis();
      if (refresh < 0.0F || now - refreshCheckedMs > 5000L) {
         refreshCheckedMs = now;
         float hz = 60.0F;

         try {
            DisplayMode mode = Display.getDesktopDisplayMode();
            if (mode != null && mode.getFrequency() >= 24) {
               hz = Math.min(500.0F, (float)mode.getFrequency());
            }
         } catch (RuntimeException | LinkageError var4) {
            hz = 60.0F;
         }

         refresh = hz;
      }

      return refresh;
   }

   @Override
   protected void onEnable() {
      this.pass.reset();
   }

   @Override
   protected void onDisable() {
      this.pass.release();
   }

   @Override
   public void onWorldChange() {
      this.pass.reset();
   }

   @Override
   public void onClientTick() {
      this.pass.releaseIfIdle();
   }

   @Override
   public float optionArtHeight(Setting setting) {
      return setting == this.style ? 22.0F : 0.0F;
   }

   @Override
   public void drawOptionArt(Setting setting, Enum<?> option, float x, float y, float w, float h, double time, boolean selected, int accent) {
      if (setting == this.style && option instanceof BlurStyle) {
         CardArt.draw((BlurStyle)option, x, y, w, h, time, selected, accent);
      }
   }

   @Override
   public Module.StartingLooks startingLooks() {
      return this.startingLooks;
   }

   private static String format(double v) {
      return v == Math.rint(v) ? String.valueOf((long)v) : String.format(Locale.ROOT, "%.1f", v);
   }

   @Override
   public Module.Notice getNotice() {
      String failure = this.pass.failure();
      if (failure != null) {
         return new Module.Notice(
            "Motion Blur stopped working on this graphics driver (" + failure + ") and is off until the game restarts. The log has the details.", true
         );
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.gameSettings != null && mc.gameSettings.anaglyph) {
            return new Module.Notice("Motion Blur is off while 3D Anaglyph is on (Options › Video Settings).", false);
         } else if (OptiFineCompat.shadersActive()) {
            return new Module.Notice("Paused while a shader pack is active. Shader packs have their own motion blur.", false);
         } else {
            return this.precision.get() == MotionBlurModule.Precision.HIGH && this.pass.highRefused()
               ? new Module.Notice(
                  "This graphics driver can't keep the trails in High precision, so Standard is used: faint trails fade in small steps, and Natural Light Blending is off.",
                  false
               )
               : null;
         }
      }
   }

   @Override
   public String summary() {
      return this.style.get().displayName() + " " + Math.round(this.strength()) + "%";
   }

   @Override
   public void onRenderHud(float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      if (this.debug.get() && !mc.gameSettings.showDebugInfo) {
         long now = System.currentTimeMillis();
         if (now >= this.debugNextMs) {
            this.debugNextMs = now + 250L;
            this.debugText = this.pass.debugLine();
            ScaledResolution res = new ScaledResolution(mc);
            float s = Math.min(1.0F, 2.0F / res.getScaleFactor());
            int w = mc.fontRendererObj.getStringWidth(this.debugText);
            if (w * s > res.getScaledWidth() - 4) {
               s = (float)(res.getScaledWidth() - 4) / Math.max(1, w);
            }

            this.debugScale = s;
         }

         GlStateManager.pushMatrix();
         GlStateManager.translate(2.0F, 2.0F, 0.0F);
         GlStateManager.scale(this.debugScale, this.debugScale, 1.0F);
         mc.fontRendererObj.drawStringWithShadow(this.debugText, 0.0F, 0.0F, -920584);
         GlStateManager.popMatrix();
      }
   }

   public String debugLineForTest() {
      return this.pass.debugLine();
   }

   public String stateForTest() {
      return this.pass.stateForTest();
   }

   public boolean allocatedForTest() {
      return this.pass.allocatedForTest();
   }

   public String resetsForTest() {
      return this.pass.resetsForTest();
   }

   public float farMotionForTest() {
      Minecraft mc = Minecraft.getMinecraft();
      return this.pass.cameraForTest().farMotionForTest((float)mc.displayWidth / Math.max(1, mc.displayHeight));
   }

   public PostChain.Pass passForTest() {
      return this.pass;
   }

   private final class Owner implements SettingStyle {
      private final BlurStyle s;

      Owner(BlurStyle s) {
         this.s = s;
      }

      @Override
      public String styleName() {
         return this.s.displayName();
      }

      @Override
      public boolean isShown() {
         return MotionBlurModule.this.style.get() == this.s;
      }

      @Override
      public boolean isActive() {
         return this.isShown();
      }

      @Override
      public boolean isMultiple() {
         return false;
      }

      @Override
      public void show() {
         MotionBlurModule.this.style.set(this.s);
      }
   }

   public static enum Precision implements Displayable {
      AUTO("Auto", "High where the graphics card supports it, else Standard"),
      HIGH("High", "16-bit float history: faint trails fade smoothly, Natural Light Blending works"),
      STANDARD("Standard", "8-bit history: less memory; faint trails fade in small steps");

      private final String name;
      private final String description;

      private Precision(String name, String description) {
         this.name = name;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.name;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   public static enum Quality implements Displayable {
      LOW("Low", "6 samples: the fastest"),
      MEDIUM("Medium", "10 samples"),
      HIGH("High", "16 samples: smooth streaks"),
      ULTRA("Ultra", "24 samples: the smoothest, for screenshots and recordings");

      private final String name;
      private final String description;

      private Quality(String name, String description) {
         this.name = name;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.name;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   public static enum TrailLook implements Displayable {
      SMOOTH("Smooth", "Old frames fade evenly"),
      PHOSPHOR("Phosphor", "Only the bright parts of old frames linger, like the old Super Secret Settings shader");

      private final String name;
      private final String description;

      private TrailLook(String name, String description) {
         this.name = name;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.name;
      }

      @Override
      public String description() {
         return this.description;
      }
   }
}
