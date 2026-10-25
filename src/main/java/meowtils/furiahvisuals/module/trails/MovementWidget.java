package meowtils.furiahvisuals.module.trails;

import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.ToggleWidget;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.sound.ModSounds;

final class MovementWidget extends Widget {
   private final TrailsModule module;
   private final float[] knob;
   private final float[] glow;

   MovementWidget(TrailsModule module) {
      this.module = module;
      this.knob = new float[module.movement.length];
      this.glow = new float[module.movement.length];

      for (int i = 0; i < this.knob.length; i++) {
         this.knob[i] = -1.0F;
      }
   }

   private static float headH() {
      return Density.dense() ? 16.0F : (Density.compact() ? 19.0F : 24.0F);
   }

   private static float rowH() {
      return Density.dense() ? 14.0F : (Density.compact() ? 16.0F : 19.0F);
   }

   private static float switchW() {
      return Density.compact() ? 20.0F : 24.0F;
   }

   private static float switchH() {
      return Density.compact() ? 11.0F : 13.0F;
   }

   private static float gap() {
      return Density.compact() ? 14.0F : 20.0F;
   }

   private int rows() {
      return (this.module.movement.length + 1) / 2;
   }

   private float columnW() {
      return (this.w - gap()) / 2.0F;
   }

   private float columnX(int column) {
      return this.x + column * (this.columnW() + gap());
   }

   private float rowY(int row) {
      return this.y + headH() + row * rowH();
   }

   @Override
   public float height() {
      return headH() + this.rows() * rowH() + 3.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      Ui.textMiddle("Movement", this.x, this.y + headH() / 2.0F, 10.0F, Theme.TEXT, false);
      float sw = switchW();
      float sh = switchH();
      float ts = Density.dense() ? 7.6F : 8.4F;

      for (int i = 0; i < this.module.movement.length; i++) {
         BooleanSetting s = this.module.movement[i];
         int column = i % 2;
         int row = i / 2;
         float cx = this.columnX(column);
         float cy = this.rowY(row) + rowH() / 2.0F;
         float sx = cx + this.columnW() - sw;
         float sy = cy - sh / 2.0F;
         boolean on = s.get();
         Ui.textMiddle(Ui.ellipsize(s.getName(), ts, Math.max(10.0F, sx - cx - 16.0F), false), cx + 10.0F, cy, ts, on ? Theme.TEXT : Theme.TEXT_DIM, false);
         this.knob[i] = this.knob[i] < 0.0F ? (on ? 1.0F : 0.0F) : Easing.approach(this.knob[i], on ? 1.0F : 0.0F, 18.0F, dt);
         boolean over = mx >= sx - 4.0F && mx < sx + sw + 4.0F && my >= sy - 3.0F && my < sy + sh + 3.0F;
         this.glow[i] = Easing.approach(this.glow[i], over ? 1.0F : 0.0F, 16.0F, dt);
         if (this.glow[i] > 0.01F) {
            Ui.roundRect(sx - 3.0F, sy - 2.0F, sw + 6.0F, sh + 4.0F, (sh + 4.0F) / 2.0F, ColorUtil.withAlpha(Theme.TEXT, 0.06F * this.glow[i]));
         }

         ToggleWidget.drawSwitch(sx, sy, sw, sh, this.knob[i], accent);
      }
   }

   private int cellAt(float mx, float my) {
      for (int i = 0; i < this.module.movement.length; i++) {
         int column = i % 2;
         int row = i / 2;
         float cx = this.columnX(column);
         float ry = this.rowY(row);
         if (mx >= cx && mx < cx + this.columnW() && my >= ry && my < ry + rowH()) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         int i = this.cellAt(mx, my);
         if (i >= 0) {
            BooleanSetting s = this.module.movement[i];
            s.set(!s.get());
            ModSounds.toggle(s.get());
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      int i = this.cellAt(mx, my);
      if (i < 0) {
         return this.contains(mx, my) ? "The ways of moving your trail follows" : null;
      } else {
         BooleanSetting s = this.module.movement[i];
         return s.getDescription() + (s.get() ? " (on)" : " (off)");
      }
   }
}
