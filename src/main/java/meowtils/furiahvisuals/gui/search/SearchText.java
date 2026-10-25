package meowtils.furiahvisuals.gui.search;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;

public final class SearchText {
   private static final String[][] SYNONYMS = new String[][]{
      {"color", "colour"},
      {"opacity", "alpha", "transparency", "transparent"},
      {"chroma", "rainbow", "rgb"},
      {"size", "scale"},
      {"distance", "range"},
      {"speed", "fast", "slow"}
   };
   static final SearchText.Folded EMPTY = new SearchText.Folded("", new int[0], 0);

   private SearchText() {
   }

   public static SearchText.Folded fold(String s) {
      if (s != null && !s.isEmpty()) {
         StringBuilder out = new StringBuilder(s.length());
         int[] from = new int[s.length() * 2 + 2];
         int n = 0;
         boolean space = true;

         for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\'' && c != 8217) {
               String base = c < 128 ? null : decompose(c);
               int count = base == null ? 1 : base.length();

               for (int k = 0; k < count; k++) {
                  char b = base == null ? c : base.charAt(k);
                  int type = Character.getType(b);
                  if (type != 6 && type != 8 && type != 7) {
                     if (Character.isLetterOrDigit(b)) {
                        if (n + 1 >= from.length) {
                           from = Arrays.copyOf(from, from.length * 2);
                        }

                        out.append(Character.toLowerCase(b));
                        from[n++] = i;
                        space = false;
                     } else if (!space) {
                        if (n + 1 >= from.length) {
                           from = Arrays.copyOf(from, from.length * 2);
                        }

                        out.append(' ');
                        from[n++] = i;
                        space = true;
                     }
                  }
               }
            }
         }

         if (n > 0 && out.charAt(n - 1) == ' ') {
            out.setLength(--n);
         }

         return new SearchText.Folded(out.toString(), Arrays.copyOf(from, n), s.length());
      } else {
         return EMPTY;
      }
   }

   private static String decompose(char c) {
      switch (c) {
         case 'Æ':
         case 'æ':
            return "ae";
         case 'Ø':
         case 'ø':
            return "o";
         case 'ß':
            return "ss";
         case 'Đ':
         case 'đ':
            return "d";
         case 'Ł':
         case 'ł':
            return "l";
         case 'Œ':
         case 'œ':
            return "oe";
         default:
            return Normalizer.normalize(String.valueOf(c), Form.NFD);
      }
   }

   static String[] alternatives(String word) {
      ArrayList<String> alts = null;

      for (String[] group : SYNONYMS) {
         boolean hit = false;

         for (String g : group) {
            if (g.equals(word) || word.length() >= 4 && word.length() >= g.length() * 0.6F && g.startsWith(word)) {
               hit = true;
               break;
            }
         }

         if (hit) {
            if (alts == null) {
               alts = new ArrayList<>();
               alts.add(word);
            }

            for (String gx : group) {
               if (!alts.contains(gx) && !gx.startsWith(word)) {
                  alts.add(gx);
               }
            }
         }
      }

      String[] stringArray;
      if (alts == null) {
         String[] stringArray2 = new String[1];
         stringArray = stringArray2;
         stringArray2[0] = word;
      } else {
         stringArray = alts.toArray(new String[0]);
      }

      return stringArray;
   }

   static boolean withinOneTypo(String a, int la, String b, int lb) {
      if (Math.abs(la - lb) > 1) {
         return false;
      } else {
         int i = 0;

         while (i < la && i < lb && a.charAt(i) == b.charAt(i)) {
            i++;
         }

         if (i == la && i == lb) {
            return true;
         } else if (la == lb) {
            return a.regionMatches(i + 1, b, i + 1, la - i - 1)
               ? true
               : i + 1 < la && a.charAt(i) == b.charAt(i + 1) && a.charAt(i + 1) == b.charAt(i) && a.regionMatches(i + 2, b, i + 2, la - i - 2);
         } else {
            return la > lb ? a.regionMatches(i + 1, b, i, lb - i) : b.regionMatches(i + 1, a, i, la - i);
         }
      }
   }

   static boolean typoMatch(String typed, String word) {
      int lt = typed.length();
      int lw;
      if (withinOneTypo(typed, lt, word, lw = word.length())) {
         return true;
      } else {
         for (int l = lt - 1; l <= lt + 1; l++) {
            if (l > 0 && l < lw && withinOneTypo(typed, lt, word, l)) {
               return true;
            }
         }

         return false;
      }
   }

   static int distance(String a, String b, int max) {
      int la = a.length();
      int lb;
      if (Math.abs(la - (lb = b.length())) > max) {
         return max + 1;
      } else {
         int[] prev2 = new int[lb + 1];
         int[] prev = new int[lb + 1];
         int[] cur = new int[lb + 1];
         int j = 0;

         while (j <= lb) {
            prev[j] = j++;
         }

         for (int i = 1; i <= la; i++) {
            cur[0] = i;
            int best = cur[0];

            for (int jx = 1; jx <= lb; jx++) {
               int cost = a.charAt(i - 1) == b.charAt(jx - 1) ? 0 : 1;
               int v = Math.min(Math.min(prev[jx] + 1, cur[jx - 1] + 1), prev[jx - 1] + cost);
               if (i > 1 && jx > 1 && a.charAt(i - 1) == b.charAt(jx - 2) && a.charAt(i - 2) == b.charAt(jx - 1)) {
                  v = Math.min(v, prev2[jx - 2] + 1);
               }

               cur[jx] = v;
               best = Math.min(best, v);
            }

            if (best > max) {
               return max + 1;
            }

            int[] t = prev2;
            prev2 = prev;
            prev = cur;
            cur = t;
         }

         return prev[lb];
      }
   }

   public static final class Folded {
      public final String text;
      final int[] from;
      final String[] words;
      final int[] wordStart;
      final int length;

      Folded(String text, int[] from, int length) {
         this.text = text;
         this.from = from;
         this.length = length;
         ArrayList<String> w = new ArrayList<>();
         ArrayList<Integer> starts = new ArrayList<>();
         int start = 0;

         for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == ' ') {
               if (i > start) {
                  w.add(text.substring(start, i));
                  starts.add(start);
               }

               start = i + 1;
            }
         }

         this.words = w.toArray(new String[0]);
         this.wordStart = new int[starts.size()];

         for (int var8 = 0; var8 < this.wordStart.length; var8++) {
            this.wordStart[var8] = starts.get(var8);
         }
      }

      boolean isEmpty() {
         return this.text.isEmpty();
      }

      boolean wordStartAt(int i) {
         return i == 0 || this.text.charAt(i - 1) == ' ';
      }

      int originalStart(int start) {
         return start < this.from.length ? this.from[start] : this.length;
      }

      int originalEnd(int end) {
         return end <= 0 ? 0 : Math.min(this.length, this.from[end - 1] + 1);
      }
   }
}
