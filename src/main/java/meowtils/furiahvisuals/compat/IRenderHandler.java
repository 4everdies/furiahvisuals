package meowtils.furiahvisuals.compat;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldProvider;

public abstract class IRenderHandler {
   private static final Map<WorldProvider, IRenderHandler> SKY = new WeakHashMap<>();
   private static final Map<WorldProvider, IRenderHandler> CLOUDS = new WeakHashMap<>();

   public abstract void render(float var1, WorldClient var2, Minecraft var3);

   public static IRenderHandler getSkyRenderer(WorldProvider provider) {
      return SKY.get(provider);
   }

   public static void setSkyRenderer(WorldProvider provider, IRenderHandler handler) {
      if (handler == null) {
         SKY.remove(provider);
      } else {
         SKY.put(provider, handler);
      }
   }

   public static void migrate(WorldProvider from, WorldProvider to) {
      IRenderHandler sky = SKY.remove(from);
      if (sky != null) {
         SKY.put(to, sky);
      }

      IRenderHandler clouds = CLOUDS.remove(from);
      if (clouds != null) {
         CLOUDS.put(to, clouds);
      }
   }

   public static IRenderHandler getCloudRenderer(WorldProvider provider) {
      return CLOUDS.get(provider);
   }

   public static void setCloudRenderer(WorldProvider provider, IRenderHandler handler) {
      if (handler == null) {
         CLOUDS.remove(provider);
      } else {
         CLOUDS.put(provider, handler);
      }
   }
}
