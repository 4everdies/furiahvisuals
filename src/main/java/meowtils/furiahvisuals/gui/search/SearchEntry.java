package meowtils.furiahvisuals.gui.search;

import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SettingStyle;

public final class SearchEntry {
   public final SearchEntry.Kind kind;
   public final Setting row;
   public final Object key;
   public final String label;
   public final String group;
   public final String description;
   public final SettingStyle owner;
   public final int order;
   final String[] choices;
   final String[] keywords;
   final SearchText.Folded fLabel;
   final SearchText.Folded fGroup;
   final SearchText.Folded fDesc;
   final SearchText.Folded[] fChoices;
   final SearchText.Folded[] fKeywords;

   SearchEntry(
      SearchEntry.Kind kind,
      Setting row,
      Object key,
      String label,
      String group,
      String description,
      SettingStyle owner,
      int order,
      String[] choices,
      String[] keywords
   ) {
      this.kind = kind;
      this.row = row;
      this.key = key;
      this.label = label == null ? "" : label;
      this.group = group;
      this.description = description == null ? "" : description;
      this.owner = owner;
      this.order = order;
      this.choices = choices == null ? new String[0] : choices;
      this.keywords = keywords == null ? new String[0] : keywords;
      this.fLabel = SearchText.fold(this.label);
      this.fGroup = SearchText.fold(group);
      this.fDesc = SearchText.fold(this.description);
      this.fChoices = new SearchText.Folded[this.choices.length];

      for (int i = 0; i < this.fChoices.length; i++) {
         this.fChoices[i] = SearchText.fold(this.choices[i]);
      }

      this.fKeywords = new SearchText.Folded[this.keywords.length];

      for (int var12 = 0; var12 < this.fKeywords.length; var12++) {
         this.fKeywords[var12] = SearchText.fold(this.keywords[var12]);
      }
   }

   public String path() {
      return this.group != null && !this.group.isEmpty() ? this.group + " › " + this.label : this.label;
   }

   @Override
   public String toString() {
      return this.kind + " " + this.path();
   }

   public static enum Kind {
      SETTING,
      CARD,
      ITEM,
      OPTION,
      BLOCK;
   }
}
