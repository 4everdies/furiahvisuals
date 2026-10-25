package meowtils.furiahvisuals.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import java.util.function.Supplier;
import meowtils.furiahvisuals.gui.widget.KeyBindWidget;
import meowtils.furiahvisuals.gui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

public final class KeyBindSetting extends Setting implements HasWidget {
   private final Supplier<KeyBinding> binding;

   public KeyBindSetting(String id, String name, String description, Supplier<KeyBinding> binding) {
      super(id, name, description);
      this.binding = binding;
      this.personal();
   }

   public KeyBinding binding() {
      return this.binding.get();
   }

   public int getKeyCode() {
      KeyBinding kb = this.binding.get();
      return kb == null ? 0 : kb.getKeyCode();
   }

   public void set(int keyCode) {
      KeyBinding kb = this.binding.get();
      if (kb != null && kb.getKeyCode() != keyCode) {
         Minecraft.getMinecraft().gameSettings.setOptionKeyBinding(kb, keyCode);
         KeyBinding.resetKeyBindingArrayAndHash();
         this.fireChanged();
      }
   }

   public String keyName() {
      int code = this.getKeyCode();
      return code == 0 ? "None" : GameSettings.getKeyDisplayString(code);
   }

   public String conflict() {
      KeyBinding kb = this.binding.get();
      int code = this.getKeyCode();
      if (kb != null && code != 0) {
         for (KeyBinding other : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if (other != kb && other.getKeyCode() == code) {
               return I18n.format(other.getKeyDescription(), new Object[0]);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   @Override
   public Widget createWidget() {
      return new KeyBindWidget(this);
   }

   @Override
   public boolean isSaved() {
      return false;
   }

   @Override
   public void reset() {
      KeyBinding kb = this.binding.get();
      if (kb != null) {
         this.set(kb.getKeyCodeDefault());
      }
   }

   @Override
   public JsonElement toJson() {
      return JsonNull.INSTANCE;
   }

   @Override
   public void fromJson(JsonElement json) {
   }
}
