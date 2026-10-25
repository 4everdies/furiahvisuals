package meowtils.furiahvisuals.module.watermark;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.UiProbe;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;

final class ModulesPanel {
   static final int ACT_BACK = 600;
   static final int ACT_ALL_ON = 601;
   static final int ACT_ALL_OFF = 602;
   static final int ACT_UNDO = 603;
   static final int ACT_CUSTOMIZE = 604;
   static final int ACT_SETUP = 610;
   static final int ACT_ROW = 640;
   static final int ACT_END = 700;
   private static final long UNDO_MS = 5000L;
   private static final long REFRESH_MS = 250L;
   private final WaterMarkModule wm;
   private final List<IslandItem> items = new ArrayList<>();
   private final List<String> details = new ArrayList<>();
   private final List<Boolean> setup = new ArrayList<>();
   private final List<float[]> rowAnim = new ArrayList<>();
   private long detailsAt;
   private int columns = 1;
   private int perColumn = 1;
   private float colW;
   private float nameW;
   private float detailW;
   private float rowH;
   private float rowGap;
   private float colGap;
   private float headerH;
   private float footerH;
   private float contentH;
   private float width;
   private float height;
   private float laidFor = -1.0F;
   private int laidColumns = -1;
   private int laidCount = -1;
   private float scroll;
   private float scrollTarget;
   private float scrollMax;
   private boolean[] undo;
   private long undoUntil;
   private float px;
   private float py;
   private float pw;
   private float ph;
   private float allOnX = -1.0F;
   private float allOnW;
   private float allOffX = -1.0F;
   private float allOffW;
   private float undoX = -1.0F;
   private float undoW;
   private float backW;
   private float linkX;
   private float linkW;

   ModulesPanel(WaterMarkModule wm) {
      this.wm = wm;
   }

   private void collect() {
      this.items.clear();
      this.items.addAll(this.wm.middleItems());

      while (this.rowAnim.size() < this.items.size()) {
         this.rowAnim.add(new float[]{-1.0F, 0.0F});
      }
   }

   void reset() {
      this.scroll = 0.0F;
      this.scrollTarget = 0.0F;
      this.laidFor = -1.0F;
      this.detailsAt = 0L;
      this.undo = null;
      this.undoUntil = 0L;

      for (float[] a : this.rowAnim) {
         a[0] = -1.0F;
      }
   }

   private void refreshDetails() {
      long now = System.currentTimeMillis();
      if (now - this.detailsAt >= 250L || this.details.size() != this.items.size()) {
         this.detailsAt = now;
         this.details.clear();
         this.setup.clear();
         boolean live = this.wm.panelLiveValues.get();

         for (IslandItem item : this.items) {
            String needs = item.panelSetup();
            if (needs != null) {
               this.details.add(needs);
               this.setup.add(Boolean.TRUE);
            } else {
               String value = live && this.wm.isShown(item) ? item.panelValue() : null;
               this.details.add(value != null && !value.isEmpty() ? value : item.panelDescription());
               this.setup.add(Boolean.FALSE);
            }
         }
      }
   }

   private int columnsFor(IslandDraw d, IslandStyle s, float sw, float minW, int n) {
      WaterMarkModule.PanelColumns mode = this.wm.panelColumns.get();
      if (mode != WaterMarkModule.PanelColumns.AUTO) {
         return Math.max(1, Math.min(n, mode.count));
      } else {
         float room = sw * 0.7F;
         float one = this.colWidth(d, s, 1.0F);
         float scale = this.wm.scale.getFloat();
         if (sw >= 2300.0F && 3.0F * one + 2.0F * s.pad * 1.6F + 2.0F * s.pad <= room) {
            return Math.min(3, n);
         } else {
            return !(scale >= 1.6F) && !(sw < 900.0F) && !(2.0F * one + s.pad * 1.6F + 2.0F * s.pad > room) ? Math.min(2, n) : 1;
         }
      }
   }

   private float colWidth(IslandDraw d, IslandStyle s, float detailShare) {
      return s.icon + s.gap + this.nameW + s.gap * 2.0F + this.detailW * detailShare + s.gap * 2.0F + switchW(s);
   }

   private static float switchW(IslandStyle s) {
      return s.icon * 1.9F;
   }

   private static float switchH(IslandStyle s) {
      return s.icon * 1.05F;
   }

   float[] measure(IslandDraw d, IslandStyle s, float sw, float sh, float minW, float maxH) {
      this.collect();
      this.refreshDetails();
      int n = this.items.size();
      int px = d.pixels(s.small);
      if (this.laidFor != px || this.laidColumns != this.wm.panelColumns.get().ordinal() || this.laidCount != n) {
         this.laidFor = px;
         this.laidColumns = this.wm.panelColumns.get().ordinal();
         this.laidCount = n;
         this.nameW = 0.0F;
         this.detailW = 0.0F;

         for (int i = 0; i < n; i++) {
            IslandItem item = this.items.get(i);
            this.nameW = Math.max(this.nameW, d.width(true, s.small, item.name));
            this.detailW = Math.max(this.detailW, d.width(false, s.small, this.details.get(i)));
            this.detailW = Math.max(this.detailW, d.width(false, s.small, item.panelDescription()));
         }

         this.detailW = Math.min(Math.max(this.detailW, d.width(false, s.small, "000 ms  ")), s.font * 11.0F);
         this.rowH = Math.round(s.height * 0.9F);
         this.rowGap = Math.max(1.0F, s.gap * 0.4F);
         this.colGap = s.pad * 1.6F;
         this.headerH = s.height;
         this.footerH = Math.round(s.height * 0.85F);
         this.columns = this.columnsFor(d, s, sw, minW, n);
         this.perColumn = Math.max(1, (n + this.columns - 1) / this.columns);
      }

      float pad = s.pad;
      float share = 1.0F;
      float most = sw * 0.7F;
      this.colW = this.colWidth(d, s, share);
      float want = pad + this.columns * this.colW + (this.columns - 1) * this.colGap + pad;
      if (want > most) {
         float over = want - most;
         float cut = Math.min(over / this.columns, this.detailW * 0.7F);
         this.colW -= cut;
         want = pad + this.columns * this.colW + (this.columns - 1) * this.colGap + pad;
      }

      this.width = Math.max(minW, Math.min(want, Math.max(minW, most)));
      if (this.width > want) {
         this.colW = this.colW + (this.width - want) / this.columns;
      }

      this.contentH = this.perColumn * this.rowH + Math.max(0, this.perColumn - 1) * this.rowGap;
      float full = this.headerH + s.gap + this.contentH + s.gap + this.footerH;
      this.height = Math.min(full, Math.max(this.headerH + this.footerH + this.rowH, maxH));
      this.scrollMax = Math.max(0.0F, full - this.height);
      this.scrollTarget = Math.max(0.0F, Math.min(this.scrollMax, this.scrollTarget));
      this.scroll = Math.max(0.0F, Math.min(this.scrollMax, this.scroll));
      return new float[]{this.width, this.height};
   }

   float scroll() {
      return this.scroll;
   }

   void draw(IslandDraw d, IslandStyle s, float x, float y, float w, float h, float headerA, float rowsT, float rowsA, IslandItem.Hit hover, float dt) {
      this.collect();
      this.refreshDetails();
      this.px = x;
      this.py = y;
      this.pw = w;
      this.ph = h;
      float base = d.getAlpha();
      float cy = y + this.headerH / 2.0F;
      this.scroll = this.scroll + (this.scrollTarget - this.scroll) * Math.min(1.0F, dt * 18.0F);
      if (Math.abs(this.scrollTarget - this.scroll) < 0.05F) {
         this.scroll = this.scrollTarget;
      }

      d.setAlpha(base * headerA);
      float ch = s.height * 0.72F;
      float lx = x + s.pad * 0.6F;
      this.backW = s.gap * 1.2F + s.icon * 0.95F + s.gap * 1.2F;
      control(d, lx, cy - ch / 2.0F, this.backW, ch, "panel ‹");
      d.roundRect(lx, cy - ch / 2.0F, this.backW, ch, ch / 2.0F, ColorUtil.withAlpha(s.text, s.light ? 0.08F : 0.1F));
      float ic = s.icon * 0.95F;
      d.icon(IslandIcons.Icon.BACK, lx + (this.backW - ic) / 2.0F, cy - ic / 2.0F, ic, s.text);
      float tx = lx + this.backW + s.pad * 0.8F;
      d.textMiddle(true, s.small, "Modules", tx, cy, s.text);
      tx += d.width(true, s.small, "Modules");
      int on = 0;

      for (IslandItem item : this.items) {
         if (this.wm.isShown(item)) {
            on++;
         }
      }

      String count = " · " + on + " of " + this.items.size() + " on";
      d.textMiddle(false, s.small, count, tx, cy, s.muted);
      float rx = x + w - s.pad * 0.6F;
      boolean undoing = this.undo != null && ClientClock.millis() < this.undoUntil;
      if (undoing) {
         this.undoW = this.chipW(d, s, "Undo");
         this.undoX = rx - this.undoW;
         this.chip(d, s, "Undo", this.undoX, cy, this.undoW, true);
         this.allOffX = -1.0F;
         this.allOnX = -1.0F;
      } else {
         this.undo = null;
         this.undoX = -1.0F;
         this.allOffW = this.chipW(d, s, "All off");
         this.allOffX = rx - this.allOffW;
         this.allOnW = this.chipW(d, s, "All on");
         this.allOnX = this.allOffX - s.gap - this.allOnW;
         this.chip(d, s, "All on", this.allOnX, cy, this.allOnW, false);
         this.chip(d, s, "All off", this.allOffX, cy, this.allOffW, false);
      }

      float lineY = y + this.headerH;
      d.rect(x + s.pad, lineY, w - s.pad * 2.0F, 1.0F / d.pixelsPerUnit(), ColorUtil.mulAlpha(s.separator, headerA));
      float top = lineY + s.gap;
      float bottom = y + h - this.footerH;
      float rowsTop = top - this.scroll;
      d.flush();
      Ui.pushScissor(d.X(x), d.Y(top), d.L(w), Math.max(0.0F, d.L(bottom - top)));

      try {
         for (int i = 0; i < this.items.size(); i++) {
            int col = i / this.perColumn;
            int row = i % this.perColumn;
            float rx0 = x + s.pad + col * (this.colW + this.colGap);
            float ry = rowsTop + row * (this.rowH + this.rowGap);
            float t;
            float a;
            if (!(ry + this.rowH < top - 1.0F)
               && !(ry > bottom + 1.0F)
               && !((a = rowsA * Easing.outCubic(t = rowsT < 0.0F ? 1.0F : Easing.clamp01((rowsT - row * 0.02F - col * 0.008F) / 0.12F))) <= 0.003F)) {
               d.setAlpha(base * a);
               this.drawRow(d, s, i, rx0, ry + (1.0F - Easing.outCubic(t)) * 4.0F, this.colW, hover, dt);
            }
         }

         d.flush();
      } finally {
         Ui.popScissor();
      }

      if (this.scrollMax > 0.5F) {
         float trackH;
         float view = trackH = bottom - top - 4.0F;
         float content = view + this.scrollMax;
         float barH = Math.max(12.0F, trackH * view / content);
         float by = top + 2.0F + (trackH - barH) * (this.scroll / this.scrollMax);
         d.setAlpha(base * rowsA);
         d.roundRect(x + w - s.pad * 0.45F - 2.0F, by, 2.5F, barH, 1.25F, ColorUtil.withAlpha(s.text, 0.35F));
      }

      d.setAlpha(base * headerA);
      String link = "Customize in the mod menu ›";
      this.linkW = d.width(false, s.small, link);
      this.linkX = x + w - s.pad - this.linkW;
      float fy = y + h - this.footerH / 2.0F;
      boolean overLink = hover.action == 604;
      control(d, this.linkX - 3.0F, fy - this.footerH / 2.0F + 2.0F, this.linkW + 6.0F, this.footerH - 4.0F, "panel link");
      d.textMiddle(false, s.small, link, this.linkX, fy, overLink ? s.text : s.muted);
      d.setAlpha(base);
   }

   static void control(IslandDraw d, float x, float y, float w, float h, String label) {
      if (UiProbe.recording() && d.getAlpha() >= 0.5F) {
         Ui.registerControl(d.X(x), d.Y(y), d.L(w), d.L(h), label);
      }
   }

   private float chipW(IslandDraw d, IslandStyle s, String label) {
      return s.gap * 1.6F + d.width(false, s.small, label) + s.gap * 1.6F;
   }

   private void chip(IslandDraw d, IslandStyle s, String label, float x, float cy, float w, boolean accent) {
      float ch = s.height * 0.72F;
      int fill = accent ? ColorUtil.withAlpha(s.accentAt(x + w / 2.0F), s.light ? 0.16F : 0.22F) : ColorUtil.withAlpha(s.text, s.light ? 0.08F : 0.1F);
      control(d, x, cy - ch / 2.0F, w, ch, "panel " + label);
      d.roundRect(x, cy - ch / 2.0F, w, ch, ch / 2.0F, fill);
      d.textMiddle(false, s.small, label, x + s.gap * 1.6F, cy, s.text);
   }

   private void drawRow(IslandDraw d, IslandStyle s, int i, float x, float y, float w, IslandItem.Hit hover, float dt) {
      IslandItem item = this.items.get(i);
      boolean on = this.wm.isShown(item);
      float[] an = this.rowAnim.get(i);
      an[0] = an[0] < 0.0F ? (on ? 1.0F : 0.0F) : Easing.approach(an[0], on ? 1.0F : 0.0F, 16.0F, dt);
      boolean over = hover.action == 640 + i || hover.action == 610 + i;
      an[1] = Easing.approach(an[1], over ? 1.0F : 0.0F, 18.0F, dt);
      float cy = y + this.rowH / 2.0F;
      int accent = s.accentAt(x + w / 2.0F);
      control(d, x - s.gap, y, w + s.gap * 2.0F, this.rowH, "row " + item.name);
      if (an[1] > 0.01F) {
         d.roundRect(x - s.gap, y, w + s.gap * 2.0F, this.rowH, Math.min(this.rowH / 2.0F, 7.0F), ColorUtil.mulAlpha(s.hover, an[1]));
      }

      int iconC = ColorUtil.lerp(s.dim, accent, an[0]);
      int nameC = ColorUtil.lerp(s.dim, s.text, an[0]);
      float ic = s.icon;
      IslandIcons.Icon icon = iconOf(item);
      if (icon != null) {
         d.icon(icon, x, cy - ic / 2.0F, ic, iconC);
      } else {
         float tw = d.width(true, s.small, "Aa");
         d.textMiddle(true, s.small, "Aa", x + (ic - tw) / 2.0F, cy, iconC);
      }

      float nx = x + ic + s.gap;
      d.textMiddle(true, s.small, item.name, nx, cy, nameC);
      float dx = nx + this.nameW + s.gap * 2.0F;
      float sw = switchW(s);
      float sh = switchH(s);
      float room = x + w - sw - s.gap * 2.0F - dx;
      String detail = this.details.get(i);
      boolean link = this.setup.get(i);
      if (room > 4.0F) {
         String shown = ellipsize(d, s, detail, room);
         d.textMiddle(false, s.small, shown, dx, cy, link ? accent : s.muted);
      }

      float sx = x + w - sw;
      float sy = cy - sh / 2.0F;
      control(d, sx, sy, sw, sh, "switch " + item.name);
      drawSwitch(d, s, sx, sy, sw, sh, an[0], accent);
   }

   static void drawSwitch(IslandDraw d, IslandStyle s, float sx, float sy, float sw, float sh, float on, int accent) {
      int track = ColorUtil.lerp(ColorUtil.withAlpha(s.text, 0.2F), accent, on);
      d.roundRect(sx, sy, sw, sh, sh / 2.0F, track);
      float r = sh / 2.0F - 1.6F;
      float kx = sx + 1.6F + r + (sw - 3.2F - 2.0F * r) * on;
      d.circle(kx, sy + sh / 2.0F, r, -1);
   }

   private static String ellipsize(IslandDraw d, IslandStyle s, String text, float room) {
      if (d.width(false, s.small, text) <= room) {
         return text;
      } else {
         for (int end = text.length() - 1; end > 0; end--) {
            String t = text.substring(0, end).trim() + "…";
            if (d.width(false, s.small, t) <= room) {
               return t;
            }
         }

         return "…";
      }
   }

   static IslandIcons.Icon iconOf(IslandItem item) {
      return item.iconChoice != null && item.iconChoice.get() != null ? item.iconChoice.get() : item.listIcon;
   }

   boolean hit(float mx, float my, IslandStyle s, IslandItem.Hit hit) {
      if (!(mx < this.px) && !(mx > this.px + this.pw) && !(my < this.py) && !(my > this.py + this.ph)) {
         float cy = this.py + this.headerH / 2.0F;
         float ch = s.height * 0.72F;
         if (my < this.py + this.headerH) {
            float lx = this.px + s.pad * 0.6F;
            if (mx >= lx && mx < lx + this.backW) {
               hit.set(null, lx, cy - ch / 2.0F, this.backW, ch, 600, "Back to the quick settings (Esc)", true);
               return true;
            } else if (this.undoX >= 0.0F && mx >= this.undoX && mx < this.undoX + this.undoW) {
               hit.set(null, this.undoX, cy - ch / 2.0F, this.undoW, ch, 603, "Every module back as it was", true);
               return true;
            } else if (this.allOnX >= 0.0F && mx >= this.allOnX && mx < this.allOnX + this.allOnW) {
               hit.set(null, this.allOnX, cy - ch / 2.0F, this.allOnW, ch, 601, "Turn every module on", true);
               return true;
            } else if (this.allOffX >= 0.0F && mx >= this.allOffX && mx < this.allOffX + this.allOffW) {
               hit.set(null, this.allOffX, cy - ch / 2.0F, this.allOffW, ch, 602, "Turn every module off", true);
               return true;
            } else {
               hit.set(null, this.px, this.py, this.pw, this.headerH, 0, null, false);
               return true;
            }
         } else {
            float fy = this.py + this.ph - this.footerH;
            if (my >= fy) {
               if (mx >= this.linkX - 2.0F && mx < this.linkX + this.linkW + 2.0F) {
                  hit.set(
                     null,
                     this.linkX - 3.0F,
                     fy + 2.0F,
                     this.linkW + 6.0F,
                     this.footerH - 4.0F,
                     604,
                     "Opens the menu at WaterMark › Modules (order, options, icons)",
                     true
                  );
                  return true;
               } else {
                  hit.set(null, this.px, fy, this.pw, this.footerH, 0, null, false);
                  return true;
               }
            } else {
               float top = this.py + this.headerH + s.gap;
               float rowsTop = top - this.scroll;

               for (int i = 0; i < this.items.size(); i++) {
                  int col = i / this.perColumn;
                  int row = i % this.perColumn;
                  float rx0 = this.px + s.pad + col * (this.colW + this.colGap);
                  float ry = rowsTop + row * (this.rowH + this.rowGap);
                  if (mx >= rx0 - s.gap && mx < rx0 + this.colW + s.gap && my >= Math.max(ry, top) && my < Math.min(ry + this.rowH, fy)) {
                     IslandItem item = this.items.get(i);
                     boolean on = this.wm.isShown(item);
                     if (this.setup.size() > i && this.setup.get(i) && mx < rx0 + this.colW - switchW(s) - s.gap) {
                        hit.set(
                           null,
                           rx0 - s.gap,
                           ry,
                           this.colW + s.gap * 2.0F,
                           this.rowH,
                           610 + i,
                           item.name + ": " + this.details.get(i).replace(" ›", "") + " (opens its page in the menu)",
                           true
                        );
                        return true;
                     }

                     hit.set(
                        null,
                        rx0 - s.gap,
                        ry,
                        this.colW + s.gap * 2.0F,
                        this.rowH,
                        640 + i,
                        item.name + (on ? ": on (click to turn off)" : ": off (click to turn on)"),
                        true
                     );
                     return true;
                  }
               }

               hit.set(null, this.px, this.py, this.pw, this.ph, 0, null, false);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   boolean wheel(int dwheel) {
      if (!(this.scrollMax <= 0.5F) && dwheel != 0) {
         this.scrollTarget = Math.max(0.0F, Math.min(this.scrollMax, this.scrollTarget - Math.signum((float)dwheel) * (this.rowH + this.rowGap) * 2.0F));
         return true;
      } else {
         return false;
      }
   }

   void click(int action) {
      this.collect();
      if (action == 600) {
         this.wm.openModulesRow(false);
      } else if (action == 601 || action == 602) {
         boolean target = action == 601;
         this.undo = new boolean[this.items.size()];

         for (int i2 = 0; i2 < this.items.size(); i2++) {
            IslandItem item = this.items.get(i2);
            this.undo[i2] = this.wm.isShown(item);
            if (item.shown != null) {
               item.shown.set(target);
            }
         }

         this.undoUntil = ClientClock.millis() + 5000L;
         this.detailsAt = 0L;
         this.wm.islandToggle(target);
      } else if (action == 603 && this.undo != null) {
         for (int i3 = 0; i3 < this.items.size() && i3 < this.undo.length; i3++) {
            IslandItem item = this.items.get(i3);
            if (item.shown != null) {
               item.shown.set(this.undo[i3]);
            }
         }

         this.undo = null;
         this.undoUntil = 0L;
         this.detailsAt = 0L;
         this.wm.islandClick();
      } else if (action == 604) {
         this.wm.islandClick();
         this.wm.openMenuAtModules(null);
      } else if (action >= 610 && action < 640) {
         int i4 = action - 610;
         if (i4 < this.items.size()) {
            this.wm.islandClick();
            this.wm.openMenuAtModules(this.items.get(i4));
         }
      } else {
         int i;
         if (action >= 640 && action < 700 && (i = action - 640) < this.items.size() && this.items.get(i).shown != null) {
            IslandItem item = this.items.get(i);
            item.shown.set(!item.shown.get());
            this.detailsAt = 0L;
            this.wm.islandToggle(item.shown.get());
         }
      }
   }

   float[] layoutForTest() {
      return new float[]{this.columns, this.perColumn, this.width, this.height, this.scroll, this.scrollMax};
   }

   float[] rowForTest(String id, IslandStyle s) {
      float top = this.py + this.headerH + s.gap - this.scroll;

      for (int i = 0; i < this.items.size(); i++) {
         if (this.items.get(i).id.equals(id)) {
            int col = i / this.perColumn;
            int row = i % this.perColumn;
            float rx0 = this.px + s.pad + col * (this.colW + this.colGap);
            float ry = top + row * (this.rowH + this.rowGap);
            return new float[]{rx0 + this.colW - switchW(s) / 2.0F, ry + this.rowH / 2.0F};
         }
      }

      return null;
   }

   List<String> detailsForTest() {
      this.collect();
      this.refreshDetails();
      ArrayList<String> out = new ArrayList<>();

      for (int i = 0; i < this.items.size(); i++) {
         out.add(this.items.get(i).id + " " + this.details.get(i) + (this.setup.get(i) ? " [setup]" : ""));
      }

      return out;
   }

   float[] buttonsForTest(IslandStyle s) {
      float cy = this.py + this.headerH / 2.0F;
      float fy = this.py + this.ph - this.footerH / 2.0F;
      float nan = Float.NaN;
      return new float[]{
         this.px + s.pad * 0.6F + this.backW / 2.0F,
         cy,
         this.allOnX < 0.0F ? nan : this.allOnX + this.allOnW / 2.0F,
         cy,
         this.allOffX < 0.0F ? nan : this.allOffX + this.allOffW / 2.0F,
         cy,
         this.undoX < 0.0F ? nan : this.undoX + this.undoW / 2.0F,
         cy,
         this.linkX + this.linkW / 2.0F,
         fy
      };
   }
}
