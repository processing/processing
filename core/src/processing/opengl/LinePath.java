/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 * Copyright 2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package processing.opengl;

import processing.core.PMatrix2D;

/**
 * The {@code LinePath} class allows to represent polygonal paths,
 * potentially composed by several disjoint polygonal segments.
 * It can be iterated by the {@link PathIterator} class including all
 * of its segment types and winding rules
 *
 */
public class LinePath {
  /**
   * The winding rule constant for specifying an even-odd rule
   * for determining the interior of a path.
   * The even-odd rule specifies that a point lies inside the
   * path if a ray drawn in any direction from that point to
   * infinity is crossed by path segments an odd number of times.
   */
  public static final int WIND_EVEN_ODD       = 0;

  /**
   * The winding rule constant for specifying a non-zero rule
   * for determining the interior of a path.
   * The non-zero rule specifies that a point lies inside the
   * path if a ray drawn in any direction from that point to
   * infinity is crossed by path segments a different number
   * of times in the counter-clockwise direction than the
   * clockwise direction.
   */
  public static final int WIND_NON_ZERO       = 1;

  /**
   * Starts segment at a given position.
   */
  public static final byte SEG_MOVETO  = 0;

  /**
   * Extends segment by adding a line to a given position.
   */
  public static final byte SEG_LINETO = 1;

  /**
   * Closes segment at current position.
   */
  public static final byte SEG_CLOSE = 2;

  /**
   * Joins path segments by extending their outside edges until they meet.
   */
  public final static int JOIN_MITER = 0;

  /**
   * Joins path segments by rounding off the corner at a radius of half the line
   * width.
   */
  public final static int JOIN_ROUND = 1;

  /**
   * Joins path segments by connecting the outer corners of their wide outlines
   * with a straight segment.
   */
  public final static int JOIN_BEVEL = 2;

  /**
   * Ends unclosed subpaths and dash segments with no added decoration.
   */
  public final static int CAP_BUTT = 0;

  /**
   * Ends unclosed subpaths and dash segments with a round decoration that has a
   * radius equal to half of the width of the pen.
   */
  public final static int CAP_ROUND = 1;

  /**
   * Ends unclosed subpaths and dash segments with a square projection that
   * extends beyond the end of the segment to a distance equal to half of the
   * line width.
   */
  public final static int CAP_SQUARE = 2;

  private static PMatrix2D identity = new PMatrix2D();

  private static float defaultMiterlimit = 10.0f;

  static final int INIT_SIZE = 20;

  static final int EXPAND_MAX = 500;

  protected byte[] pointTypes;

  protected float[] floatCoords;

  protected int[] pointColors;

  protected int numTypes;

  protected int numCoords;

  protected int windingRule;


  /**
   * Constructs a new empty single precision {@code LinePath} object with a
   * default winding rule of {@link #WIND_NON_ZERO}.
   */
  public LinePath() {
    this(WIND_NON_ZERO, INIT_SIZE);
  }


  /**
   * Constructs a new empty single precision {@code LinePath} object with the
   * specified winding rule to control operations that require the interior of
   * the path to be defined.
   *
   * @param rule
   *          the winding rule
   * @see #WIND_EVEN_ODD
   * @see #WIND_NON_ZERO
   */
  public LinePath(int rule) {
    this(rule, INIT_SIZE);
  }


  /**
   * Constructs a new {@code LinePath} object from the given specified initial
   * values. This method is only intended for internal use and should not be
   * made public if the other constructors for this class are ever exposed.
   *
   * @param rule
   *          the winding rule
   * @param initialTypes
   *          the size to make the initial array to store the path segment types
   */
  public LinePath(int rule, int initialCapacity) {
    setWindingRule(rule);
    this.pointTypes = new byte[initialCapacity];
    floatCoords = new float[initialCapacity * 2];
    pointColors = new int[initialCapacity];
  }


  void needRoom(boolean needMove, int newPoints) {
    if (needMove && numTypes == 0) {
      throw new RuntimeException("missing initial moveto "
        + "in path definition");
    }
    int size = pointTypes.length;
    if (numTypes >= size) {
      int grow = size;
      if (grow > EXPAND_MAX) {
        grow = EXPAND_MAX;
      }
      pointTypes = copyOf(pointTypes, size + grow);
    }
    size = floatCoords.length;
    if (numCoords + newPoints * 2 > size) {
      int grow = size;
      if (grow > EXPAND_MAX * 2) {
        grow = EXPAND_MAX * 2;
      }
      if (grow < newPoints * 2) {
        grow = newPoints * 2;
      }
      floatCoords = copyOf(floatCoords, size + grow);
    }
    size = pointColors.length;
    if (numCoords + newPoints > size) {
      int grow = size;
      if (grow > EXPAND_MAX) {
        grow = EXPAND_MAX;
      }
      if (grow < newPoints) {
        grow = newPoints;
      }
      pointColors = copyOf(pointColors, size + grow);
    }
  }


  /**
   * Adds a point to the path by moving to the specified coordinates specified
   * in float precision.
   * <p>
   * This method provides a single precision variant of the double precision
   * {@code moveTo()} method on the base {@code LinePath} class.
   *
   * @param x
   *          the specified X coordinate
   * @param y
   *          the specified Y coordinate
   * @see LinePath#moveTo
   */
  public final void moveTo(float x, float y, int c) {
    if (numTypes > 0 && pointTypes[numTypes - 1] == SEG_MOVETO) {
      floatCoords[numCoords - 2] = x;
      floatCoords[numCoords - 1] = y;
      pointColors[numCoords/2] = c;
    } else {
      needRoom(false, 1);
      pointTypes[numTypes++] = SEG_MOVETO;
      floatCoords[numCoords++] = x;
      floatCoords[numCoords++] = y;
      pointColors[numCoords/2-1] = c;
    }
  }


  /**
   * Adds a point to the path by drawing a straight line from the current
   * coordinates to the new specified coordinates specified in float precision.
   * <p>
   * This method provides a single precision variant of the double precision
   * {@code lineTo()} method on the base {@code LinePath} class.
   *
   * @param x
   *          the specified X coordinate
   * @param y
   *          the specified Y coordinate
   * @see LinePath#lineTo
   */
  public final void lineTo(float x, float y, int c) {
    needRoom(true, 1);
    pointTypes[numTypes++] = SEG_LINETO;
    floatCoords[numCoords++] = x;
    floatCoords[numCoords++] = y;
    pointColors[numCoords/2-1] = c;
  }


  /**
   * The iterator for this class is not multi-threaded safe, which means that
   * the {@code LinePath} class does not guarantee that modifications to the
   * geometry of this {@code LinePath} object do not affect any iterations of that
   * geometry that are already in process.
   */
  public PathIterator getPathIterator() {
    return new PathIterator(this);
  }


  /**
   * Closes the current subpath by drawing a straight line back to the
   * coordinates of the last {@code moveTo}. If the path is already closed then
   * this method has no effect.
   */
  public final void closePath() {
    if (numTypes == 0 || pointTypes[numTypes - 1] != SEG_CLOSE) {
      needRoom(false, 0);
      pointTypes[numTypes++] = SEG_CLOSE;
    }
  }


  /**
   * Returns the fill style winding rule.
   *
   * @return an integer representing the current winding rule.
   * @see #WIND_EVEN_ODD
   * @see #WIND_NON_ZERO
   * @see #setWindingRule
   */
  public final int getWindingRule() {
    return windingRule;
  }


  /**
   * Sets the winding rule for this path to the specified value.
   *
   * @param rule
   *          an integer representing the specified winding rule
   * @exception IllegalArgumentException
   *              if {@code rule} is not either {@link #WIND_EVEN_ODD} or
   *              {@link #WIND_NON_ZERO}
   * @see #getWindingRule
   */
  public final void setWindingRule(int rule) {
    if (rule != WIND_EVEN_ODD && rule != WIND_NON_ZERO) {
      throw new IllegalArgumentException("winding rule must be "
        + "WIND_EVEN_ODD or " + "WIND_NON_ZERO");
    }
    windingRule = rule;
  }


  /**
   * Resets the path to empty. The append position is set back to the beginning
   * of the path and all coordinates and point types are forgotten.
   */
  public final void reset() {
    numTypes = numCoords = 0;
  }


  static public class PathIterator {
    float floatCoords[];

    int typeIdx;

    int pointIdx;

    int colorIdx;

    LinePath path;

    static final int curvecoords[] = { 2, 2, 0 };

    PathIterator(LinePath p2df) {
      this.path = p2df;
      this.floatCoords = p2df.floatCoords;
    }

    public int currentSegment(float[] coords) {
      int type = path.pointTypes[typeIdx];
      int numCoords = curvecoords[type];
      if (numCoords > 0) {
        System.arraycopy(floatCoords, pointIdx, coords, 0, numCoords);
        int color = path.pointColors[colorIdx];
        coords[numCoords + 0] = (color >> 24) & 0xFF;
        coords[numCoords + 1] = (color >> 16) & 0xFF;
        coords[numCoords + 2] = (color >>  8) & 0xFF;
        coords[numCoords + 3] = (color >>  0) & 0xFF;
      }
      return type;
    }

    public int currentSegment(double[] coords) {
      int type = path.pointTypes[typeIdx];
      int numCoords = curvecoords[type];
      if (numCoords > 0) {
        for (int i = 0; i < numCoords; i++) {
          coords[i] = floatCoords[pointIdx + i];
        }
        int color = path.pointColors[colorIdx];
        coords[numCoords + 0] = (color >> 24) & 0xFF;
        coords[numCoords + 1] = (color >> 16) & 0xFF;
        coords[numCoords + 2] = (color >>  8) & 0xFF;
        coords[numCoords + 3] = (color >>  0) & 0xFF;
      }
      return type;
    }

    public int getWindingRule() {
      return path.getWindingRule();
    }

    public boolean isDone() {
      return (typeIdx >= path.numTypes);
    }

    public void next() {
      int type = path.pointTypes[typeIdx++];
      pointIdx += curvecoords[type];
      colorIdx++;
    }
  }


  /////////////////////////////////////////////////////////////////////////////
  //
  // Stroked path methods


  static public LinePath createStrokedPath(LinePath src, float weight,
                                           int caps, int join) {
    return createStrokedPath(src, weight, caps, join, defaultMiterlimit, null);
  }


  static public LinePath createStrokedPath(LinePath src, float weight,
                                           int caps, int join, float miterlimit) {
    return createStrokedPath(src, weight, caps, join, miterlimit, null);
  }


  /**
   * Constructs a solid <code>LinePath</code> with the specified attributes.
   *
   * @param src
   *          the original path to be stroked
   * @param weight
   *          the weight of the stroked path
   * @param cap
   *          the decoration of the ends of the segments in the path
   * @param join
   *          the decoration applied where path segments meet
   * @param miterlimit
   * @param transform
   *
   */
  static public LinePath createStrokedPath(LinePath src, float weight,
                                           int caps, int join,
                                           float miterlimit, PMatrix2D transform) {
    final LinePath dest = new LinePath();

    strokeTo(src, weight, caps, join, miterlimit, transform, new LineStroker() {
      @Override
      public void moveTo(int x0, int y0, int c0) {
        dest.moveTo(S15_16ToFloat(x0), S15_16ToFloat(y0), c0);
      }

      @Override
      public void lineJoin() {
      }

      @Override
      public void lineTo(int x1, int y1, int c1) {
        dest.lineTo(S15_16ToFloat(x1), S15_16ToFloat(y1), c1);
      }

      @Override
      public void close() {
        dest.closePath();
      }

      @Override
      public void end() {
      }
    });

    return dest;
  }


  private static void strokeTo(LinePath src, float width, int caps, int join,
                               float miterlimit, PMatrix2D transform,
                               LineStroker lsink) {
    lsink = new LineStroker(lsink, FloatToS15_16(width), caps, join,
                            FloatToS15_16(miterlimit),
                            transform == null ? identity : transform);

    PathIterator pi = src.getPathIterator();
    pathTo(pi, lsink);
  }


  private static void pathTo(PathIterator pi, LineStroker lsink) {
    float coords[] = new float[6];
    while (!pi.isDone()) {
      int color;
      switch (pi.currentSegment(coords)) {
      case SEG_MOVETO:
        color = ((int)coords[2]<<24) |
                ((int)coords[3]<<16) |
                ((int)coords[4]<< 8) |
                 (int)coords[5];
        lsink.moveTo(FloatToS15_16(coords[0]), FloatToS15_16(coords[1]), color);
        break;
      case SEG_LINETO:
        color = ((int)coords[2]<<24) |
                ((int)coords[3]<<16) |
                ((int)coords[4]<< 8) |
                 (int)coords[5];
        lsink.lineJoin();
        lsink.lineTo(FloatToS15_16(coords[0]), FloatToS15_16(coords[1]), color);
        break;
      case SEG_CLOSE:
        lsink.lineJoin();
        lsink.close();
        break;
      default:
        throw new InternalError("unknown flattened segment type");
      }
      pi.next();
    }
    lsink.end();
  }


  /////////////////////////////////////////////////////////////////////////////
  //
  // Utility methods


  public static float[] copyOf(float[] source, int length) {
    float[] target = new float[length];
    for (int i = 0; i < target.length; i++) {
      if (i > source.length - 1)
        target[i] = 0f;
      else
        target[i] = source[i];
    }
    return target;
  }


  public static byte[] copyOf(byte[] source, int length) {
    byte[] target = new byte[length];
    for (int i = 0; i < target.length; i++) {
      if (i > source.length - 1)
        target[i] = 0;
      else
        target[i] = source[i];
    }
    return target;
  }


  public static int[] copyOf(int[] source, int length) {
    int[] target = new int[length];
    for (int i = 0; i < target.length; i++) {
      if (i > source.length - 1)
        target[i] = 0;
      else
        target[i] = source[i];
    }
    return target;
  }


  // From Ken Turkowski, _Fixed-Point Square Root_, In Graphics Gems V
  public static int isqrt(int x) {
    int fracbits = 16;

    int root = 0;
    int remHi = 0;
    int remLo = x;
    int count = 15 + fracbits / 2;

    do {
      remHi = (remHi << 2) | (remLo >>> 30); // N.B. - unsigned shift R
      remLo <<= 2;
      root <<= 1;
      int testdiv = (root << 1) + 1;
      if (remHi >= testdiv) {
        remHi -= testdiv;
        root++;
      }
    } while (count-- != 0);

    return root;
  }


  public static long lsqrt(long x) {
    int fracbits = 16;

    long root = 0;
    long remHi = 0;
    long remLo = x;
    int count = 31 + fracbits / 2;

    do {
      remHi = (remHi << 2) | (remLo >>> 62); // N.B. - unsigned shift R
      remLo <<= 2;
      root <<= 1;
      long testDiv = (root << 1) + 1;
      if (remHi >= testDiv) {
        remHi -= testDiv;
        root++;
      }
    } while (count-- != 0);

    return root;
  }


  public static double hypot(double x, double y) {
    return Math.sqrt(x * x + y * y);
  }


  public static int hypot(int x, int y) {
    return (int) ((lsqrt((long) x * x + (long) y * y) + 128) >> 8);
  }


  public static long hypot(long x, long y) {
    return (lsqrt(x * x + y * y) + 128) >> 8;
  }


  static int FloatToS15_16(float flt) {
    flt = flt * 65536f + 0.5f;
    if (flt <= -(65536f * 65536f)) {
      return Integer.MIN_VALUE;
    } else if (flt >= (65536f * 65536f)) {
      return Integer.MAX_VALUE;
    } else {
      return (int) Math.floor(flt);
    }
  }


  static float S15_16ToFloat(int fix) {
    return (fix / 65536f);
  }
}
