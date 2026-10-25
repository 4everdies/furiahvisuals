package meowtils.furiahvisuals.module.ui.scoreboard;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.hud.HudFont;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.scoreboard.ScoreObjective;

final class ScoreboardLinesWidget extends RowWidget {
   private static final float HEAD = 14.0F;
   private static final float ROW = 13.0F;
   private final ScoreboardElement element;
   private final List<String> texts = new ArrayList<>();
   private String title = "§e§lFURIAH GAMES";
   private boolean sample;
   private float[] hovers = new float[0];

   ScoreboardLinesWidget(ScoreboardElement element) {
      this.element = element;
   }

   private void refresh() {
      this.texts.clear();
      ScoreObjective objective = ScoreboardElement.currentObjective();
      if (objective != null) {
         this.sample = false;
         this.title = objective.getDisplayName();

         for (String[] line : ScoreboardElement.linesOf(objective)) {
            this.texts.add(line[0]);
         }
      } else {
         this.sample = true;
         this.title = "§e§lFURIAH GAMES";

         for (String[] line : ScoreboardElement.SAMPLE) {
            this.texts.add(line[0]);
         }
      }

      if (this.hovers.length != this.texts.size()) {
         this.hovers = new float[this.texts.size()];
      }
   }

   private String scope() {
      return !this.sample && !this.element.allServers.get() ? ScoreboardRules.scope(this.title) : "*";
   }

   @Override
   public float height() {
      this.refresh();
      return 18.0F + this.texts.size() * 13.0F + 4.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.refresh();
      String where = this.sample
         ? "No scoreboard here: the sample's lines hide on every server"
         : (this.element.allServers.get() ? "Hiding on every server" : "Hiding on this server, for this scoreboard");
      Ui.text(where, this.x, this.y + 2.0F, 8.0F, Theme.TEXT_MUTED);
      float boxY = this.y + 14.0F;
      float boxH = this.texts.size() * 13.0F + 8.0F;
      Ui.surface(this.x, boxY, this.w, boxH, 7.0F, Theme.SURFACE);
      HudFont.setOnLight(Theme.LIGHT);

      for (int i = 0; i < this.texts.size(); i++) {
         String t = this.texts.get(i);
         float ry = boxY + 4.0F + i * 13.0F;
         boolean over = mx >= this.x && mx < this.x + this.w && my >= ry && my < ry + 13.0F;
         this.hovers[i] = Easing.approach(this.hovers[i], over ? 1.0F : 0.0F, 16.0F, dt);
         if (this.hovers[i] > 0.01F) {
            Ui.roundRect(this.x + 3.0F, ry, this.w - 6.0F, 13.0F, 4.0F, ColorUtil.withAlpha(Theme.TEXT, 0.05F * this.hovers[i]));
         }

         boolean quick;
         boolean hidden = (quick = this.element.hiddenByQuickRule(t))
            || this.element.rules.hidden(ScoreboardRules.scope(this.title), ScoreboardRules.normalize(t));
         drawEye(this.x + 12.0F, ry + 6.5F, !hidden, hidden ? Theme.TEXT_MUTED : accent);
         float tx = this.x + 24.0F;
         Ui.pushAlpha(hidden ? 0.4F : 1.0F);
         String shown = ScoreboardRules.normalize(t).isEmpty() ? "§7§o(blank line)" : t;
         HudFont.draw(shown, tx, ry + 2.0F, Theme.TEXT | 0xFF000000, false);
         float tw = HudFont.width(shown);
         if (hidden) {
            Ui.rect(tx, ry + 6.5F - 0.5F, tw, 1.0F, ColorUtil.withAlpha(Theme.TEXT, 0.8F));
         }

         Ui.popAlpha();
         if (quick) {
            Ui.text("quick rule", tx + tw + 6.0F, ry + 3.0F, 7.0F, Theme.TEXT_MUTED);
         }
      }

      HudFont.setOnLight(false);
   }

   private static void drawEye(float cx, float cy, boolean open, int color) {
      float w = 10.0F;
      float h = 6.0F;
      Ui.outline(cx - w / 2.0F, cy - h / 2.0F, w, h, h / 2.0F, 1.1F, color);
      Ui.circle(cx, cy, 1.6F, color);
      if (!open) {
         Ui.line(cx - 5.5F, cy + 4.0F, cx + 5.5F, cy - 4.0F, 1.2F, true, color);
      }
   }

   private int rowAt(float mx, float my) {
      float boxY = this.y + 14.0F;

      for (int i = 0; i < this.texts.size(); i++) {
         float ry = boxY + 4.0F + i * 13.0F;
         if (mx >= this.x && mx < this.x + this.w && my >= ry && my < ry + 13.0F) {
            return i;
         }
      }

      return -1;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         int i = this.rowAt(mx, my);
         if (i >= 0) {
            String t = this.texts.get(i);
            if (this.element.hiddenByQuickRule(t)) {
               ModSounds.gui(Sfx.ERROR);
               return true;
            }

            String n = ScoreboardRules.normalize(t);
            String scope = this.scope();
            boolean hidden = this.element.rules.hidden(ScoreboardRules.scope(this.title), n);
            this.element.rules.set(scope, n, !hidden);
            ModSounds.gui(hidden ? Sfx.TOGGLE_ON : Sfx.TOGGLE_OFF);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      int i = this.rowAt(mx, my);
      if (i < 0) {
         return null;
      } else {
         String t = this.texts.get(i);
         if (this.element.hiddenByQuickRule(t)) {
            return "Hidden by a quick rule below";
         } else {
            String n = ScoreboardRules.normalize(t);
            return (n.isEmpty() ? "Blank lines" : "Matches lines like \"" + n + "\"") + " (# is any number)";
         }
      }
   }
}
