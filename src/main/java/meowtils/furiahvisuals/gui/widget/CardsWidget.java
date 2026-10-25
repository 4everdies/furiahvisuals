package meowtils.furiahvisuals.gui.widget;

import java.util.Arrays;
import java.util.Set;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.search.SearchList;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

public final class CardsWidget extends Widget implements SearchList {
   private static final float GAP = 6.0F;
   private final Module module;
   private final ModeSetting<?> setting;
   private final Enum<?>[] values;
   private float[] cardHover;
   private float selX = -1.0F;
   private float selY = -1.0F;
   private int[] shown;

   public CardsWidget(Module module, ModeSetting<?> setting) {
      this.module = module;
      this.setting = setting;
      this.values = setting.getValues();
      this.cardHover = new float[this.values.length];
   }

   private String seenKey(Enum<?> value) {
      return "card." + this.module.getId() + "." + value.name();
   }

   private boolean isNew(Enum<?> value) {
      return value instanceof Displayable && ((Displayable)value).isNew() && !MenuMemory.seen(this.seenKey(value));
   }

   private static float textH() {
      return Density.textCardH(38.0F);
   }

   private float artH() {
      return Density.dense() ? 0.0F : Math.max(0.0F, this.module.optionArtHeight(this.setting));
   }

   private float cardH() {
      float a = this.artH();
      return textH() + (a > 0.0F ? a + 4.0F : 0.0F);
   }

   private static float gap() {
      return Density.dense() ? 3.0F : (Density.compact() ? 5.0F : 6.0F);
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
      return (this.w - gap() * (cols - 1)) / cols;
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
      return rows == 0 ? 0.0F : rows * this.cardH() + (rows - 1) * gap() + 4.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      int cols = this.columns();
      float cw = this.cardW();
      float ch = this.cardH();
      float g = gap();
      int selectedSlot = this.slotOf(this.setting.getIndex());
      if (selectedSlot >= 0) {
         float targetX = this.x + selectedSlot % cols * (cw + g);
         float targetY = selectedSlot / cols * (ch + g);
         this.selX = this.selX < 0.0F ? targetX : Easing.approach(this.selX, targetX, 18.0F, dt);
         this.selY = this.selY < 0.0F ? targetY : Easing.approach(this.selY, targetY, 18.0F, dt);
      }

      double time = ClientClock.seconds();
      float radius = Density.dense() ? 5.0F : 7.0F;
      Enum<?> hovered = null;

      for (int s = 0; s < this.count(); s++) {
         int i = this.valueAt(s);
         float cx = this.x + s % cols * (cw + g);
         float cy = this.y + s / cols * (ch + g);
         boolean over = mx >= cx && mx < cx + cw && my >= cy && my < cy + ch;
         if (over) {
            hovered = this.values[i];
         }

         this.cardHover[i] = Easing.approach(this.cardHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
         Ui.surface(cx, cy, cw, ch, radius, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.cardHover[i]));
      }

      this.module.onOptionHover(this.setting, hovered);
      if (selectedSlot >= 0) {
         float hy = this.y + this.selY;
         Ui.glow(this.selX + cw / 2.0F, hy + ch / 2.0F, cw * 1.25F, ColorUtil.withAlpha(accent, 0.16F));
         Ui.roundRect(this.selX, hy, cw, ch, radius, Theme.ACCENT_SOFT);
         Ui.accentOutline(this.selX, hy, cw, ch, radius, 1.2F, accent);
      }

      int selected;
      if (this.isNew(this.values[selected = this.setting.getIndex()])) {
         MenuMemory.markSeen(this.seenKey(this.values[selected]));
      }

      boolean dense = Density.dense();
      boolean compact = Density.compact();
      float nameSize = dense ? 7.8F : (compact ? 8.6F : 9.8F);
      float descSize = compact ? 7.0F : 8.0F;

      for (int s = 0; s < this.count(); s++) {
         int i = this.valueAt(s);
         float cx = this.x + s % cols * (cw + g);
         float cy = this.y + s / cols * (ch + g);
         boolean isSelected = i == selected;
         int dot = this.module.getOptionColor(this.setting, this.values[i], time);
         String tag = this.values[i] instanceof Displayable ? ((Displayable)this.values[i]).tag() : null;
         float textX = cx + (dense ? 7.0F : (compact ? 8.0F : 10.0F));
         float nameY = dense ? cy + (ch - nameSize) / 2.0F - 0.5F : (compact ? cy + 4.5F : cy + 6.5F);
         float midY = nameY + nameSize / 2.0F + 0.5F;
         float right = cx + cw - (dense ? 5.0F : 7.0F);
         if (this.isNew(this.values[i])) {
            float ts = !dense && !compact ? 6.0F : 5.4F;
            float pw = Ui.widthBold("NEW", ts) + 8.0F;
            float ph = !dense && !compact ? 11.0F : 9.5F;
            float var47;
            Ui.roundRect(var47 = right - pw, midY - ph / 2.0F, pw, ph, ph / 2.0F, ColorUtil.withAlpha(accent, 0.92F));
            Ui.textMiddle("NEW", var47 + 4.0F, midY, ts, -1, true);
            right = var47 - 4.0F;
         }

         if (tag != null && !dense) {
            float ts = compact ? 5.4F : 6.0F;
            float pw = Ui.widthBold(tag, ts) + 8.0F;
            float ph = compact ? 9.5F : 11.0F;
            float var48;
            Ui.roundRect(var48 = right - pw, midY - ph / 2.0F, pw, ph, ph / 2.0F, isSelected ? Theme.ACCENT_SOFT : Theme.PILL);
            Ui.textMiddle(tag, var48 + 4.0F, midY, ts, isSelected ? accent : Theme.TEXT_MUTED, true);
            right = var48 - 5.0F;
         }

         if (dot != 0) {
            float dx = right - 4.0F;
            Ui.glow(dx, midY, dense ? 12.0F : 16.0F, ColorUtil.withAlpha(dot, isSelected ? 0.6F : 0.3F));
            Ui.circle(dx, midY, dense ? 2.4F : 3.0F, dot | 0xFF000000);
            right -= 10.0F;
         }

         float maxW = right - textX - 2.0F;
         Ui.textBold(
            Ui.ellipsize(ModeSetting.displayName(this.values[i]), nameSize, maxW, true),
            textX,
            nameY,
            nameSize,
            isSelected ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, 0.35F + 0.65F * this.cardHover[i])
         );
         if (!dense) {
            float dy = compact ? cy + 15.0F : cy + 21.0F;
            Ui.text(
               Ui.ellipsize(ModeSetting.description(this.values[i]), descSize, cw - (textX - cx) * 2.0F, false),
               textX,
               dy,
               descSize,
               isSelected ? ColorUtil.lerp(Theme.TEXT_DIM, accent, 0.45F) : Theme.TEXT_MUTED
            );
         }

         float art;
         if ((art = this.artH()) > 0.0F) {
            float pad = textX - cx;
            this.module.drawOptionArt(this.setting, this.values[i], textX, cy + textH() - 2.0F, cw - pad * 2.0F, art, time, isSelected, accent);
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         int s = this.slotAt(mx, my);
         if (s >= 0) {
            this.setting.setIndex(this.valueAt(s));
            ModSounds.gui(Sfx.SELECT);
         }

         return true;
      } else {
         return false;
      }
   }

   private int slotAt(float mx, float my) {
      int cols = this.columns();
      float cw = this.cardW();
      float ch = this.cardH();
      float g = gap();

      for (int s = 0; s < this.count(); s++) {
         float cx = this.x + s % cols * (cw + g);
         float cy = this.y + s / cols * (ch + g);
         if (mx >= cx && mx < cx + cw && my >= cy && my < cy + ch) {
            return s;
         }
      }

      return -1;
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
            int i = this.valueAt(s);
            String tag = this.values[i] instanceof Displayable ? ((Displayable)this.values[i]).tag() : null;
            String d = ModeSetting.description(this.values[i]);
            if (Density.dense()) {
               return d.isEmpty() ? null : d + (tag != null ? "  (" + tag + ")" : "");
            } else if ("FADE".equals(tag)) {
               return "Fade: shows at full strength, then fades quickly (Duration slider)";
            } else if ("ANIM".equals(tag)) {
               return "Animation: always plays its whole animation (Speed slider)";
            } else {
               return Ui.width(d, Density.compact() ? 7.0F : 8.0F) > this.cardW() - 16.0F ? d : null;
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
            return slot < 0 ? -1.0F : slot / this.columns() * (this.cardH() + gap());
         }
      }

      return -1.0F;
   }

   @Override
   public float revealHeight(Object key) {
      return this.cardH();
   }

   @Override
   public int searchVersion() {
      return 0;
   }
}
