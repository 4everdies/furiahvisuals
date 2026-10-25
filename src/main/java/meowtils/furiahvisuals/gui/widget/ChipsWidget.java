package meowtils.furiahvisuals.gui.widget;

import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ModeSetting;

public final class ChipsWidget extends Widget {
   private static final float CHIP_H = 20.0F;
   private static final float GAP = 5.0F;
   private static final float TEXT = 8.8F;
   private final ModeSetting<?> setting;
   private final String[] labels;
   private final float[] chipX;
   private final float[] chipY;
   private final float[] chipW;
   private final float[] chipHover;
   private float chipsH = 20.0F;
   private final ChoiceControl choice;

   public ChipsWidget(ModeSetting<?> setting) {
      this.setting = setting;
      Enum[] values = setting.getValues();
      this.labels = new String[values.length];

      for (int i = 0; i < values.length; i++) {
         this.labels[i] = ModeSetting.displayName(values[i]);
      }

      this.chipX = new float[this.labels.length];
      this.chipY = new float[this.labels.length];
      this.chipW = new float[this.labels.length];
      this.chipHover = new float[this.labels.length];
      this.choice = new ChoiceControl(setting, this.labels);
   }

   @Override
   public float height() {
      return Density.compact() ? Density.row() : 22.0F + this.chipsH + 6.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private void layout() {
      float cx = this.x;
      float cy = this.y + 22.0F;

      for (int i = 0; i < this.labels.length; i++) {
         float cw = Ui.width(this.labels[i], 8.8F) + 18.0F;
         if (cx > this.x && cx + cw > this.x + this.w) {
            cx = this.x;
            cy += 25.0F;
         }

         this.chipX[i] = cx;
         this.chipY[i] = cy;
         this.chipW[i] = cw;
         cx += cw + 5.0F;
      }

      this.chipsH = cy - (this.y + 22.0F) + 20.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      if (Density.compact()) {
         this.animateHover(this.contains(mx, my), dt);
         this.choice.render(this, this.setting.getLabel(), null, this.hover, mx, my, dt, accent);
      } else {
         this.layout();
         Ui.text(this.setting.getLabel(), this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
         int selected = this.setting.getIndex();

         for (int i = 0; i < this.labels.length; i++) {
            boolean over = mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 20.0F;
            this.chipHover[i] = Easing.approach(this.chipHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
            boolean sel = i == selected;
            if (sel) {
               Ui.roundRect(this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, Theme.ACCENT_SOFT);
               Ui.accentOutline(this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, 1.0F, accent);
            } else {
               Ui.surface(this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.chipHover[i]));
            }

            int text = sel ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.chipHover[i]);
            Ui.textMiddle(this.labels[i], this.chipX[i] + 9.0F, this.chipY[i] + 10.0F, 8.8F, text, sel);
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (Density.compact()) {
         return this.choice.mouseDown(this, mx, my, button);
      } else if (this.contains(mx, my) && button == 0) {
         for (int i = 0; i < this.labels.length; i++) {
            if (mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 20.0F) {
               this.setting.setIndex(i);
               return true;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (Density.compact()) {
         return this.choice.tooltip(this, mx, my);
      } else if (!this.contains(mx, my)) {
         return null;
      } else {
         Enum[] values = this.setting.getValues();

         for (int i = 0; i < this.labels.length; i++) {
            if (mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 20.0F) {
               String d = ModeSetting.description(values[i]);
               return d.isEmpty() ? this.setting.getDescription() : d;
            }
         }

         return this.setting.getDescription();
      }
   }
}
