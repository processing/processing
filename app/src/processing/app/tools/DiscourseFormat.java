/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2005-06 Ignacio Manuel Gonzalez Moreta.
  Copyright (c) 2006-08 Ben Fry and Casey Reas

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

import java.awt.*;
import java.awt.datatransfer.*;
import javax.swing.text.Segment;

import processing.app.*;
import processing.app.syntax.*;
import processing.core.PApplet;

/**
 * Format for Discourse Tool
 * <p/>
 * Original code by <A HREF="http://usuarios.iponet.es/imoreta">owd</A>.
 * Revised and updated for revision 0108 by Ben Fry (10 March 2006).
 * This code may later be moved to its own 'Tool' plugin, but is included
 * with release 0108+ while features for the "Tools" menu are in testing.
 * <p/>
 * Updated for 0122 to simply copy the code directly to the clipboard,
 * rather than opening a new window.
 * <p/>
 * Updated for 0144 to only format the selected lines.
 * <p/>
 * Notes from the original source:
 * Discourse.java This is a dirty-mix source.
 * NOTE that: No macs and no keyboard. Unreliable source.
 * Only format processing code using fontMetrics.
 * It works under my windows XP + PentiumIV + Processing 0091.
 */
public class DiscourseFormat {

  Editor editor;
  // JTextArea of the actual Editor
  JEditTextArea textarea;


  /**
   * Creates a new window with the formated (YaBB tags) sketchcode
   * from the actual Processing Tab ready to send to the processing discourse
   * web (copy & paste)
   */
  public DiscourseFormat(Editor editor) {
    this.editor = editor;
    this.textarea = editor.getTextArea();
  }


  /**
   * Format and render sketch code.
   */
  public void show() {
    // [code] tag cancels other tags, using [quote]
    StringBuffer cf = new StringBuffer("[quote]\n");

    int selStart = textarea.getSelectionStart();
    int selStop = textarea.getSelectionStop();

    int startLine = textarea.getSelectionStartLine();
    int stopLine = textarea.getSelectionStopLine();

    // If no selection, convert all the lines
    if (selStart == selStop) {
      startLine = 0;
      stopLine = textarea.getLineCount() - 1;
    } else {
      // Make sure the selection doesn't end at the beginning of the last line
      if (textarea.getLineStartOffset(stopLine) == selStop) {
        stopLine--;
      }
    }

    // Read the code line by line
    for (int i = startLine; i <= stopLine; i++) {
      appendFormattedLine(cf, i);
    }

    cf.append("\n[/quote]");

    StringSelection formatted = new StringSelection(cf.toString());
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(formatted, new ClipboardOwner() {
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
          // i don't care about ownership
        }
      });

    editor.statusNotice("Code formatted for processing.org/discourse " +
                   "has been copied to the clipboard.");
  }


  // A terrible headache...
  public void appendFormattedLine(StringBuffer cf, int line) {
    Segment segment = new Segment();

    TextAreaPainter painter = textarea.getPainter();
    TokenMarker tokenMarker = textarea.getTokenMarker();

    // Use painter's cached info for speed
//    FontMetrics fm = painter.getFontMetrics();

    // get line text from parent text area
    textarea.getLineText(line, segment);

    char[] segmentArray = segment.array;
    int limit = segment.getEndIndex();
    int segmentOffset = segment.offset;
    int segmentCount = segment.count;
//    int width = 0;

    // If syntax coloring is disabled, do simple translation
    if (tokenMarker == null) {
      for (int j = 0; j < segmentCount; j++) {
        char c = segmentArray[j + segmentOffset];
        cf = cf.append(c);
//        int charWidth;
//        if (c == '\t') {
//          charWidth = (int) painter.nextTabStop(width, j) - width;
//        } else {
//          charWidth = fm.charWidth(c);
//        }
//        width += charWidth;
      }

    } else {
      // If syntax coloring is enabled, we have to do this
      // because tokens can vary in width
      Token tokens;
      if ((painter.getCurrentLineIndex() == line) &&
          (painter.getCurrentLineTokens() != null)) {
        tokens = painter.getCurrentLineTokens();

      } else {
        painter.setCurrentLineIndex(line);
        painter.setCurrentLineTokens(tokenMarker.markTokens(segment, line));
        tokens = painter.getCurrentLineTokens();
      }

      int offset = 0;
//      Font defaultFont = painter.getFont();
      SyntaxStyle[] styles = painter.getStyles();

      for (;;) {
        byte id = tokens.id;
        if (id == Token.END) {
          char c = segmentArray[segmentOffset + offset];
          if (segmentOffset + offset < limit) {
            cf.append(c);
          } else {
            cf.append('\n');
          }
          return; // cf.toString();
        }
        if (id == Token.NULL) {
//          fm = painter.getFontMetrics();
        } else {
          // Place open tags []
          cf.append("[color=#");
          cf.append(PApplet.hex(styles[id].getColor().getRGB() & 0xFFFFFF, 6));
          cf.append("]");

          if (styles[id].isBold())
            cf.append("[b]");

//          fm = styles[id].getFontMetrics(defaultFont);
        }
        int length = tokens.length;

        for (int j = 0; j < length; j++) {
          char c = segmentArray[segmentOffset + offset + j];
          if (offset == 0 && c == ' ') {
            // Works on Safari but not Camino 1.6.3 or Firefox 2.x on OS X.
            cf.append('\u00A0');  // &nbsp;
//            if ((j % 2) == 1) {
//              cf.append("[b]\u00A0[/b]");
//            } else {
//              cf.append(' ');
//            }
          } else {
            cf.append(c);
          }
          // Place close tags [/]
          if (j == (length - 1) && id != Token.NULL && styles[id].isBold())
            cf.append("[/b]");
          if (j == (length - 1) && id != Token.NULL)
            cf.append("[/color]");
//          int charWidth;
//          if (c == '\t') {
//            charWidth = (int) painter
//              .nextTabStop(width, offset + j)
//              - width;
//          } else {
//            charWidth = fm.charWidth(c);
//          }
//          width += charWidth;
        }
        offset += length;
        tokens = tokens.next;
      }
    }
  }
}