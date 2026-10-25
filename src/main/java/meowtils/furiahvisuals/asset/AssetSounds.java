package meowtils.furiahvisuals.asset;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.mixin.AccessorSoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISoundEventAccessor;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundEventAccessorComposite;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.client.audio.SoundRegistry;
import net.minecraft.util.ResourceLocation;

public final class AssetSounds {
   private static final String DIR = "sounds/weapons/";
   private static final String[] NAMES = new String[]{
      "chainsaw_idle", "chainsaw_full", "chainsaw_start", "chainsaw_pull", "chainsaw_stop", "chainsaw_bite", "chainsaw_others"
   };
   private static boolean warned;

   private AssetSounds() {
   }

   public static ResourceLocation event(String name) {
      return new ResourceLocation("furiahsvisuals", "weapons." + name);
   }

   public static URL url(ResourceLocation location) throws MalformedURLException {
      if (location != null && "furiahsvisuals".equals(location.getResourceDomain())) {
         final String path = location.getResourcePath();
         return path.startsWith("sounds/weapons/") && AssetSource.safe(path) ? new URL(null, "furiahsvisuals-asset:" + path, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) {
               return new URLConnection(u) {
                  @Override
                  public void connect() {
                  }

                  @Override
                  public InputStream getInputStream() throws IOException {
                     InputStream in = AssetSource.open(path);
                     if (in == null) {
                        throw new FileNotFoundException(path);
                     } else {
                        return in;
                     }
                  }
               };
            }
         }) : null;
      } else {
         return null;
      }
   }

   public static boolean ensure() {
      if (!AssetSource.ready()) {
         return false;
      } else {
         try {
            SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
            if (handler == null) {
               return false;
            } else if (handler.getSound(event(NAMES[0])) != null) {
               return true;
            } else {
               SoundRegistry registry = AccessorSoundHandler.getFvSoundRegistry(handler);

               for (String n : NAMES) {
                  SoundEventAccessorComposite c = new SoundEventAccessorComposite(event(n), 1.0, 1.0, SoundCategory.PLAYERS);
                  c.addSoundToEventPool(
                     new AssetSounds.Entry(new SoundPoolEntry(new ResourceLocation("furiahsvisuals", "sounds/weapons/" + n + ".ogg"), 1.0, 1.0, false))
                  );
                  registry.registerSound(c);
               }

               return true;
            }
         } catch (RuntimeException | LinkageError var7) {
            if (!warned) {
               warned = true;
               FuriahsVisuals.LOG.warn("The weapons' sounds could not be added to the game's sounds; they stay silent", var7);
            }

            return false;
         }
      }
   }

   public static String checkForTest() {
      StringBuilder sb = new StringBuilder("events added " + ensure());

      try {
         Method m = meowtils.furiahvisuals.hook.Hooks.method(SoundManager.class, new String[]{"getURLForSoundResource"}, ResourceLocation.class);

         if (m == null) {
            return sb.append("; SoundManager.getURLForSoundResource not found").toString();
         }

         m.setAccessible(true);
         SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();

         for (String n : NAMES) {
            URL url = (URL)m.invoke(null, new ResourceLocation("furiahsvisuals", "sounds/weapons/" + n + ".ogg"));
            InputStream in = url.openStream();
            byte[] head = new byte[4];
            long total = 0L;

            try {
               byte[] buf = new byte[8192];

               int r;
               while ((r = in.read(buf)) > 0) {
                  if (total < 4L) {
                     System.arraycopy(buf, 0, head, (int)total, (int)Math.min(4L - total, (long)r));
                  }

                  total += r;
               }
            } finally {
               in.close();
            }

            sb.append("; ")
               .append(n)
               .append(": event ")
               .append(handler.getSound(event(n)) != null ? "yes" : "NO")
               .append(", ")
               .append(url.getProtocol())
               .append(", ")
               .append(total)
               .append(" bytes")
               .append(new String(head, StandardCharsets.ISO_8859_1).equals("OggS") ? " (Ogg)" : " (NOT Ogg)");
         }
      } catch (Exception var20) {
         sb.append("; failed: ").append(var20);
      }

      return sb.toString();
   }

   private static final class Entry implements ISoundEventAccessor<SoundPoolEntry> {
      private final SoundPoolEntry entry;

      Entry(SoundPoolEntry entry) {
         this.entry = entry;
      }

      public int getWeight() {
         return 1;
      }

      public SoundPoolEntry cloneEntry() {
         return new SoundPoolEntry(this.entry);
      }
   }
}
