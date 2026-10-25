package meowtils.furiahvisuals.module.ui.crosshair;

import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.hud.ScreenProjection;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.Displayable;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

final class HitMarkers {
   private static final long COMBO_NANOS = 650000000L;
   private static final int MAX_COMBO = 5;
   private long hitNanos = -1L;
   private long killNanos = -1L;
   private boolean crit;
   private int combo;
   private Entity target;
   private double hitDx;
   private double hitDy;
   private double hitDz;
   private final float[] screen = new float[2];

   void hit(Entity target, boolean crit, long now) {
      this.combo = this.hitNanos >= 0L && now - this.hitNanos < 650000000L ? Math.min(5, this.combo + 1) : 0;
      this.hitNanos = now;
      this.crit = crit;
      this.target = target;
      if (target != null) {
         this.hitDx = 0.0;
         this.hitDy = target.height * 0.62;
         this.hitDz = 0.0;
      }
   }

   void kill(long now) {
      this.killNanos = now;
   }

   void clear() {
      this.killNanos = -1L;
      this.hitNanos = -1L;
      this.combo = 0;
      this.target = null;
   }

   float skullCover(CrosshairElement e, long now) {
      if (e.killMarker.get() && e.killStyle.get() == HitMarkers.KillStyle.SKULL && this.killNanos >= 0L) {
         float age = (float)(now - this.killNanos) / 1.0E9F;
         float dur = e.killDuration.getFloat() / 1000.0F;
         return age >= dur ? 0.0F : fade(age / dur, false);
      } else {
         return 0.0F;
      }
   }

   boolean showing(CrosshairElement e, long now) {
      return e.hitMarker.get() && this.hitNanos >= 0L && now - this.hitNanos < (long)(e.hitDuration.get() * 1000000.0)
         || e.killMarker.get() && this.killNanos >= 0L && now - this.killNanos < (long)(e.killDuration.get() * 1000000.0);
   }

   void draw(CrosshairElement e, float cx, float cy, float k, float reach, float partialTicks, long now) {
      float clear = (reach + 2.0F) * k;
      boolean calm = FuriahsVisuals.instance.isReduceFlashing();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
      Ui.premultiplied(false);
      if (e.hitMarker.get() && this.hitNanos >= 0L) {
         float age = (float)(now - this.hitNanos) / 1.0E9F;
         float dur = e.hitDuration.getFloat() / 1000.0F;
         if (age < dur) {
            float x = cx;
            float y = cy;
            float around = clear;
            if (e.hitPosition.get() == HitMarkers.Position.ON_TARGET
               && this.target != null
               && !this.target.isDead
               && ScreenProjection.project(
                  this.target.lastTickPosX + (this.target.posX - this.target.lastTickPosX) * partialTicks + this.hitDx,
                  this.target.lastTickPosY + (this.target.posY - this.target.lastTickPosY) * partialTicks + this.hitDy,
                  this.target.lastTickPosZ + (this.target.posZ - this.target.lastTickPosZ) * partialTicks + this.hitDz,
                  this.screen
               )) {
               x = this.screen[0];
               y = this.screen[1];
               around = 0.0F;
            }

            float p = age / dur;
            float size = e.hitSize.getFloat() * k * (this.crit ? 1.25F : 1.0F) * (1.0F + 0.1F * this.combo);
            int color = this.crit ? e.critColor.getColor(0.5F) : e.hitColor.getColor(0.5F);
            drawHit(e.hitStyle.get(), x, y, size, e.hitThickness.getFloat() * k, color, p, calm, around);
         } else {
            this.combo = 0;
         }
      }

      float dur;
      float age;
      if (e.killMarker.get() && this.killNanos >= 0L && (age = (float)(now - this.killNanos) / 1.0E9F) < (dur = e.killDuration.getFloat() / 1000.0F)) {
         drawKill(
            e.killStyle.get(), cx, cy, e.killSize.getFloat() * k, e.hitThickness.getFloat() * k * 1.3F, e.killColor.getColor(0.5F), age / dur, calm, clear
         );
      }
   }

   private static float fade(float p, boolean calm) {
      float a = p < 0.55F ? 1.0F : 1.0F - (p - 0.55F) / 0.45F;
      return Math.max(0.0F, a) * (calm ? 0.65F : 1.0F);
   }

   private static void drawHit(HitMarkers.HitStyle style, float x, float y, float size, float th, int color, float p, boolean calm, float clear) {
      float a = fade(p, calm);
      float pop = calm ? 1.0F : 1.0F + 0.18F * Math.max(0.0F, 1.0F - p * 5.0F);

      for (int pass = 0; pass < 2; pass++) {
         int col = pass == 0 ? ColorUtil.mulAlpha(-16777216, 0.45F * a) : ColorUtil.mulAlpha(color | 0xFF000000, a);
         float t = pass == 0 ? th + 1.5F : th;
         switch (style) {
            case BURST:
               float r0 = Math.max(clear, 3.0F * size / 6.0F) + 9.0F * easeOut(p) * size / 6.0F;
               float r1 = r0 + size * 0.5F;

               for (int i = 0; i < 4; i++) {
                  double ang = Math.toRadians(45 + i * 90);
                  float cs = (float)Math.cos(ang);
                  float sn = (float)Math.sin(ang);
                  Ui.line(x + cs * r0, y + sn * r0, x + cs * r1, y + sn * r1, t, true, col);
               }
               break;
            case RING:
               Ui.arc(x, y, Math.max(clear, size * 0.5F) + size * 0.9F * easeOut(p), t, 0.0F, 360.0F, false, col);
               break;
            case DOT:
               float r = size * 0.35F * pop + (pass == 0 ? 0.75F : 0.0F);
               Ui.roundRect(x - r, y - r, r * 2.0F, r * 2.0F, r, col);
               break;
            default:
               float innerRadius = Math.max(size * 0.45F * pop, clear);
               float outerRadius = innerRadius + size * pop;

               for (int i = 0; i < 4; i++) {
                  double ang = Math.toRadians(45 + i * 90);
                  float cs = (float)Math.cos(ang);
                  float sn = (float)Math.sin(ang);
                  float e = pass == 0 ? 0.75F : 0.0F;
                  Ui.line(x + cs * (innerRadius - e), y + sn * (innerRadius - e), x + cs * (outerRadius + e), y + sn * (outerRadius + e), t, false, col);
               }
         }
      }
   }

   private static void drawKill(HitMarkers.KillStyle style, float x, float y, float size, float th, int color, float p, boolean calm, float clear) {
      float a = fade(p, calm);
      float pop = calm ? 1.0F : 1.0F + 0.25F * Math.max(0.0F, 1.0F - p * 4.0F);

      for (int pass = 0; pass < 2; pass++) {
         int col = pass == 0 ? ColorUtil.mulAlpha(-16777216, 0.45F * a) : ColorUtil.mulAlpha(color | 0xFF000000, a);
         float t = pass == 0 ? th + 1.5F : th;
         switch (style) {
            case SKULL:
               float s = size * 2.6F * pop + (pass == 0 ? 2.0F : 0.0F);
               IslandIcons.draw(IslandIcons.Icon.SKULL, x - s / 2.0F, y - s / 2.0F, s, col);
               break;
            case RING_BURST:
               float base = Math.max(clear, size * 0.6F);
               Ui.arc(x, y, base + size * 1.2F * easeOut(p), t, 0.0F, 360.0F, false, col);
               float r0 = base + size * (0.3F + 1.4F * easeOut(p));
               float r1 = r0 + size * 0.45F;

               for (int i = 0; i < 8; i++) {
                  double ang = Math.toRadians(22.5 + i * 45);
                  float cs = (float)Math.cos(ang);
                  float sn = (float)Math.sin(ang);
                  Ui.line(x + cs * r0, y + sn * r0, x + cs * r1, y + sn * r1, t * 0.8F, true, col);
               }
               break;
            default:
               float innerRadius = Math.max(size * 0.4F * pop, clear);
               float outerRadius = innerRadius + size * pop;

               for (int i = 0; i < 4; i++) {
                  double ang = Math.toRadians(45 + i * 90);
                  float cs = (float)Math.cos(ang);
                  float sn = (float)Math.sin(ang);
                  float e = pass == 0 ? 0.75F : 0.0F;
                  Ui.line(x + cs * (innerRadius - e), y + sn * (innerRadius - e), x + cs * (outerRadius + e), y + sn * (outerRadius + e), t, false, col);
               }
         }
      }
   }

   private static float easeOut(float p) {
      float q = 1.0F - Math.min(1.0F, Math.max(0.0F, p));
      return 1.0F - q * q * q;
   }

   static enum HitStyle implements Displayable {
      CLASSIC_X("Classic X", "Four short diagonal lines round the center"),
      BURST("Burst", "Four short ticks that fly outward"),
      RING("Ring", "A ring that opens out"),
      DOT("Dot", "A dot that pops");

      private final String displayName;
      private final String description;

      private HitStyle(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   static enum KillStyle implements Displayable {
      RED_X("Red X", "A bigger, red hit marker"),
      SKULL("Skull", "A skull that pops in"),
      RING_BURST("Ring Burst", "A ring bursting out with sparks");

      private final String displayName;
      private final String description;

      private KillStyle(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   static enum Position implements Displayable {
      ON_CROSSHAIR("On Crosshair", "Round the crosshair"),
      ON_TARGET("On Target", "Where the hit landed on the target, facing you");

      private final String displayName;
      private final String description;

      private Position(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }
}
