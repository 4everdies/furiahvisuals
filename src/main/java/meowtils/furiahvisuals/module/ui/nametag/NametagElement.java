package meowtils.furiahvisuals.module.ui.nametag;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.hud.CostMeter;
import meowtils.furiahvisuals.hud.HudFont;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.module.ui.UiElement;
import meowtils.furiahvisuals.module.ui.UiHooks;
import meowtils.furiahvisuals.module.ui.UiModule;
import meowtils.furiahvisuals.module.ui.scoreboard.ScoreboardElement;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;

public final class NametagElement extends UiElement {
   BooleanSetting myBackground;
   BooleanSetting othersBackground;
   BooleanSetting outline;
   BooleanSetting shadow;
   BooleanSetting shimmer;
   BooleanSetting alsoTabChat;
   BooleanSetting showOwn;
   BooleanSetting border;
   ModeSetting<NametagElement.Shape> shape;
   ModeSetting<HudLook.Mode> look;
   ModeSetting<HudLook.Glint> glint;
   SliderSetting opacity;
   SliderSetting radius;
   ModeSetting<ScoreboardElement.Font> font;
   SliderSetting size;
   SliderSetting colorCount;
   SliderSetting colorSpeed;
   SliderSetting glowStrength;
   ModeSetting<NametagElement.NameColor> nameColor;
   final ColorSetting[] colors = new ColorSetting[4];
   ModeSetting<NametagElement.Direction> direction;
   ModeSetting<NametagElement.Glow> glow;
   ModeSetting<ScoreboardElement.Kind> holograms;
   final NametagRenderer renderer = new NametagRenderer(this);
   final HudLook hudLook = new HudLook();
   final CostMeter cost = new CostMeter();
   private Boolean hookIn;
   private Boolean sneakHookIn;
   private Boolean ownHookIn;
   private boolean failed;
   private int drawnThisFrame;
   private long frameNanos;
   private static Boolean labyMod;
   private final Map<Entity, String> notNames = new WeakHashMap<>();
   private int lastFrame = -1;
   private int frameNumber;
   private long frameSum;
   private final CostMeter frameCost = new CostMeter();
   private long lookNanos;
   static final float LABEL_H = 10.0F;
   NametagPreviewWidget lastPreview;

   public NametagElement(UiModule module, MiniSection section) {
      super(module, section);
   }

   @Override
   public void addSettings() {
      String top = this.section.getName();
      this.own(
         new NametagElement.Row(
               "nametag.preview", "Preview", "Names as they look in game; the buttons try sneaking, a wall and distance.", new Supplier<Widget>() {
                  public Widget get() {
                     return new NametagPreviewWidget(NametagElement.this);
                  }
               }
            )
            .keywords(new String[]{"nametag", "name tag", "preview"}),
         top
      );
      String bg = "Background";
      this.myBackground = this.own(
         new BooleanSetting("nametag.myBackground", "My Background", "The background behind your own name.", true)
            .keywords(new String[]{"nametag", "name tag"}),
         bg
      );
      this.othersBackground = this.own(
         new BooleanSetting(
               "nametag.othersBackground", "Others' Backgrounds", "The background behind other players' names (and holograms', when themed).", true
            )
            .keywords(new String[]{"nametag", "name tag"}),
         bg
      );
      this.shape = this.own(
         new ModeSetting<>("nametag.shape", "Shape", "Vanilla's square or a rounded, themed one.", NametagElement.Shape.class, NametagElement.Shape.ROUNDED)
            .keywords(new String[]{"rounded", "square"}),
         bg
      );
      final BooleanSupplier roundedShape = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return NametagElement.this.shape.get() == NametagElement.Shape.ROUNDED;
         }
      };
      this.look = this.own(
         new ModeSetting<>(
               "nametag.look",
               "Look",
               "The island's own look (its pill shape, opacity and outline, in the theme's colors), or the names' own opacity, corner radius and border.",
               HudLook.Mode.class,
               HudLook.Mode.SAME
            )
            .<ModeSetting>visibleWhen(roundedShape)
            .keywords(new String[]{"sync", "gui", "theme", "watermark"}),
         bg
      );
      BooleanSupplier customLook = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return roundedShape.getAsBoolean() && NametagElement.this.look.get() == HudLook.Mode.CUSTOM;
         }
      };
      this.opacity = this.own(
         new SliderSetting(
               "nametag.opacity", "Background Opacity", "How solid the background is (through walls never more than vanilla's).", 0.1, 1.0, 0.8, 0.05
            )
            .visibleWhen(customLook),
         bg
      );
      this.radius = this.own(
         new SliderSetting("nametag.radius", "Corner Radius", "How round its corners are (5: a pill).", 0.0, 5.0, 3.5, 0.5).visibleWhen(customLook), bg
      );
      this.border = this.own(new BooleanSetting("nametag.border", "Border", "A thin outline in the theme's color.", false).visibleWhen(customLook), bg);
      this.glint = this.own(
         new ModeSetting<>("nametag.glint", "Glint", "The island's glint, in time with it (never through walls).", HudLook.Glint.class, HudLook.Glint.SAME)
            .<ModeSetting>visibleWhen(roundedShape)
            .keywords(new String[]{"shine"}),
         bg
      );
      this.outline = this.own(
         new BooleanSetting("nametag.outline", "Accent Outline", "A thin outline in the accent color.", false).visibleWhen(roundedShape), bg
      );
      String tx = "Text";
      this.font = this.own(
         new ModeSetting<>("nametag.font", "Font", "Minecraft's font or the mod's.", ScoreboardElement.Font.class, ScoreboardElement.Font.MOD)
            .keywords(new String[]{"mod font"}),
         tx
      );
      this.shadow = this.own(new BooleanSetting("nametag.shadow", "Drop Shadow", "A shadow under the letters (vanilla's names have none).", false), tx);
      this.size = this.own(
         new SliderSetting(
               "nametag.size", "Size", "How big names are (from their bottom edge, so the score under a name stays clear).", 50.0, 200.0, 100.0, 5.0
            )
            .unit("%"),
         tx
      );
      String mc = "My Name Color";
      this.nameColor = this.own(
         new ModeSetting<>(
               "nametag.nameColor",
               "My Name Color",
               "Your own name's color, only on your screen.",
               NametagElement.NameColor.class,
               NametagElement.NameColor.VANILLA
            )
            .keywords(new String[]{"gradient", "chroma", "rainbow", "color"}),
         mc
      );
      final BooleanSupplier custom = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return NametagElement.this.nameColor.get() != NametagElement.NameColor.VANILLA
               && NametagElement.this.nameColor.get() != NametagElement.NameColor.CHROMA;
         }
      };
      final BooleanSupplier several = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return NametagElement.this.nameColor.get() == NametagElement.NameColor.GRADIENT
               || NametagElement.this.nameColor.get() == NametagElement.NameColor.ANIMATED;
         }
      };
      this.colorCount = this.own(
         new SliderSetting("nametag.colorCount", "Colors", "How many colors the gradient has.", 2.0, 4.0, 2.0, 1.0).visibleWhen(several), mc
      );
      int[] defaults = new int[]{-45096, -9806593, -11544321, -14257};

      for (int i = 0; i < 4; i++) {
         final int index = i;
         this.colors[i] = this.own(
            new ColorSetting(
                  "nametag.color" + (i + 1),
                  i == 0 ? "Color" : "Color " + (i + 1),
                  "One of your name's colors.",
                  ColorSetting.Mode.STATIC,
                  defaults[i],
                  defaults[(i + 1) % 4],
                  1.0
               )
               .skipSetAll()
               .visibleWhen(new BooleanSupplier() {
                  @Override
                  public boolean getAsBoolean() {
                     return index == 0 ? custom.getAsBoolean() : several.getAsBoolean() && index < NametagElement.this.colorCount.getInt();
                  }
               }),
            mc
         );
      }

      this.colorSpeed = this.own(
         new SliderSetting("nametag.colorSpeed", "Speed", "How fast the colors move.", 0.1, 3.0, 1.0, 0.05)
            .unit("×")
            .visibleWhen(
               new BooleanSupplier() {
                  @Override
                  public boolean getAsBoolean() {
                     return NametagElement.this.nameColor.get() == NametagElement.NameColor.ANIMATED
                        || NametagElement.this.nameColor.get() == NametagElement.NameColor.CHROMA;
                  }
               }
            ),
         mc
      );
      this.direction = this.own(
         new ModeSetting<>("nametag.direction", "Direction", "Which way the colors flow.", NametagElement.Direction.class, NametagElement.Direction.RIGHT)
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return NametagElement.this.nameColor.get() == NametagElement.NameColor.ANIMATED;
               }
            }),
         mc
      );
      this.shimmer = this.own(
         new BooleanSetting("nametag.shimmer", "Shimmer", "Now and then, a soft shine runs across your name's letters.", false)
            .keywords(new String[]{"shine", "sparkle"}),
         mc
      );
      this.alsoTabChat = this.own(
         new BooleanSetting(
               "nametag.alsoTabChat",
               "Also in Tab and Chat",
               "Your name in the same colors in the tab list and the chat (while their mini sections are on).",
               false
            )
            .keywords(new String[]{"tab", "chat"}),
         mc
      );
      String gl = "Glow";
      this.glow = this.own(
         new ModeSetting<>(
               "nametag.glow",
               "Glow",
               "A soft glow behind the letters, in the name's colors (never through walls).",
               NametagElement.Glow.class,
               NametagElement.Glow.OFF
            )
            .keywords(new String[]{"glow", "halo"}),
         gl
      );
      this.glowStrength = this.own(
         new SliderSetting("nametag.glowStrength", "Glow Strength", "How strong the glow is.", 0.1, 1.0, 0.6, 0.05).visibleWhen(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
               return NametagElement.this.glow.get() != NametagElement.Glow.OFF;
            }
         }), gl
      );
      String more = "More";
      this.showOwn = this.own(
         new BooleanSetting("nametag.showOwn", "My Nametag in Third Person", "Your own name above your head in third person (vanilla never shows it).", false)
            .keywords(new String[]{"own name", "f5", "third person"}),
         more
      );
      this.holograms = this.own(
         new ModeSetting<>(
               "nametag.holograms",
               "Holograms and Named Mobs",
               "Floating text (armor stands) and mobs with names: vanilla, or themed like players' names.",
               ScoreboardElement.Kind.class,
               ScoreboardElement.Kind.VANILLA
            )
            .keywords(new String[]{"hologram", "armor stand", "mob", "npc"}),
         more
      );
   }

   @Override
   public void applyTheme() {
      this.shape.set(NametagElement.Shape.ROUNDED);
      this.look.set(HudLook.Mode.SAME);
      this.font.set(ScoreboardElement.Font.MOD);
   }

   boolean hookIn() {
      if (this.hookIn == null) {
         this.hookIn = UiHooks.installed("nametag", Render.class);
         if (!this.hookIn) {
            FuriahsVisuals.LOG.warn("UI: the nametag hook is missing (another mod changed Render.renderLivingLabel); nametags stay vanilla");
         }
      }

      return this.hookIn;
   }

   private boolean sneakHookIn() {
      if (this.sneakHookIn == null) {
         this.sneakHookIn = UiHooks.installed("nametagSneak", RendererLivingEntity.class);
      }

      return this.sneakHookIn;
   }

   private boolean ownHookIn() {
      if (this.ownHookIn == null) {
         this.ownHookIn = UiHooks.installed("nametagOwn", RendererLivingEntity.class);
      }

      return this.ownHookIn;
   }

   public void fail() {
      this.failed = true;
   }

   public boolean active() {
      return this.on() && !this.failed && this.hookIn();
   }

   @Override
   public String notice() {
      if (this.failed) {
         return "Nametags ran into a problem and are vanilla until the game restarts (the log says more).";
      } else if (!this.hookIn()) {
         return "Nametags can't be restyled here: another mod changed how Minecraft draws them, so they stay vanilla.";
      } else {
         return isLabyMod() ? "With LabyMod, restyled names take the place of LabyMod's own nametags (its badges beside names then don't show)." : null;
      }
   }

   private static boolean isLabyMod() {
      if (labyMod == null) {
         try {
            Class.forName("net.labymod.api.Laby", false, NametagElement.class.getClassLoader());
            labyMod = true;
         } catch (Throwable var1) {
            labyMod = false;
         }
      }

      return labyMod;
   }

   NametagElement.Who who(Entity entity, String str) {
      if (entity != null && str != null && this.isName(entity, str)) {
         Minecraft mc = Minecraft.getMinecraft();
         if (entity == mc.thePlayer || entity == mc.getRenderManager().livingPlayer) {
            return NametagElement.Who.ME;
         } else if (entity instanceof EntityPlayer) {
            return NametagElement.Who.OTHER;
         } else {
            return this.holograms.get() == ScoreboardElement.Kind.THEMED ? NametagElement.Who.THING : null;
         }
      } else {
         return null;
      }
   }

   private boolean isName(Entity entity, String str) {
      String quick = entity instanceof EntityPlayer
         ? ScorePlayerTeam.formatPlayerName(((EntityPlayer)entity).getTeam(), entity.getName())
         : entity.getName();
      if (str.length() == quick.length() + 2 && str.startsWith(quick) && str.endsWith("§r")) {
         return true;
      } else if (str.equals(this.notNames.get(entity))) {
         return false;
      } else {
         boolean name = str.equals(entity.getDisplayName().getFormattedText());
         if (!name) {
            this.notNames.put(entity, str);
         }

         return name;
      }
   }

   public boolean label(Render<?> render, Entity entity, String str, double x, double y, double z, int maxDistance) {
      RenderManager rm = render.getRenderManager();
      if (rm != null && rm.livingPlayer != null && !(entity.getDistanceSqToEntity(rm.livingPlayer) > maxDistance * maxDistance)) {
         NametagElement.Who who = this.who(entity, str);
         if (who == null) {
            return false;
         } else {
            this.draw(entity, str, x, y, z, who, entity.isSneaking(), rm);
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean sneakingLabel(RendererLivingEntity<?> render, EntityLivingBase entity, double x, double y, double z) {
      if (!this.sneakHookIn()) {
         return false;
      } else {
         String str = entity.getDisplayName().getFormattedText();
         NametagElement.Who who = this.who(entity, str);
         if (who == null) {
            return false;
         } else {
            double ly = y - (entity.isChild() ? entity.height / 2.0 : 0.0);
            this.draw(entity, str, x, ly, z, who, true, render.getRenderManager());
            return true;
         }
      }
   }

   private static RendererLivingEntity<EntityLivingBase> castRenderer(RendererLivingEntity<?> render) {
      return (RendererLivingEntity<EntityLivingBase>)render;
   }

   public Boolean canRenderName(EntityLivingBase entity) {
      if (this.showOwn.get() && this.ownHookIn()) {
         Minecraft mc = Minecraft.getMinecraft();
         return entity == mc.thePlayer && mc.gameSettings.thirdPersonView != 0
            ? Minecraft.isGuiEnabled() && !entity.isInvisibleToPlayer(mc.thePlayer) && entity.riddenByEntity == null
            : null;
      } else {
         return null;
      }
   }

   private void draw(Entity entity, String str, double x, double y, double z, NametagElement.Who who, boolean sneaking, RenderManager rm) {
      long t0 = System.nanoTime();
      long now = ClientClock.nanos();
      int frame = this.frameNumber;
      if (frame != this.lastFrame || now - this.frameNanos > 50000000L) {
         if (this.frameSum > 0L) {
            this.frameCost.add(this.frameSum);
         }

         this.frameSum = 0L;
         this.lastFrame = frame;
         this.frameNanos = now;
         this.drawnThisFrame = 0;
         this.renderer.newFrame();
      }

      this.drawnThisFrame++;

      try {
         this.renderer.drawWorld(entity, str, x, y, z, who, sneaking, rm);
      } finally {
         long spent = System.nanoTime() - t0;
         this.cost.add(spent);
         this.frameSum += spent;
      }
   }

   @Override
   public void onRenderOverlay(float partialTicks) {
      this.frameNumber++;
   }

   HudLook look(float px) {
      long now = ClientClock.nanos();
      float dt = this.lookNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lookNanos) / 1.0E9F);
      this.lookNanos = now;
      HudLook.Mode mode = this.look.get();
      this.hudLook
         .update(mode, true, this.opacity.getFloat(), this.radius.getFloat(), this.border.get(), WaterMarkModule.GlassMode.OFF, this.glint.get(), px, dt);
      this.hudLook.glass = false;
      this.hudLook.shadow = false;
      this.hudLook.background = true;
      if (mode == HudLook.Mode.SAME) {
         this.hudLook.radius = 5.0F;
      }

      return this.hudLook;
   }

   int nameColorAt(float t, double time) {
      int c;
      switch ((NametagElement.NameColor)this.nameColor.get()) {
         case STATIC:
            c = this.colors[0].getColor(0.0F, time);
            break;
         case GRADIENT:
            c = this.stops(t, time);
            break;
         case ANIMATED:
            float dir = this.direction.get() == NametagElement.Direction.RIGHT ? -1.0F : 1.0F;
            float s = (float)(t + dir * time * this.colorSpeed.getFloat() * 0.35);
            s -= (float)Math.floor(s);
            c = this.loop(s, time);
            break;
         case CHROMA:
            double hue = t * 0.35 - time * this.colorSpeed.getFloat() * 0.2;
            hue -= Math.floor(hue);
            c = ColorUtil.hsv((float)hue, 0.8F, 1.0F) | 0xFF000000;
            break;
         default:
            c = 0;
      }

      return c;
   }

   float shimmerAt(float t, double time) {
      if (!this.shimmer.get()) {
         return 0.0F;
      } else {
         double phase = time % 4.0 / 0.9;
         if (phase > 1.0) {
            return 0.0F;
         } else {
            float center = (float)(phase * 1.5 - 0.25);
            float d = Math.abs(t - center);
            return d >= 0.2F ? 0.0F : 0.6F * (1.0F - d / 0.2F) * (1.0F - d / 0.2F);
         }
      }
   }

   boolean customName() {
      return this.nameColor.get() != NametagElement.NameColor.VANILLA || this.shimmer.get();
   }

   private int stops(float t, double time) {
      int n = Math.max(2, Math.min(4, this.colorCount.getInt()));
      float f = Math.max(0.0F, Math.min(1.0F, t)) * (n - 1);
      int i = Math.min(n - 2, (int)f);
      return ColorUtil.lerpOklab(this.colors[i].getColor(0.0F, time), this.colors[i + 1].getColor(0.0F, time), f - i);
   }

   private int loop(float s, double time) {
      int n = Math.max(2, Math.min(4, this.colorCount.getInt()));
      float f = s * n;
      int i = Math.min(n - 1, (int)f);
      return ColorUtil.lerpOklab(this.colors[i].getColor(0.0F, time), this.colors[(i + 1) % n].getColor(0.0F, time), f - i);
   }

   static String myName() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc.thePlayer != null ? mc.thePlayer.getName() : mc.getSession().getUsername();
   }

   public int[] tintFor(String formatted, int base) {
      if (this.active() && this.customName()) {
         String name = myName();
         return name != null && formatted.indexOf(name) >= 0 ? MyName.tintFormatted(this, formatted, name, ColorSetting.clock(), base) : null;
      } else {
         return null;
      }
   }

   public static void drawLine(NametagElement names, boolean modFont, String formatted, float x, float y, int color, boolean shadow) {
      int[] tint = names == null ? null : names.tintFor(formatted, color);
      if (modFont) {
         HudFont.setTint(tint);
         HudFont.draw(formatted, x, y, color, shadow);
         HudFont.setTint(null);
      } else {
         MyName.drawVanilla(Minecraft.getMinecraft().fontRendererObj, formatted, x, y, color, shadow, tint);
      }
   }

   public boolean colorsTabAndChat() {
      return this.active() && this.alsoTabChat.get() && this.customName();
   }

   @Override
   public void onWorldChange() {
      this.renderer.forget();
   }

   public void previewTestForTest(int button) {
      if (this.lastPreview != null) {
         this.lastPreview.test(button);
      }
   }

   public String stateForTest() {
      String s = "hook "
         + (this.hookIn() ? "in" : "MISSING")
         + ", sneaking hook "
         + (this.sneakHookIn() ? "in" : "MISSING")
         + ", own-name hook "
         + (this.ownHookIn() ? "in" : "MISSING")
         + ", "
         + this.drawnThisFrame
         + " names last frame, "
         + this.renderer.describeForTest()
         + ", cost per name "
         + this.cost.describe();
      this.cost.reset();
      return s;
   }

   public String frameCostForTest() {
      String s = this.frameCost.describe() + " a frame (" + this.drawnThisFrame + " names)";
      this.frameCost.reset();
      return s;
   }

   public static enum Direction implements Displayable {
      RIGHT("Left to Right", "The colors flow to the right"),
      LEFT("Right to Left", "The colors flow to the left");

      private final String displayName;
      private final String description;

      private Direction(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   public static enum Glow implements Displayable {
      OFF("Off", "No glow"),
      MINE("Mine", "A soft glow behind your own name"),
      EVERYONE("Everyone", "A soft glow behind every player's name, in its colors");

      private final String displayName;
      private final String description;

      private Glow(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   public static enum NameColor implements Displayable {
      VANILLA("Vanilla", "Your team's color, as the server sets it"),
      STATIC("Static", "One color"),
      GRADIENT("Gradient", "Two to four colors across your name"),
      ANIMATED("Animated Gradient", "Two to four colors flowing across your name"),
      CHROMA("Chroma", "The rainbow, moving across your name");

      private final String displayName;
      private final String description;

      private NameColor(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   static final class Row extends WidgetSetting {
      private final Supplier<Widget> widget;

      Row(String id, String name, String description, Supplier<Widget> widget) {
         super(id, name, description);
         this.widget = widget;
      }

      @Override
      public Widget createWidget() {
         Widget w = this.widget.get();
         if (w instanceof RowWidget) {
            ((RowWidget)w).row = this;
         }

         return w;
      }
   }

   public static enum Shape implements Displayable {
      VANILLA("Vanilla", "Vanilla's square, dark background"),
      ROUNDED("Rounded", "Rounded, in the theme's background color and opacity");

      private final String displayName;
      private final String description;

      private Shape(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   static enum Who {
      ME,
      OTHER,
      THING;
   }
}
