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

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;


public class PGraphics2 extends PGraphics {

  protected Graphics2D graphics;


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

    line = new PLine(this);
    triangle = new PTriangle(this);
  }


  //public void defaults() {


  //////////////////////////////////////////////////////////////

  // FRAME


  //public void beginFrame()

  // turn off mis.newPixels
  public void endFrame() {
    // moving this back here (post-68) because of macosx thread problem
    //mis.newPixels(pixels, cm, 0, width);
  }


  public void endShape() {
    vertex_end = vertex_count;

    // don't try to draw if there are no vertices
    // (fixes a bug in LINE_LOOP that re-adds a nonexistent vertex)
    if (vertex_count == 0) {
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


  protected void render_triangles() {
    for (int i = 0; i < triangleCount; i ++) {
      float a[] = vertices[triangles[i][VERTEX1]];
      float b[] = vertices[triangles[i][VERTEX2]];
      float c[] = vertices[triangles[i][VERTEX3]];
      int tex = triangles[i][TEXTURE_INDEX];
      int index = triangles[i][INDEX];

      //System.out.println("A " + a[X] + " " + a[Y] + " " + a[Z]);
      //System.out.println("B " + b[X] + " " + b[Y] + " " + b[Z]);
      //System.out.println("C " + c[X] + " " + c[Y] + " " + c[Z]);

      triangle.reset();

      if (tex > -1 && textures[tex] != null) {
        triangle.setTexture(textures[tex]);
        triangle.setUV(a[U], a[V], b[U], b[V], c[U], c[V]);
      }

      triangle.setIntensities(a[R], a[G], a[B], a[A],
                              b[R], b[G], b[B], b[A],
                              c[R], c[G], c[B], c[A]);

      triangle.setVertices(a[X], a[Y], a[Z],
                           b[X], b[Y], b[Z],
                           c[X], c[Y], c[Z]);

      triangle.setIndex(index);
      triangle.render();
    }
  }


  public void render_lines() {
    for (int i = 0; i < lineCount; i ++) {
      float a[] = vertices[lines[i][VERTEX1]];
      float b[] = vertices[lines[i][VERTEX2]];
      int index = lines[i][INDEX];

      line.reset();

      line.setIntensities(a[SR], a[SG], a[SB], a[SA],
                          b[SR], b[SG], b[SB], b[SA]);

      line.setVertices(a[X], a[Y], a[Z],
                       b[X], b[Y], b[Z]);

      line.setIndex(index);
      line.draw();
    }
  }
}

