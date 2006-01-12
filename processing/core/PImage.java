/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-05 Ben Fry and Casey Reas
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
import java.lang.reflect.*;
import java.io.*;


/**
 * Storage class for pixel data.
 * <P>
 * Code for copying, resizing, scaling, and blending contributed
 * by <A HREF="http://www.toxi.co.uk">toxi</A>
 * <P>
 */
public class PImage implements PConstants, Cloneable {

  /**
   * Format for this image, one of RGB, ARGB or ALPHA.
   * note that RGB images still require 0xff in the high byte
   * because of how they'll be manipulated by other functions
   */
  public int format;

  public int pixels[];
  public int width, height;
  // would scan line be useful? maybe for pow of 2 gl textures

  // note! inherited by PGraphics
  public int imageMode = CORNER;
  public boolean smooth = false;

  /** native storage for java 1.3 image object */
  //public Object image;

  /** for subclasses that need to store info about the image */
  public Object cache;

  /** modified portion of the image */
  public boolean modified;
  public int mx1, my1, mx2, my2;

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
  int blurRadius;
  int blurKernelSize;
  int[] blurKernel;
  int[][] blurMult;


  //////////////////////////////////////////////////////////////


  /**
   * Create an empty image object, set its format to RGB.
   * The pixel array is not allocated.
   */
  public PImage() {
    format = RGB;  // makes sure that this guy is useful
    cache = null;
  }


  /**
   * Create a new RGB (alpha ignored) image of a specific size.
   * All pixels are set to zero, meaning black, but since the
   * alpha is zero, it will be transparent.
   */
  public PImage(int width, int height) {
    init(width, height, RGB);
    //this(new int[width * height], width, height, ARGB);
    // toxi: is it maybe better to init the image with max alpha enabled?
    //for(int i=0; i<pixels.length; i++) pixels[i]=0xffffffff;
    // fry: i'm opting for the full transparent image, which is how
    // photoshop works, and our audience oughta be familiar with.
    // also, i want to avoid having to set all those pixels since
    // in java it's super slow, and most using this fxn will be
    // setting all the pixels anyway.
    // toxi: agreed and same reasons why i left it out ;)
  }


  public PImage(int pixels[], int width, int height, int format) {
    this.pixels = pixels;
    this.width = width;
    this.height = height;
    this.format = format;
    this.cache = null;
  }


  /**
   * Function to be used by subclasses to setup their own bidness.
   */
  public void init(int width, int height, int format) {  // ignore
    this.width = width;
    this.height = height;
    this.pixels = new int[width*height];
    this.format = format;
    this.cache = null;
  }


  /**
   * Construct a new PImage from a java.awt.Image
   *
   * this constructor assumes that you've done the work of
   * making sure a MediaTracker has been used to fully
   * download the data and that the img is valid.
   */
  public PImage(java.awt.Image img) {
    width = img.getWidth(null);
    height = img.getHeight(null);

    pixels = new int[width*height];
    PixelGrabber pg =
      new PixelGrabber(img, 0, 0, width, height, pixels, 0, width);
    try {
      pg.grabPixels();
    } catch (InterruptedException e) { }

    format = RGB;
    cache = null;
  }


  //////////////////////////////////////////////////////////////


  /**
   * mode is one of CORNERS or CORNER, because the others are
   * just too weird for the other functions
   */
  public void imageMode(int mode) {
    if ((mode == CORNER) || (mode == CORNERS)) {
      imageMode = mode;
    } else {
      String msg = "imageMode() only works with CORNER or CORNERS";
      throw new RuntimeException(msg);
    }
  }


  /**
   * If true in PImage, use bilinear interpolation for copy()
   * operations. When inherited by PGraphics, also controls shapes.
   */
  public void smooth() {
    smooth = true;
  }


  /**
   * Disable smoothing. See smooth().
   */
  public void noSmooth() {
    smooth = false;
  }



  //////////////////////////////////////////////////////////////

  // MARKING IMAGE AS MODIFIED / FOR USE w/ GET/SET


  /*
  public int[] loadPixels() {
    return getPixels(0, 0, width, height);
  }
  */


  /**
   * Note that when using imageMode(CORNERS),
   * the x2 and y2 positions are non-inclusive.
   */
  /*
  public int[] loadPixels(int x1, int y1, int x2, int y2) {
    if (modified) {
      // have to set the modified region to include the min/max
      // of the coordinates coming in.
      // also, mustn't get the pixels for the section that's
      // already been marked as modified. gah.
      // too complicated, just throw an error
      String msg =
        "getPixels(x, y, w, h) cannot be used multiple times. " +
        "Use getPixels() once to get the entire image instead.";
      throw new RuntimeException(msg);
    }

    if (imageMode == CORNER) {  // x2, y2 are w/h
      x2 += x1;
      y2 += y1;
    }

    if (pixels == null) {  // this is a java 1.3 buffered image
      if (image == null) {  // this is just an error
        throw new RuntimeException("PImage not properly setup for getPixels()");
      } else {
        pixels = new int[width*height];
      }
    }

    if (image == null) {
      // this happens when using just the 1.1 library
      // no need to do anything, since the pixels have already been grabbed

    } else {
      // copy the contents of the buffered image to pixels[]
      //((BufferedImage) image).getRGB(x, y, w, h, output.pixels, 0, width);
      try {
        //System.out.println("running getrgb...");
        Class bufferedImageClass =
          Class.forName("java.awt.image.BufferedImage");
        // getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize)
        Method getRgbMethod =
          bufferedImageClass.getMethod("getRGB", new Class[] {
            Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE,
            int[].class, Integer.TYPE, Integer.TYPE
          });
        getRgbMethod.invoke(image, new Object[] {
          new Integer(x1), new Integer(y1),
          new Integer(x2 - x1 + 1), new Integer(y2 - y1 + 1),
          pixels, new Integer(0), new Integer(width)
        });

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return pixels;  // just to be nice
  }
  */


  /**
   * For subclasses where the pixels[] buffer isn't set by default,
   * this should copy all data into the pixels[] array
   */
  public void loadPixels() {  // ignore
  }


  /**
   * Mark all pixels as needing update.
   */
  public void updatePixels() {
    updatePixels(0, 0, width, height);
  }


  /**
   * Mark the pixels in this region as needing an update.
   * <P>
   * This is not currently used by any of the renderers, however the api
   * is structured this way in the hope of being able to use this to
   * speed things up in the future.
   * <P>
   * Note that when using imageMode(CORNERS),
   * the x2 and y2 positions are non-inclusive.
   */
  public void updatePixels(int x1, int y1, int x2, int y2) {
    //if (!modified) {  // could just set directly, but..
    //}

    if (imageMode == CORNER) {  // x2, y2 are w/h
      x2 += x1;
      y2 += y1;
    }

    if (!modified) {
      mx1 = x1;
      mx2 = x2;
      my1 = y1;
      my2 = y2;
      modified = true;

    } else {
      if (x1 < mx1) mx1 = x1;
      if (x1 > mx2) mx2 = x1;
      if (y1 < my1) my1 = y1;
      if (y1 > my2) my2 = y1;

      if (x2 < mx1) mx1 = x2;
      if (x2 > mx2) mx2 = x2;
      if (y2 < my1) my1 = y2;
      if (y2 > my2) my2 = y2;
    }
  }


  //public void pixelsUpdated() {
    //mx1 = Integer.MAX_VALUE;
    //my1 = Integer.MAX_VALUE;
    //mx2 = -Integer.MAX_VALUE;
    //my2 = -Integer.MAX_VALUE;
  //modified = false;
  //}


  //////////////////////////////////////////////////////////////

  // GET/SET PIXELS


  /**
   * Returns an ARGB "color" type (a packed 32 bit int with the color.
   * If the coordinate is outside the image, zero is returned
   * (black, but completely transparent).
   * <P>
   * If the image is in RGB format (i.e. on a PVideo object),
   * the value will get its high bits set, just to avoid cases where
   * they haven't been set already.
   * <P>
   * If the image is in ALPHA format, this returns a white color
   * that has its alpha value set.
   * <P>
   * This function is included primarily for beginners. It is quite
   * slow because it has to check to see if the x, y that was provided
   * is inside the bounds, and then has to check to see what image
   * type it is. If you want things to be more efficient, access the
   * pixels[] array directly.
   */
  public int get(int x, int y) {
    if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) return 0;

    switch (format) {
      case RGB:
        return pixels[y*width + x] | 0xff000000;

      case ARGB:
        return pixels[y*width + x];

      case ALPHA:
        return (pixels[y*width + x] << 24) | 0xffffff;
    }
    return 0;
  }


  /**
   * Grab a subsection of a PImage, and copy it into a fresh PImage.
   * This honors imageMode() for the coordinates.
   */
  public PImage get(int x, int y, int w, int h) {
    if (imageMode == CORNERS) {  // if CORNER, do nothing
      //x2 += x1; y2 += y1;
      // w/h are x2/y2 in this case, bring em down to size
      w = (w - x);
      h = (h - x);
    }

    if (x < 0) {
      w += x; // clip off the left edge
      x = 0;
    }
    if (y < 0) {
      h += y; // clip off some of the height
      y = 0;
    }

    if (x + w > width) w = width - x;
    if (y + h > height) h = height - y;

    PImage newbie = new PImage(new int[w*h], w, h, format);

    int index = y*width + x;
    int index2 = 0;
    for (int row = y; row < y+h; row++) {
      System.arraycopy(pixels, index,
                       newbie.pixels, index2, w);
      index+=width;
      index2+=w;
    }
    return newbie;
  }


  /**
   * Returns a copy of this PImage. Equivalent to get(0, 0, width, height).
   */
  public PImage get() {
    try {
      return (PImage) clone();
    } catch (CloneNotSupportedException e) {
      return null;
    }
  }


  /**
   * Silently ignores if the coordinate is outside the image.
   */
  public void set(int x, int y, int c) {
    if ((x < 0) || (y < 0) || (x >= width) || (y >= height)) return;
    pixels[y*width + x] = c;
  }


  public void set(int dx, int dy, PImage src) {
    int sx = 0;
    int sy = 0;
    int sw = src.width;
    int sh = src.height;

    if (dx < 0) {  // off left edge
      sx -= dx;
      sw += dx;
      dx = 0;
    }
    if (dy < 0) {  // off top edge
      sy -= dy;
      sh += dy;
      dy = 0;
    }
    if (dx + sw > width) {  // off right edge
      sw = width - dx;
    }
    if (dy + sh > height) {  // off bottom edge
      sh = height - dy;
    }

    // this could be nonexistant
    if ((sw <= 0) || (sh <= 0)) return;

    setImpl(dx, dy, sx, sy, sw, sh, src);
  }


  /**
   * Internal function to actually handle setting a block of pixels that
   * has already been properly cropped from the image to a valid region.
   */
  protected void setImpl(int dx, int dy, int sx, int sy, int sw, int sh,
                         PImage src) {
    int srcOffset = sy * src.width + sx;
    int dstOffset = dy * width + dx;

    for (int y = sy; y < sy + sh; y++) {
      System.arraycopy(src.pixels, srcOffset, pixels, dstOffset, sw);
      srcOffset += src.width;
      dstOffset += width;
    }
  }



  //////////////////////////////////////////////////////////////

  // ALPHA CHANNEL


  /**
   * Set alpha channel for an image. Black colors in the source
   * image will make the destination image completely transparent,
   * and white will make things fully opaque. Gray values will
   * be in-between steps.
   * <P>
   * Strictly speaking the "blue" value from the source image is
   * used as the alpha color. For a fully grayscale image, this
   * is correct, but for a color image it's not 100% accurate.
   * For a more accurate conversion, first use filter(GRAY)
   * which will make the image into a "correct" grayscake by
   * performing a proper luminance-based conversion.
   */
  public void mask(int alpha[]) {
    // don't execute if mask image is different size
    if (alpha.length != pixels.length) {
      throw new RuntimeException("The PImage used with mask() must be " +
                                 "the same size as the applet.");
    }
    for (int i = 0; i < pixels.length; i++) {
      pixels[i] = ((alpha[i] & 0xff) << 24) | (pixels[i] & 0xffffff);
    }
    format = ARGB;
  }


  /**
   * Set alpha channel for an image using another image as the source.
   */
  public void mask(PImage alpha) {
    mask(alpha.pixels);
  }


  /**
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
   */
  public void filter(int kind) {
    switch (kind) {

      case BLUR:
        // TODO write basic low-pass filter blur here
        // what does photoshop do on the edges with this guy?
        // better yet.. why bother? just use gaussian with radius 1
        filter(BLUR, 1);
        break;

      case GRAY:
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

      case RGB:
        for (int i = 0; i < pixels.length; i++) {
          pixels[i] |= 0xff000000;
        }
        format = RGB;
        break;

      case THRESHOLD:
        filter(THRESHOLD, 0.5f);
        break;

      // [toxi20050728] added new filters
      case ERODE:
        dilate(true);
        break;

      case DILATE:
        dilate(false);
        break;
    }
    updatePixels();  // mark as modified
  }


  /**
   * Method to apply a variety of basic filters to this image.
   * These filters all take a parameter.
   * <P>
   * <UL>
   * <LI>filter(BLUR, int radius) performs a gaussian blur of the
   * specified radius.
   * <LI>filter(POSTERIZE, int levels) will posterize the image to
   * between 2 and 255 levels.
   * <LI>filter(THRESHOLD, float center) allows you to set the
   * center point for the threshold. It takes a value from 0 to 1.0.
   * </UL>
   * Gaussian blur code contributed by
   * <A HREF="http://incubator.quasimondo.com">Mario Klingemann</A>
   * and later updated by toxi for better speed.
   */
  public void filter(int kind, float param) {
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
        // TODO not optimized
        int levels256 = 256 / levels;
        int levels1 = levels - 1;
        for (int i = 0; i < pixels.length; i++) {
          int rlevel = ((pixels[i] >> 16) & 0xff) / levels256;
          int glevel = ((pixels[i] >> 8) & 0xff) / levels256;
          int blevel = (pixels[i] & 0xff) / levels256;

          rlevel = (rlevel * 255 / levels1) & 0xff;
          glevel = (glevel * 255 / levels1) & 0xff;
          blevel = (blevel * 255 / levels1) & 0xff;

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


    /* protected void blur(float r) {
    // adjustment to make this algorithm
    // similar to photoshop's gaussian blur settings
    int radius = (int) (r * 3.5f);
    radius = (radius < 1) ? 1 : ((radius < 248) ? radius : 248);
    //radius = min(Math.max(1, radius), 248);
    if (blurRadius != radius) {
      // it's actually a little silly to cache this stuff
      // when all the cost is gonna come from allocating 2x the
      // image size in r1[] and r2[] et al.

      blurRadius = radius;
      blurKernelSize = 1 + radius*2;
      blurKernel = new int[blurKernelSize]; //1 + radius*2];
      blurMult = new int[blurKernelSize][256]; //new int[1+radius*2][256];

      int sum = 0;
      for (int i = 1; i < radius; i++) {
        int radiusi = radius - i;
        blurKernel[radius+i] = blurKernel[radiusi] = radiusi * radiusi;
        sum += blurKernel[radiusi] + blurKernel[radiusi];
        for (int j = 0; j < 256; j++) {
          blurMult[radius+i][j] = blurMult[radiusi][j] = blurKernel[radiusi]*j;
        }
      }
      blurKernel[radius] = radius * radius;
      sum += blurKernel[radius];
      for (int j = 0; j < 256; j++) {
        blurMult[radius][j] = blurKernel[radius]*j;
      }
    }

    //void blur(BImage img,int x, int y,int w,int h){
    int sum, cr, cg, cb, k;
    int pixel, read, ri, xl, yl, ym, riw;
    //int[] pix=img.pixels;
    //int iw=img.width;

    int wh = width * height;
    int r1[] = new int[wh];
    int g1[] = new int[wh];
    int b1[] = new int[wh];

    for (int i = 0; i < wh; i++) {
      ri = pixels[i];
      r1[i] = (ri & 0xff0000) >> 16;
      g1[i] = (ri & 0x00ff00) >> 8;
      b1[i] = (ri & 0x0000ff);
    }

    int r2[] = new int[wh];
    int g2[] = new int[wh];
    int b2[] = new int[wh];

    int x = 0; //Math.max(0, x);
    int y = 0; //Math.max(0, y);
    int w = width; // x + w - Math.max(0, (x+w)-width);
    int h = height; //y + h - Math.max(0, (y+h)-height);
    int yi = y*width;

    for (yl = y; yl < h; yl++) {
      for (xl = x; xl < w; xl++) {
        cb = cg = cr = sum = 0;
        ri = xl - blurRadius;
        for (int i = 0; i < blurKernelSize; i++) {
          read = ri + i;
          if ((read >= x) && (read < w)) {
            read += yi;
            cr += blurMult[i][r1[read]];
            cg += blurMult[i][g1[read]];
            cb += blurMult[i][b1[read]];
            sum += blurKernel[i];
          }
        }
        ri = yi + xl;
        r2[ri] = cr / sum;
        g2[ri] = cg / sum;
        b2[ri] = cb / sum;
      }
      yi += width;
    }
    yi = y * width;

    for (yl = y; yl < h; yl++) {
      ym = yl - blurRadius;
      riw = ym * width;
      for (xl = x; xl < w; xl++) {
        cb = cg = cr = sum = 0;
        ri = ym;
        read = xl + riw;

        for (int i = 0; i < blurKernelSize; i++) {
          if ((ri < h) && (ri >= y)) {
            cr += blurMult[i][r2[read]];
            cg += blurMult[i][g2[read]];
            cb += blurMult[i][b2[read]];
            sum += blurKernel[i];
          }
          ri++;
          read += width;
        }
        pixels[xl+yi] = 0xff000000 | (cr/sum)<<16 | (cg/sum)<<8 | (cb/sum);
      }
      yi += width;
    }
  }
    // end of original blur code.......
    */


  /**
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
    int sum, cr, cg, cb, k;
    int pixel, read, ri, roff, ym, ymi, riw,bk0;
    int b2[] = new int[pixels.length];
    int yi = 0;

    buildBlurKernel(r);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        cb = cg = cr = sum = 0;
        read = x - blurRadius;
        if (read<0) {
          bk0=-read;
          read=0;
        } else {
          if (read >= width)
            break;
          bk0=0;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (read >= width)
            break;
          int c = pixels[read + yi];
          int[] bm=blurMult[i];
          cb += bm[c & BLUE_MASK];
          sum += blurKernel[i];
          read++;
        }
        ri = yi + x;
        b2[ri] = cb / sum;
      }
      yi += width;
    }

    yi = 0;
    ym=-blurRadius;
    ymi=ym*width;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        cb = cg = cr = sum = 0;
        if (ym<0) {
          bk0 = ri = -ym;
          read = x;
        } else {
          if (ym >= height)
            break;
          bk0 = 0;
          ri = ym;
          read = x + ymi;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (ri >= height)
            break;
          int[] bm=blurMult[i];
          cb += bm[b2[read]];
          sum += blurKernel[i];
          ri++;
          read += width;
        }
        pixels[x+yi] = (cb/sum);
      }
      yi += width;
      ymi += width;
      ym++;
    }
  }

  protected void blurRGB(float r) {
    int sum, cr, cg, cb, k;
    int pixel, read, ri, roff, ym, ymi, riw,bk0;
    int r2[] = new int[pixels.length];
    int g2[] = new int[pixels.length];
    int b2[] = new int[pixels.length];
    int yi = 0;

    buildBlurKernel(r);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        cb = cg = cr = sum = 0;
        read = x - blurRadius;
        if (read<0) {
          bk0=-read;
          read=0;
        } else {
          if (read >= width)
            break;
          bk0=0;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (read >= width)
            break;
          int c = pixels[read + yi];
          int[] bm=blurMult[i];
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
      yi += width;
    }

    yi = 0;
    ym=-blurRadius;
    ymi=ym*width;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        cb = cg = cr = sum = 0;
        if (ym<0) {
          bk0 = ri = -ym;
          read = x;
        } else {
          if (ym >= height)
            break;
          bk0 = 0;
          ri = ym;
          read = x + ymi;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (ri >= height)
            break;
          int[] bm=blurMult[i];
          cr += bm[r2[read]];
          cg += bm[g2[read]];
          cb += bm[b2[read]];
          sum += blurKernel[i];
          ri++;
          read += width;
        }
        pixels[x+yi] = 0xff000000 | (cr/sum)<<16 | (cg/sum)<<8 | (cb/sum);
      }
      yi += width;
      ymi += width;
      ym++;
    }
  }

  protected void blurARGB(float r) {
    int sum, cr, cg, cb, ca;
    int pixel, read, ri, roff, ym, ymi, riw, bk0;
    int wh = pixels.length;
    int r2[] = new int[wh];
    int g2[] = new int[wh];
    int b2[] = new int[wh];
    int a2[] = new int[wh];
    int yi = 0;

    buildBlurKernel(r);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        cb = cg = cr = ca = sum = 0;
        read = x - blurRadius;
        if (read<0) {
          bk0=-read;
          read=0;
        } else {
          if (read >= width)
            break;
          bk0=0;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (read >= width)
            break;
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
      yi += width;
    }

    yi = 0;
    ym=-blurRadius;
    ymi=ym*width;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        cb = cg = cr = ca = sum = 0;
        if (ym<0) {
          bk0 = ri = -ym;
          read = x;
        } else {
          if (ym >= height)
            break;
          bk0 = 0;
          ri = ym;
          read = x + ymi;
        }
        for (int i = bk0; i < blurKernelSize; i++) {
          if (ri >= height)
            break;
          int[] bm=blurMult[i];
          ca += bm[a2[read]];
          cr += bm[r2[read]];
          cg += bm[g2[read]];
          cb += bm[b2[read]];
          sum += blurKernel[i];
          ri++;
          read += width;
        }
        pixels[x+yi] = (ca/sum)<<24 | (cr/sum)<<16 | (cg/sum)<<8 | (cb/sum);
      }
      yi += width;
      ymi += width;
      ym++;
    }
  }

  /**
   * Generic dilate/erode filter using luminance values
   * as decision factor. [toxi 050728]
   */
  protected void dilate(boolean isInverted) {
    int currIdx=0;
    int maxIdx=pixels.length;
    int[] out=new int[maxIdx];

    if (!isInverted) {
      // erosion (grow light areas)
      while (currIdx<maxIdx) {
        int currRowIdx=currIdx;
        int maxRowIdx=currIdx+width;
        while (currIdx<maxRowIdx) {
          int colOrig,colOut;
          colOrig=colOut=pixels[currIdx];
          int idxLeft=currIdx-1;
          int idxRight=currIdx+1;
          int idxUp=currIdx-width;
          int idxDown=currIdx+width;
          if (idxLeft<currRowIdx)
            idxLeft=currIdx;
          if (idxRight>=maxRowIdx)
            idxRight=currIdx;
          if (idxUp<0)
            idxUp=0;
          if (idxDown>=maxIdx)
            idxDown=currIdx;

          int colUp=pixels[idxUp];
          int colLeft=pixels[idxLeft];
          int colDown=pixels[idxDown];
          int colRight=pixels[idxRight];

          // compute luminance
          int currLum =
            77*(colOrig>>16&0xff) + 151*(colOrig>>8&0xff) + 28*(colOrig&0xff);
          int lumLeft =
            77*(colLeft>>16&0xff) + 151*(colLeft>>8&0xff) + 28*(colLeft&0xff);
          int lumRight =
            77*(colRight>>16&0xff) + 151*(colRight>>8&0xff) + 28*(colRight&0xff);
          int lumUp =
            77*(colUp>>16&0xff) + 151*(colUp>>8&0xff) + 28*(colUp&0xff);
          int lumDown =
            77*(colDown>>16&0xff) + 151*(colDown>>8&0xff) + 28*(colDown&0xff);

          if (lumLeft>currLum) {
            colOut=colLeft;
            currLum=lumLeft;
          }
          if (lumRight>currLum) {
            colOut=colRight;
            currLum=lumRight;
          }
          if (lumUp>currLum) {
            colOut=colUp;
            currLum=lumUp;
          }
          if (lumDown>currLum) {
            colOut=colDown;
            currLum=lumDown;
          }
          out[currIdx++]=colOut;
        }
      }
    } else {
      // dilate (grow dark areas)
      while (currIdx<maxIdx) {
        int currRowIdx=currIdx;
        int maxRowIdx=currIdx+width;
        while (currIdx<maxRowIdx) {
          int colOrig,colOut;
          colOrig=colOut=pixels[currIdx];
          int idxLeft=currIdx-1;
          int idxRight=currIdx+1;
          int idxUp=currIdx-width;
          int idxDown=currIdx+width;
          if (idxLeft<currRowIdx)
            idxLeft=currIdx;
          if (idxRight>=maxRowIdx)
            idxRight=currIdx;
          if (idxUp<0)
            idxUp=0;
          if (idxDown>=maxIdx)
            idxDown=currIdx;

          int colUp=pixels[idxUp];
          int colLeft=pixels[idxLeft];
          int colDown=pixels[idxDown];
          int colRight=pixels[idxRight];

          // compute luminance
          int currLum =
            77*(colOrig>>16&0xff) + 151*(colOrig>>8&0xff) + 28*(colOrig&0xff);
          int lumLeft =
            77*(colLeft>>16&0xff) + 151*(colLeft>>8&0xff) + 28*(colLeft&0xff);
          int lumRight =
            77*(colRight>>16&0xff) + 151*(colRight>>8&0xff) + 28*(colRight&0xff);
          int lumUp =
            77*(colUp>>16&0xff) + 151*(colUp>>8&0xff) + 28*(colUp&0xff);
          int lumDown =
            77*(colDown>>16&0xff) + 151*(colDown>>8&0xff) + 28*(colDown&0xff);

          if (lumLeft<currLum) {
            colOut=colLeft;
            currLum=lumLeft;
          }
          if (lumRight<currLum) {
            colOut=colRight;
            currLum=lumRight;
          }
          if (lumUp<currLum) {
            colOut=colUp;
            currLum=lumUp;
          }
          if (lumDown<currLum) {
            colOut=colDown;
            currLum=lumDown;
          }
          out[currIdx++]=colOut;
        }
      }
    }
    System.arraycopy(out,0,pixels,0,maxIdx);
  }


  //////////////////////////////////////////////////////////////

  // REPLICATING & BLENDING (AREAS) OF PIXELS


  /**
   * Copy things from one area of this image
   * to another area in the same image.
   */
  public void copy(int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    copy(this, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2);
  }


  /**
   * Copies area of one image into another PImage object.
   */
  public void copy(PImage src,
                   int sx1, int sy1, int sx2, int sy2,
                   int dx1, int dy1, int dx2, int dy2) {
    if (imageMode == CORNER) {  // if CORNERS, do nothing
            sx2 += sx1;
            sy2 += sy1;
            dx2 += dx1;
            dy2 += dy1;

    //} else if (imageMode == CENTER) {
      //sx2 /= 2f; sy2 /= 2f;
      //dx2 /= 2f; dy2 /= 2f;
    }

    if ((src == this) &&
        intersect(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2)) {
      // if src is me, and things intersect, make a copy of the data
      blit_resize(get(sx1, sy1, sx2 - sx1, sy2 - sy1),
                  0, 0, sx2 - sx1 - 1, sy2 - sy1 - 1,
                  pixels, width, height, dx1, dy1, dx2, dy2, REPLACE);
    } else {
      blit_resize(src, sx1, sy1, sx2, sy2,
                  pixels, width, height, dx1, dy1, dx2, dy2, REPLACE);
    }
  }


  /**
   * Blend a two colors based on a particular mode.
   * <PRE>
   * BLEND - linear interpolation of colours: C = A*factor + B
   * ADD - additive blending with white clip: C = min(A*factor + B, 255)
   * SUBSTRACT - substractive blend with black clip: C = max(B - A*factor, 0)
   * DARKEST - only the darkest colour succeeds: C = min(A*factor, B)
   * LIGHTEST - only the lightest colour succeeds: C = max(A*factor, B)
   * REPLACE - destination colour equals colour of source pixel: C = A
   * </PRE>
   */
  static public int blend(int c1, int c2, int mode) {
    switch (mode) {
    case BLEND:    return blend_multiply(c1, c2);
    case ADD:      return blend_add_pin(c1, c2);
    case SUBTRACT: return blend_sub_pin(c1, c2);
    case LIGHTEST: return blend_lightest(c1, c2);
    case DARKEST:  return blend_darkest(c1, c2);
    case REPLACE:  return c2;
    }
    return 0;
  }


  /**
   * Copies and blends 1 pixel with MODE to pixel in this image.
   */
  public void blend(int sx, int sy, int dx, int dy, int mode) {
    if ((dx >= 0) && (dx < width) && (sx >= 0) && (sx < width) &&
        (dy >= 0) && (dy < height) && (sy >= 0) && (sy < height)) {
      pixels[dy * width + dx] =
        blend(pixels[dy * width + dx], pixels[sy * width + sx], mode);
    }
  }


  /**
   * Copies and blends 1 pixel with MODE to pixel in another image
   */
  public void blend(PImage src,
                    int sx, int sy, int dx, int dy, int mode) {
    if ((dx >= 0) && (dx < width) && (sx >= 0) && (sx < src.width) &&
        (dy >= 0) && (dy < height) && (sy >= 0) && (sy < src.height)) {
      pixels[dy * width + dx] =
        blend(pixels[dy * width + dx],
              src.pixels[sy * src.width + sx], mode);
    }
  }


  /**
   * Blends one area of this image to another area
   */
  public void blend(int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    blend(this, sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, mode);
  }


  /**
   * Copies area of one image into another PImage object
   */
  public void blend(PImage src,
                    int sx1, int sy1, int sx2, int sy2,
                    int dx1, int dy1, int dx2, int dy2, int mode) {
    if (imageMode == CORNER) {  // if CORNERS, do nothing
      sx2 += sx1; sy2 += sy1;
      dx2 += dx1; dy2 += dy1;

    //} else if (imageMode == CENTER) {
      //sx2 /= 2f; sy2 /= 2f;
      //dx2 /= 2f; dy2 /= 2f;
    }

    if ((src == this) &&
        intersect(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2)) {
      blit_resize(get(sx1, sy1, sx2 - sx1, sy2 - sy1),
                  0, 0, sx2 - sx1 - 1, sy2 - sy1 - 1,
                  pixels, width, height, dx1, dy1, dx2, dy2, mode);
    } else {
      blit_resize(src, sx1, sy1, sx2, sy2,
                  pixels, width, height, dx1, dy1, dx2, dy2, mode);
    }
  }


  /**
   * Check to see if two rectangles intersect one another
   */
  protected boolean intersect(int sx1, int sy1, int sx2, int sy2,
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

  // COPYING IMAGE DATA


  /**
   * Duplicate an image, returns new PImage object.
   * The pixels[] array for the new object will be unique
   * and recopied from the source image.
   */
  public Object clone() throws CloneNotSupportedException {  // ignore
    PImage c = (PImage) super.clone();

    // super.clone() will only copy the reference to the pixels
    // array, so this will do a proper duplication of it instead.
    c.pixels = new int[width * height];
    System.arraycopy(pixels, 0, c.pixels, 0, pixels.length);

    // return the goods
    return c;
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
    if (srcX2 >= img.width) srcX2 = img.width - 1;
    if (srcY2 >= img.height) srcY2 = img.height - 1;

    int srcW = srcX2 - srcX1;
    int srcH = srcY2 - srcY1;
    int destW = destX2 - destX1;
    int destH = destY2 - destY1;

    if (!smooth) {
      srcW++; srcH++;
    }

    if (destW <= 0 || destH <= 0 ||
        srcW <= 0 || srcH <= 0 ||
        destX1 >= screenW || destY1 >= screenH ||
        srcX1 >= img.width || srcY1 >= img.height) {
      return;
    }

    int dx = (int) (srcW / (float) destW * PRECISIONF);
    int dy = (int) (srcH / (float) destH * PRECISIONF);

    srcXOffset = (int) (destX1 < 0 ? -destX1 * dx : srcX1 * PRECISIONF);
    srcYOffset = (int) (destY1 < 0 ? -destY1 * dy : srcY1 * PRECISIONF);

    if (destX1 < 0) {
      destW += destX1;
      destX1 = 0;
    }
    if (destY1 < 0) {
      destH += destY1;
      destY1 = 0;
    }

    destW = low(destW, screenW - destX1);
    destH = low(destH, screenH - destY1);

    int destOffset = destY1 * screenW + destX1;
    srcBuffer = img.pixels;

    if (smooth) {
      // use bilinear filtering
      iw = img.width;
      iw1 = img.width - 1;
      ih1 = img.height - 1;

      switch (mode) {

      case BLEND:
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
      }

    } else {
      // nearest neighbour scaling (++fast!)
      switch (mode) {

      case BLEND:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.width;
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

      case ADD:
        for (int y = 0; y < destH; y++) {
          sX = srcXOffset;
          sY = (srcYOffset >> PRECISIONB) * img.width;
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
          sY = (srcYOffset >> PRECISIONB) * img.width;
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
          sY = (srcYOffset >> PRECISIONB) * img.width;
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
          sY = (srcYOffset >> PRECISIONB) * img.width;
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
          sY = (srcYOffset >> PRECISIONB) * img.width;
          for (int x = 0; x < destW; x++) {
            destPixels[destOffset + x] = srcBuffer[sY + (sX >> PRECISIONB)];
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
    ifV = PREC_MAXVAL - fracV;
    v1 = (srcYOffset >> PRECISIONB) * iw;
    v2 = low((srcYOffset >> PRECISIONB) + 1, ih1) * iw;
  }


  private int filter_bilinear() {
    fracU = sX & PREC_MAXVAL;
    ifU = PREC_MAXVAL - fracU;
    ul = (ifU * ifV) >> PRECISIONB;
    ll = (ifU * fracV) >> PRECISIONB;
    ur = (fracU * ifV) >> PRECISIONB;
    lr = (fracU * fracV) >> PRECISIONB;
    u1 = (sX >> PRECISIONB);
    u2 = low(u1 + 1, iw1);

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


  private static int low(int a, int b) {
    return (a < b) ? a : b;
  }


  private static int high(int a, int b) {
    return (a > b) ? a : b;
  }


  /**
   * returns the fractional portion of a number: frac(2.3) = .3;
   */
  private static float frac(float x) {
    return (x - (int) x);
  }


  /**
   * generic linear interpolation
   */
  private static int mix(int a, int b, int f) {
    return a + (((b - a) * f) >> 8);
  }



  /////////////////////////////////////////////////////////////

  // BLEND MODE IMPLEMENTIONS

  private static int blend_multiply(int a, int b) {
    int f = (b & ALPHA_MASK) >>> 24;

    return (low(((a & ALPHA_MASK) >>> 24) + f, 0xff) << 24 |
            mix(a & RED_MASK, b & RED_MASK, f) & RED_MASK |
            mix(a & GREEN_MASK, b & GREEN_MASK, f) & GREEN_MASK |
            mix(a & BLUE_MASK, b & BLUE_MASK, f));
  }


  /**
   * additive blend with clipping
   */
  private static int blend_add_pin(int a, int b) {
    int f = (b & ALPHA_MASK) >>> 24;

    return (low(((a & ALPHA_MASK) >>> 24) + f, 0xff) << 24 |
            low(((a & RED_MASK) +
                 ((b & RED_MASK) >> 8) * f), RED_MASK) & RED_MASK |
            low(((a & GREEN_MASK) +
                 ((b & GREEN_MASK) >> 8) * f), GREEN_MASK) & GREEN_MASK |
            low((a & BLUE_MASK) +
                (((b & BLUE_MASK) * f) >> 8), BLUE_MASK));
  }


  /**
   * subtractive blend with clipping
   */
  private static int blend_sub_pin(int a, int b) {
    int f = (b & ALPHA_MASK) >>> 24;

    return (low(((a & ALPHA_MASK) >>> 24) + f, 0xff) << 24 |
            high(((a & RED_MASK) - ((b & RED_MASK) >> 8) * f),
                 GREEN_MASK) & RED_MASK |
            high(((a & GREEN_MASK) - ((b & GREEN_MASK) >> 8) * f),
                 BLUE_MASK) & GREEN_MASK |
            high((a & BLUE_MASK) - (((b & BLUE_MASK) * f) >> 8), 0));
  }


  /**
   * only returns the blended lightest colour
   */
  private static int blend_lightest(int a, int b) {
    int f = (b & ALPHA_MASK) >>> 24;

    return (low(((a & ALPHA_MASK) >>> 24) + f, 0xff) << 24 |
            high(a & RED_MASK, ((b & RED_MASK) >> 8) * f) & RED_MASK |
            high(a & GREEN_MASK, ((b & GREEN_MASK) >> 8) * f) & GREEN_MASK |
            high(a & BLUE_MASK, ((b & BLUE_MASK) * f) >> 8));
  }


  /**
   * only returns the blended darkest colour
   */
  private static int blend_darkest(int a, int b) {
    int f = (b & ALPHA_MASK) >>> 24;

    return (low(((a & ALPHA_MASK) >>> 24) + f, 0xff) << 24 |
            mix(a & RED_MASK,
                low(a & RED_MASK,
                    ((b & RED_MASK) >> 8) * f), f) & RED_MASK |
            mix(a & GREEN_MASK,
                low(a & GREEN_MASK,
                    ((b & GREEN_MASK) >> 8) * f), f) & GREEN_MASK |
            mix(a & BLUE_MASK,
                low(a & BLUE_MASK,
                    ((b & BLUE_MASK) * f) >> 8), f));
  }



  //////////////////////////////////////////////////////////////

  // FILE I/O


  static byte tiff_header[] = {
    77, 77, 0, 42, 0, 0, 0, 8, 0, 9, 0, -2, 0, 4, 0, 0, 0, 1, 0, 0,
    0, 0, 1, 0, 0, 3, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 3, 0, 0, 0, 1,
    0, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 3, 0, 0, 0, 122, 1, 6, 0, 3, 0,
    0, 0, 1, 0, 2, 0, 0, 1, 17, 0, 4, 0, 0, 0, 1, 0, 0, 3, 0, 1, 21,
    0, 3, 0, 0, 0, 1, 0, 3, 0, 0, 1, 22, 0, 3, 0, 0, 0, 1, 0, 0, 0, 0,
    1, 23, 0, 4, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 8, 0, 8
  };


  static public boolean saveHeaderTIFF(OutputStream output,
                                       int width, int height) {
    try {
      byte tiff[] = new byte[768];
      System.arraycopy(tiff_header, 0, tiff, 0, tiff_header.length);

      tiff[30] = (byte) ((width >> 8) & 0xff);
      tiff[31] = (byte) ((width) & 0xff);
      tiff[42] = tiff[102] = (byte) ((height >> 8) & 0xff);
      tiff[43] = tiff[103] = (byte) ((height) & 0xff);

      int count = width*height*3;
      tiff[114] = (byte) ((count >> 24) & 0xff);
      tiff[115] = (byte) ((count >> 16) & 0xff);
      tiff[116] = (byte) ((count >> 8) & 0xff);
      tiff[117] = (byte) ((count) & 0xff);

      output.write(tiff);
      return true;

    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }


  static public boolean saveTIFF(OutputStream output, int pixels[],
                                 int width, int height) {
    try {
      if (!saveHeaderTIFF(output, width, height)) {
        return false;
      }
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
   * pixel buffer now using RLE compression.
   * </p>
   * Also figured out how to avoid parsing the image upside-down
   * (there's a header flag to set the image origin to top-left)
   * </p>
   * New version starting with rev 92 takes format setting into account:
   * <TT>ALPHA</TT> images written as 8bit grayscale (uses lowest byte)
   * <TT>RGB</TT> &rarr; 24 bits
   * <TT>ARGB</TT> &rarr; 32 bits
   * all versions are RLE compressed
   * </p>
   * Contributed by toxi 8-10 May 2005, based on this RLE
   * <A HREF="http://www.wotsit.org/download.asp?f=tga">specification</A>
   */
  static public boolean saveTGA(OutputStream output, int pixels[],
                                int width, int height, int format) {
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
     header[12] = (byte) (width & 0xff);
     header[13] = (byte) (width >> 8);
     header[14] = (byte) (height & 0xff);
     header[15] = (byte) (height >> 8);

     try {
       output.write(header);

       int maxLen = height * width;
       int index = 0;
       int col, prevCol;
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
   * Save this image to disk.
   * <p>
   * As of revision 0100, this function requires an absolute path,
   * in order to avoid confusion. To save inside the sketch folder,
   * use the function savePath() from PApplet, or use saveFrame() instead.
   * <p>
   * <EM>TODO</EM> write reflection code here to use java 1.4 imageio
   * methods for writing out images that might be much better.
   * won't want to use them in all cases.. how to determine? <BR>
   * boolean ImageIO.write(RenderedImage im, String formatName, File output)
   */
  public void save(String filename) {  // ignore
    boolean success = false;

    File file = new File(filename);
    if (!file.isAbsolute()) {
      System.err.println("PImage.save() requires an absolute path, " +
                         "you might need to use savePath().");
      return;
    }

    try {
      OutputStream os = null;

      if (filename.toLowerCase().endsWith(".tga")) {
        os = new BufferedOutputStream(new FileOutputStream(filename), 32768);
        success = saveTGA(os, pixels, width, height, format);

      } else {
        if (!filename.toLowerCase().endsWith(".tif") &&
            !filename.toLowerCase().endsWith(".tiff")) {
          // if no .tif extension, add it..
          filename += ".tif";
        }
        os = new BufferedOutputStream(new FileOutputStream(filename), 32768);
        success = saveTIFF(os, pixels, width, height);
      }
      os.flush();
      os.close();

    } catch (IOException e) {
      //System.err.println("Error while saving image.");
      e.printStackTrace();
      success = false;
    }
    if (!success) {
      throw new RuntimeException("Error while saving image.");
    }
  }
}

