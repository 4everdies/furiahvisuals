package meowtils.furiahvisuals.gui.widget;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.setting.Setting;

public final class NoteWidget extends Widget {
   private final Setting setting;
   private final List<String> lines = new ArrayList<>();
   private float wrappedFor = -1.0F;
   private float wrappedSize = -1.0F;

   private static float size() {
      return Density.compact() ? 7.4F : 8.0F;
   }

   private static float line() {
      return Density.compact() ? 10.0F : 11.0F;
   }

   public NoteWidget(Setting setting) {
      this.setting = setting;
   }

   private void wrap() {
      float size = size();
      if ((this.w != this.wrappedFor || size != this.wrappedSize) && !(this.w <= 0.0F)) {
         this.wrappedFor = this.w;
         this.wrappedSize = size;
         this.lines.clear();
         StringBuilder line = new StringBuilder();

         for (String word : this.setting.getDescription().split(" ")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (Ui.width(next, size) > this.w && line.length() > 0) {
               this.lines.add(line.toString());
               line.setLength(0);
               line.append(word);
            } else {
               line.setLength(0);
               line.append(next);
            }
         }

         if (line.length() > 0) {
            this.lines.add(line.toString());
         }
      }
   }

   @Override
   public float height() {
      this.wrap();
      return Math.max(1, this.lines.size()) * line() + (Density.compact() ? 3.0F : 6.0F);
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.wrap();

      for (int i = 0; i < this.lines.size(); i++) {
         Ui.text(this.lines.get(i), this.x, this.y + 2.0F + i * line(), size(), Theme.TEXT_MUTED);
      }
   }
}
