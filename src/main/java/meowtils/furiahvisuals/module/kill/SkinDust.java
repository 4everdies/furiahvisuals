package meowtils.furiahvisuals.module.kill;

import java.util.Arrays;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.SkinSampler;

public final class SkinDust {
   public final float[] x;
   public final float[] y;
   public final float[] z;
   public final float[] at;
   public final int[] color;
   public final byte[] part;
   public int count;
   public int next;
   private final long[] keys;
   private final float[] tf;
   private final int[] ti;
   private final byte[] tb;
   private final float[] lx;
   private final float[] ly;
   private final float[] lz;
   private final int[] lc;

   public SkinDust(int capacity) {
      this.x = new float[capacity];
      this.y = new float[capacity];
      this.z = new float[capacity];
      this.at = new float[capacity];
      this.color = new int[capacity];
      this.part = new byte[capacity];
      this.keys = new long[capacity];
      this.tf = new float[capacity];
      this.ti = new int[capacity];
      this.tb = new byte[capacity];
      this.lx = new float[capacity];
      this.ly = new float[capacity];
      this.lz = new float[capacity];
      this.lc = new int[capacity];
   }

   public int capacity() {
      return this.x.length;
   }

   public SkinDust sample(KillFx fx, int wanted, SkinDust.Threshold threshold) {
      wanted = Math.min(wanted, this.capacity());
      int per = Math.max(1, wanted / 6);
      int n = 0;
      int fallback = fx.color(0.5F);

      for (int p = 0; p < 6 && n < wanted; p++) {
         BodyProxy.Part part = fx.parts[p];
         if (part.visible) {
            int got = SkinSampler.sample(fx.ctx.body, p, Math.min(per, wanted - n), fx.rng, this.lx, this.ly, this.lz, this.lc, 0, fallback);

            for (int i = 0; i < got; i++) {
               double wx = part.x + part.r[0] * this.lx[i] + part.r[1] * this.ly[i] + part.r[2] * this.lz[i];
               double wy = part.y + part.r[3] * this.lx[i] + part.r[4] * this.ly[i] + part.r[5] * this.lz[i];
               double wz = part.z + part.r[6] * this.lx[i] + part.r[7] * this.ly[i] + part.r[8] * this.lz[i];
               this.x[n] = (float)(wx - fx.ctx.feetX);
               this.y[n] = (float)(wy - fx.ctx.feetY);
               this.z[n] = (float)(wz - fx.ctx.feetZ);
               this.at[n] = threshold == null ? 0.0F : Math.max(0.0F, threshold.at(wx, wy, wz));
               this.color[n] = this.lc[i];
               this.part[n] = (byte)p;
               n++;
            }
         }
      }

      this.count = n;
      this.next = 0;
      if (threshold != null) {
         this.sort();
      }

      return this;
   }

   private void sort() {
      for (int i = 0; i < this.count; i++) {
         this.keys[i] = (long)Float.floatToIntBits(this.at[i]) << 32 | i;
      }

      Arrays.sort(this.keys, 0, this.count);
      this.permute(this.x);
      this.permute(this.y);
      this.permute(this.z);
      this.permute(this.at);

      for (int var2 = 0; var2 < this.count; var2++) {
         this.ti[var2] = this.color[(int)this.keys[var2]];
         this.tb[var2] = this.part[(int)this.keys[var2]];
      }

      System.arraycopy(this.ti, 0, this.color, 0, this.count);
      System.arraycopy(this.tb, 0, this.part, 0, this.count);
   }

   private void permute(float[] a) {
      for (int i = 0; i < this.count; i++) {
         this.tf[i] = a[(int)this.keys[i]];
      }

      System.arraycopy(this.tf, 0, a, 0, this.count);
   }

   public interface Threshold {
      float at(double var1, double var3, double var5);
   }
}
