package meowtils.furiahvisuals.module.ui.crosshair;

import java.util.Arrays;
import java.util.Set;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.search.SearchList;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

final class CrosshairCardsWidget extends Widget implements SearchList {
   private static final float GAP = 6.0F;
   private static final float THUMB_PAD = 4.0F;
   private final CrosshairElement element;
   private final CrosshairStyleSetting setting;
   private final CrosshairStyle[] values = CrosshairStyle.values();
   private final float[] cardHover = new float[this.values.length];
   private final CrosshairState demo = new CrosshairState();
   private float selX = -1.0F;
   private float selY = -1.0F;
   private int[] shown;

   private static float cardH() {
      return Density.textCardH(42.0F);
   }

   CrosshairCardsWidget(CrosshairElement element, CrosshairStyleSetting setting) {
      this.element = element;
      this.setting = setting;
   }

   private int columns() {
      switch (Density.get()) {
         case COMPACT:
            return this.w >= 420.0F ? 4 : (this.w >= 240.0F ? 3 : 2);
         case DENSE:
            return this.w >= 420.0F ? 4 : (this.w >= 280.0F ? 3 : 2);
         default:
            return this.w >= 330.0F ? 3 : 2;
      }
   }

   private float cardW() {
      int cols = this.columns();
      return (this.w - 6.0F * (cols - 1)) / cols;
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

   @Override
   public float height() {
      int rows = (this.count() + this.columns() - 1) / this.columns();
      return rows == 0 ? 0.0F : rows * cardH() + (rows - 1) * 6.0F + 4.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      int cols = this.columns();
      float cw = this.cardW();
      int selectedSlot = this.slotOf(this.setting.getIndex());
      if (selectedSlot >= 0) {
         float tx = this.x + selectedSlot % cols * (cw + 6.0F);
         float ty = selectedSlot / cols * (cardH() + 6.0F);
         this.selX = this.selX < 0.0F ? tx : Easing.approach(this.selX, tx, 18.0F, dt);
         this.selY = this.selY < 0.0F ? ty : Easing.approach(this.selY, ty, 18.0F, dt);
      }

      for (int s = 0; s < this.count(); s++) {
         int i = this.valueAt(s);
         float cx = this.x + s % cols * (cw + 6.0F);
         float cy = this.y + s / cols * (cardH() + 6.0F);
         boolean over = mx >= cx && mx < cx + cw && my >= cy && my < cy + cardH();
         this.cardHover[i] = Easing.approach(this.cardHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
         Ui.surface(cx, cy, cw, cardH(), 7.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.cardHover[i]));
      }

      if (selectedSlot >= 0) {
         float hy = this.y + this.selY;
         Ui.glow(this.selX + cw / 2.0F, hy + cardH() / 2.0F, cw * 1.25F, ColorUtil.withAlpha(accent, 0.16F));
         Ui.roundRect(this.selX, hy, cw, cardH(), 7.0F, Theme.ACCENT_SOFT);
         Ui.accentOutline(this.selX, hy, cw, cardH(), 7.0F, 1.1F, accent);
      }

      int selected = this.setting.getIndex();
      float thumb = cardH() - 8.0F;

      for (int s = 0; s < this.count(); s++) {
         int i = this.valueAt(s);
         float cx = this.x + s % cols * (cw + 6.0F);
         float cy = this.y + s / cols * (cardH() + 6.0F);
         boolean isSelected = i == selected;
         float tx = cx + 4.0F;
         float ty = cy + 4.0F;
         Ui.roundRect(tx, ty, thumb, thumb, 6.0F, -15395042);
         if (!Ui.clippedOut(tx, ty, thumb, thumb)) {
            this.drawThumb(this.values[i], tx + thumb / 2.0F, ty + thumb / 2.0F, thumb);
         }

         float textX = tx + thumb + 7.0F;
         float maxW = cx + cw - textX - 6.0F;
         boolean dense = Density.dense();
         boolean compact = Density.compact();
         float ns = dense ? 7.6F : (compact ? 8.4F : 9.4F);
         float nameY = dense ? cy + (cardH() - ns) / 2.0F - 0.5F : (compact ? cy + 5.0F : cy + 7.0F);
         Ui.textBold(
            Ui.ellipsize(this.values[i].displayName(), ns, maxW, true),
            textX,
            nameY,
            ns,
            isSelected ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, 0.35F + 0.65F * this.cardHover[i])
         );
         if (!dense) {
            float ds = compact ? 6.8F : 7.4F;
            Ui.text(
               Ui.ellipsize(this.values[i].description(), ds, maxW, false),
               textX,
               compact ? cy + 16.0F : cy + 22.0F,
               ds,
               isSelected ? ColorUtil.lerp(Theme.TEXT_DIM, accent, 0.45F) : Theme.TEXT_MUTED
            );
         }
      }
   }

   private void drawThumb(CrosshairStyle style, float cx, float cy, float size) {
      CrosshairDef def = this.element.editor;
      float px = Ui.px;
      float room = size * px * 0.42F;
      MenuPixels.begin(cx, cy);

      try {
         if (style == CrosshairStyle.VANILLA) {
            float sf = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
            float s = Math.min(sf, room * 2.0F / 16.0F);
            CrosshairRenderer.drawVanilla(-s, -s, s, 1.0F);
         } else {
            this.demo.time = (float)ColorSetting.clock();
            float reach = Math.max(2.0F, CrosshairRenderer.extent(def, style) + (def.glow.get() ? 5.0F : 1.5F));
            this.demo.scale = Math.min(3.0F, room / reach);
            CrosshairRenderer.draw(def, style, this.demo, this.element.reactions(), 0.0F, 0.0F, 1.0F, 1.0F);
         }
      } finally {
         MenuPixels.end();
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         int cols = this.columns();
         float cw = this.cardW();

         for (int s = 0; s < this.count(); s++) {
            float cx = this.x + s % cols * (cw + 6.0F);
            float cy = this.y + s / cols * (cardH() + 6.0F);
            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + cardH()) {
               this.setting.setIndex(this.valueAt(s));
               ModSounds.gui(Sfx.SELECT);
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
         int cols = this.columns();
         float cw = this.cardW();

         for (int s = 0; s < this.count(); s++) {
            int i = this.valueAt(s);
            float cx = this.x + s % cols * (cw + 6.0F);
            float cy = this.y + s / cols * (cardH() + 6.0F);
            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + cardH()) {
               String d = this.values[i].description();
               return !Density.dense() && !(Ui.width(d, 7.4F) > cw - cardH() - 10.0F) ? null : d;
            }
         }

         return null;
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
            return slot < 0 ? -1.0F : slot / this.columns() * (cardH() + 6.0F);
         }
      }

      return -1.0F;
   }

   @Override
   public float revealHeight(Object key) {
      return cardH();
   }

   @Override
   public int searchVersion() {
      return 0;
   }
}
