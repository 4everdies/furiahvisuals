package meowtils.furiahvisuals.module.glow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.PresetSetting;

final class GlowPresets extends PresetSetting {
   private final GlowModule module;
   private final Map<String, PresetCodec.Preset> decoded = new HashMap<>();
   private final List<PresetSetting.Chip> chips = new ArrayList<>();
   private boolean chipsDirty = true;

   GlowPresets(GlowModule module) {
      super("preset", "Presets", "Shape, motion and animation in one click; colors stay.", GlowModule.BUILT_IN[0].id);
      this.module = module;
      this.addListener(new Runnable() {
         @Override
         public void run() {
            GlowPresets.this.chipsDirty = true;
         }
      });
   }

   @Override
   public List<PresetSetting.Chip> chips() {
      if (!this.chipsDirty) {
         return this.chips;
      } else {
         this.chipsDirty = false;
         this.chips.clear();

         for (GlowModule.BuiltIn b : GlowModule.BUILT_IN) {
            this.chips.add(new PresetSetting.Chip(b.id, b.name, b.description, b.colorA, b.colorB, b.mode, false));
         }

         for (String[] u : this.userPresets()) {
            PresetCodec.Preset p = this.decode(u[1]);
            ColorSetting.Mode mode = p != null && p.hasColor ? p.mode : ColorSetting.Mode.GLOBAL;
            this.chips
               .add(
                  new PresetSetting.Chip(
                     "user:" + u[0], u[0], "Your preset (right-click: copy its code)", p != null ? p.colorA : -1, p != null ? p.colorB : -1, mode, true
                  )
               );
         }

         return this.chips;
      }
   }

   @Override
   public String current() {
      String id = this.getSelected();
      int[] steps = this.stepsOf(id);
      return steps != null && Arrays.equals(steps, this.module.currentSteps()) ? id : "custom";
   }

   int[] stepsOf(String id) {
      if (id == null) {
         return null;
      } else {
         GlowModule.BuiltIn b = GlowModule.builtIn(id);
         if (b != null) {
            return b.steps;
         } else {
            PresetCodec.Preset p = this.decode(this.userCode(id));
            return p == null ? null : this.module.completeSteps(p.steps);
         }
      }
   }

   String nameOf(String id) {
      GlowModule.BuiltIn b = GlowModule.builtIn(id);
      if (b != null) {
         return b.name;
      } else {
         return this.userCode(id) != null ? id.substring("user:".length()) : null;
      }
   }

   @Override
   public void apply(String id) {
      int[] steps = this.stepsOf(id);
      if (steps != null) {
         this.module.applySteps(steps);
         this.select(id);
      }
   }

   @Override
   public void applySuggestedColor(String id) {
      GlowModule.BuiltIn b = GlowModule.builtIn(id);
      if (b != null) {
         this.module.applyColor(b.mode, b.colorA, b.colorB, b.direction.ordinal());
      } else {
         PresetCodec.Preset p = this.decode(this.userCode(id));
         if (p != null && p.hasColor) {
            this.module.applyColor(p.mode, p.colorA, p.colorB, p.direction);
         }
      }
   }

   @Override
   public String saveCurrent() {
      String name = this.freeName("My preset");
      String id = this.putUser(name, this.module.capture(name));
      this.select(id);
      return id;
   }

   @Override
   public String exportCode() {
      String id = this.current();
      String name = "custom".equals(id) ? "Custom" : this.nameOf(id);
      return this.module.capture(name == null ? "Custom" : name);
   }

   @Override
   public String codeOf(String id) {
      String code = this.userCode(id);
      if (code != null) {
         return code;
      } else {
         GlowModule.BuiltIn b = GlowModule.builtIn(id);
         return b == null ? null : this.module.encodeBuiltIn(b);
      }
   }

   @Override
   public String importCode(String text) {
      PresetCodec.Preset p = PresetCodec.decode(text);
      if (p == null) {
         return "The clipboard holds no Glow preset code";
      } else {
         String base = !p.name.isEmpty() && !p.name.equalsIgnoreCase("Custom") ? p.name : "Pasted preset";
         String name = this.freeName(base);
         String code = text.replaceAll("\\s+", "");
         String id = this.putUser(name, code);
         this.apply(id);
         return "Added \"" + name + "\"";
      }
   }

   private PresetCodec.Preset decode(String code) {
      if (code == null) {
         return null;
      } else {
         PresetCodec.Preset p = this.decoded.get(code);
         if (p == null && !this.decoded.containsKey(code)) {
            p = PresetCodec.decode(code);
            this.decoded.put(code, p);
         }

         return p;
      }
   }
}
