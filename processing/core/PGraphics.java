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

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;


public class PGraphics extends PImage implements PMethods, PConstants {

  /// width minus one (useful for many calculations)
  public int width1;

  /// height minus one (useful for many calculations)
  public int height1;

  /// width * height (useful for many calculations)
  public int pixelCount;

  /// the stencil buffer (only for NEW_GRAPHICS)
  public int stencil[];

  /// zbuffer (only when 3D is in use)
  public float zbuffer[];

  // ........................................................

  // specifics for java memoryimagesource
  DirectColorModel cm;
  MemoryImageSource mis;
  public Image image;

  // ........................................................

  // needs to happen before background() is called
  // and resize.. so it's gotta be outside
  boolean hints[] = new boolean[HINT_COUNT];


  // ........................................................

  // underscored_names are used for private functions or variables

  /** The current colorMode */
  public int colorMode;

  /** Max value for red (or hue) set by colorMode */
  public float colorModeX;

  /** Max value for green (or saturation) set by colorMode */
  public float colorModeY;

  /** Max value for blue (or value) set by colorMode */
  public float colorModeZ;

  /** Max value for alpha set by colorMode */
  public float colorModeA;

  /** True if colors are not in the range 0..1 */
  boolean colorScale;

  /** True if colorMode(RGB, 255) */
  boolean colorRgb255;

  /** True if tint() is enabled, read-only */
  public boolean tint;

  /** Tint that was last set, read-only */
  public int tintColor;

  /** True if the tint has an alpha value */
  boolean tintAlpha;

  public float tintR, tintG, tintB, tintA;
  public int tintRi, tintGi, tintBi, tintAi;

  // fill color
  public boolean fill;
  public int fillColor;
  public boolean fillAlpha;
  public float fillR, fillG, fillB, fillA;
  public int fillRi, fillGi, fillBi, fillAi;

  // stroke color
  public boolean stroke;
  boolean strokeAlpha;
  public float strokeR, strokeG, strokeB, strokeA;
  public int strokeRi, strokeGi, strokeBi, strokeAi;
  public int strokeColor;

  //public boolean background;
  /** Last background color that was set */
  public int backgroundColor;
  public float backgroundR, backgroundG, backgroundB;
  public int backgroundRi, backgroundGi, backgroundBi;

  // internal color for setting/calculating
  float calcR, calcG, calcB, calcA;
  int calcRi, calcGi, calcBi, calcAi;
  int calcColor;
  boolean calcAlpha;

  /** The last rgb value converted to HSB */
  int cacheHsbKey;
  /** Result of the last conversion to HSB */
  float cacheHsbValue[] = new float[3]; // inits to zero

  /** True if depth() is enabled, read-only */
  public boolean depth;

  /** Set by strokeWeight(), read-only */
  public float strokeWeight;

  /** Set by strokeJoin(), read-only */
  public int strokeJoin;

  /** Set by strokeCap(), read-only */
  public int strokeCap;

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

  /**
   * Model transformation of the form m[row][column],
   * which is a "column vector" (as opposed to "row vector") matrix.
   */
  public float m00, m01, m02, m03;
  public float m10, m11, m12, m13;
  public float m20, m21, m22, m23;
  public float m30, m31, m32, m33;

  public int angleMode;

  static final int MATRIX_STACK_DEPTH = 32;
  float matrixStack[][] = new float[MATRIX_STACK_DEPTH][16];
  int matrixStackDepth;

  // ........................................................

  public int cameraMode;
  //public int dimensions;  // 0, 2 (affine 2d), 3 (perspective/isometric)

  // perspective setup
  public float cameraFOV;
  public float cameraEyeX, cameraEyeY;
  public float cameraEyeDist, cameraNearDist, cameraFarDist;
  public float cameraAspect;

  public float p00, p01, p02, p03; // projection matrix
  public float p10, p11, p12, p13;
  public float p20, p21, p22, p23;
  public float p30, p31, p32, p33;

  // ........................................................

  // shapes

  /**
   * Type of shape passed to beginShape(),
   * zero if no shape is currently being drawn.
   */
  int shape;


  // ........................................................

  // OLD_GRAPHICS

  //protected PPolygon polygon;     // general polygon to use for shape
  //PPolygon fpolygon;    // used to fill polys for tri or quad strips
  PPolygon spolygon;    // stroke/line polygon
  //float svertices[][];  // temp vertices used for stroking end of poly

  //PPolygon tpolygon;    // for calculating concave/convex
  //int TPOLYGON_MAX_VERTICES = 512;
  //int tpolygon_vertex_order[]; // = new int[MAX_VERTICES];

  // ........................................................

  // NEW_GRAPHICS

  int shape_index;

  // vertices
  static final int DEFAULT_VERTICES = 512;
  public float vertices[][] = new float[DEFAULT_VERTICES][VERTEX_FIELD_COUNT];
  int vertex_count; // total number of vertices
  int vertex_start; // pos of first vertex of current shape in vertices array
  int vertex_end;   // total number of vertex in current shape
  // used for sorting points when triangulating a polygon
  // warning - maximum number of vertices for a polygon is DEFAULT_VERTICES
  int vertex_order[] = new int[DEFAULT_VERTICES];

  // lines
  static final int DEFAULT_LINES = 512;
  PLine line;  // used for drawing
  public int lines[][] = new int[DEFAULT_LINES][LINE_FIELD_COUNT];
  public int lineCount;

  // triangles
  static final int DEFAULT_TRIANGLES = 256;
  PTriangle triangle; // used for rendering
  public int triangles[][] = new int[DEFAULT_TRIANGLES][TRIANGLE_FIELD_COUNT];
  public int triangleCount;   // total number of triangles

  /**
   * Normals
   */
  public float normalX, normalY, normalZ;

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

  // used by NEW_GRAPHICS, or by OLD_GRAPHICS simply as a boolean
  public PImage textureImage;

  static final int DEFAULT_TEXTURES = 3;
  PImage textures[] = new PImage[DEFAULT_TEXTURES];
  int texture_index;


  // ........................................................

  // changes

  //boolean unchangedZ;
  boolean strokeChanged;
  boolean fillChanged;
  protected boolean normalChanged;


  // ........................................................

  // spline vertices

  static final int SPLINE_VERTEX_ALLOC = 128;
  float spline_vertex[][];
  int spline_vertex_index;
  boolean spline_vertices_flat;


  // ........................................................

  // precalculate sin/cos lookup tables [toxi]
  // circle resolution is determined from the actual used radii
  // passed to ellipse() method. this will automatically take any
  // scale transformations into account too

  // [toxi 031031]
  // changed table's precision to 0.5 degree steps
  // introduced new vars for more flexible code
  static final float sinLUT[], cosLUT[];
  static final float SINCOS_PRECISION = 0.5f;
  static final int SINCOS_LENGTH=(int)(360f/SINCOS_PRECISION);
  static {
    sinLUT = new float[SINCOS_LENGTH];
    cosLUT = new float[SINCOS_LENGTH];
    for (int i=0; i<SINCOS_LENGTH; i++) {
      sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
      cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
    }
  }


  // ........................................................


  public int rectMode;
  public int ellipseMode;
  public int arcMode;

  // [toxi031031] new & faster sphere code w/ support flexibile resolutions
  // will be set by sphereDetail() or 1st call to sphere()
  public int sphereDetail = 0;
  float sphereX[], sphereY[], sphereZ[];

  //int text_mode;
  //int text_space;
  public PFont textFont;

  // used by PFont/PGraphics.. forces higher quality texture rendering
  //boolean drawing_text = false;  // used by PFont


  //////////////////////////////////////////////////////////////

  // INTERNAL


  /**
   * Constructor for the PGraphics object.
   * This prototype only exists because of annoying
   * java compilers, and should not be used.
   */
  public PGraphics() { }


  /**
   * Constructor for the PGraphics object. Use this to ensure that
   * the defaults get set properly. In a subclass, use this(w, h)
   * as the first line of a subclass' constructor to properly set
   * the internal fields and defaults.
   *
   * @param  iwidth   viewport width
   * @param  iheight  viewport height
   */
  public PGraphics(int iwidth, int iheight) {
    resize(iwidth, iheight);

    // init color/stroke/fill
    defaults();

    // clear geometry for loading later
    //circleX = null;  // so that bagel knows to init these
    //sphereX = null;  // diff from cpp b/c mem in cpp is preallocated
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

    // init perspective projection based on new dimensions
    cameraFOV = 60; // at least for now
    cameraEyeX = width / 2.0f;
    cameraEyeY = height / 2.0f;
    cameraEyeDist = cameraEyeY / ((float) tan(PI * cameraFOV / 360f));
    cameraNearDist = cameraEyeDist / 10.0f;
    cameraFarDist = cameraEyeDist * 10.0f;
    cameraAspect = (float)width / (float)height;

    // reset the cameraMode if PERSPECTIVE or ORTHOGRAPHIC
    // otherwise just hose the user if it's custom
    if (depth) cameraMode(this.cameraMode);
  }


  // broken out because of subclassing for opengl
  protected void allocate() {
    pixelCount = width * height;
    pixels = new int[pixelCount];

    // because of a java 1.1 bug.. unless pixels are registered as
    // opaque before their first run, the memimgsrc will flicker
    // and run very slowly.
    for (int i = 0; i < pixelCount; i++) pixels[i] = 0xffffffff;

    // setup MemoryImageSource
    cm = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff);;
    mis = new MemoryImageSource(width, height, pixels, 0, width);
    mis.setFullBufferUpdates(true); // does this help ipaq?
    mis.setAnimated(true);
    image = Toolkit.getDefaultToolkit().createImage(mis);

    zbuffer = new float[pixelCount];
    stencil = new int[pixelCount];

    line = new PLine(this);
    triangle = new PTriangle(this);

    // init lights
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
  }


  /**
   *  set engine's default values
   */
  public void defaults() {
    colorMode(RGB, TFF);
    fill(TFF);
    stroke(0);
    strokeWeight(ONE);
    background(204);

    // init shape stuff
    shape = 0;

    // flat or affine stuff
    noDepth();

    // better to leave this turned off by default
    noLights();

    // init matrices (must do before lights)
    matrixStackDepth = 0;

    lightEnable(0);
    lightAmbient(0, 0, 0, 0);

    light(1, cameraEyeX, cameraEyeY, cameraEyeDist, 255, 255, 255);

    textureMode = IMAGE_SPACE;
    rectMode    = CORNER;
    ellipseMode = CENTER;
    arcMode     = CENTER;
    angleMode   = RADIANS;

    // no current font
    textFont = null;
    //text_mode    = ALIGN_LEFT;
    //text_space   = OBJECT_SPACE;
  }



  //////////////////////////////////////////////////////////////

  // FRAME


  /**
   * Initializes engine before drawing a new frame.
   * Called by PApplet, no need to call this.
   */
  public void beginFrame() {
    resetMatrix(); // reset model matrix
    normal(0, 0, 1);

    // reset shapes
    shape_index = 0;

    // reset vertices
    vertex_count = 0;
    vertex_start = 0;
    vertex_end = 0;

    // reset lines
    lineCount = 0;
    line.reset();

    // reset triangles
    triangleCount = 0;
    triangle.reset();

    // reset textures
    texture_index = 0;
  }


  /**
   * Indicates a completed frame.
   * Finishes rendering and swaps the buffer to the screen.
   *
   * If z-sorting has been turned on, then the triangles will
   * all be quicksorted here (to make alpha work more properly)
   * and then blit to the screen.
   */
  public void endFrame() {
    // no need to z order and render
    // shapes were already rendered in endShape();
    // (but can't return, since needs to update memimgsrc
    if (hints[DEPTH_SORT]) {
      if (triangleCount > 0) {
        //depth_sort_triangles();  // not yet
        render_triangles();
      }
      if (lineCount > 0) {
        //depth_sort_lines();  // not yet
        render_lines();
      }
    }

    // moving this back here (post-68) because of macosx thread problem
    mis.newPixels(pixels, cm, 0, width);
  }



  //////////////////////////////////////////////////////////////

  // SHAPES

  /**
   * Start a new shape of type POLYGON
   */
  public void beginShape() {
    beginShape(POLYGON);
  }


  /**
   * Start a new shape.
   *
   * @param kind indicates shape type
   */
  public void beginShape(int kind) {
    shape = kind;

    shape_index = shape_index + 1;
    if (shape_index == -1) {
      shape_index = 0;
    }

    if (hints[DEPTH_SORT]) {
      // continue with previous vertex, line and triangle count
      // all shapes are rendered at endFrame();
      vertex_start = vertex_count;
      vertex_end = 0;

    } else {
      // reset vertex, line and triangle information
      // every shape is rendered at endShape();
      vertex_count = 0;
      line.reset();
      lineCount = 0;
      triangle.reset();
      triangleCount = 0;
    }
    textureImage = null;

    spline_vertex_index = 0;
    spline_vertices_flat = true;

    //unchangedZ = true;
    strokeChanged = false;
    fillChanged = false;
    normalChanged = false;
  }


  /**
   * Sets the current normal. Mostly will apply to vertices
   * inside a beginShape/endShape block.
   */
  public void normal(float nx, float ny, float nz) {
    // if drawing a shape and the normal hasn't changed yet,
    // then need to set all the normal for each vertex so far
    if ((shape != 0) && !normalChanged) {
      for (int i = vertex_start; i < vertex_end; i++) {
        vertices[i][NX] = normalX;
        vertices[i][NY] = normalY;
        vertices[i][NZ] = normalZ;
      }
      normalChanged = true;
    }
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

    //add_texture(image);
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
    //} else {
    //triangle.setTexture(image);
    //}
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
    if (vertex_count == vertices.length) {
      float temp[][] = new float[vertex_count<<1][VERTEX_FIELD_COUNT];
      System.arraycopy(vertices, 0, temp, 0, vertex_count);
      vertices = temp;
      message(CHATTER, "allocating more vertices " + vertices.length);
    }
    float vertex[] = vertices[vertex_count++];

    //if (polygon.redundantVertex(x, y, z)) return;

    // user called vertex(), so that invalidates anything queued
    // up for curve vertices. if this is internally called by
    // spline_segment, then spline_vertex_index will be saved and restored.
    spline_vertex_index = 0;

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

    if (normalChanged) {
      vertex[NX] = normalX;
      vertex[NY] = normalY;
      vertex[NZ] = normalZ;
    }
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
    if (spline_vertex == null) {
      spline_vertex = new float[SPLINE_VERTEX_ALLOC][VERTEX_FIELD_COUNT];
    }

    // if more than 128 points, shift everything back to the beginning
    if (spline_vertex_index == SPLINE_VERTEX_ALLOC) {
      System.arraycopy(spline_vertex[SPLINE_VERTEX_ALLOC-3], 0,
                       spline_vertex[0], 0, VERTEX_FIELD_COUNT);
      System.arraycopy(spline_vertex[SPLINE_VERTEX_ALLOC-2], 0,
                       spline_vertex[1], 0, VERTEX_FIELD_COUNT);
      System.arraycopy(spline_vertex[SPLINE_VERTEX_ALLOC-1], 0,
                       spline_vertex[2], 0, VERTEX_FIELD_COUNT);
      spline_vertex_index = 3;
    }

    // 'flat' may be a misnomer here because it's actually just
    // calculating whether z is zero for all the spline points,
    // so that it knows whether to calculate all three params,
    // or just two for x and y.
    if (spline_vertices_flat) {
      if (z != 0) spline_vertices_flat = false;
    }
    float vertex[] = spline_vertex[spline_vertex_index];

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

    if (normalChanged) {
      vertex[NX] = normalX;
      vertex[NY] = normalY;
      vertex[NZ] = normalZ;
    }

    spline_vertex_index++;

    // draw a segment if there are enough points
    if (spline_vertex_index > 3) {
      if (bezier) {
        if ((spline_vertex_index % 4) == 0) {
          if (!bezier_inited) bezier_init();

          if (spline_vertices_flat) {
            spline2_segment(spline_vertex_index-4,
                            spline_vertex_index-4,
                            bezier_draw,
                            bezier_detail);
          } else {
            spline3_segment(spline_vertex_index-4,
                            spline_vertex_index-4,
                            bezier_draw,
                            bezier_detail);
          }
        }
      } else {  // catmull-rom curve (!bezier)
        if (!curve_inited) curve_init();

        if (spline_vertices_flat) {
          spline2_segment(spline_vertex_index-4,
                          spline_vertex_index-3,
                          curve_draw,
                          curve_detail);
        } else {
          spline3_segment(spline_vertex_index-4,
                          spline_vertex_index-3,
                          curve_draw,
                          curve_detail);
        }
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
    // clear the 'shape drawing' flag in case of early exit
    //shape = false;

    //System.out.println("ending shape");

    vertex_end = vertex_count;

    // ------------------------------------------------------------------
    // CREATE LINES

    int increment = 1;
    int stop = 0;
    int counter = 0;

    // make lines for both stroke triangles
    // and antialiased triangles
    //boolean check = stroke; // || smooth;

    // quick fix [rocha]
    // antialiasing fonts with lines causes some artifacts
    //if (textureImage != null && textureImage.format == ALPHA) {
    //check = false;
    //}

    if (stroke) {
      switch (shape) {

        case POINTS:
        {
          stop = vertex_end;
          for (int i = vertex_start; i < stop; i++) {
            add_line(i,i);
          }
        }
        break;

        case LINES:
        case LINE_STRIP:
        case LINE_LOOP:
        {
          // store index of first vertex
          int first = lineCount;
          stop = vertex_end-1;
          increment = (shape == LINES) ? 2 : 1;

          for (int i = vertex_start; i < stop; i+=increment) {
            add_line(i,i+1);
          }

          if (shape == LINE_LOOP) {
            add_line(stop,lines[first][PA]);
          }
        }
        break;

        case TRIANGLES:
        case TRIANGLE_STRIP:
        {
          // first draw all vertices as a line strip
          stop = vertex_end-1;

          for (int i = vertex_start; i < stop; i++) {
            counter = i - vertex_start;
            add_line(i,i+1);
            if ((shape == TRIANGLES) && (counter%3 == 1)) {
              i++;
            }
          }

          // then draw from vertex (n) to (n+2)
          stop = vertex_end-2;
          increment = (shape == TRIANGLE_STRIP) ? 1 : 3;

          for (int i = vertex_start; i < stop; i+=increment) {
            add_line(i,i+2);
          }
        }
        break;

        case QUADS:
        case QUAD_STRIP:
        {
          // first draw all vertices as a line strip
          stop = vertex_end - 1;

          for (int i = vertex_start; i < stop; i++) {
            counter = i - vertex_start;
            add_line(i,i+1);
            if ((shape == QUADS) && (counter%4 == 2)) {
              i++;
            }
          }

          // then draw from vertex (n) to (n+3)
          stop = vertex_end-2;
          increment = (shape == QUAD_STRIP) ? 2 : 4;

          for (int i=vertex_start; i < stop; i+=increment) {
            add_line(i,i+3);
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

          for (int i=vertex_start; i < stop; i++) {
            add_line(i,i+1);
          }
          // draw the last line connecting back to the first point in poly
          add_line(stop,lines[first][PA]);
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
    // 2D POINTS FROM MODEL (MX, MY, MZ) DIRECTLY TO VIEW SPACE (X, Y, Z)

    if (!depth) {
      // if no depth in use, then the points can be transformed
      for (int i = vertex_start; i < vertex_end; i++) {
        vertices[i][X] = m00*vertices[i][MX] + m01*vertices[i][MY] + m03;
        vertices[i][Y] = m10*vertices[i][MX] + m11*vertices[i][MY] + m13;
      }
    }

    // ------------------------------------------------------------------
    // 3D POINTS FROM MODEL (MX, MY, MZ) TO VIEW SPACE (VX, VY, VZ)

    if (depth) {
      for (int i = vertex_start; i < vertex_end; i++) {
        float vertex[] = vertices[i];

        vertex[VX] = m00*vertex[MX] + m01*vertex[MY] + m02*vertex[MZ] + m03;
        vertex[VY] = m10*vertex[MX] + m11*vertex[MY] + m12*vertex[MZ] + m13;
        vertex[VZ] = m20*vertex[MX] + m21*vertex[MY] + m22*vertex[MZ] + m23;
        vertex[VW] = m30*vertex[MX] + m31*vertex[MY] + m32*vertex[MZ] + m33;
      }
    }


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

    if (!normalChanged) {
      // fill first vertext w/ the normal
      vertices[vertex_start][NX] = normalX;
      vertices[vertex_start][NY] = normalY;
      vertices[vertex_start][NZ] = normalZ;
      // homogenousNormals saves time from below, which is expensive
    }

    for (int i = vertex_start; i < (normalChanged ? vertex_end : 1); i++) {
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
        if (normalChanged) {
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
        } else {
          if (fill) {
            calc_lighting(v[R],  v[G],  v[B],
                          v[MX], v[MY], v[MZ],
                          f[NX], f[NY], f[NZ], v, R);
          }
          if (stroke) {
            calc_lighting(v[SR], v[SG], v[SB],
                          v[MX], v[MY], v[MZ],
                          f[NX], f[NY], f[NZ], v, SR);
          }
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

    //if ((cameraMode == PERSPECTIVE) && (dimensions == 3)) {
    if (depth) {
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

    // ------------------------------------------------------------------
    // RENDER SHAPES FILLS HERE WHEN NOT DEPTH SORTING

    // if true, the shapes will be rendered on endFrame
    if (hints[DEPTH_SORT]) {
      shape = 0;
      return;
    }

    if (fill) render_triangles();
    if (stroke) render_lines();

    /*
    // render all triangles in current shape
    if (fill) {
      for (int i = 0; i < triangleCount; i ++) {
        float a[] = vertices[triangles[i][VA]];
        float b[] = vertices[triangles[i][VB]];
        float c[] = vertices[triangles[i][VC]];
        int index = triangles[i][TI];

        if (textureImage != null) {
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

    // ------------------------------------------------------------------
    // DRAW POINTS, LINES AND SHAPE STROKES

    // draw all lines in current shape
    if (stroke) {

      for (int i = 0; i < lineCount; i ++) {
        float a[] = vertices[lines[i][PA]];
        float b[] = vertices[lines[i][PB]];
        int index = lines[i][LI];

        line.setIntensities(a[SR], a[SG], a[SB], a[SA],
                            b[SR], b[SG], b[SB], b[SA]);

        line.setVertices( a[X], a[Y], a[Z],
                          b[X], b[Y], b[Z]);

        line.setIndex(index);

        line.draw();
        //System.out.println("shoudla drawn");
      }
    }
    */

    //System.out.println("leaving endShape");
    //shapeKind = 0;
    shape = 0;
  }


  protected final void add_line(int a, int b) {
    if (lineCount == lines.length) {
      int temp[][] = new int[lineCount<<1][LINE_FIELD_COUNT];
      System.arraycopy(lines, 0, temp, 0, lineCount);
      lines = temp;
      message(CHATTER, "allocating more lines " + lines.length);
    }
    lines[lineCount][PA] = a;
    lines[lineCount][PB] = b;
    lines[lineCount][LI] = -1;

    lines[lineCount][SM] = strokeCap | strokeJoin;
    lineCount++;
  }


  protected final void add_triangle(int a, int b, int c) {
    if (triangleCount == triangles.length) {
      int temp[][] = new int[triangleCount<<1][TRIANGLE_FIELD_COUNT];
      System.arraycopy(triangles, 0, temp, 0, triangleCount);
      triangles = temp;
      message(CHATTER, "allocating more triangles " + triangles.length);
    }
    triangles[triangleCount][VA] = a;
    triangles[triangleCount][VB] = b;
    triangles[triangleCount][VC] = c;

    if (textureImage == null) {
      triangles[triangleCount][TEX] = -1;
    } else {
      triangles[triangleCount][TEX] = texture_index;
    }

    triangles[triangleCount][TI] = shape_index;
    triangleCount++;
  }


  //protected void depth_sort_triangles() {
  //}

  protected void render_triangles() {
    for (int i = 0; i < triangleCount; i ++) {
      float a[] = vertices[triangles[i][VA]];
      float b[] = vertices[triangles[i][VB]];
      float c[] = vertices[triangles[i][VC]];
      int tex = triangles[i][TEX];
      int index = triangles[i][TI];

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


  //protected void depth_sort_lines() {
  //}

  public void render_lines() {
    for (int i = 0; i < lineCount; i ++) {
      float a[] = vertices[lines[i][PA]];
      float b[] = vertices[lines[i][PB]];
      int index = lines[i][LI];

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



  //////////////////////////////////////////////////////////////

  // POINT


  public void point(float x, float y) {
    point(x, y, 0);
  }


  public void point(float x, float y, float z) {
    if (depth) {
      if (strokeWeight < 2) {
        // just a single dot on the screen with a z value
        // TODO what is lighting calculation for this point?
        point0(screenX(x, y, z),
               screenY(x, y, z),
               screenZ(x, y, z), strokeColor);

      } else {
        float cx = screenX(x, y, z);
        float cy = screenX(x, y, z);
        float hsw = strokeWeight / 2f;

        if (strokeCap == ROUND_ENDCAP) {
          // non-smoothed, filled circle
          circle0_rough_fill(cx, cy, z, hsw, strokeColor);

        } else {  // otherwise one of the square endcaps
          //if ((strokeCap == PROJECTED_ENDCAP) ||
          //    (strokeCap == SQUARE_ENDCAP)) {
          // technically, if SQUARE_ENDCAP, nothing should be drawn
          // but we'll go easy on the lads
          // non-smoothed (since 3D), filled square

          int x1 = (int) (cx - hsw + 0.5f);
          int y1 = (int) (cy - hsw + 0.5f);
          int x2 = (int) (cx + hsw + 0.5f);
          int y2 = (int) (cy + hsw + 0.5f);

          rect0_rough_fill(x1, y1, x2, y2, z, strokeColor);
        }
      }

    } else {  // noDepth
      if (strokeWeight < 2) {
        point0(screenX(x, y), screenY(x, y), 0, strokeColor);

      } else {
        float hsw = strokeWeight / 2f;

        if ((strokeCap == PROJECTED_ENDCAP) ||
            (strokeCap == SQUARE_ENDCAP)) {
          rect0_fill(x - hsw, y - hsw, x + hsw, y + hsw, 0, strokeColor);

        } else if (strokeCap == ROUND_ENDCAP) {
          circle0_fill(x - hsw, y - hsw, 0, hsw, strokeColor);
        }
      }
    }
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
      point0(xx, yy, z, color);
      zbuffer[yy*width + xx] = screenZ(x, y, z);
      //stencil?

    } else {
      // actually has some weight, need to draw shapes instead
      // these will be
    }
  }


  private void point2(float x, float y, int color) {

  }


  private void point0(float xx, float yy, float z, int color) {
    point0((int) (xx + 0.5f), (int) (yy + 0.5f), z, color);
  }


  private void point0(int x, int y, float z, int color) {
    if ((x < 0) || (x > width1) ||
        (y < 0) || (y > height1)) return;

    int index = y*width + x;
    if ((color & 0xff000000) == 0xff000000) {  // opaque
      pixels[index] = color;

    } else {  // transparent
      // a1 is how much of the orig pixel
      int a2 = (color >> 24) & 0xff;
      int a1 = a2 ^ 0xff;

      int p2 = strokeColor;
      int p1 = pixels[index];

      int r = (a1 * ((p1 >> 16) & 0xff) + a2 * ((p2 >> 16) & 0xff)) & 0xff00;
      int g = (a1 * ((p1 >>  8) & 0xff) + a2 * ((p2 >>  8) & 0xff)) & 0xff00;
      int b = (a1 * ( p1        & 0xff) + a2 * ( p2        & 0xff)) >> 8;

      pixels[index] =  0xff000000 | (r << 8) | g | b;
    }
    if (zbuffer != null) zbuffer[index] = z;
  }


  // points are inherently flat, but always tangent
  // to the screen surface. the z is only so that things
  // get scaled properly if the pt is way in back
  private void thick_point(float x, float y, float z, // note floats
                           float r, float g, float b, float a) {
    spolygon.reset(4);
    spolygon.interpRGBA = false;  // no changes for vertices of a point

    float strokeWidth2 = strokeWeight/2.0f;

    float svertex[] = spolygon.vertices[0];
    svertex[X] = x - strokeWidth2;
    svertex[Y] = y - strokeWidth2;
    svertex[Z] = z;

    svertex[R] = r;
    svertex[G] = g;
    svertex[B] = b;
    svertex[A] = a;

    svertex = spolygon.vertices[1];
    svertex[X] = x + strokeWidth2;
    svertex[Y] = y - strokeWidth2;
    svertex[Z] = z;

    svertex = spolygon.vertices[2];
    svertex[X] = x + strokeWidth2;
    svertex[Y] = y + strokeWidth2;
    svertex[Z] = z;

    svertex = spolygon.vertices[3];
    svertex[X] = x - strokeWidth2;
    svertex[Y] = y + strokeWidth2;
    svertex[Z] = z;

    spolygon.render();
  }



  //////////////////////////////////////////////////////////////

  // LINE


  public void line(float x1, float y1, float x2, float y2) {
    beginShape(LINES);
    vertex(x1, y1);
    vertex(x2, y2);
    endShape();
  }


  public void line(float x1, float y1, float z1,
                   float x2, float y2, float z2) {
    beginShape(LINES);
    vertex(x1, y1, z1);
    vertex(x2, y2, z2);
    endShape();
  }


  private void thick_flat_line(float ox1, float oy1,
                               float r1, float g1, float b1, float a1,
                               float ox2, float oy2,
                               float r2, float g2, float b2, float a2) {
    spolygon.interpRGBA = (r1 != r2) || (g1 != g2) || (b1 != b2) || (a1 != a2);
    spolygon.interpZ = false;

    float dX = ox2-ox1 + EPSILON;
    float dY = oy2-oy1 + EPSILON;
    float len = sqrt(dX*dX + dY*dY);

    // TODO strokeWidth should be transformed!
    float rh = strokeWeight / len;

    float dx0 = rh * dY;
    float dy0 = rh * dX;
    float dx1 = rh * dY;
    float dy1 = rh * dX;

    spolygon.reset(4);

    float svertex[] = spolygon.vertices[0];
    svertex[X] = ox1+dx0;
    svertex[Y] = oy1-dy0;
    svertex[R] = r1;
    svertex[G] = g1;
    svertex[B] = b1;
    svertex[A] = a1;

    svertex = spolygon.vertices[1];
    svertex[X] = ox1-dx0;
    svertex[Y] = oy1+dy0;
    svertex[R] = r1;
    svertex[G] = g1;
    svertex[B] = b1;
    svertex[A] = a1;

    svertex = spolygon.vertices[2];
    svertex[X] = ox2-dx1;
    svertex[Y] = oy2+dy1;
    svertex[R] = r2;
    svertex[G] = g2;
    svertex[B] = b2;
    svertex[A] = a2;

    svertex = spolygon.vertices[3];
    svertex[X] = ox2+dx1;
    svertex[Y] = oy2-dy1;
    svertex[R] = r2;
    svertex[G] = g2;
    svertex[B] = b2;
    svertex[A] = a2;

    spolygon.render();
  }


  // the incoming values are transformed,
  // and the colors have been calculated
  private void thick_spatial_line(float x1, float y1, float z1,
                                  float r1, float g1, float b1,
                                  float x2, float y2, float z2,
                                  float r2, float g2, float b2) {
    spolygon.interpRGBA = (r1 != r2) || (g1 != g2) || (b1 != b2);
    spolygon.interpZ = true;

    float ox1 = x1; float oy1 = y1; float oz1 = z1;
    float ox2 = x2; float oy2 = y2; float oz2 = z2;

    float dX = ox2-ox1 + 0.0001f;
    float dY = oy2-oy1 + 0.0001f;
    float len = sqrt(dX*dX + dY*dY);

    float rh = strokeWeight / len;

    float dx0 = rh * dY;
    float dy0 = rh * dX;
    float dx1 = rh * dY;
    float dy1 = rh * dX;

    spolygon.reset(4);

    float svertex[] = spolygon.vertices[0];
    svertex[X] = ox1+dx0;
    svertex[Y] = oy1-dy0;
    svertex[Z] = oz1;
    svertex[R] = r1;
    svertex[G] = g1;
    svertex[B] = b1;

    svertex = spolygon.vertices[1];
    svertex[X] = ox1-dx0;
    svertex[Y] = oy1+dy0;
    svertex[Z] = oz1;
    svertex[R] = r1;
    svertex[G] = g1;
    svertex[B] = b1;

    svertex = spolygon.vertices[2];
    svertex[X] = ox2-dx1;
    svertex[Y] = oy2+dy1;
    svertex[Z] = oz2;
    svertex[R] = r2;
    svertex[G] = g2;
    svertex[B] = b2;

    svertex = spolygon.vertices[3];
    svertex[X] = ox2+dx1;
    svertex[Y] = oy2-dy1;
    svertex[Z] = oz2;
    svertex[R] = r2;
    svertex[G] = g2;
    svertex[B] = b2;

    spolygon.render();
  }



  //////////////////////////////////////////////////////////////

  // TRIANGLE


  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    beginShape(TRIANGLES);
    vertex(x1, y1);
    vertex(x2, y2);
    vertex(x3, y3);
    endShape();
  }



  //////////////////////////////////////////////////////////////

  // RECT


  public void rectMode(int mode) {
    rectMode = mode;
  }


  public void rect(float x1, float y1, float x2, float y2) {
    float hradius, vradius;
    switch (rectMode) {
    case CORNERS:
      break;
    case CORNER:
      x2 += x1; y2 += y1;
      break;
    case CENTER_RADIUS:
      hradius = x2;
      vradius = y2;
      x2 = x1 + hradius;
      y2 = y1 + vradius;
      x1 -= hradius;
      y1 -= vradius;
      break;
    case CENTER:
      hradius = x2 / 2.0f;
      vradius = y2 / 2.0f;
      x2 = x1 + hradius;
      y2 = y1 + vradius;
      x1 -= hradius;
      y1 -= vradius;
    }

    if (depth) {
      if (fill) rect3_fill(x1, y1, x2, y2);
      if (stroke) rect3_stroke(x1, y1, x2, y2);

    } else {
      if (fill) rect2_fill(x1, y1, x2, y2);
      if (stroke) rect2_stroke(x1, y1, x2, y2);
    }
  }


  protected void rect3_fill(float x1, float y1, float x2, float y2) {
    beginShape(QUADS);
    vertex(x1, y1);
    vertex(x2, y1);
    vertex(x2, y2);
    vertex(x1, y2);
    endShape();
  }


  protected void rect3_stroke(float x1, float y1, float x2, float y2) {
    beginShape(QUADS);
    vertex(x1, y1);
    vertex(x2, y1);
    vertex(x2, y2);
    vertex(x1, y2);
    endShape();
  }


  protected void rect2_fill(float x1, float y1, float x2, float y2) {
    if ((m01 != 0) || (m10 != 0)) {
      // this is actually transformed, transform points and draw a quad
      quad0_fill(screenX(x1, y1), screenY(x1, y1),
                 screenX(x2, y1), screenY(x2, y1),
                 screenX(x2, y2), screenY(x2, y2),
                 screenX(x1, y2), screenY(x1, y2), fillColor);

    } else {
      if ((m00 == 1) && (m11 == 1)) {
        // no scale, but maybe a translate
        rect0_fill(x1 + m02, y1 + m12, x2 + m02, y2 + m12, 0, fillColor);

      } else {
        // scaled, maybe translated
        rect0_fill(screenX(x1, y1), screenY(x1, y1),
                   screenX(x2, y2), screenY(x2, y2), 0, fillColor);
      }
    }
  }


  protected void rect0_fill(float x1, float y1, float x2, float y2,
                            float z, int color) {
    if (smooth) {
      rect0_smooth_fill(x1, y1, x2, y2, z, color);

    } else {
      rect0_rough_fill((int) (x1+0.5f), (int) (y1+0.5f),
                       (int) (x2+0.5f), (int) (y2+0.5f), z, color);
    }
  }


  protected void rect0_smooth_fill(float x1, float y1, float x2, float y2,
                                   float z, int color) {
    quad0_smooth_fill(x1, y1, x2, y1, x2, y2, x1, y2, color);
  }


  protected void rect0_rough_fill(int x1, int y1, int x2, int y2,
                                  float z, int color) {
    // needs to check if smooth
    // or if there's an affine transform on the shape
    // also the points are now floats instead of ints

    //System.out.println("flat quad");
    if (y2 < y1) {
      int temp = y1; y1 = y2; y2 = temp;
    }
    if (x2 < x1) {
      int temp = x1; x1 = x2; x2 = temp;
    }
    // checking to watch out for boogers
    if ((x1 > width1) || (x2 < 0) ||
        (y1 > height1) || (y2 < 0)) return;

    int fx1 = (int) x1;
    int fy1 = (int) y1;
    int fx2 = (int) x2;
    int fy2 = (int) y2;

    // these only affect the fill, not the stroke
    // (otherwise strange boogers at edges b/c frame changes shape)
    if (fx1 < 0) fx1 = 0;
    if (fx2 > width) fx2 = width;
    if (fy1 < 0) fy1 = 0;
    if (fy2 > height) fy2 = height;

    // [toxi 031223]
    // on avg. 20-25% faster fill routine using System.arraycopy()
    int ww = fx2 - fx1;
    int hh = fy2 - fy1;
    int[] row = new int[ww];
    for (int i = 0; i < ww; i++) row[i] = fillColor;
    int idx = fy1 * width + fx1;
    for (int y = 0; y < hh; y++) {
      System.arraycopy(row, 0, pixels, idx, ww);
      idx += width;
    }
    row = null;
  }


  protected void rect2_stroke(float x1, float y1, float x2, float y2) {
    /*
    if (strokeWeight == 1) {
      thin_flat_line(x1, y1, x2, y1);
      thin_flat_line(x2, y1, x2, y2);
      thin_flat_line(x2, y2, x1, y2);
      thin_flat_line(x1, y2, x1, y1);

    } else {
      thick_flat_line(x1, y1, fillR, fillG, fillB, fillA,
                      x2, y1, fillR, fillG, fillB, fillA);
      thick_flat_line(x2, y1, fillR, fillG, fillB, fillA,
                      x2, y2, fillR, fillG, fillB, fillA);
      thick_flat_line(x2, y2, fillR, fillG, fillB, fillA,
                      x1, y2, fillR, fillG, fillB, fillA);
      thick_flat_line(x1, y2, fillR, fillG, fillB, fillA,
                      x1, y1, fillR, fillG, fillB, fillA);
    }
    */
  }



  //////////////////////////////////////////////////////////////

  // QUAD


  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
    beginShape(QUADS);
    vertex(x1, y1);
    vertex(x2, y2);
    vertex(x3, y3);
    vertex(x4, y4);
    endShape();
  }


  protected void quad0_fill(float x1, float y1, float x2, float y2,
                            float x3, float y3, float x4, float y4,
                            int color) {
    if (smooth) {
      quad0_smooth_fill(x1, y1, x2, y2, x3, y3, x4, y4, color);

    } else {
      quad0_rough_fill((int) (x1+0.5f), (int) (y1+0.5f),
                       (int) (x2+0.5f), (int) (y2+0.5f),
                       (int) (x3+0.5f), (int) (y3+0.5f),
                       (int) (x4+0.5f), (int) (y4+0.5f),
                       color);
    }
  }


  protected void quad0_smooth_fill(float x1, float y1, float x2, float y2,
                                   float x3, float y3, float x4, float y4,
                                   int color) {
  }


  protected void quad0_rough_fill(float x1, float y1, float x2, float y2,
                                  float x3, float y3, float x4, float y4,
                                  int color) {
  }


  protected void quad0_stroke(float x1, float y1, float x2, float y2,
                              float x3, float y3, float x4, float y4,
                              int color) {
    if (smooth) {
      quad0_smooth_stroke(x1, y1, x2, y2, x3, y3, x4, y4, color);

    } else {
      quad0_rough_stroke((int) (x1+0.5f), (int) (y1+0.5f),
                         (int) (x2+0.5f), (int) (y2+0.5f),
                         (int) (x3+0.5f), (int) (y3+0.5f),
                         (int) (x4+0.5f), (int) (y4+0.5f), color);
    }
  }


  protected void quad0_smooth_stroke(float x1, float y1, float x2, float y2,
                                     float x3, float y3, float x4, float y4,
                                     int color) {
  }


  protected void quad0_rough_stroke(float x1, float y1, float x2, float y2,
                                    float x3, float y3, float x4, float y4,
                                    int color) {
  }



  //////////////////////////////////////////////////////////////

  // IMAGE


  public void image(PImage image, float x1, float y1) {
    if (!depth && !lights && !tint &&
        (imageMode != CENTER_RADIUS)) {
      // if drawing a flat image with no warping,
      // use faster routine to draw direct to the screen
      flat_image(image, (int)x1, (int)y1);

    } else {
      int savedTextureMode = textureMode;
      textureMode(IMAGE_SPACE);

      image(image, x1, y1, image.width, image.height,
            0, 0, image.width, image.height);

      textureMode(savedTextureMode);
    }
  }


  public void image(PImage image,
                    float x1, float y1, float x2, float y2) {
    int savedTextureMode = textureMode;
    textureMode(IMAGE_SPACE);

    image(image, x1, y1, x2, y2, 0, 0, image.width, image.height);

    textureMode(savedTextureMode);
  }


  public void image(PImage image,
                    float x1, float y1, float x2, float y2,
                    float u1, float v1, float u2, float v2) {
    switch (imageMode) {
    case CORNERS:
      break;
    case CORNER:
      x2 += x1; y2 += y1;
      break;
    case CENTER:
      x2 /= 2f;
      y2 /= 2f;
    case CENTER_RADIUS:
      float hr = x2;
      float vr = y2;
      x2 = x1 + hr;
      y2 = y1 + vr;
      x1 -= hr;
      y1 -= vr;
      break;
    }

    boolean savedStroke = stroke;
    boolean savedFill = fill;

    stroke = false;
    fill = true;

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

    beginShape(QUADS);
    texture(image); // move outside to make java gl happier?
    vertex(x1, y1, u1, v1);
    vertex(x1, y2, u1, v2);
    vertex(x2, y2, u2, v2);
    vertex(x2, y1, u2, v1);
    endShape();

    stroke = savedStroke;
    fill = savedFill;

    fillR = savedFillR;
    fillG = savedFillG;
    fillB = savedFillB;
    fillA = savedFillA;
  }


  /**
   * Used by OpenGL implementations of PGraphics, so that images,
   * or textures, can be loaded into texture memory.
   */
  public void cache(PImage image) {
    // keep the lower } on a separate line b/c of preproc
  }

  public void cache(PImage images[]) {
    // keep the lower } on a separate line b/c of preproc
  }

  protected void cache(PImage image, int index) {
    // keep the lower } on a separate line b/c of preproc
  }



  //////////////////////////////////////////////////////////////

  // ARC


  public void arcMode(int mode) {
    arcMode = mode;
  }


  public void arc(float start, float stop,
                  float x, float y, float radius) {
    arc(start, stop, x, y, radius, radius);
  }


  public void arc(float start, float stop,
                  float x, float y, float hr, float vr) {
    switch (arcMode) {
    case CENTER_RADIUS:
      break;
    case CENTER:
      hr /= 2f; vr /= 2f;
      break;
    case CORNER:
      hr /= 2f; vr /= 2f;
      x += hr; y += vr;
      break;
    case CORNERS:
      hr = (hr - x) / 2f;
      vr = (vr - y) / 2f;
      x += hr;
      y += vr;
      break;
    }

    if (depth) {
      if (fill) arc3_fill(start, stop, x, y, hr, vr);
      if (stroke) arc3_stroke(start, stop, x, y, hr, vr);

    } else {
      if (fill) arc2_fill(start, stop, x, y, hr, vr);
      if (stroke) arc2_stroke(start, stop, x, y, hr, vr);
    }
  }


  protected void arc3_fill(float start, float stop,
                           float x, float y, float hr, float vr) {
  }


  protected void arc3_stroke(float start, float stop,
                             float x, float y, float hr, float vr) {
  }


  protected void arc2_fill(float start, float stop,
                           float x, float y, float hr, float vr) {
  }


  protected void arc2_stroke(float start, float stop,
                             float x, float y, float hr, float vr) {
  }


  //////////////////////////////////////////////////////////////

  // ELLIPSE


  public void ellipseMode(int mode) {
    ellipseMode = mode;
  }


  // adaptive ellipse accuracy contributed by toxi
  public void ellipse(float x, float y, float hradius, float vradius) {
    switch (ellipseMode) {
    case CENTER_RADIUS:
      break;
    case CENTER:
      hradius /= 2f; vradius /= 2f;
      break;
    case CORNER:
      hradius /= 2f; vradius /= 2f;
      x += hradius; y += vradius;
      break;
    case CORNERS:
      //float w = (hradius - x);
      //float h = (vradius - y);
      //hradius = w / 2f;
      //vradius = h / 2f;
      hradius = (hradius - x) / 2f;
      vradius = (vradius - y) / 2f;
      x += hradius;
      y += vradius;
      break;
    }

    if (depth) {
      if (fill) ellipse3_fill(x, y, hradius, vradius);
      if (stroke) ellipse3_stroke(x, y, hradius, vradius);

    } else {
      if (fill) ellipse2_fill(x, y, hradius, vradius);
      if (stroke) ellipse2_stroke(x, y, hradius, vradius);
    }
  }


  protected void ellipse3_fill(float x, float y, float h, float v) {
  }

  protected void ellipse3_stroke(float x, float y, float h, float v) {
  }

  protected void ellipse2_fill(float x, float y, float h, float v) {
  }

  protected void ellipse2_stroke(float x, float y, float h, float v) {
  }

  /*
  protected void ellipse_mess(float x, float y,
                              float hradius, float vradius) {
    // adapt accuracy to radii used w/ a minimum of 4 segments [toxi]
    // now uses current scale factors to determine "real" transformed radius

    //System.out.println(m00 + " " + m11);
    //int cAccuracy = (int)(4+Math.sqrt(hradius*abs(m00)+vradius*abs(m11))*2);
    //int cAccuracy = (int)(4+Math.sqrt(hradius+vradius)*2);

    // notched this up to *3 instead of *2 because things were
    // looking a little rough, i.e. the calculate->arctangent example [fry]

    // also removed the m00 and m11 because those were causing weirdness
    // need an actual measure of magnitude in there [fry]

    int cAccuracy = (int)(4+Math.sqrt(hradius+vradius)*3);

    boolean plain =
      !lights && !smooth && (strokeWeight == 1) &&
      !fillAlpha && !strokeAlpha;

    //boolean flat = (dimensions == 0) ||
    //((dimensions == 2) && (m00 == m11) && (m00 == 1));
    // FIXME
    boolean flat = false;

    if (plain && flat) {
      if (hradius == vradius) {
        circle0((int)x, (int)y, (int)hradius);

      } else {
        ellipse0((int)x, (int)y, (int)hradius, (int)vradius);
      }

    } else {
      // [toxi031031] adapted to use new lookup tables
      float inc = (float)SINCOS_LENGTH / cAccuracy;

      float val = 0;
      beginShape(POLYGON);
      for (int i = 0; i < cAccuracy; i++) {
        vertex(x + cosLUT[(int) val] * hradius,
               y + sinLUT[(int) val] * vradius);
        val += inc;
      }
      endShape();
    }
  }
  */


  /*
  private void flat_ellipse(int centerX, int centerY, int a, int b) {
    //FIXME
    //if (dimensions == 2) {  // probably a translate but no scale
      centerX = (int) screenX(centerX, centerY, 0);
      centerY = (int) screenY(centerX, centerY, 0);
      //}
    if (fill) flat_ellipse_internal(centerX, centerY, a, b, true);
    if (stroke) flat_ellipse_internal(centerX, centerY, a, b, false);
  }
  */


  private void ellipse0_stroke_rough(int cx, int cy, int a, int b) {
    ellipse0_rough(cx, cy, a, b, false);
  }


  private void ellipse0_fill_rough(int cx, int cy, int a, int b) {
    ellipse0_rough(cx, cy, a, b, true);
  }


  /**
   * Bresenham-style ellipse drawing function, adapted from a posting to
   * comp.graphics.algortihms.
   *
   * This function is included because the quality is so much better,
   * and the drawing significantly faster than with adaptive ellipses
   * drawn using the sine/cosine tables.
   *
   * @param centerX x coordinate of the center
   * @param centerY y coordinate of the center
   * @param a horizontal radius
   * @param b vertical radius
   */
  private void ellipse0_rough(int centerX, int centerY,
                              int a, int b, boolean filling) {
    //int x, y, a2, b2, s, t;

    int a2 = a*a;
    int b2 = b*b;
    int x = 0;
    int y = b;
    int s = a2*(1-2*b) + 2*b2;
    int t = b2 - 2*a2*(2*b-1);
    ellipse0_rough_internal(centerX, centerY, x, y, filling);

    do {
      if (s < 0) {
        s += 2*b2*(2*x+3);
        t += 4*b2*(x+1);
        x++;
      } else if (t < 0) {
        s += 2*b2*(2*x+3) - 4*a2*(y-1);
        t += 4*b2*(x+1) - 2*a2*(2*y-3);
        x++;
        y--;
      } else {
        s -= 4*a2*(y-1);
        t -= 2*a2*(2*y-3);
        y--;
      }
      ellipse0_rough_internal(centerX, centerY, x, y, filling);

    } while (y > 0);
  }

  private final void ellipse0_rough_internal(int centerX, int centerY,
                                             int ellipseX, int ellipseY,
                                             boolean filling) {
    // unfortunately this can't handle fill and stroke simultaneously,
    // because the fill will later replace some of the stroke points

    if (filling) {
      for (int i = centerX - ellipseX + 1; i < centerX + ellipseX; i++) {
        point0(i, centerY - ellipseY, 0, fillColor);
        point0(i, centerY + ellipseY, 0, fillColor);
      }
    } else {
      point0(centerX - ellipseX, centerY + ellipseY, 0, strokeColor);
      point0(centerX + ellipseX, centerY + ellipseY, 0, strokeColor);
      point0(centerX - ellipseX, centerY - ellipseY, 0, strokeColor);
      point0(centerX + ellipseX, centerY - ellipseY, 0, strokeColor);
    }
  }


  /*
  private void flat_circle(int centerX, int centerY, int radius) {
    // FIXME
    //if (dimensions == 2) {  // translate but no scale
      centerX = (int) screenX(centerX, centerY, 0);
      centerY = (int) screenY(centerX, centerY, 0);
      //}
    if (fill) circle0_fill(centerX, centerY, radius);
    if (stroke) circle0_stroke(centerX, centerY, radius);
  }
  */


  private void circle0(float x, float y, float r) {
    if (fill) circle0_fill(x, y, 0, r, fillColor);
    if (stroke) circle0_stroke(x, y, 0, r, strokeColor);
  }


  private void circle0_stroke(float x, float y, float z, float r, int color) {
    if (smooth) {
      circle0_stroke_smooth(x, y, z, r, color);
    } else {
      circle0_stroke_rough(x, y, z, r, color);
    }
  }


  private void circle0_stroke_smooth(float x, float y, float z,
                                     float r, int color) {
    // TODO draw a circle that's smoothed in screen space coords
  }


  /**
   * Draw the outline around a flat circle using a bresenham-style
   * algorithm. Adapted from drawCircle function in "Computer Graphics
   * for Java Programmers" by Leen Ammeraal, p. 110
   *
   * This function is included because the quality is so much better,
   * and the drawing significantly faster than with adaptive ellipses
   * drawn using the sine/cosine tables.
   *
   * Circle quadrants break down like so:
   *              |
   *        \ NNW | NNE /
   *          \   |   /
   *       WNW  \ | /  ENE
   *     -------------------
   *       WSW  / | \  ESE
   *          /   |   \
   *        / SSW | SSE \
   *              |
   *
   * @param xc x center
   * @param yc y center
   * @param r radius
   */
  private void circle0_stroke_rough(float xcf, float ycf, float z,
                                    float rf, int color) {
    int xc = (int) (xcf + 0.5f);
    int yc = (int) (ycf + 0.5f);
    int r = (int) (rf + 0.5f);

    int x = 0, y = r, u = 1, v = 2 * r - 1, E = 0;
    while (x < y) {
      point0(xc + x, yc + y, z, color); // NNE
      point0(xc + y, yc - x, z, color); // ESE
      point0(xc - x, yc - y, z, color); // SSW
      point0(xc - y, yc + x, z, color); // WNW

      x++; E += u; u += 2;
      if (v < 2 * E) {
        y--; E -= v; v -= 2;
      }
      if (x > y) break;

      point0(xc + y, yc + x, z, color); // ENE
      point0(xc + x, yc - y, z, color); // SSE
      point0(xc - y, yc - x, z, color); // WSW
      point0(xc - x, yc + y, z, color); // NNW
    }
  }


  private void circle0_fill(float x, float y, float z, float r, int color) {
    if (smooth) {
      circle0_smooth_fill(x, y, z, r, color);
    } else {
      circle0_rough_fill(x, y, z, r, color);
    }
  }


  /**
   * Heavily adapted version of the above algorithm that handles
   * filling the ellipse. Works by drawing from the center and
   * outwards to the points themselves. Has to be done this way
   * because the values for the points are changed halfway through
   * the function, making it impossible to just store a series of
   * left and right edges to be drawn more quickly.
   *
   * @param xc x center
   * @param yc y center
   * @param r radius
   */
  private void circle0_rough_fill(float xcf, float ycf, float z,
                                  float rf, int color) {
    int xc = (int) (xcf + 0.5f);
    int yc = (int) (ycf + 0.5f);
    int r = (int) (rf + 0.5f);

    int x = 0, y = r, u = 1, v = 2 * r - 1, E = 0;
    while (x < y) {
      for (int xx = xc; xx < xc + x; xx++) {  // NNE
        point0(xx, yc + y, z, color);
      }
      for (int xx = xc; xx < xc + y; xx++) {  // ESE
        point0(xx, yc - x, z, color);
      }
      for (int xx = xc - x; xx < xc; xx++) {  // SSW
        point0(xx, yc - y, z, color);
      }
      for (int xx = xc - y; xx < xc; xx++) {  // WNW
        point0(xx, yc + x, z, color);
      }

      x++; E += u; u += 2;
      if (v < 2 * E) {
        y--; E -= v; v -= 2;
      }
      if (x > y) break;

      for (int xx = xc; xx < xc + y; xx++) {  // ENE
        point0(xx, yc + x, z, color);
      }
      for (int xx = xc; xx < xc + x; xx++) {  // SSE
        point0(xx, yc - y, z, color);
      }
      for (int xx = xc - y; xx < xc; xx++) {  // WSW
        point0(xx, yc - x, z, color);
      }
      for (int xx = xc - x; xx < xc; xx++) {  // NNW
        point0(xx, yc + y, z, color);
      }
    }
  }


  private void circle0_smooth_fill(float x, float y, float z,
                                   float r, int color) {
  }


  //////////////////////////////////////////////////////////////

  // IMAGE


  /**
   * Image drawn in flat "screen space", with no scaling or warping.
   * this is so common that a special routine is included for it,
   * because the alternative is much slower.
   *
   * @param  image  image to be drawn
   * @param  sx1    x coordinate of upper-lefthand corner in screen space
   * @param  sy1    y coordinate of upper-lefthand corner in screen space
   */
  protected void flat_image(PImage image, int sx1, int sy1) {
    int ix1 = 0;
    int iy1 = 0;
    int ix2 = image.width;
    int iy2 = image.height;

    if (imageMode == CENTER) {
      sx1 -= image.width / 2;
      sy1 -= image.height / 2;
    }

    int sx2 = sx1 + image.width;
    int sy2 = sy1 + image.height;

    // don't draw if completely offscreen
    // (without this check, ArrayIndexOutOfBoundsException)
    if ((sx1 > width1) || (sx2 < 0) ||
        (sy1 > height1) || (sy2 < 0)) return;

    if (sx1 < 0) {  // off left edge
      ix1 -= sx1;
      sx1 = 0;
    }
    if (sy1 < 0) {  // off top edge
      iy1 -= sy1;
      sy1 = 0;
    }
    if (sx2 > width) {  // off right edge
      ix2 -= sx2 - width;
      sx2 = width;
    }
    if (sy2 > height) {  // off bottom edge
      iy2 -= sy2 - height;
      sy2 = height;
    }

    int source = iy1 * image.width + ix1;
    int target = sy1 * width;

    if (image.format == RGBA) {
      for (int y = sy1; y < sy2; y++) {
        int tx = 0;

        for (int x = sx1; x < sx2; x++) {
          pixels[target + x] =
            _blend(pixels[target + x],
                   image.pixels[source + tx],
                   image.pixels[source + tx++] >>> 24);
        }
        source += image.width;
        target += width;
      }
    } else if (image.format == ALPHA) {
      for (int y = sy1; y < sy2; y++) {
        int tx = 0;

        for (int x = sx1; x < sx2; x++) {
          pixels[target + x] =
            _blend(pixels[target + x],
                   fillColor,
                   image.pixels[source + tx++]);
        }
        source += image.width;
        target += width;
      }

    } else if (image.format == RGB) {
      target += sx1;
      int tw = sx2 - sx1;
      for (int y = sy1; y < sy2; y++) {
        System.arraycopy(image.pixels, source, pixels, target, tw);
        // should set z coordinate in here
        // or maybe not, since dims=0, meaning no relevant z
        source += image.width;
        target += width;
      }
    }
  }



  //////////////////////////////////////////////////////////////

  // CURVES


  /**
   * Evalutes quadratic bezier at point t for points a, b, c, d.
   * t varies between 0 and 1, and a and d are the on curve points,
   * b and c are the control points. this can be done once with the
   * x coordinates and a second time with the y coordinates to get
   * the location of a bezier curve at t.
   *
   * for instance, to convert the following example:<code>
   * stroke(255, 102, 0);
   * line(85, 20, 10, 10);
   * line(90, 90, 15, 80);
   * stroke(0, 0, 0);
   * bezier(85, 20, 10, 10, 90, 90, 15, 80);
   *
   * // draw it in gray, using 10 steps instead of the default 20
   * // this is a slower way to do it, but useful if you need
   * // to do things with the coordinates at each step
   * stroke(128);
   * beginShape(LINE_STRIP);
   * for (int i = 0; i <= 10; i++) {
   *   float t = i / 10.0f;
   *   float x = bezier(85, 10, 90, 15, t);
   *   float y = bezier(20, 10, 90, 80, t);
   *   vertex(x, y);
   * }
   * endShape();</code>
   */
  public float bezierPoint(float a, float b, float c, float d,
                           float t) {
    float t1 = 1.0f - t;

    // quadratic bezier
    //return a*t1*t1 + 2*b*t*t1 + c*t*t;

    // cubic bezier
    //return a*t*t*t + 3*b*t*t*t1 + 3*c*t*t1*t1 + d*t1*t1*t1;
    return a*t1*t1*t1 + 3*b*t*t1*t1 + 3*c*t*t*t1 + d*t*t*t;
  }


  /**
   * Provide the tangent at the given point on the bezier curve.
   * Based on code from v3ga's wordstree sketch.
   */
  public float bezierTangent(float a, float b, float c, float d,
                             float t) {
    float t1 = 1.0f - t;

    return (a *  3 * t*t +
            b *  3 * t * (2 - 3*t) +
            c *  3 * (3*t*t - 4*t + 1) +
            d * -3 * t1*t1);
  }


  /**
   * Draw a bezier curve. The first and last points are
   * the on-curve points. The middle two are the 'control' points,
   * or 'handles' in an application like Illustrator.
   *
   * Identical to typing:
   * beginShape();
   * bezierVertex(x1, y1);
   * bezierVertex(x2, y2);
   * bezierVertex(x3, y3);
   * bezierVertex(x4, y4);
   * endShape();
   *
   * In Postscript-speak, this would be:
   * moveto(x1, y1);
   * curveto(x2, y2, x3, y3, x4, y4);
   * If you were to try and continue that curve like so:
   * curveto(x5, y5, x6, y6, x7, y7);
   * This would be done in bagel by adding these statements:
   * curveVertex(x4, y4);
   * curveVertex(x5, y5);
   * curveVertex(x6, y6);
   * curveVertex(x7, y7);
   * Note that x4/y4 are being pulled from the previous
   * curveto and used again.
   *
   * The solution here may be a bit more verbose than Postscript,
   * but in general, decisions opted for maximum flexibility,
   * since these beginShape() commands are intended as a bit lower-level.
   * Rather than having many types of curveto (curve to corner,
   * and several others described in the Postscript and Illustrator specs)
   * let someone else implement a nice moveto/lineto/curveto library on top.
   * In fact, it's tempting that we may put one in there ourselves.
   *
   * Another method for bezier (though not implemented this way)
   * 1. first start with a call to vertex()
   * 2. every three calls to bezierVertex produce a new segment
   * This option seemed no good because of the confusion of mixing
   * vertex and bezierVertex calls.
   */
  public void bezier(float x1, float y1,
                     float x2, float y2,
                     float x3, float y3,
                     float x4, float y4) {
    beginShape(LINE_STRIP);
    bezierVertex(x1, y1);
    bezierVertex(x2, y2);
    bezierVertex(x3, y3);
    bezierVertex(x4, y4);
    endShape();
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


  private boolean bezier_inited = false;
  private int bezier_detail = 20; //BEZIER_DETAIL;
  // msjvm complained when bezier_basis was final
  private float bezier_basis[][] = {
    { -1,  3, -3,  1},
    {  3, -6,  3,  0},
    { -3,  3,  0,  0},
    {  1,  0,  0,  0}
  };
  private float bezier_forward[][]; // = new float[4][4];
  private float bezier_draw[][]; // = new float[4][4];


  private void bezier_init() {
    bezierDetail(bezier_detail); //BEZIER_DETAIL);
    //bezier_inited = true;
  }


  public void bezierDetail(int detail) {
    if (bezier_forward == null) {
      bezier_forward = new float[4][4];
      bezier_draw = new float[4][4];
    }
    bezier_detail = detail;
    bezier_inited = true;

    // setup matrix for forward differencing to speed up drawing
    setup_spline_forward(detail, bezier_forward);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    mult_spline_matrix(bezier_forward, bezier_basis, bezier_draw, 4);
  }


  private boolean curve_inited = false;
  private int curve_detail = 20;
  // catmull-rom basis matrix, perhaps with optional s parameter
  private float curve_tightness = 0;
  private float curve_basis[][]; // = new float[4][4];
  private float curve_forward[][]; // = new float[4][4];
  private float curve_draw[][];


  private void curve_init() {
    curve_mode(curve_detail, curve_tightness);
  }


  public void curveDetail(int detail) {
    curve_mode(detail, curve_tightness);
  }


  public void curveTightness(float tightness) {
    curve_mode(curve_detail, tightness);
  }


  /**
   * Set the number of segments to use when drawing a Catmull-Rom
   * curve, and setting the s parameter, which defines how tightly
   * the curve fits to each vertex. Catmull-Rom curves are actually
   * a subset of this curve type where the s is set to zero.
   *
   * (This function is not optimized, since it's not expected to
   * be called all that often. there are many juicy and obvious
   * opimizations in here, but it's probably better to keep the
   * code more readable)
   */
  private void curve_mode(int segments, float s) {
    curve_detail = segments;
    //curve_mode = ((curve_tightness != 0) ||
    //            (curve_segments != CURVE_DETAIL));

    if (curve_basis == null) {
      // allocate these when used, to save startup time
      curve_basis = new float[4][4];
      curve_forward = new float[4][4];
      curve_draw = new float[4][4];
      curve_inited = true;
    }

    float c[][] = curve_basis;

    c[0][0] = s-1;     c[0][1] = s+3;  c[0][2] = -3-s;    c[0][3] = 1-s;
    c[1][0] = 2*(1-s); c[1][1] = -5-s; c[1][2] = 2*(s+2); c[1][3] = s-1;
    c[2][0] = s-1;     c[2][1] = 0;    c[2][2] = 1-s;     c[2][3] = 0;
    c[3][0] = 0;       c[3][1] = 2;    c[3][2] = 0;       c[3][3] = 0;

    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        c[i][j] /= 2f;
      }
    }
    setup_spline_forward(segments, curve_forward);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    mult_spline_matrix(curve_forward, curve_basis, curve_draw, 4);
  }


  /**
   * Get a location along a catmull-rom curve segment.
   *
   * @param t Value between zero and one for how far along the segment
   */
  public float curvePoint(float a, float b, float c, float d,
                          float t) {
    if (!curve_inited) curve_init();

    float tt = t * t;
    float ttt = t * tt;
    float m[][] = curve_basis;

    // not optimized (and probably need not be)
    return (a * (ttt*m[0][0] + tt*m[1][0] + t*m[2][0] + m[3][0]) +
            b * (ttt*m[0][1] + tt*m[1][1] + t*m[2][1] + m[3][1]) +
            c * (ttt*m[0][2] + tt*m[1][2] + t*m[2][2] + m[3][2]) +
            d * (ttt*m[0][3] + tt*m[1][3] + t*m[2][3] + m[3][3]));
  }


  public float curveTangent(float a, float b, float c, float d,
                            float t) {
    System.err.println("curveTangent not yet implemented");
    return 0;
  }


  /**
   * Draws a segment of Catmull-Rom curve.
   *
   * Identical to typing out:
   * beginShape();
   * curveVertex(x1, y1);
   * curveVertex(x2, y2);
   * curveVertex(x3, y3);
   * curveVertex(x4, y4);
   * endShape();
   *
   * As of 0070, this function no longer doubles the first and
   * last points. The curves are a bit more boring, but it's more
   * mathematically correct, and properly mirrored in curvePoint().
   */
  public void curve(float x1, float y1,
                    float x2, float y2,
                    float x3, float y3,
                    float x4, float y4) {
    beginShape(LINE_STRIP);
    curveVertex(x1, y1);
    curveVertex(x2, y2);
    curveVertex(x3, y3);
    curveVertex(x4, y4);
    endShape();
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


  /**
   * Setup forward-differencing matrix to be used for speedy
   * curve rendering. It's based on using a specific number
   * of curve segments and just doing incremental adds for each
   * vertex of the segment, rather than running the mathematically
   * expensive cubic equation.
   * @param segments number of curve segments to use when drawing
   */
  private void setup_spline_forward(int segments, float fwd[][]) {
    float f  = 1.0f / segments;
    float ff = f * f;
    float fff = ff * f;

    fwd[0][0] = 0;     fwd[0][1] = 0;    fwd[0][2] = 0; fwd[0][3] = 1;
    fwd[1][0] = fff;   fwd[1][1] = ff;   fwd[1][2] = f; fwd[1][3] = 0;
    fwd[2][0] = 6*fff; fwd[2][1] = 2*ff; fwd[2][2] = 0; fwd[2][3] = 0;
    fwd[3][0] = 6*fff; fwd[3][1] = 0;    fwd[3][2] = 0; fwd[3][3] = 0;
  }


  // internal matrix multiplication routine used by the spline code
  // should these go to 4 instead of 3?
  //void mult_curve_matrix(float m[4][4], float g[4][3], float mg[4][3]);
  private void mult_spline_matrix(float m[][], float g[][],
                                  float mg[][], int dimensions) {
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < dimensions; j++) {
        mg[i][j] = 0;
      }
    }
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < dimensions; j++) {
        for (int k = 0; k < 4; k++) {
          mg[i][j] = mg[i][j] + (m[i][k] * g[k][j]);
        }
      }
    }
  }


  /**
   * Draw a segment of spline (bezier or catmull-rom curve)
   * using the matrix m, which is the basis matrix already
   * multiplied with the forward differencing matrix.
   *
   * the x0, y0, z0 points are the point that's being used as
   * the start, and also as the accumulator. for bezier curves,
   * the x1, y1, z1 are the first point drawn, and added to.
   * for catmull-rom curves, the first control point (x2, y2, z2)
   * is the first drawn point, and is accumulated to.
   */
  private void spline2_segment(int offset, int start,
                               float m[][], int segments) {
    float x1 = spline_vertex[offset][MX];
    float y1 = spline_vertex[offset][MY];

    float x2 = spline_vertex[offset+1][MX];
    float y2 = spline_vertex[offset+1][MY];

    float x3 = spline_vertex[offset+2][MX];
    float y3 = spline_vertex[offset+2][MY];

    float x4 = spline_vertex[offset+3][MX];
    float y4 = spline_vertex[offset+3][MY];

    float x0 = spline_vertex[start][MX];
    float y0 = spline_vertex[start][MY];

    float xplot1 = m[1][0]*x1 + m[1][1]*x2 + m[1][2]*x3 + m[1][3]*x4;
    float xplot2 = m[2][0]*x1 + m[2][1]*x2 + m[2][2]*x3 + m[2][3]*x4;
    float xplot3 = m[3][0]*x1 + m[3][1]*x2 + m[3][2]*x3 + m[3][3]*x4;

    float yplot1 = m[1][0]*y1 + m[1][1]*y2 + m[1][2]*y3 + m[1][3]*y4;
    float yplot2 = m[2][0]*y1 + m[2][1]*y2 + m[2][2]*y3 + m[2][3]*y4;
    float yplot3 = m[3][0]*y1 + m[3][1]*y2 + m[3][2]*y3 + m[3][3]*y4;

    // vertex() will reset spline_vertex_index, so save it
    int splineVertexSaved = spline_vertex_index;
    vertex(x0, y0);
    for (int j = 0; j < segments; j++) {
      x0 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y0 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      vertex(x0, y0);
    }
    spline_vertex_index = splineVertexSaved;
  }


  private void spline3_segment(int offset, int start,
                               float m[][], int segments) {
    float x1 = spline_vertex[offset+0][MX];
    float y1 = spline_vertex[offset+0][MY];
    float z1 = spline_vertex[offset+0][MZ];

    float x2 = spline_vertex[offset+1][MX];
    float y2 = spline_vertex[offset+1][MY];
    float z2 = spline_vertex[offset+1][MZ];

    float x3 = spline_vertex[offset+2][MX];
    float y3 = spline_vertex[offset+2][MY];
    float z3 = spline_vertex[offset+2][MZ];

    float x4 = spline_vertex[offset+3][MX];
    float y4 = spline_vertex[offset+3][MY];
    float z4 = spline_vertex[offset+3][MZ];

    float x0 = spline_vertex[start][MX];
    float y0 = spline_vertex[start][MY];
    float z0 = spline_vertex[start][MZ];

    float xplot1 = m[1][0]*x1 + m[1][1]*x2 + m[1][2]*x3 + m[1][3]*x4;
    float xplot2 = m[2][0]*x1 + m[2][1]*x2 + m[2][2]*x3 + m[2][3]*x4;
    float xplot3 = m[3][0]*x1 + m[3][1]*x2 + m[3][2]*x3 + m[3][3]*x4;

    float yplot1 = m[1][0]*y1 + m[1][1]*y2 + m[1][2]*y3 + m[1][3]*y4;
    float yplot2 = m[2][0]*y1 + m[2][1]*y2 + m[2][2]*y3 + m[2][3]*y4;
    float yplot3 = m[3][0]*y1 + m[3][1]*y2 + m[3][2]*y3 + m[3][3]*y4;

    float zplot1 = m[1][0]*z1 + m[1][1]*z2 + m[1][2]*z3 + m[1][3]*z4;
    float zplot2 = m[2][0]*z1 + m[2][1]*z2 + m[2][2]*z3 + m[2][3]*z4;
    float zplot3 = m[3][0]*z1 + m[3][1]*z2 + m[3][2]*z3 + m[3][3]*z4;

    //unchangedZ = false;
    //dimensions = 3;

    // vertex() will reset spline_vertex_index, so save it
    int cvertexSaved = spline_vertex_index;
    vertex(x0, y0, z0);
    for (int j = 0; j < segments; j++) {
      x0 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y0 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      z0 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
      vertex(x0, y0, z0);
    }
    spline_vertex_index = cvertexSaved;
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

  // TEXT/FONTS


  public void textFont(PFont which) {
    if (which != null) {
      textFont = which;
      textFont.resetSize();
      textFont.resetLeading();

    } else {
      System.err.println("Ignoring improperly loaded font in textFont()");
    }
  }


  public void textSize(float size) {
    if (textFont != null) {
      textFont.size(size);

    } else {
      System.err.println("First set a font before setting its size.");
    }
  }


  public void textFont(PFont which, float size) {
    textFont(which);
    textSize(size);
  }


  public void textLeading(float leading) {
    if (textFont != null) {
      textFont.leading(leading);

    } else {
      System.err.println("First set a font before setting its leading.");
    }
  }


  public void textMode(int mode) {
    if (textFont != null) {
      textFont.align(mode);

    } else {
      System.err.println("First set a font before setting its mode.");
    }
  }


  public void textSpace(int space) {
    if (textFont != null) {
      textFont.space(space);

    } else {
      System.err.println("First set a font before setting the space.");
    }
  }


  public void text(char c, float x, float y) {
    text(c, x, y, 0);
  }

  public void text(char c, float x, float y, float z) {
    if (textFont != null) {
      textFont.text(c, x, y, z, this);

    } else {
      System.err.println("text(): first set a font before drawing text");
    }
  }


  public void text(String s, float x, float y) {
    text(s, x, y, 0);
  }

  public void text(String s, float x, float y, float z) {
    if (textFont != null) {
      textFont.text(s, x, y, z, this);

    } else {
      System.err.println("text(): first set a font before drawing text");
    }
  }


  public void text(String s, float x, float y, float w, float h) {
    text(s, x, y, 0, w, h);
  }

  public void text(String s, float x1, float y1, float z, float x2, float y2) {
    if (textFont != null) {
      float hradius, vradius;
      switch (rectMode) {
      case CORNER:
        x2 += x1; y2 += y1;
        break;
      case CENTER_RADIUS:
        hradius = x2;
        vradius = y2;
        x2 = x1 + hradius;
        y2 = y1 + vradius;
        x1 -= hradius;
        y1 -= vradius;
        break;
      case CENTER:
        hradius = x2 / 2.0f;
        vradius = y2 / 2.0f;
        x2 = x1 + hradius;
        y2 = y1 + vradius;
        x1 -= hradius;
        y1 -= vradius;
      }
      if (x2 < x1) {
        float temp = x1; x1 = x2; x2 = temp;
      }
      if (y2 < y1) {
        float temp = y1; y1 = y2; y2 = temp;
      }
      textFont.text(s, x1, y1, z, x2, y2, this);

    } else {
      System.err.println("text(): first set a font before drawing text");
    }
  }


  public void text(int num, float x, float y) {
    text(String.valueOf(num), x, y, 0);
  }

  public void text(int num, float x, float y, float z) {
    text(String.valueOf(num), x, y, z);
  }


  /**
   * See three-dimensional version of the same function, below.
   */
  public void text(float num, float x, float y) {
    text(PApplet.nfs(num, 0, 3), x, y, 0);
  }

  /**
   * This does a basic number formatting, to avoid the
   * generally ugly appearance of printing floats.
   * Users who want more control should use their own nfs() cmmand,
   * or if they want the long, ugly version of float,
   * use String.valueOf() to convert the float to a String first.
   */
  public void text(float num, float x, float y, float z) {
    text(PApplet.nf(num, 0, 3), x, y, z);
  }



  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS


  public void angleMode(int mode) {
    angleMode = mode;
  }


  public void translate(float tx, float ty) {
    if (depth) {
      m03 += tx*m00 + ty*m01 + m02;
      m13 += tx*m10 + ty*m11 + m12;
      m23 += tx*m20 + ty*m21 + m22;
      m33 += tx*m30 + ty*m31 + m32;

    } else {
      translate(tx, ty, 0);
    }
  }


  public void translate(float tx, float ty, float tz) {
    //dimensions = 3;

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
    //if (dimensions == 3) {
    applyMatrix(s, 0, 0, 0,  0, s, 0, 0,  0, 0, s, 0,  0, 0, 0, 1);
    //if (dimensions < 2) dimensions = 2;

    //} else {
    //dimensions = 2;
    //applyMatrix(s, 0, 0, 0,  0, s, 0, 0,  0, 0, 1, 0,  0, 0, 0, 1);
    //}

    // figure out whether 2D or 3D matrix
    //scale(xyz, xyz, xyz);
  }


  public void scale(float sx, float sy) {
    //if (dimensions == 0) dimensions = 2;
    applyMatrix(sx, 0, 0, 0,  0, sy, 0, 0,  0, 0, 1, 0,  0, 0, 0, 1);
  }


  // OPTIMIZE: same as above
  public void scale(float x, float y, float z) {
    //modelMatrixIsIdentity = false;
    //dimensions = 3;
    applyMatrix(x, 0, 0, 0,  0, y, 0, 0,  0, 0, z, 0,  0, 0, 0, 1);
  }


  public void transform(float n00, float n01, float n02, float n03,
                        float n10, float n11, float n12, float n13,
                        float n20, float n21, float n22, float n23,
                        float n30, float n31, float n32, float n33) {
    //dimensions = 3;
    applyMatrix(n00, n01, n02, n03,  n10, n11, n12, n13,
                n20, n21, n22, n23,  n30, n31, n32, n33);
  }



  //////////////////////////////////////////////////////////////

  // TRANSFORMATION MATRIX


  public void push() {
    if (matrixStackDepth+1 == MATRIX_STACK_DEPTH) {
      message(COMPLAINT, "matrix stack overflow, to much pushmatrix");
      return;
    }
    float cm[] = matrixStack[matrixStackDepth];
    cm[ 0] = m00; cm[ 1] = m01; cm[ 2] = m02; cm[ 3] = m03;
    cm[ 4] = m10; cm[ 5] = m11; cm[ 6] = m12; cm[ 7] = m13;
    cm[ 8] = m20; cm[ 9] = m21; cm[10] = m22; cm[11] = m23;
    cm[12] = m30; cm[13] = m31; cm[14] = m32; cm[15] = m33;
    matrixStackDepth++;
  }


  public void pop() {
    if (matrixStackDepth == 0) {
      message(COMPLAINT, "matrix stack underflow, to many popmatrix");
      return;
    }
    matrixStackDepth--;
    float cm[] = matrixStack[matrixStackDepth];
    m00 = cm[ 0]; m01 = cm[ 1]; m02 = cm[ 2]; m03 = cm[ 3];
    m10 = cm[ 4]; m11 = cm[ 5]; m12 = cm[ 6]; m13 = cm[ 7];
    m20 = cm[ 8]; m21 = cm[ 9]; m22 = cm[10]; m23 = cm[11];
    m30 = cm[12]; m31 = cm[13]; m32 = cm[14]; m33 = cm[15];

    if ((matrixStackDepth == 0) &&
        (m00 == 1) && (m01 == 0) && (m02 == 0) && (m03 == 0) &&
        (m10 == 0) && (m11 == 1) && (m12 == 0) && (m13 == 0) &&
        (m20 == 0) && (m21 == 0) && (m22 == 1) && (m23 == 0) &&
        (m30 == 0) && (m31 == 0) && (m32 == 0) && (m33 == 1)) {
      //dimensions = 0;
    }
  }


  /**
   * Load identity as the transform/model matrix.
   * Same as glLoadIdentity().
   */
  public void resetMatrix() {
    //dimensions = 0;
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
   * Apply a 3x2 affine transformation matrix.
   */
  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {

    float r00 = m00*n00 + m01*n10;
    float r01 = m00*n01 + m01*n11;
    float r02 = m00*n02 + m01*n12 + m02;

    float r10 = m10*n00 + m11*n10;
    float r11 = m10*n01 + m11*n11;
    float r12 = m10*n02 + m11*n12 + m12;

    m00 = r00; m01 = r01; m02 = r02;
    m10 = r10; m11 = r11; m12 = r12;
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

    if (depth) {
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

    } else {  // 3x2 affine version
      System.out.println(PApplet.nfs(m00, d, 4) + " " +
                         PApplet.nfs(m01, d, 4) + " " +
                         PApplet.nfs(m02, d, 4));

      System.out.println(PApplet.nfs(m10, d, 4) + " " +
                         PApplet.nfs(m11, d, 4) + " " +
                         PApplet.nfs(m12, d, 4));
    }
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
    if (cameraMode == PERSPECTIVE) {
      beginCamera();
      resetMatrix();
      perspective(cameraFOV, cameraAspect, cameraNearDist, cameraFarDist);
      lookat(cameraEyeX, cameraEyeY, cameraEyeDist,
             cameraEyeX, cameraEyeY, 0,
             0, 1, 0);
      endCamera();

    } else if (cameraMode == ORTHOGRAPHIC) {
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
    if (!depth) {
      System.out.println("No camera matrix when not in depth() mode.");
      return;
    }

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
    return m00*x + m01*y + m02;
  }


  public float screenY(float x, float y) {
    return m10*x + m11*y + m12;
  }


  public float screenX(float x, float y, float z) {
    if (!depth) return screenX(x, y);

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
    if (!depth) return screenY(x, y);

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
    if (!depth) return 0;

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
    if (!depth) return screenX(x, y);

    float ax = m00*x + m01*y + m02*z + m03;
    float aw = m30*x + m31*y + m32*z + m33;
    return (aw != 0) ? ax / aw : ax;
  }


  public float objectY(float x, float y, float z) {
    if (!depth) return screenY(x, y);

    float ay = m10*x + m11*y + m12*z + m13;
    float aw = m30*x + m31*y + m32*z + m33;
    return (aw != 0) ? ay / aw : ay;
  }


  public float objectZ(float x, float y, float z) {
    if (!depth) return 0;

    float az = m20*x + m21*y + m22*z + m23;
    float aw = m30*x + m31*y + m32*z + m33;
    return (aw != 0) ? az / aw : az;
  }



  //////////////////////////////////////////////////////////////

  // COLOR


  public void colorMode(int mode) {
    colorMode = mode;
  }


  public void colorMode(int mode, float max) {
    colorMode(mode, max, max, max, max);
  }


  // note that this doesn't set the alpha color max..
  // so colorMode(RGB, 255, 255, 255) would retain the previous max alpha
  // could be a problem when colorMode(HSB, 360, 100, 100);

  public void colorMode(int mode,
                        float maxX, float maxY, float maxZ) {
    colorMode(mode, maxX, maxY, maxZ, colorModeA); //maxX); //ONE);
  }


  public void colorMode(int mode,
                        float maxX, float maxY, float maxZ, float maxA) {
    colorMode = mode;

    colorModeX = maxX;  // still needs to be set for hsb
    colorModeY = maxY;
    colorModeZ = maxZ;
    colorModeA = maxA;

    // if color max values are all 1, then no need to scale
    colorScale = ((maxA != ONE) || (maxX != maxY) ||
                  (maxY != maxZ) || (maxZ != maxA));

    // if color is rgb/0..255 this will make it easier for the
    // red() green() etc functions
    colorRgb255 = (colorMode == RGB) &&
      (colorModeA == 255) && (colorModeX == 255) &&
      (colorModeY == 255) && (colorModeZ == 255);
  }


  //////////////////////////////////////////////////////////////


  protected void calc_color(float gray) {
    calc_color(gray, colorModeA);
  }


  protected void calc_color(float gray, float alpha) {
    if (gray > colorModeX) gray = colorModeX;
    if (alpha > colorModeA) alpha = colorModeA;

    if (gray < 0) gray = 0;
    if (alpha < 0) alpha = 0;

    calcR = colorScale ? (gray / colorModeX) : gray;
    calcG = calcR;
    calcB = calcR;
    calcA = colorScale ? (alpha / colorModeA) : alpha;

    calcRi = (int)(calcR*255); calcGi = (int)(calcG*255);
    calcBi = (int)(calcB*255); calcAi = (int)(calcA*255);
    calcColor = (calcAi << 24) | (calcRi << 16) | (calcGi << 8) | calcBi;
    calcAlpha = (calcAi != 255);
  }


  protected void calc_color(float x, float y, float z) {
    calc_color(x, y, z, colorModeA);
  }


  protected void calc_color(float x, float y, float z, float a) {
    if (x > colorModeX) x = colorModeX;
    if (y > colorModeY) y = colorModeY;
    if (z > colorModeZ) z = colorModeZ;
    if (a > colorModeA) a = colorModeA;

    if (x < 0) x = 0;
    if (y < 0) y = 0;
    if (z < 0) z = 0;
    if (a < 0) a = 0;

    switch (colorMode) {
    case RGB:
      if (colorScale) {
        calcR = x / colorModeX;
        calcG = y / colorModeY;
        calcB = z / colorModeZ;
        calcA = a / colorModeA;
      } else {
        calcR = x; calcG = y; calcB = z; calcA = a;
      }
      break;

    case HSB:
      x /= colorModeX; // h
      y /= colorModeY; // s
      z /= colorModeZ; // b

      calcA = colorScale ? (a/colorModeA) : a;

      if (y == 0) {  // saturation == 0
        calcR = calcG = calcB = z;

      } else {
        float which = (x - (int)x) * 6.0f;
        float f = which - (int)which;
        float p = z * (1.0f - y);
        float q = z * (1.0f - y * f);
        float t = z * (1.0f - (y * (1.0f - f)));

        switch ((int)which) {
        case 0: calcR = z; calcG = t; calcB = p; break;
        case 1: calcR = q; calcG = z; calcB = p; break;
        case 2: calcR = p; calcG = z; calcB = t; break;
        case 3: calcR = p; calcG = q; calcB = z; break;
        case 4: calcR = t; calcG = p; calcB = z; break;
        case 5: calcR = z; calcG = p; calcB = q; break;
        }
      }
      break;
    }
    calcRi = (int)(255*calcR); calcGi = (int)(255*calcG);
    calcBi = (int)(255*calcB); calcAi = (int)(255*calcA);
    calcColor = (calcAi << 24) | (calcRi << 16) | (calcGi << 8) | calcBi;
    calcAlpha = (calcAi != 255);
  }


  /**
   * unpacks AARRGGBB color for direct use with calc_color.
   * handled here with its own function since this is independent
   * of the color mode.
   *
   * strangely the old version of this code ignored the alpha
   * value. not sure if that was a bug or what.
   *
   * (note: no need for bounds check since it's a 32 bit number)
   */
  protected void calc_color_from(int rgb) {
    calcColor = rgb;
    calcAi = (rgb >> 24) & 0xff;
    calcRi = (rgb >> 16) & 0xff;
    calcGi = (rgb >> 8) & 0xff;
    calcBi = rgb & 0xff;
    calcA = (float)calcAi / 255.0f;
    calcR = (float)calcRi / 255.0f;
    calcG = (float)calcGi / 255.0f;
    calcB = (float)calcBi / 255.0f;
    calcAlpha = (calcAi != 255);
  }


  protected void calc_tint() {
    tint = true;
    tintR = calcR;
    tintG = calcG;
    tintB = calcB;
    tintA = calcA;
    tintRi = calcRi;
    tintGi = calcGi;
    tintBi = calcBi;
    tintAi = calcAi;
    tintColor = calcColor;
    tintAlpha = calcAlpha;
  }


  protected void calc_fill() {
    fill = true;
    fillChanged = true;
    fillR = calcR;
    fillG = calcG;
    fillB = calcB;
    fillA = calcA;
    fillRi = calcRi;
    fillGi = calcGi;
    fillBi = calcBi;
    fillAi = calcAi;
    fillColor = calcColor;
    fillAlpha = calcAlpha;
  }


  protected void calc_stroke() {
    stroke = true;
    strokeChanged = true;
    strokeR = calcR;
    strokeG = calcG;
    strokeB = calcB;
    strokeA = calcA;
    strokeRi = calcRi;
    strokeGi = calcGi;
    strokeBi = calcBi;
    strokeAi = calcAi;
    strokeColor = calcColor;
    strokeAlpha = calcAlpha;
  }


  protected void calc_background() {
    backgroundR = calcR;
    backgroundG = calcG;
    backgroundB = calcB;
    backgroundRi = calcRi;
    backgroundGi = calcGi;
    backgroundBi = calcBi;
    backgroundColor = calcColor;
  }


  //////////////////////////////////////////////////////////////


  public void noTint() {
    tint = false;
  }


  // if high bit isn't set, then it's not a #ffcc00 style web color
  // so redirect to the float version, b/c they want a gray.
  // only danger is that someone would try to set the color to a
  // zero alpha.. which would be kooky but not unlikely
  // (i.e. if it were in a loop) so in addition to checking the high
  // bit, check to see if the value is at least just below the
  // colorModeX (i.e. 0..255). can't just check the latter since
  // if the high bit is > 0x80 then the int value for rgb will be
  // negative. yay for no unsigned types in java!

  public void tint(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {
      tint((float) rgb);

    } else {
      calc_color_from(rgb);
      calc_tint();
    }
  }

  public void tint(float gray) {
    calc_color(gray);
    calc_tint();
  }


  public void tint(float gray, float alpha) {
    calc_color(gray, alpha);
    calc_tint();
  }


  public void tint(float x, float y, float z) {
    calc_color(x, y, z);
    calc_tint();
  }


  public void tint(float x, float y, float z, float a) {
    calc_color(x, y, z, a);
    calc_tint();
  }


  //////////////////////////////////////////////////////////////


  public void noFill() {
    fill = false;
  }


  public void fill(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      fill((float) rgb);

    } else {
      calc_color_from(rgb);
      calc_fill();
    }
  }

  public void fill(float gray) {
    calc_color(gray);
    calc_fill();
  }


  public void fill(float gray, float alpha) {
    calc_color(gray, alpha);
    calc_fill();
  }


  public void fill(float x, float y, float z) {
    calc_color(x, y, z);
    calc_fill();
  }


  public void fill(float x, float y, float z, float a) {
    calc_color(x, y, z, a);
    calc_fill();
  }


  //////////////////////////////////////////////////////////////


  public void strokeWeight(float weight) {
    strokeWeight = weight;
  }


  public void strokeJoin(int join) {
    strokeJoin = join;
  }


  public void strokeCap(int cap) {
    strokeCap = cap;
  }


  public void noStroke() {
    stroke = false;
  }


  public void stroke(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      stroke((float) rgb);

    } else {
      calc_color_from(rgb);
      calc_stroke();
    }
  }


  public void stroke(float gray) {
    calc_color(gray);
    calc_stroke();
  }


  public void stroke(float gray, float alpha) {
    calc_color(gray, alpha);
    calc_stroke();
  }


  public void stroke(float x, float y, float z) {
    calc_color(x, y, z);
    calc_stroke();
  }


  public void stroke(float x, float y, float z, float a) {
    calc_color(x, y, z, a);
    calc_stroke();
  }


  //////////////////////////////////////////////////////////////


  public void background(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      background((float) rgb);

    } else {
      calc_color_from(rgb);
      calc_background();
    }
    clear();
  }


  public void background(float gray) {
    calc_color(gray);
    calc_background();
    clear();
  }


  public void background(float x, float y, float z) {
    calc_color(x, y, z);
    calc_background();
    clear();
  }


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
    if ((image.width != width) || (image.height != height)) {
      System.err.println("background image must be the same size " +
                         "as your application");
      return;
    }
    if ((image.format != RGB) && (image.format != RGBA)) {
      System.err.println("background images should be RGB or RGBA");
      return;
    }

    // blit image to the screen
    System.arraycopy(image.pixels, 0, pixels, 0, pixels.length);

    if (zbuffer != null) {
      for (int i = 0; i < pixelCount; i++) {
        zbuffer[i] = MAX_FLOAT;
        stencil[i] = 0;
      }
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
    if (zbuffer != null) {
      for (int i = 0; i < pixelCount; i++) {
        pixels[i] = backgroundColor;
        zbuffer[i] = MAX_FLOAT;
        stencil[i] = 0;
      }
    } else {
      for (int i = 0; i < pixelCount; i++) {
        pixels[i] = backgroundColor;
      }
    }
  }



  //////////////////////////////////////////////////////////////

  // DEPTH


  /** semi-placeholder */
  public void depth() {
    depth = true;
    //dimensions = 3;
    cameraMode(PERSPECTIVE);
  }

  /** semi-placeholder */
  public void noDepth() {
    depth = false;
    //dimensions = 0;
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
   */
  public void light(int num, float x, float y, float z,
                    float r, float g, float b) {
    lightPosition(num, x, y, z);
    lightAmbient(num, 0, 0, 0);
    lightDiffuse(num, r, g, b);
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

  // HINTS

  // for the most part, hints are temporary api quirks,
  // for which a proper api hasn't been properly worked out.
  // for instance SMOOTH_IMAGES existed because smooth()
  // wasn't yet implemented, but it will soon go away.

  // they also exist for obscure features in the graphics
  // engine, like enabling/disabling single pixel lines
  // that ignore the zbuffer, the way they do in alphabot.

  public void hint(int which) {
    hints[which] = true;
  }

  public void unhint(int which) {
    hints[which] = false;
  }



  //////////////////////////////////////////////////////////////

  // MESSAGES / ERRORS / LOGGING


  public void message(int level, String message) {  // ignore
    switch (level) {
    case CHATTER:
      //System.err.println("bagel chatter: " + message);
      break;
    case COMPLAINT:
      System.err.println("bagel complaint: " + message);
      break;
    case PROBLEM:
      System.err.println("bagel problem: " + message);
      break;
    }
  }

  public void message(int level, String message, Exception e) {  // ignore
    message(level, message);
    e.printStackTrace();
  }



  //////////////////////////////////////////////////////////////

  // PIXELS

  // these functions are really slow, but easy to use
  // if folks are advanced enough to want something faster,
  // they can write it themselves (not difficult)


  public final int color(int gray) {  // ignore
    if (colorRgb255) {
      // bounds checking to make sure the numbers aren't to high or low
      if (gray > 255) gray = 255; else if (gray < 0) gray = 0;
      return 0xff000000 | (gray << 16) | (gray << 8) | gray;
    }
    calc_color(gray);
    return calcColor;
  }

  public final int color(float gray) {  // ignore
    calc_color(gray);
    return calcColor;
  }


  public final int color(int gray, int alpha) {  // ignore
    if (colorRgb255) {
      // bounds checking to make sure the numbers aren't to high or low
      if (gray > 255) gray = 255; else if (gray < 0) gray = 0;
      if (alpha > 255) alpha = 255; else if (alpha < 0) alpha = 0;

      return ((alpha & 0xff) << 24) | (gray << 16) | (gray << 8) | gray;
    }
    calc_color(gray, alpha);
    return calcColor;
  }

  public final int color(float gray, float alpha) {  // ignore
    calc_color(gray, alpha);
    return calcColor;
  }


  public final int color(int x, int y, int z) {  // ignore
    if (colorRgb255) {
      // bounds checking to make sure the numbers aren't to high or low
      if (x > 255) x = 255; else if (x < 0) x = 0;
      if (y > 255) y = 255; else if (y < 0) y = 0;
      if (z > 255) z = 255; else if (z < 0) z = 0;

      return 0xff000000 | (x << 16) | (y << 8) | z;
    }
    calc_color(x, y, z);
    return calcColor;
  }

  public final int color(float x, float y, float z) {  // ignore
    calc_color(x, y, z);
    return calcColor;
  }


  public final int color(int x, int y, int z, int a) {  // ignore
    if (colorRgb255) {
      // bounds checking to make sure the numbers aren't to high or low
      if (a > 255) a = 255; else if (a < 0) a = 0;
      if (x > 255) x = 255; else if (x < 0) x = 0;
      if (y > 255) y = 255; else if (y < 0) y = 0;
      if (z > 255) z = 255; else if (z < 0) z = 0;

      return (a << 24) | (x << 16) | (y << 8) | z;
    }
    calc_color(x, y, z, a);
    return calcColor;
  }

  public final int color(float x, float y, float z, float a) {  // ignore
    calc_color(x, y, z, a);
    return calcColor;
  }


  public final float alpha(int what) {
    float c = (what >> 24) & 0xff;
    if (colorModeA == 255) return c;
    return (c / 255.0f) * colorModeA;
  }

  public final float red(int what) {
    float c = (what >> 16) & 0xff;
    if (colorRgb255) return c;
    return (c / 255.0f) * colorModeX;
  }

  public final float green(int what) {
    float c = (what >> 8) & 0xff;
    if (colorRgb255) return c;
    return (c / 255.0f) * colorModeY;
  }

  public final float blue(int what) {
    float c = (what) & 0xff;
    if (colorRgb255) return c;
    return (c / 255.0f) * colorModeZ;
  }


  public final float hue(int what) {
    if (what != cacheHsbKey) {
      Color.RGBtoHSB((what >> 16) & 0xff, (what >> 8) & 0xff,
                     what & 0xff, cacheHsbValue);
      cacheHsbKey = what;
    }
    return cacheHsbValue[0] * colorModeX;
  }

  public final float saturation(int what) {
    if (what != cacheHsbKey) {
      Color.RGBtoHSB((what >> 16) & 0xff, (what >> 8) & 0xff,
                     what & 0xff, cacheHsbValue);
      cacheHsbKey = what;
    }
    return cacheHsbValue[1] * colorModeY;
  }

  public final float brightness(int what) {
    if (what != cacheHsbKey) {
      Color.RGBtoHSB((what >> 16) & 0xff, (what >> 8) & 0xff,
                     what & 0xff, cacheHsbValue);
      cacheHsbKey = what;
    }
    return cacheHsbValue[2] * colorModeZ;
  }


  // should only be used by other parts of the bagel library


  /*
  static private final int float_color(float r, float g, float b) {
    return (0xff000000 |
            ((int) (255.0f * r)) << 16 |
            ((int) (255.0f * g)) << 8 |
            ((int) (255.0f * b)));
  }
  */


  public final static int _blend(int p1, int p2, int a2) {
    // scale alpha by alpha of incoming pixel
    a2 = (a2 * (p2 >>> 24)) >> 8;

    int a1 = a2 ^ 0xff;
    int r = (a1 * ((p1 >> 16) & 0xff) + a2 * ((p2 >> 16) & 0xff)) & 0xff00;
    int g = (a1 * ((p1 >>  8) & 0xff) + a2 * ((p2 >>  8) & 0xff)) & 0xff00;
    int b = (a1 * ( p1        & 0xff) + a2 * ( p2        & 0xff)) >> 8;

    return 0xff000000 | (r << 8) | g | b;
  }


  //////////////////////////////////////////////////////////////

  // MATH

  // these are *only* the functions used internally
  // the real math functions are inside PApplet

  // these have been made private so as not to conflict
  // with the versions found in PApplet when fxn importing happens
  // also might be faster that way. hmm.


  private final float mag(float a, float b) {
    return (float)Math.sqrt(a*a + b*b);
  }

  private final float mag(float a, float b, float c) {
    return (float)Math.sqrt(a*a + b*b + c*c);
  }

  private final float max(float a, float b) {
    return (a > b) ? a : b;
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

