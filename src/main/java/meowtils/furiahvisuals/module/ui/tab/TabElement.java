package meowtils.furiahvisuals.module.ui.tab;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.compat.ReflectionHelper;
import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.hud.CostMeter;
import meowtils.furiahvisuals.hud.HudGl;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.ui.LabyCompat;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.module.ui.UiElement;
import meowtils.furiahvisuals.module.ui.UiModule;
import meowtils.furiahvisuals.module.ui.scoreboard.ScoreboardElement;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.IScoreObjectiveCriteria.EnumRenderType;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldSettings.GameType;

public final class TabElement extends UiElement {
   static final Comparator<NetworkPlayerInfo> ORDER = new Comparator<NetworkPlayerInfo>() {
      public int compare(NetworkPlayerInfo a, NetworkPlayerInfo b) {
         boolean sa = a.getGameType() == GameType.SPECTATOR;
         boolean sb = b.getGameType() == GameType.SPECTATOR;
         if (sa != sb) {
            return sa ? 1 : -1;
         } else {
            ScorePlayerTeam ta = a.getPlayerTeam();
            ScorePlayerTeam tb = b.getPlayerTeam();
            int c = (ta != null ? ta.getRegisteredName() : "").compareTo(tb != null ? tb.getRegisteredName() : "");
            return c != 0 ? c : a.getGameProfile().getName().compareTo(b.getGameProfile().getName());
         }
      }
   };
   ModeSetting<ScoreboardElement.Kind> kind;
   BooleanSetting background;
   SliderSetting opacity;
   ModeSetting<HudLook.Mode> look;
   SliderSetting radius;
   BooleanSetting border;
   ModeSetting<WaterMarkModule.GlassMode> glass;
   ModeSetting<HudLook.Glint> glint;
   ModeSetting<ScoreboardElement.Font> font;
   BooleanSetting shadow;
   BooleanSetting scores;
   BooleanSetting headerFooter;
   ModeSetting<TabElement.Heads> heads;
   ModeSetting<TabElement.HeadShape> headShape;
   BooleanSetting hat;
   ModeSetting<TabElement.Ping> ping;
   BooleanSetting alternate;
   BooleanSetting highlightMe;
   BooleanSetting animate;
   private final TabRenderer renderer = new TabRenderer();
   final HudLook hudLook = new HudLook();
   private final TabRenderer.Style style = new TabRenderer.Style();
   private long lastFrame;
   private boolean drewThisFrame;
   private static Field headerField;
   private static Field footerField;
   private static boolean lookedUp;
   private TabRenderer.Frame current;
   private long readAt;
   private int readPlayers;
   private int readWidth;
   private final CostMeter gatherCost = new CostMeter();
   private final CostMeter cost = new CostMeter();
   TabPreviewWidget lastPreview;

   public TabElement(UiModule module, MiniSection section) {
      super(module, section);
   }

   @Override
   public void addSettings() {
      String top = this.section.getName();
      this.own(new TabElement.Row("tab.preview", "Preview", "The player list as it looks in game; the buttons try its animations.", new Supplier<Widget>() {
         public Widget get() {
            return new TabPreviewWidget(TabElement.this);
         }
      }).keywords(new String[]{"tab", "player list", "tablist", "preview"}), top);
      String st = "Style";
      this.kind = this.own(
         new ModeSetting<>("tab.style", "Style", "Vanilla's boxes, or the island's themed look.", ScoreboardElement.Kind.class, ScoreboardElement.Kind.THEMED)
            .keywords(new String[]{"tab", "player list", "themed"}),
         st
      );
      BooleanSupplier themed = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return TabElement.this.kind.get() == ScoreboardElement.Kind.THEMED;
         }
      };
      this.background = this.own(new BooleanSetting("tab.background", "Background", "The panels behind the list (off: text only).", true), st);
      BooleanSupplier bgOn = on(this.background);
      this.opacity = this.own(
         new SliderSetting("tab.opacity", "Background Opacity", "How solid the background is (100%: as the style has it).", 0.1, 1.0, 1.0, 0.05)
            .visibleWhen(bgOn),
         st
      );
      this.look = this.own(
         new ModeSetting<>(
               "tab.look", "Look", "The island's own look, or the list's own corner radius, border and glass.", HudLook.Mode.class, HudLook.Mode.SAME
            )
            .visibleWhen(and(themed, bgOn)),
         st
      );
      BooleanSupplier custom = and(and(themed, bgOn), new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return TabElement.this.look.get() == HudLook.Mode.CUSTOM;
         }
      });
      this.radius = this.own(new SliderSetting("tab.radius", "Corner Radius", "How round its corners are.", 0.0, 10.0, 4.0, 0.5).visibleWhen(custom), st);
      this.border = this.own(new BooleanSetting("tab.border", "Border", "A thin outline.", true).visibleWhen(custom), st);
      this.glass = this.own(
         new ModeSetting<>(
               "tab.glass",
               "Glass",
               "Frosted glass behind it (from the frame's one shared blur).",
               WaterMarkModule.GlassMode.class,
               WaterMarkModule.GlassMode.AUTO
            )
            .visibleWhen(custom),
         st
      );
      this.glint = this.own(
         new ModeSetting<>("tab.glint", "Glint", "The island's glint, in sync with it.", HudLook.Glint.class, HudLook.Glint.SAME)
            .<ModeSetting>visibleWhen(and(themed, bgOn))
            .keywords(new String[]{"shine"}),
         st
      );
      String tx = "Text";
      this.font = this.own(
         new ModeSetting<>("tab.font", "Font", "Minecraft's font or the mod's.", ScoreboardElement.Font.class, ScoreboardElement.Font.MOD)
            .keywords(new String[]{"mod font"}),
         tx
      );
      this.shadow = this.own(new BooleanSetting("tab.shadow", "Text Shadow", "A drop shadow under the names (vanilla has one).", true), tx);
      this.scores = this.own(
         new BooleanSetting("tab.scores", "Scores", "The list's numbers or hearts (a server's tab-list objective).", true)
            .keywords(new String[]{"hearts", "health", "numbers"}),
         tx
      );
      this.headerFooter = this.own(
         new BooleanSetting("tab.headerFooter", "Header and Footer", "The server's text above and below the list (Themed: strips of their own).", true)
            .keywords(new String[]{"header", "footer"}),
         tx
      );
      String he = "Player Heads";
      this.heads = this.own(
         new ModeSetting<>("tab.heads", "Player Heads", "Faces before the names.", TabElement.Heads.class, TabElement.Heads.AUTO)
            .keywords(new String[]{"faces", "skins", "player heads"}),
         he
      );
      BooleanSupplier headsOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return TabElement.this.heads.get() != TabElement.Heads.OFF;
         }
      };
      this.headShape = this.own(
         new ModeSetting<>("tab.headShape", "Head Corners", "Square or rounded.", TabElement.HeadShape.class, TabElement.HeadShape.ROUNDED)
            .visibleWhen(headsOn),
         he
      );
      this.hat = this.own(new BooleanSetting("tab.hat", "Hat Layer", "The skin's hat layer over the face.", true).visibleWhen(headsOn), he);
      this.ping = this.own(
         new ModeSetting<>("tab.ping", "Ping", "How each player's ping shows.", TabElement.Ping.class, TabElement.Ping.BARS)
            .keywords(new String[]{"latency", "ms", "connection"}),
         "Ping"
      );
      String ro = "Rows";
      this.alternate = this.own(new BooleanSetting("tab.alternate", "Alternating Shading", "Every other row a little lighter.", true).visibleWhen(themed), ro);
      this.highlightMe = this.own(
         new BooleanSetting("tab.highlightMe", "Highlight Me", "Your own row with an accent bar.", true)
            .<BooleanSetting>visibleWhen(themed)
            .keywords(new String[]{"my row", "own row"}),
         ro
      );
      this.animate = this.own(
         new BooleanSetting("tab.animate", "Animations", "Opening and closing, joins and leaves, ping and score changes.", true)
            .keywords(new String[]{"animation", "open", "close"}),
         "Animations"
      );
   }

   private static BooleanSupplier on(final BooleanSetting b) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return b.get();
         }
      };
   }

   private static BooleanSupplier and(final BooleanSupplier a, final BooleanSupplier b) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return a.getAsBoolean() && b.getAsBoolean();
         }
      };
   }

   TabRenderer.Frame gather(Minecraft mc, float screenW) {
      TabRenderer.Frame f = new TabRenderer.Frame();
      ArrayList<NetworkPlayerInfo> infos = new ArrayList<>(mc.thePlayer.sendQueue.getPlayerInfoMap());
      Collections.sort(infos, ORDER);
      Scoreboard sb = mc.theWorld.getScoreboard();
      ScoreObjective obj = sb.getObjectiveInDisplaySlot(0);
      f.scores = obj == null ? 0 : (obj.getRenderType() == EnumRenderType.HEARTS ? 2 : 1);
      boolean online = mc.isIntegratedServerRunning() || mc.getNetHandler().getNetworkManager().getIsencrypted();
      f.heads = this.heads.get() == TabElement.Heads.ALWAYS || this.heads.get() == TabElement.Heads.AUTO && online;
      GuiPlayerTabOverlay tab = mc.ingameGUI.getTabList();
      HashMap<UUID, EntityPlayer> loaded = new HashMap<>();

      for (EntityPlayer p : mc.theWorld.playerEntities) {
         loaded.put(p.getUniqueID(), p);
      }

      for (NetworkPlayerInfo info : infos) {
         GameProfile gp = info.getGameProfile();
         TabRenderer.Entry e = new TabRenderer.Entry(gp.getId() == null ? gp.getName() : gp.getId().toString());
         e.name = tab.getPlayerName(info);
         e.skin = info.getLocationSkin();
         e.ping = info.getResponseTime();
         e.spectator = info.getGameType() == GameType.SPECTATOR;
         e.score = obj != null ? sb.getValueFromObjective(gp.getName(), obj).getScorePoints() : 0;
         EntityPlayer p = gp.getId() == null ? null : loaded.get(gp.getId());
         e.hat = p == null || p.isWearing(EnumPlayerModelParts.HAT);
         e.upsideDown = p != null && p.isWearing(EnumPlayerModelParts.CAPE) && ("Dinnerbone".equals(gp.getName()) || "Grumm".equals(gp.getName()));
         e.me = gp.getId() != null && gp.getId().equals(mc.thePlayer.getUniqueID());
         f.entries.add(e);
      }

      f.header = lines(mc, field(tab, true), screenW);
      f.footer = lines(mc, field(tab, false), screenW);
      return f;
   }

   private static IChatComponent field(GuiPlayerTabOverlay tab, boolean header) {
      if (!lookedUp) {
         lookedUp = true;

         try {
            headerField = ReflectionHelper.findField(GuiPlayerTabOverlay.class, new String[]{"header", "header"});
            footerField = ReflectionHelper.findField(GuiPlayerTabOverlay.class, new String[]{"footer", "footer"});
         } catch (RuntimeException var6) {
            FuriahsVisuals.LOG.warn("UI: the tab list's header and footer can't be read here", var6);
         }
      }

      Field f = header ? headerField : footerField;
      if (f == null) {
         return null;
      } else {
         try {
            return (IChatComponent)f.get(tab);
         } catch (RuntimeException | IllegalAccessException var5) {
            return null;
         }
      }
   }

   private static List<String> lines(Minecraft mc, IChatComponent c, float screenW) {
      return c == null ? null : mc.fontRendererObj.listFormattedStringToWidth(c.getFormattedText(), Math.max(50, (int)screenW - 50));
   }

   TabRenderer.Style style(HudLook l) {
      TabRenderer.Style s = this.style;
      s.themed = this.kind.get() == ScoreboardElement.Kind.THEMED;
      s.modFont = this.font.get() == ScoreboardElement.Font.MOD;
      s.shadow = this.shadow.get();
      s.roundedHeads = this.headShape.get() == TabElement.HeadShape.ROUNDED;
      s.hat = this.hat.get();
      s.alternate = this.alternate.get();
      s.highlightMe = this.highlightMe.get();
      s.headerFooter = this.headerFooter.get();
      s.showScores = this.scores.get();
      s.ping = this.ping.get().ordinal();
      s.bgOpacity = this.opacity.getFloat();
      s.names = this.module.nametag() != null && this.module.nametag().colorsTabAndChat() ? this.module.nametag() : null;
      return s;
   }

   HudLook updateLook(HudLook l, float px, float dt) {
      WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
      l.update(this.look.get(), true, wm.lookOpacity(), this.radius.getFloat(), this.border.get(), this.glass.get(), this.glint.get(), px, dt);
      l.background = this.background.get();
      if (!l.background) {
         l.glass = false;
         l.shadow = false;
      }

      return l;
   }

   float speed() {
      return this.animate.get() ? this.module.speed() : 0.0F;
   }

   @Override
   public boolean wantsGlass() {
      return this.kind.get() == ScoreboardElement.Kind.THEMED && this.background.get() && this.hudLook.glass && this.renderer.visible();
   }

   @Override
   public void onRenderOverlay(float partialTicks) {
      this.drewThisFrame = false;
   }

   private static boolean labyModDraws() {
      return LabyCompat.customTabList();
   }

   @Override
   public String notice() {
      return labyModDraws()
         ? "LabyMod's own tab list is on, so LabyMod draws the player list and this one stays out of the way (two at once would overlap). To use this one, turn off Tab List in LabyMod's settings (Multiplayer)."
         : null;
   }

   @Override
   public boolean onOverlayElement(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      if (type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
         return false;
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.thePlayer != null && mc.theWorld != null && !labyModDraws()) {
            ScaledResolution res = new ScaledResolution(mc);
            long t0 = System.nanoTime();
            long now = ClientClock.nanos();
            int players = mc.thePlayer.sendQueue.getPlayerInfoMap().size();
            if (this.current == null || now - this.readAt > 100000000L || players != this.readPlayers || res.getScaledWidth() != this.readWidth) {
               this.current = this.gather(mc, res.getScaledWidth());
               this.readAt = now;
               this.readPlayers = players;
               this.readWidth = res.getScaledWidth();
            }

            this.gatherCost.add(System.nanoTime() - t0);
            this.draw(this.current, res);
            this.drewThisFrame = true;
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public void onRenderHud(float partialTicks) {
      if (!this.drewThisFrame && this.renderer.visible() && !labyModDraws()) {
         this.draw(null, new ScaledResolution(Minecraft.getMinecraft()));
      }
   }

   private void draw(TabRenderer.Frame frame, ScaledResolution res) {
      long t0 = System.nanoTime();
      long now = ClientClock.nanos();
      float dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
      this.lastFrame = now;
      float savedPx = Ui.px;
      float savedOffset = Ui.offsetY;
      Ui.px = res.getScaleFactor();
      Ui.offsetY = 0.0F;
      Ui.beginFrameQuick(HudGl.program());
      GlStateManager.disableAlpha();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

      try {
         HudLook l = this.updateLook(this.hudLook, Ui.px, dt);
         float speed = this.speed();
         this.renderer.update(frame, dt, speed);
         this.renderer.draw(this.style(l), l, res.getScaledWidth(), 1.0F, dt, speed);
      } catch (RuntimeException var15) {
         FuriahsVisuals.LOG.error("UI: the player list failed to draw", var15);
      } finally {
         Ui.endFrame();
         Ui.px = savedPx;
         Ui.offsetY = savedOffset;
         GlStateManager.enableAlpha();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.enableTexture2D();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.cost.add(System.nanoTime() - t0);
      }
   }

   @Override
   public void onWorldChange() {
      this.renderer.clear();
      this.current = null;
   }

   @Override
   public void applyTheme() {
      this.kind.set(ScoreboardElement.Kind.THEMED);
      this.font.set(ScoreboardElement.Font.MOD);
   }

   public void previewTestForTest(int button) {
      if (this.lastPreview != null) {
         this.lastPreview.test(button);
      }
   }

   public String stateForTest() {
      String s = String.format(
         "open %.2f, drawn %.1f × %.1f at %.1f, %.1f, cost %s (reading the list: %s)",
         this.renderer.openAmount(),
         this.renderer.lastW,
         this.renderer.lastH,
         this.renderer.lastX,
         this.renderer.lastY,
         this.cost.describe(),
         this.gatherCost.describe()
      );
      this.cost.reset();
      this.gatherCost.reset();
      return s;
   }

   public static enum HeadShape implements Displayable {
      SQUARE("Square", "Square, like vanilla"),
      ROUNDED("Rounded", "Softly rounded corners");

      private final String displayName;
      private final String description;

      private HeadShape(String displayName, String description) {
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

   public static enum Heads implements Displayable {
      AUTO("Auto", "As vanilla: heads only on online-mode servers"),
      ALWAYS("Always", "Heads everywhere (players without a skin get Steve's or Alex's)"),
      OFF("Off", "No heads");

      private final String displayName;
      private final String description;

      private Heads(String displayName, String description) {
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

   public static enum Ping implements Displayable {
      BARS("Vanilla Bars", "Signal bars (the resource pack's with the Vanilla style)"),
      NUMBER("Number", "The ping in ms: green below 80, yellow below 150, orange below 250, red above"),
      BOTH("Bars + Number", "Both"),
      HIDDEN("Hidden", "No ping");

      private final String displayName;
      private final String description;

      private Ping(String displayName, String description) {
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
}
