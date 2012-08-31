/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012 Ben Fry and Casey Reas

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

package processing.opengl;

import processing.core.PApplet;
import processing.core.PShape;

public class PShape3D extends PShapeOpenGL {

  public PShape3D(PApplet parent, int family) {
    super(parent, family);
  }
  
  public boolean is2D() {
    return false;
  }

  public boolean is3D() {
    return true;
  }
 
  
  ////////////////////////////////////////////////////////////////////////
  //
  // Shape copy  
  
  
  static public PShape3D createShape(PApplet parent, PShape src) {
    PShape3D dest = null;
    if (src.getFamily() == GROUP) {
      dest = PGraphics3D.createShapeImpl(parent, GROUP);
      PShape3D.copyGroup(parent, src, dest);      
    } else if (src.getFamily() == PRIMITIVE) {
      dest = PGraphics3D.createShapeImpl(parent, src.getKind(), 
                                         src.getParams());
      PShape.copyPrimitive(src, dest);
    } else if (src.getFamily() == GEOMETRY) {
      dest = PGraphics3D.createShapeImpl(parent, src.getKind());
      PShape.copyGeometry(src, dest);
    } else if (src.getFamily() == PATH) {
      dest = PGraphics3D.createShapeImpl(parent, PATH);
      PShape.copyPath(src, dest);
    }
    dest.setName(src.getName());
    return dest;
  }  
  
  
  static public void copyGroup(PApplet parent, PShape src, PShape dest) {
    copyMatrix(src, dest);
    copyStyles(src, dest);
    copyImage(src, dest);
        
    for (int i = 0; i < src.getChildCount(); i++) {
      PShape c = PShape3D.createShape(parent, src.getChild(i));
      dest.addChild(c);
    }
  } 
}
