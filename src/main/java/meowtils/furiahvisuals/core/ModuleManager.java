package meowtils.furiahvisuals.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import meowtils.furiahvisuals.compat.EntityViewRenderEvent;
import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.fx.pass.FxPasses;
import meowtils.furiahvisuals.module.Module;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public final class ModuleManager {
   private final List<Module> modules = new ArrayList<>();
   private final Set<String> reportedErrors = new HashSet<>();

   public void register(Module module) {
      for (Module m : this.modules) {
         if (m.getId().equals(module.getId())) {
            throw new IllegalArgumentException("Duplicate module id " + module.getId());
         }
      }

      this.modules.add(module);
   }

   public List<Module> getModules() {
      return Collections.unmodifiableList(this.modules);
   }

   public Module get(String id) {
      for (Module m : this.modules) {
         if (m.getId().equals(id)) {
            return m;
         }
      }

      for (Module mx : this.modules) {
         for (String old : mx.getLegacyIds()) {
            if (old.equals(id)) {
               return mx;
            }
         }
      }

      return null;
   }

   public void tick() {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onTick();
            } catch (RuntimeException | LinkageError var4) {
               this.report(m, "tick", var4);
            }
         }
      }
   }

   public void renderWorld(float partialTicks, boolean hudHidden) {
      FxPasses.beginFrame();

      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled() && (!hudHidden || m.drawsWithHudHidden())) {
            try {
               m.onPrepareWorld(partialTicks);
            } catch (RuntimeException | LinkageError var9) {
               this.report(m, "prepare", var9);
            }
         }
      }

      try {
         FxPasses.solid();
      } catch (RuntimeException | LinkageError var8) {
         this.reportPasses("solid pass", var8);
      }

      for (int ix = 0; ix < this.modules.size(); ix++) {
         Module m = this.modules.get(ix);
         if (m.isEnabled() && (!hudHidden || m.drawsWithHudHidden())) {
            try {
               m.onRenderWorld(partialTicks);
            } catch (RuntimeException | LinkageError var7) {
               this.report(m, "render", var7);
            }
         }
      }

      try {
         FxPasses.translucentAndGlow();
      } catch (RuntimeException | LinkageError var6) {
         this.reportPasses("translucent and glow passes", var6);
      }
   }

   private void reportPasses(String what, Throwable e) {
      if (this.reportedErrors.add("passes:" + what + ':' + e.getClass().getName())) {
         FuriahsVisuals.LOG.error("The shared {} failed (further identical errors are not logged)", new Object[]{what, e});
      }
   }

   public void renderHand(float partialTicks, int renderPass) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onRenderHand(partialTicks, renderPass);
            } catch (RuntimeException | LinkageError var6) {
               this.report(m, "hand", var6);
            }
         }
      }
   }

   public void afterHand(float partialTicks) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onAfterHand(partialTicks);
            } catch (RuntimeException | LinkageError var5) {
               this.report(m, "after hand", var5);
            }
         }
      }
   }

   public void renderOverlay(float partialTicks) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onRenderOverlay(partialTicks);
            } catch (RuntimeException | LinkageError var5) {
               this.report(m, "overlay", var5);
            }
         }
      }
   }

   public boolean overlayElement(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               if (m.onOverlayElement(type, partialTicks)) {
                  return true;
               }
            } catch (RuntimeException | LinkageError var6) {
               this.report(m, "overlay " + type, var6);
            }
         }
      }

      return false;
   }

   public void overlayElementPost(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onOverlayElementPost(type, partialTicks);
            } catch (RuntimeException | LinkageError var6) {
               this.report(m, "overlay post " + type, var6);
            }
         }
      }
   }

   public void fogColors(EntityViewRenderEvent.FogColors event) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onFogColors(event);
            } catch (RuntimeException | LinkageError var5) {
               this.report(m, "fog", var5);
            }
         }
      }
   }

   public void renderFog(EntityViewRenderEvent.RenderFogEvent event) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onRenderFog(event);
            } catch (RuntimeException | LinkageError var5) {
               this.report(m, "fog distance", var5);
            }
         }
      }
   }

   public void attack(EntityPlayer attacker, Entity target) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onAttack(attacker, target);
            } catch (RuntimeException | LinkageError var6) {
               this.report(m, "attack", var6);
            }
         }
      }
   }

   public void chat(String text) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onChat(text);
            } catch (RuntimeException | LinkageError var5) {
               this.report(m, "chat", var5);
            }
         }
      }
   }

   public boolean hidesPlayer(EntityPlayer player) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               if (m.hidesPlayer(player)) {
                  return true;
               }
            } catch (RuntimeException | LinkageError var5) {
               this.report(m, "hide player", var5);
            }
         }
      }

      return false;
   }

   public void clientTick() {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onClientTick();
            } catch (RuntimeException | LinkageError var4) {
               this.report(m, "client tick", var4);
            }
         }
      }
   }

   public void renderHud(float partialTicks) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onRenderHud(partialTicks);
            } catch (RuntimeException | LinkageError var5) {
               this.report(m, "hud", var5);
            }
         }
      }
   }

   public void screenDrawn(GuiScreen screen) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               m.onScreenDrawn(screen);
            } catch (RuntimeException | LinkageError var5) {
               this.report(m, "screen", var5);
            }
         }
      }
   }

   public boolean screenMouse(GuiScreen screen) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               if (m.onScreenMouse(screen)) {
                  return true;
               }
            } catch (RuntimeException | LinkageError var5) {
               this.report(m, "mouse", var5);
            }
         }
      }

      return false;
   }

   public boolean screenKey(GuiScreen screen) {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);
         if (m.isEnabled()) {
            try {
               if (m.onScreenKey(screen)) {
                  return true;
               }
            } catch (RuntimeException | LinkageError var5) {
               this.report(m, "key", var5);
            }
         }
      }

      return false;
   }

   public void worldChanged() {
      for (int i = 0; i < this.modules.size(); i++) {
         Module m = this.modules.get(i);

         try {
            m.onWorldChange();
         } catch (RuntimeException | LinkageError var4) {
            this.report(m, "world change", var4);
         }
      }
   }

   private void report(Module m, String hook, Throwable e) {
      if (this.reportedErrors.add(m.getId() + ':' + hook + ':' + e.getClass().getName())) {
         FuriahsVisuals.LOG.error("Module '{}' failed during {} (further identical errors are not logged)", new Object[]{m.getName(), hook, e});
      }
   }
}
