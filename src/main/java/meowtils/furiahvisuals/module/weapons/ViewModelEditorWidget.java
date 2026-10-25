package meowtils.furiahvisuals.module.weapons;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class ViewModelEditorWidget extends Widget {
   private final WeaponsModule module;
   private final PageRow row;
   private final WeaponDef def;
   private final Pill toggle = new Pill();
   private final ChipRow buttons = new ChipRow();
   private final List<String> actions = new ArrayList<>();
   private String[] labels = new String[0];
   private boolean[] enabled = new boolean[0];
   private String status;
   private long statusUntil;

   ViewModelEditorWidget(WeaponsModule module, PageRow row, WeaponDef def) {
      this.module = module;
      this.row = row;
      this.def = def;
   }

   private void build() {
      this.actions.clear();
      ArrayList<String> l = new ArrayList<>();
      this.actions.add("swing");
      l.add("Swing");
      if (!this.def.isVanilla()) {
         this.actions.add("block");
         l.add(this.def.isBow() ? "Draw" : "Block");
      }

      this.actions.add("draw");
      l.add(this.def.isBow() ? "Equip" : "Draw");

      for (String inspect : this.def.inspects) {
         this.actions.add("inspect:" + inspect);
         l.add(Inspects.displayName(inspect));
      }

      this.actions.add("reset");
      l.add("Reset");
      this.actions.add("copy");
      l.add("Copy to All Weapons");
      this.labels = l.toArray(new String[0]);
      if (this.enabled.length != this.labels.length) {
         this.enabled = new boolean[this.labels.length];
      }

      boolean live = this.module.editorShowsOnScreen(this.def);

      for (int i = 0; i < this.actions.size(); i++) {
         String a = this.actions.get(i);
         this.enabled[i] = live || a.equals("reset") || a.equals("copy");
      }
   }

   @Override
   public float height() {
      if (!this.module.editing()) {
         return 34.0F;
      } else {
         this.build();
         this.buttons.layout(this.x, this.y + 40.0F, this.w, this.labels, null);
         return 40.0F + this.buttons.height + 10.0F;
      }
   }

   @Override
   public boolean isVisible() {
      return this.row.isVisible();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.module.editorShown();
      boolean editing = this.module.editing();
      String label = editing ? "Done" : "Open Editor";
      this.toggle.layout(this.x + this.w - (Ui.width(label, 8.8F) + 20.0F), this.y + 5.0F, label);
      this.toggle.draw(label, mx, my, dt, accent, true, true);
      float room = this.toggle.x - this.x - 10.0F;
      if (!editing) {
         Ui.text("View Model Editor", this.x, this.y + 5.0F, 10.0F, Theme.TEXT);
         Ui.text(
            Ui.ellipsize("Move, turn and size " + this.def.name + " on your real screen, then try its moves", 7.6F, room, false),
            this.x,
            this.y + 19.0F,
            7.6F,
            Theme.TEXT_DIM
         );
      } else {
         Ui.textBold(Ui.ellipsize("Editing " + this.def.name, 10.0F, room, true), this.x, this.y + 4.0F, 10.0F, Theme.TEXT);
         String note = this.module.editorShowsOnScreen(this.def)
            ? "Your view model is on the right: the settings below move it live."
            : "Hold an item a Vanilla rule covers (a sword) to see Vanilla's view model on the right.";
         if (this.status != null && ClientClock.millis() < this.statusUntil) {
            note = this.status;
         }

         Ui.text(
            Ui.ellipsize(note, 7.6F, room, false),
            this.x,
            this.y + 20.0F,
            7.6F,
            this.status != null && ClientClock.millis() < this.statusUntil ? accent : Theme.TEXT_DIM
         );
         this.build();
         this.buttons.layout(this.x, this.y + 40.0F, this.w, this.labels, null);
         this.buttons.draw(this.labels, null, this.enabled, null, mx, my, dt, accent);
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my) || button != 0) {
         return false;
      } else if (this.toggle.contains(mx, my)) {
         this.module.setEditing(!this.module.editing());
         ModSounds.gui(this.module.editing() ? Sfx.GUI_OPEN : Sfx.GUI_CLOSE);
         return true;
      } else if (!this.module.editing()) {
         return true;
      } else {
         int b = this.buttons.at(mx, my);
         if (b < 0 || b >= this.actions.size()) {
            return true;
         } else if (!this.enabled[b]) {
            ModSounds.gui(Sfx.ERROR);
            return true;
         } else {
            String a = this.actions.get(b);
            if (a.equals("swing")) {
               this.module.editorSwing();
            } else if (a.equals("block")) {
               this.module.editorBlock();
            } else if (a.equals("draw")) {
               this.module.editorDraw(this.def);
            } else if (a.startsWith("inspect:")) {
               if (!this.module.editorInspect(a.substring(8))) {
                  ModSounds.gui(Sfx.ERROR);
                  return true;
               }
            } else {
               if (a.equals("reset")) {
                  this.module.settingsOf(this.def).resetPlacement();
                  this.say(this.def.name + "'s view model is back to its default");
                  ModSounds.gui(Sfx.CONFIRM);
                  return true;
               }

               if (a.equals("copy")) {
                  int n = this.module.copyPlacementToAll(this.def);
                  this.say("Copied to " + n + (n == 1 ? " weapon" : " weapons"));
                  ModSounds.gui(Sfx.SUCCESS);
                  return true;
               }
            }

            ModSounds.gui(Sfx.CLICK);
            return true;
         }
      }
   }

   private void say(String message) {
      this.status = message;
      this.statusUntil = ClientClock.millis() + 3000L;
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else if (this.toggle.contains(mx, my)) {
         return this.module.editing() ? "Back to the whole page" : "Shows only the editor, with your real view model beside it";
      } else {
         int b = this.module.editing() ? this.buttons.at(mx, my) : -1;
         if (b >= 0 && b < this.actions.size()) {
            String a = this.actions.get(b);
            if (a.equals("reset")) {
               return "Puts this weapon's placement, turn, size, field of view and left-handed mode back to its default";
            } else if (a.equals("copy")) {
               return "Gives every other weapon (Vanilla too) this placement, turn, size, field of view and left-handed mode";
            } else {
               return !this.enabled[b] ? "Hold an item a Vanilla rule covers to try it" : "Plays on your real view model (nothing is sent to the server)";
            }
         } else {
            return null;
         }
      }
   }
}
