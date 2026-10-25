package meowtils.furiahvisuals.hook;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import net.minecraftforge.client.GuiIngameForge;
import meowtils.furiahvisuals.compat.RenderGameOverlayEvent;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.ui.UiHooks;
import meowtils.furiahvisuals.render.post.PostChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.IChatComponent;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public final class HudHooks {
   static final String[] INGAME_HOOKS = new String[]{"itemName", "afterHud"};
   static final String[] SECTION_HOOKS = new String[]{"actionBar", "scoreboard"};
   static final String[] CHAT_HOOKS = new String[]{"chatDraw", "chatHit", "chatScroll", "chatResetScroll", "chatLineStart", "chatLineEnd"};
   static final String[] SCREEN_HOOKS = new String[]{"chatInit", "chatInput", "chatInputBg", "chatTextBefore", "chatTextAfter"};
   private static final Field CHAT = Hooks.field(GuiIngame.class, "persistantChatGUI", "persistantChatGUI");
   private static final Field TAB = Hooks.field(GuiIngame.class, "overlayPlayerList", "overlayPlayerList");
   private static final Field SLOTS = Hooks.field(Scoreboard.class, "objectiveDisplaySlots", "objectiveDisplaySlots");
   private static final Method RENDER_SCOREBOARD = Hooks.method(
      GuiIngame.class, new String[]{"renderScoreboard", "renderScoreboard"}, ScoreObjective.class, ScaledResolution.class
   );
   private static final RenderGameOverlayEvent.ElementType[] TYPES = RenderGameOverlayEvent.ElementType.values();
   private static final boolean[] CANCELLED = new boolean[TYPES.length];
   private static final boolean[] POST_PENDING = new boolean[TYPES.length];
   private static final ByteBuffer MASK = BufferUtils.createByteBuffer(16);
   private static final boolean[] SAVED_MASK = new boolean[4];
   private static boolean ingameSwapped;
   private static boolean chatSwapped;
   private static boolean screenHooks;
   private static boolean inOverlay;
   private static boolean sectionOverlay;
   private static float pt;
   private static ScaledResolution res;
   private static boolean crossFired;
   private static boolean xpDone;
   private static boolean chatStarted;
   private static boolean actionPushed;
   private static String maskedBy;
   private static ScoreObjective[] sbSlots;
   private static ScoreObjective sbObjective;
   private static ScoreObjective sbSaved1;
   private static ScoreObjective sbSavedTeam;
   private static int sbTeamSlot = -1;
   private static boolean failed;

   private HudHooks() {
   }

   static void install() {
      Minecraft mc = Minecraft.getMinecraft();
      GuiIngame g = mc.ingameGUI;
      HudHooks.FvGuiIngame f = Swap.upgrade(g, GuiIngame.class, HudHooks.FvGuiIngame.class);
      if (f != null) {
         mc.ingameGUI = f;
         ingameSwapped = true;
         Hooks.on(INGAME_HOOKS);
      }

      GuiIngame cur = mc.ingameGUI;
      if (CHAT != null && cur != null) {
         GuiNewChat chat = cur.getChatGUI();
         HudHooks.FvGuiNewChat fc = Swap.upgrade(chat, GuiNewChat.class, HudHooks.FvGuiNewChat.class);
         if (fc != null) {
            if (fc != chat) {
               Swap.set(CHAT, cur, fc);
            }

            chatSwapped = true;
            Hooks.on(CHAT_HOOKS);
         }
      }

      if (TAB != null && cur != null) {
         GuiPlayerTabOverlay tab = cur.getTabList();
         HudHooks.FvGuiPlayerTabOverlay ft = Swap.upgrade(tab, GuiPlayerTabOverlay.class, HudHooks.FvGuiPlayerTabOverlay.class);
         if (ft != null && ft != tab) {
            Swap.set(TAB, cur, ft);
         }
      }

      screenHooks = true;
      Hooks.on(SCREEN_HOOKS);
      Hooks.on(SECTION_HOOKS);
      syncScreen();
   }

   static void uninstall() {
      Minecraft mc = Minecraft.getMinecraft();
      reset();
      screenHooks = false;
      Hooks.off(SCREEN_HOOKS);
      Hooks.off(SECTION_HOOKS);
      Hooks.off(CHAT_HOOKS);
      Hooks.off(INGAME_HOOKS);
      if (Swap.ours(mc.currentScreen, HudHooks.FvGuiChat.class)) {
         mc.currentScreen = (GuiScreen)Swap.downgrade(mc.currentScreen, GuiChat.class);
      }

      GuiIngame cur = mc.ingameGUI;
      if (cur != null) {
         if (CHAT != null && Swap.ours(cur.getChatGUI(), HudHooks.FvGuiNewChat.class)) {
            Swap.set(CHAT, cur, Swap.downgrade(cur.getChatGUI(), GuiNewChat.class));
         }

         if (TAB != null && Swap.ours(cur.getTabList(), HudHooks.FvGuiPlayerTabOverlay.class)) {
            Swap.set(TAB, cur, Swap.downgrade(cur.getTabList(), GuiPlayerTabOverlay.class));
         }

         if (Swap.ours(cur, HudHooks.FvGuiIngame.class)) {
            mc.ingameGUI = Swap.downgrade(cur, GuiIngame.class);
         }
      }

      ingameSwapped = false;
      chatSwapped = false;
   }

   static void syncScreen() {
      if (screenHooks) {
         Minecraft mc = Minecraft.getMinecraft();
         GuiScreen s = mc.currentScreen;
         if (s != null && s.getClass() == GuiChat.class) {
            HudHooks.FvGuiChat f = Swap.upgrade(s, GuiChat.class, HudHooks.FvGuiChat.class);
            if (f == null) {
               screenHooks = false;
               Hooks.off(SCREEN_HOOKS);
            } else {
               mc.currentScreen = f;
               InputHooks.screenReplaced(s, f);
               f.afterInit();
            }
         }
      }
   }

   static void frameStart() {
      syncScreen();
   }

   static void frameEnd() {
      if (inOverlay) {
         reset();
      }
   }

   static void guiStart(float partialTicks) {
      Minecraft mc = Minecraft.getMinecraft();
      if (!ingameSwapped && (!mc.gameSettings.hideGUI || mc.currentScreen != null) && mc.theWorld != null) {
         sectionOverlay = begin(partialTicks);
      }
   }

   static void guiEnd(float partialTicks) {
      if (sectionOverlay) {
         sectionOverlay = false;
         end(partialTicks);
      }
   }

   private static boolean begin(float partialTicks) {
      if (!inOverlay && Bridge.active()) {
         reset();
         inOverlay = true;
         pt = partialTicks;
         res = new ScaledResolution(Minecraft.getMinecraft());
         GuiIngameForge.right_height = 39;
         GuiIngameForge.left_height = 39;

         try {
            Bridge.events().onRenderOverlay(partialTicks);
         } catch (Throwable var2) {
            fail(var2);
         }

         hideScoreboard();
         return true;
      } else {
         return false;
      }
   }

   private static void end(float partialTicks) {
      try {
         flushCrosshair();
         flushXp();
         mask(null);
         if (actionPushed) {
            actionPushed = false;
            GlStateManager.popMatrix();
         }

         drawScoreboard(false);

         for (int i = 0; i < TYPES.length; i++) {
            if (POST_PENDING[i]) {
               POST_PENDING[i] = false;
               post(TYPES[i]);
            }
         }

         Bridge.events().onRenderOverlayPost(RenderGameOverlayEvent.ElementType.ALL, partialTicks);
      } catch (Throwable var5) {
         fail(var5);
      } finally {
         reset();
      }
   }

   private static void reset() {
      mask(null);
      if (actionPushed) {
         actionPushed = false;
         GlStateManager.popMatrix();
      }

      restoreScoreboard();
      sbObjective = null;
      inOverlay = false;
      crossFired = false;
      xpDone = false;
      chatStarted = false;

      for (int i = 0; i < TYPES.length; i++) {
         CANCELLED[i] = false;
         POST_PENDING[i] = false;
      }
   }

   private static boolean pre(RenderGameOverlayEvent.ElementType type) {
      if (inOverlay && Bridge.active()) {
         try {
            boolean c = Bridge.events().onRenderOverlayElement(type, pt);
            CANCELLED[type.ordinal()] = c;
            POST_PENDING[type.ordinal()] = !c;
            return c;
         } catch (Throwable var2) {
            fail(var2);
            return false;
         }
      } else {
         return false;
      }
   }

   private static void post(RenderGameOverlayEvent.ElementType type) {
      if (Bridge.active()) {
         try {
            Bridge.events().onRenderOverlayPost(type, pt);
         } catch (Throwable var2) {
            fail(var2);
         }
      }
   }

   private static void postIfPending(RenderGameOverlayEvent.ElementType type) {
      if (POST_PENDING[type.ordinal()]) {
         POST_PENDING[type.ordinal()] = false;
         post(type);
      }
   }

   public static void elementPost(RenderGameOverlayEvent.ElementType type, float partialTicks) {
      if (Bridge.active()) {
         Bridge.events().onRenderOverlayPost(type, partialTicks);
      }
   }

   static void sectionStart(String name) {
      if (inOverlay) {
         if ("bossHealth".equals(name)) {
            flushCrosshair();
            mask(pre(RenderGameOverlayEvent.ElementType.BOSSHEALTH) ? name : null);
         } else if ("armor".equals(name)) {
            flushCrosshair();
            pre(RenderGameOverlayEvent.ElementType.HEALTH);
            mask(pre(RenderGameOverlayEvent.ElementType.ARMOR) ? name : null);
         } else if ("health".equals(name)) {
            mask(CANCELLED[RenderGameOverlayEvent.ElementType.HEALTH.ordinal()] ? name : null);
         } else if ("food".equals(name)) {
            mask(pre(RenderGameOverlayEvent.ElementType.FOOD) ? name : null);
         } else if ("mountHealth".equals(name)) {
            mask(pre(RenderGameOverlayEvent.ElementType.HEALTHMOUNT) ? name : null);
         } else if ("air".equals(name)) {
            mask(pre(RenderGameOverlayEvent.ElementType.AIR) ? name : null);
         } else if ("toolHighlight".equals(name)) {
            // Forge's GuiIngameForge draws the held item's name in this section (vanilla: selectedItemName).
            flushXp();
            if (!ingameSwapped && res != null && UiHooks.itemName(res)) {
               mask(name);
            }
         } else if ("selectedItemName".equals(name) || "demo".equals(name) || "titleAndSubtitle".equals(name)) {
            flushXp();
         } else if ("overlayMessage".equals(name)) {
            flushXp();
            actionBarStart();
         } else if ("chat".equals(name)) {
            flushXp();
            chatStart();
         }
      }
   }

   static void sectionEnd(String name) {
      if (inOverlay) {
         if (name.equals(maskedBy)) {
            mask(null);
         }

         if ("bossHealth".equals(name)) {
            postIfPending(RenderGameOverlayEvent.ElementType.BOSSHEALTH);
         } else if ("armor".equals(name)) {
            postIfPending(RenderGameOverlayEvent.ElementType.ARMOR);
         } else if ("health".equals(name)) {
            postIfPending(RenderGameOverlayEvent.ElementType.HEALTH);
         } else if ("food".equals(name)) {
            postIfPending(RenderGameOverlayEvent.ElementType.FOOD);
         } else if ("mountHealth".equals(name)) {
            postIfPending(RenderGameOverlayEvent.ElementType.HEALTHMOUNT);
         } else if ("air".equals(name)) {
            postIfPending(RenderGameOverlayEvent.ElementType.HEALTH);
            postIfPending(RenderGameOverlayEvent.ElementType.AIR);
         } else if ("overlayMessage".equals(name)) {
            if (actionPushed) {
               actionPushed = false;
               GlStateManager.popMatrix();
            }
         } else if ("chat".equals(name) && chatStarted) {
            chatStarted = false;
            postIfPending(RenderGameOverlayEvent.ElementType.CHAT);
         }
      }
   }

   private static void mask(String section) {
      if (section != null && maskedBy == null) {
         ((Buffer)MASK).clear();
         GL11.glGetBoolean(3107, MASK);

         for (int i = 0; i < 4; i++) {
            SAVED_MASK[i] = MASK.get(i) != 0;
         }

         GlStateManager.colorMask(false, false, false, false);
         maskedBy = section;
      } else if (section == null && maskedBy != null) {
         GlStateManager.colorMask(SAVED_MASK[0], SAVED_MASK[1], SAVED_MASK[2], SAVED_MASK[3]);
         maskedBy = null;
      }
   }

   static boolean crosshair() {
      if (inOverlay && !crossFired) {
         crossFired = true;
         return !pre(RenderGameOverlayEvent.ElementType.CROSSHAIRS);
      } else {
         return true;
      }
   }

   private static void flushCrosshair() {
      crossFired = true;
      postIfPending(RenderGameOverlayEvent.ElementType.CROSSHAIRS);
   }

   private static void flushXp() {
      if (inOverlay && !xpDone) {
         xpDone = true;
         Minecraft mc = Minecraft.getMinecraft();
         RenderGameOverlayEvent.ElementType t = mc.thePlayer != null && mc.thePlayer.isRidingHorse()
            ? RenderGameOverlayEvent.ElementType.JUMPBAR
            : RenderGameOverlayEvent.ElementType.EXPERIENCE;
         pre(t);
         postIfPending(t);
      }
   }

   private static void actionBarStart() {
      if (Hooks.has("actionBar") && !actionPushed && res != null) {
         int h = res.getScaledHeight();
         int dy = UiHooks.actionBarHeight(h) - h;
         if (dy != 0) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, dy, 0.0F);
            actionPushed = true;
         }
      }
   }

   private static void hideScoreboard() {
      if (Hooks.has("scoreboard") && SLOTS != null) {
         Minecraft mc = Minecraft.getMinecraft();
         if (mc.theWorld != null && mc.thePlayer != null) {
            Scoreboard sb = mc.theWorld.getScoreboard();
            ScoreObjective[] slots = (ScoreObjective[])Swap.get(SLOTS, sb);
            if (slots != null) {
               int teamSlot = -1;
               ScorePlayerTeam team = sb.getPlayersTeam(mc.thePlayer.getName());
               if (team != null) {
                  int idx = team.getChatFormat().getColorIndex();
                  if (idx >= 0 && 3 + idx < slots.length) {
                     teamSlot = 3 + idx;
                  }
               }

               ScoreObjective obj = teamSlot >= 0 ? slots[teamSlot] : null;
               if (obj == null) {
                  obj = slots[1];
               }

               if (obj != null) {
                  sbSlots = slots;
                  sbObjective = obj;
                  sbTeamSlot = teamSlot;
                  sbSaved1 = slots[1];
                  sbSavedTeam = teamSlot >= 0 ? slots[teamSlot] : null;
                  slots[1] = null;
                  if (teamSlot >= 0) {
                     slots[teamSlot] = null;
                  }
               }
            }
         }
      }
   }

   private static void restoreScoreboard() {
      ScoreObjective[] slots = sbSlots;
      if (slots != null) {
         sbSlots = null;
         if (slots[1] == null) {
            slots[1] = sbSaved1;
         }

         if (sbTeamSlot >= 0 && slots[sbTeamSlot] == null) {
            slots[sbTeamSlot] = sbSavedTeam;
         }

         sbSaved1 = null;
         sbSavedTeam = null;
         sbTeamSlot = -1;
      }
   }

   private static void drawScoreboard(boolean underChatTranslate) {
      restoreScoreboard();
      ScoreObjective obj = sbObjective;
      sbObjective = null;
      if (obj != null) {
         Minecraft mc = Minecraft.getMinecraft();
         if (underChatTranslate) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, -(res.getScaledHeight() - 48), 0.0F);
         }

         try {
            if (!UiHooks.scoreboard(obj, res) && RENDER_SCOREBOARD != null) {
               RENDER_SCOREBOARD.invoke(mc.ingameGUI, obj, res);
            }
         } catch (Throwable var7) {
            fail(var7);
         } finally {
            if (underChatTranslate) {
               GlStateManager.popMatrix();
            }
         }
      }
   }

   private static void chatStart() {
      drawScoreboard(true);
      boolean c = pre(RenderGameOverlayEvent.ElementType.CHAT);
      chatStarted = true;
      if (c) {
         mask("chat");
      } else {
         try {
            Bridge.events().onChatPosition(0, res.getScaledHeight() - 48);
         } catch (Throwable var2) {
            fail(var2);
         }
      }
   }

   /**
    * Forge replaces the GuiIngame with its own GuiIngameForge, so the vanilla-GuiIngame overrides above cannot be
    * installed. Forge fires the same elements as RenderGameOverlayEvent.Pre/Post instead; Install.overlayPre/Post
    * forward the ones that have no profiler section here. Returns true to cancel the element.
    */
   static boolean forgePre(String name) {
      if (ingameSwapped || !inOverlay || !Bridge.active()) {
         return false;
      }

      if ("CROSSHAIRS".equals(name)) {
         return !crosshair();
      } else if ("HOTBAR".equals(name)) {
         return pre(RenderGameOverlayEvent.ElementType.HOTBAR);
      } else if ("EXPERIENCE".equals(name)) {
         xpBar(RenderGameOverlayEvent.ElementType.EXPERIENCE);
         return CANCELLED[RenderGameOverlayEvent.ElementType.EXPERIENCE.ordinal()];
      } else if ("JUMPBAR".equals(name)) {
         xpBar(RenderGameOverlayEvent.ElementType.JUMPBAR);
         return CANCELLED[RenderGameOverlayEvent.ElementType.JUMPBAR.ordinal()];
      } else {
         return false;
      }
   }

   static void forgePost(String name) {
      if (ingameSwapped || !inOverlay || !Bridge.active()) {
         return;
      }

      if ("CROSSHAIRS".equals(name)) {
         postIfPending(RenderGameOverlayEvent.ElementType.CROSSHAIRS);
      } else if ("HOTBAR".equals(name)) {
         postIfPending(RenderGameOverlayEvent.ElementType.HOTBAR);
      } else if ("EXPERIENCE".equals(name)) {
         postIfPending(RenderGameOverlayEvent.ElementType.EXPERIENCE);
      } else if ("JUMPBAR".equals(name)) {
         postIfPending(RenderGameOverlayEvent.ElementType.JUMPBAR);
      }
   }

   static boolean tabPre() {
      return inOverlay && pre(RenderGameOverlayEvent.ElementType.PLAYER_LIST);
   }

   static void tabPost() {
      if (inOverlay) {
         postIfPending(RenderGameOverlayEvent.ElementType.PLAYER_LIST);
      }
   }

   private static void fail(Throwable t) {
      if (!failed) {
         failed = true;
         FuriahsVisuals.LOG.error("Hooks: a HUD hook threw; it is logged once and the HUD goes on", t);
      }
   }

   private static void xpBar(RenderGameOverlayEvent.ElementType type) {
      if (inOverlay && !xpDone) {
         xpDone = true;
         pre(type);
      }
   }

   public static class FvGuiChat extends GuiChat {
      void afterInit() {
         GuiTextField moved = UiHooks.chatInit(this, this.inputField, this.width, this.height);
         if (moved != null) {
            this.inputField = moved;
         }
      }

      public void initGui() {
         super.initGui();
         this.afterInit();
      }

      public void drawScreen(int mouseX, int mouseY, float partialTicks) {
         GuiTextField swapped = UiHooks.chatInput(this, this.inputField, this.width, this.height, this.buttonList);
         if (swapped != null) {
            this.inputField = swapped;
         }

         int color = UiHooks.chatInputBackground(this, this.inputField, Integer.MIN_VALUE);
         if (color == Integer.MIN_VALUE) {
            UiHooks.chatText(this, this.inputField, true);
            UiHooks.chatText(this, this.inputField, false);
            super.drawScreen(mouseX, mouseY, partialTicks);
         } else {
            drawRect(2, this.height - 14, this.width - 2, this.height - 2, color);
            UiHooks.chatText(this, this.inputField, true);
            this.inputField.drawTextBox();
            UiHooks.chatText(this, this.inputField, false);
            IChatComponent hover = this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());
            if (hover != null && hover.getChatStyle().getChatHoverEvent() != null) {
               this.handleComponentHover(hover, mouseX, mouseY);
            }

            for (int i = 0; i < this.buttonList.size(); i++) {
               ((GuiButton)this.buttonList.get(i)).drawButton(this.mc, mouseX, mouseY);
            }

            for (int j = 0; j < this.labelList.size(); j++) {
               ((GuiLabel)this.labelList.get(j)).drawLabel(this.mc, mouseX, mouseY);
            }
         }
      }

      public void handleMouseInput() throws IOException {
         if (!Bridge.active() || !Bridge.events().onScreenMouse(this)) {
            super.handleMouseInput();
         }
      }

      public void handleKeyboardInput() throws IOException {
         if (!Bridge.active() || !Bridge.events().onScreenKey(this)) {
            super.handleKeyboardInput();
         }
      }
   }

   public static class FvGuiIngame extends GuiIngame {
      public FvGuiIngame(Minecraft mc) {
         super(mc);
      }

      public void renderGameOverlay(float partialTicks) {
         boolean ours = this.mc.theWorld != null && HudHooks.begin(partialTicks);

         try {
            super.renderGameOverlay(partialTicks);
         } finally {
            if (ours) {
               HudHooks.end(partialTicks);
               if (Bridge.active()) {
                  PostChain.afterHudHook();
               }
            }
         }
      }

      protected void renderTooltip(ScaledResolution sr, float partialTicks) {
         if (!HudHooks.pre(RenderGameOverlayEvent.ElementType.HOTBAR)) {
            super.renderTooltip(sr, partialTicks);
            HudHooks.postIfPending(RenderGameOverlayEvent.ElementType.HOTBAR);
         }
      }

      protected boolean showCrosshair() {
         return HudHooks.crosshair() && super.showCrosshair();
      }

      public void renderHorseJumpBar(ScaledResolution sr, int x) {
         HudHooks.xpBar(RenderGameOverlayEvent.ElementType.JUMPBAR);
         if (!HudHooks.CANCELLED[RenderGameOverlayEvent.ElementType.JUMPBAR.ordinal()] || !HudHooks.inOverlay) {
            super.renderHorseJumpBar(sr, x);
            HudHooks.postIfPending(RenderGameOverlayEvent.ElementType.JUMPBAR);
         }
      }

      public void renderExpBar(ScaledResolution sr, int x) {
         HudHooks.xpBar(RenderGameOverlayEvent.ElementType.EXPERIENCE);
         if (!HudHooks.CANCELLED[RenderGameOverlayEvent.ElementType.EXPERIENCE.ordinal()] || !HudHooks.inOverlay) {
            super.renderExpBar(sr, x);
            HudHooks.postIfPending(RenderGameOverlayEvent.ElementType.EXPERIENCE);
         }
      }

      public void renderSelectedItem(ScaledResolution sr) {
         HudHooks.flushXp();
         if (!HudHooks.inOverlay || !UiHooks.itemName(sr)) {
            super.renderSelectedItem(sr);
         }
      }
   }

   public static class FvGuiNewChat extends GuiNewChat {
      public FvGuiNewChat(Minecraft mc) {
         super(mc);
      }

      public void drawChat(int updateCounter) {
         if (!UiHooks.chatDraw(this, updateCounter)) {
            super.drawChat(updateCounter);
         }
      }

      public IChatComponent getChatComponent(int mouseX, int mouseY) {
         return UiHooks.chatAnswersHits() ? UiHooks.chatHit(this, mouseX, mouseY) : super.getChatComponent(mouseX, mouseY);
      }

      public void scroll(int amount) {
         UiHooks.chatScroll(amount);
         super.scroll(amount);
      }

      public void resetScroll() {
         UiHooks.chatResetScroll();
         super.resetScroll();
      }

      public void printChatMessageWithOptionalDeletion(IChatComponent component, int id) {
         UiHooks.chatSettingLine(true);

         try {
            super.printChatMessageWithOptionalDeletion(component, id);
         } finally {
            UiHooks.chatSettingLine(false);
         }
      }

      public void refreshChat() {
         UiHooks.chatSettingLine(true);

         try {
            super.refreshChat();
         } finally {
            UiHooks.chatSettingLine(false);
         }
      }
   }

   public static class FvGuiPlayerTabOverlay extends GuiPlayerTabOverlay {
      public FvGuiPlayerTabOverlay(Minecraft mc, GuiIngame gui) {
         super(mc, gui);
      }

      public void renderPlayerlist(int width, Scoreboard scoreboard, ScoreObjective objective) {
         if (!HudHooks.tabPre()) {
            super.renderPlayerlist(width, scoreboard, objective);
            HudHooks.tabPost();
         }
      }
   }
}
