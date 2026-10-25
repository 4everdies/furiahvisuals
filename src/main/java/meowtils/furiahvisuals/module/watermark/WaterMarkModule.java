package meowtils.furiahvisuals.module.watermark;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.ConfigManager;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.PresetManager;
import meowtils.furiahvisuals.gui.FuriahScreen;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.watermark.data.SystemStats;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.StringListSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

public final class WaterMarkModule extends Module implements PresetManager.Listener {
   static final int GREEN = -12723068;
   static final int RED = -43674;
   private static final int MAX_QUEUED = 6;
   private static final long SOURCE_CLICK_MS = 3000L;
   private static final String[] SOUND_WORDS = new String[]{"sound", "sounds", "audio", "volume", "mute", "sfx"};
   final Setting previewRow;
   final ModeSetting<WaterMarkModule.Anchor> anchor;
   final Setting positionRow;
   final SliderSetting offsetX;
   final SliderSetting offsetY;
   final SliderSetting scale;
   final Setting modulesRow;
   final StringListSetting order;
   final ModeSetting<WaterMarkModule.PanelColumns> panelColumns;
   final BooleanSetting panelLiveValues;
   final SliderSetting opacity;
   final ModeSetting<WaterMarkModule.GlassMode> glassMode;
   final BooleanSetting outline;
   final BooleanSetting shadow;
   final BooleanSetting icons;
   final BooleanSetting separators;
   final ModeSetting<WaterMarkModule.FvColor> fvColor;
   final SliderSetting fontSize;
   final ColorSetting accent;
   final ModeSetting<WaterMarkModule.GradientSpread> gradients;
   final BooleanSetting glint;
   final SliderSetting glintEvery;
   final SliderSetting glintTime;
   final SliderSetting glintAngle;
   final SliderSetting glintWidth;
   final SliderSetting glintStrength;
   final ModeSetting<WaterMarkModule.GlintDirection> glintDirection;
   final ModeSetting<WaterMarkModule.GlintColor> glintColor;
   final SliderSetting speed;
   final BooleanSetting rollingDigits;
   final BooleanSetting notifications;
   final BooleanSetting notifySections;
   final BooleanSetting notifyPresets;
   final BooleanSetting notifyTrack;
   final BooleanSetting notifySource;
   final SliderSetting notifyDuration;
   final BooleanSetting hideF3;
   final BooleanSetting hideTab;
   final BooleanSetting showInMenus;
   final ModeSetting<WaterMarkModule.Units> weatherUnits;
   final BooleanSetting islandSounds;
   final SliderSetting islandVolume;
   final BooleanSetting soundKill;
   final BooleanSetting soundStreak;
   final BooleanSetting soundNowPlaying;
   final BooleanSetting soundOther;
   final BooleanSetting soundClicks;
   final BooleanSetting soundWhoosh;
   final FvItem fv;
   final UsernameItem user;
   final ServerItem server;
   final FpsItem fps;
   final PingItem ping;
   final BpsItem bps;
   final XyzItem xyz;
   final SystemItem system;
   final TimeItem time;
   final WeatherItem weatherItem;
   final TextItem text;
   final SettingsItem gear;
   private final List<IslandItem> all = new ArrayList<>();
   private final List<IslandItem> ordered = new ArrayList<>();
   private int orderVersion = -1;
   private final SystemStats stats = new SystemStats();
   private long lastTickMs;
   final IslandRenderer island;
   private final ArrayDeque<WaterMarkModule.Notice> queue = new ArrayDeque<>();
   private WaterMarkModule.Notice current;
   private boolean suppressSections;
   private boolean settingsView;
   private boolean moduleRowOpen;
   private ConfigManager config;
   private PresetManager presets;
   private String expandRequest;

   public WaterMarkModule() {
      super("watermark", "WaterMark", "A dynamic island at the top of your screen: FPS, ping, time, music and more.", true);
      final WaterMarkModule wm = this;
      this.previewRow = new WidgetSetting("preview", "Preview", "How the island looks right now.") {
         @Override
         public Widget createWidget() {
            return new IslandPreviewWidget(wm);
         }
      };
      this.anchor = new ModeSetting<>("position", "Position", "Where the island sits.", WaterMarkModule.Anchor.class, WaterMarkModule.Anchor.TOP_CENTER)
         .nested();
      this.positionRow = new WidgetSetting("positionPicker", "Position", "Where the island sits: click a dot.") {
         @Override
         public Widget createWidget() {
            return new PositionPickerWidget(wm);
         }
      };
      this.offsetX = new SliderSetting(
            "offsetX", "Offset X", "Moves the island away from its corner, sideways (at 1080p; scales with the screen).", -400.0, 400.0, 0.0, 1.0
         )
         .unit(" px");
      this.offsetY = new SliderSetting(
            "offsetY", "Offset Y", "Moves the island away from its edge, towards the middle (at 1080p; scales with the screen).", -40.0, 400.0, 0.0, 1.0
         )
         .unit(" px");
      this.scale = new SliderSetting("scale", "Scale", "Size of the island (its own scale; Minecraft's GUI scale does not change it).", 0.5, 2.0, 1.0, 0.05)
         .unit("×");
      this.modulesRow = new WidgetSetting("modules", "Modules", "What the island shows. Drag the handles to reorder; click a row for its options.") {
         @Override
         public Widget createWidget() {
            return new ModuleListWidget(wm);
         }
      };
      this.order = new StringListSetting(
            "order", "Order", "The order of the island's modules.", "user", "server", "fps", "ping", "bps", "xyz", "system", "time", "weather", "text"
         )
         .nested();
      this.panelColumns = new ModeSetting<>(
            "panelColumns",
            "Quick Settings Panel: Columns",
            "With chat open, ⚙ › Modules lists every module in a panel: in how many columns.",
            WaterMarkModule.PanelColumns.class,
            WaterMarkModule.PanelColumns.AUTO
         )
         .keywords(new String[]{"modules panel", "quick settings panel", "columns"});
      this.panelLiveValues = new BooleanSetting(
            "panelLiveValues",
            "Quick Settings Panel: Live Values",
            "The panel's rows show each module's value now (144 fps, 23 ms); off: a short description of each.",
            true
         )
         .keywords(new String[]{"modules panel", "quick settings panel", "values"});
      this.opacity = new SliderSetting("opacity", "Opacity", "How solid the island's background is.", 0.0, 1.0, 0.85, 0.05);
      this.glassMode = new ModeSetting<>(
            "glass",
            "Glass",
            "Frosted glass: the world shows through the island, softly blurred.",
            WaterMarkModule.GlassMode.class,
            WaterMarkModule.GlassMode.AUTO
         )
         .keywords(new String[]{"blur", "frosted", "transparent"});
      this.outline = new BooleanSetting("outline", "Outline", "A thin line around the island.", true);
      this.shadow = new BooleanSetting("shadow", "Shadow", "A soft shadow under the island.", true);
      this.icons = new BooleanSetting("icons", "Icons", "Small icons before the values (each module can pick its own, or none).", true);
      this.separators = new BooleanSetting("separators", "Separators", "Thin lines between the modules.", true);
      this.fvColor = new ModeSetting<>("fvColor", "FV Color", "The color of the initials.", WaterMarkModule.FvColor.class, WaterMarkModule.FvColor.ACCENT);
      this.fontSize = new SliderSetting("fontSize", "Font Size", "Text size (at 1080p and scale 1×; the island grows with it).", 10.0, 18.0, 13.0, 0.5);
      this.accent = new ColorSetting("accent", "Accent", "The island's accent: FV, icons and highlights.", ColorSetting.Mode.STATIC, -7635713, -45096, 1.0)
         .skipSetAll();
      this.gradients = new ModeSetting<>(
         "gradients",
         "Gradient Colors",
         "How a gradient or chroma accent or Global Color spreads over the island.",
         WaterMarkModule.GradientSpread.class,
         WaterMarkModule.GradientSpread.SAME
      );
      this.glint = new BooleanSetting("glint", "Glint", "A tilted shine sweeps across the island every few seconds.", false);
      this.glintEvery = new SliderSetting("glintEvery", "Every", "Seconds from one glint to the next.", 1.5, 30.0, 6.0, 0.5).unit(" s");
      this.glintTime = new SliderSetting("glintTime", "Sweep Time", "How long one glint takes to cross the island.", 0.3, 3.0, 0.9, 0.05).unit(" s");
      this.glintDirection = new ModeSetting<>(
         "glintDirection",
         "Direction",
         "Which way the glint crosses the island.",
         WaterMarkModule.GlintDirection.class,
         WaterMarkModule.GlintDirection.LEFT_TO_RIGHT
      );
      this.glintAngle = new SliderSetting("glintAngle", "Tilt", "How far the glint leans (0 is upright).", -60.0, 60.0, 25.0, 1.0).unit("°");
      this.glintWidth = new SliderSetting("glintWidth", "Width", "How wide the glint is (at 1080p; scales with the island).", 6.0, 80.0, 22.0, 1.0).unit(" px");
      this.glintStrength = new SliderSetting("glintStrength", "Strength", "How bright the glint is.", 0.05, 1.0, 0.45, 0.05);
      this.glintColor = new ModeSetting<>("glintColor", "Color", "The glint's color.", WaterMarkModule.GlintColor.class, WaterMarkModule.GlintColor.AUTO);
      this.speed = new SliderSetting("speed", "Animation Speed", "How fast the island moves and changes shape.", 0.5, 2.0, 1.0, 0.05).unit("×");
      this.rollingDigits = new BooleanSetting("rolling", "Rolling Digits", "Changing numbers roll into place like a counter.", true)
         .keywords(new String[]{"odometer", "counter"});
      this.notifications = new BooleanSetting("notifications", "Notifications", "Short messages in the island (a section switched, a preset loaded).", true)
         .keywords(new String[]{"toast", "popup", "alert"});
      this.notifySections = new BooleanSetting("notifySections", "Section Switched", "\"Hit Effect enabled\", \"Glow disabled\".", true);
      this.notifyPresets = new BooleanSetting("notifyPresets", "Preset Loaded", "\"Preset 'Tournament' loaded\".", true);
      this.notifyTrack = new BooleanSetting("notifyTrack", "New Song", "The title of each new song (needs the Audio module).", false);
      this.notifySource = new BooleanSetting("notifySource", "Audio Source", "\"Audio source: Chrome\" when another app starts playing.", true);
      this.notifyDuration = new SliderSetting("notifyDuration", "Notification Time", "How long each message stays.", 1.0, 6.0, 2.5, 0.1).unit(" s");
      this.hideF3 = new BooleanSetting("hideF3", "Hide with F3", "The island steps aside while the debug screen is open.", true);
      this.hideTab = new BooleanSetting("hideTab", "Hide with Tab List", "The island steps aside while you hold the player list key.", true);
      this.showInMenus = new BooleanSetting("showInMenus", "Show in Menus", "Also show the island in inventories and menus (always shown in chat).", false);
      this.weatherUnits = new ModeSetting<>("weatherUnits", "Units", "Temperature units.", WaterMarkModule.Units.class, WaterMarkModule.Units.CELSIUS);
      this.islandSounds = new BooleanSetting("islandSounds", "Island Sounds", "Sounds for the island's notifications and buttons.", true)
         .<BooleanSetting>keywords(SOUND_WORDS)
         .preview(this.sample(WaterMarkModule.Notice.Kind.SOURCE));
      this.islandVolume = new SliderSetting("islandVolume", "Island Volume", "How loud the island's sounds are.", 0.0, 100.0, 100.0, 1.0)
         .unit("%")
         .<SliderSetting>keywords(SOUND_WORDS)
         .preview(this.sample(WaterMarkModule.Notice.Kind.SOURCE));
      this.soundKill = new BooleanSetting(
            "soundKill", "Kill Notification", "A crisp ping over a soft thump the moment you kill a player (with Kill Effects > Island Notification).", true
         )
         .<BooleanSetting>keywords(SOUND_WORDS)
         .preview(this.sample(WaterMarkModule.Notice.Kind.KILL));
      this.soundStreak = new BooleanSetting("soundStreak", "Streak Pitch Rise", "Each kill of a streak pings a note higher, up to ×5.", true)
         .<BooleanSetting>keywords(SOUND_WORDS)
         .preview(new Runnable() {
            @Override
            public void run() {
               WaterMarkModule.this.showSample(WaterMarkModule.Notice.Kind.KILL, 3);
            }
         });
      this.soundNowPlaying = new BooleanSetting("soundNowPlaying", "Now Playing", "A soft pop when the island shows a new song.", false)
         .<BooleanSetting>keywords(SOUND_WORDS)
         .preview(this.sample(WaterMarkModule.Notice.Kind.TRACK));
      this.soundOther = new BooleanSetting(
            "soundOther",
            "Other Notifications",
            "A soft pop for the island's other messages, like \"Audio source: Chrome\" when it changes by itself. Notices of changes you make yourself (a section, a preset) stay silent.",
            true
         )
         .<BooleanSetting>keywords(SOUND_WORDS)
         .preview(this.sample(WaterMarkModule.Notice.Kind.SOURCE));
      this.soundClicks = new BooleanSetting("soundClicks", "Island Clicks", "Clicks and switch blips for the island's buttons (with chat open).", true)
         .<BooleanSetting>keywords(SOUND_WORDS)
         .preview(new Runnable() {
            @Override
            public void run() {
               ModSounds.preview(Sfx.CLICK, WaterMarkModule.this.islandVolume.getFloat() / 100.0F);
            }
         });
      this.soundWhoosh = new BooleanSetting("soundWhoosh", "Expand Whoosh", "A very quiet whoosh when the island opens or closes its quick settings.", true)
         .<BooleanSetting>keywords(SOUND_WORDS)
         .preview(new Runnable() {
            @Override
            public void run() {
               ModSounds.preview(Sfx.WHOOSH, WaterMarkModule.this.islandVolume.getFloat() / 100.0F);
            }
         });
      this.fv = this.register(new FvItem(this), true);
      this.user = this.register(new UsernameItem(this), true);
      this.server = this.register(new ServerItem(this), true);
      this.fps = this.register(new FpsItem(this), true);
      this.ping = this.register(new PingItem(this), true);
      this.bps = this.register(new BpsItem(this), false);
      this.xyz = this.register(new XyzItem(this), false);
      this.system = this.register(new SystemItem(this), false);
      this.time = this.register(new TimeItem(this), true);
      this.weatherItem = this.register(new WeatherItem(this), false);
      this.text = this.register(new TextItem(this), false);
      this.gear = this.register(new SettingsItem(this), true);
      this.add(this.previewRow);
      this.add(this.positionRow.group("Position"));
      this.add(this.anchor.group("Position"));
      this.add(this.offsetX.group("Position"));
      this.add(this.offsetY.group("Position"));
      this.add(this.scale.group("Position"));
      this.add(this.modulesRow.group("Modules"));
      this.add(this.order.group("Modules"));
      this.add(this.panelColumns.group("Modules"));
      this.add(this.panelLiveValues.group("Modules"));
      this.add(this.opacity.group("Style"));
      this.add(this.glassMode.group("Style"));
      this.add(this.outline.group("Style"));
      this.add(this.shadow.group("Style"));
      this.add(this.icons.group("Style"));
      this.add(this.separators.group("Style"));
      this.add(this.fvColor.group("Style"));
      this.add(this.fontSize.group("Style"));
      this.add(this.accent.group("Style"));
      this.add(this.gradients.group("Style"));
      this.add(this.glint.group("Glint"));
      BooleanSupplier glintOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return WaterMarkModule.this.glint.get();
         }
      };
      this.add(this.glintEvery.<Setting>group("Glint").visibleWhen(glintOn));
      this.add(this.glintTime.<Setting>group("Glint").visibleWhen(glintOn));
      this.add(this.glintDirection.<Setting>group("Glint").visibleWhen(glintOn));
      this.add(this.glintAngle.<Setting>group("Glint").visibleWhen(glintOn));
      this.add(this.glintWidth.<Setting>group("Glint").visibleWhen(glintOn));
      this.add(this.glintStrength.<Setting>group("Glint").visibleWhen(glintOn));
      this.add(this.glintColor.<Setting>group("Glint").visibleWhen(glintOn));
      this.add(this.speed.group("Motion"));
      this.add(this.rollingDigits.group("Motion"));
      this.add(this.notifications.group("Motion"));
      BooleanSupplier notifyOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return WaterMarkModule.this.notifications.get();
         }
      };
      this.add(this.notifySections.<Setting>group("Motion").visibleWhen(notifyOn));
      this.add(this.notifyPresets.<Setting>group("Motion").visibleWhen(notifyOn));
      this.add(this.notifyTrack.<Setting>group("Motion").visibleWhen(notifyOn));
      this.add(this.notifySource.<Setting>group("Motion").visibleWhen(notifyOn));
      this.add(this.notifyDuration.<Setting>group("Motion").visibleWhen(notifyOn));
      this.add(this.hideF3.group("Visibility"));
      this.add(this.hideTab.group("Visibility"));
      this.add(this.showInMenus.group("Visibility"));
      this.add(this.weatherUnits.group("Weather"));
      this.add(this.islandSounds.group("Sounds"));
      BooleanSupplier soundsOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return WaterMarkModule.this.islandSounds.get();
         }
      };
      this.add(this.islandVolume.<Setting>group("Sounds").visibleWhen(soundsOn));
      this.add(this.soundKill.<Setting>group("Sounds").visibleWhen(soundsOn));
      this.add(this.soundStreak.<Setting>group("Sounds").visibleWhen(new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return WaterMarkModule.this.islandSounds.get() && WaterMarkModule.this.soundKill.get();
         }
      }));
      this.add(this.soundNowPlaying.<Setting>group("Sounds").visibleWhen(soundsOn));
      this.add(this.soundOther.<Setting>group("Sounds").visibleWhen(soundsOn));
      this.add(this.soundClicks.<Setting>group("Sounds").visibleWhen(soundsOn));
      this.add(this.soundWhoosh.<Setting>group("Sounds").visibleWhen(soundsOn));
      this.island = new IslandRenderer(this);
   }

   private <I extends IslandItem> I register(I item, boolean on) {
      this.all.add(item);
      if (item != null && !(item instanceof FvItem)) {
         BooleanSetting b = new BooleanSetting("item." + item.id, item.name, item.description, on).nested();
         item.shown = this.add(b);
      }

      return item;
   }

   <S extends Setting> S registerNested(S setting) {
      setting.nested();
      return this.add(setting);
   }

   public void connect(ConfigManager config, PresetManager presets) {
      this.config = config;
      this.presets = presets;
      presets.setListener(this);

      for (final Module m : FuriahsVisuals.instance.getModules().getModules()) {
         if (m != this) {
            m.getEnabledSetting().addListener(new Runnable() {
               @Override
               public void run() {
                  WaterMarkModule.this.sectionSwitched(m);
               }
            });
         }
      }
   }

   IslandDraw draw() {
      return this.island.draw();
   }

   boolean rolling() {
      return this.rollingDigits.get();
   }

   int tint(WaterMarkModule.IconTint tint, IslandStyle s, float x) {
      return s.paint(tint.paint, x);
   }

   boolean gradientsAcross() {
      return this.gradients.get() == WaterMarkModule.GradientSpread.ACROSS;
   }

   SystemStats stats() {
      return this.stats;
   }

   boolean isSettingsViewOpen() {
      return this.settingsView;
   }

   void toggleSettingsView() {
      boolean bl = this.settingsView = !this.settingsView;
      if (this.settingsView) {
         this.moduleRowOpen = false;
      }

      this.islandWhoosh();
   }

   void closeSettingsView() {
      this.settingsView = false;
   }

   boolean isModulesRowOpen() {
      return this.settingsView && this.moduleRowOpen;
   }

   void openModulesRow(boolean open) {
      if (open && !this.moduleRowOpen) {
         this.island.modulesPanel().reset();
      }

      if (open != this.moduleRowOpen) {
         this.islandWhoosh();
      }

      this.moduleRowOpen = open;
   }

   void openMenuAtModules(IslandItem item) {
      this.settingsView = false;
      this.moduleRowOpen = false;
      FuriahScreen.openAt(this.getId(), item == null ? null : "module:" + item.id, item == null ? "modules" : null);
   }

   @Override
   public boolean onScreenKey(GuiScreen screen) {
      if (this.isModulesRowOpen() && Keyboard.getEventKeyState() && Keyboard.getEventKey() == 1 && ChatDetect.isChat(screen)) {
         this.openModulesRow(false);
         return true;
      } else {
         return false;
      }
   }

   void setAllSections(boolean on) {
      this.suppressSections = true;

      try {
         for (Module m : this.island.settingsView().sections()) {
            m.setEnabled(on);
         }
      } finally {
         this.suppressSections = false;
      }

      this.post(
         new WaterMarkModule.Notice(
            WaterMarkModule.Notice.Kind.SECTION, "section:*", on ? "All sections enabled" : "All sections disabled", on ? -12723068 : -43674, null, null
         )
      );
   }

   List<IslandItem> allItems() {
      return this.all;
   }

   @Override
   public String summary() {
      int n = 0;

      for (IslandItem item : this.middleItems()) {
         if (this.isShown(item)) {
            n++;
         }
      }

      return n + (n == 1 ? " module" : " modules");
   }

   @Override
   public boolean hasMoveHud() {
      return true;
   }

   List<IslandItem> middleItems() {
      if (this.order.version() != this.orderVersion) {
         this.orderVersion = this.order.version();
         this.ordered.clear();

         for (String id : this.order.get()) {
            IslandItem item = this.find(id);
            if (item != null && item != this.fv && item != this.gear && !this.ordered.contains(item)) {
               this.ordered.add(item);
            }
         }

         for (IslandItem item : this.all) {
            if (item != this.fv && item != this.gear && !this.ordered.contains(item)) {
               this.ordered.add(item);
            }
         }
      }

      return this.ordered;
   }

   IslandItem find(String id) {
      for (int i = 0; i < this.all.size(); i++) {
         if (this.all.get(i).id.equals(id)) {
            return this.all.get(i);
         }
      }

      return null;
   }

   void moveItem(int from, int to) {
      List<IslandItem> list = this.middleItems();
      if (from >= 0 && to >= 0 && from < list.size() && to < list.size() && from != to) {
         ArrayList<String> ids = new ArrayList<>(list.size());

         for (IslandItem item : list) {
            ids.add(item.id);
         }

         ids.add(to, ids.remove(from));
         this.order.set(ids);
      }
   }

   boolean isShown(IslandItem item) {
      return item.shown == null || item.shown.get();
   }

   int accentNow(double time) {
      return this.accent.getColor(0.0F, time) | 0xFF000000;
   }

   public float[] drawPanelSwitchForColorCheck(float x, float y, float w, float h) {
      return this.island.panelSwitchForCheck(x, y, w, h);
   }

   public int panelSwitchColorForCheck() {
      return this.island.panelSwitchColor | 0xFF000000;
   }

   public float[] drawIslandForColorCheck(float x, float y, float w, float h) {
      FvItem.colorCheck = true;

      try {
         this.island.renderPreview(x, y, w, h, -10000.0F, -10000.0F);
      } finally {
         FvItem.colorCheck = false;
      }

      return this.island.fvBoxForCheck();
   }

   public int[] fvColorForCheck() {
      return new int[]{this.fv.lastColor | 0xFF000000, this.fvColor.get() == WaterMarkModule.FvColor.ACCENT ? 1 : 0};
   }

   public int lookAccent(double time) {
      return this.accentNow(time);
   }

   public float lookOpacity() {
      return this.opacity.getFloat();
   }

   public WaterMarkModule.GlassMode lookGlass() {
      return this.glassMode.get();
   }

   public boolean lookOutline() {
      return this.outline.get();
   }

   public boolean lookShadow() {
      return this.shadow.get();
   }

   public boolean glintOn() {
      return this.glint.get();
   }

   public float glintEvery() {
      return this.glintEvery.getFloat();
   }

   public float glintSweep() {
      return this.glintTime.getFloat();
   }

   public WaterMarkModule.GlintDirection glintDirection() {
      return this.glintDirection.get();
   }

   public float glintTilt() {
      return this.glintAngle.getFloat();
   }

   public float glintWidth() {
      return this.glintWidth.getFloat();
   }

   public float glintStrength() {
      return this.glintStrength.getFloat();
   }

   public WaterMarkModule.GlintColor glintColor() {
      return this.glintColor.get();
   }

   Minecraft mc() {
      return Minecraft.getMinecraft();
   }

   public float[] islandBounds() {
      return this.isEnabled() ? this.island.boundsGui() : null;
   }

   public float[] islandBoundsAt(WaterMarkModule.Anchor a) {
      return this.island.boundsAtGui(a);
   }

   public WaterMarkModule.Anchor islandAnchor() {
      return this.anchor.get();
   }

   public void setIslandAnchor(WaterMarkModule.Anchor a) {
      this.anchor.set(a);
   }

   public SliderSetting islandScale() {
      return this.scale;
   }

   public void resetIslandPlace() {
      this.anchor.reset();
      this.offsetX.reset();
      this.offsetY.reset();
      this.scale.reset();
   }

   void post(WaterMarkModule.Notice n) {
      if (this.notifications.get() && this.isEnabled()) {
         switch (n.kind) {
            case SECTION:
               if (!this.notifySections.get()) {
                  return;
               }
               break;
            case PRESET:
               if (!this.notifyPresets.get()) {
                  return;
               }
               break;
            case TRACK:
               if (!this.notifyTrack.get()) {
                  return;
               }
               break;
            case SOURCE:
               if (!this.notifySource.get()) {
                  return;
               }
         }

         Iterator<WaterMarkModule.Notice> it = this.queue.iterator();

         while (it.hasNext()) {
            if (it.next().key.equals(n.key)) {
               it.remove();
            }
         }

         this.queue.addLast(n);

         while (this.queue.size() > 6) {
            this.queue.pollFirst();
         }
      }
   }

   public void postKill(String text, int streak) {
      if (this.notifications.get() && this.isEnabled()) {
         WaterMarkModule.Notice n = new WaterMarkModule.Notice(WaterMarkModule.Notice.Kind.KILL, "kill", text, 0, IslandIcons.Icon.SKULL, null);
         Iterator<WaterMarkModule.Notice> it = this.queue.iterator();

         while (it.hasNext()) {
            if (it.next().kind == WaterMarkModule.Notice.Kind.KILL) {
               it.remove();
            }
         }

         n.startedMs = ClientClock.millis();
         this.current = n;
         if (this.islandSounds.get() && this.soundKill.get()) {
            ModSounds.island(Sfx.KILL, streakPitch(this.soundStreak.get() ? streak : 1), this.islandVolume.getFloat() / 100.0F);
         }
      }
   }

   static float streakPitch(int streak) {
      float[] steps = new float[]{1.0F, 1.1892F, 1.3348F, 1.4983F, 1.7818F};
      return steps[Math.max(0, Math.min(steps.length, streak) - 1)];
   }

   WaterMarkModule.Notice notice(long nowMs) {
      if (this.isModulesRowOpen()) {
         if (this.current != null) {
            this.queue.addFirst(this.current);
            this.current = null;
         }

         return null;
      } else {
         long duration = Math.round(this.notifyDuration.get() * 1000.0);
         if (this.current != null && nowMs - this.current.startedMs < duration) {
            return this.current;
         } else {
            this.current = this.queue.pollFirst();
            if (this.current != null) {
               this.current.startedMs = nowMs;
               this.playShown(this.current);
            }

            return this.current;
         }
      }
   }

   private void playShown(WaterMarkModule.Notice n) {
      if (n.sound != null && !n.silent) {
         float volume = this.islandVolume.getFloat() / 100.0F;
         if (n.preview) {
            ModSounds.preview(n.sound, n.pitch, volume);
         } else if (this.islandSounds.get() && this.soundOn(n.kind)) {
            ModSounds.island(n.sound, n.pitch, volume);
         }
      }
   }

   private boolean soundOn(WaterMarkModule.Notice.Kind kind) {
      switch (kind) {
         case TRACK:
            return this.soundNowPlaying.get();
         case KILL:
            return this.soundKill.get();
         default:
            return this.soundOther.get();
      }
   }

   void islandClick() {
      if (this.islandSounds.get() && this.soundClicks.get()) {
         ModSounds.island(Sfx.CLICK, 1.0F, this.islandVolume.getFloat() / 100.0F);
      }
   }

   void islandToggle(boolean on) {
      if (this.islandSounds.get() && this.soundClicks.get()) {
         ModSounds.island(on ? Sfx.TOGGLE_ON : Sfx.TOGGLE_OFF, 1.0F, this.islandVolume.getFloat() / 100.0F);
      }
   }

   void islandConfirm() {
      if (this.islandSounds.get() && this.soundClicks.get()) {
         ModSounds.island(Sfx.CONFIRM, 1.0F, this.islandVolume.getFloat() / 100.0F);
      }
   }

   void islandWhoosh() {
      if (this.islandSounds.get() && this.soundWhoosh.get()) {
         ModSounds.island(Sfx.WHOOSH, 1.0F, this.islandVolume.getFloat() / 100.0F);
      }
   }

   public void accountSkinUpdated() {
      this.post(new WaterMarkModule.Notice(WaterMarkModule.Notice.Kind.SECTION, "account-skin", "Account skin updated", -12723068, null, null));
   }

   public void soundMuteChanged(boolean muted) {
      this.post(
         new WaterMarkModule.Notice(
            WaterMarkModule.Notice.Kind.INFO,
            "sound-mute",
            muted ? "Mod sounds off" : "Mod sounds on",
            0,
            muted ? IslandIcons.Icon.SPEAKER_OFF : IslandIcons.Icon.SPEAKER,
            null
         )
      );
   }

   public void uiNotice(String key, String text, IslandIcons.Icon icon) {
      WaterMarkModule.Notice n = new WaterMarkModule.Notice(WaterMarkModule.Notice.Kind.INFO, "ui:" + key, text, 0, icon, null);
      if (this.current != null && this.current.key.equals(n.key) && this.notifications.get() && this.isEnabled()) {
         n.startedMs = ClientClock.millis();
         this.current = n;
      } else {
         this.post(n);
      }
   }

   private Runnable sample(final WaterMarkModule.Notice.Kind kind) {
      return new Runnable() {
         @Override
         public void run() {
            WaterMarkModule.this.showSample(kind, 1);
         }
      };
   }

   void showSample(WaterMarkModule.Notice.Kind kind, int streak) {
      WaterMarkModule.Notice n;
      switch (kind) {
         case TRACK:
            n = new WaterMarkModule.Notice(kind, "sample", "A new song  —  An artist", 0, IslandIcons.Icon.NOTE, null).sound(Sfx.NOTIFY);
            break;
         case KILL:
            n = new WaterMarkModule.Notice(kind, "sample", "Steve" + (streak > 1 ? "  ×" + streak : ""), 0, IslandIcons.Icon.SKULL, null).sound(Sfx.KILL);
            n.pitch = streakPitch(streak);
            break;
         default:
            n = new WaterMarkModule.Notice(WaterMarkModule.Notice.Kind.SOURCE, "sample", "Audio source: Chrome", 0, IslandIcons.Icon.APP, null)
               .sound(Sfx.NOTIFY);
      }

      n.preview = true;
      n.startedMs = ClientClock.millis();
      this.current = n;
      this.playShown(n);
   }

   private void clearNotices() {
      this.queue.clear();
      this.current = null;
   }

   WaterMarkModule.Notice currentNotice() {
      return this.current;
   }

   static String preview(String text) {
      String t = text.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
      return t.length() > 90 ? t.substring(0, 89).trim() + "…" : t;
   }

   public void partSwitched(String id, String name, boolean on) {
      if (!(Minecraft.getMinecraft().currentScreen instanceof FuriahScreen)) {
         this.post(
            new WaterMarkModule.Notice(
               WaterMarkModule.Notice.Kind.SECTION, "section:" + id, name + (on ? " enabled" : " disabled"), on ? -12723068 : -43674, null, null
            )
         );
      }
   }

   public void partOnOff(String id, String name, boolean on) {
      if (!(Minecraft.getMinecraft().currentScreen instanceof FuriahScreen)) {
         this.post(
            new WaterMarkModule.Notice(WaterMarkModule.Notice.Kind.SECTION, "section:" + id, name + (on ? " on" : " off"), on ? -12723068 : -43674, null, null)
         );
      }
   }

   public void settingNotice(String id, String text) {
      if (!(Minecraft.getMinecraft().currentScreen instanceof FuriahScreen)) {
         this.post(new WaterMarkModule.Notice(WaterMarkModule.Notice.Kind.SECTION, "section:" + id, text, -12723068, null, null));
      }
   }

   private void sectionSwitched(Module m) {
      if (!this.suppressSections && (this.config == null || !this.config.isLoading()) && (this.presets == null || !this.presets.isApplying())) {
         GuiScreen screen = Minecraft.getMinecraft().currentScreen;
         if (!(screen instanceof FuriahScreen)) {
            boolean on = m.isEnabled();
            this.post(
               new WaterMarkModule.Notice(
                  WaterMarkModule.Notice.Kind.SECTION,
                  "section:" + m.getId(),
                  m.getName() + (on ? " enabled" : " disabled"),
                  on ? -12723068 : -43674,
                  null,
                  null
               )
            );
         }
      }
   }

   @Override
   public void presetLoaded(String name, String warning) {
      this.post(new WaterMarkModule.Notice(WaterMarkModule.Notice.Kind.PRESET, "preset", "Preset '" + name + "' loaded", 0, IslandIcons.Icon.LAYERS, null));
      if (warning != null) {
         this.post(new WaterMarkModule.Notice(WaterMarkModule.Notice.Kind.INFO, "preset-warning", warning, -43674, null, warning));
      }
   }

   @Override
   public void onClientTick() {
      this.tickItems();
      boolean sys = this.isShown(this.system);
      this.stats.want(sys && this.system.wantsCpu(), sys && this.system.wantsGpu());
      IslandCheck.tick();
      if (!this.island.isInteractive() && !IslandCheck.isRunning()) {
         this.settingsView = false;
      }
   }

   private void tickItems() {
      this.lastTickMs = System.currentTimeMillis();
      List<IslandItem> items = this.all;

      for (int i = 0; i < items.size(); i++) {
         IslandItem item = items.get(i);
         if (this.isShown(item)) {
            item.tick();
         }
      }
   }

   void ensureTicked() {
      if (System.currentTimeMillis() - this.lastTickMs > 100L) {
         this.tickItems();
      }
   }

   public void wantLevelsForSky(boolean on) {
   }

   public String musicUnavailableReason() {
      return "Needs the WaterMark's Audio module, which this offline build does not include";
   }

   public float musicBass() {
      return -1.0F;
   }

   @Override
   public void onRenderHud(float partialTicks) {
      this.island.renderHud();
   }

   @Override
   public void onScreenDrawn(GuiScreen screen) {
      this.island.renderOverScreen(screen);
   }

   @Override
   public boolean onScreenMouse(GuiScreen screen) {
      return this.island.mouseInput(screen);
   }

   @Override
   public void onWorldChange() {
      this.bps.reset();
   }

   @Override
   protected void onEnable() {
   }

   @Override
   protected void onDisable() {
      this.stats.stop();
      this.clearNotices();
      this.settingsView = false;
   }

   public void shutdown() {
      this.stats.stop();
   }

   @Override
   public int getAccentColor(double time) {
      return this.accentNow(time);
   }

   @Override
   public boolean isGroupCollapsible(String group) {
      return "Weather".equals(group) || "Motion".equals(group) || "Visibility".equals(group) || "Glint".equals(group);
   }

   @Override
   public boolean isGroupCollapsedByDefault(String group) {
      return "Weather".equals(group) ? !this.isShown(this.weatherItem) : false;
   }

   @Override
   public String getPreviewInfo() {
      return this.island.costInfo();
   }

   @Override
   public void resetToDefaults() {
      for (Setting s : this.getSettings()) {
         if (s != this.enabled && !s.isPersonal()) {
            s.reset();
         }
      }
   }

   public float[] pillForTest() {
      return this.island.pillForTest();
   }

   public float costForTest() {
      return this.island.costForTest();
   }

   public float visibilityForTest() {
      return this.island.visibilityForTest();
   }

   public int layerForTest() {
      return this.island.layerForTest();
   }

   public boolean clickForTest(float x, float y) {
      return this.island.clickForTest(x, y);
   }

   public String tooltipForTest(float x, float y) {
      IslandItem.Hit h = this.island.hitForTest(x, y);
      return h == null ? null : h.tooltip;
   }

   public float[] blockForTest(String id) {
      IslandItem item = this.find(id);
      return item == null ? null : this.island.blockForTest(item);
   }

   public float[] chipForTest(int n) {
      return this.island.chipForTest(n);
   }

   public boolean glassForTest() {
      return IslandGlass.isAvailable();
   }

   public List<String> orderForTest() {
      ArrayList<String> ids = new ArrayList<>();
      ids.add(this.fv.id);

      for (IslandItem item : this.middleItems()) {
         ids.add(item.id);
      }

      ids.add(this.gear.id);
      return ids;
   }

   public void showItemForTest(String id, boolean on) {
      IslandItem item = this.find(id);
      if (item != null && item.shown != null) {
         item.shown.set(on);
      }
   }

   public void moveItemForTest(int from, int to) {
      this.moveItem(from, to);
   }

   public void noticeForTest(String text, boolean on) {
      this.post(new WaterMarkModule.Notice(WaterMarkModule.Notice.Kind.SECTION, "test:" + text, text, on ? -12723068 : -43674, null, null));
   }

   public String currentNoticeForTest() {
      return this.current == null ? null : this.current.text;
   }

   public String lastQueuedNoticeForTest() {
      return this.queue.isEmpty() ? null : this.queue.peekLast().text;
   }

   public String weatherForTest() {
      this.weatherItem.tick();
      return this.weatherItem.panelValue();
   }

   public int queuedNoticesForTest() {
      return this.queue.size();
   }

   public void clearNoticesForTest() {
      this.clearNotices();
   }

   public void sampleForTest(String kind, int streak) {
      WaterMarkModule.Notice.Kind k = "dm".equals(kind)
         ? WaterMarkModule.Notice.Kind.DISCORD_DM
         : (
            "mention".equals(kind)
               ? WaterMarkModule.Notice.Kind.DISCORD_MENTION
               : (
                  "voice".equals(kind)
                     ? WaterMarkModule.Notice.Kind.DISCORD_VOICE
                     : (
                        "kill".equals(kind)
                           ? WaterMarkModule.Notice.Kind.KILL
                           : ("track".equals(kind) ? WaterMarkModule.Notice.Kind.TRACK : WaterMarkModule.Notice.Kind.SOURCE)
                     )
               )
         );
      this.showSample(k, streak);
   }

   public int chipIndexForTest(String id) {
      List<Module> s = this.island.settingsView().sections();

      for (int i = 0; i < s.size(); i++) {
         if (s.get(i).getId().equals(id)) {
            return i;
         }
      }

      return -1;
   }

   public void quickSettingsClickForTest(int chip) {
      this.island.settingsView().sections();
      this.island.settingsView().click(chip < 0 ? 450 : 200 + chip);
   }

   public SystemStats statsForTest() {
      return this.stats;
   }

   public void settingsViewForTest(boolean open) {
      this.settingsView = open;
      this.moduleRowOpen = false;
   }

   public float[] panelForTest() {
      return this.island.panelForTest();
   }

   public float[] panelRowForTest(String id) {
      return this.island.panelRowForTest(id);
   }

   public float[] panelButtonsForTest() {
      return this.island.panelButtonsForTest();
   }

   public List<String> panelDetailsForTest() {
      return this.island.modulesPanel().detailsForTest();
   }

   public boolean panelWheelForTest(int dwheel) {
      return this.island.modulesPanel().wheel(dwheel);
   }

   public boolean modulesRowOpenForTest() {
      return this.isModulesRowOpen();
   }

   public boolean escapeForTest() {
      if (this.isModulesRowOpen()) {
         this.openModulesRow(false);
         return true;
      } else {
         return false;
      }
   }

   public void roomForTest(float units) {
      this.island.roomForTest = units;
   }

   public float[] fvOffsetForTest() {
      return this.island.fvOffsetForTest();
   }

   public void glintForTest(float progress) {
      this.island.glintForTest(progress);
   }

   public void fvEffectTimeForTest(double seconds) {
      this.fv.effectTimeForTest(seconds);
   }

   public int[] colorsForTest() {
      IslandStyle s = this.island.styleForTest();
      float[] p = this.island.pillForTest();
      float mid = p[0] + p[2] / 2.0F;
      return new int[]{
         this.fv.lastColor,
         this.fps.lastIconColor,
         this.weatherItem.lastIconColor,
         this.text.lastColor,
         this.xyz.lastLetterColor,
         s.globalAt(mid),
         s.accentAt(mid)
      };
   }

   public void expandRowForTest(String id) {
      this.expandRequest = id;
   }

   String takeExpandRequest() {
      String id = this.expandRequest;
      this.expandRequest = null;
      return id;
   }

   public Boolean currentNoticeSilentForTest() {
      return this.current == null ? null : this.current.silent;
   }

   public String currentNoticeActionForTest() {
      return this.current == null ? null : this.current.actionTip;
   }

   public static void saveIconsForTest(File file) throws IOException {
      ImageIO.write(IslandIcons.paint(), "png", file);
   }

   public List<String> personalIdsForTest() {
      ArrayList<String> ids = new ArrayList<>();

      for (Setting s : this.getSettings()) {
         if (s.isPersonal()) {
            ids.add(s.getId());
         }
      }

      return Collections.unmodifiableList(ids);
   }

   public static enum Anchor implements Displayable {
      TOP_LEFT("Top Left"),
      TOP_CENTER("Top Center"),
      TOP_RIGHT("Top Right"),
      BOTTOM_LEFT("Bottom Left"),
      BOTTOM_RIGHT("Bottom Right");

      private final String displayName;

      private Anchor(String displayName) {
         this.displayName = displayName;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return "The island sits in the " + this.displayName.toLowerCase() + " of the screen.";
      }

      boolean isLeft() {
         return this == TOP_LEFT || this == BOTTOM_LEFT;
      }

      boolean isRight() {
         return this == TOP_RIGHT || this == BOTTOM_RIGHT;
      }

      boolean isBottom() {
         return this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
      }
   }

   public static enum FvColor implements Displayable {
      ACCENT("Accent", "The island's accent color", IslandStyle.Paint.ACCENT),
      GLOBAL("Global Color", "The Global Color (static, gradient or chroma)", IslandStyle.Paint.GLOBAL),
      TEXT("Text", "The text color", IslandStyle.Paint.TEXT);

      private final String displayName;
      private final String description;
      final IslandStyle.Paint paint;

      private FvColor(String displayName, String description, IslandStyle.Paint paint) {
         this.displayName = displayName;
         this.description = description;
         this.paint = paint;
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

   public static enum GlassMode implements Displayable {
      AUTO("Auto", "Glass on the glass themes, solid on the others"),
      ON("On", "Always frosted glass"),
      OFF("Off", "Always solid");

      private final String displayName;
      private final String description;

      private GlassMode(String displayName, String description) {
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

   public static enum GlintColor implements Displayable {
      AUTO("Auto", "White, or the accent on Light Mode (white does not show on a white island)"),
      WHITE("White", "A white shine"),
      ACCENT("Accent", "The island's accent"),
      GLOBAL("Global", "The Global Color");

      private final String displayName;
      private final String description;

      private GlintColor(String displayName, String description) {
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

   public static enum GlintDirection implements Displayable {
      LEFT_TO_RIGHT("Left to Right", "Every glint starts at the left end"),
      RIGHT_TO_LEFT("Right to Left", "Every glint starts at the right end"),
      BACK_AND_FORTH("Back and Forth", "Left to right, then right to left");

      private final String displayName;
      private final String description;

      private GlintDirection(String displayName, String description) {
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

   public static enum GradientSpread implements Displayable {
      SAME("Same Everywhere", "Every part set to the same color shows exactly the same color"),
      ACROSS("Across the Island", "Gradients and chroma run from one end of the island to the other");

      private final String displayName;
      private final String description;

      private GradientSpread(String displayName, String description) {
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

   public static enum IconTint implements Displayable {
      SOFT("Soft", "The text color with a touch of the accent", IslandStyle.Paint.SOFT),
      ACCENT("Accent", "The island's accent", IslandStyle.Paint.ACCENT),
      TEXT("Text", "The text color", IslandStyle.Paint.TEXT),
      GLOBAL("Global", "The Global Color (static, gradient or chroma)", IslandStyle.Paint.GLOBAL);

      private final String displayName;
      private final String description;
      final IslandStyle.Paint paint;

      private IconTint(String displayName, String description, IslandStyle.Paint paint) {
         this.displayName = displayName;
         this.description = description;
         this.paint = paint;
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

   public static enum Listen implements Displayable {
      EXCEPT_IGNORED("All but Ignored", "Every app's sound except the ignored apps (and Minecraft)"),
      MEDIA_APP("Media App Only", "Only the app that plays the music"),
      ALL("All System Audio", "Everything you hear (Minecraft too)");

      private final String displayName;
      private final String description;

      private Listen(String displayName, String description) {
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

      String wire() {
         return this == EXCEPT_IGNORED ? "except" : (this == MEDIA_APP ? "media" : "all");
      }
   }

   static final class Notice {
      final WaterMarkModule.Notice.Kind kind;
      final String key;
      final String text;
      final int dot;
      final IslandIcons.Icon icon;
      final String tooltip;
      Runnable action;
      String actionTip;
      Sfx sound;
      float pitch = 1.0F;
      boolean silent;
      boolean preview;
      long startedMs;
      float textW = -1.0F;
      int measuredPx;
      String shown;

      Notice(WaterMarkModule.Notice.Kind kind, String key, String text, int dot, IslandIcons.Icon icon, String tooltip) {
         this.kind = kind;
         this.key = key;
         this.text = text;
         this.dot = dot;
         this.icon = icon;
         this.tooltip = tooltip;
      }

      WaterMarkModule.Notice onClick(Runnable action, String tip) {
         this.action = action;
         this.actionTip = tip;
         return this;
      }

      WaterMarkModule.Notice sound(Sfx sfx) {
         this.sound = sfx;
         return this;
      }

      boolean isDiscord() {
         return this.kind == WaterMarkModule.Notice.Kind.DISCORD_DM
            || this.kind == WaterMarkModule.Notice.Kind.DISCORD_MENTION
            || this.kind == WaterMarkModule.Notice.Kind.DISCORD_VOICE;
      }

      static enum Kind {
         SECTION,
         PRESET,
         TRACK,
         SOURCE,
         INFO,
         KILL,
         DISCORD_DM,
         DISCORD_MENTION,
         DISCORD_VOICE;
      }
   }

   public static enum PanelColumns implements Displayable {
      AUTO("Auto", "Two; one on narrow screens or a big island, three on very wide screens", 0),
      ONE("1", "One column", 1),
      TWO("2", "Two columns", 2),
      THREE("3", "Three columns", 3);

      private final String displayName;
      private final String description;
      final int count;

      private PanelColumns(String displayName, String description, int count) {
         this.displayName = displayName;
         this.description = description;
         this.count = count;
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

   public static enum SourcePref implements Displayable {
      CURRENT("Current", "The player Windows shows in its media controls"),
      RECENT("Most Recent", "The player that started playing last"),
      ONLY("Only These Apps", "Only the apps in your list below");

      private final String displayName;
      private final String description;

      private SourcePref(String displayName, String description) {
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

      String wire() {
         return this == CURRENT ? "current" : (this == RECENT ? "recent" : "only");
      }
   }

   public static enum Units implements Displayable {
      CELSIUS("°C", "Degrees Celsius"),
      FAHRENHEIT("°F", "Degrees Fahrenheit");

      private final String displayName;
      private final String description;

      private Units(String displayName, String description) {
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

   public static enum VisStyle implements Displayable {
      BARS("Bars", "Five bars dancing to the music"),
      BASS_PULSE("Bass Pulse", "A dot that pulses with the bass");

      private final String displayName;
      private final String description;

      private VisStyle(String displayName, String description) {
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

   public static enum WeatherSource implements Displayable {
      INGAME("In-game", "The world's own weather (no internet)"),
      REAL("Real", "Your city's weather from Open-Meteo");

      private final String displayName;
      private final String description;

      private WeatherSource(String displayName, String description) {
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
