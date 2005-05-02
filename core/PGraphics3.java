/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
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


/**
 * Subclass of PGraphics that handles 3D rendering.
 */
public class PGraphics3 extends PGraphics {

  /** The modelview matrix. */
  public PMatrix modelview;

  /** Inverse modelview matrix, used for lighting. */
  public PMatrix modelviewInv;

  /**
   * The camera matrix, the modelview
   * will be set to this on beginFrame.
   */
  public PMatrix camera;

  /** Inverse camera matrix */
  public PMatrix cameraInv;

  // ........................................................

  // Lighting-related variables

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
  public float[] tempLightingContribution = new float[LIGHT_COLOR_COUNT];
  public float[] worldNormal = new float[4];

  // ........................................................

  /** Camera field of view (in radians, as of rev 86) */
  public float cameraFOV;

  /** Position of the camera */
  public float cameraX, cameraY, cameraZ;

  public float cameraNear, cameraFar;
  public float cameraAspect;

  /**
   * This is turned on at beginCamera, and off at endCamera
   * Currently we don't support nested begin/end cameras.
   * If we wanted to, this variable would have to become a stack.
   */
  public boolean manipulatingCamera;

  // projection matrix
  public PMatrix projection; // = new PMatrix();

  // These two matrices always point to either the modelview
  // or the modelviewInv, but they are swapped during
  // when in camera maniuplation mode. That way camera transforms
  // are automatically accumulated in inverse on the modelview matrix.
  public PMatrix forwardTransform;
  public PMatrix reverseTransform;

  // ........................................................

  /// the stencil buffer
  public int stencil[];

  /// depth buffer
  public float zbuffer[];

  // ........................................................

  /** Maximum lights by default is 8, which is arbitrary,
      but is the minimum defined by OpenGL */
  protected static final int MAX_LIGHTS = 8;

  public int lightCount = 0;

  /** Light types */
  public int lights[];

  /** Light positions */
  public float lightsX[], lightsY[], lightsZ[];

  /** Light direction (normalized vector) */
  public float lightsNX[], lightsNY[], lightsNZ[];

  /** Light falloff */
  public float lightsFalloffConstant[];
  public float lightsFalloffLinear[];
  public float lightsFalloffQuadratic[];

  /** Light spot angle */
  public float lightsSpotAngle[];

  /** Cosine of light spot angle */
  public float lightsSpotAngleCos[];

  /** Light spot concentration */
  public float lightsSpotConcentration[];

  /** Diffuse colors for lights.
   *  For an ambient light, this will hold the ambient color.
   *  Internally these are stored as numbers between 0 and 1. */
  public float lightsDiffuseR[], lightsDiffuseG[], lightsDiffuseB[];

  /** Specular colors for lights.
      Internally these are stored as numbers between 0 and 1. */
  public float lightsSpecularR[], lightsSpecularG[], lightsSpecularB[];

  public float lightSpecularR;
  public float lightSpecularG;
  public float lightSpecularB;
  public float lightFalloffConstant;
  public float lightFalloffLinear;
  public float lightFalloffQuadratic;

  // ........................................................

  // pos of first vertex of current shape in vertices array
  protected int vertex_start;

  // i think vertex_end is actually the last vertex in the current shape
  // and is separate from vertexCount for occasions where drawing happens
  // on endFrame with all the triangles being depth sorted
  protected int vertex_end;

  // vertices may be added during clipping against the near plane.
  protected int vertex_end_including_clip_verts;

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
  PTriangle triangle;
  public int triangles[][] =
    new int[DEFAULT_TRIANGLES][TRIANGLE_FIELD_COUNT];
  public float triangleColors[][][] =
    new float[DEFAULT_TRIANGLES][3][TRIANGLE_COLOR_COUNT];
  public int triangleCount;   // total number of triangles

  // cheap picking someday
  int shape_index;

  // ........................................................

  /**
   * Sets whether texture coordinates passed to
   * vertex() calls will be based on coordinates that are
   * based on the IMAGE or NORMALIZED.
   */
  public int textureMode;

  /**
   * Current horizontal coordinate for texture, will always
   * be between 0 and 1, even if using textureMode(IMAGE).
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
    reverseTransform = modelviewInv;
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
    reverseTransform = modelviewInv;
    //resize(iwidth, iheight);
    //projection = new PMatrix();
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
    cameraFOV = 60 * DEG_TO_RAD; // at least for now
    cameraX = width / 2.0f;
    cameraY = height / 2.0f;
    //cameraZ = cameraY / ((float) tan(PI * cameraFOV / 360f));
    cameraZ = cameraY / ((float) tan(cameraFOV / 2.0f));
    cameraNear = cameraZ / 10.0f;
    cameraFar = cameraZ * 10.0f;

    cameraAspect = (float)width / (float)height;

    // init lights (in resize() instead of allocate() b/c needed by opengl)
    //lights = new PLight[MAX_LIGHTS];
    //light = new boolean[MAX_LIGHTS];
    lightsX = new float[MAX_LIGHTS];
    lightsY = new float[MAX_LIGHTS];
    lightsZ = new float[MAX_LIGHTS];
    lightsDiffuseR = new float[MAX_LIGHTS];
    lightsDiffuseG = new float[MAX_LIGHTS];
    lightsDiffuseB = new float[MAX_LIGHTS];
    lightsSpecularR = new float[MAX_LIGHTS];
    lightsSpecularG = new float[MAX_LIGHTS];
    lightsSpecularB = new float[MAX_LIGHTS];
    lights = new int[MAX_LIGHTS];
    lightsNX = new float[MAX_LIGHTS];
    lightsNY = new float[MAX_LIGHTS];
    lightsNZ = new float[MAX_LIGHTS];
    lightsFalloffConstant = new float[MAX_LIGHTS];
    lightsFalloffLinear = new float[MAX_LIGHTS];
    lightsFalloffQuadratic = new float[MAX_LIGHTS];
    lightsSpotAngle = new float[MAX_LIGHTS];
    lightsSpotAngleCos = new float[MAX_LIGHTS];
    lightsSpotConcentration = new float[MAX_LIGHTS];

    // reset the cameraMode if PERSPECTIVE or ORTHOGRAPHIC
    // will just be ignored if CUSTOM, the user's hosed anyways
    //System.out.println("setting cameraMode to " + cameraMode);
    //if (this.cameraMode != CUSTOM) cameraMode(this.cameraMode);

    // making this again here because things are weird
    projection = new PMatrix();

    modelview = new PMatrix(MATRIX_STACK_DEPTH);
    modelviewInv = new PMatrix(MATRIX_STACK_DEPTH);
    forwardTransform = modelview;
    reverseTransform = modelviewInv;

    camera = new PMatrix();
    cameraInv = new PMatrix();

    // set up the default camera
    camera();

    // defaults to perspective, if the user has setup up their
    // own projection, they'll need to fix it after resize anyway.
    // this helps the people who haven't set up their own projection.
    perspective();
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


  public void beginFrame() {
    super.beginFrame();

    modelview.set(camera);
    modelviewInv.set(cameraInv);

    // clear out the lights, they'll have to be turned on again
    lightCount = 0;
    lightingDependsOnVertexPosition = false;
    lightFalloff(1,0,0);
    lightSpecular(0,0,0);

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


  public void defaults() {
    super.defaults();

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
   * Set texture mode to either to use coordinates based on the IMAGE
   * (more intuitive for new users) or NORMALIZED (better for advanced chaps)
   */
  public void textureMode(int mode) {
    this.textureMode = mode;
  }


  /**
   * Set texture image for current shape
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

    vertex[BEEN_LIT] = 0;
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

    if (textureImage != null) {
      vertex[U] = textureU;
      vertex[V] = textureV;
    }

    vertex[NX] = normalX;
    vertex[NY] = normalY;
    vertex[NZ] = normalZ;

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
  public void bezierVertex(float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
    bezierVertex(x2, y2, 0, x3, y3, 0, x4, y4, 0);
  }


  /**
   * See notes with the bezier() function.
   */
  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    if (splineVertexCount > 0) {
      float vertex[] = splineVertices[splineVertexCount-1];
      spline_vertex(vertex[MX], vertex[MY], vertex[MZ], true);

    } else if (vertexCount > 0) {
      // make sure there's at least a call to vertex()
      float vertex[] = vertices[vertexCount-1];
      spline_vertex(vertex[MX], vertex[MY], vertex[MZ], true);

    } else {
      throw new RuntimeException("A call to vertex() must be used " +
                                 "before bezierVertex()");
    }
    spline_vertex(x2, y2, z2, true);
    spline_vertex(x3, y3, z3, true);
    spline_vertex(x4, y4, z4, true);
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
        modelview.m00*vertex[MX] + modelview.m01*vertex[MY] +
        modelview.m02*vertex[MZ] + modelview.m03;
      vertex[VY] =
        modelview.m10*vertex[MX] + modelview.m11*vertex[MY] +
        modelview.m12*vertex[MZ] + modelview.m13;
      vertex[VZ] =
        modelview.m20*vertex[MX] + modelview.m21*vertex[MY] +
        modelview.m22*vertex[MZ] + modelview.m23;
      vertex[VW] =
        modelview.m30*vertex[MX] + modelview.m31*vertex[MY] +
        modelview.m32*vertex[MZ] + modelview.m33;

      // normalize
      if (vertex[VW] != 0 && vertex[VW] != ONE) {
        vertex[VX] /= vertex[VW];
        vertex[VY] /= vertex[VW];
        vertex[VZ] /= vertex[VW];
      }
      vertex[VW] = ONE;
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
    // TRANSFORM / LIGHT / CLIP

    if (lightCount > 0 && fill) {
    handle_lighting();
    }
    else {
      handle_no_lighting();
    }



    // ------------------------------------------------------------------
    // POINTS FROM CAMERA SPACE (VX, VY, VZ) TO SCREEN SPACE (X, Y, Z)

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

      if (ow != 0 && ow != ONE) {
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
    boolean cClipped = false;
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
      cClipped = true;
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

    //System.out.println("az, bz, cameraNear, dz: " + az + ", " + bz + ", " + cameraNear + ", " + dz);
    //System.out.println("PA, PB: " + pa + ", " + pb);

    vertex(pa * va[MX] + pb * vb[MX], pa * va[MY] + pb * vb[MY], pa * va[MZ] + pb * vb[MZ]);
    int irv = vertexCount - 1;
    vertex_end_including_clip_verts++;
    float[] rv = vertices[irv];

    rv[X] = pa * va[X] + pb * vb[X];
    rv[Y] = pa * va[Y] + pb * vb[Y];
    rv[Z] = pa * va[Z] + pb * vb[Z];

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
    rv[SPA] = pa * va[SPA] + pb * vb[SPA];

    rv[ER] = pa * va[ER] + pb * vb[ER];
    rv[EG] = pa * va[EG] + pb * vb[EG];
    rv[EB] = pa * va[EB] + pb * vb[EB];

    rv[SHINE] = pa * va[SHINE] + pb * vb[SHINE];

    rv[BEEN_LIT] = 0;

    return irv;
  }

  protected final void add_triangle_no_clip(int a, int b, int c) {
    //System.out.println("adding triangle " + triangleCount);
    if (triangleCount == triangles.length) {
      int temp[][] = new int[triangleCount<<1][TRIANGLE_FIELD_COUNT];
      System.arraycopy(triangles, 0, temp, 0, triangleCount);
      triangles = temp;
      //message(CHATTER, "allocating more triangles " + triangles.length);
      float ftemp[][][] = new float[triangleCount<<1][3][TRIANGLE_COLOR_COUNT];
      System.arraycopy(triangleColors, 0, ftemp, 0, triangleCount);
      triangleColors = ftemp;
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
      float ar = min(1, triangleColors[i][0][TRI_DIFFUSE_R] +
                     triangleColors[i][0][TRI_SPECULAR_R]);
      float ag = min(1, triangleColors[i][0][TRI_DIFFUSE_G] +
                     triangleColors[i][0][TRI_SPECULAR_G]);
      float ab = min(1, triangleColors[i][0][TRI_DIFFUSE_B] +
                     triangleColors[i][0][TRI_SPECULAR_B]);
      float br = min(1, triangleColors[i][1][TRI_DIFFUSE_R] +
                     triangleColors[i][1][TRI_SPECULAR_R]);
      float bg = min(1, triangleColors[i][1][TRI_DIFFUSE_G] +
                     triangleColors[i][1][TRI_SPECULAR_G]);
      float bb = min(1, triangleColors[i][1][TRI_DIFFUSE_B] +
                     triangleColors[i][1][TRI_SPECULAR_B]);
      float cr = min(1, triangleColors[i][2][TRI_DIFFUSE_R] +
                     triangleColors[i][2][TRI_SPECULAR_R]);
      float cg = min(1, triangleColors[i][2][TRI_DIFFUSE_G] +
                     triangleColors[i][2][TRI_SPECULAR_G]);
      float cb = min(1, triangleColors[i][2][TRI_DIFFUSE_B] +
                     triangleColors[i][2][TRI_SPECULAR_B]);

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

    if (out[3] != 0 && out[3] != ONE) {
      // divide by perspective coordinate
      out[0] /= out[3]; out[1] /= out[3]; out[2] /= out[3];
    }
    out[3] = 1;

    float nlen = mag(out[0], out[1], out[2]);  // normalize
    if (nlen != 0 && nlen != ONE) {
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
    }
    else {
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

      float denom = lightsFalloffConstant[i];
      float spotTerm = 1;

      if (lights[i] == AMBIENT) {
        if (lightsFalloffQuadratic[i] != 0 || lightsFalloffLinear[i] != 0) {
          // Falloff depends on distance
          float distSq = mag(lightsX[i] - wx, lightsY[i] - wy, lightsZ[i] - wz);
          denom += lightsFalloffQuadratic[i] * distSq + lightsFalloffLinear[i]
              * (float) sqrt(distSq);
        }
        if (denom == 0)
          denom = 1;
        contribution[LIGHT_AMBIENT_R] += lightsDiffuseR[i] / denom;
        contribution[LIGHT_AMBIENT_G] += lightsDiffuseG[i] / denom;
        contribution[LIGHT_AMBIENT_B] += lightsDiffuseB[i] / denom;
      } else {
        // If not ambient, we must deal with direction

        // li is the vector from the vertex to the light
        float lix, liy, liz;
        float lightDir_dot_li = 0;
        float n_dot_li = 0;

        if (lights[i] == DIRECTIONAL) {
          lix = -lightsNX[i];
          liy = -lightsNY[i];
          liz = -lightsNZ[i];
          denom = 1;
          n_dot_li = (nx * lix + ny * liy + nz * liz);
          // If light is lighting the face away from the camera, ditch
          if (n_dot_li <= 0) {
            continue;
          }
        } else { // Point or spot light (must deal also with light location)
          lix = lightsX[i] - wx;
          liy = lightsY[i] - wy;
          liz = lightsZ[i] - wz;
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

          if (lights[i] == SPOT) { // Must deal with spot cone
            lightDir_dot_li =
              -(lightsNX[i] * lix + lightsNY[i] * liy + lightsNZ[i] * liz);
            // Outside of spot cone
            if (lightDir_dot_li <= lightsSpotAngleCos[i]) {
              continue;
            }
            spotTerm = pow(lightDir_dot_li, lightsSpotConcentration[i]);
          }

          if (lightsFalloffQuadratic[i] != 0 || lightsFalloffLinear[i] != 0) {
            // Falloff depends on distance
            denom += lightsFalloffQuadratic[i] * distSq +
              lightsFalloffLinear[i] * (float) sqrt(distSq);
          }
        }
        // Directional, point, or spot light:

        // We know n_dot_li > 0 from above "continues"

        if (denom == 0)
          denom = 1;
        float mul = n_dot_li * spotTerm / denom;
        contribution[LIGHT_DIFFUSE_R] += lightsDiffuseR[i] * mul;
        contribution[LIGHT_DIFFUSE_G] += lightsDiffuseG[i] * mul;
        contribution[LIGHT_DIFFUSE_B] += lightsDiffuseB[i] * mul;

        // SPECULAR

        // If the material and light have a specular component.
        if ((sr > 0 || sg > 0 || sb > 0) &&
            (lightsSpecularR[i] > 0 ||
             lightsSpecularG[i] > 0 ||
             lightsSpecularB[i] > 0)) {

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
            s_dot_n = pow(s_dot_n, shine);
            mul = s_dot_n * spotTerm / denom;
            contribution[LIGHT_SPECULAR_R] += lightsSpecularR[i] * mul;
            contribution[LIGHT_SPECULAR_G] += lightsSpecularG[i] * mul;
            contribution[LIGHT_SPECULAR_B] += lightsSpecularB[i] * mul;
          }

        }
      }
    }
    /*target[toffset + 0] = min(1, er + dr * diffuse_r);
    target[toffset + 1] = min(1, eg + dg * diffuse_g);
    target[toffset + 2] = min(1, eb + db * diffuse_b);

    target[SPR] = min(1, sr * specular_r);
    target[SPG] = min(1, sg * specular_g);
    target[SPB] = min(1, sb * specular_b);*/
    return;
  }


  // Multiply the lighting contribution into the vertex's colors.
  // Only do this when there is ONE lighting per vertex
  // (MANUAL_VERTEX_NORMAL or SHAPE_NORMAL mode).
  private void apply_lighting_contribution(int vIndex, float[] contribution) {
    float[] v = vertices[vIndex];

    v[R] = min(1, v[ER] + v[AR] * contribution[LIGHT_AMBIENT_R] +
               v[DR] * contribution[LIGHT_DIFFUSE_R]);
    v[G] = min(1, v[EG] + v[AG] * contribution[LIGHT_AMBIENT_G] +
               v[DG] * contribution[LIGHT_DIFFUSE_G]);
    v[B] = min(1, v[EB] + v[AB] * contribution[LIGHT_AMBIENT_R] +
               v[DB] * contribution[LIGHT_DIFFUSE_B]);
    v[A] = min(1, v[DA]);

    v[SPR] = min(1, v[SPR] * contribution[LIGHT_SPECULAR_R]);
    v[SPG] = min(1, v[SPG] * contribution[LIGHT_SPECULAR_G]);
    v[SPB] = min(1, v[SPB] * contribution[LIGHT_SPECULAR_B]);
    v[SPA] = min(1, v[SPA]);

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
    triColor[TRI_SPECULAR_A] = v[SPA];
  }


  private void copy_vertex_color_to_triangle(int triIndex,
                                             int vIndex, int colorIndex,
                                             float[] lightContribution) {
    float[] triColor = triangleColors[triIndex][colorIndex];
    float[] v = vertices[vIndex];

    triColor[TRI_DIFFUSE_R] =
      min(1, v[ER] + v[AR] * lightContribution[LIGHT_AMBIENT_R] +
          v[DR] * lightContribution[LIGHT_DIFFUSE_R]);
    triColor[TRI_DIFFUSE_G] =
      min(1, v[EG] + v[AG] * lightContribution[LIGHT_AMBIENT_G] +
          v[DG] * lightContribution[LIGHT_DIFFUSE_G]);
    triColor[TRI_DIFFUSE_B] =
      min(1, v[EB] + v[AB] * lightContribution[LIGHT_AMBIENT_R] +
          v[DB] * lightContribution[LIGHT_DIFFUSE_B]);
    triColor[TRI_DIFFUSE_A] = min(1, v[DA]);

    triColor[TRI_SPECULAR_R] =
      min(1, v[SPR] * lightContribution[LIGHT_SPECULAR_R]);
    triColor[TRI_SPECULAR_G] =
      min(1, v[SPG] * lightContribution[LIGHT_SPECULAR_G]);
    triColor[TRI_SPECULAR_B] =
      min(1, v[SPB] * lightContribution[LIGHT_SPECULAR_B]);
    triColor[TRI_SPECULAR_A] = min(1, v[SPA]);
  }


  private void light_triangle(int triIndex, float[] lightContribution) {
    int vIndex = triangles[triIndex][VERTEX1];
    copy_vertex_color_to_triangle(triIndex, vIndex, 0, lightContribution);
    vIndex = triangles[triIndex][VERTEX2];
    copy_vertex_color_to_triangle(triIndex, vIndex, 1, lightContribution);
    vIndex = triangles[triIndex][VERTEX3];
    copy_vertex_color_to_triangle(triIndex, vIndex, 2, lightContribution);
  }


  private void crossProduct(float[] u, float[] v, float[] out) {
    out[0] = u[1]*v[2] - u[2]*v[1];
    out[1] = u[2]*v[0] - u[0]*v[2];
    out[2] = u[0]*v[1] - u[1]*v[0];
  }


  private void light_triangle(int triIndex) {
    int vIndex;

    // Handle lighting on, but no lights (in this case, just use emissive)
    // This wont be used currently because lightCount == 0 is don't use lighting
    // at all... So. OK. If that ever changes, use the below:
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
    if (normalMode == MANUAL_VERTEX_NORMAL) {
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

    // If the lighting doesn't depend on the vertex position, do the following:
    // We've already dealt with MANUAL_SHAPE_NORMAL mode before we got into this
    // function, so here we only have to deal with AUTO_NORMAL mode. So we calculate
    // the normal for this triangle, and use that for the lighting
    else if (!lightingDependsOnVertexPosition) {
      vIndex = triangles[triIndex][VERTEX1];
      int vIndex2 = triangles[triIndex][VERTEX2];
      int vIndex3 = triangles[triIndex][VERTEX3];
      float[] dv1 = new float[] {vertices[vIndex2][VX] - vertices[vIndex][VX],
                               vertices[vIndex2][VY] - vertices[vIndex][VY],
                               vertices[vIndex2][VZ] - vertices[vIndex][VZ]};
      float[] dv2 = new float[] {vertices[vIndex3][VX] - vertices[vIndex][VX],
                               vertices[vIndex3][VY] - vertices[vIndex][VY],
                               vertices[vIndex3][VZ] - vertices[vIndex][VZ]};
      float[] norm = new float[3];
      crossProduct(dv1, dv2, norm);
      float nMag = mag(norm[X], norm[Y], norm[Z]);
      if (nMag != 0 && nMag != ONE) {
        norm[X] /= nMag; norm[Y] /= nMag; norm[Z] /= nMag;
      }
      vertices[vIndex][NX] = norm[X];
      vertices[vIndex][NY] = norm[Y];
      vertices[vIndex][NZ] = norm[Z];

      // The true at the end says the normal is already in world coordinates
      calc_lighting_contribution(vIndex, tempLightingContribution, true);
      copy_vertex_color_to_triangle(triIndex, vIndex, 0, tempLightingContribution);
      copy_vertex_color_to_triangle(triIndex, vIndex2, 1, tempLightingContribution);
      copy_vertex_color_to_triangle(triIndex, vIndex3, 2, tempLightingContribution);
    }

    // If lighting is position-dependent
    else {
      if (normalMode == MANUAL_SHAPE_NORMAL) {
        vIndex = triangles[triIndex][VERTEX1];
        vertices[vIndex][NX] = vertices[vertex_start][NX];
        vertices[vIndex][NY] = vertices[vertex_start][NY];
        vertices[vIndex][NZ] = vertices[vertex_start][NZ];
        calc_lighting_contribution(vIndex, tempLightingContribution);
        copy_vertex_color_to_triangle(triIndex, vIndex, 0, tempLightingContribution);

        vIndex = triangles[triIndex][VERTEX2];
        vertices[vIndex][NX] = vertices[vertex_start][NX];
        vertices[vIndex][NY] = vertices[vertex_start][NY];
        vertices[vIndex][NZ] = vertices[vertex_start][NZ];
        calc_lighting_contribution(vIndex, tempLightingContribution);
        copy_vertex_color_to_triangle(triIndex, vIndex, 1, tempLightingContribution);

        vIndex = triangles[triIndex][VERTEX3];
        vertices[vIndex][NX] = vertices[vertex_start][NX];
        vertices[vIndex][NY] = vertices[vertex_start][NY];
        vertices[vIndex][NZ] = vertices[vertex_start][NZ];
        calc_lighting_contribution(vIndex, tempLightingContribution);
        copy_vertex_color_to_triangle(triIndex, vIndex, 2, tempLightingContribution);

      }
      // lighting mode is AUTO_NORMAL
      else {
        vIndex = triangles[triIndex][VERTEX1];
        int vIndex2 = triangles[triIndex][VERTEX2];
        int vIndex3 = triangles[triIndex][VERTEX3];
        float[] dv1 = new float[] {vertices[vIndex2][VX] - vertices[vIndex][VX],
                                 vertices[vIndex2][VY] - vertices[vIndex][VY],
                                 vertices[vIndex2][VZ] - vertices[vIndex][VZ]};
        float[] dv2 = new float[] {vertices[vIndex3][VX] - vertices[vIndex][VX],
                                 vertices[vIndex3][VY] - vertices[vIndex][VY],
                                 vertices[vIndex3][VZ] - vertices[vIndex][VZ]};
        float[] norm = new float[3];
        crossProduct(dv1, dv2, norm);
        float nMag = mag(norm[X], norm[Y], norm[Z]);
        if (nMag != 0 && nMag != ONE) {
          norm[X] /= nMag; norm[Y] /= nMag; norm[Z] /= nMag;
        }
        vertices[vIndex][NX] = norm[X];
        vertices[vIndex][NY] = norm[Y];
        vertices[vIndex][NZ] = norm[Z];
        // The true at the end says the normal is already in world coordinates
        calc_lighting_contribution(vIndex, tempLightingContribution, true);
        copy_vertex_color_to_triangle(triIndex, vIndex, 0, tempLightingContribution);

        vertices[vIndex2][NX] = norm[X];
        vertices[vIndex2][NY] = norm[Y];
        vertices[vIndex2][NZ] = norm[Z];
        // The true at the end says the normal is already in world coordinates
        calc_lighting_contribution(vIndex2, tempLightingContribution, true);
        copy_vertex_color_to_triangle(triIndex, vIndex2, 1, tempLightingContribution);

        vertices[vIndex3][NX] = norm[X];
        vertices[vIndex3][NY] = norm[Y];
        vertices[vIndex3][NZ] = norm[Z];
        // The true at the end says the normal is already in world coordinates
        calc_lighting_contribution(vIndex3, tempLightingContribution, true);
        copy_vertex_color_to_triangle(triIndex, vIndex3, 2, tempLightingContribution);
      }
    }
  }

  protected void handle_lighting() {

    // If the lighting does not depend on vertex position and there is a single
    // normal specified for this shape, go ahead and apply the same lighting
    // contribution to every vertex in this shape (one lighting calc!)
    if (!lightingDependsOnVertexPosition && normalMode == MANUAL_SHAPE_NORMAL) {
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
    pushMatrix();
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
    //normal(0, -1, 0);
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
    popMatrix();

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


  public void pushMatrix() {
    if (!modelview.push()) {
      throw new RuntimeException("too many calls to pushMatrix()");
    }
    // Do this to the inverse regardless of the lights
    // to keep stack pointers in sync
    modelviewInv.push();
  }


  public void popMatrix() {
    if (!modelview.pop()) {
      throw new RuntimeException("too many calls to popMatrix() " +
                                 "(and not enough to pushMatrix)");
    }
    // Do this to the inverse regardless of the lights
    // to keep stack pointers in sync
    modelviewInv.pop();
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
   * Print the current model (or "transformation") matrix.
   */
  public void printMatrix() {
    modelview.print();
  }



  //////////////////////////////////////////////////////////////

  // CAMERA and PERSPECTIVE


  /**
   * Set matrix mode to the camera matrix (instead of
   * the current transformation matrix). This means applyMatrix,
   * resetMatrix, etc. will affect the camera.
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
      throw new RuntimeException("cannot call beginCamera while already "+
                                 "in camera manipulation mode");
    } else {
      manipulatingCamera = true;
      forwardTransform = cameraInv;
      reverseTransform = camera;
    }
  }


  /**
   * Record the current settings into the camera matrix.
   * And set the matrix mode back to the current
   * transformation matrix.
   * <P>
   * Note that this will destroy any settings to scale(),
   * translate() to your scene, because the final camera
   * matrix will be copied (not multiplied) into the modelview.
   */
  public void endCamera() {
    if (!manipulatingCamera) {
      throw new RuntimeException("cannot call endCamera while not "+
                                 "in camera manipulation mode");
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
   * Calls camera() with Processing's standard camera setup.
   */
  public void camera() {
    camera(cameraX, cameraY, cameraZ,
           cameraX, cameraY, 0,
           0, 1, 0);
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
    cameraInv.invTranslate(-eyeX, -eyeY, -eyeZ);

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
   * This behavior is pretty much familiar from OpenGL.
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
    float ymax = zNear * tan(fov / 2.0f);
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
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;
    return (aw != 0) ? ax / aw : ax;
  }


  public float modelY(float x, float y, float z) {
    float ay =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;
    return (aw != 0) ? ay / aw : ay;
  }


  public float modelZ(float x, float y, float z) {
    float az =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
    float aw =
      modelview.m30*x + modelview.m31*y + modelview.m32*z + modelview.m33;
    return (aw != 0) ? az / aw : az;
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

    for (int i = 0; i < pixelCount; i++) {
      zbuffer[i] = MAX_FLOAT;
      stencil[i] = 0;
    }
  }


  /**
   * Clears pixel buffer.
   * <P>
   * With P3D and OPENGL, this also clears the stencil and zbuffer.
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

  // SMOOTH (not available, throws error)

  // although should this bother throwing an error?
  // could be a pain in the ass when trying to debug with opengl


  public void smooth() {
    String msg = "smooth() not available with P3D";
    throw new RuntimeException(msg);
  }


  public void noSmooth() {
    String msg = "noSmooth() not available with P3D";
    throw new RuntimeException(msg);
  }


  //////////////////////////////////////////////////////////////


  public void fill(int rgb) {
    super.fill(rgb);
    colorAmbient();
  }

  public void fill(float gray) {
    super.fill(gray);
    colorAmbient();
  }


  public void fill(float gray, float alpha) {
    super.fill(gray, alpha);
    colorAmbient();
  }


  public void fill(float x, float y, float z) {
    super.fill(x, y, z);
    colorAmbient();
  }


  public void fill(float x, float y, float z, float a) {
    super.fill(x, y, z, a);
    colorAmbient();
  }


  //////////////////////////////////////////////////////////////


  public void ambient(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      ambient((float) rgb);

    } else {
      colorFrom(rgb);
      colorAmbient();
    }
  }


  public void ambient(float gray) {
    colorCalc(gray);
    colorAmbient();
  }


  public void ambient(float x, float y, float z) {
    colorCalc(x, y, z);
    colorAmbient();
  }


  private void colorAmbient() {
    ambientR = calcR;
    ambientG = calcG;
    ambientB = calcB;
    ambientRi = calcRi;
    ambientGi = calcGi;
    ambientBi = calcBi;
  }


  //////////////////////////////////////////////////////////////


  public void specular(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      specular((float) rgb);

    } else {
      colorFrom(rgb);
      colorSpecular();
    }
  }


  public void specular(float gray) {
    colorCalc(gray);
    colorSpecular();
  }


  public void specular(float gray, float alpha) {
    colorCalc(gray, alpha);
    colorSpecular();
  }


  public void specular(float x, float y, float z) {
    colorCalc(x, y, z);
    colorSpecular();
  }


  public void specular(float x, float y, float z, float a) {
    colorCalc(x, y, z, a);
    colorSpecular();
  }


  protected void colorSpecular() {
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
      colorFrom(rgb);
      colorEmissive();
    }
  }


  public void emissive(float gray) {
    colorCalc(gray);
    colorEmissive();
  }


  public void emissive(float x, float y, float z) {
    colorCalc(x, y, z);
    colorEmissive();
  }


  protected void colorEmissive() {
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
   * Add an ambient light based on the current color mode.
   */
  public void ambientLight(float r, float g, float b, float x, float y, float z) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightsDiffuseR[lightCount] = calcR;
    lightsDiffuseG[lightCount] = calcG;
    lightsDiffuseB[lightCount] = calcB;

    lights[lightCount] = AMBIENT;
    lightsFalloffConstant[lightCount] = lightFalloffConstant;
    lightsFalloffLinear[lightCount] = lightFalloffLinear;
    lightsFalloffQuadratic[lightCount] = lightFalloffQuadratic;
    lightPosition(lightCount, x, y, z);
    lightCount++;
    //return lightCount-1;
  }

  public void ambientLight(float r, float g, float b) {
    ambientLight(r, g, b, 0, 0, 0);
  }


  public void directionalLight(float r, float g, float b,
                               float nx, float ny, float nz) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightsDiffuseR[lightCount] = calcR;
    lightsDiffuseG[lightCount] = calcG;
    lightsDiffuseB[lightCount] = calcB;

    //light[lightCount] = true;
    lights[lightCount] = DIRECTIONAL;
    lightsFalloffConstant[lightCount] = lightFalloffConstant;
    lightsFalloffLinear[lightCount] = lightFalloffLinear;
    lightsFalloffQuadratic[lightCount] = lightFalloffQuadratic;
    lightsSpecularR[lightCount] = lightSpecularR;
    lightsSpecularG[lightCount] = lightSpecularG;
    lightsSpecularB[lightCount] = lightSpecularB;
    lightDirection(lightCount, nx, ny, nz);
    lightCount++;
    //return lightCount-1;
  }


  public void pointLight(float r, float g, float b,
                         float x, float y, float z) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightsDiffuseR[lightCount] = calcR;
    lightsDiffuseG[lightCount] = calcG;
    lightsDiffuseB[lightCount] = calcB;

    //light[lightCount] = true;
    lights[lightCount] = POINT;
    lightsFalloffConstant[lightCount] = lightFalloffConstant;
    lightsFalloffLinear[lightCount] = lightFalloffLinear;
    lightsFalloffQuadratic[lightCount] = lightFalloffQuadratic;
    lightsSpecularR[lightCount] = lightSpecularR;
    lightsSpecularG[lightCount] = lightSpecularG;
    lightsSpecularB[lightCount] = lightSpecularB;
    lightPosition(lightCount, x, y, z);
    lightCount++;

    lightingDependsOnVertexPosition = true;
    //return lightCount-1;
  }


  public void spotLight(float r, float g, float b,
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float angle, float concentration) {
    if (lightCount == MAX_LIGHTS) {
      throw new RuntimeException("can only create " + MAX_LIGHTS + " lights");
    }
    colorCalc(r, g, b);
    lightsDiffuseR[lightCount] = calcR;
    lightsDiffuseG[lightCount] = calcG;
    lightsDiffuseB[lightCount] = calcB;

    lights[lightCount] = SPOT;
    lightsFalloffConstant[lightCount] = lightFalloffConstant;
    lightsFalloffLinear[lightCount] = lightFalloffLinear;
    lightsFalloffQuadratic[lightCount] = lightFalloffQuadratic;
    lightsSpecularR[lightCount] = lightSpecularR;
    lightsSpecularG[lightCount] = lightSpecularG;
    lightsSpecularB[lightCount] = lightSpecularB;
    lightPosition(lightCount, x, y, z);
    lightDirection(lightCount, nx, ny, nz);
    lightsSpotAngle[lightCount] = angle;
    lightsSpotAngleCos[lightCount] = max(0, cos(angle));
    lightsSpotConcentration[lightCount] = concentration;
    lightCount++;

    lightingDependsOnVertexPosition = true;
  }


  /**
   * Set the light falloff rates for the last light that was created.
   * Default is lightFalloff(1, 0, 0).
   */
  public void lightFalloff(float constant, float linear, float quadratic) {
    lightFalloffConstant = constant;
    lightFalloffLinear = linear;
    lightFalloffQuadratic = quadratic;

    lightingDependsOnVertexPosition = true;
  }


  /**
   * Set the specular color of the last light created.
   */
  public void lightSpecular(float x, float y, float z) {
    colorCalc(x, y, z);
    lightSpecularR = calcR;
    lightSpecularG = calcG;
    lightSpecularB = calcB;

    lightingDependsOnVertexPosition = true;
  }


  /**
   * internal function to set the light position
   * based on the current modelview matrix.
   */
  protected void lightPosition(int num, float x, float y, float z) {
    lightsX[num] =
      modelview.m00*x + modelview.m01*y + modelview.m02*z + modelview.m03;
    lightsY[num] =
      modelview.m10*x + modelview.m11*y + modelview.m12*z + modelview.m13;
    lightsZ[num] =
      modelview.m20*x + modelview.m21*y + modelview.m22*z + modelview.m23;
  }


  /**
   * internal function to set the light direction
   * based on the current modelview matrix.
   */
  protected void lightDirection(int num, float x, float y, float z) {
    // Multiply by inverse transpose.
    lightsNX[num] =
      modelviewInv.m00*x + modelviewInv.m10*y +
      modelviewInv.m20*z + modelviewInv.m30;
    lightsNY[num] =
      modelviewInv.m01*x + modelviewInv.m11*y +
      modelviewInv.m21*z + modelviewInv.m31;
    lightsNZ[num] =
      modelviewInv.m02*x + modelviewInv.m12*y +
      modelviewInv.m22*z + modelviewInv.m32;

    float norm = mag(lightsNX[num], lightsNY[num], lightsNZ[num]);
    if (norm == 0 || norm == 1) return;

    lightsNX[num] /= norm;
    lightsNY[num] /= norm;
    lightsNZ[num] /= norm;
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
    //if (angleMode == DEGREES) angle *= DEG_TO_RAD;
    return (float)Math.sin(angle);
  }

  private final float cos(float angle) {
    //if (angleMode == DEGREES) angle *= DEG_TO_RAD;
    return (float)Math.cos(angle);
  }

  private final float tan(float angle) {
    //if (angleMode == DEGREES) angle *= DEG_TO_RAD;
    return (float)Math.tan(angle);
  }

  private float dot(float ax, float ay, float az,
                    float bx, float by, float bz) {
    return ax * bx + ay * by + az * bz;
  }
}

