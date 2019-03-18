/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-19 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
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

// Used for color conversion functions
import java.awt.Color;

// Used for the 'image' object that's been here forever
import java.awt.Font;
import java.awt.Image;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import processing.opengl.PGL;
import processing.opengl.PShader;

  /**
   * ( begin auto-generated from PGraphics.xml )
   *
   * Main graphics and rendering context, as well as the base API
   * implementation for processing "core". Use this class if you need to draw
   * into an off-screen graphics buffer. A PGraphics object can be
   * constructed with the <b>createGraphics()</b> function. The
   * <b>beginDraw()</b> and <b>endDraw()</b> methods (see above example) are
   * necessary to set up the buffer and to finalize it. The fields and
   * methods for this class are extensive. For a complete list, visit the <a
   * href="http://processing.googlecode.com/svn/trunk/processing/build/javadoc/core/">developer's reference.</a>
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   * Main graphics and rendering context, as well as the base API implementation.
   *
   * <h2>Subclassing and initializing PGraphics objects</h2>
   * Starting in release 0149, subclasses of PGraphics are handled differently.
   * The constructor for subclasses takes no parameters, instead a series of
   * functions are called by the hosting PApplet to specify its attributes.
   * <ul>
   * <li>setParent(PApplet) - is called to specify the parent PApplet.
   * <li>setPrimary(boolean) - called with true if this PGraphics will be the
   * primary drawing surface used by the sketch, or false if not.
   * <li>setPath(String) - called when the renderer needs a filename or output
   * path, such as with the PDF or DXF renderers.
   * <li>setSize(int, int) - this is called last, at which point it's safe for
   * the renderer to complete its initialization routine.
   * </ul>
   * The functions were broken out because of the growing number of parameters
   * such as these that might be used by a renderer, yet with the exception of
   * setSize(), it's not clear which will be necessary. So while the size could
   * be passed in to the constructor instead of a setSize() function, a function
   * would still be needed that would notify the renderer that it was time to
   * finish its initialization. Thus, setSize() simply does both.
   *
   * <h2>Know your rights: public vs. private methods</h2>
   * Methods that are protected are often subclassed by other renderers, however
   * they are not set 'public' because they shouldn't be part of the user-facing
   * public API accessible from PApplet. That is, we don't want sketches calling
   * textModeCheck() or vertexTexture() directly.
   *
   * <h2>Handling warnings and exceptions</h2>
   * Methods that are unavailable generally show a warning, unless their lack of
   * availability will soon cause another exception. For instance, if a method
   * like getMatrix() returns null because it is unavailable, an exception will
   * be thrown stating that the method is unavailable, rather than waiting for
   * the NullPointerException that will occur when the sketch tries to use that
   * method. As of release 0149, warnings will only be shown once, and exceptions
   * have been changed to warnings where possible.
   *
   * <h2>Using xxxxImpl() for subclassing smoothness</h2>
   * The xxxImpl() methods are generally renderer-specific handling for some
   * subset if tasks for a particular function (vague enough for you?) For
   * instance, imageImpl() handles drawing an image whose x/y/w/h and u/v coords
   * have been specified, and screen placement (independent of imageMode) has
   * been determined. There's no point in all renderers implementing the
   * <tt>if (imageMode == BLAH)</tt> placement/sizing logic, so that's handled
   * by PGraphics, which then calls imageImpl() once all that is figured out.
   *
   * <h2>His brother PImage</h2>
   * PGraphics subclasses PImage so that it can be drawn and manipulated in a
   * similar fashion. As such, many methods are inherited from PGraphics,
   * though many are unavailable: for instance, resize() is not likely to be
   * implemented; the same goes for mask(), depending on the situation.
   *
   * <h2>What's in PGraphics, what ain't</h2>
   * For the benefit of subclasses, as much as possible has been placed inside
   * PGraphics. For instance, bezier interpolation code and implementations of
   * the strokeCap() method (that simply sets the strokeCap variable) are
   * handled here. Features that will vary widely between renderers are located
   * inside the subclasses themselves. For instance, all matrix handling code
   * is per-renderer: Java 2D uses its own AffineTransform, P2D uses a PMatrix2D,
   * and PGraphics3D needs to keep continually update forward and reverse
   * transformations. A proper (future) OpenGL implementation will have all its
   * matrix madness handled by the card. Lighting also falls under this
   * category, however the base material property settings (emissive, specular,
   * et al.) are handled in PGraphics because they use the standard colorMode()
   * logic. Subclasses should override methods like emissiveFromCalc(), which
   * is a point where a valid color has been defined internally, and can be
   * applied in some manner based on the calcXxxx values.
   *
   * <h2>What's in the PGraphics documentation, what ain't</h2>
   * Some things are noted here, some things are not. For public API, always
   * refer to the <a href="http://processing.org/reference">reference</A>
   * on Processing.org for proper explanations. <b>No attempt has been made to
   * keep the javadoc up to date or complete.</b> It's an enormous task for
   * which we simply do not have the time. That is, it's not something that
   * to be done once&mdash;it's a matter of keeping the multiple references
   * synchronized (to say nothing of the translation issues), while targeting
   * them for their separate audiences. Ouch.
   *
   * We're working right now on synchronizing the two references, so the website reference
   * is generated from the javadoc comments. Yay.
   *
   * @webref rendering
   * @instanceName graphics any object of the type PGraphics
   * @usage Web &amp; Application
   * @see PApplet#createGraphics(int, int, String)
   */
public class PGraphics extends PImage implements PConstants {

//  /// Canvas object that covers rendering this graphics on screen.
//  public Canvas canvas;

  // ........................................................

  // width and height are already inherited from PImage


//  /// width minus one (useful for many calculations)
//  protected int width1;
//
//  /// height minus one (useful for many calculations)
//  protected int height1;

  /// width * height (useful for many calculations)
  public int pixelCount;

//  /// true if smoothing is enabled (read-only)
//  public boolean smooth;

  /// the anti-aliasing level for renderers that support it
  public int smooth;


  // ........................................................

  /// true if defaults() has been called a first time
  protected boolean settingsInited;

  /// true if settings should be re-applied on next beginDraw()
  protected boolean reapplySettings;

  /// set to a PGraphics object being used inside a beginRaw/endRaw() block
  protected PGraphics raw;

  // ........................................................

  /** path to the file being saved for this renderer (if any) */
  protected String path;

  /**
   * True if this is the main graphics context for a sketch.
   * False for offscreen buffers retrieved via createGraphics().
   */
  protected boolean primaryGraphics;

//  // TODO nervous about leaving this here since it seems likely to create
//  // back-references where we don't want them
//  protected PSurface surface;

  // ........................................................

  /**
   * Array of hint[] items. These are hacks to get around various
   * temporary workarounds inside the environment.
   * <p/>
   * Note that this array cannot be static, as a hint() may result in a
   * runtime change specific to a renderer. For instance, calling
   * hint(DISABLE_DEPTH_TEST) has to call glDisable() right away on an
   * instance of PGraphicsOpenGL.
   * <p/>
   * The hints[] array is allocated early on because it might
   * be used inside beginDraw(), allocate(), etc.
   */
  protected boolean[] hints = new boolean[HINT_COUNT];

  // ........................................................

  /**
   * Storage for renderer-specific image data. In 1.x, renderers wrote cache
   * data into the image object. In 2.x, the renderer has a weak-referenced
   * map that points at any of the images it has worked on already. When the
   * images go out of scope, they will be properly garbage collected.
   */
  protected WeakHashMap<PImage, Object> cacheMap =
    new WeakHashMap<>();


  ////////////////////////////////////////////////////////////

  // Vertex fields, moved from PConstants (after 2.0a8) because they're too
  // general to show up in all sketches as defined variables.

  // X, Y and Z are still stored in PConstants because of their general
  // usefulness, and that X we'll always want to be 0, etc.

  static public final int R = 3;  // actual rgb, after lighting
  static public final int G = 4;  // fill stored here, transform in place
  static public final int B = 5;  // TODO don't do that anymore (?)
  static public final int A = 6;

  static public final int U = 7; // texture
  static public final int V = 8;

  static public final int NX = 9; // normal
  static public final int NY = 10;
  static public final int NZ = 11;

  static public final int EDGE = 12;

  // stroke

  /** stroke argb values */
  static public final int SR = 13;
  static public final int SG = 14;
  static public final int SB = 15;
  static public final int SA = 16;

  /** stroke weight */
  static public final int SW = 17;

  // transformations (2D and 3D)

  static public final int TX = 18; // transformed xyzw
  static public final int TY = 19;
  static public final int TZ = 20;

  static public final int VX = 21; // view space coords
  static public final int VY = 22;
  static public final int VZ = 23;
  static public final int VW = 24;


  // material properties

  // Ambient color (usually to be kept the same as diffuse)
  // fill(_) sets both ambient and diffuse.
  static public final int AR = 25;
  static public final int AG = 26;
  static public final int AB = 27;

  // Diffuse is shared with fill.
  static public final int DR = 3;  // TODO needs to not be shared, this is a material property
  static public final int DG = 4;
  static public final int DB = 5;
  static public final int DA = 6;

  // specular (by default kept white)
  static public final int SPR = 28;
  static public final int SPG = 29;
  static public final int SPB = 30;

  static public final int SHINE = 31;

  // emissive (by default kept black)
  static public final int ER = 32;
  static public final int EG = 33;
  static public final int EB = 34;

  // has this vertex been lit yet
  static public final int BEEN_LIT = 35;

  // has this vertex been assigned a normal yet
  static public final int HAS_NORMAL = 36;

  static public final int VERTEX_FIELD_COUNT = 37;


  ////////////////////////////////////////////////////////////

  // STYLE PROPERTIES

  // Also inherits imageMode() and smooth() (among others) from PImage.

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
  boolean colorModeScale; // = true;

  /**
   * True if colorMode(RGB, 255). Defaults to true so that color()
   * used as part of a field declaration will properly assign values.
   */
  boolean colorModeDefault = true;

  // ........................................................

  // Tint color for images

  /**
   * True if tint() is enabled (read-only).
   *
   * Using tint/tintColor seems a better option for naming than
   * tintEnabled/tint because the latter seems ugly, even though
   * g.tint as the actual color seems a little more intuitive,
   * it's just that g.tintEnabled is even more unintuitive.
   * Same goes for fill and stroke, et al.
   */
  public boolean tint;

  /** tint that was last set (read-only) */
  public int tintColor;

  protected boolean tintAlpha;
  protected float tintR, tintG, tintB, tintA;
  protected int tintRi, tintGi, tintBi, tintAi;

  // ........................................................

  // Fill color

  /** true if fill() is enabled, (read-only) */
  public boolean fill;

  /** fill that was last set (read-only) */
  public int fillColor = 0xffFFFFFF;

  protected boolean fillAlpha;
  protected float fillR, fillG, fillB, fillA;
  protected int fillRi, fillGi, fillBi, fillAi;

  // ........................................................

  // Stroke color

  /** true if stroke() is enabled, (read-only) */
  public boolean stroke;

  /** stroke that was last set (read-only) */
  public int strokeColor = 0xff000000;

  protected boolean strokeAlpha;
  protected float strokeR, strokeG, strokeB, strokeA;
  protected int strokeRi, strokeGi, strokeBi, strokeAi;

  // ........................................................

  // Additional stroke properties

  static protected final float DEFAULT_STROKE_WEIGHT = 1;
  static protected final int DEFAULT_STROKE_JOIN = MITER;
  static protected final int DEFAULT_STROKE_CAP = ROUND;

  /**
   * Last value set by strokeWeight() (read-only). This has a default
   * setting, rather than fighting with renderers about whether that
   * renderer supports thick lines.
   */
  public float strokeWeight = DEFAULT_STROKE_WEIGHT;

  /**
   * Set by strokeJoin() (read-only). This has a default setting
   * so that strokeJoin() need not be called by defaults,
   * because subclasses may not implement it (i.e. PGraphicsGL)
   */
  public int strokeJoin = DEFAULT_STROKE_JOIN;

  /**
   * Set by strokeCap() (read-only). This has a default setting
   * so that strokeCap() need not be called by defaults,
   * because subclasses may not implement it (i.e. PGraphicsGL)
   */
  public int strokeCap = DEFAULT_STROKE_CAP;

  // ........................................................

  // Shape placement properties

  // imageMode() is inherited from PImage

  /** The current rect mode (read-only) */
  public int rectMode;

  /** The current ellipse mode (read-only) */
  public int ellipseMode;

  /** The current shape alignment mode (read-only) */
  public int shapeMode;

  /** The current image alignment (read-only) */
  public int imageMode = CORNER;

  // ........................................................

  // Text and font properties

  /** The current text font (read-only) */
  public PFont textFont;

  /** The current text align (read-only) */
  public int textAlign = LEFT;

  /** The current vertical text alignment (read-only) */
  public int textAlignY = BASELINE;

  /** The current text mode (read-only) */
  public int textMode = MODEL;

  /** The current text size (read-only) */
  public float textSize;

  /** The current text leading (read-only) */
  public float textLeading;

  static final protected String ERROR_TEXTFONT_NULL_PFONT =
    "A null PFont was passed to textFont()";

  // ........................................................

  // Material properties

//  PMaterial material;
//  PMaterial[] materialStack;
//  int materialStackPointer;

  public int ambientColor;
  public float ambientR, ambientG, ambientB;
  public boolean setAmbient;

  public int specularColor;
  public float specularR, specularG, specularB;

  public int emissiveColor;
  public float emissiveR, emissiveG, emissiveB;

  public float shininess;


  // Style stack

  static final int STYLE_STACK_DEPTH = 64;
  PStyle[] styleStack = new PStyle[STYLE_STACK_DEPTH];
  int styleStackDepth;


  ////////////////////////////////////////////////////////////


  /** Last background color that was set, zero if an image */
  public int backgroundColor = 0xffCCCCCC;

  protected boolean backgroundAlpha;
  protected float backgroundR, backgroundG, backgroundB, backgroundA;
  protected int backgroundRi, backgroundGi, backgroundBi, backgroundAi;

  static final protected String ERROR_BACKGROUND_IMAGE_SIZE =
    "background image must be the same size as your application";
  static final protected String ERROR_BACKGROUND_IMAGE_FORMAT =
    "background images should be RGB or ARGB";


  /** The current blending mode. */
  protected int blendMode;


  // ........................................................

  /**
   * Current model-view matrix transformation of the form m[row][column],
   * which is a "column vector" (as opposed to "row vector") matrix.
   */
//  PMatrix matrix;
//  public float m00, m01, m02, m03;
//  public float m10, m11, m12, m13;
//  public float m20, m21, m22, m23;
//  public float m30, m31, m32, m33;

//  static final int MATRIX_STACK_DEPTH = 32;
//  float[][] matrixStack = new float[MATRIX_STACK_DEPTH][16];
//  float[][] matrixInvStack = new float[MATRIX_STACK_DEPTH][16];
//  int matrixStackDepth;

  static final protected int MATRIX_STACK_DEPTH = 32;

  static final protected String ERROR_PUSHMATRIX_OVERFLOW =
    "Too many calls to pushMatrix().";
  static final protected String ERROR_PUSHMATRIX_UNDERFLOW =
    "Too many calls to popMatrix(), and not enough to pushMatrix().";


  // ........................................................

  /**
   * Java AWT Image object associated with this renderer. For the 1.0 version
   * of P2D and P3D, this was associated with their MemoryImageSource.
   * For PGraphicsJava2D, it will be the offscreen drawing buffer.
   */
  public Image image;

  /** Surface object that we're talking to */
  protected PSurface surface;

  // ........................................................

  // internal color for setting/calculating
  protected float calcR, calcG, calcB, calcA;
  protected int calcRi, calcGi, calcBi, calcAi;
  protected int calcColor;
  protected boolean calcAlpha;

  /** The last RGB value converted to HSB */
  int cacheHsbKey;
  /** Result of the last conversion to HSB */
  float[] cacheHsbValue = new float[3];

  // ........................................................

  /**
   * Type of shape passed to beginShape(),
   * zero if no shape is currently being drawn.
   */
  protected int shape;

  // vertices
  public static final int DEFAULT_VERTICES = 512;
  protected float vertices[][] =
    new float[DEFAULT_VERTICES][VERTEX_FIELD_COUNT];
  protected int vertexCount; // total number of vertices

  // ........................................................

  protected boolean bezierInited = false;
  public int bezierDetail = 20;

  // used by both curve and bezier, so just init here
  protected PMatrix3D bezierBasisMatrix =
    new PMatrix3D(-1,  3, -3,  1,
                   3, -6,  3,  0,
                  -3,  3,  0,  0,
                   1,  0,  0,  0);

  //protected PMatrix3D bezierForwardMatrix;
  protected PMatrix3D bezierDrawMatrix;

  // ........................................................

  protected boolean curveInited = false;
  public int curveDetail = 20;
  public float curveTightness = 0;
  // catmull-rom basis matrix, perhaps with optional s parameter
  protected PMatrix3D curveBasisMatrix;
  protected PMatrix3D curveDrawMatrix;

  protected PMatrix3D bezierBasisInverse;
  protected PMatrix3D curveToBezierMatrix;

  // ........................................................

  // spline vertices

  protected float curveVertices[][];
  protected int curveVertexCount;

  // ........................................................

  // precalculate sin/cos lookup tables [toxi]
  // circle resolution is determined from the actual used radii
  // passed to ellipse() method. this will automatically take any
  // scale transformations into account too

  // [toxi 031031]
  // changed table's precision to 0.5 degree steps
  // introduced new vars for more flexible code
  static final protected float sinLUT[];
  static final protected float cosLUT[];
  static final protected float SINCOS_PRECISION = 0.5f;
  static final protected int SINCOS_LENGTH = (int) (360f / SINCOS_PRECISION);
  static {
    sinLUT = new float[SINCOS_LENGTH];
    cosLUT = new float[SINCOS_LENGTH];
    for (int i = 0; i < SINCOS_LENGTH; i++) {
      sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
      cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
    }
  }

  // ........................................................

  /** The current font if a Java version of it is installed */
  //protected Font textFontNative;

  /** Metrics for the current native Java font */
  //protected FontMetrics textFontNativeMetrics;

//  /** Last text position, because text often mixed on lines together */
//  protected float textX, textY, textZ;

  /**
   * Internal buffer used by the text() functions
   * because the String object is slow
   */
  protected char[] textBuffer = new char[8 * 1024];
  protected char[] textWidthBuffer = new char[8 * 1024];

  protected int textBreakCount;
  protected int[] textBreakStart;
  protected int[] textBreakStop;

  // ........................................................

  public boolean edge = true;

  // ........................................................

  /// normal calculated per triangle
  static protected final int NORMAL_MODE_AUTO = 0;
  /// one normal manually specified per shape
  static protected final int NORMAL_MODE_SHAPE = 1;
  /// normals specified for each shape vertex
  static protected final int NORMAL_MODE_VERTEX = 2;

  /// Current mode for normals, one of AUTO, SHAPE, or VERTEX
  protected int normalMode;

  /// Keep track of how many calls to normal, to determine the mode.
  //protected int normalCount;

  protected boolean autoNormal;

  /** Current normal vector. */
  public float normalX, normalY, normalZ;

  // ........................................................

  /**
   * Sets whether texture coordinates passed to
   * vertex() calls will be based on coordinates that are
   * based on the IMAGE or NORMALIZED.
   */
  public int textureMode = IMAGE;

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

  // [toxi031031] new & faster sphere code w/ support flexible resolutions
  // will be set by sphereDetail() or 1st call to sphere()
  protected float sphereX[], sphereY[], sphereZ[];

  /// Number of U steps (aka "theta") around longitudinally spanning 2*pi
  public int sphereDetailU = 0;
  /// Number of V steps (aka "phi") along latitudinally top-to-bottom spanning pi
  public int sphereDetailV = 0;


  //////////////////////////////////////////////////////////////

  // INTERNAL

  // Most renderers will only override the default implementation of one or
  // two of the setXxxx() methods, so they're broken out here since the
  // default implementations for each are simple, obvious, and common.
  // They're also separate to avoid a monolithic and fragile constructor.


  public PGraphics() {
    // In 3.1.2, giving up on the async image saving as the default
    hints[DISABLE_ASYNC_SAVEFRAME] = true;
  }


  public void setParent(PApplet parent) {  // ignore
    this.parent = parent;

    // Some renderers (OpenGL) need to know what smoothing level will be used
    // before the rendering surface is even created.
    smooth = parent.sketchSmooth();
    pixelDensity = parent.sketchPixelDensity();
  }


  /**
   * Set (or unset) this as the main drawing surface. Meaning that it can
   * safely be set to opaque (and given a default gray background), or anything
   * else that goes along with that.
   */
  public void setPrimary(boolean primary) {  // ignore
    this.primaryGraphics = primary;

    // base images must be opaque (for performance and general
    // headache reasons.. argh, a semi-transparent opengl surface?)
    // use createGraphics() if you want a transparent surface.
    if (primaryGraphics) {
      format = RGB;
    }
  }


  public void setPath(String path) {  // ignore
    this.path = path;
  }


//  public void setQuality(int samples) {  // ignore
//    this.quality = samples;
//  }


  /**
   * The final step in setting up a renderer, set its size of this renderer.
   * This was formerly handled by the constructor, but instead it's been broken
   * out so that setParent/setPrimary/setPath can be handled differently.
   *
   * Important: this is ignored by the Methods task because otherwise it will
   * override setSize() in PApplet/Applet/Component, which will 1) not call
   * super.setSize(), and 2) will cause the renderer to be resized from the
   * event thread (EDT), causing a nasty crash as it collides with the
   * animation thread.
   */
  public void setSize(int w, int h) {  // ignore
    width = w;
    height = h;

    /** {@link PImage.pixelFactor} set in {@link PImage#PImage()} */
    pixelWidth = width * pixelDensity;
    pixelHeight = height * pixelDensity;

//    if (surface != null) {
//      allocate();
//    }
//    reapplySettings();
    reapplySettings = true;
  }


//  public void setSmooth(int level) {
//    this.smooth = level;
//  }


//  /**
//   * Allocate memory or an image buffer for this renderer.
//   */
//  protected void allocate() { }


  /**
   * Handle any takedown for this graphics context.
   * <p>
   * This is called when a sketch is shut down and this renderer was
   * specified using the size() command, or inside endRecord() and
   * endRaw(), in order to shut things off.
   */
  public void dispose() {  // ignore
    if (primaryGraphics && asyncImageSaver != null) {
      asyncImageSaver.dispose();
      asyncImageSaver = null;
    }
  }


  public PSurface createSurface() {  // ignore
    return surface = new PSurfaceNone(this);
  }



  //////////////////////////////////////////////////////////////

  // IMAGE METADATA FOR THIS RENDERER

  /**
   * Store data of some kind for the renderer that requires extra metadata of
   * some kind. Usually this is a renderer-specific representation of the
   * image data, for instance a BufferedImage with tint() settings applied for
   * PGraphicsJava2D, or resized image data and OpenGL texture indices for
   * PGraphicsOpenGL.
   * @param image The image to be stored
   * @param storage The metadata required by the renderer
   */
  public void setCache(PImage image, Object storage) {  // ignore
    cacheMap.put(image, storage);
  }


  /**
   * Get cache storage data for the specified renderer. Because each renderer
   * will cache data in different formats, it's necessary to store cache data
   * keyed by the renderer object. Otherwise, attempting to draw the same
   * image to both a PGraphicsJava2D and a PGraphicsOpenGL will cause errors.
   * @return metadata stored for the specified renderer
   */
  public Object getCache(PImage image) {  // ignore
    return cacheMap.get(image);
  }


  /**
   * Remove information associated with this renderer from the cache, if any.
   * @param image The image whose cache data should be removed
   */
  public void removeCache(PImage image) {  // ignore
    cacheMap.remove(image);
  }



  //////////////////////////////////////////////////////////////

  // FRAME


//  /**
//   * Some renderers have requirements re: when they are ready to draw.
//   */
//  public boolean canDraw() {  // ignore
//    return true;
//  }


  // removing because renderers will have their own animation threads and
  // can handle this however they wish
//  /**
//   * Try to draw, or put a draw request on the queue.
//   */
//  public void requestDraw() {  // ignore
//    parent.handleDraw();
//  }


  /**
   * ( begin auto-generated from PGraphics_beginDraw.xml )
   *
   * Sets the default properties for a PGraphics object. It should be called
   * before anything is drawn into the object.
   *
   * ( end auto-generated )
   * <h3>Advanced</h3>
   * When creating your own PGraphics, you should call this before
   * drawing anything.
   *
   * @webref pgraphics:method
   * @brief Sets the default properties for a PGraphics object
   */
  public void beginDraw() {  // ignore
  }


  /**
   * ( begin auto-generated from PGraphics_endDraw.xml )
   *
   * Finalizes the rendering of a PGraphics object so that it can be shown on screen.
   *
   * ( end auto-generated )
   * <h3>Advanced</h3>
   * <p/>
   * When creating your own PGraphics, you should call this when
   * you're finished drawing.
   *
   * @webref pgraphics:method
   * @brief Finalizes the rendering of a PGraphics object
   */
  public void endDraw() {  // ignore
  }


  public PGL beginPGL() {
    showMethodWarning("beginGL");
    return null;
  }


  public void endPGL() {
    showMethodWarning("endGL");
  }


  public void flush() {
    // no-op, mostly for P3D to write sorted stuff
  }


  protected void checkSettings() {
    if (!settingsInited) defaultSettings();
    if (reapplySettings) reapplySettings();
  }


  /**
   * Set engine's default values. This has to be called by PApplet,
   * somewhere inside setup() or draw() because it talks to the
   * graphics buffer, meaning that for subclasses like OpenGL, there
   * needs to be a valid graphics context to mess with otherwise
   * you'll get some good crashing action.
   *
   * This is currently called by checkSettings(), during beginDraw().
   */
  protected void defaultSettings() {  // ignore
//    System.out.println("PGraphics.defaultSettings() " + width + " " + height);

//    //smooth();  // 2.0a5
//    if (quality > 0) {  // 2.0a5
//      smooth();
//    } else {
//      noSmooth();
//    }

    colorMode(RGB, 255);
    fill(255);
    stroke(0);

    // as of 0178, no longer relying on local versions of the variables
    // being set, because subclasses may need to take extra action.
    strokeWeight(DEFAULT_STROKE_WEIGHT);
    strokeJoin(DEFAULT_STROKE_JOIN);
    strokeCap(DEFAULT_STROKE_CAP);

    // init shape stuff
    shape = 0;

    rectMode(CORNER);
    ellipseMode(DIAMETER);

    autoNormal = true;

    // no current font
    textFont = null;
    textSize = 12;
    textLeading = 14;
    textAlign = LEFT;
    textMode = MODEL;

    // if this fella is associated with an applet, then clear its background.
    // if it's been created by someone else through createGraphics,
    // they have to call background() themselves, otherwise everything gets
    // a gray background (when just a transparent surface or an empty pdf
    // is what's desired).
    // this background() call is for the Java 2D and OpenGL renderers.
    if (primaryGraphics) {
      //System.out.println("main drawing surface bg " + getClass().getName());
      background(backgroundColor);
    }

    blendMode(BLEND);

    settingsInited = true;
    // defaultSettings() overlaps reapplySettings(), don't do both
    reapplySettings = false;
  }


  /**
   * Re-apply current settings. Some methods, such as textFont(), require that
   * their methods be called (rather than simply setting the textFont variable)
   * because they affect the graphics context, or they require parameters from
   * the context (e.g. getting native fonts for text).
   *
   * This will only be called from an allocate(), which is only called from
   * size(), which is safely called from inside beginDraw(). And it cannot be
   * called before defaultSettings(), so we should be safe.
   */
  protected void reapplySettings() {
    // This might be called by allocate... So if beginDraw() has never run,
    // we don't want to reapply here, we actually just need to let
    // defaultSettings() get called a little from inside beginDraw().
    if (!settingsInited) return;  // if this is the initial setup, no need to reapply

    colorMode(colorMode, colorModeX, colorModeY, colorModeZ);
    if (fill) {
//      PApplet.println("  fill " + PApplet.hex(fillColor));
      fill(fillColor);
    } else {
      noFill();
    }
    if (stroke) {
      stroke(strokeColor);

      // The if() statements should be handled inside the functions,
      // otherwise an actual reset/revert won't work properly.
      //if (strokeWeight != DEFAULT_STROKE_WEIGHT) {
      strokeWeight(strokeWeight);
      //}
//      if (strokeCap != DEFAULT_STROKE_CAP) {
      strokeCap(strokeCap);
//      }
//      if (strokeJoin != DEFAULT_STROKE_JOIN) {
      strokeJoin(strokeJoin);
//      }
    } else {
      noStroke();
    }
    if (tint) {
      tint(tintColor);
    } else {
      noTint();
    }
//    if (smooth) {
//      smooth();
//    } else {
//      // Don't bother setting this, cuz it'll anger P3D.
//      noSmooth();
//    }
    if (textFont != null) {
//      System.out.println("  textFont in reapply is " + textFont);
      // textFont() resets the leading, so save it in case it's changed
      float saveLeading = textLeading;
      textFont(textFont, textSize);
      textLeading(saveLeading);
    }
    textMode(textMode);
    textAlign(textAlign, textAlignY);
    background(backgroundColor);

    blendMode(blendMode);

    reapplySettings = false;
  }

  // inherit from PImage
  //public void resize(int wide, int high){ }

  //////////////////////////////////////////////////////////////

  // HINTS

  /**
   * ( begin auto-generated from hint.xml )
   *
   * Set various hints and hacks for the renderer. This is used to handle
   * obscure rendering features that cannot be implemented in a consistent
   * manner across renderers. Many options will often graduate to standard
   * features instead of hints over time.
   * <br/> <br/>
   * hint(ENABLE_OPENGL_4X_SMOOTH) - Enable 4x anti-aliasing for P3D. This
   * can help force anti-aliasing if it has not been enabled by the user. On
   * some graphics cards, this can also be set by the graphics driver's
   * control panel, however not all cards make this available. This hint must
   * be called immediately after the size() command because it resets the
   * renderer, obliterating any settings and anything drawn (and like size(),
   * re-running the code that came before it again).
   * <br/> <br/>
   * hint(DISABLE_OPENGL_2X_SMOOTH) - In Processing 1.0, Processing always
   * enables 2x smoothing when the P3D renderer is used. This hint disables
   * the default 2x smoothing and returns the smoothing behavior found in
   * earlier releases, where smooth() and noSmooth() could be used to enable
   * and disable smoothing, though the quality was inferior.
   * <br/> <br/>
   * hint(ENABLE_NATIVE_FONTS) - Use the native version fonts when they are
   * installed, rather than the bitmapped version from a .vlw file. This is
   * useful with the default (or JAVA2D) renderer setting, as it will improve
   * font rendering speed. This is not enabled by default, because it can be
   * misleading while testing because the type will look great on your
   * machine (because you have the font installed) but lousy on others'
   * machines if the identical font is unavailable. This option can only be
   * set per-sketch, and must be called before any use of textFont().
   * <br/> <br/>
   * hint(DISABLE_DEPTH_TEST) - Disable the zbuffer, allowing you to draw on
   * top of everything at will. When depth testing is disabled, items will be
   * drawn to the screen sequentially, like a painting. This hint is most
   * often used to draw in 3D, then draw in 2D on top of it (for instance, to
   * draw GUI controls in 2D on top of a 3D interface). Starting in release
   * 0149, this will also clear the depth buffer. Restore the default with
   * hint(ENABLE_DEPTH_TEST), but note that with the depth buffer cleared,
   * any 3D drawing that happens later in draw() will ignore existing shapes
   * on the screen.
   * <br/> <br/>
   * hint(ENABLE_DEPTH_SORT) - Enable primitive z-sorting of triangles and
   * lines in P3D and OPENGL. This can slow performance considerably, and the
   * algorithm is not yet perfect. Restore the default with hint(DISABLE_DEPTH_SORT).
   * <br/> <br/>
   * hint(DISABLE_OPENGL_ERROR_REPORT) - Speeds up the P3D renderer setting
   * by not checking for errors while running. Undo with hint(ENABLE_OPENGL_ERROR_REPORT).
   * <br/> <br/>
   * hint(ENABLE_BUFFER_READING) - Depth and stencil buffers in P2D/P3D will be
   * downsampled to make PGL#readPixels work with multisampling. Enabling this
   * introduces some overhead, so if you experience bad performance, disable
   * multisampling with noSmooth() instead. This hint is not intended to be
   * enabled and disabled repeatedely, so call this once in setup() or after
   * creating your PGraphics2D/3D. You can restore the default with
   * hint(DISABLE_BUFFER_READING) if you don't plan to read depth from
   * this PGraphics anymore.
   * <br/> <br/>
   * hint(ENABLE_KEY_REPEAT) - Auto-repeating key events are discarded
   * by default (works only in P2D/P3D); use this hint to get all the key events
   * (including auto-repeated). Call hint(DISABLE_KEY_REPEAT) to get events
   * only when the key goes physically up or down.
   * <br/> <br/>
   * hint(DISABLE_ASYNC_SAVEFRAME) - P2D/P3D only - save() and saveFrame()
   * will not use separate threads for saving and will block until the image
   * is written to the drive. This was the default behavior in 3.0b7 and before.
   * To enable, call hint(ENABLE_ASYNC_SAVEFRAME).
   * <br/> <br/>
   * As of release 0149, unhint() has been removed in favor of adding
   * additional ENABLE/DISABLE constants to reset the default behavior. This
   * prevents the double negatives, and also reinforces which hints can be
   * enabled or disabled.
   *
   * ( end auto-generated )
   *
   * @webref rendering
   * @param which name of the hint to be enabled or disabled
   * @see PGraphics
   * @see PApplet#createGraphics(int, int, String, String)
   * @see PApplet#size(int, int)
   */
  @SuppressWarnings("deprecation")
  public void hint(int which) {
    if (which == ENABLE_NATIVE_FONTS ||
        which == DISABLE_NATIVE_FONTS) {
      showWarning("hint(ENABLE_NATIVE_FONTS) no longer supported. " +
                  "Use createFont() instead.");
    }
    if (which == ENABLE_KEY_REPEAT) {
      parent.keyRepeatEnabled = true;
    } else if (which == DISABLE_KEY_REPEAT) {
      parent.keyRepeatEnabled = false;
    }
    if (which > 0) {
      hints[which] = true;
    } else {
      hints[-which] = false;
    }
  }


  //////////////////////////////////////////////////////////////

  // VERTEX SHAPES

  /**
   * Start a new shape of type POLYGON
   */
  public void beginShape() {
    beginShape(POLYGON);
  }


  /**
   * ( begin auto-generated from beginShape.xml )
   *
   * Using the <b>beginShape()</b> and <b>endShape()</b> functions allow
   * creating more complex forms. <b>beginShape()</b> begins recording
   * vertices for a shape and <b>endShape()</b> stops recording. The value of
   * the <b>MODE</b> parameter tells it which types of shapes to create from
   * the provided vertices. With no mode specified, the shape can be any
   * irregular polygon. The parameters available for beginShape() are POINTS,
   * LINES, TRIANGLES, TRIANGLE_FAN, TRIANGLE_STRIP, QUADS, and QUAD_STRIP.
   * After calling the <b>beginShape()</b> function, a series of
   * <b>vertex()</b> commands must follow. To stop drawing the shape, call
   * <b>endShape()</b>. The <b>vertex()</b> function with two parameters
   * specifies a position in 2D and the <b>vertex()</b> function with three
   * parameters specifies a position in 3D. Each shape will be outlined with
   * the current stroke color and filled with the fill color.
   * <br/> <br/>
   * Transformations such as <b>translate()</b>, <b>rotate()</b>, and
   * <b>scale()</b> do not work within <b>beginShape()</b>. It is also not
   * possible to use other shapes, such as <b>ellipse()</b> or <b>rect()</b>
   * within <b>beginShape()</b>.
   * <br/> <br/>
   * The P3D renderer settings allow <b>stroke()</b> and <b>fill()</b>
   * settings to be altered per-vertex, however the default P2D renderer does
   * not. Settings such as <b>strokeWeight()</b>, <b>strokeCap()</b>, and
   * <b>strokeJoin()</b> cannot be changed while inside a
   * <b>beginShape()</b>/<b>endShape()</b> block with any renderer.
   *
   * ( end auto-generated )
   * @webref shape:vertex
   * @param kind Either POINTS, LINES, TRIANGLES, TRIANGLE_FAN, TRIANGLE_STRIP, QUADS, or QUAD_STRIP
   * @see PShape
   * @see PGraphics#endShape()
   * @see PGraphics#vertex(float, float, float, float, float)
   * @see PGraphics#curveVertex(float, float, float)
   * @see PGraphics#bezierVertex(float, float, float, float, float, float, float, float, float)
   */
  public void beginShape(int kind) {
    shape = kind;
  }


  /**
   * Sets whether the upcoming vertex is part of an edge.
   * Equivalent to glEdgeFlag(), for people familiar with OpenGL.
   */
  public void edge(boolean edge) {
   this.edge = edge;
  }


  /**
   * ( begin auto-generated from normal.xml )
   *
   * Sets the current normal vector. This is for drawing three dimensional
   * shapes and surfaces and specifies a vector perpendicular to the surface
   * of the shape which determines how lighting affects it. Processing
   * attempts to automatically assign normals to shapes, but since that's
   * imperfect, this is a better option when you want more control. This
   * function is identical to glNormal3f() in OpenGL.
   *
   * ( end auto-generated )
   * @webref lights_camera:lights
   * @param nx x direction
   * @param ny y direction
   * @param nz z direction
   * @see PGraphics#beginShape(int)
   * @see PGraphics#endShape(int)
   * @see PGraphics#lights()
   */
  public void normal(float nx, float ny, float nz) {
    normalX = nx;
    normalY = ny;
    normalZ = nz;

    // if drawing a shape and the normal hasn't been set yet,
    // then we need to set the normals for each vertex so far
    if (shape != 0) {
      if (normalMode == NORMAL_MODE_AUTO) {
        // One normal per begin/end shape
        normalMode = NORMAL_MODE_SHAPE;
      } else if (normalMode == NORMAL_MODE_SHAPE) {
        // a separate normal for each vertex
        normalMode = NORMAL_MODE_VERTEX;
      }
    }
  }


  public void attribPosition(String name, float x, float y, float z) {
    showMissingWarning("attrib");
  }


  public void attribNormal(String name, float nx, float ny, float nz) {
    showMissingWarning("attrib");
  }


  public void attribColor(String name, int color) {
    showMissingWarning("attrib");
  }


  public void attrib(String name, float... values) {
    showMissingWarning("attrib");
  }


  public void attrib(String name, int... values) {
    showMissingWarning("attrib");
  }


  public void attrib(String name, boolean... values) {
    showMissingWarning("attrib");
  }


  /**
   * ( begin auto-generated from textureMode.xml )
   *
   * Sets the coordinate space for texture mapping. There are two options,
   * IMAGE, which refers to the actual coordinates of the image, and
   * NORMAL, which refers to a normalized space of values ranging from 0
   * to 1. The default mode is IMAGE. In IMAGE, if an image is 100 x 200
   * pixels, mapping the image onto the entire size of a quad would require
   * the points (0,0) (0,100) (100,200) (0,200). The same mapping in
   * NORMAL_SPACE is (0,0) (0,1) (1,1) (0,1).
   *
   * ( end auto-generated )
   * @webref image:textures
   * @param mode either IMAGE or NORMAL
   * @see PGraphics#texture(PImage)
   * @see PGraphics#textureWrap(int)
   */
  public void textureMode(int mode) {
    if (mode != IMAGE && mode != NORMAL) {
      throw new RuntimeException("textureMode() only supports IMAGE and NORMAL");
    }
    this.textureMode = mode;
  }

  /**
   * ( begin auto-generated from textureWrap.xml )
   *
   * Description to come...
   *
   * ( end auto-generated from textureWrap.xml )
   *
   * @webref image:textures
   * @param wrap Either CLAMP (default) or REPEAT
   * @see PGraphics#texture(PImage)
   * @see PGraphics#textureMode(int)
   */
  public void textureWrap(int wrap) {
    showMissingWarning("textureWrap");
  }


  /**
   * ( begin auto-generated from texture.xml )
   *
   * Sets a texture to be applied to vertex points. The <b>texture()</b>
   * function must be called between <b>beginShape()</b> and
   * <b>endShape()</b> and before any calls to <b>vertex()</b>.
   * <br/> <br/>
   * When textures are in use, the fill color is ignored. Instead, use tint()
   * to specify the color of the texture as it is applied to the shape.
   *
   * ( end auto-generated )
   * @webref image:textures
   * @param image reference to a PImage object
   * @see PGraphics#textureMode(int)
   * @see PGraphics#textureWrap(int)
   * @see PGraphics#beginShape(int)
   * @see PGraphics#endShape(int)
   * @see PGraphics#vertex(float, float, float, float, float)
   */
  public void texture(PImage image) {
    textureImage = image;
  }


  /**
   * Removes texture image for current shape.
   * Needs to be called between beginShape and endShape
   *
   */
  public void noTexture() {
    textureImage = null;
  }


  protected void vertexCheck() {
    if (vertexCount == vertices.length) {
      float temp[][] = new float[vertexCount << 1][VERTEX_FIELD_COUNT];
      System.arraycopy(vertices, 0, temp, 0, vertexCount);
      vertices = temp;
    }
  }


  public void vertex(float x, float y) {
    vertexCheck();
    float[] vertex = vertices[vertexCount];

    curveVertexCount = 0;

    vertex[X] = x;
    vertex[Y] = y;
    vertex[Z] = 0;

    vertex[EDGE] = edge ? 1 : 0;

//    if (fill) {
//      vertex[R] = fillR;
//      vertex[G] = fillG;
//      vertex[B] = fillB;
//      vertex[A] = fillA;
//    }
    boolean textured = textureImage != null;
    if (fill || textured) {
      if (!textured) {
        vertex[R] = fillR;
        vertex[G] = fillG;
        vertex[B] = fillB;
        vertex[A] = fillA;
      } else {
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
      }
    }

    if (stroke) {
      vertex[SR] = strokeR;
      vertex[SG] = strokeG;
      vertex[SB] = strokeB;
      vertex[SA] = strokeA;
      vertex[SW] = strokeWeight;
    }

    if (textured) {
      vertex[U] = textureU;
      vertex[V] = textureV;
    }

    if (autoNormal) {
      float norm2 = normalX * normalX + normalY * normalY + normalZ * normalZ;
      if (norm2 < EPSILON) {
        vertex[HAS_NORMAL] = 0;
      } else {
        if (Math.abs(norm2 - 1) > EPSILON) {
          // The normal vector is not normalized.
          float norm = PApplet.sqrt(norm2);
          normalX /= norm;
          normalY /= norm;
          normalZ /= norm;
        }
        vertex[HAS_NORMAL] = 1;
      }
    } else {
      vertex[HAS_NORMAL] = 1;
    }

    vertexCount++;
  }


  public void vertex(float x, float y, float z) {
    vertexCheck();
    float[] vertex = vertices[vertexCount];

    // only do this if we're using an irregular (POLYGON) shape that
    // will go through the triangulator. otherwise it'll do thinks like
    // disappear in mathematically odd ways
    // http://dev.processing.org/bugs/show_bug.cgi?id=444
    if (shape == POLYGON) {
      if (vertexCount > 0) {
        float pvertex[] = vertices[vertexCount-1];
        if ((Math.abs(pvertex[X] - x) < EPSILON) &&
            (Math.abs(pvertex[Y] - y) < EPSILON) &&
            (Math.abs(pvertex[Z] - z) < EPSILON)) {
          // this vertex is identical, don't add it,
          // because it will anger the triangulator
          return;
        }
      }
    }

    // User called vertex(), so that invalidates anything queued up for curve
    // vertices. If this is internally called by curveVertexSegment,
    // then curveVertexCount will be saved and restored.
    curveVertexCount = 0;

    vertex[X] = x;
    vertex[Y] = y;
    vertex[Z] = z;

    vertex[EDGE] = edge ? 1 : 0;

    boolean textured = textureImage != null;
    if (fill || textured) {
      if (!textured) {
        vertex[R] = fillR;
        vertex[G] = fillG;
        vertex[B] = fillB;
        vertex[A] = fillA;
      } else {
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

    if (textured) {
      vertex[U] = textureU;
      vertex[V] = textureV;
    }

    if (autoNormal) {
      float norm2 = normalX * normalX + normalY * normalY + normalZ * normalZ;
      if (norm2 < EPSILON) {
        vertex[HAS_NORMAL] = 0;
      } else {
        if (Math.abs(norm2 - 1) > EPSILON) {
          // The normal vector is not normalized.
          float norm = PApplet.sqrt(norm2);
          normalX /= norm;
          normalY /= norm;
          normalZ /= norm;
        }
        vertex[HAS_NORMAL] = 1;
      }
    } else {
      vertex[HAS_NORMAL] = 1;
    }

    vertex[NX] = normalX;
    vertex[NY] = normalY;
    vertex[NZ] = normalZ;

    vertex[BEEN_LIT] = 0;

    vertexCount++;
  }


  /**
   * Used by renderer subclasses or PShape to efficiently pass in already
   * formatted vertex information.
   * @param v vertex parameters, as a float array of length VERTEX_FIELD_COUNT
   */
  public void vertex(float[] v) {
    vertexCheck();
    curveVertexCount = 0;
    float[] vertex = vertices[vertexCount];
    System.arraycopy(v, 0, vertex, 0, VERTEX_FIELD_COUNT);
    vertexCount++;
  }


  public void vertex(float x, float y, float u, float v) {
    vertexTexture(u, v);
    vertex(x, y);
  }

/**
   * ( begin auto-generated from vertex.xml )
   *
   * All shapes are constructed by connecting a series of vertices.
   * <b>vertex()</b> is used to specify the vertex coordinates for points,
   * lines, triangles, quads, and polygons and is used exclusively within the
   * <b>beginShape()</b> and <b>endShape()</b> function.<br />
   * <br />
   * Drawing a vertex in 3D using the <b>z</b> parameter requires the P3D
   * parameter in combination with size as shown in the above example.<br />
   * <br />
   * This function is also used to map a texture onto the geometry. The
   * <b>texture()</b> function declares the texture to apply to the geometry
   * and the <b>u</b> and <b>v</b> coordinates set define the mapping of this
   * texture to the form. By default, the coordinates used for <b>u</b> and
   * <b>v</b> are specified in relation to the image's size in pixels, but
   * this relation can be changed with <b>textureMode()</b>.
   *
   * ( end auto-generated )
 * @webref shape:vertex
 * @param x x-coordinate of the vertex
 * @param y y-coordinate of the vertex
 * @param z z-coordinate of the vertex
 * @param u horizontal coordinate for the texture mapping
 * @param v vertical coordinate for the texture mapping
 * @see PGraphics#beginShape(int)
 * @see PGraphics#endShape(int)
 * @see PGraphics#bezierVertex(float, float, float, float, float, float, float, float, float)
 * @see PGraphics#quadraticVertex(float, float, float, float, float, float)
 * @see PGraphics#curveVertex(float, float, float)
 * @see PGraphics#texture(PImage)
 */
  public void vertex(float x, float y, float z, float u, float v) {
    vertexTexture(u, v);
    vertex(x, y, z);
  }


  /**
   * Internal method to copy all style information for the given vertex.
   * Can be overridden by subclasses to handle only properties pertinent to
   * that renderer. (e.g. no need to copy the emissive color in P2D)
   */
//  protected void vertexStyle() {
//  }


  /**
   * Set (U, V) coords for the next vertex in the current shape.
   * This is ugly as its own function, and will (almost?) always be
   * coincident with a call to vertex. As of beta, this was moved to
   * the protected method you see here, and called from an optional
   * param of and overloaded vertex().
   * <p/>
   * The parameters depend on the current textureMode. When using
   * textureMode(IMAGE), the coordinates will be relative to the size
   * of the image texture, when used with textureMode(NORMAL),
   * they'll be in the range 0..1.
   * <p/>
   * Used by both PGraphics2D (for images) and PGraphics3D.
   */
  protected void vertexTexture(float u, float v) {
    if (textureImage == null) {
      throw new RuntimeException("You must first call texture() before " +
                                 "using u and v coordinates with vertex()");
    }
    if (textureMode == IMAGE) {
      u /= textureImage.width;
      v /= textureImage.height;
    }

    textureU = u;
    textureV = v;

    if (textureU < 0) textureU = 0;
    else if (textureU > 1) textureU = 1;

    if (textureV < 0) textureV = 0;
    else if (textureV > 1) textureV = 1;
  }


//  /** This feature is in testing, do not use or rely upon its implementation */
//  public void breakShape() {
//    showWarning("This renderer cannot currently handle concave shapes, " +
//                "or shapes with holes.");
//  }

  /**
   * @webref shape:vertex
   */
  public void beginContour() {
    showMissingWarning("beginContour");
  }


  /**
   * @webref shape:vertex
   */
  public void endContour() {
    showMissingWarning("endContour");
  }


  public void endShape() {
    endShape(OPEN);
  }


  /**
   * ( begin auto-generated from endShape.xml )
   *
   * The <b>endShape()</b> function is the companion to <b>beginShape()</b>
   * and may only be called after <b>beginShape()</b>. When <b>endshape()</b>
   * is called, all of image data defined since the previous call to
   * <b>beginShape()</b> is written into the image buffer. The constant CLOSE
   * as the value for the MODE parameter to close the shape (to connect the
   * beginning and the end).
   *
   * ( end auto-generated )
   * @webref shape:vertex
   * @param mode use CLOSE to close the shape
   * @see PShape
   * @see PGraphics#beginShape(int)
   */
  public void endShape(int mode) {
  }



  //////////////////////////////////////////////////////////////

  // SHAPE I/O


  /**
   * @webref shape
   * @param filename name of file to load, can be .svg or .obj
   * @see PShape
   * @see PApplet#createShape()
   */
  public PShape loadShape(String filename) {
    return loadShape(filename, null);
  }


  /**
   * @nowebref
   */
  public PShape loadShape(String filename, String options) {
    showMissingWarning("loadShape");
    return null;
  }



  //////////////////////////////////////////////////////////////

  // SHAPE CREATION


  /**
   * @webref shape
   * @see PShape
   * @see PShape#endShape()
   * @see PApplet#loadShape(String)
   */
  public PShape createShape() {
    // Defaults to GEOMETRY (rather than GROUP like the default constructor)
    // because that's how people will use it within a sketch.
    return createShape(PShape.GEOMETRY);
  }


  // POINTS, LINES, TRIANGLES, TRIANGLE_FAN, TRIANGLE_STRIP, QUADS, QUAD_STRIP
  public PShape createShape(int type) {
    // If it's a PRIMITIVE, it needs the 'params' field anyway
    if (type == PConstants.GROUP ||
        type == PShape.PATH ||
        type == PShape.GEOMETRY) {
      return createShapeFamily(type);
    }
    final String msg =
      "Only GROUP, PShape.PATH, and PShape.GEOMETRY work with createShape()";
    throw new IllegalArgumentException(msg);
  }


  /** Override this method to return an appropriate shape for your renderer */
  protected PShape createShapeFamily(int type) {
    return new PShape(this, type);
//    showMethodWarning("createShape()");
//    return null;
  }


  /**
   * @param kind either POINT, LINE, TRIANGLE, QUAD, RECT, ELLIPSE, ARC, BOX, SPHERE
   * @param p parameters that match the kind of shape
   */
  public PShape createShape(int kind, float... p) {
    int len = p.length;

    if (kind == POINT) {
      if (is3D() && len != 2 && len != 3) {
        throw new IllegalArgumentException("Use createShape(POINT, x, y) or createShape(POINT, x, y, z)");
      } else if (is2D() && len != 2) {
        throw new IllegalArgumentException("Use createShape(POINT, x, y)");
      }
      return createShapePrimitive(kind, p);

    } else if (kind == LINE) {
      if (is3D() && len != 4 && len != 6) {
        throw new IllegalArgumentException("Use createShape(LINE, x1, y1, x2, y2) or createShape(LINE, x1, y1, z1, x2, y2, z1)");
      } else if (is2D() && len != 4) {
        throw new IllegalArgumentException("Use createShape(LINE, x1, y1, x2, y2)");
      }
      return createShapePrimitive(kind, p);

    } else if (kind == TRIANGLE) {
      if (len != 6) {
        throw new IllegalArgumentException("Use createShape(TRIANGLE, x1, y1, x2, y2, x3, y3)");
      }
      return createShapePrimitive(kind, p);

    } else if (kind == QUAD) {
      if (len != 8) {
        throw new IllegalArgumentException("Use createShape(QUAD, x1, y1, x2, y2, x3, y3, x4, y4)");
      }
      return createShapePrimitive(kind, p);

    } else if (kind == RECT) {
      if (len != 4 && len != 5 && len != 8) {
        throw new IllegalArgumentException("Wrong number of parameters for createShape(RECT), see the reference");
      }
      return createShapePrimitive(kind, p);

    } else if (kind == ELLIPSE) {
      if (len != 4) {
        throw new IllegalArgumentException("Use createShape(ELLIPSE, x, y, w, h)");
      }
      return createShapePrimitive(kind, p);

    } else if (kind == ARC) {
      if (len != 6 && len != 7) {
        throw new IllegalArgumentException("Use createShape(ARC, x, y, w, h, start, stop) or createShape(ARC, x, y, w, h, start, stop, arcMode)");
      }
      return createShapePrimitive(kind, p);

    } else if (kind == BOX) {
      if (!is3D()) {
        throw new IllegalArgumentException("createShape(BOX) is not supported in 2D");
      } else if (len != 1 && len != 3) {
        throw new IllegalArgumentException("Use createShape(BOX, size) or createShape(BOX, width, height, depth)");
      }
      return createShapePrimitive(kind, p);

    } else if (kind == SPHERE) {
      if (!is3D()) {
        throw new IllegalArgumentException("createShape(SPHERE) is not supported in 2D");
      } else if (len != 1) {
        throw new IllegalArgumentException("Use createShape(SPHERE, radius)");
      }
      return createShapePrimitive(kind, p);
    }
    throw new IllegalArgumentException("Unknown shape type passed to createShape()");
  }


  /** Override this to have a custom shape object used by your renderer. */
  protected PShape createShapePrimitive(int kind, float... p) {
//    showMethodWarning("createShape()");
//    return null;
    return new PShape(this, kind, p);
  }



  //////////////////////////////////////////////////////////////

  // SHADERS

  /**
   * ( begin auto-generated from loadShader.xml )
   *
   * This is a new reference entry for Processing 2.0. It will be updated shortly.
   *
   * ( end auto-generated )
   *
   * @webref rendering:shaders
   * @param fragFilename name of fragment shader file
   */
  public PShader loadShader(String fragFilename) {
    showMissingWarning("loadShader");
    return null;
  }


  /**
   * @param vertFilename name of vertex shader file
   */
  public PShader loadShader(String fragFilename, String vertFilename) {
    showMissingWarning("loadShader");
    return null;
  }


  /**
   * ( begin auto-generated from shader.xml )
   *
   * This is a new reference entry for Processing 2.0. It will be updated shortly.
   *
   * ( end auto-generated )
   *
   * @webref rendering:shaders
   * @param shader name of shader file
   */
  public void shader(PShader shader) {
    showMissingWarning("shader");
  }


  /**
   * @param kind type of shader, either POINTS, LINES, or TRIANGLES
   */
  public void shader(PShader shader, int kind) {
    showMissingWarning("shader");
  }


  /**
   * ( begin auto-generated from resetShader.xml )
   *
   * This is a new reference entry for Processing 2.0. It will be updated shortly.
   *
   * ( end auto-generated )
   *
   * @webref rendering:shaders
   */
  public void resetShader() {
    showMissingWarning("resetShader");
  }


  /**
   * @param kind type of shader, either POINTS, LINES, or TRIANGLES
   */
  public void resetShader(int kind) {
    showMissingWarning("resetShader");
  }


  /**
   * @param shader the fragment shader to apply
   */
  public void filter(PShader shader) {
    showMissingWarning("filter");
  }



  //////////////////////////////////////////////////////////////

  // CLIPPING

  /**
   * ( begin auto-generated from clip.xml )
   *
   * Limits the rendering to the boundaries of a rectangle defined
   * by the parameters. The boundaries are drawn based on the state
   * of the <b>imageMode()</b> fuction, either CORNER, CORNERS, or CENTER.
   *
   * ( end auto-generated )
   *
   * @webref rendering
   * @param a x-coordinate of the rectangle, by default
   * @param b y-coordinate of the rectangle, by default
   * @param c width of the rectangle, by default
   * @param d height of the rectangle, by default
   */
  public void clip(float a, float b, float c, float d) {
    if (imageMode == CORNER) {
      if (c < 0) {  // reset a negative width
        a += c; c = -c;
      }
      if (d < 0) {  // reset a negative height
        b += d; d = -d;
      }

      clipImpl(a, b, a + c, b + d);

    } else if (imageMode == CORNERS) {
      if (c < a) {  // reverse because x2 < x1
        float temp = a; a = c; c = temp;
      }
      if (d < b) {  // reverse because y2 < y1
        float temp = b; b = d; d = temp;
      }

      clipImpl(a, b, c, d);

    } else if (imageMode == CENTER) {
      // c and d are width/height
      if (c < 0) c = -c;
      if (d < 0) d = -d;
      float x1 = a - c/2;
      float y1 = b - d/2;

      clipImpl(x1, y1, x1 + c, y1 + d);
    }
  }


  protected void clipImpl(float x1, float y1, float x2, float y2) {
    showMissingWarning("clip");
  }


  /**
   * ( begin auto-generated from noClip.xml )
   *
   * Disables the clipping previously started by the <b>clip()</b> function.
   *
   * ( end auto-generated )
   *
   * @webref rendering
   */
  public void noClip() {
    showMissingWarning("noClip");
  }



  //////////////////////////////////////////////////////////////

  // BLEND

  /**
   * ( begin auto-generated from blendMode.xml )
   *
   * This is a new reference entry for Processing 2.0. It will be updated shortly.
   *
   * ( end auto-generated )
   *
   * @webref rendering
   * @param mode the blending mode to use
   */
  public void blendMode(int mode) {
    this.blendMode = mode;
    blendModeImpl();
  }


  protected void blendModeImpl() {
    if (blendMode != BLEND) {
      showMissingWarning("blendMode");
    }
  }



  //////////////////////////////////////////////////////////////

  // CURVE/BEZIER VERTEX HANDLING


  protected void bezierVertexCheck() {
    bezierVertexCheck(shape, vertexCount);
  }


  protected void bezierVertexCheck(int shape, int vertexCount) {
    if (shape == 0 || shape != POLYGON) {
      throw new RuntimeException("beginShape() or beginShape(POLYGON) " +
                                 "must be used before bezierVertex() or quadraticVertex()");
    }
    if (vertexCount == 0) {
      throw new RuntimeException("vertex() must be used at least once " +
                                 "before bezierVertex() or quadraticVertex()");
    }
  }


  public void bezierVertex(float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
    bezierInitCheck();
    bezierVertexCheck();
    PMatrix3D draw = bezierDrawMatrix;

    float[] prev = vertices[vertexCount-1];
    float x1 = prev[X];
    float y1 = prev[Y];

    float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
    float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
    float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;

    float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
    float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
    float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;

    for (int j = 0; j < bezierDetail; j++) {
      x1 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y1 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      vertex(x1, y1);
    }
  }


/**
   * ( begin auto-generated from bezierVertex.xml )
   *
   * Specifies vertex coordinates for Bezier curves. Each call to
   * <b>bezierVertex()</b> defines the position of two control points and one
   * anchor point of a Bezier curve, adding a new segment to a line or shape.
   * The first time <b>bezierVertex()</b> is used within a
   * <b>beginShape()</b> call, it must be prefaced with a call to
   * <b>vertex()</b> to set the first anchor point. This function must be
   * used between <b>beginShape()</b> and <b>endShape()</b> and only when
   * there is no MODE parameter specified to <b>beginShape()</b>. Using the
   * 3D version requires rendering with P3D (see the Environment reference
   * for more information).
   *
   * ( end auto-generated )
 * @webref shape:vertex
 * @param x2 the x-coordinate of the 1st control point
 * @param y2 the y-coordinate of the 1st control point
 * @param z2 the z-coordinate of the 1st control point
 * @param x3 the x-coordinate of the 2nd control point
 * @param y3 the y-coordinate of the 2nd control point
 * @param z3 the z-coordinate of the 2nd control point
 * @param x4 the x-coordinate of the anchor point
 * @param y4 the y-coordinate of the anchor point
 * @param z4 the z-coordinate of the anchor point
 * @see PGraphics#curveVertex(float, float, float)
 * @see PGraphics#vertex(float, float, float, float, float)
 * @see PGraphics#quadraticVertex(float, float, float, float, float, float)
 * @see PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
 */
  public void bezierVertex(float x2, float y2, float z2,
                           float x3, float y3, float z3,
                           float x4, float y4, float z4) {
    bezierInitCheck();
    bezierVertexCheck();
    PMatrix3D draw = bezierDrawMatrix;

    float[] prev = vertices[vertexCount-1];
    float x1 = prev[X];
    float y1 = prev[Y];
    float z1 = prev[Z];

    float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
    float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
    float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;

    float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
    float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
    float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;

    float zplot1 = draw.m10*z1 + draw.m11*z2 + draw.m12*z3 + draw.m13*z4;
    float zplot2 = draw.m20*z1 + draw.m21*z2 + draw.m22*z3 + draw.m23*z4;
    float zplot3 = draw.m30*z1 + draw.m31*z2 + draw.m32*z3 + draw.m33*z4;

    for (int j = 0; j < bezierDetail; j++) {
      x1 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y1 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      z1 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
      vertex(x1, y1, z1);
    }
  }


  /**
   * @webref shape:vertex
   * @param cx the x-coordinate of the control point
   * @param cy the y-coordinate of the control point
   * @param x3 the x-coordinate of the anchor point
   * @param y3 the y-coordinate of the anchor point
   * @see PGraphics#curveVertex(float, float, float)
   * @see PGraphics#vertex(float, float, float, float, float)
   * @see PGraphics#bezierVertex(float, float, float, float, float, float)
   * @see PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
   */
  public void quadraticVertex(float cx, float cy,
                              float x3, float y3) {
    float[] prev = vertices[vertexCount-1];
    float x1 = prev[X];
    float y1 = prev[Y];

    bezierVertex(x1 + ((cx-x1)*2/3.0f), y1 + ((cy-y1)*2/3.0f),
                 x3 + ((cx-x3)*2/3.0f), y3 + ((cy-y3)*2/3.0f),
                 x3, y3);
  }


  /**
   * @param cz the z-coordinate of the control point
   * @param z3 the z-coordinate of the anchor point
   */
  public void quadraticVertex(float cx, float cy, float cz,
                              float x3, float y3, float z3) {
    float[] prev = vertices[vertexCount-1];
    float x1 = prev[X];
    float y1 = prev[Y];
    float z1 = prev[Z];

    bezierVertex(x1 + ((cx-x1)*2/3.0f), y1 + ((cy-y1)*2/3.0f), z1 + ((cz-z1)*2/3.0f),
                 x3 + ((cx-x3)*2/3.0f), y3 + ((cy-y3)*2/3.0f), z3 + ((cz-z3)*2/3.0f),
                 x3, y3, z3);
  }


  protected void curveVertexCheck() {
    curveVertexCheck(shape);
  }


  /**
   * Perform initialization specific to curveVertex(), and handle standard
   * error modes. Can be overridden by subclasses that need the flexibility.
   */
  protected void curveVertexCheck(int shape) {
    if (shape != POLYGON) {
      throw new RuntimeException("You must use beginShape() or " +
                                 "beginShape(POLYGON) before curveVertex()");
    }
    // to improve code init time, allocate on first use.
    if (curveVertices == null) {
      curveVertices = new float[128][3];
    }

    if (curveVertexCount == curveVertices.length) {
      // Can't use PApplet.expand() cuz it doesn't do the copy properly
      float[][] temp = new float[curveVertexCount << 1][3];
      System.arraycopy(curveVertices, 0, temp, 0, curveVertexCount);
      curveVertices = temp;
    }
    curveInitCheck();
  }


 /**
   * ( begin auto-generated from curveVertex.xml )
   *
   * Specifies vertex coordinates for curves. This function may only be used
   * between <b>beginShape()</b> and <b>endShape()</b> and only when there is
   * no MODE parameter specified to <b>beginShape()</b>. The first and last
   * points in a series of <b>curveVertex()</b> lines will be used to guide
   * the beginning and end of a the curve. A minimum of four points is
   * required to draw a tiny curve between the second and third points.
   * Adding a fifth point with <b>curveVertex()</b> will draw the curve
   * between the second, third, and fourth points. The <b>curveVertex()</b>
   * function is an implementation of Catmull-Rom splines. Using the 3D
   * version requires rendering with P3D (see the Environment reference for
   * more information).
   *
   * ( end auto-generated )
  *
  * @webref shape:vertex
  * @param x the x-coordinate of the vertex
  * @param y the y-coordinate of the vertex
  * @see PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
  * @see PGraphics#beginShape(int)
  * @see PGraphics#endShape(int)
  * @see PGraphics#vertex(float, float, float, float, float)
  * @see PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
  * @see PGraphics#quadraticVertex(float, float, float, float, float, float)
  */
  public void curveVertex(float x, float y) {
    curveVertexCheck();
    float[] v = curveVertices[curveVertexCount];
    v[X] = x;
    v[Y] = y;
    curveVertexCount++;

    // draw a segment if there are enough points
    if (curveVertexCount > 3) {
      curveVertexSegment(curveVertices[curveVertexCount-4][X],
                         curveVertices[curveVertexCount-4][Y],
                         curveVertices[curveVertexCount-3][X],
                         curveVertices[curveVertexCount-3][Y],
                         curveVertices[curveVertexCount-2][X],
                         curveVertices[curveVertexCount-2][Y],
                         curveVertices[curveVertexCount-1][X],
                         curveVertices[curveVertexCount-1][Y]);
    }
  }

  /**
   * @param z the z-coordinate of the vertex
   */
  public void curveVertex(float x, float y, float z) {
    curveVertexCheck();
    float[] v = curveVertices[curveVertexCount];
    v[X] = x;
    v[Y] = y;
    v[Z] = z;
    curveVertexCount++;

    // draw a segment if there are enough points
    if (curveVertexCount > 3) {
      curveVertexSegment(curveVertices[curveVertexCount-4][X],
                         curveVertices[curveVertexCount-4][Y],
                         curveVertices[curveVertexCount-4][Z],
                         curveVertices[curveVertexCount-3][X],
                         curveVertices[curveVertexCount-3][Y],
                         curveVertices[curveVertexCount-3][Z],
                         curveVertices[curveVertexCount-2][X],
                         curveVertices[curveVertexCount-2][Y],
                         curveVertices[curveVertexCount-2][Z],
                         curveVertices[curveVertexCount-1][X],
                         curveVertices[curveVertexCount-1][Y],
                         curveVertices[curveVertexCount-1][Z]);
    }
  }


  /**
   * Handle emitting a specific segment of Catmull-Rom curve. This can be
   * overridden by subclasses that need more efficient rendering options.
   */
  protected void curveVertexSegment(float x1, float y1,
                                    float x2, float y2,
                                    float x3, float y3,
                                    float x4, float y4) {
    float x0 = x2;
    float y0 = y2;

    PMatrix3D draw = curveDrawMatrix;

    float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
    float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
    float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;

    float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
    float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
    float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;

    // vertex() will reset splineVertexCount, so save it
    int savedCount = curveVertexCount;

    vertex(x0, y0);
    for (int j = 0; j < curveDetail; j++) {
      x0 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y0 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      vertex(x0, y0);
    }
    curveVertexCount = savedCount;
  }


  /**
   * Handle emitting a specific segment of Catmull-Rom curve. This can be
   * overridden by subclasses that need more efficient rendering options.
   */
  protected void curveVertexSegment(float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3,
                                    float x4, float y4, float z4) {
    float x0 = x2;
    float y0 = y2;
    float z0 = z2;

    PMatrix3D draw = curveDrawMatrix;

    float xplot1 = draw.m10*x1 + draw.m11*x2 + draw.m12*x3 + draw.m13*x4;
    float xplot2 = draw.m20*x1 + draw.m21*x2 + draw.m22*x3 + draw.m23*x4;
    float xplot3 = draw.m30*x1 + draw.m31*x2 + draw.m32*x3 + draw.m33*x4;

    float yplot1 = draw.m10*y1 + draw.m11*y2 + draw.m12*y3 + draw.m13*y4;
    float yplot2 = draw.m20*y1 + draw.m21*y2 + draw.m22*y3 + draw.m23*y4;
    float yplot3 = draw.m30*y1 + draw.m31*y2 + draw.m32*y3 + draw.m33*y4;

    // vertex() will reset splineVertexCount, so save it
    int savedCount = curveVertexCount;

    float zplot1 = draw.m10*z1 + draw.m11*z2 + draw.m12*z3 + draw.m13*z4;
    float zplot2 = draw.m20*z1 + draw.m21*z2 + draw.m22*z3 + draw.m23*z4;
    float zplot3 = draw.m30*z1 + draw.m31*z2 + draw.m32*z3 + draw.m33*z4;

    vertex(x0, y0, z0);
    for (int j = 0; j < curveDetail; j++) {
      x0 += xplot1; xplot1 += xplot2; xplot2 += xplot3;
      y0 += yplot1; yplot1 += yplot2; yplot2 += yplot3;
      z0 += zplot1; zplot1 += zplot2; zplot2 += zplot3;
      vertex(x0, y0, z0);
    }
    curveVertexCount = savedCount;
  }



  //////////////////////////////////////////////////////////////

  // SIMPLE SHAPES WITH ANALOGUES IN beginShape()


  /**
   * ( begin auto-generated from point.xml )
   *
   * Draws a point, a coordinate in space at the dimension of one pixel. The
   * first parameter is the horizontal value for the point, the second value
   * is the vertical value for the point, and the optional third value is the
   * depth value. Drawing this shape in 3D with the <b>z</b> parameter
   * requires the P3D parameter in combination with <b>size()</b> as shown in
   * the above example.
   *
   * ( end auto-generated )
   *
   * @webref shape:2d_primitives
   * @param x x-coordinate of the point
   * @param y y-coordinate of the point
   * @see PGraphics#stroke(int)
   */
  public void point(float x, float y) {
    beginShape(POINTS);
    vertex(x, y);
    endShape();
  }

  /**
   * @param z z-coordinate of the point
   */
  public void point(float x, float y, float z) {
    beginShape(POINTS);
    vertex(x, y, z);
    endShape();
  }

  /**
   * ( begin auto-generated from line.xml )
   *
   * Draws a line (a direct path between two points) to the screen. The
   * version of <b>line()</b> with four parameters draws the line in 2D.  To
   * color a line, use the <b>stroke()</b> function. A line cannot be filled,
   * therefore the <b>fill()</b> function will not affect the color of a
   * line. 2D lines are drawn with a width of one pixel by default, but this
   * can be changed with the <b>strokeWeight()</b> function. The version with
   * six parameters allows the line to be placed anywhere within XYZ space.
   * Drawing this shape in 3D with the <b>z</b> parameter requires the P3D
   * parameter in combination with <b>size()</b> as shown in the above example.
   *
   * ( end auto-generated )
   * @webref shape:2d_primitives
   * @param x1 x-coordinate of the first point
   * @param y1 y-coordinate of the first point
   * @param x2 x-coordinate of the second point
   * @param y2 y-coordinate of the second point
   * @see PGraphics#strokeWeight(float)
   * @see PGraphics#strokeJoin(int)
   * @see PGraphics#strokeCap(int)
   * @see PGraphics#beginShape()
   */
  public void line(float x1, float y1, float x2, float y2) {
    beginShape(LINES);
    vertex(x1, y1);
    vertex(x2, y2);
    endShape();
  }

  /**
   * @param z1 z-coordinate of the first point
   * @param z2 z-coordinate of the second point
   */
  public void line(float x1, float y1, float z1,
                   float x2, float y2, float z2) {
    beginShape(LINES);
    vertex(x1, y1, z1);
    vertex(x2, y2, z2);
    endShape();
  }

  /**
   * ( begin auto-generated from triangle.xml )
   *
   * A triangle is a plane created by connecting three points. The first two
   * arguments specify the first point, the middle two arguments specify the
   * second point, and the last two arguments specify the third point.
   *
   * ( end auto-generated )
   * @webref shape:2d_primitives
   * @param x1 x-coordinate of the first point
   * @param y1 y-coordinate of the first point
   * @param x2 x-coordinate of the second point
   * @param y2 y-coordinate of the second point
   * @param x3 x-coordinate of the third point
   * @param y3 y-coordinate of the third point
   * @see PApplet#beginShape()
   */
  public void triangle(float x1, float y1, float x2, float y2,
                       float x3, float y3) {
    beginShape(TRIANGLES);
    vertex(x1, y1);
    vertex(x2, y2);
    vertex(x3, y3);
    endShape();
  }


  /**
   * ( begin auto-generated from quad.xml )
   *
   * A quad is a quadrilateral, a four sided polygon. It is similar to a
   * rectangle, but the angles between its edges are not constrained to
   * ninety degrees. The first pair of parameters (x1,y1) sets the first
   * vertex and the subsequent pairs should proceed clockwise or
   * counter-clockwise around the defined shape.
   *
   * ( end auto-generated )
   * @webref shape:2d_primitives
   * @param x1 x-coordinate of the first corner
   * @param y1 y-coordinate of the first corner
   * @param x2 x-coordinate of the second corner
   * @param y2 y-coordinate of the second corner
   * @param x3 x-coordinate of the third corner
   * @param y3 y-coordinate of the third corner
   * @param x4 x-coordinate of the fourth corner
   * @param y4 y-coordinate of the fourth corner
   */
  public void quad(float x1, float y1, float x2, float y2,
                   float x3, float y3, float x4, float y4) {
    beginShape(QUADS);
    vertex(x1, y1);
    vertex(x2, y2);
    vertex(x3, y3);
    vertex(x4, y4);
    endShape();
  }



  //////////////////////////////////////////////////////////////

  // RECT

  /**
   * ( begin auto-generated from rectMode.xml )
   *
   * Modifies the location from which rectangles draw. The default mode is
   * <b>rectMode(CORNER)</b>, which specifies the location to be the upper
   * left corner of the shape and uses the third and fourth parameters of
   * <b>rect()</b> to specify the width and height. The syntax
   * <b>rectMode(CORNERS)</b> uses the first and second parameters of
   * <b>rect()</b> to set the location of one corner and uses the third and
   * fourth parameters to set the opposite corner. The syntax
   * <b>rectMode(CENTER)</b> draws the image from its center point and uses
   * the third and forth parameters of <b>rect()</b> to specify the image's
   * width and height. The syntax <b>rectMode(RADIUS)</b> draws the image
   * from its center point and uses the third and forth parameters of
   * <b>rect()</b> to specify half of the image's width and height. The
   * parameter must be written in ALL CAPS because Processing is a case
   * sensitive language. Note: In version 125, the mode named CENTER_RADIUS
   * was shortened to RADIUS.
   *
   * ( end auto-generated )
   * @webref shape:attributes
   * @param mode either CORNER, CORNERS, CENTER, or RADIUS
   * @see PGraphics#rect(float, float, float, float)
   */
  public void rectMode(int mode) {
    rectMode = mode;
  }


  /**
   * ( begin auto-generated from rect.xml )
   *
   * Draws a rectangle to the screen. A rectangle is a four-sided shape with
   * every angle at ninety degrees. By default, the first two parameters set
   * the location of the upper-left corner, the third sets the width, and the
   * fourth sets the height. These parameters may be changed with the
   * <b>rectMode()</b> function.
   *
   * ( end auto-generated )
   *
   * @webref shape:2d_primitives
   * @param a x-coordinate of the rectangle by default
   * @param b y-coordinate of the rectangle by default
   * @param c width of the rectangle by default
   * @param d height of the rectangle by default
   * @see PGraphics#rectMode(int)
   * @see PGraphics#quad(float, float, float, float, float, float, float, float)
   */
  public void rect(float a, float b, float c, float d) {
    float hradius, vradius;
    switch (rectMode) {
    case CORNERS:
      break;
    case CORNER:
      c += a; d += b;
      break;
    case RADIUS:
      hradius = c;
      vradius = d;
      c = a + hradius;
      d = b + vradius;
      a -= hradius;
      b -= vradius;
      break;
    case CENTER:
      hradius = c / 2.0f;
      vradius = d / 2.0f;
      c = a + hradius;
      d = b + vradius;
      a -= hradius;
      b -= vradius;
    }

    if (a > c) {
      float temp = a; a = c; c = temp;
    }

    if (b > d) {
      float temp = b; b = d; d = temp;
    }

    rectImpl(a, b, c, d);
  }


  protected void rectImpl(float x1, float y1, float x2, float y2) {
    quad(x1, y1,  x2, y1,  x2, y2,  x1, y2);
  }


  // Still need to do a lot of work here to make it behave across renderers
  // (e.g. not all renderers use the vertices array)
  // Also seems to be some issues on quality here (too dense)
  // http://code.google.com/p/processing/issues/detail?id=265
//  private void quadraticVertex(float cpx, float cpy, float x, float y) {
//    float[] prev = vertices[vertexCount - 1];
//    float prevX = prev[X];
//    float prevY = prev[Y];
//    float cp1x = prevX + 2.0f/3.0f*(cpx - prevX);
//    float cp1y = prevY + 2.0f/3.0f*(cpy - prevY);
//    float cp2x = cp1x + (x - prevX)/3.0f;
//    float cp2y = cp1y + (y - prevY)/3.0f;
//    bezierVertex(cp1x, cp1y, cp2x, cp2y, x, y);
//  }

  /**
   * @param r radii for all four corners
   */
  public void rect(float a, float b, float c, float d, float r) {
    rect(a, b, c, d, r, r, r, r);
  }

  /**
   * @param tl radius for top-left corner
   * @param tr radius for top-right corner
   * @param br radius for bottom-right corner
   * @param bl radius for bottom-left corner
   */
  public void rect(float a, float b, float c, float d,
                   float tl, float tr, float br, float bl) {
    float hradius, vradius;
    switch (rectMode) {
    case CORNERS:
      break;
    case CORNER:
      c += a; d += b;
      break;
    case RADIUS:
      hradius = c;
      vradius = d;
      c = a + hradius;
      d = b + vradius;
      a -= hradius;
      b -= vradius;
      break;
    case CENTER:
      hradius = c / 2.0f;
      vradius = d / 2.0f;
      c = a + hradius;
      d = b + vradius;
      a -= hradius;
      b -= vradius;
    }

    if (a > c) {
      float temp = a; a = c; c = temp;
    }

    if (b > d) {
      float temp = b; b = d; d = temp;
    }

    float maxRounding = PApplet.min((c - a) / 2, (d - b) / 2);
    if (tl > maxRounding) tl = maxRounding;
    if (tr > maxRounding) tr = maxRounding;
    if (br > maxRounding) br = maxRounding;
    if (bl > maxRounding) bl = maxRounding;

    rectImpl(a, b, c, d, tl, tr, br, bl);
  }


  protected void rectImpl(float x1, float y1, float x2, float y2,
                          float tl, float tr, float br, float bl) {
    beginShape();
//    vertex(x1+tl, y1);
    if (tr != 0) {
      vertex(x2-tr, y1);
      quadraticVertex(x2, y1, x2, y1+tr);
    } else {
      vertex(x2, y1);
    }
    if (br != 0) {
      vertex(x2, y2-br);
      quadraticVertex(x2, y2, x2-br, y2);
    } else {
      vertex(x2, y2);
    }
    if (bl != 0) {
      vertex(x1+bl, y2);
      quadraticVertex(x1, y2, x1, y2-bl);
    } else {
      vertex(x1, y2);
    }
    if (tl != 0) {
      vertex(x1, y1+tl);
      quadraticVertex(x1, y1, x1+tl, y1);
    } else {
      vertex(x1, y1);
    }
//    endShape();
    endShape(CLOSE);
  }

  /**
   * ( begin auto-generated from square.xml )
   *
   * Draws a square to the screen. A square is a four-sided shape with 
   * every angle at ninety degrees and each side is the same length. 
   * By default, the first two parameters set the location of the 
   * upper-left corner, the third sets the width and height. The way 
   * these parameters are interpreted, however, may be changed with the 
   * <b>rectMode()</b> function.
   *
   * ( end auto-generated )
   *
   * @webref shape:2d_primitives
   * @param x x-coordinate of the rectangle by default
   * @param y y-coordinate of the rectangle by default
   * @param extent width and height of the rectangle by default
   * @see PGraphics#rect(float, float, float, float)
   * @see PGraphics#rectMode(int)
   */
  public void square(float x, float y, float extent) {
    rect(x, y, extent, extent);
  }



  //////////////////////////////////////////////////////////////

  // ELLIPSE AND ARC


  /**
   * ( begin auto-generated from ellipseMode.xml )
   *
   * The origin of the ellipse is modified by the <b>ellipseMode()</b>
   * function. The default configuration is <b>ellipseMode(CENTER)</b>, which
   * specifies the location of the ellipse as the center of the shape. The
   * <b>RADIUS</b> mode is the same, but the width and height parameters to
   * <b>ellipse()</b> specify the radius of the ellipse, rather than the
   * diameter. The <b>CORNER</b> mode draws the shape from the upper-left
   * corner of its bounding box. The <b>CORNERS</b> mode uses the four
   * parameters to <b>ellipse()</b> to set two opposing corners of the
   * ellipse's bounding box. The parameter must be written in ALL CAPS
   * because Processing is a case-sensitive language.
   *
   * ( end auto-generated )
   * @webref shape:attributes
   * @param mode either CENTER, RADIUS, CORNER, or CORNERS
   * @see PApplet#ellipse(float, float, float, float)
   * @see PApplet#arc(float, float, float, float, float, float)
   */
  public void ellipseMode(int mode) {
    ellipseMode = mode;
  }


  /**
   * ( begin auto-generated from ellipse.xml )
   *
   * Draws an ellipse (oval) in the display window. An ellipse with an equal
   * <b>width</b> and <b>height</b> is a circle. The first two parameters set
   * the location, the third sets the width, and the fourth sets the height.
   * The origin may be changed with the <b>ellipseMode()</b> function.
   *
   * ( end auto-generated )
   * @webref shape:2d_primitives
   * @param a x-coordinate of the ellipse
   * @param b y-coordinate of the ellipse
   * @param c width of the ellipse by default
   * @param d height of the ellipse by default
   * @see PApplet#ellipseMode(int)
   * @see PApplet#arc(float, float, float, float, float, float)
   */
  public void ellipse(float a, float b, float c, float d) {
    float x = a;
    float y = b;
    float w = c;
    float h = d;

    if (ellipseMode == CORNERS) {
      w = c - a;
      h = d - b;

    } else if (ellipseMode == RADIUS) {
      x = a - c;
      y = b - d;
      w = c * 2;
      h = d * 2;

    } else if (ellipseMode == DIAMETER) {
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
  }


  /**
   * ( begin auto-generated from arc.xml )
   *
   * Draws an arc in the display window. Arcs are drawn along the outer edge
   * of an ellipse defined by the <b>x</b>, <b>y</b>, <b>width</b> and
   * <b>height</b> parameters. The origin or the arc's ellipse may be changed
   * with the <b>ellipseMode()</b> function. The <b>start</b> and <b>stop</b>
   * parameters specify the angles at which to draw the arc.
   *
   * ( end auto-generated )
   * @webref shape:2d_primitives
   * @param a x-coordinate of the arc's ellipse
   * @param b y-coordinate of the arc's ellipse
   * @param c width of the arc's ellipse by default
   * @param d height of the arc's ellipse by default
   * @param start angle to start the arc, specified in radians
   * @param stop angle to stop the arc, specified in radians
   * @see PApplet#ellipse(float, float, float, float)
   * @see PApplet#ellipseMode(int)
   * @see PApplet#radians(float)
   * @see PApplet#degrees(float)
   */
  public void arc(float a, float b, float c, float d,
                  float start, float stop) {
    arc(a, b, c, d, start, stop, 0);
  }

  /*
   * @param mode either OPEN, CHORD, or PIE
   */
  public void arc(float a, float b, float c, float d,
                  float start, float stop, int mode) {
    float x = a;
    float y = b;
    float w = c;
    float h = d;

    if (ellipseMode == CORNERS) {
      w = c - a;
      h = d - b;

    } else if (ellipseMode == RADIUS) {
      x = a - c;
      y = b - d;
      w = c * 2;
      h = d * 2;

    } else if (ellipseMode == CENTER) {
      x = a - c/2f;
      y = b - d/2f;
    }

    // make sure the loop will exit before starting while
    if (!Float.isInfinite(start) && !Float.isInfinite(stop)) {
      // ignore equal and degenerate cases
      if (stop > start) {
        // make sure that we're starting at a useful point
        while (start < 0) {
          start += TWO_PI;
          stop += TWO_PI;
        }

        if (stop - start > TWO_PI) {
          // don't change start, it is visible in PIE mode
          stop = start + TWO_PI;
        }
        arcImpl(x, y, w, h, start, stop, mode);
      }
    }
  }


//  protected void arcImpl(float x, float y, float w, float h,
//                         float start, float stop) {
//  }


  /**
   * Start and stop are in radians, converted by the parent function.
   * Note that the radians can be greater (or less) than TWO_PI.
   * This is so that an arc can be drawn that crosses zero mark,
   * and the user will still collect $200.
   */
  protected void arcImpl(float x, float y, float w, float h,
                         float start, float stop, int mode) {
    showMissingWarning("arc");
  }

  /**
   * ( begin auto-generated from circle.xml )
   *
   * Draws a circle to the screen. By default, the first two parameters 
   * set the location of the center, and the third sets the shape's width 
   * and height. The origin may be changed with the <b>ellipseMode()</b> 
   * function.
   *
   * ( end auto-generated )
   * @webref shape:2d_primitives
   * @param x x-coordinate of the ellipse
   * @param y y-coordinate of the ellipse
   * @param extent width and height of the ellipse by default
   * @see PApplet#ellipse(float, float, float, float)
   * @see PApplet#ellipseMode(int)
   */
  public void circle(float x, float y, float extent) {
    ellipse(x, y, extent, extent);
  }


  //////////////////////////////////////////////////////////////

  // BOX

  /**
   * ( begin auto-generated from box.xml )
   *
   * A box is an extruded rectangle. A box with equal dimension on all sides
   * is a cube.
   *
   * ( end auto-generated )
   *
   * @webref shape:3d_primitives
   * @param size dimension of the box in all dimensions (creates a cube)
   * @see PGraphics#sphere(float)
   */
  public void box(float size) {
    box(size, size, size);
  }


  /**
   * @param w dimension of the box in the x-dimension
   * @param h dimension of the box in the y-dimension
   * @param d dimension of the box in the z-dimension
   */
  public void box(float w, float h, float d) {
    float x1 = -w/2f; float x2 = w/2f;
    float y1 = -h/2f; float y2 = h/2f;
    float z1 = -d/2f; float z2 = d/2f;

    // TODO not the least bit efficient, it even redraws lines
    // along the vertices. ugly ugly ugly!

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
  }



  //////////////////////////////////////////////////////////////

  // SPHERE

  /**
   * ( begin auto-generated from sphereDetail.xml )
   *
   * Controls the detail used to render a sphere by adjusting the number of
   * vertices of the sphere mesh. The default resolution is 30, which creates
   * a fairly detailed sphere definition with vertices every 360/30 = 12
   * degrees. If you're going to render a great number of spheres per frame,
   * it is advised to reduce the level of detail using this function. The
   * setting stays active until <b>sphereDetail()</b> is called again with a
   * new parameter and so should <i>not</i> be called prior to every
   * <b>sphere()</b> statement, unless you wish to render spheres with
   * different settings, e.g. using less detail for smaller spheres or ones
   * further away from the camera. To control the detail of the horizontal
   * and vertical resolution independently, use the version of the functions
   * with two parameters.
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   * Code for sphereDetail() submitted by toxi [031031].
   * Code for enhanced u/v version from davbol [080801].
   *
   * @param res number of segments (minimum 3) used per full circle revolution
   * @webref shape:3d_primitives
   * @see PGraphics#sphere(float)
   */
  public void sphereDetail(int res) {
    sphereDetail(res, res);
  }


  /**
   * @param ures number of segments used longitudinally per full circle revolutoin
   * @param vres number of segments used latitudinally from top to bottom
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
      float currY = cosLUT[(int) angle % SINCOS_LENGTH];
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
   * ( begin auto-generated from sphere.xml )
   *
   * A sphere is a hollow ball made from tessellated triangles.
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
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
   *
   * @webref shape:3d_primitives
   * @param r the radius of the sphere
   * @see PGraphics#sphereDetail(int)
   */
  public void sphere(float r) {
    if ((sphereDetailU < 3) || (sphereDetailV < 2)) {
      sphereDetail(30);
    }

    edge(false);


    // 1st ring from south pole
    beginShape(TRIANGLE_STRIP);
    for (int i = 0; i < sphereDetailU; i++) {
      normal(0, -1, 0);
      vertex(0, -r, 0);
      normal(sphereX[i], sphereY[i], sphereZ[i]);
      vertex(r * sphereX[i], r * sphereY[i], r * sphereZ[i]);
    }
    normal(0, -r, 0);
    vertex(0, -r, 0);
    normal(sphereX[0], sphereY[0], sphereZ[0]);
    vertex(r * sphereX[0], r * sphereY[0], r * sphereZ[0]);
    endShape();

    int v1,v11,v2;

    // middle rings
    int voff = 0;
    for (int i = 2; i < sphereDetailV; i++) {
      v1 = v11 = voff;
      voff += sphereDetailU;
      v2 = voff;
      beginShape(TRIANGLE_STRIP);
      for (int j = 0; j < sphereDetailU; j++) {
        normal(sphereX[v1], sphereY[v1], sphereZ[v1]);
        vertex(r * sphereX[v1], r * sphereY[v1], r * sphereZ[v1++]);
        normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
        vertex(r * sphereX[v2], r * sphereY[v2], r * sphereZ[v2++]);
      }
      // close each ring
      v1 = v11;
      v2 = voff;
      normal(sphereX[v1], sphereY[v1], sphereZ[v1]);
      vertex(r * sphereX[v1], r * sphereY[v1], r * sphereZ[v1]);
      normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
      vertex(r * sphereX[v2], r * sphereY[v2], r * sphereZ[v2]);
      endShape();
    }

    // add the northern cap
    beginShape(TRIANGLE_STRIP);
    for (int i = 0; i < sphereDetailU; i++) {
      v2 = voff + i;
      normal(sphereX[v2], sphereY[v2], sphereZ[v2]);
      vertex(r * sphereX[v2], r * sphereY[v2], r * sphereZ[v2]);
      normal(0, 1, 0);
      vertex(0, r, 0);
    }
    normal(sphereX[voff], sphereY[voff], sphereZ[voff]);
    vertex(r * sphereX[voff], r * sphereY[voff], r * sphereZ[voff]);
    normal(0, 1, 0);
    vertex(0, r, 0);
    endShape();

    edge(true);
  }



  //////////////////////////////////////////////////////////////

  // BEZIER

  /**
   * ( begin auto-generated from bezierPoint.xml )
   *
   * Evaluates the Bezier at point t for points a, b, c, d. The parameter t
   * varies between 0 and 1, a and d are points on the curve, and b and c are
   * the control points. This can be done once with the x coordinates and a
   * second time with the y coordinates to get the location of a bezier curve
   * at t.
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
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
   *
   * @webref shape:curves
   * @param a coordinate of first point on the curve
   * @param b coordinate of first control point
   * @param c coordinate of second control point
   * @param d coordinate of second point on the curve
   * @param t value between 0 and 1
   * @see PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
   * @see PGraphics#bezierVertex(float, float, float, float, float, float)
   * @see PGraphics#curvePoint(float, float, float, float, float)
   */
  public float bezierPoint(float a, float b, float c, float d, float t) {
    float t1 = 1.0f - t;
    return (a*t1 + 3*b*t)*t1*t1 + (3*c*t1 + d*t)*t*t;
  }
  /**
   * ( begin auto-generated from bezierTangent.xml )
   *
   * Calculates the tangent of a point on a Bezier curve. There is a good
   * definition of <a href="http://en.wikipedia.org/wiki/Tangent"
   * target="new"><em>tangent</em> on Wikipedia</a>.
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   * Code submitted by Dave Bollinger (davol) for release 0136.
   *
   * @webref shape:curves
   * @param a coordinate of first point on the curve
   * @param b coordinate of first control point
   * @param c coordinate of second control point
   * @param d coordinate of second point on the curve
   * @param t value between 0 and 1
   * @see PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
   * @see PGraphics#bezierVertex(float, float, float, float, float, float)
   * @see PGraphics#curvePoint(float, float, float, float, float)
   */
  public float bezierTangent(float a, float b, float c, float d, float t) {
    return (3*t*t * (-a+3*b-3*c+d) +
            6*t * (a-2*b+c) +
            3 * (-a+b));
  }


  protected void bezierInitCheck() {
    if (!bezierInited) {
      bezierInit();
    }
  }


  protected void bezierInit() {
    // overkill to be broken out, but better parity with the curve stuff below
    bezierDetail(bezierDetail);
    bezierInited = true;
  }


  /**
   * ( begin auto-generated from bezierDetail.xml )
   *
   * Sets the resolution at which Beziers display. The default value is 20.
   * This function is only useful when using the P3D renderer as the default
   * P2D renderer does not use this information.
   *
   * ( end auto-generated )
   *
   * @webref shape:curves
   * @param detail resolution of the curves
   * @see PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
   * @see PGraphics#curveVertex(float, float, float)
   * @see PGraphics#curveTightness(float)
   */
  public void bezierDetail(int detail) {
    bezierDetail = detail;

    if (bezierDrawMatrix == null) {
      bezierDrawMatrix = new PMatrix3D();
    }

    // setup matrix for forward differencing to speed up drawing
    splineForward(detail, bezierDrawMatrix);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    //mult_spline_matrix(bezierForwardMatrix, bezier_basis, bezierDrawMatrix, 4);
    //bezierDrawMatrix.set(bezierForwardMatrix);
    bezierDrawMatrix.apply(bezierBasisMatrix);
  }



  public void bezier(float x1, float y1,
                     float x2, float y2,
                     float x3, float y3,
                     float x4, float y4) {
    beginShape();
    vertex(x1, y1);
    bezierVertex(x2, y2, x3, y3, x4, y4);
    endShape();
  }

  /**
   * ( begin auto-generated from bezier.xml )
   *
   * Draws a Bezier curve on the screen. These curves are defined by a series
   * of anchor and control points. The first two parameters specify the first
   * anchor point and the last two parameters specify the other anchor point.
   * The middle parameters specify the control points which define the shape
   * of the curve. Bezier curves were developed by French engineer Pierre
   * Bezier. Using the 3D version requires rendering with P3D (see the
   * Environment reference for more information).
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   * Draw a cubic bezier curve. The first and last points are
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
   * To draw a quadratic (instead of cubic) curve,
   * use the control point twice by doubling it:
   * <PRE>bezier(x1, y1, cx, cy, cx, cy, x2, y2);</PRE>
   *
   * @webref shape:curves
   * @param x1 coordinates for the first anchor point
   * @param y1 coordinates for the first anchor point
   * @param z1 coordinates for the first anchor point
   * @param x2 coordinates for the first control point
   * @param y2 coordinates for the first control point
   * @param z2 coordinates for the first control point
   * @param x3 coordinates for the second control point
   * @param y3 coordinates for the second control point
   * @param z3 coordinates for the second control point
   * @param x4 coordinates for the second anchor point
   * @param y4 coordinates for the second anchor point
   * @param z4 coordinates for the second anchor point
   *
   * @see PGraphics#bezierVertex(float, float, float, float, float, float)
   * @see PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
   */
  public void bezier(float x1, float y1, float z1,
                     float x2, float y2, float z2,
                     float x3, float y3, float z3,
                     float x4, float y4, float z4) {
    beginShape();
    vertex(x1, y1, z1);
    bezierVertex(x2, y2, z2,
                 x3, y3, z3,
                 x4, y4, z4);
    endShape();
  }



  //////////////////////////////////////////////////////////////

  // CATMULL-ROM CURVE

  /**
   * ( begin auto-generated from curvePoint.xml )
   *
   * Evalutes the curve at point t for points a, b, c, d. The parameter t
   * varies between 0 and 1, a and d are points on the curve, and b and c are
   * the control points. This can be done once with the x coordinates and a
   * second time with the y coordinates to get the location of a curve at t.
   *
   * ( end auto-generated )
   *
   * @webref shape:curves
   * @param a coordinate of first point on the curve
   * @param b coordinate of second point on the curve
   * @param c coordinate of third point on the curve
   * @param d coordinate of fourth point on the curve
   * @param t value between 0 and 1
   * @see PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
   * @see PGraphics#curveVertex(float, float)
   * @see PGraphics#bezierPoint(float, float, float, float, float)
   */
  public float curvePoint(float a, float b, float c, float d, float t) {
    curveInitCheck();

    float tt = t * t;
    float ttt = t * tt;
    PMatrix3D cb = curveBasisMatrix;

    // not optimized (and probably need not be)
    return (a * (ttt*cb.m00 + tt*cb.m10 + t*cb.m20 + cb.m30) +
            b * (ttt*cb.m01 + tt*cb.m11 + t*cb.m21 + cb.m31) +
            c * (ttt*cb.m02 + tt*cb.m12 + t*cb.m22 + cb.m32) +
            d * (ttt*cb.m03 + tt*cb.m13 + t*cb.m23 + cb.m33));
  }


  /**
   * ( begin auto-generated from curveTangent.xml )
   *
   * Calculates the tangent of a point on a curve. There's a good definition
   * of <em><a href="http://en.wikipedia.org/wiki/Tangent"
   * target="new">tangent</em> on Wikipedia</a>.
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   * Code thanks to Dave Bollinger (Bug #715)
   *
   * @webref shape:curves
   * @param a coordinate of first point on the curve
   * @param b coordinate of first control point
   * @param c coordinate of second control point
   * @param d coordinate of second point on the curve
   * @param t value between 0 and 1
   * @see PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
   * @see PGraphics#curveVertex(float, float)
   * @see PGraphics#curvePoint(float, float, float, float, float)
   * @see PGraphics#bezierTangent(float, float, float, float, float)
   */
  public float curveTangent(float a, float b, float c, float d, float t) {
    curveInitCheck();

    float tt3 = t * t * 3;
    float t2 = t * 2;
    PMatrix3D cb = curveBasisMatrix;

    // not optimized (and probably need not be)
    return (a * (tt3*cb.m00 + t2*cb.m10 + cb.m20) +
            b * (tt3*cb.m01 + t2*cb.m11 + cb.m21) +
            c * (tt3*cb.m02 + t2*cb.m12 + cb.m22) +
            d * (tt3*cb.m03 + t2*cb.m13 + cb.m23) );
  }


  /**
   * ( begin auto-generated from curveDetail.xml )
   *
   * Sets the resolution at which curves display. The default value is 20.
   * This function is only useful when using the P3D renderer as the default
   * P2D renderer does not use this information.
   *
   * ( end auto-generated )
   *
   * @webref shape:curves
   * @param detail resolution of the curves
   * @see PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
   * @see PGraphics#curveVertex(float, float)
   * @see PGraphics#curveTightness(float)
   */
  public void curveDetail(int detail) {
    curveDetail = detail;
    curveInit();
  }


  /**
   * ( begin auto-generated from curveTightness.xml )
   *
   * Modifies the quality of forms created with <b>curve()</b> and
   * <b>curveVertex()</b>. The parameter <b>squishy</b> determines how the
   * curve fits to the vertex points. The value 0.0 is the default value for
   * <b>squishy</b> (this value defines the curves to be Catmull-Rom splines)
   * and the value 1.0 connects all the points with straight lines. Values
   * within the range -5.0 and 5.0 will deform the curves but will leave them
   * recognizable and as values increase in magnitude, they will continue to deform.
   *
   * ( end auto-generated )
   *
   * @webref shape:curves
   * @param tightness amount of deformation from the original vertices
   * @see PGraphics#curve(float, float, float, float, float, float, float, float, float, float, float, float)
   * @see PGraphics#curveVertex(float, float)
   */
  public void curveTightness(float tightness) {
    curveTightness = tightness;
    curveInit();
  }


  protected void curveInitCheck() {
    if (!curveInited) {
      curveInit();
    }
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
  protected void curveInit() {
    // allocate only if/when used to save startup time
    if (curveDrawMatrix == null) {
      curveBasisMatrix = new PMatrix3D();
      curveDrawMatrix = new PMatrix3D();
      curveInited = true;
    }

    float s = curveTightness;
    curveBasisMatrix.set((s-1)/2f, (s+3)/2f,  (-3-s)/2f, (1-s)/2f,
                         (1-s),    (-5-s)/2f, (s+2),     (s-1)/2f,
                         (s-1)/2f, 0,         (1-s)/2f,  0,
                         0,        1,         0,         0);

    //setup_spline_forward(segments, curveForwardMatrix);
    splineForward(curveDetail, curveDrawMatrix);

    if (bezierBasisInverse == null) {
      bezierBasisInverse = bezierBasisMatrix.get();
      bezierBasisInverse.invert();
      curveToBezierMatrix = new PMatrix3D();
    }

    // TODO only needed for PGraphicsJava2D? if so, move it there
    // actually, it's generally useful for other renderers, so keep it
    // or hide the implementation elsewhere.
    curveToBezierMatrix.set(curveBasisMatrix);
    curveToBezierMatrix.preApply(bezierBasisInverse);

    // multiply the basis and forward diff matrices together
    // saves much time since this needn't be done for each curve
    curveDrawMatrix.apply(curveBasisMatrix);
  }


  /**
   * ( begin auto-generated from curve.xml )
   *
   * Draws a curved line on the screen. The first and second parameters
   * specify the beginning control point and the last two parameters specify
   * the ending control point. The middle parameters specify the start and
   * stop of the curve. Longer curves can be created by putting a series of
   * <b>curve()</b> functions together or using <b>curveVertex()</b>. An
   * additional function called <b>curveTightness()</b> provides control for
   * the visual quality of the curve. The <b>curve()</b> function is an
   * implementation of Catmull-Rom splines. Using the 3D version requires
   * rendering with P3D (see the Environment reference for more information).
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   * As of revision 0070, this function no longer doubles the first
   * and last points. The curves are a bit more boring, but it's more
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
   *
   * @webref shape:curves
   * @param x1 coordinates for the beginning control point
   * @param y1 coordinates for the beginning control point
   * @param x2 coordinates for the first point
   * @param y2 coordinates for the first point
   * @param x3 coordinates for the second point
   * @param y3 coordinates for the second point
   * @param x4 coordinates for the ending control point
   * @param y4 coordinates for the ending control point
   * @see PGraphics#curveVertex(float, float)
   * @see PGraphics#curveTightness(float)
   * @see PGraphics#bezier(float, float, float, float, float, float, float, float, float, float, float, float)
   */
  public void curve(float x1, float y1,
                    float x2, float y2,
                    float x3, float y3,
                    float x4, float y4) {
    beginShape();
    curveVertex(x1, y1);
    curveVertex(x2, y2);
    curveVertex(x3, y3);
    curveVertex(x4, y4);
    endShape();
  }

   /**
    * @param z1 coordinates for the beginning control point
    * @param z2 coordinates for the first point
    * @param z3 coordinates for the second point
    * @param z4 coordinates for the ending control point
    */
  public void curve(float x1, float y1, float z1,
                    float x2, float y2, float z2,
                    float x3, float y3, float z3,
                    float x4, float y4, float z4) {
    beginShape();
    curveVertex(x1, y1, z1);
    curveVertex(x2, y2, z2);
    curveVertex(x3, y3, z3);
    curveVertex(x4, y4, z4);
    endShape();
  }



  //////////////////////////////////////////////////////////////

  // SPLINE UTILITY FUNCTIONS (used by both Bezier and Catmull-Rom)


  /**
   * Setup forward-differencing matrix to be used for speedy
   * curve rendering. It's based on using a specific number
   * of curve segments and just doing incremental adds for each
   * vertex of the segment, rather than running the mathematically
   * expensive cubic equation.
   * @param segments number of curve segments to use when drawing
   * @param matrix target object for the new matrix
   */
  protected void splineForward(int segments, PMatrix3D matrix) {
    float f  = 1.0f / segments;
    float ff = f * f;
    float fff = ff * f;

    matrix.set(0,     0,    0, 1,
               fff,   ff,   f, 0,
               6*fff, 2*ff, 0, 0,
               6*fff, 0,    0, 0);
  }



  //////////////////////////////////////////////////////////////

  // SMOOTHING


  public void smooth() {  // ignore
    smooth(1);
  }


  public void smooth(int quality) {  // ignore
    if (primaryGraphics) {
      parent.smooth(quality);
    } else {
      // for createGraphics(), make sure beginDraw() not called yet
      if (settingsInited) {
        // ignore if it's just a repeat of the current state
        if (this.smooth != quality) {
          smoothWarning("smooth");
        }
      } else {
        this.smooth = quality;
      }
    }
  }


  public void noSmooth() {  // ignore
    smooth(0);
  }


  private void smoothWarning(String method) {
    PGraphics.showWarning("%s() can only be used before beginDraw()", method);
  }



  //////////////////////////////////////////////////////////////

  // IMAGE


  /**
   * ( begin auto-generated from imageMode.xml )
   *
   * Modifies the location from which images draw. The default mode is
   * <b>imageMode(CORNER)</b>, which specifies the location to be the upper
   * left corner and uses the fourth and fifth parameters of <b>image()</b>
   * to set the image's width and height. The syntax
   * <b>imageMode(CORNERS)</b> uses the second and third parameters of
   * <b>image()</b> to set the location of one corner of the image and uses
   * the fourth and fifth parameters to set the opposite corner. Use
   * <b>imageMode(CENTER)</b> to draw images centered at the given x and y
   * position.<br />
   * <br />
   * The parameter to <b>imageMode()</b> must be written in ALL CAPS because
   * Processing is a case-sensitive language.
   *
   * ( end auto-generated )
   *
   * @webref image:loading_displaying
   * @param mode either CORNER, CORNERS, or CENTER
   * @see PApplet#loadImage(String, String)
   * @see PImage
   * @see PGraphics#image(PImage, float, float, float, float)
   * @see PGraphics#background(float, float, float, float)
   */
  public void imageMode(int mode) {
    if ((mode == CORNER) || (mode == CORNERS) || (mode == CENTER)) {
      imageMode = mode;
    } else {
      String msg =
        "imageMode() only works with CORNER, CORNERS, or CENTER";
      throw new RuntimeException(msg);
    }
  }


  /**
   * ( begin auto-generated from image.xml )
   *
   * Displays images to the screen. The images must be in the sketch's "data"
   * directory to load correctly. Select "Add file..." from the "Sketch" menu
   * to add the image. Processing currently works with GIF, JPEG, and Targa
   * images. The <b>img</b> parameter specifies the image to display and the
   * <b>x</b> and <b>y</b> parameters define the location of the image from
   * its upper-left corner. The image is displayed at its original size
   * unless the <b>width</b> and <b>height</b> parameters specify a different
   * size.<br />
   * <br />
   * The <b>imageMode()</b> function changes the way the parameters work. For
   * example, a call to <b>imageMode(CORNERS)</b> will change the
   * <b>width</b> and <b>height</b> parameters to define the x and y values
   * of the opposite corner of the image.<br />
   * <br />
   * The color of an image may be modified with the <b>tint()</b> function.
   * This function will maintain transparency for GIF and PNG images.
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   * Starting with release 0124, when using the default (JAVA2D) renderer,
   * smooth() will also improve image quality of resized images.
   *
   * @webref image:loading_displaying
   * @param img the image to display
   * @param a x-coordinate of the image by default
   * @param b y-coordinate of the image by default
   * @see PApplet#loadImage(String, String)
   * @see PImage
   * @see PGraphics#imageMode(int)
   * @see PGraphics#tint(float)
   * @see PGraphics#background(float, float, float, float)
   * @see PGraphics#alpha(int)
   */
  public void image(PImage img, float a, float b) {
    // Starting in release 0144, image errors are simply ignored.
    // loadImageAsync() sets width and height to -1 when loading fails.
    if (img.width == -1 || img.height == -1) return;

    if (imageMode == CORNER || imageMode == CORNERS) {
      imageImpl(img,
                a, b, a+img.width, b+img.height,
                0, 0, img.width, img.height);

    } else if (imageMode == CENTER) {
      float x1 = a - img.width/2;
      float y1 = b - img.height/2;
      imageImpl(img,
                x1, y1, x1+img.width, y1+img.height,
                0, 0, img.width, img.height);
    }
  }

  /**
   * @param c width to display the image by default
   * @param d height to display the image by default
   */
  public void image(PImage img, float a, float b, float c, float d) {
    image(img, a, b, c, d, 0, 0, img.width, img.height);
  }


  /**
   * Draw an image(), also specifying u/v coordinates.
   * In this method, the  u, v coordinates are always based on image space
   * location, regardless of the current textureMode().
   *
   * @nowebref
   */
  public void image(PImage img,
                    float a, float b, float c, float d,
                    int u1, int v1, int u2, int v2) {
    // Starting in release 0144, image errors are simply ignored.
    // loadImageAsync() sets width and height to -1 when loading fails.
    if (img.width == -1 || img.height == -1) return;

    if (imageMode == CORNER) {
      if (c < 0) {  // reset a negative width
        a += c; c = -c;
      }
      if (d < 0) {  // reset a negative height
        b += d; d = -d;
      }

      imageImpl(img,
                a, b, a + c, b + d,
                u1, v1, u2, v2);

    } else if (imageMode == CORNERS) {
      if (c < a) {  // reverse because x2 < x1
        float temp = a; a = c; c = temp;
      }
      if (d < b) {  // reverse because y2 < y1
        float temp = b; b = d; d = temp;
      }

      imageImpl(img,
                a, b, c, d,
                u1, v1, u2, v2);

    } else if (imageMode == CENTER) {
      // c and d are width/height
      if (c < 0) c = -c;
      if (d < 0) d = -d;
      float x1 = a - c/2;
      float y1 = b - d/2;

      imageImpl(img,
                x1, y1, x1 + c, y1 + d,
                u1, v1, u2, v2);
    }
  }


  /**
   * Expects x1, y1, x2, y2 coordinates where (x2 >= x1) and (y2 >= y1).
   * If tint() has been called, the image will be colored.
   * <p/>
   * The default implementation draws an image as a textured quad.
   * The (u, v) coordinates are in image space (they're ints, after all..)
   */
  protected void imageImpl(PImage img,
                           float x1, float y1, float x2, float y2,
                           int u1, int v1, int u2, int v2) {
    boolean savedStroke = stroke;
//    boolean savedFill = fill;
    int savedTextureMode = textureMode;

    stroke = false;
//    fill = true;
    textureMode = IMAGE;

//    float savedFillR = fillR;
//    float savedFillG = fillG;
//    float savedFillB = fillB;
//    float savedFillA = fillA;
//
//    if (tint) {
//      fillR = tintR;
//      fillG = tintG;
//      fillB = tintB;
//      fillA = tintA;
//
//    } else {
//      fillR = 1;
//      fillG = 1;
//      fillB = 1;
//      fillA = 1;
//    }

    u1 *= img.pixelDensity;
    u2 *= img.pixelDensity;
    v1 *= img.pixelDensity;
    v2 *= img.pixelDensity;

    beginShape(QUADS);
    texture(img);
    vertex(x1, y1, u1, v1);
    vertex(x1, y2, u1, v2);
    vertex(x2, y2, u2, v2);
    vertex(x2, y1, u2, v1);
    endShape();

    stroke = savedStroke;
//    fill = savedFill;
    textureMode = savedTextureMode;

//    fillR = savedFillR;
//    fillG = savedFillG;
//    fillB = savedFillB;
//    fillA = savedFillA;
  }



  //////////////////////////////////////////////////////////////

  // SHAPE


  /**
   * ( begin auto-generated from shapeMode.xml )
   *
   * Modifies the location from which shapes draw. The default mode is
   * <b>shapeMode(CORNER)</b>, which specifies the location to be the upper
   * left corner of the shape and uses the third and fourth parameters of
   * <b>shape()</b> to specify the width and height. The syntax
   * <b>shapeMode(CORNERS)</b> uses the first and second parameters of
   * <b>shape()</b> to set the location of one corner and uses the third and
   * fourth parameters to set the opposite corner. The syntax
   * <b>shapeMode(CENTER)</b> draws the shape from its center point and uses
   * the third and forth parameters of <b>shape()</b> to specify the width
   * and height. The parameter must be written in "ALL CAPS" because
   * Processing is a case sensitive language.
   *
   * ( end auto-generated )
   *
   * @webref shape:loading_displaying
   * @param mode either CORNER, CORNERS, CENTER
   * @see PShape
   * @see PGraphics#shape(PShape)
   * @see PGraphics#rectMode(int)
   */
  public void shapeMode(int mode) {
    this.shapeMode = mode;
  }


  public void shape(PShape shape) {
    if (shape.isVisible()) {  // don't do expensive matrix ops if invisible
      // Flushing any remaining geometry generated in the immediate mode
      // to avoid depth-sorting issues.
      flush();

      if (shapeMode == CENTER) {
        pushMatrix();
        translate(-shape.getWidth()/2, -shape.getHeight()/2);
      }

      shape.draw(this); // needs to handle recorder too

      if (shapeMode == CENTER) {
        popMatrix();
      }
    }
  }


  /**
   * ( begin auto-generated from shape.xml )
   *
   * Displays shapes to the screen. The shapes must be in the sketch's "data"
   * directory to load correctly. Select "Add file..." from the "Sketch" menu
   * to add the shape. Processing currently works with SVG shapes only. The
   * <b>sh</b> parameter specifies the shape to display and the <b>x</b> and
   * <b>y</b> parameters define the location of the shape from its upper-left
   * corner. The shape is displayed at its original size unless the
   * <b>width</b> and <b>height</b> parameters specify a different size. The
   * <b>shapeMode()</b> function changes the way the parameters work. A call
   * to <b>shapeMode(CORNERS)</b>, for example, will change the width and
   * height parameters to define the x and y values of the opposite corner of
   * the shape.
   * <br /><br />
   * Note complex shapes may draw awkwardly with P3D. This renderer does not
   * yet support shapes that have holes or complicated breaks.
   *
   * ( end auto-generated )
   *
   * @webref shape:loading_displaying
   * @param shape the shape to display
   * @param x x-coordinate of the shape
   * @param y y-coordinate of the shape
   * @see PShape
   * @see PApplet#loadShape(String)
   * @see PGraphics#shapeMode(int)
   *
   * Convenience method to draw at a particular location.
   */
  public void shape(PShape shape, float x, float y) {
    if (shape.isVisible()) {  // don't do expensive matrix ops if invisible
      flush();

      pushMatrix();

      if (shapeMode == CENTER) {
        translate(x - shape.getWidth()/2, y - shape.getHeight()/2);

      } else if ((shapeMode == CORNER) || (shapeMode == CORNERS)) {
        translate(x, y);
      }
      shape.draw(this);

      popMatrix();
    }
  }


  // TODO unapproved
  protected void shape(PShape shape, float x, float y, float z) {
    showMissingWarning("shape");
  }


  /**
   * @param a x-coordinate of the shape
   * @param b y-coordinate of the shape
   * @param c width to display the shape
   * @param d height to display the shape
   */
  public void shape(PShape shape, float a, float b, float c, float d) {
    if (shape.isVisible()) {  // don't do expensive matrix ops if invisible
      flush();

      pushMatrix();

      if (shapeMode == CENTER) {
        // x and y are center, c and d refer to a diameter
        translate(a - c/2f, b - d/2f);
        scale(c / shape.getWidth(), d / shape.getHeight());

      } else if (shapeMode == CORNER) {
        translate(a, b);
        scale(c / shape.getWidth(), d / shape.getHeight());

      } else if (shapeMode == CORNERS) {
        // c and d are x2/y2, make them into width/height
        c -= a;
        d -= b;
        // then same as above
        translate(a, b);
        scale(c / shape.getWidth(), d / shape.getHeight());
      }
      shape.draw(this);

      popMatrix();
    }
  }


  // TODO unapproved
  protected void shape(PShape shape, float x, float y, float z, float c, float d, float e) {
    showMissingWarning("shape");
  }



  //////////////////////////////////////////////////////////////

  // TEXT/FONTS


  /**
   * Used by PGraphics to remove the requirement for loading a font.
   */
  protected PFont createDefaultFont(float size) {
    Font baseFont = new Font("Lucida Sans", Font.PLAIN, 1);
    return createFont(baseFont, size, true, null, false);
  }


  protected PFont createFont(String name, float size,
                             boolean smooth, char[] charset) {
    String lowerName = name.toLowerCase();
    Font baseFont = null;

    try {
      InputStream stream = null;
      if (lowerName.endsWith(".otf") || lowerName.endsWith(".ttf")) {
        stream = parent.createInput(name);
        if (stream == null) {
          System.err.println("The font \"" + name + "\" " +
                             "is missing or inaccessible, make sure " +
                             "the URL is valid or that the file has been " +
                             "added to your sketch and is readable.");
          return null;
        }
        baseFont = Font.createFont(Font.TRUETYPE_FONT, parent.createInput(name));

      } else {
        baseFont = PFont.findFont(name);
      }
      return createFont(baseFont, size, smooth, charset, stream != null);

    } catch (Exception e) {
      System.err.println("Problem with createFont(\"" + name + "\")");
      e.printStackTrace();
      return null;
    }
  }


  private PFont createFont(Font baseFont, float size,
                           boolean smooth, char[] charset, boolean stream) {
    return new PFont(baseFont.deriveFont(size * parent.pixelDensity),
                     smooth, charset, stream,
                     parent.pixelDensity);
  }


  public void textAlign(int alignX) {
    textAlign(alignX, BASELINE);
  }


  /**
   * ( begin auto-generated from textAlign.xml )
   *
   * Sets the current alignment for drawing text. The parameters LEFT,
   * CENTER, and RIGHT set the display characteristics of the letters in
   * relation to the values for the <b>x</b> and <b>y</b> parameters of the
   * <b>text()</b> function.
   * <br/> <br/>
   * In Processing 0125 and later, an optional second parameter can be used
   * to vertically align the text. BASELINE is the default, and the vertical
   * alignment will be reset to BASELINE if the second parameter is not used.
   * The TOP and CENTER parameters are straightforward. The BOTTOM parameter
   * offsets the line based on the current <b>textDescent()</b>. For multiple
   * lines, the final line will be aligned to the bottom, with the previous
   * lines appearing above it.
   * <br/> <br/>
   * When using <b>text()</b> with width and height parameters, BASELINE is
   * ignored, and treated as TOP. (Otherwise, text would by default draw
   * outside the box, since BASELINE is the default setting. BASELINE is not
   * a useful drawing mode for text drawn in a rectangle.)
   * <br/> <br/>
   * The vertical alignment is based on the value of <b>textAscent()</b>,
   * which many fonts do not specify correctly. It may be necessary to use a
   * hack and offset by a few pixels by hand so that the offset looks
   * correct. To do this as less of a hack, use some percentage of
   * <b>textAscent()</b> or <b>textDescent()</b> so that the hack works even
   * if you change the size of the font.
   *
   * ( end auto-generated )
   *
   * @webref typography:attributes
   * @param alignX horizontal alignment, either LEFT, CENTER, or RIGHT
   * @param alignY vertical alignment, either TOP, BOTTOM, CENTER, or BASELINE
   * @see PApplet#loadFont(String)
   * @see PFont
   * @see PGraphics#text(String, float, float)
   * @see PGraphics#textSize(float)
   * @see PGraphics#textAscent()
   * @see PGraphics#textDescent()
   */
  public void textAlign(int alignX, int alignY) {
    textAlign = alignX;
    textAlignY = alignY;
  }


  /**
   * ( begin auto-generated from textAscent.xml )
   *
   * Returns ascent of the current font at its current size. This information
   * is useful for determining the height of the font above the baseline. For
   * example, adding the <b>textAscent()</b> and <b>textDescent()</b> values
   * will give you the total height of the line.
   *
   * ( end auto-generated )
   *
   * @webref typography:metrics
   * @see PGraphics#textDescent()
   */
  public float textAscent() {
    if (textFont == null) {
      defaultFontOrDeath("textAscent");
    }
    return textFont.ascent() * textSize;
  }


  /**
   * ( begin auto-generated from textDescent.xml )
   *
   * Returns descent of the current font at its current size. This
   * information is useful for determining the height of the font below the
   * baseline. For example, adding the <b>textAscent()</b> and
   * <b>textDescent()</b> values will give you the total height of the line.
   *
   * ( end auto-generated )
   *
   * @webref typography:metrics
   * @see PGraphics#textAscent()
   */
  public float textDescent() {
    if (textFont == null) {
      defaultFontOrDeath("textDescent");
    }
    return textFont.descent() * textSize;
  }


  /**
   * ( begin auto-generated from textFont.xml )
   *
   * Sets the current font that will be drawn with the <b>text()</b>
   * function. Fonts must be loaded with <b>loadFont()</b> before it can be
   * used. This font will be used in all subsequent calls to the
   * <b>text()</b> function. If no <b>size</b> parameter is input, the font
   * will appear at its original size (the size it was created at with the
   * "Create Font..." tool) until it is changed with <b>textSize()</b>. <br
   * /> <br /> Because fonts are usually bitmaped, you should create fonts at
   * the sizes that will be used most commonly. Using <b>textFont()</b>
   * without the size parameter will result in the cleanest-looking text. <br
   * /><br /> With the default (JAVA2D) and PDF renderers, it's also possible
   * to enable the use of native fonts via the command
   * <b>hint(ENABLE_NATIVE_FONTS)</b>. This will produce vector text in
   * JAVA2D sketches and PDF output in cases where the vector data is
   * available: when the font is still installed, or the font is created via
   * the <b>createFont()</b> function (rather than the Create Font tool).
   *
   * ( end auto-generated )
   *
   * @webref typography:loading_displaying
   * @param which any variable of the type PFont
   * @see PApplet#createFont(String, float, boolean)
   * @see PApplet#loadFont(String)
   * @see PFont
   * @see PGraphics#text(String, float, float)
   * @see PGraphics#textSize(float)
   */
  public void textFont(PFont which) {
    if (which == null) {
      throw new RuntimeException(ERROR_TEXTFONT_NULL_PFONT);
    }
    textFontImpl(which, which.getDefaultSize());
  }


  /**
   * @param size the size of the letters in units of pixels
   */
  public void textFont(PFont which, float size) {
    if (which == null) {
      throw new RuntimeException(ERROR_TEXTFONT_NULL_PFONT);
    }
    // https://github.com/processing/processing/issues/3110
    if (size <= 0) {
      // Using System.err instead of showWarning to avoid running out of
      // memory with a bunch of textSize() variants (cause of this bug is
      // usually something done with map() or in a loop).
      System.err.println("textFont: ignoring size " + size + " px:" +
                             "the text size must be larger than zero");
      size = textSize;
    }
    textFontImpl(which, size);
  }


  /**
   * Called from textFont. Check the validity of args and
   * print possible errors to the user before calling this.
   * Subclasses will want to override this one.
   *
   * @param which font to set, not null
   * @param size size to set, greater than zero
   */
  protected void textFontImpl(PFont which, float size) {
    textFont = which;
//      if (hints[ENABLE_NATIVE_FONTS]) {
//        //if (which.font == null) {
//        which.findNative();
//        //}
//      }
      /*
      textFontNative = which.font;

      //textFontNativeMetrics = null;
      // changed for rev 0104 for textMode(SHAPE) in opengl
      if (textFontNative != null) {
        // TODO need a better way to handle this. could use reflection to get
        // rid of the warning, but that'd be a little silly. supporting this is
        // an artifact of supporting java 1.1, otherwise we'd use getLineMetrics,
        // as recommended by the @deprecated flag.
        textFontNativeMetrics =
          Toolkit.getDefaultToolkit().getFontMetrics(textFontNative);
        // The following is what needs to be done, however we need to be able
        // to get the actual graphics context where the drawing is happening.
        // For instance, parent.getGraphics() doesn't work for OpenGL since
        // an OpenGL drawing surface is an embedded component.
//        if (parent != null) {
//          textFontNativeMetrics = parent.getGraphics().getFontMetrics(textFontNative);
//        }

        // float w = font.getStringBounds(text, g2.getFontRenderContext()).getWidth();
      }
      */

    handleTextSize(size);
  }


  /**
   * ( begin auto-generated from textLeading.xml )
   *
   * Sets the spacing between lines of text in units of pixels. This setting
   * will be used in all subsequent calls to the <b>text()</b> function.
   *
   * ( end auto-generated )
   *
   * @webref typography:attributes
   * @param leading the size in pixels for spacing between lines
   * @see PApplet#loadFont(String)
   * @see PFont#PFont
   * @see PGraphics#text(String, float, float)
   * @see PGraphics#textFont(PFont)
   * @see PGraphics#textSize(float)
   */
  public void textLeading(float leading) {
    textLeading = leading;
  }


  /**
   * ( begin auto-generated from textMode.xml )
   *
   * Sets the way text draws to the screen. In the default configuration, the
   * <b>MODEL</b> mode, it's possible to rotate, scale, and place letters in
   * two and three dimensional space.<br />
   * <br />
   * The <b>SHAPE</b> mode draws text using the the glyph outlines of
   * individual characters rather than as textures. This mode is only
   * supported with the <b>PDF</b> and <b>P3D</b> renderer settings. With the
   * <b>PDF</b> renderer, you must call <b>textMode(SHAPE)</b> before any
   * other drawing occurs. If the outlines are not available, then
   * <b>textMode(SHAPE)</b> will be ignored and <b>textMode(MODEL)</b> will
   * be used instead.<br />
   * <br />
   * The <b>textMode(SHAPE)</b> option in <b>P3D</b> can be combined with
   * <b>beginRaw()</b> to write vector-accurate text to 2D and 3D output
   * files, for instance <b>DXF</b> or <b>PDF</b>. The <b>SHAPE</b> mode is
   * not currently optimized for <b>P3D</b>, so if recording shape data, use
   * <b>textMode(MODEL)</b> until you're ready to capture the geometry with <b>beginRaw()</b>.
   *
   * ( end auto-generated )
   *
   * @webref typography:attributes
   * @param mode either MODEL or SHAPE
   * @see PApplet#loadFont(String)
   * @see PFont#PFont
   * @see PGraphics#text(String, float, float)
   * @see PGraphics#textFont(PFont)
   * @see PGraphics#beginRaw(PGraphics)
   * @see PApplet#createFont(String, float, boolean)
   */
  public void textMode(int mode) {
    // CENTER and MODEL overlap (they're both 3)
    if ((mode == LEFT) || (mode == RIGHT)) {
      showWarning("Since Processing 1.0 beta, textMode() is now textAlign().");
      return;
    }
    if (mode == SCREEN) {
      showWarning("textMode(SCREEN) has been removed from Processing 2.0.");
      return;
    }

    if (textModeCheck(mode)) {
      textMode = mode;
    } else {
      String modeStr = String.valueOf(mode);
      switch (mode) {
        case MODEL: modeStr = "MODEL"; break;
        case SHAPE: modeStr = "SHAPE"; break;
      }
      showWarning("textMode(" + modeStr + ") is not supported by this renderer.");
    }
  }


  protected boolean textModeCheck(int mode) {
    return true;
  }


  /**
   * ( begin auto-generated from textSize.xml )
   *
   * Sets the current font size. This size will be used in all subsequent
   * calls to the <b>text()</b> function. Font size is measured in units of pixels.
   *
   * ( end auto-generated )
   *
   * @webref typography:attributes
   * @param size the size of the letters in units of pixels
   * @see PApplet#loadFont(String)
   * @see PFont#PFont
   * @see PGraphics#text(String, float, float)
   * @see PGraphics#textFont(PFont)
   */
  public void textSize(float size) {
    // https://github.com/processing/processing/issues/3110
    if (size <= 0) {
      // Using System.err instead of showWarning to avoid running out of
      // memory with a bunch of textSize() variants (cause of this bug is
      // usually something done with map() or in a loop).
      System.err.println("textSize(" + size + ") ignored: " +
                         "the text size must be larger than zero");
      return;
    }
    if (textFont == null) {
      defaultFontOrDeath("textSize", size);
    }
    textSizeImpl(size);
  }


  /**
   * Called from textSize() after validating size. Subclasses
   * will want to override this one.
   * @param size size of the text, greater than zero
   */
  protected void textSizeImpl(float size) {
    handleTextSize(size);
  }


  /**
   * Sets the actual size. Called from textSizeImpl and
   * from textFontImpl after setting the font.
   * @param size size of the text, greater than zero
   */
  protected void handleTextSize(float size) {
    textSize = size;
    textLeading = (textAscent() + textDescent()) * 1.275f;
  }


  // ........................................................


  /**
   * @param c the character to measure
   */
  public float textWidth(char c) {
    textWidthBuffer[0] = c;
    return textWidthImpl(textWidthBuffer, 0, 1);
  }


  /**
   * ( begin auto-generated from textWidth.xml )
   *
   * Calculates and returns the width of any character or text string.
   *
   * ( end auto-generated )
   *
   * @webref typography:attributes
   * @param str the String of characters to measure
   * @see PApplet#loadFont(String)
   * @see PFont#PFont
   * @see PGraphics#text(String, float, float)
   * @see PGraphics#textFont(PFont)
   * @see PGraphics#textSize(float)
   */
  public float textWidth(String str) {
    if (textFont == null) {
      defaultFontOrDeath("textWidth");
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
   * @nowebref
   */
  public float textWidth(char[] chars, int start, int length) {
    return textWidthImpl(chars, start, start + length);
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
   * ( begin auto-generated from text.xml )
   *
   * Draws text to the screen. Displays the information specified in the
   * <b>data</b> or <b>stringdata</b> parameters on the screen in the
   * position specified by the <b>x</b> and <b>y</b> parameters and the
   * optional <b>z</b> parameter. A default font will be used unless a font
   * is set with the <b>textFont()</b> function. Change the color of the text
   * with the <b>fill()</b> function. The text displays in relation to the
   * <b>textAlign()</b> function, which gives the option to draw to the left,
   * right, and center of the coordinates.
   * <br /><br />
   * The <b>x2</b> and <b>y2</b> parameters define a rectangular area to
   * display within and may only be used with string data. For text drawn
   * inside a rectangle, the coordinates are interpreted based on the current
   * <b>rectMode()</b> setting.
   *
   * ( end auto-generated )
   *
   * @webref typography:loading_displaying
   * @param c the alphanumeric character to be displayed
   * @param x x-coordinate of text
   * @param y y-coordinate of text
   * @see PGraphics#textAlign(int, int)
   * @see PGraphics#textFont(PFont)
   * @see PGraphics#textMode(int)
   * @see PGraphics#textSize(float)
   * @see PGraphics#textLeading(float)
   * @see PGraphics#textWidth(String)
   * @see PGraphics#textAscent()
   * @see PGraphics#textDescent()
   * @see PGraphics#rectMode(int)
   * @see PGraphics#fill(int, float)
   * @see_external String
   */
  public void text(char c, float x, float y) {
    if (textFont == null) {
      defaultFontOrDeath("text");
    }

    if (textAlignY == CENTER) {
      y += textAscent() / 2;
    } else if (textAlignY == TOP) {
      y += textAscent();
    } else if (textAlignY == BOTTOM) {
      y -= textDescent();
    //} else if (textAlignY == BASELINE) {
      // do nothing
    }

    textBuffer[0] = c;
    textLineAlignImpl(textBuffer, 0, 1, x, y);
  }


  /**
   * @param z z-coordinate of text
   */
  public void text(char c, float x, float y, float z) {
//    if ((z != 0) && (textMode == SCREEN)) {
//      String msg = "textMode(SCREEN) cannot have a z coordinate";
//      throw new RuntimeException(msg);
//    }

    if (z != 0) translate(0, 0, z);  // slowness, badness

    text(c, x, y);
//    textZ = z;

    if (z != 0) translate(0, 0, -z);
  }


  /**
   * @param str the alphanumeric symbols to be displayed
   */
//  public void text(String str) {
//    text(str, textX, textY, textZ);
//  }


  /**
   * <h3>Advanced</h3>
   * Draw a chunk of text.
   * Newlines that are \n (Unix newline or linefeed char, ascii 10)
   * are honored, but \r (carriage return, Windows and Mac OS) are
   * ignored.
   */
  public void text(String str, float x, float y) {
    if (textFont == null) {
      defaultFontOrDeath("text");
    }

    int length = str.length();
    if (length > textBuffer.length) {
      textBuffer = new char[length + 10];
    }
    str.getChars(0, length, textBuffer, 0);
    text(textBuffer, 0, length, x, y);
  }


  /**
   * <h3>Advanced</h3>
   * Method to draw text from an array of chars. This method will usually be
   * more efficient than drawing from a String object, because the String will
   * not be converted to a char array before drawing.
   * @param chars the alphanumberic symbols to be displayed
   * @param start array index at which to start writing characters
   * @param stop array index at which to stop writing characters
   */
  public void text(char[] chars, int start, int stop, float x, float y) {
    // If multiple lines, sum the height of the additional lines
    float high = 0; //-textAscent();
    for (int i = start; i < stop; i++) {
      if (chars[i] == '\n') {
        high += textLeading;
      }
    }
    if (textAlignY == CENTER) {
      // for a single line, this adds half the textAscent to y
      // for multiple lines, subtract half the additional height
      //y += (textAscent() - textDescent() - high)/2;
      y += (textAscent() - high)/2;
    } else if (textAlignY == TOP) {
      // for a single line, need to add textAscent to y
      // for multiple lines, no different
      y += textAscent();
    } else if (textAlignY == BOTTOM) {
      // for a single line, this is just offset by the descent
      // for multiple lines, subtract leading for each line
      y -= textDescent() + high;
    //} else if (textAlignY == BASELINE) {
      // do nothing
    }

//    int start = 0;
    int index = 0;
    while (index < stop) { //length) {
      if (chars[index] == '\n') {
        textLineAlignImpl(chars, start, index, x, y);
        start = index + 1;
        y += textLeading;
      }
      index++;
    }
    if (start < stop) {  //length) {
      textLineAlignImpl(chars, start, index, x, y);
    }
  }


  /**
   * Same as above but with a z coordinate.
   */
  public void text(String str, float x, float y, float z) {
    if (z != 0) translate(0, 0, z);  // slow!

    text(str, x, y);
//    textZ = z;

    if (z != 0) translate(0, 0, -z);  // inaccurate!
  }


  public void text(char[] chars, int start, int stop,
                   float x, float y, float z) {
    if (z != 0) translate(0, 0, z);  // slow!

    text(chars, start, stop, x, y);
//    textZ = z;

    if (z != 0) translate(0, 0, -z);  // inaccurate!
  }


  /**
   * <h3>Advanced</h3>
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
   *
   * @param x1 by default, the x-coordinate of text, see rectMode() for more info
   * @param y1 by default, the y-coordinate of text, see rectMode() for more info
   * @param x2 by default, the width of the text box, see rectMode() for more info
   * @param y2 by default, the height of the text box, see rectMode() for more info
   */
  public void text(String str, float x1, float y1, float x2, float y2) {
    if (textFont == null) {
      defaultFontOrDeath("text");
    }

    float hradius, vradius;
    switch (rectMode) {
    case CORNER:
      x2 += x1; y2 += y1;
      break;
    case RADIUS:
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

//    float currentY = y1;
    float boxWidth = x2 - x1;

//    // ala illustrator, the text itself must fit inside the box
//    currentY += textAscent(); //ascent() * textSize;
//    // if the box is already too small, tell em to f off
//    if (currentY > y2) return;

    float spaceWidth = textWidth(' ');

    if (textBreakStart == null) {
      textBreakStart = new int[20];
      textBreakStop = new int[20];
    }
    textBreakCount = 0;

    int length = str.length();
    if (length + 1 > textBuffer.length) {
      textBuffer = new char[length + 1];
    }
    str.getChars(0, length, textBuffer, 0);
    // add a fake newline to simplify calculations
    textBuffer[length++] = '\n';

    int sentenceStart = 0;
    for (int i = 0; i < length; i++) {
      if (textBuffer[i] == '\n') {
//        currentY = textSentence(textBuffer, sentenceStart, i,
//                                lineX, boxWidth, currentY, y2, spaceWidth);
        boolean legit =
          textSentence(textBuffer, sentenceStart, i, boxWidth, spaceWidth);
        if (!legit) break;
//      if (Float.isNaN(currentY)) break;  // word too big (or error)
//      if (currentY > y2) break;  // past the box
        sentenceStart = i + 1;
      }
    }

    // lineX is the position where the text starts, which is adjusted
    // to left/center/right based on the current textAlign
    float lineX = x1; //boxX1;
    if (textAlign == CENTER) {
      lineX = lineX + boxWidth/2f;
    } else if (textAlign == RIGHT) {
      lineX = x2; //boxX2;
    }

    float boxHeight = y2 - y1;
    //int lineFitCount = 1 + PApplet.floor((boxHeight - textAscent()) / textLeading);
    // incorporate textAscent() for the top (baseline will be y1 + ascent)
    // and textDescent() for the bottom, so that lower parts of letters aren't
    // outside the box. [0151]
    float topAndBottom = textAscent() + textDescent();
    int lineFitCount = 1 + PApplet.floor((boxHeight - topAndBottom) / textLeading);
    int lineCount = Math.min(textBreakCount, lineFitCount);

    if (textAlignY == CENTER) {
      float lineHigh = textAscent() + textLeading * (lineCount - 1);
      float y = y1 + textAscent() + (boxHeight - lineHigh) / 2;
      for (int i = 0; i < lineCount; i++) {
        textLineAlignImpl(textBuffer, textBreakStart[i], textBreakStop[i], lineX, y);
        y += textLeading;
      }

    } else if (textAlignY == BOTTOM) {
      float y = y2 - textDescent() - textLeading * (lineCount - 1);
      for (int i = 0; i < lineCount; i++) {
        textLineAlignImpl(textBuffer, textBreakStart[i], textBreakStop[i], lineX, y);
        y += textLeading;
      }

    } else {  // TOP or BASELINE just go to the default
      float y = y1 + textAscent();
      for (int i = 0; i < lineCount; i++) {
        textLineAlignImpl(textBuffer, textBreakStart[i], textBreakStop[i], lineX, y);
        y += textLeading;
      }
    }
  }


  /**
   * Emit a sentence of text, defined as a chunk of text without any newlines.
   * @param stop non-inclusive, the end of the text in question
   * @return false if cannot fit
   */
  protected boolean textSentence(char[] buffer, int start, int stop,
                                 float boxWidth, float spaceWidth) {
    float runningX = 0;

    // Keep track of this separately from index, since we'll need to back up
    // from index when breaking words that are too long to fit.
    int lineStart = start;
    int wordStart = start;
    int index = start;
    while (index <= stop) {
      // boundary of a word or end of this sentence
      if ((buffer[index] == ' ') || (index == stop)) {
//        System.out.println((index == stop) + " " + wordStart + " " + index);
        float wordWidth = 0;
        if (index > wordStart) {
          // we have a non-empty word, measure it
          wordWidth = textWidthImpl(buffer, wordStart, index);
        }

        if (runningX + wordWidth >= boxWidth) {
          if (runningX != 0) {
            // Next word is too big, output the current line and advance
            index = wordStart;
            textSentenceBreak(lineStart, index);
            // Eat whitespace before the first word on the next line.
            while ((index < stop) && (buffer[index] == ' ')) {
              index++;
            }
          } else {  // (runningX == 0)
            // If this is the first word on the line, and its width is greater
            // than the width of the text box, then break the word where at the
            // max width, and send the rest of the word to the next line.
            if (index - wordStart < 25) {
              do {
                index--;
                if (index == wordStart) {
                  // Not a single char will fit on this line. screw 'em.
                  return false;
                }
                wordWidth = textWidthImpl(buffer, wordStart, index);
              } while (wordWidth > boxWidth);
            } else {
              // This word is more than 25 characters long, might be faster to
              // start from the beginning of the text rather than shaving from
              // the end of it, which is super slow if it's 1000s of letters.
              // https://github.com/processing/processing/issues/211
              int lastIndex = index;
              index = wordStart + 1;
              // walk to the right while things fit
              while ((wordWidth = textWidthImpl(buffer, wordStart, index)) < boxWidth) {
                index++;
                if (index > lastIndex) {  // Unreachable?
                  break;
                }
              }
              index--;
              if (index == wordStart) {
                return false;  // nothing fits
              }
            }

            //textLineImpl(buffer, lineStart, index, x, y);
            textSentenceBreak(lineStart, index);
          }
          lineStart = index;
          wordStart = index;
          runningX = 0;

        } else if (index == stop) {
          // last line in the block, time to unload
          //textLineImpl(buffer, lineStart, index, x, y);
          textSentenceBreak(lineStart, index);
//          y += textLeading;
          index++;

        } else {  // this word will fit, just add it to the line
          runningX += wordWidth;
          wordStart = index ;  // move on to the next word including the space before the word
          index++;
        }
      } else {  // not a space or the last character
        index++;  // this is just another letter
      }
    }
//    return y;
    return true;
  }


  protected void textSentenceBreak(int start, int stop) {
    if (textBreakCount == textBreakStart.length) {
      textBreakStart = PApplet.expand(textBreakStart);
      textBreakStop = PApplet.expand(textBreakStop);
    }
    textBreakStart[textBreakCount] = start;
    textBreakStop[textBreakCount] = stop;
    textBreakCount++;
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
   *
   * @param num the numeric value to be displayed
   */
  public void text(float num, float x, float y) {
    text(PApplet.nfs(num, 0, 3), x, y);
  }


  public void text(float num, float x, float y, float z) {
    text(PApplet.nfs(num, 0, 3), x, y, z);
  }


  //////////////////////////////////////////////////////////////

  // TEXT IMPL

  // These are most likely to be overridden by subclasses, since the other
  // (public) functions handle generic features like setting alignment.


  /**
   * Handles placement of a text line, then calls textLineImpl
   * to actually render at the specific point.
   */
  protected void textLineAlignImpl(char buffer[], int start, int stop,
                                   float x, float y) {
    if (textAlign == CENTER) {
      x -= textWidthImpl(buffer, start, stop) / 2f;

    } else if (textAlign == RIGHT) {
      x -= textWidthImpl(buffer, start, stop);
    }

    textLineImpl(buffer, start, stop, x, y);
  }


  /**
   * Implementation of actual drawing for a line of text.
   */
  protected void textLineImpl(char buffer[], int start, int stop,
                              float x, float y) {
    for (int index = start; index < stop; index++) {
      textCharImpl(buffer[index], x, y);

      // this doesn't account for kerning
      x += textWidth(buffer[index]);
    }
//    textX = x;
//    textY = y;
//    textZ = 0;  // this will get set by the caller if non-zero
  }


  protected void textCharImpl(char ch, float x, float y) { //, float z) {
    PFont.Glyph glyph = textFont.getGlyph(ch);
    if (glyph != null) {
      if (textMode == MODEL) {
        float high    = glyph.height     / (float) textFont.getSize();
        float bwidth  = glyph.width      / (float) textFont.getSize();
        float lextent = glyph.leftExtent / (float) textFont.getSize();
        float textent = glyph.topExtent  / (float) textFont.getSize();

        float x1 = x + lextent * textSize;
        float y1 = y - textent * textSize;
        float x2 = x1 + bwidth * textSize;
        float y2 = y1 + high * textSize;

        textCharModelImpl(glyph.image,
                          x1, y1, x2, y2,
                          glyph.width, glyph.height);
      }
    } else if (ch != ' ' && ch != 127) {
      showWarning("No glyph found for the " + ch + " (\\u" + PApplet.hex(ch, 4) + ") character");
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


  /*
  protected void textCharScreenImpl(PImage glyph,
                                    int xx, int yy,
                                    int w0, int h0) {
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
        //int a1 = (fa * pixels1[row * textFont.twidth + col]) >> 8;
        int a1 = (fa * pixels1[row * glyph.width + col]) >> 8;
        int a2 = a1 ^ 0xff;
        //int p1 = pixels1[row * glyph.width + col];
        int p2 = pixels[(yy + row-y0)*width + (xx+col-x0)];

        pixels[(yy + row-y0)*width + xx+col-x0] =
          (0xff000000 |
           (((a1 * fr + a2 * ((p2 >> 16) & 0xff)) & 0xff00) << 8) |
           (( a1 * fg + a2 * ((p2 >>  8) & 0xff)) & 0xff00) |
           (( a1 * fb + a2 * ( p2        & 0xff)) >> 8));
      }
    }
  }
  */



  //////////////////////////////////////////////////////////////

  // PARITY WITH P5.JS

  /**
   * ( begin auto-generated from push.xml )
   *
   * The <b>push()</b> function saves the current drawing style 
   * settings and transformations, while <b>pop()</b> restores these 
   * settings. Note that these functions are always used together. 
   * They allow you to change the style and transformation settings 
   * and later return to what you had. When a new state is started 
   * with push(), it builds on the current style and transform 
   * information.<br />
   * <br />
   * <b>push()</b> stores information related to the current 
   * transformation state and style settings controlled by the 
   * following functions: <b>rotate()</b>, <b>translate()</b>, 
   * <b>scale()</b>, <b>fill()</b>, <b>stroke()</b>, <b>tint()</b>, 
   * <b>strokeWeight()</b>, <b>strokeCap()</b>, <b>strokeJoin()</b>, 
   * <b>imageMode()</b>, <b>rectMode()</b>, <b>ellipseMode()</b>, 
   * <b>colorMode()</b>, <b>textAlign()</b>, <b>textFont()</b>, 
   * <b>textMode()</b>, <b>textSize()</b>, <b>textLeading()</b>.<br />
   * <br />
   * The <b>push()</b> and <b>pop()</b> functions were added with 
   * Processing 3.5. They can be used in place of <b>pushMatrix()</b>, 
   * <b>popMatrix()</b>, <b>pushStyles()</b>, and <b>popStyles()</b>. 
   * The difference is that push() and pop() control both the 
   * transformations (rotate, scale, translate) and the drawing styles 
   * at the same time.
   *
   * ( end auto-generated )
   *
   * @webref structure
   * @see PGraphics#pop()
   */
  public void push() {
    pushStyle();
    pushMatrix();
  }

  /**
   * ( begin auto-generated from pop.xml )
   *
   * The <b>pop()</b> function restores the previous drawing style 
   * settings and transformations after <b>push()</b> has changed them. 
   * Note that these functions are always used together. They allow 
   * you to change the style and transformation settings and later 
   * return to what you had. When a new state is started with push(), 
   * it builds on the current style and transform information.<br />
   * <br />
   * <br />
   * <b>push()</b> stores information related to the current 
   * transformation state and style settings controlled by the 
   * following functions: <b>rotate()</b>, <b>translate()</b>, 
   * <b>scale()</b>, <b>fill()</b>, <b>stroke()</b>, <b>tint()</b>, 
   * <b>strokeWeight()</b>, <b>strokeCap()</b>, <b>strokeJoin()</b>, 
   * <b>imageMode()</b>, <b>rectMode()</b>, <b>ellipseMode()</b>, 
   * <b>colorMode()</b>, <b>textAlign()</b>, <b>textFont()</b>, 
   * <b>textMode()</b>, <b>textSize()</b>, <b>textLeading()</b>.<br />
   * <br />
   * The <b>push()</b> and <b>pop()</b> functions were added with 
   * Processing 3.5. They can be used in place of <b>pushMatrix()</b>, 
   * <b>popMatrix()</b>, <b>pushStyles()</b>, and <b>popStyles()</b>. 
   * The difference is that push() and pop() control both the 
   * transformations (rotate, scale, translate) and the drawing styles 
   * at the same time.
   *
   * ( end auto-generated )
   *
   * @webref structure
   * @see PGraphics#push()
   */
  public void pop() {
    popStyle();
    popMatrix();
  }



  //////////////////////////////////////////////////////////////

  // MATRIX STACK


  /**
   * ( begin auto-generated from pushMatrix.xml )
   *
   * Pushes the current transformation matrix onto the matrix stack.
   * Understanding <b>pushMatrix()</b> and <b>popMatrix()</b> requires
   * understanding the concept of a matrix stack. The <b>pushMatrix()</b>
   * function saves the current coordinate system to the stack and
   * <b>popMatrix()</b> restores the prior coordinate system.
   * <b>pushMatrix()</b> and <b>popMatrix()</b> are used in conjuction with
   * the other transformation functions and may be embedded to control the
   * scope of the transformations.
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @see PGraphics#popMatrix()
   * @see PGraphics#translate(float, float, float)
   * @see PGraphics#scale(float)
   * @see PGraphics#rotate(float)
   * @see PGraphics#rotateX(float)
   * @see PGraphics#rotateY(float)
   * @see PGraphics#rotateZ(float)
   */
  public void pushMatrix() {
    showMethodWarning("pushMatrix");
  }


  /**
   * ( begin auto-generated from popMatrix.xml )
   *
   * Pops the current transformation matrix off the matrix stack.
   * Understanding pushing and popping requires understanding the concept of
   * a matrix stack. The <b>pushMatrix()</b> function saves the current
   * coordinate system to the stack and <b>popMatrix()</b> restores the prior
   * coordinate system. <b>pushMatrix()</b> and <b>popMatrix()</b> are used
   * in conjuction with the other transformation functions and may be
   * embedded to control the scope of the transformations.
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @see PGraphics#pushMatrix()
   */
  public void popMatrix() {
    showMethodWarning("popMatrix");
  }



  //////////////////////////////////////////////////////////////

  // MATRIX TRANSFORMATIONS


  /**
   * ( begin auto-generated from translate.xml )
   *
   * Specifies an amount to displace objects within the display window. The
   * <b>x</b> parameter specifies left/right translation, the <b>y</b>
   * parameter specifies up/down translation, and the <b>z</b> parameter
   * specifies translations toward/away from the screen. Using this function
   * with the <b>z</b> parameter requires using P3D as a parameter in
   * combination with size as shown in the above example. Transformations
   * apply to everything that happens after and subsequent calls to the
   * function accumulates the effect. For example, calling <b>translate(50,
   * 0)</b> and then <b>translate(20, 0)</b> is the same as <b>translate(70,
   * 0)</b>. If <b>translate()</b> is called within <b>draw()</b>, the
   * transformation is reset when the loop begins again. This function can be
   * further controlled by the <b>pushMatrix()</b> and <b>popMatrix()</b>.
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @param x left/right translation
   * @param y up/down translation
   * @see PGraphics#popMatrix()
   * @see PGraphics#pushMatrix()
   * @see PGraphics#rotate(float)
   * @see PGraphics#rotateX(float)
   * @see PGraphics#rotateY(float)
   * @see PGraphics#rotateZ(float)
   * @see PGraphics#scale(float, float, float)
   */
  public void translate(float x, float y) {
    showMissingWarning("translate");
  }


  /**
   * @param z forward/backward translation
   */
  public void translate(float x, float y, float z) {
    showMissingWarning("translate");
  }


  /**
   * ( begin auto-generated from rotate.xml )
   *
   * Rotates a shape the amount specified by the <b>angle</b> parameter.
   * Angles should be specified in radians (values from 0 to TWO_PI) or
   * converted to radians with the <b>radians()</b> function.
   * <br/> <br/>
   * Objects are always rotated around their relative position to the origin
   * and positive numbers rotate objects in a clockwise direction.
   * Transformations apply to everything that happens after and subsequent
   * calls to the function accumulates the effect. For example, calling
   * <b>rotate(HALF_PI)</b> and then <b>rotate(HALF_PI)</b> is the same as
   * <b>rotate(PI)</b>. All tranformations are reset when <b>draw()</b>
   * begins again.
   * <br/> <br/>
   * Technically, <b>rotate()</b> multiplies the current transformation
   * matrix by a rotation matrix. This function can be further controlled by
   * the <b>pushMatrix()</b> and <b>popMatrix()</b>.
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @param angle angle of rotation specified in radians
   * @see PGraphics#popMatrix()
   * @see PGraphics#pushMatrix()
   * @see PGraphics#rotateX(float)
   * @see PGraphics#rotateY(float)
   * @see PGraphics#rotateZ(float)
   * @see PGraphics#scale(float, float, float)
   * @see PApplet#radians(float)
   */
  public void rotate(float angle) {
    showMissingWarning("rotate");
  }


  /**
   * ( begin auto-generated from rotateX.xml )
   *
   * Rotates a shape around the x-axis the amount specified by the
   * <b>angle</b> parameter. Angles should be specified in radians (values
   * from 0 to PI*2) or converted to radians with the <b>radians()</b>
   * function. Objects are always rotated around their relative position to
   * the origin and positive numbers rotate objects in a counterclockwise
   * direction. Transformations apply to everything that happens after and
   * subsequent calls to the function accumulates the effect. For example,
   * calling <b>rotateX(PI/2)</b> and then <b>rotateX(PI/2)</b> is the same
   * as <b>rotateX(PI)</b>. If <b>rotateX()</b> is called within the
   * <b>draw()</b>, the transformation is reset when the loop begins again.
   * This function requires using P3D as a third parameter to <b>size()</b>
   * as shown in the example above.
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @param angle angle of rotation specified in radians
   * @see PGraphics#popMatrix()
   * @see PGraphics#pushMatrix()
   * @see PGraphics#rotate(float)
   * @see PGraphics#rotateY(float)
   * @see PGraphics#rotateZ(float)
   * @see PGraphics#scale(float, float, float)
   * @see PGraphics#translate(float, float, float)
   */
  public void rotateX(float angle) {
    showMethodWarning("rotateX");
  }


  /**
   * ( begin auto-generated from rotateY.xml )
   *
   * Rotates a shape around the y-axis the amount specified by the
   * <b>angle</b> parameter. Angles should be specified in radians (values
   * from 0 to PI*2) or converted to radians with the <b>radians()</b>
   * function. Objects are always rotated around their relative position to
   * the origin and positive numbers rotate objects in a counterclockwise
   * direction. Transformations apply to everything that happens after and
   * subsequent calls to the function accumulates the effect. For example,
   * calling <b>rotateY(PI/2)</b> and then <b>rotateY(PI/2)</b> is the same
   * as <b>rotateY(PI)</b>. If <b>rotateY()</b> is called within the
   * <b>draw()</b>, the transformation is reset when the loop begins again.
   * This function requires using P3D as a third parameter to <b>size()</b>
   * as shown in the examples above.
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @param angle angle of rotation specified in radians
   * @see PGraphics#popMatrix()
   * @see PGraphics#pushMatrix()
   * @see PGraphics#rotate(float)
   * @see PGraphics#rotateX(float)
   * @see PGraphics#rotateZ(float)
   * @see PGraphics#scale(float, float, float)
   * @see PGraphics#translate(float, float, float)
   */
  public void rotateY(float angle) {
    showMethodWarning("rotateY");
  }


  /**
   * ( begin auto-generated from rotateZ.xml )
   *
   * Rotates a shape around the z-axis the amount specified by the
   * <b>angle</b> parameter. Angles should be specified in radians (values
   * from 0 to PI*2) or converted to radians with the <b>radians()</b>
   * function. Objects are always rotated around their relative position to
   * the origin and positive numbers rotate objects in a counterclockwise
   * direction. Transformations apply to everything that happens after and
   * subsequent calls to the function accumulates the effect. For example,
   * calling <b>rotateZ(PI/2)</b> and then <b>rotateZ(PI/2)</b> is the same
   * as <b>rotateZ(PI)</b>. If <b>rotateZ()</b> is called within the
   * <b>draw()</b>, the transformation is reset when the loop begins again.
   * This function requires using P3D as a third parameter to <b>size()</b>
   * as shown in the examples above.
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @param angle angle of rotation specified in radians
   * @see PGraphics#popMatrix()
   * @see PGraphics#pushMatrix()
   * @see PGraphics#rotate(float)
   * @see PGraphics#rotateX(float)
   * @see PGraphics#rotateY(float)
   * @see PGraphics#scale(float, float, float)
   * @see PGraphics#translate(float, float, float)
   */
  public void rotateZ(float angle) {
    showMethodWarning("rotateZ");
  }


  /**
   * <h3>Advanced</h3>
   * Rotate about a vector in space. Same as the glRotatef() function.
   * @nowebref
   * @param x
   * @param y
   * @param z
   */
  public void rotate(float angle, float x, float y, float z) {
    showMissingWarning("rotate");
  }


  /**
   * ( begin auto-generated from scale.xml )
   *
   * Increases or decreases the size of a shape by expanding and contracting
   * vertices. Objects always scale from their relative origin to the
   * coordinate system. Scale values are specified as decimal percentages.
   * For example, the function call <b>scale(2.0)</b> increases the dimension
   * of a shape by 200%. Transformations apply to everything that happens
   * after and subsequent calls to the function multiply the effect. For
   * example, calling <b>scale(2.0)</b> and then <b>scale(1.5)</b> is the
   * same as <b>scale(3.0)</b>. If <b>scale()</b> is called within
   * <b>draw()</b>, the transformation is reset when the loop begins again.
   * Using this fuction with the <b>z</b> parameter requires using P3D as a
   * parameter for <b>size()</b> as shown in the example above. This function
   * can be further controlled by <b>pushMatrix()</b> and <b>popMatrix()</b>.
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @param s percentage to scale the object
   * @see PGraphics#pushMatrix()
   * @see PGraphics#popMatrix()
   * @see PGraphics#translate(float, float, float)
   * @see PGraphics#rotate(float)
   * @see PGraphics#rotateX(float)
   * @see PGraphics#rotateY(float)
   * @see PGraphics#rotateZ(float)
   */
  public void scale(float s) {
    showMissingWarning("scale");
  }


  /**
   * <h3>Advanced</h3>
   * Scale in X and Y. Equivalent to scale(sx, sy, 1).
   *
   * Not recommended for use in 3D, because the z-dimension is just
   * scaled by 1, since there's no way to know what else to scale it by.
   *
   * @param x percentage to scale the object in the x-axis
   * @param y percentage to scale the object in the y-axis
   */
  public void scale(float x, float y) {
    showMissingWarning("scale");
  }


  /**
   * @param z percentage to scale the object in the z-axis
   */
  public void scale(float x, float y, float z) {
    showMissingWarning("scale");
  }


  /**
   * ( begin auto-generated from shearX.xml )
   *
   * Shears a shape around the x-axis the amount specified by the
   * <b>angle</b> parameter. Angles should be specified in radians (values
   * from 0 to PI*2) or converted to radians with the <b>radians()</b>
   * function. Objects are always sheared around their relative position to
   * the origin and positive numbers shear objects in a clockwise direction.
   * Transformations apply to everything that happens after and subsequent
   * calls to the function accumulates the effect. For example, calling
   * <b>shearX(PI/2)</b> and then <b>shearX(PI/2)</b> is the same as
   * <b>shearX(PI)</b>. If <b>shearX()</b> is called within the
   * <b>draw()</b>, the transformation is reset when the loop begins again.
   * <br/> <br/>
   * Technically, <b>shearX()</b> multiplies the current transformation
   * matrix by a rotation matrix. This function can be further controlled by
   * the <b>pushMatrix()</b> and <b>popMatrix()</b> functions.
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @param angle angle of shear specified in radians
   * @see PGraphics#popMatrix()
   * @see PGraphics#pushMatrix()
   * @see PGraphics#shearY(float)
   * @see PGraphics#scale(float, float, float)
   * @see PGraphics#translate(float, float, float)
   * @see PApplet#radians(float)
   */
  public void shearX(float angle) {
    showMissingWarning("shearX");
  }


  /**
   * ( begin auto-generated from shearY.xml )
   *
   * Shears a shape around the y-axis the amount specified by the
   * <b>angle</b> parameter. Angles should be specified in radians (values
   * from 0 to PI*2) or converted to radians with the <b>radians()</b>
   * function. Objects are always sheared around their relative position to
   * the origin and positive numbers shear objects in a clockwise direction.
   * Transformations apply to everything that happens after and subsequent
   * calls to the function accumulates the effect. For example, calling
   * <b>shearY(PI/2)</b> and then <b>shearY(PI/2)</b> is the same as
   * <b>shearY(PI)</b>. If <b>shearY()</b> is called within the
   * <b>draw()</b>, the transformation is reset when the loop begins again.
   * <br/> <br/>
   * Technically, <b>shearY()</b> multiplies the current transformation
   * matrix by a rotation matrix. This function can be further controlled by
   * the <b>pushMatrix()</b> and <b>popMatrix()</b> functions.
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @param angle angle of shear specified in radians
   * @see PGraphics#popMatrix()
   * @see PGraphics#pushMatrix()
   * @see PGraphics#shearX(float)
   * @see PGraphics#scale(float, float, float)
   * @see PGraphics#translate(float, float, float)
   * @see PApplet#radians(float)
   */
  public void shearY(float angle) {
    showMissingWarning("shearY");
  }


  //////////////////////////////////////////////////////////////

  // MATRIX FULL MONTY


  /**
   * ( begin auto-generated from resetMatrix.xml )
   *
   * Replaces the current matrix with the identity matrix. The equivalent
   * function in OpenGL is glLoadIdentity().
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @see PGraphics#pushMatrix()
   * @see PGraphics#popMatrix()
   * @see PGraphics#applyMatrix(PMatrix)
   * @see PGraphics#printMatrix()
   */
  public void resetMatrix() {
    showMethodWarning("resetMatrix");
  }

  /**
   * ( begin auto-generated from applyMatrix.xml )
   *
   * Multiplies the current matrix by the one specified through the
   * parameters. This is very slow because it will try to calculate the
   * inverse of the transform, so avoid it whenever possible. The equivalent
   * function in OpenGL is glMultMatrix().
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @source
   * @see PGraphics#pushMatrix()
   * @see PGraphics#popMatrix()
   * @see PGraphics#resetMatrix()
   * @see PGraphics#printMatrix()
   */
  public void applyMatrix(PMatrix source) {
    if (source instanceof PMatrix2D) {
      applyMatrix((PMatrix2D) source);
    } else if (source instanceof PMatrix3D) {
      applyMatrix((PMatrix3D) source);
    }
  }


  public void applyMatrix(PMatrix2D source) {
    applyMatrix(source.m00, source.m01, source.m02,
                source.m10, source.m11, source.m12);
  }


  /**
   * @param n00 numbers which define the 4x4 matrix to be multiplied
   * @param n01 numbers which define the 4x4 matrix to be multiplied
   * @param n02 numbers which define the 4x4 matrix to be multiplied
   * @param n10 numbers which define the 4x4 matrix to be multiplied
   * @param n11 numbers which define the 4x4 matrix to be multiplied
   * @param n12 numbers which define the 4x4 matrix to be multiplied
   */
  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    showMissingWarning("applyMatrix");
  }

  public void applyMatrix(PMatrix3D source) {
    applyMatrix(source.m00, source.m01, source.m02, source.m03,
                source.m10, source.m11, source.m12, source.m13,
                source.m20, source.m21, source.m22, source.m23,
                source.m30, source.m31, source.m32, source.m33);
  }


  /**
   * @param n03 numbers which define the 4x4 matrix to be multiplied
   * @param n13 numbers which define the 4x4 matrix to be multiplied
   * @param n20 numbers which define the 4x4 matrix to be multiplied
   * @param n21 numbers which define the 4x4 matrix to be multiplied
   * @param n22 numbers which define the 4x4 matrix to be multiplied
   * @param n23 numbers which define the 4x4 matrix to be multiplied
   * @param n30 numbers which define the 4x4 matrix to be multiplied
   * @param n31 numbers which define the 4x4 matrix to be multiplied
   * @param n32 numbers which define the 4x4 matrix to be multiplied
   * @param n33 numbers which define the 4x4 matrix to be multiplied
   */
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    showMissingWarning("applyMatrix");
  }



  //////////////////////////////////////////////////////////////

  // MATRIX GET/SET/PRINT


  public PMatrix getMatrix() {
    showMissingWarning("getMatrix");
    return null;
  }


  /**
   * Copy the current transformation matrix into the specified target.
   * Pass in null to create a new matrix.
   */
  public PMatrix2D getMatrix(PMatrix2D target) {
    showMissingWarning("getMatrix");
    return null;
  }


  /**
   * Copy the current transformation matrix into the specified target.
   * Pass in null to create a new matrix.
   */
  public PMatrix3D getMatrix(PMatrix3D target) {
    showMissingWarning("getMatrix");
    return null;
  }


  /**
   * Set the current transformation matrix to the contents of another.
   */
  public void setMatrix(PMatrix source) {
    if (source instanceof PMatrix2D) {
      setMatrix((PMatrix2D) source);
    } else if (source instanceof PMatrix3D) {
      setMatrix((PMatrix3D) source);
    }
  }


  /**
   * Set the current transformation to the contents of the specified source.
   */
  public void setMatrix(PMatrix2D source) {
    showMissingWarning("setMatrix");
  }


  /**
   * Set the current transformation to the contents of the specified source.
   */
  public void setMatrix(PMatrix3D source) {
    showMissingWarning("setMatrix");
  }


  /**
   * ( begin auto-generated from printMatrix.xml )
   *
   * Prints the current matrix to the Console (the text window at the bottom
   * of Processing).
   *
   * ( end auto-generated )
   *
   * @webref transform
   * @see PGraphics#pushMatrix()
   * @see PGraphics#popMatrix()
   * @see PGraphics#resetMatrix()
   * @see PGraphics#applyMatrix(PMatrix)
   */
  public void printMatrix() {
    showMethodWarning("printMatrix");
  }


  //////////////////////////////////////////////////////////////

  // CAMERA

  /**
   * ( begin auto-generated from beginCamera.xml )
   *
   * The <b>beginCamera()</b> and <b>endCamera()</b> functions enable
   * advanced customization of the camera space. The functions are useful if
   * you want to more control over camera movement, however for most users,
   * the <b>camera()</b> function will be sufficient.<br /><br />The camera
   * functions will replace any transformations (such as <b>rotate()</b> or
   * <b>translate()</b>) that occur before them in <b>draw()</b>, but they
   * will not automatically replace the camera transform itself. For this
   * reason, camera functions should be placed at the beginning of
   * <b>draw()</b> (so that transformations happen afterwards), and the
   * <b>camera()</b> function can be used after <b>beginCamera()</b> if you
   * want to reset the camera before applying transformations.<br /><br
   * />This function sets the matrix mode to the camera matrix so calls such
   * as <b>translate()</b>, <b>rotate()</b>, applyMatrix() and resetMatrix()
   * affect the camera. <b>beginCamera()</b> should always be used with a
   * following <b>endCamera()</b> and pairs of <b>beginCamera()</b> and
   * <b>endCamera()</b> cannot be nested.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:camera
   * @see PGraphics#camera()
   * @see PGraphics#endCamera()
   * @see PGraphics#applyMatrix(PMatrix)
   * @see PGraphics#resetMatrix()
   * @see PGraphics#translate(float, float, float)
   * @see PGraphics#scale(float, float, float)
   */
  public void beginCamera() {
    showMethodWarning("beginCamera");
  }

  /**
   * ( begin auto-generated from endCamera.xml )
   *
   * The <b>beginCamera()</b> and <b>endCamera()</b> functions enable
   * advanced customization of the camera space. Please see the reference for
   * <b>beginCamera()</b> for a description of how the functions are used.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:camera
   * @see PGraphics#beginCamera()
   * @see PGraphics#camera(float, float, float, float, float, float, float, float, float)
   */
  public void endCamera() {
    showMethodWarning("endCamera");
  }

  /**
   * ( begin auto-generated from camera.xml )
   *
   * Sets the position of the camera through setting the eye position, the
   * center of the scene, and which axis is facing upward. Moving the eye
   * position and the direction it is pointing (the center of the scene)
   * allows the images to be seen from different angles. The version without
   * any parameters sets the camera to the default position, pointing to the
   * center of the display window with the Y axis as up. The default values
   * are <b>camera(width/2.0, height/2.0, (height/2.0) / tan(PI*30.0 /
   * 180.0), width/2.0, height/2.0, 0, 0, 1, 0)</b>. This function is similar
   * to <b>gluLookAt()</b> in OpenGL, but it first clears the current camera settings.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:camera
   * @see PGraphics#beginCamera()
   * @see PGraphics#endCamera()
   * @see PGraphics#frustum(float, float, float, float, float, float)
   */
  public void camera() {
    showMissingWarning("camera");
  }

/**
 * @param eyeX x-coordinate for the eye
 * @param eyeY y-coordinate for the eye
 * @param eyeZ z-coordinate for the eye
 * @param centerX x-coordinate for the center of the scene
 * @param centerY y-coordinate for the center of the scene
 * @param centerZ z-coordinate for the center of the scene
 * @param upX usually 0.0, 1.0, or -1.0
 * @param upY usually 0.0, 1.0, or -1.0
 * @param upZ usually 0.0, 1.0, or -1.0
 */
  public void camera(float eyeX, float eyeY, float eyeZ,
                     float centerX, float centerY, float centerZ,
                     float upX, float upY, float upZ) {
    showMissingWarning("camera");
  }

/**
   * ( begin auto-generated from printCamera.xml )
   *
   * Prints the current camera matrix to the Console (the text window at the
   * bottom of Processing).
   *
   * ( end auto-generated )
 * @webref lights_camera:camera
 * @see PGraphics#camera(float, float, float, float, float, float, float, float, float)
 */
  public void printCamera() {
    showMethodWarning("printCamera");
  }



  //////////////////////////////////////////////////////////////

  // PROJECTION

  /**
   * ( begin auto-generated from ortho.xml )
   *
   * Sets an orthographic projection and defines a parallel clipping volume.
   * All objects with the same dimension appear the same size, regardless of
   * whether they are near or far from the camera. The parameters to this
   * function specify the clipping volume where left and right are the
   * minimum and maximum x values, top and bottom are the minimum and maximum
   * y values, and near and far are the minimum and maximum z values. If no
   * parameters are given, the default is used: ortho(0, width, 0, height,
   * -10, 10).
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:camera
   */
  public void ortho() {
    showMissingWarning("ortho");
  }

  /**
   * @param left left plane of the clipping volume
   * @param right right plane of the clipping volume
   * @param bottom bottom plane of the clipping volume
   * @param top top plane of the clipping volume
   */
  public void ortho(float left, float right,
                    float bottom, float top) {
    showMissingWarning("ortho");
  }

  /**
   * @param near maximum distance from the origin to the viewer
   * @param far maximum distance from the origin away from the viewer
   */
  public void ortho(float left, float right,
                    float bottom, float top,
                    float near, float far) {
    showMissingWarning("ortho");
  }

  /**
   * ( begin auto-generated from perspective.xml )
   *
   * Sets a perspective projection applying foreshortening, making distant
   * objects appear smaller than closer ones. The parameters define a viewing
   * volume with the shape of truncated pyramid. Objects near to the front of
   * the volume appear their actual size, while farther objects appear
   * smaller. This projection simulates the perspective of the world more
   * accurately than orthographic projection. The version of perspective
   * without parameters sets the default perspective and the version with
   * four parameters allows the programmer to set the area precisely. The
   * default values are: perspective(PI/3.0, width/height, cameraZ/10.0,
   * cameraZ*10.0) where cameraZ is ((height/2.0) / tan(PI*60.0/360.0));
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:camera
   */
  public void perspective() {
    showMissingWarning("perspective");
  }

  /**
   * @param fovy field-of-view angle (in radians) for vertical direction
   * @param aspect ratio of width to height
   * @param zNear z-position of nearest clipping plane
   * @param zFar z-position of farthest clipping plane
   */
  public void perspective(float fovy, float aspect, float zNear, float zFar) {
    showMissingWarning("perspective");
  }

  /**
   * ( begin auto-generated from frustum.xml )
   *
   * Sets a perspective matrix defined through the parameters. Works like
   * glFrustum, except it wipes out the current perspective matrix rather
   * than muliplying itself with it.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:camera
   * @param left left coordinate of the clipping plane
   * @param right right coordinate of the clipping plane
   * @param bottom bottom coordinate of the clipping plane
   * @param top top coordinate of the clipping plane
   * @param near near component of the clipping plane; must be greater than zero
   * @param far far component of the clipping plane; must be greater than the near value
   * @see PGraphics#camera(float, float, float, float, float, float, float, float, float)
   * @see PGraphics#beginCamera()
   * @see PGraphics#endCamera()
   * @see PGraphics#perspective(float, float, float, float)
   */
  public void frustum(float left, float right,
                      float bottom, float top,
                      float near, float far) {
    showMethodWarning("frustum");
  }

  /**
   * ( begin auto-generated from printProjection.xml )
   *
   * Prints the current projection matrix to the Console (the text window at
   * the bottom of Processing).
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:camera
   * @see PGraphics#camera(float, float, float, float, float, float, float, float, float)
   */
  public void printProjection() {
    showMethodWarning("printProjection");
  }



  //////////////////////////////////////////////////////////////

  // SCREEN TRANSFORMS


  /**
   * ( begin auto-generated from screenX.xml )
   *
   * Takes a three-dimensional X, Y, Z position and returns the X value for
   * where it will appear on a (two-dimensional) screen.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:coordinates
   * @param x 3D x-coordinate to be mapped
   * @param y 3D y-coordinate to be mapped
   * @see PGraphics#screenY(float, float, float)
   * @see PGraphics#screenZ(float, float, float)
   */
  public float screenX(float x, float y) {
    showMissingWarning("screenX");
    return 0;
  }


  /**
   * ( begin auto-generated from screenY.xml )
   *
   * Takes a three-dimensional X, Y, Z position and returns the Y value for
   * where it will appear on a (two-dimensional) screen.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:coordinates
   * @param x 3D x-coordinate to be mapped
   * @param y 3D y-coordinate to be mapped
   * @see PGraphics#screenX(float, float, float)
   * @see PGraphics#screenZ(float, float, float)
   */
  public float screenY(float x, float y) {
    showMissingWarning("screenY");
    return 0;
  }


  /**
   * @param z 3D z-coordinate to be mapped
   */
  public float screenX(float x, float y, float z) {
    showMissingWarning("screenX");
    return 0;
  }


  /**
   * @param z 3D z-coordinate to be mapped
   */
  public float screenY(float x, float y, float z) {
    showMissingWarning("screenY");
    return 0;
  }



  /**
   * ( begin auto-generated from screenZ.xml )
   *
   * Takes a three-dimensional X, Y, Z position and returns the Z value for
   * where it will appear on a (two-dimensional) screen.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:coordinates
   * @param x 3D x-coordinate to be mapped
   * @param y 3D y-coordinate to be mapped
   * @param z 3D z-coordinate to be mapped
   * @see PGraphics#screenX(float, float, float)
   * @see PGraphics#screenY(float, float, float)
   */
  public float screenZ(float x, float y, float z) {
    showMissingWarning("screenZ");
    return 0;
  }


  /**
   * ( begin auto-generated from modelX.xml )
   *
   * Returns the three-dimensional X, Y, Z position in model space. This
   * returns the X value for a given coordinate based on the current set of
   * transformations (scale, rotate, translate, etc.) The X value can be used
   * to place an object in space relative to the location of the original
   * point once the transformations are no longer in use.
   * <br/> <br/>
   * In the example, the <b>modelX()</b>, <b>modelY()</b>, and
   * <b>modelZ()</b> functions record the location of a box in space after
   * being placed using a series of translate and rotate commands. After
   * popMatrix() is called, those transformations no longer apply, but the
   * (x, y, z) coordinate returned by the model functions is used to place
   * another box in the same location.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:coordinates
   * @param x 3D x-coordinate to be mapped
   * @param y 3D y-coordinate to be mapped
   * @param z 3D z-coordinate to be mapped
   * @see PGraphics#modelY(float, float, float)
   * @see PGraphics#modelZ(float, float, float)
   */
  public float modelX(float x, float y, float z) {
    showMissingWarning("modelX");
    return 0;
  }


  /**
   * ( begin auto-generated from modelY.xml )
   *
   * Returns the three-dimensional X, Y, Z position in model space. This
   * returns the Y value for a given coordinate based on the current set of
   * transformations (scale, rotate, translate, etc.) The Y value can be used
   * to place an object in space relative to the location of the original
   * point once the transformations are no longer in use.<br />
   * <br />
   * In the example, the <b>modelX()</b>, <b>modelY()</b>, and
   * <b>modelZ()</b> functions record the location of a box in space after
   * being placed using a series of translate and rotate commands. After
   * popMatrix() is called, those transformations no longer apply, but the
   * (x, y, z) coordinate returned by the model functions is used to place
   * another box in the same location.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:coordinates
   * @param x 3D x-coordinate to be mapped
   * @param y 3D y-coordinate to be mapped
   * @param z 3D z-coordinate to be mapped
   * @see PGraphics#modelX(float, float, float)
   * @see PGraphics#modelZ(float, float, float)
   */
  public float modelY(float x, float y, float z) {
    showMissingWarning("modelY");
    return 0;
  }


  /**
   * ( begin auto-generated from modelZ.xml )
   *
   * Returns the three-dimensional X, Y, Z position in model space. This
   * returns the Z value for a given coordinate based on the current set of
   * transformations (scale, rotate, translate, etc.) The Z value can be used
   * to place an object in space relative to the location of the original
   * point once the transformations are no longer in use.<br />
   * <br />
   * In the example, the <b>modelX()</b>, <b>modelY()</b>, and
   * <b>modelZ()</b> functions record the location of a box in space after
   * being placed using a series of translate and rotate commands. After
   * popMatrix() is called, those transformations no longer apply, but the
   * (x, y, z) coordinate returned by the model functions is used to place
   * another box in the same location.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:coordinates
   * @param x 3D x-coordinate to be mapped
   * @param y 3D y-coordinate to be mapped
   * @param z 3D z-coordinate to be mapped
   * @see PGraphics#modelX(float, float, float)
   * @see PGraphics#modelY(float, float, float)
   */
  public float modelZ(float x, float y, float z) {
    showMissingWarning("modelZ");
    return 0;
  }



  //////////////////////////////////////////////////////////////

  // STYLE

  /**
   * ( begin auto-generated from pushStyle.xml )
   *
   * The <b>pushStyle()</b> function saves the current style settings and
   * <b>popStyle()</b> restores the prior settings. Note that these functions
   * are always used together. They allow you to change the style settings
   * and later return to what you had. When a new style is started with
   * <b>pushStyle()</b>, it builds on the current style information. The
   * <b>pushStyle()</b> and <b>popStyle()</b> functions can be embedded to
   * provide more control (see the second example above for a demonstration.)
   * <br /><br />
   * The style information controlled by the following functions are included
   * in the style:
   * fill(), stroke(), tint(), strokeWeight(), strokeCap(), strokeJoin(),
   * imageMode(), rectMode(), ellipseMode(), shapeMode(), colorMode(),
   * textAlign(), textFont(), textMode(), textSize(), textLeading(),
   * emissive(), specular(), shininess(), ambient()
   *
   * ( end auto-generated )
   *
   * @webref structure
   * @see PGraphics#popStyle()
   */
  public void pushStyle() {
    if (styleStackDepth == styleStack.length) {
      styleStack = (PStyle[]) PApplet.expand(styleStack);
    }
    if (styleStack[styleStackDepth] == null) {
      styleStack[styleStackDepth] = new PStyle();
    }
    PStyle s = styleStack[styleStackDepth++];
    getStyle(s);
  }

  /**
   * ( begin auto-generated from popStyle.xml )
   *
   * The <b>pushStyle()</b> function saves the current style settings and
   * <b>popStyle()</b> restores the prior settings; these functions are
   * always used together. They allow you to change the style settings and
   * later return to what you had. When a new style is started with
   * <b>pushStyle()</b>, it builds on the current style information. The
   * <b>pushStyle()</b> and <b>popStyle()</b> functions can be embedded to
   * provide more control (see the second example above for a demonstration.)
   *
   * ( end auto-generated )
   *
   * @webref structure
   * @see PGraphics#pushStyle()
   */
  public void popStyle() {
    if (styleStackDepth == 0) {
      throw new RuntimeException("Too many popStyle() without enough pushStyle()");
    }
    styleStackDepth--;
    style(styleStack[styleStackDepth]);
  }


  public void style(PStyle s) {
    //  if (s.smooth) {
    //    smooth();
    //  } else {
    //    noSmooth();
    //  }

    imageMode(s.imageMode);
    rectMode(s.rectMode);
    ellipseMode(s.ellipseMode);
    shapeMode(s.shapeMode);

    if (blendMode != s.blendMode) {
      blendMode(s.blendMode);
    }

    if (s.tint) {
      tint(s.tintColor);
    } else {
      noTint();
    }
    if (s.fill) {
      fill(s.fillColor);
    } else {
      noFill();
    }
    if (s.stroke) {
      stroke(s.strokeColor);
    } else {
      noStroke();
    }
    strokeWeight(s.strokeWeight);
    strokeCap(s.strokeCap);
    strokeJoin(s.strokeJoin);

    // Set the colorMode() for the material properties.
    // TODO this is really inefficient, need to just have a material() method,
    // but this has the least impact to the API.
    colorMode(RGB, 1);
    ambient(s.ambientR, s.ambientG, s.ambientB);
    emissive(s.emissiveR, s.emissiveG, s.emissiveB);
    specular(s.specularR, s.specularG, s.specularB);
    shininess(s.shininess);

    /*
  s.ambientR = ambientR;
  s.ambientG = ambientG;
  s.ambientB = ambientB;
  s.specularR = specularR;
  s.specularG = specularG;
  s.specularB = specularB;
  s.emissiveR = emissiveR;
  s.emissiveG = emissiveG;
  s.emissiveB = emissiveB;
  s.shininess = shininess;
     */
    //  material(s.ambientR, s.ambientG, s.ambientB,
    //           s.emissiveR, s.emissiveG, s.emissiveB,
    //           s.specularR, s.specularG, s.specularB,
    //           s.shininess);

    // Set this after the material properties.
    colorMode(s.colorMode,
              s.colorModeX, s.colorModeY, s.colorModeZ, s.colorModeA);

    // This is a bit asymmetric, since there's no way to do "noFont()",
    // and a null textFont will produce an error (since usually that means that
    // the font couldn't load properly). So in some cases, the font won't be
    // 'cleared' to null, even though that's technically correct.
    if (s.textFont != null) {
      textFont(s.textFont, s.textSize);
      textLeading(s.textLeading);
    }
    // These don't require a font to be set.
    textAlign(s.textAlign, s.textAlignY);
    textMode(s.textMode);
  }


  public PStyle getStyle() {  // ignore
    return getStyle(null);
  }


  public PStyle getStyle(PStyle s) {  // ignore
    if (s == null) {
      s = new PStyle();
    }

    s.imageMode = imageMode;
    s.rectMode = rectMode;
    s.ellipseMode = ellipseMode;
    s.shapeMode = shapeMode;

    s.blendMode = blendMode;

    s.colorMode = colorMode;
    s.colorModeX = colorModeX;
    s.colorModeY = colorModeY;
    s.colorModeZ = colorModeZ;
    s.colorModeA = colorModeA;

    s.tint = tint;
    s.tintColor = tintColor;
    s.fill = fill;
    s.fillColor = fillColor;
    s.stroke = stroke;
    s.strokeColor = strokeColor;
    s.strokeWeight = strokeWeight;
    s.strokeCap = strokeCap;
    s.strokeJoin = strokeJoin;

    s.ambientR = ambientR;
    s.ambientG = ambientG;
    s.ambientB = ambientB;
    s.specularR = specularR;
    s.specularG = specularG;
    s.specularB = specularB;
    s.emissiveR = emissiveR;
    s.emissiveG = emissiveG;
    s.emissiveB = emissiveB;
    s.shininess = shininess;

    s.textFont = textFont;
    s.textAlign = textAlign;
    s.textAlignY = textAlignY;
    s.textMode = textMode;
    s.textSize = textSize;
    s.textLeading = textLeading;

    return s;
  }



  //////////////////////////////////////////////////////////////

  // STROKE CAP/JOIN/WEIGHT

  /**
   * ( begin auto-generated from strokeWeight.xml )
   *
   * Sets the width of the stroke used for lines, points, and the border
   * around shapes. All widths are set in units of pixels.
   * <br/> <br/>
   * When drawing with P3D, series of connected lines (such as the stroke
   * around a polygon, triangle, or ellipse) produce unattractive results
   * when a thick stroke weight is set (<a
   * href="http://code.google.com/p/processing/issues/detail?id=123">see
   * Issue 123</a>). With P3D, the minimum and maximum values for
   * <b>strokeWeight()</b> are controlled by the graphics card and the
   * operating system's OpenGL implementation. For instance, the thickness
   * may not go higher than 10 pixels.
   *
   * ( end auto-generated )
   *
   * @webref shape:attributes
   * @param weight the weight (in pixels) of the stroke
   * @see PGraphics#stroke(int, float)
   * @see PGraphics#strokeJoin(int)
   * @see PGraphics#strokeCap(int)
   */
  public void strokeWeight(float weight) {
    strokeWeight = weight;
  }

  /**
   * ( begin auto-generated from strokeJoin.xml )
   *
   * Sets the style of the joints which connect line segments. These joints
   * are either mitered, beveled, or rounded and specified with the
   * corresponding parameters MITER, BEVEL, and ROUND. The default joint is
   * MITER.
   * <br/> <br/>
   * This function is not available with the P3D renderer, (<a
   * href="http://code.google.com/p/processing/issues/detail?id=123">see
   * Issue 123</a>). More information about the renderers can be found in the
   * <b>size()</b> reference.
   *
   * ( end auto-generated )
   *
   * @webref shape:attributes
   * @param join either MITER, BEVEL, ROUND
   * @see PGraphics#stroke(int, float)
   * @see PGraphics#strokeWeight(float)
   * @see PGraphics#strokeCap(int)
   */
  public void strokeJoin(int join) {
    strokeJoin = join;
  }

  /**
   * ( begin auto-generated from strokeCap.xml )
   *
   * Sets the style for rendering line endings. These ends are either
   * squared, extended, or rounded and specified with the corresponding
   * parameters SQUARE, PROJECT, and ROUND. The default cap is ROUND.
   * <br/> <br/>
   * This function is not available with the P3D renderer (<a
   * href="http://code.google.com/p/processing/issues/detail?id=123">see
   * Issue 123</a>). More information about the renderers can be found in the
   * <b>size()</b> reference.
   *
   * ( end auto-generated )
   *
   * @webref shape:attributes
   * @param cap either SQUARE, PROJECT, or ROUND
   * @see PGraphics#stroke(int, float)
   * @see PGraphics#strokeWeight(float)
   * @see PGraphics#strokeJoin(int)
   * @see PApplet#size(int, int, String, String)
   */
  public void strokeCap(int cap) {
    strokeCap = cap;
  }



  //////////////////////////////////////////////////////////////

  // STROKE COLOR


  /**
   * ( begin auto-generated from noStroke.xml )
   *
   * Disables drawing the stroke (outline). If both <b>noStroke()</b> and
   * <b>noFill()</b> are called, nothing will be drawn to the screen.
   *
   * ( end auto-generated )
   *
   * @webref color:setting
   * @see PGraphics#stroke(int, float)
   * @see PGraphics#fill(float, float, float, float)
   * @see PGraphics#noFill()
   */
  public void noStroke() {
    stroke = false;
  }


  /**
   * ( begin auto-generated from stroke.xml )
   *
   * Sets the color used to draw lines and borders around shapes. This color
   * is either specified in terms of the RGB or HSB color depending on the
   * current <b>colorMode()</b> (the default color space is RGB, with each
   * value in the range from 0 to 255).
   * <br/> <br/>
   * When using hexadecimal notation to specify a color, use "#" or "0x"
   * before the values (e.g. #CCFFAA, 0xFFCCFFAA). The # syntax uses six
   * digits to specify a color (the way colors are specified in HTML and
   * CSS). When using the hexadecimal notation starting with "0x", the
   * hexadecimal value must be specified with eight characters; the first two
   * characters define the alpha component and the remainder the red, green,
   * and blue components.
   * <br/> <br/>
   * The value for the parameter "gray" must be less than or equal to the
   * current maximum value as specified by <b>colorMode()</b>. The default
   * maximum value is 255.
   *
   * ( end auto-generated )
   *
   * @param rgb color value in hexadecimal notation
   * @see PGraphics#noStroke()
   * @see PGraphics#strokeWeight(float)
   * @see PGraphics#strokeJoin(int)
   * @see PGraphics#strokeCap(int)
   * @see PGraphics#fill(int, float)
   * @see PGraphics#noFill()
   * @see PGraphics#tint(int, float)
   * @see PGraphics#background(float, float, float, float)
   * @see PGraphics#colorMode(int, float, float, float, float)
   */
  public void stroke(int rgb) {
    colorCalc(rgb);
    strokeFromCalc();
  }


  /**
   * @param alpha opacity of the stroke
   */
  public void stroke(int rgb, float alpha) {
    colorCalc(rgb, alpha);
    strokeFromCalc();
  }


  /**
   * @param gray specifies a value between white and black
   */
  public void stroke(float gray) {
    colorCalc(gray);
    strokeFromCalc();
  }


  public void stroke(float gray, float alpha) {
    colorCalc(gray, alpha);
    strokeFromCalc();
  }


  /**
   * @param v1 red or hue value (depending on current color mode)
   * @param v2 green or saturation value (depending on current color mode)
   * @param v3 blue or brightness value (depending on current color mode)
   * @webref color:setting
   */
  public void stroke(float v1, float v2, float v3) {
    colorCalc(v1, v2, v3);
    strokeFromCalc();
  }


  public void stroke(float v1, float v2, float v3, float alpha) {
    colorCalc(v1, v2, v3, alpha);
    strokeFromCalc();
  }


  protected void strokeFromCalc() {
    stroke = true;
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

  // TINT COLOR


  /**
   * ( begin auto-generated from noTint.xml )
   *
   * Removes the current fill value for displaying images and reverts to
   * displaying images with their original hues.
   *
   * ( end auto-generated )
   *
   * @webref image:loading_displaying
   * @usage web_application
   * @see PGraphics#tint(float, float, float, float)
   * @see PGraphics#image(PImage, float, float, float, float)
   */
  public void noTint() {
    tint = false;
  }


  /**
   * ( begin auto-generated from tint.xml )
   *
   * Sets the fill value for displaying images. Images can be tinted to
   * specified colors or made transparent by setting the alpha.<br />
   * <br />
   * To make an image transparent, but not change it's color, use white as
   * the tint color and specify an alpha value. For instance, tint(255, 128)
   * will make an image 50% transparent (unless <b>colorMode()</b> has been
   * used).<br />
   * <br />
   * When using hexadecimal notation to specify a color, use "#" or "0x"
   * before the values (e.g. #CCFFAA, 0xFFCCFFAA). The # syntax uses six
   * digits to specify a color (the way colors are specified in HTML and
   * CSS). When using the hexadecimal notation starting with "0x", the
   * hexadecimal value must be specified with eight characters; the first two
   * characters define the alpha component and the remainder the red, green,
   * and blue components.<br />
   * <br />
   * The value for the parameter "gray" must be less than or equal to the
   * current maximum value as specified by <b>colorMode()</b>. The default
   * maximum value is 255.<br />
   * <br />
   * The <b>tint()</b> function is also used to control the coloring of
   * textures in 3D.
   *
   * ( end auto-generated )
   *
   * @webref image:loading_displaying
   * @usage web_application
   * @param rgb color value in hexadecimal notation
   * @see PGraphics#noTint()
   * @see PGraphics#image(PImage, float, float, float, float)
   */
  public void tint(int rgb) {
    colorCalc(rgb);
    tintFromCalc();
  }


  /**
   * @param alpha opacity of the image
   */
  public void tint(int rgb, float alpha) {
    colorCalc(rgb, alpha);
    tintFromCalc();
  }


  /**
   * @param gray specifies a value between white and black
   */
  public void tint(float gray) {
    colorCalc(gray);
    tintFromCalc();
  }


  public void tint(float gray, float alpha) {
    colorCalc(gray, alpha);
    tintFromCalc();
  }

/**
 * @param v1 red or hue value (depending on current color mode)
 * @param v2 green or saturation value (depending on current color mode)
 * @param v3 blue or brightness value (depending on current color mode)
 */
  public void tint(float v1, float v2, float v3) {
    colorCalc(v1, v2, v3);
    tintFromCalc();
  }


  public void tint(float v1, float v2, float v3, float alpha) {
    colorCalc(v1, v2, v3, alpha);
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

  // FILL COLOR


  /**
   * ( begin auto-generated from noFill.xml )
   *
   * Disables filling geometry. If both <b>noStroke()</b> and <b>noFill()</b>
   * are called, nothing will be drawn to the screen.
   *
   * ( end auto-generated )
   *
   * @webref color:setting
   * @usage web_application
   * @see PGraphics#fill(float, float, float, float)
   * @see PGraphics#stroke(int, float)
   * @see PGraphics#noStroke()
   */
  public void noFill() {
    fill = false;
  }


  /**
   * ( begin auto-generated from fill.xml )
   *
   * Sets the color used to fill shapes. For example, if you run <b>fill(204,
   * 102, 0)</b>, all subsequent shapes will be filled with orange. This
   * color is either specified in terms of the RGB or HSB color depending on
   * the current <b>colorMode()</b> (the default color space is RGB, with
   * each value in the range from 0 to 255).
   * <br/> <br/>
   * When using hexadecimal notation to specify a color, use "#" or "0x"
   * before the values (e.g. #CCFFAA, 0xFFCCFFAA). The # syntax uses six
   * digits to specify a color (the way colors are specified in HTML and
   * CSS). When using the hexadecimal notation starting with "0x", the
   * hexadecimal value must be specified with eight characters; the first two
   * characters define the alpha component and the remainder the red, green,
   * and blue components.
   * <br/> <br/>
   * The value for the parameter "gray" must be less than or equal to the
   * current maximum value as specified by <b>colorMode()</b>. The default
   * maximum value is 255.
   * <br/> <br/>
   * To change the color of an image (or a texture), use tint().
   *
   * ( end auto-generated )
   *
   * @webref color:setting
   * @usage web_application
   * @param rgb color variable or hex value
   * @see PGraphics#noFill()
   * @see PGraphics#stroke(int, float)
   * @see PGraphics#noStroke()
   * @see PGraphics#tint(int, float)
   * @see PGraphics#background(float, float, float, float)
   * @see PGraphics#colorMode(int, float, float, float, float)
   */
  public void fill(int rgb) {
    colorCalc(rgb);
    fillFromCalc();
  }

  /**
   * @param alpha opacity of the fill
   */
  public void fill(int rgb, float alpha) {
    colorCalc(rgb, alpha);
    fillFromCalc();
  }


  /**
   * @param gray number specifying value between white and black
   */
  public void fill(float gray) {
    colorCalc(gray);
    fillFromCalc();
  }


  public void fill(float gray, float alpha) {
    colorCalc(gray, alpha);
    fillFromCalc();
  }


  /**
   * @param v1 red or hue value (depending on current color mode)
   * @param v2 green or saturation value (depending on current color mode)
   * @param v3 blue or brightness value (depending on current color mode)
   */
  public void fill(float v1, float v2, float v3) {
    colorCalc(v1, v2, v3);
    fillFromCalc();
  }


  public void fill(float v1, float v2, float v3, float alpha) {
    colorCalc(v1, v2, v3, alpha);
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

  // MATERIAL PROPERTIES

  /**
   * ( begin auto-generated from ambient.xml )
   *
   * Sets the ambient reflectance for shapes drawn to the screen. This is
   * combined with the ambient light component of environment. The color
   * components set through the parameters define the reflectance. For
   * example in the default color mode, setting v1=255, v2=126, v3=0, would
   * cause all the red light to reflect and half of the green light to
   * reflect. Used in combination with <b>emissive()</b>, <b>specular()</b>,
   * and <b>shininess()</b> in setting the material properties of shapes.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:material_properties
   * @usage web_application
   * @param rgb any value of the color datatype
   * @see PGraphics#emissive(float, float, float)
   * @see PGraphics#specular(float, float, float)
   * @see PGraphics#shininess(float)
   */
  public void ambient(int rgb) {
//    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {
//      ambient((float) rgb);
//
//    } else {
//      colorCalcARGB(rgb, colorModeA);
//      ambientFromCalc();
//    }
    colorCalc(rgb);
    ambientFromCalc();
  }

/**
 * @param gray number specifying value between white and black
 */
  public void ambient(float gray) {
    colorCalc(gray);
    ambientFromCalc();
  }

/**
 * @param v1 red or hue value (depending on current color mode)
 * @param v2 green or saturation value (depending on current color mode)
 * @param v3 blue or brightness value (depending on current color mode)
 */
  public void ambient(float v1, float v2, float v3) {
    colorCalc(v1, v2, v3);
    ambientFromCalc();
  }


  protected void ambientFromCalc() {
    ambientColor = calcColor;
    ambientR = calcR;
    ambientG = calcG;
    ambientB = calcB;
    setAmbient = true;
  }

  /**
   * ( begin auto-generated from specular.xml )
   *
   * Sets the specular color of the materials used for shapes drawn to the
   * screen, which sets the color of hightlights. Specular refers to light
   * which bounces off a surface in a perferred direction (rather than
   * bouncing in all directions like a diffuse light). Used in combination
   * with <b>emissive()</b>, <b>ambient()</b>, and <b>shininess()</b> in
   * setting the material properties of shapes.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:material_properties
   * @usage web_application
   * @param rgb color to set
   * @see PGraphics#lightSpecular(float, float, float)
   * @see PGraphics#ambient(float, float, float)
   * @see PGraphics#emissive(float, float, float)
   * @see PGraphics#shininess(float)
   */
  public void specular(int rgb) {
//    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {
//      specular((float) rgb);
//
//    } else {
//      colorCalcARGB(rgb, colorModeA);
//      specularFromCalc();
//    }
    colorCalc(rgb);
    specularFromCalc();
  }


/**
 * gray number specifying value between white and black
 */
  public void specular(float gray) {
    colorCalc(gray);
    specularFromCalc();
  }


/**
 * @param v1 red or hue value (depending on current color mode)
 * @param v2 green or saturation value (depending on current color mode)
 * @param v3 blue or brightness value (depending on current color mode)
 */
  public void specular(float v1, float v2, float v3) {
    colorCalc(v1, v2, v3);
    specularFromCalc();
  }


  protected void specularFromCalc() {
    specularColor = calcColor;
    specularR = calcR;
    specularG = calcG;
    specularB = calcB;
  }


  /**
   * ( begin auto-generated from shininess.xml )
   *
   * Sets the amount of gloss in the surface of shapes. Used in combination
   * with <b>ambient()</b>, <b>specular()</b>, and <b>emissive()</b> in
   * setting the material properties of shapes.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:material_properties
   * @usage web_application
   * @param shine degree of shininess
   * @see PGraphics#emissive(float, float, float)
   * @see PGraphics#ambient(float, float, float)
   * @see PGraphics#specular(float, float, float)
   */
  public void shininess(float shine) {
    shininess = shine;
  }

  /**
   * ( begin auto-generated from emissive.xml )
   *
   * Sets the emissive color of the material used for drawing shapes drawn to
   * the screen. Used in combination with <b>ambient()</b>,
   * <b>specular()</b>, and <b>shininess()</b> in setting the material
   * properties of shapes.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:material_properties
   * @usage web_application
   * @param rgb color to set
   * @see PGraphics#ambient(float, float, float)
   * @see PGraphics#specular(float, float, float)
   * @see PGraphics#shininess(float)
   */
  public void emissive(int rgb) {
//    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {
//      emissive((float) rgb);
//
//    } else {
//      colorCalcARGB(rgb, colorModeA);
//      emissiveFromCalc();
//    }
    colorCalc(rgb);
    emissiveFromCalc();
  }

  /**
   * gray number specifying value between white and black
   */
  public void emissive(float gray) {
    colorCalc(gray);
    emissiveFromCalc();
  }

  /**
   * @param v1 red or hue value (depending on current color mode)
   * @param v2 green or saturation value (depending on current color mode)
   * @param v3 blue or brightness value (depending on current color mode)
   */
  public void emissive(float v1, float v2, float v3) {
    colorCalc(v1, v2, v3);
    emissiveFromCalc();
  }


  protected void emissiveFromCalc() {
    emissiveColor = calcColor;
    emissiveR = calcR;
    emissiveG = calcG;
    emissiveB = calcB;
  }



  //////////////////////////////////////////////////////////////

  // LIGHTS

  // The details of lighting are very implementation-specific, so this base
  // class does not handle any details of settings lights. It does however
  // display warning messages that the functions are not available.

  /**
   * ( begin auto-generated from lights.xml )
   *
   * Sets the default ambient light, directional light, falloff, and specular
   * values. The defaults are ambientLight(128, 128, 128) and
   * directionalLight(128, 128, 128, 0, 0, -1), lightFalloff(1, 0, 0), and
   * lightSpecular(0, 0, 0). Lights need to be included in the draw() to
   * remain persistent in a looping program. Placing them in the setup() of a
   * looping program will cause them to only have an effect the first time
   * through the loop.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:lights
   * @usage web_application
   * @see PGraphics#ambientLight(float, float, float, float, float, float)
   * @see PGraphics#directionalLight(float, float, float, float, float, float)
   * @see PGraphics#pointLight(float, float, float, float, float, float)
   * @see PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
   * @see PGraphics#noLights()
   */
  public void lights() {
    showMethodWarning("lights");
  }

  /**
   * ( begin auto-generated from noLights.xml )
   *
   * Disable all lighting. Lighting is turned off by default and enabled with
   * the <b>lights()</b> function. This function can be used to disable
   * lighting so that 2D geometry (which does not require lighting) can be
   * drawn after a set of lighted 3D geometry.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:lights
   * @usage web_application
   * @see PGraphics#lights()
   */
  public void noLights() {
    showMethodWarning("noLights");
  }

  /**
   * ( begin auto-generated from ambientLight.xml )
   *
   * Adds an ambient light. Ambient light doesn't come from a specific
   * direction, the rays have light have bounced around so much that objects
   * are evenly lit from all sides. Ambient lights are almost always used in
   * combination with other types of lights. Lights need to be included in
   * the <b>draw()</b> to remain persistent in a looping program. Placing
   * them in the <b>setup()</b> of a looping program will cause them to only
   * have an effect the first time through the loop. The effect of the
   * parameters is determined by the current color mode.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:lights
   * @usage web_application
   * @param v1 red or hue value (depending on current color mode)
   * @param v2 green or saturation value (depending on current color mode)
   * @param v3 blue or brightness value (depending on current color mode)
   * @see PGraphics#lights()
   * @see PGraphics#directionalLight(float, float, float, float, float, float)
   * @see PGraphics#pointLight(float, float, float, float, float, float)
   * @see PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
   */
  public void ambientLight(float v1, float v2, float v3) {
    showMethodWarning("ambientLight");
  }

  /**
   * @param x x-coordinate of the light
   * @param y y-coordinate of the light
   * @param z z-coordinate of the light
   */
  public void ambientLight(float v1, float v2, float v3,
                           float x, float y, float z) {
    showMethodWarning("ambientLight");
  }

  /**
   * ( begin auto-generated from directionalLight.xml )
   *
   * Adds a directional light. Directional light comes from one direction and
   * is stronger when hitting a surface squarely and weaker if it hits at a a
   * gentle angle. After hitting a surface, a directional lights scatters in
   * all directions. Lights need to be included in the <b>draw()</b> to
   * remain persistent in a looping program. Placing them in the
   * <b>setup()</b> of a looping program will cause them to only have an
   * effect the first time through the loop. The affect of the <b>v1</b>,
   * <b>v2</b>, and <b>v3</b> parameters is determined by the current color
   * mode. The <b>nx</b>, <b>ny</b>, and <b>nz</b> parameters specify the
   * direction the light is facing. For example, setting <b>ny</b> to -1 will
   * cause the geometry to be lit from below (the light is facing directly upward).
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:lights
   * @usage web_application
   * @param v1 red or hue value (depending on current color mode)
   * @param v2 green or saturation value (depending on current color mode)
   * @param v3 blue or brightness value (depending on current color mode)
   * @param nx direction along the x-axis
   * @param ny direction along the y-axis
   * @param nz direction along the z-axis
   * @see PGraphics#lights()
   * @see PGraphics#ambientLight(float, float, float, float, float, float)
   * @see PGraphics#pointLight(float, float, float, float, float, float)
   * @see PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
   */
  public void directionalLight(float v1, float v2, float v3,
                               float nx, float ny, float nz) {
    showMethodWarning("directionalLight");
  }

  /**
   * ( begin auto-generated from pointLight.xml )
   *
   * Adds a point light. Lights need to be included in the <b>draw()</b> to
   * remain persistent in a looping program. Placing them in the
   * <b>setup()</b> of a looping program will cause them to only have an
   * effect the first time through the loop. The affect of the <b>v1</b>,
   * <b>v2</b>, and <b>v3</b> parameters is determined by the current color
   * mode. The <b>x</b>, <b>y</b>, and <b>z</b> parameters set the position
   * of the light.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:lights
   * @usage web_application
   * @param v1 red or hue value (depending on current color mode)
   * @param v2 green or saturation value (depending on current color mode)
   * @param v3 blue or brightness value (depending on current color mode)
   * @param x x-coordinate of the light
   * @param y y-coordinate of the light
   * @param z z-coordinate of the light
   * @see PGraphics#lights()
   * @see PGraphics#directionalLight(float, float, float, float, float, float)
   * @see PGraphics#ambientLight(float, float, float, float, float, float)
   * @see PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
   */
  public void pointLight(float v1, float v2, float v3,
                         float x, float y, float z) {
    showMethodWarning("pointLight");
  }

  /**
   * ( begin auto-generated from spotLight.xml )
   *
   * Adds a spot light. Lights need to be included in the <b>draw()</b> to
   * remain persistent in a looping program. Placing them in the
   * <b>setup()</b> of a looping program will cause them to only have an
   * effect the first time through the loop. The affect of the <b>v1</b>,
   * <b>v2</b>, and <b>v3</b> parameters is determined by the current color
   * mode. The <b>x</b>, <b>y</b>, and <b>z</b> parameters specify the
   * position of the light and <b>nx</b>, <b>ny</b>, <b>nz</b> specify the
   * direction or light. The <b>angle</b> parameter affects angle of the
   * spotlight cone.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:lights
   * @usage web_application
   * @param v1 red or hue value (depending on current color mode)
   * @param v2 green or saturation value (depending on current color mode)
   * @param v3 blue or brightness value (depending on current color mode)
   * @param x x-coordinate of the light
   * @param y y-coordinate of the light
   * @param z z-coordinate of the light
   * @param nx direction along the x axis
   * @param ny direction along the y axis
   * @param nz direction along the z axis
   * @param angle angle of the spotlight cone
   * @param concentration exponent determining the center bias of the cone
   * @see PGraphics#lights()
   * @see PGraphics#directionalLight(float, float, float, float, float, float)
   * @see PGraphics#pointLight(float, float, float, float, float, float)
   * @see PGraphics#ambientLight(float, float, float, float, float, float)
   */
  public void spotLight(float v1, float v2, float v3,
                        float x, float y, float z,
                        float nx, float ny, float nz,
                        float angle, float concentration) {
    showMethodWarning("spotLight");
  }

  /**
   * ( begin auto-generated from lightFalloff.xml )
   *
   * Sets the falloff rates for point lights, spot lights, and ambient
   * lights. The parameters are used to determine the falloff with the
   * following equation:<br /><br />d = distance from light position to
   * vertex position<br />falloff = 1 / (CONSTANT + d * LINEAR + (d*d) *
   * QUADRATIC)<br /><br />Like <b>fill()</b>, it affects only the elements
   * which are created after it in the code. The default value if
   * <b>LightFalloff(1.0, 0.0, 0.0)</b>. Thinking about an ambient light with
   * a falloff can be tricky. It is used, for example, if you wanted a region
   * of your scene to be lit ambiently one color and another region to be lit
   * ambiently by another color, you would use an ambient light with location
   * and falloff. You can think of it as a point light that doesn't care
   * which direction a surface is facing.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:lights
   * @usage web_application
   * @param constant constant value or determining falloff
   * @param linear linear value for determining falloff
   * @param quadratic quadratic value for determining falloff
   * @see PGraphics#lights()
   * @see PGraphics#ambientLight(float, float, float, float, float, float)
   * @see PGraphics#pointLight(float, float, float, float, float, float)
   * @see PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
   * @see PGraphics#lightSpecular(float, float, float)
   */
  public void lightFalloff(float constant, float linear, float quadratic) {
    showMethodWarning("lightFalloff");
  }

  /**
   * ( begin auto-generated from lightSpecular.xml )
   *
   * Sets the specular color for lights. Like <b>fill()</b>, it affects only
   * the elements which are created after it in the code. Specular refers to
   * light which bounces off a surface in a perferred direction (rather than
   * bouncing in all directions like a diffuse light) and is used for
   * creating highlights. The specular quality of a light interacts with the
   * specular material qualities set through the <b>specular()</b> and
   * <b>shininess()</b> functions.
   *
   * ( end auto-generated )
   *
   * @webref lights_camera:lights
   * @usage web_application
   * @param v1 red or hue value (depending on current color mode)
   * @param v2 green or saturation value (depending on current color mode)
   * @param v3 blue or brightness value (depending on current color mode)
   * @see PGraphics#specular(float, float, float)
   * @see PGraphics#lights()
   * @see PGraphics#ambientLight(float, float, float, float, float, float)
   * @see PGraphics#pointLight(float, float, float, float, float, float)
   * @see PGraphics#spotLight(float, float, float, float, float, float, float, float, float, float, float)
   */
  public void lightSpecular(float v1, float v2, float v3) {
    showMethodWarning("lightSpecular");
  }



  //////////////////////////////////////////////////////////////

  // BACKGROUND


  /**
   * ( begin auto-generated from background.xml )
   *
   * The <b>background()</b> function sets the color used for the background
   * of the Processing window. The default background is light gray. In the
   * <b>draw()</b> function, the background color is used to clear the
   * display window at the beginning of each frame.
   * <br/> <br/>
   * An image can also be used as the background for a sketch, however its
   * width and height must be the same size as the sketch window. To resize
   * an image 'b' to the size of the sketch window, use b.resize(width, height).
   * <br/> <br/>
   * Images used as background will ignore the current <b>tint()</b> setting.
   * <br/> <br/>
   * It is not possible to use transparency (alpha) in background colors with
   * the main drawing surface, however they will work properly with <b>createGraphics()</b>.
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   * <p>Clear the background with a color that includes an alpha value. This can
   * only be used with objects created by createGraphics(), because the main
   * drawing surface cannot be set transparent.</p>
   * <p>It might be tempting to use this function to partially clear the screen
   * on each frame, however that's not how this function works. When calling
   * background(), the pixels will be replaced with pixels that have that level
   * of transparency. To do a semi-transparent overlay, use fill() with alpha
   * and draw a rectangle.</p>
   *
   * @webref color:setting
   * @usage web_application
   * @param rgb any value of the color datatype
   * @see PGraphics#stroke(float)
   * @see PGraphics#fill(float)
   * @see PGraphics#tint(float)
   * @see PGraphics#colorMode(int)
   */
  public void background(int rgb) {
//    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {
//      background((float) rgb);
//
//    } else {
//      if (format == RGB) {
//        rgb |= 0xff000000;  // ignore alpha for main drawing surface
//      }
//      colorCalcARGB(rgb, colorModeA);
//      backgroundFromCalc();
//      backgroundImpl();
//    }
    colorCalc(rgb);
    backgroundFromCalc();
  }


  /**
   * @param alpha opacity of the background
   */
  public void background(int rgb, float alpha) {
//    if (format == RGB) {
//      background(rgb);  // ignore alpha for main drawing surface
//
//    } else {
//      if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {
//        background((float) rgb, alpha);
//
//      } else {
//        colorCalcARGB(rgb, alpha);
//        backgroundFromCalc();
//        backgroundImpl();
//      }
//    }
    colorCalc(rgb, alpha);
    backgroundFromCalc();
  }


  /**
   * @param gray specifies a value between white and black
   */
  public void background(float gray) {
    colorCalc(gray);
    backgroundFromCalc();
//    backgroundImpl();
  }


  public void background(float gray, float alpha) {
    if (format == RGB) {
      background(gray);  // ignore alpha for main drawing surface

    } else {
      colorCalc(gray, alpha);
      backgroundFromCalc();
//      backgroundImpl();
    }
  }


  /**
   * @param v1 red or hue value (depending on the current color mode)
   * @param v2 green or saturation value (depending on the current color mode)
   * @param v3 blue or brightness value (depending on the current color mode)
   */
  public void background(float v1, float v2, float v3) {
    colorCalc(v1, v2, v3);
    backgroundFromCalc();
//    backgroundImpl();
  }


  public void background(float v1, float v2, float v3, float alpha) {
    colorCalc(v1, v2, v3, alpha);
    backgroundFromCalc();
  }

  /**
   * @webref color:setting
   */
  public void clear() {
    background(0, 0, 0, 0);
  }


  protected void backgroundFromCalc() {
    backgroundR = calcR;
    backgroundG = calcG;
    backgroundB = calcB;
    //backgroundA = (format == RGB) ? colorModeA : calcA;
    // If drawing surface is opaque, this maxes out at 1.0. [fry 150513]
    backgroundA = (format == RGB) ? 1 : calcA;
    backgroundRi = calcRi;
    backgroundGi = calcGi;
    backgroundBi = calcBi;
    backgroundAi = (format == RGB) ? 255 : calcAi;
    backgroundAlpha = (format == RGB) ? false : calcAlpha;
    backgroundColor = calcColor;

    backgroundImpl();
  }


  /**
   * Takes an RGB or ARGB image and sets it as the background.
   * The width and height of the image must be the same size as the sketch.
   * Use image.resize(width, height) to make short work of such a task.<br/>
   * <br/>
   * Note that even if the image is set as RGB, the high 8 bits of each pixel
   * should be set opaque (0xFF000000) because the image data will be copied
   * directly to the screen, and non-opaque background images may have strange
   * behavior. Use image.filter(OPAQUE) to handle this easily.<br/>
   * <br/>
   * When using 3D, this will also clear the zbuffer (if it exists).
   *
   * @param image PImage to set as background (must be same size as the sketch window)
   */
  public void background(PImage image) {
    if ((image.pixelWidth != pixelWidth) || (image.pixelHeight != pixelHeight)) {
      throw new RuntimeException(ERROR_BACKGROUND_IMAGE_SIZE);
    }
    if ((image.format != RGB) && (image.format != ARGB)) {
      throw new RuntimeException(ERROR_BACKGROUND_IMAGE_FORMAT);
    }
    backgroundColor = 0;  // just zero it out for images
    backgroundImpl(image);
  }


  /**
   * Actually set the background image. This is separated from the error
   * handling and other semantic goofiness that is shared across renderers.
   */
  protected void backgroundImpl(PImage image) {
    // blit image to the screen
    set(0, 0, image);
  }


  /**
   * Actual implementation of clearing the background, now that the
   * internal variables for background color have been set. Called by the
   * backgroundFromCalc() method, which is what all the other background()
   * methods call once the work is done.
   */
  protected void backgroundImpl() {
    pushStyle();
    pushMatrix();
    resetMatrix();
    noStroke();
    fill(backgroundColor);
    rect(0, 0, width, height);
    popMatrix();
    popStyle();
  }


  /**
   * Callback to handle clearing the background when begin/endRaw is in use.
   * Handled as separate function for OpenGL (or other) subclasses that
   * override backgroundImpl() but still needs this to work properly.
   */
//  protected void backgroundRawImpl() {
//    if (raw != null) {
//      raw.colorMode(RGB, 1);
//      raw.noStroke();
//      raw.fill(backgroundR, backgroundG, backgroundB);
//      raw.beginShape(TRIANGLES);
//
//      raw.vertex(0, 0);
//      raw.vertex(width, 0);
//      raw.vertex(0, height);
//
//      raw.vertex(width, 0);
//      raw.vertex(width, height);
//      raw.vertex(0, height);
//
//      raw.endShape();
//    }
//  }



  //////////////////////////////////////////////////////////////

  // COLOR MODE

  /**
   * ( begin auto-generated from colorMode.xml )
   *
   * Changes the way Processing interprets color data. By default, the
   * parameters for <b>fill()</b>, <b>stroke()</b>, <b>background()</b>, and
   * <b>color()</b> are defined by values between 0 and 255 using the RGB
   * color model. The <b>colorMode()</b> function is used to change the
   * numerical range used for specifying colors and to switch color systems.
   * For example, calling <b>colorMode(RGB, 1.0)</b> will specify that values
   * are specified between 0 and 1. The limits for defining colors are
   * altered by setting the parameters range1, range2, range3, and range 4.
   *
   * ( end auto-generated )
   *
   * @webref color:setting
   * @usage web_application
   * @param mode Either RGB or HSB, corresponding to Red/Green/Blue and Hue/Saturation/Brightness
   * @see PGraphics#background(float)
   * @see PGraphics#fill(float)
   * @see PGraphics#stroke(float)
   */
  public void colorMode(int mode) {
    colorMode(mode, colorModeX, colorModeY, colorModeZ, colorModeA);
  }


  /**
   * @param max range for all color elements
   */
  public void colorMode(int mode, float max) {
    colorMode(mode, max, max, max, max);
  }


  /**
   * @param max1 range for the red or hue depending on the current color mode
   * @param max2 range for the green or saturation depending on the current color mode
   * @param max3 range for the blue or brightness depending on the current color mode
   */
  public void colorMode(int mode, float max1, float max2, float max3) {
    colorMode(mode, max1, max2, max3, colorModeA);
  }


  /**
   * @param maxA range for the alpha
   */
  public void colorMode(int mode,
                        float max1, float max2, float max3, float maxA) {
    colorMode = mode;

    colorModeX = max1;  // still needs to be set for hsb
    colorModeY = max2;
    colorModeZ = max3;
    colorModeA = maxA;

    // if color max values are all 1, then no need to scale
    colorModeScale =
      ((maxA != 1) || (max1 != max2) || (max2 != max3) || (max3 != maxA));

    // if color is rgb/0..255 this will make it easier for the
    // red() green() etc functions
    colorModeDefault = (colorMode == RGB) &&
      (colorModeA == 255) && (colorModeX == 255) &&
      (colorModeY == 255) && (colorModeZ == 255);
  }



  //////////////////////////////////////////////////////////////

  // COLOR CALCULATIONS

  // Given input values for coloring, these functions will fill the calcXxxx
  // variables with values that have been properly filtered through the
  // current colorMode settings.

  // Renderers that need to subclass any drawing properties such as fill or
  // stroke will usally want to override methods like fillFromCalc (or the
  // same for stroke, ambient, etc.) That way the color calcuations are
  // covered by this based PGraphics class, leaving only a single function
  // to override/implement in the subclass.


  /**
   * Set the fill to either a grayscale value or an ARGB int.
   * <P>
   * The problem with this code is that it has to detect between these two
   * situations automatically. This is done by checking to see if the high bits
   * (the alpha for 0xAA000000) is set, and if not, whether the color value
   * that follows is less than colorModeX (first param passed to colorMode).
   * <P>
   * This auto-detect would break in the following situation:
   * <PRE>size(256, 256);
   * for (int i = 0; i < 256; i++) {
   *   color c = color(0, 0, 0, i);
   *   stroke(c);
   *   line(i, 0, i, 256);
   * }</PRE>
   * ...on the first time through the loop, where (i == 0), since the color
   * itself is zero (black) then it would appear indistinguishable from code
   * that reads "fill(0)". The solution is to use the four parameter versions
   * of stroke or fill to more directly specify the desired result.
   */
  protected void colorCalc(int rgb) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {
      colorCalc((float) rgb);

    } else {
      colorCalcARGB(rgb, colorModeA);
    }
  }


  protected void colorCalc(int rgb, float alpha) {
    if (((rgb & 0xff000000) == 0) && (rgb <= colorModeX)) {  // see above
      colorCalc((float) rgb, alpha);

    } else {
      colorCalcARGB(rgb, alpha);
    }
  }


  protected void colorCalc(float gray) {
    colorCalc(gray, colorModeA);
  }


  protected void colorCalc(float gray, float alpha) {
    if (gray > colorModeX) gray = colorModeX;
    if (alpha > colorModeA) alpha = colorModeA;

    if (gray < 0) gray = 0;
    if (alpha < 0) alpha = 0;

    calcR = colorModeScale ? (gray / colorModeX) : gray;
    calcG = calcR;
    calcB = calcR;
    calcA = colorModeScale ? (alpha / colorModeA) : alpha;

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
      if (colorModeScale) {
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

      calcA = colorModeScale ? (a/colorModeA) : a;

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
   * Note, no need for a bounds check for 'argb' since it's a 32 bit number.
   * Bounds now checked on alpha, however (rev 0225).
   */
  protected void colorCalcARGB(int argb, float alpha) {
    if (alpha == colorModeA) {
      calcAi = (argb >> 24) & 0xff;
      calcColor = argb;
    } else {
      calcAi = (int) (((argb >> 24) & 0xff) * PApplet.constrain((alpha / colorModeA), 0, 1));
      calcColor = (calcAi << 24) | (argb & 0xFFFFFF);
    }
    calcRi = (argb >> 16) & 0xff;
    calcGi = (argb >> 8) & 0xff;
    calcBi = argb & 0xff;
    calcA = calcAi / 255.0f;
    calcR = calcRi / 255.0f;
    calcG = calcGi / 255.0f;
    calcB = calcBi / 255.0f;
    calcAlpha = (calcAi != 255);
  }



  //////////////////////////////////////////////////////////////

  // COLOR DATATYPE STUFFING

  // The 'color' primitive type in Processing syntax is in fact a 32-bit int.
  // These functions handle stuffing color values into a 32-bit cage based
  // on the current colorMode settings.

  // These functions are really slow (because they take the current colorMode
  // into account), but they're easy to use. Advanced users can write their
  // own bit shifting operations to setup 'color' data types.


  public final int color(int c) {  // ignore
//    if (((c & 0xff000000) == 0) && (c <= colorModeX)) {
//      if (colorModeDefault) {
//        // bounds checking to make sure the numbers aren't to high or low
//        if (c > 255) c = 255; else if (c < 0) c = 0;
//        return 0xff000000 | (c << 16) | (c << 8) | c;
//      } else {
//        colorCalc(c);
//      }
//    } else {
//      colorCalcARGB(c, colorModeA);
//    }
    colorCalc(c);
    return calcColor;
  }


  public final int color(float gray) {  // ignore
    colorCalc(gray);
    return calcColor;
  }


  /**
   * @param c can be packed ARGB or a gray in this case
   */
  public final int color(int c, int alpha) {  // ignore
//    if (colorModeDefault) {
//      // bounds checking to make sure the numbers aren't to high or low
//      if (c > 255) c = 255; else if (c < 0) c = 0;
//      if (alpha > 255) alpha = 255; else if (alpha < 0) alpha = 0;
//
//      return ((alpha & 0xff) << 24) | (c << 16) | (c << 8) | c;
//    }
    colorCalc(c, alpha);
    return calcColor;
  }


  /**
   * @param c can be packed ARGB or a gray in this case
   */
  public final int color(int c, float alpha) {  // ignore
//    if (((c & 0xff000000) == 0) && (c <= colorModeX)) {
    colorCalc(c, alpha);
//  } else {
//    colorCalcARGB(c, alpha);
//  }
    return calcColor;
  }


  public final int color(float gray, float alpha) {  // ignore
    colorCalc(gray, alpha);
    return calcColor;
  }


  public final int color(int v1, int v2, int v3) {  // ignore
    colorCalc(v1, v2, v3);
    return calcColor;
  }


  public final int color(float v1, float v2, float v3) {  // ignore
    colorCalc(v1, v2, v3);
    return calcColor;
  }


  public final int color(int v1, int v2, int v3, int a) {  // ignore
    colorCalc(v1, v2, v3, a);
    return calcColor;
  }


  public final int color(float v1, float v2, float v3, float a) {  // ignore
    colorCalc(v1, v2, v3, a);
    return calcColor;
  }



  //////////////////////////////////////////////////////////////

  // COLOR DATATYPE EXTRACTION

  // Vee have veys of making the colors talk.

  /**
   * ( begin auto-generated from alpha.xml )
   *
   * Extracts the alpha value from a color.
   *
   * ( end auto-generated )
   * @webref color:creating_reading
   * @usage web_application
   * @param rgb any value of the color datatype
   * @see PGraphics#red(int)
   * @see PGraphics#green(int)
   * @see PGraphics#blue(int)
   * @see PGraphics#hue(int)
   * @see PGraphics#saturation(int)
   * @see PGraphics#brightness(int)
   */
  public final float alpha(int rgb) {
    float outgoing = (rgb >> 24) & 0xff;
    if (colorModeA == 255) return outgoing;
    return (outgoing / 255.0f) * colorModeA;
  }


  /**
   * ( begin auto-generated from red.xml )
   *
   * Extracts the red value from a color, scaled to match current
   * <b>colorMode()</b>. This value is always returned as a  float so be
   * careful not to assign it to an int value.<br /><br />The red() function
   * is easy to use and undestand, but is slower than another technique. To
   * achieve the same results when working in <b>colorMode(RGB, 255)</b>, but
   * with greater speed, use the &gt;&gt; (right shift) operator with a bit
   * mask. For example, the following two lines of code are equivalent:<br
   * /><pre>float r1 = red(myColor);<br />float r2 = myColor &gt;&gt; 16
   * &amp; 0xFF;</pre>
   *
   * ( end auto-generated )
   *
   * @webref color:creating_reading
   * @usage web_application
   * @param rgb any value of the color datatype
   * @see PGraphics#green(int)
   * @see PGraphics#blue(int)
   * @see PGraphics#alpha(int)
   * @see PGraphics#hue(int)
   * @see PGraphics#saturation(int)
   * @see PGraphics#brightness(int)
   * @see_external rightshift
   */
  public final float red(int rgb) {
    float c = (rgb >> 16) & 0xff;
    if (colorModeDefault) return c;
    return (c / 255.0f) * colorModeX;
  }


  /**
   * ( begin auto-generated from green.xml )
   *
   * Extracts the green value from a color, scaled to match current
   * <b>colorMode()</b>. This value is always returned as a  float so be
   * careful not to assign it to an int value.<br /><br />The <b>green()</b>
   * function is easy to use and undestand, but is slower than another
   * technique. To achieve the same results when working in <b>colorMode(RGB,
   * 255)</b>, but with greater speed, use the &gt;&gt; (right shift)
   * operator with a bit mask. For example, the following two lines of code
   * are equivalent:<br /><pre>float r1 = green(myColor);<br />float r2 =
   * myColor &gt;&gt; 8 &amp; 0xFF;</pre>
   *
   * ( end auto-generated )
   *
   * @webref color:creating_reading
   * @usage web_application
   * @param rgb any value of the color datatype
   * @see PGraphics#red(int)
   * @see PGraphics#blue(int)
   * @see PGraphics#alpha(int)
   * @see PGraphics#hue(int)
   * @see PGraphics#saturation(int)
   * @see PGraphics#brightness(int)
   * @see_external rightshift
   */
  public final float green(int rgb) {
    float c = (rgb >> 8) & 0xff;
    if (colorModeDefault) return c;
    return (c / 255.0f) * colorModeY;
  }


  /**
   * ( begin auto-generated from blue.xml )
   *
   * Extracts the blue value from a color, scaled to match current
   * <b>colorMode()</b>. This value is always returned as a  float so be
   * careful not to assign it to an int value.<br /><br />The <b>blue()</b>
   * function is easy to use and undestand, but is slower than another
   * technique. To achieve the same results when working in <b>colorMode(RGB,
   * 255)</b>, but with greater speed, use a bit mask to remove the other
   * color components. For example, the following two lines of code are
   * equivalent:<br /><pre>float r1 = blue(myColor);<br />float r2 = myColor
   * &amp; 0xFF;</pre>
   *
   * ( end auto-generated )
   *
   * @webref color:creating_reading
   * @usage web_application
   * @param rgb any value of the color datatype
   * @see PGraphics#red(int)
   * @see PGraphics#green(int)
   * @see PGraphics#alpha(int)
   * @see PGraphics#hue(int)
   * @see PGraphics#saturation(int)
   * @see PGraphics#brightness(int)
   * @see_external rightshift
   */
  public final float blue(int rgb) {
    float c = (rgb) & 0xff;
    if (colorModeDefault) return c;
    return (c / 255.0f) * colorModeZ;
  }


  /**
   * ( begin auto-generated from hue.xml )
   *
   * Extracts the hue value from a color.
   *
   * ( end auto-generated )
   * @webref color:creating_reading
   * @usage web_application
   * @param rgb any value of the color datatype
   * @see PGraphics#red(int)
   * @see PGraphics#green(int)
   * @see PGraphics#blue(int)
   * @see PGraphics#alpha(int)
   * @see PGraphics#saturation(int)
   * @see PGraphics#brightness(int)
   */
  public final float hue(int rgb) {
    if (rgb != cacheHsbKey) {
      Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff,
                     rgb & 0xff, cacheHsbValue);
      cacheHsbKey = rgb;
    }
    return cacheHsbValue[0] * colorModeX;
  }


  /**
   * ( begin auto-generated from saturation.xml )
   *
   * Extracts the saturation value from a color.
   *
   * ( end auto-generated )
   * @webref color:creating_reading
   * @usage web_application
   * @param rgb any value of the color datatype
   * @see PGraphics#red(int)
   * @see PGraphics#green(int)
   * @see PGraphics#blue(int)
   * @see PGraphics#alpha(int)
   * @see PGraphics#hue(int)
   * @see PGraphics#brightness(int)
   */
  public final float saturation(int rgb) {
    if (rgb != cacheHsbKey) {
      Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff,
                     rgb & 0xff, cacheHsbValue);
      cacheHsbKey = rgb;
    }
    return cacheHsbValue[1] * colorModeY;
  }


  /**
   * ( begin auto-generated from brightness.xml )
   *
   * Extracts the brightness value from a color.
   *
   * ( end auto-generated )
   *
   * @webref color:creating_reading
   * @usage web_application
   * @param rgb any value of the color datatype
   * @see PGraphics#red(int)
   * @see PGraphics#green(int)
   * @see PGraphics#blue(int)
   * @see PGraphics#alpha(int)
   * @see PGraphics#hue(int)
   * @see PGraphics#saturation(int)
   */
  public final float brightness(int rgb) {
    if (rgb != cacheHsbKey) {
      Color.RGBtoHSB((rgb >> 16) & 0xff, (rgb >> 8) & 0xff,
                     rgb & 0xff, cacheHsbValue);
      cacheHsbKey = rgb;
    }
    return cacheHsbValue[2] * colorModeZ;
  }



  //////////////////////////////////////////////////////////////

  // COLOR DATATYPE INTERPOLATION

  // Against our better judgement.


  /**
   * ( begin auto-generated from lerpColor.xml )
   *
   * Calculates a color or colors between two color at a specific increment.
   * The <b>amt</b> parameter is the amount to interpolate between the two
   * values where 0.0 equal to the first point, 0.1 is very near the first
   * point, 0.5 is half-way in between, etc.
   *
   * ( end auto-generated )
   *
   * @webref color:creating_reading
   * @usage web_application
   * @param c1 interpolate from this color
   * @param c2 interpolate to this color
   * @param amt between 0.0 and 1.0
   * @see PImage#blendColor(int, int, int)
   * @see PGraphics#color(float, float, float, float)
   * @see PApplet#lerp(float, float, float)
   */
  public int lerpColor(int c1, int c2, float amt) {  // ignore
    return lerpColor(c1, c2, amt, colorMode);
  }

  static float[] lerpColorHSB1;
  static float[] lerpColorHSB2;

  /**
   * @nowebref
   * Interpolate between two colors. Like lerp(), but for the
   * individual color components of a color supplied as an int value.
   */
  static public int lerpColor(int c1, int c2, float amt, int mode) {
    if (amt < 0) amt = 0;
    if (amt > 1) amt = 1;

    if (mode == RGB) {
      float a1 = ((c1 >> 24) & 0xff);
      float r1 = (c1 >> 16) & 0xff;
      float g1 = (c1 >> 8) & 0xff;
      float b1 = c1 & 0xff;
      float a2 = (c2 >> 24) & 0xff;
      float r2 = (c2 >> 16) & 0xff;
      float g2 = (c2 >> 8) & 0xff;
      float b2 = c2 & 0xff;

      return ((PApplet.round(a1 + (a2-a1)*amt) << 24) |
              (PApplet.round(r1 + (r2-r1)*amt) << 16) |
              (PApplet.round(g1 + (g2-g1)*amt) << 8) |
              (PApplet.round(b1 + (b2-b1)*amt)));

    } else if (mode == HSB) {
      if (lerpColorHSB1 == null) {
        lerpColorHSB1 = new float[3];
        lerpColorHSB2 = new float[3];
      }

      float a1 = (c1 >> 24) & 0xff;
      float a2 = (c2 >> 24) & 0xff;
      int alfa = (PApplet.round(a1 + (a2-a1)*amt)) << 24;

      Color.RGBtoHSB((c1 >> 16) & 0xff, (c1 >> 8) & 0xff, c1 & 0xff,
                     lerpColorHSB1);
      Color.RGBtoHSB((c2 >> 16) & 0xff, (c2 >> 8) & 0xff, c2 & 0xff,
                     lerpColorHSB2);

      /* If mode is HSB, this will take the shortest path around the
       * color wheel to find the new color. For instance, red to blue
       * will go red violet blue (backwards in hue space) rather than
       * cycling through ROYGBIV.
       */
      // Disabling rollover (wasn't working anyway) for 0126.
      // Otherwise it makes full spectrum scale impossible for
      // those who might want it...in spite of how despicable
      // a full spectrum scale might be.
      // roll around when 0.9 to 0.1
      // more than 0.5 away means that it should roll in the other direction
      /*
      float h1 = lerpColorHSB1[0];
      float h2 = lerpColorHSB2[0];
      if (Math.abs(h1 - h2) > 0.5f) {
        if (h1 > h2) {
          // i.e. h1 is 0.7, h2 is 0.1
          h2 += 1;
        } else {
          // i.e. h1 is 0.1, h2 is 0.7
          h1 += 1;
        }
      }
      float ho = (PApplet.lerp(lerpColorHSB1[0], lerpColorHSB2[0], amt)) % 1.0f;
      */
      float ho = PApplet.lerp(lerpColorHSB1[0], lerpColorHSB2[0], amt);
      float so = PApplet.lerp(lerpColorHSB1[1], lerpColorHSB2[1], amt);
      float bo = PApplet.lerp(lerpColorHSB1[2], lerpColorHSB2[2], amt);

      return alfa | (Color.HSBtoRGB(ho, so, bo) & 0xFFFFFF);
    }
    return 0;
  }


  //////////////////////////////////////////////////////////////

  // BEGINRAW/ENDRAW


  /**
   * Record individual lines and triangles by echoing them to another renderer.
   */
  public void beginRaw(PGraphics rawGraphics) {  // ignore
    this.raw = rawGraphics;
    rawGraphics.beginDraw();
  }


  public void endRaw() {  // ignore
    if (raw != null) {
      // for 3D, need to flush any geometry that's been stored for sorting
      // (particularly if the ENABLE_DEPTH_SORT hint is set)
      flush();

      // just like beginDraw, this will have to be called because
      // endDraw() will be happening outside of draw()
      raw.endDraw();
      raw.dispose();
      raw = null;
    }
  }


  public boolean haveRaw() { // ignore
    return raw != null;
  }


  public PGraphics getRaw() { // ignore
    return raw;
  }


  //////////////////////////////////////////////////////////////

  // WARNINGS and EXCEPTIONS


  static protected Map<String, Object> warnings;


  /**
   * Show a renderer error, and keep track of it so that it's only shown once.
   * @param msg the error message (which will be stored for later comparison)
   */
  static public void showWarning(String msg) {  // ignore
    if (warnings == null) {
      warnings = new HashMap<>();
    }
    if (!warnings.containsKey(msg)) {
      System.err.println(msg);
      warnings.put(msg, new Object());
    }
  }


  /**
   * Version of showWarning() that takes a parsed String.
   */
  static public void showWarning(String msg, Object... args) {  // ignore
    showWarning(String.format(msg, args));
  }


  /**
   * Display a warning that the specified method is only available with 3D.
   * @param method The method name (no parentheses)
   */
  static public void showDepthWarning(String method) {
    showWarning(method + "() can only be used with a renderer that " +
                "supports 3D, such as P3D.");
  }


  /**
   * Display a warning that the specified method that takes x, y, z parameters
   * can only be used with x and y parameters in this renderer.
   * @param method The method name (no parentheses)
   */
  static public void showDepthWarningXYZ(String method) {
    showWarning(method + "() with x, y, and z coordinates " +
                "can only be used with a renderer that " +
                "supports 3D, such as P3D. " +
                "Use a version without a z-coordinate instead.");
  }


  /**
   * Display a warning that the specified method is simply unavailable.
   */
  static public void showMethodWarning(String method) {
    showWarning(method + "() is not available with this renderer.");
  }


  /**
   * Error that a particular variation of a method is unavailable (even though
   * other variations are). For instance, if vertex(x, y, u, v) is not
   * available, but vertex(x, y) is just fine.
   */
  static public void showVariationWarning(String str) {
    showWarning(str + " is not available with this renderer.");
  }


  /**
   * Display a warning that the specified method is not implemented, meaning
   * that it could be either a completely missing function, although other
   * variations of it may still work properly.
   */
  static public void showMissingWarning(String method) {
    showWarning(method + "(), or this particular variation of it, " +
                "is not available with this renderer.");
  }


  /**
   * Show an renderer-related exception that halts the program. Currently just
   * wraps the message as a RuntimeException and throws it, but might do
   * something more specific might be used in the future.
   */
  static public void showException(String msg) {  // ignore
    throw new RuntimeException(msg);
  }


  /**
   * Same as below, but defaults to a 12 point font, just as MacWrite intended.
   */
  protected void defaultFontOrDeath(String method) {
    defaultFontOrDeath(method, 12);
  }


  /**
   * First try to create a default font, but if that's not possible, throw
   * an exception that halts the program because textFont() has not been used
   * prior to the specified method.
   */
  protected void defaultFontOrDeath(String method, float size) {
    if (parent != null) {
      textFont = createDefaultFont(size);
    } else {
      throw new RuntimeException("Use textFont() before " + method + "()");
    }
  }



  //////////////////////////////////////////////////////////////

  // RENDERER SUPPORT QUERIES


  /**
   * Return true if this renderer should be drawn to the screen. Defaults to
   * returning true, since nearly all renderers are on-screen beasts. But can
   * be overridden for subclasses like PDF so that a window doesn't open up.
   * <br/> <br/>
   * A better name? showFrame, displayable, isVisible, visible, shouldDisplay,
   * what to call this?
   */
  public boolean displayable() {  // ignore
    return true;
  }


  /**
   * Return true if this renderer supports 2D drawing. Defaults to true.
   */
  public boolean is2D() {  // ignore
    return true;
  }


  /**
   * Return true if this renderer supports 3D drawing. Defaults to false.
   */
  public boolean is3D() {  // ignore
    return false;
  }


  /**
   * Return true if this renderer does rendering through OpenGL. Defaults to false.
   */
  public boolean isGL() {  // ignore
    return false;
  }


  public boolean is2X() {
    return pixelDensity == 2;
  }


  //////////////////////////////////////////////////////////////

  // ASYNC IMAGE SAVING


  @Override
  public boolean save(String filename) { // ignore

    if (hints[DISABLE_ASYNC_SAVEFRAME]) {
      return super.save(filename);
    }

    if (asyncImageSaver == null) {
      asyncImageSaver = new AsyncImageSaver();
    }

    if (!loaded) loadPixels();
    PImage target = asyncImageSaver.getAvailableTarget(pixelWidth, pixelHeight,
                                                       format);
    if (target == null) return false;
    int count = PApplet.min(pixels.length, target.pixels.length);
    System.arraycopy(pixels, 0, target.pixels, 0, count);
    asyncImageSaver.saveTargetAsync(this, target, parent.sketchFile(filename));

    return true;
  }

  protected void processImageBeforeAsyncSave(PImage image) { }


  /**
   * If there is running async save task for this file, blocks until it completes.
   * Has to be called on main thread because OpenGL overrides this and calls GL.
   * @param filename
   */
  protected void awaitAsyncSaveCompletion(String filename) {
    if (asyncImageSaver != null) {
      asyncImageSaver.awaitAsyncSaveCompletion(parent.sketchFile(filename));
    }
  }


  protected static AsyncImageSaver asyncImageSaver;

  protected static class AsyncImageSaver {

    static final int TARGET_COUNT =
        Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

    BlockingQueue<PImage> targetPool = new ArrayBlockingQueue<>(TARGET_COUNT);
    ExecutorService saveExecutor = Executors.newFixedThreadPool(TARGET_COUNT);

    int targetsCreated = 0;

    Map<File, Future<?>> runningTasks = new HashMap<>();
    final Object runningTasksLock = new Object();


    static final int TIME_AVG_FACTOR = 32;

    volatile long avgNanos = 0;
    long lastTime = 0;
    int lastFrameCount = 0;


    public AsyncImageSaver() { } // ignore


    public void dispose() { // ignore
      saveExecutor.shutdown();
      try {
        saveExecutor.awaitTermination(5000, TimeUnit.SECONDS);
      } catch (InterruptedException e) { }
    }


    public boolean hasAvailableTarget() { // ignore
      return targetsCreated < TARGET_COUNT || targetPool.isEmpty();
    }


    /**
     * After taking a target, you must call saveTargetAsync() or
     * returnUnusedTarget(), otherwise one thread won't be able to run
     */
    public PImage getAvailableTarget(int requestedWidth, int requestedHeight, // ignore
                                     int format) {
      try {
        PImage target;
        if (targetsCreated < TARGET_COUNT && targetPool.isEmpty()) {
          target = new PImage(requestedWidth, requestedHeight);
          targetsCreated++;
        } else {
          target = targetPool.take();
          if (target.pixelWidth != requestedWidth ||
              target.pixelHeight != requestedHeight) {
            // TODO: this kills performance when saving different sizes
            target = new PImage(requestedWidth, requestedHeight);
          }
        }
        target.format = format;
        return target;
      } catch (InterruptedException e) {
        return null;
      }
    }


    public void returnUnusedTarget(PImage target) { // ignore
      targetPool.offer(target);
    }


    public void saveTargetAsync(final PGraphics renderer, final PImage target, // ignore
                                final File file) {
      target.parent = renderer.parent;

      // if running every frame, smooth the framerate
      if (target.parent.frameCount - 1 == lastFrameCount && TARGET_COUNT > 1) {

        // count with one less thread to reduce jitter
        // 2 cores - 1 save thread - no wait
        // 4 cores - 3 save threads - wait 1/2 of save time
        // 8 cores - 7 save threads - wait 1/6 of save time
        long avgTimePerFrame = avgNanos / (Math.max(1, TARGET_COUNT - 1));
        long now = System.nanoTime();
        long delay = PApplet.round((lastTime + avgTimePerFrame - now) / 1e6f);
        try {
          if (delay > 0) Thread.sleep(delay);
        } catch (InterruptedException e) { }
      }

      lastFrameCount = target.parent.frameCount;
      lastTime = System.nanoTime();

      awaitAsyncSaveCompletion(file);

      // Explicit lock, because submitting a task and putting it into map
      // has to be atomic (and happen before task tries to remove itself)
      synchronized (runningTasksLock) {
        try {
          Future<?> task = saveExecutor.submit(() -> {
            try {
              long startTime = System.nanoTime();
              renderer.processImageBeforeAsyncSave(target);
              target.save(file.getAbsolutePath());
              long saveNanos = System.nanoTime() - startTime;
              synchronized (AsyncImageSaver.this) {
                if (avgNanos == 0) {
                  avgNanos = saveNanos;
                } else if (saveNanos < avgNanos) {
                  avgNanos = (avgNanos * (TIME_AVG_FACTOR - 1) + saveNanos) /
                      (TIME_AVG_FACTOR);
                } else {
                  avgNanos = saveNanos;
                }
              }
            } finally {
              targetPool.offer(target);
              synchronized (runningTasksLock) {
                runningTasks.remove(file);
              }
            }
          });
          runningTasks.put(file, task);
        } catch (RejectedExecutionException e) {
          // the executor service was probably shut down, no more saving for us
        }
      }
    }


    public void awaitAsyncSaveCompletion(final File file) { // ignore
      Future<?> taskWithSameFilename;
      synchronized (runningTasksLock) {
        taskWithSameFilename = runningTasks.get(file);
      }

      if (taskWithSameFilename != null) {
        try {
          taskWithSameFilename.get();
        } catch (InterruptedException | ExecutionException e) { }
      }
    }

  }

}
