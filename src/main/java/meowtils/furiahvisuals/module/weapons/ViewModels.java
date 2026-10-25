package meowtils.furiahvisuals.module.weapons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.core.GameClock;
import meowtils.furiahvisuals.fx.FxFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

final class ViewModels {
   static final float[] SHOULDER_R = new float[]{1.4F, -1.2F, -0.2F};
   static final float[] SHOULDER_L = new float[]{-1.4F, -1.2F, -0.2F};
   static final float[] EMPTY_FIST = new float[]{-0.5F, -0.5F, -0.78F};
   static final float[] EMPTY_HANDLE = new float[]{0.62F, 0.3F, -0.72F};
   private static final float DUAL_OUT = 0.05F;
   private static final float DUAL_DOWN = 0.04F;
   private static final float MAX_PHYSICS_DT = 0.1F;
   private static final float MAX_ANIMATION_CATCHUP_DT = 0.25F;
   static WeaponDef.Hold testHold;
   static float[] testShoulder;
   static boolean testBlocking;
   static boolean testArcs;
   static float lastFlareForTest;
   private final WeaponsModule module;
   private final FirstPersonRenderer fp;
   boolean game;
   final ViewModels.Hand right = new ViewModels.Hand(false);
   final ViewModels.Hand left = new ViewModels.Hand(true);
   private final Random random = new Random();
   private final ViewModels.Body body = new ViewModels.Body();
   Boolean dualOverride;
   private long frame = -1L;
   private long lastNanos;
   private int lastSlot = -1;
   private Item lastItem;
   private String lastWeapon;
   private String lastLeft;
   private boolean lastSwinging;
   private int lastSwingInt;
   private boolean showing;
   /** True while the first-person guard is up (vanilla blocking or the held use key), read by Inspects. */
   volatile boolean guardActive;
   private boolean guardDebugReady;
   private boolean guardDebugBlocking;
   private boolean guardDebugUseKey;
   private boolean guardDebugMouse;
   private boolean guardDebugVanilla;
   private boolean guardDebugUsing;
   private boolean guardDebugBow;
   private boolean guardDebugClip;
   private String guardDebugWeapon;
   private boolean nextLeft;
   private final float[] swipe = new float[2];
   private WeaponDef leftDef;
   private float vanillaLeftSwing = -1.0F;
   private long vanillaLeftStart;
   private final Mat4 fist = new Mat4();
   private final Mat4 fistRot = new Mat4();
   private final Mat4 weapon = new Mat4();
   private final Mat4 basis = new Mat4();
   private final float[] pos = new float[3];
   private final float[] tip = new float[3];
   private final float[] edge = new float[3];
   private final float[] v = new float[4];
   private final float[] h = new float[4];
   private final float[] q = new float[4];
   private final float[] bx = new float[3];
   private final float[] by = new float[3];
   private final float[] bz = new float[3];
   private boolean previewSwing;
   private boolean previewDraw;
   boolean previewBlocking;
   boolean compareFx;
   long guardUntil;
   private float turnYaw;
   private float turnPitch;
   private float lastYaw;
   private float lastPitch;
   private float lookFar = 8.0F;
   private boolean turnFresh = true;
   private final float[] eyeAt = new float[4];
   private final float[] eyeLook = new float[3];
   private final float[] eyeHolder = new float[3];

   long nanos() {
      return this.game ? GameClock.nanos() : ClientClock.nanos();
   }

   ViewModels(WeaponsModule module, FirstPersonRenderer fp) {
      this.module = module;
      this.fp = fp;
   }

   private boolean dual(WeaponDef def) {
      boolean on = this.dualOverride != null ? this.dualOverride : this.module.dualWield.get();
      return on && (!def.twoHanded || def.dualHold != null);
   }

   boolean render(Minecraft mc, ItemStack held, WeaponDef def, WeaponSettings s, float pt, boolean mask, boolean leaving) {
      EntityPlayerSP player = mc.thePlayer;
      if (player == null) {
         return false;
      } else {
         WeaponAssets a = WeaponAssets.of(def, WeaponAssets.Tier.FULL);
         a.request(this.module.textureSize());
         if (!a.ready()) {
            WeaponAssets.pump();
            if (!a.ready()) {
               return leaving;
            }
         }

         this.leftDef = this.dual(def) ? this.module.leftWeaponOf(def) : null;
         WeaponAssets la = null;
         if (this.leftDef != null && !this.leftDef.isVanilla()) {
            la = WeaponAssets.of(this.leftDef, WeaponAssets.Tier.FULL);
            la.request(this.module.textureSize());
            if (!la.ready()) {
               la = null;
            }
         }

         long id;
         if ((id = FxFrame.currentFrame()) != this.frame || !mask && this.frame < 0L) {
            this.frame = id;
            long now = this.nanos();
            float elapsed = this.lastNanos == 0L ? 0.0F : Math.max(0.0F, (float)(now - this.lastNanos) / 1.0E9F);
            float dt = Math.min(MAX_PHYSICS_DT, elapsed);
            float animationDt = Math.min(MAX_ANIMATION_CATCHUP_DT, elapsed);
            this.lastNanos = now;
            this.update(player, held, def, s, leaving, pt, dt, animationDt, now);
         }

         if (leaving && this.right.anim.left()) {
            return true;
         } else {
            boolean timing = WeaponsModule.timingForTest && !mask;
            long started = 0L;
            if (timing) {
               started = System.nanoTime();
            }

            this.fp.clear();
            this.fp.fx.frame(player, pt, this.nanos());
            boolean leftHanded = s.leftHanded.get();
            boolean dual = this.leftDef != null;
            this.body.of(player);
            this.compose(this.body, this.right, a, s, def.isBow() != leftHanded, dual, dual ? null : this.right.pose);
            if (dual && la != null) {
               this.compose(this.body, this.left, la, this.module.settingsOf(this.leftDef), !leftHanded, true, null);
            }

            this.addGhosts();
            this.fp.draw(mc, pt, s.fov.getFloat(), this.module.antiAliasing.get(), mask, this.module.time(), this.nanos());
            if (timing) {
               WeaponsModule.noteFirstPerson(System.nanoTime() - started);
            }

            if (dual && this.leftDef.isVanilla() && held != null && !mask) {
               this.drawVanillaLeft(mc, player, held, !leftHanded, pt);
            }

            return true;
         }
      }
   }

   void previewSwing() {
      this.previewSwing = true;
   }

   void previewDraw() {
      this.previewDraw = true;
   }

   boolean preview(WeaponDef def, WeaponSettings s, ViewModels.Body look, float dt) {
      WeaponAssets a = WeaponAssets.of(def, WeaponAssets.Tier.FULL);
      a.request(this.module.textureSize());
      if (!a.ready()) {
         WeaponAssets.pump();
         if (!a.ready()) {
            return false;
         }
      }

      this.leftDef = this.dual(def) ? this.module.leftWeaponOf(def) : null;
      WeaponAssets la = null;
      if (this.leftDef != null && !this.leftDef.isVanilla()) {
         la = WeaponAssets.of(this.leftDef, WeaponAssets.Tier.FULL);
         la.request(this.module.textureSize());
         if (!la.ready()) {
            la = null;
         }
      }

      long now = this.nanos();
      boolean dual = this.leftDef != null;
      this.turnPitch = 0.0F;
      this.turnYaw = 0.0F;
      this.turnFresh = true;
      this.lookFar = 8.0F;
      this.right.set(def, s, dual);
      if (dual && !this.leftDef.isVanilla()) {
         this.left.set(this.leftDef, this.module.settingsOf(this.leftDef), true);
      }

      String leftId = dual ? this.leftDef.id : null;
      if (!this.showing || this.previewDraw || !def.id.equals(this.lastWeapon) || (leftId == null ? this.lastLeft != null : !leftId.equals(this.lastLeft))) {
         this.startDraw(this.right);
         if (dual && !this.leftDef.isVanilla()) {
            this.startDraw(this.left);
         }

         this.previewDraw = false;
      }

      this.showing = true;
      this.lastWeapon = def.id;
      this.lastLeft = leftId;
      if (this.previewSwing) {
         this.previewSwing = false;
         this.swingHands(false, 6, dual, now);
      }

      if (this.previewBlocking) {
         this.cancelInspects();
      }

      this.step(this.right, null, def, s, dual, this.previewBlocking, 1.0F, dt, dt);
      if (dual && !this.leftDef.isVanilla()) {
         this.step(this.left, null, this.leftDef, this.module.settingsOf(this.leftDef), true, this.previewBlocking, 1.0F, dt, dt);
      }

      if (this.vanillaLeftSwing >= 0.0F) {
         this.vanillaLeftSwing = (float)(now - this.vanillaLeftStart) / 1.0E9F / 0.3F;
         if (this.vanillaLeftSwing >= 1.0F) {
            this.vanillaLeftSwing = -1.0F;
         }
      }

      this.fp.clear();
      this.fp.fx.frameStill(now);
      this.fp.fx.update(dt);
      boolean leftHanded = s.leftHanded.get();
      this.compose(look, this.right, a, s, def.isBow() != leftHanded, dual, dual ? null : this.right.pose);
      if (dual && la != null) {
         this.compose(look, this.left, la, this.module.settingsOf(this.leftDef), !leftHanded, true, null);
      }

      this.addGhosts();
      return true;
   }

   boolean previewInspect(String id) {
      Clip c = this.right.def == null ? null : this.module.inspects.clipFor(this.right.def, id);
      if (c == null) {
         return false;
      } else {
         this.right.anim.startInspect(c, this.module.inspectSpeed.getFloat());
         Clip lc;
         if (this.leftDef != null
            && !this.leftDef.isVanilla()
            && this.left.def == this.leftDef
            && (lc = this.module.inspects.clipFor(this.left.def, id)) != null) {
            this.left.anim.startInspect(lc, this.module.inspectSpeed.getFloat());
         }

         return true;
      }
   }

   boolean previewVanillaLeft() {
      return this.leftDef != null && this.leftDef.isVanilla();
   }

   void drawPreviewVanillaLeft(Minecraft mc, ItemStack stack, boolean leftHanded) {
      this.drawVanillaLeft(mc, null, stack, !leftHanded, 1.0F);
   }

   void updateOnly(Minecraft mc, ItemStack held, WeaponDef def, WeaponSettings s, float pt) {
      EntityPlayerSP player = mc.thePlayer;
      long id = FxFrame.currentFrame();
      if (player != null && id != this.frame) {
         this.frame = id;
         long now = this.nanos();
         float elapsed = this.lastNanos == 0L ? 0.0F : Math.max(0.0F, (float)(now - this.lastNanos) / 1.0E9F);
         float dt = Math.min(MAX_PHYSICS_DT, elapsed);
         float animationDt = Math.min(MAX_ANIMATION_CATCHUP_DT, elapsed);
         this.lastNanos = now;
         this.leftDef = this.dual(def) ? this.module.leftWeaponOf(def) : null;
         this.update(player, held, def, s, false, pt, dt, animationDt, now);
      }
   }

   private void update(EntityPlayerSP p, ItemStack held, WeaponDef def, WeaponSettings s, boolean leaving, float pt, float dt, float animationDt, long now) {
      boolean dual = this.leftDef != null;
      this.turning(p, pt, dt);
      this.right.set(def, s, dual);
      if (dual && !this.leftDef.isVanilla()) {
         this.left.set(this.leftDef, this.module.settingsOf(this.leftDef), true);
      }

      int slot = p.inventory.currentItem;
      Item item = held == null ? null : held.getItem();
      String leftId = dual ? this.leftDef.id : null;
      if (leaving) {
         this.right.anim.leave();
         this.left.anim.leave();
      } else if (!this.showing
         || slot != this.lastSlot
         || item != this.lastItem
         || !def.id.equals(this.lastWeapon)
         || (leftId == null ? this.lastLeft != null : !leftId.equals(this.lastLeft))) {
         this.startDraw(this.right);
         if (dual && !this.leftDef.isVanilla()) {
            this.startDraw(this.left);
         }

         this.lastSwinging = p.isSwingInProgress;
         this.lastSwingInt = p.swingProgressInt;
         this.module.inspects.drawn(this.right);
      }

      this.showing = !leaving;
      this.lastSlot = slot;
      this.lastItem = item;
      this.lastWeapon = def.id;
      this.lastLeft = leftId;
      boolean swinging = p.isSwingInProgress;
      int si = p.swingProgressInt;
      if (!leaving && swinging && (!this.lastSwinging || si < this.lastSwingInt)) {
         this.swing(p, dual, now);
      }

      this.lastSwinging = swinging;
      this.lastSwingInt = si;
      Minecraft mc = Minecraft.getMinecraft();
      boolean screenOpen = mc.currentScreen != null;
      boolean mappedUseKey = !screenOpen && mc.gameSettings.keyBindUseItem.isKeyDown();
      boolean rightMouseDown = !screenOpen && Mouse.isButtonDown(1);
      boolean useKeyDown = mappedUseKey || rightMouseDown;
      boolean usingBow = p.isUsingItem() && WeaponRule.isBow(p.getItemInUse());
      // Do not gate a held visual guard on isUsingItem: vanilla may toggle that state during attacks.
      boolean visualGuard = useKeyDown && !def.isBow() && !usingBow;
      boolean vanillaBlocking = p.isBlocking();
      boolean blocking = vanillaBlocking || visualGuard || testBlocking || now < this.guardUntil;
      this.guardActive = blocking && !def.isBow();
      boolean hasGuardClip = this.right.anim.hasGuardClip();
      if (!this.guardDebugReady || blocking != this.guardDebugBlocking || mappedUseKey != this.guardDebugUseKey
         || rightMouseDown != this.guardDebugMouse || vanillaBlocking != this.guardDebugVanilla
         || p.isUsingItem() != this.guardDebugUsing || usingBow != this.guardDebugBow
         || hasGuardClip != this.guardDebugClip || !def.id.equals(this.guardDebugWeapon)) {
         FuriahsVisuals.LOG.debug("Guard state: weapon={}, active={}, mappedUse={}, mouseRight={}, vanillaBlocking={}, usingItem={}, usingBow={}, clip={}",
            new Object[]{def.id, blocking, mappedUseKey, rightMouseDown, vanillaBlocking, p.isUsingItem(), usingBow, hasGuardClip});
         this.guardDebugReady = true;
         this.guardDebugBlocking = blocking;
         this.guardDebugUseKey = mappedUseKey;
         this.guardDebugMouse = rightMouseDown;
         this.guardDebugVanilla = vanillaBlocking;
         this.guardDebugUsing = p.isUsingItem();
         this.guardDebugBow = usingBow;
         this.guardDebugClip = hasGuardClip;
         this.guardDebugWeapon = def.id;
      }
      if (blocking || p.isUsingItem()) {
         this.cancelInspects();
      }

      this.step(this.right, p, def, s, dual, blocking, pt, dt, animationDt);
      if (dual && !this.leftDef.isVanilla()) {
         this.step(this.left, p, this.leftDef, this.module.settingsOf(this.leftDef), true, blocking, pt, dt, animationDt);
      }

      if (this.vanillaLeftSwing >= 0.0F) {
         this.vanillaLeftSwing = (float)(now - this.vanillaLeftStart) / 1.0E9F / (armSwingTicks(p) * 0.05F);
         if (this.vanillaLeftSwing >= 1.0F) {
            this.vanillaLeftSwing = -1.0F;
         }
      }
   }

   private void cancelInspects() {
      this.right.anim.cancelInspect();
      this.left.anim.cancelInspect();
   }

   private void startDraw(ViewModels.Hand hand) {
      WeaponOptions.Draw d = this.module.draw.get();
      Clip clip = d == WeaponOptions.Draw.OFF ? null : this.module.clips.get(hand.def.draw != null ? hand.def.draw : hand.def.animations + "_draw");
      hand.anim.reset();
      hand.anim.startDraw(clip, d == WeaponOptions.Draw.QUICK ? 2.0F : 1.0F);
      if (hand.def.engine) {
         hand.engine.draw();
      }
   }

   private void onEvent(ViewModels.Hand hand, Clip.Event e) {
      if (hand.def != null) {
         if ("ignite".equals(e.type)) {
            hand.ignite = true;
         } else if ("flare".equals(e.type)) {
            hand.emberBurst = true;
         } else if ("dust".equals(e.type)) {
            hand.dust = true;
         } else if ("leaves".equals(e.type)) {
            int n = 3;

            try {
               n = e.arg != null ? Integer.parseInt(e.arg.trim()) : 3;
            } catch (NumberFormatException var5) {
               n = 3;
            }

            hand.leaves = Math.min(12, hand.leaves + Math.max(0, n));
         } else if ("spark".equals(e.type)) {
            hand.zap = 1.0F;
            hand.arcSparks = true;
         } else if ("pluck".equals(e.type)) {
            hand.bow.pluck();
         } else if ("eye".equals(e.type)) {
            hand.eye.event(e.arg != null ? e.arg.trim() : "");
         } else if (hand.def.engine) {
            if ("rev".equals(e.type)) {
               hand.engine.rev();
            } else if ("stall".equals(e.type)) {
               hand.engine.stop();
            } else if ("pull".equals(e.type)) {
               if (this.game) {
                  this.module.chainsawSound.pull();
               }
            } else if ("start".equals(e.type)) {
               hand.engine.caught();
            } else if ("smoke".equals(e.type)) {
               hand.smoke = hand.smoke + 1.0F;
            }
         }
      }
   }

   private void step(ViewModels.Hand hand, EntityPlayerSP p, WeaponDef def, WeaponSettings s, boolean dual, boolean blocking, float pt, float dt, float animationDt) {
      hand.motion.update(p, pt, dt, s, def.mass, def.twoHanded && !dual);
      hand.stepDangles(dt);
      hand.lastDt = dt;
      if (def.engine) {
         if (!hand.engine.running && !hand.anim.inspecting()) {
            hand.engine.start();
         }

         hand.engine.update(dt, s.vibration == null || s.vibration.get());
         if (hand.engine.running) {
            hand.wisp = hand.wisp + dt * (0.45F + 2.2F * hand.engine.throttle);
            if (hand.wisp >= 1.0F) {
               hand.wisp = hand.wisp - 1.0F;
               hand.smoke = hand.smoke + (0.15F + 0.25F * hand.engine.throttle);
            }
         }
      }

      if (def.isBow()) {
         float upt;
         int count;
         boolean using;
         if (p != null && p.isUsingItem() && WeaponRule.isBow(p.getItemInUse())) {
            using = true;
            count = p.getItemInUseCount();
            upt = pt;
            hand.previewUse = 0.0F;
         } else {
            using = blocking;
            hand.previewUse = blocking ? hand.previewUse + dt * 20.0F : 0.0F;
            count = 72000 - (int)Math.floor(hand.previewUse);
            upt = hand.previewUse - (float)Math.floor(hand.previewUse);
         }

         hand.bow.update(using, count, upt, dt);
         hand.bow.stepPluck(dt);
         blocking = false;
      }

      hold(def, s, dual, this.pos, this.tip, this.edge);
      Rig.basis(this.tip, this.edge, this.by, this.bx, this.bz);
      Mat4.fromBasis(this.bx, this.by, this.bz, this.q);
      hand.anim.setHold(this.pos, this.q);
      hand.anim.update(animationDt, blocking, this.module.blockHit.get(), hand.pose);
      hand.partsDue = true;
      hand.guarding = blocking;
   }

   private void swing(EntityPlayerSP p, boolean dual, long now) {
      boolean crit = p.fallDistance > 0.0F
         && !p.onGround
         && !p.isOnLadder()
         && !p.isInWater()
         && !p.isPotionActive(Potion.blindness)
         && p.ridingEntity == null;
      this.swingHands(crit, armSwingTicks(p), dual, now);
   }

   private void swingHands(boolean crit, int ticks, boolean dual, long now) {
      if (!dual) {
         this.startSwing(this.right, ticks, crit);
      } else {
         WeaponOptions.DualSwings ds = this.module.dualSwings.get();
         boolean rightHand;
         boolean leftHand;
         if (crit || ds == WeaponOptions.DualSwings.BOTH) {
            leftHand = true;
            rightHand = true;
         } else if (ds == WeaponOptions.DualSwings.RIGHT_ONLY) {
            rightHand = true;
            leftHand = false;
         } else if (this.right.settings.swingPattern != null
            && this.right.settings.swingPattern.get() == WeaponOptions.SwingPattern.DIRECTIONAL
            && this.swipe()) {
            leftHand = this.swipe[0] > 0.0F;
            rightHand = !leftHand;
         } else {
            leftHand = this.nextLeft;
            rightHand = !this.nextLeft;
            boolean var9 = this.nextLeft = !this.nextLeft;
         }

         if (rightHand) {
            this.startSwing(this.right, ticks, crit);
         }

         if (leftHand) {
            if (this.leftDef != null && this.leftDef.isVanilla()) {
               this.vanillaLeftStart = now;
               this.vanillaLeftSwing = 0.0F;
            } else {
               this.startSwing(this.left, ticks, crit);
            }
         }

         this.cancelInspects();
      }
   }

   private boolean swipe() {
      if (!this.module.swipeFromHit(this.swipe)) {
         this.module.measurer().screenSwipe(GameClock.nanos(), this.swipe);
      }

      return Math.sqrt(this.swipe[0] * this.swipe[0] + this.swipe[1] * this.swipe[1]) >= 0.35;
   }

   private void startSwing(ViewModels.Hand hand, int ticks, boolean crit) {
      if (hand.def != null) {
         this.cancelInspects();
         hand.motion.attacked();
         if (hand.def.engine) {
            hand.engine.start();
            hand.engine.rev();
         }

         String set = hand.def.animations;
         boolean quick = hand.anim.swinging();
         Clip clip = null;
         if (crit) {
            clip = this.module.clips.find("crit", hand.def.id, set);
         }

         if (clip == null) {
            List<Clip> variants = quick ? this.module.clips.swings(set + "_quick") : null;
            if (variants == null || variants.isEmpty()) {
               variants = this.module.clips.swings(hand.def.id);
            }

            if (variants.isEmpty()) {
               variants = this.module.clips.swings(set);
            }

            if (!variants.isEmpty()) {
               clip = this.pick(hand, variants);
            }
         }

         if (clip != null) {
            float seconds = ticks * 0.05F;
            hand.anim.startSwing(clip, seconds, hand.pose);
            hand.lastSwing = clip;
            hand.newSwing = true;
            hand.heatKick = 1.0F;
         }
      }
   }

   private Clip pick(ViewModels.Hand hand, List<Clip> variants) {
      WeaponOptions.SwingPattern pattern = hand.settings.swingPattern != null ? hand.settings.swingPattern.get() : WeaponOptions.SwingPattern.ALTERNATE;
      int n = variants.size();
      if (pattern == WeaponOptions.SwingPattern.DIRECTIONAL && this.swipe()) {
         float sx = hand.left ? -this.swipe[0] : this.swipe[0];
         float sy = this.swipe[1];
         float len = (float)Math.sqrt(sx * sx + sy * sy);
         Clip best = null;
         float bestDot = -2.0F;

         for (Clip c : variants) {
            float d;
            if ((c.dirX != 0.0F || c.dirY != 0.0F) && (d = (c.dirX * sx + c.dirY * sy) / len) > bestDot) {
               bestDot = d;
               best = c;
            }
         }

         if (best != null) {
            return best;
         }
      }

      if (pattern == WeaponOptions.SwingPattern.RANDOM && n > 1) {
         Clip cx;
         while ((cx = variants.get(this.random.nextInt(n))) == hand.lastSwing) {
         }

         return cx;
      } else {
         return variants.get(hand.swingCount++ % n);
      }
   }

   static int armSwingTicks(EntityPlayerSP p) {
      if (p.isPotionActive(Potion.digSpeed)) {
         return 6 - (1 + p.getActivePotionEffect(Potion.digSpeed).getAmplifier());
      } else {
         return p.isPotionActive(Potion.digSlowdown) ? 6 + (1 + p.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2 : 6;
      }
   }

   void forceDraw() {
      this.showing = false;
   }

   void reset() {
      this.showing = false;
      this.lastWeapon = null;
      this.lastLeft = null;
      this.right.anim.reset();
      this.right.motion.reset();
      this.left.anim.reset();
      this.left.motion.reset();
   }

   ViewModels.Hand dualLeft() {
      return this.leftDef != null && !this.leftDef.isVanilla() && this.left.def == this.leftDef ? this.left : null;
   }

   private void compose(ViewModels.Body b, ViewModels.Hand hand, WeaponAssets a, WeaponSettings s, boolean mirrored, boolean dual, Pose emptyPose) {
      WeaponDef def = hand.def != null ? hand.def : a.def;
      boolean slim = b.slim;
      boolean hideArms = this.module.hideArms.get();
      float vmScale = placementScale(s);
      Pose pose = hand.pose;
      Motion m = hand.motion;
      hold(def, s, dual, this.pos, this.tip, this.edge);
      if (dual) {
         this.pos[0] = this.pos[0] + 0.05F;
         this.pos[1] = this.pos[1] - 0.04F;
      }

      if (def.isBow()) {
         aimBow(def, holdFor(def, dual), hand.bow.aim(), this.pos, this.tip, this.edge);
      }

      Rig.basis(this.tip, this.edge, this.by, this.bx, this.bz);
      this.basis.frame(this.bx, this.by, this.bz, 0.0F, 0.0F, 0.0F);
      float fx = this.pos[0] + m.offset[0] + pose.rPos[0];
      float fy = this.pos[1] + m.offset[1] + pose.rPos[1];
      float fz = this.pos[2] + m.offset[2] + pose.rPos[2];
      if (def.engine) {
         fx += hand.engine.shake[0];
         fy += hand.engine.shake[1];
         fz += hand.engine.shake[2];
      }

      float tremble = def.isBow() ? hand.bow.tremble() : 0.0F;
      if (tremble > 0.0F) {
         double t = this.nanos() % 3600000000000L / 1.0E9;
         float a0 = 0.0035F * tremble;
         fx += a0 * (float)(Math.sin(t * 25.1) + 0.5 * Math.sin(t * 41.7 + 1.3));
         fy += a0 * (float)(Math.sin(t * 29.3 + 0.7) + 0.5 * Math.sin(t * 37.9 + 2.1));
      }

      this.fistRot.identity();
      this.fistRot.rotate(pose.rRot);
      Mat4.euler(m.pitch, m.yaw, m.roll, this.q);
      this.fistRot.rotate(this.q);
      this.fistRot.mul(this.basis);
      this.fist.identity();
      this.fist.translate(fx, fy, fz);
      this.fist.mul(this.fistRot);
      if (pose.weaponInView) {
         this.weapon.identity();
         this.weapon.translate(fx + pose.wPos[0], fy + pose.wPos[1], fz + pose.wPos[2]);
         this.weapon.rotate(pose.wRot);
         this.weapon.mul(this.fistRot);
      } else {
         this.weapon.set(this.fist);
         this.weapon.translate(pose.wPos[0], pose.wPos[1], pose.wPos[2]);
         this.weapon.rotate(pose.wRot);
      }

      WeaponDef.Hold hold = holdFor(def, dual);
      float scale = hold.scale * sizeFirst(s) * vmScale;
      Rig.gripToFist(def.right, scale, this.weapon);
      if (hand.partsDue) {
         hand.partsDue = false;
         this.dangle(hand, def, b.pitch);
         if (def.eye != null) {
            this.eye(hand, def, mirrored);
         }
      }

      if (hand.smoke > 0.0F) {
         this.exhaust(hand, def, scale, mirrored);
      }

      retract(hand, def, pose);
      hand.hitFlash = Math.max(0.0F, hand.hitFlash - hand.lastDt * 3.0F);
      hand.zap = Math.max(0.0F, hand.zap - hand.lastDt * 3.5F);
      heat(hand, def, pose);
      boolean pixel = hand.styleShown(def, this.module.pixelFor(b.held, def), this.module.switchAnimation.get());
      this.fp.mosaic = Math.max(this.fp.mosaic, hand.mosaic());
      this.effects(hand, def, s, pose, scale, mirrored, pixel, a.arcs(pixel), this.compareFx ? a.arcs(!pixel) : null);
      FirstPersonRenderer.Item w = this.fp.addWeapon(a, mirrored, mirrored);
      if (w != null) {
         w.matrix.set(this.weapon);
         if (mirrored) {
            w.matrix.mirrorOuterX();
         }

         w.look.set(s);
         w.look.finish = this.module.ruleFinish(b.held, def);
         w.look.pixel = pixel;
         w.look.poses = pose.parts.isEmpty() ? null : pose.parts;
         w.look.eye = def.eye != null ? hand.eye.pixel : null;
         WeaponRenderer.growForPoses(def, a.mesh(0), w.look.poses, w.bounds);
         if (pixel) {
            WeaponRenderer.growForPoses(def, a.pixelTwin().pixelMesh(0), w.look.poses, w.bounds);
            this.fp.pixelDrawn = true;
         }

         w.look.enchant = this.module.enchantMode(b.held);
         w.look.glint = pose.glint;
         w.look.chainPhase = hand.engine.phase;
         w.look.flare = Math.max(Math.max(0.0F, pose.flare), def.flareOnHit ? hand.hitFlash : 0.0F) * (calm() ? 0.35F : 1.0F);
         lastFlareForTest = w.look.flare;
         w.look.reveal = pose.reveal >= 0.0F && def.runes != null ? def.runes[0] + (def.runes[1] - def.runes[0]) * pose.reveal : 10000.0F;
         float f2 = w.look.heat = def.heatIdle > 0.0F ? hand.heat : -1.0F;
         if (def.isBow()) {
            float extra = Math.max(0.0F, pose.bend);
            w.look.bowPull = hand.bow.pull() + extra + hand.bow.pluckShake();
            w.look.bowBend = hand.bow.bend() + extra;
            w.look.bowNocked = hand.bow.nocked();
            w.bounds[0] = Math.min(w.bounds[0], def.bow.nock[0] - def.bow.draw * Math.max(0.0F, w.look.bowPull) - 0.02F);
            WeaponRenderer.growForFan(def, a.mesh(0), w.bounds);
            if (pixel) {
               WeaponRenderer.growForFan(def, a.pixelTwin().pixelMesh(0), w.bounds);
            }
         } else {
            w.look.bend = pose.bend;
         }
      }

      this.motionBlur(hand, a, w, mirrored);
      if (!hideArms) {
         float[] fistPos = new float[]{this.fist.tx(), this.fist.ty(), this.fist.tz()};
         this.fist.direction(0.0F, 1.0F, 0.0F, this.h);
         float[] shoulder = testShoulder != null ? testShoulder : (hold.shoulder != null ? hold.shoulder : SHOULDER_R);
         this.arm(b, true, fistPos, this.h, shoulder, vmScale, mirrored, mirrored);
         if (def.twoHanded && def.left != null && !dual) {
            if (def.isBow()) {
               bowHand(def, hand.bow, pose.reveal, hand.bow.pull() + Math.max(0.0F, pose.bend), this.v);
               this.weapon.transform(this.v[0], this.v[1], this.v[2], this.v);
            } else {
               this.weapon.transform(def.left.x, def.left.y, def.left.z, this.v);
            }

            float[] lp = new float[]{this.v[0] + pose.lPos[0], this.v[1] + pose.lPos[1], this.v[2] + pose.lPos[2]};
            this.weapon.direction(def.left.ax, def.left.ay, def.left.az, this.h);
            this.reachPart(def, pose, lp, this.h);
            float free = pose.lFree <= 0.0F ? 0.0F : Math.min(1.0F, pose.lFree);
            if (free > 0.0F) {
               float k = free * free * (3.0F - 2.0F * free);
               lp[0] += (EMPTY_FIST[0] - lp[0]) * k;
               lp[1] += (EMPTY_FIST[1] - 0.6F - lp[1]) * k;
               lp[2] += (EMPTY_FIST[2] - lp[2]) * k;

               for (int i = 0; i < 3; i++) {
                  this.h[i] = this.h[i] + (EMPTY_HANDLE[i] - this.h[i]) * k;
               }

               Mat4.normalize(this.h);
            }

            if (free < 0.98F) {
               float[] ls = hold.leftShoulder != null ? hold.leftShoulder : SHOULDER_L;
               boolean exact = false;
               float onPart = pose.lAtPart == null ? 0.0F : Math.min(1.0F, pose.lAt);
               if (def.left.reach != null && onPart < 1.0F) {
                  this.weapon.direction(def.left.reach[0], def.left.reach[1], def.left.reach[2], this.q);
                  Mat4.normalize(this.q);
                  float k = onPart * onPart * (3.0F - 2.0F * onPart);
                  float[] r = new float[]{lp[0] + this.q[0] * 2.0F, lp[1] + this.q[1] * 2.0F, lp[2] + this.q[2] * 2.0F};
                  ls = new float[]{r[0] + (ls[0] - r[0]) * k, r[1] + (ls[1] - r[1]) * k, r[2] + (ls[2] - r[2]) * k};
                  exact = k < 0.5F;
               }

               if (free > 0.0F && ls != SHOULDER_L) {
                  float k = free * free * (3.0F - 2.0F * free);
                  ls = new float[]{ls[0] + (SHOULDER_L[0] - ls[0]) * k, ls[1] + (SHOULDER_L[1] - ls[1]) * k, ls[2] + (SHOULDER_L[2] - ls[2]) * k};
                  exact &= k < 0.5F;
               }

               this.arm(b, false, lp, this.h, ls, vmScale, mirrored, mirrored, exact);
            }
         } else {
            float share;
            if (emptyPose != null && (share = this.emptyHandShare(emptyPose, m)) > 0.01F) {
               float k = 1.0F - share;
               float[] ep = new float[]{
                  EMPTY_FIST[0] + emptyPose.lPos[0], EMPTY_FIST[1] + emptyPose.lPos[1] - 0.45F * k * k, EMPTY_FIST[2] + emptyPose.lPos[2]
               };
               if (m.sprintHand > 0.0F) {
                  ep[1] += (float)Math.sin(b.walked * Math.PI) * 0.03F * m.sprintHand;
               }

               float[] eh = (float[])EMPTY_HANDLE.clone();
               Mat4 r = new Mat4().rotate(emptyPose.lRot);
               r.direction(eh[0], eh[1], eh[2], eh);
               this.slideAlong(def, emptyPose, ep, eh);
               this.arm(b, false, ep, eh, SHOULDER_L, 1.0F, mirrored, !mirrored);
            }
         }
      }
   }

   private void motionBlur(ViewModels.Hand hand, WeaponAssets a, FirstPersonRenderer.Item w, boolean mirrored) {
      hand.ghosts = 0;
      if (w != null && !WeaponsModule.maskPass && this.module.motionBlur.get() && hand.anim.inspecting()) {
         long now = this.nanos();
         if (hand.pastCount > 0) {
            if (now > hand.pastAt) {
               float seconds = Math.max(0.001F, (float)(now - hand.pastAt) / 1.0E9F);
               float speed = this.turnBetween(hand.past[0], w.matrix) / seconds;
               hand.blur = Math.min(1.0F, (speed - 300.0F) / 700.0F);
            }

            float k;
            if ((k = hand.blur) > 0.0F) {
               for (int i = 0; i < hand.pastCount; i++) {
                  hand.ghostAlpha[i] = 0.24F * k * (1.0F - i / 3.0F);
               }

               hand.ghosts = hand.pastCount;
               hand.ghostAssets = a;
               hand.ghostMirrored = mirrored;
               hand.ghostLook.copy(w.look);
            }
         }

         if (now - hand.pastAt >= 15000000L) {
            Mat4 oldest = hand.past[2];
            hand.past[2] = hand.past[1];
            hand.past[1] = hand.past[0];
            hand.past[0] = oldest;
            oldest.set(w.matrix);
            hand.pastCount = Math.min(3, hand.pastCount + 1);
            hand.pastAt = now;
         }
      } else {
         hand.pastCount = 0;
         hand.blur = 0.0F;
      }
   }

   private float turnBetween(Mat4 a, Mat4 b) {
      float[] u = new float[3];
      float[] v2 = new float[3];
      float most = 0.0F;

      for (int axis = 0; axis < 2; axis++) {
         a.direction(axis == 0 ? 1.0F : 0.0F, axis == 1 ? 1.0F : 0.0F, 0.0F, u);
         b.direction(axis == 0 ? 1.0F : 0.0F, axis == 1 ? 1.0F : 0.0F, 0.0F, v2);
         Mat4.normalize(u);
         Mat4.normalize(v2);
         float d = Math.max(-1.0F, Math.min(1.0F, u[0] * v2[0] + u[1] * v2[1] + u[2] * v2[2]));
         most = Math.max(most, (float)Math.toDegrees(Math.acos(d)));
      }

      return most;
   }

   private void addGhosts() {
      for (ViewModels.Hand hand : new ViewModels.Hand[]{this.right, this.left}) {
         for (int i = 0; i < hand.ghosts; i++) {
            FirstPersonRenderer.Item g = this.fp.addWeapon(hand.ghostAssets, hand.ghostMirrored, hand.ghostMirrored);
            if (g == null) {
               return;
            }

            g.ghost = true;
            g.matrix.set(hand.past[i]);
            g.look.copy(hand.ghostLook);
            g.look.alpha = hand.ghostAlpha[i];
         }

         hand.ghosts = 0;
      }
   }

   private void slideAlong(WeaponDef def, Pose pose, float[] ep, float[] eh) {
      float hold = pose.lAlongHold;
      if (!(hold <= 0.0F) && pose.lAlongFrom != null) {
         float[] f = pose.lAlongFrom;
         float[] t = pose.lAlongTo;
         float a = pose.lAlong;
         float[] on = new float[4];
         float x = f[0] + (t[0] - f[0]) * a;
         float y = f[1] + (t[1] - f[1]) * a;
         float d = Math.max(0.0F, y - def.bendFrom);
         float var19;
         this.weapon.transform(var19 = x + pose.bend * d * d, y, f[2] + (t[2] - f[2]) * a, on);
         float[] n = new float[3];
         this.weapon.direction(0.0F, 0.0F, 1.0F, n);
         Mat4.normalize(n);
         if (n[0] * on[0] + n[1] * on[1] + n[2] * on[2] > 0.0F) {
            n[0] = -n[0];
            n[1] = -n[1];
            n[2] = -n[2];
         }

         float[] along = new float[3];
         this.weapon.direction(0.0F, 1.0F, 0.0F, along);
         Mat4.normalize(along);
         float k = Math.min(1.0F, hold);
         k = k * k * (3.0F - 2.0F * k);

         for (int i = 0; i < 3; i++) {
            ep[i] += (on[i] + n[i] * 0.04F - ep[i]) * k;
            eh[i] += (along[i] - eh[i]) * k;
         }

         Mat4.normalize(eh);
      }
   }

   private void motes(ViewModels.Hand hand, WeaponDef def, int accent, float scale, boolean mirrored, float flare) {
      float[] mo = def.motes;
      float hot = def.heatIdle > 0.0F ? hand.heat / def.heatIdle : 1.0F + 2.0F * Math.max(0.0F, Math.min(1.0F, flare));
      hand.moteClock = hand.moteClock + hand.lastDt * mo[6] * hot * hot;

      for (int n = 0; hand.moteClock >= 1.0F && n < 4; n++) {
         hand.moteClock = hand.moteClock - 1.0F;
         this.fp.fx.mote(this.motePoint(mo, this.random.nextFloat(), mirrored), scale, accent, mo[7], def.moteKind);
      }

      hand.moteClock = Math.min(hand.moteClock, 1.0F);
   }

   private float[] motePoint(float[] mo, float k, boolean mirrored) {
      if (mo.length < 11) {
         return this.point(mo[0] + (mo[3] - mo[0]) * k, mo[1] + (mo[4] - mo[1]) * k, mo[2] + (mo[5] - mo[2]) * k, mirrored);
      } else {
         float a = (1.0F - k) * (1.0F - k);
         float b = 2.0F * (1.0F - k) * k;
         float c = k * k;
         float[] p = new float[3];

         for (int i = 0; i < 3; i++) {
            float control = 2.0F * mo[8 + i] - (mo[i] + mo[3 + i]) * 0.5F;
            p[i] = a * mo[i] + b * control + c * mo[3 + i];
         }

         return this.point(p[0], p[1], p[2], mirrored);
      }
   }

   private void emberBurst(WeaponDef def, int accent, float scale, boolean mirrored) {
      if (def.motes != null) {
         float[] push = null;
         if (def.moteBurst != null) {
            float[] mo = def.motes;
            float[] b = def.moteBurst;
            float[] a0 = this.point(mo[0], mo[1], mo[2], mirrored);
            float[] a1 = this.point(mo[0] + b[0], mo[1] + b[1], mo[2] + b[2], mirrored);
            push = new float[]{a1[0] - a0[0], a1[1] - a0[1], a1[2] - a0[2]};
         }

         int n = calm() ? 8 : 26;

         for (int i = 0; i < n; i++) {
            float[] at = this.motePoint(def.motes, this.random.nextFloat(), mirrored);
            float rise = def.motes[7] * (1.5F + this.random.nextFloat());
            if (push == null) {
               this.fp.fx.mote(at, scale, accent, rise, "ember");
            } else {
               this.fp.fx.mote(at, scale, accent, rise, "ember", push);
            }
         }
      }
   }

   private static void heat(ViewModels.Hand hand, WeaponDef def, Pose pose) {
      if (def.heatIdle <= 0.0F) {
         hand.heat = 0.0F;
      } else {
         float dt = hand.lastDt;
         hand.heatFlush = hand.heatFlush + (hand.heatKick - hand.heatFlush) * (1.0F - (float)Math.exp(-dt * 30.0F));
         hand.heatKick = hand.heatKick * (float)Math.exp(-dt * 2.2F);
         float idle = def.heatIdle;
         float f = pose.flare;
         float h = f >= 0.0F ? idle + (1.0F - idle) * Math.min(1.0F, f) : idle * Math.max(0.0F, 1.0F + f);
         float target = Math.max(0.0F, Math.min(1.0F, h + def.heatSwing * hand.heatFlush));
         hand.heat = calm() ? hand.heat + (target - hand.heat) * (1.0F - (float)Math.exp(-dt * 5.0F)) : target;
      }
   }

   private static void retract(ViewModels.Hand hand, WeaponDef def, Pose pose) {
      float leave = hand.anim.leaveProgress();
      if (def.extend != null && !(leave < 0.0F)) {
         float k = Math.max(0.001F, 1.0F - leave * 1.8F);
         float[] e = pose.parts.get(def.extend);
         if (e == null) {
            pose.parts.put(def.extend, new float[]{0.0F, 0.0F, k, 1.0F});
         } else {
            e[2] = Math.min(e[2], k);
         }
      }
   }

   private void effects(
      ViewModels.Hand hand, WeaponDef def, WeaponSettings s, Pose pose, float scale, boolean mirrored, boolean pixel, float[] arcs, float[] otherArcs
   ) {
      if (!WeaponsModule.maskPass) {
         boolean softPixel = this.module.glowStyle.get() == WeaponOptions.GlowStyle.SOFT || this.module.drawnAsHd(def);
         boolean soft = !pixel || softPixel;
         int glowSide = this.compareFx && !softPixel ? 1 : 0;
         int accent = s.accent != null ? s.accent.getColor(0.5F) : def.accentColor;
         float out = 1.0F;
         float[] e;
         if (def.glowPart != null && (e = pose.parts.get(def.glowPart)) != null) {
            out = e[2];
         }

         if (def.glow != null) {
            float[] g = def.glow;
            float[] a = this.point(g[0], g[1], g[2], mirrored);
            if (out > 0.02F) {
               float[] b = this.point(g[0] + (g[3] - g[0]) * out, g[1] + (g[4] - g[1]) * out, g[2] + (g[5] - g[2]) * out, mirrored);
               float flare = Math.max(Math.max(0.0F, pose.flare), def.flareOnHit ? hand.hitFlash : 0.0F);
               float strength = (s.glow != null ? s.glow.getFloat() : 1.0F) * (1.0F + 1.4F * Math.min(1.0F, flare));
               if (def.core != null && def.heatIdle > 0.0F) {
                  strength *= Math.min(1.0F, hand.heat / def.heatIdle * 4.0F);
               }

               if (soft || this.compareFx) {
                  this.fp.fx.side = glowSide;
                  this.fp.fx.beam(a, b, g[6] * scale, accent, 0.42F * strength);
                  this.fp.fx.beam(a, b, g[6] * scale * 0.38F, mix(accent, 16777215, 0.45F), 0.38F * strength);
                  this.fp.fx.side = 0;
               }
            }

            if (hand.ignite) {
               hand.ignite = false;
               float[] tip = this.point(g[3], g[4], g[5], mirrored);
               float[] dir = new float[]{tip[0] - a[0], tip[1] - a[1], tip[2] - a[2]};
               Mat4.normalize(dir);
               this.fp.fx.sparks(a, dir, calm() ? 5 : 16, scale, mix(accent, 16777215, calm() ? 0.2F : 0.6F));
            }
         }

         if (def.bow != null && def.bow.stringGlow && (soft || this.compareFx)) {
            this.fp.fx.side = glowSide;
            this.stringGlow(hand, def, pose, accent, scale, mirrored, s);
            this.fp.fx.side = 0;
         }

         if (def.motes != null && out > 0.5F) {
            this.motes(hand, def, accent, scale, mirrored, Math.max(pose.flare, def.flareOnHit ? hand.hitFlash : 0.0F));
         }

         if (hand.emberBurst) {
            hand.emberBurst = false;
            this.emberBurst(def, accent, scale, mirrored);
         }

         if (hand.dust) {
            hand.dust = false;
            if (def.dust != null) {
               this.fp.fx.dust(this.point(def.dust[0], def.dust[1], def.dust[2], mirrored), scale, calm());
            }
         }

         if (hand.leaves > 0) {
            int n = hand.leaves;
            hand.leaves = 0;
            float[] t = def.trailSpan;
            if (t != null) {
               this.fp.fx.leaves(this.point(t[0], t[1], t[2], mirrored), this.point(t[3], t[4], t[5], mirrored), calm() ? Math.max(1, n / 2) : n, scale);
            }
         }

         if (arcs != null) {
            this.arcs(hand, def, s, pose, accent, scale, mirrored, arcs, pixel, otherArcs);
         }

         if (def.shimmer != null && hand.heat > 0.05F && (s.heatShimmer == null || s.heatShimmer.get()) && !pixel) {
            float[] gx = def.shimmer;
            this.fp.fx.shimmer(this.point(gx[0], gx[1], gx[2], mirrored), this.point(gx[3], gx[4], gx[5], mirrored), gx[6] * scale, hand.heat);
         }

         this.trail(hand, def, s, accent, out, scale, mirrored);
      }
   }

   private void arcs(
      ViewModels.Hand hand, WeaponDef def, WeaponSettings s, Pose pose, int accent, float scale, boolean mirrored, float[] g, boolean pixel, float[] other
   ) {
      float dt = hand.lastDt;
      if (hand.arcLeft > 0.0F) {
         hand.arcLeft = hand.arcLeft - dt;
      } else {
         hand.arcWait = hand.arcWait - dt * (hand.anim.swinging() ? 7.0F : 1.0F);
         if (hand.arcWait <= 0.0F) {
            hand.arcLeft = 0.05F + 0.1F * this.random.nextFloat();
            hand.arcWait = (0.35F + 1.3F * this.random.nextFloat()) / Math.max(0.1F, g[7]);
         }
      }

      boolean calm = calm();
      float big = testArcs ? 1.0F : Math.max(Math.min(1.0F, pose.flare), Math.max(hand.zap, hand.hitFlash)) * (calm ? 0.5F : 1.0F);
      if (dt > 0.0F) {
         hand.arcFlicker = this.random.nextFloat();
         hand.arcJitter = this.random.nextFloat();
      }

      float flicker = calm ? 0.6F : 0.55F + 0.45F * hand.arcFlicker;
      float strength = Math.max(hand.arcLeft > 0.0F ? flicker : 0.0F, big * (calm ? 0.9F : 0.8F + 0.2F * hand.arcJitter));
      float glow = s.glow != null ? s.glow.getFloat() : 1.0F;
      if (strength * glow <= 0.01F) {
         hand.arcSparks = false;
      } else {
         hand.arcClock = hand.arcClock + dt;
         float f3 = hand.arcClock;
         float f4 = calm ? 0.12F : 0.04F;
         if (f3 >= f4) {
            hand.arcClock = 0.0F;
            hand.arcSeed = this.random.nextInt();
         }

         boolean both = other != null && other != g;
         float[][] at = this.arcAt(hand, def, pose, accent, scale, mirrored, g, big, strength * glow, calm, both ? (pixel ? 2 : 1) : 0);
         if (both) {
            this.arcAt(hand, def, pose, accent, scale, mirrored, other, big, strength * glow, calm, pixel ? 1 : 2);
         }

         if (hand.arcSparks) {
            hand.arcSparks = false;
            float[] va = at[0];
            float[] vb = at[1];
            float[] vbow = at[2];
            float[] dir = new float[]{vbow[0] - (va[0] + vb[0]) * 0.5F, vbow[1] - (va[1] + vb[1]) * 0.5F, vbow[2] - (va[2] + vb[2]) * 0.5F};
            Mat4.normalize(dir);
            this.fp.fx.sparks(vbow, dir, calm ? 4 : 12, scale, mix(accent, 16777215, calm ? 0.25F : 0.65F));
         }
      }
   }

   private float[][] arcAt(
      ViewModels.Hand hand, WeaponDef def, Pose pose, int accent, float scale, boolean mirrored, float[] g, float big, float strength, boolean calm, int side
   ) {
      float[] a = new float[]{g[0], g[1], g[2]};
      float[] b = new float[]{g[3], g[4], g[5]};
      float span = (float)Math.sqrt((g[3] - g[0]) * (g[3] - g[0]) + (g[4] - g[1]) * (g[4] - g[1]) + (g[5] - g[2]) * (g[5] - g[2]));
      float[] bow = new float[]{(g[0] + g[3]) * 0.5F, (g[1] + g[4]) * 0.5F + span * (0.3F + 0.7F * big), (g[2] + g[5]) * 0.5F};
      WeaponRenderer.posedPoint(def, pose.parts, def.arcPart, a);
      WeaponRenderer.posedPoint(def, pose.parts, def.arcPart, b);
      WeaponRenderer.posedPoint(def, pose.parts, def.arcPart, bow);
      float[] va = this.point(a[0], a[1], a[2], mirrored);
      float[] vb = this.point(b[0], b[1], b[2], mirrored);
      float[] vbow = this.point(bow[0], bow[1], bow[2], mirrored);
      float width = g[6] * scale * (1.0F + 0.5F * big);
      this.fp.fx.side = side;
      this.fp.fx.arc(va, vb, vbow, width, accent, strength, hand.arcSeed, 0.26F + 0.14F * big);
      if (big > 0.35F && !calm) {
         this.fp.fx.arc(va, vb, vbow, width * 0.6F, accent, strength * 0.8F, hand.arcSeed * 31 + 7, 0.4F);
      }

      this.fp.fx.side = 0;
      return new float[][]{va, vb, vbow};
   }

   private static boolean calm() {
      return FuriahsVisuals.instance != null && FuriahsVisuals.instance.isReduceFlashing();
   }

   private void trail(ViewModels.Hand hand, WeaponDef def, WeaponSettings s, int accent, float out, float scale, boolean mirrored) {
      WeaponOptions.Trail mode = s.trail != null ? s.trail.get() : WeaponOptions.Trail.OFF;
      boolean on = mode == WeaponOptions.Trail.SUBTLE || mode == WeaponOptions.Trail.STRONG || mode == WeaponOptions.Trail.DEFAULT && !"none".equals(def.trail);
      int index = hand.left ? 1 : 0;
      if (on && hand.anim.swinging() && !(out < 0.5F)) {
         if (hand.newSwing) {
            hand.newSwing = false;
            this.fp.fx.cutTrail(index);
         }

         float[] span = def.trailSpan;
         if (def.trailSpan == null) {
            float[] bb = def.bounds;
            span = new float[]{0.0F, bb[1] + (bb[4] - bb[1]) * 0.6F, 0.0F, 0.0F, bb[4], 0.0F};
         }

         float[] base = this.point(span[0], span[1], span[2], mirrored);
         float[] tip = this.point(span[3], span[4], span[5], mirrored);
         int color = s.trailColor != null ? s.trailColor.getColor(0.5F) : accent;
         boolean embers = "embers".equals(def.trail);
         if (embers) {
            float k = mode == WeaponOptions.Trail.SUBTLE ? 0.5F : (mode == WeaponOptions.Trail.STRONG ? 1.6F : 1.0F);
            hand.glintClock = hand.glintClock + hand.lastDt * 40.0F * k;

            for (int n2 = 0; hand.glintClock >= 1.0F && n2 < 4; n2++) {
               hand.glintClock = hand.glintClock - 1.0F;
               float f22 = this.random.nextFloat();
               float[] at = new float[]{base[0] + (tip[0] - base[0]) * f22, base[1] + (tip[1] - base[1]) * f22, base[2] + (tip[2] - base[2]) * f22};
               this.fp.fx.mote(at, scale, color, 0.06F, "ember");
            }

            hand.glintClock = Math.min(hand.glintClock, 1.0F);
         }

         if (!"ice".equals(def.trail)) {
            float life;
            float strength;
            if (mode == WeaponOptions.Trail.SUBTLE) {
               strength = 0.4F;
               life = 0.12F;
            } else if (mode == WeaponOptions.Trail.STRONG) {
               strength = 1.0F;
               life = 0.16F;
            } else if ("light".equals(def.trail)) {
               strength = 0.85F;
               life = 0.1F;
            } else if ("glow".equals(def.trail)) {
               strength = 0.5F;
               life = 0.17F;
            } else if (embers) {
               strength = 0.55F;
               life = 0.08F;
            } else {
               strength = 0.6F;
               life = 0.13F;
            }

            this.fp.fx.trail(index, tip, base, color, strength, life);
         } else {
            float k = mode == WeaponOptions.Trail.SUBTLE ? 0.5F : (mode == WeaponOptions.Trail.STRONG ? 1.6F : 1.0F);
            hand.glintClock = hand.glintClock + hand.lastDt * 70.0F * k;

            for (int n3 = 0; hand.glintClock >= 1.0F && n3 < 6; n3++) {
               hand.glintClock = hand.glintClock - 1.0F;
               float f22 = this.random.nextFloat();
               float[] at = new float[]{base[0] + (tip[0] - base[0]) * f22, base[1] + (tip[1] - base[1]) * f22, base[2] + (tip[2] - base[2]) * f22};
               this.fp.fx.mote(at, scale, color, 0.0F, "glint");
            }

            hand.glintClock = Math.min(hand.glintClock, 1.0F);
         }
      }
   }

   private void stringGlow(ViewModels.Hand hand, WeaponDef def, Pose pose, int accent, float scale, boolean mirrored, WeaponSettings s) {
      WeaponDef.Bow b = def.bow;
      float extra = Math.max(0.0F, pose.bend);
      float pull = hand.bow.pull() + extra;
      float k = b.bend * (hand.bow.bend() + extra);
      float[] nock = this.point(b.nock[0] - b.draw * Math.max(0.0F, pull), b.nock[1], hand.bow.nocked() ? b.nock[2] : 0.0F, mirrored);
      float strength = (s.glow != null ? s.glow.getFloat() : 1.0F) * b.glow(pull, pose.flare);

      for (int i = 0; i < 2; i++) {
         float ty = b.tips[i * 3 + 1];
         float sg = ty < 0.0F ? -1.0F : 1.0F;
         float d = Math.max(0.0F, Math.abs(ty) - b.from);
         float[] tip = this.point(b.tips[i * 3] - k * d * d, ty - sg * 0.6667F * k * k * d * d * d, b.tips[i * 3 + 2], mirrored);
         this.fp.fx.beam(tip, nock, 0.012F * scale, accent, 0.3F * strength);
      }
   }

   private float[] point(float x, float y, float z, boolean mirrored) {
      float[] p = new float[4];
      this.weapon.transform(x, y, z, p);
      if (mirrored) {
         p[0] = -p[0];
      }

      return p;
   }

   private static int mix(int a, int b, float k) {
      int r = Math.round((a >> 16 & 0xFF) * (1.0F - k) + (b >> 16 & 0xFF) * k);
      int g = Math.round((a >> 8 & 0xFF) * (1.0F - k) + (b >> 8 & 0xFF) * k);
      int bl = Math.round((a & 0xFF) * (1.0F - k) + (b & 0xFF) * k);
      return r << 16 | g << 8 | bl;
   }

   private void exhaust(ViewModels.Hand hand, WeaponDef def, float scale, boolean mirrored) {
      if (!WeaponsModule.maskPass) {
         float amount = hand.smoke;
         hand.smoke = 0.0F;
         float[] e = def.exhaust;
         if (e != null) {
            float[] at = new float[4];
            float[] dir = new float[3];
            this.weapon.transform(e[0], e[1], e[2], at);
            this.weapon.direction(e[3], e[4], e[5], dir);
            Mat4.normalize(dir);
            if (mirrored) {
               at[0] = -at[0];
               dir[0] = -dir[0];
            }

            this.fp.fx.smoke(at, dir, amount, scale);
         }
      }
   }

   private void reachPart(WeaponDef def, Pose pose, float[] lp, float[] h) {
      if (pose.lAtPart != null && !(pose.lAt <= 0.0F)) {
         WeaponDef.Pivot pv = def.pivots.get(pose.lAtPart);
         if (pv != null) {
            float[] part = pose.parts.get(pose.lAtPart);
            float al = (float)Math.sqrt(pv.axis[0] * pv.axis[0] + pv.axis[1] * pv.axis[1] + pv.axis[2] * pv.axis[2]);
            float s = part != null && !(al < 1.0E-6F) ? part[1] / al : 0.0F;
            this.weapon.transform(pv.origin[0] + pv.axis[0] * s, pv.origin[1] + pv.axis[1] * s, pv.origin[2] + pv.axis[2] * s, this.v);
            float k = Math.min(1.0F, pose.lAt);
            k = k * k * (3.0F - 2.0F * k);
            this.weapon.direction(0.0F, 1.0F, 0.0F, this.q);
            Mat4.normalize(this.q);

            for (int i = 0; i < 3; i++) {
               lp[i] += (this.v[i] - lp[i]) * k;
               h[i] += (this.q[i] - h[i]) * k;
            }

            Mat4.normalize(h);
         }
      }
   }

   private void dangle(ViewModels.Hand hand, WeaponDef def, float cameraPitch) {
      if (!def.dangle.isEmpty()) {
         float pr = (float)Math.toRadians(cameraPitch);
         float gx = 0.0F;
         float gy = -((float)Math.cos(pr));
         float gz = -((float)Math.sin(pr));
         float mx = this.weapon.m[0] * gx + this.weapon.m[1] * gy + this.weapon.m[2] * gz;
         float my = this.weapon.m[4] * gx + this.weapon.m[5] * gy + this.weapon.m[6] * gz;
         float target = (float)Math.toDegrees(Math.atan2(mx, -my));

         for (Entry<String, Float> e : def.dangle.entrySet()) {
            float[] d = hand.dangles.get(e.getKey());
            if (d == null) {
               d = new float[]{target * e.getValue(), 0.0F, 0.0F};
               hand.dangles.put(e.getKey(), d);
            }

            d[2] = Math.max(-85.0F, Math.min(85.0F, target * e.getValue()));
            float[] clip = hand.pose.parts.get(e.getKey());
            Map<String, float[]> map = hand.pose.parts;
            String string = e.getKey();
            float[] fArray;
            if (clip == null) {
               float[] fArray2 = new float[]{d[0], 0.0F, 1.0F, 0.0F};
               fArray = fArray2;
               fArray2[3] = 1.0F;
            } else {
               float[] fArray3 = new float[]{clip[0] + d[0], clip[1], clip[2], 0.0F};
               fArray = fArray3;
               fArray3[3] = clip[3];
            }

            map.put(string, fArray);
         }
      }
   }

   private void eye(ViewModels.Hand hand, WeaponDef def, boolean mirrored) {
      WeaponDef.Eye e = def.eye;
      this.weapon.transform(e.at[0], e.at[1], e.at[2], this.eyeAt);
      float far = this.lookFar;
      float lx = far * (float)Math.tan(Math.toRadians(Math.max(-22.0F, Math.min(22.0F, this.turnYaw * 0.06F))));
      float ly = -far * (float)Math.tan(Math.toRadians(Math.max(-16.0F, Math.min(16.0F, this.turnPitch * 0.06F))));
      if (mirrored) {
         lx = -lx;
      }

      hand.eye
         .update(
            hand.lastDt,
            this.toModel(lx - this.eyeAt[0], ly - this.eyeAt[1], -far - this.eyeAt[2], this.eyeLook),
            this.toModel(-this.eyeAt[0], -this.eyeAt[1], -this.eyeAt[2], this.eyeHolder),
            !hand.guarding,
            e.pixels
         );
      hand.eye.poses(e, hand.pose.parts);
   }

   private float[] toModel(float x, float y, float z, float[] out) {
      float mx = this.weapon.m[0] * x + this.weapon.m[1] * y + this.weapon.m[2] * z;
      float my = this.weapon.m[4] * x + this.weapon.m[5] * y + this.weapon.m[6] * z;
      float mz = this.weapon.m[8] * x + this.weapon.m[9] * y + this.weapon.m[10] * z;
      float l = (float)Math.sqrt(mx * mx + my * my + mz * mz);
      if (l < 1.0E-6F) {
         out[0] = 0.0F;
         out[1] = 0.0F;
         out[2] = 1.0F;
      } else {
         out[0] = mx / l;
         out[1] = my / l;
         out[2] = mz / l;
      }

      return out;
   }

   private void turning(EntityPlayerSP p, float pt, float dt) {
      float yaw = p.prevRotationYaw + (p.rotationYaw - p.prevRotationYaw) * pt;
      float pitch = p.prevRotationPitch + (p.rotationPitch - p.prevRotationPitch) * pt;
      if (!this.turnFresh && dt > 0.0F) {
         float k = 1.0F - (float)Math.exp(-dt / 0.08F);
         this.turnYaw = this.turnYaw + (MathHelper.wrapAngleTo180_float(yaw - this.lastYaw) / dt - this.turnYaw) * k;
         this.turnPitch = this.turnPitch + ((pitch - this.lastPitch) / dt - this.turnPitch) * k;
      }

      this.turnFresh = false;
      this.lastYaw = yaw;
      this.lastPitch = pitch;
      MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
      this.lookFar = hit != null && hit.hitVec != null && hit.typeOfHit != MovingObjectType.MISS
         ? Math.max(1.5F, Math.min(20.0F, (float)hit.hitVec.distanceTo(p.getPositionEyes(pt))))
         : 20.0F;
   }

   private float emptyHandShare(Pose pose, Motion m) {
      WeaponOptions.EmptyHand e = this.module.emptyHand.get();
      if (e == WeaponOptions.EmptyHand.OFF) {
         return 0.0F;
      } else {
         return e == WeaponOptions.EmptyHand.ALWAYS ? 1.0F : Math.max(pose.lShow, m.sprintHand);
      }
   }

   private void arm(ViewModels.Body b, boolean rightArm, float[] at, float[] handle, float[] shoulder, float scale, boolean mirrored, boolean leftAnchored) {
      this.arm(b, rightArm, at, handle, shoulder, scale, mirrored, leftAnchored, false);
   }

   private void arm(
      ViewModels.Body b, boolean rightArm, float[] at, float[] handle, float[] shoulder, float scale, boolean mirrored, boolean leftAnchored, boolean exact
   ) {
      boolean slim = b.slim;
      boolean drawRight = rightArm != mirrored;
      FirstPersonRenderer.Item it = this.fp.addArm(slim, drawRight, drawRight ? b.sleeveRight : b.sleeveLeft, leftAnchored, false);
      if (it != null) {
         if (mirrored) {
            float[] a = new float[]{-at[0], at[1], at[2]};
            float[] hh = new float[]{-handle[0], handle[1], handle[2]};
            float[] sh = new float[]{-shoulder[0], shoulder[1], shoulder[2]};
            Rig.arm(a, hh, sh, slim, drawRight, scale, exact, it.matrix);
         } else {
            Rig.arm(at, handle, shoulder, slim, drawRight, scale, exact, it.matrix);
         }
      }
   }

   private void drawVanillaLeft(Minecraft mc, EntityPlayerSP player, ItemStack stack, boolean mirrored, float pt) {
      GlStateManager.pushMatrix();

      try {
         if (mirrored) {
            GlStateManager.scale(-1.0F, 1.0F, 1.0F);
            GL11.glFrontFace(2304);
         }

         if (player != null) {
            float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * pt;
            float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * pt;
            GlStateManager.pushMatrix();
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
            RenderHelper.enableStandardItemLighting();
            GlStateManager.popMatrix();
            int light = mc.theWorld
               .getCombinedLight(new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ), 0);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >> 16);
         } else {
            RenderHelper.enableStandardItemLighting();
         }

         GlStateManager.enableRescaleNormal();
         float s = this.vanillaLeftSwing < 0.0F ? 0.0F : this.vanillaLeftSwing;
         float root = MathHelper.sqrt_float(s);
         float pi = (float) Math.PI;
         GlStateManager.translate(
            -0.4F * MathHelper.sin(root * pi), 0.2F * MathHelper.sin(root * pi * 2.0F), -0.2F * MathHelper.sin(s * pi)
         );
         GlStateManager.translate(0.61F, -0.56F, -0.72F);
         GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
         float f = MathHelper.sin(s * s * pi);
         float f1 = MathHelper.sin(root * pi);
         GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
         GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
         GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
         GlStateManager.scale(0.4F, 0.4F, 0.4F);
         if (player != null) {
            mc.getItemRenderer().renderItem(player, stack, TransformType.FIRST_PERSON);
         } else {
            mc.getRenderItem().renderItem(stack, TransformType.FIRST_PERSON);
         }
      } finally {
         GL11.glFrontFace(2305);
         GlStateManager.disableRescaleNormal();
         RenderHelper.disableStandardItemLighting();
         GlStateManager.popMatrix();
      }
   }

   private static float sizeFirst(WeaponSettings s) {
      return s.sizeFirst != null ? s.sizeFirst.getFloat() : 1.0F;
   }

   private static WeaponDef.Hold holdFor(WeaponDef def, boolean dual) {
      if (testHold != null) {
         return testHold;
      } else {
         return dual && def.dualHold != null ? def.dualHold : def.firstPerson;
      }
   }

   static void hold(WeaponDef def, WeaponSettings s, boolean dual, float[] pos, float[] tip, float[] edge) {
      WeaponDef.Hold hold = holdFor(def, dual);
      float[] o = placement(s);
      pos[0] = hold.pos[0] + o[0];
      pos[1] = hold.pos[1] + o[1];
      pos[2] = hold.pos[2] + o[2];
      System.arraycopy(hold.tip, 0, tip, 0, 3);
      System.arraycopy(hold.edge, 0, edge, 0, 3);
      float rp = o[3];
      float ry = o[4];
      float rr = o[5];
      if (rp != 0.0F || ry != 0.0F || rr != 0.0F) {
         Mat4 r = new Mat4();
         r.rotate(ry, 0.0F, 1.0F, 0.0F).rotate(rp, 1.0F, 0.0F, 0.0F);
         float[] t = new float[3];
         Mat4.normalize(tip);
         Mat4 roll = new Mat4().rotate(rr, tip[0], tip[1], tip[2]);
         roll.direction(edge[0], edge[1], edge[2], t);
         System.arraycopy(t, 0, edge, 0, 3);
         r.direction(tip[0], tip[1], tip[2], t);
         System.arraycopy(t, 0, tip, 0, 3);
         r.direction(edge[0], edge[1], edge[2], t);
         System.arraycopy(t, 0, edge, 0, 3);
      }
   }

   static void aimBow(WeaponDef def, WeaponDef.Hold hold, float k, float[] pos, float[] tip, float[] edge) {
      if (!(k <= 0.0F)) {
         WeaponDef.Hold aim = def.bow.aim;

         for (int i = 0; i < 3; i++) {
            pos[i] += (aim.pos[i] - hold.pos[i]) * k;
            tip[i] += (aim.tip[i] - tip[i]) * k;
            edge[i] += (aim.edge[i] - edge[i]) * k;
         }
      }
   }

   static void bowHand(WeaponDef def, BowDraw d, float onString, float pull, float[] out) {
      WeaponDef.Bow b = def.bow;
      float on = Math.max(d.handOn(), Math.min(1.0F, onString));
      float dip = d.dip();
      pull = Math.max(0.0F, pull);
      float nx = b.nock[0] - b.draw * pull;
      float ny = b.nock[1];
      float nz = b.nock[2];
      float rx = b.nock[0] + b.rest[0];
      float ry = b.nock[1] + b.rest[1];
      float rz = b.nock[2] + b.rest[2];
      out[0] = rx + (nx - rx) * on + 0.04F * dip;
      out[1] = ry + (ny - ry) * on - 0.2F * dip;
      out[2] = rz + (nz - rz) * on + 0.06F * dip;
   }

   static float[] placement(WeaponSettings s) {
      WeaponOptions.Placement p = s.placement.get();
      switch (p) {
         case MINIMAL:
            return new float[]{0.04F, -0.07F, -0.05F, -4.0F, 0.0F, 0.0F, 0.82F};
         case CENTERED:
            return new float[]{-0.14F, 0.02F, 0.0F, 0.0F, 8.0F, 0.0F, 1.0F};
         case CUSTOM:
            return new float[]{
               s.offsetX.getFloat(), s.offsetY.getFloat(), s.offsetZ.getFloat(), s.pitch.getFloat(), s.yaw.getFloat(), s.roll.getFloat(), s.size.getFloat()
            };
         default:
            return new float[]{0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
      }
   }

   static float placementScale(WeaponSettings s) {
      return placement(s)[6];
   }

   static final class Body {
      boolean slim;
      boolean sleeveRight = true;
      boolean sleeveLeft = true;
      ItemStack held;
      float walked;
      float pitch;

      ViewModels.Body of(AbstractClientPlayer p) {
         this.slim = "slim".equals(p.getSkinType());
         this.sleeveRight = FirstPersonRenderer.sleeve(p, true);
         this.sleeveLeft = FirstPersonRenderer.sleeve(p, false);
         this.held = p.getHeldItem();
         this.walked = p.distanceWalkedModified;
         this.pitch = p.rotationPitch;
         return this;
      }
   }

   final class Hand {
      final boolean left;
      WeaponDef def;
      WeaponSettings settings;
      final Animator anim = new Animator();
      final Motion motion = new Motion();
      final Pose pose = new Pose();
      final BowDraw bow = new BowDraw();
      float previewUse;
      private int swingCount;
      private Clip lastSwing;
      private boolean dualGuard;
      private final Map<String, float[]> dangles = new HashMap<>();
      final Engine engine = new Engine();
      private float smoke;
      private float wisp;
      private boolean newSwing;
      private boolean ignite;
      private boolean emberBurst;
      private boolean dust;
      private int leaves;
      final EyeMotion eye = new EyeMotion();
      private boolean partsDue;
      private boolean guarding;
      private float lastDt;
      private float moteClock;
      private float glintClock;
      private float arcLeft;
      private float arcWait = 0.5F;
      private float arcClock;
      private float hitFlash;
      private float zap;
      private int arcSeed;
      private boolean arcSparks;
      private float arcFlicker;
      private float arcJitter;
      private float heatKick;
      private float heatFlush;
      private float heat;
      private final Mat4[] past = new Mat4[]{new Mat4(), new Mat4(), new Mat4()};
      private int pastCount;
      private long pastAt;
      private float blur;
      private final float[] ghostAlpha = new float[3];
      private int ghosts;
      private WeaponAssets ghostAssets;
      private boolean ghostMirrored;
      private final WeaponRenderer.Look ghostLook = new WeaponRenderer.Look();
      private boolean shownPixel;
      private boolean switchFrom;
      private WeaponDef styleDef;
      private long switchStart = -1L;
      static final long SWITCH_NANOS = 250000000L;

      boolean styleShown(WeaponDef d, boolean want, boolean animate) {
         long now = ViewModels.this.nanos();
         if (d != this.styleDef) {
            this.styleDef = d;
            this.shownPixel = want;
            this.switchStart = -1L;
            return want;
         } else {
            if (want != this.shownPixel) {
               if (animate) {
                  boolean onScreen = this.switchStart >= 0L && now - this.switchStart < 125000000L ? this.switchFrom : this.shownPixel;
                  if (onScreen != want) {
                     this.switchStart = now;
                     this.switchFrom = onScreen;
                  }
               }

               this.shownPixel = want;
            }

            if (this.switchStart >= 0L) {
               long t = now - this.switchStart;
               if (t >= 250000000L) {
                  this.switchStart = -1L;
                  return want;
               } else {
                  return t < 125000000L ? this.switchFrom : want;
               }
            } else {
               return want;
            }
         }
      }

      float mosaic() {
         if (this.switchStart < 0L) {
            return 0.0F;
         } else {
            float t = Math.min(1.0F, (float)(ViewModels.this.nanos() - this.switchStart) / 2.5E8F);
            float k = t < 0.5F ? t * 2.0F : (1.0F - t) * 2.0F;
            return k * k * (3.0F - 2.0F * k);
         }
      }

      void hit() {
         if (this.def != null && (this.def.arcs != null || this.def.flareOnHit)) {
            this.hitFlash = 1.0F;
            this.arcSparks = this.def.arcs != null;
         }
      }

      void stepDangles(float dt) {
         if (this.def != null && !this.def.dangle.isEmpty()) {
            int n = Math.max(1, (int)Math.ceil(dt / 0.01F));
            float h = dt / n;

            for (Entry<String, Float> e : this.def.dangle.entrySet()) {
               float[] d = this.dangles.get(e.getKey());
               if (d != null) {
                  for (int i = 0; i < n; i++) {
                     float acc = -70.0F * (d[0] - d[2]) - 0.44F * (float)Math.sqrt(70.0) * d[1];
                     d[1] += acc * h;
                     d[0] += d[1] * h;
                  }

                  d[0] = Math.max(-85.0F, Math.min(85.0F, d[0]));
               }
            }
         }
      }

      Hand(boolean left) {
         this.left = left;
         this.anim.events = new Animator.Events() {
            @Override
            public void event(Clip clip, Clip.Event event) {
               ViewModels.this.onEvent(Hand.this, event);
            }
         };
      }

      Hand() {
         this(false);
      }

      void set(WeaponDef d, WeaponSettings s, boolean dual) {
         if (d != this.def || dual != this.dualGuard) {
            if (d != this.def) {
               this.anim.reset();
               this.motion.reset();
               this.eye.reset();
            }

            this.def = d;
            this.dualGuard = dual;
            String set = d.animations;
            Clip g = dual ? ViewModels.this.module.clips.find("dual_guard", d.id, set) : null;
            this.anim.setGuard(g != null ? g : ViewModels.this.module.clips.find("guard", d.id, set));
         }

         this.settings = s;
      }

      void set(WeaponDef d, WeaponSettings s) {
         this.set(d, s, false);
      }
   }
}
