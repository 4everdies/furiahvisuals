package meowtils.furiahvisuals.module.ui.chat;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.hud.HudFont;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

final class ChatWrap {
   static final ChatWrap.Measure MOD = new ChatWrap.Measure() {
      @Override
      public float width(String formatted) {
         return HudFont.width(formatted);
      }

      @Override
      public int fit(String formatted, float room) {
         return HudFont.trimToWidth(formatted, room).length();
      }
   };
   static final ChatWrap.Measure VANILLA = new ChatWrap.Measure() {
      @Override
      public float width(String formatted) {
         return ChatWrap.font().getStringWidth(formatted);
      }

      @Override
      public int fit(String formatted, float room) {
         return ChatWrap.font().trimStringToWidth(formatted, (int)Math.floor(room + 0.001F)).length();
      }
   };

   private static FontRenderer font() {
      return Minecraft.getMinecraft().fontRendererObj;
   }

   private ChatWrap() {
   }

   static List<ChatWrap.Line> wrap(IChatComponent message, float maxWidth, ChatWrap.Measure m, boolean colors) {
      ArrayList<ChatWrap.Line> out = new ArrayList<>();
      ChatWrap.Builder line = new ChatWrap.Builder();
      boolean wrapped = false;
      maxWidth = Math.max(8.0F, maxWidth);

      for (IChatComponent part : message) {
         String raw = part.getUnformattedTextForChat();
         if (!raw.isEmpty()) {
            String style = colors ? part.getChatStyle().getFormattingCode() : "";
            String[] chunks = raw.split("\n", -1);

            for (int c = 0; c < chunks.length; c++) {
               if (c > 0) {
                  out.add(line.build());
                  line = new ChatWrap.Builder();
                  wrapped = false;
               }

               String text = colors ? chunks[c] : EnumChatFormatting.getTextWithoutFormattingCodes(chunks[c]);
               String prefix = style;

               while (!text.isEmpty()) {
                  if (line.width == 0.0F && wrapped) {
                     int s = 0;

                     while (s < text.length() && text.charAt(s) == ' ') {
                        s++;
                     }

                     if ((text = text.substring(s)).isEmpty()) {
                        break;
                     }
                  }

                  String full = prefix + text;
                  float room = maxWidth - line.width;
                  float fw = m.width(full);
                  if (fw <= room + 0.001F) {
                     line.add(part, full, fw);
                     break;
                  }

                  int fit = Math.max(0, m.fit(full, room) - prefix.length());
                  fit = Math.min(fit, text.length());
                  int space = text.lastIndexOf(32, fit);
                  String head = null;
                  String tail = null;
                  if (fit < text.length() && text.charAt(fit) == ' ') {
                     head = text.substring(0, fit);
                     tail = text.substring(fit + 1);
                  } else if (space > 0) {
                     head = text.substring(0, space);
                     tail = text.substring(space + 1);
                  }

                  if (head != null && line.width == 0.0F) {
                     StringBuilder stringBuilder = new StringBuilder();
                     if (m.width(stringBuilder.append(prefix).append(head).toString()) <= 0.0F) {
                        head = null;
                     }
                  }

                  if (head == null && line.width > 0.0F) {
                     out.add(line.build());
                     line = new ChatWrap.Builder();
                     wrapped = true;
                  } else {
                     if (head == null) {
                        int cut = Math.max(1, fit);
                        if (cut < text.length() && text.charAt(cut - 1) == 167) {
                           cut = cut > 1 ? cut - 1 : Math.min(text.length(), cut + 1);
                        }

                        head = text.substring(0, cut);
                        tail = text.substring(cut);
                     }

                     String headFull = prefix + head;
                     line.add(part, headFull, m.width(headFull));
                     out.add(line.build());
                     line = new ChatWrap.Builder();
                     wrapped = true;
                     prefix = FontRenderer.getFormatFromString(headFull);
                     text = tail;
                  }
               }
            }
         }
      }

      if (line.width > 0.0F || out.isEmpty() || line.text.length() > 0) {
         out.add(line.build());
      }

      return out;
   }

   private static final class Builder {
      final StringBuilder text = new StringBuilder();
      final List<IChatComponent> parts = new ArrayList<>();
      final List<float[]> spans = new ArrayList<>();
      float width;

      private Builder() {
      }

      void add(IChatComponent part, String formatted, float w) {
         if (!formatted.isEmpty()) {
            this.text.append(formatted).append("§r");
            if (w > 0.0F) {
               this.parts.add(part);
               this.spans.add(new float[]{this.width, this.width + w});
            }

            this.width += w;
         }
      }

      ChatWrap.Line build() {
         int n = this.parts.size();
         float[] x0 = new float[n];
         float[] x1 = new float[n];

         for (int i = 0; i < n; i++) {
            x0[i] = this.spans.get(i)[0];
            x1[i] = this.spans.get(i)[1];
         }

         return new ChatWrap.Line(this.text.toString(), this.width, this.parts.toArray(new IChatComponent[n]), x0, x1);
      }
   }

   static final class Line {
      final String text;
      final float width;
      final IChatComponent[] parts;
      final float[] x0;
      final float[] x1;

      Line(String text, float width, IChatComponent[] parts, float[] x0, float[] x1) {
         this.text = text;
         this.width = width;
         this.parts = parts;
         this.x0 = x0;
         this.x1 = x1;
      }

      IChatComponent partAt(float x) {
         for (int i = 0; i < this.parts.length; i++) {
            if (x >= this.x0[i] && x < this.x1[i]) {
               return this.parts[i];
            }
         }

         return null;
      }
   }

   interface Measure {
      float width(String var1);

      int fit(String var1, float var2);
   }
}
