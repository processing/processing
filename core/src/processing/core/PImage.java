/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-14 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Iterator;

import javax.imageio.*;
import javax.imageio.metadata.*;


/**
   * ( begin auto-generated from PImage.xml )
   *
   * Datatype for storing images. Processing can display <b>.gif</b>,
   * <b>.jpg</b>, <b>.tga</b>, and <b>.png</b> images. Images may be
   * displayed in 2D and 3D space. Before an image is used, it must be loaded
   * with the <b>loadImage()</b> function. The <b>PImage</b> class contains
   * fields for the <b>width</b> and <b>height</b> of the image, as well as
   * an array called <b>pixels[]</b> that contains the values for every pixel
   * in the image. The methods described below allow easy access to the
   * image's pixels and alpha channel and simplify the process of compositing.<br/>
   * <br/> using the <b>pixels[]</b> array, be sure to use the
   * <b>loadPixels()</b> method on the image to make sure that the pixel data
   * is properly loaded.<br/>
   * <br/> create a new image, use the <b>createImage()</b> function. Do not
   * use the syntax <b>new PImage()</b>.
   *
   * ( end auto-generated )
   *
 * @webref image
 * @usage Web &amp; Application
 * @instanceName pimg any object of type PImage
 * @see PApplet#loadImage(String)
 * @see PApplet#imageMode(int)
 * @see PApplet#createImage(int, int, int)
 */
public class PImage implements PConstants, Cloneable {

  private static final byte TIFF_HEADER[] = {
    77, 77, 0, 42, 0, 0, 0, 8, 0, 9, 0, -2, 0, 4, 0, 0, 0, 1, 0, 0,
    0, 0, 1, 0, 0, 3, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 3, 0, 0, 0, 1,
    0, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 3, 0, 0, 0, 122, 1, 6, 0, 3, 0,
    0, 0, 1, 0, 2, 0, 0, 1, 17, 0, 4, 0, 0, 0, 1, 0, 0, 3, 0, 1, 21,
    0, 3, 0, 0, 0, 1, 0, 3, 0, 0, 1, 22, 0, 3, 0, 0, 0, 1, 0, 0, 0, 0,
    1, 23, 0, 4, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 8, 0, 8
  };

  private static final String TIFF_ERROR = "Error: Processing can only read its own TIFF files.";

  /**
   * Format for this image, one of RGB, ARGB or ALPHA.
   * note that RGB images still require 0xff in the high byte
   * because of how they'll be manipulated by other functions
   */
  public int format;

  /**
   * ( begin auto-generated from pixels.xml )
   *
   * Array containing the values for all the pixels in the display window.
   * These values are of the color datatype. This array is the size of the
   * display window. For example, if the image is 100x100 pixels, there will
   * be 10000 values and if the window is 200x300 pixels, there will be 60000
   * values. The <b>index</b> value defines the position of a value within
   * the array. For example, the statement <b>color b = pixels[230]</b> will
   * set the variable <b>b</b> to be equal to the value at that location in
   * the array.<br />
   * <br />
   * Before accessing this array, the data must loaded with the
   * <b>loadPixels()</b> function. After the array data has been modified,
   * the <b>updatePixels()</b> function must be run to update the changes.
   * Without <b>loadPixels()</b>, running the code may (or will in future
   * releases) result in a NullPointerException.
   *
   * ( end auto-generated )
   *
   * @webref image:pixels
   * @usage web_application
   * @brief     Array containing the color of every pixel in the image
   */
  public int[] pixels;

  /** 1 for most images, 2 for hi-dpi/retina */
  public int pixelDensity = 1;

  /** Actual dimensions of pixels array, taking into account the 2x setting. */
  public int pixelWidth;
  public int pixelHeight;

  /**
   * ( begin auto-generated from PImage_width.xml )
   *
   * The width of the image in units of pixels.
   *
   * ( end auto-generated )
   * @webref pimage:field
   * @usage web_application
   * @brief     Image width
   */
  public int width;

  /**
   * ( begin auto-generated from PImage_height.xml )
   *
   * The height of the image in units of pixels.
   *
   * ( end auto-generated )
   * @webref pimage:field
   * @usage web_application
   * @brief     Image height
   */
  public int height;

  /**
   * Path to parent object that will be used with save().
   * This prevents users from needing savePath() to use PImage.save().
   */
  public PApplet parent;


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /** modified portion of the image */
  protected boolean modified;
  protected int mx1, my1, mx2, my2;

  /** Loaded pixels flag */
  public boolean loaded = false;

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // private fields
  private int fracU, ifU, fracV, ifV, u1, u2, v1, v2, sX, sY, iw, iw1, ih1;
  private int ul, ll, ur, lr, cUL, cLL, cUR, cLR;
  private int srcXOffset, srcYOffset;
  private int r, g, b, a;
  private int[] srcBuffer;

  // fixed point precision is limited to 15 bits!!
  static final int PRECISIONB = 15;
  static final int PRECISIONF = 1 << PRECISIONB;
  static final int PREC_MAXVAL = PRECISIONF-1;
  static final int PREC_ALPHA_SHIFT = 24-PRECISIONB;
  static final int PREC_RED_SHIFT = 16-PRECISIONB;

  // internal kernel stuff for the gaussian blur filter
  private int blurRadius;
  private int blurKernelSize;
  private int[] blurKernel;
  private int[][] blurMult;

  // colour component bitmasks (moved from PConstants in 2.0b7)
  public static final int ALPHA_MASK = 0xff000000;
  public static final int RED_MASK   = 0x00ff0000;
  public static final int GREEN_MASK = 0x0000ff00;
  public static final int BLUE_MASK  = 0x000000ff;


  //////////////////////////////////////////////////////////////


  /**
   * ( begin auto-generated from PImage.xml )
   *
   * Datatype for storing images. Processing can display <b>.gif</b>,
   * <b>.jpg</b>, <b>.tga</b>, and <b>.png</b> images. Images may be
   * displayed in 2D and 3D space. Before an image is used, it must be loaded
   * with the <b>loadImage()</b> function. The <b>PImage</b> object contains
   * fields for the <b>width</b> and <b>height</b> of the image, as well as
   * an array called <b>pixels[]</b> which contains the values for every
   * pixel in the image. A group of methods, described below, allow easy
   * access to the image's pixels and alpha channel and simplify the process
   * of compositing.
   * <br/> <br/>
   * Before using the <b>pixels[]</b> array, be sure to use the
   * <b>loadPixels()</b> method on the image to make sure that the pixel data
   * is properly loaded.
   * <br/> <br/>
   * To create a new image, use the <b>createImage()</b> function (do not use
   * <b>new PImage()</b>).
   * ( end auto-generated )
   * @nowebref
   * @usage web_application
   * @see PApplet#loadImage(String, String)
   * @see PApplet#imageMode(int)
   * @see PApplet#createImage(int, int, int)
   */
  public PImage() {
    format = ARGB;  // default to ARGB images for release 0116
    pixelDensity = 1;
  }


  /**
   * @nowebref
   * @param width image width
   * @param height image height
   */
  public PImage(int width, int height) {
    init(width, height, RGB, 1);

    // toxi: is it maybe better to init the image with max alpha enabled?
    //for(int i=0; i<pixels.length; i++) pixels[i]=0xffffffff;
    // fry: i'm opting for the full transparent image, which is how
    // photoshop works, and our audience oughta be familiar with.
    // also, i want to avoid having to set all those pixels since
    // in java it's super slow, and most using this fxn will be
    // setting all the pixels anyway.
    // toxi: agreed and same reasons why i left it out ;)
  }


  /**
   * @nowebref
   * @param format Either RGB, ARGB, ALPHA (grayscale alpha channel)
   */
  public PImage(int width, int height, int format) {
    init(width, height, format, 1);
  }


  public PImage(int width, int height, int format, int factor) {
    init(width, height, format, factor);
  }


  /**
   * Do not remove, see notes in the other variant.
   */
  public void init(int width, int height, int format) {  // ignore
    init(width, height, format, 1);
  }


  /**
   * Function to be used by subclasses of PImage to init later than
   * at the constructor, or re-init later when things changes.
   * Used by Capture and Movie classes (and perhaps others),
   * because the width/height will not be known when super() is called.
   * (Leave this public so that other libraries can do the same.)
   */
  public void init(int width, int height, int format, int factor) {  // ignore
    this.width = width;
    this.height = height;
    this.format = format;
    this.pixelDensity = factor;

    pixelWidth = width * pixelDensity;
    pixelHeight = height * pixelDensity;
    this.pixels = new int[pixelWidth * pixelHeight];
  }


  /**
   * Check the alpha on an image, using a really primitive loop.
   */
  protected void checkAlpha() {
    if (pixels == null) return;

    for (int i = 0; i < pixels.length; i++) {
      // since transparency is often at corners, hopefully this
      // will find a non-transparent pixel quickly and exit
      if ((pixels[i] & 0xff000000) != 0xff000000) {
        format = ARGB;
        break;
      }
    }
  }


  //////////////////////////////////////////////////////////////

  public PImage(int width, int height, int[] pixels, boolean requiresCheckAlpha, PApplet parent) {
    initFromPixels(
        width,
        height,
        pixels,
        RGB,
        1
    );

    this.parent = parent;

    if (requiresCheckAlpha) {
      checkAlpha();
    }
  }

  public PImage(int width, int height, int[] pixels, boolean requiresCheckAlpha, PApplet parent,
                int format, int factor) {

    initFromPixels(width, height, pixels, format, factor);
    this.parent = parent;

    if (requiresCheckAlpha) {
      checkAlpha();
    }
  }

  private void initFromPixels(int width, int height, int[] pixels, int format, int factor) {
    this.width = width;
    this.height = height;
    this.format = format;
    this.pixelDensity = factor;
    this.pixels = pixels;
  }

  /**
   * Construct a new PImage from a java.awt.Image. This constructor assumes
   * that you've done the work of making sure a MediaTracker has been used
   * to fully download the data and that the img is valid.
   *
   * @nowebref
   * @param img assumes a MediaTracker has been used to fully download
   * the data and the img is valid
   */
  public PImage(Image img) {
    format = RGB;
    if (img instanceof BufferedImage) {
      BufferedImage bi = (BufferedImage) img;
      width = bi.getWidth();
      height = bi.getHeight();
      int type = bi.getType();
      if (type == BufferedImage.TYPE_3BYTE_BGR ||
          type == BufferedImage.TYPE_4BYTE_ABGR) {
        pixels = new int[width * height];
        bi.getRGB(0, 0, width, height, pixels, 0, width);
        if (type == BufferedImage.TYPE_4BYTE_ABGR) {
          format = ARGB;
        } else {
          opaque();
        }
      } else {
        DataBuffer db = bi.getRaster().getDataBuffer();
        if (db instanceof DataBufferInt) {
          pixels = ((DataBufferInt) db).getData();
          if (type == BufferedImage.TYPE_INT_ARGB) {
            format = ARGB;
          } else if (type == BufferedImage.TYPE_INT_RGB) {
            opaque();
          }
        }
      }
    }
    // Implements fall-through if not DataBufferInt above, or not a
    // known type, or not DataBufferInt for the data itself.
    if (pixels == null) {  // go the old school Java 1.0 route
      width = img.getWidth(null);
      height = img.getHeight(null);
      pixels = new int[width * height];
      PixelGrabber pg =
        new PixelGrabber(img, 0, 0, width, height, pixels, 0, width);
      try {
        pg.grabPixels();
      } catch (InterruptedException e) { }
    }
    pixelDensity = 1;
    pixelWidth = width;
    pixelHeight = height;
  }


  /**
   * Use the getNative() method instead, which allows library interfaces to be
   * written in a cross-platform fashion for desktop, Android, and others.
   * This is still included for PGraphics objects, which may need the image.
   */
  public Image getImage() {  // ignore
    return (Image) getNative();
  }


  /**
   * Returns a native BufferedImage from this PImage.
   */
  public Object getNative() {  // ignore
    loadPixels();
    int type = (format == RGB) ?
      BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
    BufferedImage image = new BufferedImage(pixelWidth, pixelHeight, type);
    WritableRaster wr = image.getRaster();
    wr.setDataElements(0, 0, pixelWidth, pixelHeight, pixels);
    return image;
  }


  //////////////////////////////////////////////////////////////

  // MARKING IMAGE AS MODIFIED / FOR USE w/ GET/SET


  public boolean isModified() {  // ignore
    return modified;
  }


  public void setModified() {  // ignore
    modified = true;
    mx1 = 0;
    my1 = 0;
    mx2 = pixelWidth;
    my2 = pixelHeight;
  }


  public void setModified(boolean m) {  // ignore
    modified = m;
  }


  public int getModifiedX1() {  // ignore
    return mx1;
  }


  public int getModifiedX2() {  // ignore
    return mx2;
  }


  public int getModifiedY1() {  // ignore
    return my1;
  }


  public int getModifiedY2() {  // ignore
    return my2;
  }


  /**
   * ( begin auto-generated from PImage_loadPixels.xml )
   *
   * Loads the pixel data for the image into its <b>pixels[]</b> array. This
   * function must always be called before reading from or writing to <b>pixels[]</b>.
   * <br/><br/> renderers may or may not seem to require <b>loadPixels()</b>
   * or <b>updatePixels()</b>. However, the rule is that any time you want to
   * manipulate the <b>pixels[]</b> array, you must first call
   * <b>loadPixels()</b>, and after changes have been made, call
   * <b>updatePixels()</b>. Even if the renderer may not seem to use this
   * function in the current Processing release, this will always be subject
   * to change.
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   * Call this when you want to mess with the pixels[] array.
   * <p/>
   * For subclasses where the pixels[] buffer isn't set by default,
   * this should copy all data into the pixels[] array
   *
   * @webref pimage:pixels
   * @brief Loads the pixel data for the image into its pixels[] array
   * @usage web_application
   */
  public void loadPixels() {  // ignore
    if (pixels == null || pixels.length != pixelWidth*pixelHeight) {
      pixels = new int[pixelWidth*pixelHeight];
    }
    setLoaded();
  }


  public void updatePixels() {  // ignore
    updatePixels(0, 0, pixelWidth, pixelHeight);
  }


  /**
   * ( begin auto-generated from PImage_updatePixels.xml )
   *
   * Updates the image with the data in its <b>pixels[]</b> array. Use in
   * conjunction with <b>loadPixels()</b>. If you're only reading pixels from
   * the array, there's no need to call <b>updatePixels()</b>.
   * <br/><br/> renderers may or may not seem to require <b>loadPixels()</b>
   * or <b>updatePixels()</b>. However, the rule is that any time you want to
   * manipulate the <b>pixels[]</b> array, you must first call
   * <b>loadPixels()</b>, and after changes have been made, call
   * <b>updatePixels()</b>. Even if the renderer may not seem to use this
   * function in the current Processing release, this will always be subject
   * to change.
   * <br/> <br/>
   * Currently, none of the renderers use the additional parameters to
   * <b>updatePixels()</b>, however this may be implemented in the future.
   *
   * ( end auto-generated )
   * <h3>Advanced</h3>
   * Mark the pixels in this region as needing an update.
   * This is not currently used by any of the renderers, however the api
   * is structured this way in the hope of being able to use this to
   * speed things up in the future.
   * @webref pimage:pixels
   * @brief Updates the image with the data in its pixels[] array
   * @usage web_application
   * @param x x-coordinate of the upper-left corner
   * @param y y-coordinate of the upper-left corner
   * @param w width
   * @param h height
   */
  public void updatePixels(int x, int y, int w, int h) {  // ignore
    int x2 = x + w;
    int y2 = y + h;

    if (!modified) {
      mx1 = PApplet.max(0, x);
      mx2 = PApplet.min(pixelWidth, x2);
      my1 = PApplet.max(0, y);
      my2 = PApplet.min(pixelHeight, y2);
      modified = true;

    } else {
      if (x < mx1) mx1 = PApplet.max(0, x);
      if (x > mx2) mx2 = PApplet.min(pixelWidth, x);
      if (y < my1) my1 = PApplet.max(0, y);
      if (y > my2) my2 = PApplet.min(pixelHeight, y);

      if (x2 < mx1) mx1 = PApplet.max(0, x2);
      if (x2 > mx2) mx2 = PApplet.min(pixelWidth, x2);
      if (y2 < my1) my1 = PApplet.max(0, y2);
      if (y2 > my2) my2 = PApplet.min(pixelHeight, y2);
    }
  }


  //////////////////////////////////////////////////////////////

  // COPYING IMAGE DATA


  /**
   * Duplicate an image, returns new PImage object.
   * The pixels[] array for the new object will be unique
   * and recopied from the source image. This is implemented as an
   * override of Object.clone(). We recommend using get() instead,
   * because it prevents you from needing to catch the
   * CloneNotSupportedException, and from doing a cast from the result.
   */
  @Override
  public Object clone() throws CloneNotSupportedException {  // ignore
    return get();
  }


  /**
   * ( begin auto-generated from PImage_resize.xml )
   *
   * Resize the image to a new width and height. To make the image scale
   * proportionally, use 0 as the value for the <b>wide</b> or <b>high</b>
   * parameter. For instance, to make the width of an image 150 pixels, and
   * change the height using the same proportion, use resize(150, 0).<br />
   * <br />
   * Even though a PGraphics is technically a PImage, it is not possible to
   * rescale the image data found in a PGraphics. (It's simply not possible
   * to do this consistently across renderers: technically infeasible with
   * P3D, or what would it even do with PDF?) If you want to resize PGraphics
   * content, first get a copy of its image data using the <b>get()</b>
   * method, and call <b>resize()</b> on the PImage that is returned.
   *
   * ( end auto-generated )
   * @webref pimage:method
   * @brief Changes the size of an image to a new width and height
   * @usage web_application
   * @param w the resized image width
   * @param h the resized image height
   * @see PImage#get(int, int, int, int)
   */
  public void resize(int w, int h) {  // ignore
    if (w <= 0 && h <= 0) {
      throw new IllegalArgumentException("width or height must be > 0 for resize");
    }

    if (w == 0) {  // Use height to determine relative size
      float diff = (float) h / (float) height;
      w = (int) (width * diff);
    } else if (h == 0) {  // Use the width to determine relative size
      float diff = (float) w / (float) width;
      h = (int) (height * diff);
    }

    BufferedImage img =
      shrinkImage((BufferedImage) getNative(), w*pixelDensity, h*pixelDensity);

    PImage temp = new PImage(img);
    this.pixelWidth = temp.width;
    this.pixelHeight = temp.height;

    // Get the resized pixel array
    this.pixels = temp.pixels;

    this.width = pixelWidth / pixelDensity;
    this.height = pixelHeight / pixelDensity;

    // Mark the pixels array as altered
    updatePixels();
  }


  // Adapted from getFasterScaledInstance() method from page 111 of
  // "Filthy Rich Clients" by Chet Haase and Romain Guy
  // Additional modifications and simplifications have been added,
  // plus a fix to deal with an infinite loop if images are expanded.
  // http://code.google.com/p/processing/issues/detail?id=1463
  static private BufferedImage shrinkImage(BufferedImage img,
                                           int targetWidth, int targetHeight) {
    int type = (img.getTransparency() == Transparency.OPAQUE) ?
      BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
    BufferedImage outgoing = img;
    BufferedImage scratchImage = null;
    Graphics2D g2 = null;
    int prevW = outgoing.getWidth();
    int prevH = outgoing.getHeight();
    boolean isTranslucent = img.getTransparency() != Transparency.OPAQUE;

    // Use multi-step technique: start with original size, then scale down in
    // multiple passes with drawImage() until the target size is reached
    int w = img.getWidth();
    int h = img.getHeight();

    do {
      if (w > targetWidth) {
        w /= 2;
        // if this is the last step, do the exact size
        if (w < targetWidth) {
          w = targetWidth;
        }
      } else if (targetWidth >= w) {
        w = targetWidth;
      }
      if (h > targetHeight) {
        h /= 2;
        if (h < targetHeight) {
          h = targetHeight;
        }
      } else if (targetHeight >= h) {
        h = targetHeight;
      }
      if (scratchImage == null || isTranslucent) {
        // Use a single scratch buffer for all iterations and then copy
        // to the final, correctly-sized image before returning
        scratchImage = new BufferedImage(w, h, type);
        g2 = scratchImage.createGraphics();
      }
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                          RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2.drawImage(outgoing, 0, 0, w, h, 0, 0, prevW, prevH, null);
      prevW = w;
      prevH = h;
      outgoing = scratchImage;
    } while (w != targetWidth || h != targetHeight);

    if (g2 != null) {
      g2.dispose();
    }

    // If we used a scratch buffer that is larger than our target size,
    // create an image of the right size and copy the results into it
    if (targetWidth != outgoing.getWidth() ||
        targetHeight != outgoing.getHeight()) {
      scratchImage = new BufferedImage(targetWidth, targetHeight, type);
      g2 = scratchImage.createGraphics();
      g2.drawImage(outgoing, 0, 0, null);
      g2.dispose();
      outgoing = scratchImage;
    }
    return outgoing;
  }


  //////////////////////////////////////////////////////////////

  // MARKING IMAGE AS LOADED / FOR USE IN RENDERERS


  public boolean isLoaded() { // ignore
    return loaded;
  }


  public void setLoaded() {  // ignore
    loaded = true;
  }


  public void setLoaded(boolean l) {  // ignore
    loaded = l;
  }


  //////////////////////////////////////////////////////////////

  // GET/SET PIXELS


  /**
   * ( begin auto-generated from PImage_get.xml )
   *
   * Reads the color of any pixel or grabs a section of an image. If no
   * parameters are specified, the entire image is returned. Use the <b>x</b>
   * and <b>y</b> parameters to get the value of one pixel. Get a section of
   * the display window by specifying an additional <b>width</b> and
   * <b>height</b> parameter. When getting an image, the <b>x</b> and
   * <b>y</b> parameters define the coordinates for the upper-left corner of
   * the image, regardless of the current <b>imageMode()</b>.<br />
   * <br />
   * If the pixel requested is outside of the image window, black is
   * returned. The numbers returned are scaled according to the current color
   * ranges, but only RGB values are returned by this function. For example,
   * even though you may have drawn a shape with <b>colorMode(HSB)</b>, the
   * numbers returned will be in RGB format.<br />
   * <br />
   * Getting the color of a single pixel with <b>get(x, y)</b> is easy, but
   * not as fast as grabbing the data directly from <b>pixels[]</b>. The
   * equivalent statement to <b>get(x, y)</b> using <b>pixels[]</b> is
   * <b>pixels[y*width+x]</b>. See the reference for <b>pixels[]</b> for more information.
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   * Returns an ARGB "color" type (a packed 32 bit int with the color.
   * If the coordinate is outside the image, zero is returned
   * (black, but completely transparent).
   * <P>
   * If the image is in RGB format (i.e. on a PVideo object),
   * the value will get its high bits set, just to avoid cases where
   * they haven't been set already.
   * <P>
   * If the image is in ALPHA format, this returns a white with its
   * alpha value set.
   * <P>
   * This function is included primarily for beginners. It is quite
   * slow because it has to check to see if the x, y that was provided
   * is inside the bounds, and then has to check to see what image
   * type it is. If you want things to be more efficient, access the
   * pixels[] array directly.
   *
   * @webref image:pixels
   * @brief Reads the color of any pixel or grabs a rectangle of pixels
   * @usage web_application
   * @param x x-coordinate of the pixel
   * @param y y-coordinate of the pixel
   * @see PApplet#set(int, int, int)
   * @see PApplet#pixels
   * @see PApplet#copy(PImage, int, int, int, int, int, int, int, int)
   */
  public int get(int x, int y) {
    if ((x < 0) || (y < 0) || (x >= pixelWidth) || (y >= pixelHeight)) return 0;

    switch (format) {
      case RGB:
        return pixels[y*pixelWidth + x] | 0xff000000;

      case ARGB:
        return pixels[y*pixelWidth + x];

      case ALPHA:
        return (pixels[y*pixelWidth + x] << 24) | 0xffffff;
    }
    return 0;
  }


  /**
   * @param w width of pixel rectangle to get
   * @param h height of pixel rectangle to get
   */
  public PImage get(int x, int y, int w, int h) {
    int targetX = 0;
    int targetY = 0;
    int targetWidth = w;
    int targetHeight = h;
    boolean cropped = false;

    if (x < 0) {
      w += x; // x is negative, removes the left edge from the width
      targetX = -x;
      cropped = true;
      x = 0;
    }
    if (y < 0) {
      h += y; // y is negative, clip the number of rows
      targetY = -y;
      cropped = true;
      y = 0;
    }

    if (x + w > pixelWidth) {
      w = pixelWidth - x;
      cropped = true;
    }
    if (y + h > pixelHeight) {
      h = pixelHeight - y;
      cropped = true;
    }

    if (w < 0) {
      w = 0;
    }
    if (h < 0) {
      h = 0;
    }

    int targetFormat = format;
    if (cropped && format == RGB) {
      targetFormat = ARGB;
    }

    PImage target = new PImage(targetWidth / pixelDensity,
                               targetHeight / pixelDensity,
                               targetFormat, pixelDensity);
    target.parent = parent;  // parent may be null so can't use createImage()
    if (w > 0 && h > 0) {
      getImpl(x, y, w, h, target, targetX, targetY);
    }
    return target;
  }


  /**
   * Returns a copy of this PImage. Equivalent to get(0, 0, width, height).
   * Deprecated, just use copy() instead.
   */
  public PImage get() {
    // Formerly this used clone(), which caused memory problems.
    // http://code.google.com/p/processing/issues/detail?id=42
    return get(0, 0, pixelWidth, pixelHeight);
  }


  public PImage copy() {
    return get(0, 0, pixelWidth, pixelHeight);
  }


  /**
   * Internal function to actually handle getting a block of pixels that
   * has already been properly cropped to a valid region. That is, x/y/w/h
   * are guaranteed to be inside the image space, so the implementation can
   * use the fastest possible pixel copying method.
   */
  protected void getImpl(int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         PImage target, int targetX, int targetY) {
    int sourceIndex = sourceY*pixelWidth + sourceX;
    int targetIndex = targetY*target.pixelWidth + targetX;
    for (int row = 0; row < sourceHeight; row++) {
      System.arraycopy(pixels, sourceIndex, target.pixels, targetIndex, sourceWidth);
      sourceIndex += pixelWidth;
      targetIndex += target.pixelWidth;
    }
  }


  /**
   * ( begin auto-generated from PImage_set.xml )
   *
   * Changes the color of any pixel or writes an image directly into the
   * display window.<br />
   * <br />
   * The <b>x</b> and <b>y</b> parameters specify the pixel to change and the
   * <b>color</b> parameter specifies the color value. The color parameter is
   * affected by the current color mode (the default is RGB values from 0 to
   * 255). When setting an image, the <b>x</b> and <b>y</b> parameters define
   * the coordinates for the upper-left corner of the image, regardless of
   * the current <b>imageMode()</b>.
   * <br /><br />
   * Setting the color of a single pixel with <b>set(x, y)</b> is easy, but
   * not as fast as putting the data directly into <b>pixels[]</b>. The
   * equivalent statement to <b>set(x, y, #000000)</b> using <b>pixels[]</b>
   * is <b>pixels[y*width+x] = #000000</b>. See the reference for
   * <b>pixels[]</b> for more information.
   *
   * ( end auto-generated )
   *
   * @webref image:pixels
   * @brief writes a color to any pixel or writes an image into another
   * @usage web_application
   * @param x x-coordinate of the pixel
   * @param y y-coordinate of the pixel
   * @param c any value of the color datatype
   * @see PImage#get(int, int, int, int)
   * @see PImage#pixels
   * @see PImage#copy(PImage, int, int, int, int, int, int, int, int)
   */
  public void set(int x, int y, int c) {
    if ((x < 0) || (y < 0) || (x >= pixelWidth) || (y >= pixelHeight)) return;
    pixels[y*pixelWidth + x] = c;
    updatePixels(x, y, 1, 1);  // slow...
  }


  /**
   * <h3>Advanced</h3>
   * Efficient method of drawing an image's pixels directly to this surface.
   * No variations are employed, meaning that any scale, tint, or imageMode
   * settings will be ignored.
   *
   * @param img image to copy into the original image
   */
  public void set(int x, int y, PImage img) {
    int sx = 0;
    int sy = 0;
    int sw = img.pixelWidth;
    int sh = img.pixelHeight;

    if (x < 0) {  // off left edge
      sx -= x;
      sw += x;
      x = 0;
    }
    if (y < 0) {  // off top edge
      sy -= y;
      sh += y;
      y = 0;
    }
    if (x + sw > pixelWidth) {  // off right edge
      sw = pixelWidth - x;
    }
    if (y + sh > pixelHeight) {  // off bottom edge
      sh = pixelHeight - y;
    }

    // this could be nonexistent
    if ((sw <= 0) || (sh <= 0)) return;

    setImpl(img, sx, sy, sw, sh, x, y);
  }


  /**
   * Internal function to actually handle setting a block of pixels that
   * has already been properly cropped from the image to a valid region.
   */
  protected void setImpl(PImage sourceImage,
                         int sourceX, int sourceY,
                         int sourceWidth, int sourceHeight,
                         int targetX, int targetY) {
    int sourceOffset = sourceY * sourceImage.pixelWidth + sourceX;
    int targetOffset = targetY * pixelWidth + targetX;

    for (int y = sourceY; y < sourceY + sourceHeight; y++) {
      System.arraycopy(sourceImage.pixels, sourceOffset, pixels, targetOffset, sourceWidth);
      sourceOffset += sourceImage.pixelWidth;
      targetOffset += pixelWidth;
    }

    //updatePixelsImpl(targetX, targetY, sourceWidth, sourceHeight);
    updatePixels(targetX, targetY, sourceWidth, sourceHeight);
  }



  //////////////////////////////////////////////////////////////

  // ALPHA CHANNEL


  /**
   * @param maskArray array of integers used as the alpha channel, needs to be
   * the same length as the image's pixel array.
   */
  public void mask(int maskArray[]) {  // ignore
    loadPixels();
    // don't execute if mask image is different size
    if (maskArray.length != pixels.length) {
      throw new IllegalArgumentException("mask() can only be used with an image that's the same size.");
    }
    for (int i = 0; i < pixels.length; i++) {
      pixels[i] = ((maskArray[i] & 0xff) << 24) | (pixels[i] & 0xffffff);
    }
    format = ARGB;
    updatePixels();
  }


  /**
   * ( begin auto-generated from PImage_mask.xml )
   *
   * Masks part of an image from displaying by loading another image and
   * using it as an alpha channel. This mask image should only contain
   * grayscale data, but only the blue color channel is used. The mask image
   * needs to be the same size as the image to which it is applied.<br />
   * <br />
   * In addition to using a mask image, an integer array containing the alpha
   * channel data can be specified directly. This method is useful for
   * creating dynamically generated alpha masks. This array must be of the
   * same length as the target image's pixels array and should contain only
   * grayscale data of values between 0-255.
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   *
   * Set alpha channel for an image. Black colors in the source
   * image will make the destination image completely transparent,
   * and white will make things fully opaque. Gray values will
   * be in-between steps.
   * <P>
   * Strictly speaking the "blue" value from the source image is
   * used as the alpha color. For a fully grayscale image, this
   * is correct, but for a color image it's not 100% accurate.
   * For a more accurate conversion, first use filter(GRAY)
   * which will make the image into a "correct" grayscale by
   * performing a proper luminance-based conversion.
   *
   * @webref pimage:method
   * @usage web_application
   * @param img image to use as the mask
   * @brief Masks part of an image with another image as an alpha channel
   */
  public void mask(PImage img) {
    img.loadPixels();
    mask(img.pixels);
  }



  //////////////////////////////////////////////////////////////

  // IMAGE FILTERS


  public void filter(int kind) {
    loadPixels();

    switch (kind) {
      case BLUR:
        // TODO write basic low-pass filter blur here
        // what does photoshop do on the edges with this guy?
        // better yet.. why bother? just use gaussian with radius 1
        filter(BLUR, 1);
        break;

      case GRAY:
        if (format == ALPHA) {
          // for an alpha image, convert it to an opaque grayscale
          for (int i = 0; i < pixels.length; i++) {
            int col = 255 - pixels[i];
            pixels[i] = 0xff000000 | (col << 16) | (col << 8) | col;
          }
          format = RGB;

        } else {
          // Converts RGB image data into grayscale using
          // weighted RGB components, and keeps alpha channel intact.
          // [toxi 040115]
          for (int i = 0; i < pixels.length; i++) {
            int col = pixels[i];
            // luminance = 0.3*red + 0.59*green + 0.11*blue
            // 0.30 * 256 =  77
            // 0.59 * 256 = 151
            // 0.11 * 256 =  28
            int lum = (77*(col>>16&0xff) + 151*(col>>8&0xff) + 28*(col&0xff))>>8;
            pixels[i] = (col & ALPHA_MASK) | lum<<16 | lum<<8 | lum;
          }
        }
        break;

      case INVERT:
        for (int i = 0; i < pixels.length; i++) {
          //pixels[i] = 0xff000000 |
          pixels[i] ^= 0xffffff;
        }
        break;

      case POSTERIZE:
        throw new RuntimeException("Use filter(POSTERIZE, int levels) " +
        "instead of filter(POSTERIZE)");

      case OPAQUE:
        for (int i = 0; i < pixels.length; i++) {
          pixels[i] |= 0xff000000;
        }
        format = RGB;
        break;

      case THRESHOLD:
        filter(THRESHOLD, 0.5f);
        break;

        // [toxi 050728] added new filters
      case ERODE:
        erode();  // former dilate(true);
        break;

      case DILATE:
        dilate();  // former dilate(false);
        break;
    }
    updatePixels();  // mark as modified
  }


  /**
   * ( begin auto-generated from PImage_filter.xml )
   *
   * Filters an image as defined by one of the following modes:<br /><br
   * />THRESHOLD - converts the image to black and white pixels depending if
   * they are above or below the threshold defined by the level parameter.
   * The level must be between 0.0 (black) and 1.0(white). If no level is
   * specified, 0.5 is used.<br />
   * <br />
   * GRAY - converts any colors in the image to grayscale equivalents<br />
   * <br />
   * INVERT - sets each pixel to its inverse value<br />
   * <br />
   * POSTERIZE - limits each channel of the image to the number of colors
   * specified as the level parameter<br />
   * <br />
   * BLUR - executes a Guassian blur with the level parameter specifying the
   * extent of the blurring. If no level parameter is used, the blur is
   * equivalent to Guassian blur of radius 1<br />
   * <br />
   * OPAQUE - sets the alpha channel to entirely opaque<br />
   * <br />
   * ERODE - reduces the light areas with the amount defined by the level
   * parameter<br />
   * <br />
   * DILATE - increases the light areas with the amount defined by the level parameter
   *
   * ( end auto-generated )
   *
   * <h3>Advanced</h3>
   * Method to apply a variety of basic filters to this image.
   * <P>
   * <UL>
   * <LI>filter(BLUR) provides a basic blur.
   * <LI>filter(GRAY) converts the image to grayscale based on luminance.
   * <LI>filter(INVERT) will invert the color components in the image.
   * <LI>filter(OPAQUE) set all the high bits in the image to opaque
   * <LI>filter(THRESHOLD) converts the image to black and white.
   * <LI>filter(DILATE) grow white/light areas
   * <LI>filter(ERODE) shrink white/light areas
   * </UL>
   * Luminance conversion code contributed by
   * <A HREF="http://www.toxi.co.uk">toxi</A>
   * <P/>
   * Gaussian blur code contributed by
   * <A HREF="http://incubator.quasimondo.com">Mario Klingemann</A>
   *
   * @webref image:pixels
   * @brief Converts the image to grayscale or black and white
   * @usage web_application
   * @param kind Either THRESHOLD, GRAY, OPAQUE, INVERT, POSTERIZE, BLUR, ERODE, or DILATE
   * @param param unique for each, see above
   */
  public void filter(int kind, float param) {
    loadPixels();

    switch (kind) {
      case BLUR:
        if (format == ALPHA)
          blurAlpha(param);
        else if (format == ARGB)
          blurARGB(param);
        else
          blurRGB(param);
        break;

      case GRAY:
        throw new RuntimeException("Use filter(GRAY) instead of " +
                                   "filter(GRAY, param)");

      case INVERT:
        throw new RuntimeException("Use filter(INVERT) instead of " +
                                   "filter(INVERT, param)");

      case OPAQUE:
        throw new RuntimeException("Use filter(OPAQUE) instead of " +
                                   "filter(OPAQUE, param)");

      case POSTERIZE:
        int levels = (int)param;
        if ((levels < 2) || (levels > 255)) {
          throw new RuntimeException("Levels must be between 2 and 255 for " +
                                     "filter(POSTERIZE, levels)");
        }
        int levels1 = levels - 1;
        for (int i = 0; i < pixels.length; i++) {
          int rlevel = (pixels[i] >> 16) & 0xff;
          int glevel = (pixels[i] >> 8) & 0xff;
          int blevel = pixels[i] & 0xff;
          rlevel = (((rlevel * levels) >> 8) * 255) / levels1;
          glevel = (((glevel * levels) >> 8) * 255) / levels1;
          blevel = (((blevel * levels) >> 8) * 255) / levels1;
          pixels[i] = ((0xff000000 & pixels[i]) |
                       (rlevel << 16) |
                       (glevel << 8) |
                       blevel);
        }
        break;

      case THRESHOLD:  // greater than or equal to the threshold
        int thresh = (int) (param * 255);
        for (int i = 0; i < pixels.length; i++) {
          int max = Math.max((pixels[i] & RED_MASK) >> 16,
                             Math.max((pixels[i] & GREEN_MASK) >> 8,
                                      (pixels[i] & BLUE_MASK)));
          pixels[i] = (pixels[i] & ALPHA_MASK) |
            ((max < thresh) ? 0x000000 : 0xffffff);
        }
        break;

        // [toxi20050728] added new filters
        case ERODE:
          throw new RuntimeException("Use filter(ERODE) instead of " +
                                     "filter(ERODE, param)");
        case DILATE:
          throw new RuntimeException("Use filter(DILATE) instead of " +
                                     "filter(DILATE, param)");
    }
    updatePixels();  // mark as modified
  }


  /** Set the high bits of all pixels to opaque. */
  protected void opaque() {
    for (int i = 0; i < pixels.length; i++) {
      pixels[i] = 0xFF000000 | pixels[i];
    }
  }


  /**
   * Optimized code for building the blur kernel.
   * further optimized blur code (approx. 15% for radius=20)
   * bigger speed gains for larger radii (~30%)
   * added support for various image types (ALPHA, RGB, ARGB)
   * [toxi 050728]
   */
  protected void buildBlurKernel(float r) {
    int radius = (int) (r * 3.5f);
    radius = (radius < 1) ? 1 : ((radius < 248) ? radius : 248);
    if (blurRadius != radius) {
      blurRadius = radius;
      blurKernelSize = 1 + blurRadius<<1;
      blurKernel = new int[blurKernelSize];
      blurMult = new int[blurKernelSize][256];

      int bk,bki;
      int[] bm,bmi;

      for (int i = 1, radiusi = radius - 1; i < radius; i++) {
        blurKernel[radius+i] = blurKernel[radiusi] = bki = radiusi * radiusi;
        bm=blurMult[radius+i];
        bmi=blurMult[radiusi--];
        for (int j = 0; j < 256; j++)
          bm[j] = bmi[j] = bki*j;
      }
      bk = blurKernel[radius] = radius * radius;
      bm = blurMult[radius];
      for (int j = 0; j < 256; j++)
        bm[j] = bk*j;
    }
  }


  protected void blurAlpha(float r) {
    int sum, cb;
    int read, ri, ym, ymi, bk0;
    int b2[] = new int[pixels.length];
    int yi = 0;

    buildBlurKernel(r);

    for (int y = 0; y < pixelHeight; y++) {
      for (int x = 0; x < pixelWidth; x++) {
        //cb = cg = cr = sum = 0;
        cb = sum = 0;
        read = x - blurRadius;
        if (read<0) {
          bk0=-read;
          read=0;
        } else {
          if (read >= pixelWidth)
            break;
          bk0=0;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (read >= pixelWidth)
            break;
          int c = pixels[read + yi];
          int[] bm = blurMult[i];
          cb += bm[c & BLUE_MASK];
          sum += blurKernel[i];
          read++;
        }
        ri = yi + x;
        b2[ri] = cb / sum;
      }
      yi += pixelWidth;
    }

    yi = 0;
    ym = -blurRadius;
    ymi = ym * pixelWidth;

    for (int y = 0; y < pixelHeight; y++) {
      for (int x = 0; x < pixelWidth; x++) {
        cb = sum = 0;
        if (ym < 0) {
          bk0 = ri = -ym;
          read = x;
        } else {
          if (ym >= pixelHeight)
            break;
          bk0 = 0;
          ri = ym;
          read = x + ymi;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (ri >= pixelHeight)
            break;
          int[] bm = blurMult[i];
          cb += bm[b2[read]];
          sum += blurKernel[i];
          ri++;
          read += pixelWidth;
        }
        pixels[x+yi] = (cb/sum);
      }
      yi += pixelWidth;
      ymi += pixelWidth;
      ym++;
    }
  }


  protected void blurRGB(float r) {
    int sum, cr, cg, cb; //, k;
    int /*pixel,*/ read, ri, /*roff,*/ ym, ymi, /*riw,*/ bk0;
    int r2[] = new int[pixels.length];
    int g2[] = new int[pixels.length];
    int b2[] = new int[pixels.length];
    int yi = 0;

    buildBlurKernel(r);

    for (int y = 0; y < pixelHeight; y++) {
      for (int x = 0; x < pixelWidth; x++) {
        cb = cg = cr = sum = 0;
        read = x - blurRadius;
        if (read < 0) {
          bk0 = -read;
          read = 0;
        } else {
          if (read >= pixelWidth) {
            break;
          }
          bk0 = 0;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (read >= pixelWidth) {
            break;
          }
          int c = pixels[read + yi];
          int[] bm = blurMult[i];
          cr += bm[(c & RED_MASK) >> 16];
          cg += bm[(c & GREEN_MASK) >> 8];
          cb += bm[c & BLUE_MASK];
          sum += blurKernel[i];
          read++;
        }
        ri = yi + x;
        r2[ri] = cr / sum;
        g2[ri] = cg / sum;
        b2[ri] = cb / sum;
      }
      yi += pixelWidth;
    }

    yi = 0;
    ym = -blurRadius;
    ymi = ym * pixelWidth;

    for (int y = 0; y < pixelHeight; y++) {
      for (int x = 0; x < pixelWidth; x++) {
        cb = cg = cr = sum = 0;
        if (ym < 0) {
          bk0 = ri = -ym;
          read = x;
        } else {
          if (ym >= pixelHeight) {
            break;
          }
          bk0 = 0;
          ri = ym;
          read = x + ymi;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (ri >= pixelHeight) {
            break;
          }
          int[] bm = blurMult[i];
          cr += bm[r2[read]];
          cg += bm[g2[read]];
          cb += bm[b2[read]];
          sum += blurKernel[i];
          ri++;
          read += pixelWidth;
        }
        pixels[x+yi] = 0xff000000 | (cr/sum)<<16 | (cg/sum)<<8 | (cb/sum);
      }
      yi += pixelWidth;
      ymi += pixelWidth;
      ym++;
    }
  }


  protected void blurARGB(float r) {
    int sum, cr, cg, cb, ca;
    int /*pixel,*/ read, ri, /*roff,*/ ym, ymi, /*riw,*/ bk0;
    int wh = pixels.length;
    int r2[] = new int[wh];
    int g2[] = new int[wh];
    int b2[] = new int[wh];
    int a2[] = new int[wh];
    int yi = 0;

    buildBlurKernel(r);

    for (int y = 0; y < pixelHeight; y++) {
      for (int x = 0; x < pixelWidth; x++) {
        cb = cg = cr = ca = sum = 0;
        read = x - blurRadius;
        if (read < 0) {
          bk0 = -read;
          read = 0;
        } else {
          if (read >= pixelWidth) {
            break;
          }
          bk0=0;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (read >= pixelWidth) {
            break;
          }
          int c = pixels[read + yi];
          int[] bm=blurMult[i];
          ca += bm[(c & ALPHA_MASK) >>> 24];
          cr += bm[(c & RED_MASK) >> 16];
          cg += bm[(c & GREEN_MASK) >> 8];
          cb += bm[c & BLUE_MASK];
          sum += blurKernel[i];
          read++;
        }
        ri = yi + x;
        a2[ri] = ca / sum;
        r2[ri] = cr / sum;
        g2[ri] = cg / sum;
        b2[ri] = cb / sum;
      }
      yi += pixelWidth;
    }

    yi = 0;
    ym = -blurRadius;
    ymi = ym * pixelWidth;

    for (int y = 0; y < pixelHeight; y++) {
      for (int x = 0; x < pixelWidth; x++) {
        cb = cg = cr = ca = sum = 0;
        if (ym < 0) {
          bk0 = ri = -ym;
          read = x;
        } else {
          if (ym >= pixelHeight) {
            break;
          }
          bk0 = 0;
          ri = ym;
          read = x + ymi;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (ri >= pixelHeight) {
            break;
          }
          int[] bm=blurMult[i];
          ca += bm[a2[read]];
          cr += bm[r2[read]];
          cg += bm[g2[read]];
          cb += bm[b2[read]];
          sum += blurKernel[i];
          ri++;
          read += pixelWidth;
        }
        pixels[x+yi] = (ca/sum)<<24 | (cr/sum)<<16 | (cg/sum)<<8 | (cb/sum);
      }
      yi += pixelWidth;
      ymi += pixelWidth;
      ym++;
    }
  }


  /**
   * Generic dilate/erode filter using luminance values
   * as decision factor. [toxi 050728]
   */
  protected void dilate() {  // formerly dilate(false)
    int index = 0;
    int maxIndex = pixels.length;
    int[] outgoing = new int[maxIndex];

    // erosion (grow light areas)
    while (index < maxIndex) {
      int curRowIndex = index;
      int maxRowIndex = index + pixelWidth;
      while (index < maxRowIndex) {
        int orig = pixels[index];
        int result = orig;
        int idxLeft = index - 1;
        int idxRight = index + 1;
        int idxUp = index - pixelWidth;
        int idxDown = index + pixelWidth;
        if (idxLeft < curRowIndex) {
          idxLeft = index;
        }
        if (idxRight >= maxRowIndex) {
          idxRight = index;
        }
        if (idxUp < 0) {
          idxUp = index;
        }
        if (idxDown >= maxIndex) {
          idxDown = index;
        }

        int colUp = pixels[idxUp];
        int colLeft = pixels[idxLeft];
        int colDown = pixels[idxDown];
        int colRight = pixels[idxRight];

        // compute luminance
        int currLum =
          77*(orig>>16&0xff) + 151*(orig>>8&0xff) + 28*(orig&0xff);
        int lumLeft =
          77*(colLeft>>16&0xff) + 151*(colLeft>>8&0xff) + 28*(colLeft&0xff);
        int lumRight =
          77*(colRight>>16&0xff) + 151*(colRight>>8&0xff) + 28*(colRight&0xff);
        int lumUp =
          77*(colUp>>16&0xff) + 151*(colUp>>8&0xff) + 28*(colUp&0xff);
        int lumDown =
          77*(colDown>>16&0xff) + 151*(colDown>>8&0xff) + 28*(colDown&0xff);

        if (lumLeft > currLum) {
          result = colLeft;
          currLum = lumLeft;
        }
        if (lumRight > currLum) {
          result = colRight;
          currLum = lumRight;
        }
        if (lumUp > currLum) {
          result = colUp;
          currLum = lumUp;
        }
        if (lumDown > currLum) {
          result = colDown;
          currLum = lumDown;
        }
        outgoing[index++] = result;
      }
    }
    System.arraycopy(outgoing, 0, pixels, 0, maxIndex);
  }


  protected void erode() {  // formerly dilate(true)
    int index = 0;
    int maxIndex = pixels.length;
    int[] outgoing = new int[maxIndex];

    // dilate (grow dark areas)
    while (index < maxIndex) {
      int curRowIndex = index;
      int maxRowIndex = index + pixelWidth;
      while (index < maxRowIndex) {
        int orig = pixels[index];
        int result = orig;
        int idxLeft = index - 1;
        int idxRight = index + 1;
        int idxUp = index - pixelWidth;
        int idxDown = index + pixelWidth;
        if (idxLeft < curRowIndex) {
          idxLeft = index;
        }
        if (idxRight >= maxRowIndex) {
          idxRight = index;
        }
        if (idxUp < 0) {
          idxUp = index;
        }
        if (idxDown >= maxIndex) {
          idxDown = index;
        }

        int colUp = pixels[idxUp];
        int colLeft = pixels[idxLeft];
        int colDown = pixels[idxDown];
        int colRight = pixels[idxRight];

        // compute luminance
        int currLum =
          77*(orig>>16&0xff) + 151*(orig>>8&0xff) + 28*(orig&0xff);
        int lumLeft =
          77*(colLeft>>16&0xff) + 151*(colLeft>>8&0xff) + 28*(colLeft&0xff);
        int lumRight =
          77*(colRight>>16&0xff) + 151*(colRight>>8&0xff) + 28*(colRight&0xff);
        int lumUp =
          77*(colUp>>16&0xff) + 151*(colUp>>8&0xff) + 28*(colUp&0xff);
        int lumDown =
          77*(colDown>>16&0xff) + 151*(colDown>>8&0xff) + 28*(colDown&0xff);

        if (lumLeft < currLum) {
          result = colLeft;
          currLum = lumLeft;
        }
        if (lumRight < currLum) {
          result = colRight;
          currLum = lumRight;
        }
        if (lumUp < currLum) {
          result = colUp;
          currLum = lumUp;
        }
        if (lumDown < currLum) {
          result = colDown;
          currLum = lumDown;
        }
        outgoing[index++] = result;
      }
    }
    System.arraycopy(outgoing, 0, pixels, 0, maxIndex);
  }



  //////////////////////////////////////////////////////////////

  // COPY


  /**
   * ( begin auto-generated from PImage_copy.xml )
   *
   * Copies a region of pixels from one image into another. If the source and
   * destination regions aren't the same size, it will automatically resize
   * source pixels to fit the specified target region. No alpha information
   * is used in the process, however if the source image has an alpha channel
   * set, it will be copied as well.
   * <br /><br />
   * As of release 0149, this function ignores <b>imageMode()</b>.
   *
   * ( end auto-generated )
   *
   * @webref image:pixels
   * @brief Copies the entire image
   * @usage web_application
   * @param sx X coordinate of the source's upper left corner
   * @param sy Y coordinate of the source's upper left corner
   * @param sw source image width
   * @param sh source image height
   * @param dx X coordinate of the destination's upper left corner
   * @param dy Y coordinate of the destination's upper left corner
   * @param dw destination image width
   * @param dh destination image height
   * @see PGraphics#alpha(int)
   * @see PImage#blend(PImage, int, int, int, int, int, int, int, int, int)
   */
  public void copy(int sx, int sy, int sw, int sh,
                   int dx, int dy, int dw, int dh) {
    blend(this, sx, sy, sw, sh, dx, dy, dw, dh, REPLACE);
  }


/**
 * @param src an image variable referring to the source image.
 */
  public void copy(PImage src,
                   int sx, int sy, int sw, int sh,
                   int dx, int dy, int dw, int dh) {
    blend(src, sx, sy, sw, sh, dx, dy, dw, dh, REPLACE);
  }



  //////////////////////////////////////////////////////////////

  // BLEND


  /**
   * ( begin auto-generated from blendColor.xml )
   *
   * Blends two color values together based on the blending mode given as the
   * <b>MODE</b> parameter. The possible modes are described in the reference
   * for the <b>blend()</b> function.
   *
   * ( end auto-generated )
   * <h3>Advanced</h3>
   * <UL>
   * <LI>REPLACE - destination colour equals colour of source pixel: C = A.
   *     Sometimes called "Normal" or "Copy" in other software.
   *
   * <LI>BLEND - linear interpolation of colours:
   *     <TT>C = A*factor + B</TT>
   *
   * <LI>ADD - additive blending with white clip:
   *     <TT>C = min(A*factor + B, 255)</TT>.
   *     Clipped to 0..255, Photoshop calls this "Linear Burn",
   *     and Director calls it "Add Pin".
   *
   * <LI>SUBTRACT - substractive blend with black clip:
   *     <TT>C = max(B - A*factor, 0)</TT>.
   *     Clipped to 0..255, Photoshop calls this "Linear Dodge",
   *     and Director calls it "Subtract Pin".
   *
   * <LI>DARKEST - only the darkest colour succeeds:
   *     <TT>C = min(A*factor, B)</TT>.
   *     Illustrator calls this "Darken".
   *
   * <LI>LIGHTEST - only the lightest colour succeeds:
   *     <TT>C = max(A*factor, B)</TT>.
   *     Illustrator calls this "Lighten".
   *
   * <LI>DIFFERENCE - subtract colors from underlying image.
   *
   * <LI>EXCLUSION - similar to DIFFERENCE, but less extreme.
   *
   * <LI>MULTIPLY - Multiply the colors, result will always be darker.
   *
   * <LI>SCREEN - Opposite multiply, uses inverse values of the colors.
   *
   * <LI>OVERLAY - A mix of MULTIPLY and SCREEN. Multiplies dark values,
   *     and screens light values.
   *
   * <LI>HARD_LIGHT - SCREEN when greater than 50% gray, MULTIPLY when lower.
   *
   * <LI>SOFT_LIGHT - Mix of DARKEST and LIGHTEST.
   *     Works like OVERLAY, but not as harsh.
   *
   * <LI>DODGE - Lightens light tones and increases contrast, ignores darks.
   *     Called "Color Dodge" in Illustrator and Photoshop.
   *
   * <LI>BURN - Darker areas are applied, increasing contrast, ignores lights.
   *     Called "Color Burn" in Illustrator and Photoshop.
   * </UL>
   * <P>A useful reference for blending modes and their algorithms can be
   * found in the <A HREF="http://www.w3.org/TR/SVG12/rendering.html">SVG</A>
   * specification.</P>
   * <P>It is important to note that Processing uses "fast" code, not
   * necessarily "correct" code. No biggie, most software does. A nitpicker
   * can find numerous "off by 1 division" problems in the blend code where
   * <TT>&gt;&gt;8</TT> or <TT>&gt;&gt;7</TT> is used when strictly speaking
   * <TT>/255.0</T> or <TT>/127.0</TT> should have been used.</P>
   * <P>For instance, exclusion (not intended for real-time use) reads
   * <TT>r1 + r2 - ((2 * r1 * r2) / 255)</TT> because <TT>255 == 1.0</TT>
   * not <TT>256 == 1.0</TT>. In other words, <TT>(255*255)>>8</TT> is not
   * the same as <TT>(255*255)/255</TT>. But for real-time use the shifts
   * are preferrable, and the difference is insignificant for applications
   * built with Processing.</P>
   *
   * @webref color:creating_reading
   * @usage web_application
   * @param c1 the first color to blend
   * @param c2 the second color to blend
   * @param mode either BLEND, ADD, SUBTRACT, DARKEST, LIGHTEST, DIFFERENCE, EXCLUSION, MULTIPLY, SCREEN, OVERLAY, HARD_LIGHT, SOFT_LIGHT, DODGE, or BURN
   * @see PImage#blend(PImage, int, int, int, int, int, int, int, int, int)
   * @see PApplet#color(float, float, float, float)
   */
  static public int blendColor(int c1, int c2, int mode) {  // ignore
    switch (mode) {
    case REPLACE:    return c2;
    case BLEND:      return blend_blend(c1, c2);

    case ADD:        return blend_add_pin(c1, c2);
    case SUBTRACT:   return blend_sub_pin(c1, c2);

    case LIGHTEST:   return blend_lightest(c1, c2);
    case DARKEST:    return blend_darkest(c1, c2);

    case DIFFERENCE: return blend_difference(c1, c2);
    case EXCLUSION:  return blend_exclusion(c1, c2);

    case MULTIPLY:   return blend_multiply(c1, c2);
    case SCREEN:     return blend_screen(c1, c2);

    case HARD_LIGHT: return blend_hard_light(c1, c2);
    case SOFT_LIGHT: return blend_soft_light(c1, c2);
    case OVERLAY:    return blend_overlay(c1, c2);

    case DODGE:      return blend_dodge(c1, c2);
    case BURN:       return blend_burn(c1, c2);
    }
    return 0;
  }


  public void blend(int sx, int sy, int sw, int sh,
                    int dx, int dy, int dw, int dh, int mode) {
    blend(this, sx, sy, sw, sh, dx, dy, dw, dh, mode);
  }


  /**
   * ( begin auto-generated from PImage_blend.xml )
   *
   * Blends a region of pixels into the image specified by the <b>img</b>
   * parameter. These copies utilize full alpha channel support and a choice
   * of the following modes to blend the colors of source pixels (A) with the
   * ones of pixels in the destination image (B):<br />
   * <br />
   * BLEND - linear interpolation of colours: C = A*factor + B<br />
   * <br />
   * ADD - additive blending with white clip: C = min(A*factor + B, 255)<br />
   * <br />
   * SUBTRACT - subtractive blending with black clip: C = max(B - A*factor,
   * 0)<br />
   * <br />
   * DARKEST - only the darkest colour succeeds: C = min(A*factor, B)<br />
   * <br />
   * LIGHTEST - only the lightest colour succeeds: C = max(A*factor, B)<br />
   * <br />
   * DIFFERENCE - subtract colors from underlying image.<br />
   * <br />
   * EXCLUSION - similar to DIFFERENCE, but less extreme.<br />
   * <br />
   * MULTIPLY - Multiply the colors, result will always be darker.<br />
   * <br />
   * SCREEN - Opposite multiply, uses inverse values of the colors.<br />
   * <br />
   * OVERLAY - A mix of MULTIPLY and SCREEN. Multiplies dark values,
   * and screens light values.<br />
   * <br />
   * HARD_LIGHT - SCREEN when greater than 50% gray, MULTIPLY when lower.<br />
   * <br />
   * SOFT_LIGHT - Mix of DARKEST and LIGHTEST.
   * Works like OVERLAY, but not as harsh.<br />
   * <br />
   * DODGE - Lightens light tones and increases contrast, ignores darks.
   * Called "Color Dodge" in Illustrator and Photoshop.<br />
   * <br />
   * BURN - Darker areas are applied, increasing contrast, ignores lights.
   * Called "Color Burn" in Illustrator and Photoshop.<br />
   * <br />
   * All modes use the alpha information (highest byte) of source image
   * pixels as the blending factor. If the source and destination regions are
   * different sizes, the image will be automatically resized to match the
   * destination size. If the <b>srcImg</b> parameter is not used, the
   * display window is used as the source image.<br />
   * <br />
   * As of release 0149, this function ignores <b>imageMode()</b>.
   *
   * ( end auto-generated )
   *
   * @webref image:pixels
   * @brief  Copies a pixel or rectangle of pixels using different blending modes
   * @param src an image variable referring to the source image
   * @param sx X coordinate of the source's upper left corner
   * @param sy Y coordinate of the source's upper left corner
   * @param sw source image width
   * @param sh source image height
   * @param dx X coordinate of the destinations's upper left corner
   * @param dy Y coordinate of the destinations's upper left corner
   * @param dw destination image width
   * @param dh destination image height
   * @param mode Either BLEND, ADD, SUBTRACT, LIGHTEST, DARKEST, DIFFERENCE, EXCLUSION, MULTIPLY, SCREEN, OVERLAY, HARD_LIGHT, SOFT_LIGHT, DODGE, BURN
   *
   * @see PApplet#alpha(int)
   * @see PImage#copy(PImage, int, int, int, int, int, int, int, int)
   * @see PImage#blendColor(int,int,int)
   */
  public void blend(PImage src,
                    int sx, int sy, int sw, int sh,
                    int dx, int dy, int dw, int dh, int mode) {
    int sx2 = sx + sw;
    int sy2 = sy + sh;
    int dx2 = dx + dw;
    int dy2 = dy + dh;

    loadPixels();
    if (src == this) {
      if (intersect(sx, sy, sx2, sy2, dx, dy, dx2, dy2)) {
        blit_resize(get(sx, sy, sw, sh),
                    0, 0, sw, sh,
                    pixels, pixelWidth, pixelHeight, dx, dy, dx2, dy2, mode);
      } else {
        // same as below, except skip the loadPixels() because it'd be redundant
        blit_resize(src, sx, sy, sx2, sy2,
                    pixels, pixelWidth, pixelHeight, dx, dy, dx2, dy2, mode);
      }
    } else {
      src.loadPixels();
      blit_resize(src, sx, sy, sx2, sy2,
                  pixels, pixelWidth, pixelHeight, dx, dy, dx2, dy2, mode);
      //src.updatePixels();
    }
    updatePixels();
  }


  /**
   * Check to see if two rectangles intersect one another
   */
  private boolean intersect(int sx1, int sy1, int sx2, int sy2,
                            int dx1, int dy1, int dx2, int dy2) {
    int sw = sx2 - sx1 + 1;
    int sh = sy2 - sy1 + 1;
    int dw = dx2 - dx1 + 1;
    int dh = dy2 - dy1 + 1;

    if (dx1 < sx1) {
      dw += dx1 - sx1;
      if (dw > sw) {
        dw = sw;
      }
    } else {
      int w = sw + sx1 - dx1;
      if (dw > w) {
        dw = w;
      }
    }
    if (dy1 < sy1) {
      dh += dy1 - sy1;
      if (dh > sh) {
        dh = sh;
      }
    } else {
      int h = sh + sy1 - dy1;
      if (dh > h) {
        dh = h;
      }
    }
    return !(dw <= 0 || dh <= 0);
  }


  //////////////////////////////////////////////////////////////


  /**
   * Internal blitter/resizer/copier from toxi.
   * Uses bilinear filtering if smooth() has been enabled
   * 'mode' determines the blending mode used in the process.
   */
  private void blit_resize(PImage img,
                           int srcX1, int srcY1, int srcX2, int srcY2,
                           int[] destPixels, int screenW, int screenH,
                           int destX1, int destY1, int destX2, int destY2,
                           int mode) {
    if (srcX1 < 0) srcX1 = 0;
    if (srcY1 < 0) srcY1 = 0;
    if (srcX2 > img.pixelWidth) srcX2 = img.pixelWidth;
    if (srcY2 > img.pixelHeight) srcY2 = img.pixelHeight;

    int srcW = srcX2 - srcX1;
    int srcH = srcY2 - srcY1;
    int destW = destX2 - destX1;
    int destH = destY2 - destY1;

    boolean smooth = true;  // may as well go with the smoothing these days

    if (!smooth) {
      srcW++; srcH++;
    }

    if (destW <= 0 || destH <= 0 ||
        srcW <= 0 || srcH <= 0 ||
        destX1 >= screenW || destY1 >= screenH ||
        srcX1 >= img.pixelWidth || srcY1 >= img.pixelHeight) {
      return;
    }

    int dx = (int) (srcW / (float) destW * PRECISIONF);
    int dy = (int) (srcH / (float) destH * PRECISIONF);

    srcXOffset = destX1 < 0 ? -destX1 * dx : srcX1 * PRECISIONF;
    srcYOffset = destY1 < 0 ? -destY1 * dy : srcY1 * PRECISIONF;

    if (destX1 < 0) {
      destW += destX1;
      destX1 = 0;
    }
    if (destY1 < 0) {
      destH += destY1;
      destY1 = 0;
    }

    destW = min(destW, screenW - destX1);
    destH = min(destH, screenH - destY1);

    int destOffset = destY1 * screenW + destX1;
    srcBuffer = img.pixels;

    if (smooth) {
      // use bilinear filtering
      iw = img.pixelWidth;
      iw1 = img.pixelWidth - 1;
      ih1 = img.pixelHeight - 1;

      switch (mode) {

      case BLEND:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            // davbol  - renamed old blend_multiply to blend_blend
            destPixels[destOffset + x] =
              blend_blend(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case ADD:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_add_pin(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case SUBTRACT:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_sub_pin(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case LIGHTEST:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_lightest(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case DARKEST:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_darkest(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case REPLACE:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] = filter_bilinear();
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case DIFFERENCE:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_difference(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case EXCLUSION:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_exclusion(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case MULTIPLY:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_multiply(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case SCREEN:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_screen(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case OVERLAY:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_overlay(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case HARD_LIGHT:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_hard_light(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case SOFT_LIGHT:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_soft_light(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      // davbol - proposed 2007-01-09
      case DODGE:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_dodge(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case BURN:
        for (int y = 0; y < destH; y++) {
          filter_new_scanline();
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_burn(destPixels[destOffset + x], filter_bilinear());
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      }

    } else {
      // nearest neighbour scaling (++fast!)
      switch (mode) {

      case BLEND:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            // davbol - renamed old blend_multiply to blend_blend
            destPixels[destOffset + x] =
              blend_blend(destPixels[destOffset + x],
                          srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case ADD:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_add_pin(destPixels[destOffset + x],
                            srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case SUBTRACT:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_sub_pin(destPixels[destOffset + x],
                            srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case LIGHTEST:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_lightest(destPixels[destOffset + x],
                             srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case DARKEST:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_darkest(destPixels[destOffset + x],
                            srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case REPLACE:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] = srcBuffer[sY + (sX >> PRECISIONB)];
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case DIFFERENCE:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_difference(destPixels[destOffset + x],
                               srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case EXCLUSION:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_exclusion(destPixels[destOffset + x],
                              srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case MULTIPLY:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_multiply(destPixels[destOffset + x],
                            srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case SCREEN:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_screen(destPixels[destOffset + x],
                            srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case OVERLAY:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_overlay(destPixels[destOffset + x],
                            srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case HARD_LIGHT:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_hard_light(destPixels[destOffset + x],
                            srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case SOFT_LIGHT:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_soft_light(destPixels[destOffset + x],
                            srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      // davbol - proposed 2007-01-09
      case DODGE:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_dodge(destPixels[destOffset + x],
                            srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      case BURN:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.pixelWidth;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] =
              blend_burn(destPixels[destOffset + x],
                            srcBuffer[sY + (sX >> PRECISIONB)]);
            sX += dx;
          }
          destOffset += screenW;
          srcYOffset += dy;
        }
        break;

      }
    }
  }


  private void filter_new_scanline() {
    sX = srcXOffset;
    fracV = srcYOffset & PREC_MAXVAL;
    ifV = PREC_MAXVAL - fracV + 1;
    v1 = (srcYOffset >> PRECISIONB) * iw;
    v2 = min((srcYOffset >> PRECISIONB) + 1, ih1) * iw;
  }


  private int filter_bilinear() {
    fracU = sX & PREC_MAXVAL;
    ifU = PREC_MAXVAL - fracU + 1;
    ul = (ifU * ifV) >> PRECISIONB;
    ll = ifU - ul;
    ur = ifV - ul;
    lr = PREC_MAXVAL + 1 - ul - ll - ur;
    u1 = (sX >> PRECISIONB);
    u2 = min(u1 + 1, iw1);

    // get color values of the 4 neighbouring texels
    cUL = srcBuffer[v1 + u1];
    cUR = srcBuffer[v1 + u2];
    cLL = srcBuffer[v2 + u1];
    cLR = srcBuffer[v2 + u2];

    r = ((ul*((cUL&RED_MASK)>>16) + ll*((cLL&RED_MASK)>>16) +
          ur*((cUR&RED_MASK)>>16) + lr*((cLR&RED_MASK)>>16))
         << PREC_RED_SHIFT) & RED_MASK;

    g = ((ul*(cUL&GREEN_MASK) + ll*(cLL&GREEN_MASK) +
          ur*(cUR&GREEN_MASK) + lr*(cLR&GREEN_MASK))
         >>> PRECISIONB) & GREEN_MASK;

    b = (ul*(cUL&BLUE_MASK) + ll*(cLL&BLUE_MASK) +
         ur*(cUR&BLUE_MASK) + lr*(cLR&BLUE_MASK))
           >>> PRECISIONB;

    a = ((ul*((cUL&ALPHA_MASK)>>>24) + ll*((cLL&ALPHA_MASK)>>>24) +
          ur*((cUR&ALPHA_MASK)>>>24) + lr*((cLR&ALPHA_MASK)>>>24))
         << PREC_ALPHA_SHIFT) & ALPHA_MASK;

    return a | r | g | b;
  }



  //////////////////////////////////////////////////////////////

  // internal blending methods


  private static int min(int a, int b) {
    return (a < b) ? a : b;
  }


  private static int max(int a, int b) {
    return (a > b) ? a : b;
  }


  /////////////////////////////////////////////////////////////

  // BLEND MODE IMPLEMENTATIONS

  /*
   * Jakub Valtar
   *
   * All modes use SRC alpha to interpolate between DST and the result of
   * the operation:
   *
   * R = (1 - SRC_ALPHA) * DST + SRC_ALPHA * <RESULT OF THE OPERATION>
   *
   * Comments above each mode only specify the formula of its operation.
   *
   * These implementations treat alpha 127 (=255/2) as a perfect 50 % mix.
   *
   * One alpha value between 126 and 127 is intentionally left out,
   * so the step 126 -> 127 is twice as big compared to other steps.
   * This is because our colors are in 0..255 range, but we divide
   * by right shifting 8 places (=256) which is much faster than
   * (correct) float division by 255.0f. The missing value was placed
   * between 126 and 127, because limits of the range (near 0 and 255) and
   * the middle value (127) have to blend correctly.
   *
   * Below you will often see RED and BLUE channels (RB) manipulated together
   * and GREEN channel (GN) manipulated separately. It is sometimes possible
   * because the operation won't use more than 16 bits, so we process the RED
   * channel in the upper 16 bits and BLUE channel in the lower 16 bits. This
   * decreases the number of operations per pixel and thus makes things faster.
   *
   * Some of the modes are hand tweaked (various +1s etc.) to be more accurate
   * and to produce correct values in extremes. Below is a sketch you can use
   * to check any blending function for
   *
   * 1) Discrepancies between color channels:
   *    - highlighted by the offending color
   * 2) Behavior at extremes (set colorCount to 256):
   *    - values of all corners are printed to the console
   * 3) Rounding errors:
   *    - set colorCount to lower value to better see color bands
   *

// use powers of 2 in range 2..256
// to better see color bands
final int colorCount = 256;

final int blockSize = 3;

void settings() {
  size(blockSize * 256, blockSize * 256);
}

void setup() { }

void draw() {
  noStroke();
  colorMode(RGB, colorCount-1);
  int alpha = (mouseX / blockSize) << 24;
  int r, g, b, r2, g2, b2 = 0;
  for (int x = 0; x <= 0xFF; x++) {
    for (int y = 0; y <= 0xFF; y++) {
      int dst = (x << 16) | (x << 8) | x;
      int src = (y << 16) | (y << 8) | y | alpha;
      int result = testFunction(dst, src);
      r = r2 = (result >> 16 & 0xFF);
      g = g2 = (result >>  8 & 0xFF);
      b = b2 = (result >>  0 & 0xFF);
      if (r != g && r != b) r2 = (128 + r2) % 255;
      if (g != r && g != b) g2 = (128 + g2) % 255;
      if (b != r && b != g) b2 = (128 + b2) % 255;
      fill(r2 % colorCount, g2 % colorCount, b2 % colorCount);
      rect(x * blockSize, y * blockSize, blockSize, blockSize);
    }
  }
  println(
    "alpha:", mouseX/blockSize,
    "TL:", hex(get(0, 0)),
    "TR:", hex(get(width-1, 0)),
    "BR:", hex(get(width-1, height-1)),
    "BL:", hex(get(0, height-1)));
}

int testFunction(int dst, int src) {
  // your function here
  return dst;
}

   *
   *
   */

  private static final int RB_MASK = 0x00FF00FF;
  private static final int GN_MASK = 0x0000FF00;

  /**
   * Blend
   * O = S
   */
  private static int blend_blend(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    return min((dst >>> 24) + a, 0xFF) << 24 |
        ((dst & RB_MASK) * d_a + (src & RB_MASK) * s_a) >>> 8 & RB_MASK |
        ((dst & GN_MASK) * d_a + (src & GN_MASK) * s_a) >>> 8 & GN_MASK;
  }


  /**
   * Add
   * O = MIN(D + S, 1)
   */
  private static int blend_add_pin(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);

    int rb = (dst & RB_MASK) + ((src & RB_MASK) * s_a >>> 8 & RB_MASK);
    int gn = (dst & GN_MASK) + ((src & GN_MASK) * s_a >>> 8);

    return min((dst >>> 24) + a, 0xFF) << 24 |
        min(rb & 0xFFFF0000, RED_MASK)   |
        min(gn & 0x00FFFF00, GREEN_MASK) |
        min(rb & 0x0000FFFF, BLUE_MASK);
  }


  /**
   * Subtract
   * O = MAX(0, D - S)
   */
  private static int blend_sub_pin(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);

    int rb = ((src & RB_MASK)    * s_a >>> 8);
    int gn = ((src & GREEN_MASK) * s_a >>> 8);

    return min((dst >>> 24) + a, 0xFF) << 24 |
        max((dst & RED_MASK)   - (rb & RED_MASK), 0) |
        max((dst & GREEN_MASK) - (gn & GREEN_MASK), 0) |
        max((dst & BLUE_MASK)  - (rb & BLUE_MASK), 0);
  }


  /**
   * Lightest
   * O = MAX(D, S)
   */
  private static int blend_lightest(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    int rb = max(src & RED_MASK,   dst & RED_MASK) |
             max(src & BLUE_MASK,  dst & BLUE_MASK);
    int gn = max(src & GREEN_MASK, dst & GREEN_MASK);

    return min((dst >>> 24) + a, 0xFF) << 24 |
        ((dst & RB_MASK) * d_a + rb * s_a) >>> 8 & RB_MASK |
        ((dst & GN_MASK) * d_a + gn * s_a) >>> 8 & GN_MASK;
  }


  /**
   * Darkest
   * O = MIN(D, S)
   */
  private static int blend_darkest(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    int rb = min(src & RED_MASK,   dst & RED_MASK) |
             min(src & BLUE_MASK,  dst & BLUE_MASK);
    int gn = min(src & GREEN_MASK, dst & GREEN_MASK);

    return min((dst >>> 24) + a, 0xFF) << 24 |
        ((dst & RB_MASK) * d_a + rb * s_a) >>> 8 & RB_MASK |
        ((dst & GN_MASK) * d_a + gn * s_a) >>> 8 & GN_MASK;
  }


  /**
   * Difference
   * O = ABS(D - S)
   */
  private static int blend_difference(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    int r = (dst & RED_MASK)   - (src & RED_MASK);
    int b = (dst & BLUE_MASK)  - (src & BLUE_MASK);
    int g = (dst & GREEN_MASK) - (src & GREEN_MASK);

    int rb = (r < 0 ? -r : r) |
             (b < 0 ? -b : b);
    int gn = (g < 0 ? -g : g);

    return min((dst >>> 24) + a, 0xFF) << 24 |
        ((dst & RB_MASK) * d_a + rb * s_a) >>> 8 & RB_MASK |
        ((dst & GN_MASK) * d_a + gn * s_a) >>> 8 & GN_MASK;
  }


  /**
   * Exclusion
   * O = (1 - S)D + S(1 - D)
   * O = D + S - 2DS
   */
  private static int blend_exclusion(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    int d_rb = dst & RB_MASK;
    int d_gn = dst & GN_MASK;

    int s_gn = src & GN_MASK;

    int f_r = (dst & RED_MASK) >> 16;
    int f_b = (dst & BLUE_MASK);

    int rb_sub =
        ((src & RED_MASK) * (f_r + (f_r >= 0x7F ? 1 : 0)) |
        (src & BLUE_MASK) * (f_b + (f_b >= 0x7F ? 1 : 0)))
        >>> 7 & 0x01FF01FF;
    int gn_sub = s_gn * (d_gn + (d_gn >= 0x7F00 ? 0x100 : 0))
        >>> 15 & 0x0001FF00;

    return min((dst >>> 24) + a, 0xFF) << 24 |
        (d_rb * d_a + (d_rb + (src & RB_MASK) - rb_sub) * s_a) >>> 8 & RB_MASK |
        (d_gn * d_a + (d_gn + s_gn            - gn_sub) * s_a) >>> 8 & GN_MASK;
  }


  /*
   * Multiply
   * O = DS
   */
  private static int blend_multiply(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    int d_gn = dst & GN_MASK;

    int f_r = (dst & RED_MASK) >> 16;
    int f_b = (dst & BLUE_MASK);

    int rb =
        ((src & RED_MASK)  * (f_r + 1) |
        (src & BLUE_MASK)  * (f_b + 1))
        >>>  8 & RB_MASK;
    int gn =
        (src & GREEN_MASK) * (d_gn + 0x100)
        >>> 16 & GN_MASK;

    return min((dst >>> 24) + a, 0xFF) << 24 |
        ((dst & RB_MASK) * d_a + rb * s_a) >>> 8 & RB_MASK |
        (d_gn            * d_a + gn * s_a) >>> 8 & GN_MASK;
  }


  /**
   * Screen
   * O = 1 - (1 - D)(1 - S)
   * O = D + S - DS
   */
  private static int blend_screen(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    int d_rb = dst & RB_MASK;
    int d_gn = dst & GN_MASK;

    int s_gn = src & GN_MASK;

    int f_r = (dst & RED_MASK) >> 16;
    int f_b = (dst & BLUE_MASK);

    int rb_sub =
        ((src & RED_MASK) * (f_r + 1) |
        (src & BLUE_MASK) * (f_b + 1))
        >>>  8 & RB_MASK;
    int gn_sub = s_gn * (d_gn + 0x100)
        >>> 16 & GN_MASK;

    return min((dst >>> 24) + a, 0xFF) << 24 |
        (d_rb * d_a + (d_rb + (src & RB_MASK) - rb_sub) * s_a) >>> 8 & RB_MASK |
        (d_gn * d_a + (d_gn + s_gn            - gn_sub) * s_a) >>> 8 & GN_MASK;
  }


  /**
   * Overlay
   * O = 2 * MULTIPLY(D, S) = 2DS                   for D < 0.5
   * O = 2 * SCREEN(D, S) - 1 = 2(S + D - DS) - 1   otherwise
   */
  private static int blend_overlay(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    int d_r = dst & RED_MASK;
    int d_g = dst & GREEN_MASK;
    int d_b = dst & BLUE_MASK;

    int s_r = src & RED_MASK;
    int s_g = src & GREEN_MASK;
    int s_b = src & BLUE_MASK;

    int r = (d_r < 0x800000) ?
        d_r * ((s_r >>> 16) + 1) >>> 7 :
        0xFF0000 - ((0x100 - (s_r >>> 16)) * (RED_MASK - d_r) >>> 7);
    int g = (d_g < 0x8000) ?
        d_g * (s_g + 0x100) >>> 15 :
        (0xFF00 - ((0x10000 - s_g) * (GREEN_MASK - d_g) >>> 15));
    int b = (d_b < 0x80) ?
        d_b * (s_b + 1) >>> 7 :
        (0xFF00 - ((0x100 - s_b) * (BLUE_MASK - d_b) << 1)) >>> 8;

    return min((dst >>> 24) + a, 0xFF) << 24 |
        ((dst & RB_MASK) * d_a + ((r | b) & RB_MASK) * s_a) >>> 8 & RB_MASK |
        ((dst & GN_MASK) * d_a + (g       & GN_MASK) * s_a) >>> 8 & GN_MASK;
  }


  /**
   * Hard Light
   * O = OVERLAY(S, D)
   *
   * O = 2 * MULTIPLY(D, S) = 2DS                   for S < 0.5
   * O = 2 * SCREEN(D, S) - 1 = 2(S + D - DS) - 1   otherwise
   */
  private static int blend_hard_light(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    int d_r = dst & RED_MASK;
    int d_g = dst & GREEN_MASK;
    int d_b = dst & BLUE_MASK;

    int s_r = src & RED_MASK;
    int s_g = src & GREEN_MASK;
    int s_b = src & BLUE_MASK;

    int r = (s_r < 0x800000) ?
        s_r * ((d_r >>> 16) + 1) >>> 7 :
        0xFF0000 - ((0x100 - (d_r >>> 16)) * (RED_MASK - s_r) >>> 7);
    int g = (s_g < 0x8000) ?
        s_g * (d_g + 0x100) >>> 15 :
        (0xFF00 - ((0x10000 - d_g) * (GREEN_MASK - s_g) >>> 15));
    int b = (s_b < 0x80) ?
        s_b * (d_b + 1) >>> 7 :
        (0xFF00 - ((0x100 - d_b) * (BLUE_MASK - s_b) << 1)) >>> 8;

    return min((dst >>> 24) + a, 0xFF) << 24 |
        ((dst & RB_MASK) * d_a + ((r | b) & RB_MASK) * s_a) >>> 8 & RB_MASK |
        ((dst & GN_MASK) * d_a + (g       & GN_MASK) * s_a) >>> 8 & GN_MASK;
  }


  /**
   * Soft Light (Pegtop)
   * O = (1 - D) * MULTIPLY(D, S) + D * SCREEN(D, S)
   * O = (1 - D) * DS + D * (1 - (1 - D)(1 - S))
   * O = 2DS + DD - 2DDS
   */
  private static int blend_soft_light(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    int d_r = dst & RED_MASK;
    int d_g = dst & GREEN_MASK;
    int d_b = dst & BLUE_MASK;

    int s_r1 = src & RED_MASK >> 16;
    int s_g1 = src & GREEN_MASK >> 8;
    int s_b1 = src & BLUE_MASK;

    int d_r1 = (d_r >> 16) + (s_r1 < 7F ? 1 : 0);
    int d_g1 = (d_g >> 8)  + (s_g1 < 7F ? 1 : 0);
    int d_b1 = d_b         + (s_b1 < 7F ? 1 : 0);

    int r = (s_r1 * d_r >> 7) + 0xFF * d_r1 * (d_r1 + 1) -
        ((s_r1 * d_r1 * d_r1) << 1) & RED_MASK;
    int g = (s_g1 * d_g << 1) + 0xFF * d_g1 * (d_g1 + 1) -
        ((s_g1 * d_g1 * d_g1) << 1) >>> 8 & GREEN_MASK;
    int b = (s_b1 * d_b << 9) + 0xFF * d_b1 * (d_b1 + 1) -
        ((s_b1 * d_b1 * d_b1) << 1) >>> 16;

    return min((dst >>> 24) + a, 0xFF) << 24 |
        ((dst & RB_MASK) * d_a + (r | b) * s_a) >>> 8 & RB_MASK |
        ((dst & GN_MASK) * d_a + g       * s_a) >>> 8 & GN_MASK;
  }


  /**
   * Dodge
   * O = D / (1 - S)
   */
  private static int blend_dodge(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    int r = (dst & RED_MASK)          / (256 - ((src & RED_MASK) >> 16));
    int g = ((dst & GREEN_MASK) << 8) / (256 - ((src & GREEN_MASK) >> 8));
    int b = ((dst & BLUE_MASK)  << 8) / (256 - (src & BLUE_MASK));

    int rb =
        (r > 0xFF00 ? 0xFF0000 : ((r << 8) & RED_MASK)) |
        (b > 0x00FF ? 0x0000FF : b);
    int gn =
        (g > 0xFF00 ? 0x00FF00 : (g & GREEN_MASK));

    return min((dst >>> 24) + a, 0xFF) << 24 |
        ((dst & RB_MASK) * d_a + rb * s_a) >>> 8 & RB_MASK |
        ((dst & GN_MASK) * d_a + gn * s_a) >>> 8 & GN_MASK;
  }


  /**
   * Burn
   * O = 1 - (1 - A) / B
   */
  private static int blend_burn(int dst, int src) {
    int a = src >>> 24;

    int s_a = a + (a >= 0x7F ? 1 : 0);
    int d_a = 0x100 - s_a;

    int r = ((0xFF0000 - (dst & RED_MASK)))        / (1 + (src & RED_MASK >> 16));
    int g = ((0x00FF00 - (dst & GREEN_MASK)) << 8) / (1 + (src & GREEN_MASK >> 8));
    int b = ((0x0000FF - (dst & BLUE_MASK))  << 8) / (1 + (src & BLUE_MASK));

    int rb = RB_MASK -
        (r > 0xFF00 ? 0xFF0000 : ((r << 8) & RED_MASK)) -
        (b > 0x00FF ? 0x0000FF : b);
    int gn = GN_MASK -
        (g > 0xFF00 ? 0x00FF00 : (g & GREEN_MASK));

    return min((dst >>> 24) + a, 0xFF) << 24 |
        ((dst & RB_MASK) * d_a + rb * s_a) >>> 8 & RB_MASK |
        ((dst & GN_MASK) * d_a + gn * s_a) >>> 8 & GN_MASK;
  }


  //////////////////////////////////////////////////////////////

  // FILE I/O

  static protected PImage loadTIFF(byte tiff[]) {
    if ((tiff[42] != tiff[102]) ||  // width/height in both places
        (tiff[43] != tiff[103])) {
      System.err.println(TIFF_ERROR);
      return null;
    }

    int width =
      ((tiff[30] & 0xff) << 8) | (tiff[31] & 0xff);
    int height =
      ((tiff[42] & 0xff) << 8) | (tiff[43] & 0xff);

    int count =
      ((tiff[114] & 0xff) << 24) |
      ((tiff[115] & 0xff) << 16) |
      ((tiff[116] & 0xff) << 8) |
      (tiff[117] & 0xff);
    if (count != width * height * 3) {
      System.err.println(TIFF_ERROR + " (" + width + ", " + height +")");
      return null;
    }

    // check the rest of the header
    for (int i = 0; i < TIFF_HEADER.length; i++) {
      if ((i == 30) || (i == 31) || (i == 42) || (i == 43) ||
          (i == 102) || (i == 103) ||
          (i == 114) || (i == 115) || (i == 116) || (i == 117)) continue;

      if (tiff[i] != TIFF_HEADER[i]) {
        System.err.println(TIFF_ERROR + " (" + i + ")");
        return null;
      }
    }

    PImage outgoing = new PImage(width, height, RGB);
    int index = 768;
    count /= 3;
    for (int i = 0; i < count; i++) {
      outgoing.pixels[i] =
        0xFF000000 |
        (tiff[index++] & 0xff) << 16 |
        (tiff[index++] & 0xff) << 8 |
        (tiff[index++] & 0xff);
    }
    return outgoing;
  }

  protected boolean saveTIFF(OutputStream output) {
    // shutting off the warning, people can figure this out themselves
    /*
    if (format != RGB) {
      System.err.println("Warning: only RGB information is saved with " +
                         ".tif files. Use .tga or .png for ARGB images and others.");
    }
    */
    try {
      byte tiff[] = new byte[768];
      System.arraycopy(
          TIFF_HEADER,
          0,
          tiff,
          0,
          TIFF_HEADER.length
      );

      tiff[30] = (byte) ((pixelWidth >> 8) & 0xff);
      tiff[31] = (byte) ((pixelWidth) & 0xff);
      tiff[42] = tiff[102] = (byte) ((pixelHeight >> 8) & 0xff);
      tiff[43] = tiff[103] = (byte) ((pixelHeight) & 0xff);

      int count = pixelWidth*pixelHeight*3;
      tiff[114] = (byte) ((count >> 24) & 0xff);
      tiff[115] = (byte) ((count >> 16) & 0xff);
      tiff[116] = (byte) ((count >> 8) & 0xff);
      tiff[117] = (byte) ((count) & 0xff);

      // spew the header to the disk
      output.write(tiff);

      for (int i = 0; i < pixels.length; i++) {
        output.write((pixels[i] >> 16) & 0xff);
        output.write((pixels[i] >> 8) & 0xff);
        output.write(pixels[i] & 0xff);
      }
      output.flush();
      return true;

    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }


  /**
   * Creates a Targa32 formatted byte sequence of specified
   * pixel buffer using RLE compression.
   * </p>
   * Also figured out how to avoid parsing the image upside-down
   * (there's a header flag to set the image origin to top-left)
   * </p>
   * Starting with revision 0092, the format setting is taken into account:
   * <UL>
   * <LI><TT>ALPHA</TT> images written as 8bit grayscale (uses lowest byte)
   * <LI><TT>RGB</TT> &rarr; 24 bits
   * <LI><TT>ARGB</TT> &rarr; 32 bits
   * </UL>
   * All versions are RLE compressed.
   * </p>
   * Contributed by toxi 8-10 May 2005, based on this RLE
   * <A HREF="http://www.wotsit.org/download.asp?f=tga">specification</A>
   */
  protected boolean saveTGA(OutputStream output) {
    byte header[] = new byte[18];

     if (format == ALPHA) {  // save ALPHA images as 8bit grayscale
       header[2] = 0x0B;
       header[16] = 0x08;
       header[17] = 0x28;

     } else if (format == RGB) {
       header[2] = 0x0A;
       header[16] = 24;
       header[17] = 0x20;

     } else if (format == ARGB) {
       header[2] = 0x0A;
       header[16] = 32;
       header[17] = 0x28;

     } else {
       throw new RuntimeException("Image format not recognized inside save()");
     }
     // set image dimensions lo-hi byte order
     header[12] = (byte) (pixelWidth & 0xff);
     header[13] = (byte) (pixelWidth >> 8);
     header[14] = (byte) (pixelHeight & 0xff);
     header[15] = (byte) (pixelHeight >> 8);

     try {
       output.write(header);

       int maxLen = pixelHeight * pixelWidth;
       int index = 0;
       int col; //, prevCol;
       int[] currChunk = new int[128];

       // 8bit image exporter is in separate loop
       // to avoid excessive conditionals...
       if (format == ALPHA) {
         while (index < maxLen) {
           boolean isRLE = false;
           int rle = 1;
           currChunk[0] = col = pixels[index] & 0xff;
           while (index + rle < maxLen) {
             if (col != (pixels[index + rle]&0xff) || rle == 128) {
               isRLE = (rle > 1);
               break;
             }
             rle++;
           }
           if (isRLE) {
             output.write(0x80 | (rle - 1));
             output.write(col);

           } else {
             rle = 1;
             while (index + rle < maxLen) {
               int cscan = pixels[index + rle] & 0xff;
               if ((col != cscan && rle < 128) || rle < 3) {
                 currChunk[rle] = col = cscan;
               } else {
                 if (col == cscan) rle -= 2;
                 break;
               }
               rle++;
             }
             output.write(rle - 1);
             for (int i = 0; i < rle; i++) output.write(currChunk[i]);
           }
           index += rle;
         }
       } else {  // export 24/32 bit TARGA
         while (index < maxLen) {
           boolean isRLE = false;
           currChunk[0] = col = pixels[index];
           int rle = 1;
           // try to find repeating bytes (min. len = 2 pixels)
           // maximum chunk size is 128 pixels
           while (index + rle < maxLen) {
             if (col != pixels[index + rle] || rle == 128) {
               isRLE = (rle > 1); // set flag for RLE chunk
               break;
             }
             rle++;
           }
           if (isRLE) {
             output.write(128 | (rle - 1));
             output.write(col & 0xff);
             output.write(col >> 8 & 0xff);
             output.write(col >> 16 & 0xff);
             if (format == ARGB) output.write(col >>> 24 & 0xff);

           } else {  // not RLE
             rle = 1;
             while (index + rle < maxLen) {
               if ((col != pixels[index + rle] && rle < 128) || rle < 3) {
                 currChunk[rle] = col = pixels[index + rle];
               } else {
                 // check if the exit condition was the start of
                 // a repeating colour
                 if (col == pixels[index + rle]) rle -= 2;
                 break;
               }
               rle++;
             }
             // write uncompressed chunk
             output.write(rle - 1);
             if (format == ARGB) {
               for (int i = 0; i < rle; i++) {
                 col = currChunk[i];
                 output.write(col & 0xff);
                 output.write(col >> 8 & 0xff);
                 output.write(col >> 16 & 0xff);
                 output.write(col >>> 24 & 0xff);
               }
             } else {
               for (int i = 0; i < rle; i++) {
                 col = currChunk[i];
                 output.write(col & 0xff);
                 output.write(col >> 8 & 0xff);
                 output.write(col >> 16 & 0xff);
               }
             }
           }
           index += rle;
         }
       }
       output.flush();
       return true;

     } catch (IOException e) {
       e.printStackTrace();
       return false;
     }
  }


  /**
   * Use ImageIO functions from Java 1.4 and later to handle image save.
   * Various formats are supported, typically jpeg, png, bmp, and wbmp.
   * To get a list of the supported formats for writing, use: <BR>
   * <TT>println(javax.imageio.ImageIO.getReaderFormatNames())</TT>
   */
  protected boolean saveImageIO(String path) throws IOException {
    try {
      int outputFormat = (format == ARGB) ?
        BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

      String extension =
        path.substring(path.lastIndexOf('.') + 1).toLowerCase();

      // JPEG and BMP images that have an alpha channel set get pretty unhappy.
      // BMP just doesn't write, and JPEG writes it as a CMYK image.
      // http://code.google.com/p/processing/issues/detail?id=415
      if (extension.equals("bmp") || extension.equals("jpg") || extension.equals("jpeg")) {
        outputFormat = BufferedImage.TYPE_INT_RGB;
      }

      BufferedImage bimage = new BufferedImage(pixelWidth, pixelHeight, outputFormat);
      bimage.setRGB(0, 0, pixelWidth, pixelHeight, pixels, 0, pixelWidth);

      File file = new File(path);

      ImageWriter writer = null;
      ImageWriteParam param = null;
      IIOMetadata metadata = null;

      if (extension.equals("jpg") || extension.equals("jpeg")) {
        if ((writer = imageioWriter("jpeg")) != null) {
          // Set JPEG quality to 90% with baseline optimization. Setting this
          // to 1 was a huge jump (about triple the size), so this seems good.
          // Oddly, a smaller file size than Photoshop at 90%, but I suppose
          // it's a completely different algorithm.
          param = writer.getDefaultWriteParam();
          param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
          param.setCompressionQuality(0.9f);
        }
      }

      if (extension.equals("png")) {
        if ((writer = imageioWriter("png")) != null) {
          param = writer.getDefaultWriteParam();
          if (false) {
            metadata = imageioDPI(writer, param, 100);
          }
        }
      }

      if (writer != null) {
        BufferedOutputStream output =
          new BufferedOutputStream(PApplet.createOutput(file));
        writer.setOutput(ImageIO.createImageOutputStream(output));
//        writer.write(null, new IIOImage(bimage, null, null), param);
        writer.write(metadata, new IIOImage(bimage, null, metadata), param);
        writer.dispose();

        output.flush();
        output.close();
        return true;
      }
      // If iter.hasNext() somehow fails up top, it falls through to here
      return javax.imageio.ImageIO.write(bimage, extension, file);

    } catch (Exception e) {
      e.printStackTrace();
      throw new IOException("image save failed.");
    }
  }


  private ImageWriter imageioWriter(String extension) {
    Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(extension);
    if (iter.hasNext()) {
      return iter.next();
    }
    return null;
  }


  private IIOMetadata imageioDPI(ImageWriter writer, ImageWriteParam param, double dpi) {
    // http://stackoverflow.com/questions/321736/how-to-set-dpi-information-in-an-image
    ImageTypeSpecifier typeSpecifier =
      ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
    IIOMetadata metadata =
      writer.getDefaultImageMetadata(typeSpecifier, param);

    if (!metadata.isReadOnly() && metadata.isStandardMetadataFormatSupported()) {
      // for PNG, it's dots per millimeter
      double dotsPerMilli = dpi / 25.4;

      IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
      horiz.setAttribute("value", Double.toString(dotsPerMilli));

      IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
      vert.setAttribute("value", Double.toString(dotsPerMilli));

      IIOMetadataNode dim = new IIOMetadataNode("Dimension");
      dim.appendChild(horiz);
      dim.appendChild(vert);

      IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
      root.appendChild(dim);

      try {
        metadata.mergeTree("javax_imageio_1.0", root);
        return metadata;

      } catch (IIOInvalidTreeException e) {
        System.err.println("Could not set the DPI of the output image");
        e.printStackTrace();
      }
    }
    return null;
  }


  protected String[] saveImageFormats;

  /**
   * ( begin auto-generated from PImage_save.xml )
   *
   * Saves the image into a file. Append a file extension to the name of
   * the file, to indicate the file format to be used: either TIFF (.tif),
   * TARGA (.tga), JPEG (.jpg), or PNG (.png). If no extension is included
   * in the filename, the image will save in TIFF format and .tif will be
   * added to the name.  These files are saved to the sketch's folder, which
   * may be opened by selecting "Show sketch folder" from the "Sketch" menu.
   * <br /><br />To save an image created within the code, rather
   * than through loading, it's necessary to make the image with the
   * <b>createImage()</b> function so it is aware of the location of the
   * program and can therefore save the file to the right place. See the
   * <b>createImage()</b> reference for more information.
   *
   * ( end auto-generated )
   * <h3>Advanced</h3>
   * Save this image to disk.
   * <p>
   * As of revision 0100, this function requires an absolute path,
   * in order to avoid confusion. To save inside the sketch folder,
   * use the function savePath() from PApplet, or use saveFrame() instead.
   * As of revision 0116, savePath() is not needed if this object has been
   * created (as recommended) via createImage() or createGraphics() or
   * one of its neighbors.
   * <p>
   * As of revision 0115, when using Java 1.4 and later, you can write
   * to several formats besides tga and tiff. If Java 1.4 is installed
   * and the extension used is supported (usually png, jpg, jpeg, bmp,
   * and tiff), then those methods will be used to write the image.
   * To get a list of the supported formats for writing, use: <BR>
   * <TT>println(javax.imageio.ImageIO.getReaderFormatNames())</TT>
   * <p>
   * To use the original built-in image writers, use .tga or .tif as the
   * extension, or don't include an extension. When no extension is used,
   * the extension .tif will be added to the file name.
   * <p>
   * The ImageIO API claims to support wbmp files, however they probably
   * require a black and white image. Basic testing produced a zero-length
   * file with no error.
   *
   * @webref pimage:method
   * @brief Saves the image to a TIFF, TARGA, PNG, or JPEG file
   * @usage application
   * @param filename a sequence of letters and numbers
   */
   public boolean save(String filename) {  // ignore
     boolean success = false;

     if (parent != null) {
       // use savePath(), so that the intermediate directories are created
       filename = parent.savePath(filename);

     } else {
       File file = new File(filename);
       if (file.isAbsolute()) {
         // make sure that the intermediate folders have been created
         PApplet.createPath(file);
       } else {
         String msg =
           "PImage.save() requires an absolute path. " +
           "Use createImage(), or pass savePath() to save().";
         PGraphics.showException(msg);
       }
     }

     // Make sure the pixel data is ready to go
     loadPixels();

     try {
       OutputStream os = null;

       if (saveImageFormats == null) {
         saveImageFormats = javax.imageio.ImageIO.getWriterFormatNames();
       }
       if (saveImageFormats != null) {
         for (int i = 0; i < saveImageFormats.length; i++) {
           if (filename.endsWith("." + saveImageFormats[i])) {
             if (!saveImageIO(filename)) {
               System.err.println("Error while saving image.");
               return false;
             }
             return true;
           }
         }
       }

       if (filename.toLowerCase().endsWith(".tga")) {
         os = new BufferedOutputStream(new FileOutputStream(filename), 32768);
         success = saveTGA(os); //, pixels, width, height, format);

       } else {
         if (!filename.toLowerCase().endsWith(".tif") &&
             !filename.toLowerCase().endsWith(".tiff")) {
           // if no .tif extension, add it..
           filename += ".tif";
         }
         os = new BufferedOutputStream(new FileOutputStream(filename), 32768);
         success = saveTIFF(os); //, pixels, width, height);
       }
       os.flush();
       os.close();

     } catch (IOException e) {
       System.err.println("Error while saving image.");
       e.printStackTrace();
       success = false;
     }
     return success;
   }
}
