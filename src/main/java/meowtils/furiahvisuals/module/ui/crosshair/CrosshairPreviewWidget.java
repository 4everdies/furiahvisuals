package meowtils.furiahvisuals.module.ui.crosshair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.module.misc.skins.SkinHooks;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;

final class CrosshairPreviewWidget extends RowWidget {
   private static final float CHIPS_H = 20.0F;
   private static final float BOX_H = 120.0F;
   private static final float BG_H = 16.0F;
   private static final float TEXT = 8.4F;
   private static final String[] BACKGROUNDS = new String[]{"Sky", "Grass", "Cave", "Nether", "Snow", "Player"};
   private static final int PLAYER = 5;
   private static final String[] TESTS = new String[]{"On Target", "Hit", "Crit", "Combo", "Kill", "Move", "Click", "Bow Charge", "Zoom", "Low Health"};
   private static final int T_TARGET = 0;
   private static final int T_HIT = 1;
   private static final int T_CRIT = 2;
   private static final int T_COMBO = 3;
   private static final int T_KILL = 4;
   private static final int T_MOVE = 5;
   private static final int T_CLICK = 6;
   private static final int T_BOW = 7;
   private static final int T_ZOOM = 8;
   private static final int T_LOW = 9;
   private static int background;
   private static DynamicTexture[] textures;
   private final CrosshairElement element;
   private final CrosshairState st = new CrosshairState();
   private final HitMarkers markers = new HitMarkers();
   private boolean target;
   private boolean move;
   private boolean zoom;
   private boolean low;
   private long bowStart = -1L;
   private final List<Long> pendingHits = new ArrayList<>();
   private final Pill[] testPills = new Pill[TESTS.length];
   private final float[] chipHover = new float[CrosshairElement.COUNT];
   private final float[] bgHover = new float[BACKGROUNDS.length];
   private final float[] chipX = new float[CrosshairElement.COUNT];
   private final float[] chipW = new float[CrosshairElement.COUNT];
   private final float[] bgX = new float[BACKGROUNDS.length];
   private final float[] bgW = new float[BACKGROUNDS.length];
   private float testsH = 20.0F;

   CrosshairPreviewWidget(CrosshairElement element) {
      this.element = element;

      for (int i = 0; i < this.testPills.length; i++) {
         this.testPills[i] = new Pill();
      }

      element.lastPreview = this;
   }

   static void backgroundForTest(int which) {
      background = Math.max(0, Math.min(BACKGROUNDS.length - 1, which));
   }

   private float boxY() {
      return this.y + 20.0F + 5.0F;
   }

   private float bgY() {
      return this.boxY() + 120.0F + 5.0F;
   }

   private float testsY() {
      return this.bgY() + 16.0F + 6.0F;
   }

   @Override
   public float height() {
      return this.testsY() - this.y + this.testsH + 4.0F;
   }

   private CrosshairDef shownDef() {
      int e = this.element.editing();
      if (this.zoom) {
         switch ((CrosshairElement.ZoomUse)this.element.zoom.get()) {
            case OWN:
               return this.element.defs[CrosshairElement.ZOOMED];
            case HIDDEN:
               return null;
         }
      }

      return this.element.defs[e];
   }

   private void update(float dt, long now) {
      this.st.time = (float)ColorSetting.clock();
      this.st.target = Easing.approach(this.st.target, this.target ? 1.0F : 0.0F, 12.0F, dt);
      this.st.targetPlayer = background == 5;
      this.st.move = Easing.approach(this.st.move, this.move ? 1.2F : 0.0F, this.move ? 10.0F : 6.0F, dt);
      this.st.lowHealth = Easing.approach(this.st.lowHealth, this.low ? 1.0F : 0.0F, 5.0F, dt);
      this.st.click = Math.max(0.0F, this.st.click - dt / 0.15F);
      this.st.blocking = 0.0F;
      if (this.bowStart < 0L && this.element.editing() == CrosshairElement.AIMING && !this.zoom) {
         this.bowStart = now;
      }

      if (this.bowStart >= 0L) {
         float t = (float)(now - this.bowStart) / 1.0E9F;
         if (t > 1.9F) {
            this.bowStart = this.element.editing() == CrosshairElement.AIMING ? now + 400000000L : -1L;
            this.st.bow = -1.0F;
         } else if (t >= 0.0F) {
            this.st.bow = Math.min(1.0F, (t * t + 2.0F * t) / 3.0F);
            this.st.bowFull = this.st.bow >= 1.0F ? t - 1.0F : 0.0F;
         } else {
            this.st.bow = -1.0F;
         }
      } else {
         this.st.bow = -1.0F;
      }

      for (int i = this.pendingHits.size() - 1; i >= 0; i--) {
         if (now >= this.pendingHits.get(i)) {
            this.pendingHits.remove(i);
            this.markers.hit(null, false, now);
         }
      }
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      long now = ClientClock.nanos();
      this.update(dt, now);
      Ui.textMiddle("Editing", this.x, this.y + 10.0F, 8.4F, Theme.TEXT_MUTED, false);
      float cx = this.x + Ui.width("Editing", 8.4F) + 10.0F;
      int editing = this.element.editing();

      for (int i = 0; i < CrosshairElement.COUNT; i++) {
         this.chipW[i] = 0.0F;
         if (this.element.inUse(i)) {
            String label = CrosshairElement.title(i);
            float cw = Ui.width(label, 8.4F) + 16.0F;
            if (cx + cw > this.x + this.w) {
               break;
            }

            this.chipX[i] = cx;
            this.chipW[i] = cw;
            boolean over = mx >= cx && mx < cx + cw && my >= this.y && my < this.y + 20.0F;
            this.chipHover[i] = Easing.approach(this.chipHover[i], over ? 1.0F : 0.0F, 16.0F, dt);
            if (i == editing) {
               Ui.roundRect(cx, this.y, cw, 20.0F, 10.0F, Theme.ACCENT_SOFT);
               Ui.accentOutline(cx, this.y, cw, 20.0F, 10.0F, 0.9F, accent);
            } else {
               Ui.surface(cx, this.y, cw, 20.0F, 10.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.chipHover[i]));
            }

            Ui.textMiddle(label, cx + 8.0F, this.y + 10.0F, 8.4F, i == editing ? Theme.TEXT : Theme.TEXT_DIM, i == editing);
            cx += cw + 5.0F;
         }
      }

      float bx = this.x;
      float by = this.boxY();
      float bw = this.w;
      float bh = 120.0F;
      this.drawBackground(bx, by, bw, bh);
      CrosshairDef def = this.shownDef();
      Ui.pushScissor(bx, by, bw, bh);
      MenuPixels.begin(bx + bw / 2.0F, by + bh / 2.0F);

      try {
         CrosshairRenderer.Reactions rx = this.element.reactions();
         if (def != null) {
            this.st.scale = CrosshairElement.scaleOf(def);
            float keep = 1.0F - this.markers.skullCover(this.element, now);
            if (def.style.get() == CrosshairStyle.VANILLA) {
               float sf = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
               CrosshairRenderer.drawVanilla(-sf, -sf, sf, keep);
            } else {
               CrosshairRenderer.draw(def, this.st, rx, 0.0F, 0.0F, keep, 1.0F);
            }

            CrosshairRenderer.drawExtras(def, this.st, rx, 0.0F, 0.0F, keep);
         }

         CrosshairDef markerDef = def != null ? def : this.element.defs[0];
         this.markers
            .draw(
               this.element,
               0.0F,
               0.0F,
               CrosshairElement.scaleOf(markerDef),
               def != null ? CrosshairRenderer.reachWithExtras(def, this.st, rx) : 0.0F,
               0.0F,
               now
            );
      } finally {
         MenuPixels.end();
         Ui.popScissor();
      }

      Ui.outline(bx, by, bw, bh, 8.0F, 0.8F, 822083583);
      String what = def == null
         ? (this.zoom ? "Hidden while zoomed" : "Hidden")
         : (
            this.zoom && this.element.zoom.get() == CrosshairElement.ZoomUse.OWN
               ? "Zoomed crosshair"
               : CrosshairElement.title(editing) + (editing == 0 ? " crosshair" : "")
         );
      float lw = Ui.width(what, 7.6F) + 12.0F;
      Ui.roundRect(bx + 5.0F, by + 5.0F, lw, 13.0F, 6.5F, -1946157056);
      Ui.textMiddle(what, bx + 11.0F, by + 11.5F, 7.6F, -251658241, false);
      float gy = this.bgY();
      Ui.textMiddle("Background", bx, gy + 8.0F, 7.6F, Theme.TEXT_MUTED, false);
      float var31 = bx + (Ui.width("Background", 7.6F) + 8.0F);

      for (int ix = 0; ix < BACKGROUNDS.length; ix++) {
         float tw = Ui.width(BACKGROUNDS[ix], 7.6F) + 12.0F;
         this.bgX[ix] = var31;
         this.bgW[ix] = tw;
         boolean over = mx >= var31 && mx < var31 + tw && my >= gy && my < gy + 16.0F;
         this.bgHover[ix] = Easing.approach(this.bgHover[ix], over ? 1.0F : 0.0F, 16.0F, dt);
         if (ix == background) {
            Ui.roundRect(var31, gy, tw, 16.0F, 8.0F, Theme.ACCENT_SOFT);
         } else if (this.bgHover[ix] > 0.01F) {
            Ui.roundRect(var31, gy, tw, 16.0F, 8.0F, ColorUtil.withAlpha(Theme.TEXT, 0.06F * this.bgHover[ix]));
         }

         Ui.textMiddle(BACKGROUNDS[ix], var31 + 6.0F, gy + 8.0F, 7.6F, ix == background ? Theme.TEXT : Theme.TEXT_DIM, false);
         var31 += tw + 2.0F;
      }

      float tx = this.x;
      float ty = this.testsY();

      for (int ix = 0; ix < TESTS.length; ix++) {
         float tw = Ui.width(TESTS[ix], 8.8F) + 20.0F;
         if (tx > this.x && tx + tw > this.x + this.w) {
            tx = this.x;
            ty += 25.0F;
         }

         this.testPills[ix].layout(tx, ty, TESTS[ix]);
         this.testPills[ix].draw(TESTS[ix], mx, my, dt, accent, this.toggled(ix), true);
         tx += tw + 5.0F;
      }

      this.testsH = ty - this.testsY() + 20.0F;
   }

   private boolean toggled(int test) {
      switch (test) {
         case 0:
            return this.target;
         case 1:
         case 2:
         case 3:
         case 4:
         case 6:
         default:
            return false;
         case 5:
            return this.move;
         case 7:
            return this.bowStart >= 0L;
         case 8:
            return this.zoom;
         case 9:
            return this.low;
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         long now = ClientClock.nanos();
         if (my >= this.y && my < this.y + 20.0F) {
            for (int i = 0; i < CrosshairElement.COUNT; i++) {
               if (this.chipW[i] > 0.0F && mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i]) {
                  this.element.edit(i);
                  this.zoom = false;
                  this.bowStart = -1L;
                  ModSounds.gui(Sfx.SELECT);
                  return true;
               }
            }

            return true;
         } else {
            float gy = this.bgY();
            if (my >= gy && my < gy + 16.0F) {
               for (int ix = 0; ix < BACKGROUNDS.length; ix++) {
                  if (mx >= this.bgX[ix] && mx < this.bgX[ix] + this.bgW[ix]) {
                     background = ix;
                     ModSounds.gui(Sfx.SELECT);
                     return true;
                  }
               }

               return true;
            } else {
               for (int ixx = 0; ixx < TESTS.length; ixx++) {
                  if (this.testPills[ixx].contains(mx, my)) {
                     this.test(ixx, now);
                     ModSounds.gui(Sfx.CLICK);
                     return true;
                  }
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   void test(int i, long now) {
      switch (i) {
         case 0:
            this.target = !this.target;
            break;
         case 1:
            this.markers.hit(null, false, now);
            break;
         case 2:
            this.markers.hit(null, true, now);
            break;
         case 3:
            for (int k = 0; k < 4; k++) {
               this.pendingHits.add(now + k * 110000000L);
            }
            break;
         case 4:
            this.markers.kill(now);
            break;
         case 5:
            this.move = !this.move;
            break;
         case 6:
            this.st.click = 1.0F;
            break;
         case 7:
            this.bowStart = this.bowStart >= 0L ? -1L : now;
            break;
         case 8:
            this.zoom = !this.zoom;
            break;
         case 9:
            this.low = !this.low;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (my >= this.y && my < this.y + 20.0F) {
         for (int i = 0; i < CrosshairElement.COUNT; i++) {
            if (this.chipW[i] > 0.0F && mx >= this.chipX[i] && mx < this.chipX[i] + this.chipW[i]) {
               return "Edit the " + CrosshairElement.title(i).toLowerCase(Locale.ROOT) + " crosshair (the options below change it)";
            }
         }

         return "Situational crosshairs set to Own show here too";
      } else {
         for (int ix = 0; ix < TESTS.length; ix++) {
            if (this.testPills[ix].contains(mx, my)) {
               switch (ix) {
                  case 0:
                     return "A player (Player background) or mob under the crosshair: on and off";
                  case 1:
                  case 2:
                  case 4:
                  case 6:
                  default:
                     return null;
                  case 3:
                     return "Four quick hits";
                  case 5:
                     return "As if moving: on and off";
                  case 7:
                     return "Draws a bow: the charge ring fills, then flashes";
                  case 8:
                     return "As if zoomed in: on and off";
                  case 9:
                     return "As if at low health: on and off";
               }
            }
         }

         return null;
      }
   }

   private void drawBackground(float bx, float by, float bw, float bh) {
      DynamicTexture tex = texture(background);
      float aspect = bw / bh;
      float u0 = 0.0F;
      float u1 = 1.0F;
      float v0 = 0.0F;
      float v1 = 1.0F;
      float texAspect = 3.0F;
      if (aspect >= texAspect) {
         float span = texAspect / aspect;
         v0 = 0.5F - span / 2.0F;
         v1 = 0.5F + span / 2.0F;
      } else {
         float span = aspect / texAspect;
         u0 = 0.5F - span / 2.0F;
         u1 = 0.5F + span / 2.0F;
      }

      Ui.roundedImage(tex.getGlTextureId(), bx, by, bw, bh, 8.0F, u0, v0, u1, v1, -1);
      if (background == 5) {
         drawFace(bx + bw / 2.0F, by + bh / 2.0F, bh * 0.78F);
      }
   }

   private static void drawFace(float cx, float cy, float size) {
      Minecraft mc = Minecraft.getMinecraft();
      ResourceLocation skin = SkinHooks.ownSkin();
      mc.getTextureManager().bindTexture(skin);
      ITextureObject t = mc.getTextureManager().getTexture(skin);
      if (t != null) {
         int id = t.getGlTextureId();
         float x = cx - size / 2.0F;
         float y = cy - size / 2.0F;
         Ui.image(id, x, y, size, size, 0.125F, 0.125F, 0.25F, 0.25F, -1);
         float hat = size * 1.125F;
         Ui.image(id, cx - hat / 2.0F, cy - hat / 2.0F, hat, hat, 0.625F, 0.125F, 0.75F, 0.25F, -1);
      }
   }

   private static DynamicTexture texture(int which) {
      if (textures == null) {
         textures = new DynamicTexture[BACKGROUNDS.length];
      }

      if (textures[which] == null) {
         DynamicTexture t = new DynamicTexture(96, 32);
         int[] px = t.getTextureData();

         for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 96; x++) {
               px[y * 96 + x] = pixel(which, x, y);
            }
         }

         t.updateDynamicTexture();
         textures[which] = t;
      }

      return textures[which];
   }

   private static int pixel(int which, int x, int y) {
      float n = hash(x, y, which * 977);
      float n2 = hash(x / 3, y / 3, which * 131 + 7);
      switch (which) {
         case 1: {
            float k = 0.82F + 0.18F * n + (n2 > 0.86F ? -0.12F : 0.0F);
            return rgb(84.0F * k, 146.0F * k, 50.0F * k);
         }
         case 2: {
            float k = 0.35F + 0.1F * n;
            if (n2 > 0.93F && n > 0.4F) {
               return rgb(28.0F, 28.0F, 30.0F);
            }

            return rgb(125.0F * k, 125.0F * k, 130.0F * k);
         }
         case 3: {
            float k = 0.7F + 0.3F * n;
            float glow = Math.max(0.0F, (y - 22) / 10.0F);
            return rgb(110.0F * k + 140.0F * glow, 38.0F * k + 70.0F * glow, 38.0F * k);
         }
         case 4: {
            float k = 0.93F + 0.07F * n;
            return rgb(236.0F * k, 242.0F * k, 252.0F * k);
         }
         default:
            float t = y / 31.0F;
            int r = (int)(110.0F + 60.0F * t);
            int g = (int)(160.0F + 45.0F * t);
            int b = 255;
            if (which == 0 && y >= 2 && y <= 12) {
               float band = 1.0F - Math.abs(y - 7.0F) / 6.0F;
               float c = valueNoise(x / 10.0F, y / 4.0F, 41) * band;
               if (c > 0.42F) {
                  float shade = y > 8 ? 0.93F : 1.0F;
                  return rgb(246.0F * shade, 249.0F * shade, 255.0F * shade);
               }
            }

            return rgb(r, g, b);
      }
   }

   private static float valueNoise(float x, float y, int seed) {
      int xi = (int)Math.floor(x);
      int yi = (int)Math.floor(y);
      float fx = x - xi;
      float fy = y - yi;
      float sx = fx * fx * (3.0F - 2.0F * fx);
      float sy = fy * fy * (3.0F - 2.0F * fy);
      float a = hash(xi, yi, seed);
      float b = hash(xi + 1, yi, seed);
      float c = hash(xi, yi + 1, seed);
      float d = hash(xi + 1, yi + 1, seed);
      float top = a + (b - a) * sx;
      float bottom = c + (d - c) * sx;
      return top + (bottom - top) * sy;
   }

   private static float hash(int x, int y, int seed) {
      int h = x * 374761393 + y * 668265263 + seed * Integer.MAX_VALUE;
      h = (h ^ h >>> 13) * 1274126177;
      h ^= h >>> 16;
      return (h & 65535) / 65535.0F;
   }

   private static int rgb(float r, float g, float b) {
      int ri = Math.max(0, Math.min(255, Math.round(r)));
      int gi = Math.max(0, Math.min(255, Math.round(g)));
      int bi = Math.max(0, Math.min(255, Math.round(b)));
      return 0xFF000000 | ri << 16 | gi << 8 | bi;
   }
}
