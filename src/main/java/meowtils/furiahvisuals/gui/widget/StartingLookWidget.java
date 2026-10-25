package meowtils.furiahvisuals.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

public final class StartingLookWidget extends Widget {
   public static final int SUBTLE = 0;
   public static final int BALANCED = 1;
   public static final int INTENSE = 2;
   private static final String[] NAMES = new String[]{"Subtle", "Balanced", "Intense"};
   private static final String[] TIPS = new String[]{
      "Smaller, softer, calmer: sizes, strengths and motion a third of the way down (colors stay as they are)",
      "Every size, strength and motion setting at its default (colors stay as they are)",
      "Bigger, stronger, livelier: sizes, strengths and motion a third of the way up (colors stay as they are)"
   };
   private final Supplier<List<SliderSetting>> targets;
   private final Module.StartingLooks custom;
   private final float[] chipHover = new float[3];
   private final float[] chipX = new float[3];
   private float chipW;
   private float chipY;
   private float chipH;
   private static final String LABEL = "Starting look";
   private static final float MIN_LABEL = 46.0F;
   private String shownLabel = "Starting look";

   public StartingLookWidget(Supplier<List<SliderSetting>> targets) {
      this(targets, null);
   }

   public StartingLookWidget(Supplier<List<SliderSetting>> targets, Module.StartingLooks custom) {
      this.targets = targets;
      this.custom = custom;
   }

   public List<SliderSetting> targetsNow() {
      if (this.custom == null) {
         return this.targets.get();
      } else if (!this.custom.withPage()) {
         return this.custom.targets();
      } else {
         ArrayList<SliderSetting> all = new ArrayList<>(this.targets.get());

         for (SliderSetting s : this.custom.targets()) {
            if (!all.contains(s) && s.isVisible()) {
               all.add(s);
            }
         }

         return all;
      }
   }

   public static String tip(int look) {
      return TIPS[look];
   }

   private double valueOf(SliderSetting s, int look) {
      return this.custom != null ? this.custom.value(s, look) : value(s, look);
   }

   public static boolean notAStrength(SliderSetting s) {
      if (s.getMin() < 0.0) {
         return true;
      } else {
         String unit = s.getUnit().trim().toLowerCase(Locale.ROOT);
         if (!unit.equals("s") && !unit.equals("ms") && !unit.equals("sec") && !unit.equals("ticks") && !unit.equals("°")) {
            String l = (s.getId() + " " + s.getLabel()).toLowerCase(Locale.ROOT);
            return l.contains("fade")
               || l.contains("linger")
               || l.contains("delay")
               || l.contains("duration")
               || l.contains("time")
               || l.contains("direction")
               || l.contains("angle")
               || l.contains("offset");
         } else {
            return true;
         }
      }
   }

   public static double value(SliderSetting s, int look) {
      double d = s.getDefault();
      if (look == 0) {
         return d + (s.getMin() - d) / 3.0;
      } else {
         return look == 2 ? d + (s.getMax() - d) / 3.0 : d;
      }
   }

   public void apply(int look) {
      for (SliderSetting s : this.targetsNow()) {
         s.set(this.valueOf(s, look));
      }
   }

   public int current() {
      List<SliderSetting> list = this.targetsNow();
      if (list.isEmpty()) {
         return -1;
      } else {
         for (int look = 0; look < 3; look++) {
            boolean all = true;

            for (SliderSetting s : list) {
               double want = this.valueOf(s, look);
               double steps = Math.round((want - s.getMin()) / s.getStep());
               double snapped = Math.max(s.getMin(), Math.min(s.getMax(), s.getMin() + steps * s.getStep()));
               if (Math.abs(s.get() - snapped) > s.getStep() * 0.51) {
                  all = false;
                  break;
               }
            }

            if (all) {
               return look;
            }
         }

         return -1;
      }
   }

   private static float rowH() {
      return Density.compact() ? Density.row() + 2.0F : 30.0F;
   }

   private float chipsWidth() {
      float ts = chipText();
      float cw = 0.0F;

      for (String n : NAMES) {
         cw = Math.max(cw, Ui.width(n, ts) + 16.0F);
      }

      return cw * 3.0F + 8.0F;
   }

   private boolean wrapped() {
      return this.w > 0.0F && this.chipsWidth() + 8.0F + 46.0F > this.w;
   }

   @Override
   public float height() {
      return this.wrapped() ? rowH() + (Density.compact() ? Density.control() + 1.0F : 20.0F) + 4.0F : rowH();
   }

   @Override
   public boolean isVisible() {
      return !this.targetsNow().isEmpty();
   }

   private void layout() {
      this.chipH = Density.compact() ? Density.control() + 1.0F : 20.0F;
      this.chipY = this.wrapped() ? this.y + rowH() : this.y + (this.height() - this.chipH) / 2.0F;
      float ts = chipText();
      this.chipW = 0.0F;

      for (String n : NAMES) {
         this.chipW = Math.max(this.chipW, Ui.width(n, ts) + 16.0F);
      }

      float gap = 4.0F;
      float right = this.x + this.w;

      for (int i = 2; i >= 0; i--) {
         this.chipX[i] = right - this.chipW;
         right = this.chipX[i] - gap;
      }
   }

   private static float chipText() {
      return Density.dense() ? 7.6F : (Density.compact() ? 8.0F : 9.0F);
   }

   private int chipAt(float mx, float my) {
      if (!(my < this.chipY) && !(my >= this.chipY + this.chipH)) {
         for (int i = 0; i < 3; i++) {
            if (mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.layout();
      boolean wrap = this.wrapped();
      float labelY = this.y + rowH() / 2.0F;
      float chipsY = this.chipY + this.chipH / 2.0F;
      float ts = chipText();
      float lts = Density.text();
      int lit = this.current();
      float room = wrap ? this.w : this.chipX[0] - 8.0F - this.x;
      this.shownLabel = Ui.ellipsize("Starting look", lts, room, false);
      Ui.textMiddle(this.shownLabel, this.x, labelY, lts, Theme.TEXT, false);
      if (lit < 0 && this.shownLabel.equals("Starting look")) {
         float nx;
         float var10000 = nx = this.x + Ui.width("Starting look", lts) + 6.0F;
         String own = "· your own";
         if (var10000 + Ui.width("· your own", Density.small()) <= this.x + room) {
            Ui.textMiddle(own, nx, labelY, Density.small(), Theme.TEXT_MUTED, false);
         }
      }

      int over = this.chipAt(mx, my);

      for (int i = 0; i < 3; i++) {
         this.chipHover[i] = Easing.approach(this.chipHover[i], over == i ? 1.0F : 0.0F, 16.0F, dt);
         float cx = this.chipX[i];
         boolean on = lit == i;
         Ui.registerControl(cx, this.chipY, this.chipW, this.chipH, "look " + NAMES[i]);
         if (on) {
            Ui.roundRect(
               cx, this.chipY, this.chipW, this.chipH, this.chipH / 2.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, this.chipHover[i])
            );
            Ui.accentOutline(cx, this.chipY, this.chipW, this.chipH, this.chipH / 2.0F, 1.0F, accent);
         } else {
            Ui.surface(cx, this.chipY, this.chipW, this.chipH, this.chipH / 2.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.chipHover[i]));
         }

         String n = NAMES[i];
         Ui.textMiddle(
            n, cx + (this.chipW - Ui.width(n, ts)) / 2.0F, chipsY, ts, on ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.chipHover[i]), false
         );
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button != 0) {
         return false;
      } else {
         this.layout();
         int i = this.chipAt(mx, my);
         if (i < 0) {
            return false;
         } else {
            this.apply(i);
            ModSounds.gui(Sfx.SELECT);
            return true;
         }
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      this.layout();
      int i = this.chipAt(mx, my);
      if (i >= 0) {
         return this.custom != null ? this.custom.tooltip(i) : TIPS[i];
      } else {
         return this.contains(mx, my)
            ? (this.shownLabel.equals("Starting look") ? "" : "Starting look: ")
               + (this.custom != null ? this.custom.rowTooltip() : "A starting point for this section's sizes, strengths and motion; colors are never changed")
            : null;
      }
   }
}
