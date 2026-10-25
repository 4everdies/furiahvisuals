package meowtils.furiahvisuals.gui.widget;

import java.util.Locale;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.gui.GuiScreen;

public final class SliderWidget extends Widget {
   private final SliderSetting setting;
   private String shownLabel = "";
   private float shown = -1.0F;
   private float active;
   private boolean dragging;
   private final PreviewButton preview = new PreviewButton();
   private String typed;
   private float caret;
   private float valueHover;

   public SliderWidget(SliderSetting setting) {
      this.setting = setting;
   }

   @Override
   public float height() {
      return Density.compact() ? Density.row() : 36.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private float trackX() {
      return Density.compact() ? this.x + this.w - this.trackW() : this.x;
   }

   private float trackW() {
      return Density.compact() ? Math.max(56.0F, Math.min(150.0F, this.w * 0.38F)) : this.w;
   }

   private float trackY() {
      return Density.compact() ? this.y + this.height() / 2.0F : this.y + 25.0F;
   }

   public SliderSetting getSetting() {
      return this.setting;
   }

   public float[] fillMiddleForCheck() {
      return new float[]{this.trackX() + this.trackW() * Math.max(0.0F, this.shown) / 2.0F, this.trackY()};
   }

   private float[] valueBox() {
      String v = this.typed != null ? this.typed + "  " : this.setting.format();
      if (Density.compact()) {
         float s = Density.small();
         float vw = Math.max(Ui.widthBold(v, s), Ui.width("0000", s)) + 8.0F;
         float ch = Density.control();
         return new float[]{this.trackX() - 7.0F - vw, this.y + (this.height() - ch) / 2.0F, vw, ch};
      } else {
         float vw = Ui.widthBold(v, 9.2F) + 8.0F;
         return new float[]{this.x + this.w - vw + 4.0F, this.y + 1.0F, vw, 14.0F};
      }
   }

   private boolean overValue(float mx, float my) {
      float[] b = this.valueBox();
      return mx >= b[0] - 2.0F && mx < b[0] + b[2] + 2.0F && my >= b[1] - 1.0F && my < b[1] + b[3] + 1.0F;
   }

   private boolean overTrack(float mx, float my) {
      return Density.compact()
         ? mx >= this.trackX() - 5.0F && mx < this.x + this.w + 5.0F && my >= this.y && my < this.y + this.height()
         : this.contains(mx, my) && my >= this.y + 16.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.caret += dt;
      this.animateHover(this.contains(mx, my), dt);
      this.valueHover = Easing.approach(this.valueHover, !this.overValue(mx, my) && this.typed == null ? 0.0F : 1.0F, 16.0F, dt);
      this.active = Easing.approach(this.active, !this.dragging && this.typed == null ? this.hover : 1.0F, 16.0F, dt);
      float target = (float)this.setting.getFraction();
      float f = this.shown = !(this.shown < 0.0F) && !this.dragging ? Easing.approach(this.shown, target, 20.0F, dt) : target;
      if (Density.compact()) {
         this.renderCompact(mx, my, dt, accent);
      } else {
         float[] vb = this.valueBox();
         String label;
         this.shownLabel = label = Ui.ellipsize(
            this.setting.getLabel(), 10.0F, vb[0] - 8.0F - this.x - (this.setting.getPreview() != null ? 20.0F : 0.0F), false
         );
         Ui.text(label, this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
         if (this.setting.getPreview() != null) {
            this.preview.render(this.x + Ui.width(label, 10.0F) + 13.0F, this.y + 9.5F, mx, my, dt, accent);
         }

         this.drawValue(vb, 9.2F, accent);
         String info = this.setting.getInfo();
         float room;
         if (info != null && !info.isEmpty() && (room = vb[0] - 6.0F - this.x - Ui.width(label, 10.0F) - 10.0F) > 24.0F) {
            String note = Ui.ellipsize(info, 8.0F, room, false);
            Ui.text(note, vb[0] - 6.0F - Ui.width(note, 8.0F), this.y + 5.4F, 8.0F, this.setting.isInfoWarning() ? Theme.DANGER : Theme.TEXT_MUTED);
         }

         float ty = this.trackY();
         float th = 4.0F;
         float kx = this.x + this.w * this.shown;
         float r = 5.0F + 1.2F * this.active;
         if (this.active > 0.01F) {
            Ui.glow(kx, ty, 30.0F, ColorUtil.withAlpha(accent, 0.35F * this.active));
         }

         Ui.roundRect(this.x, ty - th / 2.0F, this.w, th, th / 2.0F, Theme.TRACK);
         float fillW = Math.max(th, this.w * this.shown);
         Ui.roundRect(this.x, ty - th / 2.0F, fillW, th, th / 2.0F, accent | 0xFF000000);
         Ui.shadow(kx - r, ty - r + 1.0F, r * 2.0F, r * 2.0F, r, 1.5F, 1711276032);
         Ui.circle(kx, ty, r, Theme.KNOB);
         Ui.circle(kx, ty, r * 0.42F, ColorUtil.withAlpha(accent, this.active));
         Ui.registerControl(this.x - 5.0F, ty - 6.0F, this.w + 10.0F, 12.0F, "slider " + this.setting.getLabel());
         Ui.registerControl(vb[0], vb[1], vb[2], vb[3], "value " + this.setting.getLabel());
      }
   }

   private void renderCompact(float mx, float my, float dt, int accent) {
      float h = this.height();
      float cy = this.y + h / 2.0F;
      float ts = Density.text();
      if (this.hover > 0.01F) {
         Ui.roundRect(this.x - 4.0F, this.y, this.w + 8.0F, h, 5.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, this.hover * 0.8F));
      }

      float[] vb = this.valueBox();
      float labelRoom = vb[0] - this.x - 6.0F - (this.setting.getPreview() != null ? 16.0F : 0.0F);
      String label;
      this.shownLabel = label = Ui.ellipsize(this.setting.getLabel(), ts, labelRoom, false);
      Ui.textMiddle(label, this.x, cy, ts, Theme.TEXT, false);
      if (this.setting.getPreview() != null) {
         this.preview.render(this.x + Ui.width(label, ts) + 11.0F, cy, mx, my, dt, accent);
      }

      String info;
      if ((info = this.setting.getInfo()) != null && !info.isEmpty() && this.setting.isInfoWarning()) {
         Ui.circle(vb[0] - 4.0F, cy, 1.8F, Theme.DANGER);
      }

      this.drawValue(vb, Density.small(), accent);
      float tx = this.trackX();
      float tw = this.trackW();
      float th = Density.dense() ? 2.2F : 2.6F;
      float kx = tx + tw * this.shown;
      float r = (Density.dense() ? 3.2F : 3.8F) + 0.9F * this.active;
      if (this.active > 0.01F) {
         Ui.glow(kx, cy, 20.0F, ColorUtil.withAlpha(accent, 0.3F * this.active));
      }

      Ui.roundRect(tx, cy - th / 2.0F, tw, th, th / 2.0F, Theme.TRACK);
      Ui.roundRect(tx, cy - th / 2.0F, Math.max(th, tw * this.shown), th, th / 2.0F, accent | 0xFF000000);
      Ui.shadow(kx - r, cy - r + 0.8F, r * 2.0F, r * 2.0F, r, 1.2F, 1426063360);
      Ui.circle(kx, cy, r, Theme.KNOB);
      Ui.circle(kx, cy, r * 0.42F, ColorUtil.withAlpha(accent, this.active));
      Ui.registerControl(tx - 4.0F, cy - 5.0F, tw + 8.0F, 10.0F, "slider " + this.setting.getLabel());
      Ui.registerControl(vb[0], vb[1], vb[2], vb[3], "value " + this.setting.getLabel());
   }

   private void drawValue(float[] b, float size, int accent) {
      if (this.typed != null) {
         Ui.roundRect(b[0], b[1], b[2], b[3], 4.0F, Theme.SURFACE_HOVER);
         Ui.accentOutline(b[0], b[1], b[2], b[3], 4.0F, 0.9F, accent);
         Ui.textMiddle(this.typed, b[0] + 4.0F, b[1] + b[3] / 2.0F, size, Theme.TEXT, true);
         if (this.caret % 1.0F < 0.55F) {
            Ui.rect(b[0] + 4.0F + Ui.widthBold(this.typed, size) + 0.6F, b[1] + 3.0F, 0.8F, b[3] - 6.0F, Theme.TEXT);
         }
      } else {
         if (this.valueHover > 0.01F) {
            Ui.roundRect(b[0], b[1], b[2], b[3], 4.0F, ColorUtil.mulAlpha(Theme.SURFACE, this.valueHover * 1.6F));
         }

         String v = this.setting.format();
         Ui.textMiddle(
            v,
            b[0] + b[2] - 4.0F - Ui.widthBold(v, size),
            b[1] + b[3] / 2.0F,
            size,
            ColorUtil.lerp(Theme.TEXT_DIM, accent, Math.max(this.active, this.valueHover * 0.6F)),
            true
         );
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.setting.getPreview() != null && this.preview.contains(mx, my)) {
         this.setting.getPreview().run();
         return true;
      } else {
         if (this.typed != null && !this.overValue(mx, my)) {
            this.commit();
         }

         if (!this.contains(mx, my)) {
            return false;
         } else if (button == 1 && !Density.compact()) {
            this.setting.reset();
            ModSounds.gui(Sfx.CLICK);
            return true;
         } else if (button != 0) {
            return false;
         } else if (this.overValue(mx, my)) {
            this.typed = this.typed == null ? (this.setting.isClock() ? this.setting.format() : plain(this.setting.format())) : this.typed;
            this.caret = 0.0F;
            ModSounds.gui(Sfx.CLICK);
            return true;
         } else if (Density.compact() && !this.overTrack(mx, my)) {
            return true;
         } else {
            this.dragging = true;
            this.mouseDrag(mx, my);
            return true;
         }
      }
   }

   private static String plain(String formatted) {
      StringBuilder sb = new StringBuilder();

      for (char c : formatted.toCharArray()) {
         if ((c < '0' || c > '9') && c != '.' && c != '-') {
            if (sb.length() > 0) {
               break;
            }
         } else {
            sb.append(c);
         }
      }

      return sb.toString();
   }

   @Override
   public void mouseDrag(float mx, float my) {
      if (this.dragging) {
         double before = this.setting.get();
         this.setting.setFraction((mx - this.trackX()) / this.trackW());
         if (this.setting.get() != before) {
            ModSounds.sliderTick(this.setting.getFraction());
         }
      }
   }

   @Override
   public void mouseUp(float mx, float my) {
      if (this.dragging) {
         ModSounds.gui(Sfx.SLIDER_RELEASE);
      }

      this.dragging = false;
   }

   @Override
   public boolean mouseWheel(float mx, float my, int wheel) {
      return false;
   }

   @Override
   public boolean keyTyped(char c, int key) {
      if (this.typed == null) {
         return false;
      } else {
         this.caret = 0.0F;
         if (key == 1) {
            this.typed = null;
            return true;
         } else if (key == 28 || key == 156) {
            this.commit();
            return true;
         } else if (key == 14) {
            if (!this.typed.isEmpty()) {
               this.typed = this.typed.substring(0, this.typed.length() - 1);
               ModSounds.key();
            }

            return true;
         } else if (GuiScreen.isKeyComboCtrlA(key)) {
            this.typed = "";
            return true;
         } else {
            boolean clock = this.setting.isClock();
            boolean ok = clock
               ? c >= '0' && c <= '9' || c == ':' && this.typed.indexOf(58) < 0
               : c >= '0' && c <= '9' || c == '.' || c == ',' || c == '-' && this.typed.isEmpty();
            if (ok && this.typed.length() < 12) {
               this.typed = this.typed + (c == ',' ? '.' : c);
               ModSounds.key();
            }

            return true;
         }
      }
   }

   private void commit() {
      String t = this.typed;
      this.typed = null;
      if (t != null && !t.isEmpty() && !t.equals("-") && !t.equals(".") && !t.equals(":")) {
         try {
            double before = this.setting.get();
            this.setting.set(this.setting.parse(t));
            if (this.setting.get() != before) {
               ModSounds.gui(Sfx.SLIDER_RELEASE);
            }
         } catch (NumberFormatException var4) {
            ModSounds.gui(Sfx.ERROR);
         }
      }
   }

   @Override
   public boolean isFocused() {
      return this.typed != null;
   }

   @Override
   public void unfocus() {
      if (this.typed != null) {
         this.commit();
      }
   }

   @Override
   public boolean isDragging() {
      return this.dragging;
   }

   public void holdForTest() {
      this.dragging = true;
   }

   public void typeForTest(String text) {
      this.typed = "";

      for (char c : text.toCharArray()) {
         this.keyTyped(c, 0);
      }

      this.keyTyped('\r', 28);
   }

   @Override
   public String tooltip(float mx, float my) {
      if (this.setting.getPreview() != null && this.preview.contains(mx, my)) {
         return PreviewButton.tooltip();
      } else if (!this.contains(mx, my) || this.dragging) {
         return null;
      } else if (this.typed != null) {
         return String.format(Locale.ROOT, "Type a value from %s to %s, then Enter (Esc cancels)", trim(this.setting.getMin()), trim(this.setting.getMax()));
      } else if (this.overValue(mx, my)) {
         return "Click to type a value";
      } else {
         String info = this.setting.getInfo();
         String d = this.setting.getDescription();
         if (Density.compact() && info != null && !info.isEmpty()) {
            d = d + "  (" + info + ")";
         }

         return !this.shownLabel.isEmpty() && !this.shownLabel.equals(this.setting.getLabel()) ? this.setting.getLabel() + ": " + d : d;
      }
   }

   private static String trim(double v) {
      return v == Math.rint(v) ? String.valueOf((long)v) : String.valueOf(v);
   }
}
