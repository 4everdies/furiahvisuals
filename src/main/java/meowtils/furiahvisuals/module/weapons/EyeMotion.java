package meowtils.furiahvisuals.module.weapons;

import java.util.Map;
import java.util.Random;

final class EyeMotion {
   private static final int FOLLOW = 0;
   private static final int HOLDER = 1;
   private static final int AROUND = 2;
   private static final float SPRING = 34.0F;
   private static final float CLOSING = 0.07F;
   private static final float SHUT = 0.03F;
   private static final float OPENING = 0.12F;
   private static final float PX_STEP = 0.045F;
   private static final float PX_FRAME = 0.05F;
   private static final float[][] AROUND_LOOKS = new float[][]{{-42.0F, 6.0F}, {42.0F, 0.0F}, {0.0F, 26.0F}};
   private static final float AROUND_EACH = 0.42F;
   float yaw;
   float pitch;
   private float yawVel;
   private float pitchVel;
   float lids = 1.0F;
   private float open = 1.0F;
   private float openTarget = 1.0F;
   private int mode = 0;
   private float modeT;
   private float blinkIn = 2.5F;
   private float blinkT = -1.0F;
   private boolean again;
   private float glanceIn = 3.0F;
   private float glanceLeft;
   private float glanceYaw;
   private float glancePitch;
   private boolean glanceHolder;
   final float[] pixel = new float[]{0.0F, 0.0F, 0.0F, 1.0F};
   private float pxStepT;
   private float pxFrameT;
   private boolean fresh = true;
   private boolean snapFrame;
   private final Random random = new Random();

   void reset() {
      this.fresh = true;
      this.lids = 1.0F;
      this.openTarget = 1.0F;
      this.open = 1.0F;
      this.mode = 0;
      this.blinkT = -1.0F;
      this.glanceLeft = 0.0F;
      this.snapFrame = true;
   }

   void event(String what) {
      if ("shut".equals(what)) {
         this.openTarget = 0.0F;
         this.open = 0.0F;
         this.blinkT = -1.0F;
         this.snapFrame = true;
      } else if ("open".equals(what)) {
         this.openTarget = 1.0F;
      } else if ("blink".equals(what)) {
         this.blinkT = 0.0F;
         this.again = false;
      } else if ("me".equals(what)) {
         this.mode = 1;
         this.glanceLeft = 0.0F;
      } else if ("around".equals(what)) {
         this.mode = 2;
         this.modeT = 0.0F;
         this.glanceLeft = 0.0F;
      } else if ("forward".equals(what)) {
         this.mode = 0;
      }
   }

   void update(float dt, float[] look, float[] holder, boolean glances, int[] pixels) {
      dt = Math.max(0.0F, Math.min(dt, 0.1F));
      float[] base = this.mode != 0 && holder != null ? holder : look;
      float ty = yawOf(base);
      float tp = pitchOf(base);
      if (this.mode == 2) {
         this.modeT += dt;
         int k = (int)(this.modeT / 0.42F);
         if (k >= AROUND_LOOKS.length) {
            this.mode = holder != null ? 1 : 0;
         } else {
            ty += AROUND_LOOKS[k][0];
            tp += AROUND_LOOKS[k][1];
         }
      }

      if (this.mode == 0 && glances) {
         if (this.glanceLeft > 0.0F) {
            this.glanceLeft -= dt;
         } else if ((this.glanceIn -= dt) <= 0.0F) {
            this.glanceIn = 2.5F + this.random.nextFloat() * 4.5F;
            this.glanceLeft = 0.45F + this.random.nextFloat() * 0.6F;
            this.glanceHolder = holder != null && this.random.nextFloat() < 0.15F;
            this.glanceYaw = (25.0F + this.random.nextFloat() * 25.0F) * (this.random.nextBoolean() ? 1.0F : -1.0F);
            this.glancePitch = -6.0F + this.random.nextFloat() * 24.0F;
         }

         if (this.glanceLeft > 0.0F) {
            if (this.glanceHolder) {
               ty = yawOf(holder);
               tp = pitchOf(holder);
            } else {
               ty += this.glanceYaw;
               tp += this.glancePitch;
            }
         }
      } else {
         this.glanceLeft = 0.0F;
      }

      tp = Math.max(-60.0F, Math.min(60.0F, tp));
      if (this.fresh) {
         this.yaw = ty;
         this.pitch = tp;
         this.pitchVel = 0.0F;
         this.yawVel = 0.0F;
      }

      ty += 360.0F * Math.round((this.yaw - ty) / 360.0F);
      int n = Math.max(1, (int)Math.ceil(dt / 0.008F));
      float h = dt / n;

      for (int i = 0; i < n; i++) {
         this.yawVel = this.yawVel + (1156.0F * (ty - this.yaw) - 68.0F * this.yawVel) * h;
         this.yaw = this.yaw + this.yawVel * h;
         this.pitchVel = this.pitchVel + (1156.0F * (tp - this.pitch) - 68.0F * this.pitchVel) * h;
         this.pitch = this.pitch + this.pitchVel * h;
      }

      if (Math.abs(this.yaw) > 540.0F) {
         this.yaw = this.yaw - 360.0F * Math.round(this.yaw / 360.0F);
      }

      this.open = this.openTarget > this.open ? Math.min(this.openTarget, this.open + dt / 0.3F) : this.openTarget;
      float blink = 1.0F;
      if (this.blinkT >= 0.0F) {
         this.blinkT += dt;
         blink = this.blinkT < 0.07F ? 1.0F - this.blinkT / 0.07F : (this.blinkT < 0.1F ? 0.0F : Math.min(1.0F, (this.blinkT - 0.07F - 0.03F) / 0.12F));
         if (this.blinkT >= 0.22F) {
            this.blinkT = this.again ? 0.0F : -1.0F;
            this.again = false;
         }
      } else if (this.open > 0.99F && (this.blinkIn -= dt) <= 0.0F) {
         this.blinkIn = 2.5F + this.random.nextFloat() * 4.0F;
         this.blinkT = 0.0F;
         this.again = this.random.nextFloat() < 0.2F;
      }

      this.lids = Math.min(this.open, blink);
      if (pixels != null) {
         this.stepPixels(dt, pixels);
      }

      this.fresh = false;
   }

   private void stepPixels(float dt, int[] pixels) {
      float yr = (float)Math.toRadians(this.yaw);
      float pr = (float)Math.toRadians(this.pitch);
      float dx = (float)(Math.sin(yr) * Math.cos(pr));
      float dy = (float)Math.sin(pr);
      float dz = (float)(Math.cos(yr) * Math.cos(pr));
      int tx = Math.round(Math.max((float)(-pixels[0]), Math.min((float)pixels[0], dx * 2.2F)));
      int ty = Math.round(Math.max((float)(-pixels[1]), Math.min((float)pixels[1], dy * 2.2F)));
      int frame = Math.round((1.0F - this.lids) * pixels[2]);
      if (this.fresh || (this.pixel[3] > 0.0F ? dz < -0.88F : dz > -0.82F)) {
         float var11 = this.pixel[3] = dz < -0.85F ? -1.0F : 1.0F;
      }

      if (this.fresh) {
         this.pixel[0] = tx;
         this.pixel[1] = ty;
         this.pixel[2] = frame;
         this.snapFrame = false;
      } else {
         if (this.snapFrame) {
            this.pixel[2] = frame;
            this.snapFrame = false;
         }

         this.pxStepT = Math.min(0.045F, this.pxStepT + dt);
         if (this.pxStepT >= 0.045F && (this.pixel[0] != tx || this.pixel[1] != ty)) {
            if (this.pixel[0] != tx) {
               this.pixel[0] = this.pixel[0] + Math.signum(tx - this.pixel[0]);
            } else {
               this.pixel[1] = this.pixel[1] + Math.signum(ty - this.pixel[1]);
            }

            this.pxStepT = 0.0F;
         }

         this.pxFrameT = Math.min(0.05F, this.pxFrameT + dt);
         if (this.pxFrameT >= 0.05F && this.pixel[2] != frame) {
            this.pixel[2] = this.pixel[2] + Math.signum(frame - this.pixel[2]);
            this.pxFrameT = 0.0F;
         }
      }
   }

   void poses(WeaponDef.Eye e, Map<String, float[]> parts) {
      poses(e, this.yaw, this.pitch, this.lids, parts);
   }

   static void poses(WeaponDef.Eye e, float yaw, float pitch, float lids, Map<String, float[]> parts) {
      put(parts, e.gem, yaw);
      put(parts, e.iris, -pitch);
      put(parts, e.pupil, -pitch);
      put(parts, e.upper, -pitch + e.close * (1.0F - lids));
      put(parts, e.lower, -pitch - e.close * (1.0F - lids));
   }

   private static void put(Map<String, float[]> parts, String name, float angle) {
      if (name != null) {
         float[] clip = parts.get(name);
         float[] fArray;
         if (clip == null) {
            float[] fArray2 = new float[]{angle, 0.0F, 1.0F, 0.0F};
            fArray = fArray2;
            fArray2[3] = 1.0F;
         } else {
            float[] fArray3 = new float[4];
            fArray3[0] = clip[0] + angle;
            fArray3[1] = clip[1];
            fArray3[2] = clip[2];
            fArray = fArray3;
            fArray3[3] = clip[3];
         }

         parts.put(name, fArray);
      }
   }

   static void still(WeaponDef.Eye e, float[] look, double time, int seed, Map<String, float[]> parts, float[] px) {
      float yaw = yawOf(look);
      float pitch = pitchOf(look);
      double w = time / 3.5 + (seed & 1023) / 97.0;
      long win = (long)Math.floor(w);
      double f = w - win;
      if (hash(win, seed) < 0.5F && f > 0.3 && f < 0.5) {
         yaw += (hash(win, seed + 7) - 0.5F) * 80.0F;
         pitch += (hash(win, seed + 13) - 0.5F) * 30.0F;
      }

      pitch = Math.max(-60.0F, Math.min(60.0F, pitch));
      double b = (time + (seed & 0xFF) / 61.0) % 4.1;
      float lids = b < 0.07F ? 1.0F - (float)(b / 0.07F) : (b < 0.1F ? 0.0F : (b < 0.22F ? (float)((b - 0.07F - 0.03F) / 0.12F) : 1.0F));
      poses(e, yaw, pitch, lids, parts);
      float yr = (float)Math.toRadians(yaw);
      float pr = (float)Math.toRadians(pitch);
      float dx = (float)(Math.sin(yr) * Math.cos(pr));
      float dy = (float)Math.sin(pr);
      float dz = (float)(Math.cos(yr) * Math.cos(pr));
      px[0] = Math.round(Math.max((float)(-e.pixels[0]), Math.min((float)e.pixels[0], dx * 2.2F)));
      px[1] = Math.round(Math.max((float)(-e.pixels[1]), Math.min((float)e.pixels[1], dy * 2.2F)));
      px[2] = Math.round((1.0F - lids) * e.pixels[2]);
      px[3] = dz < -0.85F ? -1.0F : 1.0F;
   }

   static float yawOf(float[] d) {
      return (float)Math.toDegrees(Math.atan2(d[0], d[2]));
   }

   static float pitchOf(float[] d) {
      return (float)Math.toDegrees(Math.asin(Math.max(-1.0F, Math.min(1.0F, d[1]))));
   }

   private static float hash(long a, int b) {
      long h = a * 374761393L + b * 668265263L;
      h = (h ^ h >>> 13) * 1274126177L;
      h ^= h >>> 16;
      return (float)(h & 16777215L) / 1.6777215E7F;
   }
}
