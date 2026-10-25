package meowtils.furiahvisuals.hook;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;

public class FvProfiler extends Profiler {
   private List<String> stack;

   private List<String> stack() {
      if (this.stack == null) {
         this.stack = new ArrayList<>();
      }

      return this.stack;
   }

   public void startSection(String name) {
      super.startSection(name);
      if (Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
         List<String> stack = this.stack();
         if ("root".equals(name)) {
            stack.clear();
         }

         String parent = stack.isEmpty() ? "" : stack.get(stack.size() - 1);
         String grand = stack.size() < 2 ? "" : stack.get(stack.size() - 2);
         stack.add(name);
         Sections.start(name, parent, grand);
      }
   }

   public void endSection() {
      super.endSection();
      if (Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
         List<String> stack = this.stack();
         if (!stack.isEmpty()) {
            String name = stack.remove(stack.size() - 1);
            String parent = stack.isEmpty() ? "" : stack.get(stack.size() - 1);
            String grand = stack.size() < 2 ? "" : stack.get(stack.size() - 2);
            Sections.end(name, parent, grand);
         }
      }
   }
}
