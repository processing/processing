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

import java.nio.IntBuffer;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.Path;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;


public class PGraphicsFX2D extends PGraphics {
  GraphicsContext context;

  static final WritablePixelFormat<IntBuffer> argbFormat =
    PixelFormat.getIntArgbInstance();

  Path workPath;
  Path auxPath;
  boolean openContour;
  /// break the shape at the next vertex (next vertex() call is a moveto())
  boolean breakShape;

  /// coordinates for internal curve calculation
  float[] curveCoordX;
  float[] curveCoordY;
  float[] curveDrawX;
  float[] curveDrawY;

  int transformCount;
  Affine transformStack[] = new Affine[MATRIX_STACK_DEPTH];

//  Line2D.Float line = new Line2D.Float();
//  Ellipse2D.Float ellipse = new Ellipse2D.Float();
//  Rectangle2D.Float rect = new Rectangle2D.Float();
//  Arc2D.Float arc = new Arc2D.Float();
//
//  protected Color tintColorObject;
//
//  protected Color fillColorObject;
//  public boolean fillGradient;
//  public Paint fillGradientObject;
//
//  protected Color strokeColorObject;
//  public boolean strokeGradient;
//  public Paint strokeGradientObject;



  //////////////////////////////////////////////////////////////

  // INTERNAL


  public PGraphicsFX2D() { }


  //public void setParent(PApplet parent)


  //public void setPrimary(boolean primary)


  //public void setPath(String path)


  //public void setSize(int width, int height)


  //public void dispose()


  @Override
  public PSurface createSurface() {
    return surface = new PSurfaceFX(this);
  }


  //////////////////////////////////////////////////////////////

  // FRAME


  @Override
  public boolean canDraw() {
    return true;
  }


  @Override
  public void beginDraw() {
    checkSettings();
    resetMatrix(); // reset model matrix
    vertexCount = 0;
  }


  @Override
  public void endDraw() {
    if (!primarySurface) {
      // TODO this is probably overkill for most tasks...
      loadPixels();
    }

    // Marks pixels as modified so that the pixels will be updated.
    // Also sets mx1/y1/x2/y2 so that OpenGL will pick it up.
    setModified();

    //g2.dispose();
  }



  //////////////////////////////////////////////////////////////

  // SETTINGS


  //protected void checkSettings()


  //protected void defaultSettings()


  //protected void reapplySettings()



  //////////////////////////////////////////////////////////////

  // HINT


  //public void hint(int which)



  //////////////////////////////////////////////////////////////

  // SHAPE CREATION


  //protected PShape createShapeFamily(int type)


  //protected PShape createShapePrimitive(int kind, float... p)



  //////////////////////////////////////////////////////////////

  // SHAPE


  @Override
  public void beginShape(int kind) {
    shape = kind;
    vertexCount = 0;
    curveVertexCount = 0;

    // set gpath to null, because when mixing curves and straight
    // lines, vertexCount will be set back to zero, so vertexCount == 1
    // is no longer a good indicator of whether the shape is new.
    // this way, just check to see if gpath is null, and if it isn't
    // then just use it to continue the shape.
    workPath = null;
    auxPath = null;
  }


  //public boolean edge(boolean e)


  //public void normal(float nx, float ny, float nz) {


  //public void textureMode(int mode)


  @Override
  public void texture(PImage image) {
    showMethodWarning("texture");
  }


  @Override
  public void vertex(float x, float y) {
    curveVertexCount = 0;
    //float vertex[];

    if (vertexCount == vertices.length) {
      float temp[][] = new float[vertexCount<<1][VERTEX_FIELD_COUNT];
      System.arraycopy(vertices, 0, temp, 0, vertexCount);
      vertices = temp;
      //message(CHATTER, "allocating more vertices " + vertices.length);
    }
    // not everyone needs this, but just easier to store rather
    // than adding another moving part to the code...
    vertices[vertexCount][X] = x;
    vertices[vertexCount][Y] = y;
    vertexCount++;

    switch (shape) {

    case POINTS:
      point(x, y);
      break;

    case LINES:
      if ((vertexCount % 2) == 0) {
        line(vertices[vertexCount-2][X],
             vertices[vertexCount-2][Y], x, y);
      }
      break;

    case TRIANGLES:
      if ((vertexCount % 3) == 0) {
        triangle(vertices[vertexCount - 3][X],
                 vertices[vertexCount - 3][Y],
                 vertices[vertexCount - 2][X],
                 vertices[vertexCount - 2][Y],
                 x, y);
      }
      break;

    case TRIANGLE_STRIP:
      if (vertexCount >= 3) {
        triangle(vertices[vertexCount - 2][X],
                 vertices[vertexCount - 2][Y],
                 vertices[vertexCount - 1][X],
                 vertices[vertexCount - 1][Y],
                 vertices[vertexCount - 3][X],
                 vertices[vertexCount - 3][Y]);
      }
      break;

    case TRIANGLE_FAN:
      if (vertexCount >= 3) {
        // This is an unfortunate implementation because the stroke for an
        // adjacent triangle will be repeated. However, if the stroke is not
        // redrawn, it will replace the adjacent line (when it lines up
        // perfectly) or show a faint line (when off by a small amount).
        // The alternative would be to wait, then draw the shape as a
        // polygon fill, followed by a series of vertices. But that's a
        // poor method when used with PDF, DXF, or other recording objects,
        // since discrete triangles would likely be preferred.
        triangle(vertices[0][X],
                 vertices[0][Y],
                 vertices[vertexCount - 2][X],
                 vertices[vertexCount - 2][Y],
                 x, y);
      }
      break;

    case QUAD:
    case QUADS:
      if ((vertexCount % 4) == 0) {
        quad(vertices[vertexCount - 4][X],
             vertices[vertexCount - 4][Y],
             vertices[vertexCount - 3][X],
             vertices[vertexCount - 3][Y],
             vertices[vertexCount - 2][X],
             vertices[vertexCount - 2][Y],
             x, y);
      }
      break;

    case QUAD_STRIP:
      // 0---2---4
      // |   |   |
      // 1---3---5
      if ((vertexCount >= 4) && ((vertexCount % 2) == 0)) {
        quad(vertices[vertexCount - 4][X],
             vertices[vertexCount - 4][Y],
             vertices[vertexCount - 2][X],
             vertices[vertexCount - 2][Y],
             x, y,
             vertices[vertexCount - 3][X],
             vertices[vertexCount - 3][Y]);
      }
      break;

    case POLYGON:
      if (workPath == null) {
        context.moveTo(x, y);
      } else if (breakShape) {
        context.moveTo(x, y);
        breakShape = false;
      } else {
        context.lineTo(x, y);
      }
      break;
    }
  }


  @Override
  public void vertex(float x, float y, float z) {
    showDepthWarningXYZ("vertex");
  }


  @Override
  public void vertex(float[] v) {
    vertex(v[X], v[Y]);
  }


  @Override
  public void vertex(float x, float y, float u, float v) {
    showVariationWarning("vertex(x, y, u, v)");
  }


  @Override
  public void vertex(float x, float y, float z, float u, float v) {
    showDepthWarningXYZ("vertex");
  }


  @Override
  public void beginContour() {
    if (openContour) {
      PGraphics.showWarning("Already called beginContour()");
      return;
    }

    // draw contours to auxiliary path so main path can be closed later
    Path temp = auxPath;
    auxPath = workPath;
    workPath = temp;

//    if (auxPath != null) {  // first contour does not break
    breakShape = true;
    auxPath = new Path();
//    }

    breakShape = true;
    openContour = true;
  }


  @Override
  public void endContour() {
    if (!openContour) {
      PGraphics.showWarning("Need to call beginContour() first");
      return;
    }

    // close this contour
    if (workPath != null) {
      //gpath.closePath();
      auxPath.getElements().addAll(workPath.getElements());
      auxPath.getElements().add(new ClosePath());
    }

    // switch back to main path
    Path temp = workPath;
    workPath = auxPath;
    auxPath = temp;

    openContour = false;
  }


  @Override
  public void endShape(int mode) {
    if (openContour) { // correct automagically, notify user
      endContour();
      PGraphics.showWarning("Missing endContour() before endShape()");
    }
    if (workPath != null) {  // make sure something has been drawn
      if (shape == POLYGON) {
        if (mode == CLOSE) {
          //gpath.closePath();
          workPath.getElements().add(new ClosePath());
        }
        if (auxPath != null) {
          //gpath.append(auxPath, false);
          workPath.getElements().addAll(auxPath.getElements());
        }
        //drawShape(gpath);
        // TODO argh, can't go this route
      }
    }
    shape = 0;
  }



  //////////////////////////////////////////////////////////////

  // CLIPPING


  @Override
  protected void clipImpl(float x1, float y1, float x2, float y2) {
    //g2.setClip(new Rectangle2D.Float(x1, y1, x2 - x1, y2 - y1));
    showTodoWarning("clip()", 3274);
  }


  @Override
  public void noClip() {
    //g2.setClip(null);
    showTodoWarning("noClip()", 3274);
  }



  //////////////////////////////////////////////////////////////

  // BLEND


  @Override
  protected void blendModeImpl() {
    BlendMode mode = BlendMode.SRC_OVER;
    switch (blendMode) {
      case REPLACE: showWarning("blendMode(REPLACE) is not supported"); break;
      case BLEND: break;  // this is SRC_OVER, the default
      case ADD: mode = BlendMode.ADD; break; // everyone's favorite
      case SUBTRACT: showWarning("blendMode(SUBTRACT) is not supported"); break;
      case LIGHTEST: mode = BlendMode.LIGHTEN; break;
      case DARKEST: mode = BlendMode.DARKEN; break;
      case DIFFERENCE: mode = BlendMode.DIFFERENCE; break;
      case EXCLUSION: mode = BlendMode.EXCLUSION; break;
      case MULTIPLY: mode = BlendMode.MULTIPLY; break;
      case SCREEN: mode = BlendMode.SCREEN; break;
      case OVERLAY: mode = BlendMode.OVERLAY; break;
      case HARD_LIGHT: mode = BlendMode.HARD_LIGHT; break;
      case SOFT_LIGHT: mode = BlendMode.SOFT_LIGHT; break;
      case DODGE: mode = BlendMode.COLOR_DODGE; break;
      case BURN: mode = BlendMode.COLOR_BURN; break;
    }
    context.setGlobalBlendMode(mode);
  }



  //////////////////////////////////////////////////////////////

  // BEZIER VERTICES


  @Override
  public void bezierVertex(float x1, float y1,
                           float x2, float y2,
                           float x3, float y3) {
    bezierVertexCheck();
    context.bezierCurveTo(x1, y1, x2, y2, x3, y3);

  }


  @Override
  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    showDepthWarningXYZ("bezierVertex");
  }



  //////////////////////////////////////////////////////////////

  // QUADRATIC BEZIER VERTICES


  @Override
  public void quadraticVertex(float ctrlX, float ctrlY,
                              float endX, float endY) {
    context.quadraticCurveTo(ctrlX, ctrlY, endX, endY);
  }


  @Override
  public void quadraticVertex(float x2, float y2, float z2,
                         float x4, float y4, float z4) {
    showDepthWarningXYZ("quadVertex");
  }



  //////////////////////////////////////////////////////////////

  // CURVE VERTICES


  @Override
  protected void curveVertexCheck() {
    super.curveVertexCheck();

    if (curveCoordX == null) {
      curveCoordX = new float[4];
      curveCoordY = new float[4];
      curveDrawX = new float[4];
      curveDrawY = new float[4];
    }
  }


  @Override
  protected void curveVertexSegment(float x1, float y1,
                                    float x2, float y2,
                                    float x3, float y3,
                                    float x4, float y4) {
    curveCoordX[0] = x1;
    curveCoordY[0] = y1;

    curveCoordX[1] = x2;
    curveCoordY[1] = y2;

    curveCoordX[2] = x3;
    curveCoordY[2] = y3;

    curveCoordX[3] = x4;
    curveCoordY[3] = y4;

    curveToBezierMatrix.mult(curveCoordX, curveDrawX);
    curveToBezierMatrix.mult(curveCoordY, curveDrawY);

    // since the paths are continuous,
    // only the first point needs the actual moveto
    if (workPath == null) {
//      gpath = new GeneralPath();
      context.moveTo(curveDrawX[0], curveDrawY[0]);
    }

    context.bezierCurveTo(curveDrawX[1], curveDrawY[1],
                          curveDrawX[2], curveDrawY[2],
                          curveDrawX[3], curveDrawY[3]);
  }


  @Override
  public void curveVertex(float x, float y, float z) {
    showDepthWarningXYZ("curveVertex");
  }



  //////////////////////////////////////////////////////////////

  // RENDERER


  //public void flush()



  //////////////////////////////////////////////////////////////

  // POINT, LINE, TRIANGLE, QUAD


  @Override
  public void point(float x, float y) {
    if (stroke) {
//      if (strokeWeight > 1) {
      line(x, y, x + EPSILON, y + EPSILON);
//      } else {
//        set((int) screenX(x, y), (int) screenY(x, y), strokeColor);
//      }
    }
  }


  @Override
  public void line(float x1, float y1, float x2, float y2) {
    context.strokeLine(x1, y1, x2, y2);
  }


  @Override
  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    context.beginPath();
    context.moveTo(x1, y1);
    context.lineTo(x2, y2);
    context.lineTo(x3, y3);
    context.closePath();
    if (fill) context.fill();
    if (stroke) context.stroke();
  }


  @Override
  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
    context.beginPath();
    context.moveTo(x1, y1);
    context.lineTo(x2, y2);
    context.lineTo(x3, y3);
    context.lineTo(x4, y4);
    context.closePath();
    if (fill) context.fill();
    if (stroke) context.stroke();
  }



  //////////////////////////////////////////////////////////////

  // RECT


  //public void rectMode(int mode)


  //public void rect(float a, float b, float c, float d)


  @Override
  protected void rectImpl(float x1, float y1, float x2, float y2) {
//    rect.setFrame(x1, y1, x2-x1, y2-y1);
//    drawShape(rect);
    if (fill) context.fillRect(x1, y1, x2, y2);
    if (stroke) context.strokeRect(x1, y1, x2, y2);
  }



  //////////////////////////////////////////////////////////////

  // ELLIPSE


  //public void ellipseMode(int mode)


  //public void ellipse(float a, float b, float c, float d)


  @Override
  protected void ellipseImpl(float x, float y, float w, float h) {
//    ellipse.setFrame(x, y, w, h);
//    drawShape(ellipse);
    if (fill) context.fillOval(x, y, w, h);
    if (stroke) context.strokeOval(x, y, w, h);
  }



  //////////////////////////////////////////////////////////////

  // ARC


  //public void arc(float a, float b, float c, float d,
  //                float start, float stop)


  @Override
  protected void arcImpl(float x, float y, float w, float h,
                         float start, float stop, int mode) {
    // 0 to 90 in java would be 0 to -90 for p5 renderer
    // but that won't work, so -90 to 0?

    start = -start * RAD_TO_DEG;
    stop = -stop * RAD_TO_DEG;

    float sweep = stop - start;

    // The defaults, before 2.0b7, were to stroke as Arc2D.OPEN, and then fill
    // using Arc2D.PIE. That's a little wonky, but it's here for compatability.
    ArcType fillMode = ArcType.ROUND;  // Arc2D.PIE
    ArcType strokeMode = ArcType.OPEN;

    if (mode == OPEN) {
      fillMode = ArcType.OPEN;

    } else if (mode == PIE) {
      strokeMode = ArcType.ROUND;  // PIE

    } else if (mode == CHORD) {
      fillMode = ArcType.CHORD;
      strokeMode = ArcType.CHORD;
    }

    if (fill) {
      context.fillArc(x, y, w, h, PApplet.degrees(start), PApplet.degrees(sweep), fillMode);
    }
    if (stroke) {
      context.strokeArc(x, y, w, h, PApplet.degrees(start), PApplet.degrees(sweep), strokeMode);
    }
  }



  //////////////////////////////////////////////////////////////

  // BOX


  //public void box(float size)


  @Override
  public void box(float w, float h, float d) {
    showMethodWarning("box");
  }



  //////////////////////////////////////////////////////////////

  // SPHERE


  //public void sphereDetail(int res)


  //public void sphereDetail(int ures, int vres)


  @Override
  public void sphere(float r) {
    showMethodWarning("sphere");
  }



  //////////////////////////////////////////////////////////////

  // BEZIER


  //public float bezierPoint(float a, float b, float c, float d, float t)


  //public float bezierTangent(float a, float b, float c, float d, float t)


  //protected void bezierInitCheck()


  //protected void bezierInit()


  /** Ignored (not needed) by this renderer. */
  @Override
  public void bezierDetail(int detail) { }


  //public void bezier(float x1, float y1,
  //                   float x2, float y2,
  //                   float x3, float y3,
  //                   float x4, float y4)


  //public void bezier(float x1, float y1, float z1,
  //                   float x2, float y2, float z2,
  //                   float x3, float y3, float z3,
  //                   float x4, float y4, float z4)



  //////////////////////////////////////////////////////////////

  // CURVE


  //public float curvePoint(float a, float b, float c, float d, float t)


  //public float curveTangent(float a, float b, float c, float d, float t)


  /** Ignored (not needed) by this renderer. */
  @Override
  public void curveDetail(int detail) { }


  //public void curveTightness(float tightness)


  //protected void curveInitCheck()


  //protected void curveInit()


  //public void curve(float x1, float y1,
  //                  float x2, float y2,
  //                  float x3, float y3,
  //                  float x4, float y4)


  //public void curve(float x1, float y1, float z1,
  //                  float x2, float y2, float z2,
  //                  float x3, float y3, float z3,
  //                  float x4, float y4, float z4)



  //////////////////////////////////////////////////////////////

  // SMOOTH


//  @Override
//  public void smooth() {
//    smooth = true;
//
//    if (quality == 0) {
//      quality = 4;  // change back to bicubic
//    }
//  }


  @Override
  public void smooth(int quality) {
//    this.quality = quality;
//    if (quality == 0) {
//      noSmooth();
//    } else {
//      smooth();
//    }
    showMissingWarning("smooth");
  }


  @Override
  public void noSmooth() {
    showMissingWarning("noSmooth");
  }



  //////////////////////////////////////////////////////////////

  // IMAGE


  //public void imageMode(int mode)


  //public void image(PImage image, float x, float y)


  //public void image(PImage image, float x, float y, float c, float d)


  //public void image(PImage image,
  //                  float a, float b, float c, float d,
  //                  int u1, int v1, int u2, int v2)


  /**
   * Handle renderer-specific image drawing.
   */
  @Override
  protected void imageImpl(PImage who,
                           float x1, float y1, float x2, float y2,
                           int u1, int v1, int u2, int v2) {
    // Image not ready yet, or an error
    if (who.width <= 0 || who.height <= 0) return;

    ImageCache cash = (ImageCache) getCache(who);

    // Nuke the cache if the image was resized
    if (cash != null) {
      if (who.width != cash.image.getWidth() ||
          who.height != cash.image.getHeight()) {
        cash = null;
      }
    }

    if (cash == null) {
      //System.out.println("making new image cache");
      cash = new ImageCache(); //who);
      setCache(who, cash);
      who.updatePixels();  // mark the whole thing for update
      who.modified = true;
    }

    // If image previously was tinted, or the color changed
    // or the image was tinted, and tint is now disabled
    if ((tint && !cash.tinted) ||
        (tint && (cash.tintedColor != tintColor)) ||
        (!tint && cash.tinted)) {
      // For tint change, mark all pixels as needing update.
      who.updatePixels();
    }

    if (who.modified) {
      if (who.pixels == null) {
        // This might be a PGraphics that hasn't been drawn to yet.
        // Can't just bail because the cache has been created above.
        // https://github.com/processing/processing/issues/2208
        who.pixels = new int[who.width * who.height];
      }
      cash.update(who, tint, tintColor);
      who.modified = false;
    }

    context.drawImage(((ImageCache) getCache(who)).image,
                      u1, v1, u2-u1, v2-v1,
                      x1, y1, x2-x1, y2-y1);
  }


  static class ImageCache {
    boolean tinted;
    int tintedColor;
    int[] tintedTemp;  // one row of tinted pixels
    //BufferedImage image;
    WritableImage image;

    /**
     * Update the pixels of the cache image. Already determined that the tint
     * has changed, or the pixels have changed, so should just go through
     * with the update without further checks.
     */
    public void update(PImage source, boolean tint, int tintColor) {
      //int bufferType = BufferedImage.TYPE_INT_ARGB;
      int targetType = ARGB;
      boolean opaque = (tintColor & 0xFF000000) == 0xFF000000;
      if (source.format == RGB) {
        if (!tint || (tint && opaque)) {
          //bufferType = BufferedImage.TYPE_INT_RGB;
          targetType = RGB;
        }
      }
//      boolean wrongType = (image != null) && (image.getType() != bufferType);
//      if ((image == null) || wrongType) {
//        image = new BufferedImage(source.width, source.height, bufferType);
//      }
      // Must always use an ARGB image, otherwise will write zeros
      // in the alpha channel when drawn to the screen.
      // https://github.com/processing/processing/issues/2030
//      if (image == null) {
//        image = new BufferedImage(source.width, source.height,
//                                  BufferedImage.TYPE_INT_ARGB);
//      }
      if (image == null) {
        image = new WritableImage(source.width, source.height);
      }

      //WritableRaster wr = image.getRaster();
      PixelWriter pw = image.getPixelWriter();
      if (tint) {
        if (tintedTemp == null || tintedTemp.length != source.width) {
          tintedTemp = new int[source.width];
        }
        int a2 = (tintColor >> 24) & 0xff;
//        System.out.println("tint color is " + a2);
//        System.out.println("source.pixels[0] alpha is " + (source.pixels[0] >>> 24));
        int r2 = (tintColor >> 16) & 0xff;
        int g2 = (tintColor >> 8) & 0xff;
        int b2 = (tintColor) & 0xff;

        //if (bufferType == BufferedImage.TYPE_INT_RGB) {
        if (targetType == RGB) {
          // The target image is opaque, meaning that the source image has no
          // alpha (is not ARGB), and the tint has no alpha.
          int index = 0;
          for (int y = 0; y < source.height; y++) {
            for (int x = 0; x < source.width; x++) {
              int argb1 = source.pixels[index++];
              int r1 = (argb1 >> 16) & 0xff;
              int g1 = (argb1 >> 8) & 0xff;
              int b1 = (argb1) & 0xff;

              // Prior to 2.1, the alpha channel was commented out here,
              // but can't remember why (just thought unnecessary b/c of RGB?)
              // https://github.com/processing/processing/issues/2030
              tintedTemp[x] = 0xFF000000 |
                  (((r2 * r1) & 0xff00) << 8) |
                  ((g2 * g1) & 0xff00) |
                  (((b2 * b1) & 0xff00) >> 8);
            }
            //wr.setDataElements(0, y, source.width, 1, tintedTemp);
            pw.setPixels(0, y, source.width, 1, argbFormat, tintedTemp, 0, source.width);
          }
          // could this be any slower?
//          float[] scales = { tintR, tintG, tintB };
//          float[] offsets = new float[3];
//          RescaleOp op = new RescaleOp(scales, offsets, null);
//          op.filter(image, image);

        //} else if (bufferType == BufferedImage.TYPE_INT_ARGB) {
        } else if (targetType == ARGB) {
          if (source.format == RGB &&
              (tintColor & 0xffffff) == 0xffffff) {
            int hi = tintColor & 0xff000000;
            int index = 0;
            for (int y = 0; y < source.height; y++) {
              for (int x = 0; x < source.width; x++) {
                tintedTemp[x] = hi | (source.pixels[index++] & 0xFFFFFF);
              }
              //wr.setDataElements(0, y, source.width, 1, tintedTemp);
              pw.setPixels(0, y, source.width, 1, argbFormat, tintedTemp, 0, source.width);
            }
          } else {
            int index = 0;
            for (int y = 0; y < source.height; y++) {
              if (source.format == RGB) {
                int alpha = tintColor & 0xFF000000;
                for (int x = 0; x < source.width; x++) {
                  int argb1 = source.pixels[index++];
                  int r1 = (argb1 >> 16) & 0xff;
                  int g1 = (argb1 >> 8) & 0xff;
                  int b1 = (argb1) & 0xff;
                  tintedTemp[x] = alpha |
                      (((r2 * r1) & 0xff00) << 8) |
                      ((g2 * g1) & 0xff00) |
                      (((b2 * b1) & 0xff00) >> 8);
                }
              } else if (source.format == ARGB) {
                for (int x = 0; x < source.width; x++) {
                  int argb1 = source.pixels[index++];
                  int a1 = (argb1 >> 24) & 0xff;
                  int r1 = (argb1 >> 16) & 0xff;
                  int g1 = (argb1 >> 8) & 0xff;
                  int b1 = (argb1) & 0xff;
                  tintedTemp[x] =
                      (((a2 * a1) & 0xff00) << 16) |
                      (((r2 * r1) & 0xff00) << 8) |
                      ((g2 * g1) & 0xff00) |
                      (((b2 * b1) & 0xff00) >> 8);
                }
              } else if (source.format == ALPHA) {
                int lower = tintColor & 0xFFFFFF;
                for (int x = 0; x < source.width; x++) {
                  int a1 = source.pixels[index++];
                  tintedTemp[x] =
                      (((a2 * a1) & 0xff00) << 16) | lower;
                }
              }
              //wr.setDataElements(0, y, source.width, 1, tintedTemp);
              pw.setPixels(0, y, source.width, 1, argbFormat, tintedTemp, 0, source.width);
            }
          }
          // Not sure why ARGB images take the scales in this order...
//          float[] scales = { tintR, tintG, tintB, tintA };
//          float[] offsets = new float[4];
//          RescaleOp op = new RescaleOp(scales, offsets, null);
//          op.filter(image, image);
        }
      } else {  // !tint
        if (targetType == RGB && (source.pixels[0] >> 24 == 0)) {
          // If it's an RGB image and the high bits aren't set, need to set
          // the high bits to opaque because we're drawing ARGB images.
          source.filter(OPAQUE);
          // Opting to just manipulate the image here, since it shouldn't
          // affect anything else (and alpha(get(x, y)) should return 0xff).
          // Wel also make no guarantees about the values of the pixels array
          // in a PImage and how the high bits will be set.
        }
        // If no tint, just shove the pixels on in there verbatim
        //wr.setDataElements(0, 0, source.width, source.height, source.pixels);
        //System.out.println("moving the big one");
        pw.setPixels(0, 0, source.width, source.height,
                     argbFormat, source.pixels, 0, source.width);
      }
      this.tinted = tint;
      this.tintedColor = tintColor;

//      GraphicsConfiguration gc = parent.getGraphicsConfiguration();
//      compat = gc.createCompatibleImage(image.getWidth(),
//                                        image.getHeight(),
//                                        Transparency.TRANSLUCENT);
//
//      Graphics2D g = compat.createGraphics();
//      g.drawImage(image, 0, 0, null);
//      g.dispose();
    }
  }



  //////////////////////////////////////////////////////////////

  // SHAPE


  //public void shapeMode(int mode)


  //public void shape(PShape shape)


  //public void shape(PShape shape, float x, float y)


  //public void shape(PShape shape, float x, float y, float c, float d)


  //////////////////////////////////////////////////////////////

  // SHAPE I/O


  @Override
  public PShape loadShape(String filename) {
    return loadShape(filename, null);
  }


  @Override
  public PShape loadShape(String filename, String options) {
    String extension = PApplet.getExtension(filename);
    if (extension.equals("svg") || extension.equals("svgz")) {
      return new PShapeSVG(parent.loadXML(filename));
    }
    PGraphics.showWarning("Unsupported format: " + filename);
    return null;
  }



//  //////////////////////////////////////////////////////////////
//
//  // TEXT ATTRIBTUES
//
//
//  //public void textAlign(int align)
//
//
//  //public void textAlign(int alignX, int alignY)
//
//
//  @Override
//  public float textAscent() {
//    if (textFont == null) {
//      defaultFontOrDeath("textAscent");
//    }
//
//    Font font = (Font) textFont.getNative();
//    if (font != null) {
//      return getFontMetrics(font).getAscent();
//    }
//    return super.textAscent();
//  }
//
//
//  @Override
//  public float textDescent() {
//    if (textFont == null) {
//      defaultFontOrDeath("textDescent");
//    }
//    Font font = (Font) textFont.getNative();
//    if (font != null) {
//      return getFontMetrics(font).getDescent();
//    }
//    return super.textDescent();
//  }
//
//
//  //public void textFont(PFont which)
//
//
//  //public void textFont(PFont which, float size)
//
//
//  //public void textLeading(float leading)
//
//
//  //public void textMode(int mode)
//
//
//  @Override
//  protected boolean textModeCheck(int mode) {
//    return mode == MODEL;
//  }
//
//
//  /**
//   * Same as parent, but override for native version of the font.
//   * <p/>
//   * Also gets called by textFont, so the metrics
//   * will get recorded properly.
//   */
//  @Override
//  public void textSize(float size) {
//    if (textFont == null) {
//      defaultFontOrDeath("textSize", size);
//    }
//
//    // if a native version available, derive this font
////    if (textFontNative != null) {
////      textFontNative = textFontNative.deriveFont(size);
////      g2.setFont(textFontNative);
////      textFontNativeMetrics = g2.getFontMetrics(textFontNative);
////    }
//    Font font = (Font) textFont.getNative();
//    //if (font != null && (textFont.isStream() || hints[ENABLE_NATIVE_FONTS])) {
//    if (font != null) {
//      Map<TextAttribute, Object> map =
//        new HashMap<TextAttribute, Object>();
//      map.put(TextAttribute.SIZE, size);
//      map.put(TextAttribute.KERNING,
//              TextAttribute.KERNING_ON);
////      map.put(TextAttribute.TRACKING,
////              TextAttribute.TRACKING_TIGHT);
//      font = font.deriveFont(map);
//      g2.setFont(font);
//      textFont.setNative(font);
//
////      Font dfont = font.deriveFont(size);
//////      Map<TextAttribute, ?> attrs = dfont.getAttributes();
//////      for (TextAttribute ta : attrs.keySet()) {
//////        System.out.println(ta + " -> " + attrs.get(ta));
//////      }
////      g2.setFont(dfont);
////      textFont.setNative(dfont);
//    }
//
//    // take care of setting the textSize and textLeading vars
//    // this has to happen second, because it calls textAscent()
//    // (which requires the native font metrics to be set)
//    super.textSize(size);
//  }
//
//
//  //public float textWidth(char c)
//
//
//  //public float textWidth(String str)
//
//
//  @Override
//  protected float textWidthImpl(char buffer[], int start, int stop) {
//    if (textFont == null) {
//      defaultFontOrDeath("textWidth");
//    }
//
//    Font font = (Font) textFont.getNative();
//    //if (font != null && (textFont.isStream() || hints[ENABLE_NATIVE_FONTS])) {
//    if (font != null) {
//      // maybe should use one of the newer/fancier functions for this?
//      int length = stop - start;
//      FontMetrics metrics = getFontMetrics(font);
//      // Using fractional metrics makes the measurement worse, not better,
//      // at least on OS X 10.6 (November, 2010).
//      // TextLayout returns the same value as charsWidth().
////      System.err.println("using native");
////      g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
////                          RenderingHints.VALUE_FRACTIONALMETRICS_ON);
////      float m1 = metrics.charsWidth(buffer, start, length);
////      //float m2 = (float) metrics.getStringBounds(buffer, start, stop, g2).getWidth();
////      TextLayout tl = new TextLayout(new String(buffer, start, length), font, g2.getFontRenderContext());
////      float m2 = (float) tl.getBounds().getWidth();
////      System.err.println(m1 + " " + m2);
//////      return m1;
////      return m2;
//      return metrics.charsWidth(buffer, start, length);
//    }
////    System.err.println("not native");
//    return super.textWidthImpl(buffer, start, stop);
//  }
//
//
////  protected void beginTextScreenMode() {
////    loadPixels();
////  }
//
//
////  protected void endTextScreenMode() {
////    updatePixels();
////  }
//
//
//  //////////////////////////////////////////////////////////////
//
//  // TEXT
//
//  // None of the variations of text() are overridden from PGraphics.
//
//
//
//  //////////////////////////////////////////////////////////////
//
//  // TEXT IMPL
//
//
//  //protected void textLineAlignImpl(char buffer[], int start, int stop,
//  //                                 float x, float y)
//
//
//  @Override
//  protected void textLineImpl(char buffer[], int start, int stop,
//                              float x, float y) {
//    Font font = (Font) textFont.getNative();
////    if (font != null && (textFont.isStream() || hints[ENABLE_NATIVE_FONTS])) {
//    if (font != null) {
//      /*
//      // save the current setting for text smoothing. note that this is
//      // different from the smooth() function, because the font smoothing
//      // is controlled when the font is created, not now as it's drawn.
//      // fixed a bug in 0116 that handled this incorrectly.
//      Object textAntialias =
//        g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
//
//      // override the current text smoothing setting based on the font
//      // (don't change the global smoothing settings)
//      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
//                          textFont.smooth ?
//                          RenderingHints.VALUE_ANTIALIAS_ON :
//                          RenderingHints.VALUE_ANTIALIAS_OFF);
//      */
//      Object antialias =
//        g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
//      if (antialias == null) {
//        // if smooth() and noSmooth() not called, this will be null (0120)
//        antialias = RenderingHints.VALUE_ANTIALIAS_DEFAULT;
//      }
//
//      // override the current smoothing setting based on the font
//      // also changes global setting for antialiasing, but this is because it's
//      // not possible to enable/disable them independently in some situations.
//      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                          textFont.smooth ?
//                          RenderingHints.VALUE_ANTIALIAS_ON :
//                          RenderingHints.VALUE_ANTIALIAS_OFF);
//
//      g2.setColor(fillColorObject);
//
//      int length = stop - start;
//      if (length != 0) {
//      g2.drawChars(buffer, start, length, (int) (x + 0.5f), (int) (y + 0.5f));
//      // better to use round here? also, drawChars now just calls drawString
////      g2.drawString(new String(buffer, start, stop - start), Math.round(x), Math.round(y));
//
//      // better to use drawString() with floats? (nope, draws the same)
//      //g2.drawString(new String(buffer, start, length), x, y);
//
//      // this didn't seem to help the scaling issue, and creates garbage
//      // because of a fairly heavyweight new temporary object
////      java.awt.font.GlyphVector gv =
////        font.createGlyphVector(g2.getFontRenderContext(), new String(buffer, start, stop - start));
////      g2.drawGlyphVector(gv, x, y);
//      }
//
//      // return to previous smoothing state if it was changed
//      //g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAntialias);
//      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias);
//
//    } else {  // otherwise just do the default
//      super.textLineImpl(buffer, start, stop, x, y);
//    }
//  }
//
//
//  @Override
//  public FontMetrics getFontMetrics(Font font) {
//    return (g2 != null) ? g2.getFontMetrics(font) : super.getFontMetrics(font);
//  }


  //////////////////////////////////////////////////////////////

  // MATRIX STACK


  @Override
  public void pushMatrix() {
    if (transformCount == transformStack.length) {
      throw new RuntimeException("pushMatrix() cannot use push more than " +
                                 transformStack.length + " times");
    }
    context.getTransform(transformStack[transformCount]);
    transformCount++;
  }


  @Override
  public void popMatrix() {
    if (transformCount == 0) {
      throw new RuntimeException("missing a pushMatrix() " +
                                 "to go with that popMatrix()");
    }
    transformCount--;
    context.setTransform(transformStack[transformCount]);
  }



  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMS


  @Override
  public void translate(float tx, float ty) {
    context.translate(tx, ty);
  }


  //public void translate(float tx, float ty, float tz)


  @Override
  public void rotate(float angle) {
    context.rotate(PApplet.radians(angle));
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
  public void scale(float s) {
    context.scale(s, s);
  }


  @Override
  public void scale(float sx, float sy) {
    context.scale(sx, sy);
  }


  @Override
  public void scale(float sx, float sy, float sz) {
    showDepthWarningXYZ("scale");
  }


  @Override
  public void shearX(float angle) {
    Affine temp = new Affine();
    temp.appendShear(Math.tan(angle), 0);
    context.transform(temp);
  }


  @Override
  public void shearY(float angle) {
    Affine temp = new Affine();
    temp.appendShear(0, Math.tan(angle));
    context.transform(temp);
  }



  //////////////////////////////////////////////////////////////

  // MATRIX MORE


  @Override
  public void resetMatrix() {
    context.setTransform(new Affine());
  }


  //public void applyMatrix(PMatrix2D source)


  @Override
  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    context.transform(n00, n10, n01, n11, n02, n12);
  }


  //public void applyMatrix(PMatrix3D source)


  @Override
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    showVariationWarning("applyMatrix");
  }



  //////////////////////////////////////////////////////////////

  // MATRIX GET/SET


  @Override
  public PMatrix getMatrix() {
    return getMatrix((PMatrix2D) null);
  }


  @Override
  public PMatrix2D getMatrix(PMatrix2D target) {
    if (target == null) {
      target = new PMatrix2D();
    }
    //double[] transform = new double[6];
    // TODO This is not tested; apparently Affine is a full 3x4
    Affine t = context.getTransform(); //.getMatrix(transform);
//    target.set((float) transform[0], (float) transform[2], (float) transform[4],
//               (float) transform[1], (float) transform[3], (float) transform[5]);
    target.set((float) t.getMxx(), (float) t.getMxy(), (float) t.getTx(),
               (float) t.getMyx(), (float) t.getMyy(), (float) t.getTy());
    return target;
  }


  @Override
  public PMatrix3D getMatrix(PMatrix3D target) {
    showVariationWarning("getMatrix");
    return target;
  }


  //public void setMatrix(PMatrix source)


  @Override
  public void setMatrix(PMatrix2D source) {
    context.setTransform(source.m00, source.m10,
                         source.m01, source.m11,
                         source.m02, source.m12);
  }


  @Override
  public void setMatrix(PMatrix3D source) {
    showVariationWarning("setMatrix");
  }


  @Override
  public void printMatrix() {
    getMatrix((PMatrix2D) null).print();
  }



//  //////////////////////////////////////////////////////////////
//
//  // CAMERA and PROJECTION
//
//  // Inherit the plaintive warnings from PGraphics
//
//
//  //public void beginCamera()
//  //public void endCamera()
//  //public void camera()
//  //public void camera(float eyeX, float eyeY, float eyeZ,
//  //                   float centerX, float centerY, float centerZ,
//  //                   float upX, float upY, float upZ)
//  //public void printCamera()
//
//  //public void ortho()
//  //public void ortho(float left, float right,
//  //                  float bottom, float top,
//  //                  float near, float far)
//  //public void perspective()
//  //public void perspective(float fov, float aspect, float near, float far)
//  //public void frustum(float left, float right,
//  //                    float bottom, float top,
//  //                    float near, float far)
//  //public void printProjection()



  //////////////////////////////////////////////////////////////

  // SCREEN and MODEL transforms


  @Override
  public float screenX(float x, float y) {
    return (float) context.getTransform().transform(x, y).getX();
  }


  @Override
  public float screenY(float x, float y) {
    return (float) context.getTransform().transform(x, y).getY();
  }


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


  //public float modelX(float x, float y, float z)


  //public float modelY(float x, float y, float z)


  //public float modelZ(float x, float y, float z)



//  //////////////////////////////////////////////////////////////
//
//  // STYLE
//
//  // pushStyle(), popStyle(), style() and getStyle() inherited.



  //////////////////////////////////////////////////////////////

  // STROKE CAP/JOIN/WEIGHT


  @Override
  public void strokeCap(int cap) {
    super.strokeCap(cap);
    if (strokeCap == ROUND) {
      context.setLineCap(StrokeLineCap.ROUND);
    } else if (strokeCap == PROJECT) {
      context.setLineCap(StrokeLineCap.SQUARE);
    } else {
      context.setLineCap(StrokeLineCap.BUTT);
    }
  }


  @Override
  public void strokeJoin(int join) {
    super.strokeJoin(join);
    if (strokeJoin == MITER) {
      context.setLineJoin(StrokeLineJoin.MITER);
    } else if (strokeJoin == ROUND) {
      context.setLineJoin(StrokeLineJoin.ROUND);
    } else {
      context.setLineJoin(StrokeLineJoin.BEVEL);
    }
  }


  @Override
  public void strokeWeight(float weight) {
    super.strokeWeight(weight);
    context.setLineWidth(weight);
  }



  //////////////////////////////////////////////////////////////

  // STROKE

  // noStroke() and stroke() inherited from PGraphics.


  @Override
  protected void strokeFromCalc() {
    super.strokeFromCalc();
    context.setStroke(new Color(strokeR, strokeG, strokeB, strokeA));
  }



  //////////////////////////////////////////////////////////////

  // TINT

  // noTint() and tint() inherited from PGraphics.



  //////////////////////////////////////////////////////////////

  // FILL

  // noFill() and fill() inherited from PGraphics.


  @Override
  protected void fillFromCalc() {
    super.fillFromCalc();
    context.setFill(new Color(fillR, fillG, fillB, fillA));
  }



//  //////////////////////////////////////////////////////////////
//
//  // MATERIAL PROPERTIES
//
//
//  //public void ambient(int rgb)
//  //public void ambient(float gray)
//  //public void ambient(float x, float y, float z)
//  //protected void ambientFromCalc()
//  //public void specular(int rgb)
//  //public void specular(float gray)
//  //public void specular(float x, float y, float z)
//  //protected void specularFromCalc()
//  //public void shininess(float shine)
//  //public void emissive(int rgb)
//  //public void emissive(float gray)
//  //public void emissive(float x, float y, float z )
//  //protected void emissiveFromCalc()
//
//
//
//  //////////////////////////////////////////////////////////////
//
//  // LIGHTS
//
//
//  //public void lights()
//  //public void noLights()
//  //public void ambientLight(float red, float green, float blue)
//  //public void ambientLight(float red, float green, float blue,
//  //                         float x, float y, float z)
//  //public void directionalLight(float red, float green, float blue,
//  //                             float nx, float ny, float nz)
//  //public void pointLight(float red, float green, float blue,
//  //                       float x, float y, float z)
//  //public void spotLight(float red, float green, float blue,
//  //                      float x, float y, float z,
//  //                      float nx, float ny, float nz,
//  //                      float angle, float concentration)
//  //public void lightFalloff(float constant, float linear, float quadratic)
//  //public void lightSpecular(float x, float y, float z)
//  //protected void lightPosition(int num, float x, float y, float z)
//  //protected void lightDirection(int num, float x, float y, float z)



  //////////////////////////////////////////////////////////////

  // BACKGROUND


  @Override
  public void backgroundImpl() {
    // This only takes into account cases where this is the primary surface.
    // Not sure what we do with offscreen anyway.
    Paint savedFill = context.getFill();
    BlendMode savedBlend = context.getGlobalBlendMode();
    context.setFill(new Color(backgroundR, backgroundG, backgroundB, backgroundA));
    context.setGlobalBlendMode(BlendMode.SRC_OVER);
    context.fillRect(0, 0, width, height);
    context.setFill(savedFill);
    context.setGlobalBlendMode(savedBlend);
  }



//  //////////////////////////////////////////////////////////////
//
//  // COLOR MODE
//
//  // All colorMode() variations are inherited from PGraphics.
//
//
//
//  //////////////////////////////////////////////////////////////
//
//  // COLOR CALC
//
//  // colorCalc() and colorCalcARGB() inherited from PGraphics.
//
//
//
//  //////////////////////////////////////////////////////////////
//
//  // COLOR DATATYPE STUFFING
//
//  // final color() variations inherited.
//
//
//
//  //////////////////////////////////////////////////////////////
//
//  // COLOR DATATYPE EXTRACTION
//
//  // final methods alpha, red, green, blue,
//  // hue, saturation, and brightness all inherited.
//
//
//
//  //////////////////////////////////////////////////////////////
//
//  // COLOR DATATYPE INTERPOLATION
//
//  // both lerpColor variants inherited.
//
//
//
//  //////////////////////////////////////////////////////////////
//
//  // BEGIN/END RAW
//
//
//  @Override
//  public void beginRaw(PGraphics recorderRaw) {
//    showMethodWarning("beginRaw");
//  }
//
//
//  @Override
//  public void endRaw() {
//    showMethodWarning("endRaw");
//  }
//
//
//
//  //////////////////////////////////////////////////////////////
//
//  // WARNINGS and EXCEPTIONS
//
//  // showWarning and showException inherited.
//
//
//
//  //////////////////////////////////////////////////////////////
//
//  // RENDERER SUPPORT QUERIES
//
//
//  //public boolean displayable()  // true
//
//
//  //public boolean is2D()  // true
//
//
//  //public boolean is3D()  // false



  //////////////////////////////////////////////////////////////

  // PIMAGE METHODS


  @Override
  public void loadPixels() {
//    pixelFactor = 2;
    int wide = width * pixelFactor;
    int high = height * pixelFactor;

    if ((pixels == null) || (pixels.length != wide*high)) {
      pixels = new int[wide * high];
    }

//    WritableRaster raster = getRaster();
//    raster.getDataElements(0, 0, width, height, pixels);
//    if (raster.getNumBands() == 3) {
//      // Java won't set the high bits when RGB, returns 0 for alpha
//      // https://github.com/processing/processing/issues/2030
//      for (int i = 0; i < pixels.length; i++) {
//        pixels[i] = 0xff000000 | pixels[i];
//      }
//    }
    SnapshotParameters sp = new SnapshotParameters();
    if (pixelFactor == 2) {
      sp.setTransform(Transform.scale(2, 2));
    }
    WritableImage wi = ((PSurfaceFX) surface).canvas.snapshot(sp, null);
    PixelReader pr = wi.getPixelReader();
    //pr.getPixels(0, 0, width, height, argbFormat, pixels, 0, width);
    //pr.getPixels(0, 0, width*2, height*2, argbFormat, pixels, 0, width*2);
    pr.getPixels(0, 0, wide, high, argbFormat, pixels, 0, wide);
  }


//  /**
//   * Update the pixels[] buffer to the PGraphics image.
//   * <P>
//   * Unlike in PImage, where updatePixels() only requests that the
//   * update happens, in PGraphicsJava2D, this will happen immediately.
//   */
//  @Override
//  public void updatePixels(int x, int y, int c, int d) {
//    //if ((x == 0) && (y == 0) && (c == width) && (d == height)) {
////    System.err.format("%d %d %d %d .. w/h = %d %d .. pw/ph = %d %d %n", x, y, c, d, width, height, pixelWidth, pixelHeight);
//    if ((x != 0) || (y != 0) || (c != pixelWidth) || (d != pixelHeight)) {
//      // Show a warning message, but continue anyway.
//      showVariationWarning("updatePixels(x, y, w, h)");
////      new Exception().printStackTrace(System.out);
//    }
////    updatePixels();
//    if (pixels != null) {
//      getRaster().setDataElements(0, 0, width, height, pixels);
//    }
//    modified = true;
//  }
//
//
////  @Override
////  protected void updatePixelsImpl(int x, int y, int w, int h) {
////    super.updatePixelsImpl(x, y, w, h);
////
////    if ((x != 0) || (y != 0) || (w != width) || (h != height)) {
////      // Show a warning message, but continue anyway.
////      showVariationWarning("updatePixels(x, y, w, h)");
////    }
////    getRaster().setDataElements(0, 0, width, height, pixels);
////  }
//
//
//
//  //////////////////////////////////////////////////////////////
//
//  // GET/SET
//
//
//  static int getset[] = new int[1];
//
//
//  @Override
//  public int get(int x, int y) {
//    if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) return 0;
//    //return ((BufferedImage) image).getRGB(x, y);
////    WritableRaster raster = ((BufferedImage) (useOffscreen && primarySurface ? offscreen : image)).getRaster();
//    WritableRaster raster = getRaster();
//    raster.getDataElements(x, y, getset);
//    if (raster.getNumBands() == 3) {
//      // https://github.com/processing/processing/issues/2030
//      return getset[0] | 0xff000000;
//    }
//    return getset[0];
//  }
//
//
//  //public PImage get(int x, int y, int w, int h)
//
//
//  @Override
//  public PImage get() {
//    return get(0, 0, width, height);
//  }
//
//
//  @Override
//  protected void getImpl(int sourceX, int sourceY,
//                         int sourceWidth, int sourceHeight,
//                         PImage target, int targetX, int targetY) {
//    // last parameter to getRGB() is the scan size of the *target* buffer
//    //((BufferedImage) image).getRGB(x, y, w, h, output.pixels, 0, w);
////    WritableRaster raster =
////      ((BufferedImage) (useOffscreen && primarySurface ? offscreen : image)).getRaster();
//    WritableRaster raster = getRaster();
//
//    if (sourceWidth == target.width && sourceHeight == target.height) {
//      raster.getDataElements(sourceX, sourceY, sourceWidth, sourceHeight, target.pixels);
//      // https://github.com/processing/processing/issues/2030
//      if (raster.getNumBands() == 3) {
//        target.filter(OPAQUE);
//      }
//
//    } else {
//      // TODO optimize, incredibly inefficient to reallocate this much memory
//      int[] temp = new int[sourceWidth * sourceHeight];
//      raster.getDataElements(sourceX, sourceY, sourceWidth, sourceHeight, temp);
//
//      // Copy the temporary output pixels over to the outgoing image
//      int sourceOffset = 0;
//      int targetOffset = targetY*target.width + targetX;
//      for (int y = 0; y < sourceHeight; y++) {
//        if (raster.getNumBands() == 3) {
//          for (int i = 0; i < sourceWidth; i++) {
//            // Need to set the high bits for this feller
//            // https://github.com/processing/processing/issues/2030
//            target.pixels[targetOffset + i] = 0xFF000000 | temp[sourceOffset + i];
//          }
//        } else {
//          System.arraycopy(temp, sourceOffset, target.pixels, targetOffset, sourceWidth);
//        }
//        sourceOffset += sourceWidth;
//        targetOffset += target.width;
//      }
//    }
//  }
//
//
//  @Override
//  public void set(int x, int y, int argb) {
//    if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) return;
////    ((BufferedImage) image).setRGB(x, y, argb);
//    getset[0] = argb;
////    WritableRaster raster = ((BufferedImage) (useOffscreen && primarySurface ? offscreen : image)).getRaster();
////    WritableRaster raster = image.getRaster();
//    getRaster().setDataElements(x, y, getset);
//  }
//
//
//  //public void set(int x, int y, PImage img)
//
//
//  @Override
//  protected void setImpl(PImage sourceImage,
//                         int sourceX, int sourceY,
//                         int sourceWidth, int sourceHeight,
//                         int targetX, int targetY) {
//    WritableRaster raster = getRaster();
////      ((BufferedImage) (useOffscreen && primarySurface ? offscreen : image)).getRaster();
//
//    if ((sourceX == 0) && (sourceY == 0) &&
//        (sourceWidth == sourceImage.width) &&
//        (sourceHeight == sourceImage.height)) {
//      raster.setDataElements(targetX, targetY,
//                             sourceImage.width, sourceImage.height,
//                             sourceImage.pixels);
//    } else {
//      // TODO optimize, incredibly inefficient to reallocate this much memory
//      PImage temp = sourceImage.get(sourceX, sourceY, sourceWidth, sourceHeight);
//      raster.setDataElements(targetX, targetY, temp.width, temp.height, temp.pixels);
//    }
//  }



  //////////////////////////////////////////////////////////////

  // MASK


  static final String MASK_WARNING =
    "mask() cannot be used on the main drawing surface";


  @Override
  public void mask(PImage alpha) {
    showWarning(MASK_WARNING);
  }



  //////////////////////////////////////////////////////////////

  // FILTER

  // Because the PImage versions call loadPixels() and
  // updatePixels(), no need to override anything here.


  //public void filter(int kind)


  //public void filter(int kind, float param)



  //////////////////////////////////////////////////////////////

  // COPY


//  @Override
//  public void copy(int sx, int sy, int sw, int sh,
//                   int dx, int dy, int dw, int dh) {
//    if ((sw != dw) || (sh != dh)) {
//      g2.drawImage(image, dx, dy, dx + dw, dy + dh, sx, sy, sx + sw, sy + sh, null);
//
//    } else {
//      dx = dx - sx;  // java2d's "dx" is the delta, not dest
//      dy = dy - sy;
//      g2.copyArea(sx, sy, sw, sh, dx, dy);
//    }
//  }


//  @Override
//  public void copy(PImage src,
//                   int sx, int sy, int sw, int sh,
//                   int dx, int dy, int dw, int dh) {
//    g2.drawImage((Image) src.getNative(),
//                 dx, dy, dx + dw, dy + dh,
//                 sx, sy, sx + sw, sy + sh, null);
//  }



  //////////////////////////////////////////////////////////////

  // BLEND


  //static public int blendColor(int c1, int c2, int mode)


  //public void blend(int sx, int sy, int sw, int sh,
  //                  int dx, int dy, int dw, int dh, int mode)


  //public void blend(PImage src,
  //                  int sx, int sy, int sw, int sh,
  //                  int dx, int dy, int dw, int dh, int mode)



  //////////////////////////////////////////////////////////////

  // SAVE


  //public void save(String filename)



  //////////////////////////////////////////////////////////////

  /**
   * Display a warning that the specified method is simply unavailable.
   */
  static public void showTodoWarning(String method, int issue) {
    showWarning(method + "() is not yet available: " +
                "https://github.com/processing/processing/issues/" + issue);
  }
}
