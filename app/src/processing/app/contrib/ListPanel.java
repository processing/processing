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

public class ListPanel extends JPanel
implements Scrollable, ContributionListing.ChangeListener {
  ContributionTab contributionTab;
  TreeMap<Contribution, DetailPanel> panelByContribution = new TreeMap<>(ContributionListing.COMPARATOR);

  private DetailPanel selectedPanel;
  protected ContributionRowFilter filter;
  protected ContributionListing contribListing = ContributionListing.getInstance();
  protected JTable table;
  ContributionTableModel model;
  JScrollPane scrollPane;

  static Icon upToDateIcon;
  static Icon updateAvailableIcon;
  static Icon incompatibleIcon;
  static Icon foundationIcon;
  static Icon downloadingIcon;

  // Should this be in theme.txt? Of course! Is it? No.
  static final Color HEADER_BGCOLOR = new Color(0xffEBEBEB);


  public ListPanel() {
    if (upToDateIcon == null) {
      upToDateIcon = Toolkit.getLibIconX("manager/up-to-date");
      updateAvailableIcon = Toolkit.getLibIconX("manager/update-available");
      incompatibleIcon = Toolkit.getLibIconX("manager/incompatible");
      foundationIcon = Toolkit.getLibIconX("icons/foundation", 16);
      downloadingIcon = Toolkit.getLibIconX("manager/downloading");
    }
  }


  public ListPanel(final ContributionTab contributionTab,
                   final Contribution.Filter filter) {
    this();
    this.contributionTab = contributionTab;
    this.filter = new ContributionRowFilter(filter);

    setLayout(new GridBagLayout());
    setOpaque(true);
    setBackground(Color.WHITE);
    model = new ContributionTableModel();
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
    scrollPane = new JScrollPane(table);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    table.setFillsViewportHeight(true);
    table.setDefaultRenderer(Contribution.class, new ContribStatusRenderer());
    table.setFont(ManagerFrame.NORMAL_PLAIN);
    table.setRowHeight(Toolkit.zoom(28));
    table.setRowMargin(Toolkit.zoom(6));
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

    TableRowSorter<ContributionTableModel> sorter = new TableRowSorter<>(model);
    table.setRowSorter(sorter);
    sorter.setRowFilter(this.filter);
    sorter.setComparator(1, ContributionListing.COMPARATOR);
    sorter.setComparator(2, Comparator.comparing(o -> getAuthorNameWithoutMarkup(((Contribution) o).getAuthorList())));
    sorter.setComparator(0, Comparator.comparingInt(o -> getContributionStatusRank((Contribution) o)));
    table.getTableHeader().setDefaultRenderer(new ContribHeaderRenderer());

    GroupLayout layout = new GroupLayout(this);
    layout.setHorizontalGroup(layout.createParallelGroup().addComponent(scrollPane));
    layout.setVerticalGroup(layout.createSequentialGroup().addComponent(scrollPane));

    this.setLayout(layout);
    table.setVisible(true);
  }

  private int getContributionStatusRank(Contribution c) {
    int pos = 4;
    if (c.isInstalled()) {
      pos = 1;
      if (contribListing.hasUpdates(c)) {
        pos = 2;
      }
      if (!c.isCompatible(Base.getRevision())) {
        pos = 3;
      }
    }
    return pos;
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
      setFont(ManagerFrame.SMALL_PLAIN);
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
      return Optional.ofNullable(table.getRowSorter())
              .map(RowSorter::getSortKeys)
              .map(columns -> columns.isEmpty() ? null : columns.get(0))
              .orElse(null);

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
      if (isSelected) {
        label.setBackground(new Color(0xe0fffd));
      }
      if (column == 0) {
        configureStatusColumnLabel(label, contribution);
      } else if (column == 1) {
        configureNameColumnLabel(table, label, contribution);
      } else {
        configureAuthorsColumnLabel(label, contribution);
      }

      if(!contribution.isCompatible(Base.getRevision())){
        label.setForeground(Color.LIGHT_GRAY);
      }
      label.setOpaque(true);
      return label;
    }

    private void configureStatusColumnLabel(JLabel label, Contribution contribution) {
      Icon icon = null;
      label.setFont(ManagerFrame.NORMAL_PLAIN);
      if ((panelByContribution.get(contribution)).updateInProgress ||
              (panelByContribution.get(contribution)).installInProgress) {
        // Display "Loading icon" if download/install in progress
        icon = downloadingIcon;
      } else if (contribution.isInstalled()) {
        if (!contribution.isCompatible(Base.getRevision())) {
          icon = incompatibleIcon;
        } else if (contribListing.hasUpdates(contribution)) {
          icon = updateAvailableIcon;
        } else {
          icon = upToDateIcon;
        }
      }

      label.setIcon(icon);
      label.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void configureNameColumnLabel(JTable table, JLabel label, Contribution contribution) {
      // Generating ellipses based on fontMetrics
      final Font boldFont = ManagerFrame.NORMAL_BOLD;
      FontMetrics fontMetrics = table.getFontMetrics(boldFont); //table.getFont());
      int colSize = table.getColumnModel().getColumn(1).getWidth();
      int currentWidth = fontMetrics.stringWidth(contribution.getName() + " | ...");
      String sentence = contribution.getSentence();
      StringBuilder text = new StringBuilder("<html><body><font face=\"")
              .append(boldFont.getName())
              .append("\">")
              .append(contribution.getName());

      if (sentence == null) {
        text.append("</font>");
      } else {
        int i = 0;
        for (i = 0; i < sentence.length(); i++) {
          currentWidth += fontMetrics.charWidth(sentence.charAt(i));
          if (currentWidth >= colSize) {
            break;
          }
        }
        text.append(" | </font>").append(sentence, 0, i);
        // Adding ellipses only if text doesn't fits into the column
        if(i != sentence.length()) {
          text.append("...");
        }
      }
      text.append("</body></html>");
      label.setText(text.toString());
      label.setFont(ManagerFrame.NORMAL_PLAIN);
    }

    private void configureAuthorsColumnLabel(JLabel label, Contribution contribution) {
      if (contribution.isSpecial()) {
        label.setIcon(foundationIcon);
      }
      String authorList = contribution.getAuthorList();
      String name = getAuthorNameWithoutMarkup(authorList);
      label.setText(name);
      label.setHorizontalAlignment(SwingConstants.LEFT);
      label.setForeground(Color.BLACK);
      label.setFont(ManagerFrame.NORMAL_BOLD);
    }
  }

  protected class ContributionTableModel extends AbstractTableModel {
    @Override
    public int getRowCount() {
      return contribListing.allContributions.size();
    }

    @Override
    public int getColumnCount() {
      return 3;
    }

    @Override
    public String getColumnName(int column) {
      switch (column) {
        case 0: return " Status"; // Note the space
        case 1: return "Name";
        case 2: return "Author";
        default: return "";
      }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return Contribution.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      return contribListing.allContributions.stream().skip(rowIndex).findFirst().get();
    }
  }

  protected class ContributionRowFilter extends RowFilter<ContributionTableModel, Integer> {
    Contribution.Filter contributionFilter;
    Optional<String> categoryFilter = Optional.empty();
    List<String> stringFilters = Collections.emptyList();

    ContributionRowFilter(Contribution.Filter contributionFilter) {
      this.contributionFilter = contributionFilter;
    }

    public void setCategoryFilter(String categoryFilter) {
      this.categoryFilter = Optional.ofNullable(categoryFilter);
    }

    public void setStringFilters(List<String> filters) {
      this.stringFilters = filters;
    }

    @Override
    public boolean include(Entry<? extends ContributionTableModel, ? extends Integer> entry) {
      Contribution contribution = (Contribution) entry.getValue(0);
      return contributionFilter.matches(contribution)
              && categoryFilter.map(contribution::hasCategory).orElse(true)
              && stringFilters.stream().allMatch(pattern -> contribListing.matches(contribution, pattern));
    }
  }

  String getAuthorNameWithoutMarkup(String authorList) {
    StringBuilder name = new StringBuilder("");
    if (authorList != null) {
      int parentheses = 0;
      for (int i = 0; i < authorList.length(); i++) {

        if (authorList.charAt(i) == '[' || authorList.charAt(i) == ']') {
          continue;
        }
        if (authorList.charAt(i) == '(') {
          parentheses++;
        } else if (authorList.charAt(i) == ')') {
          parentheses--;
        } else if (parentheses == 0) {
          name.append(authorList.charAt(i));
        }
      }
    }
    return name.toString();
  }

  // Thread: EDT
  public void contributionAdded(final Contribution contribution) {
    if (!panelByContribution.containsKey(contribution)) {
      DetailPanel newPanel =
              new DetailPanel(ListPanel.this);
      panelByContribution.put(contribution, newPanel);
      newPanel.setContribution(contribution);
      add(newPanel);
      model.fireTableDataChanged();
      updateColors();  // XXX this is the place
    }
  }


  // Thread: EDT
  public void contributionRemoved(final Contribution contribution) {
      DetailPanel panel = panelByContribution.get(contribution);
      if (panel != null) {
        remove(panel);
        panelByContribution.remove(contribution);
      }
      model.fireTableDataChanged();
      updateColors();
      updateUI();
  }


  // Thread: EDT
  public void contributionChanged(final Contribution oldContrib,
                                  final Contribution newContrib) {
      DetailPanel panel = panelByContribution.get(oldContrib);
      if (panel == null) {
        contributionAdded(newContrib);
      } else {
        panelByContribution.remove(oldContrib);
        panel.setContribution(newContrib);
        panelByContribution.put(newContrib, panel);
        model.fireTableDataChanged();
      }
  }


  // Thread: EDT
  public void filterLibraries(String category, List<String> filters) {
    filter.setCategoryFilter(category);
    filter.setStringFilters(filters);
    model.fireTableDataChanged();
  }


  // Thread: EDT
  protected void setSelectedPanel(DetailPanel contributionPanel) {
    contributionTab.updateStatusPanel(contributionPanel);

    if (selectedPanel == contributionPanel) {
      selectedPanel.setSelected(true);

    } else {
      DetailPanel lastSelected = selectedPanel;
      selectedPanel = contributionPanel;

      if (lastSelected != null) {
        lastSelected.setSelected(false);
      }
      contributionPanel.setSelected(true);

      updateColors();
      requestFocusInWindow();
    }
  }


  protected DetailPanel getSelectedPanel() {
    return selectedPanel;
  }


  // Thread: EDT
  /**
   * Updates the colors of all library panels that are visible.
   */
  protected void updateColors() {
    int count = 0;
    for (Entry<Contribution, DetailPanel> entry : panelByContribution.entrySet()) {
      DetailPanel panel = entry.getValue();
      Border border = BorderFactory.createEmptyBorder(1, 1, 1, 1);

      if (panel.isVisible()) {
        boolean oddRow = count % 2 == 1;
        Color bgColor = null;
        Color fgColor = UIManager.getColor("List.foreground");

        if (panel.isSelected()) {
          bgColor = UIManager.getColor("List.selectionBackground");
          fgColor = UIManager.getColor("List.selectionForeground");
          border = UIManager.getBorder("List.focusCellHighlightBorder");
        } else if (Platform.isMacOS()) {
          border = oddRow
                  ? UIManager.getBorder("List.oddRowBackgroundPainter")
                  : UIManager.getBorder("List.evenRowBackgroundPainter");
        } else {
          bgColor = oddRow
                  ? new Color(219, 224, 229)
                  : new Color(241, 241, 241);
        }

        panel.setForeground(fgColor);
        if (bgColor != null) {
          panel.setBackground(bgColor);
        }
        count++;
      }

      panel.setBorder(border);
    }
  }


  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }


  /**
   * Amount to scroll to reveal a new page of items
   */
  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect,
                                         int orientation, int direction) {
    if (orientation == SwingConstants.VERTICAL) {
      int blockAmount = visibleRect.height;
      if (direction > 0) {
        visibleRect.y += blockAmount;
      } else {
        visibleRect.y -= blockAmount;
      }

      blockAmount +=
        getScrollableUnitIncrement(visibleRect, orientation, direction);
      return blockAmount;
    }
    return 0;
  }


  /**
   * Amount to scroll to reveal the rest of something we are on or a new item
   */
  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation != SwingConstants.VERTICAL) {
      return 0;
    }
    int lastHeight = 0;
    int height = 0;
    int bottomOfScrollArea = visibleRect.y + visibleRect.height;

    for (Component c : getComponents()) {
      if (!(c.isVisible() && c instanceof DetailPanel)) {
        continue;
      }
      Dimension d = c.getPreferredSize();

      int nextHeight = height + d.height;

      if (direction > 0) {
        // scrolling down
        if (nextHeight > bottomOfScrollArea) {
          return nextHeight - bottomOfScrollArea;
        }
      } else if (nextHeight > visibleRect.y) {
        if (visibleRect.y != height) {
          return visibleRect.y - height;
        } else {
          return visibleRect.y - lastHeight;
        }
      }

      lastHeight = height;
      height = nextHeight;
    }

    return 0;
  }


  @Override
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }


  @Override
  public boolean getScrollableTracksViewportWidth() {
    return true;
  }


  public int getRowCount() {
    return panelByContribution.size();
  }
}
