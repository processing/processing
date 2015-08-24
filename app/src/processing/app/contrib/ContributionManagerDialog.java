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
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

import processing.app.*;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;


/**
 * This class is the main Contribution Manager Dialog.
 * It contains all the contributions tab and the update tab.
 */
public class ContributionManagerDialog {
  static final String ANY_CATEGORY = Language.text("contrib.all");
  static final int TAB_WIDTH = 100;
  static final int TAB_HEIGHT = 34;
  static final int AUTHOR_WIDTH = 240;
  static final int STATUS_WIDTH = 66;
  static final int FILTER_WIDTH = 180;
  static final int ICON_WIDTH = 50;

  JFrame dialog;
  JTabbedPane tabbedPane;
  String title;
  JButton restartButton;

  // the calling editor, so updates can be applied
  Editor editor;

  //The tabs
  ContributionTab toolsContributionTab;
  ContributionTab librariesContributionTab;
  ContributionTab examplesContributionTab;
  ContributionTab modesContributionTab;
  UpdateContributionTab updatesContributionTab;
  JLabel numberLabel;

  ContributionListing contributionListing = ContributionListing.getInstance();

  private JLabel[] tabLabels;

  private JPanel updateTabPanel;

  private JLabel updateTabLabel;

  static Font myFont;
  // disabling this because the icon sizing is not implemented correctly
  //static int iconVer = Toolkit.highResDisplay() ? 2 : 1;
  static final int iconVer = 1;


  public ContributionManagerDialog() {
    myFont = Toolkit.getSansFont(14, Font.PLAIN);
//    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(myFont);
    numberLabel = new JLabel(Toolkit.getLibIcon("manager/notification-" + iconVer + "x.png"), SwingConstants.CENTER);
    toolsContributionTab = new ContributionTab(ContributionType.TOOL, this);
    librariesContributionTab = new ContributionTab(ContributionType.LIBRARY, this);
    modesContributionTab = new ContributionTab(ContributionType.MODE, this);
    examplesContributionTab = new ContributionTab(ContributionType.EXAMPLES, this);
    updatesContributionTab = new UpdateContributionTab(null, this);
  }


  /*
  public boolean hasUpdates() {
    return toolsContributionTab.hasUpdates()
      || librariesContributionTab.hasUpdates()
      || examplesContributionTab.hasUpdates()
      || modesContributionTab.hasUpdates();
  }
  */


  public boolean hasUpdates(Base base) {
    return toolsContributionTab.hasUpdates(base)
      || modesContributionTab.hasUpdates(base)
      || librariesContributionTab.hasUpdates(base)
      || examplesContributionTab.hasUpdates(base);
  }


  public void showFrame(Editor editor, ContributionType contributionType) {
    this.editor = editor;

    //Calculating index to switch to the required tab
    int index;
    if (contributionType == ContributionType.TOOL) {
      index = 0;
    } else if (contributionType == ContributionType.LIBRARY) {
      index = 1;
    } else if (contributionType == ContributionType.MODE) {
      index = 2;
    } else if (contributionType == ContributionType.EXAMPLES) {
      index = 3;
    } else {
      index = 4;
    }
    if (dialog == null) {
      makeFrame(editor);
      // done before as downloadAndUpdateContributionListing()
      // requires the current selected tab
      tabbedPane.setSelectedIndex(index);
      downloadAndUpdateContributionListing(editor.getBase());
      if (index != 4) {
        Component selected =
          tabbedPane.getTabComponentAt(tabbedPane.getSelectedIndex());
        selected.setBackground(new Color(0xe0fffd));
        selected.setForeground(Color.BLACK);
      } else {
        updateTabPanel.setBackground(new Color(0xe0fffd));
        updateTabLabel.setForeground(Color.BLACK);
      }
    }
    tabbedPane.setSelectedIndex(index);
    dialog.setVisible(true);
  }


  public void makeFrame(final Editor editor) {
    dialog = new JFrame(title);
    dialog.setMinimumSize(new Dimension(750, 500));
    tabbedPane = new JTabbedPane();

    makeAndShowTab(false, true);

    tabbedPane.addTab("Tools", null, toolsContributionTab.panel, "Tools");
    tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

    tabbedPane.addTab("Libraries", null, librariesContributionTab.panel,
                      "Libraries");
    tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

    tabbedPane.addTab("Modes", null, modesContributionTab.panel, "Modes");
    tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

    tabbedPane.addTab("Examples", null, examplesContributionTab.panel,
                      "Examples");
    tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);

    tabbedPane.addTab("Updates", null, updatesContributionTab.panel, "Updates");
    tabbedPane.setMnemonicAt(3, KeyEvent.VK_5);
    tabbedPane.setUI(new SpacedTabbedPaneUI());
    tabbedPane.setBackground(new Color(0x132638));
    tabbedPane.setOpaque(true);
//    tabbedPane.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

    makeAndSetTabComponents();

    tabbedPane.addChangeListener(new ChangeListener() {

      @Override
      public void stateChanged(ChangeEvent e) {
        for(int i = 0 ; i < 4; i++){
          tabLabels[i].setBackground(new Color(0x2d4251));
          tabLabels[i].setForeground(Color.WHITE);
        }
        updateTabPanel.setBackground(new Color(0x2d4251));
        updateTabLabel.setForeground(Color.WHITE);
        int currentIndex = tabbedPane.getSelectedIndex();
        if(currentIndex != 4){
          tabbedPane.getTabComponentAt(tabbedPane.getSelectedIndex()).setBackground(new Color(0xe0fffd));
          tabbedPane.getTabComponentAt(tabbedPane.getSelectedIndex()).setForeground(Color.BLACK);
        }else{
          updateTabPanel.setBackground(new Color(0xe0fffd));
          updateTabLabel.setForeground(Color.BLACK);
        }
        getActiveTab().contributionListPanel.scrollPane.requestFocusInWindow();
//        // When the tab is changed update status to the current selected panel
//        ContributionPanel currentPanel = getActiveTab().contributionListPanel
//          .getSelectedPanel();
//        if (currentPanel != null) {
//          getActiveTab().contributionListPanel.setSelectedPanel(currentPanel);
//        }
      }
    });


//    tabbedPane.setSize(450, 400);
    setLayout();

    restartButton = new JButton(Language.text("contrib.restart"));
    restartButton.setVisible(false);
    restartButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent arg0) {

        Iterator<Editor> iter = editor.getBase().getEditors().iterator();
        while (iter.hasNext()) {
          Editor ed = iter.next();
          if (ed.getSketch().isModified()) {
            int option = Messages.showYesNoQuestion(editor, title, Language
              .text("contrib.unsaved_changes"), Language
              .text("contrib.unsaved_changes.prompt"));

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

    });

    Toolkit.setIcon(dialog);
    registerDisposeListeners();

    dialog.pack();
    dialog.setLocationRelativeTo(null);
  }


  private void makeAndSetTabComponents() {
    final String[] tabTitles = {
      "Tools", "Libraries", "Modes", "Examples", "Updates"
    };
    tabLabels = new JLabel[4];

    for(int i = 0 ; i < 4; i++){
      final int temp = i;
      tabLabels[i] = new JLabel(tabTitles[i]){
        @Override
        protected void paintComponent(Graphics g) {
          g.setClip(Toolkit.createRoundRect(0, 0,
                                            getWidth(), getHeight(),
                                            temp == 0 ? 6 : 0,
                                            temp == 3 ? 6 : 0,
                                            0, 0));
          super.paintComponent(g);
        }
      };
      tabLabels[i].setForeground(Color.WHITE);
      tabLabels[i].setBackground(new Color(0x2d4251));
      tabLabels[i].setOpaque(true);
      tabLabels[i].setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
      tabLabels[i].setPreferredSize(new Dimension(TAB_WIDTH, TAB_HEIGHT));
      tabLabels[i].setHorizontalAlignment(SwingConstants.CENTER);
      tabLabels[i].setFont(Toolkit.getSansFont(14, Font.BOLD));
      tabbedPane.setTabComponentAt(i, tabLabels[i]);
    }

    updateTabPanel = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        g.setClip(Toolkit.createRoundRect(0, 0, getWidth(), getHeight(),
                                          6, 6, 0, 0));
        super.paintComponent(g);
      }
    };;
    updateTabLabel = new JLabel("Updates");
    updateTabLabel.setFont(Toolkit.getSansFont(14, Font.BOLD));
    numberLabel.setVerticalTextPosition(SwingConstants.CENTER);
    numberLabel.setHorizontalTextPosition(SwingConstants.CENTER);
    numberLabel.setFont(Toolkit.getSansFont(14, Font.BOLD));
    numberLabel.setForeground(Color.WHITE);
    updateTabPanel.setOpaque(true);
    updateTabPanel.setBackground(new Color(0x2d4251));
    updateTabLabel.setForeground(Color.WHITE);
    updateTabPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
    updateTabPanel.setPreferredSize(new Dimension(TAB_WIDTH, TAB_HEIGHT));
//    updateTabPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory
//      .createMatteBorder(0, 2, 0, 0, Color.BLACK), BorderFactory
//      .createEmptyBorder(4, 4, 4, 4)));
    tabbedPane.setTabComponentAt(4, updateTabPanel);

//    JLabel tabLabels[] = new JLabel[4];
//    for(int i = 0 ; i < tabLabels.length;i++){
//      tabLabels[i] = new JLabel(tabTitles[i]);
//      tabLabels[i]
//        .setBorder(BorderFactory.createCompoundBorder(BorderFactory
//          .createMatteBorder(0, (i == 0 ? 0 : 2), 0, (i == 3 ? 2 : 0),
//                             Color.BLACK), BorderFactory
//          .createEmptyBorder(4, 4, 4, 4)));
//      tabbedPane.setTabComponentAt(i, tabLabels[i]);
//    }

    GroupLayout tabLayout = new GroupLayout(updateTabPanel);
    tabLayout.setAutoCreateGaps(true);
    updateTabPanel.setLayout(tabLayout);
    tabLayout.setHorizontalGroup(tabLayout
      .createSequentialGroup()
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
      .addComponent(updateTabLabel)
      .addComponent(numberLabel)
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
    tabLayout.setVerticalGroup(tabLayout
      .createParallelGroup(GroupLayout.Alignment.CENTER)
      .addComponent(numberLabel).addComponent(updateTabLabel));

    numberLabel.setVisible(false);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public class SpacedTabbedPaneUI extends BasicTabbedPaneUI {
    protected Color hazAlfa(int fila) {
      int alfa = 0;
      if (fila >= 0) {
          alfa = 50 + (fila > 7 ? 70 : 10 * fila);
      }
      return new Color(0, 0, 0, alfa);
    }


    @Override
    protected void installDefaults() {
      UIManager.put("TabbedPane.selected", Color.BLACK);
      UIManager.put("TabbedPane.tabsOverlapBorder" , true);
      super.installDefaults();
      tabInsets = new Insets(0, 0, 0, 0);
      contentBorderInsets = new Insets(0, 0, 0, 0);
      tabAreaInsets = new Insets(0, 0, 0, 0);
      selectedTabPadInsets = new Insets(0, 0, 0, 0);
    }
//    @Override
//    protected int getTabLabelShiftX(int tabPlacement, int tabIndex,
//                                    boolean isSelected) {
//      return 0;
//    }
    @Override
    protected int getTabLabelShiftY(int tabPlacement, int tabIndex,
                                    boolean isSelected) {
      return 1;
    }
//    @Override
//    protected void paintTab(Graphics g, int tabPlacement,
//                            Rectangle[] rects, int tabIndex,
//                            Rectangle iconRect, Rectangle textRect) {
//      Graphics2D g2 = (Graphics2D) g;
//      g2.fill(Toolkit.createRoundRect(rects[tabIndex].x, rects[tabIndex].y,
//                                      rects[tabIndex].x + rects[tabIndex].width, rects[tabIndex].y + rects[tabIndex].height,
//                                      6,
//                                      6,
//                                      0, 0));
//    }
    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement,
                                      int tabIndex, int x, int y, int w, int h,
                                      boolean isSelected) {
      return;
    }
    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h, boolean isSelected) {
      return;
    }
    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                       Rectangle[] rects, int tabIndex,
                                       Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected) {
      return;
    }

    @Override
    protected LayoutManager createLayoutManager() {
      return new BasicTabbedPaneUI.TabbedPaneLayout() {

        @Override
        public void addLayoutComponent(String name, Component comp) {
          // TODO Auto-generated method stub
          super.addLayoutComponent(name, comp);
        }
        @Override
        protected void calculateTabRects(int tabPlacement, int tabCount) {
          super.calculateTabRects(tabPlacement, tabCount);
          rects[0].x -= 2;
          rects[1].x -= 1;
          rects[2].x -= 1;
          rects[3].x -= 1;
          rects[4].x = tabbedPane.getWidth() - rects[4].width + 1;
        }
      };
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  private void setLayout() {
    GroupLayout layout = new GroupLayout(dialog.getContentPane());
    dialog.getContentPane().setLayout(layout);
    dialog.setResizable(true);
    layout.setAutoCreateContainerGaps(true);
    layout.setHorizontalGroup(layout.createParallelGroup().addComponent(tabbedPane));
    layout.setVerticalGroup(layout.createParallelGroup().addComponent(tabbedPane));
    layout.setHonorsVisibility(tabbedPane, true);
    //TODO set color here
    dialog.getContentPane().setBackground(new Color(0x132638));
    dialog.validate();
    dialog.repaint();
  }


  /**
   * Close the window after an OK or Cancel.
   */
  protected void disposeFrame() {
    dialog.dispose();
    editor = null;
  }


/*  *//**
   * Creates and arranges the Swing components in the dialog.
   *
   * @param panel1
   *//*
  private void createComponents(JPanel panel1) {
    dialog.setResizable(true);

    Container pane = panel1;
//    pane.setLayout(new GridBagLayout());
//
//    { // Shows "Filter by Category" and the combo box for selecting a category
//      GridBagConstraints c = new GridBagConstraints();
//      c.gridx = 0;
//      c.gridy = 0;
    pane.setLayout(new BorderLayout());

    JPanel filterPanel = new JPanel();
    filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.X_AXIS));
//      pane.add(filterPanel, c);
    pane.add(filterPanel, BorderLayout.NORTH);

    filterPanel.add(Box.createHorizontalStrut(6));

    JLabel categoryLabel = new JLabel(Language.text("contrib.category"));
    filterPanel.add(categoryLabel);

    filterPanel.add(Box.createHorizontalStrut(5));

    categoryChooser = new JComboBox<String>();
    categoryChooser.setMaximumRowCount(20);
    updateCategoryChooser();
//      filterPanel.add(categoryChooser, c);
    filterPanel.add(categoryChooser);
    categoryChooser.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        category = (String) categoryChooser.getSelectedItem();
        if (ContributionManagerDialog.ANY_CATEGORY.equals(category)) {
          category = null;
        }
        filterLibraries(category, filterField.filters, isCompatibilityFilter);
        contributionListPanel.updateColors();
      }
    });

    filterPanel.add(Box.createHorizontalStrut(5));
//      filterPanel.add(Box.createHorizontalGlue());
    filterField = new FilterField();
    filterPanel.add(filterField);

    filterPanel.add(Box.createHorizontalStrut(5));

    final JCheckBox compatibleContrib = new JCheckBox(compatibleCheckboxLabel);
    compatibleContrib.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent arg0) {
        isCompatibilityFilter = compatibleContrib.isSelected();
        filterLibraries(category, filterField.filters, isCompatibilityFilter);
        contributionListPanel.updateColors();
      }
    });
    filterPanel.add(compatibleContrib);
//      filterPanel.add(Box.createHorizontalGlue());
//    }
    //filterPanel.setBorder(new EmptyBorder(13, 13, 13, 13));
    filterPanel.setBorder(new EmptyBorder(7, 7, 7, 7));

//    { // The scroll area containing the contribution listing and the status bar.
//      GridBagConstraints c = new GridBagConstraints();
//      c.fill = GridBagConstraints.BOTH;
//      c.gridx = 0;
//      c.gridy = 1;
//      c.gridwidth = 2;
//      c.weighty = 1;
//      c.weightx = 1;

    scrollPane = new JScrollPane();
    scrollPane.setPreferredSize(new Dimension(300, 300));
    scrollPane.setViewportView(contributionListPanel);
//      scrollPane.getViewport().setOpaque(true);
//      scrollPane.getViewport().setBackground(contributionListPanel.getBackground());
    scrollPane
      .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane
      .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//      scrollPane.setBorder(new EmptyBorder(0, 7, 0, 7));
    pane.add(scrollPane, BorderLayout.CENTER);

    pane.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    pane.add(Box.createHorizontalStrut(10), BorderLayout.EAST);

    status = new StatusPanel();
//      status.setBorder(new EmptyBorder(7, 7, 7, 7));

    JPanel statusRestartPane = new JPanel();
    statusRestartPane.setLayout(new BorderLayout());

    statusRestartPane.setBorder(new EmptyBorder(7, 7, 7, 7));
    statusRestartPane.setOpaque(false);

    statusRestartPane.add(status, BorderLayout.WEST);
    statusRestartPane.add(progressBar, BorderLayout.LINE_END);

     Adding both of these to EAST shouldn't pose too much of a problem,
    // since they can never get added together.
    statusRestartPane.add(restartButton, BorderLayout.EAST);
    statusRestartPane.add(retryConnectingButton, BorderLayout.EAST);

    pane.add(statusRestartPane, BorderLayout.SOUTH);

//      status = new StatusPanel();
//      status.setBorder(BorderFactory.createEtchedBorder());

//      final JLayeredPane layeredPane = new JLayeredPane();
//      layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);
//      layeredPane.add(status, JLayeredPane.PALETTE_LAYER);
//
//      layeredPane.addComponentListener(new ComponentAdapter() {
//
//        void resizeLayers() {
//          scrollPane.setSize(layeredPane.getSize());
//          scrollPane.updateUI();
//        }
//
//        public void componentShown(ComponentEvent e) {
//          resizeLayers();
//        }
//
//        public void componentResized(ComponentEvent arg0) {
//          resizeLayers();
//        }
//      });
//
//      final JViewport viewport = scrollPane.getViewport();
//      viewport.addComponentListener(new ComponentAdapter() {
//        void resizeLayers() {
//          status.setLocation(0, viewport.getHeight() - 18);
//
//          Dimension d = viewport.getSize();
//          d.height = 20;
//          d.width += 3;
//          status.setSize(d);
//        }
//        public void componentShown(ComponentEvent e) {
//          resizeLayers();
//        }
//        public void componentResized(ComponentEvent e) {
//          resizeLayers();
//        }
//      });
//
//      pane.add(layeredPane, c);
//    }

//    { // The filter text area
//      GridBagConstraints c = new GridBagConstraints();
//      c.gridx = 0;
//      c.gridy = 2;
//      c.gridwidth = 2;
//      c.weightx = 1;
//      c.fill = GridBagConstraints.HORIZONTAL;
//      filterField = new FilterField();
//
//      pane.add(filterField, c);
//    }

    dialog.setMinimumSize(new Dimension(450, 400));
  }

  private void updateCategoryChooser() {
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
        if (!s.equals("Unknown")) {
          categoriesFound = true;
        }
      }
      categoryChooser.setEnabled(categoriesFound);
    }
  }
*/


  private void registerDisposeListeners() {
    dialog.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        disposeFrame();
      }
    });
    // handle window closing commands for ctrl/cmd-W or hitting ESC.
    Toolkit.registerWindowCloseKeys(dialog.getRootPane(), new ActionListener() {
      public void actionPerformed(ActionEvent actionEvent) {
        disposeFrame();
      }
    });

    dialog.getContentPane().addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        //System.out.println(e);
        KeyStroke wc = Toolkit.WINDOW_CLOSE_KEYSTROKE;
        if ((e.getKeyCode() == KeyEvent.VK_ESCAPE)
          || (KeyStroke.getKeyStrokeForEvent(e).equals(wc))) {
          disposeFrame();
        }
      }
    });
  }


  /*protected void filterLibraries(String category, List<String> filters) {
    List<Contribution> filteredLibraries = contribListing
      .getFilteredLibraryList(category, filters);
    contributionListPanel.filterLibraries(filteredLibraries);
  }

  protected void filterLibraries(String category, List<String> filters,
                                 boolean isCompatibilityFilter) {
    List<Contribution> filteredLibraries = contribListing
      .getFilteredLibraryList(category, filters);
    filteredLibraries = contribListing
      .getCompatibleContributionList(filteredLibraries, isCompatibilityFilter);
    contributionListPanel.filterLibraries(filteredLibraries);
  }
*/
  /*protected void updateContributionListing() {
    if (editor != null) {
      ArrayList<Contribution> contributions = new ArrayList<Contribution>();

      ArrayList<Library> libraries = new ArrayList<Library>(
                                                            editor.getMode().contribLibraries);
      contributions.addAll(libraries);

      //ArrayList<ToolContribution> tools = editor.contribTools;
      List<ToolContribution> tools = editor.getToolContribs();
      contributions.addAll(tools);

      List<ModeContribution> modes = editor.getBase().getModeContribs();
      contributions.addAll(modes);

      List<ExamplesContribution> examples = editor.getBase()
        .getExampleContribs();
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
*/


  protected void downloadAndUpdateContributionListing(Base base) {
    //activeTab is required now but should be removed
    //as there is only one instance of contribListing and it should be present in this class
    final ContributionTab activeTab = getActiveTab();
    activeTab.contribListing.downloadAvailableList(base, new ContribProgressBar(
      activeTab.progressBar) {

      @Override
      public void startTask(String name, int maxValue) {
        super.startTask(name, maxValue);
        progressBar.setVisible(true);
        progressBar.setString(null);
      }

      @Override
      public void setProgress(int value) {
        super.setProgress(value);
//        int percent = 100 * value / this.max;
        progressBar.setValue(value);
      }

      @Override
      public void finishedAction() {
        progressBar.setVisible(false);
        activeTab.updateContributionListing();
        activeTab.updateCategoryChooser();


        if (error) {
          exception.printStackTrace();
          makeAndShowTab(true,false);
        } else {
          makeAndShowTab(false, false);
        }
      }
    });
  }


  void makeAndShowTab(boolean activateErrorPanel, boolean isLoading) {
    toolsContributionTab.showFrame(editor, activateErrorPanel, isLoading);
    librariesContributionTab.showFrame(editor, activateErrorPanel, isLoading);
    modesContributionTab.showFrame(editor, activateErrorPanel, isLoading);
    examplesContributionTab.showFrame(editor, activateErrorPanel, isLoading);
    updatesContributionTab.showFrame(editor, activateErrorPanel, isLoading);
  }


  /**
   *
   * @return the currently selected tab
   */
  public ContributionTab getActiveTab() {
    switch (tabbedPane.getSelectedIndex()) {
    case 0:
      return toolsContributionTab;
    case 1:
      return librariesContributionTab;
    case 2:
      return modesContributionTab;
    case 3:
      return examplesContributionTab;
    default:
      return updatesContributionTab;
    }
  }


/*
  protected void setFilterText(String filter) {
    if (filter == null || filter.isEmpty()) {
      filterField.setText("");
      filterField.showingHint = true;
    } else {
      filterField.setText(filter);
      filterField.showingHint = false;
    }
    filterField.applyFilter();
  }

//  private JPanel getPlaceholder() {
//    return contributionListPanel.statusPlaceholder;
//  }

  class FilterField extends JTextField {
    String filterHint;

    boolean showingHint;

    List<String> filters;

    public FilterField() {
      super(Language.text("contrib.filter_your_search"));
      filterHint = Language.text("contrib.filter_your_search");

      showingHint = true;
      filters = new ArrayList<String>();
      updateStyle();

      addFocusListener(new FocusListener() {
        public void focusLost(FocusEvent focusEvent) {
          if (filterField.getText().isEmpty()) {
            showingHint = true;
          }
          updateStyle();
        }

        public void focusGained(FocusEvent focusEvent) {
          if (showingHint) {
            showingHint = false;
            filterField.setText("");
          }
          updateStyle();
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
      String filter = filterField.getFilterText();
      filter = filter.toLowerCase();

      // Replace anything but 0-9, a-z, or : with a space
      filter = filter.replaceAll("[^\\x30-\\x39^\\x61-\\x7a^\\x3a]", " ");
      filters = Arrays.asList(filter.split(" "));
      filterLibraries(category, filters, isCompatibilityFilter);

      contributionListPanel.updateColors();
    }

    public String getFilterText() {
      return showingHint ? "" : getText();
    }

    public void updateStyle() {
      if (showingHint) {
        setText(filterHint);
        // setForeground(UIManager.getColor("TextField.light")); // too light
        setForeground(Color.gray);
        setFont(getFont().deriveFont(Font.ITALIC));
      } else {
        setForeground(UIManager.getColor("TextField.foreground"));
        setFont(getFont().deriveFont(Font.PLAIN));
      }
    }
  }*/


  public boolean hasAlreadyBeenOpened() {
    return dialog != null;
  }
}
