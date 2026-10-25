package meowtils.furiahvisuals.module.ui.itemhud;

import java.util.Arrays;
import java.util.Random;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.hud.HudFont;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.hud.HudPanel;
import meowtils.furiahvisuals.module.watermark.Spring;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

final class ItemHudRenderer {
   static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
   static final int HEART_RED = -50354;
   static final int FOOD_ORANGE = -875952;
   static final int ARMOR_SILVER = -2235414;
   static final int ARMOR_STEEL = -7363649;
   static final int AIR_BLUE = -6301441;
   static final int BONE_TOP = -265242;
   static final int BONE = -1517116;
   static final int POISON = -8535477;
   static final int WITHER = -11908526;
   static final int ABSORPTION = -10934;
   static final int HUNGER_EFFECT = -7617718;
   static final int MOUNT = -30116;
   private final Spring base = new Spring();
   private final Spring heartsX = new Spring();
   private final Spring heartsH = new Spring();
   private final Spring armorX = new Spring();
   private final Spring armorH = new Spring();
   private final Spring foodH = new Spring();
   private final Spring airH = new Spring();
   private float heartsA = 1.0F;
   private float armorA = 1.0F;
   private float foodA = 1.0F;
   private float airA = 1.0F;
   private float xpA = 1.0F;
   int leftAfterHearts;
   int leftAfterArmor;
   int rightAfterFood;
   int rightAfterAir;
   boolean centeredArmor;
   private final Spring selection = new Spring();
   private int lastSelected = -1;
   private final float[] pop = new float[]{1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F};
   private final int[] lastCount = new int[9];
   private final int[] lastAnim = new int[9];
   float selectedAt;
   private final float[] heartPop = new float[64];
   private final float[] heartGhost = new float[64];
   private final int[] heartFill = new int[64];
   private final int[] ghostFill = new int[64];
   private final Random rand = new Random();
   private int playerHealth;
   private int lastPlayerHealth;
   private long lastSystemTime;
   private long healthUpdateCounter;
   private float ghostHealth = -1.0F;
   private float ghostHold;
   private float xpShown = -1.0F;
   private int lastLevel = -1;
   private final float[] sparkX = new float[16];
   private final float[] sparkT = new float[16];
   float topOfHud;
   private final float[] liftOf = new float[9];
   float itemTop;
   float rightEdge;

   static int heartsHeight(ItemHudState s) {
      int rows = MathHelper.ceiling_float_int((s.maxHealth + s.absorption) / 2.0F / 10.0F);
      int rowH = Math.max(10 - (rows - 2), 3);
      int h = rows * rowH;
      if (rowH != 10) {
         h += 10 - rowH;
      }

      return h;
   }

   private static int mountRows(ItemHudState s) {
      int hearts = Math.min(30, (int)(s.mountMax + 0.5F) / 2);
      return Math.max(1, (hearts + 9) / 10);
   }

   void layout(ItemHudState s, ItemHudRenderer.Style st, float w, float dt, float speed) {
      boolean instant = speed <= 0.0F;
      float omega = 24.0F * Math.max(0.3F, speed);
      boolean heartsOn = s.survival && !st.hideHearts;
      boolean armorOn = s.survival && !st.hideArmor;
      boolean foodOn = s.survival && (s.mounted || !st.hideFood);
      boolean airOn = s.survival && s.underwater && !st.hideAir;
      boolean bars = st.icons == 2;
      float b = st.hideXp && !s.horse ? 32.0F : 39.0F;
      float hearts = heartsOn ? (bars ? 10.0F : heartsHeight(s)) : 0.0F;
      float food = foodOn ? (s.mounted ? (bars ? 10.0F : mountRows(s) * 10.0F) : 10.0F) : 0.0F;
      float armorRow = armorOn ? 10.0F : 0.0F;
      float mid = (int)w / 2;
      float rH;
      float fH;
      float aH;
      float ax;
      float hx;
      if (!st.centered) {
         ax = hx = mid - 91.0F;
         aH = b + hearts;
         fH = b;
         rH = b + food;
         this.centeredArmor = false;
      } else {
         hx = mid - 40.0F;
         float top = b + hearts;
         if (!foodOn) {
            ax = hx;
            this.centeredArmor = true;
            aH = top;
            fH = top + armorRow;
            rH = top + armorRow;
         } else {
            ax = mid - 91.0F;
            this.centeredArmor = false;
            aH = top;
            fH = top;
            rH = top + food;
         }
      }

      step(this.base, b, omega, dt, instant);
      step(this.heartsX, hx, omega, dt, instant);
      step(this.heartsH, hearts, omega, dt, instant);
      step(this.armorX, ax, omega, dt, instant);
      step(this.armorH, aH, omega, dt, instant);
      step(this.foodH, fH, omega, dt, instant);
      step(this.airH, rH, omega, dt, instant);
      float fade = instant ? 1000.0F : 12.0F * speed;
      this.heartsA = Easing.approach(this.heartsA, heartsOn ? 1.0F : 0.0F, fade, dt);
      this.armorA = Easing.approach(this.armorA, armorOn ? 1.0F : 0.0F, fade, dt);
      this.foodA = Easing.approach(this.foodA, foodOn ? 1.0F : 0.0F, fade, dt);
      this.airA = Easing.approach(this.airA, airOn ? 1.0F : 0.0F, fade, dt);
      this.xpA = Easing.approach(this.xpA, st.hideXp ? 0.0F : 1.0F, fade, dt);
      this.leftAfterHearts = Math.round(this.base.value + this.heartsH.value);
      this.leftAfterArmor = Math.round(this.armorH.value) + (armorOn ? 10 : 0);
      this.rightAfterFood = Math.round(this.foodH.value) + Math.round(food);
      this.rightAfterAir = Math.round(this.airH.value) + (airOn ? 10 : 0);
      this.topOfHud = Math.max(this.leftAfterArmor, this.rightAfterAir);
   }

   private static void step(Spring sp, float target, float omega, float dt, boolean instant) {
      if (!instant && sp.isStarted()) {
         sp.update(target, omega, dt);
      } else {
         sp.snap(target);
      }
   }

   int leftAtHearts() {
      return Math.round(this.base.value);
   }

   float baseValue() {
      return this.base.value;
   }

   int armorTop() {
      return Math.round(this.armorH.value);
   }

   int foodTop() {
      return Math.round(this.foodH.value);
   }

   int airTop() {
      return Math.round(this.airH.value);
   }

   void hotbar(ItemHudState s, ItemHudRenderer.Style st, HudLook look, float w, float h, float dt, float speed, int accent) {
      float x = (int)w / 2 - 91.0F;
      float y = h - 22.0F;
      this.itemTop = h - 19.0F;
      if (this.lastSelected >= 0 && !st.instant && !(speed <= 0.0F)) {
         float target = s.selected + 9.0F * Math.round((this.selection.value - s.selected) / 9.0F);
         this.selection.update(target, 55.0F * Math.max(0.5F, speed), dt);
         if (this.selection.isSettled(target)) {
            this.selection.snap(s.selected);
         }
      } else {
         this.selection.snap(s.selected);
      }

      this.lastSelected = s.selected;
      float sel;
      this.selectedAt = sel = (this.selection.value % 9.0F + 9.0F) % 9.0F;
      float r = Math.min(look.radius, 6.0F);
      if (st.themed) {
         HudPanel.background(x, y, 182.0F, 22.0F, r, look, st.bgOpacity);
         int text = look.colors.text | 0xFF000000;
         if (st.dividers) {
            dividers(x, y, ColorUtil.mulAlpha(text, 0.1F));
         }

         this.selectionBox(x, y, sel, accent, st);
         if (sel > 8.0F) {
            this.selectionBox(x, y, sel - 9.0F, accent, st);
         }

         HudPanel.foreground(x, y, 182.0F, 22.0F, r, look, st.bgOpacity);
      } else {
         Minecraft.getMinecraft().getTextureManager().bindTexture(WIDGETS);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         texture(x, y, 0, 0, 182, 22, -90.0F);
         if (sel > 8.0F) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 9.0F - sel);
            texture(x - 1.0F + 160.0F, y - 1.0F, 0, 22, 24, 22, -90.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, sel - 8.0F);
            texture(x - 1.0F, y - 1.0F, 0, 22, 24, 22, -90.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         } else {
            texture(x - 1.0F + sel * 20.0F, y - 1.0F, 0, 22, 24, 22, -90.0F);
         }
      }

      this.items(s, st, x, dt, speed);
   }

   private static void dividers(float x, float y, int c) {
      float lineW = 1.0F / Ui.px;
      GlStateManager.disableTexture2D();
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
      int r = c >> 16 & 0xFF;
      int g = c >> 8 & 0xFF;
      int b = c & 0xFF;
      int a = c >>> 24;

      for (int i = 1; i < 9; i++) {
         float lx = Math.round((x + 1.0F + 20.0F * i) * Ui.px) / Ui.px - lineW / 2.0F;
         wr.pos(lx, y + 17.0F, 0.0).color(r, g, b, a).endVertex();
         wr.pos(lx + lineW, y + 17.0F, 0.0).color(r, g, b, a).endVertex();
         wr.pos(lx + lineW, y + 5.0F, 0.0).color(r, g, b, a).endVertex();
         wr.pos(lx, y + 5.0F, 0.0).color(r, g, b, a).endVertex();
      }

      tess.draw();
      GlStateManager.enableTexture2D();
   }

   private void selectionBox(float x, float y, float sel, int accent, ItemHudRenderer.Style st) {
      float bx = x + 1.0F + 20.0F * sel;
      float by = y + 1.0F;
      if (!(bx < x - 10.0F) && !(bx > x + 182.0F)) {
         float vis = Easing.clamp01(1.0F - Math.max(x - bx, bx + 20.0F - (x + 182.0F)) / 20.0F);
         int a = accent | 0xFF000000;
         if (st.selectionGlow) {
            Ui.glow(bx + 10.0F, by + 10.0F, 30.0F, ColorUtil.mulAlpha(a, 0.35F * vis));
         }

         Ui.roundRect(bx, by, 20.0F, 20.0F, 4.0F, ColorUtil.mulAlpha(a, 0.22F * vis));
         Ui.outline(bx, by, 20.0F, 20.0F, 4.0F, Math.max(1.0F, (float)Math.round(Ui.px * 0.5F)) / Ui.px, ColorUtil.mulAlpha(a, 0.85F * vis));
      }
   }

   private void items(ItemHudState s, ItemHudRenderer.Style st, float x, float dt, float speed) {
      Minecraft mc = Minecraft.getMinecraft();
      RenderItem ri = mc.getRenderItem();
      GlStateManager.enableRescaleNormal();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      RenderHelper.enableGUIStandardItemLighting();
      float sel = this.selectedAt;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = s.slots[i];
         int count = stack == null ? 0 : stack.stackSize;
         int anim = stack == null ? 0 : stack.animationsToGo;
         if (stack != null && (anim > this.lastAnim[i] || count > this.lastCount[i] && this.lastCount[i] > 0)) {
            this.pop[i] = 0.0F;
         }

         this.lastCount[i] = count;
         this.lastAnim[i] = anim;
         float f = this.pop[i] = speed <= 0.0F ? 1.0F : Math.min(1.0F, this.pop[i] + dt * speed / 0.28F);
         if (stack != null) {
            float itemX = x + 3.0F + 20.0F * i;
            float d = Math.abs(sel - i);
            d = Math.min(d, 9.0F - d);
            float near = st.lift ? Math.max(0.0F, 1.0F - d) : 0.0F;
            float popNow = this.pop[i] >= 1.0F ? 0.0F : (float)Math.sin(Math.PI * this.pop[i]) * 0.18F;
            float scale = 1.0F + 0.1F * near + popNow;
            int baseY = Math.round(this.itemTop);
            boolean moved = scale != 1.0F || near != 0.0F;
            if (moved) {
               GlStateManager.pushMatrix();
               GlStateManager.translate(itemX + 8.0F, baseY + 8.0F - near, 0.0F);
               GlStateManager.scale(scale, scale, 1.0F);
               GlStateManager.translate(-(itemX + 8.0F), -(baseY + 8.0F), 0.0F);
            }

            ri.renderItemAndEffectIntoGUI(stack, Math.round(itemX), baseY);
            if (moved) {
               GlStateManager.popMatrix();
            }

            this.liftOf[i] = near;
         }
      }

      RenderHelper.disableStandardItemLighting();
      GlStateManager.disableRescaleNormal();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      boolean batch = st.modFontCounts;
      if (batch) {
         HudFont.beginBatch();
      }

      for (int i = 0; i < 9; i++) {
         ItemStack stackx = s.slots[i];
         if (stackx != null) {
            this.overlay(st, ri, mc.fontRendererObj, stackx, Math.round(x + 3.0F + 20.0F * i), Math.round(this.itemTop) - this.liftOf[i]);
         }
      }

      if (batch) {
         HudFont.endBatch();
      }

      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
   }

   private void overlay(ItemHudRenderer.Style st, RenderItem ri, FontRenderer fr, ItemStack stack, float x, float y) {
      if (!st.modFontCounts && !st.roundedDurability) {
         ri.renderItemOverlays(fr, stack, (int)x, (int)y);
      } else {
         GlStateManager.disableLighting();
         GlStateManager.disableDepth();
         GlStateManager.enableBlend();
         if (st.roundedDurability && stack.isItemDamaged()) {
            double used = (double)stack.getItemDamage() / stack.getMaxDamage();
            float left = (float)Math.max(0.0, Math.min(1.0, 1.0 - used));
            Ui.roundRect(x + 2.0F, y + 13.0F, 13.0F, 2.0F, 1.0F, -871362544);
            int c = ColorUtil.hsv(left * 0.33F, 0.85F, 1.0F) | 0xFF000000;
            Ui.roundRect(x + 2.0F, y + 13.0F, Math.max(1.2F, 13.0F * left), 2.0F, 1.0F, c);
         } else if (!st.roundedDurability && stack.isItemDamaged()) {
            ri.renderItemOverlayIntoGUI(fr, stack, (int)x, (int)y, "");
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
         }

         if (stack.stackSize != 1) {
            String n = stack.stackSize < 1 ? "§c" + stack.stackSize : String.valueOf(stack.stackSize);
            if (st.modFontCounts) {
               float tw = HudFont.width(n);
               HudFont.draw(n, x + 17.0F - tw, y + 9.0F, -1, true);
            } else {
               fr.drawStringWithShadow(n, x + 19.0F - 2.0F - fr.getStringWidth(n), y + 6.0F + 3.0F, 16777215);
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               GlStateManager.enableBlend();
            }
         }
      }
   }

   private static FontRenderer font() {
      return Minecraft.getMinecraft().fontRendererObj;
   }

   private boolean flash(ItemHudState s, int health) {
      boolean highlight = this.healthUpdateCounter > s.ticks && (this.healthUpdateCounter - s.ticks) / 3L % 2L == 1L;
      long now = ClientClock.millis();
      if (health < this.playerHealth && s.hurtTime) {
         this.lastSystemTime = now;
         this.healthUpdateCounter = s.ticks + 20;
      } else if (health > this.playerHealth && s.hurtTime) {
         this.lastSystemTime = now;
         this.healthUpdateCounter = s.ticks + 10;
      }

      if (now - this.lastSystemTime > 1000L) {
         this.playerHealth = health;
         this.lastPlayerHealth = health;
         this.lastSystemTime = now;
      }

      this.playerHealth = health;
      return highlight;
   }

   void hearts(ItemHudState s, ItemHudRenderer.Style st, int accent, float h, float dt, float speed) {
      if (!(this.heartsA <= 0.01F)) {
         int health = MathHelper.ceiling_float_int(s.health);
         boolean highlight = this.flash(s, health);
         int healthLast = this.lastPlayerHealth;
         float top = h - this.base.value;
         float left = this.heartsX.value;
         float alpha = Easing.outCubic(this.heartsA);
         if (st.icons == 2) {
            this.healthBar(s, st, health, left, top, alpha, highlight, dt, speed);
         } else {
            float max = s.maxHealth;
            float absorb = s.absorption;
            int slots = MathHelper.ceiling_float_int((max + absorb) / 2.0F);
            int rows = MathHelper.ceiling_float_int((max + absorb) / 2.0F / 10.0F);
            int rowH = Math.max(10 - (rows - 2), 3);
            this.rand.setSeed(s.ticks * 312871L);
            float regenAt = s.regen ? s.ticks % 25 + s.partial : -10.0F;
            float absorbRemaining = absorb;
            boolean themed = st.icons == 1 && HudIcons.ready(Math.round(9.0F * Ui.px));
            int status = s.poison ? -8535477 : (s.wither ? -11908526 : 0);
            WorldRenderer wr = begin(themed);

            for (int i = slots - 1; i >= 0; i--) {
               int row = MathHelper.ceiling_float_int((i + 1) / 10.0F) - 1;
               float x = left + i % 10 * 8;
               float y = top - row * rowH;
               if (health <= 4 && st.shake) {
                  y += this.rand.nextInt(2);
               }

               float d;
               if ((d = Math.abs(i - regenAt)) < 1.0F) {
                  y -= 2.0F * (1.0F - d);
               }

               boolean gold = false;
               int fill;
               if (absorbRemaining > 0.0F) {
                  gold = true;
                  fill = absorbRemaining == absorb && absorb % 2.0F == 1.0F ? 1 : 2;
                  absorbRemaining -= 2.0F;
               } else {
                  fill = i * 2 + 1 < health ? 2 : (i * 2 + 1 == health ? 1 : 0);
               }

               if (i < this.heartFill.length) {
                  if (fill < this.heartFill[i] && speed > 0.0F) {
                     this.ghostFill[i] = this.heartFill[i];
                     this.heartGhost[i] = 0.0F;
                  } else if (fill > this.heartFill[i] && speed > 0.0F) {
                     this.heartPop[i] = 0.0F;
                  }

                  this.heartFill[i] = fill;
                  this.heartGhost[i] = Math.min(1.0F, this.heartGhost[i] + dt * speed / 0.3F);
                  this.heartPop[i] = Math.min(1.0F, this.heartPop[i] + dt * speed / 0.28F);
               }

               if (themed) {
                  HudIcons.Icon shape = st.heartShape;
                  int fillColor = status != 0 ? status : st.heartRow[i % 10];
                  slot(wr, st, shape, x, y, 9.0F, alpha, highlight);
                  if (s.hardcore) {
                     HudIcons.quad(wr, shape, HudIcons.Layer.RIM, x - 0.6F, y - 0.6F, 10.2F, 0.0F, 1.0F, ColorUtil.mulAlpha(-1, alpha * 0.6F));
                  }

                  if (highlight && i * 2 + 1 <= healthLast && fill < 2) {
                     float to = i * 2 + 1 < healthLast ? 1.0F : 0.5F;
                     HudIcons.quad(wr, shape, HudIcons.Layer.FILL, x, y, 9.0F, 0.0F, to, ColorUtil.mulAlpha(-1, alpha * 0.75F));
                  }

                  if (i < this.heartGhost.length && this.heartGhost[i] < 1.0F && this.ghostFill[i] > fill) {
                     float g = this.heartGhost[i];
                     float sz = 9.0F * (1.0F - 0.35F * g);
                     float to = this.ghostFill[i] == 2 ? 1.0F : 0.5F;
                     filled(wr, st, shape, x + (9.0F - sz) / 2.0F, y + (9.0F - sz) / 2.0F, sz, 0.0F, to, fillColor, alpha * (1.0F - g));
                  }

                  if (fill > 0) {
                     float pp = i < this.heartPop.length && this.heartPop[i] < 1.0F ? 1.0F + 0.35F * (1.0F - Easing.outBack(this.heartPop[i], 2.0F)) : 1.0F;
                     float sz = 9.0F * pp;
                     filled(wr, st, shape, x + (9.0F - sz) / 2.0F, y + (9.0F - sz) / 2.0F, sz, 0.0F, fill == 2 ? 1.0F : 0.5F, gold ? -10934 : fillColor, alpha);
                  }
               } else {
                  this.vanillaHeart(s, highlight, healthLast, health, i, fill, gold, absorbRemaining, x, y, alpha);
               }
            }

            end(themed);
         }
      }
   }

   private static void slot(WorldRenderer wr, ItemHudRenderer.Style st, HudIcons.Icon icon, float x, float y, float size, float alpha, boolean flash) {
      HudIcons.quad(wr, icon, HudIcons.Layer.FILL, x, y, size, 0.0F, 1.0F, ColorUtil.mulAlpha(flash ? -637534209 : st.slot, alpha));
      HudIcons.quad(wr, icon, HudIcons.Layer.RIM, x, y, size, 0.0F, 1.0F, ColorUtil.mulAlpha(flash ? -1 : st.slotRim, alpha));
   }

   static void filled(
      WorldRenderer wr, ItemHudRenderer.Style st, HudIcons.Icon icon, float x, float y, float size, float from, float to, int color, float alpha
   ) {
      int c = color | 0xFF000000;
      if (st.glow > 0.0F) {
         HudIcons.quad(wr, icon, HudIcons.Layer.GLOW, x, y, size, from, to, ColorUtil.mulAlpha(c, alpha * st.glow));
      }

      int top = ColorUtil.lerp(c, -1, st.lighten);
      HudIcons.quad(wr, icon, HudIcons.Layer.FILL, x, y, size, from, to, ColorUtil.mulAlpha(top, alpha), ColorUtil.mulAlpha(shade(c, 0.8F), alpha));
      if (HudIcons.hasDetail(icon)) {
         boolean bone = icon == HudIcons.Icon.FOOD_DRUMSTICK;
         int dt = bone ? -265242 : ColorUtil.withAlpha(shade(c, 0.7F), 0.8F);
         int db = bone ? -1517116 : ColorUtil.withAlpha(shade(c, 0.62F), 0.8F);
         HudIcons.quad(wr, icon, HudIcons.Layer.DETAIL, x, y, size, from, to, ColorUtil.mulAlpha(dt, alpha), ColorUtil.mulAlpha(db, alpha));
      }

      if (st.gloss > 0.0F) {
         HudIcons.quad(wr, icon, HudIcons.Layer.GLOSS, x, y, size, from, to, ColorUtil.mulAlpha(-1, alpha * st.gloss));
      }

      int rim = st.glassEdge ? -1275068417 : ColorUtil.withAlpha(ColorUtil.lerp(c, -1, 0.65F), 0.75F);
      HudIcons.quad(wr, icon, HudIcons.Layer.RIM, x, y, size, from, to, ColorUtil.mulAlpha(rim, alpha));
   }

   private static int shade(int c, float k) {
      int r = Math.round((c >> 16 & 0xFF) * k);
      int g = Math.round((c >> 8 & 0xFF) * k);
      int b = Math.round((c & 0xFF) * k);
      return c & 0xFF000000 | r << 16 | g << 8 | b;
   }

   private void vanillaHeart(
      ItemHudState s, boolean highlight, int healthLast, int health, int i, int fill, boolean gold, float absorbLeft, float x, float y, float alpha
   ) {
      int topV = 9 * (s.hardcore ? 5 : 0);
      int margin = 16 + (s.poison ? 36 : (s.wither ? 72 : 0));
      GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
      texture(x, y, highlight ? 25 : 16, topV, 9, 9, -90.0F);
      if (highlight) {
         if (i * 2 + 1 < healthLast) {
            texture(x, y, margin + 54, topV, 9, 9, -90.0F);
         } else if (i * 2 + 1 == healthLast) {
            texture(x, y, margin + 63, topV, 9, 9, -90.0F);
         }
      }

      if (gold) {
         texture(x, y, fill == 1 ? margin + 153 : margin + 144, topV, 9, 9, -90.0F);
      } else if (fill == 2) {
         texture(x, y, margin + 36, topV, 9, 9, -90.0F);
      } else if (fill == 1) {
         texture(x, y, margin + 45, topV, 9, 9, -90.0F);
      }
   }

   private void healthBar(ItemHudState s, ItemHudRenderer.Style st, int health, float left, float top, float alpha, boolean highlight, float dt, float speed) {
      float cap = Math.max(1.0F, s.maxHealth + s.absorption);
      float w = 81.0F;
      float bh = 7.0F;
      float y = top + 1.0F;
      if (this.ghostHealth < 0.0F || this.ghostHealth < s.health) {
         this.ghostHealth = s.health;
         this.ghostHold = 0.0F;
      } else if (this.ghostHealth > s.health) {
         this.ghostHold += dt;
         if (this.ghostHold > 0.4F || !st.damageTrail || speed <= 0.0F) {
            this.ghostHealth = Math.max(s.health, this.ghostHealth - dt * Math.max(4.0F, (this.ghostHealth - s.health) * 3.0F) * Math.max(0.5F, speed));
         }
      }

      int status = s.poison ? -8535477 : (s.wither ? -11908526 : 0);
      Ui.roundRect(left, y, w, bh, bh / 2.0F, ColorUtil.mulAlpha(st.slot, alpha));
      if (st.damageTrail && this.ghostHealth > s.health) {
         Ui.roundRect(left, y, w * this.ghostHealth / cap, bh, bh / 2.0F, ColorUtil.mulAlpha(-855638017, alpha * 0.55F));
      }

      float hw;
      if ((hw = w * Math.max(0.0F, s.health) / cap) > 0.5F) {
         int c0 = status != 0 ? status : st.heartRow[0];
         int c1 = status != 0 ? status : st.heartRow[9];
         Ui.gradientH(left, y, hw, bh, bh / 2.0F, ColorUtil.mulAlpha(c0 | 0xFF000000, alpha), ColorUtil.mulAlpha(c1 | 0xFF000000, alpha));
         Ui.roundRect(left, y, hw, bh / 2.0F, bh / 4.0F, ColorUtil.mulAlpha(-1, alpha * st.gloss * 0.5F));
      }

      if (s.absorption > 0.0F) {
         Ui.roundRect(left + hw, y, w * s.absorption / cap, bh, bh / 2.0F, ColorUtil.mulAlpha(-10934, alpha));
      }

      if (highlight) {
         Ui.roundRect(left, y, w, bh, bh / 2.0F, ColorUtil.mulAlpha(1728053247, alpha));
      }

      if (s.hardcore) {
         Ui.outline(left, y, w, bh, bh / 2.0F, 1.0F / Ui.px, ColorUtil.mulAlpha(-1, alpha * 0.6F));
      }

      String n = health + " / " + Math.round(s.maxHealth) + (s.absorption > 0.0F ? " §6+" + MathHelper.ceiling_float_int(s.absorption) : "");
      float size = 6.2F;
      float tw = Ui.width(n.replace("§6", ""), size);
      Ui.text(n.replace("§6", ""), left + (w - tw) / 2.0F, y + (bh - Ui.lineHeight(size)) / 2.0F, size, ColorUtil.mulAlpha(-1, alpha));
   }

   void armor(ItemHudState s, ItemHudRenderer.Style st, float h, float alpha0) {
      if (!(this.armorA <= 0.01F) && s.armor > 0) {
         float alpha = Easing.outCubic(this.armorA) * alpha0;
         float left = this.armorX.value;
         float top = h - this.armorH.value;
         if (st.icons == 2) {
            thinBar(st, left, top + 3.0F, s.armor / 20.0F, st.armorRow[0], alpha);
         } else {
            boolean themed = st.icons == 1 && HudIcons.ready(Math.round(9.0F * Ui.px));
            WorldRenderer wr = begin(themed);

            for (int i = 1; i < 20; i += 2) {
               float x = left + (i - 1) / 2 * 8;
               int fill = i < s.armor ? 2 : (i == s.armor ? 1 : 0);
               if (themed) {
                  slot(wr, st, st.armorShape, x, top, 9.0F, alpha, false);
                  if (fill > 0) {
                     filled(wr, st, st.armorShape, x, top, 9.0F, 0.0F, fill == 2 ? 1.0F : 0.5F, st.armorRow[(i - 1) / 2], alpha);
                  }
               } else {
                  GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
                  texture(x, top, fill == 2 ? 34 : (fill == 1 ? 25 : 16), 9, 9, 9, -90.0F);
               }
            }

            end(themed);
         }
      }
   }

   void food(ItemHudState s, ItemHudRenderer.Style st, float h, float alpha0) {
      if (!(this.foodA <= 0.01F)) {
         float alpha = Easing.outCubic(this.foodA) * alpha0;
         float top = h - this.foodH.value;
         float r = this.rightEdge;
         if (st.icons == 2) {
            float w = 81.0F;
            Ui.roundRect(r - w, top + 2.0F, w, 5.0F, 2.5F, ColorUtil.mulAlpha(st.slot, alpha));
            float fw = w * Math.max(0, s.food) / 20.0F;
            if (fw > 0.5F) {
               int c0 = s.hungerEffect ? -7617718 : st.foodRow[9];
               int c1 = s.hungerEffect ? -7617718 : st.foodRow[0];
               Ui.gradientH(r - fw, top + 2.0F, fw, 5.0F, 2.5F, ColorUtil.mulAlpha(c0 | 0xFF000000, alpha), ColorUtil.mulAlpha(c1 | 0xFF000000, alpha));
            }
         } else {
            boolean themed = st.icons == 1 && HudIcons.ready(Math.round(9.0F * Ui.px));
            this.rand.setSeed(s.ticks * 312871L + 7L);
            WorldRenderer wr = begin(themed);

            for (int i = 0; i < 10; i++) {
               int idx = i * 2 + 1;
               float x = r - i * 8 - 9.0F;
               float y = top;
               if (s.saturation <= 0.0F && s.ticks % (s.food * 3 + 1) == 0) {
                  y = top + (this.rand.nextInt(3) - 1);
               }

               int fill = idx < s.food ? 2 : (idx == s.food ? 1 : 0);
               if (themed) {
                  slot(wr, st, st.foodShape, x, y, 9.0F, alpha, false);
                  if (fill > 0) {
                     int c = s.hungerEffect ? -7617718 : st.foodRow[i];
                     filled(wr, st, st.foodShape, x, y, 9.0F, fill == 2 ? 0.0F : 0.5F, 1.0F, c, alpha);
                  }
               } else {
                  int icon = 16 + (s.hungerEffect ? 36 : 0);
                  int bg = s.hungerEffect ? 13 : 0;
                  GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
                  texture(x, y, 16 + bg * 9, 27, 9, 9, -90.0F);
                  if (fill == 2) {
                     texture(x, y, icon + 36, 27, 9, 9, -90.0F);
                  } else if (fill == 1) {
                     texture(x, y, icon + 45, 27, 9, 9, -90.0F);
                  }
               }
            }

            end(themed);
         }
      }
   }

   void mount(ItemHudState s, ItemHudRenderer.Style st, float h, float alpha0) {
      float alpha = Easing.outCubic(this.foodA) * alpha0;
      float top = h - this.foodH.value;
      float r = this.rightEdge;
      if (st.icons == 2) {
         float w = 81.0F;
         float frac = s.mountMax <= 0.0F ? 0.0F : Math.max(0.0F, s.mountHealth) / s.mountMax;
         Ui.roundRect(r - w, top + 1.0F, w, 7.0F, 3.5F, ColorUtil.mulAlpha(st.slot, alpha));
         if (frac > 0.0F) {
            Ui.roundRect(r - w * frac, top + 1.0F, w * frac, 7.0F, 3.5F, ColorUtil.mulAlpha(-30116, alpha));
         }
      } else {
         int health = (int)Math.ceil(s.mountHealth);
         int hearts = Math.min(30, (int)(s.mountMax + 0.5F) / 2);
         boolean themed = st.icons == 1 && HudIcons.ready(Math.round(9.0F * Ui.px));
         WorldRenderer wr = begin(themed);
         float y = top;

         for (int heart = 0; hearts > 0; heart += 20) {
            int rowCount = Math.min(hearts, 10);
            hearts -= rowCount;

            for (int i = 0; i < rowCount; i++) {
               float x = r - i * 8 - 9.0F;
               int idx = i * 2 + 1 + heart;
               int fill = idx < health ? 2 : (idx == health ? 1 : 0);
               if (themed) {
                  slot(wr, st, st.heartShape, x, y, 9.0F, alpha, false);
                  if (fill > 0) {
                     filled(wr, st, st.heartShape, x, y, 9.0F, 0.0F, fill == 2 ? 1.0F : 0.5F, -30116, alpha);
                  }
               } else {
                  GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
                  texture(x, y, 52, 9, 9, 9, -90.0F);
                  if (fill == 2) {
                     texture(x, y, 88, 9, 9, 9, -90.0F);
                  } else if (fill == 1) {
                     texture(x, y, 97, 9, 9, 9, -90.0F);
                  }
               }
            }

            y -= 10.0F;
         }

         end(themed);
      }
   }

   void air(ItemHudState s, ItemHudRenderer.Style st, float h, float alpha0) {
      if (!(this.airA <= 0.01F) && s.underwater) {
         float alpha = Easing.outCubic(this.airA) * alpha0;
         float top = h - this.airH.value;
         float r = this.rightEdge;
         if (st.icons == 2) {
            thinBar(st, r - 81.0F, top + 3.0F, Math.max(0, s.air) / 300.0F, st.airRow[0], alpha);
         } else {
            int full = MathHelper.ceiling_double_int((s.air - 2) * 10.0 / 300.0);
            int partial = MathHelper.ceiling_double_int(s.air * 10.0 / 300.0) - full;
            boolean themed = st.icons == 1 && HudIcons.ready(Math.round(9.0F * Ui.px));
            WorldRenderer wr = begin(themed);

            for (int i = 0; i < full + partial; i++) {
               float x = r - i * 8 - 9.0F;
               if (themed) {
                  filled(wr, st, i < full ? HudIcons.Icon.BUBBLE : HudIcons.Icon.BUBBLE_POP, x, top, 9.0F, 0.0F, 1.0F, st.airRow[Math.min(9, i)], alpha);
               } else {
                  GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
                  texture(x, top, i < full ? 16 : 25, 18, 9, 9, -90.0F);
               }
            }

            end(themed);
         }
      }
   }

   private static void thinBar(ItemHudRenderer.Style st, float x, float y, float frac, int color, float alpha) {
      float w = 81.0F;
      Ui.roundRect(x, y, w, 3.0F, 1.5F, ColorUtil.mulAlpha(st.slot, alpha));
      float fw = w * Easing.clamp01(frac);
      if (fw > 0.5F) {
         Ui.roundRect(x, y, fw, 3.0F, 1.5F, ColorUtil.mulAlpha(color | 0xFF000000, alpha));
      }
   }

   void xp(ItemHudState s, ItemHudRenderer.Style st, HudLook look, int accent, float w, float h, float dt, float speed) {
      if (s.xpShown || s.horse) {
         float left = (int)w / 2 - 91.0F;
         float top = h - 29.0F;
         float alpha = s.horse ? 1.0F : Easing.outCubic(this.xpA);
         if (s.horse) {
            if (st.xpThemed) {
               float y = top + 1.0F;
               Ui.roundRect(left, y, 182.0F, 3.0F, 1.5F, -1728053248);
               float fw = 182.0F * Easing.clamp01(s.jump);
               if (fw > 0.5F) {
                  Ui.gradientH(left, y, fw, 3.0F, 1.5F, -10934, -30147);
               }
            } else {
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               texture(left, top, 0, 84, 182, 5, -90.0F);
               int filled = (int)(s.jump * 183.0F);
               if (filled > 0) {
                  texture(left, top, 0, 89, filled, 5, -90.0F);
               }
            }
         } else {
            float target = s.level + s.xp;
            if (!(this.xpShown < 0.0F) && !(speed <= 0.0F) && !(Math.abs(target - this.xpShown) > 3.0F)) {
               this.xpShown = Easing.approach(this.xpShown, target, 9.0F * speed, dt);
               if (Math.abs(this.xpShown - target) < 5.0E-4F) {
                  this.xpShown = target;
               }
            } else {
               this.xpShown = target;
            }

            if (this.lastLevel >= 0 && s.level > this.lastLevel && speed > 0.0F) {
               for (int i = 0; i < this.sparkX.length; i++) {
                  this.sparkX[i] = (float)Math.random();
                  this.sparkT[i] = -((float)Math.random()) * 0.15F;
               }
            }

            this.lastLevel = s.level;
            float frac = this.xpShown - (float)Math.floor(this.xpShown);
            if (target - this.xpShown < 1.0E-4F && s.xp >= 0.9999F) {
               frac = 1.0F;
            }

            if (alpha > 0.01F) {
               if (st.xpThemed) {
                  float y = top + 1.0F;
                  Ui.roundRect(left, y, 182.0F, 3.0F, 1.5F, ColorUtil.mulAlpha(-1728053248, alpha));
                  float fw = 182.0F * frac;
                  if (fw > 0.5F) {
                     int a = accent | 0xFF000000;
                     Ui.gradientH(left, y, fw, 3.0F, 1.5F, ColorUtil.mulAlpha(a, alpha), ColorUtil.mulAlpha(ColorUtil.lerp(a, -1, 0.4F), alpha));
                  }

                  for (int i = 0; i < this.sparkX.length; i++) {
                     if (this.sparkT[i] < 0.6F) {
                        this.sparkT[i] = this.sparkT[i] + dt * speed;
                        float t = Math.max(0.0F, this.sparkT[i]) / 0.6F;
                        if (this.sparkT[i] > 0.0F) {
                           Ui.glow(left + 182.0F * this.sparkX[i], y + 1.5F - 7.0F * t, 6.0F * (1.0F - t), ColorUtil.mulAlpha(-1, (1.0F - t) * alpha));
                        }
                     }
                  }
               } else {
                  Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.icons);
                  GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
                  texture(left, top, 0, 64, 182, 5, -90.0F);
                  int filled = (int)(frac * 183.0F);
                  if (filled > 0) {
                     texture(left, top, 0, 69, filled, 5, -90.0F);
                  }

                  GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               }
            }

            if (!st.hideLevel && s.level > 0) {
               String text = String.valueOf(s.level);
               float y = h - 35.0F + (st.hideXp ? 5.0F : 0.0F);
               if (st.xpThemed) {
                  float size = 7.2F;
                  float tw = Ui.width(text, size);
                  float pw = tw + 8.0F;
                  float ph = 9.0F;
                  float px = (w - pw) / 2.0F;
                  float py = y - 0.5F;
                  if (st.centered && !st.hideXp) {
                     px = left - pw - 3.0F;
                     py = top + 2.5F - ph / 2.0F;
                  }

                  Ui.roundRect(px, py, pw, ph, ph / 2.0F, look.bg(0.9F));
                  Ui.text(text, px + 4.0F, py + (ph - Ui.lineHeight(size)) / 2.0F, size, -8323296);
               } else {
                  FontRenderer fr = font();
                  int x = (int)((w - fr.getStringWidth(text)) / 2.0F);
                  int iy = (int)y;
                  fr.drawString(text, x + 1, iy, 0);
                  fr.drawString(text, x - 1, iy, 0);
                  fr.drawString(text, x, iy + 1, 0);
                  fr.drawString(text, x, iy - 1, 0);
                  fr.drawString(text, x, iy, 8453920);
                  GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               }
            }
         }
      }
   }

   static void itemName(String name, HudLook look, float w, float bottom, float alpha) {
      if (!(alpha <= 0.02F)) {
         float tw = HudFont.width(name);
         float pw = tw + 12.0F;
         float ph = 12.0F;
         float x = (w - pw) / 2.0F;
         float y = bottom - ph;
         HudPanel.background(x, y, pw, ph, ph / 2.0F, look, alpha);
         HudFont.setOnLight(look.colors.light);
         HudFont.draw(name, x + 6.0F, y + 2.0F, ColorUtil.mulAlpha(look.colors.text | 0xFF000000, alpha), false);
         HudFont.setOnLight(false);
         HudPanel.foreground(x, y, pw, ph, ph / 2.0F, look, alpha);
      }
   }

   private static WorldRenderer begin(boolean themed) {
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      if (!themed) {
         Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.icons);
         return null;
      } else {
         GlStateManager.enableTexture2D();
         GlStateManager.bindTexture(HudIcons.texture());
         WorldRenderer wr = Tessellator.getInstance().getWorldRenderer();
         wr.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         return wr;
      }
   }

   private static void end(boolean themed) {
      if (themed) {
         Tessellator.getInstance().draw();
      }

      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.icons);
   }

   static void texture(float x, float y, int u, int v, int w, int h, float z) {
      float f = 0.00390625F;
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_TEX);
      wr.pos(x, y + h, z).tex(u * f, (v + h) * f).endVertex();
      wr.pos(x + w, y + h, z).tex((u + w) * f, (v + h) * f).endVertex();
      wr.pos(x + w, y, z).tex((u + w) * f, v * f).endVertex();
      wr.pos(x, y, z).tex(u * f, v * f).endVertex();
      tess.draw();
   }

   void reset() {
      this.xpShown = -1.0F;
      this.lastLevel = -1;
      this.ghostHealth = -1.0F;
      this.lastSelected = -1;
      Arrays.fill(this.heartFill, 0);
   }

   static final class Style {
      boolean themed = true;
      boolean dividers = true;
      boolean selectionGlow;
      boolean modFontCounts = true;
      boolean roundedDurability = true;
      boolean hideHearts;
      boolean hideFood;
      boolean hideXp;
      boolean hideLevel;
      boolean hideArmor;
      boolean hideAir;
      boolean centered;
      int icons = 1;
      HudIcons.Icon heartShape = HudIcons.Icon.HEART;
      HudIcons.Icon foodShape = HudIcons.Icon.FOOD_DRUMSTICK;
      HudIcons.Icon armorShape = HudIcons.Icon.ARMOR_SHIELD;
      int heartColor = -50354;
      final int[] heartRow = row(-50354);
      final int[] foodRow = row(-875952);
      final int[] armorRow = row(-2235414);
      final int[] airRow = row(-6301441);
      int slot = -1726736352;
      int slotRim = 788529151;
      float gloss = 0.45F;
      float lighten = 0.28F;
      float glow = 0.42F;
      boolean glassEdge;
      boolean shake = true;
      boolean xpThemed = true;
      boolean animate = true;
      boolean instant;
      boolean lift = true;
      boolean damageTrail = true;
      float bgOpacity = 1.0F;

      private static int[] row(int c) {
         int[] r = new int[10];
         Arrays.fill(r, c);
         return r;
      }
   }
}
