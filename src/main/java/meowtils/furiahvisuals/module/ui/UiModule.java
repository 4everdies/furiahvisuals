package meowtils.furiahvisuals.module.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.FuriahScreen;
import meowtils.furiahvisuals.gui.MenuNav;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.hud.CostMeter;
import meowtils.furiahvisuals.hud.HudGlint;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.MiniSections;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.ui.chat.ChatElement;
import meowtils.furiahvisuals.module.ui.crosshair.CrosshairElement;
import meowtils.furiahvisuals.module.ui.itemhud.ItemHudElement;
import meowtils.furiahvisuals.module.ui.nametag.NametagElement;
import meowtils.furiahvisuals.module.ui.scoreboard.ScoreboardElement;
import meowtils.furiahvisuals.module.ui.tab.TabElement;
import meowtils.furiahvisuals.module.watermark.IslandGlass;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.IChatComponent;

public final class UiModule extends Module {
   private final MiniSections tabs = new MiniSections(this);
   private final List<MiniSection> sections = new ArrayList<>();
   private final List<UiElement> elements = new ArrayList<>();
   private final CrosshairElement crosshairElement;
   private final ScoreboardElement scoreboardElement;
   private final TabElement tabElement;
   private final ChatElement chatElement;
   private final NametagElement nametagElement;
   private final ItemHudElement itemHudElement;
   private final MiniSection lookTab;
   private final List<Module.HeaderAction> headerActions = new ArrayList<>();
   final BooleanSetting animations;
   final SliderSetting animationSpeed;
   final ModeSetting<UiModule.GlintSync> glintSync;
   private long chatSpanStart;
   private final CostMeter chatSpan = new CostMeter();
   private long vanillaListStart;
   private final CostMeter vanillaList = new CostMeter();
   private boolean scoreboardFailed;
   private boolean chatFailed;
   private boolean nametagFailed;
   private boolean itemNameFailed;

   public UiModule() {
      super("ui", "UI", "Restyles Minecraft's own HUD: crosshair, scoreboard, tab list, chat, nametags and the hotbar.", true);
      MiniSection crosshair = this.section("crosshair", "Crosshair", IslandIcons.Icon.CROSSHAIR);
      MiniSection scoreboard = this.section("scoreboard", "Scoreboard", IslandIcons.Icon.SCOREBOARD);
      MiniSection tab = this.section("tab", "Tab", IslandIcons.Icon.TABLIST);
      MiniSection chat = this.section("chat", "Chat", IslandIcons.Icon.CHAT);
      MiniSection nametag = this.section("nametag", "Nametag", IslandIcons.Icon.NAMETAG);
      MiniSection itemHud = this.section("itemhud", "Item HUD", IslandIcons.Icon.HOTBAR);
      this.lookTab = this.tabs.add("look", "Shared look", IslandIcons.Icon.GEAR, true);
      final UiModule self = this;
      Supplier<Widget> quickButtons = new Supplier<Widget>() {
         public Widget get() {
            return new QuickButtonsWidget(self);
         }
      };
      this.add(
         new UiRow("tabs", "Mini Sections", "Crosshair · Scoreboard · Tab · Chat · Nametag · Item HUD · Shared look", quickButtons)
            .<UiRow>keywords(new String[]{"tabs", "mini sections"})
            .nested()
      );
      this.add(
         new UiRow("quick", "Quick Buttons", "Apply mod theme to all · Back to vanilla · Move HUD", quickButtons)
            .<UiRow>keywords(
               new String[]{
                  "apply mod theme to all", "back to vanilla", "reset", "theme everything", "move hud", "hud editor", "layout", "position", "drag", "place"
               }
            )
            .nested()
      );
      this.headerActions
         .add(
            new Module.HeaderAction(
               "Apply mod theme to all",
               IslandIcons.Icon.STAR,
               "Turns every mini section on, in the mod's themed style (the island's look)",
               true,
               new Runnable() {
                  @Override
                  public void run() {
                     self.applyThemeToAll();
                  }
               }
            )
         );
      this.headerActions
         .add(
            new Module.HeaderAction(
               "Back to vanilla",
               IslandIcons.Icon.BACK,
               "Turns every mini section off: the game looks exactly as vanilla (your settings stay)",
               false,
               new Runnable() {
                  @Override
                  public void run() {
                     self.backToVanilla();
                  }
               }
            )
         );
      this.crosshairElement = new CrosshairElement(this, crosshair);
      this.elements.add(this.crosshairElement);
      this.scoreboardElement = new ScoreboardElement(this, scoreboard);
      this.elements.add(this.scoreboardElement);
      this.tabElement = new TabElement(this, tab);
      this.elements.add(this.tabElement);
      this.chatElement = new ChatElement(this, chat);
      this.elements.add(this.chatElement);
      this.nametagElement = new NametagElement(this, nametag);
      this.elements.add(this.nametagElement);
      this.itemHudElement = new ItemHudElement(this, itemHud);
      this.elements.add(this.itemHudElement);

      for (UiElement e : this.elements) {
         MiniSection s = e.section;
         this.tabs
            .attachSwitch(
               s,
               this.addOwned(
                  s,
                  new BooleanSetting(s.getId() + ".enabled", "Enabled", "Restyles " + s.getName() + ". Off: it looks and works exactly as vanilla.", false)
                     .group(s.getName())
               )
            );
         e.addSettings();
      }

      this.animations = this.addOwned(
         this.lookTab,
         new BooleanSetting("animations", "UI Animations", "Smooth springs for every element of the segment; off, changes show at once.", true)
            .<BooleanSetting>group("Whole Segment")
            .keywords(new String[]{"animation", "motion", "smooth"})
      );
      this.animationSpeed = this.addOwned(
         this.lookTab,
         new SliderSetting("animationSpeed", "Animation Speed", "How fast the segment's animations play.", 0.5, 2.0, 1.0, 0.05)
            .unit("×")
            .<SliderSetting>group("Whole Segment")
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return UiModule.this.animations.get();
               }
            })
      );
      this.glintSync = this.addOwned(
         this.lookTab,
         new ModeSetting<>(
               "glintSync",
               "Glint Sync",
               "How the themed elements' glint (the island's glint settings) lines up: all at once, or one wave across the screen.",
               UiModule.GlintSync.class,
               UiModule.GlintSync.TOGETHER
            )
            .<ModeSetting>group("Whole Segment")
            .keywords(new String[]{"shine", "sweep", "wave", "together"})
      );
      this.addOwned(
         this.lookTab,
         new UiRow(
               "lookPreview",
               "Look",
               "The shared themed look, Same as WaterMark: the island's panel, glass and glint, and the mod font.",
               new Supplier<Widget>() {
                  public Widget get() {
                     return new LookPreviewWidget(self);
                  }
               }
            )
            .<UiRow>group("Whole Segment")
            .keywords(new String[]{"preview", "theme", "font", "same as watermark", "glass", "shared look"})
      );
   }

   private MiniSection section(String id, String name, IslandIcons.Icon icon) {
      MiniSection s = this.tabs.add(id, name, icon);
      this.sections.add(s);
      return s;
   }

   public <S extends Setting> S addOwned(MiniSection s, S setting) {
      return this.add(setting.ownedBy(s));
   }

   @Override
   public MiniSections getTabs() {
      return this.tabs;
   }

   @Override
   public List<Module.HeaderAction> headerActions() {
      return this.headerActions;
   }

   List<MiniSection> sections() {
      return Collections.unmodifiableList(this.sections);
   }

   MiniSection current() {
      return this.tabs.current();
   }

   void applyThemeToAll() {
      for (UiElement e : this.elements) {
         e.applyTheme();
         e.section.getSwitch().set(true);
      }

      if (!this.isEnabled()) {
         this.setEnabled(true);
      }

      ModSounds.gui(Sfx.SUCCESS);
   }

   public void openMoveHud() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.theWorld != null) {
         GuiScreen current = mc.currentScreen;
         boolean fromMenu = current instanceof FuriahScreen;
         GuiScreen menuParent = fromMenu ? ((FuriahScreen)current).parentScreen() : null;
         mc.displayGuiScreen(new MoveHudScreen(this, fromMenu, menuParent));
      }
   }

   void backToVanilla() {
      for (UiElement e : this.elements) {
         e.section.getSwitch().set(false);
      }

      ModSounds.gui(Sfx.TOGGLE_OFF);
   }

   @Override
   public String summary() {
      return this.onCount() + " of " + this.sections.size() + " restyled";
   }

   @Override
   public boolean hasMoveHud() {
      return true;
   }

   int onCount() {
      return this.tabs.onCount()[0];
   }

   public float speed() {
      return this.animations.get() ? this.animationSpeed.getFloat() : 0.0F;
   }

   @Override
   public void onRenderOverlay(float partialTicks) {
      HudGlint.sync = this.glintSync.get() == UiModule.GlintSync.WAVE ? HudGlint.Sync.WAVE : HudGlint.Sync.TOGETHER;
      boolean glass = false;

      for (UiElement e : this.elements) {
         if (e.on() && e.wantsGlass()) {
            glass = true;
            break;
         }
      }

      if (glass) {
         IslandGlass.captureScreen();
      }

      for (UiElement ex : this.elements) {
         if (ex.on()) {
            ex.onRenderOverlay(partialTicks);
         }
      }
   }

   @Override
   public boolean onOverlayElement(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      for (UiElement e : this.elements) {
         if (e.on() && e.onOverlayElement(type, partialTicks)) {
            return true;
         }
      }

      if (type == RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
         this.vanillaListStart = System.nanoTime();
      } else if (type == RenderGameOverlayEvent.ElementType.CHAT) {
         this.chatSpanStart = System.nanoTime();
      }

      return false;
   }

   public String chatCostForTest() {
      String s = this.chatSpan.describe();
      this.chatSpan.reset();
      return s;
   }

   public String vanillaListCostForTest() {
      String s = this.vanillaList.describe();
      this.vanillaList.reset();
      return s;
   }

   @Override
   public void onOverlayElementPost(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      if (type == RenderGameOverlayEvent.ElementType.PLAYER_LIST && this.vanillaListStart != 0L) {
         this.vanillaList.add(System.nanoTime() - this.vanillaListStart);
         this.vanillaListStart = 0L;
      } else if (type == RenderGameOverlayEvent.ElementType.CHAT && this.chatSpanStart != 0L) {
         this.chatSpan.add(System.nanoTime() - this.chatSpanStart);
         this.chatSpanStart = 0L;
      }

      for (UiElement e : this.elements) {
         if (e.on()) {
            e.onOverlayElementPost(type, partialTicks);
         }
      }
   }

   @Override
   public void onAttack(EntityPlayer attacker, Entity target) {
      for (UiElement e : this.elements) {
         if (e.on()) {
            e.onAttack(attacker, target);
         }
      }
   }

   @Override
   public void onRenderHud(float partialTicks) {
      for (UiElement e : this.elements) {
         if (e.on()) {
            e.onRenderHud(partialTicks);
         }
      }
   }

   @Override
   public void onTick() {
      for (UiElement e : this.elements) {
         if (e.on()) {
            e.onTick();
         }
      }
   }

   @Override
   public void onWorldChange() {
      for (UiElement e : this.elements) {
         e.onWorldChange();
      }
   }

   @Override
   public void onClientTick() {
      for (UiElement e : this.elements) {
         e.onClientTick();
      }
   }

   @Override
   public boolean afterLoad(JsonObject json) {
      boolean reshaped = false;

      for (UiElement e : this.elements) {
         e.afterLoad();
         reshaped |= e.reshapedOnLoad();
      }

      return reshaped;
   }

   @Override
   protected void onDisable() {
      HudGlint.sync = HudGlint.Sync.TOGETHER;
   }

   @Override
   public String getLoadWarning() {
      StringBuilder sb = null;

      for (UiElement e : this.elements) {
         String w = e.loadWarning();
         if (w != null) {
            sb = sb == null ? new StringBuilder(w) : sb.append(' ').append(w);
         }
      }

      return sb == null ? null : sb.toString();
   }

   @Override
   public int getAccentColor(double time) {
      return FuriahsVisuals.instance.getWaterMark().lookAccent(time);
   }

   public void nextCrosshairProfile() {
      this.crosshairElement.nextProfile();
   }

   boolean scoreboardHook(ScoreObjective objective, ScaledResolution res) {
      if (this.isEnabled() && this.scoreboardElement.on() && !this.scoreboardFailed) {
         try {
            return this.scoreboardElement.drawGame(objective, res);
         } catch (RuntimeException | LinkageError var4) {
            FuriahsVisuals.LOG.error("UI: the scoreboard failed; it is vanilla until the game restarts", var4);
            this.scoreboardFailed = true;
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean chatLive() {
      return this.isEnabled() && !this.chatFailed && this.chatElement.active();
   }

   private void chatFailed(String what, Throwable e) {
      FuriahsVisuals.LOG.error("UI: the chat failed ({}); it is vanilla until the game restarts", new Object[]{what, e});
      this.chatFailed = true;
      this.chatElement.fail();
   }

   boolean chatDrawHook(GuiNewChat chat, int updateCounter) {
      if (!this.chatLive()) {
         return false;
      } else {
         try {
            return this.chatElement.draw(chat, updateCounter);
         } catch (RuntimeException | LinkageError var4) {
            this.chatFailed("drawing", var4);
            return false;
         }
      }
   }

   boolean chatAnswersHits() {
      return this.chatLive() && this.chatElement.answersHits();
   }

   IChatComponent chatHitHook(GuiNewChat chat, int mouseX, int mouseY) {
      try {
         return this.chatElement.hit(chat, mouseX, mouseY);
      } catch (RuntimeException | LinkageError var5) {
         this.chatFailed("finding what is under the mouse", var5);
         return null;
      }
   }

   void chatScrollHook(int amount, boolean reset) {
      if (this.chatLive()) {
         try {
            if (reset) {
               this.chatElement.resetScroll();
            } else {
               this.chatElement.scroll(amount);
            }
         } catch (RuntimeException | LinkageError var4) {
            this.chatFailed("scrolling", var4);
         }
      }
   }

   GuiTextField chatInitHook(GuiChat screen, GuiTextField field, int width, int height) {
      if (!this.chatLive()) {
         return null;
      } else {
         try {
            return this.chatElement.initInput(screen, field, width, height);
         } catch (RuntimeException | LinkageError var6) {
            this.chatFailed("putting the typing bar in the chat box", var6);
            return null;
         }
      }
   }

   GuiTextField chatInputHook(GuiChat screen, GuiTextField field, int width, int height, List<GuiButton> buttons) {
      if (!this.chatLive()) {
         return null;
      } else {
         try {
            return this.chatElement.inputStart(screen, field, width, height, buttons);
         } catch (RuntimeException | LinkageError var7) {
            this.chatFailed("drawing the typing bar", var7);
            return null;
         }
      }
   }

   int chatInputBackgroundHook(GuiChat screen, GuiTextField field, int color) {
      if (!this.chatLive()) {
         return color;
      } else {
         try {
            return this.chatElement.inputBackground(screen, field, color);
         } catch (RuntimeException | LinkageError var5) {
            this.chatFailed("drawing the input line", var5);
            return color;
         }
      }
   }

   void chatTextHook(GuiChat screen, GuiTextField field, boolean before) {
      if (!this.chatLive()) {
         if (!before) {
            this.chatElement.abortInput();
         }
      } else {
         try {
            this.chatElement.inputText(screen, field, before);
         } catch (RuntimeException | LinkageError var5) {
            this.chatFailed("drawing the caret", var5);
         }
      }
   }

   @Override
   public boolean onScreenMouse(GuiScreen screen) {
      if (!this.chatLive()) {
         return false;
      } else {
         try {
            return this.chatElement.screenMouse(screen);
         } catch (RuntimeException | LinkageError var3) {
            this.chatFailed("the scrollbar", var3);
            return false;
         }
      }
   }

   public ChatElement chat() {
      return this.chatElement;
   }

   private boolean nametagLive() {
      return this.isEnabled() && !this.nametagFailed && this.nametagElement.active();
   }

   private void nametagFailed(Throwable e) {
      FuriahsVisuals.LOG.error("UI: nametags failed; they are vanilla until the game restarts", e);
      this.nametagFailed = true;
      this.nametagElement.fail();
   }

   boolean nametagHook(Render<?> render, Entity entity, String str, double x, double y, double z, int maxDistance) {
      if (!this.nametagLive()) {
         return false;
      } else {
         try {
            return this.nametagElement.label(render, entity, str, x, y, z, maxDistance);
         } catch (RuntimeException | LinkageError var12) {
            this.nametagFailed(var12);
            return false;
         }
      }
   }

   boolean nametagSneakingHook(RendererLivingEntity<?> render, EntityLivingBase entity, double x, double y, double z) {
      if (!this.nametagLive()) {
         return false;
      } else {
         try {
            return this.nametagElement.sneakingLabel(render, entity, x, y, z);
         } catch (RuntimeException | LinkageError var10) {
            this.nametagFailed(var10);
            return false;
         }
      }
   }

   Boolean nametagOwnHook(EntityLivingBase entity) {
      if (!this.nametagLive()) {
         return null;
      } else {
         try {
            return this.nametagElement.canRenderName(entity);
         } catch (RuntimeException | LinkageError var3) {
            this.nametagFailed(var3);
            return null;
         }
      }
   }

   public NametagElement nametag() {
      return this.nametagElement;
   }

   boolean itemNameHook(ScaledResolution res) {
      if (this.isEnabled() && !this.itemNameFailed && this.itemHudElement.on()) {
         try {
            return this.itemHudElement.itemName(res);
         } catch (RuntimeException | LinkageError var3) {
            FuriahsVisuals.LOG.error("UI: the item name failed; it is vanilla's until the game restarts", var3);
            this.itemNameFailed = true;
            return false;
         }
      } else {
         return false;
      }
   }

   float itemHudLift() {
      if (this.isEnabled() && this.itemHudElement.on()) {
         try {
            return this.itemHudElement.extraHeight();
         } catch (RuntimeException var2) {
            return 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   public boolean chatMovable() {
      return this.chatLive();
   }

   public ItemHudElement itemHud() {
      return this.itemHudElement;
   }

   public ScoreboardElement scoreboard() {
      return this.scoreboardElement;
   }

   public TabElement tab() {
      return this.tabElement;
   }

   @Override
   public Module.Notice getNotice() {
      for (UiElement e : this.elements) {
         String n;
         if (e.section() == this.tabs.current() && (n = e.notice()) != null) {
            return new Module.Notice(n, false);
         }
      }

      return null;
   }

   public CrosshairElement crosshair() {
      return this.crosshairElement;
   }

   public String currentTabId() {
      return this.tabs.current() == null ? null : this.tabs.current().getId();
   }

   public void selectForTest(String id) {
      MiniSection s = this.tabs.byId(id);
      if (s == null) {
         throw new IllegalArgumentException("no mini section " + id);
      } else {
         this.tabs.select(s);
         MenuNav.openPage(this, id);
      }
   }

   public void setForTest(String id, String value) {
      Object v = value.startsWith("{")
         ? new JsonParser().parse(value)
         : (
            !value.equals("true") && !value.equals("false")
               ? (value.matches("-?[0-9.]+") ? new JsonPrimitive(Double.parseDouble(value)) : new JsonPrimitive(value))
               : new JsonPrimitive(Boolean.parseBoolean(value))
         );

      for (Setting s : this.getSettings()) {
         if (s.getId().equals(id)) {
            s.fromJson((JsonElement)v);
            return;
         }
      }

      throw new IllegalArgumentException("no setting " + id);
   }

   public void quickForTest(boolean apply) {
      if (apply) {
         this.applyThemeToAll();
      } else {
         this.backToVanilla();
      }
   }

   public String stateForTest() {
      StringBuilder sb = new StringBuilder("tab ").append(this.currentTabId()).append(", on:");

      for (MiniSection s : this.sections) {
         if (s.isActive()) {
            sb.append(' ').append(s.getId());
         }
      }

      return sb.append(this.isEnabled() ? "" : " (segment off)").toString();
   }

   public static enum GlintSync implements Displayable {
      TOGETHER("Together", "Everything shines at the same moment, in time with the island"),
      WAVE("Wave", "One sweep crosses the whole screen, over the island and each element in turn");

      private final String displayName;
      private final String description;

      private GlintSync(String displayName, String description) {
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
