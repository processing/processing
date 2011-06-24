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

import javax.swing.*;
import javax.swing.text.*;

import java.awt.event.*;
import java.awt.font.*;
import java.awt.*;
import java.text.*;
import java.util.*;

import processing.app.LibraryListing.LibraryInfo;
import processing.app.LibraryManager.*;

public class LibraryListPanel extends JPanel {
  
  public LibraryListPanel(LibraryListing libraries) {
    super();
    
    setLayout(new GridBagLayout());
    int row = 0;
    for (LibraryInfo libInfo : libraries.getAllLibararies()) {
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.weightx = 1;
      c.gridx = 0;
      c.gridy = row++;
      
       add(new LibraryPanel(libInfo), c);
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
  
  /**
   * Updates the widths of all library panels in this library list.
   */
  public void setWidth(int newWidth) {
    for (Component c : getComponents()) {
      if (c instanceof LibraryPanel) {
        ((LibraryPanel) c).updateSize(newWidth);
      }
    }
    
    updateLibraryListSize();
  }

  /**
   * Updates the width and height of this library list based on the sizes of
   * the library panes it contains.
   */
  private void updateLibraryListSize() {
    int height = 0;
    int width = 0;
  
    for (Component c : getComponents()) {
      if (c.isVisible()) {
        if (c instanceof LibraryListPanel) {
          Dimension d = c.getSize();
          if (d.width > width) {
            width = d.width;
          }
          height += d.height;
        }
      }
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

  /**
     * Panel that expands and gives a brief overview of a library when clicked.
     */
    class LibraryPanel extends JPanel {
      final String unclickedCardId = "unclicked";
      final String clickedCardId = "clicked";
      final int topAndBottomBorder = 2;
      
      LibraryInfo libInfo;
      
      JPanel headerPanel;
      JLabel nameLabel;
      
      JPanel infoPanel;
      JPanel unclickedCard;
      JPanel clickedCard;
      JTextArea briefText;
      JButton installOrRemove;
      
      boolean isInfoShown;
      
      
      private LibraryPanel(LibraryInfo libInfo) {
        this.libInfo = libInfo;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        setBorder(BorderFactory.createEmptyBorder(topAndBottomBorder, 2, topAndBottomBorder, 2));
        
        configureHeaderPane();
        configureInfoPane();
        
        setFocusable(true);
        setShowInfo(false);
        updateColors();
        updateLibraryListSize();
        
        addMouseListener(new MouseAdapter() {
          public void mouseClicked(MouseEvent e) {
            for (Component c : LibraryListPanel.this.getComponents()) {
              if (c != LibraryPanel.this && c instanceof LibraryPanel) {
                LibraryPanel lp = (LibraryPanel) c;
                lp.setShowInfo(false);
              }
            }
            
            setShowInfo(true);
            updateColors();
            updateLibraryListSize();
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
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        
        nameLabel = new JLabel(libInfo.name);
        headerPanel.add(this.nameLabel);
        headerPanel.add(Box.createHorizontalGlue());
        
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
        
        briefText = new JTextArea(libInfo.description);
        installOrRemove = new JButton();
  //    installOrRemove = new JButton("Install");
  //    ActionListener installLibAction = new ActionListener() {
  //
  //      public void actionPerformed(ActionEvent arg) {
  //        try {
  //          URL url = new URL(libraryUri.getText());
  //          // System.out.println("Installing library: " + url);
  //          File libFile = downloadLibrary(url);
  //          if (libFile != null) {
  //            installLibrary(libFile);
  //          }
  //        } catch (MalformedURLException e) {
  //          System.err.println("Malformed URL");
  //        }
  //        libraryUri.setText("");
  //      }
  //    };
  //    installOrRemove.addActionListener(installLibAction);
        
        clickedCard.setLayout(new BoxLayout(clickedCard, BoxLayout.X_AXIS));
        
        briefText.setHighlighter(null);
        briefText.setOpaque(false);
        briefText.setEditable(false);
        briefText.setLineWrap(true);
        briefText.setWrapStyleWord(true);
        Font font = this.briefText.getFont();
        font = font.deriveFont(font.getSize() * 0.9f);
        briefText.setFont(font);
        
        clickedCard.add(briefText);
        clickedCard.add(Box.createHorizontalGlue());
        clickedCard.add(installOrRemove);
        
        installOrRemove.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        briefText.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        
        installOrRemove.setText("Install");
        Dimension installButtonDimensions = installOrRemove.getPreferredSize();
        installButtonDimensions.width = 100;
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
        Dimension textDimentions = briefText.getPreferredSize();
        textDimentions.width = width - installOrRemove.getPreferredSize().width;
        textDimentions.height = calculateHeight(briefText, textDimentions.width);
        
        briefText.setMaximumSize(textDimentions);
        briefText.setMinimumSize(textDimentions);
        briefText.setPreferredSize(textDimentions);
        //briefText.setSize(textDimentions);
  
        Dimension d;
        if (isInfoShown) {
          d = headerPanel.getPreferredSize();
          d.width = width;
          d.height += clickedCard.getPreferredSize().height + 2 * topAndBottomBorder;
        } else {
          d = headerPanel.getPreferredSize();
          d.width = width;
          d.height += 2 * topAndBottomBorder;
        }
        
        setMaximumSize(d);
        setMinimumSize(d);
        setPreferredSize(d);
        //setSize(d);
        
        revalidate();
      }
      
    }

}
