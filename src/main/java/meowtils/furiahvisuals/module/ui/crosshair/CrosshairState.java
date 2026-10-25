package meowtils.furiahvisuals.module.ui.crosshair;

public final class CrosshairState {
   public float time;
   public float scale = 1.0F;
   public float move;
   public float click;
   public float target;
   public boolean targetPlayer;
   public float bow = -1.0F;
   public float bowFull;
   public float lowHealth;
   public float blocking;

   public void copyFrom(CrosshairState o) {
      this.time = o.time;
      this.scale = o.scale;
      this.move = o.move;
      this.click = o.click;
      this.target = o.target;
      this.targetPlayer = o.targetPlayer;
      this.bow = o.bow;
      this.bowFull = o.bowFull;
      this.lowHealth = o.lowHealth;
      this.blocking = o.blocking;
   }
}
