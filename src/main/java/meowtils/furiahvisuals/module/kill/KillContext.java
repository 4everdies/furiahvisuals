package meowtils.furiahvisuals.module.kill;

import meowtils.furiahvisuals.render.BodyProxy;

public final class KillContext {
   public final BodyProxy.Snapshot body = new BodyProxy.Snapshot();
   public double feetX;
   public double feetY;
   public double feetZ;
   public double impactX;
   public double impactY;
   public double impactZ;
   public double dirX;
   public double dirY;
   public double dirZ = 1.0;
   public double swipeX = 1.0;
   public double swipeY;
   public double swipeZ;
   public boolean crit;
   public boolean mine;
   public double groundY;
   public String victimName = "";
   public String method = "death";
   public long time;
   public long hitTime;

   public double midX() {
      return this.feetX;
   }

   public double midY() {
      return this.feetY + 1.0;
   }

   public double midZ() {
      return this.feetZ;
   }
}
