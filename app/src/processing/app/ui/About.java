/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;

import processing.app.Base;
import processing.app.Platform;


public class About extends Window {
  ImageIcon icon;
  int width, height;


  public About(Frame frame) {
    super(frame);

    icon = Toolkit.getLibIconX("about");
    width = icon.getIconWidth();
    height = icon.getIconHeight();

    /*
    if (Toolkit.highResDisplay()) {
      image = Toolkit.getLibImage("about-2x.jpg"); //$NON-NLS-1$
      width = image.getWidth(null) / 2;
      height = image.getHeight(null) / 2;
    } else {
      image = Toolkit.getLibImage("about.jpg"); //$NON-NLS-1$
      width = image.getWidth(null);
      height = image.getHeight(null);
    }
    */

    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        dispose();
      }
    });

    addKeyListener(new KeyAdapter() {
      public void keyTyped(KeyEvent e) {
        System.out.println(e);
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          dispose();
        }
      }
    });

//    Dimension screen = Toolkit.getScreenSize();
//    setBounds((screen.width-width)/2, (screen.height-height)/2, width, height);
    setSize(width, height);
//    setLocationRelativeTo(null);
    setLocationRelativeTo(frame);
    setVisible(true);
    requestFocus();
  }


  public void paint(Graphics g) {
//    Graphics2D g2 = Toolkit.prepareGraphics(g);
//    g2.scale(0.5, 0.5);

    Graphics2D g2 = (Graphics2D) g;
    // OS X looks better doing its own thing, Windows and Linux need AA
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        Platform.isMacOS() ?
                        RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT :
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    g.drawImage(icon.getImage(), 0, 0, width, height, null);
//    g.setColor(Color.ORANGE);
//    g.fillRect(0, 0, width, height);

    g.setFont(Toolkit.getSansFont(12, Font.PLAIN));
    g.setColor(Color.WHITE);
    g.drawString(Base.getVersionName(), 26, 29);
  }
}