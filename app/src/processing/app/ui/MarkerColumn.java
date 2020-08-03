/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org

Copyright (c) 2012-19 The Processing Foundation

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

package processing.app.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JPanel;

import processing.app.Mode;
import processing.app.Problem;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.syntax.PdeTextArea;
import processing.app.ui.Editor;
import processing.core.PApplet;


/**
 * Implements the column to the right of the editor window that displays ticks
 * for errors and warnings.
 * <br>
 * All errors and warnings of a sketch are drawn on the bar, clicking on one,
 * scrolls to the tab and location. Error messages displayed on hover. Markers
 * are not in sync with the error line. Similar to Eclipse's right error bar
 * which displays the overall errors in a document
 */
public class MarkerColumn extends JPanel {
  protected Editor editor;

//  static final int WIDE = 12;

  private Color errorColor;
  private Color warningColor;

  // Stores error markers displayed PER TAB along the error bar.
  private List<LineMarker> errorPoints = new ArrayList<>();


  public MarkerColumn(Editor editor, int height) {
    this.editor = editor;

    Mode mode = editor.getMode();
    errorColor = mode.getColor("editor.column.error.color");
    warningColor = mode.getColor("editor.column.warning.color");

    addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        scrollToMarkerAt(e.getY());
      }
    });

    addMouseMotionListener(new MouseMotionAdapter() {
      public void mouseMoved(final MouseEvent e) {
        showMarkerHover(e.getY());
      }
    });
  }


  @Override
  public void repaint() {
    recalculateMarkerPositions();
    super.repaint();
  }


  @Override
  public void paintComponent(Graphics g) {
    PdeTextArea pta = editor.getPdeTextArea();
    if (pta != null) {
      g.drawImage(pta.getGutterGradient(),
                  0, 0, getWidth(), getHeight(), this);
    }

    int currentTabIndex = editor.getSketch().getCurrentCodeIndex();

    for (LineMarker m : errorPoints) {
      Problem problem = m.problem;
      if (problem.getTabIndex() != currentTabIndex) continue;
      if (problem.isError()) {
        g.setColor(errorColor);
      } else {
        g.setColor(warningColor);
      }
      g.drawLine(2, m.y, getWidth() - 2, m.y);
    }
  }


  public void updateErrorPoints(final List<Problem> problems) {
    errorPoints = problems.stream()
        .map(LineMarker::new)
        .collect(Collectors.toList());
    repaint();
  }


  /** Find out which error/warning the user has clicked and scroll to it */
  private void scrollToMarkerAt(final int y) {
    try {
      LineMarker m = findClosestMarker(y);
      if (m != null) {
        editor.highlight(m.problem);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }


  /** Show tooltip on hover. */
  private void showMarkerHover(final int y) {
    try {
      LineMarker m = findClosestMarker(y);
      if (m != null) {
        Problem p = m.problem;
        editor.statusToolTip(MarkerColumn.this, p.getMessage(), p.isError());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }


  private void recalculateMarkerPositions() {
    if (errorPoints != null && errorPoints.size() > 0) {
      Sketch sketch = editor.getSketch();
      SketchCode code = sketch.getCurrentCode();
      int currentTab = sketch.getCurrentCodeIndex();
      int totalLines = PApplet.max(1, code.getLineCount()); // do not divide by zero
      int visibleLines = editor.getTextArea().getVisibleLines();
      totalLines = PApplet.max(totalLines, visibleLines);

      int topMargin = 20; // top scroll button
      int bottomMargin = 40; // bottom scroll button and horizontal scrollbar
      int height = getHeight() - topMargin - bottomMargin;

      for (LineMarker m : errorPoints) {
        Problem problem = m.problem;
        if (problem.getTabIndex() != currentTab) continue;
        // Ratio of error line to total lines
        float ratio = (problem.getLineNumber() + 1) / ((float) totalLines);
        // Ratio multiplied by height of the error bar
        float y = topMargin + ratio * height;

        m.y = (int) y;
      }
    }
  }


  private LineMarker findClosestMarker(final int y) {
    LineMarker closest = null;
    int closestDist = Integer.MAX_VALUE;
    for (LineMarker m : errorPoints) {
      int dist = Math.abs(y - m.y);
      if (dist < 3 && dist < closestDist) {
        closest = m;
        closestDist = dist;
      }
    }
    return closest;
  }


  public Dimension getPreferredSize() {
    return new Dimension(Editor.RIGHT_GUTTER, super.getPreferredSize().height);
  }


  public Dimension getMinimumSize() {
    return new Dimension(Editor.RIGHT_GUTTER, super.getMinimumSize().height);
  }


  /**
   * Line markers displayed on the Error Column.
   */
  private static class LineMarker {
    /** y co-ordinate of the marker */
    int y;

    /** Problem that the error marker represents */
    final Problem problem;


    LineMarker(Problem problem) {
      this.problem = problem;
    }
  }
}
