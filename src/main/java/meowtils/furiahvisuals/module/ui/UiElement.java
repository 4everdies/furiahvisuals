package meowtils.furiahvisuals.module.ui;

import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.module.MiniSection;
import meowtils.furiahvisuals.setting.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public abstract class UiElement {
   protected final UiModule module;
   protected final MiniSection section;

   protected UiElement(UiModule module, MiniSection section) {
      this.module = module;
      this.section = section;
   }

   public final boolean on() {
      return this.section.on();
   }

   public final MiniSection section() {
      return this.section;
   }

   protected final <S extends Setting> S own(S setting, String group) {
      return this.module.addOwned(this.section, setting.group(group));
   }

   public abstract void addSettings();

   public void applyTheme() {
   }

   public void onRenderOverlay(float partialTicks) {
   }

   public boolean onOverlayElement(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      return false;
   }

   public void onOverlayElementPost(RenderGameOverlayEvent.ElementType type, float partialTicks) {
   }

   public void onAttack(EntityPlayer attacker, Entity target) {
   }

   public void onRenderHud(float partialTicks) {
   }

   public void onTick() {
   }

   public void onClientTick() {
   }

   public void onWorldChange() {
   }

   public boolean wantsGlass() {
      return false;
   }

   public String notice() {
      return null;
   }

   public String loadWarning() {
      return null;
   }

   public void afterLoad() {
   }

   public boolean reshapedOnLoad() {
      return false;
   }
}
