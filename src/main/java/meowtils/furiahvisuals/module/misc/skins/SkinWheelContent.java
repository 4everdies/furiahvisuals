package meowtils.furiahvisuals.module.misc.skins;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.core.Keybinds;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.module.weapons.WheelContent;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.ColorUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;

final class SkinWheelContent implements WheelContent {
   private final Skins skins;
   private final List<SkinLibrary.Entry> favorites = new ArrayList<>();
   private int builtVersion = -1;
   private SkinLibrary.Entry previous;
   private boolean hasPrevious;

   SkinWheelContent(Skins skins) {
      this.skins = skins;
   }

   List<SkinLibrary.Entry> favorites() {
      SkinLibrary lib = this.skins.library();
      if (lib.version() != this.builtVersion) {
         this.builtVersion = lib.version();
         this.favorites.clear();

         for (SkinLibrary.Entry e : lib.sorted(SkinLibrary.Sort.NAME, true, null)) {
            if (e.favorite && e.state != SkinLibrary.State.ERROR) {
               this.favorites.add(e);
            }
         }
      }

      return this.favorites;
   }

   private SkinLibrary.Entry entry(int index) {
      List<SkinLibrary.Entry> f = this.favorites();
      return index >= 1 && index - 1 < f.size() ? f.get(index - 1) : null;
   }

   @Override
   public int size() {
      return 1 + this.favorites().size();
   }

   @Override
   public int perPage() {
      return 0;
   }

   @Override
   public int current() {
      if (!this.skins.active()) {
         return 0;
      } else if (this.skins.tried() != null) {
         return -1;
      } else {
         int i = this.favorites().indexOf(this.skins.wornEntry());
         return i < 0 ? -1 : i + 1;
      }
   }

   @Override
   public boolean apply(int index) {
      SkinLibrary.Entry was = this.skins.active() && this.skins.tried() == null ? this.skins.wornEntry() : null;
      boolean accountBefore = !this.skins.active();
      if (index == 0) {
         if (accountBefore) {
            return false;
         } else {
            this.previous = was;
            this.hasPrevious = was != null;
            this.skins.wearAccount();
            this.skins.announce("My account skin");
            return true;
         }
      } else {
         SkinLibrary.Entry e = this.entry(index);
         if (e != null && !this.skins.wearing(e)) {
            this.previous = was;
            this.hasPrevious = true;
            this.skins.wear(e);
            this.skins.announce(e.name);
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean swapBack() {
      if (!this.hasPrevious) {
         return false;
      } else {
         SkinLibrary.Entry back = this.previous;
         SkinLibrary.Entry now = this.skins.active() && this.skins.tried() == null ? this.skins.wornEntry() : null;
         if (back != null && !this.skins.library().contains(back)) {
            return false;
         } else {
            this.previous = now;
            if (back == null) {
               if (!this.skins.active()) {
                  return false;
               }

               this.skins.wearAccount();
               this.skins.announce("My account skin");
            } else {
               this.skins.wear(back);
               this.skins.announce(back.name);
            }

            return true;
         }
      }
   }

   @Override
   public String name(int index) {
      if (index < 0) {
         if (!this.skins.active()) {
            return "My account skin";
         } else {
            SkinSearch.Result t = this.skins.tried();
            if (t != null) {
               return t.name;
            } else {
               SkinLibrary.Entry w = this.skins.wornEntry();
               return w == null ? "My account skin" : w.name;
            }
         }
      } else if (index == 0) {
         return "My account skin";
      } else {
         SkinLibrary.Entry e = this.entry(index);
         return e == null ? "?" : e.name;
      }
   }

   @Override
   public String detail(int index) {
      SkinLibrary.Entry e;
      if (index < 0) {
         e = this.skins.active() && this.skins.tried() == null ? this.skins.wornEntry() : null;
         if (e == null) {
            return this.skins.active() ? "Not saved" : "Everyone sees it";
         }
      } else {
         if (index == 0) {
            return "Everyone sees it";
         }

         e = this.entry(index);
         if (e == null) {
            return "";
         }
      }

      return (e.slim ? "Slim" : "Classic") + (e.hd() ? " · HD " + e.size : "");
   }

   @Override
   public String target() {
      return "Only you see it";
   }

   @Override
   public boolean usable() {
      return this.skins.ownerOn();
   }

   @Override
   public KeyBinding key() {
      return Keybinds.skinWheel;
   }

   @Override
   public void pressed() {
      SkinLibrary lib = this.skins.library();

      for (SkinLibrary.Entry e : this.favorites()) {
         lib.ensureFace(e);
      }
   }

   @Override
   public void lit(int index) {
   }

   @Override
   public void idle() {
   }

   @Override
   public void prepare(int[] shown, int count, int lit, int middle, int slotPx, int bigPx, boolean live, double time) {
   }

   @Override
   public boolean picture(int index, float x, float y, float size, float alpha) {
      return this.head(index, x, y, size * 0.74F, alpha);
   }

   @Override
   public boolean bigPicture(int index, float x, float y, float size, float alpha) {
      return this.head(index, x, y, size * 0.5F, alpha);
   }

   private boolean head(int index, float x, float y, float s, float alpha) {
      int color = ColorUtil.withAlpha(-1, Math.max(0.0F, Math.min(1.0F, alpha)));
      float r = s * 0.16F;
      SkinLibrary.Entry e = null;
      boolean account;
      if (index < 0) {
         SkinSearch.Result t = this.skins.active() ? this.skins.tried() : null;
         if (t != null && t.texture != null) {
            return face(t.location(), x, y, s, r, color);
         }

         e = this.skins.active() ? this.skins.wornEntry() : null;
         account = e == null;
      } else {
         account = index == 0;
         if (!account) {
            e = this.entry(index);
         }
      }

      if (account) {
         boolean[] slim = new boolean[1];
         return face(SkinHooks.accountSkin(slim), x, y, s, r, color);
      } else if (e != null && e.faceTexture() > 0) {
         Ui.roundedImage(e.faceTexture(), x - s / 2.0F, y - s / 2.0F, s, s, r, 0.0F, 0.0F, 1.0F, 1.0F, color);
         return true;
      } else {
         return false;
      }
   }

   private static boolean face(ResourceLocation skin, float x, float y, float s, float r, int color) {
      int t = BodyProxy.texture(skin);
      if (t <= 0) {
         return false;
      } else {
         Ui.roundedImagePair(t, x - s / 2.0F, y - s / 2.0F, s, s, r, 0.125F, 0.125F, 0.25F, 0.25F, true, 0.625F, 0.125F, 0.75F, 0.25F, color);
         return true;
      }
   }
}
