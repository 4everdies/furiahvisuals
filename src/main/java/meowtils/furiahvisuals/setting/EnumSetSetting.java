package meowtils.furiahvisuals.setting;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

public class EnumSetSetting<E extends Enum<E>> extends StringListSetting implements HasWidget, EnumChoices {
   private final E[] values;
   private final boolean atLeastOne;
   private int cachedVersion = -1;
   private final boolean[] cached;

   @SafeVarargs
   public EnumSetSetting(String id, String name, String description, Class<E> type, boolean atLeastOne, E... defaults) {
      super(id, name, description, names(defaults));
      this.values = type.getEnumConstants();
      this.atLeastOne = atLeastOne;
      this.cached = new boolean[this.values.length];
   }

   private static String[] names(Enum<?>[] defaults) {
      String[] out = new String[defaults.length];

      for (int i = 0; i < defaults.length; i++) {
         out[i] = defaults[i].name();
      }

      return out;
   }

   public E[] values() {
      return this.values;
   }

   @Override
   public Enum<?>[] choices() {
      return this.values;
   }

   public boolean includes(E value) {
      if (this.cachedVersion != this.version()) {
         this.cachedVersion = this.version();

         for (int i = 0; i < this.values.length; i++) {
            this.cached[i] = this.contains(this.values[i].name());
         }
      }

      return this.cached[value.ordinal()];
   }

   public int count() {
      int n = 0;

      for (E v : this.values) {
         n += this.includes(v) ? 1 : 0;
      }

      return n;
   }

   public boolean toggle(E value) {
      if (this.includes(value)) {
         if (this.atLeastOne && this.count() <= 1) {
            return false;
         }

         this.remove(value.name());
      } else {
         this.add(value.name());
      }

      return true;
   }

   @Override
   public boolean showInMenu() {
      return true;
   }

   @Override
   public Widget createWidget() {
      return new EnumSetSetting.SetWidget<>(this);
   }

   private static final class SetWidget<E extends Enum<E>> extends Widget {
      private static final float CHIP_H = 20.0F;
      private static final float GAP = 5.0F;
      private static final float TEXT = 8.8F;
      private final EnumSetSetting<E> setting;
      private final E[] values;
      private final float[] chipX;
      private final float[] chipY;
      private final float[] chipW;
      private final float[] chipHover;
      private float chipsH = 20.0F;

      SetWidget(EnumSetSetting<E> setting) {
         this.setting = setting;
         this.values = setting.values;
         this.chipX = new float[this.values.length];
         this.chipY = new float[this.values.length];
         this.chipW = new float[this.values.length];
         this.chipHover = new float[this.values.length];
      }

      @Override
      public float height() {
         if (this.w > 0.0F) {
            this.layout();
         }

         return 22.0F + this.chipsH + 6.0F;
      }

      @Override
      public boolean isVisible() {
         return this.setting.isVisible();
      }

      private void layout() {
         float cx = this.x;
         float cy = this.y + 22.0F;

         for (int i = 0; i < this.values.length; i++) {
            float cw = Ui.width(ModeSetting.displayName(this.values[i]), 8.8F) + 18.0F;
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
         this.layout();
         Ui.text(this.setting.getLabel(), this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
         String note = this.setting.count() + " of " + this.values.length;
         Ui.text(note, this.x + this.w - Ui.width(note, 7.8F), this.y + 5.5F, 7.8F, Theme.TEXT_MUTED);

         for (int i = 0; i < this.values.length; i++) {
            boolean over = mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 20.0F;
            this.chipHover[i] = Easing.approach(this.chipHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
            boolean on = this.setting.includes(this.values[i]);
            if (on) {
               Ui.roundRect(
                  this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, this.chipHover[i])
               );
               Ui.accentOutline(this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, 0.9F, accent);
            } else {
               Ui.surface(this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.chipHover[i]));
            }

            int text = on ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT_DIM, this.chipHover[i]);
            Ui.textMiddle(ModeSetting.displayName(this.values[i]), this.chipX[i] + 9.0F, this.chipY[i] + 10.0F, 8.8F, text, on);
            Ui.registerControl(this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, "chip " + ModeSetting.displayName(this.values[i]));
         }
      }

      @Override
      public boolean mouseDown(float mx, float my, int button) {
         if (this.contains(mx, my) && button == 0) {
            for (int i = 0; i < this.values.length; i++) {
               if (mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 20.0F) {
                  if (this.setting.toggle(this.values[i])) {
                     ModSounds.toggle(this.setting.includes(this.values[i]));
                  } else {
                     ModSounds.gui(Sfx.ERROR);
                  }

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
         if (!this.contains(mx, my)) {
            return null;
         } else {
            for (int i = 0; i < this.values.length; i++) {
               if (mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 20.0F) {
                  boolean on = this.setting.includes(this.values[i]);
                  String d = ModeSetting.description(this.values[i]);
                  if (on && this.setting.atLeastOne && this.setting.count() <= 1) {
                     return "The last one stays: pick another first";
                  }

                  return (on ? "In the set (click to take it out)" : "Not in the set (click to add it)") + (d.isEmpty() ? "" : ": " + d);
               }
            }

            return this.setting.getDescription();
         }
      }
   }
}
