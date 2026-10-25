package meowtils.furiahvisuals.gui.search;

import net.minecraft.client.gui.GuiScreen;

public final class TextEdit {
   private final int max;
   private String text = "";
   private int caret;
   private int anchor;
   public static final int IGNORED = 0;
   public static final int MOVED = 1;
   public static final int CHANGED = 2;

   public TextEdit(int max) {
      this.max = max;
   }

   public String text() {
      return this.text;
   }

   public int caret() {
      return this.caret;
   }

   public int selectionStart() {
      return Math.min(this.caret, this.anchor);
   }

   public int selectionEnd() {
      return Math.max(this.caret, this.anchor);
   }

   public boolean hasSelection() {
      return this.caret != this.anchor;
   }

   public void set(String s) {
      this.text = clean(s == null ? "" : s, this.max);
      this.caret = this.anchor = this.text.length();
   }

   public void selectAll() {
      this.anchor = 0;
      this.caret = this.text.length();
   }

   public void moveTo(int index, boolean extend) {
      this.caret = Math.max(0, Math.min(this.text.length(), index));
      if (!extend) {
         this.anchor = this.caret;
      }
   }

   public void selectWord(int index) {
      int i = Math.max(0, Math.min(this.text.length(), index));
      this.anchor = this.start(i);
      this.caret = this.end(i);
   }

   private int start(int i) {
      int s = Math.min(i, this.text.length());

      while (s > 0 && this.isWordChar(s - 1)) {
         s--;
      }

      return s;
   }

   private int end(int i) {
      int e = Math.max(0, i);

      while (e < this.text.length() && this.isWordChar(e)) {
         e++;
      }

      return e;
   }

   public void insert(String s) {
      String add = clean(s, this.max);
      int a = this.selectionStart();
      int b = this.selectionEnd();
      int room = this.max - (this.text.length() - (b - a));
      if (room > 0) {
         if (add.length() > room) {
            add = add.substring(0, room);
         }

         this.text = this.text.substring(0, a) + add + this.text.substring(b);
         this.caret = this.anchor = a + add.length();
      }
   }

   private void deleteRange(int a, int b) {
      if (b > a) {
         this.text = this.text.substring(0, a) + this.text.substring(b);
      }

      this.caret = this.anchor = a;
   }

   public int key(char c, int key) {
      boolean ctrl = GuiScreen.isCtrlKeyDown();
      boolean shift = GuiScreen.isShiftKeyDown();
      if (GuiScreen.isKeyComboCtrlA(key)) {
         this.selectAll();
         return 1;
      } else if (GuiScreen.isKeyComboCtrlC(key)) {
         if (this.hasSelection()) {
            GuiScreen.setClipboardString(this.text.substring(this.selectionStart(), this.selectionEnd()));
         }

         return 1;
      } else if (GuiScreen.isKeyComboCtrlX(key)) {
         if (this.hasSelection()) {
            GuiScreen.setClipboardString(this.text.substring(this.selectionStart(), this.selectionEnd()));
            this.deleteRange(this.selectionStart(), this.selectionEnd());
            return 2;
         } else {
            return 1;
         }
      } else if (GuiScreen.isKeyComboCtrlV(key)) {
         String before = this.text;
         this.insert(GuiScreen.getClipboardString());
         return before.equals(this.text) ? 1 : 2;
      } else {
         switch (key) {
            case 14:
               if (this.hasSelection()) {
                  this.deleteRange(this.selectionStart(), this.selectionEnd());
               } else {
                  if (this.caret <= 0) {
                     return 1;
                  }

                  this.deleteRange(ctrl ? this.wordLeft(this.caret) : this.caret - 1, this.caret);
               }

               return 2;
            case 199:
               this.moveTo(0, shift);
               return 1;
            case 203:
               if (this.hasSelection() && !shift) {
                  this.moveTo(this.selectionStart(), false);
               } else {
                  this.moveTo(ctrl ? this.wordLeft(this.caret) : this.caret - 1, shift);
               }

               return 1;
            case 205:
               if (this.hasSelection() && !shift) {
                  this.moveTo(this.selectionEnd(), false);
               } else {
                  this.moveTo(ctrl ? this.wordRight(this.caret) : this.caret + 1, shift);
               }

               return 1;
            case 207:
               this.moveTo(this.text.length(), shift);
               return 1;
            case 211:
               if (this.hasSelection()) {
                  this.deleteRange(this.selectionStart(), this.selectionEnd());
               } else {
                  if (this.caret >= this.text.length()) {
                     return 1;
                  }

                  this.deleteRange(this.caret, ctrl ? this.wordRight(this.caret) : this.caret + 1);
               }

               return 2;
            default:
               if (c >= ' ' && c != 127 && !ctrl) {
                  String before = this.text;
                  this.insert(String.valueOf(c));
                  return before.equals(this.text) ? 1 : 2;
               } else {
                  return 0;
               }
         }
      }
   }

   private boolean isWordChar(int i) {
      return i >= 0 && i < this.text.length() && Character.isLetterOrDigit(this.text.charAt(i));
   }

   private int wordLeft(int i) {
      int p = Math.max(0, Math.min(this.text.length(), i));

      while (p > 0 && !this.isWordChar(p - 1)) {
         p--;
      }

      while (p > 0 && this.isWordChar(p - 1)) {
         p--;
      }

      return p;
   }

   private int wordRight(int i) {
      int p = Math.max(0, Math.min(this.text.length(), i));

      while (p < this.text.length() && !this.isWordChar(p)) {
         p++;
      }

      while (p < this.text.length() && this.isWordChar(p)) {
         p++;
      }

      return p;
   }

   static String clean(String s, int max) {
      StringBuilder b = new StringBuilder(Math.min(s.length(), max));

      for (int i = 0; i < s.length() && b.length() < max; i++) {
         char c = s.charAt(i);
         if (c == '\n' || c == '\r' || c == '\t') {
            c = ' ';
         }

         if (c >= ' ' && c != 127 && c != 167) {
            b.append(c);
         }
      }

      return b.toString();
   }
}
