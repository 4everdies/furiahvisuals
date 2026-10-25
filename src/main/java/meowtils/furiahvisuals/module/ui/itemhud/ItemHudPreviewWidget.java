package meowtils.furiahvisuals.module.ui.itemhud;

import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

final class ItemHudPreviewWidget extends RowWidget {
   private static final float BOX_H = 120.0F;
   private static final String[] TESTS = new String[]{"Damage", "Heal", "Absorption", "Poison", "Regen", "Pickup", "Switch", "Level Up", "Under Water", "Ride"};
   private final ItemHudElement element;
   private final ItemHudRenderer renderer = new ItemHudRenderer();
   private final ItemHudState state = new ItemHudState();
   private final HudLook look = new HudLook();
   private final Pill[] pills = new Pill[TESTS.length];
   private final long start = ClientClock.nanos();
   private long lastFrame;
   private long hurtUntil;
   private int airTick;
   private float testsH = 20.0F;

   ItemHudPreviewWidget(ItemHudElement element) {
      this.element = element;

      for (int i = 0; i < this.pills.length; i++) {
         this.pills[i] = new Pill();
      }

      element.lastPreview = this;
      ItemStack[] items = new ItemStack[]{
         new ItemStack(Items.diamond_sword),
         new ItemStack(Items.bow),
         new ItemStack(Items.iron_pickaxe),
         new ItemStack(Blocks.planks, 48),
         new ItemStack(Items.golden_apple, 3),
         new ItemStack(Items.ender_pearl, 16),
         new ItemStack(Items.cooked_beef, 12),
         null,
         new ItemStack(Items.water_bucket)
      };
      items[2].setItemDamage(180);
      System.arraycopy(items, 0, this.state.slots, 0, 9);
      this.state.survival = true;
      this.state.health = 15.0F;
      this.state.maxHealth = 20.0F;
      this.state.food = 14;
      this.state.saturation = 3.0F;
      this.state.armor = 12;
      this.state.air = 300;
      this.state.level = 23;
      this.state.xp = 0.55F;
      this.state.xpShown = true;
      this.state.mountMax = 22.0F;
      this.state.mountHealth = 15.0F;
   }

   @Override
   public float height() {
      return 126.0F + this.testsH + 2.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float bx = this.x;
      float by = this.y;
      float bw = this.w;
      float bh = 120.0F;
      Ui.gradientV(bx, by, bw, bh, 8.0F, -8411430, -10843586);
      float menuPx = Ui.px;
      Minecraft mc = Minecraft.getMinecraft();
      float sf = new ScaledResolution(mc).getScaleFactor() * this.element.scale.getFloat();
      float wantW = 200.0F;
      float wantH = 80.0F;
      float fit = Math.min(1.0F, Math.min((bw - 10.0F) * menuPx / (wantW * sf), (bh - 8.0F) * menuPx / (wantH * sf)));
      float unitPx = sf * fit;
      float u = unitPx / menuPx;
      long now = ClientClock.nanos();
      float realDt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
      this.lastFrame = now;
      this.state.ticks = (int)((now - this.start) / 50000000L);
      this.state.partial = (float)((now - this.start) % 50000000L) / 5.0E7F;
      this.state.hurtTime = now < this.hurtUntil;
      float screenW = bw / u;
      float screenH = (bh - 4.0F) / u;
      Ui.pushScissor(bx, by, bw, bh);
      GlStateManager.pushMatrix();
      GlStateManager.translate(bx, by + 2.0F, 0.0F);
      GlStateManager.scale(u, u, 1.0F);
      float savedOffset = Ui.offsetY;
      Ui.px = unitPx;
      Ui.offsetY = savedOffset * menuPx / unitPx + (by + 2.0F) * menuPx / unitPx;

      try {
         HudLook l = this.element.updateLook(this.look, unitPx, realDt);
         l.glass = false;
         ItemHudRenderer.Style st = this.element.style(l);
         float speed = this.element.speed();
         this.renderer.rightEdge = (int)screenW / 2 + 91.0F;
         this.renderer.layout(this.state, st, screenW, realDt, speed);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         this.renderer.hotbar(this.state, st, l, screenW, screenH, realDt, speed, l.colors.accent);
         this.renderer.hearts(this.state, st, l.colors.accent, screenH, realDt, speed);
         this.renderer.armor(this.state, st, screenH, 1.0F);
         if (this.state.mounted) {
            this.renderer.mount(this.state, st, screenH, 1.0F);
         } else {
            this.renderer.food(this.state, st, screenH, 1.0F);
         }

         this.renderer.air(this.state, st, screenH, 1.0F);
         this.renderer.xp(this.state, st, l, l.colors.accent, screenW, screenH, realDt, speed);
      } finally {
         Ui.px = menuPx;
         Ui.offsetY = savedOffset;
         GlStateManager.popMatrix();
         Ui.popScissor();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
         GlStateManager.disableAlpha();
         GlStateManager.enableTexture2D();
         GlStateManager.disableLighting();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (fit < 0.99F) {
         String label = Math.round(fit * 100.0F) + "% of its size";
         float lw = Ui.width(label, 7.6F) + 12.0F;
         Ui.roundRect(bx + 5.0F, by + 5.0F, lw, 13.0F, 6.5F, -1946157056);
         Ui.textMiddle(label, bx + 11.0F, by + 11.5F, 7.6F, -251658241, false);
      }

      float tx = this.x;
      float ty = this.y + 120.0F + 6.0F;

      for (int i = 0; i < TESTS.length; i++) {
         float tw = Ui.width(TESTS[i], 8.8F) + 20.0F;
         if (tx > this.x && tx + tw > this.x + this.w) {
            tx = this.x;
            ty += 25.0F;
         }

         this.pills[i].layout(tx, ty, TESTS[i]);
         boolean on = i == 3 && (this.state.poison || this.state.wither)
            || i == 4 && this.state.regen
            || i == 8 && this.state.underwater
            || i == 9 && this.state.mounted;
         this.pills[i].draw(TESTS[i], mx, my, dt, accent, on, true);
         tx += tw + 5.0F;
      }

      this.testsH = ty - (this.y + 120.0F + 6.0F) + 20.0F;
      int steps = Math.max(0, Math.min(300, this.state.ticks - this.airTick));
      this.airTick = this.state.ticks;
      this.state.air = this.state.underwater ? Math.max(0, this.state.air - 3 * steps) : Math.min(300, this.state.air + 12 * steps);
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         for (int i = 0; i < TESTS.length; i++) {
            if (this.pills[i].contains(mx, my)) {
               this.test(i);
               ModSounds.gui(Sfx.CLICK);
               return true;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   void test(int i) {
      long now = ClientClock.nanos();
      switch (i) {
         case 0:
            this.state.health = Math.max(1.0F, this.state.health - 5.0F);
            this.hurtUntil = now + 500000000L;
            break;
         case 1:
            this.state.health = Math.min(this.state.maxHealth, this.state.health + 4.0F);
            this.hurtUntil = now + 500000000L;
            break;
         case 2:
            this.state.absorption = this.state.absorption > 0.0F ? 0.0F : 4.0F;
            break;
         case 3:
            if (!this.state.poison && !this.state.wither) {
               this.state.poison = true;
            } else if (this.state.poison) {
               this.state.poison = false;
               this.state.wither = true;
            } else {
               this.state.wither = false;
            }
            break;
         case 4:
            this.state.regen = !this.state.regen;
            break;
         case 5:
            ItemStack planks = this.state.slots[3];
            if (planks != null) {
               planks.stackSize = Math.min(64, planks.stackSize + 8);
               planks.animationsToGo = 5;
            }
            break;
         case 6:
            this.state.selected = (this.state.selected + 1) % 9;
            break;
         case 7:
            this.state.level++;
            this.state.xp = 0.1F;
            break;
         case 8:
            this.state.underwater = !this.state.underwater;
            break;
         case 9:
            this.state.mounted = !this.state.mounted;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      for (int i = 0; i < TESTS.length; i++) {
         if (this.pills[i].contains(mx, my)) {
            switch (i) {
               case 0:
                  return "Takes two and a half hearts: lost hearts shrink away, the bar leaves a trail";
               case 1:
                  return "Heals two hearts: they pop in";
               case 2:
                  return "Two gold absorption hearts (on and off)";
               case 3:
                  return "Poison, then wither, then neither";
               case 4:
                  return "Regeneration: the wave along the hearts";
               case 5:
                  return "Picks up eight planks: their slot pops";
               case 6:
                  return "The next slot: the highlight glides (from the last slot round to the first)";
               case 7:
                  return "One level more: the bar fills and sparkles";
               case 8:
                  return "Under water: air bubbles above the hunger";
               default:
                  return "Riding: the mount's hearts take the hunger's place";
            }
         }
      }

      return null;
   }
}
