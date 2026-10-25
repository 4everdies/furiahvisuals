package meowtils.furiahvisuals.module.ui;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.hook.Hooks;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.IChatComponent;

public final class UiHooks {
   private static Set<String> installed;
   private static boolean pluginLoaded;
   private static boolean looked;

   private UiHooks() {
   }

   private static UiModule ui() {
      return FuriahsVisuals.instance == null ? null : FuriahsVisuals.instance.getUi();
   }

   public static boolean scoreboard(ScoreObjective objective, ScaledResolution resolution) {
      UiModule u = ui();
      return u == null ? false : u.scoreboardHook(objective, resolution);
   }

   public static boolean itemName(ScaledResolution res) {
      UiModule u = ui();
      return u != null && u.itemNameHook(res);
   }

   public static int actionBarHeight(int height) {
      return height - Math.round(itemHudLift());
   }

   public static float itemHudLift() {
      UiModule u = ui();
      return u == null ? 0.0F : u.itemHudLift();
   }

   public static boolean nametag(Render<?> render, Entity entity, String str, double x, double y, double z, int maxDistance) {
      UiModule u = ui();
      return u != null && u.nametagHook(render, entity, str, x, y, z, maxDistance);
   }

   public static boolean nametagSneaking(RendererLivingEntity<?> render, EntityLivingBase entity, double x, double y, double z) {
      UiModule u = ui();
      return u != null && u.nametagSneakingHook(render, entity, x, y, z);
   }

   public static Boolean nametagOwn(EntityLivingBase entity) {
      UiModule u = ui();
      return u == null ? null : u.nametagOwnHook(entity);
   }

   public static boolean chatDraw(GuiNewChat chat, int updateCounter) {
      UiModule u = ui();
      return u != null && u.chatDrawHook(chat, updateCounter);
   }

   public static boolean chatAnswersHits() {
      UiModule u = ui();
      return u != null && u.chatAnswersHits();
   }

   public static IChatComponent chatHit(GuiNewChat chat, int mouseX, int mouseY) {
      UiModule u = ui();
      return u == null ? null : u.chatHitHook(chat, mouseX, mouseY);
   }

   public static void chatScroll(int amount) {
      UiModule u = ui();
      if (u != null) {
         u.chatScrollHook(amount, false);
      }
   }

   public static void chatResetScroll() {
      UiModule u = ui();
      if (u != null) {
         u.chatScrollHook(0, true);
      }
   }

   public static void chatSettingLine(boolean inside) {
      UiModule u = ui();
      if (u != null) {
         u.chat().settingChatLine(inside);
      }
   }

   public static GuiTextField chatInit(GuiChat screen, GuiTextField field, int width, int height) {
      UiModule u = ui();
      return u == null ? null : u.chatInitHook(screen, field, width, height);
   }

   public static GuiTextField chatInput(GuiChat screen, GuiTextField field, int width, int height, List<GuiButton> buttons) {
      UiModule u = ui();
      return u == null ? null : u.chatInputHook(screen, field, width, height, buttons);
   }

   public static int chatInputBackground(GuiChat screen, GuiTextField field, int color) {
      UiModule u = ui();
      return u == null ? color : u.chatInputBackgroundHook(screen, field, color);
   }

   public static void chatText(GuiChat screen, GuiTextField field, boolean before) {
      UiModule u = ui();
      if (u != null) {
         u.chatTextHook(screen, field, before);
      }
   }

   public static void chatPosition(int x, int y) {
      UiModule u = ui();
      if (u != null) {
         u.chat().eventPosition(x, y);
      }
   }

   public static float chatInputClearance() {
      UiModule u = ui();
      return u == null ? 17.0F : u.chat().inputClearance();
   }

   public static float[] chatTypingRow() {
      UiModule u = ui();
      return u != null && u.chatMovable() ? u.chat().typingRow() : null;
   }

   public static boolean installed(String hook, Class<?> target) {
      if (!looked) {
         looked = true;
         pluginLoaded = true;
         installed = Hooks.INSTALLED;
      }

      if (pluginLoaded && installed != null) {
         return installed.contains(hook);
      } else {
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
            FuriahsVisuals.LOG.warn("UI: could not look for the {} hook in {}", new Object[]{hook, target.getName(), var9});
         }

         return false;
      }
   }
}
