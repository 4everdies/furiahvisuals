package meowtils.furiahvisuals.module.hit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.Lifecycle;
import meowtils.furiahvisuals.fx.pass.FxPass;
import meowtils.furiahvisuals.fx.style.Signature;
import meowtils.furiahvisuals.fx.style.StyleMaterials;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.material.Finish;
import meowtils.furiahvisuals.render.material.Material;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;
import meowtils.furiahvisuals.setting.SliderSetting;

public abstract class HitStyleBase implements SettingStyle {
   protected static final double TWO_PI = Math.PI * 2;
   protected final HitEffectModule module;
   protected final HitStyle type;
   public final ColorSetting color;
   private final float baseSeconds;
   private final List<Setting> settings = new ArrayList<>();
   private final Signature signature = new Signature();
   private final List<SliderSetting> lookTargets = new ArrayList<>();
   private boolean colorLast;

   protected HitStyleBase(HitEffectModule module, HitStyle type, float baseSeconds, ColorSetting color) {
      this.module = module;
      this.type = type;
      this.baseSeconds = baseSeconds;
      this.color = color;
      color.<Setting>group(type.displayName()).ownedBy(this);
   }

   protected final <S extends Setting> S setting(S setting) {
      setting.group(this.type.displayName());
      setting.ownedBy(this);
      this.settings.add(setting);
      return setting;
   }

   public final Signature signature() {
      return this.signature;
   }

   protected final void signature(ColorSetting slot, int color) {
      this.signature.add(slot, color);
   }

   protected final <S extends SliderSetting> S look(S slider) {
      this.lookTargets.add(slider);
      return slider;
   }

   public final List<SliderSetting> lookTargets() {
      return Collections.unmodifiableList(this.lookTargets);
   }

   protected final ModeSetting<Material> materialSetting(String key, Material made, String description) {
      ModeSetting<Material> m = this.setting(StyleMaterials.material(key, made, description));
      this.signature.material(m, made);
      return m;
   }

   protected final ModeSetting<Finish> finishSetting(String key, Finish finish, ModeSetting<Material> material) {
      return this.setting(StyleMaterials.finish(key, finish, material));
   }

   @Override
   public final String styleName() {
      return this.type.displayName();
   }

   @Override
   public final boolean isShown() {
      return this.module.style.get() == this.type;
   }

   @Override
   public final boolean isActive() {
      return this.isShown();
   }

   @Override
   public final boolean isMultiple() {
      return false;
   }

   @Override
   public final void show() {
      this.module.style.set(this.type);
   }

   protected final void colorLast() {
      this.colorLast = true;
   }

   public final List<Setting> getSettings() {
      ArrayList<Setting> all = new ArrayList<>();
      if (!this.colorLast) {
         all.add(this.color);
      }

      all.addAll(this.settings);
      if (this.colorLast) {
         all.add(this.color);
      }

      return Collections.unmodifiableList(all);
   }

   public final HitStyle getType() {
      return this.type;
   }

   public final Lifecycle lifecycle() {
      return this.type.lifecycle();
   }

   public final float baseSeconds() {
      return this.baseSeconds;
   }

   public void start(HitFx fx) {
   }

   public void finish(HitFx fx) {
   }

   public void render(HitFx fx, FxFrame f) {
   }

   public int passes(HitFx fx) {
      return 0;
   }

   public void renderPass(HitFx fx, FxFrame f, FxPass pass) {
   }

   public String stateForTest(HitFx fx) {
      return null;
   }

   protected static double scale(HitFx fx) {
      return fx.size * Math.max(0.5, Math.min(2.5, fx.hit.height / 1.8));
   }

   protected static int alpha(int color, float alpha) {
      return ColorUtil.mulAlpha(color, alpha);
   }

   protected static int core(int color, float alpha) {
      return ColorUtil.mulAlpha(ColorUtil.core(color), alpha);
   }

   protected static float loop(float p) {
      p -= (float)Math.floor(p);
      return 1.0F - Math.abs(2.0F * p - 1.0F);
   }
}
