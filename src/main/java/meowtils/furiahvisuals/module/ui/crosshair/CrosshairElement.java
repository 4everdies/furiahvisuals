package meowtils.furiahvisuals.module.ui.crosshair;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.core.HitConfirmer;
import meowtils.furiahvisuals.gui.FuriahScreen;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.hud.HudGl;
import meowtils.furiahvisuals.hud.ScreenProjection;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.hit.HitMeasurer;
import meowtils.furiahvisuals.module.kill.KillContext;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.module.ui.UiElement;
import meowtils.furiahvisuals.module.ui.UiModule;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderEye;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class CrosshairElement extends UiElement {
   static final int MAIN = 0;
   static final int AIMING = 1 + CrosshairElement.ItemKind.values().length;
   static final int ZOOMED = AIMING + 1;
   static final int COUNT = ZOOMED + 1;
   private static final float MORPH_SECONDS = 0.12F;
   final CrosshairDef[] defs = new CrosshairDef[COUNT];
   CrosshairDef editor;
   private int editing = 0;
   private boolean loadingEditor;
   private CrosshairElement.DefStore store;
   CrosshairProfiles profiles;
   final HitMarkers markers = new HitMarkers();
   private boolean imageNoticeDue;
   private boolean imageNoticeShown;
   private boolean imageLoadedThisLoad;
   static final String IMAGE_REMOVED = "Custom image crosshairs were removed, so yours is the Classic crosshair now. Your images are still in config/furiahsvisuals/crosshairs.";
   BooleanSetting show;
   final ModeSetting<?>[] uses = new ModeSetting[CrosshairElement.ItemKind.values().length];
   BooleanSetting aiming;
   ModeSetting<CrosshairElement.ZoomUse> zoom;
   BooleanSetting hideSprinting;
   BooleanSetting hitMarker;
   BooleanSetting killMarker;
   ModeSetting<HitMarkers.HitStyle> hitStyle;
   ModeSetting<HitMarkers.KillStyle> killStyle;
   ModeSetting<HitMarkers.Position> hitPosition;
   SliderSetting hitSize;
   SliderSetting hitThickness;
   SliderSetting hitDuration;
   SliderSetting killSize;
   SliderSetting killDuration;
   ColorSetting hitColor;
   ColorSetting critColor;
   ColorSetting killColor;
   BooleanSetting rxTarget;
   BooleanSetting rxClick;
   BooleanSetting rxBow;
   BooleanSetting rxMove;
   BooleanSetting rxLow;
   BooleanSetting rxBlock;
   ColorSetting rxTargetPlayer;
   ColorSetting rxTargetMob;
   ColorSetting rxClickColor;
   ColorSetting rxBowColor;
   ColorSetting rxMoveColor;
   ColorSetting rxLowColor;
   ColorSetting rxBlockColor;
   SliderSetting rxTargetStrength;
   SliderSetting rxClickStrength;
   SliderSetting rxBowStrength;
   SliderSetting rxMoveStrength;
   SliderSetting rxLowStrength;
   SliderSetting rxBlockStrength;
   SliderSetting rxLowHearts;
   ModeSetting<CrosshairElement.ThirdPerson> thirdPerson;
   ModeSetting<CrosshairElement.F3> f3;
   private final CrosshairState state = new CrosshairState();
   private final CrosshairRenderer.Reactions rx = new CrosshairRenderer.Reactions();
   private CrosshairDef shown;
   private CrosshairDef leaving;
   private boolean shownSet;
   private float morph = 1.0F;
   private long lastFrame;
   private boolean clickWasDown;
   private long bowFullSince;
   private float savedPx;
   private boolean drewThisFrame;
   private final Map<HitConfirmer.Attack, Boolean> crits = new IdentityHashMap<>();
   private Entity lastHitTarget;
   private long lastHitNanos;
   private String warning;
   int situationForTest = -1;
   boolean zoomForTest;
   private float moveForTest = -1.0F;
   private float clickForTest = -1.0F;
   private float targetForTest = -1.0F;
   private float lowForTest = -1.0F;
   private float blockForTest = -1.0F;
   CrosshairPreviewWidget lastPreview;
   private float morphSlowForTest = 1.0F;
   private float costMs = -1.0F;

   public void morphSlowForTest(float times) {
      this.morphSlowForTest = Math.max(1.0F, times);
   }

   public CrosshairElement(UiModule module, MiniSection section) {
      super(module, section);

      for (int i = 0; i < COUNT; i++) {
         this.defs[i] = CrosshairDef.stored(
            i == 0 ? CrosshairDef.Kind.MAIN : (i == AIMING ? CrosshairDef.Kind.AIMING : (i == ZOOMED ? CrosshairDef.Kind.ZOOMED : CrosshairDef.Kind.ITEM))
         );
      }
   }

   @Override
   public void addSettings() {
      final CrosshairElement self = this;
      String top = this.section.getName();
      this.own(
         new CrosshairElement.Row(
               "crosshair.preview", "Preview", "The crosshair live, over a background; the buttons try its reactions and markers.", new Supplier<Widget>() {
                  public Widget get() {
                     return new CrosshairPreviewWidget(CrosshairElement.this);
                  }
               }
            )
            .keywords(new String[]{"crosshair", "reticle", "preview", "test", "situation", "editing"}),
         top
      );
      this.show = this.own(
         new BooleanSetting("crosshair.show", "Show Crosshair", "Off: no crosshair at all, vanilla's included (hit markers still show).", true)
            .keywords(new String[]{"crosshair", "hide crosshair"}),
         top
      );
      this.editor = new CrosshairDef(CrosshairDef.Kind.MAIN, "crosshair.", new CrosshairStyleSetting(this));
      List<Setting> edit = this.editor.all();

      for (int i = 0; i < edit.size(); i++) {
         final int index = i;
         Setting s = edit.get(i);
         s.<Setting>notSaved().keywords(withCrosshair(s.getKeywords()));
         s.addListener(new Runnable() {
            @Override
            public void run() {
               CrosshairElement.this.editorChanged(index);
            }
         });
         this.module.addOwned(this.section, s);
         if (s == this.editor.color) {
            this.own(new CrosshairElement.Row("crosshair.shooterColors", "Shooter Colors", "One click for a classic shooter color.", new Supplier<Widget>() {
               public Widget get() {
                  return new ShooterColorsWidget(self);
               }
            }).<CrosshairElement.Row>keywords(new String[]{"green", "yellow", "cyan", "magenta", "red", "white", "hex"}).visibleWhen(this.notVanilla()), "Color");
         }
      }

      String sit = "Situational Crosshairs";

      for (final CrosshairElement.ItemKind k : CrosshairElement.ItemKind.values()) {
         ModeSetting use;
         this.uses[k.ordinal()] = use = this.own(
            new ModeSetting<>(
                  "crosshair.use." + k.id,
                  k.title,
                  k.description + ": the main crosshair, one of its own, or none.",
                  CrosshairElement.Use.class,
                  CrosshairElement.Use.MAIN
               )
               .keywords(new String[]{"situational", "per item", "held item"}),
            "Situational Crosshairs"
         );
         use.addListener(new Runnable() {
            @Override
            public void run() {
               CrosshairElement.this.useChanged(1 + k.ordinal());
            }
         });
      }

      this.aiming = this.own(
         new BooleanSetting(
               "crosshair.aiming", "Aiming Crosshair", "While you draw a bow: a crosshair of its own (a reticle with the charge ring at first).", true
            )
            .keywords(new String[]{"bow", "aim", "ads", "aim down sights"}),
         "Situational Crosshairs"
      );
      this.aiming.addListener(new Runnable() {
         @Override
         public void run() {
            CrosshairElement.this.useChanged(CrosshairElement.AIMING);
         }
      });
      this.zoom = this.own(
         new ModeSetting<>(
               "crosshair.zoom",
               "While Zoomed",
               "With OptiFine's zoom or any zoom mod (the view narrower than half of normal).",
               CrosshairElement.ZoomUse.class,
               CrosshairElement.ZoomUse.OWN
            )
            .keywords(new String[]{"zoom", "optifine", "sniper", "scope"}),
         "Situational Crosshairs"
      );
      this.zoom.addListener(new Runnable() {
         @Override
         public void run() {
            CrosshairElement.this.useChanged(CrosshairElement.ZOOMED);
         }
      });
      this.hideSprinting = this.own(
         new BooleanSetting("crosshair.hideSprinting", "Hide While Sprinting", "No crosshair while you sprint, like many shooters.", false)
            .keywords(new String[]{"sprint", "run"}),
         "Situational Crosshairs"
      );
      String hm = "Hit Markers";
      this.hitMarker = this.own(
         new BooleanSetting("crosshair.hit", "Hit Marker", "A short mark on each hit the server confirms.", true)
            .keywords(new String[]{"hitmarker", "hit marker", "hit confirm"}),
         "Hit Markers"
      );
      BooleanSupplier hitOn = on(this.hitMarker);
      this.hitStyle = this.own(
         new ModeSetting<>("crosshair.hitStyle", "Hit Style", "What a hit marker looks like.", HitMarkers.HitStyle.class, HitMarkers.HitStyle.CLASSIC_X)
            .visibleWhen(hitOn),
         "Hit Markers"
      );
      this.hitSize = this.own(
         new SliderSetting("crosshair.hitSize", "Hit Size", "How big a hit marker is (pixels at 1080p).", 2.0, 20.0, 6.0, 0.5).unit(" px").visibleWhen(hitOn),
         "Hit Markers"
      );
      this.hitThickness = this.own(
         new SliderSetting("crosshair.hitThickness", "Hit Thickness", "How thick its lines are.", 0.5, 5.0, 1.5, 0.5).unit(" px").visibleWhen(hitOn),
         "Hit Markers"
      );
      this.hitColor = this.own(
         new ColorSetting("crosshair.hitColor", "Hit Color", "A normal hit's color.", ColorSetting.Mode.STATIC, -1, -1, 1.0).visibleWhen(hitOn), "Hit Markers"
      );
      this.critColor = this.own(
         new ColorSetting(
               "crosshair.critColor", "Crit Color", "A critical hit's color (crits are a bit bigger too).", ColorSetting.Mode.STATIC, -14275, -14275, 1.0
            )
            .skipSetAll()
            .<ColorSetting>visibleWhen(hitOn)
            .keywords(new String[]{"critical", "gold"}),
         "Hit Markers"
      );
      this.hitDuration = this.own(
         new SliderSetting(
               "crosshair.hitDuration",
               "Hit Duration",
               "How long a hit marker shows; rapid hits refresh it and grow it a little (a combo).",
               80.0,
               400.0,
               150.0,
               10.0
            )
            .unit(" ms")
            .<SliderSetting>visibleWhen(hitOn)
            .keywords(new String[]{"combo"}),
         "Hit Markers"
      );
      this.hitPosition = this.own(
         new ModeSetting<>(
               "crosshair.hitPosition",
               "Hit Position",
               "Round the crosshair, or where the hit landed on the target.",
               HitMarkers.Position.class,
               HitMarkers.Position.ON_CROSSHAIR
            )
            .visibleWhen(hitOn),
         "Hit Markers"
      );
      this.killMarker = this.own(
         new BooleanSetting("crosshair.kill", "Kill Marker", "A bigger mark on your kills.", true)
            .keywords(new String[]{"kill marker", "kill confirm", "skull"}),
         "Hit Markers"
      );
      BooleanSupplier killOn = on(this.killMarker);
      this.killStyle = this.own(
         new ModeSetting<>("crosshair.killStyle", "Kill Style", "What a kill marker looks like.", HitMarkers.KillStyle.class, HitMarkers.KillStyle.RED_X)
            .visibleWhen(killOn),
         "Hit Markers"
      );
      this.killSize = this.own(
         new SliderSetting("crosshair.killSize", "Kill Size", "How big a kill marker is (pixels at 1080p).", 3.0, 24.0, 9.0, 0.5)
            .unit(" px")
            .visibleWhen(killOn),
         "Hit Markers"
      );
      this.killColor = this.own(
         new ColorSetting("crosshair.killColor", "Kill Color", "A kill marker's color.", ColorSetting.Mode.STATIC, -53200, -53200, 1.0)
            .skipSetAll()
            .visibleWhen(killOn),
         "Hit Markers"
      );
      this.killDuration = this.own(
         new SliderSetting("crosshair.killDuration", "Kill Duration", "How long a kill marker shows.", 150.0, 900.0, 450.0, 10.0)
            .unit(" ms")
            .visibleWhen(killOn),
         "Hit Markers"
      );
      String re = "Reactions";
      this.rxTarget = this.own(
         new BooleanSetting(
               "crosshair.rx.target",
               "On Target",
               "With a player or mob under it, within reach: its color changes and its shape reacts (the gap closes, brackets snap in, spinners speed up).",
               true
            )
            .keywords(new String[]{"target", "enemy", "aim"}),
         "Reactions"
      );
      BooleanSupplier targetOn = on(this.rxTarget);
      this.rxTargetPlayer = this.own(
         new ColorSetting("crosshair.rx.targetPlayer", "Player Color", "On a player.", ColorSetting.Mode.STATIC, -50373, -50373, 1.0)
            .skipSetAll()
            .visibleWhen(targetOn),
         "Reactions"
      );
      this.rxTargetMob = this.own(
         new ColorSetting("crosshair.rx.targetMob", "Mob Color", "On a mob.", ColorSetting.Mode.STATIC, -20165, -20165, 1.0).skipSetAll().visibleWhen(targetOn),
         "Reactions"
      );
      this.rxTargetStrength = this.own(strength("crosshair.rx.targetStrength", targetOn), "Reactions");
      this.rxClick = this.own(
         new BooleanSetting("crosshair.rx.click", "Click Pulse", "A small pulse on every attack click (Classic uses its Click Error instead).", true)
            .keywords(new String[]{"click", "fire", "attack"}),
         "Reactions"
      );
      BooleanSupplier clickOn = on(this.rxClick);
      this.rxClickColor = this.own(
         new ColorSetting("crosshair.rx.clickColor", "Click Color", "The pulse's color.", ColorSetting.Mode.STATIC, -1, -1, 1.0)
            .skipSetAll()
            .visibleWhen(clickOn),
         "Reactions"
      );
      this.rxClickStrength = this.own(strength("crosshair.rx.clickStrength", clickOn), "Reactions");
      this.rxBow = this.own(
         new BooleanSetting("crosshair.rx.bow", "Bow Charge", "While you draw a bow, a ring fills with the real draw power and flashes when it is full.", true)
            .keywords(new String[]{"bow", "charge", "draw"}),
         "Reactions"
      );
      BooleanSupplier bowOn = on(this.rxBow);
      this.rxBowColor = this.own(
         new ColorSetting("crosshair.rx.bowColor", "Charge Color", "The ring's color.", ColorSetting.Mode.STATIC, -1, -1, 1.0).skipSetAll().visibleWhen(bowOn),
         "Reactions"
      );
      this.rxBowStrength = this.own(strength("crosshair.rx.bowStrength", bowOn), "Reactions");
      this.rxMove = this.own(
         new BooleanSetting(
               "crosshair.rx.move", "Movement Spread", "The gap grows while you move, sprint or jump (Classic uses its Movement Error instead).", false
            )
            .keywords(new String[]{"dynamic", "spread", "movement"}),
         "Reactions"
      );
      BooleanSupplier moveOn = on(this.rxMove);
      this.rxMoveColor = this.own(
         new ColorSetting("crosshair.rx.moveColor", "Spread Color", "A tint while it spreads.", ColorSetting.Mode.STATIC, -1, -1, 1.0)
            .skipSetAll()
            .visibleWhen(moveOn),
         "Reactions"
      );
      this.rxMoveStrength = this.own(strength("crosshair.rx.moveStrength", moveOn), "Reactions");
      this.rxLow = this.own(
         new BooleanSetting("crosshair.rx.low", "Low Health", "Tints red and pulses at low health.", false)
            .keywords(new String[]{"health", "hearts", "danger"}),
         "Reactions"
      );
      BooleanSupplier lowOn = on(this.rxLow);
      this.rxLowHearts = this.own(
         new SliderSetting("crosshair.rx.lowHearts", "At Hearts", "At this many hearts or fewer.", 1.0, 5.0, 3.0, 0.5).unit(" ♥").visibleWhen(lowOn),
         "Reactions"
      );
      this.rxLowColor = this.own(
         new ColorSetting("crosshair.rx.lowColor", "Low Health Color", "The tint.", ColorSetting.Mode.STATIC, -57312, -57312, 1.0)
            .skipSetAll()
            .visibleWhen(lowOn),
         "Reactions"
      );
      this.rxLowStrength = this.own(strength("crosshair.rx.lowStrength", lowOn), "Reactions");
      this.rxBlock = this.own(
         new BooleanSetting("crosshair.rx.block", "Blocking", "A small shield while you block with a sword.", false)
            .keywords(new String[]{"block", "shield", "sword block"}),
         "Reactions"
      );
      BooleanSupplier blockOn = on(this.rxBlock);
      this.rxBlockColor = this.own(
         new ColorSetting("crosshair.rx.blockColor", "Shield Color", "The shield's color.", ColorSetting.Mode.STATIC, -7351297, -7351297, 1.0)
            .skipSetAll()
            .visibleWhen(blockOn),
         "Reactions"
      );
      this.rxBlockStrength = this.own(strength("crosshair.rx.blockStrength", blockOn), "Reactions");
      this.profiles = new CrosshairProfiles(this);
      this.own(this.profiles.nameSetting(), "Profiles");
      this.own(
         new CrosshairElement.Row(
               "crosshair.profilesView", "Profiles", "Whole crosshair setups (situational ones included) by name, and share codes.", new Supplier<Widget>() {
                  public Widget get() {
                     return new CrosshairProfilesWidget(self);
                  }
               }
            )
            .keywords(new String[]{"profile", "profiles", "share code", "code", "copy", "paste", "import", "export", "next crosshair profile"}),
         "Profiles"
      );
      this.thirdPerson = this.own(
         new ModeSetting<>(
               "crosshair.thirdPerson", "Third Person", "The crosshair in third person.", CrosshairElement.ThirdPerson.class, CrosshairElement.ThirdPerson.SHOW
            )
            .keywords(new String[]{"f5", "third person"}),
         "More"
      );
      this.f3 = this.own(
         new ModeSetting<>("crosshair.f3", "With F3", "With the debug screen open.", CrosshairElement.F3.class, CrosshairElement.F3.VANILLA)
            .keywords(new String[]{"f3", "debug", "axes"}),
         "More"
      );
      this.store = this.own(new CrosshairElement.DefStore(), top);
      this.own(this.profiles.store(), top);
      this.profiles.captureDefaults();
      FuriahsVisuals.instance.getHitConfirmer().addListener(new HitConfirmer.Listener() {
         @Override
         public void onConfirmedHit(HitConfirmer.Attack attack) {
            CrosshairElement.this.confirmedHit(attack);
         }
      });
      KillEffectsModule.addKillListener(new KillEffectsModule.KillListener() {
         @Override
         public void onMyKill(KillContext ctx) {
            if (CrosshairElement.this.on()) {
               CrosshairElement.this.markers.kill(ClientClock.nanos());
            }
         }
      });
   }

   private static String[] withCrosshair(String[] words) {
      String[] out = new String[words.length + 1];
      System.arraycopy(words, 0, out, 0, words.length);
      out[words.length] = "crosshair";
      return out;
   }

   private BooleanSupplier notVanilla() {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return CrosshairElement.this.editor.style.get() != CrosshairStyle.VANILLA;
         }
      };
   }

   private static BooleanSupplier on(final BooleanSetting b) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return b.get();
         }
      };
   }

   private static SliderSetting strength(String id, BooleanSupplier visible) {
      return new SliderSetting(id, "Strength", "How strong the reaction is.", 0.1, 1.0, 1.0, 0.05).visibleWhen(visible);
   }

   List<Setting> profileSettings() {
      ArrayList<Setting> out = new ArrayList<>();

      for (Setting s : this.module.getSettings()) {
         if (s.getOwner() == this.section
            && s.isSaved()
            && s != this.store
            && s != this.profiles.store()
            && !s.getId().equals(this.section.getId() + ".enabled")) {
            out.add(s);
         }
      }

      return out;
   }

   int editing() {
      return this.editing;
   }

   boolean inUse(int index) {
      if (index == 0) {
         return true;
      } else if (index == AIMING) {
         return this.aiming.get();
      } else {
         return index == ZOOMED ? this.zoom.get() == CrosshairElement.ZoomUse.OWN : this.uses[index - 1].get() == CrosshairElement.Use.OWN;
      }
   }

   static String title(int index) {
      if (index == 0) {
         return "Main";
      } else if (index == AIMING) {
         return "Aiming";
      } else {
         return index == ZOOMED ? "Zoomed" : CrosshairElement.ItemKind.values()[index - 1].title;
      }
   }

   void edit(int index) {
      if (index < 0 || index >= COUNT || !this.inUse(index)) {
         index = 0;
      }

      this.editing = index;
      this.loadEditor();
   }

   void loadEditor() {
      if (this.editor != null) {
         this.loadingEditor = true;

         try {
            this.editor.copyFrom(this.defs[this.editing]);
         } finally {
            this.loadingEditor = false;
         }
      }
   }

   private void editorChanged(int index) {
      if (!this.loadingEditor && this.store != null) {
         this.defs[this.editing].all().get(index).fromJson(this.editor.all().get(index).toJson());
         this.store.changed();
      }
   }

   private void useChanged(int index) {
      if (this.store != null) {
         if (this.inUse(index)) {
            if (index != 0 && index != AIMING && index != ZOOMED && this.isDefault(index)) {
               this.defs[index].copyFrom(this.defs[0]);
               this.store.changed();
            }

            if (index != 0) {
               this.edit(index);
            }
         } else if (this.editing == index) {
            this.edit(0);
         }
      }
   }

   private boolean isDefault(int index) {
      return this.defs[index].toJson().equals(CrosshairDef.stored(this.defs[index].kind).toJson());
   }

   void imageRemoved() {
      this.imageNoticeDue = true;
      this.imageNoticeShown = false;
      FuriahsVisuals.LOG.info("UI: a crosshair used the removed Custom Image style; it loads as Classic now");
   }

   @Override
   public String notice() {
      if (this.imageNoticeDue) {
         this.imageNoticeShown = true;
         return "Custom image crosshairs were removed, so yours is the Classic crosshair now. Your images are still in config/furiahsvisuals/crosshairs.";
      } else {
         return null;
      }
   }

   static String key(int index) {
      if (index == 0) {
         return "main";
      } else if (index == AIMING) {
         return "aiming";
      } else {
         return index == ZOOMED ? "zoomed" : CrosshairElement.ItemKind.values()[index - 1].id;
      }
   }

   void defsReplaced() {
      if (!this.inUse(this.editing)) {
         this.editing = 0;
      }

      this.loadEditor();
      this.store.changed();
   }

   static CrosshairElement.ItemKind kindOf(ItemStack stack) {
      if (stack == null) {
         return CrosshairElement.ItemKind.EMPTY;
      } else {
         Item item = stack.getItem();
         if (item instanceof ItemSword || item instanceof ItemAxe) {
            return CrosshairElement.ItemKind.SWORD_AXE;
         } else if (item instanceof ItemBow) {
            return CrosshairElement.ItemKind.BOW;
         } else if (item instanceof ItemFishingRod) {
            return CrosshairElement.ItemKind.ROD;
         } else if (item instanceof ItemEnderPearl
            || item instanceof ItemSnowball
            || item instanceof ItemEgg
            || item instanceof ItemEnderEye
            || item instanceof ItemExpBottle) {
            return CrosshairElement.ItemKind.THROWABLE;
         } else if (item instanceof ItemBlock) {
            return CrosshairElement.ItemKind.BLOCK;
         } else {
            return !(item instanceof ItemFood) && !(item instanceof ItemPotion) && !(item instanceof ItemBucketMilk)
               ? CrosshairElement.ItemKind.OTHER
               : CrosshairElement.ItemKind.FOOD;
         }
      }
   }

   CrosshairDef situational(CrosshairElement.ItemKind kind, boolean aimingNow, boolean zoomed, boolean sprinting) {
      if (sprinting && this.hideSprinting.get()) {
         return null;
      } else {
         if (zoomed) {
            switch ((CrosshairElement.ZoomUse)this.zoom.get()) {
               case OWN:
                  return this.defs[ZOOMED];
               case HIDDEN:
                  return null;
            }
         }

         if (aimingNow && this.aiming.get()) {
            return this.defs[AIMING];
         } else {
            switch ((CrosshairElement.Use)this.uses[kind.ordinal()].get()) {
               case OWN:
                  return this.defs[1 + kind.ordinal()];
               case HIDDEN:
                  return null;
               default:
                  return this.defs[0];
            }
         }
      }
   }

   CrosshairRenderer.Reactions reactions() {
      double t = ColorSetting.clock();
      this.rx.target = this.rxTarget.get();
      this.rx.targetPlayerColor = this.rxTargetPlayer.getColor(0.5F, t);
      this.rx.targetMobColor = this.rxTargetMob.getColor(0.5F, t);
      this.rx.targetStrength = this.rxTargetStrength.getFloat();
      this.rx.clickPulse = this.rxClick.get();
      this.rx.clickColor = this.rxClickColor.getColor(0.5F, t);
      this.rx.clickStrength = this.rxClickStrength.getFloat();
      this.rx.bowCharge = this.rxBow.get();
      this.rx.bowColor = this.rxBowColor.getColor(0.5F, t);
      this.rx.bowStrength = this.rxBowStrength.getFloat();
      this.rx.moveSpread = this.rxMove.get();
      this.rx.moveColor = this.rxMoveColor.getColor(0.5F, t);
      this.rx.moveStrength = this.rxMoveStrength.getFloat();
      this.rx.lowHealth = this.rxLow.get();
      this.rx.lowHealthColor = this.rxLowColor.getColor(0.5F, t);
      this.rx.lowHealthStrength = this.rxLowStrength.getFloat();
      this.rx.blocking = this.rxBlock.get();
      this.rx.blockColor = this.rxBlockColor.getColor(0.5F, t);
      this.rx.blockStrength = this.rxBlockStrength.getFloat();
      return this.rx;
   }

   static float scaleOf(CrosshairDef def) {
      return def.sizeMode.get() == CrosshairDef.SizeMode.FIXED ? 1.0F : Minecraft.getMinecraft().displayHeight / 1080.0F;
   }

   @Override
   public void onRenderOverlay(float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      long now = ClientClock.nanos();
      float dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
      this.lastFrame = now;
      this.drewThisFrame = false;
      EntityPlayerSP p = mc.thePlayer;
      if (p != null) {
         this.state.time = (float)ColorSetting.clock();
         double dx = p.posX - p.lastTickPosX;
         double dz = p.posZ - p.lastTickPosZ;
         float speed = (float)Math.sqrt(dx * dx + dz * dz) / 0.216F;
         float moveTarget = Math.min(1.4F, speed + (p.onGround ? 0.0F : 0.35F));
         this.state.move = approach(this.state.move, moveTarget, moveTarget > this.state.move ? 10.0F : 6.0F, dt);
         boolean down = mc.currentScreen == null && mc.inGameHasFocus && attackDown(mc);
         if (down && !this.clickWasDown) {
            this.state.click = 1.0F;
         }

         this.clickWasDown = down;
         this.state.click = Math.max(0.0F, this.state.click - dt / 0.15F);
         float target = 0.0F;
         MovingObjectPosition mop = mc.objectMouseOver;
         if (mop != null && mop.typeOfHit == MovingObjectType.ENTITY && mop.entityHit instanceof EntityLivingBase && !mop.entityHit.isInvisibleToPlayer(p)
            )
          {
            target = 1.0F;
            this.state.targetPlayer = mop.entityHit instanceof EntityPlayer;
         }

         this.state.target = approach(this.state.target, target, 12.0F, dt);
         ItemStack using = p.getItemInUse();
         if (using != null && using.getItem() instanceof ItemBow && p.isUsingItem()) {
            float f = (p.getItemInUseDuration() + partialTicks) / 20.0F;
            float power;
            this.state.bow = power = Math.min(1.0F, (f * f + f * 2.0F) / 3.0F);
            if (power >= 1.0F) {
               if (this.bowFullSince == 0L) {
                  this.bowFullSince = now;
               }

               this.state.bowFull = (float)(now - this.bowFullSince) / 1.0E9F;
            } else {
               this.bowFullSince = 0L;
               this.state.bowFull = 0.0F;
            }
         } else {
            this.state.bow = -1.0F;
            this.bowFullSince = 0L;
         }

         boolean low = p.getHealth() > 0.0F && p.getHealth() <= this.rxLowHearts.getFloat() * 2.0F;
         this.state.lowHealth = approach(this.state.lowHealth, low ? 1.0F : 0.0F, 5.0F, dt);
         this.state.blocking = approach(this.state.blocking, p.isBlocking() ? 1.0F : 0.0F, 14.0F, dt);
         if (this.moveForTest >= 0.0F) {
            this.state.move = this.moveForTest;
         }

         if (this.clickForTest >= 0.0F) {
            this.state.click = this.clickForTest;
         }

         if (this.targetForTest >= 0.0F) {
            this.state.target = this.targetForTest;
            this.state.targetPlayer = true;
         }

         if (this.lowForTest >= 0.0F) {
            this.state.lowHealth = this.lowForTest;
         }

         if (this.blockForTest >= 0.0F) {
            this.state.blocking = this.blockForTest;
         }

         CrosshairDef next = this.visible(mc) ? this.situational(this.kind(mc), aimingNow(p), this.zoomed(mc), p.isSprinting()) : null;
         if (!this.shownSet) {
            this.shownSet = true;
            this.shown = next;
            this.morph = 1.0F;
         } else if (next != this.shown) {
            this.leaving = this.morph >= 1.0F ? this.shown : (this.morph >= 0.5F ? this.shown : this.leaving);
            this.shown = next;
            this.morph = 0.0F;
         }

         float speedK = this.module.speed();
         float f = this.morph = speedK <= 0.0F ? 1.0F : Math.min(1.0F, this.morph + dt * speedK / (0.12F * this.morphSlowForTest));
         if (this.morph >= 1.0F) {
            this.leaving = null;
         }
      }
   }

   private static float approach(float v, float target, float rate, float dt) {
      return Easing.approach(v, target, rate, dt);
   }

   private static boolean attackDown(Minecraft mc) {
      int code = mc.gameSettings.keyBindAttack.getKeyCode();

      try {
         return code < 0 ? Mouse.isButtonDown(code + 100) : code > 0 && Keyboard.isKeyDown(code);
      } catch (RuntimeException var3) {
         return mc.gameSettings.keyBindAttack.isKeyDown();
      }
   }

   private CrosshairElement.ItemKind kind(Minecraft mc) {
      return this.situationForTest >= 0 ? CrosshairElement.ItemKind.values()[this.situationForTest] : kindOf(mc.thePlayer.getHeldItem());
   }

   private static boolean aimingNow(EntityPlayerSP p) {
      ItemStack using = p.getItemInUse();
      return using != null && using.getItem() instanceof ItemBow && p.isUsingItem();
   }

   private boolean zoomed(Minecraft mc) {
      if (this.zoomForTest) {
         return true;
      } else {
         float fov = ScreenProjection.fieldOfView();
         return !Float.isNaN(fov) && fov < mc.gameSettings.fovSetting * 0.5F;
      }
   }

   private boolean visible(Minecraft mc) {
      if (!this.show.get()) {
         return false;
      } else if (mc.gameSettings.showDebugInfo
         && !mc.thePlayer.hasReducedDebug()
         && !mc.gameSettings.reducedDebugInfo
         && this.f3.get() == CrosshairElement.F3.VANILLA) {
         return false;
      } else if (mc.gameSettings.thirdPersonView != 0 && this.thirdPerson.get() == CrosshairElement.ThirdPerson.HIDE) {
         return false;
      } else if (mc.playerController == null || !mc.playerController.isSpectator()) {
         return true;
      } else if (mc.pointedEntity != null) {
         return true;
      } else {
         MovingObjectPosition mop = mc.objectMouseOver;
         return mop != null
            && mop.typeOfHit == MovingObjectType.BLOCK
            && mc.theWorld != null
            && mc.theWorld.getTileEntity(mop.getBlockPos()) instanceof IInventory;
      }
   }

   @Override
   public boolean onOverlayElement(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      if (type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
         return false;
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.thePlayer == null) {
            return false;
         } else if (this.shown != null && this.shown.style.get() == CrosshairStyle.VANILLA && this.morph >= 1.0F && vanillaShows(mc)) {
            return false;
         } else {
            this.drewThisFrame = true;
            boolean anything = this.shown != null || this.leaving != null || this.markers.showing(this, ClientClock.nanos());
            if (!anything) {
               return true;
            } else {
               try {
                  this.draw(mc, partialTicks);
               } catch (RuntimeException var7) {
                  FuriahsVisuals.LOG.error("UI: the crosshair failed to draw", var7);
               }

               return true;
            }
         }
      }
   }

   @Override
   public void onOverlayElementPost(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      if (type == RenderGameOverlayEvent.ElementType.CROSSHAIRS && !this.drewThisFrame) {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.thePlayer != null) {
            try {
               this.drawExtrasOnly(mc, partialTicks);
            } catch (RuntimeException var5) {
               FuriahsVisuals.LOG.error("UI: the crosshair's extras failed to draw", var5);
            }
         }
      }
   }

   private static boolean vanillaShows(Minecraft mc) {
      if (mc.gameSettings.showDebugInfo && !mc.thePlayer.hasReducedDebug() && !mc.gameSettings.reducedDebugInfo) {
         return false;
      } else if (mc.playerController == null || !mc.playerController.isSpectator()) {
         return true;
      } else if (mc.pointedEntity != null) {
         return true;
      } else {
         MovingObjectPosition mop = mc.objectMouseOver;
         return mop != null
            && mop.typeOfHit == MovingObjectType.BLOCK
            && mc.theWorld != null
            && mc.theWorld.getTileEntity(mop.getBlockPos()) instanceof IInventory;
      }
   }

   private float beginPixels(Minecraft mc) {
      ScaledResolution sr = new ScaledResolution(mc);
      float sf = sr.getScaleFactor();
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0F, 0.0F, -90.0F);
      GlStateManager.scale(1.0F / sf, 1.0F / sf, 1.0F);
      this.savedPx = Ui.px;
      Ui.px = 1.0F;
      Ui.beginFrameQuick(HudGl.program());
      GlStateManager.disableAlpha();
      GlStateManager.enableBlend();
      return sf;
   }

   private void endPixels(Minecraft mc) {
      Ui.endFrame();
      Ui.px = this.savedPx;
      Ui.premultiplied(false);
      GlStateManager.popMatrix();
      GlStateManager.enableTexture2D();
      GlStateManager.enableAlpha();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      mc.getTextureManager().bindTexture(Gui.icons);
   }

   private void draw(Minecraft mc, float partialTicks) {
      long start = System.nanoTime();
      float sf = this.beginPixels(mc);

      try {
         float cx = mc.displayWidth / 2;
         float cy = mc.displayHeight / 2;
         CrosshairRenderer.Reactions r = this.reactions();
         float e = Easing.outCubic(this.morph);
         float keep = 1.0F - this.markers.skullCover(this, ClientClock.nanos());
         if (this.leaving != null && this.morph < 1.0F) {
            this.drawOne(mc, this.leaving, r, cx, cy, sf, (1.0F - e) * keep, 1.0F - 0.25F * e);
         }

         if (this.shown != null) {
            this.drawOne(mc, this.shown, r, cx, cy, sf, (this.morph < 1.0F ? e : 1.0F) * keep, this.morph < 1.0F ? 0.75F + 0.25F * e : 1.0F);
         }

         this.drawMarkers(mc, cx, cy, partialTicks);
      } finally {
         this.endPixels(mc);
      }

      float var14 = (float)(System.nanoTime() - start) / 1000000.0F;
      this.costMs = this.costMs < 0.0F ? var14 : this.costMs + (var14 - this.costMs) * 0.05F;
   }

   private void drawOne(Minecraft mc, CrosshairDef def, CrosshairRenderer.Reactions r, float cx, float cy, float sf, float opacity, float size) {
      this.state.scale = scaleOf(def);
      if (def.style.get() == CrosshairStyle.VANILLA) {
         ScaledResolution sr = new ScaledResolution(mc);
         float vx = sr.getScaledWidth() / 2 * sf;
         float vy = sr.getScaledHeight() / 2 * sf;
         CrosshairRenderer.drawVanilla(vx, vy, sf * size, opacity);
      } else {
         CrosshairRenderer.draw(def, this.state, r, cx, cy, opacity, size);
      }

      CrosshairRenderer.drawExtras(def, this.state, r, cx, cy, opacity);
   }

   private void drawMarkers(Minecraft mc, float cx, float cy, float partialTicks) {
      CrosshairDef d = this.shown != null ? this.shown : this.defs[0];
      this.markers.draw(this, cx, cy, scaleOf(d), CrosshairRenderer.reachWithExtras(d, this.state, this.reactions()), partialTicks, ClientClock.nanos());
   }

   private void drawExtrasOnly(Minecraft mc, float partialTicks) {
      boolean ring = this.rxBow.get() && this.state.bow >= 0.0F;
      boolean shield = this.rxBlock.get() && this.state.blocking > 0.01F;
      if (this.shown != null && (ring || shield || this.markers.showing(this, ClientClock.nanos()))) {
         this.beginPixels(mc);

         try {
            float cx = mc.displayWidth / 2;
            float cy = mc.displayHeight / 2;
            this.state.scale = scaleOf(this.shown);
            CrosshairRenderer.drawExtras(this.shown, this.state, this.reactions(), cx, cy, 1.0F);
            this.drawMarkers(mc, cx, cy, partialTicks);
         } finally {
            this.endPixels(mc);
         }
      }
   }

   @Override
   public void onAttack(EntityPlayer attacker, Entity target) {
      HitConfirmer.Attack attack = FuriahsVisuals.instance.getHitConfirmer().lastAttack();
      if (attack != null) {
         long now = GameClock.nanos();
         Iterator<HitConfirmer.Attack> it = this.crits.keySet().iterator();

         while (it.hasNext()) {
            if (now - it.next().nanos > 2000000000L) {
               it.remove();
            }
         }

         this.crits.put(attack, attacker instanceof EntityPlayerSP && HitMeasurer.isCrit((EntityPlayerSP)attacker, target));
      }
   }

   private void confirmedHit(HitConfirmer.Attack attack) {
      if (this.on()) {
         Boolean crit = this.crits.remove(attack);
         this.markers.hit(attack.target, crit != null && crit, ClientClock.nanos());
         this.lastHitTarget = attack.target;
         this.lastHitNanos = GameClock.nanos();
      }
   }

   @Override
   public void onTick() {
      Entity t = this.lastHitTarget;
      if (t != null) {
         long now = GameClock.nanos();
         if (now - this.lastHitNanos > 2000000000L) {
            this.lastHitTarget = null;
         } else {
            if (t instanceof EntityLivingBase && (((EntityLivingBase)t).getHealth() <= 0.0F || t.isDead)) {
               Module ke = FuriahsVisuals.instance.getModules().get("kill_effects");
               if (!(t instanceof EntityPlayer) || ke == null || !ke.isEnabled()) {
                  this.markers.kill(ClientClock.nanos());
               }

               this.lastHitTarget = null;
            }
         }
      }
   }

   @Override
   public void onWorldChange() {
      this.markers.clear();
      this.crits.clear();
      this.lastHitTarget = null;
      this.shownSet = false;
      this.leaving = null;
      this.state.blocking = 0.0F;
      this.state.lowHealth = 0.0F;
      this.state.target = 0.0F;
      this.state.click = 0.0F;
      this.state.move = 0.0F;
      this.state.bow = -1.0F;
   }

   @Override
   public void applyTheme() {
      if (this.defs[0].style.get() == CrosshairStyle.VANILLA) {
         this.defs[0].style.set(CrosshairStyle.CLASSIC);
         if (this.editing == 0) {
            this.loadEditor();
         }

         this.store.changed();
      }
   }

   @Override
   public void afterLoad() {
      if (!this.inUse(this.editing)) {
         this.editing = 0;
      }

      this.loadEditor();
      this.warning = this.imageLoadedThisLoad
         ? "Crosshair: Custom image crosshairs were removed, so yours is the Classic crosshair now. Your images are still in config/furiahsvisuals/crosshairs."
         : null;
      this.imageLoadedThisLoad = false;
   }

   @Override
   public String loadWarning() {
      return this.warning;
   }

   @Override
   public boolean reshapedOnLoad() {
      return this.warning != null;
   }

   @Override
   public void onClientTick() {
      this.profiles.tick();
      if (this.imageNoticeShown && !(Minecraft.getMinecraft().currentScreen instanceof FuriahScreen)) {
         this.imageNoticeDue = false;
         this.imageNoticeShown = false;
      }
   }

   public void nextProfile() {
      if (this.on()) {
         String name = this.profiles.next();
         FuriahsVisuals.instance
            .getWaterMark()
            .uiNotice("crosshair-profile", name == null ? "No crosshair profiles yet" : "Crosshair: " + name, IslandIcons.Icon.CROSSHAIR);
      }
   }

   public String legacyImageForTest() {
      JsonObject old = new JsonObject();
      old.addProperty("style", "IMAGE");
      old.addProperty("image", "fv_test_cross.png");
      old.addProperty("imageSize", 32);
      old.addProperty("imageColors", "TINT");
      old.addProperty("imageSmoothing", "PIXEL");
      old.addProperty("imageX", 2);
      old.addProperty("size", 9);
      StringBuilder sb = new StringBuilder();
      JsonObject saved = this.store.toJson().getAsJsonObject();
      saved.add("main", old);
      this.imageNoticeDue = false;
      this.store.fromJson(saved);
      this.afterLoad();
      sb.append("config: main ")
         .append(this.defs[0].style.get())
         .append(" (size ")
         .append(this.defs[0].size.get())
         .append("), editor ")
         .append(this.editor.style.get())
         .append(", saved again at once ")
         .append(this.reshapedOnLoad())
         .append(", notice ")
         .append(this.imageNoticeDue ? "due" : "NOT due")
         .append(", warning '")
         .append(this.loadWarning())
         .append("'");
      JsonObject setup = new JsonObject();
      setup.add("s", new JsonObject());
      JsonObject d = new JsonObject();
      d.add("main", old);
      setup.add("d", d);
      this.defs[0].style.set(CrosshairStyle.HEX);
      this.imageNoticeDue = false;
      String msg = this.profiles.loadOldForTest("Old image", setup);
      sb.append("; profile: main ")
         .append(this.defs[0].style.get())
         .append(", notice ")
         .append(this.imageNoticeDue ? "due" : "NOT due")
         .append(", '")
         .append(msg)
         .append("'");
      this.defs[0].style.set(CrosshairStyle.HEX);
      this.loadEditor();
      this.imageNoticeDue = false;
      msg = this.profiles.importCode(CrosshairProfiles.encode(setup), false);
      sb.append("; code: main ")
         .append(this.defs[0].style.get())
         .append(", notice ")
         .append(this.imageNoticeDue ? "due" : "NOT due")
         .append(", '")
         .append(msg)
         .append("'");
      String now = this.store.toJson().toString() + this.profiles.store().toJson().toString() + this.profiles.snapshot().toString();
      boolean left = Pattern.compile("\"image[A-Za-z]*\":|\"IMAGE\"").matcher(now).find();
      sb.append("; image settings left: ").append(left ? "SOME (FAIL)" : "none (OK)");
      this.profiles.delete(this.profiles.names().indexOf("Old image"));
      return sb.toString();
   }

   public void situationForTest(String id) {
      this.situationForTest = -1;

      for (CrosshairElement.ItemKind k : CrosshairElement.ItemKind.values()) {
         if (k.id.equals(id)) {
            this.situationForTest = k.ordinal();
         }
      }
   }

   public void zoomForTest(boolean on) {
      this.zoomForTest = on;
   }

   public void colorForTest(int rgb) {
      this.editor.color.setMode(ColorSetting.Mode.STATIC);
      this.editor.color.setColorA(0xFF000000 | rgb);
   }

   public void forceForTest(String what, float value) {
      if ("move".equals(what)) {
         this.moveForTest = value;
      } else if ("click".equals(what)) {
         this.clickForTest = value;
      } else if ("target".equals(what)) {
         this.targetForTest = value;
      } else if ("low".equals(what)) {
         this.lowForTest = value;
      } else {
         if (!"block".equals(what)) {
            throw new IllegalArgumentException("no reaction " + what);
         }

         this.blockForTest = value;
      }
   }

   public void previewTestForTest(int button) {
      if (this.lastPreview != null) {
         this.lastPreview.test(button, ClientClock.nanos());
      }
   }

   public void previewBackgroundForTest(int which) {
      CrosshairPreviewWidget.backgroundForTest(which);
   }

   public void editForTest(String key) {
      for (int i = 0; i < COUNT; i++) {
         if (key(i).equals(key)) {
            this.edit(i);
            return;
         }
      }

      throw new IllegalArgumentException("no crosshair " + key);
   }

   public void markerForTest(String what) {
      long now = ClientClock.nanos();
      if ("kill".equals(what)) {
         this.markers.kill(now);
      } else {
         this.markers.hit(null, "crit".equals(what), now);
      }
   }

   public String stateForTest() {
      int shownIndex = -1;

      for (int i = 0; i < COUNT; i++) {
         if (this.defs[i] == this.shown) {
            shownIndex = i;
         }
      }

      return "shown "
         + (this.shown == null ? "none" : key(shownIndex) + " (" + this.shown.style.get().displayName() + ")")
         + ", editing "
         + key(this.editing)
         + ", target "
         + String.format("%.2f", this.state.target)
         + ", bow "
         + String.format("%.2f", this.state.bow)
         + ", move "
         + String.format("%.2f", this.state.move)
         + ", draw "
         + (this.costMs < 0.0F ? "-" : String.format("%.3f ms", this.costMs))
         + ", view "
         + String.format("%.1f", ScreenProjection.fieldOfView())
         + "° (setting "
         + Minecraft.getMinecraft().gameSettings.fovSetting
         + ")";
   }

   public String codeForTest() {
      return this.profiles.exportCode();
   }

   public String importCodeForTest(String code) {
      return this.profiles.importCode(code, false);
   }

   public String saveProfileForTest(String name) {
      return this.profiles.save(name);
   }

   public String nextProfileForTest() {
      return this.profiles.next();
   }

   public String profilesForTest() {
      return this.profiles.describe();
   }

   public String defValueForTest(String key, String id) {
      for (int i = 0; i < COUNT; i++) {
         if (key(i).equals(key)) {
            for (Setting s : this.defs[i].all()) {
               if (s.getId().equals(id)) {
                  return s.toJson().toString();
               }
            }
         }
      }

      return null;
   }

   private final class DefStore extends Setting {
      DefStore() {
         super("crosshair.defs", "Crosshairs", "Every crosshair's settings.");
      }

      void changed() {
         this.fireChanged();
      }

      @Override
      public boolean showInMenu() {
         return false;
      }

      @Override
      public void reset() {
         for (CrosshairDef d : CrosshairElement.this.defs) {
            d.reset();
         }

         CrosshairElement.this.loadEditor();
         this.fireChanged();
      }

      @Override
      public JsonElement toJson() {
         JsonObject o = new JsonObject();

         for (int i = 0; i < CrosshairElement.COUNT; i++) {
            o.add(CrosshairElement.key(i), CrosshairElement.this.defs[i].toJson());
         }

         return o;
      }

      @Override
      public void fromJson(JsonElement json) {
         JsonObject o = json != null && json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
         boolean image = false;

         for (int i = 0; i < CrosshairElement.COUNT; i++) {
            JsonElement e = o.get(CrosshairElement.key(i));
            image |= CrosshairElement.this.defs[i].fromJson(e != null && e.isJsonObject() ? e.getAsJsonObject() : null);
         }

         if (image) {
            CrosshairElement.this.imageRemoved();
            CrosshairElement.this.imageLoadedThisLoad = true;
         }

         CrosshairElement.this.loadEditor();
         this.fireChanged();
      }
   }

   public static enum F3 implements Displayable {
      VANILLA("Vanilla Axes", "With the debug screen (F3) open, vanilla's axes show instead, as in vanilla"),
      KEEP("Keep My Crosshair", "Your crosshair stays on screen with F3 open");

      private final String displayName;
      private final String description;

      private F3(String displayName, String description) {
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

   static enum ItemKind {
      SWORD_AXE("sword", "Sword or Axe", "Swords and axes"),
      BOW("bow", "Bow", "A bow (while not drawing it: see Aiming)"),
      ROD("rod", "Fishing Rod", "A fishing rod"),
      THROWABLE("throwable", "Throwables", "Ender pearls, snowballs, eggs, eyes of ender, bottles o' enchanting"),
      BLOCK("blocks", "Blocks", "Any block"),
      FOOD("food", "Food & Potions", "Food, potions and milk"),
      EMPTY("empty", "Empty Hand", "Nothing in your hand"),
      OTHER("other", "Everything Else", "Any other item");

      final String id;
      final String title;
      final String description;

      private ItemKind(String id, String title, String description) {
         this.id = id;
         this.title = title;
         this.description = description;
      }
   }

   static final class Row extends WidgetSetting {
      private final Supplier<Widget> widget;

      Row(String id, String name, String description, Supplier<Widget> widget) {
         super(id, name, description);
         this.widget = widget;
      }

      @Override
      public Widget createWidget() {
         Widget w = this.widget.get();
         if (w instanceof RowWidget) {
            ((RowWidget)w).row = this;
         }

         return w;
      }
   }

   public static enum ThirdPerson implements Displayable {
      SHOW("Show", "As vanilla 1.8.9: the crosshair stays in third person"),
      HIDE("Hide", "No crosshair in third person");

      private final String displayName;
      private final String description;

      private ThirdPerson(String displayName, String description) {
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

   public static enum Use implements Displayable {
      MAIN("Main", "The main crosshair"),
      OWN("Own", "A crosshair of its own (pick it above to edit it)"),
      HIDDEN("Hidden", "No crosshair");

      private final String displayName;
      private final String description;

      private Use(String displayName, String description) {
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

   public static enum ZoomUse implements Displayable {
      OWN("Zoom Crosshair", "A crosshair of its own while zoomed (a thin reticle at first)"),
      HIDDEN("Hidden", "No crosshair while zoomed"),
      KEEP("Keep", "The same crosshair as without zoom");

      private final String displayName;
      private final String description;

      private ZoomUse(String displayName, String description) {
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
