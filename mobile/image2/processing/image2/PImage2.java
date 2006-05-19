package processing.image2;

import processing.core.*;
import javax.microedition.lcdui.*;

/**
 * Part of the Mobile Processing project - http://mobile.processing.org
 *
 * Copyright (c) 2006 Francis Li
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 *
 * @author Francis Li <mail@francisli.com>
 */
public class PImage2 extends PImage {    
    public int[] pixels;
    
    public PImage2(PImage img) {
        this(img.image);
    }
    
    public PImage2(Image img) {
        super(img.getWidth(), img.getHeight(), true);
        
        this.pixels = new int[width * height];
        img.getRGB(pixels, 0, width, 0, 0, width, height);
    }
    
    public PImage2(int width, int height) {
        super(width, height, true);
        this.pixels = new int[width * height];
    }
    
    public PImage2(int width, int height, int color) {
        this(width, height);
        for (int i = 0, length = pixels.length; i < length; i++) {
            pixels[i] = color;
        }
    }
    
    public PImage2(int[] pixels, int width, int height) {
        super(width, height, true);
        this.pixels = pixels;
    }
    
    public int get(int x, int y) {
        return pixels[y * width + x];
    }
    
    public void set(int x, int y, int color) {
        pixels[y * width + x] = color;
    }
        
    public int[] get() {
        int[] copy = new int[pixels.length];
        System.arraycopy(pixels, 0, copy, 0, pixels.length);
        return copy;
    }
    
    public int[] get(int x, int y, int width, int height) {
        if (PCanvas.imageMode == PMIDlet.CORNERS) {
            width -= x;
            height -= y;
        }
        int[] area = new int[width * height];
        for (int scanline = 0; scanline < height; scanline++) {
            System.arraycopy(pixels, y * this.width + x, area, scanline * width, width);
            y++;
        }
        return area;
    }
    
    public void mask(int alpha[]) {
        if (alpha.length != pixels.length) {
            throw new RuntimeException("The mask must be the same size as the image");
        }
        for (int i = 0, length = pixels.length; i < length; i++) {
            pixels[i] = ((alpha[i] & 0xff) << 24) | (pixels[i] & 0xffffff);
        }
    }

    public void mask(PImage source) {
        int spixels[];
        if (source instanceof PImage2) {
            spixels = ((PImage2) source).pixels;
        } else {
            spixels = new int[source.width * source.height];
            source.image.getRGB(spixels, 0, source.width, 0, 0, source.width, source.height);            
        }
        mask(spixels);
    }
    
    public void loadPixels() {
        PCanvas.buffer.getRGB(pixels, 0, width, 0, 0, width, height);
    }
    
    public void loadPixels(int sx, int sy, int swidth, int sheight, int dx, int dy, int dwidth, int dheight) {
        int width = PCanvas.buffer.getWidth();
        int height = PCanvas.buffer.getHeight();
        int[] spixels = new int[width * height];
        PCanvas.buffer.getRGB(spixels, 0, width, 0, 0, width, height);
        copy(spixels, width, sx, sy, swidth, sheight, dx, dy, dwidth, dheight);        
    }
    
    public void copy(int sx, int sy, int swidth, int sheight, int dx, int dy, int dwidth, int dheight) {
        int[] spixels = get(sx, sy, swidth, sheight);
        if (PCanvas.imageMode == PMIDlet.CORNERS) {
            swidth -= sx;
            sheight -= sy;
        }
        copy(spixels, swidth, 0, 0, swidth, sheight, dx, dy, dwidth, dheight);
    }
    
    public void copy(PImage source, int sx, int sy, int swidth, int sheight, int dx, int dy, int dwidth, int dheight) {
        int[] spixels;
        if (source instanceof PImage2) {
            spixels = ((PImage2) source).pixels;
        } else {
            spixels = new int[source.width * source.height];
            source.image.getRGB(spixels, 0, source.width, 0, 0, source.width, source.height);
        }
        copy(spixels, source.width, sx, sy, swidth, sheight, dx, dy, dwidth, dheight);
    }
    
    private void copy(int[] source, int scanlength, int sx, int sy, int swidth, int sheight, int dx, int dy, int dwidth, int dheight) {
        if (PCanvas.imageMode == PMIDlet.CORNERS) {
            swidth = swidth - sx;
            sheight = sheight - sy;
            dwidth = dwidth - dx;
            dheight = dheight - dy;
        }
        if (dwidth == swidth) {
            int scaleY = sy * scanlength + sx;
            dy = dy * width + dx;
            for (int y = 0; y < dheight; y++) {
                System.arraycopy(source, scaleY, pixels, dy, swidth);
                dy += width;
                scaleY = (sy + y * sheight / dheight) * scanlength + sx;
            }
        } else {
            int scaleY = sy * scanlength + sx;
            for (int y = 0; y < dheight; y++) {
                int di = (dy + y) * width + dx;
                int si = scaleY;
                for (int x = 0; x < dwidth; x++) {
                    pixels[di] = source[si];
                    di++;
                    si = scaleY + x * swidth / dwidth;
                }
                scaleY = (sy + y * sheight / dheight) * scanlength + sx;
            }
        }
    }
    
    protected void draw(Graphics g, int x, int y) {
        g.drawRGB(pixels, 0, width, x, y, width, height, true);
    }
}
