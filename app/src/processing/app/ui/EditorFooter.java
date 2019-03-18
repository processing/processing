/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2015-19 The Processing Foundation

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
import java.awt.font.FontRenderContext;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import processing.app.Mode;
import processing.app.Sketch;
import processing.app.contrib.ContributionManager;


/**
 * Console/error/whatever tabs at the bottom of the editor window.
 * This shares a lot of code with EditorHeader and the Manager tabs as well.
 */
public class EditorFooter extends Box {
  // height of this tab bar
  static final int HIGH = Toolkit.zoom(32);

  static final int CURVE_RADIUS = Toolkit.zoom(6);

  static final int TAB_TOP = Toolkit.zoom(2);
  static final int TAB_BOTTOM = Toolkit.zoom(29);
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

  Color updateColor;
  int updateLeft;

  Editor editor;

  List<Tab> tabs = new ArrayList<>();

  Font font;
  int fontAscent;

  Image offscreen;
  int sizeW, sizeH;
  int imageW, imageH;

  Image gradient;
  Color bgColor;

  JPanel cardPanel;
  CardLayout cardLayout;
  Controller controller;

  int updateCount;


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


  public void setUpdateCount(int count) {
    this.updateCount = count;
    repaint();
  }


  public void updateMode() {
    Mode mode = editor.getMode();

    textColor[SELECTED] = mode.getColor("footer.text.selected.color");
    textColor[UNSELECTED] = mode.getColor("footer.text.unselected.color");
    font = mode.getFont("footer.text.font");

    tabColor[SELECTED] = mode.getColor("footer.tab.selected.color");
    tabColor[UNSELECTED] = mode.getColor("footer.tab.unselected.color");

    updateColor = mode.getColor("footer.updates.color");

    gradient = mode.makeGradient("footer", 400, HIGH);
    // Set the default background color in case the window size reported
    // incorrectly by the OS, or we miss an update event of some kind
    // https://github.com/processing/processing/issues/3919
    bgColor = mode.getColor("footer.gradient.bottom");
    setBackground(bgColor);
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
          if (updateCount > 0 && x > updateLeft) {
            ContributionManager.openUpdates();
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
        offscreen = Toolkit.offscreenGraphics(this, imageW, imageH);
      }

      Graphics g = offscreen.getGraphics();
      g.setFont(font);  // need to set this each time through
      if (fontAscent == 0) {
        fontAscent = (int) Toolkit.getAscent(g);
      }

      Graphics2D g2 = Toolkit.prepareGraphics(g);

      g.setColor(tabColor[SELECTED]);
      // can't be done with lines, b/c retina leaves tiny hairlines
      g.fillRect(0, 0, imageW, Toolkit.zoom(2));

      g.drawImage(gradient, 0, Toolkit.zoom(2), imageW, imageH, this);

      // reset all tab positions
      for (Tab tab : tabs) {
        tab.textWidth = (int)
          font.getStringBounds(tab.name, g2.getFontRenderContext()).getWidth();
      }

      // now actually draw the tabs
      drawTabs(Editor.LEFT_GUTTER, g2);

      // the number of updates available in the Manager
      drawUpdates(g2);

      screen.drawImage(offscreen, 0, 0, imageW, imageH, null);
    }


    /**
     * @param left starting position from the left
     * @param g graphics context, or null if we're not drawing
     */
    private void drawTabs(int left, Graphics2D g) {
      int x = left;

      for (Tab tab : tabs) {
        tab.left = x;
        x += MARGIN;
        if (tab.hasIcon()) {
          x += ICON_WIDTH + MARGIN;
        }
        x += tab.textWidth + MARGIN;
        tab.right = x;

        tab.draw(g);
        x += TAB_BETWEEN;
      }
    }


    private void drawUpdates(Graphics2D g2) {
      if (updateCount != 0) {
        FontRenderContext frc = g2.getFontRenderContext();
        final int GAP = Toolkit.zoom(5);
        final String updateLabel = "Updates";
        String updatesStr = "" + updateCount;
        double countWidth = font.getStringBounds(updatesStr, frc).getWidth();
        if (fontAscent > countWidth) {
          countWidth = fontAscent;
        }
        float diameter = (float) (countWidth * 1.65f);
        float ex = getWidth() - Editor.RIGHT_GUTTER - diameter;
        float ey = (getHeight() - diameter) / 2;
        g2.setColor(updateColor);
        g2.fill(new Ellipse2D.Float(ex, ey, diameter, diameter));
        g2.setColor(textColor[SELECTED]);
        int baseline = (getHeight() + fontAscent) / 2;
        g2.drawString(updatesStr, (int) (ex + (diameter - countWidth)/2), baseline);
        double updatesWidth = font.getStringBounds(updateLabel, frc).getWidth();
        g2.setColor(textColor[UNSELECTED]);
        updateLeft = (int) (ex - updatesWidth - GAP);
        g2.drawString(updateLabel, updateLeft, baseline);
      }
    }


    public Dimension getPreferredSize() {
      return new Dimension(Toolkit.zoom(300), HIGH);
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
