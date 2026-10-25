package meowtils.furiahvisuals.module.watermark.data;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public final class SystemStats {
   private volatile float cpuSystem = -1.0F;
   private volatile float cpuProcess = -1.0F;
   private volatile float gpu = -1.0F;
   private volatile String gpuNote = "GPU usage is not read in this offline build";
   private volatile String cpuNote = "Measuring…";
   private volatile boolean cpuWanted;
   private volatile boolean gpuWanted;
   private volatile long wantedUntil;
   private Thread cpuThread;

   public float cpuSystem() {
      return this.cpuSystem;
   }

   public float cpuProcess() {
      return this.cpuProcess;
   }

   public String cpuNote() {
      return this.cpuNote;
   }

   public float gpu() {
      return this.gpu;
   }

   public String gpuNote() {
      return this.gpuNote;
   }

   public synchronized void want(boolean cpu, boolean gpuToo) {
      long now = System.currentTimeMillis();
      if (cpu || gpuToo) {
         this.wantedUntil = now + 2000L;
      }

      this.cpuWanted = cpu;
      this.gpuWanted = gpuToo;
      if (cpu && (this.cpuThread == null || !this.cpuThread.isAlive())) {
         this.cpuThread = daemon(new Runnable() {
            @Override
            public void run() {
               SystemStats.this.cpuLoop();
            }
         }, "Furiah's Visuals CPU");
      }
   }

   public synchronized void stop() {
      this.cpuWanted = false;
      this.gpuWanted = false;
      this.wantedUntil = 0L;
      if (this.cpuThread != null) {
         this.cpuThread.interrupt();
      }
   }

   private boolean stillWanted(boolean gpuSide) {
      return (gpuSide ? this.gpuWanted : this.cpuWanted) && System.currentTimeMillis() < this.wantedUntil;
   }

   private static Thread daemon(Runnable r, String name) {
      Thread t = new Thread(r, name);
      t.setDaemon(true);
      t.setPriority(1);
      t.start();
      return t;
   }

   private void cpuLoop() {
      OperatingSystemMXBean os;
      try {
         os = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
      } catch (Throwable var8) {
         this.cpuNote = "CPU load is not available on this Java";
         return;
      }

      this.cpuNote = "Whole system, or Minecraft only (see its options)";

      while (this.stillWanted(false)) {
         try {
            double sys = os.getSystemCpuLoad();
            double proc = os.getProcessCpuLoad();
            this.cpuSystem = sys < 0.0 ? -1.0F : (float)(sys * 100.0);
            this.cpuProcess = proc < 0.0 ? -1.0F : (float)(proc * 100.0);
            Thread.sleep(1000L);
         } catch (InterruptedException var6) {
            return;
         } catch (Throwable var7) {
            this.cpuNote = "CPU load is not available on this Java";
            this.cpuProcess = -1.0F;
            this.cpuSystem = -1.0F;
            return;
         }
      }
   }
}
