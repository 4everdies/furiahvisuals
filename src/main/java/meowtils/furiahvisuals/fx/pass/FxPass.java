package meowtils.furiahvisuals.fx.pass;

public enum FxPass {
   SOLID,
   TRANSLUCENT,
   GLOW;

   public static final int ALL = 7;

   public int bit() {
      return 1 << this.ordinal();
   }
}
