package meowtils.furiahvisuals.module.misc.skins;

import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class SkinBackWidget extends Widget {
   private static final String LABEL = "Back to my account skin";
   private final Skins skins;
   private final Pill pill = new Pill();

   SkinBackWidget(Skins skins) {
      this.skins = skins;
   }

   @Override
   public float height() {
      return Density.compact() ? Density.row() + 2.0F : 28.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float py = this.y + (this.height() - 20.0F) / 2.0F;
      float pw = this.pill.layout(this.x, py, "Back to my account skin");
      this.pill.draw("Back to my account skin", mx, my, dt, accent, false, true);
      String note = "Everyone sees your account's skin again; this one stays in the library";
      float nx = this.x + pw + 10.0F;
      if (this.w - pw - 10.0F > 60.0F) {
         Ui.textMiddle(
            Ui.ellipsize(note, Density.small(), this.w - pw - 10.0F, false), nx, this.y + this.height() / 2.0F, Density.small(), Theme.TEXT_MUTED, false
         );
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.pill.contains(mx, my)) {
         this.skins.wearAccount();
         ModSounds.gui(Sfx.CLICK);
         return true;
      } else {
         return this.contains(mx, my);
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      return this.pill.contains(mx, my) ? "Turns Use a Custom Skin off: your account's skin shows again" : null;
   }
}
