package meowtils.furiahvisuals.module.misc;

import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.FuriahScreen;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.Minecraft;

final class SaturationPreviewWidget extends Widget {
   private final MiscModule misc;
   private float split = 0.5F;
   private boolean dragging;
   private boolean peeking;
   private float peekHover;
   private float ix;
   private float iy;
   private float iw;
   private float ih;
   private float px;
   private float py;
   private float pw;
   private float ph;

   SaturationPreviewWidget(MiscModule misc) {
      this.misc = misc;
   }

   private float imageH() {
      float max = Density.dense() ? 96.0F : (Density.compact() ? 118.0F : 148.0F);
      return Math.min(max, this.w * 9.0F / 16.0F);
   }

   @Override
   public float height() {
      return this.imageH() + 8.0F;
   }

   private float aspect(boolean world) {
      return world ? (float)this.misc.saturation.beforeWidth() / Math.max(1, this.misc.saturation.beforeHeight()) : 1.7777778F;
   }

   private void layout(boolean world) {
      this.ih = this.imageH();
      this.iw = Math.min(this.w, this.ih * this.aspect(world));
      this.ix = this.x + (this.w - this.iw) / 2.0F;
      this.iy = this.y + 4.0F;
      float ts = Density.small();
      this.ph = Density.compact() ? 15.0F : 17.0F;
      this.pw = Ui.width("Hold to peek", ts) + 16.0F;
      this.px = this.ix + this.iw - this.pw - 6.0F;
      this.py = this.iy + this.ih - this.ph - 6.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      Minecraft mc = Minecraft.getMinecraft();
      boolean world = mc.theWorld != null;
      this.layout(world);
      this.misc.saturation.previewShown();
      int before = world ? this.misc.saturation.beforeTexture() : SampleImage.texture();
      int bw = world ? this.misc.saturation.beforeWidth() : 480;
      int bh = world ? this.misc.saturation.beforeHeight() : 270;
      float radius = Math.min(8.0F, this.ih * 0.06F);
      if (before <= 0) {
         Ui.roundRect(this.ix, this.iy, this.iw, this.ih, radius, Theme.PREVIEW_STAGE);
         String t = "Taking a picture of your view…";
         Ui.textMiddle(t, this.ix + (this.iw - Ui.width(t, Density.small())) / 2.0F, this.iy + this.ih / 2.0F, Density.small(), -6050627, false);
      } else {
         SliderSetting[] set = this.misc.shownSet();
         int after = this.misc.saturation.renderAfter(before, bw, bh, set[0].getFloat() / 100.0F, set[1].getFloat() / 100.0F, set[2].getFloat() / 100.0F);
         float v0 = world ? 1.0F : 0.0F;
         float v1 = world ? 0.0F : 1.0F;
         Ui.roundedImage(before, this.ix, this.iy, this.iw, this.ih, radius, 0.0F, v0, 1.0F, v1, -1);
         float sx = this.ix + this.iw * this.split;
         if (after > 0) {
            Ui.pushScissor(sx, this.iy, this.ix + this.iw - sx, this.ih);
            Ui.roundedImage(after, this.ix, this.iy, this.iw, this.ih, radius, 0.0F, v0, 1.0F, v1, -1);
            Ui.popScissor();
         }

         Ui.rect(sx - 0.75F, this.iy, 1.5F, this.ih, -920584);
         float hr = Density.compact() ? 6.0F : 7.0F;
         Ui.circle(sx, this.iy + this.ih / 2.0F, hr + 1.0F, ColorUtil.withAlpha(Theme.PREVIEW_STAGE, 0.6F));
         Ui.circle(sx, this.iy + this.ih / 2.0F, hr, this.dragging ? accent : -920584);
         Ui.registerControl(sx - hr, this.iy + this.ih / 2.0F - hr, hr * 2.0F, hr * 2.0F, "split");
         label("Before", this.ix + 6.0F, this.iy + 6.0F);
         label("After", this.ix + this.iw - 6.0F - labelW("After"), this.iy + 6.0F);
         boolean overPeek = mx >= this.px && mx < this.px + this.pw && my >= this.py && my < this.py + this.ph;
         this.peekHover = Easing.approach(this.peekHover, !overPeek && !this.peeking ? 0.0F : 1.0F, 16.0F, dt);
         Ui.roundRect(
            this.px, this.py, this.pw, this.ph, this.ph / 2.0F, this.peeking ? accent : ColorUtil.withAlpha(Theme.PREVIEW_STAGE, 0.75F + 0.2F * this.peekHover)
         );
         String peek = "Hold to peek";
         Ui.textMiddle(peek, this.px + (this.pw - Ui.width(peek, Density.small())) / 2.0F, this.py + this.ph / 2.0F, Density.small(), -920584, false);
         Ui.registerControl(this.px, this.py, this.pw, this.ph, peek);
      }
   }

   private static float labelW(String s) {
      return Ui.width(s, Density.small()) + 12.0F;
   }

   private static void label(String s, float lx, float ly) {
      float h = Density.compact() ? 14.0F : 16.0F;
      Ui.roundRect(lx, ly, labelW(s), h, h / 2.0F, ColorUtil.withAlpha(Theme.PREVIEW_STAGE, 0.7F));
      Ui.textMiddle(s, lx + 6.0F, ly + h / 2.0F, Density.small(), -920584, false);
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button != 0) {
         return false;
      } else {
         this.layout(Minecraft.getMinecraft().theWorld != null);
         if (mx >= this.px && mx < this.px + this.pw && my >= this.py && my < this.py + this.ph) {
            this.peeking = true;
            FuriahScreen.peek(true);
            return true;
         } else if (mx >= this.ix && mx < this.ix + this.iw && my >= this.iy && my < this.iy + this.ih) {
            this.dragging = true;
            this.mouseDrag(mx, my);
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public void mouseDrag(float mx, float my) {
      if (this.dragging && this.iw > 0.0F) {
         this.split = Math.max(0.0F, Math.min(1.0F, (mx - this.ix) / this.iw));
      }
   }

   @Override
   public void mouseUp(float mx, float my) {
      this.dragging = false;
      if (this.peeking) {
         this.peeking = false;
         FuriahScreen.peek(false);
      }
   }

   @Override
   public boolean isDragging() {
      return this.dragging || this.peeking;
   }

   @Override
   public String tooltip(float mx, float my) {
      if (mx >= this.px && mx < this.px + this.pw && my >= this.py && my < this.py + this.ph) {
         return "Hold to fade the menu out and see the whole screen";
      } else if (mx >= this.ix && mx < this.ix + this.iw && my >= this.iy && my < this.iy + this.ih) {
         return Minecraft.getMinecraft().theWorld != null
            ? "Your view now: as it is on the left of the line, with these values on its right (drag the line)"
            : "A sample picture (no world is open): as it is on the left of the line, with these values on its right";
      } else {
         return null;
      }
   }

   float splitForTest(float to) {
      if (to >= 0.0F) {
         this.split = to;
      }

      return this.split;
   }
}
