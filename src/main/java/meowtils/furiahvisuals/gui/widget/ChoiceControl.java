package meowtils.furiahvisuals.gui.widget;

import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.popup.ChoicePopover;
import meowtils.furiahvisuals.gui.popup.Popovers;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class ChoiceControl {
   private static final int CYCLE_MAX = 6;
   private final ModeSetting<?> setting;
   private final String[] labels;
   private float leftHover;
   private float rightHover;
   private float valueHover;
   private float cx;
   private float cw;
   private float ch;
   private float cy;

   ChoiceControl(ModeSetting<?> setting, String[] labels) {
      this.setting = setting;
      this.labels = labels;
   }

   private boolean cycler() {
      return this.labels.length <= 6;
   }

   private void layout(Widget row) {
      float small = Density.small();
      float max = 0.0F;

      for (String l : this.labels) {
         max = Math.max(max, Ui.widthBold(l, small));
      }

      this.cw = Math.max(this.cycler() ? 64.0F : 72.0F, Math.min(row.w * 0.55F, max + (this.cycler() ? 32.0F : 24.0F)));
      this.cx = row.x + row.w - this.cw;
      this.ch = Density.control();
      this.cy = row.y + Density.row() / 2.0F;
   }

   void render(Widget row, String label, PreviewButton preview, float hover, float mx, float my, float dt, int accent) {
      this.layout(row);
      float h = Density.row();
      float ts = Density.text();
      float small = Density.small();
      if (hover > 0.01F) {
         Ui.roundRect(row.x - 4.0F, row.y, row.w + 8.0F, h, 5.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, hover * 0.8F));
      }

      float room = this.cx - row.x - 8.0F - (preview != null ? 16.0F : 0.0F);
      String shownLabel = Ui.ellipsize(label, ts, room, false);
      Ui.textMiddle(shownLabel, row.x, this.cy, ts, Theme.TEXT, false);
      if (preview != null) {
         preview.render(row.x + Ui.width(shownLabel, ts) + 11.0F, this.cy, mx, my, dt, accent);
      }

      boolean overBox = mx >= this.cx && mx < this.cx + this.cw && my >= this.cy - this.ch / 2.0F - 1.0F && my < this.cy + this.ch / 2.0F + 1.0F;
      boolean overLeft = overBox && this.cycler() && mx < this.cx + 14.0F;
      boolean overRight = overBox && this.cycler() && mx >= this.cx + this.cw - 14.0F;
      this.leftHover = Easing.approach(this.leftHover, overLeft ? 1.0F : 0.0F, 18.0F, dt);
      this.rightHover = Easing.approach(this.rightHover, overRight ? 1.0F : 0.0F, 18.0F, dt);
      this.valueHover = Easing.approach(this.valueHover, overBox && !overLeft && !overRight ? 1.0F : 0.0F, 18.0F, dt);
      Ui.surface(
         this.cx,
         this.cy - this.ch / 2.0F,
         this.cw,
         this.ch,
         Math.min(6.0F, this.ch / 2.0F),
         ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, Math.max(this.valueHover, 0.4F * hover))
      );
      String value = this.labels[this.setting.getIndex()];
      if (this.cycler()) {
         arrow(this.cx + 7.0F, this.cy, -1, ColorUtil.lerp(Theme.TEXT_MUTED, accent, this.leftHover));
         arrow(this.cx + this.cw - 7.0F, this.cy, 1, ColorUtil.lerp(Theme.TEXT_MUTED, accent, this.rightHover));
         String v = Ui.ellipsize(value, small, this.cw - 28.0F, true);
         Ui.textMiddle(v, this.cx + (this.cw - Ui.widthBold(v, small)) / 2.0F, this.cy, small, ColorUtil.lerp(Theme.TEXT, accent, this.valueHover * 0.5F), true);
      } else {
         String v = Ui.ellipsize(value, small, this.cw - 22.0F, true);
         Ui.textMiddle(v, this.cx + 7.0F, this.cy, small, ColorUtil.lerp(Theme.TEXT, accent, this.valueHover * 0.5F), true);
         down(this.cx + this.cw - 8.0F, this.cy, ColorUtil.lerp(Theme.TEXT_MUTED, accent, this.valueHover));
      }
   }

   private static void arrow(float ax, float ay, int dir, int color) {
      for (int k = 0; k <= 4; k++) {
         float f = k / 4.0F;
         Ui.circle(ax + dir * (-1.4F + f * 2.8F), ay - 2.8F + f * 2.8F, 0.6F, color);
         Ui.circle(ax + dir * (-1.4F + f * 2.8F), ay + 2.8F - f * 2.8F, 0.6F, color);
      }
   }

   private static void down(float ax, float ay, int color) {
      for (int k = 0; k <= 4; k++) {
         float f = k / 4.0F;
         Ui.circle(ax - 2.8F + f * 2.8F, ay - 1.4F + f * 2.8F, 0.6F, color);
         Ui.circle(ax + 2.8F - f * 2.8F, ay - 1.4F + f * 2.8F, 0.6F, color);
      }
   }

   boolean mouseDown(Widget row, float mx, float my, int button) {
      this.layout(row);
      float h = Density.row();
      if (!(mx >= row.x - 4.0F) || !(mx < row.x + row.w + 4.0F) || !(my >= row.y) || !(my < row.y + h)) {
         return false;
      } else if (button != 0) {
         return false;
      } else {
         boolean inBox = mx >= this.cx && mx < this.cx + this.cw;
         int n = this.labels.length;
         if (inBox && this.cycler() && mx < this.cx + 14.0F) {
            this.setting.setIndex((this.setting.getIndex() - 1 + n) % n);
            ModSounds.gui(Sfx.CLICK);
            return true;
         } else if (inBox && this.cycler() && mx >= this.cx + this.cw - 14.0F) {
            this.setting.setIndex((this.setting.getIndex() + 1) % n);
            ModSounds.gui(Sfx.CLICK);
            return true;
         } else {
            ModSounds.gui(Sfx.DROPDOWN);
            Popovers.open(new ChoicePopover(this.setting, this.cw), this.cx, row.y, this.cw, h);
            return true;
         }
      }
   }

   String tooltip(Widget row, float mx, float my) {
      this.layout(row);
      float h = Density.row();
      if (mx >= row.x && mx < row.x + row.w && my >= row.y && my < row.y + h) {
         int n = this.labels.length;
         boolean inBox = mx >= this.cx && mx < this.cx + this.cw;
         if (inBox && this.cycler() && mx < this.cx + 14.0F) {
            return "Previous: " + this.labels[(this.setting.getIndex() - 1 + n) % n];
         } else if (inBox && this.cycler() && mx >= this.cx + this.cw - 14.0F) {
            return "Next: " + this.labels[(this.setting.getIndex() + 1) % n];
         } else {
            String d = ModeSetting.description(this.setting.getValues()[this.setting.getIndex()]);
            return inBox ? (d.isEmpty() ? this.labels[this.setting.getIndex()] : d) + "  (click for every choice)" : this.setting.getDescription();
         }
      } else {
         return null;
      }
   }
}
