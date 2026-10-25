package meowtils.furiahvisuals.gui.widget;

import java.util.List;
import java.util.Set;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.search.SearchList;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.PresetSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.gui.GuiScreen;

public final class PresetsWidget extends Widget implements SearchList {
   private static final float CHIP_H = 22.0F;
   private static final float GAP = 6.0F;
   private static final float TEXT = 9.0F;
   private static final int MAX_CHIPS = 64;
   private final PresetSetting setting;
   private final float[] chipX = new float[64];
   private final float[] chipY = new float[64];
   private final float[] chipW = new float[64];
   private final float[] chipHover = new float[64];
   private int chipCount;
   private float chipsH = 22.0F;
   private final Pill apply = new Pill();
   private final Pill save = new Pill();
   private final Pill copy = new Pill();
   private final Pill paste = new Pill();
   private String removeArmed;
   private long removeUntil;
   private String status;
   private long statusUntil;

   public PresetsWidget(PresetSetting setting) {
      this.setting = setting;
   }

   @Override
   public float height() {
      return this.chipsH + 10.0F + 24.0F + 26.0F + 6.0F;
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private static float chipWidth(PresetSetting.Chip chip) {
      return 24.0F + Ui.width(chip.name, 9.0F) + 10.0F;
   }

   private void layout(List<PresetSetting.Chip> chips, boolean custom) {
      float cx = this.x;
      float cy = this.y;
      this.chipCount = Math.min(63, chips.size());

      for (int i = 0; i <= this.chipCount; i++) {
         boolean isCustom = i == this.chipCount;
         if (isCustom && !custom) {
            break;
         }

         float cw = isCustom ? Ui.width("Custom", 9.0F) + 22.0F : chipWidth(chips.get(i)) + (chips.get(i).removable ? 12.0F : 0.0F);
         if (cx > this.x && cx + cw > this.x + this.w) {
            cx = this.x;
            cy += 28.0F;
         }

         this.chipX[i] = cx;
         this.chipY[i] = cy;
         this.chipW[i] = cw;
         cx += cw + 6.0F;
      }

      this.chipsH = cy - this.y + 22.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      List<PresetSetting.Chip> chips = this.setting.chips();
      String current = this.setting.current();
      boolean custom = "custom".equals(current);
      this.layout(chips, custom);
      long now = System.currentTimeMillis();
      if (now > this.removeUntil) {
         this.removeArmed = null;
      }

      PresetSetting.Chip shown = null;

      for (int i = 0; i <= this.chipCount; i++) {
         boolean isCustom = i == this.chipCount;
         if (isCustom && !custom) {
            break;
         }

         PresetSetting.Chip chip = isCustom ? null : chips.get(i);
         boolean selected = isCustom || chip.id.equals(current);
         if (selected && chip != null) {
            shown = chip;
         }

         boolean over = mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 22.0F;
         this.chipHover[i] = Easing.approach(this.chipHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
         float cx = this.chipX[i];
         float cy = this.chipY[i];
         float cw = this.chipW[i];
         if (selected) {
            Ui.glow(cx + cw / 2.0F, cy + 11.0F, cw * 1.1F, ColorUtil.withAlpha(accent, 0.14F));
            Ui.roundRect(cx, cy, cw, 22.0F, 11.0F, Theme.ACCENT_SOFT);
            Ui.accentOutline(cx, cy, cw, 22.0F, 11.0F, 1.0F, accent);
         } else {
            Ui.surface(cx, cy, cw, 22.0F, 11.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.chipHover[i]));
         }

         int text = selected ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.chipHover[i]);
         if (isCustom) {
            Ui.textMiddle("Custom", cx + 11.0F, cy + 11.0F, 9.0F, text, true);
         } else {
            Pill.dot(cx + 12.0F, cy + 11.0F, 4.2F, chip.colorA, chip.colorB, chip.mode);
            Ui.textMiddle(chip.name, cx + 22.0F, cy + 11.0F, 9.0F, text, selected);
            if (chip.removable && (this.chipHover[i] > 0.01F || chip.id.equals(this.removeArmed))) {
               boolean armed = chip.id.equals(this.removeArmed);
               float rx = cx + cw - 12.0F;
               float ry = cy + 11.0F;
               Ui.circle(rx, ry, 5.5F, armed ? Theme.DANGER : ColorUtil.withAlpha(Theme.TEXT_MUTED, 0.35F * this.chipHover[i]));
               Ui.textMiddle("×", rx - Ui.width("×", 8.6F) / 2.0F, ry - 0.5F, 8.6F, armed ? -1 : Theme.TEXT_DIM, false);
            }
         }
      }

      float ry = this.y + this.chipsH + 10.0F;
      if (shown != null) {
         Pill.dot(this.x + 7.0F, ry + 10.0F, 5.5F, shown.colorA, shown.colorB, shown.mode);
         String label = Ui.ellipsize("Suggested color of " + shown.name, 8.6F, this.w - 170.0F, false);
         Ui.textMiddle(label, this.x + 18.0F, ry + 10.0F, 8.6F, Theme.TEXT_DIM, false);
         String a = "Apply suggested color";
         this.apply.layout(this.x + this.w - (Ui.width(a, 8.8F) + 20.0F), ry, a);
         this.apply.draw(a, mx, my, dt, accent, true, true);
      } else {
         this.apply.w = 0.0F;
         Ui.textMiddle("Custom: your own mix of settings", this.x, ry + 10.0F, 8.6F, Theme.TEXT_MUTED, false);
      }

      float by = ry + 26.0F;
      float bx = this.x;
      bx += this.save.layout(bx, by, "Save as preset") + 6.0F;
      bx += this.copy.layout(bx, by, "Copy code") + 6.0F;
      this.paste.layout(bx, by, "Paste code");
      this.save.draw("Save as preset", mx, my, dt, accent, false, true);
      this.copy.draw("Copy code", mx, my, dt, accent, false, true);
      this.paste.draw("Paste code", mx, my, dt, accent, false, true);
      long clock = ClientClock.millis();
      if (this.status != null && clock < this.statusUntil) {
         float a = Easing.clamp01((float)(this.statusUntil - clock) / 400.0F);
         float sx = this.paste.x + this.paste.w + 10.0F;
         Ui.pushAlpha(a);
         Ui.textMiddleHeavy(Ui.ellipsizeHeavy(this.status, 8.4F, this.x + this.w - sx), sx, by + 10.0F, 8.4F, accent | 0xFF000000);
         Ui.popAlpha();
      }
   }

   private void say(String message) {
      this.status = message;
      this.statusUntil = ClientClock.millis() + 3200L;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         return false;
      } else {
         List<PresetSetting.Chip> chips = this.setting.chips();

         for (int i = 0; i < this.chipCount && i < chips.size(); i++) {
            if (mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 22.0F) {
               PresetSetting.Chip chip = chips.get(i);
               if (button == 1) {
                  GuiScreen.setClipboardString(this.setting.codeOf(chip.id));
                  this.say("Code of " + chip.name + " copied");
                  ModSounds.gui(Sfx.SUCCESS);
               } else if (button == 0 && chip.removable && mx >= this.chipX[i] + this.chipW[i] - 20.0F) {
                  if (chip.id.equals(this.removeArmed)) {
                     this.setting.remove(chip.id);
                     this.removeArmed = null;
                     this.say("Removed " + chip.name);
                     ModSounds.gui(Sfx.CLICK);
                  } else {
                     this.removeArmed = chip.id;
                     this.removeUntil = System.currentTimeMillis() + 3000L;
                     this.say("Click × again to remove " + chip.name);
                     ModSounds.gui(Sfx.CONFIRM);
                  }
               } else if (button == 0) {
                  this.setting.apply(chip.id);
                  ModSounds.gui(Sfx.SUCCESS);
               }

               return true;
            }
         }

         if (button != 0) {
            return true;
         } else {
            if (this.apply.w > 0.0F && this.apply.contains(mx, my)) {
               this.setting.applySuggestedColor(this.setting.current());
               this.say("Colors set to the suggestion");
               ModSounds.gui(Sfx.SUCCESS);
            } else if (this.save.contains(mx, my)) {
               String id = this.setting.saveCurrent();
               this.say("Saved as \"" + id.substring("user:".length()) + "\"");
               ModSounds.gui(Sfx.SUCCESS);
            } else if (this.copy.contains(mx, my)) {
               GuiScreen.setClipboardString(this.setting.exportCode());
               this.say("Preset code copied");
               ModSounds.gui(Sfx.SUCCESS);
            } else if (this.paste.contains(mx, my)) {
               String result = this.setting.importCode(GuiScreen.getClipboardString());
               this.say(result);
               ModSounds.gui(result.startsWith("Added") ? Sfx.SUCCESS : Sfx.ERROR);
            }

            return true;
         }
      }
   }

   @Override
   public void searchItems(SearchList.Items out) {
      for (PresetSetting.Chip chip : this.setting.chips()) {
         out.item(chip.id, chip.name, chip.description, "preset");
      }
   }

   @Override
   public void searchFilter(Set<Object> keys) {
   }

   @Override
   public float reveal(Object key) {
      List<PresetSetting.Chip> chips = this.setting.chips();

      for (int i = 0; i < this.chipCount && i < chips.size(); i++) {
         if (chips.get(i).id.equals(key)) {
            return this.chipY[i] - this.y;
         }
      }

      return 0.0F;
   }

   @Override
   public float revealHeight(Object key) {
      return 22.0F;
   }

   @Override
   public int searchVersion() {
      int v = 1;

      for (PresetSetting.Chip chip : this.setting.chips()) {
         v = v * 31 + chip.id.hashCode();
      }

      return v;
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else {
         List<PresetSetting.Chip> chips = this.setting.chips();

         for (int i = 0; i < this.chipCount && i < chips.size(); i++) {
            if (mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i] && my >= this.chipY[i] && my < this.chipY[i] + 22.0F) {
               return chips.get(i).description + "  (right-click: copy code)";
            }
         }

         if (this.apply.w > 0.0F && this.apply.contains(mx, my)) {
            return "Sets the glow color to this preset's suggestion";
         } else if (this.save.contains(mx, my)) {
            return "Keeps the current settings (and colors) as a preset of your own";
         } else if (this.copy.contains(mx, my)) {
            return "Copies the current settings as a short code to share";
         } else {
            return this.paste.contains(mx, my) ? "Adds the preset whose code is on the clipboard" : null;
         }
      }
   }
}
