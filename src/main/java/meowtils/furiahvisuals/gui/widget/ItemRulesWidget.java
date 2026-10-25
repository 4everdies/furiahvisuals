package meowtils.furiahvisuals.gui.widget;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import meowtils.furiahvisuals.core.ClientClock;
import meowtils.furiahvisuals.gui.Theme;
import meowtils.furiahvisuals.gui.Ui;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.ItemListSetting;
import meowtils.furiahvisuals.setting.ItemRulesSetting;
import meowtils.furiahvisuals.setting.PresetSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public final class ItemRulesWidget extends Widget {
   private static final float HEAD = 26.0F;
   private static final float ROW = 28.0F;
   private final ItemRulesSetting setting;
   private boolean expanded;
   private float open;
   private float turn;
   private final Pill add = new Pill();
   private final Map<ItemRulesSetting.Rule, ColorWidget> pickers = new IdentityHashMap<>();
   private ItemRulesSetting.Rule picking;
   private ColorWidget active;
   private String status;
   private long statusUntil;
   private static final int MAX = 32;
   private final float[] rowY = new float[32];
   private final float[] lookX = new float[32];
   private final float[] lookW = new float[32];
   private final float[] colorX = new float[32];
   private final float[] colorW = new float[32];
   private float removeX;
   private int rows;

   public ItemRulesWidget(ItemRulesSetting setting) {
      this.setting = setting;
   }

   private float bodyHeight() {
      List<ItemRulesSetting.Rule> rules = this.setting.get();
      float h = 6.0F;
      h += rules.isEmpty() ? 18.0F : rules.size() * 28.0F;
      if (this.picking != null && rules.contains(this.picking)) {
         h += this.picker(this.picking).height() + 6.0F;
      }

      return h + 20.0F + 6.0F;
   }

   @Override
   public float height() {
      return 26.0F + this.open * this.bodyHeight();
   }

   @Override
   public boolean isVisible() {
      return this.setting.isVisible();
   }

   private ColorWidget picker(ItemRulesSetting.Rule rule) {
      ColorWidget c = this.pickers.get(rule);
      if (c == null && rule.getColor() != null) {
         c = new ColorWidget(rule.getColor());
         c.setExpanded(true);
         this.pickers.put(rule, c);
      }

      return c;
   }

   private static String lookLabel(PresetSetting presets, String id) {
      if (id == null) {
         return "Keep look";
      } else {
         for (PresetSetting.Chip chip : presets.chips()) {
            if (chip.id.equals(id)) {
               return chip.name;
            }
         }

         return "Missing preset";
      }
   }

   @Override
   public void render(float mx, float my, float dt, int accent) {
      List<ItemRulesSetting.Rule> rules = this.setting.get();
      if (this.picking != null && (!rules.contains(this.picking) || this.picking.getColor() == null)) {
         this.picking = null;
      }

      boolean overHead = mx >= this.x && mx < this.x + this.w && my >= this.y && my < this.y + 26.0F;
      this.animateHover(overHead, dt);
      this.open = Easing.approach(this.open, this.expanded ? 1.0F : 0.0F, 14.0F, dt);
      this.turn = Easing.approach(this.turn, this.expanded ? 1.0F : 0.0F, 16.0F, dt);
      if (this.hover > 0.01F) {
         Ui.roundRect(this.x - 6.0F, this.y + 1.0F, this.w + 12.0F, 24.0F, 6.0F, ColorUtil.mulAlpha(Theme.SURFACE_HOVER, this.hover));
      }

      Ui.textMiddle(this.setting.getLabel(), this.x, this.y + 13.0F, 10.0F, Theme.TEXT, false);
      float nw = Ui.width(this.setting.getLabel(), 10.0F);
      Ui.textMiddle("advanced", this.x + nw + 8.0F, this.y + 13.0F, 7.8F, Theme.TEXT_MUTED, false);
      String count = rules.isEmpty() ? "none" : rules.size() + (rules.size() == 1 ? " rule" : " rules");
      float cw = Ui.width(count, 8.2F);
      Ui.textMiddle(count, this.x + this.w - 18.0F - cw, this.y + 13.0F, 8.2F, rules.isEmpty() ? Theme.TEXT_MUTED : accent, false);
      chevron(this.x + this.w - 6.0F, this.y + 13.0F, this.turn, ColorUtil.lerp(Theme.TEXT_MUTED, accent, 0.4F + 0.6F * this.hover));
      if (!(this.open <= 0.01F)) {
         Ui.pushScissor(this.x - 8.0F, this.y + 26.0F, this.w + 16.0F, this.open * this.bodyHeight());
         Ui.pushAlpha(this.open);
         PresetSetting presets = this.setting.getPresets();
         float ry = this.y + 26.0F + 6.0F;
         this.rows = Math.min(32, rules.size());
         if (rules.isEmpty()) {
            Ui.text("No overrides yet: hold an item and click Add item in hand.", this.x, ry + 2.0F, 8.4F, Theme.TEXT_MUTED);
            ry += 18.0F;
         }

         for (int i = 0; i < this.rows; i++) {
            ItemRulesSetting.Rule rule = rules.get(i);
            this.rowY[i] = ry;
            boolean overRow = mx >= this.x - 6.0F && mx < this.x + this.w + 6.0F && my >= ry && my < ry + 28.0F - 2.0F;
            Ui.surface(this.x, ry, this.w, 24.0F, 7.0F, overRow ? Theme.SURFACE_HOVER : Theme.SURFACE);
            ItemStack stack = ItemListSetting.stackOf(rule.item);
            Ui.item(stack, this.x + 5.0F, ry + 4.0F, 16.0F);
            this.removeX = this.x + this.w - 20.0F;
            Ui.textMiddle("×", this.removeX + 6.0F, ry + 12.0F - 0.5F, 9.4F, overRow && mx >= this.removeX ? Theme.DANGER : Theme.TEXT_MUTED, false);
            float right = this.removeX - 6.0F;
            if (rule.getColor() != null) {
               this.colorW[i] = 34.0F;
               this.colorX[i] = right - this.colorW[i];
               drawSwatch(rule, this.colorX[i], ry + 5.0F, this.colorW[i], 14.0F, rule == this.picking, accent);
            } else {
               String keep = "Keep colors";
               this.colorW[i] = Ui.width(keep, 8.2F) + 14.0F;
               this.colorX[i] = right - this.colorW[i];
               boolean over = mx >= this.colorX[i] && mx < this.colorX[i] + this.colorW[i] && my >= ry && my < ry + 28.0F - 4.0F;
               Ui.roundRect(this.colorX[i], ry + 4.0F, this.colorW[i], 16.0F, 8.0F, over ? Theme.SURFACE_HOVER : Theme.PILL);
               Ui.textMiddle(keep, this.colorX[i] + 7.0F, ry + 12.0F, 8.2F, over ? Theme.TEXT : Theme.TEXT_DIM, false);
            }

            String look = lookLabel(presets, rule.getPreset());
            this.lookW[i] = Math.min(130.0F, Ui.width(look, 8.2F) + 22.0F);
            this.lookX[i] = this.colorX[i] - 6.0F - this.lookW[i];
            boolean overLook = mx >= this.lookX[i] && mx < this.lookX[i] + this.lookW[i] && my >= ry && my < ry + 28.0F - 4.0F;
            Ui.roundRect(
               this.lookX[i],
               ry + 4.0F,
               this.lookW[i],
               16.0F,
               8.0F,
               rule.getPreset() != null ? (overLook ? Theme.ACCENT_SOFT_HOVER : Theme.ACCENT_SOFT) : (overLook ? Theme.SURFACE_HOVER : Theme.PILL)
            );
            Ui.textMiddle(
               Ui.ellipsize(look, 8.2F, this.lookW[i] - 18.0F, false),
               this.lookX[i] + 7.0F,
               ry + 12.0F,
               8.2F,
               rule.getPreset() != null ? Theme.TEXT : Theme.TEXT_DIM,
               false
            );
            Ui.textMiddle("›", this.lookX[i] + this.lookW[i] - 10.0F, ry + 11.5F, 9.0F, Theme.TEXT_MUTED, false);
            String name = stack != null ? stack.getDisplayName() : rule.item;
            Ui.textMiddle(Ui.ellipsize(name, 9.0F, this.lookX[i] - this.x - 34.0F, false), this.x + 27.0F, ry + 12.0F, 9.0F, Theme.TEXT, false);
            ry += 28.0F;
            ColorWidget c;
            if (rule == this.picking && (c = this.picker(rule)) != null) {
               c.x = this.x + 8.0F;
               c.y = ry;
               c.w = this.w - 16.0F;
               c.render(mx, my, dt, accent);
               ry += c.height() + 6.0F;
            }
         }

         String label = "Add item in hand";
         this.add.layout(this.x, ry, label);
         this.add.draw(label, mx, my, dt, accent, true, Minecraft.getMinecraft().thePlayer != null);
         if (this.status != null && ClientClock.millis() < this.statusUntil) {
            float sx = this.add.x + this.add.w + 10.0F;
            Ui.textMiddleHeavy(Ui.ellipsizeHeavy(this.status, 8.2F, this.x + this.w - sx), sx, ry + 10.0F, 8.2F, accent | 0xFF000000);
         }

         Ui.popAlpha();
         Ui.popScissor();
      }
   }

   private static void drawSwatch(ItemRulesSetting.Rule rule, float sx, float sy, float sw, float sh, boolean picking, int accent) {
      int c0 = rule.getColor().getColor(0.0F);
      int c1 = rule.getColor().getColor(1.0F);
      if (rule.getColor().getEffectiveMode() == ColorSetting.Mode.STATIC) {
         Ui.roundRect(sx, sy, sw, sh, 4.5F, c0 | 0xFF000000);
      } else {
         Ui.gradientH(sx, sy, sw, sh, 4.5F, c0 | 0xFF000000, c1 | 0xFF000000);
      }

      Ui.outline(sx, sy, sw, sh, 4.5F, picking ? 1.1F : 0.8F, picking ? accent : Theme.OUTLINE_HOVER);
   }

   private static void chevron(float cx, float cy, float t, int color) {
      double angle = t * Math.PI / 2.0;
      double dx = Math.cos(angle);
      double dy = Math.sin(angle);

      for (int i = 0; i <= 6; i++) {
         float k = i / 6.0F;
         double along = 1.7600000000000002 - k * 3.2 * 1.1 * 0.7071;
         double side = k * 3.2 * 1.1 * 0.7071;

         for (int s = -1; s <= 1; s += 2) {
            Ui.circle((float)(cx + dx * along - dy * side * s), (float)(cy + dy * along + dx * side * s), 0.75F, color);
         }
      }
   }

   @Override
   public boolean mouseDown(float mx, float my, int button) {
      if (!this.contains(mx, my)) {
         return false;
      } else if (my < this.y + 26.0F) {
         if (button == 0) {
            this.expanded = !this.expanded;
         }

         return true;
      } else if (this.open < 0.95F) {
         return true;
      } else {
         List<ItemRulesSetting.Rule> rules = this.setting.get();
         ColorWidget c;
         if (this.picking != null && (c = this.picker(this.picking)) != null && c.contains(mx, my)) {
            this.active = c;
            c.mouseDown(mx, my, button);
            return true;
         } else {
            for (int i = 0; i < this.rows && i < rules.size(); i++) {
               if (!(my < this.rowY[i]) && !(my >= this.rowY[i] + 28.0F - 4.0F)) {
                  ItemRulesSetting.Rule rule = rules.get(i);
                  if (mx >= this.removeX) {
                     if (button == 0) {
                        this.setting.remove(rule);
                        this.pickers.remove(rule);
                     }
                  } else if (mx >= this.colorX[i] && mx < this.colorX[i] + this.colorW[i]) {
                     if (button == 1 && rule.getColor() != null) {
                        this.setting.setColor(rule, false, 0);
                        this.pickers.remove(rule);
                     } else if (button == 0) {
                        if (rule.getColor() == null) {
                           this.setting.setColor(rule, true, -11688193);
                           this.picking = rule;
                        } else {
                           this.picking = this.picking == rule ? null : rule;
                        }
                     }
                  } else if (mx >= this.lookX[i] && mx < this.lookX[i] + this.lookW[i]) {
                     this.setting.cyclePreset(rule, button == 1 ? -1 : 1);
                  }

                  return true;
               }
            }

            if (button == 0 && this.add.contains(mx, my)) {
               Minecraft mc = Minecraft.getMinecraft();
               ItemStack held = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
               String id = ItemListSetting.idOf(held);
               if (id == null) {
                  this.say("Hold an item first");
               } else if (this.setting.find(id) != null) {
                  this.say(held.getDisplayName() + " already has a rule");
               } else {
                  this.setting.add(id);
                  this.say("Added " + held.getDisplayName() + ": pick its look and colors");
               }
            }

            return true;
         }
      }
   }

   private void say(String message) {
      this.status = message;
      this.statusUntil = ClientClock.millis() + 3200L;
   }

   @Override
   public void mouseDrag(float mx, float my) {
      if (this.active != null) {
         this.active.mouseDrag(mx, my);
      }
   }

   @Override
   public void mouseUp(float mx, float my) {
      if (this.active != null) {
         this.active.mouseUp(mx, my);
         if (!this.active.isFocused()) {
            this.active = null;
         }
      }
   }

   @Override
   public boolean isDragging() {
      return this.active != null && this.active.isDragging();
   }

   @Override
   public boolean keyTyped(char c, int key) {
      return this.active != null && this.active.keyTyped(c, key);
   }

   @Override
   public boolean isFocused() {
      return this.active != null && this.active.isFocused();
   }

   @Override
   public void unfocus() {
      if (this.active != null) {
         this.active.unfocus();
         this.active = null;
      }
   }

   @Override
   public String tooltip(float mx, float my) {
      if (!this.contains(mx, my)) {
         return null;
      } else if (my < this.y + 26.0F) {
         return this.setting.getDescription() + (this.expanded ? "" : "  (click to open)");
      } else {
         List<ItemRulesSetting.Rule> rules = this.setting.get();

         for (int i = 0; i < this.rows && i < rules.size(); i++) {
            if (!(my < this.rowY[i]) && !(my >= this.rowY[i] + 28.0F - 4.0F)) {
               if (mx >= this.removeX) {
                  return "Remove this rule";
               }

               if (mx >= this.colorX[i] && mx < this.colorX[i] + this.colorW[i]) {
                  return rules.get(i).getColor() == null ? "Give this item its own colors" : "Edit this item's colors (right-click: back to the glow's colors)";
               }

               if (mx >= this.lookX[i] && mx < this.lookX[i] + this.lookW[i]) {
                  return "This item's look: a preset, or Keep look (click: next, right-click: previous)";
               }
            }
         }

         return this.add.contains(mx, my) ? "Makes a rule for the item you are holding" : null;
      }
   }
}
