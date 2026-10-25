package meowtils.furiahvisuals.module.particles;

import java.util.List;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;

final class LibraryImageWidget extends Widget {
   private static final float THUMB = 30.0F;
   private static final float GAP = 6.0F;
   private static final int MAX = 128;
   private final LibraryImageSetting setting;
   private final float[] itemX = new float[128];
   private final float[] itemY = new float[128];
   private final String[] itemName = new String[128];
   private int items;
   private float gridH = 16.0F;

   LibraryImageWidget(LibraryImageSetting setting) {
      this.setting = setting;
   }

   @Override
   public float height() {
      return 24.0F + this.gridH + 8.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      Ui.text(this.setting.getName(), this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
      ParticlesModule p = LibraryImageSetting.particles();
      if (p != null) {
         p.ensureLibrary();
         List<String> names = p.imageNames();
         this.items = 0;
         float cx = this.x;
         float cy = this.y + 22.0F;

         for (String name : names) {
            if (this.items >= 128) {
               break;
            }

            if (cx > this.x && cx + 30.0F > this.x + this.w) {
               cx = this.x;
               cy += 36.0F;
            }

            this.itemX[this.items] = cx;
            this.itemY[this.items] = cy;
            this.itemName[this.items] = name;
            this.items++;
            cx += 36.0F;
         }

         float f = this.gridH = this.items == 0 ? 16.0F : this.itemY[this.items - 1] + 30.0F - (this.y + 22.0F);
         if (this.items == 0) {
            Ui.text("No images yet: add some in Particles > Custom Images.", this.x, this.y + 24.0F, 8.4F, Theme.TEXT_MUTED);
         } else {
            for (int i = 0; i < this.items; i++) {
               float ix = this.itemX[i];
               float iy = this.itemY[i];
               boolean over = mx >= ix && mx < ix + 30.0F && my >= iy && my < iy + 30.0F;
               boolean used = this.itemName[i].equals(this.setting.get());
               Ui.checker(ix, iy, 30.0F, 30.0F, 6.0F, 3.5F);
               int tex = p.imageTexture(this.itemName[i]);
               if (tex > 0) {
                  Ui.image(tex, ix, iy, 30.0F, 30.0F, 0.0F, 0.0F, 1.0F, 1.0F, -1);
               }

               if (used) {
                  Ui.glow(ix + 15.0F, iy + 15.0F, 51.0F, ColorUtil.withAlpha(accent, 0.25F));
                  Ui.accentOutline(ix - 1.0F, iy - 1.0F, 32.0F, 32.0F, 7.0F, 1.4F, accent);
               } else {
                  Ui.outline(ix, iy, 30.0F, 30.0F, 6.0F, 0.8F, ColorUtil.mulAlpha(Theme.OUTLINE_HOVER, over ? 1.0F : 0.5F));
               }
            }
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (this.contains(mx, my) && button == 0) {
         for (int i = 0; i < this.items; i++) {
            if (mx >= this.itemX[i] && mx < this.itemX[i] + 30.0F && my >= this.itemY[i] && my < this.itemY[i] + 30.0F) {
               this.setting.set(this.itemName[i].equals(this.setting.get()) ? "" : this.itemName[i]);
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
         for (int i = 0; i < this.items; i++) {
            if (mx >= this.itemX[i] && mx < this.itemX[i] + 30.0F && my >= this.itemY[i] && my < this.itemY[i] + 30.0F) {
               return this.itemName[i] + (this.itemName[i].equals(this.setting.get()) ? "  ·  click to stop using it" : "  ·  click to use it");
            }
         }

         return this.setting.getDescription();
      }
   }
}
