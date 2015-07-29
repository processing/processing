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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import processing.mode.java.pdex.JavaTextAreaPainter;


public class ColorControlBox {
  public boolean visible;
  ArrayList<Handle> handles;
  ColorMode colorMode;
  Color color;
  boolean ilegalColor = false;
  boolean isBW;
  boolean isHex;

  String drawContext;

  // interface
  int x, y, width, height;
  JavaTextAreaPainter painter;


  public ColorControlBox(String context, ColorMode mode,
                         ArrayList<Handle> handles) {
    this.drawContext = context;
    this.colorMode = mode;
    this.handles = handles;

    // add this box to the handles so they can update this color on change
    for (Handle h : handles) {
      h.setColorBox(this);
    }

    isBW = isGrayScale();
    isHex = isHexColor();
    color = getCurrentColor();

    visible = Settings.alwaysShowColorBoxes;
  }


  public void initInterface(JavaTextAreaPainter textAreaPainter,
                            int x, int y, int w, int h) {
    this.painter = textAreaPainter;
    this.x = x;
    this.y = y;
    this.width = w;
    this.height = h;
  }


  public void setPos(int x, int y) {
    this.x = x;
    this.y = y;
  }


  public void draw(Graphics2D g2d) {
    if (!visible) {
      return;
    }

    AffineTransform trans = g2d.getTransform();
    g2d.translate(x, y);

    // draw current color
    g2d.setColor(color);
    g2d.fillRoundRect(0, 0, width, height, 5, 5);

    // draw black outline
    g2d.setStroke(new BasicStroke(1));
    g2d.setColor(Color.BLACK);
    g2d.drawRoundRect(0, 0, width, height, 5, 5);

    if (ilegalColor) {
      g2d.setColor(Color.RED);
      g2d.setStroke(new BasicStroke(2));
      g2d.drawLine(width-3, 3, 3, height-3);
    }

    g2d.setTransform(trans);
  }


  public boolean isGrayScale() {
    if (handles.size() <= 2) {
      int value = handles.get(0).newValue.intValue();
      if ((value&0xff000000) == 0) {
        return true;
      }
    }

    return false;
  }


  /**
   * @return true if number is hex or webcolor
   */
  private boolean isHexColor() {
    if (handles.get(0).type == "hex" || handles.get(0).type == "webcolor") {
      int value = handles.get(0).value.intValue();
      if ((value & 0xff000000) != 0) {
        return true;
      }
    }
    return false;
  }


  public Color getCurrentColor()
  {
    try {
      if (handles.size() == 1)
      {
        if (isBW) {
          // treat as color(gray)
          float gray = handles.get(0).newValue.floatValue();
          return verifiedGrayColor(gray);
        }
        else {
          // treat as color(argb)
          int argb = handles.get(0).newValue.intValue();
          return verifiedHexColor(argb);
        }
      }
      else if (handles.size() == 2)
      {
        if (isBW) {
          // color(gray, alpha)
          float gray = handles.get(0).newValue.floatValue();
          return verifiedGrayColor(gray);
        }
        else {
          // treat as color(argb, a)
          int argb = handles.get(0).newValue.intValue();
          float a = handles.get(1).newValue.floatValue();
          return verifiedHexColor(argb, a);
        }
      }
      else if (handles.size() == 3)
      {
        // color(v1, v2, v3)
        float v1 = handles.get(0).newValue.floatValue();
        float v2 = handles.get(1).newValue.floatValue();
        float v3 = handles.get(2).newValue.floatValue();

        if (colorMode.modeType == ColorMode.RGB) {
          return verifiedRGBColor(v1, v2, v3, colorMode.aMax);
        }
        else {
          return verifiedHSBColor(v1, v2, v3, colorMode.aMax);
        }
      }
      else if (handles.size() == 4)
      {
        // color(v1, v2, v3, alpha)
        float v1 = handles.get(0).newValue.floatValue();
        float v2 = handles.get(1).newValue.floatValue();
        float v3 = handles.get(2).newValue.floatValue();
        float a = handles.get(3).newValue.floatValue();

        if (colorMode.modeType == ColorMode.RGB) {
          return verifiedRGBColor(v1, v2, v3, a);
        }
        else {
          return verifiedHSBColor(v1, v2, v3, a);
        }
      }
    }
    catch (Exception e) {
      System.out.println("error parsing color value: " + e.toString());
      ilegalColor = true;
      return Color.WHITE;
    }

    // couldn't figure out this color, return WHITE color
    ilegalColor = true;
    return Color.WHITE;
  }

  private Color verifiedGrayColor(float gray)
  {
    if (gray < 0 || gray > colorMode.v1Max) {
      return colorError();
    }

    ilegalColor = false;
    gray = gray/colorMode.v1Max * 255;
    return new Color((int)gray, (int)gray, (int)gray, 255);
  }

  private Color verifiedHexColor(int argb)
  {
    int r = (argb>>16)&0xff;
    int g = (argb>>8)&0xff;
    int b = (argb&0xff);

    ilegalColor = false;
    return new Color(r, g, b, 255);
  }

  private Color verifiedHexColor(int argb, float alpha)
  {
    int r = (argb>>16)&0xff;
    int g = (argb>>8)&0xff;
    int b = (argb&0xff);

    ilegalColor = false;
    return new Color(r, g, b, 255);
  }

  public Color verifiedRGBColor(float r, float g, float b, float a)
  {
    if (r < 0 || r > colorMode.v1Max ||
      g < 0 || g > colorMode.v2Max ||
      b < 0 || b > colorMode.v3Max) {
      return colorError();
    }

    ilegalColor = false;
    r = r/colorMode.v1Max * 255;
    g = g/colorMode.v2Max * 255;
    b = b/colorMode.v3Max * 255;
    return new Color((int)r, (int)g, (int)b, 255);
  }

  public Color verifiedHSBColor(float h, float s, float b, float a)
  {
    if (h < 0 || h > colorMode.v1Max ||
      s < 0 || s > colorMode.v2Max ||
      b < 0 || b > colorMode.v3Max) {
      return colorError();
    }

    ilegalColor = false;
    Color c = Color.getHSBColor(h/colorMode.v1Max, s/colorMode.v2Max, b/colorMode.v3Max);
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);
  }

  private Color colorError()
  {
    ilegalColor = true;
    return Color.WHITE;
  }

  public void colorChanged()
  {
    color = getCurrentColor();
  }

  public int getTabIndex()
  {
    return handles.get(0).tabIndex;
  }

  public int getLine()
  {
    return handles.get(0).line;
  }

  public int getCharIndex()
  {
    int lastHandle = handles.size()-1;
    return handles.get(lastHandle).newEndChar + 2;
  }

  /* Check if the point is in the box
   *
   */
  public boolean pick(int mx, int my)
  {
    if (!visible) {
      return false;
    }

    if (mx>x && mx < x+width && my>y && my<y+height) {
      return true;
    }

    return false;
  }

  /* Only show the color box if mouse is on the same line
   *
   * return true if there was change
   */
  public boolean setMouseY(int my)
  {
    boolean change = false;

    if (my>y && my<y+height) {
      if (!visible) {
        change = true;
      }
      visible = true;
    }
    else {
      if (visible) {
        change = true;
      }
      visible = false;
    }

    return change;
  }

  /* Update the color numbers with the new values that were selected
   * in the color selector
   *
   *  hue, saturation and brightness parameters are always 0-255
   */
  public void selectorChanged(int hue, int saturation, int brightness)
  {
    if (isBW) {
      // color(gray) or color(gray, alpha)
      handles.get(0).setValue((float)hue/255*colorMode.v1Max);
    }
    else {
      if (handles.size() == 1 || handles.size() == 2) {
        // color(argb)
        int prevVal = handles.get(0).newValue.intValue();
        int prevAlpha = (prevVal>>24)&0xff;
        Color c = Color.getHSBColor((float)hue/255, (float)saturation/255, (float)brightness/255);
        int newVal = (prevAlpha<<24) | (c.getRed()<<16) | (c.getGreen()<<8) | (c.getBlue());
        handles.get(0).setValue(newVal);
      }
      else if (handles.size() == 3 || handles.size() == 4) {
        // color(v1, v2, v3) or color(v1, v2, v3, alpha)
        if (colorMode.modeType == ColorMode.HSB) {
          // HSB
          float v1 = (float)hue/255 * colorMode.v1Max;
          float v2 = (float)saturation/255 * colorMode.v2Max;
          float v3 = (float)brightness/255 * colorMode.v3Max;
          handles.get(0).setValue(v1);
          handles.get(1).setValue(v2);
          handles.get(2).setValue(v3);
        }
        else {
          // RGB
          Color c = Color.getHSBColor((float)hue/255, (float)saturation/255, (float)brightness/255);
          handles.get(0).setValue((float)c.getRed()/255*colorMode.v1Max);
          handles.get(1).setValue((float)c.getGreen()/255*colorMode.v2Max);
          handles.get(2).setValue((float)c.getBlue()/255*colorMode.v3Max);
        }
      }
    }

    // update our own color
    color = getCurrentColor();

    // update code text painter so the user will see the changes
    painter.updateCodeText();
    painter.repaint();
  }


  public String toString() {
    return handles.size() + " handles, color mode: " + colorMode.toString();
  }
}
