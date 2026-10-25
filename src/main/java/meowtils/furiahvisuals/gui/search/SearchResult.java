package meowtils.furiahvisuals.gui.search;

public final class SearchResult {
   public static final int EXACT = 1;
   public static final int STARTS = 2;
   public static final int WORD_START = 3;
   public static final int CONTAINS = 4;
   public static final int GROUP = 5;
   public static final int OTHER = 6;
   public static final int TYPO = 7;
   public final SearchEntry entry;
   public final int tier;
   public final int[] label;
   public final int groupStart;
   public final int groupEnd;
   public final String snippet;
   public final int snippetStart;
   public final int snippetEnd;
   public final String choice;
   public final int choiceStart;
   public final int choiceEnd;

   SearchResult(
      SearchEntry entry,
      int tier,
      int[] label,
      int groupStart,
      int groupEnd,
      String snippet,
      int snippetStart,
      int snippetEnd,
      String choice,
      int choiceStart,
      int choiceEnd
   ) {
      this.entry = entry;
      this.tier = tier;
      this.label = label;
      this.groupStart = groupStart;
      this.groupEnd = groupEnd;
      this.snippet = snippet;
      this.snippetStart = snippetStart;
      this.snippetEnd = snippetEnd;
      this.choice = choice;
      this.choiceStart = choiceStart;
      this.choiceEnd = choiceEnd;
   }

   @Override
   public String toString() {
      return this.entry.path() + " (" + this.tier + ")";
   }
}
