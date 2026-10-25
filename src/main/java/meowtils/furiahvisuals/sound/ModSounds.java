package meowtils.furiahvisuals.sound;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;

public final class ModSounds {
   private static final long SAME_SOUND_GAP_MS = 40L;
   private static final long SLIDER_TICK_GAP_MS = 50L;
   private static final int MAX_PLAYING = 4;
   private static final int MAX_VARIANTS = 12;
   private static SoundSettings settings;
   private static final long[] lastPlayed = new long[Sfx.values().length];
   private static final ArrayDeque<ModSound> playing = new ArrayDeque<>();
   private static final Random RANDOM = new Random();
   private static final Map<String, Integer> lastVariant = new HashMap<>();
   private static List<String> recorded;
   private static SoundTheme themeOverride;
   private static int requests;
   private static int eventKey = 0;
   private static volatile int demo;

   private ModSounds() {
   }

   public static void init(SoundSettings s) {
      settings = s;
   }

   public static SoundSettings settings() {
      return settings;
   }

   public static void gui(Sfx sfx) {
      gui(sfx, 1.0F);
   }

   public static void gui(Sfx sfx, float pitch) {
      gui(sfx, null, pitch);
   }

   private static void gui(Sfx sfx, String file, float pitch) {
      requests++;
      if (settings != null && settings.gui.get()) {
         play(sfx, file, pitch, settings.guiVolume.getFloat() / 100.0F, false, false);
      }
   }

   public static int requestCount() {
      return requests;
   }

   public static void toggle(boolean on) {
      gui(on ? Sfx.TOGGLE_ON : Sfx.TOGGLE_OFF);
   }

   public static void sliderTick(double fraction) {
      requests++;
      if (settings != null && settings.sliderTicks.get()) {
         gui(Sfx.SLIDER_TICK, 0.8F + 0.5F * (float)Math.max(0.0, Math.min(1.0, fraction)));
      }
   }

   public static void hover() {
      if (settings != null && settings.hover.get()) {
         gui(Sfx.HOVER);
      }
   }

   public static void keyEvent(int keyCode) {
      eventKey = keyCode;
   }

   public static void key() {
      if (settings != null && settings.typing.get()) {
         gui(Sfx.KEY, specialKey(eventKey), 1.0F);
      }
   }

   public static void keyUp(int keyCode) {
      if (settings != null && settings.typing.get()) {
         gui(Sfx.KEY_UP, null, 1.0F);
      }
   }

   private static String specialKey(int keyCode) {
      switch (keyCode) {
         case 14:
            return "key_backspace";
         case 28:
         case 156:
            return "key_enter";
         case 57:
            return "key_space";
         default:
            return null;
      }
   }

   public static void noResults() {
      if (settings != null && settings.noResults.get()) {
         gui(Sfx.EMPTY);
      }
   }

   public static void island(Sfx sfx, float pitch, float channelVolume) {
      requests++;
      play(sfx, null, pitch, channelVolume, true, false);
   }

   public static void preview(Sfx sfx, float channelVolume) {
      preview(sfx, 1.0F, channelVolume);
   }

   public static void preview(Sfx sfx, float pitch, float channelVolume) {
      requests++;
      play(sfx, null, pitch, channelVolume, true, true);
   }

   public static void previewTheme() {
      requests++;
      if (settings != null) {
         SoundTheme theme = theme();
         if (exists(theme, "key_1")) {
            typingDemo(theme);
         } else {
            play(Sfx.SUCCESS, null, 1.0F, settings.guiVolume.getFloat() / 100.0F, true, true);
         }
      }
   }

   private static void typingDemo(SoundTheme theme) {
      final int id = ++demo;
      final float volume = settings.guiVolume.getFloat() / 100.0F;
      final ArrayList<long[]> events = new ArrayList<>();
      String text = "hello world\n";
      Random r = new Random();
      long t = 0L;

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         int code = c == ' ' ? 57 : (c == '\n' ? 28 : 30);
         events.add(new long[]{t += i == 0 ? 0L : (c != ' ' && c != '\n' ? 95L : 170L) + r.nextInt(60), code, 1L});
         events.add(new long[]{t + 60L + r.nextInt(30), code, 0L});
      }

      Collections.sort(events, new Comparator<long[]>() {
         public int compare(long[] a, long[] b) {
            return Long.compare(a[0], b[0]);
         }
      });
      Thread thread = new Thread(new Runnable() {
         @Override
         public void run() {
            long start = System.currentTimeMillis();

            for (long[] e : events) {
               long wait = start + e[0] - System.currentTimeMillis();
               if (wait > 0L) {
                  try {
                     Thread.sleep(wait);
                  } catch (InterruptedException var8) {
                     return;
                  }
               }

               if (ModSounds.demo != id) {
                  return;
               }

               if (e[2] == 1L) {
                  ModSounds.play(Sfx.KEY, ModSounds.specialKey((int)e[1]), 1.0F, volume, true, true);
               } else {
                  ModSounds.play(Sfx.KEY_UP, null, 1.0F, volume, true, true);
               }
            }
         }
      }, "Furiah's Visuals typing demo");
      thread.setDaemon(true);
      thread.start();
   }

   public static void previewGui(Sfx sfx) {
      if (settings != null) {
         preview(sfx, settings.guiVolume.getFloat() / 100.0F);
      }
   }

   private static SoundTheme theme() {
      return themeOverride != null ? themeOverride : settings.theme.get();
   }

   private static boolean exists(SoundTheme theme, String file) {
      try {
         SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
         return handler != null && handler.getSound(new ResourceLocation("furiahsvisuals", theme.dir() + "." + file)) != null;
      } catch (RuntimeException var3) {
         return false;
      }
   }

   private static String fileFor(SoundTheme theme, Sfx sfx, String special) {
      String base = special != null && exists(theme, special) ? special : sfx.file();
      int count = 0;

      while (count < 12 && exists(theme, base + "_" + (count + 1))) {
         count++;
      }

      if (count > 0) {
         String key = theme.dir() + "." + base;
         Map<String, Integer> map = lastVariant;
         int pick;
         synchronized (map) {
            Integer last = lastVariant.get(key);
            pick = 1 + RANDOM.nextInt(count);
            if (count > 1 && last != null && pick == last) {
               pick = pick % count + 1;
            }

            lastVariant.put(key, pick);
         }

         return base + "_" + pick;
      } else {
         return sfx.isOptional() && !exists(theme, base) ? null : base;
      }
   }

   private static void play(Sfx sfx, String special, float pitch, float channelVolume, final boolean priority, boolean preview) {
      if (settings != null && !settings.mute.get()) {
         final float volume = Math.min(1.0F, channelVolume * settings.volume.getFloat() / 100.0F);
         if (!(volume <= 0.001F)) {
            long now = System.currentTimeMillis();
            long gap = sfx == Sfx.SLIDER_TICK ? 50L : 40L;
            if (preview || now - lastPlayed[sfx.ordinal()] >= gap) {
               SoundTheme theme = theme();
               String file = fileFor(theme, sfx, special);
               if (file != null) {
                  lastPlayed[sfx.ordinal()] = now;
                  final float p = Math.max(0.5F, Math.min(2.0F, pitch * (0.97F + RANDOM.nextFloat() * 0.06F)));
                  final ResourceLocation location = new ResourceLocation("furiahsvisuals", theme.dir() + "." + file);
                  final Minecraft mc = Minecraft.getMinecraft();
                  if (recorded != null) {
                     float master = mc.gameSettings.getSoundLevel(SoundCategory.MASTER);
                     recorded.add(String.format(Locale.ROOT, "%s.%s x%.2f vol %.2f", theme.dir(), file, p, volume * master));
                  }

                  final boolean typing = sfx == Sfx.KEY || sfx == Sfx.KEY_UP;
                  final boolean menu = !priority || preview;
                  if (mc.isCallingFromMinecraftThread()) {
                     start(mc, location, volume, p, priority, typing, menu);
                  } else {
                     mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                           ModSounds.start(mc, location, volume, p, priority, typing, menu);
                        }
                     });
                  }
               }
            }
         }
      }
   }

   private static void start(Minecraft mc, ResourceLocation location, float volume, float pitch, boolean priority, boolean typing, boolean menu) {
      SoundHandler handler = mc.getSoundHandler();
      if (handler != null) {
         Iterator<ModSound> it = playing.iterator();

         while (it.hasNext()) {
            if (!handler.isSoundPlaying((ISound)it.next())) {
               it.remove();
            }
         }

         if (playing.size() >= 4) {
            ModSound victim = null;
            if (priority) {
               victim = playing.peekFirst();
            } else if (typing) {
               for (ModSound s : playing) {
                  if (s.menu) {
                     victim = s;
                     break;
                  }
               }
            }

            if (victim == null) {
               return;
            }

            playing.remove(victim);
            handler.stopSound(victim);
         }

         ModSound sound = new ModSound(location, volume, pitch, menu);

         try {
            handler.playSound(sound);
            playing.addLast(sound);
         } catch (RuntimeException var12) {
            FuriahsVisuals.LOG.debug("Could not play {}", new Object[]{location, var12});
         }
      }
   }

   public static void recordForTest(boolean on) {
      recorded = on ? Collections.synchronizedList(new ArrayList<>()) : null;
   }

   public static void noteForTest(String name, float pitch, float volume) {
      List<String> r = recorded;
      if (r != null) {
         float master = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.MASTER);
         float players = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.PLAYERS);
         r.add(String.format(Locale.ROOT, "%s x%.2f vol %.2f", name, pitch, volume * master * players));
      }
   }

   public static List<String> playedForTest() {
      List<String> r = recorded;
      if (r == null) {
         return new ArrayList<>();
      } else {
         synchronized (r) {
            ArrayList<String> out = new ArrayList<>(r);
            r.clear();
            return out;
         }
      }
   }

   public static int playedCountForTest() {
      return recorded == null ? 0 : recorded.size();
   }

   public static void resetSpamGuardForTest() {
      Arrays.fill(lastPlayed, 0L);
   }

   public static void themeForTest(SoundTheme theme) {
      themeOverride = theme;
   }
}
