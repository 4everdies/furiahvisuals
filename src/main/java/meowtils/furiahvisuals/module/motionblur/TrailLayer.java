package meowtils.furiahvisuals.module.motionblur;

import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.render.GlTarget;
import meowtils.furiahvisuals.render.post.PostFrame;
import meowtils.furiahvisuals.render.post.PostProgram;
import net.minecraft.client.renderer.GlStateManager;

final class TrailLayer {
   private static final String BLEND = "motionblur/trail_blend.fsh";
   private static final String OUT = "motionblur/trail_out.fsh";
   private static final int RGBA16F = 34842;
   private PostProgram blend;
   private PostProgram out;
   private int uBlendSize;
   private int uParams;
   private int uStep;
   private int uOutSize;
   private int uLinear;
   private GlTarget a;
   private GlTarget b;
   private int sizeVersion = -1;
   private boolean high;
   private boolean storedLinear;
   private boolean empty = true;
   private boolean highRefused;
   private boolean triedHigh;

   void run(PostFrame frame, float keep, boolean phosphor, boolean naturalLight, boolean wantHigh, boolean hold) {
      this.prepare(frame, wantHigh);
      boolean linear = naturalLight && this.high;
      if (linear != this.storedLinear) {
         this.storedLinear = linear;
         this.empty = true;
      }

      if (this.empty) {
         hold = false;
      }

      if (!hold) {
         int scene = frame.scene();
         this.b.bind();
         this.blend.use();
         PostProgram.set(this.uBlendSize, frame.width(), frame.height());
         PostProgram.set(this.uParams, this.empty ? 0.0F : keep, phosphor ? 1.0F : 0.0F, linear ? 1.0F : 0.0F, this.empty ? 1.0F : 0.0F);
         PostProgram.set(this.uStep, this.high ? 0.0F : 0.003921569F, this.high ? 9.765625E-4F : 0.0F);
         PostFrame.bind(1, this.a.texture);
         PostFrame.bind(0, scene);
         PostFrame.quad();
         GlTarget t = this.a;
         this.a = this.b;
         this.b = t;
         this.empty = false;
      }

      frame.bindTarget();
      GlStateManager.colorMask(true, true, true, false);
      this.out.use();
      PostProgram.set(this.uOutSize, frame.width(), frame.height());
      PostProgram.set(this.uLinear, linear ? 1.0F : 0.0F);
      PostFrame.bind(0, this.a.texture);
      PostFrame.quad();
      GlStateManager.colorMask(true, true, true, true);
   }

   private void prepare(PostFrame frame, boolean wantHigh) {
      if (this.blend == null) {
         this.blend = PostProgram.linkFile("motion blur trail", "motionblur/trail_blend.fsh", "uCur", "uHist");
         this.uBlendSize = this.blend.uniform("uSize");
         this.uParams = this.blend.uniform("uParams");
         this.uStep = this.blend.uniform("uStep");
         this.out = PostProgram.linkFile("motion blur output", "motionblur/trail_out.fsh", "uHist");
         this.uOutSize = this.out.uniform("uSize");
         this.uLinear = this.out.uniform("uLinear");
      }

      boolean wantNow = wantHigh && !this.highRefused;
      if (this.a != null && (frame.sizeVersion() != this.sizeVersion || wantNow != this.high)) {
         this.deleteTargets();
      }

      if (this.a == null) {
         this.sizeVersion = frame.sizeVersion();
         this.high = false;
         if (wantNow) {
            this.a = target(frame, true);
            GlTarget glTarget = this.b = this.a == null ? null : target(frame, true);
            if (this.a != null && this.b != null) {
               this.high = true;
            } else {
               GlTarget.delete(this.a);
               GlTarget.delete(this.b);
               this.a = null;
               this.b = null;
               this.highRefused = true;
               FuriahsVisuals.LOG.info("Motion Blur: high-precision (16-bit float) history refused by the driver; Standard is used");
            }

            this.triedHigh = true;
         }

         if (this.a == null) {
            this.a = target(frame, false);
            GlTarget glTarget = this.b = this.a == null ? null : target(frame, false);
            if (this.a == null || this.b == null) {
               GlTarget.delete(this.a);
               GlTarget.delete(this.b);
               this.a = null;
               this.b = null;
               throw new IllegalStateException("the history framebuffer is incomplete");
            }
         }

         this.empty = true;
      }
   }

   private static GlTarget target(PostFrame frame, boolean high) {
      return high && !PostFrame.floatTextures() ? null : GlTarget.create(frame.width(), frame.height(), high ? '蠚' : '聘', 6408, false, false);
   }

   void reset() {
      this.empty = true;
   }

   boolean high() {
      return this.high;
   }

   boolean highRefused() {
      return this.highRefused && this.triedHigh;
   }

   private void deleteTargets() {
      GlTarget.delete(this.a);
      GlTarget.delete(this.b);
      this.a = null;
      this.b = null;
      this.empty = true;
   }

   void release() {
      this.deleteTargets();
   }

   boolean allocated() {
      return this.a != null;
   }
}
