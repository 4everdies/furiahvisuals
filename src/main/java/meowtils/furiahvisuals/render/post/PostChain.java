package meowtils.furiahvisuals.render.post;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.ui.UiHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.lwjgl.opengl.GL11;

public final class PostChain {
   private static final List<PostChain.Pass> PASSES = new ArrayList<>();
   private static final List<PostChain.Pass> BROKEN = new ArrayList<>();
   private static int brokenFiles = -1;
   private static final PostFrame FRAME = new PostFrame();
   private static final int ERROR_CHECKS = 3;
   private static long lastRunMs;
   private static int errorChecks;
   private static float partialTicks;
   private static Boolean handHook;
   private static Boolean hudHook;
   private static Boolean afterHudHook;

   private PostChain() {
   }

   public static void register(PostChain.Pass pass) {
      if (!PASSES.contains(pass)) {
         PASSES.add(pass);
      }
   }

   public static boolean handHooked() {
      if (handHook == null) {
         handHook = UiHooks.installed("beforeHand", EntityRenderer.class);
      }

      return handHook;
   }

   public static boolean hudHooked() {
      if (hudHook == null) {
         hudHook = UiHooks.installed("beforeHud", EntityRenderer.class);
      }

      return hudHook;
   }

   public static boolean afterHudHooked() {
      if (afterHudHook == null) {
         afterHudHook = UiHooks.installed("afterHud", EntityRenderer.class);
      }

      return afterHudHook;
   }

   public static void beforeHandHook(int pass) {
      if (pass == 2) {
         run(PostChain.Point.BEFORE_HAND);
      }
   }

   public static void beforeHandEvent() {
      if (!handHooked() && !Minecraft.getMinecraft().gameSettings.anaglyph) {
         run(PostChain.Point.BEFORE_HAND);
      }
   }

   public static void beforeHudHook(float partialTicks) {
      afterHand(partialTicks);
      run(PostChain.Point.BEFORE_HUD);
   }

   public static void beforeHudEvent(float partialTicks) {
      if (!hudHooked()) {
         afterHand(partialTicks);
         run(PostChain.Point.BEFORE_HUD);
      }
   }

   public static void afterHudHook() {
      run(PostChain.Point.AFTER_HUD);
   }

   public static void afterHudEvent() {
      if (!afterHudHooked()) {
         run(PostChain.Point.AFTER_HUD);
      }
   }

   private static void afterHand(float pt) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.theWorld != null && FuriahsVisuals.instance != null) {
         partialTicks = pt;
         FuriahsVisuals.instance.getModules().afterHand(pt);
      }
   }

   public static float partialTicks() {
      return partialTicks;
   }

   private static void run(PostChain.Point point) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.theWorld != null && !PASSES.isEmpty()) {
         if (AssetSource.ready()) {
            if (brokenFiles != AssetSource.generation()) {
               brokenFiles = AssetSource.generation();
               BROKEN.clear();
            }

            boolean any = false;

            for (int i = 0; i < PASSES.size(); i++) {
               PostChain.Pass p = PASSES.get(i);
               if (!BROKEN.contains(p) && p.wants(point)) {
                  any = true;
                  break;
               }
            }

            if (any) {
               lastRunMs = System.currentTimeMillis();
               mc.mcProfiler.startSection("furiahsvisuals_post");
               boolean begun = false;
               boolean checkErrors = errorChecks < 3;

               try {
                  if (checkErrors) {
                     int ix = 0;

                     while (ix < 16 && GL11.glGetError() != 0) {
                        ix++;
                     }
                  }

                  if (!(begun = FRAME.begin(mc, point))) {
                     return;
                  }

                  for (int ix = 0; ix < PASSES.size(); ix++) {
                     PostChain.Pass p = PASSES.get(ix);
                     if (!BROKEN.contains(p) && p.wants(point)) {
                        mc.mcProfiler.startSection(p.name());

                        try {
                           p.run(point, FRAME);
                           int error;
                           if (checkErrors && (error = GL11.glGetError()) != 0) {
                              fail(p, point, "OpenGL error " + error, null);
                           }
                        } catch (RuntimeException | LinkageError var16) {
                           fail(p, point, var16.getClass().getSimpleName() + (var16.getMessage() == null ? "" : ": " + var16.getMessage()), var16);
                        } finally {
                           FRAME.passDone();
                           mc.mcProfiler.endSection();
                        }
                     }
                  }
               } finally {
                  if (begun) {
                     FRAME.end();
                     if (checkErrors) {
                        errorChecks++;
                     }
                  }

                  mc.mcProfiler.endSection();
               }
            }
         }
      }
   }

   private static void fail(PostChain.Pass p, PostChain.Point point, String why, Throwable e) {
      if (!BROKEN.contains(p)) {
         BROKEN.add(p);
         if (e != null) {
            FuriahsVisuals.LOG.error("{} failed at {} ({}) and is off until the game restarts", new Object[]{p.name(), point, why, e});
         } else {
            FuriahsVisuals.LOG.error("{} failed at {} ({}) and is off until the game restarts", new Object[]{p.name(), point, why});
         }

         try {
            p.failed(why);
         } catch (RuntimeException var6) {
         }

         try {
            p.release();
         } catch (RuntimeException | LinkageError var5) {
         }
      }
   }

   public static boolean broken(PostChain.Pass pass) {
      return BROKEN.contains(pass);
   }

   public static void repairForTest(PostChain.Pass pass) {
      BROKEN.remove(pass);
   }

   public static void placeForTest(PostChain.Pass pass, int index) {
      PASSES.remove(pass);
      if (index >= 0) {
         PASSES.add(Math.min(index, PASSES.size()), pass);
      }
   }

   public static void tick() {
      if (FRAME.allocated() && System.currentTimeMillis() - lastRunMs > 3000L) {
         FRAME.release();
      }
   }

   public static void releaseShared() {
      FRAME.release();
   }

   public static PostFrame frame() {
      return FRAME;
   }

   public static long lastRunMs() {
      return lastRunMs;
   }

   public static String hooksSummary() {
      return (handHooked() ? "hand hook" : "hand event")
         + ", "
         + (hudHooked() ? "HUD hook" : "HUD event")
         + ", "
         + (afterHudHooked() ? "after-HUD hook" : "after-HUD event");
   }

   public interface Pass {
      String name();

      boolean wants(PostChain.Point var1);

      void run(PostChain.Point var1, PostFrame var2);

      void failed(String var1);

      void release();
   }

   public static enum Point {
      BEFORE_HAND,
      BEFORE_HUD,
      AFTER_HUD;
   }
}
