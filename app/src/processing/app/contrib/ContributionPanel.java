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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

import processing.app.Base;


/**
 * Panel that expands and gives a brief overview of a library when clicked.
 */
class ContributionPanel extends JPanel {
  private final ContributionListPanel listPanel;
  private final ContributionListing contribListing = ContributionListing.getInstance();

  static private final int BUTTON_WIDTH = 100;

  /** 
   * Should only be set through setContribution(), 
   * otherwise UI components will not be updated. 
   */
  private Contribution contrib;

  private boolean alreadySelected;
  private boolean enableHyperlinks;
  private HyperlinkListener conditionalHyperlinkOpener;
  private JTextPane headerText;
  private JTextPane descriptionText;
  private JTextPane textBlock;
  private JTextPane updateNotificationLabel;
  private JButton updateButton;
  private JProgressBar installProgressBar;
  private JButton installRemoveButton;
  private JPopupMenu contextMenu;
  private JMenuItem openFolder;

//  private HashSet<JTextPane> headerPaneSet;
  private ActionListener removeActionListener;
  private ActionListener installActionListener;
  private ActionListener undoActionListener;


  ContributionPanel(ContributionListPanel contributionListPanel) {
    listPanel = contributionListPanel;
//    headerPaneSet = new HashSet<JTextPane>();

    enableHyperlinks = false;
    alreadySelected = false;
    conditionalHyperlinkOpener = new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          if (enableHyperlinks) {
            if (e.getURL() != null) {
              Base.openURL(e.getURL().toString());
            }
          }
        }
      }
    };

    installActionListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (contrib instanceof AvailableContribution) {
          installContribution((AvailableContribution) contrib);
        }
      }
    };

    undoActionListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (contrib instanceof LocalContribution) {
          LocalContribution installed = (LocalContribution) contrib;
          installed.unsetDeletionFlag();
          contribListing.replaceContribution(contrib, contrib);  // ?? 
        }
      }
    };

    removeActionListener = new ActionListener() {
      public void actionPerformed(ActionEvent arg) {
        if (contrib.isInstalled() && contrib instanceof LocalContribution) {
          updateButton.setEnabled(false);
          installRemoveButton.setEnabled(false);
          installProgressBar.setVisible(true);

          ((LocalContribution) contrib).removeContribution(listPanel.contribManager.editor,
                                                           new JProgressMonitor(installProgressBar) {
            public void finishedAction() {
              // Finished uninstalling the library
              resetInstallProgressBarState();
              installRemoveButton.setEnabled(true);
            }
          },
          listPanel.contribManager.status);
        }
      }
    };

    contextMenu = new JPopupMenu();
    openFolder = new JMenuItem("Open Folder");
    openFolder.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (contrib instanceof LocalContribution) {
          File folder = ((LocalContribution) contrib).getFolder();
          Base.openFolder(folder);
        }
      }
    });

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

    addPaneComponents();
    addProgressBarAndButton();

    setBackground(listPanel.getBackground());
    setOpaque(true);
    setSelected(false);

    setExpandListener(this, new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        listPanel.setSelectedPanel(ContributionPanel.this);
      }
    });
  }


  /**
   * Create the widgets for the header panel which is visible when the 
   * library panel is not clicked.
   */
  private void addPaneComponents() {
    setLayout(new GridBagLayout());

    { // Header text area. The name of the contribution and its authors.
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.weightx = 1;
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;

      headerText = new JTextPane();
      headerText.setInheritsPopupMenu(true);
      Insets margin = headerText.getMargin();
      margin.bottom = 0;
      headerText.setMargin(margin);
      headerText.setContentType("text/html");
      setTextStyle(headerText);
      headerText.setOpaque(false);
//      headerPaneSet.add(headerText);
      stripTextSelectionListeners(headerText);
      add(headerText, c);
    }

    { // The bottom right of the description, used to show text describing it
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 1;
      c.weighty = 1;
      c.weightx = 1;
      c.gridwidth = 2;
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.NORTHEAST;

      JPanel descriptionPanel = new JPanel(new GridBagLayout());
      descriptionPanel.setInheritsPopupMenu(true);
      descriptionPanel.setOpaque(false);
      add(descriptionPanel, c);

      {
        GridBagConstraints dc = new GridBagConstraints();
        dc.fill = GridBagConstraints.HORIZONTAL;
        dc.weightx = 1;

        descriptionText = new JTextPane();
        descriptionText.setInheritsPopupMenu(true);
        descriptionText.setContentType("text/html");
        setTextStyle(descriptionText);
        descriptionText.setOpaque(false);
        descriptionPanel.add(descriptionText, dc);
      }

      int margin = Base.isMacOS() ? 15 : 5;
      {
        GridBagConstraints dc = new GridBagConstraints();
        dc.gridx = 1;
        descriptionPanel.add(Box.createHorizontalStrut(margin), dc);
      }
    }

    { // A label below the description text showing notifications for when
      // updates are available, or instructing the user to restart the PDE if
      // necessary
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 2;
      c.weightx = 1;
      c.insets = new Insets(-5, 0, 0, 0);
      c.fill = GridBagConstraints.HORIZONTAL;
      c.anchor = GridBagConstraints.EAST;

      updateNotificationLabel = new JTextPane();
      updateNotificationLabel.setInheritsPopupMenu(true);
      updateNotificationLabel.setVisible(false);
      setTextStyle(updateNotificationLabel);
      stripTextSelectionListeners(updateNotificationLabel);
      add(updateNotificationLabel, c);
    }

    { // An update button, shown in the description area, but only visible for
      // contributions that do not require a restart.
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 1;
      c.gridy = 2;
      c.weightx = 1;
      c.insets = new Insets(-5, 0, 0, 0);
      c.anchor = GridBagConstraints.EAST;

      updateButton = new JButton("Update");
      updateButton.setInheritsPopupMenu(true);
      Dimension installButtonDimensions = updateButton.getPreferredSize();
      installButtonDimensions.width = ContributionPanel.BUTTON_WIDTH;
      updateButton.setMinimumSize(installButtonDimensions);
      updateButton.setPreferredSize(installButtonDimensions);
      updateButton.setOpaque(false);
      updateButton.setVisible(false);
      updateButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          updateButton.setEnabled(false);
          AvailableContribution ad = contribListing.getAvailableContribution(contrib);
          String url = ad.link;
          installContribution(ad, url);
        }
      });
      add(updateButton, c);
    }
  }


  private void addProgressBarAndButton() {
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 4;
    c.gridy = 0;
    c.weighty = 1;
    c.gridheight = 3;
    c.fill = GridBagConstraints.VERTICAL;
    c.anchor = GridBagConstraints.NORTH;
    JPanel rightPane = new JPanel();
    rightPane.setInheritsPopupMenu(true);
    rightPane.setOpaque(false);
    rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.Y_AXIS));
    rightPane.setMinimumSize(new Dimension(ContributionPanel.BUTTON_WIDTH, 1));
    add(rightPane, c);

    installProgressBar = new JProgressBar();
    installProgressBar.setInheritsPopupMenu(true);
    installProgressBar.setStringPainted(true);
    resetInstallProgressBarState();
    Dimension d = installProgressBar.getPreferredSize();
    d.width = ContributionPanel.BUTTON_WIDTH;
    installProgressBar.setPreferredSize(d);
    installProgressBar.setMaximumSize(d);
    installProgressBar.setMinimumSize(d);
    installProgressBar.setOpaque(false);
    rightPane.add(installProgressBar);
    installProgressBar.setAlignmentX(CENTER_ALIGNMENT);

    rightPane.add(Box.createVerticalGlue());

    installRemoveButton = new JButton(" ");
    installRemoveButton.setInheritsPopupMenu(true);

    Dimension installButtonDimensions = installRemoveButton.getPreferredSize();
    installButtonDimensions.width = ContributionPanel.BUTTON_WIDTH;
    installRemoveButton.setPreferredSize(installButtonDimensions);
    installRemoveButton.setMaximumSize(installButtonDimensions);
    installRemoveButton.setMinimumSize(installButtonDimensions);
    installRemoveButton.setOpaque(false);
    rightPane.add(installRemoveButton);
    installRemoveButton.setAlignmentX(CENTER_ALIGNMENT);

    // Set the minimum size of this pane to be the sum of the height of the
    // progress bar and install button
    d = installProgressBar.getPreferredSize();
    Dimension d2 = installRemoveButton.getPreferredSize();
    d.width = ContributionPanel.BUTTON_WIDTH;
    d.height = d.height+d2.height;
    rightPane.setMinimumSize(d);
    rightPane.setPreferredSize(d);
  }

  
  private void setExpandListener(Component component,
                                 MouseAdapter expandPanelMouseListener) {
    component.addMouseListener(expandPanelMouseListener);
    if (component instanceof Container) {
      for (Component child : ((Container) component).getComponents()) {
        setExpandListener(child, expandPanelMouseListener);
      }
    }
  }

  
  public void setContribution(Contribution contrib) {
    this.contrib = contrib;

    StringBuilder nameText = new StringBuilder();
    nameText.append("<html><body><b>");
    if (contrib.getUrl() == null) {
      nameText.append(contrib.getName());
    } else {
      nameText.append("<a href=\"" + contrib.getUrl() + "\">" + contrib.getName() + "</a>");
    }
    nameText.append("</b>");
    String authorList = contrib.getAuthorList();
    if (authorList != null && !authorList.isEmpty()) {
      nameText.append(" by ");
      nameText.append(toHtmlLinks(contrib.getAuthorList()));
    }
    nameText.append("</body></html>");
    headerText.setText(nameText.toString());

    StringBuilder description = new StringBuilder();
    description.append("<html><body>");

    boolean isFlagged = contrib.isDeletionFlagged();
    if (isFlagged) {
      description.append(ContributionListPanel.DELETION_MESSAGE);
    } else {
      String sentence = contrib.getSentence();
      if (sentence == null || sentence.isEmpty()) {
        sentence = "<i>Description unavailable.</i>";
      } else {
        sentence = sanitizeHtmlTags(sentence);
        sentence = toHtmlLinks(sentence);
      }
      description.append(sentence);
    }

    description.append("</body></html>");
    descriptionText.setText(description.toString());

    if (contribListing.hasUpdates(contrib)) {
      StringBuilder versionText = new StringBuilder();
      versionText.append("<html><body><i>");
      if (isFlagged) {
        versionText.append("To finish an update, reinstall this contribution after the restart.");
      } else {
        versionText.append("New version available!");
        if (contrib.requiresRestart()) {
          versionText.append(" To update, first remove the current version.");
        }
      }
      versionText.append("</i></body></html>");
      updateNotificationLabel.setText(versionText.toString());
      updateNotificationLabel.setVisible(true);
    } else {
      updateNotificationLabel.setText("");
      updateNotificationLabel.setVisible(false);
    }

    updateButton.setEnabled(true);
    if (contrib != null && !contrib.requiresRestart()) {
      updateButton.setVisible(isSelected() && contribListing.hasUpdates(contrib));
    }

    installRemoveButton.removeActionListener(installActionListener);
    installRemoveButton.removeActionListener(removeActionListener);
    installRemoveButton.removeActionListener(undoActionListener);

    if (isFlagged) {
      installRemoveButton.addActionListener(undoActionListener);
      installRemoveButton.setText("Undo");
    } else if (contrib.isInstalled()) {
      installRemoveButton.addActionListener(removeActionListener);
      installRemoveButton.setText("Remove");
      installRemoveButton.setVisible(true);
    } else {
      installRemoveButton.addActionListener(installActionListener);
      installRemoveButton.setText("Install");
    }

    contextMenu.removeAll();

    if (contrib.isInstalled()) {
      contextMenu.add(openFolder);
      setComponentPopupMenu(contextMenu);
    } else {
      setComponentPopupMenu(null);
    }

  }

  private void installContribution(AvailableContribution info) {
    if (info.link == null) {
      listPanel.contribManager.status.setErrorMessage("Your operating system "
        + "doesn't appear to be supported. You should visit the "
        + info.getType() + "'s library for more info.");
    } else {
      installContribution(info, info.link);
    }
  }

  
  private void installContribution(AvailableContribution ad, String url) {
    installRemoveButton.setEnabled(false);

    try {
      URL downloadUrl = new URL(url);
      installProgressBar.setVisible(true);

      JProgressMonitor downloadProgress = new JProgressMonitor(installProgressBar) {
        public void finishedAction() {
          // Finished downloading library
        }
      };

      JProgressMonitor installProgress = new JProgressMonitor(installProgressBar) {
        public void finishedAction() {
          // Finished installing library
          resetInstallProgressBarState();
          installRemoveButton.setEnabled(true);

          if (isError()) {
            listPanel.contribManager.status.setErrorMessage("An error occured while downloading the contribution.");
          }
        }
      };

      ContributionManager.downloadAndInstall(listPanel.contribManager.editor,
                                             downloadUrl, ad, 
                                             downloadProgress, installProgress,
                                             listPanel.contribManager.status);

    } catch (MalformedURLException e) {
      Base.showWarning(ContributionListPanel.INSTALL_FAILURE_TITLE,
                       ContributionListPanel.MALFORMED_URL_MESSAGE, e);
      installRemoveButton.setEnabled(true);
    }
  }

  
  void stripTextSelectionListeners(JEditorPane editorPane) {
    for (MouseListener listener : editorPane.getMouseListeners()) {
      String className = listener.getClass().getName();
      if (className.endsWith("MutableCaretEvent") || 
          className.endsWith("DragListener") || 
          className.endsWith("BasicCaret")) {
        editorPane.removeMouseListener(listener);
      }
    }
  }
  

  protected void resetInstallProgressBarState() {
    installProgressBar.setString("Starting");
    installProgressBar.setIndeterminate(true);
    installProgressBar.setValue(0);
    installProgressBar.setVisible(false);
  }

  
  /** 
   * Should be called whenever this component is selected (clicked on) 
   * or unselected, even if it is already selected.
   */
  public void setSelected(boolean isSelected) {
    // Only enable hyperlinks if this component is already selected.
    // Why? Because otherwise if the user happened to click on what is 
    // now a hyperlink, it will be opened as the mouse is released.
    enableHyperlinks = alreadySelected;

    if (contrib != null && !contrib.requiresRestart()) {
      updateButton.setVisible(isSelected() && contribListing.hasUpdates(contrib));
    }
    installRemoveButton.setVisible(isSelected() || installRemoveButton.getText().equals("Remove"));

//    for (JTextPane textPane : headerPaneSet) {
    { 
      JTextPane textPane = headerText;
      JEditorPane editorPane = textPane;

      editorPane.removeHyperlinkListener(ContributionListPanel.nullHyperlinkListener);
      editorPane.removeHyperlinkListener(conditionalHyperlinkOpener);
      if (isSelected()) {
        editorPane.addHyperlinkListener(conditionalHyperlinkOpener);
        editorPane.setEditable(false);
      } else {
        editorPane.addHyperlinkListener(ContributionListPanel.nullHyperlinkListener);
        editorPane.setEditable(true);
      }

      // Update style of hyperlinks
      setSelectionStyle(textPane, isSelected());
    }
    alreadySelected = isSelected();
  }
  

  public boolean isSelected() {
    return listPanel.getSelectedPanel() == this;
  }


//  public void setForeground(Color fg) {
//    super.setForeground(fg);
//    System.out.println(contrib.getName());
//  }
  
//  static int inc;

  public void setForeground(Color fg) {
    super.setForeground(fg);

//      PrintWriter writer = PApplet.createWriter(new File("/Users/fry/Desktop/traces/" + PApplet.nf(++inc, 4) + ".txt"));
//      new Exception().printStackTrace(writer);
//      writer.flush();
//      writer.close();

//    if (headerPaneSet != null) {
//      System.out.println(headerPaneSet.size());
////      int rgb = fg.getRGB();
//      for (JTextPane pane : headerPaneSet) {
////        setForegroundStyle(pane, rgb);
//        setForegroundStyle(pane, contrib.isInstalled());
//      }
//    }
    if (contrib != null) {
      boolean installed = contrib.isInstalled(); 
      setForegroundStyle(headerText, installed);
      setForegroundStyle(descriptionText, installed);
    }
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  
  
  static String sanitizeHtmlTags(String stringIn) {
    stringIn = stringIn.replaceAll("<", "&lt;");
    stringIn = stringIn.replaceAll(">", "&gt;");
    return stringIn;
  }

  
  /**
   * This has a [link](http://example.com/) in [it](http://example.org/).
   *
   * Becomes...
   *
   * This has a <a href="http://example.com/">link</a> in <a
   * href="http://example.org/">it</a>.
   */
  static String toHtmlLinks(String stringIn) {
    Pattern p = Pattern.compile("\\[(.*?)\\]\\((.*?)\\)");
    Matcher m = p.matcher(stringIn);

    StringBuilder sb = new StringBuilder();

    int start = 0;
    while (m.find(start)) {
      sb.append(stringIn.substring(start, m.start()));

      String text = m.group(1);
      String url = m.group(2);

      sb.append("<a href=\"");
      sb.append(url);
      sb.append("\">");
      sb.append(text);
      sb.append("</a>");

      start = m.end();
    }
    sb.append(stringIn.substring(start));
    return sb.toString();
  }
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  /** 
   * Sets coloring based on whether installed or not; 
   * also makes ugly blue HTML links into the specified color (black).
   */
  static void setForegroundStyle(JTextPane textPane, boolean installed) {
    Document doc = textPane.getDocument();
    if (doc instanceof HTMLDocument) {
      HTMLDocument html = (HTMLDocument) doc;
      StyleSheet stylesheet = html.getStyleSheet();
      
      String c = installed ? "#404040" : "#000000";  // slightly grayed when installed
//      String c = "#000000";  // just make them both black
      stylesheet.addRule("body { color:" + c + "; }");
      stylesheet.addRule("a { color:" + c + "; }");
    }
  }
  
  
  static void setTextStyle(JTextPane textPane) {
    Document doc = textPane.getDocument();
    if (doc instanceof HTMLDocument) {
      HTMLDocument html = (HTMLDocument) doc;
      StyleSheet stylesheet = html.getStyleSheet();
      stylesheet.addRule("body { " + 
                         "  margin: 0; padding: 0;" + 
                         "  font-family: Verdana, Geneva, Arial, Helvetica, sans-serif;" + 
                         "  font-size: 100%;" + "font-size: 0.95em; " +
                         "}");
    }
  }
  
  
  static void setSelectionStyle(JTextPane textPane, boolean selected) {
    Document doc = textPane.getDocument();
    if (doc instanceof HTMLDocument) {
      HTMLDocument html = (HTMLDocument) doc;
      StyleSheet styleSheet = html.getStyleSheet();
      if (selected) {
        styleSheet.addRule("a { text-decoration:underline } ");
      } else {
        styleSheet.addRule("a { text-decoration:none }");
      }
    }
  }
}