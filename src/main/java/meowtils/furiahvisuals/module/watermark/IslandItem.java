package meowtils.furiahvisuals.module.watermark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;

abstract class IslandItem {
   final String id;
   final String name;
   final String description;
   final IslandIcons.Icon listIcon;
   protected final WaterMarkModule wm;
   BooleanSetting shown;
   private final List<Setting> options = new ArrayList<>();
   final Spring width = new Spring();
   float blockX;
   float blockW;
   float full;
   float presence;
   boolean inBar;
   boolean inBarKnown;
   float rowGlow = -1.0F;
   private float held;
   private long smallerSince;
   IconSetting iconChoice;
   ModeSetting<WaterMarkModule.IconTint> iconTint;
   int lastIconColor;

   IslandItem(WaterMarkModule wm, String id, String name, String description, IslandIcons.Icon listIcon) {
      this.wm = wm;
      this.id = id;
      this.name = name;
      this.description = description;
      this.listIcon = listIcon;
   }

   protected final <S extends Setting> S option(S setting) {
      this.options.add(setting);
      return this.wm.registerNested(setting);
   }

   protected final void shared(Setting setting) {
      this.options.add(setting);
   }

   protected final IconSetting icons(IslandIcons.Icon def, IslandIcons.Icon... choices) {
      this.iconChoice(def, choices);
      this.iconTint = this.option(
         new ModeSetting<>(this.id + ".iconColor", "Icon Color", "The icon's color.", WaterMarkModule.IconTint.class, WaterMarkModule.IconTint.SOFT)
            .visibleWhen(new BooleanSupplier() {
               @Override
               public boolean getAsBoolean() {
                  return IslandItem.this.iconChoice.get() != null;
               }
            })
      );
      return this.iconChoice;
   }

   protected final IconSetting iconChoice(IslandIcons.Icon def, IslandIcons.Icon... choices) {
      this.iconChoice = this.option(new IconSetting(this.id + ".icon", "Icon", "The icon before the value, or none.", def, choices));
      return this.iconChoice;
   }

   boolean showsIcon() {
      return this.wm.icons.get() && this.iconChoice != null && this.iconChoice.get() != null;
   }

   float iconSpace(IslandStyle s) {
      return this.showsIcon() ? s.icon + s.gap : 0.0F;
   }

   int iconColor(IslandStyle s, float x) {
      WaterMarkModule.IconTint tint = this.iconTint == null ? WaterMarkModule.IconTint.SOFT : this.iconTint.get();
      return this.wm.tint(tint, s, x);
   }

   float drawIcon(IslandDraw d, IslandStyle s, float x, float cy) {
      if (!this.showsIcon()) {
         return x;
      } else {
         this.lastIconColor = this.iconColor(s, x + s.icon / 2.0F);
         d.icon(this.iconChoice.get(), x, cy - s.icon / 2.0F, s.icon, this.lastIconColor);
         return x + s.icon + s.gap;
      }
   }

   List<Setting> options() {
      return Collections.unmodifiableList(this.options);
   }

   boolean available() {
      return true;
   }

   String panelValue() {
      return null;
   }

   String panelDescription() {
      String d = this.description == null ? "" : this.description.trim();
      return d.endsWith(".") ? d.substring(0, d.length() - 1) : d;
   }

   String panelSetup() {
      return null;
   }

   void tick() {
   }

   void frame(float dt, IslandDraw d, IslandStyle s) {
   }

   abstract float measure(IslandDraw var1, IslandStyle var2);

   abstract void draw(IslandDraw var1, IslandStyle var2, float var3, float var4);

   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      return false;
   }

   void click(IslandItem.Hit hit, float mx, float my) {
   }

   final float settle(float wanted, long nowMs) {
      if (wanted >= this.held || this.held <= 0.0F) {
         this.held = wanted;
         this.smallerSince = 0L;
      } else if (this.smallerSince == 0L) {
         this.smallerSince = nowMs;
      } else if (nowMs - this.smallerSince > 1500L) {
         this.held = wanted;
         this.smallerSince = 0L;
      }

      return this.held;
   }

   final void resetHeld() {
      this.held = 0.0F;
      this.smallerSince = 0L;
   }

   static final class Hit {
      IslandItem item;
      float x;
      float y;
      float w;
      float h;
      int action;
      String tooltip;
      boolean clickable;

      void set(IslandItem item, float x, float y, float w, float h, int action, String tooltip, boolean clickable) {
         this.item = item;
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.action = action;
         this.tooltip = tooltip;
         this.clickable = clickable;
      }

      void clear() {
         this.item = null;
         this.tooltip = null;
         this.clickable = false;
         this.action = 0;
      }
   }
}
