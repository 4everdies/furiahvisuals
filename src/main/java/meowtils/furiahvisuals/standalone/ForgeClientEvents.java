package meowtils.furiahvisuals.standalone;

import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.hook.Bridge;
import meowtils.furiahvisuals.hook.Install;
import meowtils.furiahvisuals.hook.InputHooks;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Adapta os eventos do Forge para o ciclo interno do mod.
 * <p>
 * RenderTick é essencial: sem ele o {@code ClientClock} não avança, a animação
 * de abertura da GUI fica em alpha 0 e só o mundo (Steve) aparece.
 */
public final class ForgeClientEvents {
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && FuriahsVisuals.instance != null) {
            Install.tickEnd();
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (FuriahsVisuals.instance == null) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            Install.renderTickStart(event.renderTickTime);
        } else if (event.phase == TickEvent.Phase.END) {
            Install.renderTickEnd();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (FuriahsVisuals.instance != null) {
            Install.renderWorldLast(event.partialTicks);
        }
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (FuriahsVisuals.instance != null && event.type != null && Install.overlayPre(event.type.name())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onOverlayPost(RenderGameOverlayEvent.Post event) {
        if (FuriahsVisuals.instance != null && event.type != null) {
            Install.overlayPost(event.type.name());
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (FuriahsVisuals.instance != null && event.entityPlayer != null) {
            Install.attack(event.entityPlayer, event.target);
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (FuriahsVisuals.instance != null && event.entityLiving instanceof net.minecraft.entity.player.EntityPlayer
                && event.entityLiving.worldObj != null && event.entityLiving.worldObj.isRemote) {
            Install.livingDeath((net.minecraft.entity.player.EntityPlayer) event.entityLiving);
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (FuriahsVisuals.instance != null && Bridge.active()) {
            Install.chat(event.message, event.type);
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (FuriahsVisuals.instance != null) {
            Install.keyInput();
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (FuriahsVisuals.instance != null && Install.guiOpen(event.gui)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (FuriahsVisuals.instance != null && Install.mouse(event.button, event.buttonstate, event.dwheel)) {
            event.setCanceled(true);
        }
    }
}
