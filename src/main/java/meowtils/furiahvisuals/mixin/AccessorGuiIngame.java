package meowtils.furiahvisuals.mixin;

import java.lang.reflect.Field;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.item.ItemStack;

public final class AccessorGuiIngame {
   private static final Field REMAINING_HIGHLIGHT_TICKS = Access.field(GuiIngame.class, "remainingHighlightTicks", "remainingHighlightTicks");
   private static final Field HIGHLIGHTING_ITEM_STACK = Access.field(GuiIngame.class, "highlightingItemStack", "highlightingItemStack");
   private final GuiIngame target;

   private AccessorGuiIngame(GuiIngame target) {
      this.target = target;
   }

   public static AccessorGuiIngame of(GuiIngame gui) {
      return new AccessorGuiIngame(gui);
   }

   public int getFvRemainingHighlightTicks() {
      return REMAINING_HIGHLIGHT_TICKS == null ? 0 : Access.getInt(REMAINING_HIGHLIGHT_TICKS, this.target);
   }

   public ItemStack getFvHighlightingItemStack() {
      return HIGHLIGHTING_ITEM_STACK == null ? null : (ItemStack)Access.get(HIGHLIGHTING_ITEM_STACK, this.target);
   }
}
