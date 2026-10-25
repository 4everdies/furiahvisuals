package meowtils.furiahvisuals.module.watermark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import meowtils.furiahvisuals.gui.MenuNav;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.search.SearchList;
import meowtils.furiahvisuals.gui.widget.ToggleWidget;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class ModuleListWidget extends Widget implements SearchList {
   private static final float ROW = 30.0F;
   private static final float INDENT = 26.0F;
   private static final float GAP = 3.0F;
   private final WaterMarkModule wm;
   private final Map<IslandItem, ModuleListWidget.Row> rows = new HashMap<>();
   private final List<IslandItem> view = new ArrayList<>();
   private IslandItem dragging;
   private float dragY;
   private float grab;
   private int dragFrom = -1;
   private int dragTo = -1;
   private String hoverTip;
   private Set<Object> filter;

   private boolean shows(IslandItem item) {
      return this.filter == null || this.filter.contains(item);
   }

   ModuleListWidget(WaterMarkModule wm) {
      this.wm = wm;
   }

   private ModuleListWidget.Row row(IslandItem item) {
      ModuleListWidget.Row r = this.rows.get(item);
      if (r == null) {
         r = new ModuleListWidget.Row();
         this.rows.put(item, r);
      }

      return r;
   }

   private static String pageKey(IslandItem item) {
      return "module:" + item.id;
   }

   private boolean hasPage(IslandItem item) {
      return MenuNav.hasPage(this.wm, pageKey(item));
   }

   private void openPage(IslandItem item) {
      MenuNav.openPage(this.wm, pageKey(item));
   }

   @Override
   public float height() {
      float h = this.shows(this.wm.fv) ? 33.0F : 0.0F;
      List<IslandItem> middle = this.wm.middleItems();

      for (int i = 0; i < middle.size(); i++) {
         if (this.shows(middle.get(i))) {
            h += 33.0F;
         }
      }

      return h + (this.shows(this.wm.gear) ? 33.0F : 0.0F);
   }

   private List<IslandItem> viewOrder() {
      this.view.clear();
      List<IslandItem> middle = this.wm.middleItems();
      this.view.addAll(middle);
      if (this.dragging != null && this.dragFrom >= 0 && this.dragTo >= 0 && this.dragFrom < this.view.size() && this.dragTo < this.view.size()) {
         this.view.add(this.dragTo, this.view.remove(this.dragFrom));
      }

      return this.view;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.hoverTip = null;
      String expand = this.wm.takeExpandRequest();
      IslandItem item;
      if (expand != null && (item = this.wm.find(expand)) != null) {
         MenuNav.openPage(this.wm, pageKey(item));
      }

      boolean dragActive = this.dragging != null;
      float yy = this.y;
      if (this.shows(this.wm.fv)) {
         yy = this.layoutRow(this.wm.fv, yy, dt);
      }

      List<IslandItem> order = this.viewOrder();

      for (int i = 0; i < order.size(); i++) {
         if (this.shows(order.get(i))) {
            yy = this.layoutRow(order.get(i), yy, dt);
         }
      }

      if (this.shows(this.wm.gear)) {
         yy = this.layoutRow(this.wm.gear, yy, dt);
      }

      float lastHeight = yy - this.y;
      if (this.shows(this.wm.fv)) {
         this.drawRow(this.wm.fv, mx, my, dt, accent, false, dragActive);
      }

      for (int ix = 0; ix < order.size(); ix++) {
         if (order.get(ix) != this.dragging && this.shows(order.get(ix))) {
            this.drawRow(order.get(ix), mx, my, dt, accent, false, dragActive);
         }
      }

      if (this.shows(this.wm.gear)) {
         this.drawRow(this.wm.gear, mx, my, dt, accent, false, dragActive);
      }

      if (this.dragging != null) {
         ModuleListWidget.Row r = this.row(this.dragging);
         r.shownY = Math.max(this.y, Math.min(this.y + lastHeight - 30.0F, this.dragY));
         this.drawRow(this.dragging, mx, my, dt, accent, true, dragActive);
      }
   }

   private float layoutRow(IslandItem item, float top, float dt) {
      ModuleListWidget.Row r = this.row(item);
      r.y = top;
      float f = r.shownY = Float.isNaN(r.shownY) ? top : Easing.approach(r.shownY, top, 18.0F, dt);
      if (Math.abs(r.shownY - top) < 0.05F) {
         r.shownY = top;
      }

      return top + 30.0F + 3.0F;
   }

   private void drawRow(IslandItem item, float mx, float my, float dt, int accent, boolean lifted, boolean dragActive) {
      ModuleListWidget.Row r = this.row(item);
      float ry = r.shownY;
      boolean pinned = item == this.wm.fv || item == this.wm.gear;
      boolean over = !lifted && !dragActive && mx >= this.x - 6.0F && mx < this.x + this.w + 6.0F && my >= ry && my < ry + 30.0F;
      r.hover = Easing.approach(r.hover, !over && !lifted ? 0.0F : 1.0F, 16.0F, dt);
      if (lifted) {
         Ui.shadow(this.x - 6.0F, ry + 3.0F, this.w + 12.0F, 30.0F, 8.0F, 12.0F, Theme.LIGHT ? 855638016 : 1996488704);
         Ui.roundRect(this.x - 6.0F, ry, this.w + 12.0F, 30.0F, 8.0F, Theme.LIGHT ? -1 : -14933975);
         Ui.accentOutline(this.x - 6.0F, ry, this.w + 12.0F, 30.0F, 8.0F, 0.9F, accent);
      } else if (r.hover > 0.01F) {
         Ui.roundRect(this.x - 6.0F, ry, this.w + 12.0F, 30.0F, 8.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, r.hover));
      }

      float hx = this.x + 2.0F;
      float hy = ry + 15.0F;
      if (!pinned) {
         boolean overHandle = over && mx < this.x + 16.0F;
         int hc = ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT, !overHandle && !lifted ? 0.0F : 1.0F);

         for (int col = 0; col < 2; col++) {
            for (int k = -1; k <= 1; k++) {
               Ui.circle(hx + 2.0F + col * 4.5F, hy + k * 4.5F, 1.2F, hc);
            }
         }

         if (overHandle) {
            this.hoverTip = "Drag to move " + item.name;
         }
      }

      float ix = this.x + 18.0F;
      boolean on = this.wm.isShown(item);
      int textColor = on ? Theme.TEXT : Theme.TEXT_DIM;
      IslandIcons.Icon rowIcon = item.iconChoice != null && item.iconChoice.get() != null ? item.iconChoice.get() : item.listIcon;
      if (item == this.wm.fv) {
         Ui.textMiddleHeavy("FV", ix, hy, 9.6F, accent | 0xFF000000);
      } else if (rowIcon != null) {
         float s = 12.0F;
         Ui.image(
            IslandIcons.texture(),
            ix,
            hy - s / 2.0F,
            s,
            s,
            IslandIcons.u0(rowIcon),
            IslandIcons.v0(rowIcon),
            IslandIcons.u0(rowIcon) + IslandIcons.du(),
            IslandIcons.v0(rowIcon) + IslandIcons.dv(),
            on ? accent : Theme.TEXT_MUTED
         );
      } else {
         Ui.textMiddle("Aa", ix - 0.5F, hy, 8.4F, on ? accent : Theme.TEXT_MUTED, true);
      }

      float nx = ix + 26.0F - 6.0F;
      String name = item == this.wm.fv ? "Furiah's Visuals" : item.name;
      Ui.textMiddle(name, nx, hy, 10.0F, textColor, false);
      String note = this.note(item);
      float noteX = nx + Ui.width(name, 10.0F) + 8.0F;
      boolean page = this.hasPage(item);
      float rightEdge = this.x + this.w - 36.0F - (page ? 16.0F : 0.0F);
      if (note != null) {
         Ui.textMiddle(Ui.ellipsize(note, 8.0F, rightEdge - noteX, false), noteX, hy, 8.0F, Theme.TEXT_MUTED, false);
      }

      if (page) {
         chevron(this.x + this.w - 48.0F + Easing.outCubic(r.hover) * 1.5F, hy, ColorUtil.lerp(Theme.TEXT_MUTED, accent, 0.3F + 0.7F * r.hover));
      }

      if (item != this.wm.fv) {
         float target = on ? 1.0F : 0.0F;
         r.knob = r.knob < 0.0F ? target : Easing.approach(r.knob, target, 18.0F, dt);
         ToggleWidget.drawSwitch(this.x + this.w - 28.0F, hy - 7.5F, 28.0F, 15.0F, r.knob, accent);
      }

      if (over && this.hoverTip == null) {
         this.hoverTip = item.description + (page ? "  (click for its settings)" : "");
      }
   }

   private static void chevron(float cx, float cy, int color) {
      for (int k = 0; k <= 5; k++) {
         float f = k / 5.0F;
         Ui.circle(cx - 1.8F + f * 3.6F, cy - 3.6F + f * 3.6F, 0.75F, color);
         Ui.circle(cx - 1.8F + f * 3.6F, cy + 3.6F - f * 3.6F, 0.75F, color);
      }
   }

   private String note(IslandItem item) {
      if (item == this.wm.fv) {
         return "Always first";
      } else if (item == this.wm.gear) {
         return "Always last · quick settings with chat open";
      } else {
         return item == this.wm.ping && !item.available() ? "shows in multiplayer" : null;
      }
   }

   private IslandItem rowAt(float my) {
      if (this.hitRow(this.wm.fv, my)) {
         return this.wm.fv;
      } else {
         for (IslandItem item : this.wm.middleItems()) {
            if (this.hitRow(item, my)) {
               return item;
            }
         }

         return this.hitRow(this.wm.gear, my) ? this.wm.gear : null;
      }
   }

   private boolean hitRow(IslandItem item, float my) {
      ModuleListWidget.Row r = this.rows.get(item);
      return r != null && this.shows(item) && my >= r.shownY && my < r.shownY + 30.0F;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         return false;
      } else {
         IslandItem item = this.rowAt(my);
         if (item != null && button == 0) {
            ModuleListWidget.Row r = this.row(item);
            boolean pinned = item == this.wm.fv || item == this.wm.gear;
            if (item != this.wm.fv && mx >= this.x + this.w - 34.0F) {
               item.shown.toggle();
               ModSounds.toggle(item.shown.get());
               return true;
            } else if (!pinned && mx < this.x + 16.0F && this.filter == null) {
               List<IslandItem> middle = this.wm.middleItems();
               this.dragging = item;
               this.dragTo = this.dragFrom = middle.indexOf(item);
               this.grab = my - r.shownY;
               this.dragY = r.shownY;
               return true;
            } else {
               if (this.hasPage(item)) {
                  this.openPage(item);
               }

               return true;
            }
         } else {
            return item != null;
         }
      }
   }

   @Override
   public void mouseDrag(float mx, float my) {
      if (this.dragging != null) {
         this.dragY = my - this.grab;
         List<IslandItem> middle = this.wm.middleItems();
         float center = this.dragY + 15.0F;
         int to = 0;

         for (int i = 0; i < middle.size(); i++) {
            IslandItem other = middle.get(i);
            ModuleListWidget.Row r;
            if (other != this.dragging && (r = this.rows.get(other)) != null && r.y + 15.0F < center) {
               to++;
            }
         }

         this.dragTo = Math.max(0, Math.min(middle.size() - 1, to));
      }
   }

   @Override
   public void mouseUp(float mx, float my) {
      if (this.dragging != null) {
         if (this.dragFrom >= 0 && this.dragTo >= 0 && this.dragFrom != this.dragTo) {
            this.wm.moveItem(this.dragFrom, this.dragTo);
            ModSounds.gui(Sfx.SELECT);
         }

         this.dragging = null;
         this.dragTo = -1;
         this.dragFrom = -1;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      return this.contains(mx, my) ? this.hoverTip : null;
   }

   private List<IslandItem> allItems() {
      ArrayList<IslandItem> all = new ArrayList<>();
      all.add(this.wm.fv);
      all.addAll(this.wm.middleItems());
      all.add(this.wm.gear);
      return all;
   }

   @Override
   public void searchItems(SearchList.Items out) {
      for (IslandItem item : this.allItems()) {
         if (item == this.wm.fv) {
            out.item(item, "Furiah's Visuals", item.description, "FV");
         } else {
            out.item(item, item.name, item.description);
         }
      }
   }

   @Override
   public void searchFilter(Set<Object> keys) {
      this.filter = keys;
   }

   @Override
   public float reveal(Object key) {
      if (!(key instanceof IslandItem)) {
         return -1.0F;
      } else {
         float offset = 0.0F;

         for (IslandItem item : this.allItems()) {
            if (item == key) {
               return offset;
            }

            offset += 33.0F;
         }

         return -1.0F;
      }
   }

   @Override
   public float revealHeight(Object key) {
      return 30.0F;
   }

   @Override
   public int searchVersion() {
      return 0;
   }

   private static final class Row {
      float shownY = Float.NaN;
      float knob = -1.0F;
      float hover;
      float y;

      private Row() {
      }
   }
}
