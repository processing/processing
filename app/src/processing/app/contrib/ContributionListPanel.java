/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013 The Processing Foundation
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
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.table.*;

import processing.app.Base;
import processing.app.Platform;
import processing.app.ui.Toolkit;


// The "Scrollable" implementation and its methods here take care of preventing
// the scrolling area from running exceptionally slowly. Not sure why they're
// necessary in the first place, however; seems like odd behavior.
// It also allows the description text in the panels to wrap properly.

public class ContributionListPanel extends JPanel implements Scrollable, ContributionChangeListener {

  ContributionTab contributionTab;
  TreeMap<Contribution, ContributionPanel> panelByContribution;

  static HyperlinkListener nullHyperlinkListener = new HyperlinkListener() {
    public void hyperlinkUpdate(HyperlinkEvent e) { }
  };

  private ContributionPanel selectedPanel;
//  protected JPanel statusPlaceholder;
//  private StatusPanel status;
  protected ContributionFilter filter;
//  private ContributionListing contribListing;
  protected ContributionListing contribListing = ContributionListing.getInstance();
  protected JTable table;
  DefaultTableModel dtm;
  JScrollPane scrollPane;
  Font myFont;

  public ContributionListPanel() {
    // TODO Auto-generated constructor stub
  }
  public ContributionListPanel(final ContributionTab contributionTab,
                               ContributionFilter filter) {
    super();
    this.contributionTab = contributionTab;
    this.filter = filter;

//    contribListing = ContributionListing.getInstance();

    setLayout(new GridBagLayout());
    setOpaque(true);
    setBackground(Color.WHITE);

    panelByContribution = new TreeMap<Contribution, ContributionPanel>(contribListing.getComparator());

//    statusPlaceholder = new JPanel();
//    statusPlaceholder.setVisible(false);
//    status = new StatusPanel(null);


    dtm = new MyTableModel();
    table = new JTable(dtm){
      @Override
      public Component prepareRenderer(
              TableCellRenderer renderer, int row, int column) {
          Component c = super.prepareRenderer(renderer, row, column);
          if (isRowSelected(row)) {
              c.setBackground(new Color(0xe0fffd));
          } else {
              c.setBackground(Color.white);
          }
          return c;
      }
    };

    myFont = Toolkit.getSansFont(14, Font.PLAIN);
    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(myFont);
    
    // There is a space before Status
    String[] colName = { " Status", "Name", "Author" };
    dtm.setColumnIdentifiers(colName);
    scrollPane = new JScrollPane(table);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    table.setFillsViewportHeight(true);
//    table.setBorder();
    table.setDefaultRenderer(Contribution.class, new StatusRendere());
    table.setFont(Toolkit.getSansFont(14, Font.PLAIN));
    table.setRowHeight(28);
    table.setRowMargin(6);
    table.getColumnModel().setColumnMargin(0);
    table.getColumnModel().getColumn(0).setMaxWidth(60);
    table.getColumnModel().getColumn(2).setMinWidth(ContributionManagerDialog.AUTHOR_WIDTH);
    table.getColumnModel().getColumn(2).setMaxWidth(ContributionManagerDialog.AUTHOR_WIDTH);
    table.setShowGrid(false);
    table.setColumnSelectionAllowed(false);
    table.setCellSelectionEnabled(false);
    table.setAutoCreateColumnsFromModel(true);
    table.setAutoCreateRowSorter(false);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getSelectionModel()
      .addListSelectionListener(new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent event) {
          //TODO this executes 2 times when clicked and 1 time when traversed using arrow keys
          //Ideally this should always be true but while clearing the table something fishy is going on
          if (table.getSelectedRow() != -1) {
            setSelectedPanel(panelByContribution.get(table.getValueAt(table
              .getSelectedRow(), 0)));
          }
        }
      });

    TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
    table.setRowSorter(sorter);
    sorter.setComparator(1, contribListing.getComparator());
    sorter.setSortable(0, false);
    sorter.setSortable(2, false);
    table.getTableHeader().setDefaultRenderer(new MyColumnHeaderRenderer());

    GroupLayout layout = new GroupLayout(this);
    layout.setHorizontalGroup(layout.createParallelGroup().addComponent(scrollPane));
    layout.setVerticalGroup(layout.createSequentialGroup().addComponent(scrollPane));

    this.setLayout(layout);
    table.setVisible(true);

  }

  class MyColumnHeaderRenderer extends DefaultTableCellRenderer {

    /**
     * Constructs a <code>DefaultTableHeaderCellRenderer</code>.
     * <P>
     * The horizontal alignment and text position are set as appropriate to a
     * table header cell, and the opaque property is set to false.
     */
    public MyColumnHeaderRenderer() {
//      setHorizontalAlignment(CENTER);
      setHorizontalTextPosition(LEFT);
//      setVerticalAlignment(BOTTOM);
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
      setIcon(getIcon(table, column));
      if (column % 2 == 0) {
        setBackground(new Color(0xdfdfdf));
      } else {
        setBackground(new Color(0xebebeb));
      }
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
    protected Icon getIcon(JTable table, int column) {
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

  class StatusRendere extends DefaultTableCellRenderer {

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
      if (column == 0) {
        Icon icon = null;
        label.setBorder(BorderFactory.createEmptyBorder(2, 17, 0, 0));
        label.setFont(Toolkit.getSansFont(14, Font.PLAIN));
        if (contribution.isInstalled()) {
          icon = Toolkit.getLibIcon("manager/up-to-date.png");
          if (contribListing.hasUpdates(contribution)) {
            icon = Toolkit.getLibIcon("manager/update-available.png");
          }
          if (!contribution.isCompatible(Base.getRevision())) {
            icon = Toolkit.getLibIcon("manager/incompatible.png");
          }
        }
        label.setIcon(icon);
        if (isSelected) {
          label.setBackground(new Color(0xe0fffd));
        }
        label.setOpaque(true);
//        return table.getDefaultRenderer(Icon.class).getTableCellRendererComponent(table, icon, isSelected, false, row, column);
      } else if (column == 1) {
        // Generating ellipses based on fontMetrics
        FontMetrics fontMetrics = table.getFontMetrics(table.getFont());
        int colSize = table.getColumnModel().getColumn(1).getWidth();
        String sentence = contribution.getSentence();
        int currentWidth = table.getFontMetrics(table.getFont().deriveFont(Font.BOLD)).stringWidth(contribution.getName() + " | ");
        int ellipsesWidth = fontMetrics.stringWidth("...");
        String name = "<html><body><b>" + contribution.getName();
        if (sentence == null) {
          label.setText(name + "</b></body></html>");
        } else {
          sentence = " | </b>" + sentence;
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
        label.setFont(myFont);
        label.setOpaque(true);
      } else {
        label = new JLabel(
                           contribution.isSpecial() ? Toolkit
                             .getLibIcon("icons/pde-16.png") : null);
        StringBuilder name = new StringBuilder("");
        String authorList = contribution.getAuthorList();
        if (authorList != null) {
          for (int i = 0; i < authorList.length(); i++) {

            if (authorList.charAt(i) == '[' || authorList.charAt(i) == ']') {
              continue;
            }
            if (authorList.charAt(i) == '(') {
              i++;
              while (authorList.charAt(i) != ')') {
                i++;
              }
            } else {
              name.append(authorList.charAt(i));
            }
          }
        }
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
  private class MyTableModel extends DefaultTableModel{
    MyTableModel() {
      super(0,0);
    }
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
    /*   int row = 0;
    for (Entry<Contribution, ContributionPanel> entry : panelByContribution.entrySet()) {
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1;
      c.gridx = 0;
      c.gridy = row++;
      c.anchor = GridBagConstraints.NORTH;

      add(entry.getValue(), c);
    }

    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1;
    c.weighty = 1;
    c.gridx = 0;
    c.gridy = row++;
    c.anchor = GridBagConstraints.NORTH;
    add(status, c);*/
//    System.out.println(dtm.getDataVector());
    int row = table.getSelectedRow();
    dtm.getDataVector().removeAllElements();
    dtm.fireTableDataChanged();
    for (Contribution entry : contributionsSet) {
      ((MyTableModel) table.getModel()).addRow(new Object[] {
        entry, entry, entry });
    }
    if (row >= 0) {
      table.setRowSelectionInterval(row, row);
    }

  }


  public void contributionAdded(final Contribution contribution) {
    if (filter.matches(contribution)) {
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          if (!panelByContribution.containsKey(contribution)) {
            ContributionPanel newPanel = new ContributionPanel(ContributionListPanel.this);
            synchronized (panelByContribution) {
              panelByContribution.put(contribution, newPanel);
            }
            if (newPanel != null) {
              newPanel.setContribution(contribution);
              add(newPanel);
              updatePanelOrdering(panelByContribution.keySet());
              updateColors();  // XXX this is the place
            }
          }
        }
      });
    }
  }


  public void contributionRemoved(final Contribution contribution) {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        synchronized (panelByContribution) {
          ContributionPanel panel = panelByContribution.get(contribution);
          if (panel != null) {
            remove(panel);
            panelByContribution.remove(contribution);
          }
        }
        updatePanelOrdering(panelByContribution.keySet());
        updateColors();
        updateUI();
      }
    });
  }


  public void contributionChanged(final Contribution oldContrib,
                                  final Contribution newContrib) {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        synchronized (panelByContribution) {
          ContributionPanel panel = panelByContribution.get(oldContrib);
          if (panel == null) {
            contributionAdded(newContrib);
          } else {
            panelByContribution.remove(oldContrib);
            panel.setContribution(newContrib);
            panelByContribution.put(newContrib, panel);
            updatePanelOrdering(panelByContribution.keySet());
          }
        }
      }
    });
  }


  public void filterLibraries(List<Contribution> filteredContributions) {
    synchronized (panelByContribution) {
      /*Set<Contribution> hiddenPanels =
        new TreeSet<Contribution>(contribListing.getComparator());
      hiddenPanels.addAll(panelByContribution.keySet());

      for (Contribution info : filteredContributions) {
        ContributionPanel panel = panelByContribution.get(info);
        if (panel != null) {
          panel.setVisible(true);
          hiddenPanels.remove(info);
        }
      }

      for (Contribution info : hiddenPanels) {
        ContributionPanel panel = panelByContribution.get(info);
        if (panel != null) {
          panel.setVisible(false);
        }
      }*/
      TreeSet<Contribution> panelInThisTab = new TreeSet<Contribution>(contribListing.getComparator());
      for (Contribution contribution : filteredContributions) {
        if(contribution.getType() == this.contributionTab.contributionType){
          panelInThisTab.add(contribution);
        }
      }
      updatePanelOrdering(panelInThisTab);
    }
  }


  protected void setSelectedPanel(ContributionPanel contributionPanel) {
    contributionTab.updateStatusPanel(contributionPanel);
    if (selectedPanel == contributionPanel) {
      selectedPanel.setSelected(true);

    } else {

      ContributionPanel lastSelected = selectedPanel;
      selectedPanel = contributionPanel;

      if (lastSelected != null) {
        lastSelected.setSelected(false);
      }
      contributionPanel.setSelected(true);

      updateColors();
      requestFocusInWindow();
    }
  }


  protected ContributionPanel getSelectedPanel() {
    return selectedPanel;
  }


  /**
   * Updates the colors of all library panels that are visible.
   */
  protected void updateColors() {
    int count = 0;
    synchronized (panelByContribution) {
      for (Entry<Contribution, ContributionPanel> entry : panelByContribution.entrySet()) {
        ContributionPanel panel = entry.getValue();

        if (panel.isVisible() && panel.isSelected()) {
          panel.setBackground(UIManager.getColor("List.selectionBackground"));
          panel.setForeground(UIManager.getColor("List.selectionForeground"));
          panel.setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
          count++;

        } else {
          Border border = null;
          if (panel.isVisible()) {
            if (Platform.isMacOS()) {
              if (count % 2 == 1) {
                border = UIManager.getBorder("List.oddRowBackgroundPainter");
              } else {
                border = UIManager.getBorder("List.evenRowBackgroundPainter");
              }
            } else {
              if (count % 2 == 1) {
                panel.setBackground(new Color(219, 224, 229));
              } else {
                panel.setBackground(new Color(241, 241, 241));
              }
            }
            count++;
          }

          if (border == null) {
            border = BorderFactory.createEmptyBorder(1, 1, 1, 1);
          }
          panel.setBorder(border);
          panel.setForeground(UIManager.getColor("List.foreground"));
        }
      }
    }
  }


  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }


  /**
   * Amount to scroll to reveal a new page of items
   */
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.VERTICAL) {
      int blockAmount = visibleRect.height;
      if (direction > 0) {
        visibleRect.y += blockAmount;
      } else {
        visibleRect.y -= blockAmount;
      }

      blockAmount += getScrollableUnitIncrement(visibleRect, orientation, direction);
      return blockAmount;
    }
    return 0;
  }


  /**
   * Amount to scroll to reveal the rest of something we are on or a new item
   */
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.VERTICAL) {
      int lastHeight = 0, height = 0;
      int bottomOfScrollArea = visibleRect.y + visibleRect.height;

      for (Component c : getComponents()) {
        if (c.isVisible()) {
          if (c instanceof ContributionPanel) {
            Dimension d = c.getPreferredSize();

            int nextHeight = height + d.height;

            if (direction > 0) {
              // scrolling down
              if (nextHeight > bottomOfScrollArea) {
                return nextHeight - bottomOfScrollArea;
              }
            } else {
              // scrolling up
              if (nextHeight > visibleRect.y) {
                if (visibleRect.y != height) {
                  return visibleRect.y - height;
                } else {
                  return visibleRect.y - lastHeight;
                }
              }
            }

            lastHeight = height;
            height = nextHeight;
          }
        }
      }
    }
    return 0;
  }


  public boolean getScrollableTracksViewportHeight() {
    return false;
  }


  public boolean getScrollableTracksViewportWidth() {
    return true;
  }


  public int getNoOfRows() {
    return panelByContribution.size();
  }
}
