/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-15 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along
  with this program; if not, write to the Free Software Foundation, Inc.
  59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package processing.app.contrib;

import java.awt.*;
import java.util.List;
import java.util.*;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.*;
import javax.swing.table.*;

import processing.app.Base;
import processing.app.ui.Toolkit;


// The "Scrollable" implementation and its methods here take care of preventing
// the scrolling area from running exceptionally slowly. Not sure why they're
// necessary in the first place, however; seems like odd behavior.
// It also allows the description text in the panels to wrap properly.

public class ListPanel extends JPanel
implements ContributionListing.ChangeListener {
  ContributionTab contributionTab;
  TreeMap<Contribution, DetailPanel> panelByContribution = new TreeMap<Contribution, DetailPanel>(ContributionListing.COMPARATOR);
  Set<Contribution> visibleContributions = new TreeSet<Contribution>(ContributionListing.COMPARATOR);

  private DetailPanel selectedPanel;
  protected Contribution.Filter filter;
  protected ContributionListing contribListing = ContributionListing.getInstance();
  protected JTable table;
  DefaultTableModel model;
  JScrollPane scrollPane;

  static Icon upToDateIcon;
  static Icon updateAvailableIcon;
  static Icon incompatibleIcon;
  static Icon foundationIcon;

  static Font plainFont;
  static Font boldFont;
  static Font headerFont;

  // Should this be in theme.txt? Of course! Is it? No.
  static final Color HEADER_BGCOLOR = new Color(0xffEBEBEB);


  public ListPanel() {
    if (upToDateIcon == null) {
      upToDateIcon = Toolkit.getLibIconX("manager/up-to-date");
      updateAvailableIcon = Toolkit.getLibIconX("manager/update-available");
      incompatibleIcon = Toolkit.getLibIconX("manager/incompatible");
      foundationIcon = Toolkit.getLibIconX("icons/foundation", 16);

      plainFont = Toolkit.getSansFont(14, Font.PLAIN);
      boldFont = Toolkit.getSansFont(14, Font.BOLD);
      headerFont = Toolkit.getSansFont(12, Font.PLAIN);
    }
  }


  public ListPanel(final ContributionTab contributionTab,
                   Contribution.Filter filter) {
    this.contributionTab = contributionTab;
    this.filter = filter;

    setLayout(new GridBagLayout());
    setOpaque(true);
    setBackground(Color.WHITE);
    model = new ContribTableModel();
    table = new JTable(model) {
      @Override
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (isRowSelected(row)) {
          c.setBackground(new Color(0xe0fffd));
        } else {
          c.setBackground(Color.white);
        }
        return c;
      }
    };

    // There is a space before Status
    String[] colName = { " Status", "Name", "Author" };
    model.setColumnIdentifiers(colName);
    scrollPane = new JScrollPane(table);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    table.setFillsViewportHeight(true);
    table.setDefaultRenderer(Contribution.class, new ContribStatusRenderer());
    table.setFont(plainFont);
    table.setRowHeight(28);
    table.setRowMargin(6);
    table.getColumnModel().setColumnMargin(0);
    table.getColumnModel().getColumn(0).setMaxWidth(ManagerFrame.STATUS_WIDTH);
    table.getColumnModel().getColumn(2).setMinWidth(ManagerFrame.AUTHOR_WIDTH);
    table.getColumnModel().getColumn(2).setMaxWidth(ManagerFrame.AUTHOR_WIDTH);
    table.setShowGrid(false);
    table.setColumnSelectionAllowed(false);
    table.setCellSelectionEnabled(false);
    table.setAutoCreateColumnsFromModel(true);
    table.setAutoCreateRowSorter(false);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent event) {
          //TODO this executes 2 times when clicked and 1 time when traversed using arrow keys
          //Ideally this should always be true but while clearing the table something fishy is going on
          if (table.getSelectedRow() != -1) {
            setSelectedPanel(panelByContribution.get(table.getValueAt(table
              .getSelectedRow(), 0)));
            // Preventing the focus to move out of filterField after typing every character
            if (!contributionTab.filterField.hasFocus()) {
              table.requestFocusInWindow();
            }
          }
        }
      });

    TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
    table.setRowSorter(sorter);
    sorter.setComparator(1, ContributionListing.COMPARATOR);
    sorter.setComparator(2, new Comparator<Contribution>() {

      @Override
      public int compare(Contribution o1, Contribution o2) {
        return Toolkit.toPlainText(o1.getAuthorList())
          .compareTo(Toolkit.toPlainText(o2.getAuthorList()));
      }
    });
    sorter.setComparator(0, new Comparator<Contribution>() {

      @Override
      public int compare(Contribution o1, Contribution o2) {
        int pos1 = 0;
        if (o1.isInstalled()) {
          pos1 = 1;
          if (contribListing.hasUpdates(o1)) {
            pos1 = 2;
          }
          if (!o1.isCompatible(Base.getRevision())) {
            pos1 = 3;
          }
        } else {
          pos1 = 4;
        }
        int pos2 = 0;
        if (o2.isInstalled()) {
          pos2 = 1;
          if (contribListing.hasUpdates(o2)) {
            pos2 = 2;
          }
          if (!o2.isCompatible(Base.getRevision())) {
            pos2 = 3;
          }
        } else {
          pos2 = 4;
        }
        return pos1 - pos2;
      }
    });
    table.getTableHeader().setDefaultRenderer(new ContribHeaderRenderer());

    GroupLayout layout = new GroupLayout(this);
    layout.setHorizontalGroup(layout.createParallelGroup().addComponent(scrollPane));
    layout.setVerticalGroup(layout.createSequentialGroup().addComponent(scrollPane));

    this.setLayout(layout);
    table.setVisible(true);
  }


  class ContribHeaderRenderer extends DefaultTableCellRenderer {

    public ContribHeaderRenderer() {
      setHorizontalTextPosition(LEFT);
      setOpaque(true);
    }

    /**
     * Returns the default table header cell renderer.
     * <P>
     * If the column is sorted, the appropriate icon is retrieved from the
     * current Look and Feel, and a border appropriate to a table header cell
     * is applied.
     * <P>
     * Subclasses may override this method to provide custom content or
     * formatting.
     *
     * @param table the <code>JTable</code>.
     * @param value the value to assign to the header cell
     * @param isSelected This parameter is ignored.
     * @param hasFocus This parameter is ignored.
     * @param row This parameter is ignored.
     * @param column the column of the header cell to render
     * @return the default table header cell renderer
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value,
              isSelected, hasFocus, row, column);

      JTableHeader tableHeader = table.getTableHeader();
      if (tableHeader != null) {
        setForeground(tableHeader.getForeground());
      }
      setFont(headerFont);
      setIcon(getSortIcon(table, column));
      setBackground(HEADER_BGCOLOR);
//      if (column % 2 == 0) {
//        setBackground(new Color(0xdfdfdf));
//      } else {
//        setBackground(new Color(0xebebeb));
//      }
      setBorder(null);
      return this;
    }

    /**
     * Overloaded to return an icon suitable to the primary sorted column, or null if
     * the column is not the primary sort key.
     *
     * @param table the <code>JTable</code>.
     * @param column the column index.
     * @return the sort icon, or null if the column is unsorted.
     */
    protected Icon getSortIcon(JTable table, int column) {
      SortKey sortKey = getSortKey(table, column);
      if (sortKey != null && table.convertColumnIndexToView(sortKey.getColumn()) == column) {
        switch (sortKey.getSortOrder()) {
          case ASCENDING:
            return UIManager.getIcon("Table.ascendingSortIcon");
          case DESCENDING:
            return UIManager.getIcon("Table.descendingSortIcon");
        }
      }
      return null;
    }

    /**
     * Returns the current sort key, or null if the column is unsorted.
     *
     * @param table the table
     * @param column the column index
     * @return the SortKey, or null if the column is unsorted
     */
    protected SortKey getSortKey(JTable table, int column) {
      RowSorter rowSorter = table.getRowSorter();
      if (rowSorter == null) {
        return null;
      }

      List sortedColumns = rowSorter.getSortKeys();
      if (sortedColumns.size() > 0) {
        return (SortKey) sortedColumns.get(0);
      }
      return null;
    }
  }


  private class ContribStatusRenderer extends DefaultTableCellRenderer {

    @Override
    public void setVerticalAlignment(int alignment) {
      super.setVerticalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus, int row,
                                                   int column) {
      Contribution contribution = (Contribution) value;
      JLabel label = new JLabel();
      if (value == null) {
        // Working on https://github.com/processing/processing/issues/3667
        //System.err.println("null value seen in getTableCellRendererComponent()");
        // TODO this is now working, but the underlying issue is not fixed
        return label;
      }
      if (column == 0) {
        Icon icon = null;
        label.setFont(plainFont);
        if (contribution.isInstalled()) {
          icon = upToDateIcon;
          if (contribListing.hasUpdates(contribution)) {
            icon = updateAvailableIcon;
          }
          if (!contribution.isCompatible(Base.getRevision())) {
            icon = incompatibleIcon;
          }
        }
        label.setIcon(icon);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        if (isSelected) {
          label.setBackground(new Color(0xe0fffd));
        }
        label.setOpaque(true);
//        return table.getDefaultRenderer(Icon.class).getTableCellRendererComponent(table, icon, isSelected, false, row, column);

      } else if (column == 1) {
        // Generating ellipses based on fontMetrics
        String fontFace = "<font face=\"" + boldFont.getName() + "\">";
        FontMetrics fontMetrics = table.getFontMetrics(boldFont); //table.getFont());
        int colSize = table.getColumnModel().getColumn(1).getWidth();
        String sentence = Toolkit.toPlainText(contribution.getSentence());
        //int currentWidth = table.getFontMetrics(table.getFont().deriveFont(Font.BOLD)).stringWidth(contribution.getName() + " | ");
        int currentWidth = table.getFontMetrics(boldFont).stringWidth(contribution.getName() + " | ");
        int ellipsesWidth = fontMetrics.stringWidth("...");
        //String name = "<html><body><b>" + contribution.getName();
        String name = "<html><body>" + fontFace + contribution.getName();
        if (sentence == null) {
          label.setText(name + "</font></body></html>");
        } else {
          sentence = " | </font>" + sentence;
          currentWidth += ellipsesWidth;
          int i = 0;
          for (i = 0; i < sentence.length(); i++) {
            currentWidth += fontMetrics.charWidth(sentence.charAt(i));
            if (currentWidth >= colSize) {
              break;
            }
          }
          // Adding ellipses only if text doesn't fits into the column
          if(i != sentence.length()){
            label.setText(name + sentence.substring(0, i) + "...</body></html>");
          }else {
            label.setText(name + sentence + "</body></html>");
          }
        }
        if (!contribution.isCompatible(Base.getRevision())) {
          label.setForeground(Color.LIGHT_GRAY);
        }
        if (table.isRowSelected(row)) {
          label.setBackground(new Color(0xe0fffd));
        }
        label.setFont(plainFont);
        label.setOpaque(true);
      } else {
        if (contribution.isSpecial()) {
          label = new JLabel(foundationIcon);
        } else {
          label = new JLabel();
        }
        String authorList = contribution.getAuthorList();
        String name = Toolkit.toPlainText(authorList);
        label.setText(name.toString());
        label.setHorizontalAlignment(SwingConstants.LEFT);
        if(!contribution.isCompatible(Base.getRevision())){
          label.setForeground(Color.LIGHT_GRAY);
        }else{
          label.setForeground(Color.BLACK);
        }
        if (table.isRowSelected(row)) {
          label.setBackground(new Color(0xe0fffd));
        }
        label.setFont(Toolkit.getSansFont(14, Font.BOLD));
        label.setOpaque(true);
      }
      return label;
    }
  }


  static private class ContribTableModel extends DefaultTableModel {
    @Override
    public boolean isCellEditable(int row, int column) {
      return false;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return Contribution.class;
    }
  }

  void updatePanelOrdering(Set<Contribution> contributionsSet) {
    model.getDataVector().removeAllElements();
    model.fireTableDataChanged();
    int rowCount = 0;
    synchronized (contributionsSet) {
      for (Contribution entry : contributionsSet) {
        model.addRow(new Object[]{entry, entry, entry});
        if (selectedPanel != null &&
            entry.getName().equals(selectedPanel.getContrib().getName())) {
          table.setRowSelectionInterval(rowCount, rowCount);
        }
        rowCount++;
      }
    }
  }


  public void contributionAdded(final Contribution contribution) {
    if (filter.matches(contribution)) {
      // TODO: this should already be on EDT, check it [jv]
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          if (!panelByContribution.containsKey(contribution)) {
            DetailPanel newPanel =
              new DetailPanel(ListPanel.this);
            synchronized (panelByContribution) {
              panelByContribution.put(contribution, newPanel);
            }
            synchronized (visibleContributions) {
              visibleContributions.add(contribution);
            }
            if (newPanel != null) {
              newPanel.setContribution(contribution);
              updatePanelOrdering(visibleContributions);
            }
          }
        }
      });
    }
  }


  public void contributionRemoved(final Contribution contribution) {
    // TODO: this should already be on EDT, check it [jv]
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        synchronized (panelByContribution) {
          DetailPanel panel = panelByContribution.get(contribution);
          if (panel != null) {
            panelByContribution.remove(contribution);
          }
        }
        synchronized (visibleContributions) {
          visibleContributions.remove(contribution);
        }
        updatePanelOrdering(visibleContributions);
      }
    });
  }


  public void contributionChanged(final Contribution oldContrib,
                                  final Contribution newContrib) {
    // TODO: this should already be on EDT, check it [jv]
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        synchronized (panelByContribution) {
          DetailPanel panel = panelByContribution.get(oldContrib);
          if (panel == null) {
            contributionAdded(newContrib);
          } else {
            panelByContribution.remove(oldContrib);
            panel.setContribution(newContrib);
            panelByContribution.put(newContrib, panel);
          }
        }
        synchronized (visibleContributions) {
          if (visibleContributions.contains(oldContrib)) {
            visibleContributions.remove(oldContrib);
            visibleContributions.add(newContrib);
          }
          updatePanelOrdering(visibleContributions);
        }
      }
    });
  }


  public void filterLibraries(List<Contribution> filteredContributions) {
    synchronized (visibleContributions) {
      visibleContributions.clear();
      for (Contribution contribution : filteredContributions) {
        if (contribution.getType() == contributionTab.contribType) {
          visibleContributions.add(contribution);
        }
      }
      updatePanelOrdering(visibleContributions);
    }
  }


  protected void setSelectedPanel(DetailPanel contributionPanel) {
    contributionTab.updateStatusPanel(contributionPanel);
    selectedPanel = contributionPanel;
  }


  protected DetailPanel getSelectedPanel() {
    return selectedPanel;
  }

  public int getRowCount() {
    return panelByContribution.size();
  }
}
