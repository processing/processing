/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-13 Ben Fry and Casey Reas

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

package processing.jogl;

import processing.core.*;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

/**
 * LWJGL renderer.
 *
 */
public class PGraphicsJOGL extends PGraphicsOpenGL {
  protected PGL createPGL(PGraphicsOpenGL pg) {
    return new PJOGL(pg);
  }  
  
  @Override
  public PSurface createSurface() {  // ignore
    return new PSurfaceJOGL(this);
  }  
}
