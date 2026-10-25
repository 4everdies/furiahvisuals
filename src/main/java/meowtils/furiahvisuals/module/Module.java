package meowtils.furiahvisuals.module;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import meowtils.furiahvisuals.compat.EntityViewRenderEvent;
import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.gui.widget.StartingLookWidget;
import meowtils.furiahvisuals.module.watermark.IslandIcons;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.HasWidget;
import meowtils.furiahvisuals.setting.ModeSetting;
import meowtils.furiahvisuals.setting.PresetSetting;
import meowtils.furiahvisuals.setting.Setting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public abstract class Module {
   private final String id;
   private final String name;
   private final String description;
   private final List<Setting> settings = new ArrayList<>();
   protected final BooleanSetting enabled;
   private static final String[] NONE = new String[0];
   private static final List<Module.HeaderAction> NO_ACTIONS = Collections.emptyList();

   protected Module(String id, String name, String description, boolean enabledByDefault) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.enabled = this.add(new BooleanSetting("enabled", "Enabled", "Turns " + name + " on or off.", enabledByDefault).sectionSwitch());
      this.enabled.addListener(new Runnable() {
         @Override
         public void run() {
            if (Module.this.enabled.get()) {
               Module.this.onEnable();
            } else {
               Module.this.onDisable();
            }
         }
      });
   }

   protected final <S extends Setting> S add(S setting) {
      for (Setting existing : this.settings) {
         if (existing.getId().equals(setting.getId())) {
            throw new IllegalArgumentException("Duplicate setting id '" + setting.getId() + "' in module " + this.id);
         }
      }

      this.settings.add(setting);
      return setting;
   }

   public final String getId() {
      return this.id;
   }

   public String[] getLegacyIds() {
      return NONE;
   }

   public String[] getLegacyNames() {
      return NONE;
   }

   public final String getName() {
      return this.name;
   }

   public final String getDescription() {
      return this.description;
   }

   public final List<Setting> getSettings() {
      return Collections.unmodifiableList(this.settings);
   }

   public final BooleanSetting getEnabledSetting() {
      return this.enabled;
   }

   public final boolean isEnabled() {
      return this.enabled.get();
   }

   public final boolean isSwitchedOn() {
      return this.enabled.get();
   }

   public final void setEnabled(boolean on) {
      this.enabled.set(on);
   }

   public void resetToDefaults() {
      for (Setting setting : this.settings) {
         if (setting != this.enabled) {
            setting.reset();
         }
      }
   }

   public void onTick() {
   }

   public void onPrepareWorld(float partialTicks) {
   }

   public void onRenderWorld(float partialTicks) {
   }

   public void onWorldChange() {
   }

   public void onFogColors(EntityViewRenderEvent.FogColors event) {
   }

   public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
   }

   public void onRenderHand(float partialTicks, int renderPass) {
   }

   public void onAfterHand(float partialTicks) {
   }

   public void onRenderOverlay(float partialTicks) {
   }

   public void onAttack(EntityPlayer attacker, Entity target) {
   }

   public void onChat(String text) {
   }

   public boolean hidesPlayer(EntityPlayer player) {
      return false;
   }

   public void onClientTick() {
   }

   public void onRenderHud(float partialTicks) {
   }

   public boolean onOverlayElement(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      return false;
   }

   public void onOverlayElementPost(RenderGameOverlayEvent.ElementType type, float partialTicks) {
   }

   public void onScreenDrawn(GuiScreen screen) {
   }

   public boolean onScreenMouse(GuiScreen screen) {
      return false;
   }

   public boolean onScreenKey(GuiScreen screen) {
      return false;
   }

   protected void onEnable() {
   }

   protected void onDisable() {
   }

   public int getAccentColor(double time) {
      return -7635713;
   }

   public int getOptionColor(Setting setting, Enum<?> option, double time) {
      return 0;
   }

   public void onOptionHover(Setting setting, Enum<?> option) {
   }

   public float optionArtHeight(Setting setting) {
      return 0.0F;
   }

   public void drawOptionArt(Setting setting, Enum<?> option, float x, float y, float w, float h, double time, boolean selected, int accent) {
   }

   public Module.StartingLooks startingLooks() {
      return null;
   }

   public static Module.StartingLooks styleLooks(final Supplier<List<SliderSetting>> styleTargets) {
      return new Module.StartingLooks() {
         @Override
         public List<SliderSetting> targets() {
            return styleTargets.get();
         }

         @Override
         public double value(SliderSetting slider, int look) {
            return StartingLookWidget.value(slider, look);
         }

         @Override
         public String tooltip(int look) {
            return StartingLookWidget.tip(look);
         }

         @Override
         public String after() {
            return null;
         }

         @Override
         public String rowTooltip() {
            return "A starting point for sizes, strengths and motion, the picked style's own included; colors stay as they are";
         }

         @Override
         public boolean withPage() {
            return true;
         }
      };
   }

   public boolean hasPreview() {
      return false;
   }

   public void renderPreviewBackdrop(float dt, float yaw, float pitch) {
   }

   public void renderPreview(float dt) {
   }

   public void restartPreview() {
   }

   public String[] getPreviewTests() {
      return null;
   }

   public String getPreviewTestTooltip(int index) {
      return null;
   }

   public void runPreviewTest(int index) {
   }

   public String getPreviewLabel() {
      return this.name;
   }

   public String getPreviewInfo() {
      return null;
   }

   public double getPreviewModelOffsetY() {
      return 0.0;
   }

   public double getPreviewFocusX() {
      return 0.0;
   }

   public double getPreviewFocusZ() {
      return 0.0;
   }

   public boolean showsPreviewMannequin() {
      return true;
   }

   public boolean hasTestAction() {
      return false;
   }

   public String getTestTooltip() {
      return null;
   }

   public String testUnavailable() {
      return null;
   }

   public String getTestLabel() {
      return "Test";
   }

   public boolean testClosesMenu() {
      return true;
   }

   public void runTest() {
   }

   public boolean previewsInWorld() {
      return false;
   }

   public boolean drawsWithHudHidden() {
      return false;
   }

   public String summary() {
      for (Setting s : this.settings) {
         ModeSetting m;
         if (s instanceof ModeSetting && ((m = (ModeSetting)s).getStyle() == ModeSetting.Style.CARDS || s instanceof HasWidget)) {
            return ModeSetting.displayName(m.get());
         }

         if (s instanceof PresetSetting) {
            PresetSetting p = (PresetSetting)s;
            String current = p.current();

            for (PresetSetting.Chip c : p.chips()) {
               if (c.id.equals(current)) {
                  return c.name;
               }
            }

            return "Custom";
         }
      }

      return null;
   }

   public boolean hasMoveHud() {
      return false;
   }

   public MiniSections getTabs() {
      return null;
   }

   public List<Module.HeaderAction> headerActions() {
      return NO_ACTIONS;
   }

   public boolean isGroupCollapsible(String group) {
      return false;
   }

   public boolean isGroupCollapsedByDefault(String group) {
      return false;
   }

   public Module.Notice getNotice() {
      return null;
   }

   public String getLoadWarning() {
      return null;
   }

   public boolean afterLoad(JsonObject json) {
      return false;
   }

   public static final class HeaderAction {
      public final String label;
      public final IslandIcons.Icon icon;
      public final String tooltip;
      public final boolean primary;
      public final Runnable action;

      public HeaderAction(String label, IslandIcons.Icon icon, String tooltip, boolean primary, Runnable action) {
         this.label = label;
         this.icon = icon;
         this.tooltip = tooltip;
         this.primary = primary;
         this.action = action;
      }
   }

   public static final class Notice {
      public final String text;
      public final boolean blocking;

      public Notice(String text, boolean blocking) {
         this.text = text;
         this.blocking = blocking;
      }
   }

   public interface StartingLooks {
      List<SliderSetting> targets();

      double value(SliderSetting var1, int var2);

      String tooltip(int var1);

      String after();

      default String rowTooltip() {
         return "A starting point for this style's values; you can fine-tune them after";
      }

      default boolean withPage() {
         return false;
      }
   }
}
