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
import java.awt.Polygon;
import java.awt.geom.AffineTransform;


public class HProgressBar {
  int x, y, size, width;
  int pos;
  int lPolyX, rPolyX;
  Polygon rightPoly, leftPoly;


  public HProgressBar(int size, int width) {
    this.size = size;
    this.width = width;
    x = 0;
    y = 0;
    setPos(0);

    int xl[] = {0, 0, -(int)(size/1.5)};
    int yl[] = {-(int)((float)size/3), (int)((float)size/3), 0};
    leftPoly = new Polygon(xl, yl, 3);
    int xr[] = {0, (int)(size/1.5), 0};
    int yr[] = {-(int)((float)size/3), 0, (int)((float)size/3)};
    rightPoly = new Polygon(xr, yr, 3);
  }


  public void setPos(int pos) {
    this.pos = pos;
    lPolyX = 0;
    rPolyX = 0;

    if (pos > 0) {
      rPolyX = pos;
    }
    else if (pos < 0) {
      lPolyX = pos;
    }
  }


  public void setWidth(int width) {
    this.width = width;
  }


  public void draw(Graphics2D g2d) {
    AffineTransform trans = g2d.getTransform();
    g2d.translate(x, y);

    // draw white cover on text line
    g2d.setColor(ColorScheme.getInstance().whitePaneColor);
    g2d.fillRect(-200+lPolyX, -size, 200-lPolyX-width/2, size+1);
    g2d.fillRect(width/2, -size, 200+rPolyX, size+1);

    // draw left and right triangles and leading line
    g2d.setColor(ColorScheme.getInstance().progressFillColor);
    AffineTransform tmp = g2d.getTransform();
    g2d.translate(-width/2 - 5 + lPolyX, -size/2);
    g2d.fillRect(0, -1, -lPolyX, 2);
    g2d.fillPolygon(leftPoly);
    g2d.setTransform(tmp);
    g2d.translate(width/2 + 5 + rPolyX, -size/2);
    g2d.fillRect(-rPolyX, -1, rPolyX+1, 2);
    g2d.fillPolygon(rightPoly);
    g2d.setTransform(tmp);
    g2d.setTransform(trans);
  }
}
