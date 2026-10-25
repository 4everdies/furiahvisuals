package meowtils.furiahvisuals.compat;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;

public final class EntityViewRenderEvent {
   private EntityViewRenderEvent() {
   }

   public static class FogColors {
      public final EntityRenderer renderer;
      public final Entity entity;
      public final Block block;
      public final double renderPartialTicks;
      public float red;
      public float green;
      public float blue;

      public FogColors(EntityRenderer renderer, Entity entity, Block block, double renderPartialTicks, float red, float green, float blue) {
         this.renderer = renderer;
         this.entity = entity;
         this.block = block;
         this.renderPartialTicks = renderPartialTicks;
         this.red = red;
         this.green = green;
         this.blue = blue;
      }
   }

   public static class RenderFogEvent {
      public final EntityRenderer renderer;
      public final Entity entity;
      public final Block block;
      public final double renderPartialTicks;
      public final int fogMode;
      public final float farPlaneDistance;

      public RenderFogEvent(EntityRenderer renderer, Entity entity, Block block, double renderPartialTicks, int fogMode, float farPlaneDistance) {
         this.renderer = renderer;
         this.entity = entity;
         this.block = block;
         this.renderPartialTicks = renderPartialTicks;
         this.fogMode = fogMode;
         this.farPlaneDistance = farPlaneDistance;
      }
   }
}
