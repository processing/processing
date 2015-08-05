/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeInputHandler - PDE-specific handling of keys
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-14 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
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
  along with this program; if not, write to the Free Software Foundation, Inc.
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.app.syntax;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import processing.app.Base;
import processing.app.Preferences;


/**
 * Sets key bindings used by the PDE, except for those that are Mode-specific.
 * Not part of the original jeditsyntax DefaultInputHandler because it makes
 * use of Preferences and other PDE classes.
 */
public class PdeInputHandler extends DefaultInputHandler {

  public PdeInputHandler() {
    // Use option on mac for text edit controls that are ctrl on Windows/Linux.
    // (i.e. ctrl-left/right is option-left/right on OS X)
    String mod = Base.isMacOS() ? "A" : "C";

    // right now, ctrl-up/down is select up/down, but mod should be
    // used instead, because the mac expects it to be option(alt)

    addKeyBinding("BACK_SPACE", InputHandler.BACKSPACE);
    // for 0122, shift-backspace is delete, for 0176, it's now a preference,
    // to prevent holy warriors from attacking me for it.
    if (Preferences.getBoolean("editor.keys.shift_backspace_is_delete")) {
      addKeyBinding("S+BACK_SPACE", InputHandler.DELETE);
    } else {
      // Made the default for 0215, deemed better for our audience.
      addKeyBinding("S+BACK_SPACE", InputHandler.BACKSPACE);
    }

    addKeyBinding("DELETE", InputHandler.DELETE);
    addKeyBinding("S+DELETE", InputHandler.DELETE);

    // the following two were changed for 0122 for better mac/pc compatability
    addKeyBinding(mod + "+BACK_SPACE", InputHandler.BACKSPACE_WORD);  // 0122
    addKeyBinding(mod + "S+BACK_SPACE", InputHandler.BACKSPACE_WORD);  // 0215
    addKeyBinding(mod + "+DELETE", InputHandler.DELETE_WORD);  // 0122
    addKeyBinding(mod + "S+DELETE", InputHandler.DELETE_WORD);  // 0215

    // handled by listener, don't bother here
    //addKeyBinding("ENTER", InputHandler.INSERT_BREAK);
    //addKeyBinding("TAB", InputHandler.INSERT_TAB);

    addKeyBinding("INSERT", InputHandler.OVERWRITE);

    // http://dev.processing.org/bugs/show_bug.cgi?id=162
    // added for 0176, though the bindings do not appear relevant for osx
    if (Preferences.getBoolean("editor.keys.alternative_cut_copy_paste")) {
      addKeyBinding("C+INSERT", InputHandler.CLIPBOARD_COPY);
      addKeyBinding("S+INSERT", InputHandler.CLIPBOARD_PASTE);
      addKeyBinding("S+DELETE", InputHandler.CLIPBOARD_CUT);
    }

    // disabling for 0122, not sure what this does
    //addKeyBinding("C+\\", InputHandler.TOGGLE_RECT);

    // for 0122, these have been changed for better compatibility
    // HOME and END now mean the beginning/end of the document
    // for 0176 changed this to a preference so that the Mac OS X people
    // can get the "normal" behavior as well if they prefer.
    if (Preferences.getBoolean("editor.keys.home_and_end_travel_far")) {
      addKeyBinding("HOME", InputHandler.DOCUMENT_HOME);
      addKeyBinding("END", InputHandler.DOCUMENT_END);
      addKeyBinding("S+HOME", InputHandler.SELECT_DOC_HOME);
      addKeyBinding("S+END", InputHandler.SELECT_DOC_END);
    } else {
      // for 0123 added the proper windows defaults
      addKeyBinding("HOME", InputHandler.HOME);
      addKeyBinding("END", InputHandler.END);
      addKeyBinding("S+HOME", InputHandler.SELECT_HOME);
      addKeyBinding("S+END", InputHandler.SELECT_END);
      addKeyBinding("C+HOME", InputHandler.DOCUMENT_HOME);
      addKeyBinding("C+END", InputHandler.DOCUMENT_END);
      addKeyBinding("CS+HOME", InputHandler.SELECT_DOC_HOME);
      addKeyBinding("CS+END", InputHandler.SELECT_DOC_END);
    }

    if (Base.isMacOS()) {
      // Additional OS X key bindings added for 0215.
      // Also note that two more are added above and marked 0215.
      // http://code.google.com/p/processing/issues/detail?id=1354
      // Could not find a proper Apple guide, but a partial reference is here:
      // http://guides.macrumors.com/Keyboard_shortcuts&section=10#Text_Shortcuts

      // control-A  move to start of current paragraph
      addKeyBinding("C+A", InputHandler.HOME);
      addKeyBinding("CS+A", InputHandler.SELECT_HOME);
      // control-E  move to end of current paragraph
      addKeyBinding("C+E", InputHandler.END);
      addKeyBinding("CS+E", InputHandler.SELECT_END);

      // control-D  forward delete
      addKeyBinding("C+D", InputHandler.DELETE);

      // control-B  move left one character
      addKeyBinding("C+B", InputHandler.PREV_CHAR);
      addKeyBinding("CS+B", InputHandler.SELECT_PREV_CHAR);
      // control-F  move right one character
      addKeyBinding("C+F", InputHandler.NEXT_CHAR);
      addKeyBinding("CS+F", InputHandler.SELECT_NEXT_CHAR);

      // control-H  delete (just ASCII for backspace)
      addKeyBinding("C+H", InputHandler.BACKSPACE);

      // control-N  move down one line
      addKeyBinding("C+N", InputHandler.NEXT_LINE);
      addKeyBinding("CS+N", InputHandler.SELECT_NEXT_LINE);
      // control-P  move up one line
      addKeyBinding("C+P", InputHandler.PREV_LINE);
      addKeyBinding("CS+P", InputHandler.SELECT_PREV_LINE);

      // might be nice, but no handlers currently available
      // control-O  insert new line after cursor
      // control-T  transpose (swap) two surrounding character
      // control-V  move to end, then left one character
      // control-K  delete remainder of current paragraph
      // control-Y  paste text previously deleted with control-K
    }

    if (Base.isMacOS()) {
      addKeyBinding("M+LEFT", InputHandler.HOME);
      addKeyBinding("M+RIGHT", InputHandler.END);
      addKeyBinding("MS+LEFT", InputHandler.SELECT_HOME); // 0122
      addKeyBinding("MS+RIGHT", InputHandler.SELECT_END);  // 0122
    } else {
      addKeyBinding("C+LEFT", InputHandler.HOME);  // 0122
      addKeyBinding("C+RIGHT", InputHandler.END);  // 0122
      addKeyBinding("CS+HOME", InputHandler.SELECT_HOME); // 0122
      addKeyBinding("CS+END", InputHandler.SELECT_END);  // 0122
    }

    addKeyBinding("PAGE_UP", InputHandler.PREV_PAGE);
    addKeyBinding("PAGE_DOWN", InputHandler.NEXT_PAGE);
    addKeyBinding("S+PAGE_UP", InputHandler.SELECT_PREV_PAGE);
    addKeyBinding("S+PAGE_DOWN", InputHandler.SELECT_NEXT_PAGE);

    addKeyBinding("LEFT", InputHandler.PREV_CHAR);
    addKeyBinding("S+LEFT", InputHandler.SELECT_PREV_CHAR);
    addKeyBinding(mod + "+LEFT", InputHandler.PREV_WORD);
    addKeyBinding(mod + "S+LEFT", InputHandler.SELECT_PREV_WORD);
    addKeyBinding("RIGHT", InputHandler.NEXT_CHAR);
    addKeyBinding("S+RIGHT", InputHandler.SELECT_NEXT_CHAR);
    addKeyBinding(mod + "+RIGHT", InputHandler.NEXT_WORD);
    addKeyBinding(mod + "S+RIGHT", InputHandler.SELECT_NEXT_WORD);

    addKeyBinding("UP", InputHandler.PREV_LINE);
    addKeyBinding(mod + "+UP", InputHandler.PREV_LINE);  // p5
    addKeyBinding("S+UP", InputHandler.SELECT_PREV_LINE);
    addKeyBinding("DOWN", InputHandler.NEXT_LINE);
    addKeyBinding(mod + "+DOWN", InputHandler.NEXT_LINE);  // p5
    addKeyBinding("S+DOWN", InputHandler.SELECT_NEXT_LINE);

    addKeyBinding("MS+UP", InputHandler.SELECT_DOC_HOME);
    addKeyBinding("CS+UP", InputHandler.SELECT_DOC_HOME);
    addKeyBinding("MS+DOWN", InputHandler.SELECT_DOC_END);
    addKeyBinding("CS+DOWN", InputHandler.SELECT_DOC_END);

    addKeyBinding(mod + "+ENTER", InputHandler.REPEAT);
  }


  protected boolean isMnemonic(KeyEvent event) {
    // Don't do this on OS X, because alt (the option key) is used for
    // non-ASCII chars, and there are no menu mnemonics to speak of
    if (!Base.isMacOS()) {
      if ((event.getModifiers() & InputEvent.ALT_MASK) != 0 &&
          (event.getModifiers() & InputEvent.CTRL_MASK) == 0 &&
          event.getKeyChar() != KeyEvent.VK_UNDEFINED) {
        // This is probably a menu mnemonic, don't pass it through.
        // If it's an alt-NNNN sequence, those only work on the keypad
        // and pass through UNDEFINED as the keyChar.
        return true;
      }
    }
    return false;
  }


  public void keyPressed(KeyEvent event) {
    // don't pass the ctrl-, through to the editor
    // https://github.com/processing/processing/issues/3074
    if ((event.getModifiers() & InputEvent.CTRL_MASK) != 0 &&
        event.getKeyChar() == ',') {
      return;
    }
    // don't pass menu mnemonics (alt-f for file, etc) to the editor
    if (isMnemonic(event)) {
      return;
    }

    if (!handlePressed(event)) {
      super.keyPressed(event);
    }
  }


  public void keyTyped(KeyEvent event) {
    if (isMnemonic(event)) {
      return;
    }

    if (!handleTyped(event)) {
      super.keyTyped(event);
    }
  }


  // we don't need keyReleased(), so that's passed through automatically


  /**
   * Override this function in your InputHandler to do any gymnastics.
   * @return true if key has been handled (no further handling should be done)
   */
  public boolean handlePressed(KeyEvent event) {
    return false;
  }


  /**
   * Override this instead of keyPressed/keyTyped
   * @return true if key has been handled (no further handling should be done)
   */
  public boolean handleTyped(KeyEvent event) {
    return false;
  }
}