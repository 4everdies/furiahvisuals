package meowtils.furiahvisuals.module.weapons;

import java.util.HashMap;
import java.util.Map;
import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.asset.ShaderFiles;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

final class WeaponShader {
   private static final String ROOT = "weapons/";
   private WeaponShader.Program weapon;
   private WeaponShader.Program composite;
   private WeaponShader.Program blur;
   private WeaponShader.Program icon;
   private WeaponShader.Program beam;
   private WeaponShader.Program pixel;
   private WeaponShader.Program pixelFx;
   private boolean weaponFailed;
   private boolean compositeFailed;
   private boolean blurFailed;
   private boolean iconFailed;
   private boolean beamFailed;
   private boolean pixelFailed;
   private boolean pixelFxFailed;
   private Boolean supported;
   private int filesGeneration = AssetSource.generation();

   private void checkFiles() {
      int g = AssetSource.generation();
      if (g != this.filesGeneration) {
         this.filesGeneration = g;

         for (WeaponShader.Program p : new WeaponShader.Program[]{this.weapon, this.composite, this.blur, this.icon, this.beam, this.pixel, this.pixelFx}) {
            if (p != null) {
               GL20.glDeleteProgram(p.id);
            }
         }

         this.pixelFx = null;
         this.pixel = null;
         this.beam = null;
         this.icon = null;
         this.blur = null;
         this.composite = null;
         this.weapon = null;
         this.pixelFxFailed = false;
         this.pixelFailed = false;
         this.beamFailed = false;
         this.iconFailed = false;
         this.blurFailed = false;
         this.compositeFailed = false;
         this.weaponFailed = false;
      }
   }

   WeaponShader.Program pixel() {
      this.checkFiles();
      if (this.pixel == null && !this.pixelFailed) {
         this.pixel = this.make("pixel.vsh", "pixel.fsh", "pixel");
         boolean bl = this.pixelFailed = this.pixel == null;
         if (this.pixel != null) {
            int previous = GL11.glGetInteger(35725);
            this.pixel.use();
            this.pixel.set1i("uPalette", 0);
            this.pixel.set1i("uGlintTex", 2);
            GL20.glUseProgram(previous);
         }
      }

      return this.pixel;
   }

   WeaponShader.Program pixelFx() {
      this.checkFiles();
      if (this.pixelFx == null && !this.pixelFxFailed) {
         this.pixelFx = this.make("composite.vsh", "pixelfx.fsh", "pixelfx");
         boolean bl = this.pixelFxFailed = this.pixelFx == null;
         if (this.pixelFx != null) {
            int previous = GL11.glGetInteger(35725);
            this.pixelFx.use();
            this.pixelFx.set1i("uTex", 0);
            this.pixelFx.set1f("uLevels", 4.0F);
            GL20.glUseProgram(previous);
         }
      }

      return this.pixelFx;
   }

   WeaponShader.Program beam() {
      this.checkFiles();
      if (this.beam == null && !this.beamFailed) {
         this.beam = this.make("beam.vsh", "beam.fsh", "beam");
         boolean bl = this.beamFailed = this.beam == null;
         if (this.beam != null) {
            int previous = GL11.glGetInteger(35725);
            this.beam.use();
            this.beam.set1i("uTex", 0);
            GL20.glUseProgram(previous);
         }
      }

      return this.beam;
   }

   boolean supported() {
      if (this.supported == null) {
         try {
            this.supported = GLContext.getCapabilities().OpenGL20;
         } catch (RuntimeException | LinkageError var2) {
            this.supported = false;
         }
      }

      return this.supported;
   }

   WeaponShader.Program weapon() {
      this.checkFiles();
      if (this.weapon == null && !this.weaponFailed) {
         this.weapon = this.make("weapon.vsh", "weapon.fsh", "weapon");
         boolean bl = this.weaponFailed = this.weapon == null;
         if (this.weapon != null) {
            int previous = GL11.glGetInteger(35725);
            this.weapon.use();
            this.weapon.set1i("uAlbedo", 0);
            this.weapon.set1i("uMaterial", 1);
            this.weapon.set1i("uNormalMap", 2);
            GL20.glUseProgram(previous);
         }
      }

      return this.weapon;
   }

   WeaponShader.Program composite() {
      this.checkFiles();
      if (this.composite == null && !this.compositeFailed) {
         this.composite = this.make("composite.vsh", "composite.fsh", "composite");
         boolean bl = this.compositeFailed = this.composite == null;
         if (this.composite != null) {
            int previous = GL11.glGetInteger(35725);
            this.composite.use();
            this.composite.set1i("uTex", 0);
            this.composite.set1i("uBloomTex", 1);
            GL20.glUseProgram(previous);
         }
      }

      return this.composite;
   }

   WeaponShader.Program blur() {
      this.checkFiles();
      if (this.blur == null && !this.blurFailed) {
         this.blur = this.make("composite.vsh", "blur.fsh", "blur");
         boolean bl = this.blurFailed = this.blur == null;
         if (this.blur != null) {
            int previous = GL11.glGetInteger(35725);
            this.blur.use();
            this.blur.set1i("uTex", 0);
            GL20.glUseProgram(previous);
         }
      }

      return this.blur;
   }

   WeaponShader.Program icon() {
      this.checkFiles();
      if (this.icon == null && !this.iconFailed) {
         this.icon = this.make("icon.vsh", "icon.fsh", "icon");
         boolean bl = this.iconFailed = this.icon == null;
         if (this.icon != null) {
            int previous = GL11.glGetInteger(35725);
            this.icon.use();
            this.icon.set1i("uTex", 0);
            GL20.glUseProgram(previous);
         }
      }

      return this.icon;
   }

   boolean weaponFailed() {
      this.checkFiles();
      return this.weaponFailed;
   }

   private WeaponShader.Program make(String vertex, String fragment, String name) {
      if (!this.supported()) {
         return null;
      } else {
         String vs = read(vertex);
         String fs = read(fragment);
         if (vs != null && fs != null) {
            int id = link(vs, fs, name);
            return id == 0 ? null : new WeaponShader.Program(id);
         } else {
            if (AssetSource.ready()) {
               FuriahsVisuals.LOG.warn("Weapon shader '{}' is missing from the mod's files", new Object[]{name});
            }

            return null;
         }
      }
   }

   static String read(String path) {
      return ShaderFiles.text("weapons/" + path);
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

         FuriahsVisuals.LOG.warn("Weapon shader '{}' failed to link: {}", new Object[]{name, GL20.glGetProgramInfoLog(p, 4096)});
         GL20.glDeleteProgram(p);
         int n = 0;
         var8 = n;
      } catch (RuntimeException | LinkageError var12) {
         FuriahsVisuals.LOG.warn("Weapon shader '{}' failed", new Object[]{name, var12});
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
         FuriahsVisuals.LOG.warn("Weapon shader '{}' failed to compile: {}", new Object[]{name, GL20.glGetShaderInfoLog(s, 4096)});
         GL20.glDeleteShader(s);
         return 0;
      } else {
         return s;
      }
   }

   static final class Program {
      final int id;
      private final Map<String, Integer> locations = new HashMap<>();

      Program(int id) {
         this.id = id;
      }

      int loc(String name) {
         Integer l = this.locations.get(name);
         if (l == null) {
            l = GL20.glGetUniformLocation(this.id, name);
            this.locations.put(name, l);
         }

         return l;
      }

      void use() {
         GL20.glUseProgram(this.id);
      }

      void set1f(String name, float v) {
         int l = this.loc(name);
         if (l >= 0) {
            GL20.glUniform1f(l, v);
         }
      }

      void set2f(String name, float a, float b) {
         int l = this.loc(name);
         if (l >= 0) {
            GL20.glUniform2f(l, a, b);
         }
      }

      void set3f(String name, float a, float b, float c) {
         int l = this.loc(name);
         if (l >= 0) {
            GL20.glUniform3f(l, a, b, c);
         }
      }

      void set4f(String name, float a, float b, float c, float d) {
         int l = this.loc(name);
         if (l >= 0) {
            GL20.glUniform4f(l, a, b, c, d);
         }
      }

      void setColor(String name, int argb) {
         this.set3f(name, (argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F);
      }

      void set1i(String name, int v) {
         int l = this.loc(name);
         if (l >= 0) {
            GL20.glUniform1i(l, v);
         }
      }
   }
}
