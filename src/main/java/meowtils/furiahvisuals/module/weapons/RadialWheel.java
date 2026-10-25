package meowtils.furiahvisuals.module.weapons;

import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.Module;

public final class RadialWheel {
   private final WeaponWheel wheel;

   private RadialWheel(WeaponWheel wheel) {
      this.wheel = wheel;
   }

   public static RadialWheel create(WheelContent content) {
      Module m = FuriahsVisuals.instance.getModules().get("weapons");
      return !(m instanceof WeaponsModule) ? null : new RadialWheel(new WeaponWheel(((WeaponsModule)m).wheelSettings, content));
   }

   public void tick() {
      this.wheel.tick();
   }

   public void drawBackdrop() {
      if (this.wheel.showing() && !this.wheel.inMenu) {
         this.wheel.renderer.drawBackdropHud();
      }
   }

   public void drawHud() {
      if (this.wheel.showing() && !this.wheel.inMenu) {
         this.wheel.renderer.drawHud();
      }
   }

   public boolean showing() {
      return this.wheel.showing();
   }

   public boolean isOpen() {
      return this.wheel.isOpen();
   }

   public void cancel() {
      this.wheel.cancel(false);
   }

   public void keyForTest(Boolean down) {
      this.wheel.testKey = down;
   }

   public void scrollForTest(int dir) {
      this.wheel.scroll(dir);
   }

   public String describeForTest() {
      return this.wheel.describe();
   }

   public int litForTest() {
      return this.wheel.selected;
   }
}
