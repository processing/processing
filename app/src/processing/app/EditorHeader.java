/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-11 Ben Fry and Casey Reas
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

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

import javax.swing.*;


/**
 * Sketch tabs at the top of the editor window.
 */
public class EditorHeader extends JComponent {
  Color backgroundColor;
  Color textColor[] = new Color[2];

  Editor editor;

  Tab[] tabs = new Tab[0];
  Tab[] visitOrder;

  Font font;
  FontMetrics metrics;
  int fontAscent;

  JMenu menu;
  JPopupMenu popup;

  int menuLeft;
  int menuRight;

  //

  static final String STATUS[] = { "unsel", "sel" };
  static final int UNSELECTED = 0;
  static final int SELECTED = 1;

  static final String WHERE[] = { "left", "mid", "right", "menu" };
  static final int LEFT = 0;
  static final int MIDDLE = 1;
  static final int RIGHT = 2;
  static final int MENU = 3;

  static final int PIECE_WIDTH = 4;

  Image[][] pieces;

  //

  Image offscreen;
  int sizeW, sizeH;
  int imageW, imageH;

  String lastNoticeName;


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
//            for (int i = 0; i < sketch.getCodeCount(); i++) {
//              if ((x > tabLeft[i]) && (x < tabRight[i])) {
//                sketch.setCurrentCode(i);
//                repaint();
//              }
//            }
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
    pieces = new Image[STATUS.length][WHERE.length];
    for (int i = 0; i < STATUS.length; i++) {
      for (int j = 0; j < WHERE.length; j++) {
        String filename = "theme/tab-" + STATUS[i] + "-" + WHERE[j] + ".gif";
        pieces[i][j] = mode.loadImage(filename);
      }
    }

    backgroundColor = mode.getColor("header.bgcolor");
    textColor[SELECTED] = mode.getColor("header.text.selected.color");
    textColor[UNSELECTED] = mode.getColor("header.text.unselected.color");
    font = mode.getFont("header.text.font");
  }


  public void paintComponent(Graphics screen) {
    if (screen == null) return;

    Sketch sketch = editor.getSketch();
    if (sketch == null) return;  // ??

    Dimension size = getSize();
    if ((size.width != sizeW) || (size.height != sizeH)) {
      // component has been resized

      if ((size.width > imageW) || (size.height > imageH)) {
        // nix the image and recreate, it's too small
        offscreen = null;

      } else {
        // who cares, just resize
        sizeW = size.width;
        sizeH = size.height;
      }
    }

    if (offscreen == null) {
      sizeW = size.width;
      sizeH = size.height;
      imageW = sizeW;
      imageH = sizeH;
      offscreen = createImage(imageW, imageH);
    }

    Graphics g = offscreen.getGraphics();
    g.setFont(font);  // need to set this each time through
    metrics = g.getFontMetrics();
    fontAscent = metrics.getAscent();

    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // set the background for the offscreen
    g.setColor(backgroundColor);
    g.fillRect(0, 0, imageW, imageH);

//    int codeCount = sketch.getCodeCount();
//    if ((tabLeft == null) || (tabLeft.length < codeCount)) {
//      tabLeft = new int[codeCount];
//      tabRight = new int[codeCount];
//    }
    if (tabs.length != sketch.getCodeCount()) {
      tabs = new Tab[sketch.getCodeCount()];
      for (int i = 0; i < tabs.length; i++) {
        tabs[i] = new Tab(i);
      }
      visitOrder = new Tab[sketch.getCodeCount() - 1];
    }

//    int x = 6; // offset from left edge of the component
    menuRight = sizeW - 16;
    menuLeft = menuRight - pieces[0][MENU].getWidth(this);
//    int tabMax = menuLeft - x;
    int tabLeft = 6;
    int tabMax = menuLeft - tabLeft;

    // reset all tab positions
    for (Tab tab : tabs) {
      SketchCode code = sketch.getCode(tab.index);
      tab.textVisible = true;
      tab.lastVisited = code.lastVisited();

      // hide extensions for .pde files (or whatever else is the norm elsewhere
      boolean hide = editor.getMode().hideExtension(code.getExtension());
      String codeName = hide ? code.getPrettyName() : code.getFileName();
      // if modified, add the li'l glyph next to the name
      tab.text = "  " + codeName + (code.isModified() ? " \u00A7" : "  ");

      tab.textWidth = (int)
        font.getStringBounds(tab.text, g2.getFontRenderContext()).getWidth();
    }

    // make sure everything can fit
    if (!placeTabs(tabLeft, tabMax, null)) {
      //System.arraycopy(tabs, 0, visitOrder, 0, tabs.length);
      // always show the tab with the sketch's name
//      System.arraycopy(tabs, 1, visitOrder, 0, tabs.length - 1);
      int index = 0;
      // stock the array backwards so that the rightmost tabs are closed by default
      for (int i = tabs.length - 1; i > 0; --i) {
        visitOrder[index++] = tabs[i];
      }
      Arrays.sort(visitOrder);  // sort on when visited
//      for (int i = 0; i < visitOrder.length; i++) {
//        System.out.println(visitOrder[i].index + " " + visitOrder[i].text);
//      }
//      System.out.println();

      for (int i = 0; i < visitOrder.length; i++) {
        tabs[visitOrder[i].index].textVisible = false;
        if (placeTabs(tabLeft, tabMax, null)) {
          break;
        }
      }
    }

    // now actually draw the tabs
    placeTabs(tabLeft, tabMax, g);

//    for (int i = 0; i < sketch.getCodeCount(); i++) {
//      SketchCode code = sketch.getCode(i);
//      Tab tab = tabs[i];
//
//      int pieceCount = 2 + (tab.textWidth / PIECE_WIDTH);
//      int pieceWidth = pieceCount * PIECE_WIDTH;
//
//      int state = (code == sketch.getCurrentCode()) ? SELECTED : UNSELECTED;
//      g.drawImage(pieces[state][LEFT], x, 0, null);
//      x += PIECE_WIDTH;
//
//      int contentLeft = x;
//      tab.left = x;
//      for (int j = 0; j < pieceCount; j++) {
//        g.drawImage(pieces[state][MIDDLE], x, 0, null);
//        x += PIECE_WIDTH;
//      }
//      tab.right = x;
//      int textLeft = contentLeft + (pieceWidth - tab.textWidth) / 2;
//
//      g.setColor(textColor[state]);
//      int baseline = (sizeH + fontAscent) / 2;
//      //g.drawString(sketch.code[i].name, textLeft, baseline);
//      g.drawString(tab.text, textLeft, baseline);
//
//      g.drawImage(pieces[state][RIGHT], x, 0, null);
//      x += PIECE_WIDTH - 1;  // overlap by 1 pixel
//    }

//    menuLeft = sizeW - (16 + pieces[0][MENU].getWidth(this));
//    menuRight = sizeW - 16;
    // draw the dropdown menu target
    g.drawImage(pieces[popup.isVisible() ? SELECTED : UNSELECTED][MENU],
                menuLeft, 0, null);

    screen.drawImage(offscreen, 0, 0, null);
  }


  private boolean placeTabs(int left, int right, Graphics g) {
    Sketch sketch = editor.getSketch();
    int x = left;

    for (int i = 0; i < sketch.getCodeCount(); i++) {
      SketchCode code = sketch.getCode(i);
      Tab tab = tabs[i];

      int pieceCount = 2 + (tab.textWidth / PIECE_WIDTH);
      if (tab.textVisible == false) {
        pieceCount = 4;
      }
      int pieceWidth = pieceCount * PIECE_WIDTH;

      int state = (code == sketch.getCurrentCode()) ? SELECTED : UNSELECTED;
      if (g != null) {
        g.drawImage(pieces[state][LEFT], x, 0, null);
      }
      x += PIECE_WIDTH;

      int contentLeft = x;
      tab.left = x;
      for (int j = 0; j < pieceCount; j++) {
        if (g != null) {
          g.drawImage(pieces[state][MIDDLE], x, 0, null);
        }
        x += PIECE_WIDTH;
      }
      tab.right = x;

      if (tab.textVisible) {
        int textLeft = contentLeft + (pieceWidth - tab.textWidth) / 2;
        if (g != null) {
          g.setColor(textColor[state]);
          int baseline = (sizeH + fontAscent) / 2;
          //g.drawString(sketch.code[i].name, textLeft, baseline);
          g.drawString(tab.text, textLeft, baseline);
        }
      }

      if (g != null) {
        g.drawImage(pieces[state][RIGHT], x, 0, null);
      }
      x += PIECE_WIDTH - 1;  // overlap by 1 pixel
    }
    return x <= right;
  }


  /**
   * Called when a new sketch is opened.
   */
  public void rebuild() {
    //System.out.println("rebuilding editor header");
    rebuildMenu();
    repaint();
    Toolkit.getDefaultToolkit().sync();
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

    // maybe this shouldn't have a command key anyways..
    // since we're not trying to make this a full ide..
    //item = Editor.newJMenuItem("New", 'T');

    /*
    item = Editor.newJMenuItem("Previous", KeyEvent.VK_PAGE_UP);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.out.println("prev");
        }
      });
    if (editor.sketch != null) {
      item.setEnabled(editor.sketch.codeCount > 1);
    }
    menu.add(item);

    item = Editor.newJMenuItem("Next", KeyEvent.VK_PAGE_DOWN);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.out.println("ext");
        }
      });
    if (editor.sketch != null) {
      item.setEnabled(editor.sketch.codeCount > 1);
    }
    menu.add(item);

    menu.addSeparator();
    */

    //item = new JMenuItem("New Tab");
    item = Base.newJMenuItemShift("New Tab", 'N');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.getSketch().handleNewCode();
        }
      });
    menu.add(item);

    item = new JMenuItem("Rename");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.getSketch().handleRenameCode();
          /*
          // this is already being called by nameCode(), the second stage of rename
          if (editor.sketch.current == editor.sketch.code[0]) {
            editor.sketchbook.rebuildMenus();
          }
          */
        }
      });
    menu.add(item);

    item = new JMenuItem("Delete");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Sketch sketch = editor.getSketch();
          if (!sketch.isUntitled()) {  // don't bother if untitled
            if (!Base.isMacOS() &&  // ok on OS X
                editor.base.editors.size() == 1 &&  // mmm! accessor
                sketch.getCurrentCodeIndex() == 0) {
              Base.showWarning("Yeah, no." ,
                               "You can't delete the last tab " +
                               "of the last open sketch.", null);
            } else {
              editor.getSketch().handleDeleteCode();
            }
          }
        }
      });
    menu.add(item);

    menu.addSeparator();

    //  KeyEvent.VK_LEFT and VK_RIGHT will make Windows beep

    item = new JMenuItem("Previous Tab");
    KeyStroke ctrlAltLeft =
      KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Base.SHORTCUT_ALT_KEY_MASK);
    item.setAccelerator(ctrlAltLeft);
    // this didn't want to work consistently
    /*
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.sketch.prevCode();
        }
      });
    */
    menu.add(item);

    item = new JMenuItem("Next Tab");
    KeyStroke ctrlAltRight =
      KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Base.SHORTCUT_ALT_KEY_MASK);
    item.setAccelerator(ctrlAltRight);
    /*
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.sketch.nextCode();
        }
      });
    */
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
  }


  public void deselectMenu() {
    repaint();
  }


  public Dimension getPreferredSize() {
    return getMinimumSize();
  }


  public Dimension getMinimumSize() {
    if (Base.isMacOS()) {
      return new Dimension(300, Preferences.GRID_SIZE);
    }
    return new Dimension(300, Preferences.GRID_SIZE - 1);
  }


  public Dimension getMaximumSize() {
    if (Base.isMacOS()) {
      return new Dimension(3000, Preferences.GRID_SIZE);
    }
    return new Dimension(3000, Preferences.GRID_SIZE - 1);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class Tab implements Comparable {
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
