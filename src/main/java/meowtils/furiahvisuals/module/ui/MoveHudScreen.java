package meowtils.furiahvisuals.module.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.FuriahScreen;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.hud.HudEditor;
import meowtils.furiahvisuals.module.ui.chat.ChatElement;
import meowtils.furiahvisuals.module.ui.itemhud.ItemHudElement;
import meowtils.furiahvisuals.module.ui.scoreboard.ScoreboardElement;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer.EnumChatVisibility;
import org.lwjgl.input.Mouse;

public final class MoveHudScreen extends GuiScreen implements HudEditor {
   private static final float SNAP_PIXELS = 8.0F;
   private static final float LABEL_SIZE = 7.5F;
   private static final float LABEL_H = 13.0F;
   private static final float PANEL_W = 300.0F;
   private static final float DONE_W = 74.0F;
   private static final float DONE_H = 20.0F;
   private final UiModule ui;
   private final boolean fromMenu;
   private final GuiScreen menuParent;
   private final List<MoveHudScreen.Target> targets = new ArrayList<>();
   private MoveHudScreen.Target hover;
   private MoveHudScreen.Target drag;
   private float grabX;
   private float grabY;
   private float ghostX;
   private float ghostY;
   private float ghostW;
   private float ghostH;
   private WaterMarkModule.Anchor nearest;
   private float guideX = Float.NaN;
   private float guideY = Float.NaN;
   private static final float EDGE_PIXELS = 6.0F;
   private MoveHudScreen.Target resizing;
   private boolean resizeTop;
   private boolean resizeRight;
   private float fixLeft;
   private float fixBottom;
   private int hoverEdge;
   private float sf = 2.0F;
   private float sw = 1.0F;
   private float sh = 1.0F;
   private float mp = 1.0F;
   private long lastNanos;
   private float appear;
   private float panelA = 1.0F;
   private float doneHover;
   private float doneX;
   private float doneY;
   private boolean opened;
   private float[] testMouse;
   private float bestD;
   private float bestLine;
   private boolean testFree;

   public MoveHudScreen(UiModule ui, boolean fromMenu, GuiScreen menuParent) {
      this.ui = ui;
      this.fromMenu = fromMenu;
      this.menuParent = menuParent;
      this.targets.add(new MoveHudScreen.ScoreboardTarget(ui.scoreboard()));
      this.targets.add(new MoveHudScreen.ChatTarget(ui.chat()));
      this.targets.add(new MoveHudScreen.ItemHudTarget(ui.itemHud()));
      this.targets.add(new MoveHudScreen.BarsTarget(ui.itemHud()));
      this.targets.add(new MoveHudScreen.IslandTarget(FuriahsVisuals.instance.getWaterMark()));
   }

   @Override
   public boolean isDragging(Object element) {
      return this.drag != null && this.drag.element() == element;
   }

   public void initGui() {
      if (!this.opened) {
         this.opened = true;
         ModSounds.gui(Sfx.GUI_OPEN);
      }
   }

   public boolean doesGuiPauseGame() {
      return false;
   }

   public void onGuiClosed() {
      this.drag = null;
      FuriahsVisuals.instance.getConfig().saveIfDirty();
   }

   private void done() {
      if (this.fromMenu) {
         this.mc.displayGuiScreen(new FuriahScreen(this.menuParent));
      } else {
         ModSounds.gui(Sfx.GUI_CLOSE);
         this.mc.displayGuiScreen(null);
      }
   }

   protected void keyTyped(char typedChar, int keyCode) {
      if (keyCode == 1) {
         this.done();
      }
   }

   public void handleMouseInput() {
      this.readScreen();
      int button = Mouse.getEventButton();
      boolean down = Mouse.getEventButtonState();
      int wheel = Mouse.getEventDWheel();
      float gx = Mouse.getEventX() / this.sf;
      float gy = (this.mc.displayHeight - Mouse.getEventY() - 1) / this.sf;
      if (wheel != 0) {
         this.scroll(gx, gy, wheel > 0 ? 1 : -1);
      }

      if (button == 0) {
         if (down) {
            this.press(gx, gy);
         } else {
            this.release();
         }
      } else if (button == 1 && down) {
         this.rightPress(gx, gy);
      }
   }

   private void readScreen() {
      ScaledResolution res = new ScaledResolution(this.mc);
      this.sf = res.getScaleFactor();
      this.sw = (float)res.getScaledWidth_double();
      this.sh = (float)res.getScaledHeight_double();
      this.mp = Math.max(0.75F, Math.min(this.mc.displayHeight / 580.0F, this.mc.displayWidth / 900.0F));
   }

   private MoveHudScreen.Target targetAt(float gx, float gy) {
      MoveHudScreen.Target best = null;
      float bestArea = Float.MAX_VALUE;

      for (MoveHudScreen.Target t : this.targets) {
         float[] b = t.live();
         if (b != null) {
            float var10001 = b[0];
            float pad = 2.0F;
            float area;
            if (gx >= var10001 - 2.0F && gx <= b[0] + b[2] + pad && gy >= b[1] - pad && gy <= b[1] + b[3] + pad && (area = b[2] * b[3]) < bestArea) {
               bestArea = area;
               best = t;
            }
         }
      }

      return best;
   }

   private boolean overDone(float gx, float gy) {
      float x = gx * this.sf / this.mp;
      float y = gy * this.sf / this.mp;
      return this.panelA > 0.5F && x >= this.doneX && x <= this.doneX + 74.0F && y >= this.doneY && y <= this.doneY + 20.0F;
   }

   private int edgeAt(MoveHudScreen.Target t, float gx, float gy) {
      float[] b = t != null && t.resizable() ? t.live() : null;
      if (b == null) {
         return 0;
      } else {
         float reach = 6.0F / this.sf;
         boolean inX = gx >= b[0] - reach && gx <= b[0] + b[2] + reach;
         boolean inY = gy >= b[1] - reach && gy <= b[1] + b[3] + reach;
         boolean top = inX && Math.abs(gy - b[1]) <= reach;
         boolean right = inY && Math.abs(gx - (b[0] + b[2])) <= reach;
         return (top ? 1 : 0) | (right ? 2 : 0);
      }
   }

   private MoveHudScreen.Target edgeTarget(float gx, float gy) {
      for (MoveHudScreen.Target t : this.targets) {
         if (this.edgeAt(t, gx, gy) != 0) {
            return t;
         }
      }

      return null;
   }

   private void press(float gx, float gy) {
      if (this.overDone(gx, gy)) {
         this.done();
      } else {
         MoveHudScreen.Target edged = this.edgeTarget(gx, gy);
         if (edged != null) {
            float[] b2 = edged.bounds();
            int edge = this.edgeAt(edged, gx, gy);
            this.resizing = edged;
            this.resizeTop = (edge & 1) != 0;
            this.resizeRight = (edge & 2) != 0;
            this.fixLeft = b2[0];
            this.fixBottom = b2[1] + b2[3];
         } else {
            MoveHudScreen.Target t = this.targetAt(gx, gy);
            float[] b = t == null ? null : t.bounds();
            if (b != null) {
               this.drag = t;
               this.grabX = gx - b[0];
               this.grabY = gy - b[1];
               this.ghostX = b[0];
               this.ghostY = b[1];
               this.ghostW = b[2];
               this.ghostH = b[3];
               this.nearest = t.island() ? FuriahsVisuals.instance.getWaterMark().islandAnchor() : null;
            }
         }
      }
   }

   private void release() {
      if (this.resizing != null) {
         this.resizing = null;
         ModSounds.gui(Sfx.SLIDER_RELEASE);
      } else if (this.drag != null) {
         WaterMarkModule wm;
         if (this.drag.island() && this.nearest != null && (wm = FuriahsVisuals.instance.getWaterMark()).islandAnchor() != this.nearest) {
            wm.setIslandAnchor(this.nearest);
         }

         this.drag = null;
         this.nearest = null;
         this.guideY = Float.NaN;
         this.guideX = Float.NaN;
         ModSounds.gui(Sfx.SLIDER_RELEASE);
      }
   }

   private void rightPress(float gx, float gy) {
      if (this.drag == null && this.resizing == null) {
         MoveHudScreen.Target t = this.targetAt(gx, gy);
         if (t != null) {
            t.reset();
            ModSounds.gui(Sfx.CLICK);
         }
      }
   }

   private void scroll(float gx, float gy, int notches) {
      MoveHudScreen.Target t = this.drag != null ? this.drag : this.targetAt(gx, gy);
      if (t != null) {
         SliderSetting s = t.size();
         double before = s.get();
         s.set(before + notches * s.getStep());
         if (s.get() != before) {
            ModSounds.gui(Sfx.SLIDER_TICK);
         }
      }
   }

   private void resizeTo(float gx, float gy) {
      float[] b = this.resizing.bounds();
      if (b != null && this.resizing.unavailable() == null) {
         float w = this.resizeRight ? Math.max(8.0F, gx - this.fixLeft) : b[2];
         float h = this.resizeTop ? Math.max(8.0F, this.fixBottom - gy) : b[3];
         this.resizing.resize(this.fixLeft, this.fixBottom, w, h, this.resizeRight, this.resizeTop);
      } else {
         this.resizing = null;
      }
   }

   private void dragTo(float gx, float gy) {
      float[] b = this.drag.bounds();
      if (b != null && this.drag.unavailable() == null) {
         float w = b[2];
         float h = b[3];
         float x = this.drag.vertical() ? b[0] : gx - this.grabX;
         float y = gy - this.grabY;
         x = clamp(x, 0.0F, this.sw - w);
         y = clamp(y, 0.0F, this.sh - h);
         this.guideY = Float.NaN;
         this.guideX = Float.NaN;
         if (!isShiftKeyDown() && !this.testFree) {
            if (!this.drag.vertical()) {
               x = this.snap(x, w, true);
            }

            y = this.snap(y, h, false);
         }

         if (this.drag.island()) {
            this.ghostX = x;
            this.ghostY = y;
            this.ghostW = w;
            this.ghostH = h;
            this.nearest = this.nearestPlace(x + w / 2.0F, y + h / 2.0F);
         } else {
            this.drag.move(x, y, w, h);
         }
      } else {
         this.drag = null;
         this.guideY = Float.NaN;
         this.guideX = Float.NaN;
      }
   }

   private void consider(float at, float line) {
      float d = line - at;
      if (Math.abs(d) < Math.abs(this.bestD)) {
         this.bestD = d;
         this.bestLine = line;
      }
   }

   private float snap(float pos, float size, boolean x) {
      float screen = x ? this.sw : this.sh;
      this.bestD = Float.MAX_VALUE;
      this.bestLine = Float.NaN;
      this.consider(pos, 0.0F);
      this.consider(pos + size / 2.0F, screen / 2.0F);
      this.consider(pos + size, screen);

      for (MoveHudScreen.Target t : this.targets) {
         float[] b;
         if (t != this.drag && (b = t.live()) != null) {
            float start = x ? b[0] : b[1];
            float length = x ? b[2] : b[3];

            for (int mine = 0; mine < 3; mine++) {
               float at = pos + size * mine / 2.0F;
               this.consider(at, start);
               this.consider(at, start + length / 2.0F);
               this.consider(at, start + length);
            }
         }
      }

      if (Math.abs(this.bestD) <= 8.0F / this.sf) {
         if (x) {
            this.guideX = this.bestLine;
         } else {
            this.guideY = this.bestLine;
         }

         return clamp(pos + this.bestD, 0.0F, screen - size);
      } else {
         return pos;
      }
   }

   private WaterMarkModule.Anchor nearestPlace(float cx, float cy) {
      WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
      WaterMarkModule.Anchor best = null;
      float bestD = Float.MAX_VALUE;

      for (WaterMarkModule.Anchor a : WaterMarkModule.Anchor.values()) {
         float[] r = wm.islandBoundsAt(a);
         float dy;
         float dx;
         float d;
         if (r != null && (d = (dx = r[0] + r[2] / 2.0F - cx) * dx + (dy = r[1] + r[3] / 2.0F - cy) * dy) < bestD) {
            bestD = d;
            best = a;
         }
      }

      return best;
   }

   private static float clamp(float v, float lo, float hi) {
      return hi < lo ? lo : Math.max(lo, Math.min(hi, v));
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      long now = ClientClock.nanos();
      float dt = this.lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastNanos) / 1.0E9F);
      this.lastNanos = now;
      this.readScreen();
      float gy;
      float gx;
      if (this.testMouse != null) {
         gx = this.testMouse[0];
         gy = this.testMouse[1];
      } else {
         gx = Mouse.getX() / this.sf;
         gy = (this.mc.displayHeight - Mouse.getY() - 1) / this.sf;
      }

      if (this.resizing != null) {
         this.resizeTo(gx, gy);
      } else if (this.drag != null) {
         this.dragTo(gx, gy);
      }

      MoveHudScreen.Target edged = this.resizing == null && this.drag == null ? this.edgeTarget(gx, gy) : null;
      this.hoverEdge = this.resizing != null ? (this.resizeTop ? 1 : 0) | (this.resizeRight ? 2 : 0) : (edged != null ? this.edgeAt(edged, gx, gy) : 0);
      this.hover = this.resizing != null ? this.resizing : (this.drag != null ? this.drag : (edged != null ? edged : this.targetAt(gx, gy)));
      this.appear = Easing.approach(this.appear, 1.0F, 7.0F, dt);
      Ui.ensureFonts();
      int accent = FuriahsVisuals.instance.getWaterMark().lookAccent(ColorSetting.clock()) | 0xFF000000;
      Theme.apply(FuriahsVisuals.instance.getTheme().get(), accent);
      float savedPx = Ui.px;
      float savedOffset = Ui.offsetY;
      float k = this.mp / this.sf;
      GlStateManager.pushMatrix();
      GlStateManager.scale(k, k, 1.0F);
      GlStateManager.disableLighting();
      GlStateManager.disableDepth();
      GlStateManager.disableAlpha();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      Ui.px = this.mp;
      Ui.offsetY = 0.0F;
      Ui.beginFrame();
      Ui.pushAlpha(Easing.outCubic(this.appear));

      try {
         this.draw(dt, accent, gx, gy);
      } finally {
         Ui.popAlpha();
         Ui.endFrame();
         Ui.px = savedPx;
         Ui.offsetY = savedOffset;
         GlStateManager.popMatrix();
         GlStateManager.enableAlpha();
         GlStateManager.enableDepth();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private void draw(float dt, int accent, float gx, float gy) {
      float u = this.sf / this.mp;
      float vw = this.mc.displayWidth / this.mp;
      float vh = this.mc.displayHeight / this.mp;
      float line = 1.0F / this.mp;
      if (this.drag != null) {
         int faint = ColorUtil.withAlpha(16777215, 0.12F);
         Ui.rect(this.sw / 2.0F * u - line / 2.0F, 0.0F, line, vh, faint);
         Ui.rect(0.0F, this.sh / 2.0F * u - line / 2.0F, vw, line, faint);
      }

      if (!Float.isNaN(this.guideX)) {
         Ui.rect(clamp(this.guideX * u - line / 2.0F, 0.0F, vw - line), 0.0F, line, vh, accent | 0xFF000000);
      }

      if (!Float.isNaN(this.guideY)) {
         Ui.rect(0.0F, clamp(this.guideY * u - line / 2.0F, 0.0F, vh - line), vw, line, accent | 0xFF000000);
      }

      if (this.drag != null && this.drag.island()) {
         WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();

         for (WaterMarkModule.Anchor a : WaterMarkModule.Anchor.values()) {
            float[] r = wm.islandBoundsAt(a);
            if (r != null) {
               boolean near = a == this.nearest;
               float rr = r[3] * u / 2.0F;
               Ui.roundRect(r[0] * u, r[1] * u, r[2] * u, r[3] * u, rr, ColorUtil.withAlpha(accent, near ? 0.22F : 0.05F));
               Ui.outline(r[0] * u, r[1] * u, r[2] * u, r[3] * u, rr, line, near ? accent | 0xFF000000 : ColorUtil.withAlpha(16777215, 0.35F));
            }
         }
      }

      for (MoveHudScreen.Target t : this.targets) {
         if (t != this.hover) {
            this.element(t, dt, accent, u);
         }
      }

      if (this.hover != null) {
         this.element(this.hover, dt, accent, u);
      }

      this.panel(dt, accent, gx * u, gy * u, vw, vh);
   }

   private void element(MoveHudScreen.Target t, float dt, int accent, float u) {
      boolean lit = t == this.hover;
      t.hoverT = Easing.approach(t.hoverT, lit ? 1.0F : 0.0F, 14.0F, dt);
      float[] b = t.live();
      if (b != null) {
         if (t == this.drag && t.island()) {
            b = new float[]{this.ghostX, this.ghostY, this.ghostW, this.ghostH};
         }

         float pad = 2.0F;
         float x = b[0] * u - pad;
         float y = b[1] * u - pad;
         float w = b[2] * u + pad * 2.0F;
         float h = b[3] * u + pad * 2.0F;
         float r = t.island() ? h / 2.0F : 4.0F;
         float e = t.hoverT;
         Ui.roundRect(x, y, w, h, r, ColorUtil.withAlpha(accent, 0.07F + 0.11F * e));
         Ui.outline(x, y, w, h, r, 1.0F / this.mp, ColorUtil.lerp(ColorUtil.withAlpha(16777215, 0.55F), accent | 0xFF000000, e));
         if (t.resizable() && e > 0.01F) {
            this.handles(x, y, w, h, accent, e, lit ? this.hoverEdge : 0);
         }

         String note = t.note();
         boolean sizing = t == this.resizing || lit && this.hoverEdge != 0;
         String text = t.name
            + (note != null ? " (" + note + ")" : "")
            + " · "
            + (sizing && t.sizeNote() != null ? t.sizeNote() : Math.round(t.size().get() * 100.0) + "%")
            + (t.vertical() && lit ? " · up and down" : "");
         float vw = this.mc.displayWidth / this.mp;
         float vh = this.mc.displayHeight / this.mp;
         float lw = Ui.width(text, 7.5F) + 12.0F;
         float lx = clamp(x, 2.0F, vw - lw - 2.0F);
         float ly = y - 13.0F - 3.0F;
         if (ly < 2.0F && (ly = y + h + 3.0F) + 13.0F > vh - 2.0F) {
            ly = y + 3.0F;
         }

         int bg = ColorUtil.lerp(-804121056, ColorUtil.withAlpha(ColorUtil.lerp(-15591904, accent, 0.35F), 0.92F), e);
         Ui.roundRect(lx, ly, lw, 13.0F, 6.5F, bg);
         Ui.textMiddle(text, lx + 6.0F, ly + 6.5F, 7.5F, -1, false);
      }
   }

   private void handles(float x, float y, float w, float h, int accent, float e, int edges) {
      float len = Math.min(18.0F, Math.min(w, h) * 0.4F);
      float th = 3.0F;
      float arm = Math.min(9.0F, Math.min(w, h) * 0.25F);
      int on = accent | 0xFF000000;
      int off = ColorUtil.withAlpha(16777215, 0.85F);
      Ui.roundRect(x + w / 2.0F - len / 2.0F, y - th / 2.0F, len, th, th / 2.0F, ColorUtil.mulAlpha(edges == 1 ? on : off, e));
      Ui.roundRect(x + w - th / 2.0F, y + h / 2.0F - len / 2.0F, th, len, th / 2.0F, ColorUtil.mulAlpha(edges == 2 ? on : off, e));
      int c = ColorUtil.mulAlpha(edges == 3 ? on : off, e);
      Ui.roundRect(x + w - arm, y - th / 2.0F, arm + th / 2.0F, th, th / 2.0F, c);
      Ui.roundRect(x + w - th / 2.0F, y - th / 2.0F, th, arm + th / 2.0F, th / 2.0F, c);
   }

   private void panel(float dt, int accent, float mx, float my, float vw, float vh) {
      ArrayList<String> off = new ArrayList<>();

      for (MoveHudScreen.Target t : this.targets) {
         String why = t.unavailable();
         if (why != null) {
            off.add(t.name + " (" + why + ")");
         } else if (t.bounds() == null) {
            off.add(t.name + " (not showing)");
         }
      }

      String hint1 = "Drag to move · Scroll to resize · Right-click to reset";
      String hint2 = "Shift: move freely · Drag the chat's top or right edge to size it";
      String offLine = off.isEmpty() ? null : "Not here: " + join(off);
      float ph = 92.0F + (offLine != null ? 13.0F : 0.0F);
      float px0 = (vw - 300.0F) / 2.0F;
      float py0 = (vh - ph) / 2.0F;
      this.panelA = Easing.approach(this.panelA, this.drag != null ? 0.18F : 1.0F, 10.0F, dt);
      Ui.pushAlpha(this.panelA);

      try {
         boolean light = Theme.LIGHT;
         int fill = light ? -185075461 : -300673249;
         int text = light ? -15065820 : -854792;
         int dim = light ? -10854288 : -5656643;
         Ui.shadow(px0, py0 + 6.0F, 300.0F, ph, 12.0F, 22.0F, light ? 805306368 : 1879048192);
         Ui.roundRect(px0, py0, 300.0F, ph, 12.0F, fill);
         Ui.outline(px0, py0, 300.0F, ph, 12.0F, 1.0F / this.mp, light ? 570425344 : 654311423);
         float cx = px0 + 150.0F;
         String title = "Move HUD";
         Ui.textMiddle(title, cx - Ui.widthBold(title, 11.0F) / 2.0F, py0 + 16.0F, 11.0F, text, true);
         Ui.textMiddle(hint1, cx - Ui.width(hint1, 7.6F) / 2.0F, py0 + 34.0F, 7.6F, dim, false);
         Ui.textMiddle(hint2, cx - Ui.width(hint2, 7.2F) / 2.0F, py0 + 46.0F, 7.2F, ColorUtil.withAlpha(dim, 0.8F), false);
         if (offLine != null) {
            String shown = Ui.ellipsize(offLine, 7.2F, 276.0F, false);
            Ui.textMiddle(shown, cx - Ui.width(shown, 7.2F) / 2.0F, py0 + 59.0F, 7.2F, ColorUtil.withAlpha(dim, 0.8F), false);
         }

         this.doneX = cx - 37.0F;
         this.doneY = py0 + ph - 20.0F - 12.0F;
         boolean over = this.panelA > 0.5F && mx >= this.doneX && mx <= this.doneX + 74.0F && my >= this.doneY && my <= this.doneY + 20.0F;
         this.doneHover = Easing.approach(this.doneHover, over ? 1.0F : 0.0F, 16.0F, dt);
         Ui.roundRect(this.doneX, this.doneY, 74.0F, 20.0F, 10.0F, ColorUtil.lerp(accent | 0xFF000000, Theme.hover(accent), this.doneHover));
         String done = "Done";
         Ui.textMiddle(done, cx - Ui.widthBold(done, 8.6F) / 2.0F, this.doneY + 10.0F, 8.6F, -1, true);
      } finally {
         Ui.popAlpha();
      }
   }

   private static String join(List<String> parts) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < parts.size(); i++) {
         if (i > 0) {
            sb.append(" · ");
         }

         sb.append(parts.get(i));
      }

      return sb.toString();
   }

   private MoveHudScreen.Target target(String id) {
      for (MoveHudScreen.Target t : this.targets) {
         if (t.key.equals(id) || id.equals("island") && t.island()) {
            return t;
         }
      }

      throw new IllegalArgumentException("no Move HUD element " + id);
   }

   public String pressForTest(String id, float fx, float fy) {
      this.readScreen();
      MoveHudScreen.Target t = this.target(id);
      float[] b = t.live();
      if (b == null) {
         return id + ": not shown (" + t.unavailable() + ")";
      } else {
         float gx = b[0] + b[2] * fx;
         float gy = b[1] + b[3] * fy;
         this.testMouse = new float[]{gx, gy};
         this.press(gx, gy);
         return id + ": pressed at " + fmt(gx) + ", " + fmt(gy) + (this.drag == t ? " (dragging)" : " (NOT dragging)");
      }
   }

   public void moveToForTest(String xs, String ys, boolean free) {
      if (this.testMouse != null && this.drag != null) {
         this.readScreen();
         float[] b = this.drag.bounds();
         if (b != null) {
            float x = place(xs, this.sw, b[2]);
            float y = place(ys, this.sh, b[3]);
            this.testMouse[0] = x + this.grabX;
            this.testMouse[1] = y + this.grabY;
            this.testFree = free;
         }
      }
   }

   private static float place(String v, float screen, float size) {
      if (v.equals("c")) {
         return (screen - size) / 2.0F;
      } else {
         float f = Float.parseFloat(v);
         return !(f < 0.0F) && !v.startsWith("-") ? f : screen + f - size;
      }
   }

   public void releaseForTest() {
      this.release();
      this.parkForTest();
      this.testFree = false;
   }

   public void parkForTest() {
      this.testMouse = new float[]{1.0F, 1.0F};
   }

   public String resizeForTest(String id, String edge, float dx, float dy) {
      this.readScreen();
      MoveHudScreen.Target t = this.target(id);
      float[] b = t.live();
      if (b == null) {
         return id + ": not shown (" + t.unavailable() + ")";
      } else {
         float gx = edge.equals("top") ? b[0] + b[2] / 2.0F : b[0] + b[2];
         float gy = edge.equals("right") ? b[1] + b[3] / 2.0F : b[1];
         String before = fmt(b[2]) + " x " + fmt(b[3]) + " (" + t.sizeNote() + ")";
         this.testMouse = new float[]{gx, gy};
         this.press(gx, gy);
         if (this.resizing != t) {
            this.parkForTest();
            return id + ": its " + edge + " edge didn't take the press (FAIL)";
         } else {
            String grabbed = (this.resizeTop ? "top" : "") + (this.resizeTop && this.resizeRight ? "+" : "") + (this.resizeRight ? "right" : "");
            float w = this.resizeRight ? Math.max(8.0F, gx + dx - this.fixLeft) : b[2];
            float h = this.resizeTop ? Math.max(8.0F, this.fixBottom - (gy + dy)) : b[3];
            float[] a = t.resize(this.fixLeft, this.fixBottom, w, h, this.resizeRight, this.resizeTop);
            this.release();
            this.parkForTest();
            return id
               + " "
               + edge
               + " (grabbed "
               + grabbed
               + ") by "
               + fmt(dx)
               + ", "
               + fmt(dy)
               + ": "
               + before
               + " -> "
               + fmt(a[2])
               + " x "
               + fmt(a[3])
               + " ("
               + t.sizeNote()
               + "), bottom-left "
               + fmt(b[0])
               + ", "
               + fmt(b[1] + b[3])
               + " -> "
               + fmt(a[0])
               + ", "
               + fmt(a[1] + a[3]);
         }
      }
   }

   public void scrollForTest(String id, int notches) {
      MoveHudScreen.Target t = this.target(id);
      float[] b = t.live();
      if (b != null) {
         this.scroll(b[0] + b[2] / 2.0F, b[1] + b[3] / 2.0F, notches);
      }
   }

   public void resetForTest(String id) {
      this.target(id).reset();
   }

   public void doneForTest() {
      this.done();
   }

   public String stateForTest() {
      this.readScreen();
      StringBuilder sb = new StringBuilder("screen ")
         .append(fmt(this.sw))
         .append(" x ")
         .append(fmt(this.sh))
         .append(" (scale ")
         .append((int)this.sf)
         .append(")");

      for (MoveHudScreen.Target t : this.targets) {
         sb.append("; ").append(t.name).append(": ");
         String why = t.unavailable();
         float[] b = t.bounds();
         if (why != null) {
            sb.append("not here (").append(why).append(")");
         } else if (b == null) {
            sb.append("not showing");
         } else {
            sb.append(fmt(b[0]))
               .append(", ")
               .append(fmt(b[1]))
               .append(" ")
               .append(fmt(b[2]))
               .append(" x ")
               .append(fmt(b[3]))
               .append(" at ")
               .append(Math.round(t.size().get() * 100.0))
               .append("%");
         }
      }

      sb.append("; island at ").append(FuriahsVisuals.instance.getWaterMark().islandAnchor());
      sb.append("; guides ").append(Float.isNaN(this.guideX) ? "-" : fmt(this.guideX)).append(" / ").append(Float.isNaN(this.guideY) ? "-" : fmt(this.guideY));
      return sb.toString();
   }

   private static String fmt(float v) {
      return String.format(Locale.ROOT, "%.1f", v);
   }

   private final class BarsTarget extends MoveHudScreen.Target {
      private final ItemHudElement e;
      private final Object handle = new Object();

      BarsTarget(ItemHudElement e) {
         super("Hearts & Hunger", "bars");
         this.e = e;
      }

      @Override
      Object element() {
         return this.handle;
      }

      @Override
      String unavailable() {
         return this.e.on() ? this.e.barsMissing() : "off";
      }

      @Override
      float[] bounds() {
         return this.e.barsBoundsGui();
      }

      @Override
      SliderSetting size() {
         return this.e.barsScaleSetting();
      }

      @Override
      void move(float x, float y, float w, float h) {
         this.e.placeBarsGui(x, y, w, h, MoveHudScreen.this.sw, MoveHudScreen.this.sh);
      }

      @Override
      void reset() {
         this.e.resetBarsPlace();
      }

      @Override
      String note() {
         return this.e.barsOnHotbar() ? "on the hotbar" : null;
      }
   }

   private final class ChatTarget extends MoveHudScreen.Target {
      private final ChatElement e;

      ChatTarget(ChatElement e) {
         super("Chat");
         this.e = e;
      }

      @Override
      Object element() {
         return this.e;
      }

      @Override
      String unavailable() {
         if (!this.e.on()) {
            return "off";
         } else if (!MoveHudScreen.this.ui.chatMovable()) {
            return LabyCompat.advancedChat() ? "LabyMod's Advanced Chat draws it" : "vanilla here";
         } else {
            return MoveHudScreen.this.mc.gameSettings.chatVisibility == EnumChatVisibility.HIDDEN ? "hidden in Chat Settings" : null;
         }
      }

      @Override
      float[] bounds() {
         return this.e.boundsGui();
      }

      @Override
      SliderSetting size() {
         return this.e.scaleSetting();
      }

      @Override
      void move(float x, float y, float w, float h) {
         this.e.placeGui(x, y, w, h, MoveHudScreen.this.sw, MoveHudScreen.this.sh);
      }

      @Override
      void reset() {
         this.e.resetPlace();
      }

      @Override
      boolean resizable() {
         return true;
      }

      @Override
      float[] resize(float left, float bottom, float w, float h, boolean changeW, boolean changeH) {
         return this.e.resizeGui(left, bottom, w, h, changeW, changeH, MoveHudScreen.this.sw, MoveHudScreen.this.sh);
      }

      @Override
      String sizeNote() {
         return this.e.sizeLabel();
      }
   }

   private final class IslandTarget extends MoveHudScreen.Target {
      private final WaterMarkModule wm;

      IslandTarget(WaterMarkModule wm) {
         super("WaterMark");
         this.wm = wm;
      }

      @Override
      Object element() {
         return this.wm;
      }

      @Override
      String unavailable() {
         return this.wm.isEnabled() ? null : "off";
      }

      @Override
      float[] bounds() {
         return this.wm.islandBounds();
      }

      @Override
      SliderSetting size() {
         return this.wm.islandScale();
      }

      @Override
      void move(float x, float y, float w, float h) {
      }

      @Override
      void reset() {
         this.wm.resetIslandPlace();
      }

      @Override
      boolean island() {
         return true;
      }
   }

   private final class ItemHudTarget extends MoveHudScreen.Target {
      private final ItemHudElement e;

      ItemHudTarget(ItemHudElement e) {
         super("Hotbar", "itemhud");
         this.e = e;
      }

      @Override
      Object element() {
         return this.e;
      }

      @Override
      String unavailable() {
         if (!this.e.on()) {
            return "off";
         } else {
            return MoveHudScreen.this.mc.thePlayer != null && MoveHudScreen.this.mc.thePlayer.isSpectator()
               ? "vanilla while spectating"
               : null;
         }
      }

      @Override
      float[] bounds() {
         return this.e.boundsGui();
      }

      @Override
      SliderSetting size() {
         return this.e.scaleSetting();
      }

      @Override
      void move(float x, float y, float w, float h) {
         this.e.placeGui(y, h, MoveHudScreen.this.sw, MoveHudScreen.this.sh);
      }

      @Override
      void reset() {
         this.e.resetPlace();
      }

      @Override
      boolean vertical() {
         return true;
      }
   }

   private final class ScoreboardTarget extends MoveHudScreen.Target {
      private final ScoreboardElement e;

      ScoreboardTarget(ScoreboardElement e) {
         super("Scoreboard");
         this.e = e;
      }

      @Override
      Object element() {
         return this.e;
      }

      @Override
      String unavailable() {
         if (!this.e.on()) {
            return "off";
         } else if (ScoreboardElement.labyModDraws()) {
            return "LabyMod's Scoreboard widget draws it";
         } else {
            return this.e.notice() != null ? "vanilla here" : null;
         }
      }

      @Override
      float[] bounds() {
         return this.e.boundsGui();
      }

      @Override
      SliderSetting size() {
         return this.e.scaleSetting();
      }

      @Override
      void move(float x, float y, float w, float h) {
         this.e.placeGui(x, y, w, h, MoveHudScreen.this.sw, MoveHudScreen.this.sh);
      }

      @Override
      void reset() {
         this.e.resetPlace();
      }

      @Override
      String note() {
         return this.e.showingSample() ? "sample" : null;
      }
   }

   private abstract class Target {
      final String name;
      final String key;
      float hoverT;

      Target(String name) {
         this(name, name.toLowerCase(Locale.ROOT).replace(" ", ""));
      }

      Target(String name, String key) {
         this.name = name;
         this.key = key;
      }

      abstract Object element();

      abstract String unavailable();

      abstract float[] bounds();

      abstract SliderSetting size();

      abstract void move(float var1, float var2, float var3, float var4);

      abstract void reset();

      boolean vertical() {
         return false;
      }

      boolean island() {
         return false;
      }

      String note() {
         return null;
      }

      boolean resizable() {
         return false;
      }

      float[] resize(float left, float bottom, float w, float h, boolean changeW, boolean changeH) {
         return this.bounds();
      }

      String sizeNote() {
         return null;
      }

      final float[] live() {
         return this.unavailable() == null ? this.bounds() : null;
      }
   }
}
