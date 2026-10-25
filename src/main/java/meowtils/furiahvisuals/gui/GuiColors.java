package meowtils.furiahvisuals.gui;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.gui.popup.Popover;
import meowtils.furiahvisuals.gui.popup.Popovers;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.gui.widget.Segmented;
import meowtils.furiahvisuals.gui.widget.SliderWidget;
import meowtils.furiahvisuals.gui.widget.ToggleWidget;
import meowtils.furiahvisuals.module.Module;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.module.watermark.WaterMarkModule;
import meowtils.furiahvisuals.module.weapons.WeaponsModule;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.setting.SliderSetting;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public final class GuiColors extends Popover {
   public static final int TOLERANCE = 2;
   private static final float W = 452.0F;
   private static final float PAD = 12.0F;
   private static final int COLS = 5;
   private static final float OFF = -10000.0F;
   private static final String[] NAMES = new String[]{
      "Switch",
      "ON pill",
      "Slider fill",
      "Selected chip",
      "Selected tab",
      "Heading dot",
      "FV",
      "Accent text",
      "Section tab",
      "Tab dot",
      "Island switch",
      "Weapon Style",
      "Style tag",
      "Compare line",
      "New pill",
      "Island FV"
   };
   private static final int ISLAND = 15;
   private static final int ISLAND_SWITCH = 10;
   private static final int STYLE_RING = 11;
   private static final int STYLE_TAG = 12;
   private static final int COMPARE = 13;
   private static final int NEW_PILL = 14;
   private static final int ROWS = 3;
   private static final String[] TABS = new String[]{"One", "Two"};
   private static boolean pending;
   public static String lastSummary;
   public static int lastOff = -1;
   private final float[][] box = new float[NAMES.length][];
   private final int[] expected = new int[NAMES.length];
   private final int[] got = new int[NAMES.length];
   private final int[] diff = new int[NAMES.length];
   private final SliderWidget slider = new SliderWidget(new SliderSetting("colorcheck.slider", "", "", 0.0, 100.0, 60.0, 1.0));
   private final Segmented tabs = new Segmented();
   private final Pill again = new Pill();
   private boolean islandAccent;
   private int stable;
   private boolean measured;
   private float gap = 8.0F;
   private float head = 38.0F;
   private float tileH = 62.0F;
   private float islandH = 58.0F;
   private float foot = 32.0F;

   private GuiColors(float viewH) {
      Arrays.fill(this.diff, -1);
      if (this.preferredH() > viewH - 8.0F) {
         this.gap = 6.0F;
         this.head = 32.0F;
         this.tileH = 48.0F;
         this.islandH = 48.0F;
         this.foot = 28.0F;
      }
   }

   public static void request() {
      pending = true;
      lastOff = -1;
      lastSummary = null;
      FuriahsVisuals.instance.openMenuNextTick();
   }

   static void attach(float viewW, float viewH) {
      if (pending) {
         pending = false;
         GuiColors board = new GuiColors(viewH);
         Popovers.open(board, (viewW - 452.0F) / 2.0F, Math.max(0.0F, (viewH - board.preferredH()) / 2.0F - 4.0F), 0.0F, 0.0F);
      }
   }

   @Override
   protected float preferredW() {
      return 452.0F;
   }

   @Override
   protected float preferredH() {
      return this.head + 3.0F * (this.tileH + this.gap) + this.islandH + this.foot;
   }

   @Override
   protected void renderContent(float mx, float my, float dt, int accent) {
      if (this.appear > 0.99F) {
         this.appear = 1.0F;
      }

      int a = accent | 0xFF000000;
      Ui.textMiddle("Accent colors", this.x + 12.0F, this.y + this.head * 0.34F, 10.0F, Theme.TEXT, true);
      String sub = "Accent "
         + hex(a)
         + (a == Theme.ACCENT ? "" : " (the token is " + hex(Theme.ACCENT) + ")")
         + "  ·  each part within ±"
         + 2
         + " per channel, read where it is solid";
      Ui.textMiddle(Ui.ellipsize(sub, 7.6F, this.w - 24.0F, false), this.x + 12.0F, this.y + this.head * 0.71F, 7.6F, Theme.TEXT_DIM, false);
      float tw = (this.w - 24.0F - this.gap * 4.0F) / 5.0F;

      for (int i = 0; i < 15; i++) {
         float tx = this.x + 12.0F + i % 5 * (tw + this.gap);
         float ty = this.y + this.head + i / 5 * (this.tileH + this.gap);
         this.tile(i, tx, ty, tw, a, dt);
      }

      this.islandTile(this.x + 12.0F, this.y + this.head + 3.0F * (this.tileH + this.gap), this.w - 24.0F);
      if (!this.measured) {
         int n = this.stable = this.appear >= 1.0F && Ui.alpha() >= 0.999F ? this.stable + 1 : 0;
         if (this.stable >= 3) {
            this.measure();
         }
      }

      float fy = this.y + this.h - this.foot + (this.foot - 20.0F) / 2.0F;
      String summary = lastOff < 0
         ? "Measuring…"
         : (lastOff == 0 ? "All parts show the exact color." : lastOff + " part" + (lastOff == 1 ? " is" : "s are") + " off: see the red ones.");
      Ui.textMiddle(summary, this.x + 12.0F, fy + 10.0F, 8.4F, lastOff == 0 ? -12723068 : (lastOff > 0 ? Theme.OFF : Theme.TEXT_DIM), true);
      String label = "Again";
      this.again.layout(this.x + this.w - 12.0F - (Ui.width(label, 8.8F) + 20.0F), fy, label);
      this.again.draw(label, mx, my, dt, a, false, this.measured);
   }

   private void tile(int i, float tx, float ty, float tw, int a, float dt) {
      Ui.roundRect(tx, ty, tw, this.tileH, 7.0F, Theme.SURFACE);
      Ui.textMiddle(NAMES[i], tx + 7.0F, ty + 9.0F, 7.4F, Theme.TEXT_DIM, false);
      float cx = tx + tw / 2.0F;
      float cy = ty + this.tileH / 2.0F + 1.0F;
      this.expected[i] = a;
      switch (i) {
         case 0: {
            float sx = cx - 14.0F;
            float sy = cy - 7.5F;
            ToggleWidget.drawSwitch(sx, sy, 28.0F, 15.0F, 1.0F, a);
            this.box[i] = new float[]{sx + 3.0F, sy + 5.5F, 4.0F, 4.0F};
            break;
         }
         case 1:
            float px = cx - 14.0F;
            ToggleWidget.drawPill(px, cy, 28.0F, 13.0F, 1.0F, a, 0.0F);
            this.box[i] = new float[]{px + 2.5F, cy - 1.5F, 3.0F, 3.0F};
            break;
         case 2:
            this.slider.x = tx + 7.0F;
            this.slider.y = ty + this.tileH / 2.0F - 21.0F;
            this.slider.w = tw - 14.0F;
            this.slider.render(-10000.0F, -10000.0F, dt, a);
            float[] m = this.slider.fillMiddleForCheck();
            this.box[i] = new float[]{m[0] - 2.0F, m[1] - 1.0F, 4.0F, 2.0F};
            break;
         case 3: {
            String label = "Chip";
            float cw = Ui.widthBold(label, 8.6F) + 20.0F;
            float ch = 18.0F;
            float chx = cx - cw / 2.0F;
            float chy = cy - ch / 2.0F;
            Ui.roundRect(chx, chy, cw, ch, ch / 2.0F, Theme.ACCENT_SOFT);
            Ui.accentOutline(chx, chy, cw, ch, ch / 2.0F, 1.0F, a);
            Ui.textMiddle(label, chx + 10.0F, cy, 8.6F, Theme.TEXT, true);
            this.box[i] = new float[]{cx - 2.0F, chy - 1.5F, 4.0F, 4.0F};
            break;
         }
         case 4: {
            float sx = tx + 7.0F;
            float sw = tw - 14.0F;
            this.tabs.render(sx, cy - 9.0F, sw, 18.0F, TABS, 0, -10000.0F, -10000.0F, dt, a);
            this.box[i] = new float[]{sx + 0.5F, cy - 2.0F, 4.0F, 4.0F};
            break;
         }
         case 5:
            Ui.glow(cx, cy, 14.0F, ColorUtil.withAlpha(a, 0.35F));
            Ui.circle(cx, cy, 2.2F, a);
            this.box[i] = new float[]{cx - 1.0F, cy - 1.0F, 2.0F, 2.0F};
            break;
         case 6:
            float fw = Ui.widthHeavy("FV", 9.6F);
            Ui.textMiddleHeavy("FV", cx - fw / 2.0F, cy, 9.6F, a);
            this.box[i] = new float[]{cx - fw / 2.0F, cy - 4.5F, fw, 9.0F};
            break;
         case 7: {
            String s = "Saved";
            float sw = Ui.widthHeavy(s, 8.4F);
            Ui.textMiddleHeavy(s, cx - sw / 2.0F, cy, 8.4F, a);
            this.box[i] = new float[]{cx - sw / 2.0F, cy - 4.0F, sw, 8.0F};
            break;
         }
         case 8: {
            String label = "Chat";
            float th = 20.0F;
            float ic = th * 0.52F;
            float tw2 = 9.0F + ic + 5.0F + Ui.widthBold(label, 9.0F) + 9.0F;
            float tx2 = cx - tw2 / 2.0F;
            float ty2 = cy - th / 2.0F;
            Ui.roundRect(tx2, ty2, tw2, th, th / 2.0F, Theme.ACCENT_SOFT);
            Ui.accentOutline(tx2, ty2, tw2, th, th / 2.0F, 1.0F, a);
            IslandIcons.draw(IslandIcons.Icon.CHAT, tx2 + 9.0F, ty2 + (th - ic) / 2.0F, ic, a);
            Ui.textMiddle(label, tx2 + 9.0F + ic + 5.0F, cy, 9.0F, Theme.TEXT, true);
            this.box[i] = new float[]{tx2 + tw2 / 2.0F - 2.0F, ty2 - 1.5F, 4.0F, 4.0F};
            break;
         }
         case 9:
            TabRow.dot(cx, cy, 2.7F, 1.0F, 0.0F, a, true);
            this.box[i] = new float[]{cx - 1.0F, cy - 1.0F, 2.0F, 2.0F};
            break;
         case 10:
            WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
            this.box[i] = wm.drawPanelSwitchForColorCheck(tx, ty + 12.0F, tw, this.tileH - 24.0F);
            this.expected[i] = wm.panelSwitchColorForCheck();
            break;
         case 11: {
            WeaponsModule w = weapons();
            float rw = Math.min(tw - 14.0F, 64.0F);
            float rh = Math.min(22.0F, this.tileH - 30.0F);
            this.box[i] = w == null ? null : w.drawStyleRingForColorCheck(cx - rw / 2.0F, cy - rh / 2.0F, rw, rh, a);
            break;
         }
         case 12: {
            WeaponsModule w = weapons();
            this.box[i] = w == null ? null : w.drawStyleTagForColorCheck(cx - 8.0F, cy - 5.0F, a);
            break;
         }
         case 13: {
            WeaponsModule w = weapons();
            this.box[i] = w == null ? null : w.drawCompareHandleForColorCheck(cx, cy, Math.min(26.0F, this.tileH - 30.0F), a);
            break;
         }
         default: {
            WeaponsModule w = weapons();
            this.box[i] = w == null ? null : w.drawNewPillForColorCheck(cx - 10.0F, cy - 5.0F, a);
         }
      }

      this.result(i, tx, ty + this.tileH - 9.0F, tw);
   }

   private static WeaponsModule weapons() {
      Module m = FuriahsVisuals.instance.getModules().get("weapons");
      return m instanceof WeaponsModule ? (WeaponsModule)m : null;
   }

   private void islandTile(float tx, float ty, float tw) {
      Ui.roundRect(tx, ty, tw, this.islandH, 7.0F, Theme.SURFACE);
      WaterMarkModule wm = FuriahsVisuals.instance.getWaterMark();
      this.box[15] = wm.drawIslandForColorCheck(tx + 7.0F, ty + 14.0F, tw - 14.0F, this.islandH - 27.0F);
      int[] c = wm.fvColorForCheck();
      this.expected[15] = c[0];
      this.islandAccent = c[1] == 1;
      Ui.textMiddle(
         "Island FV  ·  " + (this.islandAccent ? "the island's accent " : "set to another color ") + hex(c[0]),
         tx + 7.0F,
         ty + 9.0F,
         7.4F,
         Theme.TEXT_DIM,
         false
      );
      this.result(15, tx, ty + this.islandH - 9.0F, tw);
   }

   private void result(int i, float tx, float ry, float tw) {
      if (this.diff[i] < 0) {
         Ui.textMiddle("…", tx + 7.0F, ry, 7.2F, Theme.TEXT_MUTED, false);
      } else {
         boolean ok = this.diff[i] <= 2;
         String s = (ok ? "✓ " : "× ") + hex(this.got[i]) + (this.diff[i] == 0 ? "" : "  " + this.diff[i] + " off");
         Ui.textMiddle(Ui.ellipsize(s, 7.2F, tw - 14.0F, true), tx + 7.0F, ry, 7.2F, ok ? -12723068 : Theme.OFF, true);
      }
   }

   private void measure() {
      this.measured = true;
      int off = 0;
      StringBuilder bad = new StringBuilder();

      for (int i = 0; i < NAMES.length; i++) {
         if (this.box[i] == null) {
            this.diff[i] = -1;
         } else {
            this.read(i);
            if (this.diff[i] > 2) {
               off++;
               bad.append(bad.length() == 0 ? "" : ", ")
                  .append(NAMES[i])
                  .append(' ')
                  .append(hex(this.got[i]))
                  .append(" (")
                  .append(this.diff[i])
                  .append(" off)");
            }

            FuriahsVisuals.LOG
               .info("GUI colors: {} expected {}, read {} ({} off)", new Object[]{NAMES[i], hex(this.expected[i]), hex(this.got[i]), this.diff[i]});
         }
      }

      lastOff = off;
      String theme = FuriahsVisuals.instance.getTheme().get().displayName();
      lastSummary = String.format(
         Locale.ROOT,
         "%d of %d parts within ±%d of %s (%s, GUI %.2f px/unit)%s",
         NAMES.length - off,
         NAMES.length,
         2,
         hex(this.expected[0]),
         theme,
         Ui.px,
         off == 0 ? "" : ": off: " + bad
      );
      FuriahsVisuals.LOG.info("GUI colors: {}", new Object[]{lastSummary});
      FuriahsVisuals.chat("GUI colors: " + lastSummary);
   }

   private void read(int i) {
      Minecraft mc = Minecraft.getMinecraft();
      float[] b = this.box[i];
      int x0 = clamp((int)Math.floor(b[0] * Ui.px), 0, mc.displayWidth - 1);
      int x1 = clamp((int)Math.ceil((b[0] + b[2]) * Ui.px), x0 + 1, mc.displayWidth);
      int y0 = clamp((int)Math.floor((b[1] + Ui.offsetY) * Ui.px), 0, mc.displayHeight - 1);
      int y1 = clamp((int)Math.ceil((b[1] + b[3] + Ui.offsetY) * Ui.px), y0 + 1, mc.displayHeight);
      int pw = x1 - x0;
      int ph = y1 - y0;
      ByteBuffer pixels = BufferUtils.createByteBuffer(pw * ph * 4);
      int packBuffer = GL11.glGetInteger(35053);
      if (packBuffer != 0) {
         GL15.glBindBuffer(35051, 0);
      }

      GL11.glReadPixels(x0, mc.displayHeight - y1, pw, ph, 6408, 5121, pixels);
      if (packBuffer != 0) {
         GL15.glBindBuffer(35051, packBuffer);
      }

      int e = this.expected[i];
      int er = e >> 16 & 0xFF;
      int eg = e >> 8 & 0xFF;
      int eb = e & 0xFF;
      int best = Integer.MAX_VALUE;
      int bestColor = 0;

      for (int p = 0; p < pw * ph; p++) {
         int r = pixels.get(p * 4) & 255;
         int g = pixels.get(p * 4 + 1) & 255;
         int bl = pixels.get(p * 4 + 2) & 255;
         int d = Math.max(Math.abs(r - er), Math.max(Math.abs(g - eg), Math.abs(bl - eb)));
         if (d < best) {
            best = d;
            bestColor = 0xFF000000 | r << 16 | g << 8 | bl;
         }
      }

      this.got[i] = bestColor;
      this.diff[i] = best;
   }

   private static int clamp(int v, int lo, int hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   private static String hex(int color) {
      return String.format(Locale.ROOT, "#%06X", color & 16777215);
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.measured && this.again.contains(mx, my)) {
         this.measured = false;
         this.stable = 0;
         Arrays.fill(this.diff, -1);
         lastOff = -1;
         ModSounds.gui(Sfx.CLICK);
      }

      return this.contains(mx, my);
   }

   @Override
   public String tooltip(float mx, float my) {
      return this.again.contains(mx, my) ? "Reads every part again (after changing the theme or the accent)" : null;
   }

   public static void againForTest() {
      if (Popovers.current() instanceof GuiColors) {
         GuiColors g = (GuiColors)Popovers.current();
         g.measured = false;
         g.stable = 0;
         Arrays.fill(g.diff, -1);
         lastOff = -1;
      }
   }

   public static boolean isOpen() {
      return Popovers.current() instanceof GuiColors;
   }
}
