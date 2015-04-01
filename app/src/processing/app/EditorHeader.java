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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.GeneralPath;
import java.util.Arrays;

import javax.swing.*;


/**
 * Sketch tabs at the top of the editor window.
 */
public class EditorHeader extends JComponent {
  // height of this tab bar
  static final int HIGH = 30;
  // standard UI sizing (OS-specific, but generally consistent)
//  static final int SCROLLBAR_WIDTH = 16;
  // amount of space on the left edge before the tabs start
  static final int MARGIN_WIDTH = Editor.LEFT_GUTTER;
  
  static final int ARROW_TAB_WIDTH = 23;
  // distance from the righthand side of a tab to the drop-down arrow
//  static final int ARROW_GAP_WIDTH = 8;
  // indent x/y for notch on the tab
  static final int ARROW_TOP = 14;
  static final int ARROW_BOTTOM = 23;
  static final int ARROW_WIDTH = 9;
  
//  static final int NOTCH = 0;
  // how far to raise the tab from the bottom of this Component
  static final int TAB_HEIGHT = HIGH;
//  // line that continues across all of the tabs for the current one
//  static final int TAB_STRETCH = 3;
  // amount of extra space between individual tabs
  static final int TAB_BETWEEN = 4;
  // amount of margin on the left/right for the text on the tab
  static final int TEXT_MARGIN = 16;
  // width of the tab when no text visible
  // (total tab width will be this plus TEXT_MARGIN*2)
  static final int NO_TEXT_WIDTH = 16;

  Color bgColor;
//  boolean hiding;
  Color hideColor;
  
  Color textColor[] = new Color[2];
  Color tabColor[] = new Color[2];
  Color modifiedColor;
  Color arrowColor;

  Editor editor;

  Tab[] tabs = new Tab[0];
  Tab[] visitOrder;

  Font font;
//  FontMetrics metrics;
  int fontAscent;

  JMenu menu;
  JPopupMenu popup;

  int menuLeft;
  int menuRight;

  //

//  static final String STATUS[] = { "unsel", "sel" };
  static final int UNSELECTED = 0;
  static final int SELECTED = 1;

//  static final String WHERE[] = { "left", "mid", "right" }; //, "menu" };
//  static final int LEFT = 0;
//  static final int MIDDLE = 1;
//  static final int RIGHT = 2;
//  static final int MENU = 3;

//  static final int PIECE_WIDTH = 4;
//  static final int PIECE_HEIGHT = 33;
//  Image[][] pieces;

//  static final int ARROW_WIDTH = 14;
//  static final int ARROW_HEIGHT = 14;
//  static Image tabArrow;

  //

  Image offscreen;
  int sizeW, sizeH;
  int imageW, imageH;

  String lastNoticeName;
  
  Image gradient;


  public EditorHeader(Editor eddie) {
    this.editor = eddie;

    gradient = editor.getMode().getGradient("header", 400, HIGH);

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


//  protected String tabFile(int status, int where) {
//    return "theme/tab-" + STATUS[status] + "-" + WHERE[where];
//  }


  public void updateMode() {
    Mode mode = editor.getMode();
//    if (tabArrow == null) {
//      String suffix = Toolkit.highResDisplay() ? "-2x.png" : ".png";
//      tabArrow = Toolkit.getLibImage("tab-arrow" + suffix);
//    }

    bgColor = mode.getColor("header.bgcolor");
    
    textColor[SELECTED] = mode.getColor("header.text.selected.color");
    textColor[UNSELECTED] = mode.getColor("header.text.unselected.color");
    font = mode.getFont("header.text.font");

    tabColor[SELECTED] = mode.getColor("header.tab.selected.color");
    tabColor[UNSELECTED] = mode.getColor("header.tab.unselected.color");
    
    arrowColor = mode.getColor("header.tab.arrow.color");
    modifiedColor = mode.getColor("editor.selection.color");
  }


  public void paintComponent(Graphics screen) {
    setOpaque(false);
    
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

    if (tabs.length != sketch.getCodeCount()) {
      tabs = new Tab[sketch.getCodeCount()];
      for (int i = 0; i < tabs.length; i++) {
        tabs[i] = new Tab(i);
      }
      visitOrder = new Tab[sketch.getCodeCount() - 1];
    }

    int leftover = TAB_BETWEEN + ARROW_TAB_WIDTH; // + MARGIN_WIDTH; // + SCROLLBAR_WIDTH;
    int tabMax = getWidth() - leftover;

    // reset all tab positions
    for (Tab tab : tabs) {
      SketchCode code = sketch.getCode(tab.index);
      tab.textVisible = true;
      tab.lastVisited = code.lastVisited();

      // hide extensions for .pde files (or whatever else is the norm elsewhere
      boolean hide = editor.getMode().hideExtension(code.getExtension());
//      String codeName = hide ? code.getPrettyName() : code.getFileName();
      // if modified, add the li'l glyph next to the name
//      tab.text = "  " + codeName + (code.isModified() ? " \u00A7" : "  ");
//      tab.text = "  " + codeName + "  ";
      tab.text = hide ? code.getPrettyName() : code.getFileName();
      
      tab.textWidth = (int)
        font.getStringBounds(tab.text, g2.getFontRenderContext()).getWidth();
    }

    // make sure everything can fit
    if (!placeTabs(MARGIN_WIDTH, tabMax, null)) {
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

      // Keep shrinking the tabs one-by-one until things fit properly
      for (int i = 0; i < visitOrder.length; i++) {
        tabs[visitOrder[i].index].textVisible = false;
        if (placeTabs(MARGIN_WIDTH, tabMax, null)) {
          break;
        }
      }
    }

    // now actually draw the tabs
    placeTabs(MARGIN_WIDTH, tabMax, g2);

    // draw the dropdown menu target
    menuLeft = tabs[tabs.length - 1].right + TAB_BETWEEN;
    menuRight = menuLeft + ARROW_TAB_WIDTH;
    g.setColor(tabColor[UNSELECTED]);
    drawTab(g, menuLeft, menuRight);
//    int arrowY = (getHeight() - TAB_HEIGHT - TAB_STRETCH) + (TAB_HEIGHT - ARROW_HEIGHT)/2;
//    g.drawImage(tabArrow, menuLeft, arrowY,
//                ARROW_WIDTH, ARROW_HEIGHT, null);
    // TODO draw arrow here

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

    final int bottom = getHeight(); // - TAB_STRETCH;
    final int top = bottom - TAB_HEIGHT;
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

      if (g != null) {
        g.setColor(tabColor[state]);
        drawTab(g, tab.left, tab.right);
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
          int tabHeight = TAB_HEIGHT; //bottom - top;
          int baseline = top + (tabHeight + fontAscent) / 2;
          //g.drawString(sketch.code[i].name, textLeft, baseline);
          g.drawString(tab.text, textLeft, baseline);
//          g.drawLine(tab.left, baseline-fontAscent, tab.right, baseline-fontAscent);
//          g.drawLine(tab.left, baseline, tab.right, baseline);
        }
      
        if (code.isModified()) {
          g.setColor(modifiedColor);
          //g.drawLine(tab.left + NOTCH, top, tab.right - NOTCH, top);
          g.drawLine(tab.left, top, tab.right, top);
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
  
  
  private void drawTab(Graphics g, int left, int right) {
    final int bottom = getHeight(); // - TAB_STRETCH;
    final int top = bottom - TAB_HEIGHT;
    g.fillRect(left, top, right - left, bottom - top);
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
    InputMap mInputMap = editor.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    ActionMap mActionMap = editor.getRootPane().getActionMap();
    Action action;
    String mapKey;
    KeyStroke keyStroke;

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
    item = Toolkit.newJMenuItemShift(Language.text("editor.header.new_tab"), KeyEvent.VK_N);
    action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editor.getSketch().handleNewCode();
      }
    };
    mapKey = "editor.header.new_tab";
    keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.SHORTCUT_SHIFT_KEY_MASK);
    mInputMap.put(keyStroke, mapKey);
    mActionMap.put(mapKey, action);
    item.addActionListener(action);
    menu.add(item);

    item = new JMenuItem(Language.text("editor.header.rename"));
    action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editor.getSketch().handleRenameCode();
        /*
        // this is already being called by nameCode(), the second stage of rename
        if (editor.sketch.current == editor.sketch.code[0]) {
          editor.sketchbook.rebuildMenus();
        }
        */
      }
    };
    item.addActionListener(action);
    menu.add(item);

    item = Toolkit.newJMenuItemShift(Language.text("editor.header.delete"), KeyEvent.VK_D);
    action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        Sketch sketch = editor.getSketch();
        if (!Base.isMacOS() &&  // ok on OS X
            editor.base.editors.size() == 1 &&  // mmm! accessor
            sketch.getCurrentCodeIndex() == 0) {
            Base.showWarning(Language.text("editor.header.delete.warning.title"),
                             Language.text("editor.header.delete.warning.text"), null);
        } else {
          editor.getSketch().handleDeleteCode();
        }
      }
    };
    mapKey = "editor.header.delete";
    keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.SHORTCUT_ALT_KEY_MASK);
    mInputMap.put(keyStroke, mapKey);
    mActionMap.put(mapKey, action);
    item.addActionListener(action);
    menu.add(item);

    menu.addSeparator();

    //  KeyEvent.VK_LEFT and VK_RIGHT will make Windows beep
    item = Toolkit.newJMenuItemAlt(Language.text("editor.header.previous_tab"), KeyEvent.VK_LEFT);
    action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
//        Sketch sketch = editor.getSketch();
//        sketch.setCurrentCode(sketch.getCurrentCodeIndex() - 1);
        editor.getSketch().handlePrevCode();
      }
    };
    mapKey = "editor.header.previous_tab";
    keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, Toolkit.SHORTCUT_ALT_KEY_MASK);
    mInputMap.put(keyStroke, mapKey);
    mActionMap.put(mapKey, action);
    item.addActionListener(action);
    menu.add(item);

    item = Toolkit.newJMenuItemAlt(Language.text("editor.header.next_tab"), KeyEvent.VK_RIGHT);
    action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
//        Sketch sketch = editor.getSketch();
//        sketch.setCurrentCode(sketch.getCurrentCodeIndex() + 1);
        editor.getSketch().handleNextCode();
      }
    };
    mapKey = "editor.header.next_tab";
    keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, Toolkit.SHORTCUT_ALT_KEY_MASK);
    mInputMap.put(keyStroke, mapKey);
    mActionMap.put(mapKey, action);
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
