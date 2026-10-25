package meowtils.furiahvisuals.gui;

import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.misc.skins.SkinHooks;
import meowtils.furiahvisuals.render.Camera;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.PoseSnapshot;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

final class PreviewRenderer {
   private static final float DISTANCE = 7.2F;
   private static final float FOV = 30.0F;
   private static final float FOCUS_Y = 1.3F;
   private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
   private float yaw = 28.0F;
   private float pitch = 12.0F;
   private float idle = 10.0F;
   private float time;
   private boolean dragging;
   private boolean moved;
   private float lastX;
   private float lastY;
   private final Camera camera = new Camera();
   private final Set<String> reported = new HashSet<>();
   private ModelPlayer wideModel;
   private ModelPlayer slimModel;

   void startDrag(float mx, float my) {
      this.dragging = true;
      this.moved = false;
      this.lastX = mx;
      this.lastY = my;
   }

   void drag(float mx, float my) {
      if (this.dragging) {
         float dx = mx - this.lastX;
         float dy = my - this.lastY;
         if (Math.abs(dx) + Math.abs(dy) > 0.5F) {
            this.moved = true;
         }

         this.yaw += dx * 1.1F;
         this.pitch = Math.max(-10.0F, Math.min(55.0F, this.pitch + dy * 0.6F));
         this.lastX = mx;
         this.lastY = my;
         this.idle = 0.0F;
      }
   }

   boolean endDrag() {
      boolean click = this.dragging && !this.moved;
      this.dragging = false;
      return click;
   }

   boolean isDragging() {
      return this.dragging;
   }

   void render(Module module, float x, float y, float w, float h, float dt) {
      Minecraft mc = Minecraft.getMinecraft();
      this.time += dt;
      if (!this.dragging) {
         this.idle += dt;
         this.yaw = this.yaw + dt * 14.0F * Easing.smoothstep(1.5F, 3.5F, this.idle);
      }

      int vx = Math.round(x * Ui.px);
      int vw = Math.round(w * Ui.px);
      int vh = Math.round(h * Ui.px);
      int vy = mc.displayHeight - Math.round((y + h + Ui.offsetY) * Ui.px);
      if (vw >= 8 && vh >= 8) {
         ((Buffer)VIEWPORT).clear();
         GL11.glGetInteger(2978, VIEWPORT);
         int oldX = VIEWPORT.get(0);
         int oldY = VIEWPORT.get(1);
         int oldW = VIEWPORT.get(2);
         int oldH = VIEWPORT.get(3);
         Ui.pushScissor(x, y, w, h);
         GlStateManager.clear(256);
         GlStateManager.viewport(vx, vy, vw, vh);
         GlStateManager.matrixMode(5889);
         GlStateManager.pushMatrix();
         GlStateManager.loadIdentity();
         Project.gluPerspective(30.0F, (float)vw / vh, 0.05F, 100.0F);
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();
         GlStateManager.loadIdentity();
         GlStateManager.translate(0.0F, 0.0F, -7.2F);
         GlStateManager.rotate(this.pitch, 1.0F, 0.0F, 0.0F);
         GlStateManager.rotate(this.yaw, 0.0F, 1.0F, 0.0F);
         GlStateManager.translate((float)(-module.getPreviewFocusX()), -1.3F, (float)(-module.getPreviewFocusZ()));
         GlStateManager.enableDepth();
         GlStateManager.depthMask(true);
         GlStateManager.depthFunc(515);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

         try {
            FxFrame.nextFrame();

            try {
               module.renderPreviewBackdrop(dt, this.yaw, this.pitch);
            } catch (RuntimeException | LinkageError var24) {
               this.reportOnce(module, var24);
            }

            this.drawFloor();
            double lift = module.getPreviewModelOffsetY();
            if (module.showsPreviewMannequin()) {
               this.drawMannequin(mc, this.time, lift);
            }

            try {
               module.renderPreview(dt);
            } catch (RuntimeException | LinkageError var23) {
               this.reportOnce(module, var23);
            }
         } finally {
            GlStateManager.matrixMode(5889);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            GlStateManager.viewport(oldX, oldY, oldW, oldH);
            GlStateManager.clear(256);
            Ui.popScissor();
         }
      }
   }

   private void reportOnce(Module module, Throwable e) {
      if (this.reported.add(module.getId())) {
         FuriahsVisuals.LOG.error("Preview of '{}' failed (further errors are not logged)", new Object[]{module.getName(), e});
      }
   }

   private void drawFloor() {
      Textures.ensureLoaded();
      RenderState.begin();

      try {
         this.camera.captureModelView();
         Vfx.setCamera(this.camera);
         Vfx.flatSprite(Textures.GLOW, 0.0, 0.0, 0.0, 1.9, 0.0, 788529151);
         Vfx.flatRing(Textures.LINE_CORE, 0.0, 0.0, 0.0, 1.45, 0.015, 96, 486539263);
         Vfx.flatRing(Textures.LINE_CORE, 0.0, 0.0, 0.0, 2.2, 0.012, 96, 285212671);
      } finally {
         RenderState.end();
      }
   }

   private void drawMannequin(Minecraft mc, float time, double lift) {
      ResourceLocation skin = SkinHooks.ownSkin();
      boolean slim = SkinHooks.ownSlim();
      ModelPlayer model = slim ? this.slimModel() : this.wideModel();
      pose(model, time * 20.0F);
      PoseSnapshot.PREVIEW_MANNEQUIN.mannequin(time * 20.0F, slim, lift);
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0, lift, 0.0);
      GlStateManager.enableTexture2D();
      GlStateManager.enableDepth();
      GlStateManager.depthMask(true);
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.alphaFunc(516, 0.1F);
      GlStateManager.disableCull();
      GlStateManager.enableRescaleNormal();
      GlStateManager.enableColorMaterial();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GlStateManager.disableTexture2D();
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
      RenderHelper.enableStandardItemLighting();

      try {
         mc.getTextureManager().bindTexture(skin);
         GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
         GlStateManager.scale(-1.0F, -1.0F, 1.0F);
         GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
         GlStateManager.translate(0.0F, -1.5078125F, 0.0F);
         float s = 0.0625F;
         model.bipedHead.render(s);
         model.bipedBody.render(s);
         model.bipedRightArm.render(s);
         model.bipedLeftArm.render(s);
         model.bipedRightLeg.render(s);
         model.bipedLeftLeg.render(s);
         model.bipedHeadwear.render(s);
         model.bipedBodyWear.render(s);
         model.bipedRightArmwear.render(s);
         model.bipedLeftArmwear.render(s);
         model.bipedRightLegwear.render(s);
         model.bipedLeftLegwear.render(s);
      } finally {
         RenderHelper.disableStandardItemLighting();
         GlStateManager.disableRescaleNormal();
         GlStateManager.disableColorMaterial();
         GlStateManager.enableBlend();
         GlStateManager.popMatrix();
      }
   }

   private static void pose(ModelPlayer m, float ageInTicks) {
      for (ModelRenderer part : new ModelRenderer[]{
         m.bipedHead, m.bipedHeadwear, m.bipedBody, m.bipedRightArm, m.bipedLeftArm, m.bipedRightLeg, m.bipedLeftLeg
      }) {
         part.rotateAngleX = 0.0F;
         part.rotateAngleY = 0.0F;
         part.rotateAngleZ = 0.0F;
      }

      float sway = (float)Math.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
      float swing = (float)Math.sin(ageInTicks * 0.067F) * 0.05F;
      m.bipedRightArm.rotateAngleZ = sway;
      m.bipedLeftArm.rotateAngleZ = -sway;
      m.bipedRightArm.rotateAngleX = swing;
      m.bipedLeftArm.rotateAngleX = -swing;
      ModelBase.copyModelAngles(m.bipedRightArm, m.bipedRightArmwear);
      ModelBase.copyModelAngles(m.bipedLeftArm, m.bipedLeftArmwear);
      ModelBase.copyModelAngles(m.bipedRightLeg, m.bipedRightLegwear);
      ModelBase.copyModelAngles(m.bipedLeftLeg, m.bipedLeftLegwear);
      ModelBase.copyModelAngles(m.bipedBody, m.bipedBodyWear);
      ModelBase.copyModelAngles(m.bipedHead, m.bipedHeadwear);
   }

   private ModelPlayer wideModel() {
      if (this.wideModel == null) {
         this.wideModel = new ModelPlayer(0.0F, false);
      }

      return this.wideModel;
   }

   private ModelPlayer slimModel() {
      if (this.slimModel == null) {
         this.slimModel = new ModelPlayer(0.0F, true);
      }

      return this.slimModel;
   }
}
