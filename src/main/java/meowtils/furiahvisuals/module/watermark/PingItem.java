package meowtils.furiahvisuals.module.watermark;

import java.util.function.BooleanSupplier;
import meowtils.furiahvisuals.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

final class PingItem extends IslandItem {
   private static final int GOOD = -10755203;
   private static final int OK = -3611813;
   private static final int SLOW = -19129;
   private static final int BAD = -42406;
   final BooleanSetting qualityColors;
   final ItemLabel label;
   private final RollingText value = new RollingText();
   private int ping = -1;

   PingItem(WaterMarkModule wm) {
      super(wm, "ping", "Ping", "Your ping, from the server's tab list (nothing is sent to measure it).", IslandIcons.Icon.SIGNAL);
      this.icons(IslandIcons.Icon.SIGNAL, new IslandIcons.Icon[]{IslandIcons.Icon.SIGNAL, IslandIcons.Icon.WIFI, IslandIcons.Icon.CHART, IslandIcons.Icon.BOLT})
         .tip(IslandIcons.Icon.SIGNAL, "Signal bars: as many lit as your connection is good");
      BooleanSupplier hasIcon = new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return PingItem.this.iconChoice.get() != null;
         }
      };
      this.qualityColors = this.option(
         new BooleanSetting("ping.qualityColors", "Quality Colors", "The icon turns green, yellow, orange or red with your ping.", true).visibleWhen(hasIcon)
      );
      this.iconTint.visibleWhen(new BooleanSupplier() {
         @Override
         public boolean getAsBoolean() {
            return !PingItem.this.qualityColors.get();
         }
      });
      this.label = new ItemLabel(this, "ping.label", "ping", "ms", true, ItemLabel.Place.AFTER);
   }

   @Override
   boolean available() {
      Minecraft mc = Minecraft.getMinecraft();
      return mc.theWorld != null && !mc.isSingleplayer();
   }

   @Override
   void tick() {
      Minecraft mc = Minecraft.getMinecraft();
      int p = -1;
      NetworkPlayerInfo info;
      if (mc.thePlayer != null && mc.getNetHandler() != null && (info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID())) != null) {
         p = info.getResponseTime();
      }

      if (p != this.ping || this.value.get().isEmpty()) {
         this.ping = p;
         this.value.set(p <= 0 ? "—" : Integer.toString(p), p, this.wm.rolling());
      }
   }

   @Override
   void frame(float dt, IslandDraw d, IslandStyle s) {
      this.value.update(dt);
   }

   @Override
   String panelValue() {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.theWorld == null) {
         return "Shows on a server";
      } else if (mc.isSingleplayer()) {
         return "Hidden in singleplayer";
      } else {
         return this.ping <= 0 ? "Not reported yet" : this.ping + " ms";
      }
   }

   @Override
   String panelDescription() {
      return "Your ping";
   }

   private float iconWidth(IslandStyle s) {
      return this.iconChoice.get() == IslandIcons.Icon.SIGNAL ? s.icon * 0.95F : s.icon;
   }

   @Override
   float iconSpace(IslandStyle s) {
      return this.showsIcon() ? this.iconWidth(s) + s.gap : 0.0F;
   }

   @Override
   float measure(IslandDraw d, IslandStyle s) {
      return this.iconSpace(s) + this.label.width(d, s) + this.value.width(d, false, s.font);
   }

   private int quality() {
      if (this.ping <= 0) {
         return 0;
      } else {
         return this.ping < 80 ? 4 : (this.ping < 150 ? 3 : (this.ping < 250 ? 2 : 1));
      }
   }

   private int color() {
      switch (this.quality()) {
         case 1:
            return -42406;
         case 2:
            return -19129;
         case 3:
            return -3611813;
         case 4:
            return -10755203;
         default:
            return 0;
      }
   }

   @Override
   void draw(IslandDraw d, IslandStyle s, float x, float cy) {
      if (this.showsIcon()) {
         int q = this.quality();
         float w = this.iconWidth(s);
         int lit = q == 0 ? s.muted : (this.qualityColors.get() ? this.color() : this.iconColor(s, x + w / 2.0F));
         if (this.iconChoice.get() == IslandIcons.Icon.SIGNAL) {
            float bw = w / 4.0F * 0.62F;
            float step = w / 4.0F;
            float h = s.icon;

            for (int i = 0; i < 4; i++) {
               float bh = h * (0.34F + 0.22F * i);
               d.rect(x + i * step, cy + h / 2.0F - bh, bw, bh, i < q ? lit : s.muted & 16777215 | 1711276032);
            }
         } else {
            d.icon(this.iconChoice.get(), x, cy - s.icon / 2.0F, s.icon, lit);
         }

         x += w + s.gap;
      }

      if (this.label.before()) {
         x = this.label.draw(d, s, x, cy);
      }

      this.value.draw(d, false, s.font, x, cy, s.text);
      if (this.label.after()) {
         this.label.draw(d, s, x + this.value.width(d, false, s.font), cy);
      }
   }

   @Override
   boolean hit(float mx, float my, float x, float cy, IslandStyle s, IslandItem.Hit hit) {
      String tip = this.ping <= 0
         ? "Ping: not reported yet"
         : (this.ping < 80 ? "Ping: good" : (this.ping < 150 ? "Ping: fine" : (this.ping < 250 ? "Ping: slow" : "Ping: bad")));
      hit.set(this, x - s.pad, cy - s.height / 2.0F, this.measure(this.wm.draw(), s) + s.pad * 2.0F, s.height, 0, tip, false);
      return true;
   }
}
