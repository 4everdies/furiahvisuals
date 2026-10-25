package meowtils.furiahvisuals.module.ui.crosshair;

import java.util.List;
import meowtils.furiahvisuals.core.Keybinds;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;

final class CrosshairProfilesWidget extends RowWidget {
   private static final float CHIP_H = 20.0F;
   private static final float GAP = 6.0F;
   private static final float TEXT = 8.8F;
   private static final float LINE = 13.0F;
   private final CrosshairElement element;
   private final Pill save = new Pill();
   private final Pill copy = new Pill();
   private final Pill paste = new Pill();
   private float[] hovers = new float[0];
   private float[] chipX = new float[0];
   private float[] chipY = new float[0];
   private float[] chipW = new float[0];
   private float buttonsY;
   private float messageY;

   CrosshairProfilesWidget(CrosshairElement element) {
      this.element = element;
   }

   private float layoutChips() {
      List<String> names = this.element.profiles.names();
      if (this.chipX.length != names.size()) {
         this.chipX = new float[names.size()];
         this.chipY = new float[names.size()];
         this.chipW = new float[names.size()];
         this.hovers = new float[names.size()];
      }

      float cx = this.x;
      float cy = this.y;

      for (int i = 0; i < names.size(); i++) {
         float cw = Ui.width(names.get(i), 8.8F) + 34.0F;
         if (cx > this.x && cx + cw > this.x + this.w) {
            cx = this.x;
            cy += 25.0F;
         }

         this.chipX[i] = cx;
         this.chipY[i] = cy;
         this.chipW[i] = cw;
         cx += cw + 6.0F;
      }

      return names.isEmpty() ? 14.0F : cy - this.y + 20.0F;
   }

   @Override
   public float height() {
      return this.layoutChips() + 8.0F + 20.0F + 6.0F + 26.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      CrosshairProfiles p = this.element.profiles;
      List<String> names = p.names();
      float chipsH = this.layoutChips();
      if (names.isEmpty()) {
         Ui.text("No profiles yet: Save keeps the whole setup, situational crosshairs included.", this.x, this.y + 2.0F, 8.0F, Theme.TEXT_MUTED);
      }

      int active = p.active();
      boolean modified = p.modified();

      for (int i = 0; i < names.size(); i++) {
         boolean over = mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 20.0F;
         this.hovers[i] = Easing.approach(this.hovers[i], over ? 1.0F : 0.0F, 16.0F, dt);
         boolean on = i == active;
         if (on) {
            Ui.roundRect(this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, this.hovers[i]));
            Ui.accentOutline(this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, 0.9F, accent);
         } else {
            Ui.surface(this.chipX[i], this.chipY[i], this.chipW[i], 20.0F, 10.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hovers[i]));
         }

         Ui.textMiddle(
            names.get(i), this.chipX[i] + 10.0F, this.chipY[i] + 10.0F, 8.8F, on ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hovers[i]), on
         );
         float right = this.chipX[i] + this.chipW[i] - 12.0F;
         if (this.hovers[i] > 0.05F) {
            Ui.textMiddle("×", right - 2.0F, this.chipY[i] + 10.0F - 0.5F, 10.0F, ColorUtil.withAlpha(-34182, this.hovers[i]), true);
         } else if (on && modified) {
            Ui.circle(right, this.chipY[i] + 10.0F, 2.6F, accent);
         }
      }

      this.buttonsY = this.y + chipsH + 8.0F;
      float bx = this.x;
      bx += pill(this.save, "Save", bx, this.buttonsY, mx, my, dt, accent, true) + 6.0F;
      bx += pill(this.copy, "Copy Code", bx, this.buttonsY, mx, my, dt, accent, false) + 6.0F;
      pill(this.paste, "Paste Code", bx, this.buttonsY, mx, my, dt, accent, false);
      float ty;
      this.messageY = ty = this.buttonsY + 20.0F + 6.0F;
      String msg = p.message();
      if (msg != null) {
         Ui.text(Ui.ellipsize(msg, 8.0F, this.w, false), this.x, ty, 8.0F, p.messageIsError() ? -34182 : Theme.TEXT_DIM);
      }

      int key = Keybinds.nextCrosshairProfile == null ? 0 : Keybinds.nextCrosshairProfile.getKeyCode();
      String keyName = key == 0 ? "not bound" : GameSettings.getKeyDisplayString(key);
      Ui.text("Next crosshair profile: " + keyName + " (Options → Controls → Furiah's Visuals)", this.x, ty + 13.0F, 8.0F, Theme.TEXT_MUTED);
   }

   private static float pill(Pill p, String label, float px, float py, float mx, float my, float dt, int accent, boolean main) {
      float pw = p.layout(px, py, label);
      p.draw(label, mx, my, dt, accent, main, true);
      return pw;
   }

   private int chipAt(float mx, float my) {
      for (int i = 0; i < this.chipX.length; i++) {
         if (mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 20.0F) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         CrosshairProfiles p = this.element.profiles;
         int i = this.chipAt(mx, my);
         if (i >= 0) {
            if (mx >= this.chipX[i] + this.chipW[i] - 20.0F) {
               p.delete(i);
               ModSounds.gui(Sfx.TOGGLE_OFF);
            } else {
               p.load(i);
               ModSounds.gui(Sfx.SELECT);
            }
         } else if (this.save.contains(mx, my)) {
            p.save(p.nameSetting().get());
            ModSounds.gui(Sfx.SUCCESS);
         } else if (this.copy.contains(mx, my)) {
            String code = p.exportCode();
            GuiScreen.setClipboardString(code);
            ModSounds.gui(Sfx.CLICK);
            this.element.profiles.sayCopied(code.length());
         } else if (this.paste.contains(mx, my)) {
            String msg = p.importCode(GuiScreen.getClipboardString(), false);
            ModSounds.gui(p.messageIsError() ? Sfx.ERROR : Sfx.SUCCESS);
            if (msg == null) {
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
      int i = this.chipAt(mx, my);
      if (i >= 0) {
         return mx >= this.chipX[i] + this.chipW[i] - 20.0F ? "Delete this profile" : "Load this profile";
      } else if (this.save.contains(mx, my)) {
         return "Saves the whole setup as a profile (the name above; a profile with that name is updated)";
      } else if (this.copy.contains(mx, my)) {
         return "Copies the whole setup as a code to share";
      } else if (this.paste.contains(mx, my)) {
         return "Loads a crosshair code from the clipboard";
      } else {
         String msg = this.element.profiles.message();
         return msg != null && my >= this.messageY - 2.0F && my < this.messageY + 13.0F - 2.0F && Ui.width(msg, 8.0F) > this.w ? msg : null;
      }
   }
}
