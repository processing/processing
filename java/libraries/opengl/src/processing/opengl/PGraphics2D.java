/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012 Ben Fry and Casey Reas

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

package processing.opengl;

import processing.core.PMatrix3D;

public class PGraphics2D extends PGraphicsOpenGL {
  
  public PGraphics2D() {
    super();
    hints[ENABLE_PERSPECTIVE_CORRECTED_LINES] = false;
  }
  
  
  //////////////////////////////////////////////////////////////

  // BEZIER VERTICES

  
  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    showDepthWarningXYZ("bezierVertex");
  }
  
  
  //////////////////////////////////////////////////////////////

  // QUADRATIC BEZIER VERTICES


  public void quadraticVertex(float x2, float y2, float z2,
                         float x4, float y4, float z4) {
    showDepthWarningXYZ("quadVertex");
  }  
  
  
  //////////////////////////////////////////////////////////////

  // CURVE VERTICES  
  
  
  public void curveVertex(float x, float y, float z) {
    showDepthWarningXYZ("curveVertex");
  }  
  
  
  //////////////////////////////////////////////////////////////

  // BOX


  public void box(float w, float h, float d) {
    showMethodWarning("box");
  }  

  
  //////////////////////////////////////////////////////////////

  // SPHERE


  public void sphere(float r) {
    showMethodWarning("sphere");
  }  
  
  
  //////////////////////////////////////////////////////////////

  // VERTEX SHAPES
  
  
  public void vertex(float x, float y, float z) {
    showDepthWarningXYZ("vertex");
  }
  
  public void vertex(float x, float y, float z, float u, float v) {
    showDepthWarningXYZ("vertex");
  }  
  
  public void rotateX(float angle) {
    showDepthWarning("rotateX");
  }

  public void rotateY(float angle) {
    showDepthWarning("rotateY");
  }

  public void rotateZ(float angle) {
    showDepthWarning("rotateZ");
  }

  public void rotate(float angle, float vx, float vy, float vz) {
    showVariationWarning("rotate");
  }  
  
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    showVariationWarning("applyMatrix");
  }  
  
  public void scale(float sx, float sy, float sz) {
    showDepthWarningXYZ("scale");
  }
  
  public float screenX(float x, float y, float z) {
    showDepthWarningXYZ("screenX");
    return 0;
  }

  public float screenY(float x, float y, float z) {
    showDepthWarningXYZ("screenY");
    return 0;
  }

  public float screenZ(float x, float y, float z) {
    showDepthWarningXYZ("screenZ");
    return 0;
  }  
  
  public PMatrix3D getMatrix(PMatrix3D target) {
    showVariationWarning("getMatrix");
    return target;
  }
  
  public void setMatrix(PMatrix3D source) {
    showVariationWarning("setMatrix");
  }
  
  
  //////////////////////////////////////////////////////////////  

  // LIGHTS

  public void lights() {
    showMethodWarning("lights");
  }

  public void noLights() {
    showMethodWarning("noLights");
  }

  public void ambientLight(float red, float green, float blue) {
    showMethodWarning("ambientLight");
  }

  public void ambientLight(float red, float green, float blue,
                           float x, float y, float z) {
    showMethodWarning("ambientLight");
  }

  public void directionalLight(float red, float green, float blue,
                               float nx, float ny, float nz) {
    showMethodWarning("directionalLight");
  }

  public void pointLight(float red, float green, float blue,
                         float x, float y, float z) {
    showMethodWarning("pointLight");
  }

  public void spotLight(float red, float green, float blue,
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float angle, float concentration) {
    showMethodWarning("spotLight");
  }

  public void lightFalloff(float constant, float linear, float quadratic) {
    showMethodWarning("lightFalloff");
  }

  public void lightSpecular(float v1, float v2, float v3) {
    showMethodWarning("lightSpecular");
  }

  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES
  
  /**
   * Return true if this renderer supports 2D drawing. Defaults to true.
   */
  public boolean is2D() {
    return true;
  }


  /**
   * Return true if this renderer supports 2D drawing. Defaults to false.
   */
  public boolean is3D() {
    return false;
  }  
  
}