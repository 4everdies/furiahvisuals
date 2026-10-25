package meowtils.furiahvisuals.module.watermark;

final class RollingText {
   private static final int MAX = 24;
   private static final float ROLL_SECONDS = 0.24F;
   private static final char NONE = '\u0000';
   private final char[] cur = new char[24];
   private final char[] prev = new char[24];
   private final float[] t = new float[24];
   private final boolean[] up = new boolean[24];
   private int len;
   private int prevLen;
   private String text = "";
   private double number = Double.NaN;

   void set(String s, double value, boolean animate) {
      if (!s.equals(this.text)) {
         boolean increase = Double.isNaN(value) || Double.isNaN(this.number) || value >= this.number;
         int newLen = Math.min(24, s.length());
         int span = Math.max(this.len, newLen);

         for (int j = 0; j < span; j++) {
            char oldC = j < this.len ? this.cur[j] : 0;
            char newC = j < newLen ? s.charAt(s.length() - 1 - j) : 0;
            if (oldC != newC) {
               this.prev[j] = oldC;
               this.cur[j] = newC;
               this.t[j] = animate && !this.text.isEmpty() ? 0.0F : 1.0F;
               this.up[j] = increase;
            }
         }

         this.prevLen = this.len;
         this.len = newLen;
         this.text = s;
         this.number = value;
      }
   }

   String get() {
      return this.text;
   }

   void update(float dt) {
      int span = Math.max(this.len, this.prevLen);
      boolean rolling = false;

      for (int j = 0; j < span; j++) {
         if (this.t[j] < 1.0F) {
            this.t[j] = Math.min(1.0F, this.t[j] + dt / 0.24F);
            rolling |= this.t[j] < 1.0F;
         }
      }

      if (!rolling) {
         this.prevLen = this.len;
      }
   }

   private static float charWidth(IslandDraw d, boolean bold, float size, char c) {
      return c >= '0' && c <= '9' ? d.digitWidth(bold, size) : d.charWidth(bold, size, c);
   }

   float width(IslandDraw d, boolean bold, float size) {
      float w = 0.0F;

      for (int j = 0; j < this.len; j++) {
         w += charWidth(d, bold, size, this.cur[j]);
      }

      return w;
   }

   void draw(IslandDraw d, boolean bold, float size, float x, float cy, int color) {
      this.drawBlinking(d, bold, size, x, cy, color, 1.0F);
   }

   void drawBlinking(IslandDraw d, boolean bold, float size, float x, float cy, int color, float colonAlpha) {
      float lh = d.lineHeight(bold, size);
      float top = d.middleTop(bold, size, cy);
      float right = x + this.width(d, bold, size);
      int span = Math.max(this.len, this.prevLen);
      float slotRight = right;
      int alpha = color >>> 24;
      int rgb = color & 16777215;

      for (int j = 0; j < span; j++) {
         char c = this.cur[j];
         char p = this.prev[j];
         float w = j < this.len ? charWidth(d, bold, size, c) : charWidth(d, bold, size, p == 0 ? '0' : p);
         float sx = slotRight - w;
         float k = this.t[j];
         if (k >= 1.0F) {
            if (c != 0) {
               int cc = c == ':' && colonAlpha < 1.0F ? (int)(alpha * colonAlpha) << 24 | rgb : color;
               d.glyph(bold, size, c, sx, top, cc);
            }
         } else {
            float e = 1.0F - (1.0F - k) * (1.0F - k) * (1.0F - k);
            float dir = this.up[j] ? -1.0F : 1.0F;
            if (p != 0) {
               int a = (int)(alpha * (1.0F - e));
               float pw = charWidth(d, bold, size, p);
               d.glyphClipped(bold, size, p, slotRight - pw, top + dir * e * lh * 0.9F, a << 24 | rgb, top, top + lh);
            }

            if (c != 0) {
               int a = (int)(alpha * e);
               d.glyphClipped(bold, size, c, sx, top - dir * (1.0F - e) * lh * 0.9F, a << 24 | rgb, top, top + lh);
            }
         }

         slotRight -= w;
      }
   }

   boolean isRolling() {
      int span = Math.max(this.len, this.prevLen);

      for (int j = 0; j < span; j++) {
         if (this.t[j] < 1.0F) {
            return true;
         }
      }

      return false;
   }
}
