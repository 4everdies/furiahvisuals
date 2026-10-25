package meowtils.furiahvisuals.gui.layout;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.NoteSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;

public final class SettingsMap {
   private SettingsMap() {
   }

   public static List<String> write(List<Module> modules, File out) throws IOException {
      ArrayList<String> problems = new ArrayList<>();
      StringBuilder body = new StringBuilder();
      StringBuilder table = new StringBuilder();
      table.append("| Section | Before | Rows | In the page header | Kept by a control | After |\n");
      table.append("|---|---:|---:|---:|---:|---:|\n");
      int before = 0;
      int rows = 0;
      int header = 0;
      int data = 0;

      for (Module m : modules) {
         SectionLayout l = SectionLayouts.of(m);
         int b = m.getSettings().size();
         int r = l.entries.size();
         int h = 1;
         int d = l.noRow.size() - 1;
         before += b;
         rows += r;
         header += h;
         data += d;
         if (r + h + d != b) {
            problems.add(m.getId() + ": " + b + " settings before, " + (r + h + d) + " after");
         }

         for (Setting s : l.unplanned) {
            problems.add(m.getId() + ": " + s.getId() + " is not in the plan");
         }

         checkGroups(m, l, problems);
         table.append("| ")
            .append(m.getName())
            .append(" | ")
            .append(b)
            .append(" | ")
            .append(r)
            .append(" | ")
            .append(h)
            .append(" | ")
            .append(d)
            .append(" | ")
            .append(r + h + d)
            .append(" |\n");
         section(m, l, body);
      }

      table.append("| **All** | **")
         .append(before)
         .append("** | ")
         .append(rows)
         .append(" | ")
         .append(header)
         .append(" | ")
         .append(data)
         .append(" | **")
         .append(rows + header + data)
         .append("** |\n");
      StringBuilder sb = new StringBuilder();
      sb.append("# Settings map\n\n");
      sb.append("How the menu shows every section's settings (since 1.25.0). Made from the menu's layout tables ")
         .append("(`gui/layout/Plans.java`) by `gradlew runDevClient -Pselftest=layout`; do not edit by hand.\n\n");
      sb.append("Every page uses the same order: **Main** (the style picker) · **Colors** · **Size & strength** · **Motion** · ")
         .append("**When it shows** · **Style options** (the selected style's own) · the section's **feature groups** · **Extras** · ")
         .append("**Advanced** (folded at first, with a count). Inside a group the most used come first, and a switch's options ")
         .append("follow it, indented (written here as `Switch (+ Option, Option)`). Big features open as their own sub-page (›).\n\n");
      sb.append("Only the menu changed: setting ids, configs, presets and codes are exactly as before, and the search finds every ")
         .append("setting (by its new name and its old one).\n\n");
      sb.append("## Counts\n\n").append((CharSequence)table).append('\n');
      sb.append("*Before*: the section's settings. *Rows*: settings with their own row in the menu. *In the page header*: the ")
         .append("section's on/off. *Kept by a control*: data with no row of its own (the order of the WaterMark's modules, ")
         .append("Move HUD's positions, each Ambience style's on/off on its card...).\n\n");
      sb.append((CharSequence)body);

      try (OutputStreamWriter w = new OutputStreamWriter(Files.newOutputStream(out.toPath()), StandardCharsets.UTF_8)) {
         w.write(sb.toString());
      }

      return problems;
   }

   private static void checkGroups(Module m, SectionLayout l, List<String> problems) {
      for (LayoutPage page : l.pages) {
         LinkedHashSet<SettingStyle> styles = new LinkedHashSet<>();

         for (LayoutEntry e : entriesOf(page)) {
            if (e.setting != null && e.setting.getOwner() != null) {
               styles.add(e.setting.getOwner());
            }
         }

         if (styles.isEmpty()) {
            styles.add(null);
         }

         for (SettingStyle st : styles) {
            int groups = 0;

            for (LayoutGroup g : page.groups) {
               if (shownWith(g, st)) {
                  groups++;
               }
            }

            if (groups > 8) {
               problems.add(m.getId() + " " + page + (st != null ? " (" + st.styleName() + ")" : "") + ": " + groups + " groups");
            }
         }
      }
   }

   private static boolean shownWith(LayoutGroup g, SettingStyle st) {
      for (LayoutEntry e : g.entries) {
         if (e.setting == null || e.setting.getOwner() == null || e.setting.getOwner() == st) {
            return true;
         }
      }

      return false;
   }

   private static List<LayoutEntry> entriesOf(LayoutPage page) {
      ArrayList<LayoutEntry> out = new ArrayList<>();

      for (LayoutGroup g : page.groups) {
         out.addAll(g.entries);
      }

      return out;
   }

   private static void section(Module m, SectionLayout l, StringBuilder sb) {
      int b = m.getSettings().size();
      sb.append("## ").append(m.getName()).append("\n\n");
      sb.append("**")
         .append(b)
         .append(" settings before, ")
         .append(l.entries.size() + l.noRow.size())
         .append(" after** (")
         .append(l.entries.size())
         .append(" rows, ")
         .append(l.noRow.size())
         .append(" without a row of their own).\n\n");
      int styleCount = styleCount(l);
      if (l.tabbed()) {
         List<LayoutPage> tabs = l.tabPages();
         sb.append("Tabs: ");

         for (int i = 0; i < tabs.size(); i++) {
            sb.append(i == 0 ? "" : " · ").append(tabs.get(i).title).append(" (").append(tabs.get(i).count()).append(')');
         }

         sb.append(".\n\n");
      }

      for (LayoutPage page : l.pages) {
         if (page.style == null && (!l.tabbed() || !page.isSection())) {
            String heading = page.isSection()
               ? "Section page"
               : (
                  page.isTab()
                     ? "Tab: " + page.title + " (" + page.count() + " settings)"
                     : (
                        page.parent != null && page.parent.isTab()
                           ? "Sub-page: " + page.title + " (from the " + page.parent.title + " tab)"
                           : "Sub-page: " + page.title
                     )
               );
            sb.append("### ").append(heading).append("\n\n");
            if (!page.isSection() && page.description != null) {
               sb.append(page.description).append("\n\n");
            }

            groups(page, styleCount, sb);
         }
      }

      ArrayList<LayoutPage> stylePages = new ArrayList<>();

      for (LayoutPage pagex : l.pages) {
         if (pagex.style != null) {
            stylePages.add(pagex);
         }
      }

      if (!stylePages.isEmpty()) {
         sb.append("### Each style's page (opened from its card)\n\n");
         LinkedHashMap<String, ArrayList<String>> byGroup = new LinkedHashMap<>();

         for (LayoutPage layoutPage : stylePages) {
            for (LayoutGroup g : layoutPage.groups) {
               ArrayList<String> list = byGroup.get(g.title);
               if (list == null) {
                  list = new ArrayList<>();
                  byGroup.put(g.title, list);
               }

               list.add(layoutPage.title + ": " + join(g.entries));
            }
         }

         for (Entry<String, ArrayList<String>> entry : byGroup.entrySet()) {
            sb.append("- **").append((String)entry.getKey()).append("**\n");

            for (String line : entry.getValue()) {
               sb.append("  - ").append(line).append('\n');
            }
         }

         sb.append('\n');
      }

      changes(m, l, sb);
      sb.append("**Without a row of their own**\n\n");
      LinkedHashMap<String, ArrayList<String>> why = new LinkedHashMap<>();

      for (Entry<Setting, String> entry : l.noRow.entrySet()) {
         ArrayList<String> list = why.get(entry.getValue());
         if (list == null) {
            list = new ArrayList<>();
            why.put(entry.getValue(), list);
         }

         list.add("`" + ((Setting)entry.getKey()).getId() + "`");
      }

      for (Entry<String, ArrayList<String>> entry : why.entrySet()) {
         sb.append("- ").append(capitalize(entry.getKey())).append(": ").append(joinShort(entry.getValue())).append('\n');
      }

      sb.append('\n');
   }

   private static int styleCount(SectionLayout l) {
      LinkedHashSet<SettingStyle> styles = new LinkedHashSet<>();

      for (Setting s : l.module.getSettings()) {
         if (s.getOwner() != null) {
            styles.add(s.getOwner());
         }
      }

      return styles.size();
   }

   private static void groups(LayoutPage page, int styleCount, StringBuilder sb) {
      int n = 1;

      for (LayoutGroup g : page.groups) {
         int settings = 0;

         for (LayoutEntry e : g.entries) {
            if (e.setting != null) {
               settings++;
            }
         }

         sb.append(n++).append(". **").append(g.title).append("**").append(g.collapsedByDefault() ? " (folded at first)" : "").append(": ");
         new ArrayList();
         LinkedHashMap<SettingStyle, ArrayList<LayoutEntry>> own = new LinkedHashMap<>();
         LinkedHashMap<String, Integer> knobCount = new LinkedHashMap<>();

         for (LayoutEntry ex : g.entries) {
            if (ex.setting != null && ex.setting.getOwner() != null && ex.depth == 0) {
               String k;
               Integer c = knobCount.get(k = Plan.knob(ex.setting) + "|" + ex.setting.getLabel());
               knobCount.put(k, c == null ? 1 : c + 1);
            }
         }

         LinkedHashSet<String> listed = new LinkedHashSet<>();
         ArrayList<String> parts = new ArrayList<>();

         for (int i = 0; i < g.entries.size(); i++) {
            LayoutEntry exx = g.entries.get(i);
            if (exx.depth <= 0) {
               if (exx.link != null) {
                  parts.add(exx.link.title + " › (sub-page, " + exx.link.count() + " settings)");
               } else {
                  Setting s = exx.setting;
                  SettingStyle o = s.getOwner();
                  String string = Plan.knob(s) + "|" + s.getLabel();
                  int count = o == null ? 1 : knobCount.get(string);
                  boolean common = o != null && count > 1 && count * 2 >= styleCount && (styleCount > 6 || !o.isMultiple());
                  if (o != null && !common) {
                     ArrayList<LayoutEntry> list = own.get(o);
                     if (list == null) {
                        list = new ArrayList<>();
                        own.put(o, list);
                     }

                     list.add(exx);
                  } else if (o != null) {
                     if (listed.add(string)) {
                        parts.add(describe(g, i) + (count == styleCount ? " (every " + kindOf(o) + ")" : " (" + count + " " + kindsOf(o) + ")"));
                     }
                  } else {
                     parts.add(describe(g, i));
                  }
               }
            }
         }

         int common = 0;
         LinkedHashMap<SettingStyle, Integer> perStyle = new LinkedHashMap<>();

         for (LayoutEntry exx : g.entries) {
            if (exx.setting != null) {
               SettingStyle settingStyle = exx.setting.getOwner();
               if (settingStyle == null) {
                  common++;
               } else {
                  Integer c = perStyle.get(settingStyle);
                  perStyle.put(settingStyle, c == null ? 1 : c + 1);
               }
            }
         }

         int most = 0;

         for (int n2 : perStyle.values()) {
            most = Math.max(most, n2);
         }

         sb.append(settings).append(settings == 1 ? " setting" : " settings");
         if (common + most != settings) {
            sb.append(", up to ").append(common + most).append(" at a time");
         }

         sb.append("\n");
         if (!parts.isEmpty()) {
            sb.append("   - ").append(joinParts(parts)).append('\n');
         }

         for (Entry<SettingStyle, ArrayList<LayoutEntry>> entry : own.entrySet()) {
            ArrayList<String> mine = new ArrayList<>();

            for (LayoutEntry le : entry.getValue()) {
               mine.add(describe(g, g.entries.indexOf(le)));
            }

            sb.append("   - ").append(((SettingStyle)entry.getKey()).styleName()).append(": ").append(joinParts(mine)).append('\n');
         }
      }

      sb.append('\n');
   }

   private static String describe(LayoutGroup g, int i) {
      LayoutEntry e = g.entries.get(i);
      StringBuilder sb = new StringBuilder(name(e.setting));
      ArrayList<String> options = new ArrayList<>();

      for (int k = i + 1; k < g.entries.size() && g.entries.get(k).depth > e.depth; k++) {
         if (g.entries.get(k).depth == e.depth + 1) {
            options.add(describe(g, k));
         }
      }

      if (!options.isEmpty()) {
         sb.append(" (+ ").append(join(options, ", ")).append(')');
      }

      return sb.toString();
   }

   private static String name(Setting s) {
      String label = s.getLabel();
      if (label == null || label.isEmpty()) {
         label = s instanceof NoteSetting ? "(a tip)" : s.getId();
      }

      if (s instanceof ModeSetting && ((ModeSetting)s).getStyle() == ModeSetting.Style.CARDS) {
         label = label + " (cards)";
      }

      return label;
   }

   private static String join(List<LayoutEntry> entries) {
      ArrayList<String> parts = new ArrayList<>();
      LayoutGroup g = entries.isEmpty() ? null : entries.get(0).group;

      for (int i = 0; i < entries.size(); i++) {
         if (entries.get(i).depth == 0) {
            parts.add(entries.get(i).setting != null ? describe(g, i) : entries.get(i).link.title + " ›");
         }
      }

      return joinParts(parts);
   }

   private static String joinParts(List<String> parts) {
      return join(parts, " · ");
   }

   private static String join(List<String> parts, String sep) {
      StringBuilder sb = new StringBuilder();

      for (String p : parts) {
         if (sb.length() > 0) {
            sb.append(sep);
         }

         sb.append(p);
      }

      return sb.toString();
   }

   private static String joinShort(List<String> ids) {
      return ids.size() <= 8 ? join(ids, ", ") : join(ids.subList(0, 6), ", ") + " and " + (ids.size() - 6) + " more";
   }

   private static String kindOf(SettingStyle o) {
      return o.kind();
   }

   private static String kindsOf(SettingStyle o) {
      return o.kinds();
   }

   private static String capitalize(String s) {
      return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
   }

   private static void changes(Module m, SectionLayout l, StringBuilder sb) {
      LinkedHashMap<String, LinkedHashSet<String>> moves = new LinkedHashMap<>();
      ArrayList<String> toAdvanced = new ArrayList<>();
      IdentityHashMap<SettingStyle, Boolean> styleGroups = new IdentityHashMap<>();

      for (Setting setting : m.getSettings()) {
         LayoutEntry layoutEntry = l.entries.get(setting);
         if (layoutEntry != null) {
            String old = setting.getGroup() != null && !setting.getGroup().isEmpty() ? setting.getGroup() : "(no group)";
            SettingStyle o = setting.getOwner();
            if (o != null && old.equals(o.styleName())) {
               old = "Each " + o.kind() + "'s own group";
               styleGroups.put(o, Boolean.TRUE);
            }

            String now = (!layoutEntry.page().isSection() && layoutEntry.page().style == null ? layoutEntry.page().title + " › " : "")
               + layoutEntry.group.title;
            if (!old.equalsIgnoreCase(layoutEntry.group.title)) {
               LinkedHashSet<String> to = moves.get(old);
               if (to == null) {
                  to = new LinkedHashSet<>();
                  moves.put(old, to);
               }

               to.add(now);
            }

            if (layoutEntry.group.slot == Slot.ADVANCED && !"Advanced".equalsIgnoreCase(old) && layoutEntry.depth == 0) {
               toAdvanced.add(setting.getLabel());
            }
         }
      }

      sb.append("**Merged and moved**\n\n");
      if (moves.isEmpty()) {
         sb.append("- Nothing: the groups stayed as they were.\n");
      }

      for (Entry entry : moves.entrySet()) {
         sb.append("- ")
            .append((String)entry.getKey())
            .append(" → ")
            .append(join(new ArrayList<>((Collection<? extends String>)entry.getValue()), ", "))
            .append('\n');
      }

      sb.append('\n');
      if (!toAdvanced.isEmpty()) {
         sb.append("**Moved to Advanced**: ").append(join(new ArrayList<>(new LinkedHashSet<>(toAdvanced)), ", ")).append("\n\n");
      }

      if (!l.renamed.isEmpty()) {
         LinkedHashSet<String> names = new LinkedHashSet<>();

         for (Entry<Setting, String> entry : l.renamed.entrySet()) {
            names.add("“" + entry.getValue() + "” → “" + entry.getKey().getLabel() + "”");
         }

         sb.append("**Renamed** (the search still finds the old name): ").append(join(new ArrayList<>(names), ", ")).append("\n\n");
      }
   }
}
