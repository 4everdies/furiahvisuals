package meowtils.furiahvisuals.module.weapons;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.SegmentedWidget;
import meowtils.furiahvisuals.gui.widget.SliderWidget;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

final class GripSetupWidget extends Widget {
   private final WeaponsModule module;
   private final SliderSetting along = new SliderSetting(
         "gripAlong", "Grip Along", "Where the fist holds it: from one end of its longest side (0%) to the other (100%).", 0.0, 100.0, 12.0, 1.0
      )
      .unit("%");
   private final SliderSetting side = new SliderSetting("gripSide", "Grip Side", "Moves the fist across its width.", -100.0, 100.0, 0.0, 1.0).unit("%");
   private final SliderSetting depth = new SliderSetting("gripDepth", "Grip Depth", "Moves the fist across its thickness.", -100.0, 100.0, 0.0, 1.0).unit("%");
   private final SliderSetting pitch = new SliderSetting(
         "gripPitch", "Pitch", "Tilts it forward or back in the fist (180° turns it end for end).", -180.0, 180.0, 0.0, 1.0
      )
      .unit("°");
   private final SliderSetting spin = new SliderSetting("gripSpin", "Spin", "Turns it about the handle: which side faces forward.", -180.0, 180.0, 0.0, 1.0)
      .unit("°");
   private final SliderSetting roll = new SliderSetting("gripRoll", "Roll", "Leans it sideways in the fist.", -180.0, 180.0, 0.0, 1.0).unit("°");
   private final SliderSetting size = new SliderSetting("gripSize", "Size", "Its size in your hand.", 0.25, 4.0, 1.0, 0.05).unit("x");
   private final ModeSetting<GripSetupWidget.Hands> hands = new ModeSetting<>(
      "gripHands", "Hands", "One hand, or both.", GripSetupWidget.Hands.class, GripSetupWidget.Hands.ONE
   );
   private final ModeSetting<WeaponImport.AnimSet> set = new ModeSetting<>(
      "gripSet", "Animations", "How it swings and which inspects it plays.", WeaponImport.AnimSet.class, WeaponImport.AnimSet.SWORD
   );
   private final Widget[] rows = new Widget[]{
      new SliderWidget(this.along),
      new SliderWidget(this.side),
      new SliderWidget(this.depth),
      new SliderWidget(this.pitch),
      new SliderWidget(this.spin),
      new SliderWidget(this.roll),
      new SliderWidget(this.size),
      new SegmentedWidget(this.hands),
      new SegmentedWidget(this.set)
   };
   private final Pill savePill = new Pill();
   private final Pill revertPill = new Pill();
   private Widget active;
   private String loadedId;
   private int loadedVersion = -1;
   private JsonObject json;
   private String applied;
   private boolean dirty;
   private String status;
   private boolean statusError;

   GripSetupWidget(WeaponsModule module) {
      this.module = module;
   }

   @Override
   public boolean isVisible() {
      return this.module.selectedDef().isImported() && !this.module.editing();
   }

   @Override
   public float height() {
      float h = 18.0F;

      for (Widget r : this.rows) {
         h += r.height();
      }

      return h + 44.0F;
   }

   private void sync() {
      WeaponDef def = this.module.selectedDef();
      if (!def.isImported()) {
         this.loadedId = null;
      } else if (!def.id.equals(this.loadedId) || this.module.library.version() != this.loadedVersion) {
         this.loadedId = def.id;
         this.loadedVersion = this.module.library.version();
         this.json = read(def.folder);
         this.dirty = false;
         this.status = null;
         if (this.json != null && this.json.has("import") && this.json.get("import").isJsonObject()) {
            JsonObject g = this.json.getAsJsonObject("import");
            float[] off = WeaponImport.floats(g, "offset", 2);
            float[] turn = WeaponImport.floats(g, "turn", 3);
            this.along.set(g.get("along").getAsDouble() * 100.0);
            this.side.set(off[0] * 100.0);
            this.depth.set(off[1] * 100.0);
            this.pitch.set(turn[0]);
            this.spin.set(turn[1]);
            this.roll.set(turn[2]);
            this.size.set(g.get("size").getAsDouble());
            this.hands.set(g.get("hands").getAsInt() >= 2 ? GripSetupWidget.Hands.TWO : GripSetupWidget.Hands.ONE);
            this.set.set(WeaponImport.AnimSet.of(g.get("set").getAsString()));
            this.applied = this.values();
         } else {
            this.status = this.json == null ? "Its weapon.json can't be read." : "Made by hand (no Grip Setup values): edit its weapon.json, then Reload.";
            this.statusError = this.json == null;
            this.json = null;
         }
      }
   }

   private String values() {
      return String.format(
         Locale.ROOT,
         "%.3f|%.3f|%.3f|%.1f|%.1f|%.1f|%.3f|%s|%s",
         this.along.get(),
         this.side.get(),
         this.depth.get(),
         this.pitch.get(),
         this.spin.get(),
         this.roll.get(),
         this.size.get(),
         this.hands.get(),
         this.set.get()
      );
   }

   private void store() {
      JsonObject g = this.json.getAsJsonObject("import");
      g.addProperty("along", this.along.get() / 100.0);
      g.add("offset", WeaponImport.array((float)(this.side.get() / 100.0), (float)(this.depth.get() / 100.0)));
      g.add("turn", WeaponImport.array(this.pitch.getFloat(), this.spin.getFloat(), this.roll.getFloat()));
      g.addProperty("size", this.size.get());
      g.addProperty("hands", this.hands.get() == GripSetupWidget.Hands.TWO ? 2 : 1);
      g.addProperty("set", this.set.get().name().toLowerCase(Locale.ROOT));
      WeaponImport.applyGrip(this.json);
   }

   private void preview() {
      String now = this.values();
      if (!now.equals(this.applied)) {
         this.applied = now;
         this.dirty = true;
         this.status = null;

         try {
            this.store();
            WeaponDef old = this.module.library.get(this.loadedId);
            if (old == null) {
               return;
            }

            JsonObject copy = new JsonParser().parse(this.json.toString()).getAsJsonObject();
            copy.addProperty("id", this.loadedId);
            this.module.library.replaceImported(WeaponDef.imported(copy, old.folder));
         } catch (RuntimeException var4) {
            this.status = "Can't apply these values (" + var4.getMessage() + ").";
            this.statusError = true;
         }
      }
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.sync();
      WeaponDef def = this.module.selectedDef();
      String head = def.name + (def.folder != null ? "  ·  folder \"" + def.folder.getName() + "\"" : "");
      Ui.text(Ui.ellipsize(head, 8.6F, this.w, false), this.x, this.y + 3.0F, 8.6F, Theme.TEXT_DIM);
      float ry = this.y + 18.0F;

      for (Widget r : this.rows) {
         r.x = this.x;
         r.y = ry;
         r.w = this.w;
         if (this.json != null) {
            r.render(mx, my, dt, accent);
         }

         ry += r.height();
      }

      if (this.json != null) {
         this.preview();
      }

      float py = ry + 8.0F;
      this.savePill.layout(this.x, py, "Save");
      this.revertPill.layout(this.savePill.x + this.savePill.w + 6.0F, py, "Revert");
      this.savePill.draw("Save", mx, my, dt, accent, true, this.json != null && this.dirty);
      this.revertPill.draw("Revert", mx, my, dt, accent, false, this.json != null && this.dirty);
      String s = this.status != null ? this.status : (this.dirty ? "Not saved yet: the preview shows it." : (this.json != null ? "Saved." : null));
      if (s != null) {
         float sx = this.revertPill.x + this.revertPill.w + 10.0F;
         Ui.text(Ui.ellipsize(s, 8.2F, this.x + this.w - sx, false), sx, py + 6.0F, 8.2F, this.statusError ? Theme.DANGER : Theme.TEXT_MUTED);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         return false;
      } else {
         if (this.json != null) {
            for (Widget r : this.rows) {
               if (r.contains(mx, my) && r.mouseDown(mx, my, button)) {
                  this.active = r;
                  return true;
               }
            }
         }

         if (button == 0 && this.json != null && this.dirty && this.savePill.contains(mx, my)) {
            this.save();
            return true;
         } else if (button == 0 && this.json != null && this.dirty && this.revertPill.contains(mx, my)) {
            this.loadedVersion = -1;
            this.module.library.reloadImported();
            this.status = "Back to what was saved.";
            this.statusError = false;
            return true;
         } else {
            return true;
         }
      }
   }

   @Override
   public void mouseDrag(float mx, float my) {
      if (this.active != null) {
         this.active.mouseDrag(mx, my);
      }
   }

   @Override
   public void mouseUp(float mx, float my) {
      if (this.active != null) {
         this.active.mouseUp(mx, my);
         this.active = null;
      }
   }

   @Override
   public boolean isDragging() {
      return this.active != null && this.active.isDragging();
   }

   @Override
   public String tooltip(float mx, float my) {
      if (this.json == null) {
         return null;
      } else {
         for (Widget r : this.rows) {
            String t = r.tooltip(mx, my);
            if (t != null) {
               return t;
            }
         }

         return null;
      }
   }

   private void save() {
      WeaponDef def = this.module.library.get(this.loadedId);
      if (def != null && def.folder != null) {
         try {
            this.store();
            WeaponImport.write(new File(def.folder, "weapon.json"), this.json);
            this.dirty = false;
            String id = this.loadedId;
            this.module.library.reloadImported();
            this.module.select(id);
            this.status = "Saved.";
            this.statusError = false;
            WeaponImport.reload(this.module, true);
         } catch (RuntimeException | IOException var3) {
            FuriahsVisuals.LOG.warn("Weapons: could not save {}", new Object[]{def.folder, var3});
            this.status = "Could not save its weapon.json (" + var3.getMessage() + ").";
            this.statusError = true;
         }
      }
   }

   private static JsonObject read(File folder) {
      if (folder == null) {
         return null;
      } else {
         try (InputStreamReader r = new InputStreamReader(Files.newInputStream(new File(folder, "weapon.json").toPath()), StandardCharsets.UTF_8)) {
            return new JsonParser().parse(r).getAsJsonObject();
         } catch (RuntimeException | IOException var16) {
            return null;
         }
      }
   }

   static enum Hands implements Displayable {
      ONE("One Hand", "Held in the right hand (and both hands with Dual Wield)"),
      TWO("Two Hands", "A heavy weapon: the left fist further down the handle");

      private final String name;
      private final String description;

      private Hands(String name, String description) {
         this.name = name;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.name;
      }

      @Override
      public String description() {
         return this.description;
      }
   }
}
