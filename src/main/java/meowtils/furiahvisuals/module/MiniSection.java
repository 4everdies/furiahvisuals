package meowtils.furiahvisuals.module;

import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.SettingStyle;

public final class MiniSection implements SettingStyle {
   private final MiniSections owner;
   private final String id;
   private final String name;
   private final IslandIcons.Icon icon;
   private final boolean dividerBefore;
   private BooleanSetting enabled;

   MiniSection(MiniSections owner, String id, String name, IslandIcons.Icon icon, boolean dividerBefore) {
      this.owner = owner;
      this.id = id;
      this.name = name;
      this.icon = icon;
      this.dividerBefore = dividerBefore;
   }

   public String getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public IslandIcons.Icon getIcon() {
      return this.icon;
   }

   public boolean dividerBefore() {
      return this.dividerBefore;
   }

   public BooleanSetting getSwitch() {
      return this.enabled;
   }

   public void setSwitch(BooleanSetting enabled) {
      this.enabled = enabled == null ? null : enabled.sectionSwitch();
   }

   public Module module() {
      return this.owner.module;
   }

   public boolean on() {
      return this.owner.module.isEnabled() && (this.enabled == null || this.enabled.get());
   }

   @Override
   public String styleName() {
      return this.name;
   }

   @Override
   public boolean isShown() {
      return this.owner.current() == this;
   }

   @Override
   public boolean isActive() {
      return this.enabled == null || this.enabled.get();
   }

   @Override
   public boolean isMultiple() {
      return true;
   }

   @Override
   public void show() {
      this.owner.select(this);
   }

   @Override
   public String kind() {
      return "mini section";
   }

   @Override
   public String kinds() {
      return "mini sections";
   }

   @Override
   public String toString() {
      return this.id;
   }
}
