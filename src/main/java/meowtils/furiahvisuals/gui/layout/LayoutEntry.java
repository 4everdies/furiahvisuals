package meowtils.furiahvisuals.gui.layout;

import meowtils.furiahvisuals.setting.Setting;

public final class LayoutEntry {
   public final Setting setting;
   public final LayoutPage link;
   public final int depth;
   public final Setting parent;
   LayoutGroup group;

   LayoutEntry(Setting setting, LayoutPage link, int depth, Setting parent) {
      this.setting = setting;
      this.link = link;
      this.depth = depth;
      this.parent = parent;
   }

   public LayoutGroup group() {
      return this.group;
   }

   public LayoutPage page() {
      return this.group.page;
   }

   @Override
   public String toString() {
      return this.setting != null ? this.setting.getId() : "> " + this.link.key;
   }
}
