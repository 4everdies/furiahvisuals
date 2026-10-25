package meowtils.furiahvisuals.module.ui.chat;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.hud.CostMeter;
import meowtils.furiahvisuals.hud.HudEditor;
import meowtils.furiahvisuals.hud.HudGl;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.hud.HudPosition;
import meowtils.furiahvisuals.mixin.AccessorGuiNewChat;
import meowtils.furiahvisuals.mixin.AccessorGuiTextField;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.ui.LabyCompat;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.module.ui.UiElement;
import meowtils.furiahvisuals.module.ui.UiHooks;
import meowtils.furiahvisuals.module.ui.UiModule;
import meowtils.furiahvisuals.module.ui.scoreboard.ScoreboardElement;
import meowtils.furiahvisuals.module.watermark.Spring;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer.EnumChatVisibility;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Mouse;

public final class ChatElement extends UiElement {
   ModeSetting<ScoreboardElement.Kind> kind;
   ModeSetting<ChatElement.Background> background;
   SliderSetting opacity;
   ModeSetting<HudLook.Mode> look;
   SliderSetting radius;
   BooleanSetting border;
   ModeSetting<WaterMarkModule.GlassMode> glass;
   ModeSetting<HudLook.Glint> glint;
   ModeSetting<ScoreboardElement.Font> font;
   BooleanSetting shadow;
   SliderSetting spacing;
   ModeSetting<ChatElement.TypingBar> typingBar;
   BooleanSetting scrollbar;
   BooleanSetting animate;
   BooleanSetting input;
   BooleanSetting timestamps;
   BooleanSetting stack;
   BooleanSetting mentions;
   BooleanSetting newPill;
   ModeSetting<ChatElement.Clock> clock;
   SliderSetting width;
   SliderSetting heightClosed;
   SliderSetting heightOpen;
   BooleanSetting sizeTaken;
   SliderSetting scale;
   HudPosition position;
   private final ChatRenderer renderer = new ChatRenderer();
   final HudLook hudLook = new HudLook();
   private final ChatRenderer.Style style = new ChatRenderer.Style();
   private final ChatRenderer.Frame frame = new ChatRenderer.Frame();
   private final CostMeter cost = new CostMeter();
   private long lastDraw;
   private float partialTicks;
   private float lastSf = 2.0F;
   private float lastScale = 1.0F;
   private float lastLeft = 2.0F;
   private float lastBottom;
   private Boolean hooksIn;
   private Boolean inputHooksIn;
   private boolean inSetChatLine;
   private float eventX;
   private float eventY;
   private boolean eventFresh;
   private boolean dragging;
   private float grab;
   static final float ROW_GUI = 13.0F;
   static final float TEXT_TOP = 3.0F;
   static final int TEXT_PAD = 4;
   private static final int COUNTER_W = 26;
   private static final int COUNTER_FROM = 20;
   private static final float EDGE_FADE = 8.0F;
   private GuiChat inputScreen;
   private float inputT;
   private long inputSince;
   private float rowT;
   private float rowX;
   private float rowY;
   private float rowW;
   private GuiTextField ourField;
   private GuiTextField fieldSeen;
   private GuiChat fieldScreen;
   private final float[][] screenButtons = new float[8][];
   private int screenButtonCount;
   private String ghostText = "";
   private int ghostX;
   private int ghostY;
   private int savedCounter;
   private int savedColor;
   private boolean textMoved;
   private boolean edgeFade;
   private String lastText = "";
   private int lastCursor = -1;
   private long caretMoved;
   private static final float SCREEN_ROOM = 34.0F;
   private boolean reshaped;
   private boolean failed;
   private GuiTextField movedField;
   private final Spring lift = new Spring();
   private final float[] lastBox = new float[4];
   ChatPreviewWidget lastPreview;

   public ChatElement(UiModule module, MiniSection section) {
      super(module, section);
   }

   @Override
   public void addSettings() {
      String top = this.section.getName();
      this.own(
         new ChatElement.Row("chat.preview", "Preview", "The chat as it looks in game; the buttons try its animations and extras.", new Supplier<Widget>() {
            public Widget get() {
               return new ChatPreviewWidget(ChatElement.this);
            }
         }).keywords(new String[]{"chat", "preview", "messages"}), top
      );
      String st = "Style";
      this.kind = this.own(
         new ModeSetting<>(
               "chat.style", "Style", "Vanilla's dark strips, or the island's themed look.", ScoreboardElement.Kind.class, ScoreboardElement.Kind.THEMED
            )
            .keywords(new String[]{"chat", "themed"}),
         st
      );
      BooleanSupplier themed = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ChatElement.this.kind.get() == ScoreboardElement.Kind.THEMED;
         }
      };
      this.background = this.own(
         new ModeSetting<>("chat.background", "Background", "What sits behind the messages.", ChatElement.Background.class, ChatElement.Background.PANEL)
            .keywords(new String[]{"bubbles", "panel"}),
         st
      );
      BooleanSupplier bgOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ChatElement.this.background.get() != ChatElement.Background.NONE;
         }
      };
      this.opacity = this.own(
         new SliderSetting(
               "chat.opacity",
               "Background Opacity",
               "How solid the background is (100%: as the style has it; Minecraft's own chat opacity applies too).",
               0.1,
               1.0,
               1.0,
               0.05
            )
            .visibleWhen(bgOn),
         st
      );
      this.look = this.own(
         new ModeSetting<>(
               "chat.look", "Look", "The island's own look, or the chat's own corner radius, border and glass.", HudLook.Mode.class, HudLook.Mode.SAME
            )
            .visibleWhen(and(themed, bgOn)),
         st
      );
      BooleanSupplier custom = and(and(themed, bgOn), new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ChatElement.this.look.get() == HudLook.Mode.CUSTOM;
         }
      });
      this.radius = this.own(new SliderSetting("chat.radius", "Corner Radius", "How round its corners are.", 0.0, 6.0, 4.0, 0.5).visibleWhen(custom), st);
      this.border = this.own(new BooleanSetting("chat.border", "Border", "A thin outline.", true).visibleWhen(custom), st);
      this.glass = this.own(
         new ModeSetting<>(
               "chat.glass",
               "Glass",
               "Frosted glass behind it (from the frame's one shared blur).",
               WaterMarkModule.GlassMode.class,
               WaterMarkModule.GlassMode.AUTO
            )
            .visibleWhen(custom),
         st
      );
      this.glint = this.own(
         new ModeSetting<>(
               "chat.glint", "Glint", "The island's glint, in sync with it (off by default: the chat is read a lot).", HudLook.Glint.class, HudLook.Glint.OFF
            )
            .<ModeSetting>visibleWhen(and(themed, bgOn))
            .keywords(new String[]{"shine"}),
         st
      );
      String tx = "Text";
      this.font = this.own(
         new ModeSetting<>(
               "chat.font",
               "Font",
               "Minecraft's font or the mod's (the input line keeps Minecraft's, so the cursor lands where you click).",
               ScoreboardElement.Font.class,
               ScoreboardElement.Font.MOD
            )
            .keywords(new String[]{"mod font"}),
         tx
      );
      this.shadow = this.own(
         new BooleanSetting("chat.shadow", "Text Shadow", "A drop shadow under the text (vanilla has one; always on without a background).", true), tx
      );
      this.spacing = this.own(
         new SliderSetting(
               "chat.spacing", "Line Spacing", "Extra room between lines (the chat's height setting still decides how tall it is).", 0.0, 4.0, 1.0, 0.5
            )
            .unit(" px"),
         tx
      );
      String io = "Input and Scrollbar";
      this.typingBar = this.own(
         new ModeSetting<>(
               "chat.typingBar",
               "Typing Bar",
               "Where the line you type in goes: the chat box's own bottom row (as wide as the chat, with an accent caret; no dark bar across the screen), or Minecraft's full-width bar.",
               ChatElement.TypingBar.class,
               ChatElement.TypingBar.IN_BOX
            )
            .<ModeSetting>visibleWhen(themed)
            .keywords(new String[]{"input", "input box", "input line", "typing", "text box", "caret", "black bar"}),
         io
      );
      this.input = this.own(new BooleanSetting("chat.input", "Themed Input Line", "Replaced by Typing Bar.", true).nested(), io);
      this.scrollbar = this.own(
         new BooleanSetting("chat.scrollbar", "Themed Scrollbar", "A thin scrollbar you can drag (off: vanilla's).", true)
            .<BooleanSetting>visibleWhen(themed)
            .keywords(new String[]{"scroll"}),
         io
      );
      String sz = "Size";
      this.width = this.own(
         new SliderSetting(
               "chat.width",
               "Width",
               "How wide the chat box is, in GUI pixels: from 100 to the screen's width (Minecraft's own setting stops at 320). Messages wrap to it at once, old ones too.",
               100.0,
               4000.0,
               320.0,
               1.0
            )
            .unit(" px")
            .trackMax(new DoubleSupplier() {
               @Override
               public double getAsDouble() {
                  return ChatElement.this.maxWidth();
               }
            })
            .keywords(new String[]{"chat width", "wide", "wider", "narrow"}),
         sz
      );
      this.heightClosed = this.own(
         this.heightSetting(
            "chat.heightClosed",
            "Height When Closed",
            "How tall the chat is while you aren't typing, in GUI pixels (Minecraft's own setting stops at 180).",
            90.0
         ),
         sz
      );
      this.heightOpen = this.own(
         this.heightSetting("chat.heightOpen", "Height When Open", "How tall the chat is while you type, in GUI pixels, up to the screen's height.", 180.0), sz
      );
      this.sizeTaken = this.own(
         new BooleanSetting("chat.sizeTaken", "Size Taken Over", "Whether Width and the heights started from Minecraft's chat settings.", false).nested(), sz
      );
      this.animate = this.own(
         new BooleanSetting(
               "chat.animate", "Animations", "Opening and closing, new messages sliding in, fading, smooth scrolling and replaced messages crossfading.", true
            )
            .keywords(new String[]{"animation", "open", "close", "slide"}),
         "Animations"
      );
      String ex = "Extras";
      this.timestamps = this.own(
         new BooleanSetting("chat.timestamps", "Timestamps", "A dim time before each message.", false).keywords(new String[]{"time", "clock"}), ex
      );
      this.clock = this.own(
         new ModeSetting<>("chat.clock", "Clock", "How the time is written.", ChatElement.Clock.class, ChatElement.Clock.H24).visibleWhen(on(this.timestamps)),
         ex
      );
      this.stack = this.own(
         new BooleanSetting("chat.stack", "Stack Repeats", "The same message several times in a row shows once, with \"×3\".", false)
            .keywords(new String[]{"spam", "repeat", "duplicate", "compact"}),
         ex
      );
      this.mentions = this.own(
         new BooleanSetting("chat.mentions", "Mention Highlight", "Messages with your name get an accent bar and a soft tint.", false)
            .keywords(new String[]{"ping", "name", "highlight"}),
         ex
      );
      this.newPill = this.own(
         new BooleanSetting("chat.newPill", "New Messages Pill", "Scrolled up when new messages come: a pill says so; click it to jump down.", false)
            .keywords(new String[]{"unread", "jump"}),
         ex
      );
      String la = "Layout";
      this.scale = this.own(
         new SliderSetting(
               "chat.scale",
               "Size",
               "How big the chat is, on top of Minecraft's own Chat Text Size and Width (Move HUD's scroll wheel sets it too).",
               0.5,
               2.0,
               1.0,
               0.05
            )
            .unit("×")
            .keywords(new String[]{"scale", "size", "bigger", "smaller"}),
         la
      );
      this.position = this.own(new HudPosition("chat.position", "Chat Position"), la);
   }

   private SliderSetting heightSetting(String id, String name, String description, double def) {
      final SliderSetting s = new SliderSetting(id, name, description, 20.0, 4000.0, def, 1.0).unit(" px");
      s.info(new Supplier<String>() {
         public String get() {
            return "≈ " + ChatElement.this.linesIn(s.getFloat()) + " lines";
         }
      }, null).trackMax(new DoubleSupplier() {
         @Override
         public double getAsDouble() {
            return ChatElement.this.maxHeight();
         }
      });
      return s.keywords(new String[]{"chat height", "tall", "taller", "lines"});
   }

   int linesIn(float height) {
      return Math.max(1, (int)(height / (9.0F + this.spacing.getFloat())));
   }

   private double maxWidth() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.displayWidth > 0 && mc.displayHeight > 0) {
         ScaledResolution res = new ScaledResolution(mc);
         return Math.max(100.0, Math.floor((res.getScaledWidth() - 4.0) / Math.max(0.05, this.scale.get())));
      } else {
         return Double.NaN;
      }
   }

   private double maxHeight() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.displayWidth > 0 && mc.displayHeight > 0) {
         ScaledResolution res = new ScaledResolution(mc);
         double s = Math.max(0.05, (double)mc.gameSettings.chatScale) * Math.max(0.05, this.scale.get());
         return Math.max(20.0, Math.floor((res.getScaledHeight() - 34.0F) / s));
      } else {
         return Double.NaN;
      }
   }

   @Override
   public void afterLoad() {
      this.reshaped = false;
      if (!this.input.get()) {
         this.typingBar.set(ChatElement.TypingBar.FULL_WIDTH);
         this.input.set(true);
         this.reshaped = true;
      }

      if (!this.sizeTaken.get()) {
         GameSettings gs = Minecraft.getMinecraft().gameSettings;
         if (gs != null) {
            this.width.set(GuiNewChat.calculateChatboxWidth(gs.chatWidth));
            this.heightClosed.set(GuiNewChat.calculateChatboxHeight(gs.chatHeightUnfocused));
            this.heightOpen.set(GuiNewChat.calculateChatboxHeight(gs.chatHeightFocused));
         }

         this.sizeTaken.set(true);
      }
   }

   @Override
   public boolean reshapedOnLoad() {
      return this.reshaped;
   }

   private static BooleanSupplier on(final BooleanSetting b) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return b.get();
         }
      };
   }

   private static BooleanSupplier and(final BooleanSupplier a, final BooleanSupplier b) {
      return new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return a.getAsBoolean() && b.getAsBoolean();
         }
      };
   }

   boolean hooksIn() {
      if (this.hooksIn == null) {
         this.hooksIn = UiHooks.installed("chatDraw", GuiNewChat.class)
            && UiHooks.installed("chatHit", GuiNewChat.class)
            && UiHooks.installed("chatScroll", GuiNewChat.class);
         if (!this.hooksIn) {
            FuriahsVisuals.LOG.warn("UI: the chat hooks are missing (another mod changed GuiNewChat); the chat stays vanilla");
         }
      }

      return this.hooksIn;
   }

   private boolean inputHooksIn() {
      if (this.inputHooksIn == null) {
         this.inputHooksIn = UiHooks.installed("chatInput", GuiChat.class)
            && UiHooks.installed("chatInputBg", GuiChat.class)
            && UiHooks.installed("chatTextBefore", GuiChat.class)
            && UiHooks.installed("chatTextAfter", GuiChat.class)
            && UiHooks.installed("chatInit", GuiChat.class);
      }

      return this.inputHooksIn;
   }

   public boolean active() {
      return this.on() && this.hooksIn() && !LabyCompat.advancedChat();
   }

   public void fail() {
      this.failed = true;
      this.abortInput();
   }

   public void abortInput() {
      if (this.edgeFade) {
         Ui.endFadeX();
         this.edgeFade = false;
      }

      if (this.movedField != null) {
         AccessorGuiTextField acc = AccessorGuiTextField.of(this.movedField);
         acc.setFvCursorCounter(this.savedCounter);
         acc.setFvEnabledColor(this.savedColor);
         this.movedField = null;
      }

      if (this.textMoved) {
         GlStateManager.popMatrix();
         this.textMoved = false;
      }
   }

   @Override
   public String notice() {
      if (this.failed) {
         return "The chat ran into a problem and is vanilla until the game restarts (the log says more).";
      } else if (!this.hooksIn()) {
         return "The chat can't be restyled here: another mod changed how Minecraft draws it, so it stays vanilla.";
      } else if (LabyCompat.advancedChat()) {
         return "LabyMod's Advanced Chat draws the chat here, so this stays off. To use it, turn Advanced Chat off in LabyMod's settings.";
      } else {
         return LabyCompat.present()
            ? "With LabyMod, the messages are restyled; where LabyMod shows its own chat screen, its typing bar stays LabyMod's."
            : null;
      }
   }

   ChatRenderer.Style style() {
      ChatRenderer.Style s = this.style;
      s.themed = this.kind.get() == ScoreboardElement.Kind.THEMED;
      s.modFont = this.font.get() == ScoreboardElement.Font.MOD;
      s.shadow = this.shadow.get();
      s.background = this.background.get().ordinal();
      s.spacing = this.spacing.getFloat();
      s.bgOpacity = this.opacity.getFloat();
      s.themedScrollbar = this.scrollbar.get();
      s.timestamps = this.timestamps.get();
      s.twelveHour = this.clock.get() == ChatElement.Clock.H12;
      s.stack = this.stack.get();
      s.mentions = this.mentions.get();
      s.newPill = this.newPill.get();
      Minecraft mc = Minecraft.getMinecraft();
      s.myName = mc.thePlayer != null ? mc.thePlayer.getName() : mc.getSession().getUsername();
      s.names = this.module.nametag() != null && this.module.nametag().colorsTabAndChat() ? this.module.nametag() : null;
      return s;
   }

   HudLook updateLook(HudLook l, float px, float dt) {
      WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
      l.update(this.look.get(), true, wm.lookOpacity(), this.radius.getFloat(), this.border.get(), this.glass.get(), this.glint.get(), px, dt);
      l.background = true;
      return l;
   }

   float speed() {
      return this.animate.get() ? this.module.speed() : 0.0F;
   }

   @Override
   public boolean wantsGlass() {
      return this.kind.get() == ScoreboardElement.Kind.THEMED
         && this.background.get() != ChatElement.Background.NONE
         && this.hudLook.glass
         && this.renderer.visible()
         && this.active();
   }

   @Override
   public void applyTheme() {
      this.kind.set(ScoreboardElement.Kind.THEMED);
      this.font.set(ScoreboardElement.Font.MOD);
   }

   @Override
   public void onRenderOverlay(float partialTicks) {
      this.partialTicks = partialTicks;
   }

   public void eventPosition(int x, int y) {
      this.eventX = x;
      this.eventY = y;
      this.eventFresh = true;
   }

   public boolean draw(GuiNewChat chat, int updateCounter) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.gameSettings.chatVisibility == EnumChatVisibility.HIDDEN) {
         this.lastDraw = 0L;
         return false;
      } else {
         long t0 = System.nanoTime();
         long now = ClientClock.nanos();
         float dt = this.lastDraw == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastDraw) / 1.0E9F);
         if (this.lastDraw == 0L || now - this.lastDraw > 1000000000L) {
            this.renderer.clear();
            dt = 0.0F;
         }

         this.lastDraw = now;
         ScaledResolution res = new ScaledResolution(mc);
         float sf = res.getScaleFactor();
         float ex = this.eventFresh ? this.eventX : 0.0F;
         float ey = this.eventFresh ? this.eventY : res.getScaledHeight() - 48.0F;
         this.eventFresh = false;
         ChatRenderer.Frame f2 = this.frame;
         this.measure(f2, res);
         f2.open = chat.getChatOpen() || HudEditor.isOpen();
         f2.ticks = updateCounter + this.partialTicks;
         f2.opacity = mc.gameSettings.chatOpacity * 0.9F + 0.1F;
         f2.colors = mc.gameSettings.chatColours;
         boolean rowWanted = f2.rowFull > 0.0F && (this.typingHere(mc) || HudEditor.isOpen());
         float speed = this.speed();
         f2.rowIn = this.rowT = speed <= 0.0F ? (rowWanted ? 1.0F : 0.0F) : Easing.clamp01(this.rowT + (rowWanted ? dt * speed / 0.18F : -dt * speed / 0.15F));
         f2.row = f2.rowFull * Easing.outCubic(this.rowT);
         ChatRenderer.Style st = this.style();
         this.place(f2, st, ex, ey, res, chat.getChatOpen(), dt, false);
         this.rowX = f2.left;
         this.rowY = f2.bottom + ChatRenderer.rowTop() * f2.scale;
         this.rowW = ChatRenderer.openBox(st, f2)[0] * f2.scale;
         this.lastSf = sf;
         this.lastScale = f2.scale;
         this.lastLeft = f2.left;
         this.lastBottom = f2.bottom;
         List<ChatLine> lines = AccessorGuiNewChat.of(chat).getFvChatLines();
         this.renderer.sync(lines, st, updateCounter, System.currentTimeMillis(), ClientClock.millis());
         float savedPx = Ui.px;
         float savedOffset = Ui.offsetY;
         boolean overItems = this.position.moved();
         if (overItems) {
            GlStateManager.disableDepth();
         }

         GlStateManager.pushMatrix();

         try {
            GlStateManager.translate(-ex, -ey + f2.bottom, 0.0F);
            GlStateManager.scale(f2.scale, f2.scale, 1.0F);
            Ui.px = sf * f2.scale;
            Ui.offsetY = f2.bottom / f2.scale;
            boolean clip = this.renderer.wantsClip();
            if (clip) {
               Ui.beginFrame();
            } else {
               Ui.beginFrameQuick(HudGl.program());
            }

            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
            GlStateManager.enableTexture2D();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            try {
               if (chat.getChatOpen()) {
                  this.hover(mc);
               }

               HudLook l = this.updateLook(this.hudLook, Ui.px, dt);
               this.renderer.draw(st, f2, l, dt, this.speed(), l.colors.accent, clip);
               this.drawGhost(mc);
            } finally {
               Ui.endFrame();
            }
         } finally {
            Ui.px = savedPx;
            Ui.offsetY = savedOffset;
            GlStateManager.popMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.disableAlpha();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (overItems) {
               GlStateManager.enableDepth();
            }

            this.cost.add(System.nanoTime() - t0);
         }

         return true;
      }
   }

   private void measure(ChatRenderer.Frame f, ScaledResolution res) {
      Minecraft mc = Minecraft.getMinecraft();
      float size = this.scale.getFloat();
      f.scale = Math.max(0.05F, mc.gameSettings.chatScale) * size;
      f.chatWidth = Math.max(20.0F, Math.min(this.width.getFloat() * size, res.getScaledWidth() - 4.0F));
      float maxH = Math.max(20.0F, (res.getScaledHeight() - 34.0F) / f.scale);
      f.heightOpen = Math.min(this.heightOpen.getFloat(), maxH);
      f.heightClosed = Math.min(this.heightClosed.getFloat(), maxH);
      f.rowFull = this.rowInBox() ? 13.0F / f.scale : 0.0F;
   }

   private void place(ChatRenderer.Frame f, ChatRenderer.Style st, float ex, float ey, ScaledResolution res, boolean typing, float dt, boolean dry) {
      float sw = res.getScaledWidth();
      float sh = res.getScaledHeight();
      float[] box = ChatRenderer.openBox(st, f);
      float bw = box[0] * f.scale;
      float above = box[1] * f.scale;
      float bh = (box[1] + box[2]) * f.scale;
      boolean moved = this.position.moved();
      if (moved) {
         f.left = this.position.x(sw, bw);
         f.bottom = this.position.y(sh, bh) + above;
      } else {
         f.left = ex + 2.0F;
         f.bottom = ey + 20.0F;
      }

      if (!dry) {
         float target;
         if (f.rowFull > 0.0F) {
            target = 0.0F;
            if (typing) {
               float rowTop = f.bottom + ChatRenderer.rowTop() * f.scale;
               float rowBottom = rowTop + f.rowFull * f.scale;
               float[] hb = this.hotbarRect(sw, sh);
               boolean buttons = this.typingHere(Minecraft.getMinecraft());

               for (int pass = 0; pass < 4; pass++) {
                  boolean more = false;

                  for (int i = -1; i < (buttons ? this.screenButtonCount : 0); i++) {
                     float[] o = i < 0 ? hb : this.screenButtons[i];
                     float t = rowTop - target;
                     float b = rowBottom - target;
                     if (f.left < o[0] + o[2] && f.left + bw > o[0] && b > o[1] - 2.0F && t < o[1] + o[3]) {
                        target += b - (o[1] - 2.0F);
                        more = true;
                     }
                  }

                  if (!more) {
                     break;
                  }
               }
            }
         } else {
            target = typing && moved ? Math.max(0.0F, f.bottom - (sh - 28.0F)) : 0.0F;
         }

         float speed;
         if (!((speed = this.speed()) <= 0.0F) && this.lift.isStarted()) {
            this.lift.update(target, 26.0F * speed, dt);
         } else {
            this.lift.snap(target);
         }

         f.bottom = f.bottom - this.lift.value;
         this.lastBox[0] = f.left;
         this.lastBox[1] = f.bottom - above;
         this.lastBox[2] = bw;
         this.lastBox[3] = bh;
      }
   }

   private float[] hotbarRect(float sw, float sh) {
      float[] b = this.module.itemHud() != null && this.module.itemHud().on() ? this.module.itemHud().boundsGui() : null;
      if (b != null) {
         return b;
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         float tall = mc.playerController != null && mc.playerController.shouldDrawHUD() ? 29.0F : 22.0F;
         return new float[]{(int)sw / 2 - 91.0F, sh - tall, 182.0F, tall};
      }
   }

   boolean rowInBox() {
      return this.typingBar.get() == ChatElement.TypingBar.IN_BOX && this.kind.get() == ScoreboardElement.Kind.THEMED && this.inputHooksIn();
   }

   private boolean typingHere(Minecraft mc) {
      return this.ourField != null && this.fieldScreen != null && mc.currentScreen == this.fieldScreen && this.fieldSeen == this.ourField;
   }

   public float[] typingRow() {
      Minecraft mc = Minecraft.getMinecraft();
      boolean open = this.rowInBox() && this.typingHere(mc);
      return (open || !(this.rowT <= 0.01F)) && this.lastDraw != 0L && ClientClock.nanos() - this.lastDraw <= 250000000L
         ? new float[]{this.rowX, this.rowY, this.rowX + this.rowW, this.rowY + 13.0F}
         : null;
   }

   public float[] boundsGui() {
      return this.lastDraw != 0L && ClientClock.nanos() - this.lastDraw <= 250000000L ? (float[])this.lastBox.clone() : null;
   }

   public void placeGui(float left, float top, float w, float h, float sw, float sh) {
      this.position.place(left, top, w, h, sw, sh);
   }

   public SliderSetting scaleSetting() {
      return this.scale;
   }

   public HudPosition positionSetting() {
      return this.position;
   }

   public void resetPlace() {
      this.position.reset();
      this.scale.reset();
      this.width.reset();
      this.heightOpen.reset();
   }

   public float[] resizeGui(float left, float bottom, float w, float h, boolean changeW, boolean changeH, float sw, float sh) {
      Minecraft mc = Minecraft.getMinecraft();
      ScaledResolution res = new ScaledResolution(mc);
      ChatRenderer.Frame f = new ChatRenderer.Frame();
      this.measure(f, res);
      float size = Math.max(0.05F, this.scale.getFloat());
      if (changeW) {
         this.width.set(Math.round(Math.min(w, sw - left - 1.0F) / size - 4.0F * f.scale / size));
      }

      if (changeH) {
         this.height(f, Math.min(h, bottom - 1.0F));
      }

      this.measure(f, res);
      ChatRenderer.Style st = this.style();
      float[] box = ChatRenderer.openBox(st, f);
      float bw = box[0] * f.scale;
      float bh = (box[1] + box[2]) * f.scale;
      this.position.place(left, bottom - bh, bw, bh, sw, sh);
      return new float[]{left, bottom - bh, bw, bh};
   }

   private void height(ChatRenderer.Frame f, float h) {
      this.heightOpen.set(Math.round(h / f.scale - ChatRenderer.boxExtra(f)));
   }

   public String sizeLabel() {
      return this.width.getInt() + " × " + this.heightOpen.getInt() + " px";
   }

   public boolean answersHits() {
      return this.active() && this.lastDraw != 0L && ClientClock.nanos() - this.lastDraw < 250000000L;
   }

   public IChatComponent hit(GuiNewChat chat, int mouseX, int mouseY) {
      Minecraft mc = Minecraft.getMinecraft();
      if (chat.getChatOpen() && mc.gameSettings.chatVisibility != EnumChatVisibility.HIDDEN) {
         float gx = mouseX / this.lastSf;
         float gy = (mc.displayHeight - mouseY - 1) / this.lastSf;
         return this.renderer.partAt(gx / this.lastScale, (gy - this.lastBottom) / this.lastScale);
      } else {
         return null;
      }
   }

   public void scroll(int amount) {
      if (!this.inSetChatLine) {
         this.renderer.scrollBy(amount);
      }
   }

   public void resetScroll() {
      this.renderer.scrollToBottom();
   }

   public void settingChatLine(boolean inside) {
      this.inSetChatLine = inside;
   }

   private float chatX(float windowX) {
      return windowX / this.lastSf / this.lastScale;
   }

   private float chatY(float windowYFromTop) {
      return (windowYFromTop / this.lastSf - this.lastBottom) / this.lastScale;
   }

   private void hover(Minecraft mc) {
      float x = this.chatX(Mouse.getX());
      float y = this.chatY(mc.displayHeight - Mouse.getY() - 1);
      boolean themedBar = this.kind.get() == ScoreboardElement.Kind.THEMED && this.scrollbar.get();
      this.renderer.barState(themedBar && this.renderer.overBar(x, y) != 0, this.dragging);
      if (this.dragging && !Mouse.isButtonDown(0)) {
         this.dragging = false;
      }

      if (this.dragging) {
         this.renderer.scrollToFraction(this.renderer.fractionForThumbTop(y - this.grab));
      }
   }

   public boolean screenMouse(GuiScreen screen) {
      if (!this.answersHits()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         GuiNewChat chat = mc.ingameGUI.getChatGUI();
         if (!chat.getChatOpen()) {
            this.dragging = false;
            return false;
         } else {
            int button = Mouse.getEventButton();
            boolean down = Mouse.getEventButtonState();
            float x = this.chatX(Mouse.getEventX());
            float y = this.chatY(mc.displayHeight - Mouse.getEventY() - 1);
            if (this.dragging && button == 0 && !down) {
               this.dragging = false;
               return true;
            } else if (button != 0 || !down) {
               return false;
            } else if (this.renderer.overPill(x, y)) {
               this.renderer.scrollToBottom();
               chat.resetScroll();
               return true;
            } else {
               boolean themedBar = this.kind.get() == ScoreboardElement.Kind.THEMED && this.scrollbar.get();
               int bar = themedBar ? this.renderer.overBar(x, y) : 0;
               if (bar == 1) {
                  this.dragging = true;
                  this.grab = y - this.renderer.barY;
                  return true;
               } else if (bar == 2) {
                  this.grab = this.renderer.barH / 2.0F;
                  this.renderer.scrollToFraction(this.renderer.fractionForThumbTop(y - this.grab));
                  this.dragging = true;
                  return true;
               } else {
                  return false;
               }
            }
         }
      }
   }

   public GuiTextField initInput(GuiChat screen, GuiTextField old, int width, int height) {
      this.ourField = null;
      this.fieldScreen = null;
      if (old != null && this.active() && this.rowInBox() && !old.getEnableBackgroundDrawing() && Minecraft.getMinecraft().currentScreen == screen) {
         float[] r = this.rowNow();
         Minecraft mc = Minecraft.getMinecraft();
         int fw = Math.max(24, Math.round(r[2]) - 8 - 26);
         GuiTextField f = new GuiTextField(0, mc.fontRendererObj, Math.round(r[0]) + 4, Math.round(r[1] + 3.0F), fw, 12);
         f.setMaxStringLength(old.getMaxStringLength());
         f.setEnableBackgroundDrawing(false);
         f.setCanLoseFocus(false);
         f.setFocused(old.isFocused());
         f.setText(old.getText());
         this.ourField = f;
         this.fieldSeen = f;
         this.fieldScreen = screen;
         return f;
      } else {
         return null;
      }
   }

   private float[] rowNow() {
      if (this.lastDraw != 0L && ClientClock.nanos() - this.lastDraw < 1000000000L && this.rowW > 0.0F) {
         return new float[]{this.rowX, this.rowY, this.rowW};
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         ScaledResolution res = new ScaledResolution(mc);
         ChatRenderer.Frame f = new ChatRenderer.Frame();
         this.measure(f, res);
         ChatRenderer.Style st = this.style();
         this.place(f, st, 0.0F, res.getScaledHeight() - 48.0F, res, true, 0.0F, true);
         return new float[]{f.left, f.bottom + ChatRenderer.rowTop() * f.scale, ChatRenderer.openBox(st, f)[0] * f.scale};
      }
   }

   private boolean inBox(GuiChat screen, GuiTextField field) {
      return field != null
         && field == this.ourField
         && screen == this.fieldScreen
         && this.active()
         && this.rowInBox()
         && Minecraft.getMinecraft().currentScreen == screen
         && field.getVisible();
   }

   public int inputBackground(GuiChat screen, GuiTextField field, int color) {
      return this.inBox(screen, field) ? color & 16777215 : color;
   }

   public GuiTextField inputStart(GuiChat screen, GuiTextField field, int width, int height, List<GuiButton> buttons) {
      this.fieldSeen = field;
      if (!this.inBox(screen, field)) {
         this.inputScreen = null;
         return null;
      } else {
         int n = 0;
         if (buttons != null) {
            for (GuiButton b : buttons) {
               if (b != null && b.visible && n < this.screenButtons.length) {
                  this.screenButtons[n++] = new float[]{b.xPosition, b.yPosition, b.getButtonWidth(), 20.0F};
               }
            }
         }

         this.screenButtonCount = n;
         GuiTextField swapped = null;
         int fw = Math.max(24, Math.round(this.rowW) - 8 - 26);
         if (fw != field.getWidth()) {
            GuiTextField f = new GuiTextField(0, Minecraft.getMinecraft().fontRendererObj, field.xPosition, field.yPosition, fw, 12);
            f.setMaxStringLength(field.getMaxStringLength());
            f.setEnableBackgroundDrawing(false);
            f.setCanLoseFocus(false);
            f.setFocused(field.isFocused());
            f.setText(field.getText());
            f.setCursorPosition(field.getCursorPosition());
            f.setSelectionPos(field.getSelectionEnd());
            this.ourField = f;
            this.fieldSeen = f;
            field = f;
            swapped = f;
         }

         long now = ClientClock.nanos();
         if (screen != this.inputScreen) {
            this.inputScreen = screen;
            this.inputSince = now;
         }

         float speed;
         this.inputT = (speed = this.speed()) <= 0.0F ? 1.0F : Easing.clamp01((float)(now - this.inputSince) / 1.0E9F * speed / 0.18F);
         field.xPosition = Math.round(this.rowX) + 4;
         field.yPosition = Math.round(this.rowY + 3.0F);
         AccessorGuiTextField acc = AccessorGuiTextField.of(field);
         String text = field.getText();
         int from = Math.max(0, Math.min(text.length(), acc.getFvLineScrollOffset()));
         this.ghostText = Minecraft.getMinecraft().fontRendererObj.trimStringToWidth(text.substring(from), field.getWidth());
         this.ghostX = field.xPosition;
         this.ghostY = field.yPosition;
         return swapped;
      }
   }

   private static void restoreScreenState() {
      GlStateManager.enableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void inputText(GuiChat screen, GuiTextField field, boolean before) {
      if (before) {
         this.abortInput();
         if (this.inBox(screen, field)) {
            AccessorGuiTextField acc = AccessorGuiTextField.of(field);
            this.savedCounter = acc.getFvCursorCounter();
            this.savedColor = acc.getFvEnabledColor();
            this.movedField = field;
            acc.setFvCursorCounter(6);
            float e = Easing.outCubic(this.inputT);
            if (e < 1.0F) {
               int alpha = Math.max(4, Math.round((this.savedColor >>> 24 == 0 ? 255 : this.savedColor >>> 24) * e));
               acc.setFvEnabledColor(alpha << 24 | this.savedColor & 16777215);
               GlStateManager.pushMatrix();
               GlStateManager.translate(0.0F, (1.0F - e) * 5.0F, 0.0F);
               this.textMoved = true;
            }

            if (field.getSelectedText().isEmpty()) {
               String text = field.getText();
               int from = acc.getFvLineScrollOffset();
               if (from >= 0 && from <= text.length()) {
                  String shown = Minecraft.getMinecraft().fontRendererObj.trimStringToWidth(text.substring(from), field.getWidth());
                  boolean cutLeft = from > 0;
                  boolean cutRight = from + shown.length() < text.length();
                  if (cutLeft || cutRight) {
                     float sf = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
                     float l = field.xPosition * sf;
                     float r = (field.xPosition + field.getWidth()) * sf;
                     this.edgeFade = Ui.beginFadeX(l, cutLeft ? 8.0F * sf : 0.0F, r, cutRight ? 8.0F * sf : 0.0F);
                  }
               }
            }
         }
      } else {
         boolean themed = this.movedField == field && this.inBox(screen, field);
         float slide = this.textMoved ? (1.0F - Easing.outCubic(this.inputT)) * 5.0F : 0.0F;
         this.abortInput();
         if (themed) {
            this.drawCaret(field, AccessorGuiTextField.of(field), slide);
            this.drawCounter(field);
         }
      }
   }

   private void drawCounter(GuiTextField field) {
      int n = field.getText().length();
      int max;
      if (n >= (max = field.getMaxStringLength()) - 20) {
         Minecraft mc = Minecraft.getMinecraft();
         float sf = new ScaledResolution(mc).getScaleFactor();
         float savedPx = Ui.px;
         float savedOffset = Ui.offsetY;
         Ui.px = sf;
         Ui.offsetY = 0.0F;
         Ui.beginFrameQuick(HudGl.program());

         try {
            HudLook l = this.updateLook(this.hudLook, sf, 0.0F);
            String s = n + "/" + max;
            float size = 6.6F;
            float x = this.rowX + this.rowW - 4.0F - Ui.width(s, size);
            int color = n >= max ? l.colors.accent | 0xFF000000 : ColorUtil.withAlpha(l.colors.text, 0.55F);
            Ui.textMiddle(s, x, this.rowY + 6.5F, size, ColorUtil.mulAlpha(color, Easing.outCubic(this.inputT)), false);
         } finally {
            Ui.endFrame();
            Ui.px = savedPx;
            Ui.offsetY = savedOffset;
            restoreScreenState();
         }
      }
   }

   private void drawCaret(GuiTextField field, AccessorGuiTextField acc, float slide) {
      if (field.isFocused()) {
         Minecraft mc = Minecraft.getMinecraft();
         FontRenderer fr = mc.fontRendererObj;
         String text = field.getText();
         int from = acc.getFvLineScrollOffset();
         int cursor = field.getCursorPosition();
         if (from >= 0 && from <= text.length()) {
            String shown = fr.trimStringToWidth(text.substring(from), field.getWidth());
            int j = cursor - from;
            if (j >= 0 && j <= shown.length()) {
               long now = ClientClock.nanos();
               if (!text.equals(this.lastText) || cursor != this.lastCursor) {
                  this.lastText = text;
                  this.lastCursor = cursor;
                  this.caretMoved = now;
               }

               float t;
               float blink = (t = (float)(now - this.caretMoved) / 1.0E9F) < 0.5F
                  ? 1.0F
                  : Easing.smoothstep(0.2F, 0.8F, 0.5F + 0.5F * (float)Math.cos((t - 0.5F) * Math.PI * 2.0 / 1.06F));
               float sf = new ScaledResolution(mc).getScaleFactor();
               float savedPx = Ui.px;
               float savedOffset = Ui.offsetY;
               Ui.px = sf;
               Ui.offsetY = 0.0F;
               Ui.beginFrameQuick(HudGl.program());

               try {
                  HudLook l = this.updateLook(this.hudLook, sf, 0.0F);
                  float w = Math.max(1.0F, (float)Math.round(sf * 0.5F)) / sf;
                  float x = field.xPosition + fr.getStringWidth(shown.substring(0, j)) - (j > 0 ? 0.25F : 0.0F);
                  float y = field.yPosition - 1.0F + slide;
                  int color = ColorUtil.mulAlpha(l.colors.accent | 0xFF000000, blink * Easing.outCubic(this.inputT));
                  Ui.roundRect(x, y, w, 10.0F, w / 2.0F, color);
               } finally {
                  Ui.endFrame();
                  Ui.px = savedPx;
                  Ui.offsetY = savedOffset;
                  restoreScreenState();
               }
            }
         }
      }
   }

   private void drawGhost(Minecraft mc) {
      boolean screenUp = this.inputScreen != null && mc.currentScreen == this.inputScreen;
      if (!screenUp) {
         this.inputScreen = null;
         if (!(this.rowT <= 0.01F) && !this.ghostText.isEmpty() && !this.typingHere(mc)) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(1.0F / this.frame.scale, 1.0F / this.frame.scale, 1.0F);
            GlStateManager.translate(0.0F, -this.frame.bottom, 0.0F);

            try {
               int alpha = Math.round(255.0F * this.rowT * this.rowT * this.rowT);
               if (alpha >= 4) {
                  mc.fontRendererObj.drawStringWithShadow(this.ghostText, this.ghostX, this.ghostY, alpha << 24 | 14737632);
                  GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                  GlStateManager.enableBlend();
                  GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
               }
            } finally {
               GlStateManager.popMatrix();
            }
         }
      }
   }

   public float inputClearance() {
      return 17.0F;
   }

   @Override
   public void onWorldChange() {
      this.dragging = false;
   }

   public void previewTestForTest(int button) {
      if (this.lastPreview != null) {
         this.lastPreview.test(button);
      }
   }

   public String typingForTest() {
      Minecraft mc = Minecraft.getMinecraft();
      StringBuilder sb = new StringBuilder("typing bar ").append(this.typingBar.get().name());
      ScaledResolution res = new ScaledResolution(mc);
      sb.append(", screen ").append(res.getScaledWidth()).append(" x ").append(res.getScaledHeight());
      sb.append(
         String.format(
            Locale.ROOT,
            ", chat %.0f wide, view %.0f open / %.0f closed (chat units, scale %.2f)",
            this.frame.chatWidth,
            this.frame.heightOpen,
            this.frame.heightClosed,
            this.frame.scale
         )
      );
      if (!(mc.currentScreen instanceof GuiChat)) {
         return sb.append(", chat closed, row in ").append(String.format(Locale.ROOT, "%.2f", this.rowT)).toString();
      } else {
         GuiChat screen = (GuiChat)mc.currentScreen;
         GuiTextField f = this.fieldSeen;
         boolean ours = f != null && f == this.ourField;
         sb.append(ours ? ", the field is in the row" : ", vanilla's field");
         if (f != null) {
            sb.append(" at ").append(f.xPosition).append(", ").append(f.yPosition).append(", ").append(f.getWidth()).append(" wide");
            sb.append(", dark bar ").append(this.inputBackground(screen, f, Integer.MIN_VALUE) >>> 24 == 0 ? "never drawn" : "drawn");
         }

         float[] r;
         if ((r = this.typingRow()) != null) {
            sb.append(String.format(Locale.ROOT, ", row %.1f, %.1f to %.1f, %.1f (in %.2f)", r[0], r[1], r[2], r[3], this.rowT));
            if (f != null) {
               boolean inside = f.xPosition >= r[0]
                  && f.xPosition + f.getWidth() <= r[2]
                  && f.yPosition >= r[1]
                  && f.yPosition + 8 <= r[3];
               sb.append(inside ? ", field inside the row (OK)" : ", field outside the row (FAIL)");
            }
         }

         if (f != null) {
            int n = f.getText().length();
            sb.append(", ").append(n).append(" characters, scrolled by ").append(AccessorGuiTextField.of(f).getFvLineScrollOffset());
            sb.append(ours && n >= f.getMaxStringLength() - 20 ? ", counter shown" : ", no counter");
         }

         return sb.toString();
      }
   }

   public String stateForTest() {
      String s = "hooks "
         + (this.hooksIn() ? "in" : "MISSING")
         + ", input hooks "
         + (this.inputHooksIn() ? "in" : "MISSING")
         + ", "
         + this.renderer.describeForTest()
         + ", cost "
         + this.cost.describe();
      this.cost.reset();
      return s;
   }

   public int[] windowPointForTest(String clickValue) {
      float[] p = this.renderer.findForTest(clickValue);
      if (p == null) {
         return null;
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         float gx = p[0] * this.lastScale;
         float gy = p[1] * this.lastScale + this.lastBottom;
         int wx = Math.round(gx * this.lastSf);
         int wy = mc.displayHeight - 1 - Math.round(gy * this.lastSf);
         return new int[]{wx, wy};
      }
   }

   public int[] pillPointForTest() {
      if (this.renderer.pillW <= 0.0F) {
         return null;
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         float gx = (this.renderer.pillX + this.renderer.pillW / 2.0F) * this.lastScale;
         float gy = (this.renderer.pillY + this.renderer.pillH / 2.0F) * this.lastScale + this.lastBottom;
         return new int[]{Math.round(gx * this.lastSf), mc.displayHeight - 1 - Math.round(gy * this.lastSf)};
      }
   }

   public void clickPillForTest() {
      this.renderer.scrollToBottom();
      Minecraft.getMinecraft().ingameGUI.getChatGUI().resetScroll();
   }

   public static enum Background implements Displayable {
      PANEL("Panel", "One rounded panel behind the lines"),
      BUBBLES("Bubbles", "Each message in its own rounded bubble"),
      NONE("None", "Text only, with a shadow");

      private final String displayName;
      private final String description;

      private Background(String displayName, String description) {
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

   public static enum Clock implements Displayable {
      H24("24-hour", "13:04"),
      H12("12-hour", "1:04");

      private final String displayName;
      private final String description;

      private Clock(String displayName, String description) {
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

   public static enum TypingBar implements Displayable {
      IN_BOX("Part of the chat box", "The chat box's bottom row: as wide as the chat, wherever it is"),
      FULL_WIDTH("Full width (vanilla)", "Minecraft's own dark bar across the bottom of the screen");

      private final String displayName;
      private final String description;

      private TypingBar(String displayName, String description) {
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
}
