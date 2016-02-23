/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

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

package processing.mode.java;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.text.BadLocationException;

import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.Util;
import processing.app.ui.Editor;
import processing.core.PApplet;
import processing.mode.java.pdex.LineMarker;
import processing.mode.java.pdex.Problem;


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
  protected JavaEditor editor;

//  static final int WIDE = 12;

	private Color errorColor;
	private Color warningColor;

	// Stores error markers displayed PER TAB along the error bar.
	private List<LineMarker> errorPoints =
	  Collections.synchronizedList(new ArrayList<LineMarker>());


	public MarkerColumn(JavaEditor editor, int height) {
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


  public void paintComponent(Graphics g) {
    g.drawImage(editor.getJavaTextArea().getGutterGradient(),
                0, 0, getWidth(), getHeight(), this);

    for (LineMarker m : errorPoints) {
      if (m.getType() == LineMarker.ERROR) {
        g.setColor(errorColor);
      } else {
        g.setColor(warningColor);
      }
      g.drawLine(2, m.getY(), getWidth() - 2, m.getY());
    }
  }


	public List<LineMarker> getErrorPoints() {
	  return errorPoints;
	}


	public void updateErrorPoints(final List<Problem> problems) {
	  // NOTE: ErrorMarkers are calculated for the present tab only Error Marker
	  // index in the arraylist is LOCALIZED for current tab.
	  Sketch sketch = editor.getSketch();
	  int currentTab = sketch.getCurrentCodeIndex();
	  errorPoints = Collections.synchronizedList(new ArrayList<LineMarker>());
	  // Each problem.getSourceLine() will have an extra line added because
	  // of class declaration in the beginning as well as default imports
	  for (Problem problem : problems) {
	    if (problem.getTabIndex() == currentTab) {
	      errorPoints.add(new LineMarker(problem, problem.isError()));
	    }
	  }
	  repaint();
	  editor.getErrorChecker().updateEditorStatus();
	}


	/** Find out which error/warning the user has clicked and scroll to it */
	private void scrollToMarkerAt(final int y) {
	  try {
      LineMarker m = findClosestMarker(y);
      if (m != null) {
        editor.getErrorChecker().scrollToErrorLine(m.getProblem());
      }
	  } catch (Exception ex) {
	    ex.printStackTrace();
	  }
	}


	/*
  @Override
  public JToolTip createToolTip() {
    return new ErrorToolTip(editor.getMode(), this);
  }
  */


  /** Show tooltip on hover. */
	private void showMarkerHover(final int y) {
	  try {
      LineMarker m = findClosestMarker(y);
      if (m != null) {
        Problem p = m.getProblem();
//	          String kind = p.isError() ?
//	            Language.text("editor.status.error") :
//	            Language.text("editor.status.warning");
//	          setToolTipText(kind + ": " + p.getMessage());
        editor.statusToolTip(MarkerColumn.this, p.getMessage(), p.isError());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }
	  } catch (Exception ex) {
	    ex.printStackTrace();
	  }
	}


	private void recalculateMarkerPositions() {
	  List<LineMarker> errorPoints = getErrorPoints();
	  if (errorPoints != null && errorPoints.size() > 0) {
	    Sketch sketch = editor.getSketch();
	    SketchCode code = sketch.getCurrentCode();
	    int totalLines;
	    try {
	      totalLines = Util.countLines(code.getDocumentText());
	    } catch (BadLocationException e) {
	      e.printStackTrace();
	      totalLines = 1; // do not divide by zero
	    }
	    int visibleLines = editor.getTextArea().getVisibleLines();
	    totalLines = PApplet.max(totalLines, visibleLines);

	    for (LineMarker m : errorPoints) {
	      // Ratio of error line to total lines
	      float y = (m.getLineNumber() + 1) / ((float) totalLines);
	      // Ratio multiplied by height of the error bar
	      y *= getHeight();
	      y -= 15; // -15 is just a vertical offset

	      m.setY((int) y);
	    }
	  }
	}


	private LineMarker findClosestMarker(final int y) {
	  LineMarker closest = null;
	  int closestDist = Integer.MAX_VALUE;
	  for (LineMarker m : errorPoints) {
	    int dist = Math.abs(y - m.getY());
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
}
