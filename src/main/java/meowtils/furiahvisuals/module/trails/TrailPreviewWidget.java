package meowtils.furiahvisuals.module.trails;

import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class TrailPreviewWidget extends Widget {
   private static final String[] MOVES = new String[]{"Walk", "Sprint", "Jump", "Sneak", "Stop"};
   private static final String[] VIEWS = new String[]{"Side", "Behind", "Orbit", "Dark", "Day", "Other player"};
   private static final float RADIUS = 8.0F;
   static TrailPreviewWidget lastShown;
   private final TrailsModule module;
   private final Pill[] movePills = new Pill[MOVES.length];
   private final Pill[] viewPills = new Pill[VIEWS.length];
   private float buttonsH = 45.0F;
   private boolean dragging;
   private float lastX;
   private float lastY;
   private final TrailStyleBase[] run = new TrailStyleBase[3];

   TrailPreviewWidget(TrailsModule module) {
      this.module = module;

      for (int i = 0; i < this.movePills.length; i++) {
         this.movePills[i] = new Pill();
      }

      for (int var3 = 0; var3 < this.viewPills.length; var3++) {
         this.viewPills[var3] = new Pill();
      }

      module.previews().day = MenuMemory.previewDay(module.getId());
   }

   private float stageH() {
      return Math.max(96.0F, Math.min(230.0F, (float)Math.round(this.w * 0.42F)));
   }

   @Override
   public float height() {
      if (this.w > 0.0F) {
         this.buttonsH = this.lines(MOVES, true) * 25.0F + this.lines(VIEWS, false) * 25.0F - 5.0F;
      }

      return this.stageH() + 6.0F + this.buttonsH + 2.0F;
   }

   private float lines(String[] labels, boolean moves) {
      float tx = 0.0F;
      int n = 1;

      for (int i = 0; i < labels.length; i++) {
         float tw = Ui.width(labels[i], 8.8F) + 20.0F;
         float gap = moves || i != 3 && i != 5 ? 0.0F : 12.0F;
         if (tx > 0.0F && tx + gap + tw > this.w) {
            tx = 0.0F;
            n++;
            gap = 0.0F;
         }

         tx += gap + tw + 5.0F;
      }

      return n;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      lastShown = this;
      TrailPreviews pv = this.module.previews();
      float sx = this.x;
      float sy = this.y;
      float sw = this.w;
      float sh = this.stageH();
      if (!Ui.clippedOut(sx, sy, sw, sh)) {
         int tex = pv.drawBig(Math.max(16, Math.round(sw * Ui.px)), Math.max(16, Math.round(sh * Ui.px)));
         if (tex > 0) {
            Ui.roundedImage(tex, sx, sy, sw, sh, 8.0F, 0.0F, 1.0F, 1.0F, 0.0F, -1);
         } else {
            Ui.gradientV(sx, sy, sw, sh, 8.0F, pv.day ? -13740654 : -15723493, pv.day ? -9801604 : -15065305);
         }

         Ui.outline(sx, sy, sw, sh, 8.0F, 0.8F, 620756991);
      }

      String note = this.note();
      float ns = 7.4F;
      float nw = Ui.width(note, ns) + 12.0F;
      Ui.roundRect(sx + 6.0F, sy + 6.0F, Math.min(nw, sw - 12.0F), 13.0F, 6.5F, -1610612736);
      Ui.textMiddle(Ui.ellipsize(note, ns, sw - 24.0F, false), sx + 12.0F, sy + 12.5F, ns, -251658241, false);
      float ty = this.y + sh + 6.0F;
      ty = this.row(MOVES, this.movePills, ty, mx, my, dt, accent, true);
      ty = this.row(VIEWS, this.viewPills, ty + 20.0F + 5.0F, mx, my, dt, accent, false);
      this.buttonsH = ty + 20.0F - (this.y + sh + 6.0F);
   }

   private float row(String[] labels, Pill[] pills, float top, float mx, float my, float dt, int accent, boolean moves) {
      TrailPreviews pv = this.module.previews();
      float tx = this.x;
      float ty = top;

      for (int i = 0; i < labels.length; i++) {
         float tw = Ui.width(labels[i], 8.8F) + 20.0F;
         float gap = moves || i != 3 && i != 5 ? 0.0F : 12.0F;
         if (tx > this.x && tx + gap + tw > this.x + this.w) {
            tx = this.x;
            ty += 25.0F;
            gap = 0.0F;
         }

         float var17;
         pills[i].layout(var17 = tx + gap, ty, labels[i]);
         boolean lit = moves ? pv.big().move.ordinal() == i : (i < 3 ? pv.view.ordinal() == i : (i < 5 ? i == 4 == pv.day : pv.showOther));
         pills[i].draw(labels[i], mx, my, dt, accent, lit, true);
         tx = var17 + (tw + 5.0F);
      }

      return ty;
   }

   private String note() {
      int n = this.module.runningStyles(this.run);
      StringBuilder b = new StringBuilder();
      boolean sprintOnly = false;

      for (int k = 0; k < n; k++) {
         if (k > 0) {
            b.append(" + ");
         }

         b.append(this.run[k].getType().displayName());
         sprintOnly |= this.run[k].sprintOnly();
      }

      if (sprintOnly && this.module.previews().big().move != TrailPreview.Move.SPRINT) {
         b.append(n > 1 ? " (some only while sprinting)" : " (only while sprinting)");
      }

      if (this.module.previews().showOther) {
         b.append(this.module.otherColor.get() == TrailsModule.OtherColor.TEAM ? " · another player in the red team" : " · another player");
      }

      return this.module.isEnabled() ? b.toString() : b + " (Trails is off: this is how it would look)";
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         for (int i = 0; i < this.movePills.length; i++) {
            if (this.movePills[i].contains(mx, my)) {
               this.pressMove(i);
               ModSounds.gui(Sfx.CLICK);
               return true;
            }
         }

         for (int var5 = 0; var5 < this.viewPills.length; var5++) {
            if (this.viewPills[var5].contains(mx, my)) {
               this.pressView(var5);
               ModSounds.gui(Sfx.CLICK);
               return true;
            }
         }

         if (my < this.y + this.stageH()) {
            this.dragging = true;
            this.lastX = mx;
            this.lastY = my;
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public void mouseDrag(float mx, float my) {
      if (this.dragging) {
         TrailPreviews pv = this.module.previews();
         float dx = mx - this.lastX;
         float dy = my - this.lastY;
         if (dx != 0.0F || dy != 0.0F) {
            pv.view = TrailPreviews.View.ORBIT;
            pv.orbitYaw += dx * 0.8F;
            pv.orbitPitch = Math.max(-5.0F, Math.min(60.0F, pv.orbitPitch + dy * 0.5F));
            pv.draggedAt = ClientClock.nanos();
         }

         this.lastX = mx;
         this.lastY = my;
      }
   }

   @Override
   public void mouseUp(float mx, float my) {
      this.dragging = false;
   }

   @Override
   public boolean isDragging() {
      return this.dragging;
   }

   void pressMove(int i) {
      this.module.previews().big().move = TrailPreview.Move.values()[i];
   }

   void pressView(int i) {
      TrailPreviews pv = this.module.previews();
      if (i < 3) {
         pv.view = TrailPreviews.View.values()[i];
      } else if (i < 5) {
         pv.day = i == 4;
         MenuMemory.previewDay(this.module.getId(), pv.day);
      } else {
         pv.showOther = !pv.showOther;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      for (int i = 0; i < this.movePills.length; i++) {
         if (this.movePills[i].contains(mx, my)) {
            switch (i) {
               case 0:
                  return "The figure walks (4.3 blocks/s)";
               case 1:
                  return "The figure sprints (5.6 blocks/s): Speed Scaling makes the trail longer, wider and brighter";
               case 2:
                  return "The figure walks and jumps: each jump draws its arc";
               case 3:
                  return "The figure sneaks (only with Sneaking on under Movement, dimmer with Sneak Dim)";
               default:
                  return "The figure stops: what When I Stop does";
            }
         }
      }

      for (int var4 = 0; var4 < this.viewPills.length; var4++) {
         if (this.viewPills[var4].contains(mx, my)) {
            switch (var4) {
               case 0:
                  return "The camera sees the whole figure-eight from the side";
               case 1:
                  return "The camera follows the figure from behind, like F5";
               case 2:
                  return "The camera circles the scene (drag the preview to turn it yourself)";
               case 3:
                  return "A dark backdrop, so the trail shows clearly (a dark style's on a lighter slate)";
               case 4:
                  return "The preview by day, to check its colors in daylight";
               default:
                  return "A second figure runs along with Other Players' Color (Team color: a player of the red team)";
            }
         }
      }

      return my < this.y + this.stageH() ? "The styles that run, as they look with their settings now. Drag to turn the camera." : null;
   }
}
