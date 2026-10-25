package meowtils.furiahvisuals.module.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

final class Pose {
   final float[] rPos = new float[3];
   final float[] rRot = new float[]{0.0F, 0.0F, 0.0F, 1.0F};
   final float[] wPos = new float[3];
   final float[] wRot = new float[]{0.0F, 0.0F, 0.0F, 1.0F};
   final float[] lPos = new float[3];
   final float[] lRot = new float[]{0.0F, 0.0F, 0.0F, 1.0F};
   float lShow;
   float lFree;
   String lAtPart;
   float lAt;
   boolean released;
   boolean weaponInView;
   float glint = -1.0F;
   float reveal = -1.0F;
   float flare;
   float bend;
   float[] lAlongFrom;
   float[] lAlongTo;
   float lAlong;
   float lAlongHold;
   final Map<String, float[]> parts = new HashMap<>();
   private static final float[] ID = new float[]{0.0F, 0.0F, 0.0F, 1.0F};
   private static final float[] TMP = new float[4];

   Pose identity() {
      clear(this.rPos);
      clear(this.wPos);
      clear(this.lPos);
      Mat4.qidentity(this.rRot);
      Mat4.qidentity(this.wRot);
      Mat4.qidentity(this.lRot);
      this.lShow = 0.0F;
      this.lFree = 0.0F;
      this.lAtPart = null;
      this.lAt = 0.0F;
      this.released = false;
      this.weaponInView = false;
      this.glint = -1.0F;
      this.reveal = -1.0F;
      this.flare = 0.0F;
      this.bend = 0.0F;
      this.lAlongTo = null;
      this.lAlongFrom = null;
      this.lAlong = 0.0F;
      this.lAlongHold = 0.0F;
      this.parts.clear();
      return this;
   }

   private static void clear(float[] v) {
      v[2] = 0.0F;
      v[1] = 0.0F;
      v[0] = 0.0F;
   }

   Pose set(Pose o) {
      System.arraycopy(o.rPos, 0, this.rPos, 0, 3);
      System.arraycopy(o.wPos, 0, this.wPos, 0, 3);
      System.arraycopy(o.lPos, 0, this.lPos, 0, 3);
      System.arraycopy(o.rRot, 0, this.rRot, 0, 4);
      System.arraycopy(o.wRot, 0, this.wRot, 0, 4);
      System.arraycopy(o.lRot, 0, this.lRot, 0, 4);
      this.lShow = o.lShow;
      this.lFree = o.lFree;
      this.lAtPart = o.lAtPart;
      this.lAt = o.lAt;
      this.released = o.released;
      this.weaponInView = o.weaponInView;
      this.glint = o.glint;
      this.reveal = o.reveal;
      this.flare = o.flare;
      this.bend = o.bend;
      this.lAlongFrom = o.lAlongFrom;
      this.lAlongTo = o.lAlongTo;
      this.lAlong = o.lAlong;
      this.lAlongHold = o.lAlongHold;
      this.parts.clear();

      for (Entry<String, float[]> e : o.parts.entrySet()) {
         this.parts.put(e.getKey(), (float[])e.getValue().clone());
      }

      return this;
   }

   Pose add(Pose o, float w) {
      if (w <= 0.0F) {
         return this;
      } else {
         for (int i = 0; i < 3; i++) {
            this.rPos[i] = this.rPos[i] + o.rPos[i] * w;
            this.wPos[i] = this.wPos[i] + o.wPos[i] * w;
            this.lPos[i] = this.lPos[i] + o.lPos[i] * w;
         }

         addRot(this.rRot, o.rRot, w);
         addRot(this.wRot, o.wRot, w);
         addRot(this.lRot, o.lRot, w);
         this.lShow = Math.max(this.lShow, o.lShow * w);
         this.lFree = Math.max(this.lFree, o.lFree * w);
         if (o.lAtPart != null && o.lAt * w > this.lAt) {
            this.lAtPart = o.lAtPart;
            this.lAt = o.lAt * w;
         }

         this.released = this.released | (o.released && w > 0.5F);
         if (w > 0.5F && o.weaponInView) {
            this.weaponInView = true;
         }

         if (o.glint >= 0.0F && w > 0.3F) {
            this.glint = o.glint;
         }

         if (o.reveal >= 0.0F && w > 0.3F) {
            this.reveal = o.reveal;
         }

         this.flare = o.flare < 0.0F ? Math.min(this.flare, o.flare * w) : Math.max(this.flare, o.flare * w);
         this.bend = this.bend + o.bend * w;
         if (o.lAlongFrom != null && o.lAlongHold * w > this.lAlongHold) {
            this.lAlongFrom = o.lAlongFrom;
            this.lAlongTo = o.lAlongTo;
            this.lAlong = o.lAlong;
            this.lAlongHold = o.lAlongHold * w;
         }

         for (Entry<String, float[]> e : o.parts.entrySet()) {
            float[] mine = this.parts.get(e.getKey());
            float[] theirs = e.getValue();
            if (mine == null) {
               mine = new float[]{0.0F, 0.0F, 1.0F, 1.0F};
               this.parts.put(e.getKey(), mine);
            }

            mine[0] += theirs[0] * w;
            mine[1] += theirs[1] * w;
            mine[2] += (theirs[2] - 1.0F) * w;
            mine[3] = w > 0.5F ? theirs[3] : mine[3];
         }

         return this;
      }
   }

   Pose blend(Pose o, float k) {
      for (int i = 0; i < 3; i++) {
         this.rPos[i] = this.rPos[i] + (o.rPos[i] - this.rPos[i]) * k;
         this.wPos[i] = this.wPos[i] + (o.wPos[i] - this.wPos[i]) * k;
         this.lPos[i] = this.lPos[i] + (o.lPos[i] - this.lPos[i]) * k;
      }

      Mat4.slerp(this.rRot, o.rRot, k, this.rRot);
      Mat4.slerp(this.wRot, o.wRot, k, this.wRot);
      Mat4.slerp(this.lRot, o.lRot, k, this.lRot);
      this.lShow = this.lShow + (o.lShow - this.lShow) * k;
      this.lFree = this.lFree + (o.lFree - this.lFree) * k;
      if (o.lAtPart != null || this.lAtPart != null) {
         this.lAtPart = o.lAtPart != null ? o.lAtPart : this.lAtPart;
         this.lAt = this.lAt + ((o.lAtPart != null ? o.lAt : 0.0F) - this.lAt) * k;
      }

      this.released = k > 0.5F ? o.released : this.released;
      this.weaponInView = k > 0.5F ? o.weaponInView : this.weaponInView;
      this.glint = k > 0.5F ? o.glint : this.glint;
      this.reveal = k > 0.5F ? o.reveal : this.reveal;
      this.flare = this.flare + (o.flare - this.flare) * k;
      this.bend = this.bend + (o.bend - this.bend) * k;
      if (o.lAlongFrom != null || this.lAlongFrom != null) {
         if (o.lAlongFrom != null) {
            this.lAlongFrom = o.lAlongFrom;
            this.lAlongTo = o.lAlongTo;
            this.lAlong = this.lAlong + (o.lAlong - this.lAlong) * k;
         }

         this.lAlongHold = this.lAlongHold + ((o.lAlongFrom != null ? o.lAlongHold : 0.0F) - this.lAlongHold) * k;
      }

      for (Entry<String, float[]> e : o.parts.entrySet()) {
         float[] mine = this.parts.get(e.getKey());
         if (mine == null) {
            mine = new float[]{0.0F, 0.0F, 1.0F, 1.0F};
            this.parts.put(e.getKey(), mine);
         }

         float[] theirs = e.getValue();

         for (int i = 0; i < 3; i++) {
            mine[i] += (theirs[i] - mine[i]) * k;
         }

         mine[3] = k > 0.5F ? theirs[3] : mine[3];
      }

      return this;
   }

   private static void addRot(float[] rot, float[] add, float w) {
      if (w >= 1.0F) {
         Mat4.qmul(rot, add, rot);
      } else {
         Mat4.slerp(ID, add, w, TMP);
         Mat4.qmul(rot, TMP, rot);
      }
   }

   static float angleOf(float[] q) {
      float w = Math.min(1.0F, Math.abs(q[3]));
      return (float)Math.toDegrees(2.0 * Math.acos(w));
   }
}
