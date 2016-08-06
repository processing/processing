/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-16 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.app.syntax;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;
import java.util.Map;

import processing.app.Mode;
import processing.app.ui.Editor;


/**
 * Extensions to JEditTextArea to for the PDE. These were moved out of
 * JavaTextArea because they were not Java-specific and would be helpful
 * for other Mode implementations.
 */
public class PdeTextArea extends JEditTextArea {
  protected final Editor editor;

  protected Image gutterGradient;

  /// the text marker for highlighting breakpoints in the gutter
  static public final String BREAK_MARKER = "<>";
  /// the text marker for highlighting the current line in the gutter
  static public final String STEP_MARKER = "->";

  /// maps line index to gutter text
  protected final Map<Integer, String> gutterText = new HashMap<>();


  public PdeTextArea(TextAreaDefaults defaults, InputHandler inputHandler,
                     Editor editor) {
    super(defaults, inputHandler);
    this.editor = editor;

    // change cursor to pointer in the gutter area
    painter.addMouseMotionListener(gutterCursorMouseAdapter);

    add(CENTER, painter);

    // load settings from theme.txt
    Mode mode = editor.getMode();
    gutterGradient = mode.makeGradient("editor", Editor.LEFT_GUTTER, 500);
  }


    public Image getGutterGradient() {
    return gutterGradient;
  }


  public void setMode(Mode mode) {
    ((PdeTextAreaPainter) painter).setMode(mode);
  }


  /**
   * Set the gutter text of a specific line.
   *
   * @param lineIdx
   *          the line index (0-based)
   * @param text
   *          the text
   */
  public void setGutterText(int lineIdx, String text) {
    gutterText.put(lineIdx, text);
    painter.invalidateLine(lineIdx);
  }


  /**
   * Clear the gutter text of a specific line.
   *
   * @param lineIdx
   *          the line index (0-based)
   */
  public void clearGutterText(int lineIdx) {
    gutterText.remove(lineIdx);
    painter.invalidateLine(lineIdx);
  }


  /**
   * Clear all gutter text.
   */
  public void clearGutterText() {
    for (int lineIdx : gutterText.keySet()) {
      painter.invalidateLine(lineIdx);
    }
    gutterText.clear();
  }


  /**
   * Retrieve the gutter text of a specific line.
   * @param lineIdx the line index (0-based)
   * @return the gutter text
   */
  public String getGutterText(int lineIdx) {
    return gutterText.get(lineIdx);
  }


  /**
   * Convert a character offset to a horizontal pixel position inside
   * the text area. Overridden to take gutter width into account.
   * @param line the 0-based line number
   * @param offset the character offset (0 is the first character on a line)
   * @return the horizontal position
   */
  @Override
  public int _offsetToX(int line, int offset) {
    return super._offsetToX(line, offset) + Editor.LEFT_GUTTER;
  }


  /**
   * Convert a horizontal pixel position to a character offset. Overridden to
   * take gutter width into account.
   * @param line the 0-based line number
   * @param x the horizontal pixel position
   * @return he character offset (0 is the first character on a line)
   */
  @Override
  public int xToOffset(int line, int x) {
    return super.xToOffset(line, x - Editor.LEFT_GUTTER);
  }


  /**
   * Sets default cursor (instead of text cursor) in the gutter area.
   */
  protected final MouseMotionAdapter gutterCursorMouseAdapter = new MouseMotionAdapter() {
    private int lastX; // previous horizontal position of the mouse cursor

    @Override
    public void mouseMoved(MouseEvent me) {
      if (me.getX() < Editor.LEFT_GUTTER) {
        if (lastX >= Editor.LEFT_GUTTER) {
          painter.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
      } else {
        if (lastX < Editor.LEFT_GUTTER) {
          painter.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        }
      }
      lastX = me.getX();
    }
  };


  public Editor getEditor() {
    return editor;
  }
}