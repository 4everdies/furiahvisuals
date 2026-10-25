package meowtils.furiahvisuals.hook;

import java.lang.reflect.Field;
import meowtils.furiahvisuals.compat.EntityViewRenderEvent;
import meowtils.furiahvisuals.compat.IRenderHandler;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.ambience.AmbienceHooks;
import meowtils.furiahvisuals.module.misc.MiscHooks;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.biome.BiomeGenBase;
import org.lwjgl.opengl.GL11;

public final class WorldHooks {
   static final String[] HOOKS = new String[]{"celestialAngle", "rainStrength", "thunderStrength", "skyLast", "snowFall", "snowSplash"};
   private static final Field FOG_RED = Hooks.field(EntityRenderer.class, "fogColorRed", "fogColorRed");
   private static final Field FOG_GREEN = Hooks.field(EntityRenderer.class, "fogColorGreen", "fogColorGreen");
   private static final Field FOG_BLUE = Hooks.field(EntityRenderer.class, "fogColorBlue", "fogColorBlue");
   private static final Field FAR_PLANE = Hooks.field(EntityRenderer.class, "farPlaneDistance", "farPlaneDistance");
   private static final Field PROVIDER = Hooks.field(World.class, "provider", "provider");
   private static final Field PREV_RAIN = Hooks.field(World.class, "prevRainingStrength", "prevRainingStrength");
   private static final Field RAIN = Hooks.field(World.class, "rainingStrength", "rainingStrength");
   private static final Field PREV_THUNDER = Hooks.field(World.class, "prevThunderingStrength", "prevThunderingStrength");
   private static final Field THUNDER = Hooks.field(World.class, "thunderingStrength", "thunderingStrength");
   private static final Field TEMPERATURE = Hooks.field(BiomeGenBase.class, "temperature", "temperature");
   private static WorldClient upgraded;
   private static boolean skySection;
   private static boolean skyPending;
   private static boolean skySuppressed;
   private static boolean afterSkyPending;
   private static int skyFogCalls;
   private static boolean cloudsSuppressed;
   private static boolean inFog;
   private static boolean weatherApplied;
   private static float[] realWeather = new float[4];
   private static float[] savedTemps;
   private static boolean failed;

   private WorldHooks() {
   }

   static void sync(boolean on) {
      Minecraft mc = Minecraft.getMinecraft();
      WorldClient w = mc.theWorld;
      if (!on) {
         if (w != null) {
            downgrade(w);
         }

         upgraded = null;
         Hooks.off(HOOKS);
      } else if (w != null && w != upgraded && PROVIDER != null) {
         upgraded = w;
         WorldProvider p = w.provider;
         WorldProvider q = null;
         if (p instanceof WorldHooks.FvProvider) {
            q = p;
         } else if (Swap.ours(p, WorldHooks.FvProviderSurface.class) || p.getClass() == WorldProviderSurface.class) {
            q = (WorldProvider)Swap.upgrade(p, WorldProviderSurface.class, WorldHooks.FvProviderSurface.class);
         } else if (Swap.ours(p, WorldHooks.FvProviderHell.class) || p.getClass() == WorldProviderHell.class) {
            q = (WorldProvider)Swap.upgrade(p, WorldProviderHell.class, WorldHooks.FvProviderHell.class);
         } else if (!Swap.ours(p, WorldHooks.FvProviderEnd.class) && p.getClass() != WorldProviderEnd.class) {
            FuriahsVisuals.LOG.warn("Hooks: this world's provider is a {}; sky, fog and time hooks stay off here", new Object[]{p.getClass().getName()});
         } else {
            q = (WorldProvider)Swap.upgrade(p, WorldProviderEnd.class, WorldHooks.FvProviderEnd.class);
         }

         if (q == null) {
            Hooks.off(HOOKS);
         } else {
            if (q != p) {
               IRenderHandler.migrate(p, q);
               Swap.set(PROVIDER, w, q);
            }

            Hooks.on(HOOKS);
         }
      }
   }

   private static void downgrade(WorldClient w) {
      WorldProvider p = w.provider;
      WorldProvider plain = null;
      if (Swap.ours(p, WorldHooks.FvProviderSurface.class)) {
         plain = (WorldProvider)Swap.downgrade(p, WorldProviderSurface.class);
      } else if (Swap.ours(p, WorldHooks.FvProviderHell.class)) {
         plain = (WorldProvider)Swap.downgrade(p, WorldProviderHell.class);
      } else if (Swap.ours(p, WorldHooks.FvProviderEnd.class)) {
         plain = (WorldProvider)Swap.downgrade(p, WorldProviderEnd.class);
      }

      if (plain != null && plain != p) {
         IRenderHandler.migrate(p, plain);
         Swap.set(PROVIDER, w, plain);
      }
   }

   static int dimensionId(WorldProvider self, int real) {
      if (skyPending && !inFog) {
         skyPending = false;
         IRenderHandler sky = IRenderHandler.getSkyRenderer(self);
         if (sky == null) {
            return real;
         } else {
            Minecraft mc = Minecraft.getMinecraft();

            try {
               sky.render(Bridge.partialTicks(), mc.theWorld, mc);
            } catch (Throwable var5) {
               fail(var5);
            }

            skySuppressed = true;
            afterSky();
            return real == 1 ? 0 : real;
         }
      } else {
         return real;
      }
   }

   static boolean surfaceWorld(WorldProvider self, boolean real) {
      if (skySuppressed) {
         skySuppressed = false;
         return false;
      } else if (cloudsSuppressed) {
         cloudsSuppressed = false;
         return false;
      } else {
         return real;
      }
   }

   static void skyColored() {
      if (afterSkyPending) {
         afterSky();
      }
   }

   static boolean fogShows(WorldProvider self, boolean real) {
      if (!inFog && (skySection || Bridge.inWorldRender())) {
         Minecraft mc = Minecraft.getMinecraft();
         int fogMode = skySection && skyFogCalls++ == 0 ? -1 : 0;
         float far = getFloat(FAR_PLANE, mc.entityRenderer, 0.0F);
         if (real) {
            GlStateManager.setFogStart(far * 0.05F);
            GlStateManager.setFogEnd(Math.min(far, 192.0F) * 0.5F);
         }

         inFog = true;

         try {
            Entity e = mc.getRenderViewEntity();
            Block b = e == null ? null : ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, e, Bridge.partialTicks());
            EntityViewRenderEvent.RenderFogEvent event = new EntityViewRenderEvent.RenderFogEvent(mc.entityRenderer, e, b, Bridge.partialTicks(), fogMode, far);
            if (Bridge.active()) {
               Bridge.events().onRenderFog(event);
               Bridge.events().onRenderFogFinal(event);
            }
         } catch (Throwable var11) {
            fail(var11);
         } finally {
            inFog = false;
         }

         return false;
      } else {
         return real;
      }
   }

   static float celestialAngle(World w, float real) {
      if (w != null && w.isRemote && Bridge.active()) {
         float a = MiscHooks.celestialAngle(w);
         return a == a ? a : real;
      } else {
         return real;
      }
   }

   static void fogColors() {
      Minecraft mc = Minecraft.getMinecraft();
      if (FOG_RED != null && FOG_GREEN != null && FOG_BLUE != null && mc.theWorld != null && Bridge.active()) {
         EntityRenderer r = mc.entityRenderer;
         float red = getFloat(FOG_RED, r, 0.0F);
         float green = getFloat(FOG_GREEN, r, 0.0F);
         float blue = getFloat(FOG_BLUE, r, 0.0F);
         Entity e = mc.getRenderViewEntity();
         Block b = e == null ? null : ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, e, Bridge.partialTicks());
         EntityViewRenderEvent.FogColors event = new EntityViewRenderEvent.FogColors(r, e, b, Bridge.partialTicks(), red, green, blue);

         try {
            Bridge.events().onFogColors(event);
            Bridge.events().onFogColorsFinal(event);
         } catch (Throwable var9) {
            fail(var9);
            return;
         }

         if (event.red != red || event.green != green || event.blue != blue) {
            setFloat(FOG_RED, r, event.red);
            setFloat(FOG_GREEN, r, event.green);
            setFloat(FOG_BLUE, r, event.blue);
            GlStateManager.clearColor(event.red, event.green, event.blue, 0.0F);
            GL11.glClear(16384);
         }
      }
   }

   static void skyStart() {
      skySection = true;
      skyFogCalls = 0;
      skyPending = Bridge.active();
      skySuppressed = false;
      afterSkyPending = true;
   }

   static void skyEnd() {
      if (afterSkyPending) {
         afterSky();
      }

      skySection = false;
      skyPending = false;
      skySuppressed = false;
   }

   private static void afterSky() {
      afterSkyPending = false;
      if (Bridge.active()) {
         AmbienceHooks.afterSky(Bridge.partialTicks());
      }
   }

   static void cloudsStart() {
      Minecraft mc = Minecraft.getMinecraft();
      WorldClient w = mc.theWorld;
      IRenderHandler clouds = w != null && Bridge.active() ? IRenderHandler.getCloudRenderer(w.provider) : null;
      if (clouds != null) {
         try {
            clouds.render(Bridge.partialTicks(), w, mc);
         } catch (Throwable var4) {
            fail(var4);
         }

         cloudsSuppressed = true;
      }
   }

   static void cloudsEnd() {
      cloudsSuppressed = false;
   }

   static void applyWeather() {
      WorldClient w = Minecraft.getMinecraft().theWorld;
      if (!weatherApplied && w != null && Bridge.active() && RAIN != null && THUNDER != null && PREV_RAIN != null && PREV_THUNDER != null) {
         float rain = MiscHooks.rain(w);
         float thunder = MiscHooks.thunder(w);
         if (rain == rain || thunder == thunder) {
            realWeather[0] = getFloat(PREV_RAIN, w, 0.0F);
            realWeather[1] = getFloat(RAIN, w, 0.0F);
            realWeather[2] = getFloat(PREV_THUNDER, w, 0.0F);
            realWeather[3] = getFloat(THUNDER, w, 0.0F);
            weatherApplied = true;
            float pt = Bridge.partialTicks();
            float realRain = realWeather[0] + (realWeather[1] - realWeather[0]) * pt;
            float r = rain == rain ? rain : realRain;
            setFloat(PREV_RAIN, w, r);
            setFloat(RAIN, w, r);
            if (thunder == thunder) {
               float t = r > 0.0F ? Math.min(1.0F, thunder / r) : 0.0F;
               setFloat(PREV_THUNDER, w, t);
               setFloat(THUNDER, w, t);
            }
         }
      }
   }

   static void restoreWeather() {
      WorldClient w = Minecraft.getMinecraft().theWorld;
      if (weatherApplied) {
         weatherApplied = false;
         if (w != null) {
            setFloat(PREV_RAIN, w, realWeather[0]);
            setFloat(RAIN, w, realWeather[1]);
            setFloat(PREV_THUNDER, w, realWeather[2]);
            setFloat(THUNDER, w, realWeather[3]);
         }
      }
   }

   static void applyTemperature() {
      WorldClient w = Minecraft.getMinecraft().theWorld;
      if (savedTemps == null && w != null && TEMPERATURE != null && Bridge.active()) {
         BiomeGenBase[] biomes = BiomeGenBase.getBiomeGenArray();
         BiomeGenBase probe = null;

         for (BiomeGenBase b : biomes) {
            if (b != null) {
               probe = b;
               break;
            }
         }

         if (probe != null && MiscHooks.temperature(probe, w.getSpawnPoint()) == 0.0F && probe.temperature != 0.0F) {
            savedTemps = new float[biomes.length];

            for (int i = 0; i < biomes.length; i++) {
               if (biomes[i] != null) {
                  savedTemps[i] = biomes[i].temperature;
                  biomes[i].temperature = 0.0F;
               }
            }
         }
      }
   }

   static void restoreTemperature() {
      if (savedTemps != null) {
         BiomeGenBase[] biomes = BiomeGenBase.getBiomeGenArray();

         for (int i = 0; i < biomes.length && i < savedTemps.length; i++) {
            if (biomes[i] != null) {
               biomes[i].temperature = savedTemps[i];
            }
         }

         savedTemps = null;
      }
   }

   static void frameEnd() {
      restoreWeather();
      restoreTemperature();
      skySection = false;
      skyPending = false;
      skySuppressed = false;
      cloudsSuppressed = false;
      afterSkyPending = false;
   }

   private static float getFloat(Field f, Object o, float fallback) {
      try {
         return f == null ? fallback : f.getFloat(o);
      } catch (IllegalAccessException var4) {
         return fallback;
      }
   }

   private static void setFloat(Field f, Object o, float v) {
      try {
         if (f != null) {
            f.setFloat(o, v);
         }
      } catch (IllegalAccessException var4) {
      }
   }

   private static void fail(Throwable t) {
      if (!failed) {
         failed = true;
         FuriahsVisuals.LOG.error("Hooks: a world hook failed", t);
      }
   }

   interface FvProvider {
   }

   public static class FvProviderEnd extends WorldProviderEnd implements WorldHooks.FvProvider {
      public int getDimensionId() {
         return WorldHooks.dimensionId(this, super.getDimensionId());
      }

      public boolean isSurfaceWorld() {
         return WorldHooks.surfaceWorld(this, super.isSurfaceWorld());
      }

      public boolean isSkyColored() {
         WorldHooks.skyColored();
         return super.isSkyColored();
      }

      public boolean doesXZShowFog(int x, int z) {
         return WorldHooks.fogShows(this, super.doesXZShowFog(x, z));
      }

      public float calculateCelestialAngle(long worldTime, float partialTicks) {
         return WorldHooks.celestialAngle(this.worldObj, super.calculateCelestialAngle(worldTime, partialTicks));
      }
   }

   public static class FvProviderHell extends WorldProviderHell implements WorldHooks.FvProvider {
      public int getDimensionId() {
         return WorldHooks.dimensionId(this, super.getDimensionId());
      }

      public boolean isSurfaceWorld() {
         return WorldHooks.surfaceWorld(this, super.isSurfaceWorld());
      }

      public boolean isSkyColored() {
         WorldHooks.skyColored();
         return super.isSkyColored();
      }

      public boolean doesXZShowFog(int x, int z) {
         return WorldHooks.fogShows(this, super.doesXZShowFog(x, z));
      }

      public float calculateCelestialAngle(long worldTime, float partialTicks) {
         return WorldHooks.celestialAngle(this.worldObj, super.calculateCelestialAngle(worldTime, partialTicks));
      }
   }

   public static class FvProviderSurface extends WorldProviderSurface implements WorldHooks.FvProvider {
      public int getDimensionId() {
         return WorldHooks.dimensionId(this, super.getDimensionId());
      }

      public boolean isSurfaceWorld() {
         return WorldHooks.surfaceWorld(this, super.isSurfaceWorld());
      }

      public boolean isSkyColored() {
         WorldHooks.skyColored();
         return super.isSkyColored();
      }

      public boolean doesXZShowFog(int x, int z) {
         return WorldHooks.fogShows(this, super.doesXZShowFog(x, z));
      }

      public float calculateCelestialAngle(long worldTime, float partialTicks) {
         return WorldHooks.celestialAngle(this.worldObj, super.calculateCelestialAngle(worldTime, partialTicks));
      }
   }
}
