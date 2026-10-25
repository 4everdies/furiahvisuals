package meowtils.furiahvisuals.gui;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.compat.OpenFolder;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.PresetManager;
import meowtils.furiahvisuals.gui.search.SearchEntry;
import meowtils.furiahvisuals.gui.search.SearchIndex;
import meowtils.furiahvisuals.gui.search.SearchQuery;
import meowtils.furiahvisuals.gui.search.SearchResult;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.ScrollBar;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.TextSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.gui.GuiScreen;

final class PresetsPage {
   private static final float TOP = 64.0F;
   private static final float FIELD_H = 22.0F;
   private static final int MODIFIED = -19129;
   private final PresetManager presets = FuriahsVisuals.instance.getPresets();
   private final ScrollBar bar = new ScrollBar();
   private final Pill save = new Pill();
   private final Pill importCode = new Pill();
   private final Pill folder = new Pill();
   private final SimpleDateFormat date = new SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.ENGLISH);
   private PresetsPage.RowPills[] rows = new PresetsPage.RowPills[0];
   private int hoverIndex = -1;
   private float x;
   private float y;
   private float w;
   private float h;
   private float listY;
   private float listH;
   private float fieldX;
   private float fieldW;
   private boolean fieldFocused;
   private String fieldText = "";
   private float caret;
   private String renaming;
   private String renameText = "";
   private String deleteArmed;
   private long deleteUntil;
   private String hoverRow;
   private String lastClick;
   private long lastClickMs;
   private String message;
   private boolean messageError;
   private long messageUntil;
   private float pencilX = -1.0F;
   private String filterText = "";
   private SearchQuery filterQuery = SearchQuery.of("");
   private final List<PresetManager.Preset> filtered = new ArrayList<>();
   private final List<SearchResult> filterResults = new ArrayList<>();
   private List<PresetManager.Preset> filteredFrom;
   private int filteredFromSize = -1;
   private float restore = -1.0F;
   private float extraTop;

   private static float rowH() {
      return Density.dense() ? 30.0F : (Density.compact() ? 36.0F : 44.0F);
   }

   private static float nameY() {
      return Density.dense() ? 10.0F : (Density.compact() ? 11.0F : 12.0F);
   }

   private static float subY() {
      return Density.dense() ? 16.5F : (Density.compact() ? 19.0F : 21.0F);
   }

   private static float nameSize() {
      return Density.dense() ? 8.8F : (Density.compact() ? 9.4F : 10.0F);
   }

   private static float subSize() {
      return Density.dense() ? 7.2F : (Density.compact() ? 7.6F : 8.2F);
   }

   void filter(String text) {
      if (!text.equals(this.filterText)) {
         this.filterText = text;
         this.filterQuery = SearchQuery.of(text);
         this.filteredFrom = null;
         this.bar.jumpTo(0.0F);
      }
   }

   boolean isFiltering() {
      return !this.filterQuery.isEmpty();
   }

   int matchCount() {
      this.visible();
      return this.filtered.size();
   }

   private List<PresetManager.Preset> visible() {
      List<PresetManager.Preset> all = this.presets.list();
      if (this.filterQuery.isEmpty()) {
         return all;
      } else {
         int signature = all.size();

         for (PresetManager.Preset p : all) {
            signature = signature * 31 + p.name.hashCode();
         }

         if (this.filteredFrom != all || this.filteredFromSize != signature) {
            this.filteredFrom = all;
            this.filteredFromSize = signature;
            SearchIndex index = new SearchIndex("#presets", "Presets");

            for (PresetManager.Preset p : all) {
               index.add(SearchEntry.Kind.BLOCK, null, p, p.name, null, null, null, null, null);
            }

            index.search(this.filterQuery, this.filterResults);
            this.filtered.clear();

            for (SearchResult r : this.filterResults) {
               this.filtered.add((PresetManager.Preset)r.entry.key);
            }
         }

         return this.filtered;
      }
   }

   void opened() {
      this.presets.refresh();
      this.fieldFocused = false;
      this.renaming = null;
   }

   void restoreScroll(float units) {
      this.restore = units;
   }

   float scrollTarget() {
      return this.restore >= 0.0F ? this.restore : this.bar.target;
   }

   void render(float x, float y, float w, float h, float mx, float my, float dt, int accent) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.caret += dt;
      long now = System.currentTimeMillis();
      if (now > this.deleteUntil) {
         this.deleteArmed = null;
      }

      String saveLabel = "Save as new";
      String importLabel = "Import code";
      String folderLabel = "Open folder";
      float right = x + w;
      float buttonsW = Ui.width(folderLabel, 8.8F) + 20.0F + 6.0F + Ui.width(importLabel, 8.8F) + 20.0F + 12.0F + Ui.width(saveLabel, 8.8F) + 20.0F;
      boolean below = w - buttonsW - 8.0F < 120.0F;
      float by = below ? y + 22.0F + 4.0F : y + 1.0F;
      this.extraTop = below ? 24.0F : 0.0F;
      this.folder.layout(right - (Ui.width(folderLabel, 8.8F) + 20.0F), by, folderLabel);
      this.importCode.layout(this.folder.x - 6.0F - (Ui.width(importLabel, 8.8F) + 20.0F), by, importLabel);
      this.save.layout(this.importCode.x - 12.0F - (Ui.width(saveLabel, 8.8F) + 20.0F), by, saveLabel);
      this.fieldX = x;
      this.fieldW = below ? w : this.save.x - 8.0F - x;
      boolean overField = inside(mx, my, this.fieldX, y, this.fieldW, 22.0F);
      Ui.surface(this.fieldX, y, this.fieldW, 22.0F, 7.0F, !overField && !this.fieldFocused ? Theme.SURFACE : Theme.SURFACE_HOVER);
      Ui.outline(this.fieldX, y, this.fieldW, 22.0F, 7.0F, 0.9F, this.fieldFocused ? accent | 0xFF000000 : Theme.OUTLINE);
      if (this.fieldText.isEmpty() && !this.fieldFocused) {
         Ui.textMiddle(Ui.ellipsize("Name for a new preset…", 9.0F, this.fieldW - 18.0F, false), this.fieldX + 9.0F, y + 11.0F, 9.0F, Theme.TEXT_MUTED, false);
      } else {
         String shown = tail(this.fieldText, 9.0F, this.fieldW - 20.0F);
         Ui.textMiddle(shown, this.fieldX + 9.0F, y + 11.0F, 9.0F, Theme.TEXT, false);
         if (this.fieldFocused && this.caret % 1.0F < 0.55F) {
            Ui.rect(this.fieldX + 9.0F + Ui.width(shown, 9.0F) + 1.0F, y + 5.0F, 0.8F, 12.0F, Theme.TEXT);
         }
      }

      this.save.draw(saveLabel, mx, my, dt, accent, true, true);
      this.importCode.draw(importLabel, mx, my, dt, accent, false, true);
      this.folder.draw(folderLabel, mx, my, dt, accent, false, true);
      float sy = y + 22.0F + 9.0F + this.extraTop;
      long clock = ClientClock.millis();
      if (this.message != null && clock < this.messageUntil) {
         float a = Easing.clamp01((float)(this.messageUntil - clock) / 400.0F);
         Ui.pushAlpha(a);
         Ui.text(Ui.ellipsize(this.message, 8.4F, w, false), x, sy, 8.4F, this.messageError ? Theme.DANGER : accent);
         Ui.popAlpha();
      } else {
         Ui.text(
            Ui.ellipsize("Everything is saved: every section, the WaterMark, the Global Color and the theme.", 8.4F, w, false), x, sy, 8.4F, Theme.TEXT_MUTED
         );
      }

      this.listY = y + 64.0F - 18.0F + this.extraTop;
      this.listH = y + h - this.listY;
      Ui.rect(x, this.listY - 7.0F, w, 0.6F, Theme.DIVIDER);
      List<PresetManager.Preset> list = this.visible();
      PresetManager.Preset active = this.presets.getActive();
      boolean inList = inside(mx, my, x - 6.0F, this.listY, w + 14.0F, this.listH);
      float rmx = inList && !this.bar.isDragging() ? mx : -10000.0F;
      float rmy = inList && !this.bar.isDragging() ? my : -10000.0F;
      this.hoverRow = null;
      this.hoverIndex = -1;
      Ui.pushScissor(x - 8.0F, this.listY, w + 16.0F, this.listH);
      if (list.isEmpty()) {
         String none = this.isFiltering()
            ? "No presets match “" + Ui.ellipsize(this.filterText.trim(), 9.0F, w * 0.6F, false) + "”"
            : "No presets yet: name the current settings above and click Save as new.";
         Ui.textMiddle(none, x, this.listY + 18.0F, 9.0F, Theme.TEXT_MUTED, false);
      }

      for (int i = 0; i < list.size(); i++) {
         PresetManager.Preset p = list.get(i);
         float ry = this.listY + i * rowH() - this.bar.scroll;
         if (!(ry + rowH() < this.listY) && !(ry > this.listY + this.listH)) {
            this.drawRow(i, p, p == active, ry, rmx, rmy, dt, accent);
         }
      }

      Ui.popScissor();
      this.bar.update(list.size() * rowH() + 4.0F, this.listH, dt);
      if (this.restore >= 0.0F) {
         this.bar.jumpTo(this.restore);
         this.restore = -1.0F;
      }

      this.bar.draw(x + w + 5.0F, this.listY, this.listH, mx, my, dt, accent);
   }

   private PresetsPage.RowPills pills(int i) {
      if (i >= this.rows.length) {
         PresetsPage.RowPills[] grown = new PresetsPage.RowPills[Math.max(i + 1, this.rows.length * 2)];
         System.arraycopy(this.rows, 0, grown, 0, this.rows.length);
         this.rows = grown;
      }

      if (this.rows[i] == null) {
         this.rows[i] = new PresetsPage.RowPills();
      }

      return this.rows[i];
   }

   private float layoutRow(PresetsPage.RowPills rp, float ry, boolean all, boolean armed) {
      rp.all = all;
      float right = this.x + this.w - 2.0F;
      float py = ry + (rowH() - 3.0F - 20.0F) / 2.0F;
      if (all) {
         String del = armed ? "Click again" : "Delete";
         right -= rp.delete.layout(right - (Ui.width(del, 8.8F) + 20.0F), py, del) + 5.0F;
         right -= rp.export.layout(right - (Ui.width("Export", 8.8F) + 20.0F), py, "Export") + 5.0F;
         right -= rp.duplicate.layout(right - (Ui.width("Duplicate", 8.8F) + 20.0F), py, "Duplicate") + 5.0F;
         right -= rp.update.layout(right - (Ui.width("Update", 8.8F) + 20.0F), py, "Update") + 5.0F;
      } else {
         rp.update.w = 0.0F;
         rp.duplicate.w = 0.0F;
         rp.export.w = 0.0F;
         rp.delete.w = 0.0F;
      }

      return right - (rp.load.layout(right - (Ui.width("Load", 8.8F) + 20.0F), py, "Load") + 10.0F);
   }

   private void drawRow(int index, PresetManager.Preset p, boolean isActive, float ry, float mx, float my, float dt, int accent) {
      boolean over = inside(mx, my, this.x - 6.0F, ry, this.w + 6.0F, rowH() - 3.0F);
      boolean armed = p.name.equals(this.deleteArmed);
      boolean editing = p.name.equals(this.renaming);
      if (over) {
         this.hoverRow = p.name;
         this.hoverIndex = index;
         Ui.roundRect(this.x - 6.0F, ry, this.w + 6.0F, rowH() - 3.0F, 8.0F, Theme.SURFACE_HOVER);
      } else if (isActive) {
         Ui.roundRect(this.x - 6.0F, ry, this.w + 6.0F, rowH() - 3.0F, 8.0F, Theme.ACCENT_FAINT);
      }

      if (isActive) {
         Ui.roundRect(this.x - 6.0F, ry + 9.0F, 3.0F, rowH() - 21.0F, 1.5F, accent);
      }

      PresetsPage.RowPills rp = this.pills(index);
      boolean all = over || armed || editing;
      float right = this.layoutRow(rp, ry, all, armed);
      String del = armed ? "Click again" : "Delete";
      float nx = this.x + 4.0F;
      float nameW = right - nx - 26.0F;
      float ny = nameY();
      float ns = nameSize();
      if (editing) {
         float fw = Math.max(90.0F, nameW);
         Ui.roundRect(nx - 3.0F, ry + ny - 8.0F, fw, 16.0F, 4.0F, Theme.SURFACE);
         Ui.accentOutline(nx - 3.0F, ry + ny - 8.0F, fw, 16.0F, 4.0F, 0.9F, accent);
         String shown = tail(this.renameText, ns, fw - 12.0F);
         Ui.textMiddle(shown, nx, ry + ny, ns, Theme.TEXT, true);
         if (this.caret % 1.0F < 0.55F) {
            Ui.rect(nx + Ui.widthBold(shown, ns) + 1.0F, ry + ny - 5.5F, 0.8F, 11.0F, Theme.TEXT);
         }
      } else {
         String name = Ui.ellipsize(p.name, ns, nameW, true);
         Ui.textMiddle(name, nx, ry + ny, ns, p.isBroken() ? Theme.TEXT_MUTED : Theme.TEXT, true);
         float after = nx + Ui.widthBold(name, ns) + 7.0F;
         if (!p.isBroken() && this.presets.isModified(p) && isActive) {
            Ui.glow(after + 2.5F, ry + ny + 0.5F, 12.0F, ColorUtil.withAlpha(-19129, 0.5F));
            Ui.circle(after + 2.5F, ry + ny + 0.5F, 2.5F, -19129);
            after += 10.0F;
         }

         if (over && !p.isBroken()) {
            this.pencilX = after;
            pencil(after + 4.0F, ry + ny, ColorUtil.lerp(Theme.TEXT_MUTED, accent, inside(mx, my, after - 2.0F, ry + ny - 9.0F, 14.0F, 18.0F) ? 1.0F : 0.3F));
         }
      }

      String sub = p.isBroken()
         ? p.error
         : "Saved " + this.date.format(new Date(p.saved)) + (isActive ? (this.presets.isModified(p) ? "  ·  active, changed since" : "  ·  active") : "");
      float ss = subSize();
      Ui.text(Ui.ellipsize(sub, ss, right - nx - 4.0F, false), nx, ry + subY(), ss, p.isBroken() ? Theme.DANGER : Theme.TEXT_DIM);
      rp.load.draw("Load", mx, my, dt, accent, true, !p.isBroken());
      if (all) {
         rp.update.draw("Update", mx, my, dt, accent, false, !p.isBroken());
         rp.duplicate.draw("Duplicate", mx, my, dt, accent, false, !p.isBroken());
         rp.export.draw("Export", mx, my, dt, accent, false, !p.isBroken());
         rp.delete.draw(del, mx, my, dt, accent, armed, true);
      }
   }

   private static void pencil(float cx, float cy, int color) {
      for (int i = 0; i < 7; i++) {
         float t = i / 6.0F;
         Ui.circle(cx - 3.5F + 7.0F * t, cy + 3.5F - 7.0F * t, 0.9F, color);
      }

      Ui.circle(cx - 4.0F, cy + 4.0F, 0.7F, color);
   }

   private void say(String text, boolean error) {
      this.say(text, error, error ? Sfx.ERROR : Sfx.SUCCESS);
   }

   private void say(String text, boolean error, Sfx sound) {
      this.message = text;
      this.messageError = error;
      this.messageUntil = ClientClock.millis() + (error ? 5000L : 3200L);
      ModSounds.gui(sound);
   }

   boolean mouseDown(float mx, float my, int button) {
      if (this.renaming != null && (this.hoverRow == null || !this.hoverRow.equals(this.renaming) || !inside(mx, my, this.x, this.listY, this.w, this.listH))) {
         this.commitRename();
      }

      if (this.bar.mouseDown(mx, my)) {
         return true;
      } else if (button != 0) {
         return inside(mx, my, this.x, this.y, this.w, this.h);
      } else {
         boolean wasFocused = this.fieldFocused;
         this.fieldFocused = inside(mx, my, this.fieldX, this.y, this.fieldW, 22.0F);
         if (this.fieldFocused) {
            if (!wasFocused) {
               ModSounds.gui(Sfx.CLICK);
            }

            this.caret = 0.0F;
            return true;
         } else if (this.save.contains(mx, my)) {
            this.createFromField();
            return true;
         } else if (this.importCode.contains(mx, my)) {
            String result = this.presets.importCode(GuiScreen.getClipboardString());
            if (result.startsWith("!")) {
               this.say(result.substring(1), true);
            } else {
               this.say("Imported \"" + result + "\" (load it to use it)", false);
               this.reveal(result);
            }

            return true;
         } else if (this.folder.contains(mx, my)) {
            ModSounds.gui(Sfx.CLICK);
            this.openFolder();
            return true;
         } else if (!inside(mx, my, this.x - 6.0F, this.listY, this.w + 6.0F, this.listH)) {
            return inside(mx, my, this.x, this.y, this.w, this.h);
         } else {
            List<PresetManager.Preset> list = this.visible();
            int i = (int)Math.floor((my - this.listY + this.bar.scroll) / rowH());
            if (i >= 0 && i < list.size()) {
               PresetManager.Preset p = list.get(i);
               float ry = this.listY + i * rowH() - this.bar.scroll;
               PresetsPage.RowPills rp = this.pills(i);
               boolean all = p.name.equals(this.hoverRow) || p.name.equals(this.deleteArmed) || p.name.equals(this.renaming);
               this.layoutRow(rp, ry, all, p.name.equals(this.deleteArmed));
               if (rp.load.contains(mx, my) && !p.isBroken()) {
                  String warning = this.presets.load(p);
                  this.say(warning == null ? "Loaded \"" + p.name + "\"" : "Loaded \"" + p.name + "\". " + warning, warning != null, Sfx.SUCCESS);
                  return true;
               } else if (all && rp.update.contains(mx, my) && !p.isBroken()) {
                  this.report(this.presets.update(p), "Updated \"" + p.name + "\" with the current settings");
                  return true;
               } else if (all && rp.duplicate.contains(mx, my) && !p.isBroken()) {
                  this.report(this.presets.duplicate(p), "Duplicated \"" + p.name + "\"");
                  return true;
               } else if (all && rp.export.contains(mx, my) && !p.isBroken()) {
                  GuiScreen.setClipboardString(this.presets.exportCode(p));
                  this.say("Code of \"" + p.name + "\" copied (without your weather city)", false);
                  return true;
               } else if (all && rp.delete.contains(mx, my)) {
                  if (p.name.equals(this.deleteArmed)) {
                     this.deleteArmed = null;
                     this.report(this.presets.delete(p), "Deleted \"" + p.name + "\"");
                  } else {
                     this.deleteArmed = p.name;
                     this.deleteUntil = System.currentTimeMillis() + 3000L;
                     ModSounds.gui(Sfx.CONFIRM);
                  }

                  return true;
               } else if (p.isBroken()) {
                  return true;
               } else {
                  boolean onPencil = this.pencilX > 0.0F && inside(mx, my, this.pencilX - 2.0F, ry + 3.0F, 14.0F, 18.0F);
                  boolean onName = my < ry + 20.0F;
                  long now = System.currentTimeMillis();
                  boolean doubleClick = p.name.equals(this.lastClick) && now - this.lastClickMs < 400L;
                  this.lastClick = p.name;
                  this.lastClickMs = now;
                  if (onPencil || onName && doubleClick) {
                     this.renaming = p.name;
                     this.renameText = p.name;
                     this.caret = 0.0F;
                     ModSounds.gui(Sfx.CLICK);
                  }

                  return true;
               }
            } else {
               return true;
            }
         }
      }
   }

   void mouseDrag(float mx, float my) {
      this.bar.mouseDrag(mx, my);
   }

   void mouseUp() {
      this.bar.mouseUp();
   }

   void wheel(float mx, float my, int dWheel) {
      if (inside(mx, my, this.x - 8.0F, this.listY, this.w + 20.0F, this.listH)) {
         this.bar.wheel(dWheel);
      }
   }

   boolean isFocused() {
      return this.fieldFocused || this.renaming != null;
   }

   void unfocus() {
      this.fieldFocused = false;
      if (this.renaming != null) {
         this.commitRename();
      }
   }

   boolean keyTyped(char c, int key) {
      if (this.renaming != null) {
         if (key == 1) {
            this.renaming = null;
         } else if (key != 28 && key != 156) {
            this.renameText = edit(this.renameText, c, key);
            ModSounds.key();
         } else {
            this.commitRename();
         }

         this.caret = 0.0F;
         return true;
      } else if (!this.fieldFocused) {
         return false;
      } else {
         if (key == 1) {
            this.fieldFocused = false;
         } else if (key != 28 && key != 156) {
            this.fieldText = edit(this.fieldText, c, key);
            ModSounds.key();
         } else {
            this.createFromField();
         }

         this.caret = 0.0F;
         return true;
      }
   }

   private static String edit(String text, char c, int key) {
      if (key == 14) {
         return text.isEmpty() ? text : text.substring(0, text.length() - 1);
      } else if (GuiScreen.isKeyComboCtrlA(key)) {
         return "";
      } else {
         String add = GuiScreen.isKeyComboCtrlV(key) ? GuiScreen.getClipboardString() : String.valueOf(c);
         return TextSetting.clean(text + TextSetting.clean(add, 32), 32);
      }
   }

   private void createFromField() {
      String name = this.fieldText.trim();
      String error = this.presets.create(name);
      if (error != null) {
         this.say(error, true);
         this.fieldFocused = true;
      } else {
         this.say("Saved the current settings as \"" + name + "\"", false);
         this.fieldText = "";
         this.fieldFocused = false;
         this.reveal(name);
      }
   }

   private void commitRename() {
      String from = this.renaming;
      this.renaming = null;
      if (from != null) {
         PresetManager.Preset p = this.presets.find(from);
         if (p != null && !this.renameText.trim().equals(p.name)) {
            String error = this.presets.rename(p, this.renameText);
            if (error != null) {
               this.say(error, true);
            } else {
               this.say("Renamed to \"" + this.renameText.trim() + "\"", false);
            }
         }
      }
   }

   private void report(String error, String success) {
      if (error != null) {
         this.say(error, true);
      } else {
         this.say(success, false);
      }
   }

   private void reveal(String name) {
      List<PresetManager.Preset> list = this.presets.list();

      for (int i = 0; i < list.size(); i++) {
         if (list.get(i).name.equalsIgnoreCase(name)) {
            this.bar.reveal(i * rowH(), rowH());
            return;
         }
      }
   }

   private void openFolder() {
      final File dir = this.presets.getDir();
      Thread t = new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               OpenFolder.open(dir);
            } catch (Throwable var2) {
               FuriahsVisuals.LOG.warn("Could not open {}", new Object[]{dir, var2});
            }
         }
      }, "Furiah's Visuals open folder");
      t.setDaemon(true);
      t.start();
   }

   String tooltip(float mx, float my) {
      if (this.save.contains(mx, my)) {
         return "Saves every setting of the mod under the name on the left";
      } else if (this.importCode.contains(mx, my)) {
         return "Adds the preset whose code is on the clipboard (FVP2-…, from the game or the website; older FVP1-… codes too)";
      } else if (this.folder.contains(mx, my)) {
         return "config/furiahsvisuals/presets: one .json file per preset";
      } else if (this.hoverRow != null && this.hoverIndex >= 0 && this.hoverIndex < this.rows.length && this.rows[this.hoverIndex] != null) {
         PresetsPage.RowPills rp = this.rows[this.hoverIndex];
         if (rp.load.contains(mx, my)) {
            return "Applies this preset now";
         } else if (rp.all && rp.update.contains(mx, my)) {
            return "Overwrites this preset with the current settings";
         } else if (rp.all && rp.duplicate.contains(mx, my)) {
            return "Makes a copy of this preset";
         } else if (rp.all && rp.export.contains(mx, my)) {
            return "Copies this preset as a code to share (your weather city is left out)";
         } else {
            return rp.all && rp.delete.contains(mx, my) ? "Deletes this preset (click twice)" : "Double-click the name to rename it";
         }
      } else {
         return null;
      }
   }

   private static String tail(String text, float size, float maxWidth) {
      String s = text;

      while (!s.isEmpty() && Ui.width(s, size) > maxWidth) {
         s = s.substring(1);
      }

      return s;
   }

   private static boolean inside(float mx, float my, float x, float y, float w, float h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   void typeNameForTest(String name) {
      this.fieldText = name;
   }

   void createForTest() {
      this.createFromField();
   }

   String messageForTest() {
      return this.message;
   }

   private static final class RowPills {
      final Pill load = new Pill();
      final Pill update = new Pill();
      final Pill duplicate = new Pill();
      final Pill export = new Pill();
      final Pill delete = new Pill();
      boolean all;

      private RowPills() {
      }
   }
}
