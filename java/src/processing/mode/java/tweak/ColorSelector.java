/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org
  Copyright (c) 2012-15 The Processing Foundation

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation, Inc.
  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.tweak;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;


public class ColorSelector {
  int hue, saturation, brightness;

  public JFrame frame;
  public ColorControlBox colorBox;
  ColorSelectorBox selectorBox;
  ColorSelectorSlider selectorSlider;
  SelectorTopBar topBar;


  public ColorSelector(ColorControlBox colorBox) {
    this.colorBox = colorBox;
    createFrame();
  }


  public void createFrame() {
    frame = new JFrame();
    frame.setBackground(Color.BLACK);

    Box box = Box.createHorizontalBox();
    box.setBackground(Color.BLACK);

    selectorSlider = new ColorSelectorSlider();

    if (!colorBox.isBW) {
      selectorBox = new ColorSelectorBox();
      box.add(selectorBox);
    }

    box.add(Box.createHorizontalGlue());
    box.add(selectorSlider, BorderLayout.CENTER);
    box.add(Box.createHorizontalGlue());

    frame.getContentPane().add(box, BorderLayout.CENTER);
    frame.pack();
    frame.setResizable(false);
    frame.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
  }


  public void show(int x, int y) {
    frame.setLocation(x, y);
    frame.setVisible(true);
    frame.repaint();
  }


  public void hide() {
    this.colorBox = null;
    frame.setVisible(false);
  }


  public void refreshColor() {
    if (!colorBox.ilegalColor) {
      setColor(colorBox.color);
    }
  }


  public void setColor(Color c) {
    if (selectorBox != null) {
      selectorBox.setToColor(c);
    }
    selectorSlider.setToColor(c);
    repaintSelector();
  }


  public void satBrightChanged() {
    repaintSelector();
  }


  public void hueChanged() {
    if (selectorBox != null) {
      selectorBox.renderBack();
    }
    repaintSelector();
  }


  public void repaintSelector() {
    if (selectorBox != null) {
      selectorBox.repaint();
    }
    selectorSlider.repaint();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class ColorSelectorBox extends JComponent {
    int lastX, lastY;
    BufferedImage backImg;

    ColorSelectorBox() {
      if (!colorBox.ilegalColor) {
        setToColor(colorBox.color);
      }
      renderBack();

      addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          updateMouse(e);
        }
      });
      addMouseMotionListener(new MouseAdapter() {
        public void mouseDragged(MouseEvent e) {
          updateMouse(e);
        }
      });
    }


    public void paintComponent(Graphics g) {
      g.drawImage(backImg, 0, 0, this);

      Graphics2D g2 = (Graphics2D) g;
      // otherwise the oval is hideous
      // TODO make a proper hidpi version of all this
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_ON);

      g.setColor(lastY < 128 ? Color.BLACK : Color.WHITE);
      AffineTransform tx = g2.getTransform();
      g2.translate(lastX, lastY);
      //g2.drawOval(0, 0, 5, 5);
      g2.drawOval(-3, -3, 6, 6);
      g2.drawLine(-8, 0, -6, 0);
      g2.drawLine(6, 0, 8, 0);
      g2.drawLine(0, -8, 0, -6);
      g2.drawLine(0, 6, 0, 8);
      g2.setTransform(tx);
    }


    public void renderBack() {
      int[] pixels = new int[256 * 256];
      int index = 0;
      for (int j = 0; j < 256; j++) {
        for (int i = 0; i < 256; i++) {
          pixels[index++] =  // color(hue, i, 255-j);
            Color.HSBtoRGB(hue / 255f, (i / 255f), (255-j)/255f);
        }
      }
      backImg = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
      backImg.getRaster().setDataElements(0, 0, 256, 256, pixels);
    }


    public void setToColor(Color c) {
      // set selector color
      float hsb[] = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
      saturation = (int) (hsb[1] * 255);
      brightness = (int) (hsb[2] * 255);
      lastX = saturation;
      lastY = 255 - brightness;
    }


    void updateMouse(MouseEvent event) {
      int mouseX = event.getX();
      int mouseY = event.getY();

      if (mouseX >= 0 && mouseX < 256 &&
          mouseY >= 0 && mouseY < 256) {
        lastX = mouseX;
        lastY = mouseY;
        updateColor();
      }
    }


    void updateColor() {
      saturation = lastX;
      brightness = 255 - lastY;

      satBrightChanged();
      colorBox.selectorChanged(hue, saturation, brightness);
    }


    public Dimension getPreferredSize() {
      return new Dimension(256, 256);
    }


    public Dimension getMinimumSize() {
      return getPreferredSize();
    }


    public Dimension getMaximumSize() {
      return getPreferredSize();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class ColorSelectorSlider extends JComponent {
    final int WIDE = 30;
    BufferedImage backImg;
    int lastY;

    ColorSelectorSlider() {
//      size(30, 255);
//      noLoop();
//      colorMode(HSB, 255, 255, 255);
//      strokeWeight(1);
//      noFill();
//      loadPixels();
      if (!colorBox.ilegalColor) {
        setToColor(colorBox.color);
      }

      // draw the slider background
      renderBack();

      addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          updateMouse(e);
        }
      });
      addMouseMotionListener(new MouseAdapter() {
        public void mouseDragged(MouseEvent e) {
          updateMouse(e);
        }
      });
    }

    public void paintComponent(Graphics g) {
      g.drawImage(backImg, 0, 0, this);

      Graphics2D g2 = (Graphics2D) g;
//      if (colorBox.isBW) {
//        stroke(lastY<128 ? 0 : 255);
//      }
//      else {
//        stroke(0);
//      }
      if (colorBox.isBW && lastY >= 128) {
        g2.setColor(Color.WHITE);
      } else {
        g2.setColor(Color.BLACK);
      }

      AffineTransform tx = g2.getTransform();
      g2.translate(0, lastY);
      // draw left bracket
//      beginShape();
//      vertex(5, -2);
//      vertex(1, -2);
//      vertex(1, 2);
//      vertex(5, 2);
//      endShape();
      g.drawRect(1, -2, 6, 4);

      // draw middle lines
      g.drawLine(13, 0, 17, 0);
      g.drawLine(15, -2, 15, 2);

      // draw right bracket
//      beginShape();
//      vertex(24, -2);
//      vertex(28, -2);
//      vertex(28, 2);
//      vertex(24, 2);
//      endShape();
      g.drawRect(24, -2, 4, 4);
      g2.setTransform(tx);

      /*
      if (colorBox.isBW) {
//        stroke(255);
//        rect(0, 0, 29, 254);
        g.setColor(Color.WHITE);
        g.drawRect(0, 0, WIDE, 255);
      } else {
//        stroke(0);
//        line(0, 0, 0, 255);
//        line(29, 0, 29, 255);
        g.setColor(Color.BLACK);
        g.drawLine(0, 0, 0, 255);
        g.drawLine(29, 0, 29, 255);
      }
      */
    }


    void renderBack() {
      int[] pixels = new int[WIDE * 256];
      int index = 0;

      int argb = 0;
      for (int j = 0; j < 256; j++) {
        if (colorBox.isBW) {
          int gray = 255 - j;
          argb = 0xff000000 | (gray << 16) | (gray << 8) | gray;
        } else {
          // color(255-j, 255, 255);
          argb = Color.HSBtoRGB((255 - j) / 255f, 1, 1);
        }
        for (int i = 0; i < WIDE; i++) {
          pixels[index++] = argb;
        }
      }
      backImg = new BufferedImage(WIDE, 256, BufferedImage.TYPE_INT_RGB);
      backImg.getRaster().setDataElements(0, 0, WIDE, 256, pixels);
    }


    void setToColor(Color c) {
      // set slider position
      if (colorBox.isBW) {
        hue = c.getRed();
      } else {
        float hsb[] = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hue = (int)(hsb[0]*255);
      }
      lastY = 255 - hue;
    }


    void updateMouse(MouseEvent event) {
      int mouseY = event.getY();
      if (mouseY >= 0 && mouseY < 256) {
        lastY = mouseY;
        updateColor();
      }
    }


    public void updateColor() {
      hue = 255 - lastY;
      hueChanged();
      colorBox.selectorChanged(hue, saturation, brightness);
    }


    public Dimension getPreferredSize() {
      return new Dimension(30, 255);
    }


    public Dimension getMinimumSize() {
      return getPreferredSize();
    }


    public Dimension getMaximumSize() {
      return getPreferredSize();
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  public class SelectorTopBar extends JComponent {
    int barWidth;
    int barHeight = 16;

    public SelectorTopBar(int w) {
      barWidth = w;
    }

    @Override
    public void paintComponent(Graphics g) {
      g.setColor(Color.GRAY);
      Dimension size = getSize();
      g.fillRect(0, 0, size.width, size.height);
    }

    public Dimension getPreferredSize() {
      return new Dimension(barWidth, barHeight);
    }

    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    public Dimension getMaximumSize() {
      return getPreferredSize();
    }
  }
}
