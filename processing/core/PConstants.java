/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PConstants - numbers shared throughout processing.core
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry and
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


public interface PConstants {

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


  // used by split, all the standard whitespace chars
  // (uncludes unicode nbsp, that little bostage) 

  static final String WHITESPACE = " \t\n\r\f\u00A0";


  // for colors and/or images

  static final int RGB   = 1;  // image & color
  static final int RGBA  = 2;  // image
  static final int HSB   = 3;  // color
  static final int ALPHA = 4;  // image


  // image file types

  static final int TIFF  = 0;
  static final int TARGA = 1;
  static final int JPEG  = 2;
  static final int GIF   = 3;


  // filter/convert types

  static final int BLACK_WHITE   = 0;
  static final int GRAYSCALE     = 1;
  static final int BLUR          = 2;
  static final int GAUSSIAN_BLUR = 3;
  static final int POSTERIZE     = 4;
  static final int FIND_EDGES    = 5;


  // blend mode keyword definitions

  public final static int REPLACE  = 0;
  public final static int BLEND    = 1;
  public final static int ADD      = 2;
  public final static int SUBTRACT = 4;
  public final static int LIGHTEST = 8;
  public final static int DARKEST  = 16;


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

  static final int QUADS           = (1 << 7) | 0;
  static final int QUAD_STRIP      = (1 << 7) | 1;

  static final int POLYGON         = (1 << 8) | 0;
  static final int CONCAVE_POLYGON = (1 << 8) | 1;
  static final int CONVEX_POLYGON  = (1 << 8) | 2;


  // shape modes

  static final int CORNER        = 0;
  static final int CORNERS       = 1;
  static final int CENTER_RADIUS = 2;
  static final int CENTER        = 3;  // former CENTER_DIAMETER


  // uv texture orientation modes

  static final int NORMAL_SPACE  = 0;  // 0..1
  static final int IMAGE_SPACE   = 1;


  // text placement modes

  static final int SCREEN_SPACE  = 2;
  static final int OBJECT_SPACE  = 3;


  // text alignment modes

  static final int ALIGN_LEFT    = 0;
  static final int ALIGN_CENTER  = 1;
  static final int ALIGN_RIGHT   = 2;


  // stroke modes

  static final int SQUARE_ENDCAP    = 1 << 0;
  static final int ROUND_ENDCAP     = 1 << 1;
  static final int PROJECTED_ENDCAP = 1 << 2;
  static final int STROKE_CAP_MASK  = 
    SQUARE_ENDCAP | ROUND_ENDCAP | PROJECTED_ENDCAP;

  static final int MITERED_JOIN     = 1 << 3;
  static final int ROUND_JOIN       = 1 << 4;
  static final int BEVELED_JOIN     = 1 << 5;
  static final int STROKE_JOIN_MASK = 
    MITERED_JOIN | ROUND_JOIN | BEVELED_JOIN;


  // lighting

  static final int DISABLED = 0;
  static final int AMBIENT  = 1;
  static final int DIFFUSE  = 2;
  static final int SPECULAR = 3;


  // net 

  static final int CLIENT = 0;
  static final int SERVER = 1;


  // key constants

  // only including the most-used of these guys
  // if people need more esoteric keys, they can learn about
  // the esoteric java KeyEvent api and of virtual keys

  static final int UP      = KeyEvent.VK_UP;
  static final int DOWN    = KeyEvent.VK_DOWN;
  static final int LEFT    = KeyEvent.VK_LEFT;
  static final int RIGHT   = KeyEvent.VK_RIGHT;

  static final int ALT     = KeyEvent.VK_ALT;
  static final int CONTROL = KeyEvent.VK_CONTROL;
  static final int SHIFT   = KeyEvent.VK_SHIFT;


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
  static final int NEW_GRAPHICS            = 2;
  static final int DISABLE_TEXT_SMOOTH     = 3;
  static final int DISABLE_SMOOTH_HACK     = 4;

  static final int HINT_COUNT              = 5;


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

  static final int SR = 12; // stroke
  static final int SG = 13;
  static final int SB = 14;
  static final int SA = 15;

  // not used in rendering
  // only used for calculating colors

  static final int NX = 16; // normal
  static final int NY = 17;
  static final int NZ = 18;

  static final int VX = 19; // view space coords
  static final int VY = 20;
  static final int VZ = 21;
  static final int VW = 22;

  static final int WT = 23; // stroke width

  //static final int SPY = 22;  // for subpixel rendering

  static final int VERTEX_FIELD_COUNT = 24;

  // line  fields

  static final int PA = 0; // point A
  static final int PB = 1; // point B
  static final int LI = 2; // shape index
  static final int SM = 3; // stroke mode

  static final int LINE_FIELD_COUNT = 4;

  // triangle  fields

  static final int VA = 0;    // point A
  static final int VB = 1;    // point B
  static final int VC = 2;    // point B
  static final int TI = 3;    // shape index
  static final int TEX = 4;   // texture index

  static final int TRIANGLE_FIELD_COUNT = 5;
}
