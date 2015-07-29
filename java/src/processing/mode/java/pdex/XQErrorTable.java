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

package processing.mode.java.pdex;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;

import processing.app.Base;
import processing.app.Language;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;
import processing.mode.java.JavaEditor;


public class XQErrorTable extends JTable {
  Editor editor;

	static final String[] columnNames = {
	  Language.text("editor.footer.errors.problem"),
	  Language.text("editor.footer.errors.tab"),
	  Language.text("editor.footer.errors.line")
	};

	int[] columnWidths = { 400, 100, 50 };

	/** Is the column being resized? */
	private boolean columnResizing = false;


	public XQErrorTable(final JavaEditor editor) {
	  this.editor = editor;
	  JTableHeader header = getTableHeader();

	  // Try to make things a little less awful until I find time to finish it
	  Font font = Toolkit.getSansFont(12, Font.PLAIN);
	  setFont(font);
	  header.setFont(font);
	  //setBorder(new EmptyBorder(0, Editor.LEFT_GUTTER, 0, 0));  // no effect

	  TableColumnModel columnModel = getColumnModel();
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			columnModel.getColumn(i).setPreferredWidth(columnWidths[i]);
		}

		addMouseListener(new MouseAdapter() {
			@Override
			synchronized public void mouseClicked(MouseEvent e) {
				try {
				  int row = ((XQErrorTable) e.getSource()).getSelectedRow();
					editor.getErrorChecker().scrollToErrorLine(row);
				} catch (Exception e1) {
					Base.log("Exception XQErrorTable mouseReleased " +  e);
				}
			}
		});

		addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent evt) {
        int rowIndex = rowAtPoint(evt.getPoint());

        List<Problem> problemsList = editor.getErrorChecker().problemsList;
        synchronized (problemsList) {
          if (rowIndex < problemsList.size()) {

            Problem p = problemsList.get(rowIndex);
            if (p.getImportSuggestions() != null
                && p.getImportSuggestions().length > 0) {
              String t = p.getMessage() + "(Import Suggestions available)";
              FontMetrics fm = getFontMetrics(getFont());
              int x1 = fm.stringWidth(p.getMessage());
              int x2 = fm.stringWidth(t);
              if (evt.getX() > x1 && evt.getX() < x2) {
                String[] list = p.getImportSuggestions();
                String className = list[0].substring(list[0].lastIndexOf('.') + 1);
                String[] temp = new String[list.length];
                for (int i = 0; i < list.length; i++) {
                  temp[i] = "<html>Import '" +  className + "' <font color=#777777>(" + list[i] + ")</font></html>";
                }
                showImportSuggestion(temp, evt.getXOnScreen(), evt.getYOnScreen() - 3 * getFont().getSize());
              }
            }
          }
        }
      }
    });

		header.setReorderingAllowed(false);

		// Handles the resizing of columns. When mouse press is detected on
		// table header, Stop updating the table, store new values of column
		// widths,and resume updating. Updating is disabled as long as
		// columnResizing is true
		header.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				columnResizing = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				columnResizing = false;
				TableColumnModel columnModel =
				  ((JTableHeader) e.getSource()).getColumnModel();
				for (int i = 0; i < columnModel.getColumnCount(); i++) {
					columnWidths[i] = columnModel.getColumn(i).getWidth();
				}
			}
		});

		ToolTipManager.sharedInstance().registerComponent(this);
	}


	@Override
  public boolean isCellEditable(int rowIndex, int colIndex) {
    return false;  // Disallow the editing of any cell
  }


	/**
	 * Updates table contents with new data
	 * @param tableModel - TableModel
	 * @return boolean - If table data was updated
	 */
	synchronized public boolean updateTable(final TableModel tableModel) {
	  if (!isVisible()) return false;

		SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

			protected Object doInBackground() throws Exception {
				return null;
			}

			protected void done() {
				try {
					setModel(tableModel);

					// Set column widths to user defined widths
					for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
						getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
					}
					getTableHeader().setReorderingAllowed(false);
					validate();
					repaint();
				} catch (Exception e) {
					System.out.println("Exception at XQErrorTable.updateTable " + e);
					// e.printStackTrace();
				}
			}
		};

		try {
			if (!columnResizing) {
				worker.execute();
			}
		} catch (Exception e) {
			System.out.println("ErrorTable updateTable Worker's slacking."
					+ e.getMessage());
			// e.printStackTrace();
		}
		return true;
	}


	JFrame frmImportSuggest;

	private void showImportSuggestion(String[] list, int x, int y) {
	  if (frmImportSuggest != null) {
//	    frmImportSuggest.setVisible(false);
//	    frmImportSuggest = null;
	    return;
	  }
	  final JList<String> classList = new JList<String>(list);
    classList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    frmImportSuggest = new JFrame();

    frmImportSuggest.setUndecorated(true);
    frmImportSuggest.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(Color.WHITE);
    frmImportSuggest.setBackground(Color.WHITE);
    panel.add(classList);
    JLabel label = new JLabel("<html><div alight = \"left\"><font size = \"2\"><br>(Click to insert)</font></div></html>");
    label.setBackground(Color.WHITE);
    label.setHorizontalTextPosition(SwingConstants.LEFT);
    panel.add(label);
    panel.validate();
    frmImportSuggest.getContentPane().add(panel);
    frmImportSuggest.pack();

    classList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (classList.getSelectedValue() != null) {
          try {
            String t = classList.getSelectedValue().trim();
            Base.log(t);
            int x = t.indexOf('(');
            String impString = "import " + t.substring(x + 1, t.indexOf(')')) + ";\n";
            int ct = editor.getSketch().getCurrentCodeIndex();
            editor.getSketch().setCurrentCode(0);
            editor.getTextArea().getDocument().insertString(0, impString, null);
            editor.getSketch().setCurrentCode(ct);
          } catch (BadLocationException ble) {
            Base.log("Failed to insert import");
            ble.printStackTrace();
          }
        }
        frmImportSuggest.setVisible(false);
        frmImportSuggest.dispose();
        frmImportSuggest = null;
      }
    });

    frmImportSuggest.addWindowFocusListener(new WindowFocusListener() {

      @Override
      public void windowLostFocus(WindowEvent e) {
        if (frmImportSuggest != null) {
          frmImportSuggest.dispose();
          frmImportSuggest = null;
        }
      }

      @Override
      public void windowGainedFocus(WindowEvent e) {

      }
    });

    frmImportSuggest.setLocation(x, y);
    frmImportSuggest.setBounds(x, y, 250, 100);
    frmImportSuggest.pack();
    frmImportSuggest.setVisible(true);
	}
}
