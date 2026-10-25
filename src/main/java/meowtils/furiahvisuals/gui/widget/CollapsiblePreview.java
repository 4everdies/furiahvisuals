package meowtils.furiahvisuals.gui.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.UiProbe;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

public final class CollapsiblePreview extends Widget {
   private static final float STRIP_H = 34.0F;
   private static final float CHIP_ROW = 17.0F;
   private static final float CHIP_H = 13.0F;
   private static final float CHIP_TEXT = 7.0F;
   private final Widget inner;
   private final Supplier<Boolean> openState;
   private final Consumer<Boolean> setOpen;
   private float open = -1.0F;
   private float chipHover;

   public CollapsiblePreview(Widget inner, Supplier<Boolean> openState, Consumer<Boolean> setOpen) {
      this.inner = inner;
      this.openState = openState;
      this.setOpen = setOpen;
   }

   public Widget inner() {
      return this.inner;
   }

   public void setOpenForCheck(boolean open) {
      if (this.folding()) {
         this.setOpen.accept(open);
         this.open = open ? 1.0F : 0.0F;
      }
   }

   private boolean folding() {
      return Density.compact();
   }

   private boolean isOpen() {
      return !this.folding() || this.openState.get();
   }

   private float openness() {
      float target = this.isOpen() ? 1.0F : 0.0F;
      return this.open < 0.0F ? target : this.open;
   }

   private float chipRow() {
      return this.folding() && !(this.inner instanceof ChipSlot) ? 17.0F * Easing.outCubic(this.openness()) : 0.0F;
   }

   @Override
   public float height() {
      float full = this.inner.height();
      if (!this.folding()) {
         return full;
      } else {
         float t = Easing.outCubic(this.openness());
         float strip = Math.min(34.0F, full);
         return strip + (full + 17.0F * (this.inner instanceof ChipSlot ? 0.0F : 1.0F) - strip) * t;
      }
   }

   @Override
   public boolean isVisible() {
      return this.inner.isVisible();
   }

   private static String chipLabel(boolean open) {
      return open ? "Hide" : "Show preview";
   }

   private static float chipW(boolean open) {
      return Ui.width(chipLabel(open), 7.0F) + 18.0F;
   }

   private float[] chip() {
      boolean o = this.isOpen();
      float cw = chipW(o);
      float t = Easing.outCubic(this.openness());
      float fx = this.x + this.w - cw - 5.0F;
      float fy = this.y + 5.0F;
      float ox = this.x + this.w - cw;
      float oy;
      if (this.inner instanceof ChipSlot) {
         float[] row = ((ChipSlot)this.inner).topRow();
         oy = row[0] + (row[1] - 13.0F) / 2.0F;
      } else {
         oy = this.y + 2.0F;
      }

      return new float[]{fx + (ox - fx) * t, fy + (oy - fy) * t, cw, 13.0F};
   }

   private float innerTop() {
      return this.y + this.chipRow();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float target = this.isOpen() ? 1.0F : 0.0F;
      float f = this.open = this.open < 0.0F ? target : Easing.approach(this.open, target, 12.0F, dt);
      if (this.inner instanceof ChipSlot) {
         ((ChipSlot)this.inner).reserveRight(this.folding() ? chipW(false) : 0.0F);
      }

      float h = this.height();
      float full = this.inner.height();
      float top = this.innerTop();
      float shown = this.y + h - top;
      this.inner.appear = this.appear;
      this.inner.x = this.x;
      this.inner.w = this.w;
      this.inner.y = top - (full - shown) / 2.0F;
      boolean strip = this.folding() && this.open < 0.999F;
      Ui.pushScissor(this.x - 10.0F, top, this.w + 20.0F, shown);
      boolean folded = strip && this.open < 0.5F;
      boolean innerMouse = !folded && my >= top;
      if (folded) {
         UiProbe.pushInert();
      }

      this.inner.render(innerMouse ? mx : -10000.0F, innerMouse ? my : -10000.0F, dt, accent);
      if (folded) {
         UiProbe.popInert();
      }

      Ui.popScissor();
      if (this.folding()) {
         Ui.pushScissor(this.x - 10.0F, this.y, this.w + 20.0F, h);
         float[] c = this.chip();
         boolean over = mx >= c[0] && mx < c[0] + c[2] && my >= c[1] && my < c[1] + c[3];
         boolean overStrip = !this.isOpen() && mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + h;
         this.chipHover = Easing.approach(this.chipHover, !over && !overStrip ? 0.0F : 1.0F, 16.0F, dt);
         Ui.roundRect(c[0], c[1], c[2], c[3], c[3] / 2.0F, ColorUtil.withAlpha(-16777216, 0.55F + 0.2F * this.chipHover));
         arrow(c[0] + 8.0F, c[1] + c[3] / 2.0F, this.isOpen(), ColorUtil.lerp(-855638017, accent | 0xFF000000, this.chipHover));
         Ui.textMiddle(chipLabel(this.isOpen()), c[0] + 14.0F, c[1] + c[3] / 2.0F, 7.0F, -1, false);
         Ui.registerControl(c[0], c[1], c[2], c[3], "preview chip");
         Ui.popScissor();
      }
   }

   private static void arrow(float cx, float cy, boolean up, int color) {
      float d = up ? -1.0F : 1.0F;

      for (int k = 0; k <= 4; k++) {
         float f = k / 4.0F;
         Ui.circle(cx - 2.6F + f * 2.6F, cy - d * 1.3F + d * f * 2.6F, 0.55F, color);
         Ui.circle(cx + 2.6F - f * 2.6F, cy - d * 1.3F + d * f * 2.6F, 0.55F, color);
      }
   }

   private boolean onChip(float mx, float my) {
      float[] c = this.chip();
      return mx >= c[0] && mx < c[0] + c[2] && my >= c[1] && my < c[1] + c[3];
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         return false;
      } else if (!this.folding() || button != 0 || this.isOpen() && !this.onChip(mx, my)) {
         return !this.folding() || !this.onChip(mx, my) && !(my < this.innerTop()) ? this.inner.mouseDown(mx, my, button) : true;
      } else {
         boolean now = !this.isOpen();
         this.setOpen.accept(now);
         ModSounds.gui(now ? Sfx.DROPDOWN : Sfx.CLICK);
         return true;
      }
   }

   @Override
   public void mouseDrag(float mx, float my) {
      this.inner.mouseDrag(mx, my);
   }

   @Override
   public void mouseUp(float mx, float my) {
      this.inner.mouseUp(mx, my);
   }

   @Override
   public boolean keyTyped(char c, int key) {
      return this.inner.keyTyped(c, key);
   }

   @Override
   public boolean mouseWheel(float mx, float my, int wheel) {
      return this.isOpen() && this.contains(mx, my) && my >= this.innerTop() && this.inner.mouseWheel(mx, my, wheel);
   }

   @Override
   public boolean isFocused() {
      return this.inner.isFocused();
   }

   @Override
   public boolean isDragging() {
      return this.inner.isDragging();
   }

   @Override
   public void unfocus() {
      this.inner.unfocus();
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else {
         if (this.folding()) {
            if (this.onChip(mx, my)) {
               return this.isOpen() ? "Fold the preview into a strip" : "Show the whole preview";
            }

            if (!this.isOpen()) {
               return "Click to show the whole preview";
            }

            if (my < this.innerTop()) {
               return null;
            }
         }

         return this.inner.tooltip(mx, my);
      }
   }
}
