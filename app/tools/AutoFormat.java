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

package processing.app.tools;

import processing.app.*;
import processing.core.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 * Basic but buggy handler for cleaning up code.
 * <PRE>
 * an improved algorithm that would still avoid a full state machine
 * 1. build an array of strings for the lines
 * 2. first remove everything between / * and * / (relentless)
 * 3. next remove anything inside two sets of " "
 *    but not if escaped with a \
 *    these can't extend beyond a line, so that works well
 *    (this will save from "http://blahblah" showing up as a comment)
 * 4. remove from* to the end of a line everywhere
 * 5. run through remaining text to do indents
 *    using hokey brace-counting algorithm
 * 6. also add indents for switch statements
 *    case blah: { }  (colons at end of line isn't a good way)
 *    maybe /case \w+\:/
 * </PRE>
 */
public class AutoFormat {
  Editor editor;


  public AutoFormat(Editor editor) {
    this.editor = editor;
  }


  public void show() {
    String prog = editor.textarea.getText();

    // TODO re-enable history
    //history.record(prog, SketchHistory.BEAUTIFY);

    int tabSize = Preferences.getInteger("editor.tabs.size");

    char program[] = prog.toCharArray();
    StringBuffer buffer = new StringBuffer();
    boolean gotBlankLine = false;
    int index = 0;
    int level = 0;

    while (index != program.length) {
      int begin = index;
      while ((program[index] != '\n') &&
             (program[index] != '\r')) {
        index++;
        if (program.length == index)
          break;
      }
      int end = index;
      if (index != program.length) {
        if ((index+1 != program.length) &&
            // treat \r\n from windows as one line
            (program[index] == '\r') &&
            (program[index+1] == '\n')) {
          index += 2;
        } else {
          index++;
        }
      } // otherwise don't increment

      String line = new String(program, begin, end-begin);
      line = line.trim();

      if (line.length() == 0) {
        if (!gotBlankLine) {
          // let first blank line through
          buffer.append('\n');
          gotBlankLine = true;
        }
      } else {
        //System.out.println(level);
        int idx = -1;
        String myline = line.substring(0);
        while (myline.lastIndexOf('}') != idx) {
          idx = myline.indexOf('}');
          myline = myline.substring(idx+1);
          level--;
        }
        //for (int i = 0; i < level*2; i++) {
        // TODO i've since forgotten how i made this work (maybe it's even
        //      a bug) but for now, level is incrementing/decrementing in
        //      steps of two. in the interest of getting a release out,
        //      i'm just gonna roll with that since this function will prolly
        //      be replaced entirely and there are other things to worry about.
        for (int i = 0; i < tabSize * level / 2; i++) {
          buffer.append(' ');
        }
        buffer.append(line);
        buffer.append('\n');
        //if (line.charAt(0) == '{') {
        //level++;
        //}
        idx = -1;
        myline = line.substring(0);
        while (myline.lastIndexOf('{') != idx) {
          idx = myline.indexOf('{');
          myline = myline.substring(idx+1);
          level++;
        }
        gotBlankLine = false;
      }
    }

    // save current (rough) selection point
    int selectionEnd = editor.textarea.getSelectionEnd();

    // replace with new bootiful text
    editor.setText(buffer.toString(), false);

    // make sure the caret would be past the end of the text
    if (buffer.length() < selectionEnd - 1) {
      selectionEnd = buffer.length() - 1;
    }

    // at least in the neighborhood
    editor.textarea.select(selectionEnd, selectionEnd);

    editor.sketch.setModified();
    //buttons.clear();
  }
}