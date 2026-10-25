package meowtils.furiahvisuals.module.watermark;

final class SettingsItem extends IslandItem {
   SettingsItem(WaterMarkModule wm) {
      super(wm, "settings", "Settings ⚙", "The gear: quick settings in the island (click it with chat open).", IslandIcons.Icon.GEAR);
   }

   @Override
   float measure(IslandDraw d, IslandStyle s) {
      return s.icon;
   }

   @Override
   void draw(IslandDraw d, IslandStyle s, float x, float cy) {
      boolean open = this.wm.isSettingsViewOpen();
      int accent = s.accentAt(x + s.icon / 2.0F);
      int color = open ? accent : s.dim;
      if (open) {
         d.glow(x + s.icon / 2.0F, cy, s.icon * 2.6F, accent & 16777215 | 1073741824);
      }

      d.icon(IslandIcons.Icon.GEAR, x, cy - s.icon / 2.0F, s.icon, color);
   }

   @Override
   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      hit.set(
         this,
         x - s.pad,
         cy - s.height / 2.0F,
         s.icon + s.pad * 2.0F,
         s.height,
         1,
         this.wm.isSettingsViewOpen() ? "Back to the island" : "Quick settings",
         true
      );
      return true;
   }

   @Override
   void click(IslandItem.Hit hit, float mx, float my) {
      this.wm.toggleSettingsView();
   }
}
