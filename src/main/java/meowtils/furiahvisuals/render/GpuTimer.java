package meowtils.furiahvisuals.render;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;

public final class GpuTimer {
   private static final int SLOTS = 4;
   private final int[] queries = new int[4];
   private final boolean[] pending = new boolean[4];
   private int next;
   private boolean checked;
   private boolean supported;
   private boolean running;
   private float millis;
   private float least;

   public void begin() {
      try {
         if (!this.checked) {
            this.checked = true;
            ContextCapabilities caps = GLContext.getCapabilities();
            this.supported = caps.OpenGL33;
            if (this.supported) {
               for (int i = 0; i < 4; i++) {
                  this.queries[i] = GL15.glGenQueries();
               }
            }
         }

         if (!this.supported || this.running) {
            return;
         }

         this.collect();
         if (this.pending[this.next]) {
            return;
         }

         GL15.glBeginQuery(35007, this.queries[this.next]);
         this.running = true;
      } catch (RuntimeException | LinkageError var3) {
         this.supported = false;
         this.running = false;
      }
   }

   public void end() {
      if (this.running) {
         this.running = false;

         try {
            GL15.glEndQuery(35007);
            this.pending[this.next] = true;
            this.next = (this.next + 1) % 4;
         } catch (RuntimeException | LinkageError var2) {
            this.supported = false;
         }
      }
   }

   private void collect() {
      for (int i = 0; i < 4; i++) {
         if (this.pending[i] && GL15.glGetQueryObjecti(this.queries[i], 34919) != 0) {
            long nanos = GL33.glGetQueryObjecti64(this.queries[i], 34918);
            this.pending[i] = false;
            float ms = (float)nanos / 1000000.0F;
            this.millis = this.millis == 0.0F ? ms : this.millis + (ms - this.millis) * 0.1F;
            this.least = this.least == 0.0F ? ms : Math.min(this.least, ms);
         }
      }
   }

   public float millis() {
      return this.millis;
   }

   public float least() {
      return this.least;
   }

   public void reset() {
      this.millis = 0.0F;
      this.least = 0.0F;
   }
}
