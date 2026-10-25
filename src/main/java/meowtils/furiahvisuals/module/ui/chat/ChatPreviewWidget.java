package meowtils.furiahvisuals.module.ui.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.gui.widget.Pill;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.module.ui.RowWidget;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

final class ChatPreviewWidget extends RowWidget {
   private static final float BOX_H = 180.0F;
   private static final String[] TESTS = new String[]{"Open / Close", "New message", "Burst", "Replace", "Repeat", "Scroll up"};
   private static final String[] CHATTER = new String[]{
      "<§aMossyMoth§r> anyone up for a duel?",
      "<§bAuroraSky§r> gg! that was close",
      "§6+25 coins §7(win bonus)",
      "<§eLanternLynx§r> has anyone found the hidden room behind the waterfall on the jungle map yet? I keep falling into the river",
      "<§dQuartzQuill§r> brb, getting snacks",
      "§a✔ §7Your party is ready.",
      "<§9TidalTern§r> who placed all these torches",
      "<§cEmberElk§r> the bridge is done ⚔"
   };
   private final ChatElement element;
   private final ChatRenderer renderer = new ChatRenderer();
   private final HudLook look = new HudLook();
   private final Pill[] pills = new Pill[TESTS.length];
   private final List<ChatLine> lines = new ArrayList<>();
   private final Random random = new Random(5L);
   private final ChatRenderer.Frame frame = new ChatRenderer.Frame();
   private final long start = ClientClock.nanos();
   private boolean open = true;
   private int said;
   private int loading = 20;
   private float testsH = 20.0F;
   private long lastFrame;

   ChatPreviewWidget(ChatElement element) {
      this.element = element;

      for (int i = 0; i < this.pills.length; i++) {
         this.pills[i] = new Pill();
      }

      element.lastPreview = this;
      this.add(welcome(), 0);
      this.add(text("<§aMossyMoth§r> anyone up for a duel?"), 0);
      this.add(text("§6+25 coins §7(win bonus)"), 0);
      this.add(text("<§bAuroraSky§r> gg! that was close"), 0);
      this.add(event(), 0);
      this.add(text("<§eLanternLynx§r> has anyone found the hidden room behind the waterfall on the jungle map yet? I keep falling into the river"), 0);
      this.add(text("<§cNetherNova§r> " + myName() + " nice shot!"), 0);
   }

   private static String myName() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc.thePlayer != null ? mc.thePlayer.getName() : mc.getSession().getUsername();
   }

   private int ticks() {
      return (int)((ClientClock.nanos() - this.start) / 50000000L);
   }

   private void add(IChatComponent c, int id) {
      if (id != 0) {
         for (int i = 0; i < this.lines.size(); i++) {
            if (this.lines.get(i).getChatLineID() == id) {
               this.lines.remove(i);
               break;
            }
         }
      }

      this.lines.add(0, new ChatLine(this.ticks(), c, id));

      while (this.lines.size() > 100) {
         this.lines.remove(this.lines.size() - 1);
      }
   }

   private static IChatComponent text(String s) {
      return new ChatComponentText(s);
   }

   private static IChatComponent welcome() {
      ChatComponentText c = new ChatComponentText("§7[§bServer§7] §fWelcome back! Type ");
      ChatComponentText cmd = new ChatComponentText("/help");
      cmd.setChatStyle(
         new ChatStyle()
            .setColor(EnumChatFormatting.YELLOW)
            .setChatClickEvent(new ClickEvent(Action.SUGGEST_COMMAND, "/help"))
            .setChatHoverEvent(new HoverEvent(net.minecraft.event.HoverEvent.Action.SHOW_TEXT, new ChatComponentText("Click to type /help")))
      );
      c.appendSibling(cmd);
      c.appendSibling(new ChatComponentText("§f for commands."));
      return c;
   }

   private static IChatComponent event() {
      ChatComponentText c = new ChatComponentText("§d§lEVENT §r§fDouble coins this weekend: ");
      ChatComponentText link = new ChatComponentText("details");
      link.setChatStyle(
         new ChatStyle()
            .setColor(EnumChatFormatting.AQUA)
            .setUnderlined(true)
            .setChatClickEvent(new ClickEvent(Action.OPEN_URL, "https://example.com/event"))
            .setChatHoverEvent(new HoverEvent(net.minecraft.event.HoverEvent.Action.SHOW_TEXT, new ChatComponentText("example.com/event")))
      );
      c.appendSibling(link);
      return c;
   }

   @Override
   public float height() {
      return 186.0F + this.testsH + 2.0F;
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      float bx = this.x;
      float by = this.y;
      float bw = this.w;
      float bh = 180.0F;
      Ui.gradientV(bx, by, bw, bh, 8.0F, -8411430, -10843586);
      float menuPx = Ui.px;
      Minecraft mc = Minecraft.getMinecraft();
      float sf = new ScaledResolution(mc).getScaleFactor();
      float size = this.element.scale.getFloat();
      float scale = Math.max(0.05F, mc.gameSettings.chatScale) * size;
      ScaledResolution res = new ScaledResolution(mc);
      float chatW = Math.max(20.0F, Math.min(this.element.width.getFloat() * size, res.getScaledWidth() - 4.0F));
      float wantW = chatW + 10.0F;
      float wantH = 100.0F * scale + 30.0F;
      float fit = Math.min(1.0F, Math.min((bw - 8.0F) * menuPx / (wantW * sf), (bh - 6.0F) * menuPx / (wantH * sf)));
      float unitPx = sf * fit;
      float u = unitPx / menuPx;
      float inputH = 16.0F;
      long now = ClientClock.nanos();
      float realDt = this.lastFrame == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
      this.lastFrame = now;
      this.frame.scale = scale;
      this.frame.left = 2.0F;
      this.frame.bottom = 0.0F;
      this.frame.chatWidth = chatW;
      this.frame.heightOpen = Math.min(this.element.heightOpen.getFloat(), (bh / u - inputH - 14.0F) / scale);
      this.frame.heightClosed = Math.min(this.element.heightClosed.getFloat(), this.frame.heightOpen);
      this.frame.open = this.open;
      this.frame.ticks = (float)(ClientClock.nanos() - this.start) / 5.0E7F;
      this.frame.opacity = mc.gameSettings.chatOpacity * 0.9F + 0.1F;
      this.frame.colors = mc.gameSettings.chatColours;
      boolean inBox = this.element.rowInBox();
      this.frame.rowFull = inBox ? 13.0F / scale : 0.0F;
      this.frame.rowIn = inBox ? this.renderer.openAmount() : 0.0F;
      this.frame.row = this.frame.rowFull * Easing.outCubic(this.frame.rowIn);
      Ui.pushScissor(bx, by, bw, bh);
      float chatBottom = by + bh - inputH * u - 4.0F * u;
      GlStateManager.pushMatrix();
      GlStateManager.translate(bx, chatBottom, 0.0F);
      GlStateManager.scale(u * scale, u * scale, 1.0F);
      float savedOffset = Ui.offsetY;
      Ui.px = unitPx * scale;
      Ui.offsetY = savedOffset * menuPx / Ui.px + chatBottom * menuPx / Ui.px;

      try {
         ChatRenderer.Style st = this.element.style();
         HudLook l = this.element.updateLook(this.look, Ui.px, realDt);
         l.glass = false;
         this.renderer.sync(this.lines, st, this.ticks(), System.currentTimeMillis(), ClientClock.millis());
         this.renderer.draw(st, this.frame, l, realDt, this.element.speed(), l.colors.accent, false);
      } finally {
         Ui.px = menuPx;
         Ui.offsetY = savedOffset;
         GlStateManager.popMatrix();
         restoreMenuState();
      }

      float open = this.renderer.openAmount();
      if (open > 0.01F) {
         GlStateManager.pushMatrix();
         float e = Easing.outCubic(open);
         float fx;
         float fy;
         if (inBox) {
            GlStateManager.translate(bx, chatBottom, 0.0F);
            fx = this.frame.left + 4.0F;
            fy = ChatRenderer.rowTop() * scale + 3.0F + (1.0F - e) * 5.0F;
         } else {
            GlStateManager.translate(bx, by + bh - inputH * u - 1.0F * u, 0.0F);
            fx = 4.0F;
            fy = 4.0F + (1.0F - e) * 5.0F;
         }

         GlStateManager.scale(u, u, 1.0F);
         Ui.px = unitPx;

         try {
            HudLook l = this.element.updateLook(this.look, unitPx, 0.0F);
            l.glass = false;
            if (!inBox) {
               Ui.rect(2.0F, fy - 2.0F, bw / u - 4.0F, 12.0F, ColorUtil.mulAlpha(Integer.MIN_VALUE, e));
            }

            String typed = "gg everyone";
            int alpha = Math.max(4, Math.round(255.0F * e));
            mc.fontRendererObj.drawStringWithShadow(typed, fx, fy, alpha << 24 | 14737632);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            float cx = fx + mc.fontRendererObj.getStringWidth(typed);
            float t = (float)(now - this.start) / 1.0E9F;
            float blink = Easing.smoothstep(0.2F, 0.8F, 0.5F + 0.5F * (float)Math.cos(t * Math.PI * 2.0 / 1.06F));
            if (inBox) {
               float cw = Math.max(1.0F, (float)Math.round(unitPx * 0.5F)) / unitPx;
               Ui.roundRect(cx, fy - 1.0F, cw, 10.0F, cw / 2.0F, ColorUtil.mulAlpha(l.colors.accent | 0xFF000000, blink * e));
            } else if (blink > 0.5F) {
               mc.fontRendererObj.drawStringWithShadow("_", cx, fy, alpha << 24 | 14737632);
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            }
         } finally {
            Ui.px = menuPx;
            GlStateManager.popMatrix();
            restoreMenuState();
         }
      }

      Ui.popScissor();
      if (fit < 0.99F) {
         String label = Math.round(fit * 100.0F) + "% of its size";
         float lw = Ui.width(label, 7.6F) + 12.0F;
         Ui.roundRect(bx + bw - lw - 5.0F, by + 5.0F, lw, 13.0F, 6.5F, -1946157056);
         Ui.textMiddle(label, bx + bw - lw + 1.0F, by + 11.5F, 7.6F, -251658241, false);
      }

      float tx = this.x;
      float ty = this.y + 180.0F + 6.0F;

      for (int i = 0; i < TESTS.length; i++) {
         float tw = Ui.width(TESTS[i], 8.8F) + 20.0F;
         if (tx > this.x && tx + tw > this.x + this.w) {
            tx = this.x;
            ty += 25.0F;
         }

         this.pills[i].layout(tx, ty, TESTS[i]);
         this.pills[i].draw(TESTS[i], mx, my, dt, accent, i == 0 && this.open, true);
         tx += tw + 5.0F;
      }

      this.testsH = ty - (this.y + 180.0F + 6.0F) + 20.0F;
   }

   private static void restoreMenuState() {
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
      GlStateManager.disableAlpha();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
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
            boolean bl = this.open = !this.open;
            if (!this.open) {
               this.renderer.scrollToBottom();
            }
            break;
         case 1:
            this.add(text(CHATTER[this.said++ % CHATTER.length]), 0);
            break;
         case 2:
            for (int k = 0; k < 12; k++) {
               this.add(text("§7[§bServer§7] §fLoading chunk " + (k + 1) + " of 12…"), 0);
            }
            break;
         case 3:
            this.loading = Math.min(100, this.loading + 20 + this.random.nextInt(10));
            this.add(text("§7Loading map… " + (this.loading >= 100 ? "§aDone!" : "§e" + this.loading + "%")), 4242);
            if (this.loading >= 100) {
               this.loading = 0;
            }
            break;
         case 4:
            this.add(text("§cYou can't place blocks here."), 0);
            break;
         case 5:
            if (!this.open) {
               this.open = true;
            }

            this.renderer.scrollBy(3);
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      for (int i = 0; i < TESTS.length; i++) {
         if (this.pills[i].contains(mx, my)) {
            switch (i) {
               case 0:
                  return "Opens or closes the chat (closed, old messages fade on vanilla's timing)";
               case 1:
                  return "Someone says something: it slides in, the rest moves up";
               case 2:
                  return "Twelve messages at once: shown at once, so nothing lags behind";
               case 3:
                  return "A message updated in place: it crossfades";
               case 4:
                  return "The same message again (stacked as ×2, ×3… with Stack Repeats on)";
               default:
                  return "Scrolls up three lines (then new messages show the pill, with New Messages Pill on)";
            }
         }
      }

      return null;
   }
}
