package meowtils.furiahvisuals.module.particles;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import meowtils.furiahvisuals.hook.Hooks;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityParticleEmitter;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.WorldRenderer;

public final class HookStatus {
   public static final String SPAWN_HEAD = "spawnHead";
   public static final String SPAWN_TAIL = "spawnTail";
   public static final String EMIT_HEAD = "emitHead";
   public static final String EMITTER_SPAWN = "emitterSpawn";
   public static final String DRAW_PARTICLE = "drawParticle";
   public static final String DRAW_START = "drawStart";
   public static final String DRAW_END = "drawEnd";
   public static final String BYPASS_SETTING = "bypassSetting";
   public static final String CRIT_PREDICTION = "critPrediction";
   public static final String SHARP_PREDICTION = "sharpPrediction";
   public static final String CRIT_PACKET = "critPacket";
   private static final Set<String> SEEN = Collections.synchronizedSet(new HashSet<>());
   private static Set<String> installed;
   private static boolean pluginLoaded;
   private static boolean tagged;
   private static boolean looked;

   private HookStatus() {
   }

   private static void lookUp() {
      if (!looked) {
         looked = true;
         Class[] targets = new Class[]{
            EffectRenderer.class, EntityParticleEmitter.class, RenderGlobal.class, EntityPlayerSP.class, NetHandlerPlayClient.class, WorldRenderer.class
         };
         tagged = targets.length > 0;
         pluginLoaded = true;
         installed = Hooks.INSTALLED;
      }
   }

   public static boolean mixinActive() {
      lookUp();
      return tagged;
   }

   public static boolean has(String hook) {
      lookUp();
      if (!tagged) {
         return false;
      } else {
         return SEEN.contains(hook) ? true : installed == null || !pluginLoaded || installed.contains(hook);
      }
   }

   public static void seen(String hook) {
      if (!SEEN.contains(hook)) {
         SEEN.add(hook);
      }
   }

   public static String describe() {
      lookUp();
      StringBuilder sb = new StringBuilder();
      sb.append(tagged ? "mixins on" : "MIXINS OFF").append(pluginLoaded ? ", plugin" : ", no plugin").append(": ");

      for (String h : new String[]{
         "spawnHead",
         "spawnTail",
         "emitHead",
         "emitterSpawn",
         "drawParticle",
         "drawStart",
         "drawEnd",
         "bypassSetting",
         "critPrediction",
         "sharpPrediction",
         "critPacket"
      }) {
         sb.append(h).append(has(h) ? "+" : "-").append(' ');
      }

      return sb.toString().trim();
   }
}
