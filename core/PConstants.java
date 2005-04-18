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

import java.awt.Cursor;
import java.awt.event.KeyEvent;


/**
 * Numbers shared throughout processing.core.
 * <P>
 * An attempt is made to keep the constants as short/non-verbose
 * as possible. For instance, the constant is TIFF instead of
 * FILE_TYPE_TIFF. We'll do this as long as we can get away with it.
 */
public interface PConstants {

  // renderers known to processing.core

  static final String P2D    = "processing.core.PGraphics";
  static final String P3D    = "processing.core.PGraphics3";
  static final String JAVA2D = "processing.core.PGraphics2";
  static final String OPENGL = "processing.opengl.PGraphicsGL";


  // platform IDs for PApplet.platform

  static final int WINDOWS = 1;
  static final int MACOS9  = 2;
  static final int MACOSX  = 3;
  static final int LINUX   = 4;
  static final int OTHER   = 0;


  // for better parity between c++ version (at no speed cost)

  static final float EPSILON   = 0.0001f;
  static final float TWO       = 2.0f;
  static final float ONE       = 1.0f;
  static final float HALF      = 0.5f;
  static final float TFF       = 255.0f;
  static final float MAX_FLOAT = Float.MAX_VALUE;


  // useful goodness

  static final float PI = (float) Math.PI;
  static final float HALF_PI    = PI / 2.0f;
  static final float THIRD_PI   = PI / 3.0f;
  static final float QUARTER_PI = PI / 4.0f;
  static final float TWO_PI     = PI * 2.0f;

  static final float DEG_TO_RAD = PI/180.0f;
  static final float RAD_TO_DEG = 180.0f/PI;


  // angle modes

  static final int RADIANS = 0;
  static final int DEGREES = 1;


  // used by split, all the standard whitespace chars
  // (uncludes unicode nbsp, that little bostage)

  static final String WHITESPACE = " \t\n\r\f\u00A0";


  // for colors and/or images

  static final int RGB   = 1;  // image & color
  static final int ARGB  = 2;  // image
  static final int HSB   = 3;  // color
  static final int ALPHA = 4;  // image


  // image file types

  static final int TIFF  = 0;
  static final int TARGA = 1;
  static final int JPEG  = 2;
  static final int GIF   = 3;


  // filter/convert types

  static final int BLUR      = 11;
  static final int GRAY      = 12;
  static final int INVERT    = 13;
  static final int OPAQUE    = 14;
  static final int POSTERIZE = 15;
  static final int THRESHOLD = 16;


  // blend mode keyword definitions

  public final static int REPLACE    = 0;
  public final static int BLEND      = 1 << 0;
  public final static int ADD        = 1 << 1;
  public final static int SUBTRACT   = 1 << 2;
  public final static int LIGHTEST   = 1 << 3;
  public final static int DARKEST    = 1 << 4;

  // incomplete, slated for beta
  public final static int DIFFERENCE = 1 << 5;
  public final static int MULTIPLY   = 1 << 6;
  public final static int SCREEN     = 1 << 7;
  public final static int OVERLAY    = 1 << 8;
  public final static int HARD_LIGHT = 1 << 9;
  public final static int SOFT_LIGHT = 1 << 10;



  // colour component bitmasks

  public static final int ALPHA_MASK = 0xff000000;
  public static final int RED_MASK   = 0x00ff0000;
  public static final int GREEN_MASK = 0x0000ff00;
  public static final int BLUE_MASK  = 0x000000ff;


  // for messages

  static final int CHATTER   = 0;
  static final int COMPLAINT = 1;
  static final int PROBLEM   = 2;


  // types of projection matrices

  static final int CUSTOM       = 0; // user-specified fanciness
  static final int ORTHOGRAPHIC = 2; // 2D isometric projection
  static final int PERSPECTIVE  = 3; // perspective matrix


  // rendering settings

  static final float PIXEL_CENTER = 0.5f;  // for polygon aa


  // shapes

  // the low four bits set the variety,
  // higher bits set the specific shape type

  static final int POINTS          = (1 << 4) | 0;

  static final int LINES           = (1 << 5) | 0;
  static final int LINE_STRIP      = (1 << 5) | 1;
  static final int LINE_LOOP       = (1 << 5) | 2;

  static final int TRIANGLES       = (1 << 6) | 0;
  static final int TRIANGLE_STRIP  = (1 << 6) | 1;
  static final int TRIANGLE_FAN    = (1 << 6) | 2;

  static final int QUADS           = (1 << 7) | 0;
  static final int QUAD_STRIP      = (1 << 7) | 1;

  static final int POLYGON         = (1 << 8) | 0;
  //static final int CONCAVE_POLYGON = (1 << 8) | 1;
  //static final int CONVEX_POLYGON  = (1 << 8) | 2;


  // shape modes

  static final int CORNER        = 0;
  static final int CORNERS       = 1;
  static final int CENTER_RADIUS = 2;
  static final int CENTER        = 3;  // former CENTER_DIAMETER


  // uv texture orientation modes

  static final int NORMALIZED = 1; //_SPACE  = 0;  // 0..1
  static final int IMAGE      = 2;


  // text placement modes

  //static final int SCREEN  = 4;  // var SCREEN exists elsewhere
  static final int MODEL  = 3;


  // text alignment modes
  // are inherited from LEFT, CENTER, RIGHT


  // stroke modes

  static final int SQUARE   = 1 << 0;
  static final int ROUND    = 1 << 1;
  static final int PROJECT  = 1 << 2;
  //static final int CAP_MASK = SQUARE | ROUND | PROJECT;
  static final int MITER    = 1 << 3;
  //static final int ROUND       = 1 << 4;
  static final int BEVEL    = 1 << 5;
  //static final int JOIN_MASK = MITERED | ROUND | BEVELED;


  // lighting

  static final int AMBIENT = 0;
  static final int DIRECTIONAL  = 1;
  static final int POINT  = 2;
  static final int SPOT = 3;


  // net

  //static final int CLIENT = 0;
  //static final int SERVER = 1;


  // key constants

  // only including the most-used of these guys
  // if people need more esoteric keys, they can learn about
  // the esoteric java KeyEvent api and of virtual keys

  // both key and keyCode will equal these values
  static final int BACKSPACE = 8;
  static final int TAB       = 9;
  static final int ENTER     = 10;
  static final int RETURN    = 13;
  static final int ESC       = 27;
  static final int DELETE    = 127;

  // i.e. if ((key == CODED) && (keyCode == UP))
  static final int CODED     = 0xffff;

  // key will be CODED and keyCode will be this value
  static final int UP        = KeyEvent.VK_UP;
  static final int DOWN      = KeyEvent.VK_DOWN;
  static final int LEFT      = KeyEvent.VK_LEFT;
  static final int RIGHT     = KeyEvent.VK_RIGHT;

  // key will be CODED and keyCode will be this value
  static final int ALT       = KeyEvent.VK_ALT;
  static final int CONTROL   = KeyEvent.VK_CONTROL;
  static final int SHIFT     = KeyEvent.VK_SHIFT;


  // cursor types

  static final int ARROW = Cursor.DEFAULT_CURSOR;
  static final int CROSS = Cursor.CROSSHAIR_CURSOR;
  static final int HAND  = Cursor.HAND_CURSOR;
  static final int MOVE  = Cursor.MOVE_CURSOR;
  static final int TEXT  = Cursor.TEXT_CURSOR;
  static final int WAIT  = Cursor.WAIT_CURSOR;


  // hints

  static final int SCALE_STROKE_WIDTH      = 0;
  static final int LIGHTING_AFFECTS_STROKE = 1;
  //static final int NEW_GRAPHICS            = 2;
  static final int DISABLE_TEXT_SMOOTH     = 3;
  static final int DISABLE_SMOOTH_HACK     = 4;
  static final int NO_DEPTH_TEST           = 5;
  static final int NO_FLYING_POO           = 6;
  static final int DEPTH_SORT              = 7;

  static final int HINT_COUNT              = 8;


  //////////////////////////////////////////////////////////////

  // FIELDS


  // transformed values
  // (to be used in rendering)

  static final int X = 0; // transformed xyzw
  static final int Y = 1; // formerly SX SY SZ
  static final int Z = 2;

  static final int R = 3;  // actual rgb, after lighting
  static final int G = 4;  // fill stored here, transform in place
  static final int B = 5;
  static final int A = 6;

  // values that need no transformation
  // but will be used in rendering

  static final int U = 7; // texture
  static final int V = 8;

  // incoming values, raw and untransformed
  // (won't be used in rendering)

  static final int MX = 9; // model coords xyz
  static final int MY = 10;
  static final int MZ = 11;

  static final int SR = 12; // stroke colors
  static final int SG = 13;
  static final int SB = 14;
  static final int SA = 15;

  static final int SW = 16; // stroke weight

  // not used in rendering
  // only used for calculating colors

  static final int NX = 17; // normal
  static final int NY = 18;
  static final int NZ = 19;

  static final int VX = 20; // view space coords
  static final int VY = 21;
  static final int VZ = 22;
  static final int VW = 23;

  // Ambient color (usually to be kept the same as diffuse)
  // fill(_) sets both ambient and diffuse.
  static final int AR = 24;
  static final int AG = 25;
  static final int AB = 26;

  // Diffuse is shared with fill.
  static final int DR = 3;
  static final int DG = 4;
  static final int DB = 5;
  static final int DA = 6;

  //specular (by default kept white)
  static final int SPR = 27;
  static final int SPG = 28;
  static final int SPB = 29;
  //GL doesn't use a separate specular alpha, but we do (we're better)
  static final int SPA = 30;

  static final int SHINE = 31;

  //emissive (by default kept black)
  static final int ER = 32;
  static final int EG = 33;
  static final int EB = 34;

  //has this vertex been lit yet
  static final int BEEN_LIT = 35;

  static final int VERTEX_FIELD_COUNT = 36;

  // line & triangle fields (note how these overlap)

  static final int INDEX = 0;          // shape index
  static final int VERTEX1 = 1;
  static final int VERTEX2 = 2;
  static final int VERTEX3 = 3;        // (triangles only)
  static final int TEXTURE_INDEX = 4;  // (triangles only)
  static final int STROKE_MODE = 3;    // (lines only)
  static final int STROKE_WEIGHT = 4;  // (lines only)

  static final int LINE_FIELD_COUNT = 5;
  static final int TRIANGLE_FIELD_COUNT = 5;

  static final int TRI_DIFFUSE_R = 0;
  static final int TRI_DIFFUSE_G = 1;
  static final int TRI_DIFFUSE_B = 2;
  static final int TRI_DIFFUSE_A = 3;
  static final int TRI_SPECULAR_R = 4;
  static final int TRI_SPECULAR_G = 5;
  static final int TRI_SPECULAR_B = 6;
  static final int TRI_SPECULAR_A = 7;

  static final int TRIANGLE_COLOR_COUNT = 8;


  // normal modes for lighting, these have the uglier naming
  // because the constants are never seen by users

  /// normal calculated per triangle
  static final int AUTO_NORMAL = 0;
  /// one normal manually specified per shape
  static final int MANUAL_SHAPE_NORMAL = 1;
  /// normals specified for each shape vertex
  static final int MANUAL_VERTEX_NORMAL = 2;
}
