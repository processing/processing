/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2006 Ben Fry and Casey Reas

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

package processing.core;

import java.awt.Toolkit;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;

//import java.awt.*;
//import java.awt.image.*;


/**
 * Subclass of PGraphics that handles fast 2D rendering, 
 * more commonly referred to as P2D. This class uses no Java2D 
 * and will run with Java 1.1.
 */
public class PGraphics2D extends PGraphics {

  
  protected PGraphics2D() { }
  
  
  public PGraphics2D(int iwidth, int iheight) {
    this(iwidth, iheight, null);
  }

  
  public PGraphics2D(int iwidth, int iheight, PApplet applet) {
    if (applet != null) {
      this.parent = applet;
      applet.addListeners();
    }
    resize(iwidth, iheight);
  }

  
  //resize handled by superclass
  
  
  //requestDisplay handled by superclass
  
  
  protected void allocate() {
    pixelCount = width * height;
    pixels = new int[pixelCount];

    // because of a java 1.1 bug, pixels must be registered as
    // opaque before their first run, the memimgsrc will flicker
    // and run very slowly.
    backgroundColor |= 0xff000000;  // just for good measure
    for (int i = 0; i < pixelCount; i++) pixels[i] = backgroundColor;
    //for (int i = 0; i < pixelCount; i++) pixels[i] = 0xffffffff;

    if (parent != null) {
      cm = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff);;
      mis = new MemoryImageSource(width, height, pixels, 0, width);
      mis.setFullBufferUpdates(true);
      mis.setAnimated(true);
      image = Toolkit.getDefaultToolkit().createImage(mis);
    }
  }

  
  public void beginDraw() {
    // need to call defaults(), but can only be done when it's ok
    // to draw (i.e. for opengl, no drawing can be done outside
    // beginDraw/endDraw).
    if (!defaultsInited) defaults();
    
    resetMatrix(); // reset model matrix
    
    // reset vertices
    vertexCount = 0;
  }
  
  
  public void endDraw() {
    // moving this back here (post-68) because of macosx thread problem
    if (mis != null) {
      mis.newPixels(pixels, cm, 0, width);
    }
    // mark pixels as having been updated, so that they'll work properly
    // when this PGraphics is drawn using image().
    endPixels();
  }
}