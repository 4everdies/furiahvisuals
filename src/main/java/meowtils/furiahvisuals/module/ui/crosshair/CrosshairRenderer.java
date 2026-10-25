package meowtils.furiahvisuals.module.ui.crosshair;

import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

final class CrosshairRenderer {
   private static final int MAX = 96;
   private static final int LINE = 0;
   private static final int ARC = 1;
   private static final int DOT_ROUND = 2;
   private static final int DOT_SQUARE = 3;
   private static final int[] type = new int[96];
   private static final float[] a = new float[96];
   private static final float[] b = new float[96];
   private static final float[] c = new float[96];
   private static final float[] d = new float[96];
   private static final float[] e = new float[96];
   private static final float[] f = new float[96];
   private static final float[] alpha = new float[96];
   private static final boolean[] round = new boolean[96];
   private static int n;

   private CrosshairRenderer() {
   }

   private static void line(float x0, float y0, float x1, float y1, float th, boolean roundEnds, float al) {
      if (n != 96 && !(al <= 0.002F)) {
         type[n] = 0;
         a[n] = x0;
         b[n] = y0;
         c[n] = x1;
         d[n] = y1;
         e[n] = th;
         round[n] = roundEnds;
         alpha[n] = al;
         n++;
      }
   }

   private static void arc(float r, float th, float start, float sweep, boolean roundEnds, float al) {
      if (n != 96 && !(al <= 0.002F)) {
         type[n] = 1;
         a[n] = 0.0F;
         b[n] = 0.0F;
         c[n] = r;
         d[n] = th;
         e[n] = start;
         f[n] = sweep;
         round[n] = roundEnds;
         alpha[n] = al;
         n++;
      }
   }

   private static void arcAt(float x, float y, float r, float th, float al) {
      if (n != 96 && !(al <= 0.002F)) {
         type[n] = 1;
         a[n] = x;
         b[n] = y;
         c[n] = r;
         d[n] = th;
         e[n] = 0.0F;
         f[n] = 360.0F;
         round[n] = false;
         alpha[n] = al;
         n++;
      }
   }

   private static void dot(float x, float y, float diameter, boolean roundDot, float al) {
      if (n != 96 && !(al <= 0.002F) && !(diameter <= 0.0F)) {
         type[n] = roundDot ? 2 : 3;
         a[n] = x;
         b[n] = y;
         c[n] = diameter;
         alpha[n] = al;
         n++;
      }
   }

   static void draw(CrosshairDef def, CrosshairState st, CrosshairRenderer.Reactions rx, float cx, float cy, float opacity, float morph) {
      draw(def, def.style.get(), st, rx, cx, cy, opacity, morph);
   }

   static void draw(CrosshairDef def, CrosshairStyle style, CrosshairState st, CrosshairRenderer.Reactions rx, float cx, float cy, float opacity, float morph) {
      if (!(opacity <= 0.003F)) {
         float k = st.scale * morph;
         float t = st.time;
         float pulse = def.pulseAmount.getFloat();
         float wave = pulse > 0.0F ? (float)Math.sin(t * Math.PI * 2.0 * def.pulseSpeed.get()) : 0.0F;
         float sizeK = 1.0F + pulse * 0.22F * wave;
         float alphaK = 1.0F - pulse * 0.3F * (0.5F + 0.5F * wave);
         float onTarget = rx.target ? st.target * rx.targetStrength : 0.0F;
         float clickBump = rx.clickPulse && style != CrosshairStyle.CLASSIC ? st.click * rx.clickStrength : 0.0F;
         float moveSpread = rx.moveSpread && style != CrosshairStyle.CLASSIC ? Math.min(1.4F, st.move) * rx.moveStrength : 0.0F;
         float lowPulse = rx.lowHealth ? st.lowHealth * rx.lowHealthStrength * (0.75F + 0.25F * (float)Math.sin(t * 9.0)) : 0.0F;
         float var37;
         float s = def.size.getFloat() * k * (var37 = sizeK * (1.0F + clickBump * 0.18F + lowPulse * 0.08F));
         float th = def.thickness.getFloat() * k;
         float g = (def.gap.getFloat() * (1.0F - 0.5F * onTarget) + 6.0F * moveSpread) * k * var37;
         double time = t;
         int base = def.color.getColor(0.5F, time);
         if (onTarget > 0.0F) {
            base = ColorUtil.lerp(base, st.targetPlayer ? rx.targetPlayerColor : rx.targetMobColor, Math.min(1.0F, onTarget));
         }

         if (moveSpread > 0.0F) {
            base = ColorUtil.lerp(base, rx.moveColor, Math.min(1.0F, moveSpread) * 0.6F);
         }

         if (clickBump > 0.0F) {
            base = ColorUtil.lerp(base, rx.clickColor, Math.min(1.0F, clickBump));
         }

         if (lowPulse > 0.0F) {
            base = ColorUtil.lerp(base, rx.lowHealthColor, Math.min(1.0F, lowPulse));
         }

         float alphaAll = opacity * def.opacity.getFloat() * alphaK;
         CrosshairDef.Blend blend = def.blend.get();
         float spin = def.spinSpeed.getFloat() * (def.spinDirection.get() == CrosshairDef.Spin.CLOCKWISE ? 1.0F : -1.0F);
         float rot = def.rotation.getFloat() + spin * t;
         if (style == CrosshairStyle.HEX) {
            rot += 20.0F * t;
         }

         boolean snap = def.pixelPerfect.get() && rot % 360.0F == 0.0F;
         n = 0;
         boolean rounded = def.ends.get() == CrosshairDef.Ends.ROUNDED;
         switch (style) {
            case CLASSIC:
               classic(def, st, k, snap);
               break;
            case DOT:
               dot(0.0F, 0.0F, s, true, 1.0F);
               break;
            case CIRCLE:
               arc(s, th, 0.0F, 360.0F, false, 1.0F);
               break;
            case CIRCLE_CROSS: {
               arc(s, th, 0.0F, 360.0F, false, 1.0F);
               float l0 = s - s * 0.55F;
               float l1 = s + s * 0.55F;
               line(l0, 0.0F, l1, 0.0F, th, false, 1.0F);
               line(-l0, 0.0F, -l1, 0.0F, th, false, 1.0F);
               line(0.0F, l0, 0.0F, l1, th, false, 1.0F);
               line(0.0F, -l0, 0.0F, -l1, th, false, 1.0F);
               break;
            }
            case BRACKETS: {
               float ext = s * 0.55F + g * (1.0F - 0.6F * onTarget) + 2.0F * k;
               float arm = s * 0.6F;

               for (int sx = -1; sx <= 1; sx += 2) {
                  for (int sy = -1; sy <= 1; sy += 2) {
                     float px = sx * ext;
                     float py = sy * ext;
                     line(px, py, px - sx * arm, py, th, true, 1.0F);
                     line(px, py, px, py - sy * arm, th, true, 1.0F);
                  }
               }
               break;
            }
            case CHEVRON: {
               float arm = s * 1.1F;
               line(0.0F, 0.0F, -arm, arm, th, true, 1.0F);
               line(0.0F, 0.0F, arm, arm, th, true, 1.0F);
               break;
            }
            case TRIANGLE:
               for (int i = 0; i < 3; i++) {
                  double ang = Math.toRadians(-90 + i * 120);
                  float cs = (float)Math.cos(ang);
                  float sn = (float)Math.sin(ang);
                  float r0 = g + 1.0F * k;
                  float r1 = g + s;
                  line(cs * r1, sn * r1, cs * r0, sn * r0, th, true, 1.0F);
               }
               break;
            case TRI_DOT:
               for (int i = 0; i < 3; i++) {
                  double ang = Math.toRadians(-90 + i * 120);
                  float rxx = g + s * 0.5F;
                  dot((float)Math.cos(ang) * rxx, (float)Math.sin(ang) * rxx, Math.max(2.0F * k, th * 1.6F), true, 1.0F);
               }
               break;
            case DIAMOND:
               line(0.0F, -s, s, 0.0F, th, true, 1.0F);
               line(s, 0.0F, 0.0F, s, th, true, 1.0F);
               line(0.0F, s, -s, 0.0F, th, true, 1.0F);
               line(-s, 0.0F, 0.0F, -s, th, true, 1.0F);
               if (!def.dot.get()) {
                  dot(0.0F, 0.0F, Math.max(2.0F * k, th * 1.4F), true, 1.0F);
               }
               break;
            case HEX: {
               float r = s;

               for (int i = 0; i < 6; i++) {
                  double a0 = Math.toRadians(i * 60 - 90);
                  double a1 = Math.toRadians(i * 60 - 30);
                  line((float)Math.cos(a0) * r, (float)Math.sin(a0) * r, (float)Math.cos(a1) * r, (float)Math.sin(a1) * r, th, true, 1.0F);
               }

               if (!def.dot.get()) {
                  dot(0.0F, 0.0F, Math.max(2.0F * k, th * 1.4F), true, 1.0F);
               }
               break;
            }
            case SHOTGUN: {
               float r = s * 1.8F + g;
               arc(r, Math.max(0.75F * k, th * 0.75F), 0.0F, 360.0F, false, 1.0F);
               float tick = 3.5F * k;
               line(r - tick, 0.0F, r + tick, 0.0F, th, false, 1.0F);
               line(-r + tick, 0.0F, -r - tick, 0.0F, th, false, 1.0F);
               line(0.0F, r - tick, 0.0F, r + tick, th, false, 1.0F);
               line(0.0F, -r + tick, 0.0F, -r - tick, th, false, 1.0F);
               break;
            }
            case RED_DOT:
               arc(s * 1.5F, Math.max(0.75F * k, th * 0.6F), 0.0F, 360.0F, false, 0.35F);
               dot(0.0F, 0.0F, Math.max(3.0F * k, s * 0.75F), true, 1.0F);
               break;
            case SPINNER:
               int segs = def.segments.getInt();
               float turn = (120.0F + 360.0F * onTarget) * t;
               float each = 360.0F / segs;

               for (int i = 0; i < segs; i++) {
                  arc(s, th, turn + i * each, each * 0.62F, true, 1.0F);
               }
               break;
            case ORBIT: {
               dot(0.0F, 0.0F, Math.max(2.0F * k, th * 1.4F), true, 1.0F);
               int dots = def.orbiters.getInt();
               float r = s * (1.0F - 0.45F * onTarget);
               float base0 = 200.0F * t;

               for (int i = 0; i < dots; i++) {
                  double ang = Math.toRadians(base0 + i * 360.0F / dots);
                  dot((float)Math.cos(ang) * r, (float)Math.sin(ang) * r, Math.max(1.5F * k, th * 1.2F), true, 0.9F);
               }
               break;
            }
            case PULSE:
               dot(0.0F, 0.0F, Math.max(2.0F * k, th * 1.4F), true, 1.0F);
               float phase = t * 0.8F % 1.0F;
               arc(Math.max(0.5F, s * 2.0F * phase), Math.max(0.75F * k, th * 0.8F), 0.0F, 360.0F, false, (1.0F - phase) * 0.9F);
               break;
            case RETICLE: {
               float l0 = g + 1.0F * k;
               float l1 = g + s * 2.6F;
               line(l0, 0.0F, l1, 0.0F, th, false, 1.0F);
               line(-l0, 0.0F, -l1, 0.0F, th, false, 1.0F);
               line(0.0F, l0, 0.0F, l1, th, false, 1.0F);
               line(0.0F, -l0, 0.0F, -l1, th, false, 1.0F);
               arc(s * 1.3F, Math.max(0.75F * k, th * 0.75F), 0.0F, 360.0F, false, 0.8F);
            }
         }

         if (def.dot.get() && style != CrosshairStyle.CLASSIC && style != CrosshairStyle.VANILLA) {
            dot(0.0F, 0.0F, def.dotSize.getFloat() * k, def.dotShape.get() == CrosshairDef.Ends.ROUNDED, def.dotOpacity.getFloat());
         }

         GlStateManager.pushMatrix();
         GlStateManager.translate(cx, cy, 0.0F);
         if (rot % 360.0F != 0.0F) {
            GlStateManager.rotate(rot, 0.0F, 0.0F, 1.0F);
         }

         GlStateManager.disableTexture2D();
         if (def.glow.get() || style == CrosshairStyle.RED_DOT) {
            float gs = def.glow.get() ? def.glowStrength.getFloat() : 0.45F;
            setBlend(CrosshairDef.Blend.ADDITIVE);
            int gc = base;

            for (int layer = 0; layer < 2; layer++) {
               float grow = (layer == 0 ? 3.0F : 7.0F) * k;
               float ga = alphaAll * gs * (layer == 0 ? 0.4F : 0.18F);
               emit(gc, ga, grow, snap, false, rounded);
            }
         }

         if (def.outline.get()) {
            setBlend(CrosshairDef.Blend.NORMAL);
            int oc = def.outlineColor.getColor(0.5F, time);
            float ot = def.outlineThickness.getFloat() * k;
            emit(oc, alphaAll * def.outlineOpacity.getFloat(), ot * 2.0F, snap, true, rounded);
         }

         setBlend(blend);
         emit(blend == CrosshairDef.Blend.INVERT ? ColorUtil.lerp(base, -1, 0.0F) : base, alphaAll, 0.0F, snap, false, rounded);
         GlStateManager.enableTexture2D();
         GlStateManager.popMatrix();
         Ui.premultiplied(false);
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      }
   }

   static void drawExtras(CrosshairDef def, CrosshairState st, CrosshairRenderer.Reactions rx, float cx, float cy, float opacity) {
      float k = st.scale;
      GlStateManager.pushMatrix();
      GlStateManager.translate(cx, cy, 0.0F);
      GlStateManager.disableTexture2D();
      setBlend(CrosshairDef.Blend.NORMAL);
      float ring = Math.max(10.0F, extent(def) + 6.0F) * k;
      if (rx.bowCharge && st.bow >= 0.0F) {
         float strength = rx.bowStrength;
         Ui.arc(0.0F, 0.0F, ring, 2.5F * k, 0.0F, 360.0F, false, ColorUtil.mulAlpha(-16777216, 0.35F * opacity * strength));
         Ui.arc(0.0F, 0.0F, ring, 1.6F * k, 0.0F, 360.0F, false, ColorUtil.mulAlpha(rx.bowColor, 0.22F * opacity * strength));
         if (st.bow > 0.001F) {
            Ui.arc(0.0F, 0.0F, ring, 1.6F * k, 0.0F, 360.0F * Math.min(1.0F, st.bow), true, ColorUtil.mulAlpha(rx.bowColor, opacity * strength));
         }

         float flash;
         if (st.bow >= 1.0F && (flash = Math.max(0.0F, 1.0F - st.bowFull / 0.35F)) > 0.0F) {
            Ui.arc(
               0.0F, 0.0F, ring + 2.0F * k * flash, (1.6F + 4.0F * flash) * k, 0.0F, 360.0F, false, ColorUtil.mulAlpha(-1, flash * 0.8F * opacity * strength)
            );
         }
      }

      if (rx.blocking && st.blocking > 0.01F) {
         float under = (extent(def) + 5.0F) * k;
         float w = 5.0F * k;
         float mid = under + 6.0F * k;
         float bottom = under + 11.0F * k;
         int col = ColorUtil.mulAlpha(rx.blockColor, st.blocking * opacity * rx.blockStrength);
         float th = 1.5F * k;
         Ui.line(-w, under, w, under, th, true, col);
         Ui.line(-w, under, -w, mid, th, true, col);
         Ui.line(w, under, w, mid, th, true, col);
         Ui.line(-w, mid, 0.0F, bottom, th, true, col);
         Ui.line(w, mid, 0.0F, bottom, th, true, col);
      }

      GlStateManager.enableTexture2D();
      GlStateManager.popMatrix();
   }

   static float extent(CrosshairDef def) {
      return extent(def, def.style.get());
   }

   static float reachWithExtras(CrosshairDef def, CrosshairState st, CrosshairRenderer.Reactions rx) {
      float r = extent(def);
      if (rx.bowCharge && st.bow >= 0.0F) {
         r = Math.max(r, Math.max(10.0F, r + 6.0F) + 4.0F);
      }

      return r;
   }

   static float extent(CrosshairDef def, CrosshairStyle style) {
      switch (style) {
         case CLASSIC:
            float r = 0.0F;
            if (def.inner.show.get()) {
               r = Math.max(r, def.inner.offset.getFloat() + def.inner.length.getFloat());
            }

            if (def.outer.show.get()) {
               r = Math.max(r, def.outer.offset.getFloat() + def.outer.length.getFloat());
            }

            return Math.max(4.0F, r);
         case DOT:
         case CIRCLE:
         case CIRCLE_CROSS:
         case BRACKETS:
         case CHEVRON:
         case TRIANGLE:
         case TRI_DOT:
         case DIAMOND:
         case HEX:
         case SPINNER:
         case ORBIT:
         default:
            return def.size.getFloat() + Math.max(0.0F, def.gap.getFloat());
         case SHOTGUN:
            return def.size.getFloat() * 1.8F + def.gap.getFloat() + 3.5F;
         case RED_DOT:
            return def.size.getFloat() * 1.5F;
         case PULSE:
            return def.size.getFloat() * 2.0F;
         case RETICLE:
            return def.gap.getFloat() + def.size.getFloat() * 2.6F;
         case VANILLA:
            return 8.0F;
      }
   }

   private static void classic(CrosshairDef def, CrosshairState st, float k, boolean snap) {
      boolean rounded = def.ends.get() == CrosshairDef.Ends.ROUNDED;

      for (int set = 0; set < 2; set++) {
         CrosshairDef.LineSet ls = set == 0 ? def.inner : def.outer;
         if (ls.show.get()) {
            float err = 0.0F;
            float spread = 0.0F;
            if (ls.moveError.get()) {
               float m = Math.min(1.4F, st.move);
               err += m;
               spread += 8.0F * k * m * ls.moveMult.getFloat();
            }

            if (ls.clickError.get()) {
               err += st.click;
               spread += 6.0F * k * st.click * ls.clickMult.getFloat();
            }

            err = Math.min(1.0F, err);
            float al = ls.opacity.getFloat() * (ls.fade.get() ? 1.0F - 0.65F * err : 1.0F);
            float off = ls.offset.getFloat() * k + spread;
            float len = ls.length.getFloat() * k;
            float lenV = ls.linked.get() ? len : ls.lengthV.getFloat() * k;
            float lt = ls.thickness.getFloat() * k;
            if (snap) {
               lt = Math.max(1.0F, (float)Math.round(lt));
               off = Math.round(off);
               len = Math.max(0.0F, (float)Math.round(len));
               lenV = Math.max(0.0F, (float)Math.round(lenV));
            }

            boolean split = set == 0 && def.split.get() && err > 0.01F;
            float drift = split ? def.splitDistance.getFloat() * k * err : 0.0F;
            float ratio = def.splitRatio.getFloat();

            for (int dir = 0; dir < 4; dir++) {
               boolean show = dir == 0 ? def.right.get() : (dir == 1 ? def.left.get() : (dir == 2 ? def.bottom.get() : def.top.get()));
               if (show) {
                  float var10000 = dir < 2 ? len : lenV;
                  float l = dir < 2 ? len : lenV;
                  if (!(l <= 0.0F)) {
                     float sx = dir == 0 ? 1.0F : (dir == 1 ? -1.0F : 0.0F);
                     float sy = dir == 2 ? 1.0F : (dir == 3 ? -1.0F : 0.0F);
                     if (split) {
                        float innerLen = l * (1.0F - ratio);
                        seg(sx, sy, off, off + innerLen, lt, rounded, al * lerp(1.0F, def.splitInner.getFloat(), err), snap);
                        seg(sx, sy, off + innerLen + drift, off + l + drift, lt, rounded, al * def.splitOuter.getFloat(), snap);
                     } else {
                        seg(sx, sy, off, off + l, lt, rounded, al, snap);
                     }
                  }
               }
            }
         }
      }

      if (def.dot.get()) {
         float ds = def.dotSize.getFloat() * k;
         if (snap) {
            ds = Math.max(1.0F, (float)Math.round(ds));
         }

         dot(0.0F, 0.0F, ds, def.dotShape.get() == CrosshairDef.Ends.ROUNDED, def.dotOpacity.getFloat());
      }
   }

   private static void seg(float sx, float sy, float r0, float r1, float th, boolean roundEnds, float al, boolean snap) {
      if (!(r1 <= r0)) {
         line(sx * r0, sy * r0, sx * r1, sy * r1, th, roundEnds, al);
      }
   }

   private static float lerp(float x, float y, float t) {
      return x + (y - x) * t;
   }

   private static void emit(int color, float alphaAll, float grow, boolean snap, boolean outlinePass, boolean rounded) {
      for (int i = 0; i < n; i++) {
         int col = ColorUtil.mulAlpha(color | 0xFF000000, Math.min(1.0F, alphaAll * alpha[i]));
         if (col >>> 24 != 0) {
            switch (type[i]) {
               case 0:
                  float th = e[i] + grow;
                  float x0 = a[i];
                  float y0 = b[i];
                  float x1 = c[i];
                  float y1 = d[i];
                  float dy;
                  float dx;
                  float l;
                  if (grow > 0.0F && (l = (float)Math.sqrt((dx = x1 - x0) * dx + (dy = y1 - y0) * dy)) > 0.0F) {
                     float ex = dx / l * grow / 2.0F;
                     float ey = dy / l * grow / 2.0F;
                     x0 -= ex;
                     y0 -= ey;
                     x1 += ex;
                     y1 += ey;
                  }

                  if (snap && (y0 == y1 || x0 == x1) && !round[i]) {
                     float lx = Math.min(x0, x1);
                     float rx = Math.max(x0, x1);
                     float ty = Math.min(y0, y1);
                     float by = Math.max(y0, y1);
                     if (y0 == y1) {
                        float y = Math.round(y0 - th / 2.0F);
                        Ui.rect(Math.round(lx), y, Math.max(1.0F, (float)(Math.round(rx) - Math.round(lx))), Math.max(1.0F, (float)Math.round(th)), col);
                     } else {
                        float x = Math.round(x0 - th / 2.0F);
                        Ui.rect(x, Math.round(ty), Math.max(1.0F, (float)Math.round(th)), Math.max(1.0F, (float)(Math.round(by) - Math.round(ty))), col);
                     }
                  } else {
                     Ui.line(x0, y0, x1, y1, th, round[i], col);
                  }
                  break;
               case 1:
                  if (a[i] == 0.0F && b[i] == 0.0F) {
                     Ui.arc(0.0F, 0.0F, c[i], d[i] + grow, e[i], f[i], round[i], col);
                  } else {
                     GlStateManager.pushMatrix();
                     GlStateManager.translate(a[i], b[i], 0.0F);
                     Ui.arc(0.0F, 0.0F, c[i], d[i] + grow, e[i], f[i], round[i], col);
                     GlStateManager.popMatrix();
                  }
                  break;
               case 2:
                  float r = (c[i] + grow) / 2.0F;
                  Ui.roundRect(a[i] - r, b[i] - r, r * 2.0F, r * 2.0F, r, col);
                  break;
               default:
                  float h = (c[i] + grow) / 2.0F;
                  float x = a[i] - h;
                  float y = b[i] - h;
                  float w = h * 2.0F;
                  if (snap) {
                     x = Math.round(x);
                     y = Math.round(y);
                     w = Math.max(1.0F, (float)Math.round(w));
                  }

                  Ui.rect(x, y, w, w, col);
            }
         }
      }
   }

   private static void setBlend(CrosshairDef.Blend blend) {
      GlStateManager.enableBlend();
      Ui.premultiplied(true);
      switch (blend) {
         case INVERT:
            GlStateManager.tryBlendFuncSeparate(775, 771, 1, 771);
            break;
         case ADDITIVE:
            GlStateManager.tryBlendFuncSeparate(1, 1, 1, 771);
            break;
         default:
            GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
      }
   }

   static void drawVanilla(float cx, float cy, float scale, float opacity) {
      Minecraft mc = Minecraft.getMinecraft();
      mc.getTextureManager().bindTexture(Gui.icons);
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(775, 769, 1, 0);
      GlStateManager.enableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, opacity);
      float x = cx - 7.0F * scale;
      float y = cy - 7.0F * scale;
      float s = 16.0F * scale;
      float u0 = 0.0F;
      float v0 = 0.0F;
      float u1 = 0.0625F;
      float v1 = 0.0625F;
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_TEX);
      wr.pos(x, y + s, 0.0).tex(u0, v1).endVertex();
      wr.pos(x + s, y + s, 0.0).tex(u1, v1).endVertex();
      wr.pos(x + s, y, 0.0).tex(u1, v0).endVertex();
      wr.pos(x, y, 0.0).tex(u0, v0).endVertex();
      tess.draw();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
   }

   static final class Reactions {
      boolean target = true;
      boolean clickPulse = true;
      boolean bowCharge = true;
      boolean moveSpread;
      boolean lowHealth;
      boolean blocking;
      int targetPlayerColor = -50373;
      int targetMobColor = -20165;
      int clickColor = -1;
      int bowColor = -1;
      int moveColor = -1;
      int lowHealthColor = -57312;
      int blockColor = -7351297;
      float targetStrength = 1.0F;
      float clickStrength = 0.6F;
      float bowStrength = 1.0F;
      float moveStrength = 1.0F;
      float lowHealthStrength = 1.0F;
      float blockStrength = 1.0F;
   }
}
