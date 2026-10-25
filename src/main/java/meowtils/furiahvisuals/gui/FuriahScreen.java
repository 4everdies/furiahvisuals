package meowtils.furiahvisuals.gui;

import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.Keybinds;
import meowtils.furiahvisuals.core.ModuleManager;
import meowtils.furiahvisuals.gui.layout.LayoutEntry;
import meowtils.furiahvisuals.gui.layout.LayoutGroup;
import meowtils.furiahvisuals.gui.layout.LayoutPage;
import meowtils.furiahvisuals.gui.layout.SectionLayout;
import meowtils.furiahvisuals.gui.layout.SectionLayouts;
import meowtils.furiahvisuals.gui.layout.Slot;
import meowtils.furiahvisuals.gui.popup.ChoicePopover;
import meowtils.furiahvisuals.gui.popup.ColorPopover;
import meowtils.furiahvisuals.gui.popup.ContextMenu;
import meowtils.furiahvisuals.gui.popup.Popovers;
import meowtils.furiahvisuals.gui.search.SearchBar;
import meowtils.furiahvisuals.gui.search.SearchEntry;
import meowtils.furiahvisuals.gui.search.SearchIndex;
import meowtils.furiahvisuals.gui.search.SearchIndexes;
import meowtils.furiahvisuals.gui.search.SearchList;
import meowtils.furiahvisuals.gui.search.SearchResult;
import meowtils.furiahvisuals.gui.widget.CardsWidget;
import meowtils.furiahvisuals.gui.widget.ChipsWidget;
import meowtils.furiahvisuals.gui.widget.CollapsiblePreview;
import meowtils.furiahvisuals.gui.widget.ColorWidget;
import meowtils.furiahvisuals.gui.widget.HeaderWidget;
import meowtils.furiahvisuals.gui.widget.HintWidget;
import meowtils.furiahvisuals.gui.widget.ItemListWidget;
import meowtils.furiahvisuals.gui.widget.ItemRulesWidget;
import meowtils.furiahvisuals.gui.widget.NoteWidget;
import meowtils.furiahvisuals.gui.widget.NoticeWidget;
import meowtils.furiahvisuals.gui.widget.PageLinkWidget;
import meowtils.furiahvisuals.gui.widget.PresetsWidget;
import meowtils.furiahvisuals.gui.widget.ScrollBar;
import meowtils.furiahvisuals.gui.widget.Segmented;
import meowtils.furiahvisuals.gui.widget.SegmentedWidget;
import meowtils.furiahvisuals.gui.widget.SliderWidget;
import meowtils.furiahvisuals.gui.widget.StartingLookWidget;
import meowtils.furiahvisuals.gui.widget.TextFieldWidget;
import meowtils.furiahvisuals.gui.widget.ToggleWidget;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.module.weapons.WeaponsModule;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.ItemListSetting;
import meowtils.furiahvisuals.setting.ItemRulesSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.PresetSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingDefaults;
import meowtils.furiahvisuals.setting.SettingStyle;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.TextSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class FuriahScreen extends GuiScreen {
   private static final float SIDEBAR_W = 164.0F;
   private static final float SIDEBAR_ICONS = 46.0F;
   private static final float AUTO_ICONS_BELOW = 520.0F;
   private static final float HEADER_H = 64.0F;
   private float sideW = 164.0F;
   private float sideTarget = 164.0F;
   private boolean sidePlaced;
   private MenuPrefs.SidebarStyle sideMode = MenuPrefs.SidebarStyle.FULL;
   private float headerH = 64.0F;
   private final SearchBar globalBar = new SearchBar();
   private GlobalSearch globalSearch;
   private boolean globalPage;
   private boolean globalPressed;
   private float globalX = -10000.0F;
   private float globalY;
   private float globalW;
   private float globalH;
   private float magX = -10000.0F;
   private float magY;
   private float magS;
   private float magHover;
   private float hdrSectionsX = -10000.0F;
   private float hdrSearchX = -10000.0F;
   private float hdrSectionsHover;
   private float hdrSearchHover;
   private final Map<Module, float[]> dotAnim = new IdentityHashMap<>();
   private final List<FuriahScreen.NavEntry> navEntries = new ArrayList<>();
   private final Map<Object, float[]> navAnim = new HashMap<>();
   private float navContent;
   private float backX;
   private float backY;
   private float backS = 20.0F;
   private boolean backShown;
   private float searchX;
   private float searchW;
   private float searchH = 20.0F;
   private float moveX = -10000.0F;
   private float moveY;
   private float moveW;
   private float moveHover;
   private float hdrSettingsX = -10000.0F;
   private float hdrPresetsX = -10000.0F;
   private float hdrIconY;
   private float hdrSettingsHover;
   private float hdrPresetsHover;
   private float buttonH = 22.0F;
   private float switchW = 38.0F;
   private float switchH = 20.0F;
   private float setH = 30.0F;
   private final TabRow tabRow = new TabRow();
   private boolean tabsShown;
   private float tabSlideX;
   private final List<float[]> actionBoxes = new ArrayList<>();
   private final List<Module.HeaderAction> actionList = new ArrayList<>();
   private final Map<Module.HeaderAction, float[]> actionHover = new IdentityHashMap<>();
   private boolean actionsIconOnly;
   private float jumpY;
   private float jumpH;
   private boolean jumpWanted;
   private int jumpGroups;
   private LayoutPage jumpPage;
   private final List<float[]> jumpChips = new ArrayList<>();
   private final List<HeaderWidget> jumpHeaders = new ArrayList<>();
   private float collapseX;
   private float collapseW;
   private float collapseHover;
   private final Map<HeaderWidget, Float> chipHover = new IdentityHashMap<>();
   private HeaderWidget jumpPick;
   private HeaderWidget jumpTarget;
   private int jumpFrames;
   private static final float COLUMN_GAP = 18.0F;
   private int splitAt = -1;
   private String splitFor;
   private final HeaderWidget[] sticky = new HeaderWidget[2];
   private final float[] stickyY = new float[2];
   private final float[] stickyClip = new float[2];
   private final float[] columnX = new float[2];
   private float columnW;
   private int columns = 1;
   private float lastListTop = Float.NaN;
   private float rowClipX;
   private float rowClipW;
   private float rowClipTop;
   private static final float PREVIEW_W = 206.0F;
   private float previewW = 206.0F;
   private static final float PAD = 18.0F;
   private static final float SIDE_INSET = 5.0F;
   private static final float OPEN_SECONDS = 0.22F;
   private static final float CLOSE_SECONDS = 0.14F;
   private static final float OFFSCREEN = -10000.0F;
   private final GuiScreen parent;
   private final ModuleManager modules;
   private Module module;
   private final List<Widget> widgets = new ArrayList<>();
   private final List<HeaderWidget> widgetHeaders = new ArrayList<>();
   private final List<LayoutPage> widgetPages = new ArrayList<>();
   private final Map<HeaderWidget, LayoutGroup> headerGroups = new IdentityHashMap<>();
   private final List<Integer> widgetDepths = new ArrayList<>();
   private final List<Integer> widgetParents = new ArrayList<>();
   private SectionLayout layout;
   private LayoutPage page;
   private float pageFade = 1.0F;
   private static final float SUB_INDENT = 14.0F;
   private int[][] rowChildren = new int[0][];
   private boolean[] rowOpen = new boolean[0];
   private float[] arrowTurn = new float[0];
   private static final float ARROW_W = 12.0F;
   private static final float COL_GAP = 14.0F;
   private Density.Mode shownDensity;
   private float anchorWidth = -1.0F;
   private long lastListWheel;
   private float rowMx;
   private float rowMy;
   private boolean rowMouseIn;
   private String rowTip;
   private float backHover;
   private final MenuNav.Host navHost = new MenuNav.Host() {
      @Override
      public boolean openPage(Module m, String key) {
         if (m == FuriahScreen.this.module && FuriahScreen.this.layout != null) {
            LayoutPage target = FuriahScreen.this.layout.page(key);
            if (target == null) {
               return false;
            } else {
               FuriahScreen.this.showPage(target, true);
               return true;
            }
         } else {
            return false;
         }
      }

      @Override
      public boolean openStylePage(Module m, SettingStyle style) {
         if (m == FuriahScreen.this.module && FuriahScreen.this.layout != null) {
            LayoutPage target = FuriahScreen.this.layout.stylePage(style);
            if (target == null) {
               return false;
            } else {
               FuriahScreen.this.showPage(target, true);
               return true;
            }
         } else {
            return false;
         }
      }
   };
   private final PreviewRenderer preview = new PreviewRenderer();
   private boolean docked;
   private float dragFade;
   private static boolean peekHeld;
   private float peekFade;
   private long lastNanos;
   private float open;
   private float openAlpha = 1.0F;
   private boolean closing;
   private long closeAsked;
   private final ScrollBar listBar = new ScrollBar();
   private float listRestore = -1.0F;
   private float settingsRestore = -1.0F;
   private int restoreFrames;
   private ScrollBar barDrag;
   private Widget active;
   private Widget focused;
   private boolean previewDrag;
   private float mouseX = -10000.0F;
   private float mouseY = -10000.0F;
   private int accent = -7635713;
   private boolean accentSet;
   private final ScrollBar navBar = new ScrollBar();
   private Module navShown;
   private boolean navPlaced;
   private float navShownH;
   private float resetHover;
   private float switchHover;
   private float switchKnob = -1.0F;
   private long resetConfirmUntil;
   private String tooltipText;
   private String frameTooltip;
   private float tooltipTime;
   private boolean listeningForKey;
   private float keyHover;
   private float keyListen;
   private boolean settingsPage;
   private float settingsHover;
   private final ScrollBar settingsBar = new ScrollBar();
   private boolean presetsPage;
   private float presetsHover;
   private PresetsPage presetsUi;
   private float presetsX;
   private float presetsY;
   private final ColorWidget globalWidget;
   private final Segmented viewThemeControl = new Segmented();
   private float viewThemeX;
   private float viewThemeY = -10000.0F;
   private float viewThemeW;
   private final Segmented sidebarControl = new Segmented();
   private float sidebarX;
   private float sidebarY = -10000.0F;
   private float sidebarW;
   private final Segmented scaleControl = new Segmented();
   private float scaleX;
   private float scaleY = -10000.0F;
   private float scaleW;
   private final SliderSetting scaleDraft = new SliderSetting(
         "guiScaleFine", "Fine Scale", "The GUI scale in 1 % steps (it applies when you let go).", 75.0, 200.0, 100.0, 1.0
      )
      .unit("%");
   private final SliderWidget scaleWidget = new SliderWidget(this.scaleDraft);
   private boolean syncingDraft;
   private final ToggleWidget fullScreenWidget = new ToggleWidget(MenuPrefs.FULL_SCREEN);
   private float sizeResetX;
   private float sizeResetY = -10000.0F;
   private float sizeResetW;
   private float sizeResetHover;
   private WelcomeGuide guide;
   private float guideButtonX;
   private float guideButtonY = -10000.0F;
   private float guideButtonW;
   private float guideButtonHover;
   public static boolean firstTimeForTest;
   private static final String HINT = "Pick a style, a color and a strength — everything else is optional.";
   private int topRows = 1;
   private final Map<Setting, LayoutGroup> groupOfSetting = new IdentityHashMap<>();
   private boolean resizing;
   private float resizeStartX;
   private float resizeStartY;
   private float resizeStartW;
   private float resizeStartH;
   private float resizeW;
   private float resizeH;
   private float gripHover;
   private long lastGripClick;
   private float panelHShown = -1.0F;
   private final ToggleWidget flashWidget;
   private final SoundsBlock soundsBlock;
   private Object hoverTarget;
   private Widget listHover;
   private float setAllX;
   private float setAllY;
   private float setAllW;
   private float setAllHover;
   private long setAllConfirmUntil;
   private long setAllDoneUntil;
   private int setAllCount;
   private final float[] cardHover = new float[Theme.Style.values().length];
   private final Theme.Palette[] cardPalette = new Theme.Palette[Theme.Style.values().length];
   private float lightX = -0.35F;
   private float lightY = -0.9F;
   private static final float KEY_H = 22.0F;
   private static final float SETTINGS_H = 30.0F;
   private static final float CARD_GAP = 10.0F;
   private static final float NAV_ROW = 28.0F;
   private static final float NAV_FADE = 22.0F;
   private float settingsX;
   private float settingsY;
   private float settingsW;
   private float navTop;
   private float navH;
   private final float[] cardX = new float[Theme.Style.values().length];
   private final float[] cardY = new float[Theme.Style.values().length];
   private float cardW;
   private float cardH;
   private float vw;
   private float vh;
   private float panelX;
   private float panelY;
   private float panelW;
   private float panelH;
   private float panelXShown = -1.0F;
   private float panelWShown = -1.0F;
   private float listX;
   private float listY;
   private float listW;
   private float listH;
   private float previewX;
   private float previewY;
   private float previewH;
   private float switchX = -10000.0F;
   private float switchY = -10000.0F;
   private float resetX;
   private float resetY;
   private float resetW;
   private String bannerReason;
   private float bannerX;
   private float bannerY = -10000.0F;
   private float bannerW;
   private float bannerH;
   private float bannerBtnX = -10000.0F;
   private float bannerBtnW;
   private float bannerBtnHover;
   private float testX;
   private float testY;
   private float testW;
   private float testHover;
   private float keyX;
   private float keyY;
   private float keyW;
   private final SearchBar searchBar = new SearchBar();
   private final Map<String, String> searchTexts = new HashMap<>();
   private String searchFor;
   private float searchY;
   private SectionSearch sectionSearch;
   private SectionSearch settingsSearch;
   private final Map<Setting, Widget> widgetOf = new IdentityHashMap<>();
   private final Map<Widget, Setting> settingOf = new IdentityHashMap<>();
   private boolean searchPressed;
   private SearchEntry highlightNext;
   private final SectionSearch.Host searchHost = new SectionSearch.Host() {
      @Override
      public void reveal(SearchResult result, Widget widget) {
         FuriahScreen.this.showInSection(result, widget);
      }

      @Override
      public void openOther(String page, String query, SearchEntry entry) {
         FuriahScreen.this.openFromSearch(page, query, entry);
      }

      @Override
      public void setQuery(String text) {
         FuriahScreen.this.searchBar.setText(text);
         FuriahScreen.this.searchBar.focus();
      }

      @Override
      public boolean blocked() {
         return FuriahScreen.this.blocked();
      }

      @Override
      public List<SearchIndex> otherIndexes() {
         ArrayList<SearchIndex> out = new ArrayList<>();
         String here = FuriahScreen.this.pageKey();

         for (Module m : FuriahScreen.this.modules.getModules()) {
            if (!m.getId().equals(here)) {
               out.add(SearchIndexes.of(m));
            }
         }

         if (!"#settings".equals(here)) {
            out.add(FuriahScreen.settingsIndex());
         }

         return out;
      }
   };
   private Widget revealWidget;
   private float revealOffset;
   private float revealHeight;
   private int revealFrames;
   private long revealStart;
   private Widget pulseWidget;
   private float pulseOffset;
   private float pulseHeight;
   private long pulseStart;
   private String settingsReveal;
   private long settingsRevealStart;
   private float settingsRevealY;
   private float settingsRevealH;
   private static String pendingReveal;
   private static int pendingRevealTries;
   private Module scrollOwner;
   private boolean sideways;
   private static long drawNanosTotal;
   private static long drawNanosMax;
   private static int drawFrames;
   private int warmNext;
   private static final float BASE_PX = 1.862069F;
   private static final float MIN_PANEL_W = 400.0F;
   private static final float MIN_PANEL_H = 240.0F;
   private static final float PANEL_MARGIN = 20.0F;
   private float shownPx = -1.0F;
   private long zoomToastUntil;
   private boolean headerIcons;
   private Module accentModule;
   private int accentFrom;
   private float accentBlend = 1.0F;
   private static final float TWO_COLUMNS_FROM = 540.0F;
   private float jumpMoreX = -10000.0F;
   private float jumpMoreW;
   private float jumpMoreHover;
   private final List<HeaderWidget> jumpOverflow = new ArrayList<>();
   private static SearchIndex settingsIndexCache;
   private final float[][] previewTests = new float[4][];
   private int previewTestCount;
   private final float[] previewTestHover = new float[4];
   private ColorSetting staticForTest;
   private JsonElement staticBeforeTest;
   private static boolean testCtrl;
   private static boolean testShift;
   private final Set<Integer> typingKeys = new HashSet<>();

   public static void peek(boolean held) {
      peekHeld = held;
   }

   public float peekFadeForTest() {
      return this.peekFade;
   }

   public FuriahScreen(GuiScreen parent) {
      this.parent = parent;
      this.modules = FuriahsVisuals.instance.getModules();
      this.globalWidget = new ColorWidget(FuriahsVisuals.instance.getGlobalColor()).alwaysExpanded();
      this.flashWidget = new ToggleWidget(FuriahsVisuals.instance.getReduceFlashing());
      this.soundsBlock = new SoundsBlock(FuriahsVisuals.instance.getSounds());
      this.scaleDraft.addListener(new Runnable() {
         @Override
         public void run() {
            if (!FuriahScreen.this.syncingDraft && (FuriahScreen.this.active != FuriahScreen.this.scaleWidget || !FuriahScreen.this.scaleWidget.isDragging())) {
               FuriahScreen.this.applyScaleDraft();
            }
         }
      });
      ColorWidget.globalEditor = new Runnable() {
         @Override
         public void run() {
            ModSounds.gui(Sfx.SECTION);
            FuriahScreen.this.openSettings(true);
         }
      };
      ModSounds.gui(Sfx.GUI_OPEN);
      MenuNav.host = this.navHost;
      this.globalSearch = new GlobalSearch(new GlobalSearch.Host() {
         @Override
         public void reveal(String page, String query, SearchResult result) {
            FuriahScreen.this.revealFromGlobal(page, query, result);
         }

         @Override
         public SearchIndex settingsIndex() {
            return FuriahScreen.settingsIndex();
         }
      }, this.modules.getModules());
      String page = MenuMemory.page();
      Module remembered = page != null && !page.startsWith("#") ? this.modules.get(page) : null;
      this.select(remembered != null ? remembered : this.modules.getModules().get(0));
      if ("#settings".equals(page)) {
         this.openSettings(false);
      } else if ("#presets".equals(page)) {
         this.openPresets();
      }

      if (!MenuMemory.guideDone() && firstTimeShown()) {
         this.openGuide();
      }

      GuiCheck.attach(this);
   }

   public static void openAt(String moduleId, String subPage, String revealSettingId) {
      MenuMemory.page(moduleId);
      if (subPage != null) {
         MenuMemory.subPage(moduleId, subPage);
      }

      pendingReveal = revealSettingId;
      Minecraft.getMinecraft().displayGuiScreen(new FuriahScreen(null));
   }

   public static void goTo(String moduleId, String settingId) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.currentScreen instanceof FuriahScreen) {
         FuriahScreen screen = (FuriahScreen)mc.currentScreen;
         Module m = screen.modules.get(moduleId);
         if (m != null) {
            screen.showSection(m);
            pendingReveal = settingId;
            pendingRevealTries = 0;
         }
      } else {
         pendingRevealTries = 0;
         openAt(moduleId, null, settingId);
      }
   }

   private static boolean firstTimeShown() {
      return firstTimeForTest || !Boolean.getBoolean("furiahsvisuals.selftest");
   }

   private void openGuide() {
      this.guide = new WelcomeGuide(
         new WelcomeGuide.Host() {
            @Override
            public float[] sectionSwitches() {
               return FuriahScreen.this.dotsArea();
            }

            @Override
            public boolean sidebarHidden() {
               return FuriahScreen.this.sideMode == MenuPrefs.SidebarStyle.HIDDEN;
            }

            @Override
            public float[] searchBox() {
               float[] fArray;
               if (FuriahScreen.this.globalPage) {
                  fArray = null;
               } else {
                  float[] fArray2 = new float[]{FuriahScreen.this.searchX, FuriahScreen.this.searchY, FuriahScreen.this.searchW, 0.0F};
                  fArray = fArray2;
                  fArray2[3] = FuriahScreen.this.searchH;
               }

               return fArray;
            }

            @Override
            public float[] globalSearch() {
               if (FuriahScreen.this.sideMode == MenuPrefs.SidebarStyle.HIDDEN) {
                  return new float[]{FuriahScreen.this.hdrSearchX, FuriahScreen.this.hdrIconY, FuriahScreen.this.buttonH, FuriahScreen.this.buttonH};
               } else {
                  return FuriahScreen.this.sideMode == MenuPrefs.SidebarStyle.ICONS
                     ? new float[]{FuriahScreen.this.magX, FuriahScreen.this.magY, FuriahScreen.this.magS, FuriahScreen.this.magS}
                     : new float[]{FuriahScreen.this.globalX, FuriahScreen.this.globalY, FuriahScreen.this.globalW, FuriahScreen.this.globalH};
               }
            }

            @Override
            public float[] presetsButton() {
               return FuriahScreen.this.sideMode == MenuPrefs.SidebarStyle.HIDDEN
                  ? new float[]{FuriahScreen.this.hdrPresetsX, FuriahScreen.this.hdrIconY, FuriahScreen.this.buttonH, FuriahScreen.this.buttonH}
                  : new float[]{FuriahScreen.this.presetsX, FuriahScreen.this.presetsY, FuriahScreen.this.setH, FuriahScreen.this.setH};
            }

            @Override
            public void closed(boolean finished) {
               MenuMemory.guideDone(true);
               FuriahScreen.this.guide = null;
            }
         }
      );
   }

   private String pageKey() {
      return this.settingsPage ? "#settings" : (this.presetsPage ? "#presets" : this.module.getId());
   }

   private void rememberScroll() {
      if (this.module != null && (!this.modulePage() || this.scrollOwner == this.module)) {
         if (this.settingsPage) {
            MenuMemory.scroll("#settings", this.settingsRestore >= 0.0F ? this.settingsRestore : this.settingsBar.target);
         } else if (this.presetsPage) {
            if (this.presetsUi != null) {
               MenuMemory.scroll("#presets", this.presetsUi.scrollTarget());
            }
         } else {
            MenuMemory.scroll(this.scrollKey(), this.listRestore >= 0.0F ? this.listRestore : this.listBar.target);
         }
      }
   }

   private String scrollKey() {
      return this.page != null && !this.page.isSection() ? this.module.getId() + "/" + this.page.key : this.module.getId();
   }

   private void cancelRestore() {
      this.listRestore = -1.0F;
      this.settingsRestore = -1.0F;
      this.revealWidget = null;
      this.jumpPick = null;
      this.jumpTarget = null;
   }

   private void openSettings(boolean toTop) {
      this.dropFocus();
      if (!this.settingsPage) {
         this.rememberScroll();
         float remembered = MenuMemory.scroll("#settings");
         this.settingsRestore = toTop ? -1.0F : remembered;
         this.restoreFrames = 0;
      }

      this.settingsPage = true;
      this.presetsPage = false;
      this.globalPage = false;
      this.accentSet = false;
      if (toTop) {
         this.settingsBar.target = 0.0F;
      }
   }

   private void openPresets() {
      this.dropFocus();
      if (this.presetsUi == null) {
         this.presetsUi = new PresetsPage();
      }

      if (!this.presetsPage) {
         this.rememberScroll();
         this.presetsUi.opened();
         float remembered = MenuMemory.scroll("#presets");
         if (remembered >= 0.0F) {
            this.presetsUi.restoreScroll(remembered);
         }
      }

      this.presetsPage = true;
      this.settingsPage = false;
      this.globalPage = false;
      this.accentSet = false;
   }

   private void openGlobal() {
      if (!this.globalPage) {
         Popovers.close();
         if (this.focused != null) {
            this.focused.unfocus();
            this.focused = null;
         }

         if (this.presetsUi != null) {
            this.presetsUi.unfocus();
         }

         this.searchBar.unfocus();
         this.globalPage = true;
      }

      this.globalBar.focus();
   }

   private void openSectionsList() {
      ModSounds.gui(Sfx.DROPDOWN);
      Popovers.open(new SectionsPopover(new SectionsPopover.Host() {
         @Override
         public void open(Module m) {
            FuriahScreen.this.showSection(m);
         }

         @Override
         public Module shown() {
            return FuriahScreen.this.modulePage() ? FuriahScreen.this.module : null;
         }
      }, this.modules.getModules()), this.hdrSectionsX, this.hdrIconY, 0.0F, this.buttonH);
   }

   private void closeGlobal() {
      if (this.globalPage) {
         this.globalPage = false;
         this.accentSet = false;
      }

      this.globalBar.unfocus();
   }

   private void showSection(Module m) {
      if (this.presetsUi != null) {
         this.presetsUi.unfocus();
      }

      if (this.settingsPage || this.presetsPage) {
         this.rememberScroll();
      }

      boolean wasOther = this.settingsPage || this.presetsPage || this.globalPage;
      this.settingsPage = false;
      this.presetsPage = false;
      this.globalPage = false;
      if (m != this.module) {
         this.select(m);
      } else if (wasOther) {
         this.dropFocus();
         this.accentSet = false;
      }
   }

   private void revealFromGlobal(String key, String query, SearchResult result) {
      this.globalBar.unfocus();
      Module m = this.modules.get(key);
      if (result != null && m != null) {
         ModSounds.gui(Sfx.SECTION);
         this.showSection(m);
         this.updateSearch();
         Widget widget = result.entry.row == null ? null : this.widgetOf.get(result.entry.row);
         if (widget == null) {
            this.openFromSearch(key, query, result.entry);
         } else {
            this.showInSection(result, widget);
         }
      } else {
         this.openFromSearch(key, query, result == null ? null : result.entry);
      }
   }

   private void dropFocus() {
      Popovers.close();
      if (this.focused != null) {
         this.focused.unfocus();
         this.focused = null;
      }

      if (this.presetsUi != null) {
         this.presetsUi.unfocus();
      }

      this.searchBar.unfocus();
      this.globalBar.unfocus();
   }

   private boolean modulePage() {
      return !this.settingsPage && !this.presetsPage && !this.globalPage;
   }

   private void select(Module m) {
      this.select(m, true);
   }

   private void select(Module m, boolean restart) {
      this.dropFocus();
      if (this.module != null && this.modulePage()) {
         this.rememberScroll();
      }

      this.module = m;
      if (this.sectionSearch != null) {
         this.sectionSearch.clearFilters();
         this.sectionSearch = null;
      }

      this.widgets.clear();
      this.widgetHeaders.clear();
      this.widgetPages.clear();
      this.widgetDepths.clear();
      this.widgetParents.clear();
      this.widgetOf.clear();
      this.settingOf.clear();
      this.headerGroups.clear();
      this.chipHover.clear();
      this.revealWidget = null;
      this.pulseWidget = null;
      this.jumpPick = null;
      this.jumpTarget = null;
      this.jumpPage = null;
      this.splitFor = null;
      this.lastListTop = Float.NaN;
      this.anchorWidth = -1.0F;
      this.active = null;
      this.listBar.jumpTo(0.0F);
      this.tabRow.reset();
      this.tabSlideX = 0.0F;
      this.sideways = false;
      this.addRow(new NoticeWidget(m), null, null, 0, -1);
      this.layout = SectionLayouts.of(m);
      this.groupOfSetting.clear();

      for (LayoutEntry e : this.layout.allEntries()) {
         if (e.setting != null) {
            this.groupOfSetting.put(e.setting, e.group());
         }
      }

      final String id = m.getId();
      if (firstTimeShown() && MenuMemory.hintDue(id)) {
         this.addRow(new HintWidget("Pick a style, a color and a strength — everything else is optional.", new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return MenuMemory.hintDue(id);
            }
         }, new Runnable() {
            @Override
            public void run() {
               MenuMemory.hintDone(id);
               ModSounds.gui(Sfx.CLICK);
            }
         }), null, this.layout.tabbed() ? null : this.layout.sectionPage(), 0, -1);
      }

      this.topRows = this.widgets.size();
      IdentityHashMap<Setting, Integer> rowOf = new IdentityHashMap<>();
      IdentityHashMap<LayoutPage, List<List<LayoutEntry>>> pinnedOn = new IdentityHashMap<>();
      IdentityHashMap<Setting, Boolean> moved = new IdentityHashMap<>();

      for (List<LayoutEntry> tree : this.pinnedTrees(m)) {
         LayoutPage tab = tree.get(0).page().tabPage();
         LayoutPage home = tab != null ? tab : this.layout.sectionPage();
         List<List<LayoutEntry>> list = pinnedOn.get(home);
         if (list == null) {
            list = new ArrayList<>();
            pinnedOn.put(home, list);
         }

         list.add(tree);

         for (LayoutEntry layoutEntry : tree) {
            moved.put(layoutEntry.setting, Boolean.TRUE);
         }
      }

      for (LayoutPage p : this.layout.pages) {
         List<List<LayoutEntry>> pinned = pinnedOn.get(p);
         if (pinned != null) {
            ArrayList<Setting> members = new ArrayList<>();

            for (List<LayoutEntry> tree : pinned) {
               for (LayoutEntry ex : tree) {
                  members.add(ex.setting);
               }
            }

            HeaderWidget header = new HeaderWidget("Pinned", members, m.getId() + ":pinned" + (p.isSection() ? "" : ":" + p.key), true, false);
            this.addRow(header, null, p, 0, -1);

            for (List<LayoutEntry> list : pinned) {
               int base = list.get(0).depth;

               for (LayoutEntry ex : list) {
                  Widget widget = this.rowWidget(m, ex.setting);
                  this.widgetOf.put(ex.setting, widget);
                  this.settingOf.put(widget, ex.setting);
                  Integer parent = ex != list.get(0) && ex.parent != null ? rowOf.get(ex.parent) : null;
                  rowOf.put(ex.setting, this.widgets.size());
                  this.addRow(widget, header, p, ex.depth - base, parent == null ? -1 : parent);
               }
            }
         }

         if (p.style != null) {
            this.addRow(this.startingLook(p), null, p, 0, -1);
         }

         Module.StartingLooks ownLooks = p == this.layout.sectionPage() ? m.startingLooks() : null;
         String lookAfter = ownLooks == null ? null : ownLooks.after();
         boolean lookPlaced = false;

         for (LayoutGroup g : p.groups) {
            HeaderWidget header = null;
            if (g.hasHeader()) {
               List<Setting> members = g.members();

               for (int k = members.size() - 1; k >= 0; k--) {
                  if (moved.containsKey(members.get(k))) {
                     members.remove(k);
                  }
               }

               header = new HeaderWidget(g.title, members, g.foldKey(m.getId()), true, g.collapsedByDefault());
               if (g.showsCount()) {
                  header.countInTitle();
               }

               if (g.slot == Slot.OPTIONS) {
                  header.styleInTitle();
               }

               this.headerGroups.put(header, g);
               this.addRow(header, null, p, 0, -1);
            }

            for (LayoutEntry ex : g.entries) {
               if (ex.setting == null || !moved.containsKey(ex.setting)) {
                  Widget widget;
                  if (ex.link != null) {
                     final LayoutPage target = ex.link;
                     widget = new PageLinkWidget(target, new Runnable() {
                        @Override
                        public void run() {
                           FuriahScreen.this.showPage(target, true);
                        }
                     });
                  } else {
                     widget = this.rowWidget(m, ex.setting);
                     this.widgetOf.put(ex.setting, widget);
                     this.settingOf.put(widget, ex.setting);
                     rowOf.put(ex.setting, this.widgets.size());
                  }

                  Integer parent = ex.parent == null ? null : rowOf.get(ex.parent);
                  this.addRow(widget, header, p, ex.depth, parent == null ? -1 : parent);
                  if (g.slot == Slot.MAIN && lookAfter != null && !lookPlaced && ex.setting != null && lookAfter.equals(ex.setting.getId())) {
                     this.addRow(this.startingLook(p), header, p, 0, -1);
                     lookPlaced = true;
                  }
               }
            }

            if (g.slot == Slot.MAIN && p == this.layout.sectionPage() && !lookPlaced) {
               this.addRow(this.startingLook(p), header, p, 0, -1);
               lookPlaced = true;
            }
         }
      }

      int n = this.widgets.size();
      this.rowChildren = new int[n][];
      this.rowOpen = new boolean[n];
      this.arrowTurn = new float[n];
      int[] counts = new int[n];

      for (int i = 0; i < n; i++) {
         int p = this.widgetParents.get(i);
         if (p >= 0) {
            counts[p]++;
         }
      }

      for (int var22 = 0; var22 < n; var22++) {
         this.rowChildren[var22] = new int[counts[var22]];
         counts[var22] = 0;
      }

      for (int var23 = 0; var23 < n; var23++) {
         int p = this.widgetParents.get(var23);
         if (p >= 0) {
            int n4 = counts[p]++;
            this.rowChildren[p][n4] = var23;
         }
      }

      for (int var24 = 0; var24 < n; var24++) {
         Setting s = this.settingOf.get(this.widgets.get(var24));
         this.rowOpen[var24] = this.rowChildren[var24].length > 0 && s != null && MenuMemory.optionsOpen(m.getId() + ":" + s.getId());
         this.arrowTurn[var24] = this.rowOpen[var24] ? 1.0F : 0.0F;
      }

      String key = MenuMemory.subPage(m.getId());
      LayoutPage remembered = key == null ? null : this.layout.page(key);
      LayoutPage layoutPage = this.page = remembered != null ? remembered : this.layout.sectionPage();
      if (this.layout.tabbed() && this.page.isSection()) {
         LayoutPage open = m.getTabs() == null ? null : this.layout.stylePage(m.getTabs().current());
         LayoutPage var51 = this.page = open != null ? open : this.layout.firstPage();
      }

      if (this.page.style != null && !this.page.style.isShown()) {
         this.page.style.show();
      }

      LayoutPage tabOf;
      if ((tabOf = this.page.tabPage()) != null && !tabOf.tab.isShown()) {
         tabOf.tab.show();
      }

      this.listRestore = MenuMemory.scroll(this.scrollKey());
      this.restoreFrames = 0;
      if (restart) {
         m.restartPreview();
      }
   }

   private StartingLookWidget startingLook(final LayoutPage p) {
      return new StartingLookWidget(new Supplier<List<SliderSetting>>() {
         public List<SliderSetting> get() {
            return FuriahScreen.this.lookTargets(p);
         }
      }, p == this.layout.sectionPage() ? this.module.startingLooks() : null);
   }

   private List<SliderSetting> lookTargets(LayoutPage p) {
      ArrayList<SliderSetting> out = new ArrayList<>();

      for (Entry<Setting, LayoutGroup> e : this.groupOfSetting.entrySet()) {
         Setting s = e.getKey();
         LayoutGroup g = e.getValue();
         if (s instanceof SliderSetting
            && g.page() == p
            && s.isSaved()
            && s.isVisible()
            && (s.getOwner() == null || s.getOwner().isShown())
            && (g.slot == Slot.SIZE || g.slot == Slot.MOTION)
            && !StartingLookWidget.notAStrength((SliderSetting)s)) {
            out.add((SliderSetting)s);
         }
      }

      return out;
   }

   private Widget rowWidget(Module m, Setting s) {
      Widget w = createWidget(m, s);
      String id = s.getId();
      if (s.isSaved() || !id.equals("preview") && !id.endsWith(".preview")) {
         return w;
      } else {
         final String key = m.getId() + ":preview:" + id;
         return new CollapsiblePreview(w, new Supplier<Boolean>() {
            public Boolean get() {
               return MenuMemory.optionsOpen(key);
            }
         }, new Consumer<Boolean>() {
            public void accept(Boolean open) {
               MenuMemory.optionsOpen(key, open);
            }
         });
      }
   }

   private List<List<LayoutEntry>> pinnedTrees(Module m) {
      ArrayList<List<LayoutEntry>> out = new ArrayList<>();
      IdentityHashMap<Setting, Boolean> taken = new IdentityHashMap<>();

      for (String id : MenuMemory.pins(m.getId())) {
         for (LayoutEntry e : this.layout.allEntries()) {
            if (e.setting != null && e.setting.getId().equals(id) && !taken.containsKey(e.setting)) {
               ArrayList<LayoutEntry> tree = new ArrayList<>();
               List<LayoutEntry> entries = e.group().entries;
               int at = entries.indexOf(e);
               tree.add(e);

               for (int k = at + 1; k < entries.size() && entries.get(k).depth > e.depth; k++) {
                  if (entries.get(k).setting != null && !taken.containsKey(entries.get(k).setting)) {
                     tree.add(entries.get(k));
                  }
               }

               for (LayoutEntry t : tree) {
                  taken.put(t.setting, Boolean.TRUE);
               }

               out.add(tree);
               break;
            }
         }
      }

      return out;
   }

   private void togglePin(Setting s) {
      List<String> pins = MenuMemory.pins(this.module.getId());
      if (pins.contains(s.getId())) {
         pins.remove(s.getId());
      } else {
         pins.add(s.getId());
      }

      MenuMemory.pins(this.module.getId(), pins);
      LayoutPage was = this.page;
      this.select(this.module, false);
      if (was != null && was != this.page) {
         this.showPage(was, false);
      }
   }

   private boolean isPinned(Setting s) {
      return MenuMemory.pins(this.module.getId()).contains(s.getId());
   }

   private void addRow(Widget widget, HeaderWidget header, LayoutPage p, int depth, int parent) {
      this.widgets.add(widget);
      this.widgetHeaders.add(header);
      this.widgetPages.add(p);
      this.widgetDepths.add(depth);
      this.widgetParents.add(parent);
   }

   private void showPage(LayoutPage target, boolean sound) {
      if (target != null) {
         if (target.style != null && !target.style.isShown()) {
            target.style.show();
         }

         LayoutPage toTab;
         if ((toTab = target.tabPage()) != null && !toTab.tab.isShown()) {
            toTab.tab.show();
         }

         if (target != this.page) {
            this.dropFocus();
            this.rememberScroll();
            LayoutPage fromTab = this.page == null ? null : this.page.tabPage();
            List<LayoutPage> tabs = this.layout.tabPages();
            boolean bl = this.sideways = fromTab != null && toTab != null && fromTab != toTab;
            this.tabSlideX = this.sideways ? (tabs.indexOf(toTab) > tabs.indexOf(fromTab) ? 22.0F : -22.0F) : 0.0F;
            this.page = target;
            MenuMemory.subPage(this.module.getId(), target.key);
            this.revealWidget = null;
            this.pulseWidget = null;
            this.jumpPick = null;
            this.jumpTarget = null;
            this.lastListTop = Float.NaN;
            this.anchorWidth = -1.0F;
            this.active = null;
            this.listBar.jumpTo(0.0F);
            this.listRestore = MenuMemory.scroll(this.scrollKey());
            this.restoreFrames = 0;

            for (Widget w : this.widgets) {
               w.appear = -1.0F;
            }

            float f = this.pageFade = this.sideways ? 0.1F : 0.0F;
            if (sound) {
               ModSounds.gui(this.sideways ? Sfx.CLICK : Sfx.SECTION);
            }
         }
      }
   }

   private void openTab(LayoutPage tab) {
      if (tab != null && this.page != tab) {
         this.showPage(tab, true);
      }
   }

   private void cycleTab(int dir) {
      List<LayoutPage> tabs = this.layout.tabPages();
      if (!tabs.isEmpty() && this.page != null) {
         int i = tabs.indexOf(this.page.tabPage());
         this.openTab(tabs.get(((i < 0 ? 0 : i) + dir + tabs.size()) % tabs.size()));
      }
   }

   private boolean back() {
      if (this.modulePage() && this.page != null && this.page.parent != null && !this.page.isTab()) {
         this.showPage(this.page.parent, true);
         return true;
      } else {
         return false;
      }
   }

   private boolean goBack() {
      if (this.globalPage) {
         this.closeGlobal();
         return true;
      } else {
         return this.back();
      }
   }

   private LayoutPage pageOf(Widget widget) {
      int i = this.widgets.indexOf(widget);
      return i < 0 ? null : this.widgetPages.get(i);
   }

   private String unavailable(Widget widget) {
      Setting s = this.settingOf.get(widget);
      return s == null ? null : s.unavailableReason();
   }

   private boolean shows(int i) {
      Widget widget = this.widgets.get(i);
      LayoutPage p = this.widgetPages.get(i);
      if (p != null && p != this.page) {
         return false;
      } else {
         HeaderWidget header = this.widgetHeaders.get(i);
         Setting s = this.settingOf.get(widget);
         if (widget.isVisible() && (s == null || s.isVisible()) && (header == null || !header.isCollapsed())) {
            int parent = this.widgetParents.get(i);
            return parent >= 0 && Density.compact() && parent < this.rowOpen.length && !this.rowOpen[parent] ? false : parent < 0 || this.shows(parent);
         } else {
            return false;
         }
      }
   }

   private boolean hasShowableOptions(int i) {
      if (i >= this.rowChildren.length) {
         return false;
      } else {
         for (int c : this.rowChildren[i]) {
            Setting s = this.settingOf.get(this.widgets.get(c));
            if (s != null && s.isVisible() && this.widgets.get(c).isVisible()) {
               return true;
            }
         }

         return false;
      }
   }

   private void setOptionsOpen(int i, boolean open) {
      if (i >= 0 && i < this.rowOpen.length && this.rowOpen[i] != open) {
         this.rowOpen[i] = open;
         Setting s = this.settingOf.get(this.widgets.get(i));
         if (s != null) {
            MenuMemory.optionsOpen(this.module.getId() + ":" + s.getId(), open);
         }
      }
   }

   private float slideOf(int i, Widget widget, boolean visible) {
      if (widget.appear >= 0.999F) {
         return 0.0F;
      } else {
         Setting s = this.settingOf.get(widget);
         if (s == null && widget instanceof HeaderWidget && i + 1 < this.widgets.size()) {
            s = this.settingOf.get(this.widgets.get(i + 1));
         }

         SettingStyle owner = s == null ? null : s.getOwner();
         return owner == null ? 0.0F : owner.slideOffset(visible, widget.appear);
      }
   }

   private boolean blocked() {
      Module.Notice n = this.modulePage() ? this.module.getNotice() : null;
      return n != null && n.blocking;
   }

   public static Widget createWidget(Module m, Setting s) {
      if (s instanceof HasWidget) {
         return ((HasWidget)s).createWidget();
      } else if (s instanceof BooleanSetting) {
         return new ToggleWidget((BooleanSetting)s);
      } else if (s instanceof SliderSetting) {
         return new SliderWidget((SliderSetting)s);
      } else if (s instanceof TextSetting) {
         return new TextFieldWidget((TextSetting)s);
      } else if (s instanceof ColorSetting) {
         return new ColorWidget((ColorSetting)s);
      } else if (s instanceof ModeSetting) {
         ModeSetting mode = (ModeSetting)s;
         switch (mode.getStyle()) {
            case CARDS:
               return new CardsWidget(m, mode);
            case CHIPS:
               return new ChipsWidget(mode);
            default:
               return new SegmentedWidget(mode);
         }
      } else if (s instanceof PresetSetting) {
         return new PresetsWidget((PresetSetting)s);
      } else if (s instanceof ItemListSetting) {
         return new ItemListWidget((ItemListSetting)s);
      } else if (s instanceof ItemRulesSetting) {
         return new ItemRulesWidget((ItemRulesSetting)s);
      } else {
         throw new IllegalArgumentException("No widget for setting type " + s.getClass().getName());
      }
   }

   public void initGui() {
      Keyboard.enableRepeatEvents(true);
   }

   public void onGuiClosed() {
      if (GuiCheck.running != null) {
         GuiCheck.running.abort(this);
      }

      peekHeld = false;
      Keyboard.enableRepeatEvents(false);
      if (MenuNav.host == this.navHost) {
         MenuNav.host = null;
      }

      this.dropFocus();
      if (this.sectionSearch != null) {
         this.sectionSearch.clearFilters();
      }

      GlassRenderer.release();
      Ui.endMenuScale();
      this.rememberScroll();
      MenuMemory.page(this.pageKey());
      FuriahsVisuals.instance.getConfig().saveIfDirty();
   }

   public boolean doesGuiPauseGame() {
      return false;
   }

   public GuiScreen parentScreen() {
      return this.parent;
   }

   private void close() {
      if (!this.closing) {
         this.closing = true;
         this.closeAsked = System.nanoTime();
         this.dropFocus();
         ModSounds.gui(Sfx.GUI_CLOSE);
      }
   }

   public void drawScreen(int unusedX, int unusedY, float partialTicks) {
      long start = System.nanoTime();

      try {
         this.drawScreenInner();
      } finally {
         long took = System.nanoTime() - start;
         drawNanosTotal += took;
         drawNanosMax = Math.max(drawNanosMax, took);
         drawFrames++;
      }
   }

   private void warmSearch() {
      List<Module> all = this.modules.getModules();
      if (!(this.open < 1.0F) && !this.closing && this.warmNext < all.size()) {
         Module m;
         if (!SearchIndexes.isBuilt(m = all.get(this.warmNext++))) {
            SearchIndexes.of(m);
         }
      }
   }

   public static String drawTimeForTest(boolean reset) {
      String s = drawFrames == 0
         ? "no frames"
         : String.format(Locale.ROOT, "avg %.2f ms, max %.2f ms over %d frames", drawNanosTotal / 1000000.0 / drawFrames, drawNanosMax / 1000000.0, drawFrames);
      if (reset) {
         drawNanosTotal = 0L;
         drawNanosMax = 0L;
         drawFrames = 0;
      }

      return s;
   }

   private void drawScreenInner() {
      long now = ClientClock.nanos();
      float dt = this.lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastNanos) / 1.0E9F);
      this.lastNanos = now;
      if (this.closing) {
         this.open -= dt / 0.14F;
         if (this.open <= 0.0F || dt <= 0.0F && System.nanoTime() - this.closeAsked > 1000000000L) {
            this.mc.displayGuiScreen(this.parent);
            return;
         }
      } else {
         this.open = Math.min(1.0F, this.open + dt / 0.22F);
      }

      float anim = Easing.outCubic(Easing.clamp01(this.open));
      boolean measure = GuiCheck.running != null && this.open >= 1.0F && !this.closing && GuiCheck.running.beforeFrame(this);
      Ui.ensureFonts();
      Textures.ensureLoaded();
      this.updateAccent(dt);
      Theme.apply(FuriahsVisuals.instance.getTheme().get(), this.accent);
      this.updateScale(dt);
      Ui.offsetY = (1.0F - anim) * 10.0F;
      this.mouseX = Mouse.getX() / Ui.px;
      this.mouseY = (this.mc.displayHeight - Mouse.getY() - 1) / Ui.px - Ui.offsetY;
      if (GuiCheck.running != null) {
         this.mouseX = -10000.0F;
         this.mouseY = -10000.0F;
      }

      this.layout(dt);
      if (this.open >= 1.0F && !this.closing) {
         GuiColors.attach(this.vw, this.vh);
      }

      if (this.modulePage()) {
         MenuMemory.markOpened(this.module.getId());
         this.scrollOwner = this.module;
      }

      if (pendingReveal != null && this.open >= 1.0F) {
         String id = pendingReveal;
         pendingReveal = ++pendingRevealTries < 10 ? id : null;
         if (pendingReveal == null) {
            pendingRevealTries = 0;
         }

         if (this.revealSettingForTest(id)) {
            pendingReveal = null;
            pendingRevealTries = 0;
            this.pulseWidget = this.revealWidget;
            this.pulseOffset = 0.0F;
            this.pulseHeight = -1.0F;
            this.pulseStart = ClientClock.nanos();
         }
      }

      this.warmSearch();
      ScaledResolution sr = new ScaledResolution(this.mc);
      float k = Ui.px / sr.getScaleFactor();
      GlStateManager.pushMatrix();
      GlStateManager.scale(k, k, 1.0F);
      setupState();
      Ui.beginFrame();

      try {
         if (this.mc.theWorld == null) {
            Ui.gradientV(0.0F, 0.0F, this.vw, this.vh, 0.0F, -15723749, -16447990);
         }

         if (!Theme.GLASS && !this.settingsPage) {
            GlassRenderer.skip();
         } else {
            GlassRenderer.capture(Ui.px);
            this.updateLight(dt);
         }

         boolean dragging = this.docked && this.active != null && this.active.isDragging();
         this.dragFade = Easing.approach(this.dragFade, dragging ? 1.0F : 0.0F, 12.0F, dt);
         this.peekFade = Easing.approach(this.peekFade, peekHeld ? 1.0F : 0.0F, 10.0F, dt);
         this.openAlpha = anim;
         Ui.pushAlpha(anim);
         Ui.pushAlpha((1.0F - 0.8F * this.dragFade) * (1.0F - this.peekFade));
         if (this.docked) {
            Ui.gradientH(0.0F, 0.0F, this.vw * 0.74F, this.vh, 0.0F, Theme.BACKDROP_BOTTOM, Theme.BACKDROP_TOP & 16777215);
         } else {
            Ui.gradientV(0.0F, 0.0F, this.vw, this.vh, 0.0F, Theme.BACKDROP_TOP, Theme.BACKDROP_BOTTOM);
         }

         GlStateManager.translate(0.0F, Ui.offsetY, 0.0F);
         this.frameTooltip = null;
         this.listHover = null;
         float realX = this.mouseX;
         float realY = this.mouseY;
         if (Popovers.covers(this.mouseX, this.mouseY) || this.guide != null && this.guide.contains(this.mouseX, this.mouseY)) {
            this.mouseX = -10000.0F;
            this.mouseY = -10000.0F;
         }

         if (measure || UiProbe.overlay) {
            UiProbe.begin();
         }

         this.drawPanel();
         this.drawGrip(dt);
         this.drawSidebar(dt);
         this.drawHeader(dt);
         this.updateSearch();
         this.drawSearchBar(dt);
         if (this.globalPage) {
            this.drawGlobalPage(dt);
         } else if (this.settingsPage) {
            this.drawSettingsPage(dt);
         } else if (this.presetsPage) {
            this.drawPresetsPage(dt);
         } else {
            this.drawTabs(dt);
            if (this.searching()) {
               this.drawSearchList(dt);
            } else {
               this.drawList(dt);
               this.drawJumpBar(dt);
            }

            this.drawPreview(dt);
            this.drawInfo();
         }

         this.mouseX = realX;
         this.mouseY = realY;
         if (this.guide != null) {
            boolean free = !Popovers.covers(this.mouseX, this.mouseY);
            this.guide
               .render(
                  this.panelX + this.sideW + 18.0F,
                  this.panelW - this.sideW - 36.0F,
                  this.panelY + this.panelH,
                  free ? this.mouseX : -10000.0F,
                  free ? this.mouseY : -10000.0F,
                  dt,
                  this.accent
               );
            if (free && this.guide.contains(this.mouseX, this.mouseY)) {
               this.frameTooltip = this.guide.tooltip();
            }
         }

         Popovers.render(this.mouseX, this.mouseY, dt, this.accent);
         if (Popovers.covers(this.mouseX, this.mouseY)) {
            this.frameTooltip = Popovers.tooltip(this.mouseX, this.mouseY);
         }

         if (UiProbe.recording()) {
            UiProbe.end();
            if (measure && GuiCheck.running != null) {
               GuiCheck.running.measured(this, UiProbe.items());
            }

            if (UiProbe.overlay) {
               UiProbe.drawOverlay();
            }
         }

         Ui.popAlpha();
         this.updateTooltip(this.peekFade > 0.01F ? null : this.frameTooltip, dt);
         this.drawTooltip(dt);
         this.drawZoomToast();
         Ui.popAlpha();
         Object hovered = !this.closing && !(this.open < 0.6F) ? this.hoverTargetNow() : null;
         if (hovered != this.hoverTarget) {
            this.hoverTarget = hovered;
            if (hovered != null) {
               ModSounds.hover();
            }
         }
      } finally {
         Ui.endFrame();
         restoreState();
         GlStateManager.popMatrix();
      }
   }

   private Object hoverTargetNow() {
      if (this.guide != null && this.guide.contains(this.mouseX, this.mouseY)) {
         return "guide";
      } else if (inside(this.mouseX, this.mouseY, this.settingsX, this.settingsY, this.settingsW, this.setH)
         || inside(this.mouseX, this.mouseY, this.hdrSettingsX, this.hdrIconY, this.buttonH, this.buttonH)) {
         return "#settings";
      } else if (!inside(this.mouseX, this.mouseY, this.presetsX, this.presetsY, this.setH, this.setH)
         && !inside(this.mouseX, this.mouseY, this.hdrPresetsX, this.hdrIconY, this.buttonH, this.buttonH)) {
         if (!inside(this.mouseX, this.mouseY, this.magX, this.magY, this.magS, this.magS)
            && !inside(this.mouseX, this.mouseY, this.hdrSearchX, this.hdrIconY, this.buttonH, this.buttonH)) {
            if (inside(this.mouseX, this.mouseY, this.hdrSectionsX, this.hdrIconY, this.buttonH, this.buttonH)) {
               return "sections";
            } else if (this.onGrip(this.mouseX, this.mouseY)) {
               return "grip";
            } else {
               FuriahScreen.NavEntry nav = this.navAt(this.mouseX, this.mouseY);
               if (nav != null) {
                  if (nav.module != null && this.onNavDot(nav, this.mouseX, this.mouseY)) {
                     return this.dotAnim(nav.module);
                  } else {
                     return nav.module != null ? nav.module : nav.category;
                  }
               } else if (this.backShown && inside(this.mouseX, this.mouseY, this.backX - 2.0F, this.backY - 2.0F, this.backS + 4.0F, this.backS + 4.0F)) {
                  return "back";
               } else if (this.globalPage) {
                  return this.globalSearch.hoverTarget();
               } else if (!this.modulePage()) {
                  return null;
               } else if (inside(this.mouseX, this.mouseY, this.switchX - 4.0F, this.switchY - 4.0F, this.switchW + 8.0F, this.switchH + 8.0F)) {
                  return "switch";
               } else if (inside(this.mouseX, this.mouseY, this.resetX, this.resetY, this.resetW, this.buttonH)) {
                  return "reset";
               } else if (this.module.hasTestAction() && inside(this.mouseX, this.mouseY, this.testX, this.testY, this.testW, this.buttonH)) {
                  return "test";
               } else if (inside(this.mouseX, this.mouseY, this.moveX, this.moveY, this.moveW, this.buttonH)) {
                  return "move";
               } else {
                  for (int i = 0; i < this.actionBoxes.size() && i < this.actionList.size(); i++) {
                     float[] b = this.actionBoxes.get(i);
                     if (inside(this.mouseX, this.mouseY, b[0], b[1], b[2], b[3])) {
                        return this.actionList.get(i);
                     }
                  }

                  if (this.tabsShown && this.tabRow.contains(this.mouseX, this.mouseY)) {
                     return this.tabRow.hoverTarget(this.mouseX, this.mouseY);
                  } else if (inside(this.mouseX, this.mouseY, this.collapseX, this.jumpY, this.collapseW, this.jumpH)) {
                     return "collapse";
                  } else {
                     for (int k = 0; k < this.jumpChips.size(); k++) {
                        float[] c = this.jumpChips.get(k);
                        if (inside(this.mouseX, this.mouseY, c[0], c[1], c[2], c[3])) {
                           return this.jumpHeaders.get(k);
                        }
                     }

                     return this.listHover;
                  }
               }
            }
         } else {
            return "global";
         }
      } else {
         return "#presets";
      }
   }

   private float autoPx() {
      return Math.max(0.75F, Math.min(this.mc.displayHeight / 580.0F, this.mc.displayWidth / 900.0F));
   }

   private float targetPx() {
      float want = MenuPrefs.GUI_SCALE_AUTO.get() ? this.autoPx() : 1.862069F * MenuPrefs.GUI_SCALE.getFloat() / 100.0F;
      float most = Math.min(this.mc.displayWidth / 440.0F, this.mc.displayHeight / 280.0F);
      return Math.max(0.5F, Math.min(want, most));
   }

   private int scalePercent() {
      return Math.round(this.targetPx() / 1.862069F * 100.0F);
   }

   private void updateScale(float dt) {
      float target = this.targetPx();
      float f = this.shownPx = this.shownPx < 0.0F ? target : Easing.approach(this.shownPx, target, 14.0F, dt);
      if (Math.abs(this.shownPx - target) < 0.002F) {
         this.shownPx = target;
      }

      Ui.setScale(this.shownPx, target);
      this.vw = this.mc.displayWidth / Ui.px;
      this.vh = this.mc.displayHeight / Ui.px;
      Popovers.view(this.vw, this.vh);
   }

   private void zoomStep(int dir) {
      int now;
      int next = now = this.scalePercent();
      int[] steps = MenuPrefs.SCALE_STEPS;
      if (dir > 0) {
         for (int s : steps) {
            if (s > now) {
               next = s;
               break;
            }
         }
      } else {
         for (int i = steps.length - 1; i >= 0; i--) {
            if (steps[i] < now) {
               next = steps[i];
               break;
            }
         }
      }

      if (next != now || MenuPrefs.GUI_SCALE_AUTO.get()) {
         MenuPrefs.GUI_SCALE.set(next);
         MenuPrefs.GUI_SCALE_AUTO.set(false);
         ModSounds.gui(Sfx.CLICK);
      }

      this.zoomToastUntil = ClientClock.millis() + 1300L;
   }

   private void applyScaleDraft() {
      if (this.scaleDraft.getInt() != this.scalePercent()) {
         MenuPrefs.GUI_SCALE.set(this.scaleDraft.getInt());
         MenuPrefs.GUI_SCALE_AUTO.set(false);
         this.zoomToastUntil = ClientClock.millis() + 1300L;
      }
   }

   private void syncScaleDraft() {
      if (this.active != this.scaleWidget || !this.scaleWidget.isDragging()) {
         this.syncingDraft = true;

         try {
            this.scaleDraft.set(this.scalePercent());
         } finally {
            this.syncingDraft = false;
         }
      }
   }

   private void zoomReset() {
      if (!MenuPrefs.GUI_SCALE_AUTO.get()) {
         MenuPrefs.GUI_SCALE_AUTO.set(true);
         ModSounds.gui(Sfx.CLICK);
      }

      this.zoomToastUntil = ClientClock.millis() + 1300L;
   }

   private void drawZoomToast() {
      long left = this.zoomToastUntil - ClientClock.millis();
      if (left > 0L) {
         float a = Math.min(1.0F, (float)left / 250.0F) * Math.min(1.0F, (float)(1300L - left) / 120.0F);
         String text = "GUI scale " + (MenuPrefs.GUI_SCALE_AUTO.get() ? "Auto (" + this.scalePercent() + " %)" : this.scalePercent() + " %");
         float ts = 9.0F;
         float w = Ui.width(text, ts) + 24.0F;
         float h = 20.0F;
         float x = (this.vw - w) / 2.0F;
         float y = 10.0F - Ui.offsetY;
         Ui.pushAlpha(a);
         Ui.shadow(x, y + 2.0F, w, h, h / 2.0F, 8.0F, Theme.LIGHT ? 1073741824 : -2013265920);
         Ui.roundRect(x, y, w, h, h / 2.0F, Theme.TOOLTIP);
         Ui.outline(x, y, w, h, h / 2.0F, 0.8F, Theme.ACCENT_LINE);
         Ui.textMiddle(text, x + 12.0F, y + h / 2.0F, ts, Theme.TEXT, false);
         Ui.popAlpha();
      }
   }

   private void layout(float dt) {
      this.docked = this.modulePage() && this.module.previewsInWorld();
      boolean full = MenuPrefs.FULL_SCREEN.get() && !this.resizing;
      float margin = full ? 12.0F : 20.0F;
      float roomW = Math.max(400.0F, this.vw - margin * 2.0F);
      float roomH = Math.max(240.0F, this.vh - margin * 2.0F);
      float wantH;
      float wantW;
      if (this.resizing) {
         wantW = this.resizeW;
         wantH = this.resizeH;
      } else if (full) {
         wantW = roomW;
         wantH = roomH;
      } else {
         wantW = MenuPrefs.PANEL_W.getInt() > 0 ? MenuPrefs.PANEL_W.getFloat() : 720.0F;
         wantH = MenuPrefs.PANEL_H.getInt() > 0 ? MenuPrefs.PANEL_H.getFloat() : 450.0F;
      }

      float targetH = Math.min(Math.max(240.0F, wantH), roomH);
      float targetX;
      float targetW;
      if (this.docked) {
         targetW = Math.min(Math.min(Math.max(400.0F, wantW), roomW), Math.max(540.0F, this.vw * 0.56F));
         targetX = margin;
      } else {
         targetW = Math.min(Math.max(400.0F, wantW), roomW);
         targetX = (this.vw - targetW) / 2.0F;
      }

      float rate = this.resizing ? 60.0F : 16.0F;
      this.panelW = this.panelWShown < 0.0F ? targetW : Easing.approach(this.panelWShown, targetW, rate, dt);
      this.panelX = this.panelXShown < 0.0F ? targetX : Easing.approach(this.panelXShown, targetX, rate, dt);
      this.panelH = this.panelHShown < 0.0F ? targetH : Easing.approach(this.panelHShown, targetH, rate, dt);
      this.panelWShown = this.panelW;
      this.panelXShown = this.panelX;
      this.panelHShown = this.panelH;
      this.panelY = (this.vh - this.panelH) / 2.0F;
      this.layoutSidebar(dt);
      float mainX = this.panelX + this.sideW;
      float contentX = mainX + 18.0F;
      float contentR = this.panelX + this.panelW - 18.0F;
      boolean dense = Density.dense();
      boolean compact = Density.compact();
      this.headerH = dense ? 44.0F : (compact ? 50.0F : 64.0F);
      this.buttonH = dense ? 16.0F : (compact ? 18.0F : 22.0F);
      this.switchW = dense ? 26.0F : (compact ? 30.0F : 38.0F);
      this.switchH = dense ? 14.0F : (compact ? 16.0F : 20.0F);
      this.searchH = dense ? 15.0F : (compact ? 17.0F : 22.0F);
      float row1 = this.headerRow1();
      float row2 = this.headerRow2();
      this.searchW = Math.max(130.0F, Math.min(240.0F, (contentR - contentX) * 0.36F));
      this.searchX = contentR - this.searchW;
      this.searchY = row2 - this.searchH / 2.0F;
      float by = row1 - this.buttonH / 2.0F;
      float bx = contentR;
      this.hdrIconY = by;
      boolean hidden = this.sideMode == MenuPrefs.SidebarStyle.HIDDEN;
      if (hidden) {
         this.hdrPresetsX = contentR - this.buttonH;
         this.hdrSettingsX = this.hdrPresetsX - 4.0F - this.buttonH;
         this.hdrSearchX = this.hdrSettingsX - 4.0F - this.buttonH;
         bx = this.hdrSearchX - 10.0F;
         this.hdrSectionsX = contentX;
      } else {
         this.hdrPresetsX = -10000.0F;
         this.hdrSettingsX = -10000.0F;
         this.hdrSearchX = -10000.0F;
         this.hdrSectionsX = -10000.0F;
      }

      boolean section = this.modulePage();
      this.backShown = section && this.page != null && !this.page.isSection() && !this.page.isTab();
      float rightEnd = bx;
      this.actionList.clear();
      if (section) {
         this.actionList.addAll(this.module.headerActions());
      }

      String title = !section ? "" : (this.backShown ? this.page.title : this.module.getName());
      float leftNeeds = contentX
         + (hidden ? this.buttonH + 8.0F : 0.0F)
         + (this.backShown ? this.buttonH + 8.0F : 0.0F)
         + Math.min(Ui.widthBold(title, titleSize()), 70.0F)
         + 10.0F
         + (section ? this.switchW + 10.0F : 0.0F);

      for (int pass = 0; pass < 2; pass++) {
         this.headerIcons = pass == 1;
         bx = rightEnd;
         this.moveW = section && this.module.hasMoveHud() ? this.headerButtonW("Move HUD") : 0.0F;
         this.moveX = this.moveW > 0.0F ? rightEnd - this.moveW : -10000.0F;
         this.moveY = by;
         if (this.moveW > 0.0F) {
            bx = this.moveX - 6.0F;
         }

         this.resetW = section ? this.headerButtonW(this.resetLabel()) : 0.0F;
         this.resetX = section ? bx - this.resetW : -10000.0F;
         this.resetY = by;
         if (section) {
            bx = this.resetX - 6.0F;
         }

         this.testW = section && this.module.hasTestAction() ? this.headerButtonW(this.module.getTestLabel()) : 0.0F;
         this.testX = this.testW > 0.0F ? bx - this.testW : -10000.0F;
         this.testY = by;
         if (this.testW > 0.0F) {
            bx = this.testX - 6.0F;
         }

         this.actionBoxes.clear();
         this.actionsIconOnly = Density.dense() || this.headerIcons;
         float actionsW = 0.0F;

         for (Module.HeaderAction a : this.actionList) {
            actionsW += this.headerButtonW(a.label) + 6.0F;
         }

         if (!this.actionsIconOnly && bx - actionsW < leftNeeds) {
            this.actionsIconOnly = true;
         }

         for (int i = this.actionList.size() - 1; i >= 0; i--) {
            float w = this.actionsIconOnly ? this.buttonH : this.headerButtonW(this.actionList.get(i).label);
            this.actionBoxes.add(0, new float[]{bx - w, by, w, this.buttonH});
            bx -= w + 6.0F;
         }

         if (bx + 6.0F >= leftNeeds || Density.dense()) {
            break;
         }
      }

      this.backS = this.buttonH;
      this.backX = hidden ? this.hdrSectionsX + this.buttonH + 6.0F : contentX;
      this.backY = row1 - this.backS / 2.0F;
      if (this.globalPage && this.sideMode != MenuPrefs.SidebarStyle.FULL) {
         this.globalX = this.searchX;
         this.globalY = this.searchY;
         this.globalW = this.searchW;
         this.globalH = this.searchH;
      }

      if (!section) {
         this.switchX = -10000.0F;
         this.switchY = -10000.0F;
      }

      this.tabsShown = section && this.layout != null && this.layout.tabbed();
      float below = this.panelY + this.headerH;
      String string = this.bannerReason = null;
      if (this.bannerReason != null) {
         this.bannerX = contentX;
         this.bannerW = contentR - contentX;
         this.bannerH = dense ? 20.0F : (compact ? 22.0F : 26.0F);
         this.bannerY = below + (compact ? 5.0F : 7.0F);
         below = this.bannerY + this.bannerH;
      } else {
         this.bannerY = -10000.0F;
      }

      if (this.tabsShown) {
         List<LayoutPage> tabs = this.layout.tabPages();
         float ty = below + (compact ? 6.0F : 8.0F);
         this.tabRow.layout(tabs, this.page == null ? null : this.page.tabPage(), this.tabCounts(tabs), contentX, ty, contentR - contentX, TabRow.height());
         below = ty + this.tabRow.h;
      }

      boolean bl2 = this.jumpWanted = section && !this.searching() && this.jumpGroups() >= 3;
      this.jumpH = !this.jumpWanted ? 0.0F : (this.tabsShown ? (dense ? 12.0F : (compact ? 13.0F : 15.0F)) : (dense ? 14.0F : (compact ? 16.0F : 20.0F)));
      this.jumpY = below + (compact ? 6.0F : (this.tabsShown ? 7.0F : 9.0F));
      this.listY = this.jumpWanted ? this.jumpY + this.jumpH + (compact ? 6.0F : 8.0F) : below + (compact ? 8.0F : 12.0F);
      float bottom = this.panelY + this.panelH - (compact ? 12.0F : 16.0F);
      if (this.guide != null) {
         bottom = Math.max(this.listY + 60.0F, Math.min(bottom, this.panelY + this.panelH - this.guide.reserved()));
      }

      this.previewW = Math.min(206.0F, Math.max(120.0F, (contentR - contentX) * 0.4F));
      this.previewX = contentR - this.previewW;
      this.previewY = this.listY;
      this.previewH = bottom - this.listY;
      this.listX = contentX;
      this.listW = section && this.module.hasPreview() ? this.previewX - 18.0F - this.listX : this.settingsAreaW();
      this.listH = bottom - this.listY;
      this.keyW = 180.0F;
      this.cardW = (this.settingsAreaW() - 10.0F) / 2.0F;
      this.cardH = 112.0F;
   }

   private float headerRow1() {
      return this.panelY + this.headerH * 0.4F;
   }

   private float headerRow2() {
      return this.panelY + this.headerH * 0.76F;
   }

   private static float titleSize() {
      return Density.dense() ? 12.5F : (Density.compact() ? 14.0F : 17.0F);
   }

   private static float descSize() {
      return Density.dense() ? 7.8F : (Density.compact() ? 8.4F : 9.0F);
   }

   private static float buttonText() {
      return Density.compact() ? 8.0F : 9.0F;
   }

   private float headerButtonW(String label) {
      return !Density.dense() && !this.headerIcons ? this.buttonH * 0.55F + 6.0F + Ui.width(label, buttonText()) + 11.0F : this.buttonH;
   }

   private int jumpGroups() {
      if (this.jumpPage != this.page) {
         this.jumpPage = this.page;
         this.jumpGroups = 0;

         for (int i = 0; i < this.widgets.size(); i++) {
            if (this.widgets.get(i) instanceof HeaderWidget && this.widgetPages.get(i) == this.page) {
               this.jumpGroups++;
            }
         }
      }

      return this.jumpGroups;
   }

   private void layoutSidebar(float dt) {
      MenuPrefs.SidebarStyle style = MenuPrefs.SIDEBAR.get();
      if (style == MenuPrefs.SidebarStyle.FULL && this.panelW < 520.0F) {
         style = MenuPrefs.SidebarStyle.ICONS;
      }

      this.sideMode = style;
      this.sideTarget = style == MenuPrefs.SidebarStyle.FULL ? 164.0F : (style == MenuPrefs.SidebarStyle.ICONS ? 46.0F : 0.0F);
      this.sideW = this.sidePlaced ? Easing.approach(this.sideW, this.sideTarget, 16.0F, dt) : this.sideTarget;
      this.sidePlaced = true;
      this.navEntries.clear();
      this.globalX = -10000.0F;
      this.magX = -10000.0F;
      if (style == MenuPrefs.SidebarStyle.HIDDEN) {
         this.settingsX = -10000.0F;
         this.presetsX = -10000.0F;
         this.navH = 0.0F;
         this.navContent = 0.0F;
      } else {
         boolean icons = style == MenuPrefs.SidebarStyle.ICONS;
         boolean dense = Density.dense();
         boolean compact = Density.compact();
         float rowH = navRowH();
         float step = rowH + (compact ? 1.0F : 4.0F);
         if (icons) {
            float s;
            this.magS = s = navIconSize();
            this.magX = this.panelX + (this.sideW - s) / 2.0F;
            this.magY = this.panelY + 50.0F;
            this.navTop = this.magY + s + 9.0F;
            this.setH = s;
            this.settingsW = s;
            this.settingsX = this.panelX + (this.sideW - s) / 2.0F;
            this.settingsY = this.panelY + this.panelH - 14.0F - s;
            this.presetsX = this.settingsX;
            this.presetsY = this.settingsY - s - 3.0F;
            this.navH = Math.max(0.0F, this.presetsY - 10.0F - this.navTop);
            step = s + (compact ? 1.0F : 2.0F);
         } else {
            this.globalX = this.panelX + 10.0F;
            this.globalW = this.sideW - 20.0F;
            this.globalH = dense ? 18.0F : (compact ? 20.0F : 24.0F);
            this.globalY = this.panelY + (compact ? 52.0F : 54.0F);
            this.navTop = this.globalY + this.globalH + (compact ? 7.0F : 10.0F);
            this.setH = dense ? 22.0F : (compact ? 24.0F : 30.0F);
            this.settingsX = this.panelX + 10.0F;
            this.settingsW = this.sideW - 20.0F - this.setH - 4.0F;
            this.settingsY = this.panelY + this.panelH - this.setH - 32.0F;
            this.presetsX = this.settingsX + this.settingsW + 4.0F;
            this.presetsY = this.settingsY;
            this.navH = Math.max(0.0F, this.settingsY - (compact ? 10.0F : 12.0F) - this.navTop);
         }

         float y = 0.0F;
         float catH = icons ? 6.0F : (dense ? 12.0F : (compact ? 13.0F : 18.0F));
         List<Module> all = this.modules.getModules();
         if (!icons && compact && this.listHeight(all, catH, rowH, step) > this.navH) {
            step = rowH;
            catH = dense ? 9.0F : 10.0F;
            if (this.listHeight(all, catH, rowH, rowH) > this.navH) {
               step = --rowH;
            }
         }

         for (String category : SectionCategories.NAMES) {
            List<Module> list = SectionCategories.in(category, all);
            if (!list.isEmpty()) {
               float h = icons && this.navEntries.isEmpty() ? 0.0F : catH;
               this.navEntries.add(new FuriahScreen.NavEntry(null, category, y, h));
               y += h;
               boolean folded = !icons && categoryFolded(category);

               for (Module m : list) {
                  if (!folded || m == this.module && this.modulePage()) {
                     this.navEntries.add(new FuriahScreen.NavEntry(m, category, y, icons ? navIconSize() : rowH));
                     y += step;
                  }
               }
            }
         }

         this.navContent = y > 0.0F ? y - (step - (icons ? navIconSize() : rowH)) : 0.0F;
      }
   }

   private float listHeight(List<Module> all, float catH, float rowH, float step) {
      float y = 0.0F;

      for (String category : SectionCategories.NAMES) {
         List<Module> list = SectionCategories.in(category, all);
         if (!list.isEmpty()) {
            y += catH;
            boolean folded = categoryFolded(category);

            for (Module m : list) {
               if (!folded || m == this.module && this.modulePage()) {
                  y += step;
               }
            }
         }
      }

      return y > 0.0F ? y - (step - rowH) : 0.0F;
   }

   private static float navRowH() {
      return Density.dense() ? 20.0F : (Density.compact() ? 22.0F : 28.0F);
   }

   private static float navIconSize() {
      return Density.dense() ? 20.0F : (Density.compact() ? 22.0F : 26.0F);
   }

   private static boolean categoryFolded(String category) {
      Boolean b = MenuMemory.folded("nav:" + category);
      return b != null && b;
   }

   private float settingsAreaW() {
      return this.panelX + this.panelW - 18.0F - this.listX - 12.0F;
   }

   private String resetLabel() {
      if (System.currentTimeMillis() < this.resetConfirmUntil) {
         return "Click again to reset";
      } else {
         return this.onSubPage() ? "Reset page" : "Reset section";
      }
   }

   private boolean onSubPage() {
      return this.page != null && !this.page.isSection() && !this.page.isTab();
   }

   private void updateAccent(float dt) {
      int target = Theme.readableAccent(this.module.getAccentColor(ColorSetting.clock()));
      if (!this.accentSet) {
         this.accent = target;
         this.accentSet = true;
         this.accentModule = this.module;
         this.accentBlend = 1.0F;
      } else {
         if (this.module != this.accentModule) {
            this.accentModule = this.module;
            this.accentFrom = this.accent;
            this.accentBlend = 0.0F;
         }

         this.accentBlend = Math.min(1.0F, this.accentBlend + dt / 0.25F);
         this.accent = this.accentBlend >= 1.0F ? target : ColorUtil.lerp(this.accentFrom, target, Easing.outCubic(this.accentBlend));
      }
   }

   private void updateLight(float dt) {
      float rx = Math.max(-1.2F, Math.min(1.2F, (this.mouseX - (this.panelX + this.panelW / 2.0F)) / (this.panelW / 2.0F)));
      float ry = Math.max(-1.2F, Math.min(1.2F, (this.mouseY - (this.panelY + this.panelH / 2.0F)) / (this.panelH / 2.0F)));
      float k = 1.0F - (float)Math.exp(-6.0F * dt);
      this.lightX = this.lightX + (-0.35F + 0.75F * rx - this.lightX) * k;
      this.lightY = this.lightY + (-0.9F + 0.3F * ry - this.lightY) * k;
      GlassRenderer.setLight(this.lightX, this.lightY);
   }

   private static int paneTint(int tint) {
      return ColorUtil.withAlpha(ColorUtil.lerp(tint | 0xFF000000, -1, 0.14F), ColorUtil.alpha(tint) / 255.0F * 0.85F);
   }

   private static void setupState() {
      GlStateManager.disableLighting();
      GlStateManager.disableDepth();
      GlStateManager.disableCull();
      GlStateManager.disableAlpha();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
      GlStateManager.enableTexture2D();
      GlStateManager.shadeModel(7425);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void restoreState() {
      GlStateManager.shadeModel(7424);
      GlStateManager.enableAlpha();
      GlStateManager.enableDepth();
      GlStateManager.enableCull();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void drawPanel() {
      float r = Theme.PANEL_RADIUS;
      if (Theme.GLASS && GlassRenderer.isReady()) {
         Ui.shadow(this.panelX, this.panelY + 10.0F, this.panelW, this.panelH, r, 30.0F, Theme.SHADOW);
         GlassRenderer.drawGlass(
            this.panelX, this.panelY, this.panelW, this.panelH, r, Theme.GLASS_TINT, Theme.GLASS_RIM, Theme.GLASS_BEVEL, Theme.GLASS_REFRACT, Ui.alpha()
         );
         Ui.glow(this.panelX + Math.max(60.0F, this.sideW) * 0.4F, this.panelY + 20.0F, 300.0F, ColorUtil.withAlpha(this.accent, 0.1F));
      } else {
         Ui.shadow(this.panelX, this.panelY + 4.0F, this.panelW, this.panelH, r, 22.0F, Theme.SHADOW);
         Ui.roundRect(this.panelX, this.panelY, this.panelW, this.panelH, r, Theme.PANEL);
         if (Theme.GLASS) {
            Ui.gradientV(this.panelX, this.panelY, this.panelW, this.panelH * 0.55F, r, Theme.PANEL_SHEEN, Theme.PANEL_SHEEN & 16777215);
            Ui.glow(this.panelX + this.panelW * 0.28F, this.panelY + 8.0F, 520.0F, 318767103);
            Ui.outline(this.panelX + 1.5F, this.panelY + 1.5F, this.panelW - 3.0F, this.panelH - 3.0F, r - 1.5F, 1.0F, Theme.PANEL_RIM);
         } else {
            Ui.gradientV(this.panelX, this.panelY, this.panelW, 90.0F, r, Theme.PANEL_SHEEN, Theme.PANEL_SHEEN & 16777215);
         }

         Ui.glow(this.panelX + Math.max(60.0F, this.sideW) * 0.4F, this.panelY + 20.0F, 300.0F, ColorUtil.withAlpha(this.accent, Theme.LIGHT ? 0.05F : 0.09F));
         Ui.outline(this.panelX, this.panelY, this.panelW, this.panelH, r, 1.0F, Theme.PANEL_BORDER);
      }
   }

   private void drawGrip(float dt) {
      boolean over = this.barDrag == null && this.active == null && this.onGrip(this.mouseX, this.mouseY);
      this.gripHover = Easing.approach(this.gripHover, !over && !this.resizing ? 0.0F : 1.0F, 16.0F, dt);
      float ex = this.panelX + this.panelW - 7.0F;
      float ey = this.panelY + this.panelH - 7.0F;
      Ui.registerControl(this.panelX + this.panelW - 16.0F, this.panelY + this.panelH - 16.0F, 16.0F, 16.0F, "resize grip");
      int c = ColorUtil.lerp(ColorUtil.withAlpha(Theme.TEXT_MUTED, 0.5F), this.accent, this.gripHover);

      for (int i = 1; i <= 3; i++) {
         float d = i * 3.0F;
         Ui.line(ex - d, ey, ex, ey - d, 0.9F, true, c);
      }

      if (over && !this.resizing) {
         this.frameTooltip = "Drag to resize the menu (it stays centered); double-click for its usual size";
      }
   }

   private boolean onGrip(float mx, float my) {
      return mx >= this.panelX + this.panelW - 16.0F
         && mx < this.panelX + this.panelW + 2.0F
         && my >= this.panelY + this.panelH - 16.0F
         && my < this.panelY + this.panelH + 2.0F;
   }

   private void resizeDrag(float mx, float my) {
      float kx = this.docked ? 1.0F : 2.0F;
      this.resizeW = Math.max(400.0F, Math.min(Math.max(400.0F, this.vw - 40.0F), this.resizeStartW + (mx - this.resizeStartX) * kx));
      this.resizeH = Math.max(240.0F, Math.min(Math.max(240.0F, this.vh - 40.0F), this.resizeStartH + (my - this.resizeStartY) * 2.0F));
   }

   private void resetPanelSize() {
      MenuPrefs.PANEL_W.set(0.0);
      MenuPrefs.PANEL_H.set(0.0);
      MenuPrefs.FULL_SCREEN.set(false);
      ModSounds.gui(Sfx.CLICK);
   }

   private void drawSidebar(float dt) {
      if (!(this.sideW < 1.0F)) {
         float r = Theme.PANEL_RADIUS;
         if (Theme.GLASS && GlassRenderer.isReady()) {
            float gw = this.sideW - 10.0F;
            if (gw > 4.0F) {
               GlassRenderer.drawGlass(
                  this.panelX + 5.0F,
                  this.panelY + 5.0F,
                  gw,
                  this.panelH - 10.0F,
                  Math.min(r - 5.0F, gw / 2.0F),
                  paneTint(Theme.GLASS_TINT),
                  ColorUtil.withAlpha(Theme.GLASS_RIM, 0.75F),
                  14.0F,
                  6.0F,
                  Ui.alpha()
               );
            }
         } else {
            Ui.pushScissor(this.panelX, this.panelY, this.sideW, this.panelH);
            Ui.roundRect(this.panelX, this.panelY, this.sideW + r * 2.0F, this.panelH, r, Theme.SIDEBAR);
            Ui.popScissor();
            Ui.rect(this.panelX + this.sideW, this.panelY + 14.0F, 0.8F, this.panelH - 28.0F, Theme.DIVIDER);
         }

         if (this.sideMode != MenuPrefs.SidebarStyle.HIDDEN) {
            boolean icons = this.sideMode == MenuPrefs.SidebarStyle.ICONS;
            Ui.pushScissor(this.panelX, this.panelY, this.sideW, this.panelH);
            float lx = icons ? this.panelX + this.sideW / 2.0F - 7.0F : this.panelX + 20.0F;
            float ly = this.panelY + 24.0F;
            Ui.glow(lx + 7.0F, ly + 10.0F, 54.0F, ColorUtil.withAlpha(this.accent, 0.6F));
            Ui.circle(lx + 7.0F, ly + 10.0F, 5.5F, this.accent);
            Ui.circle(lx + 7.0F, ly + 10.0F, 2.4F, -1);
            if (!icons) {
               Ui.textHeavy("FURIAH'S", lx + 21.0F, ly - 1.0F, 7.6F, this.accent | 0xFF000000);
               Ui.textBold("Visuals", lx + 21.0F, ly + 8.0F, 14.0F, Theme.TEXT);
            }

            if (icons) {
               this.drawMagnifier(dt);
            }

            this.drawNav(dt, icons);
            this.drawSettingsButton(dt, icons);
            this.drawPresetsButton(dt, icons);
            Ui.popScissor();
         }
      }
   }

   private static float navText() {
      return Density.dense() ? 8.6F : (Density.compact() ? 9.2F : 10.0F);
   }

   private void drawMagnifier(float dt) {
      float x = this.magX;
      float y = this.magY;
      float s = this.magS;
      boolean over = this.barDrag == null && inside(this.mouseX, this.mouseY, x, y, s, s);
      this.magHover = Easing.approach(this.magHover, over ? 1.0F : 0.0F, 16.0F, dt);
      Ui.registerControl(x, y, s, s, "magnifier");
      float rr = s / 2.6F;
      if (this.globalPage) {
         Ui.roundRect(x, y, s, s, rr, Theme.ACCENT_SOFT);
         Ui.roundRect(x - 6.0F, y + s * 0.28F, 2.5F, s * 0.44F, 1.5F, this.accent);
      } else if (this.magHover > 0.01F) {
         Ui.roundRect(x, y, s, s, rr, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, this.magHover));
      }

      MenuIcons.search(x + s / 2.0F, y + s / 2.0F, s * 0.46F, this.globalPage ? this.accent : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.magHover));
      if (over && !this.globalPage) {
         this.frameTooltip = "Search every section (Ctrl+Shift+F)";
      }
   }

   static void navDot(float cx, float cy, boolean on, float hover, int accent, float r) {
      if (hover > 0.01F) {
         Ui.circle(cx, cy, r + 3.4F * hover, ColorUtil.withAlpha(on ? accent : Theme.TEXT_MUTED, 0.16F * hover));
         Ui.outline(
            cx - r - 3.4F, cy - r - 3.4F, (r + 3.4F) * 2.0F, (r + 3.4F) * 2.0F, r + 3.4F, 0.8F, ColorUtil.withAlpha(on ? accent : Theme.TEXT_DIM, 0.7F * hover)
         );
      }

      if (on) {
         Ui.glow(cx, cy, r * 6.5F, ColorUtil.withAlpha(accent, 0.5F));
         Ui.circle(cx, cy, r, accent);
      } else {
         Ui.circle(cx, cy, r, Theme.TEXT_MUTED);
      }
   }

   private float[] dotAnim(Module m) {
      float[] a = this.dotAnim.get(m);
      if (a == null) {
         a = new float[1];
         this.dotAnim.put(m, a);
      }

      return a;
   }

   private float[] navDotAt(FuriahScreen.NavEntry e, float y) {
      if (this.sideMode == MenuPrefs.SidebarStyle.ICONS) {
         float x = this.navIconX(e.h);
         return new float[]{x + e.h - 4.0F, y + 4.0F};
      } else {
         return new float[]{this.panelX + 10.0F + this.navRowW() - 12.0F, y + e.h / 2.0F};
      }
   }

   private boolean onNavDot(FuriahScreen.NavEntry e, float mx, float my) {
      if (e.module == null) {
         return false;
      } else {
         float y = this.navTop - this.navBar.scroll + e.y;
         float[] d = this.navDotAt(e, y);
         return this.sideMode == MenuPrefs.SidebarStyle.ICONS
            ? Math.abs(mx - d[0]) <= 4.5F && Math.abs(my - d[1]) <= 4.5F
            : Math.abs(mx - d[0]) <= 8.0F && my >= y && my < y + e.h;
      }
   }

   private float[] dotsArea() {
      if (this.sideMode == MenuPrefs.SidebarStyle.HIDDEN) {
         return new float[]{this.hdrSectionsX, this.hdrIconY, this.buttonH, this.buttonH};
      } else {
         float top = Float.MAX_VALUE;
         float bottom = -Float.MAX_VALUE;
         float x = 0.0F;

         for (FuriahScreen.NavEntry e : this.navEntries) {
            float y;
            if (e.module != null && !((y = this.navTop - this.navBar.scroll + e.y) + e.h < this.navTop) && !(y > this.navTop + this.navH)) {
               float[] d = this.navDotAt(e, y);
               x = d[0];
               top = Math.min(top, d[1]);
               bottom = Math.max(bottom, d[1]);
            }
         }

         return top > bottom ? null : new float[]{x - 6.0F, top - 6.0F, 12.0F, bottom - top + 12.0F};
      }
   }

   private void drawNav(float dt, boolean icons) {
      this.navBar.update(this.navContent, this.navH, dt);
      Module shown = this.modulePage() ? this.module : null;
      boolean resized = Math.abs(this.navH - this.navShownH) > 0.5F;
      if (shown != this.navShown || !this.navPlaced || resized) {
         this.navShown = shown;
         this.navShownH = this.navH;
         FuriahScreen.NavEntry e = this.navEntryOf(shown);
         if (e != null) {
            float room = Math.min(22.0F, Math.max(0.0F, (this.navH - e.h) / 2.0F));
            this.navBar.reveal(e.y - room, e.h + room * 2.0F);
            if (!this.navPlaced || resized) {
               this.navBar.jumpTo(this.navBar.target);
            }
         }

         this.navPlaced = true;
      }

      float fadeTop = Math.min(22.0F, this.navBar.scroll);
      float fadeBottom = Math.min(22.0F, this.navBar.max() - this.navBar.scroll);
      boolean mouseIn = this.barDrag == null && inside(this.mouseX, this.mouseY, this.panelX, this.navTop, this.sideW, this.navH);
      Ui.pushScissor(this.panelX, this.navTop, this.sideW, this.navH);
      Ui.pushFadeV(this.navTop, fadeTop, this.navTop + this.navH, fadeBottom);

      for (FuriahScreen.NavEntry e : this.navEntries) {
         float y = this.navTop - this.navBar.scroll + e.y;
         float[] a = this.navAnim(e);
         boolean over = mouseIn && this.navHit(e, y, this.mouseX, this.mouseY);
         boolean overDot = over && this.onNavDot(e, this.mouseX, this.mouseY);
         a[0] = Easing.approach(a[0], over && !overDot ? 1.0F : 0.0F, 16.0F, dt);
         if (e.module != null) {
            float[] d = this.dotAnim(e.module);
            d[0] = Easing.approach(d[0], overDot ? 1.0F : 0.0F, 18.0F, dt);
         }

         if (!(y + e.h < this.navTop - 10.0F) && !(y > this.navTop + this.navH + 10.0F)) {
            if (e.module == null) {
               this.drawNavCategory(e, y, icons, a, over, dt);
            } else if (icons) {
               this.drawNavIcon(e, y, a[0], over && !overDot);
            } else {
               this.drawNavRow(e, y, a[0]);
            }

            if (overDot) {
               this.frameTooltip = "Turn " + e.module.getName() + (e.module.isEnabled() ? " off" : " on");
            }
         }
      }

      Ui.popFadeV();
      Ui.popScissor();
      boolean barMouse = this.barDrag == null || this.barDrag == this.navBar;
      this.navBar.draw(this.navBarX(), this.navTop, this.navH, barMouse ? this.mouseX : -10000.0F, this.mouseY, dt, this.accent);
   }

   private float[] navAnim(FuriahScreen.NavEntry e) {
      Object key = e.module != null ? e.module : "cat:" + e.category;
      float[] a = this.navAnim.get(key);
      if (a == null) {
         a = new float[]{0.0F, -1.0F};
         this.navAnim.put(key, a);
      }

      return a;
   }

   private void drawNavRow(FuriahScreen.NavEntry e, float y, float hover) {
      Module m = e.module;
      float eh = e.h;
      float ex = this.panelX + 10.0F;
      float ew = this.navRowW();
      boolean sel = m == this.module && this.modulePage();
      if (sel) {
         Ui.roundRect(ex, y, ew, eh, 8.0F, Theme.ACCENT_SOFT);
         Ui.roundRect(ex, y + eh * 0.28F, 3.0F, eh * 0.44F, 1.5F, this.accent);
      } else if (hover > 0.01F) {
         Ui.roundRect(ex, y, ew, eh, 8.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, hover));
      }

      float ts = navText();
      MenuIcons.section(
         m.getId(),
         ex + 15.0F,
         y + eh / 2.0F,
         Math.min(11.0F, eh * 0.42F),
         sel ? this.accent : ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT_DIM, 0.4F + 0.6F * hover)
      );
      boolean isNew = !MenuMemory.opened(m.getId());
      float pillW = isNew ? Ui.widthBold("NEW", 5.6F) + 7.0F : 0.0F;
      String name = Ui.ellipsize(m.getName(), ts, ew - 50.0F - (isNew ? pillW + 5.0F : 0.0F), sel);
      Ui.textMiddle(name, ex + 28.0F, y + eh / 2.0F, ts, sel ? Theme.TEXT : Theme.TEXT_DIM, sel);
      if (isNew) {
         float px = ex + 28.0F + (sel ? Ui.widthBold(name, ts) : Ui.width(name, ts)) + 5.0F;
         Ui.roundRect(px, y + eh / 2.0F - 4.5F, pillW, 9.0F, 4.5F, this.accent | 0xFF000000);
         Ui.textMiddle("NEW", px + 3.5F, y + eh / 2.0F, 5.6F, -1, true);
      }

      float[] d = this.navDotAt(e, y);
      navDot(d[0], d[1], m.isEnabled(), this.dotAnim(m)[0], this.accent, Density.compact() ? 2.4F : 2.8F);
      Ui.registerControl(ex, y, d[0] - 8.0F - ex, eh, "sidebar " + m.getName());
      Ui.registerControl(d[0] - 8.0F, y, 16.0F, eh, "dot " + m.getName());
   }

   private void drawNavIcon(FuriahScreen.NavEntry e, float y, float hover, boolean over) {
      Module m = e.module;
      float s = e.h;
      float x = this.navIconX(s);
      boolean sel = m == this.module && this.modulePage();
      float rr = s / 2.6F;
      if (sel) {
         Ui.roundRect(x, y, s, s, rr, Theme.ACCENT_SOFT);
         Ui.roundRect(x - 6.0F, y + s * 0.3F, 2.5F, s * 0.4F, 1.25F, this.accent);
      } else if (hover > 0.01F) {
         Ui.roundRect(x, y, s, s, rr, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, hover));
      }

      MenuIcons.section(m.getId(), x + s / 2.0F, y + s / 2.0F, s * 0.44F, sel ? this.accent : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, hover));
      float[] d = this.navDotAt(e, y);
      float dh = this.dotAnim(m)[0];
      Ui.registerControl(x, y, s, s, "sidebar icon " + m.getName());
      if (!m.isEnabled() && !(dh > 0.01F)) {
         Ui.outline(d[0] - 1.9F, d[1] - 1.9F, 3.8F, 3.8F, 1.9F, 0.7F, ColorUtil.withAlpha(Theme.TEXT_MUTED, 0.8F));
      } else {
         navDot(d[0], d[1], m.isEnabled(), dh, this.accent, 1.9F);
      }

      if (over) {
         String summary = m.summary();
         this.frameTooltip = m.getName() + (summary != null ? "  ·  " + summary : "") + (m.isEnabled() ? "" : "  (off)");
      }
   }

   private float navIconX(float s) {
      return this.panelX + (this.sideW - s) / 2.0F - (this.navBar.isNeeded() ? 2.0F : 0.0F);
   }

   private void drawNavCategory(FuriahScreen.NavEntry e, float y, boolean icons, float[] a, boolean over, float dt) {
      if (icons) {
         if (e.y > 0.0F) {
            Ui.rect(this.panelX + this.sideW * 0.3F, y + e.h / 2.0F, this.sideW * 0.4F, 0.8F, Theme.DIVIDER);
         }
      } else {
         boolean folded = categoryFolded(e.category);
         a[1] = a[1] < 0.0F ? (folded ? 0.0F : 1.0F) : Easing.approach(a[1], folded ? 0.0F : 1.0F, 16.0F, dt);
         float ex = this.panelX + 10.0F;
         float ew = this.navRowW();
         float cy = y + e.h / 2.0F + 1.0F;
         Ui.registerControl(ex, y, ew, e.h, "category " + e.category);
         float ts = Density.dense() ? 6.6F : 7.0F;
         Ui.textMiddle(e.category.toUpperCase(Locale.ROOT), ex + 6.0F, cy, ts, ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT_DIM, a[0]), true);
         MenuIcons.chevron(ex + ew - 8.0F, cy, 2.6F, a[1], ColorUtil.lerp(Theme.TEXT_MUTED, this.accent, 0.3F + 0.7F * a[0]));
         if (folded) {
            int on = 0;
            int n = 0;

            for (Module m : SectionCategories.in(e.category, this.modules.getModules())) {
               n++;
               if (m.isEnabled()) {
                  on++;
               }
            }

            String s = on + " of " + n + " on";
            Ui.textMiddle(s, ex + ew - 17.0F - Ui.width(s, 6.8F), cy, 6.8F, Theme.TEXT_MUTED, false);
         }

         if (over) {
            this.frameTooltip = folded ? "Show the " + e.category + " sections" : "Fold the " + e.category + " sections away";
         }
      }
   }

   private FuriahScreen.NavEntry navEntryOf(Module m) {
      if (m == null) {
         return null;
      } else {
         for (FuriahScreen.NavEntry e : this.navEntries) {
            if (e.module == m) {
               return e;
            }
         }

         return null;
      }
   }

   private boolean navHit(FuriahScreen.NavEntry e, float y, float mx, float my) {
      return this.sideMode != MenuPrefs.SidebarStyle.ICONS
         ? inside(mx, my, this.panelX + 10.0F, y, this.navRowW(), e.h)
         : e.module != null && inside(mx, my, this.navIconX(e.h), y, e.h, e.h);
   }

   private float navRowW() {
      return this.sideW - 20.0F - (this.navBar.isNeeded() ? 6.0F : 0.0F);
   }

   private float navBarX() {
      return this.panelX + this.sideW - (this.sideMode == MenuPrefs.SidebarStyle.ICONS ? 4.0F : 10.0F);
   }

   private FuriahScreen.NavEntry navAt(float mx, float my) {
      if (this.sideMode != MenuPrefs.SidebarStyle.HIDDEN && inside(mx, my, this.panelX, this.navTop, this.sideW, this.navH)) {
         for (FuriahScreen.NavEntry e : this.navEntries) {
            if (this.navHit(e, this.navTop - this.navBar.scroll + e.y, mx, my)) {
               return e;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void drawSettingsButton(float dt, boolean icons) {
      float x = this.settingsX;
      float y = this.settingsY;
      float w = this.settingsW;
      float h = this.setH;
      boolean over = this.barDrag == null && inside(this.mouseX, this.mouseY, x, y, w, h);
      this.settingsHover = Easing.approach(this.settingsHover, over ? 1.0F : 0.0F, 16.0F, dt);
      Ui.registerControl(x, y, w, h, "Settings");
      if (icons) {
         Ui.rect(this.panelX + this.sideW * 0.25F, this.presetsY - 7.0F, this.sideW * 0.5F, 0.8F, Theme.DIVIDER);
      } else {
         Ui.rect(x + 4.0F, y - (Density.compact() ? 6.0F : 10.0F), this.sideW - 28.0F, 0.8F, Theme.DIVIDER);
      }

      float rr = icons ? h / 2.6F : 8.0F;
      boolean lit = this.settingsPage && !this.globalPage;
      if (lit) {
         Ui.roundRect(x, y, w, h, rr, Theme.ACCENT_SOFT);
         Ui.roundRect(icons ? x - 6.0F : x, y + h * 0.28F, icons ? 2.5F : 3.0F, h * 0.44F, 1.5F, this.accent);
      } else if (this.settingsHover > 0.01F) {
         Ui.roundRect(x, y, w, h, rr, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, this.settingsHover));
      }

      int iconColor = lit ? this.accent : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.settingsHover);
      float angle = (float)(ClientClock.seconds() * 0.6 * this.settingsHover);
      if (icons) {
         gear(x + w / 2.0F, y + h / 2.0F, h * 0.16F, iconColor, angle);
         if (over && !lit) {
            this.frameTooltip = "Settings: global color, menu key (" + menuKeyName() + ") and theme";
         }
      } else {
         gear(x + 17.0F, y + h / 2.0F, Math.min(4.6F, h * 0.16F), iconColor, angle);
         float ts = navText();
         Ui.textMiddle("Settings", x + 29.0F, y + h / 2.0F, ts, lit ? Theme.TEXT : Theme.TEXT_DIM, lit);
         String key = Ui.ellipsize(menuKeyName(), 7.6F, w - 84.0F, true);
         Ui.textMiddle(key, x + w - 8.0F - Ui.widthBold(key, 7.6F), y + h / 2.0F, 7.6F, Theme.TEXT_MUTED, true);
         Ui.text("/furiah  ·  v1.3.5", this.panelX + 20.0F, y + h + 8.0F, Density.compact() ? 7.6F : 8.0F, Theme.TEXT_MUTED);
         if (over && !lit) {
            this.frameTooltip = "Global color, menu key and theme";
         }
      }
   }

   private void drawPresetsButton(float dt, boolean icons) {
      float x = this.presetsX;
      float y = this.presetsY;
      float s = this.setH;
      boolean over = this.barDrag == null && inside(this.mouseX, this.mouseY, x, y, s, s);
      this.presetsHover = Easing.approach(this.presetsHover, over ? 1.0F : 0.0F, 16.0F, dt);
      Ui.registerControl(x, y, s, s, "Presets");
      float rr = icons ? s / 2.6F : 8.0F;
      boolean lit = this.presetsPage && !this.globalPage;
      if (lit) {
         Ui.roundRect(x, y, s, s, rr, Theme.ACCENT_SOFT);
         if (icons) {
            Ui.roundRect(x - 6.0F, y + s * 0.28F, 2.5F, s * 0.44F, 1.5F, this.accent);
         }
      } else if (this.presetsHover > 0.01F) {
         Ui.roundRect(x, y, s, s, rr, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, this.presetsHover));
      }

      int color = lit ? this.accent : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.presetsHover);
      layers(x + s / 2.0F, y + s / 2.0F, color, this.presetsHover);
      if (over && !lit) {
         this.frameTooltip = "Presets: save and load all your settings";
      }
   }

   static void layers(float cx, float cy, int color, float hover) {
      float spread = 3.1F + 0.8F * hover;

      for (int i = 1; i >= -1; i--) {
         float ly = cy + i * spread;
         float w = 12.0F - (i + 1) * 1.2F;
         Ui.roundRect(cx - w / 2.0F, ly - 1.9F, w, 3.8F, 1.9F, i == -1 ? color : ColorUtil.mulAlpha(color, i == 0 ? 0.75F : 0.5F));
      }
   }

   static void gear(float cx, float cy, float r, int color, float angle) {
      for (int i = 0; i < 8; i++) {
         double a = angle + i * Math.PI / 4.0;
         Ui.circle(cx + (float)Math.cos(a) * r * 1.02F, cy + (float)Math.sin(a) * r * 1.02F, r * 0.3F, color);
      }

      Ui.outline(cx - r * 0.85F, cy - r * 0.85F, r * 1.7F, r * 1.7F, r * 0.85F, r * 0.42F, color);
   }

   private void drawSettingsPage(float dt) {
      float areaW = this.settingsAreaW();
      boolean mouseIn = inside(this.mouseX, this.mouseY, this.listX - 10.0F, this.listY, areaW + 20.0F, this.listH) && this.barDrag == null;
      float mx = !mouseIn && this.active == null ? -10000.0F : this.mouseX;
      float my = !mouseIn && this.active == null ? -10000.0F : this.mouseY;
      Map<String, SearchResult> match = this.settingsMatches();
      boolean all = match == null;
      Ui.pushScissor(this.listX - 10.0F, this.listY, areaW + 20.0F, this.listH);
      float y;
      float top = y = this.listY - this.settingsBar.scroll;
      float pulseY = -1.0F;
      float pulseH = 0.0F;
      boolean global = all || match.containsKey("global");
      boolean setAll = all || match.containsKey("setall");
      this.globalWidget.y = -10000.0F;
      this.setAllY = -10000.0F;
      if (global || setAll) {
         this.searchTitle("GLOBAL COLOR", this.listX, y, areaW, all ? null : first(match, "global", "setall"));
         y += 24.0F;
         if (global) {
            Ui.text("Every color set to Global follows this one, live, in every section.", this.listX, y, 8.4F, Theme.TEXT_DIM);
            this.globalWidget.x = this.listX;
            float var31;
            this.globalWidget.y = var31 = y + 14.0F;
            this.globalWidget.w = areaW;
            this.globalWidget.render(mx, my, dt, this.accent);
            if ("global".equals(this.settingsReveal)) {
               pulseY = var31;
               pulseH = this.globalWidget.height();
            }

            y = var31 + (this.globalWidget.height() + 8.0F);
         }

         if (setAll) {
            this.drawSetAllButton(dt, areaW, y, mx, my);
            if ("setall".equals(this.settingsReveal)) {
               pulseY = y;
               pulseH = 22.0F;
            }

            y += 22.0F;
         }

         y += 22.0F;
      }

      this.keyY = -10000.0F;
      if (all || match.containsKey("key")) {
         this.searchTitle("MENU KEY", this.listX, y, areaW, all ? null : match.get("key"));
         this.keyX = this.listX + areaW - this.keyW;
         this.keyY = y + 34.0F;
         Ui.textMiddle("Open this menu", this.listX, this.keyY + 11.0F - 6.0F, 10.0F, Theme.TEXT, false);
         Ui.textMiddle("Click the key, then press the new one.", this.listX, this.keyY + 11.0F + 7.0F, 8.0F, Theme.TEXT_DIM, false);
         this.drawMenuKey(dt);
         if ("key".equals(this.settingsReveal)) {
            pulseY = this.keyY;
            pulseH = 22.0F;
         }

         y += 84.0F;
      }

      this.viewThemeY = -10000.0F;
      this.sidebarY = -10000.0F;
      this.scaleY = -10000.0F;
      this.scaleWidget.y = -10000.0F;
      this.fullScreenWidget.y = -10000.0F;
      this.sizeResetY = -10000.0F;
      boolean showTheme = all || match.containsKey("viewTheme");
      boolean showSidebar = all || match.containsKey("sidebar");
      boolean showScale = all || match.containsKey("guiScale");
      boolean showPanel = all || match.containsKey("panel");
      boolean showGuide = all || match.containsKey("guide");
      this.guideButtonY = -10000.0F;
      if (showTheme || showSidebar || showScale || showPanel || showGuide) {
         this.searchTitle("INTERFACE", this.listX, y, areaW, all ? null : first(match, "viewTheme", "sidebar", "guiScale", "panel", "guide"));
         y += 30.0F;
         float cw = Math.min(300.0F, areaW * 0.62F);
         if (showTheme) {
            this.viewThemeX = this.listX + areaW - cw;
            this.viewThemeW = cw;
            this.viewThemeY = y;
            Ui.textMiddle("View theme", this.listX, y + 10.0F - 6.0F, 10.0F, Theme.TEXT, false);
            Ui.textMiddle(
               Ui.ellipsize(MenuPrefs.VIEW_THEME.get().description(), 8.0F, this.viewThemeX - this.listX - 12.0F, false),
               this.listX,
               y + 10.0F + 7.0F,
               8.0F,
               Theme.TEXT_DIM,
               false
            );
            String[] labels2 = new String[Density.Mode.values().length];

            for (int i2 = 0; i2 < labels2.length; i2++) {
               labels2[i2] = Density.Mode.values()[i2].displayName();
            }

            this.viewThemeControl.render(this.viewThemeX, y, cw, 20.0F, labels2, MenuPrefs.VIEW_THEME.get().ordinal(), mx, my, dt, this.accent);
            if ("viewTheme".equals(this.settingsReveal)) {
               pulseY = y;
               pulseH = 22.0F;
            }

            int k;
            if (inside(mx, my, this.viewThemeX, y, cw, 20.0F)
               && (k = Segmented.hit(this.viewThemeX, y, cw, 20.0F, labels2.length, mx, my)) >= 0
               && this.frameTooltip == null) {
               this.frameTooltip = Density.Mode.values()[k].description();
            }

            y += 34.0F;
         }

         if (showSidebar) {
            this.sidebarX = this.listX + areaW - cw;
            this.sidebarW = cw;
            this.sidebarY = y;
            MenuPrefs.SidebarStyle style = MenuPrefs.SIDEBAR.get();
            Ui.textMiddle("Sidebar", this.listX, y + 10.0F - 6.0F, 10.0F, Theme.TEXT, false);
            String note = style == MenuPrefs.SidebarStyle.FULL && this.sideMode == MenuPrefs.SidebarStyle.ICONS
               ? "Icons only for now: the menu is narrow"
               : style.description();
            Ui.textMiddle(Ui.ellipsize(note, 8.0F, this.sidebarX - this.listX - 12.0F, false), this.listX, y + 10.0F + 7.0F, 8.0F, Theme.TEXT_DIM, false);
            MenuPrefs.SidebarStyle[] styles = MenuPrefs.SidebarStyle.values();
            String[] labels = new String[styles.length];

            for (int i = 0; i < labels.length; i++) {
               labels[i] = styles[i].displayName();
            }

            this.sidebarControl.render(this.sidebarX, y, cw, 20.0F, labels, style.ordinal(), mx, my, dt, this.accent);
            if ("sidebar".equals(this.settingsReveal)) {
               pulseY = y;
               pulseH = 22.0F;
            }

            int k;
            if (inside(mx, my, this.sidebarX, y, cw, 20.0F)
               && (k = Segmented.hit(this.sidebarX, y, cw, 20.0F, labels.length, mx, my)) >= 0
               && this.frameTooltip == null) {
               this.frameTooltip = styles[k].description();
            }

            y += 34.0F;
         }

         if (showScale) {
            float sw = Math.min(380.0F, areaW * 0.72F);
            this.scaleX = this.listX + areaW - sw;
            this.scaleW = sw;
            this.scaleY = y;
            boolean auto = MenuPrefs.GUI_SCALE_AUTO.get();
            int pct = MenuPrefs.GUI_SCALE.getInt();
            String[] labels = new String[MenuPrefs.SCALE_STEPS.length + 1];
            labels[0] = "Auto";
            int sel = auto ? 0 : -1;

            for (int i3 = 0; i3 < MenuPrefs.SCALE_STEPS.length; i3++) {
               labels[i3 + 1] = MenuPrefs.SCALE_STEPS[i3] + "%";
               if (!auto && MenuPrefs.SCALE_STEPS[i3] == pct) {
                  sel = i3 + 1;
               }
            }

            Ui.textMiddle("GUI scale", this.listX, y + 10.0F - 6.0F, 10.0F, Theme.TEXT, false);
            int eff = this.scalePercent();
            String note = auto
               ? "Auto: " + eff + " % in this window"
               : (eff != pct ? eff + " % (the most this window fits)" : "Ctrl+scroll zooms, Ctrl+0: Auto");
            Ui.textMiddle(Ui.ellipsize(note, 8.0F, this.scaleX - this.listX - 12.0F, false), this.listX, y + 10.0F + 7.0F, 8.0F, Theme.TEXT_DIM, false);
            this.scaleControl.render(this.scaleX, y, sw, 20.0F, labels, sel, mx, my, dt, this.accent);
            if ("guiScale".equals(this.settingsReveal)) {
               pulseY = y;
               pulseH = 28.0F + this.scaleWidget.height();
            }

            if (inside(mx, my, this.scaleX, y, sw, 20.0F) && this.frameTooltip == null) {
               int k = Segmented.hit(this.scaleX, y, sw, 20.0F, labels.length, mx, my);
               this.frameTooltip = k == 0
                  ? "A size that fits the window (Ctrl+0)"
                  : (k > 0 ? "The menu at " + labels[k] + " of its size in a 1080p window" : null);
            }

            this.syncScaleDraft();
            this.scaleWidget.x = this.listX;
            float var33;
            this.scaleWidget.y = var33 = y + 28.0F;
            this.scaleWidget.w = areaW;
            this.scaleWidget.render(mx, my, dt, this.accent);
            y = var33 + (this.scaleWidget.height() + 10.0F);
         }

         if (showPanel) {
            this.fullScreenWidget.x = this.listX;
            this.fullScreenWidget.y = y;
            this.fullScreenWidget.w = areaW;
            this.fullScreenWidget.render(mx, my, dt, this.accent);
            if ("panel".equals(this.settingsReveal)) {
               pulseY = y;
               pulseH = this.fullScreenWidget.height();
            }

            boolean custom = MenuPrefs.PANEL_W.getInt() > 0 || MenuPrefs.PANEL_H.getInt() > 0;
            String hint = MenuPrefs.FULL_SCREEN.get()
               ? "The menu fills the window. Drag its bottom-right corner to give it a size of its own."
               : (
                  custom
                     ? "Its own size: "
                        + Math.round(this.panelW)
                        + " × "
                        + Math.round(this.panelH)
                        + ". Drag its bottom-right corner, double-click it for the usual size."
                     : "Drag the menu's bottom-right corner to resize it (it stays centered)."
               );
            float hintRoom = areaW - (custom ? 90.0F : 0.0F);
            float var34;
            Ui.textMiddle(
               Ui.ellipsize(hint, 8.0F, hintRoom, false), this.listX, (var34 = y + (this.fullScreenWidget.height() + 2.0F)) + 9.0F, 8.0F, Theme.TEXT_DIM, false
            );
            if (custom) {
               String label = "Usual size";
               this.sizeResetW = Ui.width(label, 8.4F) + 20.0F;
               this.sizeResetX = this.listX + areaW - this.sizeResetW;
               this.sizeResetY = var34;
               boolean over = inside(mx, my, this.sizeResetX, var34, this.sizeResetW, 18.0F);
               this.sizeResetHover = Easing.approach(this.sizeResetHover, over ? 1.0F : 0.0F, 16.0F, dt);
               Ui.surface(this.sizeResetX, var34, this.sizeResetW, 18.0F, 9.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.sizeResetHover));
               Ui.textMiddle(label, this.sizeResetX + 10.0F, var34 + 9.0F, 8.4F, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.sizeResetHover), false);
               if (over) {
                  this.frameTooltip = "Back to the menu's usual size (or double-click its corner)";
               }
            }

            y = var34 + 24.0F;
         }

         if (showGuide) {
            Ui.textMiddle("Welcome guide", this.listX, y + 10.0F - 6.0F, 10.0F, Theme.TEXT, false);
            Ui.textMiddle("Four short steps: the look, the size, your effects, where things are.", this.listX, y + 10.0F + 7.0F, 8.0F, Theme.TEXT_DIM, false);
            String label = this.guide != null ? "Showing" : "Show again";
            this.guideButtonW = Ui.width(label, 8.6F) + 22.0F;
            this.guideButtonX = this.listX + areaW - this.guideButtonW;
            this.guideButtonY = y + 1.0F;
            boolean over = this.guide == null && inside(mx, my, this.guideButtonX, this.guideButtonY, this.guideButtonW, 20.0F);
            this.guideButtonHover = Easing.approach(this.guideButtonHover, over ? 1.0F : 0.0F, 16.0F, dt);
            Ui.surface(
               this.guideButtonX, this.guideButtonY, this.guideButtonW, 20.0F, 10.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.guideButtonHover)
            );
            Ui.outline(
               this.guideButtonX,
               this.guideButtonY,
               this.guideButtonW,
               20.0F,
               10.0F,
               0.9F,
               ColorUtil.lerp(Theme.ACCENT_LINE, Theme.ACCENT, this.guideButtonHover)
            );
            Ui.textMiddle(
               label,
               this.guideButtonX + 11.0F,
               this.guideButtonY + 10.0F,
               8.6F,
               this.guide != null ? Theme.TEXT_MUTED : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.guideButtonHover),
               false
            );
            if ("guide".equals(this.settingsReveal)) {
               pulseY = y;
               pulseH = 22.0F;
            }

            y += 34.0F;
         }

         y += 16.0F;
      }

      this.flashWidget.y = -10000.0F;
      if (all || match.containsKey("flash")) {
         SearchResult r = all ? null : match.get("flash");
         this.searchTitle("ACCESSIBILITY", this.listX, y, areaW, r);
         this.flashWidget.x = this.listX;
         float var35;
         this.flashWidget.y = var35 = y + 30.0F;
         this.flashWidget.w = areaW;
         this.flashWidget.render(mx, my, dt, this.accent);
         Ui.text(
            "No effect flashes the screen (Kill Effects' screen pulse stays off).",
            this.listX,
            var35 + this.flashWidget.height() + 1.0F,
            8.0F,
            Theme.TEXT_MUTED
         );
         if ("flash".equals(this.settingsReveal)) {
            pulseY = var35;
            pulseH = this.flashWidget.height();
         }

         y = var35 + (this.flashWidget.height() + 26.0F);
      }

      HashSet<String> soundKeys = null;
      if (!all) {
         soundKeys = new HashSet<>();

         for (String k : match.keySet()) {
            if (k.startsWith("sound:")) {
               soundKeys.add(k);
            }
         }
      }

      if (all || !soundKeys.isEmpty()) {
         this.searchTitle("SOUNDS", this.listX, y, areaW, null);
         float var36;
         float h = this.soundsBlock.render(this.listX, var36 = y + 30.0F, areaW, soundKeys, mx, my, dt, this.accent);
         if (this.settingsReveal != null && this.settingsReveal.startsWith("sound:")) {
            pulseY = var36;
            pulseH = h;
         }

         y = var36 + (h + 20.0F);
      }

      Theme.Style current = FuriahsVisuals.instance.getTheme().get();
      Theme.Style[] styles = Theme.Style.values();
      int shown = 0;

      for (int i4 = 0; i4 < styles.length; i4++) {
         this.cardX[i4] = -10000.0F;
         this.cardY[i4] = -10000.0F;
         if (all || match.containsKey("theme:" + styles[i4].name())) {
            shown++;
         }
      }

      if (shown > 0) {
         this.searchTitle("THEME", this.listX, y, areaW, null);
         int slot = 0;

         for (int i = 0; i < styles.length; i++) {
            SearchResult r = all ? null : match.get("theme:" + styles[i].name());
            if (all || r != null) {
               this.cardX[i] = this.listX + slot % 2 * (this.cardW + 10.0F);
               this.cardY[i] = y + 28.0F + slot / 2 * (this.cardH + 10.0F);
               slot++;
               boolean over = inside(mx, my, this.cardX[i], this.cardY[i], this.cardW, this.cardH);
               this.cardHover[i] = Easing.approach(this.cardHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
               this.drawThemeCard(i, styles[i], this.cardX[i], this.cardY[i], this.cardW, this.cardH, styles[i] == current, this.cardHover[i]);
               if (("theme:" + styles[i].name()).equals(this.settingsReveal)) {
                  pulseY = this.cardY[i];
                  pulseH = this.cardH;
               }
            }
         }

         y += 28.0F + (shown + 1) / 2 * (this.cardH + 10.0F);
      }

      if (this.settingsSearch != null && this.settingsSearch.isActive()) {
         y += this.settingsSearch.drawRows(this.listX, y, areaW, this.listY, this.listY + this.listH, mx, my, this.active, dt, this.accent);
      }

      if (this.settingsReveal != null) {
         float t = (float)(ClientClock.nanos() - this.settingsRevealStart) / 1.0E9F;
         if (t > 1.6F) {
            this.settingsReveal = null;
         } else if (pulseH > 0.0F) {
            float a = (0.6F + 0.4F * (float)Math.sin(t * 8.0)) * (1.0F - Easing.smoothstep(1.0F, 1.6F, t));
            Ui.roundRect(this.listX - 6.0F, pulseY - 3.0F, areaW + 12.0F, pulseH + 6.0F, 8.0F, ColorUtil.withAlpha(Theme.ACCENT_FAINT, a));
            Ui.outline(this.listX - 6.0F, pulseY - 3.0F, areaW + 12.0F, pulseH + 6.0F, 8.0F, 1.3F, ColorUtil.withAlpha(this.accent, a));
            if (t < 0.5F) {
               this.settingsBar.reveal(pulseY - top - 12.0F, pulseH + 24.0F);
            }
         }
      }

      Ui.popScissor();
      this.settingsBar.update(y + this.settingsBar.scroll - this.listY, this.listH, dt);
      if (this.settingsRestore >= 0.0F) {
         this.settingsBar.jumpTo(this.settingsRestore);
         if (this.settingsBar.max() + 0.5F >= this.settingsRestore || ++this.restoreFrames > 20) {
            this.settingsRestore = -1.0F;
         }
      }

      this.settingsBar.draw(this.listX + areaW + 8.0F, this.listY, this.listH, this.mouseX, this.mouseY, dt, this.accent);
      if (this.frameTooltip == null && mouseIn && this.active == null) {
         this.frameTooltip = this.globalWidget.tooltip(this.mouseX, this.mouseY);
         if (this.frameTooltip == null) {
            this.frameTooltip = this.flashWidget.tooltip(this.mouseX, this.mouseY);
         }

         if (this.frameTooltip == null) {
            this.frameTooltip = this.scaleWidget.tooltip(this.mouseX, this.mouseY);
         }

         if (this.frameTooltip == null) {
            this.frameTooltip = this.fullScreenWidget.tooltip(this.mouseX, this.mouseY);
         }

         if (this.frameTooltip == null) {
            this.frameTooltip = this.soundsBlock.tooltip(this.mouseX, this.mouseY);
         }

         if (this.frameTooltip == null) {
            this.frameTooltip = null;
         }
      }
   }

   private void drawGlobalPage(float dt) {
      float areaW = this.settingsAreaW();
      boolean free = this.barDrag == null || this.barDrag == this.globalSearch.bar();
      this.globalSearch.render(this.listX, this.listY, areaW, this.listH, free ? this.mouseX : -10000.0F, free ? this.mouseY : -10000.0F, dt, this.accent);
      if (this.frameTooltip == null && inside(this.mouseX, this.mouseY, this.listX - 10.0F, this.listY, areaW + 20.0F, this.listH)) {
         this.frameTooltip = this.globalSearch.tooltip();
      }
   }

   private void drawPresetsPage(float dt) {
      float areaW = this.settingsAreaW();
      this.presetsUi.render(this.listX, this.listY, areaW, this.listH, this.mouseX, this.mouseY, dt, this.accent);
      if (this.frameTooltip == null && inside(this.mouseX, this.mouseY, this.listX - 10.0F, this.listY, areaW + 20.0F, this.listH)) {
         this.frameTooltip = this.presetsUi.tooltip(this.mouseX, this.mouseY);
      }
   }

   private void drawSetAllButton(float dt, float areaW, float y, float mx, float my) {
      boolean confirming = System.currentTimeMillis() < this.setAllConfirmUntil;
      boolean done = ClientClock.millis() < this.setAllDoneUntil;
      String label = confirming ? "Click again to confirm" : "Set all effects to Global";
      this.setAllW = Ui.width(label, 9.0F) + 24.0F;
      this.setAllX = this.listX + areaW - this.setAllW;
      this.setAllY = y;
      boolean over = inside(mx, my, this.setAllX, this.setAllY, this.setAllW, 22.0F);
      this.setAllHover = Easing.approach(this.setAllHover, over ? 1.0F : 0.0F, 16.0F, dt);
      if (confirming) {
         Ui.roundRect(this.setAllX, y, this.setAllW, 22.0F, 11.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, this.setAllHover));
         Ui.accentOutline(this.setAllX, y, this.setAllW, 22.0F, 11.0F, 1.0F, this.accent);
      } else {
         Ui.surface(this.setAllX, y, this.setAllW, 22.0F, 11.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.setAllHover));
      }

      Ui.textMiddle(
         label, this.setAllX + 12.0F, y + 11.0F, 9.0F, confirming ? this.accent : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.setAllHover), false
      );
      String note = done
         ? (
            this.setAllCount == 0
               ? "Everything already follows the Global Color."
               : this.setAllCount + (this.setAllCount == 1 ? " color now follows" : " colors now follow") + " the Global Color."
         )
         : "Switches every effect color in every section to Global.";
      Ui.textMiddle(Ui.ellipsize(note, 8.0F, areaW - this.setAllW - 16.0F, false), this.listX, y + 11.0F, 8.0F, done ? this.accent : Theme.TEXT_MUTED, false);
      if (over && !confirming) {
         this.frameTooltip = "Target Aura, Hit Effect, Landing Impact and every future section";
      }
   }

   private void searchTitle(String title, float x, float y, float w, SearchResult r) {
      this.sectionHeader(title, x, y, w);
   }

   private static SearchResult first(Map<String, SearchResult> match, String... keys) {
      for (String k : keys) {
         SearchResult r = match.get(k);
         if (r != null) {
            return r;
         }
      }

      return null;
   }

   private void sectionHeader(String title, float x, float y, float w) {
      float ty = y + 11.0F;
      Ui.glow(x + 3.0F, ty + 0.5F, 14.0F, ColorUtil.withAlpha(this.accent, 0.35F));
      Ui.circle(x + 3.0F, ty + 0.5F, 2.2F, this.accent | 0xFF000000);
      Ui.textMiddle(title, x + 11.0F, ty, 8.2F, Theme.TEXT_DIM, true);
      float tw = Ui.widthBold(title, 8.2F);
      Ui.rect(x + 17.0F + tw, ty, Math.max(0.0F, w - 17.0F - tw), 0.6F, Theme.DIVIDER);
   }

   private void drawThemeCard(int index, Theme.Style style, float x, float y, float w, float h, boolean selected, float hover) {
      Ui.surface(x, y, w, h, 10.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hover));
      if (selected) {
         Ui.roundRect(x, y, w, h, 10.0F, Theme.ACCENT_FAINT);
         Ui.accentOutline(x, y, w, h, 10.0F, 1.2F, this.accent);
      } else if (!Theme.GLASS) {
         Ui.outline(x, y, w, h, 10.0F, 0.8F, ColorUtil.lerp(Theme.OUTLINE, Theme.OUTLINE_HOVER, hover));
      }

      if (this.cardPalette[index] == null) {
         this.cardPalette[index] = new Theme.Palette();
      }

      Theme.Palette p = Theme.fill(style, this.accent, this.cardPalette[index]);
      float mx = x + 8.0F;
      float my = y + 8.0F;
      float mw = w - 16.0F;
      float mh = h - 44.0F;
      boolean live = this.mc.theWorld != null && GlassRenderer.drawWindow(mx, my, mw, mh, 6.0F, Ui.alpha());
      if (!live) {
         float horizon = mh * 0.62F;
         Ui.pushScissor(mx, my, mw, horizon);
         Ui.gradientV(mx, my, mw, horizon + 12.0F, 6.0F, -8410891, -4402182);
         Ui.popScissor();
         Ui.pushScissor(mx, my + horizon, mw, mh - horizon);
         Ui.gradientV(mx, my + horizon - 12.0F, mw, mh - horizon + 12.0F, 6.0F, -10316993, -11832271);
         Ui.popScissor();
      }

      Ui.roundRect(mx, my, mw, mh, 6.0F, p.backdropBottom);
      float px = mx + 10.0F;
      float py = my + 7.0F;
      float pw = mw - 20.0F;
      float ph = mh - 14.0F;
      boolean glassMini = p.glass && live && GlassRenderer.drawGlass(px, py, pw, ph, 7.0F, p.glassTint, p.glassRim, 7.0F, 3.5F, Ui.alpha());
      float pr = glassMini ? 7.0F : 5.0F;
      if (glassMini) {
         GlassRenderer.drawGlass(
            px + 2.0F, py + 2.0F, pw * 0.28F - 2.0F, ph - 4.0F, 5.0F, paneTint(p.glassTint), ColorUtil.withAlpha(p.glassRim, 0.75F), 4.0F, 2.0F, Ui.alpha()
         );
      } else {
         Ui.roundRect(px, py, pw, ph, pr, p.panel);
         if (p.glass) {
            Ui.gradientV(px, py, pw, ph * 0.55F, pr, p.panelSheen, p.panelSheen & 16777215);
         }

         Ui.pushScissor(px, py, pw * 0.28F, ph);
         Ui.roundRect(px, py, pw * 0.28F + 6.0F, ph, pr, p.sidebar);
         Ui.popScissor();
      }

      Ui.roundRect(px + 5.0F, py + 7.0F, pw * 0.28F - 10.0F, 5.0F, 2.5F, this.accent | 0xFF000000);
      Ui.roundRect(px + 5.0F, py + 16.0F, pw * 0.28F - 14.0F, 3.5F, 1.75F, ColorUtil.withAlpha(p.textMuted, 0.7F));
      Ui.roundRect(px + 5.0F, py + 23.0F, pw * 0.28F - 18.0F, 3.5F, 1.75F, ColorUtil.withAlpha(p.textMuted, 0.7F));
      float cx = px + pw * 0.28F + 7.0F;
      float cw = pw - pw * 0.28F - 14.0F;
      Ui.roundRect(cx, py + 7.0F, cw * 0.55F, 5.0F, 2.5F, p.text);
      Ui.roundRect(cx, py + 16.0F, cw * 0.8F, 3.5F, 1.75F, ColorUtil.withAlpha(p.textDim, 0.8F));
      Ui.roundRect(cx, py + ph - 14.0F, cw, 3.0F, 1.5F, p.track);
      Ui.roundRect(cx, py + ph - 14.0F, cw * 0.6F, 3.0F, 1.5F, this.accent);
      Ui.circle(cx + cw * 0.6F, py + ph - 12.5F, 3.0F, p.knob);
      if (!glassMini) {
         Ui.outline(px, py, pw, ph, pr, 0.8F, p.panelBorder);
      }

      Ui.textBold(style.displayName(), x + 10.0F, y + h - 32.0F, 10.0F, Theme.TEXT);
      Ui.text(Ui.ellipsize(style.description(), 8.0F, w - 36.0F, false), x + 10.0F, y + h - 18.0F, 8.0F, Theme.TEXT_DIM);
      if (selected) {
         float dx = x + w - 16.0F;
         float dy = y + h - 24.0F;
         Ui.glow(dx, dy, 18.0F, ColorUtil.withAlpha(this.accent, 0.5F));
         Ui.circle(dx, dy, 4.0F, this.accent);
         Ui.circle(dx, dy, 1.6F, Theme.KNOB);
      }
   }

   private void drawMenuKey(float dt) {
      boolean over = inside(this.mouseX, this.mouseY, this.keyX, this.keyY, this.keyW, 22.0F);
      this.keyHover = Easing.approach(this.keyHover, over ? 1.0F : 0.0F, 16.0F, dt);
      this.keyListen = Easing.approach(this.keyListen, this.listeningForKey ? 1.0F : 0.0F, 14.0F, dt);
      int bg = ColorUtil.lerp(ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.keyHover), Theme.ACCENT_SOFT, this.keyListen);
      Ui.surface(this.keyX, this.keyY, this.keyW, 22.0F, 8.0F, bg);
      if (this.listeningForKey) {
         float pulse = 0.5F + 0.5F * (float)Math.sin(ClientClock.seconds() * 6.0);
         Ui.outline(this.keyX, this.keyY, this.keyW, 22.0F, 8.0F, 1.0F, ColorUtil.withAlpha(this.accent, 0.45F + 0.5F * pulse));
         String wait = "Press a key…";
         Ui.textMiddleHeavy(wait, this.keyX + (this.keyW - Ui.widthHeavy(wait, 9.2F)) / 2.0F, this.keyY + 11.0F, 9.2F, this.accent | 0xFF000000);
      } else {
         if (!Theme.GLASS) {
            Ui.outline(this.keyX, this.keyY, this.keyW, 22.0F, 8.0F, 0.9F, ColorUtil.lerp(Theme.OUTLINE, Theme.OUTLINE_HOVER, this.keyHover));
         }

         Ui.textMiddle(Ui.ellipsize(menuKeyName(), 10.0F, this.keyW - 64.0F, true), this.keyX + 11.0F, this.keyY + 11.0F, 10.0F, Theme.TEXT, true);
         String change = "Change";
         Ui.textMiddle(
            change,
            this.keyX + this.keyW - 11.0F - Ui.width(change, 8.0F),
            this.keyY + 11.0F,
            8.0F,
            ColorUtil.lerp(Theme.TEXT_MUTED, this.accent, this.keyHover),
            false
         );
         if (over) {
            this.frameTooltip = "Click, then press the key that should open this menu";
         }
      }

      float ly = this.keyY + 22.0F + 6.0F;
      String conflict = this.listeningForKey ? null : this.menuKeyConflict();
      int noteColor = Theme.TEXT_MUTED;
      String note;
      if (this.listeningForKey) {
         note = "Esc to cancel";
      } else if (conflict != null) {
         note = Ui.ellipsize("Also used by " + conflict, 8.0F, this.keyW, false);
         noteColor = Theme.DANGER;
      } else {
         note = "or type /furiah";
      }

      Ui.text(note, this.keyX + this.keyW - Ui.width(note, 8.0F), ly, 8.0F, noteColor);
   }

   private static String menuKeyName() {
      return Keybinds.openMenu == null ? "None" : GameSettings.getKeyDisplayString(Keybinds.openMenu.getKeyCode());
   }

   private String menuKeyConflict() {
      if (Keybinds.openMenu != null && Keybinds.openMenu.getKeyCode() != 0) {
         int code = Keybinds.openMenu.getKeyCode();

         for (KeyBinding binding : this.mc.gameSettings.keyBindings) {
            if (binding != Keybinds.openMenu && binding.getKeyCode() == code) {
               return I18n.format(binding.getKeyDescription(), new Object[0]);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void setMenuKey(int keyCode) {
      if (Keybinds.openMenu != null) {
         this.mc.gameSettings.setOptionKeyBinding(Keybinds.openMenu, keyCode);
         KeyBinding.resetKeyBindingArrayAndHash();
      }
   }

   private void drawHeader(float dt) {
      float cx = this.panelX + this.sideW + 18.0F;
      float right = this.panelX + this.panelW - 18.0F;
      float row1 = this.headerRow1();
      float row2 = this.headerRow2();
      float ts = titleSize();
      float ds = descSize();
      boolean section = this.modulePage();
      boolean sub = section && this.page != null && !this.page.isSection() && !this.page.isTab();
      String desc;
      String title;
      if (this.globalPage) {
         title = "Search";
         int n = this.globalSearch.resultCount();
         desc = !this.globalSearch.searching()
            ? "Every section and Settings at once: pick a match to open it there."
            : (
               n == 0
                  ? "No matches in any section."
                  : (n == 1 ? "1 match" : n + " matches") + " in " + this.globalSearch.where() + ": pick one to open it there (Esc goes back)."
            );
      } else if (this.settingsPage) {
         title = "Settings";
         desc = "Global color, menu key, view theme and the look of this menu.";
      } else if (this.presetsPage) {
         title = "Presets";
         desc = "Save everything under a name, and switch between setups in one click.";
      } else if (sub) {
         title = this.page.title;
         desc = this.page.parent.title + "  ›  " + (this.page.description != null ? this.page.description : this.page.title);
      } else {
         title = this.module.getName();
         desc = this.module.getDescription();
      }

      float x = cx;
      if (this.hdrSectionsX > -5000.0F) {
         boolean over = this.barDrag == null && inside(this.mouseX, this.mouseY, this.hdrSectionsX, this.hdrIconY, this.buttonH, this.buttonH);
         boolean open = Popovers.current() instanceof SectionsPopover;
         this.hdrSectionsHover = Easing.approach(this.hdrSectionsHover, over ? 1.0F : 0.0F, 16.0F, dt);
         Ui.registerControl(this.hdrSectionsX, this.hdrIconY, this.buttonH, this.buttonH, "☰");
         Ui.surface(
            this.hdrSectionsX,
            this.hdrIconY,
            this.buttonH,
            this.buttonH,
            this.buttonH / 2.0F,
            open ? Theme.ACCENT_SOFT : ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hdrSectionsHover)
         );
         MenuIcons.sections(
            this.hdrSectionsX + this.buttonH / 2.0F,
            this.hdrIconY + this.buttonH / 2.0F,
            this.buttonH * 0.46F,
            open ? this.accent : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hdrSectionsHover)
         );
         if (over) {
            this.frameTooltip = "Sections: open one, or switch it on or off with its dot";
         }

         x = this.hdrSectionsX + this.buttonH + 8.0F;
      }

      if (this.backShown) {
         boolean overBack = this.barDrag == null
            && inside(this.mouseX, this.mouseY, this.backX - 2.0F, this.backY - 2.0F, this.backS + 4.0F, this.backS + 4.0F);
         this.backHover = Easing.approach(this.backHover, overBack ? 1.0F : 0.0F, 16.0F, dt);
         Ui.registerControl(this.backX, this.backY, this.backS, this.backS, "←");
         Ui.surface(this.backX, this.backY, this.backS, this.backS, this.backS / 2.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.backHover));
         MenuIcons.back(
            this.backX + this.backS / 2.0F - this.backHover * 1.2F,
            this.backY + this.backS / 2.0F,
            this.backS * 0.42F,
            ColorUtil.lerp(Theme.TEXT_DIM, this.accent, this.backHover)
         );
         if (overBack) {
            this.frameTooltip = "Back to " + this.page.parent.title + " (Esc, or the mouse's back button)";
         }

         x = this.backX + this.backS + 8.0F;
      }

      float buttonsLeft = right;

      for (float bx : new float[]{this.testX, this.resetX, this.moveX, this.hdrSettingsX, this.hdrSearchX}) {
         if (bx > -5000.0F) {
            buttonsLeft = Math.min(buttonsLeft, bx);
         }
      }

      for (float[] b : this.actionBoxes) {
         buttonsLeft = Math.min(buttonsLeft, b[0]);
      }

      float textEnd = buttonsLeft - 10.0F;
      float room = Math.max(20.0F, textEnd - x - (section ? this.switchW + 10.0F : 0.0F));
      String shownTitle = Ui.ellipsize(title, ts, room, true);
      Ui.textMiddle(shownTitle, x, row1, ts, Theme.TEXT, true);
      if (section) {
         this.switchX = x + Ui.widthBold(shownTitle, ts) + 10.0F;
         this.switchY = row1 - this.switchH / 2.0F;
         boolean overSwitch = this.barDrag == null
            && inside(this.mouseX, this.mouseY, this.switchX - 4.0F, this.switchY - 4.0F, this.switchW + 8.0F, this.switchH + 8.0F);
         this.switchHover = Easing.approach(this.switchHover, overSwitch ? 1.0F : 0.0F, 16.0F, dt);
         boolean locked = false;
         boolean on = this.headerSwitch().get() && !locked;
         float target = on ? 1.0F : 0.0F;
         float f = this.switchKnob = this.switchKnob < 0.0F ? target : Easing.approach(this.switchKnob, target, 18.0F, dt);
         if (!locked) {
            ToggleWidget.drawSwitch(this.switchX, this.switchY, this.switchW, this.switchH, this.switchKnob, this.accent);
         }

         if (overSwitch) {
            this.frameTooltip = locked
               ? ""
               : "Turns " + (this.page != null && this.page.styleSwitch != null ? this.page.title : this.module.getName()) + " " + (on ? "off" : "on");
         }
      }

      Ui.textMiddle(Ui.ellipsize(desc, ds, this.searchX - 12.0F - x, false), x, row2, ds, Theme.TEXT_DIM, false);
      if (section) {
         this.drawHeaderButtons(dt);
      }

      if (this.sideMode == MenuPrefs.SidebarStyle.HIDDEN) {
         this.drawHeaderIcons(dt);
      }

      Ui.rect(cx, this.panelY + this.headerH, right - cx, 0.8F, Theme.DIVIDER);
   }

   private void drawHeaderButtons(float dt) {
      float h = this.buttonH;
      float r = h / 2.0F;
      float bs = buttonText();
      boolean dense = Density.dense() || this.headerIcons;

      for (int i = 0; i < this.actionBoxes.size() && i < this.actionList.size(); i++) {
         Module.HeaderAction a = this.actionList.get(i);
         float[] b = this.actionBoxes.get(i);
         float[] hv = this.actionHover.get(a);
         if (hv == null) {
            hv = new float[1];
            this.actionHover.put(a, hv);
         }

         boolean over = this.barDrag == null && inside(this.mouseX, this.mouseY, b[0], b[1], b[2], b[3]);
         hv[0] = Easing.approach(hv[0], over ? 1.0F : 0.0F, 16.0F, dt);
         Ui.registerControl(b[0], b[1], b[2], b[3], a.label);
         if (a.primary) {
            Ui.roundRect(b[0], b[1], b[2], b[3], r, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, hv[0]));
            Ui.accentOutline(b[0], b[1], b[2], b[3], r, 1.0F, hv[0] > 0.5F ? Theme.ACCENT_HOVER : this.accent);
         } else {
            Ui.surface(b[0], b[1], b[2], b[3], r, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hv[0]));
         }

         int c = a.primary ? this.accent : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, hv[0]);
         float icon = h * 0.52F;
         float ix = this.actionsIconOnly ? b[0] + (b[2] - icon) / 2.0F : b[0] + 5.0F + (h * 0.55F - icon) / 2.0F;
         IslandIcons.draw(a.icon, ix, b[1] + (h - icon) / 2.0F, icon, c);
         if (!this.actionsIconOnly) {
            Ui.textMiddle(
               a.label, b[0] + h * 0.55F + 6.0F, b[1] + h / 2.0F, bs, a.primary ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, hv[0]), false
            );
         }

         if (over) {
            this.frameTooltip = this.actionsIconOnly ? a.label + ": " + a.tooltip : a.tooltip;
         }
      }

      if (this.testW > 0.0F) {
         boolean overx = this.barDrag == null && inside(this.mouseX, this.mouseY, this.testX, this.testY, this.testW, h);
         boolean usable = this.module.testUnavailable() == null;
         this.testHover = Easing.approach(this.testHover, overx && usable ? 1.0F : 0.0F, 16.0F, dt);
         Ui.registerControl(this.testX, this.testY, this.testW, h, "Test");
         float iconx = h * 0.36F;
         if (usable) {
            Ui.roundRect(this.testX, this.testY, this.testW, h, r, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, this.testHover));
            Ui.accentOutline(this.testX, this.testY, this.testW, h, r, 1.0F, this.testHover > 0.5F ? Theme.ACCENT_HOVER : this.accent);
            MenuIcons.play(dense ? this.testX + this.testW / 2.0F + 0.5F : this.testX + 5.0F + iconx * 0.6F, this.testY + h / 2.0F, iconx, this.accent);
         } else {
            Ui.surface(this.testX, this.testY, this.testW, h, r, Theme.SURFACE);
            MenuIcons.play(dense ? this.testX + this.testW / 2.0F + 0.5F : this.testX + 5.0F + iconx * 0.6F, this.testY + h / 2.0F, iconx, Theme.TEXT_MUTED);
         }

         if (!dense) {
            Ui.textMiddle(
               this.module.getTestLabel(),
               this.testX + h * 0.55F + 6.0F,
               this.testY + h / 2.0F,
               bs,
               usable ? ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, 0.5F + 0.5F * this.testHover) : Theme.TEXT_MUTED,
               false
            );
         }

         if (overx) {
            String tip = this.module.getTestTooltip();
            this.frameTooltip = dense ? this.module.getTestLabel() + (tip != null ? ": " + tip : "") : tip;
         }
      }

      boolean confirming = System.currentTimeMillis() < this.resetConfirmUntil;
      boolean overReset = this.barDrag == null && inside(this.mouseX, this.mouseY, this.resetX, this.resetY, this.resetW, h);
      this.resetHover = Easing.approach(this.resetHover, overReset ? 1.0F : 0.0F, 16.0F, dt);
      Ui.registerControl(this.resetX, this.resetY, this.resetW, h, "Reset section");
      if (confirming) {
         Ui.roundRect(this.resetX, this.resetY, this.resetW, h, r, ColorUtil.withAlpha(Theme.DANGER, 0.16F + 0.1F * this.resetHover));
         Ui.outline(this.resetX, this.resetY, this.resetW, h, r, 0.9F, ColorUtil.withAlpha(Theme.DANGER, 0.8F));
      } else {
         Ui.surface(this.resetX, this.resetY, this.resetW, h, r, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.resetHover));
      }

      String label = this.resetLabel();
      int resetColor = confirming ? Theme.DANGER : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.resetHover);
      MenuIcons.reset(dense ? this.resetX + this.resetW / 2.0F : this.resetX + 5.0F + h * 0.27F, this.resetY + h / 2.0F, h * 0.2F, resetColor);
      if (!dense) {
         Ui.textMiddle(label, this.resetX + h * 0.55F + 6.0F, this.resetY + h / 2.0F, bs, resetColor, false);
      }

      if (overReset) {
         this.frameTooltip = confirming
            ? "Click again to reset"
            : (
               this.onSubPage()
                  ? "Every setting of this page back to its default (asks for a second click)"
                  : "Every setting of " + this.module.getName() + " back to its default (asks for a second click)"
            );
      }

      if (this.moveW > 0.0F) {
         boolean inWorld = this.mc.theWorld != null;
         boolean overxx = this.barDrag == null && inside(this.mouseX, this.mouseY, this.moveX, this.moveY, this.moveW, h);
         this.moveHover = Easing.approach(this.moveHover, overxx && inWorld ? 1.0F : 0.0F, 16.0F, dt);
         Ui.registerControl(this.moveX, this.moveY, this.moveW, h, "Move HUD");
         Ui.pushAlpha(inWorld ? 1.0F : 0.45F);
         Ui.surface(this.moveX, this.moveY, this.moveW, h, r, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.moveHover));
         Ui.outline(this.moveX, this.moveY, this.moveW, h, r, 0.9F, ColorUtil.lerp(Theme.ACCENT_LINE, Theme.ACCENT, this.moveHover));
         int cx = ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.moveHover);
         MenuIcons.move(dense ? this.moveX + this.moveW / 2.0F : this.moveX + 5.0F + h * 0.27F, this.moveY + h / 2.0F, h * 0.5F, cx);
         if (!dense) {
            Ui.textMiddle("Move HUD", this.moveX + h * 0.55F + 6.0F, this.moveY + h / 2.0F, bs, cx, false);
         }

         Ui.popAlpha();
         if (overxx) {
            this.frameTooltip = inWorld
               ? "Move HUD: place the scoreboard, chat, hotbar and island (drag; scroll to resize)"
               : "Join a world first: Move HUD places the HUD over the game";
         }
      }
   }

   private void drawHeaderIcons(float dt) {
      float s = this.buttonH;
      float y = this.hdrIconY;
      boolean overF = this.barDrag == null && inside(this.mouseX, this.mouseY, this.hdrSearchX, y, s, s);
      boolean overS = this.barDrag == null && inside(this.mouseX, this.mouseY, this.hdrSettingsX, y, s, s);
      boolean overP = this.barDrag == null && inside(this.mouseX, this.mouseY, this.hdrPresetsX, y, s, s);
      this.hdrSearchHover = Easing.approach(this.hdrSearchHover, overF ? 1.0F : 0.0F, 16.0F, dt);
      this.hdrSettingsHover = Easing.approach(this.hdrSettingsHover, overS ? 1.0F : 0.0F, 16.0F, dt);
      this.hdrPresetsHover = Easing.approach(this.hdrPresetsHover, overP ? 1.0F : 0.0F, 16.0F, dt);
      boolean settingsLit = this.settingsPage && !this.globalPage;
      boolean presetsLit = this.presetsPage && !this.globalPage;
      Ui.registerControl(this.hdrSearchX, y, s, s, "header magnifier");
      Ui.registerControl(this.hdrSettingsX, y, s, s, "header Settings");
      Ui.registerControl(this.hdrPresetsX, y, s, s, "header Presets");
      Ui.surface(
         this.hdrSearchX, y, s, s, s / 2.0F, this.globalPage ? Theme.ACCENT_SOFT : ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hdrSearchHover)
      );
      MenuIcons.search(
         this.hdrSearchX + s / 2.0F, y + s / 2.0F, s * 0.5F, this.globalPage ? this.accent : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hdrSearchHover)
      );
      Ui.surface(
         this.hdrSettingsX, y, s, s, s / 2.0F, settingsLit ? Theme.ACCENT_SOFT : ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hdrSettingsHover)
      );
      gear(
         this.hdrSettingsX + s / 2.0F,
         y + s / 2.0F,
         s * 0.17F,
         settingsLit ? this.accent : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hdrSettingsHover),
         (float)(ClientClock.seconds() * 0.6 * this.hdrSettingsHover)
      );
      Ui.surface(this.hdrPresetsX, y, s, s, s / 2.0F, presetsLit ? Theme.ACCENT_SOFT : ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hdrPresetsHover));
      GlStateManager.pushMatrix();
      GlStateManager.translate(this.hdrPresetsX + s / 2.0F, y + s / 2.0F, 0.0F);
      GlStateManager.scale(s / 26.0F, s / 26.0F, 1.0F);
      layers(0.0F, 0.0F, presetsLit ? this.accent : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hdrPresetsHover), this.hdrPresetsHover);
      GlStateManager.popMatrix();
      if (overF) {
         this.frameTooltip = "Search every section (Ctrl+Shift+F)";
      } else if (overS) {
         this.frameTooltip = "Settings: global color, menu key (" + menuKeyName() + ") and theme";
      } else if (overP) {
         this.frameTooltip = "Presets: save and load all your settings";
      }
   }

   private BooleanSetting headerSwitch() {
      return this.page != null && this.page.styleSwitch != null ? this.page.styleSwitch : this.module.getEnabledSetting();
   }

   private void drawList(float dt) {
      boolean mouseInList;
      this.rowMouseIn = mouseInList = inside(this.mouseX, this.mouseY, this.listX - 8.0F, this.listY, this.listW + 16.0F, this.listH) && this.barDrag == null;
      this.rowMx = !mouseInList && this.active == null ? -10000.0F : this.mouseX;
      this.rowMy = !mouseInList && this.active == null ? -10000.0F : this.mouseY;
      this.rowTip = null;
      boolean compact = Density.compact();
      int anchor = -1;
      float anchorOffset = 0.0F;
      boolean themeSwitched = this.shownDensity != Density.get();
      boolean reflow = this.anchorWidth > 0.0F && Math.abs(this.listW - this.anchorWidth) > 0.25F;
      if (themeSwitched || reflow) {
         if (this.shownDensity != null) {
            for (int i = 0; i < this.widgets.size(); i++) {
               Widget w = this.widgets.get(i);
               if (w.appear > 0.5F && this.shows(i) && w.y + w.height() > this.listY) {
                  anchor = i;
                  anchorOffset = w.y - this.listY;
                  break;
               }
            }

            if (themeSwitched) {
               this.pageFade = Math.min(this.pageFade, 0.3F);
            }
         }

         this.shownDensity = Density.get();
      }

      this.anchorWidth = this.listW;
      this.pageFade = Easing.approach(this.pageFade, 1.0F, this.sideways ? 18.0F : 9.0F, dt);
      float fade = Easing.outCubic(this.pageFade);
      float slideX = this.sideways ? (1.0F - fade) * this.tabSlideX : 0.0F;
      float slideY = this.sideways ? 0.0F : (1.0F - fade) * 8.0F;
      int n = this.widgets.size();

      for (int ix = 0; ix < n; ix++) {
         Widget widget = this.widgets.get(ix);
         boolean visible = this.shows(ix);
         widget.appear = widget.appear < 0.0F ? (visible ? 1.0F : 0.0F) : Easing.approach(widget.appear, visible ? 1.0F : 0.0F, 14.0F, dt);
         if (ix < this.arrowTurn.length) {
            this.arrowTurn[ix] = Easing.approach(this.arrowTurn[ix], this.rowOpen[ix] ? 1.0F : 0.0F, 16.0F, dt);
         }
      }

      this.columns = compact && this.listW >= 540.0F ? 2 : 1;
      this.columnW = this.columns == 1 ? this.listW : (this.listW - 18.0F) / 2.0F;
      this.columnX[0] = this.listX + slideX;
      this.columnX[1] = this.listX + this.columnW + 18.0F + slideX;
      int split = this.columns == 2 ? this.columnSplit() : n;
      if (split >= n) {
         this.columns = 1;
         this.columnW = this.listW;
      }

      float top = this.listY - this.listBar.scroll + slideY;
      if (Float.isNaN(this.lastListTop)) {
         float var10000 = 0.0F;
      } else {
         float var27 = top - this.lastListTop;
      }

      this.lastListTop = top;
      Ui.pushScissor(this.listX - 10.0F, this.listY, this.listW + 20.0F, this.listH);
      Ui.pushAlpha(fade);
      float gap = Density.gap();
      this.rowClipX = this.listX - 10.0F;
      this.rowClipW = this.listW + 20.0F;
      this.rowClipTop = this.listY;
      float y;
      float end = y = this.layoutRows(0, this.topRows, this.listX + slideX, top, this.listW, dt);

      for (int c = 0; c < this.columns; c++) {
         this.rowClipX = this.columnX[c] - slideX - 10.0F;
         this.rowClipW = this.columnW + 20.0F;
         this.rowClipTop = this.stickyClip[c];
         Ui.pushScissor(this.rowClipX, this.rowClipTop, this.rowClipW, this.listY + this.listH - this.rowClipTop);
         end = Math.max(end, this.layoutRows(c == 0 ? this.topRows : split, c == 0 ? split : n, this.columnX[c], y, this.columnW, dt));
         Ui.popScissor();
      }

      this.rowClipTop = this.listY;

      for (int var22 = 0; var22 < this.columns; var22++) {
         this.drawSticky(var22, dt);
      }

      this.drawPulse();
      Ui.popAlpha();
      Ui.popScissor();
      y = end - slideY;
      this.listBar.update(y + this.listBar.scroll - this.listY + 10.0F, this.listH, dt);
      if (anchor >= 0) {
         float delta = this.widgets.get(anchor).y - this.listY - anchorOffset;
         this.listBar.jumpTo(Math.max(0.0F, Math.min(this.listBar.max(), this.listBar.scroll + delta)));
      }

      this.glideToReveal();
      if (this.jumpTarget != null) {
         this.listBar.target = Math.max(0.0F, Math.min(this.listBar.max(), this.jumpTarget.y - this.lastListTop));
         if (++this.jumpFrames > 40) {
            this.jumpTarget = null;
         }
      }

      if (this.listRestore >= 0.0F) {
         this.listBar.jumpTo(this.listRestore);
         if (this.listBar.max() + 0.5F >= this.listRestore || ++this.restoreFrames > 20) {
            this.listRestore = -1.0F;
         }
      }

      this.listBar.draw(this.listX + this.listW + 8.0F, this.listY, this.listH, this.active == null ? this.mouseX : -10000.0F, this.mouseY, dt, this.accent);
      if (this.frameTooltip == null) {
         this.frameTooltip = this.rowTip;
      }
   }

   private float layoutRows(int from, int to, float x0, float y, float width, float dt) {
      float gap = Density.gap();
      boolean compact = Density.compact();
      int i = from;

      while (i < to) {
         Widget widget = this.widgets.get(i);
         if (widget.appear <= 0.003F) {
            i++;
         } else {
            int depth = this.widgetDepths.get(i);
            float indent = depth * 14.0F;
            if (compact && this.packable(i)) {
               ArrayList<Integer> run = new ArrayList<>();
               int j = i;

               while (j < to) {
                  Widget wj = this.widgets.get(j);
                  if (wj.appear <= 0.003F) {
                     j++;
                  } else {
                     if (!this.packable(j) || this.widgetDepths.get(j) != depth || this.widgetHeaders.get(j) != this.widgetHeaders.get(i)) {
                        break;
                     }

                     run.add(j);
                     j++;
                  }
               }

               float avail = width - indent;
               int per = Math.max(1, Density.togglesPerRow(avail));
               float cellW = (avail - (per - 1) * 14.0F) / per;

               for (int r = 0; r < run.size(); r += per) {
                  float rowH = 0.0F;
                  float rowAppear = 0.0F;

                  for (int c = 0; c < per && r + c < run.size(); c++) {
                     Widget wc = this.widgets.get(run.get(r + c));
                     rowH = Math.max(rowH, wc.height() * wc.appear);
                     rowAppear = Math.max(rowAppear, wc.appear);
                  }

                  for (int var25 = 0; var25 < per && r + var25 < run.size(); var25++) {
                     this.drawRow(run.get(r + var25), x0 + indent + var25 * (cellW + 14.0F), y, cellW, dt);
                  }

                  y += rowH + gap * rowAppear;
               }

               i = j;
            } else {
               float h = widget.height() * widget.appear;
               this.drawRow(i, x0 + indent, y, width - indent, dt);
               y += h + gap * widget.appear;
               i++;
            }
         }
      }

      return y;
   }

   private float measureRows(int from, int to, float width) {
      float gap = Density.gap();
      float y = 0.0F;
      int i = from;

      while (i < to) {
         if (!this.shows(i)) {
            i++;
         } else {
            int depth = this.widgetDepths.get(i);
            float indent = depth * 14.0F;
            if (Density.compact() && this.packable(i)) {
               int count = 0;
               int j = i;
               float rowH = 0.0F;

               while (true) {
                  if (j < to) {
                     if (!this.shows(j)) {
                        j++;
                        continue;
                     }

                     if (this.packable(j) && this.widgetDepths.get(j) == depth && this.widgetHeaders.get(j) == this.widgetHeaders.get(i)) {
                        count++;
                        rowH = Math.max(rowH, this.widgets.get(j).height());
                        j++;
                        continue;
                     }
                  }

                  int per = Math.max(1, Density.togglesPerRow(width - indent));
                  y += (count + per - 1) / per * (rowH + gap);
                  i = j;
                  break;
               }
            } else {
               Widget widget = this.widgets.get(i);
               widget.w = width - indent;
               y += widget.height() + gap;
               i++;
            }
         }
      }

      return y;
   }

   private int columnSplit() {
      int n = this.widgets.size();
      StringBuilder sig = new StringBuilder();
      sig.append(this.page == null ? "" : this.page.key).append('|').append(Density.get()).append('|').append(Math.round(this.columnW));
      ArrayList<Integer> heads = new ArrayList<>();

      for (int i = this.topRows; i < n; i++) {
         if (this.widgets.get(i) instanceof HeaderWidget && this.shows(i)) {
            heads.add(i);
            sig.append(',').append(i);
         }
      }

      String s = sig.toString();
      if (s.equals(this.splitFor)) {
         return this.splitAt;
      } else {
         this.splitFor = s;
         if (heads.size() < 2) {
            this.splitAt = n;
            return n;
         } else {
            float[] hs = new float[heads.size()];
            float total = 0.0F;

            for (int k = 0; k < heads.size(); k++) {
               hs[k] = this.measureRows(heads.get(k), k + 1 < heads.size() ? heads.get(k + 1) : n, this.columnW);
               total += hs[k];
            }

            float acc = 0.0F;
            float best = Float.MAX_VALUE;
            int bestK = 1;

            for (int k = 1; k < heads.size(); k++) {
               float worst;
               if ((worst = Math.max(acc += hs[k - 1], total - acc)) < best) {
                  best = worst;
                  bestK = k;
               }
            }

            this.splitAt = heads.get(bestK);
            return this.splitAt;
         }
      }
   }

   private void findSticky(int split, float shift) {
      int n = this.widgets.size();

      for (int c = 0; c < 2; c++) {
         this.sticky[c] = null;
         this.stickyY[c] = this.listY;
         this.stickyClip[c] = this.listY;
      }

      for (int var15 = 0; var15 < this.columns; var15++) {
         int from = var15 == 0 ? this.topRows : split;
         int to = var15 == 0 ? split : n;
         HeaderWidget cur = null;
         int curIndex = -1;
         float nextY = Float.MAX_VALUE;

         for (int i = from; i < to; i++) {
            Widget w = this.widgets.get(i);
            if (w instanceof HeaderWidget && !(w.appear < 0.5F) && this.shows(i)) {
               float wy = w.y + shift;
               if (!(wy < this.listY - 0.5F)) {
                  if (cur != null) {
                     nextY = wy;
                  }
                  break;
               }

               cur = (HeaderWidget)w;
               curIndex = i;
            }
         }

         if (cur != null && !cur.isCollapsed()) {
            float hh = cur.height();
            float bottom = cur.y + shift + hh;

            for (int ix = curIndex + 1; ix < to && this.widgetHeaders.get(ix) == cur; ix++) {
               Widget w = this.widgets.get(ix);
               if (w.appear > 0.01F && this.shows(ix)) {
                  bottom = Math.max(bottom, w.y + shift + w.height() * w.appear);
               }
            }

            float sy;
            if (!(bottom <= this.listY + hh) && !((sy = Math.min(this.listY, Math.min(nextY, bottom) - hh)) + hh <= this.listY + 0.5F)) {
               this.sticky[var15] = cur;
               this.stickyY[var15] = sy;
               this.stickyClip[var15] = sy + hh;
            }
         }
      }
   }

   private void drawSticky(int c, float dt) {
      HeaderWidget h = this.sticky[c];
      if (h != null && c < this.columns) {
         float ox = h.x;
         float oy = h.y;
         float ow = h.w;
         h.x = this.columnX[c];
         h.w = this.columnW;
         h.y = this.stickyY[c];
         boolean free = this.rowMouseIn && this.active == null;
         Ui.pushScissor(this.columnX[c] - 10.0F, this.listY, this.columnW + 20.0F, this.stickyClip[c] - this.listY);
         h.render(free ? this.mouseX : -10000.0F, free ? this.mouseY : -10000.0F, dt, this.accent);
         Ui.popScissor();
         if (free && h.contains(this.mouseX, this.mouseY) && this.mouseY >= this.listY) {
            this.listHover = h;
            if (this.rowTip == null) {
               this.rowTip = "Click to fold this group (its title stays here while you scroll through it)";
            }
         }

         h.x = ox;
         h.y = oy;
         h.w = ow;
      }
   }

   private int columnAt(float mx) {
      return this.columns == 2 && mx >= this.columnX[1] - 9.0F ? 1 : 0;
   }

   private String chipLabel(HeaderWidget h) {
      LayoutGroup g = this.headerGroups.get(h);
      if (g == null) {
         return "Pinned";
      } else {
         switch (g.slot) {
            case MAIN:
               for (LayoutEntry e : g.entries) {
                  if (e.setting instanceof HasWidget || e.setting instanceof ModeSetting && ((ModeSetting)e.setting).getStyle() == ModeSetting.Style.CARDS) {
                     return "Style";
                  }
               }

               return "Main";
            case SIZE:
               return "Size";
            case WHEN:
               return "When";
            case OPTIONS:
               return "Options";
            default:
               return g.title;
         }
      }
   }

   private List<HeaderWidget> shownGroups() {
      ArrayList<HeaderWidget> out = new ArrayList<>();

      for (int i = 0; i < this.widgets.size(); i++) {
         if (this.widgets.get(i) instanceof HeaderWidget && this.shows(i)) {
            out.add((HeaderWidget)this.widgets.get(i));
         }
      }

      return out;
   }

   private Set<HeaderWidget> currentGroups(List<HeaderWidget> heads) {
      HashSet<HeaderWidget> out = new HashSet<>();
      if (this.jumpPick != null && heads.contains(this.jumpPick)) {
         out.add(this.jumpPick);
         return out;
      } else {
         for (int c = 0; c < this.columns; c++) {
            HeaderWidget cur = null;

            for (HeaderWidget h : heads) {
               if (!(h.appear < 0.5F) && this.columnAt(h.x + 1.0F) == c) {
                  if (cur == null || h.y <= this.listY + 2.0F) {
                     cur = h;
                  }

                  if (h.y > this.listY + 2.0F) {
                     break;
                  }
               }
            }

            if (cur != null) {
               out.add(cur);
            }
         }

         return out;
      }
   }

   private void drawJumpBar(float dt) {
      this.jumpChips.clear();
      this.jumpHeaders.clear();
      if (!this.jumpWanted) {
         this.collapseX = -10000.0F;
      } else {
         float y = this.jumpY;
         float h = this.jumpH;
         float x = this.listX;
         float right = this.panelX + this.panelW - 18.0F;
         boolean plain = this.tabsShown;
         float ts = plain ? (Density.dense() ? 6.6F : (Density.compact() ? 7.0F : 7.6F)) : (Density.dense() ? 6.8F : (Density.compact() ? 7.4F : 8.2F));
         float pad = Density.dense() ? 6.0F : (Density.compact() ? 7.0F : 9.0F);
         float gap = 4.0F;
         boolean free = this.barDrag == null && this.active == null;
         List<HeaderWidget> heads = this.shownGroups();
         boolean anyOpen = false;

         for (HeaderWidget hw : heads) {
            if (!hw.isCollapsed()) {
               anyOpen = true;
            }
         }

         String ca = anyOpen ? "Collapse all" : "Expand all";
         this.collapseW = Ui.width(ca, ts) + pad * 2.0F + 8.0F;
         this.collapseX = right - this.collapseW;
         boolean overC = free && inside(this.mouseX, this.mouseY, this.collapseX, y, this.collapseW, h);
         this.collapseHover = Easing.approach(this.collapseHover, overC ? 1.0F : 0.0F, 16.0F, dt);
         Ui.registerControl(this.collapseX, y, this.collapseW, h, ca);
         if (!plain || this.collapseHover > 0.01F) {
            Ui.surface(
               this.collapseX,
               y,
               this.collapseW,
               h,
               h / 2.0F,
               ColorUtil.lerp(plain ? ColorUtil.withAlpha(Theme.SURFACE, 0.0F) : Theme.SURFACE, Theme.SURFACE_HOVER, this.collapseHover)
            );
         }

         int cc = ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.collapseHover);
         MenuIcons.chevron(
            this.collapseX + pad + 1.5F,
            y + h / 2.0F,
            2.2F,
            anyOpen ? 1.0F : 0.0F,
            ColorUtil.lerp(Theme.TEXT_MUTED, this.accent, 0.4F + 0.6F * this.collapseHover)
         );
         Ui.textMiddle(ca, this.collapseX + pad + 8.0F, y + h / 2.0F, ts, cc, false);
         if (overC) {
            String avail = this.frameTooltip = anyOpen ? "Fold every group of this page" : "Unfold every group of this page";
         }

         if (!heads.isEmpty()) {
            float avail = this.collapseX - 10.0F - x;
            String[] labels = new String[heads.size()];
            float total = 0.0F;

            for (int k = 0; k < labels.length; k++) {
               labels[k] = this.chipLabel(heads.get(k));
               total += Ui.width(labels[k], ts) + pad * 2.0F + gap;
            }

            if (total - gap > avail) {
               pad = 4.0F;
               total = 0.0F;

               for (String l : labels) {
                  total += Ui.width(l, ts) + pad * 2.0F + gap;
               }
            }

            float maxChip = total - gap > avail ? Math.max(18.0F, (avail + gap) / labels.length - gap) : Float.MAX_VALUE;
            float[] widths = new float[labels.length];
            float sum = 0.0F;

            for (int k = 0; k < labels.length; k++) {
               widths[k] = Math.min(maxChip, Ui.width(labels[k], ts) + pad * 2.0F);
               sum += widths[k] + gap;
            }

            int fit = labels.length;
            this.jumpMoreX = -10000.0F;
            this.jumpOverflow.clear();
            if (sum - gap > this.collapseX - 6.0F - x) {
               float moreW = h + 6.0F;
               float room = this.collapseX - 6.0F - moreW - gap - x;
               float used = 0.0F;

               for (fit = 0; fit < labels.length && used + widths[fit] <= room; fit++) {
                  used += widths[fit] + gap;
               }

               this.jumpMoreX = x + used;
               this.jumpMoreW = moreW;

               for (int k = fit; k < labels.length; k++) {
                  this.jumpOverflow.add(heads.get(k));
               }
            }

            Set<HeaderWidget> lit = this.currentGroups(heads);
            float cx = x;

            for (int k = 0; k < fit; k++) {
               HeaderWidget hwx = heads.get(k);
               Object label = labels[k];
               float cw = Ui.width((String)label, ts) + pad * 2.0F;
               if (cw > maxChip) {
                  label = Ui.ellipsize((String)label, ts, maxChip - pad * 2.0F, false);
                  cw = maxChip;
               }

               Ui.registerControl(cx, y, cw, h, "jump " + labels[k]);
               boolean over = free && inside(this.mouseX, this.mouseY, cx, y, cw, h);
               Float hvBox = this.chipHover.get(hwx);
               float hv = Easing.approach(hvBox == null ? 0.0F : hvBox, over ? 1.0F : 0.0F, 16.0F, dt);
               this.chipHover.put(hwx, hv);
               boolean on = lit.contains(hwx);
               if (plain) {
                  if (on) {
                     Ui.rect(cx + pad * 0.6F, y + h - 1.4F, cw - pad * 1.2F, 1.2F, this.accent);
                  } else if (hv > 0.01F) {
                     Ui.rect(cx + pad * 0.6F, y + h - 1.2F, cw - pad * 1.2F, 0.8F, ColorUtil.withAlpha(Theme.TEXT_DIM, hv));
                  }
               } else if (on) {
                  Ui.roundRect(cx, y, cw, h, h / 2.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, hv));
                  Ui.accentOutline(cx, y, cw, h, h / 2.0F, 1.0F, this.accent);
               } else {
                  Ui.surface(cx, y, cw, h, h / 2.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hv));
               }

               int tc = on
                  ? Theme.TEXT
                  : (hwx.isCollapsed() ? ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT, hv) : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, hv));
               Ui.textMiddle((String)label, cx + (cw - Ui.width((String)label, ts)) / 2.0F, y + h / 2.0F, ts, tc, false);
               this.jumpChips.add(new float[]{cx, y, cw, h});
               this.jumpHeaders.add(hwx);
               if (over) {
                  LayoutGroup g = this.headerGroups.get(hwx);
                  this.frameTooltip = "Go to "
                     + (g == null ? "Pinned" : g.title)
                     + (hwx.isCollapsed() ? " (it unfolds)" : "")
                     + (((String)label).equals(labels[k]) ? "" : "  (" + labels[k] + ")");
               }

               cx += cw + gap;
            }

            if (this.jumpMoreX > -5000.0F) {
               boolean overx = free && inside(this.mouseX, this.mouseY, this.jumpMoreX, y, this.jumpMoreW, h);
               this.jumpMoreHover = Easing.approach(this.jumpMoreHover, overx ? 1.0F : 0.0F, 16.0F, dt);
               boolean anyLit = false;

               for (HeaderWidget hwxx : this.jumpOverflow) {
                  anyLit |= lit.contains(hwxx);
               }

               Ui.registerControl(this.jumpMoreX, y, this.jumpMoreW, h, "jump ⋯");
               if (plain) {
                  if (anyLit) {
                     Ui.rect(this.jumpMoreX + 3.0F, y + h - 1.4F, this.jumpMoreW - 6.0F, 1.2F, this.accent);
                  }
               } else if (anyLit) {
                  Ui.roundRect(this.jumpMoreX, y, this.jumpMoreW, h, h / 2.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, this.jumpMoreHover));
                  Ui.accentOutline(this.jumpMoreX, y, this.jumpMoreW, h, h / 2.0F, 1.0F, this.accent);
               } else {
                  Ui.surface(this.jumpMoreX, y, this.jumpMoreW, h, h / 2.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.jumpMoreHover));
               }

               int mc = ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.jumpMoreHover);

               for (int i = -1; i <= 1; i++) {
                  Ui.circle(this.jumpMoreX + this.jumpMoreW / 2.0F + i * 3.4F, y + h / 2.0F, 1.1F, mc);
               }

               if (overx) {
                  this.frameTooltip = this.jumpOverflow.size() + " more " + (this.jumpOverflow.size() == 1 ? "group" : "groups");
               }
            }
         }
      }
   }

   private void jumpTo(HeaderWidget hw) {
      boolean unfold = hw.isCollapsed();
      if (unfold) {
         hw.setCollapsed(false);
      }

      this.jumpPick = hw;
      this.jumpTarget = hw;
      this.jumpFrames = 0;
      ModSounds.gui(unfold ? Sfx.DROPDOWN : Sfx.CLICK);
   }

   private boolean jumpBarPressed(float mx, float my) {
      if (this.jumpWanted && !(my < this.jumpY) && !(my >= this.jumpY + this.jumpH)) {
         if (!inside(mx, my, this.collapseX, this.jumpY, this.collapseW, this.jumpH)) {
            for (int k = 0; k < this.jumpChips.size(); k++) {
               float[] c = this.jumpChips.get(k);
               if (inside(mx, my, c[0], c[1], c[2], c[3])) {
                  this.jumpTo(this.jumpHeaders.get(k));
                  return true;
               }
            }

            if (this.jumpMoreX > -5000.0F && inside(mx, my, this.jumpMoreX, this.jumpY, this.jumpMoreW, this.jumpH)) {
               ContextMenu menu = new ContextMenu(null);

               for (final HeaderWidget hw : new ArrayList<>(this.jumpOverflow)) {
                  LayoutGroup g = this.headerGroups.get(hw);
                  menu.add(g == null ? "Pinned" : g.title, hw.isCollapsed() ? "folded" : null, true, new Runnable() {
                     @Override
                     public void run() {
                        FuriahScreen.this.jumpTo(hw);
                     }
                  });
               }

               ModSounds.gui(Sfx.DROPDOWN);
               Popovers.open(menu, this.jumpMoreX, this.jumpY, this.jumpMoreW, this.jumpH);
               return true;
            } else {
               return true;
            }
         } else {
            List<HeaderWidget> heads = this.shownGroups();
            boolean anyOpen = false;

            for (HeaderWidget hw : heads) {
               if (!hw.isCollapsed()) {
                  anyOpen = true;
               }
            }

            for (HeaderWidget hwx : heads) {
               hwx.setCollapsed(anyOpen);
            }

            ModSounds.gui(anyOpen ? Sfx.CLICK : Sfx.DROPDOWN);
            if (anyOpen) {
               this.listBar.target = 0.0F;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private int[] tabCounts(List<LayoutPage> tabs) {
      if (!this.searching()) {
         return null;
      } else {
         int[] counts = new int[tabs.size()];
         Set seen = Collections.newSetFromMap(new IdentityHashMap());

         for (SearchResult r : this.sectionSearch.results()) {
            Setting s = r.entry.row;
            LayoutEntry e;
            LayoutPage tp;
            int i;
            if (s != null
               && seen.add(s)
               && (i = (tp = (e = SectionLayouts.entryOf(s)) == null ? null : e.page().tabPage()) == null ? -1 : tabs.indexOf(tp)) >= 0) {
               counts[i]++;
            }
         }

         return counts;
      }
   }

   private void drawTabs(float dt) {
      if (this.tabsShown) {
         List<LayoutPage> tabs = this.layout.tabPages();
         int[] counts = this.tabCounts(tabs);
         boolean free = this.barDrag == null && this.active == null;
         this.tabRow
            .draw(this.page == null ? null : this.page.tabPage(), counts, tabs, this.module.isEnabled(), this.mouseX, this.mouseY, free, dt, this.accent);
         String tip;
         if (free && this.tabRow.contains(this.mouseX, this.mouseY) && (tip = this.tabRow.tooltip(this.mouseX, this.mouseY, counts, tabs)) != null) {
            this.frameTooltip = tip;
         }
      }
   }

   private void tabPressed(float mx, float my, int button) {
      if (this.tabRow.onMore(mx, my)) {
         if (button == 0) {
            ContextMenu menu = new ContextMenu(null);

            for (final LayoutPage p : new ArrayList<>(this.tabRow.overflow)) {
               BooleanSetting sw = p.tab.getSwitch();
               menu.add(p.tab.getName(), sw == null ? null : (sw.get() ? "on" : "off"), true, new Runnable() {
                  @Override
                  public void run() {
                     if (FuriahScreen.this.searching()) {
                        FuriahScreen.this.sectionSearch.revealTab(p.tab.getName());
                     } else {
                        FuriahScreen.this.openTab(p);
                     }
                  }
               });
            }

            ModSounds.gui(Sfx.DROPDOWN);
            Popovers.open(menu, this.tabRow.moreX, this.tabRow.y, this.tabRow.moreW, this.tabRow.h);
         }
      } else {
         TabRow.Box b = this.tabRow.at(mx, my);
         if (b != null) {
            if (button == 1) {
               this.openTabMenu(b.page, mx, my);
            } else if (button == 0) {
               BooleanSetting sw = b.page.tab.getSwitch();
               if (sw != null && this.tabRow.onDot(b, mx, my)) {
                  sw.set(!sw.get());
                  ModSounds.toggle(sw.get());
               } else if (this.searching()) {
                  if (this.sectionSearch.revealTab(b.page.tab.getName())) {
                     ModSounds.gui(Sfx.CLICK);
                  } else {
                     ModSounds.gui(Sfx.ERROR);
                  }
               } else {
                  this.openTab(b.page);
               }
            }
         }
      }
   }

   private void openTabMenu(final LayoutPage tab, final float mx, float my) {
      final MiniSection t = tab.tab;
      final BooleanSetting sw = t.getSwitch();
      ContextMenu menu = new ContextMenu(t.getName());
      if (sw != null) {
         boolean locked = false;
         menu.add(sw.get() && !locked ? "Turn off" : "Turn on", null, !locked, new Runnable() {
            @Override
            public void run() {
               sw.set(!sw.get());
               ModSounds.toggle(sw.get());
            }
         }, locked ? "" : null);
      }

      final float ay = my;
      menu.add("Reset this tab…", null, true, new Runnable() {
         @Override
         public void run() {
            ContextMenu confirm = new ContextMenu("Reset every setting of " + t.getName() + "?");
            confirm.add("Reset " + t.getName(), null, true, new Runnable() {
               @Override
               public void run() {
                  FuriahScreen.this.resetTab(tab);
                  ModSounds.gui(Sfx.CLICK);
               }
            });
            confirm.add("Cancel", null, true, new Runnable() {
               @Override
               public void run() {
                  ModSounds.gui(Sfx.CLICK);
               }
            });
            ModSounds.gui(Sfx.CONFIRM);
            Popovers.open(confirm, mx, ay, 0.0F, 0.0F);
         }
      });
      ModSounds.gui(Sfx.DROPDOWN);
      Popovers.open(menu, mx, my, 0.0F, 0.0F);
   }

   private void resetTab(LayoutPage tab) {
      for (Setting s : this.module.getSettings()) {
         if (s.getOwner() == tab.tab && s != tab.tab.getSwitch() && s.isSaved() && !s.isPersonal()) {
            s.reset();
         }
      }

      this.module.restartPreview();
   }

   private static boolean hasContextMenu(Widget w) {
      return w instanceof ToggleWidget
         || w instanceof SliderWidget
         || w instanceof SegmentedWidget
         || w instanceof ChipsWidget
         || w instanceof ColorWidget
         || w instanceof TextFieldWidget
         || w instanceof CardsWidget;
   }

   private void openContextMenu(final Setting s, Widget widget, float mx, float my) {
      boolean changed = SettingDefaults.known(s) && !SettingDefaults.isDefault(s);
      boolean canPaste = MenuClipboard.canPaste(s);
      ContextMenu menu = new ContextMenu(s.getLabel());
      menu.add(this.isPinned(s) ? "Unpin" : "Pin to the top", null, true, new Runnable() {
         @Override
         public void run() {
            ModSounds.gui(Sfx.CLICK);
            FuriahScreen.this.togglePin(s);
         }
      });
      menu.add("Reset", null, changed, new Runnable() {
         @Override
         public void run() {
            s.reset();
            ModSounds.gui(Sfx.CLICK);
         }
      });
      menu.add("Copy value", null, true, new Runnable() {
         @Override
         public void run() {
            MenuClipboard.copy(s);
            ModSounds.gui(Sfx.CLICK);
         }
      });
      String from = MenuClipboard.describe();
      menu.add("Paste value", canPaste && from != null ? Ui.ellipsize(from, 7.4F, 90.0F, false) : null, canPaste, new Runnable() {
         @Override
         public void run() {
            MenuClipboard.paste(s);
            ModSounds.gui(Sfx.CLICK);
         }
      });
      ModSounds.gui(Sfx.DROPDOWN);
      Popovers.open(menu, mx, my, 0.0F, 0.0F);
   }

   private boolean packable(int i) {
      return !(this.widgets.get(i) instanceof ToggleWidget) ? false : i >= this.rowOpen.length || !this.rowOpen[i] || !this.hasShowableOptions(i);
   }

   private static float lineH(Widget w) {
      return Math.min(w.height(), Density.compact() ? Density.row() : 26.0F);
   }

   private void drawRow(int i, float x, float y, float w, float dt) {
      Widget widget = this.widgets.get(i);
      boolean visible = this.shows(i);
      float h = widget.height() * widget.appear;
      int depth = this.widgetDepths.get(i);
      boolean arrow = Density.compact() && i < this.rowChildren.length && this.rowChildren[i].length > 0 && this.hasShowableOptions(i);
      widget.x = x + this.slideOf(i, widget, visible);
      widget.y = y;
      widget.w = w - (arrow ? 12.0F : 0.0F);
      if (!(y + h <= this.listY - 2.0F) && !(y >= this.listY + this.listH + 2.0F)) {
         if (depth > 0) {
            Ui.rect(x - 14.0F + 4.0F, y + 2.0F, 0.8F, Math.max(0.0F, h - 4.0F), ColorUtil.withAlpha(Theme.ACCENT_LINE, widget.appear));
         }

         String unavailable = this.unavailable(widget);
         boolean disabled = this.blocked() && !(widget instanceof NoticeWidget) && !(widget instanceof HeaderWidget) || unavailable != null;
         boolean underTitle = this.mouseY < this.rowClipTop && widget != this.active;
         boolean mouseHere = this.rowMouseIn && !underTitle;
         boolean over = mouseHere && inside(this.mouseX, this.mouseY, x - 11.0F, y, w + 11.0F, h);
         if (unavailable != null && this.rowTip == null && visible && this.active == null && over) {
            this.rowTip = unavailable;
         }

         Ui.pushScissor(this.rowClipX, y, this.rowClipW, h);
         if (widget == this.active && this.dragFade > 0.001F) {
            Ui.pushAlphaAbsolute(this.openAlpha * widget.appear * widget.appear);
         } else {
            Ui.pushAlpha(widget.appear * widget.appear * (disabled ? 0.4F : 1.0F));
         }

         widget.render(!disabled && !underTitle ? this.rowMx : -10000.0F, !disabled && !underTitle ? this.rowMy : -10000.0F, dt, this.accent);
         float cy = y + lineH(widget) / 2.0F;
         if (arrow) {
            boolean overArrow = !disabled && over && this.mouseX >= widget.x + widget.w;
            optionsArrow(
               widget.x + widget.w + 6.0F + 1.0F,
               cy,
               this.arrowTurn[i],
               ColorUtil.lerp(Theme.TEXT_MUTED, this.accent, overArrow ? 1.0F : 0.25F + 0.5F * this.arrowTurn[i])
            );
            if (overArrow && this.rowTip == null) {
               String bl2 = this.rowTip = this.rowOpen[i] ? "Hide its options" : "Show its options";
            }
         }

         Setting s;
         if ((s = this.settingOf.get(widget)) != null && s.isSaved() && SettingDefaults.known(s) && !SettingDefaults.isDefault(s)) {
            boolean overReset = !disabled && over && this.mouseX < widget.x - 1.0F && Math.abs(this.mouseY - cy) < 8.0F;
            if (over && !disabled) {
               ColorWidget.resetGlyph(widget.x - 6.0F, cy, 3.0F, ColorUtil.lerp(Theme.TEXT_MUTED, this.accent, overReset ? 1.0F : 0.55F));
               if (overReset && this.rowTip == null) {
                  this.rowTip = "Back to the default";
               }
            } else {
               Ui.circle(widget.x - 6.0F, cy, 1.5F, this.accent | 0xFF000000);
            }
         }

         Ui.popAlpha();
         Ui.popScissor();
         if (this.rowTip == null && visible && this.active == null && over && !disabled) {
            this.rowTip = widget.tooltip(this.mouseX, this.mouseY);
         }

         if (visible
            && !disabled
            && mouseHere
            && !(widget instanceof NoticeWidget)
            && !(widget instanceof NoteWidget)
            && inside(this.mouseX, this.mouseY, widget.x, y, widget.w + (arrow ? 12.0F : 0.0F), h)) {
            this.listHover = widget;
         }
      }
   }

   private static void optionsArrow(float cx, float cy, float t, int color) {
      double angle = t * Math.PI / 2.0;
      double dx = Math.cos(angle);
      double dy = Math.sin(angle);

      for (int k = 0; k <= 5; k++) {
         float f = k / 5.0F;
         float along = 1.6F - f * 3.2F * 0.7071F;
         float side = f * 3.2F * 0.7071F;

         for (int sgn = -1; sgn <= 1; sgn += 2) {
            Ui.circle((float)(cx + dx * along - dy * side * sgn), (float)(cy + dy * along + dx * side * sgn), 0.65F, color);
         }
      }
   }

   private boolean searching() {
      return this.modulePage() && this.sectionSearch != null && this.sectionSearch.isActive();
   }

   private void updateSearch() {
      String key = this.pageKey();
      if (!key.equals(this.searchFor)) {
         if (this.searchFor != null) {
            this.searchTexts.put(this.searchFor, this.searchBar.text());
         }

         this.searchFor = key;
         String text = this.searchTexts.get(key);
         this.searchBar.setText(text == null ? "" : text);
      }

      String text;
      boolean typed = !(text = this.searchBar.text()).trim().isEmpty();
      if (this.modulePage()) {
         if (typed || this.sectionSearch != null) {
            SearchIndex index = SearchIndexes.of(this.module);
            if (this.sectionSearch == null) {
               this.sectionSearch = new SectionSearch(this.searchHost, this.module.getId(), this.module.getName(), index, this.widgetOf);
            }

            boolean was = this.sectionSearch.isActive();
            this.sectionSearch.update(text, index);
            if (was && !this.sectionSearch.isActive()) {
               this.sectionSearch.clearFilters();
            }

            if (this.highlightNext != null) {
               this.sectionSearch.highlight(this.highlightNext);
            }
         }
      } else if (this.settingsPage) {
         if (typed || this.settingsSearch != null) {
            if (this.settingsSearch == null) {
               this.settingsSearch = new SectionSearch(this.searchHost, "#settings", "Settings", settingsIndex(), null);
            }

            this.settingsSearch.update(text, settingsIndex());
            if (this.highlightNext != null && this.highlightNext.key instanceof String) {
               this.settingsPulse((String)this.highlightNext.key);
            }
         }
      } else if (this.presetsPage && this.presetsUi != null) {
         this.presetsUi.filter(text);
      }

      this.highlightNext = null;
      this.globalSearch.search(this.globalBar.text());
      if (this.globalBar.isFocused() && !this.globalPage && !this.globalBar.text().trim().isEmpty()) {
         this.openGlobal();
      }
   }

   private void drawSearchBar(float dt) {
      boolean free = this.barDrag == null && (this.active == null || this.searchPressed || this.globalPressed);
      if (this.globalX > -5000.0F) {
         boolean inSidebar = this.sideMode == MenuPrefs.SidebarStyle.FULL;
         String count = this.globalPage && this.globalSearch.searching() && !inSidebar ? results(this.globalSearch.resultCount()) : null;
         this.globalBar
            .render(
               this.globalX,
               this.globalY,
               this.globalW,
               this.globalH,
               free ? this.mouseX : -10000.0F,
               free ? this.mouseY : -10000.0F,
               dt,
               this.accent,
               inSidebar ? "Search everything…" : "Search every section…",
               count
            );
         if (free && !this.globalBar.isFocused() && this.globalBar.contains(this.mouseX, this.mouseY)) {
            this.frameTooltip = "Search every section and Settings at once (Ctrl+Shift+F)";
         }
      }

      if (!this.globalPage) {
         String name = this.settingsPage ? "Settings" : (this.presetsPage ? "Presets" : this.module.getName());
         String count = null;
         if (this.searching()) {
            count = results(this.sectionSearch.resultCount());
         } else if (this.settingsPage && this.settingsSearch != null && this.settingsSearch.isActive()) {
            count = results(this.settingsSearch.resultCount());
         } else if (this.presetsPage && this.presetsUi != null && this.presetsUi.isFiltering()) {
            count = results(this.presetsUi.matchCount());
         }

         this.searchBar
            .render(
               this.searchX,
               this.searchY,
               this.searchW,
               this.searchH,
               free ? this.mouseX : -10000.0F,
               free ? this.mouseY : -10000.0F,
               dt,
               this.accent,
               "Search " + name + "…",
               count
            );
         if (free && !this.searchBar.isFocused() && this.searchBar.contains(this.mouseX, this.mouseY)) {
            this.frameTooltip = "Search this page (Ctrl+F, or just start typing)";
         }
      }
   }

   private static String results(int n) {
      return n + (n == 1 ? " result" : " results");
   }

   private void drawSearchList(float dt) {
      this.sectionSearch.drawPage(this.listX, this.listY, this.listW, this.listH, this.mouseX, this.mouseY, this.barDrag == null, this.active, dt, this.accent);
      if (this.frameTooltip == null && this.active == null && inside(this.mouseX, this.mouseY, this.listX - 8.0F, this.listY, this.listW + 16.0F, this.listH)) {
         this.frameTooltip = this.sectionSearch.tooltip(this.mouseX, this.mouseY);
      }
   }

   private void showInSection(SearchResult result, Widget widget) {
      SearchEntry e = result.entry;
      this.searchBar.setText("");
      this.searchBar.unfocus();
      if (this.sectionSearch != null) {
         this.sectionSearch.update("", SearchIndexes.of(this.module));
         this.sectionSearch.clearFilters();
      }

      if (e.owner != null && !e.owner.isShown()) {
         e.owner.show();
      }

      LayoutPage on;
      if ((on = this.pageOf(widget)) != null && on != this.page) {
         this.showPage(on, false);
      }

      int i;
      HeaderWidget header = (i = this.widgets.indexOf(widget)) >= 0 ? this.widgetHeaders.get(i) : null;
      if (header != null && header.isCollapsed()) {
         header.setCollapsed(false);
      }

      if (i >= 0 && !this.shows(i)) {
         int k = this.widgetParents.get(i);

         while (k >= 0 && !this.shows(k)) {
            k = this.widgetParents.get(k);
         }

         Widget before;
         if (k >= 0) {
            widget = this.widgets.get(k);
         } else {
            for (int var11 = i - 1;
               var11 >= 0 && !((before = this.widgets.get(var11)) instanceof HeaderWidget) && this.widgetHeaders.get(var11) == header;
               var11--
            ) {
               if (this.shows(var11)) {
                  widget = before;
                  break;
               }
            }
         }
      }

      float offset = 0.0F;
      float height = -1.0F;
      float at;
      if (widget instanceof SearchList && e.kind != SearchEntry.Kind.SETTING && e.key != null && (at = ((SearchList)widget).reveal(e.key)) >= 0.0F) {
         offset = at;
         height = ((SearchList)widget).revealHeight(e.key);
      }

      this.cancelRestore();
      this.revealWidget = widget;
      this.revealOffset = offset;
      this.revealHeight = height;
      this.revealFrames = 0;
      this.revealStart = ClientClock.nanos();
      this.pulseWidget = widget;
      this.pulseOffset = offset;
      this.pulseHeight = height;
      this.pulseStart = ClientClock.nanos();
   }

   private void openFromSearch(String page, String query, SearchEntry entry) {
      this.searchTexts.put(page, query);
      if ("#settings".equals(page)) {
         this.openSettings(false);
      } else if ("#presets".equals(page)) {
         this.openPresets();
      } else {
         Module m = this.modules.get(page);
         if (m == null) {
            return;
         }

         this.showSection(m);
      }

      this.searchBar.unfocus();
      this.highlightNext = entry;
   }

   private void glideToReveal() {
      if (this.revealWidget != null) {
         float h = this.revealHeight > 0.0F ? this.revealHeight : this.revealWidget.height();
         float top = this.revealWidget.y + this.revealOffset - this.listY + this.listBar.scroll;
         this.listBar.target = Math.max(0.0F, Math.min(this.listBar.max(), top - Math.max(12.0F, (this.listH - h) * 0.3F)));
         if (++this.revealFrames > 45 && ClientClock.nanos() - this.revealStart > 800000000L) {
            this.revealWidget = null;
         }
      }
   }

   private void drawPulse() {
      if (this.pulseWidget != null) {
         float t = (float)(ClientClock.nanos() - this.pulseStart) / 1.0E9F;
         if (t > 1.6F) {
            this.pulseWidget = null;
         } else if (this.pulseWidget.appear > 0.05F) {
            float h = this.pulseHeight > 0.0F ? this.pulseHeight : this.pulseWidget.height();
            float a = (0.6F + 0.4F * (float)Math.sin(t * 8.0)) * (1.0F - Easing.smoothstep(1.0F, 1.6F, t));
            float px = this.pulseWidget.x - 6.0F;
            float py = this.pulseWidget.y + this.pulseOffset - 3.0F;
            float pw = this.pulseWidget.w + 12.0F;
            float ph = h + 6.0F;
            Ui.roundRect(px, py, pw, ph, 8.0F, ColorUtil.withAlpha(Theme.ACCENT_FAINT, a));
            Ui.outline(px, py, pw, ph, 8.0F, 1.3F, ColorUtil.withAlpha(this.accent, a));
         }
      }
   }

   private static SearchIndex settingsIndex() {
      if (settingsIndexCache == null) {
         SearchIndex idx = new SearchIndex("#settings", "Settings");
         ColorSetting.Mode[] modes = ColorSetting.Mode.values();
         String[] modeNames = new String[modes.length];

         for (int i = 0; i < modes.length; i++) {
            modeNames[i] = modes[i].displayName();
         }

         idx.add(
            SearchEntry.Kind.BLOCK,
            null,
            "global",
            "Global Color",
            "Global Color",
            "Every color set to Global follows this one, live, in every section.",
            null,
            modeNames,
            new String[]{"accent", "main color"}
         );
         idx.add(
            SearchEntry.Kind.BLOCK,
            null,
            "setall",
            "Set all effects to Global",
            "Global Color",
            "Switches every effect color in every section to Global.",
            null,
            null,
            new String[]{"reset colors"}
         );
         idx.add(
            SearchEntry.Kind.BLOCK,
            null,
            "key",
            "Menu Key",
            "Menu Key",
            "Open this menu: click the key, then press the new one.",
            null,
            null,
            new String[]{"keybind", "hotkey", "shortcut", "open menu"}
         );
         idx.add(
            SearchEntry.Kind.BLOCK,
            null,
            "viewTheme",
            MenuPrefs.VIEW_THEME.getLabel(),
            "Interface",
            MenuPrefs.VIEW_THEME.getDescription(),
            null,
            new String[]{"Comfortable", "Compact", "Dense"},
            MenuPrefs.VIEW_THEME.getKeywords()
         );
         idx.add(
            SearchEntry.Kind.BLOCK,
            null,
            "sidebar",
            MenuPrefs.SIDEBAR.getLabel(),
            "Interface",
            MenuPrefs.SIDEBAR.getDescription(),
            null,
            new String[]{"Full", "Icons", "Hidden"},
            MenuPrefs.SIDEBAR.getKeywords()
         );
         idx.add(
            SearchEntry.Kind.BLOCK,
            null,
            "guiScale",
            MenuPrefs.GUI_SCALE.getLabel(),
            "Interface",
            MenuPrefs.GUI_SCALE.getDescription(),
            null,
            new String[]{"Auto", "75%", "90%", "100%", "110%", "125%", "150%", "175%", "200%"},
            MenuPrefs.GUI_SCALE.getKeywords()
         );
         idx.add(
            SearchEntry.Kind.BLOCK,
            null,
            "panel",
            "Panel Size",
            "Interface",
            "Full screen, or drag the menu's bottom-right corner to resize it.",
            null,
            null,
            new String[]{"full screen", "fullscreen", "resize", "panel", "window", "maximize", "menu size"}
         );
         idx.add(
            SearchEntry.Kind.BLOCK,
            null,
            "guide",
            "Welcome Guide",
            "Interface",
            "Four short steps: the look, the size, your effects, where things are.",
            null,
            null,
            new String[]{"welcome", "tutorial", "help", "getting started", "tour", "first time"}
         );
         BooleanSetting flash = FuriahsVisuals.instance.getReduceFlashing();
         idx.add(
            SearchEntry.Kind.BLOCK,
            null,
            "flash",
            flash.getName(),
            "Accessibility",
            flash.getDescription(),
            null,
            null,
            new String[]{"epilepsy", "photosensitive", "flashes", "screen pulse"}
         );

         for (Setting setting : FuriahsVisuals.instance.getSounds().all()) {
            if (setting.isSaved()) {
               idx.add(
                  SearchEntry.Kind.BLOCK,
                  null,
                  SoundsBlock.key(setting),
                  setting.getName(),
                  "Sounds",
                  setting.getDescription(),
                  null,
                  null,
                  setting.getKeywords()
               );
            }
         }

         for (Theme.Style style : Theme.Style.values()) {
            idx.add(
               SearchEntry.Kind.BLOCK,
               null,
               "theme:" + style.name(),
               style.displayName(),
               "Theme",
               style.description(),
               null,
               null,
               new String[]{"look", "appearance"}
            );
         }

         settingsIndexCache = idx;
      }

      return settingsIndexCache;
   }

   private Map<String, SearchResult> settingsMatches() {
      if (this.settingsPage && this.settingsSearch != null && this.settingsSearch.isActive()) {
         HashMap<String, SearchResult> m = new HashMap<>();

         for (SearchResult r : this.settingsSearch.results()) {
            if (r.entry.key instanceof String && !m.containsKey(r.entry.key)) {
               m.put((String)r.entry.key, r);
            }
         }

         return m;
      } else {
         return null;
      }
   }

   private void settingsPulse(String block) {
      this.settingsReveal = block;
      this.settingsRevealStart = ClientClock.nanos();
      this.settingsRevealY = -1.0F;
   }

   private void drawInfo() {
      if (!this.module.hasPreview()) {
         String info = this.module.getPreviewInfo();
         if (info != null) {
            float w = Ui.width(info, 7.4F);
            Ui.text(info, this.panelX + this.panelW - 18.0F - w, this.panelY + this.panelH - 13.0F, 7.4F, Theme.TEXT_MUTED);
         }
      }
   }

   private void drawPreview(float dt) {
      if (this.module.hasPreview()) {
         float bx = this.previewX;
         float by = this.previewY;
         float bw = this.previewW;
         float bh = this.previewH;
         float r = Theme.GLASS ? 14.0F : 11.0F;
         boolean glassStage = Theme.GLASS
            && GlassRenderer.drawGlass(
               bx,
               by,
               bw,
               bh,
               r,
               ColorUtil.withAlpha(ColorUtil.lerp(-16447733, Theme.GLASS_TINT | 0xFF000000, 0.35F), 0.68F),
               ColorUtil.withAlpha(Theme.GLASS_RIM, 0.6F),
               12.0F,
               5.0F,
               Ui.alpha()
            );
         if (!glassStage) {
            Ui.roundRect(bx, by, bw, bh, r, Theme.PREVIEW_STAGE);
         }

         Ui.gradientV(bx, by, bw, bh, r, ColorUtil.withAlpha(this.accent, 0.08F), 0);
         if (!glassStage) {
            Ui.outline(bx, by, bw, bh, r, 0.8F, Theme.DIVIDER);
         }

         Ui.pushScissor(bx + 1.0F, by + 1.0F, bw - 2.0F, bh - 2.0F);
         this.preview.render(this.module, bx + 1.0F, by + 1.0F, bw - 2.0F, bh - 2.0F, dt);
         Ui.popScissor();
         setupState();
         float testsLeft = this.drawPreviewTests(bx, by, bw, dt);
         String info = this.module.getPreviewInfo();
         if (info != null && bx + 10.0F + Ui.width(info, 7.6F) < testsLeft - 6.0F) {
            Ui.text(info, bx + 10.0F, by + 9.0F, 7.6F, -6050627);
         }

         String label = this.module.getPreviewLabel();
         Ui.gradientV(bx, by + bh - 50.0F, bw, 50.0F, r, 0, 1711276032);
         Ui.textBold(Ui.ellipsize(label, 12.0F, bw - 24.0F, true), bx + 12.0F, by + bh - 38.0F, 12.0F, -920584);
         Ui.text("Drag to rotate · click to replay", bx + 12.0F, by + bh - 20.0F, 7.8F, -6050627);
      }
   }

   private float drawPreviewTests(float bx, float by, float bw, float dt) {
      String[] tests = this.module.getPreviewTests();
      this.previewTestCount = 0;
      float right = bx + bw - 8.0F;
      if (tests == null) {
         return right;
      } else {
         float h = 16.0F;
         float size = 7.6F;
         float x = right;

         for (int i = Math.min(tests.length, this.previewTests.length) - 1; i >= 0; i--) {
            float w = Ui.width(tests[i], size) + 16.0F;
            float y = by + 7.0F;
            float var16;
            boolean over = inside(this.mouseX, this.mouseY, var16 = x - w, y, w, h);
            this.previewTestHover[i] = Easing.approach(this.previewTestHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
            float e = this.previewTestHover[i];
            Ui.registerControl(var16, y, w, h, "preview test " + tests[i]);
            Ui.roundRect(var16, y, w, h, h / 2.0F, ColorUtil.lerp(-1945498606, Theme.ACCENT_SOFT_HOVER, e));
            if (e > 0.5F) {
               Ui.accentOutline(var16, y, w, h, h / 2.0F, 1.0F, this.accent);
            } else {
               Ui.outline(var16, y, w, h, h / 2.0F, 0.8F, 872415231);
            }

            Ui.textMiddle(tests[i], var16 + 8.0F, y + h / 2.0F, size, -920584, false);
            String tip;
            if (over && (tip = this.module.getPreviewTestTooltip(i)) != null) {
               this.frameTooltip = tip;
            }

            this.previewTests[this.previewTestCount++] = new float[]{var16, y, w, h, i};
            x = var16 - 5.0F;
         }

         return x + 5.0F;
      }
   }

   private void updateTooltip(String text, float dt) {
      if (text != null && text.equals(this.tooltipText)) {
         this.tooltipTime += dt;
      } else {
         this.tooltipText = text;
         this.tooltipTime = 0.0F;
      }
   }

   private void drawTooltip(float dt) {
      if (this.tooltipText != null && !this.tooltipText.isEmpty() && !(this.tooltipTime < 0.45F)) {
         float a = Easing.clamp01((this.tooltipTime - 0.45F) / 0.15F);
         float w = Ui.width(this.tooltipText, 8.6F) + 16.0F;
         float x = Math.min(this.mouseX + 12.0F, this.vw - w - 6.0F);
         float y = this.mouseY + 16.0F;
         if (y + 22.0F > this.vh - Ui.offsetY - 4.0F) {
            y = this.mouseY - 28.0F;
         }

         Ui.pushAlpha(a);
         Ui.shadow(x, y + 2.0F, w, 20.0F, 6.0F, 8.0F, Theme.LIGHT ? 1073741824 : -2013265920);
         Ui.roundRect(x, y, w, 20.0F, 6.0F, Theme.TOOLTIP);
         Ui.outline(x, y, w, 20.0F, 6.0F, 0.8F, Theme.TOOLTIP_BORDER);
         Ui.textMiddle(this.tooltipText, x + 8.0F, y + 10.0F, 8.6F, Theme.TEXT, false);
         Ui.popAlpha();
      }
   }

   static boolean inside(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   public void handleMouseInput() {
      float mx = Mouse.getEventX() / Ui.px;
      float my = (this.mc.displayHeight - Mouse.getEventY() - 1) / Ui.px - Ui.offsetY;
      int button = Mouse.getEventButton();
      if (button >= 0 && Mouse.getEventButtonState() || Mouse.getEventDWheel() != 0) {
         this.cancelRestore();
      }

      if (button >= 0 || Mouse.getEventDWheel() == 0 || !GuiScreen.isCtrlKeyDown() && !testCtrl) {
         if (button >= 0 && Mouse.getEventButtonState() && Popovers.isOpen()) {
            if (this.focused != null && !Popovers.covers(mx, my)) {
               this.focused.unfocus();
               this.focused = null;
            }

            Popovers.mouseDown(mx, my, button);
         } else if (button < 0 || !Mouse.getEventButtonState() || this.guide == null || this.closing || !this.guide.mouseDown(mx, my, button)) {
            if (button < 0 || Mouse.getEventButtonState() || !Popovers.mouseUp(mx, my)) {
               if (button >= 0 || !Popovers.mouseDrag(mx, my)) {
                  if (Mouse.getEventDWheel() == 0 || !Popovers.wheel(mx, my, Mouse.getEventDWheel())) {
                     if (button >= 0) {
                        if (Mouse.getEventButtonState()) {
                           this.mousePressed(mx, my, button);
                        } else {
                           this.mouseReleased(mx, my);
                        }
                     } else if (this.resizing) {
                        this.resizeDrag(mx, my);
                     } else if (this.searchPressed) {
                        this.searchBar.mouseDrag(mx, my);
                     } else if (this.globalPressed) {
                        this.globalBar.mouseDrag(mx, my);
                     } else if (this.barDrag != null) {
                        this.barDrag.mouseDrag(mx, my);
                     } else if (this.active != null) {
                        this.active.mouseDrag(mx, my);
                     } else if (this.previewDrag) {
                        this.preview.drag(mx, my);
                     } else if (this.presetsPage && !this.globalPage) {
                        this.presetsUi.mouseDrag(mx, my);
                     }

                     int wheel = Mouse.getEventDWheel();
                     if (wheel != 0) {
                        this.wheelAt(mx, my, wheel);
                     }
                  }
               }
            }
         }
      } else {
         this.zoomStep(Mouse.getEventDWheel() > 0 ? 1 : -1);
      }
   }

   private void wheelAt(float mx, float my, int wheel) {
      if (this.sideMode != MenuPrefs.SidebarStyle.HIDDEN && inside(mx, my, this.panelX, this.panelY, this.sideW, this.panelH)) {
         this.navBar.wheel(wheel);
      } else if (this.globalPage) {
         if (inside(mx, my, this.listX - 10.0F, this.listY, this.settingsAreaW() + 20.0F, this.listH)) {
            this.globalSearch.wheel(wheel);
         }
      } else if (this.settingsPage && inside(mx, my, this.listX - 10.0F, this.listY, this.settingsAreaW() + 20.0F, this.listH)) {
         if (!this.globalWidget.mouseWheel(mx, my, wheel)) {
            this.settingsBar.wheel(wheel);
         }
      } else if (this.presetsPage) {
         this.presetsUi.wheel(mx, my, wheel);
      } else if (this.modulePage() && this.tabsShown && this.tabRow.contains(mx, my)) {
         this.lastListWheel = System.nanoTime();
         (this.searching() ? this.sectionSearch.bar : this.listBar).wheel(wheel);
      } else if (this.modulePage() && inside(mx, my, this.listX - 10.0F, this.listY, this.listW + 20.0F, this.listH)) {
         long now = System.nanoTime();
         if (!this.searching() && now - this.lastListWheel > 300000000L) {
            for (int i = 0; i < this.widgets.size(); i++) {
               Widget widget = this.widgets.get(i);
               if (this.shows(i) && widget.appear > 0.9F && widget.contains(mx, my) && widget.mouseWheel(mx, my, wheel)) {
                  return;
               }
            }
         }

         this.lastListWheel = now;
         (this.searching() ? this.sectionSearch.bar : this.listBar).wheel(wheel);
      }
   }

   private void mousePressed(float mx, float my, int button) {
      int changes = FuriahsVisuals.instance.getConfig().changeCount();
      int sounds = ModSounds.requestCount();
      Widget before = this.active;
      this.mousePressedInner(mx, my, button);
      if (ModSounds.requestCount() == sounds) {
         boolean took = this.active != null && this.active != before && !this.active.isDragging();
         if (took || FuriahsVisuals.instance.getConfig().changeCount() != changes) {
            ModSounds.gui(Sfx.CLICK);
         }
      }
   }

   private void mousePressedInner(float mx, float my, int button) {
      if (!this.closing && !(this.open < 0.6F)) {
         if (this.listeningForKey) {
            this.listeningForKey = false;
         } else if (button != 3 || !this.goBack()) {
            if (this.focused != null && !this.focused.contains(mx, my)) {
               this.focused.unfocus();
               this.focused = null;
            }

            if (button == 0 && this.onGrip(mx, my)) {
               long now = System.currentTimeMillis();
               if (now - this.lastGripClick < 350L) {
                  this.lastGripClick = 0L;
                  this.resetPanelSize();
               } else {
                  this.lastGripClick = now;
                  this.resizing = true;
                  this.resizeStartX = mx;
                  this.resizeStartY = my;
                  this.resizeStartW = this.panelW;
                  this.resizeStartH = this.panelH;
                  this.resizeW = this.panelW;
                  this.resizeH = this.panelH;
               }
            } else if (button != 0
               || !inside(mx, my, this.settingsX, this.settingsY, this.settingsW, this.setH)
                  && !inside(mx, my, this.hdrSettingsX, this.hdrIconY, this.buttonH, this.buttonH)) {
               if (button != 0
                  || !inside(mx, my, this.presetsX, this.presetsY, this.setH, this.setH)
                     && !inside(mx, my, this.hdrPresetsX, this.hdrIconY, this.buttonH, this.buttonH)) {
                  if (this.globalX > -5000.0F && this.globalBar.mouseDown(mx, my, button) != 0) {
                     if (this.focused != null) {
                        this.focused.unfocus();
                        this.focused = null;
                     }

                     if (this.presetsUi != null) {
                        this.presetsUi.unfocus();
                     }

                     this.searchBar.unfocus();
                     this.globalPressed = true;
                     if (!this.globalBar.text().trim().isEmpty() || this.sideMode != MenuPrefs.SidebarStyle.FULL) {
                        this.openGlobal();
                     } else if (this.globalPage) {
                        this.closeGlobal();
                        this.globalBar.focus();
                     }
                  } else {
                     this.globalBar.unfocus();
                     if (button != 0
                        || !inside(mx, my, this.magX, this.magY, this.magS, this.magS)
                           && !inside(mx, my, this.hdrSearchX, this.hdrIconY, this.buttonH, this.buttonH)) {
                        if (button == 0 && inside(mx, my, this.hdrSectionsX, this.hdrIconY, this.buttonH, this.buttonH)) {
                           this.openSectionsList();
                        } else if (button != 0 || !this.lockBannerPressed(mx, my)) {
                           if (button == 0 && this.navBar.mouseDown(mx, my)) {
                              this.barDrag = this.navBar;
                           } else {
                              FuriahScreen.NavEntry nav = this.navAt(mx, my);
                              if (nav != null) {
                                 if (button == 0) {
                                    if (nav.module == null) {
                                       boolean fold = !categoryFolded(nav.category);
                                       MenuMemory.folded("nav:" + nav.category, fold);
                                       ModSounds.gui(fold ? Sfx.CLICK : Sfx.DROPDOWN);
                                    } else if (this.onNavDot(nav, mx, my)) {
                                       nav.module.setEnabled(!nav.module.isEnabled());
                                       ModSounds.toggle(nav.module.isEnabled());
                                    } else {
                                       if (nav.module != this.module || !this.modulePage()) {
                                          ModSounds.gui(Sfx.SECTION);
                                       }

                                       this.showSection(nav.module);
                                    }
                                 }
                              } else if (this.backShown
                                 && button == 0
                                 && inside(mx, my, this.backX - 2.0F, this.backY - 2.0F, this.backS + 4.0F, this.backS + 4.0F)) {
                                 this.goBack();
                              } else if (!this.globalPage && this.searchBar.mouseDown(mx, my, button) != 0) {
                                 if (this.focused != null) {
                                    this.focused.unfocus();
                                    this.focused = null;
                                 }

                                 if (this.presetsUi != null) {
                                    this.presetsUi.unfocus();
                                 }

                                 this.searchPressed = true;
                              } else {
                                 this.searchBar.unfocus();
                                 if (this.globalPage) {
                                    if (button == 0 && this.globalSearch.bar().mouseDown(mx, my)) {
                                       this.barDrag = this.globalSearch.bar();
                                    } else {
                                       this.globalSearch.mouseDown(mx, my, button);
                                    }
                                 } else if (this.presetsPage) {
                                    this.presetsUi.mouseDown(mx, my, button);
                                 } else if (!this.settingsPage) {
                                    for (int i = 0; i < this.actionBoxes.size() && i < this.actionList.size(); i++) {
                                       float[] b = this.actionBoxes.get(i);
                                       if (button == 0 && inside(mx, my, b[0], b[1], b[2], b[3])) {
                                          this.actionList.get(i).action.run();
                                          return;
                                       }
                                    }

                                    if (this.tabsShown && this.tabRow.contains(mx, my)) {
                                       this.tabPressed(mx, my, button);
                                    } else if (inside(mx, my, this.switchX - 4.0F, this.switchY - 4.0F, this.switchW + 8.0F, this.switchH + 8.0F)
                                       && button == 0) {
                                       BooleanSetting sw = this.headerSwitch();
                                       sw.set(!sw.get());
                                       ModSounds.toggle(sw.get());
                                    } else if (this.moveW > 0.0F && inside(mx, my, this.moveX, this.moveY, this.moveW, this.buttonH) && button == 0) {
                                       if (this.mc.theWorld != null) {
                                          ModSounds.gui(Sfx.CLICK);
                                          FuriahsVisuals.instance.getUi().openMoveHud();
                                       } else {
                                          ModSounds.gui(Sfx.ERROR);
                                       }
                                    } else if (button != 0 || !this.jumpBarPressed(mx, my)) {
                                       if (this.module.hasTestAction() && inside(mx, my, this.testX, this.testY, this.testW, this.buttonH) && button == 0) {
                                          if (this.mc.theWorld != null && !this.blocked() && this.module.testUnavailable() == null) {
                                             ModSounds.gui(Sfx.CLICK);
                                             this.module.runTest();
                                             if (this.module.testClosesMenu()) {
                                                this.close();
                                             }
                                          } else {
                                             ModSounds.gui(Sfx.ERROR);
                                          }
                                       } else if (inside(mx, my, this.resetX, this.resetY, this.resetW, this.buttonH) && button == 0) {
                                          if (System.currentTimeMillis() < this.resetConfirmUntil) {
                                             if (this.onSubPage()) {
                                                for (Setting s : this.page.settings()) {
                                                   s.reset();
                                                }
                                             } else {
                                                this.module.resetToDefaults();
                                             }

                                             this.module.restartPreview();
                                             this.resetConfirmUntil = 0L;
                                             ModSounds.gui(Sfx.CLICK);
                                          } else {
                                             this.resetConfirmUntil = System.currentTimeMillis() + 3000L;
                                             ModSounds.gui(Sfx.CONFIRM);
                                          }
                                       } else if (this.module.hasPreview()
                                          && inside(mx, my, this.previewX, this.previewY, this.previewW, this.previewH)
                                          && button == 0) {
                                          for (int var20 = 0; var20 < this.previewTestCount; var20++) {
                                             float[] b = this.previewTests[var20];
                                             if (inside(mx, my, b[0], b[1], b[2], b[3])) {
                                                this.module.runPreviewTest((int)b[4]);
                                                ModSounds.gui(Sfx.CLICK);
                                                return;
                                             }
                                          }

                                          this.previewDrag = true;
                                          this.preview.startDrag(mx, my);
                                       } else if (this.searching()) {
                                          if (this.sectionSearch.bar.mouseDown(mx, my)) {
                                             this.barDrag = this.sectionSearch.bar;
                                          } else {
                                             if (inside(mx, my, this.listX - 10.0F, this.listY, this.listW + 20.0F, this.listH)
                                                && this.sectionSearch.mouseDown(mx, my, button)
                                                && this.sectionSearch.pressed != null) {
                                                this.active = this.sectionSearch.pressed;
                                                if (this.active.isFocused()) {
                                                   this.focused = this.active;
                                                }
                                             }
                                          }
                                       } else if (this.listBar.mouseDown(mx, my)) {
                                          this.barDrag = this.listBar;
                                       } else {
                                          if (inside(mx, my, this.listX - 10.0F, this.listY, this.listW + 20.0F, this.listH)) {
                                             int col = this.columnAt(mx);
                                             if (my < this.stickyClip[col]) {
                                                HeaderWidget h = this.sticky[col];
                                                if (h != null && button == 0) {
                                                   float natural = h.y - this.lastListTop;
                                                   h.setCollapsed(true);
                                                   ModSounds.gui(Sfx.CLICK);
                                                   this.listBar.target = Math.max(0.0F, natural);
                                                }

                                                return;
                                             }

                                             boolean blocked = this.blocked();
                                             boolean compact = Density.compact();

                                             for (int i3 = 0; i3 < this.widgets.size(); i3++) {
                                                Widget widget = this.widgets.get(i3);
                                                if (!(widget.appear < 0.5F)
                                                   && this.shows(i3)
                                                   && !(my < widget.y)
                                                   && !(my >= widget.y + widget.height() * widget.appear)) {
                                                   if ((!blocked || widget instanceof HeaderWidget) && this.unavailable(widget) == null) {
                                                      boolean arrow = compact
                                                         && i3 < this.rowChildren.length
                                                         && this.rowChildren[i3].length > 0
                                                         && this.hasShowableOptions(i3);
                                                      float rowW = widget.w + (arrow ? 12.0F : 0.0F);
                                                      float cy = widget.y + lineH(widget) / 2.0F;
                                                      boolean inRow = mx >= widget.x - 11.0F
                                                         && mx < widget.x + rowW
                                                         && my >= widget.y
                                                         && my < widget.y + widget.height();
                                                      Setting s = this.settingOf.get(widget);
                                                      if (inRow
                                                         && button == 0
                                                         && mx < widget.x - 1.0F
                                                         && Math.abs(my - cy) < 8.0F
                                                         && s != null
                                                         && s.isSaved()
                                                         && SettingDefaults.known(s)
                                                         && !SettingDefaults.isDefault(s)) {
                                                         s.reset();
                                                         ModSounds.gui(Sfx.CLICK);
                                                         return;
                                                      }

                                                      if (inRow && arrow && button == 0 && mx >= widget.x + widget.w) {
                                                         this.setOptionsOpen(i3, !this.rowOpen[i3]);
                                                         ModSounds.gui(this.rowOpen[i3] ? Sfx.DROPDOWN : Sfx.CLICK);
                                                         return;
                                                      }

                                                      if (inRow && button == 1 && s != null && hasContextMenu(widget)) {
                                                         this.openContextMenu(s, widget, mx, my);
                                                         return;
                                                      }

                                                      boolean couldBefore = arrow
                                                         || compact
                                                            && i3 < this.rowChildren.length
                                                            && this.rowChildren[i3].length > 0
                                                            && this.hasShowableOptions(i3);
                                                      if (widget.mouseDown(mx, my, button)) {
                                                         this.active = widget;
                                                         if (widget.isFocused()) {
                                                            this.focused = widget;
                                                         }

                                                         if (compact
                                                            && !couldBefore
                                                            && i3 < this.rowChildren.length
                                                            && this.rowChildren[i3].length > 0
                                                            && this.hasShowableOptions(i3)) {
                                                            this.setOptionsOpen(i3, true);
                                                         }

                                                         return;
                                                      }
                                                   } else if (button == 0 && widget.contains(mx, my)) {
                                                      ModSounds.gui(Sfx.ERROR);
                                                      return;
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    }
                                 } else if (this.settingsBar.mouseDown(mx, my)) {
                                    this.barDrag = this.settingsBar;
                                 } else if (inside(mx, my, this.listX - 10.0F, this.listY, this.settingsAreaW() + 20.0F, this.listH)) {
                                    if (this.settingsSearch == null || !this.settingsSearch.isActive() || !this.settingsSearch.mouseDown(mx, my, button)) {
                                       if (this.globalWidget.mouseDown(mx, my, button)) {
                                          this.active = this.globalWidget;
                                          if (this.globalWidget.isFocused()) {
                                             this.focused = this.globalWidget;
                                          }
                                       } else if (this.scaleWidget.mouseDown(mx, my, button)) {
                                          this.active = this.scaleWidget;
                                          if (this.scaleWidget.isFocused()) {
                                             this.focused = this.scaleWidget;
                                          }
                                       } else if (button == 0) {
                                          if (inside(mx, my, this.setAllX, this.setAllY, this.setAllW, 22.0F)) {
                                             if (System.currentTimeMillis() < this.setAllConfirmUntil) {
                                                this.setAllCount = FuriahsVisuals.instance.setAllEffectsToGlobal();
                                                this.setAllConfirmUntil = 0L;
                                                this.setAllDoneUntil = ClientClock.millis() + 3500L;
                                                this.accentSet = false;
                                                ModSounds.gui(Sfx.SUCCESS);
                                             } else {
                                                this.setAllConfirmUntil = System.currentTimeMillis() + 3000L;
                                                ModSounds.gui(Sfx.CONFIRM);
                                             }
                                          } else if (inside(mx, my, this.keyX, this.keyY, this.keyW, 22.0F)) {
                                             this.listeningForKey = true;
                                             ModSounds.gui(Sfx.CLICK);
                                          } else {
                                             int vt = Segmented.hit(
                                                this.viewThemeX, this.viewThemeY, this.viewThemeW, 20.0F, Density.Mode.values().length, mx, my
                                             );
                                             if (vt >= 0) {
                                                if (vt != MenuPrefs.VIEW_THEME.get().ordinal()) {
                                                   MenuPrefs.VIEW_THEME.set(Density.Mode.values()[vt]);
                                                   ModSounds.gui(Sfx.CLICK);
                                                }
                                             } else {
                                                int sb = Segmented.hit(
                                                   this.sidebarX, this.sidebarY, this.sidebarW, 20.0F, MenuPrefs.SidebarStyle.values().length, mx, my
                                                );
                                                if (sb >= 0) {
                                                   if (sb != MenuPrefs.SIDEBAR.get().ordinal()) {
                                                      MenuPrefs.SIDEBAR.set(MenuPrefs.SidebarStyle.values()[sb]);
                                                      ModSounds.gui(Sfx.CLICK);
                                                   }
                                                } else {
                                                   int gs = Segmented.hit(
                                                      this.scaleX, this.scaleY, this.scaleW, 20.0F, MenuPrefs.SCALE_STEPS.length + 1, mx, my
                                                   );
                                                   if (gs < 0) {
                                                      if (!this.fullScreenWidget.mouseDown(mx, my, button)) {
                                                         if (inside(mx, my, this.sizeResetX, this.sizeResetY, this.sizeResetW, 18.0F)) {
                                                            this.resetPanelSize();
                                                         } else if (this.guide == null
                                                            && inside(mx, my, this.guideButtonX, this.guideButtonY, this.guideButtonW, 20.0F)) {
                                                            ModSounds.gui(Sfx.CLICK);
                                                            this.openGuide();
                                                         } else if (this.soundsBlock.mouseDown(mx, my, button)) {
                                                            this.active = this.soundsBlock.pressed();
                                                         } else if (!this.flashWidget.mouseDown(mx, my, button)) {
                                                            Theme.Style[] styles = Theme.Style.values();

                                                            for (int i2 = 0; i2 < styles.length; i2++) {
                                                               if (inside(mx, my, this.cardX[i2], this.cardY[i2], this.cardW, this.cardH)) {
                                                                  FuriahsVisuals.instance.getTheme().set(styles[i2]);
                                                                  this.accentSet = false;
                                                                  ModSounds.gui(Sfx.SELECT);
                                                                  return;
                                                               }
                                                            }
                                                         }
                                                      }
                                                   } else {
                                                      if (gs == 0) {
                                                         this.zoomReset();
                                                      } else {
                                                         int pct = MenuPrefs.SCALE_STEPS[gs - 1];
                                                         if (MenuPrefs.GUI_SCALE_AUTO.get() || MenuPrefs.GUI_SCALE.getInt() != pct) {
                                                            MenuPrefs.GUI_SCALE.set(pct);
                                                            MenuPrefs.GUI_SCALE_AUTO.set(false);
                                                            ModSounds.gui(Sfx.CLICK);
                                                         }

                                                         this.zoomToastUntil = ClientClock.millis() + 1300L;
                                                      }
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     } else {
                        ModSounds.gui(Sfx.SECTION);
                        if (this.globalPage) {
                           this.closeGlobal();
                        } else {
                           this.openGlobal();
                        }
                     }
                  }
               } else {
                  if (!this.presetsPage || this.globalPage) {
                     ModSounds.gui(Sfx.SECTION);
                  }

                  this.openPresets();
               }
            } else {
               if (!this.settingsPage || this.globalPage) {
                  ModSounds.gui(Sfx.SECTION);
               }

               this.openSettings(false);
            }
         }
      }
   }

   private void mouseReleased(float mx, float my) {
      if (this.resizing) {
         this.resizing = false;
         if (Math.abs(this.resizeW - this.resizeStartW) >= 1.0F || Math.abs(this.resizeH - this.resizeStartH) >= 1.0F) {
            MenuPrefs.PANEL_W.set(Math.round(this.resizeW));
            MenuPrefs.PANEL_H.set(Math.round(this.resizeH));
            MenuPrefs.FULL_SCREEN.set(false);
         }
      }

      this.searchBar.mouseUp();
      this.searchPressed = false;
      this.globalBar.mouseUp();
      this.globalPressed = false;
      if (this.barDrag != null) {
         this.barDrag.mouseUp();
         this.barDrag = null;
      }

      if (this.presetsUi != null) {
         this.presetsUi.mouseUp();
      }

      if (this.active != null) {
         Widget was = this.active;
         this.active.mouseUp(mx, my);
         if (this.active.isFocused()) {
            this.focused = this.active;
         }

         this.active = null;
         if (was == this.scaleWidget) {
            this.applyScaleDraft();
         }
      }

      if (this.previewDrag) {
         this.previewDrag = false;
         if (this.preview.endDrag()) {
            this.module.restartPreview();
         }
      }
   }

   public String sliderWheelForTest() {
      this.cancelRestore();

      for (int i = 0; i < this.widgets.size(); i++) {
         Widget w = this.widgets.get(i);
         if (w instanceof SliderWidget && this.shows(i) && !(w.appear < 0.99F) && !(w.y < this.listY) && !(w.y + w.height() > this.listY + this.listH)) {
            SliderSetting s = ((SliderWidget)w).getSetting();
            double before = s.get();
            float from = this.listBar.target;
            float[] c = ((SliderWidget)w).fillMiddleForCheck();
            this.lastListWheel = 0L;
            this.wheelAt(c[0], c[1], -120);
            return s.getId()
               + ": "
               + before
               + " -> "
               + s.get()
               + ", list "
               + Math.round(from)
               + " -> "
               + Math.round(this.listBar.target)
               + " (max "
               + Math.round(this.listBar.max())
               + ")";
         }
      }

      return "no slider in view";
   }

   public String colorWheelForTest() {
      this.cancelRestore();

      for (int i = 0; i < this.widgets.size(); i++) {
         Widget w = this.widgets.get(i);
         float[] h = w instanceof ColorWidget && this.shows(i) ? ((ColorWidget)w).hueStripForTest() : null;
         if (h != null) {
            this.lastListWheel = 0L;
            this.wheelAt(h[0], h[1], 120);
            float after = ((ColorWidget)w).hueStripForTest()[2];
            float from = this.listBar.target;
            this.lastListWheel = 0L;
            this.wheelAt(w.x + 12.0F, h[1], -120);
            return String.format(
               Locale.ROOT,
               "hue %.4f -> %.4f, square: list %d -> %d (max %d)",
               h[2],
               after,
               Math.round(from),
               Math.round(this.listBar.target),
               Math.round(this.listBar.max())
            );
         }
      }

      return "no open color picker";
   }

   public String previewTestsForTest() {
      String[] tests = this.module.getPreviewTests();
      if (tests == null) {
         return "none";
      } else {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < this.previewTestCount; i++) {
            float[] b = this.previewTests[i];
            sb.append(sb.length() == 0 ? "" : ", ").append(tests[(int)b[4]]).append(String.format(Locale.ROOT, " at %.0f, %.0f", b[0], b[1]));
         }

         return sb.length() == 0 ? "not drawn" : sb.toString();
      }
   }

   public void previewTestForTest(int index) {
      for (int i = 0; i < this.previewTestCount; i++) {
         float[] b = this.previewTests[i];
         if ((int)b[4] == index) {
            this.clickForTest(b[0] + b[2] / 2.0F, b[1] + b[3] / 2.0F);
            return;
         }
      }
   }

   public void revealSliderForTest() {
      this.cancelRestore();

      for (int i = 0; i < this.widgets.size(); i++) {
         if (this.widgets.get(i) instanceof SliderWidget && this.shows(i)) {
            this.listBar.target = this.listBar.scroll + (this.widgets.get(i).y - this.listY) - 30.0F;
            return;
         }
      }
   }

   public void colorStaticForTest(boolean restore) {
      if (restore) {
         if (this.staticForTest != null) {
            this.staticForTest.fromJson(this.staticBeforeTest);
            this.staticForTest = null;
         }
      } else {
         for (Widget widget : this.widgets) {
            if (widget instanceof ColorWidget && widget.isVisible()) {
               ColorSetting s = ((ColorWidget)widget).getSetting();
               if (s.getMode() == ColorSetting.Mode.GLOBAL) {
                  this.staticForTest = s;
                  this.staticBeforeTest = s.toJson();
                  s.setMode(ColorSetting.Mode.STATIC);
               }

               return;
            }
         }
      }
   }

   public void expandColorPickerForTest() {
      this.cancelRestore();

      for (Widget widget : this.widgets) {
         if (widget instanceof ColorWidget && widget.isVisible() && ((ColorWidget)widget).getSetting().getMode() != ColorSetting.Mode.GLOBAL) {
            ((ColorWidget)widget).setExpanded(true);
            this.listBar.target = this.listBar.scroll + (widget.y - this.listY) - 30.0F;
            return;
         }
      }
   }

   public String clickNearPreviewForTest() {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < this.widgets.size(); i++) {
         if (this.widgets.get(i) instanceof CollapsiblePreview && this.shows(i)) {
            Widget inner = ((CollapsiblePreview)this.widgets.get(i)).inner();
            float reachTop = inner.y;
            float reachBottom = inner.y + inner.height();

            for (int k = 0; k < this.widgets.size(); k++) {
               Widget w = this.widgets.get(k);
               float cy;
               if (k != i
                  && w instanceof ToggleWidget
                  && this.shows(k)
                  && !(w.appear < 0.99F)
                  && !((cy = w.y + w.height() / 2.0F) < reachTop)
                  && !(cy >= reachBottom)
                  && !(w.y < this.listY)
                  && !(w.y + w.height() > this.listY + this.listH)) {
                  BooleanSetting s = ((ToggleWidget)w).getSetting();
                  boolean before = s.get();
                  this.clickForTest(w.x + w.w / 2.0F, cy);
                  boolean took = s.get() != before;
                  if (took) {
                     this.clickForTest(w.x + w.w / 2.0F, cy);
                  }

                  sb.append(sb.length() == 0 ? "" : ", ").append(s.getId()).append(took ? " toggled" : " DID NOT TOGGLE");
               }
            }
         }
      }

      return sb.length() == 0 ? "no toggle within a folded preview's reach" : sb.toString();
   }

   public void scrollToTopForTest() {
      this.cancelRestore();
      this.listBar.target = 0.0F;
   }

   public String pageForTest() {
      return this.globalPage ? "#search" : this.pageKey();
   }

   public void openSettingsForTest() {
      this.openSettings(false);
   }

   public void listenForMenuKeyForTest() {
      this.settingsPage = true;
      this.presetsPage = false;
      this.globalPage = false;
      this.listeningForKey = true;
   }

   public void showSettingsForTest(boolean settings) {
      this.settingsPage = settings;
      this.presetsPage = false;
      this.globalPage = false;
      this.accentSet = false;
   }

   public boolean clickHeaderSwitchForTest() {
      boolean before = this.headerSwitch().get();
      this.mousePressed(this.switchX + this.switchW / 2.0F, this.switchY + this.switchH / 2.0F, 0);
      boolean after = this.headerSwitch().get();
      if (after != before) {
         this.headerSwitch().set(before);
      }

      return after != before;
   }

   private boolean lockBannerPressed(float mx, float my) {
      return this.bannerReason != null && inside(mx, my, this.bannerX, this.bannerY, this.bannerW, this.bannerH);
   }

   public String lockBannerForTest() {
      return this.bannerReason == null ? null : "Visuals locked: " + this.bannerReason;
   }

   public void showPresetsForTest() {
      this.openPresets();
   }

   public String createPresetForTest(String name) {
      this.openPresets();
      this.presetsUi.typeNameForTest(name);
      this.presetsUi.createForTest();
      return this.presetsUi.messageForTest();
   }

   public void clickForTest(float x, float y) {
      if (Popovers.isOpen()) {
         Popovers.mouseDown(x, y, 0);
         Popovers.mouseUp(x, y);
      } else if (this.guide == null || !this.guide.mouseDown(x, y, 0)) {
         this.mousePressed(x, y, 0);
         this.mouseReleased(x, y);
      }
   }

   public ScrollBar listBarForTest() {
      return this.listBar;
   }

   public void dragListBarForTest(float fromX, float fromY, float toX, float toY) {
      if (this.listBar.mouseDown(fromX, fromY)) {
         this.barDrag = this.listBar;
         this.listBar.mouseDrag(toX, toY);
         this.listBar.mouseUp();
         this.barDrag = null;
      }
   }

   public float[] listGeometryForTest() {
      return new float[]{this.listX + this.listW + 8.0F, this.listY, this.listH, this.vw, this.vh};
   }

   public ScrollBar navBarForTest() {
      return this.navBar;
   }

   public float[] navGeometryForTest() {
      float fadeTop = Math.min(22.0F, this.navBar.scroll);
      float fadeBottom = Math.min(22.0F, this.navBar.max() - this.navBar.scroll);
      float pinnedTop = this.sideMode == MenuPrefs.SidebarStyle.ICONS ? this.presetsY : this.settingsY;
      return new float[]{this.navBarX(), this.navTop, this.navH, this.navContent, fadeTop, fadeBottom, pinnedTop, this.panelH};
   }

   public float[] navRowForTest(String id) {
      FuriahScreen.NavEntry e = this.navEntryOf(this.modules.get(id));
      if (e == null) {
         return null;
      } else {
         float y = this.navTop - this.navBar.scroll + e.y + e.h / 2.0F;
         return this.sideMode == MenuPrefs.SidebarStyle.ICONS
            ? new float[]{this.navIconX(e.h) + e.h / 2.0F, y}
            : new float[]{this.panelX + 10.0F + this.navRowW() / 2.0F, y};
      }
   }

   public float[] navCategoryForTest(String category) {
      for (FuriahScreen.NavEntry e : this.navEntries) {
         if (e.module == null && e.category.equals(category)) {
            return new float[]{this.panelX + 10.0F + this.navRowW() / 2.0F, this.navTop - this.navBar.scroll + e.y + e.h / 2.0F};
         }
      }

      return null;
   }

   public String navAtForTest(float x, float y) {
      FuriahScreen.NavEntry e = this.navAt(x, y);
      return e == null ? null : (e.module != null ? e.module.getId() : "cat:" + e.category);
   }

   public String sidebarForTest() {
      StringBuilder sb = new StringBuilder(this.sideMode.name()).append(String.format(Locale.ROOT, " %.0f wide:", this.sideW));

      for (FuriahScreen.NavEntry e : this.navEntries) {
         sb.append(' ').append(e.module != null ? e.module.getId() : "[" + e.category + (categoryFolded(e.category) ? " folded]" : "]"));
      }

      return sb.toString();
   }

   public float[] globalButtonForTest() {
      if (this.sideMode == MenuPrefs.SidebarStyle.FULL) {
         float[] fArray2;
         if (this.globalX < -5000.0F) {
            fArray2 = null;
         } else {
            float[] fArray3 = new float[]{this.globalX + this.globalW * 0.4F, 0.0F};
            fArray2 = fArray3;
            fArray3[1] = this.globalY + this.globalH / 2.0F;
         }

         return fArray2;
      } else if (this.sideMode == MenuPrefs.SidebarStyle.ICONS) {
         float[] fArray4;
         if (this.magX < -5000.0F) {
            fArray4 = null;
         } else {
            float[] fArray5 = new float[]{this.magX + this.magS / 2.0F, 0.0F};
            fArray4 = fArray5;
            fArray5[1] = this.magY + this.magS / 2.0F;
         }

         return fArray4;
      } else {
         float[] fArray;
         if (this.hdrSearchX < -5000.0F) {
            fArray = null;
         } else {
            float[] fArray6 = new float[]{this.hdrSearchX + this.buttonH / 2.0F, 0.0F};
            fArray = fArray6;
            fArray6[1] = this.hdrIconY + this.buttonH / 2.0F;
         }

         return fArray;
      }
   }

   public float[] sectionsButtonForTest() {
      float[] fArray;
      if (this.hdrSectionsX < -5000.0F) {
         fArray = null;
      } else {
         float[] fArray2 = new float[]{this.hdrSectionsX + this.buttonH / 2.0F, 0.0F};
         fArray = fArray2;
         fArray2[1] = this.hdrIconY + this.buttonH / 2.0F;
      }

      return fArray;
   }

   public float[] sectionsRowForTest(String id, boolean dot) {
      return Popovers.current() instanceof SectionsPopover ? ((SectionsPopover)Popovers.current()).rowForTest(id, dot) : null;
   }

   public float[] navDotForTest(String id) {
      FuriahScreen.NavEntry e = this.navEntryOf(this.modules.get(id));
      return e == null ? null : this.navDotAt(e, this.navTop - this.navBar.scroll + e.y);
   }

   public float[] globalBoxForTest() {
      float[] fArray;
      if (this.globalX < -5000.0F) {
         fArray = null;
      } else {
         float[] fArray2 = new float[]{this.globalX, this.globalY, this.globalW, 0.0F};
         fArray = fArray2;
         fArray2[3] = this.globalH;
      }

      return fArray;
   }

   public String globalTextForTest() {
      return this.globalBar.text();
   }

   public void globalSearchForTest(String text) {
      if (this.globalX < -5000.0F) {
         this.openGlobal();
      } else {
         this.globalBar.focus();
      }

      this.globalBar.setText("");

      for (char c : text.toCharArray()) {
         this.keyTyped(c, 0);
      }

      this.updateSearch();
   }

   public List<String> globalResultsForTest() {
      return this.globalSearch.describeForTest();
   }

   public float[] globalResultForTest(String page) {
      return this.globalSearch.resultForTest(page);
   }

   void checkShow(GuiCheck.Page p) {
      this.dropFocus();
      if (p.special != null) {
         if ("#settings".equals(p.special)) {
            this.openSettings(true);
         } else if ("#presets".equals(p.special)) {
            this.openPresets();
         } else {
            this.globalBar.setText("color");
            this.openGlobal();
            this.globalBar.unfocus();
            this.updateSearch();
         }
      } else {
         if (p.module != this.module || !this.modulePage()) {
            this.showSection(p.module);
         }

         if (p.sub != null) {
            LayoutPage target = this.layout.page(p.sub);
            if (target != null && target != this.page) {
               this.showPage(target, false);
            }
         } else if (this.page != null && !this.page.isSection()) {
            this.showPage(this.layout.firstPage(), false);
         }

         this.tabRow.forcedMode = p.tabMode;
         if (p.weaponTab != null && this.module instanceof WeaponsModule) {
            WeaponsModule weapons = (WeaponsModule)this.module;
            boolean compare = p.weaponTab.endsWith("+compare");
            weapons.previewForTest(compare ? p.weaponTab.substring(0, p.weaponTab.length() - "+compare".length()) : p.weaponTab, null, 1.0F);
            weapons.compareForTest(compare, -1.0F);
         }

         for (int i = 0; i < this.widgets.size(); i++) {
            Widget w = this.widgets.get(i);
            if (w instanceof HeaderWidget) {
               ((HeaderWidget)w).setCollapsed(false);
            } else if (w instanceof CollapsiblePreview) {
               ((CollapsiblePreview)w).setOpenForCheck(!p.previewsFolded);
            }

            if (i < this.rowChildren.length && this.rowChildren[i].length > 0) {
               this.setOptionsOpen(i, true);
            }
         }
      }
   }

   float[] checkScroll() {
      ScrollBar b = this.checkBar();
      float[] fArray;
      if (b == null) {
         float[] fArray2 = new float[]{0.0F, this.listH, 0.0F};
         fArray = fArray2;
         fArray2[2] = 0.0F;
      } else {
         float[] fArray3 = new float[3];
         fArray3[0] = b.max();
         fArray3[1] = this.listH;
         fArray = fArray3;
         fArray3[2] = b.scroll;
      }

      return fArray;
   }

   void checkScrollTo(float units) {
      this.cancelRestore();
      ScrollBar b = this.checkBar();
      if (b != null) {
         b.target = units;
         b.jumpTo(units);
      }
   }

   private ScrollBar checkBar() {
      if (this.globalPage) {
         return this.globalSearch.bar();
      } else if (this.settingsPage) {
         return this.settingsBar;
      } else if (this.presetsPage) {
         return null;
      } else {
         return this.searching() ? this.sectionSearch.bar : this.listBar;
      }
   }

   void closeForCheck() {
      this.close();
   }

   void checkDone() {
      this.tabRow.forcedMode = -1;
      if (!this.closing) {
         this.globalBar.setText("");
         this.closeGlobal();
         this.showSection(this.modules.getModules().get(0));
         this.listBar.jumpTo(0.0F);
      }
   }

   public float[] headerForTest() {
      return new float[]{
         this.backShown ? this.backX + this.backS / 2.0F : -10000.0F,
         this.backY + this.backS / 2.0F,
         this.switchX + this.switchW / 2.0F,
         this.switchY + this.switchH / 2.0F,
         this.resetX + this.resetW / 2.0F,
         this.resetY + this.buttonH / 2.0F,
         this.moveW > 0.0F ? this.moveX + this.moveW / 2.0F : -10000.0F,
         this.moveY + this.buttonH / 2.0F,
         this.hdrSettingsX > -5000.0F ? this.hdrSettingsX + this.buttonH / 2.0F : -10000.0F,
         this.hdrIconY + this.buttonH / 2.0F,
         this.headerH,
         this.searchX,
         this.searchY,
         this.searchW,
         this.searchH
      };
   }

   public List<String> jumpBarForTest() {
      ArrayList<String> out = new ArrayList<>();
      if (!this.jumpWanted) {
         return out;
      } else {
         Set<HeaderWidget> lit = this.currentGroups(this.shownGroups());

         for (HeaderWidget h : this.jumpHeaders) {
            out.add(this.chipLabel(h) + (lit.contains(h) ? "*" : ""));
         }

         out.add("[" + (this.collapseX > -5000.0F ? "collapse/expand" : "-") + "]");
         return out;
      }
   }

   public float[] jumpChipForTest(String label) {
      if ("*all".equals(label)) {
         return new float[]{this.collapseX + this.collapseW / 2.0F, this.jumpY + this.jumpH / 2.0F};
      } else {
         for (int k = 0; k < this.jumpHeaders.size(); k++) {
            if (this.chipLabel(this.jumpHeaders.get(k)).equalsIgnoreCase(label)) {
               float[] c = this.jumpChips.get(k);
               return new float[]{c[0] + c[2] / 2.0F, c[1] + c[3] / 2.0F};
            }
         }

         return null;
      }
   }

   public String scaleForTest() {
      return String.format(
         Locale.ROOT,
         "scale %d %%%s (px %.3f, drawn at %.3f), window %.0f x %.0f units, panel %.0f x %.0f%s, sidebar %s, %d column%s",
         this.scalePercent(),
         MenuPrefs.GUI_SCALE_AUTO.get() ? " Auto" : "",
         this.targetPx(),
         Ui.px,
         this.vw,
         this.vh,
         this.panelW,
         this.panelH,
         MenuPrefs.FULL_SCREEN.get() ? " (full screen)" : (MenuPrefs.PANEL_W.getInt() > 0 ? " (own size)" : ""),
         this.sideMode,
         this.columns,
         this.columns == 1 ? "" : "s"
      );
   }

   public void ctrlWheelForTest(int dir) {
      this.zoomStep(dir);
   }

   public void resizeForTest(float dx, float dy) {
      float gx = this.panelX + this.panelW - 6.0F;
      float gy = this.panelY + this.panelH - 6.0F;
      this.lastGripClick = 0L;
      this.mousePressed(gx, gy, 0);
      this.resizeDrag(gx + dx, gy + dy);
      this.mouseReleased(gx + dx, gy + dy);
      this.lastGripClick = 0L;
   }

   public void gripDoubleClickForTest() {
      float gx = this.panelX + this.panelW - 6.0F;
      float gy = this.panelY + this.panelH - 6.0F;
      this.lastGripClick = 0L;
      this.mousePressed(gx, gy, 0);
      this.mouseReleased(gx, gy);
      this.mousePressed(gx, gy, 0);
      this.mouseReleased(gx, gy);
   }

   public float[] scrollExtentForTest() {
      return new float[]{this.listBar.max(), this.listH};
   }

   public List<String> groupsInViewForTest() {
      ArrayList<String> out = new ArrayList<>();

      for (int i = 0; i < this.widgets.size(); i++) {
         if (this.widgets.get(i) instanceof HeaderWidget && this.shows(i)) {
            HeaderWidget h = (HeaderWidget)this.widgets.get(i);
            float top = h.y;
            float bottom = h.y + h.height();

            for (int k = i + 1; k < this.widgets.size() && this.widgetHeaders.get(k) == h; k++) {
               if (this.shows(k)) {
                  bottom = Math.max(bottom, this.widgets.get(k).y + this.widgets.get(k).height());
               }
            }

            if (top >= this.listY - 0.5F && bottom <= this.listY + this.listH + 0.5F) {
               out.add(h.titleForTest());
            }
         }
      }

      return out;
   }

   public int guideStepForTest() {
      return this.guide == null ? -1 : this.guide.step();
   }

   public void openGuideForTest() {
      this.openGuide();
   }

   public boolean hintShownForTest() {
      for (int i = 0; i < this.widgets.size(); i++) {
         if (this.widgets.get(i) instanceof HintWidget && this.shows(i)) {
            return true;
         }
      }

      return false;
   }

   public String lookForTest(int look) {
      for (int i = 0; i < this.widgets.size(); i++) {
         if (this.widgets.get(i) instanceof StartingLookWidget && this.shows(i)) {
            StartingLookWidget w = (StartingLookWidget)this.widgets.get(i);
            if (look >= 0) {
               w.apply(look);
            }

            List<SliderSetting> t = w.targetsNow();
            StringBuilder sb = new StringBuilder("look " + w.current() + " of " + t.size() + ":");

            for (SliderSetting s : t) {
               sb.append(' ').append(s.getId()).append('=').append(s.format());
            }

            return sb.toString();
         }
      }

      return "no starting look on this page";
   }

   public String colorsForTest() {
      StringBuilder sb = new StringBuilder();

      for (Setting s : this.module.getSettings()) {
         if (s instanceof ColorSetting) {
            sb.append(s.getId()).append('=').append(s.toJson()).append(';');
         }
      }

      return sb.toString();
   }

   public String stickyForTest() {
      StringBuilder sb = new StringBuilder(this.columns + (this.columns == 1 ? " column" : " columns"));

      for (int c = 0; c < this.columns; c++) {
         sb.append(", sticky ")
            .append(this.sticky[c] == null ? "-" : this.sticky[c].titleForTest())
            .append(String.format(Locale.ROOT, " (%.1f)", this.stickyY[c] - this.listY));
      }

      return sb.toString();
   }

   public void wheelForTest(float x, float y, int wheel) {
      this.cancelRestore();
      this.wheelAt(x, y, wheel);
   }

   public void dragForTest(float fromX, float fromY, float toX, float toY) {
      this.mousePressed(fromX, fromY, 0);
      if (this.barDrag != null) {
         this.barDrag.mouseDrag(toX, toY);
      }

      this.mouseReleased(toX, toY);
   }

   public float[] pinnedForTest() {
      return new float[]{this.settingsX, this.settingsY, this.settingsW, this.setH, this.presetsX, this.presetsY, this.setH, this.setH};
   }

   public void selectModuleForTest(String id) {
      Module m = this.modules.get(id);
      if (m != null) {
         this.settingsPage = false;
         this.presetsPage = false;
         this.globalPage = false;
         this.select(m);
         this.accentSet = false;
      }
   }

   public void pressKeyForTest(int keyCode) {
      this.keyTyped('\u0000', keyCode);
   }

   public void dragSliderForTest(boolean hold) {
      if (!hold) {
         if (this.active != null) {
            this.active.mouseUp(0.0F, 0.0F);
            this.active = null;
         }
      } else {
         for (int i = 0; i < this.widgets.size(); i++) {
            Widget widget = this.widgets.get(i);
            if (widget instanceof SliderWidget && this.shows(i) && widget.appear > 0.9F && widget.y > this.listY && widget.y < this.listY + this.listH - 40.0F) {
               ((SliderWidget)widget).holdForTest();
               this.active = widget;
               return;
            }
         }
      }
   }

   public void expandGroupsForTest(boolean expand) {
      for (Widget widget : this.widgets) {
         if (widget instanceof HeaderWidget) {
            ((HeaderWidget)widget).setCollapsed(!expand);
         }
      }
   }

   public void scrollForTest(float units) {
      this.cancelRestore();
      this.listBar.target = units;
   }

   public boolean pressResultForTest(Setting setting, float fraction) {
      SectionSearch search = this.pageSearch();
      Widget w = search == null ? null : search.widgetForTest(setting);
      if (w == null) {
         return false;
      } else {
         float mx = w.x + fraction * w.w;
         float my = w.y + Math.min(w.height() - 2.0F, 26.0F);
         boolean took = w.mouseDown(mx, my, 0);
         w.mouseUp(mx, my);
         return took;
      }
   }

   public boolean revealSettingForTest(String id) {
      for (Entry<Setting, Widget> e : this.widgetOf.entrySet()) {
         if (e.getKey().getId().equals(id)) {
            LayoutPage on = this.pageOf(e.getValue());
            if (on != null && on != this.page) {
               this.showPage(on, false);
            }

            this.cancelRestore();
            this.revealWidget = e.getValue();
            this.revealOffset = 0.0F;
            this.revealHeight = -1.0F;
            this.revealFrames = 0;
            this.revealStart = ClientClock.nanos();
            return true;
         }
      }

      return false;
   }

   public void scrollSettingsForTest(float units) {
      this.settingsRestore = -1.0F;
      this.settingsBar.target = units;
   }

   private SectionSearch pageSearch() {
      this.updateSearch();
      return this.modulePage() ? this.sectionSearch : (this.settingsPage ? this.settingsSearch : null);
   }

   public void searchForTest(String text) {
      this.updateSearch();
      this.searchBar.setText(text);
      this.updateSearch();
   }

   public String searchTextForTest() {
      return this.searchBar.text();
   }

   public boolean searchFocusedForTest() {
      return this.searchBar.isFocused();
   }

   public List<SearchResult> searchResultsForTest() {
      SectionSearch s = this.pageSearch();
      return (List<SearchResult>)(s == null ? new ArrayList<>() : s.results());
   }

   public List<String> searchRowsForTest() {
      SectionSearch s = this.pageSearch();
      return (List<String>)(s == null ? new ArrayList<>() : s.rowsForTest());
   }

   public float searchMicrosForTest() {
      SectionSearch s = this.pageSearch();
      return s == null ? 0.0F : s.lastSearchMicrosForTest();
   }

   public int searchOtherCountForTest() {
      SectionSearch s = this.pageSearch();
      return s == null ? 0 : s.otherCountForTest();
   }

   public String didYouMeanForTest() {
      SectionSearch s = this.pageSearch();
      return s == null ? null : s.didYouMeanForTest();
   }

   public int presetMatchesForTest() {
      this.updateSearch();
      return this.presetsUi == null ? -1 : this.presetsUi.matchCount();
   }

   public void keyForTest(char c, int key, boolean ctrl) {
      this.keyForTest(c, key, ctrl, false);
   }

   public void keyForTest(char c, int key, boolean ctrl, boolean shift) {
      testCtrl = ctrl;
      testShift = shift;

      try {
         this.keyTyped(c, key);
      } finally {
         testCtrl = false;
         testShift = false;
      }
   }

   public boolean closingForTest() {
      return this.closing;
   }

   public boolean clickResultForTest(String label) {
      SectionSearch s = this.pageSearch();
      Widget w = s == null ? null : s.widgetForTest(label);
      if (w == null) {
         return false;
      } else {
         float cx = w.x + w.w - 14.0F;
         float cy = w.y + Math.min(w.height() / 2.0F, 12.0F);
         this.mousePressed(cx, cy, 0);
         this.mouseReleased(cx, cy);
         return true;
      }
   }

   public boolean jumpResultForTest(String label) {
      SectionSearch s = this.pageSearch();
      Widget w = s == null ? null : s.widgetForTest(label);
      if (w == null) {
         return false;
      } else {
         float cx = w.x + w.w + 10.0F;
         float cy = w.y + Math.min(14.0F, w.height() / 2.0F);
         this.mousePressed(cx, cy, 0);
         this.mouseReleased(cx, cy);
         return true;
      }
   }

   public boolean useStyleForTest(String style) {
      SectionSearch s = this.pageSearch();
      return s != null && s.useStyleForTest(style);
   }

   public boolean openOtherForTest(int index) {
      SectionSearch s = this.pageSearch();
      return s != null && s.openOtherForTest(index);
   }

   public boolean openOtherForTest(String page) {
      SectionSearch s = this.pageSearch();
      return s != null && s.openOtherForTest(page);
   }

   public String pulseForTest() {
      if (this.pulseWidget == null) {
         return null;
      } else {
         for (Entry<Setting, Widget> e : this.widgetOf.entrySet()) {
            if (e.getValue() == this.pulseWidget) {
               return e.getKey().getName();
            }
         }

         return "?";
      }
   }

   public void densityForTest(Density.Mode mode) {
      MenuPrefs.VIEW_THEME.set(mode);
   }

   public int[] onScreenForTest() {
      int settings = 0;
      HashSet<Float> rows = new HashSet<>();

      for (int i = 0; i < this.widgets.size(); i++) {
         Widget w = this.widgets.get(i);
         float top = Math.max(this.listY, this.stickyClip[this.columnAt(w.x + 1.0F)]);
         if (this.shows(i)
            && !(w.appear < 0.99F)
            && !(w.y < top - 0.5F)
            && !(w.y + w.height() > this.listY + this.listH + 0.5F)
            && this.settingOf.containsKey(w)) {
            settings++;
            rows.add(w.y);
         }
      }

      return new int[]{settings, rows.size()};
   }

   public boolean popoverForTest(String id, boolean menu) {
      for (Entry<Setting, Widget> e : this.widgetOf.entrySet()) {
         if (e.getKey().getId().equals(id)) {
            Widget w = e.getValue();
            if (menu) {
               this.openContextMenu(e.getKey(), w, w.x + 40.0F, w.y + 10.0F);
               return true;
            }

            if (e.getKey() instanceof ColorSetting) {
               Popovers.open(new ColorPopover((ColorSetting)e.getKey(), 0), w.x, w.y, w.w, lineH(w));
               return true;
            }

            if (e.getKey() instanceof ModeSetting) {
               Popovers.open(new ChoicePopover((ModeSetting<?>)e.getKey(), 120.0F), w.x + w.w - 120.0F, w.y, 120.0F, lineH(w));
               return true;
            }
         }
      }

      return false;
   }

   public void closePopoverForTest() {
      Popovers.close();
   }

   public boolean optionsForTest(String id, boolean open) {
      for (int i = 0; i < this.widgets.size(); i++) {
         Setting s = this.settingOf.get(this.widgets.get(i));
         if (s != null && s.getId().equals(id)) {
            this.setOptionsOpen(i, open);
            return i < this.rowChildren.length && this.rowChildren[i].length > 0;
         }
      }

      return false;
   }

   public String typeSliderForTest(String id, String text) {
      for (Entry<Setting, Widget> e : this.widgetOf.entrySet()) {
         if (e.getKey().getId().equals(id) && e.getValue() instanceof SliderWidget) {
            ((SliderWidget)e.getValue()).typeForTest(text);
            return ((SliderSetting)e.getKey()).format();
         }
      }

      return null;
   }

   public void pinForTest(String id) {
      for (Setting s : this.module.getSettings()) {
         if (s.getId().equals(id)) {
            this.togglePin(s);
            return;
         }
      }
   }

   public boolean openPageForTest(String key) {
      LayoutPage target = this.layout == null ? null : this.layout.page(key);
      if (target == null) {
         return false;
      } else {
         this.settingsPage = false;
         this.presetsPage = false;
         this.globalPage = false;
         this.showPage(target, true);
         return true;
      }
   }

   public boolean backForTest() {
      return this.back();
   }

   public void previewsOpenForTest(boolean open) {
      for (Widget w : this.widgets) {
         if (w instanceof CollapsiblePreview) {
            ((CollapsiblePreview)w).setOpenForCheck(open);
         }
      }
   }

   public boolean goBackForTest() {
      return this.goBack();
   }

   public boolean backShownForTest() {
      return this.backShown;
   }

   public String subPageForTest() {
      return this.page == null ? null : this.page.key;
   }

   public String tabsForTest() {
      if (!this.tabsShown) {
         return "";
      } else {
         StringBuilder sb = new StringBuilder("mode ")
            .append(this.tabRow.mode)
            .append(", open ")
            .append(this.page != null && this.page.tabPage() != null ? this.page.tabPage().key : "-")
            .append(':');

         for (TabRow.Box b : this.tabRow.boxes) {
            Object[] objectArray = new Object[]{b.x, this.tabRow.y, b.w, this.tabRow.h, null, null};
            Object object = objectArray[4] = Float.isNaN(b.dotX) ? "" : " dot";
            objectArray[5] = b.label == null ? " icon" : (b.label.equals(b.page.tab.getName()) ? "" : " \"" + b.label + "\"");
            sb.append(' ').append(b.page.key).append(String.format(Locale.ROOT, "[%.0f %.0f %.0f %.0f%s%s]", objectArray));
         }

         if (!this.tabRow.overflow.isEmpty()) {
            sb.append(" | ⋯");

            for (LayoutPage p : this.tabRow.overflow) {
               sb.append(' ').append(p.key);
            }
         }

         return sb.toString();
      }
   }

   public float[] tabForTest(String id) {
      for (TabRow.Box b : this.tabRow.boxes) {
         if (b.page.key.equals(id)) {
            return new float[]{b.x, this.tabRow.y, b.w, this.tabRow.h, b.dotX, this.tabRow.y + this.tabRow.h / 2.0F};
         }
      }

      return null;
   }

   public String tabCountsForTest() {
      if (!this.tabsShown) {
         return "";
      } else {
         List<LayoutPage> tabs = this.layout.tabPages();
         int[] counts = this.tabCounts(tabs);
         if (counts == null) {
            return "";
         } else {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < tabs.size(); i++) {
               sb.append(i == 0 ? "" : ", ").append(tabs.get(i).key).append(' ').append(counts[i]);
            }

            return sb.toString();
         }
      }
   }

   public float searchScrollForTest() {
      return this.sectionSearch == null ? 0.0F : this.sectionSearch.bar.target;
   }

   public float[] tabMoreForTest() {
      float[] fArray;
      if (this.tabRow.moreX > -5000.0F) {
         float[] fArray2 = new float[]{this.tabRow.moreX, this.tabRow.y, this.tabRow.moreW, 0.0F};
         fArray = fArray2;
         fArray2[3] = this.tabRow.h;
      } else {
         fArray = null;
      }

      return fArray;
   }

   public void tabModeForTest(int mode) {
      this.tabRow.forcedMode = mode;
   }

   public void pressForTest(float x, float y, int button) {
      if (Popovers.isOpen()) {
         Popovers.mouseDown(x, y, button);
         Popovers.mouseUp(x, y);
      } else {
         this.mousePressed(x, y, button);
         this.mouseReleased(x, y);
      }
   }

   public List<String> shownSettingsForTest() {
      ArrayList<String> out = new ArrayList<>();

      for (int i = 0; i < this.widgets.size(); i++) {
         Setting s = this.settingOf.get(this.widgets.get(i));
         if (s != null && this.shows(i)) {
            out.add(s.getId());
         }
      }

      return out;
   }

   public String headerActionsForTest() {
      StringBuilder sb = new StringBuilder(this.actionsIconOnly ? "icons:" : "labels:");

      for (int i = 0; i < this.actionBoxes.size() && i < this.actionList.size(); i++) {
         float[] b = this.actionBoxes.get(i);
         sb.append(' ').append(this.actionList.get(i).label).append(String.format(Locale.ROOT, "[%.0f %.0f]", b[0], b[2]));
      }

      return sb.append(
            String.format(
               Locale.ROOT,
               " | title ends before %.0f",
               Math.min(this.testX > -5000.0F ? this.testX : this.resetX, this.actionBoxes.isEmpty() ? this.resetX : this.actionBoxes.get(0)[0])
            )
         )
         .toString();
   }

   public float[] switchForTest() {
      return new float[]{this.switchX, this.switchY, this.switchW, this.switchH};
   }

   public List<String> groupsForTest() {
      ArrayList<String> out = new ArrayList<>();

      for (int i = 0; i < this.widgets.size(); i++) {
         if (this.widgets.get(i) instanceof HeaderWidget && this.shows(i)) {
            out.add(((HeaderWidget)this.widgets.get(i)).titleForTest());
         }
      }

      return out;
   }

   public float listScrollForTest() {
      return this.listBar.target;
   }

   public Boolean groupFoldedForTest(String group) {
      for (Widget w : this.widgets) {
         if (w instanceof HeaderWidget && ((HeaderWidget)w).titleForTest().equalsIgnoreCase(group)) {
            return ((HeaderWidget)w).isCollapsed();
         }
      }

      return null;
   }

   private boolean typingSomewhere() {
      return this.searchBar.isFocused()
         || this.globalBar.isFocused()
         || this.focused != null && this.focused.isFocused()
         || this.presetsPage && this.presetsUi.isFocused();
   }

   private static boolean isEnter(int keyCode) {
      return keyCode == 28 || keyCode == 156;
   }

   public void handleKeyboardInput() throws IOException {
      if (!Keyboard.getEventKeyState() && this.typingKeys.remove(Keyboard.getEventKey())) {
         ModSounds.keyUp(Keyboard.getEventKey());
      }

      super.handleKeyboardInput();
   }

   protected void keyTyped(char typedChar, int keyCode) {
      ModSounds.keyEvent(keyCode);
      boolean typing = this.typingSomewhere() || typedChar > ' ' && typedChar != 127 && !this.searchBar.isFocused() && !this.globalBar.isFocused();
      int requested = ModSounds.requestCount();

      try {
         this.keyTypedInner(typedChar, keyCode);
      } finally {
         if (typing && isEnter(keyCode) && ModSounds.requestCount() == requested) {
            ModSounds.key();
         }

         if (typing && ModSounds.requestCount() != requested) {
            this.typingKeys.add(keyCode);
         }

         ModSounds.keyEvent(0);
      }
   }

   private void keyTypedInner(char typedChar, int keyCode) {
      if (this.listeningForKey) {
         this.listeningForKey = false;
         if (keyCode != 1 && keyCode != 0) {
            this.setMenuKey(keyCode);
            ModSounds.gui(Sfx.SUCCESS);
         }
      } else {
         if (GuiScreen.isCtrlKeyDown() || testCtrl) {
            if (keyCode == 11 || keyCode == 82) {
               this.zoomReset();
               return;
            }

            if (keyCode == 13 || keyCode == 78) {
               this.zoomStep(1);
               return;
            }

            if (keyCode == 12 || keyCode == 74) {
               this.zoomStep(-1);
               return;
            }
         }

         if (Popovers.isOpen()) {
            if (keyCode == 1) {
               Popovers.close();
               return;
            }

            if (Popovers.current().wantsKeys()) {
               Popovers.current().keyTyped(typedChar, keyCode);
               return;
            }

            Popovers.close();
         }

         if (this.guide == null || this.typingSomewhere() || !this.guide.key(keyCode)) {
            boolean ctrl = GuiScreen.isCtrlKeyDown() || testCtrl;
            boolean ctrlF = ctrl && keyCode == 33;
            if (!ctrlF || !GuiScreen.isShiftKeyDown() && !testShift) {
               if (this.globalBar.isFocused()) {
                  if (keyCode == 1) {
                     if (!this.globalBar.text().isEmpty()) {
                        this.globalBar.setText("");
                        if (this.sideMode == MenuPrefs.SidebarStyle.FULL) {
                           this.closeGlobal();
                        }
                     } else {
                        this.closeGlobal();
                     }
                  } else if (ctrlF) {
                     this.globalBar.focusAll();
                  } else {
                     if (!this.globalPage || !this.globalSearch.key(keyCode)) {
                        this.globalBar.key(typedChar, keyCode);
                        if (typedChar >= ' ' && typedChar != 127 || keyCode == 14) {
                           ModSounds.key();
                        }

                        this.updateSearch();
                        if (this.globalBar.text().trim().isEmpty() && this.sideMode == MenuPrefs.SidebarStyle.FULL && this.globalPage) {
                           this.closeGlobal();
                           this.globalBar.focus();
                        }
                     }
                  }
               } else if (this.searchBar.isFocused()) {
                  if (keyCode == 1) {
                     this.searchBar.setText("");
                     this.searchBar.unfocus();
                  } else if (ctrlF) {
                     this.searchBar.focusAll();
                  } else {
                     if (!this.searchKeys(keyCode)) {
                        this.searchBar.key(typedChar, keyCode);
                        if (typedChar >= ' ' && typedChar != 127 || keyCode == 14) {
                           ModSounds.key();
                        }
                     }
                  }
               } else if (this.focused != null && this.focused.isFocused()) {
                  this.focused.keyTyped(typedChar, keyCode);
                  if (!this.focused.isFocused()) {
                     this.focused = null;
                  }
               } else if (this.presetsPage && this.presetsUi.isFocused()) {
                  this.presetsUi.keyTyped(typedChar, keyCode);
               } else if (ctrl && keyCode == 15 && this.modulePage() && this.tabsShown) {
                  Popovers.close();
                  this.cycleTab(!GuiScreen.isShiftKeyDown() && !testShift ? 1 : -1);
               } else if (ctrlF) {
                  this.dropFocus();
                  if (this.globalPage) {
                     this.globalBar.focusAll();
                  } else {
                     this.searchBar.focusAll();
                  }
               } else if (keyCode == 1) {
                  if (this.globalPage) {
                     this.closeGlobal();
                  } else if (!this.searchBar.text().isEmpty()) {
                     this.searchBar.setText("");
                  } else if (!this.back()) {
                     this.close();
                  }
               } else if (Keybinds.openMenu != null && keyCode == Keybinds.openMenu.getKeyCode()) {
                  this.close();
               } else if (!this.searchKeys(keyCode)) {
                  if (typedChar > ' ' && typedChar != 127 && !ctrl) {
                     this.dropFocus();
                     SearchBar bar = this.globalPage ? this.globalBar : this.searchBar;
                     bar.focus();
                     bar.type(String.valueOf(typedChar));
                     ModSounds.key();
                  }
               }
            } else {
               if (!this.globalPage) {
                  ModSounds.gui(Sfx.SECTION);
               }

               this.openGlobal();
               this.globalBar.focusAll();
            }
         }
      }
   }

   private boolean searchKeys(int keyCode) {
      if (keyCode != 200 && keyCode != 208 && keyCode != 28 && keyCode != 156) {
         return false;
      } else if (this.globalPage) {
         return this.globalSearch.key(keyCode);
      } else {
         SectionSearch s = this.searching() ? this.sectionSearch : (this.settingsPage ? this.settingsSearch : null);
         return s != null && s.isActive() && s.key(keyCode);
      }
   }

   private static final class NavEntry {
      final Module module;
      final String category;
      final float y;
      final float h;

      NavEntry(Module module, String category, float y, float h) {
         this.module = module;
         this.category = category;
         this.y = y;
         this.h = h;
      }
   }
}
