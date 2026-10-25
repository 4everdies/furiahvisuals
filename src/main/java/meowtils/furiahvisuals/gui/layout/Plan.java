package meowtils.furiahvisuals.gui.layout;

import java.util.ArrayList;
import java.util.List;
import meowtils.furiahvisuals.setting.ColorSetting;
import meowtils.furiahvisuals.setting.Setting;

final class Plan {
   final Plan.PlanPage main = new Plan.PlanPage(this, "", null, null);
   final List<Plan.PlanPage> subs = new ArrayList<>();
   Plan.PlanPage stylePage;
   final List<String[]> labels = new ArrayList<>();
   final List<String[]> data = new ArrayList<>();
   final List<Plan.Pattern> notSwitches = new ArrayList<>();
   final List<String[]> options = new ArrayList<>();
   boolean headerless;
   int sequence;

   Plan headerless() {
      this.headerless = true;
      return this;
   }

   Plan notSwitch(String pattern) {
      this.notSwitches.add(new Plan.Pattern(pattern));
      return this;
   }

   Plan option(String optionPattern, String switchId) {
      this.options.add(new String[]{optionPattern, switchId});
      return this;
   }

   Plan.PlanPage sub(String key, String title, String description) {
      Plan.PlanPage p = new Plan.PlanPage(this, key, title, description);
      this.subs.add(p);
      return p;
   }

   Plan.PlanPage sub(String key, String title, String description, Plan.PlanPage tab) {
      Plan.PlanPage p = this.sub(key, title, description);
      p.parent = tab;
      return p;
   }

   Plan.PlanPage tab(String key, String title, String description) {
      Plan.PlanPage p = this.sub(key, title, description);
      p.tab = true;
      return p;
   }

   Plan.PlanPage stylePages() {
      this.stylePage = new Plan.PlanPage(this, "style", null, null);
      return this.stylePage;
   }

   Plan label(String pattern, String label) {
      this.labels.add(new String[]{pattern, label});
      return this;
   }

   Plan data(String pattern, String reason) {
      this.data.add(new String[]{pattern, reason});
      return this;
   }

   static String knob(Setting s) {
      String id = s.getId();
      int dot = id.indexOf(46);
      return dot < 0 ? id : id.substring(dot + 1);
   }

   static final class Pattern {
      final String spec;
      final int seq;
      final Plan.PlanGroup target;
      final Plan.Pattern.Kind kind;
      final String a;
      final String group;

      Pattern(String spec, int seq, Plan.PlanGroup target) {
         this.spec = spec;
         this.seq = seq;
         this.target = target;
         String b = null;
         if (spec.startsWith(">")) {
            this.kind = Plan.Pattern.Kind.LINK;
            this.a = spec.substring(1);
         } else if (spec.equals("*")) {
            this.kind = Plan.Pattern.Kind.REST_OF_STYLES;
            this.a = null;
         } else if (spec.equals("**")) {
            this.kind = Plan.Pattern.Kind.REST;
            this.a = null;
         } else if (spec.equals("*:color")) {
            this.kind = Plan.Pattern.Kind.STYLE_COLORS;
            this.a = null;
         } else if (spec.startsWith("@")) {
            this.kind = Plan.Pattern.Kind.DECLARED_GROUP;
            this.a = spec.substring(1);
         } else if (spec.startsWith("*.")) {
            this.kind = Plan.Pattern.Kind.KNOB;
            int at = spec.indexOf(64);
            this.a = at < 0 ? spec.substring(2) : spec.substring(2, at);
            b = at < 0 ? null : spec.substring(at + 1);
         } else if (spec.endsWith(".*")) {
            this.kind = Plan.Pattern.Kind.PREFIX;
            this.a = spec.substring(0, spec.length() - 1);
         } else {
            this.kind = Plan.Pattern.Kind.ID;
            this.a = spec;
         }

         this.group = b;
      }

      Pattern(String spec) {
         this(spec, 0, null);
      }

      boolean catchAll() {
         return this.kind == Plan.Pattern.Kind.REST_OF_STYLES || this.kind == Plan.Pattern.Kind.REST;
      }

      boolean byId() {
         return this.kind == Plan.Pattern.Kind.ID || this.kind == Plan.Pattern.Kind.PREFIX;
      }

      boolean matches(Setting s) {
         switch (this.kind) {
            case REST_OF_STYLES:
               return s.getOwner() != null;
            case REST:
               return true;
            case STYLE_COLORS:
               return s.getOwner() != null && s instanceof ColorSetting;
            case DECLARED_GROUP:
               return this.a.equals(s.getGroup());
            case KNOB:
               return s.getOwner() != null && this.a.equals(Plan.knob(s)) && (this.group == null || this.group.equals(s.getGroup()));
            case PREFIX:
               return s.getId().startsWith(this.a);
            case ID:
               return s.getId().equals(this.a);
            default:
               return false;
         }
      }

      @Override
      public String toString() {
         return this.spec;
      }

      static enum Kind {
         LINK,
         REST_OF_STYLES,
         REST,
         STYLE_COLORS,
         DECLARED_GROUP,
         KNOB,
         PREFIX,
         ID;
      }
   }

   static final class PlanGroup {
      final Plan.PlanPage page;
      final Slot slot;
      final String title;
      final List<Plan.Pattern> patterns = new ArrayList<>();

      PlanGroup(Plan.PlanPage page, Slot slot, String title) {
         this.page = page;
         this.slot = slot;
         this.title = title;
      }

      Plan.PlanGroup add(String... specs) {
         for (String spec : specs) {
            this.patterns.add(new Plan.Pattern(spec, this.page.plan.sequence++, this));
         }

         return this;
      }
   }

   static final class PlanPage {
      final Plan plan;
      final String key;
      final String title;
      final String description;
      final List<Plan.PlanGroup> groups = new ArrayList<>();
      boolean tab;
      Plan.PlanPage parent;
      boolean headerless;

      PlanPage(Plan plan, String key, String title, String description) {
         this.plan = plan;
         this.key = key;
         this.title = title;
         this.description = description;
      }

      Plan.PlanPage headerless() {
         this.headerless = true;
         return this;
      }

      Plan.PlanGroup main() {
         return this.group(Slot.MAIN, null);
      }

      Plan.PlanGroup colors() {
         return this.group(Slot.COLORS, null);
      }

      Plan.PlanGroup size() {
         return this.group(Slot.SIZE, null);
      }

      Plan.PlanGroup motion() {
         return this.group(Slot.MOTION, null);
      }

      Plan.PlanGroup when() {
         return this.group(Slot.WHEN, null);
      }

      Plan.PlanGroup options() {
         return this.group(Slot.OPTIONS, null);
      }

      Plan.PlanGroup feature(String title) {
         return this.group(Slot.FEATURE, title);
      }

      Plan.PlanGroup extras() {
         return this.group(Slot.EXTRAS, null);
      }

      Plan.PlanGroup advanced() {
         return this.group(Slot.ADVANCED, null);
      }

      private Plan.PlanGroup group(Slot slot, String title) {
         String t = title != null ? title : slot.title;

         for (Plan.PlanGroup g : this.groups) {
            if (g.slot == slot && g.title.equals(t)) {
               return g;
            }
         }

         Plan.PlanGroup gx = new Plan.PlanGroup(this, slot, t);
         this.groups.add(gx);
         return gx;
      }
   }
}
