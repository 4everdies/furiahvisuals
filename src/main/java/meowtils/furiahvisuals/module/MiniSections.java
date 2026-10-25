package meowtils.furiahvisuals.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import meowtils.furiahvisuals.core.ConfigManager;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.setting.BooleanSetting;

public final class MiniSections {
   final Module module;
   private final List<MiniSection> list = new ArrayList<>();
   private MiniSection current;

   public MiniSections(Module module) {
      this.module = module;
   }

   public MiniSection add(String id, String name, IslandIcons.Icon icon) {
      return this.add(id, name, icon, false);
   }

   public MiniSection add(String id, String name, IslandIcons.Icon icon, boolean dividerBefore) {
      MiniSection s = new MiniSection(this, id, name, icon, dividerBefore);
      this.list.add(s);
      if (this.current == null) {
         this.current = s;
      }

      return s;
   }

   public BooleanSetting attachSwitch(MiniSection s, final BooleanSetting sw) {
      s.setSwitch(sw);
      sw.addListener(new Runnable() {
         @Override
         public void run() {
            if (sw.get() && !MiniSections.this.module.isEnabled() && !ConfigManager.isReadingSettings()) {
               MiniSections.this.module.setEnabled(true);
            }
         }
      });
      return sw;
   }

   public List<MiniSection> list() {
      return Collections.unmodifiableList(this.list);
   }

   public MiniSection current() {
      return this.current;
   }

   public void select(MiniSection s) {
      if (s != null && this.list.contains(s)) {
         this.current = s;
      }
   }

   public MiniSection byId(String id) {
      for (MiniSection s : this.list) {
         if (s.getId().equals(id)) {
            return s;
         }
      }

      return null;
   }

   public int[] onCount() {
      int on = 0;
      int all = 0;

      for (MiniSection s : this.list) {
         if (s.getSwitch() != null) {
            all++;
            if (s.getSwitch().get()) {
               on++;
            }
         }
      }

      return new int[]{on, all};
   }
}
