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

import processing.app.Contribution.ContributionInfo;
import processing.app.Contribution.ContributionInfo.Author;
import processing.app.Contribution.ContributionInfo.ContributionType;
import processing.app.ContributionListing.ContributionChangeListener;

public class ContributionListPanel extends JPanel implements Scrollable, ContributionChangeListener {
  
  public static final String INSTALL_FAILURE_TITLE = "Install Failed";

  public static final String MALFORMED_URL_MESSAGE =
                      "The link fetched from Processing.org is invalid.\n"
                    + "You can still intall this library manually by visiting\n"
                    + "the library's website.";
  
  ContributionManager contributionManager;
  
  JProgressBar setupProgressBar;
  
  TreeMap<ContributionInfo, ContributionPanel> contributionPanelsByInfo;
  
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
    
    contributionPanelsByInfo = new TreeMap<ContributionInfo, ContributionPanel>();
    
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
  
  public void contributionAdded(ContributionInfo contributionInfo) {
    
    if (setupProgressBar.isVisible()) {
      setupProgressBar.setVisible(false);
    }
    
    if (contributionPanelsByInfo.containsKey(contributionInfo)) {
      return;
    }
    
    ContributionPanel newPanel = null;
    if (contributionInfo.getType() == ContributionType.LIBRARY) {
      newPanel = new ContributionPanel();
      
    } else if (contributionInfo.getType() == ContributionType.LIBRARY_COMPILATION) {
      newPanel = new ContributionPanel();
    }
    
    synchronized (contributionPanelsByInfo) {
      contributionPanelsByInfo.put(contributionInfo, newPanel);
    }
    
    
    if (newPanel != null) {
      newPanel.setContributionInfo(contributionInfo);
      
      add(newPanel);
      recalculateConstraints();
      updateColors();
    }
  }
  
  private void recalculateConstraints() {
    int row = 0;
    for (Entry<ContributionInfo, ContributionPanel> entry : contributionPanelsByInfo.entrySet()) {
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1;
      c.gridx = 0;
      c.gridy = row++;
      
      add(entry.getValue(), c);
    }
  }

  public void contributionRemoved(ContributionInfo contributionInfo) {
    
    synchronized (contributionPanelsByInfo) {
      ContributionPanel panel = contributionPanelsByInfo.get(contributionInfo);
      if (panel != null) {
        remove(panel);
      }
    }
    
    updateUI();
  }
  
  public void contributionChanged(ContributionInfo oldInfo, ContributionInfo newInfo) {
    
    synchronized (contributionPanelsByInfo) {
      ContributionPanel panel = contributionPanelsByInfo.get(oldInfo);
      contributionPanelsByInfo.remove(oldInfo);
      
      panel.setContributionInfo(newInfo);
      contributionPanelsByInfo.put(newInfo, panel);
      
      add(panel);
    }
    
    recalculateConstraints();
  }
  
  public void filterLibraries(List<ContributionInfo> filteredContributions) {

    synchronized (contributionPanelsByInfo) {

      Set<ContributionInfo> hiddenPanels = new TreeSet(contributionPanelsByInfo.keySet());
      
      for (ContributionInfo info : filteredContributions) {
        
        ContributionPanel panel = contributionPanelsByInfo.get(info);
        if (panel != null) {
          panel.setVisible(true);
          hiddenPanels.remove(info);
        }
      }

      for (ContributionInfo info : hiddenPanels) {
        ContributionPanel panel = contributionPanelsByInfo.get(info);
        if (panel != null) {
          panel.setVisible(false);
        }
      }
    }
  }
  
  /**
   * Updates the colors of all library panels that are visible.
   */
  private void updateColors() {

    int count = 0;
    synchronized (contributionPanelsByInfo) {
      for (Entry<ContributionInfo, ContributionPanel> entry : contributionPanelsByInfo.entrySet()) {
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
        
        panel.updateHyperLinkStyles();
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
    
    boolean doOpenHyperLink;
    
    HyperlinkListener conditionalHyperlinkOpener;
    
    JTextPane headerLabel;
    
    JTextPane versionLabel;
    
    JLabel categoryLabel;
    
    JTextPane descriptionText;

    JProgressBar installProgressBar;
    
    JButton installOrRemoveButton;
    
    // JButton updateButton;

    private ArrayList<JTextPane> htmlPanes;

    private HyperlinkListener updateListner;

    
    private ContributionPanel() {
      
      htmlPanes = new ArrayList<JTextPane>();
      
      doOpenHyperLink = false;
      conditionalHyperlinkOpener = new HyperlinkListener() {
        
        public void hyperlinkUpdate(HyperlinkEvent e) {
          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (doOpenHyperLink) {
              if (e.getURL() != null) {
                Base.openURL(e.getURL().toString());
              }
            }
          }
        }
      };
      
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      
      addPaneComponents();
      addProgressBarAndButton();
      
      setBackground(ContributionListPanel.this.getBackground());
      setOpaque(true);
      setFocusable(true);
      updateInteractiveComponents();
      
      MouseAdapter expandPanelMouseListener = new MouseAdapter() {

        public void mousePressed(MouseEvent e) {
          
          if (selectedPanel != ContributionPanel.this) {
            ContributionPanel lastSelected = selectedPanel;
            selectedPanel = ContributionPanel.this;
            if (lastSelected != null) lastSelected.updateInteractiveComponents();
            updateInteractiveComponents();
            updateColors();
            getParent().requestFocusInWindow();
          } else {
            doOpenHyperLink = true;
          }
        }
        
      };
      
      setExpandListener(this, expandPanelMouseListener);
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
        
    public void setContributionInfo(final ContributionInfo info) {
      
      setFocusable(true);

      StringBuilder nameText = new StringBuilder();
      nameText.append("<html><body><b>");
      if (info.url == null) {
        nameText.append(info.name);
      } else {
        nameText.append("<a href=\"" + info.url + "\">" + info.name + "</a>");
      }
      nameText.append("</b>");
      nameText.append(createAuthorString(info.authorList));
      nameText.append("</body></html>");
      headerLabel.setText(nameText.toString());
      
      setHtmlTextStyle(headerLabel, false);
      
      categoryLabel.setText("[" + info.category + "]");
      
      StringBuilder description = new StringBuilder();
      description.append("<html><body>");
      
      if (info.sentence != null)
        description.append(info.sentence);
      
      description.append("</body></html>");
      descriptionText.setText(description.toString());
      
      for (ActionListener listener : installOrRemoveButton.getActionListeners()) {
        installOrRemoveButton.removeActionListener(listener);
      }
      
      if (info.isInstalled()) {
        // Display the version number and upgrade link
        StringBuilder versionText = new StringBuilder();
        versionText.append("<html><body>");
        versionText.append("Version ");
        if (info.prettyVersion != null) {
          versionText.append(info.prettyVersion);
        } else {
          versionText.append(info.version);
        }
        versionText.append(" installed.");
        if (info.hasUpdates()) {
          final String afterClickText = versionText.toString();
          versionText.append(" <a href=\"\">Click here to update to latest version.</a>");
          versionLabel.removeHyperlinkListener(updateListner);
          updateListner = new HyperlinkListener() {
            
            public void hyperlinkUpdate(HyperlinkEvent e) {
              if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                versionLabel.setText(afterClickText);
                installContribution(info, info.link);
              }
            }
          };
          versionLabel.addHyperlinkListener(updateListner);
        }
        versionText.append("</body></html>");
        versionLabel.setText(versionText.toString());
        versionLabel.setMargin(new Insets(0, 25, 10, 5));
        versionLabel.setVisible(true);
        
        installOrRemoveButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent arg) {
            installOrRemoveButton.setEnabled(false);
            
            installProgressBar.setVisible(true);
            contributionManager.removeContribution(info.getContribution(),
                                                   new JProgressMonitor(installProgressBar) {
              
              public void finishedAction() {
                // Finished uninstalling the library
                resetInstallProgressBarState();
                installOrRemoveButton.setEnabled(true);
              }
            });
            
          }
        });
        installOrRemoveButton.setText("Remove");
      } else {
        versionLabel.setText("");
        versionLabel.setVisible(false);
        
        installOrRemoveButton.addActionListener(new ActionListener() {
          
          public void actionPerformed(ActionEvent arg) {
            installContribution(info, info.link);
          }
        });
        installOrRemoveButton.setText("Install");
      }
      
    }
    
    private void installContribution(ContributionInfo info, String url) {
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

    void setHtmlTextStyle(JTextPane textPane, boolean justified) {
      
      Font font = UIManager.getFont("Label.font");

      if (justified) {
        StyledDocument sdoc = textPane.getStyledDocument();
        SimpleAttributeSet sa = new SimpleAttributeSet();
        StyleConstants.setAlignment(sa, StyleConstants.ALIGN_JUSTIFIED);
        sdoc.setParagraphAttributes(0, 1, sa, false);
      }
      
      Document doc = textPane.getDocument();
      
      if (doc instanceof HTMLDocument) {
        HTMLDocument html = (HTMLDocument) doc;
        StyleSheet stylesheet = html.getStyleSheet();
        
        stylesheet.addRule("body {font-family:"+font.getFamily()+";" + 
                           "font-size:"+font.getSize()+"pt;}");
      }
        
      updateHyperLinkStyle(textPane);
      htmlPanes.add(textPane);
      
      textPane.setOpaque(false);
    }
    
    void updateHyperLinkStyles() {
      for (JTextPane textPane : htmlPanes) {
        updateHyperLinkStyle(textPane);
      }
    }
    
    private void updateHyperLinkStyle(JTextPane textPane) {
      Document doc = textPane.getDocument();

      if (doc instanceof HTMLDocument) {

        HTMLDocument html = (HTMLDocument) doc;
        
        StyleSheet stylesheet = html.getStyleSheet();
        
        if (isSelected()) {
          stylesheet.addRule("a {text-decoration:underline}");
        } else {
          if (textPane != versionLabel) {
            stylesheet.addRule("a {text-decoration:none}");
          } else {
            html.removeStyle("a");
          }
        }
      }
    }
    
    /**
     * Create the widgets for the header panel which is visible when the library
     * panel is not clicked
     */
    /**
     * 
     */
    private void addPaneComponents() {
      setFocusable(true);
      setLayout(new GridBagLayout());

      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.weightx = 1;
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;
      
      headerLabel = new JTextPane();
      headerLabel.setContentType("text/html");
      
      setHtmlTextStyle(headerLabel, false);
      add(headerLabel, c);
      
      c = new GridBagConstraints();
      c.gridx = 1;
      c.gridy = 0;
      c.anchor = GridBagConstraints.WEST;
      
      categoryLabel = new JLabel();
      categoryLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 7));
      add(categoryLabel, c);
      
      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 1;
      c.weightx = 1;
      c.gridwidth = 2;
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;

      descriptionText = new JTextPane();
      descriptionText.setContentType("text/html");
      descriptionText.setMargin(new Insets(0, 25, 10, 5));
      setHtmlTextStyle(descriptionText, true);
      add(descriptionText, c);
      
      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 2;
      c.weightx = 1;
      c.gridwidth = 2;
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;

      versionLabel = new JTextPane();
      versionLabel.setContentType("text/html");
      versionLabel.setVisible(false);
      versionLabel.setEditable(false);
      setHtmlTextStyle(versionLabel, false);
      add(versionLabel, c);
    }
    
    public void addProgressBarAndButton() {
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
    
    protected void resetInstallProgressBarState() {
      installProgressBar.setString("Starting");
      installProgressBar.setIndeterminate(true);
      installProgressBar.setValue(0);
      installProgressBar.setVisible(false);
    }

    public void updateInteractiveComponents() {
      
      installOrRemoveButton.setVisible(isSelected());
      
      for (JTextPane textPane : htmlPanes) {
        if (textPane instanceof JEditorPane) {
          JEditorPane editorPane = (JEditorPane) textPane;
          
          if (isSelected()) {
            editorPane.removeHyperlinkListener(nullHyperlinkListener);
            editorPane.addHyperlinkListener(conditionalHyperlinkOpener);
            editorPane.setEditable(false);
          } else {
            editorPane.removeHyperlinkListener(conditionalHyperlinkOpener);
            editorPane.addHyperlinkListener(nullHyperlinkListener);
            editorPane.setEditable(true);
          }
        }
      }
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
            stylesheet.addRule("a {color:"+toHex(fg)+"}");
          }
        }
      }
      
      if (versionLabel != null)
        updateHyperLinkStyle(versionLabel);
    }
  }
}
