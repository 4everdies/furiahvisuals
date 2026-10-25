package meowtils.furiahvisuals.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import meowtils.furiahvisuals.gui.search.SearchEntry;
import meowtils.furiahvisuals.gui.search.SearchIndex;
import meowtils.furiahvisuals.gui.search.SearchIndexes;
import meowtils.furiahvisuals.gui.search.SearchQuery;
import meowtils.furiahvisuals.gui.search.SearchResult;
import meowtils.furiahvisuals.gui.widget.ScrollBar;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.render.ColorUtil;

final class GlobalSearch {
   private static final int PER_SECTION = 4;
   private final GlobalSearch.Host host;
   private final List<Module> modules;
   private final ScrollBar bar = new ScrollBar();
   private String tip;
   private String query = "";
   private final List<GlobalSearch.Found> found = new ArrayList<>();
   private int count;
   private int selected;
   private final List<Object[]> hits = new ArrayList<>();
   private float lastX;
   private float lastY;
   private float lastW;
   private float lastH;
   private Object hovered;

   GlobalSearch(GlobalSearch.Host host, List<Module> modules) {
      this.host = host;
      this.modules = SectionCategories.ordered(modules);
   }

   boolean searching() {
      return !SearchQuery.of(this.query).isEmpty();
   }

   int resultCount() {
      return this.count;
   }

   String where() {
      int sections = 0;
      boolean settings = false;

      for (GlobalSearch.Found f : this.found) {
         if (f.module != null) {
            sections++;
         } else {
            settings = true;
         }
      }

      String s = sections == 0 ? "" : sections + (sections == 1 ? " section" : " sections");
      return settings ? (s.isEmpty() ? "Settings" : s + " and Settings") : s;
   }

   void search(String text) {
      if (!text.equals(this.query)) {
         this.query = text;
         this.found.clear();
         this.count = 0;
         this.selected = 0;
         this.bar.jumpTo(0.0F);
         SearchQuery q = SearchQuery.of(text);
         if (!q.isEmpty()) {
            ArrayList<SearchResult> out = new ArrayList<>();

            for (Module m : this.modules) {
               SearchIndex index = SearchIndexes.of(m);
               index.search(q, out);
               if (!out.isEmpty()) {
                  GlobalSearch.Found f = new GlobalSearch.Found(m.getId(), m.getName(), m);
                  f.total = out.size();
                  f.results.addAll(out.subList(0, Math.min(4, out.size())));
                  this.found.add(f);
                  this.count = this.count + out.size();
               }
            }

            SearchIndex settings = this.host.settingsIndex();
            settings.search(q, out);
            if (!out.isEmpty()) {
               GlobalSearch.Found f = new GlobalSearch.Found(settings.page, settings.title, null);
               f.total = out.size();
               f.results.addAll(out.subList(0, Math.min(4, out.size())));
               this.found.add(f);
               this.count = this.count + out.size();
            }
         }
      }
   }

   void render(float x, float y, float w, float h, float mx, float my, float dt, int accent) {
      this.lastX = x;
      this.lastY = y;
      this.lastW = w;
      this.lastH = h;
      this.tip = null;
      this.hits.clear();
      boolean in = mx >= x - 10.0F && mx < x + w + 10.0F && my >= y && my < y + h;
      float hmx = in ? mx : -10000.0F;
      float hmy = in ? my : -10000.0F;
      Ui.pushScissor(x - 10.0F, y, w + 20.0F, h);
      float top = y - this.bar.scroll;
      float bottom = this.searching() ? this.drawResults(x, top, w, hmx, hmy, accent) : this.drawEmpty(x, top, w);
      Ui.popScissor();
      this.bar.update(bottom - top + 8.0F, h, dt);
      this.bar.draw(x + w + 8.0F, y, h, mx, my, dt, accent);
      this.hovered = null;

      for (Object[] hit : this.hits) {
         float hx = (Float)hit[2];
         float hy = (Float)hit[3];
         float hw = (Float)hit[4];
         float hh = (Float)hit[5];
         if (hmx >= hx && hmx < hx + hw && hmy >= hy && hmy < hy + hh) {
            this.hovered = hit.length > 6 ? hit[6] : hit[1];
            break;
         }
      }
   }

   Object hoverTarget() {
      return this.hovered;
   }

   ScrollBar bar() {
      return this.bar;
   }

   private float drawEmpty(float x, float y, float w) {
      float ts = Density.text();
      Ui.textMiddle(Ui.ellipsize("Type to search every section and Settings at once.", ts, w, false), x, y + 14.0F, ts, Theme.TEXT_DIM, false);
      Ui.textMiddle(
         Ui.ellipsize("Pick a match to open its section with the setting highlighted (↑/↓ and Enter work too).", Density.small(), w, false),
         x,
         y + 30.0F,
         Density.small(),
         Theme.TEXT_MUTED,
         false
      );
      return y + 44.0F;
   }

   private float drawResults(float x, float y, float w, float mx, float my, int accent) {
      if (this.found.isEmpty()) {
         Ui.textMiddle(
            "No setting in any section matches “" + Ui.ellipsize(this.query.trim(), 9.0F, w * 0.5F, false) + "”", x, y + 14.0F, 9.0F, Theme.TEXT_DIM, false
         );
         return y + 30.0F;
      } else {
         float rh = Density.compact() ? Density.row() + 2.0F : 24.0F;
         float ts = Density.text();
         float small = Density.small();
         int index = 0;

         for (GlobalSearch.Found f : this.found) {
            float hy = y + Density.header() / 2.0F;
            if (f.module != null) {
               MenuIcons.section(f.module.getId(), x + 6.0F, hy, 10.0F, accent);
            } else {
               FuriahScreen.gear(x + 6.0F, hy, 3.6F, accent, 0.0F);
            }

            String head = f.title.toUpperCase(Locale.ROOT);
            float hs = Density.headerText();
            Ui.textMiddle(head, x + 16.0F, hy, hs, Theme.TEXT_DIM, true);
            String n = f.total + (f.total == 1 ? " match" : " matches");
            float nx = x + 16.0F + Ui.widthBold(head, hs) + 8.0F;
            Ui.textMiddle(n, nx, hy, small, Theme.TEXT_MUTED, false);
            float lx = nx + Ui.width(n, small) + 8.0F;
            Ui.rect(lx, hy, Math.max(0.0F, x + w - lx), 0.6F, Theme.DIVIDER);
            y += Density.header() + 1.0F;

            for (SearchResult r : f.results) {
               SearchEntry e = r.entry;
               boolean over = mx >= x - 6.0F && mx < x + w + 6.0F && my >= y && my < y + rh;
               if (over) {
                  this.selected = index;
               }

               boolean sel = index == this.selected;
               if (sel) {
                  Ui.roundRect(x - 6.0F, y + 1.0F, w + 12.0F, rh - 2.0F, 6.0F, over ? Theme.ACCENT_SOFT : Theme.ACCENT_FAINT);
                  Ui.roundRect(x - 6.0F, y + 5.0F, 2.2F, rh - 10.0F, 1.1F, accent);
               }

               float cy = y + rh / 2.0F;
               String label = Ui.ellipsize(e.label, ts, w * 0.45F, true);
               Ui.textMiddle(label, x + 4.0F, cy, ts, Theme.TEXT, true);
               String path = e.group != null && !e.group.isEmpty() ? e.group : "";
               if (e.owner != null && !e.owner.isShown() && !e.owner.isMultiple()) {
                  path = e.owner.styleName() + (path.isEmpty() ? "" : " › " + path);
               }

               float px = x + 4.0F + Ui.widthBold(label, ts) + 10.0F;
               Ui.textMiddle(Ui.ellipsize(path, small, x + w - 16.0F - px, false), px, cy, small, Theme.TEXT_MUTED, false);
               MenuIcons.jump(x + w - 6.0F, cy, ColorUtil.lerp(Theme.TEXT_MUTED, accent, sel ? 1.0F : 0.3F));
               this.hits.add(new Object[]{"result", f, x - 6.0F, y, w + 12.0F, rh, r});
               if (over && this.tip == null) {
                  this.tip = "Open " + f.title + " and show it there";
               }

               y += rh;
               index++;
            }

            if (f.total > f.results.size()) {
               String more = "+" + (f.total - f.results.size()) + " more in " + f.title;
               float my2 = y + rh / 2.0F;
               boolean overx = mx >= x && mx < x + Ui.width(more, small) + 8.0F && my >= y && my < y + rh;
               Ui.textMiddle(more, x + 4.0F, my2, small, ColorUtil.lerp(Theme.TEXT_MUTED, accent, overx ? 1.0F : 0.3F), false);
               this.hits.add(new Object[]{"more", f, x, y, Ui.width(more, small) + 8.0F, rh});
               y += rh;
            }

            y += 6.0F;
         }

         return y;
      }
   }

   boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && mx >= this.lastX - 10.0F && mx < this.lastX + this.lastW + 10.0F && my >= this.lastY && my < this.lastY + this.lastH) {
         for (Object[] h : this.hits) {
            float hx = (Float)h[2];
            float hy = (Float)h[3];
            float hw = (Float)h[4];
            float hh = (Float)h[5];
            if (!(mx < hx) && !(mx >= hx + hw) && !(my < hy) && !(my >= hy + hh)) {
               GlobalSearch.Found f = (GlobalSearch.Found)h[1];
               this.host.reveal(f.page, this.query, "result".equals(h[0]) ? (SearchResult)h[6] : null);
               return true;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   void wheel(int wheel) {
      this.bar.wheel(wheel);
   }

   boolean key(int keyCode) {
      if (!this.searching()) {
         return false;
      } else {
         int total = 0;

         for (GlobalSearch.Found f : this.found) {
            total += f.results.size();
         }

         if (keyCode != 208 && keyCode != 200) {
            if (keyCode != 28 && keyCode != 156) {
               return false;
            } else {
               int i = 0;

               for (GlobalSearch.Found f : this.found) {
                  for (SearchResult r : f.results) {
                     if (i++ == this.selected) {
                        this.host.reveal(f.page, this.query, r);
                        return true;
                     }
                  }
               }

               return true;
            }
         } else {
            if (total > 0) {
               this.selected = Math.max(0, Math.min(total - 1, this.selected + (keyCode == 208 ? 1 : -1)));
            }

            return true;
         }
      }
   }

   String tooltip() {
      return this.tip;
   }

   List<String> describeForTest() {
      ArrayList<String> out = new ArrayList<>();

      for (GlobalSearch.Found f : this.found) {
         StringBuilder sb = new StringBuilder(f.title).append(" ").append(f.total).append(":");

         for (SearchResult r : f.results) {
            sb.append(" ").append(r.entry.label).append(";");
         }

         out.add(sb.toString());
      }

      return out;
   }

   float[] resultForTest(String page) {
      for (Object[] h : this.hits) {
         if ("result".equals(h[0]) && ((GlobalSearch.Found)h[1]).page.equals(page)) {
            return new float[]{(Float)h[2] + (Float)h[4] / 2.0F, (Float)h[3] + (Float)h[5] / 2.0F};
         }
      }

      return null;
   }

   private static final class Found {
      final String page;
      final String title;
      final Module module;
      final List<SearchResult> results = new ArrayList<>();
      int total;

      Found(String page, String title, Module module) {
         this.page = page;
         this.title = title;
         this.module = module;
      }
   }

   interface Host {
      void reveal(String var1, String var2, SearchResult var3);

      SearchIndex settingsIndex();
   }
}
