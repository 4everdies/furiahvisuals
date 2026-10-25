package meowtils.furiahvisuals.standalone.proxy;

import java.io.File;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.standalone.ForgeClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;

public final class ClientProxy extends CommonProxy {
    @Override
    public void initialize() {
        MinecraftForge.EVENT_BUS.register(new ForgeClientEvents());
        FMLCommonHandler.instance().bus().register(new ForgeClientEvents());
        File configDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
        FuriahsVisuals.start(configDir).resume();
        FuriahsVisuals.LOG.info("Furiah's Visuals {} initialized as a standalone Forge 1.8.9 mod.", FuriahsVisuals.VERSION);
    }
}
