package meowtils.furiahvisuals.gui.widget;

import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.MenuMemory;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

public final class HeaderWidget extends Widget {
   private final String title;
   private final List<Setting> members;
   private final String key;
   private final boolean collapsible;
   private final boolean collapsedByDefault;
   private boolean countInTitle;
   private boolean styleInTitle;
   private float turn = -1.0F;

   public HeaderWidget(String title, List<Setting> members) {
      this(title, members, title, false, false);
   }

   public HeaderWidget(String title, List<Setting> members, String key, boolean collapsible, boolean collapsedByDefault) {
      this.title = title.toUpperCase(Locale.ROOT);
      this.members = members;
      this.key = key;
      this.collapsible = collapsible;
      this.collapsedByDefault = collapsedByDefault;
   }

   public HeaderWidget countInTitle() {
      this.countInTitle = true;
      return this;
   }

   public HeaderWidget styleInTitle() {
      this.styleInTitle = true;
      return this;
   }

   public String titleForTest() {
      return this.title;
   }

   private String suffix() {
      if (this.countInTitle) {
         return " (" + this.count() + ")";
      } else {
         if (this.styleInTitle) {
            for (Setting s : this.members) {
               if (s.getOwner() != null && s.isVisible()) {
                  return "  ·  " + s.getOwner().styleName().toUpperCase(Locale.ROOT);
               }
            }
         }

         return "";
      }
   }

   public boolean isCollapsed() {
      if (!this.collapsible) {
         return false;
      } else {
         Boolean c = MenuMemory.folded(this.key);
         return c == null ? this.collapsedByDefault : c;
      }
   }

   public void setCollapsed(boolean collapsed) {
      if (this.collapsible) {
         MenuMemory.folded(this.key, collapsed);
      }
   }

   @Override
   public float height() {
      return Density.header();
   }

   @Override
   public boolean isVisible() {
      for (Setting s : this.members) {
         if (s.isVisible()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float hh = this.height();
      float ty = this.y + hh / 2.0F - 2.0F;
      float ts = Density.headerText();
      boolean folded = this.isCollapsed();
      if (this.collapsible) {
         this.animateHover(this.contains(mx, my), dt);
         this.turn = this.turn < 0.0F ? (folded ? 0.0F : 1.0F) : Easing.approach(this.turn, folded ? 0.0F : 1.0F, 16.0F, dt);
         if (this.hover > 0.01F) {
            Ui.roundRect(this.x - 6.0F, this.y + 2.0F, this.w + 12.0F, hh - 4.0F, 6.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, this.hover));
         }
      }

      if (this.collapsible) {
         Ui.registerControl(this.x - 6.0F, this.y + 2.0F, this.w + 12.0F, hh - 4.0F, "group " + this.title);
      }

      float dot = ts / 8.2F;
      Ui.glow(this.x + 3.0F, ty + 0.5F, 14.0F * dot, ColorUtil.withAlpha(accent, 0.35F));
      Ui.circle(this.x + 3.0F, ty + 0.5F, 2.2F * dot, accent | 0xFF000000);
      int textColor = this.collapsible ? ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.hover) : Theme.TEXT_DIM;
      Ui.textMiddle(this.title, this.x + 11.0F, ty, ts, textColor, true);
      float tw = Ui.widthBold(this.title, ts);
      String suffix = this.suffix();
      if (!suffix.isEmpty()) {
         Ui.textMiddle(suffix, this.x + 11.0F + tw, ty, ts, Theme.TEXT_MUTED, true);
         tw += Ui.widthBold(suffix, ts);
      }

      float lineEnd = this.x + this.w;
      if (this.collapsible) {
         chevron(this.x + this.w - 5.0F, ty + 0.5F, 3.2F, this.turn, ColorUtil.lerp(Theme.TEXT_MUTED, accent, 0.4F + 0.6F * this.hover));
         lineEnd -= 16.0F;
         if (folded && !this.countInTitle) {
            String n = this.count() + (this.count() == 1 ? " setting" : " settings");
            float nw = Ui.width(n, 7.6F);
            Ui.textMiddle(n, lineEnd - nw, ty, 7.6F, Theme.TEXT_MUTED, false);
            lineEnd -= nw + 8.0F;
         }
      }

      Ui.rect(this.x + 17.0F + tw, ty, Math.max(0.0F, lineEnd - (this.x + 17.0F + tw)), 0.6F, Theme.DIVIDER);
   }

   private int count() {
      int n = 0;

      for (Setting s : this.members) {
         if (s.isVisible()) {
            n++;
         }
      }

      return n;
   }

   private static void chevron(float cx, float cy, float size, float t, int color) {
      double angle = t * Math.PI / 2.0;
      double dx = Math.cos(angle);
      double dy = Math.sin(angle);

      for (int i = 0; i <= 6; i++) {
         float k = i / 6.0F;
         double along = size * 0.55 - k * size * 1.1 * 0.7071;
         double side = k * size * 1.1 * 0.7071;

         for (int s = -1; s <= 1; s += 2) {
            float px = (float)(cx + dx * along - dy * side * s);
            float py = (float)(cy + dy * along + dx * side * s);
            Ui.circle(px, py, 0.75F, color);
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (this.collapsible && button == 0 && this.contains(mx, my)) {
         this.setCollapsed(!this.isCollapsed());
         ModSounds.gui(this.isCollapsed() ? Sfx.CLICK : Sfx.DROPDOWN);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      return this.collapsible && this.contains(mx, my) ? (this.isCollapsed() ? "Click to show these settings" : "Click to fold these settings away") : null;
   }
}
