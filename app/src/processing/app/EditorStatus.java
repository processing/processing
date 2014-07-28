/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-10 Ben Fry and Casey Reas
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

import javax.swing.*;


/**
 * Panel just below the editing area that contains status messages.
 */
public class EditorStatus extends JPanel {
  Color[] bgcolor;
  Color[] fgcolor;

  static public final int NOTICE = 0;
  static public final int ERR    = 1;

  static final String NO_MESSAGE = "";

  Editor editor;

  int mode;
  String message;

  Font font;
  FontMetrics metrics;
  int ascent;

  Image offscreen;
  int sizeW, sizeH;

  boolean indeterminate;
  Thread thread;


  public EditorStatus(Editor editor) {
    this.editor = editor;
    empty();
    updateMode();
  }


  public void updateMode() {
    Mode mode = editor.getMode();
    bgcolor = new Color[] {
      mode.getColor("status.notice.bgcolor"),
      mode.getColor("status.error.bgcolor"),
    };

    fgcolor = new Color[] {
      mode.getColor("status.notice.fgcolor"),
      mode.getColor("status.error.fgcolor"),
    };

    font = mode.getFont("status.font");
    metrics = null;
  }


  public void empty() {
    mode = NOTICE;
    message = NO_MESSAGE;
    repaint();
  }


  public void notice(String message) {
    mode = NOTICE;
    this.message = message;
    repaint();
  }


  public void unnotice(String unmessage) {
    if (message.equals(unmessage)) empty();
  }


  public void error(String message) {
    mode = ERR;
    this.message = message;
    repaint();
  }


  public void startIndeterminate() {
    indeterminate = true;
    thread = new Thread() {
      public void run() {
        while (Thread.currentThread() == thread) {
          repaint();
          try {
            Thread.sleep(1000 / 10);
          } catch (InterruptedException e) { }
        }
      }
    };
    thread.start();
  }


  public void stopIndeterminate() {
    indeterminate = false;
    thread = null;
    repaint();
  }


  public void paintComponent(Graphics screen) {
    Dimension size = getSize();
    if ((size.width != sizeW) || (size.height != sizeH)) {
      // component has been resized
      offscreen = null;
    }

    if (offscreen == null) {
      sizeW = size.width;
      sizeH = size.height;
      if (Toolkit.highResDisplay()) {
        offscreen = createImage(sizeW*2, sizeH*2);
      } else {
        offscreen = createImage(sizeW, sizeH);
      }
    }

    Graphics g = offscreen.getGraphics();

    Graphics2D g2 = (Graphics2D) g;
    if (Toolkit.highResDisplay()) {
      g2.scale(2, 2);
      if (Base.isUsableOracleJava()) {
        // Oracle Java looks better with anti-aliasing turned on
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      }
    } else {
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    g.setFont(font);
    if (metrics == null) {
      metrics = g.getFontMetrics();
      ascent = metrics.getAscent();
    }

    //setBackground(bgcolor[mode]);  // does nothing

    g.setColor(bgcolor[mode]);
    g.fillRect(0, 0, sizeW, sizeH);

    g.setColor(fgcolor[mode]);
    g.setFont(font); // needs to be set each time on osx
    g.drawString(message, Preferences.GUI_SMALL, (sizeH + ascent) / 2);

    if (indeterminate) {
      int x = getWidth()*3 / 5;
      int y = getHeight() / 3;
      int w = getWidth() / 3;
      int h = getHeight() / 3;
//      g.setColor(fgcolor[mode]);
//      g.setColor(Color.DARK_GRAY);
      g.setColor(new Color(0x80000000, true));
      g.drawRect(x, y, w, h);
      for (int i = 0; i < 10; i++) {
        int r = (int) (x + Math.random() * w);
        g.drawLine(r, y, r, y+h);
      }
    }

    screen.drawImage(offscreen, 0, 0, sizeW, sizeH, null);
  }


  protected void setup() {
      setLayout(null);
  }


  public Dimension getPreferredSize() {
    return getMinimumSize();
  }


  public Dimension getMinimumSize() {
    return new Dimension(300, Preferences.GRID_SIZE);
  }


  public Dimension getMaximumSize() {
    return new Dimension(3000, Preferences.GRID_SIZE);
  }
}
