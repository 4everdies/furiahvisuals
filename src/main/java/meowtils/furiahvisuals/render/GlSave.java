package meowtils.furiahvisuals.render;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import meowtils.furiahvisuals.compat.ReflectionHelper;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

public final class GlSave {
   private static final int UNITS = 4;
   private static final IntBuffer INTS = BufferUtils.createIntBuffer(16);
   private static Boolean gl30;
   private int program;
   private int activeUnit;
   private int drawFb;
   private int readFb;
   private int matrixMode;
   private final int[] viewport = new int[4];
   private final int[] textures = new int[4];
   private final boolean[] texture2d = new boolean[2];
   private boolean blend;
   private boolean depth;
   private boolean cull;
   private boolean alpha;
   private boolean lighting;
   private boolean scissor;
   private boolean rescale;
   private boolean fog;
   private boolean depthMask;
   private boolean colorMaterial;
   private int depthFunc;
   private int srcRgb;
   private int dstRgb;
   private int srcAlpha;
   private int dstAlpha;
   private int frontFace;
   private int shadeModel;
   private boolean saved;
   private boolean quick;
   private static boolean programChecked;
   private static boolean programAsk;
   private static final long CHECK_MS = 2000L;
   private static final int FIRST_CHECKS = 30;
   private static int checks;
   private static long nextCheck;
   private static boolean quickOff;
   private GlSave probe;
   private static final FloatBuffer FLOATS = BufferUtils.createFloatBuffer(16);
   private final float[] clearColor = new float[4];

   private static boolean gl30() {
      if (gl30 == null) {
         try {
            gl30 = GLContext.getCapabilities().OpenGL30;
         } catch (RuntimeException | LinkageError var1) {
            gl30 = false;
         }
      }

      return gl30;
   }

   public static int trackedProgram() {
      try {
         return GlSave.Record.ready() ? GlSave.Record.program() : -1;
      } catch (RuntimeException | IllegalAccessException var1) {
         return -1;
      }
   }

   public static boolean texture2dOn(int unit) {
      try {
         return !GlSave.Record.ready() || GlSave.Record.texture2d(unit);
      } catch (RuntimeException | IllegalAccessException var2) {
         return true;
      }
   }

   public static boolean blendOn() {
      try {
         if (GlSave.Record.ready()) {
            return GlSave.Record.on(GlSave.Record.blendOn, GlSave.Record.blendState);
         }
      } catch (RuntimeException | IllegalAccessException var1) {
      }

      return GL11.glIsEnabled(3042);
   }

   public static int boundProgram() {
      int tracked = trackedProgram();
      if (tracked >= 0) {
         return tracked;
      } else if (programChecked && !programAsk) {
         return 0;
      } else {
         int p = GL11.glGetInteger(35725);
         if (!programChecked) {
            programChecked = true;
            programAsk = p != 0;
         }

         return p;
      }
   }

   public GlSave save() {
      this.program = GL11.glGetInteger(35725);
      this.activeUnit = GL11.glGetInteger(34016);
      this.drawFb = GL11.glGetInteger(36006);
      this.readFb = gl30() ? GL11.glGetInteger(36010) : this.drawFb;
      this.matrixMode = GL11.glGetInteger(2976);
      ((Buffer)INTS).clear();
      GL11.glGetInteger(2978, INTS);

      for (int i = 0; i < 4; i++) {
         this.viewport[i] = INTS.get(i);
      }

      for (int var2 = 0; var2 < 4; var2++) {
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + var2);
         this.textures[var2] = GL11.glGetInteger(32873);
         if (var2 < 2) {
            this.texture2d[var2] = GL11.glIsEnabled(3553);
         }
      }

      GlStateManager.setActiveTexture(this.activeUnit);
      this.blend = GL11.glIsEnabled(3042);
      this.depth = GL11.glIsEnabled(2929);
      this.cull = GL11.glIsEnabled(2884);
      this.alpha = GL11.glIsEnabled(3008);
      this.lighting = GL11.glIsEnabled(2896);
      this.scissor = GL11.glIsEnabled(3089);
      this.rescale = GL11.glIsEnabled(32826);
      this.fog = GL11.glIsEnabled(2912);
      this.colorMaterial = GL11.glIsEnabled(2903);
      this.depthMask = GL11.glGetBoolean(2930);
      this.depthFunc = GL11.glGetInteger(2932);
      this.srcRgb = GL11.glGetInteger(32969);
      this.dstRgb = GL11.glGetInteger(32968);
      this.srcAlpha = GL11.glGetInteger(32971);
      this.dstAlpha = GL11.glGetInteger(32970);
      this.frontFace = GL11.glGetInteger(2886);
      this.shadeModel = GL11.glGetInteger(2900);
      ((Buffer)FLOATS).clear();
      GL11.glGetFloat(3106, FLOATS);

      for (int var3 = 0; var3 < 4; var3++) {
         this.clearColor[var3] = FLOATS.get(var3);
      }

      this.quick = false;
      this.saved = true;
      return this;
   }

   public GlSave saveQuick() {
      if (!quickOff && GlSave.Record.ready() && !OptiFineCompat.shadersActive()) {
         long now = System.currentTimeMillis();
         if (checks < 30 || now >= nextCheck) {
            checks++;
            nextCheck = now + 2000L;
            if (this.probe == null) {
               this.probe = new GlSave();
            }

            boolean read = this.probe.readRecord();
            this.save();
            String differs = read ? this.probe.differsFrom(this) : null;
            if (differs != null) {
               if (differs.startsWith("program") || !GlSave.Record.trackerPresent()) {
                  quickOff = true;
                  FuriahsVisuals.LOG.info("Weapons: GL state saves read the driver from now on (GlStateManager's record differs: {})", new Object[]{differs});
               } else if (!GlSave.Record.toldDiffers) {
                  GlSave.Record.toldDiffers = true;
                  FuriahsVisuals.LOG
                     .info("Weapons: GlStateManager's record differs from the driver here ({}); LabyMod tracks the state, kept", new Object[]{differs});
               }
            }

            return this;
         } else {
            return !this.readRecord() ? this.save() : this;
         }
      } else {
         return this.save();
      }
   }

   private boolean readRecord() {
      try {
         int p = GlSave.Record.program();
         this.program = p < 0 ? 0 : p;
         this.activeUnit = OpenGlHelper.defaultTexUnit + GlSave.Record.activeUnit();

         for (int i = 0; i < 4; i++) {
            this.textures[i] = GlSave.Record.texture(i);
            if (i < 2) {
               this.texture2d[i] = GlSave.Record.texture2d(i);
            }
         }

         this.blend = GlSave.Record.on(GlSave.Record.blendOn, GlSave.Record.blendState);
         this.depth = GlSave.Record.on(GlSave.Record.depthOn, GlSave.Record.depthState);
         this.cull = GlSave.Record.on(GlSave.Record.cullOn, GlSave.Record.cullState);
         this.alpha = GlSave.Record.on(GlSave.Record.alphaOn, GlSave.Record.alphaState);
         this.lighting = GlSave.Record.on(null, GlSave.Record.lightingState);
         this.rescale = GlSave.Record.on(null, GlSave.Record.rescaleState);
         this.fog = GlSave.Record.on(GlSave.Record.fogOn, GlSave.Record.fogState);
         this.colorMaterial = GlSave.Record.on(GlSave.Record.colorMaterialOn, GlSave.Record.colorMaterialState);
         Object d = GlSave.Record.depthState.get(null);
         this.depthMask = GlSave.Record.depthMask.getBoolean(d);
         this.depthFunc = GlSave.Record.depthFunc.getInt(d);
         Object b = GlSave.Record.blendState.get(null);
         this.srcRgb = GlSave.Record.blendSrc.getInt(b);
         this.dstRgb = GlSave.Record.blendDst.getInt(b);
         this.srcAlpha = GlSave.Record.blendSrcAlpha.getInt(b);
         this.dstAlpha = GlSave.Record.blendDstAlpha.getInt(b);
         this.shadeModel = GlSave.Record.shadeModel.getInt(null);
      } catch (RuntimeException | ReflectiveOperationException var4) {
         GlSave.Record.broken(var4);
         return false;
      }

      this.quick = true;
      this.saved = true;
      return true;
   }

   private String differsFrom(GlSave full) {
      if (this.program != full.program) {
         return "program " + this.program + " / " + full.program;
      } else if (this.activeUnit != full.activeUnit) {
         return "active texture unit";
      } else {
         for (int i = 0; i < 4; i++) {
            if (this.textures[i] != full.textures[i]) {
               return "texture of unit " + i;
            }
         }

         if (this.texture2d[0] != full.texture2d[0] || this.texture2d[1] != full.texture2d[1]) {
            return "texture switch";
         } else if (this.blend != full.blend
            || this.srcRgb != full.srcRgb
            || this.dstRgb != full.dstRgb
            || this.srcAlpha != full.srcAlpha
            || this.dstAlpha != full.dstAlpha) {
            return "blending";
         } else if (this.depth != full.depth || this.depthMask != full.depthMask || this.depthFunc != full.depthFunc) {
            return "depth";
         } else if (this.cull != full.cull
            || this.alpha != full.alpha
            || this.lighting != full.lighting
            || this.rescale != full.rescale
            || this.fog != full.fog
            || this.colorMaterial != full.colorMaterial) {
            return "switches";
         } else {
            return this.shadeModel != full.shadeModel ? "shade model" : null;
         }
      }
   }

   public boolean fog() {
      return this.fog;
   }

   public void bindTarget() {
      this.bindFramebuffers();
      GlStateManager.viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
   }

   public int[] viewport() {
      return this.viewport;
   }

   public int drawFramebuffer() {
      return this.drawFb;
   }

   public void restore() {
      if (this.saved) {
         this.saved = false;
         if (!this.quick) {
            this.bindFramebuffers();
            GlStateManager.viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
         }

         GL20.glUseProgram(this.program);

         for (int i = 3; i >= 0; i--) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + i);
            GlStateManager.bindTexture(this.textures[i]);
            if (i < 2) {
               if (this.texture2d[i]) {
                  GlStateManager.enableTexture2D();
               } else {
                  GlStateManager.disableTexture2D();
               }
            }
         }

         GlStateManager.setActiveTexture(this.activeUnit);
         set(this.blend, 3042);
         set(this.depth, 2929);
         set(this.cull, 2884);
         set(this.alpha, 3008);
         set(this.lighting, 2896);
         set(this.rescale, 32826);
         set(this.fog, 2912);
         set(this.colorMaterial, 2903);
         if (!this.quick) {
            if (this.scissor) {
               GL11.glEnable(3089);
            } else {
               GL11.glDisable(3089);
            }
         }

         GlStateManager.depthMask(this.depthMask);
         GlStateManager.depthFunc(this.depthFunc);
         GlStateManager.tryBlendFuncSeparate(this.srcRgb, this.dstRgb, this.srcAlpha, this.dstAlpha);
         if (!this.quick) {
            GL11.glFrontFace(this.frontFace);
         }

         GlStateManager.shadeModel(this.shadeModel);
         GlStateManager.colorMask(true, true, true, true);
         if (!this.quick) {
            GlStateManager.clearColor(this.clearColor[0], this.clearColor[1], this.clearColor[2], this.clearColor[3]);
            GlStateManager.matrixMode(this.matrixMode);
         }

         GlStateManager.resetColor();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private static void set(boolean on, int cap) {
      switch (cap) {
         case 2884:
            if (on) {
               GlStateManager.enableCull();
            } else {
               GlStateManager.disableCull();
            }
            break;
         case 2896:
            if (on) {
               GlStateManager.enableLighting();
            } else {
               GlStateManager.disableLighting();
            }
            break;
         case 2903:
            if (on) {
               GlStateManager.enableColorMaterial();
            } else {
               GlStateManager.disableColorMaterial();
            }
            break;
         case 2912:
            if (on) {
               GlStateManager.enableFog();
            } else {
               GlStateManager.disableFog();
            }
            break;
         case 2929:
            if (on) {
               GlStateManager.enableDepth();
            } else {
               GlStateManager.disableDepth();
            }
            break;
         case 3008:
            if (on) {
               GlStateManager.enableAlpha();
            } else {
               GlStateManager.disableAlpha();
            }
            break;
         case 3042:
            if (on) {
               GlStateManager.enableBlend();
            } else {
               GlStateManager.disableBlend();
            }
            break;
         default:
            if (on) {
               GlStateManager.enableRescaleNormal();
            } else {
               GlStateManager.disableRescaleNormal();
            }
      }
   }

   private void bindFramebuffers() {
      if (gl30() && this.readFb != this.drawFb) {
         GL30.glBindFramebuffer(36008, this.readFb);
         GL30.glBindFramebuffer(36009, this.drawFb);
      } else {
         OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.drawFb);
      }
   }

   private static final class Record {
      private static boolean looked;
      private static boolean ready;
      private static boolean toldDiffers;
      private static boolean toldBroken;
      static Field alphaState;
      static Field lightingState;
      static Field colorMaterialState;
      static Field blendState;
      static Field depthState;
      static Field fogState;
      static Field cullState;
      static Field rescaleState;
      static Field activeUnit;
      static Field textureState;
      static Field shadeModel;
      static Field alphaOn;
      static Field colorMaterialOn;
      static Field blendOn;
      static Field depthOn;
      static Field fogOn;
      static Field cullOn;
      static Field blendSrc;
      static Field blendDst;
      static Field blendSrcAlpha;
      static Field blendDstAlpha;
      static Field depthMask;
      static Field depthFunc;
      static Field booleanState;
      static Field textureOn;
      static Field textureName;
      private static Field trackerProgram;

      static boolean ready() {
         if (!looked) {
            looked = true;

            try {
               lookUp();
               ready = true;
            } catch (RuntimeException | ReflectiveOperationException var1) {
               FuriahsVisuals.LOG.info("Weapons: GlStateManager's record can't be read ({}); GL state saves read the driver", new Object[]{var1.toString()});
            }
         }

         return ready;
      }

      private static void lookUp() throws ReflectiveOperationException {
         Class<GlStateManager> gsm = GlStateManager.class;
         alphaState = field(gsm, "alphaState", "alphaState");
         lightingState = field(gsm, "lightingState", "lightingState");
         colorMaterialState = field(gsm, "colorMaterialState", "colorMaterialState");
         blendState = field(gsm, "blendState", "blendState");
         depthState = field(gsm, "depthState", "depthState");
         fogState = field(gsm, "fogState", "fogState");
         cullState = field(gsm, "cullState", "cullState");
         rescaleState = field(gsm, "rescaleNormalState", "rescaleNormalState");
         activeUnit = field(gsm, "activeTextureUnit", "activeTextureUnit");
         textureState = field(gsm, "textureState", "textureState");
         shadeModel = field(gsm, "activeShadeModel", "activeShadeModel");
         Object lighting = lightingState.get(null);
         booleanState = field(lighting.getClass(), "currentState", "currentState");
         alphaOn = field(alphaState.get(null).getClass(), "alphaTest", "alphaTest");
         colorMaterialOn = field(colorMaterialState.get(null).getClass(), "colorMaterial", "colorMaterial");
         Class<?> blend = blendState.get(null).getClass();
         blendOn = field(blend, "blend", "blend");
         blendSrc = field(blend, "srcFactor", "srcFactor");
         blendDst = field(blend, "dstFactor", "dstFactor");
         blendSrcAlpha = field(blend, "srcFactorAlpha", "srcFactorAlpha");
         blendDstAlpha = field(blend, "dstFactorAlpha", "dstFactorAlpha");
         Class<?> depth = depthState.get(null).getClass();
         depthOn = field(depth, "depthTest", "depthTest");
         depthMask = field(depth, "maskEnabled", "maskEnabled");
         depthFunc = field(depth, "depthFunc", "depthFunc");
         fogOn = field(fogState.get(null).getClass(), "fog", "fog");
         cullOn = field(cullState.get(null).getClass(), "cullFace", "cullFace");
         Object[] textures = (Object[])textureState.get(null);
         if (textures.length < 4) {
            throw new IllegalStateException("only " + textures.length + " texture units");
         } else {
            Class<?> texture = textures[0].getClass();
            textureOn = field(texture, "texture2DState", "texture2DState");
            textureName = field(texture, "textureName", "textureName");
            on(blendOn, blendState);
            on(null, lightingState);
            texture(0);
            texture2d(0);
            activeUnit.getInt(null);
            shadeModel.getInt(null);

            try {
               Class<?> tracker = Class.forName("net.labymod.core.laby3d.opengl.GlStateTracker", false, GlSave.class.getClassLoader());
               Field f = tracker.getDeclaredField("currentProgram");
               f.setAccessible(true);
               f.getInt(null);
               trackerProgram = f;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError var8) {
               trackerProgram = null;
            }
         }
      }

      private static Field field(Class<?> owner, String... names) {
         return ReflectionHelper.findField(owner, names);
      }

      static boolean trackerPresent() {
         return trackerProgram != null;
      }

      static int program() throws IllegalAccessException {
         return trackerProgram == null ? -1 : trackerProgram.getInt(null);
      }

      static boolean on(Field outer, Field state) throws IllegalAccessException {
         Object s = state.get(null);
         if (outer != null) {
            s = outer.get(s);
         }

         return booleanState.getBoolean(s);
      }

      static int activeUnit() throws IllegalAccessException {
         return activeUnit.getInt(null);
      }

      static int texture(int unit) throws IllegalAccessException {
         return textureName.getInt(((Object[])textureState.get(null))[unit]);
      }

      static boolean texture2d(int unit) throws IllegalAccessException {
         return booleanState.getBoolean(textureOn.get(((Object[])textureState.get(null))[unit]));
      }

      static void broken(Exception e) {
         ready = false;
         if (!toldBroken) {
            toldBroken = true;
            FuriahsVisuals.LOG.info("Weapons: GlStateManager's record can't be read ({}); GL state saves read the driver", new Object[]{e.toString()});
         }
      }
   }
}
