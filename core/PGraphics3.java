/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PGraphics - main graphics and rendering context
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-05 Ben Fry and Casey Reas
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

import java.awt.*;
import java.awt.image.*;


public class PGraphics3 extends PGraphics {

  // modelview matrix
  public PMatrix modelview = new PMatrix(RADIANS, MATRIX_STACK_DEPTH);

  // ........................................................
  // Lighting-related variables

  // inverse model matrix
  public PMatrix inverseModelview = new PMatrix(RADIANS, MATRIX_STACK_DEPTH);

  // store the facing direction to speed rendering
  boolean useBackfaceCulling = false;

  // Material properties

  public float ambientR, ambientG, ambientB;
  public int ambientRi, ambientGi, ambientBi;

  public float specularR, specularG, specularB, specularA;
  public int specularRi, specularGi, specularBi, specularAi;

  public float emissiveR, emissiveG, emissiveB;
  public int emissiveRi, emissiveGi, emissiveBi;

  public float shininess;

  // ........................................................

  public int cameraMode;

  // perspective setup
  public float cameraFOV;
  public float cameraX, cameraY, cameraZ;
  public float cameraNear, cameraFar;
  public float cameraAspect;

  // This is turned on at beginCamera, and off at endCamera
  // Currently we don't support nested begin/end cameras.
  // If we wanted to, this variable would have to become a stack.
  public boolean manipulatingCamera;

  // projection matrix
  public PMatrix projection = new PMatrix(RADIANS);

  // These two matrices always point to either the modelview
  // or the inverseModelview, but they are swapped during
  // when in camera maniuplation mode. That way camera transforms
  // are automatically accumulated in inverse on the modelview matrix.
  public PMatrix forwardTransform;
  public PMatrix reverseTransform;

  // ........................................................

  /// the stencil buffer (only for NEW_GRAPHICS)
  public int stencil[];

  /// zbuffer (only when 3D is in use)
  public float zbuffer[];

  // ........................................................

  /** Maximum lights by default is 8, which is arbitrary,
      but is the minimum defined by OpenGL */
  protected static final int MAX_LIGHTS = 8;

  int lightCount = 0;

  /** True if lights are enabled */
  public boolean lights;

  /** True if this light is enabled */
  public boolean light[];

  /** Light types */
  public int lightType[];

  /** Light positions */
  public float lightX[], lightY[], lightZ[];

  /** Light direction */
  public float lightNX[], lightNY[], lightNZ[];

  /** Light falloff */
  public float lightConstantFalloff[], lightLinearFalloff[], lightQuadraticFalloff[];

  /** Light spot angle */
  public float lightSpotAngle[];

  /** Cosine of light spot angle */
  public float lightCosSpotAngle[];

  /** Light spot concentration */
  public float lightSpotConcentration[];

  /** Diffuse colors for lights.
   *  For an ambient light, this will hold the ambient color.
   *  Internally these are stored as numbers between 0 and 1. */
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
   * IMAGE or NORMALIZED, though this should probably
   * be called textureSpace().
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
  public int normalMode;
  public int normalCount;

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
  public PGraphics3() {
    forwardTransform = modelview;
    reverseTransform = inverseModelview;
  }


  /**
   * Constructor for the PGraphics3 object. Use this to ensure that
   * the defaults get set properly. In a subclass, use this(w, h)
   * as the first line of a subclass' constructor to properly set
   * the internal fields and defaults.
   *
   * @param iwidth  viewport width
   * @param iheight viewport height
   */
  public PGraphics3(int iwidth, int iheight, PApplet parent) {
    // super will add the listeners to the applet, and call resize()
    super(iwidth, iheight, parent);
    forwardTransform = modelview;
    reverseTransform = inverseModelview;
    //resize(iwidth, iheight);
  }


  /**
   * Called in repsonse to a resize event, handles setting the
   * new width and height internally, as well as re-allocating
   * the pixel buffer for the new size.
   *
   * Note that this will nuke any cameraMode() settings.
   */
  public void resize(int iwidth, int iheight) {  // ignore
    //System.out.println("PGraphics3 resize");

    width = iwidth;
    height = iheight;
    width1 = width - 1;
    height1 = height - 1;

    allocate();

    // clear the screen with the old background color
    //background(backgroundColor);

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
    lightDiffuseR = new float[MAX_LIGHTS];
    lightDiffuseG = new float[MAX_LIGHTS];
    lightDiffuseB = new float[MAX_LIGHTS];
    lightSpecularR = new float[MAX_LIGHTS];
    lightSpecularG = new float[MAX_LIGHTS];
    lightSpecularB = new float[MAX_LIGHTS];
    lightType      = new int[MAX_LIGHTS];
    lightNX        = new float[MAX_LIGHTS];
    lightNY        = new float[MAX_LIGHTS];
    lightNZ        = new float[MAX_LIGHTS];
    lightConstantFalloff = new float[MAX_LIGHTS];
    lightLinearFalloff   = new float[MAX_LIGHTS];
    lightQuadraticFalloff = new float[MAX_LIGHTS];
    lightSpotAngle        = new float[MAX_LIGHTS];
    lightCosSpotAngle        = new float[MAX_LIGHTS];
    lightSpotConcentration = new float[MAX_LIGHTS];

    // reset the cameraMode if PERSPECTIVE or ORTHOGRAPHIC
    // will just be ignored if CUSTOM, the user's hosed anyways
    //System.out.println("setting cameraMode to " + cameraMode);
    if (this.cameraMode != CUSTOM) cameraMode(this.cameraMode);
  }


  protected void allocate() {
    pixelCount = width * height;
    pixels = new int[pixelCount];

    // because of a java 1.1 bug, pixels must be registered as
    // opaque before their first run, the memimgsrc will flicker
    // and run very slowly.
    backgroundColor |= 0xff000000;  // just for good measure
    for (int i = 0; i < pixelCount; i++) pixels[i] = backgroundColor;
    //for (int i = 0; i < pixelCount; i++) pixels[i] = 0xffffffff;

    cm = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff);;
    mis = new MemoryImageSource(width, height, pixels, 0, width);
    mis.setFullBufferUpdates(true);
    mis.setAnimated(true);
    image = Toolkit.getDefaultToolkit().createImage(mis);

    zbuffer = new float[pixelCount];
    stencil = new int[pixelCount];

    line = new PLine(this);
    triangle = new PTriangle(this);
  }

  protected void resetLights() {
    //  reset lights
    // This looks like a funny reset, but it's to make the most common
    // case the easiest to attain even if the user chooses to do everything
    // by hand.
    lightCount = 0;
    light[0] = false;
    lightType[0] = AMBIENT;
    for (int i = 1; i < MAX_LIGHTS; i++) {
      light[i] = false;
      lightType[i] = POINT;
    }
  }

  public void beginFrame() {
    super.beginFrame();

    resetLights();

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

  public void angleMode(int mode) {
    super.angleMode(mode);
    modelview.angleMode(mode);
    inverseModelview.angleMode(mode);
    projection.angleMode(mode);
  }

  public void defaults() {
    super.defaults();

    manipulatingCamera = false;
    forwardTransform = modelview;
    reverseTransform = inverseModelview;

    cameraMode(PERSPECTIVE);

    //System.out.println("PGraphics3.defaults()");
    // easiest for beginners
    textureMode(IMAGE);

    // better to leave this turned off by default
    noLights();

    //lightEnable(0);
    //lightAmbient(0, 0, 0, 0);

    //light(1, cameraX, cameraY, cameraZ, 255, 255, 255);

    emissive(0.0f);
    specular(0.5f);
    shininess(1.0f);

  }

  /**
   * do anything that needs doing after setup before draw
   */
  public void postSetup() {
    modelview.storeResetValue();
    inverseModelview.storeResetValue();
  }

  //////////////////////////////////////////////////////////////


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
    normalMode = AUTO_NORMAL;
    normalCount = 0;
  }


  /**
   * Sets the current normal. Only applies
   * inside a beginShape/endShape block.
   */
  public void normal(float nx, float ny, float nz) {
    normalX = nx;
    normalY = ny;
    normalZ = nz;

    // if drawing a shape and the normal hasn't been set yet,
    // then we need to set the normals for each vertex so far
    if (shape != 0) {
      if (normalCount == 0) {
        for (int i = vertex_start; i < vertexCount; i++) {
        vertices[i][NX] = normalX;
        vertices[i][NY] = normalY;
        vertices[i][NZ] = normalZ;
      }
      }

      normalCount++;
      if (normalCount == 1) {
        // One normal per begin/end shape
        normalMode = MANUAL_SHAPE_NORMAL;
      }
      else {
        // a separate normal for each vertex
        normalMode = MANUAL_VERTEX_NORMAL;
      }
    }
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
      //message(CHATTER, "allocating more textures " + textures.length);
    }

    if (textures[0] != null) {  // wHY?
      texture_index++;
    }

    textures[texture_index] = image;
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
      //message(CHATTER, "allocating more vertices " + vertices.length);
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

      vertex[AR] = ambientR;
      vertex[AG] = ambientG;
      vertex[AB] = ambientB;

      vertex[SPR] = specularR;
      vertex[SPG] = specularG;
      vertex[SPB] = specularB;
      vertex[SPA] = specularA;

      vertex[SHINE] = shininess;

      vertex[ER] = emissiveR;
      vertex[EG] = emissiveG;
      vertex[EB] = emissiveB;
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

    vertex[NX] = normalX;
    vertex[NY] = normalY;
    vertex[NZ] = normalZ;
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
      throw new RuntimeException("need to set an image with texture() " +
                                 "before using u and v coordinates");
      //message(PROBLEM, "gotta use texture() " +
      //      "after beginShape() and before vertex()");
      //return;
    }
    if (textureMode == IMAGE) {
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
          for (int i = vertex_start; i < vertex_end-2; i += 3) {
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
          for (int i = vertex_start; i < vertex_end - 3; i += 2) {
            add_path();
            add_line(i+0, i+2);
            add_line(i+2, i+3);
            add_line(i+3, i+1);
            add_line(i+1, i+0);
          }
          /*
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
          */
        }
        break;

        case POLYGON:
          //case CONCAVE_POLYGON:
          //case CONVEX_POLYGON:
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
        case TRIANGLE_FAN:
        {
          stop = vertex_end - 1;
          for (int i = vertex_start + 1; i < stop; i++) {
            add_triangle(vertex_start, i, i+1);
          }
        }
        break;

        case TRIANGLES:
        case TRIANGLE_STRIP:
        {
          stop = vertex_end - 2;
          increment = (shape == TRIANGLES) ? 3 : 1;
          for (int i = vertex_start; i < stop; i += increment) {
            // have to switch between clockwise/counter-clockwise
            // otherwise the feller is backwards and renderer won't draw
            if ((i % 2) == 0) {
              add_triangle(i, i+2, i+1);
            } else {
              add_triangle(i, i+1, i+2);
            }
          }
        }
        break;

        case QUADS:
        {
          stop = vertexCount-3;
          for (int i = vertex_start; i < stop; i += 4) {
            // first triangle
            add_triangle(i, i+1, i+2);
            // second triangle
            add_triangle(i, i+2, i+3);
          }
        }
        break;

        case QUAD_STRIP:
        {
          stop = vertexCount-3;
          for (int i = vertex_start; i < stop; i += 2) {
            // first triangle
            add_triangle(i+0, i+2, i+1);
            // second triangle
            add_triangle(i+2, i+3, i+1);
          }
        }
        break;

        case POLYGON:
          //case CONCAVE_POLYGON:
          //case CONVEX_POLYGON:
        {
          triangulate_polygon();
        }
        break;
      }
    }


    // ------------------------------------------------------------------
    // 2D or 3D POINTS FROM MODEL (MX, MY, MZ) TO CAMERA SPACE (VX, VY, VZ)

    for (int i = vertex_start; i < vertex_end; i++) {
      float vertex[] = vertices[i];

      vertex[VX] = modelview.m00*vertex[MX] + modelview.m01*vertex[MY] + modelview.m02*vertex[MZ] + modelview.m03;
      vertex[VY] = modelview.m10*vertex[MX] + modelview.m11*vertex[MY] + modelview.m12*vertex[MZ] + modelview.m13;
      vertex[VZ] = modelview.m20*vertex[MX] + modelview.m21*vertex[MY] + modelview.m22*vertex[MZ] + modelview.m23;
      vertex[VW] = modelview.m30*vertex[MX] + modelview.m31*vertex[MY] + modelview.m32*vertex[MZ] + modelview.m33;
    }


    // ------------------------------------------------------------------
    // TRANSFORM / LIGHT / CLIP

    handle_lighting();



    // ------------------------------------------------------------------
    // POINTS FROM CAMERA SPACE (VX, VY, VZ) TO SCREEN SPACE (X, Y, Z)

    for (int i = vertex_start; i < vertex_end; i++) {
      float vx[] = vertices[i];

      float ox = projection.m00*vx[VX] + projection.m01*vx[VY] + projection.m02*vx[VZ] + projection.m03*vx[VW];
      float oy = projection.m10*vx[VX] + projection.m11*vx[VY] + projection.m12*vx[VZ] + projection.m13*vx[VW];
      float oz = projection.m20*vx[VX] + projection.m21*vx[VY] + projection.m22*vx[VZ] + projection.m23*vx[VW];
      float ow = projection.m30*vx[VX] + projection.m31*vx[VY] + projection.m32*vx[VZ] + projection.m33*vx[VW];

      if (ow != 0) {
        ox /= ow; oy /= ow; oz /= ow;
      }

      vx[X] = width * (ONE + ox) / 2.0f;
      vx[Y] = height * (ONE + oy) / 2.0f;
      vx[Z] = (oz + ONE) / 2.0f;
    }


    // ------------------------------------------------------------------
    // RENDER SHAPES FILLS HERE WHEN NOT DEPTH SORTING

    // if true, the shapes will be rendered on endFrame
    if (!hints[DEPTH_SORT]) {
      if (fill) render_triangles();
      if (stroke) render_lines();
    }

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
      //message(CHATTER, "allocating more lines " + lines.length);
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
    //System.out.println("adding triangle " + triangleCount);
    if (triangleCount == triangles.length) {
      int temp[][] = new int[triangleCount<<1][TRIANGLE_FIELD_COUNT];
      System.arraycopy(triangles, 0, temp, 0, triangleCount);
      triangles = temp;
      //message(CHATTER, "allocating more triangles " + triangles.length);
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
  //public void render_triangles() {
    //System.out.println("PGraphics3 render triangles");
    //System.out.println("rendering " + triangleCount + " triangles");

    for (int i = 0; i < triangleCount; i ++) {
      float a[] = vertices[triangles[i][VERTEX1]];
      float b[] = vertices[triangles[i][VERTEX2]];
      float c[] = vertices[triangles[i][VERTEX3]];
      int tex = triangles[i][TEXTURE_INDEX];
      int index = triangles[i][INDEX];

      triangle.reset();

      // This is only true when not textured. We really should pass SPECULAR
      // straight through to triangle rendering.
      float ar = min(1, a[R] + a[SPR]);
      float ag = min(1, a[G] + a[SPG]);
      float ab = min(1, a[B] + a[SPB]);
      float br = min(1, b[R] + b[SPR]);
      float bg = min(1, b[G] + b[SPG]);
      float bb = min(1, b[B] + b[SPB]);
      float cr = min(1, c[R] + c[SPR]);
      float cg = min(1, c[G] + c[SPG]);
      float cb = min(1, c[B] + c[SPB]);

      if (tex > -1 && textures[tex] != null) {
        triangle.setTexture(textures[tex]);
        triangle.setUV(a[U], a[V], b[U], b[V], c[U], c[V]);
      }

      triangle.setIntensities(ar, ag, ab, a[A],
                              br, bg, bb, b[A],
                              cr, cg, cb, c[A]);

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
    /*
    System.out.println("triangulating polygon " +
                       vertex_start + " " + vertex_end);
    for (int i = vertex_start; i < vertex_end; i++) {
      System.out.println(i + " " + vertices[i][X] + " " + vertices[i][Y]);
    }
    */

    // first we check if the polygon goes clockwise or counterclockwise
    float area = 0.0f;
    for (int p = vertex_end - 1, q = vertex_start; q < vertex_end; p = q++) {
      area += (vertices[q][MX] * vertices[p][MY] -
               vertices[p][MX] * vertices[q][MY]);
      //area += (vertices[q][X] * vertices[p][Y] -
      //       vertices[p][X] * vertices[q][Y]);
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

      float Ax = -vertices[vertex_order[u]][MX];
      float Ay =  vertices[vertex_order[u]][MY];
      float Bx = -vertices[vertex_order[v]][MX];
      float By =  vertices[vertex_order[v]][MY];
      float Cx = -vertices[vertex_order[w]][MX];
      float Cy =  vertices[vertex_order[w]][MY];

      /*
      float Ax = -vertices[vertex_order[u]][X];
      float Ay =  vertices[vertex_order[u]][Y];
      float Bx = -vertices[vertex_order[v]][X];
      float By =  vertices[vertex_order[v]][Y];
      float Cx = -vertices[vertex_order[w]][X];
      float Cy =  vertices[vertex_order[w]][Y];
      */

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

        //float Px = -vertices[vertex_order[p]][X];
        //float Py =  vertices[vertex_order[p]][Y];
        float Px = -vertices[vertex_order[p]][MX];
        float Py =  vertices[vertex_order[p]][MY];

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


  /**
   * This method handles the transformation, lighting, and clipping
   * operations for the shapes. Broken out as a separate function
   * so that other renderers can override. For instance, with OpenGL,
   * this section is all handled on the graphics card.
   */
  protected void handle_lighting() {

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

    // TODO: You only need to do any of this when you've got lighting and fill is on

    // TODO: You only need to calculate these repeatedly when you've got VERTEX or AUTO normals
    for (int i = vertex_start; i < vertex_end; i++) {
      float v[] = vertices[i];
      //    Multiply by TRANSPOSE!
      // It's just one of those things. Model normals should be multiplied by the
      // inverse transpose of the modelview matrix to get world normals.
      float nx = inverseModelview.m00*v[NX] + inverseModelview.m10*v[NY] + inverseModelview.m20*v[NZ] + inverseModelview.m30;
      float ny = inverseModelview.m01*v[NX] + inverseModelview.m11*v[NY] + inverseModelview.m21*v[NZ] + inverseModelview.m31;
      float nz = inverseModelview.m02*v[NX] + inverseModelview.m12*v[NY] + inverseModelview.m22*v[NZ] + inverseModelview.m32;
      float nw = inverseModelview.m03*v[NX] + inverseModelview.m13*v[NY] + inverseModelview.m23*v[NZ] + inverseModelview.m33;

      v[NX] = nx;
      v[NY] = ny;
      v[NZ] = nz;
      if (nw != 0) {
        // divide by perspective coordinate
        v[NX] /= nw; v[NY] /= nw; v[NZ] /= nw;
      }

      float nlen = mag(v[NX], v[NY], v[NZ]);  // normalize
      if (nlen != 0 && nlen != ONE) {
        v[NX] /= nlen; v[NY] /= nlen; v[NZ] /= nlen;
      }
    }


    // ------------------------------------------------------------------
    // LIGHTS

    // if no lights enabled, then all the values for r, g, b
    // have been set with calls to vertex() (no need to re-calculate here)

    if (lights) {
      // The assumption here is that we are only using vertex normals
      // I think face normals may be necessary to offer also. We'll see.
      //float f[] = vertices[vertex_start];

      for (int i = vertex_start; i < vertex_end; i++) {
        float v[] = vertices[i];
        float vx = v[VX];
        float vy = v[VY];
        float vz = v[VZ];
        float vw = v[VW];
        if (vw != 0 && vw != 1) {
          vx /= vw;
          vy /= vw;
          vz /= vw;
        }

        if (fill) {
          calc_lighting(v[AR],  v[AG], v[AB], v[R], v[G], v[B], v[SPR],  v[SPG], v[SPB], v[ER],  v[EG], v[EB],
                        vx, vy, vz,
                        v[NX], v[NY], v[NZ], v[SHINE], v, R);
        }
        // We're not lighting strokes now.
        /*if (stroke) {
          calc_lighting(v[AR],  v[AG], v[AB], v[SR], v[SG], v[SB], v[SPR],  v[SPG], v[SPB], v[ER],  v[EG], v[EB],
                        vx, vy, vz,
                        v[NX], v[NY], v[NZ], v[SHINE], v, SR);
        }*/
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
  }

  private float dot(float ax, float ay, float az, float bx, float by, float bz) {
    return ax * bx + ay * by + az * bz;
  }


  /**
   * lighting calculation of final color.
   * Assumptions:
   * camera space == world space
   * All coordinates are in world space, including normals
   * Normals are pre-normalized
   * Lights are in world-space too
   * This vertex has not yet been lit. Value changes happen in place.
   *
   * @param  r        red component of object's colour
   * @param  g        green of object's colour
   * @param  b        blue of object's colour
   * @param  wx       x coord of world point
   * @param  wy       y coord of world point
   * @param  wz       z coord of world point
   * @param  nx       x coord of normal vector
   * @param  ny       y coord of normal vector
   * @param  nz       z coord of normal vector
   * @param  target   float array to store result
   * @param  toffset  starting index in target array
   */
  private void calc_lighting(float ar, float ag, float ab,
                             float dr, float dg, float db,
                             float sr, float sg, float sb,
                             float er, float eg, float eb,
                             float wx, float wy, float wz,
                             float nx, float ny, float nz,
                             float shininess,
                             float target[], int toffset) {
    //System.out.println("calc_lighting normals " + nx + " " + ny + " " + nz);

    if (!lights) {
      target[toffset + 0] = min(1.0f, er+dr);
      target[toffset + 1] = min(1.0f, eg+dg);
      target[toffset + 2] = min(1.0f, eb+db);
      return;
    }

    // Must pre-normalize normals
    //float nlen = mag(nx, ny, nz);
    //if (nlen != 0) {
    //  nx /= nlen; ny /= nlen; nz /= nlen;
    //}


    // Since the camera space == world space,
    // we can test for visibility by the dot product of
    // the normal with the direction from pt. to eye.
    float dir = dot(nx, ny, nz, -wx, -wy, -wz);
    // If normal is away from camera, choose its opposite.
    // If we add backface culling this, will be backfacing
    // (but since this is per vertex, it's more complicated)
    if (dir < 0) {
      nx = -nx;
      ny = -ny;
      nz = -nz;
    }

    // These two terms will sum the contributions from the various lights
    float diffuse_r = 0;
    float diffuse_g = 0;
    float diffuse_b = 0;

    float specular_r = 0;
    float specular_g = 0;
    float specular_b = 0;

    for (int i = 0; i < MAX_LIGHTS; i++) {
      if (!light[i]) continue;

      float denom = lightConstantFalloff[i];
      float spotTerm = 1;

      if (lightType[i] == AMBIENT) {
        if (lightQuadraticFalloff[i] != 0 || lightLinearFalloff[i] != 0) {
          // Falloff depends on distance
          float distSq = mag(lightX[i] - wx, lightY[i] - wy, lightZ[i] - wz);
          denom += lightQuadraticFalloff[i] * distSq + lightLinearFalloff[i] * (float)sqrt(distSq);
        }
        if (denom == 0) denom = 1;
        diffuse_r += lightDiffuseR[i] * ar / denom;
        diffuse_g += lightDiffuseG[i] * ag / denom;
        diffuse_b += lightDiffuseB[i] * ab / denom;
      }
      else {
        //System.out.println("Light pos: " + lightX[i] + ", " + lightY[i] + ", " + lightZ[i]);

        //li is the vector from the vertex to the light
        float lix, liy, liz;
        float lightDir_dot_li = 0;
        float n_dot_li = 0;

        if (lightType[i] == DIRECTIONAL) {
          lix = -lightNX[i];
          liy = -lightNY[i];
          liz = -lightNZ[i];
          denom = 1;
          n_dot_li = (nx*lix + ny*liy + nz*liz);
          if (n_dot_li <= 0) {
            continue;
          }
        }
        else { // Point or spot light
          lix = lightX[i] - wx;
          liy = lightY[i] - wy;
          liz = lightZ[i] - wz;
          // normalize
          float distSq = mag(lix, liy, liz);
          if (distSq != 0) {
            lix /= distSq; liy /= distSq; liz /= distSq;
          }
          n_dot_li = (nx*lix + ny*liy + nz*liz);
          if (n_dot_li <= 0) {
            continue;
          }

          if (lightType[i] == SPOT) {
            lightDir_dot_li = -(lightNX[i]*lix + lightNY[i]*liy + lightNZ[i]*liz);
            if (lightDir_dot_li <= lightCosSpotAngle[i]) {
              continue;
            }
            spotTerm = pow(lightDir_dot_li, lightSpotConcentration[i]);
          }

          if (lightQuadraticFalloff[i] != 0 || lightLinearFalloff[i] != 0) {
            // Falloff depends on distance
            denom += lightQuadraticFalloff[i] * distSq + lightLinearFalloff[i] * (float)sqrt(distSq);
          }
        }
        // Directional, point, or spot light:

        // We know n_dot_li > 0 from above "continues"
        //if (n_dot_li > 0) {

        if (denom == 0) denom = 1;
        float mul = n_dot_li * spotTerm / denom;
        diffuse_r += lightDiffuseR[i] * mul;
        diffuse_g += lightDiffuseG[i] * mul;
        diffuse_b += lightDiffuseB[i] * mul;

        // SPECULAR

        if ((sr > 0 || sg > 0 || sb > 0) && // If the material and light have a specular component.
            (lightSpecularR[i] > 0 || lightSpecularG[i] > 0 || lightSpecularB[i] > 0) ) {

          float vmag = mag(wx, wy, wz);
          if (vmag != 0) {
            wx /= vmag; wy /= vmag; wz /= vmag;
          }
          float sx = lix - wx;
          float sy = liy - wy;
          float sz = liz - wz;
          vmag = mag(sx, sy, sz);
          if (vmag != 0) {
            sx /= vmag; sy /= vmag; sz /= vmag;
          }
          float s_dot_n = (sx*nx + sy*ny + sz*nz);
          //if (Math.random() < 0.01) System.out.println("s_dot_n: " + s_dot_n);
          if (s_dot_n > 0) {
            s_dot_n = pow(s_dot_n, shininess);
            mul = s_dot_n * spotTerm / denom;
            specular_r += lightSpecularR[i] * mul;
            specular_g += lightSpecularG[i] * mul;
            specular_b += lightSpecularB[i] * mul;
          }

        }
      }
    }

    target[toffset+0] = min(1, er + dr * diffuse_r);
    target[toffset+1] = min(1, eg + dg * diffuse_g);
    target[toffset+2] = min(1, eb + db * diffuse_b);

    target[SPR] = min(1, sr * specular_r);
    target[SPG] = min(1, sg * specular_g);
    target[SPB] = min(1, sb * specular_b);

    return;
  }



  //////////////////////////////////////////////////////////////

  // BASIC SHAPES


  public void point(float x, float y) {
    point(x, y, 0);
  }


  public void point(float x, float y, float z) {
    /*
    beginShape(POINTS);
    vertex(x, y, z);
    endShape();
    */

    // hacked workaround for carlos line bug
    beginShape(LINES);
    vertex(x, y, z);
    vertex(x + EPSILON, y + EPSILON, z);
    endShape();
  }

  /*
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
  */


  public void line(float x1, float y1, float x2, float y2) {
    line(x1, y1, 0, x2, y2, 0);
  }


  public void line(float x1, float y1, float z1,
                   float x2, float y2, float z2) {
    beginShape(LINES);
    vertex(x1, y1, z1);
    vertex(x2, y2, z2);
    endShape();
  }


  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    beginShape(TRIANGLES);
    normal(0, 0, 1);
    vertex(x1, y1);
    vertex(x2, y2);
    vertex(x3, y3);
    endShape();
  }


  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
    beginShape(QUADS);
    normal(0, 0, 1);
    vertex(x1, y1);
    vertex(x2, y2);
    vertex(x3, y3);
    vertex(x4, y4);
    endShape();
  }



  //////////////////////////////////////////////////////////////

  // PLACED SHAPES


  protected void rectImpl(float x1, float y1, float x2, float y2) {
    quad(x1, y1,  x2, y1,  x2, y2,  x1, y2);
  }


  protected void ellipseImpl(float x1, float y1, float w, float h) {
    float hradius = w / 2f;
    float vradius = h / 2f;

    float centerX = x1 + hradius;
    float centerY = y1 + vradius;

    // adapt accuracy to radii used w/ a minimum of 4 segments [toxi]
    // now uses current scale factors to determine "real" transformed radius

    //int cAccuracy = (int)(4+Math.sqrt(hradius*abs(m00)+vradius*abs(m11))*2);
    //int cAccuracy = (int)(4+Math.sqrt(hradius+vradius)*2);

    // notched this up to *3 instead of *2 because things were
    // looking a little rough, i.e. the calculate->arctangent example [fry]

    // also removed the m00 and m11 because those were causing weirdness
    // need an actual measure of magnitude in there [fry]

    int cAccuracy = (int)(4+Math.sqrt(hradius+vradius)*3);

    // [toxi031031] adapted to use new lookup tables
    float inc = (float)SINCOS_LENGTH / cAccuracy;

    float val = 0;
    /*
    beginShape(POLYGON);
    for (int i = 0; i < cAccuracy; i++) {
      vertex(centerX + cosLUT[(int) val] * hradius,
             centerY + sinLUT[(int) val] * vradius);
      val += inc;
    }
    endShape();
    */

    if (fill) {
      boolean savedStroke = stroke;
      stroke = false;

      beginShape(TRIANGLE_FAN);
      normal(0, 0, 1);
      vertex(centerX, centerY);
      for (int i = 0; i < cAccuracy; i++) {
        vertex(centerX + cosLUT[(int) val] * hradius,
               centerY + sinLUT[(int) val] * vradius);
        val += inc;
      }
      // back to the beginning
      vertex(centerX + cosLUT[0] * hradius,
             centerY + sinLUT[0] * vradius);
      endShape();

      stroke = savedStroke;
    }

    if (stroke) {
      boolean savedFill = fill;
      fill = false;

      val = 0;
      beginShape(LINE_LOOP);
      for (int i = 0; i < cAccuracy; i++) {
        vertex(centerX + cosLUT[(int) val] * hradius,
               centerY + sinLUT[(int) val] * vradius);
        val += inc;
      }
      endShape();

      fill = savedFill;
    }
  }


  /**
   * Start and stop are in radians, converted by the parent function.
   * Note that the radians can be greater (or less) than TWO_PI.
   * This is so that an arc can be drawn that crosses zero mark,
   * and the user will still collect $200.
   */
  protected void arcImpl(float x1, float y1, float w, float h,
                         float start, float stop) {
    float hr = w / 2f;
    float vr = h / 2f;

    float centerX = x1 + hr;
    float centerY = y1 + vr;

    if (fill) {
      // shut off stroke for a minute
      boolean savedStroke = stroke;
      stroke = false;

      int startLUT = (int) (0.5f + (start / TWO_PI) * SINCOS_LENGTH);
      int stopLUT = (int) (0.5f + (stop / TWO_PI) * SINCOS_LENGTH);

      beginShape(TRIANGLE_FAN);
      vertex(centerX, centerY);
      int increment = 1; // what's a good algorithm? stopLUT - startLUT;
      for (int i = startLUT; i < stopLUT; i += increment) {
        int ii = i % SINCOS_LENGTH;
        vertex(centerX + cosLUT[ii] * hr,
               centerY + sinLUT[ii] * vr);
      }
      // draw last point explicitly for accuracy
      vertex(centerX + cosLUT[stopLUT % SINCOS_LENGTH] * hr,
             centerY + sinLUT[stopLUT % SINCOS_LENGTH] * vr);
      endShape();

      stroke = savedStroke;
    }

    if (stroke) {
      // Almost identical to above, but this uses a LINE_STRIP
      // and doesn't include the first (center) vertex.

      boolean savedFill = fill;
      fill = false;

      int startLUT = (int) (0.5f + (start / TWO_PI) * SINCOS_LENGTH);
      int stopLUT = (int) (0.5f + (stop / TWO_PI) * SINCOS_LENGTH);

      beginShape(LINE_STRIP);
      int increment = 1; // what's a good algorithm? stopLUT - startLUT;
      for (int i = startLUT; i < stopLUT; i += increment) {
        int ii = i % SINCOS_LENGTH;
        vertex(centerX + cosLUT[ii] * hr,
               centerY + sinLUT[ii] * vr);
      }
      // draw last point explicitly for accuracy
      vertex(centerX + cosLUT[stopLUT % SINCOS_LENGTH] * hr,
             centerY + sinLUT[stopLUT % SINCOS_LENGTH] * vr);
      endShape();

      fill = savedFill;
    }
  }



  //////////////////////////////////////////////////////////////

  // 3D BOX


  public void box(float size) {
    box(size, size, size);
  }


  // OPT this isn't the least bit efficient
  //     because it redraws lines along the vertices
  //     ugly ugly ugly!
  public void box(float w, float h, float d) {
    float x1 = -w/2f; float x2 = w/2f;
    float y1 = -h/2f; float y2 = h/2f;
    float z1 = -d/2f; float z2 = d/2f;

    if (triangle != null) {  // triangle is null in gl
      triangle.setCulling(true);
    }

    beginShape(QUADS);

    // front
    normal(0, 0, 1);
    vertex(x1, y1, z1);
    vertex(x2, y1, z1);
    vertex(x2, y2, z1);
    vertex(x1, y2, z1);

    // right
    normal(1, 0, 0);
    vertex(x2, y1, z1);
    vertex(x2, y1, z2);
    vertex(x2, y2, z2);
    vertex(x2, y2, z1);

    // back
    normal(0, 0, -1);
    vertex(x2, y1, z2);
    vertex(x1, y1, z2);
    vertex(x1, y2, z2);
    vertex(x2, y2, z2);

    // left
    normal(-1, 0, 0);
    vertex(x1, y1, z2);
    vertex(x1, y1, z1);
    vertex(x1, y2, z1);
    vertex(x1, y2, z2);

    // top
    normal(0, 1, 0);
    vertex(x1, y1, z2);
    vertex(x2, y1, z2);
    vertex(x2, y1, z1);
    vertex(x1, y1, z1);

    // bottom
    normal(0, -1, 0);
    vertex(x1, y2, z1);
    vertex(x2, y2, z1);
    vertex(x2, y2, z2);
    vertex(x1, y2, z2);

    endShape();

    if (triangle != null) {  // triangle is null in gl
      triangle.setCulling(false);
    }
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


  /**
   * Draw a sphere with radius r centered at coordinate 0, 0, 0.
   * <P>
   * Implementation notes:
   * <P>
   * cache all the points of the sphere in a static array
   * top and bottom are just a bunch of triangles that land
   * in the center point
   * <P>
   * sphere is a series of concentric circles who radii vary
   * along the shape, based on, er.. cos or something
   * <PRE>
   * [toxi031031] new sphere code. removed all multiplies with
   * radius, as scale() will take care of that anyway
   *
   * [toxi031223] updated sphere code (removed modulos)
   * and introduced sphereAt(x,y,z,r)
   * to avoid additional translate()'s on the user/sketch side
   * </PRE>
   */
  public void sphere(float r) {
    float x = 0;  // TODO clean this back up again
    float y = 0;
    float z = 0;

    if (sphereDetail == 0) {
      sphereDetail(30);
    }

    int v1,v11,v2;
    push();
    if (x!=0f && y!=0f && z!=0f) translate(x,y,z);
    scale(r);

    if (triangle != null) {  // triangle is null in gl
      triangle.setCulling(true);
    }

    // 1st ring from south pole
    beginShape(TRIANGLE_STRIP);
    for (int i = 0; i < sphereDetail; i++) {
      normal(0, -1, 0);
      vertex(0, -1, 0);
      normal(sphereX[i], sphereY[i], sphereZ[i]);
      vertex(sphereX[i], sphereY[i], sphereZ[i]);
    }
    normal(0, -1, 0);
    vertex(0, -1, 0);
    normal(sphereX[0], sphereY[0], sphereZ[0]);
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
        normal(sphereX[v1], sphereY[v1], sphereZ[v1]);
        vertex(sphereX[v1], sphereY[v1], sphereZ[v1++]);
        normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
        vertex(sphereX[v2], sphereY[v2], sphereZ[v2++]);
      }
      // close each ring
      v1=v11;
      v2=voff;
      normal(sphereX[v1], sphereY[v1], sphereZ[v1]);
      vertex(sphereX[v1], sphereY[v1], sphereZ[v1]);
      normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
      vertex(sphereX[v2], sphereY[v2], sphereZ[v2]);
      endShape();
    }

    // add the northern cap
    beginShape(TRIANGLE_STRIP);
    for (int i = 0; i < sphereDetail; i++) {
      v2 = voff + i;
      normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
      vertex(sphereX[v2], sphereY[v2], sphereZ[v2]);
      normal(0, 1, 0);
    vertex(0, 1, 0);
    }
    normal(sphereX[voff], sphereY[voff], sphereZ[voff]);
    vertex(sphereX[voff], sphereY[voff], sphereZ[voff]);
    normal(0, 1, 0);
    vertex(0, 1, 0);
    endShape();
    pop();

    if (triangle != null) {  // triangle is null in gl
      triangle.setCulling(false);
    }
  }



  //////////////////////////////////////////////////////////////

  // CURVES


  public void bezier(float x1, float y1,
                     float x2, float y2,
                     float x3, float y3,
                     float x4, float y4) {
    bezier(x1, y1, 0,
           x2, y2, 0,
           x3, y3, 0,
           x4, y4, 0);
  }


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


  public void curve(float x1, float y1,
                    float x2, float y2,
                    float x3, float y3,
                    float x4, float y4) {
    curve(x1, y1, 0,
          x2, y2, 0,
          x3, y3, 0,
          x4, y4, 0);
  }


  public void curve(float x1, float y1, float z1,
                    float x2, float y2, float z2,
                    float x3, float y3, float z3,
                    float x4, float y4, float z4) {
    beginShape(LINE_STRIP);
    curveVertex(x1, y1, z1);
    curveVertex(x2, y2, z2);
    curveVertex(x3, y3, z3);
    curveVertex(x4, y4, z4);
    endShape();
  }


  //////////////////////////////////////////////////////////////


  protected void imageImpl(PImage image,
                           float x1, float y1, float x2, float y2,
                           int u1, int v1, int u2, int v2) {

    //float x2 = x1 + w;
    //float y2 = y1 + h;

    boolean savedStroke = stroke;
    boolean savedFill = fill;
    int savedTextureMode = textureMode;

    stroke = false;
    fill = true;
    textureMode = IMAGE;

    float savedFillR = fillR;
    float savedFillG = fillG;
    float savedFillB = fillB;
    float savedFillA = fillA;

    if (tint) {
      fillR = tintR;
      fillG = tintG;
      fillB = tintB;
      fillA = tintA;

    } else {
      fillR = 1;
      fillG = 1;
      fillB = 1;
      fillA = 1;
    }

    //System.out.println(fill + " " + fillR + " " + fillG + " " + fillB);

    beginShape(QUADS);
    texture(image);
    vertex(x1, y1, u1, v1);
    vertex(x1, y2, u1, v2);
    vertex(x2, y2, u2, v2);
    vertex(x2, y1, u2, v1);
    endShape();

    stroke = savedStroke;
    fill = savedFill;
    textureMode = savedTextureMode;

    fillR = savedFillR;
    fillG = savedFillG;
    fillB = savedFillB;
    fillA = savedFillA;
  }


  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS


  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }


  public void translate(float tx, float ty, float tz) {
    forwardTransform.translate(tx, ty, tz);
    reverseTransform.invTranslate(tx, ty, tz);
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


  // OPT could save several multiplies for the 0s and 1s by just
  //     putting the multMatrix code here and removing uneccessary terms

  public void rotateX(float angle) {
    forwardTransform.rotateX(angle);
    reverseTransform.invRotateX(angle);
  }


  public void rotateY(float angle) {
    forwardTransform.rotateY(angle);
    reverseTransform.invRotateY(angle);
  }


  /**
   * Rotate in the XY plane by an angle.
   *
   * Note that this doesn't internally set the number of
   * dimensions to three, since rotateZ() is the same as a
   * 2D rotate in the XY plane.
   */
  public void rotateZ(float angle) {
    forwardTransform.rotateZ(angle);
    reverseTransform.invRotateZ(angle);
  }


  /**
   * Rotate around an arbitrary vector, similar to glRotate(),
   * except that it takes radians (instead of degrees) by default,
   * unless angleMode is set to RADIANS.
   */
  public void rotate(float angle, float v0, float v1, float v2) {
    forwardTransform.rotate(angle, v0, v1, v2);
    reverseTransform.invRotate(angle, v0, v1, v2);
  }


  /**
   * Same as scale(s, s, s);
   */
  public void scale(float s) {
    scale(s, s, s);
  }


  /**
   * Not recommended for use in 3D, because the z-dimension is just
   * scaled by 1, since there's no way to know what else to scale it by.
   * Equivalent to scale(sx, sy, 1);
   */
  public void scale(float sx, float sy) {
    scale(sx, sy, 1);
  }


  /**
   * Scale in three dimensions.
   */
  public void scale(float x, float y, float z) {
    forwardTransform.scale(x, y, z);
    reverseTransform.invScale(x, y, z);
  }



  //////////////////////////////////////////////////////////////

  // TRANSFORMATION MATRIX


  public void push() {
    if (!modelview.push()) {
      throw new RuntimeException("too many calls to push()");
    }
    // Do this to the inverse regardless of the lights to keep stack pointers
    // in sync
    inverseModelview.push();
  }


  public void pop() {
    if (!modelview.pop()) {
      throw new RuntimeException("too many calls to pop() " +
                                 "(and not enough to push)");
    }
    //  Do this to the inverse regardless of the lights to keep stack pointers
    // in sync
    inverseModelview.pop();
  }


  public void resetProjection() {
    projection.reset();
  }

  /**
   * Load identity as the transform/model matrix.
   * Same as glLoadIdentity().
   */
  public void resetMatrix() {
    forwardTransform.reset();
    reverseTransform.reset();
  }


  /**
   * Apply a 4x4 transformation matrix. Same as glMultMatrix().
   * This call will be slow because it will try to calculate the
   * inverse of the transform. So avoid it whenever possible.
   */
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {

    forwardTransform.applyMatrix(n00, n01, n02, n03,
                          n10, n11, n12, n13,
                          n20, n21, n22, n23,
                          n30, n31, n32, n33);

    reverseTransform.invApplyMatrix(n00, n01, n02, n03,
                                      n10, n11, n12, n13,
                                      n20, n21, n22, n23,
                                      n30, n31, n32, n33);
  }



  /**
   * Print the current model (or "transformation") matrix.
   */
  public void printMatrix() {
    modelview.print();
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
    resetProjection();
    modelview.identity();
    inverseModelview.identity();

    if (mode == PERSPECTIVE) {
      //System.out.println("setting camera to perspective");
      //System.out.println("  " + cameraFOV + " " + cameraAspect);
      perspective(cameraFOV, cameraAspect, cameraNear, cameraFar);
      lookat(cameraX, cameraY, cameraZ,
             cameraX, cameraY, 0,
             0, 1, 0);

    } else if (mode == ORTHOGRAPHIC) {
      ortho(0, width, 0, height, -10, 10);
    }

    cameraMode = mode;  // this doesn't do much
  }


  /**
   * Set matrix mode to the camera matrix (instead of
   * the current transformation matrix). This means applyMatrix,
   * resetMatrix, etc. will affect the camera.
   *
   * This loads identity into the projection matrix, so if you want
   * to start with a resonable default projection, you may want to
   * call cameraMode(PERSPECTIVE); or something between begin and end.
   */
  public void beginCamera() {
    if (manipulatingCamera) {
      throw new RuntimeException("cannot call beginCamera while already "+
                                 "in camera manipulation mode");
    }
    else {
      projection.identity();
      manipulatingCamera = true;
      forwardTransform = inverseModelview;
      reverseTransform = modelview;
    cameraMode = CUSTOM;
  }
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
    if (!manipulatingCamera) {
      throw new RuntimeException("cannot call endCamera while not "+
                                 "in camera manipulation mode");
    }
    else {
      manipulatingCamera = false;
      forwardTransform = modelview;
      reverseTransform = inverseModelview;
    }
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

    projection.applyMatrix(x, 0, 0, tx,
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
    projection.applyMatrix((2*znear)/(right-left), 0, (right+left)/(right-left), 0,
                0, (2*znear)/(top-bottom), (top+bottom)/(top-bottom), 0,
                0, 0, -(zfar+znear)/(zfar-znear),-(2*zfar*znear)/(zfar-znear),
                0, 0, -1, 0);
  }


  /**
   * Same as gluLookat(). Implementation based on Mesa's glu.c
   */

  // TODO: deal with this. Lookat must ALWAYS apply to the modelview
  // regardless of the camera manipulation mode.
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

    modelview.invApplyMatrix(x0, x1, x2, 0,
                             y0, y1, y2, 0,
                             z0, z1, z2, 0,
                             0,  0,  0,  1);
    modelview.invTranslate(eyeX, eyeY, eyeZ);

    inverseModelview.applyMatrix(x0, x1, x2, 0,
                y0, y1, y2, 0,
                z0, z1, z2, 0,
                0,  0,  0,  1);
    inverseModelview.translate(eyeX, eyeY, eyeZ);
  }



  /**
   * Print the current camera (or "perspective") matrix.
   */
  public void printCamera() {
    projection.print();
  }



  //////////////////////////////////////////////////////////////

  // SCREEN AND OBJECT COORDINATES


  public float screenX(float x, float y) {
    return screenX(x, y, 0);
  }


  public float screenY(float x, float y) {
    return screenY(x, y, 0);
  }


  public float screenX(float x, float y, float z) {
    float ax = modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay = modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az = modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw = modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float ox = projection.m00*ax + projection.m01*ay + projection.m02*az + projection.m03*aw;
    float ow = projection.m30*ax + projection.m31*ay + projection.m32*az + projection.m33*aw;

    if (ow != 0) ox /= ow;
    return width * (1 + ox) / 2.0f;
  }


  public float screenY(float x, float y, float z) {
    float ax = modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay = modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az = modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw = modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float oy = projection.m10*ax + projection.m11*ay + projection.m12*az + projection.m13*aw;
    float ow = projection.m30*ax + projection.m31*ay + projection.m32*az + projection.m33*aw;

    if (ow != 0) oy /= ow;
    return height * (1 + oy) / 2.0f;
  }


  public float screenZ(float x, float y, float z) {
    float ax = modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay = modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az = modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw = modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float oz = projection.m20*ax + projection.m21*ay + projection.m22*az + projection.m23*aw;
    float ow = projection.m30*ax + projection.m31*ay + projection.m32*az + projection.m33*aw;

    if (ow != 0) oz /= ow;
    return (oz + 1) / 2.0f;
  }


  public float objectX(float x, float y, float z) {
    float ax = modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float aw = modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;
    return (aw != 0) ? ax / aw : ax;
  }


  public float objectY(float x, float y, float z) {
    float ay = modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float aw = modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;
    return (aw != 0) ? ay / aw : ay;
  }


  public float objectZ(float x, float y, float z) {
    float az = modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw = modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;
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
    //System.out.println("PGraphics3.clear(" +
    //                 PApplet.hex(backgroundColor) + ")");
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
    lightX[num] = modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    lightY[num] = modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    lightZ[num] = modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
  }

  public void lightAmbient(int num, float x, float y, float z) {
    calc_color(x, y, z);
    lightDiffuseR[num] = calcR;
    lightDiffuseG[num] = calcG;
    lightDiffuseB[num] = calcB;
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

  public void lightDirection(int num, float x, float y, float z) {
    // Multiply by inverse transpose.
    lightNX[num] = inverseModelview.m00*x + inverseModelview.m10*y + inverseModelview.m20*z + inverseModelview.m30;
    lightNY[num] = inverseModelview.m01*x + inverseModelview.m11*y + inverseModelview.m21*z + inverseModelview.m31;
    lightNZ[num] = inverseModelview.m02*x + inverseModelview.m12*y + inverseModelview.m22*z + inverseModelview.m32;
    float norm = mag(lightNX[num], lightNY[num], lightNZ[num]);
    if (norm == 0 || norm == 1) return;
    lightNX[num] /= norm;
    lightNY[num] /= norm;
    lightNZ[num] /= norm;
  }


  public void lightFalloff(int num, float constant, float linear, float quadratic) {
    lightConstantFalloff[num] = constant;
    lightLinearFalloff[num] = linear;
    lightQuadraticFalloff[num] = quadratic;
  }

  public void lightSpotAngle(int num, float spotAngle) {
    lightSpotAngle[num] = spotAngle;
    lightCosSpotAngle[num] = max(0, cos(spotAngle));
  }

  public void lightSpotConcentration(int num, float concentration) {
    lightSpotConcentration[num] = concentration;
  }

  //////////////////////////////////////////////////////////////

  // SMOOTH (not available, throws error)

  // although should this bother throwing an error?
  // could be a pain in the ass when trying to debug with opengl


  public void smooth() {
    String msg = "smooth() not available when used with depth()";
    throw new RuntimeException(msg);
  }


  public void noSmooth() {
    String msg = "noSmooth() not available when used with depth()";
    throw new RuntimeException(msg);
  }


  //////////////////////////////////////////////////////////////


  public void fill(int rgb) {
    super.fill(rgb);
    calc_ambient();
  }

  public void fill(float gray) {
    super.fill(gray);
    calc_ambient();
  }


  public void fill(float gray, float alpha) {
    super.fill(gray, alpha);
    calc_ambient();
  }


  public void fill(float x, float y, float z) {
    super.fill(x, y, z);
    calc_ambient();
  }


  public void fill(float x, float y, float z, float a) {
    super.fill(x, y, z, a);
    calc_ambient();
  }

  private void calc_ambient() {
    ambientR = calcR;
    ambientG = calcG;
    ambientB = calcB;
    ambientRi = calcRi;
    ambientGi = calcGi;
    ambientBi = calcBi;
  }


  //////////////////////////////////////////////////////////////


  /*
  public void diffuse(int rgb) {
    super.fill(rgb);
  }

  public void diffuse(float gray) {
    super.fill(gray);
  }


  public void diffuse(float gray, float alpha) {
    super.fill(gray, alpha);
  }


  public void diffuse(float x, float y, float z) {
    super.fill(x, y, z);
  }


  public void diffuse(float x, float y, float z, float a) {
    super.fill(x, y, z, a);
  }
  */


  //////////////////////////////////////////////////////////////


  public void ambient(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      ambient((float) rgb);

    } else {
      calc_color_from(rgb);
      calc_ambient();
    }
  }

  public void ambient(float gray) {
    calc_color(gray);
    calc_ambient();
  }

  public void ambient(float x, float y, float z) {
    calc_color(x, y, z);
    calc_ambient();
  }


  //////////////////////////////////////////////////////////////


  public void specular(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      specular((float) rgb);

    } else {
      calc_color_from(rgb);
      calc_specular();
    }
  }

  public void specular(float gray) {
    calc_color(gray);
    calc_specular();
  }


  public void specular(float gray, float alpha) {
    calc_color(gray, alpha);
    calc_specular();
  }


  public void specular(float x, float y, float z) {
    calc_color(x, y, z);
    calc_specular();
  }


  public void specular(float x, float y, float z, float a) {
    calc_color(x, y, z, a);
    calc_specular();
  }


  protected void calc_specular() {
    specularR = calcR;
    specularG = calcG;
    specularB = calcB;
    specularA = calcA;
    specularRi = calcRi;
    specularGi = calcGi;
    specularBi = calcBi;
    specularAi = calcAi;
  }


  //////////////////////////////////////////////////////////////


  public void emissive(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      emissive((float) rgb);

    } else {
      calc_color_from(rgb);
      calc_emissive();
    }
  }

  public void emissive(float gray) {
    calc_color(gray);
    calc_emissive();
  }

  public void emissive(float x, float y, float z) {
    calc_color(x, y, z);
    calc_emissive();
  }

  protected void calc_emissive() {
    emissiveR = calcR;
    emissiveG = calcG;
    emissiveB = calcB;
    emissiveRi = calcRi;
    emissiveGi = calcGi;
    emissiveBi = calcBi;
  }


  //////////////////////////////////////////////////////////////


  public void shininess(float shine) {
    shininess = shine;
  }


  //////////////////////////////////////////////////////////////


  public int createAmbientLight(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      return createAmbientLight((float) rgb);
    } else {
      if (lightCount >= MAX_LIGHTS) {
        throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
      }
      calc_color_from(rgb);
      return internalCreateAmbientLight(calcR, calcG, calcB);
    }
  }

  public int createAmbientLight(float gray) {
    if (lightCount >= MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    calc_color(gray);
    return internalCreateAmbientLight(calcR, calcG, calcB);
  }

  public int createAmbientLight(float lr, float lg, float lb) {
    if (lightCount >= MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    calc_color(lr, lg, lb);
    return internalCreateAmbientLight(calcR, calcG, calcB);
  }

  protected int internalCreateAmbientLight(float lr, float lg, float lb) {
    lightDiffuseR[lightCount] = lr;
    lightDiffuseG[lightCount] = lg;
    lightDiffuseB[lightCount] = lb;
    light[lightCount] = true;
    lightType[lightCount] = AMBIENT;
    lightConstantFalloff[lightCount] = 1;
    lightLinearFalloff[lightCount] = 0;
    lightQuadraticFalloff[lightCount] = 0;
    lightPosition(lightCount, 0, 0, 0);
    lightCount++;
    return lightCount-1;
  }

  public int createDirectionalLight(int rgb, float nx, float ny, float nz) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      return createDirectionalLight((float) rgb, nx, ny, nz);
    } else {
      if (lightCount >= MAX_LIGHTS) {
        throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
      }
      calc_color_from(rgb);
      return internalCreateDirectionalLight(calcR, calcG, calcB, nx, ny, nz);
    }
  }

  public int createDirectionalLight(float gray, float nx, float ny, float nz) {
    if (lightCount >= MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    calc_color(gray);
    return internalCreateDirectionalLight(calcR, calcG, calcB, nx, ny, nz);
  }

  public int createDirectionalLight(float lr, float lg, float lb, float nx, float ny, float nz) {
    if (lightCount >= MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    calc_color(lr, lg, lb);
    return internalCreateDirectionalLight(calcR, calcG, calcB, nx, ny, nz);
  }

  protected int internalCreateDirectionalLight(float lr, float lg, float lb, float nx, float ny, float nz) {
    lightDiffuseR[lightCount] = lr;
    lightDiffuseG[lightCount] = lg;
    lightDiffuseB[lightCount] = lb;
    light[lightCount] = true;
    lightType[lightCount] = DIRECTIONAL;
    lightConstantFalloff[lightCount] = 1;
    lightLinearFalloff[lightCount] = 0;
    lightQuadraticFalloff[lightCount] = 0;
    lightSpecularR[lightCount] = 0;
    lightSpecularG[lightCount] = 0;
    lightSpecularB[lightCount] = 0;
    lightDirection(lightCount, nx, ny, nz);
    lightCount++;
    return lightCount-1;
  }

  public int createPointLight(int rgb, float x, float y, float z) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      return createPointLight((float) rgb, x, y, z);
    } else {
      if (lightCount >= MAX_LIGHTS) {
        throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
      }
      calc_color_from(rgb);
      return internalCreatePointLight(calcR, calcG, calcB, x, y, z);
    }
  }

  public int createPointLight(float gray, float x, float y, float z) {
    if (lightCount >= MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    calc_color(gray);
    return internalCreatePointLight(calcR, calcG, calcB, x, y, z);
  }

  public int createPointLight(float lr, float lg, float lb, float x, float y, float z) {
    if (lightCount >= MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    calc_color(lr, lg, lb);
    return internalCreatePointLight(calcR, calcG, calcB, x, y, z);
  }

  protected int internalCreatePointLight(float lr, float lg, float lb, float x, float y, float z) {
    lightDiffuseR[lightCount] = lr;
    lightDiffuseG[lightCount] = lg;
    lightDiffuseB[lightCount] = lb;
    light[lightCount] = true;
    lightType[lightCount] = POINT;
    lightConstantFalloff[lightCount] = 1;
    lightLinearFalloff[lightCount] = 0;
    lightQuadraticFalloff[lightCount] = 0;
    lightSpecularR[lightCount] = 0;
    lightSpecularG[lightCount] = 0;
    lightSpecularB[lightCount] = 0;
    lightPosition(lightCount, x, y, z);
    lightCount++;
    return lightCount-1;
  }

  public int createSpotLight(int rgb, float x, float y, float z, float nx, float ny, float nz, float angle) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      return createSpotLight((float) rgb, x, y, z, nx, ny, nz, angle);
    } else {
      if (lightCount >= MAX_LIGHTS) {
        throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
      }
      calc_color_from(rgb);
      return internalCreateSpotLight(calcR, calcG, calcB, x, y, z, nx, ny, nz, angle);
    }
  }

  public int createSpotLight(float gray, float x, float y, float z, float nx, float ny, float nz, float angle) {
    if (lightCount >= MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    calc_color(gray);
    return internalCreateSpotLight(calcR, calcG, calcB, x, y, z, nx, ny, nz, angle);
  }

  public int createSpotLight(float lr, float lg, float lb, float x, float y, float z, float nx, float ny, float nz, float angle) {
    if (lightCount >= MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    calc_color(lr, lg, lb);
    return internalCreateSpotLight(calcR, calcG, calcB, x, y, z, nx, ny, nz, angle);
  }

  protected int internalCreateSpotLight(float lr, float lg, float lb, float x, float y, float z, float nx, float ny, float nz, float angle) {
    lightDiffuseR[lightCount] = lr;
    lightDiffuseG[lightCount] = lg;
    lightDiffuseB[lightCount] = lb;
    light[lightCount] = true;
    lightType[lightCount] = SPOT;
    lightConstantFalloff[lightCount] = 1;
    lightLinearFalloff[lightCount] = 0;
    lightQuadraticFalloff[lightCount] = 0;
    lightSpecularR[lightCount] = 0;
    lightSpecularG[lightCount] = 0;
    lightSpecularB[lightCount] = 0;
    lightPosition(lightCount, x, y, z);
    lightDirection(lightCount, nx, ny, nz);
    lightSpotAngle(lightCount, angle);
    lightSpotConcentration[lightCount] = 1;
    lightCount++;
    return lightCount-1;
  }

  //////////////////////////////////////////////////////////////

  // MATH (internal use only)


  private final float mag(float a, float b) {
    return (float)Math.sqrt(a*a + b*b);
  }

  private final float mag(float a, float b, float c) {
    return (float)Math.sqrt(a*a + b*b + c*c);
  }

  private final float min(float a, float b) {
    return (a < b) ? a : b;
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

  private final float pow(float a, float b) {
    return (float)Math.pow(a, b);
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

