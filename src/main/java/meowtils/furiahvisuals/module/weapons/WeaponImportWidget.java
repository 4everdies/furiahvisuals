package meowtils.furiahvisuals.module.weapons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Widget;

final class WeaponImportWidget extends Widget {
   private static final float SIZE = 8.2F;
   private static final float LINE = 11.0F;
   private final WeaponsModule module;
   private final Pill importPill = new Pill();
   private final Pill folderPill = new Pill();
   private final Pill reloadPill = new Pill();
   private final List<String> lines = new ArrayList<>();
   private final List<Integer> colors = new ArrayList<>();

   WeaponImportWidget(WeaponsModule module) {
      this.module = module;
   }

   private void layoutText(int accent) {
      this.lines.clear();
      this.colors.clear();
      String m = this.module.importMessage();
      if (m != null) {
         this.wrap(m, this.module.importError() ? Theme.DANGER : accent);
      }

      for (Entry<String, String> e : this.module.library.importProblems().entrySet()) {
         this.wrap("\"" + e.getKey() + "\" isn't loaded: " + e.getValue() + ".", Theme.DANGER);
      }
   }

   private void wrap(String text, int color) {
      StringBuilder line = new StringBuilder();
      int added = 0;

      for (String word : text.split(" ")) {
         String next = line.length() == 0 ? word : line + " " + word;
         if (!(Ui.width(next, 8.2F) > this.w) || line.length() <= 0) {
            line.setLength(0);
            line.append(next);
         } else if (added == 2) {
            line.append(" ").append(word);
         } else {
            this.lines.add(line.toString());
            this.colors.add(color);
            added++;
            line.setLength(0);
            line.append(word);
         }
      }

      if (line.length() > 0) {
         this.lines.add(Ui.ellipsize(line.toString(), 8.2F, this.w, false));
         this.colors.add(color);
      }
   }

   @Override
   public float height() {
      return 26.0F + (this.lines.isEmpty() ? 14.0F : this.lines.size() * 11.0F + 4.0F);
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.layoutText(accent);
      float bx = this.x;
      String imp = WeaponImport.isPicking() ? "Picking…" : "Import model…";
      bx += this.importPill.layout(bx, this.y, imp) + 6.0F;
      bx += this.folderPill.layout(bx, this.y, "Open folder") + 6.0F;
      String rel = WeaponImport.isReloading() ? "Reloading…" : "Reload";
      this.reloadPill.layout(bx, this.y, rel);
      this.importPill.draw(imp, mx, my, dt, accent, true, !WeaponImport.isPicking());
      this.folderPill.draw("Open folder", mx, my, dt, accent, false, true);
      this.reloadPill.draw(rel, mx, my, dt, accent, false, !WeaponImport.isReloading());
      float ty = this.y + 26.0F;
      if (this.lines.isEmpty()) {
         int n = this.module.library.imported().size();
         Ui.text(
            n == 0
               ? "No imported weapons yet. Each one is a folder in config/furiahsvisuals/weapons (see the README)."
               : n + (n == 1 ? " imported weapon" : " imported weapons") + ": pick one under Weapons (Imported) to set its grip.",
            this.x,
            ty + 2.0F,
            8.2F,
            Theme.TEXT_MUTED
         );
      } else {
         for (int i = 0; i < this.lines.size(); i++) {
            Ui.text(this.lines.get(i), this.x, ty + i * 11.0F, 8.2F, this.colors.get(i));
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         return false;
      } else if (button != 0) {
         return true;
      } else if (this.importPill.contains(mx, my)) {
         WeaponImport.pick(this.module);
         return true;
      } else if (this.folderPill.contains(mx, my)) {
         WeaponImport.openFolder(this.module);
         return true;
      } else if (this.reloadPill.contains(mx, my)) {
         WeaponImport.reload(this.module);
         return true;
      } else {
         return true;
      }
   }
}
