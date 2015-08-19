/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2015 The Processing Foundation

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

package processing.app.ui;

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

import processing.app.Mode;
import processing.app.Sketch;


/**
 * Console/error/whatever tabs at the bottom of the editor window.
 */
public class EditorFooter extends Box {
  // height of this tab bar
  static final int HIGH = 32;

  static final int CURVE_RADIUS = 6;

  static final int TAB_TOP = 2;
  static final int TAB_BOTTOM = 29;
  // amount of extra space between individual tabs
  static final int TAB_BETWEEN = 2;
  // amount of margin on the left/right for the text on the tab
  static final int MARGIN = 14;

  static final int ICON_WIDTH = 16;
  static final int ICON_HEIGHT = 16;
  static final int ICON_TOP = 7;
  static final int ICON_MARGIN = 7;

  Color[] textColor = new Color[2];
  Color[] tabColor = new Color[2];
//  Color errorColor;

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


  /** Add a panel with no icon. */
  public void addPanel(Component comp, String name) {
    addPanel(comp, name, null);
  }


  /**
   * Add a panel with a name and icon.
   * @param comp Component that will be shown when this tab is selected
   * @param name Title to appear on the tab itself
   * @param icon Prefix of the file name for the icon
   */
  public void addPanel(Component comp, String name, String icon) {
    tabs.add(new Tab(comp, name, icon));
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

//    errorColor = mode.getColor("status.error.bgcolor");

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

      g.setColor(tabColor[SELECTED]);
      g.fillRect(0, 0, imageW, 2);

      g.drawImage(gradient, 0, 2, imageW, imageH, this);

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
        tab.left = x;
        x += MARGIN;
        if (tab.hasIcon()) {
          x += ICON_WIDTH + MARGIN;
        }
        x += tab.textWidth + MARGIN;
        tab.right = x;

        // if drawing and not just placing
        if (g != null) {
          tab.draw(g);
          /*
          int state = tab.isCurrent() ? SELECTED : UNSELECTED;
          g.setColor(tabColor[state]);
          if (tab.notification) {
            g.setColor(errorColor);
          }
          //drawTab(g, tab.left, tab.right, tab.isFirst(), tab.isLast());
          tab.draw(g);

          int textLeft = tab.getTextLeft();
          if (tab.notification && state == UNSELECTED) {
            g.setColor(Color.LIGHT_GRAY);
          } else {
            g.setColor(textColor[state]);
          }
          int tabHeight = TAB_BOTTOM - TAB_TOP;
          int baseline = TAB_TOP + (tabHeight + fontAscent) / 2;
          g.drawString(tab.name, textLeft, baseline);
          */
        }
        x += TAB_BETWEEN;
      }
    }


    /*
    private void drawTab(Graphics g, int left, int right,
                         boolean leftNotch, boolean rightNotch) {
      Graphics2D g2 = (Graphics2D) g;
      g2.fill(Toolkit.createRoundRect(left, TAB_TOP, right, TAB_BOTTOM, 0, 0,
                                      rightNotch ? CURVE_RADIUS : 0,
                                      leftNotch ? CURVE_RADIUS : 0));
    }
    */


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

    Image enabledIcon;
    Image selectedIcon;

    int left;
    int right;
    int textWidth;

    Tab(Component comp, String name, String icon) {
      this.comp = comp;
      this.name = name;

      if (icon != null) {
        Mode mode = editor.getMode();
        final int res = Toolkit.highResDisplay() ? 2 : 1;
        enabledIcon = mode.loadImage(icon + "-enabled-" + res + "x.png");
        selectedIcon = mode.loadImage(icon + "-selected-" + res + "x.png");
        if (selectedIcon == null) {
          selectedIcon = enabledIcon;  // use this as the default
        }
      }
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

    int getTextLeft() {
      int links = left;
      if (enabledIcon != null) {
        links += ICON_WIDTH + ICON_MARGIN;
      }
      return links + ((right - links) - textWidth) / 2;
    }

    boolean hasIcon() {
      return enabledIcon != null;
    }

    void draw(Graphics g) {
      int state = isCurrent() ? SELECTED : UNSELECTED;
      g.setColor(tabColor[state]);
//      if (notification) {
//        g.setColor(errorColor);
//      }

      Graphics2D g2 = (Graphics2D) g;
      g2.fill(Toolkit.createRoundRect(left, TAB_TOP, right, TAB_BOTTOM, 0, 0,
                                      isLast() ? CURVE_RADIUS : 0,
                                      isFirst() ? CURVE_RADIUS : 0));

      if (hasIcon()) {
        Image icon = (isCurrent() || notification) ? selectedIcon : enabledIcon;
        g.drawImage(icon, left + MARGIN, ICON_TOP, ICON_WIDTH, ICON_HEIGHT, null);
      }

      int textLeft = getTextLeft();
      if (notification && state == UNSELECTED) {
        g.setColor(textColor[SELECTED]);
      } else {
        g.setColor(textColor[state]);
      }
      int tabHeight = TAB_BOTTOM - TAB_TOP;
      int baseline = TAB_TOP + (tabHeight + fontAscent) / 2;
      g.drawString(name, textLeft, baseline);
    }
  }
}
