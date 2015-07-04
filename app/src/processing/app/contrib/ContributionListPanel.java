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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javafx.collections.transformation.SortedList;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import processing.app.Base;
import processing.app.ui.Toolkit;


// The "Scrollable" implementation and its methods here take care of preventing
// the scrolling area from running exceptionally slowly. Not sure why they're
// necessary in the first place, however; seems like odd behavior.
// It also allows the description text in the panels to wrap properly.

public class ContributionListPanel extends JPanel implements Scrollable, ContributionChangeListener {

  ContributionTab contributionTab;
  StatusPanel statusPanel;
  TreeMap<Contribution, ContributionPanel> panelByContribution;

  static HyperlinkListener nullHyperlinkListener = new HyperlinkListener() {
    public void hyperlinkUpdate(HyperlinkEvent e) { }
  };

  private ContributionPanel selectedPanel;
//  protected JPanel statusPlaceholder;
//  private StatusPanel status;
  private ContributionFilter filter;
//  private ContributionListing contribListing;
  private ContributionListing contribListing = ContributionListing.getInstance();
  private JTable table;
  DefaultTableModel dtm;


  public ContributionListPanel(final ContributionTab contributionTab,
                               ContributionFilter filter, StatusPanel statusPanel) {
    super();
    this.contributionTab = contributionTab;
    this.statusPanel = statusPanel;
    this.filter = filter;

//    contribListing = ContributionListing.getInstance();

    setLayout(new GridBagLayout());
    setOpaque(true);

    if (Base.isLinux()) {
      // Because of a bug with GNOME, getColor returns the wrong value for
      // List.background. We'll just assume its white. The number of people
      // using Linux and an inverted color theme should be small enough.
      setBackground(Color.white);
    } else {
      setBackground(UIManager.getColor("List.background"));
    }

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
              c.setBackground(Color.blue);
          } else {
              c.setBackground(Color.white);
          }
          return c;
      }
    };
    String[] colName = { "Status", "Name", "Author" };
    dtm.setColumnIdentifiers(colName);
    JScrollPane scrollPane = new JScrollPane(table);
    table.setFillsViewportHeight(true);
    table.setDefaultRenderer(Contribution.class, new StatusRendere());
    table.setRowHeight(30);
    table.setRowMargin(6);
    table.getColumnModel().setColumnMargin(-1);
    table.getColumnModel().getColumn(0).setMaxWidth(60);
    table.setShowGrid(false);
    table.setColumnSelectionAllowed(false);
    table.setCellSelectionEnabled(false);
    table.setAutoCreateColumnsFromModel(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
      public void valueChanged(ListSelectionEvent event) {
          //TODO this executes 2 times when clicked and 1 time when traversed using arrow keys
          panelByContribution.get(table.getValueAt(table.getSelectedRow(), 0)).setSelected(true);
          contributionTab.contributionManagerDialog.updateStatusPanel(panelByContribution.get(table.getValueAt(table.getSelectedRow(), 0)));
      }
  });
    
    GroupLayout layout = new GroupLayout(this);
    layout.setHorizontalGroup(layout.createParallelGroup().addComponent(scrollPane));
    layout.setVerticalGroup(layout.createSequentialGroup().addComponent(scrollPane));

    this.setLayout(layout);
    table.setVisible(true);
    
  }

  class StatusRendere extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus, int row,
                                                   int column) {
      Contribution contribution = (Contribution) value;
      JLabel label = new JLabel();
      if (column == 0) {
        Icon icon = null;
        label.setBorder(BorderFactory.createEmptyBorder(0, 17, 0, 0));
        if (contribution.isInstalled()) {
          icon = UIManager.getIcon("OptionPane.warningIcon");
          if (contribListing.hasUpdates(contribution)) {
            icon = Toolkit.getLibIcon("icons/pde-16.png");
          }
          if (!contribution.isCompatible(Base.getRevision())) {
            icon = Toolkit.getLibIcon("icons/pde-16.png");
          }
        }
        label.setIcon(icon);
        if (isSelected) {
          label.setBackground(Color.BLUE);
        }
        label.setOpaque(true);
//        return table.getDefaultRenderer(Icon.class).getTableCellRendererComponent(table, icon, isSelected, false, row, column);
      } else if (column == 1) {
        JTextPane name = new JTextPane();
        name.setContentType("text/html");
        name.setEditable(false);
        name.setText("<html><body><b>" + contribution.getName() + "</b> - "
          + contribution.getSentence() + "</body></html>");
        GroupLayout layout = new GroupLayout(label);

        layout.setAutoCreateGaps(true);
        layout.setHorizontalGroup(layout.createSequentialGroup()
          .addComponent(name));
        layout
          .setVerticalGroup(layout.createParallelGroup().addComponent(name));
        if (table.isRowSelected(row)) {
          name.setBackground(Color.BLUE);
          name.setOpaque(true);
        }
        label.setLayout(layout);
      } else {
        JLabel icon = new JLabel(
                                 contribution.isSpecial() ? Toolkit
                                   .getLibIcon("icons/pde-16.png") : null);
        JTextPane author = new JTextPane();
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
        author.setText(name.toString());
        author.setEditable(false);
        author.setOpaque(false);
        if (table.isRowSelected(row)) {
          label.setBackground(Color.BLUE);
        }
        GroupLayout layout = new GroupLayout(label);

//        layout.setAutoCreateGaps(true);
        layout.setHorizontalGroup(layout.createSequentialGroup()
          .addContainerGap().addComponent(icon).addComponent(author));
        layout.setVerticalGroup(layout
          .createParallelGroup(GroupLayout.Alignment.CENTER)
          .addComponent(author).addComponent(icon));
        label.setLayout(layout);
        label.setOpaque(true);
      }
      return label;
    }

  }
  class MyTableModel extends DefaultTableModel{
    MyTableModel() {
      super(0,0);
    }
    @Override
    public boolean isCellEditable(int row, int column) {
      return false;
    }
    @Override
    public Class<?> getColumnClass(int columnIndex) {
//      if(columnIndex == 0){
//        return Icon.class;
//      }
//      if(columnIndex == 1){
//        return String.class;
//      }
      return Contribution.class;
    }
  }

  private void updatePanelOrdering(Set<Contribution> contributionsSet) {
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
    dtm.getDataVector().removeAllElements();
    dtm.fireTableDataChanged();
    for (Contribution entry : contributionsSet) {
//      ImageIcon icon = null;
//      if (entry.getKey().isInstalled()) {
//        icon = Toolkit.getLibIcon("icons/pde-16.png");
//        if (contribListing.hasUpdates(entry.getKey())) {
//          icon = Toolkit.getLibIcon("icons/pde-16.png");
//        }
//        if (!entry.getKey().isCompatible(Base.getRevision())) {
//          icon = Toolkit.getLibIcon("icons/pde-16.png");
//        }
//      }
      ((DefaultTableModel)table.getModel()).addRow(new Object[]{entry, entry, entry});//"<html><body><b>"
//      + entry.getKey().getName() + "</b> - " + entry.getKey().getSentence()
//      + "</body></html>"
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
          // To make the scroll shift to the first element
          // http://stackoverflow.com/questions/19400239/scrolling-to-the-top-jpanel-inside-a-jscrollpane
          EventQueue.invokeLater(new Runnable() {
            public void run() {
              scrollRectToVisible(new Rectangle(0, 0, 1, 1));
            }
          });
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
    if (selectedPanel == contributionPanel) {
      selectedPanel.setSelected(true);

    } else {
      
      contributionTab.contributionManagerDialog.updateStatusPanel(contributionPanel);
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
            if (Base.isMacOS()) {
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
}
