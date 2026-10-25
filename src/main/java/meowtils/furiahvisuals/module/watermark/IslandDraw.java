package meowtils.furiahvisuals.module.watermark;

import meowtils.furiahvisuals.gui.SmoothFont;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.UiProbe;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

final class IslandDraw {
   private static final int TEXT_SLOTS = 6;
   private static final int WINDOWS = 4;
   float ox;
   float oy;
   float u = 1.0F;
   private float textU = 1.0F;
   private float scale = 1.0F;
   private float pivotX;
   private float pivotY;
   private float alpha = 1.0F;
   private float outer = 1.0F;
   private final float[] winX0 = new float[4];
   private final float[] winX1 = new float[4];
   private final float[] winFade = new float[4];
   private int windows;
   private final WorldVertexBufferUploader uploader = new WorldVertexBufferUploader();
   private final WorldRenderer rects = new WorldRenderer(2048);
   private final WorldRenderer icons = new WorldRenderer(2048);
   private boolean rectsOpen;
   private boolean iconsOpen;
   private final WorldRenderer[] texts = new WorldRenderer[6];
   private final boolean[] textOpen = new boolean[6];
   private final SmoothFont[] textFont = new SmoothFont[6];
   private final int[] textPixels = new int[6];
   private final int[] textAtlasW = new int[6];
   private final int[] textAtlasH = new int[6];
   private int textSlots;
   private final IslandDraw.OneChar one = new IslandDraw.OneChar();

   IslandDraw() {
      for (int i = 0; i < 6; i++) {
         this.texts[i] = new WorldRenderer(4096);
      }
   }

   void place(float ox, float oy, float u) {
      this.place(ox, oy, u, u);
   }

   void place(float ox, float oy, float u, float textU) {
      this.ox = ox;
      this.oy = oy;
      this.u = u;
      this.textU = textU;
      this.scale = 1.0F;
      this.alpha = 1.0F;
      this.outer = 1.0F;
      this.windows = 0;
   }

   void setOuterAlpha(float a) {
      this.outer = Math.max(0.0F, Math.min(1.0F, a));
   }

   void scaleAround(float s, float px, float py) {
      this.scale = s;
      this.pivotX = px;
      this.pivotY = py;
   }

   void setAlpha(float a) {
      this.alpha = Math.max(0.0F, Math.min(1.0F, a));
   }

   float getAlpha() {
      return this.alpha;
   }

   void pushWindow(float x0, float x1, float fade) {
      if (this.windows < 4) {
         this.winX0[this.windows] = x0;
         this.winX1[this.windows] = x1;
         this.winFade[this.windows] = fade;
         this.windows++;
      }
   }

   void popWindow() {
      if (this.windows > 0) {
         this.windows--;
      }
   }

   float windowAt(float x) {
      float f = 1.0F;

      for (int i = 0; i < this.windows; i++) {
         if (x < this.winX0[i] || x > this.winX1[i]) {
            return 0.0F;
         }

         if (this.winFade[i] > 0.0F) {
            f = Math.min(f, Math.min((x - this.winX0[i]) / this.winFade[i], (this.winX1[i] - x) / this.winFade[i]));
         }
      }

      return f < 0.0F ? 0.0F : (f > 1.0F ? 1.0F : f);
   }

   private float clipLeft() {
      float v = Float.NEGATIVE_INFINITY;

      for (int i = 0; i < this.windows; i++) {
         v = Math.max(v, this.winX0[i]);
      }

      return v;
   }

   private float clipRight() {
      float v = Float.POSITIVE_INFINITY;

      for (int i = 0; i < this.windows; i++) {
         v = Math.min(v, this.winX1[i]);
      }

      return v;
   }

   float X(float x) {
      float s = this.scale == 1.0F ? x : this.pivotX + (x - this.pivotX) * this.scale;
      return this.ox + s * this.u;
   }

   float Y(float y) {
      float s = this.scale == 1.0F ? y : this.pivotY + (y - this.pivotY) * this.scale;
      return this.oy + s * this.u;
   }

   float L(float len) {
      return len * this.u * this.scale;
   }

   float pixelsPerUnit() {
      return this.u * Ui.px;
   }

   private int fade(int color, float extra) {
      float a = this.alpha * extra;
      if (a >= 1.0F) {
         return color;
      } else {
         int al = (int)((color >>> 24) * a + 0.5F);
         return al << 24 | color & 16777215;
      }
   }

   void roundRect(float x, float y, float w, float h, float r, int color) {
      float f = this.windows == 0 ? 1.0F : this.windowAt(x + w / 2.0F);
      if (f > 0.0F) {
         Ui.roundRect(this.X(x), this.Y(y), this.L(w), this.L(h), this.L(r), this.fade(color, f));
      }
   }

   void roundRectUnclipped(float x, float y, float w, float h, float r, int color) {
      Ui.roundRect(this.X(x), this.Y(y), this.L(w), this.L(h), this.L(r), this.fade(color, 1.0F));
   }

   void outline(float x, float y, float w, float h, float r, float thickness, int color) {
      float f = this.windows == 0 ? 1.0F : this.windowAt(x + w / 2.0F);
      if (f > 0.0F) {
         Ui.outline(this.X(x), this.Y(y), this.L(w), this.L(h), this.L(r), this.L(thickness), this.fade(color, f));
      }
   }

   void outlineUnclipped(float x, float y, float w, float h, float r, float thickness, int color) {
      Ui.outline(this.X(x), this.Y(y), this.L(w), this.L(h), this.L(r), this.L(thickness), this.fade(color, 1.0F));
   }

   void shadow(float x, float y, float w, float h, float r, float blur, int color) {
      Ui.shadow(this.X(x), this.Y(y), this.L(w), this.L(h), this.L(r), this.L(blur), this.fade(color, 1.0F));
   }

   void circle(float cx, float cy, float r, int color) {
      float f = this.windows == 0 ? 1.0F : this.windowAt(cx);
      if (f > 0.0F) {
         Ui.circle(this.X(cx), this.Y(cy), this.L(r), this.fade(color, f));
      }
   }

   void glow(float cx, float cy, float size, int color) {
      float f = this.windows == 0 ? 1.0F : this.windowAt(cx);
      if (f > 0.0F) {
         Ui.glow(this.X(cx), this.Y(cy), this.L(size), this.fade(color, f));
      }
   }

   void roundedImage(int texture, float x, float y, float w, float h, float r, float u0, float v0, float u1, float v1, int color) {
      float f = this.windows == 0 ? 1.0F : this.windowAt(x + w / 2.0F);
      if (f > 0.0F) {
         Ui.roundedImage(texture, this.X(x), this.Y(y), this.L(w), this.L(h), this.L(r), u0, v0, u1, v1, this.fade(color, f));
      }
   }

   private void quad(WorldRenderer wr, boolean textured, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int color) {
      float aL = 1.0F;
      float aR = 1.0F;
      if (this.windows > 0) {
         float cl = this.clipLeft();
         float cr = this.clipRight();
         if (x1 <= cl || x0 >= cr) {
            return;
         }

         if (x0 < cl) {
            if (textured) {
               u0 += (u1 - u0) * (cl - x0) / (x1 - x0);
            }

            x0 = cl;
         }

         if (x1 > cr) {
            if (textured) {
               u1 -= (u1 - u0) * (x1 - cr) / (x1 - x0);
            }

            x1 = cr;
         }

         aL = this.windowAt(x0);
         aR = this.windowAt(x1);
         if (aL <= 0.0F && aR <= 0.0F) {
            return;
         }
      }

      int cL = this.fade(color, aL * this.outer);
      int cR = this.fade(color, aR * this.outer);
      float X0 = this.X(x0);
      float X1 = this.X(x1);
      float Y0 = this.Y(y0);
      float Y1 = this.Y(y1);
      int rL = cL >> 16 & 0xFF;
      int gL = cL >> 8 & 0xFF;
      int bL = cL & 0xFF;
      int alL = cL >>> 24;
      int rR = cR >> 16 & 0xFF;
      int gR = cR >> 8 & 0xFF;
      int bR = cR & 0xFF;
      int alR = cR >>> 24;
      if (textured) {
         wr.pos(X0, Y1, 0.0).tex(u0, v1).color(rL, gL, bL, alL).endVertex();
         wr.pos(X1, Y1, 0.0).tex(u1, v1).color(rR, gR, bR, alR).endVertex();
         wr.pos(X1, Y0, 0.0).tex(u1, v0).color(rR, gR, bR, alR).endVertex();
         wr.pos(X0, Y0, 0.0).tex(u0, v0).color(rL, gL, bL, alL).endVertex();
      } else {
         wr.pos(X0, Y1, 0.0).color(rL, gL, bL, alL).endVertex();
         wr.pos(X1, Y1, 0.0).color(rR, gR, bR, alR).endVertex();
         wr.pos(X1, Y0, 0.0).color(rR, gR, bR, alR).endVertex();
         wr.pos(X0, Y0, 0.0).color(rL, gL, bL, alL).endVertex();
      }
   }

   void rect(float x, float y, float w, float h, int color) {
      if (color >>> 24 != 0 && !(this.alpha <= 0.0F) && !(w <= 0.0F) && !(h <= 0.0F)) {
         if (!this.rectsOpen) {
            this.rects.begin(7, DefaultVertexFormats.POSITION_COLOR);
            this.rectsOpen = true;
         }

         this.quad(this.rects, false, x, y, x + w, y + h, 0.0F, 0.0F, 0.0F, 0.0F, color);
      }
   }

   void hairline(float x, float y, float h, int color) {
      if (color >>> 24 != 0 && !(this.alpha <= 0.0F) && !(h <= 0.0F)) {
         float px = Ui.px * this.u * this.scale;
         float snapped = ((float)Math.floor(this.X(x) * Ui.px) / Ui.px - this.ox) / this.u;
         if (this.scale != 1.0F) {
            snapped = this.pivotX + (snapped - this.pivotX) / this.scale;
         }

         this.rect(snapped, y, 1.0F / px, h, color);
      }
   }

   void icon(IslandIcons.Icon icon, float x, float y, float size, int color) {
      if (color >>> 24 != 0 && !(this.alpha <= 0.0F) && IslandIcons.texture() != 0) {
         if (!this.iconsOpen) {
            this.icons.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            this.iconsOpen = true;
         }

         float u0 = IslandIcons.u0(icon);
         float v0 = IslandIcons.v0(icon);
         this.quad(this.icons, true, x, y, x + size, y + size, u0, v0, u0 + IslandIcons.du(), v0 + IslandIcons.dv(), color);
      }
   }

   private static SmoothFont font(boolean bold) {
      return bold ? Ui.bold : Ui.font;
   }

   int pixels(float size) {
      return SmoothFont.pixelSize(size * this.textU, Ui.px);
   }

   float width(boolean bold, float size, String s) {
      return font(bold).width(s, size * this.textU, Ui.px) / this.textU;
   }

   float digitWidth(boolean bold, float size) {
      return this.charWidth(bold, size, '0');
   }

   float charWidth(boolean bold, float size, char c) {
      SmoothFont.Glyph g = font(bold).glyph(c, this.pixels(size));
      return g == null ? 0.0F : g.advance / Ui.px / this.textU;
   }

   float lineHeight(boolean bold, float size) {
      return font(bold).linePixels(this.pixels(size)) / Ui.px / this.textU;
   }

   private int slot(SmoothFont f, int pixelSize) {
      for (int i = 0; i < this.textSlots; i++) {
         if (this.textFont[i] == f && this.textPixels[i] == pixelSize) {
            return i;
         }
      }

      if (this.textSlots == 6) {
         return -1;
      } else {
         int var4 = this.textSlots++;
         this.textFont[var4] = f;
         this.textPixels[var4] = pixelSize;
         return var4;
      }
   }

   void text(boolean bold, float size, CharSequence s, float x, float y, int color) {
      if (UiProbe.recording() && this.alpha >= 0.5F) {
         float lh = this.lineHeight(bold, size);
         float tw = this.width(bold, size, s.toString());
         if (this.windows == 0 || this.windowAt(x) > 0.5F && this.windowAt(x + tw) > 0.5F) {
            Ui.registerText(this.X(x), this.Y(y + lh * 0.15F), this.L(tw), this.L(lh * 0.7F), s.toString());
         }
      }

      this.emit(font(bold), size, s, x, y, color, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
   }

   void textMiddle(boolean bold, float size, CharSequence s, float x, float cy, int color) {
      this.text(bold, size, s, x, this.middleTop(bold, size, cy), color);
   }

   float middleTop(boolean bold, float size, float cy) {
      return cy - font(bold).capMiddlePixels(this.pixels(size)) / Ui.px / this.textU;
   }

   float heavyMiddleTop(float size, float cy) {
      return cy - heavy().capMiddlePixels(this.pixels(size)) / Ui.px / this.textU;
   }

   float sameBaseline(boolean valueBold, float valueSize, float valueTop, float size) {
      float k = Ui.px * this.textU / this.u;
      float baseline = snap((this.oy + valueTop * this.u) * k) + font(valueBold).ascentPixels(this.pixels(valueSize));
      return ((baseline - font(false).ascentPixels(this.pixels(size))) / k - this.oy) / this.u;
   }

   float[] capsOffsetForTest(float size, float cy) {
      SmoothFont f = heavy();
      int pxSize = this.pixels(size);
      float k = Ui.px * this.textU / this.u;
      float cyPx = (this.oy + cy * this.u) * k;
      float capMid = f.capMiddlePixels(pxSize);
      float now = snap(cyPx - capMid) + capMid - cyPx;
      float old = Math.round(cyPx - f.linePixels(pxSize) / 2.0F) + capMid - cyPx;
      return new float[]{now, old};
   }

   private static float snap(float px) {
      return (float)Math.ceil(px - 0.5F);
   }

   void glyph(boolean bold, float size, char c, float x, float y, int color) {
      this.one.c = c;
      this.emit(font(bold), size, this.one, x, y, color, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
   }

   void glyphClipped(boolean bold, float size, char c, float x, float y, int color, float clipTop, float clipBottom) {
      this.one.c = c;
      this.emit(font(bold), size, this.one, x, y, color, clipTop, clipBottom);
   }

   private static SmoothFont heavy() {
      return Ui.heavy != null ? Ui.heavy : Ui.bold;
   }

   void heavyGlyph(float size, char c, float x, float y, int color) {
      this.one.c = c;
      this.emit(heavy(), size, this.one, x, y, color, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
   }

   void heavyGlyphClipped(float size, char c, float x, float y, int color, float clipTop, float clipBottom) {
      this.one.c = c;
      this.emit(heavy(), size, this.one, x, y, color, clipTop, clipBottom);
   }

   float heavyCharWidth(float size, char c) {
      SmoothFont.Glyph g = heavy().glyph(c, this.pixels(size));
      return g == null ? 0.0F : g.advance / Ui.px / this.textU;
   }

   float heavyLineHeight(float size) {
      return heavy().linePixels(this.pixels(size)) / Ui.px / this.textU;
   }

   float heavyAscent(float size) {
      return heavy().ascentPixels(this.pixels(size)) / Ui.px / this.textU;
   }

   boolean heavyGlyphBox(float size, char c, float[] out) {
      SmoothFont.Glyph g = heavy().glyph(c, this.pixels(size));
      if (g != null && g.w > 4) {
         float pu = Ui.px * this.textU;
         out[0] = g.offX / pu;
         out[1] = g.offY / pu;
         out[2] = (g.offX + g.w) / pu;
         out[3] = (g.offY + g.h) / pu;
         return true;
      } else {
         return false;
      }
   }

   void heavyQuad(float size, char c, float f0, float f1, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
      if (color >>> 24 != 0 && !(this.alpha <= 0.0F)) {
         SmoothFont f = heavy();
         int pxSize;
         SmoothFont.Glyph g = f.glyph(c, pxSize = this.pixels(size));
         if (g != null && g.w > 4) {
            int i = this.slot(f, pxSize);
            if (i >= 0) {
               WorldRenderer wr = this.texts[i];
               if (!this.textOpen[i]) {
                  f.atlasTexture(pxSize);
                  this.textAtlasW[i] = f.atlasWidth(pxSize);
                  this.textAtlasH[i] = f.atlasHeight(pxSize);
                  wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                  this.textOpen[i] = true;
               }

               float iw = 1.0F / this.textAtlasW[i];
               float ih = 1.0F / this.textAtlasH[i];
               float u0 = (g.x + g.w * f0) * iw;
               float u1 = (g.x + g.w * f1) * iw;
               float v0 = g.y * ih;
               float v1 = (g.y + g.h) * ih;
               int cc = this.fade(color, this.outer);
               int r = cc >> 16 & 0xFF;
               int gr = cc >> 8 & 0xFF;
               int b = cc & 0xFF;
               int a = cc >>> 24;
               wr.pos(this.X(x3), this.Y(y3), 0.0).tex(u0, v1).color(r, gr, b, a).endVertex();
               wr.pos(this.X(x2), this.Y(y2), 0.0).tex(u1, v1).color(r, gr, b, a).endVertex();
               wr.pos(this.X(x1), this.Y(y1), 0.0).tex(u1, v0).color(r, gr, b, a).endVertex();
               wr.pos(this.X(x0), this.Y(y0), 0.0).tex(u0, v0).color(r, gr, b, a).endVertex();
            }
         }
      }
   }

   private void emit(SmoothFont f, float size, CharSequence s, float x, float y, int color, float clipTop, float clipBottom) {
      if (color >>> 24 != 0 && !(this.alpha <= 0.0F) && s.length() != 0) {
         int pxSize = this.pixels(size);

         for (int k = 0; k < s.length(); k++) {
            f.glyph(s.charAt(k), pxSize);
         }

         int i = this.slot(f, pxSize);
         if (i >= 0) {
            WorldRenderer wr = this.texts[i];
            if (!this.textOpen[i]) {
               f.atlasTexture(pxSize);
               this.textAtlasW[i] = f.atlasWidth(pxSize);
               this.textAtlasH[i] = f.atlasHeight(pxSize);
               wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
               this.textOpen[i] = true;
            }

            float px = Ui.px;
            float kk = this.textU / this.u;
            float pu = px * this.textU;
            float iw = 1.0F / this.textAtlasW[i];
            float ih = 1.0F / this.textAtlasH[i];
            float penPx = (this.ox + x * this.u) * px * kk;
            float baselinePx = snap((this.oy + y * this.u) * px * kk) + f.ascentPixels(pxSize);
            float clipTopPx = clipTop == Float.NEGATIVE_INFINITY ? Float.NEGATIVE_INFINITY : (this.oy + clipTop * this.u) * px * kk;
            float clipBottomPx = clipBottom == Float.POSITIVE_INFINITY ? Float.POSITIVE_INFINITY : (this.oy + clipBottom * this.u) * px * kk;
            float originPx = this.ox * px * kk;
            float originPy = this.oy * px * kk;

            for (int k = 0; k < s.length(); k++) {
               SmoothFont.Glyph gl = f.glyph(s.charAt(k), pxSize);
               if (gl != null) {
                  if (gl.w > 4) {
                     float gx = Math.round(penPx) + gl.offX;
                     float gy = baselinePx + gl.offY;
                     float gy1 = gy + gl.h;
                     float v0 = gl.y * ih;
                     float v1 = (gl.y + gl.h) * ih;
                     if (gy < clipTopPx) {
                        v0 += (clipTopPx - gy) * ih;
                        gy = clipTopPx;
                     }

                     if (gy1 > clipBottomPx) {
                        v1 -= (gy1 - clipBottomPx) * ih;
                        gy1 = clipBottomPx;
                     }

                     if (gy1 > gy) {
                        this.quad(
                           wr,
                           true,
                           (gx - originPx) / pu,
                           (gy - originPy) / pu,
                           (gx + gl.w - originPx) / pu,
                           (gy1 - originPy) / pu,
                           gl.x * iw,
                           v0,
                           (gl.x + gl.w) * iw,
                           v1,
                           color
                        );
                     }
                  }

                  penPx += gl.advance;
               }
            }
         }
      }
   }

   void flush() {
      if (this.rectsOpen) {
         GlStateManager.disableTexture2D();
         this.rects.finishDrawing();
         this.uploader.draw(this.rects);
         GlStateManager.enableTexture2D();
         this.rectsOpen = false;
      }

      if (this.iconsOpen) {
         GlStateManager.bindTexture(IslandIcons.texture());
         this.icons.finishDrawing();
         this.uploader.draw(this.icons);
         this.iconsOpen = false;
      }

      for (int i = 0; i < this.textSlots; i++) {
         if (this.textOpen[i]) {
            SmoothFont f = this.textFont[i];
            GlStateManager.bindTexture(f.atlasTexture(this.textPixels[i]));
            this.texts[i].finishDrawing();
            int w = f.atlasWidth(this.textPixels[i]);
            int h = f.atlasHeight(this.textPixels[i]);
            boolean grown = w != this.textAtlasW[i] || h != this.textAtlasH[i];
            if (grown) {
               GlStateManager.matrixMode(5890);
               GlStateManager.pushMatrix();
               GlStateManager.loadIdentity();
               GlStateManager.scale((float)this.textAtlasW[i] / w, (float)this.textAtlasH[i] / h, 1.0F);
               GlStateManager.matrixMode(5888);
            }

            this.uploader.draw(this.texts[i]);
            if (grown) {
               GlStateManager.matrixMode(5890);
               GlStateManager.popMatrix();
               GlStateManager.matrixMode(5888);
            }

            this.textOpen[i] = false;
         }
      }

      this.textSlots = 0;
   }

   private static final class OneChar implements CharSequence {
      char c;

      private OneChar() {
      }

      @Override
      public int length() {
         return 1;
      }

      @Override
      public char charAt(int index) {
         return this.c;
      }

      @Override
      public CharSequence subSequence(int start, int end) {
         return String.valueOf(this.c).subSequence(start, end);
      }

      @Override
      public String toString() {
         return String.valueOf(this.c);
      }
   }
}
