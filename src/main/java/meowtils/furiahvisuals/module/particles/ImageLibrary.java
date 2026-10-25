package meowtils.furiahvisuals.module.particles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.image.ImageLoader;

final class ImageLibrary {
   private final File dir;
   private final Map<String, ImageLibrary.Entry> entries = new LinkedHashMap<>();
   private final ConcurrentLinkedQueue<Runnable> clientTasks = new ConcurrentLinkedQueue<>();
   private ExecutorService worker;
   private ImageProcessor.Square square = ImageProcessor.Square.STRETCH;
   private int maxSize = 512;
   private ParticlesModule.Filtering filtering = ParticlesModule.Filtering.AUTO;
   private int generation;
   private int version;
   private boolean scanned;
   private String message;
   private boolean messageError;
   private long messageAt;

   ImageLibrary(File dir) {
      this.dir = dir;
   }

   File getDir() {
      return this.dir;
   }

   int version() {
      return this.version;
   }

   List<ImageLibrary.Entry> entries() {
      return Collections.unmodifiableList(new ArrayList<>(this.entries.values()));
   }

   ImageLibrary.Entry find(String name) {
      return name == null ? null : this.entries.get(name);
   }

   boolean hasScanned() {
      return this.scanned;
   }

   void say(String text, boolean error) {
      this.message = text;
      this.messageError = error;
      this.messageAt = ClientClock.millis();
   }

   String message() {
      return this.message != null && ClientClock.millis() - this.messageAt < (this.messageError ? 12000L : 6000L) ? this.message : null;
   }

   boolean messageIsError() {
      return this.messageError;
   }

   void post(Runnable task) {
      this.clientTasks.add(task);
   }

   void refresh() {
      this.scanned = true;
      if (!this.dir.isDirectory() && !this.dir.mkdirs()) {
         this.say("Could not create " + this.dir.getPath(), true);
      } else {
         Object[] files = this.dir.listFiles();
         if (files != null) {
            Arrays.sort(files);
            HashSet<String> seen = new HashSet<>();
            LinkedHashMap<String, ImageLibrary.Entry> sorted = new LinkedHashMap<>();
            boolean changed = false;

            for (Object f : files) {
               String name = ((File)f).getName();
               if (((File)f).isFile() && ImageLoader.isImageName(name)) {
                  seen.add(name);
                  ImageLibrary.Entry e = this.entries.get(name);
                  if (e == null) {
                     e = new ImageLibrary.Entry(name, (File)f);
                     changed = true;
                  }

                  sorted.put(name, e);
                  if (e.length != ((File)f).length() || e.modified != ((File)f).lastModified()) {
                     this.queue(e);
                     changed = true;
                  }
               }
            }

            for (ImageLibrary.Entry ex : this.entries.values()) {
               if (!seen.contains(ex.name)) {
                  ImageOps.delete(ex.texture);
                  ex.texture = -1;
                  changed = true;
               }
            }

            this.entries.clear();
            this.entries.putAll(sorted);
            if (changed) {
               this.version++;
            }
         }
      }
   }

   private void queue(final ImageLibrary.Entry e) {
      e.length = e.file.length();
      e.modified = e.file.lastModified();
      e.state = ImageLibrary.State.WAITING;
      e.error = null;
      final int gen = this.generation;
      final ImageProcessor.Square sq = this.square;
      final int max = this.maxSize;
      final File file = e.file;
      this.worker().submit(new Runnable() {
         @Override
         public void run() {
            try {
               final ImageProcessor.Result r = ImageProcessor.process(file, sq, max);
               ImageLibrary.this.post(new Runnable() {
                  @Override
                  public void run() {
                     ImageLibrary.this.ready(e, r, gen);
                  }
               });
            } catch (final ImageLoader.LoadException var2) {
               ImageLibrary.this.post(new Runnable() {
                  @Override
                  public void run() {
                     ImageLibrary.this.failed(e, var2.getMessage(), gen);
                  }
               });
            } catch (Throwable var3) {
               FuriahsVisuals.LOG.warn("Particles: could not process {}", new Object[]{file, var3});
               ImageLibrary.this.post(new Runnable() {
                  @Override
                  public void run() {
                     ImageLibrary.this.failed(e, "Could not read this image", gen);
                  }
               });
            }
         }
      });
   }

   private ExecutorService worker() {
      if (this.worker == null) {
         this.worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
               Thread t = new Thread(r, "Furiah's Visuals particle images");
               t.setDaemon(true);
               t.setPriority(1);
               return t;
            }
         });
      }

      return this.worker;
   }

   private void ready(ImageLibrary.Entry e, ImageProcessor.Result r, int gen) {
      if (this.entries.get(e.name) == e && gen == this.generation) {
         ImageOps.delete(e.texture);
         e.pixel = this.pixelFor(r.size);
         e.texture = ImageOps.upload(r.levels, r.levelSizes, r.levelSizes, !e.pixel);
         e.average = r.average;
         e.originalWidth = r.originalWidth;
         e.originalHeight = r.originalHeight;
         e.size = r.size;
         e.how = r.how;
         e.state = ImageLibrary.State.READY;
         e.error = null;
         this.version++;
      }
   }

   private void failed(ImageLibrary.Entry e, String error, int gen) {
      if (this.entries.get(e.name) == e && gen == this.generation) {
         ImageOps.delete(e.texture);
         e.texture = -1;
         e.state = ImageLibrary.State.ERROR;
         e.error = error;
         this.version++;
      }
   }

   void update() {
      Runnable task;
      for (int uploads = 0; uploads < 3 && (task = this.clientTasks.poll()) != null; uploads++) {
         try {
            task.run();
         } catch (RuntimeException | LinkageError var4) {
            FuriahsVisuals.LOG.warn("Particles: image task failed", var4);
         }
      }
   }

   void setProcessing(ImageProcessor.Square square, int maxSize) {
      if (square != this.square || maxSize != this.maxSize) {
         this.square = square;
         this.maxSize = maxSize;
         this.generation++;
         if (this.scanned) {
            for (ImageLibrary.Entry e : this.entries.values()) {
               this.queue(e);
            }

            this.version++;
         }
      }
   }

   void setFiltering(ParticlesModule.Filtering filtering) {
      if (filtering != this.filtering) {
         this.filtering = filtering;

         for (ImageLibrary.Entry e : this.entries.values()) {
            if (e.state == ImageLibrary.State.READY && e.texture > 0) {
               e.pixel = this.pixelFor(e.size);
               ImageOps.setFilter(e.texture, !e.pixel);
            }
         }

         this.version++;
      }
   }

   private boolean pixelFor(int size) {
      switch (this.filtering) {
         case PIXEL:
            return true;
         case SMOOTH:
            return false;
         default:
            return size <= 64;
      }
   }

   String importFile(File source) {
      final String picked = source.getName();

      try {
         ImageLoader.Image image = ImageLoader.load(source, 4096, true);
         if (!this.dir.isDirectory() && !this.dir.mkdirs()) {
            throw new IOException("cannot create " + this.dir);
         }

         String name = source.getName();
         int dot = name.lastIndexOf(46);
         String base = dot > 0 ? name.substring(0, dot) : name;
         String ext = dot > 0 ? name.substring(dot).toLowerCase(Locale.ROOT) : "";
         if (!ImageLoader.isImageName(name)) {
            base = name;
            ext = image.format.extension();
         }

         File target = new File(this.dir, base + ext);

         for (int n = 2; target.exists() && (target.length() != source.length() || !sameContent(target, source)); n++) {
            target = new File(this.dir, base + " (" + n + ")" + ext);
         }

         if (!target.exists()) {
            Files.copy(source.toPath(), target.toPath());
         }

         final String imported = target.getName();
         this.post(new Runnable() {
            @Override
            public void run() {
               ImageLibrary.this.refresh();
               ImageLibrary.this.say("Imported " + imported, false);
            }
         });
         return imported;
      } catch (ImageLoader.LoadException var12) {
         final ImageLoader.LoadException e = var12;
         this.post(new Runnable() {
            @Override
            public void run() {
               ImageLibrary.this.say(picked + ": " + e.getMessage(), true);
            }
         });
      } catch (RuntimeException | IOException var13) {
         FuriahsVisuals.LOG.warn("Particles: could not import {}", new Object[]{source, var13});
         this.post(new Runnable() {
            @Override
            public void run() {
               ImageLibrary.this.say("Could not copy the image into the particles folder.", true);
            }
         });
      }

      return null;
   }

   private static boolean sameContent(File a, File b) {
      try {
         return Arrays.equals(Files.readAllBytes(a.toPath()), Files.readAllBytes(b.toPath()));
      } catch (IOException var3) {
         return false;
      }
   }

   boolean delete(String name) {
      ImageLibrary.Entry e = this.entries.get(name);
      if (e == null) {
         return false;
      } else {
         try {
            Files.deleteIfExists(e.file.toPath());
         } catch (IOException var4) {
            this.say("Could not delete " + name + " (is it open somewhere?)", true);
            return false;
         }

         ImageOps.delete(e.texture);
         e.texture = -1;
         this.entries.remove(name);
         this.version++;
         this.say("Deleted " + name, false);
         return true;
      }
   }

   String rename(String name, String wanted) {
      ImageLibrary.Entry e = this.entries.get(name);
      if (e == null) {
         return null;
      } else {
         String clean = wanted.trim().replaceAll("[\\\\/:*?\"<>|]", "");
         int dot = name.lastIndexOf(46);
         String ext = dot > 0 ? name.substring(dot) : "";
         if (clean.toLowerCase(Locale.ROOT).endsWith(ext.toLowerCase(Locale.ROOT)) && !ext.isEmpty()) {
            clean = clean.substring(0, clean.length() - ext.length()).trim();
         }

         if (!clean.isEmpty() && !clean.startsWith(".")) {
            String target = clean + ext;
            if (target.equals(name)) {
               return name;
            } else {
               File to = new File(this.dir, target);
               if (to.exists()) {
                  this.say(target + " already exists.", true);
                  return null;
               } else if (!e.file.renameTo(to)) {
                  this.say("Could not rename " + name + ".", true);
                  return null;
               } else {
                  LinkedHashMap<String, ImageLibrary.Entry> copy = new LinkedHashMap<>(this.entries);
                  this.entries.clear();

                  for (Map.Entry m : copy.entrySet()) {
                     if (((String)m.getKey()).equals(name)) {
                        e.name = target;
                        e.file = to;
                        e.modified = to.lastModified();
                        this.entries.put(target, e);
                     } else {
                        this.entries.put((String)m.getKey(), (ImageLibrary.Entry)m.getValue());
                     }
                  }

                  this.version++;
                  this.say("Renamed to " + target, false);
                  return target;
               }
            }
         } else {
            this.say("That name cannot be used.", true);
            return null;
         }
      }
   }

   static final class Entry {
      String name;
      File file;
      long length = -1L;
      long modified = -1L;
      ImageLibrary.State state = ImageLibrary.State.WAITING;
      String error;
      int originalWidth;
      int originalHeight;
      int size;
      String how;
      int texture = -1;
      int average = -1;
      boolean pixel;

      Entry(String name, File file) {
         this.name = name;
         this.file = file;
      }

      String sizes() {
         return this.originalWidth + "×" + this.originalHeight + " → " + this.size + "×" + this.size + " (" + this.how + ")";
      }
   }

   static enum State {
      WAITING,
      READY,
      ERROR;
   }
}
