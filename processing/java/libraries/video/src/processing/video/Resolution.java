/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011 Andres Colubri

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.video;

import processing.core.PApplet;

/**
 * Utility class to store the resolution (width, height and fps) of a capture
 * device.
 * 
 */
public class Resolution {
  public int width, height;
  public float fps;
  public String fpsString;
  
  public Resolution() {
    width = height = 0;
    fps = 0.0f;
    fpsString = "";
  }
  
  public Resolution(int width, int height, int fpsDenominator, int fpsNumerator) {
    this.width = width;
    this.height = height;
    this.fps = (float)fpsDenominator / (float)fpsNumerator;
    this.fpsString = fpsDenominator + "/" + fpsNumerator;
  }
  
  public Resolution(int width, int height, String fpsString) {
    this.width = width;
    this.height = height;
    
    String[] parts = fpsString.split("/");
    if (parts.length == 2) {      
      int fpsDenominator = PApplet.parseInt(parts[0]);
      int fpsNumerator = PApplet.parseInt(parts[1]);
    
      this.fps = (float)fpsDenominator / (float)fpsNumerator;
      this.fpsString = fpsString;
    } else {
      this.fps = 0.0f;
      this.fpsString = "";
    }
  }
  
  public Resolution(Resolution source) {
    this.width = source.width;
    this.height = source.height;
    this.fps = source.fps;
    this.fpsString = source.fpsString;
  }  
  
  public String toString() {
    return width + "x" + height + ", " + PApplet.nfc(fps, 2) + " fps (" + fpsString +")";    
  }
}