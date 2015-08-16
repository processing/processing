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


public class ContributionTab {
  static final String ANY_CATEGORY = Language.text("contrib.all");

  ContributionType contributionType;
  ContributionManagerDialog contributionManagerDialog;
  JPanel panel;
  String title;
  ContributionFilter filter;
  JComboBox<String> categoryChooser;
  JScrollPane scrollPane;
  ContributionListPanel contributionListPanel;
  StatusPanel statusPanel;
  FilterField filterField;
  JButton restartButton;
  JLabel categoryLabel;
  JLabel loaderLabel;

  JPanel errorPanel;
  JTextPane errorMessage;
  JButton tryAgainButton;
  JButton closeButton;

  // the calling editor, so updates can be applied
  Editor editor;
  String category;
  ContributionListing contribListing;

  JProgressBar progressBar;


  public ContributionTab(ContributionType type,ContributionManagerDialog contributionManagerDialog) {
    if (type == ContributionType.MODE) {
      title = Language.text("contrib.manager_title.mode");
    } else if (type == ContributionType.TOOL) {
      title = Language.text("contrib.manager_title.tool");
    } else if (type == ContributionType.LIBRARY) {
      title = Language.text("contrib.manager_title.library");
    } else if (type == ContributionType.EXAMPLES) {
      title = Language.text("contrib.manager_title.examples");
    }

    filter = type.createFilter();
    this.contributionType = type;
    this.contributionManagerDialog = contributionManagerDialog;
    contribListing = ContributionListing.getInstance();
      statusPanel = new StatusPanel(650,this);
      contributionListPanel = new ContributionListPanel(this, filter);
    contribListing.addContributionListener(contributionListPanel);
  }


  public ContributionTab() {
    // TODO Auto-generated constructor stub
  }


  /*
  public boolean hasUpdates() {
    return contribListing.hasUpdates();
  }
  */


  public boolean hasUpdates(Base base) {
    return contribListing.hasUpdates(base);
  }

//  protected JPanel makeTextPanel(String text) {
//    JPanel panel = new JPanel(false);
//    JLabel filler = new JLabel(text);
//    filler.setHorizontalAlignment(JLabel.CENTER);
//    panel.setLayout(new GridLayout(1, 1));
//    panel.add(filler);
//    return panel;
//  }




  public void showFrame(final Editor editor, boolean activateErrorPanel,
                        final boolean isLoading) {
    this.editor = editor;
    if (panel == null) {
      setLayout(editor, activateErrorPanel, isLoading);
    }
    contributionListPanel.setVisible(!isLoading);
    loaderLabel.setVisible(isLoading);
    errorPanel.setVisible(activateErrorPanel);
    panel.validate();
    panel.repaint();
  }


  public void setLayout(final Editor editor, boolean activateErrorPanel, boolean isLoading) {
    if(panel == null){
      progressBar = new JProgressBar();
      progressBar.setVisible(false);
      createComponents();
      panel = new JPanel(false);
      loaderLabel = new JLabel(Toolkit.getLibIcon("manager/loader.gif"));
      loaderLabel.setOpaque(false);
      loaderLabel.setBackground(Color.WHITE);
    }

    /*restartButton = new JButton(Language.text("contrib.restart"));
    restartButton.setVisible(false);
    restartButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent arg0) {

        Iterator<Editor> iter = editor.getBase().getEditors().iterator();
        while (iter.hasNext()) {
          Editor ed = iter.next();
          if (ed.getSketch().isModified()) {
            int option = Base
              .showYesNoQuestion(editor, title,
                                 Language.text("contrib.unsaved_changes"),
                                 Language.text("contrib.unsaved_changes.prompt"));

            if (option == JOptionPane.NO_OPTION)
              return;
            else
              break;
          }
        }

        // Thanks to http://stackoverflow.com/a/4160543
        StringBuilder cmd = new StringBuilder();
        cmd.append(System.getProperty("java.home") + File.separator + "bin"
          + File.separator + "java ");
        for (String jvmArg : ManagementFactory.getRuntimeMXBean()
          .getInputArguments()) {
          cmd.append(jvmArg + " ");
        }
        cmd.append("-cp ")
          .append(ManagementFactory.getRuntimeMXBean().getClassPath())
          .append(" ");
        cmd.append(Base.class.getName());

        try {
          Runtime.getRuntime().exec(cmd.toString());
          System.exit(0);
        } catch (IOException e) {
          e.printStackTrace();
        }

      }

    });*/


    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
//    layout.setAutoCreateContainerGaps(true);
//    layout.setAutoCreateGaps(true);
    layout.setHorizontalGroup(layout
      .createParallelGroup(GroupLayout.Alignment.CENTER)
      .addGroup(layout
                  .createSequentialGroup()
                  .addGap(ContributionManagerDialog.STATUS_WIDTH)
                  .addComponent(filterField,
                                ContributionManagerDialog.FILTER_WIDTH,
                                ContributionManagerDialog.FILTER_WIDTH,
                                ContributionManagerDialog.FILTER_WIDTH)
//                  .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                  .addComponent(categoryChooser,
                                ContributionManagerDialog.AUTHOR_WIDTH - 10,
                                ContributionManagerDialog.AUTHOR_WIDTH - 10,
                                ContributionManagerDialog.AUTHOR_WIDTH - 10)
                  .addContainerGap()).addComponent(loaderLabel)
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
    layout.setHonorsVisibility(contributionListPanel, false);

    panel.setBackground(Color.WHITE);
    panel.setBorder(null);
 }


  /** Creates and arranges the Swing components in the dialog.
   */
  private void createComponents() {

      categoryLabel = new JLabel(Language.text("contrib.category"));

      categoryChooser = new JComboBox<String>();
      categoryChooser.setMaximumRowCount(20);
      categoryChooser.setFont(Toolkit.getSansFont(14, Font.PLAIN));

      updateCategoryChooser();

      categoryChooser.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          category = (String) categoryChooser.getSelectedItem();
          if (ContributionManagerDialog.ANY_CATEGORY.equals(category)) {
            category = null;
          }
          filterLibraries(category, filterField.filters);
//          contributionListPanel.updateColors();
        }
      });

      filterField = new FilterField();

      buildErrorPanel();

  }
  void buildErrorPanel(){
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


    closeButton = new JButton("X");
    closeButton.setContentAreaFilled(false);
    closeButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        contributionManagerDialog.makeAndShowTab(false, false);
      }
    });
    tryAgainButton = new JButton("Try Again");
    tryAgainButton.setFont(Toolkit.getSansFont(14, Font.PLAIN));
//    tryAgainButton.setContentAreaFilled(false);
//    tryAgainButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1),BorderFactory.createEmptyBorder(3, 0, 3, 0)));
    tryAgainButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        contributionManagerDialog.makeAndShowTab(false, true);
        contributionManagerDialog.downloadAndUpdateContributionListing(editor.getBase());
      }
    });
    layout.setHorizontalGroup(layout
      .createSequentialGroup()
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
    layout.setVerticalGroup(layout
      .createSequentialGroup()
      .addGroup(layout.createParallelGroup().addComponent(errorMessage)
                  .addComponent(closeButton)).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(tryAgainButton));
    errorPanel.setBackground(Color.PINK);
    errorPanel.validate();
  }



  protected void updateCategoryChooser() {
    if (categoryChooser != null) {
      ArrayList<String> categories;
      categoryChooser.removeAllItems();
      categories = new ArrayList<String>(contribListing.getCategories(filter));
//      for (int i = 0; i < categories.size(); i++) {
//        System.out.println(i + " category: " + categories.get(i));
//      }
      Collections.sort(categories);
//    categories.add(0, ContributionManagerDialog.ANY_CATEGORY);
      boolean categoriesFound = false;
      categoryChooser.addItem(ContributionManagerDialog.ANY_CATEGORY);
      for (String s : categories) {
        categoryChooser.addItem(s);
        if (!s.equals(Contribution.UNKNOWN_CATEGORY)) {
          categoriesFound = true;
        }
      }
      categoryChooser.setEnabled(categoriesFound);
    }
  }


  protected void filterLibraries(String category, List<String> filters) {
    List<Contribution> filteredLibraries =
      contribListing.getFilteredLibraryList(category, filters);
    contributionListPanel.filterLibraries(filteredLibraries);
  }

/*
  protected void filterLibraries(String category, List<String> filters, boolean isCompatibilityFilter) {
    List<Contribution> filteredLibraries =
      contribListing.getFilteredLibraryList(category, filters);
    filteredLibraries = contribListing.getCompatibleContributionList(filteredLibraries, isCompatibilityFilter);
    contributionListPanel.filterLibraries(filteredLibraries);
  }*/


  protected void updateContributionListing() {
    if (editor != null) {
      List<Contribution> contributions = new ArrayList<Contribution>();

      List<Library> libraries =
        new ArrayList<Library>(editor.getMode().contribLibraries);
      contributions.addAll(libraries);

      //ArrayList<ToolContribution> tools = editor.contribTools;
      List<ToolContribution> tools = editor.getToolContribs();
      contributions.addAll(tools);

      List<ModeContribution> modes = editor.getBase().getModeContribs();
      contributions.addAll(modes);

      List<ExamplesContribution> examples = editor.getBase().getExampleContribs();
      contributions.addAll(examples);

//    ArrayList<LibraryCompilation> compilations = LibraryCompilation.list(libraries);
//
//    // Remove libraries from the list that are part of a compilations
//    for (LibraryCompilation compilation : compilations) {
//      Iterator<Library> it = libraries.iterator();
//      while (it.hasNext()) {
//        Library current = it.next();
//        if (compilation.getFolder().equals(current.getFolder().getParentFile())) {
//          it.remove();
//        }
//      }
//    }

      contribListing.updateInstalledList(contributions);
    }
  }

  protected void setFilterText(String filter) {
    if (filter == null || filter.isEmpty()) {
      filterField.setText("");
    } else {
      filterField.setText(filter);
    }
    filterField.applyFilter();
  }


//  private JPanel getPlaceholder() {
//    return contributionListPanel.statusPlaceholder;
//  }

  //TODO: this is causing a lot of bugs as the hint is wrongly firing applyFilter()
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
      searchIcon = Toolkit.getLibIcon("manager/search.png");
      setOpaque(false);
      setBorder(BorderFactory.createMatteBorder(0, 33, 0, 0, searchIcon));

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
            setBorder(BorderFactory.createMatteBorder(0, 33, 0, 0, searchIcon));
            filterLabel.setVisible(true);
          }
        }

        public void focusGained(FocusEvent focusEvent) {
          setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
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

//      contributionListPanel.updateColors();
    }

  }


  public boolean hasAlreadyBeenOpened() {
    return panel != null;
  }

  public void updateStatusPanel(ContributionPanel contributionPanel) {
    statusPanel.update(contributionPanel);
  }
}
