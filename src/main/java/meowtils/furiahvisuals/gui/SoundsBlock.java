package meowtils.furiahvisuals.gui;

import java.util.Set;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.sound.SoundSettings;

final class SoundsBlock {
   private static final float HIDDEN = -10000.0F;
   private final Setting[] settings;
   private final Widget[] widgets;
   private Widget pressed;

   SoundsBlock(SoundSettings sounds) {
      this.settings = sounds.all();
      this.widgets = new Widget[this.settings.length];

      for (int i = 0; i < this.settings.length; i++) {
         this.widgets[i] = FuriahScreen.createWidget(null, this.settings[i]);
      }
   }

   static String key(Setting s) {
      return "sound:" + s.getId();
   }

   float render(float x, float y, float w, Set<String> keys, float mx, float my, float dt, int accent) {
      float top = y;

      for (int i = 0; i < this.widgets.length; i++) {
         Widget widget = this.widgets[i];
         boolean show = this.settings[i].isVisible() && (keys == null || keys.contains(key(this.settings[i])));
         if (!show) {
            widget.y = -10000.0F;
         } else {
            widget.x = x;
            widget.y = y;
            widget.w = w;
            widget.appear = 1.0F;
            widget.render(mx, my, dt, accent);
            y += widget.height() + 2.0F;
         }
      }

      return y - top;
   }

   boolean mouseDown(float mx, float my, int button) {
      this.pressed = null;

      for (Widget widget : this.widgets) {
         if (widget.y != -10000.0F && widget.mouseDown(mx, my, button)) {
            this.pressed = widget;
            return true;
         }
      }

      return false;
   }

   Widget pressed() {
      return this.pressed;
   }

   String tooltip(float mx, float my) {
      for (Widget widget : this.widgets) {
         String t;
         if (widget.y != -10000.0F && (t = widget.tooltip(mx, my)) != null) {
            return t;
         }
      }

      return null;
   }
}
