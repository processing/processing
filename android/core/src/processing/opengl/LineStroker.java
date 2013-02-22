/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package processing.opengl;

import processing.core.PMatrix2D;

public class LineStroker  {
  private LineStroker output;
  private int capStyle;
  private int joinStyle;
  private int m00, m01;
  private int m10, m11;
  private int lineWidth2;
  private long scaledLineWidth2;

  // For any pen offset (pen_dx, pen_dy) that does not depend on
  // the line orientation, the pen should be transformed so that:
  //
  // pen_dx' = m00*pen_dx + m01*pen_dy
  // pen_dy' = m10*pen_dx + m11*pen_dy
  //
  // For a round pen, this means:
  //
  // pen_dx(r, theta) = r*cos(theta)
  // pen_dy(r, theta) = r*sin(theta)
  //
  // pen_dx'(r, theta) = r*(m00*cos(theta) + m01*sin(theta))
  // pen_dy'(r, theta) = r*(m10*cos(theta) + m11*sin(theta))
  private int numPenSegments;
  private int[] pen_dx;
  private int[] pen_dy;

  private boolean[] penIncluded;
  private int[] join;
  private int[] offset = new int[2];
  private int[] reverse = new int[100];
  private int[] miter = new int[2];
  private long miterLimitSq;
  private int prev;
  private int rindex;
  private boolean started;
  private boolean lineToOrigin;
  private boolean joinToOrigin;
  private int sx0, sy0, sx1, sy1, x0, y0;
  private int scolor0, pcolor0, color0;
  private int mx0, my0, omx, omy;
  private int px0, py0;
  private double m00_2_m01_2;
  private double m10_2_m11_2;
  private double m00_m10_m01_m11;

  /**
   * Empty constructor. <code>setOutput</code> and <code>setParameters</code>
   * must be called prior to calling any other methods.
   */
  public LineStroker() {
  }

  /**
   * Constructs a <code>LineStroker</code>.
   *
   * @param output
   *          an output <code>LineStroker</code>.
   * @param lineWidth
   *          the desired line width in pixels, in S15.16 format.
   * @param capStyle
   *          the desired end cap style, one of <code>CAP_BUTT</code>,
   *          <code>CAP_ROUND</code> or <code>CAP_SQUARE</code>.
   * @param joinStyle
   *          the desired line join style, one of <code>JOIN_MITER</code>,
   *          <code>JOIN_ROUND</code> or <code>JOIN_BEVEL</code>.
   * @param miterLimit
   *          the desired miter limit, in S15.16 format.
   * @param transform
   *          a <code>Transform4</code> object indicating the transform that has
   *          been previously applied to all incoming coordinates. This is
   *          required in order to produce consistently shaped end caps and
   *          joins.
   */
  public LineStroker(LineStroker output, int lineWidth, int capStyle, int joinStyle,
                 int miterLimit, PMatrix2D transform) {
    setOutput(output);
    setParameters(lineWidth, capStyle, joinStyle, miterLimit, transform);
  }

  /**
   * Sets the output <code>LineStroker</code> of this <code>LineStroker</code>.
   *
   * @param output
   *          an output <code>LineStroker</code>.
   */
  public void setOutput(LineStroker output) {
    this.output = output;
  }

  /**
   * Sets the parameters of this <code>LineStroker</code>.
   *
   * @param lineWidth
   *          the desired line width in pixels, in S15.16 format.
   * @param capStyle
   *          the desired end cap style, one of <code>CAP_BUTT</code>,
   *          <code>CAP_ROUND</code> or <code>CAP_SQUARE</code>.
   * @param joinStyle
   *          the desired line join style, one of <code>JOIN_MITER</code>,
   *          <code>JOIN_ROUND</code> or <code>JOIN_BEVEL</code>.
   * @param miterLimit
   *          the desired miter limit, in S15.16 format.
   * @param transform
   *          a <code>Transform4</code> object indicating the transform that has
   *          been previously applied to all incoming coordinates. This is
   *          required in order to produce consistently shaped end caps and
   *          joins.
   */
  public void setParameters(int lineWidth, int capStyle, int joinStyle,
                            int miterLimit, PMatrix2D transform) {
    this.m00 = LinePath.FloatToS15_16(transform.m00);
    this.m01 = LinePath.FloatToS15_16(transform.m01);
    this.m10 = LinePath.FloatToS15_16(transform.m10);
    this.m11 = LinePath.FloatToS15_16(transform.m11);

    this.lineWidth2 = lineWidth >> 1;
    this.scaledLineWidth2 = ((long) m00 * lineWidth2) >> 16;
    this.capStyle = capStyle;
    this.joinStyle = joinStyle;

    this.m00_2_m01_2 = (double) m00 * m00 + (double) m01 * m01;
    this.m10_2_m11_2 = (double) m10 * m10 + (double) m11 * m11;
    this.m00_m10_m01_m11 = (double) m00 * m10 + (double) m01 * m11;

    double dm00 = m00 / 65536.0;
    double dm01 = m01 / 65536.0;
    double dm10 = m10 / 65536.0;
    double dm11 = m11 / 65536.0;
    double determinant = dm00 * dm11 - dm01 * dm10;

    if (joinStyle == LinePath.JOIN_MITER) {
      double limit = (miterLimit / 65536.0) * (lineWidth2 / 65536.0)
        * determinant;
      double limitSq = limit * limit;
      this.miterLimitSq = (long) (limitSq * 65536.0 * 65536.0);
    }

    this.numPenSegments = (int) (3.14159f * lineWidth / 65536.0f);
    if (pen_dx == null || pen_dx.length < numPenSegments) {
      this.pen_dx = new int[numPenSegments];
      this.pen_dy = new int[numPenSegments];
      this.penIncluded = new boolean[numPenSegments];
      this.join = new int[2 * numPenSegments];
    }

    for (int i = 0; i < numPenSegments; i++) {
      double r = lineWidth / 2.0;
      double theta = i * 2 * Math.PI / numPenSegments;

      double cos = Math.cos(theta);
      double sin = Math.sin(theta);
      pen_dx[i] = (int) (r * (dm00 * cos + dm01 * sin));
      pen_dy[i] = (int) (r * (dm10 * cos + dm11 * sin));
    }

    prev = LinePath.SEG_CLOSE;
    rindex = 0;
    started = false;
    lineToOrigin = false;
  }

  private void computeOffset(int x0, int y0, int x1, int y1, int[] m) {
    long lx = (long) x1 - (long) x0;
    long ly = (long) y1 - (long) y0;

    int dx, dy;
    if (m00 > 0 && m00 == m11 && m01 == 0 & m10 == 0) {
      long ilen = LinePath.hypot(lx, ly);
      if (ilen == 0) {
        dx = dy = 0;
      } else {
        dx = (int) ((ly * scaledLineWidth2) / ilen);
        dy = (int) (-(lx * scaledLineWidth2) / ilen);
      }
    } else {
      double dlx = x1 - x0;
      double dly = y1 - y0;
      double det = (double) m00 * m11 - (double) m01 * m10;
      int sdet = (det > 0) ? 1 : -1;
      double a = dly * m00 - dlx * m10;
      double b = dly * m01 - dlx * m11;
      double dh = LinePath.hypot(a, b);
      double div = sdet * lineWidth2 / (65536.0 * dh);
      double ddx = dly * m00_2_m01_2 - dlx * m00_m10_m01_m11;
      double ddy = dly * m00_m10_m01_m11 - dlx * m10_2_m11_2;
      dx = (int) (ddx * div);
      dy = (int) (ddy * div);
    }

    m[0] = dx;
    m[1] = dy;
  }

  private void ensureCapacity(int newrindex) {
    if (reverse.length < newrindex) {
      int[] tmp = new int[Math.max(newrindex, 6 * reverse.length / 5)];
      System.arraycopy(reverse, 0, tmp, 0, rindex);
      this.reverse = tmp;
    }
  }

  private boolean isCCW(int x0, int y0, int x1, int y1, int x2, int y2) {
    int dx0 = x1 - x0;
    int dy0 = y1 - y0;
    int dx1 = x2 - x1;
    int dy1 = y2 - y1;
    return (long) dx0 * dy1 < (long) dy0 * dx1;
  }

  private boolean side(int x, int y, int x0, int y0, int x1, int y1) {
    long lx = x;
    long ly = y;
    long lx0 = x0;
    long ly0 = y0;
    long lx1 = x1;
    long ly1 = y1;

    return (ly0 - ly1) * lx + (lx1 - lx0) * ly + (lx0 * ly1 - lx1 * ly0) > 0;
  }

  private int computeRoundJoin(int cx, int cy, int xa, int ya, int xb, int yb,
                               int side, boolean flip, int[] join) {
    int px, py;
    int ncoords = 0;

    boolean centerSide;
    if (side == 0) {
      centerSide = side(cx, cy, xa, ya, xb, yb);
    } else {
      centerSide = (side == 1) ? true : false;
    }
    for (int i = 0; i < numPenSegments; i++) {
      px = cx + pen_dx[i];
      py = cy + pen_dy[i];

      boolean penSide = side(px, py, xa, ya, xb, yb);
      if (penSide != centerSide) {
        penIncluded[i] = true;
      } else {
        penIncluded[i] = false;
      }
    }

    int start = -1, end = -1;
    for (int i = 0; i < numPenSegments; i++) {
      if (penIncluded[i]
        && !penIncluded[(i + numPenSegments - 1) % numPenSegments]) {
        start = i;
      }
      if (penIncluded[i] && !penIncluded[(i + 1) % numPenSegments]) {
        end = i;
      }
    }

    if (end < start) {
      end += numPenSegments;
    }

    if (start != -1 && end != -1) {
      long dxa = cx + pen_dx[start] - xa;
      long dya = cy + pen_dy[start] - ya;
      long dxb = cx + pen_dx[start] - xb;
      long dyb = cy + pen_dy[start] - yb;

      boolean rev = (dxa * dxa + dya * dya > dxb * dxb + dyb * dyb);
      int i = rev ? end : start;
      int incr = rev ? -1 : 1;
      while (true) {
        int idx = i % numPenSegments;
        px = cx + pen_dx[idx];
        py = cy + pen_dy[idx];
        join[ncoords++] = px;
        join[ncoords++] = py;
        if (i == (rev ? start : end)) {
          break;
        }
        i += incr;
      }
    }

    return ncoords / 2;
  }

  private static final long ROUND_JOIN_THRESHOLD = 1000L;

  private static final long ROUND_JOIN_INTERNAL_THRESHOLD = 1000000000L;

  private void drawRoundJoin(int x, int y, int omx, int omy, int mx, int my,
                             int side, int color,
                             boolean flip, boolean rev, long threshold) {
    if ((omx == 0 && omy == 0) || (mx == 0 && my == 0)) {
      return;
    }

    long domx = (long) omx - mx;
    long domy = (long) omy - my;
    long len = domx * domx + domy * domy;
    if (len < threshold) {
      return;
    }

    if (rev) {
      omx = -omx;
      omy = -omy;
      mx = -mx;
      my = -my;
    }

    int bx0 = x + omx;
    int by0 = y + omy;
    int bx1 = x + mx;
    int by1 = y + my;

    int npoints = computeRoundJoin(x, y, bx0, by0, bx1, by1, side, flip, join);
    for (int i = 0; i < npoints; i++) {
      emitLineTo(join[2 * i], join[2 * i + 1], color, rev);
    }
  }

  // Return the intersection point of the lines (ix0, iy0) -> (ix1, iy1)
  // and (ix0p, iy0p) -> (ix1p, iy1p) in m[0] and m[1]
  private void computeMiter(int ix0, int iy0, int ix1, int iy1, int ix0p,
                            int iy0p, int ix1p, int iy1p, int[] m) {
    long x0 = ix0;
    long y0 = iy0;
    long x1 = ix1;
    long y1 = iy1;

    long x0p = ix0p;
    long y0p = iy0p;
    long x1p = ix1p;
    long y1p = iy1p;

    long x10 = x1 - x0;
    long y10 = y1 - y0;
    long x10p = x1p - x0p;
    long y10p = y1p - y0p;

    long den = (x10 * y10p - x10p * y10) >> 16;
    if (den == 0) {
      m[0] = ix0;
      m[1] = iy0;
      return;
    }

    long t = (x1p * (y0 - y0p) - x0 * y10p + x0p * (y1p - y0)) >> 16;
    m[0] = (int) (x0 + (t * x10) / den);
    m[1] = (int) (y0 + (t * y10) / den);
  }

  private void drawMiter(int px0, int py0, int x0, int y0, int x1, int y1,
                         int omx, int omy, int mx, int my, int color,
                         boolean rev) {
    if (mx == omx && my == omy) {
      return;
    }
    if (px0 == x0 && py0 == y0) {
      return;
    }
    if (x0 == x1 && y0 == y1) {
      return;
    }

    if (rev) {
      omx = -omx;
      omy = -omy;
      mx = -mx;
      my = -my;
    }

    computeMiter(px0 + omx, py0 + omy, x0 + omx, y0 + omy, x0 + mx, y0 + my, x1
      + mx, y1 + my, miter);

    // Compute miter length in untransformed coordinates
    long dx = (long) miter[0] - x0;
    long dy = (long) miter[1] - y0;
    long a = (dy * m00 - dx * m10) >> 16;
    long b = (dy * m01 - dx * m11) >> 16;
    long lenSq = a * a + b * b;

    if (lenSq < miterLimitSq) {
      emitLineTo(miter[0], miter[1], color, rev);
    }
  }

  public void moveTo(int x0, int y0, int c0) {
    // System.out.println("LineStroker.moveTo(" + x0/65536.0 + ", " + y0/65536.0 + ")");

    if (lineToOrigin) {
      // not closing the path, do the previous lineTo
      lineToImpl(sx0, sy0, scolor0, joinToOrigin);
      lineToOrigin = false;
    }

    if (prev == LinePath.SEG_LINETO) {
      finish();
    }

    this.sx0 = this.x0 = x0;
    this.sy0 = this.y0 = y0;
    this.scolor0 = this.color0 = c0;
    this.rindex = 0;
    this.started = false;
    this.joinSegment = false;
    this.prev = LinePath.SEG_MOVETO;
  }

  boolean joinSegment = false;

  public void lineJoin() {
    // System.out.println("LineStroker.lineJoin()");
    this.joinSegment = true;
  }

  public void lineTo(int x1, int y1, int c1) {
    // System.out.println("LineStroker.lineTo(" + x1/65536.0 + ", " + y1/65536.0 + ")");

    if (lineToOrigin) {
      if (x1 == sx0 && y1 == sy0) {
        // staying in the starting point
        return;
      }

      // not closing the path, do the previous lineTo
      lineToImpl(sx0, sy0, scolor0, joinToOrigin);
      lineToOrigin = false;
    } else if (x1 == x0 && y1 == y0) {
      return;
    } else if (x1 == sx0 && y1 == sy0) {
      lineToOrigin = true;
      joinToOrigin = joinSegment;
      joinSegment = false;
      return;
    }

    lineToImpl(x1, y1, c1, joinSegment);
    joinSegment = false;
  }

  private void lineToImpl(int x1, int y1, int c1, boolean joinSegment) {
    computeOffset(x0, y0, x1, y1, offset);
    int mx = offset[0];
    int my = offset[1];

    if (!started) {
      emitMoveTo(x0 + mx, y0 + my, color0);
      this.sx1 = x1;
      this.sy1 = y1;
      this.mx0 = mx;
      this.my0 = my;
      started = true;
    } else {
      boolean ccw = isCCW(px0, py0, x0, y0, x1, y1);
      if (joinSegment) {
        if (joinStyle == LinePath.JOIN_MITER) {
          drawMiter(px0, py0, x0, y0, x1, y1, omx, omy, mx, my, color0, ccw);
        } else if (joinStyle == LinePath.JOIN_ROUND) {
          drawRoundJoin(x0, y0, omx, omy, mx, my, 0, color0, false, ccw,
                        ROUND_JOIN_THRESHOLD);
        }
      } else {
        // Draw internal joins as round
        drawRoundJoin(x0, y0, omx, omy, mx, my, 0, color0, false, ccw,
                      ROUND_JOIN_INTERNAL_THRESHOLD);
      }

      emitLineTo(x0, y0, color0, !ccw);
    }

    emitLineTo(x0 + mx, y0 + my, color0, false);
    emitLineTo(x1 + mx, y1 + my, c1, false);

    emitLineTo(x0 - mx, y0 - my, color0, true);
    emitLineTo(x1 - mx, y1 - my, c1, true);

    this.omx = mx;
    this.omy = my;
    this.px0 = x0;
    this.py0 = y0;
    this.pcolor0 = color0;
    this.x0 = x1;
    this.y0 = y1;
    this.color0 = c1;
    this.prev = LinePath.SEG_LINETO;
  }

  public void close() {
    if (lineToOrigin) {
      // ignore the previous lineTo
      lineToOrigin = false;
    }

    if (!started) {
      finish();
      return;
    }

    computeOffset(x0, y0, sx0, sy0, offset);
    int mx = offset[0];
    int my = offset[1];

    // Draw penultimate join
    boolean ccw = isCCW(px0, py0, x0, y0, sx0, sy0);
    if (joinSegment) {
      if (joinStyle == LinePath.JOIN_MITER) {
        drawMiter(px0, py0, x0, y0, sx0, sy0, omx, omy, mx, my, pcolor0, ccw);
      } else if (joinStyle == LinePath.JOIN_ROUND) {
        drawRoundJoin(x0, y0, omx, omy, mx, my, 0, color0, false, ccw,
                      ROUND_JOIN_THRESHOLD);
      }
    } else {
      // Draw internal joins as round
      drawRoundJoin(x0, y0, omx, omy, mx, my, 0, color0, false, ccw,
                    ROUND_JOIN_INTERNAL_THRESHOLD);
    }

    emitLineTo(x0 + mx, y0 + my, color0);
    emitLineTo(sx0 + mx, sy0 + my, scolor0);

    ccw = isCCW(x0, y0, sx0, sy0, sx1, sy1);

    // Draw final join on the outside
    if (!ccw) {
      if (joinStyle == LinePath.JOIN_MITER) {
        drawMiter(x0, y0, sx0, sy0, sx1, sy1, mx, my, mx0, my0, color0, false);
      } else if (joinStyle == LinePath.JOIN_ROUND) {
        drawRoundJoin(sx0, sy0, mx, my, mx0, my0, 0, scolor0, false, false,
                      ROUND_JOIN_THRESHOLD);
      }
    }

    emitLineTo(sx0 + mx0, sy0 + my0, scolor0);
    emitLineTo(sx0 - mx0, sy0 - my0, scolor0); // same as reverse[0], reverse[1]

    // Draw final join on the inside
    if (ccw) {
      if (joinStyle == LinePath.JOIN_MITER) {
        drawMiter(x0, y0, sx0, sy0, sx1, sy1, -mx, -my, -mx0, -my0, color0,
                  false);
      } else if (joinStyle == LinePath.JOIN_ROUND) {
        drawRoundJoin(sx0, sy0, -mx, -my, -mx0, -my0, 0, scolor0, true, false,
                      ROUND_JOIN_THRESHOLD);
      }
    }

    emitLineTo(sx0 - mx, sy0 - my, scolor0);
    emitLineTo(x0 - mx, y0 - my, color0);
    for (int i = rindex - 3; i >= 0; i -= 3) {
      emitLineTo(reverse[i], reverse[i + 1], reverse[i + 2]);
    }

    this.x0 = this.sx0;
    this.y0 = this.sy0;
    this.rindex = 0;
    this.started = false;
    this.joinSegment = false;
    this.prev = LinePath.SEG_CLOSE;
    emitClose();
  }

  public void end() {
    if (lineToOrigin) {
      // not closing the path, do the previous lineTo
      lineToImpl(sx0, sy0, scolor0, joinToOrigin);
      lineToOrigin = false;
    }

    if (prev == LinePath.SEG_LINETO) {
      finish();
    }

    output.end();
    this.joinSegment = false;
    this.prev = LinePath.SEG_MOVETO;
  }

  long lineLength(long ldx, long ldy) {
    long ldet = ((long) m00 * m11 - (long) m01 * m10) >> 16;
    long la = (ldy * m00 - ldx * m10) / ldet;
    long lb = (ldy * m01 - ldx * m11) / ldet;
    long llen = (int) LinePath.hypot(la, lb);
    return llen;
  }

  private void finish() {
    if (capStyle == LinePath.CAP_ROUND) {
      drawRoundJoin(x0, y0, omx, omy, -omx, -omy, 1, color0, false, false,
                    ROUND_JOIN_THRESHOLD);
    } else if (capStyle == LinePath.CAP_SQUARE) {
      long ldx = px0 - x0;
      long ldy = py0 - y0;
      long llen = lineLength(ldx, ldy);
      if (0 < llen) {
        long s = (long) lineWidth2 * 65536 / llen;

        int capx = x0 - (int) (ldx * s >> 16);
        int capy = y0 - (int) (ldy * s >> 16);

        emitLineTo(capx + omx, capy + omy, color0);
        emitLineTo(capx - omx, capy - omy, color0);
      }
    }

    for (int i = rindex - 3; i >= 0; i -= 3) {
      emitLineTo(reverse[i], reverse[i + 1], reverse[i + 2]);
    }
    this.rindex = 0;

    if (capStyle == LinePath.CAP_ROUND) {
      drawRoundJoin(sx0, sy0, -mx0, -my0, mx0, my0, 1, scolor0, false, false,
                    ROUND_JOIN_THRESHOLD);
    } else if (capStyle == LinePath.CAP_SQUARE) {
      long ldx = sx1 - sx0;
      long ldy = sy1 - sy0;
      long llen = lineLength(ldx, ldy);
      if (0  < llen) {
        long s = (long) lineWidth2 * 65536 / llen;

        int capx = sx0 - (int) (ldx * s >> 16);
        int capy = sy0 - (int) (ldy * s >> 16);

        emitLineTo(capx - mx0, capy - my0, scolor0);
        emitLineTo(capx + mx0, capy + my0, scolor0);
      }
    }

    emitClose();
    this.joinSegment = false;
  }

  private void emitMoveTo(int x0, int y0, int c0) {
    output.moveTo(x0, y0, c0);
  }

  private void emitLineTo(int x1, int y1, int c1) {
    output.lineTo(x1, y1, c1);
  }

  private void emitLineTo(int x1, int y1, int c1, boolean rev) {
    if (rev) {
      ensureCapacity(rindex + 3);
      reverse[rindex++] = x1;
      reverse[rindex++] = y1;
      reverse[rindex++] = c1;
    } else {
      emitLineTo(x1, y1, c1);
    }
  }

  private void emitClose() {
    output.close();
  }
}
