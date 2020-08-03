/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-16 The Processing Foundation

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.mode.java.debug;

import java.util.HashSet;
import java.util.Set;

import processing.mode.java.JavaEditor;


/**
 * Model/Controller for a highlighted source code line. Implements a custom
 * background color and a text based marker placed in the left-hand gutter area.
 */
public class LineHighlight {
  protected final JavaEditor editor; // the view, used for highlighting lines by setting a background color
  protected final LineID lineID; // the id of the line
  protected String marker; //
  protected int priority = 0;
  protected static final Set<LineHighlight> allHighlights = new HashSet<>();


  /**
   * Create a {@link LineHighlight}.
   */
  public LineHighlight(LineID lineID, JavaEditor editor) {
    this.lineID = lineID;
    this.editor = editor;
    lineID.addListener(this);
    lineID.startTracking(editor.getTab(lineID.fileName()).getDocument()); // TODO: overwrite a previous doc?
    paint(); // already checks if on current tab
    allHighlights.add(this);
  }


  protected static boolean isHighestPriority(LineHighlight hl) {
    for (LineHighlight check : allHighlights) {
      if (check.getLineID().equals(hl.getLineID()) &&
          check.priority() > hl.priority()) {
        return false;
      }
    }
    return true;
  }


  public void setPriority(int p) {
    this.priority = p;
  }


  public int priority() {
    return priority;
  }


  /**
   * Create a {@link LineHighlight} on the current tab.
   *
   * @param lineIdx the line index on the current tab to highlight
   * @param editor the {@link JavaEditor}
   */
  // TODO: Remove and replace by {@link #LineHighlight(LineID lineID, JavaEditor editor)}
  public LineHighlight(int lineIdx, JavaEditor editor) {
    this(editor.getLineIDInCurrentTab(lineIdx), editor);
  }


  /**
   * Set a text based marker displayed in the left hand gutter area of this
   * highlighted line.
   *
   * @param marker the marker text
   */
  public void setMarker(String marker) {
    this.marker = marker;
    paint();
  }


  /**
   * Retrieve the line id of this {@link LineHighlight}.
   *
   * @return the line id
   */
  public LineID getLineID() {
    return lineID;
  }


  /**
   * Test if this highlight is on a certain line.
   *
   * @param testLine the line to test
   * @return true if this highlight is on the given line
   */
  public boolean isOnLine(LineID testLine) {
    return lineID.equals(testLine);
  }


  /**
   * Event handler for line number changes (due to editing). Will remove the
   * highlight from the old line number and repaint it at the new location.
   *
   * @param line the line that has changed
   * @param oldLineIdx the old line index (0-based)
   * @param newLineIdx the new line index (0-based)
   */
  public void lineChanged(LineID line, int oldLineIdx, int newLineIdx) {
    // clear old line
    if (editor.isInCurrentTab(new LineID(line.fileName(), oldLineIdx))) {
      editor.getJavaTextArea().clearGutterText(oldLineIdx);
    }

    // paint new line
    // but only if it's on top -> fixes current line being hidden by breakpoint moving it down.
    // lineChanged events seem to come in inverse order of startTracking the LineID. (and bp is created first...)
    if (LineHighlight.isHighestPriority(this)) {
      paint();
    }
  }


  /**
   * Notify this line highlight that it is no longer used. Call this for
   * cleanup before the {@link LineHighlight} is discarded.
   */
  public void dispose() {
    lineID.removeListener(this);
    lineID.stopTracking();
    allHighlights.remove(this);
  }


  /**
   * (Re-)paint this line highlight.
   */
  public void paint() {
    if (editor.isInCurrentTab(lineID)) {
      if (marker != null) {
        editor.getJavaTextArea().setGutterText(lineID.lineIdx(), marker);
      }
    }
  }


  /**
   * Clear this line highlight.
   */
  public void clear() {
    if (editor.isInCurrentTab(lineID)) {
      editor.getJavaTextArea().clearGutterText(lineID.lineIdx());
    }
  }
}
