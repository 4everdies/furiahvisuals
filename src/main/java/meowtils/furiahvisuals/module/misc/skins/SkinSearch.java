package meowtils.furiahvisuals.module.misc.skins;

import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

final class SkinSearch {
   private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
   private static final Pattern UUID_PLAIN = Pattern.compile("[0-9a-fA-F]{32}");
   private static final Pattern UUID_DASHED = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

   private SkinSearch() {
   }

   static String invalid(String q) {
      String t = q == null ? "" : q.trim();
      if (t.isEmpty()) {
         return "Type a player's name.";
      } else if (!NAME.matcher(t).matches() && !UUID_PLAIN.matcher(t).matches() && !UUID_DASHED.matcher(t).matches()) {
         if (t.length() < 3) {
            return "Names are at least 3 letters long.";
         } else {
            return t.length() > 16 && t.indexOf(45) < 0 ? "Names are at most 16 letters long." : "Names use only letters, digits and underscores.";
         }
      } else {
         return null;
      }
   }

   static final class Result {
      final String query;
      volatile SkinSearch.Status status = SkinSearch.Status.SEARCHING;
      String name;
      UUID id;
      boolean slim;
      byte[] png;
      SkinImage image;
      String message;
      long at;
      SkinTexture texture;
      private ResourceLocation location;

      Result(String query) {
         this.query = query;
      }

      boolean done() {
         return this.status != SkinSearch.Status.SEARCHING;
      }

      ResourceLocation location() {
         if (this.location == null && this.image != null) {
            this.location = new ResourceLocation("furiahsvisuals", "skins/search_" + this.image.hash);
         }

         return this.location;
      }

      ResourceLocation shown() {
         if (this.status == SkinSearch.Status.FOUND) {
            return this.texture != null ? this.location() : null;
         } else {
            return this.status == SkinSearch.Status.DEFAULT_SKIN && this.id != null ? DefaultPlayerSkin.getDefaultSkin(this.id) : null;
         }
      }
   }

   static enum Status {
      SEARCHING,
      FOUND,
      DEFAULT_SKIN,
      NO_PLAYER,
      BUSY,
      OFFLINE,
      ERROR;
   }
}
