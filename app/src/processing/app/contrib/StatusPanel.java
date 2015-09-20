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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;

import processing.app.ui.Toolkit;
import processing.app.Base;
import processing.app.Platform;


class StatusPanel extends JPanel {
  static final int BUTTON_WIDTH = 150;

  JTextPane label;
  JButton installButton;
  JPanel progressBarPanel;
  JLabel updateLabel;
  JButton updateButton;
  JButton removeButton;
  GroupLayout layout;
  JLabel iconLabel;
  ContributionListing contributionListing = ContributionListing.getInstance();
  ContributionTab contributionTab;

  private String bodyRule;

  public StatusPanel(int width, final ContributionTab contributionTab) {
    super();
    setBackground(new Color(0xebebeb));
//    setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.BLACK));
    this.contributionTab = contributionTab;

    iconLabel = new JLabel();
    iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

    label = new JTextPane();
    label.setEditable(false);
    label.setOpaque(false);
    label.setContentType("text/html");
    bodyRule = "a, body { font-family: " + ContributionManagerDialog.font.getFamily() + "; " +
            "font-size: " + ContributionManagerDialog.font.getSize() + "pt; color: black; text-decoration: none;}";
    label.addHyperlinkListener(new HyperlinkListener() {

      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          if (e.getURL() != null) {
            Platform.openURL(e.getURL().toString());
          }
        }
      }
    });
    installButton =
      new JButton("Install", Toolkit.getLibIconX("manager/install"));
    installButton.setFont(Toolkit.getSansFont(14, Font.PLAIN));
    installButton.setHorizontalAlignment(SwingConstants.LEFT);
//    installButton.setContentAreaFilled(false);
//    installButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1),BorderFactory.createEmptyBorder(3, 0, 3, 0)));
    installButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        installButton.setEnabled(false);
        ContributionPanel currentPanel = contributionTab.contributionListPanel
          .getSelectedPanel();
        currentPanel.install();
        StatusPanel.this.update(currentPanel);
      }
    });
    progressBarPanel = new JPanel();
    progressBarPanel.setLayout(new BorderLayout());
    progressBarPanel.setOpaque(false);
    updateLabel = new JLabel(" ");
    updateLabel.setFont(Toolkit.getSansFont(14, Font.PLAIN));
    updateLabel.setHorizontalAlignment(SwingConstants.CENTER);
    updateButton =
      new JButton("Update", Toolkit.getLibIconX("manager/update"));
    updateButton.setFont(Toolkit.getSansFont(14, Font.PLAIN));
    updateButton.setHorizontalAlignment(SwingConstants.LEFT);
    updateButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateButton.setEnabled(false);
        ContributionPanel currentPanel =
          contributionTab.contributionListPanel.getSelectedPanel();
        currentPanel.update();
        StatusPanel.this.update(currentPanel);
      }
    });

    removeButton =
      new JButton("Remove", Toolkit.getLibIconX("manager/remove"));
    removeButton.setFont(Toolkit.getSansFont(14, Font.BOLD));
    removeButton.setHorizontalAlignment(SwingConstants.LEFT);
    removeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        removeButton.setEnabled(false);
        ContributionPanel currentPanel =
          contributionTab.contributionListPanel.getSelectedPanel();
        currentPanel.remove();
        StatusPanel.this.update(currentPanel);
      }
    });

    int labelWidth = (width != 0) ? (3 * width / 4) : GroupLayout.PREFERRED_SIZE;
    layout = new GroupLayout(this);
    this.setLayout(layout);

    layout.setAutoCreateContainerGaps(true);
    layout.setAutoCreateGaps(true);

    layout.setHorizontalGroup(layout
      .createSequentialGroup()
      .addGap(0)
      .addComponent(iconLabel, ContributionManagerDialog.STATUS_WIDTH, ContributionManagerDialog.STATUS_WIDTH, ContributionManagerDialog.STATUS_WIDTH)
      .addGap(0)
      .addComponent(label, labelWidth, labelWidth, labelWidth)
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(installButton, BUTTON_WIDTH, BUTTON_WIDTH,
                                BUTTON_WIDTH)
                  .addComponent(progressBarPanel)
                  .addComponent(updateLabel, BUTTON_WIDTH, BUTTON_WIDTH, BUTTON_WIDTH)
                  .addComponent(updateButton)
                  .addComponent(removeButton)));

    layout.setVerticalGroup(layout
      .createParallelGroup(GroupLayout.Alignment.LEADING)
      .addComponent(iconLabel)
      .addComponent(label)
      .addGroup(layout.createSequentialGroup()
                  .addComponent(installButton)
                  .addGroup(layout.createParallelGroup()
                              .addComponent(progressBarPanel)
                              .addComponent(updateLabel))
                  .addComponent(updateButton).addComponent(removeButton)));

    layout.linkSize(SwingConstants.HORIZONTAL, installButton, progressBarPanel,
                    updateButton, removeButton);

    progressBarPanel.setVisible(false);
    updateLabel.setVisible(false);

    installButton.setEnabled(false);
    updateButton.setEnabled(false);
    removeButton.setEnabled(false);
    updateLabel.setVisible(true);

    layout.setHonorsVisibility(updateLabel, false); // Makes the label take up space even though not visible

    validate();

  }

  public StatusPanel() {
    // TODO Auto-generated constructor stub
  }

  void setMessage(String message) {
    if (label != null) {
      label.setText(message);
      label.repaint();
    }
  }

  void setErrorMessage(String message) {
    if (label != null) {
      label.setText(message);
      label.repaint();
    }
  }

  void clear() {
    if (label != null) {
      label.setText(null);
      label.repaint();
    }
  }

  public void update(ContributionPanel panel) {
    progressBarPanel.removeAll();

    Icon icon = null;
    if (panel.getContrib().isSpecial()) {
      icon = Toolkit.getLibIcon("icons/foundation-32.png");  // was 48?
    }
    iconLabel.setIcon(icon);
    label.setText(panel.description.toString());
    ((HTMLDocument)label.getDocument()).getStyleSheet().addRule(bodyRule);

    updateButton.setEnabled(contributionListing.hasDownloadedLatestList()
      && (contributionListing.hasUpdates(panel.getContrib()) && !panel
        .getContrib().isUpdateFlagged())
        && !panel.isUpdateInProgress);

    String latestVersion =
      contributionListing.getLatestVersion(panel.getContrib());
    String currentVersion = panel.getContrib().getPrettyVersion();

    installButton.setEnabled(!panel.getContrib().isInstalled()
                             && contributionListing.hasDownloadedLatestList()
                             && panel.getContrib().isCompatible(Base.getRevision())
                             && !panel.isInstallInProgress);

    if (installButton.isEnabled()) {
      updateLabel.setText(latestVersion + " available");
    } else {
      updateLabel.setText(currentVersion + " installed");
    }

    if (latestVersion != null) {
      latestVersion = "Update to " + latestVersion;
    } else {
      latestVersion = "Update";
    }

    if (currentVersion == null) {
      currentVersion = "";
    }

    if (updateButton.isEnabled()) {
      updateButton.setText(latestVersion);
    } else {
      updateButton.setText("Update");
    }

    removeButton.setEnabled(panel.getContrib().isInstalled()
                            && !panel.isRemoveInProgress);
    progressBarPanel.add(panel.installProgressBar);
    progressBarPanel.setVisible(false);
    updateLabel.setVisible(true);
    if (panel.isUpdateInProgress || panel.isInstallInProgress || panel.isRemoveInProgress) {
      progressBarPanel.setVisible(true);
      updateLabel.setVisible(false);
      progressBarPanel.repaint();
    }
  }
}
/*
interface ErrorWidget {
  void setErrorMessage(String msg);
}

class StatusPanel extends JPanel implements ErrorWidget {
  String errorMessage;

  StatusPanel() {
    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        clearErrorMessage();
      }
    });
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    g.setFont(new Font("SansSerif", Font.PLAIN, 10));
    int baseline = (getSize().height + g.getFontMetrics().getAscent()) / 2;

    if (contribListing.isDownloadingListing()) {
      g.setColor(Color.black);
      g.drawString("Downloading software listing...", 2, baseline);
      setVisible(true);
    } else if (errorMessage != null) {
      g.setColor(Color.red);
      g.drawString(errorMessage, 2, baseline);
      setVisible(true);
    } else {
      setVisible(false);
    }
  }

  public void setErrorMessage(String message) {
    errorMessage = message;
    setVisible(true);

    JPanel placeholder = getPlaceholder();
    Dimension d = getPreferredSize();
    if (Base.isWindows()) {
      d.height += 5;
      placeholder.setPreferredSize(d);
    }
    placeholder.setVisible(true);
  }

  void clearErrorMessage() {
    errorMessage = null;
    repaint();

    getPlaceholder().setVisible(false);
  }
}
*/