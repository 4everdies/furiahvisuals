package meowtils.furiahvisuals.module.trails;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.Combat;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.core.PlayerFilter;
import meowtils.furiahvisuals.core.TestDummy;
import meowtils.furiahvisuals.fx.FxParticles;
import meowtils.furiahvisuals.fx.Rng;
import meowtils.furiahvisuals.fx.kit.Sketch;
import meowtils.furiahvisuals.fx.look.WorldLight;
import meowtils.furiahvisuals.fx.pass.FxPass;
import meowtils.furiahvisuals.fx.pass.FxPasses;
import meowtils.furiahvisuals.fx.pass.PassItem;
import meowtils.furiahvisuals.fx.style.Signature;
import meowtils.furiahvisuals.fx.trail.PathRecorder;
import meowtils.furiahvisuals.fx.trail.TrailAnchor;
import meowtils.furiahvisuals.fx.trail.TrailShape;
import meowtils.furiahvisuals.gui.FuriahScreen;
import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.gui.widget.SectionLinkWidget;
import meowtils.furiahvisuals.gui.widget.SignatureColorsWidget;
import meowtils.furiahvisuals.gui.widget.StartingLookWidget;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.trails.styles.DoodleLineStyle;
import meowtils.furiahvisuals.module.trails.styles.SmokeStyle;
import meowtils.furiahvisuals.module.weapons.WeaponsModule;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;

public final class TrailsModule extends Module {
   private static final double REDUCED_BEYOND = 16.0;
   private static final int MAX_TRACKED = 48;
   private static final double TEST_SECONDS = 5.0;
   private static final float HEAD_IN = 0.15F;
   private static final float HEAD_OUT = 0.4F;
   private static final float FADE_OUT_TIME = 0.4F;
   private static final float RETRACT_TIME = 0.5F;
   public final ModeSetting<TrailStyle> style;
   public final BooleanSetting combine;
   private final TrailStyleBase[] styles;
   public final BooleanSetting moveWalking;
   public final BooleanSetting moveSprinting;
   public final BooleanSetting moveAir;
   public final BooleanSetting moveFlying;
   public final BooleanSetting moveRiding;
   public final BooleanSetting moveSneaking;
   public final BooleanSetting moveSwimming;
   final BooleanSetting[] movement;
   public final SliderSetting minSpeed;
   public final SliderSetting sneakDim;
   public final BooleanSetting firstPerson;
   public final BooleanSetting calm;
   public final SliderSetting calmOpacity;
   public final SliderSetting nearFade;
   public final SliderSetting sampleSpacing;
   public final SliderSetting pieceCap;
   public final ModeSetting<TrailsModule.CurveDetail> curveDetail;
   public final ModeSetting<TrailsModule.ShowFor> showFor;
   public final SliderSetting maxDistance;
   public final SliderSetting maxPlayers;
   public final BooleanSetting skipTeammates;
   public final BooleanSetting fairPlay;
   public final ModeSetting<TrailsModule.OtherColor> otherColor;
   public final ModeSetting<TrailsModule.Detail> detailOthers;
   public final BooleanSetting sprintBurst;
   public final BooleanSetting bubbles;
   public final ModeSetting<TrailsModule.Lighting> lighting;
   public final WorldLight worldLight;
   private final FxParticles extraPieces = new FxParticles(256);
   private static final int RINGS = 32;
   private final double[] ringX = new double[32];
   private final double[] ringY = new double[32];
   private final double[] ringZ = new double[32];
   private final float[] ringAge = new float[32];
   private final int[] ringColor = new int[32];
   private int ringCount;
   private final Rng extraRng = new Rng();
   private PlayerTrail self;
   private final PlayerTrail[] others = new PlayerTrail[48];
   private int otherCount;
   private final EntityPlayer[] picked = new EntityPlayer[32];
   private final double[] pickedDistance = new double[32];
   private int pickedCount;
   private long frames;
   private World lastWorld;
   private long testAt;
   private double testUntil = Double.NEGATIVE_INFINITY;
   private final TrailPreviews previews = new TrailPreviews(this);
   private final TrailShape shape = new TrailShape();
   private final TrailFrame frame = new TrailFrame();
   private final double[] point = new double[3];
   private final boolean[] ran = new boolean[TrailStyle.values().length];
   public static final int MAX_COMBINED = 3;
   private final TrailStyleBase[] running = new TrailStyleBase[3];
   private int runningCount;
   private long updates;
   private float calmNow = 1.0F;
   private float renderMillis;
   private int drawnPoints;
   private float longestSegment;
   private float headGap;
   private final float[] longestWhere = new float[7];
   private double lowest;
   private double highest;
   private final Module.StartingLooks looks = new Module.StartingLooks() {
      @Override
      public List<SliderSetting> targets() {
         return TrailsModule.this.getSelectedStyle().lookTargets();
      }

      @Override
      public double value(SliderSetting slider, int look) {
         return StartingLookWidget.value(slider, look);
      }

      @Override
      public String tooltip(int look) {
         return look == 0
            ? "Thinner, shorter, softer: the style's size, length, strength and motion a third of the way down (colors stay as they are)"
            : (
               look == 1
                  ? "The style's size, length, strength and motion at their defaults (colors stay as they are)"
                  : "Bigger, longer, brighter: the style's size, length, strength and motion a third of the way up (colors stay as they are)"
            );
      }

      @Override
      public String after() {
         return "style";
      }

      @Override
      public String rowTooltip() {
         return "A starting point for the picked style's size, length, strength and motion; colors are never changed";
      }
   };
   private boolean prepared;
   private boolean selfHidden;
   private double preparedNow;
   private float preparedMillis;
   private final FxPasses.Clock passClock = new FxPasses.Clock();
   private FxPass drawingPass;
   private TrailsModule.TrailPassItem[] passItems = new TrailsModule.TrailPassItem[8];
   private int passItemCount;
   private TrailsModule.TrailPassItem shapeItem;
   private long frameNumber;

   public TrailsModule() {
      super("trails", "Trails", "Effects that follow you as you move: glowing ribbons, comets and more.", false);
      final TrailsModule self = this;
      this.add(
         (new WidgetSetting(
               "preview",
               "Preview",
               "A figure running a figure-eight with the styles that run: Walk, Sprint, Jump, Sneak or Stop; the camera from the side, from behind or circling (drag it); on a dark backdrop or by day."
            ) {
               @Override
               public Widget createWidget() {
                  return new TrailPreviewWidget(self);
               }
            })
            .<WidgetSetting>keywords(new String[]{"preview", "walk", "sprint", "jump", "sneak", "stop", "camera", "orbit", "dark", "day", "backdrop"})
            .group("Style")
      );
      this.style = this.add(new TrailStyleSetting(this).group("Style"));
      TrailStyle[] types = TrailStyle.values();
      this.styles = new TrailStyleBase[types.length];

      for (TrailStyle type : types) {
         TrailStyleBase s;
         this.styles[type.ordinal()] = s = type.create(this);

         for (Setting setting : s.getSettings()) {
            this.add(setting);
         }
      }

      this.combine = this.add(
         new BooleanSetting(
               "combine",
               "Combine Styles",
               "Run up to 3 styles at once (e.g. Ribbon and Comet): each card gets its own on/off pill. Clicking a card still picks whose settings show below.",
               false
            )
            .group("Style")
      );
      this.combine.addListener(new Runnable() {
         @Override
         public void run() {
            if (TrailsModule.this.combine.get() && TrailsModule.this.combinedOn() == 0) {
               TrailsModule.this.getSelectedStyle().on.set(true);
            }
         }
      });
      this.otherColor = this.add(
         new ModeSetting<>(
               "otherColor",
               "Other Players' Color",
               "Same as mine, or each player's scoreboard team color (Bed Wars teams, say); yours when they have no team.",
               TrailsModule.OtherColor.class,
               TrailsModule.OtherColor.SAME
            )
            .<ModeSetting>group("Colors")
            .keywords(new String[]{"team", "teams", "bed wars", "bedwars", "others"})
      );
      this.showFor = this.add(
         new ModeSetting<>(
               "showFor", "Show For", "Whose trails show: yours, everyone's, or only other players'.", TrailsModule.ShowFor.class, TrailsModule.ShowFor.ONLY_ME
            )
            .<ModeSetting>group("When it shows")
            .keywords(new String[]{"others", "other players", "everyone", "all players"})
      );
      Supplier<String> onlyMe = new Supplier<String>() {
         public String get() {
            return TrailsModule.this.showFor.get() == TrailsModule.ShowFor.ONLY_ME ? "Show For is Only me" : null;
         }
      };
      this.maxDistance = this.add(
         new SliderSetting("maxDistance", "Max Distance for Other Players", "Other players' trails show only this close to you.", 8.0, 64.0, 32.0, 1.0)
            .unit(" blocks")
            .<SliderSetting>group("When it shows")
            .unavailableWhen(onlyMe)
      );
      this.skipTeammates = this.add(
         new BooleanSetting("skipTeammates", "Skip Teammates", "No trails for your teammates (the same team rule as Target Aura's).", false)
            .<BooleanSetting>group("When it shows")
            .unavailableWhen(onlyMe)
      );
      this.fairPlay = this.add(
         new BooleanSetting(
               "fairPlay",
               "Fair Play",
               "Other players' trails grow only while you can see them: half a second after one goes behind cover, their trail stops and fades, so it never shows where they went.",
               true
            )
            .<BooleanSetting>group("When it shows")
            .<BooleanSetting>unavailableWhen(onlyMe)
            .keywords(new String[]{"cover", "walls", "line of sight", "fair"})
      );
      this.add(
         (new WidgetSetting(
               "movement",
               "Movement",
               "The ways of moving your trail follows: Walking, Sprinting, In the air, Flying, Riding (on), Sneaking and Swimming (off)."
            ) {
               @Override
               public Widget createWidget() {
                  return new MovementWidget(self);
               }
            })
            .<WidgetSetting>keywords(new String[]{"walking", "sprinting", "jumping", "air", "flying", "riding", "horse", "sneaking", "swimming", "water"})
            .group("When it shows")
      );
      this.moveWalking = this.add(move("walking", "Walking", "The trail grows while you walk.", true));
      this.moveSprinting = this.add(move("sprinting", "Sprinting", "The trail grows while you sprint.", true));
      this.moveAir = this.add(move("air", "In the Air", "The trail grows while you jump or fall (it draws each jump's arc).", true));
      this.moveFlying = this.add(move("flying", "Flying", "The trail grows while you fly (creative). Other players' flight counts as In the air.", true));
      this.moveRiding = this.add(move("riding", "Riding", "The trail grows while you ride (horses, minecarts, boats).", true));
      this.moveSneaking = this.add(move("sneaking", "Sneaking", "The trail grows while you sneak (dimmer, see Sneak Dim).", false));
      this.moveSwimming = this.add(move("swimming", "Swimming", "The trail grows while you swim.", false));
      this.movement = new BooleanSetting[]{
         this.moveWalking, this.moveSprinting, this.moveAir, this.moveFlying, this.moveRiding, this.moveSneaking, this.moveSwimming
      };
      this.minSpeed = this.add(
         new SliderSetting("minSpeed", "Minimum Speed", "The trail grows only above this speed (walking is 4.3 blocks/s, sprinting 5.6).", 0.0, 8.0, 1.0, 0.1)
            .unit(" blocks/s")
            .group("When it shows")
      );
      this.sneakDim = this.add(
         new SliderSetting("sneakDim", "Sneak Dim", "While you sneak, trails get this much dimmer and thinner.", 0.0, 100.0, 50.0, 1.0)
            .unit("%")
            .<SliderSetting>group("When it shows")
            .unavailableWhen(new Supplier<String>() {
               public String get() {
                  return TrailsModule.this.moveSneaking.get() ? null : "Turn on Sneaking under Movement first";
               }
            })
      );
      this.firstPerson = this.add(
         new BooleanSetting("firstPerson", "My Trail in First Person", "Off: your own trail is hidden in first person (it still shows in F5).", true)
            .group("When it shows")
      );
      this.calm = this.add(
         new BooleanSetting(
               "calm",
               "Calm in Combat",
               "While you fight (Target Aura has a target, or you hit someone or were hit in the last 5 s), trails fade down, then come back afterwards.",
               false
            )
            .group("When it shows")
      );
      this.calmOpacity = this.add(
         new SliderSetting("calmOpacity", "Opacity in Combat", "How much of the trails stays while you fight.", 0.0, 100.0, 25.0, 5.0)
            .unit("%")
            .<SliderSetting>group("When it shows")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return TrailsModule.this.calm.get();
               }
            })
      );
      this.sprintBurst = this.add(
         new BooleanSetting("sprintBurst", "Sprint Burst", "A soft puff and a thin ring at the feet when sprinting starts (at most once a second).", false)
            .<BooleanSetting>group("Extras")
            .keywords(new String[]{"sprint", "puff", "ring", "dash"})
      );
      this.bubbles = this.add(
         new BooleanSetting("bubbles", "Bubbles Underwater", "Small bubbles rise from the trail's anchor while it is underwater.", false)
            .<BooleanSetting>group("Extras")
            .keywords(new String[]{"water", "swim", "underwater", "bubble"})
      );
      this.add(
         (new WidgetSetting("swingTrails", "Weapon Swing Trails", "Swing trails live in Weapons (Motion › Swing Trail), one home per setting: this goes there.") {
               @Override
               public Widget createWidget() {
                  return new SectionLinkWidget(
                     "Weapon Swing Trails", "in Weapons › Motion", "Swing trails live in Weapons: opens Weapons with Swing Trail shown", new Runnable() {
                        @Override
                        public void run() {
                           TrailsModule.this.openSwingTrails();
                        }
                     }
                  );
               }
            })
            .<WidgetSetting>group("Extras")
            .keywords(new String[]{"weapon", "swing", "sword", "streak", "swing trail"})
      );
      this.lighting = this.add(
         new ModeSetting<>(
               "lighting",
               "Lighting",
               "Emissive: trails glow the same everywhere. Lit by the world: darker in dark places, like the blocks around them.",
               TrailsModule.Lighting.class,
               TrailsModule.Lighting.EMISSIVE
            )
            .<ModeSetting>group("Advanced")
            .keywords(new String[]{"light", "dark", "emissive", "glow"})
      );
      this.worldLight = new WorldLight(
         "Lighting (above) is for the glowing parts; this is for the solid ones: materials (gold, brass, chrome), paper, glass and drawn strokes take the light where the trail is (block light and daylight), so a gold chain doesn't glow in a dark cave.",
         "Rose Path, Brushstroke, Wraith, and Doodle Line with Pencil",
         true
      );
      this.add(this.worldLight.match);
      this.add(this.worldLight.minimum);
      this.add(this.worldLight.night);
      this.add(SignatureColorsWidget.setting(new SignatureColorsWidget.Source() {
         @Override
         public Signature selectedSignature() {
            return TrailsModule.this.getSelectedStyle().signature();
         }

         @Override
         public String selectedStyleName() {
            return TrailsModule.this.style.get().displayName();
         }
      }));
      this.nearFade = this.add(
         new SliderSetting(
               "nearFade",
               "Near-Camera Fade",
               "Anything of a trail closer to the camera than this fades out, so trails never block your view.",
               0.5,
               3.0,
               1.2,
               0.1
            )
            .unit(" blocks")
            .group("Advanced")
      );
      this.sampleSpacing = this.add(
         new SliderSetting(
               "sampleSpacing",
               "Sample Spacing",
               "How far apart the path's points are recorded: smaller follows every turn, larger is lighter.",
               0.05,
               0.5,
               0.15,
               0.01
            )
            .unit(" blocks")
            .group("Advanced")
      );
      this.curveDetail = this.add(
         new ModeSetting<>(
               "curveDetail",
               "Curve Detail",
               "How finely curves are drawn (more near the camera and in sharp turns).",
               TrailsModule.CurveDetail.class,
               TrailsModule.CurveDetail.MEDIUM
            )
            .group("Advanced")
      );
      this.maxPlayers = this.add(
         new SliderSetting(
               "maxPlayers",
               "Max Players with Trails",
               "The most players with trails at once, nearest first; you always count, and come first.",
               1.0,
               32.0,
               12.0,
               1.0
            )
            .<SliderSetting>group("Advanced")
            .unavailableWhen(onlyMe)
      );
      this.detailOthers = this.add(
         new ModeSetting<>(
               "detailOthers",
               "Detail for Others",
               "How much of other players' trails is drawn: far ones with fewer curve pieces, half the pieces and no distortion, without Afterimage and Speed Lines.",
               TrailsModule.Detail.class,
               TrailsModule.Detail.REDUCED_FAR
            )
            .<ModeSetting>group("Advanced")
            .unavailableWhen(onlyMe)
      );
      this.pieceCap = this.add(
         new SliderSetting(
               "pieceCap", "Piece Cap", "The most pieces (sparkles, prints, puffs) alive at once, for the whole section.", 200.0, 4000.0, 1500.0, 50.0
            )
            .group("Advanced")
      );
   }

   void openSwingTrails() {
      Module w = FuriahsVisuals.instance.getModules().get("weapons");
      String id = w instanceof WeaponsModule ? ((WeaponsModule)w).swingTrailSettingId() : null;
      FuriahScreen.goTo("weapons", id);
   }

   private static BooleanSetting move(String key, String name, String description, boolean on) {
      return new BooleanSetting("move." + key, name, description, on).<BooleanSetting>group("When it shows").nested();
   }

   public TrailStyleBase getStyle(TrailStyle type) {
      return this.styles[type.ordinal()];
   }

   public TrailStyleBase getSelectedStyle() {
      return this.styles[this.style.get().ordinal()];
   }

   TrailPreviews previews() {
      return this.previews;
   }

   boolean drawsDark(TrailStyleBase s) {
      if (s == null) {
         return false;
      } else {
         switch (s.getType()) {
            case ROSE_PATH:
            case BRUSHSTROKE:
            case WRAITH:
               return true;
            case DOODLE_LINE:
               return s instanceof DoodleLineStyle && ((DoodleLineStyle)s).tool.get() == Sketch.Tool.PENCIL;
            case SMOKE:
               return s instanceof SmokeStyle && ((SmokeStyle)s).variant.get() == SmokeStyle.Variant.INK;
            default:
               return false;
         }
      }
   }

   public int combinedOn() {
      int n = 0;

      for (TrailStyleBase s : this.styles) {
         if (s.on.get()) {
            n++;
         }
      }

      return n;
   }

   public boolean togglePill(TrailStyle type) {
      TrailStyleBase s = this.styles[type.ordinal()];
      if (s.on.get()) {
         if (this.combinedOn() <= 1) {
            return false;
         } else {
            s.on.set(false);
            return true;
         }
      } else if (this.combinedOn() >= 3) {
         return false;
      } else {
         s.on.set(true);
         return true;
      }
   }

   @Override
   public int getAccentColor(double time) {
      return this.getSelectedStyle().color.getDisplayColor() | 0xFF000000;
   }

   @Override
   public Module.StartingLooks startingLooks() {
      return this.looks;
   }

   private void submitPass(PlayerTrail trail, TrailStyleBase style) {
      if (this.passItemCount == this.passItems.length) {
         this.passItems = Arrays.copyOf(this.passItems, this.passItemCount * 2);
      }

      TrailsModule.TrailPassItem it;
      if ((it = this.passItems[this.passItemCount]) == null) {
         it = this.passItems[this.passItemCount] = new TrailsModule.TrailPassItem();
      }

      this.passItemCount++;
      it.trail = trail;
      it.style = style;
      FxPasses.submit(it, this.passClock);
   }

   @Override
   public void onPrepareWorld(float partialTicks) {
      this.frameNumber++;
      this.prepared = false;
      Minecraft mc = Minecraft.getMinecraft();
      EntityPlayerSP me = mc.thePlayer;
      if (me != null && mc.theWorld != null) {
         long start = System.nanoTime();
         double now = GameClock.seconds();
         float dt = Math.min(0.1F, GameClock.dt());
         this.collectRunning();

         for (int i2 = 0; i2 < this.styles.length; i2++) {
            boolean runs = this.isRunning(this.styles[i2]);
            if (this.ran[i2] && !runs) {
               this.frame.clearPieces(this.styles[i2].getType());
            }

            this.ran[i2] = runs;
         }

         float calmTarget = this.calm.get() && Combat.fighting() ? this.calmOpacity.getFloat() / 100.0F : 1.0F;
         this.calmNow = approach(this.calmNow, calmTarget, dt / 0.5F);
         if (mc.theWorld != this.lastWorld) {
            this.lastWorld = mc.theWorld;
            this.otherCount = 0;
         }

         this.frames++;
         if (dt > 0.0F) {
            this.updates++;
         }

         this.setUpFrame(mc, partialTicks, now, dt);
         this.startTestRun(mc, now);
         TrailsModule.ShowFor showing = this.showFor.get();
         PlayerTrail p = this.self;
         if (p == null || p.entityId != me.getEntityId()) {
            p = this.self = new PlayerTrail(me.getEntityId(), true);
         }

         if (dt > 0.0F) {
            this.update(p, me, partialTicks, now, dt, showing != TrailsModule.ShowFor.OTHERS, 0.3F);
         } else {
            p.pose.from(me, partialTicks, false);
         }

         this.pickOthers(mc, me, showing, now);

         for (int i = 0; i < this.pickedCount; i++) {
            EntityPlayer e = this.picked[i];
            PlayerTrail o2 = this.other(e.getEntityId(), now);
            o2.pickedFrame = this.frames;
            TrailsModule.Detail detail = this.detailOthers.get();
            o2.reduced = detail == TrailsModule.Detail.ALWAYS || detail == TrailsModule.Detail.REDUCED_FAR && this.pickedDistance[i] > 256.0;
            o2.teamColor = this.otherColor.get() == TrailsModule.OtherColor.TEAM ? PlayerFilter.teamColor(e) : 0;
            boolean inSight = this.inSight(mc, e, o2, now);
            if (dt > 0.0F) {
               this.update(o2, e, partialTicks, now, dt, inSight, inSight ? 0.3F : 0.5F);
            } else {
               o2.pose.from(e, partialTicks, true);
            }
         }

         int var19 = 0;

         while (var19 < this.otherCount) {
            PlayerTrail o = this.others[var19];
            if (o.pickedFrame != this.frames) {
               if (dt > 0.0F) {
                  o.fadeTime = 0.3F;
                  this.advance(o, false, 0.0, false, o.state, now, dt, this.running, this.runningCount, this.updates);
               }

               if (o.shown <= 0.0F) {
                  this.others[var19] = this.others[--this.otherCount];
                  this.others[this.otherCount] = null;
                  continue;
               }
            }

            var19++;
         }

         this.worldLight.frame(mc.theWorld, partialTicks);
         p.light = this.frame.lightAt(p.pose.x, p.pose.y + 0.9, p.pose.z);
         this.lightOf(mc.theWorld, p);

         for (int var20 = 0; var20 < this.otherCount; var20++) {
            PlayerTrail o = this.others[var20];
            o.light = this.frame.lightAt(o.pose.x, o.pose.y + 0.9, o.pose.z);
            this.lightOf(mc.theWorld, o);
         }

         boolean hidden = !this.firstPerson.get() && mc.gameSettings.thirdPersonView == 0 && mc.getRenderViewEntity() == me;
         this.drawnPoints = 0;
         this.longestSegment = 0.0F;
         this.headGap = 0.0F;
         this.lowest = Double.MAX_VALUE;
         this.highest = -Double.MAX_VALUE;
         if (this.hasSomething()) {
            this.selfHidden = hidden;
            this.preparedNow = now;
            this.prepared = true;
            this.passItemCount = 0;

            for (int k = 0; k < this.runningCount; k++) {
               TrailStyleBase st = this.running[k];
               if (st.passes() != 0 && !st.passFailed) {
                  st.update(this.frame);
                  if (!hidden) {
                     this.submitPass(p, st);
                  }

                  for (int i3 = 0; i3 < this.otherCount; i3++) {
                     this.submitPass(this.others[i3], st);
                  }

                  this.submitPass(null, st);
               }
            }

            this.preparedMillis = (float)(System.nanoTime() - start) / 1000000.0F;
         }
      }
   }

   private void lightOf(World w, PlayerTrail t) {
      float level = this.worldLight.level(w, t.pose.x, t.pose.y + 0.9, t.pose.z);
      t.matLight = this.worldLight.light(level);
      t.night = this.worldLight.night(level);
   }

   @Override
   public void onRenderWorld(float partialTicks) {
      float passMillis = this.passClock.takeMillis();
      if (this.prepared) {
         this.prepared = false;
         long start = System.nanoTime();
         double now = this.preparedNow;
         PlayerTrail p = this.self;
         Textures.ensureLoaded();
         RenderState.begin();

         try {
            for (int k = 0; k < this.runningCount; k++) {
               if (this.running[k].passes() == 0) {
                  this.running[k].update(this.frame);
                  if (!this.selfHidden) {
                     this.draw(this.frame, p, this.running[k], now, this.calmNow);
                  }

                  for (int i = 0; i < this.otherCount; i++) {
                     this.draw(this.frame, this.others[i], this.running[k], now, this.calmNow);
                  }

                  this.running[k].renderPieces(this.frame);
                  RenderState.additive();
               }
            }

            this.extraPieces.update(this.frame.dt);
            this.renderExtras(this.frame);
         } finally {
            RenderState.end();
         }

         float var13 = this.preparedMillis + (float)(System.nanoTime() - start) / 1000000.0F + passMillis;
         this.renderMillis = this.renderMillis == 0.0F ? var13 : this.renderMillis + (var13 - this.renderMillis) * 0.05F;
      }
   }

   private PlayerTrail other(int entityId, double now) {
      for (int i = 0; i < this.otherCount; i++) {
         if (this.others[i].entityId == entityId) {
            return this.others[i];
         }
      }

      if (this.otherCount == this.others.length) {
         int weakest = 0;

         for (int ix = 1; ix < this.otherCount; ix++) {
            if (this.others[ix].shown < this.others[weakest].shown) {
               weakest = ix;
            }
         }

         this.others[weakest] = this.others[--this.otherCount];
      }

      PlayerTrail o = new PlayerTrail(entityId, false);
      o.lastSeen = now;
      this.others[this.otherCount++] = o;
      return o;
   }

   private void pickOthers(Minecraft mc, EntityPlayerSP me, TrailsModule.ShowFor showing, double now) {
      this.pickedCount = 0;
      EntityOtherPlayerMP dummy = now < this.testUntil ? TestDummy.get() : null;
      if (showing != TrailsModule.ShowFor.ONLY_ME || dummy != null) {
         int slots = Math.min(this.picked.length, this.maxPlayers.getInt() - (showing != TrailsModule.ShowFor.OTHERS ? 1 : 0));
         if (dummy != null) {
            this.picked[0] = dummy;
            this.pickedDistance[0] = dummy.getDistanceSqToEntity(me);
            this.pickedCount = 1;
            slots = Math.max(1, slots);
            if (showing == TrailsModule.ShowFor.ONLY_ME) {
               return;
            }
         }

         double max = this.maxDistance.getFloat();
         double maxSq = max * max;
         boolean skip = this.skipTeammates.get();
         List players = mc.theWorld.playerEntities;

         for (int i = 0; i < players.size(); i++) {
            EntityPlayer e = (EntityPlayer)players.get(i);
            double d;
            if (e != me && e != dummy && !PlayerFilter.isNpc(e) && !((d = e.getDistanceSqToEntity(me)) > maxSq) && (!skip || !PlayerFilter.isTeammate(mc, e))) {
               int at = this.pickedCount;

               while (at > 0 && this.pickedDistance[at - 1] > d && this.picked[at - 1] != dummy) {
                  at--;
               }

               if (at < slots) {
                  for (int j = Math.min(this.pickedCount, slots - 1); j > at; j--) {
                     this.picked[j] = this.picked[j - 1];
                     this.pickedDistance[j] = this.pickedDistance[j - 1];
                  }

                  this.picked[at] = e;
                  this.pickedDistance[at] = d;
                  this.pickedCount = Math.min(slots, this.pickedCount + 1);
               }
            }
         }
      }
   }

   private boolean inSight(Minecraft mc, EntityPlayer e, PlayerTrail o, double now) {
      if (!this.fairPlay.get()) {
         o.lastSeen = now;
         return true;
      } else {
         if (Double.isNaN(o.lastSeen)) {
            o.lastSeen = now;
         }

         if (now >= o.lookAgain) {
            o.lookAgain = now + 0.25 + (o.entityId & 7) * 0.003;
            Vec3 eye = new Vec3(this.frame.eyeX, this.frame.eyeY, this.frame.eyeZ);
            if (clear(mc.theWorld, eye, e.posX, e.posY + e.getEyeHeight(), e.posZ)
               || clear(mc.theWorld, eye, e.posX, e.posY + 0.1, e.posZ)) {
               o.lastSeen = now;
            }
         }

         return now - o.lastSeen <= 0.5;
      }
   }

   private static boolean clear(World w, Vec3 from, double x, double y, double z) {
      MovingObjectPosition hit = w.rayTraceBlocks(from, new Vec3(x, y, z), false, true, false);
      return hit == null || hit.typeOfHit != MovingObjectType.BLOCK;
   }

   private void collectRunning() {
      this.runningCount = this.runningStyles(this.running);
   }

   private boolean isRunning(TrailStyleBase s) {
      for (int k = 0; k < this.runningCount; k++) {
         if (this.running[k] == s) {
            return true;
         }
      }

      return false;
   }

   public int runningStyles(TrailStyleBase[] out) {
      int n = 0;
      if (this.combine.get()) {
         for (TrailStyleBase s : this.styles) {
            if (s.on.get() && n < 3) {
               out[n++] = s;
            }
         }
      }

      if (n == 0) {
         out[n++] = this.getSelectedStyle();
      }

      return n;
   }

   public int runningCount() {
      return this.runningCount;
   }

   public TrailStyleBase running(int k) {
      return this.running[k];
   }

   private boolean hasSomething() {
      if (this.extraPieces.size() <= 0 && this.ringCount <= 0) {
         for (int k = 0; k < this.runningCount; k++) {
            TrailStyleBase st = this.running[k];
            if (st.pieceCount(this.frame) > 0 || hasPath(this.self, st)) {
               return true;
            }

            for (int i = 0; i < this.otherCount; i++) {
               if (hasPath(this.others[i], st)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean hasPath(PlayerTrail p, TrailStyleBase st) {
      if (p == null) {
         return false;
      } else {
         TrailAnchor a = st.anchor();

         for (int side = 0; side < a.sides(); side++) {
            PathRecorder r = p.paths[a.ordinal()][side];
            if (r != null && r.count() > 0) {
               return true;
            }
         }

         return false;
      }
   }

   private void setUpFrame(Minecraft mc, float partialTicks, double now, float dt) {
      TrailFrame f = this.frame;
      f.cam.captureWorld();
      Vfx.setCamera(f.cam);
      f.viewX = mc.getRenderManager().viewerPosX;
      f.viewY = mc.getRenderManager().viewerPosY;
      f.viewZ = mc.getRenderManager().viewerPosZ;
      f.eyeX = f.viewX + f.cam.posX;
      f.eyeY = f.viewY + f.cam.posY;
      f.eyeZ = f.viewZ + f.cam.posZ;
      f.partialTicks = partialTicks;
      f.dt = dt;
      f.now = now;
      f.colorTime = ColorSetting.clock();
      f.preview = false;
      f.world = mc.theWorld;
      f.nearOuter = this.nearFade.getFloat();
      f.nearInner = f.nearOuter * 0.4F;
      f.fog = true;
      f.lit = this.lighting.get() == TrailsModule.Lighting.LIT;
      f.updateLight();
      double scale = f.cam.projectionScaleY > 0.0 ? f.cam.projectionScaleY : 1.0 / Math.tan(Math.toRadians(35.0));
      f.pixelsPerBlock = (float)(mc.displayHeight * scale * 0.5);
   }

   private void update(PlayerTrail p, EntityPlayer e, float partialTicks, double now, float dt, boolean allowed, float fadeTime) {
      p.pose.from(e, partialTicks, !p.self);
      p.entity = e;
      if (e instanceof AbstractClientPlayer) {
         AbstractClientPlayer a = (AbstractClientPlayer)e;
         p.skin = a.getLocationSkin();
         p.slim = "slim".equals(a.getSkinType());
      }

      boolean canShow = allowed && !e.isDead && e.getHealth() > 0.0F && !e.isInvisible() && !e.isSpectator();
      p.fadeTime = fadeTime;
      double tx = e.posX - e.lastTickPosX;
      double ty = e.posY - e.lastTickPosY;
      double tz = e.posZ - e.lastTickPosZ;
      double horizontal = Math.sqrt(tx * tx + tz * tz);
      double perTick = Math.sqrt(horizontal * horizontal + ty * ty);
      boolean teleport = perTick > 4.0 || !p.self && horizontal > 2.0;
      boolean flying = p.self && e.capabilities.isFlying;
      int state = (e.onGround ? 1 : 0)
         | (e.isSprinting() ? 2 : 0)
         | (e.isSneaking() ? 4 : 0)
         | (e.isInWater() ? 8 : 0)
         | (flying ? 16 : 0)
         | (e.isRiding() ? 32 : 0);
      this.advance(p, canShow, perTick, teleport, state, now, dt, this.running, this.runningCount, this.updates);
      this.extras(p, canShow, now, dt);
   }

   private void extras(PlayerTrail p, boolean canShow, double now, float dt) {
      boolean sprinting = (p.state & 2) != 0;
      boolean may = canShow && (!p.self || this.firstPerson.get() || !this.firstPersonView()) && !this.piecesFull(this.frame);
      if (this.sprintBurst.get() && may && sprinting && !p.wasSprinting && now - p.burstAt >= 1.0) {
         p.burstAt = now;
         this.burst(p);
      }

      p.wasSprinting = sprinting;
      if (this.bubbles.get() && may && (p.state & 8) != 0) {
         this.bubbles(p, dt);
      } else {
         p.bubblesDue = 0.0F;
      }
   }

   private boolean firstPersonView() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc.gameSettings.thirdPersonView == 0 && mc.getRenderViewEntity() == mc.thePlayer;
   }

   private int extrasColor(PlayerTrail p) {
      TrailStyleBase st = this.running[0];
      st.tint = p.teamColor;
      st.light = this.frame.lit ? p.light : 1.0F;
      int c = st.colorAt(0.0F, 0.0F, p.speed, this.frame.colorTime, 0.0F);
      st.tint = 0;
      st.light = 1.0F;
      return c;
   }

   private void burst(PlayerTrail p) {
      int c = this.extrasColor(p);
      double x = p.pose.x;
      double y = p.pose.y;
      double z = p.pose.z;
      Rng rng = this.extraRng;
      rng.seed(p.entityId * 31 + (int)(this.frame.now * 997.0));

      for (int i = 0; i < 9; i++) {
         double a = i * Math.PI * 2.0 / 9.0 + rng.next() * 0.5;
         float sp = 0.9F + rng.next() * 0.6F;
         float cos = (float)Math.cos(a);
         float sin = (float)Math.sin(a);
         this.extraPieces
            .spawn(
               8, x + cos * 0.2, y + 0.08, z + sin * 0.2, cos * sp, 0.2F + rng.next() * 0.3F, sin * sp, 0.5F + rng.next() * 0.25F, 0.26F + rng.next() * 0.14F
            )
            .drag(3.0F)
            .end(0.55F)
            .color(ColorUtil.lerp(c, -1, 0.5F))
            .alpha(0.55F * p.shown * this.calmNow);
      }

      int n;
      if (this.ringCount < 32) {
         int n2 = this.ringCount;
         n = n2;
         this.ringCount = n2 + 1;
      } else {
         n = this.oldestRing();
      }

      this.ringX[n] = x;
      this.ringY[n] = y + 0.03;
      this.ringZ[n] = z;
      this.ringAge[n] = 0.0F;
      this.ringColor[n] = c;
   }

   private int oldestRing() {
      int oldest = 0;

      for (int i = 1; i < this.ringCount; i++) {
         if (this.ringAge[i] > this.ringAge[oldest]) {
            oldest = i;
         }
      }

      return oldest;
   }

   private void bubbles(PlayerTrail p, float dt) {
      World w = this.frame.world;
      if (w != null && this.runningCount != 0) {
         this.running[0].anchor().place(p.pose, 0, this.point);
         if (!water(w, this.point[0], this.point[1], this.point[2])) {
            p.bubblesDue = 0.0F;
         } else {
            p.bubblesDue = p.bubblesDue + dt * (p.moveSpeed > 0.5F ? 9.0F : 3.5F);
            if (!(p.bubblesDue < 1.0F)) {
               int c = ColorUtil.lerp(this.extrasColor(p), -1, 0.65F);
               double top = Math.floor(this.point[1]) + 1.0;

               for (int k = 0; k < 8 && water(w, this.point[0], top + 0.5, this.point[2]); k++) {
                  top++;
               }

               if (p.self && this.firstPersonView()) {
                  top = Math.min(top, this.frame.eyeY - 0.4);
               }

               double height;
               if ((height = top - 0.1 - this.point[1]) < 0.2) {
                  p.bubblesDue = 0.0F;
               } else {
                  Rng rng = this.extraRng;

                  while (p.bubblesDue >= 1.0F) {
                     p.bubblesDue--;
                     rng.seed(p.entityId * 17 + (int)(this.frame.now * 3001.0) + (int)(p.bubblesDue * 1000.0F));
                     float rise = 0.9F + rng.next() * 0.4F;
                     this.extraPieces
                        .spawn(
                           6,
                           this.point[0] + rng.signed() * 0.15,
                           this.point[1],
                           this.point[2] + rng.signed() * 0.15,
                           rng.signed() * 0.15F,
                           rise,
                           rng.signed() * 0.15F,
                           (float)Math.min(2.0, height / rise),
                           0.05F + rng.next() * 0.05F
                        )
                        .color(c)
                        .alpha(0.8F * p.shown * this.calmNow);
                  }
               }
            }
         }
      }
   }

   private static boolean water(World w, double x, double y, double z) {
      return w.getBlockState(new BlockPos(x, y, z)).getBlock().getMaterial() == Material.water;
   }

   private void renderExtras(TrailFrame f) {
      if (this.ringCount > 0) {
         RenderState.additive();
         int i = 0;

         while (i < this.ringCount) {
            this.ringAge[i] = this.ringAge[i] + f.dt;
            float t = this.ringAge[i] / 0.45F;
            if (t >= 1.0F) {
               int last = --this.ringCount;
               this.ringX[i] = this.ringX[last];
               this.ringY[i] = this.ringY[last];
               this.ringZ[i] = this.ringZ[last];
               this.ringAge[i] = this.ringAge[last];
               this.ringColor[i] = this.ringColor[last];
            } else {
               double r = 0.15 + 1.0 * Easing.outCubic(t);
               float a = (1.0F - t) * (1.0F - t) * f.fog(this.ringX[i], this.ringY[i], this.ringZ[i]) * this.calmNow;
               double x = this.ringX[i] - f.viewX;
               double y = this.ringY[i] - f.viewY;
               double z = this.ringZ[i] - f.viewZ;
               Vfx.flatRing(Textures.LINE_GLOW, x, y, z, r, 0.08 + 0.06 * (1.0 - t), 40, ColorUtil.mulAlpha(this.ringColor[i], a * 0.8F));
               Vfx.flatRing(Textures.LINE_CORE, x, y + 0.002, z, r, 0.025, 40, ColorUtil.mulAlpha(ColorUtil.core(this.ringColor[i]), a * 0.6F));
               i++;
            }
         }
      }

      if (this.extraPieces.size() > 0) {
         this.extraPieces.render(f.viewX, f.viewY, f.viewZ);
      }
   }

   void advance(
      PlayerTrail p, boolean canShow, double perTick, boolean teleport, int state, double now, float dt, TrailStyleBase[] styles, int count, long update
   ) {
      if (canShow && !p.canShow) {
         for (PlayerTrail.StyleState s : p.styles) {
            if (s != null) {
               s.since = now;
               s.frozenAt = Double.NaN;
               s.ghostUntil = Double.NaN;
            }
         }

         p.shown = 1.0F;
      }

      p.canShow = canShow;
      if (!canShow) {
         p.shown = Math.max(0.0F, p.shown - dt / Math.max(0.05F, p.fadeTime));
      }

      if (p.hasLast && !teleport) {
         double dx = p.pose.x - p.lastX;
         double dy = p.pose.y - p.lastY;
         double dz = p.pose.z - p.lastZ;
         double jump = Math.sqrt(dx * dx + dy * dy + dz * dz);
         teleport = jump > Math.max(4.0, Math.max(p.speed, (float)perTick * 20.0F) * dt * 3.0 + 1.0);
      }

      p.lastX = p.pose.x;
      p.lastY = p.pose.y;
      p.lastZ = p.pose.z;
      p.hasLast = true;
      if (!teleport) {
         float tickSpeed = (float)perTick * 20.0F;
         p.speed = p.speed + (tickSpeed - p.speed) * (1.0F - (float)Math.exp(-dt / 0.25F));
         p.moveSpeed = p.moveSpeed + (tickSpeed - p.moveSpeed) * (1.0F - (float)Math.exp(-dt / 0.06F));
      }

      p.state = state;
      boolean sneaking = (state & 4) != 0;
      boolean allowed = (state & 32) != 0
         ? this.moveRiding.get()
         : (
            (state & 16) != 0
               ? this.moveFlying.get()
               : (
                  (state & 8) != 0
                     ? this.moveSwimming.get()
                     : (
                        sneaking
                           ? this.moveSneaking.get()
                           : ((state & 1) == 0 ? this.moveAir.get() : ((state & 2) != 0 ? this.moveSprinting.get() : this.moveWalking.get()))
                     )
               )
         );
      float on = Math.max(0.25F, this.minSpeed.getFloat());
      float off = on - Math.min(0.25F, on * 0.1F);
      boolean fastEnough = p.growing ? p.moveSpeed >= off : p.moveSpeed >= on;
      p.growing = canShow && allowed && fastEnough && !teleport;
      p.sneak = approach(p.sneak, sneaking && this.moveSneaking.get() ? 1.0F : 0.0F, dt / 0.15F);
      double spacing = this.sampleSpacing.getFloat();
      int fedNow = 0;

      for (int k = 0; k < count; k++) {
         TrailAnchor a = styles[k].anchor();
         if ((fedNow & 1 << a.ordinal()) == 0) {
            fedNow |= 1 << a.ordinal();
            float maxAge = 0.0F;
            float maxLength = 0.0F;

            for (int j = 0; j < count; j++) {
               TrailStyleBase st = styles[j];
               if (st.anchor() == a) {
                  maxAge = Math.max(
                     maxAge, st.length.getFloat() * (st.whenStop != null && st.whenStop.get() == TrailStyleBase.WhenStop.FREEZE ? 3.6F : 2.6F) + 0.5F
                  );
                  maxLength = Math.max(maxLength, (st.maxLength != null ? st.maxLength.getFloat() : 64.0F) + 1.0F);
               }
            }

            for (int side = 0; side < a.sides(); side++) {
               a.place(p.pose, side, this.point);
               PathRecorder r = p.path(a, side);
               if (p.fed[a.ordinal()] != update - 1L) {
                  r.breakRun();
               }

               if (teleport) {
                  r.breakRun();
               } else if (p.growing) {
                  r.feed(this.point[0], this.point[1], this.point[2], now, p.speed, p.pose.bodyYaw, state, spacing, true);
               } else if (r.attached()) {
                  r.finish(this.point[0], this.point[1], this.point[2], now, p.speed, p.pose.bodyYaw, state);
               }

               r.expire(now, maxAge, maxLength, r.sinceLast(this.point[0], this.point[1], this.point[2]));
            }

            p.fed[a.ordinal()] = update;
         }
      }

      for (int var29 = 0; var29 < count; var29++) {
         this.whenStopped(p, p.style(styles[var29].getType()), styles[var29], now, dt);
      }
   }

   private void whenStopped(PlayerTrail p, PlayerTrail.StyleState s, TrailStyleBase st, double now, float dt) {
      float length = st.length.getFloat();
      boolean grow = p.growing;
      float f = s.head = grow ? Math.min(1.0F, s.head + dt / 0.15F) : Math.max(0.0F, s.head - dt / 0.4F);
      if (st.whenStop == null) {
         s.grew = grow;
         s.alpha = 1.0F;
         if (grow) {
            s.maxAge = st.lengthNow(p.speed);
         }
      } else {
         TrailStyleBase.WhenStop mode = st.whenStop.get();
         if (s.hasGhost()) {
            s.ghostAlpha = Double.isNaN(s.ghostFrozenAt) ? (s.ghostAlpha -= dt / 0.4F) : st.fade((float)((now - s.ghostFrozenAt) / Math.max(0.05F, length)));
            if (s.ghostAlpha <= 0.0F) {
               s.ghostUntil = Double.NaN;
            }
         }

         boolean started = grow && !s.grew;
         s.grew = grow;
         if (grow) {
            if (started) {
               boolean left = s.alpha > 0.01F && s.retract <= 0.0F && s.since < now;
               if (left && mode != TrailStyleBase.WhenStop.RETRACT) {
                  s.ghostSince = s.since;
                  s.ghostUntil = now;
                  s.ghostAlpha = s.alpha;
                  s.ghostFrozenAt = mode == TrailStyleBase.WhenStop.FREEZE ? s.frozenAt : Double.NaN;
                  s.ghostMaxAge = s.maxAge;
               }

               s.since = now;
               s.alpha = 1.0F;
               s.retract = 0.0F;
               s.frozenAt = Double.NaN;
            }

            s.maxAge = st.lengthNow(p.speed);
         } else {
            switch (mode) {
               case FADE:
                  s.alpha = Math.max(0.0F, s.alpha - dt / 0.4F);
                  break;
               case RETRACT:
                  if (s.retract <= 0.0F) {
                     s.retractFrom = Math.max(s.length[0], s.length[1]);
                  }

                  s.retract = Math.min(1.0F, s.retract + dt / 0.5F);
                  if (s.retract >= 1.0F) {
                     s.alpha = 0.0F;
                  }
                  break;
               default:
                  if (Double.isNaN(s.frozenAt)) {
                     s.frozenAt = now;
                  }

                  s.alpha = st.fade((float)((now - s.frozenAt) / Math.max(0.05F, length)));
            }

            if (s.alpha <= 0.0F) {
               s.since = now;
            }
         }

         if (p.shown <= 0.0F) {
            s.since = now;
            s.ghostUntil = Double.NaN;
         }
      }
   }

   void draw(TrailFrame f, PlayerTrail p, TrailStyleBase st, double now, float strength) {
      TrailAnchor a = st.anchor();
      PlayerTrail.StyleState s = p.style(st.getType());
      float common = p.shown * strength * (1.0F - this.sneakDim.getFloat() / 100.0F * p.sneak);
      if (!(common <= 0.002F)) {
         float smoothing = st.smoothing != null ? st.smoothing.getFloat() / 100.0F : 0.0F;
         int detail = Math.max(0, this.curveDetail.get().ordinal() - (p.reduced ? 1 : 0));
         float cap;
         float maxAlong = cap = st.maxLength != null ? st.maxLength.getFloat() : Float.MAX_VALUE;
         if (s.retract > 0.0F) {
            maxAlong = Math.min(maxAlong, s.retractFrom * (1.0F - Easing.inOutSine(s.retract)));
         }

         double clock = Double.isNaN(s.frozenAt) ? now : s.frozenAt;
         float alpha = s.alpha * common;
         TrailsModule.TrailPassItem kept = this.shapeItem;
         boolean reuse = false;
         if (kept != null) {
            reuse = kept.shapesFrame == this.frameNumber && kept.shapesTrail == p && kept.shapesStyle == st;
            if (!reuse) {
               kept.shapesFrame = this.frameNumber;
               kept.shapesTrail = p;
               kept.shapesStyle = st;
               Arrays.fill(kept.shapeKept, false);
            }
         }

         for (int side = 0; side < a.sides(); side++) {
            PathRecorder r = p.paths[a.ordinal()][side];
            if (r != null && r.count() != 0) {
               a.place(p.pose, side, this.point);
               if (st.whenStop == null) {
                  boolean attached = r.attached() && p.growing;
                  if (alpha > 0.002F && (attached || s.head > 0.004F)) {
                     this.shape.runs = 0;
                     this.shape.length = 0.0F;
                     this.drawSide(f, p, st, s, r, side, attached, false, alpha, s.maxAge);
                  }
               } else {
                  if (s.hasGhost() && s.ghostAlpha * common > 0.002F) {
                     int slot = side * 2 + 1;
                     if (reuse && kept.shapeKept[slot]) {
                        this.shape.restore(kept.shapes[slot]);
                     } else {
                        double ghostClock = Double.isNaN(s.ghostFrozenAt) ? now : s.ghostFrozenAt;
                        this.shape
                           .build(
                              r,
                              this.point[0],
                              this.point[1],
                              this.point[2],
                              false,
                              p.speed,
                              ghostClock,
                              s.ghostSince,
                              s.ghostUntil,
                              s.ghostMaxAge,
                              cap,
                              smoothing,
                              detail,
                              f.eyeX,
                              f.eyeY,
                              f.eyeZ
                           );
                        this.keep(kept, slot);
                     }

                     if (this.shape.runs > 0) {
                        this.drawSide(f, p, st, s, r, side, false, true, s.ghostAlpha * common, s.ghostMaxAge);
                     }
                  }

                  if (!(alpha <= 0.002F) || !(s.head <= 0.004F)) {
                     boolean attached = r.attached() && p.growing;
                     int slotx = side * 2;
                     if (reuse && kept.shapeKept[slotx]) {
                        this.shape.restore(kept.shapes[slotx]);
                     } else {
                        this.shape
                           .build(
                              r,
                              this.point[0],
                              this.point[1],
                              this.point[2],
                              attached,
                              p.speed,
                              clock,
                              s.since,
                              Double.POSITIVE_INFINITY,
                              s.maxAge,
                              maxAlong,
                              smoothing,
                              detail,
                              f.eyeX,
                              f.eyeY,
                              f.eyeZ
                           );
                        this.keep(kept, slotx);
                     }

                     s.length[side] = this.shape.length;
                     if (this.shape.runs > 0 || s.head > 0.004F) {
                        this.drawSide(f, p, st, s, r, side, attached, false, alpha, s.maxAge);
                     }
                  }
               }
            }
         }
      }
   }

   private void keep(TrailsModule.TrailPassItem kept, int slot) {
      if (kept != null) {
         if (kept.shapes[slot] == null) {
            kept.shapes[slot] = new TrailShape.Saved();
         }

         this.shape.save(kept.shapes[slot]);
         kept.shapeKept[slot] = true;
      }
   }

   void drawInPass(TrailFrame f, PlayerTrail trail, TrailStyleBase st, double clock, FxPass pass) {
      FxPass before = this.drawingPass;
      this.drawingPass = pass;

      try {
         if (trail == null) {
            st.piecesState();
            st.renderPiecesPass(f, pass);
         } else {
            this.draw(f, trail, st, clock, 1.0F);
         }
      } finally {
         this.drawingPass = before;
      }
   }

   private void drawSide(
      TrailFrame f,
      PlayerTrail p,
      TrailStyleBase st,
      PlayerTrail.StyleState s,
      PathRecorder r,
      int side,
      boolean attached,
      boolean ghost,
      float alpha,
      float maxAge
   ) {
      p.shape = this.shape;
      p.path = r;
      p.side = side;
      p.headX = this.point[0];
      p.headY = this.point[1];
      p.headZ = this.point[2];
      p.attached = attached;
      p.ghost = ghost;
      p.alpha = alpha;
      p.widthScale = 1.0F - 0.5F * this.sneakDim.getFloat() / 100.0F * p.sneak;
      p.maxAge = maxAge;
      p.style = s;
      st.tint = p.teamColor;
      st.light = f.lit ? p.light : 1.0F;
      st.matLight = f.preview ? 1.0F : p.matLight;
      st.night = f.preview ? this.worldLight.previewNight() : p.night;
      boolean firstDraw = this.drawingPass == null || (st.passes() & this.drawingPass.bit() - 1) == 0;
      if (!f.preview && p.self && firstDraw) {
         this.measure(this.shape);
         if (attached && this.shape.runs > 0) {
            int last = this.shape.runEnd[this.shape.runs - 1] - 1;
            double gx = this.shape.x[last] - this.point[0];
            double gy = this.shape.y[last] - this.point[1];
            double gz = this.shape.z[last] - this.point[2];
            this.headGap = Math.max(this.headGap, (float)Math.sqrt(gx * gx + gy * gy + gz * gz));
         }
      }

      if (this.drawingPass != null) {
         st.renderPass(p, f, this.drawingPass);
      } else {
         st.render(p, f);
      }
   }

   private void measure(TrailShape s) {
      for (int k = 0; k < s.runs; k++) {
         this.drawnPoints = this.drawnPoints + (s.runEnd[k] - s.runStart[k]);

         for (int i = s.runStart[k]; i < s.runEnd[k]; i++) {
            this.lowest = Math.min(this.lowest, s.y[i]);
            this.highest = Math.max(this.highest, s.y[i]);
            double dz;
            double dy;
            double dx;
            float d;
            if (i > s.runStart[k]
               && (d = (float)Math.sqrt((dx = s.x[i] - s.x[i - 1]) * dx + (dy = s.y[i] - s.y[i - 1]) * dy + (dz = s.z[i] - s.z[i - 1]) * dz))
                  > this.longestSegment) {
               this.longestSegment = d;
               this.longestWhere[0] = k;
               this.longestWhere[1] = s.runs;
               this.longestWhere[2] = i - s.runStart[k];
               this.longestWhere[3] = s.runEnd[k] - s.runStart[k];
               this.longestWhere[4] = s.along[i];
               this.longestWhere[5] = s.age[i];
               this.longestWhere[6] = s.age[i - 1];
            }
         }
      }
   }

   private static float approach(float v, float target, float step) {
      return v < target ? Math.min(target, v + step) : Math.max(target, v - step);
   }

   public boolean piecesFull(TrailFrame f) {
      return f.preview ? f.allPieces() >= Math.max(60, this.pieceCap.getInt() / 10) : f.allPieces() + this.extraPieces.size() >= this.pieceCap.getInt();
   }

   public int piecesForTest(TrailStyle type) {
      return this.styles[type.ordinal()].pieceCount(this.frame);
   }

   public TrailFrame worldFrameForTest() {
      return this.frame;
   }

   @Override
   public void onWorldChange() {
      this.clearAll();

      for (TrailStyleBase st : this.styles) {
         st.passFailed = false;
      }
   }

   @Override
   protected void onDisable() {
      this.clearAll();
   }

   private void clearAll() {
      this.self = null;
      Arrays.fill(this.others, null);
      this.otherCount = 0;
      this.frame.clearPieces(null);
      this.extraPieces.clear();
      this.ringCount = 0;
      this.calmNow = 1.0F;
      this.testUntil = Double.NEGATIVE_INFINITY;
   }

   @Override
   public boolean hasTestAction() {
      return true;
   }

   @Override
   public String getTestTooltip() {
      String why = this.testUnavailable();
      return why != null
         ? why
         : "Closes the menu, then your test dummy runs a circle around you for 5 seconds with the styles that run (shown whatever Show For says)";
   }

   @Override
   public String testUnavailable() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc.theWorld != null && !mc.isSingleplayer() ? "Singleplayer only: the test dummy exists only on your screen" : null;
   }

   @Override
   public void runTest() {
      this.testAt = ClientClock.millis() + 250L;
   }

   private void startTestRun(Minecraft mc, double now) {
      if (this.testAt != 0L && ClientClock.millis() >= this.testAt && mc.currentScreen == null) {
         this.testAt = 0L;
         if (TestDummy.runAround(mc, 5000L, false)) {
            this.testUntil = now + 5.0 + 1.5;
         }
      }
   }

   public PlayerTrail selfForTest() {
      return this.self;
   }

   public float selfShownForTest() {
      return this.self == null ? 0.0F : this.self.shown;
   }

   public float selfLightForTest() {
      return this.self == null ? 1.0F : this.self.light;
   }

   public String extrasForTest() {
      return this.extraPieces.size() + " pieces, " + this.ringCount + " ring(s)";
   }

   public void openSwingTrailsForTest() {
      this.openSwingTrails();
   }

   public PlayerTrail otherForTest(int entityId) {
      for (int i = 0; i < this.otherCount; i++) {
         if (this.others[i].entityId == entityId) {
            return this.others[i];
         }
      }

      return null;
   }

   public String othersForTest() {
      return this.otherCount + " followed, " + this.pickedCount + " picked";
   }

   public String otherStateForTest(int entityId) {
      PlayerTrail o = this.otherForTest(entityId);
      return o == null
         ? "no trail"
         : (o.pickedFrame == this.frames ? "picked" : "not picked")
            + ", "
            + (o.canShow ? "may show" : "may not show")
            + ", strength "
            + String.format(Locale.ROOT, "%.2f", o.shown)
            + ", growing "
            + o.growing
            + ", reduced "
            + o.reduced
            + ", team color "
            + (o.teamColor == 0 ? "none" : String.format("#%06X", o.teamColor & 16777215));
   }

   public void previewButtonForTest(boolean move, int i) {
      if (move) {
         this.previews.big().move = TrailPreview.Move.values()[i];
      } else if (i < 3) {
         this.previews.view = TrailPreviews.View.values()[i];
      } else if (i < 5) {
         this.previews.day = i == 4;
         MenuMemory.previewDay(this.getId(), this.previews.day);
      } else {
         this.previews.showOther = !this.previews.showOther;
      }
   }

   public boolean previewDayForTest() {
      return this.previews.day;
   }

   public boolean drawsDarkForTest(TrailStyle type) {
      return this.drawsDark(this.getStyle(type));
   }

   public float previewMillisForTest() {
      return this.previews.drawMillisForTest();
   }

   public int cardSamplesForTest(TrailStyle type) {
      TrailPreview sc = this.previews.cardForTest(type);
      if (sc == null) {
         return 0;
      } else {
         PathRecorder r = sc.trail.paths[this.getStyle(type).anchor().ordinal()][0];
         return r == null ? 0 : r.count();
      }
   }

   public float renderMillisForTest() {
      return this.renderMillis;
   }

   public int drawnPointsForTest() {
      return this.drawnPoints;
   }

   public float longestSegmentForTest() {
      return this.longestSegment;
   }

   public String longestWhereForTest() {
      float[] w = this.longestWhere;
      return String.format(
         Locale.ROOT,
         "run %d of %d, point %d of %d, %.2f blocks from the head, ages %.2f and %.2f s",
         (int)w[0] + 1,
         (int)w[1],
         (int)w[2],
         (int)w[3],
         w[4],
         w[5],
         w[6]
      );
   }

   public float headGapForTest() {
      return this.headGap;
   }

   public float heightForTest() {
      return this.highest >= this.lowest ? (float)(this.highest - this.lowest) : 0.0F;
   }

   public float calmForTest() {
      return this.calmNow;
   }

   public int colorForTest() {
      return ColorUtil.withAlpha(this.getSelectedStyle().color.getDisplayColor(), 255);
   }

   public static enum CurveDetail implements Displayable {
      LOW("Low", "Fewest pieces per curve"),
      MEDIUM("Medium", "Smooth curves"),
      HIGH("High", "The smoothest curves, up close");

      private final String name;
      private final String description;

      private CurveDetail(String name, String description) {
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

   public static enum Detail implements Displayable {
      FULL("Full", "Every trail in full detail"),
      REDUCED_FAR("Reduced beyond 16 blocks", "Far trails get fewer curve pieces, half the pieces and no distortion; Afterimage and Speed Lines are off there"),
      ALWAYS("Always reduced", "Other players' trails are always drawn with less");

      private final String name;
      private final String description;

      private Detail(String name, String description) {
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

   public static enum Lighting implements Displayable {
      EMISSIVE("Emissive", "Trails glow the same everywhere"),
      LIT("Lit by the world", "Trails are darker in dark places, like the blocks around them");

      private final String name;
      private final String description;

      private Lighting(String name, String description) {
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

   public static enum OtherColor implements Displayable {
      SAME("Same as mine", "Other players' trails look like yours"),
      TEAM("Team color", "Each player's trail takes their scoreboard team's color (yours when they have none)");

      private final String name;
      private final String description;

      private OtherColor(String name, String description) {
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

   public static enum ShowFor implements Displayable {
      ONLY_ME("Only me", "Only your own trail"),
      ALL("All players", "Yours and the players' around you"),
      OTHERS("Only others", "The players' around you, not yours");

      private final String name;
      private final String description;

      private ShowFor(String name, String description) {
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

   private final class TrailPassItem implements PassItem {
      PlayerTrail trail;
      TrailStyleBase style;
      final TrailShape.Saved[] shapes = new TrailShape.Saved[4];
      final boolean[] shapeKept = new boolean[4];
      PlayerTrail shapesTrail;
      TrailStyleBase shapesStyle;
      long shapesFrame = -1L;

      private TrailPassItem() {
      }

      @Override
      public int passes() {
         return this.style.passes();
      }

      @Override
      public void drawPass(FxPass pass) {
         TrailsModule.this.drawingPass = pass;
         TrailsModule.this.shapeItem = Integer.bitCount(this.style.passes()) > 1 ? this : null;

         try {
            if (this.trail == null) {
               this.style.piecesState();
               this.style.renderPiecesPass(TrailsModule.this.frame, pass);
            } else {
               TrailsModule.this.draw(TrailsModule.this.frame, this.trail, this.style, TrailsModule.this.preparedNow, TrailsModule.this.calmNow);
            }
         } finally {
            TrailsModule.this.drawingPass = null;
            TrailsModule.this.shapeItem = null;
         }
      }

      @Override
      public void passFailed() {
         this.style.passFailed = true;
      }

      @Override
      public double passDistanceSq() {
         if (this.trail == null) {
            return 0.0;
         } else {
            double dx = this.trail.pose.x - TrailsModule.this.frame.eyeX;
            double dy = this.trail.pose.y + 0.9 - TrailsModule.this.frame.eyeY;
            double dz = this.trail.pose.z - TrailsModule.this.frame.eyeZ;
            return dx * dx + dy * dy + dz * dz;
         }
      }
   }
}
