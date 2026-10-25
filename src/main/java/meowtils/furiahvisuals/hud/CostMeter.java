package meowtils.furiahvisuals.hud;

import java.util.Arrays;

public final class CostMeter {
   private final float[] times = new float[120];
   private final float[] sorted = new float[120];
   private int n;
   private int next;

   public void add(long nanos) {
      this.times[this.next] = (float)nanos / 1000000.0F;
      this.next = (this.next + 1) % this.times.length;
      this.n = Math.min(this.n + 1, this.times.length);
   }

   public String describe() {
      if (this.n == 0) {
         return "-";
      } else {
         System.arraycopy(this.times, 0, this.sorted, 0, this.n);
         Arrays.sort(this.sorted, 0, this.n);
         return String.format("%.3f ms (90%%: %.3f)", this.sorted[this.n / 2], this.sorted[Math.min(this.n - 1, (int)(this.n * 0.9F))]);
      }
   }

   public void reset() {
      this.n = 0;
      this.next = 0;
   }
}
