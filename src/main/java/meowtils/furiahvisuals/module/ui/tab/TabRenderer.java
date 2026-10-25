package meowtils.furiahvisuals.module.ui.tab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.hud.HudFont;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.hud.HudPanel;
import meowtils.furiahvisuals.module.ui.nametag.NametagElement;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

final class TabRenderer {
   private static final int MAX_PLAYERS = 80;
   private static final int PER_COLUMN = 20;
   private static final float OPEN_SECONDS = 0.2F;
   private static final float CLOSE_SECONDS = 0.15F;
   private static final float STAGGER_TOTAL = 0.14F;
   private static final float ROW_FADE = 0.12F;
   private final Map<String, TabRenderer.Row> rows = new HashMap<>();
   private final List<TabRenderer.Row> order = new ArrayList<>();
   private TabRenderer.Frame last;
   private TabRenderer.Frame measuredFrame;
   private boolean measuredMod;
   private boolean measuredScores;
   private float measuredPx;
   private float measuredNameW;
   private float measuredScoreW;
   private float open;
   private boolean opening;
   private float sinceOpen;
   float lastX;
   float lastY;
   float lastW;
   float lastH;
   private static final TabRenderer.QuadBatch QUADS = new TabRenderer.QuadBatch();

   boolean visible() {
      return this.open > 0.003F && this.last != null;
   }

   float openAmount() {
      return this.open;
   }

   void update(TabRenderer.Frame frame, float dt, float speed) {
      boolean instant = speed <= 0.0F;
      if (frame != null) {
         if (!this.opening) {
            this.opening = true;
            this.sinceOpen = 0.0F;
            if (this.open <= 0.003F) {
               for (TabRenderer.Row r : this.rows.values()) {
                  r.y = Float.NaN;
                  r.x = Float.NaN;
               }
            }
         }

         this.last = frame;

         for (TabRenderer.Row r : this.rows.values()) {
            r.present = false;
         }

         int n = Math.min(80, frame.entries.size());

         for (int i = 0; i < n; i++) {
            TabRenderer.Entry e = frame.entries.get(i);
            TabRenderer.Row r = this.rows.get(e.key);
            if (r == null) {
               r = new TabRenderer.Row();
               r.e = e;
               r.appear = this.open > 0.5F && !instant ? 0.0F : 1.0F;
               this.rows.put(e.key, r);
            } else {
               if (r.e.score != e.score) {
                  r.oldScore = r.e.score;
                  r.scoreMix = 0.0F;
               }

               if (r.e.ping != e.ping && !samePingText(r.e.ping, e.ping)) {
                  r.oldPing = r.e.ping;
                  r.pingMix = 0.0F;
               }

               if (!r.e.name.equals(e.name)) {
                  r.oldName = r.e.name;
                  r.nameMix = 0.0F;
               }

               r.e = e;
            }

            r.present = true;
         }
      } else {
         this.opening = false;
      }

      float target = this.opening ? 1.0F : 0.0F;
      this.open = instant ? target : (this.opening ? Math.min(1.0F, this.open + dt * speed / 0.2F) : Math.max(0.0F, this.open - dt * speed / 0.15F));
      this.sinceOpen = this.sinceOpen + dt * Math.max(speed, 1.0E-4F);
      Iterator<TabRenderer.Row> it = this.rows.values().iterator();

      while (it.hasNext()) {
         TabRenderer.Row r = it.next();
         if (this.opening) {
            float t = r.present ? 1.0F : 0.0F;
            r.appear = instant ? t : Easing.approach(r.appear, t, 11.0F * speed, dt);
            r.scoreMix = instant ? 1.0F : Math.min(1.0F, r.scoreMix + dt * speed / 0.25F);
            r.pingMix = instant ? 1.0F : Math.min(1.0F, r.pingMix + dt * speed / 0.25F);
            float f2 = r.nameMix = instant ? 1.0F : Math.min(1.0F, r.nameMix + dt * speed / 0.25F);
            if (!r.present && r.appear < 0.01F) {
               it.remove();
            }
         }
      }

      if (this.open <= 0.0F && !this.opening) {
         this.rows.clear();
         this.last = null;
      }
   }

   private static boolean samePingText(int a, int b) {
      return a == b;
   }

   private static FontRenderer font() {
      return Minecraft.getMinecraft().fontRendererObj;
   }

   private static float width(TabRenderer.Style st, String text) {
      return st.modFont ? HudFont.width(text) : font().getStringWidth(text);
   }

   private static void text(TabRenderer.Style st, String t, float x, float y, int color, boolean shadow) {
      if (color >>> 24 >= 6) {
         if (st.modFont) {
            HudFont.draw(t, x, y, color, shadow);
         } else {
            font().drawString(t, x, y, color, shadow);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
         }
      }
   }

   static int pingColor(int ms) {
      return ms < 0 ? -5592406 : (ms < 80 ? -11141291 : (ms < 150 ? -171 : (ms < 250 ? -22016 : -43691)));
   }

   static String pingText(int ms) {
      return ms < 0 ? "?" : ms + "ms";
   }

   private static int level(int ms) {
      return ms < 0 ? 5 : (ms < 150 ? 0 : (ms < 300 ? 1 : (ms < 600 ? 2 : (ms < 1000 ? 3 : 4))));
   }

   void draw(TabRenderer.Style st, HudLook look, float screenW, float alphaAll, float dt, float speed) {
      if (this.last != null && !(this.open <= 0.003F)) {
         boolean instant = speed <= 0.0F;
         TabRenderer.Frame f = this.last;
         float e = Easing.outCubic(this.open);
         float alpha = alphaAll * e;
         this.order.clear();

         for (TabRenderer.Entry entry : f.entries) {
            TabRenderer.Row r2 = this.rows.get(entry.key);
            if (r2 != null && r2.present) {
               this.order.add(r2);
            }

            if (this.order.size() == 80) {
               break;
            }
         }

         int count = this.order.size();

         for (TabRenderer.Row r : this.rows.values()) {
            if (!r.present) {
               this.order.add(r);
            }
         }

         if (f != this.measuredFrame || st.modFont != this.measuredMod || st.showScores != this.measuredScores || Ui.px != this.measuredPx) {
            this.measuredFrame = f;
            this.measuredMod = st.modFont;
            this.measuredScores = st.showScores;
            this.measuredPx = Ui.px;
            this.measuredNameW = 0.0F;
            this.measuredScoreW = 0.0F;

            for (int j = 0; j < count; j++) {
               TabRenderer.Row rx = this.order.get(j);
               this.measuredNameW = Math.max(this.measuredNameW, width(st, rx.e.name));
               if (st.showScores && f.scores == 1) {
                  this.measuredScoreW = Math.max(this.measuredScoreW, width(st, " " + rx.e.score));
               }
            }
         }

         float f2 = this.measuredNameW;
         float scoreW = this.measuredScoreW;
         if (st.showScores && f.scores == 2) {
            scoreW = 90.0F;
         }

         float pingW = st.ping == 3 ? 0.0F : (st.ping == 0 ? 11.0F : (st.ping == 1 ? width(st, "999ms") + 3.0F : 11.0F + width(st, "999ms") + 3.0F));
         int rowsPer = count;

         int cols;
         for (cols = 1; rowsPer > 20; rowsPer = (count + cols - 1) / cols) {
            cols++;
         }

         float pad = st.themed ? 3.0F : 0.0F;
         float rowH = st.themed ? 10.0F : 9.0F;
         float cell = (f.heads ? 9.0F : 0.0F) + f2 + scoreW + pingW + 3.0F + pad * 2.0F;
         float colW = Math.min(cols * cell, screenW - 50.0F) / cols;
         float gridW;
         float panelW = gridW = colW * cols + (cols - 1) * 5.0F;
         List<String> header = st.headerFooter ? f.header : null;
         List<String> footer = st.headerFooter ? f.footer : null;
         if (header != null) {
            for (String s : header) {
               panelW = Math.max(panelW, width(st, s) + pad * 2.0F);
            }
         }

         if (footer != null) {
            for (String s : footer) {
               panelW = Math.max(panelW, width(st, s) + pad * 2.0F);
            }
         }

         float cx = screenW / 2.0F;
         float top = 10.0F;
         GlStateManager.pushMatrix();
         float sy = 0.4F + 0.6F * e;
         float sx = 0.94F + 0.06F * e;
         GlStateManager.translate(cx, top, 0.0F);
         GlStateManager.scale(sx, sy, 1.0F);
         GlStateManager.translate(-cx, -top, 0.0F);
         float y = top;
         float r3 = st.themed ? look.radius : 0.0F;
         HudFont.setOnLight(st.themed && look.colors.light && look.background && st.bgOpacity > 0.5F);
         int base = st.themed && look.background && st.bgOpacity > 0.5F ? look.colors.text | 0xFF000000 : -1;
         boolean shadow = st.shadow && (!st.themed || !look.colors.light || !look.background);
         if (header != null && !header.isEmpty()) {
            float h = header.size() * 9.0F + (st.themed ? pad * 2.0F : 1.0F);
            float hx = cx - panelW / 2.0F - (st.themed ? 0.0F : 1.0F);
            float hw = panelW + (st.themed ? 0.0F : 2.0F);
            panel(st, look, hx, top - (st.themed ? 0.0F : 1.0F), hw, h, r3, alpha);
            float ty = top + (st.themed ? pad : 0.0F);

            for (String s : header) {
               text(st, s, cx - width(st, s) / 2.0F, ty, ColorUtil.mulAlpha(base, alpha), shadow);
               ty += 9.0F;
            }

            if (st.themed) {
               HudPanel.foreground(hx, top, hw, h, r3, look, alpha);
            }

            y = top + (h + (st.themed ? 3.0F : 1.0F));
         }

         float gridX = cx - gridW / 2.0F;
         float gridH = rowsPer * rowH + (st.themed ? pad * 2.0F : 0.0F);
         float px0 = cx - panelW / 2.0F - (st.themed ? 0.0F : 1.0F);
         float pw = panelW + (st.themed ? 0.0F : 2.0F);
         panel(st, look, px0, y - (st.themed ? 0.0F : 1.0F), pw, gridH + (st.themed ? 0.0F : 1.0F), r3, alpha);
         float gridTop = y + (st.themed ? pad : 0.0F);
         float stagger = count <= 1 ? 0.0F : Math.min(0.012F, 0.14F / count);
         QUADS.begin();

         for (int i = 0; i < this.order.size(); i++) {
            TabRenderer.Row row = this.order.get(i);
            if (i < count) {
               int col = i / rowsPer;
               int line = i % rowsPer;
               float tx = gridX + col * (colW + 5.0F);
               float ty = gridTop + line * rowH;
               if (!Float.isNaN(row.x) && !instant) {
                  row.x = Easing.approach(row.x, tx, 14.0F * speed, dt);
                  row.y = Easing.approach(row.y, ty, 14.0F * speed, dt);
               } else {
                  row.x = tx;
                  row.y = ty;
               }
            }

            if (!Float.isNaN(row.x)) {
               float fadeIn = this.opening && !instant ? clamp((this.sinceOpen - i * stagger) / 0.12F) : 1.0F;
               float a;
               row.rowAlpha = a = alpha * Easing.outCubic(Math.min(row.appear, fadeIn));
               if (!(a <= 0.01F)) {
                  float slide = (1.0F - Math.min(1.0F, row.appear)) * -8.0F;
                  float rx = row.x + slide;
                  if (st.themed) {
                     if (row.e.me && st.highlightMe) {
                        QUADS.rect(rx, row.y, colW, rowH - 1.0F, ColorUtil.mulAlpha(look.colors.accent | 0xFF000000, 0.16F * a));
                        QUADS.rect(rx, row.y, 1.5F, rowH - 1.0F, ColorUtil.mulAlpha(look.colors.accent | 0xFF000000, 0.95F * a));
                     } else if (st.alternate && i % rowsPer % 2 == 1) {
                        QUADS.rect(rx, row.y, colW, rowH - 1.0F, ColorUtil.mulAlpha(look.colors.text | 0xFF000000, 0.05F * a));
                     }
                  } else {
                     QUADS.rect(rx, row.y, colW, 8.0F, ColorUtil.mulAlpha(553648127, a));
                  }

                  if (st.ping != 3 && st.ping != 1 && st.themed) {
                     bars(rx + colW - 11.0F - pad, row.y, row.e.ping, a);
                  }
               }
            }
         }

         QUADS.end();
         if (f.heads) {
            Ui.beginImageBatch();

            try {
               for (int var58 = 0; var58 < this.order.size(); var58++) {
                  TabRenderer.Row rowx = this.order.get(var58);
                  if (!Float.isNaN(rowx.x) && !(rowx.rowAlpha <= 0.01F)) {
                     float hx = rowx.x + (1.0F - Math.min(1.0F, rowx.appear)) * -8.0F + (st.themed ? pad : 0.0F);
                     head(st, rowx.e, hx, rowx.y + (st.themed ? 0.5F : 0.0F), rowx.rowAlpha);
                  }
               }
            } finally {
               Ui.endImageBatch();
            }
         }

         if (st.modFont) {
            HudFont.beginBatch();
         }

         try {
            for (int var59 = 0; var59 < this.order.size(); var59++) {
               TabRenderer.Row rowx = this.order.get(var59);
               if (!Float.isNaN(rowx.x) && !(rowx.rowAlpha <= 0.01F)) {
                  this.drawRow(st, look, f, rowx, rowx.x + (1.0F - Math.min(1.0F, rowx.appear)) * -8.0F, rowx.y, colW, scoreW, pingW, pad, base, shadow);
               }
            }
         } finally {
            if (st.modFont) {
               HudFont.endBatch();
            }
         }

         if (st.themed) {
            HudPanel.foreground(px0, y, pw, gridH, r3, look, alpha);
         }

         this.lastX = px0;
         this.lastY = top;
         this.lastW = pw;
         y += gridH + (st.themed ? 3.0F : 1.0F);
         if (footer != null && !footer.isEmpty()) {
            float h = footer.size() * 9.0F + (st.themed ? pad * 2.0F : 1.0F);
            float hx = cx - panelW / 2.0F - (st.themed ? 0.0F : 1.0F);
            float hw = panelW + (st.themed ? 0.0F : 2.0F);
            panel(st, look, hx, y - (st.themed ? 0.0F : 1.0F), hw, h, r3, alpha);
            float ty = y + (st.themed ? pad : 0.0F);

            for (String s : footer) {
               text(st, s, cx - width(st, s) / 2.0F, ty, ColorUtil.mulAlpha(base, alpha), shadow);
               ty += 9.0F;
            }

            if (st.themed) {
               HudPanel.foreground(hx, y, hw, h, r3, look, alpha);
            }

            y += h;
         }

         this.lastH = y - top;
         HudFont.setOnLight(false);
         GlStateManager.popMatrix();
      }
   }

   private static float clamp(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }

   private static void panel(TabRenderer.Style st, HudLook look, float x, float y, float w, float h, float r, float alpha) {
      if (look.background) {
         if (st.themed) {
            HudPanel.background(x, y, w, h, r, look, alpha * st.bgOpacity);
         } else {
            Ui.rect(x, y, w, h, ColorUtil.mulAlpha(Integer.MIN_VALUE, alpha * st.bgOpacity));
         }
      }
   }

   private static void bars(float x, float y, int ms, float a) {
      int lvl = level(ms);
      int filled = lvl == 5 ? 0 : 5 - lvl;
      int color = pingColor(ms);

      for (int b = 0; b < 5; b++) {
         float bh = 2.0F + b * 1.25F;
         int c = b < filled ? color | 0xFF000000 : 1090519039;
         QUADS.rect(x + b * 2.0F, y + 7.5F - bh, 1.25F, bh, ColorUtil.mulAlpha(c, a));
      }
   }

   private void drawRow(
      TabRenderer.Style st,
      HudLook look,
      TabRenderer.Frame f,
      TabRenderer.Row row,
      float x,
      float y,
      float colW,
      float scoreW,
      float pingW,
      float pad,
      int base,
      boolean shadow
   ) {
      float a = row.rowAlpha;
      TabRenderer.Entry en = row.e;
      float tx = x + (st.themed ? pad : 0.0F);
      float ty = y + (st.themed ? 1.0F : 0.0F);
      if (f.heads) {
         tx += 9.0F;
      }

      float right = x + colW - (st.themed ? pad : 0.0F);
      float scoreRight = right - pingW - 1.0F;
      float nameMax = scoreRight - (st.showScores && f.scores != 0 ? scoreW + 1.0F : 0.0F) - tx;
      int nameColor = en.spectator ? ColorUtil.mulAlpha(base, 0.56F) : base;
      String name = fit(st, en.spectator ? "§o" + en.name : en.name, nameMax);
      float nm = Easing.outCubic(row.nameMix);
      if (row.oldName != null && row.nameMix < 1.0F) {
         text(st, fit(st, en.spectator ? "§o" + row.oldName : row.oldName, nameMax), tx, ty, ColorUtil.mulAlpha(nameColor, a * (1.0F - nm)), shadow);
         text(st, name, tx, ty, ColorUtil.mulAlpha(nameColor, a * nm), shadow);
      } else if (en.me && st.names != null && a * 255.0F >= 6.0F) {
         NametagElement.drawLine(st.names, st.modFont, name, tx, ty, ColorUtil.mulAlpha(nameColor, a), shadow);
         if (!st.modFont) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
         }
      } else {
         text(st, name, tx, ty, ColorUtil.mulAlpha(nameColor, a), shadow);
      }

      float pingX = right;
      if (st.ping != 3) {
         if (st.ping == 0 || st.ping == 2) {
            if (!st.themed) {
               vanillaBars(right - 11.0F, y, en.ping, a);
            }

            pingX = right - 12.0F;
         }

         if (st.ping == 1 || st.ping == 2) {
            float pm = Easing.outCubic(row.pingMix);
            String now = pingText(en.ping);
            if (row.pingMix < 1.0F) {
               String old = pingText(row.oldPing);
               text(st, "§r" + old, pingX - width(st, old), ty, ColorUtil.mulAlpha(pingColor(row.oldPing), a * (1.0F - pm)), shadow);
            }

            text(st, "§r" + now, pingX - width(st, now), ty, ColorUtil.mulAlpha(pingColor(en.ping), a * (row.pingMix < 1.0F ? pm : 1.0F)), shadow);
         }
      }

      if (st.showScores && f.scores != 0 && !en.spectator) {
         float sx = Math.max(tx, scoreRight - scoreW);
         if (f.scores == 1) {
            float sm = Easing.outCubic(row.scoreMix);
            String now = "§e" + en.score;
            if (row.scoreMix < 1.0F) {
               String old = "§e" + row.oldScore;
               text(st, old, scoreRight - width(st, old), ty - 4.0F * sm, ColorUtil.mulAlpha(-1, a * (1.0F - sm)), shadow);
               text(st, now, scoreRight - width(st, now), ty + 4.0F * (1.0F - sm), ColorUtil.mulAlpha(-1, a * sm), shadow);
            } else {
               text(st, now, scoreRight - width(st, now), ty, ColorUtil.mulAlpha(-1, a), shadow);
            }
         } else {
            hearts(st, en.score, sx, ty, Math.min(scoreW, scoreRight - sx), a, shadow);
         }
      }
   }

   private static String fit(TabRenderer.Style st, String t, float maxW) {
      if (maxW <= 0.0F) {
         return "";
      } else if (width(st, t) <= maxW + 0.5F) {
         return t;
      } else {
         return st.modFont ? HudFont.trimToWidth(t, maxW) : font().trimStringToWidth(t, (int)maxW);
      }
   }

   private static void head(TabRenderer.Style st, TabRenderer.Entry en, float x, float y, float a) {
      if (en.skin != null) {
         Minecraft mc = Minecraft.getMinecraft();
         mc.getTextureManager().bindTexture(en.skin);
         ITextureObject t = mc.getTextureManager().getTexture(en.skin);
         if (t != null) {
            int id = t.getGlTextureId();
            int c = ColorUtil.mulAlpha(-1, a);
            float v0 = en.upsideDown ? 0.25F : 0.125F;
            float v1 = en.upsideDown ? 0.125F : 0.25F;
            Ui.roundedImagePair(id, x, y, 8.0F, 8.0F, st.roundedHeads ? 1.6F : 0.0F, 0.125F, v0, 0.25F, v1, st.hat && en.hat, 0.625F, v0, 0.75F, v1, c);
         }
      }
   }

   private static void vanillaBars(float x, float y, int ms, float a) {
      Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.icons);
      GlStateManager.enableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, a);
      int lvl = level(ms);
      float u0 = 0.0F;
      float v0 = (176.0F + lvl * 8.0F) / 256.0F;
      float u1 = 0.0390625F;
      float v1 = (184.0F + lvl * 8.0F) / 256.0F;
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_TEX);
      wr.pos(x, y + 8.0F, 0.0).tex(u0, v1).endVertex();
      wr.pos(x + 10.0F, y + 8.0F, 0.0).tex(u1, v1).endVertex();
      wr.pos(x + 10.0F, y, 0.0).tex(u1, v0).endVertex();
      wr.pos(x, y, 0.0).tex(u0, v0).endVertex();
      tess.draw();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void hearts(TabRenderer.Style st, int score, float x, float y, float w, float a, boolean shadow) {
      int full = score / 2;
      boolean half = score % 2 == 1;
      int shown = Math.max(10, (score + 1) / 2);
      float step = Math.min((w - 2.0F) / shown, 9.0F);
      if (step > 3.0F) {
         for (int i = 0; i < shown; i++) {
            float hx = x + i * step;
            IslandIcons.draw(IslandIcons.Icon.HEART, hx, y - 0.5F, 8.0F, ColorUtil.mulAlpha(1090519039, a));
            if (i < full) {
               IslandIcons.draw(IslandIcons.Icon.HEART, hx, y - 0.5F, 8.0F, ColorUtil.mulAlpha(i >= 10 ? -11443 : -45747, a));
            } else if (i == full && half) {
               IslandIcons.draw(IslandIcons.Icon.HEART, hx, y - 0.5F, 8.0F, ColorUtil.mulAlpha(-45747, 0.5F * a));
            }
         }
      } else {
         float f = Math.max(0.0F, Math.min(1.0F, score / 20.0F));
         int color = 0xFF000000 | (int)((1.0F - f) * 255.0F) << 16 | (int)(f * 255.0F) << 8;
         String s = score / 2.0F + "hp";
         text(st, s, x + (w - width(st, s)) / 2.0F, y, ColorUtil.mulAlpha(color, a), shadow);
      }
   }

   void clear() {
      this.rows.clear();
      this.last = null;
      this.open = 0.0F;
      this.opening = false;
   }

   static final class Entry {
      final String key;
      String name = "";
      ResourceLocation skin;
      int ping;
      int score;
      boolean spectator;
      boolean hat = true;
      boolean upsideDown;
      boolean me;

      Entry(String key) {
         this.key = key;
      }
   }

   static final class Frame {
      final List<TabRenderer.Entry> entries = new ArrayList<>();
      List<String> header;
      List<String> footer;
      boolean heads;
      int scores;
   }

   static final class QuadBatch {
      private final float[] xs = new float[4096];
      private final int[] colors = new int[1024];
      private int n;

      void begin() {
         this.n = 0;
      }

      void rect(float x, float y, float w, float h, int color) {
         if (this.n != this.colors.length && color >>> 24 != 0) {
            int i = this.n * 4;
            this.xs[i] = x;
            this.xs[i + 1] = y;
            this.xs[i + 2] = w;
            this.xs[i + 3] = h;
            this.colors[this.n++] = color;
         }
      }

      void end() {
         if (this.n != 0) {
            float al = Ui.alpha();
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

            for (int k = 0; k < this.n; k++) {
               int c = al < 1.0F ? ColorUtil.mulAlpha(this.colors[k], al) : this.colors[k];
               int ca = c >>> 24;
               int cr = c >> 16 & 0xFF;
               int cg = c >> 8 & 0xFF;
               int cb = c & 0xFF;
               float x = this.xs[k * 4];
               float y = this.xs[k * 4 + 1];
               float w = this.xs[k * 4 + 2];
               float h = this.xs[k * 4 + 3];
               wr.pos(x, y + h, 0.0).color(cr, cg, cb, ca).endVertex();
               wr.pos(x + w, y + h, 0.0).color(cr, cg, cb, ca).endVertex();
               wr.pos(x + w, y, 0.0).color(cr, cg, cb, ca).endVertex();
               wr.pos(x, y, 0.0).color(cr, cg, cb, ca).endVertex();
            }

            tess.draw();
            GlStateManager.enableTexture2D();
            this.n = 0;
         }
      }
   }

   private static final class Row {
      TabRenderer.Entry e;
      float x = Float.NaN;
      float y = Float.NaN;
      float appear;
      boolean present;
      int oldScore;
      float scoreMix = 1.0F;
      int oldPing;
      float pingMix = 1.0F;
      String oldName;
      float nameMix = 1.0F;
      float rowAlpha;

      private Row() {
      }
   }

   static final class Style {
      boolean themed = true;
      boolean modFont = true;
      boolean shadow = true;
      boolean roundedHeads = true;
      boolean hat = true;
      boolean alternate = true;
      boolean highlightMe = true;
      boolean headerFooter = true;
      boolean showScores = true;
      int ping;
      float bgOpacity = 1.0F;
      NametagElement names;
   }
}
