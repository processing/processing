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
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import processing.app.*;
import processing.app.ui.Editor;
import processing.app.ui.Toolkit;


/**
 * This class is the main Contribution Manager Dialog.
 * It contains all the contributions tab and the update tab.
 */
public class ManagerFrame {
  static final String ANY_CATEGORY = Language.text("contrib.all");

//  static final int TAB_WIDTH = 100;
//  static final int TAB_HEIGHT = 34;
  static final int AUTHOR_WIDTH = 240;
  static final int STATUS_WIDTH = 66;

  static final String title = "Contribution Manager";

  Base base;
  JFrame frame;
//  JTabbedPane tabbedPane;
  ManagerTabs tabs;

//   the calling editor, so updates can be applied
//  Editor editor;

  ContributionTab librariesTab;
  ContributionTab modesTab;
  ContributionTab toolsTab;
  ContributionTab examplesTab;
  UpdateContributionTab updatesTab;
  
  static ContributionListing contributionListing;
//  JLabel numberLabel;

//  private JLabel[] tabLabels;
//  private JPanel updateTabPanel;
//  private JLabel updateTabLabel;


  public ManagerFrame(Base base) {
    this.base = base;

//    numberLabel = new JLabel(Toolkit.getLibIconX("manager/notification"));
    contributionListing = ContributionListing.getInstance();
    librariesTab = new ContributionTab(this, ContributionType.LIBRARY);
    modesTab = new ContributionTab(this, ContributionType.MODE);
    toolsTab = new ContributionTab(this, ContributionType.TOOL);
    examplesTab = new ContributionTab(this, ContributionType.EXAMPLES);
    updatesTab = new UpdateContributionTab(this, null);
  }


  // TODO remove this Editor... need to use Base.getActiveEditor()
  // The editor may be closed while still running the contrib manager
  public void showFrame(ContributionType contributionType) {
//    this.editor = editor;

    ContributionTab showTab = getTab(contributionType);
    if (frame == null) {
      makeFrame();
      // done before as downloadAndUpdateContributionListing()
      // requires the current selected tab
      tabs.setPanel(showTab);
      downloadAndUpdateContributionListing(base);
    } else {
      tabs.setPanel(showTab);
    }
    frame.setVisible(true);
    // Avoid the search box taking focus and hiding the 'search' text
    tabs.requestFocusInWindow();
  }


  private void makeFrame() {
    frame = new JFrame(title);
    frame.setMinimumSize(new Dimension(750, 500));
    tabs = new ManagerTabs(base);

    makeAndShowTab(false, true);

    tabs.addPanel(librariesTab, "Libraries");
    tabs.addPanel(modesTab, "Modes");
    tabs.addPanel(toolsTab, "Tools");
    tabs.addPanel(examplesTab, "Examples");
    tabs.addPanel(updatesTab, "Updates");

    /*
    tabbedPane.addTab("Libraries", null, librariesTab, "Libraries");
    tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

    tabbedPane.addTab("Modes", null, modesTab, "Modes");
    tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

    tabbedPane.addTab("Tools", null, toolsTab, "Tools");
    tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

    tabbedPane.addTab("Examples", null, examplesTab, "Examples");
    tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);

    tabbedPane.addTab("Updates", null, updatesTab, "Updates");
    tabbedPane.setMnemonicAt(4, KeyEvent.VK_5);

    tabbedPane.setUI(new SpacedTabbedPaneUI());
    tabbedPane.setBackground(new Color(0x132638));
    tabbedPane.setOpaque(true);

    for (int i = 0; i < 5; i++) {
      tabbedPane.setToolTipTextAt(i, null);
    }
    */

    /*
    final String[] tabTitles = {
      "Libraries", "Modes", "Tools", "Examples", "Updates"
    };
    tabLabels = new JLabel[4];

    for (int i = 0 ; i < 4; i++) {
      final int temp = i;
      tabLabels[i] = new JLabel(tabTitles[i]) {
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
    */

    /*
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
    tabbedPane.setTabComponentAt(4, updateTabPanel);
    */

    /*
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
    */

    /*
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
      }
    });
    */

    frame.setResizable(true);
//    tabbedPane.setBorder(new EmptyBorder(BORDER, BORDER, BORDER, BORDER));

    Container c = frame.getContentPane();
    c.add(tabs);
    c.setBackground(base.getDefaultMode().getColor("manager.tab.background"));

    frame.validate();
    frame.repaint();

    Toolkit.setIcon(frame);
    registerDisposeListeners();

    frame.pack();
    frame.setLocationRelativeTo(null);
  }


  /**
   * Close the window after an OK or Cancel.
   */
  protected void disposeFrame() {
    frame.dispose();
//    editor = null;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /*
  private class SpacedTabbedPaneUI extends BasicTabbedPaneUI {

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


    @Override
    protected int getTabLabelShiftY(int tabPlacement, int tabIndex,
                                    boolean isSelected) {
      return 1;
    }


    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement,
                                      int tabIndex, int x, int y, int w, int h,
                                      boolean isSelected) {
    }


    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                  int x, int y, int w, int h, boolean isSelected) {
    }


    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                       Rectangle[] rects, int tabIndex,
                                       Rectangle iconRect, Rectangle textRect,
                                       boolean isSelected) {
    }


    @Override
    protected LayoutManager createLayoutManager() {
      return new BasicTabbedPaneUI.TabbedPaneLayout() {

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
  */


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  private void registerDisposeListeners() {
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        disposeFrame();
      }
    });
    // handle window closing commands for ctrl/cmd-W or hitting ESC.
    Toolkit.registerWindowCloseKeys(frame.getRootPane(), new ActionListener() {
      public void actionPerformed(ActionEvent actionEvent) {
        disposeFrame();
      }
    });

    frame.getContentPane().addKeyListener(new KeyAdapter() {
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


  void downloadAndUpdateContributionListing(Base base) {
    final ContributionTab activeTab = getActiveTab();

    ContribProgressMonitor progress =
      new ContribProgressBar(activeTab.progressBar) {

      @Override
      public void finishedAction() {
        updateContributionListing();

        if (error) {
          exception.printStackTrace();
          makeAndShowTab(true, false);
        } else {
          makeAndShowTab(false, false);
        }
      }
    };
    contributionListing.downloadAvailableList(base, progress);
  }


  void makeAndShowTab(boolean error, boolean loading) {
    Editor editor = base.getActiveEditor();
    librariesTab.showFrame(editor, error, loading);
    modesTab.showFrame(editor, error, loading);
    toolsTab.showFrame(editor, error, loading);
    examplesTab.showFrame(editor, error, loading);
    updatesTab.showFrame(editor, error, loading);
  }


  protected ContributionTab getTab(ContributionType contributionType) {
    if (contributionType == ContributionType.LIBRARY) {
      return librariesTab;
    } else if (contributionType == ContributionType.MODE) {
      return modesTab;
    } else if (contributionType == ContributionType.TOOL) {
      return toolsTab;
    } else if (contributionType == ContributionType.EXAMPLES) {
      return examplesTab;
    }
    return updatesTab;
  }


  ContributionTab getActiveTab() {
    return (ContributionTab) tabs.getPanel();
  }
  
  protected void updateContributionListing() {
    if (base.getActiveEditor() != null) {
      List<Contribution> contributions = new ArrayList<Contribution>();

      List<Library> libraries =
        new ArrayList<Library>(base.getActiveEditor().getMode().contribLibraries);

      // Only add core libraries that are installed in the sketchbook
      // https://github.com/processing/processing/issues/3688
      //libraries.addAll(editor.getMode().coreLibraries);
      final String sketchbookPath =
        Base.getSketchbookLibrariesFolder().getAbsolutePath();
      for (Library lib : base.getActiveEditor().getMode().coreLibraries) {
        if (lib.getLibraryPath().startsWith(sketchbookPath)) {
          libraries.add(lib);
        }
      }

      contributions.addAll(libraries);

      List<ToolContribution> tools = base.getToolContribs();
      contributions.addAll(tools);

      List<ModeContribution> modes = base.getModeContribs();
      contributions.addAll(modes);

      List<ExamplesContribution> examples = base.getExampleContribs();
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

      contributionListing.updateInstalledList(contributions);
    }
  }
}
