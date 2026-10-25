package meowtils.furiahvisuals.hook;

import meowtils.furiahvisuals.core.EventHandler;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.mixin.AccessorMinecraft;
import meowtils.furiahvisuals.module.misc.MiscHooks;
import meowtils.furiahvisuals.render.post.PostChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.Timer;

public final class Bridge {
   private static volatile EventHandler events;
   private static volatile boolean active;
   private static float partialTicks;
   private static boolean inWorld;

   private Bridge() {
   }

   public static void attach(EventHandler handler) {
      events = handler;
   }

   public static EventHandler events() {
      return events;
   }

   public static boolean active() {
      return active && events != null && FuriahsVisuals.instance != null;
   }

   static void setActive(boolean on) {
      active = on;
   }

   public static float partialTicks() {
      return partialTicks;
   }

   public static boolean inWorldRender() {
      return inWorld;
   }

   static void frameStart() {
      Timer t = AccessorMinecraft.getFvTimer(Minecraft.getMinecraft());
      if (t != null) {
         partialTicks = t.renderPartialTicks;
      }

      inWorld = false;
      Install.sync();
      if (active()) {
         events.onRenderTickStart(partialTicks);
      }
   }

   static void frameEnd() {
      inWorld = false;
      WorldHooks.frameEnd();
      HudHooks.frameEnd();
      Minecraft mc = Minecraft.getMinecraft();
      if (active() && mc.currentScreen != null) {
         events.onDrawScreen(mc.currentScreen);
      }
   }

   static void levelStart() {
      inWorld = true;
      if (active()) {
         MiscHooks.lightmapStart(Minecraft.getMinecraft().entityRenderer);
      }
   }

   static void lightmapDone() {
      if (active()) {
         MiscHooks.lightmapEnd();
      }
   }

   static void levelEnd() {
      inWorld = false;
   }

   static void hand() {
      if (active()) {
         Minecraft mc = Minecraft.getMinecraft();
         int pass = mc.gameSettings.anaglyph ? EntityRenderer.anaglyphField : 2;
         PostChain.beforeHandHook(pass);
         events.onRenderHand(partialTicks, pass);
         CameraHooks.hurtApply();
      }
   }

   static void beforeHud() {
      if (active()) {
         PostChain.beforeHudHook(partialTicks);
         HudHooks.guiStart(partialTicks);
      }
   }

   static void afterHud() {
      if (active()) {
         HudHooks.guiEnd(partialTicks);
      }
   }
}
