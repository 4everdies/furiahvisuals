package meowtils.furiahvisuals.mixin;

import java.lang.reflect.Field;
import net.minecraft.client.renderer.WorldRenderer;

public final class AccessorWorldRenderer {
   private static final Field VERTEX_COUNT = Access.field(WorldRenderer.class, "vertexCount", "vertexCount");
   private final WorldRenderer target;

   private AccessorWorldRenderer(WorldRenderer target) {
      this.target = target;
   }

   public static boolean available() {
      return VERTEX_COUNT != null;
   }

   public static AccessorWorldRenderer of(WorldRenderer wr) {
      return new AccessorWorldRenderer(wr);
   }

   public int getFvVertexCount() {
      return VERTEX_COUNT == null ? this.target.getVertexCount() : Access.getInt(VERTEX_COUNT, this.target);
   }

   public void setFvVertexCount(int v) {
      if (VERTEX_COUNT != null) {
         Access.setInt(VERTEX_COUNT, this.target, v);
      }
   }
}
