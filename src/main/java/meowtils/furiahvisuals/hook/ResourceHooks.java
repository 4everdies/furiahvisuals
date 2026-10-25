package meowtils.furiahvisuals.hook;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import meowtils.furiahvisuals.asset.AssetSource;
import meowtils.furiahvisuals.asset.ExtensionResources;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundList;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

public final class ResourceHooks {
   static final String DOMAIN = "furiahsvisuals";
   private static final Field DEFAULT_PACKS = Hooks.field(Minecraft.class, "defaultResourcePacks", "defaultResourcePacks");
   private static final Field DOMAIN_MANAGERS = Hooks.field(SimpleReloadableResourceManager.class, "domainResourceManagers", "domainResourceManagers");
   private static final Field DOMAINS = Hooks.field(SimpleReloadableResourceManager.class, "setResourceDomains", "setResourceDomains");
   private static final Method SOUND_MAP = Hooks.method(SoundHandler.class, new String[]{"getSoundMap", "getSoundMap"}, InputStream.class);
   private static final Method LOAD_SOUND = Hooks.method(
      SoundHandler.class, new String[]{"loadSoundResource", "loadSoundResource"}, ResourceLocation.class, SoundList.class
   );

   private ResourceHooks() {
   }

   static void install() {
      Minecraft mc = Minecraft.getMinecraft();
      uninstall();
      ResourceHooks.Pack pack = new ResourceHooks.Pack();
      Object packs = DEFAULT_PACKS == null ? null : Swap.get(DEFAULT_PACKS, mc);
      if (packs instanceof List) {
         ((List)packs).add(pack);
      }

      IResourceManager rm = mc.getResourceManager();
      if (rm instanceof SimpleReloadableResourceManager) {
         ((SimpleReloadableResourceManager)rm).reloadResourcePack(pack);
      }

      loadSounds(mc);

      try {
         mc.getLanguageManager().onResourceManagerReload(rm);
      } catch (Throwable var5) {
         FuriahsVisuals.LOG.warn("Resources: the language file could not be loaded", var5);
      }
   }

   static void uninstall() {
      Minecraft mc = Minecraft.getMinecraft();
      Object packs = DEFAULT_PACKS == null ? null : Swap.get(DEFAULT_PACKS, mc);
      if (packs instanceof List) {
         List<IResourcePack> list = (List<IResourcePack>)packs;

         for (int i = list.size() - 1; i >= 0; i--) {
            if (ResourceHooks.Pack.class.getName().equals(list.get(i).getClass().getName())) {
               list.remove(i);
            }
         }
      }

      IResourceManager rm = mc.getResourceManager();
      if (rm instanceof SimpleReloadableResourceManager && DOMAIN_MANAGERS != null) {
         Object managers = Swap.get(DOMAIN_MANAGERS, rm);
         if (managers instanceof Map) {
            ((Map)managers).remove("furiahsvisuals");
         }

         Object domains = DOMAINS == null ? null : Swap.get(DOMAINS, rm);
         if (domains instanceof Set) {
            ((Set)domains).remove("furiahsvisuals");
         }
      }
   }

   private static void loadSounds(Minecraft mc) {
      SoundHandler handler = mc.getSoundHandler();
      if (handler != null && SOUND_MAP != null && LOAD_SOUND != null) {
         InputStream in = ExtensionResources.open("/assets/furiahsvisuals/sounds.json");
         if (in != null) {
            try {
               Map<String, SoundList> map = (Map<String, SoundList>)SOUND_MAP.invoke(handler, in);

               for (Entry<String, SoundList> e : map.entrySet()) {
                  LOAD_SOUND.invoke(handler, new ResourceLocation("furiahsvisuals", e.getKey()), e.getValue());
               }
            } catch (Throwable var14) {
               FuriahsVisuals.LOG.warn("Resources: sounds.json could not be loaded", var14);
            } finally {
               try {
                  in.close();
               } catch (IOException var13) {
               }
            }
         }
      } else {
         FuriahsVisuals.LOG.warn("Resources: the game's sound loader was not found; the menu sounds stay silent until the next resource reload");
      }
   }

   static final class Pack implements IResourcePack {
      public InputStream getInputStream(ResourceLocation location) throws IOException {
         InputStream in = this.open(location);
         if (in == null) {
            throw new FileNotFoundException(location.toString());
         } else {
            return in;
         }
      }

      public boolean resourceExists(ResourceLocation location) {
         InputStream in = this.open(location);
         if (in == null) {
            return false;
         } else {
            try {
               in.close();
            } catch (IOException var4) {
            }

            return true;
         }
      }

      private InputStream open(ResourceLocation location) {
         return "furiahsvisuals".equals(location.getResourceDomain()) && AssetSource.safe(location.getResourcePath())
            ? ExtensionResources.open("/assets/furiahsvisuals/" + location.getResourcePath())
            : null;
      }

      public Set<String> getResourceDomains() {
         return Collections.singleton("furiahsvisuals");
      }

      public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer serializer, String section) throws IOException {
         return null;
      }

      public BufferedImage getPackImage() throws IOException {
         throw new FileNotFoundException("pack.png");
      }

      public String getPackName() {
         return "Furiah's Visuals";
      }
   }
}
