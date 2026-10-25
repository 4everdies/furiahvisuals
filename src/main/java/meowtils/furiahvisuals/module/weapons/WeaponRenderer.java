package meowtils.furiahvisuals.module.weapons;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.render.GlSave;
import meowtils.furiahvisuals.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class WeaponRenderer {
   private final WeaponShader shaders;
   private WeaponShader.Program program;
   private int white = -1;
   private final float[] rgb = new float[3];
   private WeaponLight light;
   private boolean fog;
   private float time;
   private boolean baseBlend = true;
   WeaponsModule module;
   private boolean clearStrings;
   private final Map<String, float[]> bowPoses = new HashMap<>();
   private final Map<String, float[]> bowPoseArrays = new HashMap<>();
   private final float[] tipPoint = new float[3];
   private static final FloatBuffer STRETCH = BufferUtils.createFloatBuffer(16);
   private static final ResourceLocation GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");
   private static final float[] EYE_REST = new float[]{0.0F, 0.0F, 0.0F, 1.0F};
   private final Map<String, float[]> eyelessPoses = new HashMap<>();

   private boolean flatShading() {
      return this.module != null && this.module.pixelShading.get() == WeaponOptions.PixelShading.FLAT;
   }

   private boolean pixelHalos() {
      return this.module == null || this.module.glowStyle.get() == WeaponOptions.GlowStyle.PIXEL_HALO;
   }

   private static boolean calm() {
      return FuriahsVisuals.instance != null && FuriahsVisuals.instance.isReduceFlashing();
   }

   WeaponRenderer(WeaponShader shaders) {
      this.shaders = shaders;
   }

   WeaponShader shaders() {
      return this.shaders;
   }

   boolean beginShaded(WeaponLight light, boolean fog, float time) {
      WeaponShader.Program p = this.shaders.weapon();
      if (p == null) {
         return false;
      } else {
         p.use();
         light.apply(p);
         p.set1f("uFog", fog ? 1.0F : 0.0F);
         p.set1f("uTime", time);
         p.set1f("uAlpha", 1.0F);
         this.program = p;
         this.light = light;
         this.fog = fog;
         this.time = time;
         return true;
      }
   }

   WeaponShader.Program program() {
      return this.program;
   }

   void setTime(float time) {
      this.time = time;
   }

   void endShaded() {
      this.program = null;
      this.baseBlend = true;
   }

   void setBaseBlend(boolean on) {
      this.baseBlend = on;
   }

   void setClearStrings(boolean on) {
      this.clearStrings = on;
   }

   void draw(WeaponAssets a, int lod, WeaponRenderer.Look look, WeaponRenderer.Mode mode, boolean mirrored) {
      if (a.def.bow != null) {
         this.drawBow(a, lod, look, mode, mirrored);
      } else {
         this.drawModel(a, lod, look, mode, mirrored);
      }
   }

   private void drawModel(WeaponAssets a, int lod, WeaponRenderer.Look look, WeaponRenderer.Mode mode, boolean mirrored) {
      if (!look.pixel || !this.drawPixel(a, lod, look, mode, mirrored)) {
         WeaponMesh mesh = a.mesh(lod);
         if (mesh != null) {
            if (mirrored) {
               GL11.glFrontFace(2304);
            }

            try {
               switch (mode) {
                  case SHADED:
                     this.drawShaded(a, mesh, look);
                     return;
                  case MASK:
                     this.drawMask(a, mesh, look);
                     return;
                  default:
                     this.drawSimple(a, mesh, look);
               }
            } finally {
               if (mirrored) {
                  GL11.glFrontFace(2305);
               }
            }
         }
      }
   }

   private void drawBow(WeaponAssets a, int lod, WeaponRenderer.Look look, WeaponRenderer.Mode mode, boolean mirrored) {
      WeaponDef def = a.def;
      Map<String, float[]> own = look.poses;
      boolean pixel = look.pixel && a.pixelTwin().pixelMesh(0) != null;
      boolean rigid = pixel || mode != WeaponRenderer.Mode.SHADED || this.program == null;
      look.poses = this.bowPoses(def, look, own, rigid);

      try {
         this.drawModel(a, lod, look, mode, mirrored);
         this.drawString(def, look, pixel, rigid, pixel ? a.pixelTwin().pixelMesh(0).pixel : 0.0F, mode == WeaponRenderer.Mode.MASK);
      } finally {
         look.poses = own;
      }
   }

   private float[] poseArray(String part, float angle, float slide, float shown) {
      float[] p = this.bowPoseArrays.get(part);
      if (p == null) {
         p = new float[4];
         this.bowPoseArrays.put(part, p);
      }

      p[0] = angle;
      p[1] = slide;
      p[2] = 1.0F;
      p[3] = shown;
      return p;
   }

   private Map<String, float[]> bowPoses(WeaponDef def, WeaponRenderer.Look look, Map<String, float[]> own, boolean rigid) {
      WeaponDef.Bow b = def.bow;
      this.bowPoses.clear();
      if (own != null) {
         this.bowPoses.putAll(own);
      }

      if (b.arrow != null) {
         for (String string : def.pivots.keySet()) {
            if (ridesOn(def, string, b.arrow)) {
               float slide = string.equals(b.arrow) ? b.draw * Math.max(0.0F, look.bowPull) : 0.0F;
               this.bowPoses.put(string, this.poseArray(string, 0.0F, slide, look.bowNocked ? 1.0F : 0.0F));
            }
         }

         if (!def.pivots.containsKey(b.arrow)) {
            this.bowPoses.put(b.arrow, this.poseArray(b.arrow, 0.0F, 0.0F, look.bowNocked ? 1.0F : 0.0F));
         }
      }

      for (Entry entry : b.fan.entrySet()) {
         float[] mine = own != null ? own.get(entry.getKey()) : null;
         float angle = (Float)entry.getValue() * look.bowBend + (mine != null ? mine[0] : 0.0F);
         if (angle != 0.0F || mine != null) {
            this.bowPoses
               .put((String)entry.getKey(), this.poseArray((String)entry.getKey(), angle, mine != null ? mine[1] : 0.0F, mine != null ? mine[3] : 1.0F));
         }
      }

      float k = b.bend * look.bowBend;
      if (rigid && k != 0.0F) {
         for (String[] limb : b.limbs) {
            double before = 0.0;

            for (int i = 0; i < limb.length; i++) {
               WeaponDef.Pivot pv = def.pivots.get(limb[i]);
               if (pv != null) {
                  WeaponDef.Pivot next = i + 1 < limb.length ? def.pivots.get(limb[i + 1]) : null;
                  float d0 = Math.max(0.0F, Math.abs(pv.origin[1]) - b.from);
                  float d1 = Math.max(0.0F, (next != null ? Math.abs(next.origin[1]) : Math.abs(b.tips[1])) - b.from);
                  double turn = Math.atan(k * (d0 + d1));
                  float[] mine = own != null ? own.get(limb[i]) : null;
                  float[] p = this.poseArray(
                     limb[i],
                     (float)Math.toDegrees(turn - before) + (mine != null ? mine[0] : 0.0F),
                     mine != null ? mine[1] : 0.0F,
                     mine != null ? mine[3] : 1.0F
                  );
                  this.bowPoses.put(limb[i], p);
                  before = turn;
               }
            }
         }
      }

      return this.bowPoses;
   }

   private static void followBend(WeaponDef def, String part, float k) {
      WeaponDef.Pivot pv = def.pivots.get(part);
      if (pv != null) {
         WeaponDef.Bow b = def.bow;
         float y = pv.origin[1];
         float s = y < 0.0F ? -1.0F : 1.0F;
         float d = Math.max(0.0F, Math.abs(y) - b.from);
         float dx = -k * d * d;
         float dy = -s * 0.6667F * k * k * d * d * d;
         float turn = s * (float)Math.toDegrees(Math.atan(2.0F * k * d));
         GlStateManager.translate(pv.origin[0] + dx, pv.origin[1] + dy, pv.origin[2]);
         GlStateManager.rotate(turn, 0.0F, 0.0F, 1.0F);
         GlStateManager.translate(-pv.origin[0], -pv.origin[1], -pv.origin[2]);
      }
   }

   private static boolean ridesOn(WeaponDef def, String part, String root) {
      String name = part;

      for (int depth = 0; name != null && depth < 8; depth++) {
         if (name.equals(root)) {
            return true;
         }

         WeaponDef.Pivot pv = def.pivots.get(name);
         name = pv != null ? pv.parent : null;
      }

      return false;
   }

   private float[] stringTip(WeaponDef def, WeaponRenderer.Look look, int i, boolean rigid, float[] out) {
      WeaponDef.Bow b = def.bow;
      out[0] = b.tips[i * 3];
      out[1] = b.tips[i * 3 + 1];
      out[2] = b.tips[i * 3 + 2];
      float k = b.bend * look.bowBend;
      if (k == 0.0F) {
         return out;
      } else {
         String[] limb = b.limbs[i];
         if (rigid && limb.length > 0) {
            posedPoint(def, look.poses, limb[limb.length - 1], out);
            return out;
         } else {
            float s = out[1] < 0.0F ? -1.0F : 1.0F;
            float d = Math.max(0.0F, Math.abs(out[1]) - b.from);
            out[0] -= k * d * d;
            out[1] -= s * 0.6667F * k * k * d * d * d;
            return out;
         }
      }
   }

   private void drawString(WeaponDef def, WeaponRenderer.Look look, boolean pixel, boolean rigid, float pixelSize, boolean mask) {
      WeaponDef.Bow b = def.bow;
      float[] up = this.stringTip(def, look, 0, rigid, new float[3]);
      float[] down = this.stringTip(def, look, 1, rigid, this.tipPoint);
      float[] nock = new float[]{b.nock[0] - b.draw * look.bowPull, b.nock[1], look.bowNocked ? b.nock[2] : 0.0F};
      Tessellator t = Tessellator.getInstance();
      WorldRenderer wr = t.getWorldRenderer();
      if (mask) {
         bind(0, this.whiteTexture());
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         wr.begin(7, DefaultVertexFormats.POSITION_TEX);
         stringShape(wr, true, up, down, nock, pixel, pixelSize, 1.0F, 1.0F, 1.0F, 1.0F);
         t.draw();
      } else {
         float[] lit = this.light != null && !b.stringGlow ? this.light.light : null;
         float lr = lit != null ? lit[0] : 1.0F;
         float lg = lit != null ? lit[1] : 1.0F;
         float lb = lit != null ? lit[2] : 1.0F;
         float glow = b.stringGlow ? b.glow(look.bowPull, look.flare) : 1.0F;
         boolean clear = this.clearStrings && b.stringAlpha < 0.999F;
         float alpha = look.alpha * b.stringAlpha;
         float pre = clear ? alpha : 1.0F;
         int c = b.stringColor;
         float r = (c >> 16 & 0xFF) / 255.0F * lr * glow * pre;
         float g = (c >> 8 & 0xFF) / 255.0F * lg * glow * pre;
         float bl = (c & 0xFF) / 255.0F * lb * glow * pre;
         GL20.glUseProgram(0);
         boolean lightmapOn = GlSave.texture2dOn(1);
         boolean blendWas = GlSave.blendOn();
         GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
         GlStateManager.disableTexture2D();
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         GlStateManager.disableTexture2D();
         GlStateManager.disableLighting();
         if (clear) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
         } else {
            GlStateManager.disableBlend();
         }

         wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
         stringShape(wr, false, up, down, nock, pixel, pixelSize, r, g, bl, clear ? alpha : look.alpha);
         t.draw();
         GlStateManager.enableTexture2D();
         if (lightmapOn) {
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         }

         if (blendWas) {
            GlStateManager.enableBlend();
         } else if (clear) {
            GlStateManager.disableBlend();
         }

         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         if (this.program != null) {
            this.program.use();
         }
      }
   }

   private static void stringShape(
      WorldRenderer wr, boolean mask, float[] up, float[] down, float[] nock, boolean pixel, float pixelSize, float r, float g, float bl, float alpha
   ) {
      if (pixel && pixelSize > 0.0F) {
         pixelString(wr, mask, up, nock, pixelSize, r, g, bl, alpha);
         pixelString(wr, mask, down, nock, pixelSize, r, g, bl, alpha);
      } else {
         cord(wr, mask, up, nock, 0.0021F, r, g, bl, alpha);
         cord(wr, mask, nock, down, 0.0021F, r, g, bl, alpha);
      }
   }

   private static void vertex(WorldRenderer wr, boolean mask, float x, float y, float z, float r, float g, float b, float a) {
      if (mask) {
         wr.pos(x, y, z).tex(0.5, 0.5).endVertex();
      } else {
         wr.pos(x, y, z).color(r, g, b, a).endVertex();
      }
   }

   private static void cord(WorldRenderer wr, boolean mask, float[] a, float[] b, float radius, float r, float g, float bl, float alpha) {
      float dx = b[0] - a[0];
      float dy = b[1] - a[1];
      float dz = b[2] - a[2];
      float len = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (!(len < 1.0E-5F)) {
         float ux = 0.0F;
         float uy = 0.0F;
         float uz = 1.0F;
         float var34;
         float var35;
         float vx = (var34 = dy / len) * uz - (var35 = dz / len) * uy;
         float var33;
         float vy = var35 * ux - (var33 = dx / len) * uz;
         float vz = var33 * uy - var34 * ux;
         float vl = (float)Math.sqrt(vx * vx + vy * vy + vz * vz);
         float var40;
         float var41;
         ux = (var40 = vy / vl) * var35 - (var41 = vz / vl) * var34;
         float var39;
         uy = var41 * var33 - (var39 = vx / vl) * var35;
         uz = var39 * var34 - var40 * var33;
         float[] shade = new float[]{1.0F, 0.78F, 0.6F, 0.78F};
         float[][] side = new float[][]{{ux, uy, uz}, {var39, var40, var41}, {-ux, -uy, -uz}, {-var39, -var40, -var41}};

         for (int k = 0; k < 4; k++) {
            float[] s0 = side[k];
            float[] s1 = side[(k + 1) % 4];
            float k0x = (s0[0] + s1[0]) * radius;
            float k0y = (s0[1] + s1[1]) * radius;
            float k0z = (s0[2] + s1[2]) * radius;
            float[] s2 = side[(k + 3) % 4];
            float k1x = (s2[0] + s0[0]) * radius;
            float k1y = (s2[1] + s0[1]) * radius;
            float k1z = (s2[2] + s0[2]) * radius;
            float f = shade[k];
            vertex(wr, mask, a[0] + k1x, a[1] + k1y, a[2] + k1z, r * f, g * f, bl * f, alpha);
            vertex(wr, mask, a[0] + k0x, a[1] + k0y, a[2] + k0z, r * f, g * f, bl * f, alpha);
            vertex(wr, mask, b[0] + k0x, b[1] + k0y, b[2] + k0z, r * f, g * f, bl * f, alpha);
            vertex(wr, mask, b[0] + k1x, b[1] + k1y, b[2] + k1z, r * f, g * f, bl * f, alpha);
         }
      }
   }

   private static void pixelString(WorldRenderer wr, boolean mask, float[] a, float[] b, float px, float r, float g, float bl, float alpha) {
      int steps = Math.max(1, Math.round(Math.abs(b[1] - a[1]) / px));
      float h = px / 2.0F;

      for (int i = 0; i <= steps; i++) {
         float t = (float)i / steps;
         float x = Math.round((a[0] + (b[0] - a[0]) * t) / px) * px;
         float y = Math.round((a[1] + (b[1] - a[1]) * t) / px) * px;
         float z = Math.round((a[2] + (b[2] - a[2]) * t) / px) * px;
         box(wr, mask, x - h, y - h, z - h, x + h, y + h, z + h, r, g, bl, alpha);
      }
   }

   private static void box(WorldRenderer wr, boolean mask, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a) {
      float s = 0.8F;
      float t = 0.9F;
      vertex(wr, mask, x0, y0, z1, r, g, b, a);
      vertex(wr, mask, x1, y0, z1, r, g, b, a);
      vertex(wr, mask, x1, y1, z1, r, g, b, a);
      vertex(wr, mask, x0, y1, z1, r, g, b, a);
      vertex(wr, mask, x0, y0, z0, r, g, b, a);
      vertex(wr, mask, x0, y1, z0, r, g, b, a);
      vertex(wr, mask, x1, y1, z0, r, g, b, a);
      vertex(wr, mask, x1, y0, z0, r, g, b, a);
      vertex(wr, mask, x1, y0, z0, r * s, g * s, b * s, a);
      vertex(wr, mask, x1, y1, z0, r * s, g * s, b * s, a);
      vertex(wr, mask, x1, y1, z1, r * s, g * s, b * s, a);
      vertex(wr, mask, x1, y0, z1, r * s, g * s, b * s, a);
      vertex(wr, mask, x0, y0, z0, r * s, g * s, b * s, a);
      vertex(wr, mask, x0, y0, z1, r * s, g * s, b * s, a);
      vertex(wr, mask, x0, y1, z1, r * s, g * s, b * s, a);
      vertex(wr, mask, x0, y1, z0, r * s, g * s, b * s, a);
      vertex(wr, mask, x0, y1, z0, r * t, g * t, b * t, a);
      vertex(wr, mask, x0, y1, z1, r * t, g * t, b * t, a);
      vertex(wr, mask, x1, y1, z1, r * t, g * t, b * t, a);
      vertex(wr, mask, x1, y1, z0, r * t, g * t, b * t, a);
      vertex(wr, mask, x0, y0, z0, r * t, g * t, b * t, a);
      vertex(wr, mask, x1, y0, z0, r * t, g * t, b * t, a);
      vertex(wr, mask, x1, y0, z1, r * t, g * t, b * t, a);
      vertex(wr, mask, x0, y0, z1, r * t, g * t, b * t, a);
   }

   private static void setCore(WeaponShader.Program p, WeaponDef def) {
      float[] c = def.core;
      if (c == null) {
         p.set4f("uCore", 0.0F, 0.0F, 0.0F, 0.0F);
      } else {
         p.set4f("uCore", c[0], c[1], c[2], c[3]);
      }
   }

   private void drawShaded(WeaponAssets a, WeaponMesh mesh, WeaponRenderer.Look look) {
      WeaponShader.Program p = this.program;
      if (p != null) {
         WeaponSettings s = look.settings;
         WeaponDef def = a.def;
         bind(1, a.material >= 0 ? a.material : this.whiteTexture());
         bind(2, a.normal >= 0 ? a.normal : this.whiteTexture());
         bind(0, a.albedo >= 0 ? a.albedo : this.whiteTexture());
         p.set1f("uHasMaps", a.albedo >= 0 && a.material >= 0 ? 1.0F : 0.0F);
         p.set1f("uHasNormalMap", a.normal >= 0 ? 1.0F : 0.0F);
         p.set1f("uAlpha", look.alpha);
         p.set1f("uWear", s != null && s.wear != null ? s.wear.getFloat() / 100.0F : 0.0F);
         p.set1f("uGlow", s != null && s.glow != null ? s.glow.getFloat() : 1.0F);
         p.set1f("uSeed", look.seed);
         p.set1f("uHeat", look.heat < 0.0F ? def.heatIdle : look.heat);
         p.set1f("uHeatIdle", def.heatIdle);
         setCore(p, def);
         p.set1f("uSheen", look.enchant);
         p.set3f("uSheenColor", 0.55F, 0.32F, 1.0F);
         p.set1f("uGlint", look.glint);
         p.set1f("uReveal", look.reveal);
         p.set1f("uFlare", look.flare);
         WeaponOptions.Finish finish = look.finish(def);
         p.set1f("uBendFrom", def.bendFrom);
         WeaponDef.Bow bow = def.bow;
         float bowK = bow != null ? bow.bend * look.bowBend : 0.0F;
         if (bowK != 0.0F) {
            p.set1f("uBowFrom", bow.from);
         }

         mesh.begin();
         boolean chainOn = false;
         boolean bendOn = false;
         boolean bowOn = false;

         try {
            for (WeaponMesh.Part part : mesh.parts) {
               float[] pose = look.poses != null ? look.poses.get(part.name) : null;
               if ((pose == null || !(pose[3] < 0.5F)) && !part.name.endsWith("#static")) {
                  boolean fans = bowK != 0.0F && bow.fan.containsKey(part.name);
                  boolean bowBent = bowK != 0.0F && !fans && (bow.arrow == null || !ridesOn(def, part.name, bow.arrow));
                  if (bowBent != bowOn) {
                     p.set1f("uBowBend", bowBent ? bowK : 0.0F);
                     bowOn = bowBent;
                  }

                  this.material(p, def, s, part, finish);
                  boolean bend = look.bend != 0.0F && def.roleOf(part.name) == WeaponDef.Role.BLADE;
                  if (bend || bendOn) {
                     p.set1f("uBend", bend ? look.bend : 0.0F);
                     bendOn = bend;
                  }

                  boolean chain = def.roleOf(part.name) == WeaponDef.Role.CHAIN && def.chainPath != null;
                  if (chain) {
                     float[] c = def.chainPath;
                     p.set4f("uChainPath", c[0], c[1], c[2], c[3]);
                     p.set1f("uChainPhase", look.chainPhase);
                  }

                  if (chain != chainOn) {
                     p.set1f("uChain", chain ? 1.0F : 0.0F);
                     chainOn = chain;
                  }

                  if (fans) {
                     GlStateManager.pushMatrix();
                     followBend(def, part.name, bowK);
                     this.drawPosed(def, mesh, part, look.poses);
                     GlStateManager.popMatrix();
                  } else {
                     this.drawPosed(def, mesh, part, look.poses);
                  }
               }
            }
         } finally {
            if (chainOn) {
               p.set1f("uChain", 0.0F);
            }

            if (bendOn) {
               p.set1f("uBend", 0.0F);
            }

            if (bowOn) {
               p.set1f("uBowBend", 0.0F);
            }

            WeaponMesh.end();
         }
      }
   }

   private void material(WeaponShader.Program p, WeaponDef def, WeaponSettings s, WeaponMesh.Part part, WeaponOptions.Finish finish) {
      p.set4f("uRect", part.uvRect[0], part.uvRect[1], part.uvRect[2], part.uvRect[3]);
      switch (def.roleOf(part.name)) {
         case BLADE:
            p.set1f("uMat", finish.code);
            if (finish == WeaponOptions.Finish.PAINTED && s != null && s.paint != null) {
               this.color(s.paint, 0.0F);
               p.set3f("uColA", this.rgb[0], this.rgb[1], this.rgb[2]);
               this.color(s.paint, 1.0F);
               p.set3f("uColB", this.rgb[0], this.rgb[1], this.rgb[2]);
            } else {
               this.accentColor(s, def);
               p.set3f("uColA", this.rgb[0], this.rgb[1], this.rgb[2]);
               p.set3f("uColB", this.rgb[0], this.rgb[1], this.rgb[2]);
            }
            break;
         case METAL:
            p.set1f("uMat", 20.0F);
            p.setColor("uColA", def.metalColor);
            p.set1f("uRough", def.metalRoughness);
            break;
         case HANDLE:
            p.set1f("uMat", 21.0F);
            if (s != null && s.handleColor != null) {
               this.color(s.handleColor, 0.5F);
               p.set3f("uColA", this.rgb[0], this.rgb[1], this.rgb[2]);
            } else {
               p.setColor("uColA", def.handleColor);
            }

            WeaponOptions.HandleMaterial own = WeaponOptions.HandleMaterial.of(def.handleMaterial);
            WeaponOptions.HandleMaterial chosen = s != null && s.handleMaterial != null ? s.handleMaterial.get() : own;
            p.set1f("uHandle", chosen == own ? 0.0F : chosen.code);
            p.set1f("uRough", chosen == WeaponOptions.HandleMaterial.RUBBER ? 0.8F : 0.6F);
            break;
         case ACCENT:
            p.set1f("uMat", 22.0F);
            this.accentColor(s, def);
            p.set3f("uColA", this.rgb[0], this.rgb[1], this.rgb[2]);
            break;
         case ENERGY:
            p.set1f("uMat", 23.0F);
            this.accentColor(s, def);
            p.set3f("uColA", this.rgb[0], this.rgb[1], this.rgb[2]);
            break;
         case PAINT:
            p.set1f("uMat", 24.0F);
            this.accentColor(s, def);
            p.set3f("uColA", this.rgb[0], this.rgb[1], this.rgb[2]);
            break;
         case CHAIN:
            p.set1f("uMat", 25.0F);
            p.set1f("uRough", 0.38F);
            break;
         default:
            p.set1f("uMat", 0.0F);
      }
   }

   private void accentColor(WeaponSettings s, WeaponDef def) {
      if (s != null && s.accent != null) {
         this.color(s.accent, 0.5F);
      } else {
         int c = def.accentColor;
         this.rgb[0] = (c >> 16 & 0xFF) / 255.0F;
         this.rgb[1] = (c >> 8 & 0xFF) / 255.0F;
         this.rgb[2] = (c & 0xFF) / 255.0F;
      }
   }

   private void color(ColorSetting c, float position) {
      int v = c.getColor(position);
      this.rgb[0] = (v >> 16 & 0xFF) / 255.0F;
      this.rgb[1] = (v >> 8 & 0xFF) / 255.0F;
      this.rgb[2] = (v & 0xFF) / 255.0F;
   }

   private void drawPosed(WeaponDef def, WeaponMesh mesh, WeaponMesh.Part part, Map<String, float[]> poses) {
      if (!moves(def, part.name, poses, 0)) {
         mesh.draw(part);
      } else {
         GlStateManager.pushMatrix();
         this.applyPose(def, part.name, poses, 0, null, null);
         mesh.draw(part);
         GlStateManager.popMatrix();
      }
   }

   private static boolean moves(WeaponDef def, String name, Map<String, float[]> poses, int depth) {
      if ((poses == null || !poses.containsKey(name)) && !def.idle.containsKey(name)) {
         WeaponDef.Pivot pv = def.pivots.get(name);
         return pv != null && pv.parent != null && depth < 8 && moves(def, pv.parent, poses, depth + 1);
      } else {
         return true;
      }
   }

   private void idleMotion(WeaponDef def, String name, float[] idle) {
      WeaponDef.Pivot pv = def.pivots.get(name);
      if (pv != null) {
         double t = this.time;
         float l = (float)Math.sqrt(pv.axis[0] * pv.axis[0] + pv.axis[1] * pv.axis[1] + pv.axis[2] * pv.axis[2]);
         if (!(l < 1.0E-6F)) {
            float ax = pv.axis[0] / l;
            float ay = pv.axis[1] / l;
            float az = pv.axis[2] / l;
            float bob = idle[2] > 0.0F ? idle[1] * (float)Math.sin((Math.PI * 2) * t / idle[2]) : 0.0F;
            GlStateManager.translate(pv.origin[0] + ax * bob, pv.origin[1] + ay * bob, pv.origin[2] + az * bob);
            GlStateManager.rotate((float)(t * idle[0] % 360.0), ax, ay, az);
            GlStateManager.translate(-pv.origin[0], -pv.origin[1], -pv.origin[2]);
         }
      }
   }

   static void growForPoses(WeaponDef def, WeaponMesh mesh, Map<String, float[]> poses, float[] bounds) {
      if (mesh != null && (poses != null || !def.idle.isEmpty())) {
         for (WeaponMesh.Part part : mesh.parts) {
            WeaponDef.Pivot pv = def.pivots.get(part.name);
            if (pv != null && moves(def, part.name, poses, 0)) {
               float[] pose = poses != null ? poses.get(part.name) : null;
               float reach = 0.0F;

               for (int k = 0; k < 8; k++) {
                  float x = (k & 1) == 0 ? part.min[0] : part.max[0];
                  float y = (k & 2) == 0 ? part.min[1] : part.max[1];
                  float z = (k & 4) == 0 ? part.min[2] : part.max[2];
                  float dx = x - pv.origin[0];
                  float dy = y - pv.origin[1];
                  float dz = z - pv.origin[2];
                  reach = Math.max(reach, (float)Math.sqrt(dx * dx + dy * dy + dz * dz));
               }

               if (pose != null) {
                  reach = reach * Math.max(1.0F, pose[2]) + Math.abs(pose[1]);
               }

               float[] center = pv.origin;
               String parent = pv.parent;

               WeaponDef.Pivot pp;
               for (int depth = 0; parent != null && depth < 8 && (pp = def.pivots.get(parent)) != null; depth++) {
                  float dx = center[0] - pp.origin[0];
                  float dy = center[1] - pp.origin[1];
                  float dz = center[2] - pp.origin[2];
                  reach += (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
                  center = pp.origin;
                  parent = pp.parent;
               }

               for (int i = 0; i < 3; i++) {
                  bounds[i] = Math.min(bounds[i], center[i] - reach);
                  bounds[3 + i] = Math.max(bounds[3 + i], center[i] + reach);
               }
            }
         }
      }
   }

   static void growForFan(WeaponDef def, WeaponMesh mesh, float[] bounds) {
      if (mesh != null && def.bow != null && !def.bow.fan.isEmpty()) {
         for (WeaponMesh.Part part : mesh.parts) {
            growForFan(def, part.name, part.min, part.max, bounds);
         }
      }
   }

   static void growForFan(WeaponDef def, PixelMesh mesh, float[] bounds) {
      if (mesh != null && def.bow != null && !def.bow.fan.isEmpty()) {
         for (PixelMesh.Part part : mesh.parts) {
            growForFan(def, part.name, part.min, part.max, bounds);
         }
      }
   }

   private static void growForFan(WeaponDef def, String name, float[] min, float[] max, float[] bounds) {
      WeaponDef.Pivot pv = def.bow.fan.containsKey(name) ? def.pivots.get(name) : null;
      if (pv != null) {
         float reach = 0.0F;

         for (int k = 0; k < 8; k++) {
            float x = (k & 1) == 0 ? min[0] : max[0];
            float y = (k & 2) == 0 ? min[1] : max[1];
            float z = (k & 4) == 0 ? min[2] : max[2];
            float dx = x - pv.origin[0];
            float dy = y - pv.origin[1];
            float dz = z - pv.origin[2];
            reach = Math.max(reach, (float)Math.sqrt(dx * dx + dy * dy + dz * dz));
         }

         for (int i = 0; i < 3; i++) {
            bounds[i] = Math.min(bounds[i], pv.origin[i] - reach);
            bounds[3 + i] = Math.max(bounds[3 + i], pv.origin[i] + reach);
         }
      }
   }

   static void posedPoint(WeaponDef def, Map<String, float[]> poses, String part, float[] p) {
      String name = part;

      for (int depth = 0; name != null && depth < 8; depth++) {
         WeaponDef.Pivot pv = def.pivots.get(name);
         float[] pose = poses != null ? poses.get(name) : null;
         if (pv != null && pose != null) {
            float l = (float)Math.sqrt(pv.axis[0] * pv.axis[0] + pv.axis[1] * pv.axis[1] + pv.axis[2] * pv.axis[2]);
            float ax = l > 1.0E-6F ? pv.axis[0] / l : 0.0F;
            float ay = l > 1.0E-6F ? pv.axis[1] / l : 1.0F;
            float az = l > 1.0E-6F ? pv.axis[2] / l : 0.0F;
            float x = p[0] - pv.origin[0];
            float y = p[1] - pv.origin[1];
            float z = p[2] - pv.origin[2];
            float along = (x * ax + y * ay + z * az) * (Math.max(0.001F, pose[2]) - 1.0F);
            x += ax * along;
            y += ay * along;
            z += az * along;
            if (pose[0] != 0.0F) {
               double a = Math.toRadians(pose[0]);
               float c = (float)Math.cos(a);
               float s = (float)Math.sin(a);
               float d = (x * ax + y * ay + z * az) * (1.0F - c);
               float rx = x * c + (ay * z - az * y) * s + ax * d;
               float ry = y * c + (az * x - ax * z) * s + ay * d;
               float rz = z * c + (ax * y - ay * x) * s + az * d;
               x = rx;
               y = ry;
               z = rz;
            }

            p[0] = x + pv.origin[0] + ax * pose[1];
            p[1] = y + pv.origin[1] + ay * pose[1];
            p[2] = z + pv.origin[2] + az * pose[1];
         }

         name = pv != null ? pv.parent : null;
      }
   }

   private void applyPose(WeaponDef def, String name, Map<String, float[]> poses, int depth, Map<String, Float> stretch, String rigid) {
      WeaponDef.Pivot pv = def.pivots.get(name);
      if (pv != null && pv.parent != null && depth < 8) {
         this.applyPose(def, pv.parent, poses, depth + 1, stretch, rigid);
      }

      float[] idle;
      if ((idle = def.idle.get(name)) != null) {
         this.idleMotion(def, name, idle);
      }

      float[] pose = poses != null ? poses.get(name) : null;
      if (pose != null) {
         Float f = stretch != null ? stretch.get(name) : null;
         posePart(def, name, pose, name.equals(rigid) ? 0.0F : (f != null ? f : 1.0F));
      }
   }

   private static void posePart(WeaponDef def, String name, float[] pose, float stretchFactor) {
      WeaponDef.Pivot pv = def.pivots.get(name);
      float ox = 0.0F;
      float oy = 0.0F;
      float oz = 0.0F;
      float ax = 0.0F;
      float ay = 1.0F;
      float az = 0.0F;
      if (pv != null) {
         ox = pv.origin[0];
         oy = pv.origin[1];
         oz = pv.origin[2];
         float l = (float)Math.sqrt(pv.axis[0] * pv.axis[0] + pv.axis[1] * pv.axis[1] + pv.axis[2] * pv.axis[2]);
         if (l > 1.0E-6F) {
            ax = pv.axis[0] / l;
            ay = pv.axis[1] / l;
            az = pv.axis[2] / l;
         }
      }

      GlStateManager.translate(ox + ax * pose[1], oy + ay * pose[1], oz + az * pose[1]);
      if (pose[0] != 0.0F) {
         GlStateManager.rotate(pose[0], ax, ay, az);
      }

      if (pose[2] != 1.0F) {
         float k = (Math.max(0.001F, pose[2]) - 1.0F) * stretchFactor;
         ((Buffer)STRETCH).clear();
         STRETCH.put(
            new float[]{
               1.0F + k * ax * ax,
               k * ax * ay,
               k * ax * az,
               0.0F,
               k * ay * ax,
               1.0F + k * ay * ay,
               k * ay * az,
               0.0F,
               k * az * ax,
               k * az * ay,
               1.0F + k * az * az,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               1.0F
            }
         );
         ((Buffer)STRETCH).flip();
         GlStateManager.multMatrix(STRETCH);
      }

      GlStateManager.translate(-ox, -oy, -oz);
   }

   private boolean drawPixel(WeaponAssets a, int lod, WeaponRenderer.Look look, WeaponRenderer.Mode mode, boolean mirrored) {
      WeaponAssets px = a.pixelTwin();
      px.request(0, a.tier == WeaponAssets.Tier.FULL);
      PixelMesh pm = px.pixelMesh(a.tier == WeaponAssets.Tier.CARD ? 0 : lod);
      if (pm != null && px.pixelModel != null) {
         if (mirrored) {
            GL11.glFrontFace(2304);
         }

         try {
            switch (mode) {
               case SHADED:
                  this.drawPixelShaded(px, pm, look);
                  return true;
               case MASK:
                  this.drawPixelMask(px, pm, look);
                  return true;
               default:
                  this.drawPixelBaked(px, look);
                  return true;
            }
         } finally {
            if (mirrored) {
               GL11.glFrontFace(2305);
            }
         }
      } else {
         return false;
      }
   }

   private void drawPixelShaded(WeaponAssets px, PixelMesh pm, WeaponRenderer.Look look) {
      WeaponShader.Program pp = this.shaders.pixel();
      if (pp != null && this.light != null) {
         WeaponDef def = px.def;
         WeaponSettings s = look.settings;
         WeaponOptions.Finish finish = look.finish(def);
         PixelPalette pal = px.palette(finish);
         pal.update(def, px.pixelModel, s, finish, FxFrame.currentFrame());
         pp.use();
         this.light.applyPixel(pp);
         pp.set1f("uFog", this.fog ? 1.0F : 0.0F);
         pp.set1f("uTime", this.time);
         pp.set1f("uFlat", this.flatShading() ? 1.0F : 0.0F);
         pp.set1f("uCalm", calm() ? 1.0F : 0.0F);
         pp.set1f("uFinish", pal.pattern);
         pp.set1f("uHandle", pal.handle);
         pp.set2f("uAlong", pm.alongMin, pm.alongMax);
         pp.set1f("uPixel", pm.pixel);
         pp.set1f("uWear", s != null && s.wear != null ? s.wear.getFloat() / 100.0F : 0.0F);
         pp.set1f("uSeed", def.id.hashCode() & 1023);
         pp.set1f("uSheen", look.enchant);
         pp.set1f("uGlint", look.glint);
         pp.set1f("uReveal", look.reveal);
         pp.set1f("uFlare", look.flare);
         pp.set1f("uGlow", s != null && s.glow != null ? s.glow.getFloat() : 1.0F);
         pp.set1f("uAlpha", look.alpha);
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         if (look.enchant == 2) {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + 2);
            Minecraft.getMinecraft().getTextureManager().bindTexture(GLINT);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         }

         bind(0, pal.texture());
         boolean clearBlade = finish == WeaponOptions.Finish.ICE;
         boolean halos = this.pixelHalos();
         boolean ghost = look.alpha < 0.999F;
         float heat = look.heat < 0.0F ? def.heatIdle : look.heat;
         pp.set1f("uHeat", heat);
         pp.set1f("uHeatIdle", def.heatIdle);
         setCore(pp, def);
         boolean edgeHalos = pal.edgeGlows || heat > 0.0F;
         pp.set1f("uBendFrom", def.bendFrom);
         float bendSet = 0.0F;
         boolean chainSet = false;
         if (def.chainPath != null) {
            float[] c = def.chainPath;
            pp.set4f("uChainPath", c[0], c[1], c[2], c[3]);
            pp.set1f("uChainPhase", look.chainPhase);
         }

         float[] cut = extendCut(def, look, pm);
         float[] cutSet = null;
         pp.set4f("uClip", 0.0F, 0.0F, 0.0F, 1.0E9F);
         Map<String, float[]> poses = this.pixelPoses(def, look.poses);
         pm.begin();

         try {
            pp.set1f("uPass", 0.0F);

            for (PixelMesh.Part part : pm.parts) {
               if (!hidden(look, part.name) && !eyeHidden(def, look, part.name, true)) {
                  bendSet = pixelBend(pp, def, look, part.name, bendSet);
                  chainSet = pixelChain(pp, def, part.name, chainSet);
                  cutSet = pixelCut(pp, part.name.equals(def.extend) ? cut : eyeCut(def, look, pm, part.name), cutSet);
                  boolean pushed = pushEye(def, look, pm, part, this.pushPose(def, part.name, poses, pm, cut != null));
                  PixelMesh.draw(part.first, part.count);
                  if (!clearBlade) {
                     PixelMesh.draw(part.bladeFirst, part.bladeCount);
                  }

                  popPose(pushed);
               }
            }

            boolean anyClear = clearBlade;
            boolean anyHalo = false;

            for (PixelMesh.Part partx : pm.parts) {
               anyClear |= partx.clearCount > 0;
               anyHalo |= partx.haloCount > 0 || edgeHalos && partx.edgeHaloCount > 0;
            }

            if (anyClear || (halos &= anyHalo)) {
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
               GlStateManager.depthMask(false);
               if (anyClear) {
                  pp.set1f("uPass", 1.0F);

                  for (PixelMesh.Part partx : pm.parts) {
                     if (!hidden(look, partx.name) && !eyeHidden(def, look, partx.name, true)) {
                        bendSet = pixelBend(pp, def, look, partx.name, bendSet);
                        chainSet = pixelChain(pp, def, partx.name, chainSet);
                        cutSet = pixelCut(pp, partx.name.equals(def.extend) ? cut : eyeCut(def, look, pm, partx.name), cutSet);
                        boolean pushed = pushEye(def, look, pm, partx, this.pushPose(def, partx.name, poses, pm, cut != null));
                        PixelMesh.draw(partx.clearFirst, partx.clearCount);
                        if (clearBlade) {
                           PixelMesh.draw(partx.bladeFirst, partx.bladeCount);
                        }

                        popPose(pushed);
                     }
                  }
               }

               if (halos) {
                  pp.set1f("uPass", 2.0F);

                  for (PixelMesh.Part partxx : pm.parts) {
                     if (!hidden(look, partxx.name) && !eyeHidden(def, look, partxx.name, true)) {
                        bendSet = pixelBend(pp, def, look, partxx.name, bendSet);
                        chainSet = pixelChain(pp, def, partxx.name, chainSet);
                        cutSet = pixelCut(pp, partxx.name.equals(def.extend) ? cut : eyeCut(def, look, pm, partxx.name), cutSet);
                        boolean pushed = pushEye(def, look, pm, partxx, this.pushPose(def, partxx.name, poses, pm, cut != null));
                        PixelMesh.draw(partxx.haloFirst, partxx.haloCount);
                        if (edgeHalos) {
                           PixelMesh.draw(partxx.edgeHaloFirst, partxx.edgeHaloCount);
                        }

                        popPose(pushed);
                     }
                  }
               }

               if (!ghost) {
                  GlStateManager.depthMask(true);
               }

               if (!this.baseBlend) {
                  GlStateManager.disableBlend();
               }
            }
         } finally {
            PixelMesh.end();
            if (bendSet != 0.0F) {
               pp.set1f("uBend", 0.0F);
            }

            if (chainSet) {
               pp.set1f("uChain", 0.0F);
            }

            if (cutSet != null) {
               pp.set4f("uClip", 0.0F, 0.0F, 0.0F, 1.0E9F);
            }

            if (this.program != null) {
               this.program.use();
            } else {
               GL20.glUseProgram(0);
            }
         }
      } else {
         this.drawPixelBaked(px, look);
      }
   }

   private static float[] extendCut(WeaponDef def, WeaponRenderer.Look look, PixelMesh pm) {
      float[] pose = def.extend != null && look.poses != null ? look.poses.get(def.extend) : null;
      WeaponDef.Pivot pv = pose != null && pose[2] < 0.999F ? def.pivots.get(def.extend) : null;
      PixelMesh.Part part = pv != null ? pm.part(def.extend) : null;
      float l = pv != null ? (float)Math.sqrt(pv.axis[0] * pv.axis[0] + pv.axis[1] * pv.axis[1] + pv.axis[2] * pv.axis[2]) : 0.0F;
      if (part != null && !(l < 1.0E-6F)) {
         float ax = pv.axis[0] / l;
         float ay = pv.axis[1] / l;
         float az = pv.axis[2] / l;
         float lo = Float.MAX_VALUE;
         float hi = -Float.MAX_VALUE;

         for (int k = 0; k < 8; k++) {
            float d = ((k & 1) == 0 ? part.min[0] : part.max[0]) * ax
               + ((k & 2) == 0 ? part.min[1] : part.max[1]) * ay
               + ((k & 4) == 0 ? part.min[2] : part.max[2]) * az;
            lo = Math.min(lo, d);
            hi = Math.max(hi, d);
         }

         float origin = pv.origin[0] * ax + pv.origin[1] * ay + pv.origin[2] * az;
         float end = origin + Math.max(0.0F, pose[2]) * (hi - origin);
         return new float[]{ax, ay, az, lo + Math.round((end - lo) / pm.pixel) * pm.pixel};
      } else {
         return null;
      }
   }

   private static float[] pixelCut(WeaponShader.Program pp, float[] want, float[] set) {
      if (want == null ? set != null : !Arrays.equals(want, set)) {
         if (want != null) {
            pp.set4f("uClip", want[0], want[1], want[2], want[3]);
         } else {
            pp.set4f("uClip", 0.0F, 0.0F, 0.0F, 1.0E9F);
         }
      }

      return want;
   }

   private Map<String, float[]> pixelPoses(WeaponDef def, Map<String, float[]> poses) {
      if (def.eye != null && poses != null) {
         this.eyelessPoses.clear();

         for (Entry<String, float[]> e : poses.entrySet()) {
            if (!def.eye.owns(e.getKey())) {
               this.eyelessPoses.put(e.getKey(), e.getValue());
            }
         }

         return this.eyelessPoses.isEmpty() ? null : this.eyelessPoses;
      } else {
         return poses;
      }
   }

   private static float[] currentLightmap() {
      FloatBuffer fb = BufferUtils.createFloatBuffer(16);
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GL11.glGetFloat(2819, fb);
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      return new float[]{fb.get(0), fb.get(1)};
   }

   private static boolean pushEye(WeaponDef def, WeaponRenderer.Look look, PixelMesh pm, PixelMesh.Part part, boolean pushed) {
      WeaponDef.Eye e = def.eye;
      if (e == null) {
         return pushed;
      } else {
         float[] s = look.eye != null ? look.eye : EYE_REST;
         float dx = 0.0F;
         float dy = 0.0F;
         float dz = 0.0F;
         if (part.name.equals(e.pupil)) {
            float reach = Math.max(0.0F, e.pixels[1] - s[2]);
            dx = s[0] * pm.pixel;
            dy = Math.max(-reach, Math.min(reach, s[1])) * pm.pixel;
            dz = s[3] < 0.0F ? -(part.min[2] + part.max[2]) : 0.0F;
         } else if (part.name.equals(e.upper)) {
            dy = -s[2] * pm.pixel;
         } else if (part.name.equals(e.lower)) {
            dy = s[2] * pm.pixel;
         }

         if (dx == 0.0F && dy == 0.0F && dz == 0.0F) {
            return pushed;
         } else {
            if (!pushed) {
               GlStateManager.pushMatrix();
            }

            GlStateManager.translate(dx, dy, dz);
            return true;
         }
      }
   }

   private static float[] eyeCut(WeaponDef def, WeaponRenderer.Look look, PixelMesh pm, String part) {
      WeaponDef.Eye e = def.eye;
      boolean upper = e != null && part.equals(e.upper);
      if (e != null && (upper || part.equals(e.lower))) {
         PixelMesh.Part gem = pm.part(e.gem);
         if (gem == null) {
            return null;
         } else {
            float frame = (look.eye != null ? look.eye : EYE_REST)[2] * pm.pixel;
            float[] fArray;
            if (upper) {
               float[] fArray2 = new float[]{0.0F, 1.0F, 0.0F, 0.0F};
               fArray = fArray2;
               fArray2[3] = gem.max[1] + frame;
            } else {
               float[] fArray3 = new float[]{0.0F, -1.0F, 0.0F, 0.0F};
               fArray = fArray3;
               fArray3[3] = -(gem.min[1] - frame);
            }

            return fArray;
         }
      } else {
         return null;
      }
   }

   private static boolean eyeHidden(WeaponDef def, WeaponRenderer.Look look, String part, boolean cut) {
      WeaponDef.Eye e = def.eye;
      if (e == null) {
         return false;
      } else {
         float frame = (look.eye != null ? look.eye : EYE_REST)[2];
         return part.equals(e.pupil) ? frame >= e.pixels[2] : !cut && (part.equals(e.upper) || part.equals(e.lower)) && frame < e.pixels[2];
      }
   }

   private static boolean pixelChain(WeaponShader.Program pp, WeaponDef def, String part, boolean set) {
      boolean on = def.chainPath != null && def.roleOf(part) == WeaponDef.Role.CHAIN;
      if (on != set) {
         pp.set1f("uChain", on ? 1.0F : 0.0F);
      }

      return on;
   }

   private static float pixelBend(WeaponShader.Program pp, WeaponDef def, WeaponRenderer.Look look, String part, float set) {
      float b = look.bend != 0.0F && def.roleOf(part) == WeaponDef.Role.BLADE ? look.bend : 0.0F;
      if (b != set) {
         pp.set1f("uBend", b);
      }

      return b;
   }

   private static boolean hidden(WeaponRenderer.Look look, String part) {
      float[] pose = look.poses != null ? look.poses.get(part) : null;
      return pose != null && pose[3] < 0.5F;
   }

   private void drawPixelMask(WeaponAssets px, PixelMesh pm, WeaponRenderer.Look look) {
      bind(0, this.whiteTexture());
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      Map<String, float[]> poses = this.pixelPoses(px.def, look.poses);
      pm.beginShape();

      try {
         for (PixelMesh.Part part : pm.parts) {
            if (!hidden(look, part.name) && !eyeHidden(px.def, look, part.name, false)) {
               boolean pushed = pushEye(px.def, look, pm, part, this.pushPose(px.def, part.name, poses, pm, false));
               PixelMesh.draw(part.first, part.count);
               PixelMesh.draw(part.bladeFirst, part.bladeCount);
               PixelMesh.draw(part.clearFirst, part.clearCount);
               popPose(pushed);
            }
         }
      } finally {
         PixelMesh.end();
      }
   }

   private void drawPixelBaked(WeaponAssets px, WeaponRenderer.Look look) {
      PixelMesh pm = px.pixelMesh(0);
      if (pm != null && pm.hasBaked() && px.pixelModel != null) {
         WeaponDef def = px.def;
         WeaponSettings s = look.settings;
         WeaponOptions.Finish finish = look.finish(def);
         PixelPalette pal = px.palette(finish);
         pal.update(def, px.pixelModel, s, finish, FxFrame.currentFrame());
         long key = (s != null ? s.version() : 0) * 1000003L + finish.ordinal() * 7919L + (pal.animated ? ClientClock.millis() / 100L : 0L);
         if (pm.bakedKey != key) {
            px.bakeColors(pm, pal, s, key);
         }

         bind(0, this.whiteTexture());
         RenderHelper.enableStandardItemLighting();
         GlStateManager.enableRescaleNormal();
         GlStateManager.color(1.0F, 1.0F, 1.0F, look.alpha);
         Map<String, float[]> poses = this.pixelPoses(def, look.poses);
         pm.beginBaked();

         try {
            for (PixelMesh.Part part : pm.parts) {
               if (!hidden(look, part.name) && !eyeHidden(def, look, part.name, false)) {
                  boolean pushed = pushEye(def, look, pm, part, this.pushPose(def, part.name, poses, pm, false));
                  PixelMesh.draw(part.bFirst, part.bCount);
                  popPose(pushed);
               }
            }

            RenderHelper.disableStandardItemLighting();
            float[] lm = currentLightmap();
            float bx = lm[0];
            float by = lm[1];
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);

            for (PixelMesh.Part partx : pm.parts) {
               if (!hidden(look, partx.name) && !eyeHidden(def, look, partx.name, false)) {
                  boolean pushed = pushEye(def, look, pm, partx, this.pushPose(def, partx.name, poses, pm, false));
                  PixelMesh.draw(partx.bGlowFirst, partx.bGlowCount);
                  PixelMesh.draw(partx.bClearFirst, partx.bClearCount);
                  popPose(pushed);
               }
            }

            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
         } finally {
            PixelMesh.end();
            GlStateManager.resetColor();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            RenderHelper.disableStandardItemLighting();
         }
      }
   }

   private boolean pushPose(WeaponDef def, String name, Map<String, float[]> poses, PixelMesh pm, boolean cutExtend) {
      if (!moves(def, name, poses, 0)) {
         return false;
      } else {
         GlStateManager.pushMatrix();
         this.applyPose(def, name, poses, 0, pm.stretch, cutExtend ? def.extend : null);
         return true;
      }
   }

   private static void popPose(boolean pushed) {
      if (pushed) {
         GlStateManager.popMatrix();
      }
   }

   static void growForPoses(WeaponDef def, PixelMesh mesh, Map<String, float[]> poses, float[] bounds) {
      if (mesh != null) {
         for (int i = 0; i < 3; i++) {
            bounds[i] = Math.min(bounds[i], mesh.min[i]);
            bounds[3 + i] = Math.max(bounds[3 + i], mesh.max[i]);
         }

         if (poses != null || !def.idle.isEmpty()) {
            for (PixelMesh.Part part : mesh.parts) {
               WeaponDef.Pivot pv = def.pivots.get(part.name);
               if (pv != null && moves(def, part.name, poses, 0)) {
                  float[] pose = poses != null ? poses.get(part.name) : null;
                  float reach = 0.0F;

                  for (int k = 0; k < 8; k++) {
                     float x = (k & 1) == 0 ? part.min[0] : part.max[0];
                     float y = (k & 2) == 0 ? part.min[1] : part.max[1];
                     float z = (k & 4) == 0 ? part.min[2] : part.max[2];
                     float dx = x - pv.origin[0];
                     float dy = y - pv.origin[1];
                     float dz = z - pv.origin[2];
                     reach = Math.max(reach, (float)Math.sqrt(dx * dx + dy * dy + dz * dz));
                  }

                  if (pose != null) {
                     reach = reach * Math.max(1.0F, pose[2]) + Math.abs(pose[1]);
                  }

                  float[] center = pv.origin;
                  String parent = pv.parent;

                  WeaponDef.Pivot pp;
                  for (int depth = 0; parent != null && depth < 8 && (pp = def.pivots.get(parent)) != null; depth++) {
                     float dx = center[0] - pp.origin[0];
                     float dy = center[1] - pp.origin[1];
                     float dz = center[2] - pp.origin[2];
                     reach += (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
                     center = pp.origin;
                     parent = pp.parent;
                  }

                  for (int i = 0; i < 3; i++) {
                     bounds[i] = Math.min(bounds[i], center[i] - reach);
                     bounds[3 + i] = Math.max(bounds[3 + i], center[i] + reach);
                  }
               }
            }
         }
      }
   }

   private void drawMask(WeaponAssets a, WeaponMesh mesh, WeaponRenderer.Look look) {
      bind(0, a.albedo >= 0 ? a.albedo : this.whiteTexture());
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      mesh.begin();

      try {
         for (WeaponMesh.Part part : mesh.parts) {
            float[] pose = look.poses != null ? look.poses.get(part.name) : null;
            if ((pose == null || !(pose[3] < 0.5F)) && a.def.roleOf(part.name) != WeaponDef.Role.CHAIN) {
               this.drawPosed(a.def, mesh, part, look.poses);
            }
         }
      } finally {
         WeaponMesh.end();
      }
   }

   private void drawSimple(WeaponAssets a, WeaponMesh mesh, WeaponRenderer.Look look) {
      WeaponSettings s = look.settings;
      WeaponDef def = a.def;
      WeaponOptions.Finish finish = look.finish(def);
      RenderHelper.enableStandardItemLighting();
      GlStateManager.enableRescaleNormal();
      mesh.begin();

      try {
         for (WeaponMesh.Part part : mesh.parts) {
            float[] pose = look.poses != null ? look.poses.get(part.name) : null;
            if ((pose == null || !(pose[3] < 0.5F)) && def.roleOf(part.name) != WeaponDef.Role.CHAIN) {
               boolean painted = false;
               if (part.name.endsWith("#static")) {
                  bind(0, this.whiteTexture());
                  GlStateManager.color(0.34F, 0.35F, 0.37F, look.alpha);
                  this.drawPosed(def, mesh, part, look.poses);
               } else {
                  int c;
                  switch (def.roleOf(part.name)) {
                     case BLADE:
                        c = finish == WeaponOptions.Finish.PAINTED && s != null && s.paint != null ? s.paint.getColor(0.5F) : finish.swatch;
                        break;
                     case METAL:
                        c = def.metalColor;
                        break;
                     case HANDLE:
                        c = s != null && s.handleColor != null ? s.handleColor.getColor(0.5F) : def.handleColor;
                        break;
                     case ACCENT:
                     case ENERGY:
                     case PAINT:
                        c = s != null && s.accent != null ? s.accent.getColor(0.5F) : def.accentColor;
                        break;
                     default:
                        c = -1;
                        painted = true;
                  }

                  bind(0, painted && a.albedo >= 0 ? a.albedo : this.whiteTexture());
                  GlStateManager.color((c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F, look.alpha);
                  this.drawPosed(def, mesh, part, look.poses);
               }
            }
         }
      } finally {
         WeaponMesh.end();
         RenderHelper.disableStandardItemLighting();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   private static void bind(int unit, int texture) {
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
      GlStateManager.bindTexture(texture);
      if (unit != 0) {
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      }
   }

   int whiteTexture() {
      if (this.white < 0) {
         this.white = TextureUtil.glGenTextures();
         GlStateManager.bindTexture(this.white);
         ByteBuffer px = BufferUtils.createByteBuffer(4);
         ((Buffer)px.put((byte)-1).put((byte)-1).put((byte)-1).put((byte)-1)).flip();
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glTexImage2D(3553, 0, 32856, 1, 1, 0, 6408, 5121, px);
      }

      return this.white;
   }

   static final class Look {
      WeaponSettings settings;
      float alpha = 1.0F;
      int enchant;
      float heat = -1.0F;
      float seed;
      float glint = -1.0F;
      Map<String, float[]> poses;
      WeaponOptions.Finish finish;
      float chainPhase;
      float reveal = 10000.0F;
      float flare;
      float bend;
      boolean pixel;
      float bowBend;
      float bowPull;
      boolean bowNocked;
      float[] eye;

      WeaponRenderer.Look set(WeaponSettings s) {
         this.settings = s;
         this.alpha = 1.0F;
         this.enchant = 0;
         this.heat = -1.0F;
         this.seed = 0.0F;
         this.glint = -1.0F;
         this.poses = null;
         this.finish = null;
         this.chainPhase = 0.0F;
         this.reveal = 10000.0F;
         this.flare = 0.0F;
         this.bend = 0.0F;
         this.pixel = false;
         this.bowBend = 0.0F;
         this.bowPull = 0.0F;
         this.bowNocked = false;
         this.eye = null;
         return this;
      }

      WeaponRenderer.Look copy(WeaponRenderer.Look o) {
         this.settings = o.settings;
         this.alpha = o.alpha;
         this.enchant = o.enchant;
         this.heat = o.heat;
         this.seed = o.seed;
         this.glint = o.glint;
         this.poses = o.poses;
         this.finish = o.finish;
         this.chainPhase = o.chainPhase;
         this.reveal = o.reveal;
         this.flare = o.flare;
         this.bend = o.bend;
         this.pixel = o.pixel;
         this.bowBend = o.bowBend;
         this.bowPull = o.bowPull;
         this.bowNocked = o.bowNocked;
         this.eye = o.eye;
         return this;
      }

      WeaponOptions.Finish finish(WeaponDef def) {
         if (this.finish != null) {
            return this.finish;
         } else {
            WeaponOptions.Finish f = this.settings != null ? this.settings.effectiveFinish() : WeaponOptions.Finish.of(def.finish);
            return f != null ? f : WeaponOptions.Finish.STEEL_POLISHED;
         }
      }
   }

   static enum Mode {
      SHADED,
      MASK,
      SIMPLE;
   }
}
