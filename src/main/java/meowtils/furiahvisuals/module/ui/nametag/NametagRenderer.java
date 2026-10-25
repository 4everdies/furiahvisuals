package meowtils.furiahvisuals.module.ui.nametag;

import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.hud.HudFont;
import meowtils.furiahvisuals.hud.HudGl;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.hud.HudPanel;
import meowtils.furiahvisuals.hud.ScreenProjection;
import meowtils.furiahvisuals.module.ui.LabyCompat;
import meowtils.furiahvisuals.module.ui.scoreboard.ScoreboardElement;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.OptiFineCompat;
import meowtils.furiahvisuals.setting.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class NametagRenderer {
   static final float UNIT = 0.02666667F;
   private static final int FAINT = 32;
   private final NametagElement e;
   final NameGlow glow = new NameGlow();
   private float fov = Float.NaN;
   private int glows;
   private HudLook look;
   private static boolean reported;
   private static final float SEE_THROUGH = 0.25F;
   private static final int ARC = 6;

   NametagRenderer(NametagElement e) {
      this.e = e;
   }

   void newFrame() {
      this.fov = ScreenProjection.fieldOfView();
      this.glows = 0;
      this.look = this.e.look(Ui.px);
   }

   private static void faceCamera(RenderManager rm) {
      boolean front = Minecraft.getMinecraft().gameSettings.thirdPersonView == 2;
      float yaw = rm.playerViewY;
      float pitch = rm.playerViewX;
      float[] laby = LabyCompat.cameraAngles();
      if (laby != null) {
         yaw = laby[0] + (front ? 180.0F : 0.0F);
         pitch = laby[1];
      }

      GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(front ? -pitch : pitch, 1.0F, 0.0F, 0.0F);
   }

   private float pixelsPerBlock(double distance) {
      float f = Float.isNaN(this.fov) ? 70.0F : this.fov;
      double half = Math.tan(Math.toRadians(f) / 2.0);
      return (float)(Minecraft.getMinecraft().displayHeight / 2.0 / (Math.max(0.05, distance) * half));
   }

   static float quantize(float px) {
      float p = Math.max(0.7F, Math.min(8.0F, px));
      long k = Math.round(Math.log(p) / Math.log(Math.sqrt(2.0)));
      return (float)Math.pow(Math.sqrt(2.0), k);
   }

   void drawWorld(Entity entity, String str, double x, double y, double z, NametagElement.Who who, boolean sneaking, RenderManager rm) {
      float size = this.e.size.getFloat() / 100.0F;
      double ay = y + entity.height + 0.5;
      double dist = Math.sqrt(x * x + ay * ay + z * z);
      float px = quantize(this.pixelsPerBlock(dist) * 0.02666667F * size);
      float savedPx = Ui.px;
      float savedOffset = Ui.offsetY;
      GlStateManager.pushMatrix();
      boolean shaders = OptiFineCompat.shadersActive();
      if (!reported && LabyCompat.present()) {
         reported = true;
         FuriahsVisuals.LOG.info("UI: GL state before the first restyled name (LabyMod): {}", new Object[]{HudGl.describeState()});
      }

      int outerProgram = shaders ? 0 : HudGl.program();
      if (outerProgram != 0) {
         GL20.glUseProgram(0);
      }

      if (!shaders) {
         Ui.beginFrameQuick(0);
      }

      try {
         GlStateManager.translate((float)x, (float)ay, (float)z);
         GL11.glNormal3f(0.0F, 1.0F, 0.0F);
         faceCamera(rm);
         GlStateManager.scale(-0.02666667F, -0.02666667F, 0.02666667F);
         if (sneaking) {
            GlStateManager.translate(0.0F, 9.374999F, 0.0F);
         }

         float dy = "deadmau5".equals(str) ? -10.0F : 0.0F;
         GlStateManager.translate(0.0F, 8.0F + dy, 0.0F);
         GlStateManager.scale(size, size, 1.0F);
         GlStateManager.translate(0.0F, -8.0F - dy, 0.0F);
         Ui.px = px;
         Ui.offsetY = 0.0F;
         if (this.look == null) {
            this.look = this.e.look(px);
         }

         HudGl.set(2896, false);
         HudGl.set(3008, false);
         HudGl.set(3042, true);
         HudGl.blendFunc(770, 771, 1, 0);
         HudGl.set(3553, true);
         if (sneaking) {
            HudGl.set(2929, true);
            HudGl.depthMask(false);
            this.background(str, who, dy, shaders, 1.0F, NametagRenderer.Part.FULL);
            this.text(str, who, dy, 32);
            this.depthOnly(str, dy);
         } else {
            HudGl.set(2929, false);
            HudGl.depthMask(false);
            this.background(str, who, dy, shaders, 1.0F, NametagRenderer.Part.BEHIND);
            this.text(str, who, dy, 32);
            HudGl.set(2929, true);
            this.background(str, who, dy, shaders, 1.0F, NametagRenderer.Part.FRONT);
            this.drawGlow(str, who, dy);
            this.text(str, who, dy, 255);
            this.depthOnly(str, dy);
         }
      } finally {
         if (!shaders) {
            Ui.endFrame();
         }

         if (outerProgram != 0) {
            GL20.glUseProgram(outerProgram);
         }

         Ui.px = savedPx;
         Ui.offsetY = savedOffset;
         HudFont.setOnLight(false);
         HudFont.setTint(null);
         GlStateManager.enableDepth();
         GlStateManager.depthMask(true);
         GlStateManager.enableTexture2D();
         GlStateManager.enableAlpha();
         GlStateManager.enableLighting();
         GlStateManager.disableBlend();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.popMatrix();
      }
   }

   private void depthOnly(String str, float dy) {
      HudGl.colorMask(false);
      HudGl.set(3008, true);
      GlStateManager.alphaFunc(516, 0.5F);
      HudGl.depthMask(true);

      try {
         boolean shadow = this.e.shadow.get();
         if (this.modFont()) {
            HudFont.draw(str, -HudFont.width(str) / 2.0F, dy, -1, shadow);
         } else {
            FontRenderer fr = font();
            MyName.drawVanilla(fr, str, -fr.getStringWidth(str) / 2, dy, -1, shadow, null);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         }
      } finally {
         GlStateManager.alphaFunc(516, 0.1F);
         HudGl.set(3008, false);
         HudGl.colorMask(true);
      }
   }

   void drawFlat(String str, NametagElement.Who who, boolean sneaking, boolean behind) {
      this.look = this.e.look(Ui.px);

      try {
         if (!sneaking && !behind) {
            this.background(str, who, 0.0F, false, 1.0F, NametagRenderer.Part.FULL);
            this.drawGlow(str, who, 0.0F);
            this.text(str, who, 0.0F, 255);
         } else {
            this.background(str, who, 0.0F, false, 1.0F, behind ? NametagRenderer.Part.BEHIND : NametagRenderer.Part.FULL);
            this.text(str, who, 0.0F, 32);
         }
      } finally {
         HudFont.setOnLight(false);
         HudFont.setTint(null);
      }
   }

   private boolean modFont() {
      return this.e.font.get() == ScoreboardElement.Font.MOD;
   }

   private static FontRenderer font() {
      return Minecraft.getMinecraft().fontRendererObj;
   }

   private float width(String str) {
      return this.modFont() ? HudFont.width(str) : font().getStringWidth(str);
   }

   private boolean hasBackground(NametagElement.Who who) {
      return who == NametagElement.Who.ME ? this.e.myBackground.get() : this.e.othersBackground.get();
   }

   private boolean rounded() {
      return this.e.shape.get() == NametagElement.Shape.ROUNDED;
   }

   private void background(String str, NametagElement.Who who, float dy, boolean shaders, float alpha, NametagRenderer.Part part) {
      if (this.hasBackground(who)) {
         float w = this.width(str);
         if (!this.rounded()) {
            if (part != NametagRenderer.Part.FRONT) {
               int j = (int)Math.ceil(w) / 2;
               fillRect(-j - 1, -1.0F + dy, j + 1, 8.0F + dy, ColorUtil.mulAlpha(1073741824, alpha));
            }
         } else {
            float x0 = -w / 2.0F - 2.5F;
            float y0 = -1.5F + dy;
            float bw = w + 5.0F;
            float bh = 10.0F;
            float r = Math.min(this.look.radius, bh / 2.0F);
            int full = this.look.bg(alpha);
            float a = (full >>> 24) / 255.0F;
            float seen = Math.min(a, 0.25F * alpha);
            float fa = part == NametagRenderer.Part.BEHIND
               ? seen
               : (part == NametagRenderer.Part.FRONT ? (seen >= 1.0F ? 0.0F : (a - seen) / (1.0F - seen)) : a);
            if (fa > 0.002F) {
               int fill = ColorUtil.withAlpha(full, fa);
               if (shaders) {
                  roundMesh(x0, y0, bw, bh, r, fill);
               } else {
                  Ui.roundRect(x0, y0, bw, bh, r, fill);
               }
            }

            if (part != NametagRenderer.Part.BEHIND) {
               int edge = this.e.outline.get()
                  ? ColorUtil.mulAlpha(this.look.colors.accent | 0xFF000000, 0.95F * alpha)
                  : (this.look.outline ? ColorUtil.mulAlpha(this.look.colors.outline, alpha) : 0);
               if (edge != 0) {
                  if (shaders) {
                     roundMeshOutline(x0, y0, bw, bh, r, Math.max(0.6F, 1.0F / Ui.px), edge);
                  } else {
                     Ui.outline(x0, y0, bw, bh, r, Math.max(1.0F, (float)Math.round(Ui.px * 0.6F)) / Ui.px, edge);
                  }
               }

               if (this.look.glint && !shaders) {
                  HudPanel.glint(x0, y0, bw, bh, r, this.look.colors.accent, this.look.global, this.look.colors.light, alpha);
               }
            }
         }
      }
   }

   private void text(String str, NametagElement.Who who, float dy, int alpha) {
      boolean light = this.rounded() && this.hasBackground(who) && this.look.colors.light;
      int base = light ? this.look.colors.text & 16777215 : 16777215;
      int color = alpha << 24 | base;
      int[] tint = who == NametagElement.Who.ME && this.e.customName()
         ? MyName.tintFormatted(this.e, str, NametagElement.myName(), ColorSetting.clock(), base)
         : null;
      boolean shadow = this.e.shadow.get();
      if (this.modFont()) {
         HudFont.setOnLight(light);
         HudFont.setTint(tint);
         float w = HudFont.width(str);
         HudFont.draw(str, -w / 2.0F, dy, color, shadow);
         HudFont.setTint(null);
         HudFont.setOnLight(false);
      } else {
         FontRenderer fr = font();
         MyName.drawVanilla(fr, str, -fr.getStringWidth(str) / 2, dy, color, shadow, tint);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      }
   }

   private void drawGlow(String str, NametagElement.Who who, float dy) {
      NametagElement.Glow mode = this.e.glow.get();
      if (mode != NametagElement.Glow.OFF && who != NametagElement.Who.THING && (mode != NametagElement.Glow.MINE || who == NametagElement.Who.ME)) {
         String plain = MyName.plain(str);
         NameGlow.Entry g = this.glow.get(plain, false);
         if (g != null) {
            this.glows++;
            float w = this.width(str);
            final double time = ColorSetting.clock();
            String name = NametagElement.myName();
            int nameAt = plain.indexOf(who == NametagElement.Who.ME ? name : stripRank(plain));
            int[] own = MyName.ownColors(str, 16777215);
            final int single = nameAt >= 0 && nameAt < own.length ? own[nameAt] : (own.length > 0 ? own[own.length - 1] : 16777215);
            final boolean custom = who == NametagElement.Who.ME && this.e.nameColor.get() != NametagElement.NameColor.VANILLA;
            NameGlow.Colors colors = new NameGlow.Colors() {
               @Override
               public int at(float t) {
                  return custom ? NametagRenderer.this.e.nameColorAt(t, time) : single;
               }
            };
            NameGlow.draw(g, -w / 2.0F, dy, w, colors, Math.min(1.0F, this.e.glowStrength.getFloat() * 0.95F));
         }
      }
   }

   private static String stripRank(String plain) {
      int space = plain.lastIndexOf(32);
      return space >= 0 && space + 1 < plain.length() ? plain.substring(space + 1) : plain;
   }

   private static void fillRect(float x0, float y0, float x1, float y1, int c) {
      GlStateManager.disableTexture2D();
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
      int r = c >> 16 & 0xFF;
      int g = c >> 8 & 0xFF;
      int b = c & 0xFF;
      int a = c >>> 24;
      wr.pos(x0, y0, 0.0).color(r, g, b, a).endVertex();
      wr.pos(x0, y1, 0.0).color(r, g, b, a).endVertex();
      wr.pos(x1, y1, 0.0).color(r, g, b, a).endVertex();
      wr.pos(x1, y0, 0.0).color(r, g, b, a).endVertex();
      tess.draw();
      GlStateManager.enableTexture2D();
   }

   private static float[] edge(float x, float y, float w, float h, float r, float inset) {
      float[] out = new float[56];
      float[][] corners = new float[][]{{x + r, y + r, 270.0F}, {x + r, y + h - r, 180.0F}, {x + w - r, y + h - r, 90.0F}, {x + w - r, y + r, 0.0F}};
      float rr = Math.max(0.0F, r - inset);
      int n = 0;

      for (float[] k : corners) {
         for (int i = 0; i <= 6; i++) {
            double a = Math.toRadians(k[2] - 90.0F * i / 6.0F);
            out[n++] = k[0] + (float)Math.cos(a) * rr;
            out[n++] = k[1] + (float)Math.sin(a) * rr;
         }
      }

      return out;
   }

   private static void roundMesh(float x, float y, float w, float h, float r, int c) {
      float[] p = edge(x, y, w, h, r, 0.0F);
      GlStateManager.disableTexture2D();
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(6, DefaultVertexFormats.POSITION_COLOR);
      int cr = c >> 16 & 0xFF;
      int cg = c >> 8 & 0xFF;
      int cb = c & 0xFF;
      int ca = c >>> 24;
      wr.pos(x + w / 2.0F, y + h / 2.0F, 0.0).color(cr, cg, cb, ca).endVertex();

      for (int i = 0; i < p.length; i += 2) {
         wr.pos(p[i], p[i + 1], 0.0).color(cr, cg, cb, ca).endVertex();
      }

      wr.pos(p[0], p[1], 0.0).color(cr, cg, cb, ca).endVertex();
      tess.draw();
      GlStateManager.enableTexture2D();
   }

   private static void roundMeshOutline(float x, float y, float w, float h, float r, float t, int c) {
      float[] o = edge(x, y, w, h, r, 0.0F);
      float[] in = edge(x + t, y + t, w - 2.0F * t, h - 2.0F * t, Math.max(0.0F, r - t), 0.0F);
      GlStateManager.disableTexture2D();
      Tessellator tess = Tessellator.getInstance();
      WorldRenderer wr = tess.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
      int cr = c >> 16 & 0xFF;
      int cg = c >> 8 & 0xFF;
      int cb = c & 0xFF;
      int ca = c >>> 24;
      int n = o.length / 2;

      for (int i = 0; i < n; i++) {
         int a = i * 2;
         int b = (i + 1) % n * 2;
         wr.pos(o[a], o[a + 1], 0.0).color(cr, cg, cb, ca).endVertex();
         wr.pos(o[b], o[b + 1], 0.0).color(cr, cg, cb, ca).endVertex();
         wr.pos(in[b], in[b + 1], 0.0).color(cr, cg, cb, ca).endVertex();
         wr.pos(in[a], in[a + 1], 0.0).color(cr, cg, cb, ca).endVertex();
      }

      tess.draw();
      GlStateManager.enableTexture2D();
   }

   void forget() {
      this.glow.clear();
   }

   String describeForTest() {
      return this.glow.size() + " glows kept, " + this.glows + " drawn last frame";
   }

   String frameCostForTest() {
      return "";
   }

   static enum Part {
      BEHIND,
      FRONT,
      FULL;
   }
}
