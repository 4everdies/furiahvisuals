package meowtils.furiahvisuals.module.particles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.search.SearchList;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Widget;
import net.minecraft.client.gui.GuiScreen;

final class LibraryWidget extends Widget implements SearchList {
   private static final float ROW = 36.0F;
   private static final float THUMB = 28.0F;
   private static final float MESSAGE_SIZE = 8.2F;
   private static final float MESSAGE_LINE = 11.0F;
   private static final int MESSAGE_LINES = 3;
   private final ParticlesModule module;
   private final ImageLibrary library;
   private final Pill importPill = new Pill();
   private final Pill folderPill = new Pill();
   private final Pill reloadPill = new Pill();
   private final Pill renamePill = new Pill();
   private final Pill deletePill = new Pill();
   private boolean refreshed;
   private List<ImageLibrary.Entry> shown;
   private float rowsY;
   private String renaming;
   private String renameText = "";
   private float caret;
   private String deleteArmed;
   private long deleteUntil;
   private String hoverRow;
   private Set<Object> filter;

   LibraryWidget(ParticlesModule module) {
      this.module = module;
      this.library = module.library;
   }

   private int rows() {
      return this.shown == null ? 0 : this.shown.size();
   }

   private List<String> messageLines(String message) {
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

   private float messageHeight() {
      String message = this.library.message();
      return message == null ? 0.0F : this.messageLines(message).size() * 11.0F + 3.0F;
   }

   @Override
   public float height() {
      float h = 26.0F + this.messageHeight();
      return h + (this.rows() == 0 ? 30.0F : this.rows() * 36.0F) + 4.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.module.pageShown();
      if (!this.refreshed) {
         this.refreshed = true;
         this.library.refresh();
      }

      this.caret += dt;
      if (System.currentTimeMillis() > this.deleteUntil) {
         this.deleteArmed = null;
      }

      this.shown = this.library.entries();
      if (this.filter != null) {
         ArrayList<ImageLibrary.Entry> matched = new ArrayList<>();

         for (ImageLibrary.Entry e : this.shown) {
            if (this.filter.contains(e.name)) {
               matched.add(e);
            }
         }

         this.shown = matched;
      }

      float bx = this.x;
      String imp = ImageImport.isPicking() ? "Picking…" : "Import image…";
      bx += this.importPill.layout(bx, this.y, imp) + 6.0F;
      bx += this.folderPill.layout(bx, this.y, "Open folder") + 6.0F;
      this.reloadPill.layout(bx, this.y, "Reload");
      this.importPill.draw(imp, mx, my, dt, accent, true, !ImageImport.isPicking());
      this.folderPill.draw("Open folder", mx, my, dt, accent, false, true);
      this.reloadPill.draw("Reload", mx, my, dt, accent, false, true);
      float ty = this.y + 26.0F;
      String message = this.library.message();
      if (message != null) {
         List<String> lines = this.messageLines(message);
         int color = this.library.messageIsError() ? Theme.DANGER : accent;

         for (int i = 0; i < lines.size(); i++) {
            Ui.text(lines.get(i), this.x, ty + i * 11.0F, 8.2F, color);
         }

         ty += lines.size() * 11.0F + 3.0F;
      }

      this.rowsY = ty;
      if (this.rows() == 0) {
         Ui.text("No images yet: import one, or drop images (PNG, JPG, GIF, BMP) into the folder and press Reload.", this.x, ty + 4.0F, 8.4F, Theme.TEXT_MUTED);
      } else {
         this.hoverRow = null;

         for (int i = 0; i < this.shown.size(); i++) {
            ImageLibrary.Entry ex = this.shown.get(i);
            float ry = this.rowsY + i * 36.0F;
            boolean over = mx >= this.x - 4.0F && mx < this.x + this.w + 4.0F && my >= ry && my < ry + 36.0F - 2.0F;
            if (over) {
               this.hoverRow = ex.name;
               Ui.roundRect(this.x - 4.0F, ry, this.w + 8.0F, 34.0F, 7.0F, Theme.SURFACE_HOVER);
            }

            Ui.checker(this.x, ry + 3.0F, 28.0F, 28.0F, 5.0F, 3.5F);
            if (ex.state == ImageLibrary.State.READY) {
               Ui.image(ex.texture, this.x, ry + 3.0F, 28.0F, 28.0F, 0.0F, 0.0F, 1.0F, 1.0F, -1);
            }

            Ui.outline(this.x, ry + 3.0F, 28.0F, 28.0F, 5.0F, 0.8F, Theme.OUTLINE_HOVER);
            float nx = this.x + 28.0F + 10.0F;
            float right = this.x + this.w;
            boolean armed = ex.name.equals(this.deleteArmed);
            String del = armed ? "Click again" : "Delete";
            String ren = "Rename";
            boolean pills = over || armed || ex.name.equals(this.renaming);
            if (pills) {
               this.deletePill.layout(right - (Ui.width(del, 8.8F) + 20.0F), ry + 7.0F, del);
               this.renamePill.layout(this.deletePill.x - 6.0F - (Ui.width(ren, 8.8F) + 20.0F), ry + 7.0F, ren);
               right = this.renamePill.x - 8.0F;
            }

            if (ex.name.equals(this.renaming)) {
               float fw = Math.max(80.0F, right - nx);
               Ui.roundRect(nx - 3.0F, ry + 3.0F, fw, 15.0F, 4.0F, Theme.SURFACE);
               Ui.accentOutline(nx - 3.0F, ry + 3.0F, fw, 15.0F, 4.0F, 0.9F, accent);
               String shownText = Ui.ellipsize(this.renameText, 9.0F, fw - 12.0F, false);
               Ui.textMiddle(shownText, nx, ry + 10.5F, 9.0F, Theme.TEXT, false);
               if (this.caret % 1.0F < 0.55F) {
                  Ui.rect(nx + Ui.width(shownText, 9.0F) + 1.0F, ry + 5.5F, 0.8F, 10.0F, Theme.TEXT);
               }
            } else {
               Ui.textMiddle(Ui.ellipsize(ex.name, 9.2F, right - nx, false), nx, ry + 10.5F, 9.2F, Theme.TEXT, false);
            }

            int infoColor = Theme.TEXT_DIM;
            String info;
            if (ex.state == ImageLibrary.State.READY) {
               info = ex.sizes() + (ex.pixel ? "  ·  pixel" : "  ·  smooth");
            } else if (ex.state == ImageLibrary.State.ERROR) {
               info = ex.error;
               infoColor = Theme.DANGER;
            } else {
               info = "Processing…";
            }

            Ui.textMiddle(Ui.ellipsize(info, 8.2F, right - nx, false), nx, ry + 24.0F, 8.2F, infoColor, false);
            if (pills) {
               this.renamePill.draw(ren, mx, my, dt, accent, false, true);
               this.deletePill.draw(del, mx, my, dt, accent, armed, true);
            }
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         if (this.renaming != null) {
            this.commitRename();
         }

         return false;
      } else if (button != 0) {
         return true;
      } else if (this.importPill.contains(mx, my)) {
         ImageImport.pick(this.library);
         return true;
      } else if (this.folderPill.contains(mx, my)) {
         ImageImport.openFolder(this.library);
         return true;
      } else if (this.reloadPill.contains(mx, my)) {
         this.library.refresh();
         this.library.say("Reloaded the particles folder", false);
         return true;
      } else if (this.shown == null) {
         return true;
      } else {
         for (int i = 0; i < this.shown.size(); i++) {
            ImageLibrary.Entry e = this.shown.get(i);
            float ry = this.rowsY + i * 36.0F;
            if (!(my < ry) && !(my >= ry + 36.0F - 2.0F)) {
               boolean pills = e.name.equals(this.hoverRow) || e.name.equals(this.deleteArmed) || e.name.equals(this.renaming);
               if (pills && this.deletePill.contains(mx, my)) {
                  if (e.name.equals(this.deleteArmed)) {
                     this.deleteArmed = null;
                     if (this.library.delete(e.name)) {
                        this.module.critImages.remove(e.name);
                        this.module.sharpImages.remove(e.name);
                     }
                  } else {
                     this.deleteArmed = e.name;
                     this.deleteUntil = System.currentTimeMillis() + 3000L;
                  }

                  return true;
               }

               if (pills && this.renamePill.contains(mx, my)) {
                  if (e.name.equals(this.renaming)) {
                     this.commitRename();
                  } else {
                     this.renaming = e.name;
                     int dot = e.name.lastIndexOf(46);
                     this.renameText = dot > 0 ? e.name.substring(0, dot) : e.name;
                     this.caret = 0.0F;
                  }

                  return true;
               }

               if (this.renaming != null && !e.name.equals(this.renaming)) {
                  this.commitRename();
               }

               return true;
            }
         }

         return true;
      }
   }

   private void commitRename() {
      String from = this.renaming;
      this.renaming = null;
      if (from != null) {
         String to = this.library.rename(from, this.renameText);
         if (to != null && !to.equals(from)) {
            this.module.critImages.rename(from, to);
            this.module.sharpImages.rename(from, to);
         }
      }
   }

   @Override
   public boolean keyTyped(char c, int key) {
      if (this.renaming == null) {
         return false;
      } else {
         if (key == 1) {
            this.renaming = null;
         } else if (key == 28 || key == 156) {
            this.commitRename();
         } else if (key == 14) {
            if (!this.renameText.isEmpty()) {
               this.renameText = this.renameText.substring(0, this.renameText.length() - 1);
            }
         } else if (GuiScreen.isKeyComboCtrlV(key)) {
            this.append(GuiScreen.getClipboardString());
         } else if (GuiScreen.isKeyComboCtrlA(key)) {
            this.renameText = "";
         } else {
            this.append(String.valueOf(c));
         }

         this.caret = 0.0F;
         return true;
      }
   }

   private void append(String s) {
      if (s != null) {
         for (char ch : s.toCharArray()) {
            if (ch >= ' ' && ch != 127 && "\\/:*?\"<>|".indexOf(ch) < 0 && this.renameText.length() < 64) {
               this.renameText = this.renameText + ch;
            }
         }
      }
   }

   @Override
   public boolean isFocused() {
      return this.renaming != null;
   }

   @Override
   public void unfocus() {
      if (this.renaming != null) {
         this.commitRename();
      }
   }

   @Override
   public void searchItems(SearchList.Items out) {
      for (ImageLibrary.Entry e : this.library.entries()) {
         out.item(e.name, e.name, null);
      }
   }

   @Override
   public void searchFilter(Set<Object> keys) {
      this.filter = keys;
   }

   @Override
   public float reveal(Object key) {
      List<ImageLibrary.Entry> all = this.library.entries();

      for (int i = 0; i < all.size(); i++) {
         if (all.get(i).name.equals(key)) {
            return 26.0F + this.messageHeight() + i * 36.0F;
         }
      }

      return -1.0F;
   }

   @Override
   public float revealHeight(Object key) {
      return 34.0F;
   }

   @Override
   public int searchVersion() {
      int v = 1;

      for (ImageLibrary.Entry e : this.library.entries()) {
         v = v * 31 + e.name.hashCode() + (e.state == ImageLibrary.State.READY ? 1 : 0);
      }

      return v;
   }

   @Override
   public String tooltip(float mx, float my) {
      if (this.importPill.contains(mx, my)) {
         return "Pick PNG, JPG, GIF (first frame) or BMP images, up to 25 MB (big ones are scaled down); they are copied into the particles folder";
      } else if (this.folderPill.contains(mx, my)) {
         return "config/furiahsvisuals/particles: drop images there, then press Reload";
      } else if (this.reloadPill.contains(mx, my)) {
         return "Looks at the particles folder again";
      } else {
         if (this.shown != null && this.hoverRow != null && !this.deletePill.contains(mx, my) && !this.renamePill.contains(mx, my)) {
            for (ImageLibrary.Entry e : this.shown) {
               if (e.name.equals(this.hoverRow) && e.state == ImageLibrary.State.ERROR && e.error != null) {
                  return e.error;
               }
            }
         }

         return null;
      }
   }
}
