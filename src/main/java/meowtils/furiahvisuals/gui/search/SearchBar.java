package meowtils.furiahvisuals.gui.search;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import net.minecraft.client.gui.GuiScreen;

public final class SearchBar {
   public static final float H = 24.0F;
   private static final float SIZE = 9.4F;
   private static final float TEXT_X = 25.0F;
   private final TextEdit edit = new TextEdit(64);
   private boolean focused;
   private float focus;
   private float hover;
   private float clearHover;
   private float caretTime;
   private float scroll;
   private float x;
   private float y;
   private float w;
   private float h = 24.0F;
   private float size = 9.4F;
   private float textX = 25.0F;
   private float textRight;
   private boolean selecting;
   private long lastClickMs;
   public static final int NONE = 0;
   public static final int FOCUSED = 1;
   public static final int CLEARED = 2;

   public String text() {
      return this.edit.text();
   }

   public void setText(String s) {
      this.edit.set(s);
      this.scroll = 0.0F;
      this.caretTime = 0.0F;
   }

   public boolean isFocused() {
      return this.focused;
   }

   public void focus() {
      this.focused = true;
      this.caretTime = 0.0F;
   }

   public void focusAll() {
      this.focus();
      this.edit.selectAll();
   }

   public void unfocus() {
      this.focused = false;
      this.selecting = false;
      this.edit.moveTo(this.edit.caret(), false);
   }

   public boolean contains(float mx, float my) {
      return mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + this.h;
   }

   private boolean onClear(float mx, float my) {
      float k = this.h / 24.0F;
      float cx = this.x + this.w - 12.0F * k;
      float cy = this.y + this.h / 2.0F;
      return !this.edit.text().isEmpty() && Math.abs(mx - cx) <= 9.0F * k && Math.abs(my - cy) <= 9.0F * k;
   }

   public void render(float x, float y, float w, float mx, float my, float dt, int accent, String placeholder, String count) {
      this.render(x, y, w, 24.0F, mx, my, dt, accent, placeholder, count);
   }

   public void render(float x, float y, float w, float h, float mx, float my, float dt, int accent, String placeholder, String count) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      float k = h / 24.0F;
      this.size = Math.min(9.4F, Math.max(7.2F, 9.4F * (0.35F + 0.65F * k)));
      this.textX = 25.0F * k;
      this.caretTime += dt;
      boolean over = this.contains(mx, my);
      this.hover = Easing.approach(this.hover, over ? 1.0F : 0.0F, 16.0F, dt);
      this.focus = Easing.approach(this.focus, this.focused ? 1.0F : 0.0F, 16.0F, dt);
      this.clearHover = Easing.approach(this.clearHover, this.onClear(mx, my) ? 1.0F : 0.0F, 18.0F, dt);
      float r = h / 2.0F;
      Ui.registerControl(x, y, w, h, "search box " + placeholder);
      Ui.surface(x, y, w, h, r, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, Math.max(this.hover * 0.7F, this.focus * 0.5F)));
      if (this.focus > 0.01F) {
         Ui.glow(x + w * 0.5F, y + h / 2.0F, w * 0.9F, ColorUtil.withAlpha(accent, 0.06F * this.focus));
      }

      if (this.focus > 0.99F) {
         Ui.accentOutline(x, y, w, h, r, 1.0F, accent);
      } else {
         Ui.outline(x, y, w, h, r, this.focus > 0.5F ? 1.0F : 0.9F, ColorUtil.lerp(Theme.OUTLINE, accent | 0xFF000000, this.focus));
      }

      float cx = x + 13.0F * k;
      float cy = y + h / 2.0F - 0.8F * k;
      float ring = 4.0F * k;
      int iconColor = ColorUtil.lerp(Theme.TEXT_MUTED, accent, Math.max(this.focus, this.edit.text().isEmpty() ? 0.0F : 0.6F));
      Ui.outline(cx - ring, cy - ring, ring * 2.0F, ring * 2.0F, ring, 1.3F * Math.min(1.0F, k + 0.1F), iconColor);

      for (int i = 0; i <= 4; i++) {
         float t = i / 4.0F;
         Ui.circle(cx + ring * 0.8F + t * 2.6F * k, cy + ring * 0.8F + t * 2.6F * k, 0.9F * Math.min(1.0F, k + 0.1F), iconColor);
      }

      float right = x + w - 8.0F * k;
      String text = this.edit.text();
      if (!text.isEmpty()) {
         float bx = x + w - 12.0F * k;
         float by = y + h / 2.0F;
         Ui.circle(bx, by, 6.6F * k, ColorUtil.lerp(ColorUtil.withAlpha(Theme.TEXT_MUTED, 0.25F), Theme.ACCENT_SOFT_HOVER, this.clearHover));
         int xc = ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.clearHover);

         for (int i = -3; i <= 3; i++) {
            float d = i * 0.85F * k;
            Ui.circle(bx + d, by + d, 0.62F, xc);
            Ui.circle(bx + d, by - d, 0.62F, xc);
         }

         right = bx - 11.0F * k;
      }

      if (count != null) {
         float cs = Math.min(8.0F, this.size - 1.0F);
         float cw = Ui.width(count, cs);
         Ui.textMiddle(count, right - cw, y + h / 2.0F, cs, ColorUtil.lerp(Theme.TEXT_MUTED, accent, 0.35F), false);
         right -= cw + 10.0F * k;
      }

      this.textRight = right;
      float tx = x + this.textX;
      float room = Math.max(10.0F, this.textRight - tx);
      if (text.isEmpty()) {
         this.scroll = 0.0F;
         Ui.textMiddle(
            Ui.ellipsize(placeholder, this.size, room, false),
            tx,
            y + h / 2.0F,
            this.size,
            ColorUtil.withAlpha(Theme.TEXT_MUTED, this.focused ? 0.75F : 1.0F),
            false
         );
      } else {
         float caretX = Ui.width(text.substring(0, this.edit.caret()), this.size);
         if (caretX - this.scroll > room - 2.0F) {
            this.scroll = caretX - room + 2.0F;
         } else if (caretX - this.scroll < 0.0F) {
            this.scroll = caretX;
         }

         this.scroll = Math.max(0.0F, Math.min(this.scroll, Math.max(0.0F, Ui.width(text, this.size) - room + 2.0F)));
      }

      Ui.pushScissor(tx - 1.0F, y, room + 2.0F, h);
      float pad = Math.max(3.0F, h * 0.21F);
      if (!text.isEmpty()) {
         if (this.focused && this.edit.hasSelection()) {
            float sa = Ui.width(text.substring(0, this.edit.selectionStart()), this.size) - this.scroll;
            float sb = Ui.width(text.substring(0, this.edit.selectionEnd()), this.size) - this.scroll;
            Ui.roundRect(tx + sa - 0.5F, y + pad, sb - sa + 1.0F, h - pad * 2.0F, 2.0F, Theme.ACCENT_SELECTION);
         }

         Ui.textMiddle(text, tx - this.scroll, y + h / 2.0F, this.size, Theme.TEXT, false);
      }

      if (this.focused && this.caretTime % 1.06F < 0.6F) {
         float caretX = text.isEmpty() ? 0.0F : Ui.width(text.substring(0, this.edit.caret()), this.size) - this.scroll;
         Ui.rect(tx + caretX, y + pad + 1.0F, 0.8F, h - pad * 2.0F - 2.0F, Theme.TEXT);
      }

      Ui.popScissor();
   }

   private int indexAt(float mx) {
      String text = this.edit.text();
      float local = mx - (this.x + this.textX) + this.scroll;
      int best = 0;
      float bestD = Math.abs(local);

      for (int i = 1; i <= text.length(); i++) {
         float d = Math.abs(local - Ui.width(text.substring(0, i), this.size));
         if (d < bestD) {
            bestD = d;
            best = i;
         }
      }

      return best;
   }

   public int mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         return 0;
      } else if (this.onClear(mx, my) && button == 0) {
         this.setText("");
         this.focus();
         return 2;
      } else if (button == 1) {
         this.setText("");
         this.focus();
         return 2;
      } else {
         long now = System.currentTimeMillis();
         int index = this.indexAt(mx);
         if (this.focused && now - this.lastClickMs < 350L) {
            this.edit.selectWord(index);
            this.lastClickMs = 0L;
         } else {
            this.edit.moveTo(index, this.focused && GuiScreen.isShiftKeyDown());
            this.lastClickMs = now;
            this.selecting = true;
         }

         this.focus();
         return 1;
      }
   }

   public void mouseDrag(float mx, float my) {
      if (this.selecting && this.focused) {
         this.edit.moveTo(this.indexAt(mx), true);
      }
   }

   public void mouseUp() {
      this.selecting = false;
   }

   public int key(char c, int key) {
      this.caretTime = 0.0F;
      return this.edit.key(c, key);
   }

   public void type(String s) {
      this.edit.insert(s);
      this.caretTime = 0.0F;
   }
}
