package meowtils.furiahvisuals.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class UiProbe {
   private static boolean recording;
   private static int inert;
   private static float basePx;
   private static final List<UiProbe.Item> ITEMS = new ArrayList<>();
   public static boolean overlay;

   private UiProbe() {
   }

   public static void begin() {
      ITEMS.clear();
      inert = 0;
      basePx = Ui.px;
      recording = true;
   }

   public static void end() {
      recording = false;
      inert = 0;
   }

   public static boolean recording() {
      return recording;
   }

   public static void pushInert() {
      inert++;
   }

   public static void popInert() {
      if (inert > 0) {
         inert--;
      }
   }

   public static List<UiProbe.Item> items() {
      return ITEMS;
   }

   static void add(float x, float y, float w, float h, String label, boolean text) {
      if (recording && inert <= 0 && Ui.px == basePx && !(w <= 0.5F) && !(h <= 0.5F) && !(Ui.alpha() < 0.5F)) {
         float[] clip = Ui.currentClip();
         if (clip != null) {
            float x1 = Math.max(x, clip[0]);
            float y1 = Math.max(y, clip[1]);
            float x2 = Math.min(x + w, clip[0] + clip[2]);
            float y2 = Math.min(y + h, clip[1] + clip[3]);
            if (x2 - x1 <= 0.5F || y2 - y1 <= 0.5F) {
               return;
            }

            x = x1;
            y = y1;
            w = x2 - x1;
            h = y2 - y1;
         }

         ITEMS.add(new UiProbe.Item(x, y, w, h, label == null ? "" : label, text));
      }
   }

   public static List<UiProbe.Overlap> overlaps(List<UiProbe.Item> items) {
      ArrayList<UiProbe.Overlap> out = new ArrayList<>();

      for (int i = 0; i < items.size(); i++) {
         UiProbe.Item a = items.get(i);

         for (int j = i + 1; j < items.size(); j++) {
            UiProbe.Item b = items.get(j);
            float area = a.overlapArea(b);
            if (!(area < 1.0F)) {
               if (a.text && b.text) {
                  if (!a.label.equals(b.label) || !(Math.abs(a.x - b.x) < 2.0F) || !(Math.abs(a.y - b.y) < 2.0F)) {
                     out.add(new UiProbe.Overlap(a, b, area));
                  }
               } else if (a.text || b.text) {
                  UiProbe.Item text = a.text ? a : b;
                  UiProbe.Item control = a.text ? b : a;
                  if (!control.contains(text, 1.0F) && !text.contains(control, 0.5F)) {
                     out.add(new UiProbe.Overlap(a, b, area));
                  }
               } else if (!a.contains(b, 0.5F) && !b.contains(a, 0.5F)) {
                  out.add(new UiProbe.Overlap(a, b, area));
               }
            }
         }
      }

      return out;
   }

   public static void drawOverlay() {
      List<UiProbe.Overlap> bad = overlaps(ITEMS);
      Set red = Collections.newSetFromMap(new IdentityHashMap());

      for (UiProbe.Overlap o : bad) {
         red.add(o.a);
         red.add(o.b);
      }

      for (UiProbe.Item it : ITEMS) {
         int c = red.contains(it) ? -53200 : (it.text ? 1883291903 : -1606353024);
         Ui.outline(it.x, it.y, it.w, it.h, 0.0F, red.contains(it) ? 1.0F : 0.5F, c);
      }

      if (!bad.isEmpty()) {
         Ui.textMiddle(bad.size() + (bad.size() == 1 ? " overlap" : " overlaps"), 6.0F, 8.0F, 8.0F, -44976, true);
      }
   }

   public static final class Item {
      public final float x;
      public final float y;
      public final float w;
      public final float h;
      public final String label;
      public final boolean text;

      Item(float x, float y, float w, float h, String label, boolean text) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.label = label;
         this.text = text;
      }

      boolean contains(UiProbe.Item o, float slack) {
         return o.x >= this.x - slack && o.y >= this.y - slack && o.x + o.w <= this.x + this.w + slack && o.y + o.h <= this.y + this.h + slack;
      }

      float overlapArea(UiProbe.Item o) {
         float ix = Math.min(this.x + this.w, o.x + o.w) - Math.max(this.x, o.x);
         float iy = Math.min(this.y + this.h, o.y + o.h) - Math.max(this.y, o.y);
         return ix > 0.0F && iy > 0.0F ? ix * iy : 0.0F;
      }

      @Override
      public String toString() {
         return String.format(Locale.ROOT, "%s '%s' at %.1f,%.1f %.1fx%.1f", this.text ? "text" : "control", this.label, this.x, this.y, this.w, this.h);
      }
   }

   public static final class Overlap {
      public final UiProbe.Item a;
      public final UiProbe.Item b;
      public final float area;

      Overlap(UiProbe.Item a, UiProbe.Item b, float area) {
         this.a = a;
         this.b = b;
         this.area = area;
      }

      @Override
      public String toString() {
         return String.format(Locale.ROOT, "%s overlaps %s (%.1f units²)", this.a, this.b, this.area);
      }
   }
}
