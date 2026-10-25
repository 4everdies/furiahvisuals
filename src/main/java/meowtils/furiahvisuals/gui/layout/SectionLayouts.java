package meowtils.furiahvisuals.gui.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;

public final class SectionLayouts {
   private static final Map<Module, SectionLayout> LAYOUTS = new IdentityHashMap<>();
   private static final Map<Setting, LayoutEntry> ENTRIES = new IdentityHashMap<>();

   private SectionLayouts() {
   }

   public static SectionLayout of(Module m) {
      SectionLayout l = LAYOUTS.get(m);
      if (l == null) {
         long t0 = System.nanoTime();
         l = build(m);
         LAYOUTS.put(m, l);
         ENTRIES.putAll(l.entries);
         if (!l.unplanned.isEmpty()) {
            FuriahsVisuals.LOG.warn("Menu layout of {}: {} settings are not in its plan (shown under Advanced)", new Object[]{m.getId(), l.unplanned.size()});
         }

         FuriahsVisuals.LOG.debug("Menu layout of {} made in {} ms", new Object[]{m.getId(), (System.nanoTime() - t0) / 1000000L});
      }

      return l;
   }

   public static SectionLayout fresh(Module m) {
      return build(m);
   }

   public static LayoutEntry entryOf(Setting s) {
      return ENTRIES.get(s);
   }

   public static String groupOf(Setting s) {
      LayoutEntry e = ENTRIES.get(s);
      return e == null ? null : e.group.path();
   }

   static SectionLayout build(Module m) {
      Plan candidate = Plans.of(m.getId());
      final Plan plan = candidate == null ? declaredPlan(m) : candidate;

      SectionLayout layout = new SectionLayout(m);
      ArrayList<Setting> all = new ArrayList<>();

      for (Setting s : m.getSettings()) {
         if (s != m.getEnabledSetting()) {
            all.add(s);
         }
      }

      IdentityHashMap<Setting, Boolean> eligible = new IdentityHashMap<>();
      ArrayList<Plan.Pattern> byId = new ArrayList<>();
      collectById(plan.main, byId);

      for (Plan.PlanPage sp : plan.subs) {
         collectById(sp, byId);
      }

      for (Setting sx : all) {
         if (sx.showInMenu()) {
            eligible.put(sx, Boolean.TRUE);
         } else {
            for (Plan.Pattern p : byId) {
               if (p.matches(sx)) {
                  eligible.put(sx, Boolean.TRUE);
                  break;
               }
            }
         }
      }

      Map<Setting, Setting> found = Dependencies.find(all, new Predicate<Setting>() {
         public boolean test(Setting s) {
            for (Plan.Pattern px : plan.notSwitches) {
               if (px.matches(s)) {
                  return true;
               }
            }

            return false;
         }
      });

      for (String[] o : plan.options) {
         Plan.Pattern option = new Plan.Pattern(o[0]);
         Setting sw = null;

         for (Setting sxx : all) {
            if (sxx.getId().equals(o[1])) {
               sw = sxx;
            }
         }

         for (Setting sxxx : all) {
            if (sw != null && sxxx != sw && option.matches(sxxx)) {
               found.put(sxxx, sw);
            }
         }
      }

      IdentityHashMap<Setting, Setting> parentOf = new IdentityHashMap<>();
      IdentityHashMap<Setting, List<Setting>> childrenOf = new IdentityHashMap<>();

      for (Setting sxxxx : all) {
         Setting px = found.get(sxxxx);
         if (px != null
            && eligible.containsKey(sxxxx)
            && eligible.containsKey(px)
            && (plan.stylePage == null || sxxxx.getOwner() == null || px.getOwner() != null)) {
            parentOf.put(sxxxx, px);
            List<Setting> list = childrenOf.get(px);
            if (list == null) {
               list = new ArrayList<>();
               childrenOf.put(px, list);
            }

            list.add(sxxxx);
         }
      }

      LayoutPage section = new LayoutPage("", m.getName(), null, null, null, m.getDescription());
      section.headerless = plan.headerless;
      layout.pages.add(section);
      ArrayList<SectionLayouts.Rule> rules = new ArrayList<>();
      LinkedHashMap<String, LayoutPage> byKey = new LinkedHashMap<>();
      addGroups(plan.main, section, null, false, rules);

      for (Plan.PlanPage planPage : plan.subs) {
         MiniSection tab = planPage.tab && m.getTabs() != null ? m.getTabs().byId(planPage.key) : null;
         if (planPage.tab && tab == null) {
            FuriahsVisuals.LOG.warn("Menu layout of {}: no mini section '{}' for its tab", new Object[]{m.getId(), planPage.key});
         }

         LayoutPage parent = planPage.parent != null && byKey.containsKey(planPage.parent.key) ? byKey.get(planPage.parent.key) : section;
         LayoutPage page = new LayoutPage(planPage.key, planPage.title, parent, null, null, planPage.description, tab);
         page.headerless = planPage.headerless;
         layout.pages.add(page);
         byKey.put(planPage.key, page);
         LayoutPage on = page.tabPage();
         addGroups(planPage, page, on == null ? null : on.tab, on != null, rules);
      }

      boolean stylePages = plan.stylePage != null;
      if (stylePages) {
         for (SettingStyle st : styles(all)) {
            LayoutPage page = new LayoutPage("style:" + st.styleName(), st.styleName(), section, st, styleSwitch(m, all, st), null);
            layout.pages.add(page);
            addGroups(plan.stylePage, page, st, true, rules);
         }
      }

      ArrayList<SectionLayouts.Rule> arrayList = new ArrayList<>(rules);
      Collections.sort(arrayList, new Comparator<SectionLayouts.Rule>() {
         public int compare(SectionLayouts.Rule a, SectionLayouts.Rule b) {
            if (a.pattern.catchAll() != b.pattern.catchAll()) {
               return a.pattern.catchAll() ? 1 : -1;
            } else {
               return a.pattern.seq - b.pattern.seq;
            }
         }
      });
      IdentityHashMap<Setting, Boolean> placed = new IdentityHashMap<>();

      for (SectionLayouts.Rule r : arrayList) {
         if (r.pattern.kind != Plan.Pattern.Kind.LINK) {
            for (Setting setting : all) {
               if (!placed.containsKey(setting)
                  && eligible.containsKey(setting)
                  && !parentOf.containsKey(setting)
                  && (setting.showInMenu() || r.pattern.byId())
                  && (!r.styleScoped ? !stylePages || setting.getOwner() == null : setting.getOwner() == r.style)
                  && r.pattern.matches(setting)) {
                  r.matched.add(setting);
                  placed.put(setting, Boolean.TRUE);
               }
            }
         }
      }

      for (Setting sxxxxx : all) {
         if (eligible.containsKey(sxxxxx) && !parentOf.containsKey(sxxxxx) && !placed.containsKey(sxxxxx)) {
            layout.unplanned.add(sxxxxx);
            placed.put(sxxxxx, Boolean.TRUE);
         }
      }

      for (LayoutPage page : layout.pages) {
         for (LayoutGroup layoutGroup : page.groups) {
            for (SectionLayouts.Rule rx : rules) {
               if (rx.group == layoutGroup) {
                  if (rx.pattern.kind == Plan.Pattern.Kind.LINK) {
                     LayoutPage target = byKey.get(rx.pattern.a);
                     if (target != null) {
                        add(layoutGroup, new LayoutEntry(null, target, 0, null));
                     }
                  } else {
                     for (Setting sxxxxxx : rx.matched) {
                        addTree(layout, layoutGroup, sxxxxxx, 0, null, childrenOf);
                     }
                  }
               }
            }
         }
      }

      for (Setting sxxxxxx : layout.unplanned) {
         LayoutPage home = section;

         for (LayoutPage px : layout.pages) {
            if (px.tab != null && px.tab == sxxxxxx.getOwner()) {
               home = px;
            }
         }

         addTree(layout, advancedOf(home), sxxxxxx, 0, null, childrenOf);
      }

      boolean changed = true;

      while (changed) {
         changed = false;

         for (LayoutPage page : layout.pages) {
            for (int i = page.groups.size() - 1; i >= 0; i--) {
               LayoutGroup g = page.groups.get(i);

               for (int ei = g.entries.size() - 1; ei >= 0; ei--) {
                  LayoutEntry e = g.entries.get(ei);
                  if (e.link != null && e.link.groups.isEmpty()) {
                     g.entries.remove(ei);
                     changed = true;
                  }
               }

               if (g.entries.isEmpty()) {
                  page.groups.remove(i);
                  changed = true;
               }
            }
         }

         for (int pi = layout.pages.size() - 1; pi >= 1; pi--) {
            if (layout.pages.get(pi).groups.isEmpty()) {
               layout.pages.remove(pi);
               changed = true;
            }
         }
      }

      for (Setting sxxxxxx : all) {
         for (String[] label : plan.labels) {
            if (new Plan.Pattern(label[0]).matches(sxxxxxx)) {
               if (!label[1].equals(sxxxxxx.getName())) {
                  layout.renamed.put(sxxxxxx, sxxxxxx.getName());
               }

               sxxxxxx.setLabel(label[1]);
               break;
            }
         }
      }

      layout.noRow.put(m.getEnabledSetting(), "the section's on/off switch, in the page's header");

      for (Setting sxxxxxx : all) {
         if (!layout.entries.containsKey(sxxxxxx)) {
            String why = null;

            for (String[] d : plan.data) {
               if (new Plan.Pattern(d[0]).matches(sxxxxxx)) {
                  why = d[1];
                  break;
               }
            }

            layout.noRow.put(sxxxxxx, why != null ? why : "kept by the section's own controls");
         }
      }

      return layout;
   }

   private static void collectById(Plan.PlanPage page, List<Plan.Pattern> out) {
      for (Plan.PlanGroup g : page.groups) {
         for (Plan.Pattern p : g.patterns) {
            if (p.byId()) {
               out.add(p);
            }
         }
      }
   }

   private static void addGroups(Plan.PlanPage planPage, LayoutPage page, SettingStyle style, boolean styleScoped, List<SectionLayouts.Rule> rules) {
      for (Plan.PlanGroup pg : planPage.groups) {
         LayoutGroup g = new LayoutGroup(pg.slot, pg.title);
         g.page = page;
         page.groups.add(g);

         for (Plan.Pattern p : pg.patterns) {
            rules.add(new SectionLayouts.Rule(p, g, style, styleScoped));
         }
      }
   }

   private static LayoutGroup advancedOf(LayoutPage page) {
      for (LayoutGroup g : page.groups) {
         if (g.slot == Slot.ADVANCED) {
            return g;
         }
      }

      LayoutGroup gx = new LayoutGroup(Slot.ADVANCED, Slot.ADVANCED.title);
      gx.page = page;
      page.groups.add(gx);
      return gx;
   }

   private static void add(LayoutGroup g, LayoutEntry e) {
      e.group = g;
      g.entries.add(e);
   }

   private static void addTree(SectionLayout layout, LayoutGroup g, Setting s, int depth, Setting parent, Map<Setting, List<Setting>> childrenOf) {
      if (!layout.entries.containsKey(s)) {
         LayoutEntry e = new LayoutEntry(s, null, depth, parent);
         add(g, e);
         layout.entries.put(s, e);
         List<Setting> children = childrenOf.get(s);
         if (children != null) {
            for (Setting c : children) {
               addTree(layout, g, c, Math.min(depth + 1, 2), s, childrenOf);
            }
         }
      }
   }

   private static List<SettingStyle> styles(List<Setting> all) {
      ArrayList<SettingStyle> out = new ArrayList<>();

      for (Setting s : all) {
         SettingStyle o = s.getOwner();
         if (o != null && !out.contains(o)) {
            out.add(o);
         }
      }

      return out;
   }

   private static BooleanSetting styleSwitch(Module m, List<Setting> all, SettingStyle st) {
      for (Setting s : all) {
         if (s.getOwner() == st) {
            int dot = s.getId().indexOf(46);
            if (dot > 0) {
               String id = s.getId().substring(0, dot) + ".on";

               for (Setting o : all) {
                  if (o.getId().equals(id) && o instanceof BooleanSetting) {
                     return (BooleanSetting)o;
                  }
               }
            }

            return null;
         }
      }

      return null;
   }

   private static Plan declaredPlan(Module m) {
      Plan p = new Plan();
      Plan.PlanGroup none = p.main.main();
      ArrayList<String> groups = new ArrayList<>();

      for (Setting s : m.getSettings()) {
         if (s.getGroup() != null && !groups.contains(s.getGroup())) {
            groups.add(s.getGroup());
         }
      }

      for (String g : groups) {
         p.main.feature(g).add("@" + g);
      }

      none.add("**");
      return p;
   }

   private static final class Rule {
      final Plan.Pattern pattern;
      final LayoutGroup group;
      final SettingStyle style;
      final boolean styleScoped;
      final List<Setting> matched = new ArrayList<>();

      Rule(Plan.Pattern pattern, LayoutGroup group, SettingStyle style, boolean styleScoped) {
         this.pattern = pattern;
         this.group = group;
         this.style = style;
         this.styleScoped = styleScoped;
      }
   }
}
