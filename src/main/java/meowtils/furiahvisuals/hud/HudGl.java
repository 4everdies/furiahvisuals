package meowtils.furiahvisuals.hud;

import java.lang.reflect.Field;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.OptiFineCompat;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public final class HudGl {
   private static final int FIRST_CHECKS = 30;
   private static final long CHECK_MS = 2000L;
   private static boolean looked;
   private static Field tracker;
   private static int checks;
   private static long nextCheck;
   private static boolean askDriver;
   private static Field shadeRecord;
   private static boolean shadeLooked;

   private HudGl() {
   }

   public static int program() {
      if (!askDriver && !OptiFineCompat.shadersActive()) {
         int known = known();
         long now = System.currentTimeMillis();
         if (checks < 30 || now >= nextCheck) {
            checks++;
            nextCheck = now + 2000L;
            int real = GL11.glGetInteger(35725);
            if (real != known) {
               askDriver = true;
               FuriahsVisuals.LOG.info("UI: the HUD's shader program is asked from the driver from now on (expected {}, found {})", new Object[]{known, real});
               return real;
            }
         }

         return known;
      } else {
         return GL11.glGetInteger(35725);
      }
   }

   public static void bindTexture(int id) {
      GL11.glBindTexture(3553, id);
      GlStateManager.bindTexture(id);
   }

   public static void set(int cap, boolean on) {
      if (on) {
         GL11.glEnable(cap);
      } else {
         GL11.glDisable(cap);
      }

      switch (cap) {
         case 2896:
            if (on) {
               GlStateManager.enableLighting();
            } else {
               GlStateManager.disableLighting();
            }
            break;
         case 2929:
            if (on) {
               GlStateManager.enableDepth();
            } else {
               GlStateManager.disableDepth();
            }
            break;
         case 3008:
            if (on) {
               GlStateManager.enableAlpha();
            } else {
               GlStateManager.disableAlpha();
            }
            break;
         case 3042:
            if (on) {
               GlStateManager.enableBlend();
            } else {
               GlStateManager.disableBlend();
            }
            break;
         case 3553:
            if (on) {
               GlStateManager.enableTexture2D();
            } else {
               GlStateManager.disableTexture2D();
            }
      }
   }

   public static int recordedShadeModel() {
      if (!shadeLooked) {
         shadeLooked = true;

         try {
            Field f = meowtils.furiahvisuals.hook.Hooks.field(GlStateManager.class, "activeShadeModel");
            if (f != null) {
               f.getInt(null);
               shadeRecord = f;
            }
         } catch (RuntimeException | ReflectiveOperationException var6) {
         }
      }

      if (shadeRecord != null) {
         try {
            return shadeRecord.getInt(null);
         } catch (RuntimeException | IllegalAccessException var5) {
            shadeRecord = null;
         }
      }

      return 7424;
   }

   public static void colorMask(boolean on) {
      GL11.glColorMask(on, on, on, on);
      GlStateManager.colorMask(on, on, on, on);
   }

   public static void depthMask(boolean on) {
      GL11.glDepthMask(on);
      GlStateManager.depthMask(on);
   }

   public static void blendFunc(int src, int dst, int srcAlpha, int dstAlpha) {
      GL14.glBlendFuncSeparate(src, dst, srcAlpha, dstAlpha);
      GlStateManager.tryBlendFuncSeparate(src, dst, srcAlpha, dstAlpha);
   }

   public static String describeState() {
      try {
         return "program "
            + GL11.glGetInteger(35725)
            + " (known "
            + known()
            + "), blend "
            + GL11.glIsEnabled(3042)
            + " "
            + GL11.glGetInteger(32969)
            + "/"
            + GL11.glGetInteger(32968)
            + ", depth test "
            + GL11.glIsEnabled(2929)
            + " mask "
            + GL11.glGetBoolean(2930)
            + " func "
            + GL11.glGetInteger(2932)
            + ", alpha test "
            + GL11.glIsEnabled(3008)
            + ", texture unit "
            + (GL11.glGetInteger(34016) - 33984)
            + " 2D "
            + GL11.glIsEnabled(3553)
            + " bound "
            + GL11.glGetInteger(32873);
      } catch (RuntimeException | LinkageError var1) {
         return "unknown (" + var1 + ")";
      }
   }

   private static int known() {
      if (!looked) {
         looked = true;

         try {
            Class<?> c = Class.forName("net.labymod.core.laby3d.opengl.GlStateTracker", false, HudGl.class.getClassLoader());
            Field f = c.getDeclaredField("currentProgram");
            f.setAccessible(true);
            f.getInt(null);
            tracker = f;
         } catch (ReflectiveOperationException | RuntimeException | LinkageError var2) {
            tracker = null;
         }
      }

      if (tracker != null) {
         try {
            return tracker.getInt(null);
         } catch (RuntimeException | IllegalAccessException var3) {
            tracker = null;
         }
      }

      return 0;
   }
}
