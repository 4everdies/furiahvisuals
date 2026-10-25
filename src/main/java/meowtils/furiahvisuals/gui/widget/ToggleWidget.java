package meowtils.furiahvisuals.gui.widget;

import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.sound.ModSounds;

public final class ToggleWidget extends Widget {
   private final BooleanSetting setting;
   private float knob = -1.0F;
   private final PreviewButton preview = new PreviewButton();
   private String shownLabel = "";

   public ToggleWidget(BooleanSetting setting) {
      this.setting = setting;
   }

   public BooleanSetting getSetting() {
      return this.setting;
   }

   @Override
   public float height() {
      return Density.compact() ? Density.row() : 26.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      boolean locked = false;
      this.animateHover(this.contains(mx, my) && !locked, dt);
      float target = this.setting.get() && !locked ? 1.0F : 0.0F;
      float f = this.knob = this.knob < 0.0F ? target : Easing.approach(this.knob, target, 18.0F, dt);
      if (Density.compact()) {
         this.renderCompact(mx, my, dt, accent, locked);
      } else {
         if (this.hover > 0.01F) {
            Ui.roundRect(this.x - 6.0F, this.y + 1.0F, this.w + 12.0F, this.height() - 2.0F, 6.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, this.hover));
         }

         float room = this.w - 28.0F - 10.0F - (this.setting.getPreview() != null ? 20.0F : 0.0F);
         this.shownLabel = Ui.ellipsize(this.setting.getLabel(), 10.0F, room, false);
         Ui.textMiddle(this.shownLabel, this.x, this.y + 13.0F, 10.0F, locked ? Theme.TEXT_DIM : Theme.TEXT, false);
         if (this.setting.getPreview() != null) {
            this.preview.render(this.x + Ui.width(this.shownLabel, 10.0F) + 13.0F, this.y + 13.0F, mx, my, dt, accent);
         }

         if (!locked) {
            drawSwitch(this.x + this.w - 28.0F, this.y + 13.0F - 7.5F, 28.0F, 15.0F, this.knob, accent);
         }
      }
   }

   private static float pillW() {
      return Density.dense() ? 25.0F : 28.0F;
   }

   private void renderCompact(float mx, float my, float dt, int accent, boolean locked) {
      float h = this.height();
      float cy = this.y + h / 2.0F;
      float ts = Density.text();
      if (this.hover > 0.01F) {
         Ui.roundRect(this.x - 4.0F, this.y, this.w + 8.0F, h, 5.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, this.hover * 0.8F));
      }

      float pw = pillW();
      float room = this.w - pw - 8.0F - (this.setting.getPreview() != null ? 16.0F : 0.0F);
      String label;
      this.shownLabel = label = Ui.ellipsize(this.setting.getLabel(), ts, room, false);
      Ui.textMiddle(label, this.x, cy, ts, locked ? Theme.TEXT_DIM : Theme.TEXT, false);
      if (this.setting.getPreview() != null) {
         this.preview.render(this.x + Ui.width(label, ts) + 11.0F, cy, mx, my, dt, accent);
      }

      if (!locked) {
         drawPill(this.x + this.w - pw, cy, pw, Density.control(), this.knob, accent, this.hover);
      }
   }

   public static void drawPill(float px, float cy, float pw, float ph, float t, int accent, float hover) {
      float py = cy - ph / 2.0F;
      Ui.registerControl(px, py, pw, ph, "ON/OFF pill");
      int fill = ColorUtil.lerp(ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hover), accent | 0xFF000000, t);
      if (t > 0.01F) {
         Ui.glow(px + pw / 2.0F, cy, pw * 1.5F, ColorUtil.withAlpha(accent, 0.18F * t));
      }

      Ui.roundRect(px, py, pw, ph, ph / 2.0F, fill);
      if (t < 0.99F) {
         Ui.outline(px, py, pw, ph, ph / 2.0F, 0.7F, ColorUtil.withAlpha(Theme.OUTLINE_HOVER, 1.0F - t));
      }

      float size = Math.max(5.6F, ph * 0.46F);
      String s = t >= 0.5F ? "ON" : "OFF";
      int color = t >= 0.5F
         ? ColorUtil.withAlpha(Theme.KNOB, 0.4F + 0.6F * (t - 0.5F) * 2.0F)
         : ColorUtil.withAlpha(Theme.TEXT_MUTED, 0.4F + 0.6F * (0.5F - t) * 2.0F);
      Ui.textMiddle(s, px + (pw - Ui.widthBold(s, size)) / 2.0F, cy, size, color, true);
   }

   public static void drawSwitch(float sx, float sy, float sw, float sh, float t, int accent) {
      Ui.registerControl(sx, sy, sw, sh, "switch");
      int track = ColorUtil.lerp(Theme.TRACK, accent | 0xFF000000, t);
      if (t > 0.01F) {
         Ui.glow(sx + sw / 2.0F, sy + sh / 2.0F, sw * 1.6F, ColorUtil.withAlpha(accent, 0.22F * t));
      }

      Ui.roundRect(sx, sy, sw, sh, sh / 2.0F, track);
      float r = sh / 2.0F - 2.0F;
      float kx = sx + 2.0F + r + (sw - 4.0F - 2.0F * r) * t;
      Ui.shadow(kx - r, sy + 2.0F, r * 2.0F, r * 2.0F, r, 1.5F, 1426063360);
      Ui.circle(kx, sy + sh / 2.0F, r, Theme.KNOB);
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.setting.getPreview() != null && this.preview.contains(mx, my)) {
         this.setting.getPreview().run();
         return true;
      } else if (button == 0 && this.contains(mx, my)) {
         this.setting.toggle();
         ModSounds.toggle(this.setting.get());
         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (this.setting.getPreview() != null && this.preview.contains(mx, my)) {
         return PreviewButton.tooltip();
      } else if (!this.contains(mx, my)) {
         return null;
      } else {
         return !this.shownLabel.equals(this.setting.getLabel()) && !this.shownLabel.isEmpty()
            ? this.setting.getLabel() + ": " + this.setting.getDescription()
            : this.setting.getDescription();
      }
   }
}
