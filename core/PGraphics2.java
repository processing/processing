/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PGraphics2 - graphics engine implemented via java2d
  Part of the Processing project - http://processing.org

  Copyright (c) 2005 Ben Fry and Casey Reas

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

//import java.applet.*;
import java.awt.*;
//import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
//import java.io.*;


// Graphics, GeneralPath, AffineTransform, BasicStroke, Graphics2D

public class PGraphics2 extends PGraphics {

  Graphics2D graphics;
  GeneralPath path;

  int transformCount;
  AffineTransform transformStack[] =
    new AffineTransform[MATRIX_STACK_DEPTH];
  double transform[] = new double[6];

  Ellipse2D.Float ellipse = new Ellipse2D.Float();
  Rectangle2D.Float rect = new Rectangle2D.Float();
  Arc2D.Float arc = new Arc2D.Float();


  //////////////////////////////////////////////////////////////

  // INTERNAL


  /**
   * Constructor for the PGraphics2 object.
   * This prototype only exists because of annoying
   * java compilers, and should not be used.
   */
  public PGraphics2() { }


  /**
   * Constructor for the PGraphics object. Use this to ensure that
   * the defaults get set properly. In a subclass, use this(w, h)
   * as the first line of a subclass' constructor to properly set
   * the internal fields and defaults.
   *
   * @param iwidth  viewport width
   * @param iheight viewport height
   */
  public PGraphics2(int iwidth, int iheight) {
    resize(iwidth, iheight);
  }


  /**
   * Called in repsonse to a resize event, handles setting the
   * new width and height internally, as well as re-allocating
   * the pixel buffer for the new size.
   *
   * Note that this will nuke any cameraMode() settings.
   */
  public void resize(int iwidth, int iheight) {  // ignore
    //System.out.println("resize " + iwidth + " " + iheight);

    width = iwidth;
    height = iheight;
    width1 = width - 1;
    height1 = height - 1;

    allocate();

    // clear the screen with the old background color
    background(backgroundColor);
  }


  // broken out because of subclassing for opengl
  protected void allocate() {
    image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    Graphics2D graphics = (Graphics2D) image.getGraphics();
  }



  //////////////////////////////////////////////////////////////

  // FRAME


  // turn off mis.newPixels
  public void endFrame() {
    // moving this back here (post-68) because of macosx thread problem
    //mis.newPixels(pixels, cm, 0, width);
  }



  //////////////////////////////////////////////////////////////

  // SHAPES


  public void vertex(float x, float y) {
    splineVertexCount = 0;
    float vertex[];

    if (vertexCount == vertices.length) {
      float temp[][] = new float[vertexCount<<1][VERTEX_FIELD_COUNT];
      System.arraycopy(vertices, 0, temp, 0, vertexCount);
      vertices = temp;
      message(CHATTER, "allocating more vertices " + vertices.length);
    }
    // not everyone needs this, but just easier to store rather
    // than adding another moving part to the code...
    vertices[vertexCount][MX] = x;
    vertices[vertexCount][MY] = x;
    vertexCount++;

    switch (shape) {

    case POINTS:
      point(x, y);
      break;

    case LINES:
      if ((vertexCount % 2) == 0) {
        line(vertices[vertexCount-2][MX],
             vertices[vertexCount-2][MY], x, y);
      }
      break;

    case LINE_STRIP:
    case LINE_LOOP:
      if (vertexCount == 1) {
        path = new GeneralPath();
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
      break;

    case TRIANGLES:
      if ((vertexCount % 3) == 0) {
        triangle(vertices[vertexCount - 3][MX],
                 vertices[vertexCount - 3][MY],
                 vertices[vertexCount - 2][MX],
                 vertices[vertexCount - 2][MY],
                 x, y);
      }
      break;

    case TRIANGLE_STRIP:
      if (vertexCount == 3) {
        triangle(vertices[0][MX], vertices[0][MY],
                 vertices[1][MX], vertices[1][MY],
                 x, y);
      } else if (vertexCount > 3) {
        path = new GeneralPath();
        // when vertexCount == 4, draw an un-closed triangle
        // for indices 2, 3, 1
        path.moveTo(vertices[vertexCount - 2][MX],
                    vertices[vertexCount - 2][MY]);
        path.lineTo(vertices[vertexCount - 1][MX],
                    vertices[vertexCount - 1][MY]);
        path.lineTo(vertices[vertexCount - 3][MX],
                    vertices[vertexCount - 3][MY]);
        draw_shape(path);
      }
      break;

    case TRIANGLE_FAN:
      if (vertexCount == 3) {
        triangle(vertices[0][MX], vertices[0][MY],
                 vertices[1][MX], vertices[1][MY],
                 x, y);
      } else if (vertexCount > 3) {
        path = new GeneralPath();
        // when vertexCount > 3, draw an un-closed triangle
        // for indices 0 (center), previous, current
        path.moveTo(vertices[0][MX],
                    vertices[0][MY]);
        path.lineTo(vertices[vertexCount - 2][MX],
                    vertices[vertexCount - 2][MY]);
        path.lineTo(x, y);
        draw_shape(path);
      }
      break;

    case QUADS:
      if ((vertexCount % 4) == 0) {
        quad(vertices[vertexCount - 4][MX],
             vertices[vertexCount - 4][MY],
             vertices[vertexCount - 3][MX],
             vertices[vertexCount - 3][MY],
             vertices[vertexCount - 2][MX],
             vertices[vertexCount - 2][MY],
             x, y);
      }
      break;

    case QUAD_STRIP:
      // 0---2---4
      // |   |   |
      // 1---3---5
      if (vertexCount == 4) {
        // note difference in winding order:
        quad(vertices[0][MX], vertices[0][MY],
             vertices[2][MX], vertices[2][MY],
             x, y,
             vertices[1][MX], vertices[1][MY]);

      } else if (vertexCount > 4) {
        path = new GeneralPath();
        // when vertexCount == 5, draw an un-closed triangle
        // for indices 2, 4, 5, 3
        path.moveTo(vertices[vertexCount - 3][MX],
                    vertices[vertexCount - 3][MY]);
        path.lineTo(vertices[vertexCount - 1][MX],
                    vertices[vertexCount - 1][MY]);
        path.lineTo(x, y);
        path.lineTo(vertices[vertexCount - 2][MX],
                    vertices[vertexCount - 2][MY]);
        draw_shape(path);
      }
      break;

    case POLYGON:
    case CONCAVE_POLYGON:
    case CONVEX_POLYGON:
      if (vertexCount == 1) {
        path = new GeneralPath();
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
      break;
    }
  }


  public void bezierVertex(float x, float y) {
    vertexCount = 0;

    if (splineVertices == null) {
      splineVertices = new float[DEFAULT_SPLINE_VERTICES][VERTEX_FIELD_COUNT];
    }

    // if more than 128 points, shift everything back to the beginning
    if (splineVertexCount == DEFAULT_SPLINE_VERTICES) {
      System.arraycopy(splineVertices[DEFAULT_SPLINE_VERTICES - 3], 0,
                       splineVertices[0], 0, VERTEX_FIELD_COUNT);
      System.arraycopy(splineVertices[DEFAULT_SPLINE_VERTICES - 2], 0,
                       splineVertices[1], 0, VERTEX_FIELD_COUNT);
      splineVertexCount = 3;
    }
    splineVertices[splineVertexCount][MX] = x;
    splineVertices[splineVertexCount][MY] = y;
    splineVertexCount++;

    switch (shape) {
    case LINE_LOOP:
    case POLYGON:
    case CONCAVE_POLYGON:
    case CONVEX_POLYGON:
      if (splineVertexCount == 1) {
        path.moveTo(x, y);

      } else if (splineVertexCount >= 4) {
        path.curveTo(splineVertices[splineVertexCount-3][MX],
                     splineVertices[splineVertexCount-3][MY],
                     splineVertices[splineVertexCount-2][MX],
                     splineVertices[splineVertexCount-2][MY],
                     x, y);
      }
      break;
    }
  }


  public void curveVertex(float x, float y) {
    // TODO handle inverse matrix action
  }


  public void endShape() {
    shape = 0;

    switch (shape) {
    case LINE_STRIP:
      stroke_shape(path);
      break;

    case LINE_LOOP:
      path.closePath();
      stroke_shape(path);
      break;

    case POLYGON:
    case CONCAVE_POLYGON:
    case CONVEX_POLYGON:
      path.closePath();
      draw_shape(path);
      break;
    }
  }



  //////////////////////////////////////////////////////////////

  // SHAPES


  protected void fill_shape(Shape s) {
    if (fill) {
      graphics.setColor(fillColorObject);
      graphics.fill(s);
    }
  }

  protected void stroke_shape(Shape s) {
    if (stroke) {
      graphics.setColor(strokeColorObject);
      graphics.draw(s);
    }
  }

  protected void draw_shape(Shape s) {
    if (fill) {
      graphics.setColor(fillColorObject);
      graphics.fill(s);
    }
    if (stroke) {
      graphics.setColor(strokeColorObject);
      graphics.draw(s);
    }
  }


  public void point(float x, float y) {
    //graphics.setColor(strokeColorObject);
    //graphics.drawLine(x1, y1, x2, y2);
    line(x, y, x, y);
  }


  public void line(float x1, float y1, float x2, float y2) {
    graphics.setColor(strokeColorObject);
    graphics.drawLine(x1, y1, x2, y2);
  }


  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    GeneralPath gp = new GeneralPath();
    gp.moveTo(x1, y1);
    gp.lineTo(x2, y2);
    gp.lineTo(x3, y3);
    gp.closePath();

    draw_shape(gp);
  }


  public void rect(float x1, float y1, float x2, float y2) {
    switch (rectMode) {
    case CORNERS:
      rect.setFrameFromDiagonal(x1, y1, x2, y2);
      break;
    case CORNER:
      rect.setFrame(x1, y1, x2, y2);
      break;
    case CENTER_RADIUS:
      rect.setFrame(x1 - x2, y1 - y2, x1 + x2, y1 + y2);
      break;
    case CENTER:
      rect.setFrame(x1 - x2/2.0f, y1 - y2/2.0f, x1 + x2/2.0f, y1 + y2/2.0f);
      break;
    }
    draw_shape(rect);
  }


  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
    GeneralPath gp = new GeneralPath();
    gp.moveTo(x1, y1);
    gp.lineTo(x2, y2);
    gp.lineTo(x3, y3);
    gp.lineTo(x4, y4);
    gp.closePath();

    draw_shape(gp);
  }


  public void arc(float start, float stop,
                  float x, float y, float radius) {
    arc(start, stop, x, y, radius, radius);
  }


  public void arc(float start, float stop,
                  float x, float y, float w, float h) {
    if (arcMode == CORNERS) {
      w -= x;
      h -= y;

    } else if (arcMode == CENTER_RADIUS) {
      x -= w;
      y -= h;
      w *= 2;
      h *= 2;

    } else if (arcMode == CENTER) {
      x -= w;
      y -= h;
    }

    arc.setArc(x, y, w, h, start, stop-start, Arc2D.PIE);
    draw_shape(arc);
  }


  public void ellipse(float x, float y, float hradius, float vradius) {
    ellipse.setFrame(x, y, hradius, vradius);
    draw_shape(ellipse);
  }


  public void circle(float x, float y, float radius) {
    ellipse(x, y, radius, radius);
  }


  public void bezier(float x1, float y1,
                     float x2, float y2,
                     float x3, float y3,
                     float x4, float y4) {
    GeneralPath gp = new GeneralPath();
    gp.moveTo(x1, y1);
    gp.curveTo(x2, y2, x3, y3, x4, y4);
    gp.closePath();

    draw_shape(gp);
  }


  public void bezierDetail(int detail) {
    // ignored in java2d
  }

  public void curveDetail(int detail) {
    // ignored in java2d
  }


  public void curveTightness(float tightness) {
    // TODO
  }


  public void curve(float x1, float y1,
                    float x2, float y2,
                    float x3, float y3,
                    float x4, float y4) {
    // TODO need inverse catmull rom to bezier matrix
  }


  //////////////////////////////////////////////////////////////

  // IMAGES


  public void image(PImage image, float x1, float y1) {
    image(image, x1, y1, image.width, image.height);
  }


  public void image(PImage image,
                    float x1, float y1, float x2, float y2) {
    if (imageMode == CORNER) {
      x2 += x1;  // x2 is width, need to add x1
      y2 += y1;

    } else if ((imageMode == CENTER) ||
               (imageMode == CENTER_RADIUS)) {
      x1 -= image.width /2f;
      y1 -= image.height / 2f;
    }
    check_image_cache();
    graphics.drawImage((Image) image.cache, x1, y1, x2, y2, null);
  }


  public void image(PImage image,
                    float x1, float y1, float x2, float y2,
                    float u1, float v1, float u2, float v2) {
    if (imageMode == CORNER) {
      x2 += x1;  // x2 is width, need to add x1
      y2 += y1;

    } else if ((imageMode == CENTER) ||
               (imageMode == CENTER_RADIUS)) {
      x1 -= image.width /2f;
      y1 -= image.height / 2f;
    }
    check_image_cache();
    graphics.drawImage((Image) image.cache,
                       (int)x1, (int)y1, (int)x2, (int)y2,
                       (int)u1, (int)v1, (int)u2, (int)v2,
                       null);
  }


  protected void check_image_cache(PImage what) {
    if (image.cache == null) {
      cache = new BufferedImage(image.width, image.height,
                                BufferedImage.TYPE_INT_ARGB);
      image.modified();  // mark the whole thing for update
    }

    if (image.modified) {
      // update the sub-portion of the image as necessary
      BufferedImage bi = (BufferedImage) image.cache;

      bi.setRGB(image.mx1, image.my1,
                image.mx2 - image.mx1 + 1,
                image.my2 - image.my1 + 1,
                image.pixels,
                image.my1*image.width + image.mx1,  // offset for copy
                image.width);  // scan size
      image.resetModified();
    }
  }


  //////////////////////////////////////////////////////////////


  /*
  public void textFont(PFont which);

  public void textSize(float size);

  public void textFont(PFont which, float size);

  public void textLeading(float leading);

  public void textMode(int mode);

  public void textSpace(int space);

  public void text(char c, float x, float y);

  public void text(char c, float x, float y, float z);

  public void text(String s, float x, float y);

  public void text(String s, float x, float y, float z);

  public void text(String s, float x, float y, float w, float h);

  public void text(String s, float x1, float y1, float z, float x2, float y2);

  public void text(int num, float x, float y);

  public void text(int num, float x, float y, float z);

  public void text(float num, float x, float y);

  public void text(float num, float x, float y, float z);
  */


  //////////////////////////////////////////////////////////////

  // MATRIX


  public void translate(float tx, float ty) {
    graphics.translate(tx, ty);
  }


  public void rotate(float angle) {
    graphics.rotate(angle);
  }


  public void scale(float s) {
    graphics.scale(s, s);
  }


  public void scale(float sx, float sy) {
    graphics.scale(sx, sy);
  }


  public void push() {
    if (transformCount == transformStack.length) {
      die("push() cannot use push more than " +
          transformStack.length + " times");
    }
    transformStack[transformCount] = graphics.getTransform();
    transformCount++;
  }


  public void pop() {
    if (transformCount == 0) {
      die("missing a pop() to go with that push()");
    }
    transformCount--;
    graphics.setTransform(transformStack[transformCount]);
  }


  public void resetMatrix() {
    graphics.setTransform(new AffineTransform());
  }


  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    graphics.transform(new AffineTransform(n00, n10, n01, n11, n02, n22));
  }


  public void printMatrix() {
    // TODO maybe format this the same way as the superclass
    //AffineTransform t = graphics.getTransform();
    //System.out.println(t);  // not sure what this does
    graphics.getTransform().getMatrix(transform);

    m00 = (float) transform[0];
    m01 = (float) transform[2];
    m02 = (float) transform[4];

    m10 = (float) transform[1];
    m11 = (float) transform[3];
    m12 = (float) transform[5];

    super.printMatrix();
  }


  public float screenX(float x, float y) {
    graphics.getTransform().getMatrix(transform);
    //return m00*x + m01*y + m02;
    return (float)transform[0]*x + (float)transform[2]*y + (float)transform[4];
  }


  public float screenY(float x, float y) {
    graphics.getTransform().getMatrix(transform);
    return (float)transform[1]*x + (float)transform[3]*y + (float)transform[5];
  }



  //////////////////////////////////////////////////////////////

  // STROKE


  public void strokeWeight(float weight) {
    super.strokeWeight(weight);
    setStroke();
  }


  public void strokeJoin(int join) {
    super.strokeJoin(join);
    setStroke();
  }


  public void strokeCap(int cap) {
    super.strokeCap(cap);
    setStroke();
  }


  protected void setStroke() {
    int cap = BasicStroke.CAP_BUTT;
    if (strokeCap == ROUND) {
      cap = BasicStroke.CAP_ROUND;
    } else if (strokeCap == PROJECTED) {
      cap = BasicStroke.CAP_SQUARE;
    }

    int join = BasicStroke.JOIN_BEVEL;
    if (strokeJoin == MITERED) {
      join = BasicStroke.JOIN_MITER;
    } else if (strokeJoin == ROUND) {
      join = BasicStroke.JOIN_ROUND;
    }

    graphics.setStroke(new BasicStroke(strokeWeight, cap, join));
  }



  //////////////////////////////////////////////////////////////

  // STROKE/FILL/BACKGROUND


  protected Color tintColorObject;
  protected Color fillColorObject;
  protected Color strokeColorObject;

  protected void calc_tint() {
    super.calc_tint();
    // TODO actually implement tinted images
    tintColorObject = new Color(tintColor);
  }

  protected void calc_fill() {
    super.calc_fill();
    //graphics.setPaint(new Color(fillColor));
    fillColorObject = new Color(fillColor);
  }

  protected void calc_stroke() {
    super.calc_stroke();
    ///graphics.setStroke(new Color(fillColor));
    strokeColorObject = new Color(strokeColor);
  }


  public void background(PImage image) {
    if ((image.width != width) || (image.height != height)) {
      die("background image must be the same size " +
          "as your application");
    }
    if ((image.format != RGB) && (image.format != ARGB)) {
      die("background images should be RGB or ARGB");
    }

    // make sure it's been properly updated
    check_image_cache(image);

    // blit image to the screen
    graphics.drawImage((BufferedImage) image.cache, 0, 0);
  }


  /**
   * Clears pixel buffer. Also clears the stencil and zbuffer
   * if they exist. Their existence is more accurate than using 'depth'
   * to test whether to clear them, because if they're non-null,
   * it means that depth() has been called somewhere in the program,
   * even if noDepth() was called before draw() exited.
   */
  public void clear() {
    graphics.setColor(new Color(backgroundColor));
    graphics.fillRect(0, 0, width, height);
  }



  //////////////////////////////////////////////////////////////

  // FROM PIMAGE


  public void alpha(int alpha[]) {
    // does nothing in PGraphics
  }

  public void alpha(PImage alpha) {
    // does nothing in PGraphics
  }

  public void filter(int kind) {
    // TODO
  }

  public void filter(int kind, float param) {
    // TODO
  }


  public int get(int x, int y) {
    ((BufferedImage) image).getRGB(x, y);
  }


  public PImage get(int x, int y, int w, int h) {
    PImage output = new PImage(w, h);
    ((BufferedImage) image).getRGB(x, y, w, h, output.pixels, 0, width);
  }


  public void set(int x, int y, int c) {
    ((BufferedImage) image).setRGB(x, y, c);
  }


  public void copy(PImage src, int dx, int dy) {
    // TODO if this image is not RGB, needs to behave differently
    //      (if it's gray, need to copy gray pixels)
    //      for alpha, just leave it be.. copy() doesn't composite
    ((BufferedImage) image).setRGB(dx, dy, src.width, src.height,
                                   src.pixels, 0, src.width);
  }


  public void copy(int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    // TODO
  }


  public void copy(PImage src, int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    // TODO
  }


  public void blend(PImage src, int sx, int sy, int dx, int dy, int mode) {
    // TODO
  }


  public void blend(int sx, int sy, int dx, int dy, int mode) {
    // TODO
  }


  public void blend(int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    // TODO
  }


  public void blend(PImage src, int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    // TODO
  }


  /**
   * This is used to both set the pixels[] array so that it can be
   * manipulated, and it also returns a PImage object that can be
   * messed with directly.
   */
  public PImage get() {
    //PImage outgoing = new PImage(width, height);
    // int[] getRGB(int startX, int startY, int w, int h,
    //              int[] rgbArray, int offset, int scansize)
    if (pixels == null) {
      pixels = new int[width * height];
    }
    image.getRGB(0, 0, width, height, pixels, 0, width);
    return new PImage(pixels, width, height, RGB);
  }


  public void save(String filename) {
    //static boolean write(RenderedImage im, String formatName, File output)
    // maybe use ImageIO.save(File file) here if it's available?
    get().save(filename);
  }


  public void smooth() {
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
  }


  public void noSmooth() {
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_OFF);
  }
}
