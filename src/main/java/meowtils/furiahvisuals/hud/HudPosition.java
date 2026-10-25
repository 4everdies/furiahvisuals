package meowtils.furiahvisuals.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import meowtils.furiahvisuals.setting.Setting;

public final class HudPosition extends Setting {
   private boolean moved;
   private float anchorX;
   private float anchorY;
   private float offsetX;
   private float offsetY;

   public HudPosition(String id, String name) {
      super(id, name, "Where it sits (set in Move HUD).");
   }

   public boolean moved() {
      return this.moved;
   }

   public float x(float screenW, float w) {
      return inside(this.anchorX * screenW + this.offsetX - this.anchorX * w, screenW, w);
   }

   public float y(float screenH, float h) {
      return inside(this.anchorY * screenH + this.offsetY - this.anchorY * h, screenH, h);
   }

   private static float inside(float v, float screen, float size) {
      return size < screen ? Math.max(0.0F, Math.min(screen - size, v)) : v;
   }

   public String describe() {
      return this.moved
         ? String.format(Locale.ROOT, "anchor %.1f, %.1f, offset %.1f, %.1f", this.anchorX, this.anchorY, this.offsetX, this.offsetY)
         : "not moved";
   }

   public void place(float left, float top, float w, float h, float screenW, float screenH) {
      float cx = (left + w / 2.0F) / Math.max(1.0F, screenW);
      float cy = (top + h / 2.0F) / Math.max(1.0F, screenH);
      this.anchorX = cx < 0.33333334F ? 0.0F : (cx > 0.6666667F ? 1.0F : 0.5F);
      this.anchorY = cy < 0.33333334F ? 0.0F : (cy > 0.6666667F ? 1.0F : 0.5F);
      this.offsetX = left + this.anchorX * w - this.anchorX * screenW;
      this.offsetY = top + this.anchorY * h - this.anchorY * screenH;
      this.moved = true;
      this.fireChanged();
   }

   @Override
   public boolean showInMenu() {
      return false;
   }

   @Override
   public void reset() {
      if (this.moved) {
         this.moved = false;
         this.offsetY = 0.0F;
         this.offsetX = 0.0F;
         this.anchorY = 0.0F;
         this.anchorX = 0.0F;
         this.fireChanged();
      }
   }

   @Override
   public JsonElement toJson() {
      JsonObject o = new JsonObject();
      o.addProperty("moved", this.moved);
      if (this.moved) {
         o.addProperty("anchorX", this.anchorX);
         o.addProperty("anchorY", this.anchorY);
         o.addProperty("offsetX", this.offsetX);
         o.addProperty("offsetY", this.offsetY);
      }

      return o;
   }

   @Override
   public void fromJson(JsonElement json) {
      this.moved = false;
      this.offsetY = 0.0F;
      this.offsetX = 0.0F;
      this.anchorY = 0.0F;
      this.anchorX = 0.0F;
      if (json != null && json.isJsonObject()) {
         JsonObject o = json.getAsJsonObject();

         try {
            if (o.has("moved") && o.get("moved").getAsBoolean()) {
               this.anchorX = clamp01(o.get("anchorX").getAsFloat());
               this.anchorY = clamp01(o.get("anchorY").getAsFloat());
               this.offsetX = o.get("offsetX").getAsFloat();
               this.offsetY = o.get("offsetY").getAsFloat();
               this.moved = true;
            }
         } catch (RuntimeException var4) {
            this.moved = false;
         }
      }
   }

   private static float clamp01(float v) {
      return Math.max(0.0F, Math.min(1.0F, v));
   }
}
