package processing.app.contrib;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import processing.app.Base;
import processing.app.ui.Toolkit;


public class UpdateListPanel extends ListPanel {
  static final Color SECTION_COLOR = new Color(0xFFf8f8f8);
  static final String[] PLURAL_TYPES = {
    ContributionType.LIBRARY.getPluralTitle(),
    ContributionType.MODE.getPluralTitle(),
    ContributionType.TOOL.getPluralTitle(),
    ContributionType.EXAMPLES.getPluralTitle(),
  };
  Set<String> sectionNames = new HashSet<String>(Arrays.asList(PLURAL_TYPES));

  public UpdateListPanel(ContributionTab contributionTab,
                         Contribution.Filter filter) {
    this.contributionTab = contributionTab;
    this.filter = filter;

    setOpaque(true);
    setBackground(Color.WHITE);

    model = new DefaultTableModel() {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }

      @Override
      public Class<?> getColumnClass(int columnIndex) {
        return (columnIndex == 0) ? Icon.class : String.class;
      }
    };

    model.setColumnIdentifiers(new String[] {
      "", "Name", "Author", "Installed", "Available"
    });

    table = new JTable(model) {
      @Override
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        String title = (String) getValueAt(row, 1);
        if (sectionNames.contains(title)) {
          c.setBackground(SECTION_COLOR);
        } else {
          c.setBackground(Color.WHITE);
        }
        return c;
      }

      @Override
      public void changeSelection(int rowIndex, int columnIndex,
                                  boolean toggle, boolean extend) {
        String title = (String) getValueAt(rowIndex, 1);
        // Disallow selection on the fake rows
        if (!sectionNames.contains(title)) {
          super.changeSelection(rowIndex, columnIndex, toggle, extend);
        }
      }
    };

    scrollPane = new JScrollPane(table);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());

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

    table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     boolean hasFocus,
                                                     int row, int column) {
        return super.getTableCellRendererComponent(table, value, isSelected,
                                                   false, row, column);
      }
    });

    table.getTableHeader().setDefaultRenderer(new ContribHeaderRenderer()); /* {
      @Override
      public Component getTableCellRendererComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     boolean hasFocus, int row,
                                                     int column) {
        super.getTableCellRendererComponent(table, value, isSelected,
                                            hasFocus, row, column);
        JTableHeader tableHeader = table.getTableHeader();
        if (tableHeader != null) {
          setForeground(tableHeader.getForeground());
        }
        setIcon(getSortIcon(table, column));
        setBackground(new Color(0xebebeb));
        return this;
      }
    });*/

    GroupLayout layout = new GroupLayout(this);
    layout.setHorizontalGroup(layout.createParallelGroup().addComponent(scrollPane));
    layout.setVerticalGroup(layout.createSequentialGroup().addComponent(scrollPane));

    setLayout(layout);
    table.setVisible(true);

    panelByContribution = new TreeMap<Contribution, DetailPanel>(new Comparator<Contribution>() {
      @Override
      public int compare(Contribution o1, Contribution o2) {
        int diff =
          ContributionManager.getTypeIndex(o1.getType()) -
          ContributionManager.getTypeIndex(o2.getType());
        if (diff == 0) {
          diff = o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
        }
        return diff;
      }
    });
  }

  @Override
  void updatePanelOrdering(Set<Contribution> contributionsSet) {
//    int updateCount = panelByContribution.size();
//    new Exception("update count is " + updateCount).printStackTrace(System.out);
//    (UpdateContributionTab) contributionTab

    model.getDataVector().removeAllElements();
    model.fireTableDataChanged();
    ContributionType currentType = null;

    // Avoid ugly synthesized bold
    Font boldFont = Toolkit.getSansFont(table.getFont().getSize(), Font.BOLD);
    String fontFace = "<font face=\"" + boldFont.getName() + "\">";

    for (Contribution entry : contributionsSet) {
      if (entry.getType() != currentType) {
        currentType = entry.getType();
        model.addRow(new Object[] {
          null, currentType.getPluralTitle(), null, null, null
        });
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
        icon = upToDateIcon;
        if (contribListing.hasUpdates(entry)) {
          icon = updateAvailableIcon;
        }
        if (!entry.isCompatible(Base.getRevision())) {
          icon = incompatibleIcon;
        }
      }
      model.addRow(new Object[] {
        icon,
        "<html>" + fontFace + entry.getName() + "</font></html>",
        name,
        entry.getPrettyVersion(),
        contributionTab.contribListing.getLatestVersion(entry)
      });
    }
    UpdateContributionTab tab = (UpdateContributionTab) contributionTab;
    ((UpdateStatusPanel) tab.statusPanel).update();
  }


  @Override
  public void contributionAdded(final Contribution contribution) {
    if (filter.matches(contribution)) {
      // TODO: this should already be on EDT, check it [jv]
      EventQueue.invokeLater(new Runnable() {
        public void run() {
          // TODO make this longer and more contorted [fry]
          DetailPanel newPanel =
            contributionTab.contribDialog.getTab(contribution.getType()).contributionListPanel.panelByContribution.get(contribution);
          if (newPanel == null) {
            newPanel = new DetailPanel(UpdateListPanel.this);
          }
          synchronized (panelByContribution) {
            if (!panelByContribution.containsKey(contribution)) {
              panelByContribution.put(contribution, newPanel);
            }
            synchronized (visibleContributions) {
              visibleContributions.add(contribution);
            }
            if (newPanel != null) {
              newPanel.setContribution(contribution);
              add(newPanel);
              updatePanelOrdering(panelByContribution.keySet());
              updateColors(); // XXX this is the place
            }
          }
        }
      });
    }
  }
  
  @Override
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
          }
        }
        synchronized (visibleContributions) {
          if (visibleContributions.contains(oldContrib)) {
            visibleContributions.remove(oldContrib);
          }
          updatePanelOrdering(visibleContributions);
        }
      }
    });
  }
}