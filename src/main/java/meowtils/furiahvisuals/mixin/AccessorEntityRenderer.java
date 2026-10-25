package meowtils.furiahvisuals.mixin;

import java.lang.reflect.Field;
import net.minecraft.client.renderer.EntityRenderer;

public final class AccessorEntityRenderer {
   private static final Field LIGHTMAP_UPDATE_NEEDED = Access.field(EntityRenderer.class, "lightmapUpdateNeeded", "lightmapUpdateNeeded");
   private final EntityRenderer target;

   private AccessorEntityRenderer(EntityRenderer target) {
      this.target = target;
   }

   public static AccessorEntityRenderer of(EntityRenderer r) {
      return LIGHTMAP_UPDATE_NEEDED != null && r != null ? new AccessorEntityRenderer(r) : null;
   }

   public boolean getFvLightmapUpdateNeeded() {
      try {
         return LIGHTMAP_UPDATE_NEEDED.getBoolean(this.target);
      } catch (Exception var2) {
         return false;
      }
   }

   public void setFvLightmapUpdateNeeded(boolean needed) {
      try {
         LIGHTMAP_UPDATE_NEEDED.setBoolean(this.target, needed);
      } catch (Exception var3) {
      }
   }
}
