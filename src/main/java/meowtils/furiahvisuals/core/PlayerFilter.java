package meowtils.furiahvisuals.core;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EnumChatFormatting;

public final class PlayerFilter {
   private static final String COLOR_CODES = "0123456789abcdef";
   private static final int[] PALETTE = new int[]{
      0, 170, 43520, 43690, 11141120, 11141290, 16755200, 11184810, 5592405, 5592575, 5635925, 5636095, 16733525, 16733695, 16777045, 16777215
   };

   private PlayerFilter() {
   }

   public static boolean isNpc(EntityPlayer p) {
      UUID id = p.getUniqueID();
      return id != null && id.version() == 2;
   }

   public static boolean isTeammate(Minecraft mc, EntityPlayer other) {
      if (mc.thePlayer == null) {
         return false;
      } else if (mc.thePlayer.isOnSameTeam(other)) {
         return true;
      } else {
         char mine = nameColor(mc.thePlayer);
         return mine != 0 && mine == nameColor(other);
      }
   }

   public static int teamColor(EntityPlayer p) {
      Team t = p.getTeam();
      if (!(t instanceof ScorePlayerTeam)) {
         return 0;
      } else {
         ScorePlayerTeam team = (ScorePlayerTeam)t;
         EnumChatFormatting format = team.getChatFormat();
         if (format != null && format.isColor() && format.getColorIndex() >= 0 && format.getColorIndex() < PALETTE.length) {
            return 0xFF000000 | PALETTE[format.getColorIndex()];
         } else {
            String prefix = team.getColorPrefix();
            int color = 0;

            for (int i = 0; prefix != null && i + 1 < prefix.length(); i++) {
               if (prefix.charAt(i) == 167) {
                  char code = Character.toLowerCase(prefix.charAt(i + 1));
                  if ("0123456789abcdef".indexOf(code) >= 0) {
                     color = code;
                  } else if (code == 'r') {
                     color = 0;
                  }
               }
            }

            return color == 0 ? 0 : 0xFF000000 | PALETTE["0123456789abcdef".indexOf(color)];
         }
      }
   }

   private static char nameColor(EntityPlayer p) {
      String formatted = p.getDisplayName().getFormattedText();
      int nameStart = formatted.indexOf(p.getName());
      int limit = nameStart < 0 ? formatted.length() : nameStart;
      char color = 0;

      for (int i = 0; i + 1 < limit; i++) {
         if (formatted.charAt(i) == 167) {
            char code = Character.toLowerCase(formatted.charAt(i + 1));
            if ("0123456789abcdef".indexOf(code) >= 0) {
               color = code;
            } else if (code == 'r') {
               color = 0;
            }
         }
      }

      return color;
   }
}
