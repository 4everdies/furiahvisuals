package meowtils.furiahvisuals.mixin;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;

public final class AccessorGuiNewChat {
   private static final Field CHAT_LINES = Access.field(GuiNewChat.class, "chatLines", "chatLines");
   private final GuiNewChat target;

   private AccessorGuiNewChat(GuiNewChat target) {
      this.target = target;
   }

   public static AccessorGuiNewChat of(GuiNewChat chat) {
      return new AccessorGuiNewChat(chat);
   }

   public List<ChatLine> getFvChatLines() {
      Object l = CHAT_LINES == null ? null : Access.get(CHAT_LINES, this.target);
      return l == null ? Collections.emptyList() : (List)l;
   }
}
