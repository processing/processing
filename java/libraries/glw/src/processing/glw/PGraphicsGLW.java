/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-12 Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
 */

package processing.glw;

import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

/**
 * GLW renderer. It's only role is to drive the main animation loop by calling 
 * requestDraw() and so allowing the offscreen canvases to be drawn inside the
 * draw() method of the sketch. Currently, it cannot be used to draw into.
 *
 */
public class PGraphicsGLW extends PGraphicsOpenGL {
  protected PGL createPGL(PGraphicsOpenGL pg) {
    return new PNEWT(pg);
  }
  
  public void beginDraw() {
    if (primarySurface) {
      setCurrentPG(this);  
    } else {
      throw new RuntimeException("GLW renderer cannot be used as an offscreen surface");
    }
    
    report("top beginDraw()");

    if (!checkGLThread()) {
      return;
    }
    
    if (drawing) {
      return;
    }
    
    if (!glParamsRead) {
      getGLParameters();
    }
    
    drawing = true;

    report("bot beginDraw()"); 
  }
  
  public void endDraw() {
    report("top endDraw()");

    if (!drawing) {
      return;
    }
    
    if (primarySurface) {
      setCurrentPG(null);
    } else {
      throw new RuntimeException("GLW renderer cannot be used as an offscreen surface.");
    }    
    drawing = false;

    report("bot endDraw()");    
  }
  
  protected void vertexImpl(float x, float y, float z, float u, float v) {
    throw new RuntimeException("The main GLW renderer cannot be used to draw to.");
  }
}
