package meowtils.furiahvisuals.module.weapons;

import java.util.List;
import java.util.Random;
import meowtils.furiahvisuals.core.Combat;
import meowtils.furiahvisuals.core.Keybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;

final class Inspects {
   private final WeaponsModule module;
   private final Random random = new Random();
   private float autoTimer;
   private float autoTarget = -1.0F;
   private int order;
   private String last;
   private int lastHurt;
   private int lastSlot = -1;
   private boolean afterDraw;

   Inspects(WeaponsModule module) {
      this.module = module;
   }

   void tick(Minecraft mc) {
      EntityPlayerSP p = mc.thePlayer;
      if (p != null) {
         KeyBinding key = Keybinds.inspectWeapon;
         ViewModels.Hand hand = this.module.activeHand();
         int slot = p.inventory.currentItem;
         if (this.lastSlot >= 0 && slot != this.lastSlot && hand != null) {
            this.cancel(hand);
         }

         this.lastSlot = slot;
         if (p.hurtTime > this.lastHurt && p.hurtTime > 0 && this.module.cancelOnDamage.get() && hand != null) {
            this.cancel(hand);
         }

         this.lastHurt = p.hurtTime;

         while (key != null && key.isPressed()) {
            if (hand != null) {
               if (hand.anim.inspecting()) {
                  WeaponOptions.KeyAgain k = this.module.keyAgain.get();
                  if (k == WeaponOptions.KeyAgain.CANCEL) {
                     this.cancel(hand);
                  } else {
                     this.start(hand, k == WeaponOptions.KeyAgain.NEXT);
                  }
               } else if (allowedByKey(p)) {
                  this.start(hand, false);
               }
            }
         }

         WeaponOptions.AutoInspect auto = this.module.autoInspect.get();
         if (auto == WeaponOptions.AutoInspect.OFF || hand == null) {
            this.autoTimer = 0.0F;
            this.autoTarget = -1.0F;
         } else if (hand.anim.inspecting() || hand.anim.drawing()) {
            this.autoTimer = 0.0F;
         } else if (this.idle(mc, p)) {
            if (this.autoTarget < 0.0F) {
               this.autoTarget = this.nextWait(auto);
            }

            this.autoTimer += 0.05F;
            if (this.autoTimer >= this.autoTarget) {
               this.start(hand, false);
               this.autoTimer = 0.0F;
               this.autoTarget = this.nextWait(auto);
            }
         }

         if (this.afterDraw && hand != null && !hand.anim.drawing()) {
            this.afterDraw = false;
            if (allowedByKey(p)) {
               this.start(hand, false);
            }
         }
      }
   }

   private float nextWait(WeaponOptions.AutoInspect auto) {
      if (auto == WeaponOptions.AutoInspect.EVERY) {
         return this.module.autoEvery.getFloat();
      } else {
         float lo = Math.min(this.module.autoMin.getFloat(), this.module.autoMax.getFloat());
         float hi = Math.max(this.module.autoMin.getFloat(), this.module.autoMax.getFloat());
         return lo + this.random.nextFloat() * (hi - lo);
      }
   }

   private boolean allowedByKey(EntityPlayerSP p) {
      return !p.isUsingItem() && !p.isBlocking() && !this.module.guarding();
   }

   private boolean idle(Minecraft mc, EntityPlayerSP p) {
      if (mc.currentScreen != null || p.isUsingItem() || p.isBlocking() || this.module.guarding() || Combat.fighting() || p.isSwingInProgress) {
         return false;
      } else {
         return this.module.autoNotSprinting.get() && p.isSprinting() ? false : !this.module.autoFirstPersonOnly.get() || mc.gameSettings.thirdPersonView == 0;
      }
   }

   void drawn(ViewModels.Hand hand) {
      if (this.module.afterEquip.get() && this.random.nextFloat() * 100.0F < this.module.afterEquipChance.getFloat()) {
         this.afterDraw = true;
      }
   }

   void cancel(ViewModels.Hand hand) {
      if (hand != null) {
         hand.anim.cancelInspect();
      }

      ViewModels.Hand other;
      if ((other = this.module.dualLeftHand()) != null && other != hand) {
         other.anim.cancelInspect();
      }
   }

   boolean start(ViewModels.Hand hand, boolean next) {
      WeaponDef def = hand.def;
      WeaponSettings s = hand.settings;
      if (def != null && s != null) {
         List<String> enabled = s.inspects.enabled();
         if (enabled.isEmpty()) {
            return false;
         } else {
            WeaponOptions.InspectOrder o = s.inspectOrder.get();
            String id = o == WeaponOptions.InspectOrder.ALWAYS_SAME
               ? enabled.get(0)
               : (
                  o == WeaponOptions.InspectOrder.IN_ORDER
                     ? enabled.get(this.order++ % enabled.size())
                     : this.weighted(s, enabled, !next && enabled.size() <= 1 ? null : this.last)
               );
            if (!this.play(hand, id)) {
               return false;
            } else {
               this.last = id;
               return true;
            }
         }
      } else {
         return false;
      }
   }

   boolean play(ViewModels.Hand hand, String id) {
      Clip clip = this.clipFor(hand.def, id);
      if (clip == null) {
         return false;
      } else {
         hand.anim.startInspect(clip, this.module.inspectSpeed.getFloat());
         ViewModels.Hand other = this.module.dualLeftHand();
         Clip c;
         if (other != null && other != hand && other.def != null && (c = this.clipFor(other.def, id)) != null) {
            other.anim.startInspect(c, this.module.inspectSpeed.getFloat());
         }

         return true;
      }
   }

   private String weighted(WeaponSettings s, List<String> enabled, String not) {
      int total = 0;

      for (String e : enabled) {
         if (!e.equals(not)) {
            total += s.inspects.weight(e);
         }
      }

      if (total <= 0) {
         return enabled.get(0);
      } else {
         int r = this.random.nextInt(total);

         for (String ex : enabled) {
            if (!ex.equals(not) && (r -= s.inspects.weight(ex)) < 0) {
               return ex;
            }
         }

         return enabled.get(0);
      }
   }

   static String displayName(String id) {
      String[] pairs = new String[]{
         "flip_present", "Flip & Present", "toss_catch", "Toss & Catch", "pull_start", "Pull-Start", "retract_reignite", "Retract & Re-ignite"
      };

      for (int i = 0; i < pairs.length; i += 2) {
         if (pairs[i].equals(id)) {
            return pairs[i + 1];
         }
      }

      StringBuilder sb = new StringBuilder();

      for (String word : id.split("_")) {
         if (!word.isEmpty()) {
            if (sb.length() > 0) {
               sb.append(' ');
            }

            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
         }
      }

      return sb.toString();
   }

   Clip clipFor(WeaponDef def, String inspect) {
      return this.module.clips.find(inspect, def.id, def.animations, "inspect");
   }

   boolean playClip(ViewModels.Hand hand, String name) {
      Clip c = this.module.clips.get(name);
      if (c != null && hand != null) {
         hand.anim.startInspect(c, this.module.inspectSpeed.getFloat());
         return true;
      } else {
         return false;
      }
   }
}
