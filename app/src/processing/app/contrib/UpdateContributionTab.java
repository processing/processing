package processing.app.contrib;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import processing.app.Base;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;


public class UpdateContributionTab extends ContributionTab {

  public UpdateContributionTab(ContributionType type,ContributionManagerDialog contributionManagerDialog) {
    super();
    filter = ContributionType.createUpdateFilter();
    contributionListPanel = new UpdateContribListingPanel(this, filter);
    statusPanel = new UpdateStatusPanel(650, this);
    this.contributionType = type;
    this.contributionManagerDialog = contributionManagerDialog;
    contribListing = ContributionListing.getInstance();
    contribListing.addContributionListener(contributionListPanel);
  }

  @Override
  public void setLayout(Editor editor, boolean activateErrorPanel,
                        boolean isLoading) {
    if (panel == null) {
      progressBar = new JProgressBar();
      progressBar.setVisible(false);
      buildErrorPanel();
      panel = new JPanel(false);
      loaderLabel = new JLabel(Toolkit.getLibIcon("icons/loader.gif"));
      loaderLabel.setOpaque(false);
      loaderLabel.setBackground(Color.WHITE);
    }

    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setHorizontalGroup(layout
      .createParallelGroup(GroupLayout.Alignment.CENTER)
      .addComponent(loaderLabel)
      .addComponent(contributionListPanel).addComponent(errorPanel)
      .addComponent(statusPanel));

    layout.setVerticalGroup(layout
      .createSequentialGroup()
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(loaderLabel)
                  .addComponent(contributionListPanel))
      .addComponent(errorPanel)
      .addComponent(statusPanel, GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
    layout.setHonorsVisibility(contributionListPanel, false);

    panel.setBackground(Color.WHITE);
//    panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

  }

  public class UpdateContribListingPanel extends ContributionListPanel {

    public UpdateContribListingPanel(ContributionTab contributionTab,
                                     ContributionFilter filter) {
      super.contributionTab = contributionTab;
      super.filter = filter;
      setOpaque(true);
      setBackground(Color.WHITE);

//      statusPlaceholder = new JPanel();
//      statusPlaceholder.setVisible(false);
//      status = new StatusPanel(null);

      String[] colName = { "", "Name", "Author", "Installed", "Update To" };
      dtm = new MyTableModel(){
        @Override
        public Class<?> getColumnClass(int columnIndex) {
          if (columnIndex == 0) {
            return Icon.class;
          }
          return String.class;
        }
      };
      dtm.setColumnIdentifiers(colName);
      table = new JTable(dtm){
        @Override
        public Component prepareRenderer(
                TableCellRenderer renderer, int row, int column) {
          Component c = super.prepareRenderer(renderer, row, column);
          String title = (String) getValueAt(row, 1);
          if (title.equals("<html><i>Library</i></html>") || title.equals("<html><i>Tools</i></html>")
            || title.equals("<html><i>Modes</i></html>") || title.equals("<html><i>Examples</i></html>")) {
            ((JComponent) c).setBorder(BorderFactory
              .createMatteBorder(row == 0 ? 0 : 2, 0, 2, 0, Color.BLACK));
          }
          return c;
        }
        @Override
        public void changeSelection(int rowIndex, int columnIndex,
                                    boolean toggle, boolean extend) {
          String title = (String) getValueAt(rowIndex, 1);
          if(title.equals("<html><i>Library</i></html>") || title.equals("<html><i>Tools</i></html>")
                          || title.equals("<html><i>Modes</i></html>") || title.equals("<html><i>Examples</i></html>")){
            return;
          }
          super.changeSelection(rowIndex, columnIndex, toggle, extend);
        }
//        @Override
//        public boolean isRowSelected(int row) {
//          if (row == 0) {
//            return false;
//          }
//          return super.isRowSelected(row);
//        }
      };
      JScrollPane scrollPane = new JScrollPane(table);
      table.setFillsViewportHeight(true);
      table.setSelectionBackground(new Color(0xe0fffd));
      table.setSelectionForeground(table.getForeground());
      table.setFont(Toolkit.getSansFont(14, Font.PLAIN));
      table.setRowHeight(30);
      table.setRowMargin(6);
      table.getColumnModel().setColumnMargin(-1);
      table.getColumnModel().getColumn(0).setMaxWidth(60);
      table.setShowGrid(false);
      table.setCellSelectionEnabled(false);
      table.setRowSelectionAllowed(true);
      table.setAutoCreateColumnsFromModel(true);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      table.setDefaultRenderer(String.class, new StatusRendere());
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
          setBackground(new Color(0xebebeb));
//          setBorder(BorderFactory.createMatteBorder(2, 0, 2, 0, Color.BLACK));
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

    class StatusRendere extends DefaultTableCellRenderer {
      @Override
      public Component getTableCellRendererComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     boolean hasFocus, int row,
                                                     int column) {
        return super.getTableCellRendererComponent(table, value, isSelected, false,
                                                   row, column);
      }
    }
  //
//    @Override
//    public void contributionAdded(Contribution contribution) {
//      if(filter.matches(contribution)){
//      super.contributionAdded(contribution);
//      }
//    }
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
          icon = Toolkit.getLibIcon("manager/up-to-date.png");
          if (contribListing.hasUpdates(entry)) {
            icon = Toolkit.getLibIcon("manager/update-available.png");
          }
          if (!entry.isCompatible(Base.getRevision())) {
            icon = Toolkit.getLibIcon("manager/incompatible.png");
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
  public class UpdateStatusPanel extends StatusPanel {
    public UpdateStatusPanel(int width, final ContributionTab contributionTab) {
      super();
      updateButton = new JButton("Update All", Toolkit.getLibIcon("manager/update.png"));
      updateButton.setFont(Toolkit.getSansFont(14, Font.PLAIN));
      updateButton.setHorizontalAlignment(SwingConstants.LEFT);
//      updateButton.setContentAreaFilled(false);
//      updateButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1),BorderFactory.createEmptyBorder(3, 0, 3, 0)));
      updateButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          // TODO Auto-generated method stub
          for(ContributionPanel contributionPanel : contributionTab.contributionListPanel.panelByContribution.values()){
            contributionPanel.update();
          }
        }
      });
      this.setBackground(new Color(0xe0fffd));
//      this.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.BLACK));
      layout = new GroupLayout(this);
      this.setLayout(layout);

      layout.setAutoCreateContainerGaps(true);
      layout.setAutoCreateGaps(true);

      layout.setHorizontalGroup(layout
        .createSequentialGroup()
        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                         GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        .addComponent(updateButton, BUTTON_WIDTH, BUTTON_WIDTH, BUTTON_WIDTH));
      layout.setVerticalGroup(layout.createParallelGroup()
        .addComponent(updateButton));
      updateButton.setVisible(true);
    }

    @Override
    public void update(ContributionPanel panel) {
      if (contributionListPanel.getNoOfRows() > 0) {
        updateButton.setEnabled(true);
      } else {
        updateButton.setEnabled(false);
      }
    }
  }
}
