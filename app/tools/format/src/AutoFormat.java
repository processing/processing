/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2006 Ben Fry and Casey Reas

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

import java.io.*;
import java.util.StringTokenizer;


/**
 * Tool for auto-formatting code that interfaces to
 * <A HREF="http://jalopy.sourceforge.net/">Jalopy</A>. This is to replace
 * the buggy code formatter found in previous releases.
 */
public class AutoFormat {
  Editor editor;


  public AutoFormat(Editor editor) {
    this.editor = editor;
  }


  public void show() {
    String originalText = editor.textarea.getText();
    int indentSize = Preferences.getInteger("editor.tabs.size");

    String formattedText = null; //strOut.toString();
    if (formattedText.equals(originalText)) {
      editor.message("No changes necessary for Auto Format.");

    } else {
      // replace with new bootiful text
      // selectionEnd hopefully at least in the neighborhood
      editor.setText(formattedText, selectionEnd, selectionEnd);
      editor.sketch.setModified(true);

      /*
      // warn user if there are too many parens in either direction
      if (paren != 0) {
        editor.error("Warning: Too many " +
                     ((paren < 0) ? "right" : "left") +
                     " parentheses.");

      } else if (c_level != 0) {  // check braces only if parens are ok
        editor.error("Warning: Too many " +
                     ((c_level < 0) ? "right" : "left") +
                     " curly braces.");
      } else {
        editor.message("Auto Format finished.");
      }
      */
    }
  }
}
