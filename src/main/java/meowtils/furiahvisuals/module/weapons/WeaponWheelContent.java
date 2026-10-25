package meowtils.furiahvisuals.module.weapons;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import meowtils.furiahvisuals.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;

final class WeaponWheelContent implements WheelContent {
   private final WeaponsModule module;
   private final WheelSettings s;
   private final Map<WeaponRule, WheelSlotsSetting.Slot> previous = new WeakHashMap<>();
   private WheelIcons icons;
   private final WheelIcons.Request[] requests = new WheelIcons.Request[32];
   private int requestCount;
   private final WheelIcons.Request center = new WheelIcons.Request();
   private final float[] uv = new float[4];
   private int litIndex = -1;

   WeaponWheelContent(WeaponsModule module, WheelSettings settings) {
      this.module = module;
      this.s = settings;

      for (int i = 0; i < this.requests.length; i++) {
         this.requests[i] = new WheelIcons.Request();
      }
   }

   private WheelIcons icons() {
      if (this.icons == null) {
         this.icons = this.module.wheelIcons();
      }

      return this.icons;
   }

   List<WheelSlotsSetting.Slot> slots() {
      return this.s.slots.get();
   }

   private static ItemStack held() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      return p == null ? null : p.getHeldItem();
   }

   WeaponRule targetRule() {
      return this.targetRule(false);
   }

   WeaponRule targetRule(boolean bow) {
      ItemStack held = held();
      WeaponRule r = this.module.rules.match(held);
      if (!bow) {
         if (r != null) {
            return r;
         } else {
            for (WeaponRule x : this.module.rules.get()) {
               if (x.kind == WeaponRule.Kind.GROUP && x.group == WeaponRule.Group.SWORDS) {
                  return x;
               }
            }

            return null;
         }
      } else if (r != null && r.takesBows() && WeaponRule.isBow(held)) {
         return r;
      } else {
         for (WeaponRule xx : this.module.rules.get()) {
            if (xx.kind == WeaponRule.Kind.GROUP && xx.group == WeaponRule.Group.BOWS || xx.kind == WeaponRule.Kind.ITEM && "minecraft:bow".equals(xx.item)) {
               return xx;
            }
         }

         return null;
      }
   }

   String targetLabel(boolean bow) {
      WeaponRule r = this.targetRule(bow);
      WeaponRule.Group g = bow ? WeaponRule.Group.BOWS : WeaponRule.Group.SWORDS;
      return r == null ? g.displayName + " (new rule)" : r.targetLabel();
   }

   private boolean isBow(WheelSlotsSetting.Slot slot) {
      return slot != null && this.module.isBowWeapon(slot.weapon);
   }

   void remember(WeaponRule r) {
      if (r != null) {
         this.previous.put(r, new WheelSlotsSetting.Slot(r.weapon, r.finish, r.style));
      }
   }

   private boolean apply(WheelSlotsSetting.Slot slot) {
      if (slot != null && this.module.library.get(slot.weapon) != null) {
         boolean bow = this.isBow(slot);
         WeaponRule r = this.targetRule(bow);
         if (r == null) {
            r = WeaponRule.group(bow ? WeaponRule.Group.BOWS : WeaponRule.Group.SWORDS, "vanilla");
            this.module.rules.add(r);
         }

         if (slot.same(r.weapon, r.finish, r.style)) {
            return false;
         } else {
            this.remember(r);
            this.module.rules.setWeapon(r, slot.weapon);
            this.module.rules.setFinish(r, slot.finish);
            this.module.rules.setStyle(r, slot.style);
            if (!bow || WeaponRule.isBow(held())) {
               this.module.playDraw(this.module.library.get(slot.weapon));
            }

            return true;
         }
      } else {
         return false;
      }
   }

   void forgetForTest() {
      this.previous.clear();
   }

   @Override
   public int size() {
      return this.slots().size();
   }

   @Override
   public int perPage() {
      return 0;
   }

   @Override
   public int current() {
      WeaponRule r = this.targetRule();
      String weapon = r == null ? "vanilla" : r.weapon;
      String finish = r == null ? null : r.finish;
      String style = r == null ? null : r.style;
      List<WheelSlotsSetting.Slot> list = this.slots();

      for (int i = 0; i < list.size(); i++) {
         if (list.get(i).same(weapon, finish, style)) {
            return i;
         }
      }

      for (int var7 = 0; var7 < list.size(); var7++) {
         if (list.get(var7).same(weapon, finish)) {
            return var7;
         }
      }

      for (int var8 = 0; var8 < list.size(); var8++) {
         if (list.get(var8).weapon.equals(weapon)) {
            return var8;
         }
      }

      return -1;
   }

   @Override
   public boolean apply(int index) {
      return index >= 0 && index < this.slots().size() && this.apply(this.slots().get(index));
   }

   @Override
   public boolean swapBack() {
      WeaponRule r = this.targetRule();
      WheelSlotsSetting.Slot back = r == null ? null : this.previous.get(r);
      return back != null && this.module.library.get(back.weapon) != null ? this.apply(back) : false;
   }

   private WeaponDef def(int index) {
      if (index < 0) {
         WeaponRule r = this.targetRule();
         return this.module.library.get(r == null ? "vanilla" : r.weapon);
      } else {
         return index < this.slots().size() ? this.module.library.get(this.slots().get(index).weapon) : null;
      }
   }

   @Override
   public String name(int index) {
      WeaponDef d = this.def(index);
      return d == null ? (index < 0 ? "Missing weapon" : "Missing") : d.name;
   }

   @Override
   public String detail(int index) {
      WeaponDef d = this.def(index);
      if (d == null) {
         return "";
      } else if (d.isVanilla()) {
         return "Your resource pack's item";
      } else {
         WeaponOptions.StyleChoice style;
         WeaponOptions.Finish finish;
         if (index < 0) {
            WeaponRule r = this.targetRule();
            finish = r == null ? null : WeaponOptions.Finish.of(r.finish);
            style = WeaponOptions.StyleChoice.of(r == null ? null : r.style);
         } else {
            finish = this.slots().get(index).finishOrNull();
            style = this.slots().get(index).styleChoice();
         }

         WeaponOptions.Finish f = finish != null ? finish : this.module.settingsOf(d).finish.get();
         return d.categoryName() + " · " + f.displayName + " · " + (this.module.pixelOf(d, style) ? "Pixelated" : "HD");
      }
   }

   @Override
   public String target() {
      List<WheelSlotsSetting.Slot> list = this.slots();
      return this.targetLabel(this.litIndex >= 0 && this.litIndex < list.size() && this.isBow(list.get(this.litIndex)));
   }

   @Override
   public boolean usable() {
      return this.module.isEnabled();
   }

   @Override
   public KeyBinding key() {
      return this.s.key.binding();
   }

   @Override
   public void pressed() {
      this.module.preloadWheel();
   }

   @Override
   public void lit(int index) {
      this.litIndex = index;
      WeaponDef d;
      if (index >= 0 && index < this.slots().size() && (d = this.module.library.get(this.slots().get(index).weapon)) != null && !d.isVanilla()) {
         WeaponAssets.of(d, WeaponAssets.Tier.FULL).request(this.module.textureSize());
      }
   }

   @Override
   public void idle() {
      if (this.icons != null) {
         this.icons.idle();
      }
   }

   @Override
   public void prepare(int[] shown, int count, int lit, int middle, int slotPx, int bigPx, boolean live, double time) {
      List<WheelSlotsSetting.Slot> slots = this.slots();
      float liveAngle = (float)(time * 24.0 % 360.0);
      int n = 0;

      for (int i = 0; i < count && n < this.requests.length; i++) {
         int index = shown[i];
         if (index >= 0 && index < slots.size()) {
            WheelSlotsSetting.Slot slot = slots.get(index);
            WheelIcons.Request r = this.requests[n++];
            r.def = this.module.library.get(slot.weapon);
            r.finish = slot.finishOrNull();
            r.pixel = this.module.pixelOf(r.def, slot.styleChoice());
            r.lit = index == lit;
            r.angle = r.lit ? (float)(time * 90.0 % 360.0) : (liveAngle + index * 47.0F) % 360.0F;
            r.cell = index % 32;
         }
      }

      this.requestCount = n;
      WheelIcons.Request c = null;
      if (middle != -2) {
         WheelSlotsSetting.Slot sel = middle >= 0 && middle < slots.size() ? slots.get(middle) : null;
         if (sel == null) {
            WeaponRule r = this.targetRule();
            this.center.def = this.module.library.get(r == null ? "vanilla" : r.weapon);
            this.center.finish = r == null ? null : WeaponOptions.Finish.of(r.finish);
            this.center.pixel = this.module.pixelOf(this.center.def, WeaponOptions.StyleChoice.of(r == null ? null : r.style));
         } else {
            this.center.def = this.module.library.get(sel.weapon);
            this.center.finish = sel.finishOrNull();
            this.center.pixel = this.module.pixelOf(this.center.def, sel.styleChoice());
         }

         this.center.angle = (float)(time * 40.0 % 360.0);
         this.center.lit = true;
         c = this.center;
      }

      this.icons().prepare(this.requests, n, c, slotPx, bigPx, live);
   }

   @Override
   public boolean picture(int index, float x, float y, float size, float alpha) {
      WheelIcons ic = this.icons();
      int tex = ic.atlasTexture();
      int cell = index % 32;
      boolean ready = false;

      for (int i = 0; i < this.requestCount; i++) {
         WheelIcons.Request r = this.requests[i];
         if (r.cell == cell && r.ready && r.def != null) {
            ready = true;
            break;
         }
      }

      if (tex > 0 && ready) {
         ic.cellUv(cell, this.uv);
         image(tex, x - size / 2.0F, y - size / 2.0F, size, size, this.uv[0], this.uv[1], this.uv[2], this.uv[3], alpha);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean bigPicture(int index, float x, float y, float size, float alpha) {
      int tex = this.icons().bigTexture();
      if (tex > 0 && this.center.ready && this.center.def != null) {
         image(tex, x - size / 2.0F, y - size / 2.0F, size, size, 0.0F, 1.0F, 1.0F, 0.0F, alpha);
         return true;
      } else {
         return false;
      }
   }

   private static void image(int tex, float x, float y, float w, float h, float u0, float v0, float u1, float v1, float a) {
      int c = Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
      GlStateManager.tryBlendFuncSeparate(1, 771, 1, 771);
      Ui.image(tex, x, y, w, h, u0, v0, u1, v1, c << 24 | c << 16 | c << 8 | c);
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 771);
   }
}
