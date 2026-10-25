package meowtils.furiahvisuals.hook;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

public final class InputHooks {
   private static final Field READ_BUFFER = Hooks.field(Mouse.class, "readBuffer");
   private static final int EVENT_SIZE = 22;
   private static final int WHEEL_AT = 10;
   private static GuiScreen lastScreen;
   private static boolean screenKnown;
   private static boolean inKey;
   private static boolean failed;

   private InputHooks() {
   }

   static boolean mouseAvailable() {
      return READ_BUFFER != null;
   }

   static void mouse() {
      if (Bridge.active() && READ_BUFFER != null) {
         try {
            ByteBuffer b = (ByteBuffer)READ_BUFFER.get(null);
            if (b == null) {
               return;
            }

            int from = b.position();
            int to = b.limit();
            if ((to - from) % 22 != 0) {
               return;
            }

            for (int at = from; at < to; at += 22) {
               int button = b.get(at);
               boolean state = b.get(at + 1) != 0;
               int wheel = b.getInt(at + 10);
               if ((button >= 0 || wheel != 0) && Bridge.events().onMouse(button, state, wheel)) {
                  b.put(at, (byte)-1);
                  b.put(at + 1, (byte)0);
                  b.putInt(at + 10, 0);
               }
            }
         } catch (Throwable var7) {
            fail(var7);
         }
      }
   }

   static void keyInput() {
      if (!inKey && Bridge.active()) {
         inKey = true;

         try {
            Bridge.events().onKeyInput();
         } catch (Throwable var4) {
            fail(var4);
         } finally {
            inKey = false;
         }
      }
   }

   static void screenHandled(GuiScreen s) {
      screenKnown = true;
      lastScreen = s;
   }

   static void screenReplaced(GuiScreen from, GuiScreen to) {
      if (lastScreen == from) {
         lastScreen = to;
      }
   }

   static void checkScreen() {
      Minecraft mc = Minecraft.getMinecraft();
      GuiScreen s = mc.currentScreen;
      if (!screenKnown) {
         screenKnown = true;
         lastScreen = s;
      } else if (s != lastScreen) {
         lastScreen = s;
         if (Bridge.active()) {
            try {
               if (Bridge.events().onGuiOpen(s) && s != null) {
                  mc.displayGuiScreen(null);
                  lastScreen = mc.currentScreen;
               }
            } catch (Throwable var3) {
               fail(var3);
            }
         }
      }
   }

   static void reset() {
      screenKnown = false;
      lastScreen = null;
   }

   private static void fail(Throwable t) {
      if (!failed) {
         failed = true;
         FuriahsVisuals.LOG.error("Hooks: an input hook threw; it is logged once", t);
      }
   }
}
