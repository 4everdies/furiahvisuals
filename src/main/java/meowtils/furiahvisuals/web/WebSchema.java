package meowtils.furiahvisuals.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.Keybinds;
import meowtils.furiahvisuals.gui.MenuIcons;
import meowtils.furiahvisuals.gui.MenuPrefs;
import meowtils.furiahvisuals.gui.SectionCategories;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.layout.LayoutEntry;
import meowtils.furiahvisuals.gui.layout.LayoutGroup;
import meowtils.furiahvisuals.gui.layout.LayoutPage;
import meowtils.furiahvisuals.gui.layout.SectionLayout;
import meowtils.furiahvisuals.gui.layout.SectionLayouts;
import meowtils.furiahvisuals.gui.layout.Slot;
import meowtils.furiahvisuals.gui.widget.StartingLookWidget;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.ambience.AmbienceModule;
import meowtils.furiahvisuals.module.trails.TrailsModule;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.module.weapons.WeaponDef;
import meowtils.furiahvisuals.module.weapons.WeaponsModule;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Choices;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.EnumChoices;
import meowtils.furiahvisuals.setting.KeyBindSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.NoteSetting;
import meowtils.furiahvisuals.setting.PresetSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingDefaults;
import meowtils.furiahvisuals.setting.SettingProbe;
import meowtils.furiahvisuals.setting.SettingStyle;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.WidgetSetting;
import meowtils.furiahvisuals.sound.SoundSettings;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

final class WebSchema {
   static final int SCHEMA_VERSION = 1;
   static final int ICON = 128;
   final List<String> problems = new ArrayList<>();
   final Map<String, Object> icons = new LinkedHashMap<>();
   private Map<IslandIcons.Icon, BufferedImage> islandIcons;
   int sectionCount;
   int styleCount;
   int settingCount;
   int ruleCount;
   private static final String[] LOOKS = new String[]{"subtle", "balanced", "intense"};

   JsonObject build() {
      JsonObject root = new JsonObject();
      root.addProperty("schemaVersion", 1);
      root.addProperty("modVersion", "1.3.5");
      List<Module> modules = FuriahsVisuals.instance.getModules().getModules();
      JsonArray categories = new JsonArray();
      JsonArray sections = new JsonArray();

      for (String category : SectionCategories.NAMES) {
         List<Module> in = SectionCategories.in(category, modules);
         if (!in.isEmpty()) {
            JsonObject c = new JsonObject();
            c.addProperty("id", WebValues.id(category));
            c.addProperty("label", category);
            JsonArray ids = new JsonArray();

            for (Module m : in) {
               try {
                  sections.add(this.section(m));
                  ids.add(new JsonPrimitive(WebKeys.section(m)));
                  this.sectionCount++;
               } catch (RuntimeException var15) {
                  this.problems.add(m.getName() + ": not exported (" + var15 + ")");
                  FuriahsVisuals.LOG.warn("Website export: {} failed", new Object[]{m.getName(), var15});
               }
            }

            c.add("sectionIds", ids);
            categories.add(c);
         }
      }

      root.add("categories", categories);
      root.add("sections", sections);
      root.add("settingsPage", this.settingsPage());
      JsonObject special = new JsonObject();
      special.addProperty("guiTheme", "settings.theme");
      special.addProperty("viewTheme", "settings." + MenuPrefs.VIEW_THEME.getId());
      special.addProperty("guiScale", "settings." + MenuPrefs.GUI_SCALE.getId());
      special.addProperty("globalColor", "settings.globalColor");
      special.addProperty("soundTheme", "settings." + FuriahsVisuals.instance.getSounds().theme.getId());
      special.addProperty("muteSounds", "settings." + FuriahsVisuals.instance.getSounds().mute.getId());
      special.addProperty("reduceFlashing", "settings.reduceFlashing");
      special.addProperty("sidebarStyle", "settings." + MenuPrefs.SIDEBAR.getId());
      root.add("specialKeys", special);
      root.add("themes", this.themes());
      return root;
   }

   private JsonObject section(Module m) {
      String id = WebKeys.section(m);
      JsonObject s = new JsonObject();
      s.addProperty("id", id);
      s.addProperty("label", m.getName());
      s.addProperty("icon", this.sectionIcon(m));
      s.addProperty("description", m.getDescription());
      s.addProperty("enabledKey", WebKeys.key(m, m.getEnabledSetting()));
      s.addProperty("enabledByDefault", m.getEnabledSetting().getDefault());
      s.add("headerActions", this.headerActions(m));
      SectionLayout layout = SectionLayouts.fresh(m);
      WebSchema.Owner own = new WebSchema.Owner(m, null, m.getName());
      own.skip.add(m.getEnabledSetting());
      this.findStyles(own, m.getSettings());
      if (layout.tabbed()) {
         JsonArray tabs = new JsonArray();

         for (LayoutPage page : layout.tabPages()) {
            tabs.add(this.tab(m, layout, page));
         }

         s.add("tabs", tabs);
         if (!layout.sectionPage().groups.isEmpty()) {
            this.problems.add(m.getName() + ": its own page has groups besides its tabs; the website shows only the tabs");
         }
      } else {
         LinkedHashSet<LayoutPage> done = new LinkedHashSet<>();
         this.addPage(own, layout, layout.sectionPage(), null, done);

         for (LayoutPage page : layout.pages) {
            if (page.style != null) {
               if (page.styleSwitch != null) {
                  own.skip.add(page.styleSwitch);
               }

               this.addGroups(own, page, null, true);
               done.add(page);
            }
         }

         this.addUnlinked(own, layout, null, done);
         s.add("groups", own.finishedGroups());
         JsonArray looks = this.looks(own);
         if (looks != null) {
            s.add("startingLooks", looks);
         }
      }

      if (own.styles.size() > 0) {
         s.add("styles", own.styles);
         s.add("stylePicker", own.stylePicker);
      }

      return s;
   }

   private JsonObject tab(Module m, SectionLayout layout, LayoutPage page) {
      MiniSection mini = page.tab;
      WebSchema.Owner own = new WebSchema.Owner(m, mini, m.getName() + " › " + page.title);
      own.skip.add(m.getEnabledSetting());
      String id = slug(mini.getName());
      JsonObject t = new JsonObject();
      t.addProperty("id", id);
      t.addProperty("label", mini.getName());
      String icon = WebKeys.section(m) + "-" + id + ".png";
      t.addProperty("icon", icon);
      this.icons.put(icon, this.island(mini.getIcon()));
      if (mini.getSwitch() != null) {
         own.skip.add(mini.getSwitch());
         t.addProperty("enabledKey", WebKeys.key(m, mini.getSwitch()));
         t.addProperty("enabledByDefault", mini.getSwitch().getDefault());
      }

      ArrayList<Setting> mine = new ArrayList<>();

      for (Setting s : m.getSettings()) {
         if (s.getOwner() == mini) {
            mine.add(s);
         }
      }

      this.findStyles(own, mine);
      LinkedHashSet<LayoutPage> done = new LinkedHashSet<>();
      this.addPage(own, layout, page, null, done);
      this.addUnlinked(own, layout, page, done);
      t.add("groups", own.finishedGroups());
      if (own.styles.size() > 0) {
         t.add("styles", own.styles);
         t.add("stylePicker", own.stylePicker);
      }

      return t;
   }

   private JsonArray headerActions(Module m) {
      JsonArray out = new JsonArray();
      out.add(action("reset-section", "Reset section", "reset.png", false));
      this.icons.put("reset.png", new IconExport.Painter() {
         @Override
         public void paint(float size) {
            MenuIcons.reset(size / 2.0F, size / 2.0F, size * 0.34F, -1);
         }
      });
      if (m.hasTestAction()) {
         out.add(action("test", m.getTestLabel(), "test.png", true));
         this.icons.put("test.png", this.island(IslandIcons.Icon.PLAY));
      }

      if (m.hasMoveHud()) {
         out.add(action("move-hud", "Move HUD", "move-hud.png", true));
         this.icons.put("move-hud.png", new IconExport.Painter() {
            @Override
            public void paint(float size) {
               MenuIcons.move(size / 2.0F, size / 2.0F, size * 0.78F, -1);
            }
         });
      }

      for (Module.HeaderAction a : m.headerActions()) {
         String icon = a.icon.name().toLowerCase(Locale.ROOT) + ".png";
         this.icons.put(icon, this.island(a.icon));
         out.add(action(slug(a.label), a.label, icon, true));
      }

      return out;
   }

   private static JsonObject action(String id, String label, String icon, boolean inGameOnly) {
      JsonObject a = new JsonObject();
      a.addProperty("id", id);
      a.addProperty("label", label);
      a.addProperty("icon", icon);
      if (inGameOnly) {
         a.addProperty("inGameOnly", true);
      }

      return a;
   }

   private String sectionIcon(final Module m) {
      String file = WebKeys.section(m) + ".png";
      this.icons.put(file, new IconExport.Painter() {
         @Override
         public void paint(float size) {
            MenuIcons.section(m.getId(), size / 2.0F, size / 2.0F, size * 0.8F, -1);
         }
      });
      return file;
   }

   private BufferedImage island(IslandIcons.Icon icon) {
      if (this.islandIcons == null) {
         this.islandIcons = IslandIcons.images();
      }

      return this.islandIcons.get(icon);
   }

   private void findStyles(WebSchema.Owner o, List<Setting> settings) {
      Module m = o.module;
      if (o.tab == null && m instanceof WeaponsModule) {
         for (WeaponDef d : ((WeaponsModule)m).cardWeapons()) {
            o.style(WebValues.id(d.id), d.name, d.category.displayName, d.description);
         }

         o.pickerKey = WebKeys.section(m) + ".card";
         o.stylePicker = picker(o.pickerKey, "vanilla", null);

         for (Setting s : settings) {
            if (s.getId().equals("cards")) {
               o.skip.add(s);
            }
         }
      } else {
         ModeSetting<?> pick = WebStyles.picker(m, o.tab, settings);
         if (pick != null) {
            for (Enum enum_ : pick.getValues()) {
               String tag = enum_ instanceof Displayable ? ((Displayable)enum_).tag() : null;
               o.style(WebValues.id(enum_.name()), ModeSetting.displayName(enum_), tag, ModeSetting.description(enum_));
            }

            o.picker = pick;
            o.pickerKey = WebKeys.key(m, pick);
            o.skip.add(pick);
            String def = defaultChoice(pick);
            JsonObject combinable = null;
            if (m instanceof TrailsModule || m instanceof AmbienceModule) {
               JsonArray on = new JsonArray();

               for (Enum c : pick.getValues()) {
                  BooleanSetting sw = styleSwitch(m, c);
                  if (sw == null) {
                     this.problems.add(o.path + ": no on/off switch for " + c.name());
                  } else {
                     o.skip.add(sw);
                     if (sw.getDefault()) {
                        on.add(new JsonPrimitive(WebValues.id(c.name())));
                     }
                  }
               }

               combinable = new JsonObject();
               combinable.addProperty("max", m instanceof TrailsModule ? 3 : pick.getValues().length);
               combinable.addProperty("activeKey", WebKeys.section(m) + ".active");
               combinable.add("defaultActive", on);
            }

            o.stylePicker = picker(o.pickerKey, def, combinable);
            if (m instanceof AmbienceModule) {
               for (Setting setting : settings) {
                  if (setting.getId().equals("styles")) {
                     o.skip.add(setting);
                  }
               }
            }
         }
      }
   }

   private static JsonObject picker(String key, String def, JsonObject combinable) {
      JsonObject p = new JsonObject();
      p.addProperty("key", key);
      if (def != null) {
         p.addProperty("default", def);
      }

      if (combinable != null) {
         p.add("combinable", combinable);
      }

      return p;
   }

   static BooleanSetting styleSwitch(Module m, Enum<?> style) {
      String byId = style.name().toLowerCase(Locale.ROOT) + ".on";
      String name = ModeSetting.displayName(style);

      for (Setting s : m.getSettings()) {
         if (s instanceof BooleanSetting && s.getId().endsWith(".on") && (s.getId().equals(byId) || s.getName().equals(name))) {
            return (BooleanSetting)s;
         }
      }

      return null;
   }

   private static String defaultChoice(ModeSetting<?> s) {
      JsonElement d = SettingDefaults.get(s);
      JsonElement web = WebValues.toWeb(s, d != null ? d : s.toJson());
      return web == null ? WebValues.id(s.getDefault().name()) : web.getAsString();
   }

   private void addPage(WebSchema.Owner o, SectionLayout layout, LayoutPage page, String prefix, Set<LayoutPage> done) {
      if (done.add(page)) {
         for (LayoutPage sub : this.addGroups(o, page, prefix, false)) {
            this.addPage(o, layout, sub, subTitle(o, sub), done);
         }
      }
   }

   private void addUnlinked(WebSchema.Owner o, SectionLayout layout, LayoutPage tab, Set<LayoutPage> done) {
      for (LayoutPage page : layout.pages) {
         if (page.parent != null && page.style == null && page.tab == null && !done.contains(page) && page.tabPage() == tab) {
            this.problems.add(o.path + ": the page " + page.title + " is not opened by a row; its groups follow the others");
            this.addPage(o, layout, page, subTitle(o, page), done);
         }
      }
   }

   private static String subTitle(WebSchema.Owner o, LayoutPage sub) {
      String path = sub.path();
      LayoutPage tab;
      if (o.tab != null && (tab = sub.tabPage()) != null && path.startsWith(tab.title + " › ")) {
         path = path.substring(tab.title.length() + 3);
      }

      return path;
   }

   private List<LayoutPage> addGroups(WebSchema.Owner o, LayoutPage page, String prefix, boolean merge) {
      ArrayList<LayoutPage> links = new ArrayList<>();

      for (LayoutGroup g : page.groups) {
         String label = prefix == null ? g.title : (g.slot != Slot.MAIN && page.groups.size() != 1 ? prefix + " › " + g.title : prefix);
         String kind = prefix == null ? kind(g.slot) : "feature";
         JsonObject group = o.group(label, kind, merge || prefix != null);
         JsonArray rows = group.getAsJsonArray("settings");
         IdentityHashMap<Setting, JsonObject> made = new IdentityHashMap<>();

         for (LayoutEntry e : g.entries) {
            if (e.link != null) {
               links.add(e.link);
            } else {
               JsonObject row = this.setting(o, e.setting);
               if (row != null) {
                  made.put(e.setting, row);
                  o.exported.add(e.setting);
                  if (prefix == null && (g.slot == Slot.SIZE || g.slot == Slot.MOTION) && e.setting instanceof SliderSetting) {
                     o.lookSliders.add((SliderSetting)e.setting);
                  }

                  JsonObject parent = e.parent == null ? null : made.get(e.parent);
                  JsonObject rule;
                  if (parent != null && (rule = rule(e.setting, e.parent, parent.get("key").getAsString())) != null) {
                     this.addRule(row, rule);
                  }

                  if (parent != null && "toggle".equals(parent.get("type").getAsString())) {
                     if (!parent.has("children")) {
                        parent.add("children", new JsonArray());
                     }

                     parent.getAsJsonArray("children").add(row);
                  } else {
                     rows.add(row);
                  }
               }
            }
         }
      }

      return links;
   }

   private static String kind(Slot slot) {
      switch (slot) {
         case MAIN:
            return "main";
         case COLORS:
            return "colors";
         case SIZE:
            return "size";
         case MOTION:
            return "motion";
         case WHEN:
            return "when";
         case OPTIONS:
            return "styleOptions";
         case EXTRAS:
            return "extras";
         case ADVANCED:
            return "advanced";
         default:
            return "feature";
      }
   }

   private JsonObject setting(WebSchema.Owner o, Setting s) {
      if (o.skip.contains(s) || s instanceof NoteSetting) {
         return null;
      } else if (s instanceof WidgetSetting) {
         return null;
      } else {
         String style = o.styleOf(s);
         if ("".equals(style)) {
            String owner = s.getOwner().styleName();
            Integer n = o.unknownStyles.get(owner);
            o.unknownStyles.put(owner, n == null ? 1 : n + 1);
            return null;
         } else {
            JsonObject row = this.row(o.module, s);
            if (row == null) {
               return null;
            } else {
               if (style != null && o.pickerKey != null) {
                  JsonObject r2 = new JsonObject();
                  r2.addProperty("key", o.pickerKey);
                  r2.addProperty("equals", style);
                  this.addRule(row, r2);
               } else {
                  JsonObject r;
                  if (style == null && o.picker != null && s.hasCondition() && (r = styleRule(o, s)) != null) {
                     this.addRule(row, r);
                  }
               }

               return row;
            }
         }
      }
   }

   JsonObject row(Module m, Setting s) {
      JsonObject row = new JsonObject();
      String label = s.getLabel();
      row.addProperty("key", WebKeys.key(m, s));
      row.addProperty("label", label != null && !label.isEmpty() ? label : s.getId());
      if (s.getDescription() != null && !s.getDescription().trim().isEmpty()) {
         row.addProperty("tooltip", s.getDescription().trim());
      }

      if (s.getKeywords().length > 0) {
         JsonArray k = new JsonArray();

         for (String w : s.getKeywords()) {
            k.add(new JsonPrimitive(w));
         }

         row.add("keywords", k);
      }

      if (s instanceof KeyBindSetting) {
         KeyBinding kb = ((KeyBindSetting)s).binding();
         row.addProperty("type", "keybind");
         row.addProperty("default", keyName(kb == null ? 0 : kb.getKeyCodeDefault()));
         this.settingCount++;
         return row;
      } else {
         String type = WebValues.type(s);
         row.addProperty("type", type);
         JsonElement mod = SettingDefaults.get(s);
         if (mod == null) {
            mod = s.toJson();
         }

         JsonElement def;
         if ((def = WebValues.toWeb(s, mod)) != null && !def.isJsonNull()) {
            row.add("default", def);
            if (s instanceof SliderSetting) {
               SliderSetting sl = (SliderSetting)s;
               row.addProperty("min", WebValues.number(sl.getMin()));
               row.addProperty("max", WebValues.number(sl.getMax()));
               row.addProperty("step", WebValues.number(sl.getStep()));
               String unit = sl.isClock() ? "min" : sl.getUnit().trim();
               if (!unit.isEmpty()) {
                  row.addProperty("unit", unit);
               }
            } else if (s instanceof ModeSetting) {
               JsonArray options = new JsonArray();

               for (Enum c : ((ModeSetting)s).getValues()) {
                  options.add(option(WebValues.id(c.name()), ModeSetting.displayName(c)));
               }

               row.add("options", options);
            } else if (s instanceof EnumChoices) {
               JsonArray options = new JsonArray();

               for (Enum<?> c : ((EnumChoices)s).choices()) {
                  options.add(option(WebValues.id(c.name()), ModeSetting.displayName(c)));
               }

               row.add("options", options);
            } else if (s instanceof Choices) {
               JsonArray options = new JsonArray();

               for (String[] c : ((Choices)s).choices()) {
                  options.add(option(c[0], c[1]));
               }

               row.add("options", options);
            } else if (s instanceof PresetSetting) {
               JsonArray options = new JsonArray();

               for (PresetSetting.Chip chip : WebValues.builtIn((PresetSetting)s)) {
                  options.add(option(WebValues.id(chip.id), chip.name));
               }

               options.add(option("custom", "Custom"));
               row.add("options", options);
            } else if (s instanceof ColorSetting) {
               JsonArray modes = new JsonArray();

               for (ColorSetting.Mode mode : ColorSetting.Mode.values()) {
                  if (mode != ColorSetting.Mode.GLOBAL || ((ColorSetting)s).allowsGlobal()) {
                     modes.add(new JsonPrimitive(mode.name().toLowerCase(Locale.ROOT)));
                  }
               }

               row.add("colorModes", modes);
            }

            this.settingCount++;
            return row;
         } else {
            this.problems.add((m == null ? "Settings" : m.getName()) + " › " + row.get("label").getAsString() + ": no default value");
            return null;
         }
      }
   }

   private static JsonObject option(String value, String label) {
      JsonObject o = new JsonObject();
      o.addProperty("value", value);
      o.addProperty("label", label);
      return o;
   }

   private static String keyName(int code) {
      if (code <= 0) {
         return "NONE";
      } else {
         String name = Keyboard.getKeyName(code);
         return name != null && !name.isEmpty() ? name : "NONE";
      }
   }

   private JsonArray looks(WebSchema.Owner o) {
      Module m = o.module;
      LinkedHashMap<SliderSetting, double[]> values = new LinkedHashMap<>();
      Module.StartingLooks own = m.startingLooks();
      if (own == null || own.withPage()) {
         for (SliderSetting s : o.lookSliders) {
            if (!StartingLookWidget.notAStrength(s)) {
               double[] v = new double[3];

               for (int look = 0; look < 3; look++) {
                  v[look] = StartingLookWidget.value(s, look);
               }

               values.put(s, v);
            }
         }
      }

      if (own != null) {
         ModeSetting pick = o.picker instanceof ModeSetting ? (ModeSetting)o.picker : null;
         Object was = pick == null ? null : SettingProbe.current(pick);

         try {
            Enum[] enumArray;
            if (pick == null) {
               Enum[] enumArray2 = new Enum[1];
               enumArray = enumArray2;
               enumArray2[0] = null;
            } else {
               enumArray = pick.getValues();
            }

            for (Enum c : enumArray) {
               if (c != null) {
                  SettingProbe.set(pick, c);
               }

               for (SliderSetting t : own.targets()) {
                  if (o.exported.contains(t) && !values.containsKey(t)) {
                     double[] v = new double[3];

                     for (int look = 0; look < 3; look++) {
                        v[look] = own.value(t, look);
                     }

                     values.put(t, v);
                  }
               }
            }
         } finally {
            if (pick != null) {
               SettingProbe.set(pick, was);
            }
         }
      }

      if (values.isEmpty()) {
         return null;
      } else {
         JsonArray out = new JsonArray();

         for (int look = 0; look < 3; look++) {
            JsonObject set = new JsonObject();

            for (Entry e : values.entrySet()) {
               set.addProperty(WebKeys.key(m, (Setting)e.getKey()), WebValues.number(snap((SliderSetting)e.getKey(), ((double[])e.getValue())[look])));
            }

            JsonObject l = new JsonObject();
            l.addProperty("look", LOOKS[look]);
            l.add("values", set);
            out.add(l);
         }

         return out;
      }
   }

   private static double snap(SliderSetting s, double v) {
      v = Math.max(s.getMin(), Math.min(s.getMax(), v));
      double steps = Math.round((v - s.getMin()) / s.getStep());
      int decimals = Math.max(0, BigDecimal.valueOf(s.getStep()).stripTrailingZeros().scale());
      double snapped = BigDecimal.valueOf(s.getMin() + steps * s.getStep()).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
      return Math.max(s.getMin(), Math.min(s.getMax(), snapped));
   }

   private void addRule(JsonObject row, JsonObject rule) {
      if (!row.has("dependsOn")) {
         row.add("dependsOn", new JsonArray());
      }

      row.getAsJsonArray("dependsOn").add(rule);
      this.ruleCount++;
   }

   static JsonObject rule(Setting s, Setting parent, String parentKey) {
      Object[] trials = SettingProbe.trials(parent);
      if (trials != null && !(parent instanceof ColorSetting)) {
         Object was = SettingProbe.current(parent);
         ArrayList<Object> values = new ArrayList<>();
         values.add(was);

         for (Object object : trials) {
            values.add(object);
         }

         ArrayList shown = new ArrayList();
         ArrayList hidden = new ArrayList();

         try {
            for (Object e : values) {
               SettingProbe.set(parent, e);
               (visible(s) ? shown : hidden).add(e);
            }
         } finally {
            SettingProbe.set(parent, was);
         }

         if (!shown.isEmpty() && !hidden.isEmpty()) {
            JsonObject r = new JsonObject();
            r.addProperty("key", parentKey);
            if (parent instanceof BooleanSetting) {
               if (!shown.contains(Boolean.TRUE)) {
                  r.addProperty("equals", false);
               }

               return r;
            } else if (!(parent instanceof ModeSetting)) {
               if (parent instanceof SliderSetting && hidden.size() == 1) {
                  r.addProperty("notEquals", WebValues.number((Double)hidden.get(0)));
                  return r;
               } else {
                  return null;
               }
            } else {
               if (shown.size() == 1) {
                  r.addProperty("equals", WebValues.id(((Enum)shown.get(0)).name()));
               } else {
                  JsonArray jsonArray = new JsonArray();

                  for (Object v : shown) {
                     jsonArray.add(new JsonPrimitive(WebValues.id(((Enum)v).name())));
                  }

                  r.add("oneOf", jsonArray);
               }

               return r;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static JsonObject styleRule(WebSchema.Owner o, Setting s) {
      if (!(o.picker instanceof ModeSetting)) {
         return null;
      } else {
         ModeSetting pick = (ModeSetting)o.picker;
         Object was = SettingProbe.current(pick);
         JsonArray shown = new JsonArray();
         int count = 0;

         try {
            for (Enum c : pick.getValues()) {
               SettingProbe.set(pick, c);
               if (visible(s)) {
                  shown.add(new JsonPrimitive(WebValues.id(c.name())));
                  count++;
               }
            }
         } finally {
            SettingProbe.set(pick, was);
         }

         if (count != 0 && count != pick.getValues().length) {
            JsonObject r = new JsonObject();
            r.addProperty("key", o.pickerKey);
            r.add("oneOf", shown);
            return r;
         } else {
            return null;
         }
      }
   }

   private static boolean visible(Setting s) {
      try {
         return s.isVisibleIgnoringStyle();
      } catch (RuntimeException var2) {
         return false;
      }
   }

   private JsonObject settingsPage() {
      FuriahsVisuals fv = FuriahsVisuals.instance;
      SoundSettings sounds = fv.getSounds();
      JsonObject page = new JsonObject();
      page.addProperty("id", "settings");
      page.addProperty("label", "Settings");
      page.addProperty("icon", "settings.png");
      this.icons.put("settings.png", this.island(IslandIcons.Icon.GEAR));
      page.addProperty("description", "Global color, menu key, view theme and the look of this menu.");
      page.addProperty("enabledKey", "settings.enabled");
      page.addProperty("enabledByDefault", true);
      JsonArray groups = new JsonArray();
      JsonArray color = new JsonArray();
      add(color, this.row(null, fv.getGlobalColor()));
      groups.add(settingsGroup("global-color", "Global Color", color));
      JsonArray key = new JsonArray();
      KeyBinding menuKey = Keybinds.openMenu;
      JsonObject k = new JsonObject();
      k.addProperty("key", "settings.menuKey");
      k.addProperty("label", "Menu Key");
      k.addProperty("tooltip", "The key that opens this menu (also in Options › Controls).");
      k.addProperty("type", "keybind");
      k.addProperty("default", keyName(menuKey == null ? 54 : menuKey.getKeyCodeDefault()));
      key.add(k);
      this.settingCount++;
      groups.add(settingsGroup("menu-key", "Menu Key", key));
      JsonArray ui = new JsonArray();
      add(ui, this.row(null, MenuPrefs.VIEW_THEME));
      add(ui, this.row(null, MenuPrefs.SIDEBAR));
      ui.add(this.guiScale());
      add(ui, this.row(null, MenuPrefs.FULL_SCREEN));
      groups.add(settingsGroup("interface", "Interface", ui));
      JsonArray access = new JsonArray();
      add(access, this.row(null, fv.getReduceFlashing()));
      groups.add(settingsGroup("accessibility", "Accessibility", access));
      JsonArray sound = new JsonArray();
      IdentityHashMap<Setting, JsonObject> made = new IdentityHashMap<>();

      for (Setting s : sounds.all()) {
         JsonObject row;
         if (!(s instanceof NoteSetting) && s.isSaved() && (row = this.row(null, s)) != null) {
            made.put(s, row);
            JsonObject parent = s != sounds.gui && s.hasCondition() ? made.get(sounds.gui) : null;
            JsonObject rule = parent == null ? null : rule(s, sounds.gui, parent.get("key").getAsString());
            if (rule != null) {
               this.addRule(row, rule);
               if (!parent.has("children")) {
                  parent.add("children", new JsonArray());
               }

               parent.getAsJsonArray("children").add(row);
            } else {
               sound.add(row);
            }
         }
      }

      groups.add(settingsGroup("sounds", "Sounds", sound));
      JsonArray theme = new JsonArray();
      add(theme, this.row(null, fv.getTheme()));
      groups.add(settingsGroup("theme", "Theme", theme));
      page.add("groups", groups);
      return page;
   }

   private JsonObject guiScale() {
      JsonObject row = new JsonObject();
      row.addProperty("key", "settings." + MenuPrefs.GUI_SCALE.getId());
      row.addProperty("label", "GUI Scale");
      row.addProperty("tooltip", MenuPrefs.GUI_SCALE.getDescription());
      row.addProperty("type", "choice");
      row.addProperty("default", MenuPrefs.GUI_SCALE_AUTO.getDefault() ? "auto" : Integer.toString((int)MenuPrefs.GUI_SCALE.getDefault()));
      JsonArray options = new JsonArray();
      options.add(option("auto", "Auto"));

      for (int step : MenuPrefs.SCALE_STEPS) {
         options.add(option(Integer.toString(step), step + "%"));
      }

      row.add("options", options);
      this.settingCount++;
      return row;
   }

   private static void add(JsonArray rows, JsonObject row) {
      if (row != null) {
         rows.add(row);
      }
   }

   private static JsonObject settingsGroup(String id, String label, JsonArray settings) {
      JsonObject g = new JsonObject();
      g.addProperty("id", id);
      g.addProperty("label", label);
      g.addProperty("kind", "feature");
      g.addProperty("scope", "section");
      g.add("settings", settings);
      return g;
   }

   private JsonArray themes() {
      JsonArray out = new JsonArray();
      int accent = Theme.readableAccent(defaultAccent());
      Theme.Style before = FuriahsVisuals.instance.getTheme().get();
      int beforeAccent = Theme.ACCENT;

      try {
         for (Theme.Style style : Theme.Style.values()) {
            Theme.apply(style, accent);
            int base = Theme.LIGHT ? -1 : -16777216;
            int panel = over(Theme.PANEL, base);
            JsonObject t = new JsonObject();
            t.addProperty("accent", WebValues.rgba(Theme.ACCENT));
            t.addProperty("accentHover", WebValues.rgba(Theme.ACCENT_HOVER));
            t.addProperty("accentSoft", WebValues.rgba(Theme.ACCENT_SOFT));
            t.addProperty("text", WebValues.rgba(Theme.TEXT));
            t.addProperty("textDim", WebValues.rgba(Theme.TEXT_DIM));
            t.addProperty("background", WebValues.rgba(over(Theme.SIDEBAR, panel)));
            t.addProperty("panel", WebValues.rgba(panel));
            t.addProperty("border", WebValues.rgba(Theme.PANEL_BORDER));
            t.addProperty("on", WebValues.rgba(Theme.ACCENT));
            t.addProperty("off", WebValues.rgba(over(Theme.TRACK, panel)));
            t.addProperty("textMuted", WebValues.rgba(Theme.TEXT_MUTED));
            t.addProperty("accentSoftHover", WebValues.rgba(Theme.ACCENT_SOFT_HOVER));
            t.addProperty("accentFaint", WebValues.rgba(Theme.ACCENT_FAINT));
            t.addProperty("accentSelection", WebValues.rgba(Theme.ACCENT_SELECTION));
            t.addProperty("accentLine", WebValues.rgba(Theme.ACCENT_LINE));
            t.addProperty("statusOn", WebValues.rgba(-12723068));
            t.addProperty("danger", WebValues.rgba(Theme.DANGER));
            t.addProperty("knob", WebValues.rgba(Theme.KNOB));
            t.addProperty("sidebar", WebValues.rgba(Theme.SIDEBAR));
            t.addProperty("surface", WebValues.rgba(Theme.SURFACE));
            t.addProperty("surfaceHover", WebValues.rgba(Theme.SURFACE_HOVER));
            t.addProperty("track", WebValues.rgba(Theme.TRACK));
            t.addProperty("divider", WebValues.rgba(Theme.DIVIDER));
            t.addProperty("pill", WebValues.rgba(Theme.PILL));
            t.addProperty("scrollTrack", WebValues.rgba(Theme.SCROLL_TRACK));
            t.addProperty("outline", WebValues.rgba(Theme.OUTLINE));
            t.addProperty("outlineHover", WebValues.rgba(Theme.OUTLINE_HOVER));
            t.addProperty("tooltip", WebValues.rgba(Theme.TOOLTIP));
            t.addProperty("tooltipBorder", WebValues.rgba(Theme.TOOLTIP_BORDER));
            t.addProperty("panelSheen", WebValues.rgba(Theme.PANEL_SHEEN));
            t.addProperty("shadow", WebValues.rgba(Theme.SHADOW));
            t.addProperty("previewStage", WebValues.rgba(Theme.PREVIEW_STAGE));
            if (Theme.GLASS) {
               t.addProperty("glassTint", WebValues.rgba(Theme.GLASS_TINT));
               t.addProperty("glassRim", WebValues.rgba(Theme.GLASS_RIM));
            }

            JsonObject theme = new JsonObject();
            theme.addProperty("id", WebValues.id(style.name()));
            theme.addProperty("label", style.displayName());
            theme.add("tokens", t);
            out.add(theme);
         }
      } finally {
         Theme.apply(before, beforeAccent);
      }

      return out;
   }

   private static int defaultAccent() {
      JsonElement d = SettingDefaults.get(FuriahsVisuals.instance.getGlobalColor());
      return d != null && d.isJsonObject() && d.getAsJsonObject().has("a")
         ? ColorUtil.parseHex(d.getAsJsonObject().get("a").getAsString(), -7635713)
         : -7635713;
   }

   private static int over(int top, int base) {
      float a = ColorUtil.alpha(top) / 255.0F;
      return ColorUtil.lerp(base | 0xFF000000, top | 0xFF000000, a) | 0xFF000000;
   }

   static String slug(String label) {
      String s = label.toLowerCase(Locale.ROOT).replace("&", " and ").replace("+", " ");
      s = s.replaceAll("[^a-z0-9]+", "-");
      return s.replaceAll("^-+|-+$", "");
   }

   private final class Owner {
      final Module module;
      final String sectionId;
      final MiniSection tab;
      final String path;
      final JsonArray styles = new JsonArray();
      final Set<String> styleIds = new LinkedHashSet<>();
      final Map<String, String> styleByName = new LinkedHashMap<>();
      Setting picker;
      String pickerKey;
      JsonObject stylePicker;
      final List<JsonObject> groups = new ArrayList<>();
      final Map<String, JsonObject> groupByLabel = new LinkedHashMap<>();
      final Set<String> groupIds = new LinkedHashSet<>();
      final Set<Setting> skip = Collections.newSetFromMap(new IdentityHashMap<>());
      final Map<String, Integer> unknownStyles = new LinkedHashMap<>();
      final Set<Setting> exported = Collections.newSetFromMap(new IdentityHashMap<>());
      final List<SliderSetting> lookSliders = new ArrayList<>();

      Owner(Module module, MiniSection tab, String path) {
         this.module = module;
         this.sectionId = WebKeys.section(module);
         this.tab = tab;
         this.path = path;
      }

      void style(String id, String label, String tag, String description) {
         if (!this.styleIds.add(id)) {
            WebSchema.this.problems.add(this.path + ": two styles are called " + id);
         } else {
            JsonObject s = new JsonObject();
            s.addProperty("id", id);
            s.addProperty("label", label);
            if (tag != null && !tag.trim().isEmpty() && !"NEW".equalsIgnoreCase(tag.trim())) {
               s.addProperty("tag", tag.trim());
            }

            s.addProperty("description", description == null ? "" : description);
            this.styles.add(s);
            this.styleByName.put(label, id);
            WebSchema.this.styleCount++;
         }
      }

      String styleOf(Setting s) {
         SettingStyle o = s.getOwner();
         if (o != null && o != this.tab && !(o instanceof MiniSection) && !this.styleIds.isEmpty()) {
            String id = this.styleByName.get(o.styleName());
            if (id == null) {
               int dot = s.getId().indexOf(46);
               String prefix = dot > 0 ? WebValues.id(s.getId().substring(0, dot)) : null;
               id = prefix != null && this.styleIds.contains(prefix) ? prefix : "";
            }

            return id;
         } else {
            return null;
         }
      }

      JsonObject group(String label, String kind, boolean merge) {
         if (merge && this.groupByLabel.containsKey(label)) {
            return this.groupByLabel.get(label);
         } else {
            String base = WebSchema.slug(label);
            String id = base.isEmpty() ? "group" : base;

            for (int n = 2; !this.groupIds.add(id); n++) {
               id = base + "-" + n;
            }

            JsonObject g = new JsonObject();
            g.addProperty("id", id);
            g.addProperty("label", label);
            g.addProperty("kind", kind);
            g.addProperty("scope", "section");
            g.add("settings", new JsonArray());
            this.groups.add(g);
            if (!this.groupByLabel.containsKey(label)) {
               this.groupByLabel.put(label, g);
            }

            return g;
         }
      }

      JsonArray finishedGroups() {
         for (Entry<String, Integer> e : this.unknownStyles.entrySet()) {
            WebSchema.this.problems.add(this.path + ": " + e.getValue() + " settings of \"" + e.getKey() + "\" left out (the website has no card for it)");
         }

         this.unknownStyles.clear();
         JsonArray out = new JsonArray();

         for (JsonObject g : this.groups) {
            if (g.getAsJsonArray("settings").size() > 0) {
               out.add(g);
            }
         }

         return out;
      }
   }
}
