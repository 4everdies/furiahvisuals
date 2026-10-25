package meowtils.furiahvisuals.module.ui.nametag;

import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.module.ui.scoreboard.ScoreboardElement;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

final class NametagPreviewWidget extends RowWidget {
   private static final float BOX_H = 120.0F;
   private static final String[] TESTS = new String[]{"Behind Wall", "Sneak", "Distance", "Score"};
   private static final float[] DISTANCES = new float[]{1.0F, 0.62F, 0.38F};
   private final NametagElement element;
   private final Pill[] pills = new Pill[TESTS.length];
   private boolean behind;
   private boolean sneak;
   private boolean score;
   private int distance;
   private float testsH = 20.0F;

   NametagPreviewWidget(NametagElement element) {
      this.element = element;

      for (int i = 0; i < this.pills.length; i++) {
         this.pills[i] = new Pill();
      }

      element.lastPreview = this;
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
      Ui.gradientV(bx, by, bw, bh, 8.0F, -7490334, -10580922);
      float menuPx = Ui.px;
      float d = DISTANCES[this.distance];
      float u = Math.min((bh - 18.0F) / 50.0F, bw / 300.0F) * d;
      float unitPx = u * menuPx;
      String me = NametagElement.myName();
      String[] names = new String[]{"§a" + me, "§c[RED] §fNetherNova", "§6§lDAILY REWARDS"};
      NametagElement.Who[] who = new NametagElement.Who[]{NametagElement.Who.ME, NametagElement.Who.OTHER, NametagElement.Who.THING};
      Ui.pushScissor(bx, by, bw, bh);

      try {
         float groundY = by + bh - 10.0F;
         if (this.behind) {
            Ui.roundRect(bx + 10.0F, by + 20.0F, bw - 20.0F, bh - 34.0F, 4.0F, -9539981);
            Ui.rect(bx + 10.0F, by + 20.0F + (bh - 34.0F) / 2.0F, bw - 20.0F, 1.0F, 1073741824);
         }

         for (int i = 0; i < 3; i++) {
            float cx = bx + bw * (0.18F + 0.32F * i);
            float figureH = 24.0F * u;
            float figureW = figureH * 0.42F;
            if (!this.behind) {
               int body = i == 2 ? 1442840575 : -869585094;
               Ui.roundRect(cx - figureW / 2.0F, groundY - figureH, figureW, figureH, figureW / 3.0F, body);
               Ui.roundRect(cx - figureW * 0.45F, groundY - figureH - figureW * 0.85F, figureW * 0.9F, figureW * 0.9F, figureW * 0.2F, body);
            }

            float labelBottom = groundY - figureH - figureW * 0.95F - 4.0F;
            boolean sneaking = this.sneak && i == 1;
            if (sneaking) {
               labelBottom += 9.0F * u;
            }

            this.drawLabel(names[i], who[i], cx, labelBottom, u, unitPx, menuPx, sneaking);
            if (this.score && i == 1 && !sneaking) {
               drawVanillaLabel("12 §c❤", cx, labelBottom + 11.0F * u, u, unitPx, menuPx, this.behind);
            }
         }
      } finally {
         Ui.popScissor();
      }

      if (this.distance > 0) {
         String label = this.distance == 1 ? "Middle distance" : "Far away";
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
         boolean on = i == 0 && this.behind || i == 1 && this.sneak || i == 2 && this.distance > 0 || i == 3 && this.score;
         this.pills[i].draw(TESTS[i], mx, my, dt, accent, on, true);
         tx += tw + 5.0F;
      }

      this.testsH = ty - (this.y + 120.0F + 6.0F) + 20.0F;
   }

   private void drawLabel(String str, NametagElement.Who who, float cx, float bottom, float u, float unitPx, float menuPx, boolean sneaking) {
      boolean themed = who != NametagElement.Who.THING || this.element.holograms.get() == ScoreboardElement.Kind.THEMED;
      if (!themed) {
         drawVanillaLabel(str, cx, bottom, u, unitPx, menuPx, this.behind);
      } else {
         float size = this.element.size.getFloat() / 100.0F;
         GlStateManager.pushMatrix();
         GlStateManager.translate(cx, bottom, 0.0F);
         GlStateManager.scale(u * size, u * size, 1.0F);
         GlStateManager.translate(0.0F, -8.0F, 0.0F);
         Ui.px = unitPx * size;

         try {
            this.element.renderer.drawFlat(str, who, sneaking, this.behind);
         } finally {
            Ui.px = menuPx;
            GlStateManager.popMatrix();
            restoreMenuState();
         }
      }
   }

   private static void drawVanillaLabel(String str, float cx, float bottom, float u, float unitPx, float menuPx, boolean behind) {
      FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
      GlStateManager.pushMatrix();
      GlStateManager.translate(cx, bottom, 0.0F);
      GlStateManager.scale(u, u, 1.0F);
      GlStateManager.translate(0.0F, -8.0F, 0.0F);
      Ui.px = unitPx;

      try {
         int j = fr.getStringWidth(str) / 2;
         Ui.rect(-j - 1, -1.0F, 2 * j + 2, 9.0F, 1073741824);
         fr.drawString(str, -fr.getStringWidth(str) / 2, 0, behind ? 553648127 : -1);
      } finally {
         Ui.px = menuPx;
         GlStateManager.popMatrix();
         restoreMenuState();
      }
   }

   private static void restoreMenuState() {
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
      GlStateManager.disableAlpha();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
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
      switch (i) {
         case 0:
            this.behind = !this.behind;
            break;
         case 1:
            this.sneak = !this.sneak;
            break;
         case 2:
            this.distance = (this.distance + 1) % DISTANCES.length;
            break;
         case 3:
            this.score = !this.score;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      for (int i = 0; i < TESTS.length; i++) {
         if (this.pills[i].contains(mx, my)) {
            switch (i) {
               case 0:
                  return "The names seen through a wall: the background and a faint name, as vanilla shows them";
               case 1:
                  return "The red player sneaks: a faint name at the head, never seen through walls";
               case 2:
                  return "Near, middle and far: the text is drawn for its size on the screen";
               default:
                  return "Vanilla's score line under a name (it stays vanilla's)";
            }
         }
      }

      return null;
   }
}
