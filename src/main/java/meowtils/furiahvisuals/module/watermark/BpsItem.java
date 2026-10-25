package meowtils.furiahvisuals.module.watermark;

import java.util.Locale;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;

final class BpsItem extends IslandItem {
   final BooleanSetting vertical;
   final SliderSetting decimals;
   final ItemLabel label;
   private final RollingText value = new RollingText();
   private final double[] samples = new double[10];
   private int count;
   private int next;
   private double lastShown = Double.NaN;

   BpsItem(WaterMarkModule wm) {
      super(wm, "bps", "BPS", "Your speed in blocks per second.", IslandIcons.Icon.SPEED);
      this.icons(IslandIcons.Icon.SPEED, new IslandIcons.Icon[]{IslandIcons.Icon.SPEED, IslandIcons.Icon.ARROWS, IslandIcons.Icon.BOLT});
      this.label = new ItemLabel(this, "bps.label", "bps", "b/s", true, ItemLabel.Place.AFTER);
      this.vertical = this.option(
         new BooleanSetting("bps.vertical", "Include Vertical Movement", "Counts jumping and falling too (off: only horizontal speed).", false)
      );
      this.decimals = this.option(new SliderSetting("bps.decimals", "Decimals", "Digits after the point.", 0.0, 2.0, 1.0, 1.0));
   }

   @Override
   void tick() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      double speed = 0.0;
      if (p != null) {
         double dx = p.posX - p.lastTickPosX;
         double dz = p.posZ - p.lastTickPosZ;
         double dy = this.vertical.get() ? p.posY - p.lastTickPosY : 0.0;
         speed = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
         if (speed > 200.0) {
            speed = 0.0;
         }
      }

      this.samples[this.next] = speed;
      this.next = (this.next + 1) % this.samples.length;
      this.count = Math.min(this.count + 1, this.samples.length);
      double sum = 0.0;

      for (int i = 0; i < this.count; i++) {
         sum += this.samples[i];
      }

      double avg = this.count == 0 ? 0.0 : sum / this.count;
      int dec;
      double rounded = Math.round(avg * Math.pow(10.0, dec = this.decimals.getInt())) / Math.pow(10.0, dec);
      if (rounded != this.lastShown || this.value.get().isEmpty()) {
         this.lastShown = rounded;
         this.value.set(String.format(Locale.ROOT, "%." + dec + "f", rounded), rounded, this.wm.rolling());
      }
   }

   @Override
   String panelValue() {
      return this.value.get().isEmpty() ? null : this.value.get() + " b/s";
   }

   @Override
   String panelDescription() {
      return "Your speed";
   }

   void reset() {
      this.count = 0;
      this.next = 0;
   }

   double speedForTest() {
      double sum = 0.0;

      for (int i = 0; i < this.count; i++) {
         sum += this.samples[i];
      }

      return this.count == 0 ? 0.0 : sum / this.count;
   }

   @Override
   void frame(float dt, IslandDraw d, IslandStyle s) {
      this.value.update(dt);
   }

   @Override
   float measure(IslandDraw d, IslandStyle s) {
      return this.iconSpace(s) + this.label.width(d, s) + this.value.width(d, false, s.font);
   }

   @Override
   void draw(IslandDraw d, IslandStyle s, float x, float cy) {
      x = this.drawIcon(d, s, x, cy);
      if (this.label.before()) {
         x = this.label.draw(d, s, x, cy);
      }

      this.value.draw(d, false, s.font, x, cy, s.text);
      if (this.label.after()) {
         this.label.draw(d, s, x + this.value.width(d, false, s.font), cy);
      }
   }

   @Override
   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      hit.set(
         this,
         x - s.pad,
         cy - s.height / 2.0F,
         this.measure(this.wm.draw(), s) + s.pad * 2.0F,
         s.height,
         0,
         this.vertical.get() ? "Blocks per second (all directions)" : "Blocks per second (horizontal)",
         false
      );
      return true;
   }
}
