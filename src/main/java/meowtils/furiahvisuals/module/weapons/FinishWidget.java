package meowtils.furiahvisuals.module.weapons;

import java.util.List;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class FinishWidget extends Widget {
   private final FinishSetting setting;
   private final ChipRow chips = new ChipRow();
   private String[] labels;
   private int[] dots;
   private boolean[] withDot;
   private boolean[] selected;

   FinishWidget(FinishSetting setting) {
      this.setting = setting;
      List<WeaponOptions.Finish> options = setting.options();
      int n = options.size();
      this.labels = new String[n];
      this.dots = new int[n];
      this.withDot = new boolean[n];
      this.selected = new boolean[n];

      for (int i = 0; i < n; i++) {
         WeaponOptions.Finish f = options.get(i);
         this.labels[i] = f.displayName;
         this.dots[i] = f == WeaponOptions.Finish.CUSTOM ? 0 : f.swatch;
         this.withDot[i] = f != WeaponOptions.Finish.CUSTOM;
      }
   }

   @Override
   public float height() {
      this.chips.layout(this.x, this.y + 22.0F, this.w, this.labels, this.withDot);
      return 22.0F + this.chips.height + 6.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.chips.layout(this.x, this.y + 22.0F, this.w, this.labels, this.withDot);
      Ui.text(this.setting.getName(), this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
      List<WeaponOptions.Finish> options = this.setting.options();

      for (int i = 0; i < options.size(); i++) {
         this.selected[i] = options.get(i) == this.setting.get();
      }

      this.chips.draw(this.labels, this.selected, null, this.dots, mx, my, dt, accent);
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (this.contains(mx, my) && button == 0) {
         int i = this.chips.at(mx, my);
         if (i >= 0) {
            this.setting.set(this.setting.options().get(i));
            ModSounds.gui(Sfx.SELECT);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else {
         int i = this.chips.at(mx, my);
         return i >= 0 ? this.setting.options().get(i).description : this.setting.getDescription();
      }
   }
}
