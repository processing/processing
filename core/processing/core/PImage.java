package processing.core;

import javax.microedition.lcdui.*;

/** A basic, MIDP 1.0 compliant implementation of PImage. This is an immutable
 * image, due to limitations in MIDP 1.0.
 *
 * @author Marlon J. Manrique <marlonj@darkgreenmedia.com>
 */
public class PImage {
    /**
    * The native image.
    */
    public Image image;
    /**
    * A constant with the image width.
    */
    public final int width;
    /**
    * A constant with the image height.
    */
    public final int height;
    
    /**
    * Creates a image with the image specified.
    *
    * @param img The image.
    */
    public PImage(Image img)
    {
        image = img;
        width = image.getWidth();
        height = image.getHeight();
    }
    
    public PImage(byte[] png) {
        this(png, 0, png.length);
    }
    
    public PImage(byte[] png, int offset, int length) {
        try {
            image = Image.createImage(png, offset, length);
            width = image.getWidth();
            height = image.getHeight();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
