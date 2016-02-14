/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import processing.app.*;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;


public class ContributionTab extends JPanel {
  static final String ANY_CATEGORY = Language.text("contrib.all");
  static final int FILTER_WIDTH = 180;

  ContributionType contribType;
  ManagerFrame contribDialog;

  Contribution.Filter filter;
  JComboBox<String> categoryChooser;
  ListPanel contributionListPanel;
  StatusPanel statusPanel;
  FilterField filterField;
  JLabel categoryLabel;
  JLabel loaderLabel;

  JPanel errorPanel;
  JTextPane errorMessage;
  JButton tryAgainButton;
  JButton closeButton;

  // the calling editor, so updates can be applied
  Editor editor;
  String category;

  JProgressBar progressBar;


  public ContributionTab() { }


  public ContributionTab(ManagerFrame dialog, ContributionType type) {
    this.contribDialog = dialog;
    this.contribType = type;

    /*
    if (type == ContributionType.MODE) {
      title = Language.text("contrib.manager_title.mode");
    } else if (type == ContributionType.TOOL) {
      title = Language.text("contrib.manager_title.tool");
    } else if (type == ContributionType.LIBRARY) {
      title = Language.text("contrib.manager_title.library");
    } else if (type == ContributionType.EXAMPLES) {
      title = Language.text("contrib.manager_title.examples");
    }
    */

    filter = new Contribution.Filter() {
      public boolean matches(Contribution contrib) {
        return contrib.getType() == contribType;
      }
    };

    statusPanel = new StatusPanel(this, 650);
    contributionListPanel = new ListPanel(this, filter);
    ManagerFrame.contributionListing.addListener(contributionListPanel);
  }


//  public boolean hasUpdates(Base base) {
//    return contribListing.hasUpdates(base);
//  }


  public void showFrame(final Editor editor, boolean error, boolean loading) {
    this.editor = editor;

    setLayout(error, loading);
    contributionListPanel.setVisible(!loading);
    loaderLabel.setVisible(loading);
    errorPanel.setVisible(error);

    validate();
    repaint();
  }


  protected void setLayout(boolean activateErrorPanel,
                           boolean isLoading) {
    if (progressBar == null) {
      progressBar = new JProgressBar();

      createComponents();
      buildErrorPanel();

      loaderLabel = new JLabel(Toolkit.getLibIcon("manager/loader.gif"));
      loaderLabel.setOpaque(false);
      loaderLabel.setBackground(Color.WHITE);
    }

    int scrollBarWidth = contributionListPanel.scrollPane.getVerticalScrollBar().getPreferredSize().width;

    GroupLayout layout = new GroupLayout(this);
    setLayout(layout);
//    layout.setAutoCreateContainerGaps(true);
//    layout.setAutoCreateGaps(true);
    layout.setHorizontalGroup(layout
      .createParallelGroup(GroupLayout.Alignment.CENTER)
      .addGroup(layout
                  .createSequentialGroup()
                  .addGap(ManagerFrame.STATUS_WIDTH)
                  .addComponent(filterField,
                                FILTER_WIDTH, FILTER_WIDTH, FILTER_WIDTH)
//                  .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                  .addComponent(categoryChooser,
                                ManagerFrame.AUTHOR_WIDTH,
                                ManagerFrame.AUTHOR_WIDTH,
                                ManagerFrame.AUTHOR_WIDTH)
                  .addGap(scrollBarWidth)).addComponent(loaderLabel)
      .addComponent(contributionListPanel).addComponent(errorPanel)
      .addComponent(statusPanel));

    layout.setVerticalGroup(layout
      .createSequentialGroup()
      .addContainerGap()
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(categoryChooser)
                  .addComponent(filterField))
      .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(loaderLabel)
                  .addComponent(contributionListPanel))
      .addComponent(errorPanel)
      .addComponent(statusPanel, GroupLayout.PREFERRED_SIZE,
                    GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
    layout.linkSize(SwingConstants.VERTICAL, categoryChooser, filterField);

    // these will occupy space even if not visible
    layout.setHonorsVisibility(contributionListPanel, false);
    layout.setHonorsVisibility(categoryChooser, false);

    setBackground(Color.WHITE);
    setBorder(null);
  }


  private void createComponents() {
    categoryLabel = new JLabel(Language.text("contrib.category"));

    categoryChooser = new JComboBox<String>();
    categoryChooser.setMaximumRowCount(20);
    categoryChooser.setFont(Toolkit.getSansFont(14, Font.PLAIN));

    updateCategoryChooser();

    categoryChooser.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        category = (String) categoryChooser.getSelectedItem();
        if (ManagerFrame.ANY_CATEGORY.equals(category)) {
          category = null;
        }
        filterLibraries(category, filterField.filters);
      }
    });

    filterField = new FilterField();
  }


  protected void buildErrorPanel() {
    errorPanel = new JPanel();
    GroupLayout layout = new GroupLayout(errorPanel);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);
    errorPanel.setLayout(layout);
//    errorPanel.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.BLACK));
    errorMessage = new JTextPane();
    errorMessage.setEditable(false);
    errorMessage.setContentType("text/html");
    errorMessage.setText("<html><body>Could not connect to the Processing server.<br>"
      + "Contributions cannot be installed or updated without an Internet connection.<br>"
      + "Please verify your network connection again, then try connecting again.</body></html>");
    errorMessage.setFont(Toolkit.getSansFont(14, Font.PLAIN));
    errorMessage.setMaximumSize(new Dimension(550, 50));
    errorMessage.setOpaque(false);

    StyledDocument doc = errorMessage.getStyledDocument();
    SimpleAttributeSet center = new SimpleAttributeSet();
    StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
    doc.setParagraphAttributes(0, doc.getLength(), center, false);

    // TODO https://github.com/processing/processing/issues/3706
    closeButton = new JButton("X");
    closeButton.setContentAreaFilled(false);
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        contribDialog.makeAndShowTab(false, false);
      }
    });
    tryAgainButton = new JButton("Try Again");
    tryAgainButton.setFont(Toolkit.getSansFont(14, Font.PLAIN));
    tryAgainButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        contribDialog.makeAndShowTab(false, true);
        contribDialog.downloadAndUpdateContributionListing(editor.getBase());
      }
    });
    layout.setHorizontalGroup(layout.createSequentialGroup()
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
      .addGroup(layout
                  .createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(errorMessage)
                  .addComponent(tryAgainButton, StatusPanel.BUTTON_WIDTH,
                                StatusPanel.BUTTON_WIDTH,
                                StatusPanel.BUTTON_WIDTH))
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
      .addComponent(closeButton));
    layout.setVerticalGroup(layout.createSequentialGroup()
      .addGroup(layout.createParallelGroup().addComponent(errorMessage)
                  .addComponent(closeButton)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(tryAgainButton));
    errorPanel.setBackground(Color.PINK);
    errorPanel.validate();
  }


  protected void updateCategoryChooser() {
    if (categoryChooser != null) {
      ArrayList<String> categories;
      categoryChooser.removeAllItems();
      categories = new ArrayList<String>(ManagerFrame.contributionListing.getCategories(filter));
//      for (int i = 0; i < categories.size(); i++) {
//        System.out.println(i + " category: " + categories.get(i));
//      }
      Collections.sort(categories);
//    categories.add(0, ContributionManagerDialog.ANY_CATEGORY);
      boolean categoriesFound = false;
      categoryChooser.addItem(ManagerFrame.ANY_CATEGORY);
      for (String s : categories) {
        categoryChooser.addItem(s);
        if (!s.equals(Contribution.UNKNOWN_CATEGORY)) {
          categoriesFound = true;
        }
      }
      categoryChooser.setVisible(categoriesFound);
    }
  }


  protected void filterLibraries(String category, List<String> filters) {
    List<Contribution> filteredLibraries =
      ManagerFrame.contributionListing.getFilteredLibraryList(category, filters);
    contributionListPanel.filterLibraries(filteredLibraries);
  }

  protected void setFilterText(String filter) {
    if (filter == null || filter.isEmpty()) {
      filterField.setText("");
    } else {
      filterField.setText(filter);
    }
    filterField.applyFilter();
  }


  class FilterField extends JTextField {
    Icon searchIcon;
    List<String> filters;
    JLabel filterLabel;

    public FilterField () {
      super("");

      filterLabel = new JLabel("Filter");
      filterLabel.setFont(Toolkit.getSansFont(14, Font.PLAIN));
      filterLabel.setOpaque(false);

      setFont(Toolkit.getSansFont(14, Font.PLAIN));
      searchIcon = Toolkit.getLibIconX("manager/search");
      filterLabel.setIcon(searchIcon);
      //searchIcon = new ImageIcon(java.awt.Toolkit.getDefaultToolkit().getImage("NSImage://NSComputerTemplate"));
      setOpaque(false);
      //setBorder(BorderFactory.createMatteBorder(0, 33, 0, 0, searchIcon));

      GroupLayout fl = new GroupLayout(this);
      setLayout(fl);
      fl.setHorizontalGroup(fl.createSequentialGroup().addComponent(filterLabel));
      fl.setVerticalGroup(fl.createSequentialGroup()
                          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                                           GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                          .addComponent(filterLabel)
                          .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                                           GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));

      filters = new ArrayList<String>();

      addFocusListener(new FocusListener() {
        public void focusLost(FocusEvent focusEvent) {
          if (getText().isEmpty()) {
//            setBorder(BorderFactory.createMatteBorder(0, 33, 0, 0, searchIcon));
            filterLabel.setVisible(true);
          }
        }

        public void focusGained(FocusEvent focusEvent) {
//          setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
          filterLabel.setVisible(false);
        }
      });

      getDocument().addDocumentListener(new DocumentListener() {
        public void removeUpdate(DocumentEvent e) {
          applyFilter();
        }

        public void insertUpdate(DocumentEvent e) {
          applyFilter();
        }

        public void changedUpdate(DocumentEvent e) {
          applyFilter();
        }
      });
    }

    public void applyFilter() {
      String filter = getText();
      filter = filter.toLowerCase();

      // Replace anything but 0-9, a-z, or : with a space
      filter = filter.replaceAll("[^\\x30-\\x39^\\x61-\\x7a^\\x3a]", " ");
      filters = Arrays.asList(filter.split(" "));
      filterLibraries(category, filters);

    }
  }


//  public boolean hasAlreadyBeenOpened() {
//    return panel != null;
//  }


  public void updateStatusPanel(DetailPanel contributionPanel) {
    statusPanel.update(contributionPanel);
  }


  protected void updateAll() {
    Collection<DetailPanel> collection =
      contributionListPanel.panelByContribution.values();
    for (DetailPanel detailPanel : collection) {
      detailPanel.update();
    }
  }


  protected boolean hasUpdates() {
    return contributionListPanel.getRowCount() > 0;
  }
}
