/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
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

package processing.app.ui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.util.Arrays;

import javax.swing.*;

import processing.app.Language;
import processing.app.Messages;
import processing.app.Mode;
import processing.app.Platform;
import processing.app.Sketch;
import processing.app.SketchCode;


/**
 * Sketch tabs at the top of the editor window.
 */
public class EditorHeader extends JComponent {
  // height of this tab bar
  static final int HIGH = Toolkit.zoom(29);

  static final int ARROW_TAB_WIDTH = Toolkit.zoom(18);
  static final int ARROW_TOP = Toolkit.zoom(11);
  static final int ARROW_BOTTOM = Toolkit.zoom(18);
  static final int ARROW_WIDTH = Toolkit.zoom(6);

  static final int CURVE_RADIUS = Toolkit.zoom(6);

  static final int TAB_TOP = 0;
  static final int TAB_BOTTOM = Toolkit.zoom(27);
  // amount of extra space between individual tabs
  static final int TAB_BETWEEN = Toolkit.zoom(3);
  // amount of margin on the left/right for the text on the tab
  static final int TEXT_MARGIN = Toolkit.zoom(16);
  // width of the tab when no text visible
  // (total tab width will be this plus TEXT_MARGIN*2)
  static final int NO_TEXT_WIDTH = Toolkit.zoom(16);

  Color textColor[] = new Color[2];
  Color tabColor[] = new Color[2];
  Color modifiedColor;
  Color arrowColor;

  Editor editor;

  Tab[] tabs = new Tab[0];
  Tab[] visitOrder;

  Font font;
  int fontAscent;

  JMenu menu;
  JPopupMenu popup;

  int menuLeft;
  int menuRight;

  static final int UNSELECTED = 0;
  static final int SELECTED = 1;

  Image offscreen;
  int sizeW, sizeH;
  int imageW, imageH;

  String lastNoticeName;

  Image gradient;


  public EditorHeader(Editor eddie) {
    this.editor = eddie;

    updateMode();

    addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          int x = e.getX();
          int y = e.getY();

          if ((x > menuLeft) && (x < menuRight)) {
            popup.show(EditorHeader.this, x, y);
          } else {
            Sketch sketch = editor.getSketch();
            for (Tab tab : tabs) {
              if (tab.contains(x)) {
                sketch.setCurrentCode(tab.index);
                repaint();
              }
            }
          }
        }

        public void mouseExited(MouseEvent e) {
          // only clear if it's been set
          if (lastNoticeName != null) {
            // only clear if it's the same as what we set it to
            editor.clearNotice(lastNoticeName);
            lastNoticeName = null;
          }
        }
    });

    addMouseMotionListener(new MouseMotionAdapter() {
        public void mouseMoved(MouseEvent e) {
          int x = e.getX();
          for (Tab tab : tabs) {
            if (tab.contains(x) && !tab.textVisible) {
              lastNoticeName = editor.getSketch().getCode(tab.index).getPrettyName();
              editor.statusNotice(lastNoticeName);
            }
          }
        }
      });
  }


  public void updateMode() {
    Mode mode = editor.getMode();

    textColor[SELECTED] = mode.getColor("header.text.selected.color");
    textColor[UNSELECTED] = mode.getColor("header.text.unselected.color");
    font = mode.getFont("header.text.font");

    tabColor[SELECTED] = mode.getColor("header.tab.selected.color");
    tabColor[UNSELECTED] = mode.getColor("header.tab.unselected.color");

    arrowColor = mode.getColor("header.tab.arrow.color");
    //modifiedColor = mode.getColor("editor.selection.color");
    modifiedColor = mode.getColor("header.tab.modified.color");

    gradient = mode.makeGradient("header", 400, HIGH);
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
//    Toolkit.dpiStroke(g2);

    g.drawImage(gradient, 0, 0, imageW, imageH, this);

    if (tabs.length != sketch.getCodeCount()) {
      tabs = new Tab[sketch.getCodeCount()];
      for (int i = 0; i < tabs.length; i++) {
        tabs[i] = new Tab(i);
      }
      visitOrder = new Tab[sketch.getCodeCount() - 1];
    }

    int leftover = TAB_BETWEEN + ARROW_TAB_WIDTH;
    int tabMax = getWidth() - leftover;

    // reset all tab positions
    for (Tab tab : tabs) {
      SketchCode code = sketch.getCode(tab.index);
      tab.textVisible = true;
      tab.lastVisited = code.lastVisited();

      // hide extensions for .pde files
      boolean hide = editor.getMode().hideExtension(code.getExtension());
      tab.text = hide ? code.getPrettyName() : code.getFileName();

      // if modified, add the li'l glyph next to the name
//      if (code.isModified()) {
//        tab.text += " \u00A7";
//      }

      tab.textWidth = (int)
        font.getStringBounds(tab.text, g2.getFontRenderContext()).getWidth();
    }
    // try to make everything fit
    if (!placeTabs(Editor.LEFT_GUTTER, tabMax, null)) {
      // always show the tab with the sketch's name
      int index = 0;
      // stock the array backwards so the rightmost tabs are closed by default
      for (int i = tabs.length - 1; i > 0; --i) {
        visitOrder[index++] = tabs[i];
      }
      Arrays.sort(visitOrder);  // sort on when visited

      // Keep shrinking the tabs one-by-one until things fit properly
      for (int i = 0; i < visitOrder.length; i++) {
        tabs[visitOrder[i].index].textVisible = false;
        if (placeTabs(Editor.LEFT_GUTTER, tabMax, null)) {
          break;
        }
      }
    }

    // now actually draw the tabs
    if (!placeTabs(Editor.LEFT_GUTTER, tabMax - ARROW_TAB_WIDTH, g2)){
      // draw the dropdown menu target at the right of the window
      menuRight = tabMax;
      menuLeft = menuRight - ARROW_TAB_WIDTH;
    } else {
      // draw the dropdown menu target next to the tabs
      menuLeft = tabs[tabs.length - 1].right + TAB_BETWEEN;
      menuRight = menuLeft + ARROW_TAB_WIDTH;
    }

    // draw the two pixel line that extends left/right below the tabs
    g.setColor(tabColor[SELECTED]);
    // can't be done with lines, b/c retina leaves tiny hairlines
    g.fillRect(Editor.LEFT_GUTTER, TAB_BOTTOM,
               editor.getTextArea().getWidth() - Editor.LEFT_GUTTER,
               Toolkit.zoom(2));

    // draw the tab for the menu
    g.setColor(tabColor[UNSELECTED]);
    drawTab(g, menuLeft, menuRight, false, true);

    // draw the arrow on the menu tab
    g.setColor(arrowColor);
    GeneralPath trianglePath = new GeneralPath();
    float x1 = menuLeft + (ARROW_TAB_WIDTH - ARROW_WIDTH) / 2f;
    float x2 = menuLeft + (ARROW_TAB_WIDTH + ARROW_WIDTH) / 2f;
    trianglePath.moveTo(x1, ARROW_TOP);
    trianglePath.lineTo(x2, ARROW_TOP);
    trianglePath.lineTo((x1 + x2) / 2, ARROW_BOTTOM);
    trianglePath.closePath();
    g2.fill(trianglePath);

    screen.drawImage(offscreen, 0, 0, imageW, imageH, null);
  }


  private boolean placeTabs(int left, int right, Graphics2D g) {
    Sketch sketch = editor.getSketch();
    int x = left;

//    final int bottom = getHeight(); // - TAB_STRETCH;
//    final int top = bottom - TAB_HEIGHT;
//    GeneralPath path = null;

    for (int i = 0; i < sketch.getCodeCount(); i++) {
      SketchCode code = sketch.getCode(i);
      Tab tab = tabs[i];

//      int pieceCount = 2 + (tab.textWidth / PIECE_WIDTH);
//      if (tab.textVisible == false) {
//        pieceCount = 4;
//      }
//      int pieceWidth = pieceCount * PIECE_WIDTH;

      int state = (code == sketch.getCurrentCode()) ? SELECTED : UNSELECTED;
//      if (g != null) {
//        //g.drawImage(pieces[state][LEFT], x, 0, PIECE_WIDTH, PIECE_HEIGHT, null);
//        path = new GeneralPath();
//        path.moveTo(x, bottom);
//        path.lineTo(x, top + NOTCH);
//        path.lineTo(x + NOTCH, top);
//      }
      tab.left = x;
      x += TEXT_MARGIN;
//      x += PIECE_WIDTH;

//      int contentLeft = x;
//      for (int j = 0; j < pieceCount; j++) {
//        if (g != null) {
//          g.drawImage(pieces[state][MIDDLE], x, 0, PIECE_WIDTH, PIECE_HEIGHT, null);
//        }
//        x += PIECE_WIDTH;
//      }
//      if (g != null) {
      int drawWidth = tab.textVisible ? tab.textWidth : NO_TEXT_WIDTH;
      x += drawWidth + TEXT_MARGIN;
//        path.moveTo(x, top);
//      }
      tab.right = x;

      if (g != null && tab.right < right) {
        g.setColor(tabColor[state]);
        drawTab(g, tab.left, tab.right, i == 0, false);
//        path.lineTo(x - NOTCH, top);
//        path.lineTo(x, top + NOTCH);
//        path.lineTo(x, bottom);
//        path.closePath();
//        g.setColor(tabColor[state]);
//        g.fill(path);
//        // have to draw an extra outline to make things line up on retina
//        g.draw(path);
//        //g.drawImage(pieces[state][RIGHT], x, 0, PIECE_WIDTH, PIECE_HEIGHT, null);

        if (tab.textVisible) {
          int textLeft = tab.left + ((tab.right - tab.left) - tab.textWidth) / 2;
          g.setColor(textColor[state]);
//          int baseline = (int) Math.ceil((sizeH + fontAscent) / 2.0);
          //int baseline = bottom - (TAB_HEIGHT - fontAscent)/2;
          int tabHeight = TAB_BOTTOM - TAB_TOP;
          int baseline = TAB_TOP + (tabHeight + fontAscent) / 2;
          //g.drawString(sketch.code[i].name, textLeft, baseline);
          g.drawString(tab.text, textLeft, baseline);
//          g.drawLine(tab.left, baseline-fontAscent, tab.right, baseline-fontAscent);
//          g.drawLine(tab.left, baseline, tab.right, baseline);
        }

        if (code.isModified()) {
          g.setColor(modifiedColor);
          //g.drawLine(tab.left + NOTCH, top, tab.right - NOTCH, top);
          //g.drawLine(tab.left + (i == 0 ? CURVE_RADIUS : 0), TAB_TOP, tab.right-1, TAB_TOP);
          g.drawLine(tab.right, TAB_TOP, tab.right, TAB_BOTTOM);
        }
      }

//      if (g != null) {
//        g.drawImage(pieces[state][RIGHT], x, 0, PIECE_WIDTH, PIECE_HEIGHT, null);
//      }
//      x += PIECE_WIDTH - 1;  // overlap by 1 pixel
      x += TAB_BETWEEN;
    }

    // removed 150130
//    // Draw this last because of half-pixel overlaps on retina displays
//    if (g != null) {
//      g.setColor(tabColor[SELECTED]);
//      g.fillRect(0, bottom, getWidth(), TAB_STRETCH);
//    }

    return x <= right;
  }


  private void drawTab(Graphics g, int left, int right,
                       boolean leftNotch, boolean rightNotch) {
//    final int bottom = getHeight(); // - TAB_STRETCH;
//    final int top = bottom - TAB_HEIGHT;
//    g.fillRect(left, top, right - left, bottom - top);

    Graphics2D g2 = (Graphics2D) g;
    g2.fill(Toolkit.createRoundRect(left, TAB_TOP,
                                    right, TAB_BOTTOM,
                                    leftNotch ? CURVE_RADIUS : 0,
                                    rightNotch ? CURVE_RADIUS : 0,
                                    0, 0));

//    path.moveTo(left, TAB_BOTTOM);
//    if (left == MARGIN_WIDTH) {  // first tab on the left
//      path.lineTo(left, TAB_TOP - CURVE_RADIUS);
//    }

  }


  /**
   * Called when a new sketch is opened.
   */
  public void rebuild() {
    //System.out.println("rebuilding editor header");
    rebuildMenu();
    repaint();
  }


  public void rebuildMenu() {
    //System.out.println("rebuilding");
    if (menu != null) {
      menu.removeAll();

    } else {
      menu = new JMenu();
      popup = menu.getPopupMenu();
      add(popup);
      popup.setLightWeightPopupEnabled(true);

      /*
      popup.addPopupMenuListener(new PopupMenuListener() {
          public void popupMenuCanceled(PopupMenuEvent e) {
            // on redraw, the isVisible() will get checked.
            // actually, a repaint may be fired anyway, so this
            // may be redundant.
            repaint();
          }
          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) { }
        });
      */
    }
    JMenuItem item;
    final JRootPane rootPane = editor.getRootPane();
    InputMap inputMap =
      rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    ActionMap actionMap = rootPane.getActionMap();

    Action action;
    String mapKey;
    KeyStroke keyStroke;

    item = Toolkit.newJMenuItemShift(Language.text("editor.header.new_tab"), KeyEvent.VK_N);
    action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editor.getSketch().handleNewCode();
      }
    };
    mapKey = "editor.header.new_tab";
    keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.SHORTCUT_SHIFT_KEY_MASK);
    inputMap.put(keyStroke, mapKey);
    actionMap.put(mapKey, action);
    item.addActionListener(action);
    menu.add(item);

    item = new JMenuItem(Language.text("editor.header.rename"));
    action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editor.getSketch().handleRenameCode();
      }
    };
    item.addActionListener(action);
    menu.add(item);

    item = Toolkit.newJMenuItemShift(Language.text("editor.header.delete"), KeyEvent.VK_D);
    action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Sketch sketch = editor.getSketch();
        if (!Platform.isMacOS() &&  // ok on OS X
            editor.base.getEditors().size() == 1 &&  // mmm! accessor
            sketch.getCurrentCodeIndex() == 0) {
            Messages.showWarning(Language.text("editor.header.delete.warning.title"),
                                 Language.text("editor.header.delete.warning.text"));
        } else {
          editor.getSketch().handleDeleteCode();
        }
      }
    };
    mapKey = "editor.header.delete";
    keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.SHORTCUT_SHIFT_KEY_MASK);
    inputMap.put(keyStroke, mapKey);
    actionMap.put(mapKey, action);
    item.addActionListener(action);
    menu.add(item);

    menu.addSeparator();

    //  KeyEvent.VK_LEFT and VK_RIGHT will make Windows beep

    mapKey = "editor.header.previous_tab";
    item = Toolkit.newJMenuItemExt(mapKey);
    action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editor.getSketch().handlePrevCode();
      }
    };
    keyStroke = item.getAccelerator();
    inputMap.put(keyStroke, mapKey);
    actionMap.put(mapKey, action);
    item.addActionListener(action);
    menu.add(item);

    mapKey = "editor.header.next_tab";
    item = Toolkit.newJMenuItemExt(mapKey);
    action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editor.getSketch().handleNextCode();
      }
    };
    keyStroke = item.getAccelerator();
    inputMap.put(keyStroke, mapKey);
    actionMap.put(mapKey, action);
    item.addActionListener(action);
    menu.add(item);

    Sketch sketch = editor.getSketch();
    if (sketch != null) {
      menu.addSeparator();

      ActionListener jumpListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.getSketch().setCurrentCode(e.getActionCommand());
        }
      };
      for (SketchCode code : sketch.getCode()) {
        item = new JMenuItem(code.getPrettyName());
        item.addActionListener(jumpListener);
        menu.add(item);
      }
    }

    Toolkit.setMenuMnemonics(menu);
  }


  public void deselectMenu() {
    repaint();
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


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static class Tab implements Comparable {
    int index;
    int left;
    int right;
    String text;
    int textWidth;
    boolean textVisible;
    long lastVisited;

    Tab(int index) {
      this.index = index;
    }

    boolean contains(int x) {
      return x >= left && x <= right;
    }

    // sort by the last time visited
    public int compareTo(Object o) {
      Tab other = (Tab) o;
      // do this here to deal with situation where both are 0
      if (lastVisited == other.lastVisited) {
        return 0;
      }
      if (lastVisited == 0) {
        return -1;
      }
      if (other.lastVisited == 0) {
        return 1;
      }
      return (int) (lastVisited - other.lastVisited);
    }
  }
}
