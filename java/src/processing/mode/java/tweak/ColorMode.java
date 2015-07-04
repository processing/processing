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


public class ColorMode {
  final static int RGB = 0;
  final static int HSB = 1;

  float v1Max, v2Max, v3Max, aMax;
  int modeType;

  boolean unrecognizedMode;
  String drawContext;


  public ColorMode(String context) {
    this.drawContext = context;
    modeType = RGB;
    v1Max = 255;
    v2Max = 255;
    v3Max = 255;
    aMax = 255;
    unrecognizedMode = false;
  }


  public ColorMode(String context, int type,
                   float v1, float v2, float v3, float a) {
    this.drawContext = context;
    modeType = type;
    v1Max = v1;
    v2Max = v2;
    v3Max = v3;
    aMax = a;
    unrecognizedMode = false;
  }


  static public ColorMode fromString(String context, String mode) {
    try {
      String[] elements = mode.split(",");

      // determine the type of the color mode
      int type = RGB;
      if (elements[0].trim().equals("HSB")) {
        type = HSB;
      }

      if (elements.length == 1) {
        // colorMode in the form of colorMode(type)
        return new ColorMode(context, type, 255, 255, 255, 255);

      } else if (elements.length == 2) {
        // colorMode in the form of colorMode(type, max)
        float max = Float.parseFloat(elements[1].trim());
        return new ColorMode(context, type, max, max, max, max);

      } else if (elements.length == 4) {
        // colorMode in the form of colorMode(type, max1, max2, max3)
        float r = Float.parseFloat(elements[1].trim());
        float g = Float.parseFloat(elements[2].trim());
        float b = Float.parseFloat(elements[3].trim());
        return new ColorMode(context, type, r, g, b, 255);

      } else if (elements.length == 5) {
        // colorMode in the form of colorMode(type, max1, max2, max3, maxA)
        float r = Float.parseFloat(elements[1].trim());
        float g = Float.parseFloat(elements[2].trim());
        float b = Float.parseFloat(elements[3].trim());
        float a = Float.parseFloat(elements[4].trim());
        return new ColorMode(context, type, r, g, b, a);
      }
    } catch (Exception e) { }

    // if we failed to parse this mode (uses variables etc..)
    // we should still keep it so we'll know there is a mode declaration
    // and we should mark it as unrecognizable
    ColorMode newMode = new ColorMode(context);
    newMode.unrecognizedMode = true;
    return newMode;
  }


  public String toString() {
    String type = (modeType == RGB) ? "RGB" : "HSB";
    return "ColorMode: " + type +
      ": (" + v1Max + ", " + v2Max + ", " + v3Max + ", " + aMax + ")";
  }
}
