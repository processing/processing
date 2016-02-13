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

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;

import java.io.*;
import java.math.BigDecimal;



public class StatusPanel extends JPanel {
  static final int BUTTON_WIDTH = 150;

  static Icon foundationIcon;
  static Icon installIcon;
  static Icon updateIcon;
  static Icon removeIcon;
  static Font buttonFont;

  JTextPane label;
  JButton installButton;
  JPanel progressPanel;
  JLabel updateLabel;
  JLabel downloadSizeLabel;     // Label to display the downloadFile size
  JButton updateButton;
  JButton removeButton;
  GroupLayout layout;
  JLabel iconLabel;
  ContributionListing contributionListing = ContributionListing.getInstance();
  ContributionTab contributionTab;

  private String bodyRule;
  private boolean getDownloadSize = false;  // To store download file size
  Thread t1 = new Thread();    // Thread to calculate the download file size


  /** Needed by ContributionListPanel */
  public StatusPanel() { }


  public StatusPanel(final ContributionTab contributionTab, int width) {
    this.contributionTab = contributionTab;

    if (foundationIcon == null) {
      foundationIcon = Toolkit.getLibIconX("icons/foundation", 32);
      installIcon = Toolkit.getLibIconX("manager/install");
      updateIcon = Toolkit.getLibIconX("manager/update");
      removeIcon = Toolkit.getLibIconX("manager/remove");
      buttonFont = Toolkit.getSansFont(14, Font.PLAIN);
    }

    setBackground(new Color(0xebebeb));

    iconLabel = new JLabel();
    iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

    label = new JTextPane();
    label.setEditable(false);
    label.setOpaque(false);
    label.setContentType("text/html");
    bodyRule = "a, body { font-family: " + buttonFont.getFamily() + "; " +
            "font-size: " + buttonFont.getSize() + "pt; color: black; text-decoration: none;}";
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
    installButton = new JButton("Install", installIcon);
    installButton.setDisabledIcon(installIcon);
    installButton.setFont(buttonFont);
    installButton.setHorizontalAlignment(SwingConstants.LEFT);
    installButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        installButton.setEnabled(false);
        DetailPanel currentPanel =
          contributionTab.contributionListPanel.getSelectedPanel();
        currentPanel.install();
        StatusPanel.this.update(currentPanel);
      }
    });
    progressPanel = new JPanel();
    progressPanel.setLayout(new BorderLayout());
    progressPanel.setOpaque(false);

    updateLabel = new JLabel(" ");
    updateLabel.setFont(buttonFont);
    updateLabel.setHorizontalAlignment(SwingConstants.CENTER);

    /* Setting up the JLabel to display the download file size */
    downloadSizeLabel = new JLabel("");
    downloadSizeLabel.setFont(buttonFont);
    downloadSizeLabel.setHorizontalAlignment(SwingConstants.CENTER);

    updateButton = new JButton("Update", updateIcon);
    updateButton.setDisabledIcon(updateIcon);
    updateButton.setFont(buttonFont);
    updateButton.setHorizontalAlignment(SwingConstants.LEFT);
    updateButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        updateButton.setEnabled(false);
        DetailPanel currentPanel =
          contributionTab.contributionListPanel.getSelectedPanel();
        currentPanel.update();
        StatusPanel.this.update(currentPanel);
      }
    });

    removeButton = new JButton("Remove", removeIcon);
    removeButton.setDisabledIcon(removeIcon);
    removeButton.setFont(buttonFont);
    removeButton.setHorizontalAlignment(SwingConstants.LEFT);
    removeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        removeButton.setEnabled(false);
        DetailPanel currentPanel =
          contributionTab.contributionListPanel.getSelectedPanel();
        currentPanel.remove();
        StatusPanel.this.update(currentPanel);
      }
    });

    int labelWidth = (width != 0) ?
      (3 * width / 4) : GroupLayout.PREFERRED_SIZE;
    layout = new GroupLayout(this);
    this.setLayout(layout);

    layout.setAutoCreateContainerGaps(true);
    layout.setAutoCreateGaps(true);

    layout.setHorizontalGroup(layout
      .createSequentialGroup()
      .addGap(0)
      .addComponent(iconLabel,
                    ManagerFrame.STATUS_WIDTH,
                    ManagerFrame.STATUS_WIDTH,
                    ManagerFrame.STATUS_WIDTH)
      .addGap(0)
      .addComponent(label, labelWidth, labelWidth, labelWidth)
      .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                       GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                  .addComponent(installButton,
                                BUTTON_WIDTH, BUTTON_WIDTH, BUTTON_WIDTH)
                  .addComponent(progressPanel)
                  .addComponent(updateLabel,
                                BUTTON_WIDTH, BUTTON_WIDTH, BUTTON_WIDTH)
                  .addComponent(downloadSizeLabel,BUTTON_WIDTH,
                       BUTTON_WIDTH,BUTTON_WIDTH)
                  .addComponent(updateButton)
                  .addComponent(removeButton))
      .addGap(12));  // make buttons line up relative to the scrollbar

    layout.setVerticalGroup(layout
      .createParallelGroup(GroupLayout.Alignment.LEADING)
      .addComponent(iconLabel)
      .addComponent(label)
      .addGroup(layout.createSequentialGroup()
                  .addComponent(installButton)
                  .addGroup(layout.createParallelGroup()
                              .addComponent(progressPanel)
                              .addComponent(updateLabel))
                  .addComponent(downloadSizeLabel)
                  .addComponent(updateButton).addComponent(removeButton)));

    layout.linkSize(SwingConstants.HORIZONTAL,
                    installButton, progressPanel, updateButton, removeButton);

    progressPanel.setVisible(false);
    updateLabel.setVisible(false);

    installButton.setEnabled(false);
    updateButton.setEnabled(false);
    removeButton.setEnabled(false);
    updateLabel.setVisible(true);
    downloadSizeLabel.setVisible(false);

    // Makes the label take up space even though not visible
    layout.setHonorsVisibility(updateLabel, false);

    validate();
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


  void clearMessage() {
    if (label != null) {
      label.setText(null);
      label.repaint();
    }
  }


  public void update(DetailPanel panel) {
    progressPanel.removeAll();

    iconLabel.setIcon(panel.getContrib().isSpecial() ? foundationIcon : null);
    label.setText(panel.description);
    ((HTMLDocument)label.getDocument()).getStyleSheet().addRule(bodyRule);

    updateButton.setEnabled(contributionListing.hasDownloadedLatestList() &&
                            (contributionListing.hasUpdates(panel.getContrib()) &&
                             !panel.getContrib().isUpdateFlagged()) &&
                            !panel.updateInProgress);

    String latestVersion =
      contributionListing.getLatestVersion(panel.getContrib());
    String currentVersion = panel.getContrib().getPrettyVersion();

    installButton.setEnabled(!panel.getContrib().isInstalled()
                             && contributionListing.hasDownloadedLatestList()
                             && panel.getContrib().isCompatible(Base.getRevision())
                             && !panel.installInProgress);

    if (panel.getContrib().isCompatible(Base.getRevision())) {
      if (installButton.isEnabled()) {
        updateLabel.setText(latestVersion + " available");
        downloadSizeLabel.setVisible(false);
        getDownloadSize = true;
        t1.stop(); // Killing previous thread
      } else {
        updateLabel.setText(currentVersion + " installed");
        downloadSizeLabel.setVisible(false);
        getDownloadSize = false;
        t1.stop(); // Killing previous thread
      }
    } else {
      updateLabel.setText(currentVersion + " not compatible");
      downloadSizeLabel.setVisible(false);
      getDownloadSize = false;
      t1.stop(); // Killing previous thread
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
                            && !panel.removeInProgress);
    progressPanel.add(panel.installProgressBar);
    progressPanel.setVisible(false);
    updateLabel.setVisible(true);
    if (panel.updateInProgress || panel.installInProgress || panel.removeInProgress) {
      progressPanel.setVisible(true);
      updateLabel.setVisible(false);
      progressPanel.repaint();
    }

    /** Creating a Thread to calculate the size of the dowload file **/
    // Thread is used to avoid the delay caused by the HTTPUrlConnection class
    // while fetching the fileSize from the provided file URL
    if (getDownloadSize == true) {
      t1 = new Thread(new Runnable() {
        HttpURLConnection conn = null;

        public void run() {
          double downloadSize =0 ;
          DetailPanel currentPanel =
            contributionTab.contributionListPanel.getSelectedPanel();

          try {
            URL url = new URL((contributionListing
              .getAvailableContribution(currentPanel.getContrib())).link);
            conn = (HttpURLConnection)url.openConnection();
            downloadSize = conn.getContentLength();  // Fetching the download filesize
            conn.disconnect();

            // Calculating the file size in standard unit
            String unit[] = {"Bytes", "KB", "MB", "GB", "TB"};
            int u = 0;
            while (downloadSize >= 1024.0) {
              downloadSize = (downloadSize / 1024.0);
              u++;
            }
            downloadSize = new BigDecimal(downloadSize)
              .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

            if (downloadSize > 0.0) {
              downloadSizeLabel.setText("Size :   " + downloadSize + " " + unit[u]);
            } else{
                downloadSizeLabel.setText("Size not available!");
            }
            downloadSizeLabel.setVisible(true);
            return;

            } catch (MalformedURLException e) {
                System.out.println("Malformed Url");
            } catch (IOException e) {
                System.out.println("IOException caught");
            }
          }
          public void stop() {
            if (t1 != null)
              conn.disconnect();
          }
      });
      t1.start();
    }
  }
}