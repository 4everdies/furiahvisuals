package meowtils.furiahvisuals.module.weapons;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.sound.ModSounds;
import meowtils.furiahvisuals.sound.Sfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

final class WeaponWheel {
   static final List<WeaponWheel> ALL = new CopyOnWriteArrayList<>();
   static final long TAP_NANOS = 200000000L;
   private static final float SELECTOR_PER_TURN = 0.005882353F;
   static final float DEAD_ZONE = 0.34F;
   private final WheelSettings s;
   final WheelContent content;
   final WheelRenderer renderer;
   WeaponWheel.State state = WeaponWheel.State.CLOSED;
   boolean inMenu;
   int selected = -1;
   int page;
   float selX;
   float selY;
   long openedAt;
   long closedAt;
   int picked = -1;
   private long pressedAt;
   private boolean keyWasDown;
   private int lastHurtTime;
   private boolean escUsed;
   private long usedAtMs = -4611686018427387904L;
   private static final long KEEP_LOADED_MS = 300000L;
   Boolean testKey;

   WeaponWheel(WheelSettings settings, WheelContent content) {
      this.s = settings;
      this.content = content;
      this.renderer = new WheelRenderer(this, settings, content);
      ALL.add(this);
   }

   int size() {
      return Math.min(48, this.content.size());
   }

   int perPage() {
      int own = this.content.perPage();
      return Math.max(2, Math.min(12, own > 0 ? own : (int)Math.round(this.s.perPage.get())));
   }

   int pages() {
      int n = this.size();
      return Math.max(1, (n + this.perPage() - 1) / this.perPage());
   }

   int pageStart(int p) {
      return Math.max(0, p) * this.perPage();
   }

   int pageCount(int p) {
      return Math.max(0, Math.min(this.perPage(), this.size() - this.pageStart(p)));
   }

   private boolean usable(Minecraft mc) {
      EntityPlayerSP p = mc.thePlayer;
      return this.content.usable()
         && WeaponHooks.hooksIn()
         && mc.theWorld != null
         && p != null
         && mc.currentScreen == null
         && !p.isDead
         && p.getHealth() > 0.0F
         && (mc.playerController == null || !mc.playerController.isSpectator());
   }

   private static boolean keyDown(int code) {
      try {
         if (code == 0) {
            return false;
         } else if (code < 0) {
            int button = code + 100;
            return button >= 0 && button < 16 && Mouse.isButtonDown(button);
         } else {
            return code < 256 && Keyboard.isKeyDown(code);
         }
      } catch (RuntimeException | LinkageError var2) {
         return false;
      }
   }

   void frame() {
      Minecraft mc = Minecraft.getMinecraft();
      long now = ClientClock.nanos();
      long real = System.nanoTime();
      if (this.state == WeaponWheel.State.CLOSING && now - this.closedAt > 150000000L) {
         this.state = WeaponWheel.State.CLOSED;
         this.picked = -1;
         this.renderer.closed();
      }

      if (!this.inMenu) {
         boolean usable = this.usable(mc);
         KeyBinding kb = this.content.key();
         boolean down = this.testKey != null ? this.testKey : kb != null && keyDown(kb.getKeyCode());
         boolean pressed = down && !this.keyWasDown;
         this.keyWasDown = down;
         if (!usable) {
            if (this.state == WeaponWheel.State.OPEN) {
               this.cancel(false);
            } else if (this.state == WeaponWheel.State.PENDING) {
               this.state = WeaponWheel.State.CLOSED;
            }
         } else {
            boolean hold = this.s.mode.get() == WeaponOptions.WheelMode.HOLD;
            if (pressed) {
               this.usedAtMs = System.currentTimeMillis();
               this.content.pressed();
            }

            switch (this.state) {
               case CLOSED:
               case CLOSING:
                  if (pressed) {
                     if (hold && this.s.quickSwap.get()) {
                        this.state = WeaponWheel.State.PENDING;
                        this.pressedAt = real;
                     } else {
                        this.open();
                     }
                  }
                  break;
               case PENDING:
                  if (!down) {
                     this.state = WeaponWheel.State.CLOSED;
                     if (this.content.swapBack()) {
                        ModSounds.gui(Sfx.SELECT);
                     }
                  } else if (real - this.pressedAt >= 200000000L) {
                     this.open();
                  }
                  break;
               case OPEN:
                  if (hold && !down) {
                     this.pickOrCancel();
                  } else if (!hold && pressed) {
                     this.pickOrCancel();
                  }
            }
         }
      }
   }

   void tick() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      int hurt = p == null ? 0 : p.hurtTime;
      if (this.state == WeaponWheel.State.OPEN && !this.inMenu && this.s.closeOnDamage.get() && hurt > this.lastHurtTime) {
         this.cancel(true);
      }

      this.lastHurtTime = hurt;
   }

   void open() {
      for (WeaponWheel w : ALL) {
         if (w != this && (w.state == WeaponWheel.State.OPEN || w.state == WeaponWheel.State.PENDING)) {
            w.cancel(false);
         }
      }

      this.state = WeaponWheel.State.OPEN;
      this.openedAt = ClientClock.nanos();
      this.picked = -1;
      this.selected = Math.min(this.content.current(), this.size() - 1);
      int n = this.page = this.selected >= 0 ? this.selected / this.perPage() : 0;
      if (this.selected >= 0) {
         this.pointAt(this.selected);
      } else {
         this.selY = 0.0F;
         this.selX = 0.0F;
      }

      this.renderer.opened();
      ModSounds.gui(Sfx.GUI_OPEN);
   }

   void openInMenu() {
      this.inMenu = true;
      this.usedAtMs = System.currentTimeMillis();
      this.content.pressed();
      this.open();
   }

   boolean recentlyUsed() {
      return System.currentTimeMillis() - this.usedAtMs < 300000L;
   }

   void pickOrCancel() {
      if (this.state == WeaponWheel.State.OPEN) {
         if (this.selected >= 0 && this.selected < this.size()) {
            boolean changed = this.content.apply(this.selected);
            this.picked = this.selected;
            this.state = WeaponWheel.State.CLOSING;
            this.closedAt = ClientClock.nanos();
            ModSounds.gui(changed ? Sfx.SELECT : Sfx.GUI_CLOSE);
         } else {
            this.cancel(true);
         }
      }
   }

   void cancel(boolean sound) {
      if (this.state == WeaponWheel.State.OPEN || this.state == WeaponWheel.State.PENDING) {
         boolean wasOpen = this.state == WeaponWheel.State.OPEN;
         this.state = wasOpen ? WeaponWheel.State.CLOSING : WeaponWheel.State.CLOSED;
         this.closedAt = ClientClock.nanos();
         this.picked = -1;
         if (sound && wasOpen) {
            ModSounds.gui(Sfx.GUI_CLOSE);
         }
      }
   }

   void closeInMenu() {
      if (this.state == WeaponWheel.State.OPEN) {
         this.cancel(false);
      }

      this.inMenu = false;
      this.state = WeaponWheel.State.CLOSED;
      this.picked = -1;
      this.renderer.closed();
   }

   boolean isOpen() {
      return this.state == WeaponWheel.State.OPEN;
   }

   boolean showing() {
      return this.state == WeaponWheel.State.OPEN || this.state == WeaponWheel.State.CLOSING;
   }

   private void pointAt(int index) {
      int p = index / this.perPage();
      int n = this.pageCount(p);
      if (n > 0) {
         double a = (index - this.pageStart(p)) * ((Math.PI * 2) / n);
         this.selX = (float)Math.sin(a) * 0.72F;
         this.selY = (float)(-Math.cos(a)) * 0.72F;
      }
   }

   private void selectFromSelector() {
      float len = (float)Math.sqrt(this.selX * this.selX + this.selY * this.selY);
      int was = this.selected;
      int n = this.pageCount(this.page);
      if (!(len < 0.34F) && n > 0) {
         double a = Math.atan2(this.selX, -this.selY);
         if (a < 0.0) {
            a += Math.PI * 2;
         }

         double step = (Math.PI * 2) / n;
         int i = (int)Math.floor((a + step / 2.0) / step) % n;
         if (was >= this.pageStart(this.page)
            && was < this.pageStart(this.page) + n
            && was != this.pageStart(this.page) + i
            && Math.abs(((a - (was - this.pageStart(this.page)) * step) % (Math.PI * 2) + (Math.PI * 3)) % (Math.PI * 2) - Math.PI)
               < step / 2.0 + Math.toRadians(3.0)) {
            i = was - this.pageStart(this.page);
         }

         this.selected = this.pageStart(this.page) + i;
      } else {
         this.selected = -1;
      }

      if (this.selected != was) {
         this.tickSound();
      }
   }

   private void tickSound() {
      int n = Math.max(1, this.size());
      ModSounds.sliderTick(this.selected < 0 ? 0.0 : (double)this.selected / n);
      this.content.lit(this.selected);
   }

   boolean turn(float yaw, float pitch) {
      if (this.state == WeaponWheel.State.OPEN && !this.inMenu && this.s.freezeCamera.get()) {
         Minecraft mc = Minecraft.getMinecraft();
         float invert = mc.gameSettings.invertMouse ? -1.0F : 1.0F;
         this.moveSelector(yaw * 0.005882353F, -pitch * invert * 0.005882353F);
         return true;
      } else {
         return false;
      }
   }

   void moveSelector(float dx, float dy) {
      this.selX += dx;
      this.selY += dy;
      float len = (float)Math.sqrt(this.selX * this.selX + this.selY * this.selY);
      if (len > 1.0F) {
         this.selX /= len;
         this.selY /= len;
      }

      this.selectFromSelector();
   }

   void pointSelector(float x, float y) {
      float len = (float)Math.sqrt(x * x + y * y);
      if (len > 1.0F) {
         x /= len;
         y /= len;
      }

      if (x != this.selX || y != this.selY) {
         this.selX = x;
         this.selY = y;
         this.selectFromSelector();
      }
   }

   void scroll(int dir) {
      int n = this.size();
      if (n != 0) {
         int next = this.selected < 0
            ? (dir > 0 ? this.pageStart(this.page) : this.pageStart(this.page) + Math.max(0, this.pageCount(this.page) - 1))
            : ((this.selected + dir) % n + n) % n;
         this.selected = next;
         int p = next / this.perPage();
         if (p != this.page) {
            this.renderer.pageTurn(this.page, p, dir);
            this.page = p;
         }

         this.pointAt(next);
         this.tickSound();
      }
   }

   boolean mouse(int button, boolean down, int wheel) {
      if (this.state != WeaponWheel.State.OPEN || this.inMenu) {
         return false;
      } else if (wheel != 0) {
         this.scroll(wheel < 0 ? 1 : -1);
         return true;
      } else if (button >= 0 && down) {
         if (button == 0) {
            this.pickOrCancel();
         } else if (button == 1) {
            this.cancel(true);
         }

         return true;
      } else {
         return false;
      }
   }

   void keyInput() {
      if (this.escUsed) {
         this.escUsed = false;
         Minecraft.getMinecraft().getSoundHandler().resumeSounds();
      }

      if (this.state == WeaponWheel.State.OPEN && !this.inMenu) {
         for (KeyBinding kb : Minecraft.getMinecraft().gameSettings.keyBindsHotbar) {
            while (kb.isPressed()) {
            }
         }
      }
   }

   boolean screenOpening(GuiScreen screen) {
      if (this.state == WeaponWheel.State.PENDING) {
         this.state = WeaponWheel.State.CLOSED;
         return false;
      } else if (this.state == WeaponWheel.State.OPEN && !this.inMenu && screen != null) {
         boolean esc = screen instanceof GuiIngameMenu || Keyboard.getEventKey() == 1 && Keyboard.getEventKeyState();
         this.cancel(true);
         if (esc) {
            this.escUsed = true;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   String describe() {
      return this.state + (this.inMenu ? " (menu)" : "") + ", lit " + this.selected + ", page " + this.page + "/" + this.pages();
   }

   static void logForTest(String line) {
      FuriahsVisuals.LOG.info("[Wheel] {}", new Object[]{line});
   }

   static enum State {
      CLOSED,
      PENDING,
      OPEN,
      CLOSING;
   }
}
