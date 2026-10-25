package meowtils.furiahvisuals.module.ui.tab;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.module.misc.skins.SkinHooks;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;

final class TabPreviewWidget extends RowWidget {
   private static final float BOX_H = 190.0F;
   private static final String[] TESTS = new String[]{"Open / Close", "Join", "Leave", "Pings", "Scores", "Crowd"};
   private static final String[][] SAMPLE = new String[][]{
      {"§c[RED] §fPixelFox", "23"},
      {"§c[RED] §fNetherNova", "61"},
      {"§c[RED] §fMossyMoth", "140"},
      {"§9[BLUE] §fAuroraSky", "95"},
      {"§9[BLUE] §fCopperCreeper", "210"},
      {"§9[BLUE] §fStoneSkipper", "480"},
      {"§a[GREEN] §fBlockBuilder", "75"},
      {"§e[YELLOW] §fLanternLynx", "160"},
      {"§7DustyDrifter", "300", "spectator"}
   };
   private final TabElement element;
   private final TabRenderer renderer = new TabRenderer();
   private final HudLook look = new HudLook();
   private final Pill[] pills = new Pill[TESTS.length];
   private final List<TabRenderer.Entry> players = new ArrayList<>();
   private final Random random = new Random(11L);
   private boolean open = true;
   private boolean crowd;
   private boolean scores;
   private int joined;
   private float testsH = 20.0F;

   TabPreviewWidget(TabElement element) {
      this.element = element;

      for (int i = 0; i < this.pills.length; i++) {
         this.pills[i] = new Pill();
      }

      this.build();
      element.lastPreview = this;
   }

   private static TabRenderer.Entry player(String name, int ping) {
      String plain = name.replaceAll("§.", "").replace("[RED] ", "").replace("[BLUE] ", "").replace("[GREEN] ", "").replace("[YELLOW] ", "");
      UUID id = UUID.nameUUIDFromBytes(plain.getBytes(StandardCharsets.UTF_8));
      TabRenderer.Entry e = new TabRenderer.Entry(id.toString());
      e.name = name;
      e.ping = ping;
      e.skin = DefaultPlayerSkin.getDefaultSkin(id);
      e.score = 3 + Math.abs(plain.hashCode()) % 20;
      return e;
   }

   private void build() {
      this.players.clear();

      for (String[] s : SAMPLE) {
         TabRenderer.Entry e = player(s[0], Integer.parseInt(s[1]));
         e.spectator = s.length > 2;
         this.players.add(e);
      }

      Minecraft mc = Minecraft.getMinecraft();
      String me = mc.thePlayer != null ? mc.thePlayer.getName() : mc.getSession().getUsername();
      TabRenderer.Entry you = player("§a[GREEN] §f" + me, 31);
      you.me = true;
      you.skin = SkinHooks.ownSkin();
      this.players.add(you);
      if (this.crowd) {
         for (int i = this.players.size(); i < 80; i++) {
            this.players.add(player("§7Player" + (i < 10 ? "0" : "") + i, 20 + this.random.nextInt(400)));
         }
      }

      this.sort();
   }

   private void sort() {
      Collections.sort(this.players, new Comparator<TabRenderer.Entry>() {
         public int compare(TabRenderer.Entry a, TabRenderer.Entry b) {
            if (a.spectator != b.spectator) {
               return a.spectator ? 1 : -1;
            } else {
               return a.name.replaceAll("§.", "").compareTo(b.name.replaceAll("§.", ""));
            }
         }
      });
   }

   private TabRenderer.Frame frame() {
      TabRenderer.Frame f = new TabRenderer.Frame();

      for (TabRenderer.Entry e : this.players) {
         TabRenderer.Entry c = new TabRenderer.Entry(e.key);
         c.name = e.name;
         c.ping = e.ping;
         c.skin = e.skin;
         c.score = e.score;
         c.spectator = e.spectator;
         c.me = e.me;
         f.entries.add(c);
      }

      f.heads = this.element.heads.get() != TabElement.Heads.OFF;
      f.scores = this.scores ? 1 : 0;
      f.header = Arrays.asList("§b§lFURIAH NETWORK", "§7play.example.net §8· §aLobby 3");
      f.footer = Arrays.asList("§7Ranks and more at §bstore.example.net");
      return f;
   }

   @Override
   public float height() {
      return 196.0F + this.testsH + 2.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float bx = this.x;
      float by = this.y;
      float bw = this.w;
      float bh = 190.0F;
      Ui.gradientV(bx, by, bw, bh, 8.0F, -9463592, -11568586);
      float menuPx = Ui.px;
      float unitPx = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
      float fit = 1.0F;
      if (this.renderer.lastH > 0.0F && this.renderer.lastW > 0.0F) {
         fit = Math.min(
            1.0F, Math.min((bh - 14.0F) * menuPx / ((this.renderer.lastH + 12.0F) * unitPx), (bw - 10.0F) * menuPx / (this.renderer.lastW * unitPx))
         );
      }

      float var21;
      float u = (var21 = unitPx * fit) / menuPx;
      Ui.pushScissor(bx, by, bw, bh);
      GlStateManager.pushMatrix();
      GlStateManager.translate(bx, by + 4.0F, 0.0F);
      GlStateManager.scale(u, u, 1.0F);
      Ui.px = var21;

      try {
         HudLook l = this.element.updateLook(this.look, var21, dt);
         l.glass = false;
         float speed = this.element.speed();
         this.renderer.update(this.open ? this.frame() : null, dt, speed);
         this.renderer.draw(this.element.style(l), l, bw / u, Ui.alpha(), dt, speed);
      } finally {
         Ui.px = menuPx;
         GlStateManager.popMatrix();
         Ui.popScissor();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
         GlStateManager.disableAlpha();
         GlStateManager.enableTexture2D();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (fit < 0.99F) {
         String label = Math.round(fit * 100.0F) + "% of its size";
         float lw = Ui.width(label, 7.6F) + 12.0F;
         Ui.roundRect(bx + 5.0F, by + bh - 18.0F, lw, 13.0F, 6.5F, -1946157056);
         Ui.textMiddle(label, bx + 11.0F, by + bh - 11.5F, 7.6F, -251658241, false);
      }

      float tx = this.x;
      float ty = this.y + 190.0F + 6.0F;

      for (int i = 0; i < TESTS.length; i++) {
         float tw = Ui.width(TESTS[i], 8.8F) + 20.0F;
         if (tx > this.x && tx + tw > this.x + this.w) {
            tx = this.x;
            ty += 25.0F;
         }

         this.pills[i].layout(tx, ty, TESTS[i]);
         boolean on = i == 0 && this.open || i == 4 && this.scores || i == 5 && this.crowd;
         this.pills[i].draw(TESTS[i], mx, my, dt, accent, on, true);
         tx += tw + 5.0F;
      }

      this.testsH = ty - (this.y + 190.0F + 6.0F) + 20.0F;
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (button == 0 && this.contains(mx, my)) {
         for (int i = 0; i < TESTS.length; i++) {
            if (this.pills[i].contains(mx, my)) {
               this.test(i);
               ModSounds.gui(Sfx.CLICK);
               return true;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   void test(int i) {
      switch (i) {
         case 0:
            this.open = !this.open;
            break;
         case 1: {
            String[] names = new String[]{"FreshFern", "QuartzQuill", "EmberElk", "TidalTern"};
            TabRenderer.Entry e = player(
               "§d[PINK] §f" + names[this.joined % names.length] + (this.joined >= names.length ? Integer.toString(this.joined) : ""),
               40 + this.random.nextInt(100)
            );
            this.players.add(e);
            this.joined++;
            this.sort();
            break;
         }
         case 2:
            for (int k = this.players.size() - 1; k >= 0; k--) {
               if (!this.players.get(k).me && !this.players.get(k).spectator) {
                  this.players.remove(k);
                  break;
               }
            }
            break;
         case 3: {
            for (TabRenderer.Entry e : this.players) {
               e.ping = Math.max(1, e.ping + this.random.nextInt(161) - 80);
            }
            break;
         }
         case 4:
            if (this.scores) {
               for (TabRenderer.Entry ex : this.players) {
                  ex.score = ex.score + this.random.nextInt(4);
               }
            }

            this.scores = true;
            break;
         case 5:
            this.crowd = !this.crowd;
            this.build();
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      for (int i = 0; i < TESTS.length; i++) {
         if (this.pills[i].contains(mx, my)) {
            switch (i) {
               case 0:
                  return "Opens or closes the list (as holding and letting go of Tab)";
               case 1:
                  return "A player joins: the row slides in, the others make room";
               case 2:
                  return "A player leaves: the row slides out";
               case 3:
                  return "Everyone's ping changes";
               case 4:
                  return "Shows the scores column, then changes the scores";
               default:
                  return "80 players (the most vanilla shows), in four columns";
            }
         }
      }

      return null;
   }
}
