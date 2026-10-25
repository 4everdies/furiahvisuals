package meowtils.furiahvisuals.mixin;

import java.lang.reflect.Field;
import net.minecraft.client.gui.GuiTextField;

public final class AccessorGuiTextField {
   private static final Field CURSOR_COUNTER = Access.field(GuiTextField.class, "cursorCounter", "cursorCounter");
   private static final Field LINE_SCROLL_OFFSET = Access.field(GuiTextField.class, "lineScrollOffset", "lineScrollOffset");
   private static final Field ENABLED_COLOR = Access.field(GuiTextField.class, "enabledColor", "enabledColor");
   private final GuiTextField target;

   private AccessorGuiTextField(GuiTextField target) {
      this.target = target;
   }

   public static AccessorGuiTextField of(GuiTextField field) {
      return new AccessorGuiTextField(field);
   }

   public int getFvCursorCounter() {
      return CURSOR_COUNTER == null ? 0 : Access.getInt(CURSOR_COUNTER, this.target);
   }

   public void setFvCursorCounter(int v) {
      if (CURSOR_COUNTER != null) {
         Access.setInt(CURSOR_COUNTER, this.target, v);
      }
   }

   public int getFvLineScrollOffset() {
      return LINE_SCROLL_OFFSET == null ? 0 : Access.getInt(LINE_SCROLL_OFFSET, this.target);
   }

   public int getFvEnabledColor() {
      return ENABLED_COLOR == null ? 14737632 : Access.getInt(ENABLED_COLOR, this.target);
   }

   public void setFvEnabledColor(int v) {
      if (ENABLED_COLOR != null) {
         Access.setInt(ENABLED_COLOR, this.target, v);
      }
   }
}
