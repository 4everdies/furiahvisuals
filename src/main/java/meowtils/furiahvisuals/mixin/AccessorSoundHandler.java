package meowtils.furiahvisuals.mixin;

import java.lang.reflect.Field;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundRegistry;

public final class AccessorSoundHandler {
   private static final Field SND_REGISTRY = Access.field(SoundHandler.class, "sndRegistry", "sndRegistry");

   private AccessorSoundHandler() {
   }

   public static SoundRegistry getFvSoundRegistry(SoundHandler handler) {
      return SND_REGISTRY == null ? null : (SoundRegistry)Access.get(SND_REGISTRY, handler);
   }
}
