package meowtils.furiahvisuals.module.weapons;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class WeaponChoiceWidget extends Widget {
   private final WeaponsModule module;
   private final WeaponChoiceSetting setting;
   private final WeaponDef owner;
   private final ChipRow chips = new ChipRow();
   private final List<String> ids = new ArrayList<>();
   private String[] labels = new String[0];
   private boolean[] selected = new boolean[0];
   private int version = -1;

   WeaponChoiceWidget(WeaponsModule module, WeaponChoiceSetting setting, WeaponDef owner) {
      this.module = module;
      this.setting = setting;
      this.owner = owner;
   }

   private void refresh() {
      if (this.version != this.module.library.version()) {
         this.version = this.module.library.version();
         this.ids.clear();
         ArrayList<String> names = new ArrayList<>();
         this.ids.add("same");
         names.add("Same, Mirrored");

         for (WeaponDef d : this.module.library.all()) {
            if (d != this.owner && !d.twoHanded) {
               this.ids.add(d.id);
               names.add(d.name);
            }
         }

         this.labels = names.toArray(new String[0]);
         this.selected = new boolean[this.labels.length];
      }
   }

   @Override
   public float height() {
      this.refresh();
      this.chips.layout(this.x, this.y + 22.0F, this.w, this.labels, null);
      return 22.0F + this.chips.height + 6.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.refresh();
      this.chips.layout(this.x, this.y + 22.0F, this.w, this.labels, null);
      boolean usable = !this.owner.twoHanded;
      Ui.pushAlpha(usable ? 1.0F : 0.45F);
      Ui.text(this.setting.getName(), this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
      String current = this.setting.get();

      for (int i = 0; i < this.ids.size(); i++) {
         this.selected[i] = this.ids.get(i).equals(current);
      }

      this.chips.draw(this.labels, this.selected, null, null, usable ? mx : -1.0F, usable ? my : -1.0F, dt, accent);
      Ui.popAlpha();
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my) || button != 0) {
         return false;
      } else if (this.owner.twoHanded) {
         ModSounds.gui(Sfx.ERROR);
         return true;
      } else {
         int i = this.chips.at(mx, my);
         if (i >= 0) {
            this.setting.set(this.ids.get(i));
            ModSounds.gui(Sfx.SELECT);
         }

         return true;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else if (this.owner.isBow()) {
         return "Not for bows: a bow takes both hands, one on the bow and one on the string";
      } else {
         return this.owner.twoHanded ? this.owner.name + " is two-handed: it stays in both hands when Dual Wield is on" : this.setting.getDescription();
      }
   }
}
