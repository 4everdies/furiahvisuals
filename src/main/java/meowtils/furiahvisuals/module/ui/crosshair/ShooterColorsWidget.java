package meowtils.furiahvisuals.module.ui.crosshair;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class ShooterColorsWidget extends RowWidget {
   private static final int[] COLORS = new int[]{-16711936, -256, -16711681, -65281, -65536, -1};
   private static final String[] NAMES = new String[]{"Green", "Yellow", "Cyan", "Magenta", "Red", "White"};
   private static final float H = 22.0F;
   private static final float R = 6.5F;
   private static final float STEP = 19.0F;
   private static final float LABEL = 8.5F;
   private final CrosshairElement element;
   private final float[] hovers = new float[COLORS.length];

   ShooterColorsWidget(CrosshairElement element) {
      this.element = element;
   }

   @Override
   public float height() {
      return 22.0F;
   }

   private float firstX() {
      return this.x + Ui.width("Shooter colors", 8.5F) + 14.0F + 6.5F;
   }

   private int chipAt(float mx, float my) {
      if (!(my < this.y) && !(my >= this.y + 22.0F)) {
         float fx = this.firstX();

         for (int i = 0; i < COLORS.length; i++) {
            float cx = fx + i * 19.0F;
            if (Math.abs(mx - cx) <= 9.0F) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      Ui.textMiddle("Shooter colors", this.x, this.y + 11.0F, 8.5F, Theme.TEXT_MUTED, false);
      ColorSetting c = this.element.editor.color;
      int hovered = this.chipAt(mx, my);
      float fx = this.firstX();

      for (int i = 0; i < COLORS.length; i++) {
         this.hovers[i] = Easing.approach(this.hovers[i], hovered == i ? 1.0F : 0.0F, 16.0F, dt);
         float cx = fx + i * 19.0F;
         float cy = this.y + 11.0F;
         boolean picked = c.getMode() == ColorSetting.Mode.STATIC && (c.getColorA() | 0xFF000000) == COLORS[i];
         float r = 6.5F + 1.0F * this.hovers[i];
         if (picked) {
            Ui.circle(cx, cy, r + 2.6F, accent | 0xFF000000);
            Ui.circle(cx, cy, r + 1.3F, Theme.SURFACE);
         }

         Ui.circle(cx, cy, r, COLORS[i]);
         Ui.outline(cx - r, cy - r, r * 2.0F, r * 2.0F, r, 0.8F, 1073741824);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         int i = this.chipAt(mx, my);
         if (i >= 0) {
            ColorSetting c = this.element.editor.color;
            c.setMode(ColorSetting.Mode.STATIC);
            c.setColorA(COLORS[i]);
            ModSounds.gui(Sfx.SELECT);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      int i = this.chipAt(mx, my);
      return i < 0 ? null : NAMES[i] + String.format(" (#%06X)", COLORS[i] & 16777215);
   }
}
