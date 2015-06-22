/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2015 The Processing Foundation

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.awt;

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShapeSVG;
import processing.data.*;


/**
 * Implements features for PShape that are specific to AWT and Java2D.
 * At the moment, this is gradients and java.awt.Paint handling.
 */
public class PShapeJava2D extends PShapeSVG {
  Paint strokeGradientPaint;
  Paint fillGradientPaint;


  public PShapeJava2D(XML svg) {
    super(svg);
  }


  public PShapeJava2D(PShapeSVG parent, XML properties, boolean parseKids) {
    super(parent, properties, parseKids);
  }


  @Override
  protected void setParent(PShapeSVG parent) {
    if (parent instanceof PShapeJava2D) {
      PShapeJava2D pj = (PShapeJava2D) parent;
      fillGradientPaint = pj.fillGradientPaint;
      strokeGradientPaint = pj.strokeGradientPaint;

    } else {  // parent is null or not Java2D
      fillGradientPaint = null;
      strokeGradientPaint = null;
    }
  }


  /** Factory method for subclasses. */
  @Override
  protected PShapeSVG createShape(PShapeSVG parent, XML properties, boolean parseKids) {
    return new PShapeJava2D(parent, properties, parseKids);
  }




  static class LinearGradientPaint implements Paint {
    float x1, y1, x2, y2;
    float[] offset;
    int[] color;
    int count;
    float opacity;

    public LinearGradientPaint(float x1, float y1, float x2, float y2,
                               float[] offset, int[] color, int count,
                               float opacity) {
      this.x1 = x1;
      this.y1 = y1;
      this.x2 = x2;
      this.y2 = y2;
      this.offset = offset;
      this.color = color;
      this.count = count;
      this.opacity = opacity;
    }

    public PaintContext createContext(ColorModel cm,
                                      Rectangle deviceBounds, Rectangle2D userBounds,
                                      AffineTransform xform, RenderingHints hints) {
      Point2D t1 = xform.transform(new Point2D.Float(x1, y1), null);
      Point2D t2 = xform.transform(new Point2D.Float(x2, y2), null);
      return new LinearGradientContext((float) t1.getX(), (float) t1.getY(),
                                       (float) t2.getX(), (float) t2.getY());
    }

    public int getTransparency() {
      return TRANSLUCENT;  // why not.. rather than checking each color
    }

    public class LinearGradientContext implements PaintContext {
      int ACCURACY = 2;
      float tx1, ty1, tx2, ty2;

      public LinearGradientContext(float tx1, float ty1, float tx2, float ty2) {
        this.tx1 = tx1;
        this.ty1 = ty1;
        this.tx2 = tx2;
        this.ty2 = ty2;
      }

      public void dispose() { }

      public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }

      public Raster getRaster(int x, int y, int w, int h) {
        WritableRaster raster =
          getColorModel().createCompatibleWritableRaster(w, h);

        int[] data = new int[w * h * 4];

        // make normalized version of base vector
        float nx = tx2 - tx1;
        float ny = ty2 - ty1;
        float len = (float) Math.sqrt(nx*nx + ny*ny);
        if (len != 0) {
          nx /= len;
          ny /= len;
        }

        int span = (int) PApplet.dist(tx1, ty1, tx2, ty2) * ACCURACY;
        if (span <= 0) {
          //System.err.println("span is too small");
          // annoying edge case where the gradient isn't legit
          int index = 0;
          for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
              data[index++] = 0;
              data[index++] = 0;
              data[index++] = 0;
              data[index++] = 255;
            }
          }

        } else {
          int[][] interp = new int[span][4];
          int prev = 0;
          for (int i = 1; i < count; i++) {
            int c0 = color[i-1];
            int c1 = color[i];
            int last = (int) (offset[i] * (span-1));
            //System.out.println("last is " + last);
            for (int j = prev; j <= last; j++) {
              float btwn = PApplet.norm(j, prev, last);
              interp[j][0] = (int) PApplet.lerp((c0 >> 16) & 0xff, (c1 >> 16) & 0xff, btwn);
              interp[j][1] = (int) PApplet.lerp((c0 >> 8) & 0xff, (c1 >> 8) & 0xff, btwn);
              interp[j][2] = (int) PApplet.lerp(c0 & 0xff, c1 & 0xff, btwn);
              interp[j][3] = (int) (PApplet.lerp((c0 >> 24) & 0xff, (c1 >> 24) & 0xff, btwn) * opacity);
              //System.out.println(j + " " + interp[j][0] + " " + interp[j][1] + " " + interp[j][2]);
            }
            prev = last;
          }

          int index = 0;
          for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
              //float distance = 0; //PApplet.dist(cx, cy, x + i, y + j);
              //int which = PApplet.min((int) (distance * ACCURACY), interp.length-1);
              float px = (x + i) - tx1;
              float py = (y + j) - ty1;
              // distance up the line is the dot product of the normalized
              // vector of the gradient start/stop by the point being tested
              int which = (int) ((px*nx + py*ny) * ACCURACY);
              if (which < 0) which = 0;
              if (which > interp.length-1) which = interp.length-1;
              //if (which > 138) System.out.println("grabbing " + which);

              data[index++] = interp[which][0];
              data[index++] = interp[which][1];
              data[index++] = interp[which][2];
              data[index++] = interp[which][3];
            }
          }
        }
        raster.setPixels(0, 0, w, h, data);

        return raster;
      }
    }
  }


  static class RadialGradientPaint implements Paint {
    float cx, cy, radius;
    float[] offset;
    int[] color;
    int count;
    float opacity;

    public RadialGradientPaint(float cx, float cy, float radius,
                               float[] offset, int[] color, int count,
                               float opacity) {
      this.cx = cx;
      this.cy = cy;
      this.radius = radius;
      this.offset = offset;
      this.color = color;
      this.count = count;
      this.opacity = opacity;
    }

    public PaintContext createContext(ColorModel cm,
                                      Rectangle deviceBounds, Rectangle2D userBounds,
                                      AffineTransform xform, RenderingHints hints) {
      return new RadialGradientContext();
    }

    public int getTransparency() {
      return TRANSLUCENT;
    }

    public class RadialGradientContext implements PaintContext {
      int ACCURACY = 5;

      public void dispose() {}

      public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }

      public Raster getRaster(int x, int y, int w, int h) {
        WritableRaster raster =
          getColorModel().createCompatibleWritableRaster(w, h);

        int span = (int) radius * ACCURACY;
        int[][] interp = new int[span][4];
        int prev = 0;
        for (int i = 1; i < count; i++) {
          int c0 = color[i-1];
          int c1 = color[i];
          int last = (int) (offset[i] * (span - 1));
          for (int j = prev; j <= last; j++) {
            float btwn = PApplet.norm(j, prev, last);
            interp[j][0] = (int) PApplet.lerp((c0 >> 16) & 0xff, (c1 >> 16) & 0xff, btwn);
            interp[j][1] = (int) PApplet.lerp((c0 >> 8) & 0xff, (c1 >> 8) & 0xff, btwn);
            interp[j][2] = (int) PApplet.lerp(c0 & 0xff, c1 & 0xff, btwn);
            interp[j][3] = (int) (PApplet.lerp((c0 >> 24) & 0xff, (c1 >> 24) & 0xff, btwn) * opacity);
          }
          prev = last;
        }

        int[] data = new int[w * h * 4];
        int index = 0;
        for (int j = 0; j < h; j++) {
          for (int i = 0; i < w; i++) {
            float distance = PApplet.dist(cx, cy, x + i, y + j);
            int which = PApplet.min((int) (distance * ACCURACY), interp.length-1);

            data[index++] = interp[which][0];
            data[index++] = interp[which][1];
            data[index++] = interp[which][2];
            data[index++] = interp[which][3];
          }
        }
        raster.setPixels(0, 0, w, h, data);

        return raster;
      }
    }
  }


  protected Paint calcGradientPaint(Gradient gradient) {
    if (gradient instanceof LinearGradient) {
      System.out.println("creating linear gradient");
      LinearGradient grad = (LinearGradient) gradient;
      return new LinearGradientPaint(grad.x1, grad.y1, grad.x2, grad.y2,
                                     grad.offset, grad.color, grad.count,
                                     opacity);

    } else if (gradient instanceof RadialGradient) {
      System.out.println("creating radial gradient");
      RadialGradient grad = (RadialGradient) gradient;
      return new RadialGradientPaint(grad.cx, grad.cy, grad.r,
                                     grad.offset, grad.color, grad.count,
                                     opacity);
    }
    return null;
  }


//  protected Paint calcGradientPaint(Gradient gradient,
//                                    float x1, float y1, float x2, float y2) {
//    if (gradient instanceof LinearGradient) {
//      LinearGradient grad = (LinearGradient) gradient;
//      return new LinearGradientPaint(x1, y1, x2, y2,
//                                     grad.offset, grad.color, grad.count,
//                                     opacity);
//    }
//    throw new RuntimeException("Not a linear gradient.");
//  }


//  protected Paint calcGradientPaint(Gradient gradient,
//                                    float cx, float cy, float r) {
//    if (gradient instanceof RadialGradient) {
//      RadialGradient grad = (RadialGradient) gradient;
//      return new RadialGradientPaint(cx, cy, r,
//                                     grad.offset, grad.color, grad.count,
//                                     opacity);
//    }
//    throw new RuntimeException("Not a radial gradient.");
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  @Override
  protected void styles(PGraphics g) {
    super.styles(g);

    //if (g instanceof PGraphicsJava2D) {
    PGraphicsJava2D p2d = (PGraphicsJava2D) g;

    if (strokeGradient != null) {
      p2d.strokeGradient = true;
      if (strokeGradientPaint == null) {
        strokeGradientPaint = calcGradientPaint(strokeGradient);
      }
      p2d.strokeGradientObject = strokeGradientPaint;
    } else {
      // need to shut off, in case parent object has a gradient applied
      //p2d.strokeGradient = false;
    }
    if (fillGradient != null) {
      p2d.fillGradient = true;
      if (fillGradientPaint == null) {
        fillGradientPaint = calcGradientPaint(fillGradient);
      }
      p2d.fillGradientObject = fillGradientPaint;
    } else {
      // need to shut off, in case parent object has a gradient applied
      //p2d.fillGradient = false;
    }
    //}
  }
}