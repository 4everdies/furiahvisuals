package meowtils.furiahvisuals.module.watermark;

import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

final class FpsItem extends IslandItem {
   final BooleanSetting smooth;
   final ItemLabel label;
   private final RollingText value = new RollingText();
   private float avgFrame = -1.0F;
   private float sinceUpdate;
   private int shown;
   private long sampledFrame = -1L;
   private long sampledNanos;

   FpsItem(WaterMarkModule wm) {
      super(wm, "fps", "FPS", "Frames per second.", IslandIcons.Icon.MONITOR);
      this.icons(
         IslandIcons.Icon.MONITOR, new IslandIcons.Icon[]{IslandIcons.Icon.MONITOR, IslandIcons.Icon.CHART, IslandIcons.Icon.BOLT, IslandIcons.Icon.SPEED}
      );
      this.label = new ItemLabel(this, "fps.suffix", "fps", "fps", true, ItemLabel.Place.AFTER);
      this.smooth = this.option(
         new BooleanSetting("fps.smooth", "Smooth Mode", "Its own average of the frame times, updated four times a second (instead of once a second).", false)
      );
   }

   @Override
   void frame(float dt, IslandDraw d, IslandStyle s) {
      long frame = GameClock.frame();
      if (frame != this.sampledFrame) {
         long now = System.nanoTime();
         float real;
         if (frame == this.sampledFrame + 1L && (real = Math.min(0.1F, Math.max(0.0F, (float)(now - this.sampledNanos) / 1.0E9F))) > 0.0F && real < 1.0F) {
            this.avgFrame = this.avgFrame < 0.0F ? real : this.avgFrame + (real - this.avgFrame) * (1.0F - (float)Math.exp(-real / 0.5F));
         }

         this.sampledFrame = frame;
         this.sampledNanos = now;
      }

      this.sinceUpdate += dt;
      int fps;
      if (this.smooth.get()) {
         if (this.sinceUpdate < 0.25F && this.value.get().length() > 0) {
            fps = this.shown;
         } else {
            this.sinceUpdate = 0.0F;
            fps = this.avgFrame > 0.0F ? Math.round(1.0F / this.avgFrame) : Minecraft.getDebugFPS();
         }
      } else {
         fps = Minecraft.getDebugFPS();
      }

      if (fps != this.shown || this.value.get().isEmpty()) {
         this.shown = fps;
         this.value.set(Integer.toString(fps), fps, this.wm.rolling());
      }

      this.value.update(dt);
   }

   @Override
   float measure(IslandDraw d, IslandStyle s) {
      return this.iconSpace(s) + this.label.width(d, s) + this.value.width(d, false, s.font);
   }

   @Override
   String panelValue() {
      return Minecraft.getDebugFPS() + " fps";
   }

   @Override
   String panelDescription() {
      return "Frames per second";
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
         this.smooth.get() ? "Frames per second (smoothed)" : "Frames per second",
         false
      );
      return true;
   }
}
