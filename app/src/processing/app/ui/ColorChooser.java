/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
  Copyright (c) 2006-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.ui;

import processing.app.Language;
import processing.app.Platform;
import processing.core.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;


/**
 * Generic color selector frame, pulled from the Tool object. API not really
 * worked out here (what should the constructor be? how flexible?) So use with
 * caution and be ready for it to break in future releases.
 */
public class ColorChooser {  //extends JFrame implements DocumentListener {

  int hue, saturation, brightness;  // range 360, 100, 100
  int red, green, blue;   // range 256, 256, 256

  ColorRange range;
  ColorSlider slider;

  JTextField hueField, saturationField, brightnessField;
  JTextField redField, greenField, blueField;

  JTextField hexField;

  JPanel colorPanel;
  DocumentListener colorListener;

  JDialog window;


//  public String getMenuTitle() {
//    return "Color Selector";
//  }


  public ColorChooser(Frame owner, boolean modal, Color initialColor,
                      String buttonName, ActionListener buttonListener) {
    //super("Color Selector");
    window = new JDialog(owner, Language.text("color_chooser"), modal);
    window.getContentPane().setLayout(new BorderLayout());

    Box box = Box.createHorizontalBox();
    box.setBorder(new EmptyBorder(12, 12, 12, 12));

    range = new ColorRange();
    Box rangeBox = new Box(BoxLayout.Y_AXIS);
    rangeBox.setAlignmentY(0);
    rangeBox.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    rangeBox.add(range);
    box.add(rangeBox);
    box.add(Box.createHorizontalStrut(10));

    slider = new ColorSlider();
    Box sliderBox = new Box(BoxLayout.Y_AXIS);
    sliderBox.setAlignmentY(0);
    sliderBox.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    sliderBox.add(slider);
    box.add(sliderBox);
    box.add(Box.createHorizontalStrut(10));

    box.add(createColorFields(buttonName, buttonListener));
//    System.out.println("1: " + hexField.getInsets());

    box.add(Box.createHorizontalStrut(10));

//    System.out.println("2: " + hexField.getInsets());

    window.getContentPane().add(box, BorderLayout.CENTER);
//    System.out.println(hexField);
//    System.out.println("3: " + hexField.getInsets());
//    colorPanel.setInsets(hexField.getInsets());

    window.pack();
    window.setResizable(false);

//    Dimension size = getSize();
//    Dimension screen = Toolkit.getScreenSize();
//    setLocation((screen.width - size.width) / 2,
//                (screen.height - size.height) / 2);
    window.setLocationRelativeTo(null);

    window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    window.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
          hide();
        }
      });
    Toolkit.registerWindowCloseKeys(window.getRootPane(), new ActionListener() {
        public void actionPerformed(ActionEvent actionEvent) {
          hide();
        }
      });

    Toolkit.setIcon(window);

    colorListener = new ColorListener();
    hueField.getDocument().addDocumentListener(colorListener);
    saturationField.getDocument().addDocumentListener(colorListener);
    brightnessField.getDocument().addDocumentListener(colorListener);
    redField.getDocument().addDocumentListener(colorListener);
    greenField.getDocument().addDocumentListener(colorListener);
    blueField.getDocument().addDocumentListener(colorListener);
    hexField.getDocument().addDocumentListener(colorListener);

    setColor(initialColor);
//    System.out.println("4: " + hexField.getInsets());
  }


  //hexField.setText("#FFFFFF");


  public void show() {
    window.setVisible(true);
  }


  public void hide() {
    window.setVisible(false);
  }


  public Color getColor() {
    return new Color(red, green, blue);
  }


  public void setColor(Color color) {
    updateRGB(color.getRGB());
  }


  public String getHexColor() {
    return "#" + PApplet.hex(red, 2) + PApplet.hex(green, 2) + PApplet.hex(blue, 2);
  }


  public class ColorListener implements DocumentListener {

    public void changedUpdate(DocumentEvent e) {
      //System.out.println("changed");
    }

    public void removeUpdate(DocumentEvent e) {
      //System.out.println("remove");
    }


  boolean updating;

  public void insertUpdate(DocumentEvent e) {
    if (updating) return;  // don't update forever recursively
    updating = true;

    Document doc = e.getDocument();
    if (doc == hueField.getDocument()) {
      hue = bounded(hue, hueField, 359);
      updateRGB();
      updateHex();

    } else if (doc == saturationField.getDocument()) {
      saturation = bounded(saturation, saturationField, 99);
      updateRGB();
      updateHex();

    } else if (doc == brightnessField.getDocument()) {
      brightness = bounded(brightness, brightnessField, 99);
      updateRGB();
      updateHex();

    } else if (doc == redField.getDocument()) {
      red = bounded(red, redField, 255);
      updateHSB();
      updateHex();

    } else if (doc == greenField.getDocument()) {
      green = bounded(green, greenField, 255);
      updateHSB();
      updateHex();

    } else if (doc == blueField.getDocument()) {
      blue = bounded(blue, blueField, 255);
      updateHSB();
      updateHex();

    } else if (doc == hexField.getDocument()) {
      String str = hexField.getText();
      if (str.startsWith("#")) {
        str = str.substring(1);
      }
      while (str.length() < 6) {
        str += "0";
      }
      if (str.length() > 6) {
        str = str.substring(0, 6);
      }
      updateRGB(Integer.parseInt(str, 16));
      updateHSB();
    }
    range.repaint();
    slider.repaint();
    //colorPanel.setBackground(new Color(red, green, blue));
    colorPanel.repaint();
    updating = false;
  }
}


  /**
   * Set the RGB values based on the current HSB values.
   */
  protected void updateRGB() {
    updateRGB(Color.HSBtoRGB(hue / 359f,
                             saturation / 99f,
                             brightness / 99f));
  }


  /**
   * Set the RGB values based on a calculated ARGB int.
   * Used by both updateRGB() to set the color from the HSB values,
   * and by updateHex(), to unpack the hex colors and assign them.
   */
  protected void updateRGB(int rgb) {
    red = (rgb >> 16) & 0xff;
    green = (rgb >> 8) & 0xff;
    blue = rgb & 0xff;

    redField.setText(String.valueOf(red));
    greenField.setText(String.valueOf(green));
    blueField.setText(String.valueOf(blue));
  }


  /**
   * Set the HSB values based on the current RGB values.
   */
  protected void updateHSB() {
    float hsb[] = new float[3];
    Color.RGBtoHSB(red, green, blue, hsb);

    hue = (int) (hsb[0] * 359.0f);
    saturation = (int) (hsb[1] * 99.0f);
    brightness = (int) (hsb[2] * 99.0f);

    hueField.setText(String.valueOf(hue));
    saturationField.setText(String.valueOf(saturation));
    brightnessField.setText(String.valueOf(brightness));
  }


  protected void updateHex() {
    hexField.setText(getHexColor());
  }


  /**
   * Get the bounded value for a specific range. If the value is outside
   * the max, you can't edit right away, so just act as if it's already
   * been bounded and return the bounded value, then fire an event to set
   * it to the value that was just returned.
   */
  protected int bounded(int current, final JTextField field, final int max) {
    String text = field.getText();
    if (text.length() == 0) {
      return 0;
    }
    try {
      int value = Integer.parseInt(text);
      if (value > max) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              field.setText(String.valueOf(max));
            }
          });
        return max;
      }
      return value;

    } catch (NumberFormatException e) {
      return current;  // should not be reachable
    }
  }


  protected Container createColorFields(String buttonName, ActionListener buttonListener) {
    Box box = Box.createVerticalBox();
    box.setAlignmentY(0);

    final int GAP = Platform.isWindows() ? 5 : 0;
    final int BETWEEN = Platform.isWindows() ? 8 : 6; //10;

    Box row;

    row = Box.createHorizontalBox();
    if (Platform.isMacOS()) {
      row.add(Box.createHorizontalStrut(17));
    } else {
      row.add(createFixedLabel(""));
    }
    // Can't just set the bg color of the panel because it also tints the bevel
    // (on OS X), which looks odd. So instead we override paintComponent().
    colorPanel = new JPanel() {
        public void paintComponent(Graphics g) {
          g.setColor(new Color(red, green, blue));
          Dimension size = getSize();
          g.fillRect(0, 0, size.width, size.height);
        }
      };
    colorPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
    Dimension dim = new Dimension(70, 25);
    colorPanel.setMinimumSize(dim);
    colorPanel.setMaximumSize(dim);
    colorPanel.setPreferredSize(dim);
    row.add(colorPanel);
    row.add(Box.createHorizontalGlue());
    box.add(row);
    box.add(Box.createVerticalStrut(BETWEEN));
//    if (Base.isMacOS()) {  // need a little extra
//      box.add(Box.createVerticalStrut(BETWEEN));
//    }


    row = Box.createHorizontalBox();
    row.add(createFixedLabel("H"));
    row.add(hueField = new NumberField(4, false));
    row.add(new JLabel(" \u00B0"));  // degree symbol
    row.add(Box.createHorizontalGlue());
    box.add(row);
    box.add(Box.createVerticalStrut(GAP));

    row = Box.createHorizontalBox();
    row.add(createFixedLabel("S"));
    row.add(saturationField = new NumberField(4, false));
    row.add(new JLabel(" %"));
    row.add(Box.createHorizontalGlue());
    box.add(row);
    box.add(Box.createVerticalStrut(GAP));

    row = Box.createHorizontalBox();
    row.add(createFixedLabel("B"));
    row.add(brightnessField = new NumberField(4, false));
    row.add(new JLabel(" %"));
    row.add(Box.createHorizontalGlue());
    box.add(row);
    box.add(Box.createVerticalStrut(BETWEEN));

    //

    row = Box.createHorizontalBox();
    row.add(createFixedLabel("R"));
    row.add(redField = new NumberField(4, false));
    row.add(Box.createHorizontalGlue());
    box.add(row);
    box.add(Box.createVerticalStrut(GAP));

    row = Box.createHorizontalBox();
    row.add(createFixedLabel("G"));
    row.add(greenField = new NumberField(4, false));
    row.add(Box.createHorizontalGlue());
    box.add(row);
    box.add(Box.createVerticalStrut(GAP));

    row = Box.createHorizontalBox();
    row.add(createFixedLabel("B"));
    row.add(blueField = new NumberField(4, false));
    row.add(Box.createHorizontalGlue());
    box.add(row);
    box.add(Box.createVerticalStrut(BETWEEN));

    //

    row = Box.createHorizontalBox();
    row.add(createFixedLabel(""));
    // Windows needs extra space, OS X and Linux do not
    // Mac OS X needs 6 because #CCCCCC is quite wide
    final int hexCount = Platform.isWindows() ? 7 : 6;
    row.add(hexField = new NumberField(hexCount, true));
    row.add(Box.createHorizontalGlue());
    box.add(row);
    box.add(Box.createVerticalStrut(GAP));

    //

//    // Not great, because the insets make things weird anyway
//    //Dimension dim = new Dimension(hexField.getPreferredSize());
//    Dimension dim = new Dimension(70, 20);
//    colorPanel.setMinimumSize(dim);
//    colorPanel.setMaximumSize(dim);
//    colorPanel.setPreferredSize(dim);
////    colorPanel.setBorder(new EmptyBorder(hexField.getInsets()));

    //

    row = Box.createHorizontalBox();
    if (Platform.isMacOS()) {
      row.add(Box.createHorizontalStrut(11));
    } else {
      row.add(createFixedLabel(""));
    }
    JButton button = new JButton(buttonName);
    button.addActionListener(buttonListener);
    //System.out.println("button: " + button.getInsets());
    row.add(button);
    row.add(Box.createHorizontalGlue());
    box.add(row);

    row = Box.createHorizontalBox();
    if (Platform.isMacOS()) {
      row.add(Box.createHorizontalStrut(11));
    } else {
      row.add(createFixedLabel(""));
    }
    button = new JButton(Language.text("prompt.cancel"));
    button.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        ColorChooser.this.hide();
      }
    });
    row.add(button);
    row.add(Box.createHorizontalGlue());
    box.add(row);
    //

    box.add(Box.createVerticalGlue());
    return box;
  }


  int labelH;

  /**
   * return a label of a fixed width
   */
  protected JLabel createFixedLabel(String title) {
    JLabel label = new JLabel(title);
    if (labelH == 0) {
      labelH = label.getPreferredSize().height;
    }
    Dimension dim = new Dimension(15, labelH);
    label.setPreferredSize(dim);
    label.setMinimumSize(dim);
    label.setMaximumSize(dim);
    return label;
  }


  public class ColorRange extends JComponent {

    static final int WIDE = 256;
    static final int HIGH = 256;

    private int lastX, lastY;

    public ColorRange() {
      setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          updateMouse(e);
        }
      });

      addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          updateMouse(e);
        }
      });

      addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          super.keyPressed(e);

          if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
              ColorChooser.this.hide();
          }
        }
      });
    }

    private void updateMouse(MouseEvent e) {
      int mouseX = e.getX();
      int mouseY = e.getY();

      if ((mouseX >= 0) && (mouseX < WIDE) &&
            (mouseY >= 0) && (mouseY < HIGH)) {
        int nsaturation = (int) (100 * (mouseX / 255.0f));
        int nbrightness = 100 - ((int) (100 * (mouseY / 255.0f)));
        saturationField.setText(String.valueOf(nsaturation));
        brightnessField.setText(String.valueOf(nbrightness));

        lastX = mouseX;
        lastY = mouseY;
      }
    }

    @Override
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      for (int j = 0; j < WIDE; j++) {
        for (int i = 0; i < HIGH; i++) {
          g.setColor(Color.getHSBColor(hue / 360f, i / 256f, (255 - j) / 256f));
          g.fillRect(i, j, 1, 1);
        }
      }

      g.setColor((brightness > 50) ? Color.BLACK : Color.WHITE);
      g.drawRect(lastX - 5, lastY - 5, 10, 10);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(WIDE, HIGH);
    }

    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }
  }


  public class ColorSlider extends JComponent {

    static final int WIDE = 20;
    static final int HIGH = 256;

    public ColorSlider() {
      setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          updateMouse(e);
        }
      });

      addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent e) {
          updateMouse(e);
        }
      });

      addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          super.keyPressed(e);

          if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
              ColorChooser.this.hide();
          }
        }
      });
    }

    private void updateMouse(MouseEvent e) {
      int mouseX = e.getX();
      int mouseY = e.getY();

      if ((mouseX >= 0) && (mouseX < WIDE) &&
              (mouseY >= 0) && (mouseY < HIGH)) {
        int nhue = 359 - (int) (359 * (mouseY / 255.0f));
        hueField.setText(String.valueOf(nhue));
      }
    }

    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      int sel = 255 - (int) (255 * (hue / 359.0));
      for (int j = 0; j < HIGH; j++) {
        Color color = Color.getHSBColor((255 - j) / 256f, 1, 1);
        if (j == sel) {
            color = Color.BLACK;
        }
        g.setColor(color);
        g.drawRect(0, j, WIDE, 1);
      }
    }

    public Dimension getPreferredSize() {
      return new Dimension(WIDE, HIGH);
    }

    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    public Dimension getMaximumSize() {
      return getPreferredSize();
    }
  }


  /**
   * Extension of JTextField that only allows numbers
   */
  static class NumberField extends JTextField {

    public boolean allowHex;

    public NumberField(int cols, boolean allowHex) {
      super(cols);
      this.allowHex = allowHex;
    }

    protected Document createDefaultModel() {
      return new NumberDocument(this);
    }

    public Dimension getPreferredSize() {
      if (!allowHex) {
        return new Dimension(45, super.getPreferredSize().height);
      }
      return super.getPreferredSize();
    }

    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    public Dimension getMaximumSize() {
      return getPreferredSize();
    }
  }


  /**
   * Document model to go with JTextField that only allows numbers.
   */
  static class NumberDocument extends PlainDocument {

    NumberField parentField;

    public NumberDocument(NumberField parentField) {
      this.parentField = parentField;
      //System.out.println("setting parent to " + parentSelector);
    }

    public void insertString(int offs, String str, AttributeSet a)
      throws BadLocationException {

      if (str == null) return;

      char chars[] = str.toCharArray();
      int charCount = 0;
      // remove any non-digit chars
      for (int i = 0; i < chars.length; i++) {
        boolean ok = Character.isDigit(chars[i]);
        if (parentField.allowHex) {
          if ((chars[i] >= 'A') && (chars[i] <= 'F')) ok = true;
          if ((chars[i] >= 'a') && (chars[i] <= 'f')) ok = true;
          if ((offs == 0) && (i == 0) && (chars[i] == '#')) ok = true;
        }
        if (ok) {
          if (charCount != i) {  // shift if necessary
            chars[charCount] = chars[i];
          }
          charCount++;
        }
      }
      super.insertString(offs, new String(chars, 0, charCount), a);
      // can't call any sort of methods on the enclosing class here
      // seems to have something to do with how Document objects are set up
    }
  }


//  static public void main(String[] args) {
//    ColorSelector cs = new ColorSelector();
//    cs.init(null);
//    EventQueue.invokeLater(cs);
//  }
}
