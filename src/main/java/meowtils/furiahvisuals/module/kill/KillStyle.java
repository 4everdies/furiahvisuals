package meowtils.furiahvisuals.module.kill;

import java.util.Locale;
import java.util.function.Function;
import meowtils.furiahvisuals.module.kill.styles.BeamStyle;
import meowtils.furiahvisuals.module.kill.styles.BlackHoleStyle;
import meowtils.furiahvisuals.module.kill.styles.ChainedStyle;
import meowtils.furiahvisuals.module.kill.styles.ConsumedStyle;
import meowtils.furiahvisuals.module.kill.styles.DecapitationStyle;
import meowtils.furiahvisuals.module.kill.styles.DerezzStyle;
import meowtils.furiahvisuals.module.kill.styles.DisintegrateStyle;
import meowtils.furiahvisuals.module.kill.styles.ErasedStyle;
import meowtils.furiahvisuals.module.kill.styles.FateCardStyle;
import meowtils.furiahvisuals.module.kill.styles.FireworkStyle;
import meowtils.furiahvisuals.module.kill.styles.FoldAwayStyle;
import meowtils.furiahvisuals.module.kill.styles.InkWashStyle;
import meowtils.furiahvisuals.module.kill.styles.KnockoutStyle;
import meowtils.furiahvisuals.module.kill.styles.MeltdownStyle;
import meowtils.furiahvisuals.module.kill.styles.OvergrowthStyle;
import meowtils.furiahvisuals.module.kill.styles.RagdollStyle;
import meowtils.furiahvisuals.module.kill.styles.SanctifyStyle;
import meowtils.furiahvisuals.module.kill.styles.ShatterStyle;
import meowtils.furiahvisuals.module.kill.styles.ShockwaveStyle;
import meowtils.furiahvisuals.module.kill.styles.SignalLostStyle;
import meowtils.furiahvisuals.module.kill.styles.SliceStyle;
import meowtils.furiahvisuals.module.kill.styles.SoulAscendStyle;
import meowtils.furiahvisuals.module.kill.styles.TombstoneStyle;
import meowtils.furiahvisuals.module.kill.styles.WindDownStyle;
import meowtils.furiahvisuals.setting.Displayable;

public enum KillStyle implements Displayable {
   RAGDOLL("Ragdoll", "Goes limp and flies with the killing blow", 2.4F, RagdollStyle::new),
   DECAPITATION("Decapitation", "The head pops off and rolls away", 2.6F, DecapitationStyle::new),
   BEAM("Beam", "Pillar of light, lightning or an orbital laser", 2.3F, BeamStyle::new),
   SHOCKWAVE("Shockwave", "A blast ring throws the body and the ground", 1.9F, ShockwaveStyle::new),
   DISINTEGRATE("Disintegrate", "Crumbles to dust from a burning edge", 3.0F, DisintegrateStyle::new),
   SLICE("Slice", "Blade slashes cut the body in two", 2.6F, SliceStyle::new),
   SHATTER("Shatter", "Freezes into ice, then bursts into shards", 2.5F, ShatterStyle::new),
   SOUL_ASCEND("Soul Ascend", "A glowing soul rises and fades away", 3.4F, SoulAscendStyle::new),
   BLACK_HOLE("Black Hole", "Swallowed by a tiny black hole", 2.8F, BlackHoleStyle::new),
   DEREZZ("Derezz", "Glitches, then breaks into floating voxels", 2.8F, DerezzStyle::new),
   FIREWORK("Firework", "Rockets up and bursts into a firework", 2.7F, FireworkStyle::new),
   TOMBSTONE("Tombstone", "Sinks away as a gravestone rises", 6.1F, TombstoneStyle::new),
   OVERGROWTH(
      "Overgrowth",
      "Brambles and roses wrap it, then wither away",
      2.4F,
      true,
      OvergrowthStyle::new,
      "thorns",
      "vines",
      "roses",
      "overgrown",
      "bramble",
      "wither",
      "gothic"
   ),
   INK_WASH("Ink Wash", "Repainted in ink, then diffuses away", 2.0F, true, InkWashStyle::new, "ink", "wash", "sumi-e", "brush", "painting", "japanese", "seal"),
   FOLD_AWAY("Fold Away", "Folds into a paper crane that flies off", 2.2F, true, FoldAwayStyle::new, "paper", "origami", "fold", "crane", "plane", "cute"),
   SANCTIFY(
      "Sanctify",
      "Becomes a stained-glass window, then light",
      2.4F,
      true,
      SanctifyStyle::new,
      "stained glass",
      "window",
      "cathedral",
      "saint",
      "holy",
      "light",
      "gothic"
   ),
   SIGNAL_LOST(
      "Signal Lost",
      "Glitches out, then switches off like an old TV",
      1.4F,
      true,
      SignalLostStyle::new,
      "glitch",
      "crt",
      "signal",
      "error",
      "rgb",
      "digital",
      "cyber"
   ),
   MELTDOWN("Meltdown", "Melts into a chrome puddle, then a sphere", 2.2F, true, MeltdownStyle::new, "chrome", "liquid", "metal", "melt", "mercury", "y2k"),
   CONSUMED(
      "Consumed", "Charred by black smoke, then crumbles away", 2.0F, true, ConsumedStyle::new, "shadow", "smoke", "dark", "ash", "embers", "char", "consumed"
   ),
   WIND_DOWN(
      "Wind Down",
      "A brass automaton winds down and falls apart",
      2.2F,
      true,
      WindDownStyle::new,
      "clockwork",
      "brass",
      "automaton",
      "gears",
      "wind-up",
      "steampunk"
   ),
   FATE_CARD(
      "Fate Card", "Drawn into a tarot card that burns away", 2.0F, true, FateCardStyle::new, "tarot", "card", "fate", "occult", "mystic", "arcana", "portrait"
   ),
   ERASED("Erased", "Sketched in pencil, then rubbed out", 1.8F, true, ErasedStyle::new, "doodle", "sketch", "pencil", "eraser", "erased", "drawing", "cartoon"),
   CHAINED("Chained", "Golden chains drag the body under", 2.2F, true, ChainedStyle::new, "chains", "chained", "gold", "bound", "shackles", "drag"),
   KNOCKOUT(
      "Knockout",
      "K.O.! Launched into the sky like a cartoon",
      1.8F,
      true,
      KnockoutStyle::new,
      "comic",
      "ko",
      "knockout",
      "cartoon",
      "pop art",
      "halftone",
      "launch"
   );

   private final String displayName;
   private final String description;
   private final float seconds;
   private final Function<KillEffectsModule, KillStyleBase> factory;
   private final boolean isNew;
   private final String[] keywords;

   private KillStyle(String displayName, String description, float seconds, Function<KillEffectsModule, KillStyleBase> factory) {
      this(displayName, description, seconds, false, factory);
   }

   private KillStyle(
      String displayName, String description, float seconds, boolean isNew, Function<KillEffectsModule, KillStyleBase> factory, String... keywords
   ) {
      this.displayName = displayName;
      this.description = description;
      this.seconds = seconds;
      this.isNew = isNew;
      this.factory = factory;
      this.keywords = keywords;
   }

   @Override
   public String[] keywords() {
      return this.keywords.length == 0 ? null : (String[])this.keywords.clone();
   }

   @Override
   public boolean isNew() {
      return this.isNew;
   }

   @Override
   public String displayName() {
      return this.displayName;
   }

   @Override
   public String description() {
      return this.description;
   }

   @Override
   public String tag() {
      return String.format(Locale.ROOT, this.seconds < 10.0F ? "%.1f s" : "%.0f s", this.seconds);
   }

   KillStyleBase create(KillEffectsModule module) {
      return this.factory.apply(module);
   }
}
