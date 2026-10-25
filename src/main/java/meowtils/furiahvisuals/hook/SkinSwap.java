package meowtils.furiahvisuals.hook;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import meowtils.furiahvisuals.module.misc.skins.SkinHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;

public final class SkinSwap {
   static final String[] HOOKS = new String[]{"skinInfo", "modelInfo", "skinPlayer", "modelPlayer"};
   private static final Field INFO_MAP = Hooks.field(NetHandlerPlayClient.class, "playerInfoMap", "playerInfoMap");
   private static final Field PLAYER_INFO = Hooks.field(AbstractClientPlayer.class, "playerInfo", "playerInfo");
   private static boolean on;

   private SkinSwap() {
   }

   static void install() {
      if (INFO_MAP != null && PLAYER_INFO != null) {
         on = true;
         Hooks.on(HOOKS);
         sync();
      }
   }

   static void uninstall() {
      on = false;
      Hooks.off(HOOKS);
      Map<UUID, NetworkPlayerInfo> map = map();
      if (map != null) {
         for (Entry<UUID, NetworkPlayerInfo> e : map.entrySet()) {
            NetworkPlayerInfo i = e.getValue();
            if (Swap.ours(i, SkinSwap.FvNetworkPlayerInfo.class)) {
               NetworkPlayerInfo plain = Swap.downgrade(i, NetworkPlayerInfo.class);
               e.setValue(plain);
               repointPlayer(i, plain);
            }
         }
      }
   }

   private static Map<UUID, NetworkPlayerInfo> map() {
      NetHandlerPlayClient nh = Minecraft.getMinecraft().getNetHandler();
      Object o = nh != null && INFO_MAP != null ? Swap.get(INFO_MAP, nh) : null;
      return o instanceof Map ? (Map)o : null;
   }

   static void sync() {
      if (on) {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.thePlayer != null) {
            Map<UUID, NetworkPlayerInfo> map = map();
            if (map != null) {
               UUID me = mc.thePlayer.getUniqueID();
               NetworkPlayerInfo i = map.get(me);
               if (i != null && i.getClass() == NetworkPlayerInfo.class) {
                  SkinSwap.FvNetworkPlayerInfo f = Swap.upgrade(i, NetworkPlayerInfo.class, SkinSwap.FvNetworkPlayerInfo.class);
                  if (f != null && f != i) {
                     map.put(me, f);
                     repointPlayer(i, f);
                  }
               }
            }
         }
      }
   }

   private static void repointPlayer(NetworkPlayerInfo from, NetworkPlayerInfo to) {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.theWorld != null) {
         for (Object o : mc.theWorld.playerEntities) {
            if (o instanceof AbstractClientPlayer && Swap.get(PLAYER_INFO, o) == from) {
               Swap.set(PLAYER_INFO, o, to);
            }
         }
      }
   }

   public static class FvNetworkPlayerInfo extends NetworkPlayerInfo {
      public FvNetworkPlayerInfo(GameProfile profile) {
         super(profile);
      }

      public ResourceLocation getLocationSkin() {
         ResourceLocation r = Bridge.active() ? SkinHooks.skinFor(this.getGameProfile()) : null;
         return r != null ? r : super.getLocationSkin();
      }

      public String getSkinType() {
         String t = Bridge.active() ? SkinHooks.typeFor(this.getGameProfile()) : null;
         return t != null ? t : super.getSkinType();
      }
   }
}
