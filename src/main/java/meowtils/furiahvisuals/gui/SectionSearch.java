package meowtils.furiahvisuals.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.layout.LayoutEntry;
import meowtils.furiahvisuals.gui.layout.SectionLayouts;
import meowtils.furiahvisuals.gui.search.SearchEntry;
import meowtils.furiahvisuals.gui.search.SearchIndex;
import meowtils.furiahvisuals.gui.search.SearchList;
import meowtils.furiahvisuals.gui.search.SearchQuery;
import meowtils.furiahvisuals.gui.search.SearchResult;
import meowtils.furiahvisuals.gui.widget.ScrollBar;
import meowtils.furiahvisuals.gui.widget.Widget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;

final class SectionSearch {
   static final float JUMP_W = 20.0F;
   private static final String HIDDEN_NOTE = "Shown in the section once the option it belongs to is on";
   private static final float OFFSCREEN = -10000.0F;
   private static final int MAX_OTHERS = 5;
   private static final long OTHERS_DELAY_NANOS = 110000000L;
   private final SectionSearch.Host host;
   private final String page;
   private final String pageTitle;
   private SearchIndex index;
   private final Map<Setting, Widget> widgets;
   private final List<SearchResult> results = new ArrayList<>();
   private final List<SectionSearch.Row> rows = new ArrayList<>();
   private final List<SectionSearch.Other> others = new ArrayList<>();
   private int otherCount;
   private long othersDue;
   private boolean othersOpen;
   private String didYouMean;
   private float didHover;
   private String text = "";
   private SearchQuery query = SearchQuery.of("");
   private int stateSignature;
   final ScrollBar bar = new ScrollBar();
   private Object selectedKey;
   private Object pulseKey;
   private long pulseStart;
   Widget pressed;
   private float lastSearchMicros;
   private float lastX;
   private float lastW;
   private float viewTop;

   private static float titleH() {
      return Density.header();
   }

   private static float linkH() {
      return Density.compact() ? Density.row() + 2.0F : 22.0F;
   }

   private static float snippetH() {
      return Density.compact() ? 11.0F : 13.0F;
   }

   SectionSearch(SectionSearch.Host host, String page, String pageTitle, SearchIndex index, Map<Setting, Widget> widgets) {
      this.host = host;
      this.page = page;
      this.pageTitle = pageTitle;
      this.index = index;
      this.widgets = widgets;
   }

   String text() {
      return this.text;
   }

   boolean isActive() {
      return !this.query.isEmpty();
   }

   int resultCount() {
      return this.results.size();
   }

   List<SearchResult> results() {
      return this.results;
   }

   boolean revealTab(String tab) {
      for (SectionSearch.Row r : this.rows) {
         if (r.kind == 0 && !r.leaving && r.title != null && (r.title.equals(tab) || r.title.startsWith(tab + " "))) {
            this.bar.target = Math.max(0.0F, Math.min(this.bar.max(), r.y - this.viewTop + this.bar.scroll));
            return true;
         }
      }

      return false;
   }

   float lastSearchMicrosForTest() {
      return this.lastSearchMicros;
   }

   int otherCountForTest() {
      return this.otherCount;
   }

   List<SectionSearch.Other> othersForTest() {
      return this.others;
   }

   String didYouMeanForTest() {
      return this.didYouMean;
   }

   List<String> rowsForTest() {
      ArrayList<String> out = new ArrayList<>();

      for (SectionSearch.Row r : this.rows) {
         if (!r.leaving) {
            switch (r.kind) {
               case 0:
                  out.add("#" + r.title);
                  break;
               case 1:
                  out.add("@" + r.title);
                  break;
               case 2:
                  out.add(r.result.entry.label + (r.filter != null ? " " + r.filter.size() + " shown" : ""));
                  break;
               case 3:
               default:
                  out.add("!empty");
                  break;
               case 4:
                  out.add(">" + r.other.index.title + " › " + r.other.result.entry.path());
                  break;
               case 5:
                  out.add("+more");
            }
         }
      }

      return out;
   }

   void update(String newText, SearchIndex newIndex) {
      boolean changed = !newText.equals(this.text) || newIndex != this.index;
      if (changed) {
         boolean sameQuery = newIndex != this.index && newText.equals(this.text);
         boolean hadResults = this.query != null && !this.query.isEmpty() && !this.results.isEmpty();
         this.text = newText;
         this.index = newIndex;
         this.query = SearchQuery.of(this.text);
         long t0 = System.nanoTime();
         this.index.search(this.query, this.results);
         this.lastSearchMicros = (float)(System.nanoTime() - t0) / 1000.0F;
         if (!sameQuery && hadResults && !this.query.isEmpty() && this.results.isEmpty()) {
            ModSounds.noResults();
         }

         String string = this.didYouMean = this.results.isEmpty() ? this.index.didYouMean(this.query) : null;
         if (this.didYouMean != null && SearchQuery.of(this.didYouMean).isEmpty()) {
            this.didYouMean = null;
         }

         if (!sameQuery) {
            this.bar.jumpTo(0.0F);
            this.selectedKey = null;
            this.othersOpen = false;
         }

         this.others.clear();
         this.otherCount = 0;
         this.othersDue = this.query.isEmpty() ? 0L : System.nanoTime() + 110000000L;
         this.regroup();
      } else if (this.othersDue != 0L && System.nanoTime() >= this.othersDue) {
         this.othersDue = 0L;
         this.searchOthers();
         this.regroup();
      } else if (!this.query.isEmpty() && this.signature() != this.stateSignature) {
         this.regroup();
      }
   }

   private int signature() {
      int s = 1;

      for (int i = 0; i < this.results.size(); i++) {
         SettingStyle o = this.results.get(i).entry.owner;
         if (o != null) {
            s = s * 31 + (o.isShown() ? 1 : 0) + (o.isActive() ? 2 : 0);
         }
      }

      return s;
   }

   private void searchOthers() {
      this.others.clear();
      this.otherCount = 0;
      ArrayList<SearchResult> found = new ArrayList<>();
      ArrayList<SectionSearch.Other> all = new ArrayList<>();

      for (SearchIndex other : this.host.otherIndexes()) {
         other.search(this.query, found);

         for (SearchResult r : found) {
            SettingStyle o = r.entry.owner;
            if (o == null
               || o.isShown()
               || !o.foundElsewhereByNameOnly()
               || r.entry.row != null && r.entry.row.isFoundAnywhere()
               || r.groupStart >= 0 && r.groupStart < o.styleName().length()) {
               this.otherCount++;
               all.add(new SectionSearch.Other(other, r));
            }
         }
      }

      Collections.sort(all, new Comparator<SectionSearch.Other>() {
         public int compare(SectionSearch.Other a, SectionSearch.Other b) {
            return a.result.tier - b.result.tier;
         }
      });

      for (int i = 0; i < all.size() && i < 5; i++) {
         this.others.add(all.get(i));
      }
   }

   private void regroup() {
      this.stateSignature = this.signature();
      HashMap<Object, SectionSearch.Row> old = new HashMap<>();

      for (SectionSearch.Row r : this.rows) {
         if (!r.leaving) {
            old.put(r.key, r);
         }
      }

      ArrayList<SectionSearch.Row> fresh = new ArrayList<>();
      IdentityHashMap<Widget, SectionSearch.Row> byWidget = new IdentityHashMap<>();
      LinkedHashMap<String, List<SectionSearch.Row>> groups = new LinkedHashMap<>();
      LinkedHashMap<SettingStyle, List<SectionSearch.Row>> otherStyles = new LinkedHashMap<>();
      HashMap<String, int[]> groupRanges = new HashMap<>();
      if (this.widgets != null) {
         for (SearchResult searchResult : this.results) {
            SearchEntry e = searchResult.entry;
            Widget w = e.row == null ? null : this.widgets.get(e.row);
            if (w != null) {
               SectionSearch.Row row = byWidget.get(w);
               if (row == null) {
                  row = new SectionSearch.Row(2, w);
                  row.widget = w;
                  row.result = searchResult;
                  byWidget.put(w, row);
                  SettingStyle owner = e.owner;
                  if (owner != null && !owner.isMultiple() && !owner.isShown()) {
                     listOf(otherStyles, owner).add(row);
                  } else {
                     String g = SectionLayouts.groupOf(e.row);
                     if (g == null) {
                        g = e.row.getGroup() == null ? "" : e.row.getGroup();
                     }

                     if (owner != null && owner.isMultiple() && e.group != null) {
                        g = e.group;
                     }

                     String title = owner != null && owner.isMultiple() && !owner.isActive() ? g + " (off)" : g;
                     listOf(groups, title).add(row);
                     if (searchResult.groupStart >= 0 && g.equals(e.group) && !groupRanges.containsKey(title)) {
                        groupRanges.put(title, new int[]{searchResult.groupStart, searchResult.groupEnd});
                     }
                  }

                  if (e.kind == SearchEntry.Kind.SETTING) {
                     if (searchResult.snippet != null) {
                        row.snippet = searchResult.snippet;
                        row.snipA = searchResult.snippetStart;
                        row.snipB = searchResult.snippetEnd;
                     } else if (searchResult.choice != null) {
                        row.snippet = "Choice: " + searchResult.choice;
                        row.snipA = 8 + searchResult.choiceStart;
                        row.snipB = 8 + searchResult.choiceEnd;
                     } else if (!e.row.isVisibleIgnoringStyle()) {
                        LayoutEntry le = SectionLayouts.entryOf(e.row);
                        row.snippet = le != null && le.parent != null
                           ? "Shown once “" + le.parent.getLabel() + "” is on"
                           : "Shown in the section once the option it belongs to is on";
                        row.snipB = 0;
                        row.snipA = 0;
                     } else {
                        LayoutEntry le = SectionLayouts.entryOf(e.row);
                        if (le != null && le.parent != null) {
                           row.snippet = "Under “" + le.parent.getLabel() + "”";
                           row.snipB = 0;
                           row.snipA = 0;
                        }
                     }
                  }
               }

               row.all.add(searchResult);
               if (e.kind == SearchEntry.Kind.SETTING) {
                  row.whole = true;
               } else if (e.key != null) {
                  if (row.filter == null) {
                     row.filter = new HashSet<>();
                  }

                  row.filter.add(e.key);
               }
            }
         }
      }

      for (Entry<String, List<SectionSearch.Row>> entry : groups.entrySet()) {
         if (!((String)entry.getKey()).isEmpty()) {
            SectionSearch.Row t = this.title((String)entry.getKey(), old);
            int[] range = groupRanges.get(entry.getKey());
            t.titleRanges = range;
            fresh.add(t);
         }

         fresh.addAll((Collection<? extends SectionSearch.Row>)entry.getValue());
      }

      if (!otherStyles.isEmpty()) {
         fresh.add(this.title("Other " + ((SettingStyle)otherStyles.keySet().iterator().next()).kinds(), old));

         for (Entry<SettingStyle, List<SectionSearch.Row>> entry : otherStyles.entrySet()) {
            SectionSearch.Row h = carry(new SectionSearch.Row(1, entry.getKey()), old);
            h.style = (SettingStyle)entry.getKey();
            h.title = ((SettingStyle)entry.getKey()).styleName();
            fresh.add(h);
            fresh.addAll((Collection<? extends SectionSearch.Row>)entry.getValue());
         }
      }

      if (!this.query.isEmpty()) {
         if (this.results.isEmpty()) {
            fresh.add(carry(new SectionSearch.Row(3, "empty"), old));
            if (!this.others.isEmpty()) {
               fresh.add(this.title("Found in other sections", old));

               for (SectionSearch.Other other : this.others) {
                  fresh.add(this.otherRow(other, old));
               }
            }
         } else if (this.otherCount > 0) {
            fresh.add(carry(new SectionSearch.Row(5, "more"), old));
            if (this.othersOpen) {
               for (SectionSearch.Other other : this.others) {
                  fresh.add(this.otherRow(other, old));
               }
            }
         }
      }

      for (SectionSearch.Row rowx : fresh) {
         if (rowx.kind == 2) {
            SectionSearch.Row prev = old.get(rowx.key);
            if (prev != null) {
               rowx.appear = prev.appear;
               rowx.hover = prev.hover;
               rowx.jump = prev.jump;
            } else {
               rowx.appear = 0.0F;
            }
         }
      }

      HashSet<Object> kept = new HashSet<>();

      for (SectionSearch.Row rx : fresh) {
         kept.add(rx.key);
      }

      for (int i = 0; i < this.rows.size(); i++) {
         SectionSearch.Row rx = this.rows.get(i);
         if (!kept.contains(rx.key) && rx.appear > 0.01F) {
            rx.leaving = true;
            fresh.add(Math.min(i, fresh.size()), rx);
         }
      }

      this.rows.clear();
      this.rows.addAll(fresh);
      this.applyFilters();
      if (this.selectedKey == null || !this.has(this.selectedKey)) {
         this.selectedKey = null;

         for (SectionSearch.Row rx : this.rows) {
            if (rx.selectable()) {
               this.selectedKey = rx.key;
               break;
            }
         }
      }
   }

   private static <K> List<SectionSearch.Row> listOf(Map<K, List<SectionSearch.Row>> map, K key) {
      List<SectionSearch.Row> list = map.get(key);
      if (list == null) {
         list = new ArrayList<>();
         map.put(key, list);
      }

      return list;
   }

   private SectionSearch.Row title(String text, Map<Object, SectionSearch.Row> old) {
      SectionSearch.Row t = carry(new SectionSearch.Row(0, "t:" + text), old);
      t.title = text;
      return t;
   }

   private SectionSearch.Row otherRow(SectionSearch.Other o, Map<Object, SectionSearch.Row> old) {
      SectionSearch.Row r = carry(new SectionSearch.Row(4, "o:" + o.index.page + "#" + o.result.entry.order), old);
      r.other = o;
      return r;
   }

   private static SectionSearch.Row carry(SectionSearch.Row r, Map<Object, SectionSearch.Row> old) {
      SectionSearch.Row prev = old.get(r.key);
      float f = r.appear = prev != null ? prev.appear : 0.0F;
      if (prev != null) {
         r.hover = prev.hover;
         r.use = prev.use;
      }

      return r;
   }

   private boolean has(Object key) {
      for (SectionSearch.Row r : this.rows) {
         if (r.key.equals(key) && r.selectable()) {
            return true;
         }
      }

      return false;
   }

   private void applyFilters() {
      for (SectionSearch.Row r : this.rows) {
         if (r.kind == 2 && !r.leaving && r.widget instanceof SearchList) {
            ((SearchList)r.widget).searchFilter(r.whole ? null : r.filter);
         }
      }
   }

   void clearFilters() {
      if (this.widgets != null) {
         for (Widget w : this.widgets.values()) {
            if (w instanceof SearchList) {
               ((SearchList)w).searchFilter(null);
            }
         }
      }
   }

   void drawPage(float x, float y, float w, float h, float mx, float my, boolean mouseFree, Widget active, float dt, int accent) {
      boolean mouseIn = mouseFree && mx >= x - 8.0F && mx < x + w + 8.0F && my >= y && my < y + h;
      Ui.pushScissor(x - 10.0F, y, w + 20.0F, h);
      float content = this.drawRows(
         x, y - this.bar.scroll, w, y, y + h, !mouseIn && active == null ? -10000.0F : mx, !mouseIn && active == null ? -10000.0F : my, active, dt, accent
      );
      Ui.popScissor();
      this.bar.update(content + 10.0F, h, dt);
      this.bar.draw(x + w + 8.0F, y, h, active == null ? mx : -10000.0F, my, dt, accent);
   }

   float drawRows(float x, float top, float w, float clipTop, float clipBottom, float mx, float my, Widget active, float dt, int accent) {
      this.lastX = x;
      this.lastW = w;
      this.viewTop = clipTop;
      float y = top;
      boolean blocked = this.host.blocked();
      long now = ClientClock.nanos();

      for (int i = 0; i < this.rows.size(); i++) {
         SectionSearch.Row r = this.rows.get(i);
         float target = r.leaving ? 0.0F : 1.0F;
         float f = r.appear = r.appear < 0.0F ? target : Easing.approach(r.appear, target, 14.0F, dt);
         if (r.leaving && r.appear <= 0.003F) {
            this.rows.remove(i--);
         } else {
            float full = this.height(r, w);
            float h = full * Easing.outCubic(Easing.clamp01(r.appear));
            r.y = y;
            r.h = h;
            if (h > 0.5F && y + h > clipTop - 2.0F && y < clipBottom + 2.0F) {
               Ui.pushScissor(x - 10.0F, Math.max(y, clipTop), w + 20.0F, Math.min(y + h, clipBottom) - Math.max(y, clipTop));
               Ui.pushAlpha(r.appear * r.appear);
               this.drawRow(r, x + (1.0F - r.appear) * 10.0F, y, w, mx, my, active, blocked, dt, accent, now);
               Ui.popAlpha();
               Ui.popScissor();
            }

            y += h;
         }
      }

      return y - top;
   }

   private float height(SectionSearch.Row r, float w) {
      switch (r.kind) {
         case 0:
         case 1:
            return titleH();
         case 2:
            r.widget.w = w - 20.0F;
            return r.widget.height() + (r.snippet != null ? snippetH() : 0.0F) + 4.0F;
         case 3:
            return this.didYouMean != null ? 48.0F : 32.0F;
         default:
            return linkH();
      }
   }

   private void drawRow(SectionSearch.Row r, float x, float y, float w, float mx, float my, Widget active, boolean blocked, float dt, int accent, long now) {
      switch (r.kind) {
         case 0:
            this.drawTitle(r, x, y, w, accent);
            return;
         case 1:
            this.drawStyle(r, x, y, w, mx, my, dt, accent);
            return;
         case 2:
         default:
            boolean selected = r.key.equals(this.selectedKey);
            boolean pulsing = r.key.equals(this.pulseKey) && now - this.pulseStart < 1500000000L;
            float wh = r.widget.height();
            if (selected || pulsing) {
               float a = selected ? 0.1F : 0.0F;
               if (pulsing) {
                  float t = (float)(now - this.pulseStart) / 1.0E9F;
                  a = Math.max(a, (0.14F + 0.12F * (float)Math.sin(t * 9.0)) * (1.0F - Easing.smoothstep(0.9F, 1.5F, t)));
               }

               if (a > 0.001F) {
                  int fill = pulsing ? ColorUtil.withAlpha(Theme.ACCENT_SOFT_HOVER, Math.min(1.0F, a / 0.26F)) : Theme.ACCENT_FAINT;
                  Ui.roundRect(x - 6.0F, y, w + 12.0F, wh + (r.snippet != null ? snippetH() : 0.0F) + 2.0F, 8.0F, fill);
               }

               Ui.roundRect(x - 6.0F, y + 5.0F, 2.2F, Math.max(4.0F, wh - 10.0F), 1.1F, selected ? Theme.ACCENT : Theme.ACCENT_LINE);
            }

            Widget w0 = r.widget;
            w0.x = x;
            w0.y = y;
            w0.w = w - 20.0F;
            w0.appear = 1.0F;
            boolean disabled = blocked || unavailable(r) != null;
            if (w0 == active) {
               Ui.pushAlpha(1.0F);
            } else {
               Ui.pushAlpha(disabled ? 0.4F : 1.0F);
            }

            w0.render(disabled ? -10000.0F : mx, disabled ? -10000.0F : my, dt, accent);
            Ui.popAlpha();
            if (r.snippet != null) {
               drawSnippet(r.snippet, r.snipA, r.snipB, x, y + wh + 1.0F, w - 20.0F, accent);
            }

            float jx = x + w - 10.0F;
            float jy = y + Math.min(14.0F, wh / 2.0F);
            boolean overJump = Math.abs(mx - jx) <= 8.0F && Math.abs(my - jy) <= 8.0F;
            r.jump = Easing.approach(r.jump, overJump ? 1.0F : 0.0F, 18.0F, dt);
            boolean overRow = mx >= x - 6.0F && mx < x + w + 6.0F && my >= y && my < y + wh;
            r.hover = Easing.approach(r.hover, overRow ? 1.0F : 0.0F, 16.0F, dt);
            float strength = Math.max(0.35F, Math.max(r.hover, Math.max(selected ? 1.0F : 0.0F, r.jump)));
            if (r.jump > 0.01F) {
               Ui.circle(jx, jy, 8.0F, ColorUtil.withAlpha(accent, 0.18F * r.jump));
            }

            arrow(jx, jy, ColorUtil.withAlpha(ColorUtil.lerp(Theme.TEXT_MUTED, accent, Math.max(r.jump, selected ? 0.6F : 0.0F)), strength));
            return;
         case 3:
            this.drawEmpty(x, y, w, mx, my, dt, accent);
            return;
         case 4:
            this.drawOther(r, x, y, w, mx, my, dt, accent);
            return;
         case 5:
            this.drawMore(r, x, y, w, mx, my, dt, accent);
      }
   }

   private static void arrow(float cx, float cy, int color) {
      for (int i = 0; i <= 6; i++) {
         float t = i / 6.0F;
         Ui.circle(cx - 3.2F + t * 6.4F, cy + 3.2F - t * 6.4F, 0.7F, color);
      }

      for (int var6 = 1; var6 <= 4; var6++) {
         float t = var6 * 0.9F;
         Ui.circle(cx + 3.2F - t, cy - 3.2F, 0.7F, color);
         Ui.circle(cx + 3.2F, cy - 3.2F + t, 0.7F, color);
      }
   }

   private static void drawSnippet(String text, int a, int b, float x, float y, float w, int accent) {
      float size = Density.compact() ? 7.2F : 7.8F;
      Ui.text(Ui.ellipsize(text, size, w, false), x, y, size, Theme.TEXT_MUTED);
   }

   private void drawTitle(SectionSearch.Row r, float x, float y, float w, int accent) {
      float ty = y + titleH() / 2.0F - 2.0F;
      float ts = Density.headerText();
      Ui.glow(x + 3.0F, ty + 0.5F, 14.0F, ColorUtil.withAlpha(accent, 0.35F));
      Ui.circle(x + 3.0F, ty + 0.5F, 2.2F * ts / 8.2F, accent | 0xFF000000);
      String title = r.title.toUpperCase(Locale.ROOT);
      Ui.textMiddle(title, x + 11.0F, ty, ts, Theme.TEXT_DIM, true);
      float tw = Ui.widthBold(title, ts);
      Ui.rect(x + 17.0F + tw, ty, Math.max(0.0F, w - 17.0F - tw), 0.6F, Theme.DIVIDER);
   }

   private static String use(SectionSearch.Row r) {
      return "Use this " + (r.style != null ? r.style.kind() : "style");
   }

   private float useX(SectionSearch.Row r, float x, float w) {
      return x + w - Ui.width(use(r), 8.0F) - 18.0F;
   }

   private void drawStyle(SectionSearch.Row r, float x, float y, float w, float mx, float my, float dt, int accent) {
      float ty = y + titleH() / 2.0F;
      String title = r.title;
      Ui.rect(x + 2.0F, ty, 5.0F, 0.8F, Theme.ACCENT_LINE);
      Ui.textMiddle(title, x + 12.0F, ty, 9.2F, Theme.TEXT, true);
      float ux = this.useX(r, x, w);
      float uw = x + w - ux;
      float uh = Math.min(17.0F, titleH() - 2.0F);
      float uy = ty - uh / 2.0F;
      boolean over = mx >= ux && mx < ux + uw && my >= uy && my < uy + uh;
      r.use = Easing.approach(r.use, over ? 1.0F : 0.0F, 16.0F, dt);
      Ui.registerControl(ux, uy, uw, uh, "use style " + title);
      Ui.roundRect(ux, uy, uw, uh, uh / 2.0F, ColorUtil.lerp(Theme.ACCENT_SOFT, Theme.ACCENT_SOFT_HOVER, r.use));
      Ui.accentOutline(ux, uy, uw, uh, uh / 2.0F, 1.0F, r.use > 0.5F ? Theme.ACCENT_HOVER : accent);
      Ui.textMiddle(use(r), ux + 9.0F, ty, 8.0F, ColorUtil.lerp(Theme.TEXT_DIM, Theme.TEXT, 0.4F + 0.6F * r.use), false);
      float tw = Ui.widthBold(title, 9.2F);
      Ui.rect(x + 18.0F + tw, ty, Math.max(0.0F, ux - 8.0F - (x + 18.0F + tw)), 0.6F, Theme.DIVIDER);
   }

   private void drawEmpty(float x, float y, float w, float mx, float my, float dt, int accent) {
      String q = Ui.ellipsize(this.text.trim(), 9.4F, w * 0.6F, false);
      Ui.textMiddle("No options match “" + q + "”", x, y + 12.0F, 9.4F, Theme.TEXT, false);
      if (this.didYouMean != null) {
         String lead = "Did you mean ";
         float lw = Ui.width(lead, 8.6F);
         float dx = x + lw;
         float dy = y + 31.0F;
         float dw = Ui.widthHeavy(this.didYouMean, 8.6F);
         boolean over = mx >= dx - 2.0F && mx < dx + dw + 2.0F && my >= dy - 8.0F && my < dy + 8.0F;
         this.didHover = Easing.approach(this.didHover, over ? 1.0F : 0.0F, 16.0F, dt);
         Ui.textMiddle(lead, x, dy, 8.6F, Theme.TEXT_DIM, false);
         Ui.textMiddleHeavy(this.didYouMean, dx, dy, 8.6F, accent | 0xFF000000);
         Ui.rect(dx, dy + 5.5F, dw, 0.7F, ColorUtil.lerp(Theme.ACCENT_LINE, Theme.ACCENT, this.didHover));
         Ui.textMiddle("?", dx + dw + 1.0F, dy, 8.6F, Theme.TEXT_DIM, false);
      }
   }

   private void drawMore(SectionSearch.Row r, float x, float y, float w, float mx, float my, float dt, int accent) {
      boolean over = mx >= x - 6.0F && mx < x + w + 6.0F && my >= y && my < y + linkH();
      r.hover = Easing.approach(r.hover, over ? 1.0F : 0.0F, 16.0F, dt);
      if (r.hover > 0.01F) {
         Ui.roundRect(x - 6.0F, y + 1.0F, w + 12.0F, linkH() - 2.0F, 7.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, r.hover));
      }

      String t = "Also in other sections (" + this.otherCount + ")";
      Ui.textMiddle(t, x, y + linkH() / 2.0F, 8.2F, ColorUtil.lerp(Theme.TEXT_MUTED, accent, 0.3F + 0.5F * r.hover), false);
      float cx = x + Ui.width(t, 8.2F) + 8.0F;
      float cy = y + linkH() / 2.0F;

      for (int i = -2; i <= 2; i++) {
         float k = Math.abs(i) / 2.0F;
         float px = this.othersOpen ? cx + i * 1.1F : cx + (1.0F - k) * 2.0F - 1.0F;
         float py = this.othersOpen ? cy + (1.0F - k) * 2.0F - 1.0F : cy + i * 1.1F;
         Ui.circle(px, py, 0.65F, Theme.TEXT_MUTED);
      }
   }

   private void drawOther(SectionSearch.Row r, float x, float y, float w, float mx, float my, float dt, int accent) {
      boolean selected = r.key.equals(this.selectedKey);
      boolean over = mx >= x - 6.0F && mx < x + w + 6.0F && my >= y && my < y + linkH();
      r.hover = Easing.approach(r.hover, over ? 1.0F : 0.0F, 16.0F, dt);
      float bg = Math.max(r.hover, selected ? 0.8F : 0.0F);
      if (bg > 0.01F) {
         Ui.roundRect(x - 6.0F, y + 1.0F, w + 12.0F, linkH() - 2.0F, 7.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, bg));
      }

      if (selected) {
         Ui.roundRect(x - 6.0F, y + 6.0F, 2.2F, linkH() - 12.0F, 1.1F, accent);
      }

      SearchEntry e = r.other.result.entry;
      String section = r.other.index.title;
      float cy = y + linkH() / 2.0F;
      Ui.textMiddle(section, x + 4.0F, cy, 8.6F, Theme.TEXT_DIM, true);
      float sx = x + 4.0F + Ui.widthBold(section, 8.6F);
      String path = " › " + e.path();
      Ui.textMiddle(Ui.ellipsize(path, 8.6F, w - (sx - x) - 20.0F, false), sx, cy, 8.6F, Theme.TEXT, false);
      arrow(x + w - 10.0F, cy, ColorUtil.withAlpha(ColorUtil.lerp(Theme.TEXT_MUTED, accent, bg), 0.45F + 0.55F * bg));
   }

   boolean mouseDown(float mx, float my, int button) {
      this.pressed = null;
      boolean blocked = this.host.blocked();

      for (SectionSearch.Row r : this.rows) {
         if (!r.leaving && !(r.appear < 0.5F) && !(my < r.y) && !(my >= r.y + r.h)) {
            switch (r.kind) {
               case 1:
                  if (button == 0 && this.onUse(r, mx, my)) {
                     r.style.show();
                     return true;
                  }

                  return false;
               case 2:
                  float jx = r.widget.x + r.widget.w + 10.0F;
                  float jy = r.y + Math.min(14.0F, r.widget.height() / 2.0F);
                  if (button == 0 && Math.abs(mx - jx) <= 8.0F && Math.abs(my - jy) <= 8.0F) {
                     this.host.reveal(r.result, r.widget);
                     return true;
                  }

                  if (!blocked && unavailable(r) == null) {
                     this.selectedKey = r.key;
                     if (r.widget.mouseDown(mx, my, button)) {
                        this.pressed = r.widget;
                        return true;
                     }

                     return false;
                  }

                  if (button == 0) {
                     ModSounds.gui(Sfx.ERROR);
                  }

                  return true;
               case 3:
                  if (button == 0 && this.didYouMean != null && my >= r.y + 22.0F) {
                     this.host.setQuery(this.didYouMean);
                  }

                  return true;
               case 4:
                  if (button == 0) {
                     this.host.openOther(r.other.index.page, this.text, r.other.result.entry);
                  }

                  return true;
               case 5:
                  if (button == 0) {
                     this.othersOpen = !this.othersOpen;
                     this.regroup();
                  }

                  return true;
               default:
                  return false;
            }
         }
      }

      return false;
   }

   private boolean onUse(SectionSearch.Row r, float mx, float my) {
      float uh = Math.min(17.0F, titleH() - 2.0F);
      float ux = this.useX(r, this.lastX, this.lastW);
      float uw = this.lastX + this.lastW - ux;
      float uy = r.y + titleH() / 2.0F - uh / 2.0F;
      return mx >= ux && mx < ux + uw && my >= uy && my < uy + uh;
   }

   boolean key(int keyCode) {
      if (keyCode != 208 && keyCode != 200) {
         if (keyCode != 28 && keyCode != 156) {
            return false;
         } else {
            for (SectionSearch.Row r : this.rows) {
               if (r.selectable() && r.key.equals(this.selectedKey)) {
                  if (r.kind == 2) {
                     this.host.reveal(r.result, r.widget);
                  } else {
                     this.host.openOther(r.other.index.page, this.text, r.other.result.entry);
                  }

                  return true;
               }
            }

            return true;
         }
      } else {
         ArrayList<SectionSearch.Row> sel = new ArrayList<>();
         int at = -1;

         for (SectionSearch.Row r2 : this.rows) {
            if (r2.selectable()) {
               if (r2.key.equals(this.selectedKey)) {
                  at = sel.size();
               }

               sel.add(r2);
            }
         }

         if (sel.isEmpty()) {
            return true;
         } else {
            int next = at < 0 ? (keyCode == 208 ? 0 : sel.size() - 1) : Math.max(0, Math.min(sel.size() - 1, at + (keyCode == 208 ? 1 : -1)));
            SectionSearch.Row r2x = sel.get(next);
            this.selectedKey = r2x.key;
            float top = r2x.y + this.bar.scroll - this.viewTop;
            this.bar.reveal(Math.max(0.0F, top - 26.0F), r2x.h + 30.0F);
            return true;
         }
      }
   }

   void highlight(SearchEntry e) {
      for (SectionSearch.Row r : this.rows) {
         if (r.kind == 2 && !r.leaving) {
            for (SearchResult res : r.all) {
               if (res.entry == e) {
                  this.selectedKey = r.key;
                  this.pulseKey = r.key;
                  this.pulseStart = ClientClock.nanos();
                  return;
               }
            }
         }
      }
   }

   Widget widgetForTest(String label) {
      for (SectionSearch.Row r : this.rows) {
         if (r.kind == 2 && !r.leaving) {
            for (SearchResult res : r.all) {
               if (res.entry.label.equalsIgnoreCase(label)) {
                  return r.widget;
               }
            }
         }
      }

      return null;
   }

   Widget widgetForTest(Setting setting) {
      Widget w = this.widgets == null ? null : this.widgets.get(setting);

      for (SectionSearch.Row r : this.rows) {
         if (r.kind == 2 && !r.leaving && w != null && r.widget == w) {
            return w;
         }
      }

      return null;
   }

   boolean useStyleForTest(String style) {
      for (SectionSearch.Row r : this.rows) {
         if (r.kind == 1 && !r.leaving && r.title.equalsIgnoreCase(style)) {
            r.style.show();
            return true;
         }
      }

      return false;
   }

   boolean openOtherForTest(int index) {
      int n = 0;

      for (SectionSearch.Row r : this.rows) {
         if (r.kind == 4 && !r.leaving && n++ == index) {
            this.host.openOther(r.other.index.page, this.text, r.other.result.entry);
            return true;
         }
      }

      return false;
   }

   boolean openOtherForTest(String page) {
      for (SectionSearch.Row r : this.rows) {
         if (r.kind == 4 && !r.leaving && r.other.index.page.equals(page)) {
            this.host.openOther(r.other.index.page, this.text, r.other.result.entry);
            return true;
         }
      }

      return false;
   }

   private static String unavailable(SectionSearch.Row r) {
      return r.result != null && r.result.entry.row != null ? r.result.entry.row.unavailableReason() : null;
   }

   String tooltip(float mx, float my) {
      for (SectionSearch.Row r : this.rows) {
         if (!r.leaving && !(r.appear < 0.5F) && !(my < r.y) && !(my >= r.y + r.h)) {
            switch (r.kind) {
               case 1:
                  return this.onUse(r, mx, my) ? "Makes " + r.style.styleName() + " the selected " + r.style.kind() : null;
               case 2:
                  float jx = r.widget.x + r.widget.w + 10.0F;
                  float jy = r.y + Math.min(14.0F, r.widget.height() / 2.0F);
                  if (Math.abs(mx - jx) <= 8.0F && Math.abs(my - jy) <= 8.0F) {
                     SettingStyle o = r.result.entry.owner;
                     return o != null && !o.isShown() && !o.isMultiple()
                        ? "Use " + o.styleName() + " and show this option in the section"
                        : "Show in the section (Enter)";
                  }

                  String why = unavailable(r);
                  return why != null ? why : r.widget.tooltip(mx, my);
               case 3:
               default:
                  return null;
               case 4:
                  return "Open " + r.other.index.title + " and show it there";
               case 5:
                  return this.othersOpen ? "Hide the matches of the other sections" : "Show the best matches of the other sections";
            }
         }
      }

      return null;
   }

   interface Host {
      void reveal(SearchResult var1, Widget var2);

      void openOther(String var1, String var2, SearchEntry var3);

      void setQuery(String var1);

      boolean blocked();

      List<SearchIndex> otherIndexes();
   }

   static final class Other {
      final SearchIndex index;
      final SearchResult result;

      Other(SearchIndex index, SearchResult result) {
         this.index = index;
         this.result = result;
      }
   }

   private static final class Row {
      static final int TITLE = 0;
      static final int STYLE = 1;
      static final int WIDGET = 2;
      static final int EMPTY = 3;
      static final int OTHER = 4;
      static final int MORE = 5;
      final int kind;
      final Object key;
      String title;
      int[] titleRanges;
      SettingStyle style;
      Widget widget;
      SearchResult result;
      final List<SearchResult> all = new ArrayList<>();
      Set<Object> filter;
      boolean whole;
      String snippet;
      int snipA;
      int snipB;
      SectionSearch.Other other;
      float appear = -1.0F;
      float hover;
      float jump;
      float use;
      boolean leaving;
      float y;
      float h;

      Row(int kind, Object key) {
         this.kind = kind;
         this.key = key;
      }

      boolean selectable() {
         return !this.leaving && (this.kind == 2 || this.kind == 4);
      }
   }
}
