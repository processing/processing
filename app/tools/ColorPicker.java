/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2006 Ben Fry and Casey Reas

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

package processing.app.tools;

import processing.app.*;
import processing.core.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;


public class ColorPicker {

  Editor editor;

  int hue, saturation, brightness;  // 360, 100, 100
  int red, green, blue;   // 256, 256, 256

  ColorRange range;
  ColorSlider slider;

  JTextField hueField, saturationField, brightnessField;
  JTextField redField, greenField, blueField;

  JPanel colorPanel;


  public ColorPicker(Editor editor) {
    this.editor = editor;
  }


  public void show() {
    JFrame frame = new JFrame("Color Picker");
    frame.getContentPane().setLayout(new BorderLayout());

    Box box = Box.createHorizontalBox();
    box.setBorder(new EmptyBorder(12, 12, 12, 12));

    range = new ColorRange();
    range.init();
    JPanel rangePanel = new JPanel();
    rangePanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    rangePanel.add(range);
    box.add(rangePanel);
    box.add(Box.createHorizontalStrut(10));

    slider = new ColorSlider();
    slider.init();
    JPanel sliderPanel = new JPanel();
    sliderPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    sliderPanel.add(slider);
    box.add(sliderPanel);
    box.add(Box.createHorizontalStrut(10));

    box.add(createColorFields());

    frame.getContentPane().add(box, BorderLayout.CENTER);
    frame.pack();
    frame.show();
  }


  protected Container createColorFields() {
    //JLabel label = new JLabel();
    //int labelH = label.getPreferredSize().height;

    //JPanel panel = new JPanel();
    Box box = Box.createVerticalBox();

    colorPanel = new JPanel() {
        public void paintComponent(Graphics g) {
          g.setColor(new Color(red, green, blue));
          Dimension size = getSize();
          g.fillRect(0, 0, size.width, size.height);
        }
      };
    colorPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    Dimension dim = new Dimension(60, 30);
    colorPanel.setMinimumSize(dim);
    colorPanel.setMaximumSize(dim);
    colorPanel.setPreferredSize(dim);
    box.add(colorPanel);
    box.add(Box.createVerticalStrut(10));

    Box row;

    row = Box.createHorizontalBox();
    row.add(createFixedLabel("H:"));
    row.add(hueField = new NumberField(4));
    row.add(new JLabel("\u00B0"));  // degree symbol
    box.add(row);

    row = Box.createHorizontalBox();
    row.add(createFixedLabel("S:"));
    row.add(saturationField = new NumberField(4));
    row.add(new JLabel("%"));
    box.add(row);

    row = Box.createHorizontalBox();
    row.add(createFixedLabel("B:"));
    row.add(brightnessField = new NumberField(4));
    row.add(new JLabel("%"));
    box.add(row);

    //

    row = Box.createHorizontalBox();
    row.add(createFixedLabel("R:"));
    row.add(redField = new NumberField(4));
    box.add(row);

    row = Box.createHorizontalBox();
    row.add(createFixedLabel("G:"));
    row.add(greenField = new NumberField(4));
    box.add(row);

    row = Box.createHorizontalBox();
    row.add(createFixedLabel("B:"));
    row.add(blueField = new NumberField(4));
    box.add(row);

    return box;
  }


  int labelH;

  // return a label of a fixed width
  protected JLabel createFixedLabel(String title) {
    JLabel label = new JLabel(title);
    if (labelH == 0) {
      labelH = label.getPreferredSize().height;
    }
    Dimension dim = new Dimension(20, labelH);
    label.setPreferredSize(dim);
    label.setMinimumSize(dim);
    label.setMaximumSize(dim);
    return label;
  }

  /*
  public void setFixed(int what) {
    fixedColor = what;
    range.redraw();
  }
  */


  public class ColorRange extends PApplet {

    public void setup() {
      size(256, 256, P3D);
      noLoop();

      colorMode(HSB, 360, 256, 256);
      stroke(255);
      ellipseMode(CENTER);
    }

    public void draw() {
      int index = 0;
      for (int j = 0; j < 256; j++) {
        for (int i = 0; i < 256; i++) {
          pixels[index++] = color(hue, i, j);
        }
      }
    }

    public void mousePressed() {
      updateMouse();
    }

    public void mouseDragged() {
      updateMouse();
    }

    public void updateMouse() {
      if ((mouseX >= 0) && (mouseX < 256) &&
          (mouseY >= 0) && (mouseY < 256)) {
        int nsaturation = (int) (100 * (mouseX / 255.0f));
        int nbrightness = 100 - ((int) (100 * (mouseY / 255.0f)));
        saturationField.setText(String.valueOf(nsaturation));
        brightnessField.setText(String.valueOf(nbrightness));
      }
    }
  }


  public class ColorSlider extends PApplet {

    public void setup() {
      size(20, 256, P3D);
      noLoop();

      colorMode(HSB, 360, 100, 100);
    }

    public void draw() {
      int index = 0;
      for (int j = 0; j < 256; j++) {
        for (int i = 0; i < 256; i++) {
          pixels[index++] = color(hue, i, j);
        }
      }
    }

    public void mousePressed() {
      updateMouse();
    }

    public void mouseDragged() {
      updateMouse();
    }

    public void updateMouse() {
      if ((mouseX >= 0) && (mouseX < 256) &&
          (mouseY >= 0) && (mouseY < 256)) {
        int nhue = 359 - (int) (359 * (mouseY / 255.0f));
        hueField.setText(String.valueOf(nhue));
      }
    }
  }


  /**
   * Extension of JTextField that only allows numbers
   */
  class NumberField extends JTextField {

    public NumberField(int cols) {
      super(cols);
    }

    protected Document createDefaultModel() {
      return new NumberDocument();
    }
  }

  class NumberDocument extends PlainDocument {
    public void insertString(int offs, String str, AttributeSet a)
      throws BadLocationException {

      if (str == null) return;

      char chars[] = str.toCharArray();
      int charCount = 0;
      // remove any non-digit chars
      for (int i = 0; i < chars.length; i++) {
        if (Character.isDigit(chars[i])) {
          if (charCount != i) {  // shift if necessary
            chars[charCount++] = chars[i];
          }
        }
      }
      super.insertString(offs, new String(chars, 0, charCount), a);
    }
  }
}