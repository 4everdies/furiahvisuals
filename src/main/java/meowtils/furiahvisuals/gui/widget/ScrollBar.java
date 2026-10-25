package meowtils.furiahvisuals.gui.widget;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;

public final class ScrollBar {
   public static final float MIN_THUMB = 24.0F;
   public static final float WHEEL_STEP = 42.0F;
   private static final float THIN = 2.5F;
   private static final float WIDE = 5.0F;
   private static final float REACH = 6.0F;
   public float scroll;
   public float target;
   private float content;
   private float view;
   private float x;
   private float y;
   private float h;
   private boolean dragging;
   private float grab;
   private float hover;

   public void update(float contentHeight, float viewHeight, float dt) {
      this.content = contentHeight;
      this.view = viewHeight;
      float max = this.max();
      this.target = clamp(this.target, 0.0F, max);
      this.scroll = this.dragging ? this.target : Easing.approach(this.scroll, this.target, 18.0F, dt);
      this.scroll = clamp(this.scroll, 0.0F, max);
   }

   public float max() {
      return Math.max(0.0F, this.content - this.view);
   }

   public boolean isNeeded() {
      return this.max() > 0.5F;
   }

   public boolean isDragging() {
      return this.dragging;
   }

   private float thumbHeight() {
      return this.content <= 0.0F ? this.h : Math.min(this.h, Math.max(24.0F, this.h * this.view / this.content));
   }

   private float thumbY() {
      float max = this.max();
      return this.y + (this.h - this.thumbHeight()) * (max > 0.0F ? this.scroll / max : 0.0F);
   }

   private boolean nearLine(float mx, float my) {
      return mx >= this.x - 6.0F && mx < this.x + 2.5F + 6.0F && my >= this.y && my < this.y + this.h;
   }

   private boolean onThumb(float mx, float my) {
      float ty = this.thumbY();
      return this.nearLine(mx, my) && my >= ty && my < ty + this.thumbHeight();
   }

   public void draw(float x, float y, float h, float mx, float my, float dt, int accent) {
      this.x = x;
      this.y = y;
      this.h = h;
      if (!this.isNeeded()) {
         this.hover = 0.0F;
      } else {
         boolean over = this.dragging || this.nearLine(mx, my);
         this.hover = Easing.approach(this.hover, over ? 1.0F : 0.0F, 16.0F, dt);
         float w = 2.5F + 2.5F * this.hover;
         float lx = x + 1.25F - w / 2.0F;
         Ui.registerControl(lx, y, w, h, "scrollbar");
         Ui.roundRect(lx, y, w, h, w / 2.0F, Theme.SCROLL_TRACK);
         float ty = this.thumbY();
         float th = this.thumbHeight();
         if (this.hover > 0.01F) {
            Ui.glow(x + 1.25F, ty + th / 2.0F, th + 18.0F, ColorUtil.withAlpha(accent, 0.12F * this.hover));
         }

         Ui.roundRect(lx, ty, w, th, w / 2.0F, ColorUtil.lerp(Theme.ACCENT_LINE, Theme.ACCENT, this.hover));
      }
   }

   public boolean mouseDown(float mx, float my) {
      if (this.isNeeded() && this.nearLine(mx, my)) {
         if (this.onThumb(mx, my)) {
            this.dragging = true;
            this.grab = my - this.thumbY();
         } else {
            float th = this.thumbHeight();
            float travel = this.h - th;
            float f = travel > 0.0F ? clamp((my - th / 2.0F - this.y) / travel, 0.0F, 1.0F) : 0.0F;
            this.target = f * this.max();
         }

         return true;
      } else {
         return false;
      }
   }

   public void mouseDrag(float mx, float my) {
      if (this.dragging) {
         float travel = this.h - this.thumbHeight();
         float f = travel > 0.0F ? clamp((my - this.grab - this.y) / travel, 0.0F, 1.0F) : 0.0F;
         this.target = this.scroll = f * this.max();
      }
   }

   public void mouseUp() {
      this.dragging = false;
   }

   public void wheel(int dWheel) {
      if (dWheel != 0) {
         this.target = clamp(this.target - Math.signum((float)dWheel) * 42.0F, 0.0F, this.max());
      }
   }

   public void reveal(float top, float height) {
      if (top < this.target) {
         this.target = clamp(top, 0.0F, this.max());
      } else if (top + height > this.target + this.view) {
         this.target = clamp(top + height - this.view, 0.0F, this.max());
      }
   }

   public void jumpTo(float offset) {
      this.target = this.scroll = clamp(offset, 0.0F, this.max());
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }
}
