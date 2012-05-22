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
import processing.core.PGraphics;

public class PShape2D extends PShapeOpenGL {
  
  public PShape2D(PApplet parent, int family) {
    super(parent, family);
  }  
  
  public boolean is2D() {
    return true;
  }

  public boolean is3D() {
    return false;
  }
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Drawing methods  
  
  public void vertex(float x, float y, float z) {
    PGraphics.showDepthWarningXYZ("vertex");
  }
  
  public void vertex(float x, float y, float z, float u, float v) {
    PGraphics.showDepthWarningXYZ("vertex");
  }  
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Bezier curves   
  
  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    PGraphics.showDepthWarningXYZ("bezierVertex");
  }

  public void quadraticVertex(float x2, float y2, float z2,
                         float x4, float y4, float z4) {
    PGraphics.showDepthWarningXYZ("quadVertex");
  }  

  public void curveVertex(float x, float y, float z) {
    PGraphics.showDepthWarningXYZ("curveVertex");
  }    
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Geometric transformations  
  
  public void translate(float tx, float ty, float tz) {
    PGraphics.showVariationWarning("translate");
  }  
  
  public void rotateX(float angle) {
    PGraphics.showDepthWarning("rotateX");
  }

  public void rotateY(float angle) {
    PGraphics.showDepthWarning("rotateY");
  }

  public void rotateZ(float angle) {
    PGraphics.showDepthWarning("rotateZ");
  }

  public void rotate(float angle, float vx, float vy, float vz) {
    PGraphics.showVariationWarning("rotate");
  }  
  
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    PGraphics.showVariationWarning("applyMatrix");
  }  
  
  public void scale(float sx, float sy, float sz) {
    PGraphics.showDepthWarningXYZ("scale");
  }
  
  ///////////////////////////////////////////////////////////  
  
  //
  
  // Setters/getters of individual vertices  

  public float getVertexZ(int index) {
    PGraphics.showDepthWarningXYZ("getVertexZ");
    return 0;
  }  
  
  public void setVertex(int index, float x, float y) {
    super.setVertex(index, x, y, 0);
  }
  
  public void setVertex(int index, float x, float y, float z) {
    PGraphics.showDepthWarningXYZ("setVertex");
  }
}
