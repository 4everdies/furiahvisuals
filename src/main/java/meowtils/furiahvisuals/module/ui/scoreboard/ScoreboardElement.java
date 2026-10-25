package meowtils.furiahvisuals.module.ui.scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.hud.HudEditor;
import meowtils.furiahvisuals.hud.HudGl;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.hud.HudPosition;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.ui.LabyCompat;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.module.ui.UiElement;
import meowtils.furiahvisuals.module.ui.UiHooks;
import meowtils.furiahvisuals.module.ui.UiModule;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.TextSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

public final class ScoreboardElement extends UiElement {
   static final String SAMPLE_TITLE = "§e§lFURIAH GAMES";
   static final String[][] SAMPLE = new String[][]{
      {"§701/10/26 §8m12A", "9"},
      {" ", "8"},
      {"Kills: §a12", "7"},
      {"Deaths: §c3", "6"},
      {"  ", "5"},
      {"Coins: §6340", "4"},
      {"Map: §bGlacier", "3"},
      {"   ", "2"},
      {"§ewww.server.net", "1"}
   };
   ModeSetting<ScoreboardElement.Kind> kind;
   BooleanSetting background;
   SliderSetting opacity;
   ModeSetting<HudLook.Mode> look;
   SliderSetting radius;
   BooleanSetting border;
   ModeSetting<WaterMarkModule.GlassMode> glass;
   ModeSetting<HudLook.Glint> glint;
   BooleanSetting showTitle;
   BooleanSetting titleStrip;
   BooleanSetting titleUnderline;
   ModeSetting<ScoreboardElement.TitleAlign> titleAlign;
   ModeSetting<ScoreboardElement.Font> font;
   BooleanSetting shadow;
   BooleanSetting numbers;
   ModeSetting<ScoreboardElement.NumberColor> numberColor;
   ColorSetting customNumberColor;
   BooleanSetting allServers;
   BooleanSetting hideBlank;
   BooleanSetting hideAddress;
   TextSetting hideContaining;
   BooleanSetting animate;
   SliderSetting scale;
   SliderSetting padding;
   SliderSetting spacing;
   ModeSetting<ScoreboardElement.Align> align;
   HudPosition position;
   ScoreboardRules rules;
   private final ScoreboardRenderer renderer = new ScoreboardRenderer();
   final HudLook hudLook = new HudLook();
   private final ScoreboardRenderer.Style style = new ScoreboardRenderer.Style();
   private long lastFrame;
   private boolean drewThisFrame;
   private Boolean hookIn;
   private boolean showingSample;
   private List<ScoreboardRenderer.Entry> sample;
   private float costMs = -1.0F;
   private float lastK = 1.0F;
   ScoreboardPreviewWidget lastPreview;

   public ScoreboardElement(UiModule module, MiniSection section) {
      super(module, section);
   }

   @Override
   public void addSettings() {
      String top = this.section.getName();
      this.own(
         new ScoreboardElement.Row(
               "scoreboard.preview", "Preview", "The scoreboard as it looks in game; the buttons try its animations.", new Supplier<Widget>() {
                  public Widget get() {
                     return new ScoreboardPreviewWidget(ScoreboardElement.this);
                  }
               }
            )
            .keywords(new String[]{"scoreboard", "sidebar", "preview"}),
         top
      );
      String st = "Style";
      this.kind = this.own(
         new ModeSetting<>(
               "scoreboard.style", "Style", "Vanilla's boxes, or the island's themed look.", ScoreboardElement.Kind.class, ScoreboardElement.Kind.THEMED
            )
            .keywords(new String[]{"scoreboard", "sidebar", "themed"}),
         st
      );
      BooleanSupplier themed = is(this.kind, ScoreboardElement.Kind.THEMED);
      this.background = this.own(
         new BooleanSetting("scoreboard.background", "Background", "The panel behind the lines (off: text only).", true)
            .keywords(new String[]{"scoreboard", "panel"}),
         st
      );
      BooleanSupplier bgOn = on(this.background);
      this.opacity = this.own(
         new SliderSetting("scoreboard.opacity", "Background Opacity", "How solid the background is (100%: as the style has it).", 0.1, 1.0, 1.0, 0.05)
            .<SliderSetting>visibleWhen(bgOn)
            .keywords(new String[]{"scoreboard", "transparency"}),
         st
      );
      this.look = this.own(
         new ModeSetting<>(
               "scoreboard.look",
               "Look",
               "The island's own look, or the scoreboard's own corner radius, border and glass.",
               HudLook.Mode.class,
               HudLook.Mode.SAME
            )
            .visibleWhen(and(themed, bgOn)),
         st
      );
      BooleanSupplier custom = and(and(themed, bgOn), is(this.look, HudLook.Mode.CUSTOM));
      this.radius = this.own(new SliderSetting("scoreboard.radius", "Corner Radius", "How round its corners are.", 0.0, 10.0, 4.0, 0.5).visibleWhen(custom), st);
      this.border = this.own(new BooleanSetting("scoreboard.border", "Border", "A thin outline.", true).visibleWhen(custom), st);
      this.glass = this.own(
         new ModeSetting<>(
               "scoreboard.glass",
               "Glass",
               "Frosted glass behind it (from the frame's one shared blur).",
               WaterMarkModule.GlassMode.class,
               WaterMarkModule.GlassMode.AUTO
            )
            .visibleWhen(custom),
         st
      );
      this.glint = this.own(
         new ModeSetting<>(
               "scoreboard.glint", "Glint", "The island's glint, in sync with it (Whole Segment: Together or Wave).", HudLook.Glint.class, HudLook.Glint.SAME
            )
            .<ModeSetting>visibleWhen(and(themed, bgOn))
            .keywords(new String[]{"shine", "sweep"}),
         st
      );
      String ti = "Title";
      this.showTitle = this.own(new BooleanSetting("scoreboard.title", "Show Title", "The scoreboard's title above its lines.", true), ti);
      BooleanSupplier titleOn = on(this.showTitle);
      this.titleStrip = this.own(
         new BooleanSetting("scoreboard.titleStrip", "Header Strip", "The title on a strip of its own.", true).visibleWhen(and(themed, titleOn)), ti
      );
      this.titleUnderline = this.own(
         new BooleanSetting("scoreboard.titleUnderline", "Accent Underline", "A thin accent line under the title.", true).visibleWhen(and(themed, titleOn)), ti
      );
      this.titleAlign = this.own(
         new ModeSetting<>(
               "scoreboard.titleAlign", "Title Alignment", "Where the title sits.", ScoreboardElement.TitleAlign.class, ScoreboardElement.TitleAlign.CENTER
            )
            .visibleWhen(and(themed, titleOn)),
         ti
      );
      String tx = "Text";
      this.font = this.own(
         new ModeSetting<>("scoreboard.font", "Font", "Minecraft's font or the mod's.", ScoreboardElement.Font.class, ScoreboardElement.Font.MOD)
            .keywords(new String[]{"mod font", "font"}),
         tx
      );
      this.shadow = this.own(new BooleanSetting("scoreboard.shadow", "Text Shadow", "A drop shadow under the text.", false), tx);
      this.numbers = this.own(
         new BooleanSetting("scoreboard.numbers", "Red Numbers", "The scores at the right.", true).keywords(new String[]{"scores", "numbers", "red numbers"}),
         tx
      );
      BooleanSupplier numbersOn = on(this.numbers);
      this.numberColor = this.own(
         new ModeSetting<>(
               "scoreboard.numberColor", "Number Color", "The numbers' color.", ScoreboardElement.NumberColor.class, ScoreboardElement.NumberColor.RED
            )
            .visibleWhen(numbersOn),
         tx
      );
      this.customNumberColor = this.own(
         new ColorSetting("scoreboard.customNumberColor", "Custom Number Color", "The numbers' own color.", ColorSetting.Mode.STATIC, -43691, -43691, 1.0)
            .skipSetAll()
            .visibleWhen(and(numbersOn, is(this.numberColor, ScoreboardElement.NumberColor.CUSTOM))),
         tx
      );
      String hl = "Hide Lines";
      this.own(
         new ScoreboardElement.Row(
               "scoreboard.lines", "Lines", "The scoreboard now: click a line's eye to hide it (or show it again).", new Supplier<Widget>() {
                  public Widget get() {
                     return new ScoreboardLinesWidget(ScoreboardElement.this);
                  }
               }
            )
            .keywords(new String[]{"hide lines", "hide", "lines", "eye", "rules"}),
         hl
      );
      this.allServers = this.own(
         new BooleanSetting(
               "scoreboard.allServers",
               "Hide on All Servers",
               "Lines you hide from now on are hidden on every server (off: only on this server, for this scoreboard's title).",
               false
            )
            .keywords(new String[]{"per server", "global"}),
         hl
      );
      this.hideBlank = this.own(
         new BooleanSetting("scoreboard.hideBlank", "Hide Blank Lines", "The empty spacer lines between groups.", false)
            .keywords(new String[]{"spacer", "empty"}),
         hl
      );
      this.hideAddress = this.own(
         new BooleanSetting("scoreboard.hideAddress", "Hide Server Address", "The line that is only the server's address (www.server.net).", false)
            .keywords(new String[]{"ip", "website", "domain"}),
         hl
      );
      this.hideContaining = this.own(
         new TextSetting(
               "scoreboard.hideContaining", "Hide Lines Containing", "Any of these words or bits of text, separated by commas (case doesn't matter).", "", 120
            )
            .keywords(new String[]{"filter", "contains"}),
         hl
      );
      this.animate = this.own(
         new BooleanSetting(
               "scoreboard.animate", "Animations", "Numbers roll, changed text crossfades, lines slide in and out, the panel resizes smoothly.", true
            )
            .keywords(new String[]{"animation", "roll"}),
         "Animations"
      );
      String la = "Layout";
      this.scale = this.own(new SliderSetting("scoreboard.scale", "Scale", "How big it is.", 0.5, 2.0, 1.0, 0.05).unit("×").keywords(new String[]{"size"}), la);
      this.padding = this.own(new SliderSetting("scoreboard.padding", "Padding", "Room around the lines.", 0.0, 8.0, 3.0, 0.5).visibleWhen(themed), la);
      this.spacing = this.own(new SliderSetting("scoreboard.spacing", "Line Spacing", "Extra room between lines.", 0.0, 6.0, 0.0, 0.5), la);
      this.align = this.own(
         new ModeSetting<>("scoreboard.align", "Text Alignment", "Where the lines' text sits.", ScoreboardElement.Align.class, ScoreboardElement.Align.LEFT),
         la
      );
      this.position = this.own(new HudPosition("scoreboard.position", "Scoreboard Position"), la);
      this.rules = this.own(new ScoreboardRules("scoreboard.rules"), hl);
   }

   private static BooleanSupplier on(final BooleanSetting b) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return b.get();
         }
      };
   }

   private static <E extends Enum<E>> BooleanSupplier is(final ModeSetting<E> m, final E value) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return m.get() == value;
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

   boolean hookIn() {
      if (this.hookIn == null) {
         this.hookIn = UiHooks.installed("scoreboard", GuiIngame.class);
         if (!this.hookIn) {
            FuriahsVisuals.LOG.warn("UI: the scoreboard hook is missing (another mod changed GuiIngame.renderScoreboard); the scoreboard stays vanilla");
         }
      }

      return this.hookIn;
   }

   @Override
   public String notice() {
      if (!this.hookIn()) {
         return "The scoreboard can't be restyled here: another mod changed how Minecraft draws it, so it stays vanilla.";
      } else {
         return labyModDraws()
            ? "LabyMod's Scoreboard widget is on, so LabyMod draws the scoreboard and this one stays out of the way (two at once would overlap). To use this one, turn off the Scoreboard widget in LabyMod's widget editor."
            : null;
      }
   }

   public static boolean labyModDraws() {
      return LabyCompat.scoreboardWidget();
   }

   boolean hidden(String scope, String text) {
      String n = ScoreboardRules.normalize(text);
      if (this.hideBlank.get() && n.isEmpty()) {
         return true;
      } else if (this.hideAddress.get() && ScoreboardRules.isAddress(n)) {
         return true;
      } else {
         String words = this.hideContaining.get().trim();
         if (!words.isEmpty()) {
            String lower = n.toLowerCase(Locale.ROOT);

            for (String w : words.split(",")) {
               String t = w.trim().toLowerCase(Locale.ROOT);
               if (!t.isEmpty() && lower.contains(t)) {
                  return true;
               }
            }
         }

         return this.rules.hidden(scope, n);
      }
   }

   boolean hiddenByQuickRule(String text) {
      String n = ScoreboardRules.normalize(text);
      if (this.hideBlank.get() && n.isEmpty()) {
         return true;
      } else if (this.hideAddress.get() && ScoreboardRules.isAddress(n)) {
         return true;
      } else {
         String words = this.hideContaining.get().trim();
         if (!words.isEmpty()) {
            String lower = n.toLowerCase(Locale.ROOT);

            for (String w : words.split(",")) {
               String t = w.trim().toLowerCase(Locale.ROOT);
               if (!t.isEmpty() && lower.contains(t)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   String scopeFor(String title) {
      return this.allServers.get() ? "*" : ScoreboardRules.scope(title);
   }

   static ScoreObjective currentObjective() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.theWorld != null && mc.thePlayer != null) {
         Scoreboard sb = mc.theWorld.getScoreboard();
         ScoreObjective objective = null;
         ScorePlayerTeam team = sb.getPlayersTeam(mc.thePlayer.getName());
         int slot;
         if (team != null && (slot = team.getChatFormat().getColorIndex()) >= 0) {
            objective = sb.getObjectiveInDisplaySlot(3 + slot);
         }

         return objective != null ? objective : sb.getObjectiveInDisplaySlot(1);
      } else {
         return null;
      }
   }

   static List<String[]> linesOf(ScoreObjective objective) {
      Scoreboard scoreboard = objective.getScoreboard();
      Collection<Score> collection = scoreboard.getSortedScores(objective);
      ArrayList<Score> list = new ArrayList<>();

      for (Score s : collection) {
         if (s.getPlayerName() != null && !s.getPlayerName().startsWith("#")) {
            list.add(s);
         }
      }

      ArrayList<Score> shown = list;
      if (list.size() > 15) {
         int skip = Math.max(0, collection.size() - 15);
         shown = new ArrayList<>(list.subList(Math.min(skip, list.size()), list.size()));
      }

      ArrayList<String[]> out = new ArrayList<>();

      for (int i = shown.size() - 1; i >= 0; i--) {
         Score sx = shown.get(i);
         ScorePlayerTeam team = scoreboard.getPlayersTeam(sx.getPlayerName());
         out.add(new String[]{ScorePlayerTeam.formatPlayerName(team, sx.getPlayerName()), Integer.toString(sx.getScorePoints()), sx.getPlayerName()});
      }

      return out;
   }

   ScoreboardRenderer.Style style(HudLook l) {
      ScoreboardRenderer.Style s = this.style;
      s.themed = this.kind.get() == ScoreboardElement.Kind.THEMED;
      s.modFont = this.font.get() == ScoreboardElement.Font.MOD;
      s.shadow = this.shadow.get();
      s.showTitle = this.showTitle.get();
      s.strip = this.titleStrip.get();
      s.underline = this.titleUnderline.get();
      s.titleCentered = this.titleAlign.get() == ScoreboardElement.TitleAlign.CENTER;
      s.showScores = this.numbers.get();
      s.scoreColor = this.numberColor.get() == ScoreboardElement.NumberColor.ACCENT
         ? l.colors.accent | 0xFF000000
         : (this.numberColor.get() == ScoreboardElement.NumberColor.CUSTOM ? this.customNumberColor.getColor(0.5F) | 0xFF000000 : -43691);
      s.pad = this.padding.getFloat();
      s.spacing = this.spacing.getFloat();
      s.scale = this.scale.getFloat();
      s.align = this.align.get().ordinal();
      s.animate = this.animate.get();
      s.bgOpacity = this.opacity.getFloat();
      return s;
   }

   HudLook updateLook(float px, float dt) {
      return this.updateLook(this.hudLook, px, dt);
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

   public boolean drawGame(ScoreObjective objective, ScaledResolution res) {
      if (this.hookIn() && !labyModDraws()) {
         String title = objective.getDisplayName();
         String scope = ScoreboardRules.scope(title);
         ArrayList<ScoreboardRenderer.Entry> entries = new ArrayList<>();

         for (String[] line : linesOf(objective)) {
            if (!this.hidden(scope, line[0])) {
               entries.add(new ScoreboardRenderer.Entry(line[2], line[0], Integer.parseInt(line[1])));
            }
         }

         this.draw(title, entries, res);
         this.drewThisFrame = true;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean onOverlayElement(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      if (type == RenderGameOverlayEvent.ElementType.CHAT && !this.drewThisFrame && this.hookIn() && !labyModDraws()) {
         this.showingSample = HudEditor.isOpen();
         if (this.showingSample) {
            this.draw("§e§lFURIAH GAMES", this.sampleEntries(), new ScaledResolution(Minecraft.getMinecraft()));
         } else if (this.renderer.visible()) {
            this.draw(null, null, new ScaledResolution(Minecraft.getMinecraft()));
         }
      } else if (type == RenderGameOverlayEvent.ElementType.CHAT) {
         this.showingSample = false;
      }

      return false;
   }

   private List<ScoreboardRenderer.Entry> sampleEntries() {
      if (this.sample == null) {
         this.sample = new ArrayList<>();

         for (int i = 0; i < SAMPLE.length; i++) {
            this.sample.add(new ScoreboardRenderer.Entry("\u0000sample" + i, SAMPLE[i][0], Integer.parseInt(SAMPLE[i][1])));
         }
      }

      return this.sample;
   }

   private void draw(String title, List<ScoreboardRenderer.Entry> entries, ScaledResolution res) {
      long t0 = System.nanoTime();

      try {
         this.drawTimed(title, entries, res, ClientClock.nanos());
      } catch (Throwable var8) {
         float ms = (float)(System.nanoTime() - t0) / 1000000.0F;
         this.costMs = this.costMs < 0.0F ? ms : this.costMs + (ms - this.costMs) * 0.05F;
         throw var8;
      }

      float ms = (float)(System.nanoTime() - t0) / 1000000.0F;
      this.costMs = this.costMs < 0.0F ? ms : this.costMs + (ms - this.costMs) * 0.05F;
   }

   private void drawTimed(String title, List<ScoreboardRenderer.Entry> entries, ScaledResolution res, long now) {
      float dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
      this.lastFrame = now;
      float k = this.scale.getFloat();
      float sf = res.getScaleFactor();
      float savedPx = Ui.px;
      float savedOffset = Ui.offsetY;
      boolean overItems = this.position.moved();
      if (overItems) {
         GlStateManager.disableDepth();
      }

      GlStateManager.pushMatrix();
      GlStateManager.scale(k, k, 1.0F);
      Ui.px = sf * k;
      Ui.offsetY = 0.0F;
      Ui.beginFrameQuick(HudGl.program());
      GlStateManager.disableAlpha();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

      try {
         HudLook l = this.updateLook(Ui.px, dt);
         ScoreboardRenderer.Style s = this.style(l);
         this.renderer.update(title == null ? "" : title, entries, dt, this.speed());
         this.renderer.avoid(HudEditor.dragging(this) ? null : FuriahsVisuals.instance.getWaterMark().islandBounds(), k);
         this.renderer.draw(s, l, res.getScaledWidth() / k, res.getScaledHeight() / k, this.position, k, this.opacityFor(s), dt, this.speed());
         this.lastK = k;
      } catch (RuntimeException var17) {
         FuriahsVisuals.LOG.error("UI: the scoreboard failed to draw", var17);
      } finally {
         Ui.endFrame();
         Ui.px = savedPx;
         Ui.offsetY = savedOffset;
         GlStateManager.popMatrix();
         GlStateManager.enableAlpha();
         GlStateManager.disableBlend();
         GlStateManager.enableTexture2D();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         if (overItems) {
            GlStateManager.enableDepth();
         }
      }
   }

   float opacityFor(ScoreboardRenderer.Style s) {
      return 1.0F;
   }

   public float[] boundsGui() {
      if (this.renderer.visible() && !(this.renderer.lastW <= 0.0F)) {
         float k = this.lastK;
         return new float[]{this.renderer.lastX * k, this.renderer.lastY * k, this.renderer.lastW * k, this.renderer.lastH * k};
      } else {
         return null;
      }
   }

   public void placeGui(float left, float top, float w, float h, float sw, float sh) {
      this.position.place(left, top, w, h, sw, sh);
   }

   public boolean showingSample() {
      return this.showingSample;
   }

   public SliderSetting scaleSetting() {
      return this.scale;
   }

   public HudPosition positionSetting() {
      return this.position;
   }

   public void resetPlace() {
      this.position.reset();
      this.scale.reset();
   }

   @Override
   public void onWorldChange() {
      this.renderer.clear();
   }

   @Override
   public void applyTheme() {
      this.kind.set(ScoreboardElement.Kind.THEMED);
      this.font.set(ScoreboardElement.Font.MOD);
   }

   public String stateForTest() {
      return "hook "
         + (this.hookIn() ? "in" : "MISSING")
         + ", drawn "
         + String.format("%.1f × %.1f at %.1f, %.1f", this.renderer.lastW, this.renderer.lastH, this.renderer.lastX, this.renderer.lastY)
         + ", cost "
         + (this.costMs < 0.0F ? "-" : String.format("%.3f ms", this.costMs))
         + ", rules "
         + this.rules.toJson();
   }

   public void hideForTest(String title, String text, boolean hide) {
      this.rules.set(this.scopeFor(title), ScoreboardRules.normalize(text), hide);
   }

   public void previewTestForTest(int button) {
      if (this.lastPreview != null) {
         this.lastPreview.test(button);
      }
   }

   public static enum Align implements Displayable {
      LEFT("Left", "Lines start at the left edge"),
      CENTER("Center", "Lines centered"),
      RIGHT("Right", "Lines end before the numbers");

      private final String displayName;
      private final String description;

      private Align(String displayName, String description) {
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

   public static enum Font implements Displayable {
      VANILLA("Vanilla", "Minecraft's font (your resource pack's)"),
      MOD("Mod Font", "The mod's font, crisp at every GUI scale");

      private final String displayName;
      private final String description;

      private Font(String displayName, String description) {
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

   public static enum Kind implements Displayable {
      VANILLA("Vanilla", "Vanilla's dark boxes (with this tab's options)"),
      THEMED("Themed", "The island's look: background, border, corner radius, shadow, glass and glint");

      private final String displayName;
      private final String description;

      private Kind(String displayName, String description) {
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

   public static enum NumberColor implements Displayable {
      RED("Vanilla Red", "Red, like vanilla"),
      ACCENT("Accent", "The island's accent color"),
      CUSTOM("Custom", "A color of your own");

      private final String displayName;
      private final String description;

      private NumberColor(String displayName, String description) {
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

   public static enum TitleAlign implements Displayable {
      CENTER("Centered", "The title in the middle, like vanilla"),
      LEFT("Left", "The title at the left edge");

      private final String displayName;
      private final String description;

      private TitleAlign(String displayName, String description) {
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
