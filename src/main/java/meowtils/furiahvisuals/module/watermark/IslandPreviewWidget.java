package meowtils.furiahvisuals.module.watermark;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;

final class IslandPreviewWidget extends Widget {
   private static final float H = 78.0F;
   private final WaterMarkModule wm;

   IslandPreviewWidget(WaterMarkModule wm) {
      this.wm = wm;
   }

   @Override
   public float height() {
      return 78.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float h = 72.0F;
      float r = 10.0F;
      Ui.gradientV(this.x, this.y, this.w, h * 0.62F + r, r, -8412195, -4402962);
      Ui.pushScissor(this.x, this.y + h * 0.62F, this.w, h * 0.38F);
      Ui.gradientV(this.x, this.y + h * 0.62F - r, this.w, h * 0.38F + r, r, -9659828, -11832524);
      Ui.popScissor();
      Ui.glow(this.x + this.w * 0.78F, this.y + h * 0.2F, 90.0F, 1090516696);
      if (!Theme.LIGHT) {
         Ui.roundRect(this.x, this.y, this.w, h, r, 855638016);
      }

      Ui.outline(this.x, this.y, this.w, h, r, 0.8F, Theme.OUTLINE);
      this.wm.island.renderPreview(this.x, this.y, this.w, h, mx, my);
      String where = this.wm.anchor.get().displayName() + "  ·  " + this.wm.scale.format();
      Ui.text(where, this.x + 9.0F, this.y + h - 13.0F, 7.4F, -855638017);
      if (!this.wm.isEnabled()) {
         String off = "The WaterMark is off";
         Ui.roundRect(
            this.x + this.w - Ui.width(off, 7.6F) - 20.0F, this.y + 6.0F, Ui.width(off, 7.6F) + 14.0F, 14.0F, 7.0F, ColorUtil.withAlpha(-16777216, 0.45F)
         );
         Ui.textMiddle(off, this.x + this.w - Ui.width(off, 7.6F) - 13.0F, this.y + 13.0F, 7.6F, -1, false);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      return button == 0 && this.contains(mx, my) && this.wm.island.previewClick(mx, my);
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else {
         String tip = this.wm.island.previewTooltip();
         return tip != null ? tip : "Live preview: hover and click it like the island with chat open";
      }
   }
}
