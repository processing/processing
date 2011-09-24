/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-11 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.net.*;

import processing.app.ContributionListing.AdvertisedContribution;
import processing.app.ContributionListing.ContributionChangeListener;
import processing.app.contribution.*;

public class ContributionListPanel extends JPanel implements Scrollable, ContributionChangeListener {
  
  static public final String DELETION_MESSAGE = "<i>This tool has "
      + "been flagged for deletion. Restart all instances of the editor to "
      + "finalize the removal process.</i>";
  
  static public final String INSTALL_FAILURE_TITLE = "Install Failed";

  static public final String MALFORMED_URL_MESSAGE =
                      "The link fetched from Processing.org is invalid.\n"
                    + "You can still intall this library manually by visiting\n"
                    + "the library's website.";
  
  ContributionManagerDialog contribManager;
  
  TreeMap<Contribution, ContributionPanel> panelByContribution;
  
  static private HyperlinkListener nullHyperlinkListener = new HyperlinkListener() {
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
    }
  };
  
  protected ContributionPanel selectedPanel;
  
  protected JPanel statusPlaceholder;
  
  ContributionListing.Filter permaFilter;
  
  ContributionListing contribListing;
  
  public ContributionListPanel(ContributionManagerDialog libraryManager,
                               ContributionListing.Filter filter) {
    
    super();
    
    this.contribManager = libraryManager;
    this.permaFilter = filter;
    
    contribListing = ContributionListing.getInstance();
    
    setLayout(new GridBagLayout());
    setOpaque(true);
    
    if (Base.isLinux()) {
      // Because of a bug with GNOME, getColor returns the wrong value for
      // List.background. We'll just assume its white. The number of people
      // using Linux and an inverted color theme should be small enough.
      setBackground(Color.white);
    } else {
      setBackground(UIManager.getColor("List.background"));
    }
    
    panelByContribution = new TreeMap<Contribution, ContributionPanel>(
        contribListing.getComparator());
    
    statusPlaceholder = new JPanel();
    statusPlaceholder.setVisible(false);
  }
  
  private void updatePanelOrdering() {
    int row = 0;
    for (Entry<Contribution, ContributionPanel> entry : panelByContribution.entrySet()) {
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1;
      c.gridx = 0;
      c.gridy = row++;
      c.anchor = GridBagConstraints.NORTH;
      
      add(entry.getValue(), c);
    }
    
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.weightx = 1;
    c.weighty = 1;
    c.gridx = 0;
    c.gridy = row++;
    c.anchor = GridBagConstraints.NORTH;
    add(statusPlaceholder, c);
  }

  public void contributionAdded(final Contribution contribution) {
    
    if (permaFilter.matches(contribution)) {
      SwingUtilities.invokeLater(new Runnable() {
  
        public void run() {
          if (panelByContribution.containsKey(contribution)) {
            return;
          }
          
          ContributionPanel newPanel = new ContributionPanel();
          
          synchronized (panelByContribution) {
            panelByContribution.put(contribution, newPanel);
          }
          
          
          if (newPanel != null) {
            newPanel.setContribution(contribution);
            
            add(newPanel);
            updatePanelOrdering();
            updateColors();
          }
        }
    });
    }
  }
  
  public void contributionRemoved(final Contribution contribution) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        synchronized (panelByContribution) {
          ContributionPanel panel = panelByContribution.get(contribution);
          if (panel != null) {
            remove(panel);
          }
        }
        
        updateUI();
      }
    });
  }
  
  public void contributionChanged(final Contribution oldContrib,
                                  final Contribution newContrib) {

    SwingUtilities.invokeLater(new Runnable() {

      public void run() {
        synchronized (panelByContribution) {
          ContributionPanel panel = panelByContribution.get(oldContrib);
          if (panel == null) {
            contributionAdded(newContrib);
          } else {
            panelByContribution.remove(oldContrib);
  
            panel.setContribution(newContrib);
            panelByContribution.put(newContrib, panel);
  
            updatePanelOrdering();
          }
        }
      }
    });
  }
  
  public void filterLibraries(List<Contribution> filteredContributions) {

    synchronized (panelByContribution) {

      Set<Contribution> hiddenPanels = new TreeSet(contribListing.getComparator());
      hiddenPanels.addAll(panelByContribution.keySet());
      
      for (Contribution info : filteredContributions) {
        
        ContributionPanel panel = panelByContribution.get(info);
        if (panel != null) {
          panel.setVisible(true);
          hiddenPanels.remove(info);
        }
      }

      for (Contribution info : hiddenPanels) {
        ContributionPanel panel = panelByContribution.get(info);
        if (panel != null) {
          panel.setVisible(false);
        }
      }
    }
  }
  
  protected void setSelectedPanel(ContributionPanel panel) {
    
    if (selectedPanel == panel) {
      selectedPanel.setSelected(true);
    } else {
      ContributionPanel lastSelected = selectedPanel;
      selectedPanel = panel;
      
      if (lastSelected != null) {
        lastSelected.setSelected(false);
      }
      panel.setSelected(true);
      
      updateColors();
      requestFocusInWindow();
    }
  }
  
  /**
   * Updates the colors of all library panels that are visible.
   */
  protected void updateColors() {

    int count = 0;
    synchronized (panelByContribution) {
      for (Entry<Contribution, ContributionPanel> entry : panelByContribution.entrySet()) {
        ContributionPanel panel = entry.getValue();
        
        if (panel.isVisible() && panel.isSelected()) {
          panel.setBackground(UIManager.getColor("List.selectionBackground"));
          panel.setForeground(UIManager.getColor("List.selectionForeground"));
          panel.setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
          count++;
          
        } else {
          Border border = null;
          if (Base.isMacOS() && panel.isVisible()) {
            if (count % 2 == 1) {
              border = UIManager.getBorder("List.evenRowBackgroundPainter");
            } else {
              border = UIManager.getBorder("List.oddRowBackgroundPainter");
            }
            count++;
          }

          if (border == null) {
            border = BorderFactory.createEmptyBorder(1, 1, 1, 1);
          }

          panel.setBorder(border);

          panel.setBackground(ContributionListPanel.this.getBackground());
          panel.setForeground(UIManager.getColor("List.foreground"));
        }
      }
    }
  }
  
  static String toHex(Color c) {
    StringBuilder hex = new StringBuilder();
    hex.append(Integer.toString(c.getRed(), 16));
    hex.append(Integer.toString(c.getGreen(), 16));
    hex.append(Integer.toString(c.getBlue(), 16));
    return hex.toString();
  }
  
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  /**
   * Amount to scroll to reveal a new page of items
   */
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.VERTICAL) {
      int blockAmount = visibleRect.height;
      if (direction > 0) {
        visibleRect.y += blockAmount;
      } else {
        visibleRect.y -= blockAmount;
      }
      
      blockAmount += getScrollableUnitIncrement(visibleRect, orientation, direction);
      return blockAmount;
    }
    return 0;
  }

  /**
   * Amount to scroll to reveal the rest of something we are on or a new item
   */
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.VERTICAL) {

      int lastHeight = 0, height = 0;
      int bottomOfScrollArea = visibleRect.y + visibleRect.height;

      for (Component c : getComponents()) {
        if (c.isVisible()) {
          if (c instanceof ContributionPanel) {
            Dimension d = c.getPreferredSize();

            int nextHeight = height + d.height;

            if (direction > 0) {
              // scrolling down
              if (nextHeight > bottomOfScrollArea) {
                return nextHeight - bottomOfScrollArea;
              }
            } else {
              // scrolling up
              if (nextHeight > visibleRect.y) {
                if (visibleRect.y != height) {
                  return visibleRect.y - height;
                } else {
                  return visibleRect.y - lastHeight;
                }
              }
            }

            lastHeight = height;
            height = nextHeight;
          }
        }
      }
    }
    
    return 0;
  }
  
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  static public String sanitizeHtmlTags(String stringIn) {
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
  static public String toHtmlLinks(String stringIn) {
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
  
  /**
   * Panel that expands and gives a brief overview of a library when clicked.
   */
  private class ContributionPanel extends JPanel {
    
    private static final int BUTTON_WIDTH = 100;
    
    /** Should only be set through setContribution(), otherwise UI components
     *  will not be updated. */
    Contribution contrib;
    
    boolean alreadySelected;
    
    boolean enableHyperlinks;
    
    HyperlinkListener conditionalHyperlinkOpener;
    
    JTextPane headerText;
    
    JTextPane descriptionText;

    JTextPane updateNotificationLabel;

    JButton updateButton;
    
    JProgressBar installProgressBar;
    
    JButton installRemoveButton;
    
    JPopupMenu contextMenu;
    
    JMenuItem openFolder;

    private HashSet<JTextPane> htmlPanes;

    private ActionListener removeActionListener;

    private ActionListener installActionListener;
    
    private ActionListener undoActionListener;
    
    private ContributionPanel() {
      
      htmlPanes = new HashSet<JTextPane>();
      
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
          if (contrib instanceof AdvertisedContribution) {
            installContribution((AdvertisedContribution) contrib);
          }
        }
      };
      
      undoActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (contrib instanceof InstalledContribution) {
            InstalledContribution installed = (InstalledContribution) contrib;
            ContributionManager.removeFlagForDeletion(installed);
            contribListing.replaceContribution(contrib, contrib);
          }
        }
      };
      
      removeActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent arg) {
          if (contrib.isInstalled() && contrib instanceof InstalledContribution) {
            updateButton.setEnabled(false);
            installRemoveButton.setEnabled(false);
            
            installProgressBar.setVisible(true);
            
            ContributionManager.removeContribution(contribManager.editor,
                                                   (InstalledContribution) contrib,
                                                   new JProgressMonitor(installProgressBar) {
              public void finishedAction() {
                // Finished uninstalling the library
                resetInstallProgressBarState();
                installRemoveButton.setEnabled(true);
              }
            },
            contribManager.statusBar);
          }
        }
      };
      
      contextMenu = new JPopupMenu();
      openFolder = new JMenuItem("Open Folder");
      openFolder.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (contrib instanceof InstalledContribution) {
            File folder = ((InstalledContribution) contrib).getFolder();
            Base.openFolder(folder);
          }
        }
      });
      
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      
      addPaneComponents();
      addProgressBarAndButton();
      
      setBackground(ContributionListPanel.this.getBackground());
      setOpaque(true);
      setSelected(false);
      
      setExpandListener(this, new MouseAdapter() {

        public void mousePressed(MouseEvent e) {
          setSelectedPanel(ContributionPanel.this);
        }
      });
    }
    
    /**
     * Create the widgets for the header panel which is visible when the library
     * panel is not clicked
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
        setHtmlTextStyle(headerText);
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
        c.anchor = GridBagConstraints.EAST;
        
        descriptionText = new JTextPane();
        descriptionText.setInheritsPopupMenu(true);
        setHtmlTextStyle(descriptionText);
        stripTextSelectionListeners(descriptionText);
        add(descriptionText, c);
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
        setHtmlTextStyle(updateNotificationLabel);
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
        
        updateButton = new JButton("Update now");
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
            AdvertisedContribution ad = contribListing
                .getAdvertisedContribution(contrib);
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
        nameText.append(ContributionListPanel.toHtmlLinks(contrib.getAuthorList()));
      }
      nameText.append("</body></html>");
      headerText.setText(nameText.toString());
      
      StringBuilder description = new StringBuilder();
      description.append("<html><body>");
      
      boolean isFlagged = ContributionManager.isFlaggedForDeletion(contrib);
      if (isFlagged) {
        description.append(ContributionListPanel.DELETION_MESSAGE);
      } else if (contrib.getSentence() != null) {
        String sentence = ContributionListPanel.sanitizeHtmlTags(contrib.getSentence());
        sentence = ContributionListPanel.toHtmlLinks(sentence);
        description.append(sentence);
      }
      
      description.append("</body></html>");
      descriptionText.setText(description.toString());
      setAlignment(descriptionText, StyleConstants.ALIGN_JUSTIFIED);
      
      if (contribListing.hasUpdates(contrib)) {
        StringBuilder versionText = new StringBuilder();
        versionText.append("<html><body><i>");
        if (isFlagged) {
          versionText.append("To finish an update, reinstall this contribution after the restart.");
        } else {
          versionText.append("New version available!");
          if (ContributionManager.requiresRestart(contrib)) {
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
      if (contrib != null && !ContributionManager.requiresRestart(contrib)) {
        updateButton.setVisible(isSelected()
            && contribListing.hasUpdates(contrib));
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
    
    private void installContribution(AdvertisedContribution info) {
      if (info.link == null) {
        contribManager.statusBar.setErrorMessage("Your operating system "
            + "doesn't appear to be supported. You should visit the "
            + info.getType() + "'s library for more info.");
      } else {
        installContribution(info, info.link);
      }
    }
    
    private void installContribution(AdvertisedContribution ad, String url) {
      
      installRemoveButton.setEnabled(false);
      
      try {
        URL downloadUrl = new URL(url);
        
        installProgressBar.setVisible(true);
        
        ContributionManager.downloadAndInstall(contribManager.editor,
                                          downloadUrl, ad,
          new JProgressMonitor(installProgressBar) {

            public void finishedAction() {
              // Finished downloading library
            }
          },
          new JProgressMonitor(installProgressBar) {

            public void finishedAction() {
              // Finished installing library
              resetInstallProgressBarState();
              installRemoveButton.setEnabled(true);
              
              if (isError()) {
                contribManager.statusBar.setErrorMessage("An error occured when "
                                               + "downloading the contribution.");
              }
            }
          },
          contribManager.statusBar
        );
        
      } catch (MalformedURLException e) {
        Base.showWarning(ContributionListPanel.INSTALL_FAILURE_TITLE,
                         ContributionListPanel.MALFORMED_URL_MESSAGE, e);
        installRemoveButton.setEnabled(true);
      }
    }
    
    void setHtmlTextStyle(JTextPane textPane) {
      
      textPane.setContentType("text/html");
      Font font = UIManager.getFont("Label.font");

      Document doc = textPane.getDocument();
      
      if (doc instanceof HTMLDocument) {
        HTMLDocument html = (HTMLDocument) doc;
        StyleSheet stylesheet = html.getStyleSheet();
        
        stylesheet.addRule("body {font-family:"+font.getFamily()+";" + 
                           "font-size:"+font.getSize()+"pt;}");
      }
        
      htmlPanes.add(textPane);
      
      textPane.setOpaque(false);
    }

    void stripTextSelectionListeners(JEditorPane editorPane) {
      for (MouseListener l : editorPane.getMouseListeners()) {
        String className = l.getClass().getName();
        if (className.endsWith("MutableCaretEvent")
            || className.endsWith("DragListener")
            || className.endsWith("BasicCaret")) {
          editorPane.removeMouseListener(l);
        }
      }
    }
    
    /**
     * @param align one of StyleConstants
     */
    void setAlignment(JTextPane textPane, int align) {
      StyledDocument sdoc = textPane.getStyledDocument();
      SimpleAttributeSet sa = new SimpleAttributeSet();
      StyleConstants.setAlignment(sa, align);
      sdoc.setParagraphAttributes(0, 1, sa, false);
    }
    
    protected void resetInstallProgressBarState() {
      installProgressBar.setString("Starting");
      installProgressBar.setIndeterminate(true);
      installProgressBar.setValue(0);
      installProgressBar.setVisible(false);
    }
    
    /** Should be called whenever this component is selected (clicked on) or
     * unselected, even if it is already selected. 
     * @param selected */
    public void setSelected(boolean isSelected) {
      
      // Only enable hyperlinks if this component is already selected.
      //   Why? Because otherwise if the user happened to click on what is now a
      //   hyperlink, it will be opened as the mouse is released.
      enableHyperlinks = alreadySelected;

      if (contrib != null && !ContributionManager.requiresRestart(contrib)) {
        updateButton.setVisible(isSelected()
            && contribListing.hasUpdates(contrib));
      }
      installRemoveButton.setVisible(isSelected());
      
      for (JTextPane textPane : htmlPanes) {
        if (textPane instanceof JEditorPane) {
          JEditorPane editorPane = (JEditorPane) textPane;
          
          editorPane.removeHyperlinkListener(ContributionListPanel.nullHyperlinkListener);
          editorPane.removeHyperlinkListener(conditionalHyperlinkOpener);
          if (isSelected()) {
            editorPane.addHyperlinkListener(conditionalHyperlinkOpener);
            editorPane.setEditable(false);
          } else {
            editorPane.addHyperlinkListener(ContributionListPanel.nullHyperlinkListener);
            editorPane.setEditable(true);
          }
          
        }
        
        // Update style of hyperlinks
        Document doc = textPane.getDocument();
        if (doc instanceof HTMLDocument) {
          HTMLDocument html = (HTMLDocument) doc;
          
          StyleSheet stylesheet = html.getStyleSheet();
          
          if (isSelected()) {
            stylesheet.addRule("a {text-decoration:underline}");
          } else {
            stylesheet.addRule("a {text-decoration:none}");
          }
        }
      }
      
      alreadySelected = isSelected();
    }
    
    public boolean isSelected() {
      return this == selectedPanel;
    }
    
    public void setForeground(Color fg) {
      super.setForeground(fg);
      
      if (htmlPanes != null) {
        for (JTextPane pane : htmlPanes) {
          Document doc = pane.getDocument();
          
          if (doc instanceof HTMLDocument) {
            HTMLDocument html = (HTMLDocument) doc;
            StyleSheet stylesheet = html.getStyleSheet();
            stylesheet.addRule("body {color:" + ContributionListPanel.toHex(fg) + ";}");
            stylesheet.addRule("a {color:" + ContributionListPanel.toHex(fg) + "}");
          }
        }
      }
    }
  }
}
