package meowtils.furiahvisuals.module.ui;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;

final class QuickButtonsWidget extends Widget {
   private static final float H = 22.0F;
   private static final float TEXT = 9.0F;
   private static final String APPLY = "Apply mod theme to all";
   private static final String BACK = "Back to vanilla";
   private static final String MOVE = "Move HUD";
   private final UiModule module;
   private float hoverApply;
   private float hoverBack;
   private float hoverMove;
   private float applyX;
   private float applyW;
   private float backX;
   private float backW;
   private float moveX;
   private float moveW;

   QuickButtonsWidget(UiModule module) {
      this.module = module;
   }

   @Override
   public float height() {
      return 32.0F;
   }

   private void layout() {
      this.applyX = this.x;
      this.applyW = Ui.width("Apply mod theme to all", 9.0F) + 26.0F;
      this.backX = this.applyX + this.applyW + 6.0F;
      this.backW = Ui.width("Back to vanilla", 9.0F) + 26.0F;
      this.moveW = Ui.width("Move HUD", 9.0F) + 26.0F;
      this.moveX = this.x + this.w - this.moveW;
   }

   private static boolean inWorld() {
      return Minecraft.getMinecraft().theWorld != null;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.layout();
      float by = this.y + 3.0F;
      boolean overApply = mx >= this.applyX && mx < this.applyX + this.applyW && my >= by && my < by + 22.0F;
      boolean overBack = mx >= this.backX && mx < this.backX + this.backW && my >= by && my < by + 22.0F;
      boolean overMove = mx >= this.moveX && mx < this.moveX + this.moveW && my >= by && my < by + 22.0F;
      this.hoverApply = Easing.approach(this.hoverApply, overApply ? 1.0F : 0.0F, 16.0F, dt);
      this.hoverBack = Easing.approach(this.hoverBack, overBack ? 1.0F : 0.0F, 16.0F, dt);
      this.hoverMove = Easing.approach(this.hoverMove, overMove && inWorld() ? 1.0F : 0.0F, 16.0F, dt);
      Ui.roundRect(this.applyX, by, this.applyW, 22.0F, 11.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, this.hoverApply));
      Ui.accentOutline(this.applyX, by, this.applyW, 22.0F, 11.0F, 0.9F, accent);
      Ui.textMiddle("Apply mod theme to all", this.applyX + 13.0F, by + 11.0F, 9.0F, Theme.TEXT, true);
      Ui.surface(this.backX, by, this.backW, 22.0F, 11.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hoverBack));
      Ui.textMiddle("Back to vanilla", this.backX + 13.0F, by + 11.0F, 9.0F, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hoverBack), false);
      float moveA = inWorld() ? 1.0F : 0.45F;
      Ui.pushAlpha(moveA);
      Ui.surface(this.moveX, by, this.moveW, 22.0F, 11.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.hoverMove));
      Ui.outline(this.moveX, by, this.moveW, 22.0F, 11.0F, 0.9F, ColorUtil.lerp(Theme.ACCENT_LINE, Theme.ACCENT, this.hoverMove));
      Ui.textMiddle("Move HUD", this.moveX + 13.0F, by + 11.0F, 9.0F, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hoverMove), false);
      Ui.popAlpha();
      int on = this.module.onCount();
      int all = this.module.sections().size();
      String state = on == 0 ? "All vanilla" : (on == all ? "All " + all + " restyled" : on + " of " + all + " restyled");
      float stateX = this.backX + this.backW + 10.0F;
      if (stateX + Ui.width(state, 8.5F) < this.moveX - 8.0F) {
         Ui.textMiddle(state, stateX, by + 11.0F, 8.5F, Theme.TEXT_MUTED, false);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         this.layout();
         float by = this.y + 3.0F;
         if (my >= by && my < by + 22.0F) {
            if (mx >= this.applyX && mx < this.applyX + this.applyW) {
               this.module.applyThemeToAll();
            } else if (mx >= this.backX && mx < this.backX + this.backW) {
               this.module.backToVanilla();
            } else if (mx >= this.moveX && mx < this.moveX + this.moveW) {
               if (inWorld()) {
                  this.module.openMoveHud();
               } else {
                  ModSounds.gui(Sfx.ERROR);
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      this.layout();
      float by = this.y + 3.0F;
      if (my < by || my >= by + 22.0F) {
         return null;
      } else if (mx >= this.applyX && mx < this.applyX + this.applyW) {
         return "Turns every mini section on, in the mod's themed style (the island's look)";
      } else if (mx >= this.backX && mx < this.backX + this.backW) {
         return "Turns every mini section off: the game looks exactly as vanilla (your settings stay)";
      } else if (mx >= this.moveX && mx < this.moveX + this.moveW) {
         return inWorld()
            ? "Place the scoreboard, chat, hotbar and island: drag, scroll to resize, right-click to reset"
            : "Join a world first: Move HUD places the HUD over the game";
      } else {
         return null;
      }
   }
}
