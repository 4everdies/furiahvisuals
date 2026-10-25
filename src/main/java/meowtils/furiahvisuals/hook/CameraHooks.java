package meowtils.furiahvisuals.hook;

import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.misc.MiscHooks;
import meowtils.furiahvisuals.module.weapons.WeaponHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MouseHelper;

public final class CameraHooks {
   private static boolean mouseInstalled;
   private static EntityLivingBase hurtEntity;
   private static int savedHurt;
   private static int savedMaxHurt;
   private static boolean handPaused;
   private static boolean failed;

   private CameraHooks() {
   }

   static void install() {
      Minecraft mc = Minecraft.getMinecraft();
      CameraHooks.FvMouseHelper f = Swap.upgrade(mc.mouseHelper, MouseHelper.class, CameraHooks.FvMouseHelper.class);
      if (f != null) {
         mc.mouseHelper = f;
         mouseInstalled = true;
         Hooks.on("turn");
      }

      Hooks.on("hurtShake");
   }

   static void uninstall() {
      Minecraft mc = Minecraft.getMinecraft();
      hurtRestore();
      if (Swap.ours(mc.mouseHelper, CameraHooks.FvMouseHelper.class)) {
         mc.mouseHelper = Swap.downgrade(mc.mouseHelper, MouseHelper.class);
      }

      mouseInstalled = false;
      Hooks.off("turn", "hurtShake");
   }

   static void hurtApply() {
      if (hurtEntity == null && Bridge.active() && Hooks.has("hurtShake")) {
         try {
            Entity v = Minecraft.getMinecraft().getRenderViewEntity();
            if (!(v instanceof EntityLivingBase)) {
               return;
            }

            EntityLivingBase e = (EntityLivingBase)v;
            float pt = Bridge.partialTicks();
            float f = e.hurtTime - pt;
            if (f < 0.0F || e.maxHurtTime <= 0) {
               return;
            }

            float k = MiscHooks.hurtShake(14.0F) / 14.0F;
            if (k >= 0.9999F) {
               return;
            }

            float x = f / e.maxHurtTime;
            float want = Math.max(0.0F, k * MathHelper.sin(x * x * x * x * (float) Math.PI));
            hurtEntity = e;
            savedHurt = e.hurtTime;
            savedMaxHurt = e.maxHurtTime;
            if (want <= 0.0F) {
               e.hurtTime = 0;
               return;
            }

            double x4 = Math.asin(Math.min(1.0, (double)want)) / Math.PI;
            double nx = Math.pow(x4, 0.25);
            e.maxHurtTime = 100000;
            e.hurtTime = (int)Math.round(nx * 100000.0 + pt);
         } catch (Throwable var11) {
            fail(var11);
         }
      }
   }

   static void hurtRestore() {
      EntityLivingBase e = hurtEntity;
      if (e != null) {
         hurtEntity = null;
         e.hurtTime = savedHurt;
         e.maxHurtTime = savedMaxHurt;
      }
   }

   static void handItemStart() {
      if (hurtEntity != null) {
         handPaused = true;
         hurtRestore();
      }
   }

   static void handItemEnd() {
      if (handPaused) {
         handPaused = false;
         hurtApply();
      }
   }

   private static void fail(Throwable t) {
      if (!failed) {
         failed = true;
         FuriahsVisuals.LOG.error("Hooks: a camera hook threw; it is logged once", t);
      }
   }

   public static class FvMouseHelper extends MouseHelper {
      public void mouseXYChange() {
         super.mouseXYChange();
         Minecraft mc = Minecraft.getMinecraft();
         if (Bridge.active() && mc.thePlayer != null && (this.deltaX != 0 || this.deltaY != 0)) {
            try {
               float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
               float f1 = f * f * f * 8.0F;
               float yaw = this.deltaX * f1;
               float pitch = this.deltaY * f1 * (mc.gameSettings.invertMouse ? -1 : 1);
               if (WeaponHooks.turn(mc.thePlayer, yaw, pitch)) {
                  this.deltaX = 0;
                  this.deltaY = 0;
               }
            } catch (Throwable var6) {
               CameraHooks.fail(var6);
            }
         }
      }
   }
}
