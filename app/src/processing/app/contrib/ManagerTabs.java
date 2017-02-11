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

package processing.app.contrib;

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
import javax.swing.border.EmptyBorder;

import processing.app.Base;
import processing.app.Mode;
import processing.app.ui.Toolkit;


/**
 * Console/error/whatever tabs at the bottom of the editor window.
 */
public class ManagerTabs extends Box {
  // height of this tab bar
  static final int HIGH = Toolkit.zoom(34);

  // amount of space around the entire window
  static final int BORDER = Toolkit.zoom(8);

  static final int CURVE_RADIUS = Toolkit.zoom(6);

  static final int TAB_TOP = Toolkit.zoom(0);
  static final int TAB_BOTTOM = HIGH - Toolkit.zoom(2);
  // amount of extra space between individual tabs
  static final int TAB_BETWEEN = Toolkit.zoom(2);
  // amount of margin on the left/right for the text on the tab
  static final int MARGIN = Toolkit.zoom(14);

  static final int ICON_WIDTH = Toolkit.zoom(16);
  static final int ICON_HEIGHT = Toolkit.zoom(16);
  static final int ICON_TOP = Toolkit.zoom(7);
  static final int ICON_MARGIN = Toolkit.zoom(7);

  static final int UNSELECTED = 0;
  static final int SELECTED = 1;

  Color[] textColor = new Color[2];
  Color[] tabColor = new Color[2];

  List<Tab> tabList = new ArrayList<>();

  Mode mode;

  Font font;
  int fontAscent;

  Image offscreen;
  int sizeW, sizeH;
  int imageW, imageH;

  Image gradient;

  JPanel cardPanel;
  CardLayout cardLayout;
  Controller controller;

  Component currentPanel;


  public ManagerTabs(Base base) {
    super(BoxLayout.Y_AXIS);

    // A mode shouldn't actually override these, they're coming from theme.txt.
    // But use the default (Java) mode settings just in case.
    mode = base.getDefaultMode();

    textColor[SELECTED] = mode.getColor("manager.tab.text.selected.color");
    textColor[UNSELECTED] = mode.getColor("manager.tab.text.unselected.color");
    font = mode.getFont("manager.tab.text.font");

    tabColor[SELECTED] = mode.getColor("manager.tab.selected.color");
    tabColor[UNSELECTED] = mode.getColor("manager.tab.unselected.color");

    gradient = mode.makeGradient("manager.tab", Toolkit.zoom(400), HIGH);

    setBorder(new EmptyBorder(BORDER, BORDER, BORDER, BORDER));

    controller = new Controller();
    add(controller);

    cardLayout = new CardLayout();
    cardPanel = new JPanel(cardLayout);
    add(cardPanel);
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
    if (tabList.isEmpty()) {
      currentPanel = comp;
    }
    tabList.add(new Tab(comp, name, icon));
    cardPanel.add(name, comp);
  }


//  public void setPanel(int index) {
//    cardLayout.show(cardPanel, tabs.get(index).name);
//  }


  public void setPanel(Component comp) {
    for (Tab tab : tabList) {
      if (tab.comp == comp) {
        currentPanel = comp;
        cardLayout.show(cardPanel, tab.name);
        repaint();
      }
    }
  }


  public Component getPanel() {
    return currentPanel;
  }


  public void setNotification(Component comp, boolean note) {
    for (Tab tab : tabList) {
      if (tab.comp == comp) {
        tab.notification = note;
        repaint();
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class Controller extends JComponent {

    Controller() {
      addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          int x = e.getX();
          for (Tab tab : tabList) {
            if (tab.contains(x)) {
              //cardLayout.show(cardPanel, tab.name);
              setPanel(tab.comp);
              repaint();
            }
          }
        }
      });
    }

    public void paintComponent(Graphics screen) {
      if (screen == null) return;

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
        offscreen = Toolkit.offscreenGraphics(this, imageW, imageH);
      }

      Graphics g = offscreen.getGraphics();
      g.setFont(font);  // need to set this each time through
      if (fontAscent == 0) {
        fontAscent = (int) Toolkit.getAscent(g);
      }

      Graphics2D g2 = Toolkit.prepareGraphics(g);

      g.drawImage(gradient, 0, 0, imageW, imageH, this);

      g.setColor(tabColor[SELECTED]);
      // draw the two pixel line that extends left/right below the tabs
      // can't be done with lines, b/c retina leaves tiny hairlines
      g.fillRect(0, TAB_BOTTOM, imageW, Toolkit.zoom(2));

      // reset all tab positions
      for (Tab tab : tabList) {
        tab.textWidth = (int)
          font.getStringBounds(tab.name, g2.getFontRenderContext()).getWidth();
      }

      placeTabs(0);
      // now actually draw the tabs
      drawTabs(g2);

      screen.drawImage(offscreen, 0, 0, imageW, imageH, null);
    }


    /**
     * @param left starting position from the left
     * @param g graphics context, or null if we're not drawing
     */
    private void placeTabs(int left) {  //, Graphics2D g) {
      int x = left;

      for (Tab tab : tabList) {
        tab.left = x;
        x += MARGIN;
        if (tab.hasIcon()) {
          x += ICON_WIDTH + MARGIN;
        }
        x += tab.textWidth + MARGIN;
        tab.right = x;

//        // if drawing and not just placing
//        if (g != null) {
//          tab.draw(g);
//        }
        x += TAB_BETWEEN;
      }
      // Align the final tab (the "updates") to the right-hand side
      Tab lastTab = tabList.get(tabList.size() - 1);
      int offset = getWidth() - lastTab.right;
      lastTab.left += offset;
      lastTab.right += offset;
    }


    private void drawTabs(Graphics2D g) {
      for (Tab tab: tabList) {
        tab.draw(g);
      }
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

    Image enabledIcon;
    Image selectedIcon;

    int left;
    int right;
    int textWidth;

    Tab(Component comp, String name, String icon) {
      this.comp = comp;
      this.name = name;

      if (icon != null) {
        enabledIcon = mode.loadImageX(icon + "-enabled");
        selectedIcon = mode.loadImageX(icon + "-selected");
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

    boolean hasLeftNotch() {
      return (tabList.get(0) == this ||
              tabList.get(tabList.size() - 1) == this);
    }

    boolean hasRightNotch() {
      return (tabList.get(tabList.size() - 1) == this ||
              tabList.get(tabList.size() - 2) == this);

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

      Graphics2D g2 = (Graphics2D) g;
//      g2.fill(Toolkit.createRoundRect(left, TAB_TOP, right, TAB_BOTTOM, 0, 0,
//                                      isLast() ? CURVE_RADIUS : 0,
//                                      hastLeftCurve() ? CURVE_RADIUS : 0));
      g2.fill(Toolkit.createRoundRect(left, TAB_TOP,
                                      right, TAB_BOTTOM,
                                      hasLeftNotch() ? CURVE_RADIUS : 0,
                                      hasRightNotch() ? CURVE_RADIUS : 0,
                                      0, 0));

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
