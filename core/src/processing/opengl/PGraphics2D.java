/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2018 The Processing Foundation

  Based on code originally written by Miles Fogle:
  https://github.com/processing/processing/issues/5256

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

import static processing.core.PApplet.println;

import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PMatrix2D;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.core.PShapeSVG;

//TODO: track debug performance stats
public final class PGraphics2D extends PGraphicsOpenGL {
  static final String NON_2D_SHAPE_ERROR = "The shape object is not 2D, cannot be displayed with this renderer";
  static final String STROKE_PERSPECTIVE_ERROR = "Strokes cannot be perspective-corrected in 2D";

  static final String NON_2D_SHADER_ERROR = "This shader cannot be used for 2D rendering";
  static final String WRONG_SHADER_PARAMS = "The P2D renderer does not accept shaders of different tyes";

  static protected final int SHADER2D = 7;

  // Enables/disables matrix pre-multiplication
  // https://github.com/processing/processing/wiki/Advanced-OpenGL#vertex-coordinates-are-in-model-space
  // https://github.com/processing/processing/issues/2904
  // see above URLs for some discussion on premultiplying matrix vs. flushing buffer on matrix change.
  // rather than committing to one or the other, this implementation supports both
  public static boolean premultiplyMatrices = true;

  // Uses the implementations in the parent PGraphicsOpenGL class, which is needed to to draw obj files
  // and apply shader filters.
  protected boolean useParentImpl = false;

  protected boolean initialized;

  protected PGL.Tessellator tess;

  protected PShader twoShader;
  protected PShader defTwoShader;

  protected int positionLoc;
  protected int colorLoc;
  protected int texCoordLoc;
  protected int texFactorLoc;

  protected int transformLoc;
  protected int texScaleLoc;

  static protected URL defP2DShaderVertURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/2DVert.glsl");
  static protected URL defP2DShaderFragURL =
    PGraphicsOpenGL.class.getResource("/processing/opengl/shaders/2DFrag.glsl");


  public PGraphics2D() {
    super();
    initTess();
    initVerts();
  }


  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES


  @Override
  public boolean is2D() {
    return true;
  }


  @Override
  public boolean is3D() {
    return false;
  }


  //////////////////////////////////////////////////////////////

  // RENDERING


  @Override
  public void beginDraw() {
    super.beginDraw();
    if (!useParentImpl) {
      pgl.depthFunc(PGL.LESS);
      depth = 1.0f;
    }
  }


  @Override
  public void flush() {
    // If no vertices where created with the base implementation, then flush() will do nothing.
    super.flush();
    flushBuffer();
  }


  // These two methods are meant for debugging (comparing the new and old P2D renderers)
  // and will go away.


  public void useOldP2D() {
    useParentImpl = true;
    pgl.depthFunc(PGL.LEQUAL);
  }


  public void useNewP2D() {
    useParentImpl = false;
    pgl.depthFunc(PGL.LESS);
  }


  //////////////////////////////////////////////////////////////

  // HINTS


  @Override
  public void hint(int which) {
    if (which == ENABLE_STROKE_PERSPECTIVE) {
      showWarning(STROKE_PERSPECTIVE_ERROR);
      return;
    }
    super.hint(which);
  }


  //////////////////////////////////////////////////////////////

  // PROJECTION


  @Override
  public void ortho() {
    showMethodWarning("ortho");
  }


  @Override
  public void ortho(float left, float right,
                    float bottom, float top) {
    showMethodWarning("ortho");
  }


  @Override
  public void ortho(float left, float right,
                    float bottom, float top,
                    float near, float far) {
    showMethodWarning("ortho");
  }


  @Override
  public void perspective() {
    showMethodWarning("perspective");
  }


  @Override
  public void perspective(float fov, float aspect, float zNear, float zFar) {
    showMethodWarning("perspective");
  }


  @Override
  public void frustum(float left, float right, float bottom, float top,
                      float znear, float zfar) {
    showMethodWarning("frustum");
  }


  @Override
  protected void defaultPerspective() {
    super.ortho(0, width, -height, 0, -1, +1);
  }


  //////////////////////////////////////////////////////////////

  // CAMERA


  @Override
  public void beginCamera() {
    showMethodWarning("beginCamera");
  }


  @Override
  public void endCamera() {
    showMethodWarning("endCamera");
  }


  @Override
  public void camera() {
    showMethodWarning("camera");
  }


  @Override
  public void camera(float eyeX, float eyeY, float eyeZ,
                     float centerX, float centerY, float centerZ,
                     float upX, float upY, float upZ) {
    showMethodWarning("camera");
  }


  @Override
  protected void defaultCamera() {
    eyeDist = 1;
    resetMatrix();
  }


  //////////////////////////////////////////////////////////////

  // SHAPE


  @Override
  public void shape(PShape shape) {
    if (shape.is2D()) {
      if (!useParentImpl) {
        useOldP2D();
        super.shape(shape);
        useNewP2D();
      } else {
        super.shape(shape);
      }
    } else {
      showWarning(NON_2D_SHAPE_ERROR);
    }
  }


  @Override
  public void shape(PShape shape, float x, float y) {
    if (shape.is2D()) {
      if (!useParentImpl) {
        useOldP2D();
        super.shape(shape, x, y);
        useNewP2D();
      } else {
        super.shape(shape, x, y);
      }
    } else {
      showWarning(NON_2D_SHAPE_ERROR);
    }
  }


  @Override
  public void shape(PShape shape, float a, float b, float c, float d) {
    if (shape.is2D()) {
      if (!useParentImpl) {
        useOldP2D();
        super.shape(shape, a, b, c, d);
        useNewP2D();
      } else {
        super.shape(shape, a, b, c, d);
      }
    } else {
      showWarning(NON_2D_SHAPE_ERROR);
    }
  }


  @Override
  public void shape(PShape shape, float x, float y, float z) {
    showDepthWarningXYZ("shape");
  }


  @Override
  public void shape(PShape shape, float x, float y, float z,
                    float c, float d, float e) {
    showDepthWarningXYZ("shape");
  }



  //////////////////////////////////////////////////////////////

  // SHAPE I/O


  static protected boolean isSupportedExtension(String extension) {
    return extension.equals("svg") || extension.equals("svgz");
  }


  static protected PShape loadShapeImpl(PGraphics pg,
                                        String filename, String extension) {
    if (extension.equals("svg") || extension.equals("svgz")) {
      PShapeSVG svg = new PShapeSVG(pg.parent.loadXML(filename));
      return PShapeOpenGL.createShape((PGraphicsOpenGL) pg, svg);
    }
    return null;
  }


  //////////////////////////////////////////////////////////////

  // SCREEN TRANSFORMS


  @Override
  public float modelX(float x, float y, float z) {
    showDepthWarning("modelX");
    return 0;
  }


  @Override
  public float modelY(float x, float y, float z) {
    showDepthWarning("modelY");
    return 0;
  }


  @Override
  public float modelZ(float x, float y, float z) {
    showDepthWarning("modelZ");
    return 0;
  }


  //////////////////////////////////////////////////////////////

  // VERTEX SHAPES


  @Override
  public void texture(PImage image) {
    super.texture(image);

    if (image == null) {
      return;
    }

    init();

    Texture t = currentPG.getTexture(image);
    texWidth = t.width;
    texHeight = t.height;
    imageTex = t.glName;
    textureImpl(imageTex);
  }


  @Override
  public void beginShape(int kind) {
    if (useParentImpl) {
      super.beginShape(kind);
      return;
    }

    shapeType = kind;
    vertCount = 0;
    contourCount = 0;
  }


  @Override
  public void endShape(int mode) {
    if (useParentImpl) {
      super.endShape(mode);
      return;
    }

    //end the current contour
    appendContour(vertCount);

    if (fill) {
      incrementDepth();

      if (shapeType == POLYGON) {
        if (knownConvexPolygon) {
          for (int i = 2; i < vertCount; ++i) {
            check(3);
            vertexImpl(shapeVerts[0]);
            vertexImpl(shapeVerts[i - 1]);
            vertexImpl(shapeVerts[i]);
          }

          knownConvexPolygon = false;
        } else {
          tess.beginPolygon(this);
          tess.beginContour();

          int c = 0;
          for (int i = 0; i < vertCount; ++i) {
            if (contours[c] == i) {
              tess.endContour();
              tess.beginContour();
              c++; //lol no, this is java
            }

            tempDoubles[0] = shapeVerts[i].x;
            tempDoubles[1] = shapeVerts[i].y;
            tess.addVertex(tempDoubles, 0, shapeVerts[i]);
          }
          tess.endContour();
          tess.endPolygon();
        }
      } else if (shapeType == QUAD_STRIP) {
        for (int i = 0; i <= vertCount - 4; i += 2) {
          check(6);
          vertexImpl(shapeVerts[i + 0]);
          vertexImpl(shapeVerts[i + 1]);
          vertexImpl(shapeVerts[i + 2]);
          vertexImpl(shapeVerts[i + 1]);
          vertexImpl(shapeVerts[i + 2]);
          vertexImpl(shapeVerts[i + 3]);
        }
      } else if (shapeType == QUADS) {
        for (int i = 0; i <= vertCount - 4; i += 4) {
          check(6);
          vertexImpl(shapeVerts[i + 0]);
          vertexImpl(shapeVerts[i + 1]);
          vertexImpl(shapeVerts[i + 2]);
          vertexImpl(shapeVerts[i + 0]);
          vertexImpl(shapeVerts[i + 2]);
          vertexImpl(shapeVerts[i + 3]);
        }
      } else if (shapeType == TRIANGLE_STRIP) {
        for (int i = 0; i <= vertCount - 3; i += 1) {
          check(3);
          vertexImpl(shapeVerts[i + 0]);
          vertexImpl(shapeVerts[i + 1]);
          vertexImpl(shapeVerts[i + 2]);
        }
      } else if (shapeType == TRIANGLE_FAN) {
        for (int i = 0; i <= vertCount - 3; i += 1) {
          check(3);
          vertexImpl(shapeVerts[0 + 0]);
          vertexImpl(shapeVerts[i + 1]);
          vertexImpl(shapeVerts[i + 2]);
        }

        //close the fan
        if (vertCount >= 3) {
          check(3);
          vertexImpl(shapeVerts[0]);
          vertexImpl(shapeVerts[vertCount - 1]);
          vertexImpl(shapeVerts[1]);
        }
      } else if (shapeType == TRIANGLES) {
        for (int i = 0; i <= vertCount - 3; i += 3) {
          check(3);
          vertexImpl(shapeVerts[i + 0]);
          vertexImpl(shapeVerts[i + 1]);
          vertexImpl(shapeVerts[i + 2]);
        }
      }
    }

    if (stroke) {
      incrementDepth();

      if (shapeType == POLYGON) {
        if (vertCount < 3) {
          return;
        }

        int c = 0;
        sr.beginLine();
        for (int i = 0; i < vertCount; ++i) {
          if (contours[c] == i) {
            sr.endLine(mode == CLOSE);
            sr.beginLine();
            c++;
          }

          sr.lineVertex(shapeVerts[i].x, shapeVerts[i].y);
        }
        sr.endLine(mode == CLOSE);
      } else if (shapeType == QUAD_STRIP) {
        for (int i = 0; i <= vertCount - 4; i += 2) {
          sr.beginLine();
          sr.lineVertex(shapeVerts[i + 0].x, shapeVerts[i + 0].y);
          sr.lineVertex(shapeVerts[i + 1].x, shapeVerts[i + 1].y);
          sr.lineVertex(shapeVerts[i + 3].x, shapeVerts[i + 3].y);
          sr.lineVertex(shapeVerts[i + 2].x, shapeVerts[i + 2].y);
          sr.endLine(true);
        }
      } else if (shapeType == QUADS) {
        for (int i = 0; i <= vertCount - 4; i += 4) {
          sr.beginLine();
          sr.lineVertex(shapeVerts[i + 0].x, shapeVerts[i + 0].y);
          sr.lineVertex(shapeVerts[i + 1].x, shapeVerts[i + 1].y);
          sr.lineVertex(shapeVerts[i + 2].x, shapeVerts[i + 2].y);
          sr.lineVertex(shapeVerts[i + 3].x, shapeVerts[i + 3].y);
          sr.endLine(true);
        }
      } else if (shapeType == TRIANGLE_STRIP) {
        for (int i = 0; i <= vertCount - 3; i += 1) {
          sr.beginLine();
          sr.lineVertex(shapeVerts[i + 0].x, shapeVerts[i + 0].y);
          sr.lineVertex(shapeVerts[i + 1].x, shapeVerts[i + 1].y);
          sr.lineVertex(shapeVerts[i + 2].x, shapeVerts[i + 2].y);
          sr.endLine(true);
        }
      } else if (shapeType == TRIANGLE_FAN) {
        for (int i = 0; i <= vertCount - 3; i += 1) {
          sr.beginLine();
          sr.lineVertex(shapeVerts[0 + 0].x, shapeVerts[0 + 0].y);
          sr.lineVertex(shapeVerts[i + 1].x, shapeVerts[i + 1].y);
          sr.lineVertex(shapeVerts[i + 2].x, shapeVerts[i + 2].y);
          sr.endLine(true);
        }

        //close the fan
        if (vertCount >= 3) {
          sr.beginLine();
          sr.lineVertex(shapeVerts[0].x, shapeVerts[0].y);
          sr.lineVertex(shapeVerts[vertCount - 1].x, shapeVerts[vertCount - 1].y);
          sr.lineVertex(shapeVerts[1].x, shapeVerts[1].y);
          sr.endLine(true);
        }
      } else if (shapeType == TRIANGLES) {
        for (int i = 0; i <= vertCount - 3; i += 3) {
          sr.beginLine();
          sr.lineVertex(shapeVerts[i + 0].x, shapeVerts[i + 0].y);
          sr.lineVertex(shapeVerts[i + 1].x, shapeVerts[i + 1].y);
          sr.lineVertex(shapeVerts[i + 2].x, shapeVerts[i + 2].y);
          sr.endLine(true);
        }
      } else if (shapeType == LINES) {
        for (int i = 0; i <= vertCount - 2;  i += 2) {
          TessVertex s1 = shapeVerts[i + 0];
          TessVertex s2 = shapeVerts[i + 1];
          singleLine(s1.x, s1.y, s2.x, s2.y, strokeColor);
        }
      } else if (shapeType == POINTS) {
        for (int i = 0; i <= vertCount - 1; i += 1) {
          singlePoint(shapeVerts[i].x, shapeVerts[i].y, strokeColor);
        }
      }
    }
  }


  @Override
  public void beginContour() {
    super.beginContour();
    if (useParentImpl) {
      return;
    }

    //XXX: not sure what the exact behavior should be for invalid calls to begin/endContour()
    //but this should work for valid cases for now
    appendContour(vertCount);
  }


  @Override
  public void vertex(float x, float y) {
    if (useParentImpl) {
      super.vertex(x, y);
      return;
    }

    curveVerts = 0;
    shapeVertex(x, y, 0, 0, fillColor, 0);
  }


  @Override
  public void vertex(float x, float y, float u, float v) {
    if (useParentImpl) {
      super.vertex(x, y, u, v);
      return;
    }

    curveVerts = 0;
    textureImpl(imageTex);
    shapeVertex(x, y, u, v, tint? tintColor : 0xFFFFFFFF, 1);
  }


  @Override
  public void vertex(float x, float y, float z) {
    showDepthWarningXYZ("vertex");
  }


  @Override
  public void vertex(float x, float y, float z, float u, float v) {
    showDepthWarningXYZ("vertex");
  }


  //////////////////////////////////////////////////////////////

  // BEZIER VERTICES


  //this method is almost wholesale copied from PGraphics.bezierVertex()
  //TODO: de-duplicate this code if there is a convenient way to do so
  @Override
  public void bezierVertex(float x2, float y2, float x3, float y3, float x4, float y4) {
    if (useParentImpl) {
      super.bezierVertex(x2, y2, x3, y3, x4, y4);
      return;
    }

    bezierInitCheck();
//    bezierVertexCheck(); //TODO: re-implement this (and other run-time sanity checks)
    PMatrix3D draw = bezierDrawMatrix;

    //(these are the only lines that are different)
    float x1 = shapeVerts[vertCount - 1].x;
    float y1 = shapeVerts[vertCount - 1].y;

    float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
    float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
    float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;

    float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
    float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
    float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;

    for (int j = 0; j < bezierDetail; j++) {
      x1 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y1 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      shapeVertex(x1, y1, 0, 0, fillColor, 0);
    }
  }


  @Override
  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    showDepthWarningXYZ("bezierVertex");
  }


  //////////////////////////////////////////////////////////////

  // QUADRATIC BEZIER VERTICES


  //this method is almost wholesale copied from PGraphics.quadraticVertex()
  //TODO: de-duplicate this code if there is a convenient way to do so
  @Override
  public void quadraticVertex(float cx, float cy,
                              float x3, float y3) {
    if (useParentImpl) {
      super.quadraticVertex(cx, cy, x3, y3);
      return;
    }

    //(these are the only lines that are different)
    float x1 = shapeVerts[vertCount - 1].x;
    float y1 = shapeVerts[vertCount - 1].y;

    //TODO: optimize this?
    bezierVertex(x1 + ((cx-x1)*2/3.0f), y1 + ((cy-y1)*2/3.0f),
                 x3 + ((cx-x3)*2/3.0f), y3 + ((cy-y3)*2/3.0f),
                 x3, y3);
  }


  @Override
  public void quadraticVertex(float x2, float y2, float z2,
                         float x4, float y4, float z4) {
    showDepthWarningXYZ("quadVertex");
  }


  //////////////////////////////////////////////////////////////

  // CURVE VERTICES


  //curve vertices
  private float cx1, cy1, cx2, cy2, cx3, cy3, cx4, cy4;
  private int curveVerts;


  @Override
  public void curveVertex(float x, float y) {
    if (useParentImpl) {
      super.curveVertex(x, y);
      return;
    }

//    curveVertexCheck(); //TODO: re-implement this (and other runtime checks)

    curveInitCheck();

    cx1 = cx2;
    cx2 = cx3;
    cx3 = cx4;

    cy1 = cy2;
    cy2 = cy3;
    cy3 = cy4;

    cx4 = x;
    cy4 = y;

    curveVerts += 1;

    if (curveVerts > 3) {
      println("drawing curve...");

      PMatrix3D draw = curveDrawMatrix;

      float xplot1 = draw.m10*cx1 + draw.m11*cx2 + draw.m12*cx3 + draw.m13*cx4;
      float xplot2 = draw.m20*cx1 + draw.m21*cx2 + draw.m22*cx3 + draw.m23*cx4;
      float xplot3 = draw.m30*cx1 + draw.m31*cx2 + draw.m32*cx3 + draw.m33*cx4;

      float yplot1 = draw.m10*cy1 + draw.m11*cy2 + draw.m12*cy3 + draw.m13*cy4;
      float yplot2 = draw.m20*cy1 + draw.m21*cy2 + draw.m22*cy3 + draw.m23*cy4;
      float yplot3 = draw.m30*cy1 + draw.m31*cy2 + draw.m32*cy3 + draw.m33*cy4;

      float x0 = cx2;
      float y0 = cy2;

      if (curveVerts == 4) {
        shapeVertex(x0, y0, 0, 0, fillColor, 0);
      }

      for (int j = 0; j < curveDetail; j++) {
        x0 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
        y0 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
        shapeVertex(x0, y0, 0, 0, fillColor, 0);
      }
    }
  }


  @Override
  public void curveVertex(float x, float y, float z) {
    showDepthWarningXYZ("curveVertex");
  }


  //////////////////////////////////////////////////////////////

  // PRIMITIVES


  /*
   * Re-implementations of the various shape drawing methods.
   *
   * Ideally we could just call the versions in PGraphics,
   * since most of those will work correctly without modification,
   * but there's no good way to do that in Java,
   * so as long as we're inheriting from PGraphicsOpenGL,
   * we need to re-implement them.
   */


  @Override
  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
    if (useParentImpl) {
      super.quad(x1, y1, x2, y2, x3, y3, x4, y4);
      return;
    }

    beginShape(QUADS);
    vertex(x1, y1);
    vertex(x2, y2);
    vertex(x3, y3);
    vertex(x4, y4);
    endShape();
  }


  @Override
  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    if (useParentImpl) {
      super.triangle(x1, y1, x2, y2, x3, y3);
      return;
    }

    beginShape(TRIANGLES);
    vertex(x1, y1);
    vertex(x2, y2);
    vertex(x3, y3);
    endShape();
  }


  @Override
  public void ellipseImpl(float a, float b, float c, float d) {
    if (useParentImpl) {
      super.ellipseImpl(a, b, c, d);
      return;
    }

    //TODO: optimize this function, it is still pretty slow
    //TODO: try using a lookup table and see if we can make it faster than real trig

    beginShape(POLYGON);

    //convert corner/diameter to center/radius
    float rx = c * 0.5f;
    float ry = d * 0.5f;
    float x = a + rx;
    float y = b + ry;

    //since very wide stroke and/or very small radius might cause the
    //stroke to account for a significant portion of the overall radius,
    //we take it into account when calculating detail, just to be safe
    int segments = circleDetail(PApplet.max(rx, ry) + (stroke? strokeWeight : 0), TWO_PI);
    float step = TWO_PI / segments;

    float angle = 0;
    for (int i = 0; i < segments; ++i) {
      angle += step;
      shapeVertex(x + PApplet.sin(angle) * rx, y + PApplet.cos(angle) * ry, 0, 0, fillColor, 0);
    }

    knownConvexPolygon = true;
    endShape(CLOSE);
  }


  @Override
  public void line(float x1, float y1, float x2, float y2) {
    if (useParentImpl) {
      super.line(x1, y1, x2, y2);
      return;
    }

    incrementDepth();
    singleLine(x1, y1, x2, y2, strokeColor);
  }


  @Override
  public void point(float x, float y) {
    if (useParentImpl) {
      super.point(x, y);
      return;
    }

    incrementDepth();
    singlePoint(x, y, strokeColor);
  }


  @Override
  protected void arcImpl(float x, float y, float w, float h, float start, float stop, int mode) {
    if (useParentImpl) {
      super.arcImpl(x, y, w, h, start, stop, mode);
      return;
    }

    //INVARIANT: stop > start
    //INVARIANT: stop - start <= TWO_PI

    //convert corner/diameter to center/radius
    w *= 0.5f;
    h *= 0.5f;
    x += w;
    y += h;

    float diff = stop - start;
    int segments = circleDetail(PApplet.max(w, h), diff);
    float step = diff / segments;

    beginShape(POLYGON);

    //no constant is defined for the default arc mode, so we just use a literal 0
    //(this is consistent with code elsewhere)
    if (mode == 0 || mode == PIE) {
      vertex(x, y);
    }

    if (mode == 0) {
      //kinda hacky way to disable drawing a stroke along the first edge
      appendContour(vertCount);
    }

    for (int i = 0; i <= segments; ++i) {
      float s = PApplet.cos(start) * w;
      float c = PApplet.sin(start) * h;

      vertex(x + s, y + c);

      start += step;
    }

    //for the case `(mode == PIE || mode == 0) && diff > HALF_PI`, the polygon
    //will not actually be convex, but we still want to tessellate as if it is
    knownConvexPolygon = true;
    if (mode == CHORD || mode == PIE) {
      endShape(CLOSE);
    } else {
      endShape();
    }
  }


  @Override
  protected void rectImpl(float x1, float y1, float x2, float y2,
                          float tl, float tr, float br, float bl) {
    if (useParentImpl) {
      super.rectImpl(x1, y1, x2, y2, tl, tr, br, bl);
      return;
    }

    beginShape();
    if (tr != 0) {
      vertex(x2-tr, y1);
      quadraticVertex(x2, y1, x2, y1+tr);
    } else {
      vertex(x2, y1);
    }
    if (br != 0) {
      vertex(x2, y2-br);
      quadraticVertex(x2, y2, x2-br, y2);
    } else {
      vertex(x2, y2);
    }
    if (bl != 0) {
      vertex(x1+bl, y2);
      quadraticVertex(x1, y2, x1, y2-bl);
    } else {
      vertex(x1, y2);
    }
    if (tl != 0) {
      vertex(x1, y1+tl);
      quadraticVertex(x1, y1, x1+tl, y1);
    } else {
      vertex(x1, y1);
    }
    knownConvexPolygon = true;
    endShape(CLOSE);
  }


  //////////////////////////////////////////////////////////////

  // BOX


  @Override
  public void box(float w, float h, float d) {
    showMethodWarning("box");
  }


  //////////////////////////////////////////////////////////////

  // SPHERE


  @Override
  public void sphere(float r) {
    showMethodWarning("sphere");
  }


  //////////////////////////////////////////////////////////////

  // PIXELS


  @Override
  public void loadPixels() {
    super.loadPixels();

    allocatePixels();
    readPixels();
  }


  @Override
  public void updatePixels() {
    super.updatePixels();
    image(this, 0, 0, width * 2, height * 2, 0, 0, pixelWidth, pixelHeight);
    flushBuffer();
  }


  //////////////////////////////////////////////////////////////

  // CLIPPING

  /*
  @Override
  public void clipImpl(float x1, float y1, float x2, float y2) {
    //XXX: exactly the same as the implementation in PGraphicsOpenGL,
    //but calls flushBuffer() instead of flush()
    flushBuffer();
    pgl.enable(PGL.SCISSOR_TEST);

    float h = y2 - y1;
    clipRect[0] = (int)x1;
    clipRect[1] = (int)(height - y1 - h);
    clipRect[2] = (int)(x2 - x1);
    clipRect[3] = (int)h;
    pgl.scissor(clipRect[0], clipRect[1], clipRect[2], clipRect[3]);

    clip = true;
  }

  @Override
  public void noClip() {
    //XXX: exactly the same as the implementation in PGraphicsOpenGL,
    //but calls flushBuffer() instead of flush()
    if (clip) {
      flushBuffer();
      pgl.disable(PGL.SCISSOR_TEST);
      clip = false;
    }
  }
*/


  //////////////////////////////////////////////////////////////

  // TEXT


  //NOTE: a possible improvement to text rendering performance is to batch all glyphs
  //from the same texture page together instead of rendering each char strictly in sequence.
  //it remains to be seen whether this would improve performance in practice
  //(I don't know how common it is for a font to occupy multiple texture pages)

  @Override
  protected void textCharModelImpl(FontTexture.TextureInfo info,
      float x0, float y0, float x1, float y1) {
    incrementDepth();
    check(6);
    textureImpl(textTex.textures[info.texIndex].glName);
    vertexImpl(x0, y0, info.u0, info.v0, fillColor, 1);
    vertexImpl(x1, y0, info.u1, info.v0, fillColor, 1);
    vertexImpl(x0, y1, info.u0, info.v1, fillColor, 1);
    vertexImpl(x1, y0, info.u1, info.v0, fillColor, 1);
    vertexImpl(x0, y1, info.u0, info.v1, fillColor, 1);
    vertexImpl(x1, y1, info.u1, info.v1, fillColor, 1);
  }


  //////////////////////////////////////////////////////////////

  // MATRIX OPS


  /*
   * Monkey-patch all methods that modify matrices to optionally flush the vertex buffer.
   * If you see a method that isn't here but should be, or is here but shouldn't,
   * feel free to add/remove it.
   */


  @Override
  public void applyMatrix(float n00, float n01, float n02, float n10, float n11, float n12) {
    preMatrixChanged();
    super.applyMatrix(n00, n01, n02, n10, n11, n12);
    postMatrixChanged();
  }


  @Override
  public void applyMatrix(PMatrix2D source) {
    preMatrixChanged();
    super.applyMatrix(source);
    postMatrixChanged();
  }


  @Override
  public void applyProjection(float n00, float n01, float n02, float n03,
      float n10, float n11, float n12, float n13,
      float n20, float n21, float n22, float n23,
      float n30, float n31, float n32, float n33) {
    preMatrixChanged();
    super.applyProjection(n00, n01, n02, n03,
        n10, n11, n12, n13,
        n20, n21, n22, n23,
        n30, n31, n32, n33);
    postMatrixChanged();
  }


  @Override
  public void applyProjection(PMatrix3D mat) {
    preMatrixChanged();
    super.applyProjection(mat);
    postMatrixChanged();
  }


  @Override
  public void popMatrix() {
    preMatrixChanged();
    super.popMatrix();
    postMatrixChanged();
  }


  @Override
  public void popProjection() {
    preMatrixChanged();
    super.popProjection();
    postMatrixChanged();
  }


  @Override
  public void pushMatrix() {
    preMatrixChanged();
    super.pushMatrix();
    postMatrixChanged();
  }


  @Override
  public void pushProjection() {
    preMatrixChanged();
    super.pushProjection();
    postMatrixChanged();
  }


  @Override
  public void resetMatrix() {
    preMatrixChanged();
    super.resetMatrix();
    postMatrixChanged();
  }


  @Override
  public void resetProjection() {
    preMatrixChanged();
    super.resetProjection();
    postMatrixChanged();
  }


  @Override
  public void rotate(float angle) {
    preMatrixChanged();
    super.rotate(angle);
    postMatrixChanged();
  }


  @Override
  public void scale(float s) {
    preMatrixChanged();
    super.scale(s);
    postMatrixChanged();
  }


  @Override
  public void scale(float sx, float sy) {
    preMatrixChanged();
    super.scale(sx, sy);
    postMatrixChanged();
  }


  @Override
  public void setMatrix(PMatrix2D source) {
    preMatrixChanged();
    super.setMatrix(source);
    postMatrixChanged();
  }


  @Override
  public void setProjection(PMatrix3D mat) {
    preMatrixChanged();
    super.setProjection(mat);
    postMatrixChanged();
  }


  @Override
  public void shearX(float angle) {
    preMatrixChanged();
    super.shearX(angle);
    postMatrixChanged();
  }


  @Override
  public void shearY(float angle) {
    preMatrixChanged();
    super.shearY(angle);
    postMatrixChanged();
  }


  @Override
  public void translate(float tx, float ty) {
    preMatrixChanged();
    super.translate(tx, ty);
    postMatrixChanged();
  }


  @Override
  public void updateProjmodelview() {
    preMatrixChanged();
    super.updateProjmodelview();
    postMatrixChanged();
  }


  @Override
  public void updateGLModelview() {
    preMatrixChanged();
    super.updateGLModelview();
    postMatrixChanged();
  }


  @Override
  public void updateGLProjection() {
    preMatrixChanged();
    super.updateGLProjection();
    postMatrixChanged();
  }


  @Override
  public void updateGLProjmodelview() {
    preMatrixChanged();
    super.updateGLProjmodelview();
    postMatrixChanged();
  }


  //////////////////////////////////////////////////////////////

  // MATRIX MORE!


  @Override
  protected void begin2D() {
    pushProjection();
    defaultPerspective();
    pushMatrix();
    defaultCamera();
  }


  @Override
  protected void end2D() {
    popMatrix();
    popProjection();
  }


  //////////////////////////////////////////////////////////////

  // SHADER FILTER


  @Override
  public void filter(PShader shader) {
    // The filter method needs to use the geometry-generation in the base class.
    // We could re-implement it here, but this is easier.
    if (!useParentImpl) {
      useOldP2D();
      super.filter(shader);
      useNewP2D();
    } else {
      super.filter(shader);
    }
  }


  //////////////////////////////////////////////////////////////

  // SHADER API


  @Override
  public void shader(PShader shader) {
    if (useParentImpl) {
      super.shader(shader);
      return;
    }
    flushBuffer(); // Flushing geometry drawn with a different shader.

    if (shader != null) shader.init();
    boolean res = checkShaderLocs(shader);
    if (res) {
      twoShader = shader;
      shader.type = SHADER2D;
    } else {
      PGraphics.showWarning(NON_2D_SHADER_ERROR);
    }
  }


  @Override
  public void shader(PShader shader, int kind) {
    if (useParentImpl) {
      super.shader(shader, kind);
      return;
    }
    PGraphics.showWarning(WRONG_SHADER_PARAMS);
  }


  @Override
  public void resetShader() {
    if (useParentImpl) {
      super.resetShader();
      return;
    }
    flushBuffer();
    twoShader = null;
  }


  @Override
  public void resetShader(int kind) {
    if (useParentImpl) {
      super.resetShader(kind);
      return;
    }
    PGraphics.showWarning(WRONG_SHADER_PARAMS);
  }


  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS


  @Override
  public void translate(float tx, float ty, float tz) {
    showDepthWarningXYZ("translate");
  }

  @Override
  public void rotateX(float angle) {
    showDepthWarning("rotateX");
  }

  @Override
  public void rotateY(float angle) {
    showDepthWarning("rotateY");
  }

  @Override
  public void rotateZ(float angle) {
    showDepthWarning("rotateZ");
  }

  @Override
  public void rotate(float angle, float vx, float vy, float vz) {
    showVariationWarning("rotate");
  }

  @Override
  public void applyMatrix(PMatrix3D source) {
    showVariationWarning("applyMatrix");
  }

  @Override
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    showVariationWarning("applyMatrix");
  }

  @Override
  public void scale(float sx, float sy, float sz) {
    showDepthWarningXYZ("scale");
  }

  //////////////////////////////////////////////////////////////

  // SCREEN AND MODEL COORDS


  @Override
  public float screenX(float x, float y, float z) {
    showDepthWarningXYZ("screenX");
    return 0;
  }

  @Override
  public float screenY(float x, float y, float z) {
    showDepthWarningXYZ("screenY");
    return 0;
  }

  @Override
  public float screenZ(float x, float y, float z) {
    showDepthWarningXYZ("screenZ");
    return 0;
  }

  @Override
  public PMatrix3D getMatrix(PMatrix3D target) {
    showVariationWarning("getMatrix");
    return target;
  }

  @Override
  public void setMatrix(PMatrix3D source) {
    showVariationWarning("setMatrix");
  }

  //////////////////////////////////////////////////////////////

  // LIGHTS


  @Override
  public void lights() {
    showMethodWarning("lights");
  }

  @Override
  public void noLights() {
    showMethodWarning("noLights");
  }

  @Override
  public void ambientLight(float red, float green, float blue) {
    showMethodWarning("ambientLight");
  }

  @Override
  public void ambientLight(float red, float green, float blue,
                           float x, float y, float z) {
    showMethodWarning("ambientLight");
  }

  @Override
  public void directionalLight(float red, float green, float blue,
                               float nx, float ny, float nz) {
    showMethodWarning("directionalLight");
  }

  @Override
  public void pointLight(float red, float green, float blue,
                         float x, float y, float z) {
    showMethodWarning("pointLight");
  }

  @Override
  public void spotLight(float red, float green, float blue,
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float angle, float concentration) {
    showMethodWarning("spotLight");
  }

  @Override
  public void lightFalloff(float constant, float linear, float quadratic) {
    showMethodWarning("lightFalloff");
  }

  @Override
  public void lightSpecular(float v1, float v2, float v3) {
    showMethodWarning("lightSpecular");
  }


  //////////////////////////////////////////////////////////////

  // PRIVATE IMPLEMENTATION


  //superclass does lazy initialization, so we need to as well
  private void init() {
    if (initialized) return;
    initialized = true;

    String[] vertSource = pgl.loadVertexShader(defP2DShaderVertURL);
    String[] fragSource = pgl.loadFragmentShader(defP2DShaderFragURL);
    twoShader = new PShader(parent, vertSource, fragSource);
    loadShaderLocs(twoShader);
    defTwoShader = twoShader;

    //generate vbo
    IntBuffer vboBuff = IntBuffer.allocate(1);
    pgl.genBuffers(1, vboBuff);
    vbo = vboBuff.get(0);
  }


  //maxVerts can be tweaked for memory/performance trade-off
  //in my testing, performance seems to plateau after around 6000 (= 2000*3)
  //memory usage should be around ~165kb for 6000 verts
  final private int maxVerts = 2000*3;
  final private int vertSize = 7*Float.BYTES; //xyzuvcf
  private float[] vertexData = new float[maxVerts*7];
  private int usedVerts = 0;

  private float depth = 1.0f;

  private int imageTex;
  private int tex;
  private int vbo;
  private int texWidth, texHeight;


  private void incrementDepth() {
    //by resetting the depth buffer when needed, we are able to have arbitrarily many
    //layers, unlimited by depth buffer precision. in practice, the precision of this
    //algorithm seems to be very good (~1,000,000 layers), so it pretty much won't happen
    //unless you're drawing enough geometry per frame to set your computer on fire
    if (depth < -0.9999f) {
      flushBuffer();
      pgl.clear(PGL.DEPTH_BUFFER_BIT);
      //depth test will fail at depth = 1.0 after clearing the depth buffer,
      //but since we always increment before drawing anything, this should be okay
      depth = 1.0f;
    }

    //found to be a small but reliable increment value for a 24-bit depth buffer
    //through trial and error. as numbers approach zero, absolute floating point
    //precision increases, while absolute fixed point precision stays the same,
    //so regardless of representation, this value should work for all depths in
    //range (-1, 1), as long as it works for depths at either end of the range
    depth -= 0.000001f;

    //TODO: use an increment value based on good math instead of lazy trial-and-error
  }


  private void initTess() {
    PGL.TessellatorCallback callback = new PGL.TessellatorCallback() {

      public void begin(int type) {
        // TODO Auto-generated method stub

      }

      public void end() {
        // TODO Auto-generated method stub

      }

      public void vertex(Object data) {
        if (usedVerts % 3 == 0) {
          check(3);
        }

        TessVertex vert = (TessVertex)data;
        vertexImpl(vert.x, vert.y, vert.u, vert.v, vert.c, vert.f);
      }

      public void combine(double[] coords, Object[] data, float[] weights, Object[] outData) {
        //here we do some horrible things to blend the colors
        float r = 0, g = 0, b = 0, a = 0;
        for (int i = 0; i < data.length; ++i) {
          int c = ((TessVertex)data[i]).c;
          a += weights[i] * ((c >> 24) & 0xFF);
          r += weights[i] * ((c >> 16) & 0xFF);
          g += weights[i] * ((c >> 8) & 0xFF);
          b += weights[i] * (c & 0xFF);
        }
        int c = ((int)a << 24) + ((int)r << 16) + ((int)g << 8) + (int)b;

        float u = 0, v = 0, f = 0;
        for (int i = 0; i < data.length; ++i) {
          u += weights[i] * ((TessVertex)data[i]).u;
          v += weights[i] * ((TessVertex)data[i]).v;
          f += weights[i] * ((TessVertex)data[i]).f;
        }

        outData[0] = new TessVertex((float)coords[0], (float)coords[1], u, v, c, f);
      }

      public void error(int err) {
        println("glu error: " + err);
      }
    };
    tess = pgl.createTessellator(callback);

    // We specify the edge flag callback as a no-op to force the tesselator to only pass us
    // triangle primitives (no triangle fans or triangle strips), for simplicity
    tess.setCallback(PGL.TESS_EDGE_FLAG);
    tess.setWindingRule(PGL.TESS_WINDING_NONZERO);
  }


  private void initVerts() {
    for (int i = 0; i < shapeVerts.length; ++i) {
      shapeVerts[i] = new TessVertex();
    }
  }


  private void flushBuffer() {
    if (usedVerts == 0) {
      return;
    }

    init();

    //upload vertex data
    pgl.bindBuffer(PGL.ARRAY_BUFFER, vbo);
    pgl.bufferData(PGL.ARRAY_BUFFER, usedVerts * vertSize,
        FloatBuffer.wrap(vertexData), PGL.DYNAMIC_DRAW);

    PShader shader = getShader();
    shader.bind();
    setAttribs();
    loadUniforms();

    pgl.drawArrays(PGL.TRIANGLES, 0, usedVerts);

    usedVerts = 0;
    shader.unbind();

    //XXX: DEBUG
//    println("flushed: " + tex + ", " + imageTex);
  }


  private boolean checkShaderLocs(PShader shader) {
    int positionLoc = shader.getAttributeLoc("position");
    if (positionLoc == -1) {
      positionLoc = shader.getAttributeLoc("vertex");
    }
    int colorLoc = shader.getAttributeLoc("color");
    int texCoordLoc = shader.getAttributeLoc("texCoord");
    int texFactorLoc = shader.getAttributeLoc("texFactor");
    int transformLoc = shader.getUniformLoc("transform");
    if (transformLoc == -1) {
      transformLoc = shader.getUniformLoc("transformMatrix");
    }
    int texScaleLoc = shader.getUniformLoc("texScale");
    return positionLoc != -1 && colorLoc != -1 && texCoordLoc != -1 &&
           texFactorLoc != -1 && transformLoc != -1 && texScaleLoc != -1;
  }


  private void loadShaderLocs(PShader shader) {
    positionLoc = shader.getAttributeLoc("position");
    if (positionLoc == -1) {
      positionLoc = shader.getAttributeLoc("vertex");
    }
    colorLoc = shader.getAttributeLoc("color");
    texCoordLoc = shader.getAttributeLoc("texCoord");
    texFactorLoc = shader.getAttributeLoc("texFactor");
    transformLoc = shader.getUniformLoc("transform");
    if (transformLoc == -1) {
      transformLoc = shader.getUniformLoc("transformMatrix");
    }
    texScaleLoc = shader.getUniformLoc("texScale");
  }


  private PShader getShader() {
    PShader shader;
    if (twoShader == null) {
      shader = defTwoShader;
    } else {
      shader = twoShader;
    }
    if (shader != defTwoShader) {
      loadShaderLocs(shader);
    }
    return shader;
  }


  private void setAttribs() {
    pgl.vertexAttribPointer(positionLoc, 3, PGL.FLOAT, false, vertSize, 0);
    pgl.enableVertexAttribArray(positionLoc);
    pgl.vertexAttribPointer(texCoordLoc, 2, PGL.FLOAT, false, vertSize, 3*Float.BYTES);
    pgl.enableVertexAttribArray(texCoordLoc);
    pgl.vertexAttribPointer(colorLoc, 4, PGL.UNSIGNED_BYTE, true, vertSize, 5*Float.BYTES);
    pgl.enableVertexAttribArray(colorLoc);
    pgl.vertexAttribPointer(texFactorLoc, 1, PGL.FLOAT, false, vertSize, 6*Float.BYTES);
    pgl.enableVertexAttribArray(texFactorLoc);
  }


  private void loadUniforms() {
    //set matrix uniform
    if (premultiplyMatrices) {
      pgl.uniformMatrix4fv(transformLoc, 1, true, FloatBuffer.wrap(new PMatrix3D().get(null)));
    } else {
      pgl.uniformMatrix4fv(transformLoc, 1, true, FloatBuffer.wrap(projmodelview.get(null)));
    }

    //set texture info
    pgl.activeTexture(PGL.TEXTURE0);
    pgl.bindTexture(PGL.TEXTURE_2D, tex);
    //enable uv scaling only for use-defined images, not for fonts
    if (tex == imageTex) {
      pgl.uniform2f(texScaleLoc, 1f/texWidth, 1f/texHeight);
    } else {
      pgl.uniform2f(texScaleLoc, 1, 1);
    }
  }


  private void textureImpl(int glId) {
    if (glId == tex) {
      return; //texture is already bound; no work to be done
    }

    flushBuffer();
    tex = glId;
  }


  private void check(int newVerts) {
    if (usedVerts + newVerts > maxVerts) {
      flushBuffer();
    }
  }


  private void vertexImpl(float x, float y, float u, float v, int c, float f) {
    int idx = usedVerts * 7;
    if (premultiplyMatrices) {
      //inline multiply only x and y to avoid an allocation and a few flops
      vertexData[idx + 0] = projmodelview.m00*x + projmodelview.m01*y + projmodelview.m03;
      vertexData[idx + 1] = projmodelview.m10*x + projmodelview.m11*y + projmodelview.m13;
    } else {
      vertexData[idx + 0] = x;
      vertexData[idx + 1] = y;
    }
    vertexData[idx + 2] = depth;
    vertexData[idx + 3] = u;
    vertexData[idx + 4] = v;
    vertexData[idx + 5] = Float.intBitsToFloat(c);
    vertexData[idx + 6] = f;
    usedVerts++;
  }


  private void vertexImpl(TessVertex vert) {
    vertexImpl(vert.x, vert.y, vert.u, vert.v, vert.c, vert.f);
  }


  //one of POINTS, LINES, TRIANGLES, TRIANGLE_FAN, TRIANGLE_STRIP, QUADS, QUAD_STRIP, POLYGON
  private int shapeType;
  private int vertCount;
  private TessVertex[] shapeVerts = new TessVertex[16]; //initial size is arbitrary

  //list of indices (into shapeVerts array) at which a new contour begins
  private int[] contours = new int[2]; //initial size is arbitrary
  private int contourCount;


  private void appendContour(int vertIndex) {
    //dynamically expand contour array as needed
    if (contourCount >= contours.length) {
      contours = PApplet.expand(contours, contours.length * 2);
    }

    contours[contourCount] = vertIndex;
    contourCount += 1;
  }


  //used by endShape() as a temporary to avoid unnecessary allocations
  private double[] tempDoubles = new double[3];

  //If this flag is set, then the next call to endShape() with shape type of POLYGON
  //will triangulate blindly instead of going through the GLU tessellator (for performance).
  //This is useful for shapes (like ellipse(), rect(), etc.) that we know will always be convex.
  //TODO: Make this an optional argument to endShape()
  //once we start integrating PGraphics4D into the rest of the codebase.
  private boolean knownConvexPolygon = false;


  private void shapeVertex(float x, float y, float u, float v, int c, float f) {
    //avoid adding a duplicate because it will cause the GLU tess to fail spectacularly
    //by spitting out-of-memory errors and passing null parameters to the combine() callback
    //TODO: figure out why that happens and how to stop it
    //(P2D renderer doesn't appear to have such a problem, so presumably there must be a way)
    for (int i = 0; i < vertCount; ++i) {
      if (shapeVerts[i].x == x && shapeVerts[i].y == y) {
        return;
      }
    }

    //dynamically expand input vertex array as needed
    if (vertCount >= shapeVerts.length) {
      shapeVerts = (TessVertex[]) PApplet.expand(shapeVerts, shapeVerts.length * 2);

      //allocate objects for the new half of the array so we don't NPE ourselves
      for (int i = shapeVerts.length/2; i < shapeVerts.length; ++i) {
        shapeVerts[i] = new TessVertex();
      }
    }

    shapeVerts[vertCount].set(x, y, u, v, c, f);
    vertCount += 1;
  }


  float ellipseDetailMultiplier = 1;


  private void preMatrixChanged() {
    if (!premultiplyMatrices) {
      flushBuffer();
    }
  }


  private void postMatrixChanged() {
    //this serves as a rough approximation of how much the longest axis
    //of an ellipse will be scaled by a given matrix
    //(in other words, the amount by which its on-screen size changes)
    float sxi = projmodelview.m00 * width / 2;
    float syi = projmodelview.m10 * height / 2;
    float sxj = projmodelview.m01 * width / 2;
    float syj = projmodelview.m11 * height / 2;
    float Imag = PApplet.sqrt(sxi * sxi + syi * syi);
    float Jmag = PApplet.sqrt(sxj * sxj + syj * syj);
    ellipseDetailMultiplier = PApplet.max(Imag, Jmag);
  }


  private void triangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
    check(3);
    vertexImpl(x1, y1, 0, 0, color, 0);
    vertexImpl(x2, y2, 0, 0, color, 0);
    vertexImpl(x3, y3, 0, 0, color, 0);
  }


  //below r == LINE_DETAIL_LIMIT, all lines will be drawn as plain rectangles
  //instead of using fancy stroke rendering algorithms, since the result is visually indistinguishable
  static final private float LINE_DETAIL_LIMIT = 1.0f;


  private void singleLine(float x1, float y1, float x2, float y2, int color) {
    float r = strokeWeight * 0.5f;

    float dx = x2 - x1;
    float dy = y2 - y1;
    float d = PApplet.sqrt(dx*dx + dy*dy);
    float tx = dy / d * r;
    float ty = dx / d * r;

    if (strokeCap == PROJECT) {
      x1 -= ty;
      x2 += ty;
      y1 -= tx;
      y2 += tx;
    }

    triangle(x1 - tx, y1 + ty, x1 + tx, y1 - ty, x2 - tx, y2 + ty, color);
    triangle(x2 + tx, y2 - ty, x2 - tx, y2 + ty, x1 + tx, y1 - ty, color);

    if (r >= LINE_DETAIL_LIMIT && strokeCap == ROUND) {
      float angle = PApplet.atan2(dx, dy);

      int segments = circleDetail(r, HALF_PI);
      float step = HALF_PI / segments;

      float psin = ty;
      float pcos = tx;
      for (int i = 1; i < segments; ++i) {
        angle += step;
        float nsin = PApplet.sin(angle) * r;
        float ncos = PApplet.cos(angle) * r;

        triangle(x2, y2, x2 + psin, y2 + pcos, x2 + nsin, y2 + ncos, color);
        triangle(x2, y2, x2 - pcos, y2 + psin, x2 - ncos, y2 + nsin, color);
        triangle(x1, y1, x1 - psin, y1 - pcos, x1 - nsin, y1 - ncos, color);
        triangle(x1, y1, x1 + pcos, y1 - psin, x1 + ncos, y1 - nsin, color);

        psin = nsin;
        pcos = ncos;
      }

      triangle(x2, y2, x2 + psin, y2 + pcos, x2 + tx, y2 - ty, color);
      triangle(x2, y2, x2 - pcos, y2 + psin, x2 + ty, y2 + tx, color);
      triangle(x1, y1, x1 - psin, y1 - pcos, x1 - tx, y1 + ty, color);
      triangle(x1, y1, x1 + pcos, y1 - psin, x1 - ty, y1 - tx, color);
    }
  }


  private void singlePoint(float x, float y, int color) {
    float r = strokeWeight * 0.5f;
    if (strokeCap == ROUND) {
      int segments = circleDetail(r);
      float step = QUARTER_PI / segments;

      float x1 = 0;
      float y1 = r;
      float angle = 0;
      for (int i = 0; i < segments; ++i) {
        angle += step;
        float x2, y2;
        //this is not just for performance
        //it also ensures the circle is drawn with no diagonal gaps
        if (i < segments - 1) {
          x2 = PApplet.sin(angle) * r;
          y2 = PApplet.cos(angle) * r;
        } else {
          x2 = y2 = PApplet.sin(QUARTER_PI) * r;
        }

        triangle(x, y, x + x1, y + y1, x + x2, y + y2, strokeColor);
        triangle(x, y, x + x1, y - y1, x + x2, y - y2, strokeColor);
        triangle(x, y, x - x1, y + y1, x - x2, y + y2, strokeColor);
        triangle(x, y, x - x1, y - y1, x - x2, y - y2, strokeColor);

        triangle(x, y, x + y1, y + x1, x + y2, y + x2, strokeColor);
        triangle(x, y, x + y1, y - x1, x + y2, y - x2, strokeColor);
        triangle(x, y, x - y1, y + x1, x - y2, y + x2, strokeColor);
        triangle(x, y, x - y1, y - x1, x - y2, y - x2, strokeColor);

        x1 = x2;
        y1 = y2;
      }
    } else {
      triangle(x - r, y - r, x + r, y - r, x - r, y + r, color);
      triangle(x + r, y - r, x - r, y + r, x + r, y + r, color);
    }
  }


  private StrokeRenderer sr = new StrokeRenderer();


  private class StrokeRenderer {
    int lineVertexCount;
    float fx, fy;
    float sx, sy, sdx, sdy;
    float px, py, pdx, pdy;
    float lx, ly;
    float r;

    void beginLine() {
      lineVertexCount = 0;
      r = strokeWeight * 0.5f;
    }

    void lineVertex(float x, float y) {
      //disallow adding consecutive duplicate vertices,
      //as it is pointless and just creates an extra edge case
      if (lineVertexCount > 0 && x == lx && y == ly) {
        return;
      }

      if (lineVertexCount == 0) {
        fx = x;
        fy = y;
      } else if (r < LINE_DETAIL_LIMIT) {
        singleLine(lx, ly, x, y, strokeColor);
      } else if (lineVertexCount == 1) {
        sx = x;
        sy = y;
      } else {
        //find leg angles
        float angle1 = PApplet.atan2(lx - px, ly - py);
        float angle2 = PApplet.atan2(lx -  x, ly -  y);

        //find minimum absolute angle between the two legs
        //FROM: https://stackoverflow.com/a/7869457/3064745
        //NOTE: this only works for angles that are in range [-180, 180] !!!
        float diff = angle1 - angle2;
        diff += diff > PI? -TWO_PI : diff < -PI? TWO_PI : 0;

        if (strokeJoin == BEVEL || strokeJoin == ROUND ||
            PApplet.abs(diff) < PI/15 || PApplet.abs(diff) > PI - 0.001f) {
          float dx = lx - px;
          float dy = ly - py;
          float d = PApplet.sqrt(dx*dx + dy*dy);
          float tx =  dy / d * r;
          float ty = -dx / d * r;

          if (lineVertexCount == 2) {
            sdx = tx;
            sdy = ty;
          } else {
            triangle(px - pdx, py - pdy, px + pdx, py + pdy, lx - tx, ly - ty, strokeColor);
            triangle(px + pdx, py + pdy, lx - tx, ly - ty, lx + tx, ly + ty, strokeColor);
          }

          dx = x - lx;
          dy = y - ly;
          d = PApplet.sqrt(dx*dx + dy*dy);
          float nx =  dy / d * r;
          float ny = -dx / d * r;

          if (strokeJoin == ROUND) {
            float theta1 = diff > 0? angle1 - HALF_PI : angle1 + HALF_PI;
            float theta2 = diff > 0? angle2 + HALF_PI : angle2 - HALF_PI;

            //find minimum absolute angle diff (again)
            float delta = theta2 - theta1;
            delta += delta > PI? -TWO_PI : delta < -PI? TWO_PI : 0;

            //start and end points of arc
            float ax1 = diff < 0? lx + tx : lx - tx;
            float ay1 = diff < 0? ly + ty : ly - ty;
            float ax2 = diff < 0? lx + nx : lx - nx;
            float ay2 = diff < 0? ly + ny : ly - ny;

            arcJoin(lx, ly, theta1, delta, ax1, ay1, ax2, ay2);
          } else if (diff < 0) {
            triangle(lx, ly, lx + tx, ly + ty, lx + nx, ly + ny, strokeColor);
          } else {
            triangle(lx, ly, lx - tx, ly - ty, lx - nx, ly - ny, strokeColor);
          }

          pdx = nx;
          pdy = ny;
        } else {
          //find offset (hypotenuse) of miter joint
          float theta = HALF_PI - diff/2;
          float offset = r / PApplet.cos(theta);

          //find bisecting vector
          float angle = (angle1 + angle2)/2;
          float bx = PApplet.sin(angle) * offset;
          float by = PApplet.cos(angle) * offset;
          if (PApplet.abs(angle1 - angle2) < PI) {
            bx *= -1;
            by *= -1;
          }

          if (lineVertexCount == 2) {
            sdx = bx;
            sdy = by;
          } else {
            triangle(px - pdx, py - pdy, px + pdx, py + pdy, lx - bx, ly - by, strokeColor);
            triangle(px + pdx, py + pdy, lx - bx, ly - by, lx + bx, ly + by, strokeColor);
          }

          pdx = bx;
          pdy = by;
        }
      }

      px = lx;
      py = ly;
      lx = x;
      ly = y;

      lineVertexCount += 1;
    }

    void endLine(boolean closed) {
      if (lineVertexCount < 2) {
        return;
      }

      if (lineVertexCount == 2) {
        singleLine(px, py, lx, ly, strokeColor);
        return;
      }

      if (r < LINE_DETAIL_LIMIT) {
        if (closed) {
          singleLine(lx, ly, fx, fy, strokeColor);
        }
        return;
      }

      if (closed) {
        //draw the last two legs
        lineVertex(fx, fy);
        lineVertex(sx, sy);

        //connect first and second vertices
        triangle(px - pdx, py - pdy, px + pdx, py + pdy, sx - sdx, sy - sdy, strokeColor);
        triangle(px + pdx, py + pdy, sx - sdx, sy - sdy, sx + sdx, sy + sdy, strokeColor);
      } else {
        //draw last line (with cap)
        float dx = lx - px;
        float dy = ly - py;
        float d = PApplet.sqrt(dx*dx + dy*dy);
        float tx =  dy / d * r;
        float ty = -dx / d * r;

        if (strokeCap == PROJECT) {
          lx -= ty;
          ly += tx;
        }

        triangle(px - pdx, py - pdy, px + pdx, py + pdy, lx - tx, ly - ty, strokeColor);
        triangle(px + pdx, py + pdy, lx - tx, ly - ty, lx + tx, ly + ty, strokeColor);

        if (strokeCap == ROUND) {
          lineCap(lx, ly, PApplet.atan2(dx, dy));
        }

        //draw first line (with cap)
        dx = fx - sx;
        dy = fy - sy;
        d = PApplet.sqrt(dx*dx + dy*dy);
        tx =  dy / d * r;
        ty = -dx / d * r;

        if (strokeCap == PROJECT) {
          fx -= ty;
          fy += tx;
        }

        triangle(sx - sdx, sy - sdy, sx + sdx, sy + sdy, fx + tx, fy + ty, strokeColor);
        triangle(sx + sdx, sy + sdy, fx + tx, fy + ty, fx - tx, fy - ty, strokeColor);

        if (strokeCap == ROUND) {
          lineCap(fx, fy, PApplet.atan2(dx, dy));
        }
      }
    }

    void arcJoin(float x, float y, float start, float delta, float x1, float y1, float x3, float y3) {
      int segments = circleDetail(r, delta);
      float step = delta / segments;

      for (int i = 0; i < segments - 1; ++i) {
        start += step;
        float x2 = x + PApplet.sin(start) * r;
        float y2 = y + PApplet.cos(start) * r;

        triangle(x, y, x1, y1, x2, y2, strokeColor);

        x1 = x2;
        y1 = y2;
      }

      triangle(x, y, x1, y1, x3, y3, strokeColor);
    }

    //XXX: wet code, will probably get removed when we optimize lineCap()
    void arcJoin(float x, float y, float start, float delta) {
      int segments = circleDetail(r, delta);
      float step = delta / segments;

      float x1 = x + PApplet.sin(start) * r;
      float y1 = y + PApplet.cos(start) * r;
      for (int i = 0; i < segments; ++i) {
        start += step;
        float x2 = x + PApplet.sin(start) * r;
        float y2 = y + PApplet.cos(start) * r;

        triangle(x, y, x1, y1, x2, y2, strokeColor);

        x1 = x2;
        y1 = y2;
      }
    }

    void lineCap(float x, float y, float angle) {
      //TODO: optimize this
      arcJoin(x, y, angle - HALF_PI, PI);
    }
  }


  //returns the total number of points needed to approximate an arc of a given radius and extent
  int circleDetail(float radius, float delta) {
    radius *= ellipseDetailMultiplier;
    return (int)(PApplet.min(127, PApplet.sqrt(radius) / QUARTER_PI * PApplet.abs(delta) * 0.75f) + 1);
  }


  //returns the number of points per quadrant needed to approximate a circle of a given radius
  int circleDetail(float radius) {
    return circleDetail(radius, QUARTER_PI);
  }


  private class TessVertex {
    float x, y, u, v;
    int c;
    float f; //1.0 if textured, 0.0 if flat

    public TessVertex() {
      //no-op
    }

    public TessVertex(float x, float y, float u, float v, int c, float f) {
      set(x, y, u, v, c, f);
    }

    public void set(float x, float y, float u, float v, int c, float f) {
      this.x = x;
      this.y = y;
      this.u = u;
      this.v = v;
      this.c = c;
      this.f = f;
    }

    @Override
    public String toString() {
      return x + ", " + y;
    }
  }
}