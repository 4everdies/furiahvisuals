package meowtils.furiahvisuals.module.watermark;

import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.module.watermark.data.SystemStats;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.TextSetting;

final class SystemItem extends IslandItem {
   final BooleanSetting showCpu;
   final BooleanSetting showGpu;
   final ModeSetting<SystemItem.CpuScope> cpuScope;
   final BooleanSetting labels;
   final TextSetting cpuLabel;
   final TextSetting gpuLabel;
   final BooleanSetting percent;
   private final RollingText cpu = new RollingText();
   private final RollingText gpu = new RollingText();
   private int lastCpu = Integer.MIN_VALUE;
   private int lastGpu = Integer.MIN_VALUE;
   private float cpuLabelW;
   private float gpuLabelW;
   private float pctW;
   private float dotW;
   private String cpuShown = "";
   private String gpuShown = "";
   private String measuredCpu;
   private String measuredGpu;
   private int measuredPx;

   SystemItem(WaterMarkModule wm) {
      super(wm, "system", "CPU & GPU", "Processor and graphics card load.", IslandIcons.Icon.CHIP);
      this.icons(IslandIcons.Icon.CHIP, new IslandIcons.Icon[]{IslandIcons.Icon.CHIP, IslandIcons.Icon.BOLT, IslandIcons.Icon.CHART, IslandIcons.Icon.MONITOR});
      this.showCpu = this.option(new BooleanSetting("system.cpu", "Show CPU", "The processor's load.", true));
      this.showGpu = this.option(new BooleanSetting("system.gpu", "Show GPU", "The graphics card's load.", true));
      this.cpuScope = this.option(
         new ModeSetting<>("system.cpuScope", "CPU", "What the CPU value measures.", SystemItem.CpuScope.class, SystemItem.CpuScope.SYSTEM)
      );
      this.labels = this.option(new BooleanSetting("system.labels", "Show Labels", "Writes \"CPU\" and \"GPU\" (or your own words) before the values.", true));
      BooleanSupplier labelsOn = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return SystemItem.this.labels.get();
         }
      };
      this.cpuLabel = this.option(
         new TextSetting("system.cpuLabel", "CPU Label", "What the CPU label says (at most 16 characters).", "CPU", 16).visibleWhen(labelsOn)
      );
      this.gpuLabel = this.option(
         new TextSetting("system.gpuLabel", "GPU Label", "What the GPU label says (at most 16 characters).", "GPU", 16).visibleWhen(labelsOn)
      );
      this.percent = this.option(new BooleanSetting("system.percent", "Percent Sign", "Writes % after the values.", true));
   }

   @Override
   boolean available() {
      return this.showCpu.get() || this.showGpu.get();
   }

   @Override
   void tick() {
      SystemStats stats = this.wm.stats();
      int c = Math.round(this.cpuScope.get() == SystemItem.CpuScope.MINECRAFT ? stats.cpuProcess() : stats.cpuSystem());
      int g = Math.round(stats.gpu());
      if (c != this.lastCpu || this.cpu.get().isEmpty()) {
         this.lastCpu = c;
         this.cpu.set(c < 0 ? "—" : Integer.toString(c), c, this.wm.rolling());
      }

      if (g != this.lastGpu || this.gpu.get().isEmpty()) {
         this.lastGpu = g;
         this.gpu.set(g < 0 ? "N/A" : Integer.toString(g), g, this.wm.rolling());
      }
   }

   @Override
   String panelValue() {
      if (!this.available()) {
         return "Nothing to show (CPU and GPU off)";
      } else {
         StringBuilder sb = new StringBuilder();
         if (this.showCpu.get()) {
            sb.append("CPU ").append(this.cpu.get().isEmpty() ? "—" : this.cpu.get()).append(this.lastCpu >= 0 ? "%" : "");
         }

         if (this.showGpu.get()) {
            sb.append(sb.length() > 0 ? " · " : "").append("GPU ").append(this.gpu.get().isEmpty() ? "—" : this.gpu.get()).append(this.lastGpu >= 0 ? "%" : "");
         }

         return sb.toString();
      }
   }

   @Override
   String panelDescription() {
      return "System usage";
   }

   boolean wantsCpu() {
      return this.showCpu.get();
   }

   boolean wantsGpu() {
      return this.showGpu.get();
   }

   @Override
   void frame(float dt, IslandDraw d, IslandStyle s) {
      this.cpu.update(dt);
      this.gpu.update(dt);
   }

   private void metrics(IslandDraw d, IslandStyle s) {
      int px = d.pixels(s.font);
      String c = this.labels.get() ? this.cpuLabel.get().trim() : "";
      String g = this.labels.get() ? this.gpuLabel.get().trim() : "";
      if (px != this.measuredPx || !c.equals(this.measuredCpu) || !g.equals(this.measuredGpu)) {
         this.measuredPx = px;
         this.measuredCpu = c;
         this.measuredGpu = g;
         this.cpuShown = c.isEmpty() ? "" : c + " ";
         this.gpuShown = g.isEmpty() ? "" : g + " ";
         this.cpuLabelW = this.cpuShown.isEmpty() ? 0.0F : d.width(false, s.small, this.cpuShown);
         this.gpuLabelW = this.gpuShown.isEmpty() ? 0.0F : d.width(false, s.small, this.gpuShown);
         this.pctW = d.width(false, s.small, "%");
         this.dotW = d.width(false, s.font, "  ·  ");
      }
   }

   @Override
   float measure(IslandDraw d, IslandStyle s) {
      this.metrics(d, s);
      float w = this.iconSpace(s);
      boolean pct = this.percent.get();
      if (this.showCpu.get()) {
         w += this.cpuLabelW + this.cpu.width(d, false, s.font) + (pct && this.lastCpu >= 0 ? this.pctW : 0.0F);
      }

      if (this.showCpu.get() && this.showGpu.get()) {
         w += this.dotW;
      }

      if (this.showGpu.get()) {
         w += this.gpuLabelW + this.gpu.width(d, false, s.font) + (pct && this.lastGpu >= 0 ? this.pctW : 0.0F);
      }

      return w;
   }

   @Override
   void draw(IslandDraw d, IslandStyle s, float x, float cy) {
      this.metrics(d, s);
      x = this.drawIcon(d, s, x, cy);
      float smallTop = d.sameBaseline(false, s.font, d.middleTop(false, s.font, cy), s.small);
      float smallMid = d.middleTop(false, s.small, cy);
      boolean pct = this.percent.get();
      if (this.showCpu.get()) {
         if (this.cpuLabelW > 0.0F) {
            d.text(false, s.small, this.cpuShown, x, smallMid, s.dim);
            x += this.cpuLabelW;
         }

         this.cpu.draw(d, false, s.font, x, cy, s.text);
         x += this.cpu.width(d, false, s.font);
         if (pct && this.lastCpu >= 0) {
            d.text(false, s.small, "%", x, smallTop, s.dim);
            x += this.pctW;
         }
      }

      if (this.showCpu.get() && this.showGpu.get()) {
         d.textMiddle(false, s.font, "  ·  ", x, cy, s.muted);
         x += this.dotW;
      }

      if (this.showGpu.get()) {
         if (this.gpuLabelW > 0.0F) {
            d.text(false, s.small, this.gpuShown, x, smallMid, s.dim);
            x += this.gpuLabelW;
         }

         this.gpu.draw(d, false, s.font, x, cy, this.lastGpu < 0 ? s.muted : s.text);
         x += this.gpu.width(d, false, s.font);
         if (pct && this.lastGpu >= 0) {
            d.text(false, s.small, "%", x, smallTop, s.dim);
         }
      }
   }

   @Override
   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      SystemStats stats = this.wm.stats();
      String tip = "CPU: " + (this.cpuScope.get() == SystemItem.CpuScope.MINECRAFT ? "Minecraft's share" : "whole system") + "  ·  GPU: " + stats.gpuNote();
      hit.set(this, x - s.pad, cy - s.height / 2.0F, this.measure(this.wm.draw(), s) + s.pad * 2.0F, s.height, 0, tip, false);
      return true;
   }

   static enum CpuScope implements Displayable {
      SYSTEM("Whole system", "The load of the whole computer, like Task Manager"),
      MINECRAFT("Minecraft only", "Only this game's share of the computer");

      private final String displayName;
      private final String description;

      private CpuScope(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }
}
