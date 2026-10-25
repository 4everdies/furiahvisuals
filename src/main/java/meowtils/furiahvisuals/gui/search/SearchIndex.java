package meowtils.furiahvisuals.gui.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;

public final class SearchIndex {
   private static final Comparator<SearchResult> BY_TIER = new Comparator<SearchResult>() {
      public int compare(SearchResult a, SearchResult b) {
         return a.tier != b.tier ? a.tier - b.tier : a.entry.order - b.entry.order;
      }
   };
   public final String page;
   public final String title;
   private final List<SearchEntry> entries = new ArrayList<>();
   int version;

   public SearchIndex(String page, String title) {
      this.page = page;
      this.title = title;
   }

   public List<SearchEntry> entries() {
      return this.entries;
   }

   public SearchEntry add(
      SearchEntry.Kind kind, Setting row, Object key, String label, String group, String description, SettingStyle owner, String[] choices, String[] keywords
   ) {
      SearchEntry e = new SearchEntry(kind, row, key, label, group, description, owner, this.entries.size(), choices, keywords);
      this.entries.add(e);
      return e;
   }

   public List<SearchResult> search(SearchQuery q, List<SearchResult> out) {
      out.clear();
      if (q.isEmpty()) {
         return out;
      } else {
         for (int i = 0; i < this.entries.size(); i++) {
            SearchResult r = match(this.entries.get(i), q);
            if (r != null) {
               out.add(r);
            }
         }

         Collections.sort(out, BY_TIER);
         return out;
      }
   }

   public int count(SearchQuery q) {
      if (q.isEmpty()) {
         return 0;
      } else {
         int n = 0;

         for (int i = 0; i < this.entries.size(); i++) {
            if (match(this.entries.get(i), q) != null) {
               n++;
            }
         }

         return n;
      }
   }

   public String didYouMean(SearchQuery q) {
      if (q.isEmpty()) {
         return null;
      } else {
         int max = Math.max(2, q.folded.length() / 3);
         String best = null;
         int bestD = max + 1;

         for (SearchEntry e : this.entries) {
            if (!e.fLabel.isEmpty()) {
               int d = SearchText.distance(q.folded, e.fLabel.text, max);
               if (q.words.length == 1) {
                  for (String w : e.fLabel.words) {
                     d = Math.min(d, SearchText.distance(q.folded, w, max));
                  }
               }

               if (d < bestD) {
                  bestD = d;
                  best = e.label;
               }
            }
         }

         return best;
      }
   }

   static SearchResult match(SearchEntry e, SearchQuery q) {
      int n = q.words.length;
      int[] label = new int[n * 2];
      int labelCount = 0;
      int groupStart = -1;
      int groupEnd = -1;
      boolean allLabel = true;
      boolean allLabelOrGroup = true;
      boolean typo = false;
      boolean allWordStarts = true;
      String snippet = null;
      int snippetStart = 0;
      int snippetEnd = 0;
      String choice = null;
      int choiceStart = 0;
      int choiceEnd = 0;

      for (int k = 0; k < n; k++) {
         String[] alts = q.alternatives[k];
         long hit = find(e.fLabel, alts);
         if (hit >= 0L) {
            int s2 = start(hit);
            if (!e.fLabel.wordStartAt(s2)) {
               allWordStarts = false;
            }

            label[labelCount++] = e.fLabel.originalStart(s2);
            label[labelCount++] = e.fLabel.originalEnd(s2 + length(hit));
         } else {
            allLabel = false;
            hit = find(e.fGroup, alts);
            if (hit >= 0L) {
               if (groupStart < 0) {
                  groupStart = e.fGroup.originalStart(start(hit));
                  groupEnd = e.fGroup.originalEnd(start(hit) + length(hit));
               }
            } else {
               allLabelOrGroup = false;
               boolean found = false;

               for (int c2 = 0; c2 < e.fChoices.length && !found; c2++) {
                  hit = find(e.fChoices[c2], alts);
                  if (hit >= 0L) {
                     found = true;
                     if (choice == null) {
                        choice = e.choices[c2];
                        choiceStart = e.fChoices[c2].originalStart(start(hit));
                        choiceEnd = e.fChoices[c2].originalEnd(start(hit) + length(hit));
                     }
                  }
               }

               if (!found) {
                  hit = find(e.fDesc, alts);
                  if (hit >= 0L) {
                     if (snippet == null) {
                        int[] range = new int[2];
                        snippet = snippet(e.description, e.fDesc.originalStart(start(hit)), e.fDesc.originalEnd(start(hit) + length(hit)), range);
                        snippetStart = range[0];
                        snippetEnd = range[1];
                     }
                  } else {
                     for (int var36 = 0; var36 < e.fKeywords.length && !found; var36++) {
                        hit = find(e.fKeywords[var36], alts);
                        if (hit >= 0L) {
                           found = true;
                           if (snippet == null) {
                              snippet = "Also: " + e.keywords[var36];
                              snippetStart = 6 + e.fKeywords[var36].originalStart(start(hit));
                              snippetEnd = 6 + e.fKeywords[var36].originalEnd(start(hit) + length(hit));
                           }
                        }
                     }

                     if (!found) {
                        String w = q.words[k];
                        if (w.length() < 4) {
                           return null;
                        }

                        int word = typoWord(e.fLabel, w);
                        if (word >= 0) {
                           typo = true;
                           int s = e.fLabel.wordStart[word];
                           label[labelCount++] = e.fLabel.originalStart(s);
                           label[labelCount++] = e.fLabel.originalEnd(s + e.fLabel.words[word].length());
                        } else {
                           word = typoWord(e.fGroup, w);
                           if (word >= 0) {
                              typo = true;
                              if (groupStart < 0) {
                                 int s = e.fGroup.wordStart[word];
                                 groupStart = e.fGroup.originalStart(s);
                                 groupEnd = e.fGroup.originalEnd(s + e.fGroup.words[word].length());
                              }
                           } else {
                              for (int c = 0; c < e.fChoices.length && !found; c++) {
                                 word = typoWord(e.fChoices[c], w);
                                 if (word >= 0) {
                                    found = true;
                                    if (choice == null) {
                                       int s3 = e.fChoices[c].wordStart[word];
                                       choice = e.choices[c];
                                       choiceStart = e.fChoices[c].originalStart(s3);
                                       choiceEnd = e.fChoices[c].originalEnd(s3 + e.fChoices[c].words[word].length());
                                    }
                                 }
                              }

                              if (!found && (word = typoWord(e.fDesc, w)) >= 0) {
                                 found = true;
                                 if (snippet == null) {
                                    int s = e.fDesc.wordStart[word];
                                    int[] range = new int[2];
                                    snippet = snippet(e.description, e.fDesc.originalStart(s), e.fDesc.originalEnd(s + e.fDesc.words[word].length()), range);
                                    snippetStart = range[0];
                                    snippetEnd = range[1];
                                 }
                              }

                              for (int var33 = 0; var33 < e.fKeywords.length && !found; var33++) {
                                 word = typoWord(e.fKeywords[var33], w);
                                 if (word >= 0) {
                                    found = true;
                                    if (snippet == null) {
                                       snippet = "Also: " + e.keywords[var33];
                                       snippetStart = 6;
                                       snippetEnd = snippet.length();
                                    }
                                 }
                              }

                              if (!found) {
                                 return null;
                              }

                              typo = true;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      int var10000;
      if (allLabel && !typo) {
         String lab = e.fLabel.text;
         var10000 = !e.fLabel.text.equals(q.folded) && (n != 1 || !anyEquals(lab, q.alternatives[0]))
            ? (!lab.startsWith(q.folded) && (n != 1 || !anyStarts(lab, q.alternatives[0])) ? (allWordStarts ? 3 : 4) : 2)
            : 1;
      } else {
         var10000 = allLabelOrGroup && !typo ? 5 : (!typo ? 6 : 7);
      }

      int tier = var10000;
      int[] ranges = labelCount == 0 ? new int[0] : merge(label, labelCount);
      if (allLabelOrGroup) {
         snippet = null;
      }

      return new SearchResult(e, tier, ranges, groupStart, groupEnd, snippet, snippetStart, snippetEnd, choice, choiceStart, choiceEnd);
   }

   private static long find(SearchText.Folded f, String[] alternatives) {
      if (f.isEmpty()) {
         return -1L;
      } else {
         long best = -1L;

         for (String a : alternatives) {
            int from = 0;

            int i;
            while (from <= f.text.length() - a.length() && (i = f.text.indexOf(a, from)) >= 0) {
               if (f.wordStartAt(i)) {
                  return (long)i << 32 | a.length();
               }

               if (best < 0L) {
                  best = (long)i << 32 | a.length();
               }

               from = i + 1;
            }
         }

         return best;
      }
   }

   private static int start(long hit) {
      return (int)(hit >>> 32);
   }

   private static int length(long hit) {
      return (int)(hit & 4294967295L);
   }

   private static int typoWord(SearchText.Folded f, String typed) {
      for (int i = 0; i < f.words.length; i++) {
         if (f.words[i].length() >= 3 && SearchText.typoMatch(typed, f.words[i])) {
            return i;
         }
      }

      return -1;
   }

   private static boolean anyEquals(String text, String[] words) {
      for (String w : words) {
         if (text.equals(w)) {
            return true;
         }
      }

      return false;
   }

   private static boolean anyStarts(String text, String[] words) {
      for (String w : words) {
         if (text.startsWith(w)) {
            return true;
         }
      }

      return false;
   }

   private static int[] merge(int[] ranges, int count) {
      int pairs = count / 2;

      for (int i = 1; i < pairs; i++) {
         for (int j = i; j > 0 && ranges[j * 2] < ranges[(j - 1) * 2]; j--) {
            int s = ranges[j * 2];
            int e = ranges[j * 2 + 1];
            ranges[j * 2] = ranges[(j - 1) * 2];
            ranges[j * 2 + 1] = ranges[(j - 1) * 2 + 1];
            ranges[(j - 1) * 2] = s;
            ranges[(j - 1) * 2 + 1] = e;
         }
      }

      int[] out = new int[count];
      int n = 0;

      for (int i = 0; i < pairs; i++) {
         int s = ranges[i * 2];
         int e = ranges[i * 2 + 1];
         if (n > 0 && s <= out[n - 1]) {
            out[n - 1] = Math.max(out[n - 1], e);
         } else {
            out[n++] = s;
            out[n++] = e;
         }
      }

      return Arrays.copyOf(out, n);
   }

   static String snippet(String text, int start, int end, int[] range) {
      int from = Math.max(0, start - 26);
      int to = Math.min(text.length(), end + 42);
      int space;
      if (from > 0 && (space = text.indexOf(32, from)) >= 0 && space < start) {
         from = space + 1;
      }

      if (to < text.length() && (space = text.lastIndexOf(32, to)) > end) {
         to = space;
      }

      String prefix = from > 0 ? "…" : "";
      int cut = to;

      while (cut > from && text.charAt(cut - 1) == ' ') {
         cut--;
      }

      String piece = prefix + text.substring(from, cut) + (to < text.length() ? "…" : "");
      int shift = prefix.length() - from;
      range[0] = start + shift;
      range[1] = end + shift;
      return piece;
   }
}
