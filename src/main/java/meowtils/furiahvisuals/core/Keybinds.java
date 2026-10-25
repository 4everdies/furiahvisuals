package meowtils.furiahvisuals.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import meowtils.furiahvisuals.compat.ClientRegistry;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.misc.MiscModule;
import meowtils.furiahvisuals.module.ui.UiModule;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.module.weapons.WeaponsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public final class Keybinds {
   public static final String CATEGORY = "key.categories.furiahsvisuals";
   public static KeyBinding openMenu;
   public static KeyBinding muteSounds;
   public static KeyBinding inspectWeapon;
   public static KeyBinding nextCrosshairProfile;
   public static KeyBinding weaponWheel;
   public static KeyBinding toggleSaturation;
   public static KeyBinding skinWheel;
   public static KeyBinding nextSkin;
   public static KeyBinding previousSkin;
   public static KeyBinding toggleWeaponStyle;
   public static KeyBinding toggleFullbright;
   private static final Map<Module, KeyBinding> TOGGLES = new LinkedHashMap<>();

   private Keybinds() {
   }

   static void register(ModuleManager modules) {
      openMenu = new KeyBinding("key.furiahsvisuals.menu", 54, "key.categories.furiahsvisuals");
      ClientRegistry.registerKeyBinding(openMenu);
      muteSounds = new KeyBinding("key.furiahsvisuals.mute", 0, "key.categories.furiahsvisuals");
      ClientRegistry.registerKeyBinding(muteSounds);
      inspectWeapon = new KeyBinding("key.furiahsvisuals.inspect", 33, "key.categories.furiahsvisuals");
      ClientRegistry.registerKeyBinding(inspectWeapon);
      nextCrosshairProfile = new KeyBinding("key.furiahsvisuals.crosshairProfile", 0, "key.categories.furiahsvisuals");
      ClientRegistry.registerKeyBinding(nextCrosshairProfile);
      weaponWheel = new KeyBinding("key.furiahsvisuals.wheel", 48, "key.categories.furiahsvisuals");
      ClientRegistry.registerKeyBinding(weaponWheel);
      toggleSaturation = new KeyBinding("key.furiahsvisuals.saturation", 0, "key.categories.furiahsvisuals");
      ClientRegistry.registerKeyBinding(toggleSaturation);
      skinWheel = new KeyBinding("key.furiahsvisuals.skinWheel", 0, "key.categories.furiahsvisuals");
      ClientRegistry.registerKeyBinding(skinWheel);
      nextSkin = new KeyBinding("key.furiahsvisuals.nextSkin", 0, "key.categories.furiahsvisuals");
      ClientRegistry.registerKeyBinding(nextSkin);
      previousSkin = new KeyBinding("key.furiahsvisuals.previousSkin", 0, "key.categories.furiahsvisuals");
      ClientRegistry.registerKeyBinding(previousSkin);
      toggleWeaponStyle = new KeyBinding("key.furiahsvisuals.weaponStyle", 0, "key.categories.furiahsvisuals");
      ClientRegistry.registerKeyBinding(toggleWeaponStyle);
      toggleFullbright = new KeyBinding("key.furiahsvisuals.fullbright", 0, "key.categories.furiahsvisuals");
      ClientRegistry.registerKeyBinding(toggleFullbright);
      Map<String, Integer> saved = null;

      for (Module m : modules.getModules()) {
         KeyBinding toggle = new KeyBinding("Toggle " + m.getName(), 0, "key.categories.furiahsvisuals");
         ClientRegistry.registerKeyBinding(toggle);
         TOGGLES.put(m, toggle);
         if (m.getLegacyNames().length > 0) {
            if (saved == null) {
               saved = savedKeys();
            }

            carryOver(m, toggle, saved);
         }
      }

      KeyBinding.resetKeyBindingArrayAndHash();
   }

   private static void carryOver(Module m, KeyBinding toggle, Map<String, Integer> saved) {
      if (!saved.containsKey("key_" + toggle.getKeyDescription())) {
         for (String old : m.getLegacyNames()) {
            Integer code = saved.get("key_Toggle " + old);
            if (code != null && code != 0) {
               toggle.setKeyCode(code);
               FuriahsVisuals.LOG.info("Key binding 'Toggle {}' carried over to 'Toggle {}'", new Object[]{old, m.getName()});
               return;
            }
         }
      }
   }

   static int carryOverForTest(Module m, Map<String, Integer> saved) {
      KeyBinding toggle = TOGGLES.get(m);
      int before = toggle.getKeyCode();

      int var5;
      try {
         toggle.setKeyCode(0);
         carryOver(m, toggle, saved);
         int n = toggle.getKeyCode();
         var5 = n;
      } finally {
         toggle.setKeyCode(before);
      }

      return var5;
   }

   public static KeyBinding toggleOf(Module m) {
      return TOGGLES.get(m);
   }

   static String toggleNameForTest(Module m) {
      return TOGGLES.get(m).getKeyDescription();
   }

   private static Map<String, Integer> savedKeys() {
      HashMap<String, Integer> out = new HashMap<>();
      File file = new File(Minecraft.getMinecraft().mcDataDir, "options.txt");
      if (!file.isFile()) {
         return out;
      } else {
         try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
               int colon = line.lastIndexOf(58);
               if (line.startsWith("key_") && colon > 4) {
                  try {
                     out.put(line.substring(0, colon), Integer.parseInt(line.substring(colon + 1).trim()));
                  } catch (NumberFormatException var6) {
                  }
               }
            }
         } catch (RuntimeException | IOException var7) {
            FuriahsVisuals.LOG.debug("Could not read options.txt for the renamed key bindings", var7);
         }

         return out;
      }
   }

   private static void cycleSkin(int dir) {
      Module misc = FuriahsVisuals.instance.getModules().get("misc");
      if (misc instanceof MiscModule && misc.isEnabled()) {
         ((MiscModule)misc).skins.cycle(dir);
      }
   }

   static void handle(Minecraft mc) {
      if (openMenu != null) {
         while (openMenu.isPressed()) {
            if (mc.currentScreen == null) {
               FuriahsVisuals.instance.openMenu();
            }
         }

         while (muteSounds.isPressed()) {
            FuriahsVisuals.instance.toggleSoundMute();
         }

         while (nextCrosshairProfile.isPressed()) {
            Module ui = FuriahsVisuals.instance.getModules().get("ui");
            if (ui instanceof UiModule && ui.isEnabled()) {
               ((UiModule)ui).nextCrosshairProfile();
            }
         }

         while (toggleSaturation.isPressed()) {
            Module misc = FuriahsVisuals.instance.getModules().get("misc");
            if (misc instanceof MiscModule) {
               boolean on = ((MiscModule)misc).toggleSaturation();
               if (mc.ingameGUI != null) {
                  mc.ingameGUI.setRecordPlaying("§dFuriah's Visuals §8» §fSaturation " + (on ? "§aON" : "§cOFF"), false);
               }

               WaterMarkModule wm;
               if ((wm = FuriahsVisuals.instance.getWaterMark()) != null) {
                  wm.partSwitched("saturation", "Saturation", on);
               }
            }
         }

         while (nextSkin.isPressed()) {
            cycleSkin(1);
         }

         while (previousSkin.isPressed()) {
            cycleSkin(-1);
         }

         while (toggleWeaponStyle.isPressed()) {
            Module weapons = FuriahsVisuals.instance.getModules().get("weapons");
            if (weapons instanceof WeaponsModule) {
               ((WeaponsModule)weapons).toggleStyle();
            }
         }

         while (toggleFullbright.isPressed()) {
            Module misc = FuriahsVisuals.instance.getModules().get("misc");
            if (misc instanceof MiscModule) {
               ((MiscModule)misc).fullbrightKey();
            }
         }

         for (Entry<Module, KeyBinding> e : TOGGLES.entrySet()) {
            while (e.getValue().isPressed()) {
               Module m = e.getKey();
               m.setEnabled(!m.isEnabled());
               if (mc.ingameGUI != null) {
                  mc.ingameGUI.setRecordPlaying("§dFuriah's Visuals §8» §f" + m.getName() + " " + (m.isEnabled() ? "§aON" : "§cOFF"), false);
               }
            }
         }
      }
   }
}
