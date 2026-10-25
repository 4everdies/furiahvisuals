package meowtils.furiahvisuals.gui.popup;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;

public abstract class Popover {
   public float x;
   public float y;
   public float w;
   public float h;
   protected float appear;
   float ax;
   float ay;
   float aw;
   float ah;

   protected abstract float preferredW();

   protected abstract float preferredH();

   protected abstract void renderContent(float var1, float var2, float var3, int var4);

   public void render(float mx, float my, float dt, int accent) {
      this.appear = Easing.approach(this.appear, 1.0F, 16.0F, dt);
      float a = Easing.outCubic(this.appear);
      Ui.pushAlpha(a);
      float lift = (1.0F - a) * 4.0F;
      float py = this.y - lift;
      Ui.shadow(this.x, py + 3.0F, this.w, this.h, 8.0F, 14.0F, Theme.LIGHT ? 1073741824 : -1728053248);
      Ui.roundRect(this.x, py, this.w, this.h, 8.0F, opaque(Theme.TOOLTIP));
      Ui.outline(this.x, py, this.w, this.h, 8.0F, 0.8F, Theme.TOOLTIP_BORDER);
      float keepY = this.y;
      this.y = py;
      this.renderContent(mx, my, dt, accent);
      this.y = keepY;
      Ui.popAlpha();
   }

   static int opaque(int color) {
      return ColorUtil.withAlpha(color, Math.max(0.94F, ColorUtil.alpha(color) / 255.0F));
   }

   public boolean contains(float mx, float my) {
      return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + this.h;
   }

   public boolean mouseDown(float mx, float my, int button) {
      return this.contains(mx, my);
   }

   public void mouseDrag(float mx, float my) {
   }

   public void mouseUp(float mx, float my) {
   }

   public boolean keyTyped(char c, int key) {
      return false;
   }

   public boolean wantsKeys() {
      return false;
   }

   public boolean wheel(float mx, float my, int wheel) {
      return this.contains(mx, my);
   }

   public boolean isDragging() {
      return false;
   }

   public String tooltip(float mx, float my) {
      return null;
   }

   protected void closed() {
   }

   void place(float viewW, float viewH) {
      this.w = this.preferredW();
      this.h = this.preferredH();
      float below = this.ay + this.ah + 4.0F;
      float above = this.ay - this.h - 4.0F;
      this.y = !(below + this.h <= viewH - 8.0F) && !(above < 8.0F) ? above : below;
      this.y = Math.max(8.0F, Math.min(viewH - this.h - 8.0F, this.y));
      this.x = Math.max(8.0F, Math.min(viewW - this.w - 8.0F, this.aw <= 0.0F ? this.ax : this.ax + this.aw - this.w));
   }

   void keepInside(float viewW, float viewH) {
      float nh = this.preferredH();
      if (nh != this.h) {
         this.h = nh;
         if (this.y + this.h > viewH - 8.0F) {
            this.y = Math.max(8.0F, viewH - this.h - 8.0F);
         }
      }
   }
}
