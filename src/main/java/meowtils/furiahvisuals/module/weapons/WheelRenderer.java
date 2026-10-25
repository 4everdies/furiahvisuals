package meowtils.furiahvisuals.module.weapons;

import java.util.Arrays;
import java.util.Locale;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.hud.HudGl;
import meowtils.furiahvisuals.hud.HudGlint;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.module.ui.UiModule;
import meowtils.furiahvisuals.module.watermark.IslandGlass;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

final class WheelRenderer {
   static final long OPEN_NANOS = 190000000L;
   static final long CLOSE_NANOS = 150000000L;
   static final long STAGGER_NANOS = 150000000L;
   static final long PAGE_NANOS = 260000000L;
   private static final float PAGE_TURN = 0.75F;
   private static final float MIN_RADIUS = 70.0F;
   private final WeaponWheel wheel;
   private final WheelSettings s;
   private final WheelContent content;
   private final HudLook look = new HudLook();
   private final HudGlint.Shine shine = new HudGlint.Shine();
   private final int[] shown = new int[24];
   private final float[] lit = new float[48];
   private long lastNanos;
   private float dt;
   private int pageFrom = -1;
   private int pageDir;
   private long pageAt;
   private float selectorShown;
   float sw;
   float sh;
   float cx;
   float cy;
   float radius;
   float inner;
   private boolean fitShrunk;
   private float iconPx;
   private float bigPx;
   private long picturesNanos;
   private long backdropNanos;
   private long costPictures;
   private long costBackdrop;
   private long costWheel;
   private long costMax;
   private long costFrames;
   private int bg;
   private int slotFill;
   private int slotEdge;
   private int outline;
   private int accent;
   private int text;
   private int dim;
   private int muted;
   private int glassTint;
   private boolean glass;
   private boolean border;
   private boolean shadow;
   private boolean glint;
   private boolean minimal;

   WheelRenderer(WeaponWheel wheel, WheelSettings settings, WheelContent content) {
      this.wheel = wheel;
      this.s = settings;
      this.content = content;
   }

   void opened() {
      Arrays.fill(this.lit, 0.0F);
      this.lastNanos = 0L;
      this.pageFrom = -1;
      this.selectorShown = 0.0F;
   }

   void closed() {
      this.content.idle();
   }

   void pageTurn(int from, int to, int dir) {
      this.pageFrom = from;
      this.pageDir = dir;
      this.pageAt = ClientClock.nanos();
   }

   float visibility() {
      long now = ClientClock.nanos();
      float open = Math.min(1.0F, (float)(now - this.wheel.openedAt) / 1.9E8F);
      float v = Easing.outCubic(Math.max(0.0F, open));
      if (this.wheel.state == WeaponWheel.State.CLOSING) {
         float c = Math.min(1.0F, (float)(now - this.wheel.closedAt) / 1.5E8F);
         v *= 1.0F - Easing.inCubic(c);
      } else if (this.wheel.state == WeaponWheel.State.CLOSED || this.wheel.state == WeaponWheel.State.PENDING) {
         v = 0.0F;
      }

      return v;
   }

   private float scale() {
      long now = ClientClock.nanos();
      float open = Math.min(1.0F, Math.max(0.0F, (float)(now - this.wheel.openedAt) / 1.9E8F));
      float k = 0.84F + 0.16F * Easing.outBack(open);
      if (this.wheel.state == WeaponWheel.State.CLOSING) {
         float c = Math.min(1.0F, (float)(now - this.wheel.closedAt) / 1.5E8F);
         k *= this.wheel.picked >= 0 ? 1.0F + 0.05F * Easing.outCubic(c) : 1.0F - 0.08F * Easing.inCubic(c);
      }

      return k;
   }

   private float popIn(int k, int n) {
      long since = ClientClock.nanos() - this.wheel.openedAt - (n <= 1 ? 0L : 150000000L * k / n);
      return Math.max(0.0F, Math.min(1.0F, (float)since / 1.52E8F));
   }

   private static float textSize(float size, float min, float max) {
      return Math.round(Math.max(min, Math.min(max, size)) * 2.0F) / 2.0F;
   }

   private void layout(float w, float h, boolean inGame) {
      this.sw = w;
      this.sh = h;
      this.cx = w / 2.0F;
      this.cy = h / 2.0F;
      float r = this.s.size.getFloat() / 100.0F * h / 2.0F;
      this.fitShrunk = false;
      float max;
      if (inGame && (max = this.roomInGame(r)) < r) {
         r = Math.max(70.0F, max);
         this.fitShrunk = true;
      }

      this.radius = r;
      this.inner = r * this.s.hole.getFloat() / 100.0F;
   }

   private float roomInGame(float wanted) {
      Minecraft mc = Minecraft.getMinecraft();
      float sf = scaleFactor(mc);
      float k = sf / Ui.px;
      float best = Float.MAX_VALUE;
      float[] hotbar = hotbarGui(mc, sf);
      if (hotbar != null) {
         best = Math.min(best, this.clearOf(hotbar, k, this.wheel.pages() > 1 ? 34.0F : 16.0F));
      }

      WaterMarkModule wm;
      float[] island = (wm = FuriahsVisuals.instance.getWaterMark()) == null ? null : wm.islandBounds();
      if (island != null) {
         best = Math.min(best, this.clearOf(island, k, 16.0F));
      }

      return best == Float.MAX_VALUE ? wanted : best;
   }

   private float clearOf(float[] gui, float k, float margin) {
      float x0 = gui[0] * k;
      float y0 = gui[1] * k;
      float x1 = (gui[0] + gui[2]) * k;
      float y1 = (gui[1] + gui[3]) * k;
      float dx = Math.max(x0 - this.cx, Math.max(0.0F, this.cx - x1));
      float dy = Math.max(y0 - this.cy, Math.max(0.0F, this.cy - y1));
      float d = (float)Math.sqrt(dx * dx + dy * dy);
      return (d - margin) / 1.1F;
   }

   private static float[] hotbarGui(Minecraft mc, float sf) {
      UiModule ui = FuriahsVisuals.instance.getUi();
      float[] b;
      if (ui != null && ui.itemHud() != null && ui.itemHud().on() && (b = ui.itemHud().boundsGui()) != null) {
         return b;
      } else {
         float gw = mc.displayWidth / sf;
         float gh = mc.displayHeight / sf;
         float tall = mc.playerController != null && mc.playerController.shouldDrawHUD() ? 29.0F : 22.0F;
         return new float[]{(int)gw / 2 - 91.0F, gh - tall, 182.0F, tall};
      }
   }

   private static float scaleFactor(Minecraft mc) {
      int limit = mc.gameSettings.guiScale == 0 ? 1000 : mc.gameSettings.guiScale;
      int f = 1;

      while (f < limit && mc.displayWidth / (f + 1) >= 320 && mc.displayHeight / (f + 1) >= 240) {
         f++;
      }

      if (mc.isUnicode() && f % 2 != 0 && f != 1) {
         f--;
      }

      return f;
   }

   private float slotIcon(float r0, float r1, int n) {
      float ring = r1 - r0;
      float mid = r0 + ring / 2.0F;
      float chord = (float)(2.0 * mid * Math.sin(Math.PI / Math.max(2, n)));
      float size = Math.max(14.0F, Math.min(ring * 0.86F, chord * 0.85F));
      return this.s.labels.get() == WeaponOptions.WheelLabels.UNDER ? size * 0.78F : size;
   }

   private float discOf(float r0, float R) {
      float pad = Math.max(3.0F, R * 0.035F);
      return Math.max(10.0F, r0 - pad - Math.max(4.0F, this.s.gap.getFloat() * 0.9F + 2.0F));
   }

   void prepare() {
      long start = System.nanoTime();
      Minecraft mc = Minecraft.getMinecraft();
      float px = mc.displayHeight / 1080.0F;
      if (!(px <= 0.0F)) {
         float saved = Ui.px;
         Ui.px = px;

         try {
            this.layout(mc.displayWidth / px, mc.displayHeight / px, !this.wheel.inMenu);
         } finally {
            Ui.px = saved;
         }

         int n = 0;
         boolean live = this.s.icons.get() == WeaponOptions.WheelIcons.LIVE;
         this.iconPx = this.slotIcon(this.inner, this.radius, this.wheel.pageCount(this.wheel.page)) * 1.15F * px;
         int[] nArray;
         if (this.pageFrom >= 0 && this.pageFrom != this.wheel.page && ClientClock.nanos() - this.pageAt < 260000000L) {
            int[] nArray2 = new int[]{this.wheel.page, 0};
            nArray = nArray2;
            nArray2[1] = this.pageFrom;
         } else {
            int[] nArray3 = new int[1];
            nArray = nArray3;
            nArray3[0] = this.wheel.page;
         }

         for (int p : nArray) {
            int first = this.wheel.pageStart(p);
            int count = this.wheel.pageCount(p);

            for (int i = 0; i < count && n < this.shown.length; i++) {
               this.shown[n++] = first + i;
            }
         }

         int middle = this.s.labels.get() == WeaponOptions.WheelLabels.CENTER
            ? (this.wheel.selected >= 0 && this.wheel.selected < this.wheel.size() ? this.wheel.selected : -1)
            : -2;
         this.bigPx = this.discOf(this.inner, this.radius) * 0.95F * px;
         this.content.prepare(this.shown, n, this.wheel.selected, middle, Math.round(this.iconPx), Math.round(this.bigPx), live, ClientClock.seconds());
         this.picturesNanos = System.nanoTime() - start;
      }
   }

   void drawBackdropHud() {
      long start = System.nanoTime();
      Minecraft mc = Minecraft.getMinecraft();
      float px = mc.displayHeight / 1080.0F;
      float savedPx = Ui.px;
      GlStateManager.matrixMode(5889);
      GlStateManager.pushMatrix();
      GlStateManager.loadIdentity();
      GlStateManager.ortho(0.0, mc.displayWidth, mc.displayHeight, 0.0, 1000.0, 3000.0);
      GlStateManager.matrixMode(5888);
      GlStateManager.pushMatrix();
      GlStateManager.loadIdentity();
      GlStateManager.translate(0.0F, 0.0F, -2000.0F);
      GlStateManager.scale(px, px, 1.0F);
      Ui.px = px;
      Ui.beginFrameQuick(HudGl.program());

      try {
         GlStateManager.disableAlpha();
         GlStateManager.depthFunc(519);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
         this.backdrop(mc.displayWidth / px, mc.displayHeight / px, this.visibility());
      } finally {
         Ui.endFrame();
         Ui.px = savedPx;
         GlStateManager.matrixMode(5889);
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.popMatrix();
         GlStateManager.depthFunc(515);
         restoreHudState();
      }

      this.backdropNanos = System.nanoTime() - start;
   }

   void drawHud() {
      long start = System.nanoTime();
      Minecraft mc = Minecraft.getMinecraft();
      float px = mc.displayHeight / 1080.0F;
      float sf = scaleFactor(mc);
      float savedPx = Ui.px;
      GlStateManager.pushMatrix();
      GlStateManager.scale(px / sf, px / sf, 1.0F);
      Ui.px = px;
      Ui.ensureFonts();
      Ui.beginFrameQuick(HudGl.program());

      try {
         GlStateManager.disableAlpha();
         GlStateManager.depthFunc(519);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
         this.layout(mc.displayWidth / px, mc.displayHeight / px, true);
         this.wheel(this.visibility());
      } finally {
         Ui.endFrame();
         Ui.px = savedPx;
         GlStateManager.popMatrix();
         GlStateManager.depthFunc(515);
         restoreHudState();
      }

      this.noteCost(System.nanoTime() - start);
   }

   void drawInMenu(float menuPx) {
      long start = System.nanoTime();
      Minecraft mc = Minecraft.getMinecraft();
      float px = mc.displayHeight / 1080.0F;
      float savedPx = Ui.px;
      GlStateManager.pushMatrix();
      GlStateManager.scale(px / menuPx, px / menuPx, 1.0F);
      Ui.px = px;

      try {
         float w = mc.displayWidth / px;
         float h = mc.displayHeight / px;
         float v = this.visibility();
         long b = System.nanoTime();
         this.backdrop(w, h, Math.max(v, 1.0E-4F));
         this.backdropNanos = System.nanoTime() - b;
         this.layout(w, h, false);
         this.wheel(v);
      } finally {
         Ui.px = savedPx;
         GlStateManager.popMatrix();
      }

      this.noteCost(System.nanoTime() - start - this.backdropNanos);
   }

   private void noteCost(long wheelNanos) {
      long total = this.picturesNanos + this.backdropNanos + wheelNanos;
      this.costPictures = this.costPictures + this.picturesNanos;
      this.costBackdrop = this.costBackdrop + this.backdropNanos;
      this.costWheel += wheelNanos;
      this.costMax = Math.max(this.costMax, total);
      this.costFrames++;
      this.backdropNanos = 0L;
      this.picturesNanos = 0L;
   }

   private static void restoreHudState() {
      GlStateManager.enableAlpha();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void backdrop(float w, float h, float v) {
      if (!(v <= 0.002F)) {
         if (!this.s.blur.get()) {
            Ui.rect(0.0F, 0.0F, w, h, ColorUtil.withAlpha(-16777216, 0.16F * v));
         } else {
            float strength = this.s.blurStrength.getFloat() / 100.0F;
            if (!IslandGlass.hasScreen()) {
               IslandGlass.captureScreen();
            }

            boolean glass = IslandGlass.hasScreen() && IslandGlass.draw(0.0F, 0.0F, w, h, 0.0F, ColorUtil.withAlpha(-16382198, 0.5F), 0, v * strength);
            if (!glass) {
               Ui.rect(0.0F, 0.0F, w, h, ColorUtil.withAlpha(-16777216, 0.4F * strength * v));
            }
         }
      }
   }

   private void colors(float dtSec) {
      WeaponOptions.WheelTheme theme = this.s.theme.get();
      this.look.update(HudLook.Mode.SAME, true, 1.0F, 0.0F, true, null, HudLook.Glint.SAME, Ui.px, dtSec);
      this.text = this.look.colors.text;
      this.dim = this.look.colors.dim;
      this.muted = this.look.colors.muted;
      boolean bl = this.minimal = theme == WeaponOptions.WheelTheme.MINIMAL;
      if (theme == WeaponOptions.WheelTheme.CUSTOM) {
         float opacity = this.s.backgroundOpacity.getFloat() / 100.0F;
         this.bg = ColorUtil.withAlpha(this.s.background.getColor(0.0F), opacity);
         int slot = this.s.slotColor.getColor(0.0F);
         this.slotFill = ColorUtil.withAlpha(slot, Math.max(0.35F, Math.min(1.0F, opacity + 0.12F)));
         this.slotEdge = ColorUtil.withAlpha(ColorUtil.lerp(slot, -1, 0.25F), 0.5F);
         this.accent = this.s.highlight.getColor(0.0F) | 0xFF000000;
         this.outline = this.look.colors.outline;
         this.border = this.s.border.get();
         this.glass = this.s.glass.get();
         this.glassTint = ColorUtil.withAlpha(this.s.background.getColor(0.0F), Math.max(0.3F, opacity));
         this.shadow = true;
         this.glint = false;
      } else {
         this.bg = this.look.colors.bg;
         int solid = this.bg | 0xFF000000;
         float a = ColorUtil.alpha(this.bg) / 255.0F;
         this.slotFill = ColorUtil.withAlpha(ColorUtil.lerp(solid, this.text, this.look.colors.light ? 0.06F : 0.1F), Math.min(1.0F, a * 0.85F + 0.12F));
         this.slotEdge = ColorUtil.withAlpha(this.look.colors.outline, 0.7F);
         this.accent = this.look.colors.accent;
         this.outline = this.look.colors.outline;
         this.border = this.look.outline;
         this.glass = this.look.glass;
         this.glassTint = this.look.colors.glassTint;
         this.shadow = this.look.shadow;
         this.glint = this.look.glint;
      }
   }

   private int accentAt(float fraction) {
      return this.s.theme.get() == WeaponOptions.WheelTheme.CUSTOM && this.s.highlight.getMode() != ColorSetting.Mode.STATIC
         ? this.s.highlight.getColor(fraction) | 0xFF000000
         : this.accent;
   }

   private void wheel(float v) {
      long now = ClientClock.nanos();
      this.dt = this.lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastNanos) / 1.0E9F);
      this.lastNanos = now;
      if (!(v <= 0.002F) && WheelShapes.ready()) {
         this.colors(this.dt);
         float k = this.scale();
         float R = this.radius * k;
         float r0 = this.inner * k;
         float pad = Math.max(3.0F, R * 0.035F);
         float ringIn = r0 - pad;
         float ringOut = R + pad;
         float disc = this.discOf(r0, R);
         float gap = this.s.gap.getFloat() * k;
         float round = Math.max(2.0F, Math.min(12.0F, (R - r0) * 0.14F));
         Ui.pushAlphaAbsolute(1.0F);

         try {
            if (!this.minimal) {
               if (this.shadow) {
                  WheelShapes.draw(
                     this.cx,
                     this.cy,
                     ringIn,
                     ringOut,
                     0.0,
                     0.0,
                     0.0F,
                     0.0F,
                     0.0F,
                     0.0F,
                     2,
                     22.0F * k,
                     ColorUtil.withAlpha(-16777216, (this.look.colors.light ? 0.18F : 0.38F) * v)
                  );
               }

               this.panel(ringIn, ringOut, v);
               this.panel(0.0F, disc, v);
               if (this.glint) {
                  this.glintOver(ringIn, ringOut, disc, v);
               }

               if (this.border) {
                  int line = ColorUtil.mulAlpha(this.outline, v);
                  WheelShapes.draw(this.cx, this.cy, ringIn, ringOut, 0.0, 0.0, 0.0F, 0.0F, 0.0F, 0.0F, 1, 1.0F / Ui.px, line);
                  WheelShapes.draw(this.cx, this.cy, 0.0F, disc, 0.0, 0.0, 0.0F, 0.0F, 0.0F, 0.0F, 1, 1.0F / Ui.px, line);
               }
            } else {
               int line = ColorUtil.withAlpha(this.text, 0.3F * v);
               WheelShapes.draw(this.cx, this.cy, R - 0.75F, R + 0.75F, 0.0, 0.0, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0.0F, line);
               WheelShapes.draw(this.cx, this.cy, r0 - 0.75F, r0 + 0.75F, 0.0, 0.0, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0.0F, line);
            }

            float turn = this.pageFrom >= 0 && this.pageFrom != this.wheel.page ? Math.min(1.0F, (float)(now - this.pageAt) / 2.6E8F) : 1.0F;
            if (turn < 1.0F) {
               float e = Easing.inOutCubic(turn);
               this.slotsOf(this.pageFrom, R, r0, gap, round, v * (1.0F - e), this.pageDir * e * 0.75F);
               this.slotsOf(this.wheel.page, R, r0, gap, round, v * e, -this.pageDir * (1.0F - e) * 0.75F);
            } else {
               this.pageFrom = -1;
               this.slotsOf(this.wheel.page, R, r0, gap, round, v, 0.0F);
            }

            this.selector(r0, ringIn, v);
            this.middle(disc, v);
            if (this.wheel.pages() > 1) {
               this.dots(ringOut, v);
            }
         } finally {
            Ui.popAlpha();
         }
      }
   }

   private void panel(float r0, float r1, float v) {
      boolean drawn = this.glass && WheelShapes.glass(this.cx, this.cy, r0, r1, 0.0, 0.0, 0.0F, 0.0F, 0.0F, 0.0F, this.glassTint, v);
      if (!drawn) {
         WheelShapes.draw(this.cx, this.cy, r0, r1, 0.0, 0.0, 0.0F, 0.0F, 0.0F, 0.0F, 0, 0.0F, ColorUtil.mulAlpha(this.bg, v));
      }
   }

   private void glintOver(float ringIn, float ringOut, float disc, float v) {
      float px = Ui.px;
      if (HudGlint.shine(
         this.cx * px,
         this.cy * px,
         ringOut * 2.0F * px,
         ringOut * 2.0F * px,
         this.look.colors.accent,
         this.look.global,
         this.look.colors.light,
         1.6F,
         this.shine
      )) {
         int color = ColorUtil.mulAlpha(this.shine.color, v);
         WheelShapes.glint(
            this.cx, this.cy, ringIn, ringOut, 0.0, 0.0, 0.0F, 0.0F, this.shine.band, this.shine.halfWidth, this.shine.angle, this.shine.trail, color
         );
         WheelShapes.glint(this.cx, this.cy, 0.0F, disc, 0.0, 0.0, 0.0F, 0.0F, this.shine.band, this.shine.halfWidth, this.shine.angle, this.shine.trail, color);
      }
   }

   private void slotsOf(int page, float R, float r0, float gap, float round, float v, float rotation) {
      int first = this.wheel.pageStart(page);
      int n = this.wheel.pageCount(page);
      if (n > 0 && !(v <= 0.002F)) {
         double step = (Math.PI * 2) / n;
         double half = n == 1 ? 0.0 : step / 2.0;
         float ring = R - r0;
         float icon = this.slotIcon(r0, R, n);
         boolean under = this.s.labels.get() == WeaponOptions.WheelLabels.UNDER;
         float ts = textSize((this.radius - this.inner) * 0.1F, 7.5F, 11.0F);

         for (int i = 0; i < n; i++) {
            int index = first + i;
            boolean on = index == this.wheel.selected && this.wheel.state != WeaponWheel.State.CLOSING || index == this.wheel.picked;
            this.lit[index] = Easing.approach(this.lit[index], on ? 1.0F : 0.0F, 18.0F, this.dt);
            float l = this.lit[index];
            float pop = this.popIn(i, n);
            float pv = v * Easing.outCubic(pop);
            if (!(pv <= 0.002F)) {
               double mid = i * step + rotation;
               float dx = (float)Math.sin(mid);
               float dy = (float)(-Math.cos(mid));
               float lift = l * ring * 0.08F - (1.0F - Easing.outBack(pop)) * ring * 0.18F;
               float lx = dx * lift;
               float ly = dy * lift;
               int hl = this.accentAt((float)i / n);
               if (!this.minimal) {
                  if (l > 0.01F) {
                     WheelShapes.draw(this.cx, this.cy, r0, R, mid, half, gap, round, lx, ly, 2, 16.0F, ColorUtil.withAlpha(hl, 0.55F * l * pv));
                  }

                  int fill = ColorUtil.lerp(
                     this.slotFill,
                     ColorUtil.withAlpha(ColorUtil.lerp(this.slotFill | 0xFF000000, hl, 0.32F), Math.max(ColorUtil.alpha(this.slotFill) / 255.0F, 0.85F)),
                     l
                  );
                  int edge = l > 0.01F ? ColorUtil.lerp(this.border ? this.slotEdge : this.slotEdge & 16777215, hl, l) : (this.border ? this.slotEdge : 0);
                  float width = l > 0.01F ? Math.max(1.0F, (float)Math.round(2.0F * Ui.px)) / Ui.px : 1.0F / Ui.px;
                  WheelShapes.fillBorder(
                     this.cx, this.cy, r0, R, mid, half, gap, round, lx, ly, ColorUtil.mulAlpha(fill, pv), width, ColorUtil.mulAlpha(edge, pv)
                  );
               } else {
                  if (n > 1) {
                     double b = mid - half;
                     float bx = (float)Math.sin(b);
                     float by = (float)(-Math.cos(b));
                     Ui.line(
                        this.cx + bx * (r0 + 2.0F),
                        this.cy + by * (r0 + 2.0F),
                        this.cx + bx * (R - 2.0F),
                        this.cy + by * (R - 2.0F),
                        1.2F,
                        true,
                        ColorUtil.withAlpha(this.text, 0.28F * pv)
                     );
                  }

                  if (l > 0.01F) {
                     WheelShapes.draw(this.cx, this.cy, r0, R, mid, half, gap, round, lx, ly, 2, 14.0F, ColorUtil.withAlpha(hl, 0.18F * l * pv));
                     float sweep = (float)Math.toDegrees(step) - 6.0F;
                     Ui.arc(this.cx + lx, this.cy + ly, R + 3.0F, 3.0F, (float)Math.toDegrees(mid) - sweep / 2.0F, sweep, true, ColorUtil.withAlpha(hl, l * pv));
                  }
               }

               float rad = r0 + ring * 0.5F + lift;
               float ix = this.cx + dx * rad;
               float iy = this.cy + dy * rad;
               if (under) {
                  iy -= ts * 0.6F;
               }

               float size = icon * (1.0F + 0.12F * l) * (0.6F + 0.4F * Easing.outBack(pop));
               this.picture(index, ix, iy, size, pv);
               if (under) {
                  String name = this.content.name(index);
                  float chord = (float)(2.0 * rad * Math.sin(Math.max(half, 0.2))) * 0.82F;
                  name = Ui.ellipsize(name, ts, Math.max(20.0F, chord), false);
                  float nw = Ui.width(name, ts);
                  Ui.textMiddle(name, ix - nw / 2.0F, iy + icon * 0.5F + ts * 0.55F, ts, ColorUtil.mulAlpha(ColorUtil.lerp(this.dim, this.text, l), pv), false);
               }
            }
         }
      }
   }

   private void picture(int index, float x, float y, float size, float a) {
      if (!this.content.picture(index, x, y, size, a)) {
         String name = this.content.name(index);
         Ui.circle(x, y, size * 0.22F, ColorUtil.withAlpha(this.text, 0.12F * a));
         String letter = name != null && !name.isEmpty() ? name.substring(0, 1) : "?";
         float ts = textSize((this.radius - this.inner) * 0.16F, 9.0F, 16.0F);
         Ui.textMiddle(letter, x - Ui.width(letter, ts) / 2.0F, y, ts, ColorUtil.withAlpha(this.dim, a), false);
      }
   }

   private void selector(float r0, float ringIn, float v) {
      float len = (float)Math.sqrt(this.wheel.selX * this.wheel.selX + this.wheel.selY * this.wheel.selY);
      boolean mouse = this.s.freezeCamera.get() || this.wheel.inMenu;
      this.selectorShown = Easing.approach(
         this.selectorShown, mouse && this.wheel.state == WeaponWheel.State.OPEN ? Math.min(1.0F, len / 0.34F) : 0.0F, 20.0F, this.dt
      );
      if (!(this.selectorShown < 0.02F)) {
         float deg = (float)Math.toDegrees(Math.atan2(this.wheel.selX, -this.wheel.selY));
         int color = ColorUtil.withAlpha(
            this.wheel.selected >= 0 ? this.accentAt(0.0F) : this.text, (this.wheel.selected >= 0 ? 0.95F : 0.45F) * this.selectorShown * v
         );
         float r = Math.max(6.0F, ringIn - 3.5F);
         Ui.arc(this.cx, this.cy, r, 2.6F, deg - 13.0F, 26.0F, true, color);
      }
   }

   private void middle(float disc, float v) {
      float steady = this.discOf(this.inner, this.radius);
      String rule = "→ " + this.content.target();
      float rs = textSize(steady * 0.12F, 8.0F, 11.0F);
      rule = Ui.ellipsize(rule, rs, steady * 1.5F, false);
      if (this.s.labels.get() != WeaponOptions.WheelLabels.CENTER) {
         Ui.textMiddle(rule, this.cx - Ui.width(rule, rs) / 2.0F, this.cy, rs, ColorUtil.mulAlpha(this.muted, v), false);
      } else {
         int sel = this.wheel.selected >= 0 && this.wheel.selected < this.wheel.size() ? this.wheel.selected : -1;
         boolean current = sel < 0;
         float a = current ? 0.6F : 1.0F;
         float big = disc * 1.02F;
         this.content.bigPicture(sel, this.cx, this.cy - disc * 0.3F, big, v * a);
         String name = this.content.name(sel);
         float ns = textSize(steady * 0.2F, 11.0F, 20.0F);
         name = Ui.ellipsize(name, ns, steady * 1.6F, true);
         float ny = this.cy + steady * 0.3F;
         Ui.textMiddle(name, this.cx - Ui.widthBold(name, ns) / 2.0F, ny, ns, ColorUtil.mulAlpha(this.text, v * (current ? 0.75F : 1.0F)), true);
         String sub = this.content.detail(sel);
         if (current) {
            sub = "Now: " + sub;
         }

         float ss = textSize(steady * 0.13F, 8.0F, 12.0F);
         sub = Ui.ellipsize(sub, ss, steady * 1.6F, false);
         Ui.textMiddle(sub, this.cx - Ui.width(sub, ss) / 2.0F, ny + ns * 0.95F, ss, ColorUtil.mulAlpha(this.dim, v), false);
         Ui.textMiddle(rule, this.cx - Ui.width(rule, rs) / 2.0F, this.cy + steady * 0.74F, rs, ColorUtil.mulAlpha(this.muted, v), false);
      }
   }

   private void dots(float ringOut, float v) {
      int n = this.wheel.pages();
      float y = this.cy + ringOut + 16.0F;
      float w = 0.0F;

      for (int i = 0; i < n; i++) {
         w += (i == this.wheel.page ? 18.0F : 7.0F) + (i > 0 ? 6.0F : 0.0F);
      }

      float x = this.cx - w / 2.0F;

      for (int i = 0; i < n; i++) {
         boolean on = i == this.wheel.page;
         float dw = on ? 18.0F : 7.0F;
         Ui.roundRect(x, y - 3.5F, dw, 7.0F, 3.5F, on ? ColorUtil.withAlpha(this.accentAt(0.0F), v) : ColorUtil.withAlpha(this.text, 0.35F * v));
         x += dw + 6.0F;
      }
   }

   String layoutForTest() {
      return String.format(
         Locale.ROOT,
         "radius %.0f, hole %.0f, middle at %.0f,%.0f of %.0fx%.0f, shrunk %s, picture %.0f px, middle %.0f px",
         this.radius,
         this.inner,
         this.cx,
         this.cy,
         this.sw,
         this.sh,
         this.fitShrunk ? "yes" : "no",
         this.iconPx,
         this.bigPx
      );
   }

   String costForTest() {
      double f = Math.max(1L, this.costFrames);
      String out = String.format(
         Locale.ROOT,
         "%.3f ms a frame (pictures %.3f, backdrop %.3f, wheel %.3f; max %.3f, %d frames)",
         this.costFrames == 0L ? 0.0 : (this.costPictures + this.costBackdrop + this.costWheel) / 1000000.0 / f,
         this.costPictures / 1000000.0 / f,
         this.costBackdrop / 1000000.0 / f,
         this.costWheel / 1000000.0 / f,
         this.costMax / 1000000.0,
         this.costFrames
      );
      this.costFrames = 0L;
      this.costMax = 0L;
      this.costWheel = 0L;
      this.costBackdrop = 0L;
      this.costPictures = 0L;
      return out;
   }
}
