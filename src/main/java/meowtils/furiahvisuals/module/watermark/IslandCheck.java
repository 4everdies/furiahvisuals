package meowtils.furiahvisuals.module.watermark;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.GuiCheck;
import meowtils.furiahvisuals.gui.UiProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;

public final class IslandCheck {
   private static final String[] KINDS = new String[]{
      "quick settings",
      "quick settings (narrow)",
      "panel, 1 column",
      "panel, 2 columns",
      "panel, 3 columns",
      "panel, 1 column, big island",
      "panel, 1 column, big island, scrolled"
   };
   private static final int SETTLE = 24;
   private static final int SCROLL = 12;
   private static IslandCheck running;
   public static String lastSummary;
   public static int lastCount = -1;
   private final WaterMarkModule wm;
   private final String menuSummary;
   private final int menuCount;
   private int anchor;
   private int kind = -1;
   private int wait;
   private boolean measureNext;
   private boolean scrolled;
   private int states;
   private final Map<String, String[]> found = new LinkedHashMap<>();
   private final long started = System.currentTimeMillis();
   private WaterMarkModule.Anchor anchorBefore;
   private WaterMarkModule.PanelColumns columnsBefore;
   private double scaleBefore;

   private IslandCheck(WaterMarkModule wm, String menuSummary, int menuCount) {
      this.wm = wm;
      this.menuSummary = menuSummary;
      this.menuCount = menuCount;
   }

   public static boolean isRunning() {
      return running != null;
   }

   public static boolean start(String menuSummary, int menuCount) {
      Minecraft mc = Minecraft.getMinecraft();
      WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
      if (mc.theWorld != null && wm != null && wm.isEnabled()) {
         IslandCheck c = new IslandCheck(wm, menuSummary, menuCount);
         c.anchorBefore = wm.anchor.get();
         c.columnsBefore = wm.panelColumns.get();
         c.scaleBefore = wm.scale.get();
         running = c;
         FuriahsVisuals.LOG.info("GUI check: the island, {} states", new Object[]{WaterMarkModule.Anchor.values().length * KINDS.length});
         return true;
      } else {
         return false;
      }
   }

   static void tick() {
      IslandCheck c = running;
      if (c != null) {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.theWorld == null) {
            c.finish(false);
         } else if (!(mc.currentScreen instanceof GuiChat)) {
            mc.displayGuiScreen(new GuiChat());
            c.wait = 6;
         } else if (c.wait > 0) {
            c.wait--;
         } else if (!c.measureNext) {
            c.next();
         }
      }
   }

   private void next() {
      WaterMarkModule.Anchor[] anchors = WaterMarkModule.Anchor.values();
      this.kind++;
      if (this.kind >= KINDS.length) {
         this.kind = 0;
         this.anchor++;
      }

      if (this.anchor >= anchors.length) {
         this.finish(true);
      } else {
         this.wm.anchor.set(anchors[this.anchor]);
         this.wm.island.roomForTest = this.kind == 1 ? 260.0F : Float.NaN;
         this.wm.scale.set(this.kind >= 5 ? 2.0 : this.scaleBefore);
         this.wm
            .panelColumns
            .set(this.kind == 3 ? WaterMarkModule.PanelColumns.TWO : (this.kind == 4 ? WaterMarkModule.PanelColumns.THREE : WaterMarkModule.PanelColumns.ONE));
         if (!this.wm.isSettingsViewOpen()) {
            this.wm.settingsViewForTest(true);
         }

         boolean panel = this.kind >= 2;
         if (panel != this.wm.isModulesRowOpen()) {
            this.wm.openModulesRow(panel);
         }

         this.scrolled = false;
         this.wait = 24;
         this.measureNext = true;
         if (this.kind == 6) {
            for (int i = 0; i < 40; i++) {
               this.wm.island.modulesPanel().wheel(-120);
            }

            this.wait = 36;
         }
      }
   }

   static boolean measuring() {
      IslandCheck c = running;
      return c != null && c.measureNext && c.wait <= 0;
   }

   static void measured(List<UiProbe.Item> items) {
      IslandCheck c = running;
      if (c != null) {
         c.measureNext = false;
         c.states++;
         String state = WaterMarkModule.Anchor.values()[c.anchor].displayName() + ", " + KINDS[c.kind];

         for (UiProbe.Overlap o : UiProbe.overlaps(new ArrayList<>(items))) {
            String key = c.kind + "|" + o.a.label + "|" + o.b.label;
            if (!c.found.containsKey(key)) {
               c.found.put(key, new String[]{state, o.toString()});
            }
         }

         if (items.isEmpty()) {
            c.found.put(c.kind + "|empty|" + c.anchor, new String[]{state, "nothing recorded (the island was not drawn)"});
         }
      }
   }

   private void finish(boolean done) {
      running = null;
      this.wm.anchor.set(this.anchorBefore);
      this.wm.panelColumns.set(this.columnsBefore);
      this.wm.scale.set(this.scaleBefore);
      this.wm.island.roomForTest = Float.NaN;
      this.wm.openModulesRow(false);
      this.wm.settingsViewForTest(false);
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.currentScreen instanceof GuiChat) {
         mc.displayGuiScreen(null);
      }

      String island = String.format(
         Locale.ROOT,
         "the island %s: %d overlap%s in %d states (%.0f s)",
         done ? "done" : "stopped",
         this.found.size(),
         this.found.size() == 1 ? "" : "s",
         this.states,
         (System.currentTimeMillis() - this.started) / 1000.0
      );
      StringBuilder sb = new StringBuilder("\n").append(island).append('\n');

      for (String[] f : this.found.values()) {
         sb.append('\n').append("Island (").append(f[0]).append(")\n  ").append(f[1]).append('\n');
         FuriahsVisuals.LOG.info("GUI check: island ({}): {}", new Object[]{f[0], f[1]});
      }

      File out = new File(mc.mcDataDir, "logs/furiahsvisuals-gui-check.txt");

      try {
         out.getParentFile().mkdirs();
         Files.write(out.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException var7) {
         FuriahsVisuals.LOG.warn("GUI check: could not write {}", new Object[]{out, var7});
      }

      lastSummary = island;
      lastCount = this.found.size();
      FuriahsVisuals.LOG.info("GUI check: {}", new Object[]{island});
      FuriahsVisuals.chat("GUI check: " + island + (this.found.isEmpty() ? "." : ": see logs/furiahsvisuals-gui-check.txt"));
      GuiCheck.islandDone(this.menuSummary + "; " + island, this.menuCount + this.found.size());
   }
}
