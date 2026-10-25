package meowtils.furiahvisuals.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.gui.GuiCheck;
import meowtils.furiahvisuals.gui.GuiColors;
import meowtils.furiahvisuals.gui.UiProbe;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.watermark.IslandCheck;
import meowtils.furiahvisuals.module.weapons.WeaponsModule;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

final class FuriahCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "furiah";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/furiah [movehud | dummy [remove|jump|run|kill [crit]] | weapon [reload|play <clip>] | hooks | gui [check|overlay|colors]]";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("furiahsvisuals");
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (FuriahsVisuals.instance == null || !FuriahsVisuals.instance.isRunning()) {
            FuriahsVisuals.chat("Furiah's Visuals is not initialized.");
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (args.length == 0) {
            FuriahsVisuals.instance.openMenuNextTick();
            return;
        }
        if ("movehud".equalsIgnoreCase(args[0]) || "hud".equalsIgnoreCase(args[0])) {
            if (mc.theWorld != null) {
                FuriahsVisuals.instance.openMoveHudNextTick();
            }
            return;
        }
        if ("hooks".equalsIgnoreCase(args[0])) {
            java.util.List<String> on = new java.util.ArrayList<String>(meowtils.furiahvisuals.hook.Hooks.INSTALLED);
            Collections.sort(on);
            FuriahsVisuals.chat("Hooks active (" + on.size() + "): " + (on.isEmpty() ? "none" : String.join(", ", on)));
            return;
        }
        if ("gui".equalsIgnoreCase(args[0])) {
            String what = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
            if (what.equals("check") && args.length > 2 && args[2].equalsIgnoreCase("island")) {
                if (!IslandCheck.start("the menu was not checked", 0)) {
                    FuriahsVisuals.chat("The island's check needs a world and the WaterMark on.");
                }
            } else if (what.equals("check")) {
                GuiCheck.request();
            } else if (what.equals("overlay")) {
                UiProbe.overlay = !UiProbe.overlay;
                FuriahsVisuals.chat(UiProbe.overlay
                    ? "GUI overlay on: the menu outlines every control and text, overlapping ones in red."
                    : "GUI overlay off.");
            } else if (what.equals("colors") || what.equals("colours")) {
                GuiColors.request();
            } else {
                FuriahsVisuals.chat("Usage: /furiah gui check [island] | overlay | colors");
            }
            return;
        }
        if ("weapon".equalsIgnoreCase(args[0]) || "weapons".equalsIgnoreCase(args[0])) {
            Module module = FuriahsVisuals.instance.getModules().get("weapons");
            if (module instanceof WeaponsModule) {
                FuriahsVisuals.chat(((WeaponsModule) module).command(Arrays.copyOfRange(args, 1, args.length)));
            }
            return;
        }
        if (!"dummy".equalsIgnoreCase(args[0])) {
            FuriahsVisuals.chat("Usage: " + getCommandUsage(sender));
            return;
        }
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        if (action.isEmpty() || action.equals("spawn")) {
            TestDummy.spawn(mc);
        } else if (action.equals("remove")) {
            TestDummy.remove(mc, true);
        } else if (action.equals("run")) {
            if (TestDummy.runAround(mc, Long.MAX_VALUE, true)) {
                FuriahsVisuals.chat("The test dummy runs circles around you to try Trails; /furiah dummy remove stops it.");
            }
        } else if (action.equals("kill")) {
            boolean crit = args.length > 2 && "crit".equalsIgnoreCase(args[2]);
            if (!mc.isSingleplayer()) {
                FuriahsVisuals.chat("The test dummy only works in singleplayer.");
            } else if (TestDummy.isDead()) {
                FuriahsVisuals.chat("The test dummy is already dead; it comes back in a moment.");
            } else if (!FuriahsVisuals.instance.getKillEffects().isEnabled()) {
                FuriahsVisuals.chat("Kill Effects is switched off.");
            } else {
                FuriahsVisuals.instance.getKillEffects().testKill(mc, crit);
            }
        } else if (action.equals("jump")) {
            BooleanSetting jumps = dummyJumpSetting();
            boolean on = !TestDummy.isJumping();
            if (jumps != null) {
                jumps.set(on);
            } else {
                TestDummy.setJumping(on);
            }
            FuriahsVisuals.chat(on ? "The test dummy now jumps every 2 seconds." : "The test dummy stopped jumping.");
        } else {
            FuriahsVisuals.chat("Usage: " + getCommandUsage(sender));
        }
    }

    private static BooleanSetting dummyJumpSetting() {
        Module module = FuriahsVisuals.instance.getModules().get("landing_impact");
        if (module == null) {
            return null;
        }
        for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting && setting.getId().equals("dummyJumps")) {
                return (BooleanSetting) setting;
            }
        }
        return null;
    }
}
