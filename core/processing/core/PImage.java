package processing.core;

import javax.microedition.lcdui.*;

/**
 * Part of the Mobile Processing project - http://mobile.processing.org
 *
 * Copyright (c) 2005 Francis Li
 * Copyright (c) 2005 Marlon J. Manrique 
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
 * @author Marlon J. Manrique <marlonj@darkgreenmedia.com>
 */
public class PImage {
    /** The native image. */
    public Image image;
    /** A constant with the image width. */
    public final int width;
    /** A constant with the image height. */
    public final int height;
    /** If true, this is a mutable image */
    public final boolean mutable;
    
    public PImage(int width, int height) {
        image = Image.createImage(width, height);
        this.width = width;
        this.height = height;
        mutable = true;
    }
    
    public PImage(Image img) {
        image = img;
        width = image.getWidth();
        height = image.getHeight();
        mutable = false;
    }
    
    public PImage(byte[] png) {
        this(png, 0, png.length);
    }
    
    public PImage(byte[] png, int offset, int length) {
        try {
            image = Image.createImage(png, offset, length);
            width = image.getWidth();
            height = image.getHeight();
            mutable = false;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public void copy(PImage source, int sx, int sy, int swidth, int sheight, int dx, int dy, int dwidth, int dheight) {
        if (!mutable) {
            throw new RuntimeException("this image cannot be overwritten");
        }
        Graphics g = image.getGraphics();
        if ((dwidth == swidth) && (dheight == sheight)) {
            g.setClip(dx, dy, dwidth, dheight);
            g.drawImage(source.image, dx - sx, dy - sy, Graphics.TOP | Graphics.LEFT);
        } else if (dwidth == swidth) {
            int scaleY = dy - sy;
            for (int y = 0; y < dheight; y++) {
                g.setClip(dx, dy + y, dwidth, 1);
                g.drawImage(source.image, dx - sx, scaleY, Graphics.TOP | Graphics.LEFT);
                scaleY = dy - sy - y * sheight / dheight + y;
            }
        } else if (dheight == sheight) {
            int scaleX = dx - sx;
            for (int x = 0; x < dwidth; x++) {
                g.setClip(dx + x, dy, 1, dheight);
                g.drawImage(source.image, scaleX, dy - sy, Graphics.TOP | Graphics.LEFT);
                scaleX = dx - sx - x * swidth / dwidth + x;
            }
        } else {
            int scaleY = dy - sy;
            for (int y = 0; y < dheight; y++) {
                int scaleX = dx - sx;
                for (int x = 0; x < dwidth; x++) {
                    g.setClip(dx + x, dy + y, 1, 1);
                    g.drawImage(source.image, scaleX, scaleY, Graphics.TOP | Graphics.LEFT);
                    scaleX = dx - sx - x * swidth / dwidth + x;
                }
                scaleY = dy - sy - y * sheight / dheight + y;
            }
        }
    }
}
