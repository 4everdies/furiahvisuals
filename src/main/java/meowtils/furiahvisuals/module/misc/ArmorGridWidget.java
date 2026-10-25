package meowtils.furiahvisuals.module.misc;

import java.util.Locale;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.ToggleWidget;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.sound.ModSounds;

final class ArmorGridWidget extends Widget {
   private static final String[] COLUMNS = new String[]{"Me", "Others"};
   private final MiscModule module;
   private final float[][] knob = new float[4][2];
   private final float[][] glow = new float[4][2];

   ArmorGridWidget(MiscModule module) {
      this.module = module;

      for (float[] row : this.knob) {
         row[1] = -1.0F;
         row[0] = -1.0F;
      }
   }

   private static float headH() {
      return Density.dense() ? 16.0F : (Density.compact() ? 19.0F : 24.0F);
   }

   private static float rowH() {
      return Density.dense() ? 14.0F : (Density.compact() ? 16.0F : 19.0F);
   }

   private static float switchW() {
      return Density.compact() ? 20.0F : 24.0F;
   }

   private static float switchH() {
      return Density.compact() ? 11.0F : 13.0F;
   }

   private float columnX(int who) {
      float right = this.x + this.w - switchW();
      return who == 1 ? right : right - (Density.compact() ? 46.0F : 56.0F);
   }

   private float rowY(int piece) {
      return this.y + headH() + piece * rowH();
   }

   private float noteH() {
      return MiscHooks.armorHooked() ? 0.0F : 13.0F;
   }

   @Override
   public float height() {
      return headH() + 4.0F * rowH() + this.noteH() + 3.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float sw = switchW();
      float sh = switchH();
      float hy = this.y + headH() / 2.0F;
      float firstColumn = this.columnX(0) + sw / 2.0F - Ui.width(COLUMNS[0], 7.6F) / 2.0F;
      Ui.textMiddle(Ui.ellipsize("Hide Armor", 10.0F, Math.max(10.0F, firstColumn - this.x - 8.0F), false), this.x, hy, 10.0F, Theme.TEXT, false);

      for (int who = 0; who < 2; who++) {
         float cw = Ui.width(COLUMNS[who], 7.6F);
         Ui.textMiddle(COLUMNS[who], this.columnX(who) + sw / 2.0F - cw / 2.0F, hy, 7.6F, Theme.TEXT_DIM, false);
      }

      float ts = Density.dense() ? 7.6F : 8.4F;

      for (int p = 0; p < 4; p++) {
         float cy = this.rowY(p) + rowH() / 2.0F;
         boolean any = this.module.cell(p, true) || this.module.cell(p, false);
         float room = this.columnX(0) - this.x - 18.0F;
         Ui.textMiddle(Ui.ellipsize(MiscModule.PIECES[p], ts, room, false), this.x + 10.0F, cy, ts, any ? Theme.TEXT : Theme.TEXT_DIM, false);

         for (int who = 0; who < 2; who++) {
            boolean on = this.module.cell(p, who == 0);
            this.knob[p][who] = this.knob[p][who] < 0.0F ? (on ? 1.0F : 0.0F) : Easing.approach(this.knob[p][who], on ? 1.0F : 0.0F, 18.0F, dt);
            float sx = this.columnX(who);
            float sy = cy - sh / 2.0F;
            boolean over = mx >= sx - 4.0F && mx < sx + sw + 4.0F && my >= sy - 3.0F && my < sy + sh + 3.0F;
            this.glow[p][who] = Easing.approach(this.glow[p][who], over ? 1.0F : 0.0F, 16.0F, dt);
            if (this.glow[p][who] > 0.01F) {
               Ui.roundRect(sx - 3.0F, sy - 2.0F, sw + 6.0F, sh + 4.0F, (sh + 4.0F) / 2.0F, ColorUtil.withAlpha(Theme.TEXT, 0.06F * this.glow[p][who]));
            }

            ToggleWidget.drawSwitch(sx, sy, sw, sh, this.knob[p][who], accent);
         }
      }

      if (!MiscHooks.armorHooked()) {
         float ny = this.rowY(4) + 2.0F;
         Ui.text(Ui.ellipsize("Its game hooks could not be installed here: armor shows as usual.", 7.4F, this.w, false), this.x, ny, 7.4F, Theme.DANGER);
      }
   }

   private int[] cellAt(float mx, float my) {
      float sw = switchW();
      float sh = switchH();

      for (int p = 0; p < 4; p++) {
         float cy = this.rowY(p) + rowH() / 2.0F;

         for (int who = 0; who < 2; who++) {
            float sx = this.columnX(who);
            float sy = cy - sh / 2.0F;
            if (mx >= sx - 4.0F && mx < sx + sw + 4.0F && my >= sy - 3.0F && my < sy + sh + 3.0F) {
               return new int[]{p, who};
            }
         }
      }

      return null;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         int[] c = this.cellAt(mx, my);
         if (c != null) {
            BooleanSetting cell = this.module.hide[c[0]][c[1]];
            cell.set(!cell.get());
            if (cell.get() && !this.module.armorOn.get()) {
               this.module.armorOn.set(true);
            }

            ModSounds.toggle(cell.get());
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      int[] c = this.cellAt(mx, my);
      if (c == null) {
         return this.contains(mx, my) ? "Hide pieces of armor: only the look, the armor still counts" : null;
      } else {
         String piece = MiscModule.PIECES[c[0]].toLowerCase(Locale.ROOT);
         boolean on = this.module.cell(c[0], c[1] == 0);
         return c[1] == 0
            ? "Your " + piece + ": " + (on ? "hidden" : "shown") + " (in third person and the inventory)"
            : "Other players' " + piece + ": " + (on ? "hidden" : "shown") + " (on your screen only)";
      }
   }
}
