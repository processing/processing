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

  static final String title = "Manager";

  JFrame dialog;
  JTabbedPane tabbedPane;
  JButton restartButton;

  // the calling editor, so updates can be applied
  Editor editor;

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

  static Font font;


  public ContributionManagerDialog() {
    font = Toolkit.getSansFont(14, Font.PLAIN);
    numberLabel = new JLabel(Toolkit.getLibIconX("manager/notification"), SwingConstants.CENTER);
    librariesContributionTab = new ContributionTab(ContributionType.LIBRARY, this);
    modesContributionTab = new ContributionTab(ContributionType.MODE, this);
    toolsContributionTab = new ContributionTab(ContributionType.TOOL, this);
    examplesContributionTab = new ContributionTab(ContributionType.EXAMPLES, this);
    updatesContributionTab = new UpdateContributionTab(null, this);
  }


  /*
  public boolean hasUpdates(Base base) {
    return librariesContributionTab.hasUpdates(base)
      || modesContributionTab.hasUpdates(base)
      || toolsContributionTab.hasUpdates(base)
      || examplesContributionTab.hasUpdates(base);
  }
   */


  public void showFrame(Editor editor, ContributionType contributionType) {
    this.editor = editor;

    // Calculating index to switch to the required tab
    int index = getIndex(contributionType);
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


  int getIndex(ContributionType contributionType) {
    int index;
    if (contributionType == ContributionType.LIBRARY) {
      index = 0;
    } else if (contributionType == ContributionType.MODE) {
      index = 1;
    } else if (contributionType == ContributionType.TOOL) {
      index = 2;
    } else if (contributionType == ContributionType.EXAMPLES) {
      index = 3;
    } else {
      index = 4;
    }
    return index;
  }
  
  ContributionTab getTab(ContributionType contributionType) {
    if (contributionType == ContributionType.LIBRARY) {
      return librariesContributionTab;
    } else if (contributionType == ContributionType.MODE) {
      return modesContributionTab;
    } else if (contributionType == ContributionType.TOOL) {
      return toolsContributionTab;
    } else if (contributionType == ContributionType.EXAMPLES) {
      return examplesContributionTab;
    }
    return updatesContributionTab;
  }


  private void makeFrame(final Editor editor) {
    dialog = new JFrame(title);
    dialog.setMinimumSize(new Dimension(750, 500));
    tabbedPane = new JTabbedPane();

    makeAndShowTab(false, true);

    tabbedPane.addTab("Libraries", null, librariesContributionTab.panel, "Libraries");
    tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

    tabbedPane.addTab("Modes", null, modesContributionTab.panel,
                      "Modes");
    tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

    tabbedPane.addTab("Tools", null, toolsContributionTab.panel, "Tools");
    tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

    tabbedPane.addTab("Examples", null, examplesContributionTab.panel,
                      "Examples");
    tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);

    tabbedPane.addTab("Updates", null, updatesContributionTab.panel, "Updates");
    tabbedPane.setMnemonicAt(4, KeyEvent.VK_5);

    tabbedPane.setUI(new SpacedTabbedPaneUI());
    tabbedPane.setBackground(new Color(0x132638));
    tabbedPane.setOpaque(true);

    for (int i = 0; i < 5; i++) {
      tabbedPane.setToolTipTextAt(i, null);
    }

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

        /*
        @Override
        public void addLayoutComponent(String name, Component comp) {
          super.addLayoutComponent(name, comp);
        }
        */

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


  void downloadAndUpdateContributionListing(Base base) {
    //activeTab is required now but should be removed
    //as there is only one instance of contribListing and it should be present in this class
    final ContributionTab activeTab = getActiveTab();

    ContribProgressMonitor progress =
      new ContribProgressBar(activeTab.progressBar) {

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
    };
    activeTab.contribListing.downloadAvailableList(base, progress);
  }


  void makeAndShowTab(boolean activateErrorPanel, boolean isLoading) {
    librariesContributionTab.showFrame(editor, activateErrorPanel, isLoading);
    modesContributionTab.showFrame(editor, activateErrorPanel, isLoading);
    toolsContributionTab.showFrame(editor, activateErrorPanel, isLoading);
    examplesContributionTab.showFrame(editor, activateErrorPanel, isLoading);
    updatesContributionTab.showFrame(editor, activateErrorPanel, isLoading);
  }


  ContributionTab getActiveTab() {
    switch (tabbedPane.getSelectedIndex()) {
    case 0:
      return librariesContributionTab;
    case 1:
      return modesContributionTab;
    case 2:
      return toolsContributionTab;
    case 3:
      return examplesContributionTab;
    default:
      return updatesContributionTab;
    }
  }
}
