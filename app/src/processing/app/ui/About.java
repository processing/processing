/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-15 The Processing Foundation
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
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;

import processing.app.Base;


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

//    Dimension screen = Toolkit.getScreenSize();
//    setBounds((screen.width-width)/2, (screen.height-height)/2, width, height);
    setLocationRelativeTo(null);
    setVisible(true);
  }


  public void paint(Graphics g) {
    g.drawImage(icon.getImage(), 0, 0, width, height, null);

//    Graphics2D g2 = (Graphics2D) g;
//    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
//                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

    g.setFont(Toolkit.getSansFont(Font.PLAIN, 10));
    //g.setFont(new Font("SansSerif", Font.PLAIN, 10)); //$NON-NLS-1$
    g.setColor(Color.white);
    g.drawString(Base.getVersionName(), 26, 29);
  }
}