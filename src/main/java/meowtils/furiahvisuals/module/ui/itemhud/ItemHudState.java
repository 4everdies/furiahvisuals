package meowtils.furiahvisuals.module.ui.itemhud;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

final class ItemHudState {
   final ItemStack[] slots = new ItemStack[9];
   int selected;
   int ticks;
   float partial;
   boolean survival;
   boolean spectator;
   float health;
   float maxHealth;
   float absorption;
   boolean poison;
   boolean wither;
   boolean hardcore;
   boolean regen;
   boolean hurtTime;
   int food;
   float saturation;
   boolean hungerEffect;
   int armor;
   boolean underwater;
   int air;
   int level;
   float xp;
   boolean xpShown;
   boolean mounted;
   boolean horse;
   float mountHealth;
   float mountMax;
   float jump;

   void read(Minecraft mc, int updateCounter, float partialTicks) {
      EntityPlayer p = (EntityPlayer)(mc.getRenderViewEntity() instanceof EntityPlayer ? (EntityPlayer)mc.getRenderViewEntity() : mc.thePlayer);
      this.ticks = updateCounter;
      this.partial = partialTicks;
      this.survival = mc.playerController.shouldDrawHUD() && mc.getRenderViewEntity() instanceof EntityPlayer;
      this.spectator = mc.playerController.isSpectator();

      for (int i = 0; i < 9; i++) {
         this.slots[i] = mc.thePlayer.inventory.mainInventory[i];
      }

      this.selected = mc.thePlayer.inventory.currentItem;
      this.health = p.getHealth();
      this.maxHealth = (float)p.getEntityAttribute(SharedMonsterAttributes.maxHealth).getAttributeValue();
      this.absorption = p.getAbsorptionAmount();
      this.poison = p.isPotionActive(Potion.poison);
      this.wither = !this.poison && p.isPotionActive(Potion.wither);
      this.regen = p.isPotionActive(Potion.regeneration);
      this.hardcore = mc.theWorld.getWorldInfo().isHardcoreModeEnabled();
      this.hurtTime = p.hurtResistantTime > 0;
      this.food = mc.thePlayer.getFoodStats().getFoodLevel();
      this.saturation = p.getFoodStats().getSaturationLevel();
      this.hungerEffect = mc.thePlayer.isPotionActive(Potion.hunger);
      this.armor = mc.thePlayer.getTotalArmorValue();
      this.underwater = p.isInsideOfMaterial(Material.water);
      this.air = p.getAir();
      this.level = mc.thePlayer.experienceLevel;
      this.xp = mc.thePlayer.experience;
      this.xpShown = mc.playerController.gameIsSurvivalOrAdventure();
      this.mounted = mc.thePlayer.ridingEntity instanceof EntityLivingBase;
      this.horse = mc.thePlayer.isRidingHorse();
      if (this.mounted) {
         EntityLivingBase m = (EntityLivingBase)mc.thePlayer.ridingEntity;
         this.mountHealth = m.getHealth();
         this.mountMax = m.getMaxHealth();
      }

      this.jump = this.horse ? mc.thePlayer.getHorseJumpPower() : 0.0F;
   }
}
