package meowtils.furiahvisuals.gui.widget;

import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.KeyBindSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

public final class KeyBindWidget extends Widget {
   private final KeyBindSetting setting;
   private boolean listening;
   private float listen;
   private float pulse;
   private float buttonX;
   private float buttonY;
   private float buttonW;
   private float buttonH;

   public KeyBindWidget(KeyBindSetting setting) {
      this.setting = setting;
   }

   @Override
   public float height() {
      return Density.compact() ? Density.row() : 28.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private float textSize() {
      return Density.compact() ? Density.small() + 0.4F : 9.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float h = this.height();
      float cy = this.y + h / 2.0F;
      float ts = this.textSize();
      this.listen = Easing.approach(this.listen, this.listening ? 1.0F : 0.0F, 16.0F, dt);
      this.pulse += dt;
      String key = this.listening ? "Press a key…" : this.setting.keyName();
      this.buttonH = Density.compact() ? Density.control() + 1.0F : 20.0F;
      this.buttonW = Math.max(Density.compact() ? 54.0F : 70.0F, Ui.width(key, ts) + 18.0F);
      this.buttonX = this.x + this.w - this.buttonW;
      this.buttonY = cy - this.buttonH / 2.0F;
      boolean over = mx >= this.buttonX && mx < this.buttonX + this.buttonW && my >= this.buttonY && my < this.buttonY + this.buttonH;
      this.animateHover(over, dt);
      String conflict = this.listening ? null : this.setting.conflict();
      String note = this.listening ? "Esc to cancel · Backspace for none" : (conflict != null ? "Also used by " + conflict : null);
      float noteW = note == null ? 0.0F : Math.min(Ui.width(note, Density.small()), Math.max(0.0F, this.w * 0.45F));
      float room = this.w - this.buttonW - 10.0F - (note == null ? 0.0F : noteW + 8.0F);
      float ls = Density.text();
      Ui.textMiddle(Ui.ellipsize(this.setting.getLabel(), ls, room, false), this.x, cy, ls, Theme.TEXT, false);
      if (note != null) {
         String shown = Ui.ellipsize(note, Density.small(), noteW, false);
         Ui.textMiddle(
            shown, this.buttonX - 8.0F - Ui.width(shown, Density.small()), cy, Density.small(), conflict != null ? Theme.DANGER : Theme.TEXT_MUTED, false
         );
      }

      Ui.registerControl(this.buttonX, this.buttonY, this.buttonW, this.buttonH, "key " + this.setting.getLabel());
      float glow = this.listening ? 0.55F + 0.45F * (float)Math.sin(this.pulse * 6.0) : 0.0F;
      Ui.surface(
         this.buttonX,
         this.buttonY,
         this.buttonW,
         this.buttonH,
         this.buttonH / 2.0F,
         ColorUtil.lerp(ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hover), Theme.ACCENT_SOFT, this.listen)
      );
      if (this.listen > 0.5F) {
         Ui.accentOutline(
            this.buttonX, this.buttonY, this.buttonW, this.buttonH, this.buttonH / 2.0F, 1.0F, ColorUtil.lerp(Theme.ACCENT, Theme.ACCENT_HOVER, glow)
         );
      } else {
         Ui.outline(this.buttonX, this.buttonY, this.buttonW, this.buttonH, this.buttonH / 2.0F, 0.8F, conflict != null ? Theme.DANGER : Theme.OUTLINE);
      }

      int text = this.listening
         ? Theme.TEXT
         : (this.setting.getKeyCode() == 0 ? Theme.TEXT_MUTED : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, 0.5F + 0.5F * this.hover));
      Ui.textMiddle(key, this.buttonX + (this.buttonW - Ui.width(key, ts)) / 2.0F, cy, ts, text, false);
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         this.unfocus();
         return false;
      } else if (this.listening) {
         this.listening = false;
         if (button >= 2) {
            this.setting.set(button - 100);
            ModSounds.gui(Sfx.SUCCESS);
         } else {
            ModSounds.gui(Sfx.CLICK);
         }

         return true;
      } else if (button == 1 && !Density.compact()) {
         this.setting.reset();
         ModSounds.gui(Sfx.CLICK);
         return true;
      } else if (button == 0) {
         this.listening = true;
         this.pulse = 0.0F;
         ModSounds.gui(Sfx.CLICK);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyTyped(char c, int key) {
      if (!this.listening) {
         return false;
      } else {
         this.listening = false;
         if (key != 1 && (key != 0 || c != 0)) {
            if (key != 14 && key != 211) {
               this.setting.set(key == 0 ? c + 256 : key);
               ModSounds.gui(Sfx.SUCCESS);
               return true;
            } else {
               this.setting.set(0);
               ModSounds.gui(Sfx.CLICK);
               return true;
            }
         } else {
            ModSounds.gui(Sfx.CLICK);
            return true;
         }
      }
   }

   @Override
   public boolean isFocused() {
      return this.listening;
   }

   @Override
   public void unfocus() {
      this.listening = false;
   }

   public boolean listeningForTest() {
      return this.listening;
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else if (mx >= this.buttonX) {
         return this.listening
            ? "Press the key (or a mouse button: middle, back, forward)"
            : "Click, then press a key: also in Options › Controls (" + this.setting.getLabel() + ")";
      } else {
         return this.setting.getDescription();
      }
   }
}
