/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeEditorHeader - sketch tabs at the top of the screen
  Part of the Processing project - http://Proce55ing.net

  Except where noted, code is written by Ben Fry and
  Copyright (c) 2001-03 Massachusetts Institute of Technology

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


public class PdeEditorHeader extends JComponent {
  //static final String SKETCH_TITLER = "sketch";

  //static Color primaryColor;
  static Color textColor[] = new Color[2];
  //static Color unselectedColor;

  PdeEditor editor;
  //PdeSketch sketch;

  int tabLeft[];
  int tabRight[];

  int menuLeft;
  int menuRight;

  //int sketchLeft;
  //int sketchRight;
  //int sketchTitleLeft;
  //boolean sketchModified;

  Font font;
  FontMetrics metrics;
  int fontAscent;

  //

  boolean menuVisible;
  JMenu menu;

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

    pieces = new Image[2][3];
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 4; j++) {
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

    addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          //System.out.println("got mouse");
          if ((sketchRight != 0) &&
              (e.getX() > sketchLeft) && (e.getX() < sketchRight)) {
            editor.skSaveAs(true);
          }
        }
      });
  }


  public void reset() {
    sketchLeft = 0;
    repaint();
  }


  public void paintComponent(Graphics screen) {
    if (screen == null) return;
    //if (editor.sketchName == null) return;

    PdeSketch sketch = editor.sketch;

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

    if ((tabLeft == null) ||
        (tabLeft.length < sketch.fileCount)) {
      tabLeft = new int[sketch.fileCount];
      tabRight = new int[sketch.fileCount];
    }

    int x = PdePreferences.GUI_SMALL;
    for (int i = 0; i < sketch.fileCount; i++) {
      String text = sketch.modified[i] ? 
        ("  " + sketch.names[i] + "  ") :
        ("  " + sketch.names[i] + " \u00A7");

      int textWidth = metrics.stringWidth(text);
      int pieceCount = 2 + (textWidth / PIECE_WIDTH);
      int pieceWidth = pieceCount * PIECE_WIDTH;

      state = (i == sketch.current) ? SELECTED : UNSELECTED;
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

      g.setColor(textColor[STATUS]);
      int baseline = (sizeH + fontAscent) / 2;
      g.drawString(names[i], textLeft, baseline);

      g.drawImage(pieces[state][RIGHT], x, 0, null);
      x += PIECE_WIDTH - 1;  // overlap by 1 pixel
    }

    menuLeft = sizeW - (16 + pieces[0][MENU].getWidth());
    menuRight = sizeW - 16;
    // draw the dropdown menu target
    g.drawImage(pieces[menuVisible ? SELECTED : UNSELECTED][MENU], 
                menuLeft, 0);

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


  /*
    New [code,tab,source] file...  Cmd-shift-N
    Rename
    Delete
    Hide
    Unhide  >
    Reset file list (not needed?)
  */
  public JMenu rebuildMenu() {
    if (menu != null) {
      menu.removeAll();
    } else {
      menu = new JMenu();
    }
    JMenuItem item;

    item = new JMenuItem("New");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
        }
      });
    menu.add(item);

    item = new JMenuItem("Rename");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
        }
      });
    menu.add(item);

    item = new JMenuItem("Delete");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
        }
      });
    menu.add(item);

    item = new JMenuItem("Hide");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
          // don't let the user hide if only 1 file open
        }
      });
    menu.add(item);

    JMenu unhide = new JMenu("Unhide");
    ActionListener unhideListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
          //System.out.println("unhide " + e);
          File from = (File) e.getActionCommand();
          String filename = from.getName();
          if (!filename.endsWith(".x")) {
            System.err.println("error while trying to unhide a file");
          } else if (!filename.exists()) {
            System.err.println("file no longer exists");
          } else {
            File to = new File(from.getPath(), 
                               filename.substring(0, filename.length() - 2));
            if (!from.renameTo(to)) {
              System.err.println("problem while unhiding");
            }
          }
          rebuildMenu();
        }
      };
    for (int i = 0; i < hiddenCount; i++) {
      item = new JMenuItem(hiddenNames[i]);
      item.setActionCommand(hiddenFiles[i]);
      item.addActionListener(unhideListener);
      hidden.add(item);
    }
    menu.add(unhide);

    menu.addSeparator();

    ActionListener jumpListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) { 
          //System.out.println("jump to " + e.getActionCommand());
          System.out.println("jump to " + e);
        }
      };
    for (int i = 0; i < fileCount; i++) {
      item = new JMenuItem(names[i]);
      //item.setActionCommand(files[i]);
      item.addActionListener(jumpListener);
      menu.add(item);
    }
  }


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
