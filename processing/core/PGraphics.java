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

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;


/**
 * Main graphics and rendering context, as well as
 * the base API implementation for processing "core".
 * <P>
 * As of beta, this class is semi-disabled.
 */
public class PGraphics extends PImage implements PConstants {

  /// width minus one (useful for many calculations)
  public int width1;

  /// height minus one (useful for many calculations)
  public int height1;

  /// width * height (useful for many calculations)
  public int pixelCount;

  // ........................................................

  // specifics for java memoryimagesource
  DirectColorModel cm;
  MemoryImageSource mis;
  public Image image;

  // ........................................................

  // needs to happen before background() is called
  // and resize.. so it's gotta be outside
  protected boolean hints[] = new boolean[HINT_COUNT];

  // ........................................................

  // underscored_names are used for private functions or variables

  /** The current colorMode */
  public int colorMode; // = RGB;

  /** Max value for red (or hue) set by colorMode */
  public float colorModeX; // = 255;

  /** Max value for green (or saturation) set by colorMode */
  public float colorModeY; // = 255;

  /** Max value for blue (or value) set by colorMode */
  public float colorModeZ; // = 255;

  /** Max value for alpha set by colorMode */
  public float colorModeA; // = 255;

  /** True if colors are not in the range 0..1 */
  boolean colorScale; // = true;

  /** True if colorMode(RGB, 255) */
  boolean colorRgb255; // = true;

  // ........................................................

  /** true if tint() is enabled (read-only) */
  public boolean tint;

  /** tint that was last set (read-only) */
  public int tintColor;

  boolean tintAlpha;
  float tintR, tintG, tintB, tintA;
  int tintRi, tintGi, tintBi, tintAi;

  // ........................................................

  /** true if fill() is enabled, (read-only) */
  public boolean fill;

  /** fill that was last set (read-only) */
  public int fillColor;

  boolean fillAlpha;
  float fillR, fillG, fillB, fillA;
  int fillRi, fillGi, fillBi, fillAi;

  // ........................................................

  /** true if stroke() is enabled, (read-only) */
  public boolean stroke;

  /** stroke that was last set (read-only) */
  public int strokeColor;

  boolean strokeAlpha;
  float strokeR, strokeG, strokeB, strokeA;
  int strokeRi, strokeGi, strokeBi, strokeAi;

  // ........................................................

  /** Last background color that was set, zero if an image */
  public int backgroundColor;

  float backgroundR, backgroundG, backgroundB;
  int backgroundRi, backgroundGi, backgroundBi;

  // ........................................................

  // internal color for setting/calculating
  protected float calcR, calcG, calcB, calcA;
  int calcRi, calcGi, calcBi, calcAi;
  int calcColor;
  boolean calcAlpha;

  /** The last rgb value converted to HSB */
  int cacheHsbKey;
  /** Result of the last conversion to HSB */
  float cacheHsbValue[] = new float[3]; // inits to zero

  // ........................................................

  /** Last value set by strokeWeight() (read-only) */
  public float strokeWeight;

  /** Set by strokeJoin() (read-only) */
  public int strokeJoin;

  /** Set by strokeCap() (read-only) */
  public int strokeCap;

  // ........................................................

  /**
   * Model transformation of the form m[row][column],
   * which is a "column vector" (as opposed to "row vector") matrix.
   */
  public float m00, m01, m02;
  public float m10, m11, m12;

  //public int angleMode;

  static final int MATRIX_STACK_DEPTH = 32;
  float matrixStack[][] = new float[MATRIX_STACK_DEPTH][16];
  int matrixStackDepth;

  // ........................................................

  Path path;

  // ........................................................

  /**
   * Type of shape passed to beginShape(),
   * zero if no shape is currently being drawn.
   */
  protected int shape;

  // vertices
  static final int DEFAULT_VERTICES = 512;
  public float vertices[][] = new float[DEFAULT_VERTICES][VERTEX_FIELD_COUNT];
  int vertexCount; // total number of vertices


  // ........................................................

  protected boolean bezierInited = false;
  public int bezierDetail = 20;
  // msjvm complained when bezier_basis was final
  protected float bezier_basis[][] = {
    { -1,  3, -3,  1},
    {  3, -6,  3,  0},
    { -3,  3,  0,  0},
    {  1,  0,  0,  0}
  };

  protected PMatrix bezierBasis =
    new PMatrix(-1,  3, -3,  1,
                 3, -6,  3,  0,
                -3,  3,  0,  0,
                 1,  0,  0,  0);

  protected float bezier_forward[][]; // = new float[4][4];
  protected float bezier_draw[][]; // = new float[4][4];

  // ........................................................

  protected boolean curve_inited = false;
  protected int curveDetail = 20;
  // catmull-rom basis matrix, perhaps with optional s parameter
  public float curveTightness = 0;
  protected float curve_basis[][]; // = new float[4][4];
  protected float curve_forward[][]; // = new float[4][4];
  protected float curve_draw[][];

  protected PMatrix bezierBasisInverse;
  protected PMatrix curveToBezierMatrix;

  // ........................................................

  // spline vertices

  static final int DEFAULT_SPLINE_VERTICES = 128;
  protected float splineVertices[][];
  protected int splineVertexCount;

  // ........................................................

  // precalculate sin/cos lookup tables [toxi]
  // circle resolution is determined from the actual used radii
  // passed to ellipse() method. this will automatically take any
  // scale transformations into account too

  // [toxi 031031]
  // changed table's precision to 0.5 degree steps
  // introduced new vars for more flexible code
  static final float sinLUT[];
  static final float cosLUT[];
  static final float SINCOS_PRECISION = 0.5f;
  static final int SINCOS_LENGTH = (int) (360f / SINCOS_PRECISION);
  static {
    sinLUT = new float[SINCOS_LENGTH];
    cosLUT = new float[SINCOS_LENGTH];
    for (int i = 0; i < SINCOS_LENGTH; i++) {
      sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
      cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
    }
  }

  // ........................................................

  /** The current rect mode (read-only) */
  public int rectMode;

  /** The current ellipse mode (read-only) */
  public int ellipseMode;

  /** The current text font (read-only) */
  public PFont textFont;

  /** The current font if a Java version of it is installed */
  public Font textFontNative;

  /** Metrics for the current native Java font */
  public FontMetrics textFontNativeMetrics;

  /** The current text align (read-only) */
  public int textAlign;

  /** The current text mode (read-only) */
  public int textMode;

  /** The current text size (read-only) */
  public float textSize;

  /** The current text leading (read-only) */
  public float textLeading;

  /** Last text position, because text often mixed on lines together */
  public float textX, textY, textZ;

  /**
   * Internal buffer used by the text() functions
   * because the String object is slow
   */
  protected char textBuffer[] = new char[8 * 1024];
  protected char textWidthBuffer[] = new char[8 * 1024];


  //////////////////////////////////////////////////////////////

  // VARIABLES FOR 3D (used to prevent the need for a subclass)


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

  // Material properties

  public float ambientR, ambientG, ambientB;
  public float specularR, specularG, specularB, specularA;
  public float emissiveR, emissiveG, emissiveB;
  public float shininess;

  // ........................................................

  /** Camera field of view (in radians, as of rev 86) */
  public float cameraFOV;

  /** Position of the camera */
  public float cameraX, cameraY, cameraZ;

  public float cameraNear, cameraFar;
  public float cameraAspect;

  // projection matrix
  public PMatrix projection; // = new PMatrix();

  // ........................................................

  /// the stencil buffer
  public int stencil[];

  /// depth buffer
  public float zbuffer[];

  // ........................................................

  /** Maximum lights by default is 8, which is arbitrary,
      but is the minimum defined by OpenGL */
  public static final int MAX_LIGHTS = 8;

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

  /** Current specular color for lighting */
  public float lightSpecularR;
  public float lightSpecularG;
  public float lightSpecularB;

  /** Current light falloff */
  public float lightFalloffConstant;
  public float lightFalloffLinear;
  public float lightFalloffQuadratic;

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

  /** Current image being used as a texture */
  public PImage textureImage;

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
   * @param iwidth  viewport width
   * @param iheight viewport height
   */
  //public PGraphics(int iwidth, int iheight) {
  //resize(iwidth, iheight);

    // init color/stroke/fill
    // called instead just before setup on first frame
    //defaults();

    // clear geometry for loading later
    //circleX = null;  // so that bagel knows to init these
    //sphereX = null;  // diff from cpp b/c mem in cpp is preallocated
  //}


  /**
   * Constructor for the PGraphics object. Use this to ensure that
   * the defaults get set properly. In a subclass, use this(w, h)
   * as the first line of a subclass' constructor to properly set
   * the internal fields and defaults.
   *
   * @param iwidth  viewport width
   * @param iheight viewport height
   */
  public PGraphics(int iwidth, int iheight, PApplet applet) {
    if (applet != null) applet.addListeners();
    //applet.addMouseListener(applet);
    //applet.addMouseMotionListener(applet);
    //applet.addKeyListener(applet);
    //applet.addFocusListener(applet);
    //}
    resize(iwidth, iheight);
  }


  /**
   * Called in repsonse to a resize event, handles setting the
   * new width and height internally, as well as re-allocating
   * the pixel buffer for the new size.
   * <P>
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
    //background(backgroundColor);
  }


  /**
   * Parent thread has requested that visual action be taken.
   */
  public void requestDisplay(PApplet parent) {  // ignore
    parent.display();
  }


  // broken out because of subclassing
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
  }



  //////////////////////////////////////////////////////////////

  // FRAME


  /**
   * Initializes engine before drawing a new frame.
   * Called by PApplet, no need to call this.
   */
  public void beginFrame() {  // ignore
    resetMatrix(); // reset model matrix

    // reset vertices
    vertexCount = 0;
  }


  /**
   * Indicates a completed frame.
   * Finishes rendering and swaps the buffer to the screen.
   *
   * If z-sorting has been turned on, then the triangles will
   * all be quicksorted here (to make alpha work more properly)
   * and then blit to the screen.
   */
  public void endFrame() {  // ignore
    // moving this back here (post-68) because of macosx thread problem
    mis.newPixels(pixels, cm, 0, width);
  }


  /**
   * Set engine's default values. This has to be called by PApplet,
   * somewhere inside setup() or draw() because it talks to the
   * graphics buffer, meaning that for subclasses like OpenGL, there
   * needs to be a valid graphics context to mess with otherwise
   * you'll get some good crashing action.
   */
  public void defaults() {  // ignore
    //System.out.println("PGraphics.defaults() " + width + " " + height);
    colorMode(RGB, TFF);
    fill(TFF);
    stroke(0);

    strokeWeight(ONE);
    //strokeCap(SQUARE);
    strokeCap(ROUND);
    strokeJoin(MITER);

    background(204);

    // init shape stuff
    shape = 0;

    // init matrices (must do before lights)
    matrixStackDepth = 0;

    rectMode(CORNER);
    ellipseMode(CENTER);
    //arcMode(CENTER);
    //angleMode(RADIANS);

    // no current font
    textFont = null;
    textSize = 12;
    textLeading = 14;
    textAlign = LEFT;
    textMode = MODEL;
  }


  /**
   * do anything that needs doing after setup before draw
   */
  //public void postSetup() {
  //}


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

    // reset vertex, line and triangle information
    // every shape is rendered at endShape();
    vertexCount = 0;

    splineVertexCount = 0;
    //spline_vertices_flat = true;

    //strokeChanged = false;
    //fillChanged = false;
    //normalChanged = false;
  }


  public void normal(float nx, float ny, float nz) {
    depthError("normal");
  }

  public void textureMode(int mode) {
    depthError("textureMode");
  }

  public void texture(PImage image) {
    depthError("texture");
  }


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
        path = new Path();
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
        path = new Path();
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
        path = new Path();
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
        path = new Path();
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
      //case CONCAVE_POLYGON:
      //case CONVEX_POLYGON:
      if (vertexCount == 1) {
        path = new Path();
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
      break;
    }
  }


  public void vertex(float x, float y, float z) {
    depthErrorXYZ("vertex");
  }


  public void vertex(float x, float y, float u, float v) {
    throw new RuntimeException("vertex() with u, v coordinates " +
                               "can only be used with OPENGL or P3D");
  }


  public void vertex(float x, float y, float z, float u, float v) {
    throw new RuntimeException("vertex() with u, v coordinates " +
                               "can only be used with OPENGL or P3D");
  }


  public void bezierVertex(float x1, float y1,
                           float x2, float y2,
                           float x3, float y3) {
    // if there hasn't yet been a call to vertex(), throw an error

    // otherwise, draw a bezier segment to this point
  }


  protected void bezier_vertex(float x, float y) {
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
    case LINE_STRIP:
    case POLYGON:
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

    default:
      throw new RuntimeException("bezierVertex() can only be used with " +
                                 "LINE_LOOP and POLYGON shapes");
    }
  }


  public void bezierVertex(float x1, float y1, float z1,
                           float x2, float y2, float z2,
                           float x3, float y3, float z3) {
    depthErrorXYZ("bezierVertex");
  }


  /**
   * See notes with the curve() function.
   */
  public void curveVertex(float x, float y) {
    //throw new RuntimeException("curveVertex() temporarily disabled");
    // TODO get matrix setup happening
  }


  /**
   * See notes with the curve() function.
   */
  public void curveVertex(float x, float y, float z) {
    depthErrorXYZ("curveVertex");
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
      path.closePath();
      draw_shape(path);
      break;
    }
  }



  //////////////////////////////////////////////////////////////

  // STROKE/FILL/DRAW


  //protected void fill_shape(Shape s) {
  protected void fill_shape(Path s) {
    if (fill) {
      //graphics.setColor(fillColorObject);
      //graphics.fill(s);
    }
  }

  //protected void stroke_shape(Shape s) {
  protected void stroke_shape(Path s) {
    if (stroke) {
      //graphics.setColor(strokeColorObject);
      //graphics.draw(s);
    }
  }

  //protected void draw_shape(Shape s) {
  protected void draw_shape(Path s) {
    if (fill) {
      //graphics.setColor(fillColorObject);
      //graphics.fill(s);
    }
    if (stroke) {
      //graphics.setColor(strokeColorObject);
      //graphics.draw(s);
    }
  }



  //////////////////////////////////////////////////////////////

  // POINT


  public void point(float x, float y) {
    // TODO
  }


  public void point(float x, float y, float z) {
    depthErrorXYZ("point");
  }


  public void line(float x1, float y1, float x2, float y2) {
    // TODO
  }


  public void line(float x1, float y1, float z1,
                   float x2, float y2, float z2) {
    depthErrorXYZ("line");
  }


  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    // TODO
  }


  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
    // TODO
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

    if (x1 > x2) {
      float temp = x1; x1 = x2; x2 = temp;
    }

    if (y1 > y2) {
      float temp = y1; y1 = y2; y2 = temp;
    }

    rectImpl(x1, y1, x2, y2);
  }


  protected void rectImpl(float x1, float y1, float x2, float y2) {
    // TODO write rect drawing function
  }



  //////////////////////////////////////////////////////////////

  // ELLIPSE AND ARC


  public void ellipseMode(int mode) {
    ellipseMode = mode;
  }


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

    if (w < 0) {  // undo negative width
      x += w;
      w = -w;
    }

    if (h < 0) {  // undo negative height
      y += h;
      h = -h;
    }

    ellipseImpl(x, y, w, h);
  }


  protected void ellipseImpl(float x, float y, float w, float h) {
    // TODO draw an ellipse
  }


  /**
   * Identical parameters and placement to ellipse,
   * but draws only an arc of that ellipse.
   * <p/>
   * start and stop are always radians because angleMode() was goofy.
   * ellipseMode() sets the placement.
   * <p/>
   * also tries to be smart about start < stop.
   */
  public void arc(float a, float b, float c, float d,
                  float start, float stop) {
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

    //if (angleMode == DEGREES) {
    //start = start * DEG_TO_RAD;
    //stop = stop * DEG_TO_RAD;
    //}
    // before running a while loop like this,
    // make sure it will exit at some point.
    if (Float.isInfinite(start) || Float.isInfinite(stop)) return;
    while (stop < start) stop += TWO_PI;

    arcImpl(x, y, w, h, start, stop);
  }


  protected void arcImpl(float x, float y, float w, float h,
                         float start, float stop) {
  }



  //////////////////////////////////////////////////////////////

  // 3D SHAPES


  public void box(float size) {
    depthError("box");
  }

  public void box(float w, float h, float d) {
    depthError("box");
  }

  public void sphereDetail(int res) {
    depthError("sphereDetail");
  }

  public void sphere(float r) {
    depthError("sphere");
  }



  //////////////////////////////////////////////////////////////

  // CURVES


  /**
   * Evalutes quadratic bezier at point t for points a, b, c, d.
   * t varies between 0 and 1, and a and d are the on curve points,
   * b and c are the control points. this can be done once with the
   * x coordinates and a second time with the y coordinates to get
   * the location of a bezier curve at t.
   * <P>
   * For instance, to convert the following example:<PRE>
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
   *   float x = bezierPoint(85, 10, 90, 15, t);
   *   float y = bezierPoint(20, 10, 90, 80, t);
   *   vertex(x, y);
   * }
   * endShape();</PRE>
   */
  public float bezierPoint(float a, float b, float c, float d, float t) {
    float t1 = 1.0f - t;
    return a*t1*t1*t1 + 3*b*t*t1*t1 + 3*c*t*t*t1 + d*t*t*t;
  }


  /**
   * Provide the tangent at the given point on the bezier curve.
   * Based on code from v3ga's wordstree sketch.
   */
  public float bezierTangent(float a, float b, float c, float d, float t) {
    float t1 = 1.0f - t;

    return (a *  3 * t*t +
            b *  3 * t * (2 - 3*t) +
            c *  3 * (3*t*t - 4*t + 1) +
            d * -3 * t1*t1);
  }


  protected void bezier_init() {
    bezierDetail(bezierDetail);
  }


  public void bezierDetail(int detail) {
    if (bezier_forward == null) {
      bezier_forward = new float[4][4];
      bezier_draw = new float[4][4];
    }
    bezierDetail = detail;
    bezierInited = true;

    // setup matrix for forward differencing to speed up drawing
    setup_spline_forward(detail, bezier_forward);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    mult_spline_matrix(bezier_forward, bezier_basis, bezier_draw, 4);
  }


  /**
   * Draw a quadratic bezier curve. The first and last points are
   * the on-curve points. The middle two are the 'control' points,
   * or 'handles' in an application like Illustrator.
   * <P>
   * Identical to typing:
   * <PRE>beginShape();
   * vertex(x1, y1);
   * bezierVertex(x2, y2, x3, y3, x4, y4);
   * endShape();
   * </PRE>
   * In Postscript-speak, this would be:
   * <PRE>moveto(x1, y1);
   * curveto(x2, y2, x3, y3, x4, y4);</PRE>
   * If you were to try and continue that curve like so:
   * <PRE>curveto(x5, y5, x6, y6, x7, y7);</PRE>
   * This would be done in processing by adding these statements:
   * <PRE>bezierVertex(x5, y5, x6, y6, x7, y7)
   * </PRE>
   * To draw a cubic (instead of quadratic) curve,
   * use the control point twice by doubling it:
   * <PRE>bezier(x1, y1, cx, cy, cx, cy, x2, y2);</PRE>
   */
  public void bezier(float x1, float y1,
                     float x2, float y2,
                     float x3, float y3,
                     float x4, float y4) {
    beginShape(LINE_STRIP);
    vertex(x1, y1);
    bezierVertex(x2, y2, x3, y3, x4, y4);
    endShape();
  }


  public void bezier(float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     float x3, float y3, float z3,
                     float x4, float y4, float z4) {
    depthErrorXYZ("bezier");
  }


  //////////////////////////////////////////////////////////////


  /**
   * Get a location along a catmull-rom curve segment.
   *
   * @param t Value between zero and one for how far along the segment
   */
  public float curvePoint(float a, float b, float c, float d, float t) {
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


  public void curveDetail(int detail) {
    curve_mode(detail, curveTightness);
  }


  public void curveTightness(float tightness) {
    curve_mode(curveDetail, tightness);
  }


  protected void curve_init() {
    curve_mode(curveDetail, curveTightness);
  }


  /**
   * Set the number of segments to use when drawing a Catmull-Rom
   * curve, and setting the s parameter, which defines how tightly
   * the curve fits to each vertex. Catmull-Rom curves are actually
   * a subset of this curve type where the s is set to zero.
   * <P>
   * (This function is not optimized, since it's not expected to
   * be called all that often. there are many juicy and obvious
   * opimizations in here, but it's probably better to keep the
   * code more readable)
   */
  protected void curve_mode(int segments, float s) {
    curveDetail = segments;

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

    if (bezierBasisInverse == null) {
      bezierBasisInverse = new PMatrix(bezierBasis).invert();
    }

    // hack here to get PGraphics2 working
    curveToBezierMatrix = new PMatrix(c[0][0], c[0][1], c[0][2], c[0][3],
                                      c[1][0], c[1][1], c[1][2], c[1][3],
                                      c[2][0], c[2][1], c[2][2], c[2][3],
                                      c[3][0], c[3][1], c[3][2], c[3][3]);
    curveToBezierMatrix.preApply(bezierBasisInverse);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    mult_spline_matrix(curve_forward, curve_basis, curve_draw, 4);
  }


  /**
   * Draws a segment of Catmull-Rom curve.
   * <P>
   * As of 0070, this function no longer doubles the first and
   * last points. The curves are a bit more boring, but it's more
   * mathematically correct, and properly mirrored in curvePoint().
   * <P>
   * Identical to typing out:<PRE>
   * beginShape();
   * curveVertex(x1, y1);
   * curveVertex(x2, y2);
   * curveVertex(x3, y3);
   * curveVertex(x4, y4);
   * endShape();
   * </PRE>
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
    depthErrorXYZ("curve");
  }


  //////////////////////////////////////////////////////////////


  /**
   * Setup forward-differencing matrix to be used for speedy
   * curve rendering. It's based on using a specific number
   * of curve segments and just doing incremental adds for each
   * vertex of the segment, rather than running the mathematically
   * expensive cubic equation.
   * @param segments number of curve segments to use when drawing
   */
  protected void setup_spline_forward(int segments, float fwd[][]) {
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
  protected void mult_spline_matrix(float m[][], float g[][],
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
   * <P>
   * the x0, y0, z0 points are the point that's being used as
   * the start, and also as the accumulator. for bezier curves,
   * the x1, y1, z1 are the first point drawn, and added to.
   * for catmull-rom curves, the first control point (x2, y2, z2)
   * is the first drawn point, and is accumulated to.
   */
  protected void spline2_segment(int offset, int start,
                                 float m[][], int segments) {
    float x1 = splineVertices[offset][MX];
    float y1 = splineVertices[offset][MY];

    float x2 = splineVertices[offset+1][MX];
    float y2 = splineVertices[offset+1][MY];

    float x3 = splineVertices[offset+2][MX];
    float y3 = splineVertices[offset+2][MY];

    float x4 = splineVertices[offset+3][MX];
    float y4 = splineVertices[offset+3][MY];

    float x0 = splineVertices[start][MX];
    float y0 = splineVertices[start][MY];

    float xplot1 = m[1][0]*x1 + m[1][1]*x2 + m[1][2]*x3 + m[1][3]*x4;
    float xplot2 = m[2][0]*x1 + m[2][1]*x2 + m[2][2]*x3 + m[2][3]*x4;
    float xplot3 = m[3][0]*x1 + m[3][1]*x2 + m[3][2]*x3 + m[3][3]*x4;

    float yplot1 = m[1][0]*y1 + m[1][1]*y2 + m[1][2]*y3 + m[1][3]*y4;
    float yplot2 = m[2][0]*y1 + m[2][1]*y2 + m[2][2]*y3 + m[2][3]*y4;
    float yplot3 = m[3][0]*y1 + m[3][1]*y2 + m[3][2]*y3 + m[3][3]*y4;

    // vertex() will reset splineVertexCount, so save it
    int splineVertexSaved = splineVertexCount;
    vertex(x0, y0);
    for (int j = 0; j < segments; j++) {
      x0 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y0 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      vertex(x0, y0);
    }
    splineVertexCount = splineVertexSaved;
  }


  protected void spline3_segment(int offset, int start,
                                 float m[][], int segments) {
    float x1 = splineVertices[offset+0][MX];
    float y1 = splineVertices[offset+0][MY];
    float z1 = splineVertices[offset+0][MZ];

    float x2 = splineVertices[offset+1][MX];
    float y2 = splineVertices[offset+1][MY];
    float z2 = splineVertices[offset+1][MZ];

    float x3 = splineVertices[offset+2][MX];
    float y3 = splineVertices[offset+2][MY];
    float z3 = splineVertices[offset+2][MZ];

    float x4 = splineVertices[offset+3][MX];
    float y4 = splineVertices[offset+3][MY];
    float z4 = splineVertices[offset+3][MZ];

    float x0 = splineVertices[start][MX];
    float y0 = splineVertices[start][MY];
    float z0 = splineVertices[start][MZ];

    float xplot1 = m[1][0]*x1 + m[1][1]*x2 + m[1][2]*x3 + m[1][3]*x4;
    float xplot2 = m[2][0]*x1 + m[2][1]*x2 + m[2][2]*x3 + m[2][3]*x4;
    float xplot3 = m[3][0]*x1 + m[3][1]*x2 + m[3][2]*x3 + m[3][3]*x4;

    float yplot1 = m[1][0]*y1 + m[1][1]*y2 + m[1][2]*y3 + m[1][3]*y4;
    float yplot2 = m[2][0]*y1 + m[2][1]*y2 + m[2][2]*y3 + m[2][3]*y4;
    float yplot3 = m[3][0]*y1 + m[3][1]*y2 + m[3][2]*y3 + m[3][3]*y4;

    float zplot1 = m[1][0]*z1 + m[1][1]*z2 + m[1][2]*z3 + m[1][3]*z4;
    float zplot2 = m[2][0]*z1 + m[2][1]*z2 + m[2][2]*z3 + m[2][3]*z4;
    float zplot3 = m[3][0]*z1 + m[3][1]*z2 + m[3][2]*z3 + m[3][3]*z4;

    // vertex() will reset splineVertexCount, so save it
    int cvertexSaved = splineVertexCount;
    vertex(x0, y0, z0);
    for (int j = 0; j < segments; j++) {
      x0 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y0 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      z0 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
      vertex(x0, y0, z0);
    }
    splineVertexCount = cvertexSaved;
  }



  //////////////////////////////////////////////////////////////

  // IMAGE


  public void image(PImage image, float x, float y) {
    imageImpl(image,
              x, y, x+image.width, y+image.height,
              0, 0, image.width, image.height);
  }


  public void image(PImage image,
                    float x, float y, float c, float d) {
    image(image, x, y, c, d, 0, 0, image.width, image.height);
  }


  /**
   * u, v coordinates are always based on image space location,
   * regardless of the current textureMode().
   */
  public void image(PImage image,
                    float a, float b, float c, float d,
                    int u1, int v1, int u2, int v2) {
    if (imageMode == CORNER) {
      if (c < 0) {  // reset a negative width
        a += c; c = -c;
      }
      if (d < 0) {  // reset a negative height
        b += d; d = -d;
      }

      imageImpl(image,
                a, b, a + c, b + d,
                u1, v1, u2, v2);

    } else if (imageMode == CORNERS) {
      if (c < a) {  // reverse because x2 < x1
        float temp = a; a = c; c = temp;
      }
      if (d < b) {  // reverse because y2 < y1
        float temp = b; b = d; d = temp;
      }

      imageImpl(image,
                a, b, c, d,
                u1, v1, u2, v2);
    }
  }


  /**
   * Expects x1, y1, x2, y2 coordinates where (x2 >= x1) and (y2 >= y1).
   * If tint() has been called, the image will be colored.
   */
  protected void imageImpl(PImage image,
                           float x1, float y1, float x2, float y2,
                           int u1, int v1, int u2, int v2) {
    // TODO blit an image to the screen
    System.err.println("unimplemented imageImpl() in PGraphics");
  }



  //////////////////////////////////////////////////////////////

  // TEXT/FONTS


  /**
   * Sets the alignment of the text to one of LEFT, CENTER, or RIGHT.
   */
  public void textAlign(int align) {
    textAlign = align;
  }


  /**
   * Returns the ascent of the current font at the current size.
   * This is a method, rather than a variable inside the PGraphics object
   * because it requires calculation.
   */
  public float textAscent() {
    if (textFont == null) {
      throw new RuntimeException("use textFont() before textAscent()");
    }

    return textFont.ascent() * textSize;
  }


  /**
   * Returns the descent of the current font at the current size.
   * This is a method, rather than a variable inside the PGraphics object
   * because it requires calculation.
   */
  public float textDescent() {
    if (textFont != null) {
      return textFont.descent() * textSize;

    } else {
      throw new RuntimeException("use textFont() before textDescent()");
    }
  }


  /**
   * Sets the current font. The font's size will be the "natural"
   * size of this font (the size that was set when using "Create Font").
   * The leading will also be reset.
   */
  public void textFont(PFont which) {
    if (which != null) {
      textFont = which;
      textFontNative = which.font;
      textSize(textFont.size);

    } else {
      throw new RuntimeException("a null PFont was passed to textFont()");
    }
  }


  /**
   * Useful function to set the font and size at the same time.
   */
  public void textFont(PFont which, float size) {
    textFont(which);
    textSize(size);
  }


  /**
   * Set the text leading to a specific value. If using a custom
   * value for the text leading, you'll have to call textLeading()
   * again after any calls to textSize().
   */
  public void textLeading(float leading) {
    textLeading = leading;
  }


  /**
   * Sets the text rendering/placement to be either SCREEN (direct
   * to the screen, exact coordinates, only use the font's original size)
   * or MODEL (the default, where text is manipulated by translate() and
   * can have a textSize). The text size cannot be set when using
   * textMode(SCREEN), because it uses the pixels directly from the font.
   */
  public void textMode(int mode) {
    if ((mode != SCREEN) && (mode != MODEL)) {
      throw new RuntimeException("Only textMode(SCREEN) or textMode(MODEL) " +
                                 "can be used. Maybe you meant textAlign()?");
    }

    if (textFont != null) {
      textMode = mode;

      // reset the font to its natural size
      // (helps with width calculations and all that)
      if (textMode == SCREEN) {
        textSize(textFont.size);
      }

    } else {
      throw new RuntimeException("use textFont() before textMode()");
    }
  }


  /**
   * Sets the text size, also resets the value for the leading.
   */
  public void textSize(float size) {
    if (textFont != null) {
      if ((textMode == SCREEN) && (size != textFont.size)) {
        throw new RuntimeException("textSize() cannot be used with " +
                                   "textMode(SCREEN)");
      }
      textSize = size;
      //textLeading = textSize *
      //  ((textFont.ascent() + textFont.descent()) * 1.275f);
      textLeading = (textAscent() + textDescent()) * 1.275f;

    } else {
      throw new RuntimeException("Use textFont() before textSize()");
    }
  }


  // ........................................................


  public float textWidth(char c) {
    textBuffer[0] = c;
    return textWidthImpl(textBuffer, 0, 1);
  }


  /**
   * Return the width of a line of text. If the text has multiple
   * lines, this returns the length of the longest line.
   */
  public float textWidth(String str) {
    if (textFont == null) {
      throw new RuntimeException("use textFont() before textWidth()");
    }

    int length = str.length();
    if (length > textWidthBuffer.length) {
      textWidthBuffer = new char[length + 10];
    }
    str.getChars(0, length, textWidthBuffer, 0);

    float wide = 0;
    int index = 0;
    int start = 0;

    while (index < length) {
      if (textWidthBuffer[index] == '\n') {
        wide = Math.max(wide, textWidthImpl(textWidthBuffer, start, index));
        start = index+1;
      }
      index++;
    }
    if (start < length) {
      wide = Math.max(wide, textWidthImpl(textWidthBuffer, start, index));
    }
    return wide;
  }


  /**
   * Implementation of returning the text width of
   * the chars [start, stop) in the buffer.
   * Unlike the previous version that was inside PFont, this will
   * return the size not of a 1 pixel font, but the actual current size.
   */
  protected float textWidthImpl(char buffer[], int start, int stop) {
    float wide = 0;
    for (int i = start; i < stop; i++) {
      // could add kerning here, but it just ain't implemented
      wide += textFont.width(buffer[i]) * textSize;
    }
    return wide;
  }


  // ........................................................


  /**
   * Write text where we just left off.
   */
  public void text(char c) {
    text(c, textX, textY, textZ);
  }


  /**
   * Draw a single character on screen.
   * Extremely slow when used with textMode(SCREEN) and Java 2D,
   * because loadPixels has to be called first and updatePixels last.
   */
  public void text(char c, float x, float y) {
    if (textFont == null) {
      throw new RuntimeException("use textFont() before text()");
    }

    if (textMode == SCREEN) loadPixels();

    textBuffer[0] = c;
    textLineImpl(textBuffer, 0, 1, x, y);

    if (textMode == SCREEN) updatePixels();
  }


  /**
   * Draw a single character on screen (with a z coordinate)
   */
  public void text(char c, float x, float y, float z) {
    if ((z != 0) && (textMode == SCREEN)) {
      String msg = "textMode(SCREEN) cannot have a z coordinate";
      throw new RuntimeException(msg);
    }

    if (z != 0) translate(0, 0, z);  // slowness, badness

    text(c, x, y);
    textZ = z;

    if (z != 0) translate(0, 0, -z);
  }


  /**
   * Write text where we just left off.
   */
  public void text(String str) {
    text(str, textX, textY, textZ);
  }


  /**
   * Draw a chunk of text.
   * Newlines that are \n (Unix newline or linefeed char, ascii 10)
   * are honored, but \r (carriage return, Windows and Mac OS) are
   * ignored.
   */
  public void text(String str, float x, float y) {
    if (textMode == SCREEN) loadPixels();

    int length = str.length();
    if (length > textBuffer.length) {
      textBuffer = new char[length + 10];
    }
    str.getChars(0, length, textBuffer, 0);

    int start = 0;
    int index = 0;
    while (index < length) {
      if (textBuffer[index] == '\n') {
        textLineImpl(textBuffer, start, index, x, y);
        start = index + 1;
        y += textLeading;
      }
      index++;
    }
    if (start < length) {
      textLineImpl(textBuffer, start, index, x, y);
    }
    if (textMode == SCREEN) updatePixels();
  }


  /**
   * Same as above but with a z coordinate.
   */
  public void text(String str, float x, float y, float z) {
    if ((z != 0) && (textMode == SCREEN)) {
      String msg = "textMode(SCREEN) cannot have a z coordinate";
      throw new RuntimeException(msg);
    }

    if (z != 0) translate(0, 0, z);  // slow!

    text(str, x, y);
    textZ = z;

    if (z != 0) translate(0, 0, -z);
  }


  /**
   * Handles placement of a text line, then calls textLinePlaced
   * to actually render at the specific point.
   */
  protected void textLineImpl(char buffer[], int start, int stop,
                              float x, float y) {
    if (textAlign == CENTER) {
      x -= textWidthImpl(buffer, start, stop) / 2f;

    } else if (textAlign == RIGHT) {
      x -= textWidthImpl(buffer, start, stop);
    }
    textLinePlacedImpl(buffer, start, stop, x, y);
  }


  protected void textLinePlacedImpl(char buffer[], int start, int stop,
                                    float x, float y) {
    for (int index = start; index < stop; index++) {
      textCharImpl(buffer[index], x, y); //, 0); //z);

      // this doesn't account for kerning
      x += textWidth(buffer[index]);
    }
    textX = x;
    textY = y;
    textZ = 0;  // this will get set by the caller if non-zero
  }


  /**
   * Draw text in a box that is constrained to a particular size.
   * The current rectMode() determines what the coordinates mean
   * (whether x1/y1/x2/y2 or x/y/w/h).
   * <P/>
   * Note that the x,y coords of the start of the box
   * will align with the *ascent* of the text, not the baseline,
   * as is the case for the other text() functions.
   * <P/>
   * Newlines that are \n (Unix newline or linefeed char, ascii 10)
   * are honored, and \r (carriage return, Windows and Mac OS) are
   * ignored.
   */
  public void text(String str, float x1, float y1, float x2, float y2) {
    if (textFont == null) {
      throw new RuntimeException("use textFont() before text()");
    }

    if (textMode == SCREEN) loadPixels();

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

    float spaceWidth = textWidth(' ');
    float runningX = x1; //boxX1;
    float currentY = y1; //boxY1;
    float boxWidth = x2 - x1; //boxX2 - boxX1;

    // lineX is the position where the text starts, which is adjusted
    // to left/center/right based on the current textAlign
    float lineX = x1; //boxX1;
    if (textAlign == CENTER) {
      lineX = lineX + boxWidth/2f;
    } else if (textAlign == RIGHT) {
      lineX = x2; //boxX2;
    }

    // ala illustrator, the text itself must fit inside the box
    currentY += textAscent(); //ascent() * textSize;
    // if the box is already too small, tell em to f off
    if (currentY > y2) return; //boxY2) return;

    int length = str.length();
    if (length > textBuffer.length) {
      textBuffer = new char[length + 10];
    }
    str.getChars(0, length, textBuffer, 0);

    int wordStart = 0;
    int wordStop = 0;
    int lineStart = 0;
    int index = 0;
    while (index < length) {
      if ((textBuffer[index] == ' ') || (index == length-1)) {
        // boundary of a word
        float wordWidth = textWidthImpl(textBuffer, wordStart, index);

        if (runningX + wordWidth > x2) { //boxX2) {
          if (runningX == x1) { //boxX1) {
            // if this is the first word, and its width is
            // greater than the width of the text box,
            // then break the word where at the max width,
            // and send the rest of the word to the next line.
            do {
              index--;
              if (index == wordStart) {
                // not a single char will fit on this line. screw 'em.
                //System.out.println("screw you");
                return;
              }
              wordWidth = textWidthImpl(textBuffer, wordStart, index);
            } while (wordWidth > boxWidth);
            textLineImpl(textBuffer, lineStart, index, lineX, currentY);

          } else {
            // next word is too big, output current line
            // and advance to the next line
            textLineImpl(textBuffer, lineStart, wordStop, lineX, currentY);
            // only increment index if a word wasn't broken inside the
            // do/while loop above.. also, this is a while() loop too,
            // because multiple spaces don't count for shit when they're
            // at the end of a line like this.

            index = wordStop;  // back that ass up
            while ((index < length) &&
                   (textBuffer[index] == ' ')) {
              index++;
            }
          }
          lineStart = index;
          wordStart = index;
          wordStop = index;
          runningX = x1; //boxX1;
          currentY += textLeading;
          //if (currentY > boxY2) return;  // box is now full
          if (currentY > y2) return;  // box is now full

        } else {
          runningX += wordWidth + spaceWidth;
          // on to the next word
          wordStop = index;
          wordStart = index + 1;
        }

      } else if (textBuffer[index] == '\n') {
        if (lineStart != index) {  // if line is not empty
          textLineImpl(textBuffer, lineStart, index, lineX, currentY);
        }
        lineStart = index + 1;
        wordStart = lineStart;
        currentY += textLeading;
        //if (currentY > boxY2) return;  // box is now full
        if (currentY > y2) return;  // box is now full
      }
      index++;
    }
    if ((lineStart < length) && (lineStart != index)) {
      textLineImpl(textBuffer, lineStart, index, lineX, currentY);
    }

    if (textMode == SCREEN) updatePixels();
  }


  public void text(String s, float x1, float y1, float x2, float y2, float z) {
    if ((z != 0) && (textMode == SCREEN)) {
      String msg = "textMode(SCREEN) cannot have a z coordinate";
      throw new RuntimeException(msg);
    }

    if (z != 0) translate(0, 0, z);  // slowness, badness

    text(s, x1, y1, x2, y2);
    textZ = z;

    if (z != 0) translate(0, 0, -z);  // TEMPORARY HACK! SLOW!
  }


  public void text(int num, float x, float y) {
    text(String.valueOf(num), x, y);
  }


  public void text(int num, float x, float y, float z) {
    text(String.valueOf(num), x, y, z);
  }


  /**
   * This does a basic number formatting, to avoid the
   * generally ugly appearance of printing floats.
   * Users who want more control should use their own nf() cmmand,
   * or if they want the long, ugly version of float,
   * use String.valueOf() to convert the float to a String first.
   */
  public void text(float num, float x, float y) {
    text(PApplet.nfs(num, 0, 3), x, y);
  }


  public void text(float num, float x, float y, float z) {
    text(PApplet.nfs(num, 0, 3), x, y, z);
  }


  // ........................................................


  //font.getStringBounds(text, g2.getFontRenderContext()).getWidth();

  protected void textCharImpl(char ch, float x, float y) { //, float z) {
    int index = textFont.index(ch);
    if (index == -1) return;

    PImage glyph = textFont.images[index];

    if (textMode == MODEL) {
      float high    = (float) textFont.height[index]     / textFont.fheight;
      float bwidth  = (float) textFont.width[index]      / textFont.fwidth;
      float lextent = (float) textFont.leftExtent[index] / textFont.fwidth;
      float textent = (float) textFont.topExtent[index]  / textFont.fheight;

      float x1 = x + lextent * textSize;
      float y1 = y - textent * textSize;
      float x2 = x1 + bwidth * textSize;
      float y2 = y1 + high * textSize;

      textCharModelImpl(glyph,
                        x1, y1, x2, y2,
                        //x1, y1, z, x2, y2, z,
                        textFont.width[index], textFont.height[index]);

    } else if (textMode == SCREEN) {
      int xx = (int) x + textFont.leftExtent[index];;
      int yy = (int) y - textFont.topExtent[index];

      int w0 = textFont.width[index];
      int h0 = textFont.height[index];

      textCharScreenImpl(glyph, xx, yy, w0, h0);
    }
  }


  protected void textCharModelImpl(PImage glyph,
                                   float x1, float y1, //float z1,
                                   float x2, float y2, //float z2,
                                   int u2, int v2) {
    boolean savedTint = tint;
    int savedTintColor = tintColor;
    float savedTintR = tintR;
    float savedTintG = tintG;
    float savedTintB = tintB;
    float savedTintA = tintA;
    boolean savedTintAlpha = tintAlpha;

    tint = true;
    tintColor = fillColor;
    tintR = fillR;
    tintG = fillG;
    tintB = fillB;
    tintA = fillA;
    tintAlpha = fillAlpha;

    imageImpl(glyph, x1, y1, x2, y2, 0, 0, u2, v2);

    tint = savedTint;
    tintColor = savedTintColor;
    tintR = savedTintR;
    tintG = savedTintG;
    tintB = savedTintB;
    tintA = savedTintA;
    tintAlpha = savedTintAlpha;
  }


  // should take image, int x1, int y1, and x2, y2

  protected void textCharScreenImpl(PImage glyph,
                                    int xx, int yy, //int x2, int y2,
                                    int w0, int h0) {
    /*
    System.out.println("textimplscreen");
    rectMode(CORNER);
    stroke(255);
    rect(xx, yy, w0, h0);
    */

    int x0 = 0;
    int y0 = 0;

    if ((xx >= width) || (yy >= height) ||
        (xx + w0 < 0) || (yy + h0 < 0)) return;

    if (xx < 0) {
      x0 -= xx;
      w0 += xx;
      xx = 0;
    }
    if (yy < 0) {
      y0 -= yy;
      h0 += yy;
      yy = 0;
    }
    if (xx + w0 > width) {
      w0 -= ((xx + w0) - width);
    }
    if (yy + h0 > height) {
      h0 -= ((yy + h0) - height);
    }

    int fr = fillRi;
    int fg = fillGi;
    int fb = fillBi;
    int fa = fillAi;

    int pixels1[] = glyph.pixels; //images[glyph].pixels;

    // TODO this can be optimized a bit
    for (int row = y0; row < y0 + h0; row++) {
      for (int col = x0; col < x0 + w0; col++) {
        int a1 = (fa * pixels1[row * textFont.twidth + col]) >> 8;
        int a2 = a1 ^ 0xff;
        //int p1 = pixels1[row * textFont.width[glyph] + col];
        int p1 = pixels1[row * glyph.width + col];
        int p2 = pixels[(yy + row-y0)*width + (xx+col-x0)];

        pixels[(yy + row-y0)*width + xx+col-x0] =
          (0xff000000 |
           (((a1 * fr + a2 * ((p2 >> 16) & 0xff)) & 0xff00) << 8) |
           (( a1 * fg + a2 * ((p2 >>  8) & 0xff)) & 0xff00) |
           (( a1 * fb + a2 * ( p2        & 0xff)) >> 8));
      }
    }
  }



  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS


  public void translate(float tx, float ty) {
    m02 += tx*m00 + ty*m01 + m02;
    m12 += tx*m10 + ty*m11 + m12;
  }


  public void translate(float tx, float ty, float tz) {
    depthErrorXYZ("translate");
  }


  /**
   * Two dimensional rotation. Same as rotateZ (this is identical
   * to a 3D rotation along the z-axis) but included for clarity --
   * it'd be weird for people drawing 2D graphics to be using rotateZ.
   * And they might kick our a-- for the confusion.
   */
  public void rotate(float angle) {
    float c = (float) Math.cos(angle);
    float s = (float) Math.sin(angle);

    applyMatrix(c, -s, 0,  s, c, 0);
  }


  public void rotateX(float angle) {
    depthError("rotateX");
  }

  public void rotateY(float angle) {
    depthError("rotateY");
  }

  public void rotateZ(float angle) {
    depthError("rotateZ");
  }


  public void rotate(float angle, float vx, float vy, float vz) {
    throw new RuntimeException("rotate(angle, x, y, z) " +
                               "can only be used with P3D or OPENGL");
  }


  /**
   * This will scale in all three dimensions, but not set the
   * dimensions higher than two, in case this is still 2D mode.
   */
  public void scale(float s) {
    applyMatrix(s, 0, 0,
                0, s, 0);
  }


  public void scale(float sx, float sy) {
    applyMatrix(sx, 0, 0,
                0, sy, 0);
  }


  public void scale(float x, float y, float z) {
    depthErrorXYZ("scale");
  }



  //////////////////////////////////////////////////////////////

  // TRANSFORMATION MATRIX


  public void pushMatrix() {
    if (matrixStackDepth+1 == MATRIX_STACK_DEPTH) {
      throw new RuntimeException("too many calls to pushMatrix()");
    }
    float mat[] = matrixStack[matrixStackDepth];
    mat[0] = m00; mat[1] = m01; mat[2] = m02;
    mat[3] = m10; mat[4] = m11; mat[5] = m12;
    matrixStackDepth++;
  }


  public void popMatrix() {
    if (matrixStackDepth == 0) {
      throw new RuntimeException("too many calls to popMatrix() " +
                                 "(and not enough to pushMatrix)");
    }
    matrixStackDepth--;
    float mat[] = matrixStack[matrixStackDepth];
    m00 = mat[0]; m01 = mat[1]; m02 = mat[2];
    m10 = mat[3]; m11 = mat[4]; m12 = mat[5];
  }


  /**
   * Load identity as the transform/model matrix.
   * Same as glLoadIdentity().
   */
  public void resetMatrix() {
    m00 = 1; m01 = 0; m02 = 0;
    m10 = 0; m11 = 1; m12 = 0;
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


  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    throw new RuntimeException("applyMatrix() with a 4x4 matrix " +
                               "can only be used with OPENGL or P3D");
  }


  /**
   * Loads the current matrix into m00, m01 etc (or modelview and
   * projection when using 3D) so that the values can be read.
   * <P/>
   * Note that there is no "updateMatrix" because that gets too
   * complicated (unnecessary) when considering the 3D matrices.
   */
  public void loadMatrix() {
    // no-op on base PGraphics because they're used directly
  }


  /**
   * Print the current model (or "transformation") matrix.
   */
  public void printMatrix() {
    loadMatrix();  // just to make sure

    float big = Math.abs(m00);
    if (Math.abs(m01) > big) big = Math.abs(m01);
    if (Math.abs(m02) > big) big = Math.abs(m02);
    if (Math.abs(m10) > big) big = Math.abs(m10);
    if (Math.abs(m11) > big) big = Math.abs(m11);
    if (Math.abs(m12) > big) big = Math.abs(m12);

    // avoid infinite loop
    if (Float.isNaN(big) || Float.isInfinite(big)) {
      big = 1000000; // set to something arbitrary
    }

    int d = 1;
    int bigi = (int) big;
    while ((bigi /= 10) != 0) d++;  // cheap log()

    System.out.println(PApplet.nfs(m00, d, 4) + " " +
                       PApplet.nfs(m01, d, 4) + " " +
                       PApplet.nfs(m02, d, 4));

    System.out.println(PApplet.nfs(m10, d, 4) + " " +
                       PApplet.nfs(m11, d, 4) + " " +
                       PApplet.nfs(m12, d, 4));

    System.out.println();
  }



  //////////////////////////////////////////////////////////////

  // CAMERA (none are supported in 2D)


  public void beginCamera() {
    depthError("beginCamera");
  }

  public void endCamera() {
    depthError("endCamera");
  }

  public void camera() {
    depthError("camera");
  }

  public void camera(float eyeX, float eyeY, float eyeZ,
                     float centerX, float centerY, float centerZ,
                     float upX, float upY, float upZ) {
    depthError("camera");
  }

  public void printCamera() {
    depthError("printCamera");
  }



  //////////////////////////////////////////////////////////////

  // PROJECTION (none are supported in 2D)


  public void ortho() {
    depthError("ortho");
  }

  public void ortho(float left, float right,
                    float bottom, float top,
                    float near, float far) {
    depthError("ortho");
  }

  public void perspective() {
    depthError("perspective");
  }

  public void perspective(float fovy, float aspect, float zNear, float zFar) {
    depthError("perspective");
  }

  public void frustum(float left, float right, float bottom,
                      float top, float znear, float zfar) {
    depthError("frustum");
  }

  public void printProjection() {
    depthError("printCamera");
  }



  //////////////////////////////////////////////////////////////

  // SCREEN TRANSFORMS


  /**
   * Given an x and y coordinate, returns the x position of where
   * that point would be placed on screen, once affected by translate(),
   * scale(), or any other transformations.
   */
  public float screenX(float x, float y) {
    return m00*x + m01*y + m02;
  }


  /**
   * Given an x and y coordinate, returns the y position of where
   * that point would be placed on screen, once affected by translate(),
   * scale(), or any other transformations.
   */
  public float screenY(float x, float y) {
    return m10*x + m11*y + m12;
  }


  /**
   * Maps a three dimensional point to its placement on-screen.
   * <P>
   * Given an (x, y, z) coordinate, returns the x position of where
   * that point would be placed on screen, once affected by translate(),
   * scale(), or any other transformations.
   */
  public float screenX(float x, float y, float z) {
    depthErrorXYZ("screenX");
    return 0;
  }


  /**
   * Maps a three dimensional point to its placement on-screen.
   * <P>
   * Given an (x, y, z) coordinate, returns the y position of where
   * that point would be placed on screen, once affected by translate(),
   * scale(), or any other transformations.
   */
  public float screenY(float x, float y, float z) {
    depthErrorXYZ("screenY");
    return 0;
  }


  /**
   * Maps a three dimensional point to its placement on-screen.
   * <P>
   * Given an (x, y, z) coordinate, returns its z value.
   * This value can be used to determine if an (x, y, z) coordinate
   * is in front or in back of another (x, y, z) coordinate.
   * The units are based on how the zbuffer is set up, and don't
   * relate to anything "real". They're only useful for in
   * comparison to another value obtained from screenZ(),
   * or directly out of the zbuffer[].
   */
  public float screenZ(float x, float y, float z) {
    depthErrorXYZ("screenZ");
    return 0;
  }


  /**
   * Returns the model space x value for an x, y, z coordinate.
   * <P>
   * This will give you a coordinate after it has been transformed
   * by translate(), rotate(), and camera(), but not yet transformed
   * by the projection matrix. For instance, his can be useful for
   * figuring out how points in 3D space relate to the edge
   * coordinates of a shape.
   */
  public float modelX(float x, float y, float z) {
    depthError("modelX");
    return 0;
  }


  /**
   * Returns the model space y value for an x, y, z coordinate.
   */
  public float modelY(float x, float y, float z) {
    depthError("modelY");
    return 0;
  }


  /**
   * Returns the model space z value for an x, y, z coordinate.
   */
  public float modelZ(float x, float y, float z) {
    depthError("modelZ");
    return 0;
  }



  //////////////////////////////////////////////////////////////

  // COLOR


  public void colorMode(int mode) {
    colorMode(mode, colorModeX, colorModeY, colorModeZ, colorModeA);
  }


  public void colorMode(int mode, float max) {
    colorMode(mode, max, max, max, max);
  }


  /**
   * Set the colorMode and the maximum values for (r, g, b)
   * or (h, s, b).
   * <P>
   * Note that this doesn't set the maximum for the alpha value,
   * which might be confusing if for instance you switched to
   * <PRE>colorMode(HSB, 360, 100, 100);</PRE>
   * because the alpha values were still between 0 and 255.
   */
  public void colorMode(int mode,
                        float maxX, float maxY, float maxZ) {
    colorMode(mode, maxX, maxY, maxZ, colorModeA);
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


  protected void colorCalc(float gray) {
    colorCalc(gray, colorModeA);
  }


  protected void colorCalc(float gray, float alpha) {
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


  protected void colorCalc(float x, float y, float z) {
    colorCalc(x, y, z, colorModeA);
  }


  protected void colorCalc(float x, float y, float z, float a) {
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
   * Unpacks AARRGGBB color for direct use with colorCalc.
   * <P>
   * Handled here with its own function since this is indepenent
   * of the color mode.
   * <P>
   * Strangely the old version of this code ignored the alpha
   * value. not sure if that was a bug or what.
   * <P>
   * Note, no need for a bounds check since it's a 32 bit number.
   */
  protected void colorCalcARGB(int argb) {
    calcColor = argb;
    calcAi = (argb >> 24) & 0xff;
    calcRi = (argb >> 16) & 0xff;
    calcGi = (argb >> 8) & 0xff;
    calcBi = argb & 0xff;
    calcA = (float)calcAi / 255.0f;
    calcR = (float)calcRi / 255.0f;
    calcG = (float)calcGi / 255.0f;
    calcB = (float)calcBi / 255.0f;
    calcAlpha = (calcAi != 255);
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


  /**
   * Set the tint to either a grayscale or ARGB value. See notes
   * attached to the fill() function.
   */
  public void stroke(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      stroke((float) rgb);

    } else {
      colorCalcARGB(rgb);
      strokeFromCalc();
    }
  }


  public void stroke(float gray) {
    colorCalc(gray);
    strokeFromCalc();
  }


  public void stroke(float gray, float alpha) {
    colorCalc(gray, alpha);
    strokeFromCalc();
  }


  public void stroke(float x, float y, float z) {
    colorCalc(x, y, z);
    strokeFromCalc();
  }


  public void stroke(float x, float y, float z, float a) {
    colorCalc(x, y, z, a);
    strokeFromCalc();
  }


  protected void strokeFromCalc() {
    stroke = true;
    //strokeChanged = true;
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


  //////////////////////////////////////////////////////////////


  public void noTint() {
    tint = false;
  }


  /**
   * Set the tint to either a grayscale or ARGB value. See notes
   * attached to the fill() function.
   */
  public void tint(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {
      tint((float) rgb);

    } else {
      colorCalcARGB(rgb);
      tintFromCalc();
    }
  }

  public void tint(float gray) {
    colorCalc(gray);
    tintFromCalc();
  }


  public void tint(float gray, float alpha) {
    colorCalc(gray, alpha);
    tintFromCalc();
  }


  public void tint(float x, float y, float z) {
    colorCalc(x, y, z);
    tintFromCalc();
  }


  public void tint(float x, float y, float z, float a) {
    colorCalc(x, y, z, a);
    tintFromCalc();
  }


  protected void tintFromCalc() {
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


  //////////////////////////////////////////////////////////////


  public void noFill() {
    fill = false;
  }


  /**
   * Set the fill to either a grayscale value or an ARGB int.
   * <P>
   * The problem with this code is that it has to detect between
   * these two situations automatically. This is done by checking
   * to see if the high bits (the alpha for 0xAA000000) is set,
   * and if not, whether the color value that follows is less than
   * colorModeX (the first param passed to colorMode).
   * <P>
   * This auto-detect would break in the following situation:
   * <PRE>size(256, 256);
   * for (int i = 0; i < 256; i++) {
   *   color c = color(0, 0, 0, i);
   *   stroke(c);
   *   line(i, 0, i, 256);
   * }</PRE>
   * ...on the first time through the loop, where (i == 0),
   * since the color itself is zero (black) then it would appear
   * indistinguishable from someone having written fill(0).
   */
  public void fill(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      fill((float) rgb);

    } else {
      colorCalcARGB(rgb);
      fillFromCalc();
    }
  }

  public void fill(float gray) {
    colorCalc(gray);
    fillFromCalc();
  }


  public void fill(float gray, float alpha) {
    colorCalc(gray, alpha);
    fillFromCalc();
  }


  public void fill(float x, float y, float z) {
    colorCalc(x, y, z);
    fillFromCalc();
  }


  public void fill(float x, float y, float z, float a) {
    colorCalc(x, y, z, a);
    fillFromCalc();
  }


  protected void fillFromCalc() {
    fill = true;
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


  //////////////////////////////////////////////////////////////


  public void ambient(int rgb) {
    depthError("ambient");
  }

  public void ambient(float gray) {
    depthError("ambient");
  }

  public void ambient(float x, float y, float z) {
    depthError("ambient");
  }


  //////////////////////////////////////////////////////////////


  public void specular(int rgb) {
    depthError("specular");
  }

  public void specular(float gray) {
    depthError("specular");
  }

  public void specular(float gray, float alpha) {
    depthError("specular");
  }

  public void specular(float x, float y, float z) {
    depthError("specular");
  }

  public void specular(float x, float y, float z, float a) {
    depthError("specular");
  }

  public void shininess(float shine) {
    depthError("shininess");
  }


  //////////////////////////////////////////////////////////////


  public void emissive(int rgb) {
    depthError("emissive");
  }

  public void emissive(float gray) {
    depthError("emissive");
  }

  public void emissive(float x, float y, float z ) {
    depthError("emissive");
  }



  //////////////////////////////////////////////////////////////

  // LIGHTS


  public void lights() {
    depthError("lights");
  }

  public void ambientLight(float red, float green, float blue) {
    depthError("ambientLight");
  }

  public void ambientLight(float red, float green, float blue,
                           float x, float y, float z) {
    depthError("ambientLight");
  }

  public void directionalLight(float red, float green, float blue,
                               float nx, float ny, float nz) {
    depthError("directionalLight");
  }

  public void pointLight(float red, float green, float blue,
                         float x, float y, float z) {
    depthError("pointLight");
  }

  public void spotLight(float red, float green, float blue,
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float angle, float concentration) {
    depthError("spotLight");
  }

  public void lightFalloff(float constant, float linear, float quadratic) {
    depthError("lightFalloff");
  }

  public void lightSpecular(float x, float y, float z) {
    depthError("lightSpecular");
  }



  //////////////////////////////////////////////////////////////


  /**
   * Set the background to a gray or ARGB color.
   * <P>
   * Note that background() should be called before any
   * transformations occur, because some implementations may
   * require the current transformation matrix to be identity
   * before drawing.
   */
  public void background(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {
      background((float) rgb);

    } else {
      colorCalcARGB(rgb);
      backgroundFromCalc();
    }
    clear();
  }


  /**
   * Set the background to a grayscale value, based on the
   * current colorMode.
   */
  public void background(float gray) {
    colorCalc(gray);
    backgroundFromCalc();
    clear();
  }


  /**
   * Set the background to an r, g, b or h, s, b value,
   * based on the current colorMode.
   */
  public void background(float x, float y, float z) {
    colorCalc(x, y, z);
    backgroundFromCalc();
    clear();
  }


  protected void backgroundFromCalc() {
    backgroundR = calcR;
    backgroundG = calcG;
    backgroundB = calcB;
    backgroundRi = calcRi;
    backgroundGi = calcGi;
    backgroundBi = calcBi;
    backgroundColor = calcColor;
  }


  /**
   * Takes an RGB or ARGB image and sets it as the background.
   * <P>
   * Note that even if the image is set as RGB, the high 8 bits of
   * each pixel should be set opaque (0xFF000000), because the image data
   * will be copied directly to the screen, and non-opaque background
   * images may have strange behavior. Using image.filter(OPAQUE)
   * will handle this easily.
   * <P>
   * When using 3D, this will also clear out the zbuffer and
   * stencil buffer if they exist.
   */
  public void background(PImage image) {
    if ((image.width != width) || (image.height != height)) {
      throw new RuntimeException("background image must be " +
                                 "the same size as your application");
    }
    if ((image.format != RGB) && (image.format != ARGB)) {
      throw new RuntimeException("background images should be RGB or ARGB");
    }

    // zero this out since it's an image
    backgroundColor = 0;

    // blit image to the screen
    System.arraycopy(image.pixels, 0, pixels, 0, pixels.length);
  }


  /**
   * Clears pixel buffer.
   * <P>
   * Subclasses (PGraphics3) will also clear the
   * stencil and zbuffer if they exist.
   */
  protected void clear() {
    for (int i = 0; i < pixelCount; i++) {
      pixels[i] = backgroundColor;
    }
  }



  //////////////////////////////////////////////////////////////

  // MESSAGES / ERRORS / LOGGING


  protected void depthError(String method) {
    throw new RuntimeException(method + "() can only be used " +
                               "with P3D or OPENGL.");
  }

  protected void depthErrorXYZ(String method) {
    throw new RuntimeException(method + "(x, y, z) can only be used with " +
                               "OPENGL or P3D, use " +
                               method + "(x, y) instead.");
  }


  //////////////////////////////////////////////////////////////

  // COLOR MANIPULATION

  // these functions are really slow, but easy to use
  // if folks are advanced enough to want something faster,
  // they can write it themselves (not difficult)


  public final int color(int gray) {  // ignore
    if (colorRgb255) {
      // bounds checking to make sure the numbers aren't to high or low
      if (gray > 255) gray = 255; else if (gray < 0) gray = 0;
      return 0xff000000 | (gray << 16) | (gray << 8) | gray;
    }
    colorCalc(gray);
    return calcColor;
  }

  public final int color(float gray) {  // ignore
    colorCalc(gray);
    return calcColor;
  }


  public final int color(int gray, int alpha) {  // ignore
    if (colorRgb255) {
      // bounds checking to make sure the numbers aren't to high or low
      if (gray > 255) gray = 255; else if (gray < 0) gray = 0;
      if (alpha > 255) alpha = 255; else if (alpha < 0) alpha = 0;

      return ((alpha & 0xff) << 24) | (gray << 16) | (gray << 8) | gray;
    }
    colorCalc(gray, alpha);
    return calcColor;
  }

  public final int color(float gray, float alpha) {  // ignore
    colorCalc(gray, alpha);
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
    colorCalc(x, y, z);
    return calcColor;
  }

  public final int color(float x, float y, float z) {  // ignore
    colorCalc(x, y, z);
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
    colorCalc(x, y, z, a);
    return calcColor;
  }

  public final int color(float x, float y, float z, float a) {  // ignore
    colorCalc(x, y, z, a);
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



  //////////////////////////////////////////////////////////////

  // PATH

  class Path {

    public void moveTo(float x, float y) {  // ignore
    }

    public void lineTo(float x, float y) {  // ignore
    }

    public void curveTo(float x1, float y1,  // ignore
                        float x2, float y2,
                        float x3, float y3) {
    }

    public void closePath() {  // ignore
    }
  }


  //////////////////////////////////////////////////////////////


  /**
   * Use with caution on PGraphics. This should not be used with
   * the base PGraphics that's tied to a PApplet, but it can be used
   * with user-created PGraphics objects that are drawn to the screen.
   */
  public void mask(int alpha[]) {  // ignore
    super.mask(alpha);
  }


  /**
   * Use with caution on PGraphics. This should not be used with
   * the base PGraphics that's tied to a PApplet, but it can be used
   * with user-created PGraphics objects that are drawn to the screen.
   */
  public void mask(PImage alpha) {  // ignore
    super.mask(alpha);
  }
}
