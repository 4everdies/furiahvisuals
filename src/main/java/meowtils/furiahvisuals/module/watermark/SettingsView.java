package meowtils.furiahvisuals.module.watermark;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;

final class SettingsView {
   static final int ACT_ALL = 100;
   static final int ACT_CHIP = 200;
   static final int ACT_THEME = 300;
   static final int ACT_POS = 400;
   static final int ACT_SOUND = 450;
   static final int ACT_MODULES = 460;
   private static final String MODULES = "Modules";
   private static final String[] IDS = new String[]{
      "target_aura", "hit_effect", "kill_effects", "landing_impact", "trails", "weapons", "glow", "particles", "ambience", "dynamic_skies", "motion_blur", "ui"
   };
   private static final String[] SHORT = new String[]{
      "Aura", "Hit", "Kills", "Landing", "Trails", "Weapons", "Glow", "Particles", "Ambience", "Sky", "Blur", "UI"
   };
   private static final IslandIcons.Icon[] ICONS = new IslandIcons.Icon[]{
      IslandIcons.Icon.AURA,
      IslandIcons.Icon.HIT,
      IslandIcons.Icon.SKULL,
      IslandIcons.Icon.LANDING,
      IslandIcons.Icon.TRAIL,
      IslandIcons.Icon.SWORD,
      IslandIcons.Icon.GLOW,
      IslandIcons.Icon.PARTICLES,
      IslandIcons.Icon.WIND,
      IslandIcons.Icon.SKY,
      IslandIcons.Icon.MOTION,
      IslandIcons.Icon.UI
   };
   private final WaterMarkModule wm;
   private final List<Module> sections = new ArrayList<>();
   private final List<String> labels = new ArrayList<>();
   private final List<IslandIcons.Icon> icons = new ArrayList<>();
   private final float[] labelW = new float[IDS.length];
   private final float[] chipGlow = new float[IDS.length];
   private float allKnob = -1.0F;
   private int measuredPx;
   private boolean narrow;
   private float allLabelW;
   private float modulesLabelW;
   private float allEnd;
   private float chipsStart;
   private float chipsEnd;
   private float themesStart;
   private float themesEnd;
   private float posStart;
   private float soundStart;
   private float modulesStart;
   private float width;
   private final float[] chipX = new float[IDS.length];
   private final float[] chipW = new float[IDS.length];

   SettingsView(WaterMarkModule wm) {
      this.wm = wm;
   }

   private void collect() {
      if (this.sections.isEmpty()) {
         for (int i = 0; i < IDS.length; i++) {
            Module m = FuriahsVisuals.instance.getModules().get(IDS[i]);
            if (m != null) {
               this.sections.add(m);
               this.labels.add(SHORT[i]);
               this.icons.add(ICONS[i]);
            }
         }
      }
   }

   List<Module> sections() {
      this.collect();
      return this.sections;
   }

   void setNarrow(boolean narrow) {
      this.narrow = narrow;
   }

   private float chipIcon(IslandStyle s) {
      return s.icon * 0.95F;
   }

   private float swatch(IslandStyle s) {
      return s.icon * 0.42F;
   }

   private float screenW(IslandStyle s) {
      return s.icon * 2.1F;
   }

   private float switchW(IslandStyle s) {
      return s.icon * 1.9F;
   }

   float measure(IslandDraw d, IslandStyle s, boolean asNarrow) {
      this.collect();
      int px = d.pixels(s.small);
      if (px != this.measuredPx) {
         this.measuredPx = px;

         for (int i = 0; i < this.labels.size(); i++) {
            this.labelW[i] = d.width(false, s.small, this.labels.get(i));
         }

         this.allLabelW = d.width(true, s.small, "All");
         this.modulesLabelW = d.width(false, s.small, "Modules");
      }

      float w = s.pad + this.switchW(s) + s.gap + this.allLabelW + s.pad;

      for (int i = 0; i < this.sections.size(); i++) {
         w += this.chipWidth(s, i, asNarrow) + (i == 0 ? s.pad * 0.6F : s.gap * 0.6F);
      }

      w += s.pad * 0.6F;
      w += s.pad + Theme.Style.values().length * this.swatch(s) * 2.0F + (Theme.Style.values().length - 1) * s.gap + s.pad;
      w += s.pad + this.screenW(s) + s.pad;
      w += s.pad + s.icon + s.pad;
      float var12;
      return var12 = w + (s.pad * 0.6F + this.modulesChipWidth(s, asNarrow) + s.pad * 0.6F);
   }

   private float chipWidth(IslandStyle s, int i, boolean asNarrow) {
      float w = s.gap * 1.2F + this.chipIcon(s) + s.gap * 1.2F;
      if (!asNarrow) {
         w += this.labelW[i] + s.gap * 0.3F;
      }

      return w;
   }

   private float modulesChipWidth(IslandStyle s, boolean asNarrow) {
      float w = s.gap * 1.2F + this.chipIcon(s) + s.gap * 0.5F + s.icon * 0.6F + s.gap * 0.9F;
      if (!asNarrow) {
         w += s.gap * 0.3F + this.modulesLabelW;
      }

      return w;
   }

   void draw(IslandDraw d, IslandStyle s, float x, float cy, float dt) {
      this.collect();
      float start = x;
      x += s.pad;
      boolean anyOn = false;
      boolean allOn = !this.sections.isEmpty();

      for (Module m : this.sections) {
         anyOn |= m.isEnabled();
         allOn &= m.isEnabled();
      }

      float target = allOn ? 1.0F : 0.0F;
      this.allKnob = this.allKnob < 0.0F ? target : Easing.approach(this.allKnob, target, 18.0F, dt);
      float sw = this.switchW(s);
      float sh = s.icon * 1.05F;
      ModulesPanel.control(d, x, cy - sh / 2.0F, sw, sh, "quick All");
      int track = ColorUtil.lerp(ColorUtil.withAlpha(s.text, 0.2F), s.accentAt(x + this.switchW(s) / 2.0F), this.allKnob);
      d.roundRect(x, cy - sh / 2.0F, sw, sh, sh / 2.0F, track);
      float r = sh / 2.0F - 1.6F;
      float kx = x + 1.6F + r + (sw - 3.2F - 2.0F * r) * this.allKnob;
      d.circle(kx, cy, r, -1);
      if (anyOn && !allOn) {
         d.circle(x + sw * 0.5F, cy, 1.2F, s.text);
      }

      float var28;
      d.textMiddle(true, s.small, "All", var28 = x + (sw + s.gap), cy, s.text);
      float var29;
      this.allEnd = (var29 = var28 + (this.allLabelW + s.pad)) - x;
      d.hairline(var29, cy - s.height * 0.25F, s.height * 0.5F, s.separator);
      this.chipsStart = var29 - x;

      for (int i = 0; i < this.sections.size(); i++) {
         float f = i == 0 ? s.pad * 0.6F : s.gap * 0.6F;
         Module m = this.sections.get(i);
         float cw = this.chipWidth(s, i, this.narrow);
         this.chipX[i] = (x = var29 + f) - start;
         this.chipW[i] = cw;
         boolean on = m.isEnabled();
         this.chipGlow[i] = Easing.approach(this.chipGlow[i], on ? 1.0F : 0.0F, 14.0F, dt);
         float ch = s.height * 0.72F;
         int accent = s.accentAt(x + cw / 2.0F);
         ModulesPanel.control(d, x, cy - ch / 2.0F, cw, ch, "quick " + this.labels.get(i));
         if (this.chipGlow[i] > 0.01F) {
            d.roundRect(x, cy - ch / 2.0F, cw, ch, ch / 2.0F, ColorUtil.withAlpha(accent, (s.light ? 0.16F : 0.22F) * this.chipGlow[i]));
         }

         float ic = this.chipIcon(s);
         int color = ColorUtil.lerp(s.muted, accent, this.chipGlow[i]);
         d.icon(this.icons.get(i), x + s.gap * 1.2F, cy - ic / 2.0F, ic, color);
         if (!this.narrow) {
            d.textMiddle(false, s.small, this.labels.get(i), x + s.gap * 1.2F + ic + s.gap * 0.3F, cy, ColorUtil.lerp(s.muted, s.text, this.chipGlow[i]));
         }

         var29 = x + cw;
      }

      this.chipsEnd = (x = var29 + s.pad * 0.6F) - start;
      d.hairline(x, cy - s.height * 0.25F, s.height * 0.5F, s.separator);
      this.themesStart = x - start;
      x += s.pad;
      Theme.Style current = FuriahsVisuals.instance.getTheme().get();
      float sr = this.swatch(s);

      for (Theme.Style style : Theme.Style.values()) {
         float cx = x + sr;
         ModulesPanel.control(d, cx - sr, cy - sr, sr * 2.0F, sr * 2.0F, "quick theme " + style.name());
         if (style == current) {
            d.outline(cx - sr - 2.2F, cy - sr - 2.2F, sr * 2.0F + 4.4F, sr * 2.0F + 4.4F, sr + 2.2F, 1.2F, s.accentAt(cx));
         }

         swatchFill(d, s, style, cx, cy, sr);
         x += sr * 2.0F + s.gap;
      }

      float var33;
      this.themesEnd = (var33 = x + (s.pad - s.gap)) - start;
      d.hairline(var33, cy - s.height * 0.25F, s.height * 0.5F, s.separator);
      this.posStart = var33 - start;
      float scrW = this.screenW(s);
      float scrH = s.icon * 1.3F;
      float sy = cy - scrH / 2.0F;
      ModulesPanel.control(d, x = var33 + s.pad, sy, scrW, scrH, "quick position");
      d.outline(x, sy, scrW, scrH, 2.5F, 1.0F, ColorUtil.withAlpha(s.text, 0.55F));
      WaterMarkModule.Anchor at = this.wm.anchor.get();

      for (WaterMarkModule.Anchor a : WaterMarkModule.Anchor.values()) {
         float dx = x + scrW * dotX(a);
         float dy = sy + scrH * dotY(a);
         if (a == at) {
            d.circle(dx, dy, 2.1F, s.accentAt(dx));
         } else {
            d.circle(dx, dy, 1.4F, ColorUtil.withAlpha(s.text, 0.5F));
         }
      }

      float var35;
      d.hairline(var35 = x + (scrW + s.pad), cy - s.height * 0.25F, s.height * 0.5F, s.separator);
      this.soundStart = var35 - start;
      boolean muted = FuriahsVisuals.instance.getSounds().mute.get();
      ModulesPanel.control(d, x = var35 + s.pad, cy - s.icon / 2.0F, s.icon, s.icon, "quick sounds");
      d.icon(muted ? IslandIcons.Icon.SPEAKER_OFF : IslandIcons.Icon.SPEAKER, x, cy - s.icon / 2.0F, s.icon, muted ? s.muted : s.text);
      float var37;
      d.hairline(var37 = x + (s.icon + s.pad), cy - s.height * 0.25F, s.height * 0.5F, s.separator);
      this.modulesStart = var37 - start;
      float mw = this.modulesChipWidth(s, this.narrow);
      float mh = s.height * 0.72F;
      ModulesPanel.control(d, x = var37 + s.pad * 0.6F, cy - mh / 2.0F, mw, mh, "quick Modules");
      d.roundRect(x, cy - mh / 2.0F, mw, mh, mh / 2.0F, ColorUtil.withAlpha(s.text, s.light ? 0.08F : 0.1F));
      float ic = this.chipIcon(s);
      float mx = x + s.gap * 1.2F;
      d.icon(IslandIcons.Icon.LIST, mx, cy - ic / 2.0F, ic, s.text);
      mx += ic;
      if (!this.narrow) {
         float var62;
         d.textMiddle(false, s.small, "Modules", var62 = mx + s.gap * 0.3F, cy, s.text);
         mx = var62 + this.modulesLabelW;
      }

      float ai = s.icon * 0.6F;
      d.icon(IslandIcons.Icon.FORWARD, mx + s.gap * 0.5F, cy - ai / 2.0F, ai, ColorUtil.withAlpha(s.text, 0.6F));
      float var39;
      this.width = (var39 = x + (mw + s.pad * 0.6F)) - start;
   }

   private static void swatchFill(IslandDraw d, IslandStyle s, Theme.Style style, float cx, float cy, float r) {
      switch (style) {
         case LIGHT:
            d.circle(cx, cy, r, -788999);
            d.outline(cx - r, cy - r, r * 2.0F, r * 2.0F, r, 0.8F, 855638016);
            break;
         case LIQUID_GLASS:
            d.circle(cx, cy, r, -6310186);
            d.circle(cx - r * 0.3F, cy - r * 0.3F, r * 0.45F, -855638017);
            break;
         case TINTED_GLASS:
            d.circle(cx, cy, r, s.accentAt(cx));
            d.circle(cx - r * 0.3F, cy - r * 0.3F, r * 0.45F, -1711276033);
            break;
         default:
            d.circle(cx, cy, r, -15329249);
            d.outline(cx - r, cy - r, r * 2.0F, r * 2.0F, r, 0.8F, 1442840575);
      }
   }

   private static float dotX(WaterMarkModule.Anchor a) {
      switch (a) {
         case TOP_LEFT:
         case BOTTOM_LEFT:
            return 0.2F;
         case TOP_RIGHT:
         case BOTTOM_RIGHT:
            return 0.8F;
         default:
            return 0.5F;
      }
   }

   private static float dotY(WaterMarkModule.Anchor a) {
      return a != WaterMarkModule.Anchor.BOTTOM_LEFT && a != WaterMarkModule.Anchor.BOTTOM_RIGHT ? 0.26F : 0.74F;
   }

   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      float lx = mx - x;
      float top = cy - s.height / 2.0F;
      if (lx < 0.0F || lx > this.width) {
         return false;
      } else if (lx < this.allEnd) {
         boolean allOn = !this.sections.isEmpty();

         for (Module m : this.sections) {
            allOn &= m.isEnabled();
         }

         hit.set(
            null,
            x + s.pad * 0.5F,
            top + 3.0F,
            this.allEnd - s.pad,
            s.height - 6.0F,
            100,
            allOn ? "Turn every visual section off" : "Turn every visual section on",
            true
         );
         return true;
      } else {
         for (int i = 0; i < this.sections.size(); i++) {
            if (lx >= this.chipX[i] && lx < this.chipX[i] + this.chipW[i]) {
               Module m = this.sections.get(i);
               hit.set(
                  null,
                  x + this.chipX[i],
                  cy - s.height * 0.36F,
                  this.chipW[i],
                  s.height * 0.72F,
                  200 + i,
                  m.getName() + (m.isEnabled() ? ": on (click to turn off)" : ": off (click to turn on)"),
                  true
               );
               return true;
            }
         }

         if (lx >= this.themesStart && lx < this.themesEnd) {
            Theme.Style[] styles = Theme.Style.values();
            float sr = this.swatch(s);
            float sx = x + this.themesStart + s.pad;

            for (int ix = 0; ix < styles.length; ix++) {
               float cx = sx + sr + ix * (sr * 2.0F + s.gap);
               if (Math.abs(mx - cx) <= sr + s.gap / 2.0F) {
                  hit.set(null, cx - sr - 3.0F, cy - sr - 3.0F, sr * 2.0F + 6.0F, sr * 2.0F + 6.0F, 300 + ix, "Theme: " + styles[ix].displayName(), true);
                  return true;
               }
            }

            return false;
         } else if (lx >= this.modulesStart) {
            hit.set(
               null,
               x + this.modulesStart + s.pad * 0.6F,
               cy - s.height * 0.36F,
               this.modulesChipWidth(s, this.narrow),
               s.height * 0.72F,
               460,
               "The island's modules: every one by name, to turn on or off",
               true
            );
            return true;
         } else if (lx >= this.soundStart) {
            hit.set(
               null,
               x + this.soundStart + s.pad * 0.5F,
               cy - s.icon * 0.8F,
               s.icon + s.pad,
               s.icon * 1.6F,
               450,
               FuriahsVisuals.instance.getSounds().mute.get() ? "Mod sounds: off (click to turn on)" : "Mod sounds",
               true
            );
            return true;
         } else {
            if (lx >= this.posStart) {
               float scrW = this.screenW(s);
               float scrH = s.icon * 1.3F;
               float sx = x + this.posStart + s.pad;
               float sy = cy - scrH / 2.0F;
               Enum best = null;
               float bestD = Float.MAX_VALUE;

               for (WaterMarkModule.Anchor a : WaterMarkModule.Anchor.values()) {
                  float dx = sx + scrW * dotX(a) - mx;
                  float dy;
                  float dd = dx * dx + (dy = sy + scrH * dotY(a) - my) * dy;
                  if (dd < bestD) {
                     bestD = dd;
                     best = a;
                  }
               }

               if (best != null) {
                  hit.set(
                     null,
                     sx - 3.0F,
                     sy - 3.0F,
                     scrW + 6.0F,
                     scrH + 6.0F,
                     400 + best.ordinal(),
                     "Move the island: " + ((WaterMarkModule.Anchor)best).displayName(),
                     true
                  );
                  return true;
               }
            }

            return false;
         }
      }
   }

   float[] chipCenterForTest(int n) {
      return n >= 0 && n < this.sections.size() ? new float[]{this.chipX[n] + this.chipW[n] / 2.0F} : null;
   }

   void click(int action) {
      if (action == 100) {
         boolean allOn = !this.sections.isEmpty();

         for (Module m : this.sections) {
            allOn &= m.isEnabled();
         }

         this.wm.setAllSections(!allOn);
         this.wm.islandToggle(!allOn);
      } else if (action >= 200 && action < 200 + this.sections.size()) {
         Module m = this.sections.get(action - 200);
         m.setEnabled(!m.isEnabled());
         this.wm.islandToggle(m.isEnabled());
      } else if (action >= 300 && action < 300 + Theme.Style.values().length) {
         FuriahsVisuals.instance.getTheme().set(Theme.Style.values()[action - 300]);
         this.wm.islandClick();
      } else if (action >= 400 && action < 400 + WaterMarkModule.Anchor.values().length) {
         this.wm.anchor.set(WaterMarkModule.Anchor.values()[action - 400]);
         this.wm.islandClick();
      } else if (action == 450) {
         FuriahsVisuals.instance.toggleSoundMute();
         this.wm.islandToggle(!FuriahsVisuals.instance.getSounds().mute.get());
      } else if (action == 460) {
         this.wm.openModulesRow(true);
      }
   }
}
