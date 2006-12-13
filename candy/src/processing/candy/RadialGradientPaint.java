/*
  Candy 2 - SVG Importer for Processing - http://processing.org

  Code to handle linear and radial gradients 
  Copyright (c) 2006- Ben Fry and Casey Reas

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
*/

package processing.candy;

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import processing.core.PApplet;


public class RadialGradientPaint implements Paint {
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
        
        /*
        Point2D transformedPoint =
            xform.transform(new Point2D.Float(cx, cy), null);
        // this causes problems
        //Point2D transformedRadius =
        //    xform.deltaTransform(new Point2D.Float(radius, radius), null);
        return new RadialGradientContext((float) transformedPoint.getX(),
                                         (float) transformedPoint.getY(),
                                         radius, //(float) transformedRadius.distance(0, 0),
                                         offset, color, count, opacity);
         */
    }

    public int getTransparency() {
        /*
            int a1 = mPointColor.getAlpha();
            int a2 = mBackgroundColor.getAlpha();
            return (((a1 & a2) == 0xff) ? OPAQUE : TRANSLUCENT);
         */
        //return (opacity == 1) ? OPAQUE : TRANSLUCENT;
        return TRANSLUCENT;  // why not.. rather than checking each color
    }


    public class RadialGradientContext implements PaintContext {
        int ACCURACY = 5;

        //float cx, cy, radius;
        //float[] offset;
        //int[] color;
        //int count;
        //float opacity;

        /*
        public RadialGradientContext(float cx, float cy, float radius,
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
        */

        public void dispose() {}

        public ColorModel getColorModel() { return ColorModel.getRGBdefault(); }

        public Raster getRaster(int x, int y, int w, int h) {
            WritableRaster raster =
                getColorModel().createCompatibleWritableRaster(w, h);

            //System.out.println("radius here is " + radius);
            //System.out.println("count is " + count);
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
                    //System.out.println(interp[j][3]);
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


