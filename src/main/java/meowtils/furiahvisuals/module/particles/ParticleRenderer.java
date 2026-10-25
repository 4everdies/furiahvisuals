package meowtils.furiahvisuals.module.particles;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.mixin.AccessorWorldRenderer;
import meowtils.furiahvisuals.render.GpuTimer;
import meowtils.furiahvisuals.render.OptiFineCompat;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumType;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;

final class ParticleRenderer {
   static final int SOURCE_VANILLA = 0;
   static final int SOURCE_NATIVE = 1;
   static final int SOURCE_CUSTOM = 2;
   private static final int VERTEX_INTS = 7;
   private static final int QUAD_INTS = 28;
   private static final int MAX_BATCHES = 64;
   private static final int LIGHT_FULL = 15728880;
   private static final float TWO_PI = (float) (Math.PI * 2);
   private final ParticleSprites sprites;
   private int[] quads = new int[14336];
   private int[] quadTexture = new int[512];
   private int[] quadBatch = new int[512];
   private int[] sorted = new int[14336];
   private int quadCount;
   private final int[] batchTexture = new int[64];
   private final int[] batchCount = new int[64];
   private final int[] batchCursor = new int[64];
   private static final int GLOW_FLOATS = 6;
   private float[] glowData = new float[3072];
   private int[] glowAverage = new int[512];
   private int[] glowTint = new int[512];
   private float[] glowDist = new float[512];
   private float[] glowSelect = new float[512];
   private int[] glowVertices = new int[14336];
   private int glowCount;
   private final int[] scratch = new int[28];
   private final float[] px = new float[4];
   private final float[] py = new float[4];
   private final float[] pz = new float[4];
   private final float[] pu = new float[4];
   private final float[] pv = new float[4];
   private final int[] pc = new int[4];
   private final int[] pl = new int[4];
   private ByteBuffer viewSource;
   private IntBuffer view;
   private boolean frameActive;
   private boolean reroute;
   private boolean fastWrite;
   private boolean endSeen = true;
   private boolean broken;
   private long now;
   private double clock;
   private double colorClock;
   private float partialTicks;
   private double camX;
   private double camY;
   private double camZ;
   private float rX;
   private float rZ;
   private float uX;
   private float uY;
   private float uZ;
   private int particlesTex;
   private int blocksTex;
   private WorldRenderer lastWorldRenderer;
   private boolean hasAccessor;
   private VertexFormat lastFormat;
   private int fStride;
   private int fPos;
   private int fUv;
   private int fColor;
   private int fLight;
   private final int[] colorSource = new int[3];
   private final ColorSetting[] customColor = new ColorSetting[3];
   private final boolean[] randomPoint = new boolean[3];
   private boolean colorsMineOnly;
   private float scale;
   private int scaleMask;
   private float pulseAmount;
   private float pulseHz;
   private boolean pulseRandom;
   private int pulseMask;
   private boolean spin;
   private boolean spinRandomStart;
   private float spinSpeed;
   private float spinVariance;
   private int spinDirection;
   private int spinMask;
   private float flickerAmount;
   private float flickerHz;
   private boolean flickerStrobe;
   private boolean flickerOpacity;
   private boolean flickerRandom;
   private int flickerMask;
   private float fadeOut;
   private int fadeMask;
   private float closeFade;
   private int closeMask;
   private boolean fullbright;
   private int fullbrightMask;
   private boolean smooth;
   private int smoothMask;
   private int smoothTex;
   private float smoothInsetU;
   private float smoothInsetV;
   private boolean glow;
   private boolean glowCustom;
   private float glowSize;
   private float glowIntensity;
   private ColorSetting glowColorSetting;
   private int glowMask;
   private int maxGlow;
   private final ParticleRenderer.ImageSet[] images = new ParticleRenderer.ImageSet[]{
      ParticleRenderer.ImageSet.EMPTY, ParticleRenderer.ImageSet.EMPTY, ParticleRenderer.ImageSet.EMPTY
   };
   private static final int LUT = 64;
   private final int[][] customLut = new int[][]{new int[64], new int[64], new int[64]};
   private final int[] glowLut = new int[64];
   private boolean measuring;
   private boolean forceMeasuring;
   private long measureUntil;
   private boolean profiling;
   private final GpuTimer gpu = new GpuTimer();
   private final long[] profile = new long[6];
   private final long[] profileTotal = new long[6];
   private int profileFrames;
   private long measuredNanos;
   private long frameStart;
   private int lastParticles;
   private int lastGlows;
   private int lastBatches;
   private float lastMillis;
   private float lastFrameMillis;
   private int frameParticles;
   private static final int TRIG = 1024;
   private static final float[] SIN = new float[1025];

   ParticleRenderer(ParticleSprites sprites) {
      this.sprites = sprites;
   }

   static float glowStrength(int average) {
      return Math.max(0.3F, (float)Math.sqrt((average >>> 24) / 255.0F));
   }

   boolean isFrameActive() {
      return this.frameActive;
   }

   void setMeasuring(boolean on) {
      this.forceMeasuring = on;
   }

   void requestMeasuring() {
      this.measureUntil = System.nanoTime() + 1000000000L;
   }

   void setProfiling(boolean on) {
      this.profiling = on;
      Arrays.fill(this.profileTotal, 0L);
      this.profileFrames = 0;
   }

   String profileReport() {
      StringBuilder sb = new StringBuilder();
      String[] names = new String[]{"process", "glow build", "glow draw", "sort", "copy", "draw"};

      for (int i = 0; i < names.length; i++) {
         sb.append(names[i]).append(String.format(Locale.ROOT, " %.3f ms", this.profileTotal[i] / 1000000.0 / Math.max(1, this.profileFrames)));
         sb.append(i + 1 < names.length ? ", " : "");
      }

      return sb.append(" (").append(this.profileFrames).append(" frames)").toString();
   }

   int getParticleCount() {
      return this.lastParticles;
   }

   int getGlowCount() {
      return this.lastGlows;
   }

   int getBatchCount() {
      return this.lastBatches;
   }

   float getMillis() {
      return this.lastMillis;
   }

   float getFrameMillis() {
      return this.lastFrameMillis;
   }

   boolean canUseBatches() {
      return !this.broken && this.endSeen && HookStatus.has("drawEnd") && AccessorWorldRenderer.available();
   }

   boolean isBroken() {
      return this.broken;
   }

   void begin(ParticlesModule module, float partialTicks) {
      this.frameStart = System.nanoTime();
      boolean bl = this.measuring = this.forceMeasuring || this.frameStart < this.measureUntil;
      if (this.quadCount > 0) {
         this.endSeen = false;
      }

      this.quadCount = 0;
      this.glowCount = 0;
      this.frameParticles = 0;
      boolean bl2 = this.frameActive = !this.broken && module.isActive() && HookStatus.has("drawParticle");
      if (this.frameActive) {
         long t0 = this.measuring ? System.nanoTime() : 0L;
         this.now = GameClock.nanos();
         this.clock = GameClock.seconds();
         this.colorClock = ColorSetting.clock();
         this.partialTicks = partialTicks;
         this.lastWorldRenderer = Tessellator.getInstance().getWorldRenderer();
         this.hasAccessor = AccessorWorldRenderer.available();
         this.reroute = this.canUseBatches();
         this.fastWrite = this.reroute && !OptiFineCompat.shadersActive();
         Vec3 cam = ActiveRenderInfo.getPosition();
         this.camX = cam.xCoord;
         this.camY = cam.yCoord;
         this.camZ = cam.zCoord;
         this.readSettings(module);
         if (this.measuring) {
            this.measuredNanos = System.nanoTime() - t0;
         }
      }
   }

   private void readSettings(ParticlesModule m) {
      for (int kind = 1; kind <= 2; kind++) {
         ParticlesModule.ColorSource source = m.colorSource(kind).get();
         this.colorSource[kind] = source == ParticlesModule.ColorSource.NATIVE ? 1 : (source == ParticlesModule.ColorSource.CUSTOM ? 2 : 0);
         this.customColor[kind] = m.customColor(kind);
         this.randomPoint[kind] = m.randomPoint(kind).get();
         this.images[kind] = this.reroute ? m.imageSet(kind) : ParticleRenderer.ImageSet.EMPTY;
      }

      this.colorsMineOnly = m.colorScope.get() == ParticlesModule.Scope.MINE;
      this.scale = m.scale.getFloat();
      this.scaleMask = m.scaleApplies.get().mask;
      this.pulseAmount = m.pulseAmount.getFloat() / 100.0F;
      this.pulseHz = m.pulseSpeed.getFloat();
      this.pulseRandom = m.pulseRandom.get();
      this.pulseMask = m.pulseApplies.get().mask;
      this.spin = m.spin.get();
      this.spinSpeed = (float)Math.toRadians(m.spinSpeed.get());
      this.spinVariance = m.spinVariance.getFloat() / 100.0F;
      this.spinDirection = m.spinDirection.get().ordinal();
      this.spinRandomStart = m.spinRandomStart.get();
      this.spinMask = m.spinApplies.get().mask;
      this.flickerAmount = m.flickerAmount.getFloat() / 100.0F;
      this.flickerHz = m.flickerSpeed.getFloat();
      this.flickerStrobe = m.flickerStyle.get() == ParticlesModule.FlickerStyle.STROBE;
      this.flickerOpacity = m.flickerAffects.get() == ParticlesModule.FlickerTarget.OPACITY;
      this.flickerRandom = m.flickerRandom.get();
      this.flickerMask = m.flickerApplies.get().mask;
      this.fadeOut = m.fadeOut.getFloat() / 100.0F;
      this.fadeMask = m.fadeApplies.get().mask;
      this.closeFade = m.closeFade.getFloat();
      this.closeMask = m.closeApplies.get().mask;
      this.fullbright = m.fullbright.get();
      this.fullbrightMask = m.fullbrightApplies.get().mask;
      this.smooth = m.smoothFiltering.get() && this.reroute;
      this.smoothMask = m.smoothApplies.get().mask;
      this.smoothTex = this.smooth ? this.sprites.smoothTexture() : -1;
      this.smoothInsetU = 0.5F / Math.max(1, this.sprites.width());
      this.smoothInsetV = 0.5F / Math.max(1, this.sprites.height());
      this.glow = m.glow.get() && this.reroute;
      this.glowSize = m.glowSize.getFloat();
      this.glowIntensity = m.glowIntensity.getFloat();
      this.glowCustom = m.glowColorMode.get() == ParticlesModule.GlowColor.CUSTOM;
      this.glowColorSetting = m.glowColor;
      this.glowMask = m.glowApplies.get().mask;
      this.maxGlow = m.maxGlowing.getInt();

      for (int var7 = 1; var7 <= 2; var7++) {
         if (this.colorSource[var7] == 2) {
            this.fillLut(this.customLut[var7], this.customColor[var7]);
         }
      }

      if (this.glow && this.glowCustom) {
         this.fillLut(this.glowLut, this.glowColorSetting);
      }

      if (this.reroute) {
         TextureManager tm = Minecraft.getMinecraft().getTextureManager();
         ITextureObject particles = tm.getTexture(ParticleSprites.PARTICLES);
         ITextureObject blocks = tm.getTexture(TextureMap.locationBlocksTexture);
         this.particlesTex = particles == null ? -1 : particles.getGlTextureId();
         int n = this.blocksTex = blocks == null ? -1 : blocks.getGlTextureId();
         if (this.particlesTex <= 0 || this.blocksTex <= 0) {
            this.reroute = false;
            this.fastWrite = false;
            this.smooth = false;
            this.glow = false;
            this.images[1] = ParticleRenderer.ImageSet.EMPTY;
            this.images[2] = ParticleRenderer.ImageSet.EMPTY;
         }
      }
   }

   void draw(EntityFX fx, WorldRenderer wr, Entity entity, float pt, float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
      if (wr != this.lastWorldRenderer) {
         this.lastWorldRenderer = wr;
         this.hasAccessor = AccessorWorldRenderer.available();
      }

      int start = this.vertexCount(wr);
      fx.renderParticle(wr, entity, pt, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
      int end = this.vertexCount(wr);
      int n = end - start;
      if (n >= 4 && (n & 3) == 0) {
         boolean timed = (this.measuring || this.profiling) && (this.frameParticles & 7) == 0;
         long t0 = timed ? System.nanoTime() : 0L;
         this.rX = rotationX;
         this.rZ = rotationYZ;
         this.uX = rotationXY;
         this.uY = rotationZ;
         this.uZ = rotationXZ;

         try {
            this.process(fx, ParticleTag.of(fx), wr, start, end);
            this.frameParticles++;
         } catch (RuntimeException | LinkageError var18) {
            this.broken = true;
            this.frameActive = false;
            ParticlesModule.reportRenderError(var18);
         }

         if (timed) {
            long dt = (System.nanoTime() - t0) * 8L;
            this.measuredNanos += dt;
            this.profile[0] = this.profile[0] + dt;
         }
      }
   }

   private int vertexCount(WorldRenderer wr) {
      return this.hasAccessor ? AccessorWorldRenderer.of(wr).getFvVertexCount() : wr.getVertexCount();
   }

   private IntBuffer view(ByteBuffer buffer) {
      if (buffer != this.viewSource) {
         this.viewSource = buffer;
         ByteBuffer d = buffer.duplicate();
         d.order(ByteOrder.nativeOrder());
         ((Buffer)d).clear();
         this.view = d.asIntBuffer();
      }

      return this.view;
   }

   private boolean layout(VertexFormat format) {
      if (format == this.lastFormat) {
         return this.fStride > 0;
      } else {
         this.lastFormat = format;
         this.fStride = 0;
         this.fLight = -1;
         this.fColor = -1;
         this.fUv = -1;
         this.fPos = -1;
         List elements = format.getElements();

         for (int i = 0; i < elements.size(); i++) {
            VertexFormatElement e = (VertexFormatElement)elements.get(i);
            int offset = format.getOffset(i);
            switch (e.getUsage()) {
               case POSITION:
                  if (e.getType() == EnumType.FLOAT && e.getElementCount() == 3) {
                     this.fPos = offset;
                  }
                  break;
               case UV:
                  if (e.getIndex() == 0 && e.getType() == EnumType.FLOAT && e.getElementCount() == 2) {
                     this.fUv = offset;
                  } else if (e.getIndex() == 1 && e.getType() == EnumType.SHORT && e.getElementCount() == 2) {
                     this.fLight = offset;
                  }
                  break;
               case COLOR:
                  if (e.getType() == EnumType.UBYTE && e.getElementCount() == 4) {
                     this.fColor = offset;
                  }
            }
         }

         if (this.fPos >= 0 && this.fUv >= 0 && this.fColor >= 0) {
            this.fStride = format.getNextOffset();
         }

         return this.fStride > 0;
      }
   }

   private void process(EntityFX fx, ParticleTag tag, WorldRenderer wr, int start, int end) {
      VertexFormat format = wr.getVertexFormat();
      if (this.layout(format)) {
         boolean batchable = this.reroute && this.hasAccessor && format == DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP && this.fLight >= 0;
         ByteBuffer buf = wr.getByteBuffer();
         int t = tag.getFvTag();
         int kind = t & 3;
         int bit = 1 << kind;
         int seed = tag.getFvSeed();
         boolean mine = (t & 4) != 0;
         int layer = fx.getFXLayer();
         float life = !(this.fadeOut > 0.0F) && this.colorSource[kind] != 2 && !this.glowCustom
            ? 0.0F
            : Math.max(0.0F, Math.min(1.0F, (tag.getFvAge() + this.partialTicks) / Math.max(1, tag.getFvMaxAge())));
         ParticleRenderer.ImageSet set = this.images[kind];
         int image = -1;
         int slot;
         if ((t & 16) != 0 && batchable && set.textures.length > 0 && set.textures[slot = ((t & 0xFF00) >>> 8) % set.textures.length] > 0) {
            image = slot;
         }

         float s = 1.0F;
         if ((this.scaleMask & bit) != 0) {
            s *= this.scale;
         }

         if (this.pulseAmount > 0.0F && (this.pulseMask & bit) != 0) {
            double cycles = this.clock * this.pulseHz + (this.pulseRandom ? (seed & 0xFF) * 0.00390625F : 0.0F);
            s *= Math.max(0.0F, 1.0F + this.pulseAmount * sin((float)(cycles - Math.floor(cycles)) * (float) (Math.PI * 2)));
         }

         if (image >= 0) {
            s *= set.scale;
         }

         float angle = 0.0F;
         if (this.spin && (this.spinMask & bit) != 0) {
            float dir = this.spinDirection == 1 ? 1.0F : (this.spinDirection == 2 ? -1.0F : ((seed >>> 8 & 1) == 0 ? -1.0F : 1.0F));
            float speed = this.spinSpeed * (1.0F + this.spinVariance * ((seed >>> 9 & 0xFF) * 0.007843138F - 1.0F));
            float startAngle = this.spinRandomStart ? (seed >>> 17 & 0xFF) * 0.024543693F : 0.0F;
            angle = startAngle + dir * speed * (float)((this.now - tag.getFvBorn()) * 1.0E-9);
         }

         int source = this.colorSource[kind];
         if (this.colorsMineOnly && !mine) {
            source = 0;
         }

         if ((t & 8) != 0 && image < 0 && source == 1) {
            source = 0;
         }

         int custom = source == 2 ? this.customLut[kind][this.randomPoint[kind] ? seed >>> 25 & 63 : lutIndex(life)] : 0;
         float bright = 1.0F;
         float alpha = 1.0F;
         if (this.flickerAmount > 0.0F && (this.flickerMask & bit) != 0) {
            float k = this.flicker(seed);
            if (this.flickerOpacity) {
               alpha *= k;
            } else {
               bright *= k;
            }
         }

         if (this.fadeOut > 0.0F && (this.fadeMask & bit) != 0 && life > 1.0F - this.fadeOut) {
            float f = Math.max(0.0F, Math.min(1.0F, (1.0F - life) / this.fadeOut));
            alpha *= f * f * (3.0F - 2.0F * f);
         }

         boolean smoothed = batchable && image < 0 && this.smooth && this.smoothTex > 0 && layer != 1 && (this.smoothMask & bit) != 0;
         boolean glows = this.glow && batchable && (this.glowMask & bit) != 0;
         boolean full = glows || this.fullbright && (this.fullbrightMask & bit) != 0;
         boolean closeFades = this.closeFade > 0.0F && (this.closeMask & bit) != 0;
         boolean seeThrough = alpha < 1.0F || closeFades || source == 2 && custom >>> 24 < 255;
         boolean batch = batchable && (image >= 0 || smoothed || glows || seeThrough && fx.getAlpha() == 1.0F);
         float cos = angle == 0.0F ? 1.0F : cos(angle);
         float sin = angle == 0.0F ? 0.0F : sin(angle);
         boolean moves = s != 1.0F || angle != 0.0F;

         for (int v0 = start; v0 < end; v0 += 4) {
            int base = v0 * this.fStride;

            for (int i = 0; i < 4; i++) {
               int o = base + i * this.fStride;
               this.px[i] = buf.getFloat(o + this.fPos);
               this.py[i] = buf.getFloat(o + this.fPos + 4);
               this.pz[i] = buf.getFloat(o + this.fPos + 8);
               this.pu[i] = buf.getFloat(o + this.fUv);
               this.pv[i] = buf.getFloat(o + this.fUv + 4);
               this.pc[i] = buf.getInt(o + this.fColor);
               this.pl[i] = this.fLight >= 0 ? buf.getInt(o + this.fLight) : 0;
            }

            float cx = (this.px[0] + this.px[1] + this.px[2] + this.px[3]) * 0.25F;
            float cy = (this.py[0] + this.py[1] + this.py[2] + this.py[3]) * 0.25F;
            float cz = (this.pz[0] + this.pz[1] + this.pz[2] + this.pz[3]) * 0.25F;
            float uMin = Math.min(Math.min(this.pu[0], this.pu[1]), Math.min(this.pu[2], this.pu[3]));
            float uMax = Math.max(Math.max(this.pu[0], this.pu[1]), Math.max(this.pu[2], this.pu[3]));
            float vMin = Math.min(Math.min(this.pv[0], this.pv[1]), Math.min(this.pv[2], this.pv[3]));
            float vMax = Math.max(Math.max(this.pv[0], this.pv[1]), Math.max(this.pv[2], this.pv[3]));
            float oa = (this.px[0] - cx) * this.rX + (this.pz[0] - cz) * this.rZ;
            float ob = (this.px[0] - cx) * this.uX + (this.py[0] - cy) * this.uY + (this.pz[0] - cz) * this.uZ;
            float half = Math.max(Math.abs(oa), Math.abs(ob));
            if (moves && angle == 0.0F) {
               for (int i = 0; i < 4; i++) {
                  this.px[i] = cx + (this.px[i] - cx) * s;
                  this.py[i] = cy + (this.py[i] - cy) * s;
                  this.pz[i] = cz + (this.pz[i] - cz) * s;
               }
            } else if (moves) {
               float sc = s * cos;
               float ss = s * sin;

               for (int i = 0; i < 4; i++) {
                  float ox = this.px[i] - cx;
                  float oy = this.py[i] - cy;
                  float oz = this.pz[i] - cz;
                  float a = ox * this.rX + oz * this.rZ;
                  float b = ox * this.uX + oy * this.uY + oz * this.uZ;
                  float na = a * sc - b * ss;
                  float nb = a * ss + b * sc;
                  this.px[i] = cx + na * this.rX + nb * this.uX;
                  this.py[i] = cy + nb * this.uY;
                  this.pz[i] = cz + na * this.rZ + nb * this.uZ;
               }
            }

            float quadAlpha = alpha;
            if (closeFades) {
               float dx = cx - (float)this.camX;
               float dy = cy - (float)this.camY;
               float dz = cz - (float)this.camZ;
               float f = Math.min(1.0F, (float)Math.sqrt(dx * dx + dy * dy + dz * dz) / this.closeFade);
               quadAlpha = alpha * (f * f * (3.0F - 2.0F * f));
            }

            if (this.pc[0] == this.pc[1] && this.pc[1] == this.pc[2] && this.pc[2] == this.pc[3]) {
               this.pc[2] = this.pc[3] = shade(this.pc[0], source, custom, bright, quadAlpha);
               this.pc[1] = this.pc[3];
               this.pc[0] = this.pc[3];
            } else {
               for (int i = 0; i < 4; i++) {
                  this.pc[i] = shade(this.pc[i], source, custom, bright, quadAlpha);
               }
            }

            if (full) {
               this.pl[3] = 15728880;
               this.pl[2] = 15728880;
               this.pl[1] = 15728880;
               this.pl[0] = 15728880;
            }

            int texture;
            if (image >= 0) {
               texture = set.textures[image];
               float du = uMax - uMin;
               float dv = vMax - vMin;
               float iu = du > 0.0F ? 1.0F / du : 0.0F;
               float ivv = dv > 0.0F ? 1.0F / dv : 0.0F;

               for (int i = 0; i < 4; i++) {
                  this.pu[i] = du > 0.0F ? (this.pu[i] - uMin) * iu : 0.5F;
                  this.pv[i] = dv > 0.0F ? (this.pv[i] - vMin) * ivv : 0.5F;
               }
            } else if (smoothed) {
               texture = this.smoothTex;
               float du = uMax - uMin;
               float dv = vMax - vMin;
               if (du > 2.0F * this.smoothInsetU && dv > 2.0F * this.smoothInsetV) {
                  float ku = (du - 2.0F * this.smoothInsetU) / du;
                  float kv = (dv - 2.0F * this.smoothInsetV) / dv;

                  for (int i = 0; i < 4; i++) {
                     this.pu[i] = uMin + this.smoothInsetU + (this.pu[i] - uMin) * ku;
                     this.pv[i] = vMin + this.smoothInsetV + (this.pv[i] - vMin) * kv;
                  }
               }
            } else {
               texture = layer == 1 ? this.blocksTex : this.particlesTex;
            }

            if (glows) {
               float strength;
               int average;
               if (image >= 0) {
                  average = set.averages[image];
                  strength = set.glowStrength[image];
               } else if (layer == 1) {
                  average = this.sprites.snowballAverage();
                  strength = 1.0F;
               } else {
                  int cell = ParticleSprites.cell(uMin, vMin);
                  average = this.sprites.cellAverage(cell);
                  strength = this.sprites.cellGlowStrength(cell);
               }

               this.addGlow(cx, cy, cz, half * s * this.glowSize, average, strength, this.pc[0], life);
            }

            if (batch) {
               this.store(texture);
            } else {
               for (int i = 0; i < 4; i++) {
                  int o = base + i * this.fStride;
                  buf.putFloat(o + this.fPos, this.px[i]);
                  buf.putFloat(o + this.fPos + 4, this.py[i]);
                  buf.putFloat(o + this.fPos + 8, this.pz[i]);
                  buf.putInt(o + this.fColor, this.pc[i]);
                  if (this.fLight >= 0) {
                     buf.putInt(o + this.fLight, this.pl[i]);
                  }
               }
            }
         }

         if (batch) {
            AccessorWorldRenderer.of(wr).setFvVertexCount(start);
         }
      }
   }

   private float flicker(int seed) {
      double x = this.clock * this.flickerHz + (this.flickerRandom ? rand(seed, 6) * 64.0 : 0.0);
      if (this.flickerStrobe) {
         return x - Math.floor(x) < 0.5 ? 1.0F : 1.0F - this.flickerAmount;
      } else {
         long i = (long)Math.floor(x);
         float f = (float)(x - i);
         int salt = this.flickerRandom ? seed : 0;
         float n0 = hash01(salt, (int)i);
         float n1 = hash01(salt, (int)(i + 1L));
         float k = f * f * (3.0F - 2.0F * f);
         return 1.0F - this.flickerAmount * (n0 + (n1 - n0) * k);
      }
   }

   private void fillLut(int[] lut, ColorSetting color) {
      for (int i = 0; i < 64; i++) {
         lut[i] = color.getColor(i / 63.0F, this.colorClock);
      }
   }

   private static int lutIndex(float position) {
      int i = (int)(position * 63.0F + 0.5F);
      return i < 0 ? 0 : (i >= 64 ? 63 : i);
   }

   private static int mul255(int x, int y) {
      int t = x * y + 128;
      return t + (t >> 8) >> 8;
   }

   private static int shade(int c, int source, int custom, float bright, float alpha) {
      int r = c & 0xFF;
      int g = c >>> 8 & 0xFF;
      int b = c >>> 16 & 0xFF;
      int a = c >>> 24;
      if (source == 1) {
         b = 255;
         g = 255;
         r = 255;
      } else if (source == 2) {
         r = custom >> 16 & 0xFF;
         g = custom >> 8 & 0xFF;
         b = custom & 0xFF;
         a = mul255(a, custom >>> 24);
      }

      if (bright != 1.0F) {
         r = (int)(r * bright);
         g = (int)(g * bright);
         b = (int)(b * bright);
      }

      if (alpha != 1.0F) {
         a = Math.min(255, (int)(a * alpha + 0.5F));
      }

      return r | g << 8 | b << 16 | a << 24;
   }

   private int glowColor(int average, float strength, int tint, float life) {
      float a = (tint >>> 24) / 255.0F * this.glowIntensity * strength;
      int b;
      int g;
      int r;
      if (this.glowCustom) {
         int c = this.glowLut[lutIndex(life)];
         r = c >> 16 & 0xFF;
         g = c >> 8 & 0xFF;
         b = c & 0xFF;
         a *= (c >>> 24) * 0.003921569F;
         int lum = Math.max(tint & 0xFF, Math.max(tint >>> 8 & 0xFF, tint >>> 16 & 0xFF));
         r = mul255(r, lum);
         g = mul255(g, lum);
         b = mul255(b, lum);
      } else {
         r = mul255(average >> 16 & 0xFF, tint & 0xFF);
         g = mul255(average >> 8 & 0xFF, tint >>> 8 & 0xFF);
         b = mul255(average & 0xFF, tint >>> 16 & 0xFF);
      }

      if (a > 1.0F) {
         r = Math.min(255, (int)(r * a));
         g = Math.min(255, (int)(g * a));
         b = Math.min(255, (int)(b * a));
         a = 1.0F;
      }

      return (int)(a * 255.0F + 0.5F) << 24 | r << 16 | g << 8 | b;
   }

   private void store(int texture) {
      if (this.quadCount == this.quadTexture.length) {
         int n = this.quadCount * 2;
         int[] q = new int[n * 28];
         System.arraycopy(this.quads, 0, q, 0, this.quadCount * 28);
         this.quads = q;
         int[] tx = new int[n];
         System.arraycopy(this.quadTexture, 0, tx, 0, this.quadCount);
         this.quadTexture = tx;
         this.quadBatch = new int[n];
         this.sorted = new int[n * 28];
      }

      int o = this.quadCount * 28;
      int[] q = this.quads;

      for (int i = 0; i < 4; o += 7) {
         q[o] = Float.floatToRawIntBits(this.px[i]);
         q[o + 1] = Float.floatToRawIntBits(this.py[i]);
         q[o + 2] = Float.floatToRawIntBits(this.pz[i]);
         q[o + 3] = Float.floatToRawIntBits(this.pu[i]);
         q[o + 4] = Float.floatToRawIntBits(this.pv[i]);
         q[o + 5] = this.pc[i];
         q[o + 6] = this.pl[i];
         i++;
      }

      this.quadTexture[this.quadCount++] = texture;
   }

   private void addGlow(float x, float y, float z, float half, int average, float strength, int tint, float life) {
      if (tint >>> 24 != 0 && !(half <= 0.0F)) {
         if (this.glowCount == this.glowTint.length) {
            int n = this.glowCount * 2;
            float[] d = new float[n * 6];
            System.arraycopy(this.glowData, 0, d, 0, this.glowCount * 6);
            this.glowData = d;
            int[] a = new int[n];
            System.arraycopy(this.glowAverage, 0, a, 0, this.glowCount);
            this.glowAverage = a;
            int[] t = new int[n];
            System.arraycopy(this.glowTint, 0, t, 0, this.glowCount);
            this.glowTint = t;
            float[] dist = new float[n];
            System.arraycopy(this.glowDist, 0, dist, 0, this.glowCount);
            this.glowDist = dist;
            this.glowSelect = new float[n];
            this.glowVertices = new int[n * 28];
         }

         int o = this.glowCount * 6;
         this.glowData[o] = x;
         this.glowData[o + 1] = y;
         this.glowData[o + 2] = z;
         this.glowData[o + 3] = half;
         this.glowData[o + 4] = strength;
         this.glowData[o + 5] = life;
         this.glowAverage[this.glowCount] = average;
         this.glowTint[this.glowCount] = tint;
         float dx = x - (float)this.camX;
         float dy = y - (float)this.camY;
         float dz = z - (float)this.camZ;
         this.glowDist[this.glowCount] = dx * dx + dy * dy + dz * dz;
         this.glowCount++;
      }
   }

   void end() {
      this.endSeen = true;
      this.lastParticles = this.frameParticles;
      if (this.frameActive && (this.quadCount != 0 || this.glowCount != 0)) {
         long t0 = this.measuring ? System.nanoTime() : 0L;
         if (this.profiling) {
            this.gpu.begin();
         }

         try {
            Tessellator tess = Tessellator.getInstance();
            WorldRenderer wr = tess.getWorldRenderer();
            GlStateManager.enableBlend();
            GlStateManager.depthMask(false);
            GlStateManager.alphaFunc(516, 0.003921569F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.lastGlows = this.glowCount > 0 ? this.drawGlows(tess, wr) : 0;
            GlStateManager.blendFunc(770, 771);
            this.lastBatches = this.quadCount > 0 ? this.drawQuads(tess, wr) : 0;
         } catch (RuntimeException | LinkageError var10) {
            this.broken = true;
            ParticlesModule.reportRenderError(var10);
         } finally {
            GlStateManager.depthMask(true);
            GlStateManager.blendFunc(770, 771);
            GlStateManager.disableBlend();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.quadCount = 0;
            this.glowCount = 0;
            if (this.profiling) {
               this.gpu.end();
            }
         }

         this.finishMeasure(this.measuring ? System.nanoTime() - t0 : 0L);
      } else {
         this.lastGlows = 0;
         this.lastBatches = 0;
         this.finishMeasure(0L);
         this.quadCount = 0;
         this.glowCount = 0;
      }
   }

   float getGpuMillis() {
      return this.gpu.millis();
   }

   private void finishMeasure(long endNanos) {
      this.lastFrameMillis = (float)(System.nanoTime() - this.frameStart) / 1000000.0F;
      if (this.measuring) {
         this.lastMillis = (float)(this.measuredNanos + endNanos) / 1000000.0F;
         this.measuredNanos = 0L;
      }

      if (this.profiling) {
         for (int i = 0; i < this.profile.length; i++) {
            this.profileTotal[i] = this.profileTotal[i] + this.profile[i];
            this.profile[i] = 0L;
         }

         this.profileFrames++;
      }
   }

   private int drawQuads(Tessellator tess, WorldRenderer wr) {
      long t0 = this.profiling ? System.nanoTime() : 0L;
      int batches = 0;

      for (int i = 0; i < this.quadCount; i++) {
         int texture = this.quadTexture[i];
         int b = 0;

         while (b < batches && this.batchTexture[b] != texture) {
            b++;
         }

         if (b == batches) {
            if (batches == 64) {
               b = 0;
            } else {
               this.batchTexture[batches] = texture;
               this.batchCount[batches] = 0;
               batches++;
            }
         }

         this.batchCount[b]++;
         this.quadBatch[i] = b;
      }

      int cursor = 0;

      for (int b = 0; b < batches; b++) {
         this.batchCursor[b] = cursor;
         cursor += this.batchCount[b];
      }

      for (int var16 = 0; var16 < this.quadCount; var16++) {
         int n = this.quadBatch[var16];
         int n2 = this.batchCursor[n]++;
         System.arraycopy(this.quads, var16 * 28, this.sorted, n2 * 28, 28);
      }

      long t1 = this.profiling ? System.nanoTime() : 0L;
      int first = 0;

      for (int b = 0; b < batches; b++) {
         GlStateManager.bindTexture(this.batchTexture[b]);
         wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
         long w0 = this.profiling ? System.nanoTime() : 0L;
         this.write(wr, this.sorted, first * 28, this.batchCount[b] * 28);
         long w1 = this.profiling ? System.nanoTime() : 0L;
         tess.draw();
         if (this.profiling) {
            this.profile[4] = this.profile[4] + (w1 - w0);
            this.profile[5] = this.profile[5] + (System.nanoTime() - w1);
         }

         first += this.batchCount[b];
      }

      if (this.profiling) {
         this.profile[3] = this.profile[3] + (t1 - t0);
      }

      return batches;
   }

   private void write(WorldRenderer wr, int[] src, int off, int len) {
      if (this.fastWrite && wr.getVertexFormat() == DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP) {
         ByteBuffer buffer = wr.getByteBuffer();
         AccessorWorldRenderer acc = AccessorWorldRenderer.of(wr);
         int count = acc.getFvVertexCount();
         if ((count * 7L + len) * 4L <= buffer.capacity()) {
            IntBuffer iv = this.view(buffer);
            ((Buffer)iv).position(count * 7);
            iv.put(src, off, len);
            acc.setFvVertexCount(count + len / 7);
            return;
         }
      }

      boolean bulk = wr.getVertexFormat() == DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP;

      for (int o = off; o < off + len; o += 28) {
         if (bulk) {
            System.arraycopy(src, o, this.scratch, 0, 28);
            wr.addVertexData(this.scratch);
         } else {
            int i = 0;

            for (int v = o; i < 4; v += 7) {
               int c = src[v + 5];
               int l = src[v + 6];
               wr.pos(Float.intBitsToFloat(src[v]), Float.intBitsToFloat(src[v + 1]), Float.intBitsToFloat(src[v + 2]))
                  .tex(Float.intBitsToFloat(src[v + 3]), Float.intBitsToFloat(src[v + 4]))
                  .color(c & 0xFF, c >>> 8 & 0xFF, c >>> 16 & 0xFF, c >>> 24)
                  .lightmap(l >>> 16 & 65535, l & 65535)
                  .endVertex();
               i++;
            }
         }
      }
   }

   private int drawGlows(Tessellator tess, WorldRenderer wr) {
      long t0 = this.profiling ? System.nanoTime() : 0L;
      float limit = Float.MAX_VALUE;
      int allowed = this.glowCount;
      if (this.glowCount > this.maxGlow) {
         System.arraycopy(this.glowDist, 0, this.glowSelect, 0, this.glowCount);
         limit = select(this.glowSelect, this.glowCount, this.maxGlow - 1);
         allowed = this.maxGlow;
      }

      int drawn = 0;
      int[] out = this.glowVertices;

      for (int i = 0; i < this.glowCount && drawn < allowed; i++) {
         if (!(this.glowDist[i] > limit)) {
            int o = i * 6;
            float x = this.glowData[o];
            float y = this.glowData[o + 1];
            float z = this.glowData[o + 2];
            float h = this.glowData[o + 3];
            int argb = this.glowColor(this.glowAverage[i], this.glowData[o + 4], this.glowTint[i], this.glowData[o + 5]);
            if (argb >>> 24 != 0) {
               int c = argb >> 16 & 0xFF | argb & 0xFF00 | (argb & 0xFF) << 16 | argb & 0xFF000000;
               int v = drawn * 28;
               glowVertex(out, v, x - this.rX * h - this.uX * h, y - this.uY * h, z - this.rZ * h - this.uZ * h, 1.0F, 1.0F, c);
               glowVertex(out, v + 7, x - this.rX * h + this.uX * h, y + this.uY * h, z - this.rZ * h + this.uZ * h, 1.0F, 0.0F, c);
               glowVertex(out, v + 14, x + this.rX * h + this.uX * h, y + this.uY * h, z + this.rZ * h + this.uZ * h, 0.0F, 0.0F, c);
               glowVertex(out, v + 21, x + this.rX * h - this.uX * h, y - this.uY * h, z + this.rZ * h - this.uZ * h, 0.0F, 1.0F, c);
               drawn++;
            }
         }
      }

      if (drawn == 0) {
         return 0;
      } else {
         long t1 = this.profiling ? System.nanoTime() : 0L;
         Textures.ensureLoaded();
         GlStateManager.bindTexture(Textures.GLOW);
         GlStateManager.blendFunc(770, 1);
         wr.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
         this.write(wr, out, 0, drawn * 28);
         tess.draw();
         GlStateManager.blendFunc(770, 771);
         if (this.profiling) {
            long t2 = System.nanoTime();
            this.profile[1] = this.profile[1] + (t1 - t0);
            this.profile[2] = this.profile[2] + (t2 - t1);
         }

         return drawn;
      }
   }

   private static void glowVertex(int[] out, int o, float x, float y, float z, float u, float v, int color) {
      out[o] = Float.floatToRawIntBits(x);
      out[o + 1] = Float.floatToRawIntBits(y);
      out[o + 2] = Float.floatToRawIntBits(z);
      out[o + 3] = Float.floatToRawIntBits(u);
      out[o + 4] = Float.floatToRawIntBits(v);
      out[o + 5] = color;
      out[o + 6] = 15728880;
   }

   private static float select(float[] a, int n, int k) {
      int lo = 0;
      int hi = n - 1;

      while (lo < hi) {
         float pivot = a[lo + hi >>> 1];
         int i = lo;
         int j = hi;

         while (i <= j) {
            while (a[i] < pivot) {
               i++;
            }

            while (a[j] > pivot) {
               j--;
            }

            if (i <= j) {
               float t = a[i];
               a[i] = a[j];
               a[j] = t;
               i++;
               j--;
            }
         }

         if (k <= j) {
            hi = j;
         } else {
            if (k < i) {
               break;
            }

            lo = i;
         }
      }

      return a[k];
   }

   static float sin(float radians) {
      float x = radians * 162.97466F;
      int i = (int)x;
      if (x < i) {
         i--;
      }

      float f = x - i;
      int var4;
      return SIN[var4 = i & 1023] + (SIN[var4 + 1] - SIN[var4]) * f;
   }

   static float cos(float radians) {
      return sin(radians + (float) (Math.PI / 2));
   }

   static float rand(int seed, int salt) {
      int h = seed * -1640531527 + salt * -2048144789;
      h ^= h >>> 16;
      h *= 2146121005;
      h ^= h >>> 15;
      h *= -2073254261;
      h ^= h >>> 16;
      return (h >>> 8) * 5.9604645E-8F;
   }

   private static float hash01(int seed, int i) {
      return rand(seed ^ 1540483477, i);
   }

   static {
      for (int i = 0; i <= 1024; i++) {
         SIN[i] = (float)Math.sin(i * Math.PI * 2.0 / 1024.0);
      }
   }

   static final class ImageSet {
      static final ParticleRenderer.ImageSet EMPTY = new ParticleRenderer.ImageSet(new int[0], new int[0], 1.0F);
      final int[] textures;
      final int[] averages;
      final float[] glowStrength;
      final float scale;

      ImageSet(int[] textures, int[] averages, float scale) {
         this.textures = textures;
         this.averages = averages;
         this.scale = scale;
         this.glowStrength = new float[averages.length];

         for (int i = 0; i < averages.length; i++) {
            this.glowStrength[i] = ParticleRenderer.glowStrength(averages[i]);
         }
      }
   }
}
