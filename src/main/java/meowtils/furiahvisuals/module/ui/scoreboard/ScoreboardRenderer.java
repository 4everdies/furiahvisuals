package meowtils.furiahvisuals.module.ui.scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.hud.HudFont;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.hud.HudPanel;
import meowtils.furiahvisuals.hud.HudPosition;
import meowtils.furiahvisuals.module.watermark.Spring;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

final class ScoreboardRenderer {
   private static final Comparator<ScoreboardRenderer.Line> BY_Y = new Comparator<ScoreboardRenderer.Line>() {
      public int compare(ScoreboardRenderer.Line a, ScoreboardRenderer.Line b) {
         return Float.compare(a.y, b.y);
      }
   };
   private static final int BURST = 6;
   private final Map<String, ScoreboardRenderer.Line> lines = new HashMap<>();
   private final List<ScoreboardRenderer.Line> drawList = new ArrayList<>();
   private String title = "";
   private String oldTitle;
   private float titleMix = 1.0F;
   private float panelW = -1.0F;
   private float panelH = -1.0F;
   private float titleH = -1.0F;
   private float shown;
   private int presentCount;
   float lastX;
   float lastY;
   float lastW;
   float lastH;
   private float avoidX;
   private float avoidY;
   private float avoidW;
   private float avoidH;
   private boolean avoiding;
   private final Spring push = new Spring();

   void update(String newTitle, List<ScoreboardRenderer.Entry> entries, float dt, float speed) {
      boolean none = entries == null;
      this.shown = speed <= 0.0F ? (none ? 0.0F : 1.0F) : Easing.approach(this.shown, none ? 0.0F : 1.0F, 10.0F * speed, dt);
      if (none) {
         for (ScoreboardRenderer.Line l : this.lines.values()) {
            l.present = false;
         }

         this.animate(dt, speed, false);
      } else {
         int changes = 0;
         if (!newTitle.equals(this.title)) {
            this.oldTitle = this.title.isEmpty() ? null : this.title;
            this.title = newTitle;
            this.titleMix = this.oldTitle == null ? 1.0F : 0.0F;
         }

         for (ScoreboardRenderer.Line l : this.lines.values()) {
            l.present = false;
         }

         int i = 0;

         while (i < entries.size()) {
            ScoreboardRenderer.Entry e = entries.get(i);
            ScoreboardRenderer.Line l = this.lines.get(e.key);
            String score = Integer.toString(e.score);
            if (l == null) {
               l = new ScoreboardRenderer.Line(e.key);
               l.text = e.text;
               l.score = score;
               l.y = i;
               this.lines.put(e.key, l);
               changes++;
            } else {
               if (!l.text.equals(e.text)) {
                  l.oldText = l.text;
                  l.text = e.text;
                  l.textMix = 0.0F;
                  l.rollText = ScoreboardRules.normalize(l.oldText).equals(ScoreboardRules.normalize(e.text))
                     && formatting(l.oldText).equals(formatting(e.text));
                  changes++;
               }

               if (!l.score.equals(score)) {
                  l.oldScore = l.score;
                  l.score = score;
                  l.roll = 0.0F;
                  changes++;
               }
            }

            l.present = true;
            l.index = i++;
         }

         this.presentCount = entries.size();
         this.animate(dt, speed, changes > 6);
      }
   }

   private void animate(float dt, float speed, boolean burst) {
      boolean instant = speed <= 0.0F || burst;
      Iterator<ScoreboardRenderer.Line> it = this.lines.values().iterator();

      while (it.hasNext()) {
         ScoreboardRenderer.Line l = it.next();
         float target = l.present ? 1.0F : 0.0F;
         float f = l.appear = instant ? target : Easing.approach(l.appear, target, 11.0F * speed, dt);
         if (l.present) {
            l.y = !instant && !Float.isNaN(l.y) ? Easing.approach(l.y, l.index, 14.0F * speed, dt) : l.index;
         }

         l.textMix = instant ? 1.0F : Math.min(1.0F, l.textMix + dt * speed / 0.22F);
         float f2 = l.roll = instant ? 1.0F : Math.min(1.0F, l.roll + dt * speed / 0.3F);
         if (!l.present && l.appear < 0.01F) {
            it.remove();
         }
      }

      this.titleMix = instant ? 1.0F : Math.min(1.0F, this.titleMix + dt * speed / 0.25F);
   }

   boolean visible() {
      return this.shown > 0.003F && (!this.lines.isEmpty() || this.presentCount > 0);
   }

   private static FontRenderer font() {
      return Minecraft.getMinecraft().fontRendererObj;
   }

   private static float width(ScoreboardRenderer.Style st, String text) {
      return st.modFont ? HudFont.width(text) : font().getStringWidth(text);
   }

   private static void text(ScoreboardRenderer.Style st, String text, float x, float y, int color) {
      if (color >>> 24 >= 6) {
         if (st.modFont) {
            HudFont.draw(text, x, y, color, st.shadow);
         } else {
            font().drawString(text, x, y, color, st.shadow);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
         }
      }
   }

   void avoid(float[] island, float k) {
      if (island == null) {
         this.avoidW = 0.0F;
      } else {
         this.avoidX = island[0] / k;
         this.avoidY = island[1] / k;
         this.avoidW = island[2] / k;
         this.avoidH = island[3] / k;
      }
   }

   private float keepClear(float left, float top, float w, float h, float screenH, float dt, float speed) {
      float target = 0.0F;
      if (this.avoidW > 0.0F) {
         float gap = 3.0F;
         float slack = this.avoiding ? 4.0F : 0.0F;
         boolean bl = this.avoiding = left < this.avoidX + this.avoidW + slack
            && left + w > this.avoidX - slack
            && top < this.avoidY + this.avoidH + gap + slack
            && top + h > this.avoidY - gap - slack;
         if (this.avoiding) {
            boolean islandHigh = this.avoidY + this.avoidH / 2.0F < screenH / 2.0F;
            float to = islandHigh ? this.avoidY + this.avoidH + gap : this.avoidY - gap - h;
            target = Math.max(0.0F, Math.min(screenH - h, to)) - top;
         }
      } else {
         this.avoiding = false;
      }

      if (!(speed <= 0.0F) && this.push.isStarted()) {
         this.push.update(target, 18.0F * speed, dt);
      } else {
         this.push.snap(target);
      }

      return this.push.value;
   }

   void draw(ScoreboardRenderer.Style st, HudLook look, float screenW, float screenH, HudPosition pos, float posScale, float alphaAll, float dt, float speed) {
      float alpha = alphaAll * Easing.outCubic(Math.min(1.0F, this.shown));
      if (!(alpha <= 0.003F)) {
         boolean instant = speed <= 0.0F;
         float lineH = 9.0F + st.spacing;
         float pad = st.themed ? st.pad : 2.0F;
         float gap = st.themed ? 6.0F : 4.0F;
         float nameW = 0.0F;
         float scoreW = 0.0F;
         int count = 0;

         for (ScoreboardRenderer.Line l : this.lines.values()) {
            if (l.present) {
               count++;
               nameW = Math.max(nameW, width(st, l.text));
               if (st.showScores) {
                  scoreW = Math.max(scoreW, width(st, "§c" + l.score));
               }
            }
         }

         float titleW = st.showTitle ? width(st, this.title) : 0.0F;
         float contentW = Math.max(titleW, nameW + (st.showScores ? gap + scoreW : 0.0F));
         float targetW = contentW + pad * 2.0F;
         float titleTarget = !st.showTitle ? 0.0F : (st.themed ? 9.0F + (st.strip ? pad + 4.0F : pad + (st.underline ? 2.0F : st.spacing / 2.0F)) : 10.0F);
         this.titleH = !(this.titleH < 0.0F) && !instant ? Easing.approach(this.titleH, titleTarget, 12.0F * speed, dt) : titleTarget;
         float targetH = titleTarget + count * lineH + (st.themed ? pad + (st.showTitle ? 0.0F : pad) : 0.0F);
         this.panelW = !(this.panelW < 0.0F) && !instant ? Easing.approach(this.panelW, targetW, 14.0F * speed, dt) : targetW;
         this.panelH = !(this.panelH < 0.0F) && !instant ? Easing.approach(this.panelH, targetH, 12.0F * speed, dt) : targetH;
         float w = this.panelW;
         float h = this.panelH;
         float top;
         float left;
         if (pos != null && pos.moved()) {
            left = pos.x(screenW * posScale, w * posScale) / posScale;
            top = pos.y(screenH * posScale, h * posScale) / posScale;
         } else {
            float bottom = screenH / 2.0F + count * 9.0F / 3.0F + (st.themed ? pad : 0.0F);
            top = bottom - h;
            left = screenW - (st.themed ? 3.0F : 1.0F) - w;
         }

         top += this.keepClear(left, top, w, h, screenH, dt, speed);
         this.lastX = left;
         this.lastY = top;
         this.lastW = w;
         this.lastH = h;
         float r = st.themed ? look.radius : 0.0F;
         if (st.themed) {
            HudPanel.background(left, top, w, h, r, look, alpha * st.bgOpacity);
            HudFont.setOnLight(look.colors.light && look.background && st.bgOpacity > 0.5F);
         } else if (look.background) {
            if (st.showTitle) {
               Ui.rect(left, top, w, this.titleH, ColorUtil.mulAlpha(1610612736, alpha * st.bgOpacity));
            }

            Ui.rect(left, top + this.titleH, w, h - this.titleH, ColorUtil.mulAlpha(1342177280, alpha * st.bgOpacity));
         }

         int base = st.themed && look.background && st.bgOpacity > 0.5F ? look.colors.text | 0xFF000000 : -1;
         if (st.showTitle) {
            float ty = top + (st.themed ? (st.strip ? pad * 0.5F + 2.5F : pad) : 1.0F);
            if (st.themed && st.strip && look.background) {
               Ui.roundRect(
                  left + 2.0F,
                  top + 2.0F,
                  w - 4.0F,
                  this.titleH - 3.0F,
                  Math.max(0.0F, r - 2.0F),
                  ColorUtil.mulAlpha(look.colors.text | 0xFF000000, 0.07F * alpha)
               );
            }

            float e = Easing.outCubic(this.titleMix);
            if (this.oldTitle != null && this.titleMix < 1.0F) {
               this.drawTitle(st, this.oldTitle, left, w, pad, ty, ColorUtil.mulAlpha(base, alpha * (1.0F - e)));
            }

            float tw = this.drawTitle(st, this.title, left, w, pad, ty, ColorUtil.mulAlpha(base, alpha * (this.oldTitle != null ? e : 1.0F)));
            if (st.themed && st.underline) {
               float ux = st.titleCentered ? left + (w - tw) / 2.0F : left + pad;
               float uh = 1.5F / Ui.px;
               Ui.rect(ux, ty + 10.0F, tw, uh, ColorUtil.mulAlpha(look.colors.accent | 0xFF000000, 0.9F * alpha));
            }
         }

         this.drawList.clear();
         this.drawList.addAll(this.lines.values());
         Collections.sort(this.drawList, BY_Y);
         float y0 = top + this.titleH + (st.themed && !st.showTitle ? pad : 0.0F);
         float nameRight = left + w - pad - (st.showScores ? scoreW + gap : 0.0F);

         for (ScoreboardRenderer.Line lx : this.drawList) {
            float a;
            if (!Float.isNaN(lx.y) && !((a = Easing.outCubic(Math.max(0.0F, Math.min(1.0F, lx.appear)))) <= 0.01F)) {
               float ly = y0 + lx.y * lineH + st.spacing / 2.0F;
               float slide = (1.0F - a) * 8.0F;
               float la = alpha * a;
               float ex = Easing.outCubic(lx.textMix);
               if (lx.oldText != null && lx.textMix < 1.0F && lx.rollText) {
                  rollName(st, lx.oldText, lx.text, ex, left + pad + slide, nameRight + slide, ly, lineH, ColorUtil.mulAlpha(base, la));
               } else {
                  if (lx.oldText != null && lx.textMix < 1.0F) {
                     drawName(st, lx.oldText, left + pad + slide, nameRight + slide, ly, ColorUtil.mulAlpha(base, la * (1.0F - ex)));
                  }

                  drawName(
                     st,
                     lx.text,
                     left + pad + slide,
                     nameRight + slide,
                     ly,
                     ColorUtil.mulAlpha(base, la * (lx.oldText != null && lx.textMix < 1.0F ? ex : 1.0F))
                  );
               }

               if (st.showScores) {
                  drawScore(st, lx, left + w - pad + slide, ly, lineH, ColorUtil.mulAlpha(st.scoreColor | 0xFF000000, la));
               }
            }
         }

         if (st.themed) {
            HudFont.setOnLight(false);
            HudPanel.foreground(left, top, w, h, r, look, alpha);
         }
      }
   }

   private float drawTitle(ScoreboardRenderer.Style st, String t, float left, float w, float pad, float y, int color) {
      float tw = width(st, t);
      float tx = !st.titleCentered && st.themed ? left + pad : left + (w - tw) / 2.0F;
      text(st, t, tx, y, color);
      return tw;
   }

   private static void drawName(ScoreboardRenderer.Style st, String t, float left, float right, float y, int color) {
      float tw = width(st, t);
      float x = st.align == 1 ? left + (right - left - tw) / 2.0F : (st.align == 2 ? right - tw : left);
      text(st, t, x, y, color);
   }

   private static void rollName(ScoreboardRenderer.Style st, String old, String now, float e, float left, float right, float y, float lineH, int color) {
      int max = Math.min(old.length(), now.length());
      int start = 0;

      while (start < max && old.charAt(start) == now.charAt(start)) {
         start++;
      }

      int endOld = old.length();

      int endNow;
      for (endNow = now.length(); endOld > start && endNow > start && old.charAt(endOld - 1) == now.charAt(endNow - 1); endNow--) {
         endOld--;
      }

      while (start > 0 && Character.isDigit(now.charAt(start - 1)) && (start < 2 || now.charAt(start - 2) != 167)) {
         start--;
      }

      String prefix = now.substring(0, start);
      String midOld = old.substring(start, endOld);
      String midNow = now.substring(start, endNow);
      String suffix = now.substring(endNow);
      float wOld = width(st, old);
      float wNow = width(st, now);
      float tw = wOld + (wNow - wOld) * e;
      float x = st.align == 1 ? left + (right - left - tw) / 2.0F : (st.align == 2 ? right - tw : left);
      String codes = formattingAt(prefix);
      float pw = width(st, prefix);
      text(st, prefix, x, y, color);
      float lift = lineH * 0.55F;
      text(st, codes + midOld, x + pw, y - lift * e, ColorUtil.mulAlpha(color, 1.0F - e));
      text(st, codes + midNow, x + pw, y + lift * (1.0F - e), ColorUtil.mulAlpha(color, e));
      float sOld = width(st, prefix + midOld);
      float sNow = width(st, prefix + midNow);
      text(st, formattingAt(prefix + midNow) + suffix, x + sOld + (sNow - sOld) * e, y, color);
   }

   private static String formattingAt(String s) {
      int lastColor = -1;
      StringBuilder styles = new StringBuilder();

      for (int i = 0; i < s.length() - 1; i++) {
         if (s.charAt(i) == 167) {
            char c = Character.toLowerCase(s.charAt(i + 1));
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && c != 'r') {
               if ("klmno".indexOf(c) >= 0) {
                  styles.append('§').append(c);
               }
            } else {
               lastColor = i;
               styles.setLength(0);
            }

            i++;
         }
      }

      return (lastColor >= 0 ? s.substring(lastColor, lastColor + 2) : "") + styles;
   }

   private static String formatting(String s) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < s.length() - 1; i++) {
         if (s.charAt(i) == 167) {
            sb.append(s.charAt(i + 1));
            i++;
         }
      }

      return sb.toString();
   }

   private static void drawScore(ScoreboardRenderer.Style st, ScoreboardRenderer.Line l, float right, float y, float lineH, int color) {
      String now = l.score;
      if (l.oldScore != null && !(l.roll >= 1.0F)) {
         float e = Easing.outCubic(l.roll);
         float lift = lineH * 0.55F;
         String old = l.oldScore;
         if (old.length() != now.length()) {
            text(st, old, right - width(st, old), y - lift * e, ColorUtil.mulAlpha(color, 1.0F - e));
            text(st, now, right - width(st, now), y + lift * (1.0F - e), ColorUtil.mulAlpha(color, e));
         } else {
            float x = right;

            for (int i = now.length() - 1; i >= 0; i--) {
               String cNow = now.substring(i, i + 1);
               String cOld = old.substring(i, i + 1);
               float cw = width(st, cNow);
               x -= cw;
               if (cNow.equals(cOld)) {
                  text(st, cNow, x, y, color);
               } else {
                  text(st, cOld, x + (cw - width(st, cOld)) / 2.0F, y - lift * e, ColorUtil.mulAlpha(color, 1.0F - e));
                  text(st, cNow, x, y + lift * (1.0F - e), ColorUtil.mulAlpha(color, e));
               }
            }
         }
      } else {
         text(st, "§r" + now, right - width(st, now), y, color);
      }
   }

   void clear() {
      this.lines.clear();
      this.title = "";
      this.oldTitle = null;
      this.titleH = -1.0F;
      this.panelH = -1.0F;
      this.panelW = -1.0F;
      this.shown = 0.0F;
      this.presentCount = 0;
   }

   static final class Entry {
      final String key;
      final String text;
      final int score;

      Entry(String key, String text, int score) {
         this.key = key;
         this.text = text;
         this.score = score;
      }
   }

   private static final class Line {
      final String key;
      String text = "";
      String oldText;
      float textMix = 1.0F;
      boolean rollText;
      String score = "";
      String oldScore;
      float roll = 1.0F;
      float appear;
      float y = Float.NaN;
      int index;
      boolean present;

      Line(String key) {
         this.key = key;
      }
   }

   static final class Style {
      boolean themed = true;
      boolean modFont = true;
      boolean shadow;
      boolean showTitle = true;
      boolean strip = true;
      boolean underline = true;
      boolean titleCentered = true;
      boolean showScores = true;
      boolean animate = true;
      int scoreColor = -43691;
      float pad = 3.0F;
      float spacing;
      float scale = 1.0F;
      float bgOpacity = 1.0F;
      int align;
   }
}
