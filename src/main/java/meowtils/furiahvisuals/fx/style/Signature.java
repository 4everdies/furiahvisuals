package meowtils.furiahvisuals.fx.style;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.render.material.Material;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ModeSetting;

public final class Signature {
   private final List<ColorSetting> slots = new ArrayList<>();
   private final List<Integer> colors = new ArrayList<>();
   private ModeSetting<Material> material;
   private Material defaultMaterial;

   public void add(ColorSetting slot, int color) {
      int at = this.slots.indexOf(slot);
      if (at >= 0) {
         this.colors.set(at, color);
      } else {
         this.slots.add(slot);
         this.colors.add(color);
      }
   }

   public void material(ModeSetting<Material> setting, Material made) {
      this.material = setting;
      this.defaultMaterial = made;
   }

   public boolean hasMaterial() {
      return this.material != null;
   }

   public boolean isEmpty() {
      return this.slots.isEmpty() && this.material == null;
   }

   public void use() {
      for (int i = 0; i < this.slots.size(); i++) {
         ColorSetting s = this.slots.get(i);
         s.setMode(ColorSetting.Mode.STATIC);
         s.setColorA(this.colors.get(i));
      }

      if (this.material != null) {
         this.material.set(this.defaultMaterial);
      }
   }

   public boolean inUse() {
      for (int i = 0; i < this.slots.size(); i++) {
         ColorSetting s = this.slots.get(i);
         if (s.getMode() != ColorSetting.Mode.STATIC || s.getColorA() != this.colors.get(i)) {
            return false;
         }
      }

      return this.material == null || this.material.get() == this.defaultMaterial;
   }

   public int[] colors() {
      int n = this.colors.size() + (this.material != null ? 1 : 0);
      int[] out = new int[n];

      for (int i = 0; i < this.colors.size(); i++) {
         out[i] = this.colors.get(i);
      }

      if (this.material != null) {
         out[n - 1] = this.defaultMaterial.swatch();
      }

      return out;
   }

   public int[] slotColors() {
      int[] out = new int[this.colors.size()];

      for (int i = 0; i < out.length; i++) {
         out[i] = this.colors.get(i);
      }

      return out;
   }
}
