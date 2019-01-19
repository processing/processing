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

  static final int AUTHOR_WIDTH = Toolkit.zoom(240);
  static final int STATUS_WIDTH = Toolkit.zoom(66);

  static final String title = "Contribution Manager";

  Base base;
  JFrame frame;
  ManagerTabs tabs;

  ContributionTab librariesTab;
  ContributionTab modesTab;
  ContributionTab toolsTab;
  ContributionTab examplesTab;
  UpdateContributionTab updatesTab;

  static Font SMALL_PLAIN;
  static Font SMALL_BOLD;
  static Font NORMAL_PLAIN;
  static Font NORMAL_BOLD;


  public ManagerFrame(Base base) {
    this.base = base;

    final int smallSize = Toolkit.zoom(12);
    final int normalSize = Toolkit.zoom(14);
    SMALL_PLAIN = Toolkit.getSansFont(smallSize, Font.PLAIN);
    SMALL_BOLD = Toolkit.getSansFont(smallSize, Font.BOLD);
    NORMAL_PLAIN = Toolkit.getSansFont(normalSize, Font.PLAIN);
    NORMAL_BOLD = Toolkit.getSansFont(normalSize, Font.BOLD);

    librariesTab = new ContributionTab(this, ContributionType.LIBRARY);
    modesTab = new ContributionTab(this, ContributionType.MODE);
    toolsTab = new ContributionTab(this, ContributionType.TOOL);
    examplesTab = new ContributionTab(this, ContributionType.EXAMPLES);
    updatesTab = new UpdateContributionTab(this);
  }


  public void showFrame(ContributionType contributionType) {
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
    frame.setMinimumSize(Toolkit.zoom(750, 500));
    tabs = new ManagerTabs(base);

    makeAndShowTab(false, true);

    tabs.addPanel(librariesTab, "Libraries");
    tabs.addPanel(modesTab, "Modes");
    tabs.addPanel(toolsTab, "Tools");
    tabs.addPanel(examplesTab, "Examples");
    tabs.addPanel(updatesTab, "Updates");

    frame.setResizable(true);

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
  }


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


  // TODO move this to ContributionTab (this is handled weirdly, period) [fry]
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
          makeAndShowTab(true, false);
        } else {
          makeAndShowTab(false, false);
        }
      }
    };
    activeTab.contribListing.downloadAvailableList(base, progress);
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
}
