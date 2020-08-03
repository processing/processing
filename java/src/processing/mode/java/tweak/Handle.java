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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Locale;


public class Handle {
  public String type;
  public String name;
  public String strValue;
  public String strNewValue;
  public int varIndex;
  public int startChar;
  public int endChar;
  public int newStartChar;
  public int newEndChar;
  public int line;
  int tabIndex;
  int decimalPlaces; // number of digits after the decimal point
  float incValue;

  java.lang.Number value, newValue;
  String strDiff;

  // connect with color control box
  ColorControlBox colorBox;

  // interface
  int x, y, width, height;
  int xCenter, xCurrent, xLast;
  HProgressBar progBar = null;
  String textFormat;

  // the client that sends the changes
  TweakClient tweakClient;


  public Handle(String t, String n, int vi, String v, int ti, int l, int sc,
                int ec, int dp) {
    type = t;
    name = n;
    varIndex = vi;
    strValue = v;
    tabIndex = ti;
    line = l;
    startChar = sc;
    endChar = ec;
    decimalPlaces = dp;

    incValue = (float) (1 / Math.pow(10, decimalPlaces));

    if ("int".equals(type)) {
      value = newValue = Integer.parseInt(strValue);
      strNewValue = strValue;
      textFormat = "%d";

    } else if ("hex".equals(type)) {
      Long val = Long.parseLong(strValue.substring(2, strValue.length()), 16);
      value = newValue = val.intValue();
      strNewValue = strValue;
      textFormat = "0x%x";

    } else if ("webcolor".equals(type)) {
      Long val = Long.parseLong(strValue.substring(1, strValue.length()), 16);
      val = val | 0xff000000;
      value = newValue = val.intValue();
      strNewValue = strValue;
      textFormat = "#%06x";

    } else if ("float".equals(type)) {
      value = newValue = Float.parseFloat(strValue);
      strNewValue = strValue;
      textFormat = "%.0" + decimalPlaces + "f";
    }

    newStartChar = startChar;
    newEndChar = endChar;
  }


  public void initInterface(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;

    // create drag ball
    progBar = new HProgressBar(height, width);
  }


  public void setCenterX(int mx) {
    xLast = xCurrent = xCenter = mx;
  }


  public void setCurrentX(int mx) {
    xLast = xCurrent;
    xCurrent = mx;

    progBar.setPos(xCurrent - xCenter);

    updateValue();
  }


  public void resetProgress() {
    progBar.setPos(0);
  }


  public void updateValue() {
    float change = getChange();

    if ("int".equals(type)) {
      if (newValue.intValue() + (int) change > Integer.MAX_VALUE ||
          newValue.intValue() + (int) change < Integer.MIN_VALUE) {
        change = 0;
        return;
      }
      setValue(newValue.intValue() + (int) change);
    } else if ("hex".equals(type)) {
      setValue(newValue.intValue() + (int) change);
    } else if ("webcolor".equals(type)) {
      setValue(newValue.intValue() + (int) change);
    } else if ("float".equals(type)) {
      setValue(newValue.floatValue() + change);
    }

    updateColorBox();
  }


  public void setValue(Number value) {
    if ("int".equals(type)) {
      newValue = value.intValue();
      strNewValue = String.format(Locale.US, textFormat, newValue.intValue());

    } else if ("hex".equals(type)) {
      newValue = value.intValue();
      strNewValue = String.format(Locale.US, textFormat, newValue.intValue());

    } else if ("webcolor".equals(type)) {
      newValue = value.intValue();
      // keep only RGB
      int val = (newValue.intValue() & 0xffffff);
      strNewValue = String.format(Locale.US, textFormat, val);

    } else if ("float".equals(type)) {
      BigDecimal bd = new BigDecimal(value.floatValue());
      bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP);
      newValue = bd.floatValue();
      strNewValue = String.format(Locale.US, textFormat, newValue.floatValue());
    }

    // send new data to the server in the sketch
    sendNewValue();
  }


  public void updateColorBox() {
    if (colorBox != null) {
      colorBox.colorChanged();
    }
  }


  private float getChange() {
    int pixels = xCurrent - xLast;
    return pixels * incValue;
  }


  public void setPos(int nx, int ny) {
    x = nx;
    y = ny;
  }


  public void setWidth(int w) {
    width = w;
    progBar.setWidth(w);
  }


  public void draw(Graphics2D g2d, boolean hasFocus) {
    AffineTransform prevTrans = g2d.getTransform();
    g2d.translate(x, y);

    // draw underline on the number
    g2d.setColor(ColorScheme.getInstance().progressFillColor);
    g2d.drawLine(0, 0, width, 0);

    if (hasFocus) {
      if (progBar != null) {
        g2d.translate(width / 2, 2);
        progBar.draw(g2d);
      }
    }

    g2d.setTransform(prevTrans);
  }


  public boolean pick(int mx, int my) {
    return pickText(mx, my);
  }


  public boolean pickText(int mx, int my) {
    return (mx > x - 2 && mx < x + width + 2 && my > y - height && my < y);
  }


  public boolean valueChanged() {
    if ("int".equals(type)) {
      return (value.intValue() != newValue.intValue());
    } else if ("hex".equals(type)) {
      return (value.intValue() != newValue.intValue());
    } else if ("webcolor".equals(type)) {
      return (value.intValue() != newValue.intValue());
    } else {
      return (value.floatValue() != newValue.floatValue());
    }
  }


  public void setColorBox(ColorControlBox box) {
    colorBox = box;
  }


  public void setTweakClient(TweakClient client) {
    tweakClient = client;
  }


  public void sendNewValue() {
    int index = varIndex;
    try {
      if ("int".equals(type)) {
        tweakClient.sendInt(index, newValue.intValue());
      } else if ("hex".equals(type)) {
        tweakClient.sendInt(index, newValue.intValue());
      } else if ("webcolor".equals(type)) {
        tweakClient.sendInt(index, newValue.intValue());
      } else if ("float".equals(type)) {
        tweakClient.sendFloat(index, newValue.floatValue());
      }
    } catch (Exception e) {
      System.out.println("error sending new value!");
    }
  }


  public String toString() {
    return type + " " + name + " = " + strValue + " (tab: " + tabIndex
      + ", line: " + line + ", start: " + startChar + ", end: "
      + endChar + ")";
  }
}


/*
 * Used for sorting the handles by order of occurrence inside each tab
 */
class HandleComparator implements Comparator<Handle> {
  public int compare(Handle handle1, Handle handle2) {
    int tab = handle1.tabIndex - handle2.tabIndex;
    if (tab != 0) {
      return tab;
    }
    return handle1.startChar - handle2.startChar;
  }
}
