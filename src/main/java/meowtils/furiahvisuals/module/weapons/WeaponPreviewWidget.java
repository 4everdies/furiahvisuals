package meowtils.furiahvisuals.module.weapons;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.ChipSlot;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class WeaponPreviewWidget extends Widget implements ChipSlot {
   private static final float HEAD = 24.0F;
   private static final float MAX_STAGE_H = 250.0F;
   private static final float RADIUS = 8.0F;
   private static final long DOUBLE_CLICK_MS = 350L;
   private static final float ICON_PILL = 26.0F;
   private static final float MIN_NAME = 70.0F;
   private static final String[] NO_LABELS = new String[]{"", "", ""};
   private float reservedRight;
   private boolean tabIcons;
   private String shownName = "";
   private String fullName = "";
   private final WeaponsModule module;
   private final ChipRow tabs = new ChipRow();
   private final ChipRow buttons = new ChipRow();
   private final String[] tabLabels = new String[]{WeaponViews.Tab.FIRST_PERSON.label, WeaponViews.Tab.THIRD_PERSON.label, WeaponViews.Tab.MODEL.label};
   private final boolean[] tabSelected = new boolean[3];
   private final List<String> actions = new ArrayList<>();
   private String[] buttonLabels = new String[0];
   private boolean[] buttonLit = new boolean[0];
   private boolean[] buttonOn = new boolean[0];
   private int dragButton = -1;
   private float lastX;
   private float lastY;
   private long lastClick;
   private boolean draggingSplit;

   WeaponPreviewWidget(WeaponsModule module) {
      this.module = module;
   }

   private WeaponViews.Stage stage() {
      return this.module.stage;
   }

   private float stageH() {
      return Math.round(Math.min(250.0F, this.w * 0.5625F));
   }

   private float stageY() {
      return this.y + 24.0F;
   }

   private void buildButtons(WeaponDef def) {
      this.actions.clear();
      ArrayList<String> labels = new ArrayList<>();
      WeaponViews.Stage st = this.stage();
      boolean canDual = !def.twoHanded || def.dualHold != null;
      switch (st.tab) {
         case FIRST_PERSON:
            this.actions.add("swing");
            labels.add("Swing");
            this.actions.add("block");
            labels.add(def.isBow() ? "Draw" : "Block");
            this.actions.add("draw");
            labels.add(def.isBow() ? "Equip" : "Draw");

            for (String inspect : def.inspects) {
               this.actions.add("inspect:" + inspect);
               labels.add(Inspects.displayName(inspect));
            }

            if (canDual) {
               this.actions.add("dual");
               labels.add("Dual Wield");
            }
            break;
         case THIRD_PERSON:
            for (WeaponViews.Stance s : WeaponViews.Stance.values()) {
               this.actions.add("stance:" + s.name());
               labels.add(s == WeaponViews.Stance.BLOCK && def.isBow() ? "Draw" : s.label);
            }

            if (!def.twoHanded) {
               this.actions.add("dual");
               labels.add("Dual Wield");
            }
            break;
         default:
            this.actions.add("reset");
            labels.add("See It Whole");
      }

      this.actions.add("compare");
      labels.add("Compare");
      this.buttonLabels = labels.toArray(new String[0]);
      if (this.buttonLit.length != this.buttonLabels.length) {
         this.buttonLit = new boolean[this.buttonLabels.length];
         this.buttonOn = new boolean[this.buttonLabels.length];
      }

      for (int i = 0; i < this.actions.size(); i++) {
         String a = this.actions.get(i);
         this.buttonOn[i] = !a.equals("compare") || !def.isVanilla();
         this.buttonLit[i] = a.equals("block") && st.blocking
            || a.equals("dual") && st.dual
            || a.equals("stance:" + st.stance.name())
            || a.equals("compare") && this.comparing(def);
      }
   }

   private boolean comparing(WeaponDef def) {
      return this.stage().compare && !def.isVanilla();
   }

   private float splitX() {
      return this.x + this.w * this.stage().split();
   }

   private static float handleR() {
      return Density.compact() ? 6.0F : 7.0F;
   }

   private boolean onSplit(float mx, float my) {
      return this.comparing(this.module.selectedDef()) && this.onStage(mx, my) && Math.abs(mx - this.splitX()) <= Math.max(handleR(), 5.0F);
   }

   @Override
   public float height() {
      WeaponDef def = this.module.selectedDef();
      this.buildButtons(def);
      this.buttons.layout(this.x, this.stageY() + this.stageH() + 8.0F, this.w, this.buttonLabels, null);
      return 24.0F + this.stageH() + 8.0F + this.buttons.height + 8.0F;
   }

   @Override
   public boolean isVisible() {
      return !this.module.editing();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      WeaponDef def = this.module.selectedDef();
      WeaponSettings s = this.module.settingsOf(def);
      WeaponViews.Stage st = this.stage();

      for (int i = 0; i < 3; i++) {
         this.tabSelected[i] = WeaponViews.Tab.values()[i] == st.tab;
      }

      float reserve = this.reservedRight > 0.0F ? this.reservedRight + 8.0F : 0.0F;
      float labelsW = 0.0F;

      for (String t : this.tabLabels) {
         labelsW += Ui.width(t, 8.8F) + 18.0F + 5.0F;
      }

      this.tabIcons = this.w - reserve - labelsW < 70.0F;
      float tabsW = this.tabIcons ? 93.0F : labelsW;
      float tx = this.x + this.w - reserve - tabsW + 5.0F;
      if (this.tabIcons) {
         this.tabs.layoutFixed(tx, this.y, 3, 26.0F);
      } else {
         this.tabs.layout(tx, this.y, tabsW, this.tabLabels, null);
      }

      this.tabs.draw(this.tabIcons ? NO_LABELS : this.tabLabels, this.tabSelected, null, null, mx, my, dt, accent);
      if (this.tabIcons) {
         for (int i = 0; i < 3; i++) {
            boolean over = mx >= this.tabs.cx[i] && mx < this.tabs.cx[i] + this.tabs.cw[i] && my >= this.tabs.cy[i] && my < this.tabs.cy[i] + 20.0F;
            int c = this.tabSelected[i] ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, over ? 1.0F : 0.0F);
            tabIcon(WeaponViews.Tab.values()[i], this.tabs.cx[i] + 13.0F, this.tabs.cy[i] + 10.0F, c);
         }
      }

      float nameRoom = tx - 5.0F - 6.0F - this.x;
      this.fullName = def.name;
      this.shownName = Ui.ellipsize(def.name, 11.0F, nameRoom, true);
      Ui.textBold(this.shownName, this.x, this.y + 1.0F, 11.0F, Theme.TEXT);
      String what = def.categoryName() + " · " + (def.twoHanded ? "two-handed" : "one-handed") + (def.isVanilla() ? " · as your resource pack draws it" : "");
      Ui.text(Ui.ellipsize(what, 7.4F, nameRoom, false), this.x, this.y + 14.0F, 7.4F, Theme.TEXT_DIM);
      float sy = this.stageY();
      float sh = this.stageH();
      int tex = -1;
      if (!Ui.clippedOut(this.x, sy, this.w, sh)) {
         tex = this.module.views().drawPreview(st, def, s, Math.round(this.w * Ui.px), Math.round(sh * Ui.px), dt);
      }

      if (tex > 0) {
         Ui.roundedImage(tex, this.x, sy, this.w, sh, 8.0F, 0.0F, 1.0F, 1.0F, 0.0F, -1);
         if (this.comparing(def)) {
            this.drawSplit(sy, sh, accent);
         }
      } else {
         Ui.gradientV(this.x, sy, this.w, sh, 8.0F, -13618113, -15592680);
         String msg = st.loading ? "Loading " + def.name + "…" : this.module.previewProblem(def);
         if (msg != null) {
            Ui.textMiddle(msg, this.x + this.w / 2.0F - Ui.width(msg, 8.4F) / 2.0F, sy + sh / 2.0F, 8.4F, -855638017, false);
         }
      }

      Ui.outline(this.x, sy, this.w, sh, 8.0F, 0.8F, 620756991);
      String hint = st.tab == WeaponViews.Tab.THIRD_PERSON
         ? "Drag to orbit · scroll to zoom"
         : (st.tab == WeaponViews.Tab.MODEL ? "Drag to turn · scroll to zoom · right-drag to move · double-click: whole" : null);
      if (hint != null) {
         float hw = Ui.width(hint, 6.6F) + 12.0F;
         Ui.roundRect(this.x + this.w - hw - 6.0F, sy + sh - 17.0F, hw, 12.0F, 6.0F, -1879048192);
         Ui.textMiddle(hint, this.x + this.w - hw, sy + sh - 11.0F, 6.6F, -855638017, false);
      }

      this.buildButtons(def);
      this.buttons.layout(this.x, sy + sh + 8.0F, this.w, this.buttonLabels, null);
      this.buttons.draw(this.buttonLabels, this.buttonLit, this.buttonOn, null, mx, my, dt, accent);
   }

   private void drawSplit(float sy, float sh, int accent) {
      WeaponViews.Stage st = this.stage();
      float sx = this.splitX();
      Ui.pushScissor(sx, sy, this.x + this.w - sx, sh);
      if (st.compareTexture > 0) {
         Ui.roundedImage(st.compareTexture, this.x, sy, this.w, sh, 8.0F, 0.0F, 1.0F, 1.0F, 0.0F, -1);
      } else {
         Ui.gradientV(this.x, sy, this.w, sh, 8.0F, -13618113, -15592680);
      }

      Ui.popScissor();
      float ts = Density.small();
      String loading = "Loading…";
      if (st.compareTexture <= 0 && st.compareLoading && this.x + this.w - sx > Ui.width(loading, ts) + 16.0F) {
         Ui.textMiddle(loading, sx + (this.x + this.w - sx - Ui.width(loading, ts)) / 2.0F, sy + sh / 2.0F, ts, -6050627, false);
      }

      drawHandle(sx, sy, sh, this.draggingSplit ? accent : -920584);
      Ui.registerControl(sx - handleR(), sy + sh / 2.0F - handleR(), handleR() * 2.0F, handleR() * 2.0F, "split");
      label("HD", this.x + 6.0F, sy + 6.0F);
      label("Pixelated", this.x + this.w - 6.0F - labelW("Pixelated"), sy + 6.0F);
   }

   private static void drawHandle(float sx, float sy, float sh, int color) {
      Ui.rect(sx - 0.75F, sy, 1.5F, sh, -920584);
      float hr = handleR();
      Ui.circle(sx, sy + sh / 2.0F, hr + 1.0F, ColorUtil.withAlpha(Theme.PREVIEW_STAGE, 0.6F));
      Ui.circle(sx, sy + sh / 2.0F, hr, color);
   }

   static float[] handleForColorCheck(float cx, float cy, float h, int accent) {
      drawHandle(cx, cy - h / 2.0F, h, accent);
      return new float[]{cx - 1.0F, cy - 1.0F, 2.0F, 2.0F};
   }

   private static float labelW(String s) {
      return Ui.width(s, Density.small()) + 12.0F;
   }

   private static void label(String s, float lx, float ly) {
      float h = Density.compact() ? 14.0F : 16.0F;
      Ui.roundRect(lx, ly, labelW(s), h, h / 2.0F, ColorUtil.withAlpha(Theme.PREVIEW_STAGE, 0.7F));
      Ui.textMiddle(s, lx + 6.0F, ly + h / 2.0F, Density.small(), -920584, false);
   }

   private boolean onStage(float mx, float my) {
      return mx >= this.x && mx < this.x + this.w && my >= this.stageY() && my < this.stageY() + this.stageH();
   }

   @Override
   public void reserveRight(float width) {
      this.reservedRight = width;
   }

   @Override
   public float[] topRow() {
      return new float[]{this.y, 20.0F};
   }

   private static void tabIcon(WeaponViews.Tab tab, float cx, float cy, int c) {
      float t = 1.0F;
      switch (tab) {
         case FIRST_PERSON:
            Ui.arc(cx, cy + 4.0F, 6.0F, t, 310.0F, 100.0F, true, c);
            Ui.arc(cx, cy - 4.0F, 6.0F, t, 130.0F, 100.0F, true, c);
            Ui.circle(cx, cy, 1.7F, c);
            break;
         case THIRD_PERSON:
            Ui.circle(cx, cy - 2.6F, 2.1F, c);
            Ui.arc(cx, cy + 5.2F, 4.6F, t, 270.0F, 180.0F, true, c);
            break;
         default:
            float r = 4.6F;
            float hx = r * 0.866F;
            float hy = r * 0.5F;
            Ui.line(cx, cy - r, cx + hx, cy - hy, t, true, c);
            Ui.line(cx + hx, cy - hy, cx + hx, cy + hy, t, true, c);
            Ui.line(cx + hx, cy + hy, cx, cy + r, t, true, c);
            Ui.line(cx, cy + r, cx - hx, cy + hy, t, true, c);
            Ui.line(cx - hx, cy + hy, cx - hx, cy - hy, t, true, c);
            Ui.line(cx - hx, cy - hy, cx, cy - r, t, true, c);
            Ui.line(cx, cy, cx, cy + r, t, true, c);
            Ui.line(cx, cy, cx - hx, cy - hy, t, true, c);
            Ui.line(cx, cy, cx + hx, cy - hy, t, true, c);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         return false;
      } else {
         WeaponViews.Stage st = this.stage();
         if (button == 0) {
            int t = this.tabs.at(mx, my);
            if (t >= 0) {
               if (st.tab != WeaponViews.Tab.values()[t]) {
                  st.tab = WeaponViews.Tab.values()[t];
                  ModSounds.gui(Sfx.SELECT);
               }

               return true;
            }

            int b = this.buttons.at(mx, my);
            if (b >= 0 && b < this.actions.size()) {
               if (this.buttonOn[b]) {
                  this.press(this.actions.get(b));
               }

               return true;
            }

            if (this.onSplit(mx, my)) {
               this.draggingSplit = true;
               this.mouseDrag(mx, my);
               return true;
            }
         }

         if (this.onStage(mx, my) && st.tab != WeaponViews.Tab.FIRST_PERSON) {
            long now = System.currentTimeMillis();
            if (button == 0 && st.tab == WeaponViews.Tab.MODEL && now - this.lastClick < 350L) {
               resetModelView(st);
            }

            this.lastClick = button == 0 ? now : this.lastClick;
            this.dragButton = button;
            this.lastX = mx;
            this.lastY = my;
            return true;
         } else {
            if (this.onStage(mx, my) && button == 0) {
               this.press("swing");
            }

            return true;
         }
      }
   }

   private void press(String action) {
      WeaponViews.Stage st = this.stage();
      WeaponDef def = this.module.selectedDef();
      WeaponViews views = this.module.views();
      if (action.equals("swing")) {
         views.previewSwing(st, def);
      } else if (action.equals("block")) {
         st.blocking = !st.blocking;
      } else if (action.equals("draw")) {
         views.previewDraw(st, def);
      } else if (action.equals("dual")) {
         st.dual = !st.dual;
      } else if (action.equals("compare")) {
         st.compare = !st.compare;
      } else if (action.equals("reset")) {
         resetModelView(st);
      } else if (action.startsWith("stance:")) {
         st.stance = WeaponViews.Stance.valueOf(action.substring(7));
      } else if (action.startsWith("inspect:") && !views.previewInspect(st, def, action.substring(8))) {
         ModSounds.gui(Sfx.ERROR);
         return;
      }

      ModSounds.gui(Sfx.CLICK);
   }

   private static void resetModelView(WeaponViews.Stage st) {
      st.yaw = 0.0F;
      st.pitch = 0.0F;
      st.zoom = 1.0F;
      st.panX = 0.0F;
      st.panY = 0.0F;
   }

   @Override
   public void mouseDrag(float mx, float my) {
      if (this.draggingSplit) {
         if (this.w > 0.0F) {
            this.stage().split((mx - this.x) / this.w);
         }
      } else if (this.dragButton >= 0) {
         WeaponViews.Stage st = this.stage();
         float dx = mx - this.lastX;
         float dy = my - this.lastY;
         this.lastX = mx;
         this.lastY = my;
         if (st.tab == WeaponViews.Tab.THIRD_PERSON) {
            st.orbitYaw += dx * 0.8F;
            st.orbitPitch = Math.max(-10.0F, Math.min(60.0F, st.orbitPitch + dy * 0.5F));
         } else if (st.tab == WeaponViews.Tab.MODEL) {
            if (this.dragButton == 1) {
               float d = st.baseDistance / Math.max(0.5F, st.zoom);
               st.panX = st.panX + dx / this.w * 2.0F * st.tanY * st.aspect * d;
               st.panY = st.panY - dy / this.stageH() * 2.0F * st.tanY * d;
            } else {
               st.yaw += dx * 0.7F;
               st.pitch = Math.max(-89.0F, Math.min(89.0F, st.pitch + dy * 0.7F));
            }
         }
      }
   }

   @Override
   public void mouseUp(float mx, float my) {
      this.dragButton = -1;
      this.draggingSplit = false;
   }

   @Override
   public boolean isDragging() {
      return this.draggingSplit;
   }

   @Override
   public boolean mouseWheel(float mx, float my, int wheel) {
      WeaponViews.Stage st = this.stage();
      if (this.onStage(mx, my) && st.tab != WeaponViews.Tab.FIRST_PERSON) {
         float steps = wheel / 120.0F;
         if (st.tab == WeaponViews.Tab.THIRD_PERSON) {
            st.distance = Math.max(1.6F, Math.min(7.0F, st.distance * (float)Math.pow(0.88, steps)));
            return true;
         } else {
            float zoom = Math.max(0.6F, Math.min(10.0F, st.zoom * (float)Math.pow(1.18, steps)));
            float ndcX = (mx - this.x) / this.w * 2.0F - 1.0F;
            float ndcY = 1.0F - (my - this.stageY()) / this.stageH() * 2.0F;
            float d1 = st.baseDistance / st.zoom;
            float d2 = st.baseDistance / zoom;
            st.panX = st.panX + ndcX * st.tanY * st.aspect * (d2 - d1);
            st.panY = st.panY + ndcY * st.tanY * (d2 - d1);
            if (zoom <= 1.001F) {
               st.panX = 0.0F;
               st.panY = 0.0F;
            }

            st.zoom = zoom;
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else {
         int t = this.tabs.at(mx, my);
         if (t >= 0) {
            String name = this.tabIcons ? this.tabLabels[t] + ": " : "";
            switch (WeaponViews.Tab.values()[t]) {
               case FIRST_PERSON:
                  return name + "Your view model as the game draws it, idling (the buttons below play its moves)";
               case THIRD_PERSON:
                  return name + "Your skin holding it, as others see you";
               default:
                  return name + "The weapon alone, to look at up close";
            }
         } else if (!this.shownName.equals(this.fullName)
            && my >= this.y
            && my < this.y + 13.0F
            && mx >= this.x
            && mx < this.x + Ui.widthBold(this.shownName, 11.0F)) {
            return this.fullName;
         } else {
            int b = this.buttons.at(mx, my);
            if (b >= 0 && b < this.actions.size()) {
               String a = this.actions.get(b);
               if (a.equals("swing")) {
                  return "Plays a swing (the preview's: nothing is sent to the game)";
               }

               boolean bow = this.module.selectedDef().isBow();
               if (a.equals("block")) {
                  return bow ? "Draws the string as vanilla times it, and holds it (click again to loose)" : "Holds the guard up (click again to lower it)";
               }

               if (a.equals("draw")) {
                  return bow ? "Plays the equip: the bow swings up into the hold" : "Plays the draw";
               }

               if (a.equals("stance:BLOCK") && bow) {
                  return "Draws and looses, again and again";
               }

               if (a.equals("dual")) {
                  return "Shows it dual-wielded in the preview (the section's Dual Wield setting is under Hands)";
               }

               if (a.equals("compare")) {
                  return this.module.selectedDef().isVanilla()
                     ? "Vanilla is your pack's item: it has no styles to compare"
                     : "Splits the preview: the weapon HD left of a line you can drag, Pixelated right of it";
               }

               if (a.startsWith("inspect:")) {
                  return "Plays this inspect";
               }
            }

            if (this.onSplit(mx, my)) {
               return "Drag the line: HD on its left, Pixelated on its right";
            } else {
               return this.onStage(mx, my) && this.stage().tab == WeaponViews.Tab.FIRST_PERSON ? "Click to swing" : null;
            }
         }
      }
   }
}
