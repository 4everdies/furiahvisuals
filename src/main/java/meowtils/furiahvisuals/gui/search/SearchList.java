package meowtils.furiahvisuals.gui.search;

import java.util.Set;
import meowtils.furiahvisuals.setting.Setting;

public interface SearchList {
   void searchItems(SearchList.Items var1);

   void searchFilter(Set<Object> var1);

   float reveal(Object var1);

   float revealHeight(Object var1);

   int searchVersion();

   public interface Items {
      void item(Object var1, String var2, String var3, String... var4);

      void option(Object var1, Setting var2);
   }
}
