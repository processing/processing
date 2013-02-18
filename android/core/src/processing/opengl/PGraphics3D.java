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
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.core.PShapeOBJ;

public class PGraphics3D extends PGraphicsOpenGL {

  public PGraphics3D() {
    super();
  }


  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES


  @Override
  public boolean is2D() {
    return false;
  }


  @Override
  public boolean is3D() {
    return true;
  }


  //////////////////////////////////////////////////////////////

  // PROJECTION


  @Override
  protected void defaultPerspective() {
    perspective();
  }


  //////////////////////////////////////////////////////////////

  // CAMERA


  @Override
  protected void defaultCamera() {
    camera();
  }


  //////////////////////////////////////////////////////////////

  // MATRIX MORE!


  @Override
  protected void begin2D() {
    pushProjection();
    ortho(0, width, 0, height, -1, +1);
    pushMatrix();
    camera(width/2, height/2);
  }


  @Override
  protected void end2D() {
    popMatrix();
    popProjection();
  }


  //////////////////////////////////////////////////////////////

  // SHAPE I/O


  static protected boolean isSupportedExtension(String extension) {
    return extension.equals("obj");
  }


  static protected PShape loadShapeImpl(PGraphics pg, String filename,
                                                      String extension) {
    PShapeOBJ obj = null;

    if (extension.equals("obj")) {
      obj = new PShapeOBJ(pg.parent, filename);

    } else if (extension.equals("objz")) {
      try {
        InputStream input =
          new GZIPInputStream(pg.parent.createInput(filename));
        obj = new PShapeOBJ(pg.parent, PApplet.createReader(input));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (obj != null) {
      int prevTextureMode = pg.textureMode;
      pg.textureMode = NORMAL;
      PShapeOpenGL p3d = PShapeOpenGL.createShape3D(pg.parent, obj);
      pg.textureMode = prevTextureMode;
      return p3d;
    } else {
      return null;
    }
  }


  //////////////////////////////////////////////////////////////

  // SHAPE CREATION


  @Override
  public PShape createShape(PShape source) {
    return PShapeOpenGL.createShape3D(parent, source);
  }


  @Override
  public PShape createShape() {
    return createShape(PShape.GEOMETRY);
  }


  @Override
  public PShape createShape(int type) {
    return createShapeImpl(parent, type);
  }


  @Override
  public PShape createShape(int kind, float... p) {
    return createShapeImpl(parent, kind, p);
  }


  static protected PShapeOpenGL createShapeImpl(PApplet parent, int type) {
    PShapeOpenGL shape = null;
    if (type == PConstants.GROUP) {
      shape = new PShapeOpenGL(parent, PConstants.GROUP);
    } else if (type == PShape.PATH) {
      shape = new PShapeOpenGL(parent, PShape.PATH);
    } else if (type == PShape.GEOMETRY) {
      shape = new PShapeOpenGL(parent, PShape.GEOMETRY);
    }

    /*
    (type == POINTS) {
      shape = new PShapeOpenGL(parent, PShape.GEOMETRY);

      shape.setKind(POINTS);
    } else if (type == LINES) {
      shape = new PShapeOpenGL(parent, PShape.GEOMETRY);

      shape.setKind(LINES);
    } else if (type == TRIANGLE || type == TRIANGLES) {
      shape = new PShapeOpenGL(parent, PShape.GEOMETRY);

      shape.setKind(TRIANGLES);
    } else if (type == TRIANGLE_FAN) {
      shape = new PShapeOpenGL(parent, PShape.GEOMETRY);
      shape.setKind(TRIANGLE_FAN);
    } else if (type == TRIANGLE_STRIP) {
      shape = new PShapeOpenGL(parent, PShape.GEOMETRY);
      shape.setKind(TRIANGLE_STRIP);
    } else if (type == QUAD || type == QUADS) {
      shape = new PShapeOpenGL(parent, PShape.GEOMETRY);
      shape.setKind(QUADS);
    } else if (type == QUAD_STRIP) {
      shape = new PShapeOpenGL(parent, PShape.GEOMETRY);
      shape.setKind(QUAD_STRIP);
    } else if (type == POLYGON) {
      shape = new PShapeOpenGL(parent, PShape.GEOMETRY);
      shape.setKind(POLYGON);
    }
    */

    shape.is3D(true);
    return shape;
  }


  static protected PShapeOpenGL createShapeImpl(PApplet parent,
                                                int kind, float... p) {
    PShapeOpenGL shape = null;
    int len = p.length;

    if (kind == POINT) {
      if (len != 2 && len != 3) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShapeOpenGL(parent, PShape.PRIMITIVE);
      shape.setKind(POINT);
    } else if (kind == LINE) {
      if (len != 4 && len != 6) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShapeOpenGL(parent, PShape.PRIMITIVE);
      shape.setKind(LINE);
    } else if (kind == TRIANGLE) {
      if (len != 6) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShapeOpenGL(parent, PShape.PRIMITIVE);
      shape.setKind(TRIANGLE);
    } else if (kind == QUAD) {
      if (len != 8) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShapeOpenGL(parent, PShape.PRIMITIVE);
      shape.setKind(QUAD);
    } else if (kind == RECT) {
      if (len != 4 && len != 5 && len != 8) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShapeOpenGL(parent, PShape.PRIMITIVE);
      shape.setKind(RECT);
    } else if (kind == ELLIPSE) {
      if (len != 4) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShapeOpenGL(parent, PShape.PRIMITIVE);
      shape.setKind(ELLIPSE);
    } else if (kind == ARC) {
      if (len != 6 && len != 7) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShapeOpenGL(parent, PShape.PRIMITIVE);
      shape.setKind(ARC);
    } else if (kind == BOX) {
      if (len != 1 && len != 3) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShapeOpenGL(parent, PShape.PRIMITIVE);
      shape.setKind(BOX);
    } else if (kind == SPHERE) {
      if (len != 1) {
        showWarning("Wrong number of parameters");
        return null;
      }
      shape = new PShapeOpenGL(parent, PShape.PRIMITIVE);
      shape.setKind(SPHERE);
    } else {
      showWarning("Unrecognized primitive type");
    }

    if (shape != null) {
      shape.setParams(p);
    }

    shape.is3D(true);
    return shape;
  }
}