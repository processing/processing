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

import java.awt.Color;


public class ColorScheme {
  private static ColorScheme instance = null;
  public Color redStrokeColor;
  public Color progressFillColor;
  public Color progressEmptyColor;
  public Color markerColor;
  public Color whitePaneColor;


  private ColorScheme() {
    redStrokeColor = new Color(160, 20, 20);    // dark red
    progressEmptyColor = new Color(180, 180, 180, 200);
    progressFillColor = new Color(0, 0, 0, 200);
    markerColor = new Color(228, 200, 91, 127);
    whitePaneColor = new Color(255, 255, 255, 120);
  }


  public static ColorScheme getInstance() {
    if (instance == null) {
      instance = new ColorScheme();
    }
    return instance;
  }
}
