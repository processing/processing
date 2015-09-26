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
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import processing.app.Language;
import processing.app.Mode;
import processing.app.ui.Editor;


public class ErrorTable extends JTable {
  Editor editor;

  public interface Entry {
    public boolean isError();
    public boolean isWarning();
  }

	static final String[] columnNames = {
	  "",  // the blank column used for spacing
	  Language.text("editor.footer.errors.problem"),
	  Language.text("editor.footer.errors.tab"),
	  Language.text("editor.footer.errors.line")
	};

	//int[] columnWidths = { Editor.LEFT_GUTTER, 400, 100, 50 };
	//static final int[] DEFAULT_WIDTHS = { Editor.LEFT_GUTTER, 400, 100, 50 };

	static final int DATA_COLUMN = 0;
	static final int PROBLEM_COLUMN = 1;
	static final int TAB_COLUMN = 2;
	static final int LINE_COLUMN = 3;

//	/** Is the column being resized? */
//	private boolean columnResizing = false;

	Font headerFont;
	Color headerColor;
	Color headerBgColor;

	Font rowFont;
	Color rowColor;
	Color rowBgColor;


	public ErrorTable(final Editor editor) {
	  super(new DefaultTableModel(columnNames, 0));

    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	  this.editor = editor;
	  JTableHeader header = getTableHeader();

	  Mode mode = editor.getMode();
    header.setDefaultRenderer(new GradyHeaderRenderer(mode));
    setDefaultRenderer(Object.class, new GradyRowRenderer(mode));
    //setShowGrid(false);
    setIntercellSpacing(new Dimension(0, 0));

    // be specific about the width of the first column
    TableColumn emptyColumn = columnModel.getColumn(0);
    emptyColumn.setMaxWidth(Editor.LEFT_GUTTER);
    emptyColumn.setMinWidth(Editor.LEFT_GUTTER);

    columnModel.getColumn(PROBLEM_COLUMN).setPreferredWidth(400);
    columnModel.getColumn(TAB_COLUMN).setPreferredWidth(100);
    columnModel.getColumn(LINE_COLUMN).setPreferredWidth(50);
//    // the other columns are just a preference
//    for (int i = 1; i < columnModel.getColumnCount(); i++) {
//      columnModel.getColumn(i).setPreferredWidth(columnWidths[i]);
//    }

		addMouseListener(new MouseAdapter() {
      @Override
      synchronized public void mouseClicked(MouseEvent e) {
        try {
          int row = ((ErrorTable) e.getSource()).getSelectedRow();
          if (row >= 0 && row < getRowCount()) {
            Object data = getModel().getValueAt(row, DATA_COLUMN);
            int clickCount = e.getClickCount();
            if (clickCount == 1) {
              editor.errorTableClick(data);
            } else if (clickCount > 1) {
              editor.errorTableDoubleClick(data);
            }
//				  editor.getErrorChecker().scrollToErrorLine(row);
          }
        } catch (Exception ex) {
          ex.printStackTrace();
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
//		header.setResizingAllowed(false);

		/*
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
		*/

		ToolTipManager.sharedInstance().registerComponent(this);
	}


	public void clearRows() {
    DefaultTableModel dtm = (DefaultTableModel) getModel();
    dtm.setRowCount(0);
  }


  public void addRow(Entry data, String message, String filename, String line) {
    DefaultTableModel dtm = (DefaultTableModel) getModel();
    dtm.addRow(new Object[] { data, message, filename, line });
  }


  /*
  public void updateColumns() {
    // figure out the column widths
    TableColumnModel columnModel = getColumnModel();
    int tabWidth = getMaxColumnWidth(this, TAB_COLUMN);
    int lineWidth = getMaxColumnWidth(this, LINE_COLUMN);
    int problemWidth = getWidth() - Editor.LEFT_GUTTER - tabWidth - lineWidth;

    columnModel.getColumn(DATA_COLUMN).setMaxWidth(Editor.LEFT_GUTTER);
    columnModel.getColumn(TAB_COLUMN).setMaxWidth(tabWidth);
    columnModel.getColumn(LINE_COLUMN).setMaxWidth(lineWidth);
    columnModel.getColumn(PROBLEM_COLUMN).setMaxWidth(problemWidth);

//    System.out.println(tabWidth + " " + lineWidth + " " + problemWidth);

//    for (int i = 0; i < columnModel.getColumnCount(); i++) {
//      columnModel.getColumn(i).setPreferredWidth(columnWidths[i]);
//    }
  }


  static private int getMaxColumnWidth(JTable table, int col) {
    TableCellRenderer renderer =
      table.getTableHeader().getDefaultRenderer();
    Component comp =
      renderer.getTableCellRendererComponent(table, columnNames[col],
                                             false, false, 0, col);
    int maxWidth = comp.getPreferredSize().width;

//    TableColumn column = table.getColumnModel().getColumn(col);
//    renderer = column.getCellRenderer();
    renderer = table.getDefaultRenderer(Object.class);
//    System.out.println("renderer is " + renderer);

    for (int row = 0; row < table.getModel().getRowCount(); row++) {
      Object value = table.getModel().getValueAt(row, col);
      comp = renderer.getTableCellRendererComponent(table, value,
                                                    false, false, row, col);
      double valueWidth = comp.getPreferredSize().getWidth();
      maxWidth = (int) Math.max(maxWidth, valueWidth);
    }
    return maxWidth;
  }
 */


	@Override
  public boolean isCellEditable(int rowIndex, int colIndex) {
    return false;  // Disallow the editing of any cell
  }


	/*
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
	*/


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
	  Color bgColorError;
	  Color bgColorWarning;

//	  int indicatorSize;
	  Color errorIndicatorColor;
	  Color warningIndicatorColor;

	  public GradyRowRenderer(Mode mode) {
	    setFont(mode.getFont("errors.row.font"));
	    setAlignmentX(LEFT_ALIGNMENT);

	    textColor = mode.getColor("errors.row.fgcolor");
	    bgColor = mode.getColor("errors.row.bgcolor");
	    textColorSelected = mode.getColor("errors.selection.fgcolor");
	    bgColorSelected = mode.getColor("errors.selection.bgcolor");
	    bgColorError = mode.getColor("errors.selection.error.bgcolor");
	    bgColorWarning = mode.getColor("errors.selection.warning.bgcolor");

//	    indicatorSize = mode.getInteger("errors.indicator.size");
	    errorIndicatorColor = mode.getColor("errors.indicator.error.color");
	    warningIndicatorColor = mode.getColor("errors.indicator.warning.color");

	    setOpaque(true);
	  }

	  @Override
	  public Component getTableCellRendererComponent(JTable table, Object value,
	                                                 boolean selected,
	                                                 boolean focused,
	                                                 int row, int column) {
	    Entry entry = (Entry) table.getValueAt(row, DATA_COLUMN);

	    if (selected) {
	      setForeground(textColorSelected);
	      if (entry.isError()) {
	        setBackground(bgColorError);
	      } else if (entry.isWarning()) {
	        setBackground(bgColorWarning);
	      } else {
	        setBackground(bgColorSelected);
	      }
	    } else {
	      setForeground(textColor);
	      setBackground(bgColor);
	    }
	    if (column == DATA_COLUMN) {
	      setText("\u2022");
	      setHorizontalAlignment(SwingConstants.CENTER);
	      if (entry.isError()) {
	        setForeground(errorIndicatorColor);
	      } else if (entry.isWarning()) {
	        setForeground(warningIndicatorColor);
	      } else {
	        setText("");  // no dot
	      }
	    } else if (value == null) {
	      setText("");
	    } else {
        setHorizontalAlignment(SwingConstants.LEFT);
	      setText(value.toString());
	    }
	    return this;
	  }

//	  Component dot = new JComponent() {
//	    @Override
//	    public void paintComponent(Graphics g) {
//
//	    }
//    };
	}
}
