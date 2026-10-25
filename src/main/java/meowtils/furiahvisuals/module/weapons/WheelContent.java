package meowtils.furiahvisuals.module.weapons;

import net.minecraft.client.settings.KeyBinding;

public interface WheelContent {
   int size();

   int perPage();

   int current();

   boolean apply(int var1);

   boolean swapBack();

   String name(int var1);

   String detail(int var1);

   String target();

   boolean usable();

   KeyBinding key();

   void pressed();

   void lit(int var1);

   void idle();

   void prepare(int[] var1, int var2, int var3, int var4, int var5, int var6, boolean var7, double var8);

   boolean picture(int var1, float var2, float var3, float var4, float var5);

   boolean bigPicture(int var1, float var2, float var3, float var4, float var5);
}
