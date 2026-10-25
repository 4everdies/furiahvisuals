package meowtils.furiahvisuals.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.ambience.AmbienceModule;
import meowtils.furiahvisuals.module.trails.TrailsModule;
import meowtils.furiahvisuals.module.weapons.WeaponDef;
import meowtils.furiahvisuals.module.weapons.WeaponsModule;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;

public final class WebStyles {
   private WebStyles() {
   }

   static ModeSetting<?> picker(Module m, Object tab, List<Setting> settings) {
      for (Setting s : settings) {
         if (s instanceof ModeSetting
            && s.getOwner() == tab
            && (m instanceof AmbienceModule ? s.getId().equals("editing") : ((ModeSetting)s).getStyle() == ModeSetting.Style.CARDS)) {
            return (ModeSetting<?>)s;
         }
      }

      return null;
   }

   public static List<WebStyles.Card> cards(Module m) {
      ArrayList<WebStyles.Card> out = new ArrayList<>();
      if (m instanceof WeaponsModule) {
         for (WeaponDef d : ((WeaponsModule)m).cardWeapons()) {
            out.add(new WebStyles.Card(WebValues.id(d.id), d.name, null, d.id));
         }

         return out;
      } else {
         ModeSetting<?> pick = picker(m, null, m.getSettings());
         if (pick == null) {
            return Collections.emptyList();
         } else {
            for (Enum c : pick.getValues()) {
               out.add(new WebStyles.Card(WebValues.id(c.name()), ModeSetting.displayName(c), c, null));
            }

            return out;
         }
      }
   }

   public static void select(Module m, WebStyles.Card c) {
      ModeSetting<?> pick = c.choice == null ? null : picker(m, null, m.getSettings());
      if (pick != null) {
         pick.setIndex(c.choice.ordinal());
         if (m instanceof TrailsModule || m instanceof AmbienceModule) {
            for (Enum v : pick.getValues()) {
               BooleanSetting sw = WebSchema.styleSwitch(m, v);
               if (sw != null) {
                  sw.set(v == c.choice);
               }
            }
         }
      }
   }

   public static final class Card {
      public final String id;
      public final String label;
      public final Enum<?> choice;
      public final String weapon;

      Card(String id, String label, Enum<?> choice, String weapon) {
         this.id = id;
         this.label = label;
         this.choice = choice;
         this.weapon = weapon;
      }
   }
}
