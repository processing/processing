package processing.core;

import javax.microedition.lcdui.*;

/** A basic, MIDP 1.0 compliant implementation of PImage. This is an immutable
 * image, due to limitations in MIDP 1.0.
 *
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
