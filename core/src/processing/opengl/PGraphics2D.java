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

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.core.PShapeSVG;
import processing.data.XML;

public class PGraphics2D extends PGraphicsOpenGL {

  public PGraphics2D() {
    super();
    hints[ENABLE_PERSPECTIVE_CORRECTED_STROKE] = false;
  }


  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES


  public boolean is2D() {
    return true;
  }


  public boolean is3D() {
    return false;
  }


  //////////////////////////////////////////////////////////////

  // HINTS


  public void hint(int which) {
    if (which == ENABLE_PERSPECTIVE_CORRECTED_STROKE) {
      showWarning("2D lines cannot be perspective-corrected.");
      return;
    }
    super.hint(which);
  }


  //////////////////////////////////////////////////////////////

  // PROJECTION


  public void ortho() {
    showMethodWarning("ortho");
  }


  public void ortho(float left, float right,
                    float bottom, float top) {
    showMethodWarning("ortho");
  }


  public void ortho(float left, float right,
                    float bottom, float top,
                    float near, float far) {
    showMethodWarning("ortho");
  }


  public void perspective() {
    showMethodWarning("perspective");
  }


  public void perspective(float fov, float aspect, float zNear, float zFar) {
    showMethodWarning("perspective");
  }


  public void frustum(float left, float right, float bottom, float top,
                      float znear, float zfar) {
    showMethodWarning("frustum");
  }


  protected void defaultPerspective() {
    super.ortho(-width/2, +width/2, -height/2, +height/2, -1, +1);
  }


  //////////////////////////////////////////////////////////////

  // CAMERA


  public void beginCamera() {
    showMethodWarning("beginCamera");
  }


  public void endCamera() {
    showMethodWarning("endCamera");
  }


  public void camera() {
    showMethodWarning("camera");
  }


  public void camera(float eyeX, float eyeY, float eyeZ,
                     float centerX, float centerY, float centerZ,
                     float upX, float upY, float upZ) {
    showMethodWarning("camera");
  }


  protected void defaultCamera() {
    super.camera(width/2, height/2);
  }


  //////////////////////////////////////////////////////////////

  // MATRIX MORE!


  protected void begin2D() {
    pushProjection();
    defaultPerspective();
    pushMatrix();
    defaultCamera();
  }


  protected void end2D() {
    popMatrix();
    popProjection();
  }


  //////////////////////////////////////////////////////////////

  // SHAPE


  public void shape(PShape shape) {
    if (shape.is2D()) {
      super.shape(shape);
    } else {
      showWarning("The shape object is not 2D, cannot be displayed with this renderer");
    }
  }


  public void shape(PShape shape, float x, float y) {
    if (shape.is2D()) {
      super.shape(shape, x, y);
    } else {
      showWarning("The shape object is not 2D, cannot be displayed with this renderer");
    }
  }


  public void shape(PShape shape, float a, float b, float c, float d) {
    if (shape.is2D()) {
      super.shape(shape, a, b, c, d);
    } else {
      showWarning("The shape object is not 2D, cannot be displayed with this renderer");
    }
  }


  public void shape(PShape shape, float x, float y, float z) {
    showDepthWarningXYZ("shape");
  }


  public void shape(PShape shape, float x, float y, float z, float c, float d, float e) {
    showDepthWarningXYZ("shape");
  }


  //////////////////////////////////////////////////////////////

  // SHAPE I/O


  static protected boolean isSupportedExtension(String extension) {
    return extension.equals("svg") || extension.equals("svgz");
  }


  static protected PShape2D loadShapeImpl(PGraphics pg, String filename, String extension) {
    PShapeSVG svg = null;

    if (extension.equals("svg")) {
      svg = new PShapeSVG(pg.parent, filename);

    } else if (extension.equals("svgz")) {
      try {
        InputStream input = new GZIPInputStream(pg.parent.createInput(filename));
        XML xml = new XML(PApplet.createReader(input));
        svg = new PShapeSVG(xml);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (svg != null) {
      PShape2D p2d = PShape2D.createShape(pg.parent, svg);
      return p2d;
    } else {
      return null;
    }
  }


  //////////////////////////////////////////////////////////////

  // SHAPE CREATION


  public PShape createShape(PShape source) {
    return PShape2D.createShape(parent, source);
  }


  public PShape createShape() {
    return createShape(POLYGON);
  }


  public PShape createShape(int type) {
    return createShapeImpl(parent, type);
  }


  public PShape createShape(int kind, float... p) {
    return createShapeImpl(parent, kind, p);
  }


  static protected PShape2D createShapeImpl(PApplet parent, int type) {
    PShape2D shape = null;
    if (type == PShape.GROUP) {
      shape = new PShape2D(parent, PShape.GROUP);
    } else if (type == PShape.PATH) {
      shape = new PShape2D(parent, PShape.PATH);
    } else if (type == POINTS) {
      shape = new PShape2D(parent, PShape.GEOMETRY);
      shape.setKind(POINTS);
    } else if (type == LINES) {
      shape = new PShape2D(parent, PShape.GEOMETRY);
      shape.setKind(LINES);
    } else if (type == TRIANGLE || type == TRIANGLES) {
      shape = new PShape2D(parent, PShape.GEOMETRY);
      shape.setKind(TRIANGLES);
    } else if (type == TRIANGLE_FAN) {
      shape = new PShape2D(parent, PShape.GEOMETRY);
      shape.setKind(TRIANGLE_FAN);
    } else if (type == TRIANGLE_STRIP) {
      shape = new PShape2D(parent, PShape.GEOMETRY);
      shape.setKind(TRIANGLE_STRIP);
    } else if (type == QUAD || type == QUADS) {
      shape = new PShape2D(parent, PShape.GEOMETRY);
      shape.setKind(QUADS);
    } else if (type == QUAD_STRIP) {
      shape = new PShape2D(parent, PShape.GEOMETRY);
      shape.setKind(QUAD_STRIP);
    } else if (type == POLYGON) {
      shape = new PShape2D(parent, PShape.GEOMETRY);
      shape.setKind(POLYGON);
    }
    return shape;
  }


  static protected PShape2D createShapeImpl(PApplet parent, int kind, float... p) {
    PShape2D shape = null;
    int len = p.length;

    if (kind == POINT) {
      if (len != 2) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape2D(parent, PShape.PRIMITIVE);
      shape.setKind(POINT);
    } else if (kind == LINE) {
      if (len != 4) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape2D(parent, PShape.PRIMITIVE);
      shape.setKind(LINE);
    } else if (kind == TRIANGLE) {
      if (len != 6) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape2D(parent, PShape.PRIMITIVE);
      shape.setKind(TRIANGLE);
    } else if (kind == QUAD) {
      if (len != 8) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape2D(parent, PShape.PRIMITIVE);
      shape.setKind(QUAD);
    } else if (kind == RECT) {
      if (len != 4 && len != 5 && len != 8) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape2D(parent, PShape.PRIMITIVE);
      shape.setKind(RECT);
    } else if (kind == ELLIPSE) {
      if (len != 4) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape2D(parent, PShape.PRIMITIVE);
      shape.setKind(ELLIPSE);
    } else if (kind == ARC) {
      if (len != 6) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShape2D(parent, PShape.PRIMITIVE);
      shape.setKind(ARC);
    } else if (kind == BOX) {
      showWarning("Primitive not supported in 2D");
    } else if (kind == SPHERE) {
      showWarning("Primitive not supported in 2D");
    } else {
      showWarning("Unrecognized primitive type");
    }

    if (shape != null) {
      shape.setParams(p);
    }

    return shape;
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

  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS

  public void translate(float tx, float ty, float tz) {
    showDepthWarningXYZ("translate");
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

  public void applyMatrix(PMatrix3D source) {
    showVariationWarning("applyMatrix");
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

  //////////////////////////////////////////////////////////////

  // SCREEN AND MODEL COORDS

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
}