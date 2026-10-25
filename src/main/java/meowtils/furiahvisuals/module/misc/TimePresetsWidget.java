package meowtils.furiahvisuals.module.misc;

import java.util.Locale;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class TimePresetsWidget extends Widget {
   private static final String LABEL = "Presets";
   private final MiscModule misc;
   private final float[] hover = new float[MiscModule.TIME_PRESET_NAMES.length];
   private final float[] chipX = new float[this.hover.length];
   private final float[] chipY = new float[this.hover.length];
   private final float[] chipW = new float[this.hover.length];
   private float laidOutW = -1.0F;
   private float laidOutTs;
   private int lines = 1;

   TimePresetsWidget(MiscModule misc) {
      this.misc = misc;
   }

   private static float rowH() {
      return Density.compact() ? Density.row() + 2.0F : 26.0F;
   }

   private static float chipH() {
      return Density.compact() ? Density.control() + 1.0F : 20.0F;
   }

   private static float chipText() {
      return Density.dense() ? 7.6F : (Density.compact() ? 8.0F : 9.0F);
   }

   private static String label(int i) {
      return MiscModule.TIME_PRESET_NAMES[i] + " " + clock(MiscModule.TIME_PRESETS[i]);
   }

   static String clock(int minute) {
      return String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60);
   }

   private void layout() {
      float ts = chipText();
      if (!(Math.abs(this.laidOutW - this.w) < 0.01F) || !(this.laidOutW > 0.0F) || this.laidOutTs != ts) {
         this.laidOutW = this.w;
         this.laidOutTs = ts;
         float gap = 4.0F;
         float cx = this.x;
         float line = 0.0F;

         for (int i = 0; i < this.chipW.length; i++) {
            this.chipW[i] = Ui.width(label(i), ts) + 18.0F;
            if (cx > this.x && cx + this.chipW[i] > this.x + this.w) {
               cx = this.x;
               line++;
            }

            this.chipX[i] = cx - this.x;
            this.chipY[i] = line;
            cx += this.chipW[i] + gap;
         }

         this.lines = (int)line + 1;
      }
   }

   @Override
   public float height() {
      this.layout();
      return rowH() + this.lines * (chipH() + 4.0F) + 2.0F;
   }

   private int chipAt(float mx, float my) {
      this.layout();
      float ch = chipH();

      for (int i = 0; i < this.chipW.length; i++) {
         float cx = this.x + this.chipX[i];
         float cy = this.y + rowH() + this.chipY[i] * (ch + 4.0F);
         if (mx >= cx && mx < cx + this.chipW[i] && my >= cy && my < cy + ch) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.layout();
      float ts = chipText();
      float ch = chipH();
      int lit = this.misc.currentTimePreset();
      Ui.textMiddle("Presets", this.x, this.y + rowH() / 2.0F, Density.text(), Theme.TEXT, false);
      int over = this.chipAt(mx, my);

      for (int i = 0; i < this.chipW.length; i++) {
         this.hover[i] = Easing.approach(this.hover[i], over == i ? 1.0F : 0.0F, 16.0F, dt);
         float cx = this.x + this.chipX[i];
         float cy = this.y + rowH() + this.chipY[i] * (ch + 4.0F);
         boolean on = lit == i;
         Ui.registerControl(cx, cy, this.chipW[i], ch, "time preset " + MiscModule.TIME_PRESET_NAMES[i]);
         if (on) {
            Ui.roundRect(cx, cy, this.chipW[i], ch, ch / 2.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, this.hover[i]));
            Ui.accentOutline(cx, cy, this.chipW[i], ch, ch / 2.0F, 1.0F, accent);
         } else {
            Ui.surface(cx, cy, this.chipW[i], ch, ch / 2.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hover[i]));
         }

         String n = label(i);
         Ui.textMiddle(
            n,
            cx + (this.chipW[i] - Ui.width(n, ts)) / 2.0F,
            cy + ch / 2.0F,
            ts,
            on ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hover[i]),
            false
         );
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button != 0) {
         return false;
      } else {
         int i = this.chipAt(mx, my);
         if (i < 0) {
            return false;
         } else {
            this.misc.applyTimePreset(i);
            ModSounds.gui(Sfx.SELECT);
            return true;
         }
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      int i = this.chipAt(mx, my);
      if (i >= 0) {
         return MiscModule.TIME_PRESET_NAMES[i] + ": sets the Time to " + clock(MiscModule.TIME_PRESETS[i]) + " (turns the Time Changer on)";
      } else {
         return this.contains(mx, my) ? "One click sets the Time; drag the slider above for any other" : null;
      }
   }
}
