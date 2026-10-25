package meowtils.furiahvisuals.module.watermark;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;

final class PositionPickerWidget extends Widget {
   private static final float SCREEN_W = 104.0F;
   private static final float SCREEN_H = 58.0F;
   private final WaterMarkModule wm;
   private final float[] dotHover = new float[WaterMarkModule.Anchor.values().length];
   private final float[] dotSelect = new float[WaterMarkModule.Anchor.values().length];
   private int hoverDot = -1;

   PositionPickerWidget(WaterMarkModule wm) {
      this.wm = wm;
   }

   @Override
   public float height() {
      return 70.0F;
   }

   private float screenX() {
      return this.x + this.w - 104.0F - 2.0F;
   }

   private float screenY() {
      return this.y + 6.0F;
   }

   private static float fx(WaterMarkModule.Anchor a) {
      return a.isLeft() ? 0.17F : (a.isRight() ? 0.83F : 0.5F);
   }

   private static float fy(WaterMarkModule.Anchor a) {
      return a.isBottom() ? 0.8F : 0.2F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      WaterMarkModule.Anchor current = this.wm.anchor.get();
      Ui.textMiddle("Position", this.x, this.y + 18.0F, 10.0F, Theme.TEXT, false);
      Ui.textMiddleHeavy(current.displayName(), this.x, this.y + 33.0F, 9.0F, accent | 0xFF000000);
      Ui.text("It grows away from its corner.", this.x, this.y + 44.0F, 8.0F, Theme.TEXT_MUTED);
      float sx = this.screenX();
      float sy = this.screenY();
      Ui.surface(sx, sy, 104.0F, 58.0F, 7.0F, Theme.SURFACE);
      Ui.outline(sx, sy, 104.0F, 58.0F, 7.0F, 0.9F, Theme.OUTLINE_HOVER);
      Ui.roundRect(sx + 52.0F - 18.0F, sy + 58.0F - 7.0F, 36.0F, 3.5F, 1.75F, ColorUtil.withAlpha(Theme.TEXT_MUTED, 0.5F));
      this.hoverDot = -1;
      WaterMarkModule.Anchor[] anchors = WaterMarkModule.Anchor.values();

      for (int i = 0; i < anchors.length; i++) {
         WaterMarkModule.Anchor a = anchors[i];
         float dx = sx + 104.0F * fx(a);
         float dy = sy + 58.0F * fy(a);
         boolean over = Math.abs(mx - dx) < 10.0F && Math.abs(my - dy) < 9.0F;
         if (over) {
            this.hoverDot = i;
         }

         this.dotHover[i] = Easing.approach(this.dotHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
         this.dotSelect[i] = Easing.approach(this.dotSelect[i], a == current ? 1.0F : 0.0F, 14.0F, dt);
         float pillW = 20.0F + 6.0F * this.dotSelect[i];
         float pillH = 6.0F + 1.5F * this.dotSelect[i];
         if (this.dotSelect[i] > 0.02F) {
            Ui.glow(dx, dy, 34.0F, ColorUtil.withAlpha(accent, 0.45F * this.dotSelect[i]));
         }

         float px = a.isLeft() ? dx - 7.0F : (a.isRight() ? dx + 7.0F - pillW : dx - pillW / 2.0F);
         int col = ColorUtil.lerp(ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT_DIM, this.dotHover[i]), accent, this.dotSelect[i]);
         Ui.roundRect(px, dy - pillH / 2.0F, pillW, pillH, pillH / 2.0F, col);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         WaterMarkModule.Anchor[] anchors = WaterMarkModule.Anchor.values();
         float sx = this.screenX();
         float sy = this.screenY();
         int best = -1;
         float bestD = Float.MAX_VALUE;

         for (int i = 0; i < anchors.length; i++) {
            float dx = sx + 104.0F * fx(anchors[i]) - mx;
            float dy;
            float d = dx * dx + (dy = sy + 58.0F * fy(anchors[i]) - my) * dy;
            if (d < bestD) {
               bestD = d;
               best = i;
            }
         }

         if (best >= 0 && mx >= sx - 6.0F && mx <= sx + 104.0F + 6.0F && my >= sy - 6.0F && my <= sy + 58.0F + 6.0F) {
            this.wm.anchor.set(anchors[best]);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (this.hoverDot >= 0) {
         return WaterMarkModule.Anchor.values()[this.hoverDot].displayName();
      } else {
         return this.contains(mx, my) ? "Where the island sits: click a place on the little screen" : null;
      }
   }
}
