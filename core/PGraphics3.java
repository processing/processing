/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PGraphics - main graphics and rendering context
  Part of the Processing project - http://processing.org

  Copyright (c) 2004- Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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
import java.awt.image.*;
//import java.io.*;

public class PGraphics3 extends PGraphics {

  public float m03;
  public float m13;
  public float m20, m21, m22, m23;
  public float m30, m31, m32, m33;

  // ........................................................

  public int cameraMode;

  // perspective setup
  public float cameraFOV;
  public float cameraX, cameraY, cameraZ;
  public float cameraNear, cameraFar;
  public float cameraAspect;

  public float p00, p01, p02, p03; // projection matrix
  public float p10, p11, p12, p13;
  public float p20, p21, p22, p23;
  public float p30, p31, p32, p33;

  // ........................................................

  /// the stencil buffer (only for NEW_GRAPHICS)
  public int stencil[];

  /// zbuffer (only when 3D is in use)
  public float zbuffer[];

  // ........................................................

  /** Maximum lights by default is 8, which is arbitrary,
      but is the minimum defined by OpenGL */
  static final int MAX_LIGHTS = 8;

  /** True if lights are enabled */
  public boolean lights;

  /** True if this light is enabled */
  public boolean light[];

  /** Light positions */
  public float lightX[], lightY[], lightZ[];

  /** Ambient colors for lights.
      Internally these are stored as numbers between 0 and 1. */
  public float lightAmbientR[], lightAmbientG[], lightAmbientB[];

  /** Diffuse colors for lights.
      Internally these are stored as numbers between 0 and 1. */
  public float lightDiffuseR[], lightDiffuseG[], lightDiffuseB[];

  /** Specular colors for lights.
      Internally these are stored as numbers between 0 and 1. */
  public float lightSpecularR[], lightSpecularG[], lightSpecularB[];

  // ........................................................

  // pos of first vertex of current shape in vertices array
  protected int vertex_start;

  // i think vertex_end is actually the last vertex in the current shape
  // and is separate from vertexCount for occasions where drawing happens
  // on endFrame with all the triangles being depth sorted
  protected int vertex_end;

  // used for sorting points when triangulating a polygon
  // warning - maximum number of vertices for a polygon is DEFAULT_VERTICES
  int vertex_order[] = new int[DEFAULT_VERTICES];

  // ........................................................

  public int pathCount;
  public int pathOffset[] = new int[64];
  public int pathLength[] = new int[64];

  // ........................................................

  // lines
  static final int DEFAULT_LINES = 512;
  PLine line;  // used for drawing
  public int lines[][] = new int[DEFAULT_LINES][LINE_FIELD_COUNT];
  public int lineCount;

  // ........................................................

  // triangles
  static final int DEFAULT_TRIANGLES = 256;
  PTriangle triangle; // used for rendering
  public int triangles[][] = new int[DEFAULT_TRIANGLES][TRIANGLE_FIELD_COUNT];
  public int triangleCount;   // total number of triangles

  // cheap picking someday
  int shape_index;

  // ........................................................

  /**
   * IMAGE_SPACE or NORMAL_SPACE, though this should probably
   * be called textureSpace().. hrm
   */
  public int textureMode;

  /**
   * Current horizontal coordinate for texture,
   * will always be between 0 and 1,
   * even if using textureMode(IMAGE_SPACE)
   */
  public float textureU;

  /** Current vertical coordinate for texture, see above. */
  public float textureV;

  public PImage textureImage;

  static final int DEFAULT_TEXTURES = 3;
  protected PImage textures[] = new PImage[DEFAULT_TEXTURES];
  int texture_index;

  // ........................................................

  /**
   * Normals
   */
  public float normalX, normalY, normalZ;
  //protected boolean normalChanged;

  // ........................................................

  // [toxi031031] new & faster sphere code w/ support flexibile resolutions
  // will be set by sphereDetail() or 1st call to sphere()
  public int sphereDetail = 0;
  float sphereX[], sphereY[], sphereZ[];

  // ........................................................


  /**
   * Constructor for the PGraphics3 object.
   * This prototype only exists because of annoying
   * java compilers, and should not be used.
   */
  public PGraphics3() { }


  /**
   * Constructor for the PGraphics3 object. Use this to ensure that
   * the defaults get set properly. In a subclass, use this(w, h)
   * as the first line of a subclass' constructor to properly set
   * the internal fields and defaults.
   *
   * @param iwidth  viewport width
   * @param iheight viewport height
   */
  public PGraphics3(int iwidth, int iheight) {
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
    width = iwidth;
    height = iheight;
    width1 = width - 1;
    height1 = height - 1;

    allocate();

    // clear the screen with the old background color
    background(backgroundColor);

    // init perspective projection based on new dimensions
    cameraFOV = 60; // at least for now
    cameraX = width / 2.0f;
    cameraY = height / 2.0f;
    cameraZ = cameraY / ((float) tan(PI * cameraFOV / 360f));
    cameraNear = cameraZ / 10.0f;
    cameraFar = cameraZ * 10.0f;
    cameraAspect = (float)width / (float)height;

    // init lights (here instead of allocate b/c needed by opengl)
    light = new boolean[MAX_LIGHTS];
    lightX = new float[MAX_LIGHTS];
    lightY = new float[MAX_LIGHTS];
    lightZ = new float[MAX_LIGHTS];
    lightAmbientR = new float[MAX_LIGHTS];
    lightAmbientG = new float[MAX_LIGHTS];
    lightAmbientB = new float[MAX_LIGHTS];
    lightDiffuseR = new float[MAX_LIGHTS];
    lightDiffuseG = new float[MAX_LIGHTS];
    lightDiffuseB = new float[MAX_LIGHTS];
    lightSpecularR = new float[MAX_LIGHTS];
    lightSpecularG = new float[MAX_LIGHTS];
    lightSpecularB = new float[MAX_LIGHTS];

    // reset the cameraMode if PERSPECTIVE or ORTHOGRAPHIC
    // will just be ignored if CUSTOM, the user's hosed anyways
    //if (depth) cameraMode(this.cameraMode);
  }


  protected void allocate() {
    pixelCount = width * height;
    pixels = new int[pixelCount];

    // because of a java 1.1 bug, pixels must be registered as
    // opaque before their first run, the memimgsrc will flicker
    // and run very slowly.
    for (int i = 0; i < pixelCount; i++) pixels[i] = 0xffffffff;

    cm = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff);;
    mis = new MemoryImageSource(width, height, pixels, 0, width);
    mis.setFullBufferUpdates(true);
    mis.setAnimated(true);
    image = Toolkit.getDefaultToolkit().createImage(mis);

    // TODO don't allocate these until depth() is called
    zbuffer = new float[pixelCount];
    stencil = new int[pixelCount];

    line = new PLine(this);
    triangle = new PTriangle(this);
  }


  public void defaults() {
    super.defaults();

    // easiest for beginners
    textureMode(IMAGE_SPACE);

    // better to leave this turned off by default
    noLights();

    lightEnable(0);
    lightAmbient(0, 0, 0, 0);

    light(1, cameraX, cameraY, cameraZ, 255, 255, 255);
  }


  public void beginFrame() {
    super.beginFrame();

    // reset lines
    lineCount = 0;
    if (line != null) line.reset();  // is this necessary?
    pathCount = 0;

    // reset triangles
    triangleCount = 0;
    if (triangle != null) triangle.reset();  // necessary?

    vertex_start = 0;
    //vertex_end = 0;

    // reset textures
    texture_index = 0;

    normal(0, 0, 1);
  }


  public void endFrame() {
    // no need to z order and render
    // shapes were already rendered in endShape();
    // (but can't return, since needs to update memimgsrc
    if (hints[DEPTH_SORT]) {
      if (triangleCount > 0) {
        depth_sort_triangles();
        render_triangles();
      }
      if (lineCount > 0) {
        depth_sort_lines();
        render_lines();
      }
    }

    // blit to screen
    super.endFrame();
  }


  public void beginShape(int kind) {
    shape = kind;

    shape_index = shape_index + 1;
    if (shape_index == -1) {
      shape_index = 0;
    }

    if (hints[DEPTH_SORT]) {
      // continue with previous vertex, line and triangle count
      // all shapes are rendered at endFrame();
      vertex_start = vertexCount;
      vertex_end = 0;

    } else {
      // reset vertex, line and triangle information
      // every shape is rendered at endShape();
      vertexCount = 0;
      if (line != null) line.reset();  // necessary?
      lineCount = 0;
      pathCount = 0;
      if (triangle != null) triangle.reset();  // necessary?
      triangleCount = 0;
    }
    textureImage = null;

    splineVertexCount = 0;
  }


  public void vertex(float x, float y) {
    setup_vertex(x, y, 0);
  }


  public void vertex(float x, float y, float u, float v) {
    texture_vertex(u, v);
    setup_vertex(x, y, 0);
  }


  public void vertex(float x, float y, float z) {
    setup_vertex(x, y, z);
  }


  public void vertex(float x, float y, float z,
                     float u, float v) {
    texture_vertex(u, v);
    setup_vertex(x, y, z);
  }


  protected void setup_vertex(float x, float y, float z) {
    if (vertexCount == vertices.length) {
      float temp[][] = new float[vertexCount<<1][VERTEX_FIELD_COUNT];
      System.arraycopy(vertices, 0, temp, 0, vertexCount);
      vertices = temp;
      message(CHATTER, "allocating more vertices " + vertices.length);
    }
    float vertex[] = vertices[vertexCount++];

    //if (polygon.redundantVertex(x, y, z)) return;

    // user called vertex(), so that invalidates anything queued
    // up for curve vertices. if this is internally called by
    // spline_segment, then splineVertexCount will be saved and restored.
    splineVertexCount = 0;

    vertex[MX] = x;
    vertex[MY] = y;
    vertex[MZ] = z;

    if (fill) {
      vertex[R] = fillR;
      vertex[G] = fillG;
      vertex[B] = fillB;
      vertex[A] = fillA;
    }

    if (stroke) {
      vertex[SR] = strokeR;
      vertex[SG] = strokeG;
      vertex[SB] = strokeB;
      vertex[SA] = strokeA;
      vertex[SW] = strokeWeight;
    }

    if (textureImage != null) {
      vertex[U] = textureU;
      vertex[V] = textureV;
    }

    //if (normalChanged) {
    vertex[NX] = normalX;
    vertex[NY] = normalY;
    vertex[NZ] = normalZ;
    //}
  }


  /**
   * set UV coords for the next vertex in the current shape.
   * this is ugly as its own fxn, and will almost always be
   * coincident with a call to vertex, so it's being moved
   * to be an optional param of and overloaded vertex()
   *
   * @param  u  U coordinate (X coord in image 0<=X<=image width)
   * @param  v  V coordinate (Y coord in image 0<=Y<=image height)
   */
  protected void texture_vertex(float u, float v) {
    if (textureImage == null) {
      message(PROBLEM, "gotta use texture() " +
              "after beginShape() and before vertex()");
      return;
    }
    if (textureMode == IMAGE_SPACE) {
      u /= (float) textureImage.width;
      v /= (float) textureImage.height;
    }

    textureU = u;
    textureV = v;

    if (textureU < 0) textureU = 0;
    else if (textureU > ONE) textureU = ONE;

    if (textureV < 0) textureV = 0;
    else if (textureV > ONE) textureV = ONE;
  }


  protected void spline_vertex(float x, float y, float z, boolean bezier) {
    // allocate space for the spline vertices
    // to improve processing applet load times, don't allocate until actual use
    if (splineVertices == null) {
      splineVertices = new float[DEFAULT_SPLINE_VERTICES][VERTEX_FIELD_COUNT];
    }

    // if more than 128 points, shift everything back to the beginning
    if (splineVertexCount == DEFAULT_SPLINE_VERTICES) {
      System.arraycopy(splineVertices[DEFAULT_SPLINE_VERTICES-3], 0,
                       splineVertices[0], 0, VERTEX_FIELD_COUNT);
      System.arraycopy(splineVertices[DEFAULT_SPLINE_VERTICES-2], 0,
                       splineVertices[1], 0, VERTEX_FIELD_COUNT);
      System.arraycopy(splineVertices[DEFAULT_SPLINE_VERTICES-1], 0,
                       splineVertices[2], 0, VERTEX_FIELD_COUNT);
      splineVertexCount = 3;
    }

    // 'flat' may be a misnomer here because it's actually just
    // calculating whether z is zero for all the spline points,
    // so that it knows whether to calculate all three params,
    // or just two for x and y.
    //if (spline_vertices_flat) {
    //if (z != 0) spline_vertices_flat = false;
    //}
    float vertex[] = splineVertices[splineVertexCount];

    vertex[MX] = x;
    vertex[MY] = y;
    vertex[MZ] = z;

    if (fill) {
      vertex[R] = fillR;
      vertex[G] = fillG;
      vertex[B] = fillB;
      vertex[A] = fillA;
    }

    if (stroke) {
      vertex[SR] = strokeR;
      vertex[SG] = strokeG;
      vertex[SB] = strokeB;
      vertex[SA] = strokeA;
      vertex[SW] = strokeWeight;
    }

    // this complicated "if" construct may defeat the purpose
    if (textureImage != null) {
      vertex[U] = textureU;
      vertex[V] = textureV;
    }

    //if (normalChanged) {
    vertex[NX] = normalX;
    vertex[NY] = normalY;
    vertex[NZ] = normalZ;
    //}

    splineVertexCount++;

    // draw a segment if there are enough points
    if (splineVertexCount > 3) {
      if (bezier) {
        if ((splineVertexCount % 4) == 0) {
          if (!bezier_inited) bezier_init();
          spline3_segment(splineVertexCount-4,
                          splineVertexCount-4,
                          bezier_draw,
                          bezier_detail);
        }
      } else {  // catmull-rom curve (!bezier)
        if (!curve_inited) curve_init();
        spline3_segment(splineVertexCount-4,
                        splineVertexCount-3,
                        curve_draw,
                        curve_detail);
      }
    }
  }


  /**
   * See notes with the bezier() function.
   */
  public void bezierVertex(float x, float y) {
    spline_vertex(x, y, 0, true);
  }

  /**
   * See notes with the bezier() function.
   */
  public void bezierVertex(float x, float y, float z) {
    spline_vertex(x, y, z, true);
  }

  /**
   * See notes with the curve() function.
   */
  public void curveVertex(float x, float y) {
    spline_vertex(x, y, 0, false);
  }

  /**
   * See notes with the curve() function.
   */
  public void curveVertex(float x, float y, float z) {
    spline_vertex(x, y, z, false);
  }


  /**
   * Sets the current normal. Mostly will apply to vertices
   * inside a beginShape/endShape block.
   */
  public void normal(float nx, float ny, float nz) {
    // if drawing a shape and the normal hasn't changed yet,
    // then need to set all the normal for each vertex so far
    /*
    if ((shape != 0) && !normalChanged) {
      for (int i = vertex_start; i < vertex_end; i++) {
        vertices[i][NX] = normalX;
        vertices[i][NY] = normalY;
        vertices[i][NZ] = normalZ;
      }
      normalChanged = true;
    }
    */
    normalX = nx;
    normalY = ny;
    normalZ = nz;
  }


  /**
   * set texture mode to either IMAGE_SPACE (more intuitive
   * for new users) or NORMAL_SPACE (better for advanced chaps)
   */
  public void textureMode(int mode) {
    this.textureMode = mode;
  }


  /**
   * set texture image for current shape
   * needs to be called between @see beginShape and @see endShape
   *
   * @param image reference to a PImage object
   */
  public void texture(PImage image) {
    textureImage = image;

    if (texture_index == textures.length - 1) {
      PImage temp[] = new PImage[texture_index<<1];
      System.arraycopy(textures, 0, temp, 0, texture_index);
      textures = temp;
      message(CHATTER, "allocating more textures " + textures.length);
    }

    if (textures[0] != null) {  // wHY?
      texture_index++;
    }

    textures[texture_index] = image;
  }


  public void endShape() {
    vertex_end = vertexCount;

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
          stop = vertexCount-3;
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

    for (int i = vertex_start; i < vertex_end; i++) {
      float vertex[] = vertices[i];

      vertex[VX] = m00*vertex[MX] + m01*vertex[MY] + m02*vertex[MZ] + m03;
      vertex[VY] = m10*vertex[MX] + m11*vertex[MY] + m12*vertex[MZ] + m13;
      vertex[VZ] = m20*vertex[MX] + m21*vertex[MY] + m22*vertex[MZ] + m23;
      vertex[VW] = m30*vertex[MX] + m31*vertex[MY] + m32*vertex[MZ] + m33;
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

    if (fill) render_triangles();
    if (stroke) render_lines();

    shape = 0;
  }


  protected final void add_path() {
    if (pathCount == pathOffset.length) {
      int temp1[] = new int[pathCount << 1];
      System.arraycopy(pathOffset, 0, temp1, 0, pathCount);
      pathOffset = temp1;
      int temp2[] = new int[pathCount << 1];
      System.arraycopy(pathLength, 0, temp2, 0, pathCount);
      pathLength = temp2;
    }
    pathOffset[pathCount] = lineCount;
    pathLength[pathCount] = 0;
    pathCount++;
  }


  protected final void add_line(int a, int b) {
    if (lineCount == lines.length) {
      int temp[][] = new int[lineCount<<1][LINE_FIELD_COUNT];
      System.arraycopy(lines, 0, temp, 0, lineCount);
      lines = temp;
      message(CHATTER, "allocating more lines " + lines.length);
    }
    lines[lineCount][VERTEX1] = a;
    lines[lineCount][VERTEX2] = b;
    lines[lineCount][INDEX] = -1;

    lines[lineCount][STROKE_MODE] = strokeCap | strokeJoin;
    lines[lineCount][STROKE_WEIGHT] = (int) (strokeWeight + 0.5f); // hmm
    lineCount++;

    // mark this piece as being part of the current path
    pathLength[pathCount-1]++;
  }


  protected final void add_triangle(int a, int b, int c) {
    if (triangleCount == triangles.length) {
      int temp[][] = new int[triangleCount<<1][TRIANGLE_FIELD_COUNT];
      System.arraycopy(triangles, 0, temp, 0, triangleCount);
      triangles = temp;
      message(CHATTER, "allocating more triangles " + triangles.length);
    }
    triangles[triangleCount][VERTEX1] = a;
    triangles[triangleCount][VERTEX2] = b;
    triangles[triangleCount][VERTEX3] = c;

    if (textureImage == null) {
      triangles[triangleCount][TEXTURE_INDEX] = -1;
    } else {
      triangles[triangleCount][TEXTURE_INDEX] = texture_index;
    }

    triangles[triangleCount][INDEX] = shape_index;
    triangleCount++;
  }


  protected void depth_sort_triangles() {
  }

  protected void render_triangles() {
    for (int i = 0; i < triangleCount; i ++) {
      float a[] = vertices[triangles[i][VERTEX1]];
      float b[] = vertices[triangles[i][VERTEX2]];
      float c[] = vertices[triangles[i][VERTEX3]];
      int tex = triangles[i][TEXTURE_INDEX];
      int index = triangles[i][INDEX];

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


  protected void depth_sort_lines() {
  }

  protected void render_lines() {
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


  /**
   * triangulate the current polygon.
   * simple ear clipping polygon triangulation adapted
   * from code by john w. ratcliff (jratcliff at verant.com)
   */
  private void triangulate_polygon() {

    // first we check if the polygon goes clockwise or counterclockwise
    float area = 0.0f;
    for (int p = vertex_end - 1, q = vertex_start; q < vertex_end; p = q++) {
      area += (vertices[q][X] * vertices[p][Y] -
               vertices[p][X] * vertices[q][Y]);
    }

    // then sort the vertices so they are always in a counterclockwise order
    int j = 0;
    //if (0.0f < area) {  // def <
    if (area > 0) {
      for (int i = vertex_start; i < vertex_end; i++) {
        j = i - vertex_start;
        vertex_order[j] = i;
      }
    } else {
      for (int i = vertex_start; i < vertex_end; i++) {
        j = i - vertex_start;
        vertex_order[j] = (vertex_end - 1) - j;
      }
    }

    // remove vc-2 Vertices, creating 1 triangle every time
    int vc = vertex_end - vertex_start;
    int count = 2*vc;  // complex polygon detection

    for (int m = 0, v = vc - 1; vc > 2; ) {
      boolean snip = true;

      // if we start over again, is a complex polygon
      if (0 >= (count--)) {
        break; // triangulation failed
      }

      // get 3 consecutive vertices <u,v,w>
      int u = v ; if (vc <= u) u = 0;    // previous
      v = u + 1; if (vc <= v) v = 0;     // current
      int w = v + 1; if (vc <= w) w = 0; // next

      // triangle A B C
      //float Ax, Ay, Bx, By, Cx, Cy, Px, Py;

      float Ax = -vertices[vertex_order[u]][X];
      float Ay =  vertices[vertex_order[u]][Y];
      float Bx = -vertices[vertex_order[v]][X];
      float By =  vertices[vertex_order[v]][Y];
      float Cx = -vertices[vertex_order[w]][X];
      float Cy =  vertices[vertex_order[w]][Y];

      // first we check if <u,v,w> continues going ccw
      if (EPSILON > (((Bx-Ax) * (Cy-Ay)) - ((By-Ay) * (Cx-Ax)))) {
        continue;
      }

      for (int p = 0; p < vc; p++) {
        //float ax, ay, bx, by, cx, cy, apx, apy, bpx, bpy, cpx, cpy;
        //float cCROSSap, bCROSScp, aCROSSbp;

        if( (p == u) || (p == v) || (p == w) ) {
          continue;
        }

        float Px = -vertices[vertex_order[p]][X];
        float Py =  vertices[vertex_order[p]][Y];

        float ax  = Cx - Bx;  float ay  = Cy - By;
        float bx  = Ax - Cx;  float by  = Ay - Cy;
        float cx  = Bx - Ax;  float cy  = By - Ay;
        float apx = Px - Ax;  float apy = Py - Ay;
        float bpx = Px - Bx;  float bpy = Py - By;
        float cpx = Px - Cx;  float cpy = Py - Cy;

        float aCROSSbp = ax * bpy - ay * bpx;
        float cCROSSap = cx * apy - cy * apx;
        float bCROSScp = bx * cpy - by * cpx;

        if ((aCROSSbp >= 0.0f) && (bCROSScp >= 0.0f) && (cCROSSap >= 0.0f)) {
          snip = false;
        }
      }

      if (snip) {
        add_triangle(vertex_order[u], vertex_order[v], vertex_order[w]);

        m++;

        // remove v from remaining polygon
        for (int s = v, t = v + 1; t < vc; s++, t++) {
          vertex_order[s] = vertex_order[t];
        }
        vc--;

        // reset error detection counter
        count = 2 * vc;
      }
    }
  }



  //////////////////////////////////////////////////////////////

  // LIGHTS AND COLOR


  /**
   * This method handles the transformation, lighting, and clipping
   * operations for the shapes. Broken out as a separate function
   * so that other renderers can override. For instance, with OpenGL,
   * this section is all handled on the graphics card.
   */
  protected void light_and_transform() {

    // ------------------------------------------------------------------
    // CULLING

    // simple culling
    // if they share the same clipping code, then cull
    /*
    boolean clipped = true;
    float x = vertices[vertex_start][X];
    float y = vertices[vertex_start][Y];
    int clipCode = ((y < 0 ? 8 : 0) | (y > height1 ? 4 : 0) |
            (x < 0 ? 2 : 0) | (x > width1 ? 1 : 0));
    for (int i = vertex_start + 1; i < vertex_end; i++) {
      x = vertices[i][X];
      y = vertices[i][Y];
      int code = ((y < 0 ? 8 : 0) | (y > height1 ? 4 : 0) |
            (x < 0 ? 2 : 0) | (x > width1 ? 1 : 0));
      if (code != clipCode) {
        clipped = false;
        break;
      }
    }
    if ((clipCode != 0) && clipped) return;
    */


    // ------------------------------------------------------------------
    // NORMALS

    /*
    if (!normalChanged) {
      // fill first vertext w/ the normal
      vertices[vertex_start][NX] = normalX;
      vertices[vertex_start][NY] = normalY;
      vertices[vertex_start][NZ] = normalZ;
      // homogenousNormals saves time from below, which is expensive
    }
    */

    for (int i = vertex_start; i < vertex_end; i++) {
      float v[] = vertices[i];
      float nx = m00*v[NX] + m01*v[NY] + m02*v[NZ] + m03;
      float ny = m10*v[NX] + m11*v[NY] + m12*v[NZ] + m13;
      float nz = m20*v[NX] + m21*v[NY] + m22*v[NZ] + m23;
      float nw = m30*v[NX] + m31*v[NY] + m32*v[NZ] + m33;

      if (nw != 0) {
        // divide by perspective coordinate
        v[NX] = nx/nw; v[NY] = ny/nw; v[NZ] = nz/nw;
      } else {
        // can't do inline above
        v[NX] = nx; v[NY] = ny; v[NZ] = nz;
      }

      float nlen = mag(v[NX], v[NY], v[NZ]);  // normalize
      if (nlen != 0) {
        v[NX] /= nlen; v[NY] /= nlen; v[NZ] /= nlen;
      }
    }


    // ------------------------------------------------------------------
    // LIGHTS

    // if no lights enabled, then all the values for r, g, b
    // have been set with calls to vertex() (no need to re-calculate here)

    if (lights) {
      float f[] = vertices[vertex_start];

      for (int i = vertex_start; i < vertex_end; i++) {
        float v[] = vertices[i];
        if (fill) {
          calc_lighting(v[R],  v[G], v[B],
                        v[MX], v[MY], v[MZ],
                        v[NX], v[NY], v[NZ], v, R);
        }
        if (stroke) {
          calc_lighting(v[SR], v[SG], v[SB],
                        v[MX], v[MY], v[MZ],
                        v[NX], v[NY], v[NZ], v, SR);
        }
      }
    }


    // ------------------------------------------------------------------
    // NEAR PLANE CLIPPING AND CULLING

    //if ((cameraMode == PERSPECTIVE) && (dimensions == 3) && clip) {
      //float z_plane = eyeDist + ONE;

      //for (int i = 0; i < lineCount; i ++) {
          //line3dClip();
      //}

      //for (int i = 0; i < triangleCount; i ++) {
      //}
    //}


    // ------------------------------------------------------------------
    // POINTS FROM VIEW SPACE (VX, VY, VZ) TO SCREEN SPACE (X, Y, Z)

    for (int i = vertex_start; i < vertex_end; i++) {
      float vx[] = vertices[i];

      float ox = p00*vx[VX] + p01*vx[VY] + p02*vx[VZ] + p03*vx[VW];
      float oy = p10*vx[VX] + p11*vx[VY] + p12*vx[VZ] + p13*vx[VW];
      float oz = p20*vx[VX] + p21*vx[VY] + p22*vx[VZ] + p23*vx[VW];
      float ow = p30*vx[VX] + p31*vx[VY] + p32*vx[VZ] + p33*vx[VW];

      if (ow != 0) {
        ox /= ow; oy /= ow; oz /= ow;
      }

      vx[X] = width * (ONE + ox) / 2.0f;
      vx[Y] = height * (ONE + oy) / 2.0f;
      vx[Z] = (oz + ONE) / 2.0f;
    }
  }


  /**
   * lighting calculation of final colour.
   * for now, ip is being done in screen space (transformed),
   * because the normals are also being transformed
   *
   * @param  r        red component of object's colour
   * @param  g        green of object's colour
   * @param  b        blue of object's colour
   * @param  ix       x coord of intersection
   * @param  iy       y coord of intersection
   * @param  iz       z coord of intersection
   * @param  nx       x coord of normal vector
   * @param  ny       y coord of normal
   * @param  nz       z coord of normal
   * @param  target   float array to store result
   * @param  toffset  starting index in target array
   */
  private void calc_lighting(float r, float g, float b,
                             float ix, float iy, float iz,
                             float nx, float ny, float nz,
                             float target[], int toffset) {
    //System.out.println("calc_lighting normals " + nx + " " + ny + " " + nz);

    if (!lights) {
      target[toffset + 0] = r;
      target[toffset + 1] = g;
      target[toffset + 2] = b;
      return;
    }

    float nlen = mag(nx, ny, nz);
    if (nlen != 0) {
      nx /= nlen; ny /= nlen; nz /= nlen;
    }

    // get direction based on inverse of perspective(?) matrix
    //screenToWorld.getDirection(x + 0.5, y + 0.5, d);

    /*
      // q in screen space
      double qs[] = new double[4];
      qs[0] = x;
      qs[1] = y;
      qs[2] = 0;
      qs[3] = 1;

      // q in world space
      // transformed 4 vector (homogenous coords)
      double qw[] = new double[4];
      multiply(mat, qs, qw);
      dw.x = qw[0] * mat[3][2] - qw[3] * mat[0][2];
      dw.y = qw[1] * mat[3][2] - qw[3] * mat[1][2];
      dw.z = qw[2] * mat[3][2] - qw[3] * mat[2][2];
    */
    // multiply (inverse matrix) x (x y 0 1) = qw

    /*
      // CALC OF DIRECTION OF EYE TO SCREEN/OBJECT
      // !!! don't delete this code.. used for specular
      float qwx = i00*sx + i01*sy + i03;
      float qwy = i10*sx + i11*sy + i13;
      float qwz = i20*sx + i21*sy + i23;
      float qww = i30*sx + i31*sy + i33;

      float dwx = qwx*i32 - qww*i02;
      float dwy = qwy*i32 - qww*i12;
      float dwz = qwz*i32 - qww*i22;
    */

    //double kdr = material.kDiffuseReflection;   == 1
    //double ksr = material.kSpecularReflection;  == 0
    //double e = material.shadingExponent;        == 0
    //RgbColor Cmat = material.color;             == r, g, b

    // Direction of light i from ip, Li = L[i].position - ip
    //Vector3 Li = new Vector3();

    // Radiance of a light source, a color
    //RgbColor Ii = new RgbColor();

    // The halfway vector
    //Vector3 Hi = new Vector3();

    //float N_dot_Li, N_dot_Hi, N_dot_Hi_e;

    float diffuse_r = 0; // = lights[0].r;  // sum in ambient term
    float diffuse_g = 0; // = lights[0].g;
    float diffuse_b = 0; // = lights[0].b;

    //float specular_r = 0;
    //float specular_g = 0;
    //float specular_b = 0;

    for (int i = 1; i < MAX_LIGHTS; i++) {
      if (!light[i]) continue;

      //Light light = (Light) list.value;
      //Ii = light.color;

      //Vector3.subtract(light.position, ip, Li);
      //Li.normalize();
      // li is the vector of the light as it points towards the point
      // at which it intersects the object
      float lix = lightX[i] - ix;
      float liy = lightY[i] - iy;
      float liz = lightZ[i] - iz;
      float m = mag(lix, liy, liz);
      if (m != 0) {
        lix /= m; liy /= m; liz /= m;
      }
      float n_dot_li = (nx*lix + ny*liy + nz*liz);
      //N_dot_Li = Vector3.dotProduct(N, Li);

      //if (N_dot_Li > 0.0) {
      if (n_dot_li > 0) {
        //System.out.println("n_dot_li = " + n_dot_li);
        diffuse_r += lightDiffuseR[i] * n_dot_li;
        diffuse_g += lightDiffuseG[i] * n_dot_li;
        diffuse_b += lightDiffuseB[i] * n_dot_li;

        /*
          // not doing any specular for now

          //Vector3.subtract(light.position, direction, Hi);
          float hix = lights[i].x - dwx;
          float hiy = lights[i].y - dwy;
          float hiz = lights[i].z - dwz;
          float n_dot_hi = (nx*hix + ny*hiy + nz*hiz);
          //N_dot_Hi = Vector3.dotProduct(N, Hi);
          if (n_dot_hi > 0) {
          //N_dot_Hi_e = pow(N_dot_Hi / Hi.getLength(), e);
          // since e == 1 for now, this can be simplified
          //float n_dot_hi_e = pow(n_dot_hi / sqrt(hix*hix + hiy*hiy + hiz*hiz), e);
          float n_dot_hi_e = n_dot_hi /
          sqrt(hix*hix + hiy*hiy + hiz*hiz);
          specular_r += lights[i].r * n_dot_hi_e;
          specular_g += lights[i].g * n_dot_hi_e;
          specular_b += lights[i].b * n_dot_hi_e;
          //specular_r += Ii.r * N_dot_Hi_e;
          //specular_g += Ii.g * N_dot_Hi_e;
          //specular_b += Ii.b * N_dot_Hi_e;
          }
        */
      }
    }
    // specular reflection (ksr) is set to zero, so simplify
    //I.r = (kdr * Cmat.r * diffuse_r) + (ksr * specular_r);
    //I.g = (kdr * Cmat.g * diffuse_g) + (ksr * specular_g);
    //I.b = (kdr * Cmat.b * diffuse_b) + (ksr * specular_b);

    //System.out.println(r + " " + g + " " + b + "  " +
    //               diffuse_r + " " + diffuse_g + " " + diffuse_b);

    // TODO ** this sucks! **
    //System.out.println(lights[0].r + " " + lights[0].g + " " +
    //               lights[0].b);

    target[toffset+0] = lightAmbientR[0] + (r * diffuse_r);
    target[toffset+1] = lightAmbientG[0] + (g * diffuse_g);
    target[toffset+2] = lightAmbientB[0] + (b * diffuse_b);

    if (target[toffset+0] > ONE) target[toffset+0] = ONE;
    if (target[toffset+1] > ONE) target[toffset+1] = ONE;
    if (target[toffset+2] > ONE) target[toffset+2] = ONE;

    //if (calc1) {
    //calcR1 = lights[0].r + (r * diffuse_r); if (calcR1 > 1) calcR1 = 1;
    //calcG1 = lights[0].g + (g * diffuse_g); if (calcG1 > 1) calcG1 = 1;
    //calcB1 = lights[0].b + (b * diffuse_b); if (calcB1 > 1) calcB1 = 1;

    //System.out.println(255*calcR1 + " " + 255*calcG1 + " " + 255*calcB1);
    //} else {
    //calcR2 = lights[0].r + (r * diffuse_r); if (calcR2 > 1) calcR2 = 1;
    //calcG2 = lights[0].g + (g * diffuse_g); if (calcG2 > 1) calcG2 = 1;
    //calcB2 = lights[0].b + (b * diffuse_b); if (calcB2 > 1) calcB2 = 1;
    //System.out.println(255*calcR2 + " " + 255*calcG2 + " " + 255*calcB2);
    //}
  }


  private void point3(float x, float y, float z, int color) {
    // need to get scaled version of the stroke
    float x1 = screenX(x - 0.5f, y - 0.5f, z);
    float y1 = screenY(x - 0.5f, y - 0.5f, z);
    float x2 = screenX(x + 0.5f, y + 0.5f, z);
    float y2 = screenY(x + 0.5f, y + 0.5f, z);

    float weight = (abs(x2 - x1) + abs(y2 - y1)) / 2f;
    if (weight < 1.5f) {
      int xx = (int) ((x1 + x2) / 2f);
      int yy = (int) ((y1 + y2) / 2f);
      //point0(xx, yy, z, color);
      zbuffer[yy*width + xx] = screenZ(x, y, z);
      //stencil?

    } else {
      // actually has some weight, need to draw shapes instead
      // these will be
    }
  }



  //////////////////////////////////////////////////////////////

  // SHAPES

  public void point(float x, float y) {
    point(x, y, 0);
  }

  public void point(float x, float y, float z) {
  }

  public void line(float x1, float y1, float x2, float y2) {
    line(x1, y1, 0, x2, y2, 0);
  }

  public void line(float x1, float y1, float z1,
                   float x2, float y2, float z2) {
  }

  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
  }

  //public void rectMode(int mode)

  public void rect(float x1, float y1, float x2, float y2) {
  }

  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
  }

  public void circle(float x, float y, float radius) {
  }

  public void ellipseMode(int mode) {
  }

  public void ellipse(float x, float y, float hradius, float vradius) {
  }

  //public void arcMode(int mode)

  public void arc(float start, float stop,
                  float x, float y, float radius) {
  }

  public void arc(float start, float stop,
                  float x, float y, float hr, float vr) {
  }


  //////////////////////////////////////////////////////////////

  // CURVES


  public void bezier(float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     float x3, float y3, float z3,
                     float x4, float y4, float z4) {
    beginShape(LINE_STRIP);
    bezierVertex(x1, y1, z1);
    bezierVertex(x2, y2, z2);
    bezierVertex(x3, y3, z3);
    bezierVertex(x4, y4, z4);
    endShape();
  }



  //////////////////////////////////////////////////////////////

  // 3D BOX


  // solid or wire depends on settings for stroke and fill
  // slices/stacks can be set by an advanced option

  //public void cube(float size) {
  public void box(float size) {
    //box(-size/2, -size/2, -size/2,  size/2, size/2, size/2);
    box(size, size, size);
  }


  // OPT this isn't the least bit efficient
  //     because it redraws lines along the vertices
  //     ugly ugly ugly!
  //public void box(float x1, float y1, float z1,
  //          float x2, float y2, float z2) {
  public void box(float w, float h, float d) {
    float x1 = -w/2f; float x2 = w/2f;
    float y1 = -h/2f; float y2 = h/2f;
    float z1 = -d/2f; float z2 = d/2f;

    //if (hints[NEW_GRAPHICS]) triangle.setCulling(true);
    triangle.setCulling(true);

    beginShape(QUADS);

    // front
    vertex(x1, y1, z1);
    vertex(x2, y1, z1);
    vertex(x2, y2, z1);
    vertex(x1, y2, z1);

    // right
    vertex(x2, y1, z1);
    vertex(x2, y1, z2);
    vertex(x2, y2, z2);
    vertex(x2, y2, z1);

    // back
    vertex(x2, y1, z2);
    vertex(x1, y1, z2);
    vertex(x1, y2, z2);
    vertex(x2, y2, z2);

    // left
    vertex(x1, y1, z2);
    vertex(x1, y1, z1);
    vertex(x1, y2, z1);
    vertex(x1, y2, z2);

    // top
    vertex(x1, y1, z2);
    vertex(x2, y1, z2);
    vertex(x2, y1, z1);
    vertex(x1, y1, z1);

    // bottom
    vertex(x1, y2, z1);
    vertex(x2, y2, z1);
    vertex(x2, y2, z2);
    vertex(x1, y2, z2);

    endShape();

    //if (hints[NEW_GRAPHICS]) triangle.setCulling(false);
    triangle.setCulling(false);
  }



  //////////////////////////////////////////////////////////////

  // 3D SPHERE


  // [toxi031031] used by the new sphere code below
  // precompute vertices along unit sphere with new detail setting

  public void sphereDetail(int res) {
    if (res < 3) res = 3; // force a minimum res
    if (res == sphereDetail) return;

    float delta = (float)SINCOS_LENGTH/res;
    float[] cx = new float[res];
    float[] cz = new float[res];
    // calc unit circle in XZ plane
    for (int i = 0; i < res; i++) {
      cx[i] = cosLUT[(int) (i*delta) % SINCOS_LENGTH];
      cz[i] = sinLUT[(int) (i*delta) % SINCOS_LENGTH];
    }
    // computing vertexlist
    // vertexlist starts at south pole
    int vertCount = res * (res-1) + 2;
    int currVert = 0;

    // re-init arrays to store vertices
    sphereX = new float[vertCount];
    sphereY = new float[vertCount];
    sphereZ = new float[vertCount];

    float angle_step = (SINCOS_LENGTH*0.5f)/res;
    float angle = angle_step;

    // step along Y axis
    for (int i = 1; i < res; i++) {
      float curradius = sinLUT[(int) angle % SINCOS_LENGTH];
      float currY = -cosLUT[(int) angle % SINCOS_LENGTH];
      for (int j = 0; j < res; j++) {
        sphereX[currVert] = cx[j] * curradius;
        sphereY[currVert] = currY;
        sphereZ[currVert++] = cz[j] * curradius;
      }
      angle += angle_step;
    }
    sphereDetail = res;
  }


  // cache all the points of the sphere in a static array
  // top and bottom are just a bunch of triangles that land
  // in the center point

  // sphere is a series of concentric circles who radii vary
  // along the shape, based on, er.. cos or something

  // [toxi031031] new sphere code. removed all multiplies with
  // radius, as scale() will take care of that anyway

  // [toxi031223] updated sphere code (removed modulos)
  // and introduced sphereAt(x,y,z,r)
  // to avoid additional translate()'s on the user/sketch side
  public void sphere(float r) {
    sphere(0, 0, 0, r);
  }

  public void sphere(float x, float y, float z, float r) {
    if (sphereDetail == 0) {
      sphereDetail(30);
    }

    int v1,v11,v2;
    push();
    if (x!=0f && y!=0f && z!=0f) translate(x,y,z);
    scale(r);

    //if (hints[NEW_GRAPHICS]) triangle.setCulling(true);
    triangle.setCulling(true);

    // 1st ring from south pole
    beginShape(TRIANGLE_STRIP);
    for (int i = 0; i < sphereDetail; i++) {
      vertex(0, -1, 0);
      vertex(sphereX[i], sphereY[i], sphereZ[i]);
    }
    vertex(0, -1, 0);
    vertex(sphereX[0], sphereY[0], sphereZ[0]);
    endShape();

    // middle rings
    int voff = 0;
    for(int i = 2; i < sphereDetail; i++) {
      v1=v11=voff;
      voff += sphereDetail;
      v2=voff;
      beginShape(TRIANGLE_STRIP);
      for (int j = 0; j < sphereDetail; j++) {
        vertex(sphereX[v1], sphereY[v1], sphereZ[v1++]);
        vertex(sphereX[v2], sphereY[v2], sphereZ[v2++]);
      }
      // close each ring
      v1=v11;
      v2=voff;
      vertex(sphereX[v1], sphereY[v1], sphereZ[v1]);
      vertex(sphereX[v2], sphereY[v2], sphereZ[v2]);
      endShape();
    }

    // add the northern cap
    beginShape(TRIANGLE_STRIP);
    for (int i = 0; i < sphereDetail; i++) {
      v2 = voff + i;
      vertex(0, 1, 0);
      vertex(sphereX[v2], sphereY[v2], sphereZ[v2]);
    }
    vertex(0, 1, 0);
    vertex(sphereX[voff], sphereY[voff], sphereZ[voff]);
    endShape();
    pop();

    //if (hints[NEW_GRAPHICS]) triangle.setCulling(false);
    triangle.setCulling(false);
  }



  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS


  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }


  public void translate(float tx, float ty, float tz) {
    m03 += tx*m00 + ty*m01 + tz*m02;
    m13 += tx*m10 + ty*m11 + tz*m12;
    m23 += tx*m20 + ty*m21 + tz*m22;
    m33 += tx*m30 + ty*m31 + tz*m32;
  }


  // OPT could save several multiplies for the 0s and 1s by just
  //     putting the multMatrix code here and removing uneccessary terms

  public void rotateX(float angle) {
    //dimensions = 3;
    float c = cos(angle);
    float s = sin(angle);
    applyMatrix(1, 0, 0, 0,  0, c, -s, 0,  0, s, c, 0,  0, 0, 0, 1);
  }


  public void rotateY(float angle) {
    //dimensions = 3;
    float c = cos(angle);
    float s = sin(angle);
    applyMatrix(c, 0, s, 0,  0, 1, 0, 0,  -s, 0, c, 0,  0, 0, 0, 1);
  }


  /**
   * Two dimensional rotation. Same as rotateZ (this is identical
   * to a 3D rotation along the z-axis) but included for clarity --
   * it'd be weird for people drawing 2D graphics to be using rotateZ.
   * And they might kick our a-- for the confusion.
   */
  public void rotate(float angle) {
    rotateZ(angle);
  }


  /**
   * Rotate in the XY plane by an angle.
   *
   * Note that this doesn't internally set the number of
   * dimensions to three, since rotateZ() is the same as a
   * 2D rotate in the XY plane.
   */
  public void rotateZ(float angle) {
    //rotate(angle, 0, 0, 1);
    //if (dimensions == 0) dimensions = 2;  // otherwise already 2 or higher
    float c = cos(angle);
    float s = sin(angle);
    applyMatrix(c, -s, 0, 0,  s, c, 0, 0,  0, 0, 1, 0,  0, 0, 0, 1);
  }


  /**
   * Rotate around an arbitrary vector, similar to glRotate(),
   * except that it takes radians (instead of degrees) by default,
   * unless angleMode is set to RADIANS.
   */
  public void rotate(float angle, float v0, float v1, float v2) {
    // should be in radians (i think), instead of degrees (gl uses degrees)
    // based on 15-463 code, but similar to opengl ref p.443

    //modelMatrixIsIdentity = false;
    //dimensions = 3;

    // TODO should make sure this vector is normalized

    float c = cos(angle);
    float s = sin(angle);
    float t = 1.0f - c;

    applyMatrix((t*v0*v0) + c, (t*v0*v1) - (s*v2), (t*v0*v2) + (s*v1), 0,
                (t*v0*v1) + (s*v2), (t*v1*v1) + c, (t*v1*v2) - (s*v0), 0,
                (t*v0*v2) - (s*v1), (t*v1*v2) + (s*v0), (t*v2*v2) + c, 0,
                0, 0, 0, 1);
  }


  /**
   * This will scale in all three dimensions, but not set the
   * dimensions higher than two, in case this is still 2D mode.
   */
  public void scale(float s) {
    applyMatrix(s, 0, 0, 0,  0, s, 0, 0,  0, 0, s, 0,  0, 0, 0, 1);
  }


  public void scale(float sx, float sy) {
    applyMatrix(sx, 0, 0, 0,  0, sy, 0, 0,  0, 0, 1, 0,  0, 0, 0, 1);
  }


  public void scale(float x, float y, float z) {
    applyMatrix(x, 0, 0, 0,  0, y, 0, 0,  0, 0, z, 0,  0, 0, 0, 1);
  }



  //////////////////////////////////////////////////////////////

  // TRANSFORMATION MATRIX


  public void push() {
    if (matrixStackDepth+1 == MATRIX_STACK_DEPTH) {
      message(COMPLAINT, "matrix stack overflow, to much pushmatrix");
      return;
    }
    float mat[] = matrixStack[matrixStackDepth];
    mat[ 0] = m00; mat[ 1] = m01; mat[ 2] = m02; mat[ 3] = m03;
    mat[ 4] = m10; mat[ 5] = m11; mat[ 6] = m12; mat[ 7] = m13;
    mat[ 8] = m20; mat[ 9] = m21; mat[10] = m22; mat[11] = m23;
    mat[12] = m30; mat[13] = m31; mat[14] = m32; mat[15] = m33;
    matrixStackDepth++;
  }


  public void pop() {
    if (matrixStackDepth == 0) {
      message(COMPLAINT, "matrix stack underflow, to many popmatrix");
      return;
    }
    matrixStackDepth--;
    float mat[] = matrixStack[matrixStackDepth];
    m00 = mat[ 0]; m01 = mat[ 1]; m02 = mat[ 2]; m03 = mat[ 3];
    m10 = mat[ 4]; m11 = mat[ 5]; m12 = mat[ 6]; m13 = mat[ 7];
    m20 = mat[ 8]; m21 = mat[ 9]; m22 = mat[10]; m23 = mat[11];
    m30 = mat[12]; m31 = mat[13]; m32 = mat[14]; m33 = mat[15];
  }


  /**
   * Load identity as the transform/model matrix.
   * Same as glLoadIdentity().
   */
  public void resetMatrix() {
    m00 = 1; m01 = 0; m02 = 0; m03 = 0;
    m10 = 0; m11 = 1; m12 = 0; m13 = 0;
    m20 = 0; m21 = 0; m22 = 1; m23 = 0;
    m30 = 0; m31 = 0; m32 = 0; m33 = 1;
  }


  /**
   * Apply a 4x4 transformation matrix. Same as glMultMatrix().
   */
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {

    float r00 = m00*n00 + m01*n10 + m02*n20 + m03*n30;
    float r01 = m00*n01 + m01*n11 + m02*n21 + m03*n31;
    float r02 = m00*n02 + m01*n12 + m02*n22 + m03*n32;
    float r03 = m00*n03 + m01*n13 + m02*n23 + m03*n33;

    float r10 = m10*n00 + m11*n10 + m12*n20 + m13*n30;
    float r11 = m10*n01 + m11*n11 + m12*n21 + m13*n31;
    float r12 = m10*n02 + m11*n12 + m12*n22 + m13*n32;
    float r13 = m10*n03 + m11*n13 + m12*n23 + m13*n33;

    float r20 = m20*n00 + m21*n10 + m22*n20 + m23*n30;
    float r21 = m20*n01 + m21*n11 + m22*n21 + m23*n31;
    float r22 = m20*n02 + m21*n12 + m22*n22 + m23*n32;
    float r23 = m20*n03 + m21*n13 + m22*n23 + m23*n33;

    float r30 = m30*n00 + m31*n10 + m32*n20 + m33*n30;
    float r31 = m30*n01 + m31*n11 + m32*n21 + m33*n31;
    float r32 = m30*n02 + m31*n12 + m32*n22 + m33*n32;
    float r33 = m30*n03 + m31*n13 + m32*n23 + m33*n33;

    m00 = r00; m01 = r01; m02 = r02; m03 = r03;
    m10 = r10; m11 = r11; m12 = r12; m13 = r13;
    m20 = r20; m21 = r21; m22 = r22; m23 = r23;
    m30 = r30; m31 = r31; m32 = r32; m33 = r33;
  }



  /**
   * Print the current model (or "transformation") matrix.
   */
  public void printMatrix() {
    int big = (int) Math.abs(max(max(max(max(abs(m00), abs(m01)),
                                         max(abs(m02), abs(m03))),
                                     max(max(abs(m10), abs(m11)),
                                         max(abs(m12), abs(m13)))),
                                 max(max(max(abs(m20), abs(m21)),
                                         max(abs(m22), abs(m23))),
                                     max(max(abs(m30), abs(m31)),
                                         max(abs(m32), abs(m33))))));
    int d = 1;
    while ((big /= 10) != 0) d++;  // cheap log()

    System.out.println(PApplet.nfs(m00, d, 4) + " " +
                       PApplet.nfs(m01, d, 4) + " " +
                       PApplet.nfs(m02, d, 4) + " " +
                       PApplet.nfs(m03, d, 4));

    System.out.println(PApplet.nfs(m10, d, 4) + " " +
                       PApplet.nfs(m11, d, 4) + " " +
                       PApplet.nfs(m12, d, 4) + " " +
                       PApplet.nfs(m13, d, 4));

    System.out.println(PApplet.nfs(m20, d, 4) + " " +
                       PApplet.nfs(m21, d, 4) + " " +
                       PApplet.nfs(m22, d, 4) + " " +
                       PApplet.nfs(m23, d, 4));

    System.out.println(PApplet.nfs(m30, d, 4) + " " +
                       PApplet.nfs(m31, d, 4) + " " +
                       PApplet.nfs(m32, d, 4) + " " +
                       PApplet.nfs(m33, d, 4));

    System.out.println();
  }



  //////////////////////////////////////////////////////////////

  // CAMERA


  /**
   * Calling cameraMode(PERSPECTIVE) will setup the standard
   * Processing transformation.
   *
   * cameraMode(ORTHOGRAPHIC) will setup a straight orthographic
   * projection.
   *
   * Note that this setting gets nuked if resize() is called.
   */
  public void cameraMode(int mode) {
    if (mode == PERSPECTIVE) {
      //System.out.println("setting camera to perspective");
      //System.out.println("  " + cameraFOV + " " + cameraAspect);
      beginCamera();
      resetMatrix();
      perspective(cameraFOV, cameraAspect, cameraNear, cameraFar);
      lookat(cameraX, cameraY, cameraZ,
             cameraX, cameraY, 0,
             0, 1, 0);
      endCamera();

    } else if (mode == ORTHOGRAPHIC) {
      beginCamera();
      resetMatrix();
      ortho(0, width, 0, height, -10, 10);
      endCamera();
    }

    cameraMode = mode;  // this doesn't do much
  }


  /**
   * Set matrix mode to the camera matrix (instead of
   * the current transformation matrix). This means applyMatrix,
   * resetMatrix, etc. will affect the camera.
   *
   * You'll need to call resetMatrix() if you want to
   * completely change the camera's settings.
   */
  public void beginCamera() {
    // this will be written over by cameraMode() if necessary
    cameraMode = CUSTOM;
  }


  /**
   * Record the current settings into the camera matrix.
   * And set the matrix mode back to the current
   * transformation matrix.
   *
   * Note that this will destroy any settings to scale(),
   * translate() to your scene.
   */
  public void endCamera() {
    p00 = m00; p01 = m01; p02 = m02; p03 = m03;
    p10 = m10; p11 = m11; p12 = m12; p13 = m13;
    p20 = m20; p21 = m21; p22 = m22; p23 = m23;
    p30 = m30; p31 = m31; p32 = m32; p33 = m33;
    resetMatrix();
  }

  /**
   * Same as gluOrtho(). Implementation based on Mesa's matrix.c
   */
  public void ortho(float left, float right,
                    float bottom, float top,
                    float near, float far) {
    float x =  2.0f / (right - left);
    float y =  2.0f / (top - bottom);
    float z = -2.0f / (far - near);

    float tx = -(right + left) / (right - left);
    float ty = -(top + bottom) / (top - bottom);
    float tz = -(far + near) / (far - near);

    applyMatrix(x, 0, 0, tx,
                0, y, 0, ty,
                0, 0, z, tz,
                0, 0, 0, 1);
  }


  /**
   * Same as gluPerspective(). Implementation based on Mesa's glu.c
   */
  public void perspective(float fovy, float aspect, float zNear, float zFar) {
    //System.out.println("perspective: " + fovy + " " + aspect + " " +
    //               zNear + " " + zFar);
    float ymax = zNear * tan(fovy * PI / 360.0f);
    float ymin = -ymax;

    float xmin = ymin * aspect;
    float xmax = ymax * aspect;

    frustum(xmin, xmax, ymin, ymax, zNear, zFar);
  }


  /**
   * Same as glFrustum(). Implementation based on the explanation
   * in the OpenGL reference book.
   */
  public void frustum(float left, float right, float bottom,
                      float top, float znear, float zfar) {
    //System.out.println("frustum: " + left + " " + right + "  " +
    //               bottom + " " + top + "  " + znear + " " + zfar);
    applyMatrix((2*znear)/(right-left), 0, (right+left)/(right-left), 0,
                0, (2*znear)/(top-bottom), (top+bottom)/(top-bottom), 0,
                0, 0, -(zfar+znear)/(zfar-znear),-(2*zfar*znear)/(zfar-znear),
                0, 0, -1, 0);
  }


  /**
   * Same as gluLookat(). Implementation based on Mesa's glu.c
   */
  public void lookat(float eyeX, float eyeY, float eyeZ,
                     float centerX, float centerY, float centerZ,
                     float upX, float upY, float upZ) {
    float z0 = eyeX - centerX;
    float z1 = eyeY - centerY;
    float z2 = eyeZ - centerZ;
    float mag = sqrt(z0*z0 + z1*z1 + z2*z2);

    if (mag != 0) {
      z0 /= mag;
      z1 /= mag;
      z2 /= mag;
    }

    float y0 = upX;
    float y1 = upY;
    float y2 = upZ;

    float x0 =  y1*z2 - y2*z1;
    float x1 = -y0*z2 + y2*z0;
    float x2 =  y0*z1 - y1*z0;

    y0 =  z1*x2 - z2*x1;
    y1 = -z0*x2 + z2*x0;
    y2 =  z0*x1 - z1*x0;

    mag = sqrt(x0*x0 + x1*x1 + x2*x2);
    if (mag != 0) {
      x0 /= mag;
      x1 /= mag;
      x2 /= mag;
    }

    mag = sqrt(y0*y0 + y1*y1 + y2*y2);
    if (mag != 0) {
      y0 /= mag;
      y1 /= mag;
      y2 /= mag;
    }

    applyMatrix(x0, x1, x2, 0,
                y0, y1, y2, 0,
                z0, z1, z2, 0,
                0,  0,  0,  1);
    translate(-eyeX, -eyeY, -eyeZ);
  }



  /**
   * Print the current camera (or "perspective") matrix.
   */
  public void printCamera() {
    int big = (int) Math.abs(max(max(max(max(abs(p00), abs(p01)),
                                         max(abs(p02), abs(p03))),
                                     max(max(abs(p10), abs(p11)),
                                         max(abs(p12), abs(p13)))),
                                 max(max(max(abs(p20), abs(p21)),
                                         max(abs(p22), abs(p23))),
                                     max(max(abs(p30), abs(p31)),
                                         max(abs(p32), abs(p33))))));
    int d = 1;
    while ((big /= 10) != 0) d++;  // cheap log()

    System.out.println(PApplet.nfs(p00, d, 4) + " " +
                       PApplet.nfs(p01, d, 4) + " " +
                       PApplet.nfs(p02, d, 4) + " " +
                       PApplet.nfs(p03, d, 4));

    System.out.println(PApplet.nfs(p10, d, 4) + " " +
                       PApplet.nfs(p11, d, 4) + " " +
                       PApplet.nfs(p12, d, 4) + " " +
                       PApplet.nfs(p13, d, 4));

    System.out.println(PApplet.nfs(p20, d, 4) + " " +
                       PApplet.nfs(p21, d, 4) + " " +
                       PApplet.nfs(p22, d, 4) + " " +
                       PApplet.nfs(p23, d, 4));

    System.out.println(PApplet.nfs(p30, d, 4) + " " +
                       PApplet.nfs(p31, d, 4) + " " +
                       PApplet.nfs(p32, d, 4) + " " +
                       PApplet.nfs(p33, d, 4));

    System.out.println();
  }


  public float screenX(float x, float y) {
    return screenX(x, y, 0);
  }


  public float screenY(float x, float y) {
    return screenY(x, y, 0);
  }


  public float screenX(float x, float y, float z) {
    float ax = m00*x + m01*y + m02*z + m03;
    float ay = m10*x + m11*y + m12*z + m13;
    float az = m20*x + m21*y + m22*z + m23;
    float aw = m30*x + m31*y + m32*z + m33;

    float ox = p00*ax + p01*ay + p02*az + p03*aw;
    float ow = p30*ax + p31*ay + p32*az + p33*aw;

    if (ow != 0) ox /= ow;
    return width * (1 + ox) / 2.0f;
  }


  public float screenY(float x, float y, float z) {
    float ax = m00*x + m01*y + m02*z + m03;
    float ay = m10*x + m11*y + m12*z + m13;
    float az = m20*x + m21*y + m22*z + m23;
    float aw = m30*x + m31*y + m32*z + m33;

    float oy = p10*ax + p11*ay + p12*az + p13*aw;
    float ow = p30*ax + p31*ay + p32*az + p33*aw;

    if (ow != 0) oy /= ow;
    return height * (1 + oy) / 2.0f;
  }


  public float screenZ(float x, float y, float z) {
    float ax = m00*x + m01*y + m02*z + m03;
    float ay = m10*x + m11*y + m12*z + m13;
    float az = m20*x + m21*y + m22*z + m23;
    float aw = m30*x + m31*y + m32*z + m33;

    float oz = p20*ax + p21*ay + p22*az + p23*aw;
    float ow = p30*ax + p31*ay + p32*az + p33*aw;

    if (ow != 0) oz /= ow;
    return (oz + 1) / 2.0f;
  }


  public float objectX(float x, float y, float z) {
    float ax = m00*x + m01*y + m02*z + m03;
    float aw = m30*x + m31*y + m32*z + m33;
    return (aw != 0) ? ax / aw : ax;
  }


  public float objectY(float x, float y, float z) {
    float ay = m10*x + m11*y + m12*z + m13;
    float aw = m30*x + m31*y + m32*z + m33;
    return (aw != 0) ? ay / aw : ay;
  }


  public float objectZ(float x, float y, float z) {
    float az = m20*x + m21*y + m22*z + m23;
    float aw = m30*x + m31*y + m32*z + m33;
    return (aw != 0) ? az / aw : az;
  }



  //////////////////////////////////////////////////////////////

  // BACKGROUND


  /**
   * Takes an RGB or RGBA image and sets it as the background.
   *
   * Note that even if the image is set as RGB, the high 8 bits of
   * each pixel must be set (0xFF000000), because the image data will
   * be copied directly to the screen.
   *
   * Also clears out the zbuffer and stencil buffer if they exist.
   */
  public void background(PImage image) {
    super.background(image);

    for (int i = 0; i < pixelCount; i++) {
      zbuffer[i] = MAX_FLOAT;
      stencil[i] = 0;
    }
  }


  /**
   * Clears pixel buffer. Also clears the stencil and zbuffer
   * if they exist. Their existence is more accurate than using 'depth'
   * to test whether to clear them, because if they're non-null,
   * it means that depth() has been called somewhere in the program,
   * even if noDepth() was called before draw() exited.
   */
  public void clear() {
    for (int i = 0; i < pixelCount; i++) {
      pixels[i] = backgroundColor;
      zbuffer[i] = MAX_FLOAT;
      stencil[i] = 0;
    }
  }



  //////////////////////////////////////////////////////////////

  // LIGHTS


  public void lights() {
    lights = true;
  }

  public void noLights() {
    lights = false;
  }


  /**
   * Simpler macro for setting up a diffuse light at a position.
   * Turns on a diffuse light with the color passed in,
   * and sets that light's ambient and specular components to zero.
   *
   * (The variables are named red, green, blue instead of r, g, b
   * because otherwise the compiler gets stuck on g.light() inside
   * the auto-generated section of PApplet)
   */
  public void light(int num, float x, float y, float z,
                    float red, float green, float blue) {
    lightPosition(num, x, y, z);
    lightAmbient(num, 0, 0, 0);
    lightDiffuse(num, red, green, blue);
    lightSpecular(num, 0, 0, 0);
    lightEnable(num);
  }


  public void lightEnable(int num) {
    light[num] = true;
  }

  public void lightDisable(int num) {
    light[num] = false;
  }


  public void lightPosition(int num, float x, float y, float z) {
    lightX[num] = x;
    lightY[num] = y;
    lightZ[num] = z;
  }

  public void lightAmbient(int num, float x, float y, float z) {
    calc_color(x, y, z);
    lightAmbientR[num] = calcR;
    lightAmbientG[num] = calcG;
    lightAmbientB[num] = calcB;
  }

  public void lightDiffuse(int num, float x, float y, float z) {
    calc_color(x, y, z);
    lightDiffuseR[num] = calcR;
    lightDiffuseG[num] = calcG;
    lightDiffuseB[num] = calcB;
  }

  public void lightSpecular(int num, float x, float y, float z) {
    calc_color(x, y, z);
    lightSpecularR[num] = calcR;
    lightSpecularG[num] = calcG;
    lightSpecularB[num] = calcB;
  }



  //////////////////////////////////////////////////////////////

  // MATH


  private final float mag(float a, float b) {
    return (float)Math.sqrt(a*a + b*b);
  }

  private final float mag(float a, float b, float c) {
    return (float)Math.sqrt(a*a + b*b + c*c);
  }

  private final float max(float a, float b) {
    return (a > b) ? a : b;
  }

  private final float max(float a, float b, float c) {
    return Math.max(a, Math.max(b, c));
  }

  private final float sq(float a) {
    return a*a;
  }

  private final float sqrt(float a) {
    return (float)Math.sqrt(a);
  }

  private final float abs(float a) {
    return (a < 0) ? -a : a;
  }

  private final float sin(float angle) {
    if (angleMode == DEGREES) angle *= DEG_TO_RAD;
    return (float)Math.sin(angle);
  }

  private final float cos(float angle) {
    if (angleMode == DEGREES) angle *= DEG_TO_RAD;
    return (float)Math.cos(angle);
  }

  private final float tan(float angle) {
    if (angleMode == DEGREES) angle *= DEG_TO_RAD;
    return (float)Math.tan(angle);
  }
}

