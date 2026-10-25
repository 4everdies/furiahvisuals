package meowtils.furiahvisuals.gui;

import java.util.Locale;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.widget.Segmented;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class WelcomeGuide {
   static final int STEPS = 4;
   private static final String[] CAPTIONS = new String[]{"The look of this menu", "How much fits", "Your effects", "Finding your way"};
   private static final int[] SIZE_STEPS = new int[]{0, 90, 100, 110, 125};
   private final WelcomeGuide.Host host;
   private int step;
   private float appear;
   private float slide;
   private float x;
   private float y;
   private float w;
   private float h;
   private final Segmented themeSeg = new Segmented();
   private final Segmented viewSeg = new Segmented();
   private final Segmented sizeSeg = new Segmented();
   private float ctlX;
   private float ctlW;
   private float themeY = -10000.0F;
   private float viewY = -10000.0F;
   private float sizeY = -10000.0F;
   private float skipX;
   private float skipW;
   private float backX;
   private float backW;
   private float nextX;
   private float nextW;
   private float btnY;
   private float btnH;
   private float skipHover;
   private float backHover;
   private float nextHover;
   private String tip;

   WelcomeGuide(WelcomeGuide.Host host) {
      this.host = host;
   }

   int step() {
      return this.step;
   }

   boolean contains(float mx, float my) {
      return this.appear > 0.05F && mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + this.h;
   }

   String tooltip() {
      return this.tip;
   }

   private static float bodyText() {
      return 8.8F;
   }

   void render(float areaX, float areaW, float bottom, float mx, float my, float dt, int accent) {
      this.appear = Easing.approach(this.appear, 1.0F, 10.0F, dt);
      this.slide = Easing.approach(this.slide, 1.0F, 12.0F, dt);
      this.tip = null;
      this.w = Math.min(400.0F, areaW - 16.0F);
      this.x = areaX + (areaW - this.w) / 2.0F;
      this.h = this.cardHeight();
      this.y = bottom - this.h - 8.0F + (1.0F - Easing.outCubic(this.appear)) * 14.0F;
      boolean in = this.contains(mx, my);
      float hmx = in ? mx : -10000.0F;
      float hmy = in ? my : -10000.0F;
      Ui.pushAlpha(Easing.outCubic(this.appear));
      Ui.shadow(this.x, this.y + 4.0F, this.w, this.h, 12.0F, 18.0F, Theme.LIGHT ? 1073741824 : -1728053248);
      Ui.roundRect(this.x, this.y, this.w, this.h, 12.0F, Theme.TOOLTIP);
      Ui.outline(this.x, this.y, this.w, this.h, 12.0F, 1.0F, Theme.ACCENT_LINE);
      Ui.glow(this.x + 30.0F, this.y + 14.0F, 120.0F, ColorUtil.withAlpha(accent, 0.12F));
      float px = this.x + 16.0F;
      float ty = this.y + 16.0F;

      for (int i = 0; i < 4; i++) {
         float dx = px + i * 9.0F;
         Ui.circle(dx + 2.5F, ty, i == this.step ? 2.6F : 2.0F, i == this.step ? accent : ColorUtil.withAlpha(Theme.TEXT_MUTED, i < this.step ? 0.9F : 0.5F));
      }

      String cap = this.step + 1 + " of " + 4 + "  ·  " + CAPTIONS[this.step].toUpperCase(Locale.ROOT);
      Ui.textMiddle(cap, px + 36.0F + 6.0F, ty, 7.2F, Theme.TEXT_DIM, true);
      float cy = ty + 16.0F;
      Ui.pushAlpha(Easing.outCubic(this.slide));
      cy = this.drawStep(px, cy, this.w - 32.0F, hmx, hmy, dt, accent);
      Ui.popAlpha();
      this.drawButtons(hmx, hmy, dt, accent);
      Ui.popAlpha();
      if (this.step == 2) {
         this.pointAt(this.host.sectionSwitches(), accent);
      } else if (this.step == 3) {
         this.pointAt(this.host.globalSearch(), accent);
         this.pointAt(this.host.searchBox(), accent);
         this.pointAt(this.host.presetsButton(), accent);
      }
   }

   private float cardHeight() {
      switch (this.step) {
         case 0:
            return 116.0F;
         case 1:
            return 120.0F;
         case 2:
            return 104.0F;
         default:
            return 120.0F;
      }
   }

   float reserved() {
      return this.cardHeight() + 14.0F;
   }

   private float drawStep(float px, float cy, float tw, float mx, float my, float dt, int accent) {
      float ts = bodyText();
      this.sizeY = -10000.0F;
      this.viewY = -10000.0F;
      this.themeY = -10000.0F;
      this.ctlX = px;
      this.ctlW = tw;
      switch (this.step) {
         case 0:
            Ui.textBold("Welcome to Furiah's Visuals", px, cy - 2.0F, 12.0F, Theme.TEXT);
            float var20;
            Ui.text(
               Ui.ellipsize("Pick a look for this menu; it changes at once (Settings has it too).", ts, tw, false), px, var20 = cy + 18.0F, ts, Theme.TEXT_DIM
            );
            cy = var20 + 16.0F;
            Theme.Style[] styles = Theme.Style.values();
            String[] labels = new String[styles.length];

            for (int ix = 0; ix < styles.length; ix++) {
               labels[ix] = styles[ix].displayName();
            }

            this.themeY = cy;
            this.themeSeg.render(px, cy, tw, 20.0F, labels, FuriahsVisuals.instance.getTheme().get().ordinal(), mx, my, dt, accent);
            int kx = Segmented.hit(px, cy, tw, 20.0F, labels.length, mx, my);
            if (kx >= 0) {
               this.tip = styles[kx].description();
            }

            return cy + 20.0F;
         case 1:
            Ui.text(
               Ui.ellipsize("Thin rows fit the most; Auto sizes the menu to your window. Both change live.", ts, tw, false), px, cy - 4.0F, ts, Theme.TEXT_DIM
            );
            float lw = 70.0F;
            float cw = tw - lw;
            float var18;
            Ui.textMiddle("View theme", px, (var18 = cy + 12.0F) + 10.0F, ts, Theme.TEXT, false);
            String[] vl = new String[Density.Mode.values().length];

            for (int i = 0; i < vl.length; i++) {
               vl[i] = Density.Mode.values()[i].displayName();
            }

            this.ctlX = px + lw;
            this.ctlW = cw;
            this.viewY = var18;
            this.viewSeg.render(this.ctlX, var18, cw, 20.0F, vl, MenuPrefs.VIEW_THEME.get().ordinal(), mx, my, dt, accent);
            int k = Segmented.hit(this.ctlX, var18, cw, 20.0F, vl.length, mx, my);
            if (k >= 0) {
               this.tip = Density.Mode.values()[k].description();
            }

            Ui.textMiddle("GUI size", px, (cy = var18 + 26.0F) + 10.0F, ts, Theme.TEXT, false);
            String[] sl = new String[SIZE_STEPS.length];
            int sel = -1;

            for (int i = 0; i < SIZE_STEPS.length; i++) {
               String string = sl[i] = SIZE_STEPS[i] == 0 ? "Auto" : SIZE_STEPS[i] + "%";
               if (SIZE_STEPS[i] == 0 ? MenuPrefs.GUI_SCALE_AUTO.get() : !MenuPrefs.GUI_SCALE_AUTO.get() && MenuPrefs.GUI_SCALE.getInt() == SIZE_STEPS[i]) {
                  sel = i;
               }
            }

            this.sizeY = cy;
            this.sizeSeg.render(this.ctlX, cy, cw, 20.0F, sl, sel, mx, my, dt, accent);
            if (Segmented.hit(this.ctlX, cy, cw, 20.0F, sl.length, mx, my) >= 0) {
               this.tip = "The menu's size (Ctrl+scroll in the menu too; Ctrl+0 for Auto)";
            }

            return cy + 20.0F;
         case 2:
            Ui.text(
               Ui.ellipsize(
                  this.host.sidebarHidden()
                     ? "Open ☰ in the header: click a section's dot to switch it on,"
                     : "Click the dot at the end of a section in the sidebar to switch it on,",
                  ts,
                  tw,
                  false
               ),
               px,
               cy - 4.0F,
               ts,
               Theme.TEXT_DIM
            );
            Ui.text(Ui.ellipsize("and its name to set it up: every section starts with a good style picked.", ts, tw, false), px, cy + 9.0F, ts, Theme.TEXT_DIM);
            return cy + 22.0F;
         default:
            for (String[] l : new String[][]{
               {
                     "Search",
                     this.host.sidebarHidden() ? "the magnifier: every section; the box: this page" : "top of the sidebar: everything; top right: this page"
               },
               {"Presets", "the stacked layers at the bottom of the sidebar"},
               {"Move HUD", "in the UI and WaterMark headers"},
               {"This guide", "Settings › Interface"}
            }) {
               Ui.textMiddle(l[0], px, cy, ts, Theme.TEXT, true);
               Ui.textMiddle(Ui.ellipsize(l[1], ts, tw - 74.0F, false), px + 74.0F, cy, ts, Theme.TEXT_DIM, false);
               cy += 14.0F;
            }

            return cy;
      }
   }

   private void drawButtons(float mx, float my, float dt, int accent) {
      this.btnH = 18.0F;
      this.btnY = this.y + this.h - this.btnH - 12.0F;
      float bs = 8.4F;
      String next = this.step == 3 ? "Done" : "Next";
      this.nextW = Ui.width(next, bs) + 26.0F;
      this.nextX = this.x + this.w - 16.0F - this.nextW;
      this.backW = this.step > 0 ? Ui.width("Back", bs) + 22.0F : 0.0F;
      this.backX = this.nextX - 6.0F - this.backW;
      this.skipW = this.step < 3 ? Ui.width("Skip the guide", bs) + 4.0F : 0.0F;
      this.skipX = this.x + 16.0F;
      boolean overNext = inside(mx, my, this.nextX, this.btnY, this.nextW, this.btnH);
      boolean overBack = this.backW > 0.0F && inside(mx, my, this.backX, this.btnY, this.backW, this.btnH);
      boolean overSkip = this.skipW > 0.0F && inside(mx, my, this.skipX, this.btnY, this.skipW, this.btnH);
      this.nextHover = Easing.approach(this.nextHover, overNext ? 1.0F : 0.0F, 16.0F, dt);
      this.backHover = Easing.approach(this.backHover, overBack ? 1.0F : 0.0F, 16.0F, dt);
      this.skipHover = Easing.approach(this.skipHover, overSkip ? 1.0F : 0.0F, 16.0F, dt);
      Ui.roundRect(this.nextX, this.btnY, this.nextW, this.btnH, this.btnH / 2.0F, this.nextHover > 0.5F ? Theme.ACCENT_HOVER : Theme.ACCENT);
      Ui.textMiddle(next, this.nextX + (this.nextW - Ui.width(next, bs)) / 2.0F, this.btnY + this.btnH / 2.0F, bs, -1, false);
      if (this.backW > 0.0F) {
         Ui.surface(this.backX, this.btnY, this.backW, this.btnH, this.btnH / 2.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.backHover));
         Ui.textMiddle("Back", this.backX + 11.0F, this.btnY + this.btnH / 2.0F, bs, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.backHover), false);
      }

      if (this.skipW > 0.0F) {
         Ui.textMiddle("Skip the guide", this.skipX, this.btnY + this.btnH / 2.0F, bs, ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT, this.skipHover), false);
         if (overSkip) {
            this.tip = "Close the guide (Settings › Interface shows it again)";
         }
      }
   }

   private void pointAt(float[] r, int accent) {
      if (r != null && !(r[0] < -5000.0F)) {
         float t = (float)ClientClock.nanos() / 1.0E9F % 1.4F / 1.4F;
         float grow = 2.0F + 4.0F * t;
         float a = 0.85F * (1.0F - t);
         float w1 = r[2] + grow * 2.0F;
         float h1 = r[3] + grow * 2.0F;
         float w2 = r[2] + 4.0F;
         float h2 = r[3] + 4.0F;
         Ui.outline(r[0] - grow, r[1] - grow, w1, h1, Math.min(w1, h1) / 2.0F, 1.4F, ColorUtil.withAlpha(accent, a * this.appear));
         Ui.outline(r[0] - 2.0F, r[1] - 2.0F, w2, h2, Math.min(w2, h2) / 2.0F, 1.0F, ColorUtil.withAlpha(accent, 0.8F * this.appear));
      }
   }

   private static boolean inside(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         return false;
      } else if (button != 0) {
         return true;
      } else if (inside(mx, my, this.nextX, this.btnY, this.nextW, this.btnH)) {
         this.next();
         return true;
      } else if (this.backW > 0.0F && inside(mx, my, this.backX, this.btnY, this.backW, this.btnH)) {
         this.back();
         return true;
      } else if (this.skipW > 0.0F && inside(mx, my, this.skipX, this.btnY, this.skipW, this.btnH)) {
         ModSounds.gui(Sfx.CLICK);
         this.host.closed(false);
         return true;
      } else {
         if (this.step == 0) {
            int k = Segmented.hit(this.ctlX, this.themeY, this.ctlW, 20.0F, Theme.Style.values().length, mx, my);
            if (k >= 0) {
               FuriahsVisuals.instance.getTheme().set(Theme.Style.values()[k]);
               ModSounds.gui(Sfx.SELECT);
            }
         } else if (this.step == 1) {
            int k = Segmented.hit(this.ctlX, this.viewY, this.ctlW, 20.0F, Density.Mode.values().length, mx, my);
            if (k >= 0) {
               MenuPrefs.VIEW_THEME.set(Density.Mode.values()[k]);
               ModSounds.gui(Sfx.CLICK);
            }

            if ((k = Segmented.hit(this.ctlX, this.sizeY, this.ctlW, 20.0F, SIZE_STEPS.length, mx, my)) >= 0) {
               if (SIZE_STEPS[k] == 0) {
                  MenuPrefs.GUI_SCALE_AUTO.set(true);
               } else {
                  MenuPrefs.GUI_SCALE.set(SIZE_STEPS[k]);
                  MenuPrefs.GUI_SCALE_AUTO.set(false);
               }

               ModSounds.gui(Sfx.CLICK);
            }
         }

         return true;
      }
   }

   void next() {
      if (this.step == 3) {
         ModSounds.gui(Sfx.SUCCESS);
         this.host.closed(true);
      } else {
         this.step++;
         this.slide = 0.0F;
         ModSounds.gui(Sfx.CLICK);
      }
   }

   void back() {
      if (this.step > 0) {
         this.step--;
         this.slide = 0.0F;
         ModSounds.gui(Sfx.CLICK);
      }
   }

   boolean key(int keyCode) {
      if (keyCode == 1) {
         this.host.closed(false);
         return true;
      } else if (keyCode == 28 || keyCode == 156 || keyCode == 205) {
         this.next();
         return true;
      } else if (keyCode == 203) {
         this.back();
         return true;
      } else {
         return false;
      }
   }

   interface Host {
      float[] sectionSwitches();

      boolean sidebarHidden();

      float[] searchBox();

      float[] globalSearch();

      float[] presetsButton();

      void closed(boolean var1);
   }
}
