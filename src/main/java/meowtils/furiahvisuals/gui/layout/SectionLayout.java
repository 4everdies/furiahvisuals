package meowtils.furiahvisuals.gui.layout;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;

public final class SectionLayout {
   public final Module module;
   public final List<LayoutPage> pages = new ArrayList<>();
   final Map<Setting, LayoutEntry> entries = new IdentityHashMap<>();
   public final Map<Setting, String> noRow = new LinkedHashMap<>();
   public final List<Setting> unplanned = new ArrayList<>();
   final Map<Setting, String> renamed = new LinkedHashMap<>();

   SectionLayout(Module module) {
      this.module = module;
   }

   public LayoutPage sectionPage() {
      return this.pages.get(0);
   }

   public LayoutPage page(String key) {
      for (LayoutPage p : this.pages) {
         if (p.key.equals(key)) {
            return p;
         }
      }

      return null;
   }

   public LayoutPage stylePage(SettingStyle style) {
      for (LayoutPage p : this.pages) {
         if (style != null && (p.style == style || p.tab == style)) {
            return p;
         }
      }

      return null;
   }

   public List<LayoutPage> tabPages() {
      ArrayList<LayoutPage> out = new ArrayList<>();

      for (LayoutPage p : this.pages) {
         if (p.tab != null) {
            out.add(p);
         }
      }

      return out;
   }

   public boolean tabbed() {
      for (LayoutPage p : this.pages) {
         if (p.tab != null) {
            return true;
         }
      }

      return false;
   }

   public LayoutPage firstPage() {
      for (LayoutPage p : this.pages) {
         if (p.tab != null) {
            return p;
         }
      }

      return this.pages.get(0);
   }

   public LayoutEntry entry(Setting s) {
      return this.entries.get(s);
   }

   public List<LayoutEntry> allEntries() {
      ArrayList<LayoutEntry> out = new ArrayList<>();

      for (LayoutPage p : this.pages) {
         for (LayoutGroup g : p.groups) {
            out.addAll(g.entries);
         }
      }

      return out;
   }

   public Map<Setting, String> renamed() {
      return this.renamed;
   }
}
