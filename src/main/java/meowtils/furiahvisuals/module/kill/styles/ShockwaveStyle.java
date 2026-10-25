package meowtils.furiahvisuals.module.kill.styles;

import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.Neon;
import meowtils.furiahvisuals.fx.Rng;
import meowtils.furiahvisuals.fx.Terrain;
import meowtils.furiahvisuals.fx.WarpShell;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.Distortion;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.RenderState;
import meowtils.furiahvisuals.render.Textures;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockPos;

public final class ShockwaveStyle extends KillStyleBase {
   private static final int MAX_DEBRIS = 72;
   private static final float WAVE_SECONDS = 0.75F;
   private final SliderSetting radius;
   private final SliderSetting strength;
   private final SliderSetting debris;
   private final BooleanSetting distortion;
   private final double[] m = new double[9];
   private static final int[][] FACES = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
   private final double[] px = new double[8];
   private final double[] py = new double[8];
   private final double[] pz = new double[8];

   public ShockwaveStyle(KillEffectsModule module) {
      super(module, KillStyle.SHOCKWAVE, new ColorSetting("shockwave.color", "Color", "Color of the flash and the ring.", -18340));
      this.radius = this.setting(new SliderSetting("shockwave.radius", "Radius", "How far the ring races out.", 3.0, 10.0, 6.0, 0.5).unit(" blocks"));
      this.strength = this.setting(
         new SliderSetting("shockwave.strength", "Strength", "How hard the body is thrown and the debris lifted.", 0.3, 2.0, 1.0, 0.05).unit("x")
      );
      this.debris = this.setting(new SliderSetting("shockwave.debris", "Debris", "How much ground is lifted.", 0.0, 2.0, 1.0, 0.05).unit("x"));
      this.distortion = this.setting(new BooleanSetting("shockwave.distortion", "Distortion", "The swelling shell bends the scene behind it.", true));
   }

   @Override
   public float seconds(KillFx fx) {
      return 1.9F;
   }

   @Override
   public boolean leavesBody() {
      return true;
   }

   private double reach(KillFx fx) {
      return Math.min(fx.preview ? 2.4 : 1.0E9, this.radius.getFloat() * fx.size * (fx.crit ? 1.3 : 1.0));
   }

   @Override
   public void start(KillFx fx) {
      ShockwaveStyle.State s = new ShockwaveStyle.State();
      fx.data = s;
      Minecraft mc = Minecraft.getMinecraft();
      WorldClient world = fx.preview ? null : mc.theWorld;
      double r = this.reach(fx);
      s.ground = world == null
         ? Terrain.HeightField.flat(fx.ctx.groundY)
         : Terrain.HeightField.sample(world, fx.ctx.feetX, fx.ctx.groundY, fx.ctx.feetZ, r + 1.0, 1);
      s.texture = Textures.STONE;
      s.u0 = 0.0F;
      s.v0 = 0.0F;
      s.du = 1.0F;
      s.dv = 1.0F;
      if (world != null) {
         try {
            BlockPos below = new BlockPos(fx.ctx.feetX, fx.ctx.groundY - 0.5, fx.ctx.feetZ);
            IBlockState state = world.getBlockState(below);
            TextureAtlasSprite sprite;
            if (state.getBlock().getMaterial().isSolid() && (sprite = mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(state)) != null) {
               s.texture = BodyProxy.texture(TextureMap.locationBlocksTexture);
               s.u0 = sprite.getMinU();
               s.v0 = sprite.getMinV();
               s.du = sprite.getMaxU() - sprite.getMinU();
               s.dv = sprite.getMaxV() - sprite.getMinV();
            }
         } catch (RuntimeException var17) {
         }
      }

      Rng rng = fx.rng;
      s.count = Math.min(72, fx.count(30.0F * this.debris.getFloat() * (float)(r / 6.0), true));
      if (this.debris.getFloat() <= 0.0F) {
         s.count = 0;
      }

      for (int i = 0; i < s.count; i++) {
         s.ringAngle[i] = rng.next() * 6.2832F;
         float dist = (float)(r * (0.18 + 0.8 * Math.sqrt(rng.next())));
         s.born[i] = 0.75F * (float)Math.pow(dist / r, 1.5);
         s.x[i] = fx.ctx.feetX + Math.cos(s.ringAngle[i]) * dist;
         s.z[i] = fx.ctx.feetZ + Math.sin(s.ringAngle[i]) * dist;
         double g = s.ground.at(s.x[i], s.z[i]);
         s.y[i] = Double.isNaN(g) ? fx.ctx.groundY : g;
         float lift = this.strength.getFloat() * (1.0F - 0.5F * dist / (float)r);
         s.vx[i] = (float)Math.cos(s.ringAngle[i]) * (1.5F + rng.next() * 2.0F) * lift;
         s.vz[i] = (float)Math.sin(s.ringAngle[i]) * (1.5F + rng.next() * 2.0F) * lift;
         s.vy[i] = (3.5F + rng.next() * 3.5F) * lift;

         float axl;
         do {
            s.ax[i] = rng.signed();
            s.ay[i] = rng.signed();
            s.az[i] = rng.signed();
         } while ((axl = (float)Math.sqrt(s.ax[i] * s.ax[i] + s.ay[i] * s.ay[i] + s.az[i] * s.az[i])) < 0.1F);

         s.ax[i] = s.ax[i] / axl;
         s.ay[i] = s.ay[i] / axl;
         s.az[i] = s.az[i] / axl;
         s.spin[i] = rng.signed() * 10.0F;
         s.angle[i] = rng.next() * 6.2832F;
         s.size[i] = (0.07F + rng.next() * 0.09F) * fx.size;
         s.u[i] = rng.next() * 0.75F;
         s.v[i] = rng.next() * 0.75F;
         s.landed[i] = -1.0F;
      }

      fx.ragdoll()
         .fling(
            fx.ctx.impactX,
            fx.ctx.impactY,
            fx.ctx.impactZ,
            fx.ctx.body.vx * 0.5,
            0.0,
            fx.ctx.body.vz * 0.5,
            fx.ctx.dirX,
            Math.max(-0.3, fx.ctx.dirY) + 0.25,
            fx.ctx.dirZ,
            7.5 * this.strength.getFloat() * (fx.crit ? 1.4 : 1.0) * (fx.preview ? 0.35 : 1.0),
            4.5 * this.strength.getFloat()
         );
      if (!fx.lod) {
         fx.burst(fx.ctx.midX(), fx.ctx.midY(), fx.ctx.midZ(), fx.ctx.dirX, fx.ctx.dirY, fx.ctx.dirZ, fx.count(14.0F, true), 6.0F, 0.9F);
      }
   }

   @Override
   public void update(KillFx param1, float param2) {
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.IllegalStateException: No common supertype for ternary expression
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.getExprType(FunctionExprent.java:223)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.checkExprTypeBounds(FunctionExprent.java:371)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.checkTypeExpr(VarTypeProcessor.java:140)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.checkTypeExprent(VarTypeProcessor.java:121)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.lambda$processVarTypes$2(VarTypeProcessor.java:114)
      //   at org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph.iterateExprents(DirectGraph.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.processVarTypes(VarTypeProcessor.java:114)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.calculateVarTypes(VarTypeProcessor.java:44)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionsProcessor.setVarVersions(VarVersionsProcessor.java:68)
      //   at org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor.setVarVersions(VarProcessor.java:47)
      //   at org.jetbrains.java.decompiler.main.rels.MethodProcessor.codeToJava(MethodProcessor.java:234)
      //
      // Bytecode:
      // 000: aload 1
      // 001: getfield meowtils/furiahvisuals/module/kill/KillFx.data Ljava/lang/Object;
      // 004: checkcast meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State
      // 007: astore 3
      // 008: aload 1
      // 009: invokevirtual meowtils/furiahvisuals/module/kill/KillFx.collider ()Lmeowtils/furiahvisuals/fx/physics/BlockCollider;
      // 00c: astore 4
      // 00e: bipush 0
      // 00f: istore 5
      // 011: iload 5
      // 013: aload 3
      // 014: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.count I
      // 017: if_icmpge 1f8
      // 01a: aload 1
      // 01b: getfield meowtils/furiahvisuals/module/kill/KillFx.t F
      // 01e: aload 3
      // 01f: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.born [F
      // 022: iload 5
      // 024: faload
      // 025: fcmpg
      // 026: iflt 1f2
      // 029: aload 3
      // 02a: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.landed [F
      // 02d: iload 5
      // 02f: faload
      // 030: fconst_0
      // 031: fcmpl
      // 032: iflt 038
      // 035: goto 1f2
      // 038: iload 5
      // 03a: istore 10
      // 03c: aload 3
      // 03d: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vy [F
      // 040: iload 10
      // 042: aload 3
      // 043: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vy [F
      // 046: iload 10
      // 048: faload
      // 049: ldc_w 20.0
      // 04c: fload 2
      // 04d: fmul
      // 04e: fsub
      // 04f: fastore
      // 050: aload 3
      // 051: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.x [D
      // 054: iload 5
      // 056: daload
      // 057: aload 3
      // 058: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vx [F
      // 05b: iload 5
      // 05d: faload
      // 05e: fload 2
      // 05f: fmul
      // 060: f2d
      // 061: dadd
      // 062: dstore 11
      // 064: aload 3
      // 065: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.y [D
      // 068: iload 5
      // 06a: daload
      // 06b: aload 3
      // 06c: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vy [F
      // 06f: iload 5
      // 071: faload
      // 072: fload 2
      // 073: fmul
      // 074: f2d
      // 075: dadd
      // 076: dstore 13
      // 078: aload 3
      // 079: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.z [D
      // 07c: iload 5
      // 07e: daload
      // 07f: aload 3
      // 080: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vz [F
      // 083: iload 5
      // 085: faload
      // 086: fload 2
      // 087: fmul
      // 088: f2d
      // 089: dadd
      // 08a: dstore 15
      // 08c: aload 1
      // 08d: getfield meowtils/furiahvisuals/module/kill/KillFx.preview Z
      // 090: ifne 0ed
      // 093: aload 4
      // 095: dload 11
      // 097: aload 3
      // 098: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.y [D
      // 09b: iload 5
      // 09d: daload
      // 09e: aload 3
      // 09f: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.size [F
      // 0a2: iload 5
      // 0a4: faload
      // 0a5: f2d
      // 0a6: dadd
      // 0a7: dload 15
      // 0a9: invokevirtual meowtils/furiahvisuals/fx/physics/BlockCollider.solid (DDD)Z
      // 0ac: ifeq 0ed
      // 0af: iload 5
      // 0b1: istore 17
      // 0b3: aload 3
      // 0b4: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vx [F
      // 0b7: iload 17
      // 0b9: aload 3
      // 0ba: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vx [F
      // 0bd: iload 17
      // 0bf: faload
      // 0c0: ldc_w -0.35
      // 0c3: fmul
      // 0c4: fastore
      // 0c5: iload 5
      // 0c7: istore 18
      // 0c9: aload 3
      // 0ca: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vz [F
      // 0cd: iload 18
      // 0cf: aload 3
      // 0d0: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vz [F
      // 0d3: iload 18
      // 0d5: faload
      // 0d6: ldc_w -0.35
      // 0d9: fmul
      // 0da: fastore
      // 0db: aload 3
      // 0dc: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.x [D
      // 0df: iload 5
      // 0e1: daload
      // 0e2: dstore 11
      // 0e4: aload 3
      // 0e5: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.z [D
      // 0e8: iload 5
      // 0ea: daload
      // 0eb: dstore 15
      // 0ed: dload 13
      // 0ef: aload 3
      // 0f0: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.ground Lmeowtils/furiahvisuals/fx/Terrain$HeightField;
      // 0f3: dload 11
      // 0f5: dload 15
      // 0f7: invokevirtual meowtils/furiahvisuals/fx/Terrain$HeightField.at (DD)D
      // 0fa: dup2
      // 0fb: dstore 6
      // 0fd: invokestatic java/lang/Double.isNaN (D)Z
      // 100: ifeq 10d
      // 103: aload 1
      // 104: getfield meowtils/furiahvisuals/module/kill/KillFx.ctx Lmeowtils/furiahvisuals/module/kill/KillContext;
      // 107: getfield meowtils/furiahvisuals/module/kill/KillContext.groundY D
      // 10a: goto 10f
      // 10d: dload 6
      // 10f: aload 3
      // 110: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.size [F
      // 113: iload 5
      // 115: faload
      // 116: f2d
      // 117: dadd
      // 118: dup2
      // 119: dstore 8
      // 11b: dcmpg
      // 11c: ifge 1bb
      // 11f: aload 3
      // 120: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vy [F
      // 123: iload 5
      // 125: faload
      // 126: fconst_0
      // 127: fcmpg
      // 128: ifge 1bb
      // 12b: dload 8
      // 12d: dstore 13
      // 12f: aload 3
      // 130: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vy [F
      // 133: iload 5
      // 135: faload
      // 136: ldc_w -2.5
      // 139: fcmpg
      // 13a: ifge 198
      // 13d: iload 5
      // 13f: istore 17
      // 141: aload 3
      // 142: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vy [F
      // 145: iload 17
      // 147: aload 3
      // 148: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vy [F
      // 14b: iload 17
      // 14d: faload
      // 14e: ldc_w -0.3
      // 151: fmul
      // 152: fastore
      // 153: iload 5
      // 155: istore 18
      // 157: aload 3
      // 158: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vx [F
      // 15b: iload 18
      // 15d: aload 3
      // 15e: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vx [F
      // 161: iload 18
      // 163: faload
      // 164: ldc_w 0.5
      // 167: fmul
      // 168: fastore
      // 169: iload 5
      // 16b: istore 19
      // 16d: aload 3
      // 16e: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vz [F
      // 171: iload 19
      // 173: aload 3
      // 174: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vz [F
      // 177: iload 19
      // 179: faload
      // 17a: ldc_w 0.5
      // 17d: fmul
      // 17e: fastore
      // 17f: iload 5
      // 181: istore 20
      // 183: aload 3
      // 184: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.spin [F
      // 187: iload 20
      // 189: aload 3
      // 18a: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.spin [F
      // 18d: iload 20
      // 18f: faload
      // 190: ldc_w 0.5
      // 193: fmul
      // 194: fastore
      // 195: goto 1bb
      // 198: aload 3
      // 199: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.landed [F
      // 19c: iload 5
      // 19e: aload 1
      // 19f: getfield meowtils/furiahvisuals/module/kill/KillFx.t F
      // 1a2: fastore
      // 1a3: aload 3
      // 1a4: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vz [F
      // 1a7: iload 5
      // 1a9: fconst_0
      // 1aa: fastore
      // 1ab: aload 3
      // 1ac: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vy [F
      // 1af: iload 5
      // 1b1: fconst_0
      // 1b2: fastore
      // 1b3: aload 3
      // 1b4: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.vx [F
      // 1b7: iload 5
      // 1b9: fconst_0
      // 1ba: fastore
      // 1bb: aload 3
      // 1bc: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.x [D
      // 1bf: iload 5
      // 1c1: dload 11
      // 1c3: dastore
      // 1c4: aload 3
      // 1c5: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.y [D
      // 1c8: iload 5
      // 1ca: dload 13
      // 1cc: dastore
      // 1cd: aload 3
      // 1ce: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.z [D
      // 1d1: iload 5
      // 1d3: dload 15
      // 1d5: dastore
      // 1d6: iload 5
      // 1d8: istore 17
      // 1da: aload 3
      // 1db: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.angle [F
      // 1de: iload 17
      // 1e0: aload 3
      // 1e1: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.angle [F
      // 1e4: iload 17
      // 1e6: faload
      // 1e7: aload 3
      // 1e8: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.spin [F
      // 1eb: iload 5
      // 1ed: faload
      // 1ee: fload 2
      // 1ef: fmul
      // 1f0: fadd
      // 1f1: fastore
      // 1f2: iinc 5 1
      // 1f5: goto 011
      // 1f8: bipush 24
      // 1fa: aload 1
      // 1fb: getfield meowtils/furiahvisuals/module/kill/KillFx.t F
      // 1fe: ldc 0.75
      // 200: fdiv
      // 201: invokestatic meowtils/furiahvisuals/render/Easing.clamp01 (F)F
      // 204: ldc_w 24.0
      // 207: fmul
      // 208: f2i
      // 209: invokestatic java/lang/Math.min (II)I
      // 20c: istore 5
      // 20e: aload 1
      // 20f: getfield meowtils/furiahvisuals/module/kill/KillFx.lod Z
      // 212: ifne 325
      // 215: aload 0
      // 216: aload 1
      // 217: invokespecial meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle.reach (Lmeowtils/furiahvisuals/module/kill/KillFx;)D
      // 21a: dstore 6
      // 21c: aload 3
      // 21d: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.dustDone I
      // 220: iload 5
      // 222: if_icmpge 325
      // 225: aload 3
      // 226: dup
      // 227: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.dustDone I
      // 22a: dup_x1
      // 22b: bipush 1
      // 22c: iadd
      // 22d: putfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.dustDone I
      // 230: istore 8
      // 232: dload 6
      // 234: iload 8
      // 236: bipush 1
      // 237: iadd
      // 238: i2d
      // 239: ldc2_w 24.0
      // 23c: ddiv
      // 23d: ldc2_w 0.66
      // 240: invokestatic java/lang/Math.pow (DD)D
      // 243: dmul
      // 244: dstore 9
      // 246: bipush 0
      // 247: istore 11
      // 249: iload 11
      // 24b: bipush 3
      // 24c: if_icmpge 322
      // 24f: aload 1
      // 250: getfield meowtils/furiahvisuals/module/kill/KillFx.rng Lmeowtils/furiahvisuals/fx/Rng;
      // 253: invokevirtual meowtils/furiahvisuals/fx/Rng.next ()F
      // 256: f2d
      // 257: ldc2_w 6.2832
      // 25a: dmul
      // 25b: dstore 12
      // 25d: aload 1
      // 25e: getfield meowtils/furiahvisuals/module/kill/KillFx.ctx Lmeowtils/furiahvisuals/module/kill/KillContext;
      // 261: getfield meowtils/furiahvisuals/module/kill/KillContext.feetX D
      // 264: dload 12
      // 266: invokestatic java/lang/Math.cos (D)D
      // 269: dload 9
      // 26b: dmul
      // 26c: dadd
      // 26d: dstore 14
      // 26f: aload 1
      // 270: getfield meowtils/furiahvisuals/module/kill/KillFx.ctx Lmeowtils/furiahvisuals/module/kill/KillContext;
      // 273: getfield meowtils/furiahvisuals/module/kill/KillContext.feetZ D
      // 276: dload 12
      // 278: invokestatic java/lang/Math.sin (D)D
      // 27b: dload 9
      // 27d: dmul
      // 27e: dadd
      // 27f: dstore 16
      // 281: aload 3
      // 282: getfield meowtils/furiahvisuals/module/kill/styles/ShockwaveStyle$State.ground Lmeowtils/furiahvisuals/fx/Terrain$HeightField;
      // 285: dload 14
      // 287: dload 16
      // 289: invokevirtual meowtils/furiahvisuals/fx/Terrain$HeightField.at (DD)D
      // 28c: dstore 18
      // 28e: aload 1
      // 28f: getfield meowtils/furiahvisuals/module/kill/KillFx.particles Lmeowtils/furiahvisuals/fx/FxParticles;
      // 292: bipush 8
      // 294: dload 14
      // 296: dload 18
      // 298: invokestatic java/lang/Double.isNaN (D)Z
      // 29b: ifeq 2a8
      // 29e: aload 1
      // 29f: getfield meowtils/furiahvisuals/module/kill/KillFx.ctx Lmeowtils/furiahvisuals/module/kill/KillContext;
      // 2a2: getfield meowtils/furiahvisuals/module/kill/KillContext.groundY D
      // 2a5: goto 2aa
      // 2a8: dload 18
      // 2aa: ldc2_w 0.1
      // 2ad: dadd
      // 2ae: dload 16
      // 2b0: dload 12
      // 2b2: invokestatic java/lang/Math.cos (D)D
      // 2b5: d2f
      // 2b6: ldc_w 1.2
      // 2b9: fmul
      // 2ba: ldc_w 0.3
      // 2bd: aload 1
      // 2be: getfield meowtils/furiahvisuals/module/kill/KillFx.rng Lmeowtils/furiahvisuals/fx/Rng;
      // 2c1: invokevirtual meowtils/furiahvisuals/fx/Rng.next ()F
      // 2c4: ldc_w 0.4
      // 2c7: fmul
      // 2c8: fadd
      // 2c9: dload 12
      // 2cb: invokestatic java/lang/Math.sin (D)D
      // 2ce: d2f
      // 2cf: ldc_w 1.2
      // 2d2: fmul
      // 2d3: ldc_w 1.2
      // 2d6: aload 1
      // 2d7: getfield meowtils/furiahvisuals/module/kill/KillFx.rng Lmeowtils/furiahvisuals/fx/Rng;
      // 2da: invokevirtual meowtils/furiahvisuals/fx/Rng.next ()F
      // 2dd: ldc_w 0.6
      // 2e0: fmul
      // 2e1: fadd
      // 2e2: ldc_w 0.35
      // 2e5: aload 1
      // 2e6: getfield meowtils/furiahvisuals/module/kill/KillFx.rng Lmeowtils/furiahvisuals/fx/Rng;
      // 2e9: invokevirtual meowtils/furiahvisuals/fx/Rng.next ()F
      // 2ec: ldc_w 0.3
      // 2ef: fmul
      // 2f0: fadd
      // 2f1: aload 1
      // 2f2: getfield meowtils/furiahvisuals/module/kill/KillFx.size F
      // 2f5: fmul
      // 2f6: invokevirtual meowtils/furiahvisuals/fx/FxParticles.spawn (IDDDFFFFF)Lmeowtils/furiahvisuals/fx/FxParticles;
      // 2f9: ldc_w 0.9
      // 2fc: aload 1
      // 2fd: getfield meowtils/furiahvisuals/module/kill/KillFx.size F
      // 300: fmul
      // 301: invokevirtual meowtils/furiahvisuals/fx/FxParticles.end (F)Lmeowtils/furiahvisuals/fx/FxParticles;
      // 304: ldc_w 1.4
      // 307: invokevirtual meowtils/furiahvisuals/fx/FxParticles.drag (F)Lmeowtils/furiahvisuals/fx/FxParticles;
      // 30a: ldc_w -6648712
      // 30d: invokevirtual meowtils/furiahvisuals/fx/FxParticles.color (I)Lmeowtils/furiahvisuals/fx/FxParticles;
      // 310: ldc_w 0.5
      // 313: aload 1
      // 314: getfield meowtils/furiahvisuals/module/kill/KillFx.opacity F
      // 317: fmul
      // 318: invokevirtual meowtils/furiahvisuals/fx/FxParticles.alpha (F)Lmeowtils/furiahvisuals/fx/FxParticles;
      // 31b: pop
      // 31c: iinc 11 1
      // 31f: goto 249
      // 322: goto 21c
      // 325: return
   }

   @Override
   public void render(KillFx fx, FxFrame f) {
      ShockwaveStyle.State s = (ShockwaveStyle.State)fx.data;
      float t = fx.t;
      float a = fx.alpha();
      double r = this.reach(fx);
      double cx = f.rx(fx.ctx.midX());
      double cy = f.ry(fx.ctx.midY());
      double cz = f.rz(fx.ctx.midZ());
      float flash = 1.0F - Easing.smoothstep(0.0F, 0.25F, t);
      fx.look.glow = this.color.getColor(0.5F);
      fx.look.glowAmount = 0.8F * flash;
      fx.drawBody(f);
      RenderState.additive();
      if (flash > 0.01F) {
         float g = Math.max(0.35F, fx.glow);
         Vfx.sprite(Textures.GLOW, cx, cy, cz, 5.5 * fx.size * (fx.crit ? 1.3 : 1.0), 0.0F, alpha(this.color.getColor(0.5F), 0.6F * a * flash * g));
         Vfx.sprite(Textures.FLARE, cx, cy, cz, 3.2 * fx.size, t * 2.0F, core(this.color.getColor(0.5F), 0.9F * a * flash));
      }

      float shellT;
      if ((shellT = Easing.clamp01(t / 0.6F)) < 1.0F && !fx.lod) {
         double sr = 0.4 + r * 0.55 * Easing.outCubic(shellT);
         float sa = a * (1.0F - shellT);
         if (this.distortion.get()) {
            WarpShell.draw(
               f,
               fx.ctx.midX(),
               fx.ctx.midY(),
               fx.ctx.midZ(),
               sr,
               0.0,
               1.0,
               0.0,
               1.0,
               1.0,
               0.0,
               1.0,
               0.0,
               0.0,
               1.0,
               false,
               1.4F,
               this.color.getColor(0.5F),
               0.6F,
               sa,
               Distortion.refracts() && !f.preview
            );
            RenderState.additive();
         } else {
            Vfx.sprite(Textures.BUBBLE, cx, cy, cz, sr * 2.0, 0.0F, alpha(this.color.getColor(0.5F), 0.5F * sa));
         }
      }

      float waveT;
      if ((waveT = Easing.clamp01(t / 0.75F)) < 1.0F || t < 1.15F) {
         double wr = r * Math.pow(waveT, 0.66);
         float wa = a * (1.0F - Easing.smoothstep(0.55F, 1.0F, Easing.clamp01(t / 1.15F)));
         if (wa > 0.01F && wr > 0.05) {
            Neon.ring(f, s.ground, fx.ctx.feetX, fx.ctx.feetZ, wr, 0.12 * fx.size, 72, this.color, 0, wa, t * 0.2F);
            RenderState.decal(true);
            double band = 0.9 * fx.size;
            Vfx.flatBand(
               Textures.WAVE,
               f.rx(fx.ctx.feetX),
               f.ry(fx.ctx.groundY) + 0.03,
               f.rz(fx.ctx.feetZ),
               Math.max(0.0, wr - band),
               wr,
               0.0,
               Math.PI * 2,
               72,
               alpha(this.color.getColor(0.3F), 0.55F * wa)
            );
            RenderState.decal(false);
         }
      }

      this.drawDebris(fx, f, s, a);
   }

   private void drawDebris(KillFx fx, FxFrame f, ShockwaveStyle.State s, float a) {
      if (s.count != 0) {
         RenderState.translucent(true);

         for (int i = 0; i < s.count; i++) {
            if (!(fx.t < s.born[i])) {
               float fade = s.landed[i] < 0.0F ? 1.0F : 1.0F - Easing.smoothstep(0.35F, 0.9F, fx.t - s.landed[i]);
               float grow = Easing.outCubic(Easing.clamp01((fx.t - s.born[i]) / 0.08F));
               float alpha = a * fade * (1.0F - Easing.smoothstep(fx.styleSeconds - 0.3F, fx.styleSeconds, fx.t));
               if (!(alpha <= 0.01F)) {
                  axisAngle(s.ax[i], s.ay[i], s.az[i], s.angle[i], this.m);
                  double h = s.size[i] * grow;
                  float u0 = s.u0 + s.u[i] * s.du;
                  float v0 = s.v0 + s.v[i] * s.dv;
                  float u1 = u0 + s.du * 0.25F;
                  float v1 = v0 + s.dv * 0.25F;
                  this.cube(f, s.texture, s.x[i], s.y[i], s.z[i], h, u0, v0, u1, v1, alpha);
               }
            }
         }

         Vfx.flush();
         RenderState.additive();
      }
   }

   private static void axisAngle(double x, double y, double z, double angle, double[] out) {
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      double t = 1.0 - c;
      out[0] = t * x * x + c;
      out[1] = t * x * y - s * z;
      out[2] = t * x * z + s * y;
      out[3] = t * x * y + s * z;
      out[4] = t * y * y + c;
      out[5] = t * y * z - s * x;
      out[6] = t * x * z - s * y;
      out[7] = t * y * z + s * x;
      out[8] = t * z * z + c;
   }

   private void cube(FxFrame f, int texture, double x, double y, double z, double h, float u0, float v0, float u1, float v1, float alpha) {
      for (int c = 0; c < 8; c++) {
         double lx = (c & 1) != 0 ? h : -h;
         double ly = (c & 2) != 0 ? h : -h;
         double lz = (c & 4) != 0 ? h : -h;
         this.px[c] = f.rx(x) + this.m[0] * lx + this.m[1] * ly + this.m[2] * lz;
         this.py[c] = f.ry(y) + this.m[3] * lx + this.m[4] * ly + this.m[5] * lz;
         this.pz[c] = f.rz(z) + this.m[6] * lx + this.m[7] * ly + this.m[8] * lz;
      }

      for (int[] face : FACES) {
         double ay = this.py[face[1]] - this.py[face[0]];
         double bz = this.pz[face[3]] - this.pz[face[0]];
         double az = this.pz[face[1]] - this.pz[face[0]];
         double by = this.py[face[3]] - this.py[face[0]];
         double nx = ay * bz - az * by;
         double bx = this.px[face[3]] - this.px[face[0]];
         double ax = this.px[face[1]] - this.px[face[0]];
         double ny = az * bx - ax * bz;
         double nz = ax * by - ay * bx;
         double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
         if (!(nl < 1.0E-12)) {
            double mx = (this.px[face[0]] + this.px[face[2]]) * 0.5 - f.rx(x);
            if (nx * mx + ny * ((this.py[face[0]] + this.py[face[2]]) * 0.5 - f.ry(y)) + nz * ((this.pz[face[0]] + this.pz[face[2]]) * 0.5 - f.rz(z)) < 0.0) {
               nx = -nx;
               ny = -ny;
               nz = -nz;
            }

            if (!(nx * (this.px[face[0]] - Vfx.camera().posX) + ny * (this.py[face[0]] - Vfx.camera().posY) + nz * (this.pz[face[0]] - Vfx.camera().posZ) > 0.0)
               )
             {
               float shade = (float)(0.62 + 0.38 * Math.max(0.0, ny / nl) + 0.12 * Math.abs(nx / nl));
               int g = Math.min(255, Math.round(255.0F * shade));
               int c = Math.max(0, Math.min(255, Math.round(alpha * 255.0F))) << 24 | g << 16 | g << 8 | g;
               Vfx.quadUV(
                  texture,
                  this.px[face[0]],
                  this.py[face[0]],
                  this.pz[face[0]],
                  u0,
                  v0,
                  c,
                  this.px[face[1]],
                  this.py[face[1]],
                  this.pz[face[1]],
                  u1,
                  v0,
                  c,
                  this.px[face[2]],
                  this.py[face[2]],
                  this.pz[face[2]],
                  u1,
                  v1,
                  c,
                  this.px[face[3]],
                  this.py[face[3]],
                  this.pz[face[3]],
                  u0,
                  v1,
                  c
               );
            }
         }
      }
   }

   private static final class State {
      final double[] x = new double[72];
      final double[] y = new double[72];
      final double[] z = new double[72];
      final float[] vx = new float[72];
      final float[] vy = new float[72];
      final float[] vz = new float[72];
      final float[] ax = new float[72];
      final float[] ay = new float[72];
      final float[] az = new float[72];
      final float[] angle = new float[72];
      final float[] spin = new float[72];
      final float[] size = new float[72];
      final float[] born = new float[72];
      final float[] landed = new float[72];
      final float[] u = new float[72];
      final float[] v = new float[72];
      final float[] ringAngle = new float[72];
      int count;
      int spawned;
      Terrain.HeightField ground;
      int texture;
      float u0;
      float v0;
      float du;
      float dv;
      int dustDone;

      private State() {
      }
   }
}
