/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-15 The Processing Foundation
  Copyright (c) 2004-13 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;


/**
 * Console/error/whatever tabs at the bottom of the editor window.
 */
public class EditorFooter extends Box {
  // height of this tab bar
  static final int HIGH = 29;

  static final int CURVE_RADIUS = 6;

  static final int TAB_TOP = 0;
  static final int TAB_BOTTOM = 23;
  // amount of extra space between individual tabs
  static final int TAB_BETWEEN = 3;
  // amount of margin on the left/right for the text on the tab
  static final int TEXT_MARGIN = 16;

  Color[] textColor = new Color[2];
  Color[] tabColor = new Color[2];
  Color errorColor;

  Editor editor;

  List<Tab> tabs = new ArrayList<>();

  Font font;
  int fontAscent;

  JMenu menu;
  JPopupMenu popup;

  static final int UNSELECTED = 0;
  static final int SELECTED = 1;

  Image offscreen;
  int sizeW, sizeH;
  int imageW, imageH;

  Image gradient;

  JPanel cardPanel;
  CardLayout cardLayout;
  Controller controller;


  public EditorFooter(Editor eddie) {
    super(BoxLayout.Y_AXIS);
    this.editor = eddie;

    updateMode();

    cardLayout = new CardLayout();
    cardPanel = new JPanel(cardLayout);
    add(cardPanel);

    controller = new Controller();
    add(controller);
  }


  public void addPanel(String name, Component comp) {
    tabs.add(new Tab(name, comp));
    cardPanel.add(name, comp);
  }


//  public void setPanel(int index) {
//    cardLayout.show(cardPanel, tabs.get(index).name);
//  }


  public void setPanel(Component comp) {
    for (Tab tab : tabs) {
      if (tab.comp == comp) {
        cardLayout.show(cardPanel, tab.name);
        repaint();
      }
    }
  }


  public void setNotification(Component comp, boolean note) {
    for (Tab tab : tabs) {
      if (tab.comp == comp) {
        tab.notification = note;
        repaint();
      }
    }
  }


  public void updateMode() {
    Mode mode = editor.getMode();

    textColor[SELECTED] = mode.getColor("footer.text.selected.color");
    textColor[UNSELECTED] = mode.getColor("footer.text.unselected.color");
    font = mode.getFont("footer.text.font");

    tabColor[SELECTED] = mode.getColor("footer.tab.selected.color");
    tabColor[UNSELECTED] = mode.getColor("footer.tab.unselected.color");

    errorColor = mode.getColor("status.error.bgcolor");

    gradient = mode.makeGradient("footer", 400, HIGH);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class Controller extends JComponent {

    Controller() {
      addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          int x = e.getX();
          for (Tab tab : tabs) {
            if (tab.contains(x)) {
              //editor.setFooterPanel(tab.index);
              cardLayout.show(cardPanel, tab.name);
              repaint();
            }
          }
        }
      });
    }

    public void paintComponent(Graphics screen) {
      if (screen == null) return;
      Sketch sketch = editor.getSketch();
      if (sketch == null) return;  // possible?

      Dimension size = getSize();
      if ((size.width != sizeW) || (size.height != sizeH)) {
        // component has been resized

        if ((size.width > imageW) || (size.height > imageH)) {
          // nix the image and recreate, it's too small
          offscreen = null;

        } else {
          // if the image is larger than necessary, no need to change
          sizeW = size.width;
          sizeH = size.height;
        }
      }

      if (offscreen == null) {
        sizeW = size.width;
        sizeH = size.height;
        imageW = sizeW;
        imageH = sizeH;
        if (Toolkit.highResDisplay()) {
          offscreen = createImage(imageW*2, imageH*2);
        } else {
          offscreen = createImage(imageW, imageH);
        }
      }

      Graphics g = offscreen.getGraphics();
      g.setFont(font);  // need to set this each time through
      if (fontAscent == 0) {
        fontAscent = (int) Toolkit.getAscent(g);
      }

      Graphics2D g2 = Toolkit.prepareGraphics(g);
      g.drawImage(gradient, 0, 0, imageW, imageH, this);

      // reset all tab positions
      for (Tab tab : tabs) {
        tab.textWidth = (int)
          font.getStringBounds(tab.name, g2.getFontRenderContext()).getWidth();
      }

      // now actually draw the tabs
      placeTabs(Editor.LEFT_GUTTER, g2);

//      // draw the two pixel line that extends left/right below the tabs
//      g.setColor(tabColor[SELECTED]);
//      // can't be done with lines, b/c retina leaves tiny hairlines
//      g.fillRect(Editor.LEFT_GUTTER, TAB_BOTTOM,
//                 editor.getTextArea().getWidth() - Editor.LEFT_GUTTER, 2);

      screen.drawImage(offscreen, 0, 0, imageW, imageH, null);
    }


    /**
     * @param left starting position from the left
     * @param g graphics context, or null if we're not drawing
     */
    private void placeTabs(int left, Graphics2D g) {
      int x = left;

      for (Tab tab : tabs) {
        int state = tab.isCurrent() ? SELECTED : UNSELECTED;
        tab.left = x;
        x += TEXT_MARGIN;
        x += tab.textWidth + TEXT_MARGIN;
        tab.right = x;

        // if drawing and not just placing
        if (g != null) {
          g.setColor(tabColor[state]);
          if (tab.notification) {
            g.setColor(errorColor);
          }
          drawTab(g, tab.left, tab.right, tab.isFirst(), tab.isLast());

          int textLeft = tab.left + ((tab.right - tab.left) - tab.textWidth) / 2;
          if (tab.notification && state == UNSELECTED) {
            g.setColor(Color.LIGHT_GRAY);
          } else {
            g.setColor(textColor[state]);
          }
          int tabHeight = TAB_BOTTOM - TAB_TOP;
          int baseline = TAB_TOP + (tabHeight + fontAscent) / 2;
          g.drawString(tab.name, textLeft, baseline);
        }
        x += TAB_BETWEEN;
      }
    }


    private void drawTab(Graphics g, int left, int right,
                         boolean leftNotch, boolean rightNotch) {
      Graphics2D g2 = (Graphics2D) g;
      g2.fill(Toolkit.createRoundRect(left, TAB_TOP, right, TAB_BOTTOM, 0, 0,
                                      rightNotch ? CURVE_RADIUS : 0,
                                      leftNotch ? CURVE_RADIUS : 0));
    }


    public Dimension getPreferredSize() {
      return new Dimension(300, HIGH);
    }


    public Dimension getMinimumSize() {
      return getPreferredSize();
    }


    public Dimension getMaximumSize() {
      return new Dimension(super.getMaximumSize().width, HIGH);
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class Tab {
    String name;
    Component comp;
    boolean notification;

    int left;
    int right;
    int textWidth;

    Tab(String name, Component comp) {
      this.name = name;
      this.comp = comp;
    }

    boolean contains(int x) {
      return x >= left && x <= right;
    }

    boolean isCurrent() {
      return comp.isVisible();
    }

    boolean isFirst() {
      return tabs.get(0) == this;
    }

    boolean isLast() {
      return tabs.get(tabs.size() - 1) == this;
    }
  }
}
