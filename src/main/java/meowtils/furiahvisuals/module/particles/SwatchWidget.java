package meowtils.furiahvisuals.module.particles;

import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;

final class SwatchWidget extends Widget {
   private static final float TILE = 34.0F;
   private static final float GAP = 6.0F;
   private static final String[] LABELS = new String[]{"Vanilla", "Native", "Custom"};
   private final ParticlesModule module;
   private final float[] hover = new float[6];

   SwatchWidget(ParticlesModule module) {
      this.module = module;
   }

   private float blockW() {
      return 114.0F;
   }

   private boolean sideBySide() {
      return 2.0F * this.blockW() + 26.0F <= this.w;
   }

   private String hint() {
      boolean crit = this.plainNative(1);
      boolean sharp = this.plainNative(2);
      if (!crit && !sharp) {
         return null;
      } else {
         String which = crit && sharp ? "crit and sharpness sprites are" : (crit ? "crit sprites are" : "sharpness sprites are");
         return "Your pack's " + which + " white — Native will show white particles.";
      }
   }

   private boolean plainNative(int kind) {
      return this.module.colorSource(kind).get() == ParticlesModule.ColorSource.NATIVE
         && this.module.sprites.isPlain(kind == 2)
         && this.module.images(kind).activeCount() == 0;
   }

   @Override
   public float height() {
      float blocks = this.sideBySide() ? 1.0F : 2.0F;
      return blocks * 64.0F + (this.hint() != null ? 14.0F : 0.0F) + 2.0F;
   }

   private float tileX(int kind, int source) {
      float bx = this.sideBySide() ? this.x + (kind - 1) * (this.blockW() + 26.0F) : this.x;
      return bx + source * 40.0F;
   }

   private float tileY(int kind) {
      return this.sideBySide() ? this.y + 14.0F : this.y + 14.0F + (kind - 1) * 64.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      Minecraft mc = Minecraft.getMinecraft();
      TextureManager tm = mc.getTextureManager();
      tm.bindTexture(ParticleSprites.PARTICLES);
      ITextureObject particles = tm.getTexture(ParticleSprites.PARTICLES);
      int particlesTex = particles == null ? -1 : particles.getGlTextureId();
      double t = ColorSetting.clock();
      float life = (float)(t % 1.6 / 1.6);

      for (int kind = 1; kind <= 2; kind++) {
         ModeSetting<ParticlesModule.ColorSource> setting = this.module.colorSource(kind);
         int selected = setting.get().ordinal();
         float titleY = this.tileY(kind) - 14.0F;
         Ui.textBold(kind == 1 ? "CRIT" : "SHARPNESS", this.tileX(kind, 0), titleY + 1.0F, 7.6F, Theme.TEXT_DIM);
         ParticleRenderer.ImageSet images = this.module.imageSet(kind);
         int imageTex = images.textures.length > 0 ? images.textures[0] : -1;
         int sprite = kind == 2 ? 66 : 65;
         float u0 = sprite % 16 / 16.0F;
         float v0 = sprite / 16 / 16.0F;

         for (int s = 0; s < 3; s++) {
            float tx = this.tileX(kind, s);
            float ty = this.tileY(kind);
            int h = (kind - 1) * 3 + s;
            boolean over = mx >= tx && mx < tx + 34.0F && my >= ty && my < ty + 34.0F + 14.0F;
            this.hover[h] = Easing.approach(this.hover[h], over ? 1.0F : 0.0F, 16.0F, dt);
            boolean sel = s == selected;
            Ui.roundRect(tx, ty, 34.0F, 34.0F, 8.0F, ColorUtil.lerp(-15724008, -14933974, this.hover[h]));
            if (sel) {
               Ui.glow(tx + 17.0F, ty + 17.0F, 54.4F, ColorUtil.withAlpha(accent, 0.18F));
               Ui.accentOutline(tx, ty, 34.0F, 34.0F, 8.0F, 1.2F, accent);
            } else {
               Ui.outline(tx, ty, 34.0F, 34.0F, 8.0F, 0.8F, ColorUtil.mulAlpha(Theme.OUTLINE_HOVER, 0.6F + 0.4F * this.hover[h]));
            }

            int tint = this.tint(kind, s, life);
            float pad = 5.0F;
            if (imageTex > 0) {
               Ui.image(imageTex, tx + pad, ty + pad, 34.0F - 2.0F * pad, 34.0F - 2.0F * pad, 0.0F, 0.0F, 1.0F, 1.0F, tint);
            } else {
               Ui.image(particlesTex, tx + pad, ty + pad, 34.0F - 2.0F * pad, 34.0F - 2.0F * pad, u0, v0, u0 + 0.0625F, v0 + 0.0625F, tint);
            }

            String label = LABELS[s];
            float lw = sel ? Ui.widthBold(label, 8.0F) : Ui.width(label, 8.0F);
            Ui.textMiddle(label, tx + (34.0F - lw) / 2.0F, ty + 34.0F + 8.0F, 8.0F, sel ? Theme.TEXT : Theme.TEXT_DIM, sel);
         }
      }

      String hint = this.hint();
      if (hint != null) {
         float hy = this.sideBySide() ? this.y + 14.0F + 34.0F + 18.0F : this.y + 128.0F + 2.0F;
         Ui.text(Ui.ellipsize(hint, 8.2F, this.w, false), this.x, hy, 8.2F, Theme.TEXT_MUTED);
      }
   }

   private int tint(int kind, int source, float life) {
      if (source == 1) {
         return -1;
      } else if (source == 2) {
         return this.module.customColor(kind).getColor(life) | 0xFF000000;
      } else {
         float age = life * 10.0F;
         float grey = 0.75F;
         float r = 0.75F;
         float g = grey * (float)Math.pow(0.96, age);
         float b = grey * (float)Math.pow(0.9, age);
         if (kind == 2) {
            r *= 0.3F;
            g *= 0.8F;
         }

         return ColorUtil.argb(255, (int)(r * 255.0F), (int)(g * 255.0F), (int)(b * 255.0F));
      }
   }

   private int hit(int kind, float mx, float my) {
      for (int s = 0; s < 3; s++) {
         float tx = this.tileX(kind, s);
         float ty = this.tileY(kind);
         if (mx >= tx && mx < tx + 34.0F && my >= ty && my < ty + 34.0F + 14.0F) {
            return s;
         }
      }

      return -1;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (this.contains(mx, my) && button == 0) {
         for (int kind = 1; kind <= 2; kind++) {
            int s = this.hit(kind, mx, my);
            if (s >= 0) {
               this.module.colorSource(kind).set(ParticlesModule.ColorSource.values()[s]);
               return true;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      for (int kind = 1; kind <= 2; kind++) {
         int s = this.hit(kind, mx, my);
         if (s >= 0) {
            return ParticlesModule.ColorSource.values()[s].description();
         }
      }

      return null;
   }
}
