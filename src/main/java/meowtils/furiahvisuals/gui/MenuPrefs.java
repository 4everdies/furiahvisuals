package meowtils.furiahvisuals.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.Displayable;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.setting.StringListSetting;

public final class MenuPrefs {
   private static final int RECENTS = 10;
   public static final int FAVORITES = 16;
   public static final ModeSetting<Density.Mode> VIEW_THEME = new ModeSetting<>(
         "viewTheme", "View Theme", "How much fits on a page: Comfortable (roomy), Compact or Dense.", Density.Mode.class, Density.Mode.COMPACT
      )
      .keywords(new String[]{"density", "compact", "comfortable", "dense", "size", "rows"});
   public static final ModeSetting<MenuPrefs.SidebarStyle> SIDEBAR = new ModeSetting<>(
         "sidebarStyle",
         "Sidebar",
         "The sidebar: Full, icons only, or hidden (narrow menus show icons only by themselves).",
         MenuPrefs.SidebarStyle.class,
         MenuPrefs.SidebarStyle.FULL
      )
      .keywords(new String[]{"sidebar", "navigation", "icons", "sections list"});
   public static final BooleanSetting GUI_SCALE_AUTO = new BooleanSetting(
      "guiScaleAuto", "GUI Scale: Auto", "The menu picks a size that fits the window.", true
   );
   public static final SliderSetting GUI_SCALE = new SliderSetting(
         "guiScale",
         "GUI Scale",
         "The menu's size (100 %: its size in a 1080p window). Separate from Minecraft's own GUI scale; Ctrl+scroll zooms, Ctrl+0 goes back to Auto.",
         75.0,
         200.0,
         100.0,
         1.0
      )
      .unit("%")
      .keywords(new String[]{"gui scale", "zoom", "size", "bigger", "smaller", "menu size"});
   public static final BooleanSetting FULL_SCREEN = new BooleanSetting(
         "guiFullScreen", "Full Screen", "The menu fills the window, minus a small margin.", false
      )
      .keywords(new String[]{"full screen", "fullscreen", "maximize", "panel size"});
   public static final SliderSetting PANEL_W = new SliderSetting(
      "guiPanelWidth", "Panel Width", "The menu's width, dragged at its corner (0: the usual size).", 0.0, 5000.0, 0.0, 1.0
   );
   public static final SliderSetting PANEL_H = new SliderSetting(
      "guiPanelHeight", "Panel Height", "The menu's height, dragged at its corner (0: the usual size).", 0.0, 5000.0, 0.0, 1.0
   );
   public static final int[] SCALE_STEPS = new int[]{75, 90, 100, 110, 125, 150, 175, 200};
   public static final StringListSetting FAVORITE_COLORS = new StringListSetting(
      "favoriteColors", "Favorite Colors", "The colors you starred, at the top of every color picker."
   );
   public static final StringListSetting RECENT_COLORS = new StringListSetting("recentColors", "Recent Colors", "The colors you picked last.");

   private MenuPrefs() {
   }

   public static List<Setting> all() {
      return Arrays.asList(VIEW_THEME, SIDEBAR, GUI_SCALE_AUTO, GUI_SCALE, FULL_SCREEN, PANEL_W, PANEL_H, FAVORITE_COLORS, RECENT_COLORS);
   }

   public static List<Integer> favorites() {
      return parse(FAVORITE_COLORS.get());
   }

   public static List<Integer> recents() {
      return parse(RECENT_COLORS.get());
   }

   public static boolean isFavorite(int color) {
      return FAVORITE_COLORS.contains(hex(color));
   }

   public static boolean toggleFavorite(int color) {
      String h = hex(color);
      if (FAVORITE_COLORS.contains(h)) {
         FAVORITE_COLORS.remove(h);
         return false;
      } else {
         ArrayList<String> list = new ArrayList<>(FAVORITE_COLORS.get());
         list.add(0, h);

         while (list.size() > 16) {
            list.remove(list.size() - 1);
         }

         FAVORITE_COLORS.set(list);
         return true;
      }
   }

   public static void used(int color) {
      String h = hex(color);
      ArrayList<String> list = new ArrayList<>(RECENT_COLORS.get());

      for (int i = list.size() - 1; i >= 0; i--) {
         if (list.get(i).equalsIgnoreCase(h)) {
            list.remove(i);
         }
      }

      list.add(0, h);

      while (list.size() > 10) {
         list.remove(list.size() - 1);
      }

      RECENT_COLORS.set(list);
   }

   private static String hex(int color) {
      return color >>> 24 == 255 ? ColorUtil.toHex(color) : String.format("#%08X", color);
   }

   private static List<Integer> parse(List<String> list) {
      ArrayList<Integer> out = new ArrayList<>();

      for (String s : list) {
         String h = s.startsWith("#") ? s.substring(1) : s;

         try {
            long v = Long.parseLong(h, 16);
            out.add(h.length() > 6 ? (int)v : (int)(v | 4278190080L));
         } catch (NumberFormatException var7) {
         }
      }

      return out;
   }

   static {
      Density.attach(VIEW_THEME);
   }

   public static enum SidebarStyle implements Displayable {
      FULL("Full", "Every section's name and icon, in categories"),
      ICONS("Icons", "Only the icons (names in tooltips): more room for the settings"),
      HIDDEN("Hidden", "No sidebar: the header's ☰ lists the sections, its magnifier searches them all");

      private final String displayName;
      private final String description;

      private SidebarStyle(String displayName, String description) {
         this.displayName = displayName;
         this.description = description;
      }

      @Override
      public String displayName() {
         return this.displayName;
      }

      @Override
      public String description() {
         return this.description;
      }
   }
}
