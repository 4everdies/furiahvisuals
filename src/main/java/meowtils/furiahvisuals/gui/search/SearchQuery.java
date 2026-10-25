package meowtils.furiahvisuals.gui.search;

public final class SearchQuery {
   public static final int MAX_LENGTH = 64;
   public final String raw;
   final String folded;
   final String[] words;
   final String[][] alternatives;

   private SearchQuery(String raw) {
      this.raw = raw;
      this.folded = SearchText.fold(raw).text;
      this.words = this.folded.isEmpty() ? new String[0] : this.folded.split(" ");
      this.alternatives = new String[this.words.length][];

      for (int i = 0; i < this.words.length; i++) {
         this.alternatives[i] = SearchText.alternatives(this.words[i]);
      }
   }

   public static SearchQuery of(String raw) {
      return new SearchQuery(raw == null ? "" : raw);
   }

   public boolean isEmpty() {
      return this.words.length == 0;
   }
}
