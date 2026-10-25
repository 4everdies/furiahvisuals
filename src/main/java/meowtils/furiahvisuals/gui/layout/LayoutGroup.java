package meowtils.furiahvisuals.gui.layout;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.setting.Setting;

public final class LayoutGroup {
   public final Slot slot;
   public final String title;
   public final List<LayoutEntry> entries = new ArrayList<>();
   LayoutPage page;

   LayoutGroup(Slot slot, String title) {
      this.slot = slot;
      this.title = title;
   }

   public LayoutPage page() {
      return this.page;
   }

   public boolean collapsedByDefault() {
      return this.slot == Slot.ADVANCED;
   }

   public boolean showsCount() {
      return this.slot == Slot.ADVANCED;
   }

   public String path() {
      String up = this.page.path();
      if (up == null) {
         return this.title;
      } else {
         return this.slot != Slot.MAIN && this.page.groups.size() != 1 ? up + " › " + this.title : up;
      }
   }

   public boolean hasHeader() {
      return !this.page.headerless && (this.page.isSection() || this.page.groups.size() > 1);
   }

   public String foldKey(String moduleId) {
      return moduleId + ":" + (this.page.key.isEmpty() ? "" : this.page.key + ":") + this.title;
   }

   public List<Setting> members() {
      ArrayList<Setting> out = new ArrayList<>();

      for (LayoutEntry e : this.entries) {
         if (e.setting != null) {
            out.add(e.setting);
         } else {
            out.addAll(e.link.settings());
         }
      }

      return out;
   }

   @Override
   public String toString() {
      return this.title + this.entries;
   }
}
