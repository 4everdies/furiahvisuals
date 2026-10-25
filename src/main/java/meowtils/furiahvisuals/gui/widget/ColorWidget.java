package meowtils.furiahvisuals.gui.widget;

import java.util.Locale;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.MenuIcons;
import meowtils.furiahvisuals.gui.MenuPrefs;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.popup.ColorPopover;
import meowtils.furiahvisuals.gui.popup.Popovers;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SettingDefaults;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.gui.GuiScreen;

public final class ColorWidget extends Widget {
   public static Runnable globalEditor;
   private static final float ROW = 26.0F;
   private static final float PICKER_H = 92.0F;
   private static final float STRIP_W = 11.0F;
   private static final float GAP = 8.0F;
   private static final float SLIDER_H = 26.0F;
   private static final int DRAG_NONE = 0;
   private static final int DRAG_SV = 1;
   private static final int DRAG_HUE = 2;
   private static final int DRAG_ALPHA = 3;
   private static final int DRAG_SLIDER = 4;
   private static final int S_SCROLL = 0;
   private static final int S_SPEED = 1;
   private static final int S_SPREAD = 2;
   private static final int S_SAT = 3;
   private static final int S_BRIGHT = 4;
   private final ColorSetting setting;
   private final ColorSetting.Mode[] modes;
   private final String[] modeLabels;
   private final Segmented modeControl = new Segmented();
   private boolean expanded;
   private boolean alwaysExpanded;
   private float expand;
   private int editing;
   private float hue;
   private float sat;
   private float val;
   private float alpha = 1.0F;
   private int synced;
   private boolean hasSync;
   private int drag;
   private int dragSlider;
   private boolean hexFocused;
   private String hexText = "";
   private float caret;
   private float linkHover;
   private boolean inPopover;
   private static final float MODE_W = 11.0F;
   private static final float ICON_W = 11.0F;
   private float rowModeX;
   private float rowSwatchX;
   private float rowSwatchW;
   private float rowSwatchBX;
   private float rowHexX;
   private float rowHexW;
   private float rowStarX;
   private float rowResetX;
   private float rowCh;

   public ColorWidget(ColorSetting setting) {
      this.setting = setting;
      ColorSetting.Mode[] all = ColorSetting.Mode.values();
      int n = setting.allowsGlobal() ? all.length : all.length - 1;
      this.modes = new ColorSetting.Mode[n];
      int k = 0;

      for (ColorSetting.Mode m : all) {
         if (m != ColorSetting.Mode.GLOBAL || setting.allowsGlobal()) {
            this.modes[k++] = m;
         }
      }

      this.modeLabels = new String[n];

      for (int i = 0; i < n; i++) {
         this.modeLabels[i] = ModeSetting.displayName(this.modes[i]);
      }
   }

   public void setExpanded(boolean expanded) {
      if (this.compactRow()) {
         if (expanded) {
            this.openPopover(0);
         }
      } else {
         this.expanded = expanded;
      }
   }

   public ColorWidget alwaysExpanded() {
      this.alwaysExpanded = true;
      this.expanded = true;
      this.expand = 1.0F;
      return this;
   }

   public ColorWidget inPopover() {
      this.inPopover = true;
      return this;
   }

   public void editSlot(int slot) {
      this.editing = slot == 1 && this.gradient() ? 1 : 0;
      this.hasSync = false;
   }

   public void applyColor(int color) {
      if (this.editing == 1 && this.gradient()) {
         this.setting.setColorB(color);
      } else {
         this.setting.setColorA(color);
      }

      this.hasSync = false;
   }

   private boolean compactRow() {
      return Density.compact() && !this.alwaysExpanded && !this.inPopover;
   }

   public ColorSetting getSetting() {
      return this.setting;
   }

   private ColorSetting.Mode mode() {
      return this.setting.getMode();
   }

   private boolean gradient() {
      return this.mode() == ColorSetting.Mode.GRADIENT;
   }

   private boolean chroma() {
      return this.mode() == ColorSetting.Mode.CHROMA;
   }

   private boolean onGlobal() {
      return this.mode() == ColorSetting.Mode.GLOBAL;
   }

   private float expandedHeight() {
      float h = 34.0F;
      if (this.onGlobal()) {
         return h + 34.0F + 6.0F;
      } else {
         if (this.gradient()) {
            h += 28.0F;
         }

         h += 126.0F;
         if (this.gradient()) {
            h += 26.0F;
         } else if (this.chroma()) {
            h += 52.0F;
         }

         return h + 4.0F;
      }
   }

   @Override
   public float height() {
      return this.compactRow() ? Density.row() : 26.0F + this.expand * this.expandedHeight();
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private float modeY() {
      return this.y + 26.0F + 6.0F;
   }

   private float chipsY() {
      return this.modeY() + 28.0F;
   }

   private float pickerY() {
      return this.gradient() ? this.chipsY() + 28.0F : this.modeY() + 28.0F;
   }

   private float svW() {
      return this.w - 38.0F;
   }

   private float hueX() {
      return this.x + this.svW() + 8.0F;
   }

   private float alphaX() {
      return this.hueX() + 11.0F + 8.0F;
   }

   private float bottomY() {
      return this.pickerY() + 92.0F + 8.0F;
   }

   private float slidersY() {
      return this.bottomY() + 18.0F + 8.0F;
   }

   private void sync() {
      int c = this.editing == 0 ? this.setting.getColorA() : this.setting.getColorB();
      if (!this.hasSync || c != this.synced) {
         float[] hsv = ColorUtil.toHsv(c, new float[3]);
         if (hsv[1] > 0.001F && hsv[2] > 0.001F) {
            this.hue = hsv[0];
         }

         this.sat = hsv[1];
         this.val = hsv[2];
         this.alpha = ColorUtil.alpha(c) / 255.0F;
         this.synced = c;
         this.hasSync = true;
      }
   }

   private void apply() {
      int c;
      this.synced = c = ColorUtil.withAlpha(ColorUtil.hsv(this.hue, this.sat, this.val), this.alpha);
      if (this.editing == 0) {
         this.setting.setColorA(c);
      } else {
         this.setting.setColorB(c);
      }
   }

   private int current() {
      return ColorUtil.withAlpha(ColorUtil.hsv(this.hue, this.sat, this.val), this.alpha);
   }

   private int modeIndex() {
      for (int i = 0; i < this.modes.length; i++) {
         if (this.modes[i] == this.mode()) {
            return i;
         }
      }

      return 0;
   }

   private void layoutRow() {
      float ch;
      this.rowCh = ch = Density.control();
      float right = this.x + this.w;
      this.rowResetX = right - 11.0F;
      right = this.rowResetX - 3.0F;
      boolean staticColor = this.mode() == ColorSetting.Mode.STATIC;
      float f = this.rowStarX = staticColor ? right - 11.0F : -1.0F;
      if (staticColor) {
         right = this.rowStarX - 3.0F;
      }

      float small = Density.small();
      float f2 = this.rowHexW = !staticColor && !this.gradient() && !this.chroma() ? 0.0F : Ui.width("#FFFFFF", small) + 2.0F;
      if (this.gradient() || this.chroma()) {
         this.rowHexW = Ui.width("9.99×", small) + 2.0F;
      } else if (this.onGlobal()) {
         this.rowHexW = Ui.width("Global", small) + 2.0F;
      }

      this.rowHexX = right - this.rowHexW;
      right = this.rowHexX - 5.0F;
      if (this.gradient()) {
         this.rowSwatchW = ch * 1.15F;
         this.rowSwatchBX = right - this.rowSwatchW;
         this.rowSwatchX = this.rowSwatchBX - 9.0F - this.rowSwatchW;
         right = this.rowSwatchX - 5.0F;
      } else {
         this.rowSwatchW = Math.max(18.0F, ch * 1.7F);
         this.rowSwatchX = right - this.rowSwatchW;
         this.rowSwatchBX = -1.0F;
         right = this.rowSwatchX - 5.0F;
      }

      this.rowModeX = right - this.modes.length * 11.0F;
   }

   private void renderRow(float mx, float my, float dt, int accent) {
      this.layoutRow();
      float rh = Density.row();
      float cy = this.y + rh / 2.0F;
      float ch = this.rowCh;
      boolean overRow = mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + rh;
      this.animateHover(overRow, dt);
      if (this.hover > 0.01F) {
         Ui.roundRect(this.x - 4.0F, this.y, this.w + 8.0F, rh, 5.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, this.hover * 0.8F));
      }

      float ts = Density.text();
      Ui.textMiddle(Ui.ellipsize(this.setting.getLabel(), ts, this.rowModeX - this.x - 6.0F, false), this.x, cy, ts, Theme.TEXT, false);
      int current = this.modeIndex();

      for (int i = 0; i < this.modes.length; i++) {
         float bx = this.rowModeX + i * 11.0F;
         boolean sel = i == current;
         boolean over = mx >= bx && mx < bx + 11.0F && my >= cy - ch / 2.0F && my < cy + ch / 2.0F;
         if (sel) {
            Ui.roundRect(bx + 0.5F, cy - ch / 2.0F, 10.0F, ch, 3.5F, Theme.ACCENT_SOFT);
         } else if (over) {
            Ui.roundRect(bx + 0.5F, cy - ch / 2.0F, 10.0F, ch, 3.5F, Theme.SURFACE_HOVER);
         }

         modeGlyph(this.modes[i], bx + 5.5F, cy, sel ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT, over ? 0.7F : 0.0F), accent);
      }

      float sy = cy - ch / 2.0F;
      if (this.gradient()) {
         smallSwatch(this.setting.getColorA(), this.rowSwatchX, sy, this.rowSwatchW, ch);

         for (int k = 0; k <= 4; k++) {
            float f = k / 4.0F;
            Ui.circle(this.rowSwatchX + this.rowSwatchW + 2.0F + f * 5.0F, cy, 0.55F, Theme.TEXT_MUTED);
         }

         Ui.circle(this.rowSwatchX + this.rowSwatchW + 6.0F, cy - 1.2F, 0.55F, Theme.TEXT_MUTED);
         Ui.circle(this.rowSwatchX + this.rowSwatchW + 6.0F, cy + 1.2F, 0.55F, Theme.TEXT_MUTED);
         smallSwatch(this.setting.getColorB(), this.rowSwatchBX, sy, this.rowSwatchW, ch);
      } else {
         drawSwatch(this.setting, this.rowSwatchX, sy, this.rowSwatchW, ch);
      }

      String side = this.gradient()
         ? (this.setting.getScrollSpeed() <= 0.0 ? "still" : String.format(Locale.ROOT, "%.2f×", this.setting.getScrollSpeed()))
         : (
            this.chroma()
               ? String.format(Locale.ROOT, "%.2f×", this.setting.getChromaSpeed())
               : (this.onGlobal() ? "Global" : ColorUtil.toHex(this.setting.getColorA()))
         );
      float small = Density.small();
      Ui.textMiddle(Ui.ellipsize(side, small, this.rowHexW + 4.0F, false), this.rowHexX, cy, small, Theme.TEXT_DIM, false);
      if (this.rowStarX >= 0.0F) {
         boolean fav = MenuPrefs.isFavorite(this.setting.getColorA());
         boolean over = mx >= this.rowStarX && mx < this.rowStarX + 11.0F && my >= this.y && my < this.y + rh;
         star(this.rowStarX + 5.5F, cy, 3.6F, fav ? accent : ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT, over ? 0.8F : 0.0F), fav);
      }

      boolean changed = !SettingDefaults.isDefault(this.setting);
      boolean overReset = mx >= this.rowResetX && mx < this.rowResetX + 11.0F && my >= this.y && my < this.y + rh;
      resetGlyph(
         this.rowResetX + 5.5F,
         cy,
         3.4F,
         changed ? ColorUtil.lerp(Theme.TEXT_DIM, accent, overReset ? 1.0F : 0.0F) : ColorUtil.withAlpha(Theme.TEXT_MUTED, 0.5F)
      );
   }

   private static void smallSwatch(int c, float sx, float sy, float sw, float sh) {
      if (c >>> 24 < 255) {
         Ui.checker(sx, sy, sw, sh, 3.5F, 3.0F);
      }

      Ui.roundRect(sx, sy, sw, sh, 3.5F, c);
      Ui.outline(sx, sy, sw, sh, 3.5F, 0.8F, Theme.OUTLINE_HOVER);
   }

   private static void modeGlyph(ColorSetting.Mode m, float cx, float cy, int color, int accent) {
      switch (m) {
         case GLOBAL:
            Ui.outline(cx - 3.2F, cy - 3.2F, 6.4F, 6.4F, 3.2F, 0.8F, color);
            Ui.rect(cx - 3.2F, cy - 0.3F, 6.4F, 0.6F, color);
            Ui.outline(cx - 1.3F, cy - 3.2F, 2.6F, 6.4F, 1.3F, 0.6F, color);
            break;
         case STATIC:
            Ui.roundRect(cx - 2.8F, cy - 2.8F, 5.6F, 5.6F, 1.4F, color);
            break;
         case GRADIENT:
            Ui.gradientH(cx - 3.4F, cy - 2.4F, 6.8F, 4.8F, 1.4F, color, ColorUtil.withAlpha(color, 0.25F));
            break;
         default:
            Ui.hueStrip(cx - 3.4F, cy - 2.4F, 6.8F, 4.8F, 1.4F, false);
      }
   }

   public static void star(float cx, float cy, float r, int color, boolean filled) {
      float[] px = new float[10];
      float[] py = new float[10];

      for (int i = 0; i < 10; i++) {
         double a = (-Math.PI / 2) + i * Math.PI / 5.0;
         float rr = i % 2 == 0 ? r : r * 0.45F;
         px[i] = cx + (float)Math.cos(a) * rr;
         py[i] = cy + (float)Math.sin(a) * rr;
      }

      for (int var11 = 0; var11 < 10; var11++) {
         int j = (var11 + 1) % 10;
         Ui.line(px[var11], py[var11], px[j], py[j], 0.8F, true, color);
      }

      if (filled) {
         Ui.circle(cx, cy + 0.2F, r * 0.5F, color);

         for (int var12 = 0; var12 < 10; var12 += 2) {
            Ui.line(cx, cy + 0.2F, px[var12], py[var12], r * 0.42F, true, color);
         }
      }
   }

   public static void resetGlyph(float cx, float cy, float r, int color) {
      MenuIcons.reset(cx, cy, r, color);
   }

   private void openPopover(int slot) {
      Popovers.open(new ColorPopover(this.setting, slot), this.x, this.y, this.w, Density.row());
   }

   private boolean rowMouseDown(float mx, float my, int button) {
      float rh = Density.row();
      if (!(mx >= this.x) || !(mx < this.x + this.w) || !(my >= this.y) || !(my < this.y + rh)) {
         return false;
      } else if (button != 0) {
         return true;
      } else {
         float ch = this.rowCh;
         float cy = this.y + rh / 2.0F;

         for (int i = 0; i < this.modes.length; i++) {
            float bx = this.rowModeX + i * 11.0F;
            if (mx >= bx && mx < bx + 11.0F && my >= cy - ch / 2.0F - 2.0F && my < cy + ch / 2.0F + 2.0F) {
               if (this.modes[i] != this.mode()) {
                  this.setting.setMode(this.modes[i]);
                  ModSounds.gui(Sfx.CLICK);
               }

               return true;
            }
         }

         if (this.rowStarX >= 0.0F && mx >= this.rowStarX && mx < this.rowStarX + 11.0F) {
            MenuPrefs.toggleFavorite(this.setting.getColorA());
            ModSounds.gui(Sfx.CLICK);
            return true;
         } else if (mx >= this.rowResetX && mx < this.rowResetX + 11.0F) {
            if (!SettingDefaults.isDefault(this.setting)) {
               this.setting.reset();
               this.hasSync = false;
               ModSounds.gui(Sfx.CLICK);
            }

            return true;
         } else if (this.onGlobal() && mx >= this.rowSwatchX && globalEditor != null) {
            globalEditor.run();
            return true;
         } else {
            int slot = this.rowSwatchBX >= 0.0F && mx >= this.rowSwatchBX - 2.0F && mx < this.rowSwatchBX + this.rowSwatchW + 2.0F ? 1 : 0;
            ModSounds.gui(Sfx.DROPDOWN);
            this.openPopover(slot);
            return true;
         }
      }
   }

   private String rowTooltip(float mx, float my) {
      float rh = Density.row();
      float ch = this.rowCh;
      float cy = this.y + rh / 2.0F;
      if (mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + rh) {
         for (int i = 0; i < this.modes.length; i++) {
            float bx = this.rowModeX + i * 11.0F;
            if (mx >= bx && mx < bx + 11.0F && my >= cy - ch / 2.0F - 2.0F && my < cy + ch / 2.0F + 2.0F) {
               return this.modeLabels[i] + ": " + this.modes[i].description();
            }
         }

         if (this.rowStarX >= 0.0F && mx >= this.rowStarX && mx < this.rowStarX + 11.0F) {
            return MenuPrefs.isFavorite(this.setting.getColorA())
               ? "A favorite (click to remove it)"
               : "Add to your favorite colors (at the top of every color picker)";
         } else if (mx >= this.rowResetX && mx < this.rowResetX + 11.0F) {
            return SettingDefaults.isDefault(this.setting) ? "At its default" : "Back to the default";
         } else {
            return this.onGlobal() && mx >= this.rowSwatchX
               ? "Follows the Global Color (click to edit it on the Settings page)"
               : this.setting.getDescription() + "  (click to pick)";
         }
      } else {
         return null;
      }
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      if (!this.gradient() && this.editing == 1) {
         this.editing = 0;
         this.hasSync = false;
      }

      if (this.compactRow()) {
         this.expanded = false;
         this.expand = 0.0F;
         this.renderRow(mx, my, dt, accent);
      } else {
         this.sync();
         boolean overRow = mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + 26.0F;
         this.animateHover(overRow, dt);
         this.expand = this.alwaysExpanded ? 1.0F : Easing.approach(this.expand, this.expanded ? 1.0F : 0.0F, 14.0F, dt);
         this.caret += dt;
         if (this.hover > 0.01F && !this.alwaysExpanded) {
            Ui.roundRect(this.x - 6.0F, this.y + 1.0F, this.w + 12.0F, 24.0F, 6.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, this.hover));
         }

         Ui.textMiddle(this.setting.getLabel(), this.x, this.y + 13.0F, 10.0F, Theme.TEXT, false);
         float sw = 36.0F;
         float sh = 15.0F;
         float sx = this.x + this.w - sw;
         float sy = this.y + (26.0F - sh) / 2.0F;
         drawSwatch(this.setting, sx, sy, sw, sh);
         String label = ModeSetting.displayName(this.mode());
         Ui.textMiddle(label, sx - 8.0F - Ui.width(label, 8.5F), this.y + 13.0F, 8.5F, Theme.TEXT_DIM, false);
         if (!(this.expand <= 0.01F)) {
            Ui.pushScissor(this.x - 8.0F, this.y + 26.0F, this.w + 16.0F, this.expand * this.expandedHeight());
            Ui.pushAlpha(this.expand);
            this.modeControl.render(this.x, this.modeY(), this.w, 20.0F, this.modeLabels, this.modeIndex(), mx, my, dt, accent);
            if (this.onGlobal()) {
               this.drawGlobalRow(mx, my, dt, accent);
            } else {
               if (this.gradient()) {
                  this.drawChips(mx, my, accent);
               }

               this.drawPicker();
               this.drawBottomRow(accent);
               this.drawSliders(accent);
            }

            Ui.popAlpha();
            Ui.popScissor();
         }
      }
   }

   private void drawGlobalRow(float mx, float my, float dt, int accent) {
      float gy = this.modeY() + 28.0F;
      ColorSetting g = ColorSetting.getGlobal();
      Ui.surface(this.x, gy, this.w, 34.0F, 8.0F, Theme.SURFACE);
      if (g != null) {
         drawSwatch(g, this.x + 8.0F, gy + 8.0F, 54.0F, 18.0F);
         Ui.textMiddle("Global Color", this.x + 70.0F, gy + 11.0F, 9.0F, Theme.TEXT, true);
         Ui.textMiddle(ModeSetting.displayName(g.getMode()), this.x + 70.0F, gy + 24.0F, 8.0F, Theme.TEXT_DIM, false);
      }

      float var10001 = this.x + this.w - 10.0F;
      String link = "Edit global color ›";
      float lw;
      float lx;
      boolean over = mx >= (lx = var10001 - (lw = Ui.widthHeavy("Edit global color ›", 8.8F))) - 4.0F && mx < this.x + this.w && my >= gy && my < gy + 34.0F;
      this.linkHover = Easing.approach(this.linkHover, over ? 1.0F : 0.0F, 16.0F, dt);
      Ui.textMiddleHeavy(link, lx, gy + 17.0F, 8.8F, this.linkHover > 0.5F ? Theme.ACCENT_HOVER : Theme.ACCENT);
      Ui.rect(lx, gy + 23.0F, lw * this.linkHover, 1.0F, accent | 0xFF000000);
   }

   private void drawPicker() {
      float py = this.pickerY();
      float svw = this.svW();
      Ui.svSquare(this.x, py, svw, 92.0F, 5.0F, this.hue);
      Ui.outline(this.x, py, svw, 92.0F, 5.0F, 0.8F, Theme.OUTLINE);
      float kx = this.x + this.sat * svw;
      float ky = py + (1.0F - this.val) * 92.0F;
      Ui.shadow(kx - 5.0F, ky - 5.0F, 10.0F, 10.0F, 5.0F, 2.0F, 1879048192);
      Ui.circle(kx, ky, 5.0F, -1);
      Ui.circle(kx, ky, 3.6F, this.current() | 0xFF000000);
      float hx = this.hueX();
      Ui.hueStrip(hx, py, 11.0F, 92.0F, 4.0F, true);
      this.marker(hx, py + this.hue * 92.0F, ColorUtil.hsv(this.hue, 1.0F, 1.0F));
      float ax = this.alphaX();
      Ui.checker(ax, py, 11.0F, 92.0F, 4.0F, 3.5F);
      int opaque = ColorUtil.hsv(this.hue, this.sat, this.val);
      Ui.gradientV(ax, py, 11.0F, 92.0F, 4.0F, opaque | 0xFF000000, opaque & 16777215);
      this.marker(ax, py + (1.0F - this.alpha) * 92.0F, this.current());
   }

   private void marker(float sx, float my, int color) {
      Ui.shadow(sx - 1.5F, my - 3.5F, 14.0F, 7.0F, 3.5F, 1.5F, 1879048192);
      Ui.roundRect(sx - 1.5F, my - 3.5F, 14.0F, 7.0F, 3.5F, -1);
      Ui.roundRect(sx + 0.5F, my - 1.8F, 10.0F, 3.6F, 1.8F, color | 0xFF000000);
   }

   private static void drawSwatch(ColorSetting s, float sx, float sy, float sw, float sh) {
      int c0 = s.getColor(0.0F);
      int c1 = s.getColor(1.0F);
      if (c0 >>> 24 < 255 || c1 >>> 24 < 255) {
         Ui.checker(sx, sy, sw, sh, 4.5F, 3.5F);
      }

      if (s.getEffectiveMode() == ColorSetting.Mode.STATIC) {
         Ui.roundRect(sx, sy, sw, sh, 4.5F, c0);
      } else {
         Ui.gradientH(sx, sy, sw, sh, 4.5F, c0, c1);
      }

      Ui.outline(sx, sy, sw, sh, 4.5F, 0.8F, Theme.OUTLINE_HOVER);
   }

   private void drawChips(float mx, float my, int accent) {
      float cy = this.chipsY();

      for (int i = 0; i < 2; i++) {
         float cx = this.x + i * 62.0F;
         boolean sel = this.editing == i;
         Ui.roundRect(cx, cy, 56.0F, 20.0F, 6.0F, sel ? Theme.ACCENT_SOFT : Theme.SURFACE);
         if (sel) {
            Ui.accentOutline(cx, cy, 56.0F, 20.0F, 6.0F, 1.0F, accent);
         }

         Ui.textMiddle(i == 0 ? "A" : "B", cx + 8.0F, cy + 10.0F, 9.0F, sel ? Theme.TEXT : Theme.TEXT_DIM, true);
         int c = i == 0 ? this.setting.getColorA() : this.setting.getColorB();
         Ui.checker(cx + 22.0F, cy + 4.0F, 28.0F, 12.0F, 3.5F, 3.0F);
         Ui.roundRect(cx + 22.0F, cy + 4.0F, 28.0F, 12.0F, 3.5F, c);
      }

      float bx = this.x + 124.0F;
      boolean over = mx >= bx && mx < bx + 20.0F && my >= cy && my < cy + 20.0F;
      Ui.roundRect(bx, cy, 20.0F, 20.0F, 6.0F, over ? Theme.SURFACE_HOVER : Theme.SURFACE);
      Ui.textMiddle("⇄", bx + 10.0F - Ui.width("⇄", 10.0F) / 2.0F, cy + 10.0F, 10.0F, Theme.TEXT_DIM, false);
   }

   private void drawBottomRow(int accent) {
      float by = this.bottomY();
      Ui.roundRect(this.x, by, 78.0F, 18.0F, 5.0F, Theme.SURFACE);
      Ui.outline(this.x, by, 78.0F, 18.0F, 5.0F, 0.9F, this.hexFocused ? accent | 0xFF000000 : Theme.OUTLINE);
      String text = this.hexFocused ? "#" + this.hexText : ColorUtil.toHex(this.current());
      Ui.textMiddle(text, this.x + 7.0F, by + 9.0F, 9.2F, this.hexFocused ? Theme.TEXT : Theme.TEXT_DIM, false);
      if (this.hexFocused && this.caret % 1.0F < 0.55F) {
         float cx = this.x + 7.0F + Ui.width(text, 9.2F) + 1.0F;
         Ui.rect(cx, by + 4.0F, 0.8F, 10.0F, Theme.TEXT);
      }

      Ui.checker(this.x + 84.0F, by, 26.0F, 18.0F, 5.0F, 3.5F);
      Ui.roundRect(this.x + 84.0F, by, 26.0F, 18.0F, 5.0F, this.current());
      Ui.outline(this.x + 84.0F, by, 26.0F, 18.0F, 5.0F, 0.8F, Theme.OUTLINE_HOVER);
      float pw;
      if ((this.gradient() || this.chroma()) && (pw = Math.min(90.0F, this.w - 124.0F)) > 20.0F) {
         drawSwatch(this.setting, this.x + this.w - pw, by, pw, 18.0F);
      }
   }

   private int sliderCount() {
      return this.gradient() ? 1 : (this.chroma() ? 4 : 0);
   }

   private float[] sliderBox(int i) {
      float sy = this.slidersY();
      if (this.gradient()) {
         return new float[]{this.x, sy, this.w};
      } else {
         float half = (this.w - 14.0F) / 2.0F;
         return new float[]{this.x + i % 2 * (half + 14.0F), sy + i / 2 * 26.0F, half};
      }
   }

   private int sliderKind(int i) {
      return this.gradient() ? 0 : new int[]{1, 2, 3, 4}[i];
   }

   private float sliderValue(int kind) {
      switch (kind) {
         case 0:
            return (float)(this.setting.getScrollSpeed() / 3.0);
         case 1:
            return (float)((this.setting.getChromaSpeed() - 0.1) / 2.9);
         case 2:
            return this.setting.getChromaSpread();
         case 3:
            return this.setting.getChromaSaturation();
         default:
            return this.setting.getChromaBrightness();
      }
   }

   private void setSliderValue(int kind, float f) {
      switch (kind) {
         case 0:
            this.setting.setScrollSpeed(f * 3.0);
            break;
         case 1:
            this.setting.setChromaSpeed(0.1 + f * 2.9);
            break;
         case 2:
            this.setting.setChromaSpread(f);
            break;
         case 3:
            this.setting.setChromaSaturation(f);
            break;
         default:
            this.setting.setChromaBrightness(f);
      }
   }

   private String sliderLabel(int kind) {
      switch (kind) {
         case 0:
            return this.setting.getScrollSpeed() <= 0.0 ? "Scroll  off" : String.format(Locale.ROOT, "Scroll  %.2fx", this.setting.getScrollSpeed());
         case 1:
            return String.format(Locale.ROOT, "Speed  %.2fx", this.setting.getChromaSpeed());
         case 2:
            return String.format(Locale.ROOT, "Spread  %d%%", Math.round(this.setting.getChromaSpread() * 100.0F));
         case 3:
            return String.format(Locale.ROOT, "Saturation  %d%%", Math.round(this.setting.getChromaSaturation() * 100.0F));
         default:
            return String.format(Locale.ROOT, "Brightness  %d%%", Math.round(this.setting.getChromaBrightness() * 100.0F));
      }
   }

   private void drawSliders(int accent) {
      for (int i = 0; i < this.sliderCount(); i++) {
         float[] b = this.sliderBox(i);
         int kind = this.sliderKind(i);
         float f = Easing.clamp01(this.sliderValue(kind));
         Ui.text(this.sliderLabel(kind), b[0], b[1] + 1.0F, 8.0F, Theme.TEXT_DIM);
         float ty = b[1] + 16.0F;
         Ui.roundRect(b[0], ty - 1.5F, b[2], 3.0F, 1.5F, Theme.TRACK);
         Ui.roundRect(b[0], ty - 1.5F, Math.max(3.0F, b[2] * f), 3.0F, 1.5F, accent);
         Ui.circle(b[0] + b[2] * f, ty, 4.0F, Theme.KNOB);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (this.compactRow()) {
         return this.rowMouseDown(mx, my, button);
      } else if (!this.contains(mx, my)) {
         if (this.hexFocused) {
            this.commitHex();
         }

         return false;
      } else if (my < this.y + 26.0F) {
         if (button == 0 && !this.alwaysExpanded) {
            this.expanded = !this.expanded;
            ModSounds.gui(this.expanded ? Sfx.DROPDOWN : Sfx.CLICK);
            if (this.hexFocused) {
               this.commitHex();
            }
         } else if (button == 1) {
            this.setting.reset();
            this.hasSync = false;
            ModSounds.gui(Sfx.CLICK);
         }

         return true;
      } else if (!(this.expand < 0.95F) && button == 0) {
         int m = Segmented.hit(this.x, this.modeY(), this.w, 20.0F, this.modeLabels.length, mx, my);
         if (m >= 0) {
            this.setting.setMode(this.modes[m]);
            ModSounds.gui(Sfx.CLICK);
            return true;
         } else if (this.onGlobal()) {
            float gy = this.modeY() + 28.0F;
            if (my >= gy && my < gy + 34.0F && globalEditor != null) {
               globalEditor.run();
            }

            return true;
         } else {
            float cy;
            if (this.gradient() && my >= (cy = this.chipsY()) && my < cy + 20.0F) {
               if (mx >= this.x && mx < this.x + 56.0F) {
                  this.editing = 0;
                  this.hasSync = false;
                  ModSounds.gui(Sfx.CLICK);
               } else if (mx >= this.x + 62.0F && mx < this.x + 118.0F) {
                  this.editing = 1;
                  this.hasSync = false;
                  ModSounds.gui(Sfx.CLICK);
               } else if (mx >= this.x + 124.0F && mx < this.x + 144.0F) {
                  int a = this.setting.getColorA();
                  this.setting.setColorA(this.setting.getColorB());
                  this.setting.setColorB(a);
                  this.hasSync = false;
                  ModSounds.gui(Sfx.CLICK);
               }

               return true;
            } else {
               float py = this.pickerY();
               if (my >= py && my < py + 92.0F) {
                  if (mx >= this.x && mx < this.x + this.svW()) {
                     this.drag = 1;
                  } else if (mx >= this.hueX() - 2.0F && mx < this.hueX() + 11.0F + 2.0F) {
                     this.drag = 2;
                  } else if (mx >= this.alphaX() - 2.0F && mx < this.alphaX() + 11.0F + 2.0F) {
                     this.drag = 3;
                  }

                  if (this.drag != 0) {
                     this.mouseDrag(mx, my);
                  }

                  return true;
               } else {
                  float by = this.bottomY();
                  if (my >= by - 2.0F && my < by + 20.0F) {
                     if (mx >= this.x && mx < this.x + 78.0F) {
                        this.hexFocused = true;
                        this.hexText = ColorUtil.toHex(this.current()).substring(1);
                        this.caret = 0.0F;
                        ModSounds.gui(Sfx.CLICK);
                     }

                     return true;
                  } else {
                     for (int i = 0; i < this.sliderCount(); i++) {
                        float[] b = this.sliderBox(i);
                        if (mx >= b[0] - 4.0F && mx < b[0] + b[2] + 4.0F && my >= b[1] && my < b[1] + 26.0F) {
                           this.drag = 4;
                           this.dragSlider = this.sliderKind(i);
                           this.mouseDrag(mx, my);
                           return true;
                        }
                     }

                     return true;
                  }
               }
            }
         }
      } else {
         return true;
      }
   }

   @Override
   public void mouseDrag(float mx, float my) {
      float py = this.pickerY();
      switch (this.drag) {
         case 1:
            float beforexx = this.val + this.sat;
            this.sat = Easing.clamp01((mx - this.x) / this.svW());
            this.val = 1.0F - Easing.clamp01((my - py) / 92.0F);
            this.apply();
            if (this.val + this.sat != beforexx) {
               ModSounds.sliderTick(this.val);
            }
            break;
         case 2:
            float beforex = this.hue;
            this.hue = Math.min(0.9999F, Easing.clamp01((my - py) / 92.0F));
            this.apply();
            if (this.hue != beforex) {
               ModSounds.sliderTick(1.0F - this.hue);
            }
            break;
         case 3:
            float before = this.alpha;
            this.alpha = 1.0F - Easing.clamp01((my - py) / 92.0F);
            this.apply();
            if (this.alpha != before) {
               ModSounds.sliderTick(this.alpha);
            }
            break;
         case 4:
            for (int i = 0; i < this.sliderCount(); i++) {
               if (this.sliderKind(i) == this.dragSlider) {
                  float[] b = this.sliderBox(i);
                  float beforexxx = this.sliderValue(this.dragSlider);
                  this.setSliderValue(this.dragSlider, Easing.clamp01((mx - b[0]) / b[2]));
                  if (this.sliderValue(this.dragSlider) != beforexxx) {
                     ModSounds.sliderTick(this.sliderValue(this.dragSlider));
                  }
               }
            }
      }
   }

   @Override
   public void mouseUp(float mx, float my) {
      if (this.drag != 0) {
         ModSounds.gui(Sfx.SLIDER_RELEASE);
      }

      this.drag = 0;
   }

   @Override
   public boolean mouseWheel(float mx, float my, int wheel) {
      if (!this.compactRow() && !(this.expand < 0.95F) && !this.onGlobal() && wheel != 0) {
         float py = this.pickerY();
         if (!(my < py) && !(my >= py + 92.0F)) {
            this.sync();
            float step = Math.signum((float)wheel) * (GuiScreen.isShiftKeyDown() ? 0.25F : 1.0F) / 60.0F;
            if (mx >= this.hueX() - 2.0F && mx < this.hueX() + 11.0F + 2.0F) {
               float before = this.hue;
               this.hue = Math.min(0.9999F, Easing.clamp01(this.hue - step));
               this.apply();
               if (this.hue != before) {
                  ModSounds.sliderTick(1.0F - this.hue);
               }

               return true;
            } else if (mx >= this.alphaX() - 2.0F && mx < this.alphaX() + 11.0F + 2.0F) {
               float before = this.alpha;
               this.alpha = Easing.clamp01(this.alpha + step);
               this.apply();
               if (this.alpha != before) {
                  ModSounds.sliderTick(this.alpha);
               }

               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public float[] hueStripForTest() {
      return !this.compactRow() && !(this.expand < 0.95F) && !this.onGlobal() ? new float[]{this.hueX() + 5.5F, this.pickerY() + 46.0F, this.hue} : null;
   }

   @Override
   public boolean isDragging() {
      return this.drag != 0;
   }

   @Override
   public boolean keyTyped(char c, int key) {
      if (!this.hexFocused) {
         return false;
      } else {
         if (key == 1) {
            this.hexFocused = false;
         } else if (key == 28 || key == 156) {
            this.commitHex();
         } else if (key == 14) {
            if (!this.hexText.isEmpty()) {
               this.hexText = this.hexText.substring(0, this.hexText.length() - 1);
            }
         } else if (GuiScreen.isKeyComboCtrlV(key)) {
            this.append(GuiScreen.getClipboardString());
         } else if (GuiScreen.isKeyComboCtrlA(key)) {
            this.hexText = "";
         } else {
            this.append(String.valueOf(c));
         }

         if (key != 1 && key != 28 && key != 156) {
            ModSounds.key();
         }

         this.caret = 0.0F;
         return true;
      }
   }

   private void append(String s) {
      if (s != null) {
         for (char ch : s.toCharArray()) {
            if (Character.digit(ch, 16) >= 0 && this.hexText.length() < 6) {
               this.hexText = this.hexText + Character.toUpperCase(ch);
            }
         }
      }
   }

   private void commitHex() {
      this.hexFocused = false;
      if (this.hexText.length() == 6 || this.hexText.length() == 3) {
         int parsed = ColorUtil.parseHex(this.hexText, 0);
         float[] hsv = ColorUtil.toHsv(parsed, new float[3]);
         if (hsv[1] > 0.001F && hsv[2] > 0.001F) {
            this.hue = hsv[0];
         }

         this.sat = hsv[1];
         this.val = hsv[2];
         this.apply();
      }
   }

   @Override
   public boolean isFocused() {
      return this.hexFocused;
   }

   @Override
   public void unfocus() {
      if (this.hexFocused) {
         this.commitHex();
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (this.compactRow()) {
         return this.rowTooltip(mx, my);
      } else if (this.inPopover) {
         return null;
      } else {
         return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + 26.0F && !this.expanded
            ? this.setting.getDescription() + "  (click to edit, right-click for more)"
            : null;
      }
   }
}
