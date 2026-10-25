package meowtils.furiahvisuals.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

final class ModSound extends PositionedSound {
   final boolean menu;

   ModSound(ResourceLocation location, float volume, float pitch, boolean menu) {
      super(location);
      this.menu = menu;
      this.volume = volume;
      this.pitch = pitch;
      this.repeat = false;
      this.repeatDelay = 0;
      this.attenuationType = AttenuationType.NONE;
      Entity view = Minecraft.getMinecraft().getRenderViewEntity();
      if (view != null) {
         this.xPosF = (float)view.posX;
         this.yPosF = (float)(view.posY + view.getEyeHeight());
         this.zPosF = (float)view.posZ;
      }
   }
}
