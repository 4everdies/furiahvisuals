package meowtils.furiahvisuals.module.watermark;

import java.util.Locale;
import meowtils.furiahvisuals.setting.BooleanSetting;
import meowtils.furiahvisuals.setting.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;

final class XyzItem extends IslandItem {
   private static final String[] FACING = new String[]{"S", "W", "N", "E"};
   final SliderSetting decimals;
   final BooleanSetting letters;
   final BooleanSetting facing;
   private final RollingText x = new RollingText();
   private final RollingText y = new RollingText();
   private final RollingText z = new RollingText();
   private double lx = Double.NaN;
   private double ly = Double.NaN;
   private double lz = Double.NaN;
   private int face = -1;
   private float letterW;
   private float spaceW;
   private float faceW;
   int lastLetterColor;
   private int measuredPx;

   XyzItem(WaterMarkModule wm) {
      super(wm, "xyz", "XYZ", "Your coordinates.", IslandIcons.Icon.COMPASS);
      this.icons(null, new IslandIcons.Icon[]{IslandIcons.Icon.COMPASS, IslandIcons.Icon.PIN});
      this.letters = this.option(new BooleanSetting("xyz.letters", "Axis Letters", "Writes x, y and z before the numbers.", true));
      this.decimals = this.option(new SliderSetting("xyz.decimals", "Decimals", "Digits after the point.", 0.0, 1.0, 0.0, 1.0));
      this.facing = this.option(new BooleanSetting("xyz.facing", "Show Facing", "The direction you look at: N, E, S or W.", false));
   }

   @Override
   void tick() {
      EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;
      if (p != null) {
         int dec = this.decimals.getInt();
         this.lx = this.set(this.x, p.posX, this.lx, dec);
         this.ly = this.set(this.y, p.posY, this.ly, dec);
         this.lz = this.set(this.z, p.posZ, this.lz, dec);
         this.face = MathHelper.floor_double(p.rotationYaw * 4.0 / 360.0 + 0.5) & 3;
      }
   }

   private double set(RollingText t, double v, double last, int dec) {
      double shown = dec == 0 ? Math.floor(v) : Math.floor(v * 10.0) / 10.0;
      if (shown != last || t.get().isEmpty()) {
         t.set(dec == 0 ? Integer.toString((int)shown) : String.format(Locale.ROOT, "%.1f", shown), shown, this.wm.rolling());
      }

      return shown;
   }

   @Override
   void frame(float dt, IslandDraw d, IslandStyle s) {
      this.x.update(dt);
      this.y.update(dt);
      this.z.update(dt);
   }

   @Override
   String panelValue() {
      return this.x.get().isEmpty() ? "In a world" : this.x.get() + " " + this.y.get() + " " + this.z.get();
   }

   @Override
   String panelDescription() {
      return "Coordinates";
   }

   private void metrics(IslandDraw d, IslandStyle s) {
      int px = d.pixels(s.font);
      if (px != this.measuredPx) {
         this.measuredPx = px;
         this.letterW = Math.max(d.charWidth(false, s.font, 'x'), Math.max(d.charWidth(false, s.font, 'y'), d.charWidth(false, s.font, 'z')));
         this.spaceW = d.charWidth(false, s.font, ' ') * 1.6F;
         this.faceW = d.charWidth(true, s.font, 'W');
      }
   }

   private float letterSpace() {
      return this.letters.get() ? this.letterW + this.spaceW * 0.7F : 0.0F;
   }

   @Override
   float measure(IslandDraw d, IslandStyle s) {
      this.metrics(d, s);
      float w = this.iconSpace(s)
         + 3.0F * this.letterSpace()
         + this.x.width(d, false, s.font)
         + this.y.width(d, false, s.font)
         + this.z.width(d, false, s.font)
         + 2.0F * this.spaceW;
      if (this.facing.get()) {
         w += this.spaceW + this.faceW;
      }

      return w;
   }

   @Override
   void draw(IslandDraw d, IslandStyle s, float px, float cy) {
      this.metrics(d, s);
      px = this.drawIcon(d, s, px, cy);
      float top = d.middleTop(false, s.font, cy);
      px = this.axis(d, s, 'x', this.x, px, top, cy);
      px += this.spaceW;
      px = this.axis(d, s, 'y', this.y, px, top, cy);
      px += this.spaceW;
      px = this.axis(d, s, 'z', this.z, px, top, cy);
      if (this.facing.get() && this.face >= 0) {
         float var12;
         d.text(true, s.font, FACING[this.face], var12 = px + this.spaceW, d.middleTop(true, s.font, cy), s.accentAt(var12 + this.faceW / 2.0F));
      }
   }

   private float axis(IslandDraw d, IslandStyle s, char letter, RollingText value, float px, float top, float cy) {
      if (this.letters.get()) {
         this.lastLetterColor = s.accentAt(px + this.letterW / 2.0F);
         d.glyph(false, s.font, letter, px, top, this.lastLetterColor);
         px += this.letterSpace();
      }

      value.draw(d, false, s.font, px, cy, s.text);
      return px + value.width(d, false, s.font);
   }

   @Override
   boolean hit(float mx, float my, float px, float cy, IslandStyle s, IslandItem.Hit hit) {
      hit.set(this, px - s.pad, cy - s.height / 2.0F, this.measure(this.wm.draw(), s) + s.pad * 2.0F, s.height, 0, "Your coordinates", false);
      return true;
   }
}
