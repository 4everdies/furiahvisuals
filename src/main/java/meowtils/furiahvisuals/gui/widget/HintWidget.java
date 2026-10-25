package meowtils.furiahvisuals.gui.widget;

import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;

public final class HintWidget extends Widget {
   private final String text;
   private final BooleanSupplier due;
   private final Runnable dismissed;
   private boolean closed;
   private float closeHover;

   public HintWidget(String text, BooleanSupplier due, Runnable dismissed) {
      this.text = text;
      this.due = due;
      this.dismissed = dismissed;
   }

   @Override
   public float height() {
      return (Density.compact() ? Density.row() + 4.0F : 28.0F) + 4.0F;
   }

   @Override
   public boolean isVisible() {
      return !this.closed && this.due.getAsBoolean();
   }

   private boolean onClose(float mx, float my) {
      float h = this.height() - 4.0F;
      float cx = this.x + this.w - h / 2.0F;
      float cy = this.y + h / 2.0F;
      return Math.abs(mx - cx) <= h / 2.0F && Math.abs(my - cy) <= h / 2.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float h = this.height() - 4.0F;
      float cy = this.y + h / 2.0F;
      this.closeHover = Easing.approach(this.closeHover, this.onClose(mx, my) ? 1.0F : 0.0F, 16.0F, dt);
      Ui.roundRect(this.x - 6.0F, this.y, this.w + 12.0F, h, h / 2.0F, Theme.ACCENT_FAINT);
      Ui.outline(this.x - 6.0F, this.y, this.w + 12.0F, h, h / 2.0F, 0.8F, Theme.ACCENT_LINE);
      float sx = this.x + 4.0F;
      Ui.line(sx, cy - 3.6F, sx, cy + 3.6F, 1.0F, true, accent);
      Ui.line(sx - 3.6F, cy, sx + 3.6F, cy, 1.0F, true, accent);
      Ui.circle(sx, cy, 1.1F, accent);
      float ts = Density.compact() ? Density.text() : 9.4F;
      Ui.textMiddle(Ui.ellipsize(this.text, ts, this.w - 34.0F, false), this.x + 14.0F, cy, ts, Theme.TEXT, false);
      float bx = this.x + this.w - h / 2.0F;
      int xc = ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT, this.closeHover);
      if (this.closeHover > 0.01F) {
         Ui.circle(bx, cy, h * 0.36F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, this.closeHover));
      }

      Ui.line(bx - 2.6F, cy - 2.6F, bx + 2.6F, cy + 2.6F, 1.0F, true, xc);
      Ui.line(bx - 2.6F, cy + 2.6F, bx + 2.6F, cy - 2.6F, 1.0F, true, xc);
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.onClose(mx, my)) {
         this.closed = true;
         this.dismissed.run();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      return this.onClose(mx, my) ? "Close this hint" : null;
   }
}
