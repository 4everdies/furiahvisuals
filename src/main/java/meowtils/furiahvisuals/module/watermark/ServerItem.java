package meowtils.furiahvisuals.module.watermark;

import meowtils.furiahvisuals.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

final class ServerItem extends IslandItem {
   final BooleanSetting streamer;
   final ItemLabel label;
   private String text = "";
   private float textW;
   private int measuredPx;
   private String measured;

   ServerItem(WaterMarkModule wm) {
      super(wm, "server", "Server IP", "The server you play on.", IslandIcons.Icon.GLOBE);
      this.icons(IslandIcons.Icon.GLOBE, new IslandIcons.Icon[]{IslandIcons.Icon.GLOBE, IslandIcons.Icon.SERVER, IslandIcons.Icon.WIFI, IslandIcons.Icon.PIN});
      this.label = new ItemLabel(this, "server.label", "server", "IP", false, ItemLabel.Place.BEFORE);
      this.streamer = this.option(
         new BooleanSetting("server.streamer", "Streamer Mode", "Shows the server's name from your server list (or \"Hidden\") instead of its address.", false)
      );
   }

   @Override
   void tick() {
      this.text = label(Minecraft.getMinecraft(), this.streamer.get());
   }

   static String label(Minecraft mc, boolean streamer) {
      if (mc.theWorld == null) {
         return "Menu";
      } else if (mc.isSingleplayer()) {
         return "Singleplayer";
      } else {
         ServerData data = mc.getCurrentServerData();
         if (data == null) {
            return "Multiplayer";
         } else if (data.isOnLAN()) {
            return "LAN";
         } else if (!streamer) {
            return data.serverIP == null ? "Multiplayer" : data.serverIP;
         } else {
            String name = data.serverName;
            return name != null && !name.trim().isEmpty() && !name.equalsIgnoreCase(data.serverIP) && !name.equals("Minecraft Server")
               ? name.trim()
               : "Hidden";
         }
      }
   }

   @Override
   String panelValue() {
      return this.text;
   }

   @Override
   String panelDescription() {
      return "The server's address";
   }

   @Override
   float measure(IslandDraw d, IslandStyle s) {
      int px = d.pixels(s.font);
      if (px != this.measuredPx || !this.text.equals(this.measured)) {
         this.measuredPx = px;
         this.measured = this.text;
         this.textW = d.width(false, s.font, this.text);
      }

      return this.iconSpace(s) + this.label.width(d, s) + this.textW;
   }

   @Override
   void draw(IslandDraw d, IslandStyle s, float x, float cy) {
      x = this.drawIcon(d, s, x, cy);
      if (this.label.before()) {
         x = this.label.draw(d, s, x, cy);
      }

      d.textMiddle(false, s.font, this.text, x, cy, s.text);
      if (this.label.after()) {
         this.label.draw(d, s, x + this.textW, cy);
      }
   }

   @Override
   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      hit.set(
         this,
         x - s.pad,
         cy - s.height / 2.0F,
         this.measure(this.wm.draw(), s) + s.pad * 2.0F,
         s.height,
         0,
         this.streamer.get() ? "Streamer mode: the address is hidden" : "Server address",
         false
      );
      return true;
   }
}
