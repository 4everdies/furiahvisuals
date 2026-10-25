package meowtils.furiahvisuals.module.misc.skins;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Density;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.popup.ContextMenu;
import meowtils.furiahvisuals.gui.popup.Popovers;
import meowtils.furiahvisuals.gui.search.SearchList;
import meowtils.furiahvisuals.gui.widget.ColorWidget;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.TextSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

final class SkinLibraryWidget extends Widget implements SearchList {
   private static final float GAP = 6.0F;
   private static final float RADIUS = 7.0F;
   private static final float HEADER = 24.0F;
   private static final float UNDO = 26.0F;
   private static final String ACCOUNT = "\u0001account";
   private static final String FILTER_HINT = "Filter skins…";
   private final Skins skins;
   private final List<SkinLibrary.Entry> list = new ArrayList<>();
   private final Map<String, Float> hovers = new HashMap<>();
   private final Pill undoPill = new Pill();
   private boolean showAccount = true;
   private int builtVersion = -1;
   private String builtKey = "";
   private Set<Object> searchKeys;
   private String filter = "";
   private boolean filterFocused;
   private float caret;
   private SkinLibrary.Entry renaming;
   private String renameText = "";
   private String hoverKey;
   private long hoverStart;
   private double clock;
   private float sortHover;
   private float favHover;
   private float filterHover;

   SkinLibraryWidget(Skins skins) {
      this.skins = skins;
   }

   private void build() {
      SkinLibrary lib = this.skins.library();
      SkinLibrary.Sort sort = this.skins.sort.get();
      boolean fav = this.skins.favoritesFirst.get();
      String key = sort + "|" + fav + "|" + this.filter + "|" + (this.searchKeys == null ? "-" : String.valueOf(this.searchKeys.hashCode()));
      if (lib.version() != this.builtVersion || !key.equals(this.builtKey)) {
         this.builtVersion = lib.version();
         this.builtKey = key;
         this.list.clear();

         for (SkinLibrary.Entry e : lib.sorted(sort, fav, this.filter)) {
            if (this.searchKeys == null || this.searchKeys.contains(e.fileName())) {
               this.list.add(e);
            }
         }

         String f = this.filter.trim().toLowerCase(Locale.ROOT);
         this.showAccount = (f.isEmpty() || "my account skin".contains(f)) && (this.searchKeys == null || this.searchKeys.contains("\u0001account"));
      }
   }

   private int count() {
      return this.list.size() + (this.showAccount ? 1 : 0);
   }

   private SkinLibrary.Entry at(int slot) {
      return this.showAccount ? (slot == 0 ? null : this.list.get(slot - 1)) : this.list.get(slot);
   }

   private static String keyOf(SkinLibrary.Entry e) {
      return e == null ? "\u0001account" : e.fileName();
   }

   private int columns() {
      if (Density.dense()) {
         return this.w >= 460.0F ? 3 : (this.w >= 300.0F ? 2 : 1);
      } else {
         float min = Density.compact() ? 78.0F : 98.0F;
         int most = Density.compact() ? 6 : 5;
         return Math.max(2, Math.min(most, (int)((this.w + 6.0F) / (min + 6.0F))));
      }
   }

   private float tileW() {
      int cols = this.columns();
      return (this.w - 6.0F * (cols - 1)) / cols;
   }

   private float tileH() {
      return Density.dense() ? 26.0F : Math.round(this.tileW() * (Density.compact() ? 1.3F : 1.4F));
   }

   private float undoH() {
      return this.skins.library().undoEntry() != null ? 30.0F : 0.0F;
   }

   private float gridY() {
      return this.y + 24.0F + this.undoH();
   }

   private float tileX(int slot) {
      return this.x + slot % this.columns() * (this.tileW() + 6.0F);
   }

   private float tileY(int slot) {
      return this.gridY() + slot / this.columns() * (this.tileH() + 6.0F);
   }

   @Override
   public float height() {
      this.build();
      int rows = (this.count() + this.columns() - 1) / this.columns();
      float grid = rows == 0 ? 24.0F : rows * this.tileH() + (rows - 1) * 6.0F;
      return 24.0F + this.undoH() + grid + 4.0F;
   }

   private float filterW() {
      return Math.max(90.0F, Math.min(180.0F, this.w * 0.42F));
   }

   private String sortLabel() {
      return "Sort: " + this.skins.sort.get().displayName();
   }

   private float favW() {
      return Ui.width("First", Density.small()) + 24.0F;
   }

   private float sortW() {
      return Ui.width(this.sortLabel(), Density.small()) + 16.0F;
   }

   private float favX() {
      return this.x + this.w - this.favW();
   }

   private float sortX() {
      return this.favX() - 6.0F - this.sortW();
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      this.skins.update();
      this.build();
      this.clock += dt;
      this.caret += dt;
      SkinLibrary lib = this.skins.library();
      this.renderHeader(mx, my, dt, accent);
      if (lib.undoEntry() != null) {
         this.renderUndo(lib, mx, my, dt, accent);
      }

      if (this.count() == 0) {
         Ui.text(
            this.filter.isEmpty() ? "No skins yet: upload one, or drop PNG skins into the folder and press Reload." : "No skin is called that.",
            this.x,
            this.gridY() + 6.0F,
            8.4F,
            Theme.TEXT_MUTED
         );
      } else {
         String over = null;
         float tw = this.tileW();
         float th = this.tileH();

         for (int s = 0; s < this.count(); s++) {
            float tx = this.tileX(s);
            float ty = this.tileY(s);
            if (mx >= tx && mx < tx + tw && my >= ty && my < ty + th) {
               over = keyOf(this.at(s));
            }
         }

         if (over == null ? this.hoverKey != null : !over.equals(this.hoverKey)) {
            this.hoverKey = over;
            this.hoverStart = ClientClock.millis();
         }

         String highlight = lib.highlight();

         for (int sx = 0; sx < this.count(); sx++) {
            SkinLibrary.Entry e = this.at(sx);
            float tx = this.tileX(sx);
            float ty = this.tileY(sx);
            String key = keyOf(e);
            boolean isOver = key.equals(over);
            float h = this.hovers.containsKey(key) ? this.hovers.get(key) : 0.0F;
            h = Easing.approach(h, isOver ? 1.0F : 0.0F, 16.0F, dt);
            this.hovers.put(key, h);
            if (!Ui.clippedOut(tx, ty, tw, th)) {
               boolean pointed = e != null && highlight != null && highlight.equals(e.hash);
               if (Density.dense()) {
                  this.renderRow(e, tx, ty, tw, th, h, mx, my, accent, pointed);
               } else {
                  this.renderTile(e, tx, ty, tw, th, h, isOver, mx, my, accent, pointed);
               }
            }
         }

         this.skins.previews().trim();
      }
   }

   private void renderHeader(float mx, float my, float dt, int accent) {
      float ts = Density.small();
      float fh = 18.0F;
      float fy = this.y + 1.0F;
      float fw = this.filterW();
      boolean overFilter = mx >= this.x && mx < this.x + fw && my >= fy && my < fy + fh;
      this.filterHover = Easing.approach(this.filterHover, !overFilter && !this.filterFocused ? 0.0F : 1.0F, 16.0F, dt);
      Ui.surface(this.x, fy, fw, fh, fh / 2.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.filterHover));
      if (this.filterFocused) {
         Ui.accentOutline(this.x, fy, fw, fh, fh / 2.0F, 0.9F, accent);
      }

      Ui.registerControl(this.x, fy, fw, fh, "filter skins");
      float textX = this.x + 9.0F;
      float room = fw - 18.0F - (this.filter.isEmpty() ? 0.0F : 10.0F);
      if (this.filter.isEmpty() && !this.filterFocused) {
         Ui.textMiddle("Filter skins…", textX, fy + fh / 2.0F, ts, Theme.TEXT_MUTED, false);
      } else {
         String shown = tail(this.filter, ts, room);
         Ui.textMiddle(shown, textX, fy + fh / 2.0F, ts, Theme.TEXT, false);
         if (this.filterFocused && this.caret % 1.0F < 0.55F) {
            Ui.rect(textX + Ui.width(shown, ts) + 1.0F, fy + 4.0F, 0.8F, fh - 8.0F, Theme.TEXT);
         }
      }

      if (!this.filter.isEmpty()) {
         float cx = this.x + fw - 9.0F;
         float cy = fy + fh / 2.0F;
         Ui.line(cx - 2.6F, cy - 2.6F, cx + 2.6F, cy + 2.6F, 1.0F, true, Theme.TEXT_DIM);
         Ui.line(cx - 2.6F, cy + 2.6F, cx + 2.6F, cy - 2.6F, 1.0F, true, Theme.TEXT_DIM);
      }

      float sx = this.sortX();
      float sw = this.sortW();
      boolean overSort = mx >= sx && mx < sx + sw && my >= fy && my < fy + fh;
      this.sortHover = Easing.approach(this.sortHover, overSort ? 1.0F : 0.0F, 16.0F, dt);
      if (sx > this.x + fw + 6.0F) {
         Ui.surface(sx, fy, sw, fh, fh / 2.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.sortHover));
         Ui.registerControl(sx, fy, sw, fh, "sort");
         Ui.textMiddle(this.sortLabel(), sx + 8.0F, fy + fh / 2.0F, ts, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.sortHover), false);
      }

      float fx = this.favX();
      float fw2 = this.favW();
      boolean fav = this.skins.favoritesFirst.get();
      boolean overFav = mx >= fx && mx < fx + fw2 && my >= fy && my < fy + fh;
      this.favHover = Easing.approach(this.favHover, overFav ? 1.0F : 0.0F, 16.0F, dt);
      Ui.surface(fx, fy, fw2, fh, fh / 2.0F, fav ? Theme.ACCENT_SOFT : ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, this.favHover));
      Ui.registerControl(fx, fy, fw2, fh, "favorites first");
      ColorWidget.star(fx + 10.0F, fy + fh / 2.0F, 3.6F, fav ? accent : Theme.TEXT_DIM, fav);
      Ui.textMiddle("First", fx + 17.0F, fy + fh / 2.0F, ts, fav ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, this.favHover), false);
   }

   private void renderUndo(SkinLibrary lib, float mx, float my, float dt, int accent) {
      SkinLibrary.Entry e = lib.undoEntry();
      float uy = this.y + 24.0F;
      float uh = 26.0F;
      Ui.surface(this.x, uy, this.w, uh, 8.0F, Theme.SURFACE);
      float left = Math.max(0.0F, (float)(e.deleteAt - System.currentTimeMillis()) / 5000.0F);
      Ui.roundRect(this.x + 4.0F, uy + uh - 3.0F, (this.w - 8.0F) * left, 1.4F, 0.7F, accent);
      String label = "Undo";
      float pw = Ui.width(label, 8.8F) + 20.0F;
      this.undoPill.layout(this.x + this.w - pw - 4.0F, uy + (uh - 20.0F) / 2.0F, label);
      this.undoPill.draw(label, mx, my, dt, accent, true, true);
      Ui.textMiddle(
         Ui.ellipsize("Deleted " + e.name, Density.text(), this.w - pw - 24.0F, false), this.x + 10.0F, uy + uh / 2.0F, Density.text(), Theme.TEXT_DIM, false
      );
   }

   private void renderTile(
      SkinLibrary.Entry e, float tx, float ty, float tw, float th, float hover, boolean isOver, float mx, float my, int accent, boolean pointed
   ) {
      SkinLibrary lib = this.skins.library();
      boolean wearing = e == null ? !this.skins.active() : this.skins.wearing(e);
      boolean compact = Density.compact();
      int tex = this.picture(e, tw, th, isOver);
      if (tex > 0) {
         Ui.roundedImage(tex, tx, ty, tw, th, 7.0F, 0.0F, 1.0F, 1.0F, 0.0F, -1);
      } else {
         Ui.gradientV(tx, ty, tw, th, 7.0F, -13881285, -15460836);
         if (e != null && e.state == SkinLibrary.State.ERROR) {
            errorMark(tx + tw / 2.0F, ty + th * 0.4F, Math.min(tw, th) * 0.14F);
         } else if (e != null && e.faceTexture() > 0) {
            float fs = Math.min(tw, th) * 0.32F;
            Ui.image(e.faceTexture(), tx + (tw - fs) / 2.0F, ty + th * 0.38F - fs / 2.0F, fs, fs, 0.0F, 0.0F, 1.0F, 1.0F, -1);
         }
      }

      if (hover > 0.01F) {
         Ui.roundRect(tx, ty, tw, th, 7.0F, ColorUtil.withAlpha(-1, 0.06F * hover));
      }

      float tagS = compact ? 5.4F : 6.0F;
      float tagH = compact ? 9.5F : 11.0F;
      float tagX = tx + 5.0F;
      if (e == null) {
         tagX += tag("ACCOUNT", tagX, ty + 5.0F, tagS, tagH);
      } else if (e.state == SkinLibrary.State.READY) {
         tagX += tag(e.slim ? "SLIM" : "CLASSIC", tagX, ty + 5.0F, tagS, tagH) + 3.0F;
         if (e.hd() && tagX + Ui.widthBold("HD", tagS) + 8.0F < tx + tw - 22.0F) {
            tagX += tag("HD", tagX, ty + 5.0F, tagS, tagH) + 3.0F;
         }

         if (e.account && tagX + Ui.widthBold("ACCOUNT", tagS) + 8.0F < tx + tw - 22.0F) {
            tag("ACCOUNT", tagX, ty + 5.0F, tagS, tagH);
         }
      }

      if (e != null && e.favorite) {
         ColorWidget.star(tx + tw - (isOver ? 22.0F : 9.0F), ty + 5.0F + tagH / 2.0F, 3.4F, -11686, true);
      }

      if (e != null && isOver) {
         moreButton(tx + tw - 13.0F, ty + 5.0F + tagH / 2.0F, mx, my);
      }

      float shadeTop = ty + th * 0.6F;
      Ui.gradientV(tx, shadeTop, tw, ty + th - shadeTop, 7.0F, 0, -805306368);
      float ns = compact ? 7.6F : 8.8F;
      float nameY = ty + th - ns - (wearing ? (compact ? 13.0F : 15.0F) : 6.0F);
      String name = e == null ? "My account skin" : e.name;
      if (e != null && e == this.renaming) {
         this.renameField(tx + 4.0F, nameY - 3.0F, tw - 8.0F, ns + 6.0F, ns, accent);
      } else {
         Ui.textBold(Ui.ellipsize(name, ns, tw - 12.0F, true), tx + 6.0F, nameY, ns, wearing ? -1 : ColorUtil.lerp(-1907990, -1, hover));
      }

      if (wearing) {
         float ws = compact ? 6.4F : 7.0F;
         Ui.circle(tx + 9.0F, ty + th - ws / 2.0F - 6.0F, 2.2F, accent);
         Ui.text("Wearing", tx + 14.0F, ty + th - ws - 6.0F, ws, ColorUtil.lerp(accent, -1, 0.35F));
      } else if (e != null && e.state == SkinLibrary.State.ERROR) {
         Ui.text(Ui.ellipsize("Not a skin", 6.6F, tw - 12.0F, false), tx + 6.0F, ty + th - 13.0F, 6.6F, Theme.DANGER);
      }

      Ui.outline(tx, ty, tw, th, 7.0F, 0.8F, wearing ? 0 : 620756991);
      Ui.registerControl(tx, ty, tw, th, "skin " + name);
      if (wearing) {
         Ui.glow(tx + tw / 2.0F, ty + th / 2.0F, tw * 1.25F, ColorUtil.withAlpha(accent, 0.2F));
         Ui.accentOutline(tx - 1.5F, ty - 1.5F, tw + 3.0F, th + 3.0F, 8.5F, 1.4F, accent);
      }

      if (pointed) {
         float a = 0.5F + 0.5F * (float)Math.sin(this.clock * 9.0);
         Ui.outline(tx - 2.5F, ty - 2.5F, tw + 5.0F, th + 5.0F, 9.5F, 1.6F, ColorUtil.withAlpha(accent, a));
      }
   }

   private void renderRow(SkinLibrary.Entry e, float rx, float ry, float rw, float rh, float hover, float mx, float my, int accent, boolean pointed) {
      boolean wearing = e == null ? !this.skins.active() : this.skins.wearing(e);
      Ui.surface(rx, ry, rw, rh, 7.0F, ColorUtil.lerp(Theme.SURFACE, Theme.SURFACE_HOVER, hover));
      if (wearing) {
         Ui.roundRect(rx, ry, rw, rh, 7.0F, Theme.ACCENT_SOFT);
         Ui.accentOutline(rx, ry, rw, rh, 7.0F, 1.1F, accent);
      }

      if (pointed) {
         float a = 0.5F + 0.5F * (float)Math.sin(this.clock * 9.0);
         Ui.outline(rx - 1.5F, ry - 1.5F, rw + 3.0F, rh + 3.0F, 8.5F, 1.4F, ColorUtil.withAlpha(accent, a));
      }

      Ui.registerControl(rx, ry, rw, rh, "skin " + (e == null ? "account" : e.name));
      float hs = rh - 8.0F;
      float hx = rx + 4.0F;
      float hy = ry + 4.0F;
      this.head(e, hx, hy, hs);
      float right = rx + rw - 6.0F;
      boolean overRow = mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
      if (e != null && overRow) {
         moreButton(right - 5.0F, ry + rh / 2.0F, mx, my);
         right -= 14.0F;
      }

      if (e != null && e.favorite) {
         ColorWidget.star(right - 4.0F, ry + rh / 2.0F, 3.2F, -11686, true);
         right -= 12.0F;
      }

      float ts = 5.6F;
      String model = e == null
         ? "ACCOUNT"
         : (e.state == SkinLibrary.State.ERROR ? "NOT A SKIN" : (e.slim ? "SLIM" : "CLASSIC") + (e.hd() ? " · HD" : "") + (e.account ? " · ACCOUNT" : ""));
      float mw = Ui.widthBold(model, ts);
      Ui.textMiddle(model, right - mw, ry + rh / 2.0F, ts, e != null && e.state == SkinLibrary.State.ERROR ? Theme.DANGER : Theme.TEXT_MUTED, true);
      right -= mw + 8.0F;
      float nx = hx + hs + 7.0F;
      float ns = 7.8F;
      String name = e == null ? "My account skin" : e.name;
      if (wearing) {
         String wl = "Wearing";
         float ww = Ui.width(wl, 6.6F);
         Ui.textMiddle(wl, right - ww, ry + rh / 2.0F, 6.6F, accent, false);
         right -= ww + 8.0F;
      }

      if (e != null && e == this.renaming) {
         this.renameField(nx - 3.0F, ry + 4.0F, Math.max(60.0F, right - nx), rh - 8.0F, ns, accent);
      } else {
         Ui.textBold(
            Ui.ellipsize(name, ns, right - nx, true),
            nx,
            ry + rh / 2.0F - ns / 2.0F - 0.5F,
            ns,
            wearing ? Theme.TEXT : ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, 0.3F + 0.7F * hover)
         );
      }
   }

   private void head(SkinLibrary.Entry e, float hx, float hy, float hs) {
      if (e == null) {
         boolean[] slim = new boolean[1];
         ResourceLocation r = SkinHooks.accountSkin(slim);
         int t = BodyProxy.texture(r);
         if (t > 0) {
            Ui.roundedImagePair(t, hx, hy, hs, hs, 2.0F, 0.125F, 0.125F, 0.25F, 0.25F, true, 0.625F, 0.125F, 0.75F, 0.25F, -1);
         }
      } else {
         if (e.faceTexture() > 0) {
            Ui.roundedImage(e.faceTexture(), hx, hy, hs, hs, 2.0F, 0.0F, 0.0F, 1.0F, 1.0F, -1);
         } else if (e.state == SkinLibrary.State.ERROR) {
            Ui.roundRect(hx, hy, hs, hs, 2.0F, -12966872);
            errorMark(hx + hs / 2.0F, hy + hs / 2.0F, hs * 0.3F);
         } else {
            Ui.roundRect(hx, hy, hs, hs, 2.0F, Theme.TRACK);
         }
      }
   }

   private int picture(SkinLibrary.Entry e, float tw, float th, boolean isOver) {
      SkinLibrary lib = this.skins.library();
      SkinPreviews pv = this.skins.previews();
      float scale = Math.max(2.0F, Ui.px);
      int pw = Math.round(tw * scale);
      int ph = Math.round(th * scale);
      boolean ready;
      int version;
      String key;
      boolean slim;
      ResourceLocation skin;
      if (e == null) {
         boolean[] s = new boolean[1];
         skin = SkinHooks.accountSkin(s);
         slim = s[0];
         key = "account|" + skin + "|" + slim;
         version = 0;
         ready = true;
      } else {
         if (e.state == SkinLibrary.State.ERROR) {
            return -1;
         }

         skin = e.location();
         slim = e.slim;
         key = e.hash.isEmpty() ? e.fileName() : e.hash;
         version = e.version;
         ready = e.state == SkinLibrary.State.READY && (pv.cardFresh(key, version, slim, pw, ph) || lib.want(e));
      }

      int live;
      return isOver
            && ClientClock.millis() - this.hoverStart > 250L
            && (e == null || e.state == SkinLibrary.State.READY && lib.want(e))
            && (live = pv.drawLive(0, key, skin, slim, Math.round(tw * Ui.px), Math.round(th * Ui.px), this.clock)) > 0
         ? live
         : pv.card(key, skin, slim, version, ready, pw, ph);
   }

   private static float tag(String text, float x, float y, float size, float h) {
      float tw = Ui.widthBold(text, size) + 8.0F;
      Ui.roundRect(x, y, tw, h, h / 2.0F, -1728053248);
      Ui.textMiddle(text, x + 4.0F, y + h / 2.0F, size, -419430401, true);
      return tw;
   }

   private static void moreButton(float cx, float cy, float mx, float my) {
      boolean over = Math.abs(mx - cx) < 7.0F && Math.abs(my - cy) < 6.0F;
      Ui.roundRect(cx - 7.0F, cy - 5.5F, 14.0F, 11.0F, 5.5F, over ? -872415232 : -1728053248);

      for (int i = -1; i <= 1; i++) {
         Ui.circle(cx + i * 3.4F, cy, 1.05F, -251658241);
      }
   }

   private static boolean onMore(float cx, float cy, float mx, float my) {
      return Math.abs(mx - cx) < 8.0F && Math.abs(my - cy) < 7.0F;
   }

   private static void errorMark(float cx, float cy, float r) {
      Ui.circle(cx, cy, r, -10868176);
      Ui.line(cx, cy - r * 0.5F, cx, cy + r * 0.12F, Math.max(1.0F, r * 0.18F), true, Theme.DANGER);
      Ui.circle(cx, cy + r * 0.45F, Math.max(0.8F, r * 0.1F), Theme.DANGER);
   }

   private void renameField(float fx, float fy, float fw, float fh, float size, int accent) {
      Ui.roundRect(fx, fy, fw, fh, 4.0F, -535555048);
      Ui.accentOutline(fx, fy, fw, fh, 4.0F, 0.9F, accent);
      String shown = tail(this.renameText, size, fw - 10.0F);
      Ui.textMiddle(shown, fx + 4.0F, fy + fh / 2.0F, size, -1, false);
      if (this.caret % 1.0F < 0.55F) {
         Ui.rect(fx + 4.0F + Ui.width(shown, size) + 1.0F, fy + 2.5F, 0.8F, fh - 5.0F, -1);
      }
   }

   private static String tail(String s, float size, float maxWidth) {
      if (Ui.width(s, size) <= maxWidth) {
         return s;
      } else {
         for (int i = 1; i < s.length(); i++) {
            String t = "…" + s.substring(i);
            if (Ui.width(t, size) <= maxWidth) {
               return t;
            }
         }

         return "…";
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         if (this.renaming != null) {
            this.commitRename();
         }

         this.filterFocused = false;
         return false;
      } else {
         SkinLibrary lib = this.skins.library();
         float fy = this.y + 1.0F;
         float fh = 18.0F;
         if (my >= fy && my < fy + fh) {
            if (mx >= this.x && mx < this.x + this.filterW()) {
               if (!this.filter.isEmpty() && mx >= this.x + this.filterW() - 16.0F) {
                  this.filter = "";
                  ModSounds.gui(Sfx.CLICK);
               } else if (!this.filterFocused) {
                  ModSounds.gui(Sfx.CLICK);
               }

               this.filterFocused = true;
               this.caret = 0.0F;
               return true;
            } else {
               this.filterFocused = false;
               if (mx >= this.sortX() && mx < this.sortX() + this.sortW()) {
                  SkinLibrary.Sort[] all = SkinLibrary.Sort.values();
                  int i = this.skins.sort.get().ordinal();
                  this.skins.sort.set(all[(i + (button == 1 ? all.length - 1 : 1)) % all.length]);
                  ModSounds.gui(Sfx.SELECT);
                  return true;
               } else if (mx >= this.favX() && mx < this.favX() + this.favW()) {
                  this.skins.favoritesFirst.set(!this.skins.favoritesFirst.get());
                  ModSounds.toggle(this.skins.favoritesFirst.get());
                  return true;
               } else {
                  return true;
               }
            }
         } else {
            this.filterFocused = false;
            if (lib.undoEntry() != null && this.undoPill.contains(mx, my)) {
               this.skins.undo();
               ModSounds.gui(Sfx.CONFIRM);
               return true;
            } else {
               float tw = this.tileW();
               float th = this.tileH();

               for (int s = 0; s < this.count(); s++) {
                  float tx = this.tileX(s);
                  float ty = this.tileY(s);
                  if (!(mx < tx) && !(mx >= tx + tw) && !(my < ty) && !(my >= ty + th)) {
                     SkinLibrary.Entry e = this.at(s);
                     if (this.renaming != null && this.renaming != e) {
                        this.commitRename();
                     }

                     if (e != null && e == this.renaming) {
                        return true;
                     }

                     boolean more = e != null
                        && (
                           Density.dense()
                              ? onMore(tx + tw - 11.0F, ty + th / 2.0F, mx, my)
                              : onMore(tx + tw - 13.0F, ty + 5.0F + (Density.compact() ? 9.5F : 11.0F) / 2.0F, mx, my)
                        );
                     if (button != 1 && !more) {
                        if (button == 0) {
                           if (e == null) {
                              if (this.skins.active()) {
                                 this.skins.wearAccount();
                                 ModSounds.gui(Sfx.SELECT);
                              }
                           } else if (e.state == SkinLibrary.State.ERROR) {
                              ModSounds.gui(Sfx.ERROR);
                           } else if (!this.skins.wearing(e)) {
                              this.skins.wear(e);
                              ModSounds.gui(Sfx.SELECT);
                           }
                        }

                        return true;
                     }

                     if (e != null) {
                        this.openMenu(e, more ? tx + tw - 20.0F : mx, more ? ty + 4.0F : my, more ? 14.0F : 1.0F, more ? 12.0F : 1.0F);
                        ModSounds.gui(Sfx.DROPDOWN);
                     }

                     return true;
                  }
               }

               if (this.renaming != null) {
                  this.commitRename();
               }

               return true;
            }
         }
      }
   }

   private void openMenu(final SkinLibrary.Entry e, float ax, float ay, float aw, float ah) {
      final SkinLibrary lib = this.skins.library();
      boolean ok = e.state != SkinLibrary.State.ERROR;
      ContextMenu m = new ContextMenu(e.name);
      m.add("Wear", null, ok && !this.skins.wearing(e), new Runnable() {
         @Override
         public void run() {
            SkinLibraryWidget.this.skins.wear(e);
         }
      });
      m.add("Rename", null, true, new Runnable() {
         @Override
         public void run() {
            SkinLibraryWidget.this.renaming = e;
            SkinLibraryWidget.this.renameText = e.name;
            SkinLibraryWidget.this.caret = 0.0F;
         }
      });
      m.add("Model: Classic", e.slim ? null : "now", ok && e.slim, new Runnable() {
         @Override
         public void run() {
            lib.setSlim(e, false);
            SkinLibraryWidget.this.skins.changed(e);
         }
      });
      m.add("Model: Slim", e.slim ? "now" : null, ok && !e.slim, new Runnable() {
         @Override
         public void run() {
            lib.setSlim(e, true);
            SkinLibraryWidget.this.skins.changed(e);
         }
      });
      m.add(e.favorite ? "Remove from favorites" : "Add to favorites", null, true, new Runnable() {
         @Override
         public void run() {
            lib.setFavorite(e, !e.favorite);
         }
      });
      m.add("Show in folder", null, true, new Runnable() {
         @Override
         public void run() {
            SkinFiles.showInFolder(new File(lib.getDir(), e.fileName()));
         }
      });
      m.add("Delete", "Undo for 5 s", true, new Runnable() {
         @Override
         public void run() {
            SkinLibraryWidget.this.skins.delete(e);
         }
      });
      Popovers.open(m, ax, ay, aw, ah);
   }

   private void commitRename() {
      SkinLibrary.Entry e = this.renaming;
      this.renaming = null;
      if (e != null && this.skins.library().rename(e, this.renameText)) {
         this.skins.changed(e);
      }
   }

   @Override
   public boolean keyTyped(char c, int key) {
      if (this.renaming != null) {
         if (key == 1) {
            this.renaming = null;
         } else if (key == 28 || key == 156) {
            this.commitRename();
         } else if (key == 14) {
            if (!this.renameText.isEmpty()) {
               this.renameText = this.renameText.substring(0, this.renameText.length() - 1);
            }
         } else if (GuiScreen.isKeyComboCtrlV(key)) {
            this.renameText = TextSetting.clean(this.renameText + GuiScreen.getClipboardString(), 32);
         } else if (GuiScreen.isKeyComboCtrlA(key)) {
            this.renameText = "";
         } else if (c >= ' ' && c != 127 && this.renameText.length() < 32) {
            this.renameText = TextSetting.clean(this.renameText + c, 32);
         }

         this.caret = 0.0F;
         ModSounds.key();
         return true;
      } else if (!this.filterFocused) {
         return false;
      } else {
         if (key == 1 || key == 28 || key == 156) {
            this.filterFocused = false;
         } else if (key == 14) {
            if (!this.filter.isEmpty()) {
               this.filter = this.filter.substring(0, this.filter.length() - 1);
            }
         } else if (GuiScreen.isKeyComboCtrlV(key)) {
            this.filter = TextSetting.clean(this.filter + GuiScreen.getClipboardString(), 32);
         } else if (GuiScreen.isKeyComboCtrlA(key)) {
            this.filter = "";
         } else if (c >= ' ' && c != 127 && this.filter.length() < 32) {
            this.filter = TextSetting.clean(this.filter + c, 32);
         }

         this.caret = 0.0F;
         ModSounds.key();
         return true;
      }
   }

   @Override
   public boolean isFocused() {
      return this.renaming != null || this.filterFocused;
   }

   @Override
   public void unfocus() {
      if (this.renaming != null) {
         this.commitRename();
      }

      this.filterFocused = false;
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else {
         float fy = this.y + 1.0F;
         float fh = 18.0F;
         if (my >= fy && my < fy + fh) {
            if (mx >= this.x && mx < this.x + this.filterW()) {
               return "Shows only the skins whose name has these letters";
            } else if (mx >= this.sortX() && mx < this.sortX() + this.sortW()) {
               return this.skins.sort.get().description() + " (click for the next order; right-click for the one before)";
            } else if (mx >= this.favX() && mx < this.favX() + this.favW()) {
               return this.skins.favoritesFirst.get()
                  ? "Favorites come first (click: in the order alone)"
                  : "Favorites in the order with the rest (click: first)";
            } else {
               return null;
            }
         } else if (this.skins.library().undoEntry() != null && this.undoPill.contains(mx, my)) {
            return "Brings the skin back (and puts it on again if you wore it)";
         } else {
            float tw = this.tileW();
            float th = this.tileH();

            for (int s = 0; s < this.count(); s++) {
               float tx = this.tileX(s);
               float ty = this.tileY(s);
               if (!(mx < tx) && !(mx >= tx + tw) && !(my < ty) && !(my >= ty + th)) {
                  SkinLibrary.Entry e = this.at(s);
                  if (e == null) {
                     return this.skins.active()
                        ? "Your account's skin: click to wear it again (Use a Custom Skin turns off)"
                        : "Your account's skin: you wear it now";
                  }

                  if (e.state == SkinLibrary.State.ERROR) {
                     return e.fileName() + ": " + e.error;
                  }

                  String model = e.slim ? "Slim" : "Classic";
                  String size = e.hd() ? ", HD " + e.size + "×" + e.size : "";
                  return e.name
                     + " ("
                     + model
                     + size
                     + "). "
                     + (this.skins.wearing(e) ? "You wear it now." : "Click to wear it.")
                     + " ⋯ or right-click for more.";
               }
            }

            return null;
         }
      }
   }

   @Override
   public void searchItems(SearchList.Items out) {
      out.item("\u0001account", "My account skin", "Your account's own skin", "account", "default", "real skin");

      for (SkinLibrary.Entry e : this.skins.library().entries()) {
         out.item(e.fileName(), e.name, "A skin of your library" + (e.hd() ? " (HD)" : ""), e.slim ? "slim" : "classic");
      }
   }

   @Override
   public void searchFilter(Set<Object> keys) {
      this.searchKeys = keys;
      this.builtVersion = -1;
   }

   @Override
   public float reveal(Object key) {
      this.build();

      for (int s = 0; s < this.count(); s++) {
         if (keyOf(this.at(s)).equals(key)) {
            return this.tileY(s) - this.y;
         }
      }

      return -1.0F;
   }

   @Override
   public float revealHeight(Object key) {
      return this.tileH();
   }

   @Override
   public int searchVersion() {
      return this.skins.library().version();
   }
}
