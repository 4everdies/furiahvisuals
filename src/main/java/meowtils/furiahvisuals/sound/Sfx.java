package meowtils.furiahvisuals.sound;

public enum Sfx {
   GUI_OPEN("gui_open"),
   GUI_CLOSE("gui_close"),
   SECTION("section"),
   CLICK("click"),
   TOGGLE_ON("toggle_on"),
   TOGGLE_OFF("toggle_off"),
   SELECT("select"),
   SLIDER_TICK("slider_tick"),
   SLIDER_RELEASE("slider_release"),
   DROPDOWN("dropdown"),
   SUCCESS("success"),
   ERROR("error"),
   CONFIRM("confirm"),
   EMPTY("empty"),
   HOVER("hover"),
   KEY("key"),
   KEY_UP("key_up", true),
   NOTIFY("notify"),
   DISCORD_DM("discord_dm"),
   DISCORD_MENTION("discord_mention"),
   VOICE_JOIN("voice_join"),
   VOICE_LEAVE("voice_leave"),
   KILL("kill"),
   WHOOSH("whoosh");

   private final String file;
   private final boolean optional;

   private Sfx(String file) {
      this(file, false);
   }

   private Sfx(String file, boolean optional) {
      this.file = file;
      this.optional = optional;
   }

   public String file() {
      return this.file;
   }

   public boolean isOptional() {
      return this.optional;
   }
}
