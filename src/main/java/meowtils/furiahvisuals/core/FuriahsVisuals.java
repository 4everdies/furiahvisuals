package meowtils.furiahvisuals.core;

import java.io.File;
import java.util.Arrays;
import meowtils.furiahvisuals.core.Combat;
import meowtils.furiahvisuals.core.ConfigManager;
import meowtils.furiahvisuals.core.EventHandler;
import meowtils.furiahvisuals.core.FuriahCommand;
import meowtils.furiahvisuals.core.HitConfirmer;
import meowtils.furiahvisuals.core.Keybinds;
import meowtils.furiahvisuals.core.ModuleManager;
import meowtils.furiahvisuals.core.PresetManager;
import meowtils.furiahvisuals.fx.kit.Comic;
import meowtils.furiahvisuals.fx.kit.KitTextures;
import meowtils.furiahvisuals.gui.FuriahScreen;
import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.gui.MenuPrefs;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.hook.Bridge;
import meowtils.furiahvisuals.hook.Install;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.ambience.AmbienceModule;
import meowtils.furiahvisuals.module.glow.GlowModule;
import meowtils.furiahvisuals.module.hit.HitEffectModule;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.landing.LandingImpactModule;
import meowtils.furiahvisuals.module.misc.MiscModule;
import meowtils.furiahvisuals.module.motionblur.MotionBlurModule;
import meowtils.furiahvisuals.module.particles.ParticlesModule;
import meowtils.furiahvisuals.module.sky.SkyModule;
import meowtils.furiahvisuals.module.targetaura.TargetAuraModule;
import meowtils.furiahvisuals.module.trails.TrailsModule;
import meowtils.furiahvisuals.module.ui.UiModule;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.module.weapons.WeaponsModule;
import meowtils.furiahvisuals.render.material.Matcaps;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingDefaults;
import meowtils.furiahvisuals.setting.StringMapSetting;
import meowtils.furiahvisuals.setting.TextSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.SoundSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.client.ClientCommandHandler;

public final class FuriahsVisuals {
    public static final String MODID = "furiahsvisuals";
    public static final String NAME = "Furiah's Visuals";
    public static final String VERSION = "1.3.5";
    public static final Logger LOG = LogManager.getLogger((String)"Furiah's Visuals");
    public static FuriahsVisuals instance;
    private final ModuleManager modules = new ModuleManager();
    private final HitConfirmer hitConfirmer = new HitConfirmer();
    private ConfigManager config;
    private final ModeSetting<Theme.Style> theme = new ModeSetting<Theme.Style>("theme", "Theme", "Look of this menu.", Theme.Style.class, Theme.Style.DARK);
    private final ColorSetting globalColor = ColorSetting.global("globalColor", "Global Color", "The color every effect set to Global uses.", ColorSetting.Mode.GRADIENT, -45096, -9806593, 1.0);
    private final BooleanSetting reduceFlashing = (BooleanSetting)new BooleanSetting("reduceFlashing", "Reduce Flashing", "No effect flashes the screen (Kill Effects' screen pulse stays off).", false).personal();
    private final SoundSettings sounds = new SoundSettings();
    private final TextSetting activePreset = new TextSetting("activePreset", "Active preset", "", "", 32);
    private final StringMapSetting menuMemory = new StringMapSetting("menu");
    private PresetManager presets;
    private WaterMarkModule watermark;
    private AmbienceModule ambience;
    private SkyModule sky;
    private KillEffectsModule killEffects;
    private UiModule ui;
    private File configDir;
    private boolean openMenuPending;
    private boolean openMoveHudPending;
    private boolean running;

    public static FuriahsVisuals start(File configDir) {
        FuriahsVisuals mod;
        instance = mod = new FuriahsVisuals();
        mod.preInit(configDir);
        mod.init();
        return mod;
    }

    private void preInit(File configDir) {
        this.configDir = configDir;
        this.config = new ConfigManager(new File(this.configDir, "furiahsvisuals.json"), this.modules);
    }

    private void init() {
        ColorSetting.setGlobal(this.globalColor);
        this.registerModules();
        this.config.addGeneral(this.theme);
        this.config.addGeneral(this.globalColor);
        this.config.addGeneral(this.reduceFlashing);
        for (Setting s : this.sounds.all()) {
            if (!s.isSaved()) continue;
            this.config.addGeneral(s);
        }
        ModSounds.init(this.sounds);
        this.config.addGeneralNotInPresets(this.activePreset);
        this.config.addGeneralNotInPresets(this.menuMemory);
        MenuMemory.attach(this.menuMemory);
        for (Setting s : MenuPrefs.all()) {
            this.config.addGeneralNotInPresets(s);
        }
        this.presets = new PresetManager(new File(this.configDir, "furiahsvisuals/presets"), this.config, this.modules, this.activePreset);
        for (Module m : this.modules.getModules()) {
            SettingDefaults.capture(m.getSettings());
        }
        SettingDefaults.capture(Arrays.asList(this.theme, this.globalColor, this.reduceFlashing));
        SettingDefaults.capture(Arrays.asList(this.sounds.all()));
        SettingDefaults.capture(MenuPrefs.all());
        this.config.onFirstStart(new Runnable(){

            @Override
            public void run() {
                for (Module m : FuriahsVisuals.this.modules.getModules()) {
                    if (m == FuriahsVisuals.this.watermark) continue;
                    m.setEnabled(false);
                }
                LOG.info("First start: every section is off but the WaterMark");
            }
        });
        this.config.load();
        this.config.attachListeners();
        this.watermark.connect(this.config, this.presets);
        Keybinds.register(this.modules);
        Bridge.attach(new EventHandler());
        ClientCommandHandler.instance.registerCommand(new FuriahCommand());
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){

            @Override
            public void run() {
                FuriahsVisuals.this.config.saveIfDirty();
                FuriahsVisuals.this.watermark.shutdown();
            }
        }, "Furiah's Visuals config save"));
        Matcaps.prefetch();
        Comic.warmUp();
        KitTextures.warmUp();
    }

    public void resume() {
        this.running = true;
        Install.install();
    }

    public void pause() {
        this.running = false;
        Install.uninstall();
        try {
            this.config.saveIfDirty();
        }
        catch (RuntimeException e) {
            LOG.error("Could not save the settings", (Throwable)e);
        }
    }

    public boolean isRunning() {
        return this.running;
    }

    private void registerModules() {
        this.modules.register(new TargetAuraModule());
        this.modules.register(new HitEffectModule());
        this.killEffects = new KillEffectsModule();
        this.modules.register(this.killEffects);
        this.modules.register(new LandingImpactModule());
        this.modules.register(new TrailsModule());
        this.modules.register(new WeaponsModule());
        this.modules.register(new GlowModule());
        this.modules.register(new ParticlesModule());
        this.ambience = new AmbienceModule();
        this.modules.register(this.ambience);
        this.sky = new SkyModule();
        this.modules.register(this.sky);
        this.modules.register(new MotionBlurModule());
        this.watermark = new WaterMarkModule();
        this.modules.register(this.watermark);
        this.ui = new UiModule();
        this.modules.register(this.ui);
        this.modules.register(new MiscModule());
    }

    public WaterMarkModule getWaterMark() {
        return this.watermark;
    }

    public UiModule getUi() {
        return this.ui;
    }

    public AmbienceModule getAmbience() {
        return this.ambience;
    }

    public SkyModule getSky() {
        return this.sky;
    }

    public KillEffectsModule getKillEffects() {
        return this.killEffects;
    }

    public BooleanSetting getReduceFlashing() {
        return this.reduceFlashing;
    }

    public boolean isReduceFlashing() {
        return this.reduceFlashing.get();
    }

    public SoundSettings getSounds() {
        return this.sounds;
    }

    public void toggleSoundMute() {
        this.sounds.mute.set(!this.sounds.mute.get());
        if (this.watermark != null) {
            this.watermark.soundMuteChanged(this.sounds.mute.get());
        }
    }

    public ModuleManager getModules() {
        return this.modules;
    }

    public HitConfirmer getHitConfirmer() {
        return this.hitConfirmer;
    }

    void onLocalAttack(EntityPlayer attacker, Entity target) {
        this.hitConfirmer.attack(target);
        Combat.attacked();
        this.modules.attack(attacker, target);
    }

    public ConfigManager getConfig() {
        return this.config;
    }

    public PresetManager getPresets() {
        return this.presets;
    }

    public File getDataDir() {
        return new File(this.configDir, MODID);
    }

    public ModeSetting<Theme.Style> getTheme() {
        return this.theme;
    }

    public ColorSetting getGlobalColor() {
        return this.globalColor;
    }

    public int setAllEffectsToGlobal() {
        int changed = 0;
        for (Module m : this.modules.getModules()) {
            for (Setting s : m.getSettings()) {
                ColorSetting c;
                if (!(s instanceof ColorSetting) || !(c = (ColorSetting)s).followsSetAll() || c.getMode() == ColorSetting.Mode.GLOBAL) continue;
                c.setMode(ColorSetting.Mode.GLOBAL);
                ++changed;
            }
        }
        return changed;
    }

    public void openMenu() {
        Minecraft.getMinecraft().displayGuiScreen((GuiScreen)new FuriahScreen(null));
    }

    public static void chat(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage((IChatComponent)new ChatComponentText("\u00a7d[Furiah's Visuals] \u00a77" + message));
        }
    }

    public void openMenuNextTick() {
        this.openMenuPending = true;
    }

    void openMoveHudNextTick() {
        this.openMoveHudPending = true;
    }

    void tick() {
        if (this.openMenuPending) {
            this.openMenuPending = false;
            this.openMenu();
        }
        if (this.openMoveHudPending) {
            this.openMoveHudPending = false;
            this.ui.openMoveHud();
        }
    }
}
