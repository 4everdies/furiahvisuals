package meowtils.furiahvisuals.module.weapons;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class StylePickerWidget extends Widget {
   static final String SEEN = "weapons.stylePicker";
   private static final String[] TITLES = new String[]{"HD", "Pixelated"};
   private static final String[] LINES = new String[]{"Smooth, detailed models", "3D pixel art that fits Minecraft"};
   private static final String[] TIPS = new String[]{
      "Every weapon in its smooth, detailed HD model (a weapon's own Style still wins)",
      "Every weapon as 3D pixel art that fits Minecraft (a weapon's own Style still wins)"
   };
   private static final String HINT = "Every weapon has both styles. You can also set a style per weapon.";
   private static final float TURN_SPEED = 26.0F;
   private final WeaponsModule module;
   private final WeaponStyleSetting setting;
   private final WeaponViews.Card[] tiles = new WeaponViews.Card[]{new WeaponViews.Card(), new WeaponViews.Card()};
   private final List<WeaponViews.Card> drawn = new ArrayList<>(2);
   private final float[] hover = new float[2];
   private final float[] uv = new float[4];
   private float angle = 24.0F;
   private float ringX = -1.0F;

   StylePickerWidget(WeaponsModule module, WeaponStyleSetting setting) {
      this.module = module;
      this.setting = setting;
      this.tiles[0].style = 0;
      this.tiles[1].style = 1;
   }

   private static float gap() {
      return Density.dense() ? 4.0F : (Density.compact() ? 5.0F : 6.0F);
   }

   private float tileW() {
      return (this.w - gap()) / 2.0F;
   }

   private float pictureH() {
      return Math.round(this.tileW() * (Density.dense() ? 0.4F : (Density.compact() ? 0.46F : 0.5F)));
   }

   private static float titleSize() {
      return Density.dense() ? 8.0F : (Density.compact() ? 8.8F : 9.8F);
   }

   private static float lineSize() {
      return Density.compact() ? 7.0F : 7.6F;
   }

   private static float textH() {
      return titleSize() + lineSize() + (Density.compact() ? 11.0F : 15.0F);
   }

   private float tileH() {
      return this.pictureH() + textH();
   }

   private static float hintSize() {
      return Density.small();
   }

   @Override
   public float height() {
      return this.tileH() + 5.0F + hintSize() + 4.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private WeaponDef shownDef() {
      WeaponDef d = this.module.selectedDef();
      if (!d.isVanilla()) {
         return d;
      } else {
         for (WeaponDef x : this.module.library.all()) {
            if (!x.isVanilla() && !x.isImported()) {
               return x;
            }
         }

         return d;
      }
   }

   private float tileX(int i) {
      return this.x + i * (this.tileW() + gap());
   }

   private int tileAt(float mx, float my) {
      float tw = this.tileW();
      float th = this.tileH();

      for (int i = 0; i < 2; i++) {
         float tx = this.tileX(i);
         if (mx >= tx && mx < tx + tw && my >= this.y && my < this.y + th) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float tw = this.tileW();
      float th = this.tileH();
      float ph = this.pictureH();
      float radius = Density.dense() ? 5.0F : 7.0F;
      int active = this.setting.get() == WeaponOptions.WeaponStyle.PIXELATED ? 1 : 0;
      this.ringX = this.ringX < 0.0F ? this.tileX(active) : Easing.approach(this.ringX, this.tileX(active), 18.0F, dt);
      WeaponDef def = this.shownDef();
      this.angle = (this.angle + dt * 26.0F) % 360.0F;
      float inset = Density.dense() ? 3.0F : 4.0F;
      float pw = tw - inset * 2.0F;
      float pictureH = ph - inset;
      this.drawn.clear();

      for (int i = 0; i < 2; i++) {
         WeaponViews.Card c = this.tiles[i];
         c.def = def;
         c.angle = this.angle;
         c.cell = -1;
         if (!Ui.clippedOut(this.tileX(i) + inset, this.y + inset, pw, pictureH)) {
            this.drawn.add(c);
         }
      }

      int atlas = this.drawn.isEmpty() ? -1 : this.module.views().drawTiles(this.drawn, Math.round(pw * Ui.px), Math.round(pictureH * Ui.px));

      for (int ix = 0; ix < 2; ix++) {
         float tx = this.tileX(ix);
         boolean over = mx >= tx && mx < tx + tw && my >= this.y && my < this.y + th;
         this.hover[ix] = Easing.approach(this.hover[ix], over ? 1.0F : 0.0F, 16.0F, dt);
         Ui.surface(tx, this.y, tw, th, radius, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hover[ix]));
      }

      ringFill(this.ringX, this.y, tw, th, radius, accent);
      float ts = titleSize();
      float ls = lineSize();
      float textX0 = Density.compact() ? 7.0F : 9.0F;

      for (int ix = 0; ix < 2; ix++) {
         float tx = this.tileX(ix);
         float px = tx + inset;
         float py = this.y + inset;
         if (atlas > 0 && this.tiles[ix].cell >= 0) {
            this.module.views().tileUv(this.tiles[ix].cell, this.uv);
            Ui.roundedImage(atlas, px, py, pw, pictureH, radius - 2.0F, this.uv[0], this.uv[1], this.uv[2], this.uv[3], -1);
         } else {
            Ui.gradientV(px, py, pw, pictureH, radius - 2.0F, -13749699, -15461095);
         }

         float titleY = this.y + ph + (Density.compact() ? 4.0F : 6.0F);
         Ui.textBold(TITLES[ix], tx + textX0, titleY, ts, ix == active ? Theme.TEXT : Theme.TEXT_DIM);
         Ui.text(Ui.ellipsize(LINES[ix], ls, tw - textX0 * 2.0F, false), tx + textX0, titleY + ts + 3.0F, ls, Theme.TEXT_MUTED);
         Ui.registerControl(tx, this.y, tw, th, TITLES[ix]);
         if (ix == 1 && !MenuMemory.seen("weapons.stylePicker")) {
            float ns = Density.compact() ? 5.4F : 6.0F;
            float nh = Density.compact() ? 9.5F : 11.0F;
            float nw = Ui.widthBold("NEW", ns) + 8.0F;
            float nx = px + pw - nw - 4.0F;
            float ny = py + 4.0F;
            Ui.roundRect(nx, ny, nw, nh, nh / 2.0F, ColorUtil.withAlpha(accent, 0.92F));
            Ui.textMiddle("NEW", nx + 4.0F, ny + nh / 2.0F, ns, -1, true);
         }
      }

      ringLine(this.ringX, this.y, tw, th, radius, accent);
      Ui.text(
         Ui.ellipsize("Every weapon has both styles. You can also set a style per weapon.", hintSize(), this.w, false),
         this.x,
         this.y + th + 5.0F,
         hintSize(),
         Theme.TEXT_MUTED
      );
   }

   private static void ringFill(float rx, float ry, float rw, float rh, float radius, int accent) {
      Ui.glow(rx + rw / 2.0F, ry + rh / 2.0F, rw * 1.15F, ColorUtil.withAlpha(accent, 0.16F));
      Ui.roundRect(rx, ry, rw, rh, radius, Theme.ACCENT_SOFT);
   }

   private static void ringLine(float rx, float ry, float rw, float rh, float radius, int accent) {
      Ui.accentOutline(rx, ry, rw, rh, radius, 1.2F, accent);
   }

   static float[] ringForColorCheck(float rx, float ry, float rw, float rh, int accent) {
      float radius = Density.dense() ? 5.0F : 7.0F;
      ringFill(rx, ry, rw, rh, radius, accent);
      Ui.textMiddle(TITLES[1], rx + 7.0F, ry + rh / 2.0F, titleSize(), Theme.TEXT, true);
      ringLine(rx, ry, rw, rh, radius, accent);
      return new float[]{rx + rw / 2.0F - 2.0F, ry - 1.5F, 4.0F, 4.0F};
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         int i = this.tileAt(mx, my);
         if (i >= 0) {
            this.setting.set(i == 1 ? WeaponOptions.WeaponStyle.PIXELATED : WeaponOptions.WeaponStyle.HD);
            MenuMemory.markSeen("weapons.stylePicker");
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
         int i = this.tileAt(mx, my);
         return i >= 0 ? TIPS[i] : this.setting.getDescription();
      }
   }
}
