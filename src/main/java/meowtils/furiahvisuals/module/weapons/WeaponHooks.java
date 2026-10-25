package meowtils.furiahvisuals.module.weapons;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.hook.Hooks;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.ui.UiHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class WeaponHooks {
   static final String FIRST_PERSON = "firstPerson";
   static final String FIRST_PERSON_END = "firstPersonEnd";
   private static WeaponsModule module;
   private static boolean broken;
   private static String brokenReason;
   private static Boolean hooksIn;
   private static Entity worldEntity;
   private static float worldPartialTicks;
   private static boolean wheelBroken;
   private static Set<String> installed;
   private static boolean pluginLoaded;
   private static boolean looked;

   private WeaponHooks() {
   }

   private static WeaponsModule module() {
      Module m;
      if (module == null && (m = FuriahsVisuals.instance.getModules().get("weapons")) instanceof WeaponsModule) {
         module = (WeaponsModule)m;
      }

      return module;
   }

   private static boolean active() {
      WeaponsModule m = module();
      return m != null && m.isEnabled() && !broken && hooksIn();
   }

   static boolean hooksIn() {
      if (hooksIn == null) {
         hooksIn = missingHooks().isEmpty();
      }

      return hooksIn;
   }

   static List<String> missingHooks() {
      ArrayList<String> missing = new ArrayList<>();

      for (String h : new String[]{"firstPerson", "firstPersonEnd", "armPose", "heldItem", "heldItemEnd", "icon"}) {
         if (!installed(h)) {
            missing.add(h);
         }
      }

      return missing;
   }

   static boolean broken() {
      return broken;
   }

   static String brokenReason() {
      return brokenReason;
   }

   private static void fail(String where, Throwable e) {
      if (!broken) {
         broken = true;
         brokenReason = "Weapons stopped after an error (" + e + ") and items are drawn as vanilla until you restart.";
         FuriahsVisuals.LOG.error("Weapons: {} failed; the section is off for this session", new Object[]{where, e});
      }
   }

   public static boolean firstPerson(ItemStack itemToRender, float partialTicks) {
      if (!active()) {
         return false;
      } else {
         try {
            return module.firstPerson(itemToRender, partialTicks);
         } catch (RuntimeException | LinkageError var3) {
            fail("the first-person view model", var3);
            return false;
         }
      }
   }

   public static void firstPersonEnd(float partialTicks) {
      if (module != null && !broken) {
         try {
            module.firstPersonEnd(partialTicks);
         } catch (RuntimeException | LinkageError var2) {
            fail("the first-person view model", var2);
         }
      }
   }

   public static void maskPass(boolean on) {
      WeaponsModule.maskPass = on;
   }

   public static boolean icon(ItemStack stack, int x, int y, float zLevel) {
      if (stack != null && active()) {
         try {
            return module.icon(stack, x, y, zLevel);
         } catch (RuntimeException | LinkageError var5) {
            fail("the weapon icons", var5);
            return false;
         }
      } else {
         return false;
      }
   }

   public static void armPose(ModelPlayer model, Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks) {
      if (entity instanceof AbstractClientPlayer) {
         ThirdPerson.resetShoulders(model, "slim".equals(((AbstractClientPlayer)entity).getSkinType()));
      }

      if (active()) {
         try {
            module.thirdPerson().pose(model, entity, limbSwing, limbSwingAmount, ageInTicks);
         } catch (RuntimeException | LinkageError var6) {
            fail("the third-person pose", var6);
         }
      }
   }

   public static void worldItemEntity(Entity entity, float partialTicks) {
      worldEntity = entity;
      worldPartialTicks = partialTicks;
   }

   public static boolean worldItem(ItemStack stack, boolean frame) {
      if (stack != null && worldEntity != null && active()) {
         try {
            return module.thirdPerson().drawWorldItem(worldEntity, stack, frame, worldPartialTicks);
         } catch (RuntimeException | LinkageError var3) {
            fail("dropped items and item frames", var3);
            return false;
         }
      } else {
         return false;
      }
   }

   public static void armorPose(ModelBiped model, Entity entity) {
      if (entity instanceof EntityPlayer && active()) {
         try {
            module.thirdPerson().copyPose(model, entity);
         } catch (RuntimeException | LinkageError var3) {
            fail("the third-person pose", var3);
         }
      }
   }

   public static boolean heldItem(ModelBase model, EntityLivingBase entity, float partialTicks) {
      if (active() && model instanceof ModelBiped) {
         try {
            return module.thirdPerson().drawHeld((ModelBiped)model, entity, partialTicks);
         } catch (RuntimeException | LinkageError var4) {
            fail("the third-person weapon", var4);
            return false;
         }
      } else {
         return false;
      }
   }

   public static void heldItemEnd(ModelBase model, EntityLivingBase entity) {
      if (active() && model instanceof ModelBiped) {
         try {
            module.thirdPerson().drawVanillaCopy((ModelBiped)model, entity);
         } catch (RuntimeException | LinkageError var3) {
            fail("the third-person left hand", var3);
         }
      }
   }

   private static void wheelFail(String where, Throwable e) {
      if (!wheelBroken) {
         wheelBroken = true;
         FuriahsVisuals.LOG.error("Weapons: the wheels failed ({}) and are off for this session", new Object[]{where, e});
      }

      try {
         for (WeaponWheel w : WeaponWheel.ALL) {
            w.cancel(false);
         }
      } catch (RuntimeException var4) {
      }
   }

   public static void wheelFrame() {
      module();
      if (!wheelBroken) {
         try {
            for (WeaponWheel w : WeaponWheel.ALL) {
               w.frame();
               if (w.showing()) {
                  w.renderer.prepare();
               }
            }
         } catch (RuntimeException | LinkageError var2) {
            wheelFail("its frame", var2);
         }
      }
   }

   public static boolean turn(Object entity, float yaw, float pitch) {
      if (!wheelBroken && entity == Minecraft.getMinecraft().thePlayer) {
         try {
            for (WeaponWheel w : WeaponWheel.ALL) {
               if (w.turn(yaw, pitch)) {
                  return true;
               }
            }

            return false;
         } catch (RuntimeException | LinkageError var5) {
            wheelFail("the mouse", var5);
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean wheelMouse(int button, boolean down, int wheelTurn) {
      if (wheelBroken) {
         return false;
      } else {
         try {
            for (WeaponWheel w : WeaponWheel.ALL) {
               if (w.mouse(button, down, wheelTurn)) {
                  return true;
               }
            }

            return false;
         } catch (RuntimeException | LinkageError var5) {
            wheelFail("the mouse", var5);
            return false;
         }
      }
   }

   public static void wheelKeyInput() {
      if (!wheelBroken) {
         try {
            for (WeaponWheel w : WeaponWheel.ALL) {
               w.keyInput();
            }
         } catch (RuntimeException | LinkageError var2) {
            wheelFail("the keys", var2);
         }
      }
   }

   public static boolean wheelScreen(GuiScreen screen) {
      if (wheelBroken) {
         return false;
      } else {
         try {
            boolean keep = false;

            for (WeaponWheel w : WeaponWheel.ALL) {
               keep |= w.screenOpening(screen);
            }

            return keep;
         } catch (RuntimeException | LinkageError var4) {
            wheelFail("a screen opening", var4);
            return false;
         }
      }
   }

   static boolean turnHooked() {
      return UiHooks.installed("turn", Entity.class);
   }

   public static String hookCheckForTest() {
      installed("firstPerson");
      boolean reported = pluginLoaded && installed != null;
      StringBuilder sb = new StringBuilder(reported ? "plugin reported" : "PLUGIN DID NOT REPORT");

      for (String h : new String[]{"firstPerson", "firstPersonEnd", "armPose", "heldItem", "heldItemEnd", "icon"}) {
         sb.append("; ")
            .append(h)
            .append(" plugin")
            .append(reported ? (installed.contains(h) ? "+" : "-") : "?")
            .append(" class")
            .append(merged(h) ? "+" : "-");
      }

      return sb.toString();
   }

   static boolean installed(String hook) {
      if (!looked) {
         looked = true;
         pluginLoaded = true;
         installed = Hooks.INSTALLED;
         if (!pluginLoaded) {
            FuriahsVisuals.LOG.info("Weapons: the mixin plugin did not report; the hooks are looked up in their classes instead");
         }
      }

      return pluginLoaded && installed != null ? installed.contains(hook) : merged(hook);
   }

   private static boolean merged(String hook) {
      Class<?> target;
      if ("firstPerson".equals(hook) || "firstPersonEnd".equals(hook)) {
         target = ItemRenderer.class;
      } else if ("armPose".equals(hook)) {
         target = ModelPlayer.class;
      } else if (!"heldItem".equals(hook) && !"heldItemEnd".equals(hook)) {
         if (!"icon".equals(hook)) {
            return false;
         }

         target = RenderItem.class;
      } else {
         target = LayerHeldItem.class;
      }

      try {
         for (Method m : target.getDeclaredMethods()) {
            String name = m.getName();
            int at = name.indexOf("fv$" + hook);
            int end = at + 3 + hook.length();
            if (at >= 0 && (name.length() == end || name.charAt(end) == '$')) {
               return true;
            }
         }
      } catch (Throwable var9) {
         FuriahsVisuals.LOG.warn("Weapons: could not look for the {} hook in {}", new Object[]{hook, target.getName(), var9});
      }

      return false;
   }
}
