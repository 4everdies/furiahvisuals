package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import meowtils.furiahvisuals.asset.AssetSource;

public final class WeaponDef {
   public final String id;
   public final String name;
   public final String description;
   public final WeaponDef.Category category;
   public final List<String> tags;
   public final boolean twoHanded;
   public final WeaponDef.Grip grip;
   public final boolean dualWield;
   public final float mass;
   public final float[] bounds;
   public final WeaponDef.GripPoint right;
   public final WeaponDef.GripPoint left;
   public final Map<String, WeaponDef.Role> parts;
   public final String finish;
   public final List<String> finishes;
   public final String handleMaterial;
   public final int handleColor;
   public final int metalColor;
   public final float metalRoughness;
   public final String animations;
   public final List<String> inspects;
   public final String trail;
   public final int accentColor;
   public final boolean accentStatic;
   public final String draw;
   public final WeaponDef.Hold firstPerson;
   public final WeaponDef.Hold thirdPerson;
   public final Map<String, WeaponDef.Pivot> pivots;
   public final Map<String, Float> dangle;
   public final float[] chainPath;
   public final boolean engine;
   public final float[] exhaust;
   public final float[] glow;
   public final String glowPart;
   public final float[] trailSpan;
   public final String extend;
   public final Map<String, float[]> idle;
   public final float[] runes;
   public final float[] motes;
   public final String moteKind;
   public final float[] moteBurst;
   public final float[] dust;
   public final float bendFrom;
   public final float[] arcs;
   public final String arcPart;
   public final float heatIdle;
   public final float heatSwing;
   public final float[] core;
   public final boolean flareOnHit;
   public final float[] shimmer;
   public final WeaponDef.Bow bow;
   public final WeaponDef.Eye eye;
   public final boolean isNew;
   private final Map<String, String> textures;
   final String resourceRoot;
   final File folder;
   static final WeaponDef.Hold DEFAULT_FP = new WeaponDef.Hold(
      new float[]{0.38F, -0.42F, -0.72F}, new float[]{-0.5F, 0.3F, -0.8F}, new float[]{0.0F, -1.0F, -0.2F}, 1.0F
   );
   static final WeaponDef.Hold DEFAULT_FP_2H = new WeaponDef.Hold(
      new float[]{0.3F, -0.5F, -0.7F}, new float[]{-0.75F, 0.35F, -0.55F}, new float[]{0.0F, -1.0F, -0.3F}, 1.0F
   );
   static final WeaponDef.Hold TP_HAMMER = new WeaponDef.Hold(
      new float[]{0.0F, 0.0F, 0.0F}, new float[]{0.0F, -0.72F, -0.7F}, new float[]{0.0F, 0.7F, -0.72F}, 1.0F
   );
   static final WeaponDef.Hold TP_REVERSE = new WeaponDef.Hold(
      new float[]{0.0F, 0.0F, 0.0F}, new float[]{0.0F, -0.92F, 0.4F}, new float[]{0.0F, -0.4F, -0.92F}, 1.0F
   );
   static final WeaponDef.Hold TP_TWO_HANDED = new WeaponDef.Hold(
      new float[]{0.0F, 0.0F, 0.0F}, new float[]{0.12F, -0.75F, -0.65F}, new float[]{-0.02F, 0.67F, -0.74F}, 1.0F
   );
   public final WeaponDef.Hold dualHold;

   private WeaponDef(JsonObject o, String resourceRoot, File folder) {
      this.resourceRoot = resourceRoot;
      this.folder = folder;
      this.id = str(o, "id", folder != null ? folder.getName() : "weapon").toLowerCase(Locale.ROOT);
      this.name = str(o, "name", this.id);
      this.description = str(o, "description", "");
      this.category = folder != null ? WeaponDef.Category.IMPORTED : WeaponDef.Category.of(str(o, "category", "imported"));
      this.tags = strings(o, "tags");
      this.twoHanded = num(o, "hands", 1.0) >= 2.0;
      this.grip = this.twoHanded ? WeaponDef.Grip.TWO_HANDED : WeaponDef.Grip.of(str(o, "grip", "hammer"));
      this.dualWield = !this.twoHanded && bool(o, "dualWield", true);
      this.mass = (float)num(o, "mass", 0.6);
      float[] b = floats(o, "bounds", 6);
      float[] fArray9;
      if (b != null) {
         fArray9 = b;
      } else {
         float[] fArray10 = new float[]{-0.1F, -0.2F, -0.05F, 0.1F, 0.6F, 0.0F};
         fArray9 = fArray10;
         fArray10[5] = 0.05F;
      }

      this.bounds = fArray9;
      JsonObject grips = o.has("grips") && o.get("grips").isJsonObject() ? o.getAsJsonObject("grips") : new JsonObject();
      this.right = gripPoint(grips, "right", new float[]{0.0F, 0.0F, 0.0F});
      this.left = grips.has("left") ? gripPoint(grips, "left", new float[]{0.0F, -0.3F, 0.0F}) : null;
      LinkedHashMap p = new LinkedHashMap();
      if (o.has("parts") && o.get("parts").isJsonObject()) {
         for (Entry e : o.getAsJsonObject("parts").entrySet()) {
            p.put(e.getKey(), WeaponDef.Role.of(((JsonElement)e.getValue()).getAsString()));
         }
      }

      this.parts = Collections.unmodifiableMap(p);
      List<String> f = strings(o, "finishes");
      this.finish = str(o, "finish", f.isEmpty() ? "steel_polished" : f.get(0));
      ArrayList<String> offered = new ArrayList<>(f.isEmpty() ? Collections.singletonList(this.finish) : f);
      if (!"vanilla".equals(this.id)) {
         for (String every : new String[]{"bone", "crimson", "sapphire"}) {
            if (!offered.contains(every)) {
               offered.add(every);
            }
         }
      }

      this.finishes = Collections.unmodifiableList(offered);
      JsonObject handle = o.has("handle") && o.get("handle").isJsonObject() ? o.getAsJsonObject("handle") : new JsonObject();
      this.handleMaterial = str(handle, "material", "rubber");
      this.handleColor = color(str(handle, "color", "#202022"));
      JsonObject metal = o.has("metal") && o.get("metal").isJsonObject() ? o.getAsJsonObject("metal") : new JsonObject();
      this.metalColor = color(str(metal, "color", "#8A8C90"));
      this.metalRoughness = (float)num(metal, "roughness", 0.35);
      this.animations = str(o, "animations", this.twoHanded ? "heavy" : "sword");
      this.inspects = strings(o, "inspects");
      this.trail = str(o, "trail", "none");
      this.accentColor = color(str(o, "accent", "#B45CFF"));
      this.accentStatic = bool(o, "accentStatic", false);
      this.draw = str(o, "draw", null);
      this.firstPerson = WeaponDef.Hold.read(obj(o, "firstPerson"), this.twoHanded ? DEFAULT_FP_2H : DEFAULT_FP);
      this.thirdPerson = WeaponDef.Hold.read(
         obj(o, "thirdPerson"), this.twoHanded ? TP_TWO_HANDED : (this.grip == WeaponDef.Grip.REVERSE ? TP_REVERSE : TP_HAMMER)
      );
      this.dualHold = obj(o, "dualHold") == null ? null : WeaponDef.Hold.read(obj(o, "dualHold"), DEFAULT_FP);
      LinkedHashMap<String, WeaponDef.Pivot> pv = new LinkedHashMap<>();
      JsonObject pivotJson = obj(o, "pivots");
      if (pivotJson != null) {
         for (Entry entry : pivotJson.entrySet()) {
            if (((JsonElement)entry.getValue()).isJsonObject()) {
               float[] origin = floats(((JsonElement)entry.getValue()).getAsJsonObject(), "origin", 3);
               float[] axis = floats(((JsonElement)entry.getValue()).getAsJsonObject(), "axis", 3);
               String parent = str(((JsonElement)entry.getValue()).getAsJsonObject(), "parent", null);
               Object k = entry.getKey();
               float[] fArray12 = origin != null ? origin : new float[3];
               float[] fArray11;
               if (axis != null) {
                  fArray11 = axis;
               } else {
                  float[] fArray13 = new float[]{0.0F, 0.0F, 0.0F};
                  fArray11 = fArray13;
                  fArray13[2] = 1.0F;
               }

               pv.put((String)k, new WeaponDef.Pivot(fArray12, fArray11, parent != null && !parent.equals(entry.getKey()) ? parent : null));
            }
         }
      }

      for (Entry<String, WeaponDef.Pivot> entryx : new ArrayList<>(pv.entrySet())) {
         Object up = entryx.getValue().parent;

         int depth;
         for (depth = 0; up != null && pv.containsKey(up) && depth < 8; depth++) {
            up = pv.get(up).parent;
         }

         if (entryx.getValue().parent != null && (!pv.containsKey(entryx.getValue().parent) || depth >= 8)) {
            pv.put(entryx.getKey(), new WeaponDef.Pivot(entryx.getValue().origin, entryx.getValue().axis, null));
         }
      }

      this.pivots = Collections.unmodifiableMap(pv);
      LinkedHashMap dg = new LinkedHashMap();
      JsonObject jsonObject = obj(o, "dangle");
      if (jsonObject != null) {
         for (Entry e : jsonObject.entrySet()) {
            if (((JsonElement)e.getValue()).isJsonPrimitive() && pv.containsKey(e.getKey())) {
               dg.put(e.getKey(), Math.max(0.0F, Math.min(1.0F, ((JsonElement)e.getValue()).getAsFloat())));
            }
         }
      }

      this.dangle = Collections.unmodifiableMap(dg);
      JsonObject chainJson = obj(o, "chain");
      this.chainPath = chainJson == null ? null : floats(chainJson, "path", 4);
      this.engine = bool(o, "engine", false);
      JsonObject exhaustJson = obj(o, "exhaust");
      float[] exPos = exhaustJson == null ? null : floats(exhaustJson, "pos", 3);
      float[] exDir = exhaustJson == null ? null : floats(exhaustJson, "dir", 3);
      float[] fArray8;
      if (exPos == null) {
         fArray8 = null;
      } else {
         float[] fArray15 = new float[]{exPos[0], exPos[1], exPos[2], exDir != null ? exDir[0] : 0.0F, exDir != null ? exDir[1] : 1.0F, 0.0F};
         fArray8 = fArray15;
         fArray15[5] = exDir != null ? exDir[2] : 0.0F;
      }

      this.exhaust = fArray8;
      JsonObject glowJson = obj(o, "glow");
      float[] gFrom = glowJson == null ? null : floats(glowJson, "from", 3);
      float[] gTo = glowJson == null ? null : floats(glowJson, "to", 3);
      float[] fArray7;
      if (gFrom != null && gTo != null) {
         float[] fArray17 = new float[]{gFrom[0], gFrom[1], gFrom[2], gTo[0], gTo[1], gTo[2], 0.0F};
         fArray7 = fArray17;
         fArray17[6] = (float)num(glowJson, "width", 0.06);
      } else {
         fArray7 = null;
      }

      this.glow = fArray7;
      this.glowPart = glowJson == null ? null : str(glowJson, "part", null);
      JsonObject trailJson = obj(o, "trailSpan");
      float[] tFrom = trailJson == null ? null : floats(trailJson, "from", 3);
      float[] tTo = trailJson == null ? null : floats(trailJson, "to", 3);
      float[] fArray6;
      if (tFrom != null && tTo != null) {
         float[] fArray19 = new float[]{tFrom[0], tFrom[1], tFrom[2], tTo[0], tTo[1], 0.0F};
         fArray6 = fArray19;
         fArray19[5] = tTo[2];
      } else {
         fArray6 = null;
      }

      this.trailSpan = fArray6;
      this.extend = str(o, "extend", null);
      LinkedHashMap id = new LinkedHashMap();
      JsonObject idleJson = obj(o, "idle");
      if (idleJson != null) {
         for (Entry ex : idleJson.entrySet()) {
            if (((JsonElement)ex.getValue()).isJsonObject() && pv.containsKey(ex.getKey())) {
               JsonObject m = ((JsonElement)ex.getValue()).getAsJsonObject();
               float[] bob = floats(m, "bob", 2);
               id.put(ex.getKey(), new float[]{(float)num(m, "spin", 0.0), bob != null ? bob[0] : 0.0F, bob != null ? bob[1] : 0.0F});
            }
         }
      }

      this.idle = Collections.unmodifiableMap(id);
      JsonObject runesJson = obj(o, "runes");
      float[] fArray5;
      if (runesJson == null) {
         fArray5 = null;
      } else {
         float[] fArray20 = new float[]{(float)num(runesJson, "from", 0.0), 0.0F};
         fArray5 = fArray20;
         fArray20[1] = (float)num(runesJson, "to", 1.0);
      }

      this.runes = fArray5;
      JsonObject motesJson = obj(o, "motes");
      float[] mFrom = motesJson == null ? null : floats(motesJson, "from", 3);
      float[] mTo = motesJson == null ? null : floats(motesJson, "to", 3);
      float[] mVia = motesJson == null ? null : floats(motesJson, "via", 3);
      float[] fArray4;
      if (mFrom == null || mTo == null) {
         fArray4 = null;
      } else if (mVia == null) {
         float[] fArray22 = new float[]{mFrom[0], mFrom[1], mFrom[2], mTo[0], mTo[1], mTo[2], (float)num(motesJson, "rate", 2.0), 0.0F};
         fArray4 = fArray22;
         fArray22[7] = (float)num(motesJson, "rise", 0.05);
      } else {
         float[] fArray23 = new float[]{
            mFrom[0],
            mFrom[1],
            mFrom[2],
            mTo[0],
            mTo[1],
            mTo[2],
            (float)num(motesJson, "rate", 2.0),
            (float)num(motesJson, "rise", 0.05),
            mVia[0],
            mVia[1],
            0.0F
         };
         fArray4 = fArray23;
         fArray23[10] = mVia[2];
      }

      this.motes = fArray4;
      this.moteKind = motesJson == null ? null : str(motesJson, "kind", "mote");
      this.moteBurst = this.motes == null ? null : floats(motesJson, "burst", 3);
      JsonObject dustJson = obj(o, "dust");
      this.dust = dustJson == null ? null : floats(dustJson, "at", 3);
      JsonObject bendJson = obj(o, "bend");
      this.bendFrom = bendJson == null ? 0.0F : (float)num(bendJson, "from", 0.0);
      JsonObject arcJson = obj(o, "arcs");
      float[] aFrom = arcJson == null ? null : floats(arcJson, "from", 3);
      float[] aTo = arcJson == null ? null : floats(arcJson, "to", 3);
      float[] fArray3;
      if (aFrom != null && aTo != null) {
         float[] fArray25 = new float[]{aFrom[0], aFrom[1], aFrom[2], aTo[0], aTo[1], aTo[2], (float)num(arcJson, "width", 0.004), 0.0F};
         fArray3 = fArray25;
         fArray25[7] = (float)num(arcJson, "rate", 1.0);
      } else {
         fArray3 = null;
      }

      this.arcs = fArray3;
      this.arcPart = arcJson == null ? null : str(arcJson, "part", null);
      JsonObject heatJson = obj(o, "heat");
      this.heatIdle = heatJson == null ? 0.0F : Math.max(0.0F, Math.min(1.0F, (float)num(heatJson, "idle", 0.5)));
      this.heatSwing = heatJson == null ? 0.0F : Math.max(0.0F, Math.min(1.0F, (float)num(heatJson, "swing", 0.3)));
      JsonObject coreJson = obj(o, "core");
      float[] coreAt = coreJson == null ? null : floats(coreJson, "at", 3);
      float[] fArray2;
      if (coreAt == null) {
         fArray2 = null;
      } else {
         float[] fArray27 = new float[]{coreAt[0], coreAt[1], coreAt[2], 0.0F};
         fArray2 = fArray27;
         fArray27[3] = Math.max(0.01F, (float)num(coreJson, "reach", 1.0));
      }

      this.core = fArray2;
      this.flareOnHit = bool(o, "flareOnHit", false);
      JsonObject shimmerJson = obj(o, "shimmer");
      float[] sFrom = shimmerJson == null ? null : floats(shimmerJson, "from", 3);
      float[] sTo = shimmerJson == null ? null : floats(shimmerJson, "to", 3);
      float[] fArray;
      if (sFrom != null && sTo != null) {
         float[] fArray29 = new float[]{sFrom[0], sFrom[1], sFrom[2], sTo[0], sTo[1], sTo[2], 0.0F};
         fArray = fArray29;
         fArray29[6] = (float)num(shimmerJson, "height", 0.08);
      } else {
         fArray = null;
      }

      this.shimmer = fArray;
      this.bow = WeaponDef.Bow.read(obj(o, "bow"));
      this.eye = WeaponDef.Eye.read(obj(o, "eye"), pv);
      this.isNew = folder == null && bool(o, "new", false);
      LinkedHashMap<String, String> tx = new LinkedHashMap<>();
      JsonObject texJson = obj(o, "textures");

      for (String kind : new String[]{"albedo", "material", "normal"}) {
         tx.put(kind, texJson != null ? str(texJson, kind, kind + ".png") : kind + ".png");
      }

      this.textures = Collections.unmodifiableMap(tx);
   }

   private static JsonObject obj(JsonObject o, String key) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
   }

   String texture(String kind) {
      return this.textures.get(kind);
   }

   InputStream open(String name) throws IOException {
      if (this.folder != null) {
         return WeaponAssets.openFile(this.folder, name);
      } else {
         return this.resourceRoot == null ? null : AssetSource.open(this.resourceRoot + name);
      }
   }

   static WeaponDef bundled(JsonObject json, String root) {
      return new WeaponDef(json, root, null);
   }

   static WeaponDef imported(JsonObject json, File folder) {
      return new WeaponDef(json, null, folder);
   }

   static WeaponDef vanilla() {
      JsonObject o = new JsonObject();
      o.addProperty("id", "vanilla");
      o.addProperty("name", "Vanilla");
      o.addProperty("category", "vanilla");
      o.addProperty("description", "Your resource pack's own item, exactly as it draws it");
      o.addProperty("hands", 1);
      o.addProperty("mass", 0.5);
      JsonArray tags = new JsonArray();

      for (String t : new String[]{"vanilla", "default", "resource pack", "texture pack", "item"}) {
         tags.add(new JsonPrimitive(t));
      }

      o.add("tags", tags);
      JsonArray inspects = new JsonArray();

      for (String t : new String[]{"flip_present", "look_over", "twirl", "toss_catch"}) {
         inspects.add(new JsonPrimitive(t));
      }

      o.add("inspects", inspects);
      o.addProperty("animations", "vanilla");
      return new WeaponDef(o, null, null);
   }

   public boolean isVanilla() {
      return "vanilla".equals(this.id);
   }

   public boolean isImported() {
      return this.folder != null;
   }

   public boolean isBow() {
      return this.bow != null;
   }

   public float length() {
      return this.bounds[4] - this.bounds[1];
   }

   public String categoryName() {
      return this.category.displayName;
   }

   public WeaponDef.Role roleOf(String group) {
      WeaponDef.Role r = this.parts.get(group);
      return r != null ? r : WeaponDef.Role.FIXED;
   }

   private static WeaponDef.GripPoint gripPoint(JsonObject grips, String key, float[] fallbackPos) {
      JsonObject g = grips.has(key) && grips.get(key).isJsonObject() ? grips.getAsJsonObject(key) : new JsonObject();
      float[] pos = floats(g, "pos", 3);
      float[] axis = floats(g, "axis", 3);
      float[] edge = floats(g, "edge", 3);
      float[] reach = floats(g, "reach", 3);
      float[] fArray3 = pos != null ? pos : fallbackPos;
      float[] fArray2;
      if (axis != null) {
         fArray2 = axis;
      } else {
         float[] fArray4 = new float[]{0.0F, 1.0F, 0.0F};
         fArray2 = fArray4;
         fArray4[2] = 0.0F;
      }

      float[] fArray;
      if (edge != null) {
         fArray = edge;
      } else {
         float[] fArray5 = new float[]{1.0F, 0.0F, 0.0F};
         fArray = fArray5;
         fArray5[2] = 0.0F;
      }

      return new WeaponDef.GripPoint(fArray3, fArray2, fArray, reach);
   }

   static String str(JsonObject o, String key, String fallback) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
   }

   static double num(JsonObject o, String key, double fallback) {
      JsonElement e = o.get(key);

      try {
         return e != null && e.isJsonPrimitive() ? e.getAsDouble() : fallback;
      } catch (RuntimeException var6) {
         return fallback;
      }
   }

   static boolean bool(JsonObject o, String key, boolean fallback) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : fallback;
   }

   static List<String> strings(JsonObject o, String key) {
      ArrayList<String> out = new ArrayList<>();
      JsonElement e = o.get(key);
      if (e != null && e.isJsonArray()) {
         for (JsonElement x : e.getAsJsonArray()) {
            if (x.isJsonPrimitive()) {
               out.add(x.getAsString());
            }
         }
      }

      return Collections.unmodifiableList(out);
   }

   static float[] floats(JsonObject o, String key, int n) {
      JsonElement e = o.get(key);
      if (e != null && e.isJsonArray()) {
         JsonArray a = e.getAsJsonArray();
         if (a.size() < n) {
            return null;
         } else {
            float[] out = new float[n];

            for (int i = 0; i < n; i++) {
               out[i] = a.get(i).getAsFloat();
            }

            return out;
         }
      } else {
         return null;
      }
   }

   static int color(String hex) {
      String h = hex.startsWith("#") ? hex.substring(1) : hex;

      try {
         long v = Long.parseLong(h, 16);
         return h.length() <= 6 ? (int)(4278190080L | v) : (int)v;
      } catch (NumberFormatException var4) {
         return -8355712;
      }
   }

   public static final class Bow {
      public final float[] tips;
      public final float[] nock;
      public final float draw;
      public final float from;
      public final float bend;
      public final String[][] limbs;
      public final String arrow;
      public final int stringColor;
      public final WeaponDef.Hold aim;
      public final float[] rest;
      public final Map<String, Float> fan;
      public final boolean stringGlow;
      public final float glowRest;
      public final float stringAlpha;
      static final WeaponDef.Hold AIM = new WeaponDef.Hold(
         new float[]{0.06F, -0.2F, -0.78F}, new float[]{-0.12F, 1.0F, 0.02F}, new float[]{0.0F, 0.0F, -1.0F}, 1.0F
      );

      Bow(
         float[] tips,
         float[] nock,
         float draw,
         float from,
         float bend,
         String[][] limbs,
         String arrow,
         int stringColor,
         WeaponDef.Hold aim,
         float[] rest,
         Map<String, Float> fan,
         boolean stringGlow,
         float glowRest,
         float stringAlpha
      ) {
         this.tips = tips;
         this.nock = nock;
         this.draw = draw;
         this.from = from;
         this.bend = bend;
         this.limbs = limbs;
         this.arrow = arrow;
         this.stringColor = stringColor;
         this.aim = aim;
         this.rest = rest;
         this.fan = fan;
         this.stringGlow = stringGlow;
         this.glowRest = glowRest;
         this.stringAlpha = stringAlpha;
      }

      public float glow(float pull, float flare) {
         return this.glowRest + (1.0F - this.glowRest) * Math.max(0.0F, Math.min(1.0F, Math.max(pull, flare)));
      }

      static WeaponDef.Bow read(JsonObject o) {
         if (o != null && o.has("tips") && o.get("tips").isJsonArray() && o.getAsJsonArray("tips").size() == 2) {
            float[] tips = new float[6];

            for (int i = 0; i < 2; i++) {
               JsonElement t = o.getAsJsonArray("tips").get(i);
               if (!t.isJsonArray() || t.getAsJsonArray().size() != 3) {
                  return null;
               }

               for (int k = 0; k < 3; k++) {
                  tips[i * 3 + k] = t.getAsJsonArray().get(k).getAsFloat();
               }
            }

            float[] nock = WeaponDef.floats(o, "nock", 3);
            String[][] limbs = new String[2][];
            JsonElement l = o.get("limbs");

            for (int i = 0; i < 2; i++) {
               ArrayList<String> names = new ArrayList<>();
               if (l != null && l.isJsonArray() && l.getAsJsonArray().size() > i && l.getAsJsonArray().get(i).isJsonArray()) {
                  for (JsonElement n : l.getAsJsonArray().get(i).getAsJsonArray()) {
                     names.add(n.getAsString());
                  }
               }

               limbs[i] = names.toArray(new String[0]);
            }

            float[] rest = WeaponDef.floats(o, "rest", 3);
            LinkedHashMap fan = new LinkedHashMap();
            JsonObject fanJson = WeaponDef.obj(o, "fan");
            if (fanJson != null) {
               for (Entry e : fanJson.entrySet()) {
                  if (((JsonElement)e.getValue()).isJsonPrimitive() && ((JsonElement)e.getValue()).getAsJsonPrimitive().isNumber()) {
                     fan.put(e.getKey(), ((JsonElement)e.getValue()).getAsFloat());
                  }
               }
            }

            float[] fArray2;
            if (nock != null) {
               fArray2 = nock;
            } else {
               float[] fArray3 = new float[]{tips[0], 0.0F, 0.0F};
               fArray2 = fArray3;
               fArray3[2] = 0.0F;
            }

            float f = Math.max(0.0F, (float)WeaponDef.num(o, "draw", 0.5));
            float f2 = Math.max(0.0F, (float)WeaponDef.num(o, "from", 0.1));
            float f3 = (float)WeaponDef.num(o, "bend", 0.15);
            String string = WeaponDef.str(o, "arrow", null);
            int n = WeaponDef.color(WeaponDef.str(o, "string", "#2E2A24"));
            WeaponDef.Hold hold = WeaponDef.Hold.read(WeaponDef.obj(o, "aim"), AIM);
            float[] fArray;
            if (rest != null) {
               fArray = rest;
            } else {
               float[] fArray4 = new float[]{0.08F, -0.32F, 0.0F};
               fArray = fArray4;
               fArray4[2] = 0.1F;
            }

            return new WeaponDef.Bow(
               tips,
               fArray2,
               f,
               f2,
               f3,
               limbs,
               string,
               n,
               hold,
               fArray,
               Collections.unmodifiableMap(fan),
               WeaponDef.bool(o, "stringGlow", false),
               Math.max(0.0F, Math.min(1.0F, (float)WeaponDef.num(o, "glowRest", 1.0))),
               Math.max(0.1F, Math.min(1.0F, (float)WeaponDef.num(o, "stringAlpha", 1.0)))
            );
         } else {
            return null;
         }
      }
   }

   public static enum Category {
      VANILLA("Vanilla"),
      KNIVES("Knives"),
      SWORDS("Swords"),
      POLEARMS("Polearms"),
      AXES_HAMMERS("Axes & Hammers"),
      BOWS("Bows"),
      MODERN("Modern"),
      MAGIC("Magic"),
      IMPORTED("Imported");

      public final String displayName;

      private Category(String displayName) {
         this.displayName = displayName;
      }

      static WeaponDef.Category of(String s) {
         for (WeaponDef.Category c : values()) {
            if (c.name().equalsIgnoreCase(s)) {
               return c;
            }
         }

         return IMPORTED;
      }
   }

   public static final class Eye {
      public final float[] at;
      public final String gem;
      public final String iris;
      public final String pupil;
      public final String upper;
      public final String lower;
      public final float close;
      public final int[] pixels;

      Eye(float[] at, String gem, String iris, String pupil, String upper, String lower, float close, int[] pixels) {
         this.at = at;
         this.gem = gem;
         this.iris = iris;
         this.pupil = pupil;
         this.upper = upper;
         this.lower = lower;
         this.close = close;
         this.pixels = pixels;
      }

      boolean owns(String part) {
         return part.equals(this.gem) || part.equals(this.iris) || part.equals(this.pupil) || part.equals(this.upper) || part.equals(this.lower);
      }

      static WeaponDef.Eye read(JsonObject o, Map<String, WeaponDef.Pivot> pivots) {
         float[] at = o == null ? null : WeaponDef.floats(o, "at", 3);
         String gem = o == null ? null : WeaponDef.str(o, "gem", null);
         if (at != null && gem != null && pivots.containsKey(gem)) {
            List<String> lids = WeaponDef.strings(o, "lids");
            int[] px = new int[]{1, 1, 2};
            JsonElement p = o.get("pixels");
            if (p != null && p.isJsonArray() && p.getAsJsonArray().size() == 3) {
               for (int i = 0; i < 3; i++) {
                  px[i] = Math.max(0, Math.min(4, p.getAsJsonArray().get(i).getAsInt()));
               }
            }

            return new WeaponDef.Eye(
               at,
               gem,
               WeaponDef.str(o, "iris", null),
               WeaponDef.str(o, "pupil", null),
               lids.size() > 0 ? lids.get(0) : null,
               lids.size() > 1 ? lids.get(1) : null,
               Math.max(0.0F, Math.min(90.0F, (float)WeaponDef.num(o, "close", 38.0))),
               px
            );
         } else {
            return null;
         }
      }
   }

   public static enum Grip {
      HAMMER,
      REVERSE,
      TWO_HANDED;

      static WeaponDef.Grip of(String s) {
         if ("reverse".equalsIgnoreCase(s)) {
            return REVERSE;
         } else {
            return !"two_handed".equalsIgnoreCase(s) && !"two-handed".equalsIgnoreCase(s) ? HAMMER : TWO_HANDED;
         }
      }
   }

   public static final class GripPoint {
      public final float x;
      public final float y;
      public final float z;
      public final float ax;
      public final float ay;
      public final float az;
      public final float ex;
      public final float ey;
      public final float ez;
      public final float[] reach;

      GripPoint(float[] pos, float[] axis, float[] edge) {
         this(pos, axis, edge, null);
      }

      GripPoint(float[] pos, float[] axis, float[] edge, float[] reach) {
         this.x = pos[0];
         this.y = pos[1];
         this.z = pos[2];
         this.ax = axis[0];
         this.ay = axis[1];
         this.az = axis[2];
         this.ex = edge[0];
         this.ey = edge[1];
         this.ez = edge[2];
         this.reach = reach;
      }
   }

   public static final class Hold {
      public final float[] pos;
      public final float[] tip;
      public final float[] edge;
      public final float scale;
      public final float[] shoulder;
      public final float[] leftShoulder;
      public final float twist;
      public final boolean shoulderRest;

      Hold(float[] pos, float[] tip, float[] edge, float scale) {
         this(pos, tip, edge, scale, null, null, 18.0F, false);
      }

      Hold(float[] pos, float[] tip, float[] edge, float scale, float[] shoulder, float[] leftShoulder, float twist, boolean shoulderRest) {
         this.pos = pos;
         this.tip = tip;
         this.edge = edge;
         this.scale = scale;
         this.shoulder = shoulder;
         this.leftShoulder = leftShoulder;
         this.twist = twist;
         this.shoulderRest = shoulderRest;
      }

      static WeaponDef.Hold read(JsonObject o, WeaponDef.Hold fallback) {
         if (o == null) {
            return fallback;
         } else {
            float[] pos = WeaponDef.floats(o, "pos", 3);
            float[] tip = WeaponDef.floats(o, "tip", 3);
            float[] edge = WeaponDef.floats(o, "edge", 3);
            float[] sh = WeaponDef.floats(o, "shoulder", 3);
            float[] lsh = WeaponDef.floats(o, "leftShoulder", 3);
            String rest = WeaponDef.str(o, "rest", "");
            return new WeaponDef.Hold(
               pos != null ? pos : fallback.pos,
               tip != null ? tip : fallback.tip,
               edge != null ? edge : fallback.edge,
               (float)WeaponDef.num(o, "scale", fallback.scale),
               sh != null ? sh : fallback.shoulder,
               lsh != null ? lsh : fallback.leftShoulder,
               (float)WeaponDef.num(o, "twist", fallback.twist),
               "shoulder".equals(rest) || "side".equals(rest) || fallback.shoulderRest && !o.has("rest")
            );
         }
      }
   }

   public static final class Pivot {
      public final float[] origin;
      public final float[] axis;
      public final String parent;

      Pivot(float[] origin, float[] axis, String parent) {
         this.origin = origin;
         this.axis = axis;
         this.parent = parent;
      }
   }

   public static enum Role {
      BLADE,
      METAL,
      HANDLE,
      ACCENT,
      ENERGY,
      FIXED,
      PAINT,
      CHAIN;

      static WeaponDef.Role of(String s) {
         for (WeaponDef.Role r : values()) {
            if (r.name().equalsIgnoreCase(s)) {
               return r;
            }
         }

         return FIXED;
      }
   }
}
