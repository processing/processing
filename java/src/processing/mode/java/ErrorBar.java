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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;

import processing.app.Base;
import processing.app.SketchCode;
import processing.mode.java.pdex.ErrorCheckerService;
import processing.mode.java.pdex.ErrorMarker;
import processing.mode.java.pdex.Problem;

/**
 * The bar on the left of the text area which displays all errors as rectangles. <br>
 * <br>
 * All errors and warnings of a sketch are drawn on the bar, clicking on one,
 * scrolls to the tab and location. Error messages displayed on hover. Markers
 * are not in sync with the error line. Similar to eclipse's right error bar
 * which displays the overall errors in a document
 *
 * @author Manindra Moharana &lt;me@mkmoharana.com&gt;
 *
 */
public class ErrorBar extends JPanel {
	/**
	 * Preferred height of the component
	 */
	protected int preferredHeight;

	/**
	 * Preferred height of the component
	 */
	protected int preferredWidth = 12;

	/**
	 * Height of marker
	 */
	public static final int errorMarkerHeight = 4;

	/**
	 * Color of Error Marker
	 */
	public Color errorColor; // = new Color(0xED2630);

	/**
	 * Color of Warning Marker
	 */
	public Color warningColor; // = new Color(0xFFC30E);

	/**
	 * Background color of the component
	 */
	public Color backgroundColor; // = new Color(0x2C343D);

	/**
	 * JavaEditor instance
	 */
	protected JavaEditor editor;

	/**
	 * ErrorCheckerService instance
	 */
	protected ErrorCheckerService errorCheckerService;

	/**
	 * Stores error markers displayed PER TAB along the error bar.
	 */
	protected List<ErrorMarker> errorPoints = new ArrayList<ErrorMarker>();

	/**
	 * Stores previous list of error markers.
	 */
	protected ArrayList<ErrorMarker> errorPointsOld = new ArrayList<ErrorMarker>();

	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(backgroundColor);
		g.fillRect(0, 0, getWidth(), getHeight());

		for (ErrorMarker emarker : errorPoints) {
			if (emarker.getType() == ErrorMarker.Error) {
				g.setColor(errorColor);
			} else {
				g.setColor(warningColor);
			}
			g.fillRect(2, emarker.getY(), (getWidth() - 3), errorMarkerHeight);
		}
	}


	public Dimension getPreferredSize() {
		return new Dimension(preferredWidth, preferredHeight);
	}


	public Dimension getMinimumSize() {
		return getPreferredSize();
	}


	public ErrorBar(JavaEditor editor, int height, JavaMode mode) {
		this.editor = editor;
		this.preferredHeight = height;
		this.errorCheckerService = editor.errorCheckerService;

		errorColor = mode.getColor("errorbar.errorcolor"); //, errorColor);
		warningColor = mode.getColor("errorbar.warningcolor"); //, warningColor);
		//backgroundColor = mode.getColor("errorbar.backgroundcolor"); //, backgroundColor);
		backgroundColor = mode.getColor("gutter.bgcolor");

		addListeners();
	}

	/**
	 * Update error markers in the error bar.
	 *
	 * @param problems
	 *            - List of problems.
	 */
	synchronized public void updateErrorPoints(final List<Problem> problems) {
		// NOTE TO SELF: ErrorMarkers are calculated for the present tab only
		// Error Marker index in the arraylist is LOCALIZED for current tab.
		// Also, need to do the update in the UI thread via SwingWorker to prevent
	  // concurrency issues.
		final int fheight = this.getHeight();
		SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

      protected Object doInBackground() throws Exception {
        SketchCode sc = editor.getSketch().getCurrentCode();
        int totalLines = 0, currentTab = editor.getSketch()
            .getCurrentCodeIndex();
        try {
          totalLines = Base.countLines(sc.getDocument()
              .getText(0, sc.getDocument().getLength())) + 1;
        } catch (BadLocationException e) {
          e.printStackTrace();
        }
        // System.out.println("Total lines: " + totalLines);
        synchronized (errorPoints) {
          errorPointsOld.clear();
          for (ErrorMarker marker : errorPoints) {
            errorPointsOld.add(marker);
          }
          errorPoints.clear();

          // Each problem.getSourceLine() will have an extra line added
          // because of
          // class declaration in the beginning as well as default imports
          synchronized (problems) {
            for (Problem problem : problems) {
              if (problem.getTabIndex() == currentTab) {
                // Ratio of error line to total lines
                float y = (problem.getLineNumber() + 1)
                    / ((float) totalLines);
                // Ratio multiplied by height of the error bar
                y *= fheight - 15; // -15 is just a vertical offset
                errorPoints
                    .add(new ErrorMarker(problem, (int) y,
                                         problem.isError() ? ErrorMarker.Error
                                             : ErrorMarker.Warning));
                // System.out.println("Y: " + y);
              }
            }
          }
        }
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
            for (ErrorMarker eMarker : errorPoints) {
              // -2 and +2 are extra allowance, clicks in the
              // vicinity of the markers register that way
              if (e.getY() >= eMarker.getY() - 2
                  && e.getY() <= eMarker.getY() + 2 + errorMarkerHeight) {
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
            for (ErrorMarker eMarker : errorPoints) {
              if (evt.getY() >= eMarker.getY() - 2 &&
                  evt.getY() <= eMarker.getY() + 2 + errorMarkerHeight) {
                Problem p = eMarker.getProblem();
                String msg = (p.isError() ? "Error: " : "Warning: ") + p.getMessage();
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
}
