package meowtils.furiahvisuals.render.post;

import meowtils.furiahvisuals.asset.ShaderFiles;
import org.lwjgl.opengl.GL20;

public final class PostProgram {
   private static final String VERTEX = "post/fullscreen.vsh";
   private final String name;
   private final int id;

   private PostProgram(String name, int id) {
      this.name = name;
      this.id = id;
   }

   public static PostProgram linkFile(String name, String fragmentFile, String... samplers) {
      return link(name, ShaderFiles.require(fragmentFile), samplers);
   }

   public static PostProgram link(String name, String fragment, String... samplers) {
      int vs = compile(name, 35633, ShaderFiles.require("post/fullscreen.vsh"));

      int fs;
      try {
         fs = compile(name, 35632, fragment);
      } catch (IllegalStateException var9) {
         GL20.glDeleteShader(vs);
         throw var9;
      }

      int p = GL20.glCreateProgram();
      GL20.glAttachShader(p, vs);
      GL20.glAttachShader(p, fs);
      GL20.glLinkProgram(p);
      GL20.glDeleteShader(vs);
      GL20.glDeleteShader(fs);
      if (GL20.glGetProgrami(p, 35714) == 0) {
         String log = GL20.glGetProgramInfoLog(p, 2048);
         GL20.glDeleteProgram(p);
         throw new IllegalStateException("the " + name + " shader did not link: " + log.trim());
      } else {
         PostProgram program = new PostProgram(name, p);
         GL20.glUseProgram(p);

         for (int i = 0; i < samplers.length; i++) {
            int at = GL20.glGetUniformLocation(p, samplers[i]);
            if (at >= 0) {
               GL20.glUniform1i(at, i);
            }
         }

         return program;
      }
   }

   private static int compile(String name, int type, String source) {
      int s = GL20.glCreateShader(type);
      GL20.glShaderSource(s, source);
      GL20.glCompileShader(s);
      if (GL20.glGetShaderi(s, 35713) == 0) {
         String log = GL20.glGetShaderInfoLog(s, 2048);
         GL20.glDeleteShader(s);
         throw new IllegalStateException("the " + name + " shader did not compile: " + log.trim());
      } else {
         return s;
      }
   }

   public void use() {
      GL20.glUseProgram(this.id);
   }

   public int uniform(String uniform) {
      return GL20.glGetUniformLocation(this.id, uniform);
   }

   public static void set(int at, float x) {
      if (at >= 0) {
         GL20.glUniform1f(at, x);
      }
   }

   public static void set(int at, float x, float y) {
      if (at >= 0) {
         GL20.glUniform2f(at, x, y);
      }
   }

   public static void set(int at, float x, float y, float z) {
      if (at >= 0) {
         GL20.glUniform3f(at, x, y, z);
      }
   }

   public static void set(int at, float x, float y, float z, float w) {
      if (at >= 0) {
         GL20.glUniform4f(at, x, y, z, w);
      }
   }

   public String name() {
      return this.name;
   }

   public void delete() {
      GL20.glDeleteProgram(this.id);
   }

   public static void delete(PostProgram p) {
      if (p != null) {
         p.delete();
      }
   }
}
