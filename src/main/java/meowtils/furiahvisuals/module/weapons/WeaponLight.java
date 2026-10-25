package meowtils.furiahvisuals.module.weapons;

import java.lang.reflect.Field;
import meowtils.furiahvisuals.compat.ReflectionHelper;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.module.ambience.FogCapture;
import meowtils.furiahvisuals.module.sky.SkyModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

final class WeaponLight {
   private static Field lightmapField;
   private static boolean looked;
   final float[] light = new float[]{1.0F, 1.0F, 1.0F};
   final float[] up = new float[]{0.0F, 1.0F, 0.0F};
   final float[] sunDir = new float[]{0.0F, 1.0F, 0.0F};
   float sun;
   final float[] sky = new float[3];
   final float[] horizon = new float[3];
   final float[] ground = new float[3];
   private final float[] target = new float[3];
   private final float[] skyColors = new float[6];
   private boolean started;
   private final Mat4 view = new Mat4();
   private final float[] tmp = new float[4];
   final float[] item0 = new float[]{0.0F, 1.0F, 0.0F};
   final float[] item1 = new float[]{0.0F, 1.0F, 0.0F};
   private static final float[] L0 = unit(0.2F, 1.0F, -0.7F);
   private static final float[] L1 = unit(-0.2F, 1.0F, 0.7F);
   private final Mat4 itemTurn = new Mat4();
   private long envFrame = Long.MIN_VALUE;
   private float envPt;
   private float envPitch;
   private float envYaw;
   private World envWorld;
   private float sunOpen;
   private static final long SKY_MS = 500L;
   private static final float[] SKY_COLOR = new float[3];
   private static long skyAt;
   private static World skyWorld;
   private static boolean skyFlash;

   private static float[] unit(float x, float y, float z) {
      float l = (float)Math.sqrt(x * x + y * y + z * z);
      return new float[]{x / l, y / l, z / l};
   }

   private void itemLights() {
      this.itemTurn.direction(L0[0], L0[1], L0[2], this.item0);
      this.itemTurn.direction(L1[0], L1[1], L1[2], this.item1);
      Mat4.normalize(this.item0);
      Mat4.normalize(this.item1);
   }

   void updateFirstPerson(Minecraft mc, float pt, float dt) {
      Entity e = mc.getRenderViewEntity();
      if (e != null && mc.theWorld != null) {
         BlockPos pos = new BlockPos(e.posX, e.posY + e.getEyeHeight(), e.posZ);
         int combined = mc.theWorld.getCombinedLight(pos, 0);
         lightColor(mc, combined, this.target);
         this.smooth(dt);
         float pitch = e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * pt;
         float yaw = e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * pt;
         this.environment(mc, e, pt, pitch, yaw);
         this.itemTurn.identity().rotate(pitch, 1.0F, 0.0F, 0.0F).rotate(yaw, 0.0F, 1.0F, 0.0F);
         this.itemLights();
      } else {
         this.defaults();
      }
   }

   void updateEntity(Minecraft mc, Entity holder, float pt, float viewPitch, float viewYaw) {
      if (mc.theWorld == null) {
         this.defaults();
      } else {
         lightColor(mc, holder.getBrightnessForRender(pt), this.light);
         this.started = true;
         long frame = FxFrame.currentFrame();
         if (frame != this.envFrame || pt != this.envPt || viewPitch != this.envPitch || viewYaw != this.envYaw || mc.theWorld != this.envWorld) {
            Entity viewer = mc.getRenderViewEntity();
            this.environment(mc, viewer != null ? viewer : holder, pt, viewPitch, viewYaw);
            this.itemTurn.set(this.view);
            this.itemLights();
            this.envFrame = frame;
            this.envPt = pt;
            this.envPitch = viewPitch;
            this.envYaw = viewYaw;
            this.envWorld = mc.theWorld;
         }

         this.sun = this.sunUnder(mc.theWorld, holder);
      }
   }

   void studio(float pitch, float yaw) {
      this.light[2] = 1.0F;
      this.light[1] = 1.0F;
      this.light[0] = 1.0F;
      this.started = true;
      this.envFrame = Long.MIN_VALUE;
      this.view.identity().rotate(pitch, 1.0F, 0.0F, 0.0F).rotate(yaw + 180.0F, 0.0F, 1.0F, 0.0F);
      this.view.direction(0.0F, 1.0F, 0.0F, this.up);
      Mat4.set(this.sky, 0.62F, 0.66F, 0.78F);
      Mat4.set(this.horizon, 0.42F, 0.4F, 0.48F);
      Mat4.set(this.ground, 0.12F, 0.11F, 0.13F);
      this.view.direction(-0.4F, 0.8F, 0.45F, this.sunDir);
      Mat4.normalize(this.sunDir);
      this.sun = 0.35F;
      this.itemTurn.identity().rotate(-30.0F, 0.0F, 1.0F, 0.0F).rotate(165.0F, 1.0F, 0.0F, 0.0F);
      this.itemLights();
   }

   private void defaults() {
      this.envFrame = Long.MIN_VALUE;
      this.light[2] = 1.0F;
      this.light[1] = 1.0F;
      this.light[0] = 1.0F;
      Mat4.set(this.up, 0.0F, 1.0F, 0.0F);
      Mat4.set(this.sky, 0.55F, 0.65F, 0.85F);
      Mat4.set(this.horizon, 0.7F, 0.78F, 0.9F);
      Mat4.set(this.ground, 0.2F, 0.18F, 0.16F);
      this.sun = 0.0F;
   }

   private void smooth(float dt) {
      if (!this.started) {
         System.arraycopy(this.target, 0, this.light, 0, 3);
         this.started = true;
      } else if (!(dt <= 0.0F)) {
         float k = 1.0F - (float)Math.exp(-dt / 0.12F);

         for (int i = 0; i < 3; i++) {
            this.light[i] = this.light[i] + (this.target[i] - this.light[i]) * k;
         }
      }
   }

   private static void lightColor(Minecraft mc, int combined, float[] out) {
      int sky = combined >> 20 & 15;
      int block = combined >> 4 & 15;
      int[] map = lightmap(mc.entityRenderer);
      if (map != null && map.length >= 256) {
         int c = map[sky * 16 + block];
         out[0] = (c >> 16 & 0xFF) / 255.0F;
         out[1] = (c >> 8 & 0xFF) / 255.0F;
         out[2] = (c & 0xFF) / 255.0F;
      } else {
         float v = Math.max(sky, block) / 15.0F;
         out[1] = out[2] = 0.05F + 0.95F * v * v;
         out[0] = out[2];
      }
   }

   private static int[] lightmap(EntityRenderer r) {
      if (!looked) {
         looked = true;

         try {
            lightmapField = ReflectionHelper.findField(EntityRenderer.class, new String[]{"lightmapColors", "lightmapColors"});
         } catch (RuntimeException var3) {
            lightmapField = null;
            FuriahsVisuals.LOG.warn("The lightmap could not be read; weapons are lit by the light level instead");
         }
      }

      if (lightmapField == null) {
         return null;
      } else {
         try {
            return (int[])lightmapField.get(r);
         } catch (RuntimeException | IllegalAccessException var2) {
            return null;
         }
      }
   }

   private void environment(Minecraft mc, Entity e, float pt, float pitch, float yaw) {
      WorldClient w = mc.theWorld;
      this.view.identity().rotate(pitch, 1.0F, 0.0F, 0.0F).rotate(yaw + 180.0F, 0.0F, 1.0F, 0.0F);
      this.view.direction(0.0F, 1.0F, 0.0F, this.up);
      FogCapture.color(this.horizon);
      boolean hasSky = !w.provider.getHasNoSky();
      if (hasSky) {
         skyColor(w, e, pt, this.sky);
      } else {
         System.arraycopy(this.horizon, 0, this.sky, 0, 3);
      }

      SkyModule skies = FuriahsVisuals.instance.getSky();
      float k = skies == null ? 0.0F : skies.reflectionColors(this.skyColors);
      if (k > 0.0F) {
         for (int i = 0; i < 3; i++) {
            this.sky[i] = this.sky[i] + (this.skyColors[i] - this.sky[i]) * k;
            this.horizon[i] = this.horizon[i] + (this.skyColors[3 + i] - this.horizon[i]) * k * 0.6F;
         }
      }

      for (int i = 0; i < 3; i++) {
         this.ground[i] = this.horizon[i] * 0.28F + 0.035F;
      }

      float angle = w.getCelestialAngle(pt) * (float) Math.PI * 2.0F;
      float sx = -MathHelper.sin(angle);
      float sy = MathHelper.cos(angle);
      this.view.direction(sx, sy, 0.0F, this.sunDir);
      Mat4.normalize(this.sunDir);
      float day = hasSky ? MathHelper.clamp_float(sy * 2.5F + 0.25F, 0.0F, 1.0F) : 0.0F;
      float rain = w.getRainStrength(pt);
      this.sunOpen = day * (1.0F - rain * 0.9F);
      this.sun = this.sunUnder(w, e);
   }

   private static void skyColor(World w, Entity e, float pt, float[] out) {
      long now = System.currentTimeMillis();
      boolean flash = w.getLastLightningBolt() > 0;
      if (w != skyWorld || now - skyAt >= 500L || flash || skyFlash) {
         Vec3 s = w.getSkyColor(e, pt);
         SKY_COLOR[0] = (float)s.xCoord;
         SKY_COLOR[1] = (float)s.yCoord;
         SKY_COLOR[2] = (float)s.zCoord;
         skyAt = now;
         skyWorld = w;
         skyFlash = flash;
      }

      System.arraycopy(SKY_COLOR, 0, out, 0, 3);
   }

   private float sunUnder(World w, Entity e) {
      if (this.sunOpen <= 0.0F) {
         return 0.0F;
      } else {
         int skyLight = w.getLightFor(EnumSkyBlock.SKY, new BlockPos(e.posX, e.posY + e.getEyeHeight(), e.posZ));
         return this.sunOpen * (skyLight / 15.0F);
      }
   }

   void apply(WeaponShader.Program p) {
      p.set3f("uLight", this.light[0], this.light[1], this.light[2]);
      p.set3f("uUp", this.up[0], this.up[1], this.up[2]);
      p.set3f("uSunDir", this.sunDir[0], this.sunDir[1], this.sunDir[2]);
      p.set1f("uSun", this.sun);
      p.set3f("uSky", this.sky[0], this.sky[1], this.sky[2]);
      p.set3f("uHorizon", this.horizon[0], this.horizon[1], this.horizon[2]);
      p.set3f("uGround", this.ground[0], this.ground[1], this.ground[2]);
   }

   void applyPixel(WeaponShader.Program p) {
      p.set3f("uLight", this.light[0], this.light[1], this.light[2]);
      p.set3f("uL0", this.item0[0], this.item0[1], this.item0[2]);
      p.set3f("uL1", this.item1[0], this.item1[1], this.item1[2]);
   }

   float brightness() {
      return (this.light[0] + this.light[1] + this.light[2]) / 3.0F;
   }
}
