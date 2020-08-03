/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  SizeInfo - parsed elements of a size() or fullScreen() call
  Part of the Processing project - http://processing.org

  Copyright (c) 2015 The Processing Foundation

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

package processing.mode.java.preproc;

import processing.app.Messages;
import processing.core.PApplet;
import processing.data.StringList;


public class SurfaceInfo {
  StringList statements = new StringList();

  String width;
  String height;
  String renderer;
  String path;

  String display;
  /** null for nothing in setup(), 0 for noSmooth(), N for smooth(N) */
  //Integer quality;
//  String smooth;


  boolean hasOldSyntax() {
    if (width.equals("screenWidth") ||
        width.equals("screenHeight") ||
        height.equals("screenHeight") ||
        height.equals("screenWidth")) {
      final String message =
        "The screenWidth and screenHeight variables are named\n" +
        "displayWidth and displayHeight in Processing 3.\n" +
        "Or you can use the fullScreen() method instead of size().";
      Messages.showWarning("Time for a quick update", message, null);
      return true;
    }
    if (width.equals("screen.width") ||
        width.equals("screen.height") ||
        height.equals("screen.height") ||
        height.equals("screen.width")) {
      final String message =
        "The screen.width and screen.height variables are named\n" +
        "displayWidth and displayHeight in Processing 3.\n" +
        "Or you can use the fullScreen() method instead of size().";
      Messages.showWarning("Time for a quick update", message, null);
      return true;
    }
    return false;
  }


  boolean hasBadSize() {
    if (!width.equals("displayWidth") &&
        !width.equals("displayHeight") &&
        PApplet.parseInt(width, -1) == -1) {
      return true;
    }
    if (!height.equals("displayWidth") &&
        !height.equals("displayHeight") &&
        PApplet.parseInt(height, -1) == -1) {
      return true;
    }
    return false;
  }


  void checkEmpty() {
    if (renderer != null) {
      if (renderer.length() == 0) {  // if empty, set null
        renderer = null;
      }
    }
    if (path != null) {
      if (path.length() == 0) {
        path = null;
      }
    }
    if (display != null) {
      if (display.length() == 0) {
        display = null;
      }
    }
  }


//  public String getStatements() {
//    return statements.join(" ");
//  }


  public StringList getStatements() {
    return statements;
  }


  /**
   * Add an item that will be moved from size() into the settings() method.
   * This needs to be the exact version of the statement so that it can be
   * matched against and removed from the size() method in the code.
   */
  public void addStatement(String stmt) {
    statements.append(stmt);
  }


  public void addStatements(StringList list) {
    statements.append(list);
  }


  /** @return true if there's code to be inserted for a settings() method. */
  public boolean hasSettings() {
    return statements.size() != 0;
  }


  /** @return the contents of the settings() method to be inserted */
  public String getSettings() {
    return statements.join(" ");
  }


  // Added for Android Mode to check whether OpenGL is in use
  // https://github.com/processing/processing/issues/4441
  /**
   * Return the renderer specified (null if none specified).
   * @since 3.2.2
   */
  public String getRenderer() {
    return renderer;
  }
}
