/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2015 The Processing Foundation

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core;


/**
 * Experimental/enhanced renderer that draws directly to the Graphics context
 * without an intermediate image. This greatly speeds up performance
 * (especially on hidpi displays), but prevents pixel access. It also causes
 * some rendering hiccups (frame rate is not as smooth), but that's why it's
 * experimental.
 *
 * Works with both normal and hidpi, without need for an extra E2D_2X.
 *
 * This is not the final class name, and it's not clear in what form (or if)
 * this will ship with 3.0.
 */
public class PGraphicsDanger2D extends PGraphicsJava2D {
  // doesn't exist/not necessary because Java2D will do this automatically
  //static final boolean HIDPI = true;
  //static final boolean HIDPI = false;


//  public PGraphicsDanger2D() {
//    if (HIDPI) {
//      pixelFactor = 2;
//    }
//  }


  @Override
  public PSurface createSurface() {
    return surface = new PSurfaceDanger(this);
  }


  @Override
  public void beginDraw() {
    //g2 = checkImage();  // already set g2

//    if (HIDPI) {
//      g2.scale(2, 2);
//    }

    // Calling getGraphics() seems to nuke the smoothing settings
    smooth(quality);

    checkSettings();
    resetMatrix(); // reset model matrix
    vertexCount = 0;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  @Override
  public void loadPixels() {
    nope("loadPixels");
  }

  @Override
  public void updatePixels() {
    nope("updatePixels");
  }

  @Override
  public void updatePixels(int x, int y, int c, int d) {
    nope("updatePixels");
  }

  //

  @Override
  public int get(int x, int y) {
    nope("get");
    return 0;  // not reached
  }

  @Override
  public PImage get(int x, int y, int c, int d) {
    nope("get");
    return null;  // not reached
  }

  @Override
  public PImage get() {
    nope("get");
    return null;  // not reached
  }

  @Override
  public void set(int x, int y, int argb) {
    nope("set");
  }

  @Override
  public void set(int x, int y, PImage image) {
    nope("set");
  }

  //

  @Override
  public void mask(int alpha[]) {
    nope("mask");
  }

  @Override
  public void mask(PImage alpha) {
    nope("mask");
  }

  //

  @Override
  public void filter(int kind) {
    nope("filter");
  }

  @Override
  public void filter(int kind, float param) {
    nope("filter");
  }

  //

  @Override
  public void copy(int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    nope("copy");
  }

  @Override
  public void copy(PImage src,
                   int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    nope("copy");
  }

  //

  public void blend(int sx, int sy, int dx, int dy, int mode) {
    nope("blend");
  }

  public void blend(PImage src,
                    int sx, int sy, int dx, int dy, int mode) {
    nope("blend");
  }

  @Override
  public void blend(int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    nope("blend");
  }

  @Override
  public void blend(PImage src,
                    int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    nope("blend");
  }

  //

  @Override
  public boolean save(String filename) {
    nope("save");
    return false;
  }

  //

  protected void nope(String function) {
    throw new RuntimeException(function + "() is unavailable with E2D");
  }
}