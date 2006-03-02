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
 * <p/>
 * For version 0099, some changes have been made to make the indents
 * smarter. There are still issues though:
 * + indent happens when it picks up a curly brace on the previous line,
 *   but not if there's a  blank line between them.
 * + It also doesn't handle single indent situations where a brace
 *   isn't used (i.e. an if statement or for loop that's a single line).
 *   It shouldn't actually be using braces.
 * Solving these issues, however, would probably best be done by a
 * smarter parser/formatter, rather than continuing to hack this class.
 */
public class EditorListener {
  Editor editor;
  JEditTextArea textarea;

  boolean externalEditor;
  boolean tabsExpand;
  boolean tabsIndent;
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
    tabsExpand = Preferences.getBoolean("editor.tabs.expand");
    //tabsIndent = Preferences.getBoolean("editor.tabs.indent");
    tabSize = Preferences.getInteger("editor.tabs.size");
    tabString = Editor.EMPTY.substring(0, tabSize);
    autoIndent = Preferences.getBoolean("editor.indent");
    externalEditor = Preferences.getBoolean("editor.external");
  }


  //public void setExternalEditor(boolean externalEditor) {
  //this.externalEditor = externalEditor;
  //}


  /**
   * Intercepts key pressed events for JEditTextArea.
   * <p/>
   * Called by JEditTextArea inside processKeyEvent(). Note that this
   * won't intercept actual characters, because those are fired on
   * keyTyped().
   */
  public boolean keyPressed(KeyEvent event) {
    // don't do things if the textarea isn't editable
    if (externalEditor) return false;

    //deselect();  // this is for paren balancing
    char c = event.getKeyChar();
    int code = event.getKeyCode();

    //System.out.println((int)c + " " + code + " " + event);
    //System.out.println();

    if ((event.getModifiers() & KeyEvent.META_MASK) != 0) {
      //event.consume();  // does nothing
      return false;
    }

    // TODO i don't like these accessors. clean em up later.
    if (!editor.sketch.modified) {
      if ((code == KeyEvent.VK_BACK_SPACE) || (code == KeyEvent.VK_TAB) ||
          (code == KeyEvent.VK_ENTER) || ((c >= 32) && (c < 128))) {
        editor.sketch.setModified(true);
      }
    }

    if ((code == KeyEvent.VK_UP) &&
        ((event.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
      // back up to the last empty line
      char contents[] = textarea.getText().toCharArray();
      //int origIndex = textarea.getCaretPosition() - 1;
      int caretIndex = textarea.getCaretPosition();

      int index = calcLineStart(caretIndex - 1, contents);
      //System.out.println("line start " + (int) contents[index]);
      index -= 2;  // step over the newline
      //System.out.println((int) contents[index]);
      boolean onlySpaces = true;
      while (index > 0) {
        if (contents[index] == 10) {
          if (onlySpaces) {
            index++;
            break;
          } else {
            onlySpaces = true;  // reset
          }
        } else if (contents[index] != ' ') {
          onlySpaces = false;
        }
        index--;
      }
      // if the first char, index will be -2
      if (index < 0) index = 0;

      if ((event.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
        textarea.setSelectionStart(caretIndex);
        textarea.setSelectionEnd(index);
      } else {
        textarea.setCaretPosition(index);
      }
      event.consume();
      return true;

    } else if ((code == KeyEvent.VK_DOWN) &&
               ((event.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
      char contents[] = textarea.getText().toCharArray();
      int caretIndex = textarea.getCaretPosition();

      int index = caretIndex;
      int lineStart = 0;
      boolean onlySpaces = false;  // don't count this line
      while (index < contents.length) {
        if (contents[index] == 10) {
          if (onlySpaces) {
            index = lineStart;  // this is it
            break;
          } else {
            lineStart = index + 1;
            onlySpaces = true;  // reset
          }
        } else if (contents[index] != ' ') {
          onlySpaces = false;
        }
        index++;
      }
      // if the first char, index will be -2
      //if (index < 0) index = 0;

      //textarea.setSelectionStart(index);
      //textarea.setSelectionEnd(index);
      if ((event.getModifiers() & KeyEvent.SHIFT_MASK) != 0) {
        textarea.setSelectionStart(caretIndex);
        textarea.setSelectionEnd(index);
      } else {
        textarea.setCaretPosition(index);
      }
      event.consume();
      return true;
    }


    switch ((int) c) {

    case 9:
      if (tabsExpand) {  // expand tabs
        textarea.setSelectedText(tabString);
        event.consume();
        return true;

      } else if (tabsIndent) {
        // this code is incomplete

        // if this brace is the only thing on the line, outdent
        //char contents[] = getCleanedContents();
        char contents[] = textarea.getText().toCharArray();
        // index to the character to the left of the caret
        int prevCharIndex = textarea.getCaretPosition() - 1;

        // now find the start of this line
        int lineStart = calcLineStart(prevCharIndex, contents);

        int lineEnd = lineStart;
        while ((lineEnd < contents.length - 1) &&
               (contents[lineEnd] != 10)) {
          lineEnd++;
        }

        // get the number of braces, to determine whether this is an indent
        int braceBalance = 0;
        int index = lineStart;
        while ((index < contents.length) &&
               (contents[index] != 10)) {
          if (contents[index] == '{') {
            braceBalance++;
          } else if (contents[index] == '}') {
            braceBalance--;
          }
          index++;
        }

        // if it's a starting indent, need to ignore it, so lineStart
        // will be the counting point. but if there's a closing indent,
        // then the lineEnd should be used.
        int where = (braceBalance > 0) ? lineStart : lineEnd;
        int indent = calcBraceIndent(where, contents);
        if (indent == -1) {
          // no braces to speak of, do nothing
          indent = 0;
        } else {
          indent += tabSize;
        }

        // and the number of spaces it has
        int spaceCount = calcSpaceCount(prevCharIndex, contents);

        textarea.setSelectionStart(lineStart);
        textarea.setSelectionEnd(lineStart + spaceCount);
        textarea.setSelectedText(Editor.EMPTY.substring(0, indent));

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
        //int origCount = spaceCount;

        // now before inserting this many spaces, walk forward from
        // the caret position, so that the number of spaces aren't
        // just being duplicated again
        int index = origIndex + 1;
        int extraCount = 0;
        while ((index < contents.length) &&
               (contents[index] == ' ')) {
          //spaceCount--;
          extraCount++;
          index++;
        }

        // hitting return on a line with spaces *after* the caret
        // can cause trouble. for simplicity's sake, just ignore this case.
        //if (spaceCount < 0) spaceCount = origCount;
        if (spaceCount - extraCount > 0) {
          spaceCount -= extraCount;
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

        /*
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
        */
        int pairedSpaceCount = calcBraceIndent(prevCharIndex, contents); //, 1);
        if (pairedSpaceCount == -1) return false;

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


  /**
   * Return the index for the first character on this line.
   */
  protected int calcLineStart(int index, char contents[]) {
    // backup from the current caret position to the last newline,
    // so that we can figure out how far this line was indented
    int spaceCount = 0;
    boolean finished = false;
    while ((index != -1) && (!finished)) {
      if ((contents[index] == 10) ||
          (contents[index] == 13)) {
        finished = true;
        //index++; // maybe ?
      } else {
        index--;  // new
      }
    }
    // add one because index is either -1 (the start of the document)
    // or it's the newline character for the previous line
    return index + 1;
  }


  /**
   * Calculate the number of spaces on this line.
   */
  protected int calcSpaceCount(int index, char contents[]) {
    index = calcLineStart(index, contents);

    int spaceCount = 0;
    // now walk forward and figure out how many spaces there are
    while ((index < contents.length) && (index >= 0) &&
           (contents[index++] == ' ')) {
      spaceCount++;
    }
    return spaceCount;
  }


  /**
   * Walk back from 'index' until the brace that seems to be
   * the beginning of the current block, and return the number of
   * spaces found on that line.
   */
  protected int calcBraceIndent(int index, char contents[]) {
    // now that we know things are ok to be indented, walk
    // backwards to the last { to see how far its line is indented.
    // this isn't perfect cuz it'll pick up commented areas,
    // but that's not really a big deal and can be fixed when
    // this is all given a more complete (proper) solution.
    int braceDepth = 1;
    boolean finished = false;
    while ((index != -1) && (!finished)) {
      if (contents[index] == '}') {
        // aww crap, this means we're one deeper
        // and will have to find one more extra {
        braceDepth++;
        //if (braceDepth == 0) {
        //finished = true;
        //}
        index--;
      } else if (contents[index] == '{') {
        braceDepth--;
        if (braceDepth == 0) {
          finished = true;
        }
        index--;
      } else {
        index--;
      }
    }
    // never found a proper brace, be safe and don't do anything
    if (!finished) return -1;

    // check how many spaces on the line with the matching open brace
    //int pairedSpaceCount = calcSpaceCount(index, contents);
    //System.out.println(pairedSpaceCount);
    return calcSpaceCount(index, contents);
  }


  /**
   * Get the character array and blank out the commented areas.
   * This hasn't yet been tested, the plan was to make auto-indent
   * less gullible (it gets fooled by braces that are commented out).
   */
  protected char[] getCleanedContents() {
    char c[] = textarea.getText().toCharArray();

    int index = 0;
    while (index < c.length - 1) {
      if ((c[index] == '/') && (c[index+1] == '*')) {
        c[index++] = 0;
        c[index++] = 0;
        while ((index < c.length - 1) &&
               !((c[index] == '*') && (c[index+1] == '/'))) {
          c[index++] = 0;
        }

      } else if ((c[index] == '/') && (c[index+1] == '/')) {
        // clear out until the end of the line
        while ((index < c.length) && (c[index] != 10)) {
          c[index++] = 0;
        }
        if (index != c.length) {
          index++;  // skip over the newline
        }
      }
    }
    return c;
  }

  /*
  protected char[] getCleanedContents() {
    char c[] = textarea.getText().toCharArray();
    boolean insideMulti;  // multi-line comment
    boolean insideSingle;  // single line double slash

    //for (int i = 0; i < c.length - 1; i++) {
    int index = 0;
    while (index < c.length - 1) {
      if (insideMulti && (c[index] == '*') && (c[index+1] == '/')) {
        insideMulti = false;
        index += 2;
      } else if ((c[index] == '/') && (c[index+1] == '*')) {
        insideMulti = true;
        index += 2;
      } else if ((c[index] == '/') && (c[index+1] == '/')) {
        // clear out until the end of the line
        while (c[index] != 10) {
          c[index++] = 0;
        }
        index++;
      }
    }
  }
  */
}
