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


public class PGraphics2 extends PGraphics {

  Graphics2D graphics;
  GeneralPath path;

  int transformCount;
  AffineTransform transformStack[] =
    new AffineTransform[MATRIX_STACK_DEPTH];
  double transform[] = new double[6];


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


  public void endShape() {
    // don't try to draw if there are no vertices
    // (fixes a bug in LINE_LOOP that re-adds a nonexistent vertex)
    if (vertexCount == 0) {
      shape = 0;
      return;
    }


    // ------------------------------------------------------------------
    // CREATE LINES

    int increment = 1;
    int stop = 0;
    int counter = 0;

    if (stroke) {
      switch (shape) {

        case POINTS:
        {
          stop = vertex_end;
          for (int i = vertex_start; i < stop; i++) {
            add_path();  // total overkill for points
            add_line(i, i);
          }
        }
        break;

        case LINES:
        case LINE_STRIP:
        case LINE_LOOP:
        {
          // store index of first vertex
          int first = lineCount;
          stop = vertex_end - 1;
          increment = (shape == LINES) ? 2 : 1;

          // for LINE_STRIP and LINE_LOOP, make this all one path
          if (shape != LINES) add_path();

          for (int i = vertex_start; i < stop; i+=increment) {
            // for LINES, make a new path for each segment
            if (shape == LINES) add_path();
            add_line(i, i+1);
          }

          // for LINE_LOOP, close the loop with a final segment
          if (shape == LINE_LOOP) {
            add_line(stop, lines[first][VERTEX1]);
          }
        }
        break;

        case TRIANGLES:
        {
          for (int i = vertex_start; i < vertex_end; i += 3) {
            add_path();
            counter = i - vertex_start;
            add_line(i+0, i+1);
            add_line(i+1, i+2);
            add_line(i+2, i+0);
          }
        }
        break;

        case TRIANGLE_STRIP:
        {
          // first draw all vertices as a line strip
          stop = vertex_end-1;

          add_path();
          for (int i = vertex_start; i < stop; i++) {
            counter = i - vertex_start;
            add_line(i,i+1);
          }

          // then draw from vertex (n) to (n+2)
          stop = vertex_end-2;
          for (int i = vertex_start; i < stop; i++) {
            add_path();
            add_line(i,i+2);
          }
        }
        break;

        case TRIANGLE_FAN:
        {
          // this just draws a series of line segments
          // from the center to each exterior point
          for (int i = vertex_start + 1; i < vertex_end; i++) {
            add_path();
            add_line(vertex_start, i);
          }

          // then a single line loop around the outside.
          add_path();
          for (int i = vertex_start + 1; i < vertex_end-1; i++) {
            add_line(i, i+1);
          }
          // closing the loop
          add_line(vertex_end-1, vertex_start + 1);
        }
        break;

        case QUADS:
        {
          for (int i = vertex_start; i < vertex_end; i += 4) {
            add_path();
            counter = i - vertex_start;
            add_line(i+0, i+1);
            add_line(i+1, i+2);
            add_line(i+2, i+3);
            add_line(i+3, i+0);
          }
        }
        break;

        case QUAD_STRIP:
        {
          // first draw all vertices as a line strip
          stop = vertex_end - 1;

          add_path();
          for (int i = vertex_start; i < stop; i++) {
            counter = i - vertex_start;
            add_line(i, i+1);
          }

          // then draw from vertex (n) to (n+3)
          stop = vertex_end-2;
          increment = 2;

          add_path();
          for (int i = vertex_start; i < stop; i += increment) {
            add_line(i, i+3);
          }
        }
        break;

        case POLYGON:
        case CONCAVE_POLYGON:
        case CONVEX_POLYGON:
        {
          // store index of first vertex
          int first = lineCount;
          stop = vertex_end - 1;

          add_path();
          for (int i = vertex_start; i < stop; i++) {
            add_line(i, i+1);
          }
          // draw the last line connecting back to the first point in poly
          add_line(stop, lines[first][VERTEX1]);
        }
        break;
      }
    }


    // ------------------------------------------------------------------
    // CREATE TRIANGLES

    if (fill) {
      switch (shape) {
        case TRIANGLES:
        case TRIANGLE_STRIP:
        {
          stop = vertex_end - 2;
          increment = (shape == TRIANGLES) ? 3 : 1;
          for (int i = vertex_start; i < stop; i += increment) {
            add_triangle(i, i+1, i+2);
          }
        }
        break;

        case QUADS:
        case QUAD_STRIP:
        {
          stop = vertex_count-3;
          increment = (shape == QUADS) ? 4 : 2;

          for (int i = vertex_start; i < stop; i += increment) {
            // first triangle
            add_triangle(i, i+1, i+2);
            // second triangle
            add_triangle(i, i+2, i+3);
          }
        }
        break;

        case POLYGON:
        case CONCAVE_POLYGON:
        case CONVEX_POLYGON:
        {
          triangulate_polygon();
        }
        break;
      }
    }


    // ------------------------------------------------------------------
    // 2D or 3D POINTS FROM MODEL (MX, MY, MZ) TO VIEW SPACE (X, Y, Z)

    if (depth) {
      for (int i = vertex_start; i < vertex_end; i++) {
        float vertex[] = vertices[i];

        vertex[VX] = m00*vertex[MX] + m01*vertex[MY] + m02*vertex[MZ] + m03;
        vertex[VY] = m10*vertex[MX] + m11*vertex[MY] + m12*vertex[MZ] + m13;
        vertex[VZ] = m20*vertex[MX] + m21*vertex[MY] + m22*vertex[MZ] + m23;
        vertex[VW] = m30*vertex[MX] + m31*vertex[MY] + m32*vertex[MZ] + m33;
      }
    } else {
      // if no depth in use, then the points can be transformed simpler
      for (int i = vertex_start; i < vertex_end; i++) {
        vertices[i][X] = m00*vertices[i][MX] + m01*vertices[i][MY] + m03;
        vertices[i][Y] = m10*vertices[i][MX] + m11*vertices[i][MY] + m13;
      }
    }


    // ------------------------------------------------------------------
    // TRANSFORM / LIGHT / CLIP

    light_and_transform();


    // ------------------------------------------------------------------
    // RENDER SHAPES FILLS HERE WHEN NOT DEPTH SORTING

    // if true, the shapes will be rendered on endFrame
    if (hints[DEPTH_SORT]) {
      shape = 0;
      return;
    }

    // set smoothing mode
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              smooth ?
                              RenderingHints.VALUE_ANTIALIAS_ON :
                              RenderingHints.VALUE_ANTIALIAS_OFF);

    if (fill) render_triangles();
    if (stroke) render_lines();

    shape = 0;
  }



  //////////////////////////////////////////////////////////////

  // POINT


  public void point(float x, float y) {
  }

  public void line(float x1, float y1, float x2, float y2) {
  }

  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
  }

  public void rect(float x1, float y1, float x2, float y2) {
  }

  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
  }

  public void image(PImage image, float x1, float y1) {
  }

  public void image(PImage image,
                    float x1, float y1, float x2, float y2) {
  }

  public void image(PImage image,
                    float x1, float y1, float x2, float y2,
                    float u1, float v1, float u2, float v2) {
  }

  //public void cache(PImage image) {
  //}

  //public void cache(PImage images[]) {
  //}

  //public void arcMode(int mode) {
  //}

  public void arc(float start, float stop,
                  float x, float y, float radius) {
  }

  public void arc(float start, float stop,
                  float x, float y, float hr, float vr) {
  }

  //public void ellipseMode(int mode) {
  //}

  public void ellipse(float x, float y, float hradius, float vradius) {
  }

  public void circle(float x, float y, float radius) {
  }

  //public float bezierPoint(float a, float b, float c, float d,
  //                       float t);

  //public float bezierTangent(float a, float b, float c, float d,
  //                       float t);

  public void bezier(float x1, float y1,
                     float x2, float y2,
                     float x3, float y3,
                     float x4, float y4) {
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

  //public float curvePoint(float a, float b, float c, float d, float t);

  //public float curveTangent(float a, float b, float c, float d, float t);

  public void curve(float x1, float y1,
                    float x2, float y2,
                    float x3, float y3,
                    float x4, float y4) {
    // TODO
  }

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
}