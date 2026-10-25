package meowtils.furiahvisuals.gui.layout;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;

public final class LayoutPage {
   public final String key;
   public final String title;
   public final LayoutPage parent;
   public final SettingStyle style;
   public final BooleanSetting styleSwitch;
   public final String description;
   public final MiniSection tab;
   public final List<LayoutGroup> groups = new ArrayList<>();
   boolean headerless;

   LayoutPage(String key, String title, LayoutPage parent, SettingStyle style, BooleanSetting styleSwitch, String description) {
      this(key, title, parent, style, styleSwitch, description, null);
   }

   LayoutPage(String key, String title, LayoutPage parent, SettingStyle style, BooleanSetting styleSwitch, String description, MiniSection tab) {
      this.key = key;
      this.title = title;
      this.parent = parent;
      this.style = style;
      this.styleSwitch = styleSwitch;
      this.description = description;
      this.tab = tab;
   }

   public boolean isSection() {
      return this.parent == null;
   }

   public boolean isTab() {
      return this.tab != null;
   }

   public LayoutPage tabPage() {
      for (LayoutPage p = this; p != null; p = p.parent) {
         if (p.tab != null) {
            return p;
         }
      }

      return null;
   }

   public String path() {
      if (this.parent == null) {
         return null;
      } else {
         String up = this.parent.path();
         return up == null ? this.title : up + " › " + this.title;
      }
   }

   public List<Setting> settings() {
      ArrayList<Setting> out = new ArrayList<>();

      for (LayoutGroup g : this.groups) {
         out.addAll(g.members());
      }

      return out;
   }

   public int count() {
      return this.settings().size();
   }

   @Override
   public String toString() {
      return this.key.isEmpty() ? "(section)" : this.key;
   }
}
