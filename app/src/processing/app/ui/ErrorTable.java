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

package processing.app.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import processing.app.Language;
import processing.app.Messages;
import processing.app.Mode;
import processing.app.ui.Editor;


public class ErrorTable extends JTable {
  Editor editor;

	static final String[] columnNames = {
	  "",
	  Language.text("editor.footer.errors.problem"),
	  Language.text("editor.footer.errors.tab"),
	  Language.text("editor.footer.errors.line")
	};

	int[] columnWidths = { Editor.LEFT_GUTTER, 400, 100, 50 };

	/** Is the column being resized? */
	private boolean columnResizing = false;

	Font headerFont;
	Color headerColor;
	Color headerBgColor;

	Font rowFont;
	Color rowColor;
	Color rowBgColor;


	public ErrorTable(final Editor editor) {
	  super(new DefaultTableModel(columnNames, 0));

	  this.editor = editor;
	  JTableHeader header = getTableHeader();

	  Mode mode = editor.getMode();
    header.setDefaultRenderer(new GradyHeaderRenderer(mode));
    setDefaultRenderer(Object.class, new GradyRowRenderer(mode));
    //setShowGrid(false);
    setIntercellSpacing(new Dimension(0, 0));

    // this did nothing, no columns existed yet
    /*
	  TableColumnModel columnModel = getColumnModel();
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			columnModel.getColumn(i).setPreferredWidth(columnWidths[i]);
			//System.out.println("class is " + columnModel.getColumn(i).getClass());
		}
		*/
//    DefaultTableModel tm = new DefaultTableModel(columnNames, 0);

		addMouseListener(new MouseAdapter() {
			@Override
			synchronized public void mouseClicked(MouseEvent e) {
				try {
				  int row = ((ErrorTable) e.getSource()).getSelectedRow();
				  Object data = getModel().getValueAt(row, 0);
				  int clickCount = e.getClickCount();
				  if (clickCount == 1) {
				    editor.errorTableClick(data);
				  } else if (clickCount > 1) {
				    editor.errorTableDoubleClick(data);
				  }
//					editor.getErrorChecker().scrollToErrorLine(row);
				} catch (Exception e1) {
					Messages.log("Exception XQErrorTable mouseReleased " +  e);
				}
			}
		});

		/*
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
    */

		header.setReorderingAllowed(false);

		// Handles the resizing of columns. When mouse press is detected on
		// table header, Stop updating the table, store new values of column
		// widths, and resume updating. Updating is disabled as long as
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


	public void clearRows() {
    DefaultTableModel dtm = (DefaultTableModel) getModel();
    dtm.setRowCount(0);
  }


  public void addRow(Object data, String message, String filename, String line) {
    DefaultTableModel dtm = (DefaultTableModel) getModel();
    dtm.addRow(new Object[] { data, message, filename, line });
  }


	@Override
  public boolean isCellEditable(int rowIndex, int colIndex) {
    return false;  // Disallow the editing of any cell
  }


	/**
	 * Updates table contents with new data
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


	// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


	static class GradyHeaderRenderer extends JLabel implements TableCellRenderer {

	  public GradyHeaderRenderer(Mode mode) {
	    setFont(mode.getFont("errors.header.font"));
	    setAlignmentX(LEFT_ALIGNMENT);

	    setForeground(mode.getColor("errors.header.fgcolor"));
	    setBackground(mode.getColor("errors.header.bgcolor"));
	    setOpaque(true);
	  }

	  @Override
	  public Component getTableCellRendererComponent(JTable table, Object value,
	                                                 boolean selected,
	                                                 boolean focused,
	                                                 int row, int column) {
	    setText(value == null ? "" : value.toString());
	    return this;
	  }
	}


	// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


	static class GradyRowRenderer extends JLabel implements TableCellRenderer {
	  Color textColor;
	  Color bgColor;
	  Color textColorSelected;
	  Color bgColorSelected;

	  public GradyRowRenderer(Mode mode) {
	    setFont(mode.getFont("errors.row.font"));
	    setAlignmentX(LEFT_ALIGNMENT);

	    textColor = mode.getColor("errors.row.fgcolor");
	    bgColor = mode.getColor("errors.row.bgcolor");
	    textColorSelected = mode.getColor("errors.selection.fgcolor");
	    bgColorSelected = mode.getColor("errors.selection.bgcolor");
	    setOpaque(true);
	  }

	  @Override
	  public Component getTableCellRendererComponent(JTable table, Object value,
	                                                 boolean selected,
	                                                 boolean focused,
	                                                 int row, int column) {
	    setBackground(Color.RED);
	    if (selected) {
	      setForeground(textColorSelected);
	      setBackground(bgColorSelected);
	    } else {
	      setForeground(textColor);
	      setBackground(bgColor);
	    }
	    if (column == 0 || value == null) {
	      setText("");
	    } else {
	      setText(value.toString());
	    }
	    return this;
	  }
	}
}
