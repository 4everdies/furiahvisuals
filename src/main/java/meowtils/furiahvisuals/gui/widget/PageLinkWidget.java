package meowtils.furiahvisuals.gui.widget;

import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.layout.LayoutPage;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.Setting;

public final class PageLinkWidget extends Widget {
   private static final float H = 30.0F;
   private final LayoutPage page;
   private final Runnable open;

   public PageLinkWidget(LayoutPage page, Runnable open) {
      this.page = page;
      this.open = open;
   }

   public LayoutPage page() {
      return this.page;
   }

   @Override
   public float height() {
      return Density.compact() ? Density.row() + 3.0F : 30.0F;
   }

   @Override
   public boolean isVisible() {
      for (Setting s : this.page.settings()) {
         if (s.isVisible()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      boolean over = this.contains(mx, my);
      this.animateHover(over, dt);
      float h = this.height();
      float cy = this.y + h / 2.0F;
      float ts = Density.text();
      Ui.roundRect(
         this.x - 6.0F, this.y + 2.0F, this.w + 12.0F, h - 4.0F, 8.0F, ColorUtil.lerp(ColorUtil.mulAlpha(Theme.SURFACE, 0.6F), Theme.SURFACE_HOVER, this.hover)
      );
      Ui.textMiddle(this.page.title, this.x + 4.0F, cy, ts, Theme.TEXT, false);
      int n = 0;

      for (Setting s : this.page.settings()) {
         if (s.isVisible()) {
            n++;
         }
      }

      String count = n + (n == 1 ? " setting" : " settings");
      float tw = Ui.width(this.page.title, ts);
      Ui.textMiddle(count, this.x + 4.0F + tw + 10.0F, cy, Density.small(), Theme.TEXT_MUTED, false);
      float ax = this.x + this.w - 8.0F + Easing.outCubic(this.hover) * 2.0F;
      chevron(ax, cy, ColorUtil.lerp(Theme.TEXT_MUTED, accent, 0.35F + 0.65F * this.hover));
   }

   static void chevron(float cx, float cy, int color) {
      for (int k = 0; k <= 4; k++) {
         float f = k / 4.0F;
         Ui.circle(cx - 2.6F + f * 2.6F, cy - 3.4F + f * 3.4F, 0.75F, color);
         Ui.circle(cx - 2.6F + f * 2.6F, cy + 3.4F - f * 3.4F, 0.75F, color);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         this.open.run();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      return this.contains(mx, my) ? (this.page.description != null ? this.page.description : "Open " + this.page.title) : null;
   }
}
