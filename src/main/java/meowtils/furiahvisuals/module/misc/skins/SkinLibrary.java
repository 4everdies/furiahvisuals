package meowtils.furiahvisuals.module.misc.skins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.image.ImageLoader;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

public final class SkinLibrary {
   private static final long KEEP_TEXTURE = 4000L;
   public static final long UNDO_MS = 5000L;
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private final File dir;
   private final Map<String, SkinLibrary.Entry> entries = new LinkedHashMap<>();
   private final Map<String, String> hashes = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<Runnable> clientTasks = new ConcurrentLinkedQueue<>();
   private final Map<String, String> duplicates = new HashMap<>();
   private ExecutorService worker;
   private boolean loaded;
   private boolean scanned;
   private int version;
   private String message;
   private boolean messageError;
   private long messageAt;
   private String highlight;
   private long highlightAt;
   private SkinLibrary.Entry undoEntry;
   private boolean undoWasWorn;
   private Runnable onTexture;
   volatile boolean autoModel = true;
   private List<String> said;
   private volatile long saveStamp;
   static final int RECENT = 10;
   private final List<String> recent = new ArrayList<>();

   public SkinLibrary(File dir) {
      this.dir = dir;
   }

   public File getDir() {
      return this.dir;
   }

   public int version() {
      return this.version;
   }

   void onTexture(Runnable r) {
      this.onTexture = r;
   }

   public List<SkinLibrary.Entry> entries() {
      ArrayList<SkinLibrary.Entry> out = new ArrayList<>(this.entries.size());

      for (SkinLibrary.Entry e : this.entries.values()) {
         if (!e.deleting()) {
            out.add(e);
         }
      }

      return out;
   }

   public List<SkinLibrary.Entry> sorted(final SkinLibrary.Sort sort, final boolean favoritesFirst, String filter) {
      ArrayList<SkinLibrary.Entry> out = new ArrayList<>();
      String f = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);

      for (SkinLibrary.Entry e : this.entries()) {
         if (f.isEmpty() || e.name.toLowerCase(Locale.ROOT).contains(f)) {
            out.add(e);
         }
      }

      Collections.sort(out, new Comparator<SkinLibrary.Entry>() {
         public int compare(SkinLibrary.Entry a, SkinLibrary.Entry b) {
            if (favoritesFirst && a.favorite != b.favorite) {
               return a.favorite ? -1 : 1;
            } else {
               int c;
               switch (sort) {
                  case NAME:
                     c = a.name.compareToIgnoreCase(b.name);
                     break;
                  case ADDED:
                     c = Long.compare(b.added, a.added);
                     break;
                  default:
                     c = Long.compare(b.lastWorn, a.lastWorn);
                     if (c == 0) {
                        c = Long.compare(b.added, a.added);
                     }
               }

               return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
            }
         }
      });
      return out;
   }

   public boolean contains(SkinLibrary.Entry e) {
      return e != null && this.entries.get(e.file) == e && !e.deleting();
   }

   public SkinLibrary.Entry byHash(String hash) {
      if (hash != null && !hash.isEmpty()) {
         for (SkinLibrary.Entry e : this.entries.values()) {
            if (!e.deleting() && hash.equals(e.hash)) {
               return e;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public SkinLibrary.Entry byName(String name) {
      if (name == null) {
         return null;
      } else {
         for (SkinLibrary.Entry e : this.entries.values()) {
            if (!e.deleting() && e.name.equalsIgnoreCase(name)) {
               return e;
            }
         }

         return null;
      }
   }

   public void say(String text, boolean error) {
      this.message = text;
      this.messageError = error;
      this.messageAt = ClientClock.millis();
      if (this.said != null) {
         this.said.add(text);
      }
   }

   public List<String> messagesForTest(boolean keep) {
      List<String> out = (List<String>)(this.said == null ? new ArrayList<>() : this.said);
      this.said = keep ? new ArrayList<>() : null;
      return out;
   }

   public String message() {
      return this.message != null && ClientClock.millis() - this.messageAt < (this.messageError ? 12000L : 6000L) ? this.message : null;
   }

   public boolean messageIsError() {
      return this.messageError;
   }

   public String highlight() {
      return this.highlight != null && ClientClock.millis() - this.highlightAt < 2000L ? this.highlight : null;
   }

   public long highlightAt() {
      return this.highlightAt;
   }

   private void point(String hash) {
      this.highlight = hash;
      this.highlightAt = ClientClock.millis();
   }

   void post(Runnable task) {
      this.clientTasks.add(task);
   }

   private File jsonFile() {
      return new File(this.dir, "library.json");
   }

   public void load() {
      if (!this.loaded) {
         this.loaded = true;
         File f = this.jsonFile();
         if (f.isFile()) {
            try {
               JsonElement root = new JsonParser().parse(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
               if (!root.isJsonObject() || !root.getAsJsonObject().has("skins")) {
                  return;
               }

               if (root.getAsJsonObject().has("recent") && root.getAsJsonObject().get("recent").isJsonArray()) {
                  for (JsonElement el : root.getAsJsonObject().getAsJsonArray("recent")) {
                     if (el.isJsonPrimitive() && this.recent.size() < 10 && SkinSearch.invalid(el.getAsString()) == null) {
                        this.recent.add(el.getAsString());
                     }
                  }
               }

               for (JsonElement elx : root.getAsJsonObject().getAsJsonArray("skins")) {
                  JsonObject o;
                  String file;
                  if (elx.isJsonObject()
                     && !(file = str(o = elx.getAsJsonObject(), "file", "")).isEmpty()
                     && !file.contains("/")
                     && !file.contains("\\")
                     && !this.entries.containsKey(file)) {
                     SkinLibrary.Entry e = new SkinLibrary.Entry();
                     e.file = file;
                     e.name = cleanName(str(o, "name", baseName(file)), baseName(file));
                     e.slim = "slim".equals(str(o, "model", "classic"));
                     e.source = SkinLibrary.Source.of(str(o, "source", "folder"));
                     e.added = num(o, "added");
                     e.lastWorn = num(o, "lastWorn");
                     e.favorite = o.has("favorite") && o.get("favorite").getAsBoolean();
                     e.hash = str(o, "hash", "");
                     e.size = (int)Math.max(64L, Math.min(1024L, num(o, "size") == 0L ? 64L : num(o, "size")));
                     e.account = o.has("account") && o.get("account").getAsBoolean();
                     this.entries.put(file, e);
                     if (!e.hash.isEmpty()) {
                        this.hashes.put(e.hash, file);
                     }
                  }
               }

               this.version++;
            } catch (RuntimeException | IOException var8) {
               FuriahsVisuals.LOG.warn("Skins: could not read {}; the folder's skins are listed again", new Object[]{f, var8});
            }
         }
      }
   }

   private static String str(JsonObject o, String key, String fallback) {
      JsonElement e = o.get(key);
      return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
   }

   private static long num(JsonObject o, String key) {
      try {
         JsonElement e = o.get(key);
         return e != null && e.isJsonPrimitive() ? e.getAsLong() : 0L;
      } catch (RuntimeException var3) {
         return 0L;
      }
   }

   public void save() {
      JsonObject root = new JsonObject();
      root.addProperty("version", 1);
      JsonArray list = new JsonArray();

      for (SkinLibrary.Entry entry : this.entries.values()) {
         JsonObject o = new JsonObject();
         o.addProperty("name", entry.name);
         o.addProperty("file", entry.file);
         o.addProperty("model", entry.slim ? "slim" : "classic");
         o.addProperty("source", entry.source.key);
         o.addProperty("added", entry.added);
         o.addProperty("lastWorn", entry.lastWorn);
         o.addProperty("favorite", entry.favorite);
         o.addProperty("hash", entry.hash);
         o.addProperty("size", entry.size);
         o.addProperty("hd", entry.hd());
         o.addProperty("account", entry.account);
         list.add(o);
      }

      root.add("skins", list);
      JsonArray searches = new JsonArray();

      for (String s : this.recent) {
         searches.add(new JsonPrimitive(s));
      }

      root.add("recent", searches);
      final String string = GSON.toJson(root);
      final long stamp = ++this.saveStamp;
      this.worker().submit(new Runnable() {
         @Override
         public void run() {
            if (stamp == SkinLibrary.this.saveStamp) {
               SkinLibrary.this.writeJson(string);
            }
         }
      });
   }

   private void writeJson(String text) {
      File f = this.jsonFile();

      try {
         if (!this.dir.isDirectory() && !this.dir.mkdirs()) {
            throw new IOException("cannot create " + this.dir);
         }

         File tmp = new File(this.dir, "library.json.tmp");
         Files.write(tmp.toPath(), text.getBytes(StandardCharsets.UTF_8));

         try {
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (IOException var5) {
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (RuntimeException | IOException var6) {
         FuriahsVisuals.LOG.warn("Skins: could not write {}", new Object[]{f, var6});
      }
   }

   public boolean hasScanned() {
      return this.scanned;
   }

   public void refresh() {
      this.load();
      this.scanned = true;
      if (!this.dir.isDirectory() && !this.dir.mkdirs()) {
         this.say("Could not create " + this.dir.getPath(), true);
      } else {
         File[] files = this.dir.listFiles();
         if (files != null) {
            HashSet<String> seen = new HashSet<>();
            boolean changed = false;
            long now = System.currentTimeMillis();

            for (File f : files) {
               String name = f.getName();
               if (f.isFile() && name.toLowerCase(Locale.ROOT).endsWith(".png")) {
                  seen.add(name);
                  String stamp = f.length() + ":" + f.lastModified();
                  if (!stamp.equals(this.duplicates.get(name))) {
                     SkinLibrary.Entry e = this.entries.get(name);
                     if (e == null) {
                        e = new SkinLibrary.Entry();
                        e.file = name;
                        e.name = cleanName(baseName(name), "Skin");
                        e.added = now;
                        e.source = SkinLibrary.Source.FOLDER;
                        e.slim = false;
                        this.entries.put(name, e);
                        changed = true;
                        e.length = -2L;
                     }

                     if (e.length != f.length() || e.modified != f.lastModified()) {
                        this.queue(e, f.length(), f.lastModified());
                     }
                  }
               }
            }

            Iterator<SkinLibrary.Entry> it = this.entries.values().iterator();

            while (it.hasNext()) {
               SkinLibrary.Entry ex = it.next();
               if (!seen.contains(ex.file) && !ex.deleting()) {
                  this.free(ex);
                  this.hashes.remove(ex.hash);
                  it.remove();
                  changed = true;
               }
            }

            if (changed) {
               this.version++;
               this.save();
            }
         }
      }
   }

   private void queue(final SkinLibrary.Entry e, long length, long modified) {
      final boolean fresh = e.length == -2L;
      e.length = length;
      e.modified = modified;
      e.queued = true;
      if (e.state != SkinLibrary.State.READY) {
         e.state = SkinLibrary.State.WAITING;
      }

      final File file = new File(this.dir, e.file);
      this.worker().submit(new Runnable() {
         @Override
         public void run() {
            try {
               final SkinImage img = SkinImage.read(ImageLoader.readBytes(file), file.getName());
               SkinLibrary.this.post(new Runnable() {
                  @Override
                  public void run() {
                     SkinLibrary.this.ready(e, img, fresh);
                  }
               });
            } catch (final ImageLoader.LoadException var2) {
               SkinLibrary.this.post(new Runnable() {
                  @Override
                  public void run() {
                     SkinLibrary.this.failed(e, var2.getMessage());
                  }
               });
            } catch (Throwable var3) {
               FuriahsVisuals.LOG.warn("Skins: could not read {}", new Object[]{file, var3});
               SkinLibrary.this.post(new Runnable() {
                  @Override
                  public void run() {
                     SkinLibrary.this.failed(e, "Could not read this file.");
                  }
               });
            }
         }
      });
   }

   private void ready(SkinLibrary.Entry e, SkinImage img, boolean fresh) {
      e.queued = false;
      if (this.entries.get(e.file) == e) {
         SkinLibrary.Entry same = this.byHash(img.hash);
         if (same != null && same != e) {
            this.duplicates.put(e.file, e.length + ":" + e.modified);
            this.entries.remove(e.file);
            this.version++;
            this.say(e.file + " is already in your library as " + same.name + ".", false);
            this.point(same.hash);
            this.save();
         } else {
            boolean pixelsChanged = !img.hash.equals(e.hash);
            if (pixelsChanged && !e.hash.isEmpty()) {
               this.hashes.remove(e.hash);
               this.free(e);
            }

            e.hash = img.hash;
            e.size = img.size;
            this.hashes.put(e.hash, e.file);
            if (fresh) {
               e.slim = this.autoModel && img.slim;
            }

            int[] side = new int[1];
            int[] face = img.face(side);
            e.face = face;
            e.faceSide = side[0];
            if (e.faceTexture > 0) {
               TextureUtil.deleteTexture(e.faceTexture);
            }

            e.faceTexture = uploadFace(face, side[0]);
            e.state = SkinLibrary.State.READY;
            e.error = null;
            e.version++;
            if (System.currentTimeMillis() - e.wantedAt < 1500L && e.texture == null) {
               this.upload(e, img);
            }

            this.version++;
            if (pixelsChanged || fresh) {
               this.save();
            }
         }
      }
   }

   private void failed(SkinLibrary.Entry e, String error) {
      e.queued = false;
      if (this.entries.get(e.file) == e) {
         this.free(e);
         e.state = SkinLibrary.State.ERROR;
         e.error = error;
         this.version++;
      }
   }

   private static int uploadFace(int[] face, int side) {
      int id = TextureUtil.glGenTextures();
      TextureUtil.allocateTexture(id, side, side);
      TextureUtil.uploadTexture(id, face, side, side);
      return id;
   }

   private ExecutorService worker() {
      if (this.worker == null) {
         this.worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
               Thread t = new Thread(r, "Furiah's Visuals skins");
               t.setDaemon(true);
               t.setPriority(1);
               return t;
            }
         });
      }

      return this.worker;
   }

   public boolean want(SkinLibrary.Entry e) {
      if (e != null && e.state != SkinLibrary.State.ERROR) {
         e.wantedAt = System.currentTimeMillis();
         if (e.texture != null) {
            return true;
         } else {
            if (!e.queued) {
               File f = new File(this.dir, e.file);
               e.length = -1L;
               this.queue(e, f.length(), f.lastModified());
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public void ensureFace(SkinLibrary.Entry e) {
      if (e != null && e.face == null && !e.queued && e.state != SkinLibrary.State.ERROR) {
         File f = new File(this.dir, e.file);
         e.length = -1L;
         this.queue(e, f.length(), f.lastModified());
      }
   }

   private void upload(SkinLibrary.Entry e, SkinImage img) {
      SkinTexture t = new SkinTexture(img.argb, img.size);
      Minecraft.getMinecraft().getTextureManager().loadTexture(e.location(), t);
      e.texture = t;
      if (this.onTexture != null) {
         this.onTexture.run();
      }
   }

   private void free(SkinLibrary.Entry e) {
      if (e.texture != null) {
         e.texture.deleteGlTexture();
         e.texture = null;
      }
   }

   public void update(SkinLibrary.Entry keep) {
      Runnable task;
      for (int n = 0; n < 3 && (task = this.clientTasks.poll()) != null; n++) {
         try {
            task.run();
         } catch (RuntimeException | LinkageError var7) {
            FuriahsVisuals.LOG.warn("Skins: a task failed", var7);
         }
      }

      long now = System.currentTimeMillis();
      if (this.undoEntry != null && now >= this.undoEntry.deleteAt) {
         this.finishDelete();
      }

      for (SkinLibrary.Entry e : this.entries.values()) {
         if (e.texture != null && e != keep && now - e.wantedAt > 4000L) {
            this.free(e);
         }
      }
   }

   public String importFile(File source) {
      final String picked = source.getName();

      try {
         if (source.length() > 8388608L) {
            throw new ImageLoader.LoadException(String.format(Locale.ROOT, "This file is %.1f MB; skins can be up to 8 MB.", source.length() / 1048576.0));
         }

         byte[] bytes = ImageLoader.readBytes(source);
         final SkinImage img = SkinImage.read(bytes, picked);
         if (this.hashes.containsKey(img.hash)) {
            this.post(new Runnable() {
               @Override
               public void run() {
                  SkinLibrary.Entry same = SkinLibrary.this.byHash(img.hash);
                  SkinLibrary.this.say("Already in your library" + (same != null ? ": " + same.name : "") + ".", false);
                  SkinLibrary.this.point(img.hash);
               }
            });
            return null;
         }

         File target = this.copyInto(bytes, baseName(picked));
         final String file = target.getName();
         this.hashes.put(img.hash, file);
         final boolean slim = this.autoModel && img.slim;
         final String name = cleanName(baseName(picked), "Skin");
         this.post(new Runnable() {
            @Override
            public void run() {
               SkinLibrary.this.add(file, name, slim, SkinLibrary.Source.UPLOAD, img);
               SkinLibrary.this.say("Added " + name + (img.legacy ? " (an old 64×32 skin, brought up to date)" : "") + ".", false);
            }
         });
         return file;
      } catch (ImageLoader.LoadException var9) {
         final ImageLoader.LoadException e = var9;
         this.post(new Runnable() {
            @Override
            public void run() {
               SkinLibrary.this.say(picked + ": " + e.getMessage(), true);
            }
         });
      } catch (RuntimeException | IOException var10) {
         FuriahsVisuals.LOG.warn("Skins: could not add {}", new Object[]{source, var10});
         this.post(new Runnable() {
            @Override
            public void run() {
               SkinLibrary.this.say("Could not copy the skin into the skins folder.", true);
            }
         });
      }

      return null;
   }

   public List<String> recent() {
      return Collections.unmodifiableList(this.recent);
   }

   void searched(String name) {
      Iterator<String> it = this.recent.iterator();

      while (it.hasNext()) {
         if (it.next().equalsIgnoreCase(name)) {
            it.remove();
         }
      }

      this.recent.add(0, name);

      while (this.recent.size() > 10) {
         this.recent.remove(this.recent.size() - 1);
      }

      this.save();
   }

   public void clearRecent() {
      if (!this.recent.isEmpty()) {
         this.recent.clear();
         this.save();
      }
   }

   public void recentForTest(List<String> names) {
      this.recent.clear();
      this.recent.addAll(names);
      this.save();
   }

   void saveDownloaded(
      final byte[] png, final String name, final boolean slim, final SkinImage img, final SkinLibrary.Source source, final Consumer<SkinLibrary.Entry> then
   ) {
      SkinLibrary.Entry same = this.byHash(img.hash);
      if (same == null && !this.hashes.containsKey(img.hash)) {
         this.hashes.put(img.hash, "");
         this.worker().submit(new Runnable() {
            @Override
            public void run() {
               try {
                  final File target = SkinLibrary.this.copyInto(png, name);
                  SkinLibrary.this.post(new Runnable() {
                     @Override
                     public void run() {
                        SkinLibrary.Entry e = SkinLibrary.this.add(target.getName(), SkinLibrary.cleanName(name, "Skin"), slim, source, img);
                        SkinLibrary.this.say("Saved " + e.name + " to your library.", false);
                        if (then != null) {
                           then.accept(e);
                        }
                     }
                  });
               } catch (RuntimeException | IOException var2) {
                  FuriahsVisuals.LOG.warn("Skins: could not save {}", new Object[]{name, var2});
                  SkinLibrary.this.hashes.remove(img.hash);
                  SkinLibrary.this.post(new Runnable() {
                     @Override
                     public void run() {
                        SkinLibrary.this.say("Could not save the skin into the skins folder.", true);
                     }
                  });
               }
            }
         });
      } else {
         this.say("Already in your library" + (same != null ? ": " + same.name : "") + ".", false);
         this.point(img.hash);
         if (same != null && then != null) {
            then.accept(same);
         }
      }
   }

   File copyInto(byte[] bytes, String base) throws IOException {
      if (!this.dir.isDirectory() && !this.dir.mkdirs()) {
         throw new IOException("cannot create " + this.dir);
      } else {
         String safe = base.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
         if (safe.isEmpty() || safe.startsWith(".")) {
            safe = "skin";
         }

         File target = new File(this.dir, safe + ".png");

         for (int n = 2; target.exists(); n++) {
            target = new File(this.dir, safe + " (" + n + ").png");
         }

         Files.write(target.toPath(), bytes);
         return target;
      }
   }

   SkinLibrary.Entry add(String file, String name, boolean slim, SkinLibrary.Source source, SkinImage img) {
      SkinLibrary.Entry e = new SkinLibrary.Entry();
      e.file = file;
      e.name = name;
      e.slim = slim;
      e.source = source;
      e.added = System.currentTimeMillis();
      File f = new File(this.dir, file);
      e.length = f.length();
      e.modified = f.lastModified();
      this.entries.put(file, e);
      e.wantedAt = System.currentTimeMillis();
      this.ready(e, img, false);
      e.slim = slim;
      this.version++;
      this.save();
      return e;
   }

   public boolean rename(SkinLibrary.Entry e, String wanted) {
      String clean = cleanName(wanted, null);
      if (clean == null) {
         this.say("Names are 1 to 32 letters long.", true);
         return false;
      } else {
         if (!clean.equals(e.name)) {
            e.name = clean;
            this.version++;
            this.save();
         }

         return true;
      }
   }

   public void setSlim(SkinLibrary.Entry e, boolean slim) {
      if (e.slim != slim) {
         e.slim = slim;
         e.version++;
         this.version++;
         this.save();
      }
   }

   public void setFavorite(SkinLibrary.Entry e, boolean favorite) {
      if (e.favorite != favorite) {
         e.favorite = favorite;
         this.version++;
         this.save();
      }
   }

   void markAccount(SkinLibrary.Entry e) {
      for (SkinLibrary.Entry x : this.entries.values()) {
         x.account = x == e;
      }

      this.version++;
      this.save();
   }

   void worn(SkinLibrary.Entry e) {
      e.lastWorn = System.currentTimeMillis();
      this.version++;
      this.save();
   }

   public void delete(SkinLibrary.Entry e, boolean wasWorn) {
      if (this.undoEntry != null) {
         this.finishDelete();
      }

      e.deleteAt = System.currentTimeMillis() + 5000L;
      this.undoEntry = e;
      this.undoWasWorn = wasWorn;
      this.version++;
   }

   public SkinLibrary.Entry undoEntry() {
      return this.undoEntry;
   }

   public SkinLibrary.Entry undo(boolean[] wornOut) {
      SkinLibrary.Entry e = this.undoEntry;
      if (e == null) {
         return null;
      } else {
         this.undoEntry = null;
         e.deleteAt = 0L;
         wornOut[0] = this.undoWasWorn;
         this.version++;
         return e;
      }
   }

   public void finishDelete() {
      SkinLibrary.Entry e = this.undoEntry;
      if (e != null) {
         this.undoEntry = null;

         try {
            Files.deleteIfExists(new File(this.dir, e.file).toPath());
         } catch (IOException var3) {
            FuriahsVisuals.LOG.warn("Skins: could not delete {}", new Object[]{e.file, var3});
            e.deleteAt = 0L;
            this.say("Could not delete " + e.name + " (is the file open somewhere?).", true);
            this.version++;
            return;
         }

         this.free(e);
         if (e.faceTexture > 0) {
            TextureUtil.deleteTexture(e.faceTexture);
            e.faceTexture = -1;
         }

         this.hashes.remove(e.hash);
         this.entries.remove(e.file);
         this.version++;
         this.save();
      }
   }

   static String baseName(String file) {
      int dot = file.lastIndexOf(46);
      return dot > 0 ? file.substring(0, dot) : file;
   }

   static String cleanName(String name, String fallback) {
      String clean = TextSetting.clean(name == null ? "" : name.trim(), 32).trim();
      return clean.isEmpty() ? fallback : clean;
   }

   public static final class Entry {
      public String name;
      String file;
      public boolean slim;
      public SkinLibrary.Source source = SkinLibrary.Source.FOLDER;
      public long added;
      public long lastWorn;
      public boolean favorite;
      public String hash = "";
      public int size = 64;
      public boolean account;
      long length = -1L;
      long modified = -1L;
      public SkinLibrary.State state = SkinLibrary.State.WAITING;
      public String error;
      boolean queued;
      int[] face;
      int faceSide;
      int faceTexture = -1;
      SkinTexture texture;
      long wantedAt;
      private ResourceLocation location;
      long deleteAt;
      public int version;

      public ResourceLocation location() {
         if (this.location == null || !this.location.getResourcePath().endsWith(this.hash)) {
            this.location = new ResourceLocation("furiahsvisuals", "skins/" + this.hash);
         }

         return this.location;
      }

      public boolean hd() {
         return this.size > 64;
      }

      public boolean textureReady() {
         return this.texture != null;
      }

      public int faceTexture() {
         return this.faceTexture;
      }

      public String fileName() {
         return this.file;
      }

      boolean deleting() {
         return this.deleteAt != 0L;
      }
   }

   public static enum Sort implements Displayable {
      RECENT("Recently used", "The skins you wore last come first"),
      NAME("Name", "A to Z"),
      ADDED("Date added", "The newest come first");

      private final String name;
      private final String description;

      private Sort(String name, String description) {
         this.name = name;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.name;
      }

      @Override
      public String description() {
         return this.description;
      }
   }

   public static enum Source {
      UPLOAD("upload"),
      FOLDER("folder"),
      SEARCH("search"),
      ACCOUNT("account");

      final String key;

      private Source(String key) {
         this.key = key;
      }

      static SkinLibrary.Source of(String key) {
         for (SkinLibrary.Source s : values()) {
            if (s.key.equals(key)) {
               return s;
            }
         }

         return FOLDER;
      }
   }

   public static enum State {
      WAITING,
      READY,
      ERROR;
   }
}
