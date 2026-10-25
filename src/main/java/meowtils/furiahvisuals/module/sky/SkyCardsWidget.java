package meowtils.furiahvisuals.module.sky;

import java.util.Arrays;
import java.util.Set;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.search.SearchList;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class SkyCardsWidget extends Widget implements SearchList {
   private static final float GAP = 6.0F;
   private static final float MIN_TILE_W = 120.0F;
   private static final float ASPECT = 0.42F;
   private static final float RADIUS = 7.0F;
   private static final float NAME_SIZE = 8.6F;
   private static final float DESC_SIZE = 6.6F;
   private final SkyModule module;
   private final SkyStyleSetting setting;
   private final SkyStyle[] values = SkyStyle.values();
   private final float[] tileHover = new float[this.values.length];
   private float selX = -1.0F;
   private float selY = -1.0F;
   private int[] shown;

   SkyCardsWidget(SkyModule module, SkyStyleSetting setting) {
      this.module = module;
      this.setting = setting;
   }

   private int columns() {
      float min = 120.0F * Density.cardScale();
      int most = Density.dense() ? 6 : (Density.compact() ? 5 : 4);
      return Math.max(2, Math.min(most, (int)((this.w + 6.0F) / (min + 6.0F))));
   }

   private static boolean showsDescription() {
      return !Density.dense();
   }

   private static float nameSize() {
      return Density.dense() ? 6.8F : (Density.compact() ? 7.6F : 8.6F);
   }

   private float tileW() {
      int cols = this.columns();
      return (this.w - 6.0F * (cols - 1)) / cols;
   }

   private float tileH() {
      return Math.round(this.tileW() * 0.42F);
   }

   private int count() {
      return this.shown == null ? this.values.length : this.shown.length;
   }

   private int valueAt(int slot) {
      return this.shown == null ? slot : this.shown[slot];
   }

   private int slotOf(int value) {
      if (this.shown == null) {
         return value;
      } else {
         for (int i = 0; i < this.shown.length; i++) {
            if (this.shown[i] == value) {
               return i;
            }
         }

         return -1;
      }
   }

   private float tileX(int slot) {
      return this.x + slot % this.columns() * (this.tileW() + 6.0F);
   }

   private float tileY(int slot) {
      return this.y + slot / this.columns() * (this.tileH() + 6.0F);
   }

   private int slotAt(float mx, float my) {
      float tw = this.tileW();
      float th = this.tileH();

      for (int s = 0; s < this.count(); s++) {
         float tx = this.tileX(s);
         float ty = this.tileY(s);
         if (mx >= tx && mx < tx + tw && my >= ty && my < ty + th) {
            return s;
         }
      }

      return -1;
   }

   @Override
   public float height() {
      int rows = (this.count() + this.columns() - 1) / this.columns();
      return rows == 0 ? 0.0F : rows * this.tileH() + (rows - 1) * 6.0F + 4.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      int cols = this.columns();
      float tw = this.tileW();
      float th = this.tileH();
      int selectedSlot = this.slotOf(this.setting.getIndex());
      if (selectedSlot >= 0) {
         float targetX = this.x + selectedSlot % cols * (tw + 6.0F);
         float targetY = selectedSlot / cols * (th + 6.0F);
         this.selX = this.selX < 0.0F ? targetX : Easing.approach(this.selX, targetX, 18.0F, dt);
         this.selY = this.selY < 0.0F ? targetY : Easing.approach(this.selY, targetY, 18.0F, dt);
      }

      boolean wait = false;
      int atlas = false ? 0 : this.module.drawCardViews(Math.max(8, Math.round(tw * Ui.px)), Math.max(8, Math.round(th * Ui.px)));
      if (selectedSlot >= 0) {
         Ui.glow(this.selX + tw / 2.0F, this.y + this.selY + th / 2.0F, tw * 1.35F, ColorUtil.withAlpha(accent, 0.24F));
      }

      SkyStyle hovered = null;
      int selected = this.setting.getIndex();

      for (int s = 0; s < this.count(); s++) {
         int i = this.valueAt(s);
         SkyStyle sky = this.values[i];
         float tx = this.tileX(s);
         float ty = this.tileY(s);
         boolean over = mx >= tx && mx < tx + tw && my >= ty && my < ty + th;
         boolean usable = this.module.isUsable(sky);
         if (over && usable) {
            hovered = sky;
         }

         this.tileHover[i] = Easing.approach(this.tileHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
         boolean isSelected = i == selected;
         Ui.pushAlpha(usable ? 1.0F : 0.4F);
         if (atlas > 0) {
            float[] uv = this.module.cardViewUv(sky);
            Ui.roundedImage(atlas, tx, ty, tw, th, 7.0F, uv[0], uv[1], uv[2], uv[3], -1);
         } else if (!wait) {
            Ui.gradientH(tx, ty, tw, th, 7.0F, ColorUtil.shade(sky.colorA, 0.35F), ColorUtil.shade(sky.colorB, 0.35F));
         }

         if (this.tileHover[i] > 0.01F) {
            Ui.roundRect(tx, ty, tw, th, 7.0F, ColorUtil.withAlpha(-1, 0.07F * this.tileHover[i]));
         }

         float shadeTop = ty + th * 0.28F;
         Ui.gradientV(tx, shadeTop, tw, ty + th - shadeTop, 7.0F, 0, -671088640);
         int nameColor = isSelected ? -1 : ColorUtil.lerp(-1907990, -1, this.tileHover[i]);
         float ns = nameSize();
         if (showsDescription()) {
            Ui.textBold(Ui.ellipsize(sky.displayName(), ns, tw - 12.0F, true), tx + 6.0F, ty + th - ns - 6.6F - 8.0F, ns, nameColor);
            Ui.text(Ui.ellipsize(sky.description(), 6.6F, tw - 12.0F, false), tx + 6.0F, ty + th - 6.6F - 5.0F, 6.6F, isSelected ? -419430401 : -1191182337);
         } else {
            Ui.textBold(Ui.ellipsize(sky.displayName(), ns, tw - 8.0F, true), tx + 4.0F, ty + th - ns - 4.0F, ns, nameColor);
         }

         String tag = sky.tag();
         if (tag != null) {
            float pw = Ui.widthBold(tag, 5.6F) + 7.0F;
            float px = tx + tw - pw - 4.0F;
            Ui.roundRect(px, ty + 4.0F, pw, 9.5F, 4.75F, isSelected ? accent | 0xFF000000 : -1342177280);
            Ui.textMiddle(tag, px + 3.5F, ty + 8.75F, 5.6F, -1, true);
         }

         if (this.module.isModified(sky)) {
            Ui.circle(tx + 8.5F, ty + 8.5F, 3.6F, -1342177280);
            Ui.circle(tx + 8.5F, ty + 8.5F, 2.4F, ColorUtil.lerp(accent, -1, 0.15F) | 0xFF000000);
         }

         Ui.outline(tx, ty, tw, th, 7.0F, 0.8F, isSelected ? 0 : 620756991);
         Ui.popAlpha();
      }

      this.module.onOptionHover(this.setting, hovered);
      if (selectedSlot >= 0) {
         Ui.accentOutline(this.selX - 1.5F, this.y + this.selY - 1.5F, tw + 3.0F, th + 3.0F, 8.5F, 1.4F, accent);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         int s = this.slotAt(mx, my);
         if (s >= 0) {
            if (this.module.isUsable(this.values[this.valueAt(s)])) {
               this.setting.setIndex(this.valueAt(s));
               ModSounds.gui(Sfx.SELECT);
            } else {
               ModSounds.gui(Sfx.ERROR);
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
         int s = this.slotAt(mx, my);
         if (s < 0) {
            return null;
         } else {
            SkyStyle sky = this.values[this.valueAt(s)];
            String why = this.module.unusableReason(sky);
            if (why != null) {
               return sky.displayName() + ": " + why;
            } else {
               String full = showsDescription() && !(Ui.width(sky.description(), 6.6F) > this.tileW() - 12.0F) ? null : sky.description();
               String changed = this.module.isModified(sky) ? "Its settings differ from its defaults" : null;
               if (sky.isDynamic()) {
                  return (full != null ? full + ". " : "")
                     + (sky.tag() != null ? "New: its" : "Its")
                     + " own options show under the skies once it is picked"
                     + (changed != null ? ". " + changed : "");
               } else {
                  return full != null && changed != null ? full + ". " + changed : (full != null ? full : changed);
               }
            }
         }
      }
   }

   @Override
   public void searchItems(SearchList.Items out) {
   }

   @Override
   public void searchFilter(Set<Object> keys) {
      if (keys == null) {
         this.shown = null;
      } else {
         int n = 0;
         int[] idx = new int[this.values.length];

         for (int i = 0; i < this.values.length; i++) {
            if (keys.contains(this.values[i])) {
               idx[n++] = i;
            }
         }

         this.shown = Arrays.copyOf(idx, n);
      }
   }

   @Override
   public float reveal(Object key) {
      for (int i = 0; i < this.values.length; i++) {
         if (this.values[i] == key) {
            int slot = this.slotOf(i);
            return slot < 0 ? -1.0F : slot / this.columns() * (this.tileH() + 6.0F);
         }
      }

      return -1.0F;
   }

   @Override
   public float revealHeight(Object key) {
      return this.tileH();
   }

   @Override
   public int searchVersion() {
      return 0;
   }
}
