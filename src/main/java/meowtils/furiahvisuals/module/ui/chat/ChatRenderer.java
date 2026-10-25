package meowtils.furiahvisuals.module.ui.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.hud.HudFont;
import meowtils.furiahvisuals.hud.HudLook;
import meowtils.furiahvisuals.hud.HudPanel;
import meowtils.furiahvisuals.module.ui.nametag.NametagElement;
import meowtils.furiahvisuals.module.watermark.Spring;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

final class ChatRenderer {
   static final int PANEL = 0;
   static final int BUBBLES = 1;
   static final int NONE = 2;
   private final List<ChatRenderer.Message> messages = new ArrayList<>();
   private final List<ChatRenderer.Message> next = new ArrayList<>();
   private final Map<ChatLine, ChatRenderer.Message> known = new IdentityHashMap<>();
   private int seenSize = -1;
   private ChatLine seenHead;
   private ChatLine seenTail;
   private boolean seenStack;
   private boolean seenMentions;
   private String seenName = "";
   private boolean initial = true;
   private final long[] arrivals = new long[6];
   private int arrivalAt;
   private int layoutVersion;
   private float layoutWidth = -1.0F;
   private float layoutPx = -1.0F;
   private boolean layoutMod;
   private boolean layoutStamps;
   private boolean layoutColors = true;
   private boolean layoutTwelve;
   private float stampW;
   private float scrollTarget;
   private final Spring scroll = new Spring();
   private int unseen;
   private float pillShown;
   private float openT;
   private boolean wasOpen;
   float lineH = 9.0F;
   float contentH;
   float viewOpen;
   float viewClosed;
   float textX;
   float textW;
   float panelX;
   float panelY;
   float panelW;
   float panelH;
   float panelA;
   float barX;
   float barY;
   float barW;
   float barH;
   float trackY;
   float trackH;
   boolean barShown;
   float pillX;
   float pillY;
   float pillW;
   float pillH;
   private int hitCount;
   private float[] hitTop = new float[48];
   private float[] hitX = new float[48];
   private ChatWrap.Line[] hitLine = new ChatWrap.Line[48];
   private int drawCount;
   private ChatRenderer.Message[] drawMsg = new ChatRenderer.Message[48];
   private int[] drawIndex = new int[48];
   private float[] drawTop = new float[48];
   private float[] drawVis = new float[48];
   private boolean snapPositions;
   private List<ChatRenderer.Message> keep;
   private static final float BUBBLE_PAD = 1.5F;
   private static final float BUBBLE_GAP = 2.0F;
   private static final float PANEL_PAD = 2.0F;
   private boolean moving;
   private boolean barHover;
   private boolean barDrag;
   private float barShownT;

   void sync(List<ChatLine> source, ChatRenderer.Style st, int ticks, long nowMs, long clockMs) {
      int n = source.size();
      ChatLine head = n > 0 ? source.get(0) : null;
      ChatLine tail = n > 0 ? source.get(n - 1) : null;
      String name = st.myName == null ? "" : st.myName;
      if (n != this.seenSize
         || head != this.seenHead
         || tail != this.seenTail
         || st.stack != this.seenStack
         || st.mentions != this.seenMentions
         || !name.equals(this.seenName)) {
         boolean rebuild = st.stack != this.seenStack || st.mentions != this.seenMentions || !name.equals(this.seenName);
         this.seenSize = n;
         this.seenHead = head;
         this.seenTail = tail;
         this.seenStack = st.stack;
         this.seenMentions = st.mentions;
         this.seenName = name;
         if (rebuild) {
            this.messages.clear();
            this.initial = true;
         }

         this.known.clear();
         ChatRenderer.Message oldNewest = this.messages.isEmpty() ? null : this.messages.get(0);

         for (ChatRenderer.Message m : this.messages) {
            m.seen = false;

            for (ChatLine l : m.sources) {
               this.known.put(l, m);
            }
         }

         this.next.clear();
         ArrayList<ChatRenderer.Message> fresh = null;
         ChatRenderer.Message prev = null;
         int addedLive = 0;

         for (int i = n - 1; i >= 0; i--) {
            ChatLine l = source.get(i);
            ChatRenderer.Message m = this.known.get(l);
            if (m != null && m == prev) {
               m.sources.add(l);
            } else if (m != null && !m.seen) {
               m.seen = true;
               m.sources.clear();
               m.sources.add(l);
               this.next.add(m);
               prev = m;
            } else if (st.stack && prev != null && prev.id == 0 && l.getChatLineID() == 0 && prev.key().equals(l.getChatComponent().getFormattedText())) {
               prev.sources.add(l);
               prev.counter = Math.max(prev.counter, l.getUpdatedCounter());
               prev.time = timeOf(l, ticks, nowMs);
               if (!this.initial) {
                  prev.pop = 0.0F;
               }
            } else {
               m = new ChatRenderer.Message(l);
               m.seen = true;
               m.time = timeOf(l, ticks, nowMs);
               boolean bl = m.mention = st.mentions && mentions(m.component.getUnformattedText(), name);
               if (!this.initial) {
                  m.appear = 0.0F;
                  addedLive++;
                  if (fresh == null) {
                     fresh = new ArrayList<>(4);
                  }

                  fresh.add(m);
               }

               this.next.add(m);
               prev = m;
            }
         }

         this.messages.clear();

         for (int var22 = this.next.size() - 1; var22 >= 0; var22--) {
            ChatRenderer.Message m = this.next.get(var22);
            m.count = m.sources.size();
            this.messages.add(m);
         }

         this.next.clear();
         if (fresh != null) {
            boolean burst = this.noteArrivals(fresh.size(), clockMs);

            for (ChatRenderer.Message m : fresh) {
               if (burst) {
                  m.appear = 1.0F;
               }

               if (m.id != 0 && oldNewest != null && oldNewest.id == m.id && !oldNewest.seen && this.messages.get(0) == m && oldNewest.lines != null) {
                  m.oldLines = oldNewest.lines;
                  m.replace = 0.0F;
                  m.appear = 1.0F;
                  m.y.snap(oldNewest.y.value);
               }
            }

            if (burst) {
               this.snapPositions = true;
            }

            if (this.scrollTarget > 0.5F) {
               this.keep = fresh;
               this.snapPositions = true;
               this.unseen += addedLive;
            }
         }

         this.initial = false;
      }
   }

   private boolean noteArrivals(int count, long nowMs) {
      for (int i = 0; i < count; i++) {
         this.arrivals[this.arrivalAt] = nowMs;
         this.arrivalAt = (this.arrivalAt + 1) % this.arrivals.length;
      }

      long oldest = this.arrivals[this.arrivalAt];
      return oldest != 0L && nowMs - oldest < 250L;
   }

   private static long timeOf(ChatLine l, int ticks, long nowMs) {
      return nowMs - Math.max(0, ticks - l.getUpdatedCounter()) * 50L;
   }

   static boolean mentions(String text, String name) {
      if (name != null && name.length() >= 2) {
         String plain = EnumChatFormatting.getTextWithoutFormattingCodes(text);
         String lower = plain.toLowerCase(Locale.ROOT);
         String n = name.toLowerCase(Locale.ROOT);

         for (int at = lower.indexOf(n); at >= 0; at = lower.indexOf(n, at + 1)) {
            int end = at + n.length();
            boolean wordStart = at == 0 || !isNameChar(lower.charAt(at - 1));
            boolean wordEnd = end >= lower.length() || !isNameChar(lower.charAt(end));
            if (wordStart && wordEnd) {
               int after = end < lower.length() ? lower.charAt(end) : 32;
               if (after != 62 && after != 58 && after != 187) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean isNameChar(char c) {
      return Character.isLetterOrDigit(c) || c == '_';
   }

   void clear() {
      this.messages.clear();
      this.seenSize = -1;
      this.seenTail = null;
      this.seenHead = null;
      this.initial = true;
      this.scrollTarget = 0.0F;
      this.scroll.snap(0.0F);
      this.unseen = 0;
      this.openT = 0.0F;
   }

   void scrollBy(int lines) {
      this.scrollTarget = Math.max(0.0F, this.scrollTarget + lines * this.lineH);
      this.clampScroll();
   }

   void scrollToBottom() {
      this.scrollTarget = 0.0F;
      this.unseen = 0;
   }

   void scrollToFraction(float t) {
      this.scrollTarget = Easing.clamp01(t) * Math.max(0.0F, this.contentH - this.viewOpen);
      this.scroll.snap(this.scrollTarget);
      if (this.scrollTarget < 0.5F) {
         this.unseen = 0;
      }
   }

   private void clampScroll() {
      float max = Math.max(0.0F, this.contentH - this.viewOpen);
      this.scrollTarget = Math.max(0.0F, Math.min(max, this.scrollTarget));
      if (this.scrollTarget < 0.5F) {
         this.unseen = 0;
      }
   }

   float scrollTarget() {
      return this.scrollTarget;
   }

   private static FontRenderer font() {
      return Minecraft.getMinecraft().fontRendererObj;
   }

   private static float width(ChatRenderer.Style st, String s) {
      return st.modFont ? HudFont.width(s) : font().getStringWidth(s);
   }

   private void layout(ChatRenderer.Style st, ChatRenderer.Frame f) {
      float pad = padX(st);
      float stamp = st.timestamps ? width(st, "00:00") + 4.0F : 0.0F;
      float full = (float)Math.floor(f.chatWidth / f.scale);
      float w = (pad > 0.0F ? full + 4.0F - 2.0F * pad : full) - stamp;
      if (Math.abs(w - this.layoutWidth) > 0.01F
         || st.modFont != this.layoutMod
         || st.modFont && Ui.px != this.layoutPx
         || st.timestamps != this.layoutStamps
         || f.colors != this.layoutColors
         || st.twelveHour != this.layoutTwelve) {
         this.layoutWidth = w;
         this.layoutMod = st.modFont;
         this.layoutPx = Ui.px;
         this.layoutStamps = st.timestamps;
         this.layoutColors = f.colors;
         this.layoutTwelve = st.twelveHour;
         this.layoutVersion++;
      }

      this.stampW = stamp;
      this.textW = w;
      ChatWrap.Measure m = st.modFont ? ChatWrap.MOD : ChatWrap.VANILLA;

      for (ChatRenderer.Message msg : this.messages) {
         if (msg.layoutVersion != this.layoutVersion || msg.lines == null || msg.fittedCount != msg.count) {
            msg.lines = ChatWrap.wrap(msg.component, w, m, f.colors);
            if (msg.count > 1) {
               ChatWrap.Line last = msg.lines.get(msg.lines.size() - 1);
               if (last.width + this.counterWidth(st, msg.count) > w) {
                  msg.lines = ChatWrap.wrap(msg.component, w - this.counterWidth(st, msg.count), m, f.colors);
               }
            }

            msg.widest = 0.0F;

            for (ChatWrap.Line l : msg.lines) {
               msg.widest = Math.max(msg.widest, l.width);
            }

            if (msg.count > 1) {
               msg.widest = Math.max(msg.widest, msg.lines.get(msg.lines.size() - 1).width + this.counterWidth(st, msg.count));
            }

            msg.layoutVersion = this.layoutVersion;
            msg.fittedCount = msg.count;
            msg.stamp = null;
         }

         if (st.timestamps && msg.stamp == null) {
            msg.stamp = stamp(msg.time, st.twelveHour);
         }
      }
   }

   private static String stamp(long time, boolean twelve) {
      Calendar c = Calendar.getInstance();
      c.setTimeInMillis(time);
      int h = c.get(11);
      int min = c.get(12);
      if (twelve) {
         h = h % 12 == 0 ? 12 : h % 12;
      }

      return (twelve ? Integer.toString(h) : (h < 10 ? "0" : "") + h) + ":" + (min < 10 ? "0" : "") + min;
   }

   private float counterWidth(ChatRenderer.Style st, int count) {
      return width(st, "×" + count) + 7.0F;
   }

   private static float padX(ChatRenderer.Style st) {
      return st.background == 2 || !st.themed && st.background != 1 ? 0.0F : 3.0F;
   }

   private float messageHeight(ChatRenderer.Style st, ChatRenderer.Message m) {
      float h = m.lines.size() * this.lineH;
      return st.background == 1 ? h + 3.0F + 2.0F : h;
   }

   static float[] openBox(ChatRenderer.Style st, ChatRenderer.Frame f) {
      float lineH = 9.0F + st.spacing;
      float view = Math.max(1, (int)(f.heightOpen / lineH)) * lineH;
      float w = padX(st) > 0.0F ? (float)Math.floor(f.chatWidth / f.scale) + 4.0F : (float)Math.ceil(f.chatWidth / f.scale) + 4.0F;
      return new float[]{w, view + 2.0F, 2.0F + f.rowFull};
   }

   static float rowTop() {
      return 2.0F;
   }

   static float boxExtra(ChatRenderer.Frame f) {
      return 4.0F + f.rowFull;
   }

   boolean visible() {
      return this.panelA > 0.003F || this.drawCount > 0;
   }

   boolean wantsClip() {
      if (!this.moving && (!(this.openT > 0.0F) || !(this.openT < 1.0F))) {
         for (ChatRenderer.Message m : this.messages) {
            if (m.appear < 1.0F || !m.y.isStarted()) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   void draw(ChatRenderer.Style st, ChatRenderer.Frame f, HudLook look, float dt, float speed, int accent, boolean clip) {
      this.lineH = 9.0F + st.spacing;
      this.layout(st, f);
      boolean instant = speed <= 0.0F;
      float omega = 26.0F * Math.max(0.3F, speed);
      float left = f.left / f.scale;
      float pad = padX(st);
      this.textX = left + pad + this.stampW;
      this.contentH = 0.0F;

      for (ChatRenderer.Message message : this.messages) {
         message.height = this.messageHeight(st, message);
         this.contentH = this.contentH + message.height;
      }

      this.viewOpen = Math.max(1, (int)(f.heightOpen / this.lineH)) * this.lineH;
      this.viewClosed = Math.max(1, (int)(f.heightClosed / this.lineH)) * this.lineH;
      if (!f.open && this.wasOpen) {
         this.scrollToBottom();
      }

      this.wasOpen = f.open;
      if (this.keep != null) {
         float added = 0.0F;

         for (ChatRenderer.Message message : this.keep) {
            if (message.lines != null && this.messages.contains(message)) {
               added += message.height;
               message.appear = 1.0F;
            }
         }

         this.scrollTarget += added;
         this.scroll.snap(this.scroll.value + added);
         this.keep = null;
      }

      this.clampScroll();
      if (!instant && !this.snapPositions) {
         this.scroll.update(this.scrollTarget, omega * 0.8F, dt);
      } else {
         this.scroll.snap(this.scrollTarget);
      }

      float sc = this.scroll.value;
      this.openT = instant ? (f.open ? 1.0F : 0.0F) : Easing.clamp01(this.openT + (f.open ? dt * speed / 0.18F : -dt * speed / 0.15F));
      this.moving = !instant && !this.snapPositions && Math.abs(this.scroll.value - this.scrollTarget) > 0.01F;
      float f2 = 0.0F;

      for (ChatRenderer.Message m : this.messages) {
         if (!m.y.isStarted()) {
            m.y.snap(m.appear < 1.0F ? -m.height : f2);
         }

         if (!instant && !this.snapPositions) {
            m.y.update(f2, omega, dt);
            if (m.y.value != f2) {
               this.moving = true;
            }
         } else {
            m.y.snap(f2);
         }

         f2 += m.height;
         m.appear = instant ? 1.0F : Math.min(1.0F, m.appear + dt * speed / 0.2F);
         m.pop = instant ? 1.0F : Math.min(1.0F, m.pop + dt * speed / 0.32F);
         float f3 = m.replace = instant ? 1.0F : Math.min(1.0F, m.replace + dt * speed / 0.24F);
         if (m.replace >= 1.0F) {
            m.oldLines = null;
         }
      }

      this.snapPositions = false;
      this.drawCount = 0;
      this.hitCount = 0;
      float f4 = 0.0F;
      float maxK = Math.max(1.0F, this.viewOpen / this.lineH - 1.0F);

      for (ChatRenderer.Message m : this.messages) {
         float msgBottom = -m.y.value + sc;
         float topOfMsg = msgBottom - m.height;
         if (!(topOfMsg > this.lineH * 2.0F)) {
            if (msgBottom < -Math.max(this.viewOpen, this.viewClosed) - this.lineH * 2.0F) {
               break;
            }

            float age = f.ticks - m.counter;
            float closedAge = age >= 200.0F ? 0.0F : sq(Easing.clamp01((1.0F - age / 200.0F) * 10.0F));
            float inner = st.background == 1 ? msgBottom - 1.5F : msgBottom;

            for (int li = m.lines.size() - 1; li >= 0; li--) {
               float bottomOfLine = inner - (m.lines.size() - 1 - li) * this.lineH;
               float top = bottomOfLine - this.lineH;
               float openVis = this.edge(top, bottomOfLine, -this.viewOpen, 0.0F);
               float closedVis = closedAge * this.edge(top, bottomOfLine, -this.viewClosed, 0.0F);
               float vis = closedVis
                  + (openVis - closedVis)
                     * Easing.smoothstep(
                        0.0F, 1.0F, Easing.clamp01(this.openT * 1.6F - 0.6F * (Math.max(0.0F, Math.min(maxK, -bottomOfLine / this.lineH)) / maxK))
                     );
               if (!(vis <= 0.003F)) {
                  f4 += this.lineH * vis;
                  this.add(m, li, top, vis);
               }
            }
         }
      }

      boolean onLight = st.themed && st.background != 2 && look.colors.light && look.background && st.bgOpacity > 0.5F;
      int base = !onLight && (!st.themed || st.background == 2 || !look.background || !(st.bgOpacity > 0.5F)) ? -1 : look.colors.text | 0xFF000000;
      float bgAlpha = f.opacity * st.bgOpacity;
      this.panelX = left;
      this.panelW = pad > 0.0F ? this.textW + this.stampW + pad * 2.0F : (float)Math.ceil(f.chatWidth / f.scale) + 4.0F;
      float msgH = f4 > 0.0F ? f4 + 4.0F : 0.0F;
      float row = f.row;
      boolean inPanel = st.background == 0 && st.themed;
      this.panelH = msgH + (inPanel ? row : 0.0F);
      this.panelY = msgH > 0.0F ? 2.0F - msgH : 2.0F;
      this.panelA = Math.min(1.0F, f4 / this.lineH);
      if (inPanel && row > 0.0F) {
         this.panelA = Math.max(this.panelA, f.rowIn);
      }

      float r = Math.min(look.radius, 6.0F);
      if (inPanel && this.panelH > 0.5F) {
         HudPanel.background(this.panelX, this.panelY, this.panelW, this.panelH, r, look, this.panelA * bgAlpha);
         if (row > 0.5F && msgH > 0.0F) {
            float hair = Math.max(1.0F, (float)Math.round(Ui.px * 0.5F)) / Ui.px;
            Ui.rect(this.panelX + 4.0F, 2.0F - hair / 2.0F, this.panelW - 8.0F, hair, ColorUtil.mulAlpha(look.colors.separator, f.rowIn * bgAlpha));
         }
      } else if (row > 0.5F && st.themed) {
         float rr = Math.min(Math.max(r, 3.0F), (row - 1.0F) / 2.0F);
         HudPanel.background(this.panelX, 3.0F, this.panelW, row - 1.0F, rr, look, f.rowIn * Math.max(0.6F, bgAlpha));
         HudPanel.foreground(this.panelX, 3.0F, this.panelW, row - 1.0F, rr, look, f.rowIn * Math.max(0.6F, bgAlpha));
      }

      if (clip) {
         float view = this.openT > 0.0F ? this.viewOpen : this.viewClosed;
         float below = st.background == 1 ? 4.0F : 2.0F;
         float above = view + 2.0F + (st.background == 1 ? 5.0F : 1.0F);
         Ui.pushScissor(this.panelX - 6.0F, -above, this.panelW + 12.0F, above + below);
      }

      for (int i2 = 0; i2 < this.drawCount; i2++) {
         ChatRenderer.Message mx = this.drawMsg[i2];
         int lix = this.drawIndex[i2];
         float vis = this.drawVis[i2];
         float top = this.drawTop[i2];
         float a = vis * Easing.outCubic(mx.appear);
         if (st.background == 0 && !st.themed) {
            Ui.rect(left, top, this.panelW, this.lineH, ColorUtil.mulAlpha(Integer.MIN_VALUE, a * bgAlpha));
         } else if (st.background == 1 && lix == mx.lines.size() - 1) {
            this.drawBubble(st, look, mx, top, a * bgAlpha, left, r);
         }

         if (mx.mention && lix == mx.lines.size() - 1) {
            this.drawMention(st, mx, top + this.lineH, a, left, accent);
         }
      }

      HudFont.setOnLight(onLight);
      if (st.modFont) {
         HudFont.beginBatch();
      }

      boolean anyMoving = false;

      for (int i = 0; i < this.drawCount; i++) {
         ChatRenderer.Message mxx = this.drawMsg[i];
         if (mxx.appear < 1.0F) {
            anyMoving = true;
         } else {
            this.drawLine(st, mxx, this.drawIndex[i], this.drawTop[i], this.drawVis[i] * f.opacity, base, 0.0F);
         }
      }

      if (st.modFont) {
         HudFont.endBatch();
      }

      if (anyMoving) {
         for (int var33 = 0; var33 < this.drawCount; var33++) {
            ChatRenderer.Message mxx = this.drawMsg[var33];
            if (!(mxx.appear >= 1.0F)) {
               float e = Easing.outCubic(mxx.appear);
               float s = 0.96F + 0.04F * e;
               float ay = this.drawTop[var33] + this.lineH;
               GlStateManager.pushMatrix();
               GlStateManager.translate(left, ay, 0.0F);
               GlStateManager.scale(s, s, 1.0F);
               GlStateManager.translate(-left, -ay, 0.0F);
               this.drawLine(st, mxx, this.drawIndex[var33], this.drawTop[var33], this.drawVis[var33] * f.opacity * e, base, -(1.0F - e) * 10.0F);
               GlStateManager.popMatrix();
            }
         }
      }

      for (int var34 = 0; var34 < this.drawCount; var34++) {
         ChatRenderer.Message mxx = this.drawMsg[var34];
         if (mxx.count > 1 && this.drawIndex[var34] == mxx.lines.size() - 1) {
            this.drawCounter(st, mxx, this.drawTop[var34], this.drawVis[var34] * f.opacity, accent);
         }
      }

      HudFont.setOnLight(false);
      if (clip) {
         Ui.popScissor();
      }

      if (st.background == 0 && st.themed && this.panelH > 0.5F) {
         HudPanel.foreground(this.panelX, this.panelY, this.panelW, this.panelH, r, look, this.panelA * bgAlpha);
      }

      this.drawScrollbar(st, look, f, accent, dt, speed);
      this.drawPill(st, look, f, accent, dt, speed);
      if (f.open) {
         for (int var35 = 0; var35 < this.drawCount; var35++) {
            ChatRenderer.Message mxx = this.drawMsg[var35];
            if (!(this.drawVis[var35] * Easing.outCubic(mxx.appear) < 0.5F)
               && !(this.drawTop[var35] < -this.viewOpen - 0.5F)
               && !(this.drawTop[var35] + this.lineH > 0.5F)) {
               if (this.hitCount == this.hitLine.length) {
                  this.hitTop = Arrays.copyOf(this.hitTop, this.hitCount * 2);
                  this.hitX = Arrays.copyOf(this.hitX, this.hitCount * 2);
                  this.hitLine = Arrays.copyOf(this.hitLine, this.hitCount * 2);
               }

               this.hitTop[this.hitCount] = this.drawTop[var35];
               this.hitX[this.hitCount] = this.textX;
               this.hitLine[this.hitCount] = mxx.lines.get(this.drawIndex[var35]);
               this.hitCount++;
            }
         }
      }
   }

   private static float sq(float v) {
      return v * v;
   }

   private float edge(float top, float bottom, float viewTop, float viewBottom) {
      float out = Math.max(viewTop - top, bottom - viewBottom);
      return out <= 0.0F ? 1.0F : Easing.clamp01(1.0F - out / this.lineH);
   }

   private void add(ChatRenderer.Message m, int li, float top, float vis) {
      if (this.drawCount == this.drawMsg.length) {
         int c = this.drawCount * 2;
         this.drawMsg = Arrays.copyOf(this.drawMsg, c);
         this.drawIndex = Arrays.copyOf(this.drawIndex, c);
         this.drawTop = Arrays.copyOf(this.drawTop, c);
         this.drawVis = Arrays.copyOf(this.drawVis, c);
      }

      this.drawMsg[this.drawCount] = m;
      this.drawIndex[this.drawCount] = li;
      this.drawTop[this.drawCount] = top;
      this.drawVis[this.drawCount] = vis;
      this.drawCount++;
   }

   private void drawLine(ChatRenderer.Style st, ChatRenderer.Message m, int li, float top, float alpha, int base, float slide) {
      if (!(alpha <= 0.02F)) {
         float ty = top + (this.lineH - 9.0F) / 2.0F + 1.0F;
         if (st.timestamps && li == 0 && m.stamp != null) {
            text(st, m.stamp, this.textX - this.stampW + slide, ty, ColorUtil.mulAlpha(base, alpha * 0.5F));
         }

         float e = Easing.outCubic(m.replace);
         if (m.oldLines != null && m.replace < 1.0F) {
            int oi = li - (m.lines.size() - m.oldLines.size());
            if (oi >= 0 && oi < m.oldLines.size()) {
               text(st, m.oldLines.get(oi).text, this.textX + slide, ty, ColorUtil.mulAlpha(base, alpha * (1.0F - e)), st.names);
            }

            text(st, m.lines.get(li).text, this.textX + slide, ty, ColorUtil.mulAlpha(base, alpha * e), st.names);
         } else {
            text(st, m.lines.get(li).text, this.textX + slide, ty, ColorUtil.mulAlpha(base, alpha), st.names);
         }
      }
   }

   private static void text(ChatRenderer.Style st, String s, float x, float y, int color) {
      text(st, s, x, y, color, null);
   }

   private static void text(ChatRenderer.Style st, String s, float x, float y, int color, NametagElement names) {
      if (color >>> 24 >= 6) {
         boolean shadow = st.shadow || st.background == 2;
         if (names != null) {
            NametagElement.drawLine(names, st.modFont, s, x, y, color, shadow);
            if (!st.modFont) {
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
            }
         } else {
            if (st.modFont) {
               HudFont.draw(s, x, y, color, shadow);
            } else {
               font().drawString(s, x, y, color, shadow);
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               GlStateManager.enableBlend();
               GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
            }
         }
      }
   }

   private void drawBubble(ChatRenderer.Style st, HudLook look, ChatRenderer.Message m, float lastLineTop, float alpha, float left, float r) {
      if (!(alpha <= 0.003F)) {
         float bw = m.widest + this.stampW + padX(st) * 2.0F;
         float bh = m.lines.size() * this.lineH + 3.0F;
         float bottom = lastLineTop + this.lineH + 1.5F;
         float top = bottom - bh;
         float slide = -(1.0F - Easing.outCubic(m.appear)) * 10.0F;
         float br = Math.min(bh / 2.0F, Math.max(r, 3.0F));
         if (st.themed) {
            HudPanel.background(left + slide, top, bw, bh, br, look, alpha);
            HudPanel.foreground(left + slide, top, bw, bh, br, look, alpha);
         } else {
            Ui.roundRect(left + slide, top, bw, bh, br, ColorUtil.mulAlpha(Integer.MIN_VALUE, alpha));
         }
      }
   }

   private void drawMention(ChatRenderer.Style st, ChatRenderer.Message m, float bottom, float alpha, float left, int accent) {
      float h = m.lines.size() * this.lineH;
      float top = bottom - h;
      float w = st.background == 1 ? m.widest + this.stampW + padX(st) * 2.0F : this.panelW;
      if (st.background == 1) {
         left -= (1.0F - Easing.outCubic(m.appear)) * 10.0F;
      }

      Ui.rect(left, top, w, h, ColorUtil.mulAlpha(accent | 0xFF000000, 0.16F * alpha));
      float bar = Math.max(1.0F, (float)Math.round(1.5F * Ui.px)) / Ui.px;
      Ui.roundRect(left, top + 0.5F, bar, h - 1.0F, bar / 2.0F, ColorUtil.mulAlpha(accent | 0xFF000000, 0.95F * alpha));
   }

   private void drawCounter(ChatRenderer.Style st, ChatRenderer.Message m, float lastTop, float alpha, int accent) {
      ChatWrap.Line last = m.lines.get(m.lines.size() - 1);
      String label = "×" + m.count;
      float tw = width(st, label);
      float h = 8.0F;
      float w = tw + 5.0F;
      float x = this.textX + last.width + 2.0F;
      float y = lastTop + (this.lineH - h) / 2.0F + 0.5F;
      float p = Easing.outBack(m.pop, 2.2F);
      float s = m.pop >= 1.0F ? 1.0F : 1.0F + 0.45F * (1.0F - p);
      GlStateManager.pushMatrix();
      GlStateManager.translate(x + w / 2.0F, y + h / 2.0F, 0.0F);
      GlStateManager.scale(s, s, 1.0F);
      GlStateManager.translate(-(x + w / 2.0F), -(y + h / 2.0F), 0.0F);
      Ui.roundRect(x, y, w, h, h / 2.0F, ColorUtil.mulAlpha(accent | 0xFF000000, 0.28F * alpha));
      text(st, "§r" + label, x + 2.5F, y - 0.5F, ColorUtil.mulAlpha(ColorUtil.lerp(accent | 0xFF000000, -1, 0.55F), alpha));
      GlStateManager.popMatrix();
   }

   private void drawScrollbar(ChatRenderer.Style st, HudLook look, ChatRenderer.Frame f, int accent, float dt, float speed) {
      float max = this.contentH - this.viewOpen;
      boolean want = this.openT > 0.01F && max > 0.5F;
      this.barShownT = speed <= 0.0F ? (want ? 1.0F : 0.0F) : Easing.approach(this.barShownT, want ? 1.0F : 0.0F, 14.0F * speed, dt);
      boolean bl2 = this.barShown = this.barShownT > 0.02F && max > 0.5F;
      if (this.barShown) {
         float a = this.barShownT * this.openT;
         float thumbH = Math.max(8.0F, this.viewOpen * this.viewOpen / this.contentH);
         float t = Easing.clamp01(this.scroll.value / max);
         this.trackY = -this.viewOpen;
         this.trackH = this.viewOpen;
         this.barH = thumbH;
         this.barY = -thumbH - (this.viewOpen - thumbH) * t;
         if (st.themedScrollbar && st.themed) {
            float w = !this.barHover && !this.barDrag ? 2.0F : 3.0F;
            this.barW = 3.0F;
            this.barX = this.panelX + this.panelW - 4.0F;
            int text = look.colors.text | 0xFF000000;
            Ui.roundRect(this.barX + (3.0F - w) / 2.0F, this.trackY, w, this.trackH, w / 2.0F, ColorUtil.mulAlpha(text, 0.07F * a));
            int c = !this.barHover && !this.barDrag ? text : ColorUtil.lerp(text, accent | 0xFF000000, 0.6F);
            Ui.roundRect(
               this.barX + (3.0F - w) / 2.0F, this.barY, w, this.barH, w / 2.0F, ColorUtil.mulAlpha(c, (!this.barHover && !this.barDrag ? 0.45F : 0.85F) * a)
            );
         } else {
            this.barX = this.panelX - 3.0F;
            this.barW = 2.0F;
            int strength = this.scroll.value > 0.5F ? 170 : 96;
            int color = this.unseen > 0 ? 13382451 : 3355562;
            Ui.rect(this.barX, this.barY, 2.0F, this.barH, ColorUtil.mulAlpha(strength << 24 | color, a));
            Ui.rect(this.barX + 1.0F, this.barY, 1.0F, this.barH, ColorUtil.mulAlpha(strength << 24 | 13421772, a));
         }
      }
   }

   private void drawPill(ChatRenderer.Style st, HudLook look, ChatRenderer.Frame f, int accent, float dt, float speed) {
      boolean want = st.newPill && f.open && this.unseen > 0 && this.scrollTarget > 0.5F;
      this.pillShown = speed <= 0.0F ? (want ? 1.0F : 0.0F) : Easing.approach(this.pillShown, want ? 1.0F : 0.0F, 16.0F * speed, dt);
      if (this.pillShown <= 0.02F) {
         this.pillW = 0.0F;
      } else {
         String label = this.unseen == 1 ? "1 new message" : this.unseen + " new messages";
         float tw = width(st, label);
         this.pillH = 10.0F;
         this.pillW = tw + 16.0F;
         this.pillX = this.panelX + (this.panelW - this.pillW) / 2.0F;
         float e = Easing.outCubic(this.pillShown);
         this.pillY = -this.pillH - 3.0F + (1.0F - e) * 4.0F;
         Ui.shadow(this.pillX, this.pillY + 1.0F, this.pillW, this.pillH, this.pillH / 2.0F, 4.0F, ColorUtil.mulAlpha(1711276032, e));
         Ui.roundRect(this.pillX, this.pillY, this.pillW, this.pillH, this.pillH / 2.0F, ColorUtil.mulAlpha(accent | 0xFF000000, 0.92F * e));
         float ax = this.pillX + 6.5F;
         float ay = this.pillY + this.pillH / 2.0F;
         float lw = Math.max(1.0F, (float)Math.round(0.9F * Ui.px)) / Ui.px;
         Ui.line(ax, ay - 2.4F, ax, ay + 2.0F, lw, true, ColorUtil.mulAlpha(-1, e));
         Ui.line(ax - 1.9F, ay + 0.3F, ax, ay + 2.2F, lw, true, ColorUtil.mulAlpha(-1, e));
         Ui.line(ax + 1.9F, ay + 0.3F, ax, ay + 2.2F, lw, true, ColorUtil.mulAlpha(-1, e));
         text(st, "§r" + label, this.pillX + 11.0F, this.pillY + 1.0F, ColorUtil.mulAlpha(-1, e));
      }
   }

   IChatComponent partAt(float x, float y) {
      if (this.pillW > 0.0F && this.pillShown > 0.5F && x >= this.pillX && x < this.pillX + this.pillW && y >= this.pillY && y < this.pillY + this.pillH) {
         return null;
      } else {
         for (int i = 0; i < this.hitCount; i++) {
            if (y >= this.hitTop[i] && y < this.hitTop[i] + this.lineH) {
               float lx = x - this.hitX[i];
               return lx < 0.0F ? null : this.hitLine[i].partAt(lx);
            }
         }

         return null;
      }
   }

   float[] findForTest(String clickValue) {
      for (int i = 0; i < this.hitCount; i++) {
         ChatWrap.Line l = this.hitLine[i];

         for (int p = 0; p < l.parts.length; p++) {
            ClickEvent c = l.parts[p].getChatStyle().getChatClickEvent();
            if (c != null && clickValue.equals(c.getValue())) {
               return new float[]{this.hitX[i] + (l.x0[p] + l.x1[p]) / 2.0F, this.hitTop[i] + this.lineH / 2.0F};
            }
         }
      }

      return null;
   }

   boolean overPill(float x, float y) {
      return this.pillW > 0.0F && this.pillShown > 0.5F && x >= this.pillX && x < this.pillX + this.pillW && y >= this.pillY && y < this.pillY + this.pillH;
   }

   int overBar(float x, float y) {
      if (!this.barShown || !(this.barX - 2.0F <= x) || !(x < this.barX + this.barW + 2.0F)) {
         return 0;
      } else if (y >= this.barY && y < this.barY + this.barH) {
         return 1;
      } else {
         return y >= this.trackY && y < this.trackY + this.trackH ? 2 : 0;
      }
   }

   void barState(boolean hover, boolean drag) {
      this.barHover = hover;
      this.barDrag = drag;
   }

   float fractionForThumbTop(float thumbTop) {
      float room = this.viewOpen - this.barH;
      return room <= 0.0F ? 0.0F : Easing.clamp01((-this.barH - thumbTop) / room);
   }

   float openAmount() {
      return this.openT;
   }

   int messageCount() {
      return this.messages.size();
   }

   int unseen() {
      return this.unseen;
   }

   float scrollValue() {
      return this.scroll.value;
   }

   String describeForTest() {
      int stacked = 0;
      int maxCount = 0;
      int mentions = 0;
      int lines = 0;

      for (ChatRenderer.Message m : this.messages) {
         if (m.count > 1) {
            stacked++;
            maxCount = Math.max(maxCount, m.count);
         }

         if (m.mention) {
            mentions++;
         }

         lines += m.lines == null ? 0 : m.lines.size();
      }

      return this.messages.size()
         + " messages in "
         + lines
         + " lines, "
         + stacked
         + " stacked (most ×"
         + maxCount
         + "), "
         + mentions
         + " mentions, "
         + this.drawCount
         + " lines drawn, "
         + this.hitCount
         + " clickable, scroll "
         + String.format("%.1f/%.1f", this.scroll.value, this.scrollTarget)
         + ", unseen "
         + this.unseen
         + ", open "
         + String.format("%.2f", this.openT);
   }

   static final class Frame {
      float left = 2.0F;
      float bottom;
      float scale = 1.0F;
      float chatWidth = 320.0F;
      float heightOpen = 180.0F;
      float heightClosed = 90.0F;
      boolean open;
      float ticks;
      float opacity = 1.0F;
      boolean colors = true;
      float row;
      float rowFull;
      float rowIn;
   }

   private static final class Message {
      final List<ChatLine> sources = new ArrayList<>(2);
      final IChatComponent component;
      final int id;
      int counter;
      long time;
      String key;
      int count = 1;
      boolean mention;
      String stamp;
      List<ChatWrap.Line> lines;
      int layoutVersion = -1;
      int fittedCount;
      float height;
      float widest;
      final Spring y = new Spring();
      float appear = 1.0F;
      float pop = 1.0F;
      List<ChatWrap.Line> oldLines;
      float replace = 1.0F;
      boolean seen;

      Message(ChatLine source) {
         this.sources.add(source);
         this.component = source.getChatComponent();
         this.id = source.getChatLineID();
         this.counter = source.getUpdatedCounter();
      }

      String key() {
         if (this.key == null) {
            this.key = this.component.getFormattedText();
         }

         return this.key;
      }
   }

   static final class Style {
      boolean themed = true;
      boolean modFont = true;
      boolean shadow = true;
      boolean themedScrollbar = true;
      int background = 0;
      float spacing = 1.0F;
      float bgOpacity = 1.0F;
      boolean timestamps;
      boolean twelveHour;
      boolean stack;
      boolean mentions;
      boolean newPill;
      String myName = "";
      NametagElement names;
   }
}
