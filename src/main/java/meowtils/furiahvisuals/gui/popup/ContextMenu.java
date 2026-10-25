package meowtils.furiahvisuals.gui.popup;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

public final class ContextMenu extends Popover {
   private static final float PAD = 4.0F;
   private final String title;
   private final List<ContextMenu.Item> items = new ArrayList<>();

   public ContextMenu(String title) {
      this.title = title;
   }

   public ContextMenu add(String label, String hint, boolean enabled, Runnable action) {
      this.items.add(new ContextMenu.Item(label, hint, enabled, action));
      return this;
   }

   public ContextMenu add(String label, String hint, boolean enabled, Runnable action, String tooltip) {
      ContextMenu.Item i = new ContextMenu.Item(label, hint, enabled, action);
      i.tooltip = tooltip;
      this.items.add(i);
      return this;
   }

   @Override
   public String tooltip(float mx, float my) {
      float ry = this.y + 4.0F + this.titleH();

      for (ContextMenu.Item i : this.items) {
         if (mx >= this.x && mx < this.x + this.w && my >= ry && my < ry + row()) {
            return i.tooltip;
         }

         ry += row();
      }

      return null;
   }

   private static float row() {
      return Math.max(16.0F, Density.row() - 1.0F);
   }

   private float titleH() {
      return this.title == null ? 0.0F : 15.0F;
   }

   @Override
   protected float preferredW() {
      float ts = Density.text();
      float max = this.title == null ? 0.0F : Ui.width(this.title, 7.4F);

      for (ContextMenu.Item i : this.items) {
         max = Math.max(max, Ui.width(i.label, ts) + (i.hint == null ? 0.0F : Ui.width(i.hint, 7.4F) + 14.0F));
      }

      return Math.max(110.0F, max + 24.0F);
   }

   @Override
   protected float preferredH() {
      return 8.0F + this.titleH() + this.items.size() * row();
   }

   @Override
   protected void renderContent(float mx, float my, float dt, int accent) {
      float ts = Density.text();
      if (this.title != null) {
         Ui.textMiddle(Ui.ellipsize(this.title, 7.4F, this.w - 16.0F, false), this.x + 9.0F, this.y + 4.0F + 7.5F, 7.4F, Theme.TEXT_MUTED, false);
      }

      float ry = this.y + 4.0F + this.titleH();

      for (ContextMenu.Item i : this.items) {
         boolean over = i.enabled && mx >= this.x && mx < this.x + this.w && my >= ry && my < ry + row();
         i.hover = Easing.approach(i.hover, over ? 1.0F : 0.0F, 18.0F, dt);
         if (i.hover > 0.01F) {
            Ui.roundRect(this.x + 4.0F, ry + 1.0F, this.w - 8.0F, row() - 2.0F, 5.0F, ColorUtil.withAlpha(Theme.ACCENT_SOFT, i.hover));
         }

         int color = !i.enabled ? Theme.TEXT_MUTED : ColorUtil.lerp(Theme.TEXT, accent, i.hover * 0.35F);
         Ui.textMiddle(i.label, this.x + 9.0F, ry + row() / 2.0F, ts, color, false);
         if (i.hint != null) {
            Ui.textMiddle(i.hint, this.x + this.w - 9.0F - Ui.width(i.hint, 7.4F), ry + row() / 2.0F, 7.4F, Theme.TEXT_MUTED, false);
         }

         ry += row();
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      float ry = this.y + 4.0F + this.titleH();

      for (ContextMenu.Item i : this.items) {
         if (my >= ry && my < ry + row()) {
            if (i.enabled) {
               Popovers.close(this);
               i.action.run();
            } else {
               ModSounds.gui(Sfx.ERROR);
            }

            return true;
         }

         ry += row();
      }

      return true;
   }

   public static final class Item {
      final String label;
      final String hint;
      final boolean enabled;
      final Runnable action;
      String tooltip;
      float hover;

      public Item(String label, String hint, boolean enabled, Runnable action) {
         this.label = label;
         this.hint = hint;
         this.enabled = enabled;
         this.action = action;
      }
   }
}
