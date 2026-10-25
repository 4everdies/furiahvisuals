package meowtils.furiahvisuals.module.misc.skins;

import java.io.File;
import java.util.List;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.module.weapons.RadialWheel;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.TextSetting;
import net.minecraft.util.ResourceLocation;

public final class Skins {
   private final Module owner;
   public final BooleanSetting on;
   public final TextSetting worn;
   public final ModeSetting<SkinLibrary.Sort> sort;
   public final BooleanSetting favoritesFirst;
   public final BooleanSetting autoModel;
   public final BooleanSetting rememberSearches;
   public final BooleanSetting notify;
   public final BooleanSetting askBeforeAccount;
   private SkinLibrary library;
   private RadialWheel wheel;
   private SkinWheelContent wheelContent;
   private SkinPreviews previews;
   private SkinLibrary.Entry wornEntry;
   private String resolvedFrom;
   private Object shown;
   private String warnedAbout;

   public Skins(
      Module owner,
      BooleanSetting on,
      TextSetting worn,
      ModeSetting<SkinLibrary.Sort> sort,
      BooleanSetting favoritesFirst,
      BooleanSetting autoModel,
      BooleanSetting rememberSearches,
      BooleanSetting notify,
      BooleanSetting askBeforeAccount
   ) {
      this.owner = owner;
      this.on = on;
      this.worn = worn;
      this.sort = sort;
      this.favoritesFirst = favoritesFirst;
      this.autoModel = autoModel;
      this.rememberSearches = rememberSearches;
      this.notify = notify;
      this.askBeforeAccount = askBeforeAccount;
      worn.addListener(new Runnable() {
         @Override
         public void run() {
            Skins.this.resolvedFrom = null;
         }
      });
   }

   public SkinLibrary library() {
      if (this.library == null) {
         this.library = new SkinLibrary(new File(FuriahsVisuals.instance.getDataDir(), "skins"));
         this.library.load();
      }

      this.library.autoModel = this.autoModel.get();
      return this.library;
   }

   SkinPreviews previews() {
      if (this.previews == null) {
         this.previews = new SkinPreviews();
      }

      return this.previews;
   }

   public boolean active() {
      return this.owner.isEnabled() && this.on.get() && this.wornEntry != null;
   }

   public SkinLibrary.Entry wornEntry() {
      this.resolve();
      return this.wornEntry;
   }

   SkinSearch.Result tried() {
      return null;
   }

   public void update() {
      SkinLibrary lib = this.library();
      this.resolve();
      lib.update(this.shown instanceof SkinLibrary.Entry ? (SkinLibrary.Entry)this.shown : null);
      if (!this.active()) {
         this.show(null);
      } else {
         if (lib.want(this.wornEntry)) {
            this.show(this.wornEntry);
         } else if (this.shown instanceof SkinLibrary.Entry && this.shown != this.wornEntry) {
            lib.want((SkinLibrary.Entry)this.shown);
         }
      }
   }

   public void stop() {
      this.show(null);
   }

   private void show(Object what) {
      this.shown = what;
      if (what instanceof SkinLibrary.Entry) {
         SkinLibrary.Entry e = (SkinLibrary.Entry)what;
         SkinHooks.wear(e.location(), e.slim);
      } else {
         SkinHooks.wear(null, false);
      }
   }

   private void resolve() {
      String v = this.worn.get();
      SkinLibrary lib = this.library();
      if (!v.equals(this.resolvedFrom) || this.wornEntry != null && !lib.contains(this.wornEntry)) {
         this.resolvedFrom = v;
         this.wornEntry = null;
         if (!v.isEmpty()) {
            int bar = v.indexOf(124);
            String hash = bar < 0 ? v : v.substring(0, bar);
            String name = bar < 0 ? "" : v.substring(bar + 1);
            SkinLibrary.Entry e = lib.byHash(hash);
            if (e == null && !name.isEmpty()) {
               e = lib.byName(name);
            }

            if (e != null) {
               this.wornEntry = e;
            } else {
               if (this.on.get() && this.owner.isEnabled() && !v.equals(this.warnedAbout)) {
                  this.warnedAbout = v;
                  FuriahsVisuals.chat(
                     "Skins: "
                        + (name.isEmpty() ? "the skin picked" : "§f" + name + "§7")
                        + " isn't in your skin library, so your account skin shows. Add it in Misc › Skins."
                  );
               }
            }
         }
      }
   }

   public void wear(SkinLibrary.Entry e) {
      if (e != null && e.state != SkinLibrary.State.ERROR) {
         SkinLibrary lib = this.library();
         this.worn.set(value(e));
         this.resolvedFrom = null;
         this.resolve();
         lib.worn(e);
         lib.want(e);
         this.turnOn();
      }
   }

   private void turnOn() {
      if (!this.on.get()) {
         this.on.set(true);
      }

      if (!this.owner.isEnabled()) {
         this.owner.setEnabled(true);
      }
   }

   public void wearAccount() {
      this.on.set(false);
      this.show(null);
   }

   public boolean wearing(SkinLibrary.Entry e) {
      return e != null && this.active() && e == this.wornEntry;
   }

   static String value(SkinLibrary.Entry e) {
      return e.hash + "|" + e.name;
   }

   public void changed(SkinLibrary.Entry e) {
      if (e == this.wornEntry) {
         this.worn.set(value(e));
         this.resolvedFrom = this.worn.get();
         if (this.shown == e) {
            this.show(e);
         }
      }
   }

   public void delete(SkinLibrary.Entry e) {
      boolean wasWorn = this.wearing(e);
      if (wasWorn) {
         this.wearAccount();
      }

      this.library().delete(e, wasWorn);
      if (this.previews != null) {
         this.previews.forget(e.hash);
      }
   }

   public void undo() {
      boolean[] wasWorn = new boolean[1];
      SkinLibrary.Entry e = this.library().undo(wasWorn);
      if (e != null && wasWorn[0]) {
         this.wear(e);
      }
   }

   public ResourceLocation shownSkin() {
      return SkinHooks.worn();
   }

   boolean ownerOn() {
      return this.owner.isEnabled();
   }

   public RadialWheel wheel() {
      if (this.wheel == null) {
         this.wheelContent = new SkinWheelContent(this);
         this.wheel = RadialWheel.create(this.wheelContent);
      }

      return this.wheel;
   }

   void announce(String name) {
      if (this.notify.get()) {
         WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
         if (wm != null) {
            wm.uiNotice("skin", "Skin: " + name, IslandIcons.Icon.SKIN);
         }
      }
   }

   public void cycle(int dir) {
      this.wheel();
      List<SkinLibrary.Entry> favs = this.wheelContent.favorites();
      if (favs.isEmpty()) {
         WaterMarkModule wm;
         if (this.notify.get() && (wm = FuriahsVisuals.instance.getWaterMark()) != null) {
            wm.uiNotice("skin", "No favorite skins yet", IslandIcons.Icon.SKIN);
         }
      } else {
         int at = this.active() ? favs.indexOf(this.wornEntry) : -1;
         int next = at < 0 ? (dir > 0 ? 0 : favs.size() - 1) : ((at + dir) % favs.size() + favs.size()) % favs.size();
         SkinLibrary.Entry e = favs.get(next);
         this.wear(e);
         this.announce(e.name);
      }
   }

   String accountRefused(SkinLibrary.Entry e) {
      return "Changing your Mojang account's skin needs the internet, which this offline build never uses.";
   }

   public Widget currentWidget() {
      return new CurrentSkinWidget(this);
   }

   public Widget backWidget() {
      return new SkinBackWidget(this);
   }

   public Widget addWidget() {
      return new AddSkinsWidget(this);
   }

   public Widget libraryWidget() {
      return new SkinLibraryWidget(this);
   }
}
