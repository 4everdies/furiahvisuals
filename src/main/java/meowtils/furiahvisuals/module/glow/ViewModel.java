package meowtils.furiahvisuals.module.glow;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import meowtils.furiahvisuals.compat.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.lwjgl.BufferUtils;

final class ViewModel {
   private static Field itemToRender;
   private static Field equipped;
   private static Field prevEquipped;
   private static boolean looked;
   private static boolean available;
   private static final double[] POINT = new double[3];
   private static Method fovModifier;
   private static Method hurtCamera;
   private static Method viewBobbing;
   private static Field farPlane;
   private static Field renderHandFlag;
   private static Field debugView;
   private static boolean cameraLooked;
   private static boolean cameraAvailable;
   private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);

   private ViewModel() {
   }

   private static void lookUpCamera(Minecraft mc) {
      if (!cameraLooked) {
         cameraLooked = true;

         try {
            EntityRenderer er = mc.entityRenderer;
            fovModifier = ReflectionHelper.findMethod(
               EntityRenderer.class, er, new String[]{"getFOVModifier", "getFOVModifier"}, new Class[]{float.class, boolean.class}
            );
            hurtCamera = ReflectionHelper.findMethod(EntityRenderer.class, er, new String[]{"hurtCameraEffect", "hurtCameraEffect"}, new Class[]{float.class});
            viewBobbing = ReflectionHelper.findMethod(EntityRenderer.class, er, new String[]{"setupViewBobbing", "setupViewBobbing"}, new Class[]{float.class});
            farPlane = ReflectionHelper.findField(EntityRenderer.class, new String[]{"farPlaneDistance", "farPlaneDistance"});
            renderHandFlag = ReflectionHelper.findField(EntityRenderer.class, new String[]{"renderHand", "renderHand"});
            debugView = ReflectionHelper.findField(EntityRenderer.class, new String[]{"debugView", "debugView"});
            cameraAvailable = true;
         } catch (RuntimeException var2) {
            cameraAvailable = false;
         }
      }
   }

   static boolean handEnabled(Minecraft mc) {
      lookUpCamera(mc);
      if (!cameraAvailable) {
         return false;
      } else {
         try {
            return renderHandFlag.getBoolean(mc.entityRenderer) && !debugView.getBoolean(mc.entityRenderer);
         } catch (RuntimeException | IllegalAccessException var2) {
            return false;
         }
      }
   }

   static boolean pushHandCamera(Minecraft mc, float partialTicks) {
      lookUpCamera(mc);
      if (!cameraAvailable) {
         return false;
      } else {
         EntityRenderer er = mc.entityRenderer;

         float far;
         float fov;
         try {
            fov = (Float)fovModifier.invoke(er, partialTicks, false);
            far = farPlane.getFloat(er) * 2.0F;
         } catch (RuntimeException | ReflectiveOperationException var7) {
            cameraAvailable = false;
            return false;
         }

         GlStateManager.matrixMode(5889);
         GlStateManager.pushMatrix();
         GlStateManager.loadIdentity();
         perspective(fov, (float)mc.displayWidth / Math.max(1, mc.displayHeight), 0.05F, far);
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();
         GlStateManager.loadIdentity();

         try {
            hurtCamera.invoke(er, partialTicks);
            if (mc.gameSettings.viewBobbing) {
               viewBobbing.invoke(er, partialTicks);
            }
         } catch (RuntimeException | ReflectiveOperationException var6) {
            cameraAvailable = false;
         }

         return true;
      }
   }

   static void popHandCamera() {
      GlStateManager.matrixMode(5888);
      GlStateManager.popMatrix();
      GlStateManager.matrixMode(5889);
      GlStateManager.popMatrix();
      GlStateManager.matrixMode(5888);
   }

   private static void perspective(float fovY, float aspect, float near, float far) {
      float f = (float)(1.0 / Math.tan(Math.toRadians(fovY) / 2.0));
      ((Buffer)MATRIX).clear();
      MATRIX.put(f / aspect).put(0.0F).put(0.0F).put(0.0F);
      MATRIX.put(0.0F).put(f).put(0.0F).put(0.0F);
      MATRIX.put(0.0F).put(0.0F).put((far + near) / (near - far)).put(-1.0F);
      MATRIX.put(0.0F).put(0.0F).put(2.0F * far * near / (near - far)).put(0.0F);
      ((Buffer)MATRIX).flip();
      GlStateManager.multMatrix(MATRIX);
   }

   private static void lookUp() {
      if (!looked) {
         looked = true;

         try {
            itemToRender = ReflectionHelper.findField(ItemRenderer.class, new String[]{"itemToRender", "itemToRender"});
            equipped = ReflectionHelper.findField(ItemRenderer.class, new String[]{"equippedProgress", "equippedProgress"});
            prevEquipped = ReflectionHelper.findField(ItemRenderer.class, new String[]{"prevEquippedProgress", "prevEquippedProgress"});
            available = true;
         } catch (RuntimeException var1) {
            available = false;
         }
      }
   }

   static ItemStack stack(Minecraft mc) {
      lookUp();
      if (available) {
         try {
            return (ItemStack)itemToRender.get(mc.getItemRenderer());
         } catch (RuntimeException | IllegalAccessException var2) {
         }
      }

      return mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
   }

   static float lowered(Minecraft mc, float partialTicks) {
      lookUp();
      if (!available) {
         return 0.0F;
      } else {
         try {
            ItemRenderer r = mc.getItemRenderer();
            float prev = prevEquipped.getFloat(r);
            float now = equipped.getFloat(r);
            return 1.0F - (prev + (now - prev) * partialTicks);
         } catch (RuntimeException | IllegalAccessException var5) {
            return 0.0F;
         }
      }
   }

   static boolean switching(Minecraft mc) {
      lookUp();
      if (!available) {
         return false;
      } else {
         try {
            ItemRenderer r = mc.getItemRenderer();
            return equipped.getFloat(r) < 1.0F || prevEquipped.getFloat(r) < 1.0F;
         } catch (RuntimeException | IllegalAccessException var2) {
            return false;
         }
      }
   }

   static double[] center(Minecraft mc, float pt) {
      EntityPlayerSP p = mc.thePlayer;
      double[] out = POINT;
      if (p == null) {
         out[0] = 0.5;
         out[1] = -0.5;
         out[2] = -0.75;
         return out;
      } else {
         ItemStack held = stack(mc);
         float down = lowered(mc, pt);
         float swing = p.getSwingProgress(pt);
         float root = MathHelper.sqrt_float(swing);
         float pi = (float) Math.PI;
         double z;
         double y;
         double x;
         if (held == null) {
            x = 0.58 - 0.3 * MathHelper.sin(root * pi);
            y = -0.6 + 0.4 * MathHelper.sin(root * pi * 2.0F) - 0.6 * down;
            z = -0.72 - 0.4 * MathHelper.sin(swing * pi);
         } else if (held.getItem() instanceof ItemMap) {
            x = 0.0;
            y = -0.34 - 0.5 * down;
            z = -0.7;
         } else if (p.getItemInUseCount() > 0) {
            switch (held.getItemUseAction()) {
               case EAT:
               case DRINK:
                  x = 0.3;
                  y = -0.4;
                  break;
               case BOW:
                  x = 0.32;
                  y = -0.34;
                  break;
               case BLOCK:
                  x = 0.4;
                  y = -0.44;
                  break;
               default:
                  x = 0.46;
                  y = -0.44;
            }

            z = -0.74;
         } else {
            x = 0.6 - 0.4 * MathHelper.sin(root * pi);
            y = -0.22 + 0.2 * MathHelper.sin(root * pi * 2.0F) - 0.6 * down;
            z = -0.84 - 0.2 * MathHelper.sin(swing * pi);
         }

         float armPitch = p.prevRenderArmPitch + (p.renderArmPitch - p.prevRenderArmPitch) * pt;
         float armYaw = p.prevRenderArmYaw + (p.renderArmYaw - p.prevRenderArmYaw) * pt;
         float pitch = p.prevRotationPitch + (p.rotationPitch - p.prevRotationPitch) * pt;
         float yaw = p.prevRotationYaw + (p.rotationYaw - p.prevRotationYaw) * pt;
         double ax = Math.toRadians((pitch - armPitch) * 0.1F);
         double ay = Math.toRadians((yaw - armYaw) * 0.1F);
         double cy = Math.cos(ay);
         double sy = Math.sin(ay);
         double x1 = x * cy + z * sy;
         double z1 = -x * sy + z * cy;
         double cx = Math.cos(ax);
         double sx = Math.sin(ax);
         double y2 = y * cx - z1 * sx;
         double z2 = y * sx + z1 * cx;
         out[0] = x1;
         out[1] = y2;
         out[2] = z2;
         return out;
      }
   }
}
