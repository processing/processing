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
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.Util;
import processing.mode.java.pdex.ErrorCheckerService;
import processing.mode.java.pdex.LineMarker;
import processing.mode.java.pdex.Problem;
import processing.app.Language;


/**
 * Implements the column to the right of the editor window that displays ticks
 * for errors and warnings.
 * <br>
 * All errors and warnings of a sketch are drawn on the bar, clicking on one,
 * scrolls to the tab and location. Error messages displayed on hover. Markers
 * are not in sync with the error line. Similar to eclipse's right error bar
 * which displays the overall errors in a document
 */
public class ErrorColumn extends JPanel {
  protected JavaEditor editor;
  protected ErrorCheckerService errorCheckerService;

  static final int WIDE = 12;
//	protected int preferredHeight;
//	protected int preferredWidth = 12;

//	static final int errorMarkerHeight = 4;

	private Color errorColor;
	private Color warningColor;
	private Color backgroundColor;

	/** Stores error markers displayed PER TAB along the error bar. */
	private List<LineMarker> errorPoints =
	  Collections.synchronizedList(new ArrayList<LineMarker>());

	/** Stores previous list of error markers. */
	private List<LineMarker> errorPointsOld = new ArrayList<LineMarker>();


	public void paintComponent(Graphics g) {
//		Graphics2D g2d = (Graphics2D) g;
//		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(backgroundColor);
		g.fillRect(0, 0, getWidth(), getHeight());

		for (LineMarker m : errorPoints) {
			if (m.getType() == LineMarker.ERROR) {
				g.setColor(errorColor);
			} else {
				g.setColor(warningColor);
			}
			//g.fillRect(2, emarker.getY(), (getWidth() - 3), errorMarkerHeight);
			g.drawLine(2, m.getY(), getWidth() - 2, m.getY());
		}
	}


//	public Dimension getPreferredSize() {
//		return new Dimension(preferredWidth, preferredHeight);
//	}


//	public Dimension getMinimumSize() {
//		return getPreferredSize();
//	}


	public ErrorColumn(JavaEditor editor, int height) {
		this.editor = editor;
//		this.preferredHeight = height;
		this.errorCheckerService = editor.errorCheckerService;

		Mode mode = editor.getMode();
		errorColor = mode.getColor("editor.column.error.color");
		warningColor = mode.getColor("editor.column.warning.color");
		backgroundColor = mode.getColor("editor.gutter.bgcolor");

		addListeners();
	}


	public List<LineMarker> getErrorPoints() {
	  return errorPoints;
	}


	synchronized public void updateErrorPoints(final List<Problem> problems) {
		// NOTE TO SELF: ErrorMarkers are calculated for the present tab only
		// Error Marker index in the arraylist is LOCALIZED for current tab.
		// Also, need to do the update in the UI thread via SwingWorker to prevent
	  // concurrency issues.
		final int fheight = this.getHeight();
		SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

      protected Object doInBackground() throws Exception {
        Sketch sketch = editor.getSketch();
        SketchCode sc = sketch.getCurrentCode();
        int totalLines = 0;
        int currentTab = sketch.getCurrentCodeIndex();
        try {
          Document doc = sc.getDocument();
          totalLines = Util.countLines(doc.getText(0, doc.getLength())) + 1;
        } catch (BadLocationException e) {
          e.printStackTrace();
        }
        // System.out.println("Total lines: " + totalLines);
//        synchronized (errorPoints) {
        errorPointsOld = errorPoints;
        errorPoints = new ArrayList<>();
//          errorPointsOld.clear();
//          for (ErrorMarker marker : errorPoints) {
//            errorPointsOld.add(marker);
//          }
//          errorPoints.clear();

          // Each problem.getSourceLine() will have an extra line added
          // because of
          // class declaration in the beginning as well as default imports
        synchronized (problems) {
          for (Problem problem : problems) {
            if (problem.getTabIndex() == currentTab) {
              // Ratio of error line to total lines
              float y = (problem.getLineNumber() + 1) / ((float) totalLines);
              // Ratio multiplied by height of the error bar
              y *= fheight - 15; // -15 is just a vertical offset
              errorPoints.add(new LineMarker(problem, (int) y,
                                              problem.isError() ?
                                                  LineMarker.ERROR
                                                  : LineMarker.WARNING));
                // System.out.println("Y: " + y);
              }
            }
          }
//        }
        return null;
      }

			protected void done() {
				repaint();
			}
		};

		try {
			worker.execute(); // I eat concurrency bugs for breakfast.
		} catch (Exception exp) {
			System.out.println("Errorbar update markers is slacking."
					+ exp.getMessage());
			// e.printStackTrace();
		}
	}

	/**
	 * Check if new errors have popped up in the sketch since the last check
	 *
	 * @return true - if errors have changed
	 */
	public boolean errorPointsChanged() {
		if (errorPointsOld.size() != errorPoints.size()) {
			editor.getTextArea().repaint();
			// System.out.println("2 Repaint " + System.currentTimeMillis());
			return true;
		}

		else {
			for (int i = 0; i < errorPoints.size(); i++) {
				if (errorPoints.get(i).getY() != errorPointsOld.get(i).getY()) {
					editor.getTextArea().repaint();
					// System.out.println("3 Repaint " +
					// System.currentTimeMillis());
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Add various mouse listeners.
	 */
	protected void addListeners() {

		addMouseListener(new MouseAdapter() {

			// Find out which error/warning the user has clicked
			// and then scroll to that
			@Override
			public void mouseClicked(final MouseEvent e) {
        SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

          protected Object doInBackground() throws Exception {
            for (LineMarker eMarker : errorPoints) {
              // -2 and +2 are extra allowance, clicks in the
              // vicinity of the markers register that way
              if (e.getY() >= eMarker.getY() - 2
                  && e.getY() <= eMarker.getY() + 2) {
                errorCheckerService.scrollToErrorLine(eMarker.getProblem());
                return null;
              }
            }
            return null;
          }
        };

				try {
					worker.execute();
				} catch (Exception exp) {
					System.out.println("Errorbar mouseClicked is slacking."
							+ exp.getMessage());
					// e.printStackTrace();
				}

			}
		});

		// Tooltip on hover
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(final MouseEvent evt) {
        SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

          protected Object doInBackground() throws Exception {
            for (LineMarker eMarker : errorPoints) {
              if (evt.getY() >= eMarker.getY() - 2 &&
                  evt.getY() <= eMarker.getY() + 2) {
                Problem p = eMarker.getProblem();
                String msg = ((p.isError()
                               ? Language.text("editor.status.error")
                               : Language.text("editor.status.warning")) + ": "
                              + p.getMessage());

                setToolTipText(msg);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                break;
              }
            }
            return null;
          }
        };

				try {
					worker.execute();
				} catch (Exception exp) {
					System.out
							.println("Errorbar mousemoved Worker is slacking."
									+ exp.getMessage());
					// e.printStackTrace();
				}
			}
		});
	}


	public Dimension getPreferredSize() {
	  return new Dimension(WIDE, super.getPreferredSize().height);
	}


	public Dimension getMinimumSize() {
	  return new Dimension(WIDE, super.getMinimumSize().height);
	}
}
