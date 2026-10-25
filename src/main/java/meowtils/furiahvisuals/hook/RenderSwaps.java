package meowtils.furiahvisuals.hook;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.module.kill.KillHooks;
import meowtils.furiahvisuals.module.misc.MiscHooks;
import meowtils.furiahvisuals.module.ui.UiHooks;
import meowtils.furiahvisuals.module.weapons.WeaponHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.tileentity.RenderItemFrame;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class RenderSwaps {
   static final String[] PLAYER_HOOKS = new String[]{"hideShadow", "nametag", "nametagSneak", "nametagOwn"};
   static final String[] LAYER_HOOKS = new String[]{"hideArmor", "hideHead", "heldItem", "heldItemEnd"};
   static final String[] MODEL_HOOKS = new String[]{"armPose", "armorPose"};
   static final String[] FIRST_PERSON_HOOKS = new String[]{"firstPerson", "firstPersonEnd"};
   static final String[] ICON_HOOKS = new String[]{"icon", "iconPlain"};
   static final String[] DROPPED_HOOKS = new String[]{"droppedStart", "droppedItem"};
   static final String[] FRAME_HOOKS = new String[]{"frameStart", "frameItem"};
   private static final Field MC_RENDER_ITEM = Hooks.field(Minecraft.class, "renderItem", "renderItem");
   private static final Field MC_ITEM_RENDERER = Hooks.field(Minecraft.class, "itemRenderer", "itemRenderer");
   private static final Field ITEM_TO_RENDER = Hooks.field(ItemRenderer.class, "itemToRender", "itemToRender");
   private static final Field ENTITY_RENDER_MAP = Hooks.field(RenderManager.class, "entityRenderMap", "entityRenderMap");
   private static final Field SKIN_MAP = Hooks.field(RenderManager.class, "skinMap", "skinMap");
   private static final Field PLAYER_RENDERER = Hooks.field(RenderManager.class, "playerRenderer", "playerRenderer");
   private static final Field MAIN_MODEL = Hooks.field(RendererLivingEntity.class, "mainModel", "mainModel");
   private static final Field LAYERS = Hooks.field(RendererLivingEntity.class, "layerRenderers", "layerRenderers");
   private static final Field ARMOR_LEGGINGS = Hooks.field(LayerArmorBase.class, "modelLeggings", "modelLeggings");
   private static final Field ARMOR_ARMOR = Hooks.field(LayerArmorBase.class, "modelArmor", "modelArmor");
   private static final Field RELOAD_LISTENERS = reloadListenersField();
   private static final List<Runnable> UNDO = new ArrayList<>();
   static int dropped;
   static int framed;
   private static boolean failed;

   private RenderSwaps() {
   }

   private static Field reloadListenersField() {
      try {
         return Hooks.field(Class.forName("net.minecraft.client.resources.SimpleReloadableResourceManager"), "reloadListeners", "reloadListeners");
      } catch (Throwable var1) {
         return null;
      }
   }

   static void install() {
      Minecraft mc = Minecraft.getMinecraft();
      RenderManager rm = mc.getRenderManager();
      Map<Class<?>, Render<?>> entityMap = entityMap(rm);
      Map<String, RenderPlayer> skinMap = skinMap(rm);

      try {
         installRenderItem(mc, rm, entityMap, skinMap);
         installItemRenderer(mc);
         if (entityMap != null) {
            installWorldItems(mc, entityMap);
         }

         if (skinMap != null) {
            installPlayers(rm, skinMap);
         }

         if (entityMap != null) {
            installArmorStand(entityMap);
            wrapAllLayers(entityMap, skinMap);
         }
      } catch (Throwable var5) {
         FuriahsVisuals.LOG.error("Hooks: a rendering hook could not be installed; the ones before it stay", var5);
      }
   }

   static void uninstall() {
      for (int i = UNDO.size() - 1; i >= 0; i--) {
         try {
            UNDO.get(i).run();
         } catch (Throwable var2) {
            FuriahsVisuals.LOG.error("Hooks: could not undo a rendering hook", var2);
         }
      }

      UNDO.clear();
      Hooks.off(PLAYER_HOOKS);
      Hooks.off(LAYER_HOOKS);
      Hooks.off(MODEL_HOOKS);
      Hooks.off(FIRST_PERSON_HOOKS);
      Hooks.off(ICON_HOOKS);
      Hooks.off(DROPPED_HOOKS);
      Hooks.off(FRAME_HOOKS);
      dropped = 0;
      framed = 0;
   }

   private static Map<Class<?>, Render<?>> entityMap(RenderManager rm) {
      Object o = ENTITY_RENDER_MAP == null ? null : Swap.get(ENTITY_RENDER_MAP, rm);
      return o instanceof Map ? (Map)o : null;
   }

   private static Map<String, RenderPlayer> skinMap(RenderManager rm) {
      Object o = SKIN_MAP == null ? null : Swap.get(SKIN_MAP, rm);
      return o instanceof Map ? (Map)o : null;
   }

   private static List<LayerRenderer<?>> layers(RendererLivingEntity<?> r) {
      Object o = LAYERS == null ? null : Swap.get(LAYERS, r);
      return o instanceof List ? (List)o : null;
   }

   private static List<Object> holders(Minecraft mc, Map<Class<?>, Render<?>> entityMap, Map<String, RenderPlayer> skinMap) {
      List<Object> all = new ArrayList<>();
      all.add(mc);
      all.add(mc.entityRenderer);
      all.add(mc.ingameGUI);
      all.add(mc.getItemRenderer());
      if (mc.currentScreen != null) {
         all.add(mc.currentScreen);
      }

      if (entityMap != null) {
         all.addAll(entityMap.values());
      }

      if (skinMap != null) {
         all.addAll(skinMap.values());
      }

      return all;
   }

   private static void installRenderItem(final Minecraft mc, final RenderManager rm, Map<Class<?>, Render<?>> entityMap, Map<String, RenderPlayer> skinMap) {
      RenderItem old = mc.getRenderItem();
      RenderSwaps.FvRenderItem f = Swap.upgrade(old, RenderItem.class, RenderSwaps.FvRenderItem.class);
      if (f != null && MC_RENDER_ITEM != null) {
         if (f != old) {
            repoint(old, f, mc, entityMap, skinMap);
         }

         Hooks.on(ICON_HOOKS);
         UNDO.add(new Runnable() {
            @Override
            public void run() {
               RenderItem cur = mc.getRenderItem();
               if (Swap.ours(cur, RenderSwaps.FvRenderItem.class)) {
                  RenderItem plain = Swap.downgrade(cur, RenderItem.class);
                  RenderSwaps.repoint(cur, plain, mc, RenderSwaps.entityMap(rm), RenderSwaps.skinMap(rm));
               }
            }
         });
      }
   }

   private static void repoint(Object from, Object to, Minecraft mc, Map<Class<?>, Render<?>> entityMap, Map<String, RenderPlayer> skinMap) {
      for (Object h : holders(mc, entityMap, skinMap)) {
         Swap.rebind(h, from, to);
      }

      replaceListener(mc, from, to);
   }

   private static void replaceListener(Minecraft mc, Object from, Object to) {
      if (RELOAD_LISTENERS != null && mc.getResourceManager() instanceof IReloadableResourceManager) {
         Object o = Swap.get(RELOAD_LISTENERS, mc.getResourceManager());
         if (o instanceof List) {
            List<Object> list = (List<Object>)o;

            for (int i = 0; i < list.size(); i++) {
               if (list.get(i) == from) {
                  list.set(i, to);
               }
            }
         }
      }
   }

   private static void installItemRenderer(final Minecraft mc) {
      ItemRenderer old = mc.getItemRenderer();
      if (ITEM_TO_RENDER != null && MC_ITEM_RENDERER != null) {
         RenderSwaps.FvItemRenderer f = Swap.upgrade(old, ItemRenderer.class, RenderSwaps.FvItemRenderer.class);
         if (f != null) {
            if (f != old) {
               Swap.rebind(mc, old, f);
               Swap.rebind(mc.entityRenderer, old, f);
            }

            Hooks.on(FIRST_PERSON_HOOKS);
            UNDO.add(new Runnable() {
               @Override
               public void run() {
                  ItemRenderer cur = mc.getItemRenderer();
                  if (Swap.ours(cur, RenderSwaps.FvItemRenderer.class)) {
                     ItemRenderer plain = Swap.downgrade(cur, ItemRenderer.class);
                     Swap.rebind(mc, cur, plain);
                     Swap.rebind(mc.entityRenderer, cur, plain);
                  }
               }
            });
         }
      }
   }

   private static void installWorldItems(Minecraft mc, Map<Class<?>, Render<?>> entityMap) {
      if (swapIn(entityMap, EntityItem.class, RenderEntityItem.class, RenderSwaps.FvRenderEntityItem.class)) {
         Hooks.on(DROPPED_HOOKS);
      }

      if (swapIn(entityMap, EntityItemFrame.class, RenderItemFrame.class, RenderSwaps.FvRenderItemFrame.class)) {
         Hooks.on(FRAME_HOOKS);
      }
   }

   private static <T extends Render<?>> boolean swapIn(
      final Map<Class<?>, Render<?>> map, final Class<?> entityClass, final Class<T> base, Class<? extends T> sub
   ) {
      Render<?> old = map.get(entityClass);
      final T f = (T)Swap.upgrade(old, base, (Class<T>)sub);
      if (f == null) {
         return false;
      } else {
         if (f != old) {
            map.put(entityClass, f);
            rebindLayers(f, old, f);
         }

         UNDO.add(new Runnable() {
            @Override
            public void run() {
               Render<?> cur = map.get(entityClass);
               if (cur == f) {
                  T plain = Swap.downgrade(cur, base);
                  map.put(entityClass, plain);
                  RenderSwaps.rebindLayers(plain, cur, plain);
               }
            }
         });
         return true;
      }
   }

   private static void rebindLayers(Object renderer, Object from, Object to) {
      if (renderer instanceof RendererLivingEntity) {
         List<LayerRenderer<?>> layers = layers((RendererLivingEntity<?>)renderer);
         if (layers != null) {
            for (LayerRenderer<?> l : layers) {
               Swap.rebind(l, from, to);
               if (l instanceof RenderSwaps.FvLayer) {
                  Swap.rebind(((RenderSwaps.FvLayer)l).inner, from, to);
               }
            }
         }
      }
   }

   private static void installPlayers(final RenderManager rm, final Map<String, RenderPlayer> skinMap) {
      boolean any = false;

      for (Entry<String, RenderPlayer> e : new ArrayList<>(skinMap.entrySet())) {
         final String key = e.getKey();
         RenderPlayer old = e.getValue();
         final RenderSwaps.FvRenderPlayer f = Swap.upgrade(old, RenderPlayer.class, RenderSwaps.FvRenderPlayer.class);
         if (f != null) {
            any = true;
            if (f != old) {
               skinMap.put(key, f);
               if (PLAYER_RENDERER != null && Swap.get(PLAYER_RENDERER, rm) == old) {
                  Swap.set(PLAYER_RENDERER, rm, f);
               }

               rebindLayers(f, old, f);
            }

            installPlayerModels(f);
            UNDO.add(new Runnable() {
               @Override
               public void run() {
                  RenderPlayer cur = skinMap.get(key);
                  if (cur == f) {
                     RenderPlayer plain = Swap.downgrade(cur, RenderPlayer.class);
                     skinMap.put(key, plain);
                     if (RenderSwaps.PLAYER_RENDERER != null && Swap.get(RenderSwaps.PLAYER_RENDERER, rm) == cur) {
                        Swap.set(RenderSwaps.PLAYER_RENDERER, rm, plain);
                     }

                     RenderSwaps.rebindLayers(plain, cur, plain);
                  }
               }
            });
         }
      }

      if (any) {
         Hooks.on(PLAYER_HOOKS);
      }
   }

   private static void installPlayerModels(final RendererLivingEntity<?> r) {
      if (MAIN_MODEL != null) {
         ModelBase old = (ModelBase)Swap.get(MAIN_MODEL, r);
         if (old != null && (old.getClass() == ModelPlayer.class || Swap.ours(old, RenderSwaps.FvModelPlayer.class))) {
            final RenderSwaps.FvModelPlayer m = Swap.upgrade(old, ModelPlayer.class, RenderSwaps.FvModelPlayer.class);
            if (m != null) {
               if (m != old) {
                  Swap.set(MAIN_MODEL, r, m);
                  rebindLayers(r, old, m);
               }

               Hooks.on("armPose");
               UNDO.add(new Runnable() {
                  @Override
                  public void run() {
                     Object cur = Swap.get(RenderSwaps.MAIN_MODEL, r);
                     if (cur == m) {
                        ModelPlayer plain = Swap.downgrade(cur, ModelPlayer.class);
                        Swap.set(RenderSwaps.MAIN_MODEL, r, plain);
                        RenderSwaps.rebindLayers(r, cur, plain);
                     }
                  }
               });
            }
         }

         List<LayerRenderer<?>> layers = layers(r);
         if (layers != null && ARMOR_LEGGINGS != null && ARMOR_ARMOR != null) {
            for (LayerRenderer<?> l : layers) {
               final Object layer = l instanceof RenderSwaps.FvLayer ? ((RenderSwaps.FvLayer)l).inner : l;
               if (layer instanceof LayerArmorBase) {
                  for (final Field f : new Field[]{ARMOR_LEGGINGS, ARMOR_ARMOR}) {
                     Object model = Swap.get(f, layer);
                     if (model != null && model.getClass() == ModelBiped.class) {
                        final RenderSwaps.FvModelBiped m = Swap.upgrade(model, ModelBiped.class, RenderSwaps.FvModelBiped.class);
                        if (m != null) {
                           Swap.set(f, layer, m);
                           Hooks.on("armorPose");
                           UNDO.add(new Runnable() {
                              @Override
                              public void run() {
                                 if (Swap.get(f, layer) == m) {
                                    Swap.set(f, layer, Swap.downgrade(m, ModelBiped.class));
                                 }
                              }
                           });
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void installArmorStand(Map<Class<?>, Render<?>> map) {
      swapIn(map, EntityArmorStand.class, ArmorStandRenderer.class, RenderSwaps.FvArmorStandRenderer.class);
   }

   private static void wrapAllLayers(Map<Class<?>, Render<?>> entityMap, Map<String, RenderPlayer> skinMap) {
      IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>();
      List<Render<?>> all = new ArrayList<>(entityMap.values());
      if (skinMap != null) {
         all.addAll(skinMap.values());
      }

      boolean any = false;

      for (Render<?> r : all) {
         if (r instanceof RendererLivingEntity && seen.put(r, Boolean.TRUE) == null) {
            RendererLivingEntity<?> living = (RendererLivingEntity<?>)r;
            final List<LayerRenderer<?>> layers = layers(living);
            if (layers != null) {
               for (int i = 0; i < layers.size(); i++) {
                  LayerRenderer<?> l = layers.get(i);
                  int kind = l instanceof LayerArmorBase ? 1 : (l instanceof LayerCustomHead ? 2 : (l instanceof LayerHeldItem ? 3 : 0));
                  if (kind != 0) {
                     layers.set(i, new RenderSwaps.FvLayer(l, kind, living));
                     any = true;
                  }
               }

               UNDO.add(new Runnable() {
                  @Override
                  public void run() {
                     for (int ix = 0; ix < layers.size(); ix++) {
                        if (layers.get(ix) instanceof RenderSwaps.FvLayer) {
                           layers.set(ix, ((RenderSwaps.FvLayer)layers.get(ix)).inner);
                        }
                     }
                  }
               });
            }
         }
      }

      if (any) {
         Hooks.on(LAYER_HOOKS);
      }
   }

   private static void fail(Throwable t) {
      if (!failed) {
         failed = true;
         FuriahsVisuals.LOG.error("Hooks: a rendering hook threw; it is logged once", t);
      }
   }

   public static class FvArmorStandRenderer extends ArmorStandRenderer {
      public FvArmorStandRenderer(RenderManager rm) {
         super(rm);
      }

      protected void renderLivingLabel(EntityArmorStand entity, String str, double x, double y, double z, int maxDistance) {
         if (!Bridge.active() || !UiHooks.nametag(this, entity, str, x, y, z, maxDistance)) {
            super.renderLivingLabel(entity, str, x, y, z, maxDistance);
         }
      }
   }

   public static class FvItemRenderer extends ItemRenderer {
      public FvItemRenderer(Minecraft mc) {
         super(mc);
      }

      public void renderItemInFirstPerson(float partialTicks) {
         CameraHooks.handItemStart();

         try {
            if (!Bridge.active() || !WeaponHooks.firstPerson((ItemStack)Swap.get(RenderSwaps.ITEM_TO_RENDER, this), partialTicks)) {
               super.renderItemInFirstPerson(partialTicks);
               if (Bridge.active()) {
                  WeaponHooks.firstPersonEnd(partialTicks);
               }

               return;
            }
         } finally {
            CameraHooks.handItemEnd();
         }
      }
   }

   static final class FvLayer implements LayerRenderer<EntityLivingBase> {
      static final int ARMOR = 1;
      static final int HEAD = 2;
      static final int HELD = 3;
      final LayerRenderer inner;
      private final int kind;
      private final RendererLivingEntity<?> owner;

      FvLayer(LayerRenderer<?> inner, int kind, RendererLivingEntity<?> owner) {
         this.inner = inner;
         this.kind = kind;
         this.owner = owner;
      }

      // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      public void doRenderLayer(
         EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale
      ) {
         if (!Bridge.active()) {
            this.inner.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
         } else if (this.kind == 2) {
            if (!MiscHooks.hideHead(entity)) {
               this.inner.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
            }
         } else if (this.kind == 3) {
            ModelBase model = this.owner.getMainModel();
            if (!WeaponHooks.heldItem(model, entity, partialTicks)) {
               this.inner.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
               WeaponHooks.heldItemEnd(model, entity);
            }
         } else {
            ItemStack[] hidden = null;
            if (entity instanceof EntityPlayer) {
               ItemStack[] armor = ((EntityPlayer)entity).inventory.armorInventory;

               for (int slot = 1; slot <= 4 && slot <= armor.length; slot++) {
                  if (armor[slot - 1] != null && MiscHooks.hideArmor(entity, slot)) {
                     if (hidden == null) {
                        hidden = new ItemStack[armor.length];
                     }

                     hidden[slot - 1] = armor[slot - 1];
                     armor[slot - 1] = null;
                  }
               }
            }

            boolean var16 = false /* VF: Semaphore variable */;

            try {
               var16 = true;
               this.inner.doRenderLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scale);
               var16 = false;
            } finally {
               if (var16) {
                  if (hidden != null) {
                     ItemStack[] armor = ((EntityPlayer)entity).inventory.armorInventory;

                     for (int i = 0; i < hidden.length; i++) {
                        if (hidden[i] != null && armor[i] == null) {
                           armor[i] = hidden[i];
                        }
                     }
                  }
               }
            }

            if (hidden != null) {
               ItemStack[] armor = ((EntityPlayer)entity).inventory.armorInventory;

               for (int ix = 0; ix < hidden.length; ix++) {
                  if (hidden[ix] != null && armor[ix] == null) {
                     armor[ix] = hidden[ix];
                  }
               }
            }
         }
      }

      public boolean shouldCombineTextures() {
         return this.inner.shouldCombineTextures();
      }
   }

   public static class FvModelBiped extends ModelBiped {
      public FvModelBiped(float scale) {
         super(scale);
      }

      public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entity) {
         super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity);
         if (Bridge.active()) {
            WeaponHooks.armorPose(this, entity);
         }
      }
   }

   public static class FvModelPlayer extends ModelPlayer {
      public FvModelPlayer(float scale, boolean smallArms) {
         super(scale, smallArms);
      }

      public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entity) {
         super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity);
         if (Bridge.active()) {
            WeaponHooks.armPose(this, entity, limbSwing, limbSwingAmount, ageInTicks);
         }
      }
   }

   public static class FvRenderEntityItem extends RenderEntityItem {
      public FvRenderEntityItem(RenderManager rm, RenderItem ri) {
         super(rm, ri);
      }

      public void doRender(EntityItem entity, double x, double y, double z, float entityYaw, float partialTicks) {
         WeaponHooks.worldItemEntity(entity, partialTicks);
         RenderSwaps.dropped++;

         try {
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
         } finally {
            RenderSwaps.dropped--;
         }
      }
   }

   public static class FvRenderItem extends RenderItem {
      public FvRenderItem(TextureManager tm, ModelManager mm) {
         super(tm, mm);
      }

      public void renderItemAndEffectIntoGUI(ItemStack stack, int x, int y) {
         if (!Bridge.active() || !WeaponHooks.icon(stack, x, y, this.zLevel)) {
            super.renderItemAndEffectIntoGUI(stack, x, y);
         }
      }

      public void renderItemIntoGUI(ItemStack stack, int x, int y) {
         if (!Bridge.active() || !WeaponHooks.icon(stack, x, y, this.zLevel)) {
            super.renderItemIntoGUI(stack, x, y);
         }
      }

      public void renderItem(ItemStack stack, IBakedModel model) {
         if (RenderSwaps.dropped <= 0 || !Bridge.active() || !WeaponHooks.worldItem(stack, false)) {
            super.renderItem(stack, model);
         }
      }

      public void renderItem(ItemStack stack, TransformType type) {
         if (RenderSwaps.framed <= 0 || !Bridge.active() || !WeaponHooks.worldItem(stack, true)) {
            super.renderItem(stack, type);
         }
      }
   }

   public static class FvRenderItemFrame extends RenderItemFrame {
      public FvRenderItemFrame(RenderManager rm, RenderItem ri) {
         super(rm, ri);
      }

      public void doRender(EntityItemFrame entity, double x, double y, double z, float entityYaw, float partialTicks) {
         WeaponHooks.worldItemEntity(entity, partialTicks);
         RenderSwaps.framed++;

         try {
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
         } finally {
            RenderSwaps.framed--;
         }
      }
   }

   public static class FvRenderPlayer extends RenderPlayer {
      public FvRenderPlayer(RenderManager rm) {
         super(rm);
      }

      public void doRender(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks) {
         try {
            if (Bridge.active() && Bridge.events().onRenderPlayer(entity)) {
               return;
            }
         } catch (Throwable var11) {
            RenderSwaps.fail(var11);
         }

         super.doRender(entity, x, y, z, entityYaw, partialTicks);
      }

      public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
         if (!Bridge.active() || !KillHooks.hideShadow(entity)) {
            super.doRenderShadowAndFire(entity, x, y, z, yaw, partialTicks);
         }
      }

      protected void renderLivingLabel(AbstractClientPlayer entity, String str, double x, double y, double z, int maxDistance) {
         if (!Bridge.active() || !UiHooks.nametag(this, entity, str, x, y, z, maxDistance)) {
            super.renderLivingLabel(entity, str, x, y, z, maxDistance);
         }
      }

      public void renderName(AbstractClientPlayer entity, double x, double y, double z) {
         if (Bridge.active() && entity.isSneaking() && this.canRenderName(entity)) {
            double d = entity.getDistanceSqToEntity(this.renderManager.livingPlayer);
            if (d < 1024.0) {
               GlStateManager.alphaFunc(516, 0.1F);
               if (UiHooks.nametagSneaking(this, entity, x, y, z)) {
                  return;
               }
            }
         }

         super.renderName(entity, x, y, z);
      }

      protected boolean canRenderName(AbstractClientPlayer entity) {
         Boolean show = Bridge.active() ? UiHooks.nametagOwn(entity) : null;
         return show != null ? show : super.canRenderName(entity);
      }
   }
}
