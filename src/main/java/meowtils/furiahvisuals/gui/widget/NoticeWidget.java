package meowtils.furiahvisuals.gui.widget;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.render.ColorUtil;

public final class NoticeWidget extends Widget {
   private static final float SIZE = 8.6F;
   private static final float LINE = 11.0F;
   private final Module module;
   private final List<String> lines = new ArrayList<>();
   private String wrappedText;
   private float wrappedWidth;

   public NoticeWidget(Module module) {
      this.module = module;
   }

   @Override
   public boolean isVisible() {
      return this.module.getNotice() != null;
   }

   @Override
   public float height() {
      Module.Notice n = this.module.getNotice();
      if (n == null) {
         return this.lines.isEmpty() ? 0.0F : 18.0F + this.lines.size() * 11.0F;
      } else {
         this.wrap(n.text);
         return 18.0F + this.lines.size() * 11.0F;
      }
   }

   private void wrap(String text) {
      float width = this.w - 40.0F;
      if (!text.equals(this.wrappedText) || !(Math.abs(width - this.wrappedWidth) < 0.5F)) {
         this.wrappedText = text;
         this.wrappedWidth = width;
         this.lines.clear();
         StringBuilder line = new StringBuilder();

         for (String word : text.split(" ")) {
            String tryLine = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && Ui.width(tryLine, 8.6F) > width) {
               this.lines.add(line.toString());
               line.setLength(0);
               line.append(word);
            } else {
               line.setLength(0);
               line.append(tryLine);
            }
         }

         if (line.length() > 0) {
            this.lines.add(line.toString());
         }
      }
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      Module.Notice n = this.module.getNotice();
      if (n != null) {
         this.wrap(n.text);
         float h = this.height() - 8.0F;
         int tone = n.blocking ? Theme.DANGER : accent;
         Ui.roundRect(this.x, this.y + 2.0F, this.w, h, 8.0F, ColorUtil.withAlpha(tone, 0.12F));
         Ui.outline(this.x, this.y + 2.0F, this.w, h, 8.0F, 0.8F, ColorUtil.withAlpha(tone, 0.45F));
         float bx = this.x + 14.0F;
         float by = this.y + 2.0F + 7.0F + 5.5F;
         Ui.circle(bx, by, 6.5F, ColorUtil.withAlpha(tone, 0.9F));
         String mark = n.blocking ? "!" : "i";
         Ui.textMiddle(mark, bx - Ui.widthBold(mark, 8.4F) / 2.0F, by, 8.4F, -1, true);
         float ty = this.y + 2.0F + 7.0F;

         for (String line : this.lines) {
            Ui.text(line, this.x + 28.0F, ty, 8.6F, Theme.TEXT);
            ty += 11.0F;
         }
      }
   }
}
