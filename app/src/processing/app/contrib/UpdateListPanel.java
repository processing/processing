package processing.app.contrib;

import java.awt.Color;
import java.awt.Component;
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


public class UpdateListPanel extends ListPanel {
  static final Color SECTION_COLOR = new Color(0xFFf8f8f8);
  static final String[] PLURAL_TYPES = {
    ContributionType.LIBRARY.getPluralTitle(),
    ContributionType.MODE.getPluralTitle(),
    ContributionType.TOOL.getPluralTitle(),
    ContributionType.EXAMPLES.getPluralTitle(),
  };
  Set<String> sectionNames = new HashSet<>(Arrays.asList(PLURAL_TYPES));

  public UpdateListPanel(ContributionTab contributionTab,
                         Contribution.Filter filter) {
    super(contributionTab, filter);
  }
}
