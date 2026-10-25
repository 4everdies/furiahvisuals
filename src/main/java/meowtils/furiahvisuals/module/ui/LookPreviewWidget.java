package meowtils.furiahvisuals.module.ui;

import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.hud.HudFont;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.hud.HudPanel;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;

final class LookPreviewWidget extends Widget {
   private static final String[] BOARD = new String[]{
      "§e§lFURIAH §r§7§o(sample)", "§fKills: §a12", "§fMap: §bGlacier", "§c❤ §f20  §6✫ §7Rank  §d⚔", "§nunderline§r §mstrike§r §kobfus§r"
   };
   private static final String CHAT = "§7<§bSteve§7> §fhello §e§lworld§r §a§o:)";
   private static final String PILL = "§d§lFV §8· §fThemed";
   private final UiModule module;
   private final HudLook board = new HudLook();
   private final HudLook chat = new HudLook();
   private final HudLook pill = new HudLook();

   LookPreviewWidget(UiModule module) {
      this.module = module;
   }

   @Override
   public float height() {
      return 104.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float top = this.y + 4.0F;
      float h = this.height() - 8.0F;
      Ui.gradientV(this.x, top, this.w, h, 8.0F, -8804632, -10580930);
      this.board.update(HudLook.Mode.SAME, true, 0.85F, 4.0F, true, WaterMarkModule.GlassMode.OFF, HudLook.Glint.SAME, Ui.px, dt);
      this.chat.update(HudLook.Mode.SAME, true, 0.85F, 4.0F, true, WaterMarkModule.GlassMode.OFF, HudLook.Glint.SAME, Ui.px, dt);
      this.pill.update(HudLook.Mode.SAME, true, 0.85F, 4.0F, true, WaterMarkModule.GlassMode.OFF, HudLook.Glint.SAME, Ui.px, dt);
      this.pill.glass = false;
      this.chat.glass = false;
      this.board.glass = false;
      float alpha = Ui.alpha();
      HudFont.setOnLight(this.board.colors.light);
      float bw = 0.0F;

      for (String s : BOARD) {
         bw = Math.max(bw, HudFont.width(s));
      }

      float bh = BOARD.length * 10.0F + 8.0F;
      float var20;
      float bx = this.x + this.w - (var20 = bw + 12.0F) - 10.0F;
      float by = top + (h - bh) / 2.0F;
      HudPanel.background(bx, by, var20, bh, this.board.radius, this.board, alpha);

      for (int i = 0; i < BOARD.length; i++) {
         float ty = by + 4.0F + i * 10.0F;
         if (i == 0) {
            HudFont.drawCentered(BOARD[i], bx + var20 / 2.0F, ty, this.board.colors.text | 0xFF000000, false);
         } else {
            HudFont.draw(BOARD[i], bx + 6.0F, ty, this.board.colors.text | 0xFF000000, false);
         }
      }

      HudPanel.foreground(bx, by, var20, bh, this.board.radius, this.board, alpha);
      float cw = HudFont.width("§7<§bSteve§7> §fhello §e§lworld§r §a§o:)") + 12.0F;
      float ch = 13.0F;
      float cx = this.x + 10.0F;
      float cy = top + h - ch - 10.0F;
      HudPanel.background(cx, cy, cw, ch, this.chat.radius, this.chat, alpha);
      HudFont.draw("§7<§bSteve§7> §fhello §e§lworld§r §a§o:)", cx + 6.0F, cy + 2.5F, this.chat.colors.text | 0xFF000000, !this.chat.colors.light);
      HudPanel.foreground(cx, cy, cw, ch, this.chat.radius, this.chat, alpha);
      float pw = HudFont.width("§d§lFV §8· §fThemed") + 16.0F;
      float ph = 15.0F;
      float pxl = this.x + 10.0F;
      float pyl = top + 10.0F;
      HudPanel.background(pxl, pyl, pw, ph, ph / 2.0F, this.pill, alpha);
      HudFont.draw("§d§lFV §8· §fThemed", pxl + 8.0F, pyl + 3.5F, this.pill.colors.text | 0xFF000000, false);
      HudPanel.foreground(pxl, pyl, pw, ph, ph / 2.0F, this.pill, alpha);
      HudFont.setOnLight(false);
      Ui.textMiddle("The island's look, glint and the mod font", this.x + 10.0F, top + h / 2.0F, 8.0F, -6050627, false);
   }
}
