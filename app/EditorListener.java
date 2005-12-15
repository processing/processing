/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-05 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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

package processing.app;

import processing.app.syntax.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;


/**
 * Filters key events for tab expansion/indent/etc.
 */
public class EditorListener {
  Editor editor;
  JEditTextArea textarea;

  boolean externalEditor;
  boolean expandTabs;
  int tabSize;
  String tabString;
  boolean autoIndent;

  int selectionStart, selectionEnd;
  int position;


  public EditorListener(Editor editor, JEditTextArea textarea) {
    this.editor = editor;
    this.textarea = textarea;

    // let him know that i'm leechin'
    textarea.editorListener = this;

    applyPreferences();
  }


  public void applyPreferences() {
    expandTabs = Preferences.getBoolean("editor.tabs.expand");
    tabSize = Preferences.getInteger("editor.tabs.size");
    tabString = Editor.EMPTY.substring(0, tabSize);
    autoIndent = Preferences.getBoolean("editor.indent");
    externalEditor = Preferences.getBoolean("editor.external");
  }


  //public void setExternalEditor(boolean externalEditor) {
  //this.externalEditor = externalEditor;
  //}


  // called by JEditTextArea inside processKeyEvent
  public boolean keyPressed(KeyEvent event) {
    // don't do things if the textarea isn't editable
    if (externalEditor) return false;

    //deselect();  // this is for paren balancing
    char c = event.getKeyChar();
    int code = event.getKeyCode();

    //System.out.println(c + " " + code + " " + event);
    //System.out.println();

    if ((event.getModifiers() & KeyEvent.META_MASK) != 0) {
      //event.consume();  // does nothing
      return false;
    }

    // TODO i don't like these accessors. clean em up later.
    if (!editor.sketch.current.modified) {
      if ((code == KeyEvent.VK_BACK_SPACE) || (code == KeyEvent.VK_TAB) ||
          (code == KeyEvent.VK_ENTER) || ((c >= 32) && (c < 128))) {
        editor.sketch.setModified();
      }
    }

    switch ((int) c) {

    case 9:  // expand tabs
      if (expandTabs) {
        //tc.replaceSelection(tabString);
        textarea.setSelectedText(tabString);
        event.consume();
        return true;
      }
      break;

    case 10:  // auto-indent
    case 13:
      if (autoIndent) {
        char contents[] = textarea.getText().toCharArray();

        // this is the previous character
        // (i.e. when you hit return, it'll be the last character
        // just before where the newline will be inserted)
        int origIndex = textarea.getCaretPosition() - 1;

        // NOTE all this cursing about CRLF stuff is probably moot
        // NOTE since the switch to JEditTextArea, which seems to use
        // NOTE only LFs internally (thank god). disabling for 0099.
        // walk through the array to the current caret position,
        // and count how many weirdo windows line endings there are,
        // which would be throwing off the caret position number
        /*
        int offset = 0;
        int realIndex = origIndex;
        for (int i = 0; i < realIndex-1; i++) {
          if ((contents[i] == 13) && (contents[i+1] == 10)) {
            offset++;
            realIndex++;
          }
        }
        // back up until \r \r\n or \n.. @#($* cross platform
        //System.out.println(origIndex + " offset = " + offset);
        origIndex += offset; // ARGH!#(* WINDOWS#@($*
        */

        int spaceCount = calcSpaceCount(origIndex, contents);

        // now before inserting this many spaces, walk forward from
        // the caret position, so that the number of spaces aren't
        // just being duplicated again
        int index = origIndex + 1;
        while ((index < contents.length) &&
               (contents[index] == ' ')) {
          spaceCount--;
          index++;
        }

        // if the last character was a left curly brace, then indent
        if (origIndex != -1) {
          if (contents[origIndex] == '{') {
            spaceCount += tabSize;
          }
        }

        String insertion = "\n" + Editor.EMPTY.substring(0, spaceCount);
        textarea.setSelectedText(insertion);

        // mark this event as already handled
        event.consume();
        return true;
      }
      break;

    case '}':
      if (autoIndent) {
        // first remove anything that was there (in case this multiple
        // characters are selected, so that it's not in the way of the
        // spaces for the auto-indent
        //System.out.println(textarea.getSelectionStart());
        //System.out.println(textarea.getSelectionEnd());
        if (textarea.getSelectionStart() != textarea.getSelectionEnd()) {
          textarea.setSelectedText("");
        }

        // if this brace is the only thing on the line, outdent
        char contents[] = textarea.getText().toCharArray();
        // index to the character to the left of the caret
        int prevCharIndex = textarea.getCaretPosition() - 1;

        // backup from the current caret position to the last newline,
        // checking for anything besides whitespace along the way.
        // if there's something besides whitespace, exit without
        // messing any sort of indenting.
        int index = prevCharIndex;
        //int spaceCount = 0;
        boolean finished = false;
        while ((index != -1) && (!finished)) {
          if (contents[index] == 10) {
            finished = true;
            index++;
          } else if (contents[index] != ' ') {
            // don't do anything, this line has other stuff on it
            return false;
          } else {
            index--;
          }
        }
        if (!finished) return false;  // brace with no start
        int lineStartIndex = index;
        //System.out.println("line starts at " + lineStartIndex);

        // now that we know things are ok to be indented, walk
        // backwards to the last { to see how far its line is indented.
        // this isn't perfect cuz it'll pick up commented areas,
        // but that's not really a big deal and can be fixed when
        // this is all given a more complete (proper) solution.
        index = prevCharIndex;
        int braceDepth = 1;
        finished = false;
        while ((index != -1) && (!finished)) {
          if (contents[index] == '}') {
            // aww crap, this means we're one deeper
            // and will have to find one more extra {
            braceDepth++;
            index--;
          } else if (contents[index] == '{') {
            braceDepth--;
            if (braceDepth == 0) {
              finished = true;
            } // otherwise just teasing, keep going..
          } else {
            index--;
          }
        }
        // never found a proper brace, be safe and don't do anything
        if (!finished) return false;

        // check how many spaces on the line with the matching open brace
        int pairedSpaceCount = calcSpaceCount(index, contents);
        //System.out.println(pairedSpaceCount);

        /*
        // now walk forward and figure out how many spaces there are
        while ((index < contents.length) && (index >= 0) &&
               (contents[index++] == ' ')) {
          spaceCount++;
        }
        */

        // number of spaces found on this line
        //int newSpaceCount = Math.max(0, spaceCount - tabSize);
        // number of spaces on this current line
        //int spaceCount = calcSpaces(caretIndex, contents);
        //System.out.println("spaces is " + spaceCount);
        //String insertion = "\n" + Editor.EMPTY.substring(0, spaceCount);
        //int differential = newSpaceCount - spaceCount;
        //System.out.println("diff is " + differential);
        //int newStart = textarea.getSelectionStart() + differential;
        //textarea.setSelectionStart(newStart);
        //textarea.setSelectedText("}");
        textarea.setSelectionStart(lineStartIndex);
        textarea.setSelectedText(Editor.EMPTY.substring(0, pairedSpaceCount));

        // mark this event as already handled
        event.consume();
        return true;
      }
      break;
    }
    return false;
  }


  protected int calcSpaceCount(int index, char contents[]) {
    // backup from the current caret position to the last newline,
    // so that we can figure out how far this line was indented
    int spaceCount = 0;
    boolean finished = false;
    while ((index != -1) && (!finished)) {
      if ((contents[index] == 10) ||
          (contents[index] == 13)) {
        finished = true;
        index++; // maybe ?
      } else {
        index--;  // new
      }
    }
    // now walk forward and figure out how many spaces there are
    while ((index < contents.length) && (index >= 0) &&
           (contents[index++] == ' ')) {
      spaceCount++;
    }
    return spaceCount;
  }
}
