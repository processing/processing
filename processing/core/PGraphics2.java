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

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;


// Graphics, GeneralPath, AffineTransform, BasicStroke, Graphics2D

public class PGraphics2 extends PGraphics {

  Graphics2D graphics;
  GeneralPath gpath;

  int transformCount;
  AffineTransform transformStack[] =
    new AffineTransform[MATRIX_STACK_DEPTH];
  double transform[] = new double[6];

  Line2D.Float line = new Line2D.Float();
  Ellipse2D.Float ellipse = new Ellipse2D.Float();
  Rectangle2D.Float rect = new Rectangle2D.Float();
  Arc2D.Float arc = new Arc2D.Float();

  protected Color tintColorObject;
  protected Color fillColorObject;
  protected Color strokeColorObject;



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
    graphics = (Graphics2D) image.getGraphics();
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
      //message(CHATTER, "allocating more vertices " + vertices.length);
    }
    // not everyone needs this, but just easier to store rather
    // than adding another moving part to the code...
    vertices[vertexCount][MX] = x;
    vertices[vertexCount][MY] = y;
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
        gpath = new GeneralPath();
        gpath.moveTo(x, y);
      } else {
        gpath.lineTo(x, y);
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
      if (vertexCount >= 3) {
        triangle(vertices[vertexCount - 2][MX],
                 vertices[vertexCount - 2][MY],
                 vertices[vertexCount - 1][MX],
                 vertices[vertexCount - 1][MY],
                 vertices[vertexCount - 3][MX],
                 vertices[vertexCount - 3][MY]);
        /*
      if (vertexCount == 3) {
        triangle(vertices[0][MX], vertices[0][MY],
                 vertices[1][MX], vertices[1][MY],
                 x, y);
      } else if (vertexCount > 3) {
        gpath = new GeneralPath();
        // when vertexCount == 4, draw an un-closed triangle
        // for indices 2, 3, 1
        gpath.moveTo(vertices[vertexCount - 2][MX],
                     vertices[vertexCount - 2][MY]);
        gpath.lineTo(vertices[vertexCount - 1][MX],
                     vertices[vertexCount - 1][MY]);
        gpath.lineTo(vertices[vertexCount - 3][MX],
                     vertices[vertexCount - 3][MY]);
        draw_shape(gpath);
      }
        */
      }
      break;

    case TRIANGLE_FAN:
      if (vertexCount == 3) {
        triangle(vertices[0][MX], vertices[0][MY],
                 vertices[1][MX], vertices[1][MY],
                 x, y);
      } else if (vertexCount > 3) {
        gpath = new GeneralPath();
        // when vertexCount > 3, draw an un-closed triangle
        // for indices 0 (center), previous, current
        gpath.moveTo(vertices[0][MX],
                    vertices[0][MY]);
        gpath.lineTo(vertices[vertexCount - 2][MX],
                    vertices[vertexCount - 2][MY]);
        gpath.lineTo(x, y);
        draw_shape(gpath);
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
      if ((vertexCount >= 4) && ((vertexCount % 2) == 0)) {
        //if (vertexCount == 4) {//
        // note difference in winding order:
        //quad(vertices[0][MX], vertices[0][MY],
        //   vertices[2][MX], vertices[2][MY],
        //   x, y,
        //   vertices[1][MX], vertices[1][MY]);
        quad(vertices[vertexCount - 4][MX],
             vertices[vertexCount - 4][MY],
             vertices[vertexCount - 2][MX],
             vertices[vertexCount - 2][MY],
             x, y,
             vertices[vertexCount - 3][MX],
             vertices[vertexCount - 3][MY]);

        /*
      } else if ((vertexCount > 4) && ((vertexCount % 2) == 0)) {
        gpath = new GeneralPath();
        // when vertexCount == 6, draw an un-closed triangle
        // for indices 2, 4, 5, 3
        gpath.moveTo(vertices[vertexCount - 4][MX],
                     vertices[vertexCount - 4][MY]);
        gpath.lineTo(vertices[vertexCount - 2][MX],
                     vertices[vertexCount - 2][MY]);
        gpath.lineTo(x, y);
        gpath.lineTo(vertices[vertexCount - 3][MX],
                     vertices[vertexCount - 3][MY]);
        draw_shape(gpath);
        */
      }
      break;

    case POLYGON:
    case CONCAVE_POLYGON:
    case CONVEX_POLYGON:
      if (vertexCount == 1) {
        //System.out.println("starting poly path " + x + " " + y);
        gpath = new GeneralPath();
        gpath.moveTo(x, y);
      } else {
        //System.out.println("continuing poly path " + x + " " + y);
        gpath.lineTo(x, y);
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
        gpath.moveTo(x, y);

      } else if (splineVertexCount >= 4) {
        gpath.curveTo(splineVertices[splineVertexCount-3][MX],
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
    throw new RuntimeException("curveVertex() not yet implemented");
  }


  public void endShape() {
    //System.out.println("endShape");

    switch (shape) {
    case LINE_STRIP:
      stroke_shape(gpath);
      break;

    case LINE_LOOP:
      gpath.closePath();
      stroke_shape(gpath);
      break;

    case POLYGON:
    case CONCAVE_POLYGON:
    case CONVEX_POLYGON:
      //System.out.println("finishing polygon");
      gpath.closePath();
      draw_shape(gpath);
      break;
    }

    shape = 0;
  }



  //////////////////////////////////////////////////////////////


  /*
  protected void fill_shape(Shape s) {
    if (fill) {
      graphics.setColor(fillColorObject);
      graphics.fill(s);
    }
  }
  */

  protected void stroke_shape(Shape s) {
    if (stroke) {
      //System.out.println("stroking shape");
      graphics.setColor(strokeColorObject);
      graphics.draw(s);
    }
  }

  protected void draw_shape(Shape s) {
    if (fill) {
      //System.out.println("filling shape");
      graphics.setColor(fillColorObject);
      graphics.fill(s);
    }
    if (stroke) {
      //System.out.println("stroking shape");
      graphics.setColor(strokeColorObject);
      graphics.draw(s);
    }
  }


  //////////////////////////////////////////////////////////////


  public void point(float x, float y) {
    line(x, y, x, y);
  }


  public void line(float x1, float y1, float x2, float y2) {
    //graphics.setColor(strokeColorObject);
    //graphics.drawLine(x1, y1, x2, y2);
    line.setLine(x1, y1, x2, y2);
    stroke_shape(line);
  }


  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    gpath = new GeneralPath();
    gpath.moveTo(x1, y1);
    gpath.lineTo(x2, y2);
    gpath.lineTo(x3, y3);
    gpath.closePath();

    draw_shape(gpath);
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


  //////////////////////////////////////////////////////////////


  protected void rectImpl(float x1, float y1, float x2, float y2) {
    /*
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
    */
    rect.setFrame(x1, y1, x2-x1, y2-y1);
    draw_shape(rect);
  }


  /*
  public void ellipse(float a, float b, float c, float d) {
    float x = a;
    float y = b;
    float w = c;
    float h = d;

    if (ellipseMode == CORNERS) {
      w = c - a;
      h = d - b;

    } else if (ellipseMode == CENTER_RADIUS) {
      x = a - c;
      y = b - d;
      w = c * 2;
      h = d * 2;

    } else if (ellipseMode == CENTER) {
      x = a - c/2f;
      y = b - d/2f;
    }

    ellipse.setFrame(x, y, w, h);
    draw_shape(ellipse);
  }
  */

  protected void ellipseImpl(float x, float y, float w, float h) {
    ellipse.setFrame(x, y, w, h);
    draw_shape(ellipse);
  }


  public void arcImpl(float x, float y, float w, float h,
                      float start, float stop) {
    arc.setArc(x, y, w, h, start, stop-start, Arc2D.PIE);
    draw_shape(arc);
  }


  //////////////////////////////////////////////////////////////


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


  protected void imageImpl(PImage who,
                           float x1, float y1, float x2, float y2,
                           //float x, float y, float w, float h,
                           int u1, int v1, int u2, int v2) {
    if (who.cache == null) {
      who.cache = new ImageCache(who);

      //who.cache = new BufferedImage(who.width, who.height,
      //                            BufferedImage.TYPE_INT_ARGB);
      who.updatePixels();  // mark the whole thing for update
    }

    ImageCache cash = (ImageCache) who.cache;
    // if image previously was tinted, or the color changed
    // or the image was tinted, and tint is now disabled
    if ((tint && !cash.tinted) ||
        (tint && (cash.tintedColor != tintColor)) ||
        (!tint && cash.tinted)) {
      // for tint change, mark all pixels as needing update
      who.updatePixels();
    }

    if (who.modified) {
      cash.update();
      who.modified = false;
    }

      /*
    if (who.modified) {
      ((ImageCache) who.cache).update();

      // update the sub-portion of the image as necessary
      BufferedImage bi = (BufferedImage) who.cache;

      bi.setRGB(who.mx1,
                who.my1,
                who.mx2 - who.mx1,
                who.my2 - who.my1,
                who.pixels,
                who.my1*who.width + who.mx1,  // offset for copy
                who.width);  // scan size
      //who.pixelsUpdated();
      who.modified = false;
    }
      */

    //int x2 = (int) (x + w);
    //int y2 = (int) (y + h);

    graphics.drawImage(((ImageCache) who.cache).image,
                       //(int) x, (int) y, x2, y2,
                       (int) x1, (int) y1, (int) x2, (int) y2,
                       u1, v1, u2, v2, null);
  }


  class ImageCache {
    PImage source;
    boolean tinted;
    int tintedColor;
    int tintedPixels[];
    BufferedImage image;

    public ImageCache(PImage source) {
      this.source = source;
      // if RGB, set the image type to RGB,
      // otherwise it's ALPHA or ARGB, and use an ARGB bimage
      int type = (source.format == RGB) ?
        BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
      image = new BufferedImage(source.width, source.height, type);
    }

    public void update() {  //boolean t, int argb) {
      if ((source.format == ARGB) || (source.format == RGB)) {
        if (tint) {
          // create tintedPixels[] if necessary
          if (tintedPixels == null) {
            tintedPixels = new int[source.width * source.height];
          }

          int argb2 = tintColor;
          int a2 = (tintColor >> 24) & 0xff;
          int r2 = (tintColor >> 16) & 0xff;
          int g2 = (tintColor >> 8) & 0xff;
          int b2 = (tintColor) & 0xff;

          // multiply each of the color components into tintedPixels
          for (int i = 0; i < tintedPixels.length; i++) {
            int argb1 = source.pixels[i];
            int a1 = (argb1 >> 24) & 0xff;
            int r1 = (argb1 >> 16) & 0xff;
            int g1 = (argb1 >> 8) & 0xff;
            int b1 = (argb1) & 0xff;

            tintedPixels[i] =
              (((a2 * a1) & 0xff00) << 16) |
              (((r2 * r1) & 0xff00) << 8) |
              ((g2 * g1) & 0xff00) |
              (((b2 * b1) & 0xff00) >> 8);
          }
          tinted = true;
          tintedColor = tintColor;

          // finally, do a setRGB based on tintedPixels
          image.setRGB(0, 0, source.width, source.height,
                       tintedPixels, 0, source.width);

        } else {  // no tint
          // just do a setRGB like before
          image.setRGB(0, 0, source.width, source.height,
                       source.pixels, 0, source.width);
        }

      } else if (source.format == ALPHA) {
        if (tintedPixels == null) {
          tintedPixels = new int[source.width * source.height];
        }

        int lowbits = tintColor & 0x00ffffff;
        if (((tintColor >> 24) & 0xff) >= 254) {
          // no actual alpha to the tint, set the image's alpha
          // as the high 8 bits, and use the color as the low 24 bits
          for (int i = 0; i < tintedPixels.length; i++) {
            // don't bother with the math if value is zero
            tintedPixels[i] = (source.pixels[i] == 0) ?
              0 : (source.pixels[i] << 24) | lowbits;
          }

        } else {
          // multiply each image alpha by the tint alpha
          int alphabits = (tintColor >> 24) & 0xff;
          for (int i = 0; i < tintedPixels.length; i++) {
            tintedPixels[i] = (source.pixels[i] == 0) ?
              0 : (((alphabits * source.pixels[i]) & 0xFF00) << 16) | lowbits;
          }
        }

        // mark the pixels for next time
        tinted = true;
        tintedColor = tintColor;

        // finally, do a setRGB based on tintedPixels
        image.setRGB(0, 0, source.width, source.height,
                     tintedPixels, 0, source.width);
        /*
        int argb2 = tint ? tintColor : 0xFFFFFFFF;
        int a2 = (tintColor >> 24) & 0xff;
        int r2 = (tintColor >> 16) & 0xff;
        int g2 = (tintColor >> 8) & 0xff;
        int b2 = (tintColor) & 0xff;

          // multiply each of the color components into tintedPixels
          for (int i = 0; i < tintedPixels.length; i++) {
            int argb1 = source.pixels[i];
            int a1 = (argb1 >> 24) & 0xff;
            int r1 = (argb1 >> 16) & 0xff;
            int g1 = (argb1 >> 8) & 0xff;
            int b1 = (argb1) & 0xff;
        */
      }
    }
  }


  //////////////////////////////////////////////////////////////


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


  //////////////////////////////////////////////////////////////


  public void push() {
    if (transformCount == transformStack.length) {
      throw new RuntimeException("push() cannot use push more than " +
                                 transformStack.length + " times");
    }
    transformStack[transformCount] = graphics.getTransform();
    transformCount++;
  }


  public void pop() {
    if (transformCount == 0) {
      throw new RuntimeException("missing a pop() to go with that push()");
    }
    transformCount--;
    graphics.setTransform(transformStack[transformCount]);
  }


  public void resetMatrix() {
    graphics.setTransform(new AffineTransform());
  }


  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    graphics.transform(new AffineTransform(n00, n10, n01, n11, n02, n12));
  }


  public void printMatrix() {
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


  protected void calc_tint() {
    super.calc_tint();
    // TODO actually implement tinted images
    tintColorObject = new Color(tintColor, true);
  }

  protected void calc_fill() {
    super.calc_fill();
    fillColorObject = new Color(fillColor, true);
  }

  protected void calc_stroke() {
    super.calc_stroke();
    strokeColorObject = new Color(strokeColor, true);
  }


  //////////////////////////////////////////////////////////////


  public void strokeWeight(float weight) {
    super.strokeWeight(weight);
    set_stroke();
  }


  public void strokeJoin(int join) {
    super.strokeJoin(join);
    set_stroke();
  }


  public void strokeCap(int cap) {
    super.strokeCap(cap);
    set_stroke();
  }


  protected void set_stroke() {
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


  public void background(PImage image) {
    if ((image.width != width) || (image.height != height)) {
      throw new RuntimeException("background image must be " +
                                 "the same size as your application");
    }
    if ((image.format != RGB) && (image.format != ARGB)) {
      throw new RuntimeException("background images should be RGB or ARGB");
    }

    // make sure it's been properly updated
    //check_image_cache(image);
    // blit image to the screen
    //graphics.drawImage((BufferedImage) image.cache, 0, 0, null);
    push();
    resetMatrix();
    imageImpl(image, 0, 0, width, height, 0, 0, width, height);
    pop();
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


  public void smooth() {
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
  }


  public void noSmooth() {
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_OFF);
  }


  //////////////////////////////////////////////////////////////


  public void loadPixels() {
    if ((pixels == null) || (pixels.length != width * height)) {
      pixels = new int[width * height];
    }
    ((BufferedImage) image).getRGB(0, 0, width, height, pixels, 0, width);
  }


  public void updatePixels() {
    updatePixels(0, 0, width, height);
  }


  public void updatePixels(int x, int y, int c, int d) {
    ((BufferedImage) image).setRGB(x, y,
                                   (imageMode == CORNER) ? c : (c - x),
                                   (imageMode == CORNER) ? d : (d - y),
                                   pixels, 0, width);
  }


  //////////////////////////////////////////////////////////////


  public int get(int x, int y) {
    return ((BufferedImage) image).getRGB(x, y);
  }


  public PImage get(int x, int y, int w, int h) {
    PImage output = new PImage(w, h);
    ((BufferedImage) image).getRGB(x, y, w, h, output.pixels, 0, width);
    return output;
  }


  /**
   * This is used to both set the pixels[] array so that it can be
   * manipulated, and it also returns a PImage object that can be
   * messed with directly.
   */
  /*
  public PImage get() {
    //PImage outgoing = new PImage(width, height);
    // int[] getRGB(int startX, int startY, int w, int h,
    //              int[] rgbArray, int offset, int scansize)
    if (pixels == null) {
      pixels = new int[width * height];
    }
    ((BufferedImage) image).getRGB(0, 0, width, height, pixels, 0, width);
    return new PImage(pixels, width, height, RGB);
  }
  */


  public void set(int x, int y, int argb) {
    ((BufferedImage) image).setRGB(x, y, argb);
  }


  //////////////////////////////////////////////////////////////


  public void filter(int kind) {
    loadPixels();
    super.filter(kind);
    updatePixels();
  }


  public void filter(int kind, float param) {
    loadPixels();
    super.filter(kind, param);
    updatePixels();
  }


  //////////////////////////////////////////////////////////////


  public void copy(PImage src, int dx, int dy) {
    loadPixels();
    super.copy(src, dx, dy);
    updatePixels();
  }


  public void copy(PImage src, int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    loadPixels();
    super.copy(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2);
    updatePixels();
  }


  public void blend(PImage src, int sx, int sy, int dx, int dy, int mode) {
    loadPixels();
    super.blend(src, sx, sy, dx, dy, mode);
    updatePixels();
  }


  public void blend(int sx, int sy, int dx, int dy, int mode) {
    loadPixels();
    super.blend(sx, sy, dx, dy, mode);
    updatePixels();
  }


  public void blend(int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    loadPixels();
    super.blend(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
    updatePixels();
  }


  public void blend(PImage src, int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    loadPixels();
    super.blend(src, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
    updatePixels();
  }


  public void save(String filename) {
    loadPixels();
    super.save(filename);

    //boolean ImageIO.write(RenderedImage im, String formatName, File output)
  }
}
