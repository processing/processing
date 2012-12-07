package processing.mode.experimental;

/*
 Part of the XQMode project - https://github.com/Manindra29/XQMode

 Under Google Summer of Code 2012 - 
 http://www.google-melange.com/gsoc/homepage/google/gsoc2012

 Copyright (C) 2012 Manindra Moharana

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License version 2
 as published by the Free Software Foundation.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

/**
 * Custom JTable implementation for XQMode. Minor tweaks and addtions.
 * 
 * @author Manindra Moharana &lt;me@mkmoharana.com&gt;
 * 
 */
public class XQErrorTable extends JTable {

	/**
	 * Column Names of JTable
	 */
	public static final String[] columnNames = { "Problem", "Tab", "Line" };

	/**
	 * Column Widths of JTable.
	 */
	public int[] columnWidths = { 600, 100, 50 }; // Default Values

	/**
	 * Is the column being resized?
	 */
	private boolean columnResizing = false;

	/**
	 * ErrorCheckerService instance
	 */
	protected ErrorCheckerService errorCheckerService;

	@Override
	public boolean isCellEditable(int rowIndex, int colIndex) {
		return false; // Disallow the editing of any cell
	}

	public XQErrorTable(final ErrorCheckerService errorCheckerService) {
		this.errorCheckerService = errorCheckerService;
		for (int i = 0; i < this.getColumnModel().getColumnCount(); i++) {
			this.getColumnModel().getColumn(i)
					.setPreferredWidth(columnWidths[i]);
		}

		this.getTableHeader().setReorderingAllowed(false);

		this.addMouseListener(new MouseAdapter() {
			@Override
			synchronized public void mouseReleased(MouseEvent e) {
				try {
					errorCheckerService.scrollToErrorLine(((XQErrorTable) e
							.getSource()).getSelectedRow());
					// System.out.print("Row clicked: "
					// + ((XQErrorTable) e.getSource()).getSelectedRow());
				} catch (Exception e1) {
					System.out.println("Exception XQErrorTable mouseReleased "
							+ e);
				}
			}
		});

		// Handles the resizing of columns. When mouse press is detected on
		// table header, Stop updating the table, store new values of column
		// widths,and resume updating. Updating is disabled as long as
		// columnResizing is true
		this.getTableHeader().addMouseListener(new MouseAdapter() {
			
			@Override
			public void mousePressed(MouseEvent e) {
				columnResizing = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				columnResizing = false;
				for (int i = 0; i < ((JTableHeader) e.getSource())
						.getColumnModel().getColumnCount(); i++) {
					columnWidths[i] = ((JTableHeader) e.getSource())
							.getColumnModel().getColumn(i).getWidth();
					// System.out.println("nw " + columnWidths[i]);
				}
			}
		});
	}

	
	/**
	 * Updates table contents with new data
	 * @param tableModel - TableModel
	 * @return boolean - If table data was updated
	 */
	@SuppressWarnings("rawtypes")
	synchronized public boolean updateTable(final TableModel tableModel) {

		// If problems list is not visible, no need to update
		if (!this.isVisible()) {
			return false;
		}

		SwingWorker worker = new SwingWorker() {

			protected Object doInBackground() throws Exception {
				return null;
			}

			protected void done() {

				try {
					setModel(tableModel);
					
					// Set column widths to user defined widths
					for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
						getColumnModel().getColumn(i).setPreferredWidth(
								columnWidths[i]);
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

}
