package meowtils.furiahvisuals.module.kill.styles;

import java.util.Locale;
import meowtils.furiahvisuals.core.FuriahsVisuals;
import meowtils.furiahvisuals.fx.FxFrame;
import meowtils.furiahvisuals.fx.Neon;
import meowtils.furiahvisuals.fx.physics.PhysicsClock;
import meowtils.furiahvisuals.fx.physics.RigidBox;
import meowtils.furiahvisuals.module.kill.KillEffectsModule;
import meowtils.furiahvisuals.module.kill.KillFx;
import meowtils.furiahvisuals.module.kill.KillStyle;
import meowtils.furiahvisuals.module.kill.KillStyleBase;
import meowtils.furiahvisuals.render.BodyProxy;
import meowtils.furiahvisuals.render.ColorUtil;
import meowtils.furiahvisuals.render.Easing;
import meowtils.furiahvisuals.render.PlayerModel;
import meowtils.furiahvisuals.render.Vfx;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.SliderSetting;

public final class SliceStyle extends KillStyleBase {
   private static final int POINTS = 36;
   private static final double SPAN = Math.toRadians(122.0);
   private static final float SLASH = 0.16F;
   private final SliderSetting slashes;
   private final SliderSetting slide;
   private final SliderSetting glow;
   private final double[] cx = new double[8];
   private final double[] cy = new double[8];
   private final double[] cz = new double[8];
   private static final double[] IDENTITY = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0};
   private static final int[][] EDGES = new int[][]{{0, 1}, {1, 3}, {3, 2}, {2, 0}, {4, 5}, {5, 7}, {7, 6}, {6, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
   private final float[] corner = new float[3];
   private final double[] c = new double[3];
   private final double[] r = new double[9];
   private final double[] now = new double[9];

   public SliceStyle(KillEffectsModule module) {
      super(module, KillStyle.SLICE, new ColorSetting("slice.color", "Color", "Color of the blade and the cut faces.", -9706497));
      this.slashes = this.setting(new SliderSetting("slice.slashes", "Slash Count", "How many blade arcs sweep across.", 1.0, 3.0, 1.0, 1.0));
      this.slide = this.setting(new SliderSetting("slice.slide", "Slide Speed", "How fast the upper part slides off.", 0.3, 2.0, 1.0, 0.05).unit("x"));
      this.glow = this.setting(new SliderSetting("slice.glow", "Cut Glow", "How bright the cut faces glow.", 0.0, 2.0, 1.0, 0.05).unit("x"));
   }

   @Override
   public float seconds(KillFx fx) {
      return 2.6F;
   }

   @Override
   public boolean leavesBody() {
      return true;
   }

   @Override
   public void start(KillFx fx) {
      SliceStyle.State s = new SliceStyle.State();
      fx.data = s;
      double dx = fx.ctx.dirX;
      double dy = fx.ctx.dirY;
      double dz = fx.ctx.dirZ;
      double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
      dx /= dl;
      dy /= dl;
      dz /= dl;
      double sx = fx.ctx.swipeX;
      double sy = fx.ctx.swipeY;
      double sz = fx.ctx.swipeZ;
      double along;
      double sl;
      if ((sl = Math.sqrt((sx = sx - dx * (along = sx * dx + sy * dy + sz * dz)) * sx + (sy = sy - dy * along) * sy + (sz = sz - dz * along) * sz)) < 1.0E-4) {
         sx = -dz;
         sy = 0.0;
         sz = dx;
         sl = Math.sqrt(sx * sx + dx * dx);
      }

      s.swx = sx / sl;
      s.swy = sy / sl;
      s.swz = sz / sl;
      double nx = s.swy * dz - s.swz * dy;
      double ny = s.swz * dx - s.swx * dz;
      double nz = s.swx * dy - s.swy * dx;
      double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
      nx /= nl;
      ny /= nl;
      nz /= nl;
      if (ny < 0.0) {
         nx = -nx;
         ny = -ny;
         nz = -nz;
      }

      s.n1x = nx;
      s.n1y = ny;
      s.n1z = nz;
      s.px = fx.ctx.impactX;
      s.py = Math.max(fx.ctx.feetY + 0.75, Math.min(fx.ctx.feetY + 1.45, fx.ctx.impactY));
      s.pz = fx.ctx.impactZ;
      double qx = fx.ctx.feetX;
      double qz = fx.ctx.feetZ;
      double d1 = nx * qx + ny * s.py + nz * qz;
      int cap = ColorUtil.mulAlpha(this.color.getColor(0.5F), Math.min(1.0F, 0.55F * this.glow.getFloat() + 0.1F));
      if (fx.crit) {
         double c = Math.cos(1.22);
         double sn = Math.sin(1.22);
         double cx = dy * nz - dz * ny;
         double cy = dz * nx - dx * nz;
         double cz = dx * ny - dy * nx;
         s.n2x = nx * c + cx * sn;
         s.n2y = ny * c + cy * sn;
         s.n2z = nz * c + cz * sn;
         double d2 = s.n2x * qx + s.n2y * s.py + s.n2z * qz;
         this.piece(fx, s, nx, ny, nz, d1, true, s.n2x, s.n2y, s.n2z, d2, cap, 0.1F);
         this.piece(fx, s, nx, ny, nz, d1, true, -s.n2x, -s.n2y, -s.n2z, -d2, cap, 0.1F);
      } else {
         this.piece(fx, s, nx, ny, nz, d1, false, 0.0, 0.0, 0.0, 0.0, cap, 0.1F);
      }

      this.piece(fx, s, -nx, -ny, -nz, -d1, false, 0.0, 0.0, 0.0, 0.0, cap, 0.35F);
      double k = this.slide.getFloat() * (fx.crit ? 1.2 : 1.0) * (fx.preview ? 0.5 : 1.0);

      for (int i = 0; i < s.count; i++) {
         SliceStyle.Piece p = s.pieces[i];
         boolean lower = i == s.count - 1;
         if (lower) {
            double tx = p.r0[2];
            double tz = p.r0[8];
            if (dx * tx + dz * tz < 0.0) {
               tx = -tx;
               tz = -tz;
            }

            p.vx = dx * 1.2;
            p.vy = 0.4;
            p.vz = dz * 1.2;
            p.wx = tz * 5.0;
            p.wy = fx.rng.signed() * 0.8;
            p.wz = -tx * 5.0;
         } else {
            double gx = -nx * -ny;
            double gy = -1.0 + ny * ny;
            double gz = -nz * -ny;
            double gl = Math.sqrt(gx * gx + gy * gy + gz * gz);
            double mz;
            double my;
            double mx;
            if (gl < 0.25) {
               mx = s.swx;
               my = 0.15;
               mz = s.swz;
            } else {
               mx = gx / gl;
               my = gy / gl;
               mz = gz / gl;
            }

            double side = fx.crit ? (i == 0 ? 1 : -1) : 0.0;
            p.vx = (mx + s.n2x * side * 0.8) * 2.3 * k + dx * 0.8;
            p.vy = Math.max(my * 2.3 * k, 0.4) + 0.6;
            double ax = p.vz = (mz + s.n2z * side * 0.8) * 2.3 * k + dz * 0.8;
            double az = -p.vx;
            double al = Math.sqrt(ax * ax + az * az);
            p.wx = al < 1.0E-4 ? 0.0 : ax / al * 3.2;
            p.wy = fx.rng.signed() * 1.5;
            p.wz = al < 1.0E-4 ? 0.0 : az / al * 3.2;
         }
      }
   }

   @Override
   public boolean focus(KillFx fx, double[] out) {
      if (!(fx.data instanceof SliceStyle.State)) {
         return false;
      } else {
         SliceStyle.State s = (SliceStyle.State)fx.data;
         double x = 0.0;
         double z = 0.0;
         int n = 0;

         for (int i = 0; i < s.count; i++) {
            if (!s.pieces[i].empty) {
               x += s.pieces[i].box.cx;
               z += s.pieces[i].box.cz;
               n++;
            }
         }

         if (n == 0) {
            return false;
         } else {
            out[0] = x / n;
            out[1] = z / n;
            return true;
         }
      }
   }

   public static int piecesForTest(KillFx fx) {
      if (!(fx.data instanceof SliceStyle.State)) {
         return 0;
      } else {
         SliceStyle.State s = (SliceStyle.State)fx.data;
         int n = 0;

         for (int i = 0; i < s.count; i++) {
            if (!s.pieces[i].empty) {
               n++;
            }
         }

         return n;
      }
   }

   private void piece(
      KillFx fx,
      SliceStyle.State s,
      double n1x,
      double n1y,
      double n1z,
      double d1,
      boolean clip2,
      double n2x,
      double n2y,
      double n2z,
      double d2,
      int cap,
      float delay
   ) {
      SliceStyle.Piece p = new SliceStyle.Piece();
      double yaw = Math.toRadians(fx.ctx.body.pose.bodyYaw);
      double sideX = Math.cos(yaw);
      double sideZ = Math.sin(yaw);
      double frontX = -Math.sin(yaw);
      double frontZ = Math.cos(yaw);
      double[] r0 = p.r0;
      r0[0] = sideX;
      r0[2] = frontX;
      r0[4] = 1.0;
      r0[6] = sideZ;
      r0[8] = frontZ;
      double minX = 1.0E9;
      double minY = 1.0E9;
      double minZ = 1.0E9;
      double maxX = -1.0E9;
      double maxY = -1.0E9;
      double maxZ = -1.0E9;
      boolean any = false;

      for (int i = 0; i < 6; i++) {
         BodyProxy.Part src = fx.parts[i];
         p.start[i].copy(src);
         p.start[i].clip = true;
         p.start[i].cnx = n1x;
         p.start[i].cny = n1y;
         p.start[i].cnz = n1z;
         p.start[i].cd = d1;
         p.start[i].clip2 = clip2;
         p.start[i].c2x = n2x;
         p.start[i].c2y = n2y;
         p.start[i].c2z = n2z;
         p.start[i].c2d = d2;
         p.start[i].capColor = cap;
         PlayerModel.Box b = PlayerModel.base(i, fx.ctx.body.slim);
         this.corners(src, b);

         for (int c = 0; c < 8; c++) {
            if (kept(this.cx[c], this.cy[c], this.cz[c], n1x, n1y, n1z, d1, clip2, n2x, n2y, n2z, d2)) {
               any = true;
               double u = this.cx[c] * sideX + this.cz[c] * sideZ;
               double w = this.cx[c] * frontX + this.cz[c] * frontZ;
               minX = Math.min(minX, u);
               minY = Math.min(minY, this.cy[c]);
               minZ = Math.min(minZ, w);
               maxX = Math.max(maxX, u);
               maxY = Math.max(maxY, this.cy[c]);
               maxZ = Math.max(maxZ, w);
            }
         }

         for (int[] e : EDGES) {
            for (int pl = 0; pl < (clip2 ? 2 : 1); pl++) {
               double ax = pl == 0 ? n1x : n2x;
               double ad;
               double az;
               double ay;
               double da = this.cx[e[0]] * ax
                  + this.cy[e[0]] * (ay = pl == 0 ? n1y : n2y)
                  + this.cz[e[0]] * (az = pl == 0 ? n1z : n2z)
                  - (ad = pl == 0 ? d1 : d2);
               double z;
               double y;
               double t;
               double x;
               double db;
               if (da >= 0.0 != (db = this.cx[e[1]] * ax + this.cy[e[1]] * ay + this.cz[e[1]] * az - ad) >= 0.0
                  && kept(
                     x = this.cx[e[0]] + (this.cx[e[1]] - this.cx[e[0]]) * (t = da / (da - db)),
                     y = this.cy[e[0]] + (this.cy[e[1]] - this.cy[e[0]]) * t,
                     z = this.cz[e[0]] + (this.cz[e[1]] - this.cz[e[0]]) * t,
                     n1x,
                     n1y,
                     n1z,
                     d1 - 1.0E-6,
                     clip2,
                     n2x,
                     n2y,
                     n2z,
                     d2 - 1.0E-6
                  )) {
                  any = true;
                  double u = x * sideX + z * sideZ;
                  double w = x * frontX + z * frontZ;
                  minX = Math.min(minX, u);
                  minY = Math.min(minY, y);
                  minZ = Math.min(minZ, w);
                  maxX = Math.max(maxX, u);
                  maxY = Math.max(maxY, y);
                  maxZ = Math.max(maxZ, w);
               }
            }
         }
      }

      p.empty = !any;
      double cu = (minX + maxX) * 0.5;
      double cw = (minZ + maxZ) * 0.5;
      p.c0x = cu * sideX + cw * frontX;
      p.c0y = (minY + maxY) * 0.5;
      p.c0z = cu * sideZ + cw * frontZ;
      p.n1x = n1x;
      p.n1y = n1y;
      p.n1z = n1z;
      p.d1 = d1;
      p.clip2 = clip2;
      p.n2x = n2x;
      p.n2y = n2y;
      p.n2z = n2z;
      p.d2 = d2;
      p.delay = delay;
      if (any) {
         p.box.place(p.c0x, p.c0y, p.c0z, r0, Math.max(0.05, (maxX - minX) * 0.5), Math.max(0.05, (maxY - minY) * 0.5), Math.max(0.05, (maxZ - minZ) * 0.5));
         p.box.restitution = 0.2F;
         p.box.friction = 0.6F;
      }

      s.pieces[s.count++] = p;
   }

   private void corners(BodyProxy.Part p, PlayerModel.Box b) {
      double k = 0.05859375 * p.scale;

      for (int c = 0; c < 8; c++) {
         double lx = ((c & 1) != 0 ? b.w : 0.0F) + b.x - b.cx();
         double ly = ((c & 2) != 0 ? b.h : 0.0F) + b.y - b.cy();
         double lz = ((c & 4) != 0 ? b.d : 0.0F) + b.z - b.cz();
         double var12;
         double var13;
         double var14;
         this.cx[c] = p.x + p.r[0] * (var12 = lx * k) + p.r[1] * (var13 = ly * k) + p.r[2] * (var14 = lz * k);
         this.cy[c] = p.y + p.r[3] * var12 + p.r[4] * var13 + p.r[5] * var14;
         this.cz[c] = p.z + p.r[6] * var12 + p.r[7] * var13 + p.r[8] * var14;
      }
   }

   private static boolean kept(
      double x, double y, double z, double n1x, double n1y, double n1z, double d1, boolean clip2, double n2x, double n2y, double n2z, double d2
   ) {
      return x * n1x + y * n1y + z * n1z >= d1 && (!clip2 || x * n2x + y * n2y + z * n2z >= d2);
   }

   @Override
   public void update(KillFx fx, float dt) {
      SliceStyle.State s = (SliceStyle.State)fx.data;
      if (!s.sparked && fx.t >= 0.08F) {
         s.sparked = true;
         if (!fx.lod) {
            for (int i = 0; i < fx.count(18.0F, true); i++) {
               double o = fx.rng.signed() * 0.45;
               fx.particles
                  .spawn(
                     2,
                     s.px + s.swx * o,
                     s.py + s.swy * o,
                     s.pz + s.swz * o,
                     (float)(s.swx * 4.0 + fx.ctx.dirX * 1.5 + fx.rng.signed()),
                     (float)(s.swy * 4.0 + 1.0 + fx.rng.signed()),
                     (float)(s.swz * 4.0 + fx.ctx.dirZ * 1.5 + fx.rng.signed()),
                     0.35F + fx.rng.next() * 0.25F,
                     0.02F * fx.size
                  )
                  .gravity(8.0F)
                  .drag(2.0F)
                  .color(this.color, fx.rng.next())
                  .hot(0.7F)
                  .alpha(fx.opacity);
            }
         }
      }

      int steps = s.clock.advance(dt);

      for (int i = 0; i < s.count; i++) {
         SliceStyle.Piece p = s.pieces[i];
         if (!p.empty) {
            if (!p.moving && fx.t >= p.delay) {
               p.moving = true;
               p.box.setVelocity(p.vx, p.vy, p.vz, p.wx, p.wy, p.wz, 0.008333333333333333);
            }

            if (p.moving) {
               for (int k = 0; k < steps; k++) {
                  p.box.step(fx.collider(), 26.0, 0.004);
               }
            }
         }
      }

      if (fx.trace && (int)(fx.t * 4.0F) != (int)((fx.t - dt) * 4.0F)) {
         StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "[slice] t=%.2f", fx.t));

         for (int ix = 0; ix < s.count; ix++) {
            SliceStyle.Piece p = s.pieces[ix];
            sb.append(
               String.format(
                  Locale.ROOT,
                  " | piece %d%s c=(%.2f %.2f %.2f) half=(%.2f %.2f %.2f) axisY=(%.2f %.2f %.2f) %s",
                  ix,
                  p.empty ? " empty" : "",
                  p.box.cx - fx.ctx.feetX,
                  p.box.cy - fx.ctx.feetY,
                  p.box.cz - fx.ctx.feetZ,
                  p.box.hx,
                  p.box.hy,
                  p.box.hz,
                  p.box.rot[1],
                  p.box.rot[4],
                  p.box.rot[7],
                  p.box.sleeping ? "asleep" : (p.moving ? "moving" : "held")
               )
            );
         }

         FuriahsVisuals.LOG.info(sb.toString());
      }
   }

   @Override
   public void render(KillFx fx, FxFrame f) {
      SliceStyle.State s = (SliceStyle.State)fx.data;
      double alpha = s.clock.alpha();

      for (int i = 0; i < s.count; i++) {
         SliceStyle.Piece p = s.pieces[i];
         if (!p.empty) {
            if (p.moving) {
               p.box.interpolate(alpha, this.c, this.now);

               for (int row = 0; row < 3; row++) {
                  for (int col = 0; col < 3; col++) {
                     this.r[row * 3 + col] = this.now[row * 3] * p.r0[col * 3]
                        + this.now[row * 3 + 1] * p.r0[col * 3 + 1]
                        + this.now[row * 3 + 2] * p.r0[col * 3 + 2];
                  }
               }
            } else {
               this.c[0] = p.c0x;
               this.c[1] = p.c0y;
               this.c[2] = p.c0z;
               System.arraycopy(IDENTITY, 0, this.r, 0, 9);
            }

            place(p, this.c, this.r);
            fx.look.glowAmount = 0.0F;
            fx.drawParts(f, p.parts);
         }
      }

      this.drawSlashes(fx, f, s);
   }

   private static void place(SliceStyle.Piece p, double[] c, double[] r) {
      for (int i = 0; i < 6; i++) {
         BodyProxy.Part src = p.start[i];
         BodyProxy.Part dst = p.parts[i];
         dst.copy(src);
         double lx = src.x - p.c0x;
         double ly = src.y - p.c0y;
         double lz = src.z - p.c0z;
         dst.x = c[0] + r[0] * lx + r[1] * ly + r[2] * lz;
         dst.y = c[1] + r[3] * lx + r[4] * ly + r[5] * lz;
         dst.z = c[2] + r[6] * lx + r[7] * ly + r[8] * lz;

         for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
               dst.r[row * 3 + col] = r[row * 3] * src.r[col] + r[row * 3 + 1] * src.r[3 + col] + r[row * 3 + 2] * src.r[6 + col];
            }
         }

         turnPlane(p.n1x, p.n1y, p.n1z, p.d1, p, c, r, dst, false);
         if (p.clip2) {
            turnPlane(p.n2x, p.n2y, p.n2z, p.d2, p, c, r, dst, true);
         }
      }
   }

   private static void turnPlane(double nx, double ny, double nz, double d, SliceStyle.Piece p, double[] c, double[] r, BodyProxy.Part dst, boolean second) {
      double px = nx * d - p.c0x;
      double py = ny * d - p.c0y;
      double pz = nz * d - p.c0z;
      double qx = c[0] + r[0] * px + r[1] * py + r[2] * pz;
      double qy = c[1] + r[3] * px + r[4] * py + r[5] * pz;
      double qz = c[2] + r[6] * px + r[7] * py + r[8] * pz;
      double mx = r[0] * nx + r[1] * ny + r[2] * nz;
      double my = r[3] * nx + r[4] * ny + r[5] * nz;
      double mz = r[6] * nx + r[7] * ny + r[8] * nz;
      if (second) {
         dst.c2x = mx;
         dst.c2y = my;
         dst.c2z = mz;
         dst.c2d = mx * qx + my * qy + mz * qz;
      } else {
         dst.cnx = mx;
         dst.cny = my;
         dst.cnz = mz;
         dst.cd = mx * qx + my * qy + mz * qz;
      }
   }

   private void drawSlashes(KillFx fx, FxFrame f, SliceStyle.State s) {
      int n = this.slashes.getInt() + (fx.crit ? 1 : 0);
      int offset = 0;

      for (int k = 0; k < n; k++) {
         float start = k * 0.06F;
         float p = Easing.clamp01((fx.t - start) / 0.34F);
         if (!(p <= 0.0F) && !(p >= 1.0F)) {
            double ux = s.swx;
            double uy = s.swy;
            double uz = s.swz;
            if (fx.crit && k == n - 1) {
               double dx = fx.ctx.dirX;
               double dy = fx.ctx.dirY;
               double dz = fx.ctx.dirZ;
               ux = s.n2y * dz - s.n2z * dy;
               uy = s.n2z * dx - s.n2x * dz;
               uz = s.n2x * dy - s.n2y * dx;
               double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
               ux /= ul;
               uy /= ul;
               uz /= ul;
            }

            double vx = uy * fx.ctx.dirZ - uz * fx.ctx.dirY;
            double vy = uz * fx.ctx.dirX - ux * fx.ctx.dirZ;
            double vz = ux * fx.ctx.dirY - uy * fx.ctx.dirX;
            double vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
            double radius = 1.0 * fx.size * (1.0 - 0.12 * k);
            double shift = (k - (n - 1) * 0.5) * 0.12;
            double var49;
            double ccx = s.px - fx.ctx.dirX * 0.3 - (var49 = vx / vl) * radius * 0.82 + var49 * shift;
            double var51;
            double ccy = s.py - fx.ctx.dirY * 0.3 - (var51 = vy / vl) * radius * 0.82 + var51 * shift;
            double var53;
            double ccz = s.pz - fx.ctx.dirZ * 0.3 - (var53 = vz / vl) * radius * 0.82 + var53 * shift;
            float head = Easing.outCubic(Easing.clamp01(p / 0.45F));
            float a = fx.alpha() * (1.0F - Easing.smoothstep(0.5F, 1.0F, p));
            int pts = Math.max(2, Math.round(36.0F * head));

            for (int i = 0; i < pts; i++) {
               float t = head * i / (pts - 1);
               double th = -SPAN / 2.0 + SPAN * t;
               double co = Math.cos(th);
               double si = Math.sin(th);
               Vfx.X[offset + i] = f.rx(ccx + (var49 * co + ux * si) * radius);
               Vfx.Y[offset + i] = f.ry(ccy + (var51 * co + uy * si) * radius);
               Vfx.Z[offset + i] = f.rz(ccz + (var53 * co + uz * si) * radius);
               float taper = (float)Math.pow(Math.sin(Math.PI * t), 0.9);
               Vfx.W[offset + i] = (float)(0.2 * fx.size * taper * (1.0F - 0.7F * Easing.smoothstep(0.3F, 1.0F, p)));
               Vfx.C[offset + i] = alpha(this.color.getColor(1.0F - t), a * (0.3F + 0.7F * taper));
            }

            Neon.add(offset, pts, false);
            offset += pts;
         }
      }

      Neon.flush();
   }

   private static final class Piece {
      final BodyProxy.Part[] parts = BodyProxy.newParts();
      final BodyProxy.Part[] start = BodyProxy.newParts();
      final RigidBox box = new RigidBox();
      final double[] r0 = new double[9];
      double c0x;
      double c0y;
      double c0z;
      boolean clip2;
      double n1x;
      double n1y;
      double n1z;
      double d1;
      double n2x;
      double n2y;
      double n2z;
      double d2;
      float delay;
      double vx;
      double vy;
      double vz;
      double wx;
      double wy;
      double wz;
      boolean moving;
      boolean empty;

      private Piece() {
      }
   }

   private static final class State {
      final SliceStyle.Piece[] pieces = new SliceStyle.Piece[3];
      int count;
      final PhysicsClock clock = new PhysicsClock();
      double px;
      double py;
      double pz;
      double n1x;
      double n1y;
      double n1z;
      double n2x;
      double n2y;
      double n2z;
      double swx;
      double swy;
      double swz;
      boolean sparked;

      private State() {
      }
   }
}
