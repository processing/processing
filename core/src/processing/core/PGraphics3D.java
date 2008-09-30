/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-08 Ben Fry and Casey Reas
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

import java.awt.Toolkit;
import java.awt.image.*;
import java.util.*;


/**
 * Subclass of PGraphics that handles 3D rendering.
 * It can render 3D inside a browser window and requires no plug-ins.
 * <p/>
 * The renderer is mostly set up based on the structure of the OpenGL API,
 * if you have questions about specifics that aren't covered here,
 * look for reference on the OpenGL implementation of a similar feature.
 * <p/>
 * Lighting and camera implementation by Simon Greenwold.
 */
public class PGraphics3D extends PGraphics {

  static public final int TRI_DIFFUSE_R = 0;
  static public final int TRI_DIFFUSE_G = 1;
  static public final int TRI_DIFFUSE_B = 2;
  static public final int TRI_DIFFUSE_A = 3;
  static public final int TRI_SPECULAR_R = 4;
  static public final int TRI_SPECULAR_G = 5;
  static public final int TRI_SPECULAR_B = 6;
  static public final int TRI_COLOR_COUNT = 7;

  // ........................................................

  // Whether or not we have to worry about vertex position for lighting calcs
  private boolean lightingDependsOnVertexPosition;

  static final int LIGHT_AMBIENT_R = 0;
  static final int LIGHT_AMBIENT_G = 1;
  static final int LIGHT_AMBIENT_B = 2;
  static final int LIGHT_DIFFUSE_R = 3;
  static final int LIGHT_DIFFUSE_G = 4;
  static final int LIGHT_DIFFUSE_B = 5;
  static final int LIGHT_SPECULAR_R = 6;
  static final int LIGHT_SPECULAR_G = 7;
  static final int LIGHT_SPECULAR_B = 8;
  static final int LIGHT_COLOR_COUNT = 9;

  // Used to shuttle lighting calcs around
  // (no need to re-allocate all the time)
  protected float[] tempLightingContribution = new float[LIGHT_COLOR_COUNT];
  protected float[] worldNormal = new float[4];

  /// Used in light_triangle(). Allocated here once to avoid re-allocating
  protected float[] norm = new float[3];

  // ........................................................

  /**
   * This is turned on at beginCamera, and off at endCamera
   * Currently we don't support nested begin/end cameras.
   * If we wanted to, this variable would have to become a stack.
   */
  protected boolean manipulatingCamera;

  float[][] matrixStack = new float[MATRIX_STACK_DEPTH][16];
  float[][] matrixInvStack = new float[MATRIX_STACK_DEPTH][16];
  int matrixStackDepth;

  // These two matrices always point to either the modelview
  // or the modelviewInv, but they are swapped during
  // when in camera manipulation mode. That way camera transforms
  // are automatically accumulated in inverse on the modelview matrix.
  protected PMatrix3D forwardTransform;
  protected PMatrix3D reverseTransform;

  // Added by ewjordan for accurate texturing purposes. Screen plane is
  // not scaled to pixel-size, so these manually keep track of its size
  // from frustum() calls. Sorry to add public vars, is there a way
  // to compute these from something publicly available without matrix ops?
  // (used once per triangle in PTriangle with ENABLE_ACCURATE_TEXTURES)
  protected float leftScreen;
  protected float rightScreen;
  protected float topScreen;
  protected float bottomScreen;
  protected float nearPlane; //depth of near clipping plane

  // ........................................................

  // pos of first vertex of current shape in vertices array
  protected int vertex_start;

  // i think vertex_end is actually the last vertex in the current shape
  // and is separate from vertexCount for occasions where drawing happens
  // on endDraw() with all the triangles being depth sorted
  protected int vertex_end;

  // vertices may be added during clipping against the near plane.
  protected int vertex_end_including_clip_verts;

  // used for sorting points when triangulating a polygon
  // warning - maximum number of vertices for a polygon is DEFAULT_VERTICES
  protected int vertex_order[] = new int[DEFAULT_VERTICES];

  // ........................................................

  protected int pathCount;
  protected int pathOffset[] = new int[64];
  protected int pathLength[] = new int[64];

  // ........................................................

  // line & triangle fields (note that these overlap)
  static protected final int INDEX = 0;          // shape index
  static protected final int VERTEX1 = 1;
  static protected final int VERTEX2 = 2;
  static protected final int VERTEX3 = 3;        // (triangles only)
  static protected final int TEXTURE_INDEX = 4;  // (triangles only)
  static protected final int STROKE_MODE = 3;    // (lines only)
  static protected final int STROKE_WEIGHT = 4;  // (lines only)

  static protected final int LINE_FIELD_COUNT = 5;
  static protected final int TRIANGLE_FIELD_COUNT = 5;

  // lines
  static final int DEFAULT_LINES = 512;
  public PLine line;  // used for drawing
  protected int[][] lines = new int[DEFAULT_LINES][LINE_FIELD_COUNT];
  protected int lineCount;

  // triangles
  static final int DEFAULT_TRIANGLES = 256;
  public PTriangle triangle;
  protected int[][] triangles =
    new int[DEFAULT_TRIANGLES][TRIANGLE_FIELD_COUNT];
  protected float triangleColors[][][] =
    new float[DEFAULT_TRIANGLES][3][TRI_COLOR_COUNT];
  protected int triangleCount;   // total number of triangles

  // cheap picking someday
  public int shape_index;

  // ........................................................

  static final int DEFAULT_TEXTURES = 3;
  protected PImage[] textures = new PImage[DEFAULT_TEXTURES];
  int textureIndex;

  // ........................................................
  
  DirectColorModel cm;
  MemoryImageSource mis;


  //////////////////////////////////////////////////////////////


  public PGraphics3D() { }


  /**
   * Called in repsonse to a resize event, handles setting the
   * new width and height internally, as well as re-allocating
   * the pixel buffer for the new size.
   *
   * Note that this will nuke any cameraMode() settings.
   */
  public void setSize(int iwidth, int iheight) {  // ignore
//    System.out.println("PGraphics3D.resize() " + iwidth + " " + iheight);

    width = iwidth;
    height = iheight;
    width1 = width - 1;
    height1 = height - 1;

    allocate();
    reapplySettings();

    // init lights (in resize() instead of allocate() b/c needed by opengl)
    lightType = new int[MAX_LIGHTS];
    lightPosition = new float[MAX_LIGHTS][3];
    lightDiffuse = new float[MAX_LIGHTS][3];
    lightNormal = new float[MAX_LIGHTS][3];
    lightSpecular = new float[MAX_LIGHTS][3];
    lightFalloffConstant = new float[MAX_LIGHTS];
    lightFalloffLinear = new float[MAX_LIGHTS];
    lightFalloffQuadratic = new float[MAX_LIGHTS];
    lightSpotAngle = new float[MAX_LIGHTS];
    lightSpotAngleCos = new float[MAX_LIGHTS];
    lightSpotConcentration = new float[MAX_LIGHTS];
    currentLightSpecular = new float[3];

    projection = new PMatrix3D();
    modelview = new PMatrix3D();
    modelviewInv = new PMatrix3D();

//    modelviewStack = new float[MATRIX_STACK_DEPTH][16];
//    modelviewInvStack = new float[MATRIX_STACK_DEPTH][16];
//    modelviewStackPointer = 0;

    forwardTransform = modelview;
    reverseTransform = modelviewInv;

    // init perspective projection based on new dimensions
    cameraFOV = 60 * DEG_TO_RAD; // at least for now
    cameraX = width / 2.0f;
    cameraY = height / 2.0f;
    cameraZ = cameraY / ((float) Math.tan(cameraFOV / 2.0f));
    cameraNear = cameraZ / 10.0f;
    cameraFar = cameraZ * 10.0f;
    cameraAspect = (float)width / (float)height;

    camera = new PMatrix3D();
    cameraInv = new PMatrix3D();

    // set up the default camera
    camera();

    // defaults to perspective, if the user has setup up their
    // own projection, they'll need to fix it after resize anyway.
    // this helps the people who haven't set up their own projection.
    perspective();
  }


  protected void allocate() {
    //System.out.println(this + " allocating for " + width + " " + height);
    //new Exception().printStackTrace();

    pixelCount = width * height;
    pixels = new int[pixelCount];
    zbuffer = new float[pixelCount];

    if (primarySurface) {
      //for (int i = 0; i < pixelCount; i++) pixels[i] = backgroundColor;
      // Not necessary because background() will be called w/ defaults(),
      // and we're no longer subject to the Java 1.1 bug for transparent
      // pixels in MemoryImageSource.
      //Arrays.fill(pixels, backgroundColor);

      cm = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff);;
      mis = new MemoryImageSource(width, height, pixels, 0, width);
      mis.setFullBufferUpdates(true);
      mis.setAnimated(true);
      image = Toolkit.getDefaultToolkit().createImage(mis);

    } else {
      // when not the main drawing surface, need to set the zbuffer,
      // because there's a possibility that background() will not be called
      //for (int i = 0; i < pixelCount; i++) zbuffer[i] = Float.MAX_VALUE;
      Arrays.fill(zbuffer, Float.MAX_VALUE);
    }

//    stencil = new int[pixelCount];

    line = new PLine(this);
    triangle = new PTriangle(this);

    // can't un-set this because this may be only a resize (Bug #463)
    //settingsInited = false;
    //System.out.println(this + " done allocating");
  }


  public void beginDraw() {
//    insideResizeWait();
//    insideDraw = true;

    // need to call defaults(), but can only be done when it's ok
    // to draw (i.e. for opengl, no drawing can be done outside
    // beginDraw/endDraw).
    if (!settingsInited) defaultSettings();

    resetMatrix(); // reset model matrix

    // reset vertices
    vertexCount = 0;

    modelview.set(camera);
    modelviewInv.set(cameraInv);

    // clear out the lights, they'll have to be turned on again
    lightCount = 0;
    lightingDependsOnVertexPosition = false;
    lightFalloff(1, 0, 0);
    lightSpecular(0, 0, 0);

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
    textureIndex = 0;

    normal(0, 0, 1);
  }


  /**
   * See notes in PGraphics.
   * If z-sorting has been turned on, then the triangles will
   * all be quicksorted here (to make alpha work more properly)
   * and then blit to the screen.
   */
  public void endDraw() {
    // no need to z order and render
    // shapes were already rendered in endShape();
    // (but can't return, since needs to update memimgsrc)
    if (hints[ENABLE_DEPTH_SORT]) {
      flush();
    }
    // blit to screen
    // moving this back here (post-68) because of macosx thread problem
    if (mis != null) {
      mis.newPixels(pixels, cm, 0, width);
    }
    // mark pixels as having been updated, so that they'll work properly
    // when this PGraphics is drawn using image().
    updatePixels();

    //System.out.println(this + " end draw");
//    insideDraw = false;
  }


  /**
   * Emit any sorted geometry that's been collected on this frame.
   */
  protected void flush() {
    if (triangleCount > 0) {
      if (hints[ENABLE_DEPTH_SORT]) {
        sortTriangles();
      }
      renderTriangles();
    }
    if (lineCount > 0) {
      if (hints[ENABLE_DEPTH_SORT]) {
        sortLines();
      }
      renderLines();
    }
    // Clear this out in case flush() is called again.
    // For instance, with hint(ENABLE_DEPTH_SORT), it will be called
    // once on endRaw(), and once again at endDraw().
    triangleCount = 0;
    lineCount = 0;
  }


  public void defaultSettings() {
    super.defaultSettings();

    manipulatingCamera = false;
    forwardTransform = modelview;
    reverseTransform = modelviewInv;

    perspective();

    // easiest for beginners
    textureMode(IMAGE);

    emissive(0.0f);
    specular(0.5f);
    shininess(1.0f);
  }
  
  
  public void hint(int which) {
    if (which == DISABLE_DEPTH_SORT) {
      flush();
    }
    super.hint(which);
  }


  //////////////////////////////////////////////////////////////


  public void beginShape(int kind) {
    shape = kind;

    shape_index = shape_index + 1;
    if (shape_index == -1) {
      shape_index = 0;
    }

    if (hints[ENABLE_DEPTH_SORT]) {
      // continue with previous vertex, line and triangle count
      // all shapes are rendered at endDraw();
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
    normalMode = NORMAL_MODE_AUTO;
    normalCount = 0;
  }


  /**
   * Sets the current normal vector.
   * <P/>
   * This is for drawing three dimensional shapes and surfaces,
   * allowing you to specify a vector perpendicular to the surface
   * of the shape, which determines how lighting affects it.
   * <P/>
   * For the most part, PGraphics3D will attempt to automatically
   * assign normals to shapes, but since that's imperfect,
   * this is a better option when you want more control.
   * <P/>
   * For people familiar with OpenGL, this function is basically
   * identical to glNormal3f().
   * <P/>
   * Only applies inside a beginShape/endShape block.
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
        normalMode = NORMAL_MODE_SHAPE;
      } else {
        // a separate normal for each vertex
        normalMode = NORMAL_MODE_VERTEX;
      }
    }
  }


  public void texture(PImage image) {
    textureImage = image;

    if (textureIndex == textures.length - 1) {
      textures = (PImage[]) PApplet.expand(textures);
    }
    if (textures[textureIndex] != null) {  // ???
      textureIndex++;
    }
    textures[textureIndex] = image;
  }


  public void vertex(float x, float y) {
    setup_vertex(x, y, 0);
  }


  public void vertex(float x, float y, float u, float v) {
    textureVertex(u, v);
    setup_vertex(x, y, 0);
  }


  public void vertex(float x, float y, float z) {
    setup_vertex(x, y, z);
  }


  public void vertex(float x, float y, float z,
                     float u, float v) {
    textureVertex(u, v);
    setup_vertex(x, y, z);
  }


  protected void setup_vertex(float x, float y, float z) {
    if (vertexCount == vertices.length) {
      float temp[][] = new float[vertexCount << 1][VERTEX_FIELD_COUNT];
      System.arraycopy(vertices, 0, temp, 0, vertexCount);
      vertices = temp;
      int temp2[] = new int[vertexCount << 1];
      System.arraycopy(vertex_order, 0, temp2, 0, vertexCount);
      vertex_order = temp2;
      //System.out.println("allocating more vertices " + vertices.length);
    }
    float vertex[] = vertices[vertexCount];

    // only do this if we're using an irregular (POLYGON) shape that
    // will go through the triangulator. otherwise it'll do thinks like
    // disappear in mathematically odd ways
    // http://dev.processing.org/bugs/show_bug.cgi?id=444
    if (shape == POLYGON) {
      if (vertexCount > 0) {
        float pvertex[] = vertices[vertexCount-1];
        if ((abs(pvertex[X] - x) < EPSILON) &&
            (abs(pvertex[Y] - y) < EPSILON) &&
            (abs(pvertex[Z] - z) < EPSILON)) {
          // this vertex is identical, don't add it,
          // because it will anger the triangulator
          return;
        }
      }
    }

    // user called vertex(), so that invalidates anything queued
    // up for curve vertices. if this is internally called by
    // spline_segment, then splineVertexCount will be saved and restored.
    splineVertexCount = 0;

    vertex[X] = x;
    vertex[Y] = y;
    vertex[Z] = z;

    if (fill) {
      if (textureImage != null) {
        if (tint) {
          vertex[R] = tintR;
          vertex[G] = tintG;
          vertex[B] = tintB;
          vertex[A] = tintA;
        } else {
          vertex[R] = 1;
          vertex[G] = 1;
          vertex[B] = 1;
          vertex[A] = 1;
        }
      } else {
        vertex[R] = fillR;
        vertex[G] = fillG;
        vertex[B] = fillB;
        vertex[A] = fillA;
      }

      vertex[AR] = ambientR;
      vertex[AG] = ambientG;
      vertex[AB] = ambientB;

      vertex[SPR] = specularR;
      vertex[SPG] = specularG;
      vertex[SPB] = specularB;
      //vertex[SPA] = specularA;

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

    vertex[BEEN_LIT] = 0;

    vertexCount++;
  }


  public void endShape(int mode) {
    vertex_end = vertexCount;
    vertex_end_including_clip_verts = vertex_end;

    // don't try to draw if there are no vertices
    // (fixes a bug in LINE_LOOP that re-adds a nonexistent vertex)
    if (vertexCount == 0) {
      shape = 0;
      return;
    }


    // ------------------------------------------------------------------
    // 2D or 3D POINTS FROM MODEL (MX, MY, MZ) TO CAMERA SPACE (VX, VY, VZ)
    // It is necessary to do this now because we will be clipping them on
    // add_triangle.

    for (int i = vertex_start; i < vertex_end; i++) {
      float vertex[] = vertices[i];

      vertex[VX] =
        modelview.m00*vertex[X] + modelview.m01*vertex[Y] +
        modelview.m02*vertex[Z] + modelview.m03;
      vertex[VY] =
        modelview.m10*vertex[X] + modelview.m11*vertex[Y] +
        modelview.m12*vertex[Z] + modelview.m13;
      vertex[VZ] =
        modelview.m20*vertex[X] + modelview.m21*vertex[Y] +
        modelview.m22*vertex[Z] + modelview.m23;
      vertex[VW] =
        modelview.m30*vertex[X] + modelview.m31*vertex[Y] +
        modelview.m32*vertex[Z] + modelview.m33;

      // normalize
      if (vertex[VW] != 0 && vertex[VW] != 1) {
        vertex[VX] /= vertex[VW];
        vertex[VY] /= vertex[VW];
        vertex[VZ] /= vertex[VW];
      }
      vertex[VW] = 1;
    }

    // ------------------------------------------------------------------
    // CREATE LINES

    int increment = 1;
    int stop = 0;
    //int counter = 0;

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
        //case LINE_STRIP:
        //case LINE_LOOP:
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
          //if (shape == LINE_LOOP) {
          if (mode == CLOSE) {
            add_line(stop, lines[first][VERTEX1]);
          }
        }
        break;

        case TRIANGLES:
        {
          for (int i = vertex_start; i < vertex_end-2; i += 3) {
            add_path();
            //counter = i - vertex_start;
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
            //counter = i - vertex_start;
            add_line(i, i+1);
          }

          // then draw from vertex (n) to (n+2)
          stop = vertex_end-2;
          for (int i = vertex_start; i < stop; i++) {
            add_path();
            add_line(i, i+2);
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
            //counter = i - vertex_start;
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
          //int first = lineCount;
          stop = vertex_end - 1;

          add_path();
          for (int i = vertex_start; i < stop; i++) {
            add_line(i, i+1);
            //System.out.println("adding line " + i);
          }
          if (mode == CLOSE) {
            // draw the last line connecting back to the first point in poly
            add_line(stop, vertex_start); //lines[first][VERTEX1]);
          }
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
    // TRANSFORM / LIGHT / CLIP

    if (lightCount > 0 && fill) {
      handle_lighting();
    }
    else {
      handle_no_lighting();
    }


    // ------------------------------------------------------------------
    // POINTS FROM CAMERA SPACE (VX, VY, VZ) TO SCREEN SPACE (X, Y, Z)
    // this appears to be wasted time with the opengl renderer

    for (int i = vertex_start; i < vertex_end_including_clip_verts; i++) {
      float vx[] = vertices[i];

      float ox =
        projection.m00*vx[VX] + projection.m01*vx[VY] +
        projection.m02*vx[VZ] + projection.m03*vx[VW];
      float oy =
        projection.m10*vx[VX] + projection.m11*vx[VY] +
        projection.m12*vx[VZ] + projection.m13*vx[VW];
      float oz =
        projection.m20*vx[VX] + projection.m21*vx[VY] +
        projection.m22*vx[VZ] + projection.m23*vx[VW];
      float ow =
        projection.m30*vx[VX] + projection.m31*vx[VY] +
        projection.m32*vx[VZ] + projection.m33*vx[VW];

      if (ow != 0 && ow != 1) {
        ox /= ow; oy /= ow; oz /= ow;
      }

      vx[TX] = width * (1 + ox) / 2.0f;
      vx[TY] = height * (1 + oy) / 2.0f;
      vx[TZ] = (oz + 1) / 2.0f;
    }


    // ------------------------------------------------------------------
    // RENDER SHAPES FILLS HERE WHEN NOT DEPTH SORTING

    // if true, the shapes will be rendered on endDraw
    if (!hints[ENABLE_DEPTH_SORT]) {
      if (fill) renderTriangles();
      if (stroke) renderLines();
    }

    shape = 0;
  }

  
  //////////////////////////////////////////////////////////////

  // STYLE
  
  
//  public void style(Style s) {
//    super.style(s);
//
//    // Set the colorMode() for the material properties. 
//    // TODO this is really inefficient, need to just have a material() method, 
//    // but this has the least impact to the API.
//    colorMode(RGB, 1);
//    ambient(s.ambientR, s.ambientG, s.ambientB);
//    emissive(s.emissiveR, s.emissiveG, s.emissiveB);
//    specular(s.specularR, s.specularG, s.specularB);
//    shininess(s.shininess);
//    
//    // Reset the colorMode again.
//    colorMode(s.colorMode, 
//              s.colorModeX, s.colorModeY, s.colorModeZ, s.colorModeA);
//  }

  

  //////////////////////////////////////////////////////////////

  
  // begin a new section of stroked geometry
  protected final void add_path() {
    if (pathCount == pathOffset.length) {
//      int temp1[] = new int[pathCount << 1];
//      System.arraycopy(pathOffset, 0, temp1, 0, pathCount);
//      pathOffset = temp1;
//      int temp2[] = new int[pathCount << 1];
//      System.arraycopy(pathLength, 0, temp2, 0, pathCount);
//      pathLength = temp2;
      pathOffset = PApplet.expand(pathOffset);
      pathLength = PApplet.expand(pathLength);
    }
    pathOffset[pathCount] = lineCount;
    pathLength[pathCount] = 0;
    pathCount++;
  }


  protected void add_line(int a, int b) {
    add_line_with_clip(a, b);
  }

  protected final void add_line_with_clip(int a, int b) {
    float az = vertices[a][VZ];
    float bz = vertices[b][VZ];
    if (az > cameraNear) {
      if (bz > cameraNear) {
        return;
      }
      int cb = interpolate_clip_vertex(a, b);
      add_line_no_clip(cb, b);
      return;
    }
    else {
      if (bz <= cameraNear) {
        add_line_no_clip(a, b);
        return;
      }
      int cb = interpolate_clip_vertex(a, b);
      add_line_no_clip(a, cb);
      return;
    }
  }

  protected final void add_line_no_clip(int a, int b) {
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


  protected void add_triangle(int a, int b, int c) {
    add_triangle_with_clip(a, b, c);
    //add_triangle_no_clip(a, b, c);
  }

  protected final void add_triangle_with_clip(int a, int b, int c) {
    boolean aClipped = false;
    boolean bClipped = false;
    //boolean cClipped = false;
    int     clippedCount = 0;

    cameraNear = -8;
    if (vertices[a][VZ] > cameraNear) {
      aClipped = true;
      clippedCount++;
    }
    if (vertices[b][VZ] > cameraNear) {
      bClipped = true;
      clippedCount++;
    }
    if (vertices[c][VZ] > cameraNear) {
      //cClipped = true;
      clippedCount++;
    }
    if (clippedCount == 0) {
      add_triangle_no_clip(a, b, c);
      return;
    }
    else if (clippedCount == 3) {
      return;
    }
    //                                                          | .
    // In this case there is only one visible point.            |/|
    // So we'll have to make two new points on the clip line   <| |
    // and add that triangle instead.                           |\|
    //                                                          | .
    else if (clippedCount == 2) {
      //System.out.println("Clipped two");

      int ca, cb, cc, cd, ce;
      if (!aClipped) {
        ca = a;
        cb = b;
        cc = c;
      }
      else if (!bClipped) {
        ca = b;
        cb = a;
        cc = c;
      }
      else { //if (!cClipped) {
        ca = c;
        cb = b;
        cc = a;
      }

      cd = interpolate_clip_vertex(ca, cb);
      ce = interpolate_clip_vertex(ca, cc);
      add_triangle_no_clip(ca, cd, ce);
      return;
    }

    //                                                          . |
    // In this case there are two visible points.               |\|
    // So we'll have to make two new points on the clip line    | |>
    // and then add two new triangles.                          |/|
    //                                                          . |
    else { // (clippedCount == 1) {
      //System.out.println("Clipped one");
      int ca, cb, cc, cd, ce;
      if (aClipped) {
        //System.out.println("aClipped");
        ca = c;
        cb = b;
        cc = a;
      }
      else if (bClipped) {
        //System.out.println("bClipped");
        ca = a;
        cb = c;
        cc = b;
      }
      else { //if (cClipped) {
        //System.out.println("cClipped");
        ca = a;
        cb = b;
        cc = c;
      }

      cd = interpolate_clip_vertex(ca, cc);
      ce = interpolate_clip_vertex(cb, cc);
      add_triangle_no_clip(ca, cd, cb);
      //System.out.println("ca: " + ca + ", " + vertices[ca][VX] + ", " + vertices[ca][VY] + ", " + vertices[ca][VZ]);
      //System.out.println("cd: " + cd + ", " + vertices[cd][VX] + ", " + vertices[cd][VY] + ", " + vertices[cd][VZ]);
      //System.out.println("cb: " + cb + ", " + vertices[cb][VX] + ", " + vertices[cb][VY] + ", " + vertices[cb][VZ]);
      add_triangle_no_clip(cb, cd, ce);
      return;
    }
  }

  
  private final int interpolate_clip_vertex(int a, int b) {
    float[] va;
    float[] vb;
    // Set up va, vb such that va[VZ] >= vb[VZ]
    if (vertices[a][VZ] < vertices[b][VZ]) {
      va = vertices[b];
      vb = vertices[a];
    }
    else {
      va = vertices[a];
      vb = vertices[b];
    }
    float az = va[VZ];
    float bz = vb[VZ];

    float dz = az - bz;
    // If they have the same z, just use pt. a.
    if (dz == 0) {
      return a;
    }
    //float pa = (az - cameraNear) / dz;
    //float pb = (cameraNear - bz) / dz;
    float pa = (cameraNear - bz) / dz;
    float pb = 1 - pa;

    vertex(pa * va[X] + pb * vb[X],
           pa * va[Y] + pb * vb[Y],
           pa * va[Z] + pb * vb[Z]);
    int irv = vertexCount - 1;
    vertex_end_including_clip_verts++;
    
    float[] rv = vertices[irv];

    rv[TX] = pa * va[TX] + pb * vb[TX];
    rv[TY] = pa * va[TY] + pb * vb[TY];
    rv[TZ] = pa * va[TZ] + pb * vb[TZ];

    rv[VX] = pa * va[VX] + pb * vb[VX];
    rv[VY] = pa * va[VY] + pb * vb[VY];
    rv[VZ] = pa * va[VZ] + pb * vb[VZ];
    rv[VW] = pa * va[VW] + pb * vb[VW];

    rv[R] = pa * va[R] + pb * vb[R];
    rv[G] = pa * va[G] + pb * vb[G];
    rv[B] = pa * va[B] + pb * vb[B];
    rv[A] = pa * va[A] + pb * vb[A];

    rv[U] = pa * va[U] + pb * vb[U];
    rv[V] = pa * va[V] + pb * vb[V];

    rv[SR] = pa * va[SR] + pb * vb[SR];
    rv[SG] = pa * va[SG] + pb * vb[SG];
    rv[SB] = pa * va[SB] + pb * vb[SB];
    rv[SA] = pa * va[SA] + pb * vb[SA];

    rv[NX] = pa * va[NX] + pb * vb[NX];
    rv[NY] = pa * va[NY] + pb * vb[NY];
    rv[NZ] = pa * va[NZ] + pb * vb[NZ];

    rv[SW] = pa * va[SW] + pb * vb[SW];

    rv[AR] = pa * va[AR] + pb * vb[AR];
    rv[AG] = pa * va[AG] + pb * vb[AG];
    rv[AB] = pa * va[AB] + pb * vb[AB];

    rv[SPR] = pa * va[SPR] + pb * vb[SPR];
    rv[SPG] = pa * va[SPG] + pb * vb[SPG];
    rv[SPB] = pa * va[SPB] + pb * vb[SPB];
    //rv[SPA] = pa * va[SPA] + pb * vb[SPA];

    rv[ER] = pa * va[ER] + pb * vb[ER];
    rv[EG] = pa * va[EG] + pb * vb[EG];
    rv[EB] = pa * va[EB] + pb * vb[EB];

    rv[SHINE] = pa * va[SHINE] + pb * vb[SHINE];

    rv[BEEN_LIT] = 0;

    return irv;
  }


  protected final void add_triangle_no_clip(int a, int b, int c) {
    if (triangleCount == triangles.length) {
      int temp[][] = new int[triangleCount<<1][TRIANGLE_FIELD_COUNT];
      System.arraycopy(triangles, 0, temp, 0, triangleCount);
      triangles = temp;
      //message(CHATTER, "allocating more triangles " + triangles.length);
      float ftemp[][][] = new float[triangleCount<<1][3][TRI_COLOR_COUNT];
      System.arraycopy(triangleColors, 0, ftemp, 0, triangleCount);
      triangleColors = ftemp;
    }
    triangles[triangleCount][VERTEX1] = a;
    triangles[triangleCount][VERTEX2] = b;
    triangles[triangleCount][VERTEX3] = c;

    if (textureImage == null) {
      triangles[triangleCount][TEXTURE_INDEX] = -1;
    } else {
      triangles[triangleCount][TEXTURE_INDEX] = textureIndex;
    }

    triangles[triangleCount][INDEX] = shape_index;
    triangleCount++;
  }


  protected void sortTriangles() {
    //System.out.println("sorting " + triangleCount + " triangles");
    depth_sort_triangles_internal(0, triangleCount-1);
  }


  protected void depth_sort_triangles_internal(int i, int j) {
    int pivotIndex = (i+j)/2;
    depth_sort_triangles_swap(pivotIndex, j);
    int k = depth_sort_triangles_partition(i-1, j);
    depth_sort_triangles_swap(k, j);
    if ((k-i) > 1) depth_sort_triangles_internal(i, k-1);
    if ((j-k) > 1) depth_sort_triangles_internal(k+1, j);
  }


  protected int depth_sort_triangles_partition(int left, int right) {
    int pivot = right;
    do {
      while (depth_sort_triangles_compare(++left, pivot) < 0) { }
      while ((right != 0) &&
             (depth_sort_triangles_compare(--right, pivot) > 0)) { }
      depth_sort_triangles_swap(left, right);
    } while (left < right);
    depth_sort_triangles_swap(left, right);
    return left;
  }


  protected void depth_sort_triangles_swap(int a, int b) {
    int tempi[] = triangles[a];
    triangles[a] = triangles[b];
    triangles[b] = tempi;
    float tempf[][] = triangleColors[a];
    triangleColors[a] = triangleColors[b];
    triangleColors[b] = tempf;
  }


  protected float depth_sort_triangles_compare(int a, int b) {
    if (Float.isNaN(vertices[triangles[a][VERTEX1]][TZ]) ||
        Float.isNaN(vertices[triangles[a][VERTEX2]][TZ]) ||
        Float.isNaN(vertices[triangles[a][VERTEX3]][TZ]) ||
        Float.isNaN(vertices[triangles[b][VERTEX1]][TZ]) ||
        Float.isNaN(vertices[triangles[b][VERTEX2]][TZ]) ||
        Float.isNaN(vertices[triangles[b][VERTEX3]][TZ])) {
      System.err.println("NaN values in triangle");
    }
    return
      (vertices[triangles[b][VERTEX1]][TZ] +
       vertices[triangles[b][VERTEX2]][TZ] +
       vertices[triangles[b][VERTEX3]][TZ]) -
      (vertices[triangles[a][VERTEX1]][TZ] +
       vertices[triangles[a][VERTEX2]][TZ] +
       vertices[triangles[a][VERTEX3]][TZ]);
  }


  protected void renderTriangles() {
    if (raw != null) {
      raw.colorMode(RGB, 1);
      raw.noStroke();
      raw.beginShape(TRIANGLES);
    }

    for (int i = 0; i < triangleCount; i ++) {
      float a[] = vertices[triangles[i][VERTEX1]];
      float b[] = vertices[triangles[i][VERTEX2]];
      float c[] = vertices[triangles[i][VERTEX3]];
      int tex = triangles[i][TEXTURE_INDEX];
      int index = triangles[i][INDEX];

      // ewjordan: hack to 'fix' accuracy issues when drawing in 2d
      // see also render_lines() where similar hack is employed
      float shift = 0.15f;//was 0.49f
      boolean shifted = false;
      if (drawing2D() && (a[Z] == 0)) {
        shifted = true;
        a[TX] += shift;
        a[TY] += shift;
        a[VX] += shift*a[VW];
        a[VY] += shift*a[VW];
        b[TX] += shift;
        b[TY] += shift;
        b[VX] += shift*b[VW];
        b[VY] += shift*b[VW];
        c[TX] += shift;
        c[TY] += shift;
        c[VX] += shift*c[VW];
        c[VY] += shift*c[VW];
      }

      triangle.reset();

      // This is only true when not textured. 
      // We really should pass specular straight through to triangle rendering.
      float ar = clamp(triangleColors[i][0][TRI_DIFFUSE_R] + triangleColors[i][0][TRI_SPECULAR_R]);
      float ag = clamp(triangleColors[i][0][TRI_DIFFUSE_G] + triangleColors[i][0][TRI_SPECULAR_G]);
      float ab = clamp(triangleColors[i][0][TRI_DIFFUSE_B] + triangleColors[i][0][TRI_SPECULAR_B]);
      float br = clamp(triangleColors[i][1][TRI_DIFFUSE_R] + triangleColors[i][1][TRI_SPECULAR_R]);
      float bg = clamp(triangleColors[i][1][TRI_DIFFUSE_G] + triangleColors[i][1][TRI_SPECULAR_G]);
      float bb = clamp(triangleColors[i][1][TRI_DIFFUSE_B] + triangleColors[i][1][TRI_SPECULAR_B]);
      float cr = clamp(triangleColors[i][2][TRI_DIFFUSE_R] + triangleColors[i][2][TRI_SPECULAR_R]);
      float cg = clamp(triangleColors[i][2][TRI_DIFFUSE_G] + triangleColors[i][2][TRI_SPECULAR_G]);
      float cb = clamp(triangleColors[i][2][TRI_DIFFUSE_B] + triangleColors[i][2][TRI_SPECULAR_B]);

      if (tex > -1 && textures[tex] != null) {
        triangle.setTexture(textures[tex]);
        triangle.setUV(a[U], a[V], b[U], b[V], c[U], c[V]);
      }

      triangle.setIntensities(ar, ag, ab, a[A],
                              br, bg, bb, b[A],
                              cr, cg, cb, c[A]);

      triangle.setVertices(a[TX], a[TY], a[TZ],
                           b[TX], b[TY], b[TZ],
                           c[TX], c[TY], c[TZ]);

      // Need to pass camera-space coordinates to triangle renderer
      // in order to compute true texture coordinates, else skip it
      if (hints[ENABLE_ACCURATE_TEXTURES]){
        triangle.setCamVertices(a[VX], a[VY], a[VZ],
                                b[VX], b[VY], b[VZ],
                                c[VX], c[VY], c[VZ]);
      }

      triangle.setIndex(index);
      triangle.render();

      //System.out.println(i + " " + a[Z] + " " + b[Z] + " " + c[Z]);

      if (raw != null) {
        if (raw instanceof PGraphics3D) {
          if ((a[VW] != 0) && (b[VW] != 0) && (c[VW] != 0)) {
            raw.fill(ar, ag, ab, a[A]);
            raw.vertex(a[VX] / a[VW], a[VY] / a[VW], a[VZ] / a[VW]);
            raw.fill(br, bg, bb, b[A]);
            raw.vertex(b[VX] / b[VW], b[VY] / b[VW], b[VZ] / b[VW]);
            raw.fill(cr, cg, cb, c[A]);
            raw.vertex(c[VX] / c[VW], c[VY] / c[VW], c[VZ] / c[VW]);
          }
        } else {  // raw instanceof PGraphics2D
          raw.fill(ar, ag, ab, a[A]);
          raw.vertex(a[TX], a[TY]);
          raw.fill(br, bg, bb, b[A]);
          raw.vertex(b[TX], b[TY]);
          raw.fill(cr, cg, cb, c[A]);
          raw.vertex(c[TX], c[TY]);
        }
      }

      if (drawing2D() && shifted){
        a[TX] -= shift;
        a[TY] -= shift;
        a[VX] -= shift*a[VW];
        a[VY] -= shift*a[VW];
        b[TX] -= shift;
        b[TY] -= shift;
        b[VX] -= shift*b[VW];
        b[VY] -= shift*b[VW];
        c[TX] -= shift;
        c[TY] -= shift;
        c[VX] -= shift*c[VW];
        c[VY] -= shift*c[VW];
      }
    }
    triangleCount = 0;

    if (raw != null) {
      raw.endShape();
    }
  }


  protected void sortLines() {
  }


  protected void renderLines() {
    if (raw != null) {
      raw.colorMode(RGB, 1);
      raw.noFill();
      raw.beginShape(LINES);
    }

    for (int i = 0; i < lineCount; i ++) {
      float a[] = vertices[lines[i][VERTEX1]];
      float b[] = vertices[lines[i][VERTEX2]];
      int index = lines[i][INDEX];

      // 2D hack added by ewjordan 6/13/07
      // Offset coordinates by a little bit if drawing 2D graphics.
      // http://dev.processing.org/bugs/show_bug.cgi?id=95

      // This hack fixes a bug caused by numerical precision issues when
      // applying the 3D transformations to coordinates in the screen plane
      // that should actually not be altered under said transformations.
      // It will not be applied if any transformations other than translations
      // are active, nor should it apply in OpenGL mode (PGraphicsOpenGL
      // overrides render_lines(), so this should be fine).
      // This fix exposes a last-pixel bug in the lineClipCode() function
      // of PLine.java, so that fix must remain in place if this one is used.

      // Note: the "true" fix for this bug is to change the pixel coverage
      // model so that the threshold for display does not lie on an integer
      // boundary. Search "diamond exit rule" for info the OpenGL approach.

      if (drawing2D() && a[Z] == 0) {
        a[TX] += 0.01;
        a[TY] += 0.01;
        a[VX] += 0.01*a[VW];
        a[VY] += 0.01*a[VW];
        b[TX] += 0.01;
        b[TY] += 0.01;
        b[VX] += 0.01*b[VW];
        b[VY] += 0.01*b[VW];
      }
      // end 2d-hack

      line.reset();

      line.setIntensities(a[SR], a[SG], a[SB], a[SA],
                          b[SR], b[SG], b[SB], b[SA]);

      line.setVertices(a[TX], a[TY], a[TZ],
                       b[TX], b[TY], b[TZ]);

      if (raw != null) {
        if (raw instanceof PGraphics3D) {
          if ((a[VW] != 0) && (b[VW] != 0)) {
            raw.stroke(a[SR], a[SG], a[SB], a[SA]);
            raw.vertex(a[VX] / a[VW], a[VY] / a[VW], a[VZ] / a[VW]);
            raw.stroke(b[SR], b[SG], b[SB], b[SA]);
            raw.vertex(b[VX] / b[VW], b[VY] / b[VW], b[VZ] / b[VW]);
          }
        } else {
          raw.stroke(a[SR], a[SG], a[SB], a[SA]);
          raw.vertex(a[TX], a[TY]);
          raw.stroke(b[SR], b[SG], b[SB], b[SA]);
          raw.vertex(b[TX], b[TY]);
        }
      }

      /*
      // Seems okay to remove this because these vertices are not used again,
      // but if problems arise, this needs to be uncommented because the above
      // change is destructive and may need to be undone before proceeding.
      if (drawing2D() && a[MZ] == 0) {
        a[X] -= 0.01;
        a[Y] -= 0.01;
        a[VX] -= 0.01*a[VW];
        a[VY] -= 0.01*a[VW];
        b[X] -= 0.01;
        b[Y] -= 0.01;
        b[VX] -= 0.01*b[VW];
        b[VY] -= 0.01*b[VW];
      }
      */

      line.setIndex(index);
      line.draw();
    }

    if (raw != null) {
      raw.endShape();
    }
  }


  /**
   * Triangulate the current polygon.
   * <BR> <BR>
   * Simple ear clipping polygon triangulation adapted from code by
   * John W. Ratcliff (jratcliff at verant.com). Presumably
   * <A HREF="http://www.flipcode.org/cgi-bin/fcarticles.cgi?show=63943">this</A>
   * bit of code from the web.
   */
  private void triangulate_polygon() {
    // this clipping algorithm only works in 2D, so in cases where a
    // polygon is drawn perpendicular to the z-axis, the area will be zero,
    // and triangulation will fail. as such, when the area calculates to
    // zero, figure out whether x or y is empty, and calculate based on the
    // two dimensions that actually contain information.
    // http://dev.processing.org/bugs/show_bug.cgi?id=111
    int d1 = X;
    int d2 = Y;
    // this brings up the nastier point that there may be cases where
    // a polygon is irregular in space and will throw off the
    // clockwise/counterclockwise calculation. for instance, if clockwise
    // relative to x and z, but counter relative to y and z or something
    // like that.. will wait to see if this is in fact a problem before
    // hurting my head on the math.

    /*
    // trying to track down bug #774
    for (int i = vertex_start; i < vertex_end; i++) {
      if (i > vertex_start) {
        if (vertices[i-1][MX] == vertices[i][MX] &&
            vertices[i-1][MY] == vertices[i][MY]) {
          System.out.print("**** " );
        }
      }
      System.out.println(i + " " + vertices[i][MX] + " " + vertices[i][MY]);
    }
    System.out.println();
    */

    // first we check if the polygon goes clockwise or counterclockwise
    float area = 0;
    for (int p = vertex_end - 1, q = vertex_start; q < vertex_end; p = q++) {
      area += (vertices[q][d1] * vertices[p][d2] -
               vertices[p][d1] * vertices[q][d2]);
    }
    // rather than checking for the perpendicular case first, only do it
    // when the area calculates to zero. checking for perpendicular would be
    // a needless waste of time for the 99% case.
    if (area == 0) {
      // figure out which dimension is the perpendicular axis
      boolean foundValidX = false;
      boolean foundValidY = false;

      for (int i = vertex_start; i < vertex_end; i++) {
        for (int j = i; j < vertex_end; j++){
          if ( vertices[i][X] != vertices[j][X] ) foundValidX = true;
          if ( vertices[i][Y] != vertices[j][Y] ) foundValidY = true;
        }
      }

      if (foundValidX) {
        //d1 = MX;  // already the case
        d2 = Z;
      } else if (foundValidY) {
        // ermm.. which is the proper order for cw/ccw here?
        d1 = Y;
        d2 = Z;
      } else {
        // screw it, this polygon is just f-ed up
        return;
      }

      // re-calculate the area, with what should be good values
      for (int p = vertex_end - 1, q = vertex_start; q < vertex_end; p = q++) {
        area += (vertices[q][d1] * vertices[p][d2] -
                 vertices[p][d1] * vertices[q][d2]);
      }
    }

    // don't allow polygons to come back and meet themselves,
    // otherwise it will anger the triangulator
    // http://dev.processing.org/bugs/show_bug.cgi?id=97
    float vfirst[] = vertices[vertex_start];
    float vlast[] = vertices[vertex_end-1];
    if ((abs(vfirst[X] - vlast[X]) < EPSILON) &&
        (abs(vfirst[Y] - vlast[Y]) < EPSILON) &&
        (abs(vfirst[Z] - vlast[Z]) < EPSILON)) {
      vertex_end--;
    }

    // then sort the vertices so they are always in a counterclockwise order
    int j = 0;
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

      // Upgrade values to doubles, and multiply by 10 so that we can have
      // some better accuracy as we tessellate. This seems to have negligible
      // speed differences on Windows and Intel Macs, but causes a 50% speed
      // drop for PPC Macs with the bug's example code that draws ~200 points
      // in a concave polygon. Apple has abandoned PPC so we may as well too.
      // http://dev.processing.org/bugs/show_bug.cgi?id=774

      // triangle A B C
      double Ax = -10 * vertices[vertex_order[u]][d1];
      double Ay =  10 * vertices[vertex_order[u]][d2];
      double Bx = -10 * vertices[vertex_order[v]][d1];
      double By =  10 * vertices[vertex_order[v]][d2];
      double Cx = -10 * vertices[vertex_order[w]][d1];
      double Cy =  10 * vertices[vertex_order[w]][d2];

      // first we check if <u,v,w> continues going ccw
      if (EPSILON > (((Bx-Ax) * (Cy-Ay)) - ((By-Ay) * (Cx-Ax)))) {
        continue;
      }

      for (int p = 0; p < vc; p++) {
        if ((p == u) || (p == v) || (p == w)) {
          continue;
        }

        double Px = -10 * vertices[vertex_order[p]][d1];
        double Py =  10 * vertices[vertex_order[p]][d2];

        double ax  = Cx - Bx;  double ay  = Cy - By;
        double bx  = Ax - Cx;  double by  = Ay - Cy;
        double cx  = Bx - Ax;  double cy  = By - Ay;
        double apx = Px - Ax;  double apy = Py - Ay;
        double bpx = Px - Bx;  double bpy = Py - By;
        double cpx = Px - Cx;  double cpy = Py - Cy;

        double aCROSSbp = ax * bpy - ay * bpx;
        double cCROSSap = cx * apy - cy * apx;
        double bCROSScp = bx * cpy - by * cpx;

        if ((aCROSSbp >= 0.0) && (bCROSScp >= 0.0) && (cCROSSap >= 0.0)) {
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


  private void toWorldNormal(float nx, float ny, float nz, float[] out) {
    out[0] =
      modelviewInv.m00*nx + modelviewInv.m10*ny +
      modelviewInv.m20*nz + modelviewInv.m30;
    out[1] =
      modelviewInv.m01*nx + modelviewInv.m11*ny +
      modelviewInv.m21*nz + modelviewInv.m31;
    out[2] =
      modelviewInv.m02*nx + modelviewInv.m12*ny +
      modelviewInv.m22*nz + modelviewInv.m32;
    out[3] =
      modelviewInv.m03*nx + modelviewInv.m13*ny +
      modelviewInv.m23*nz + modelviewInv.m33;

    if (out[3] != 0 && out[3] != 1) {
      // divide by perspective coordinate
      out[0] /= out[3]; out[1] /= out[3]; out[2] /= out[3];
    }
    out[3] = 1;

    float nlen = mag(out[0], out[1], out[2]);  // normalize
    if (nlen != 0 && nlen != 1) {
      out[0] /= nlen; out[1] /= nlen; out[2] /= nlen;
    }
  }


  private void calc_lighting_contribution(int vIndex,
                                          float[] contribution) {
    calc_lighting_contribution(vIndex, contribution, false);
  }

  private void calc_lighting_contribution(int vIndex,
                                          float[] contribution,
                                          boolean normalIsWorld) {
    float[] v = vertices[vIndex];

    float sr = v[SPR];
    float sg = v[SPG];
    float sb = v[SPB];

    float wx = v[VX];
    float wy = v[VY];
    float wz = v[VZ];
    float shine = v[SHINE];

    float nx;
    float ny;
    float nz;
    if (!normalIsWorld) {
      toWorldNormal(v[NX], v[NY], v[NZ], worldNormal);
      nx = worldNormal[X];
      ny = worldNormal[Y];
      nz = worldNormal[Z];
    } else {
      nx = v[NX];
      ny = v[NY];
      nz = v[NZ];
    }


    // Since the camera space == world space,
    // we can test for visibility by the dot product of
    // the normal with the direction from pt. to eye.
    float dir = dot(nx, ny, nz, -wx, -wy, -wz);
    // If normal is away from camera, choose its opposite.
    // If we add backface culling, this will be backfacing
    // (but since this is per vertex, it's more complicated)
    if (dir < 0) {
      nx = -nx;
      ny = -ny;
      nz = -nz;
    }

    // These two terms will sum the contributions from the various lights
    contribution[LIGHT_AMBIENT_R] = 0;
    contribution[LIGHT_AMBIENT_G] = 0;
    contribution[LIGHT_AMBIENT_B] = 0;

    contribution[LIGHT_DIFFUSE_R] = 0;
    contribution[LIGHT_DIFFUSE_G] = 0;
    contribution[LIGHT_DIFFUSE_B] = 0;

    contribution[LIGHT_SPECULAR_R] = 0;
    contribution[LIGHT_SPECULAR_G] = 0;
    contribution[LIGHT_SPECULAR_B] = 0;

    // for (int i = 0; i < MAX_LIGHTS; i++) {
    // if (!light[i]) continue;
    for (int i = 0; i < lightCount; i++) {

      float denom = lightFalloffConstant[i];
      float spotTerm = 1;

      if (lightType[i] == AMBIENT) {
        if (lightFalloffQuadratic[i] != 0 || lightFalloffLinear[i] != 0) {
          // Falloff depends on distance
          float distSq = mag(lightPosition[i][0] - wx,
                             lightPosition[i][1] - wy,
                             lightPosition[i][2] - wz);
          denom +=
            lightFalloffQuadratic[i] * distSq +
            lightFalloffLinear[i] * (float) sqrt(distSq);
        }
        if (denom == 0) denom = 1;

        contribution[LIGHT_AMBIENT_R] += lightDiffuse[i][0] / denom;
        contribution[LIGHT_AMBIENT_G] += lightDiffuse[i][1] / denom;
        contribution[LIGHT_AMBIENT_B] += lightDiffuse[i][2] / denom;

      } else {
        // If not ambient, we must deal with direction

        // li is the vector from the vertex to the light
        float lix, liy, liz;
        float lightDir_dot_li = 0;
        float n_dot_li = 0;

        if (lightType[i] == DIRECTIONAL) {
          lix = -lightNormal[i][0];
          liy = -lightNormal[i][1];
          liz = -lightNormal[i][2];
          denom = 1;
          n_dot_li = (nx * lix + ny * liy + nz * liz);
          // If light is lighting the face away from the camera, ditch
          if (n_dot_li <= 0) {
            continue;
          }
        } else { // Point or spot light (must deal also with light location)
          lix = lightPosition[i][0] - wx;
          liy = lightPosition[i][1] - wy;
          liz = lightPosition[i][2] - wz;
          // normalize
          float distSq = mag(lix, liy, liz);
          if (distSq != 0) {
            lix /= distSq;
            liy /= distSq;
            liz /= distSq;
          }
          n_dot_li = (nx * lix + ny * liy + nz * liz);
          // If light is lighting the face away from the camera, ditch
          if (n_dot_li <= 0) {
            continue;
          }

          if (lightType[i] == SPOT) { // Must deal with spot cone
            lightDir_dot_li =
              -(lightNormal[i][0] * lix +
                lightNormal[i][1] * liy +
                lightNormal[i][2] * liz);
            // Outside of spot cone
            if (lightDir_dot_li <= lightSpotAngleCos[i]) {
              continue;
            }
            spotTerm = (float) Math.pow(lightDir_dot_li, lightSpotConcentration[i]);
          }

          if (lightFalloffQuadratic[i] != 0 || lightFalloffLinear[i] != 0) {
            // Falloff depends on distance
            denom +=
              lightFalloffQuadratic[i] * distSq +
              lightFalloffLinear[i] * (float) sqrt(distSq);
          }
        }
        // Directional, point, or spot light:

        // We know n_dot_li > 0 from above "continues"

        if (denom == 0)
          denom = 1;
        float mul = n_dot_li * spotTerm / denom;
        contribution[LIGHT_DIFFUSE_R] += lightDiffuse[i][0] * mul;
        contribution[LIGHT_DIFFUSE_G] += lightDiffuse[i][1] * mul;
        contribution[LIGHT_DIFFUSE_B] += lightDiffuse[i][2] * mul;

        // SPECULAR

        // If the material and light have a specular component.
        if ((sr > 0 || sg > 0 || sb > 0) &&
            (lightSpecular[i][0] > 0 ||
             lightSpecular[i][1] > 0 ||
             lightSpecular[i][2] > 0)) {

          float vmag = mag(wx, wy, wz);
          if (vmag != 0) {
            wx /= vmag;
            wy /= vmag;
            wz /= vmag;
          }
          float sx = lix - wx;
          float sy = liy - wy;
          float sz = liz - wz;
          vmag = mag(sx, sy, sz);
          if (vmag != 0) {
            sx /= vmag;
            sy /= vmag;
            sz /= vmag;
          }
          float s_dot_n = (sx * nx + sy * ny + sz * nz);

          if (s_dot_n > 0) {
            s_dot_n = (float) Math.pow(s_dot_n, shine);
            mul = s_dot_n * spotTerm / denom;
            contribution[LIGHT_SPECULAR_R] += lightSpecular[i][0] * mul;
            contribution[LIGHT_SPECULAR_G] += lightSpecular[i][1] * mul;
            contribution[LIGHT_SPECULAR_B] += lightSpecular[i][2] * mul;
          }

        }
      }
    }
    return;
  }


  // Multiply the lighting contribution into the vertex's colors.
  // Only do this when there is ONE lighting per vertex
  // (MANUAL_VERTEX_NORMAL or SHAPE_NORMAL mode).
  private void apply_lighting_contribution(int vIndex, float[] contribution) {
    float[] v = vertices[vIndex];

    v[R] = clamp(v[ER] + v[AR] * contribution[LIGHT_AMBIENT_R] + v[DR] * contribution[LIGHT_DIFFUSE_R]);
    v[G] = clamp(v[EG] + v[AG] * contribution[LIGHT_AMBIENT_G] + v[DG] * contribution[LIGHT_DIFFUSE_G]);
    v[B] = clamp(v[EB] + v[AB] * contribution[LIGHT_AMBIENT_B] + v[DB] * contribution[LIGHT_DIFFUSE_B]);
    v[A] = clamp(v[DA]);

    v[SPR] = clamp(v[SPR] * contribution[LIGHT_SPECULAR_R]);
    v[SPG] = clamp(v[SPG] * contribution[LIGHT_SPECULAR_G]);
    v[SPB] = clamp(v[SPB] * contribution[LIGHT_SPECULAR_B]);
    //v[SPA] = min(1, v[SPA]);

    v[BEEN_LIT] = 1;
  }


  private void light_vertex_always(int vIndex, float[] contribution) {
    calc_lighting_contribution(vIndex, contribution);
    apply_lighting_contribution(vIndex, contribution);
  }


  private void light_vertex_if_not_already_lit(int vIndex,
                                               float[] contribution) {
    if (vertices[vIndex][BEEN_LIT] == 0) {
      light_vertex_always(vIndex, contribution);
    }
  }


  private void copy_prelit_vertex_color_to_triangle(int triIndex, int vIndex,
                                                    int colorIndex) {
    float[] triColor = triangleColors[triIndex][colorIndex];
    float[] v = vertices[vIndex];

    triColor[TRI_DIFFUSE_R] = v[R];
    triColor[TRI_DIFFUSE_G] = v[G];
    triColor[TRI_DIFFUSE_B] = v[B];
    triColor[TRI_DIFFUSE_A] = v[A];
    triColor[TRI_SPECULAR_R] = v[SPR];
    triColor[TRI_SPECULAR_G] = v[SPG];
    triColor[TRI_SPECULAR_B] = v[SPB];
    //triColor[TRI_SPECULAR_A] = v[SPA];
  }


  private void copy_vertex_color_to_triangle(int triIndex,
                                             int vIndex, int colorIndex,
                                             float[] lightContribution) {
    float[] triColor = triangleColors[triIndex][colorIndex];
    float[] v = vertices[vIndex];

    triColor[TRI_DIFFUSE_R] =
      clamp(v[ER] + v[AR] * lightContribution[LIGHT_AMBIENT_R] +
             v[DR] * lightContribution[LIGHT_DIFFUSE_R]);
    triColor[TRI_DIFFUSE_G] =
      clamp(v[EG] + v[AG] * lightContribution[LIGHT_AMBIENT_G] +
             v[DG] * lightContribution[LIGHT_DIFFUSE_G]);
    triColor[TRI_DIFFUSE_B] =
      clamp(v[EB] + v[AB] * lightContribution[LIGHT_AMBIENT_B] +
             v[DB] * lightContribution[LIGHT_DIFFUSE_B]);
    triColor[TRI_DIFFUSE_A] = clamp(v[DA]);

    triColor[TRI_SPECULAR_R] = clamp(v[SPR] * lightContribution[LIGHT_SPECULAR_R]);
    triColor[TRI_SPECULAR_G] = clamp(v[SPG] * lightContribution[LIGHT_SPECULAR_G]);
    triColor[TRI_SPECULAR_B] = clamp(v[SPB] * lightContribution[LIGHT_SPECULAR_B]);
  }


  private void light_triangle(int triIndex, float[] lightContribution) {
    int vIndex = triangles[triIndex][VERTEX1];
    copy_vertex_color_to_triangle(triIndex, vIndex, 0, lightContribution);
    vIndex = triangles[triIndex][VERTEX2];
    copy_vertex_color_to_triangle(triIndex, vIndex, 1, lightContribution);
    vIndex = triangles[triIndex][VERTEX3];
    copy_vertex_color_to_triangle(triIndex, vIndex, 2, lightContribution);
  }


  private void light_triangle(int triIndex) {
    int vIndex;

    // Handle lighting on, but no lights (in this case, just use emissive)
    // This wont be used currently because lightCount == 0 is don't use
    // lighting at all... So. OK. If that ever changes, use the below:
    /*
    if (lightCount == 0) {
      vIndex = triangles[triIndex][VERTEX1];
      copy_emissive_vertex_color_to_triangle(triIndex, vIndex, 0);
      vIndex = triangles[triIndex][VERTEX2];
      copy_emissive_vertex_color_to_triangle(triIndex, vIndex, 1);
      vIndex = triangles[triIndex][VERTEX3];
      copy_emissive_vertex_color_to_triangle(triIndex, vIndex, 2);
      return;
    }
    */

    // In MANUAL_VERTEX_NORMAL mode, we have a specific normal
    // for each vertex. In that case, we light any verts that
    // haven't already been lit and copy their colors straight
    // into the triangle.
    if (normalMode == NORMAL_MODE_VERTEX) {
      vIndex = triangles[triIndex][VERTEX1];
      light_vertex_if_not_already_lit(vIndex, tempLightingContribution);
      copy_prelit_vertex_color_to_triangle(triIndex, vIndex, 0);

      vIndex = triangles[triIndex][VERTEX2];
      light_vertex_if_not_already_lit(vIndex, tempLightingContribution);
      copy_prelit_vertex_color_to_triangle(triIndex, vIndex, 1);

      vIndex = triangles[triIndex][VERTEX3];
      light_vertex_if_not_already_lit(vIndex, tempLightingContribution);
      copy_prelit_vertex_color_to_triangle(triIndex, vIndex, 2);

    }

    // If the lighting doesn't depend on the vertex position, do the
    // following: We've already dealt with NORMAL_MODE_SHAPE mode before
    // we got into this function, so here we only have to deal with
    // NORMAL_MODE_AUTO. So we calculate the normal for this triangle,
    // and use that for the lighting.
    else if (!lightingDependsOnVertexPosition) {
      vIndex = triangles[triIndex][VERTEX1];
      int vIndex2 = triangles[triIndex][VERTEX2];
      int vIndex3 = triangles[triIndex][VERTEX3];

      /*
      dv1[0] = vertices[vIndex2][VX] - vertices[vIndex][VX];
      dv1[1] = vertices[vIndex2][VY] - vertices[vIndex][VY];
      dv1[2] = vertices[vIndex2][VZ] - vertices[vIndex][VZ];

      dv2[0] = vertices[vIndex3][VX] - vertices[vIndex][VX];
      dv2[1] = vertices[vIndex3][VY] - vertices[vIndex][VY];
      dv2[2] = vertices[vIndex3][VZ] - vertices[vIndex][VZ];

      cross(dv1, dv2, norm);
      */
      
      cross(vertices[vIndex2][VX] - vertices[vIndex][VX], 
            vertices[vIndex2][VY] - vertices[vIndex][VY], 
            vertices[vIndex2][VZ] - vertices[vIndex][VZ],
            vertices[vIndex3][VX] - vertices[vIndex][VX],
            vertices[vIndex3][VY] - vertices[vIndex][VY],
            vertices[vIndex3][VZ] - vertices[vIndex][VZ], norm);
      
      float nmag = mag(norm[X], norm[Y], norm[Z]);
      if (nmag != 0 && nmag != 1) {
        norm[X] /= nmag; norm[Y] /= nmag; norm[Z] /= nmag;
      }
      vertices[vIndex][NX] = norm[X];
      vertices[vIndex][NY] = norm[Y];
      vertices[vIndex][NZ] = norm[Z];

      // The true at the end says the normal is already in world coordinates
      calc_lighting_contribution(vIndex, tempLightingContribution, true);
      copy_vertex_color_to_triangle(triIndex, vIndex, 0,
                                    tempLightingContribution);
      copy_vertex_color_to_triangle(triIndex, vIndex2, 1,
                                    tempLightingContribution);
      copy_vertex_color_to_triangle(triIndex, vIndex3, 2,
                                    tempLightingContribution);
    }

    // If lighting is position-dependent
    else {
      if (normalMode == NORMAL_MODE_SHAPE) {
        vIndex = triangles[triIndex][VERTEX1];
        vertices[vIndex][NX] = vertices[vertex_start][NX];
        vertices[vIndex][NY] = vertices[vertex_start][NY];
        vertices[vIndex][NZ] = vertices[vertex_start][NZ];
        calc_lighting_contribution(vIndex, tempLightingContribution);
        copy_vertex_color_to_triangle(triIndex, vIndex, 0,
                                      tempLightingContribution);

        vIndex = triangles[triIndex][VERTEX2];
        vertices[vIndex][NX] = vertices[vertex_start][NX];
        vertices[vIndex][NY] = vertices[vertex_start][NY];
        vertices[vIndex][NZ] = vertices[vertex_start][NZ];
        calc_lighting_contribution(vIndex, tempLightingContribution);
        copy_vertex_color_to_triangle(triIndex, vIndex, 1,
                                      tempLightingContribution);

        vIndex = triangles[triIndex][VERTEX3];
        vertices[vIndex][NX] = vertices[vertex_start][NX];
        vertices[vIndex][NY] = vertices[vertex_start][NY];
        vertices[vIndex][NZ] = vertices[vertex_start][NZ];
        calc_lighting_contribution(vIndex, tempLightingContribution);
        copy_vertex_color_to_triangle(triIndex, vIndex, 2,
                                      tempLightingContribution);
      }

      // lighting mode is AUTO_NORMAL
      else {
        vIndex = triangles[triIndex][VERTEX1];
        int vIndex2 = triangles[triIndex][VERTEX2];
        int vIndex3 = triangles[triIndex][VERTEX3];
        
        /*
        dv1[0] = vertices[vIndex2][VX] - vertices[vIndex][VX];
        dv1[1] = vertices[vIndex2][VY] - vertices[vIndex][VY];
        dv1[2] = vertices[vIndex2][VZ] - vertices[vIndex][VZ];
        
        dv2[0] = vertices[vIndex3][VX] - vertices[vIndex][VX];
        dv2[1] = vertices[vIndex3][VY] - vertices[vIndex][VY];
        dv2[2] = vertices[vIndex3][VZ] - vertices[vIndex][VZ];

        cross(dv1, dv2, norm);
        */
        
        cross(vertices[vIndex2][VX] - vertices[vIndex][VX],
              vertices[vIndex2][VY] - vertices[vIndex][VY],
              vertices[vIndex2][VZ] - vertices[vIndex][VZ],
              vertices[vIndex3][VX] - vertices[vIndex][VX],
              vertices[vIndex3][VY] - vertices[vIndex][VY],
              vertices[vIndex3][VZ] - vertices[vIndex][VZ], norm);
        float nmag = mag(norm[X], norm[Y], norm[Z]);
        if (nmag != 0 && nmag != 1) {
          norm[X] /= nmag; norm[Y] /= nmag; norm[Z] /= nmag;
        }
        vertices[vIndex][NX] = norm[X];
        vertices[vIndex][NY] = norm[Y];
        vertices[vIndex][NZ] = norm[Z];
        // The true at the end says the normal is already in world coordinates
        calc_lighting_contribution(vIndex, tempLightingContribution, true);
        copy_vertex_color_to_triangle(triIndex, vIndex, 0,
                                      tempLightingContribution);

        vertices[vIndex2][NX] = norm[X];
        vertices[vIndex2][NY] = norm[Y];
        vertices[vIndex2][NZ] = norm[Z];
        // The true at the end says the normal is already in world coordinates
        calc_lighting_contribution(vIndex2, tempLightingContribution, true);
        copy_vertex_color_to_triangle(triIndex, vIndex2, 1,
                                      tempLightingContribution);

        vertices[vIndex3][NX] = norm[X];
        vertices[vIndex3][NY] = norm[Y];
        vertices[vIndex3][NZ] = norm[Z];
        // The true at the end says the normal is already in world coordinates
        calc_lighting_contribution(vIndex3, tempLightingContribution, true);
        copy_vertex_color_to_triangle(triIndex, vIndex3, 2,
                                      tempLightingContribution);
      }
    }
  }


  protected void handle_lighting() {
    // If the lighting does not depend on vertex position and there is a single
    // normal specified for this shape, go ahead and apply the same lighting
    // contribution to every vertex in this shape (one lighting calc!)
    if (!lightingDependsOnVertexPosition && normalMode == NORMAL_MODE_SHAPE) {
      calc_lighting_contribution(vertex_start, tempLightingContribution);
      for (int tri = 0; tri < triangleCount; tri++) {
        light_triangle(tri, tempLightingContribution);
      }
    }
    // Otherwise light each triangle individually...
    else {
      for (int tri = 0; tri < triangleCount; tri++) {
        light_triangle(tri);
      }
    }
  }


  protected void handle_no_lighting() {
    int vIndex;
    for (int tri = 0; tri < triangleCount; tri++) {
      vIndex = triangles[tri][VERTEX1];
      copy_prelit_vertex_color_to_triangle(tri, vIndex, 0);
      vIndex = triangles[tri][VERTEX2];
      copy_prelit_vertex_color_to_triangle(tri, vIndex, 1);
      vIndex = triangles[tri][VERTEX3];
      copy_prelit_vertex_color_to_triangle(tri, vIndex, 2);
    }
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


  /**
   * Compared to the implementation in PGraphics, this adds normal().
   */
  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    beginShape(TRIANGLES);
    normal(0, 0, 1);
    vertex(x1, y1);
    vertex(x2, y2);
    vertex(x3, y3);
    endShape();
  }


  /**
   * Compared to the implementation in PGraphics, this adds normal().
   */
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

  // BOX


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

  // SPHERE


  public void sphereDetail(int res) {
    sphereDetail(res, res);
  }


  /**
   * Set the detail level for approximating a sphere. The ures and vres params
   * control the horizontal and vertical resolution.
   *
   * Code for sphereDetail() submitted by toxi [031031].
   * Code for enhanced u/v version from davbol [080801].
   */
  public void sphereDetail(int ures, int vres) {
    if (ures < 3) ures = 3; // force a minimum res
    if (vres < 2) vres = 2; // force a minimum res
    if ((ures == sphereDetailU) && (vres == sphereDetailV)) return;

    float delta = (float)SINCOS_LENGTH/ures;
    float[] cx = new float[ures];
    float[] cz = new float[ures];
    // calc unit circle in XZ plane
    for (int i = 0; i < ures; i++) {
      cx[i] = cosLUT[(int) (i*delta) % SINCOS_LENGTH];
      cz[i] = sinLUT[(int) (i*delta) % SINCOS_LENGTH];
    }
    // computing vertexlist
    // vertexlist starts at south pole
    int vertCount = ures * (vres-1) + 2;
    int currVert = 0;

    // re-init arrays to store vertices
    sphereX = new float[vertCount];
    sphereY = new float[vertCount];
    sphereZ = new float[vertCount];

    float angle_step = (SINCOS_LENGTH*0.5f)/vres;
    float angle = angle_step;

    // step along Y axis
    for (int i = 1; i < vres; i++) {
      float curradius = sinLUT[(int) angle % SINCOS_LENGTH];
      float currY = -cosLUT[(int) angle % SINCOS_LENGTH];
      for (int j = 0; j < ures; j++) {
        sphereX[currVert] = cx[j] * curradius;
        sphereY[currVert] = currY;
        sphereZ[currVert++] = cz[j] * curradius;
      }
      angle += angle_step;
    }
    sphereDetailU = ures;
    sphereDetailV = vres;
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
   * [toxi 031031] new sphere code. removed all multiplies with
   * radius, as scale() will take care of that anyway
   *
   * [toxi 031223] updated sphere code (removed modulos)
   * and introduced sphereAt(x,y,z,r)
   * to avoid additional translate()'s on the user/sketch side
   *
   * [davbol 080801] now using separate sphereDetailU/V
   * </PRE>
   */
  public void sphere(float r) {
    float x = 0;  // TODO clean this back up again
    float y = 0;
    float z = 0;

    if ((sphereDetailU < 3) || (sphereDetailV < 2)) {
      sphereDetail(30);
    }

    int v1,v11,v2;
    pushMatrix();
    if (x != 0f && y != 0f && z != 0f) translate(x,y,z);
    scale(r);

    if (triangle != null) {  // triangle is null in gl
      triangle.setCulling(true);
    }

    // 1st ring from south pole
    beginShape(TRIANGLE_STRIP);
    for (int i = 0; i < sphereDetailU; i++) {
      normal(0, -1, 0);
      vertex(0, -1, 0);
      normal(sphereX[i], sphereY[i], sphereZ[i]);
      vertex(sphereX[i], sphereY[i], sphereZ[i]);
    }
    //normal(0, -1, 0);
    vertex(0, -1, 0);
    normal(sphereX[0], sphereY[0], sphereZ[0]);
    vertex(sphereX[0], sphereY[0], sphereZ[0]);
    endShape();

    // middle rings
    int voff = 0;
    for(int i = 2; i < sphereDetailV; i++) {
      v1 = v11 = voff;
      voff += sphereDetailU;
      v2 = voff;
      beginShape(TRIANGLE_STRIP);
      for (int j = 0; j < sphereDetailU; j++) {
        normal(sphereX[v1], sphereY[v1], sphereZ[v1]);
        vertex(sphereX[v1], sphereY[v1], sphereZ[v1++]);
        normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
        vertex(sphereX[v2], sphereY[v2], sphereZ[v2++]);
      }
      // close each ring
      v1 = v11;
      v2 = voff;
      normal(sphereX[v1], sphereY[v1], sphereZ[v1]);
      vertex(sphereX[v1], sphereY[v1], sphereZ[v1]);
      normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
      vertex(sphereX[v2], sphereY[v2], sphereZ[v2]);
      endShape();
    }

    // add the northern cap
    beginShape(TRIANGLE_STRIP);
    for (int i = 0; i < sphereDetailU; i++) {
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
    popMatrix();

    if (triangle != null) {  // triangle is null in gl
      triangle.setCulling(false);
    }
  }



  //////////////////////////////////////////////////////////////

  // CURVES

  /*

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
    vertex(x1, y1, z1);
    bezierVertex(x2, y2, z2,
                 x3, y3, z3,
                 x4, y4, z4);
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

  */



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


  public void rotateX(float angle) {
    forwardTransform.rotateX(angle);
    reverseTransform.invRotateX(angle);
  }


  public void rotateY(float angle) {
    forwardTransform.rotateY(angle);
    reverseTransform.invRotateY(angle);
  }


  public void rotateZ(float angle) {
    forwardTransform.rotateZ(angle);
    reverseTransform.invRotateZ(angle);
  }


  /**
   * Rotate around an arbitrary vector, similar to glRotate(),
   * except that it takes radians (instead of degrees).
   */
  public void rotate(float angle, float v0, float v1, float v2) {
    forwardTransform.rotate(angle, v0, v1, v2);
    reverseTransform.invRotate(angle, v0, v1, v2);
  }


  /**
   * Same as scale(s, s, s).
   */
  public void scale(float s) {
    scale(s, s, s);
  }


  /**
   * Same as scale(sx, sy, 1).
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


  public void pushMatrix() {
    if (matrixStackDepth == MATRIX_STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    modelview.get(matrixStack[matrixStackDepth]);
    modelviewInv.get(matrixInvStack[matrixStackDepth]);
    matrixStackDepth++;
  }


  public void popMatrix() {
    if (matrixStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    matrixStackDepth--;
    modelview.set(matrixStack[matrixStackDepth]);
    modelviewInv.set(matrixInvStack[matrixStackDepth]);
  }


  public void resetMatrix() {
    forwardTransform.reset();
    reverseTransform.reset();
  }


  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    applyMatrix(n00, n01, n02, 0,
                n10, n11, n12, 0,
                0,   0,   1,   0,
                0,   0,   0,   1);
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

    forwardTransform.apply(n00, n01, n02, n03,
                           n10, n11, n12, n13,
                           n20, n21, n22, n23,
                           n30, n31, n32, n33);

    reverseTransform.invApply(n00, n01, n02, n03,
                              n10, n11, n12, n13,
                              n20, n21, n22, n23,
                              n30, n31, n32, n33);
  }


  /**
   * Load the modelview into m00, m01, et al so that it can be used.
   */
//  public void loadMatrix() {
//    m00 = modelview.m00;
//    m01 = modelview.m01;
//    m02 = modelview.m02;
//    m03 = modelview.m03;
//
//    m10 = modelview.m10;
//    m11 = modelview.m11;
//    m12 = modelview.m12;
//    m13 = modelview.m13;
//
//    m20 = modelview.m20;
//    m21 = modelview.m21;
//    m22 = modelview.m22;
//    m23 = modelview.m23;
//
//    m30 = modelview.m30;
//    m31 = modelview.m31;
//    m32 = modelview.m32;
//    m33 = modelview.m33;
//  }


  /**
   * Print the current model (or "transformation") matrix.
   */
  public void printMatrix() {
    modelview.print();
  }



  //////////////////////////////////////////////////////////////

  // CAMERA and PERSPECTIVE


  /**
   * Set matrix mode to the camera matrix (instead of the current
   * transformation matrix). This means applyMatrix, resetMatrix, etc.
   * will affect the camera.
   * <P>
   * Note that the camera matrix is *not* the perspective matrix,
   * it is in front of the modelview matrix (hence the name "model"
   * and "view" for that matrix).
   * <P>
   * beginCamera() specifies that all coordinate transforms until endCamera()
   * should be pre-applied in inverse to the camera transform matrix.
   * Note that this is only challenging when a user specifies an arbitrary
   * matrix with applyMatrix(). Then that matrix will need to be inverted,
   * which may not be possible. But take heart, if a user is applying a
   * non-invertible matrix to the camera transform, then he is clearly
   * up to no good, and we can wash our hands of those bad intentions.
   * <P>
   * begin/endCamera clauses do not automatically reset the camera transform
   * matrix. That's because we set up a nice default camera transform int
   * setup(), and we expect it to hold through draw(). So we don't reset
   * the camera transform matrix at the top of draw(). That means that an
   * innocuous-looking clause like
   * <PRE>
   * beginCamera();
   * translate(0, 0, 10);
   * endCamera();
   * </PRE>
   * at the top of draw(), will result in a runaway camera that shoots
   * infinitely out of the screen over time. In order to prevent this,
   * it is necessary to call some function that does a hard reset of the
   * camera transform matrix inside of begin/endCamera. Two options are
   * <PRE>
   * camera(); // sets up the nice default camera transform
   * resetMatrix(); // sets up the identity camera transform
   * </PRE>
   * So to rotate a camera a constant amount, you might try
   * <PRE>
   * beginCamera();
   * camera();
   * rotateY(PI/8);
   * endCamera();
   * </PRE>
   */
  public void beginCamera() {
    if (manipulatingCamera) {
      throw new RuntimeException("beginCamera() cannot be called again " +
                                 "before endCamera()");
    } else {
      manipulatingCamera = true;
      forwardTransform = cameraInv;
      reverseTransform = camera;
    }
  }


  /**
   * Record the current settings into the camera matrix, and set
   * the matrix mode back to the current transformation matrix.
   * <P>
   * Note that this will destroy any settings to scale(), translate(),
   * or whatever, because the final camera matrix will be copied
   * (not multiplied) into the modelview.
   */
  public void endCamera() {
    if (!manipulatingCamera) {
      throw new RuntimeException("Cannot call endCamera() " +
                                 "without first calling beginCamera()");
    }
    // reset the modelview to use this new camera matrix
    modelview.set(camera);
    modelviewInv.set(cameraInv);

    // set matrix mode back to modelview
    forwardTransform = modelview;
    reverseTransform = modelviewInv;

    // all done
    manipulatingCamera = false;
  }


  /**
   * Set camera to the default settings.
   * <P>
   * Processing camera behavior:
   * <P>
   * Camera behavior can be split into two separate components, camera
   * transformation, and projection. The transformation corresponds to the
   * physical location, orientation, and scale of the camera. In a physical
   * camera metaphor, this is what can manipulated by handling the camera
   * body (with the exception of scale, which doesn't really have a physcial
   * analog). The projection corresponds to what can be changed by
   * manipulating the lens.
   * <P>
   * We maintain separate matrices to represent the camera transform and
   * projection. An important distinction between the two is that the camera
   * transform should be invertible, where the projection matrix should not,
   * since it serves to map three dimensions to two. It is possible to bake
   * the two matrices into a single one just by multiplying them together,
   * but it isn't a good idea, since lighting, z-ordering, and z-buffering
   * all demand a true camera z coordinate after modelview and camera
   * transforms have been applied but before projection. If the camera
   * transform and projection are combined there is no way to recover a
   * good camera-space z-coordinate from a model coordinate.
   * <P>
   * Fortunately, there are no functions that manipulate both camera
   * transformation and projection.
   * <P>
   * camera() sets the camera position, orientation, and center of the scene.
   * It replaces the camera transform with a new one. This is different from
   * gluLookAt(), but I think the only reason that GLU's lookat doesn't fully
   * replace the camera matrix with the new one, but instead multiplies it,
   * is that GL doesn't enforce the separation of camera transform and
   * projection, so it wouldn't be safe (you'd probably stomp your projection).
   * <P>
   * The transformation functions are the same ones used to manipulate the
   * modelview matrix (scale, translate, rotate, etc.). But they are bracketed
   * with beginCamera(), endCamera() to indicate that they should apply
   * (in inverse), to the camera transformation matrix.
   * <P>
   * This differs considerably from camera transformation in OpenGL.
   * OpenGL only lets you say, apply everything from here out to the
   * projection or modelview matrix. This makes it very hard to treat camera
   * manipulation as if it were a physical camera. Imagine that you want to
   * move your camera 100 units forward. In OpenGL, you need to apply the
   * inverse of that transformation or else you'll move your scene 100 units
   * forward--whether or not you've specified modelview or projection matrix.
   * Remember they're just multiplied by model coods one after another.
   * So in order to treat a camera like a physical camera, it is necessary
   * to pre-apply inverse transforms to a matrix that will be applied to model
   * coordinates. OpenGL provides nothing of this sort, but Processing does!
   * This is the camera transform matrix.
   */
  public void camera() {
    camera(cameraX, cameraY, cameraZ,
           cameraX, cameraY, 0,
           0, 1, 0);
  }


  /**
   * More flexible method for dealing with camera().
   * <P>
   * The actual call is like gluLookat. Here's the real skinny on
   * what does what:
   * <PRE>
   * camera(); or
   * camera(ex, ey, ez, cx, cy, cz, ux, uy, uz);
   * </PRE>
   * do not need to be called from with beginCamera();/endCamera();
   * That's because they always apply to the camera transformation,
   * and they always totally replace it. That means that any coordinate
   * transforms done before camera(); in draw() will be wiped out.
   * It also means that camera() always operates in untransformed world
   * coordinates. Therefore it is always redundant to call resetMatrix();
   * before camera(); This isn't technically true of gluLookat, but it's
   * pretty much how it's used.
   * <P>
   * Now, beginCamera(); and endCamera(); are useful if you want to move
   * the camera around using transforms like translate(), etc. They will
   * wipe out any coordinate system transforms that occur before them in
   * draw(), but they will not automatically wipe out the camera transform.
   * This means that they should be at the top of draw(). It also means
   * that the following:
   * <PRE>
   * beginCamera();
   * rotateY(PI/8);
   * endCamera();
   * </PRE>
   * will result in a camera that spins without stopping. If you want to
   * just rotate a small constant amount, try this:
   * <PRE>
   * beginCamera();
   * camera(); // sets up the default view
   * rotateY(PI/8);
   * endCamera();
   * </PRE>
   * That will rotate a little off of the default view. Note that this
   * is entirely equivalent to
   * <PRE>
   * camera(); // sets up the default view
   * beginCamera();
   * rotateY(PI/8);
   * endCamera();
   * </PRE>
   * because camera() doesn't care whether or not it's inside a
   * begin/end clause. Basically it's safe to use camera() or
   * camera(ex, ey, ez, cx, cy, cz, ux, uy, uz) as naked calls because
   * they do all the matrix resetting automatically.
   */
  public void camera(float eyeX, float eyeY, float eyeZ,
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

    // just does an apply to the main matrix,
    // since that'll be copied out on endCamera
    camera.set(x0, x1, x2, 0,
               y0, y1, y2, 0,
               z0, z1, z2, 0,
               0,  0,  0,  1);
    camera.translate(-eyeX, -eyeY, -eyeZ);

    cameraInv.reset();
    cameraInv.invApply(x0, x1, x2, 0,
                       y0, y1, y2, 0,
                       z0, z1, z2, 0,
                       0,  0,  0,  1);
    cameraInv.translate(eyeX, eyeY, eyeZ);

    modelview.set(camera);
    modelviewInv.set(cameraInv);
  }


  /**
   * Print the current camera matrix.
   */
  public void printCamera() {
    camera.print();
  }


  /**
   * Calls ortho() with the proper parameters for Processing's
   * standard orthographic projection.
   */
  public void ortho() {
    ortho(0, width, 0, height, -10, 10);
  }


  /**
   * Similar to gluOrtho(), but wipes out the current projection matrix.
   * <P>
   * Implementation partially based on Mesa's matrix.c.
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

    projection.set(x, 0, 0, tx,
                   0, y, 0, ty,
                   0, 0, z, tz,
                   0, 0, 0, 1);
  }


  /**
   * Calls perspective() with Processing's standard coordinate projection.
   * <P>
   * Projection functions:
   * <UL>
   * <LI>frustrum()
   * <LI>ortho()
   * <LI>perspective()
   * </UL>
   * Each of these three functions completely replaces the projection
   * matrix with a new one. They can be called inside setup(), and their
   * effects will be felt inside draw(). At the top of draw(), the projection
   * matrix is not reset. Therefore the last projection function to be
   * called always dominates. On resize, the default projection is always
   * established, which has perspective.
   * <P>
   * This behavior is pretty much familiar from OpenGL, except where
   * functions replace matrices, rather than multiplying against the
   * previous.
   * <P>
   */
  public void perspective() {
    perspective(cameraFOV, cameraAspect, cameraNear, cameraFar);
  }


  /**
   * Similar to gluPerspective(). Implementation based on Mesa's glu.c
   */
  public void perspective(float fov, float aspect, float zNear, float zFar) {
    //float ymax = zNear * tan(fovy * PI / 360.0f);
    float ymax = zNear * (float) Math.tan(fov / 2);
    float ymin = -ymax;

    float xmin = ymin * aspect;
    float xmax = ymax * aspect;

    frustum(xmin, xmax, ymin, ymax, zNear, zFar);
  }


  /**
   * Same as glFrustum(), except that it wipes out (rather than
   * multiplies against) the current perspective matrix.
   * <P>
   * Implementation based on the explanation in the OpenGL blue book.
   */
  public void frustum(float left, float right, float bottom,
                      float top, float znear, float zfar) {

    //if (hints[ENABLE_ACCURATE_TEXTURES]){
    //These vars are only needed if accurate textures are used, however,
    //there is the possibility that accurate texturing will only be turned
    //on after the perspective matrix has already been set, so we might as
    //well store these no matter what since it's not much overhead.
    leftScreen = left;
    rightScreen = right;
    bottomScreen = bottom;
    topScreen = top;
    nearPlane = znear;
    //}

    //System.out.println(projection);
    projection.set((2*znear)/(right-left), 0, (right+left)/(right-left), 0,
                   0, (2*znear)/(top-bottom), (top+bottom)/(top-bottom), 0,
                   0, 0, -(zfar+znear)/(zfar-znear),-(2*zfar*znear)/(zfar-znear),
                   0, 0, -1, 0);
  }


  /**
   * Print the current projection matrix.
   */
  public void printProjection() {
    projection.print();
  }


  /*
   * This function checks if the modelview matrix is set up to likely be
   * drawing in 2D. It merely checks if the non-translational piece of the
   * matrix is unity. If this is to be used, it should be coupled with a
   * check that the raw vertex coordinates lie in the z=0 plane.
   * Mainly useful for applying sub-pixel shifts to avoid 2d artifacts
   * in the screen plane.
   * Added by ewjordan 6/13/07
   *
   * TODO need to invert the logic here so that we can simply return
   * the value, rather than calculating true/false and returning it.
   */
  private boolean drawing2D() {
    if (modelview.m00 != 1.0f ||
        modelview.m11 != 1.0f ||
        modelview.m22 != 1.0f || // check scale
        modelview.m01 != 0.0f ||
        modelview.m02 != 0.0f || // check rotational pieces
        modelview.m10 != 0.0f ||
        modelview.m12 != 0.0f ||
        modelview.m20 != 0.0f ||
        modelview.m21 != 0.0f ||
        !((camera.m23-modelview.m23) <= EPSILON &&
          (camera.m23-modelview.m23) >= -EPSILON)) { // check for z-translation
      // Something about the modelview matrix indicates 3d drawing
      // (or rotated 2d, in which case 2d subpixel fixes probably aren't needed)
      return false;
    } else {
      //The matrix is mapping z=0 vertices to the screen plane,
      // which means it's likely that 2D drawing is happening.
      return true;
    }
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
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float ox =
      projection.m00*ax + projection.m01*ay +
      projection.m02*az + projection.m03*aw;
    float ow =
      projection.m30*ax + projection.m31*ay +
      projection.m32*az + projection.m33*aw;

    if (ow != 0) ox /= ow;
    return width * (1 + ox) / 2.0f;
  }


  public float screenY(float x, float y, float z) {
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float oy =
      projection.m10*ax + projection.m11*ay +
      projection.m12*az + projection.m13*aw;
    float ow =
      projection.m30*ax + projection.m31*ay +
      projection.m32*az + projection.m33*aw;

    if (ow != 0) oy /= ow;
    return height * (1 + oy) / 2.0f;
  }


  public float screenZ(float x, float y, float z) {
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float oz =
      projection.m20*ax + projection.m21*ay +
      projection.m22*az + projection.m23*aw;
    float ow =
      projection.m30*ax + projection.m31*ay +
      projection.m32*az + projection.m33*aw;

    if (ow != 0) oz /= ow;
    return (oz + 1) / 2.0f;
  }


  public float modelX(float x, float y, float z) {
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float ox =
      cameraInv.m00*ax + cameraInv.m01*ay +
      cameraInv.m02*az + cameraInv.m03*aw;
    float ow =
      cameraInv.m30*ax + cameraInv.m31*ay +
      cameraInv.m32*az + cameraInv.m33*aw;

    return (ow != 0) ? ox / ow : ox;
  }


  public float modelY(float x, float y, float z) {
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float oy =
      cameraInv.m10*ax + cameraInv.m11*ay +
      cameraInv.m12*az + cameraInv.m13*aw;
    float ow =
      cameraInv.m30*ax + cameraInv.m31*ay +
      cameraInv.m32*az + cameraInv.m33*aw;

    return (ow != 0) ? oy / ow : oy;
  }


  public float modelZ(float x, float y, float z) {
    float ax =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;

    float oz =
      cameraInv.m20*ax + cameraInv.m21*ay +
      cameraInv.m22*az + cameraInv.m23*aw;
    float ow =
      cameraInv.m30*ax + cameraInv.m31*ay +
      cameraInv.m32*az + cameraInv.m33*aw;

    return (ow != 0) ? oz / ow : oz;
  }


  //////////////////////////////////////////////////////////////


  // strokeWeight() doesn't really work properly either,
  // but that will be dealt with in some other way.


  public void strokeJoin(int join) {
    methodError("strokeJoin");
  }


  public void strokeCap(int cap) {
    methodError("strokeCap");
  }



  //////////////////////////////////////////////////////////////


  protected void fillFromCalc() {
    super.fillFromCalc();
    ambientFromCalc();
  }


  //////////////////////////////////////////////////////////////


  public void ambient(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      ambient((float) rgb);

    } else {
      colorCalcARGB(rgb, colorModeA);
      ambientFromCalc();
    }
  }


  public void ambient(float gray) {
    colorCalc(gray);
    ambientFromCalc();
  }


  public void ambient(float x, float y, float z) {
    colorCalc(x, y, z);
    ambientFromCalc();
  }


  protected void ambientFromCalc() {
    ambientR = calcR;
    ambientG = calcG;
    ambientB = calcB;
//    material.ambient(calcR, calcG, calcB);
  }


  //////////////////////////////////////////////////////////////


  public void specular(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      specular((float) rgb);

    } else {
      colorCalcARGB(rgb, colorModeA);
      specularFromCalc();
    }
  }


  public void specular(float gray) {
    colorCalc(gray);
    specularFromCalc();
  }


//  public void specular(float gray, float alpha) {
//    colorCalc(gray, alpha);
//    specularFromCalc();
//  }


  public void specular(float x, float y, float z) {
    colorCalc(x, y, z);
    specularFromCalc();
  }


//  public void specular(float x, float y, float z, float a) {
//    colorCalc(x, y, z, a);
//    specularFromCalc();
//  }


  protected void specularFromCalc() {
    specularR = calcR;
    specularG = calcG;
    specularB = calcB;
    //specularA = calcA;
    //specularRi = calcRi;
    //specularGi = calcGi;
    //specularBi = calcBi;
    //specularAi = calcAi;
  }


  public void shininess(float shine) {
    shininess = shine;
  }


  //////////////////////////////////////////////////////////////


  public void emissive(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      emissive((float) rgb);

    } else {
      colorCalcARGB(rgb, colorModeA);
      emissiveFromCalc();
    }
  }


  public void emissive(float gray) {
    colorCalc(gray);
    emissiveFromCalc();
  }


  public void emissive(float x, float y, float z) {
    colorCalc(x, y, z);
    emissiveFromCalc();
  }


  protected void emissiveFromCalc() {
    emissiveR = calcR;
    emissiveG = calcG;
    emissiveB = calcB;
    //emissiveRi = calcRi;
    //emissiveGi = calcGi;
    //emissiveBi = calcBi;
  }


  //////////////////////////////////////////////////////////////


  /**
   * Sets up an ambient and directional light.
   * <PRE>
   * The Lighting Skinny:
   *
   * The way lighting works is complicated enough that it's worth
   * producing a document to describe it. Lighting calculations proceed
   * pretty much exactly as described in the OpenGL red book.
   *
   * Light-affecting material properties:
   *
   *   AMBIENT COLOR
   *   - multiplies by light's ambient component
   *   - for believability this should match diffuse color
   *
   *   DIFFUSE COLOR
   *   - multiplies by light's diffuse component
   *
   *   SPECULAR COLOR
   *   - multiplies by light's specular component
   *   - usually less colored than diffuse/ambient
   *
   *   SHININESS
   *   - the concentration of specular effect
   *   - this should be set pretty high (20-50) to see really
   *     noticeable specularity
   *
   *   EMISSIVE COLOR
   *   - constant additive color effect
   *
   * Light types:
   *
   *   AMBIENT
   *   - one color
   *   - no specular color
   *   - no direction
   *   - may have falloff (constant, linear, and quadratic)
   *   - may have position (which matters in non-constant falloff case)
   *   - multiplies by a material's ambient reflection
   *
   *   DIRECTIONAL
   *   - has diffuse color
   *   - has specular color
   *   - has direction
   *   - no position
   *   - no falloff
   *   - multiplies by a material's diffuse and specular reflections
   *
   *   POINT
   *   - has diffuse color
   *   - has specular color
   *   - has position
   *   - no direction
   *   - may have falloff (constant, linear, and quadratic)
   *   - multiplies by a material's diffuse and specular reflections
   *
   *   SPOT
   *   - has diffuse color
   *   - has specular color
   *   - has position
   *   - has direction
   *   - has cone angle (set to half the total cone angle)
   *   - has concentration value
   *   - may have falloff (constant, linear, and quadratic)
   *   - multiplies by a material's diffuse and specular reflections
   *
   * Normal modes:
   *
   * All of the primitives (rect, box, sphere, etc.) have their normals
   * set nicely. During beginShape/endShape normals can be set by the user.
   *
   *   AUTO-NORMAL
   *   - if no normal is set during the shape, we are in auto-normal mode
   *   - auto-normal calculates one normal per triangle (face-normal mode)
   *
   *   SHAPE-NORMAL
   *   - if one normal is set during the shape, it will be used for
   *     all vertices
   *
   *   VERTEX-NORMAL
   *   - if multiple normals are set, each normal applies to
   *     subsequent vertices
   *   - (except for the first one, which applies to previous
   *     and subsequent vertices)
   *
   * Efficiency consequences:
   *
   *   There is a major efficiency consequence of position-dependent
   *   lighting calculations per vertex. (See below for determining
   *   whether lighting is vertex position-dependent.) If there is no
   *   position dependency then the only factors that affect the lighting
   *   contribution per vertex are its colors and its normal.
   *   There is a major efficiency win if
   *
   *   1) lighting is not position dependent
   *   2) we are in AUTO-NORMAL or SHAPE-NORMAL mode
   *
   *   because then we can calculate one lighting contribution per shape
   *   (SHAPE-NORMAL) or per triangle (AUTO-NORMAL) and simply multiply it
   *   into the vertex colors. The converse is our worst-case performance when
   *
   *   1) lighting is position dependent
   *   2) we are in AUTO-NORMAL mode
   *
   *   because then we must calculate lighting per-face * per-vertex.
   *   Each vertex has a different lighting contribution per face in
   *   which it appears. Yuck.
   *
   * Determining vertex position dependency:
   *
   *   If any of the following factors are TRUE then lighting is
   *   vertex position dependent:
   *
   *   1) Any lights uses non-constant falloff
   *   2) There are any point or spot lights
   *   3) There is a light with specular color AND there is a
   *      material with specular color
   *
   * So worth noting is that default lighting (a no-falloff ambient
   * and a directional without specularity) is not position-dependent.
   * We should capitalize.
   *
   * Simon Greenwold, April 2005
   * </PRE>
   */
  public void lights() {
    // need to make sure colorMode is RGB 255 here
    int colorModeSaved = colorMode;
    colorMode = RGB;

    lightFalloff(1, 0, 0);
    lightSpecular(0, 0, 0);

    ambientLight(colorModeX * 0.5f,
                 colorModeY * 0.5f,
                 colorModeZ * 0.5f);
    directionalLight(colorModeX * 0.5f,
                     colorModeY * 0.5f,
                     colorModeZ * 0.5f,
                     0, 0, -1);

    colorMode = colorModeSaved;

    lightingDependsOnVertexPosition = false;
  }


  /**
   * Turn off all lights.
   */
  public void noLights() {
    // write any queued geometry, because lighting will be goofed after
    flush();
    // set the light count back to zero
    lightCount = 0;
  }


  /**
   * Add an ambient light based on the current color mode.
   */
  public void ambientLight(float r, float g, float b) {
    ambientLight(r, g, b, 0, 0, 0);
  }


  /**
   * Add an ambient light based on the current color mode.
   * This version includes an (x, y, z) position for situations
   * where the falloff distance is used.
   */
  public void ambientLight(float r, float g, float b,
                           float x, float y, float z) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;

    lightType[lightCount] = AMBIENT;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightPosition(lightCount, x, y, z);
    lightCount++;
    //return lightCount-1;
  }


  public void directionalLight(float r, float g, float b,
                               float nx, float ny, float nz) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;

    lightType[lightCount] = DIRECTIONAL;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightSpecular[lightCount][0] = currentLightSpecular[0];
    lightSpecular[lightCount][1] = currentLightSpecular[1];
    lightSpecular[lightCount][2] = currentLightSpecular[2];
    lightDirection(lightCount, nx, ny, nz);
    lightCount++;
  }


  public void pointLight(float r, float g, float b,
                         float x, float y, float z) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;

    lightType[lightCount] = POINT;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightSpecular[lightCount][0] = currentLightSpecular[0];
    lightSpecular[lightCount][1] = currentLightSpecular[1];
    lightSpecular[lightCount][2] = currentLightSpecular[2];
    lightPosition(lightCount, x, y, z);
    lightCount++;

    lightingDependsOnVertexPosition = true;
  }


  public void spotLight(float r, float g, float b,
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float angle, float concentration) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightDiffuse[lightCount][0] = calcR;
    lightDiffuse[lightCount][1] = calcG;
    lightDiffuse[lightCount][2] = calcB;

    lightType[lightCount] = SPOT;
    lightFalloffConstant[lightCount] = currentLightFalloffConstant;
    lightFalloffLinear[lightCount] = currentLightFalloffLinear;
    lightFalloffQuadratic[lightCount] = currentLightFalloffQuadratic;
    lightSpecular[lightCount][0] = currentLightSpecular[0];
    lightSpecular[lightCount][1] = currentLightSpecular[1];
    lightSpecular[lightCount][2] = currentLightSpecular[2];
    lightPosition(lightCount, x, y, z);
    lightDirection(lightCount, nx, ny, nz);
    lightSpotAngle[lightCount] = angle;
    lightSpotAngleCos[lightCount] = Math.max(0, (float) Math.cos(angle));
    lightSpotConcentration[lightCount] = concentration;
    lightCount++;

    lightingDependsOnVertexPosition = true;
  }


  /**
   * Set the light falloff rates for the last light that was created.
   * Default is lightFalloff(1, 0, 0).
   */
  public void lightFalloff(float constant, float linear, float quadratic) {
    currentLightFalloffConstant = constant;
    currentLightFalloffLinear = linear;
    currentLightFalloffQuadratic = quadratic;

    lightingDependsOnVertexPosition = true;
  }


  /**
   * Set the specular color of the last light created.
   */
  public void lightSpecular(float x, float y, float z) {
    colorCalc(x, y, z);
    currentLightSpecular[0] = calcR;
    currentLightSpecular[1] = calcG;
    currentLightSpecular[2] = calcB;

    lightingDependsOnVertexPosition = true;
  }


  /**
   * internal function to set the light position
   * based on the current modelview matrix.
   */
  protected void lightPosition(int num, float x, float y, float z) {
    lightPosition[num][0] =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    lightPosition[num][1] =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    lightPosition[num][2] =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
  }


  /**
   * internal function to set the light direction
   * based on the current modelview matrix.
   */
  protected void lightDirection(int num, float x, float y, float z) {
    // Multiply by inverse transpose.
    lightNormal[num][0] =
      modelviewInv.m00*x + modelviewInv.m10*y +
      modelviewInv.m20*z + modelviewInv.m30;
    lightNormal[num][1] =
      modelviewInv.m01*x + modelviewInv.m11*y +
      modelviewInv.m21*z + modelviewInv.m31;
    lightNormal[num][2] =
      modelviewInv.m02*x + modelviewInv.m12*y +
      modelviewInv.m22*z + modelviewInv.m32;

    float n = mag(lightNormal[num][0], lightNormal[num][1], lightNormal[num][2]);
    if (n == 0 || n == 1) return;

    lightNormal[num][0] /= n;
    lightNormal[num][1] /= n;
    lightNormal[num][2] /= n;
  }



  //////////////////////////////////////////////////////////////

  // BACKGROUND


  /**
   * Takes an RGB or RGBA image and sets it as the background.
   * <P>
   * Note that even if the image is set as RGB, the high 8 bits of
   * each pixel must be set (0xFF000000), because the image data will
   * be copied directly to the screen.
   * <P>
   * Also clears out the zbuffer and stencil buffer if they exist.
   */
  public void background(PImage image) {
    super.background(image);
    Arrays.fill(zbuffer, Float.MAX_VALUE);
  }


  /**
   * Clear pixel buffer. With P3D and OPENGL, this also clears the zbuffer.
   */
  protected void clear3() {
    Arrays.fill(pixels, backgroundColor);
    Arrays.fill(zbuffer, Float.MAX_VALUE);
    clearRaw();
  }
   

  /**
   * Handled as separate function for OpenGL subclass that overrides clear()
   * but still needs this to work properly.
   */
  protected void clearRaw() {
    if (raw != null) {
      raw.colorMode(RGB, 1);
      raw.noStroke();
      raw.fill(backgroundR, backgroundG, backgroundB);
      raw.beginShape(TRIANGLES);

      raw.vertex(0, 0);
      raw.vertex(width, 0);
      raw.vertex(0, height);

      raw.vertex(width, 0);
      raw.vertex(width, height);
      raw.vertex(0, height);

      raw.endShape();
    }
  }



  //////////////////////////////////////////////////////////////

  // SMOOTH (not available, throws error)

  // although should this bother throwing an error?
  // could be a pain in the ass when trying to debug with opengl


  public void smooth() {
    methodError("smooth");
  }


  public void noSmooth() {
    // no need to do an error here, really...
  }



  //////////////////////////////////////////////////////////////

  // MATH (internal use only)


  private final float mag(float a, float b, float c) {
    return (float) Math.sqrt(a*a + b*b + c*c);
  }

  
  private final float clamp(float a) {
    return (a < 1) ? a : 1;
  }
  
  
  private final float abs(float a) {
    return (a < 0) ? -a : a;
  }

  
  private float dot(float ax, float ay, float az,
                    float bx, float by, float bz) {
    return ax*bx + ay*by + az*bz;
  }

  
  private final void cross(float a0, float a1, float a2,
                           float b0, float b1, float b2, 
                           float[] out) {
    out[0] = a1*b2 - a2*b1;
    out[1] = a2*b0 - a0*b2;
    out[2] = a0*b1 - a1*b0;
  }
  

  /*
  private final void cross(float[] a, float[] b, float[] out) {
    out[0] = a[1]*b[2] - a[2]*b[1];
    out[1] = a[2]*b[0] - a[0]*b[2];
    out[2] = a[0]*b[1] - a[1]*b[0];
  }
  */
}

