/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

package processing.core;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;


public class PFont2 extends PFont {

  /**
   * This is the union of the Mac Roman and Windows ANSI 
   * character sets. ISO Latin 1 would be Unicode characters
   * 0x80 -> 0xFF, but in practice, it would seem that most
   * designers using P5 would rather have the characters
   * that they expect from their platform's fonts. 
   *
   * This is more of an interim solution until a much better
   * font solution can be determined. (i.e. create fonts on 
   * the fly from some sort of vector format). 
   *
   * Not that I expect that to happen.
   */
  static final char[] EXTRA_CHARS = {
    0x0080, 0x0081, 0x0082, 0x0083, 0x0084, 0x0085, 0x0086, 0x0087, 
    0x0088, 0x0089, 0x008A, 0x008B, 0x008C, 0x008D, 0x008E, 0x008F, 
    0x0090, 0x0091, 0x0092, 0x0093, 0x0094, 0x0095, 0x0096, 0x0097, 
    0x0098, 0x0099, 0x009A, 0x009B, 0x009C, 0x009D, 0x009E, 0x009F, 
    0x00A0, 0x00A1, 0x00A2, 0x00A3, 0x00A4, 0x00A5, 0x00A6, 0x00A7, 
    0x00A8, 0x00A9, 0x00AA, 0x00AB, 0x00AC, 0x00AD, 0x00AE, 0x00AF, 
    0x00B0, 0x00B1, 0x00B4, 0x00B5, 0x00B6, 0x00B7, 0x00B8, 0x00BA, 
    0x00BB, 0x00BF, 0x00C0, 0x00C1, 0x00C2, 0x00C3, 0x00C4, 0x00C5, 
    0x00C6, 0x00C7, 0x00C8, 0x00C9, 0x00CA, 0x00CB, 0x00CC, 0x00CD, 
    0x00CE, 0x00CF, 0x00D1, 0x00D2, 0x00D3, 0x00D4, 0x00D5, 0x00D6, 
    0x00D7, 0x00D8, 0x00D9, 0x00DA, 0x00DB, 0x00DC, 0x00DD, 0x00DF, 
    0x00E0, 0x00E1, 0x00E2, 0x00E3, 0x00E4, 0x00E5, 0x00E6, 0x00E7, 
    0x00E8, 0x00E9, 0x00EA, 0x00EB, 0x00EC, 0x00ED, 0x00EE, 0x00EF, 
    0x00F1, 0x00F2, 0x00F3, 0x00F4, 0x00F5, 0x00F6, 0x00F7, 0x00F8, 
    0x00F9, 0x00FA, 0x00FB, 0x00FC, 0x00FD, 0x00FF, 0x0102, 0x0103, 
    0x0104, 0x0105, 0x0106, 0x0107, 0x010C, 0x010D, 0x010E, 0x010F, 
    0x0110, 0x0111, 0x0118, 0x0119, 0x011A, 0x011B, 0x0131, 0x0139, 
    0x013A, 0x013D, 0x013E, 0x0141, 0x0142, 0x0143, 0x0144, 0x0147, 
    0x0148, 0x0150, 0x0151, 0x0152, 0x0153, 0x0154, 0x0155, 0x0158, 
    0x0159, 0x015A, 0x015B, 0x015E, 0x015F, 0x0160, 0x0161, 0x0162, 
    0x0163, 0x0164, 0x0165, 0x016E, 0x016F, 0x0170, 0x0171, 0x0178, 
    0x0179, 0x017A, 0x017B, 0x017C, 0x017D, 0x017E, 0x0192, 0x02C6, 
    0x02C7, 0x02D8, 0x02D9, 0x02DA, 0x02DB, 0x02DC, 0x02DD, 0x03A9, 
    0x03C0, 0x2013, 0x2014, 0x2018, 0x2019, 0x201A, 0x201C, 0x201D, 
    0x201E, 0x2020, 0x2021, 0x2022, 0x2026, 0x2030, 0x2039, 0x203A, 
    0x2044, 0x20AC, 0x2122, 0x2202, 0x2206, 0x220F, 0x2211, 0x221A, 
    0x221E, 0x222B, 0x2248, 0x2260, 0x2264, 0x2265, 0x25CA, 0xF8FF, 
    0xFB01, 0xFB02
  };

  static char[] charset;
  static {
    charset = new char[126-33+1 + EXTRA_CHARS.length];
    int index = 0;
    for (int i = 33; i <= 126; i++) {
      charset[index++] = (char)i;
    }
    for (int i = 0; i < EXTRA_CHARS.length; i++) {
      charset[index++] = EXTRA_CHARS[i];
    }
  };


  public PFont2(String name, int size) {
    this(new Font(name, Font.PLAIN, size), false, true);
  }

  public PFont2(String name, int size, boolean smooth) {
    this(new Font(name, Font.PLAIN, size), false, smooth);
  }

  public PFont2(Font font, boolean all, boolean smooth) {
    //int firstChar = 33;
    //int lastChar = 126;

    //this.charCount = lastChar - firstChar + 1;
    this.charCount = all ? 65536 : charset.length;
    this.mbox = font.getSize();

    fwidth = fheight = mbox;

    /*
    // size for image/texture is next power of 2 over font size
    iwidth = iheight = (int) 
      Math.pow(2, Math.ceil(Math.log(mbox) / Math.log(2)));

    iwidthf = iheightf = (float) iwidth;
    */

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

    int mbox3 = mbox * 3;

    BufferedImage playground = 
      new BufferedImage(mbox3, mbox3, BufferedImage.TYPE_INT_RGB);

    Graphics2D g = (Graphics2D) playground.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                       smooth ? 
                       RenderingHints.VALUE_ANTIALIAS_ON :
                       RenderingHints.VALUE_ANTIALIAS_OFF);

    g.setFont(font);
    FontMetrics metrics = g.getFontMetrics();

    //ascent = metrics.getAscent();
    //descent = metrics.getDescent();
    //System.out.println("descent found was " + descent);

    int maxWidthHeight = 0;
    int index = 0;
    for (int i = 0; i < charCount; i++) {
      char c = all ? (char)i : charset[i];
      if (!font.canDisplay(c)) {  // skip chars not in the font
        continue;
      }

      g.setColor(Color.white);
      g.fillRect(0, 0, mbox3, mbox3);
      g.setColor(Color.black);
      g.drawString(String.valueOf(c), mbox, mbox * 2);

      // grabs copy of the current data.. so no updates (do each time)
      Raster raster = playground.getData();

      //int w = metrics.charWidth(c);
      int minX = 1000, maxX = 0;
      int minY = 1000, maxY = 0;
      boolean pixelFound = false;

      for (int y = 0; y < mbox3; y++) {
        for (int x = 0; x < mbox3; x++) {
          int sample = raster.getSample(x, y, 0);  // maybe?
          // or int samples[] = raster.getPixel(x, y, null);

          //if (sample == 0) {  // or just not white? hmm
          if (sample != 255) {
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            pixelFound = true; 
            //System.out.println(x + " " + y + " = " + sample);
          }
        }
      }

      if (!pixelFound) {
        //System.out.println("no pixels found in char " + ((char)i));
        // this was dumb that it was set to 20 & 30, because for small
        // fonts, those guys don't exist
        minX = minY = 0; //20;
        maxX = maxY = 0; //30;

        // this will create a 1 pixel white (clear) character..
        // maybe better to set one to -1 so nothing is added?
      }

      value[index] = c;
      height[index] = (maxY - minY) + 1;
      width[index] = (maxX - minX) + 1;
      setWidth[index] = metrics.charWidth(c);
      //System.out.println((char)c + " " + setWidth[index]);

      // cache locations of the ascii charset
      //if (value[i] < 128) ascii[value[i]] = i;
      if (c < 128) ascii[c] = index;

      // offset from vertical location of baseline
      // of where the char was drawn (mbox*2)
      topExtent[index] = mbox*2 - minY;

      // offset from left of where coord was drawn
      leftExtent[index] = minX - mbox;

      if (c == 'd') {
        ascent = topExtent[index];
      }
      if (c == 'p') {
        descent = -topExtent[index] + height[index];
      }

      if (width[index] > maxWidthHeight) maxWidthHeight = width[index];
      if (height[index] > maxWidthHeight) maxWidthHeight = height[index];

      bitmaps[index] = new PImage(new int[width[index] * height[index]],
                                  width[index], height[index], ALPHA);

      for (int y = minY; y <= maxY; y++) {
        for (int x = minX; x <= maxX; x++) {
          int value = 255 - raster.getSample(x, y, 0);
          int pindex = (y - minY) * width[index] + (x - minX);
          bitmaps[index].pixels[pindex] = value;
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

    //images = bitmaps;
    //System.out.println("Mbox 2 is " + mbox2);
    images = new PImage[charCount];
    // copy from bitmaps into actual image pixels
    for (int i = 0; i < charCount; i++) {
      images[i] = new PImage(new int[mbox2*mbox2], mbox2, mbox2, ALPHA);
      for (int y = 0; y < height[i]; y++) {
        System.arraycopy(bitmaps[i].pixels, y*width[i],
                         images[i].pixels, y*mbox2, 
                         width[i]);
      }
      bitmaps[i] = null;
    }
  }
}
