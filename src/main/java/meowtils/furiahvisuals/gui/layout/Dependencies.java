package meowtils.furiahvisuals.gui.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingProbe;

final class Dependencies {
   private static final int MAX_OPTIONS = 16;
   private static final int PAIR_REACH = 16;

   private Dependencies() {
   }

   static Map<Setting, Setting> find(List<Setting> settings, Predicate<Setting> notSwitch) {
      IdentityHashMap<Setting, Integer> index = new IdentityHashMap<>();
      ArrayList<Setting> conditional = new ArrayList<>();
      ArrayList<Setting> switches = new ArrayList<>();

      for (int i = 0; i < settings.size(); i++) {
         Setting s = settings.get(i);
         index.put(s, i);
         if (s.hasCondition()) {
            conditional.add(s);
         }

         Object[] objectArray = notSwitch.test(s) ? null : SettingProbe.trials(s);
         if (objectArray != null && objectArray.length > 0) {
            switches.add(s);
         }
      }

      IdentityHashMap<Setting, ArrayList<Setting>> switchesOf = new IdentityHashMap<>();

      for (Setting setting : switches) {
         Object[] objectArray = SettingProbe.trials(setting);
         ArrayList<Setting> watch = new ArrayList<>();

         for (Setting setting2 : conditional) {
            if (setting2 != setting && (setting.getOwner() == null || setting.getOwner() == setting2.getOwner())) {
               watch.add(setting2);
            }
         }

         if (!watch.isEmpty()) {
            boolean[] base = new boolean[watch.size()];

            for (int j = 0; j < base.length; j++) {
               base[j] = visible(watch.get(j));
            }

            boolean[] blArray = new boolean[watch.size()];
            Object was = SettingProbe.current(setting);

            try {
               for (Object t : objectArray) {
                  SettingProbe.set(setting, t);

                  for (int i = 0; i < base.length; i++) {
                     if (!blArray[i] && visible(watch.get(i)) != base[i]) {
                        blArray[i] = true;
                     }
                  }
               }
            } finally {
               SettingProbe.set(setting, was);
            }

            for (int var46 = 0; var46 < blArray.length; var46++) {
               if (blArray[var46]) {
                  ArrayList<Setting> list = (ArrayList<Setting>)switchesOf.get(watch.get(var46));
                  if (list == null) {
                     list = new ArrayList<>();
                     switchesOf.put(watch.get(var46), list);
                  }

                  list.add(setting);
               }
            }
         }
      }

      IdentityHashMap<Setting, Boolean> byPair = new IdentityHashMap<>();

      for (Setting setting : conditional) {
         Setting[] pair;
         if (!switchesOf.containsKey(setting) && (pair = pair(setting, switches, index)) != null) {
            ArrayList list = new ArrayList();
            list.add(pair[0]);
            list.add(pair[1]);
            switchesOf.put(setting, list);
            byPair.put(setting, Boolean.TRUE);
         }
      }

      IdentityHashMap<Setting, Setting> identityHashMap = new IdentityHashMap<>();
      IdentityHashMap identityHashMap2 = new IdentityHashMap();

      for (int round = 0; round < 64; round++) {
         identityHashMap.clear();

         for (Entry<Setting, ArrayList<Setting>> entry : switchesOf.entrySet()) {
            Setting c = entry.getKey();
            int n = index.get(c);
            boolean outer = byPair.containsKey(c);
            Setting best = null;
            int bestScore = Integer.MAX_VALUE;

            for (Setting p : entry.getValue()) {
               if (!identityHashMap2.containsKey(p)) {
                  int pi = index.get(p);
                  boolean sameStyle = p.getOwner() == c.getOwner();
                  if (!sameStyle || pi <= n) {
                     int distance = outer ? pi : n - pi;
                     int score = (p.showInMenu() ? 0 : 1000000) + (sameStyle ? distance : 500000 + Math.abs(n - pi));
                     if (score < bestScore) {
                        bestScore = score;
                        best = p;
                     }
                  }
               }
            }

            if (best != null) {
               identityHashMap.put(c, best);
            }
         }

         IdentityHashMap<Setting, Integer> count = new IdentityHashMap<>();
         boolean bl = false;
         Iterator<Setting> iterator = identityHashMap.values().iterator();

         while (iterator.hasNext()) {
            Setting settingx;
            Integer n = count.get(settingx = iterator.next());
            count.put(settingx, n == null ? 1 : n + 1);
         }

         for (Entry entry : count.entrySet()) {
            if ((Integer)entry.getValue() > 16) {
               identityHashMap2.put(entry.getKey(), Boolean.TRUE);
               bl = true;
            }
         }

         if (!bl) {
            break;
         }
      }

      return identityHashMap;
   }

   private static Setting[] pair(Setting c, List<Setting> switches, Map<Setting, Integer> index) {
      final int ci = index.get(c);
      List near = new ArrayList();

      for (Setting p : switches) {
         if (p != c && (p.getOwner() == null || p.getOwner() == c.getOwner()) && (p.getOwner() != c.getOwner() || index.get(p) <= ci)) {
            near.add(p);
         }
      }

      final Map<Setting, Integer> idx = index;
      Collections.sort(near, new Comparator<Setting>() {
         public int compare(Setting a, Setting b) {
            return Math.abs(idx.get(a) - ci) - Math.abs(idx.get(b) - ci);
         }
      });
      if (near.size() > 16) {
         near = near.subList(0, 16);
      }

      boolean base = visible(c);

      for (int i = 0; i < near.size(); i++) {
         Setting a = (Setting)near.get(i);
         Object[] ta = SettingProbe.trials(a);
         Object wasA = SettingProbe.current(a);

         for (int j = i + 1; j < near.size(); j++) {
            Setting b = (Setting)near.get(j);
            Object[] tb = SettingProbe.trials(b);
            Object wasB = SettingProbe.current(b);

            try {
               for (Object va : ta) {
                  SettingProbe.set(a, va);

                  for (Object vb : tb) {
                     SettingProbe.set(b, vb);
                     if (visible(c) != base) {
                        return new Setting[]{a, b};
                     }
                  }

                  SettingProbe.set(b, wasB);
               }
            } finally {
               SettingProbe.set(b, wasB);
               SettingProbe.set(a, wasA);
            }
         }
      }

      return null;
   }

   private static boolean visible(Setting s) {
      try {
         return s.isVisibleIgnoringStyle();
      } catch (RuntimeException var2) {
         return false;
      }
   }
}
