package processing.app.contrib;

import java.awt.Color;
import java.awt.Component;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import processing.app.Base;
import processing.app.contrib.ContributionListPanel.MyColumnHeaderRenderer;
import processing.app.ui.Toolkit;

public class UpdateContribListingPanel extends ContributionListPanel {

  public UpdateContribListingPanel(ContributionTab contributionTab,
                                   ContributionFilter filter) {
    super.contributionTab = contributionTab;
    super.filter = filter;
    setOpaque(true);

    if (Base.isLinux()) {
      // Because of a bug with GNOME, getColor returns the wrong value for
      // List.background. We'll just assume its white. The number of people
      // using Linux and an inverted color theme should be small enough.
      setBackground(Color.white);
    } else {
      setBackground(UIManager.getColor("List.background"));
    }

//    statusPlaceholder = new JPanel();
//    statusPlaceholder.setVisible(false);
//    status = new StatusPanel(null);
    
    String[] colName = { "", "Name", "Author", "Installed", "Update To" };
    dtm = new MyTableModel();
    dtm.setColumnIdentifiers(colName);
    table = new JTable(dtm){
      @Override
      public Component prepareRenderer(
              TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        String title = (String) getValueAt(row, 1);
        if (title.equals("<html><i>Library</i></html>") || title.equals("Tools")
          || title.equals("Modes") || title.equals("Examples")) {
          ((JComponent) c).setBorder(BorderFactory
            .createMatteBorder(row == 0 ? 0 : 2, 0, 2, 0, Color.BLACK));
        }
        return c;
      }
      @Override
      public void changeSelection(int rowIndex, int columnIndex,
                                  boolean toggle, boolean extend) {
        String title = (String) getValueAt(rowIndex, 1);
        if(title.equals("<html><i>Library</i></html>")){
          return;
        }
        super.changeSelection(rowIndex, columnIndex, toggle, extend);
      }
//      @Override
//      public boolean isRowSelected(int row) {
//        if (row == 0) {
//          return false;
//        }
//        return super.isRowSelected(row);
//      }
    };
    JScrollPane scrollPane = new JScrollPane(table);
    table.setFillsViewportHeight(true);
    table.setRowHeight(30);
    table.setRowMargin(6);
    table.getColumnModel().setColumnMargin(-1);
    table.getColumnModel().getColumn(0).setMaxWidth(60);
    table.setShowGrid(false);
    table.setCellSelectionEnabled(false);
    table.setRowSelectionAllowed(true);
    table.setAutoCreateColumnsFromModel(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getTableHeader().setDefaultRenderer(new MyColumnHeaderRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     boolean hasFocus, int row,
                                                     int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                                            row, column);
        JTableHeader tableHeader = table.getTableHeader();
        if (tableHeader != null) {
          setForeground(tableHeader.getForeground());
        }
        setIcon(getIcon(table, column));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(2, 0, 2, 0, Color.BLACK));
        return this;
      }
    });

    GroupLayout layout = new GroupLayout(this);
    layout.setHorizontalGroup(layout.createParallelGroup().addComponent(scrollPane));
    layout.setVerticalGroup(layout.createSequentialGroup().addComponent(scrollPane));

    this.setLayout(layout);
    table.setVisible(true);
    
    panelByContribution = new TreeMap<Contribution, ContributionPanel>(new Comparator<Contribution>() {

      @Override
      public int compare(Contribution o1, Contribution o2) {
        int val1 = 0;
        int val2 = 0;
        switch(o1.getType()){
        case LIBRARY: val1 = 1;
        break;
        case TOOL: val1 = 2;
        break;
        case MODE: val1 = 3;
        break;
        case EXAMPLES: val1 = 4;
        break;
        }
        switch(o2.getType()){
        case LIBRARY: val2 = 1;
        break;
        case TOOL: val2 = 2;
        break;
        case MODE: val2 = 3;
        break;
        case EXAMPLES: val2 = 4;
        break;
        }
        if(val1 == val2){
          return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
        }
        return val1 - val2;
      }
    });
    
  }
//  
//  @Override
//  public void contributionAdded(Contribution contribution) {
//    if(filter.matches(contribution)){
//    super.contributionAdded(contribution);
//    }
//  }
//  
  @Override
  void updatePanelOrdering(Set<Contribution> contributionsSet) {
    contributionTab.contributionManagerDialog.numberLabel.setText(Integer
      .toString(panelByContribution.size()));
    dtm.getDataVector().removeAllElements();
    dtm.fireTableDataChanged();
    ContributionType temp = null;
    for (Contribution entry : contributionsSet) {
      if(entry.getType() != temp){
        temp = entry.getType();
        dtm.addRow(new Object[] { null, "<html><i>" + temp.getTitle() + "</i></html>", null, null, null });
      }
      //TODO Make this into a function
      StringBuilder name = new StringBuilder("");
      String authorList = entry.getAuthorList();
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
      Icon icon = null;
      if (entry.isInstalled()) {
        icon = Toolkit.getLibIcon("icons/installedAndUptodate.png");
        if (contribListing.hasUpdates(entry)) {
          icon = Toolkit.getLibIcon("icons/installedNeedsUpdate.png");
        }
        if (!entry.isCompatible(Base.getRevision())) {
          icon = Toolkit.getLibIcon("icons/installedIncompatible.png");
        }
      }
      dtm
        .addRow(new Object[] {
          icon, "<html><b>" + entry.getName() + "</b></html>", name, entry.getPrettyVersion(),
          contributionTab.contribListing.getLatestVersion(entry) });
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
      if(columnIndex == 0){
        return Icon.class;
      }
      return super.getColumnClass(columnIndex);
    }
  }
}
