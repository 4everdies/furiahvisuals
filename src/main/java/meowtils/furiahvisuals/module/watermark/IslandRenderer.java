package meowtils.furiahvisuals.module.watermark;

import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.gui.FuriahScreen;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.UiProbe;
import meowtils.furiahvisuals.hud.HudEditor;
import meowtils.furiahvisuals.hud.HudGlint;
import meowtils.furiahvisuals.hud.HudPanel;
import meowtils.furiahvisuals.module.ui.UiHooks;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.boss.BossStatus;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

final class IslandRenderer {
   private static final float MARGIN = 10.0F;
   private static final float OMEGA = 17.0F;
   private static final float CROSSFADE = 0.3F;
   private static final float MOVE = 0.3F;
   private static final float VIS_FADE = 0.18F;
   private static final float TOOLTIP_DELAY = 0.35F;
   private static final float MAX_NOTICE = 420.0F;
   private static final Object NORMAL = new Object();
   private static final Object SETTINGS = new Object();
   private static final Object DISCORD = new Object();
   private static final float PANEL_BAR_FADE = 0.1F;
   private static final float PANEL_CONTENT_FADE = 0.08F;
   private static final int ACT_NOTICE = 900;
   private final WaterMarkModule wm;
   private final IslandDraw d = new IslandDraw();
   private final IslandStyle s = new IslandStyle();
   private final SettingsView settings;
   private final ModulesPanel panel;
   private final IslandItem.Hit hover = new IslandItem.Hit();
   private long lastNanos;
   private long idleFrame = -1L;
   private long idleNanos;
   private float dt;
   private float p = 1.0F;
   private int cachedSf;
   private int sfWidth;
   private int sfHeight;
   private int sfScale;
   private boolean sfUnicode;
   private float vis;
   private WaterMarkModule.Anchor shownAnchor;
   private WaterMarkModule.Anchor fromAnchor;
   private WaterMarkModule.Anchor align = WaterMarkModule.Anchor.TOP_CENTER;
   private float moveT = 1.0F;
   private final Spring avoidTop = new Spring();
   private final Spring avoidBottom = new Spring();
   private Object layer = NORMAL;
   private Object oldLayer;
   private float mix = 1.0F;
   private final Spring middle = new Spring();
   private boolean locked = true;
   private boolean narrow;
   private WaterMarkModule.Notice capsule;
   private float capsuleA;
   private final Spring grow = new Spring();
   private boolean panelWanted;
   private boolean rowsStarted;
   private float barFade = 1.0F;
   private float panelHeaderA;
   private float panelRowsA;
   private float panelRowsT = -1.0F;
   private float grownFrom;
   private float barPillW;
   private float pillX;
   private float pillY;
   private float pillW;
   private float pillH;
   private float cy;
   private float fvBlock;
   private float middleX;
   private float middleW;
   private float gearX;
   private float normalW;
   private float settingsW;
   private float layerX;
   private float capX;
   private float capY;
   private float capW;
   private float capH;
   private boolean laidOut;
   private float screenX;
   private float screenY;
   private float screenW;
   private float screenH;
   private float screenPillW;
   private float screenPillH;
   private long screenAtMs;
   private long interactiveUntilMs;
   private boolean interactiveNow;
   private float hoverA;
   private float hx;
   private float hy;
   private float hw;
   private float hh;
   private float lastMx = Float.NaN;
   private float lastMy = Float.NaN;
   private long lastProbeMs;
   private String tooltip;
   private float tooltipTime;
   private boolean pressedOnIsland;
   private boolean glDepth;
   private boolean glCull;
   private boolean glBlend;
   private boolean glAlpha;
   private boolean glLighting;
   private int glShade;
   private int glSrcRgb;
   private int glDstRgb;
   private int glSrcA;
   private int glDstA;
   private float costMs = -1.0F;
   private long costShownMs;
   private String costText;
   private float previewX;
   private float previewY;
   private float previewU;
   float roomForTest = Float.NaN;
   int panelSwitchColor;

   IslandRenderer(WaterMarkModule wm) {
      this.wm = wm;
      this.settings = new SettingsView(wm);
      this.panel = new ModulesPanel(wm);
      this.grow.snap(0.0F);
   }

   IslandDraw draw() {
      return this.d;
   }

   SettingsView settingsView() {
      return this.settings;
   }

   ModulesPanel modulesPanel() {
      return this.panel;
   }

   private boolean panelShowing() {
      return this.panelWanted && this.grow.value >= 0.98F && this.panelHeaderA >= 0.5F;
   }

   boolean isInteractive() {
      return ClientClock.millis() < this.interactiveUntilMs;
   }

   void renderHud() {
      Minecraft mc = Minecraft.getMinecraft();
      GuiScreen screen = mc.currentScreen;
      boolean chat = ChatDetect.isChat(screen);
      boolean editor = HudEditor.is(screen);
      if (screen == null || chat || editor || !this.wm.showInMenus.get()) {
         boolean want;
         if (chat || editor) {
            want = true;
         } else if (screen != null) {
            want = false;
         } else {
            want = (!this.wm.hideF3.get() || !mc.gameSettings.showDebugInfo) && (!this.wm.hideTab.get() || !tabListShown(mc));
         }

         if (!want && this.vis <= 0.002F) {
            this.idle(ClientClock.nanos());
         } else {
            this.render(true, chat, want);
         }
      }
   }

   void renderOverScreen(GuiScreen screen) {
      if (screen != null && !(screen instanceof FuriahScreen) && !HudEditor.is(screen)) {
         if (ChatDetect.isChat(screen)) {
            if (this.tooltip != null && this.tooltipTime >= 0.35F && this.laidOut) {
               this.drawTooltipPass();
            }
         } else {
            if (this.wm.showInMenus.get()) {
               this.render(false, false, true);
            }
         }
      }
   }

   boolean mouseInput(GuiScreen screen) {
      int button = Mouse.getEventButton();
      if (button < 0) {
         int wheel = Mouse.getEventDWheel();
         if (wheel != 0 && this.panelWanted && ChatDetect.isChat(screen) && this.laidOut) {
            Minecraft mc = Minecraft.getMinecraft();
            float mx = Mouse.getEventX() / this.p;
            if (this.onIsland(mx, (mc.displayHeight - Mouse.getEventY() - 1) / this.p)) {
               this.panel.wheel(wheel);
               this.lastProbeMs = 0L;
               return true;
            }
         }

         return false;
      } else {
         boolean down = Mouse.getEventButtonState();
         if (!down) {
            if (this.pressedOnIsland) {
               this.pressedOnIsland = false;
               return true;
            } else {
               return false;
            }
         } else if (ChatDetect.isChat(screen) && this.laidOut && !(this.vis < 0.5F) && !(this.moveT < 1.0F)) {
            Minecraft mc = Minecraft.getMinecraft();
            float mx = Mouse.getEventX() / this.p;
            float my;
            if (this.onIsland(mx, my = (mc.displayHeight - Mouse.getEventY() - 1) / this.p)) {
               this.pressedOnIsland = true;
               IslandItem.Hit h;
               if (button == 0 && this.hitTest(mx, my, h = new IslandItem.Hit()) && h.clickable) {
                  this.click(h, mx, my);
                  this.lastProbeMs = 0L;
               }

               return true;
            } else if (this.wm.isModulesRowOpen()) {
               this.wm.openModulesRow(false);
               this.pressedOnIsland = true;
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   void renderPreview(float x, float y, float w, float h, float mx, float my) {
      Minecraft mc = Minecraft.getMinecraft();
      if (minimized(mc)) {
         this.idle(ClientClock.nanos());
      } else {
         this.wm.ensureTicked();
         this.tick(ClientClock.nanos());
         this.p = mc.displayHeight / 1080.0F * this.wm.scale.getFloat();
         float u = this.p / Ui.px;
         boolean over = mx >= x && mx < x + w && my >= y && my < y + h;
         if (over) {
            this.interactiveUntilMs = ClientClock.millis() + 400L;
         }

         this.interactiveNow = over;
         this.d.place(x, y, u, u);
         this.update(0.0F, 0.0F);
         float fit = Math.min(1.0F, (w - 28.0F) / Math.max(1.0F, this.pillW * u));
         float uu = u * fit;
         float boxW = w / uu;
         float boxH = h / uu;
         WaterMarkModule.Anchor a = this.wm.anchor.get();
         float px0 = a.isLeft() ? 14.0F / uu : (a.isRight() ? boxW - 14.0F / uu - this.pillW : (boxW - this.pillW) / 2.0F);
         float py0 = (boxH - this.pillH) / 2.0F;
         this.previewX = x;
         this.previewY = y;
         this.previewU = uu;
         this.d.place(x, y, uu, u);
         this.d.setOuterAlpha(Ui.alpha());
         this.align = a;
         this.place(px0, py0);
         this.probeHover(over ? (mx - x) / uu : Float.NaN, over ? (my - y) / uu : Float.NaN, this.mix >= 1.0F);
         this.paint(1.0F, 0.0F);
      }
   }

   boolean previewClick(float mx, float my) {
      if (this.laidOut && !(this.previewU <= 0.0F)) {
         float ix = (mx - this.previewX) / this.previewU;
         float iy = (my - this.previewY) / this.previewU;
         if (!this.onIsland(ix, iy)) {
            return false;
         } else {
            IslandItem.Hit h = new IslandItem.Hit();
            if (this.hitTest(ix, iy, h) && h.clickable) {
               this.click(h, ix, iy);
               this.lastProbeMs = 0L;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   String previewTooltip() {
      return this.tooltip;
   }

   private static boolean minimized(Minecraft mc) {
      return mc.displayWidth < 160 || mc.displayHeight < 120;
   }

   private void render(boolean hud, boolean chat, boolean want) {
      long start = System.nanoTime();
      long now = ClientClock.nanos();
      Minecraft mc = Minecraft.getMinecraft();
      if (minimized(mc)) {
         this.idle(now);
      } else {
         this.tick(now);
         this.p = mc.displayHeight / 1080.0F * this.wm.scale.getFloat();
         if (!(this.p <= 0.0F)) {
            if (chat) {
               this.interactiveUntilMs = ClientClock.millis() + 400L;
            }

            this.interactiveNow = chat;
            this.vis = Easing.approach(this.vis, want ? 1.0F : 0.0F, 16.666666F, this.dt);
            if (!want && this.vis < 0.002F) {
               this.vis = 0.0F;
            } else {
               float sf = this.scaleFactor(mc);
               float savedPx = Ui.px;
               float savedOffset = Ui.offsetY;
               Ui.ensureFonts();
               GlStateManager.pushMatrix();
               GlStateManager.scale(this.p / sf, this.p / sf, 1.0F);
               this.saveGl();
               setupGl();
               Ui.px = this.p;
               Ui.offsetY = 0.0F;
               Ui.beginFrame();

               try {
                  float sw = mc.displayWidth / this.p;
                  float sh = mc.displayHeight / this.p;
                  this.d.place(0.0F, 0.0F, 1.0F);
                  this.update(sw, sh);
                  this.positionOnScreen(mc, sw, sh, hud && mc.theWorld != null, chat, sf);
                  boolean ready = this.vis >= 0.5F && this.moveT >= 1.0F && this.mix >= 1.0F;
                  if (chat) {
                     this.probeHover(Mouse.getX() / this.p, (mc.displayHeight - Mouse.getY() - 1) / this.p, ready);
                  } else {
                     this.probeHover(Float.NaN, Float.NaN, false);
                  }

                  float slide;
                  float moveA;
                  if (this.moveT < 0.5F) {
                     moveA = 1.0F - Easing.outCubic(this.moveT * 2.0F);
                     slide = -10.0F * Easing.inCubic(this.moveT * 2.0F);
                  } else {
                     moveA = Easing.outCubic((this.moveT - 0.5F) * 2.0F);
                     slide = -10.0F * (1.0F - moveA);
                  }

                  if (this.align.isBottom()) {
                     slide = -slide;
                  }

                  boolean probe = chat && IslandCheck.measuring() && ready;
                  if (probe) {
                     UiProbe.begin();
                  }

                  this.paint(this.vis * moveA, slide);
                  if (probe) {
                     UiProbe.end();
                     IslandCheck.measured(UiProbe.items());
                  }
               } finally {
                  Ui.endFrame();
                  this.restoreGl();
                  GlStateManager.popMatrix();
                  Ui.px = savedPx;
                  Ui.offsetY = savedOffset;
               }

               float var23 = (float)(System.nanoTime() - start) / 1000000.0F;
               this.costMs = this.costMs < 0.0F ? var23 : this.costMs + (var23 - this.costMs) * 0.05F;
            }
         }
      }
   }

   private void tick(long now) {
      if (this.idleFrame >= 0L && this.idleFrame != GameClock.frame() && this.idleNanos > this.lastNanos) {
         this.lastNanos = this.idleNanos;
      }

      this.dt = this.lastNanos == 0L ? 0.0F : Math.min(0.1F, Math.max(0.0F, (float)(now - this.lastNanos) / 1.0E9F));
      this.lastNanos = now;
   }

   private void idle(long now) {
      this.idleFrame = GameClock.frame();
      this.idleNanos = now;
   }

   private float scaleFactor(Minecraft mc) {
      boolean unicode = mc.isUnicode();
      if (mc.displayWidth != this.sfWidth
         || mc.displayHeight != this.sfHeight
         || mc.gameSettings.guiScale != this.sfScale
         || unicode != this.sfUnicode
         || this.cachedSf == 0) {
         this.sfWidth = mc.displayWidth;
         this.sfHeight = mc.displayHeight;
         this.sfScale = mc.gameSettings.guiScale;
         this.sfUnicode = unicode;
         int limit = this.sfScale == 0 ? 1000 : this.sfScale;
         int f = 1;

         while (f < limit && this.sfWidth / (f + 1) >= 320 && this.sfHeight / (f + 1) >= 240) {
            f++;
         }

         if (unicode && f % 2 != 0 && f != 1) {
            f--;
         }

         this.cachedSf = f;
      }

      return this.cachedSf;
   }

   private static boolean tabListShown(Minecraft mc) {
      return mc.gameSettings.keyBindPlayerList.isKeyDown() && mc.thePlayer != null && mc.theWorld != null
         ? !mc.isIntegratedServerRunning() || mc.thePlayer.sendQueue.getPlayerInfoMap().size() > 1 || mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(0) != null
         : false;
   }

   private void saveGl() {
      this.glDepth = GL11.glIsEnabled(2929);
      this.glCull = GL11.glIsEnabled(2884);
      this.glBlend = GL11.glIsEnabled(3042);
      this.glAlpha = GL11.glIsEnabled(3008);
      this.glLighting = GL11.glIsEnabled(2896);
      this.glShade = GL11.glGetInteger(2900);
      this.glSrcRgb = GL11.glGetInteger(32969);
      this.glDstRgb = GL11.glGetInteger(32968);
      this.glSrcA = GL11.glGetInteger(32971);
      this.glDstA = GL11.glGetInteger(32970);
   }

   private static void setupGl() {
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

   private void restoreGl() {
      GlStateManager.shadeModel(this.glShade);
      GlStateManager.tryBlendFuncSeparate(this.glSrcRgb, this.glDstRgb, this.glSrcA, this.glDstA);
      if (this.glBlend) {
         GlStateManager.enableBlend();
      } else {
         GlStateManager.disableBlend();
      }

      if (this.glAlpha) {
         GlStateManager.enableAlpha();
      } else {
         GlStateManager.disableAlpha();
      }

      if (this.glDepth) {
         GlStateManager.enableDepth();
      } else {
         GlStateManager.disableDepth();
      }

      if (this.glCull) {
         GlStateManager.enableCull();
      } else {
         GlStateManager.disableCull();
      }

      if (this.glLighting) {
         GlStateManager.enableLighting();
      }

      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void update(float sw, float sh) {
      long nowMs = ClientClock.millis();
      float speed = this.wm.speed.getFloat();
      float omega = 17.0F * speed;
      this.s.size(this.wm.fontSize.getFloat());
      double time = ColorSetting.clock();
      this.s.colors(FuriahsVisuals.instance.getTheme().get(), this.wm.accentNow(time), this.wm.opacity.getFloat(), this.dt);
      this.s.palette(this.wm.accent, this.wm.gradientsAcross(), time);
      float presenceRate = 3.0F / (0.22F / speed);
      FvItem fv = this.wm.fv;
      fv.frame(this.dt, this.d, this.s);
      this.fvBlock = this.s.pad + fv.measure(this.d, this.s) + this.s.pad;
      SettingsItem gear = this.wm.gear;
      boolean gearWanted = this.wm.isShown(gear);
      gear.frame(this.dt, this.d, this.s);
      gear.width.update(gearWanted ? this.s.pad + gear.measure(this.d, this.s) + this.s.pad : 0.0F, omega, this.dt);
      gear.presence = Easing.approach(gear.presence, gearWanted ? 1.0F : 0.0F, presenceRate, this.dt);
      if (!gearWanted && this.wm.isSettingsViewOpen()) {
         this.wm.closeSettingsView();
      }

      List<IslandItem> items = this.wm.middleItems();
      boolean quick = this.wm.isSettingsViewOpen();
      float sum = 0.0F;

      for (int i = 0; i < items.size(); i++) {
         IslandItem item = items.get(i);
         if (!quick || !item.inBarKnown) {
            item.inBar = this.wm.isShown(item);
            item.inBarKnown = true;
         }

         boolean want2 = item.inBar && item.available();
         if (!want2 && item.presence <= 0.001F && item.width.value <= 0.01F) {
            item.presence = 0.0F;
            item.width.snap(0.0F);
            item.resetHeld();
         } else {
            item.frame(this.dt, this.d, this.s);
            float full;
            item.full = full = this.s.pad + item.measure(this.d, this.s) + this.s.pad;
            float target = want2 ? item.settle(full, nowMs) : 0.0F;
            if (!item.width.isStarted()) {
               item.width.snap(0.0F);
            }

            item.width.update(target, omega, this.dt);
            if (item.width.value < 0.0F) {
               item.width.value = 0.0F;
            }

            item.presence = Easing.approach(item.presence, want2 ? 1.0F : 0.0F, presenceRate, this.dt);
            sum += item.width.value;
         }
      }

      this.normalW = sum;
      WaterMarkModule.Notice notice = this.wm.notice(nowMs);
      boolean settingsOpen = this.wm.isSettingsViewOpen();
      Object want = settingsOpen ? SETTINGS : (notice != null ? notice : NORMAL);
      if (settingsOpen && notice != null) {
         this.capsule = notice;
      }

      this.capsuleA = Easing.approach(this.capsuleA, settingsOpen && notice != null ? 1.0F : 0.0F, 14.0F, this.dt);
      if (this.capsuleA < 0.01F && (!settingsOpen || notice == null)) {
         this.capsule = null;
      }

      if (want != this.layer) {
         this.oldLayer = this.layer;
         this.layer = want;
         this.mix = 0.0F;
         this.locked = false;
      }

      this.mix = Math.min(1.0F, this.mix + this.dt / (0.3F / speed));
      if (this.mix >= 1.0F) {
         this.oldLayer = null;
      }

      float room = sw > 0.0F ? sw - 20.0F - 2.0F * this.s.endPad - this.fvBlock - gear.width.value : Float.MAX_VALUE;
      if (!Float.isNaN(this.roomForTest)) {
         room = Math.min(room, this.roomForTest);
      }

      if (this.layer == SETTINGS || this.oldLayer == SETTINGS) {
         float fullx = this.settings.measure(this.d, this.s, false);
         this.narrow = fullx > room;
         this.settings.setNarrow(this.narrow);
         float a = this.settingsW = this.narrow ? this.settings.measure(this.d, this.s, true) : fullx;
      }

      float targetx = this.layerWidth(this.layer);
      if (this.layer == NORMAL && this.oldLayer == null && !this.locked && Math.abs(this.middle.value - this.normalW) < 0.5F) {
         this.locked = true;
      }

      if (this.locked) {
         this.middle.snap(this.normalW);
      } else {
         this.middle.update(targetx, omega, this.dt);
      }

      this.middleW = Math.max(0.0F, this.middle.value);
      this.pillW = this.s.endPad + this.fvBlock + this.middleW + gear.width.value + this.s.endPad;
      this.pillH = this.s.height;
      this.barPillW = this.pillW;
      this.updatePanel(settingsOpen, sw, sh, omega, speed);
      WaterMarkModule.Anchor a = this.wm.anchor.get();
      if (this.shownAnchor == null) {
         this.shownAnchor = a;
      } else if (a != this.shownAnchor && this.moveT >= 1.0F) {
         this.fromAnchor = this.shownAnchor;
         this.moveT = 0.0F;
      }

      if (this.moveT < 1.0F) {
         this.moveT = Math.min(1.0F, this.moveT + this.dt / (0.3F / speed));
         if (this.moveT >= 0.5F) {
            this.shownAnchor = a;
         }
      }
   }

   private void updatePanel(boolean settingsOpen, float sw, float sh, float omega, float speed) {
      boolean open = settingsOpen && this.wm.isModulesRowOpen();
      if (open && !this.panelWanted) {
         this.panelWanted = true;
         this.rowsStarted = false;
         this.grownFrom = this.barPillW;
      } else if (!open && this.panelWanted) {
         this.panelWanted = false;
         this.rowsStarted = false;
         this.panelRowsT = -1.0F;
      }

      if (this.panelWanted) {
         this.barFade = Math.max(0.0F, this.barFade - this.dt / (0.1F / speed));
         if (this.barFade <= 0.0F) {
            this.grow.update(1.0F, omega, this.dt);
         }

         float g = Easing.clamp01(this.grow.value);
         this.panelHeaderA = Easing.clamp01((g - 0.6F) / 0.3F);
         if (g >= 0.6F) {
            this.panelRowsT = this.rowsStarted ? this.panelRowsT + this.dt * speed : 0.0F;
            this.rowsStarted = true;
            this.panelRowsA = 1.0F;
         } else {
            this.panelRowsA = 0.0F;
         }
      } else {
         float rate = settingsOpen ? 0.08F : 0.04F;
         this.panelHeaderA = Math.max(0.0F, this.panelHeaderA - this.dt / (rate / speed));
         this.panelRowsA = Math.min(this.panelRowsA, this.panelHeaderA);
         if (this.panelHeaderA <= 0.0F || !settingsOpen) {
            this.grow.update(0.0F, omega, this.dt);
         }

         if (this.grow.value <= 0.02F) {
            this.grow.snap(0.0F);
            this.barFade = Math.min(1.0F, this.barFade + this.dt / (0.1F / speed));
         }
      }

      float g = Easing.clamp01(this.grow.value);
      if (!(g <= 5.0E-4F) || this.panelWanted) {
         float screenW = sw > 0.0F ? sw : 1920.0F / this.wm.scale.getFloat();
         float screenH = sh > 0.0F ? sh : 1080.0F / this.wm.scale.getFloat();
         float maxH = Math.min(screenH * 0.6F, screenH - 20.0F - 8.0F);
         float[] size = this.panel.measure(this.d, this.s, screenW, screenH, this.grownFrom, maxH);
         this.pillW = this.barPillW + (size[0] - this.barPillW) * g;
         this.pillH = this.s.height + (size[1] - this.s.height) * g;
      }
   }

   private float layerWidth(Object l) {
      if (l == NORMAL) {
         return this.normalW;
      } else {
         return l == SETTINGS ? this.settingsW : this.noticeWidth((WaterMarkModule.Notice)l);
      }
   }

   private float noticeWidth(WaterMarkModule.Notice n) {
      int px = this.d.pixels(this.s.font);
      if (n.textW < 0.0F || n.measuredPx != px) {
         n.measuredPx = px;
         n.shown = n.text;
         n.textW = this.d.width(false, this.s.font, n.text);
         if (n.textW > 420.0F) {
            for (int end = n.text.length() - 1; end > 1; end--) {
               String t = n.text.substring(0, end).trim() + "…";
               float w = this.d.width(false, this.s.font, t);
               if (w <= 420.0F) {
                  n.shown = t;
                  n.textW = w;
                  break;
               }
            }
         }
      }

      float lead = n.dot != 0 ? 6.0F * this.s.font / 13.0F + this.s.gap : (n.icon != null ? this.s.icon + this.s.gap : 0.0F);
      return this.s.pad + lead + n.textW + this.s.pad;
   }

   private void positionOnScreen(Minecraft mc, float sw, float sh, boolean inWorld, boolean chat, float sf) {
      WaterMarkModule.Anchor a;
      this.align = a = this.moveT < 0.5F && this.fromAnchor != null ? this.fromAnchor : this.shownAnchor;
      float scale = this.wm.scale.getFloat();
      float offX = this.wm.offsetX.getFloat() / scale;
      float offY = this.wm.offsetY.getFloat() / scale;
      float omega = 17.0F * this.wm.speed.getFloat();
      float x;
      switch (a) {
         case TOP_LEFT:
         case BOTTOM_LEFT:
            x = 10.0F + offX;
            break;
         case TOP_RIGHT:
         case BOTTOM_RIGHT:
            x = sw - 10.0F - offX - this.pillW;
            break;
         default:
            x = (sw - this.pillW) / 2.0F + offX;
      }

      boolean grown = this.grow.value > 0.001F;
      float edge = grown ? 4.0F : 2.0F;
      if (this.pillW < sw - 2.0F * edge) {
         x = Math.max(edge, Math.min(sw - this.pillW - edge, x));
      }

      float y;
      if (!a.isBottom()) {
         float top = 10.0F + offY;
         float need = 0.0F;
         if (inWorld && a == WaterMarkModule.Anchor.TOP_CENTER && BossStatus.bossName != null && BossStatus.statusBarTime > 0) {
            float bossBottom = 20.0F * sf / this.p;
            need = Math.max(0.0F, bossBottom + 2.0F - top);
         }

         this.avoidTop.update(need, omega, this.dt);
         this.avoidBottom.snap(0.0F);
         y = top + this.avoidTop.value;
      } else {
         float bottom = sh - 10.0F - offY;
         float need = 0.0F;
         if (inWorld) {
            if (chat) {
               float[] row = UiHooks.chatTypingRow();
               if (row == null) {
                  need = Math.max(need, bottom - (sh - UiHooks.chatInputClearance() * sf / this.p));
               } else {
                  float g = sf / this.p;
                  float top = (row[1] - 3.0F) * g;
                  if (x < row[2] * g && x + this.pillW > row[0] * g && bottom > top && bottom - this.pillH < row[3] * g) {
                     need = Math.max(need, bottom - top);
                  }
               }
            }

            float half;
            boolean overHotbar = x < sw / 2.0F + (half = 91.0F * sf / this.p + 4.0F) && x + this.pillW > sw / 2.0F - half;
            if (overHotbar) {
               float rows = mc.playerController != null && mc.playerController.shouldDrawHUD() ? 52.0F : 25.0F;
               need = Math.max(need, bottom - (sh - (rows + UiHooks.itemHudLift()) * sf / this.p));
            }
         }

         this.avoidBottom.update(Math.max(0.0F, need), omega, this.dt);
         this.avoidTop.snap(0.0F);
         y = bottom - this.avoidBottom.value - this.pillH;
      }

      if (grown) {
         y = Math.max(4.0F, Math.min(sh - this.pillH - 4.0F, y));
      }

      this.place(x, y);
      float g = this.p / sf;
      this.screenX = this.pillX * g;
      this.screenY = this.pillY * g;
      this.screenW = this.pillW * g;
      this.screenH = this.pillH * g;
      this.screenPillW = this.pillW;
      this.screenPillH = this.pillH;
      this.screenAtMs = ClientClock.millis();
   }

   float[] boundsGui() {
      return ClientClock.millis() - this.screenAtMs <= 500L && !(this.vis < 0.05F) && !(this.screenW <= 0.0F)
         ? new float[]{this.screenX, this.screenY, this.screenW, this.screenH}
         : null;
   }

   float[] boundsAtGui(WaterMarkModule.Anchor a) {
      if (this.screenPillW <= 0.0F) {
         return null;
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         float sf = this.scaleFactor(mc);
         float scale = this.wm.scale.getFloat();
         float pp = mc.displayHeight / 1080.0F * scale;
         float sw = mc.displayWidth / pp;
         float sh = mc.displayHeight / pp;
         float offX = this.wm.offsetX.getFloat() / scale;
         float offY = this.wm.offsetY.getFloat() / scale;
         float w = this.screenPillW;
         float h = this.screenPillH;
         float x = a.isLeft() ? 10.0F + offX : (a.isRight() ? sw - 10.0F - offX - w : (sw - w) / 2.0F + offX);
         if (w < sw - 4.0F) {
            x = Math.max(2.0F, Math.min(sw - w - 2.0F, x));
         }

         float y = a.isBottom() ? sh - 10.0F - offY - h : 10.0F + offY;
         float g = pp / sf;
         return new float[]{x * g, y * g, w * g, h * g};
      }
   }

   private void place(float x, float y) {
      float pu = this.d.u * Ui.px;
      float left = Math.round((this.d.ox + x * this.d.u) * Ui.px);
      float top = Math.round((this.d.oy + y * this.d.u) * Ui.px);
      this.pillX = (left / Ui.px - this.d.ox) / this.d.u;
      this.pillY = (top / Ui.px - this.d.oy) / this.d.u;
      this.pillW = Math.round(this.pillW * pu) / pu;
      this.pillH = Math.round(this.pillH * pu) / pu;
      this.cy = this.align.isBottom() ? this.pillY + this.pillH - this.s.height / 2.0F : this.pillY + this.s.height / 2.0F;
      this.middleX = this.pillX + this.s.endPad + this.fvBlock;
      this.gearX = this.middleX + this.middleW;
      this.s.span(this.pillX, this.pillW);
      this.laidOut = true;
   }

   private boolean glassOn() {
      WaterMarkModule.GlassMode mode = this.wm.glassMode.get();
      return mode == WaterMarkModule.GlassMode.ON || mode == WaterMarkModule.GlassMode.AUTO && this.s.glass;
   }

   private void paint(float alpha, float slide) {
      if (!(alpha <= 0.002F)) {
         float x = this.pillX;
         float y = this.pillY + slide;
         float w = this.pillW;
         float h = this.pillH;
         float r = this.s.height / 2.0F;
         float barY = this.align.isBottom() ? y + h - this.s.height : y;
         float c;
         this.cy = c = barY + this.s.height / 2.0F;
         float onePx = 1.0F / this.p;
         this.d.setAlpha(alpha);
         boolean glass = this.glassOn() && IslandGlass.capture(this.d.X(x), this.d.Y(y), this.d.L(w), this.d.L(h));
         if (this.wm.shadow.get()) {
            this.d.shadow(x, y + 2.5F, w, h, r, 9.0F, ColorUtil.mulAlpha(this.s.shadow, this.s.light ? 0.35F : 0.6F));
         }

         if (!glass || !IslandGlass.draw(this.d.X(x), this.d.Y(y), this.d.L(w), this.d.L(h), this.d.L(r), this.s.glassTint, this.s.glassRim, alpha)) {
            this.d.roundRectUnclipped(x, y, w, h, r, this.s.bg);
         }

         this.hoverA = Easing.approach(this.hoverA, this.hover.tooltip == null && !this.hover.clickable ? 0.0F : 1.0F, 18.0F, this.dt);
         if (this.hover.tooltip != null || this.hover.clickable) {
            this.hx = this.hover.x;
            this.hy = this.hover.y;
            this.hw = this.hover.w;
            this.hh = this.hover.h;
         }

         boolean rowHover = this.hover.action >= 610 && this.hover.action < 700;
         if (this.hoverA > 0.01F && this.hw > 0.0F && !rowHover) {
            float hr = Math.min(this.hh / 2.0F, r - 2.0F);
            int col = ColorUtil.mulAlpha(this.s.hover, this.hoverA * (this.hover.clickable ? 1.0F : 0.45F));
            this.d.roundRect(this.hx, this.hy + slide, this.hw, this.hh, hr, col);
         }

         float bar = alpha * this.barFade;
         float bh = this.s.height;
         float mX = x + this.s.endPad + this.fvBlock;
         if (bar > 0.002F) {
            this.d.setAlpha(bar);
            this.wm.fv.draw(this.d, this.s, x + this.s.endPad + this.s.pad, c);
            this.d.pushWindow(mX, mX + this.middleW, 6.0F);
            float e = Easing.inOutSine(this.mix);
            if (this.oldLayer != null) {
               this.drawLayer(this.oldLayer, mX, c, bar * (1.0F - e), 1.0F - 0.08F * e, false);
            }

            float inA = Easing.clamp01((this.mix - 0.12F) / 0.88F);
            this.drawLayer(this.layer, mX, c, bar * Easing.outCubic(inA), 0.94F + 0.06F * Easing.outCubic(this.mix), true);
            this.d.popWindow();
            this.d.scaleAround(1.0F, 0.0F, 0.0F);
            this.d.setAlpha(bar);
            if (this.wm.separators.get() && this.middleW > 0.05F) {
               this.d.hairline(mX, c - bh * 0.25F, bh * 0.5F, ColorUtil.mulAlpha(this.s.separator, Math.min(1.0F, this.middleW / 8.0F)));
            }
         }

         SettingsItem gear = this.wm.gear;
         float gw = gear.width.value;
         if (gw > 0.05F) {
            float gX;
            gear.blockX = gX = mX + this.middleW;
            gear.blockW = gw;
            if (bar > 0.002F) {
               this.d.setAlpha(bar);
               if (this.wm.separators.get()) {
                  this.d.hairline(gX, c - bh * 0.25F, bh * 0.5F, ColorUtil.mulAlpha(this.s.separator, gear.presence));
               }

               float full = this.s.pad + gear.measure(this.d, this.s) + this.s.pad;
               this.d.setAlpha(bar * gear.presence);
               if (gw < full - 0.5F) {
                  this.d.pushWindow(gX, gX + gw, 3.0F);
                  gear.draw(this.d, this.s, gX + (gw - full) / 2.0F + this.s.pad, c);
                  this.d.popWindow();
               } else {
                  gear.draw(this.d, this.s, gX + this.s.pad, c);
               }
            }
         }

         this.d.setAlpha(alpha);
         if (this.grow.value > 0.001F && (this.panelHeaderA > 0.002F || this.panelRowsA > 0.002F)) {
            this.d.flush();
            Ui.pushScissor(this.d.X(x), this.d.Y(y), this.d.L(w), this.d.L(h));

            try {
               this.panel.draw(this.d, this.s, x, y, w, h, this.panelHeaderA, this.panelRowsT, this.panelRowsA, this.hover, this.dt);
               this.d.flush();
            } finally {
               Ui.popScissor();
            }

            this.d.setAlpha(alpha);
         }

         if (this.wm.outline.get()) {
            this.d.outlineUnclipped(x, y, w, h, r, onePx, this.s.outline);
         }

         this.d.flush();
         this.drawGlint(x, y, w, h, r, alpha);
         this.drawCapsule(alpha, slide);
      }
   }

   void glintForTest(float progress) {
      HudGlint.progressOverride = progress;
   }

   private void drawGlint(float x, float y, float w, float h, float r, float alpha) {
      if (this.wm.glint.get() && !(alpha <= 0.01F) && !FvItem.colorCheck) {
         float mid = x + w / 2.0F;
         HudPanel.glint(
            this.d.X(x),
            this.d.Y(y),
            this.d.L(w),
            this.d.L(h),
            this.d.L(r),
            this.s.accentAt(mid),
            this.s.globalAt(mid),
            this.s.light,
            alpha,
            this.wm.scale.getFloat()
         );
      }
   }

   private void drawLayer(Object l, float mX, float c, float alpha, float scale, boolean current) {
      if (!(alpha <= 0.003F)) {
         float lw = this.layerWidth(l);
         float slack = this.middleW - lw;
         WaterMarkModule.Anchor a = this.align;
         float lx = mX + (a.isLeft() ? 0.0F : (a.isRight() ? slack : slack / 2.0F));
         if (current) {
            this.layerX = lx;
         }

         this.d.scaleAround(scale, lx + lw / 2.0F, c);
         this.d.setAlpha(alpha);
         if (l == NORMAL) {
            this.drawItems(lx, c, alpha);
         } else if (l == SETTINGS) {
            this.settings.draw(this.d, this.s, lx, c, this.dt);
         } else {
            this.drawNotice((WaterMarkModule.Notice)l, lx, c);
         }
      }
   }

   private void drawItems(float x, float c, float alpha) {
      List<IslandItem> items = this.wm.middleItems();
      boolean lines = this.wm.separators.get();
      boolean first = true;

      for (int i = 0; i < items.size(); i++) {
         IslandItem item = items.get(i);
         float bw = item.width.value;
         if (bw <= 0.05F) {
            item.blockW = 0.0F;
         } else {
            float a = alpha * item.presence;
            this.d.setAlpha(a);
            if (lines && !first) {
               this.d.hairline(x, c - this.s.height * 0.25F, this.s.height * 0.5F, this.s.separator);
            }

            first = false;
            item.blockX = x;
            item.blockW = bw;
            float full = item.full;
            if (bw < full - 0.5F) {
               this.d.pushWindow(x, x + bw, 3.0F);
               item.draw(this.d, this.s, x + (bw - full) / 2.0F + this.s.pad, c);
               this.d.popWindow();
            } else {
               item.draw(this.d, this.s, x + (bw - full) / 2.0F + this.s.pad, c);
            }

            x += bw;
         }
      }

      this.d.setAlpha(alpha);
   }

   private void drawNotice(WaterMarkModule.Notice n, float x, float c) {
      this.noticeWidth(n);
      float tx = x + this.s.pad;
      if (n.dot != 0) {
         float r = 3.0F * this.s.font / 13.0F;
         this.d.glow(tx + r, c, r * 5.0F, ColorUtil.withAlpha(n.dot, 0.45F));
         this.d.circle(tx + r, c, r, n.dot);
         tx += r * 2.0F + this.s.gap;
      } else if (n.icon != null) {
         this.d.icon(n.icon, tx, c - this.s.icon / 2.0F, this.s.icon, this.s.accentAt(tx + this.s.icon / 2.0F));
         tx += this.s.icon + this.s.gap;
      }

      this.d.textMiddle(false, this.s.font, n.shown != null ? n.shown : n.text, tx, c, this.s.text);
   }

   private void drawCapsule(float alpha, float slide) {
      WaterMarkModule.Notice n = this.capsule;
      if (n != null && !(this.capsuleA <= 0.01F)) {
         float a = alpha * this.capsuleA;
         float w = this.noticeWidth(n);
         float h = this.pillH * 0.84F;
         float x = this.pillX + (this.pillW - w) / 2.0F;
         boolean below = !this.align.isBottom();
         float y = below
            ? this.pillY + this.pillH + 6.0F + slide - 4.0F * (1.0F - this.capsuleA)
            : this.pillY - 6.0F - h + slide + 4.0F * (1.0F - this.capsuleA);
         this.capX = x;
         this.capY = y;
         this.capW = w;
         this.capH = h;
         this.d.setAlpha(a);
         if (this.wm.shadow.get()) {
            this.d.shadow(x, y + 2.0F, w, h, h / 2.0F, 7.0F, ColorUtil.mulAlpha(this.s.shadow, this.s.light ? 0.3F : 0.5F));
         }

         this.d.roundRectUnclipped(x, y, w, h, h / 2.0F, this.glassOn() ? ColorUtil.mulAlpha(this.s.glassTint, 1.6F) : this.s.bg);
         if (this.wm.outline.get()) {
            this.d.outlineUnclipped(x, y, w, h, h / 2.0F, 1.0F / this.p, this.s.outline);
         }

         this.drawNotice(n, x, y + h / 2.0F);
         this.d.flush();
         this.d.setAlpha(alpha);
      } else {
         this.capW = 0.0F;
      }
   }

   private boolean onIsland(float mx, float my) {
      return mx >= this.pillX - 2.0F && mx <= this.pillX + this.pillW + 2.0F && my >= this.pillY - 2.0F && my <= this.pillY + this.pillH + 2.0F
         ? true
         : this.capW > 0.0F && mx >= this.capX && mx <= this.capX + this.capW && my >= this.capY && my <= this.capY + this.capH;
   }

   private void probeHover(float mx, float my, boolean ready) {
      if (!Float.isNaN(mx) && this.interactiveNow && ready) {
         long now = System.currentTimeMillis();
         if (mx != this.lastMx || my != this.lastMy || now - this.lastProbeMs > 250L) {
            this.lastMx = mx;
            this.lastMy = my;
            this.lastProbeMs = now;
            String before = this.tooltip;
            if (!this.onIsland(mx, my) || !this.hitTest(mx, my, this.hover)) {
               this.hover.clear();
            }

            this.tooltip = this.hover.tooltip;
            if (this.tooltip == null || !this.tooltip.equals(before)) {
               float var7 = this.tooltipTime = before != null && this.tooltip != null && this.tooltipTime >= 0.35F ? 0.35F : 0.0F;
            }
         }

         if (this.tooltip != null) {
            this.tooltipTime = this.tooltipTime + this.dt;
         }
      } else {
         this.hover.clear();
         this.tooltip = null;
         this.tooltipTime = 0.0F;
         this.lastMx = Float.NaN;
      }
   }

   private boolean hitTest(float mx, float my, IslandItem.Hit h) {
      h.clear();
      if (this.capW > 0.0F && mx >= this.capX && mx <= this.capX + this.capW && my >= this.capY && my <= this.capY + this.capH && this.capsule != null) {
         if (this.capsule.action != null) {
            h.set(null, this.capX, this.capY, this.capW, this.capH, 900, this.capsule.actionTip, true);
         } else {
            h.set(null, this.capX, this.capY, this.capW, this.capH, 0, this.capsule.tooltip != null ? this.capsule.tooltip : this.capsule.text, false);
         }

         return true;
      } else if (this.panelShowing()) {
         return this.panel.hit(mx, my, this.s, h);
      } else if (!(this.grow.value > 0.02F) && !(this.barFade < 0.98F)) {
         if (mx < this.middleX) {
            return this.wm.fv.hit(mx, my, this.pillX + this.s.endPad + this.s.pad, this.cy, this.s, h);
         } else if (mx >= this.gearX && this.wm.gear.blockW > 0.0F) {
            return this.wm.gear.hit(mx, my, this.gearX + this.s.pad, this.cy, this.s, h);
         } else if (this.layer == SETTINGS) {
            return this.settings.hit(mx, my, this.layerX, this.cy, this.s, h);
         } else if (this.layer instanceof WaterMarkModule.Notice) {
            WaterMarkModule.Notice n = (WaterMarkModule.Notice)this.layer;
            if (n.action != null) {
               h.set(null, this.layerX, this.pillY, this.noticeWidth(n), this.pillH, 900, n.actionTip, true);
               return true;
            } else {
               h.set(null, this.layerX, this.pillY, this.noticeWidth(n), this.pillH, 0, n.tooltip, false);
               return n.tooltip != null;
            }
         } else {
            List<IslandItem> items = this.wm.middleItems();

            for (int i = 0; i < items.size(); i++) {
               IslandItem item = items.get(i);
               if (item.blockW > 0.0F && item.presence > 0.9F && mx >= item.blockX && mx < item.blockX + item.blockW) {
                  return item.hit(mx, my, item.blockX + (item.blockW - item.full) / 2.0F + this.s.pad, this.cy, this.s, h);
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private void click(IslandItem.Hit h, float mx, float my) {
      int sounds = ModSounds.requestCount();
      this.clickInner(h, mx, my);
      if (ModSounds.requestCount() == sounds) {
         this.wm.islandClick();
      }
   }

   private void clickInner(IslandItem.Hit h, float mx, float my) {
      if (h.item != null) {
         h.item.click(h, mx, my);
      } else if (h.action == 900) {
         WaterMarkModule.Notice n = this.capsule != null && this.capW > 0.0F && my >= this.capY && my <= this.capY + this.capH
            ? this.capsule
            : (this.layer instanceof WaterMarkModule.Notice ? (WaterMarkModule.Notice)this.layer : null);
         if (n != null && n.action != null) {
            n.action.run();
         }
      } else if (h.action >= 600 && h.action < 700) {
         this.panel.click(h.action);
      } else if (h.action >= 100) {
         this.settings.click(h.action);
      }
   }

   private void drawTooltipPass() {
      Minecraft mc = Minecraft.getMinecraft();
      float sf = this.scaleFactor(mc);
      float savedPx = Ui.px;
      float savedOffset = Ui.offsetY;
      GlStateManager.pushMatrix();
      GlStateManager.scale(this.p / sf, this.p / sf, 1.0F);
      this.saveGl();
      setupGl();
      Ui.px = this.p;
      Ui.offsetY = 0.0F;
      Ui.beginFrame();

      try {
         this.d.place(0.0F, 0.0F, 1.0F);
         float mx = Mouse.getX() / this.p;
         float my = (mc.displayHeight - Mouse.getY() - 1) / this.p;
         this.drawTooltip(this.tooltip, mx, my, mc.displayWidth / this.p, mc.displayHeight / this.p);
      } finally {
         Ui.endFrame();
         this.restoreGl();
         GlStateManager.popMatrix();
         Ui.px = savedPx;
         Ui.offsetY = savedOffset;
      }
   }

   private void drawTooltip(String text, float mx, float my, float sw, float sh) {
      float a = Easing.clamp01((this.tooltipTime - 0.35F) / 0.12F);
      if (!(a <= 0.0F) && text != null) {
         float size = this.s.small;
         float w = this.d.width(false, size, text) + 16.0F;
         float h = this.d.lineHeight(false, size) + 10.0F;
         boolean below = !this.align.isBottom();
         float x = Math.max(4.0F, Math.min(sw - w - 4.0F, mx + 10.0F));
         float y = below ? Math.max(my + 16.0F, this.pillY + this.pillH + 6.0F) : Math.min(my - h - 10.0F, this.pillY - h - 6.0F);
         y = Math.max(4.0F, Math.min(sh - h - 4.0F, y));
         this.d.setAlpha(a);
         this.d.shadow(x, y + 2.0F, w, h, 6.0F, 8.0F, this.s.light ? 1073741824 : -2013265920);
         this.d.roundRectUnclipped(x, y, w, h, 6.0F, this.s.light ? -117440513 : -233301212);
         this.d.outlineUnclipped(x, y, w, h, 6.0F, 1.0F / this.p, this.s.outline);
         this.d.textMiddle(false, size, text, x + 8.0F, y + h / 2.0F, this.s.text);
         this.d.flush();
         this.d.setAlpha(1.0F);
      }
   }

   String costInfo() {
      if (this.costMs < 0.0F) {
         return null;
      } else {
         long now = System.currentTimeMillis();
         if (this.costText == null || now - this.costShownMs > 500L) {
            this.costShownMs = now;
            this.costText = String.format(Locale.ROOT, "WaterMark: %.3f ms per frame", this.costMs);
         }

         return this.costText;
      }
   }

   float costForTest() {
      return this.costMs;
   }

   float[] pillForTest() {
      return new float[]{this.pillX, this.pillY, this.pillW, this.pillH, this.p, this.middleW, this.normalW};
   }

   IslandItem.Hit hitForTest(float x, float y) {
      IslandItem.Hit h = new IslandItem.Hit();
      return this.onIsland(x, y) && this.hitTest(x, y, h) ? h : null;
   }

   boolean clickForTest(float x, float y) {
      IslandItem.Hit h = new IslandItem.Hit();
      if (this.onIsland(x, y) && this.hitTest(x, y, h) && h.clickable) {
         this.click(h, x, y);
         return true;
      } else {
         return false;
      }
   }

   float visibilityForTest() {
      return this.vis;
   }

   IslandStyle styleForTest() {
      return this.s;
   }

   float[] fvOffsetForTest() {
      return this.d.capsOffsetForTest(this.s.font, this.cy);
   }

   int layerForTest() {
      return this.layer == NORMAL ? 0 : (this.layer == SETTINGS ? (this.wm.isModulesRowOpen() ? 4 : 1) : (this.layer == DISCORD ? 3 : 2));
   }

   float[] panelForTest() {
      float[] l = this.panel.layoutForTest();
      return new float[]{
         l[0], l[1], l[2], l[3], l[4], l[5], this.grow.value, this.panelHeaderA, this.panelRowsA, this.pillX, this.pillY, this.pillW, this.pillH
      };
   }

   float[] panelRowForTest(String id) {
      return this.panel.rowForTest(id, this.s);
   }

   float[] panelButtonsForTest() {
      return this.panel.buttonsForTest(this.s);
   }

   float[] blockForTest(IslandItem item) {
      return new float[]{item.blockX, item.blockW, item.presence};
   }

   float[] fvBoxForCheck() {
      float fx = this.pillX + this.s.endPad + this.s.pad;
      float fw = this.wm.fv.measure(this.d, this.s);
      float half = this.s.font * 0.42F;
      return new float[]{this.d.X(fx), this.d.Y(this.cy - half), this.d.L(fw), this.d.L(half * 2.0F)};
   }

   float[] panelSwitchForCheck(float x, float y, float w, float h) {
      this.d.place(x, y, 1.0F);
      this.d.setOuterAlpha(Ui.alpha());
      float sw = this.s.icon * 3.0F;
      float sh = this.s.icon * 1.65F;
      float sx = (w - sw) / 2.0F;
      float sy = (h - sh) / 2.0F;
      this.panelSwitchColor = this.s.accentAt(sx + sw / 2.0F);
      ModulesPanel.drawSwitch(this.d, this.s, sx, sy, sw, sh, 1.0F, this.panelSwitchColor);
      this.d.flush();
      return new float[]{this.d.X(sx + sh * 0.55F), this.d.Y(sy + sh / 2.0F - 1.0F), 2.0F, 2.0F};
   }

   float[] chipForTest(int n) {
      float[] c = this.settings.chipCenterForTest(n);
      float[] fArray;
      if (c == null) {
         fArray = null;
      } else {
         float[] fArray2 = new float[]{this.layerX + c[0], 0.0F};
         fArray = fArray2;
         fArray2[1] = this.cy;
      }

      return fArray;
   }
}
