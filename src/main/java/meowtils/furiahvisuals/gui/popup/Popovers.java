package meowtils.furiahvisuals.gui.popup;

public final class Popovers {
   private static Popover current;
   private static float viewW = 1000.0F;
   private static float viewH = 600.0F;
   private static boolean pressed;

   private Popovers() {
   }

   public static void open(Popover p, float x, float y, float w, float h) {
      close();
      p.ax = x;
      p.ay = y;
      p.aw = w;
      p.ah = h;
      p.place(viewW, viewH);
      current = p;
   }

   public static Popover current() {
      return current;
   }

   public static boolean isOpen() {
      return current != null;
   }

   public static void close() {
      Popover p = current;
      current = null;
      pressed = false;
      if (p != null) {
         p.closed();
      }
   }

   public static void close(Popover p) {
      if (current == p) {
         close();
      }
   }

   public static void view(float w, float h) {
      viewW = w;
      viewH = h;
      if (current != null) {
         current.keepInside(w, h);
      }
   }

   public static void render(float mx, float my, float dt, int accent) {
      if (current != null) {
         current.render(mx, my, dt, accent);
      }
   }

   public static boolean mouseDown(float mx, float my, int button) {
      if (current == null) {
         return false;
      } else if (current.contains(mx, my)) {
         pressed = true;
         current.mouseDown(mx, my, button);
         return true;
      } else {
         close();
         return true;
      }
   }

   public static boolean mouseDrag(float mx, float my) {
      if (current != null && pressed) {
         current.mouseDrag(mx, my);
         return true;
      } else {
         return false;
      }
   }

   public static boolean mouseUp(float mx, float my) {
      if (current != null && pressed) {
         pressed = false;
         current.mouseUp(mx, my);
         return true;
      } else {
         pressed = false;
         return false;
      }
   }

   public static boolean wheel(float mx, float my, int wheel) {
      return current != null && current.contains(mx, my) && current.wheel(mx, my, wheel);
   }

   public static boolean covers(float mx, float my) {
      return current != null && current.contains(mx, my);
   }

   public static String tooltip(float mx, float my) {
      return current != null && current.contains(mx, my) ? current.tooltip(mx, my) : null;
   }
}
