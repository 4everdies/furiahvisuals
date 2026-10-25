package meowtils.furiahvisuals.hook;

import meowtils.furiahvisuals.core.FuriahsVisuals;

final class Sections {
   private static boolean failed;

   private Sections() {
   }

   static void start(String name, String parent, String grand) {
      try {
         if ("gui".equals(parent) && "gameRenderer".equals(grand)) {
            HudHooks.sectionStart(name);
         } else if ("level".equals(parent)) {
            if ("camera".equals(name)) {
               WorldHooks.fogColors();
               CameraHooks.hurtApply();
            } else if ("sky".equals(name)) {
               WorldHooks.skyStart();
            } else if ("clouds".equals(name)) {
               WorldHooks.cloudsStart();
            } else if ("weather".equals(name)) {
               WorldHooks.applyTemperature();
            } else if ("hand".equals(name)) {
               Bridge.hand();
            } else if ("center".equals(name)) {
               Bridge.lightmapDone();
            }
         } else if ("gameRenderer".equals(parent)) {
            if ("level".equals(name)) {
               Bridge.levelStart();
            } else if ("gui".equals(name) && "root".equals(grand)) {
               Bridge.beforeHud();
            }
         } else if ("mouse".equals(name) && "tick".equals(parent)) {
            InputHooks.mouse();
         } else if ("gameRenderer".equals(name)) {
            if ("root".equals(parent)) {
               Bridge.frameStart();
            } else if ("tick".equals(parent)) {
               WorldHooks.applyWeather();
               WorldHooks.applyTemperature();
            }
         }
      } catch (Throwable var4) {
         fail(var4);
      }
   }

   static void end(String name, String parent, String grand) {
      try {
         if ("gui".equals(parent) && "gameRenderer".equals(grand)) {
            HudHooks.sectionEnd(name);
         } else if ("level".equals(parent)) {
            if ("camera".equals(name) || "hand".equals(name)) {
               CameraHooks.hurtRestore();
            } else if ("sky".equals(name)) {
               WorldHooks.skyEnd();
            } else if ("clouds".equals(name)) {
               WorldHooks.cloudsEnd();
            } else if ("weather".equals(name)) {
               WorldHooks.restoreTemperature();
            }
         } else if ("gameRenderer".equals(parent)) {
            if ("level".equals(name)) {
               Bridge.levelEnd();
            } else if ("gui".equals(name) && "root".equals(grand)) {
               Bridge.afterHud();
            }
         } else if ("gameRenderer".equals(name)) {
            if ("root".equals(parent)) {
               Bridge.frameEnd();
            } else if ("tick".equals(parent)) {
               WorldHooks.restoreWeather();
               WorldHooks.restoreTemperature();
            }
         }
      } catch (Throwable var4) {
         fail(var4);
      }
   }

   private static void fail(Throwable t) {
      if (!failed) {
         failed = true;
         FuriahsVisuals.LOG.error("Hooks: a frame hook threw; it is logged once and the frame goes on", t);
      }
   }
}
