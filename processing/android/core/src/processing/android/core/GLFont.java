/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-07 Ben Fry & Casey Reas
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

package processing.android.core;

import java.io.*;
import java.util.Arrays;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;


public class GLFont extends PFont {
  public GLFont(PApplet parent, InputStream input) throws IOException {
    DataInputStream is = new DataInputStream(input);

    // number of character images stored in this font
    charCount = is.readInt();

    // bit count is ignored since this is always 8
    //int numBits = is.readInt();
    // used to be the bitCount, but now used for version number.
    // version 8 is any font before 69, so 9 is anything from 83+
    // 9 was buggy so gonna increment to 10.
    int version = is.readInt();

    // this was formerly ignored, now it's the actual font size
    //mbox = is.readInt();
    size = is.readInt();
    // this was formerly mboxY, the one that was used
    // this will make new fonts downward compatible
    //mbox2 = is.readInt();
    mbox2 = is.readInt();

    fwidth = size; //mbox;
    fheight = size; //mbox;

    // size for image ("texture") is next power of 2
    // over the font size. for most vlw fonts, the size is 48
    // so the next power of 2 is 64.
    // double-check to make sure that mbox2 is a power of 2
    // there was a bug in the old font generator that broke this
    //mbox2 = (int) Math.pow(2, Math.ceil(Math.log(mbox2) / Math.log(2)));
    mbox2 = (int) Math.pow(2, Math.ceil(Math.log(mbox2) / Math.log(2)));
    // size for the texture is stored in the font
    twidth = theight = mbox2; //mbox2;

    ascent  = is.readInt();  // formerly baseHt (zero/ignored)
    descent = is.readInt();  // formerly ignored struct padding

    // allocate enough space for the character info
    value       = new int[charCount];
    height      = new int[charCount];
    width       = new int[charCount];
    setWidth    = new int[charCount];
    topExtent   = new int[charCount];
    leftExtent  = new int[charCount];

    ascii = new int[128];
    for (int i = 0; i < 128; i++) ascii[i] = -1;

    // read the information about the individual characters
    for (int i = 0; i < charCount; i++) {
      value[i]      = is.readInt();
      height[i]     = is.readInt();
      width[i]      = is.readInt();
      setWidth[i]   = is.readInt();
      topExtent[i]  = is.readInt();
      leftExtent[i] = is.readInt();

      // pointer in the c version, ignored
      is.readInt();

      // cache locations of the ascii charset
      if (value[i] < 128) ascii[value[i]] = i;

      // the values for getAscent() and getDescent() from FontMetrics
      // seem to be way too large.. perhaps they're the max?
      // as such, use a more traditional marker for ascent/descent
      if (value[i] == 'd') {
        if (ascent == 0) ascent = topExtent[i];
      }
      if (value[i] == 'p') {
        if (descent == 0) descent = -topExtent[i] + height[i];
      }
    }

    // not a roman font, so throw an error and ask to re-build.
    // that way can avoid a bunch of error checking hacks in here.
    if ((ascent == 0) && (descent == 0)) {
      throw new RuntimeException("Please use \"Create Font\" to " +
                                 "re-create this font.");
    }

    images = new GLTexture[charCount];
    GLTexture tex;
    GLTexture.Parameters params = GLTexture.newParameters(ARGB);
    for (int i = 0; i < charCount; i++) {
      tex = new GLTexture(parent, twidth, theight, params);
      tex.loadPixels();
      int bitmapSize = height[i] * width[i];

      byte temp[] = new byte[bitmapSize];
      is.readFully(temp);

      // convert the bitmap to an alpha channel
      int w = width[i];
      int h = height[i];
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          int valu = temp[y*w + x] & 0xff;
          tex.pixels[y * twidth + x] =  valu << 24 | 255 << 16 | 255 << 8 | 255;
          //(valu << 24) | 0xFFFFFF;  // windows
          //0xFFFFFF00 | valu;  // macosx

          //System.out.print((images[i].pixels[y*64+x] > 128) ? "*" : ".");
        }
        //System.out.println();
      }
      tex.loadTexture();
      images[i] = tex;
      //System.out.println();
    }

    if (version >= 10) {  // includes the font name at the end of the file
      name = is.readUTF();
      psname = is.readUTF();
    }
    if (version == 11) {
      smooth = is.readBoolean();
    }
  }

  /**
   * Create a new image-based font on the fly.
   *
   * @param font the font object to create from
   * @param charset array of all unicode chars that should be included
   * @param smooth true to enable smoothing/anti-aliasing
   */
  public GLFont(PApplet parent, Typeface font, int size, boolean smooth, char charset[]) {
    // save this so that we can use the native version
    this.font = font;
    this.smooth = smooth;

//    name = font.getName();
//    psname = font.getPSName();

    // fix regression from sorting (bug #564)
    if (charset != null) {
      // charset needs to be sorted to make index lookup run more quickly
      // http://dev.processing.org/bugs/show_bug.cgi?id=494
      Arrays.sort(charset);
    }

    // the count gets reset later based on how many of
    // the chars are actually found inside the font.
    this.charCount = (charset == null) ? 65536 : charset.length;
    this.size = size;

    fwidth = fheight = size;

    PImage bitmaps[] = new PImage[charCount];

    // allocate enough space for the character info
    value       = new int[charCount];
    height      = new int[charCount];
    width       = new int[charCount];
    setWidth    = new int[charCount];
    topExtent   = new int[charCount];
    leftExtent  = new int[charCount];

    ascii = new int[128];
    for (int i = 0; i < 128; i++) ascii[i] = -1;

    int mbox3 = size * 3;

//    BufferedImage playground =
//      new BufferedImage(mbox3, mbox3, BufferedImage.TYPE_INT_RGB);
    Bitmap playground = Bitmap.createBitmap(mbox3, mbox3, Config.ARGB_8888);
//    Graphics2D g = (Graphics2D) playground.getGraphics();
    Canvas canvas = new Canvas(playground); 
//    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                       smooth ?
//                       RenderingHints.VALUE_ANTIALIAS_ON :
//                       RenderingHints.VALUE_ANTIALIAS_OFF);
    Paint paint = new Paint();
    paint.setAntiAlias(smooth);

//    g.setFont(font);
//    FontMetrics metrics = g.getFontMetrics();
    paint.setTypeface(font);
    paint.setTextSize(size);
    
    int samples[] = new int[mbox3 * mbox3];

    int maxWidthHeight = 0;
    int index = 0;
    for (int i = 0; i < charCount; i++) {
      char c = (charset == null) ? (char)i : charset[i];

//      if (!font.canDisplay(c)) {  // skip chars not in the font
//        continue;
//      }

//      g.setColor(Color.white);
//    g.fillRect(0, 0, mbox3, mbox3);
      canvas.drawColor(Color.WHITE);
//      g.setColor(Color.black);
      paint.setColor(Color.BLACK);
//      g.drawString(String.valueOf(c), size, size * 2);
      canvas.drawText(String.valueOf(c), size, size * 2, paint);

      // grabs copy of the current data.. so no updates (do each time)
//      Raster raster = playground.getData();
//      raster.getSamples(0, 0, mbox3, mbox3, 0, samples);
      playground.getPixels(samples, 0, mbox3, 0, 0, mbox3, mbox3);

      int minX = 1000, maxX = 0;
      int minY = 1000, maxY = 0;
      boolean pixelFound = false;

      for (int y = 0; y < mbox3; y++) {
        for (int x = 0; x < mbox3; x++) {
          //int sample = raster.getSample(x, y, 0);  // maybe?
          int sample = samples[y * mbox3 + x] & 0xff;
          // or int samples[] = raster.getPixel(x, y, null);

          //if (sample == 0) {  // or just not white? hmm
          if (sample != 255) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            pixelFound = true;
          }
        }
      }

      if (!pixelFound) {
        minX = minY = 0;
        maxX = maxY = 0;
        // this will create a 1 pixel white (clear) character..
        // maybe better to set one to -1 so nothing is added?
      }

      value[index] = c;
      height[index] = (maxY - minY) + 1;
      width[index] = (maxX - minX) + 1;
//      setWidth[index] = metrics.charWidth(c);
      setWidth[index] = (int) paint.measureText(String.valueOf(c));
      //System.out.println((char)c + " " + setWidth[index]);

      // cache locations of the ascii charset
      //if (value[i] < 128) ascii[value[i]] = i;
      if (c < 128) ascii[c] = index;

      // offset from vertical location of baseline
      // of where the char was drawn (size*2)
      topExtent[index] = size*2 - minY;

      // offset from left of where coord was drawn
      leftExtent[index] = minX - size;

      if (c == 'd') {
        ascent = topExtent[index];
      }
      if (c == 'p') {
        descent = -topExtent[index] + height[index];
      }

      if (width[index] > maxWidthHeight) maxWidthHeight = width[index];
      if (height[index] > maxWidthHeight) maxWidthHeight = height[index];

      bitmaps[index] = new PImage(width[index], height[index], ARGB);

      for (int y = minY; y <= maxY; y++) {
        for (int x = minX; x <= maxX; x++) {
          int val = 255 - (samples[y * mbox3 + x] & 0xff);
          int pindex = (y - minY) * width[index] + (x - minX);
          bitmaps[index].pixels[pindex] = val << 24 | 255 << 16 | 255 << 8 | 255;
        }
      }
      index++;
    }
    charCount = index;

    // foreign font, so just make ascent the max topExtent
    if ((ascent == 0) && (descent == 0)) {
      for (int i = 0; i < charCount; i++) {
        char cc = (char) value[i];
        if (Character.isWhitespace(cc) ||
            (cc == '\u00A0') || (cc == '\u2007') || (cc == '\u202F')) {
          continue;
        }
        if (topExtent[i] > ascent) {
          ascent = topExtent[i];
        }
        int d = -topExtent[i] + height[i];
        if (d > descent) {
          descent = d;
        }
      }
    }
    // size for image/texture is next power of 2 over largest char
    mbox2 = (int)
      Math.pow(2, Math.ceil(Math.log(maxWidthHeight) / Math.log(2)));
    twidth = theight = mbox2;

    // shove the smaller PImage data into textures of next-power-of-2 size,
    // so that this font can be used immediately by p5.
    images = new GLTexture[charCount];
    GLTexture tex;
    GLTexture.Parameters params = GLTexture.newParameters(ARGB);
    for (int i = 0; i < charCount; i++) {
      tex = new GLTexture(parent, mbox2, mbox2, params);
      tex.loadPixels();
      for (int y = 0; y < height[i]; y++) {
        System.arraycopy(bitmaps[i].pixels, y*width[i],
                         tex.pixels, y*mbox2,
                         width[i]);
      }
      tex.loadTexture();
      images[i] = tex;
      bitmaps[i] = null;
    }
  }

}
