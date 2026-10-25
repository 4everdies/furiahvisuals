package meowtils.furiahvisuals.gui.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import meowtils.furiahvisuals.gui.FuriahScreen;
import meowtils.furiahvisuals.gui.layout.LayoutEntry;
import meowtils.furiahvisuals.gui.layout.SectionLayouts;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.EnumSetSetting;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.ItemListSetting;
import meowtils.furiahvisuals.setting.ItemRulesSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;

public final class SearchIndexes {
   private static final Map<Module, SearchIndexes.Built> MODULES = new HashMap<>();

   private SearchIndexes() {
   }

   public static boolean isBuilt(Module m) {
      SearchIndexes.Built b = MODULES.get(m);
      return b != null && b.index != null;
   }

   public static SearchIndex of(Module m) {
      SearchIndexes.Built b = MODULES.get(m);
      if (b == null) {
         b = new SearchIndexes.Built();

         for (Setting s : m.getSettings()) {
            Widget w;
            if (s != m.getEnabledSetting()
               && s.showInMenu()
               && (s instanceof HasWidget || s instanceof ItemListSetting || s instanceof ItemRulesSetting)
               && (w = FuriahScreen.createWidget(m, s)) instanceof SearchList) {
               b.lists.put(s, (SearchList)w);
            }
         }

         MODULES.put(m, b);
      }

      int version = 17;

      for (SearchList list : b.lists.values()) {
         version = version * 31 + list.searchVersion();
      }

      if (b.index == null || b.index.version != version) {
         b.index = build(m, b.lists);
         b.index.version = version;
      }

      return b.index;
   }

   private static SearchIndex build(Module m, Map<Setting, SearchList> lists) {
      final SearchIndex index = new SearchIndex(m.getId(), m.getName());

      for (LayoutEntry entry : SectionLayouts.of(m).allEntries()) {
         final Setting s = entry.setting;
         if (s != null && s.isSearched()) {
            final SettingStyle owner = s.getOwner();
            String shown = entry.group().path();
            String group = owner != null ? owner.searchGroup(shown) : shown;
            boolean cards = s instanceof ModeSetting && ((ModeSetting)s).getStyle() == ModeSetting.Style.CARDS;
            index.add(SearchEntry.Kind.SETTING, s, null, s.getLabel(), group, s.getDescription(), owner, cards ? null : choices(s), keywords(s));
            if (cards) {
               for (Enum v : ((ModeSetting)s).getValues()) {
                  index.add(
                     SearchEntry.Kind.CARD, s, v, ModeSetting.displayName(v), join(group, s.getLabel()), ModeSetting.description(v), owner, null, cardWords(v)
                  );
               }
            }

            SearchList list;
            if ((list = lists.get(s)) != null) {
               final String listPath = join(group, s.getLabel());
               final HashMap names = new HashMap();
               list.searchItems(
                  new SearchList.Items() {
                     @Override
                     public void item(Object key, String name, String description, String... keywords) {
                        names.put(key, name);
                        index.add(SearchEntry.Kind.ITEM, s, key, name, listPath, description, owner, null, keywords);
                     }

                     @Override
                     public void option(Object itemKey, Setting option) {
                        String item = (String)names.get(itemKey);
                        index.add(
                           SearchEntry.Kind.OPTION,
                           s,
                           option,
                           option.getLabel(),
                           item == null ? listPath : listPath + " › " + item,
                           option.getDescription(),
                           owner,
                           SearchIndexes.choices(option),
                           SearchIndexes.keywords(option)
                        );
                     }
                  }
               );
            }
         }
      }

      return index;
   }

   private static String[] cardWords(Enum<?> v) {
      if (!(v instanceof Displayable)) {
         return null;
      } else {
         Displayable d = (Displayable)v;
         String tag = d.tag();
         String[] words = d.keywords();
         if (words != null && words.length != 0) {
            if (tag == null) {
               return (String[])words.clone();
            } else {
               String[] out = Arrays.copyOf(words, words.length + 1);
               out[words.length] = tag;
               return out;
            }
         } else {
            String[] stringArray;
            if (tag == null) {
               stringArray = null;
            } else {
               String[] stringArray2 = new String[1];
               stringArray = stringArray2;
               stringArray2[0] = tag;
            }

            return stringArray;
         }
      }
   }

   private static String[] keywords(Setting s) {
      String[] k = s.getKeywords();
      if (!s.getLabel().equals(s.getName()) && s.getName() != null && !s.getName().isEmpty()) {
         String[] out = Arrays.copyOf(k, k.length + 1);
         out[k.length] = s.getName();
         return out;
      } else {
         return k;
      }
   }

   private static String join(String group, String name) {
      return group != null && !group.isEmpty() && !group.equalsIgnoreCase(name) ? group + " › " + name : name;
   }

   static String[] choices(Setting s) {
      if (!(s instanceof ModeSetting) && !(s instanceof EnumSetSetting)) {
         if (!(s instanceof ColorSetting)) {
            return null;
         } else {
            ColorSetting.Mode[] modes = ColorSetting.Mode.values();
            String[] names = new String[modes.length];

            for (int i = 0; i < modes.length; i++) {
               names[i] = modes[i].displayName();
            }

            return names;
         }
      } else {
         Enum[] values = s instanceof ModeSetting ? ((ModeSetting)s).getValues() : ((EnumSetSetting)s).values();
         String[] names = new String[values.length];

         for (int i = 0; i < values.length; i++) {
            names[i] = ModeSetting.displayName(values[i]);
         }

         return names;
      }
   }

   private static final class Built {
      SearchIndex index;
      final Map<Setting, SearchList> lists = new LinkedHashMap<>();

      private Built() {
      }
   }
}
