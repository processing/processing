/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeEditorHeader - panel that containing the sketch title
  Part of the Processing project - http://Proce55ing.net

  Copyright (c) 2001-03 
  Ben Fry, Massachusetts Institute of Technology and 
  Casey Reas, Interaction Design Institute Ivrea

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


public class PdeEditorHeader extends JPanel {
  static final String SKETCH_TITLER = "sketch";

  static Color primaryColor;
  static Color secondaryColor;
  static Color backgroundColor;

  PdeEditor editor;

  int sketchLeft;
  int sketchRight;
  int sketchTitleLeft;
  boolean sketchModified;

  Font font;
  FontMetrics metrics;
  int fontAscent;

  Image offscreen;
  int sizeW, sizeH;
  int imageW, imageH;


  public PdeEditorHeader(PdeEditor eddie) { 
    this.editor = eddie; // weird name for listener

    if (primaryColor == null) {
      backgroundColor = PdePreferences.getColor("header.bgcolor");
      primaryColor    = PdePreferences.getColor("header.fgcolor.primary");
      secondaryColor  = PdePreferences.getColor("header.fgcolor.secondary");
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
    //userLeft = 0;
    //update();
    repaint();
  }


  /*
  public void update() {
    paint(this.getGraphics());
  }

  public void update(Graphics g) {
    paint(g);
  }
  */


  public void paintComponent(Graphics screen) {
    if (screen == null) return;
    if (editor.sketchName == null) return;

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
      //userLeft = 0; // reset
      imageW = sizeW;
      imageH = sizeH;
      offscreen = createImage(imageW, imageH);
    }

    Graphics g = offscreen.getGraphics();
    if (font == null) {
      font = PdePreferences.getFont("header.font");
      g.setFont(font);
      metrics = g.getFontMetrics();
      fontAscent = metrics.getAscent();
    }

    //if (sketchLeft == 0) {
    sketchTitleLeft = PdePreferences.GUI_SMALL;
    sketchLeft = sketchTitleLeft + 
      metrics.stringWidth(SKETCH_TITLER) + PdePreferences.GUI_SMALL;
    sketchRight = sketchLeft + metrics.stringWidth(editor.sketchName);
    int modifiedLeft = sketchRight + PdePreferences.GUI_SMALL;
    //int modifiedLeft = sketchLeft + 
    //metrics.stringWidth(editor.sketchName) + PdePreferences.GUI_SMALL;

    //sketch = editor.sketchName;
    //if (sketch == null) sketch = "";
    //}

    //if (userLeft == 0) {
    //userLeft = sizeW - 20 - metrics.stringWidth(editor.userName);
    //userTitleLeft = userLeft - PdePreferences.GUI_SMALL - 
    //metrics.stringWidth(USER_TITLER);

    //user = editor.userName;
    //if (user == null) user = "";
    //}

    int baseline = (sizeH + fontAscent) / 2;

    g.setColor(backgroundColor);
    g.fillRect(0, 0, imageW, imageH);

    //boolean boringUser = editor.userName.equals("default");

    g.setFont(font); // needs to be set each time
    g.setColor(secondaryColor);
    g.drawString(SKETCH_TITLER, sketchTitleLeft, baseline);
    if (sketchModified) g.drawString("\u00A7", modifiedLeft, baseline);
    //if (!boringUser) g.drawString(USER_TITLER, userTitleLeft, baseline);

    g.setColor(primaryColor);
    //g.drawString(sketch, sketchLeft, baseline);
    //String additional = sketchModified ? " \u2020" : "";
    //String additional = sketchModified ? " \u00A4" : "";
    //String additional = sketchModified ? " \u2022" : "";
    g.drawString(editor.sketchName, sketchLeft, baseline);

    //if (!boringUser) g.drawString(editor.userName, userLeft, baseline);

    //g.setColor(fgColor[mode]);
    //g.drawString(message, PdePreferences.GUI_SMALL, (sizeH + fontAscent) / 2);

    screen.drawImage(offscreen, 0, 0, null);
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
