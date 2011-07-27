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

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import java.awt.event.*;
import java.awt.*;
import java.net.*;

import processing.app.ContributionListing.ContributionChangeListener;
import processing.app.contribution.*;

public class ContributionListPanel extends JPanel implements Scrollable, ContributionChangeListener {
  
  public static final String INSTALL_FAILURE_TITLE = "Install Failed";

  public static final String MALFORMED_URL_MESSAGE =
                      "The link fetched from Processing.org is invalid.\n"
                    + "You can still intall this library manually by visiting\n"
                    + "the library's website.";
  
  ContributionManager contributionManager;
  
  JProgressBar setupProgressBar;
  
  TreeMap<Contribution, ContributionPanel> contributionPanelsByInfo;
  
  private static HyperlinkListener nullHyperlinkListener = new HyperlinkListener() {
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
    }
  };
  
  protected ContributionPanel selectedPanel;
  
  public ContributionListPanel(ContributionManager libraryManager) {
    super();
    
    this.contributionManager = libraryManager;
    
    setLayout(new GridBagLayout());
    setFocusable(true);
    setOpaque(true);
    
    if (Base.isLinux()) {
      // Thanks to a bug with GNOME, getColor returns the wrong value for
      // List.background. We'll just assume its white. The intersection
      // of people using Linux and people using a weird inverted color theme
      // should be small enough.
      setBackground(Color.white);
    } else {
      setBackground(UIManager.getColor("List.background"));
    }
    
    contributionPanelsByInfo = new TreeMap<Contribution, ContributionPanel>();
    
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 1;
    c.weighty = 1;
    c.anchor = GridBagConstraints.CENTER;
    
    setupProgressBar = new JProgressBar();
    setupProgressBar.setString("");
    setupProgressBar.setStringPainted(true);
    add(setupProgressBar, c);
    
  }
  
  public void contributionAdded(Contribution contributionInfo) {
    
    setupProgressBar.setVisible(false);
    
    if (contributionPanelsByInfo.containsKey(contributionInfo)) {
      return;
    }
    
    ContributionPanel newPanel = new ContributionPanel();
    
    synchronized (contributionPanelsByInfo) {
      contributionPanelsByInfo.put(contributionInfo, newPanel);
    }
    
    
    if (newPanel != null) {
      newPanel.setContribution(contributionInfo);
      
      add(newPanel);
      updatePanelOrdering();
      updateColors();
    }
  }
  
  private void updatePanelOrdering() {
    int row = 0;
    for (Entry<Contribution, ContributionPanel> entry : contributionPanelsByInfo.entrySet()) {
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1;
      c.gridx = 0;
      c.gridy = row++;
      
      add(entry.getValue(), c);
    }
  }

  public void contributionRemoved(Contribution contributionInfo) {
    
    synchronized (contributionPanelsByInfo) {
      ContributionPanel panel = contributionPanelsByInfo.get(contributionInfo);
      if (panel != null) {
        remove(panel);
      }
    }
    
    updateUI();
  }
  
  public void contributionChanged(Contribution oldInfo, Contribution newInfo) {
    
    synchronized (contributionPanelsByInfo) {
      ContributionPanel panel = contributionPanelsByInfo.get(oldInfo);
      contributionPanelsByInfo.remove(oldInfo);
      
      panel.setContribution(newInfo);
      contributionPanelsByInfo.put(newInfo, panel);
      
      add(panel);
    }
    
    updatePanelOrdering();
  }
  
  public void filterLibraries(List<Contribution> filteredContributions) {

    synchronized (contributionPanelsByInfo) {

      Set<Contribution> hiddenPanels = new TreeSet(contributionPanelsByInfo.keySet());
      
      for (Contribution info : filteredContributions) {
        
        ContributionPanel panel = contributionPanelsByInfo.get(info);
        if (panel != null) {
          panel.setVisible(true);
          hiddenPanels.remove(info);
        }
      }

      for (Contribution info : hiddenPanels) {
        ContributionPanel panel = contributionPanelsByInfo.get(info);
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
    synchronized (contributionPanelsByInfo) {
      for (Entry<Contribution, ContributionPanel> entry : contributionPanelsByInfo.entrySet()) {
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

  public JProgressBar getSetupProgressBar() {
    return setupProgressBar;
  }
  
  /**
   * Panel that expands and gives a brief overview of a library when clicked.
   */
  private class ContributionPanel extends JPanel {
    
    private static final int BUTTON_WIDTH = 100;
    
    /** Should only be set through setContribution(), otherwise UI components
     *  will not be updated. */
    Contribution info;
    
    boolean alreadySelected;
    
    boolean enableHyperlinks;
    
    HyperlinkListener conditionalHyperlinkOpener;
    
    JTextPane headerText;
    
    JPanel descriptionPanel;
    
    JLabel categoryLabel;
    
    JPanel iconArea;
    
    JTextPane descriptionText;

    JTextPane updateNotificationLabel;

    JButton updateButton;
    
    JProgressBar installProgressBar;
    
    JButton installOrRemoveButton;

    private HashSet<JTextPane> htmlPanes;

    private ActionListener removeActionListener;

    private ActionListener installActionListener;
    
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
        public void actionPerformed(ActionEvent arg) {
          if (info instanceof AdvertisedContribution) {
            installContribution((AdvertisedContribution) info);
          }
        }
      };
      
      removeActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent arg) {
          if (info.isInstalled() && info instanceof InstalledContribution) {
            updateButton.setEnabled(false);
            installOrRemoveButton.setEnabled(false);
            
            installProgressBar.setVisible(true);
            contributionManager.removeContribution((InstalledContribution) info,
                                                   new JProgressMonitor(installProgressBar) {
              
              public void finishedAction() {
                // Finished uninstalling the library
                resetInstallProgressBarState();
                installOrRemoveButton.setEnabled(true);
              }
            });
          }
        }
      };
      
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      
      addPaneComponents();
      addProgressBarAndButton();
      
      setBackground(ContributionListPanel.this.getBackground());
      setOpaque(true);
      setFocusable(true);
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
      setFocusable(true);
      setLayout(new GridBagLayout());
    
      {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        
        headerText = new JTextPane();
        setHtmlTextStyle(headerText);
        add(headerText, c);
      }
      
      {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        
        categoryLabel = new JLabel();
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 7));
        add(categoryLabel, c);
      }
      
      {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 1;
        c.weightx = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.WEST;
        
        descriptionPanel = new JPanel();
        descriptionPanel.setOpaque(false);
        descriptionPanel.setLayout(new GridBagLayout());
        add(descriptionPanel, c);
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        
        iconArea = new JPanel() {
          protected void paintComponent(Graphics g) {
            Image icon = contributionManager.getContributionIcon(info.getType());
            if (icon != null) {
              g.drawImage(icon, 0, 0, this);
            }
          };
        };
        iconArea.setOpaque(false);
        Dimension d = new Dimension(ContributionManager.ICON_WIDTH,
                                    ContributionManager.ICON_HEIGHT);
        iconArea.setMinimumSize(d);
        iconArea.setPreferredSize(d);
        
        descriptionPanel.add(iconArea, c);
      }
      
      {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weighty = 1;
        c.weightx = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.EAST;
        
        descriptionText = new JTextPane();
        setHtmlTextStyle(descriptionText);
        descriptionPanel.add(descriptionText, c);
      }
      
      {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.insets = new Insets(-5, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.EAST;
        
        updateNotificationLabel = new JTextPane();
        updateNotificationLabel.setVisible(false);
        setHtmlTextStyle(updateNotificationLabel);
        descriptionPanel.add(updateNotificationLabel, c);
      }
      
      {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 1;
        c.weightx = 1;
        c.insets = new Insets(-5, 0, 0, 0);
        c.anchor = GridBagConstraints.EAST;
        
        updateButton = new JButton("Update now");
        Dimension installButtonDimensions = updateButton.getPreferredSize();
        installButtonDimensions.width = BUTTON_WIDTH;
        updateButton.setMinimumSize(installButtonDimensions);
        updateButton.setPreferredSize(installButtonDimensions);
        updateButton.setOpaque(false);
        updateButton.setVisible(false);
        updateButton.addActionListener(new ActionListener() {
          
          public void actionPerformed(ActionEvent e) {
            updateButton.setEnabled(false);
            // XXX: There is a bug here. The advertised library will be 'replaced' instead
            installContribution((AdvertisedContribution) contributionManager.contributionListing.getAdvertisedContribution(info));
          }
        });
        descriptionPanel.add(updateButton, c);
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
      rightPane.setOpaque(false);
      rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.Y_AXIS));
      rightPane.setMinimumSize(new Dimension(BUTTON_WIDTH, 1));
      add(rightPane, c);
      
      installProgressBar = new JProgressBar();
      installProgressBar.setStringPainted(true);
      resetInstallProgressBarState();
      Dimension d = installProgressBar.getPreferredSize();
      d.width = BUTTON_WIDTH;
      installProgressBar.setPreferredSize(d);
      installProgressBar.setMaximumSize(d);
      installProgressBar.setMinimumSize(d);
      installProgressBar.setOpaque(false);
      rightPane.add(installProgressBar);
      installProgressBar.setAlignmentX(CENTER_ALIGNMENT);
      
      rightPane.add(Box.createVerticalGlue());
      
      installOrRemoveButton = new JButton(" ");
    
      Dimension installButtonDimensions = installOrRemoveButton.getPreferredSize();
      installButtonDimensions.width = BUTTON_WIDTH;
      installOrRemoveButton.setPreferredSize(installButtonDimensions);
      installOrRemoveButton.setMaximumSize(installButtonDimensions);
      installOrRemoveButton.setMinimumSize(installButtonDimensions);
      installOrRemoveButton.setOpaque(false);
      rightPane.add(installOrRemoveButton);
      installOrRemoveButton.setAlignmentX(CENTER_ALIGNMENT);
      
      // Set the minimum size of this pane to be the sum of the height of the
      // progress bar and install button
      d = installProgressBar.getPreferredSize();
      Dimension d2 = installOrRemoveButton.getPreferredSize();
      d.width = BUTTON_WIDTH;
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
    
    public void setContribution(Contribution info) {
      
      this.info = info;
      
      setFocusable(true);

      StringBuilder nameText = new StringBuilder();
      nameText.append("<html><body><b>");
      if (info.getUrl() == null) {
        nameText.append(info.getName());
      } else {
        nameText.append("<a href=\"" + info.getUrl() + "\">" + info.getName() + "</a>");
      }
      nameText.append("</b>");
      nameText.append(createAuthorString(info.getAuthorList()));
      nameText.append("</body></html>");
      headerText.setText(nameText.toString());
      
      categoryLabel.setText("[" + info.getCategory() + "]");
      
      StringBuilder description = new StringBuilder();
      description.append("<html><body>");
      if (info.getSentence() != null)
        description.append(info.getSentence());
      
      description.append("</body></html>");
      descriptionText.setText(description.toString());
      setAlignment(descriptionText, StyleConstants.ALIGN_JUSTIFIED);
      
      if (contributionManager.hasUpdates(info)) {
        StringBuilder versionText = new StringBuilder();
        versionText.append("<html><body><i>");
        versionText.append("New version available!");
        versionText.append("</i></body></html>");
        updateNotificationLabel.setText(versionText.toString());
        updateNotificationLabel.setVisible(true);
      } else {
        updateNotificationLabel.setText("");
        updateNotificationLabel.setVisible(false);
        updateButton.setVisible(false);
      }
      
      if (info.isInstalled()) {
        installOrRemoveButton.removeActionListener(installActionListener);
        installOrRemoveButton.addActionListener(removeActionListener);
        installOrRemoveButton.setText("Remove");
      } else {
        installOrRemoveButton.removeActionListener(removeActionListener);
        installOrRemoveButton.addActionListener(installActionListener);
        installOrRemoveButton.setText("Install");
      }
      
    }
    
    private void installContribution(AdvertisedContribution info) {
      
      String url = info.link;
      installOrRemoveButton.setEnabled(false);
      
      try {
        URL downloadUrl = new URL(url);
        
        installProgressBar.setVisible(true);
        
        contributionManager.downloadAndInstall(downloadUrl, info,
          new JProgressMonitor(installProgressBar) {

            public void finishedAction() {
              // Finished downloading library
            }
          },
          new JProgressMonitor(installProgressBar) {

            public void finishedAction() {
              // Finished installing library
              resetInstallProgressBarState();
              installOrRemoveButton.setEnabled(true);
            }
          }
        );
        
      } catch (MalformedURLException e) {
        Base.showWarning(INSTALL_FAILURE_TITLE, MALFORMED_URL_MESSAGE, e);
        installOrRemoveButton.setEnabled(true);
      }
    }
    
    private String createAuthorString(List<Author> authorList) {
      StringBuilder authors = new StringBuilder();
      
      if (authorList != null && !authorList.isEmpty()) {
        authors.append(" by ");
        
        for (int i = 0; i < authorList.size(); i++) {
          Author author = authorList.get(i);
          if (author.url == null) {
            authors.append(author.name);
          } else {
            authors.append("<a href=\"");
            authors.append(author.url);
            authors.append("\">");
            authors.append(author.name);
            authors.append("</a>");
          }
          if (i + 2 < authorList.size()) {
            authors.append(", ");
          } else if (i + 2 == authorList.size()) {
            if (authorList.size() > 2) {
              authors.append(", and ");
            } else {
              authors.append(" and ");
            }
          }
        }
        
      }
      
      return authors.toString();
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
      
      updateButton.setVisible(isSelected() && contributionManager.hasUpdates(info));
      installOrRemoveButton.setVisible(isSelected());
      
      for (JTextPane textPane : htmlPanes) {
        if (textPane instanceof JEditorPane) {
          JEditorPane editorPane = (JEditorPane) textPane;
          
          editorPane.removeHyperlinkListener(nullHyperlinkListener);
          editorPane.removeHyperlinkListener(conditionalHyperlinkOpener);
          if (isSelected()) {
            editorPane.addHyperlinkListener(conditionalHyperlinkOpener);
            editorPane.setEditable(false);
          } else {
            editorPane.addHyperlinkListener(nullHyperlinkListener);
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
      
      if (categoryLabel != null)
        categoryLabel.setForeground(fg);
      
      if (htmlPanes != null) {
        for (JTextPane pane : htmlPanes) {
          Document doc = pane.getDocument();
          
          if (doc instanceof HTMLDocument) {
            HTMLDocument html = (HTMLDocument) doc;
            StyleSheet stylesheet = html.getStyleSheet();
            stylesheet.addRule("body {color:" + toHex(fg) + ";}");
            stylesheet.addRule("a {color:" + toHex(fg) + "}");
          }
        }
      }
    }
  }
}
