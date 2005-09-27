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
    private Image peer;
    /**
    * A constant with the image width.
    */
    public final int width;
    /**
    * A constant with the image height.
    */
    public final int height;
    
    /**
    * Creates a image with the peer specified.
    *
    * @param img The image.
    */
    public PImage(Image img)
    {
        peer = img;
        width = peer.getWidth();
        height = peer.getHeight();
    }
    
    public PImage(byte[] png, int offset, int length) {
        try {
            peer = Image.createImage(png, offset, length);
            width = peer.getWidth();
            height = peer.getHeight();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
    * Returns the peer image.
    *
    * @return The peer image.
    */

    protected Image getPeer()
    {
        return peer;
    }
}
