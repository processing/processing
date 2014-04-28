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
import javax.swing.border.EmptyBorder;
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
  static public final String REMOVE_RESTART_MESSAGE =
    "<i>Please restart Processing to finish removing this item.</i>";

  static public final String INSTALL_RESTART_MESSAGE =
    "<i>Please restart Processing to finish installing this item.</i>";

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
  private JTextPane descriptionBlock;
//  private JTextPane notificationBlock;
  private JLabel notificationBlock;
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
          contribListing.replaceContribution(contrib, contrib);
        }
      }
    };

    undoActionListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (contrib instanceof LocalContribution) {
          LocalContribution installed = (LocalContribution) contrib;
          installed.setDeletionFlag(false);
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
          installProgressBar.setIndeterminate(true);

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
//    addProgressBarAndButton();

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
    setLayout(new BorderLayout());

    descriptionBlock = new JTextPane();
    descriptionBlock.setInheritsPopupMenu(true);
    Insets margin = descriptionBlock.getMargin();
    margin.bottom = 0;
    descriptionBlock.setMargin(margin);
    descriptionBlock.setContentType("text/html");
    setTextStyle(descriptionBlock);
    descriptionBlock.setOpaque(false);
//    stripTextSelectionListeners(descriptionBlock);

    descriptionBlock.setBorder(new EmptyBorder(4, 7, 7, 7));
    descriptionBlock.setHighlighter(null);
    add(descriptionBlock, BorderLayout.CENTER);
    
    Box updateBox = Box.createHorizontalBox();  //new BoxLayout(filterPanel, BoxLayout.X_AXIS)
    
    notificationBlock = new JLabel();
    notificationBlock.setInheritsPopupMenu(true);
    notificationBlock.setVisible(false);
    notificationBlock.setOpaque(false);
    // not needed after changing to JLabel
//    notificationBlock.setContentType("text/html");
//    notificationBlock.setHighlighter(null);
//    setTextStyle(notificationBlock);
    notificationBlock.setFont(new Font("Verdana", Font.ITALIC, 10));
//    stripTextSelectionListeners(notificationBlock);

      updateButton = new JButton("Update");
      updateButton.setInheritsPopupMenu(true);
      Dimension updateButtonDimensions = updateButton.getPreferredSize();
      updateButtonDimensions.width = BUTTON_WIDTH;
      updateButton.setMinimumSize(updateButtonDimensions);
      updateButton.setPreferredSize(updateButtonDimensions);
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
//      add(updateButton, c);
//    }
    updateBox.add(updateButton);
    updateBox.add(notificationBlock);
    updateBox.setBorder(new EmptyBorder(4, 7, 7, 7));
    add(updateBox, BorderLayout.SOUTH);
    
//  }
//
//
//  private void addProgressBarAndButton() {
    
//    Box statusBox = Box.createVerticalBox();
//    GridBagConstraints c = new GridBagConstraints();
//    c.gridx = 4;
//    c.gridy = 0;
//    c.weighty = 1;
//    c.gridheight = 3;
//    c.fill = GridBagConstraints.VERTICAL;
//    c.anchor = GridBagConstraints.NORTH;
    JPanel rightPane = new JPanel();
    rightPane.setInheritsPopupMenu(true);
    rightPane.setOpaque(false);
    rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.Y_AXIS));
    rightPane.setMinimumSize(new Dimension(ContributionPanel.BUTTON_WIDTH, 1));
//    add(rightPane, c);
//    statusBox.add(rightPane);
    add(rightPane, BorderLayout.EAST);

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

//    StringBuilder nameText = new StringBuilder();
//    nameText.append("<html><body><b>");
//    if (contrib.getUrl() == null) {
//      nameText.append(contrib.getName());
//    } else {
//      nameText.append("<a href=\"" + contrib.getUrl() + "\">" + contrib.getName() + "</a>");
//    }
//    nameText.append("</b>");
//    String authorList = contrib.getAuthorList();
//    if (authorList != null && !authorList.isEmpty()) {
//      nameText.append(" by ");
//      nameText.append(toHtmlLinks(contrib.getAuthorList()));
//    }
//    nameText.append("</body></html>");
//    headerText.setText(nameText.toString());
//
//    StringBuilder description = new StringBuilder();
//    description.append("<html><body>");
//    boolean isFlagged = contrib.isDeletionFlagged();
//    if (isFlagged) {
//      description.append(ContributionListPanel.DELETION_MESSAGE);
//    } else {
//      String sentence = contrib.getSentence();
//      if (sentence == null || sentence.isEmpty()) {
//        sentence = "<i>Description unavailable.</i>";
//      } else {
//        sentence = sanitizeHtmlTags(sentence);
//        sentence = toHtmlLinks(sentence);
//      }
//      description.append(sentence);
//    }
//    description.append("</body></html>");
//    descriptionText.setText(description.toString());
    
    StringBuilder description = new StringBuilder();
    description.append("<html><body><b>");
    if (contrib.getUrl() == null) {
      description.append(contrib.getName());
    } else {
      description.append("<a href=\"" + contrib.getUrl() + "\">" + contrib.getName() + "</a>");
    }
    description.append("</b>");
    String authorList = contrib.getAuthorList();
    if (authorList != null && !authorList.isEmpty()) {
      description.append(" by ");
      description.append(toHtmlLinks(contrib.getAuthorList()));
    }
    description.append("<br/>");

    //System.out.println("checking restart flag for " + contrib + " " + contrib.getName() + " and it's " + contrib.isRestartFlagged());
    if (contrib.isDeletionFlagged()) {
      description.append(REMOVE_RESTART_MESSAGE);
    } else if (contrib.isRestartFlagged()) {
      description.append(INSTALL_RESTART_MESSAGE);
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
    //descriptionText.setText(description.toString());
    descriptionBlock.setText(description.toString());
//    System.out.println(description);

    if (contribListing.hasUpdates(contrib)) {
      StringBuilder versionText = new StringBuilder();
      versionText.append("<html><body><i>");
      if (contrib.isDeletionFlagged()) {
        // Already marked for deletion, see requiresRestart() notes below.
        versionText.append("To finish an update, reinstall this contribution after restarting.");
      } else {
        versionText.append("New version available!");
        if (contrib.getType().requiresRestart()) {
          // If a contribution can't be reinstalled in-place, the user may need
          // to remove the current version, restart Processing, then install.
          versionText.append(" To update, first remove the current version.");
        }
      }
      versionText.append("</i></body></html>");
      notificationBlock.setText(versionText.toString());
      notificationBlock.setVisible(true);
    } else {
      notificationBlock.setText("");
      notificationBlock.setVisible(false);
    }

    updateButton.setEnabled(true);
    if (contrib != null && !contrib.getType().requiresRestart()) {
      updateButton.setVisible(isSelected() && contribListing.hasUpdates(contrib));
    }

    installRemoveButton.removeActionListener(installActionListener);
    installRemoveButton.removeActionListener(removeActionListener);
    installRemoveButton.removeActionListener(undoActionListener);

    if (contrib.isDeletionFlagged()) {
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
      // not sure why we'd re-enable the button if it had an error...
//      installRemoveButton.setEnabled(true);
    }
  }

  
  // This doesn't actually seem to work?
  /*
  static void stripTextSelectionListeners(JEditorPane editorPane) {
    for (MouseListener listener : editorPane.getMouseListeners()) {
      String className = listener.getClass().getName();
      if (className.endsWith("MutableCaretEvent") || 
          className.endsWith("DragListener") || 
          className.endsWith("BasicCaret")) {
        editorPane.removeMouseListener(listener);
      }
    }
  }
  */
  

  protected void resetInstallProgressBarState() {
    installProgressBar.setString("Starting");
    installProgressBar.setIndeterminate(false);
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

    if (contrib != null && !contrib.getType().requiresRestart()) {
      updateButton.setVisible(isSelected() && contribListing.hasUpdates(contrib));
    }
    installRemoveButton.setVisible(isSelected() || installRemoveButton.getText().equals("Remove"));

//    for (JTextPane textPane : headerPaneSet) {
    { 
//      JTextPane textPane = headerText;
//      JTextPane textPane = textBlock;
      JEditorPane editorPane = descriptionBlock;  //textPane;

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
//      setSelectionStyle(textPane, isSelected());
      setSelectionStyle(descriptionBlock, isSelected());
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
//      setForegroundStyle(headerText, installed);
//      setForegroundStyle(descriptionText, installed);
      setForegroundStyle(descriptionBlock, installed, isSelected());
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
  static void setForegroundStyle(JTextPane textPane, boolean installed, boolean selected) {
    Document doc = textPane.getDocument();
    if (doc instanceof HTMLDocument) {
      HTMLDocument html = (HTMLDocument) doc;
      StyleSheet stylesheet = html.getStyleSheet();
      
      String c = (installed && !selected) ? "#555555" : "#000000";  // slightly grayed when installed
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