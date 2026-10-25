package meowtils.furiahvisuals.core;

import meowtils.furiahvisuals.compat.EntityViewRenderEvent;
import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.kit.KitTextures;
import meowtils.furiahvisuals.module.ambience.AmbienceModule;
import meowtils.furiahvisuals.module.ambience.FogCapture;
import meowtils.furiahvisuals.module.misc.MiscHooks;
import meowtils.furiahvisuals.module.ui.UiHooks;
import meowtils.furiahvisuals.module.weapons.WeaponHooks;
import meowtils.furiahvisuals.render.post.PostChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IChatComponent;

public final class EventHandler {
   private final Minecraft mc = Minecraft.getMinecraft();
   private WorldClient lastWorld;

   EventHandler() {
   }

   public void onClientTick() {
      FuriahsVisuals mod = FuriahsVisuals.instance;
      GameClock.tick(this.mc);
      ClientClock.tick();
      PostChain.tick();
      if (this.mc.theWorld != this.lastWorld) {
         this.lastWorld = this.mc.theWorld;
         TestDummy.onWorldChange();
         mod.getHitConfirmer().clear();
         mod.getModules().worldChanged();
      }

      Keybinds.handle(this.mc);
      mod.tick();
      Combat.tick(this.mc);
      mod.getModules().clientTick();
      if (this.mc.theWorld != null && this.mc.thePlayer != null) {
         TestDummy.tick(this.mc);
         mod.getHitConfirmer().tick(this.mc);
         mod.getModules().tick();
      }

      mod.getConfig().tick();
   }

   public void onAttack(EntityPlayer entityPlayer, Entity target) {
      if (this.mc.thePlayer != null && entityPlayer == this.mc.thePlayer) {
         FuriahsVisuals.instance.onLocalAttack(entityPlayer, target);
      }
   }

   public void onLivingDeath(EntityPlayer victim) {
      if (victim != null && victim != this.mc.thePlayer && this.mc.theWorld != null && victim.worldObj == this.mc.theWorld) {
         FuriahsVisuals.instance.getKillEffects().onDeath(victim);
      }
   }

   public boolean onMouse(int button, boolean buttonstate, int dwheel) {
      return WeaponHooks.wheelMouse(button, buttonstate, dwheel) ? true : TestDummy.onMouse(this.mc, button, buttonstate);
   }

   public void onRenderTickStart(float renderTickTime) {
      GameClock.frame(this.mc, renderTickTime);
      ClientClock.frame(this.mc, renderTickTime);
      MiscHooks.frame(this.mc, renderTickTime);
      KitTextures.uploadReady();
      WeaponHooks.wheelFrame();
   }

   public void onKeyInput() {
      WeaponHooks.wheelKeyInput();
   }

   public boolean onGuiOpen(GuiScreen gui) {
      return WeaponHooks.wheelScreen(gui);
   }

   public void onChat(IChatComponent message, byte type) {
      if (type != 2 && message != null && this.mc.theWorld != null) {
         FuriahsVisuals.instance.getModules().chat(message.getFormattedText());
      }
   }

   public boolean onRenderPlayer(EntityPlayer entityPlayer) {
      return entityPlayer != null && FuriahsVisuals.instance.getModules().hidesPlayer(entityPlayer);
   }

   public void onFogColors(EntityViewRenderEvent.FogColors event) {
      if (this.mc.theWorld != null) {
         FuriahsVisuals.instance.getModules().fogColors(event);
      }
   }

   public void onFogColorsFinal(EntityViewRenderEvent.FogColors event) {
      if (this.mc.theWorld != null) {
         FogCapture.colors(event);
      }
   }

   public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
      if (this.mc.theWorld != null) {
         FuriahsVisuals.instance.getModules().renderFog(event);
      }
   }

   public void onRenderFogFinal(EntityViewRenderEvent.RenderFogEvent event) {
      if (this.mc.theWorld != null) {
         AmbienceModule ambience = FuriahsVisuals.instance.getAmbience();
         FogCapture.fog(event, ambience == null ? 1.0F : ambience.fogFactor());
      }
   }

   public void onRenderWorldLast(float partialTicks) {
      if (this.mc.theWorld != null) {
         FxFrame.nextFrame();
         FuriahsVisuals.instance.getModules().renderWorld(partialTicks, this.mc.gameSettings.hideGUI);
      }
   }

   public void onRenderWorldLastFinal() {
      if (this.mc.theWorld != null) {
         PostChain.beforeHandEvent();
      }
   }

   public void onRenderHand(float partialTicks, int renderPass) {
      if (this.mc.theWorld != null) {
         FuriahsVisuals.instance.getModules().renderHand(partialTicks, renderPass);
      }
   }

   public void onRenderOverlay(float partialTicks) {
      if (this.mc.theWorld != null) {
         PostChain.beforeHudEvent(partialTicks);
         FuriahsVisuals.instance.getModules().renderOverlay(partialTicks);
      }
   }

   public boolean onRenderOverlayElement(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      return type != RenderGameOverlayEvent.ElementType.ALL
         && this.mc.theWorld != null
         && FuriahsVisuals.instance.getModules().overlayElement(type, partialTicks);
   }

   public void onChatPosition(int posX, int posY) {
      UiHooks.chatPosition(posX, posY);
   }

   public void onRenderOverlayPost(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      if (this.mc.theWorld != null) {
         if (type == RenderGameOverlayEvent.ElementType.ALL) {
            FuriahsVisuals.instance.getModules().renderHud(partialTicks);
            PostChain.afterHudEvent();
         } else {
            FuriahsVisuals.instance.getModules().overlayElementPost(type, partialTicks);
         }
      }
   }

   public void onDrawScreen(GuiScreen gui) {
      FuriahsVisuals.instance.getModules().screenDrawn(gui);
   }

   public boolean onScreenMouse(GuiScreen gui) {
      return FuriahsVisuals.instance.getModules().screenMouse(gui);
   }

   public boolean onScreenKey(GuiScreen gui) {
      return FuriahsVisuals.instance.getModules().screenKey(gui);
   }
}
