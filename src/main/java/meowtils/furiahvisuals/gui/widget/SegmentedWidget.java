package meowtils.furiahvisuals.gui.widget;

import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

public final class SegmentedWidget extends Widget {
   private static final float PREVIEW_W = 22.0F;
   private final ModeSetting<?> setting;
   private final String[] labels;
   private final Segmented control = new Segmented();
   private final ChoiceControl choice;
   private final PreviewButton preview = new PreviewButton();

   public SegmentedWidget(ModeSetting<?> setting) {
      this.setting = setting;
      Enum[] values = setting.getValues();
      this.labels = new String[values.length];

      for (int i = 0; i < values.length; i++) {
         this.labels[i] = ModeSetting.displayName(values[i]);
      }

      this.choice = new ChoiceControl(setting, this.labels);
   }

   private boolean inline() {
      float label = Ui.width(this.setting.getLabel(), 10.0F) + (this.setting.getPreview() != null ? 22.0F : 0.0F);
      return label + 14.0F + Segmented.preferredWidth(this.labels) <= this.w;
   }

   @Override
   public float height() {
      if (Density.compact()) {
         return Density.row();
      } else {
         return this.inline() ? 28.0F : 46.0F;
      }
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private float controlX() {
      return this.inline() ? this.x + this.w - this.controlW() : this.x;
   }

   private float controlY() {
      return this.inline() ? this.y + 4.0F : this.y + 22.0F;
   }

   private float controlW() {
      return this.inline() ? Math.min(this.w * 0.66F, Segmented.preferredWidth(this.labels)) : this.w;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      if (Density.compact()) {
         this.animateHover(this.contains(mx, my), dt);
         this.choice.render(this, this.setting.getLabel(), this.setting.getPreview() != null ? this.preview : null, this.hover, mx, my, dt, accent);
      } else {
         boolean inline = this.inline();
         if (inline) {
            Ui.textMiddle(this.setting.getLabel(), this.x, this.y + 14.0F, 10.0F, Theme.TEXT, false);
         } else {
            Ui.text(this.setting.getLabel(), this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
         }

         if (this.setting.getPreview() != null) {
            this.preview.render(this.x + Ui.width(this.setting.getLabel(), 10.0F) + 13.0F, inline ? this.y + 14.0F : this.y + 9.5F, mx, my, dt, accent);
         }

         this.control.render(this.controlX(), this.controlY(), this.controlW(), 20.0F, this.labels, this.setting.getIndex(), mx, my, dt, accent);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.setting.getPreview() != null && this.preview.contains(mx, my)) {
         this.setting.getPreview().run();
         return true;
      } else if (Density.compact()) {
         return this.choice.mouseDown(this, mx, my, button);
      } else {
         int i = Segmented.hit(this.controlX(), this.controlY(), this.controlW(), 20.0F, this.labels.length, mx, my);
         if (button == 0 && i >= 0) {
            if (i != this.setting.getIndex()) {
               this.setting.setIndex(i);
               ModSounds.gui(Sfx.CLICK);
            }

            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (this.setting.getPreview() != null && this.preview.contains(mx, my)) {
         return PreviewButton.tooltip();
      } else if (Density.compact()) {
         return this.choice.tooltip(this, mx, my);
      } else {
         return this.contains(mx, my) ? this.setting.getDescription() : null;
      }
   }
}
