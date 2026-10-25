package meowtils.furiahvisuals.module.weapons;

import java.util.Arrays;
import java.util.List;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.misc.skins.SkinHooks;
import meowtils.furiahvisuals.render.GlSave;
import meowtils.furiahvisuals.render.GlTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

final class WeaponViews {
   private static final int MAX_CELLS = 96;
   private static final long CARD_FRAME = 25000000L;
   private static final float CARD_FOV = 20.0F;
   private static final float MODEL_FOV = 24.0F;
   private static final float BODY_FOV = 30.0F;
   private static final float[] ITEM_BOX = new float[]{-0.12F, -0.62F, -0.12F, 0.12F, 0.62F, 0.12F};
   private static final ItemStack DIAMOND_SWORD = new ItemStack(Items.diamond_sword);
   private final WeaponsModule module;
   private final WeaponStudio studio;
   private final GlSave save = new GlSave();
   private GlTarget view;
   private GlTarget compareView;
   private final WeaponViews.Atlas cardAtlas = new WeaponViews.Atlas(6);
   private final WeaponViews.Atlas tileAtlas = new WeaponViews.Atlas(2);
   private boolean failed;
   private final FirstPersonRenderer fp;
   final ViewModels hands;
   final VanillaView vanillaHand;
   private final ViewModels.Body body = new ViewModels.Body();
   private ModelPlayer wideModel;
   private ModelPlayer slimModel;
   private float[] wideRest;
   private float[] slimRest;
   private final Mat4 turn = new Mat4();
   private final Mat4 model = new Mat4();
   private final Mat4 proj = new Mat4();
   private final Mat4 m = new Mat4();
   private final float[] box = new float[6];
   private final float[] frame = new float[6];

   WeaponViews(WeaponsModule module, WeaponRenderer renderer) {
      this.module = module;
      this.studio = new WeaponStudio(renderer);
      this.fp = new FirstPersonRenderer(renderer);
      this.hands = new ViewModels(module, this.fp);
      this.vanillaHand = new VanillaView(module, this.hands);
   }

   int drawCards(List<WeaponViews.Card> cards, int w, int h) {
      return this.drawCards(this.cardAtlas, cards, w, h);
   }

   int drawTiles(List<WeaponViews.Card> cards, int w, int h) {
      return this.drawCards(this.tileAtlas, cards, w, h);
   }

   private int drawCards(WeaponViews.Atlas at, List<WeaponViews.Card> cards, int w, int h) {
      if (this.failed) {
         return -1;
      } else {
         w = Math.max(32, Math.min(512, w));
         h = Math.max(32, Math.min(512, h));
         int n = Math.min(cards.size(), 96);
         int rows = Math.max(1, (n + at.cols - 1) / at.cols);
         if (at.target != null && (at.cellW != w || at.cellH != h || rows > at.rows)) {
            GlTarget.delete(at.target);
            at.target = null;
         }

         if (at.target == null) {
            at.rows = Math.max(rows, at.rows);
            at.owner = new String[at.cells()];
            at.taken = new boolean[at.cells()];
         }

         int cells = at.cells();
         at.cellW = w;
         at.cellH = h;
         long now = ClientClock.nanos();
         boolean due = now - at.drawn >= 25000000L;
         Arrays.fill(at.taken, false);

         for (int i = 0; i < cards.size(); i++) {
            WeaponViews.Card c = cards.get(i);
            c.cell = -1;
            if (i < n) {
               String key = c.key();

               for (int k = 0; k < cells; k++) {
                  if (!at.taken[k] && key.equals(at.owner[k])) {
                     c.cell = k;
                     at.taken[k] = true;
                     break;
                  }
               }

               at.fresh[i] = c.cell < 0;
            }
         }

         for (int var30 = 0; var30 < n; var30++) {
            WeaponViews.Card c = cards.get(var30);
            if (c.cell < 0) {
               for (int kx = 0; kx < cells; kx++) {
                  if (!at.taken[kx]) {
                     c.cell = kx;
                     at.taken[kx] = true;
                     break;
                  }
               }
            }
         }

         GlStateManager.matrixMode(5889);
         GlStateManager.pushMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();
         this.save.save();

         try {
            begin();
            if (at.target == null) {
               at.target = GlTarget.create(w * at.cols, h * at.rows, 32856, 6408, true, false);
               if (at.target == null) {
                  this.failed = true;
                  return -1;
               }

               GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
               GlStateManager.clear(16384);
            }

            for (int var31 = 0; var31 < n; var31++) {
               WeaponViews.Card c = cards.get(var31);
               if (c.cell >= 0 && (due || at.fresh[var31])) {
                  boolean drawn;
                  if (c.def.isVanilla()) {
                     drawn = this.itemTurntable(this.vanillaStack(), c.angle, w * 2, h * 2, 20.0F, 0.9F, cardGlow(c.def));
                  } else {
                     WeaponAssets a = WeaponAssets.of(c.def, WeaponAssets.Tier.CARD);
                     a.request(256);
                     boolean pixel = this.module.pixelOf(c.def, c.style < 0 ? null : c.style == 1);
                     WeaponAssets px = pixel ? a.pixelTwin() : null;
                     if (px != null) {
                        px.request(0);
                     }

                     boolean waiting = px != null && px.loading();
                     if (!a.ready() || waiting) {
                        WeaponAssets.pump();
                        waiting = px != null && px.loading();
                     }

                     drawn = a.ready() && !waiting && this.weaponTurntable(a, 1, this.module.settingsOf(c.def), c.angle, w * 2, h * 2, cardGlow(c.def), pixel);
                  }

                  if (!drawn) {
                     at.owner[c.cell] = null;
                     c.cell = -1;
                  } else {
                     this.studio.finish(at.target, c.cell % at.cols * w, c.cell / at.cols * h, w, h, w * 2, h * 2);
                     at.owner[c.cell] = c.key();
                  }
               }
            }

            if (due) {
               at.drawn = now;
            }

            return at.target.texture;
         } catch (RuntimeException | LinkageError var24) {
            this.failed = true;
            FuriahsVisuals.LOG.warn("The weapon cards' previews are off: they could not be drawn", var24);
            return -1;
         } finally {
            this.save.restore();
            GlStateManager.matrixMode(5889);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
         }
      }
   }

   float[] cellUv(int cell, float[] out) {
      return cellUv(this.cardAtlas, cell, out);
   }

   float[] tileUv(int cell, float[] out) {
      return cellUv(this.tileAtlas, cell, out);
   }

   private static float[] cellUv(WeaponViews.Atlas at, int cell, float[] out) {
      int cellW = at.cellW;
      int cellH = at.cellH;
      float aw = cellW * at.cols;
      float ah = cellH * Math.max(1, at.rows);
      int cx = cell % at.cols * cellW;
      int cy = cell / at.cols * cellH;
      out[0] = cx / aw;
      out[2] = (cx + cellW) / aw;
      out[1] = (cy + cellH) / ah;
      out[3] = cy / ah;
      return out;
   }

   private static int cardGlow(WeaponDef def) {
      return def.isVanilla() ? 982497535 : def.accentColor & 16777215 | 1006632960;
   }

   int drawPreview(WeaponViews.Stage stage, WeaponDef def, WeaponSettings s, int w, int h, float dt) {
      stage.loading = false;
      stage.compareTexture = -1;
      stage.compareLoading = false;
      if (this.failed) {
         return -1;
      } else {
         w = Math.max(32, Math.min(2048, w));
         h = Math.max(32, Math.min(2048, h));
         stage.time += dt;
         GlStateManager.matrixMode(5889);
         GlStateManager.pushMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();
         this.save.save();

         try {
            begin();
            if (this.view != null && (this.view.width != w || this.view.height != h)) {
               GlTarget.delete(this.view);
               this.view = null;
            }

            if (this.view == null) {
               this.view = GlTarget.create(w, h, 32856, 6408, true, false);
               if (this.view == null) {
                  this.failed = true;
                  return -1;
               }
            }

            WeaponAssets a = null;
            if (!def.isVanilla()) {
               a = WeaponAssets.of(def, WeaponAssets.Tier.FULL);
               a.request(this.module.textureSize());
               if (!a.ready()) {
                  WeaponAssets.pump();
               }

               if (a.state() == WeaponAssets.State.FAILED) {
                  return -1;
               }

               if (!a.ready()) {
                  stage.loading = true;
                  return -1;
               }
            }

            int ww = w * 2;
            int wh = h * 2;
            boolean compare = stage.compare && a != null && this.compareTarget(w, h);
            Boolean style = compare ? Boolean.FALSE : null;
            boolean drawn;
            switch (stage.tab) {
               case FIRST_PERSON:
                  drawn = this.firstPerson(stage, def, s, ww, wh, dt, compare);
                  break;
               case THIRD_PERSON:
                  drawn = this.thirdPerson(stage, def, a, s, ww, wh, style);
                  break;
               default:
                  drawn = def.isVanilla() ? this.itemModel(stage, ww, wh) : this.weaponModel(stage, a, s, ww, wh, style);
            }

            if (!drawn) {
               stage.loading = true;
               return -1;
            } else {
               this.studio.finish(this.view, 0, 0, w, h, ww, wh);
               if (compare) {
                  this.comparePixelated(stage, def, a, s, w, h);
               }

               return this.view.texture;
            }
         } catch (RuntimeException | LinkageError var18) {
            this.failed = true;
            FuriahsVisuals.LOG.warn("The weapon preview is off: it could not be drawn", var18);
            return -1;
         } finally {
            this.save.restore();
            GlStateManager.matrixMode(5889);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
         }
      }
   }

   private boolean firstPerson(WeaponViews.Stage stage, WeaponDef def, WeaponSettings s, int w, int h, float dt, boolean compare) {
      Minecraft mc = Minecraft.getMinecraft();
      boolean[] slim = new boolean[1];
      skin(mc, slim);
      this.hands.dualOverride = stage.dual;
      this.hands.previewBlocking = stage.blocking;
      this.hands.compareFx = compare;
      if (def.isVanilla()) {
         if (!this.studio.begin(w, h)) {
            return false;
         } else {
            backdrop(-13749699, -15592680, 0.45F, -0.35F, 1.1F, 1.2F, cardGlow(def));
            this.vanillaFirstPerson(mc, stage, def, s, w, h, dt);
            return true;
         }
      } else {
         this.body.slim = slim[0];
         EntityPlayerSP p = mc.thePlayer;
         this.body.sleeveRight = p == null || FirstPersonRenderer.sleeve(p, true);
         this.body.sleeveLeft = p == null || FirstPersonRenderer.sleeve(p, false);
         this.body.held = null;
         this.body.walked = stage.time * 1.5F;
         if (!this.hands.preview(def, s, this.body, dt)) {
            return false;
         } else {
            if (compare) {
               this.fp.studioStyle(false);
            }

            return this.firstPersonScene(def, s, w, h);
         }
      }
   }

   private boolean firstPersonScene(WeaponDef def, WeaponSettings s, int w, int h) {
      Minecraft mc = Minecraft.getMinecraft();
      ResourceLocation skin = skin(mc, new boolean[1]);
      if (!this.studio.begin(w, h)) {
         return false;
      } else {
         backdrop(-13749699, -15592680, 0.45F, -0.35F, 1.1F, 1.2F, cardGlow(def));
         GlStateManager.clear(256);
         this.fp.drawStudio(mc, skin, s.fov.getFloat(), w, h, this.module.clientTime());
         if (this.hands.previewVanillaLeft()) {
            GL20.glUseProgram(0);
            GlStateManager.clear(256);
            this.loadPerspective(70.0F, (float)w / h);
            noLightmap();
            this.hands.drawPreviewVanillaLeft(mc, this.vanillaStack(), s.leftHanded.get());
         }

         return true;
      }
   }

   private boolean compareTarget(int w, int h) {
      if (this.compareView != null && (this.compareView.width != w || this.compareView.height != h)) {
         GlTarget.delete(this.compareView);
         this.compareView = null;
      }

      if (this.compareView == null) {
         this.compareView = GlTarget.create(w, h, 32856, 6408, true, false);
      }

      return this.compareView != null;
   }

   private void comparePixelated(WeaponViews.Stage stage, WeaponDef def, WeaponAssets a, WeaponSettings s, int w, int h) {
      WeaponAssets px = a.pixelTwin();
      px.request(0, true);
      if (px.loading()) {
         WeaponAssets.pump();
         if (px.loading()) {
            stage.compareLoading = true;
            return;
         }
      }

      int ww = w * 2;
      int wh = h * 2;
      boolean drawn;
      switch (stage.tab) {
         case FIRST_PERSON:
            this.fp.studioStyle(true);
            drawn = this.firstPersonScene(def, s, ww, wh);
            break;
         case THIRD_PERSON:
            drawn = this.thirdPerson(stage, def, a, s, ww, wh, Boolean.TRUE);
            break;
         default:
            drawn = this.weaponModel(stage, a, s, ww, wh, Boolean.TRUE);
      }

      if (drawn) {
         this.studio.finish(this.compareView, 0, 0, w, h, ww, wh);
         stage.compareTexture = this.compareView.texture;
      }
   }

   private void vanillaFirstPerson(Minecraft mc, WeaponViews.Stage stage, WeaponDef def, WeaponSettings s, int w, int h, float dt) {
      this.vanillaHand.previewStep(def, s, dt);
      float swing = 0.0F;
      if (stage.vanillaSwing >= 0.0F) {
         stage.vanillaSwing += dt;
         swing = stage.vanillaSwing / 0.3F;
         if (swing >= 1.0F) {
            stage.vanillaSwing = -1.0F;
            swing = 0.0F;
         }
      }

      float equip = 0.0F;
      if (stage.vanillaDraw >= 0.0F) {
         stage.vanillaDraw += dt;
         equip = 1.0F - Math.min(1.0F, stage.vanillaDraw / 0.2F);
         if (stage.vanillaDraw >= 0.2F) {
            stage.vanillaDraw = -1.0F;
         }
      }

      ItemStack stack = this.vanillaStack();
      GL20.glUseProgram(0);
      GlStateManager.clear(256);
      this.loadPerspective(s.fov.getFloat(), (float)w / h);
      noLightmap();
      boolean dual = stage.dual && "same".equals(s.leftWeapon.get());

      for (int copy = 0; copy < (dual ? 2 : 1); copy++) {
         GlStateManager.matrixMode(5888);
         GlStateManager.loadIdentity();
         RenderHelper.enableStandardItemLighting();
         this.vanillaHand.transform(s, copy == 1, this.m);
         GlStateManager.multMatrix(this.m.buffer());
         if (this.m.mirrored()) {
            GL11.glFrontFace(2304);
         }

         try {
            vanillaHold(copy == 1 ? 0.0F : swing, equip, stage.blocking);
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
            mc.getRenderItem().renderItem(stack, TransformType.FIRST_PERSON);
         } finally {
            GL11.glFrontFace(2305);
            RenderHelper.disableStandardItemLighting();
         }
      }
   }

   void previewSwing(WeaponViews.Stage stage, WeaponDef def) {
      if (def.isVanilla()) {
         stage.vanillaSwing = 0.0F;
         this.vanillaHand.hand.anim.cancelInspect();
         this.vanillaHand.hand.motion.attacked();
      } else {
         this.hands.previewSwing();
      }
   }

   void previewDraw(WeaponViews.Stage stage, WeaponDef def) {
      if (def.isVanilla()) {
         stage.vanillaDraw = 0.0F;
         this.vanillaHand.hand.anim.cancelInspect();
      } else {
         this.hands.previewDraw();
      }
   }

   boolean previewInspect(WeaponViews.Stage stage, WeaponDef def, String inspect) {
      stage.blocking = false;
      if (def.isVanilla()) {
         Clip c = this.module.inspects.clipFor(def, inspect);
         if (c == null) {
            return false;
         } else {
            this.vanillaHand.hand.anim.startInspect(c, this.module.inspectSpeed.getFloat());
            return true;
         }
      } else {
         return this.hands.previewInspect(inspect);
      }
   }

   private static void vanillaHold(float swing, float equip, boolean blocking) {
      float pi = (float) Math.PI;
      if (!blocking) {
         float root = MathHelper.sqrt_float(swing);
         GlStateManager.translate(
            -0.4F * MathHelper.sin(root * pi), 0.2F * MathHelper.sin(root * pi * 2.0F), -0.2F * MathHelper.sin(swing * pi)
         );
      }

      float sw = blocking ? 0.0F : swing;
      GlStateManager.translate(0.56F, -0.52F, -0.72F);
      GlStateManager.translate(0.0F, equip * -0.6F, 0.0F);
      GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
      float f = MathHelper.sin(sw * sw * pi);
      float f1 = MathHelper.sin(MathHelper.sqrt_float(sw) * pi);
      GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
      GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
      GlStateManager.scale(0.4F, 0.4F, 0.4F);
      if (blocking) {
         GlStateManager.translate(-0.5F, 0.2F, 0.0F);
         GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
         GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
         GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
      }
   }

   private boolean thirdPerson(WeaponViews.Stage stage, WeaponDef def, WeaponAssets a, WeaponSettings s, int w, int h, Boolean pixel) {
      Minecraft mc = Minecraft.getMinecraft();
      boolean[] slimOut = new boolean[1];
      ResourceLocation skin = skin(mc, slimOut);
      boolean slim = slimOut[0];
      if (!this.studio.begin(w, h)) {
         return false;
      } else {
         backdrop(-13618113, -15526887, 0.0F, -0.25F, 1.3F, 1.1F, cardGlow(def));
         GL20.glUseProgram(0);
         GlStateManager.clear(256);
         this.loadPerspective(30.0F, (float)w / h);
         GlStateManager.matrixMode(5888);
         GlStateManager.loadIdentity();
         GlStateManager.translate(0.0F, 0.0F, -stage.distance);
         GlStateManager.rotate(stage.orbitPitch, 1.0F, 0.0F, 0.0F);
         GlStateManager.rotate(stage.orbitYaw, 0.0F, 1.0F, 0.0F);
         GlStateManager.translate(0.0F, -0.95F, 0.0F);
         floorShadow();
         ModelPlayer body3 = slim ? this.slimModel() : this.wideModel();
         boolean dual = stage.dual && !def.twoHanded;
         WeaponDef leftDef = dual ? this.module.leftWeaponOf(def) : null;
         this.poseMannequin(body3, slim ? this.slimRest : this.wideRest, stage, def, dual, slim);
         GlStateManager.pushMatrix();

         try {
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(-1.0F, -1.0F, 1.0F);
            GlStateManager.scale(0.9375F, 0.9375F, 0.9375F);
            GlStateManager.translate(0.0F, -1.5078125F, 0.0F);
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
            noLightmap();
            RenderHelper.enableStandardItemLighting();
            mc.getTextureManager().bindTexture(skin);
            float u = 0.0625F;
            body3.bipedHead.render(u);
            body3.bipedBody.render(u);
            body3.bipedRightArm.render(u);
            body3.bipedLeftArm.render(u);
            body3.bipedRightLeg.render(u);
            body3.bipedLeftLeg.render(u);
            body3.bipedHeadwear.render(u);
            body3.bipedBodyWear.render(u);
            body3.bipedRightArmwear.render(u);
            body3.bipedLeftArmwear.render(u);
            body3.bipedRightLegwear.render(u);
            body3.bipedLeftLegwear.render(u);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableColorMaterial();
            this.studio.light.studio(stage.orbitPitch, stage.orbitYaw - 180.0F);
            ThirdPerson tp = this.module.thirdPerson();
            if (def.isVanilla()) {
               heldItem(mc, body3, this.vanillaStack(), true);
            } else {
               tp.drawStudio(body3, def, a, slim, false, true, this.studio.light, this.module.clientTime(), pixel, bowPull(stage, def));
            }

            if (leftDef != null) {
               if (leftDef.isVanilla()) {
                  heldItem(mc, body3, this.vanillaStack(), false);
               } else {
                  WeaponAssets la = WeaponAssets.of(leftDef, WeaponAssets.Tier.FULL);
                  la.request(this.module.textureSize());
                  if (la.ready()) {
                     tp.drawStudio(body3, leftDef, la, slim, false, false, this.studio.light, this.module.clientTime(), pixel, -1.0F);
                  }
               }
            }
         } finally {
            GlStateManager.popMatrix();
         }

         return true;
      }
   }

   private static void heldItem(Minecraft mc, ModelPlayer body3, ItemStack stack, boolean right) {
      GlStateManager.pushMatrix();

      try {
         (right ? body3.bipedRightArm : body3.bipedLeftArm).postRender(0.0625F);
         if (right) {
            GlStateManager.translate(-0.0625F, 0.4375F, 0.0625F);
         } else {
            GlStateManager.translate(0.0625F, 0.4375F, 0.0625F);
            GlStateManager.scale(-1.0F, 1.0F, 1.0F);
            GL11.glFrontFace(2304);
         }

         GL20.glUseProgram(0);
         RenderHelper.enableStandardItemLighting();
         GlStateManager.enableRescaleNormal();
         GlStateManager.enableAlpha();
         mc.getRenderItem().renderItem(stack, TransformType.THIRD_PERSON);
      } finally {
         GL11.glFrontFace(2305);
         RenderHelper.disableStandardItemLighting();
         GlStateManager.popMatrix();
      }
   }

   private void poseMannequin(ModelPlayer b, float[] rest, WeaponViews.Stage stage, WeaponDef def, boolean dual, boolean slim) {
      ModelRenderer[] parts = parts(b);

      for (int i = 0; i < parts.length; i++) {
         parts[i].rotateAngleZ = 0.0F;
         parts[i].rotateAngleY = 0.0F;
         parts[i].rotateAngleX = 0.0F;
         parts[i].rotationPointX = rest[i * 3];
         parts[i].rotationPointY = rest[i * 3 + 1];
         parts[i].rotationPointZ = rest[i * 3 + 2];
      }

      float t = stage.time;
      float age = t * 20.0F;
      WeaponViews.Stance st = stage.stance;
      float limb = 0.0F;
      float amount = 0.0F;
      if (st == WeaponViews.Stance.WALK) {
         limb = t * 12.0F;
         amount = 0.62F;
      } else if (st == WeaponViews.Stance.SPRINT) {
         limb = t * 17.0F;
         amount = 0.95F;
      }

      float swing = 0.0F;
      if (st == WeaponViews.Stance.ATTACK) {
         float cycle = t % 0.75F;
         swing = cycle < 0.3F ? cycle / 0.3F : 0.0F;
      }

      boolean blocking = st == WeaponViews.Stance.BLOCK && !def.isBow();
      float pi = (float) Math.PI;
      b.bipedRightArm.rotateAngleX = MathHelper.cos(limb * 0.6662F + pi) * amount;
      b.bipedLeftArm.rotateAngleX = MathHelper.cos(limb * 0.6662F) * amount;
      b.bipedRightLeg.rotateAngleX = MathHelper.cos(limb * 0.6662F) * 1.4F * amount;
      b.bipedLeftLeg.rotateAngleX = MathHelper.cos(limb * 0.6662F + pi) * 1.4F * amount;
      b.bipedRightArm.rotateAngleX = b.bipedRightArm.rotateAngleX * 0.5F - pi / 10.0F * (blocking ? 3.0F : 1.0F);
      if (blocking) {
         b.bipedRightArm.rotateAngleY = (float) (-Math.PI / 6);
      }

      b.heldItemRight = blocking ? 3 : 1;
      b.swingProgress = swing;
      b.isSneak = false;
      if (swing > 0.0F) {
         b.bipedBody.rotateAngleY = MathHelper.sin(MathHelper.sqrt_float(swing) * pi * 2.0F) * 0.2F;
         b.bipedRightArm.rotationPointZ = MathHelper.sin(b.bipedBody.rotateAngleY) * 5.0F;
         b.bipedRightArm.rotationPointX = -MathHelper.cos(b.bipedBody.rotateAngleY) * 5.0F;
         b.bipedLeftArm.rotationPointZ = -MathHelper.sin(b.bipedBody.rotateAngleY) * 5.0F;
         b.bipedLeftArm.rotationPointX = MathHelper.cos(b.bipedBody.rotateAngleY) * 5.0F;
         b.bipedRightArm.rotateAngleY = b.bipedRightArm.rotateAngleY + b.bipedBody.rotateAngleY;
         b.bipedLeftArm.rotateAngleY = b.bipedLeftArm.rotateAngleY + b.bipedBody.rotateAngleY;
         b.bipedLeftArm.rotateAngleX = b.bipedLeftArm.rotateAngleX + b.bipedBody.rotateAngleY;
         float f = 1.0F - swing;
         f *= f;
         f *= f;
         f = 1.0F - f;
         float f1 = MathHelper.sin(f * pi);
         float f2 = MathHelper.sin(swing * pi) * -(b.bipedHead.rotateAngleX - 0.7F) * 0.75F;
         b.bipedRightArm.rotateAngleX -= f1 * 1.2F + f2;
         b.bipedRightArm.rotateAngleY = b.bipedRightArm.rotateAngleY + b.bipedBody.rotateAngleY * 2.0F;
         b.bipedRightArm.rotateAngleZ = b.bipedRightArm.rotateAngleZ + MathHelper.sin(swing * pi) * -0.4F;
      }

      b.bipedRightArm.rotateAngleZ = b.bipedRightArm.rotateAngleZ + (MathHelper.cos(age * 0.09F) * 0.05F + 0.05F);
      b.bipedLeftArm.rotateAngleZ = b.bipedLeftArm.rotateAngleZ - (MathHelper.cos(age * 0.09F) * 0.05F + 0.05F);
      b.bipedRightArm.rotateAngleX = b.bipedRightArm.rotateAngleX + MathHelper.sin(age * 0.067F) * 0.05F;
      b.bipedLeftArm.rotateAngleX = b.bipedLeftArm.rotateAngleX - MathHelper.sin(age * 0.067F) * 0.05F;
      this.module.thirdPerson().poseArms(b, def, blocking, st == WeaponViews.Stance.SPRINT, dual, slim, limb, amount, 0.0F, bowPull(stage, def));
      ModelBase.copyModelAngles(b.bipedRightLeg, b.bipedRightLegwear);
      ModelBase.copyModelAngles(b.bipedLeftLeg, b.bipedLeftLegwear);
      ModelBase.copyModelAngles(b.bipedBody, b.bipedBodyWear);
      ModelBase.copyModelAngles(b.bipedHead, b.bipedHeadwear);
   }

   static float bowPull(WeaponViews.Stage stage, WeaponDef def) {
      if (def.isBow() && stage.stance == WeaponViews.Stance.BLOCK) {
         float c = stage.time % 2.4F;
         return c < 2.0F ? BowDraw.charge(c * 20.0F) : -1.0F;
      } else {
         return -1.0F;
      }
   }

   private static ModelRenderer[] parts(ModelPlayer b) {
      return new ModelRenderer[]{b.bipedHead, b.bipedHeadwear, b.bipedBody, b.bipedRightArm, b.bipedLeftArm, b.bipedRightLeg, b.bipedLeftLeg};
   }

   private ModelPlayer wideModel() {
      if (this.wideModel == null) {
         this.wideModel = new ModelPlayer(0.0F, false);
         this.wideRest = rest(this.wideModel);
      }

      return this.wideModel;
   }

   private ModelPlayer slimModel() {
      if (this.slimModel == null) {
         this.slimModel = new ModelPlayer(0.0F, true);
         this.slimRest = rest(this.slimModel);
      }

      return this.slimModel;
   }

   private static float[] rest(ModelPlayer b) {
      ModelRenderer[] parts = parts(b);
      float[] out = new float[parts.length * 3];

      for (int i = 0; i < parts.length; i++) {
         out[i * 3] = parts[i].rotationPointX;
         out[i * 3 + 1] = parts[i].rotationPointY;
         out[i * 3 + 2] = parts[i].rotationPointZ;
      }

      return out;
   }

   private boolean weaponModel(WeaponViews.Stage stage, WeaponAssets a, WeaponSettings s, int w, int h, Boolean pixel) {
      if (a.frame(0, this.frame) && this.studio.begin(w, h)) {
         backdrop(-13618113, -15592680, 0.0F, 0.0F, 1.25F, 1.25F, cardGlow(a.def));
         GlStateManager.clear(256);
         this.orbit(stage, this.frame, (float)w / h);
         this.studio.light.studio(0.0F, 180.0F);
         WeaponRenderer.Look look = new WeaponRenderer.Look().set(s);
         look.chainPhase = this.module.clientTime() * 0.12F;
         look.pixel = this.module.pixelOf(a.def, pixel);
         this.studio.draw(a, look, 0, this.model, 24.0F, w, h, this.module.clientTime());
         return true;
      } else {
         return false;
      }
   }

   private boolean itemModel(WeaponViews.Stage stage, int w, int h) {
      if (!this.studio.begin(w, h)) {
         return false;
      } else {
         backdrop(-13618113, -15592680, 0.0F, 0.0F, 1.25F, 1.25F, cardGlow(this.module.library.vanilla()));
         GlStateManager.clear(256);
         this.orbit(stage, ITEM_BOX, (float)w / h);
         this.model.rotate(45.0F, 0.0F, 0.0F, 1.0F);
         this.drawItem(this.vanillaStack(), 24.0F, (float)w / h);
         return true;
      }
   }

   private void orbit(WeaponViews.Stage stage, float[] b, float aspect) {
      float cx = (b[0] + b[3]) / 2.0F;
      float cy = (b[1] + b[4]) / 2.0F;
      float cz = (b[2] + b[5]) / 2.0F;
      float dx = b[3] - b[0];
      float dy = b[4] - b[1];
      float dz = b[5] - b[2];
      float radius = (float)Math.sqrt(dx * dx + dy * dy + dz * dz) / 2.0F;
      float tanY = (float)Math.tan(Math.toRadians(24.0) / 2.0);
      float dist;
      stage.baseDistance = dist = radius / (tanY * Math.min(1.0F, aspect)) / 1.2F;
      stage.tanY = tanY;
      stage.aspect = aspect;
      this.model.identity();
      this.model.translate(stage.panX, stage.panY, -dist / Math.max(0.5F, stage.zoom));
      this.model.rotate(stage.pitch, 1.0F, 0.0F, 0.0F);
      this.model.rotate(stage.yaw, 0.0F, 1.0F, 0.0F);
      this.model.rotate(-62.0F, 0.0F, 0.0F, 1.0F);
      this.model.translate(-cx, -cy, -cz);
   }

   private boolean weaponTurntable(WeaponAssets a, int lod, WeaponSettings s, float angle, int w, int h, int glow, boolean pixel) {
      if (a.frame(lod, this.frame) && this.studio.begin(w, h)) {
         backdrop(-13749699, -15461095, -0.05F, -0.1F, 0.95F, 1.2F, glow);
         GlStateManager.clear(256);
         float cx = (this.frame[0] + this.frame[3]) / 2.0F;
         float cy = (this.frame[1] + this.frame[4]) / 2.0F;
         float cz = (this.frame[2] + this.frame[5]) / 2.0F;
         float rx = (this.frame[3] - this.frame[0]) / 2.0F;
         float rz = (this.frame[5] - this.frame[2]) / 2.0F;
         float r = (float)Math.sqrt(rx * rx + rz * rz);
         this.box[0] = -r;
         this.box[1] = this.frame[1] - cy;
         this.box[2] = -r;
         this.box[3] = r;
         this.box[4] = this.frame[4] - cy;
         this.box[5] = r;
         this.turn.identity().rotate(-62.0F, 0.0F, 0.0F, 1.0F);
         this.studio.fit(this.box, this.turn, 20.0F, (float)w / h, 0.86F, this.model);
         this.model.rotate(angle, 0.0F, 1.0F, 0.0F);
         this.model.translate(-cx, -cy, -cz);
         this.studio.light.studio(0.0F, 180.0F);
         WeaponRenderer.Look look = new WeaponRenderer.Look().set(s);
         look.chainPhase = this.module.clientTime() * 0.12F;
         look.pixel = pixel;
         this.studio.draw(a, look, lod, this.model, 20.0F, w, h, this.module.clientTime());
         return true;
      } else {
         return false;
      }
   }

   private boolean itemTurntable(ItemStack stack, float angle, int w, int h, float fov, float fill, int glow) {
      if (!this.studio.begin(w, h)) {
         return false;
      } else {
         backdrop(-13749699, -15461095, -0.05F, -0.1F, 0.95F, 1.2F, glow);
         GlStateManager.clear(256);
         this.turn.identity().rotate(-62.0F, 0.0F, 0.0F, 1.0F);
         this.studio.fit(ITEM_BOX, this.turn, fov, (float)w / h, fill, this.model);
         this.model.rotate(angle, 0.0F, 1.0F, 0.0F);
         this.model.rotate(45.0F, 0.0F, 0.0F, 1.0F);
         this.drawItem(stack, fov, (float)w / h);
         return true;
      }
   }

   private void drawItem(ItemStack stack, float fov, float aspect) {
      Minecraft mc = Minecraft.getMinecraft();
      GL20.glUseProgram(0);
      this.loadPerspective(fov, aspect);
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      RenderHelper.enableGUIStandardItemLighting();
      GlStateManager.multMatrix(this.model.buffer());
      noLightmap();
      GlStateManager.enableRescaleNormal();
      GlStateManager.enableDepth();
      GlStateManager.depthMask(true);
      GlStateManager.enableAlpha();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

      try {
         mc.getRenderItem().renderItem(stack, TransformType.NONE);
      } finally {
         RenderHelper.disableStandardItemLighting();
      }
   }

   private static void begin() {
      GL20.glUseProgram(0);
      GL11.glDisable(3089);
   }

   private void loadPerspective(float fov, float aspect) {
      this.proj.perspective(fov, aspect, 0.05F, 50.0F);
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.multMatrix(this.proj.buffer());
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
   }

   private static void noLightmap() {
      GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
      GlStateManager.disableTexture2D();
      GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
   }

   private static void backdrop(int top, int bottom, float cx, float cy, float rx, float ry, int glow) {
      GL20.glUseProgram(0);
      GlStateManager.matrixMode(5889);
      GlStateManager.loadIdentity();
      GlStateManager.matrixMode(5888);
      GlStateManager.loadIdentity();
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      GlStateManager.disableTexture2D();
      GlStateManager.disableCull();
      GlStateManager.disableAlpha();
      GlStateManager.disableLighting();
      GlStateManager.shadeModel(7425);
      GlStateManager.disableBlend();
      Tessellator t = Tessellator.getInstance();
      WorldRenderer wr = t.getWorldRenderer();
      wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
      color(wr.pos(-1.0, -1.0, 0.0), bottom).endVertex();
      color(wr.pos(1.0, -1.0, 0.0), bottom).endVertex();
      color(wr.pos(1.0, 1.0, 0.0), top).endVertex();
      color(wr.pos(-1.0, 1.0, 0.0), top).endVertex();
      t.draw();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 1, 0, 1);
      wr.begin(6, DefaultVertexFormats.POSITION_COLOR);
      color(wr.pos(cx, cy, 0.0), glow).endVertex();
      int rim = glow & 16777215;

      for (int i = 0; i <= 48; i++) {
         double a = i / 48.0 * Math.PI * 2.0;
         color(wr.pos(cx + Math.cos(a) * rx, cy + Math.sin(a) * ry, 0.0), rim).endVertex();
      }

      t.draw();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.depthMask(true);
      GlStateManager.enableDepth();
   }

   private static void floorShadow() {
      GlStateManager.disableTexture2D();
      GlStateManager.disableCull();
      GlStateManager.disableLighting();
      GlStateManager.depthMask(false);
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
      Tessellator t = Tessellator.getInstance();
      WorldRenderer wr = t.getWorldRenderer();
      wr.begin(6, DefaultVertexFormats.POSITION_COLOR);
      wr.pos(0.0, 0.001, 0.0).color(0, 0, 0, 120).endVertex();

      for (int i = 0; i <= 40; i++) {
         double a = -i / 40.0 * Math.PI * 2.0;
         wr.pos(Math.cos(a) * 0.75, 0.001, Math.sin(a) * 0.75).color(0, 0, 0, 0).endVertex();
      }

      t.draw();
      wr.begin(8, DefaultVertexFormats.POSITION_COLOR);

      for (int var10 = 0; var10 <= 64; var10++) {
         double a = var10 / 64.0 * Math.PI * 2.0;
         double c = Math.cos(a);
         double s = Math.sin(a);
         wr.pos(c * 1.02, 0.002, s * 1.02).color(255, 255, 255, 30).endVertex();
         wr.pos(c * 1.06, 0.002, s * 1.06).color(255, 255, 255, 0).endVertex();
      }

      t.draw();
      GlStateManager.disableBlend();
      GlStateManager.depthMask(true);
      GlStateManager.enableTexture2D();
   }

   private static WorldRenderer color(WorldRenderer wr, int argb) {
      return wr.color(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
   }

   static ResourceLocation skin(Minecraft mc, boolean[] slim) {
      slim[0] = SkinHooks.ownSlim();
      return SkinHooks.ownSkin();
   }

   ItemStack vanillaStack() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      if (p != null) {
         WeaponDef vanilla = this.module.library.vanilla();
         ItemStack held = p.getHeldItem();
         if (held != null && this.module.weaponFor(held) == vanilla) {
            return held;
         }

         for (int i = 0; i < 9; i++) {
            ItemStack s = p.inventory.mainInventory[i];
            if (s != null && this.module.weaponFor(s) == vanilla) {
               return s;
            }
         }

         if (held != null && held.getItem() instanceof ItemSword) {
            return held;
         }
      }

      return DIAMOND_SWORD;
   }

   void release() {
      for (WeaponViews.Atlas at : new WeaponViews.Atlas[]{this.cardAtlas, this.tileAtlas}) {
         GlTarget.delete(at.target);
         at.target = null;
         Arrays.fill(at.owner, null);
         at.rows = 0;
      }

      GlTarget.delete(this.view);
      this.view = null;
      GlTarget.delete(this.compareView);
      this.compareView = null;
      this.studio.release();
      this.fp.release();
   }

   static final class Atlas {
      GlTarget target;
      int cellW;
      int cellH;
      final int cols;
      int rows;
      String[] owner = new String[0];
      boolean[] taken = new boolean[0];
      final boolean[] fresh = new boolean[96];
      long drawn;

      Atlas(int cols) {
         this.cols = cols;
      }

      int cells() {
         return this.cols * this.rows;
      }
   }

   static final class Card {
      WeaponDef def;
      float angle;
      int style = -1;
      int cell = -1;

      String key() {
         return this.style < 0 ? this.def.id : this.def.id + (this.style == 1 ? "|px" : "|hd");
      }
   }

   static final class Stage {
      WeaponViews.Tab tab = WeaponViews.Tab.FIRST_PERSON;
      WeaponViews.Stance stance = WeaponViews.Stance.IDLE;
      float time;
      float yaw;
      float pitch;
      float zoom = 1.0F;
      float panX;
      float panY;
      float orbitYaw = 32.0F;
      float orbitPitch = 8.0F;
      float distance = 4.6F;
      boolean dual;
      boolean blocking;
      boolean compare;
      final float[] splits = new float[]{0.66F, 0.5F, 0.5F};
      int compareTexture = -1;
      boolean compareLoading;
      float vanillaSwing = -1.0F;
      float vanillaDraw = -1.0F;
      boolean loading;
      float baseDistance = 1.0F;
      float tanY = 0.2F;
      float aspect = 1.6F;

      float split() {
         return this.splits[this.tab.ordinal()];
      }

      void split(float at) {
         this.splits[this.tab.ordinal()] = Math.max(0.0F, Math.min(1.0F, at));
      }
   }

   static enum Stance {
      IDLE("Idle"),
      WALK("Walk"),
      SPRINT("Sprint"),
      ATTACK("Attack"),
      BLOCK("Block");

      final String label;

      private Stance(String label) {
         this.label = label;
      }
   }

   static enum Tab {
      FIRST_PERSON("First Person"),
      THIRD_PERSON("Third Person"),
      MODEL("Model");

      final String label;

      private Tab(String label) {
         this.label = label;
      }
   }
}
