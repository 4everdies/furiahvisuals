package meowtils.furiahvisuals.gui.widget;

import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.popup.Popover;
import meowtils.furiahvisuals.gui.popup.Popovers;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.TextSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.gui.GuiScreen;

public final class TextFieldWidget extends Widget {
   private final TextSetting setting;
   private boolean focused;
   private boolean allSelected;
   private float caret;
   private float focus;
   private boolean fieldOnly;

   public TextFieldWidget(TextSetting setting) {
      this.setting = setting;
   }

   TextFieldWidget fieldOnly() {
      this.fieldOnly = true;
      this.focused = true;
      return this;
   }

   private float fieldH() {
      return Density.compact() ? Density.control() + 2.0F : 20.0F;
   }

   @Override
   public float height() {
      return Density.compact() ? Density.row() : 28.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private float fieldW() {
      if (this.fieldOnly) {
         return this.w;
      } else {
         return Density.compact() ? Math.max(90.0F, Math.min(200.0F, this.w * 0.5F)) : Math.max(120.0F, Math.min(260.0F, this.w * 0.56F));
      }
   }

   private float fieldX() {
      return this.x + this.w - this.fieldW();
   }

   private float fieldY() {
      return this.y + (this.height() - this.fieldH()) / 2.0F;
   }

   private float textSize() {
      return Density.compact() ? Density.small() + 0.4F : 9.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.caret += dt;
      float fh = this.fieldH();
      float ts = this.textSize();
      boolean overField = mx >= this.fieldX() && mx < this.fieldX() + this.fieldW() && my >= this.fieldY() && my < this.fieldY() + fh;
      this.animateHover(overField, dt);
      this.focus = Easing.approach(this.focus, this.focused ? 1.0F : 0.0F, 16.0F, dt);
      if (!this.fieldOnly) {
         float ls = Density.text();
         Ui.textMiddle(
            Ui.ellipsize(this.setting.getLabel(), ls, this.w - this.fieldW() - 12.0F, false), this.x, this.y + this.height() / 2.0F, ls, Theme.TEXT, false
         );
      }

      float fx = this.fieldX();
      float fy = this.fieldY();
      float fw = this.fieldW();
      Ui.surface(fx, fy, fw, fh, 6.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, Math.max(this.hover, this.focus)));
      if (this.focus > 0.99F) {
         Ui.accentOutline(fx, fy, fw, fh, 6.0F, 0.9F, accent);
      } else {
         Ui.outline(fx, fy, fw, fh, 6.0F, 0.9F, ColorUtil.lerp(Theme.OUTLINE, accent | 0xFF000000, this.focus));
      }

      String text = this.setting.get();
      String counter = this.focused ? text.length() + "/" + this.setting.getMaxLength() : "";
      float counterW = counter.isEmpty() ? 0.0F : Ui.width(counter, 7.4F) + 6.0F;
      float room = fw - 16.0F - counterW;
      if (text.isEmpty() && !this.focused) {
         Ui.textMiddle("Empty", fx + 8.0F, fy + fh / 2.0F, ts, Theme.TEXT_MUTED, false);
      } else {
         String shown = tail(text, ts, room);
         if (this.allSelected && this.focused && !text.isEmpty()) {
            Ui.roundRect(fx + 7.0F, fy + 3.0F, Ui.width(shown, ts) + 2.0F, fh - 6.0F, 2.0F, Theme.ACCENT_SELECTION);
         }

         Ui.textMiddle(shown, fx + 8.0F, fy + fh / 2.0F, ts, Theme.TEXT, false);
         if (this.focused && !this.allSelected && this.caret % 1.0F < 0.55F) {
            Ui.rect(fx + 8.0F + Ui.width(shown, ts) + 1.0F, fy + 3.5F, 0.8F, fh - 7.0F, Theme.TEXT);
         }
      }

      if (!counter.isEmpty()) {
         Ui.textMiddle(
            counter,
            fx + fw - 7.0F - Ui.width(counter, 7.4F),
            fy + fh / 2.0F,
            7.4F,
            text.length() >= this.setting.getMaxLength() ? accent : Theme.TEXT_MUTED,
            false
         );
      }
   }

   private static String tail(String s, float size, float maxWidth) {
      if (Ui.width(s, size) <= maxWidth) {
         return s;
      } else {
         for (int i = 1; i < s.length(); i++) {
            String t = "…" + s.substring(i);
            if (Ui.width(t, size) <= maxWidth) {
               return t;
            }
         }

         return "…";
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      boolean inField = mx >= this.fieldX() && mx < this.fieldX() + this.fieldW() && my >= this.fieldY() && my < this.fieldY() + this.fieldH();
      if (!this.contains(mx, my)) {
         return false;
      } else if (button == 1 && !Density.compact() && !this.fieldOnly) {
         this.setting.reset();
         ModSounds.gui(Sfx.CLICK);
         return true;
      } else if (button != 0 || !inField && (!Density.compact() || this.fieldOnly)) {
         if (this.focused) {
            this.unfocus();
         }

         return false;
      } else if (Density.compact() && !this.fieldOnly && Ui.width(this.setting.get(), this.textSize()) > this.fieldW() - 20.0F) {
         ModSounds.gui(Sfx.DROPDOWN);
         Popovers.open(new TextFieldWidget.TextPopover(this.setting), this.x, this.y, this.w, this.height());
         return true;
      } else {
         if (!this.focused) {
            ModSounds.gui(Sfx.CLICK);
         }

         this.focused = true;
         this.allSelected = false;
         this.caret = 0.0F;
         return true;
      }
   }

   @Override
   public boolean keyTyped(char c, int key) {
      if (!this.focused) {
         return false;
      } else {
         this.caret = 0.0F;
         if (key != 1 && key != 28 && key != 156) {
            String text = this.setting.get();
            if (GuiScreen.isKeyComboCtrlA(key)) {
               this.allSelected = !text.isEmpty();
               return true;
            } else if (key == 14) {
               this.setting.set(!this.allSelected && !text.isEmpty() ? text.substring(0, text.length() - 1) : "");
               this.allSelected = false;
               ModSounds.key();
               return true;
            } else {
               String add;
               if (GuiScreen.isKeyComboCtrlV(key)) {
                  add = GuiScreen.getClipboardString();
               } else {
                  if (c < ' ' || c == 127) {
                     return true;
                  }

                  add = String.valueOf(c);
               }

               this.setting.set((this.allSelected ? "" : text) + TextSetting.clean(add, this.setting.getMaxLength()));
               this.allSelected = false;
               ModSounds.key();
               return true;
            }
         } else {
            this.unfocus();
            return true;
         }
      }
   }

   @Override
   public boolean isFocused() {
      return this.focused;
   }

   @Override
   public void unfocus() {
      this.focused = false;
      this.allSelected = false;
   }

   @Override
   public String tooltip(float mx, float my) {
      if (this.fieldOnly) {
         return null;
      } else {
         return this.contains(mx, my) && !this.focused ? this.setting.getDescription() : null;
      }
   }

   static final class TextPopover extends Popover {
      private static final float PAD = 7.0F;
      private final TextSetting setting;
      private final TextFieldWidget field;

      TextPopover(TextSetting setting) {
         this.setting = setting;
         this.field = new TextFieldWidget(setting).fieldOnly();
         this.field.appear = 1.0F;
      }

      @Override
      protected float preferredW() {
         return 300.0F;
      }

      @Override
      protected float preferredH() {
         return 26.0F + this.field.height();
      }

      @Override
      protected void renderContent(float mx, float my, float dt, int accent) {
         Ui.textMiddle(Ui.ellipsize(this.setting.getLabel(), 7.4F, this.w - 14.0F, false), this.x + 7.0F, this.y + 7.0F + 4.0F, 7.4F, Theme.TEXT_MUTED, false);
         this.field.x = this.x + 7.0F;
         this.field.y = this.y + 7.0F + 12.0F;
         this.field.w = this.w - 14.0F;
         this.field.render(mx, my, dt, accent);
         if (!this.field.isFocused()) {
            Popovers.close(this);
         }
      }

      @Override
      public boolean mouseDown(float mx, float my, int button) {
         this.field.mouseDown(mx, my, button);
         return true;
      }

      @Override
      public boolean wantsKeys() {
         return true;
      }

      @Override
      public boolean keyTyped(char c, int key) {
         boolean used = this.field.keyTyped(c, key);
         if (!this.field.isFocused()) {
            Popovers.close(this);
         }

         return used;
      }

      @Override
      protected void closed() {
         this.field.unfocus();
      }
   }
}
