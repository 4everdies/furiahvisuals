package meowtils.furiahvisuals.module.kill;

import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.EnumChoices;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.StringListSetting;

final class StyleSetSetting extends StringListSetting implements HasWidget, EnumChoices {
   StyleSetSetting(String id, String name, String description) {
      super(id, name, description, allNames());
   }

   @Override
   public Enum<?>[] choices() {
      return KillStyle.values();
   }

   private static String[] allNames() {
      KillStyle[] all = KillStyle.values();
      String[] names = new String[all.length];

      for (int i = 0; i < all.length; i++) {
         names[i] = all[i].name();
      }

      return names;
   }

   boolean includes(KillStyle style) {
      return this.contains(style.name());
   }

   void toggle(KillStyle style) {
      if (this.includes(style)) {
         this.remove(style.name());
      } else {
         this.add(style.name());
      }
   }

   @Override
   public boolean showInMenu() {
      return true;
   }

   @Override
   public Widget createWidget() {
      return new StyleSetSetting.SetWidget(this);
   }

   private static final class SetWidget extends Widget {
      private static final float CHIP_H = 20.0F;
      private static final float GAP = 5.0F;
      private static final float TEXT = 8.8F;
      private static final float PILL_TEXT = 5.6F;
      private final StyleSetSetting setting;
      private final KillStyle[] styles = KillStyle.values();
      private final float[] chipX = new float[this.styles.length];
      private final float[] chipY = new float[this.styles.length];
      private final float[] chipW = new float[this.styles.length];
      private final float[] chipHover = new float[this.styles.length];
      private float chipsH = 20.0F;

      SetWidget(StyleSetSetting setting) {
         this.setting = setting;
      }

      @Override
      public float height() {
         return 22.0F + this.chipsH + 6.0F;
      }

      @Override
      public boolean isVisible() {
         return this.setting.isVisible();
      }

      private static boolean isNew(KillStyle s) {
         return s.isNew() && !MenuMemory.seen("card.kill_effects." + s.name());
      }

      private static float pillW() {
         return Ui.widthBold("NEW", 5.6F) + 8.0F;
      }

      private void layout() {
         float cx = this.x;
         float cy = this.y + 22.0F;

         for (int i = 0; i < this.styles.length; i++) {
            float cw = Ui.width(this.styles[i].displayName(), 8.8F) + 18.0F + (isNew(this.styles[i]) ? pillW() + 4.0F : 0.0F);
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
         Ui.text(this.setting.getName(), this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
         int n = 0;

         for (KillStyle s : this.styles) {
            n += this.setting.includes(s) ? 1 : 0;
         }

         String note = n == 0 ? "none: the selected style plays" : n + " of " + this.styles.length;
         Ui.text(note, this.x + this.w - Ui.width(note, 7.8F), this.y + 5.5F, 7.8F, Theme.TEXT_MUTED);

         for (int i = 0; i < this.styles.length; i++) {
            boolean over = mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 20.0F;
            this.chipHover[i] = Easing.approach(this.chipHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
            boolean on = this.setting.includes(this.styles[i]);
            if (on) {
               Ui.roundRect(
                  this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, this.chipHover[i])
               );
               Ui.accentOutline(this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, 0.9F, accent);
            } else {
               Ui.surface(this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.chipHover[i]));
            }

            int text = on ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT_DIM, this.chipHover[i]);
            Ui.textMiddle(this.styles[i].displayName(), this.chipX[i] + 9.0F, this.chipY[i] + 10.0F, 8.8F, text, on);
            if (isNew(this.styles[i])) {
               float pw = pillW();
               float ph = 10.0F;
               float px = this.chipX[i] + this.chipW[i] - 9.0F - pw + 4.0F;
               float py = this.chipY[i] + (20.0F - ph) / 2.0F;
               Ui.roundRect(px, py, pw, ph, ph / 2.0F, ColorUtil.withAlpha(accent, 0.92F));
               Ui.textMiddle("NEW", px + 4.0F, py + ph / 2.0F, 5.6F, -1, true);
            }
         }
      }

      @Override
      public boolean mouseDown(float mx, float my, int button) {
         if (this.contains(mx, my) && button == 0) {
            for (int i = 0; i < this.styles.length; i++) {
               if (mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 20.0F) {
                  this.setting.toggle(this.styles[i]);
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
            for (int i = 0; i < this.styles.length; i++) {
               if (mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 20.0F) {
                  return (this.setting.includes(this.styles[i]) ? "In the set: " : "Not in the set: ") + this.styles[i].description();
               }
            }

            return this.setting.getDescription();
         }
      }
   }
}
