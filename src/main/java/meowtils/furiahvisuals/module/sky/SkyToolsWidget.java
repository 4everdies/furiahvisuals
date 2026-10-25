package meowtils.furiahvisuals.module.sky;

import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class SkyToolsWidget extends Widget {
   private static final float ROW = 26.0F;
   private static final float CHIP_H = 18.0F;
   private static final float CHIP_GAP = 5.0F;
   private static final float MIN_CHIP_W = 112.0F;
   private static final float LABEL = 8.8F;
   private static final long ARMED_MS = 3000L;
   private static final long MESSAGE_MS = 4000L;
   private static final int APPLY = 1;
   private static final int RESET = 2;
   private final SkyModule module;
   private final Pill copyPill = new Pill();
   private final Pill allPill = new Pill();
   private final Pill resetPill = new Pill();
   private final SkyStyle[] values = SkyStyle.values();
   private final float[] chipHover = new float[this.values.length];
   private boolean picking;
   private float pickOpen;
   private int armed;
   private long armedUntil;
   private String message;
   private long messageAt;
   private float rowH = 26.0F;

   SkyToolsWidget(SkyModule module) {
      this.module = module;
   }

   void showForTest(boolean copyList, boolean armApply) {
      this.picking = copyList;
      this.armed = armApply ? 1 : 0;
      this.armedUntil = System.currentTimeMillis() + 60000L;
   }

   private int columns() {
      return Math.max(2, Math.min(5, (int)((this.w + 5.0F) / 117.0F)));
   }

   private int chipRows() {
      int cols = this.columns();
      return (this.values.length - 1 + cols - 1) / cols;
   }

   private float chipsHeight() {
      return this.chipRows() * 23.0F + 4.0F;
   }

   private boolean showsMessage() {
      return this.message != null && ClientClock.millis() - this.messageAt < 4000L;
   }

   @Override
   public float height() {
      return this.rowH + this.chipsHeight() * this.pickOpen + (this.showsMessage() ? 13.0F : 0.0F) + 4.0F;
   }

   private String copyLabel() {
      return this.picking ? "Close" : "Copy from…";
   }

   private String allLabel() {
      return this.armed == 1 ? "Click again: all " + this.values.length + " skies" : "Apply to all skies";
   }

   private String resetLabel() {
      return this.armed == 2 ? "Click again to reset" : "Reset this sky";
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.module.tools = this;
      long now = System.currentTimeMillis();
      if (this.armed != 0 && now > this.armedUntil) {
         this.armed = 0;
      }

      this.pickOpen = Easing.approach(this.pickOpen, this.picking ? 1.0F : 0.0F, 14.0F, dt);
      SkyStyle sky = this.module.style.get();
      String c = this.copyLabel();
      String a = this.allLabel();
      String r = this.resetLabel();
      float cw = Ui.width(c, 8.8F) + 20.0F;
      float aw = Ui.width(a, 8.8F) + 20.0F;
      float rw = Ui.width(r, 8.8F) + 20.0F;
      float buttons = cw + aw + rw + 12.0F;
      String lead = "Settings of ";
      float labelW = Ui.width(lead, 8.8F) + Ui.widthBold(sky.displayName(), 8.8F) + 14.0F;
      boolean wrap = labelW + buttons + 10.0F > this.w;
      float by = wrap ? this.y + 26.0F : this.y + 2.0F;
      float bx = wrap ? this.x : this.x + this.w - buttons;
      Pill[] pills = new Pill[]{this.copyPill, this.allPill, this.resetPill};
      String[] labels = new String[]{c, a, r};
      float[] widths = new float[]{cw, aw, rw};
      float px = bx;
      float py = by;

      for (int i = 0; i < 3; i++) {
         if (wrap && px > this.x && px + widths[i] > this.x + this.w) {
            px = this.x;
            py += 24.0F;
         }

         pills[i].layout(px, py, labels[i]);
         px += widths[i] + 6.0F;
      }

      this.rowH = wrap ? py + 20.0F + 6.0F - this.y : 26.0F;
      float ty = this.y + 12.0F;
      Ui.textMiddle(lead, this.x, ty, 8.8F, Theme.TEXT_DIM, false);
      float nx = this.x + Ui.width(lead, 8.8F);
      Ui.textMiddle(sky.displayName(), nx, ty, 8.8F, Theme.TEXT, true);
      if (this.module.isModified(sky)) {
         float dx = nx + Ui.widthBold(sky.displayName(), 8.8F) + 7.0F;
         Ui.circle(dx, ty, 2.6F, accent | 0xFF000000);
      }

      this.copyPill.draw(c, mx, my, dt, accent, this.picking, true);
      this.allPill.draw(a, mx, my, dt, accent, this.armed == 1, true);
      this.resetPill.draw(r, mx, my, dt, accent, this.armed == 2, true);
      float cy = this.y + this.rowH;
      if (this.showsMessage()) {
         Ui.textHeavy(Ui.ellipsizeHeavy(this.message, 8.2F, this.w), this.x, cy + 1.0F, 8.2F, accent | 0xFF000000);
         cy += 13.0F;
      }

      if (this.pickOpen > 0.01F) {
         Ui.pushAlpha(this.pickOpen);
         int cols = this.columns();
         float chipW = (this.w - 5.0F * (cols - 1)) / cols;
         int slot = 0;

         for (int i = 0; i < this.values.length; i++) {
            SkyStyle s = this.values[i];
            if (s != sky) {
               float chx = this.x + slot % cols * (chipW + 5.0F);
               float chy = cy + 2.0F + slot / cols * 23.0F;
               slot++;
               boolean over = this.picking && mx >= chx && mx < chx + chipW && my >= chy && my < chy + 18.0F;
               this.chipHover[i] = Easing.approach(this.chipHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
               Ui.surface(chx, chy, chipW, 18.0F, 9.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.chipHover[i]));
               Ui.gradientH(chx + 5.0F, chy + 5.0F, 8.0F, 8.0F, 4.0F, s.colorA | 0xFF000000, s.colorB | 0xFF000000);
               int text = ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, 0.35F + 0.65F * this.chipHover[i]);
               Ui.textMiddle(Ui.ellipsize(s.displayName(), 8.2F, chipW - 32.0F, false), chx + 18.0F, chy + 9.0F, 8.2F, text, false);
               if (this.module.isModified(s)) {
                  Ui.circle(chx + chipW - 8.0F, chy + 9.0F, 2.2F, accent | 0xFF000000);
               }
            }
         }

         Ui.popAlpha();
      }
   }

   private SkyStyle chipAt(float mx, float my) {
      if (this.picking && !(this.pickOpen < 0.9F)) {
         SkyStyle sky = this.module.style.get();
         int cols = this.columns();
         float chipW = (this.w - 5.0F * (cols - 1)) / cols;
         float cy = this.y + this.rowH + (this.showsMessage() ? 13.0F : 0.0F);
         int slot = 0;

         for (SkyStyle s : this.values) {
            if (s != sky) {
               float chx = this.x + slot % cols * (chipW + 5.0F);
               float chy = cy + 2.0F + slot / cols * 23.0F;
               slot++;
               if (mx >= chx && mx < chx + chipW && my >= chy && my < chy + 18.0F) {
                  return s;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void say(String text) {
      this.message = text;
      this.messageAt = ClientClock.millis();
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         this.armed = 0;
         return false;
      } else if (button != 0) {
         return true;
      } else {
         SkyStyle sky = this.module.style.get();
         if (this.copyPill.contains(mx, my)) {
            this.picking = !this.picking;
            this.armed = 0;
            ModSounds.gui(Sfx.DROPDOWN);
            return true;
         } else if (this.allPill.contains(mx, my)) {
            if (this.armed == 1) {
               this.armed = 0;
               this.module.applyToAll(sky);
               this.say("Every sky now has " + sky.displayName() + "'s settings (their own options stay).");
               ModSounds.gui(Sfx.SUCCESS);
            } else {
               this.armed = 1;
               this.armedUntil = System.currentTimeMillis() + 3000L;
               ModSounds.gui(Sfx.CONFIRM);
            }

            return true;
         } else if (this.resetPill.contains(mx, my)) {
            if (this.armed == 2) {
               this.armed = 0;
               this.module.resetSky(sky);
               this.say(sky.displayName() + " is back to its defaults.");
               ModSounds.gui(Sfx.SUCCESS);
            } else {
               this.armed = 2;
               this.armedUntil = System.currentTimeMillis() + 3000L;
               ModSounds.gui(Sfx.CONFIRM);
            }

            return true;
         } else {
            SkyStyle from = this.chipAt(mx, my);
            if (from != null) {
               this.module.copyInto(from, sky);
               this.picking = false;
               this.armed = 0;
               this.say(sky.displayName() + " now has " + from.displayName() + "'s settings (its own options stay).");
               ModSounds.gui(Sfx.SELECT);
               return true;
            } else {
               this.armed = 0;
               return true;
            }
         }
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      SkyStyle sky = this.module.style.get();
      if (this.copyPill.contains(mx, my)) {
         return this.picking
            ? "Closes the list of skies"
            : "Gives " + sky.displayName() + " another sky's settings: motion, colors, when and where, stars, clouds, music, quality (not a sky's own options)";
      } else if (this.allPill.contains(mx, my)) {
         return this.armed == 1
            ? "Every sky takes " + sky.displayName() + "'s settings (their own options stay)"
            : "Gives every sky " + sky.displayName() + "'s settings; click twice";
      } else if (this.resetPill.contains(mx, my)) {
         return "Puts " + sky.displayName() + " back to its defaults, its own options too; click twice";
      } else {
         SkyStyle chip = this.chipAt(mx, my);
         if (chip != null) {
            return "Copy "
               + chip.displayName()
               + "'s settings into "
               + sky.displayName()
               + (this.module.isModified(chip) ? " (changed from its defaults)" : "");
         } else {
            return my < this.y + 26.0F && this.module.isModified(sky) ? sky.displayName() + "'s settings differ from its defaults" : null;
         }
      }
   }
}
