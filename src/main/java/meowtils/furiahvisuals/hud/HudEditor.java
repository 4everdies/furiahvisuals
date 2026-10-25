package meowtils.furiahvisuals.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

public interface HudEditor {
   static boolean isOpen() {
      return is(Minecraft.getMinecraft().currentScreen);
   }

   static boolean is(GuiScreen screen) {
      return screen instanceof HudEditor;
   }

   static boolean dragging(Object element) {
      GuiScreen s = Minecraft.getMinecraft().currentScreen;
      return s instanceof HudEditor && ((HudEditor)s).isDragging(element);
   }

   boolean isDragging(Object var1);
}
