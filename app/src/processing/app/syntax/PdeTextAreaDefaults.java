/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeTextAreaDefaults - grabs font/color settings for the editor
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-06 Ben Fry and Casey Reas
  Copyright (c) 2001-03 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.syntax;

import processing.app.*;


public class PdeTextAreaDefaults extends TextAreaDefaults {

  public PdeTextAreaDefaults(Mode theme) {
    inputHandler = new DefaultInputHandler();
    //inputHandler.addDefaultKeyBindings();  // 0122

    // Use option on mac for text edit controls that are ctrl on Windows/Linux.
    // (i.e. ctrl-left/right is option-left/right on OS X)
    String mod = Base.isMacOS() ? "A" : "C";

    // right now, ctrl-up/down is select up/down, but mod should be
    // used instead, because the mac expects it to be option(alt)

    inputHandler.addKeyBinding("BACK_SPACE", InputHandler.BACKSPACE);
    // for 0122, shift-backspace is delete, for 0176, it's now a preference,
    // to prevent holy warriors from attacking me for it.
    if (Preferences.getBoolean("editor.keys.shift_backspace_is_delete")) {
      inputHandler.addKeyBinding("S+BACK_SPACE", InputHandler.DELETE);
    } else {
      inputHandler.addKeyBinding("S+BACK_SPACE", InputHandler.BACKSPACE);
    }

    inputHandler.addKeyBinding("DELETE", InputHandler.DELETE);
    inputHandler.addKeyBinding("S+DELETE", InputHandler.DELETE);

    // the following two were changing for 0122 for better mac/pc compatability
    inputHandler.addKeyBinding(mod + "+BACK_SPACE", InputHandler.BACKSPACE_WORD);
    inputHandler.addKeyBinding(mod + "+DELETE", InputHandler.DELETE_WORD);

    // handled by listener, don't bother here
    //inputHandler.addKeyBinding("ENTER", InputHandler.INSERT_BREAK);
    //inputHandler.addKeyBinding("TAB", InputHandler.INSERT_TAB);

    inputHandler.addKeyBinding("INSERT", InputHandler.OVERWRITE);

    // http://dev.processing.org/bugs/show_bug.cgi?id=162
    // added for 0176, though the bindings do not appear relevant for osx
    if (Preferences.getBoolean("editor.keys.alternative_cut_copy_paste")) {
      inputHandler.addKeyBinding("C+INSERT", InputHandler.CLIPBOARD_COPY);
      inputHandler.addKeyBinding("S+INSERT", InputHandler.CLIPBOARD_PASTE);
      inputHandler.addKeyBinding("S+DELETE", InputHandler.CLIPBOARD_CUT);
    }

    // disabling for 0122, not sure what this does
    //inputHandler.addKeyBinding("C+\\", InputHandler.TOGGLE_RECT);

    // for 0122, these have been changed for better compatibility
    // HOME and END now mean the beginning/end of the document
    // for 0176 changed this to a preference so that the Mac OS X people
    // can get the "normal" behavior as well if they prefer.
    if (Preferences.getBoolean("editor.keys.home_and_end_travel_far")) {
      inputHandler.addKeyBinding("HOME", InputHandler.DOCUMENT_HOME);
      inputHandler.addKeyBinding("END", InputHandler.DOCUMENT_END);
      inputHandler.addKeyBinding("S+HOME", InputHandler.SELECT_DOC_HOME);
      inputHandler.addKeyBinding("S+END", InputHandler.SELECT_DOC_END);
    } else {
      // for 0123 added the proper windows defaults
      inputHandler.addKeyBinding("HOME", InputHandler.HOME);
      inputHandler.addKeyBinding("END", InputHandler.END);
      inputHandler.addKeyBinding("S+HOME", InputHandler.SELECT_HOME);
      inputHandler.addKeyBinding("S+END", InputHandler.SELECT_END);
      inputHandler.addKeyBinding("C+HOME", InputHandler.DOCUMENT_HOME);
      inputHandler.addKeyBinding("C+END", InputHandler.DOCUMENT_END);
      inputHandler.addKeyBinding("CS+HOME", InputHandler.SELECT_DOC_HOME);
      inputHandler.addKeyBinding("CS+END", InputHandler.SELECT_DOC_END);
    }

    if (Base.isMacOS()) {
      //inputHandler.addKeyBinding("C+A", InputHandler.HOME);
      //inputHandler.addKeyBinding("C+E", InputHandler.END);
      //inputHandler.addKeyBinding("C+D", InputHandler.DELETE);  // option-delete (fwd)
    }

    if (Base.isMacOS()) {
      inputHandler.addKeyBinding("M+LEFT", InputHandler.HOME);
      inputHandler.addKeyBinding("M+RIGHT", InputHandler.END);
      inputHandler.addKeyBinding("MS+LEFT", InputHandler.SELECT_HOME); // 0122
      inputHandler.addKeyBinding("MS+RIGHT", InputHandler.SELECT_END);  // 0122
    } else {
      inputHandler.addKeyBinding("C+LEFT", InputHandler.HOME);  // 0122
      inputHandler.addKeyBinding("C+RIGHT", InputHandler.END);  // 0122
      inputHandler.addKeyBinding("CS+HOME", InputHandler.SELECT_HOME); // 0122
      inputHandler.addKeyBinding("CS+END", InputHandler.SELECT_END);  // 0122
    }

    inputHandler.addKeyBinding("PAGE_UP", InputHandler.PREV_PAGE);
    inputHandler.addKeyBinding("PAGE_DOWN", InputHandler.NEXT_PAGE);
    inputHandler.addKeyBinding("S+PAGE_UP", InputHandler.SELECT_PREV_PAGE);
    inputHandler.addKeyBinding("S+PAGE_DOWN", InputHandler.SELECT_NEXT_PAGE);

    inputHandler.addKeyBinding("LEFT", InputHandler.PREV_CHAR);
    inputHandler.addKeyBinding("S+LEFT", InputHandler.SELECT_PREV_CHAR);
    inputHandler.addKeyBinding(mod + "+LEFT", InputHandler.PREV_WORD);
    inputHandler.addKeyBinding(mod + "S+LEFT", InputHandler.SELECT_PREV_WORD);
    inputHandler.addKeyBinding("RIGHT", InputHandler.NEXT_CHAR);
    inputHandler.addKeyBinding("S+RIGHT", InputHandler.SELECT_NEXT_CHAR);
    inputHandler.addKeyBinding(mod + "+RIGHT", InputHandler.NEXT_WORD);
    inputHandler.addKeyBinding(mod + "S+RIGHT", InputHandler.SELECT_NEXT_WORD);

    inputHandler.addKeyBinding("UP", InputHandler.PREV_LINE);
    inputHandler.addKeyBinding(mod + "+UP", InputHandler.PREV_LINE);  // p5
    inputHandler.addKeyBinding("S+UP", InputHandler.SELECT_PREV_LINE);
    inputHandler.addKeyBinding("DOWN", InputHandler.NEXT_LINE);
    inputHandler.addKeyBinding(mod + "+DOWN", InputHandler.NEXT_LINE);  // p5
    inputHandler.addKeyBinding("S+DOWN", InputHandler.SELECT_NEXT_LINE);

    inputHandler.addKeyBinding("MS+UP", InputHandler.SELECT_DOC_HOME);
    inputHandler.addKeyBinding("CS+UP", InputHandler.SELECT_DOC_HOME);
    inputHandler.addKeyBinding("MS+DOWN", InputHandler.SELECT_DOC_END);
    inputHandler.addKeyBinding("CS+DOWN", InputHandler.SELECT_DOC_END);

    inputHandler.addKeyBinding(mod + "+ENTER", InputHandler.REPEAT);

    document = new SyntaxDocument();
    editable = true;
    
    // Set to 0 for revision 0215 because it causes strange jumps
    // http://code.google.com/p/processing/issues/detail?id=1055
    electricScroll = 0;
    
    caretVisible = true;
    caretBlinks = Preferences.getBoolean("editor.caret.blink");
    blockCaret = Preferences.getBoolean("editor.caret.block");
    cols = 80;
    // Set the number of rows lower to avoid layout badness with large fonts
    // http://code.google.com/p/processing/issues/detail?id=1275
    rows = 5;

    font = Preferences.getFont("editor.font");
    antialias = Preferences.getBoolean("editor.antialias");

    styles = new SyntaxStyle[Token.ID_COUNT];

    // comments
    styles[Token.COMMENT1] = theme.getStyle("comment1");
    styles[Token.COMMENT2] = theme.getStyle("comment2");

    // abstract, final, private
    styles[Token.KEYWORD1] = theme.getStyle("keyword1");

    // beginShape, point, line
    styles[Token.KEYWORD2] = theme.getStyle("keyword2");

    // byte, char, short, color
    styles[Token.KEYWORD3] = theme.getStyle("keyword3");

    // constants: null, true, this, RGB, TWO_PI
    styles[Token.LITERAL1] = theme.getStyle("literal1");

    // p5 built in variables: mouseX, width, pixels
    styles[Token.LITERAL2] = theme.getStyle("literal2");

    // ??
    styles[Token.LABEL] = theme.getStyle("label");

    // + - = /
    styles[Token.OPERATOR] = theme.getStyle("operator");

    // area that's not in use by the text (replaced with tildes)
    styles[Token.INVALID] = theme.getStyle("invalid");

    fgcolor = theme.getColor("editor.fgcolor");
    bgcolor = theme.getColor("editor.bgcolor");

    caretColor = theme.getColor("editor.caret.color");
    selectionColor = theme.getColor("editor.selection.color");
    lineHighlight = theme.getBoolean("editor.linehighlight");
    lineHighlightColor = theme.getColor("editor.linehighlight.color");
    bracketHighlight = theme.getBoolean("editor.brackethighlight");
    bracketHighlightColor = theme.getColor("editor.brackethighlight.color");
    eolMarkers = theme.getBoolean("editor.eolmarkers");
    eolMarkerColor = theme.getColor("editor.eolmarkers.color");
  }
}
