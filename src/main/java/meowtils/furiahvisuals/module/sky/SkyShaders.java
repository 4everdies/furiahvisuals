package meowtils.furiahvisuals.module.sky;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.asset.ShaderFiles;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

final class SkyShaders {
   private static final String VERTEX = "sky/sky.vsh";
   private static final String PRELUDE = "sky/prelude.glsl";
   static final String COMPOSITE_VERTEX = "sky/composite.vsh";
   static final String COMPOSITE_FRAGMENT = "sky/composite.fsh";
   private final SkyShaders.Program[] programs = new SkyShaders.Program[SkyStyle.values().length];
   private final boolean[] failed = new boolean[SkyStyle.values().length];
   private String head;
   private String tail;
   private boolean sourcesFailed;
   private int noiseTexture = -1;
   private int compositeProgram = -1;
   private int compositeTex;
   private int compositeAlpha;
   private int filesGeneration = AssetSource.generation();
   private Boolean supported;

   private void checkFiles() {
      int g = AssetSource.generation();
      if (g != this.filesGeneration) {
         this.filesGeneration = g;

         for (int i = 0; i < this.programs.length; i++) {
            if (this.programs[i] != null) {
               GL20.glDeleteProgram(this.programs[i].id);
               this.programs[i] = null;
            }

            this.failed[i] = false;
         }

         this.head = null;
         this.tail = null;
         this.sourcesFailed = false;
         if (this.compositeProgram > 0) {
            GL20.glDeleteProgram(this.compositeProgram);
         }

         this.compositeProgram = -1;
      }
   }

   SkyShaders.Program get(SkyStyle style) {
      this.checkFiles();
      int i = style.ordinal();
      if (this.programs[i] != null || this.failed[i]) {
         return this.programs[i];
      } else if (GLContext.getCapabilities().OpenGL20 && this.loadSources()) {
         String body = read("styles/" + style.shaderName() + ".glsl");
         String vertex = ShaderFiles.text("sky/sky.vsh");
         String prelude = ShaderFiles.text("sky/prelude.glsl");
         if (body != null && vertex != null && prelude != null) {
            int id = link(vertex, prelude + this.head + "\n" + body + "\n" + this.tail, style.displayName());
            if (id == 0) {
               this.failed[i] = true;
               return null;
            } else {
               this.programs[i] = new SkyShaders.Program(id);
               return this.programs[i];
            }
         } else {
            if (AssetSource.ready()) {
               FuriahsVisuals.LOG.warn("Sky style {} has no shader file", new Object[]{style.displayName()});
            }

            this.failed[i] = true;
            return null;
         }
      } else {
         this.failed[i] = true;
         return null;
      }
   }

   boolean isAvailable(SkyStyle style) {
      return this.get(style) != null;
   }

   boolean supported() {
      if (this.supported == null) {
         this.supported = GLContext.getCapabilities().OpenGL20;
      }

      return this.supported;
   }

   int composite() {
      this.checkFiles();
      if (this.compositeProgram < 0) {
         String vertex = ShaderFiles.text("sky/composite.vsh");
         String fragment = ShaderFiles.text("sky/composite.fsh");
         int n = this.compositeProgram = vertex != null && fragment != null ? link(vertex, fragment, "composite") : 0;
         if (this.compositeProgram != 0) {
            this.compositeTex = GL20.glGetUniformLocation(this.compositeProgram, "uTex");
            this.compositeAlpha = GL20.glGetUniformLocation(this.compositeProgram, "uAlpha");
         }
      }

      return this.compositeProgram;
   }

   int compositeTexLocation() {
      return this.compositeTex;
   }

   int compositeAlphaLocation() {
      return this.compositeAlpha;
   }

   private boolean loadSources() {
      if (this.head != null) {
         return true;
      } else if (this.sourcesFailed) {
         return false;
      } else {
         this.head = read("head.glsl");
         this.tail = read("tail.glsl");
         if (this.head != null && this.tail != null) {
            return true;
         } else {
            if (AssetSource.ready()) {
               FuriahsVisuals.LOG.warn("Sky shaders are missing from the mod's files; the vanilla sky stays");
            }

            this.sourcesFailed = true;
            this.head = null;
            return false;
         }
      }
   }

   private static String read(String path) {
      return ShaderFiles.text("sky/" + path);
   }

   private static int link(String vertex, String fragment, String name) {
      int previous = GL11.glGetInteger(35725);

      int var8;
      try {
         int vs = compile(35633, vertex, name);
         int fs = compile(35632, fragment, name);
         if (vs == 0 || fs == 0) {
            return 0;
         }

         int p = GL20.glCreateProgram();
         GL20.glAttachShader(p, vs);
         GL20.glAttachShader(p, fs);
         GL20.glLinkProgram(p);
         GL20.glDeleteShader(vs);
         GL20.glDeleteShader(fs);
         if (GL20.glGetProgrami(p, 35714) != 0) {
            return p;
         }

         FuriahsVisuals.LOG.warn("Sky shader '{}' failed to link: {}", new Object[]{name, GL20.glGetProgramInfoLog(p, 4096)});
         GL20.glDeleteProgram(p);
         int n = 0;
         var8 = n;
      } catch (RuntimeException | LinkageError var12) {
         FuriahsVisuals.LOG.warn("Sky shader '{}' failed", new Object[]{name, var12});
         return 0;
      } finally {
         GL20.glUseProgram(previous);
      }

      return var8;
   }

   private static int compile(int type, String source, String name) {
      int s = GL20.glCreateShader(type);
      GL20.glShaderSource(s, source);
      GL20.glCompileShader(s);
      if (GL20.glGetShaderi(s, 35713) == 0) {
         FuriahsVisuals.LOG.warn("Sky shader '{}' failed to compile: {}", new Object[]{name, GL20.glGetShaderInfoLog(s, 4096)});
         GL20.glDeleteShader(s);
         return 0;
      } else {
         return s;
      }
   }

   int noiseTexture() {
      if (this.noiseTexture >= 0) {
         return this.noiseTexture;
      } else {
         int n = 256;
         int[] red = new int[65536];
         int[] blue = new int[65536];
         int x = -1640531527;

         for (int i = 0; i < 65536; i++) {
            int var10 = x ^ x << 13;
            int var11 = var10 ^ var10 >>> 17;
            int var12 = var11 ^ var11 << 5;
            red[i] = var12 & 0xFF;
            int var13 = var12 ^ var12 << 13;
            int var14 = var13 ^ var13 >>> 17;
            x = var14 ^ var14 << 5;
            blue[i] = x & 0xFF;
         }

         ByteBuffer data = BufferUtils.createByteBuffer(262144);

         for (int y = 0; y < 256; y++) {
            for (int xx = 0; xx < 256; xx++) {
               int i = y * 256 + xx;
               int j = (y + 17 & 0xFF) * 256 + (xx + 37 & 0xFF);
               data.put((byte)red[i]).put((byte)red[j]).put((byte)blue[i]).put((byte)blue[j]);
            }
         }

         ((Buffer)data).flip();
         this.noiseTexture = TextureUtil.glGenTextures();
         GlStateManager.bindTexture(this.noiseTexture);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 10497);
         GL11.glTexParameteri(3553, 10243, 10497);
         GL11.glTexImage2D(3553, 0, 32856, 256, 256, 0, 6408, 5121, data);
         return this.noiseTexture;
      }
   }

   static final class Program {
      final int id;
      final int noise;
      final int time;
      final int detail;
      final int scale;
      final int colA;
      final int colB;
      final int parallax;
      final int clouds;
      final int sunOwn;
      final int rain;
      final int flash;
      final int flashDir;
      final int stars;
      final int twinkle;
      final int starCol;
      final int bright;
      final int sat;
      final int alpha;
      final int spin;
      final int custom;
      final int image;
      final int hasImage;
      final int optA;
      final int optB;
      final int beat;
      final int look;
      final int contrast;
      final int[] meteorA = new int[4];
      final int[] meteorB = new int[4];

      Program(int id) {
         this.id = id;
         this.noise = loc(id, "uNoise");
         this.time = loc(id, "uTime");
         this.detail = loc(id, "uDetail");
         this.scale = loc(id, "uScale");
         this.colA = loc(id, "uColA");
         this.colB = loc(id, "uColB");
         this.parallax = loc(id, "uParallax");
         this.clouds = loc(id, "uClouds");
         this.sunOwn = loc(id, "uSunOwn");
         this.rain = loc(id, "uRain");
         this.flash = loc(id, "uFlash");
         this.flashDir = loc(id, "uFlashDir");
         this.stars = loc(id, "uStars");
         this.twinkle = loc(id, "uTwinkle");
         this.starCol = loc(id, "uStarCol");

         for (int i = 0; i < 4; i++) {
            this.meteorA[i] = loc(id, "uMeteorA[" + i + "]");
            this.meteorB[i] = loc(id, "uMeteorB[" + i + "]");
         }

         this.bright = loc(id, "uBright");
         this.sat = loc(id, "uSat");
         this.alpha = loc(id, "uAlpha");
         this.spin = loc(id, "uSpin");
         this.custom = loc(id, "uCustom");
         this.image = loc(id, "uImage");
         this.hasImage = loc(id, "uHasImage");
         this.optA = loc(id, "uOptA");
         this.optB = loc(id, "uOptB");
         this.beat = loc(id, "uBeat");
         this.look = loc(id, "uLook");
         this.contrast = loc(id, "uContrast");
      }

      private static int loc(int program, String name) {
         return GL20.glGetUniformLocation(program, name);
      }
   }
}
