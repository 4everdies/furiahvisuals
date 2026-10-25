package meowtils.furiahvisuals.gui;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import meowtils.furiahvisuals.gui.popup.Popover;
import meowtils.furiahvisuals.gui.popup.Popovers;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class SectionsPopover extends Popover {
   private static final float PAD = 5.0F;
   private final SectionsPopover.Host host;
   private final List<Object> rows = new ArrayList<>();
   private final Map<Module, float[]> anim = new IdentityHashMap<>();

   SectionsPopover(SectionsPopover.Host host, List<Module> modules) {
      this.host = host;

      for (String category : SectionCategories.NAMES) {
         List<Module> in = SectionCategories.in(category, modules);
         if (!in.isEmpty()) {
            this.rows.add(category);
            this.rows.addAll(in);
         }
      }
   }

   private static float row() {
      return Math.max(17.0F, Density.row());
   }

   private static float titleH() {
      return 13.0F;
   }

   @Override
   protected float preferredW() {
      return 210.0F;
   }

   @Override
   protected float preferredH() {
      float h = 10.0F;

      for (Object r : this.rows) {
         h += r instanceof Module ? row() : titleH();
      }

      return h;
   }

   private float[] anim(Module m) {
      float[] a = this.anim.get(m);
      if (a == null) {
         a = new float[]{0.0F, 0.0F};
         this.anim.put(m, a);
      }

      return a;
   }

   private float dotX() {
      return this.x + this.w - 5.0F - 10.0F;
   }

   private boolean onDot(float mx, float my, float ry) {
      return Math.abs(mx - this.dotX()) <= 8.0F && my >= ry && my < ry + row();
   }

   @Override
   protected void renderContent(float mx, float my, float dt, int accent) {
      float ry = this.y + 5.0F;
      float ts = Density.text();
      float small = Density.small();
      Module shown = this.host.shown();

      for (Object r : this.rows) {
         if (!(r instanceof Module)) {
            String t = ((String)r).toUpperCase(Locale.ROOT);
            Ui.textMiddle(t, this.x + 5.0F + 6.0F, ry + titleH() / 2.0F + 1.0F, 6.6F, Theme.TEXT_MUTED, true);
            ry += titleH();
         } else {
            Module m = (Module)r;
            float rh = row();
            float cy = ry + rh / 2.0F;
            boolean over = mx >= this.x && mx < this.x + this.w && my >= ry && my < ry + rh;
            boolean dot = over && this.onDot(mx, my, ry);
            float[] a = this.anim(m);
            a[0] = Easing.approach(a[0], over && !dot ? 1.0F : 0.0F, 18.0F, dt);
            a[1] = Easing.approach(a[1], dot ? 1.0F : 0.0F, 18.0F, dt);
            boolean sel = m == shown;
            if (sel) {
               Ui.roundRect(this.x + 5.0F, ry + 1.0F, this.w - 10.0F, rh - 2.0F, 5.0F, Theme.ACCENT_SOFT);
            } else if (a[0] > 0.01F) {
               Ui.roundRect(this.x + 5.0F, ry + 1.0F, this.w - 10.0F, rh - 2.0F, 5.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, a[0]));
            }

            MenuIcons.section(
               m.getId(),
               this.x + 5.0F + 11.0F,
               cy,
               Math.min(10.0F, rh * 0.5F),
               sel ? accent : ColorUtil.lerp(Theme.TEXT_MUTED, Theme.TEXT_DIM, 0.4F + 0.6F * a[0])
            );
            float tx = this.x + 5.0F + 22.0F;
            float room = this.dotX() - 10.0F - tx;
            String name = Ui.ellipsize(m.getName(), ts, room * 0.62F, sel);
            Ui.textMiddle(name, tx, cy, ts, sel ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, a[0]), sel);
            String summary = m.summary();
            if (summary != null) {
               float sx = tx + (sel ? Ui.widthBold(name, ts) : Ui.width(name, ts)) + 6.0F;
               Ui.textMiddle(Ui.ellipsize(summary, small, this.dotX() - 10.0F - sx, false), sx, cy, small, Theme.TEXT_MUTED, false);
            }

            FuriahScreen.navDot(this.dotX(), cy, m.isEnabled(), a[1], accent, Density.compact() ? 2.4F : 2.8F);
            ry += rh;
         }
      }
   }

   private Object[] at(float mx, float my) {
      if (!(mx < this.x) && !(mx >= this.x + this.w)) {
         float ry = this.y + 5.0F;

         for (Object r : this.rows) {
            float rh = r instanceof Module ? row() : titleH();
            if (r instanceof Module && my >= ry && my < ry + rh) {
               return new Object[]{r, this.onDot(mx, my, ry)};
            }

            ry += rh;
         }

         return null;
      } else {
         return null;
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      Object[] hit = this.at(mx, my);
      if (hit != null && button == 0) {
         Module m = (Module)hit[0];
         if ((Boolean)hit[1]) {
            m.setEnabled(!m.isEnabled());
            ModSounds.toggle(m.isEnabled());
            return true;
         } else {
            if (m != this.host.shown()) {
               ModSounds.gui(Sfx.SECTION);
            }

            Popovers.close(this);
            this.host.open(m);
            return true;
         }
      } else {
         return true;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      Object[] hit = this.at(mx, my);
      if (hit == null) {
         return null;
      } else {
         Module m = (Module)hit[0];
         return ((Boolean)hit[1]).booleanValue() ? "Turn " + m.getName() + (m.isEnabled() ? " off" : " on") : m.getDescription();
      }
   }

   float[] rowForTest(String id, boolean dot) {
      float ry = this.y + 5.0F;

      for (Object r : this.rows) {
         float rh = r instanceof Module ? row() : titleH();
         if (r instanceof Module && ((Module)r).getId().equals(id)) {
            return new float[]{dot ? this.dotX() : this.x + 5.0F + 40.0F, ry + rh / 2.0F};
         }

         ry += rh;
      }

      return null;
   }

   interface Host {
      void open(Module var1);

      Module shown();
   }
}
