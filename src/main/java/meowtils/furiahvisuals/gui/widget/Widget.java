package meowtils.furiahvisuals.gui.widget;

import meowtils.furiahvisuals.render.Easing;

public abstract class Widget {
   public float x;
   public float y;
   public float w;
   public float appear = -1.0F;
   protected float hover;

   public abstract float height();

   public boolean isVisible() {
      return true;
   }

   public abstract void render(float var1, float var2, float var3, int var4);

   public boolean mouseDown(float mx, float my, int button) {
      return false;
   }

   public void mouseDrag(float mx, float my) {
   }

   public void mouseUp(float mx, float my) {
   }

   public boolean keyTyped(char c, int key) {
      return false;
   }

   public boolean mouseWheel(float mx, float my, int wheel) {
      return false;
   }

   public boolean isFocused() {
      return false;
   }

   public boolean isDragging() {
      return false;
   }

   public void unfocus() {
   }

   public String tooltip(float mx, float my) {
      return null;
   }

   public boolean contains(float mx, float my) {
      return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + this.height();
   }

   protected void animateHover(boolean over, float dt) {
      this.hover = Easing.approach(this.hover, over ? 1.0F : 0.0F, 16.0F, dt);
   }
}
