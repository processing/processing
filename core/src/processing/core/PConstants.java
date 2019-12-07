/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-11 Ben Fry and Casey Reas
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

import java.awt.Cursor;
import java.awt.event.KeyEvent;


/**
 * Numbers shared throughout processing.core.
 * <P>
 * An attempt is made to keep the constants as short/non-verbose
 * as possible. For instance, the constant is TIFF instead of
 * FILE_TYPE_TIFF. We'll do this as long as we can get away with it.
 *
 * @usage Web &amp; Application
 */
public interface PConstants {

  int X = 0;
  int Y = 1;
  int Z = 2;


  // renderers known to processing.core

  /*
  // List of renderers used inside PdePreprocessor
  static final StringList rendererList = new StringList(new String[] {
    "JAVA2D", "JAVA2D_2X",
    "P2D", "P2D_2X", "P3D", "P3D_2X", "OPENGL",
    "E2D", "FX2D", "FX2D_2X",  // experimental
    "LWJGL.P2D", "LWJGL.P3D",  // hmm
    "PDF"  // no DXF because that's only for beginRaw()
  });
  */

  String JAVA2D = "processing.awt.PGraphicsJava2D";

  String P2D = "processing.opengl.PGraphics2D";
  String P3D = "processing.opengl.PGraphics3D";

  // When will it be time to remove this?
  @Deprecated
  String OPENGL = P3D;

  // Experimental, higher-performance Java 2D renderer (but no pixel ops)
//  static final String E2D = PGraphicsDanger2D.class.getName();

  // Experimental JavaFX renderer; even better 2D performance
  String FX2D = "processing.javafx.PGraphicsFX2D";

  String PDF = "processing.pdf.PGraphicsPDF";
  String SVG = "processing.svg.PGraphicsSVG";
  String DXF = "processing.dxf.RawDXF";

  // platform IDs for PApplet.platform

  int OTHER   = 0;
  int WINDOWS = 1;
  int MACOSX  = 2;
  int LINUX   = 3;

  String[] platformNames = {
    "other", "windows", "macosx", "linux"
  };


  float EPSILON = 0.0001f;


  // max/min values for numbers

  /**
   * Same as Float.MAX_VALUE, but included for parity with MIN_VALUE,
   * and to avoid teaching static methods on the first day.
   */
  float MAX_FLOAT = Float.MAX_VALUE;
  /**
   * Note that Float.MIN_VALUE is the smallest <EM>positive</EM> value
   * for a floating point number, not actually the minimum (negative) value
   * for a float. This constant equals 0xFF7FFFFF, the smallest (farthest
   * negative) value a float can have before it hits NaN.
   */
  float MIN_FLOAT = -Float.MAX_VALUE;
  /** Largest possible (positive) integer value */
  int MAX_INT = Integer.MAX_VALUE;
  /** Smallest possible (negative) integer value */
  int MIN_INT = Integer.MIN_VALUE;

  // shapes

  int VERTEX = 0;
  int BEZIER_VERTEX = 1;
  int QUADRATIC_VERTEX = 2;
  int CURVE_VERTEX = 3;
  int BREAK = 4;

  @Deprecated
  int QUAD_BEZIER_VERTEX = 2;  // should not have been exposed

  // useful goodness

  /**
   * ( begin auto-generated from PI.xml )
   *
   * PI is a mathematical constant with the value 3.14159265358979323846. It
   * is the ratio of the circumference of a circle to its diameter. It is
   * useful in combination with the trigonometric functions <b>sin()</b> and
   * <b>cos()</b>.
   *
   * ( end auto-generated )
   * @webref constants
   * @see PConstants#TWO_PI
   * @see PConstants#TAU
   * @see PConstants#HALF_PI
   * @see PConstants#QUARTER_PI
   *
   */
  float PI = (float) Math.PI;
  /**
   * ( begin auto-generated from HALF_PI.xml )
   *
   * HALF_PI is a mathematical constant with the value
   * 1.57079632679489661923. It is half the ratio of the circumference of a
   * circle to its diameter. It is useful in combination with the
   * trigonometric functions <b>sin()</b> and <b>cos()</b>.
   *
   * ( end auto-generated )
   * @webref constants
   * @see PConstants#PI
   * @see PConstants#TWO_PI
   * @see PConstants#TAU
   * @see PConstants#QUARTER_PI
   */
  float HALF_PI = (float) (Math.PI / 2.0);
  float THIRD_PI = (float) (Math.PI / 3.0);
  /**
   * ( begin auto-generated from QUARTER_PI.xml )
   *
   * QUARTER_PI is a mathematical constant with the value 0.7853982. It is
   * one quarter the ratio of the circumference of a circle to its diameter.
   * It is useful in combination with the trigonometric functions
   * <b>sin()</b> and <b>cos()</b>.
   *
   * ( end auto-generated )
   * @webref constants
   * @see PConstants#PI
   * @see PConstants#TWO_PI
   * @see PConstants#TAU
   * @see PConstants#HALF_PI
   */
  float QUARTER_PI = (float) (Math.PI / 4.0);
  /**
   * ( begin auto-generated from TWO_PI.xml )
   *
   * TWO_PI is a mathematical constant with the value 6.28318530717958647693.
   * It is twice the ratio of the circumference of a circle to its diameter.
   * It is useful in combination with the trigonometric functions
   * <b>sin()</b> and <b>cos()</b>.
   *
   * ( end auto-generated )
   * @webref constants
   * @see PConstants#PI
   * @see PConstants#TAU
   * @see PConstants#HALF_PI
   * @see PConstants#QUARTER_PI
   */
  float TWO_PI = (float) (2.0 * Math.PI);
  /**
   * ( begin auto-generated from TAU.xml )
   *
   * TAU is an alias for TWO_PI, a mathematical constant with the value
   * 6.28318530717958647693. It is twice the ratio of the circumference
   * of a circle to its diameter. It is useful in combination with the
   * trigonometric functions <b>sin()</b> and <b>cos()</b>.
   *
   * ( end auto-generated )
   * @webref constants
   * @see PConstants#PI
   * @see PConstants#TWO_PI
   * @see PConstants#HALF_PI
   * @see PConstants#QUARTER_PI
   */
  float TAU = (float) (2.0 * Math.PI);

  float DEG_TO_RAD = PI/180.0f;
  float RAD_TO_DEG = 180.0f/PI;


  // angle modes

  //static final int RADIANS = 0;
  //static final int DEGREES = 1;


  // used by split, all the standard whitespace chars
  // (also includes unicode nbsp, that little bostage)

  String WHITESPACE = " \t\n\r\f\u00A0";


  // for colors and/or images

  int RGB   = 1;  // image & color
  int ARGB  = 2;  // image
  int HSB   = 3;  // color
  int ALPHA = 4;  // image
//  static final int CMYK  = 5;  // image & color (someday)


  // image file types

  int TIFF  = 0;
  int TARGA = 1;
  int JPEG  = 2;
  int GIF   = 3;


  // filter/convert types

  int BLUR      = 11;
  int GRAY      = 12;
  int INVERT    = 13;
  int OPAQUE    = 14;
  int POSTERIZE = 15;
  int THRESHOLD = 16;
  int ERODE     = 17;
  int DILATE    = 18;


  // blend mode keyword definitions
  // @see processing.core.PImage#blendColor(int,int,int)

  int REPLACE    = 0;
  int BLEND      = 1 << 0;
  int ADD        = 1 << 1;
  int SUBTRACT   = 1 << 2;
  int LIGHTEST   = 1 << 3;
  int DARKEST    = 1 << 4;
  int DIFFERENCE = 1 << 5;
  int EXCLUSION  = 1 << 6;
  int MULTIPLY   = 1 << 7;
  int SCREEN     = 1 << 8;
  int OVERLAY    = 1 << 9;
  int HARD_LIGHT = 1 << 10;
  int SOFT_LIGHT = 1 << 11;
  int DODGE      = 1 << 12;
  int BURN       = 1 << 13;

  // for messages

  int CHATTER   = 0;
  int COMPLAINT = 1;
  int PROBLEM   = 2;


  // types of transformation matrices

  int PROJECTION = 0;
  int MODELVIEW  = 1;

  // types of projection matrices

  int CUSTOM       = 0; // user-specified fanciness
  int ORTHOGRAPHIC = 2; // 2D isometric projection
  int PERSPECTIVE  = 3; // perspective matrix


  // shapes

  // the low four bits set the variety,
  // higher bits set the specific shape type

  int GROUP           = 0;   // createShape()

  int POINT           = 2;   // primitive
  int POINTS          = 3;   // vertices

  int LINE            = 4;   // primitive
  int LINES           = 5;   // beginShape(), createShape()
  int LINE_STRIP      = 50;  // beginShape()
  int LINE_LOOP       = 51;

  int TRIANGLE        = 8;   // primitive
  int TRIANGLES       = 9;   // vertices
  int TRIANGLE_STRIP  = 10;  // vertices
  int TRIANGLE_FAN    = 11;  // vertices

  int QUAD            = 16;  // primitive
  int QUADS           = 17;  // vertices
  int QUAD_STRIP      = 18;  // vertices

  int POLYGON         = 20;  // in the end, probably cannot
  int PATH            = 21;  // separate these two

  int RECT            = 30;  // primitive
  int ELLIPSE         = 31;  // primitive
  int ARC             = 32;  // primitive

  int SPHERE          = 40;  // primitive
  int BOX             = 41;  // primitive

//  static public final int POINT_SPRITES = 52;
//  static public final int NON_STROKED_SHAPE = 60;
//  static public final int STROKED_SHAPE     = 61;


  // shape closing modes

  int OPEN = 1;
  int CLOSE = 2;


  // shape drawing modes

  /** Draw mode convention to use (x, y) to (width, height) */
  int CORNER   = 0;
  /** Draw mode convention to use (x1, y1) to (x2, y2) coordinates */
  int CORNERS  = 1;
  /** Draw mode from the center, and using the radius */
  int RADIUS   = 2;
  /**
   * Draw from the center, using second pair of values as the diameter.
   * Formerly called CENTER_DIAMETER in alpha releases.
   */
  int CENTER   = 3;
  /**
   * Synonym for the CENTER constant. Draw from the center,
   * using second pair of values as the diameter.
   */
  int DIAMETER = 3;


  // arc drawing modes

  //static final int OPEN = 1;  // shared
  int CHORD  = 2;
  int PIE    = 3;


  // vertically alignment modes for text

  /** Default vertical alignment for text placement */
  int BASELINE = 0;
  /** Align text to the top */
  int TOP = 101;
  /** Align text from the bottom, using the baseline. */
  int BOTTOM = 102;


  // uv texture orientation modes

  /** texture coordinates in 0..1 range */
  int NORMAL     = 1;
  /** texture coordinates based on image width/height */
  int IMAGE      = 2;


  // texture wrapping modes

  /** textures are clamped to their edges */
  int CLAMP = 0;
  /** textures wrap around when uv values go outside 0..1 range */
  int REPEAT = 1;


  // text placement modes

  /**
   * textMode(MODEL) is the default, meaning that characters
   * will be affected by transformations like any other shapes.
   * <p/>
   * Changed value in 0093 to not interfere with LEFT, CENTER, and RIGHT.
   */
  int MODEL = 4;

  /**
   * textMode(SHAPE) draws text using the the glyph outlines of
   * individual characters rather than as textures. If the outlines are
   * not available, then textMode(SHAPE) will be ignored and textMode(MODEL)
   * will be used instead. For this reason, be sure to call textMode()
   * <EM>after</EM> calling textFont().
   * <p/>
   * Currently, textMode(SHAPE) is only supported by OPENGL mode.
   * It also requires Java 1.2 or higher (OPENGL requires 1.4 anyway)
   */
  int SHAPE = 5;


  // text alignment modes
  // are inherited from LEFT, CENTER, RIGHT

  // stroke modes

  int SQUARE   = 1 << 0;  // called 'butt' in the svg spec
  int ROUND    = 1 << 1;
  int PROJECT  = 1 << 2;  // called 'square' in the svg spec
  int MITER    = 1 << 3;
  int BEVEL    = 1 << 5;


  // lighting

  int AMBIENT = 0;
  int DIRECTIONAL  = 1;
  //static final int POINT  = 2;  // shared with shape feature
  int SPOT = 3;


  // key constants

  // only including the most-used of these guys
  // if people need more esoteric keys, they can learn about
  // the esoteric java KeyEvent api and of virtual keys

  // both key and keyCode will equal these values
  // for 0125, these were changed to 'char' values, because they
  // can be upgraded to ints automatically by Java, but having them
  // as ints prevented split(blah, TAB) from working
  char BACKSPACE = 8;
  char TAB       = 9;
  char ENTER     = 10;
  char RETURN    = 13;
  char ESC       = 27;
  char DELETE    = 127;

  // i.e. if ((key == CODED) && (keyCode == UP))
  int CODED     = 0xffff;

  // key will be CODED and keyCode will be this value
  int UP        = KeyEvent.VK_UP;
  int DOWN      = KeyEvent.VK_DOWN;
  int LEFT      = KeyEvent.VK_LEFT;
  int RIGHT     = KeyEvent.VK_RIGHT;

  // key will be CODED and keyCode will be this value
  int ALT       = KeyEvent.VK_ALT;
  int CONTROL   = KeyEvent.VK_CONTROL;
  int SHIFT     = KeyEvent.VK_SHIFT;


  // orientations (only used on Android, ignored on desktop)

  /** Screen orientation constant for portrait (the hamburger way). */
  int PORTRAIT = 1;
  /** Screen orientation constant for landscape (the hot dog way). */
  int LANDSCAPE = 2;

  /** Use with fullScreen() to indicate all available displays. */
  int SPAN = 0;

  // cursor types

  int ARROW = Cursor.DEFAULT_CURSOR;
  int CROSS = Cursor.CROSSHAIR_CURSOR;
  int HAND  = Cursor.HAND_CURSOR;
  int MOVE  = Cursor.MOVE_CURSOR;
  int TEXT  = Cursor.TEXT_CURSOR;
  int WAIT  = Cursor.WAIT_CURSOR;


  // hints - hint values are positive for the alternate version,
  // negative of the same value returns to the normal/default state

  @Deprecated
  int ENABLE_NATIVE_FONTS        =  1;
  @Deprecated
  int DISABLE_NATIVE_FONTS       = -1;

  int DISABLE_DEPTH_TEST         =  2;
  int ENABLE_DEPTH_TEST          = -2;

  int ENABLE_DEPTH_SORT          =  3;
  int DISABLE_DEPTH_SORT         = -3;

  int DISABLE_OPENGL_ERRORS      =  4;
  int ENABLE_OPENGL_ERRORS       = -4;

  int DISABLE_DEPTH_MASK         =  5;
  int ENABLE_DEPTH_MASK          = -5;

  int DISABLE_OPTIMIZED_STROKE   =  6;
  int ENABLE_OPTIMIZED_STROKE    = -6;

  int ENABLE_STROKE_PERSPECTIVE  =  7;
  int DISABLE_STROKE_PERSPECTIVE = -7;

  int DISABLE_TEXTURE_MIPMAPS    =  8;
  int ENABLE_TEXTURE_MIPMAPS     = -8;

  int ENABLE_STROKE_PURE         =  9;
  int DISABLE_STROKE_PURE        = -9;

  int ENABLE_BUFFER_READING      =  10;
  int DISABLE_BUFFER_READING     = -10;

  int DISABLE_KEY_REPEAT         =  11;
  int ENABLE_KEY_REPEAT          = -11;

  int DISABLE_ASYNC_SAVEFRAME    =  12;
  int ENABLE_ASYNC_SAVEFRAME     = -12;

  int HINT_COUNT                 =  13;
}
