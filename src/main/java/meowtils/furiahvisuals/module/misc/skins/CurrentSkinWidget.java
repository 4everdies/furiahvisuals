package meowtils.furiahvisuals.module.misc.skins;

import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.util.ResourceLocation;

final class CurrentSkinWidget extends Widget {
   private static final String[] MOVES = new String[]{"Idle", "Walk"};
   private static final String[] SIDES = new String[]{"Front", "Back"};
   private final Skins skins;
   private float yaw;
   private float targetYaw;
   private boolean dragging;
   private float dragX;
   private boolean walk;
   private ResourceLocation lastSkin;
   private boolean lastSlim;
   private double clock;
   private final float[] segHover = new float[4];
   private final Pill savePill = new Pill();

   CurrentSkinWidget(Skins skins) {
      this.skins = skins;
   }

   @Override
   public float height() {
      return Density.dense() ? 116.0F : (Density.compact() ? 146.0F : 180.0F);
   }

   private float previewW() {
      return Math.round(this.previewH() * 0.72F);
   }

   private float previewH() {
      return this.height() - 8.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.skins.update();
      this.clock += dt;
      if (!this.dragging) {
         this.yaw = this.yaw + (this.targetYaw - this.yaw) * Math.min(1.0F, dt * 9.0F);
      }

      SkinLibrary lib = this.skins.library();
      SkinSearch.Result t = this.skins.active() ? this.skins.tried() : null;
      SkinLibrary.Entry e = this.skins.active() && t == null ? this.skins.wornEntry() : null;
      boolean slim;
      ResourceLocation skin;
      if (t != null && t.texture != null) {
         skin = t.location();
         slim = t.slim;
      } else if (e != null && lib.want(e)) {
         skin = e.location();
         slim = e.slim;
      } else if ((e != null || t != null) && this.lastSkin != null) {
         skin = this.lastSkin;
         slim = this.lastSlim;
      } else {
         boolean[] s = new boolean[1];
         skin = SkinHooks.accountSkin(s);
         slim = s[0];
      }

      this.lastSkin = skin;
      this.lastSlim = slim;
      float pw = this.previewW();
      float ph = this.previewH();
      float px = this.x;
      float py = this.y + 4.0F;
      boolean overPreview = mx >= px && mx < px + pw && my >= py && my < py + ph;
      int tex = this.skins.previews().drawBig(skin, slim, Math.round(pw * Ui.px), Math.round(ph * Ui.px), this.yaw, this.walk, this.clock);
      if (tex > 0) {
         Ui.roundedImage(tex, px, py, pw, ph, 9.0F, 0.0F, 1.0F, 1.0F, 0.0F, -1);
      } else {
         Ui.gradientV(px, py, pw, ph, 9.0F, -13881285, -15460836);
      }

      Ui.outline(px, py, pw, ph, 9.0F, 0.8F, !overPreview && !this.dragging ? 620756991 : Theme.OUTLINE_HOVER);
      Ui.registerControl(px, py, pw, ph, "skin preview");
      if (overPreview && !this.dragging) {
         String hint = "Drag to turn";
         float hs = 6.8F;
         float hw = Ui.width(hint, hs) + 10.0F;
         Ui.roundRect(px + (pw - hw) / 2.0F, py + ph - 17.0F, hw, 12.0F, 6.0F, -1728053248);
         Ui.textMiddle(hint, px + (pw - hw) / 2.0F + 5.0F, py + ph - 11.0F, hs, -788529153, false);
      }

      float tx = px + pw + 14.0F;
      float tw = this.w - pw - 14.0F;
      float ty = this.y + 6.0F;
      boolean dense = Density.dense();
      String name = t != null ? t.name : (e != null ? e.name : "My account skin");
      float ns = dense ? 10.5F : 12.5F;
      Ui.textBold(Ui.ellipsize(name, ns, tw, true), tx, ty, ns, Theme.TEXT);
      String model = slim ? "Slim model · 3-pixel arms" : "Classic model · 4-pixel arms";
      float var35;
      Ui.text(Ui.ellipsize(model, Density.small(), tw, false), tx, var35 = ty + (ns + 6.0F), Density.small(), Theme.TEXT_DIM);
      ty = var35 + (Density.small() + 7.0F);
      float bx = tx;
      if (t != null) {
         bx = tx + (badge("Not saved", tx, ty, accent) + 5.0F);
         if (t.image != null && t.image.hd()) {
            bx += badge("HD " + t.image.size + "×" + t.image.size, bx, ty, accent) + 5.0F;
         }
      }

      if (e != null && e.hd()) {
         bx += badge("HD " + e.size + "×" + e.size, bx, ty, accent) + 5.0F;
      }

      if (e == null && t == null || e != null && e.account) {
         bx += badge("Account", bx, ty, accent) + 5.0F;
      }

      if (bx > tx) {
         ty += 17.0F;
      }

      boolean custom = e != null || t != null;
      String note = !custom ? "Your account skin: everyone sees it" : (e != null && e.account ? "Also your account skin" : "Only you see this skin");
      Ui.circle(tx + 3.0F, ty + 4.5F, 2.6F, custom ? accent : Theme.TEXT_MUTED);
      Ui.text(Ui.ellipsize(note, Density.small(), tw - 10.0F, false), tx + 10.0F, ty, Density.small(), Theme.TEXT_DIM);
      ty += Density.small() + 8.0F;
      this.savePill.x = -1000.0F;
      if (t != null) {
         float f = this.y + this.height();
         float f2 = dense ? 24.0F : 30.0F;
         if (ty + 20.0F < f - f2) {
            this.savePill.layout(tx, ty, "Save");
            this.savePill.draw("Save", mx, my, dt, accent, true, true);
         }
      }

      float sy = this.y + this.height() - (dense ? 20.0F : 26.0F);
      float segW = Math.min(110.0F, (tw - 8.0F) / 2.0F);
      this.segmented(MOVES, this.walk ? 1 : 0, tx, sy, segW, mx, my, dt, accent, 0);
      int side = Math.abs(wrap(this.targetYaw)) > 90.0F ? 1 : 0;
      this.segmented(SIDES, side, tx + segW + 8.0F, sy, segW, mx, my, dt, accent, 2);
   }

   private static float badge(String text, float x, float y, int accent) {
      float s = 6.8F;
      float bw = Ui.widthBold(text, s) + 10.0F;
      Ui.roundRect(x, y, bw, 12.0F, 6.0F, Theme.ACCENT_SOFT);
      Ui.textMiddle(text, x + 5.0F, y + 6.0F, s, Theme.TEXT, true);
      return bw;
   }

   private float segH() {
      return Density.dense() ? 16.0F : 19.0F;
   }

   private void segmented(String[] labels, int picked, float sx, float sy, float sw, float mx, float my, float dt, int accent, int hoverBase) {
      float h = this.segH();
      float half = sw / 2.0F;
      Ui.surface(sx, sy, sw, h, h / 2.0F, Theme.SURFACE);

      for (int i = 0; i < 2; i++) {
         float hx = sx + i * half;
         boolean over = mx >= hx && mx < hx + half && my >= sy && my < sy + h;
         this.segHover[hoverBase + i] = Easing.approach(this.segHover[hoverBase + i], over ? 1.0F : 0.0F, 16.0F, dt);
         if (i == picked) {
            Ui.roundRect(hx + 1.5F, sy + 1.5F, half - 3.0F, h - 3.0F, (h - 3.0F) / 2.0F, Theme.ACCENT_SOFT);
         }

         Ui.registerControl(hx, sy, half, h, labels[i]);
         float ts = Density.small();
         int color = i == picked ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.segHover[hoverBase + i]);
         Ui.textMiddle(labels[i], hx + (half - Ui.width(labels[i], ts)) / 2.0F, sy + h / 2.0F, ts, color, false);
      }
   }

   private static float wrap(float degrees) {
      float d = degrees % 360.0F;
      if (d > 180.0F) {
         d -= 360.0F;
      } else if (d < -180.0F) {
         d += 360.0F;
      }

      return d;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         float pw = this.previewW();
         float ph = this.previewH();
         float px = this.x;
         float py = this.y + 4.0F;
         if (mx >= px && mx < px + pw && my >= py && my < py + ph) {
            this.dragging = true;
            this.dragX = mx;
            return true;
         } else {
            float tx = px + pw + 14.0F;
            float tw = this.w - pw - 14.0F;
            float sy = this.y + this.height() - (Density.dense() ? 20.0F : 26.0F);
            float h = this.segH();
            float segW = Math.min(110.0F, (tw - 8.0F) / 2.0F);
            if (my >= sy && my < sy + h) {
               if (mx >= tx && mx < tx + segW) {
                  boolean w2 = mx >= tx + segW / 2.0F;
                  if (w2 != this.walk) {
                     this.walk = w2;
                     ModSounds.gui(Sfx.SELECT);
                  }

                  return true;
               }

               float bx = tx + segW + 8.0F;
               if (mx >= bx && mx < bx + segW) {
                  boolean back = mx >= bx + segW / 2.0F;
                  float base = Math.round(this.yaw / 360.0F) * 360.0F;
                  float target = base + (back ? 180.0F : 0.0F);
                  if (Math.abs(target - this.yaw) > 180.0F) {
                     target += target > this.yaw ? -360.0F : 360.0F;
                  }

                  this.targetYaw = target;
                  ModSounds.gui(Sfx.SELECT);
                  return true;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public void mouseDrag(float mx, float my) {
      if (this.dragging) {
         this.yaw = this.yaw + (mx - this.dragX) * 1.6F;
         this.targetYaw = this.yaw;
         this.dragX = mx;
      }
   }

   @Override
   public void mouseUp(float mx, float my) {
      this.dragging = false;
   }

   @Override
   public boolean isDragging() {
      return this.dragging;
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else {
         float pw = this.previewW();
         if (mx < this.x + pw) {
            return "You in the skin you wear: drag to turn";
         } else {
            return this.savePill.contains(mx, my) ? "Keeps the skin you tried on in your library, named after the player" : null;
         }
      }
   }
}
