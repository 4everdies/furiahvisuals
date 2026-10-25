package meowtils.furiahvisuals.gui.popup;

import java.util.Arrays;
import java.util.Locale;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

public final class ChoicePopover extends Popover {
   private static final float PAD = 5.0F;
   private static final int MAX_ROWS = 10;
   private static final int SEARCH_FROM = 10;
   private final ModeSetting<?> setting;
   private final String[] labels;
   private final String[] descriptions;
   private final float[] hover;
   private final float minW;
   private String query = "";
   private int[] shown;
   private int highlight = -1;
   private float scroll;
   private float scrollShown;
   private float caret;

   public ChoicePopover(ModeSetting<?> setting, float minWidth) {
      this.setting = setting;
      Enum[] values = setting.getValues();
      this.labels = new String[values.length];
      this.descriptions = new String[values.length];

      for (int i = 0; i < values.length; i++) {
         this.labels[i] = ModeSetting.displayName(values[i]);
         this.descriptions[i] = ModeSetting.description(values[i]);
      }

      this.hover = new float[values.length];
      this.minW = minWidth;
      this.filter();

      for (int s = 0; s < this.shown.length; s++) {
         if (this.shown[s] == setting.getIndex()) {
            this.highlight = s;
            this.scrollShown = this.scroll = Math.max(0.0F, (s - 5) * row());
         }
      }
   }

   private boolean searchable() {
      return this.labels.length > 10;
   }

   private static float row() {
      return Math.max(16.0F, Density.row());
   }

   private float searchH() {
      return this.searchable() ? row() + 4.0F : 0.0F;
   }

   private void filter() {
      String q = this.query.trim().toLowerCase(Locale.ROOT);
      int[] idx = new int[this.labels.length];
      int n = 0;

      for (int i = 0; i < this.labels.length; i++) {
         if (q.isEmpty() || this.labels[i].toLowerCase(Locale.ROOT).contains(q)) {
            idx[n++] = i;
         }
      }

      this.shown = Arrays.copyOf(idx, n);
      this.highlight = n == 0 ? -1 : Math.min(Math.max(0, this.highlight), n - 1);
   }

   @Override
   protected float preferredW() {
      float max = 0.0F;

      for (String l : this.labels) {
         max = Math.max(max, Ui.widthBold(l, Density.text()));
      }

      return Math.max(this.minW, Math.min(260.0F, max + 34.0F));
   }

   @Override
   protected float preferredH() {
      int rows = Math.max(1, Math.min(10, this.shown.length));
      return 10.0F + this.searchH() + rows * row();
   }

   private float listTop() {
      return this.y + 5.0F + this.searchH();
   }

   private float listH() {
      return Math.max(1, Math.min(10, this.shown.length)) * row();
   }

   private float maxScroll() {
      return Math.max(0.0F, this.shown.length * row() - this.listH());
   }

   @Override
   protected void renderContent(float mx, float my, float dt, int accent) {
      this.caret += dt;
      float ts = Density.text();
      if (this.searchable()) {
         float sy = this.y + 5.0F;
         float sh = row();
         Ui.surface(this.x + 5.0F, sy, this.w - 10.0F, sh, 6.0F, Theme.SURFACE);
         String shownQuery = this.query.isEmpty() ? "Search…" : this.query;
         Ui.textMiddle(
            Ui.ellipsize(shownQuery, ts, this.w - 10.0F - 16.0F, false),
            this.x + 5.0F + 7.0F,
            sy + sh / 2.0F,
            ts,
            this.query.isEmpty() ? Theme.TEXT_MUTED : Theme.TEXT,
            false
         );
         if (this.caret % 1.0F < 0.55F) {
            float cx = this.x + 5.0F + 7.0F + (this.query.isEmpty() ? 0.0F : Ui.width(this.query, ts)) + 0.5F;
            Ui.rect(cx, sy + 4.0F, 0.8F, sh - 8.0F, Theme.TEXT);
         }
      }

      this.scroll = Math.max(0.0F, Math.min(this.maxScroll(), this.scroll));
      this.scrollShown = Easing.approach(this.scrollShown, this.scroll, 18.0F, dt);
      float top = this.listTop();
      float lh = this.listH();
      Ui.pushScissor(this.x, top, this.w, lh);
      int selected = this.setting.getIndex();
      float r = row();

      for (int s = 0; s < this.shown.length; s++) {
         int i = this.shown[s];
         float ry = top + s * r - this.scrollShown;
         if (!(ry + r < top) && !(ry > top + lh)) {
            boolean over = mx >= this.x && mx < this.x + this.w && my >= Math.max(ry, top) && my < Math.min(ry + r, top + lh);
            this.hover[i] = Easing.approach(this.hover[i], over ? 1.0F : 0.0F, 18.0F, dt);
            if (over) {
               this.highlight = s;
            }

            boolean sel = i == selected;
            if (sel) {
               Ui.roundRect(this.x + 5.0F, ry + 1.0F, this.w - 10.0F, r - 2.0F, 5.0F, Theme.ACCENT_SOFT);
            } else if (this.hover[i] > 0.01F || s == this.highlight) {
               Ui.roundRect(this.x + 5.0F, ry + 1.0F, this.w - 10.0F, r - 2.0F, 5.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, Math.max(this.hover[i], 0.7F)));
            }

            if (sel) {
               Ui.circle(this.x + 5.0F + 6.0F, ry + r / 2.0F, 2.0F, accent);
            }

            Ui.textMiddle(
               Ui.ellipsize(this.labels[i], ts, this.w - 10.0F - 18.0F, sel),
               this.x + 5.0F + 12.0F,
               ry + r / 2.0F,
               ts,
               sel ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hover[i]),
               sel
            );
         }
      }

      if (this.shown.length == 0) {
         Ui.textMiddle("Nothing matches", this.x + 5.0F + 12.0F, top + r / 2.0F, ts, Theme.TEXT_MUTED, false);
      }

      Ui.popScissor();
   }

   private int rowAt(float mx, float my) {
      float top = this.listTop();
      if (!(mx < this.x) && !(mx >= this.x + this.w) && !(my < top) && !(my >= top + this.listH())) {
         int s = (int)((my - top + this.scrollShown) / row());
         return s >= 0 && s < this.shown.length ? s : -1;
      } else {
         return -1;
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      int s = this.rowAt(mx, my);
      if (s >= 0 && button == 0) {
         this.pick(this.shown[s]);
      }

      return true;
   }

   private void pick(int i) {
      if (i != this.setting.getIndex()) {
         this.setting.setIndex(i);
      }

      ModSounds.gui(Sfx.CLICK);
      Popovers.close(this);
   }

   @Override
   public boolean wheel(float mx, float my, int wheel) {
      this.scroll = this.scroll - Math.signum((float)wheel) * row() * 2.0F;
      return true;
   }

   @Override
   public boolean wantsKeys() {
      return true;
   }

   @Override
   public boolean keyTyped(char c, int key) {
      if (key != 208 && key != 200) {
         if (key != 28 && key != 156) {
            if (!this.searchable()) {
               return true;
            } else if (key == 14) {
               if (!this.query.isEmpty()) {
                  this.query = this.query.substring(0, this.query.length() - 1);
                  this.filter();
                  ModSounds.key();
               }

               return true;
            } else {
               if (c >= ' ' && c != 127 && this.query.length() < 32) {
                  this.query = this.query + c;
                  this.filter();
                  this.scroll = 0.0F;
                  this.caret = 0.0F;
                  ModSounds.key();
               }

               return true;
            }
         } else {
            if (this.highlight >= 0 && this.highlight < this.shown.length) {
               this.pick(this.shown[this.highlight]);
            }

            return true;
         }
      } else {
         if (this.shown.length > 0) {
            this.highlight = Math.max(0, Math.min(this.shown.length - 1, this.highlight + (key == 208 ? 1 : -1)));
            float r = row();
            float top = this.highlight * r;
            if (top < this.scroll) {
               this.scroll = top;
            } else if (top + r > this.scroll + this.listH()) {
               this.scroll = top + r - this.listH();
            }
         }

         return true;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      int s = this.rowAt(mx, my);
      if (s < 0) {
         return null;
      } else {
         String d = this.descriptions[this.shown[s]];
         return d != null && !d.isEmpty() ? d : null;
      }
   }
}
