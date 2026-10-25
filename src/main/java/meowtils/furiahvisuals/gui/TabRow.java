package meowtils.furiahvisuals.gui;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import meowtils.furiahvisuals.gui.layout.LayoutPage;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.BooleanSetting;

final class TabRow {
   static final int FULL = 0;
   static final int SHORT = 1;
   static final int ICONS = 2;
   static final int MORE = 3;
   private static final float OFFSCREEN = -10000.0F;
   float x;
   float y;
   float w;
   float h;
   int mode;
   int forcedMode = -1;
   final List<TabRow.Box> boxes = new ArrayList<>();
   final List<LayoutPage> overflow = new ArrayList<>();
   float moreX = -10000.0F;
   float moreW;
   private float litX = Float.NaN;
   private float litW;
   private LayoutPage litFor;
   private float moreHover;
   private final Map<LayoutPage, float[]> anim = new IdentityHashMap<>();
   private static final float GAP = 4.0F;
   private static final float DIVIDER = 9.0F;
   private static final float ICON_GAP = 4.0F;
   private static final float DOT_GAP = 5.0F;

   float text() {
      return Density.dense() ? 7.4F : (Density.compact() ? 8.0F : 8.6F);
   }

   private float icon() {
      return this.h * 0.48F;
   }

   private float dotR() {
      return Density.dense() ? 2.2F : 2.6F;
   }

   private float pad() {
      return Density.dense() ? 5.5F : (Density.compact() ? 6.5F : 8.0F);
   }

   static float height() {
      return Density.dense() ? 17.0F : (Density.compact() ? 20.0F : 24.0F);
   }

   private float tabW(LayoutPage p, float labelW, float countW) {
      boolean dot = p.tab.getSwitch() != null;
      float pd = labelW < 0.0F ? this.pad() * 0.75F : this.pad();
      float wd = pd + this.icon() + (labelW < 0.0F ? 0.0F : 4.0F + labelW) + countW + (dot ? 5.0F + this.dotR() * 2.0F : 0.0F) + pd;
      return Math.max(wd, this.h);
   }

   private static boolean utility(LayoutPage p, int index) {
      return p.tab.dividerBefore() && index > 0;
   }

   private float countW(int count) {
      return count < 0 ? 0.0F : 5.0F + Math.max(this.h * 0.62F, Ui.width(String.valueOf(count), 7.0F) + 7.0F);
   }

   void layout(List<LayoutPage> tabs, LayoutPage open, int[] counts, float x, float y, float w, float h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.boxes.clear();
      this.overflow.clear();
      this.moreX = -10000.0F;
      int n = tabs.size();
      if (n != 0) {
         float ts = this.text();
         float[] label = new float[n];
         float[] cw = new float[n];

         for (int i = 0; i < n; i++) {
            label[i] = Ui.width(tabs.get(i).tab.getName(), ts);
            cw[i] = this.countW(counts == null ? -1 : counts[i]);
         }

         float sumIcons = this.sum(tabs, label, cw, -1.0F, true);
         int want = this.forcedMode;
         float room = w;
         float minLabel = Ui.width("Abcd…", ts);
         if (want == 3) {
            room = Math.min(w, sumIcons * 0.55F);
         } else if (want == 1) {
            room = Math.min(w, (this.sum(tabs, label, cw, Float.MAX_VALUE, true) + this.sum(tabs, label, cw, minLabel, true)) / 2.0F);
         }

         float maxLabel = Float.MAX_VALUE;
         boolean utilityIcon = false;
         if (want != 0 && (want >= 0 || !(this.sum(tabs, label, cw, Float.MAX_VALUE, false) <= room))) {
            if (want < 0 && this.sum(tabs, label, cw, Float.MAX_VALUE, true) <= room) {
               this.mode = 0;
               utilityIcon = true;
            } else if (want != 1 && (want >= 0 || !(this.sum(tabs, label, cw, minLabel, true) <= room))) {
               this.mode = want != 2 && (want >= 0 || !(sumIcons <= room)) ? 3 : 2;
            } else {
               this.mode = 1;
               utilityIcon = true;
               maxLabel = this.shortenedLabel(tabs, label, cw, room, minLabel);
            }
         } else {
            this.mode = 0;
         }

         float moreRoom = this.mode == 3 ? h + 6.0F + 4.0F : 0.0F;
         float cx = x;
         boolean openPlaced = false;

         for (int i = 0; i < n; i++) {
            LayoutPage p = tabs.get(i);
            float lw = utilityIcon && utility(p, i) ? -1.0F : (this.mode == 0 ? label[i] : (this.mode == 1 ? Math.min(label[i], maxLabel) : -1.0F));
            float tw = this.tabW(p, lw, cw[i]);
            float div = p.tab.dividerBefore() && i > 0 ? 9.0F : 0.0F;
            boolean last = i == n - 1;
            if (this.mode == 3) {
               float reserve = 0.0F;
               if (!openPlaced && open != null && p != open && tabs.indexOf(open) > i) {
                  reserve = this.tabW(open, -1.0F, cw[tabs.indexOf(open)]) + 4.0F;
               }

               boolean fits = cx + div + tw + (last ? 0.0F : moreRoom) + reserve <= x + room + 0.01F;
               if (!fits && p != open) {
                  this.overflow.add(p);
                  continue;
               }
            }

            TabRow.Box b = new TabRow.Box(p);
            float var32;
            b.x = var32 = cx + div;
            b.w = tw;
            b.label = lw < 0.0F ? null : (lw < label[i] - 0.5F ? Ui.ellipsize(p.tab.getName(), ts, lw, false) : p.tab.getName());
            if (p.tab.getSwitch() != null) {
               b.dotX = var32 + tw - (lw < 0.0F ? this.pad() * 0.75F : this.pad()) - this.dotR();
            }

            this.boxes.add(b);
            if (p == open) {
               openPlaced = true;
            }

            cx = var32 + (tw + 4.0F);
         }

         if (!this.overflow.isEmpty()) {
            this.moreW = h + 6.0F;
            this.moreX = cx;
         }
      }
   }

   private float sum(List<LayoutPage> tabs, float[] label, float[] cw, float maxLabel, boolean utilityIcon) {
      float sum = 4.0F * (tabs.size() - 1);

      for (int i = 0; i < tabs.size(); i++) {
         LayoutPage p = tabs.get(i);
         if (utility(p, i)) {
            sum += 9.0F;
         }

         float lw = !(maxLabel < 0.0F) && (!utilityIcon || !utility(p, i)) ? Math.min(label[i], maxLabel) : -1.0F;
         sum += this.tabW(p, lw, cw[i]);
      }

      return sum;
   }

   private float shortenedLabel(List<LayoutPage> tabs, float[] label, float[] cw, float room, float minLabel) {
      float lo = minLabel;
      float hi = 0.0F;

      for (float l : label) {
         hi = Math.max(hi, l);
      }

      for (int it = 0; it < 18; it++) {
         float mid = (lo + hi) / 2.0F;
         if (this.sum(tabs, label, cw, mid, true) <= room) {
            lo = mid;
         } else {
            hi = mid;
         }
      }

      return lo;
   }

   private float[] anim(LayoutPage p) {
      float[] a = this.anim.get(p);
      if (a == null) {
         a = new float[]{0.0F, -1.0F, 0.0F};
         this.anim.put(p, a);
      }

      return a;
   }

   void draw(LayoutPage open, int[] counts, List<LayoutPage> tabs, boolean sectionOn, float mx, float my, boolean free, float dt, int accent) {
      float ts = this.text();
      float ic = this.icon();
      float r = this.dotR();
      TabRow.Box openBox = null;

      for (TabRow.Box b : this.boxes) {
         if (b.page == open) {
            openBox = b;
         }
      }

      if (openBox != null) {
         if (!Float.isNaN(this.litX) && this.litFor != null && tabs.contains(this.litFor)) {
            float k = 1.0F - (float)Math.exp(-dt * 18.0F);
            this.litX = this.litX + (openBox.x - this.litX) * k;
            this.litW = this.litW + (openBox.w - this.litW) * k;
         } else {
            this.litX = openBox.x;
            this.litW = openBox.w;
         }

         this.litFor = open;
         Ui.roundRect(this.litX, this.y, this.litW, this.h, this.h / 2.0F, Theme.ACCENT_SOFT);
         Ui.accentOutline(this.litX, this.y, this.litW, this.h, this.h / 2.0F, 1.0F, accent);
      }

      for (int k = 0; k < this.boxes.size(); k++) {
         TabRow.Box bx = this.boxes.get(k);
         LayoutPage p = bx.page;
         MiniSection t = p.tab;
         int i = tabs.indexOf(p);
         int count = counts != null && i >= 0 ? counts[i] : -1;
         float[] a = this.anim(p);
         boolean over = free && mx >= bx.x && mx < bx.x + bx.w && my >= this.y && my < this.y + this.h;
         boolean overDot = over && this.onDot(bx, mx, my);
         a[0] = Easing.approach(a[0], over && !overDot ? 1.0F : 0.0F, 16.0F, dt);
         BooleanSetting sw = t.getSwitch();
         boolean on = sw != null && sw.get();
         a[1] = a[1] < 0.0F ? (on ? 1.0F : 0.0F) : Easing.approach(a[1], on ? 1.0F : 0.0F, 14.0F, dt);
         a[2] = Easing.approach(a[2], overDot ? 1.0F : 0.0F, 18.0F, dt);
         boolean isOpen = p == open;
         if (t.dividerBefore() && i > 0) {
            Ui.rect(bx.x - 4.5F - 0.4F, this.y + this.h * 0.2F, 0.8F, this.h * 0.6F, Theme.DIVIDER);
         }

         Ui.registerControl(bx.x, this.y, bx.w, this.h, "tab " + t.getName());
         Ui.pushAlpha(count == 0 ? 0.42F : 1.0F);
         if (!isOpen) {
            Ui.surface(bx.x, this.y, bx.w, this.h, this.h / 2.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, a[0]));
         }

         int tc = isOpen ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, a[0]);
         float pd = bx.label == null ? this.pad() * 0.75F : this.pad();
         float ix = bx.label == null && sw == null ? bx.x + (bx.w - ic) / 2.0F : bx.x + pd;
         IslandIcons.draw(t.getIcon(), ix, this.y + (this.h - ic) / 2.0F, ic, isOpen ? accent : tc);
         float tx = ix + ic + 4.0F;
         if (bx.label != null) {
            Ui.textMiddle(bx.label, tx, this.y + this.h / 2.0F, ts, tc, isOpen);
            tx += Ui.width(bx.label, ts);
         } else {
            tx -= 4.0F;
         }

         if (count >= 0) {
            String s = String.valueOf(count);
            float pw = Math.max(this.h * 0.62F, Ui.width(s, 7.0F) + 7.0F);
            float ph = this.h * 0.62F;
            float pxl = tx + 5.0F;
            Ui.roundRect(pxl, this.y + (this.h - ph) / 2.0F, pw, ph, ph / 2.0F, count > 0 ? Theme.ACCENT_SOFT : Theme.SURFACE_HOVER);
            Ui.textMiddle(s, pxl + (pw - Ui.width(s, 7.0F)) / 2.0F, this.y + this.h / 2.0F, 7.0F, count > 0 ? Theme.TEXT : Theme.TEXT_MUTED, false);
         }

         if (!Float.isNaN(bx.dotX)) {
            dot(bx.dotX, this.y + this.h / 2.0F, r, a[1], a[2], accent, sectionOn);
         }

         Ui.popAlpha();
      }

      if (this.moreX > -5000.0F) {
         boolean overx = free && mx >= this.moreX && mx < this.moreX + this.moreW && my >= this.y && my < this.y + this.h;
         this.moreHover = Easing.approach(this.moreHover, overx ? 1.0F : 0.0F, 16.0F, dt);
         boolean anyOpen = this.overflow.contains(open);
         Ui.registerControl(this.moreX, this.y, this.moreW, this.h, "tabs ⋯");
         if (anyOpen) {
            Ui.roundRect(this.moreX, this.y, this.moreW, this.h, this.h / 2.0F, Theme.ACCENT_SOFT);
            Ui.accentOutline(this.moreX, this.y, this.moreW, this.h, this.h / 2.0F, 1.0F, accent);
         } else {
            Ui.surface(this.moreX, this.y, this.moreW, this.h, this.h / 2.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.moreHover));
         }

         int c = ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.moreHover);

         for (int ixx = -1; ixx <= 1; ixx++) {
            Ui.circle(this.moreX + this.moreW / 2.0F + ixx * 3.6F, this.y + this.h / 2.0F, 1.2F, c);
         }
      }
   }

   static void dot(float cx, float cy, float r, float on, float hover, int accent, boolean sectionOn) {
      Ui.pushAlpha(sectionOn ? 1.0F : 0.5F);
      if (hover > 0.01F) {
         int ring = ColorUtil.lerp(Theme.TEXT_DIM, accent, on);
         Ui.circle(cx, cy, r + 3.0F * hover, ColorUtil.withAlpha(ring, 0.16F * hover));
         Ui.outline(cx - r - 3.0F, cy - r - 3.0F, (r + 3.0F) * 2.0F, (r + 3.0F) * 2.0F, r + 3.0F, 0.8F, ColorUtil.withAlpha(ring, 0.7F * hover));
      }

      if (on > 0.01F) {
         Ui.glow(cx, cy, r * 5.0F, ColorUtil.withAlpha(accent, 0.4F * on));
      }

      Ui.circle(cx, cy, r, ColorUtil.lerp(Theme.TEXT_DIM, accent, on));
      Ui.popAlpha();
   }

   boolean onDot(TabRow.Box b, float mx, float my) {
      if (Float.isNaN(b.dotX)) {
         return false;
      } else {
         float half = Math.max(6.0F, this.dotR() + 3.5F);
         return Math.abs(mx - b.dotX) <= half && Math.abs(my - (this.y + this.h / 2.0F)) <= Math.max(half, this.h / 2.0F);
      }
   }

   TabRow.Box at(float mx, float my) {
      if (!(my < this.y) && !(my >= this.y + this.h)) {
         for (TabRow.Box b : this.boxes) {
            if (mx >= b.x && mx < b.x + b.w) {
               return b;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   boolean onMore(float mx, float my) {
      return this.moreX > -5000.0F && mx >= this.moreX && mx < this.moreX + this.moreW && my >= this.y && my < this.y + this.h;
   }

   boolean contains(float mx, float my) {
      return my >= this.y && my < this.y + this.h && mx >= this.x && mx < this.x + this.w;
   }

   String tooltip(float mx, float my, int[] counts, List<LayoutPage> tabs) {
      if (this.onMore(mx, my)) {
         return this.overflow.size() + " more " + (this.overflow.size() == 1 ? "tab" : "tabs");
      } else {
         TabRow.Box b = this.at(mx, my);
         if (b == null) {
            return null;
         } else {
            MiniSection t = b.page.tab;
            BooleanSetting sw = t.getSwitch();
            if (this.onDot(b, mx, my)) {
               return "Turn " + t.getName() + (sw.get() ? " off" : " on");
            } else {
               int i = tabs.indexOf(b.page);
               String count = counts == null || i < 0
                  ? ""
                  : (counts[i] == 0 ? " (no matches)" : (counts[i] == 1 ? " (1 match)" : " (" + counts[i] + " matches)"));
               String state = sw == null ? "" : (sw.get() ? ": on" : ": off");
               boolean shortened = b.label == null || !b.label.equals(t.getName());
               return !shortened && count.isEmpty() ? null : t.getName() + state + count;
            }
         }
      }
   }

   Object hoverTarget(float mx, float my) {
      if (this.onMore(mx, my)) {
         return "tabs ⋯";
      } else {
         TabRow.Box b = this.at(mx, my);
         if (b == null) {
            return null;
         } else {
            return this.onDot(b, mx, my) ? this.anim(b.page) : b.page;
         }
      }
   }

   void reset() {
      this.litX = Float.NaN;
      this.litFor = null;
      this.anim.clear();
   }

   static final class Box {
      final LayoutPage page;
      float x;
      float w;
      String label;
      float dotX = Float.NaN;

      Box(LayoutPage page) {
         this.page = page;
      }
   }
}
