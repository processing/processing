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
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.text.*;

import java.awt.event.*;
import java.awt.font.*;
import java.awt.*;
import java.net.*;
import java.text.*;

import processing.app.LibraryListing.LibraryInfo;
import processing.app.LibraryListing.LibraryInfo.Author;

public class LibraryListPanel extends JPanel implements Scrollable {
  
  private static HyperlinkListener nullHyperlinkListener = new HyperlinkListener() {
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
    }
  };
  
  private static HyperlinkListener hyperlinkOpener = new HyperlinkListener() {
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        Base.openURL(e.getURL().toString());
      }
    }
  };
  
  HashMap<LibraryInfo, LibraryPanel> libPanelsByInfo;
  private PreferredViewPositionListener preferredViewPositionListener;
  LibraryListing libraries;
  LibraryManager libraryManager;
  JProgressBar setupProgressBar;
  
  public LibraryListPanel(LibraryManager libraryManager, LibraryListing libraryListing) {
    super();
    
    this.libraryManager = libraryManager;
    
    preferredViewPositionListener = new PreferredViewPositionListener() {
      
      public void handlePreferredLocation(Point p) {
      }

    };
    
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
    
    libPanelsByInfo = new HashMap<LibraryInfo, LibraryPanel>();
    
    addMouseListener(new MouseAdapter() {

      public void mousePressed(MouseEvent mouseEvent) {
        requestFocusInWindow();
      }
    });
    
    if (libraryListing == null) {
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1;
      c.weighty = 1;
      c.anchor = GridBagConstraints.CENTER;
      
      setupProgressBar = new JProgressBar();
      setupProgressBar.setString("");
      setupProgressBar.setStringPainted(true);
      add(setupProgressBar, c);
    } else {
      setLibraryList(libraryListing);
    }
    
  }
  
  public void setLibraryList(LibraryListing libraryListing) {
    libraries = libraryListing;
    if (setupProgressBar != null) {
      remove(setupProgressBar);
      setupProgressBar = null;
    }
    populateLibraryPanels();
  }
  
  private void populateLibraryPanels() {
    int row = 0;
    for (LibraryInfo libInfo : libraries.getAllLibararies()) {
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1;
      c.gridx = 0;
      c.gridy = row++;
      
      LibraryPanel libPanel = new LibraryPanel(libInfo);
      libPanelsByInfo.put(libPanel.libInfo, libPanel);
      
      add(libPanel, c);
    }
    
    updateColors();
  }

  public void filterLibraries(String category, List<String> filters) {
    
    if (libraries != null) {
      List<LibraryInfo> hiddenLibraries = libraries.getAllLibararies();
      for (LibraryInfo lib : libraries.getFilteredLibraryList(category, filters)) {
        libPanelsByInfo.get(lib).setVisible(true);
        hiddenLibraries.remove(lib);
      }
      
      for (LibraryInfo lib : hiddenLibraries) {
        libPanelsByInfo.get(lib).setVisible(false);
      }
    }
  }

  /**
   * Sets the foreground color for a component and all of its subcompenents
   * recursively.
   */
  private static void cascadeForgroundColor(Component component, Color color) {
    if (component instanceof Container) {
      for (Component c : ((Container) component).getComponents()) {
        cascadeForgroundColor(c, color);
      }
    }
    if (component instanceof JLabel || component instanceof JTextComponent) {
      component.setForeground(color);
    }
  }

  /**
   * Counts the numbers of lines needed to display the text in a JTextArea given
   * a width as a contained. This assumed that both word wrap and wrap-style
   * word are enabled for the JTextArea.
   */
  private static int lineCount(JTextArea textArea, int width) {
    try {
      AttributedString text = new AttributedString(textArea.getText());
      FontRenderContext frc = textArea.getFontMetrics(textArea.getFont())
          .getFontRenderContext();
      AttributedCharacterIterator charIt = text.getIterator();
      LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(charIt, frc);
      lineMeasurer.setPosition(charIt.getBeginIndex());
    
      // Get lines from lineMeasurer until the entire
      // paragraph has been displayed.
      int noLines = 0;
      while (lineMeasurer.getPosition() < charIt.getEndIndex()) {
        lineMeasurer.nextLayout(width);
        noLines++;
      }
      return noLines;
      
    } catch (IllegalArgumentException e) {
      return 1;
    }
    
  }

  /**
   * Updates the colors of all library panels that are visible.
   */
  private void updateColors() {
    
    int count = 0;
    for (Entry<LibraryInfo, LibraryPanel> entry : libPanelsByInfo.entrySet()) {
      LibraryPanel libPanel = entry.getValue();

      if (libPanel.isVisible()) {
        if (libPanel.isSelected) {
          libPanel.setBackground(UIManager.getColor("List.selectionBackground"));
          cascadeForgroundColor(libPanel, UIManager.getColor("List.selectionForeground"));
          libPanel.setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
        } else {
          Border border = null;
          if (Base.isMacOS()) {
            if (count % 2 == 1) {
              border = UIManager.getBorder("List.evenRowBackgroundPainter");
            } else {
              border = UIManager.getBorder("List.oddRowBackgroundPainter");
            }
          }

          if (border == null) {
            border = BorderFactory.createEmptyBorder(1, 1, 1, 1);
          }

          libPanel.setBorder(border);

          libPanel.setBackground(LibraryListPanel.this.getBackground());
          cascadeForgroundColor(libPanel, UIManager.getColor("List.foreground"));
        }

        count++;
      }
    }
  }

  /**
   * Calculates the height in pixels of the text in a JTextArea given a width
   * as a contained. This assumed that both word wrap and wrap-style word are
   * enabled for the JTextArea.
   */
  @SuppressWarnings("unused")
  private static int calculateHeight(JTextArea textArea, int width) {
    Font font = textArea.getFont();
    FontMetrics fontMetrics = textArea.getFontMetrics(font);
    int lineHeight = fontMetrics.getAscent() + fontMetrics.getDescent();
    return lineHeight * lineCount(textArea, width);
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
          if (c instanceof LibraryPanel) {
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

  public void setPreferredViewPositionListener(PreferredViewPositionListener preferredViewPositionListener) {
    this.preferredViewPositionListener = preferredViewPositionListener;
  }

  /**
   * Panel that expands and gives a brief overview of a library when clicked.
   */
  class LibraryPanel extends JPanel {
    
    private static final int BUTTON_WIDTH = 100;

    LibraryInfo libInfo;

    JTextPane authorLabel;
    
    JTextPane descriptionText;

    JProgressBar installProgressBar;
    
    JButton installOrRemove;

    boolean isSelected;
    
    String authorsWithLinks = "";
    
    private LibraryPanel(LibraryInfo libInfo) {
      this.libInfo = libInfo;
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      
      generateAuthorString();

      addPaneComponents();
      addProgressBarAndButton();

      setBackground(LibraryListPanel.this.getBackground());
      setOpaque(true);
      setFocusable(true);
      setSelected(false);

      MouseAdapter expandPanelMouseListener = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          
          for (Component c : LibraryListPanel.this.getComponents()) {
            if (c instanceof LibraryPanel) {
              LibraryPanel lp = (LibraryPanel) c;
              if (lp.isSelected) {
                lp.setSelected(false);
                break;
              }
            }
          }

          setSelected(true);
          updateColors();
          getParent().requestFocusInWindow();
        }
      };
      
      addMouseListener(expandPanelMouseListener);
//      for (MouseListener l : authorLabel.getMouseListeners()) {
//        authorLabel.removeMouseListener(l);
//      }
//      for (MouseListener l : descriptionText.getMouseListeners()) {
//        descriptionText.removeMouseListener(l);
//      }
      authorLabel.addMouseListener(expandPanelMouseListener);
      descriptionText.addMouseListener(expandPanelMouseListener);
    }

    private void generateAuthorString() {
      if (!libInfo.authors.isEmpty()) {
        authorsWithLinks = "<html><body><small>by ";
        
        for (int i = 0; i < libInfo.authors.size(); i++) {
          Author author = libInfo.authors.get(i);
          authorsWithLinks += "<a href=\"" + author.url + "\">" + author.name + "</a>";
          if (i + 2 < libInfo.authors.size()) {
            authorsWithLinks += ", ";
          } else if (i + 2 == libInfo.authors.size()) {
            if (libInfo.authors.size() > 2) {
              authorsWithLinks += ", and ";
            } else {
              authorsWithLinks += " and ";
            }
          }
        }
        
        authorsWithLinks += "</small></body></html>";
      }
    }

    /**
     * Create the widgets for the header panel which is visible when the library
     * panel is not clicked
     */
    private void addPaneComponents() {
      setFocusable(true);
      setLayout(new GridBagLayout());

      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.anchor = GridBagConstraints.WEST;
      JLabel nameLabel = new JLabel(libInfo.name);
      Font font = nameLabel.getFont();
      font = font.deriveFont(font.getStyle() | Font.BOLD);
      nameLabel.setFont(font);
      add(nameLabel, c);
      
      c = new GridBagConstraints();
      c.gridx = 1;
      c.gridy = 0;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.anchor = GridBagConstraints.EAST;
      authorLabel = new JTextPane();
      authorLabel.setContentType("text/html");
      authorLabel.setText(authorsWithLinks);
      authorLabel.setHighlighter(null);
      authorLabel.setOpaque(false);
      authorLabel.setEditable(false);
      authorLabel.addHyperlinkListener(hyperlinkOpener);
      add(authorLabel, c);
      
//      c = new GridBagConstraints();
//      c.gridx = 2;
//      c.gridy = 0;
//      c.anchor = GridBagConstraints.NORTHEAST;
//      JLabel categoryLabel = new JLabel(libInfo.categoryName + "  ");
//      font = categoryLabel.getFont();
//      font = font.deriveFont(font.getSize() * 0.7f);
//      categoryLabel.setFont(font);
//      headerPanel.add(categoryLabel, c);
      
      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 1;
      c.weightx = 1;
      c.gridwidth = 3;
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;

      descriptionText = new JTextPane();
      descriptionText.setContentType("text/html");
      descriptionText.setText("<html><body>" + libInfo.description + "</body></html>");
      descriptionText.setHighlighter(null);
      descriptionText.setOpaque(false);
      descriptionText.setEditable(false);
      descriptionText.setMargin(new Insets(0, 0, 10, 5));
      descriptionText.addHyperlinkListener(hyperlinkOpener);
      add(descriptionText, c);
    }
    
    public void addProgressBarAndButton() {
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 3;
      c.gridy = 0;
      c.weighty = 1;
      c.gridheight = 2;
      c.fill = GridBagConstraints.VERTICAL;
      c.anchor = GridBagConstraints.NORTH;
      JPanel rightPane = new JPanel();
      rightPane.setOpaque(false);
      rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.Y_AXIS));
      add(rightPane, c);
      
      installProgressBar = new JProgressBar();
      installProgressBar.setString("");
      installProgressBar.setStringPainted(true);
      installProgressBar.setVisible(false);
      Dimension d = installProgressBar.getPreferredSize();
      d.width = BUTTON_WIDTH;
      installProgressBar.setPreferredSize(d);
      installProgressBar.setMaximumSize(d);
      installProgressBar.setMinimumSize(d);
      installProgressBar.setOpaque(false);
      rightPane.add(installProgressBar);
      installProgressBar.setAlignmentX(CENTER_ALIGNMENT);
      
      rightPane.add(Box.createVerticalGlue());
      
      installOrRemove = new JButton();
      if (libInfo.isInstalled) {
        
      } else {
        installOrRemove.setText("Install");
        ActionListener installLibAction = new ActionListener() {
  
          public void actionPerformed(ActionEvent arg) {
            try {
              URL url = new URL(libInfo.link);
              
              libraryManager.installLibraryFromUrl(url, null);
            } catch (MalformedURLException e) {
              Base.showWarning("Install Failed",
                               "The link fetched from Processing.org is invalid.\n" +
                               "You can still intall this library manually by visiting\n" +
                               "the library's website.", e);
            }
          }
        };
        installOrRemove.addActionListener(installLibAction);
      }
      Dimension installButtonDimensions = installOrRemove.getPreferredSize();
      installButtonDimensions.width = BUTTON_WIDTH;
      installOrRemove.setPreferredSize(installButtonDimensions);
      installOrRemove.setMaximumSize(installButtonDimensions);
      installOrRemove.setMinimumSize(installButtonDimensions);
      installOrRemove.setOpaque(false);
      rightPane.add(installOrRemove);
      installOrRemove.setAlignmentX(CENTER_ALIGNMENT);
      
      // Set the minimum size of this pane to be the sum of the height of the
      // progress bar and install button
      d = installProgressBar.getPreferredSize();
      Dimension d2 = installOrRemove.getPreferredSize();
      d.width = BUTTON_WIDTH;
      d.height = d.height+d2.height;
      rightPane.setMinimumSize(d);
      rightPane.setPreferredSize(d);
    }

    public void setSelected(boolean doShow) {
      isSelected = doShow;
      installOrRemove.setVisible(doShow);
      
//      if (doShow) {
//        authorLabel.removeHyperlinkListener(nullHyperlinkListener);
//        authorLabel.addHyperlinkListener(hyperlinkOpener);
//      } else {
//        authorLabel.removeHyperlinkListener(hyperlinkOpener);
//        authorLabel.addHyperlinkListener(nullHyperlinkListener);
//      }
    }

    /**
     * Updates the sizes of components in this panel given width as a constraint
     */
    public void updateSize(int width) {
      Dimension d = getPreferredSize();
      d.width = width;

      setMaximumSize(d);
      setMinimumSize(d);
      setPreferredSize(d);
      setSize(d);
      
      revalidate();
    }

  }

  public static interface PreferredViewPositionListener {
    
    void handlePreferredLocation(Point p);
    
  }

  public JProgressBar getSetupProgressBar() {
    return setupProgressBar;
  }

}
