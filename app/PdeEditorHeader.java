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


public class PdeEditorHeader extends JComponent /*implements MouseListener*/ {
  //static final String SKETCH_TITLER = "sketch";

  //static Color primaryColor;
  static Color backgroundColor;
  static Color textColor[] = new Color[2];
  //static Color unselectedColor;

  PdeEditor editor;
  //PdeSketch sketch;

  int tabLeft[];
  int tabRight[];

  //int sketchLeft;
  //int sketchRight;
  //int sketchTitleLeft;
  //boolean sketchModified;

  Font font;
  FontMetrics metrics;
  int fontAscent;

  //PdeSketch sketch;

  //

  JMenu menu;

  //boolean menuVisible;
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

      //primaryColor    = PdePreferences.getColor("header.fgcolor.primary");
      //secondaryColor  = PdePreferences.getColor("header.fgcolor.secondary");
    }

    /*
    addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          //System.out.println("got mouse");
          if ((sketchRight != 0) &&
              (e.getX() > sketchLeft) && (e.getX() < sketchRight)) {
            editor.skSaveAs(true);
          }
        }
      });
    */

    //addMouseListener(this);
    //addMouseMotionListener(this);

    addMouseListener(new MouseAdapter() { 
        public void mousePressed(MouseEvent e) {
          int x = e.getX();
          int y = e.getY();

          if ((x > menuLeft) && (x < menuRight)) {
            popup.show(PdeEditorHeader.this, x, y);

          } else {
            //for (int i = 0; i < sketch.codeCount; i++) {
            for (int i = 0; i < editor.sketch.codeCount; i++) {
              if ((x > tabLeft[i]) && (x < tabRight[i])) {
                //setCurrent(i);
                editor.sketch.setCurrent(i);
                repaint();
              }
            }
          }
        }
      });
  }


  /*
  public void reset() {
    sketchLeft = 0;
    repaint();
  }
  */


  public void paintComponent(Graphics screen) {
    if (screen == null) return;
    //if (editor.sketchName == null) return;

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
      g.setFont(font);
      metrics = g.getFontMetrics();
      fontAscent = metrics.getAscent();
    }

    // set the background for the offscreen
    g.setColor(backgroundColor);
    //System.out.println("bg = " + backgroundColor);
    //g.setColor(Color.red);
    g.fillRect(0, 0, imageW, imageH);

    if ((tabLeft == null) ||
        (tabLeft.length < sketch.codeCount)) {
      tabLeft = new int[sketch.codeCount];
      tabRight = new int[sketch.codeCount];
    }

    int x = PdePreferences.GUI_SMALL;
    for (int i = 0; i < sketch.codeCount; i++) {
      PdeCode code = sketch.code[i];

      // if modified, add the li'l glyph next to the name
      String text = "  " + code.name + (code.modified ? " \u00A7" : "  ");
      //System.out.println("code " + i + " " + text);

      int textWidth = metrics.stringWidth(text);
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

    /*
    sketchTitleLeft = PdePreferences.GUI_SMALL;
    sketchLeft = sketchTitleLeft + 
      metrics.stringWidth(SKETCH_TITLER) + PdePreferences.GUI_SMALL;
    sketchRight = sketchLeft + metrics.stringWidth(editor.sketchName);
    int modifiedLeft = sketchRight + PdePreferences.GUI_SMALL;

    int baseline = (sizeH + fontAscent) / 2;

    g.setColor(backgroundColor);
    g.fillRect(0, 0, imageW, imageH);

    g.setFont(font); // needs to be set each time
    g.setColor(secondaryColor);
    g.drawString(SKETCH_TITLER, sketchTitleLeft, baseline);
    if (sketch.getModified()) g.drawString("\u00A7", modifiedLeft, baseline);

    g.drawString(editor.sketchName, sketchLeft, baseline);
    */

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


  /*
    New [code,tab,source] file...  Cmd-shift-N
    Rename
    Delete
    Hide
    Unhide  >
    Reset file list (not needed?)
  */
  //public JMenu rebuildMenu() {
  //JMenuItem newItem;

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

    item = PdeEditor.newJMenuItem("New", 'T');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
          //System.out.println("TODO write code for New");
          editor.sketch.newCode();
        }
      });
    menu.add(item);

    /*
    if (newItem == null) {
      newItem = PdeEditor.newJMenuItem("New", 'T');
      newItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { 
            //System.out.println("TODO write code for New");
            editor.sketch.newCode();
          }
        });
    }
    System.out.println("adding new");
    menu.add(newItem);
    */

    item = new JMenuItem("Rename");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
          editor.sketch.renameCode();
        }
      });
    menu.add(item);

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
        //item.setActionCommand(hiddenFiles[i]);
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
            //System.out.println("jump to " + e.getActionCommand());
            //System.out.println("jump to " + e);
            editor.sketch.setCurrent(e.getActionCommand());
          }
        };
      for (int i = 0; i < sketch.codeCount; i++) {
        item = new JMenuItem(sketch.code[i].name);
        //item.setActionCommand(files[i]);
        item.addActionListener(jumpListener);
        menu.add(item);
      }
    }
  }


  public void deselectMenu() {
    //menuVisible = false;   // ??
    repaint();
  }


  /*
  public void setCurrent(int which) {
    current = which;

    editor.sketch.setCurrent(which);

    // set to the text for this file, and wipe out the undo buffer
    editor.changeText(contents, true); 
  }
  */


  public Dimension getPreferredSize() {
    return getMinimumSize();
  }

  public Dimension getMinimumSize() {
    return new Dimension(300, PdePreferences.GRID_SIZE);
  }

  public Dimension getMaximumSize() {
    return new Dimension(3000, PdePreferences.GRID_SIZE);
  }
}
