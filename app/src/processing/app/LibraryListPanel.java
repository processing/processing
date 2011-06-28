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

import javax.swing.*;
import javax.swing.text.*;

import java.awt.event.*;
import java.awt.font.*;
import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.*;

import processing.app.LibraryListing.LibraryInfo;
import processing.app.LibraryManager.LibraryInstaller;

public class LibraryListPanel extends JPanel implements Scrollable {
  
  HashMap<LibraryInfo, LibraryPanel> libPanelsByInfo;
  private PreferredViewPositionListener preferredViewPositionListener;
  private LibraryManager libraryManager;
  LibraryListing libraries;

  public LibraryListPanel(LibraryManager libraryManager) {
    super();
    
    preferredViewPositionListener = null;
    this.libraryManager = libraryManager;
    libraries = libraryManager.getLibraryListing(null);
    
    setLayout(new GridBagLayout());
    
    libPanelsByInfo = new HashMap<LibraryInfo, LibraryPanel>();
    
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
    
    GridBagConstraints verticalFill = new GridBagConstraints();
    verticalFill.fill = GridBagConstraints.VERTICAL;
    verticalFill.weighty = 1;
    verticalFill.gridx = 0;
    verticalFill.gridy = row++;
    add(Box.createVerticalGlue(), verticalFill);
    
    setFocusable(true);
    addMouseListener(new MouseAdapter() {

      public void mousePressed(MouseEvent mouseEvent) {
        requestFocusInWindow();
      }
    });

  }
  
  public void filterLibraries(String category, List<String> filters) {
    
    List<LibraryInfo> hiddenLibraries = libraries.getAllLibararies();
    for (LibraryInfo lib : libraries.getFilteredLibraryList(category, filters)) {
      libPanelsByInfo.get(lib).setVisible(true);
      hiddenLibraries.remove(lib);
    }
    
    for (LibraryInfo lib : hiddenLibraries) {
      libPanelsByInfo.get(lib).setVisible(false);
    }
    
    updateLibraryListSizeAndPosition(0, false, null);
  }
  
  /**
   * Updates the widths of all library panels in this library list.
   */
  public void setWidth(int newWidth) {
    for (Component c : getComponents()) {
      if (c instanceof LibraryPanel) {
        ((LibraryPanel) c).updateSize(newWidth);
      }
    }
    
    updateLibraryListSizeAndPosition(0, true, null);
  }

  /**
   * Updates the width and height of this library list based on the sizes of the
   * library panes it contains.
   * 
   * @param obtainedSpace
   *          the height of the space obtained from the closing of the previous
   *          panel
   * @param obtainedSpaceIsAbove
   *          true if the panel that was closed was above the panel which was
   *          just opened
   * @param expandedPanel
   *          the panel that was expanded, or null if none
   */
  private void updateLibraryListSizeAndPosition(int obtainedSpace,
                                                boolean obtainedSpaceIsAbove,
                                                LibraryPanel expandedPanel) {

    int height = 0;
    int width = 0;
    
//    int selectedTop = 0, selectedBottom = 0;
//    if (expandedPanel != null) {
//      selectedTop = expandedPanel.getLocation().y;
//      selectedBottom = selectedTop + expandedPanel.getHeight();
//    }
  
    for (Component c : getComponents()) {
      if (c.isVisible()) {
        if (c instanceof LibraryPanel) {
          Dimension d = c.getPreferredSize();
          if (d.width > width) {
            width = d.width;
          }
          height += d.height;
        }
      }
    }
    
    Rectangle r = getVisibleRect();
    if (obtainedSpaceIsAbove && preferredViewPositionListener != null) {

      Point p = new Point(r.x, r.y);
      p.y -= obtainedSpace;

      preferredViewPositionListener.handlePreferredLocation(p);
    }

    setPreferredSize(new Dimension(width, height));
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
    for (Component c : getComponents()) {
      if (c instanceof LibraryPanel) {
        LibraryPanel libPanel = (LibraryPanel) c;
        
        if (libPanel.isVisible()) {
          if (libPanel.isInfoShown) {
            libPanel.setBackground(UIManager.getColor("List.selectionBackground"));
            cascadeForgroundColor(libPanel, UIManager.getColor("List.selectionForeground"));
            libPanel.setBorder(UIManager.getBorder("List.focusCellHighlightBorder"));
          } else {
            if (Base.isMacOS()) {
              if (count % 2 == 1) {
                libPanel.setBorder(UIManager.getBorder("List.evenRowBackgroundPainter"));
              } else {
                libPanel.setBorder(UIManager.getBorder("List.oddRowBackgroundPainter"));
              }
            }
            libPanel.setBackground(UIManager.getColor("List.background"));
            cascadeForgroundColor(libPanel, UIManager.getColor("List.foreground"));
          }
          
          count++;
        }
      }
    }
  }

  /**
   * Calculates the height in pixels of the text in a JTextArea given a width
   * as a contained. This assumed that both word wrap and wrap-style word are
   * enabled for the JTextArea.
   */
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
              if (nextHeight > bottomOfScrollArea) {
                return nextHeight - bottomOfScrollArea;
              }
            } else {
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
    return false;
  }

  public void setPreferredViewPositionListener(PreferredViewPositionListener preferredViewPositionListener) {
    this.preferredViewPositionListener = preferredViewPositionListener;
  }

  /**
   * Panel that expands and gives a brief overview of a library when clicked.
   */
  class LibraryPanel extends JPanel {
    
    private static final int BUTTON_WIDTH = 100;

    final String unclickedCardId = "unclicked";

    final String clickedCardId = "clicked";

    final int topAndBottomBorder = 2;

    LibraryInfo libInfo;

    JPanel headerPanel;

//    JTextArea briefText;

    JPanel infoPanel;

    JPanel unclickedCard;

    JPanel clickedCard;

    JTextArea description;

    JButton installOrRemove;

    boolean isInfoShown;

    private LibraryPanel(LibraryInfo libInfo) {
      this.libInfo = libInfo;
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      setBorder(BorderFactory.createEmptyBorder(topAndBottomBorder, 2,
                                                topAndBottomBorder, 2));

      configureHeaderPane();
      configureInfoPane();

      setFocusable(true);
      setShowInfo(false);
      updateColors();
      updateLibraryListSizeAndPosition(0, true, null);

      addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          
          int obtainedSpace = 0;
          boolean obtainedSpaceIsAbove = true;
          
          for (Component c : LibraryListPanel.this.getComponents()) {
            if (c == LibraryPanel.this) {
              obtainedSpaceIsAbove = false;
            } else if (c instanceof LibraryPanel) {
              LibraryPanel lp = (LibraryPanel) c;
              if (lp.isInfoShown) {
                obtainedSpace = lp.description.getSize().height;
                lp.setShowInfo(false);
                break;
              }
            }
          }
          
          setShowInfo(true);
          updateColors();
          updateLibraryListSizeAndPosition(obtainedSpace, obtainedSpaceIsAbove,
                                           LibraryPanel.this);
          requestFocusInWindow();
        }
      });

    }

    /**
     * Create the widgets for the header panel which is visible when the library
     * panel is not clicked
     */
    private void configureHeaderPane() {
      headerPanel = new JPanel();
      headerPanel.setFocusable(true);
      headerPanel.setOpaque(false);
      headerPanel.setLayout(new GridBagLayout());

      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.weightx = 1;
      c.anchor = GridBagConstraints.WEST;
      JLabel nameLabel = new JLabel(libInfo.name);
      headerPanel.add(nameLabel, c);
      
      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 1;
      c.weightx = 1;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.anchor = GridBagConstraints.WEST;
//      briefText = new JTextArea(libInfo.brief);
//      briefText.setHighlighter(null);
//      briefText.setOpaque(false);
//      briefText.setEditable(false);
//      briefText.setLineWrap(true);
//      briefText.setWrapStyleWord(true);
//      Font font = briefText.getFont();
//      font = font.deriveFont(font.getSize() * 0.85f);
//      briefText.setFont(font);
//      headerPanel.add(briefText, c);
      
      c = new GridBagConstraints();
      c.gridx = 1;
      c.gridy = 1;
      c.anchor = GridBagConstraints.EAST;
      headerPanel.add(Box.createRigidArea(new Dimension(BUTTON_WIDTH, 1)), c);
      
      add(headerPanel);
    }

    /**
     * Create the widgets for the info panel which is visible when the library
     * panel is clicked
     */
    private void configureInfoPane() {
      infoPanel = new JPanel();
      infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
      infoPanel.setLayout(new CardLayout());

      unclickedCard = new JPanel();
      clickedCard = new JPanel();

      infoPanel.setOpaque(false);
      unclickedCard.setOpaque(false);
      clickedCard.setOpaque(false);

      infoPanel.setFocusable(true);
      unclickedCard.setFocusable(true);
      clickedCard.setFocusable(true);

      description = new JTextArea(libInfo.description);
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

      clickedCard.setLayout(new BoxLayout(clickedCard, BoxLayout.X_AXIS));

      description.setHighlighter(null);
      description.setOpaque(false);
      description.setEditable(false);
      description.setLineWrap(true);
      description.setWrapStyleWord(true);
      Font font = this.description.getFont();
      font = font.deriveFont(font.getSize() * 0.9f);
      description.setFont(font);

      clickedCard.add(description);
      clickedCard.add(Box.createHorizontalGlue());
      clickedCard.add(installOrRemove);

      installOrRemove.setAlignmentY(Component.BOTTOM_ALIGNMENT);
      description.setAlignmentY(Component.BOTTOM_ALIGNMENT);

      Dimension installButtonDimensions = installOrRemove.getPreferredSize();
      installButtonDimensions.width = BUTTON_WIDTH;
      installOrRemove.setPreferredSize(installButtonDimensions);

      infoPanel.add(unclickedCard, unclickedCardId);
      infoPanel.add(clickedCard, clickedCardId);

      add(infoPanel);
    }

    /**
     * Turns on/off the info panel which contains a brief description of the
     * library and a button to Install/Remove a library.
     */
    public void setShowInfo(boolean doShow) {
      isInfoShown = doShow;
      CardLayout cardLayout = (CardLayout) infoPanel.getLayout();

      if (isInfoShown) {
        cardLayout.show(infoPanel, clickedCardId);
      } else {
        cardLayout.show(infoPanel, unclickedCardId);
      }

      int width = getSize().width;
      updateSize(width > 0 ? width : 100);
    }

    /**
     * Updates the sizes of components in this panel given width as a constraint
     */
    public void updateSize(int width) {
      if (isInfoShown) {
        Dimension textDimentions = description.getPreferredSize();
        textDimentions.width = width - installOrRemove.getPreferredSize().width;
        textDimentions.height = calculateHeight(description, textDimentions.width);

        description.setMaximumSize(textDimentions);
        description.setMinimumSize(textDimentions);
        description.setPreferredSize(textDimentions);
        description.setSize(textDimentions);
      }

      Dimension d;
      if (isInfoShown) {
        d = headerPanel.getPreferredSize();
        d.width = width;
        d.height += clickedCard.getPreferredSize().height + 2
            * topAndBottomBorder;
      } else {
        d = headerPanel.getPreferredSize();
        d.width = width;
        d.height += 2 * topAndBottomBorder;
      }

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

}
