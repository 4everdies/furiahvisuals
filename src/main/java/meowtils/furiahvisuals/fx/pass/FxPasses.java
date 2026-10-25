package meowtils.furiahvisuals.fx.pass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;

public final class FxPasses {
   private static final List<PassItem> ITEMS = new ArrayList<>();
   private static final List<FxPasses.Clock> CLOCKS = new ArrayList<>();
   private static PassItem[] order = new PassItem[16];
   private static FxPasses.Clock[] orderClocks = new FxPasses.Clock[16];
   private static final Camera CAMERA = new Camera();
   private static final Set<String> REPORTED = new HashSet<>();
   private static boolean cameraTaken;
   public static Runnable testSubmitter;

   private FxPasses() {
   }

   public static void beginFrame() {
      ITEMS.clear();
      CLOCKS.clear();
      cameraTaken = false;
   }

   public static void submit(PassItem item, FxPasses.Clock clock) {
      if (item != null && item.passes() != 0) {
         ITEMS.add(item);
         CLOCKS.add(clock);
      }
   }

   public static int size() {
      return ITEMS.size();
   }

   public static void solid() {
      if (testSubmitter != null) {
         testSubmitter.run();
      }

      run(FxPass.SOLID);
   }

   public static void translucentAndGlow() {
      try {
         run(FxPass.TRANSLUCENT);
         run(FxPass.GLOW);
      } finally {
         ITEMS.clear();
         CLOCKS.clear();
      }
   }

   public static Camera camera() {
      if (!cameraTaken) {
         CAMERA.captureWorld();
         cameraTaken = true;
      }

      return CAMERA;
   }

   private static void run(FxPass pass) {
      int n = gather(pass);
      if (n != 0) {
         Textures.ensureLoaded();
         RenderState.begin();

         try {
            Vfx.setCamera(camera());
            if (pass == FxPass.TRANSLUCENT) {
               sortFarToNear(n);
            }

            state(pass);

            for (int i = 0; i < n; i++) {
               PassItem item = order[i];
               long t0 = System.nanoTime();

               try {
                  item.drawPass(pass);
               } catch (RuntimeException | LinkageError var10) {
                  report(item, pass, var10);
                  item.passFailed();
               }

               between(pass);
               FxPasses.Clock c = orderClocks[i];
               if (c != null) {
                  c.nanos = c.nanos + (System.nanoTime() - t0);
               }

               order[i] = null;
               orderClocks[i] = null;
            }
         } finally {
            RenderState.end();
         }
      }
   }

   private static int gather(FxPass pass) {
      int n = 0;
      int bit = pass.bit();

      for (int i = 0; i < ITEMS.size(); i++) {
         PassItem item = ITEMS.get(i);
         if ((item.passes() & bit) != 0) {
            if (n == order.length) {
               order = Arrays.copyOf(order, n * 2);
               orderClocks = Arrays.copyOf(orderClocks, n * 2);
            }

            order[n] = item;
            orderClocks[n] = CLOCKS.get(i);
            n++;
         }
      }

      return n;
   }

   private static void sortFarToNear(int n) {
      for (int i = 1; i < n; i++) {
         PassItem item = order[i];
         FxPasses.Clock clock = orderClocks[i];
         double d = item.passDistanceSq();

         int k;
         for (k = i; k > 0 && order[k - 1].passDistanceSq() < d; k--) {
            order[k] = order[k - 1];
            orderClocks[k] = orderClocks[k - 1];
         }

         order[k] = item;
         orderClocks[k] = clock;
      }
   }

   public static void state(FxPass pass) {
      switch (pass) {
         case SOLID:
            RenderState.translucent(true);
            break;
         case TRANSLUCENT:
            RenderState.translucent(false);
            break;
         default:
            RenderState.decal(false);
            RenderState.additive();
      }
   }

   private static void between(FxPass pass) {
      Vfx.flush();
      if (pass == FxPass.GLOW) {
         RenderState.decal(false);
         RenderState.additive();
      } else {
         RenderState.decal(false);
         state(pass);
      }
   }

   public static void drawNow(List<? extends PassItem> items) {
      for (FxPass pass : FxPass.values()) {
         int n = 0;

         for (int i = 0; i < items.size(); i++) {
            PassItem item = items.get(i);
            if ((item.passes() & pass.bit()) != 0) {
               if (n == order.length) {
                  order = Arrays.copyOf(order, n * 2);
                  orderClocks = Arrays.copyOf(orderClocks, n * 2);
               }

               order[n] = item;
               orderClocks[n] = null;
               n++;
            }
         }

         if (n != 0) {
            if (pass == FxPass.TRANSLUCENT) {
               sortFarToNear(n);
            }

            state(pass);

            for (int var9 = 0; var9 < n; var9++) {
               try {
                  order[var9].drawPass(pass);
               } catch (RuntimeException | LinkageError var8) {
                  report(order[var9], pass, var8);
                  order[var9].passFailed();
               }

               between(pass);
               order[var9] = null;
            }
         }
      }

      RenderState.decal(false);
      RenderState.additive();
   }

   private static void report(PassItem item, FxPass pass, Throwable e) {
      if (REPORTED.add(item.getClass().getName() + ':' + pass + ':' + e.getClass().getName())) {
         FuriahsVisuals.LOG
            .error("Effect {} failed in the {} pass (further identical errors are not logged)", new Object[]{item.getClass().getSimpleName(), pass, e});
      }
   }

   public static final class Clock {
      public long nanos;

      public float takeMillis() {
         float ms = (float)this.nanos / 1000000.0F;
         this.nanos = 0L;
         return ms;
      }
   }
}
