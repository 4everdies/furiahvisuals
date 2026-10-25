package meowtils.furiahvisuals.module.weapons;

import java.util.List;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class InspectWeightsWidget extends Widget {
   private static final float TITLE = 22.0F;
   private static final float ROW = 24.0F;
   private static final float PILL_W = 24.0F;
   private static final float PILL_H = 15.0F;
   private final WeaponsModule module;
   private final InspectWeightsSetting setting;
   private final WeaponDef def;
   private float[] rowHover = new float[0];

   InspectWeightsWidget(WeaponsModule module, InspectWeightsSetting setting, WeaponDef def) {
      this.module = module;
      this.setting = setting;
      this.def = def;
   }

   @Override
   public float height() {
      int n = this.setting.inspects().size();
      return 22.0F + (n == 0 ? 16.0F : n * 24.0F) + 4.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private float rowY(int i) {
      return this.y + 22.0F + i * 24.0F;
   }

   private float pillsX() {
      return this.x + this.w - 81.0F - 26.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      List<String> inspects = this.setting.inspects();
      if (this.rowHover.length != inspects.size()) {
         this.rowHover = new float[inspects.size()];
      }

      Ui.text(this.setting.getName(), this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
      String head = "How often";
      Ui.text(head, this.pillsX() + 39.0F - Ui.width(head, 7.0F) / 2.0F, this.y + 7.0F, 7.0F, Theme.TEXT_MUTED);
      if (inspects.isEmpty()) {
         Ui.text("This weapon has no inspects.", this.x, this.y + 22.0F + 2.0F, 8.4F, Theme.TEXT_MUTED);
      } else {
         for (int i = 0; i < inspects.size(); i++) {
            String id = inspects.get(i);
            int weight = this.setting.weight(id);
            boolean on = weight > 0;
            float ry = this.rowY(i);
            boolean over = mx >= this.x && mx < this.x + this.w && my >= ry && my < ry + 24.0F - 3.0F;
            this.rowHover[i] = Easing.approach(this.rowHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
            Ui.surface(this.x, ry, this.w, 21.0F, 6.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.rowHover[i]));
            float bx = this.x + 7.0F;
            float by = ry + 10.5F - 5.5F;
            if (on) {
               Ui.roundRect(bx, by, 11.0F, 11.0F, 3.0F, accent | 0xFF000000);
               Ui.textMiddle("✓", bx + 2.0F, by + 5.5F, 8.0F, -1, true);
            } else {
               Ui.outline(bx, by, 11.0F, 11.0F, 3.0F, 1.0F, Theme.OUTLINE_HOVER);
            }

            Ui.textMiddle(Inspects.displayName(id), bx + 18.0F, ry + 10.5F, 8.8F, on ? Theme.TEXT : Theme.TEXT_DIM, false);
            float px = this.pillsX();
            float py = ry + 3.0F;

            for (int k = 1; k <= 3; k++) {
               boolean lit = on && weight == k;
               boolean overPill = mx >= px && mx < px + 24.0F && my >= py && my < py + 15.0F;
               if (lit) {
                  Ui.roundRect(px, py, 24.0F, 15.0F, 7.5F, Theme.ACCENT_SOFT);
                  Ui.accentOutline(px, py, 24.0F, 15.0F, 7.5F, 0.9F, accent);
               } else {
                  Ui.roundRect(px, py, 24.0F, 15.0F, 7.5F, overPill ? Theme.SURFACE_HOVER : Theme.PILL);
               }

               String label = k + "×";
               Ui.textMiddle(label, px + 12.0F - Ui.width(label, 7.6F) / 2.0F, py + 7.5F, 7.6F, lit ? Theme.TEXT : Theme.TEXT_MUTED, lit);
               px += 27.0F;
            }

            float playX = this.x + this.w - 20.0F;
            float pcx = playX + 9.0F;
            float pcy = ry + 10.5F;
            boolean overPlay = mx >= playX && mx < this.x + this.w && my >= ry && my < ry + 24.0F - 3.0F;
            Ui.circle(pcx, pcy, 6.5F, overPlay ? Theme.ACCENT_SOFT_HOVER : Theme.PILL);
            IslandIcons.drawPlay(pcx + 0.7F, pcy, 8.4F, overPlay ? Theme.TEXT : Theme.TEXT_DIM);
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (this.contains(mx, my) && button == 0) {
         List<String> inspects = this.setting.inspects();

         for (int i = 0; i < inspects.size(); i++) {
            float ry = this.rowY(i);
            if (!(my < ry) && !(my >= ry + 24.0F - 3.0F)) {
               String id = inspects.get(i);
               if (mx >= this.x + this.w - 20.0F) {
                  if (this.module.previewInspect(this.def, id)) {
                     ModSounds.gui(Sfx.CLICK);
                  } else {
                     ModSounds.gui(Sfx.ERROR);
                  }

                  return true;
               }

               float px = this.pillsX();
               if (mx >= px && mx < px + 81.0F) {
                  int k = 1 + (int)((mx - px) / 27.0F);
                  this.setting.setWeight(id, Math.min(3, k));
                  ModSounds.gui(Sfx.SELECT);
                  return true;
               }

               boolean on = this.setting.weight(id) > 0;
               this.setting.setWeight(id, on ? 0 : 1);
               ModSounds.gui(on ? Sfx.TOGGLE_OFF : Sfx.TOGGLE_ON);
               return true;
            }
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
         List<String> inspects = this.setting.inspects();

         for (int i = 0; i < inspects.size(); i++) {
            float ry = this.rowY(i);
            if (!(my < ry) && !(my >= ry + 24.0F - 3.0F)) {
               if (mx >= this.x + this.w - 20.0F) {
                  return "Plays it in the preview above";
               }

               if (mx >= this.pillsX() && mx < this.pillsX() + 81.0F) {
                  return "How often it comes up next to the others (Order: Random)";
               }

               return this.setting.weight(inspects.get(i)) > 0 ? "Plays: click to leave it out" : "Left out: click to play it again";
            }
         }

         return this.setting.getDescription();
      }
   }
}
