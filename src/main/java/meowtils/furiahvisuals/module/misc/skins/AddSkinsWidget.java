package meowtils.furiahvisuals.module.misc.skins;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class AddSkinsWidget extends Widget {
   private static final float MESSAGE_SIZE = 8.2F;
   private static final float MESSAGE_LINE = 11.0F;
   private static final int MESSAGE_LINES = 3;
   private final Skins skins;
   private final Pill upload = new Pill();
   private final Pill folder = new Pill();
   private final Pill reload = new Pill();
   private boolean refreshed;

   AddSkinsWidget(Skins skins) {
      this.skins = skins;
   }

   private List<String> lines(String message) {
      ArrayList<String> lines = new ArrayList<>();
      if (message == null) {
         return lines;
      } else {
         StringBuilder line = new StringBuilder();
         String[] words = message.split(" ");

         for (int i = 0; i < words.length; i++) {
            String next = line.length() == 0 ? words[i] : line + " " + words[i];
            if (Ui.width(next, 8.2F) > this.w && line.length() > 0) {
               lines.add(line.toString());
               line.setLength(0);
               line.append(words[i]);
               if (lines.size() == 2) {
                  for (int j = i + 1; j < words.length; j++) {
                     line.append(' ').append(words[j]);
                  }
                  break;
               }
            } else {
               line.setLength(0);
               line.append(next);
            }
         }

         if (line.length() > 0) {
            lines.add(Ui.ellipsize(line.toString(), 8.2F, this.w, false));
         }

         return lines;
      }
   }

   @Override
   public float height() {
      String m = this.skins.library().message();
      return 26.0F + (m == null ? 0.0F : this.lines(m).size() * 11.0F + 3.0F);
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      SkinLibrary lib = this.skins.library();
      if (!this.refreshed) {
         this.refreshed = true;
         lib.refresh();
      }

      String up = SkinFiles.isPicking() ? "Picking…" : "Upload PNG…";
      float bx = this.x;
      bx += this.upload.layout(bx, this.y, up) + 6.0F;
      bx += this.folder.layout(bx, this.y, "Open folder") + 6.0F;
      this.reload.layout(bx, this.y, "Reload");
      this.upload.draw(up, mx, my, dt, accent, true, !SkinFiles.isPicking());
      this.folder.draw("Open folder", mx, my, dt, accent, false, true);
      this.reload.draw("Reload", mx, my, dt, accent, false, true);
      String m = lib.message();
      if (m != null) {
         List<String> ls = this.lines(m);
         int color = lib.messageIsError() ? Theme.DANGER : accent;

         for (int i = 0; i < ls.size(); i++) {
            Ui.text(ls.get(i), this.x, this.y + 26.0F + i * 11.0F, 8.2F, color);
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         return false;
      } else if (button != 0) {
         return true;
      } else {
         SkinLibrary lib = this.skins.library();
         if (this.upload.contains(mx, my) && !SkinFiles.isPicking()) {
            ModSounds.gui(Sfx.CLICK);
            SkinFiles.pick(lib);
         } else if (this.folder.contains(mx, my)) {
            ModSounds.gui(Sfx.CLICK);
            SkinFiles.openFolder(lib.getDir());
         } else if (this.reload.contains(mx, my)) {
            ModSounds.gui(Sfx.CLICK);
            lib.refresh();
            lib.say("Looked at the skins folder again.", false);
         }

         return true;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (this.upload.contains(mx, my)) {
         return "Pick PNG skins: 64×64, 64×32 (old skins), or HD up to 1024×1024, 8 MB at most. They are checked, then copied into the skins folder";
      } else if (this.folder.contains(mx, my)) {
         return "config/furiahsvisuals/skins: drop PNG skins there, then press Reload";
      } else {
         return this.reload.contains(mx, my) ? "Looks at the skins folder again" : null;
      }
   }
}
