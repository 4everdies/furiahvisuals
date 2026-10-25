package meowtils.furiahvisuals.module.ui.scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.hud.HudPosition;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.ScoreObjective;

final class ScoreboardPreviewWidget extends RowWidget {
   private static final float BOX_H = 150.0F;
   private static final String[] TESTS = new String[]{"Numbers", "Text", "Add Line", "Remove Line", "Live"};
   private final ScoreboardElement element;
   private final ScoreboardRenderer renderer = new ScoreboardRenderer();
   private final HudLook look = new HudLook();
   private final HudPosition place = new HudPosition("preview", "Preview");
   private final Pill[] pills = new Pill[TESTS.length];
   private final List<String[]> sample = new ArrayList<>();
   private final Random random = new Random(7L);
   private boolean live;
   private boolean eventLine;
   private boolean mapChanged;
   private float testsH = 20.0F;

   ScoreboardPreviewWidget(ScoreboardElement element) {
      this.element = element;

      for (int i = 0; i < this.pills.length; i++) {
         this.pills[i] = new Pill();
      }

      for (int var3 = 0; var3 < ScoreboardElement.SAMPLE.length; var3++) {
         this.sample.add(new String[]{ScoreboardElement.SAMPLE[var3][0], ScoreboardElement.SAMPLE[var3][1], "sample" + var3});
      }

      this.live = ScoreboardElement.currentObjective() != null;
      element.lastPreview = this;
   }

   @Override
   public float height() {
      return 156.0F + this.testsH + 2.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float bx = this.x;
      float by = this.y;
      float bw = this.w;
      float bh = 150.0F;
      Ui.gradientV(bx, by, bw, bh, 8.0F, -8804632, -10580930);
      ScoreObjective objective = this.live ? ScoreboardElement.currentObjective() : null;
      List<String[]> lines;
      String title;
      if (objective != null) {
         title = objective.getDisplayName();
         lines = new ArrayList<>();

         for (String[] l : ScoreboardElement.linesOf(objective)) {
            lines.add(new String[]{l[0], l[1], l[2]});
         }
      } else {
         title = "§e§lFURIAH GAMES";
         lines = this.sample;
      }

      String scope = ScoreboardRules.scope(title);
      ArrayList<ScoreboardRenderer.Entry> entries = new ArrayList<>();

      for (String[] l : lines) {
         if (!this.element.hidden(scope, l[0])) {
            entries.add(new ScoreboardRenderer.Entry(l[2], l[0], Integer.parseInt(l[1])));
         }
      }

      float menuPx = Ui.px;
      float sf = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
      float unitPx = sf * this.element.scale.getFloat();
      float fit = 1.0F;
      if (this.renderer.lastH > 0.0F && this.renderer.lastW > 0.0F) {
         fit = Math.min(1.0F, Math.min(bh * 0.9F * menuPx / (this.renderer.lastH * unitPx), bw * 0.75F * menuPx / (this.renderer.lastW * unitPx)));
      }

      float var33;
      float u = (var33 = unitPx * fit) / menuPx;
      Ui.pushScissor(bx, by, bw, bh);
      GlStateManager.pushMatrix();
      GlStateManager.translate(bx, by, 0.0F);
      GlStateManager.scale(u, u, 1.0F);
      Ui.px = var33;

      try {
         HudLook lx = this.element.updateLook(this.look, var33, dt);
         lx.glass = false;
         ScoreboardRenderer.Style s = this.element.style(lx);
         float speed = this.element.speed();
         this.renderer.update(title, entries, dt, speed);
         float sw = bw / u;
         float sh = bh / u;
         if (this.renderer.lastW > 0.0F) {
            this.place.place(sw - this.renderer.lastW - 6.0F, (sh - this.renderer.lastH) / 2.0F, this.renderer.lastW, this.renderer.lastH, sw, sh);
         }

         this.renderer.draw(s, lx, sw, sh, this.renderer.lastW > 0.0F ? this.place : null, 1.0F, Ui.alpha(), dt, speed);
      } finally {
         Ui.px = menuPx;
         GlStateManager.popMatrix();
         Ui.popScissor();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
         GlStateManager.disableAlpha();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }

      String label = (objective != null ? "Live: the scoreboard shown now" : "Sample")
         + (fit < 0.99F ? " · " + Math.round(fit * 100.0F) + "% of its size" : "");
      float lw = Ui.width(label, 7.6F) + 12.0F;
      Ui.roundRect(bx + 5.0F, by + 5.0F, lw, 13.0F, 6.5F, -1946157056);
      Ui.textMiddle(label, bx + 11.0F, by + 11.5F, 7.6F, -251658241, false);
      float tx = this.x;
      float ty = this.y + 150.0F + 6.0F;
      boolean hasLive = ScoreboardElement.currentObjective() != null;

      for (int i = 0; i < TESTS.length; i++) {
         if (i == 4 && !hasLive) {
            this.pills[i].w = 0.0F;
         } else {
            float tw = Ui.width(TESTS[i], 8.8F) + 20.0F;
            if (tx > this.x && tx + tw > this.x + this.w) {
               tx = this.x;
               ty += 25.0F;
            }

            this.pills[i].layout(tx, ty, TESTS[i]);
            this.pills[i].draw(TESTS[i], mx, my, dt, accent, i == 4 && this.live, i == 4 || !this.live || !hasLive);
            tx += tw + 5.0F;
         }
      }

      this.testsH = ty - (this.y + 150.0F + 6.0F) + 20.0F;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         for (int i = 0; i < TESTS.length; i++) {
            if (this.pills[i].w > 0.0F && this.pills[i].contains(mx, my)) {
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
      if (i == 4) {
         this.live = !this.live;
      } else {
         this.live = false;
         switch (i) {
            case 0:
               for (String[] lx : this.sample) {
                  if (lx[0].startsWith("Kills") || lx[0].startsWith("Coins") || lx[0].startsWith("Deaths") && this.random.nextBoolean()) {
                     String digits = lx[0].replaceAll("[^0-9]", "");
                     int v = digits.isEmpty() ? 0 : Integer.parseInt(digits);
                     int nv = v + 1 + this.random.nextInt(lx[0].startsWith("Coins") ? 40 : 3);
                     lx[0] = lx[0].replace(Integer.toString(v), Integer.toString(nv));
                  }
               }
               break;
            case 1:
               for (String[] l : this.sample) {
                  if (l[0].startsWith("Map: ")) {
                     l[0] = this.mapChanged ? "Map: §bGlacier" : "Map: §aJungle Temple";
                  }
               }

               this.mapChanged = !this.mapChanged;
               break;
            case 2:
               if (!this.eventLine) {
                  this.sample.add(3, new String[]{"§dEvent: §fDouble Coins", "7", "sampleEvent"});
                  this.eventLine = true;
               }
               break;
            case 3:
               for (int k = 0; k < this.sample.size(); k++) {
                  if (this.sample.get(k)[2].equals("sampleEvent")) {
                     this.sample.remove(k);
                     this.eventLine = false;
                     break;
                  }
               }
         }
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      for (int i = 0; i < TESTS.length; i++) {
         if (this.pills[i].w > 0.0F && this.pills[i].contains(mx, my)) {
            switch (i) {
               case 0:
                  return "Some scores change: their digits roll";
               case 1:
                  return "A line changes: it crossfades";
               case 2:
                  return "A line appears: it slides in, the panel grows";
               case 3:
                  return "That line goes: it slides out, the panel shrinks";
               default:
                  return "The scoreboard shown now, or the sample";
            }
         }
      }

      return null;
   }

   static int muted() {
      return Theme.TEXT_MUTED;
   }
}
