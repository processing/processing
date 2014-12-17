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

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;

import processing.app.Language;
import static processing.mode.experimental.ExperimentalMode.log;

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
	public static final String[] columnNames = { Language.text("editor.footer.errors.problem"), Language.text("editor.footer.errors.tab"), Language.text("editor.footer.errors.line") };

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
			synchronized public void mouseClicked(MouseEvent e) {
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
			
//			public void mouseMoved(MouseEvent evt) {
//		    log(evt);
////		    String tip = null;
////		    java.awt.Point p = evt.getPoint();
//		    int rowIndex = rowAtPoint(evt.getPoint());
//		    int colIndex = columnAtPoint(evt.getPoint());
//		    synchronized (errorCheckerService.problemsList) {
//		      if (rowIndex < errorCheckerService.problemsList.size()) {
//		        Problem p = errorCheckerService.problemsList.get(rowIndex);
//		        if (p.getImportSuggestions() != null
//		            && p.getImportSuggestions().length > 0) {
//		          log("Import Suggestions available");
//		        }
//		      }
//		    }
////		    return super.getToolTipText(evt);
//		  }
		});
		
		final XQErrorTable thisTable = this; 
		
		this.addMouseMotionListener(new MouseMotionListener() {
      
      @Override
      public void mouseMoved(MouseEvent evt) {
//        log(evt);
//      String tip = null;
//      java.awt.Point p = evt.getPoint();
        int rowIndex = rowAtPoint(evt.getPoint());
//        int colIndex = columnAtPoint(evt.getPoint());
        synchronized (errorCheckerService.problemsList) {
          if (rowIndex < errorCheckerService.problemsList.size()) {
            
            Problem p = errorCheckerService.problemsList.get(rowIndex);
            if (p.getImportSuggestions() != null
                && p.getImportSuggestions().length > 0) {
              String t = p.getMessage() + "(Import Suggestions available)";
              int x1 = thisTable.getFontMetrics(thisTable.getFont())
                  .stringWidth(p.getMessage()), x2 = thisTable
                  .getFontMetrics(thisTable.getFont()).stringWidth(t);
              if(evt.getX() < x1 || evt.getX() > x2) return;
              String[] list = p.getImportSuggestions();
              String className = list[0].substring(list[0].lastIndexOf('.') + 1);
              String[] temp = new String[list.length];
              for (int i = 0; i < list.length; i++) {
                temp[i] = "<html>Import '" +  className + "' <font color=#777777>(" + list[i] + ")</font></html>";
              }
              showImportSuggestion(temp, evt.getXOnScreen(), evt.getYOnScreen() - 3 * thisTable.getFont().getSize());
            }
          }

        }
      }
      
      @Override
      public void mouseDragged(MouseEvent e) {
        
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
		
		ToolTipManager.sharedInstance().registerComponent(this);
	}
	
	/**
	 * Updates table contents with new data
	 * @param tableModel - TableModel
	 * @return boolean - If table data was updated
	 */
	synchronized public boolean updateTable(final TableModel tableModel) {

		// If problems list is not visible, no need to update
		if (!this.isVisible()) {
			return false;
		}

		SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {

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
	JFrame frmImportSuggest;
	private void showImportSuggestion(String list[], int x, int y){
	  if(frmImportSuggest != null) {
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
    
    final DebugEditor editor = errorCheckerService.getEditor();
    classList.addListSelectionListener(new ListSelectionListener() {
      
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (classList.getSelectedValue() != null) {
          try {
            String t = classList.getSelectedValue().trim();
            log(t);
            int x = t.indexOf('(');
            String impString = "import " + t.substring(x + 1, t.indexOf(')')) + ";\n";
            int ct = editor.getSketch().getCurrentCodeIndex();
            editor.getSketch().setCurrentCode(0);
            editor.textArea().getDocument().insertString(0, impString, null);
            editor.getSketch().setCurrentCode(ct);
          } catch (BadLocationException ble) {
            log("Failed to insert import");
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
