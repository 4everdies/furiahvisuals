package meowtils.furiahvisuals.module.misc.skins;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

public final class SkinHooks {
   private static ResourceLocation worn;
   private static String wornType = "default";
   private static int bypass;
   private static UUID alsoWornBy;
   private static ResourceLocation fetched;
   private static boolean fetchedSlim;

   private SkinHooks() {
   }

   public static void alsoForTest(UUID id) {
      alsoWornBy = id;
   }

   private static boolean wears(UUID id) {
      return isMe(id) || id != null && id.equals(alsoWornBy);
   }

   static void wear(ResourceLocation skin, boolean slim) {
      worn = skin;
      wornType = slim ? "slim" : "default";
   }

   public static ResourceLocation worn() {
      return worn;
   }

   public static boolean wornSlim() {
      return worn != null && "slim".equals(wornType);
   }

   public static ResourceLocation skinFor(GameProfile profile) {
      ResourceLocation w = worn;
      return w != null && bypass == 0 && profile != null && wears(profile.getId()) ? w : null;
   }

   public static String typeFor(GameProfile profile) {
      return worn != null && bypass == 0 && profile != null && wears(profile.getId()) ? wornType : null;
   }

   public static ResourceLocation skinFor(UUID id) {
      ResourceLocation w = worn;
      return w != null && bypass == 0 && wears(id) ? w : null;
   }

   public static String typeFor(UUID id) {
      return worn != null && bypass == 0 && wears(id) ? wornType : null;
   }

   static boolean isMe(UUID id) {
      if (id == null) {
         return false;
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.thePlayer != null) {
            return id.equals(mc.thePlayer.getUniqueID());
         } else {
            UUID own = sessionId();
            return own != null && id.equals(own);
         }
      }
   }

   static UUID sessionId() {
      try {
         GameProfile p = Minecraft.getMinecraft().getSession().getProfile();
         return p == null ? null : p.getId();
      } catch (RuntimeException var1) {
         return null;
      }
   }

   public static ResourceLocation ownSkin() {
      if (worn != null) {
         return worn;
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         ResourceLocation own;
         if (mc.thePlayer instanceof AbstractClientPlayer
            && (own = mc.thePlayer.getLocationSkin()) != null
            && mc.getTextureManager().getTexture(own) != null) {
            return own;
         } else if (fetched != null) {
            return fetched;
         } else {
            UUID id = mc.thePlayer != null ? mc.thePlayer.getUniqueID() : sessionId();
            return id != null ? DefaultPlayerSkin.getDefaultSkin(id) : DefaultPlayerSkin.getDefaultSkinLegacy();
         }
      }
   }

   public static boolean ownSlim() {
      if (worn != null) {
         return "slim".equals(wornType);
      } else {
         Minecraft mc = Minecraft.getMinecraft();
         ResourceLocation own;
         if (mc.thePlayer instanceof AbstractClientPlayer
            && (own = mc.thePlayer.getLocationSkin()) != null
            && mc.getTextureManager().getTexture(own) != null) {
            return "slim".equals(mc.thePlayer.getSkinType());
         } else if (fetched != null) {
            return fetchedSlim;
         } else {
            UUID id = mc.thePlayer != null ? mc.thePlayer.getUniqueID() : sessionId();
            return id != null && "slim".equals(DefaultPlayerSkin.getSkinType(id));
         }
      }
   }

   static void fetchedAccount(ResourceLocation skin, boolean slim) {
      fetched = skin;
      fetchedSlim = slim;
   }

   static ResourceLocation accountSkin(boolean[] slimOut) {
      if (fetched != null) {
         slimOut[0] = fetchedSlim;
         return fetched;
      } else {
         bypass++;

         try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer instanceof AbstractClientPlayer) {
               ResourceLocation own = mc.thePlayer.getLocationSkin();
               ITextureObject t = own == null ? null : mc.getTextureManager().getTexture(own);
               if (t != null) {
                  slimOut[0] = "slim".equals(mc.thePlayer.getSkinType());
                  return own;
               }
            }

            UUID id = mc.thePlayer != null ? mc.thePlayer.getUniqueID() : sessionId();
            slimOut[0] = id != null && "slim".equals(DefaultPlayerSkin.getSkinType(id));
            return id != null ? DefaultPlayerSkin.getDefaultSkin(id) : DefaultPlayerSkin.getDefaultSkinLegacy();
         } finally {
            bypass--;
         }
      }
   }
}
