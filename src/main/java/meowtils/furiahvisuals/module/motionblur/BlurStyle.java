package meowtils.furiahvisuals.module.motionblur;

import meowtils.furiahvisuals.setting.Displayable;

public enum BlurStyle implements Displayable {
   CLASSIC("Classic", "Frame blending: fading trails", true, false),
   REALISTIC("Realistic", "Camera blur, like shader packs", false, true),
   HYBRID("Hybrid", "Camera blur plus faint trails", true, true);

   private final String name;
   private final String description;
   private final boolean trails;
   private final boolean camera;

   private BlurStyle(String name, String description, boolean trails, boolean camera) {
      this.name = name;
      this.description = description;
      this.trails = trails;
      this.camera = camera;
   }

   @Override
   public String displayName() {
      return this.name;
   }

   @Override
   public String description() {
      return this.description;
   }

   public boolean trails() {
      return this.trails;
   }

   public boolean camera() {
      return this.camera;
   }
}
