package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class Clip {
   final String name;
   final float length;
   final boolean loop;
   final float dirX;
   final float dirY;
   final boolean weaponInView;
   final Clip.Track right;
   final Clip.Track weapon;
   final Clip.Track left;
   final Clip.Curve leftShow;
   final Clip.Curve leftFree;
   final String leftAtPart;
   final Clip.Curve leftAt;
   final Clip.Curve glint;
   final Clip.Curve reveal;
   final Clip.Curve flare;
   final Clip.Curve bend;
   final float[] leftAlongFrom;
   final float[] leftAlongTo;
   final Clip.Curve leftAlong;
   final Clip.Curve leftAlongHold;
   final Map<String, Clip.Curve[]> parts = new LinkedHashMap<>();
   final List<Clip.Event> events = new ArrayList<>();

   Clip(String name, JsonObject o) {
      this.name = name;
      this.length = (float)Math.max(0.02, WeaponDef.num(o, "length", 1.0));
      this.loop = WeaponDef.bool(o, "loop", false);
      float[] dir = WeaponDef.floats(o, "dir", 2);
      float dl = dir == null ? 0.0F : (float)Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1]);
      this.dirX = dl > 1.0E-4F ? dir[0] / dl : 0.0F;
      this.dirY = dl > 1.0E-4F ? dir[1] / dl : 0.0F;
      this.weaponInView = "view".equalsIgnoreCase(WeaponDef.str(o, "weaponSpace", "fist"));
      this.right = this.track(o.get("right"), true);
      this.weapon = this.track(o.get("weapon"), false);
      this.left = this.track(o.get("left"), true);
      this.leftShow = curve(o.get("leftShow"), 1);
      this.leftFree = curve(o.get("leftFree"), 1);
      JsonElement la = o.get("leftAt");
      if (la != null && la.isJsonObject()) {
         this.leftAtPart = WeaponDef.str(la.getAsJsonObject(), "part", null);
         this.leftAt = curve(la.getAsJsonObject().get("keys"), 1);
      } else {
         this.leftAtPart = null;
         this.leftAt = new Clip.Curve(0);
      }

      this.glint = curve(o.get("glint"), 1);
      this.reveal = curve(o.get("reveal"), 1);
      this.flare = curve(o.get("flare"), 1);
      this.bend = curve(o.get("bend"), 1);
      JsonElement lal = o.get("leftAlong");
      if (lal != null && lal.isJsonObject()) {
         JsonObject la2 = lal.getAsJsonObject();
         this.leftAlongFrom = WeaponDef.floats(la2, "from", 3);
         this.leftAlongTo = WeaponDef.floats(la2, "to", 3);
         this.leftAlong = curve(la2.get("along"), 1);
         this.leftAlongHold = curve(la2.get("hold"), 1);
      } else {
         this.leftAlongTo = null;
         this.leftAlongFrom = null;
         this.leftAlong = this.leftAlongHold = new Clip.Curve(0);
      }

      JsonElement pe = o.get("parts");
      if (pe != null && pe.isJsonObject()) {
         for (Entry e : pe.getAsJsonObject().entrySet()) {
            this.parts
               .put(
                  (String)e.getKey(),
                  new Clip.Curve[]{curve((JsonElement)e.getValue(), 1), curve((JsonElement)e.getValue(), 2), curveOr((JsonElement)e.getValue(), 3, 1.0F)}
               );
         }
      }

      JsonElement ev;
      if ((ev = o.get("events")) != null && ev.isJsonArray()) {
         for (JsonElement x : ev.getAsJsonArray()) {
            JsonArray a = x.getAsJsonArray();
            this.events.add(new Clip.Event(a.get(0).getAsFloat(), a.get(1).getAsString(), a.size() > 2 ? a.get(2).getAsString() : null));
         }
      }
   }

   void sample(float u, Pose out) {
      this.sample(u, out, null, null);
   }

   void sample(float u, Pose out, float[] holdPos, float[] holdRot) {
      out.identity();
      this.right.sample(u, out.rPos, out.rRot, holdPos, holdRot);
      this.weapon.sample(u, out.wPos, out.wRot, null, null);
      this.left.sample(u, out.lPos, out.lRot, null, null);
      out.lShow = this.leftShow.t.length == 0 ? 0.0F : this.leftShow.sample(u);
      out.lFree = this.leftFree.t.length == 0 ? 0.0F : this.leftFree.sample(u);
      out.lAtPart = this.leftAtPart;
      out.lAt = this.leftAtPart != null && this.leftAt.t.length != 0 ? this.leftAt.sample(u) : 0.0F;
      float g = this.glint.t.length == 0 ? -1.0F : this.glint.sample(u);
      out.glint = g >= 0.0F && g <= 1.0F ? g : -1.0F;
      float rv = this.reveal.t.length == 0 ? -1.0F : this.reveal.sample(u);
      out.reveal = rv >= 0.0F && rv <= 1.0F ? rv : -1.0F;
      out.flare = this.flare.t.length == 0 ? 0.0F : Math.max(-1.0F, this.flare.sample(u));
      float f = out.bend = this.bend.t.length == 0 ? 0.0F : this.bend.sample(u);
      if (this.leftAlongFrom != null && this.leftAlongTo != null && this.leftAlongHold.t.length > 0) {
         out.lAlongFrom = this.leftAlongFrom;
         out.lAlongTo = this.leftAlongTo;
         out.lAlong = this.leftAlong.t.length == 0 ? 0.0F : this.leftAlong.sample(u);
         out.lAlongHold = Math.max(0.0F, Math.min(1.0F, this.leftAlongHold.sample(u)));
      }

      for (Entry<String, Clip.Curve[]> e : this.parts.entrySet()) {
         Clip.Curve[] c = e.getValue();
         out.parts.put(e.getKey(), new float[]{c[0].sample(u), c[1].sample(u), c[2].t.length == 0 ? 1.0F : c[2].sample(u), 1.0F});
      }

      boolean released = false;

      for (Clip.Event ev : this.events) {
         if (ev.t > u) {
            break;
         }

         if ("release".equals(ev.type)) {
            released = true;
         } else if ("catch".equals(ev.type)) {
            released = false;
         }
      }

      out.released = released;
      out.weaponInView = this.weaponInView;
   }

   Clip.Event eventBetween(float from, float to, String type) {
      for (Clip.Event ev : this.events) {
         if (ev.t > from && ev.t <= to && (type == null || type.equals(ev.type))) {
            return ev;
         }
      }

      return null;
   }

   private Clip.Track track(JsonElement e, boolean viewSpace) {
      if (e != null && e.isJsonArray()) {
         JsonArray a = e.getAsJsonArray();
         Clip.Track tr = new Clip.Track(a.size());

         for (int i = 0; i < a.size(); i++) {
            if (a.get(i).isJsonObject()) {
               JsonObject k = a.get(i).getAsJsonObject();
               tr.t[i] = (float)WeaponDef.num(k, "t", 0.0);
               Object pos = WeaponDef.floats(k, "pos", 3);
               float[] tip = WeaponDef.floats(k, "tip", 3);
               float[] edge = WeaponDef.floats(k, "edge", 3);
               if (pos == null || tip == null || edge == null) {
                  throw new IllegalArgumentException("an exact key needs pos, tip and edge");
               }

               System.arraycopy(pos, 0, tr.pos[i], 0, 3);
               float[] x = new float[3];
               float[] y = new float[3];
               float[] z = new float[3];
               Rig.basis(tip, edge, y, x, z);
               Mat4.fromBasis(x, y, z, tr.rot[i]);
               tr.abs[i] = viewSpace;
               tr.ease[i] = Clip.Ease.of(WeaponDef.str(k, "ease", "inOutSine"));
            } else {
               JsonArray ka = a.get(i).getAsJsonArray();
               tr.t[i] = ka.get(0).getAsFloat();
               JsonArray posA = ka.get(1).getAsJsonArray();
               JsonArray rot = ka.get(2).getAsJsonArray();

               for (int j = 0; j < 3; j++) {
                  tr.pos[i][j] = posA.get(j).getAsFloat();
               }

               Mat4.euler(rot.get(0).getAsFloat(), rot.get(1).getAsFloat(), rot.get(2).getAsFloat(), tr.rot[i]);
               tr.ease[i] = Clip.Ease.of(ka.size() > 3 ? ka.get(3).getAsString() : "inOutSine");
            }
         }

         return tr;
      } else {
         return new Clip.Track(0);
      }
   }

   private static Clip.Curve curve(JsonElement e, int index) {
      if (e != null && e.isJsonArray()) {
         JsonArray a = e.getAsJsonArray();
         Clip.Curve c = new Clip.Curve(a.size());

         for (int i = 0; i < a.size(); i++) {
            JsonArray k = a.get(i).getAsJsonArray();
            c.t[i] = k.get(0).getAsFloat();
            c.v[i] = k.size() > index && k.get(index).isJsonPrimitive() && k.get(index).getAsJsonPrimitive().isNumber() ? k.get(index).getAsFloat() : 0.0F;
            String ease = "inOutSine";

            for (int j = 1; j < k.size(); j++) {
               if (k.get(j).isJsonPrimitive() && k.get(j).getAsJsonPrimitive().isString()) {
                  ease = k.get(j).getAsString();
               }
            }

            c.ease[i] = Clip.Ease.of(ease);
         }

         return c;
      } else {
         return new Clip.Curve(0);
      }
   }

   private static Clip.Curve curveOr(JsonElement e, int index, float fallback) {
      Clip.Curve c = curve(e, index);
      if (c.t.length > 0) {
         JsonArray a = e.getAsJsonArray();

         for (int i = 0; i < a.size(); i++) {
            JsonArray k = a.get(i).getAsJsonArray();
            if (k.size() <= index || !k.get(index).isJsonPrimitive() || !k.get(index).getAsJsonPrimitive().isNumber()) {
               c.v[i] = fallback;
            }
         }
      }

      return c;
   }

   private static float catmull(float p0, float p1, float p2, float p3, float s) {
      float s2 = s * s;
      float s3 = s2 * s;
      return 0.5F * (2.0F * p1 + (-p0 + p2) * s + (2.0F * p0 - 5.0F * p1 + 4.0F * p2 - p3) * s2 + (-p0 + 3.0F * p1 - 3.0F * p2 + p3) * s3);
   }

   static final class Curve {
      final float[] t;
      final float[] v;
      final int[] ease;

      Curve(int n) {
         this.t = new float[n];
         this.v = new float[n];
         this.ease = new int[n];
      }

      float sample(float u) {
         int n = this.t.length;
         if (n == 0) {
            return 0.0F;
         } else if (u <= this.t[0] || n == 1) {
            return this.v[0];
         } else if (u >= this.t[n - 1]) {
            return this.v[n - 1];
         } else {
            int i = 0;

            while (i < n - 2 && u > this.t[i + 1]) {
               i++;
            }

            float s = Clip.Ease.apply(this.ease[i + 1], (u - this.t[i]) / Math.max(1.0E-6F, this.t[i + 1] - this.t[i]));
            return this.v[i] + (this.v[i + 1] - this.v[i]) * s;
         }
      }
   }

   static final class Ease {
      static final String[] NAMES = new String[]{
         "linear",
         "inSine",
         "outSine",
         "inOutSine",
         "inCubic",
         "outCubic",
         "inOutCubic",
         "inQuart",
         "outQuart",
         "inOutQuart",
         "outBack",
         "inBack",
         "inOutBack",
         "step"
      };
      private static final List<String> UNKNOWN = new ArrayList<>();

      static int of(String name) {
         for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equalsIgnoreCase(name)) {
               return i;
            }
         }

         List<String> list = UNKNOWN;
         synchronized (list) {
            if (!UNKNOWN.contains(name)) {
               UNKNOWN.add(name);
            }

            return 3;
         }
      }

      static List<String> takeUnknown() {
         List<String> list = UNKNOWN;
         synchronized (list) {
            ArrayList<String> out = new ArrayList<>(UNKNOWN);
            UNKNOWN.clear();
            return out;
         }
      }

      static float apply(int ease, float s) {
         s = s < 0.0F ? 0.0F : (s > 1.0F ? 1.0F : s);
         switch (ease) {
            case 0:
               return s;
            case 1:
               return 1.0F - (float)Math.cos(s * Math.PI / 2.0);
            case 2:
               return (float)Math.sin(s * Math.PI / 2.0);
            case 3:
               return 0.5F - 0.5F * (float)Math.cos(s * Math.PI);
            case 4:
               return s * s * s;
            case 5: {
               float r = 1.0F - s;
               return 1.0F - r * r * r;
            }
            case 6:
               return s < 0.5F ? 4.0F * s * s * s : 1.0F - (float)Math.pow(-2.0F * s + 2.0F, 3.0) / 2.0F;
            case 7:
               return s * s * s * s;
            case 8: {
               float r = 1.0F - s;
               return 1.0F - r * r * r * r;
            }
            case 9:
               return s < 0.5F ? 8.0F * s * s * s * s : 1.0F - (float)Math.pow(-2.0F * s + 2.0F, 4.0) / 2.0F;
            case 10: {
               float c1 = 1.70158F;
               float c3 = c1 + 1.0F;
               float r = s - 1.0F;
               return 1.0F + c3 * r * r * r + c1 * r * r;
            }
            case 11: {
               float c1 = 1.70158F;
               float c3 = c1 + 1.0F;
               return c3 * s * s * s - c1 * s * s;
            }
            case 12: {
               float c1 = 1.70158F;
               float c2 = c1 * 1.525F;
               return s < 0.5F
                  ? (float)(Math.pow(2.0F * s, 2.0) * ((c2 + 1.0F) * 2.0F * s - c2)) / 2.0F
                  : (float)(Math.pow(2.0F * s - 2.0F, 2.0) * ((c2 + 1.0F) * (s * 2.0F - 2.0F) + c2) + 2.0) / 2.0F;
            }
            default:
               return s >= 1.0F ? 1.0F : 0.0F;
         }
      }
   }

   static final class Event {
      final float t;
      final String type;
      final String arg;

      Event(float t, String type, String arg) {
         this.t = t;
         this.type = type;
         this.arg = arg;
      }
   }

   static final class Track {
      final float[] t;
      final float[][] pos;
      final float[][] rot;
      final int[] ease;
      final boolean[] abs;
      private final float[][] dp = new float[4][3];
      private final float[][] dr = new float[4][4];
      private final float[] inv = new float[4];

      Track(int n) {
         this.t = new float[n];
         this.pos = new float[n][3];
         this.rot = new float[n][4];
         this.ease = new int[n];
         this.abs = new boolean[n];
      }

      private void delta(int k, float[] holdPos, float[] holdRot, float[] outPos, float[] outRot) {
         if (this.abs[k] && holdPos != null) {
            outPos[0] = this.pos[k][0] - holdPos[0];
            outPos[1] = this.pos[k][1] - holdPos[1];
            outPos[2] = this.pos[k][2] - holdPos[2];
            Mat4.qconj(holdRot, this.inv);
            Mat4.qmul(this.rot[k], this.inv, outRot);
         } else {
            System.arraycopy(this.pos[k], 0, outPos, 0, 3);
            System.arraycopy(this.rot[k], 0, outRot, 0, 4);
         }
      }

      void sample(float u, float[] outPos, float[] outRot, float[] holdPos, float[] holdRot) {
         int n = this.t.length;
         if (n == 0) {
            outPos[2] = 0.0F;
            outPos[1] = 0.0F;
            outPos[0] = 0.0F;
            Mat4.qidentity(outRot);
         } else if (u <= this.t[0] || n == 1) {
            this.delta(0, holdPos, holdRot, outPos, outRot);
         } else if (u >= this.t[n - 1]) {
            this.delta(n - 1, holdPos, holdRot, outPos, outRot);
         } else {
            int i = 0;

            while (i < n - 2 && u > this.t[i + 1]) {
               i++;
            }

            float span = Math.max(1.0E-6F, this.t[i + 1] - this.t[i]);
            float s = Clip.Ease.apply(this.ease[i + 1], (u - this.t[i]) / span);
            this.delta(Math.max(0, i - 1), holdPos, holdRot, this.dp[0], this.dr[0]);
            this.delta(i, holdPos, holdRot, this.dp[1], this.dr[1]);
            this.delta(i + 1, holdPos, holdRot, this.dp[2], this.dr[2]);
            this.delta(Math.min(n - 1, i + 2), holdPos, holdRot, this.dp[3], this.dr[3]);

            for (int k = 0; k < 3; k++) {
               outPos[k] = Clip.catmull(this.dp[0][k], this.dp[1][k], this.dp[2][k], this.dp[3][k], s);
            }

            Mat4.slerp(this.dr[1], this.dr[2], s, outRot);
         }
      }
   }
}
