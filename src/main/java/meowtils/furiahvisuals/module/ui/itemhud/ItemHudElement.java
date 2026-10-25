package meowtils.furiahvisuals.module.ui.itemhud;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraftforge.client.GuiIngameForge;
import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.hook.HudHooks;
import meowtils.furiahvisuals.hud.CostMeter;
import meowtils.furiahvisuals.hud.HudGl;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.hud.HudPosition;
import meowtils.furiahvisuals.mixin.AccessorGuiIngame;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.module.ui.UiElement;
import meowtils.furiahvisuals.module.ui.UiHooks;
import meowtils.furiahvisuals.module.ui.UiModule;
import meowtils.furiahvisuals.module.ui.scoreboard.ScoreboardElement;
import meowtils.furiahvisuals.module.watermark.Spring;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public final class ItemHudElement extends UiElement {
   ModeSetting<ScoreboardElement.Kind> kind;
   ModeSetting<ScoreboardElement.Kind> xp;
   SliderSetting opacity;
   ModeSetting<HudLook.Mode> look;
   SliderSetting radius;
   BooleanSetting border;
   ModeSetting<WaterMarkModule.GlassMode> glass;
   ModeSetting<HudLook.Glint> glint;
   BooleanSetting dividers;
   BooleanSetting selectionGlow;
   BooleanSetting counts;
   BooleanSetting durability;
   BooleanSetting hideHearts;
   BooleanSetting hideFood;
   BooleanSetting hideXp;
   BooleanSetting hideLevel;
   BooleanSetting hideArmor;
   BooleanSetting hideAir;
   ModeSetting<ItemHudElement.Layout> layout;
   ModeSetting<ItemHudElement.Icons> icons;
   ModeSetting<ItemHudElement.HeartShape> heartShape;
   ModeSetting<ItemHudElement.FoodShape> foodShape;
   ModeSetting<ItemHudElement.ArmorShape> armorShape;
   ModeSetting<ItemHudElement.IconColor> heartColor;
   ModeSetting<ItemHudElement.IconColor> foodColor;
   ModeSetting<ItemHudElement.IconColor> armorColor;
   ModeSetting<ItemHudElement.IconColor> airColor;
   ColorSetting heartSlot;
   ColorSetting foodSlot;
   ColorSetting armorSlot;
   ColorSetting airSlot;
   BooleanSetting shake;
   BooleanSetting damageTrail;
   BooleanSetting animate;
   BooleanSetting instant;
   BooleanSetting lift;
   BooleanSetting itemName;
   BooleanSetting iconGlow;
   SliderSetting scale;
   SliderSetting barsScale;
   HudPosition position;
   HudPosition barsPosition;
   private float k = 1.0F;
   private float raised;
   private boolean detached;
   private float kb = 1.0F;
   private float barsLeft;
   private float barsBottom;
   private float barsBaseV;
   private int barsBase;
   private int rowsTall;
   private final Spring barsTall = new Spring();
   final ItemHudRenderer renderer = new ItemHudRenderer();
   private final ItemHudRenderer.Style style = new ItemHudRenderer.Style();
   final ItemHudState state = new ItemHudState();
   final HudLook hudLook = new HudLook();
   private final CostMeter cost = new CostMeter();
   private long lastFrame;
   private long frameStart;
   private ScaledResolution frameRes;
   private float dt;
   private boolean ready;
   private long spent;
   private Boolean nameHookIn;
   ItemHudPreviewWidget lastPreview;

   public ItemHudElement(UiModule module, MiniSection section) {
      super(module, section);
   }

   @Override
   public void addSettings() {
      String top = this.section.getName();
      this.own(
         new ItemHudElement.Row(
               "itemhud.preview", "Preview", "The hotbar and its bars as they look in game; the buttons try what happens to them.", new Supplier<Widget>() {
                  public Widget get() {
                     return new ItemHudPreviewWidget(ItemHudElement.this);
                  }
               }
            )
            .keywords(new String[]{"hotbar", "health", "hearts", "hunger", "food", "xp", "level", "preview"}),
         top
      );
      String st = "Style";
      this.kind = this.own(
         new ModeSetting<>(
               "itemhud.style",
               "Style",
               "Vanilla's hotbar, or a themed panel (the island's look).",
               ScoreboardElement.Kind.class,
               ScoreboardElement.Kind.THEMED
            )
            .keywords(new String[]{"hotbar", "themed"}),
         st
      );
      final BooleanSupplier themed = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ItemHudElement.this.kind.get() == ScoreboardElement.Kind.THEMED;
         }
      };
      this.opacity = this.own(
         new SliderSetting("itemhud.opacity", "Background Opacity", "How solid the panel is (100%: as the style has it).", 0.1, 1.0, 1.0, 0.05)
            .visibleWhen(themed),
         st
      );
      this.look = this.own(
         new ModeSetting<>(
               "itemhud.look", "Look", "The island's own look, or the hotbar's own corner radius, border and glass.", HudLook.Mode.class, HudLook.Mode.SAME
            )
            .visibleWhen(themed),
         st
      );
      BooleanSupplier custom = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return themed.getAsBoolean() && ItemHudElement.this.look.get() == HudLook.Mode.CUSTOM;
         }
      };
      this.radius = this.own(new SliderSetting("itemhud.radius", "Corner Radius", "How round its corners are.", 0.0, 8.0, 4.0, 0.5).visibleWhen(custom), st);
      this.border = this.own(new BooleanSetting("itemhud.border", "Border", "A thin outline.", true).visibleWhen(custom), st);
      this.glass = this.own(
         new ModeSetting<>(
               "itemhud.glass",
               "Glass",
               "Frosted glass behind it (from the frame's one shared blur).",
               WaterMarkModule.GlassMode.class,
               WaterMarkModule.GlassMode.AUTO
            )
            .visibleWhen(custom),
         st
      );
      this.glint = this.own(
         new ModeSetting<>("itemhud.glint", "Glint", "The island's glint, in sync with it.", HudLook.Glint.class, HudLook.Glint.SAME)
            .<ModeSetting>visibleWhen(themed)
            .keywords(new String[]{"shine"}),
         st
      );
      this.dividers = this.own(new BooleanSetting("itemhud.dividers", "Slot Dividers", "Subtle lines between the slots.", true).visibleWhen(themed), st);
      this.selectionGlow = this.own(
         new BooleanSetting("itemhud.selectionGlow", "Selection Glow", "A soft glow under the selected slot.", false).visibleWhen(themed), st
      );
      this.counts = this.own(
         new BooleanSetting("itemhud.counts", "Counts in the Mod Font", "Stack counts in the mod's font.", true).keywords(new String[]{"stack", "count"}), st
      );
      this.durability = this.own(
         new BooleanSetting("itemhud.durability", "Rounded Durability Bars", "Rounded durability bars, smoothly from green to red.", true)
            .keywords(new String[]{"durability", "damage"}),
         st
      );
      String hide = "Hide";
      this.hideHearts = this.own(
         new BooleanSetting("itemhud.hideHearts", "Hide Hearts", "No hearts (the rest moves down into their place).", false).keywords(new String[]{"health"}),
         hide
      );
      this.hideFood = this.own(new BooleanSetting("itemhud.hideHunger", "Hide Hunger", "No hunger.", false).keywords(new String[]{"food"}), hide);
      this.hideXp = this.own(
         new BooleanSetting("itemhud.hideXp", "Hide XP Bar", "No experience bar (the hearts drop into its place).", false).keywords(new String[]{"experience"}),
         hide
      );
      this.hideLevel = this.own(new BooleanSetting("itemhud.hideLevel", "Hide XP Level", "No level number.", false).keywords(new String[]{"experience"}), hide);
      this.hideArmor = this.own(new BooleanSetting("itemhud.hideArmor", "Hide Armor", "No armor row.", false), hide);
      this.hideAir = this.own(
         new BooleanSetting("itemhud.hideAir", "Hide Air Bubbles", "No air bubbles under water.", false).keywords(new String[]{"bubbles"}), hide
      );
      this.layout = this.own(
         new ModeSetting<>("itemhud.layout", "Layout", "Where the hearts sit.", ItemHudElement.Layout.class, ItemHudElement.Layout.VANILLA)
            .keywords(new String[]{"centered hearts"}),
         "Layout"
      );
      this.scale = this.own(
         new SliderSetting("itemhud.scale", "Size", "How big the hotbar and its bars are (Move HUD's scroll wheel sets it too).", 0.5, 2.0, 1.0, 0.05)
            .unit("×")
            .keywords(new String[]{"scale", "size", "bigger", "smaller"}),
         "Layout"
      );
      this.barsScale = this.own(
         new SliderSetting(
               "itemhud.barsScale",
               "Hearts & Hunger Size",
               "How big the hearts, hunger, armor and air are, compared with the hotbar (Move HUD's scroll wheel over them sets it too; drag them there to put them anywhere).",
               0.5,
               2.0,
               1.0,
               0.05
            )
            .unit("×")
            .keywords(new String[]{"hearts", "hunger", "health", "armor", "scale", "size", "move"}),
         "Layout"
      );
      this.position = this.own(new HudPosition("itemhud.position", "Item HUD Position"), "Layout");
      this.barsPosition = this.own(new HudPosition("itemhud.barsPosition", "Hearts & Hunger Position"), "Layout");
      String ic = "Icons";
      this.icons = this.own(
         new ModeSetting<>("itemhud.icons", "Icon Style", "Hearts, hunger, armor and air.", ItemHudElement.Icons.class, ItemHudElement.Icons.THEMED)
            .keywords(new String[]{"hearts", "bars", "icons"}),
         ic
      );
      BooleanSupplier themedIcons = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ItemHudElement.this.icons.get() == ItemHudElement.Icons.THEMED;
         }
      };
      this.heartShape = this.own(
         new ModeSetting<>("itemhud.heartShape", "Heart Shape", "The shape of the hearts.", ItemHudElement.HeartShape.class, ItemHudElement.HeartShape.HEART)
            .visibleWhen(themedIcons),
         ic
      );
      this.foodShape = this.own(
         new ModeSetting<>(
               "itemhud.foodShape", "Hunger Shape", "The shape of the hunger icons.", ItemHudElement.FoodShape.class, ItemHudElement.FoodShape.DRUMSTICK
            )
            .visibleWhen(themedIcons),
         ic
      );
      this.armorShape = this.own(
         new ModeSetting<>(
               "itemhud.armorShape", "Armor Shape", "The shape of the armor icons.", ItemHudElement.ArmorShape.class, ItemHudElement.ArmorShape.SHIELD
            )
            .visibleWhen(themedIcons),
         ic
      );
      this.iconGlow = this.own(
         new BooleanSetting("itemhud.iconGlow", "Icon Glow", "A soft glow around the icons, in their own color.", true)
            .<BooleanSetting>visibleWhen(themedIcons)
            .keywords(new String[]{"neon", "glow"}),
         ic
      );
      String co = "Icon Colors";
      BooleanSupplier notVanillaIcons = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return ItemHudElement.this.icons.get() != ItemHudElement.Icons.VANILLA;
         }
      };
      this.heartColor = this.own(
         colorChoice("itemhud.heartColor", "Hearts", "The hearts' color (poison, wither and absorption keep theirs).", notVanillaIcons), co
      );
      this.heartSlot = this.own(
         colorSlot("itemhud.heartSlot", "Hearts Color Slot", "Your own color for the hearts.", -50354, -34104, this.heartColor, notVanillaIcons), co
      );
      this.foodColor = this.own(colorChoice("itemhud.foodColor", "Hunger", "The hunger's color (the hunger effect keeps its green).", notVanillaIcons), co);
      this.foodSlot = this.own(
         colorSlot("itemhud.foodSlot", "Hunger Color Slot", "Your own color for the hunger.", -875952, -11654, this.foodColor, notVanillaIcons), co
      );
      this.armorColor = this.own(colorChoice("itemhud.armorColor", "Armor", "The armor's color.", notVanillaIcons), co);
      this.armorSlot = this.own(
         colorSlot("itemhud.armorSlot", "Armor Color Slot", "Your own color for the armor.", -2235414, -6310696, this.armorColor, notVanillaIcons), co
      );
      this.airColor = this.own(colorChoice("itemhud.airColor", "Air", "The air bubbles' color.", notVanillaIcons), co);
      this.airSlot = this.own(
         colorSlot("itemhud.airSlot", "Air Color Slot", "Your own color for the air bubbles.", -6301441, -9790209, this.airColor, notVanillaIcons), co
      );
      this.shake = this.own(new BooleanSetting("itemhud.shake", "Low-Health Shake", "At two hearts or less the hearts shake, like vanilla's.", true), ic);
      this.damageTrail = this.own(
         new BooleanSetting("itemhud.damageTrail", "Damage Trail", "Health lost stays a moment as a light trail, then drains away.", true)
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return ItemHudElement.this.icons.get() == ItemHudElement.Icons.BARS;
               }
            }),
         ic
      );
      this.xp = this.own(
         new ModeSetting<>(
               "itemhud.xp",
               "XP Bar",
               "Vanilla's, or a slim rounded bar in the accent with the level in a small pill.",
               ScoreboardElement.Kind.class,
               ScoreboardElement.Kind.THEMED
            )
            .keywords(new String[]{"experience", "level"}),
         "XP Bar"
      );
      String an = "Animations";
      this.animate = this.own(
         new BooleanSetting("itemhud.animate", "Animations", "The selection gliding, pickups popping, hearts animating, rows moving into place.", true)
            .keywords(new String[]{"animation"}),
         an
      );
      this.instant = this.own(
         new BooleanSetting("itemhud.instant", "Instant Selection", "The highlight jumps to the selected slot instead of gliding.", false), an
      );
      this.lift = this.own(new BooleanSetting("itemhud.lift", "Selected Item Grows", "The selected item a little bigger, lifted a pixel.", true), an);
      this.itemName = this.own(
         new BooleanSetting(
               "itemhud.itemName", "Item Name Pill", "The held item's name in a themed pill when you switch items (fading on vanilla's timing).", true
            )
            .keywords(new String[]{"tooltip", "item name"}),
         an
      );
   }

   private static ModeSetting<ItemHudElement.IconColor> colorChoice(String id, String name, String description, BooleanSupplier visible) {
      return new ModeSetting<>(id, name, description, ItemHudElement.IconColor.class, ItemHudElement.IconColor.CLASSIC)
         .<ModeSetting>visibleWhen(visible)
         .keywords(new String[]{"color", "colour"});
   }

   private static ColorSetting colorSlot(
      String id, String name, String description, int a, int b, final ModeSetting<ItemHudElement.IconColor> choice, final BooleanSupplier visible
   ) {
      return new ColorSetting(id, name, description, ColorSetting.Mode.STATIC, a, b, 1.0).skipSetAll().visibleWhen(new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return visible.getAsBoolean() && choice.get() == ItemHudElement.IconColor.SLOT;
         }
      });
   }

   @Override
   public void applyTheme() {
      this.kind.set(ScoreboardElement.Kind.THEMED);
      this.icons.set(ItemHudElement.Icons.THEMED);
      this.xp.set(ScoreboardElement.Kind.THEMED);
   }

   ItemHudRenderer.Style style(HudLook l) {
      ItemHudRenderer.Style s = this.style;
      s.themed = this.kind.get() == ScoreboardElement.Kind.THEMED;
      s.dividers = this.dividers.get();
      s.selectionGlow = this.selectionGlow.get();
      s.modFontCounts = this.counts.get();
      s.roundedDurability = this.durability.get();
      s.hideHearts = this.hideHearts.get();
      s.hideFood = this.hideFood.get();
      s.hideXp = this.hideXp.get();
      s.hideLevel = this.hideLevel.get();
      s.hideArmor = this.hideArmor.get();
      s.hideAir = this.hideAir.get();
      s.centered = this.layout.get() == ItemHudElement.Layout.CENTERED;
      s.icons = this.icons.get().ordinal();
      s.heartShape = this.heartShape.get().icon;
      s.foodShape = this.foodShape.get().icon;
      s.armorShape = this.armorShape.get().icon;
      int accent = l.colors.accent | 0xFF000000;
      boolean glass = l.colors.glass;
      boolean light = l.colors.light;
      fillRow(s.heartRow, this.heartColor.get(), this.heartSlot, -50354, accent);
      fillRow(s.foodRow, this.foodColor.get(), this.foodSlot, -875952, accent);
      fillRow(s.armorRow, this.armorColor.get(), this.armorSlot, light ? -7363649 : -2235414, accent);
      fillRow(s.airRow, this.airColor.get(), this.airSlot, -6301441, accent);
      s.heartColor = s.heartRow[0];
      float glow;
      if (glass) {
         s.slot = ColorUtil.withAlpha(l.colors.glassTint, Math.max(0.34F, (l.colors.glassTint >>> 24) / 255.0F * 0.9F));
         s.slotRim = ColorUtil.withAlpha(l.colors.glassRim, 0.6F);
         s.gloss = 0.6F;
         s.lighten = 0.3F;
         glow = 0.4F;
      } else {
         s.slot = ColorUtil.withAlpha(l.colors.bg, light ? 0.72F : 0.62F);
         s.slotRim = light ? 855638016 : ColorUtil.withAlpha(ColorUtil.lerp(l.colors.outline | 0xFF000000, -1, 0.5F), 0.22F);
         s.gloss = light ? 0.5F : 0.45F;
         s.lighten = light ? 0.25F : 0.28F;
         glow = light ? 0.22F : 0.42F;
      }

      s.glow = this.iconGlow.get() ? glow : 0.0F;
      s.glassEdge = glass;
      s.shake = this.shake.get();
      s.xpThemed = this.xp.get() == ScoreboardElement.Kind.THEMED;
      s.animate = this.animate.get();
      s.instant = this.instant.get();
      s.lift = this.lift.get();
      s.damageTrail = this.damageTrail.get();
      s.bgOpacity = this.opacity.getFloat();
      return s;
   }

   private static void fillRow(int[] row, ItemHudElement.IconColor choice, ColorSetting slot, int classic, int accent) {
      for (int i = 0; i < row.length; i++) {
         switch (choice) {
            case ACCENT:
               row[i] = accent;
               break;
            case SLOT:
               row[i] = slot.getColor((float)i / (row.length - 1)) | 0xFF000000;
               break;
            default:
               row[i] = classic;
         }
      }
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
      return this.kind.get() == ScoreboardElement.Kind.THEMED && this.hudLook.glass;
   }

   @Override
   public void onRenderOverlay(float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      long t0 = System.nanoTime();
      long now = ClientClock.nanos();
      this.dt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
      this.lastFrame = now;
      this.ready = false;
      if (mc.thePlayer != null && mc.theWorld != null && mc.playerController != null) {
         this.frameStart = t0;
         this.spent = 0L;
         ScaledResolution res;
         this.frameRes = res = new ScaledResolution(mc);
         this.state.read(mc, mc.ingameGUI.getUpdateCounter(), partialTicks);
         float w = res.getScaledWidth();
         float h = res.getScaledHeight();
         float sf = res.getScaleFactor();
         this.k = Math.max(0.25F, Math.min(this.scale.getFloat(), (w - 8.0F) / 182.0F));
         float uw = w / this.k;
         HudLook l = this.updateLook(this.hudLook, sf * this.k, this.dt);
         ItemHudRenderer.Style st = this.style(l);
         this.renderer.rightEdge = (int)uw / 2 + 91.0F;
         float speed = this.speed();
         this.renderer.layout(this.state, st, uw, this.dt, speed);
         boolean barsMoved = this.barsPosition.moved();
         this.kb = Math.max(0.25F, Math.min(this.k * this.barsScale.getFloat(), (w - 8.0F) / 182.0F));
         this.detached = barsMoved || this.kb != this.k;
         this.barsBase = this.renderer.leftAtHearts();
         this.barsBaseV = this.renderer.baseValue();
         this.rowsTall = Math.max(0, Math.round(this.renderer.topOfHud) - this.barsBase - 1);
         if (!(speed <= 0.0F) && this.barsTall.isStarted()) {
            this.barsTall.update(this.rowsTall, 24.0F * Math.max(0.3F, speed), this.dt);
         } else {
            this.barsTall.snap(this.rowsTall);
         }

         float tall = Math.max(0.0F, this.barsTall.value) * this.kb;
         this.raised = 0.0F;
         if (this.position.moved()) {
            float hotbar = 22.0F * this.k;
            this.raised = Math.max(0.0F, h - (this.position.y(h, hotbar) + hotbar));
            float above = barsMoved ? Math.max(22.0F, this.barsBaseV - 10.0F) * this.k : (this.barsBaseV - 9.0F) * this.k + tall;
            this.raised = Math.min(this.raised, Math.max(0.0F, h - above));
            this.raised = Math.round(this.raised * sf) / sf;
         }

         if (barsMoved) {
            this.barsLeft = this.barsPosition.x(w, 182.0F * this.kb);
            this.barsBottom = this.barsPosition.y(h, tall) + tall;
         } else {
            this.barsLeft = (int)uw / 2 * this.k - 91.0F * this.kb;
            this.barsBottom = h - this.raised - (this.barsBaseV - 9.0F) * this.k;
         }

         this.barsLeft = Math.round(this.barsLeft * sf) / sf;
         this.barsBottom = Math.round(this.barsBottom * sf) / sf;
         this.ready = true;
         this.spent = this.spent + (System.nanoTime() - t0);
      }
   }

   private boolean placed() {
      return this.k != 1.0F || this.raised != 0.0F;
   }

   private int height(int units) {
      return this.placed() ? Math.round(units * this.k + this.raised) : units;
   }

   private int rowHeight(int units) {
      if (!this.detached) {
         return this.height(units);
      } else {
         return this.barsPosition.moved()
            ? this.height(this.barsBase)
            : Math.round(this.frameRes.getScaledHeight() - this.barsBottom + (units - this.barsBaseV + 9.0F) * this.kb);
      }
   }

   public float extraHeight() {
      if (this.ready && (this.placed() || this.detached)) {
         float top = this.detached ? this.rowHeight(Math.round(this.renderer.topOfHud)) : this.renderer.topOfHud * this.k + this.raised;
         return Math.max(0.0F, top - this.renderer.topOfHud);
      } else {
         return 0.0F;
      }
   }

   @Override
   public boolean onOverlayElement(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      if (!this.ready) {
         return false;
      } else {
         switch (type) {
            case HOTBAR:
            case HEALTH:
            case ARMOR:
            case FOOD:
            case HEALTHMOUNT:
            case AIR:
            case EXPERIENCE:
            case JUMPBAR:
               if (type == RenderGameOverlayEvent.ElementType.HOTBAR && this.state.spectator) {
                  return false;
               } else {
                  long t0 = System.nanoTime();
                  ScaledResolution res = this.frameRes;
                  boolean placed = this.placed();
                  boolean own = this.detached
                     && (
                        type == RenderGameOverlayEvent.ElementType.HEALTH
                           || type == RenderGameOverlayEvent.ElementType.ARMOR
                           || type == RenderGameOverlayEvent.ElementType.FOOD
                           || type == RenderGameOverlayEvent.ElementType.HEALTHMOUNT
                           || type == RenderGameOverlayEvent.ElementType.AIR
                     );
                  float w = res.getScaledWidth() / this.k;
                  float h = (res.getScaledHeight() - this.raised) / this.k;
                  float savedPx = Ui.px;
                  float savedOffset = Ui.offsetY;
                  boolean pushed = own || placed;
                  if (own) {
                     GlStateManager.pushMatrix();
                     GlStateManager.translate(
                        this.barsLeft - (this.renderer.rightEdge - 182.0F) * this.kb, this.barsBottom - (h - this.barsBaseV + 9.0F) * this.kb, 0.0F
                     );
                     GlStateManager.scale(this.kb, this.kb, 1.0F);
                  } else if (placed) {
                     GlStateManager.pushMatrix();
                     GlStateManager.scale(this.k, this.k, 1.0F);
                  }

                  Ui.px = res.getScaleFactor() * (own ? this.kb : this.k);
                  Ui.offsetY = 0.0F;
                  Ui.beginFrameQuick(HudGl.program());
                  boolean drawn = true;

                  try {
                     ItemHudRenderer.Style st = this.style;
                     int accent = this.hudLook.colors.accent;
                     float speed = this.speed();
                     GlStateManager.enableBlend();
                     GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                     GlStateManager.disableAlpha();
                     GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                     switch (type) {
                        case HOTBAR:
                           this.renderer.hotbar(this.state, st, this.hudLook, w, h, this.dt, speed, accent);
                           if (placed || this.detached) {
                              GuiIngameForge.left_height = GuiIngameForge.right_height = this.height(this.barsBase);
                           }
                           break;
                        case HEALTH:
                           this.renderer.hearts(this.state, st, accent, h, this.dt, speed);
                           drawn = !st.hideHearts;
                           GuiIngameForge.left_height = this.rowHeight(this.renderer.leftAfterHearts);
                           if (st.centered) {
                              GuiIngameForge.right_height = this.rowHeight(this.renderer.leftAfterHearts);
                           }
                           break;
                        case ARMOR:
                           this.renderer.armor(this.state, st, h, 1.0F);
                           drawn = !st.hideArmor;
                           GuiIngameForge.left_height = this.rowHeight(this.renderer.leftAfterArmor);
                           if (this.renderer.centeredArmor) {
                              GuiIngameForge.right_height = this.rowHeight(this.renderer.leftAfterArmor);
                           }
                           break;
                        case FOOD:
                           this.renderer.food(this.state, st, h, 1.0F);
                           drawn = !st.hideFood;
                           GuiIngameForge.right_height = this.rowHeight(this.renderer.rightAfterFood);
                           break;
                        case HEALTHMOUNT:
                           this.renderer.mount(this.state, st, h, 1.0F);
                           GuiIngameForge.right_height = this.rowHeight(this.renderer.rightAfterFood);
                           break;
                        case AIR:
                           this.renderer.air(this.state, st, h, 1.0F);
                           drawn = !st.hideAir;
                           GuiIngameForge.right_height = this.rowHeight(this.renderer.rightAfterAir);
                           break;
                        default:
                           this.renderer.xp(this.state, st, this.hudLook, accent, w, h, this.dt, speed);
                           drawn = !st.hideXp || !st.hideLevel || this.state.horse;
                     }
                  } finally {
                     Ui.endFrame();
                     if (pushed) {
                        GlStateManager.popMatrix();
                     }

                     Ui.px = savedPx;
                     Ui.offsetY = savedOffset;
                     GlStateManager.enableTexture2D();
                     GlStateManager.enableAlpha();
                     GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                     if (type != RenderGameOverlayEvent.ElementType.EXPERIENCE && type != RenderGameOverlayEvent.ElementType.JUMPBAR) {
                        GlStateManager.disableBlend();
                     } else {
                        GlStateManager.enableBlend();
                     }

                     this.spent = this.spent + (System.nanoTime() - t0);
                  }

                  if (drawn) {
                     HudHooks.elementPost(type, partialTicks);
                  }

                  return true;
               }
            default:
               return false;
         }
      }
   }

   @Override
   public void onRenderHud(float partialTicks) {
      if (this.spent > 0L) {
         this.cost.add(this.spent);
         this.spent = 0L;
      }
   }

   private boolean nameHookIn() {
      if (this.nameHookIn == null) {
         this.nameHookIn = UiHooks.installed("itemName", GuiIngameForge.class);
      }

      return this.nameHookIn;
   }

   public boolean itemName(ScaledResolution res) {
      Minecraft mc = Minecraft.getMinecraft();
      if (!this.nameHookIn() || mc.playerController.isSpectator() || !mc.gameSettings.heldItemTooltips) {
         return false;
      } else if (this.itemName.get()) {
         AccessorGuiIngame acc = AccessorGuiIngame.of(mc.ingameGUI);
         int ticks = acc.getFvRemainingHighlightTicks();
         ItemStack stack = acc.getFvHighlightingItemStack();
         if (ticks > 0 && stack != null) {
            String name = stack.getDisplayName();
            if (stack.hasDisplayName()) {
               name = EnumChatFormatting.ITALIC + name;
            }

            float alpha = Math.min(1.0F, Math.max(0.0F, (ticks - this.state.partial) * 256.0F / 10.0F / 255.0F));
            float savedPx = Ui.px;
            float savedOffset = Ui.offsetY;
            Ui.px = res.getScaleFactor();
            Ui.offsetY = 0.0F;
            Ui.beginFrameQuick(HudGl.program());

            try {
               int top = Math.max(GuiIngameForge.left_height, GuiIngameForge.right_height);
               float floor = (mc.playerController.shouldDrawHUD() ? 39.0F : 25.0F) * this.k + this.raised;
               float bottom = res.getScaledHeight() - Math.max((float)top, floor) - 2.0F;
               ItemHudRenderer.itemName(name, this.hudLook, res.getScaledWidth(), bottom, alpha);
            } finally {
               Ui.endFrame();
               Ui.px = savedPx;
               Ui.offsetY = savedOffset;
               GlStateManager.enableTexture2D();
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }

            return true;
         } else {
            return true;
         }
      } else {
         return this.extraHeight() > 0.0F && this.vanillaName(mc, res);
      }
   }

   private boolean vanillaName(Minecraft mc, ScaledResolution res) {
      AccessorGuiIngame acc = AccessorGuiIngame.of(mc.ingameGUI);
      int ticks = acc.getFvRemainingHighlightTicks();
      ItemStack stack = acc.getFvHighlightingItemStack();
      if (ticks > 0 && stack != null) {
         String name = stack.getDisplayName();
         if (stack.hasDisplayName()) {
            name = EnumChatFormatting.ITALIC + name;
         }

         int opacity = Math.min(255, (int)(ticks * 256.0F / 10.0F));
         if (opacity <= 0) {
            return true;
         } else {
            int y = res.getScaledHeight() - 59 - Math.round(this.extraHeight());
            if (!mc.playerController.shouldDrawHUD()) {
               y += 14;
            }

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            FontRenderer font = mc.fontRendererObj;
            font.drawStringWithShadow(name, (res.getScaledWidth() - font.getStringWidth(name)) / 2, y, 16777215 | opacity << 24);
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
            return true;
         }
      } else {
         return true;
      }
   }

   public float[] boundsGui() {
      if (this.ready && this.frameRes != null && !this.state.spectator) {
         float w = this.frameRes.getScaledWidth();
         float h = this.frameRes.getScaledHeight();
         float x = ((int)(w / this.k) / 2 - 91.0F) * this.k;
         float tall = Math.max(22.0F, this.barsBaseV - 10.0F) * this.k;
         return new float[]{x, h - this.raised - tall, 182.0F * this.k, tall};
      } else {
         return null;
      }
   }

   public void placeGui(float top, float tall, float sw, float sh) {
      float hotbar = 22.0F * this.k;
      this.position.place((sw - 182.0F * this.k) / 2.0F, top + tall - hotbar, 182.0F * this.k, hotbar, sw, sh);
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
   }

   public float[] barsBoundsGui() {
      if (this.ready && this.frameRes != null && this.barsMissing() == null && !(this.barsTall.value < 1.0F)) {
         float tall = this.barsTall.value * this.kb;
         return new float[]{this.barsLeft, this.barsBottom - tall, 182.0F * this.kb, tall};
      } else {
         return null;
      }
   }

   public String barsMissing() {
      if (this.state.spectator) {
         return "vanilla while spectating";
      } else if (!this.state.survival) {
         return "shown in Survival";
      } else {
         return this.rowsTall <= 0 ? "all hidden" : null;
      }
   }

   public void placeBarsGui(float x, float y, float w, float h, float sw, float sh) {
      this.barsPosition.place(x, y, w, h, sw, sh);
   }

   public boolean barsOnHotbar() {
      return !this.barsPosition.moved();
   }

   public SliderSetting barsScaleSetting() {
      return this.barsScale;
   }

   public HudPosition barsPositionSetting() {
      return this.barsPosition;
   }

   public void resetBarsPlace() {
      this.barsPosition.reset();
      this.barsScale.reset();
   }

   @Override
   public void onWorldChange() {
      this.renderer.reset();
   }

   public void previewTestForTest(int button) {
      if (this.lastPreview != null) {
         this.lastPreview.test(button);
      }
   }

   public String stateForTest() {
      String s = "item name hook "
         + (this.nameHookIn() ? "in" : "MISSING")
         + ", left_height after hearts "
         + this.renderer.leftAfterHearts
         + ", after armor "
         + this.renderer.leftAfterArmor
         + ", right_height after food "
         + this.renderer.rightAfterFood
         + ", after air "
         + this.renderer.rightAfterAir
         + ", hearts & hunger "
         + (this.barsPosition.moved() ? "moved" : "on the hotbar")
         + (
            this.detached
               ? String.format(
                  Locale.ROOT,
                  " (own: x %.1f, bottom %.1f, size %.2f, Forge's row heights %d / %d)",
                  this.barsLeft,
                  this.barsBottom,
                  this.kb,
                  this.rowHeight(this.renderer.leftAfterHearts),
                  this.rowHeight(this.renderer.rightAfterFood)
               )
               : ""
         )
         + ", selection at "
         + String.format("%.2f", this.renderer.selectedAt)
         + ", cost "
         + this.cost.describe();
      this.cost.reset();
      return s;
   }

   public static String forgeHeightsForTest() {
      return GuiIngameForge.left_height + " / " + GuiIngameForge.right_height;
   }

   public static enum ArmorShape implements Displayable {
      SHIELD("Shield", "A shield", HudIcons.Icon.ARMOR_SHIELD),
      CHESTPLATE("Chestplate", "A chestplate", HudIcons.Icon.ARMOR_CHEST);

      private final String displayName;
      private final String description;
      final HudIcons.Icon icon;

      private ArmorShape(String displayName, String description, HudIcons.Icon icon) {
         this.displayName = displayName;
         this.description = description;
         this.icon = icon;
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

   public static enum FoodShape implements Displayable {
      DRUMSTICK("Drumstick", "A drumstick", HudIcons.Icon.FOOD_DRUMSTICK),
      BOLT("Energy Bolt", "A lightning bolt", HudIcons.Icon.FOOD_BOLT),
      CIRCLE("Circle", "A circle", HudIcons.Icon.FOOD_CIRCLE);

      private final String displayName;
      private final String description;
      final HudIcons.Icon icon;

      private FoodShape(String displayName, String description, HudIcons.Icon icon) {
         this.displayName = displayName;
         this.description = description;
         this.icon = icon;
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

   public static enum HeartShape implements Displayable {
      HEART("Heart", "A heart", HudIcons.Icon.HEART),
      SQUARE("Rounded Square", "A rounded square", HudIcons.Icon.HEART_SQUARE),
      HEXAGON("Hexagon", "A hexagon", HudIcons.Icon.HEART_HEX),
      CIRCLE("Circle", "A circle", HudIcons.Icon.HEART_CIRCLE);

      private final String displayName;
      private final String description;
      final HudIcons.Icon icon;

      private HeartShape(String displayName, String description, HudIcons.Icon icon) {
         this.displayName = displayName;
         this.description = description;
         this.icon = icon;
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

   public static enum IconColor implements Displayable {
      CLASSIC("Classic", "The usual color: red hearts, golden hunger, silver armor, blue air"),
      ACCENT("Accent", "The theme's accent color (the island's)"),
      SLOT("Color Slot", "A color of your own; a gradient runs along the row");

      private final String displayName;
      private final String description;

      private IconColor(String displayName, String description) {
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

   public static enum Icons implements Displayable {
      VANILLA("Vanilla", "Your resource pack's icons"),
      THEMED("Themed Icons", "Crisp vector icons, drawn at your screen's pixels"),
      BARS("Bars", "Health as a rounded bar with its number, hunger as a bar, armor and air as thin bars");

      private final String displayName;
      private final String description;

      private Icons(String displayName, String description) {
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

   public static enum Layout implements Displayable {
      VANILLA("Vanilla", "Hearts on the left, hunger on the right"),
      CENTERED("Centered Hearts", "The hearts in the middle; armor and hunger a row above, left and right");

      private final String displayName;
      private final String description;

      private Layout(String displayName, String description) {
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
}
