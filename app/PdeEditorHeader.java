/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeEditorHeader - sketch tabs at the top of the screen
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry and
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

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;


public class PdeEditorHeader extends JComponent {
  static Color backgroundColor;
  static Color textColor[] = new Color[2];

  PdeEditor editor;

  int tabLeft[];
  int tabRight[];

  Font font;
  FontMetrics metrics;
  int fontAscent;

  JMenu menu;
  JPopupMenu popup;

  JMenuItem renameItem;

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


  public PdeEditorHeader(PdeEditor eddie) {
    this.editor = eddie; // weird name for listener

    pieces = new Image[STATUS.length][WHERE.length];
    for (int i = 0; i < STATUS.length; i++) {
      for (int j = 0; j < WHERE.length; j++) {
        pieces[i][j] = PdeBase.getImage("tab-" + STATUS[i] + "-" +
                                        WHERE[j] + ".gif", this);
      }
    }

    if (backgroundColor == null) {
      backgroundColor =
        PdePreferences.getColor("header.bgcolor");
      textColor[SELECTED] =
        PdePreferences.getColor("header.text.selected.color");
      textColor[UNSELECTED] =
        PdePreferences.getColor("header.text.unselected.color");
    }

    addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          int x = e.getX();
          int y = e.getY();

          if ((x > menuLeft) && (x < menuRight)) {
            popup.show(PdeEditorHeader.this, x, y);

          } else {
            for (int i = 0; i < editor.sketch.codeCount; i++) {
              if ((x > tabLeft[i]) && (x < tabRight[i])) {
                editor.sketch.setCurrent(i);
                repaint();
              }
            }
          }
        }
      });
  }


  public void paintComponent(Graphics screen) {
    if (screen == null) return;

    PdeSketch sketch = editor.sketch;
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
        //userLeft = 0; // reset
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
    if (font == null) {
      font = PdePreferences.getFont("header.text.font");
    }
    g.setFont(font);  // need to set this each time through
    metrics = g.getFontMetrics();
    fontAscent = metrics.getAscent();
    //}

    //Graphics2D g2 = (Graphics2D) g;
    //g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
    //                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // set the background for the offscreen
    g.setColor(backgroundColor);
    g.fillRect(0, 0, imageW, imageH);

    if ((tabLeft == null) ||
        (tabLeft.length < sketch.codeCount)) {
      tabLeft = new int[sketch.codeCount];
      tabRight = new int[sketch.codeCount];
    }

    // disable rename on the first tab
    renameItem.setEnabled(sketch.current != sketch.code[0]);

    int x = PdePreferences.GUI_SMALL;
    for (int i = 0; i < sketch.codeCount; i++) {
      PdeCode code = sketch.code[i];

      String codeName = (code.flavor == PdeSketch.PDE) ?
        code.name : code.file.getName();

      // if modified, add the li'l glyph next to the name
      String text = "  " + codeName + (code.modified ? " \u00A7" : "  ");

      //int textWidth = metrics.stringWidth(text);
      Graphics2D g2 = (Graphics2D) g;
      int textWidth = (int)
        font.getStringBounds(text, g2.getFontRenderContext()).getWidth();

      int pieceCount = 2 + (textWidth / PIECE_WIDTH);
      int pieceWidth = pieceCount * PIECE_WIDTH;

      int state = (code == sketch.current) ? SELECTED : UNSELECTED;
      g.drawImage(pieces[state][LEFT], x, 0, null);
      x += PIECE_WIDTH;

      int contentLeft = x;
      tabLeft[i] = x;
      for (int j = 0; j < pieceCount; j++) {
        g.drawImage(pieces[state][MIDDLE], x, 0, null);
        x += PIECE_WIDTH;
      }
      tabRight[i] = x;
      int textLeft = contentLeft + (pieceWidth - textWidth) / 2;

      g.setColor(textColor[state]);
      int baseline = (sizeH + fontAscent) / 2;
      //g.drawString(sketch.code[i].name, textLeft, baseline);
      g.drawString(text, textLeft, baseline);

      g.drawImage(pieces[state][RIGHT], x, 0, null);
      x += PIECE_WIDTH - 1;  // overlap by 1 pixel
    }

    menuLeft = sizeW - (16 + pieces[0][MENU].getWidth(this));
    menuRight = sizeW - 16;
    // draw the dropdown menu target
    g.drawImage(pieces[popup.isVisible() ? SELECTED : UNSELECTED][MENU],
                menuLeft, 0, null);

    screen.drawImage(offscreen, 0, 0, null);
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
    if (menu != null) {
      menu.removeAll();

    } else {
      menu = new JMenu();
      popup = menu.getPopupMenu();
      add(popup);
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
    }
    JMenuItem item;

    // maybe this shouldn't have a command key anyways..
    // since we're not trying to make this a full ide..
    //item = PdeEditor.newJMenuItem("New", 'T');

    /*
    item = PdeEditor.newJMenuItem("Previous", KeyEvent.VK_PAGE_UP);
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.out.println("prev");
        }
      });
    if (editor.sketch != null) {
      item.setEnabled(editor.sketch.codeCount > 1);
    }
    menu.add(item);

    item = PdeEditor.newJMenuItem("Next", KeyEvent.VK_PAGE_DOWN);
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

    item = new JMenuItem("New Tab");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.sketch.newCode();
        }
      });
    menu.add(item);

    item = new JMenuItem("Rename");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.sketch.renameCode();
        }
      });
    menu.add(item);
    renameItem = item;

    item = new JMenuItem("Delete");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.sketch.deleteCode();
        }
      });
    menu.add(item);

    item = new JMenuItem("Hide");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.sketch.hideCode();
        }
      });
    menu.add(item);

    JMenu unhide = new JMenu("Unhide");
    ActionListener unhideListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          editor.sketch.unhideCode((String) (e.getActionCommand()));
          rebuildMenu();
        }
      };
    PdeSketch sketch = editor.sketch;
    if (sketch != null) {
      for (int i = 0; i < sketch.hiddenCount; i++) {
        item = new JMenuItem(sketch.hidden[i].name);
        item.setActionCommand(sketch.hidden[i].name);
        item.addActionListener(unhideListener);
        unhide.add(item);
      }
    }
    if (unhide.getItemCount() == 0) {
      unhide.setEnabled(false);
    }

    menu.add(unhide);

    if (sketch != null) {
      menu.addSeparator();

      ActionListener jumpListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            editor.sketch.setCurrent(e.getActionCommand());
          }
        };
      for (int i = 0; i < sketch.codeCount; i++) {
        item = new JMenuItem(sketch.code[i].name);
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
    return new Dimension(300, PdePreferences.GRID_SIZE - 1);
  }

  public Dimension getMaximumSize() {
    return new Dimension(3000, PdePreferences.GRID_SIZE - 1);
  }
}
