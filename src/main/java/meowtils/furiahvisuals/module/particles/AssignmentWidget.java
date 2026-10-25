package meowtils.furiahvisuals.module.particles;

import java.util.List;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Segmented;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.ModeSetting;

final class AssignmentWidget extends Widget {
   private static final float THUMB = 30.0F;
   private static final float GAP = 6.0F;
   private static final int MAX = 128;
   private final ParticlesModule module;
   private final ImageAssignmentSetting setting;
   private final int kind;
   private final String[] labels;
   private final Segmented control = new Segmented();
   private final float[] itemX = new float[128];
   private final float[] itemY = new float[128];
   private final float[] itemW = new float[128];
   private final String[] itemName = new String[128];
   private final boolean[] itemMissing = new boolean[128];
   private int items;
   private float gridH;

   AssignmentWidget(ParticlesModule module, ImageAssignmentSetting setting, int kind) {
      this.module = module;
      this.setting = setting;
      this.kind = kind;
      ImageAssignmentSetting.Mode[] modes = ImageAssignmentSetting.Mode.values();
      this.labels = new String[modes.length];

      for (int i = 0; i < modes.length; i++) {
         this.labels[i] = ModeSetting.displayName(modes[i]);
      }
   }

   @Override
   public float height() {
      float h = 46.0F;
      if (this.setting.getMode() != ImageAssignmentSetting.Mode.VANILLA) {
         h += this.gridH + 8.0F;
      }

      return h;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      Ui.text(this.setting.getName(), this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
      this.control.render(this.x, this.y + 20.0F, this.w, 20.0F, this.labels, this.setting.getMode().ordinal(), mx, my, dt, accent);
      if (this.setting.getMode() != ImageAssignmentSetting.Mode.VANILLA) {
         this.layout();
         List<ImageLibrary.Entry> entries = this.module.library.entries();
         if (this.items == 0) {
            Ui.text(entries.isEmpty() ? "Import an image above first." : "No usable images yet.", this.x, this.y + 50.0F, 8.4F, Theme.TEXT_MUTED);
         } else {
            for (int i = 0; i < this.items; i++) {
               String name = this.itemName[i];
               boolean used = this.setting.uses(name);
               float ix = this.itemX[i];
               float iy = this.itemY[i];
               boolean over = mx >= ix && mx < ix + this.itemW[i] && my >= iy && my < iy + 30.0F;
               if (this.itemMissing[i]) {
                  Ui.roundRect(ix, iy, this.itemW[i], 30.0F, 7.0F, ColorUtil.withAlpha(Theme.DANGER, over ? 0.3F : 0.18F));
                  Ui.outline(ix, iy, this.itemW[i], 30.0F, 7.0F, 0.9F, ColorUtil.withAlpha(Theme.DANGER, 0.8F));
                  Ui.textMiddle(Ui.ellipsize("missing: " + name, 8.0F, this.itemW[i] - 12.0F, false), ix + 6.0F, iy + 15.0F, 8.0F, Theme.TEXT, false);
               } else {
                  ImageLibrary.Entry e = this.module.library.find(name);
                  Ui.checker(ix, iy, 30.0F, 30.0F, 6.0F, 3.5F);
                  if (e != null && e.state == ImageLibrary.State.READY) {
                     Ui.image(e.texture, ix, iy, 30.0F, 30.0F, 0.0F, 0.0F, 1.0F, 1.0F, -1);
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
   }

   private void layout() {
      this.items = 0;
      float cx = this.x;
      float cy = this.y + 48.0F;

      for (ImageLibrary.Entry e : this.module.library.entries()) {
         if (e.state != ImageLibrary.State.ERROR && this.items < 128) {
            cx = this.place(e.name, false, 30.0F, cx, cy);
            cy = this.itemY[this.items - 1];
         }
      }

      for (String name : this.setting.active()) {
         ImageLibrary.Entry ex = this.module.library.find(name);
         if ((ex == null || ex.state == ImageLibrary.State.ERROR) && this.items < 128 && this.module.library.hasScanned()) {
            cx = this.place(name, true, Math.min(this.w, 22.0F + Ui.width("missing: " + name, 8.0F)), cx, cy);
            cy = this.itemY[this.items - 1];
         }
      }

      this.gridH = this.items == 0 ? 16.0F : this.itemY[this.items - 1] + 30.0F - (this.y + 48.0F);
   }

   private float place(String name, boolean missing, float width, float cx, float cy) {
      if (cx > this.x && cx + width > this.x + this.w) {
         cx = this.x;
         cy += 36.0F;
      }

      this.itemX[this.items] = cx;
      this.itemY[this.items] = cy;
      this.itemW[this.items] = width;
      this.itemName[this.items] = name;
      this.itemMissing[this.items] = missing;
      this.items++;
      return cx + width + 6.0F;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (this.contains(mx, my) && button == 0) {
         int m = Segmented.hit(this.x, this.y + 20.0F, this.w, 20.0F, this.labels.length, mx, my);
         if (m >= 0) {
            this.setting.setMode(ImageAssignmentSetting.Mode.values()[m]);
            return true;
         } else if (this.setting.getMode() == ImageAssignmentSetting.Mode.VANILLA) {
            return true;
         } else {
            for (int i = 0; i < this.items; i++) {
               if (mx >= this.itemX[i] && mx < this.itemX[i] + this.itemW[i] && my >= this.itemY[i] && my < this.itemY[i] + 30.0F) {
                  if (this.itemMissing[i]) {
                     this.setting.remove(this.itemName[i]);
                  } else {
                     this.setting.click(this.itemName[i]);
                  }

                  return true;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else {
         int m = Segmented.hit(this.x, this.y + 20.0F, this.w, 20.0F, this.labels.length, mx, my);
         if (m >= 0) {
            return ImageAssignmentSetting.Mode.values()[m].description();
         } else {
            for (int i = 0; i < this.items; i++) {
               if (mx >= this.itemX[i] && mx < this.itemX[i] + this.itemW[i] && my >= this.itemY[i] && my < this.itemY[i] + 30.0F) {
                  if (this.itemMissing[i]) {
                     return this.itemName[i] + " is not in the particles folder any more (click to drop it)";
                  }

                  ImageLibrary.Entry e = this.module.library.find(this.itemName[i]);
                  String what = this.setting.getMode() == ImageAssignmentSetting.Mode.RANDOM
                     ? (this.setting.uses(this.itemName[i]) ? "click to take it out of the set" : "click to add it to the set")
                     : "click to use it";
                  return this.itemName[i] + (e != null && e.state == ImageLibrary.State.READY ? "  ·  " + e.sizes() : "") + "  ·  " + what;
               }
            }

            return this.setting.getDescription();
         }
      }
   }
}
