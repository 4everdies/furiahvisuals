package meowtils.furiahvisuals.hook;

import java.lang.reflect.Field;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.hook.Bridge;
import meowtils.furiahvisuals.hook.CameraHooks;
import meowtils.furiahvisuals.hook.FvProfiler;
import meowtils.furiahvisuals.hook.Hooks;
import meowtils.furiahvisuals.hook.HudHooks;
import meowtils.furiahvisuals.hook.InputHooks;
import meowtils.furiahvisuals.hook.ParticleSwaps;
import meowtils.furiahvisuals.hook.RenderSwaps;
import meowtils.furiahvisuals.hook.ResourceHooks;
import meowtils.furiahvisuals.hook.SkinSwap;
import meowtils.furiahvisuals.hook.Swap;
import meowtils.furiahvisuals.hook.WorldHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;

public final class Install {
    static final String[] PROFILER_HOOKS = new String[]{"beforeHand", "beforeHud", "lightmapStart", "lightmapEnd"};
    private static final Field PROFILER = profilerField();
    private static boolean installed;
    private static boolean pending;

    private Install() {
    }

    /** O campo e final; se o nome nao resolver (SRG sem tabela), acha-o pelo tipo, que e unico. */
    private static Field profilerField() {
        Field f = Hooks.field(Minecraft.class, "mcProfiler");
        if (f != null) {
            return f;
        }
        for (Field c : Minecraft.class.getDeclaredFields()) {
            if (c.getType() == Profiler.class && !java.lang.reflect.Modifier.isStatic(c.getModifiers())) {
                c.setAccessible(true);
                return c;
            }
        }
        return null;
    }

    public static boolean installed() {
        return installed;
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.ingameGUI == null || mc.getRenderManager() == null || mc.effectRenderer == null || mc.mouseHelper == null || mc.getItemRenderer() == null) {
            pending = true;
            return;
        }
        pending = false;
        installed = true;
        Bridge.setActive(true);
        InputHooks.reset();
        Install.step("resources", new Runnable(){

            @Override
            public void run() {
                ResourceHooks.install();
            }
        });
        Install.step("profiler", new Runnable(){

            @Override
            public void run() {
                FvProfiler f = Swap.upgrade(Minecraft.getMinecraft().mcProfiler, Profiler.class, FvProfiler.class);
                if (f == null || PROFILER == null) {
                    FuriahsVisuals.LOG.warn("Hooks: the profiler could not be swapped; the frame hooks (sky, fog, weather, HUD order, hand) stay off");
                    return;
                }
                Swap.set(PROFILER, Minecraft.getMinecraft(), (Object)f);
                Hooks.on(PROFILER_HOOKS);
            }
        });
        Install.step("world", new Runnable(){

            @Override
            public void run() {
                WorldHooks.sync(true);
                ParticleSwaps.syncWorld(true);
            }
        });
        Install.step("HUD", new Runnable(){

            @Override
            public void run() {
                HudHooks.install();
            }
        });
        Install.step("particles", new Runnable(){

            @Override
            public void run() {
                ParticleSwaps.install();
            }
        });
        Install.step("rendering", new Runnable(){

            @Override
            public void run() {
                RenderSwaps.install();
            }
        });
        Install.step("camera", new Runnable(){

            @Override
            public void run() {
                CameraHooks.install();
            }
        });
        Install.step("skins", new Runnable(){

            @Override
            public void run() {
                SkinSwap.install();
            }
        });
        FuriahsVisuals.LOG.info("Hooks in: {}", new Object[]{Hooks.INSTALLED});
    }

    public static synchronized void uninstall() {
        pending = false;
        if (!installed) {
            return;
        }
        installed = false;
        Bridge.setActive(false);
        Install.step("skins", new Runnable(){

            @Override
            public void run() {
                SkinSwap.uninstall();
            }
        });
        Install.step("camera", new Runnable(){

            @Override
            public void run() {
                CameraHooks.uninstall();
            }
        });
        Install.step("rendering", new Runnable(){

            @Override
            public void run() {
                RenderSwaps.uninstall();
            }
        });
        Install.step("particles", new Runnable(){

            @Override
            public void run() {
                ParticleSwaps.uninstall();
            }
        });
        Install.step("HUD", new Runnable(){

            @Override
            public void run() {
                HudHooks.uninstall();
            }
        });
        Install.step("world", new Runnable(){

            @Override
            public void run() {
                WorldHooks.restoreWeather();
                WorldHooks.restoreTemperature();
                WorldHooks.sync(false);
            }
        });
        Install.step("profiler", new Runnable(){

            @Override
            public void run() {
                Minecraft mc = Minecraft.getMinecraft();
                if (Swap.ours(mc.mcProfiler, FvProfiler.class) && PROFILER != null) {
                    Swap.set(PROFILER, mc, Swap.downgrade(mc.mcProfiler, Profiler.class));
                }
                Hooks.off(PROFILER_HOOKS);
            }
        });
        Install.step("resources", new Runnable(){

            @Override
            public void run() {
                ResourceHooks.uninstall();
            }
        });
        Hooks.INSTALLED.clear();
    }

    private static void step(String what, Runnable r) {
        try {
            r.run();
        }
        catch (Throwable t) {
            FuriahsVisuals.LOG.error("Hooks: the {} step failed", new Object[]{what, t});
        }
    }

    static void sync() {
        if (!installed) {
            return;
        }
        WorldHooks.sync(true);
        ParticleSwaps.syncWorld(true);
        SkinSwap.sync();
        InputHooks.checkScreen();
        HudHooks.syncScreen();
    }

    public static void tickEnd() {
        if (pending) {
            Install.install();
        }
        if (!installed) {
            return;
        }
        ParticleSwaps.tickEnd();
        try {
            Install.sync();
            if (Bridge.active()) {
                Bridge.events().onClientTick();
            }
        }
        catch (Throwable t) {
            FuriahsVisuals.LOG.error("Hooks: the client tick threw", t);
        }
    }

    /** True quando o profiler foi trocado: ai o inicio/fim do frame, o mouse etc. vem das secoes dele. */
    public static boolean profilerHooks() {
        return Hooks.has("beforeHud");
    }

    public static void renderTickStart(float partialTicks) {
        // Com o hook do profiler ativo, Sections ja chama Bridge.frameStart(); nao repetir.
        if (!installed || profilerHooks()) {
            return;
        }
        try {
            Bridge.frameStart();
        }
        catch (Throwable t) {
            FuriahsVisuals.LOG.error("Hooks: renderTickStart threw", t);
        }
    }

    public static void renderTickEnd() {
        if (!installed || profilerHooks()) {
            return;
        }
        try {
            Bridge.frameEnd();
        }
        catch (Throwable t) {
            FuriahsVisuals.LOG.error("Hooks: renderTickEnd threw", t);
        }
    }

    /** RenderWorldLastEvent e a fonte standalone do ciclo de desenho de mundo no Forge. */
    public static void renderWorldLast(float partialTicks) {
        if (!installed || !Bridge.active()) {
            return;
        }
        try {
            Bridge.events().onRenderWorldLast(partialTicks);
            Bridge.events().onRenderWorldLastFinal();
        } catch (Throwable t) {
            FuriahsVisuals.LOG.error("Hooks: Forge RenderWorldLast event threw", t);
        }
    }

    /** GuiOpenEvent: devolve true para cancelar a tela. */
    public static boolean guiOpen(net.minecraft.client.gui.GuiScreen gui) {
        if (!installed || !Bridge.active()) {
            return false;
        }
        boolean cancel = false;
        try {
            cancel = Bridge.events().onGuiOpen(gui);
        }
        catch (Throwable t) {
            FuriahsVisuals.LOG.error("Hooks: onGuiOpen threw", t);
        }
        if (!cancel) {
            InputHooks.screenHandled(gui);
        }
        return cancel;
    }

    /** MouseEvent: so como reserva; com o profiler trocado, InputHooks.mouse() ja le o buffer do LWJGL. */
    public static boolean mouse(int button, boolean pressed, int dwheel) {
        if (!installed || !Bridge.active() || (profilerHooks() && InputHooks.mouseAvailable())) {
            return false;
        }
        try {
            return Bridge.events().onMouse(button, pressed, dwheel);
        }
        catch (Throwable t) {
            FuriahsVisuals.LOG.error("Hooks: onMouse threw", t);
            return false;
        }
    }

    public static void attack(net.minecraft.entity.player.EntityPlayer player, net.minecraft.entity.Entity target) {
        if (!installed) {
            return;
        }
        ParticleSwaps.attack();
        if (Bridge.active()) {
            Bridge.events().onAttack(player, target);
        }
    }

    /** Forge's RenderGameOverlayEvent.Pre: true cancels the element (see HudHooks.forgePre). */
    public static boolean overlayPre(String element) {
        if (!installed || !Bridge.active()) {
            return false;
        }
        try {
            return HudHooks.forgePre(element);
        }
        catch (Throwable t) {
            FuriahsVisuals.LOG.error("Hooks: overlay pre threw", t);
            return false;
        }
    }

    public static void overlayPost(String element) {
        if (!installed || !Bridge.active()) {
            return;
        }
        try {
            HudHooks.forgePost(element);
        }
        catch (Throwable t) {
            FuriahsVisuals.LOG.error("Hooks: overlay post threw", t);
        }
    }

    public static void livingDeath(net.minecraft.entity.player.EntityPlayer player) {
        if (installed && Bridge.active()) {
            try {
                Bridge.events().onLivingDeath(player);
            } catch (Throwable t) {
                FuriahsVisuals.LOG.error("Hooks: onLivingDeath threw", t);
            }
        }
    }

    public static void chat(net.minecraft.util.IChatComponent message, byte type) {
        if (installed && Bridge.active()) {
            Bridge.events().onChat(message, type);
        }
    }

    public static void keyInput() {
        if (installed) {
            InputHooks.keyInput();
        }
    }
}
