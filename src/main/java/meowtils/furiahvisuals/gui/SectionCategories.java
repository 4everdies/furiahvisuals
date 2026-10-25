package meowtils.furiahvisuals.gui;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.module.Module;

public final class SectionCategories {
   public static final String[] NAMES = new String[]{"Combat", "Items", "World", "HUD", "Other"};

   private SectionCategories() {
   }

   public static String of(Module m) {
      String id = m.getId();
      if ("target_aura".equals(id)
         || "hit_effect".equals(id)
         || "kill_effects".equals(id)
         || "landing_impact".equals(id)
         || "trails".equals(id)
         || "particles".equals(id)) {
         return "Combat";
      } else if ("weapons".equals(id) || "glow".equals(id)) {
         return "Items";
      } else if ("ambience".equals(id) || "dynamic_skies".equals(id) || "motion_blur".equals(id)) {
         return "World";
      } else {
         return !"watermark".equals(id) && !"ui".equals(id) ? "Other" : "HUD";
      }
   }

   public static List<Module> in(String category, List<Module> modules) {
      ArrayList<Module> out = new ArrayList<>();

      for (Module m : modules) {
         if (of(m).equals(category)) {
            out.add(m);
         }
      }

      return out;
   }

   public static List<Module> ordered(List<Module> modules) {
      ArrayList<Module> out = new ArrayList<>();

      for (String c : NAMES) {
         out.addAll(in(c, modules));
      }

      return out;
   }
}
