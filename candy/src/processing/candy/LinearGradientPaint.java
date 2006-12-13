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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import processing.core.PApplet;


public class LinearGradientPaint implements Paint {
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
        //return new LinearGradientContext();
        /*
        Point2D t1 = xform.transform(new Point2D.Float(x1, y1), null);
        Point2D t2 = xform.transform(new Point2D.Float(x2, y2), null);
        return new LinearGradientContext((float) t1.getX(), (float) t1.getY(),
                                         (float) t2.getX(), (float) t2.getY(),
                                         offset, color, count, opacity);
         */
        Point2D t1 = xform.transform(new Point2D.Float(x1, y1), null);
        Point2D t2 = xform.transform(new Point2D.Float(x2, y2), null);
        return new LinearGradientContext((float) t1.getX(), (float) t1.getY(),
                                         (float) t2.getX(), (float) t2.getY());

    }

    
    public int getTransparency() {
        /*
            int a1 = mPointColor.getAlpha();
            int a2 = mBackgroundColor.getAlpha();
            return (((a1 & a2) == 0xff) ? OPAQUE : TRANSLUCENT);
         */
        //return OPAQUE;
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
            
            //System.out.println(x1 + " " + y1 + " " + x2 + " " + y2 + " .. t = " + 
              //                 tx1 + " " + ty1 + " " + tx2 + " " + ty2);
        }        
        
        /*
    float x1, y1, x2, y2;
    float[] offset;
    int[] color;
    int count;
    float opacity;

    public LinearGradientContext(float x1, float y1, float x2, float y2,
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
         */

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