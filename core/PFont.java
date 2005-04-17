/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-05 Ben Fry & Casey Reas
  Portions Copyright (c) 2001-04 Massachusetts Institute of Technology

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
import java.io.*;
import java.lang.reflect.*;
import java.util.*;


/**
 * Grayscale bitmap font class used by Processing.
 * <P>
 * Awful (and by that, I mean awesome) ascii (non)art for how this works:
 * <PRE>
 *   |
 *   |                   height is the full used height of the image
 *   |
 *   |   ..XX..       }
 *   |   ..XX..       }
 *   |   ......       }
 *   |   XXXX..       }  topExtent (top y is baseline - topExtent)
 *   |   ..XX..       }
 *   |   ..XX..       }  dotted areas are where the image data
 *   |   ..XX..       }  is actually located for the character
 *   +---XXXXXX----   }  (it extends to the right and down
 *   |                   for power of two texture sizes)
 *   ^^^^ leftExtent (amount to move over before drawing the image
 *
 *   ^^^^^^^^^^^^^^ setWidth (width displaced by char)
 * </PRE>
 */
public class PFont implements PConstants {

  public int charCount;
  public PImage images[];

  /**
   * Name of the font as seen by Java when it was created.
   * If the font is available, the native version will be used.
   */
  public String name;

  /**
   * Postscript name of the font that this bitmap was created from.
   */
  public String psname;

  /** "natural" size of the font (most often 48) */
  public int size;

  /** next power of 2 over the max image size (usually 64) */
  public int mbox2;

  /** floating point width (convenience) */
  protected float fwidth;

  /** floating point width (convenience) */
  protected float fheight;

  /** texture width, same as mbox2, but reserved for future use */
  public int twidth;

  /** texture height, same as mbox2, but reserved for future use */
  public int theight;

  public int value[];  // char code
  public int height[]; // height of the bitmap data
  public int width[];  // width of bitmap data
  public int setWidth[];  // width displaced by the char
  public int topExtent[];  // offset for the top
  public int leftExtent[];  // offset for the left

  public int ascent;
  public int descent;

  int ascii[];  // quick lookup for the ascii chars

  // shared by the text() functions to avoid incessant allocation of memory
  protected char textBuffer[] = new char[8 * 1024];
  protected char widthBuffer[] = new char[8 * 1024];


  public PFont() { }  // for subclasses


  public PFont(InputStream input) throws IOException {
    DataInputStream is = new DataInputStream(input);

    // number of character images stored in this font
    charCount = is.readInt();

    // bit count is ignored since this is always 8
    //int numBits = is.readInt();
    // used to be the bitCount, but now used for version number.
    // version 8 is any font before 69, so 9 is anything from 83+
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

    images = new PImage[charCount];
    for (int i = 0; i < charCount; i++) {
      int pixels[] = new int[twidth * theight];
      images[i] = new PImage(pixels, twidth, theight, ALPHA);
      int bitmapSize = height[i] * width[i];

      byte temp[] = new byte[bitmapSize];
      is.readFully(temp);

      // convert the bitmap to an alpha channel
      int w = width[i];
      int h = height[i];
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          int valu = temp[y*w + x] & 0xff;
          images[i].pixels[y * twidth + x] = valu;
          //(valu << 24) | 0xFFFFFF;  // windows
          //0xFFFFFF00 | valu;  // macosx

          //System.out.print((images[i].pixels[y*64+x] > 128) ? "*" : ".");
        }
        //System.out.println();
      }
      //System.out.println();
    }

    if (version == 9) {  // includes the font name at the end of the file
      name = is.readUTF();
      psname = is.readUTF();
    }
  }


  /**
   * Write this PFont to an OutputStream. It is assumed that the
   * calling class will handle closing the stream when finished.
   */
  public void write(OutputStream output) throws IOException {
    DataOutputStream os = new DataOutputStream(output);

    os.writeInt(charCount);

    // formerly numBits, now used for version number
    os.writeInt((name != null) ? 9 : 8);

    os.writeInt(size);    // formerly mboxX (was 64, now 48)
    os.writeInt(mbox2);   // formerly mboxY (was 64, still 64)
    os.writeInt(ascent);  // formerly baseHt (was ignored)
    os.writeInt(descent); // formerly struct padding for c version

    for (int i = 0; i < charCount; i++) {
      os.writeInt(value[i]);
      os.writeInt(height[i]);
      os.writeInt(width[i]);
      os.writeInt(setWidth[i]);
      os.writeInt(topExtent[i]);
      os.writeInt(leftExtent[i]);
      os.writeInt(0); // padding
    }

    for (int i = 0; i < charCount; i++) {
      for (int y = 0; y < height[i]; y++) {
        for (int x = 0; x < width[i]; x++) {
          os.write(images[i].pixels[y * mbox2 + x] & 0xff);
        }
      }
    }

    if (name != null) {  // version 9
      os.writeUTF(name);
    }

    os.flush();
  }


  /**
   * Get index for the char (convert from unicode to bagel charset).
   * @return index into arrays or -1 if not found
   */
  public int index(char c) {
    // degenerate case, but the find function will have trouble
    // if there are somehow zero chars in the lookup
    if (value.length == 0) return -1;

    // quicker lookup for the ascii fellers
    if (c < 128) return ascii[c];

    // some other unicode char, hunt it out
    return index_hunt(c, 0, value.length-1);
  }


  protected int index_hunt(int c, int start, int stop) {
    int pivot = (start + stop) / 2;

    // if this is the char, then return it
    if (c == value[pivot]) return pivot;

    // char doesn't exist, otherwise would have been the pivot
    //if (start == stop) return -1;
    if (start >= stop) return -1;

    // if it's in the lower half, continue searching that
    if (c < value[pivot]) return index_hunt(c, start, pivot-1);

    // if it's in the upper half, continue there
    return index_hunt(c, pivot+1, stop);
  }


  /**
   * Currently un-implemented for .vlw fonts,
   * but honored for layout in case subclasses use it.
   */
  public float kern(char a, char b) {
    return 0;
  }


  /**
   * Returns the ascent of this font from the baseline.
   * The value is based on a font of size 1.
   */
  public float ascent() {
    return ((float)ascent / fheight);
  }


  /**
   * Returns how far this font descends from the baseline.
   * The value is based on a font size of 1.
   */
  public float descent() {
    return ((float)descent / fheight);
  }


  /**
   * Width of this character for a font of size 1.
   */
  public float width(char c) {
    if (c == 32) return width('i');

    int cc = index(c);
    if (cc == -1) return 0;

    return ((float)setWidth[cc] / fwidth);
  }


  /**
   * Return the width of a line of text of size 1. If the text has
   * multiple lines, this returns the length of the longest line.
   */
  public float width(String str) {
    int length = str.length();
    if (length > widthBuffer.length) {
      widthBuffer = new char[length + 10];
    }
    str.getChars(0, length, widthBuffer, 0);

    float wide = 0;
    int index = 0;
    int start = 0;

    while (index < length) {
      if (widthBuffer[index] == '\n') {
        wide = Math.max(wide, calcWidth(widthBuffer, start, index));
        start = index+1;
      }
      index++;
    }
    if (start < length) {
      wide = Math.max(wide, calcWidth(widthBuffer, start, index));
    }
    return wide;
  }


  private float calcWidth(char buffer[], int start, int stop) {
    float wide = 0;
    for (int i = start; i < stop; i++) {
      wide += width(buffer[i]);
    }
    return wide;
  }


  /**
   * Draw a character at an x, y position.
   */
  public void text(char c, float x, float y, PGraphics parent) {
    text(c, x, y, 0, parent);
  }


  /**
   * Draw a character at an x, y, z position.
   */
  public void text(char c, float x, float y, float z, PGraphics parent) {
    if (parent.textAlign == CENTER) {
      x -= parent.textSize * width(c) / 2f;

    } else if (parent.textAlign == RIGHT) {
      x -= parent.textSize * width(c);
    }

    //textImpl(c, x, y, z, parent);
    parent.textImpl(c, x, y, z);
  }


  /**
   * Internal function to draw a character at an x, y, z position.
   * This version is called after the textM
   */
  /*
  protected void textImpl(char c, float x, float y, float z,
                          PGraphics parent) {
    int glyph = index(c);
    if (glyph == -1) return;

    if (parent.textMode == MODEL) {
      float high    = (float) height[glyph]     / fheight;
      float bwidth  = (float) width[glyph]      / fwidth;
      float lextent = (float) leftExtent[glyph] / fwidth;
      float textent = (float) topExtent[glyph]  / fheight;

      float x1 = x + lextent * parent.textSize;
      float y1 = y - textent * parent.textSize;
      float x2 = x1 + bwidth * parent.textSize;
      float y2 = y1 + high * parent.textSize;

      boolean savedTint = parent.tint;
      int savedTintColor = parent.tintColor;
      float savedTintR = parent.tintR;
      float savedTintG = parent.tintG;
      float savedTintB = parent.tintB;
      float savedTintA = parent.tintA;
      boolean savedTintAlpha = parent.tintAlpha;

      parent.tint = true;
      parent.tintColor = parent.fillColor;
      parent.tintR = parent.fillR;
      parent.tintG = parent.fillG;
      parent.tintB = parent.fillB;
      parent.tintA = parent.fillA;
      parent.tintAlpha = parent.fillAlpha;

      parent.imageImpl(images[glyph],
                       x1, y1, x2, y2, //x2-x1, y2-y1,
                       0, 0, width[glyph], height[glyph]);

      parent.tint = savedTint;
      parent.tintColor = savedTintColor;
      parent.tintR = savedTintR;
      parent.tintG = savedTintG;
      parent.tintB = savedTintB;
      parent.tintA = savedTintA;
      parent.tintAlpha = savedTintAlpha;

    } else {  // textMode SCREEN
      int xx = (int) x + leftExtent[glyph];;
      int yy = (int) y - topExtent[glyph];

      int x0 = 0;
      int y0 = 0;
      int w0 = width[glyph];
      int h0 = height[glyph];

      if ((xx >= parent.width) || (yy >= parent.height) ||
          (xx + w0 < 0) || (yy + h0 < 0)) return;

      if (xx < 0) {
        x0 -= xx;
        w0 += xx;
        xx = 0;
      }
      if (yy < 0) {
        y0 -= yy;
        h0 += yy;
        yy = 0;
      }
      if (xx + w0 > parent.width) {
        w0 -= ((xx + w0) - parent.width);
      }
      if (yy + h0 > parent.height) {
        h0 -= ((yy + h0) - parent.height);
      }

      int fr = parent.fillRi;
      int fg = parent.fillGi;
      int fb = parent.fillBi;
      int fa = parent.fillAi;

      int pixels1[] = images[glyph].pixels;
      int pixels2[] = parent.pixels;

      for (int row = y0; row < y0 + h0; row++) {
        for (int col = x0; col < x0 + w0; col++) {
          int a1 = (fa * pixels1[row * twidth + col]) >> 8;
          int a2 = a1 ^ 0xff;
          int p1 = pixels1[row * width[glyph] + col];
          int p2 = pixels2[(yy + row-y0)*parent.width + (xx+col-x0)];

          pixels2[(yy + row-y0)*parent.width + xx+col-x0] =
            (0xff000000 |
             (((a1 * fr + a2 * ((p2 >> 16) & 0xff)) & 0xff00) << 8) |
             (( a1 * fg + a2 * ((p2 >>  8) & 0xff)) & 0xff00) |
             (( a1 * fb + a2 * ( p2        & 0xff)) >> 8));
        }
      }
    }
  }
  */


  public void text(String str, float x, float y, PGraphics parent) {
    text(str, x, y, 0, parent);
  }


  public void text(String str, float x, float y, float z, PGraphics parent) {
    int length = str.length();
    if (length > textBuffer.length) {
      textBuffer = new char[length + 10];
    }
    str.getChars(0, length, textBuffer, 0);

    int start = 0;
    int index = 0;
    while (index < length) {
      if (textBuffer[index] == '\n') {
        textLine(start, index, x, y, z, parent);
        start = index + 1;
        y += parent.textLeading;
      }
      index++;
    }
    if (start < length) {
      textLine(start, index, x, y, z, parent);
    }
  }


  protected void textLine(int start, int stop,
                          float x, float y, float z,
                          PGraphics parent) {
    if (parent.textAlign == CENTER) {
      x -= parent.textSize * calcWidth(textBuffer, start, stop) / 2f;

    } else if (parent.textAlign == RIGHT) {
      x -= parent.textSize * calcWidth(textBuffer, start, stop);
    }

    for (int index = start; index < stop; index++) {
      //textImpl(textBuffer[index], x, y, z, parent);
      parent.textImpl(textBuffer[index], x, y, z);
      x += parent.textSize *width(textBuffer[index]);
    }
  }


  /**
   * Same as below, just without a z coordinate.
   */
  public void text(String str, float x, float y,
                   float c, float d, PGraphics parent) {
    text(str, x, y, c, d, 0, parent);
  }


  /**
   * Draw text in a box that is constrained to a particular size.
   * <P>
   * The parent PApplet will have converted the coords based on
   * the current rectMode().
   * <P>
   * Note that the x,y coords of the start of the box
   * will align with the *ascent* of the text, not the baseline,
   * as is the case for the other text() functions.
   */
  public void text(String str, float boxX1, float boxY1,
                   float boxX2, float boxY2, float boxZ, PGraphics parent) {
    float spaceWidth = width(' ') * parent.textSize;
    float runningX = boxX1;
    float currentY = boxY1;
    float boxWidth = boxX2 - boxX1;

    float lineX = boxX1;
    if (parent.textAlign == CENTER) {
      lineX = lineX + boxWidth/2f;
    } else if (parent.textAlign == RIGHT) {
      lineX = boxX2;
    }

    // ala illustrator, the text itself must fit inside the box
    currentY += ascent() * parent.textSize;
    // if the box is already too small, tell em to f off
    if (currentY > boxY2) return;

    int length = str.length();
    if (length > textBuffer.length) {
      textBuffer = new char[length + 10];
    }
    str.getChars(0, length, textBuffer, 0);

    int wordStart = 0;
    int wordStop = 0;
    int lineStart = 0;
    int index = 0;
    while (index < length) {
      if ((textBuffer[index] == ' ') || (index == length-1)) {
        // boundary of a word
        float wordWidth = parent.textSize *
          calcWidth(textBuffer, wordStart, index);

        if (runningX + wordWidth > boxX2) {
          if (runningX == boxX1) {
            // if this is the first word, and its width is
            // greater than the width of the text box,
            // then break the word where at the max width,
            // and send the rest of the word to the next line.
            do {
              index--;
              if (index == wordStart) {
                // not a single char will fit on this line. screw 'em.
                //System.out.println("screw you");
                return;
              }
              wordWidth = parent.textSize *
                calcWidth(textBuffer, wordStart, index);
            } while (wordWidth > boxWidth);
            textLine(lineStart, index, lineX, currentY, boxZ, parent);

          } else {
            // next word is too big, output current line
            // and advance to the next line
            textLine(lineStart, wordStop, lineX, currentY, boxZ, parent);
            // only increment index if a word wasn't broken inside the
            // do/while loop above.. also, this is a while() loop too,
            // because multiple spaces don't count for shit when they're
            // at the end of a line like this.

            index = wordStop;  // back that ass up
            while ((index < length) &&
                   (textBuffer[index] == ' ')) {
              index++;
            }
          }
          lineStart = index;
          wordStart = index;
          wordStop = index;
          runningX = boxX1;
          currentY += parent.textLeading;
          if (currentY > boxY2) return;  // box is now full

        } else {
          runningX += wordWidth + spaceWidth;
          // on to the next word
          wordStop = index;
          wordStart = index + 1;
        }

      } else if (textBuffer[index] == '\n') {
        if (lineStart != index) {  // if line is not empty
          textLine(lineStart, index, lineX, currentY, boxZ, parent);
        }
        lineStart = index + 1;
        wordStart = lineStart;
        currentY += parent.textLeading;
        if (currentY > boxY2) return;  // box is now full
      }
      index++;
    }
    if ((lineStart < length) && (lineStart != index)) {
      textLine(lineStart, index, lineX, currentY, boxZ, parent);
    }
  }


  //////////////////////////////////////////////////////////////


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


  /**
   * The default Processing character set.
   * <P>
   * This is the union of the Mac Roman and Windows ANSI
   * character sets. ISO Latin 1 would be Unicode characters
   * 0x80 -> 0xFF, but in practice, it would seem that most
   * designers using P5 would rather have the characters
   * that they expect from their platform's fonts.
   * <P>
   * This is more of an interim solution until a much better
   * font solution can be determined. (i.e. create fonts on
   * the fly from some sort of vector format).
   * <P>
   * Not that I expect that to happen.
   */
  static public char[] DEFAULT_CHARSET;
  static {
    DEFAULT_CHARSET = new char[126-33+1 + EXTRA_CHARS.length];
    int index = 0;
    for (int i = 33; i <= 126; i++) {
      DEFAULT_CHARSET[index++] = (char)i;
    }
    for (int i = 0; i < EXTRA_CHARS.length; i++) {
      DEFAULT_CHARSET[index++] = EXTRA_CHARS[i];
    }
  };


  /**
   * Create a new .vlw font on the fly. See documentation with
   * the later version of this constructor.
   */
  /*
  public PFont(String name, int fontsize) {
    this(new Font(name, Font.PLAIN, fontsize), false, true);
  }
  */


  /**
   * Create a new .vlw font on the fly. See documentation with
   * the later version of this constructor.
   */
  /*
  public PFont(String name, int fontsize, boolean smooth) {
    this(new Font(name, Font.PLAIN, fontsize), false, smooth);
  }
  */


  /**
   * Use reflection to create a new .vlw font on the fly.
   * This only works with Java 1.3 and higher.
   *
   * @param font the font object to create from
   * @param all true to include all available characters in the font
   * @param smooth true to enable smoothing/anti-aliasing
   */
  public PFont(Font font, char charset[], boolean smooth) {
    if (PApplet.javaVersion < 1.3f) {
      throw new RuntimeException("Can only create fonts with " +
                                 "Java 1.3 or higher");
    }

    name = font.getName();
    psname = font.getPSName();

    try {
      // the count gets reset later based on how many of
      // the chars are actually found inside the font.
      this.charCount = (charset == null) ? 65536 : charset.length;
      this.size = font.getSize();

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

      /*
        BufferedImage playground =
          new BufferedImage(mbox3, mbox3, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = (Graphics2D) playground.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           smooth ?
                           RenderingHints.VALUE_ANTIALIAS_ON :
                           RenderingHints.VALUE_ANTIALIAS_OFF);
      */

      Class bufferedImageClass =
        Class.forName("java.awt.image.BufferedImage");
      Constructor bufferedImageConstructor =
        bufferedImageClass.getConstructor(new Class[] {
          Integer.TYPE,
          Integer.TYPE,
          Integer.TYPE });
      Field typeIntRgbField = bufferedImageClass.getField("TYPE_INT_RGB");
      int typeIntRgb = typeIntRgbField.getInt(typeIntRgbField);
      Object playground =
        bufferedImageConstructor.newInstance(new Object[] {
          new Integer(mbox3),
          new Integer(mbox3),
          new Integer(typeIntRgb) });

      Class graphicsClass =
        Class.forName("java.awt.Graphics2D");
      Method getGraphicsMethod =
        bufferedImageClass.getMethod("getGraphics", new Class[] { });
      //Object g = getGraphicsMethod.invoke(playground, new Object[] { });
      Graphics g = (Graphics)
        getGraphicsMethod.invoke(playground, new Object[] { });

      Class renderingHintsClass =
        Class.forName("java.awt.RenderingHints");
      Class renderingHintsKeyClass =
        Class.forName("java.awt.RenderingHints$Key");
      //PApplet.printarr(renderingHintsClass.getFields());

      Field antialiasingKeyField =
        renderingHintsClass.getDeclaredField("KEY_TEXT_ANTIALIASING");
      Object antialiasingKey =
        antialiasingKeyField.get(renderingHintsClass);

      Field antialiasField = smooth ?
        renderingHintsClass.getField("VALUE_TEXT_ANTIALIAS_ON") :
        renderingHintsClass.getField("VALUE_TEXT_ANTIALIAS_OFF");
      Object antialiasState =
        antialiasField.get(renderingHintsClass);

      Method setRenderingHintMethod =
        graphicsClass.getMethod("setRenderingHint",
                                new Class[] { renderingHintsKeyClass,
                                              Object.class });
      setRenderingHintMethod.invoke(g, new Object[] {
        antialiasingKey,
        antialiasState
      });

      g.setFont(font);
      FontMetrics metrics = g.getFontMetrics();

      Method canDisplayMethod = null;
      Method getDataMethod = null;
      Method getSamplesMethod = null;

      int samples[] = new int[mbox3 * mbox3];

      canDisplayMethod =
        Font.class.getMethod("canDisplay", new Class[] { Character.TYPE });
      getDataMethod =
        bufferedImageClass.getMethod("getData", new Class[] { });
      Class rasterClass = Class.forName("java.awt.image.Raster");
      getSamplesMethod = rasterClass.getMethod("getSamples", new Class[] {
        Integer.TYPE,
        Integer.TYPE,
        Integer.TYPE,
        Integer.TYPE,
        Integer.TYPE,
        // integer array type?
        //Array.class
        samples.getClass()
      });

      //} catch (Exception e) {
      //e.printStackTrace();
      //return;
      //}

    //Array samples = Array.newInstance(Integer.TYPE, mbox3*mbox3);

    int maxWidthHeight = 0;
    int index = 0;
    for (int i = 0; i < charCount; i++) {
      char c = (charset == null) ? (char)i : charset[i];

      //if (!font.canDisplay(c)) {  // skip chars not in the font
      try {
        Character ch = new Character(c);
        Boolean canDisplay = (Boolean)
          canDisplayMethod.invoke(font, new Object[] { ch });
        if (canDisplay.booleanValue() == false) {
          continue;
        }
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }

      g.setColor(Color.white);
      g.fillRect(0, 0, mbox3, mbox3);
      g.setColor(Color.black);
      g.drawString(String.valueOf(c), size, size * 2);

      // grabs copy of the current data.. so no updates (do each time)
      /*
      Raster raster = playground.getData();
      raster.getSamples(0, 0, mbox3, mbox3, 0, samples);
      */

      Object raster = getDataMethod.invoke(playground, new Object[] {});
      getSamplesMethod.invoke(raster, new Object[] {
        new Integer(0),
        new Integer(0),
        new Integer(mbox3),
        new Integer(mbox3),
        new Integer(0),
        samples
      });

      //int w = metrics.charWidth(c);
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

      bitmaps[index] = new PImage(new int[width[index] * height[index]],
                                  width[index], height[index], ALPHA);

      for (int y = minY; y <= maxY; y++) {
        for (int x = minX; x <= maxX; x++) {
          int val = 255 - (samples[y * mbox3 + x] & 0xff);
          int pindex = (y - minY) * width[index] + (x - minX);
          bitmaps[index].pixels[pindex] = val;
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
    images = new PImage[charCount];
    for (int i = 0; i < charCount; i++) {
      images[i] = new PImage(new int[mbox2*mbox2], mbox2, mbox2, ALPHA);
      for (int y = 0; y < height[i]; y++) {
        System.arraycopy(bitmaps[i].pixels, y*width[i],
                         images[i].pixels, y*mbox2,
                         width[i]);
      }
      bitmaps[i] = null;
    }

    } catch (Exception e) {  // catch-all for reflection stuff
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }


  /**
   * Get a list of the fonts installed on the system.
   * <P>
   * Not recommended for use in applets, but this is implemented
   * in PFont because the Java methods to access this information
   * have changed between 1.1 and 1.4, and the 1.4 method is
   * typical of the sort of undergraduate-level over-abstraction
   * that the seems to have made its way into the Java API after 1.1.
   */
  static public String[] list() {
    if (PApplet.javaVersion < 1.3f) {
      return Toolkit.getDefaultToolkit().getFontList();
    }

    // getFontList is deprecated in 1.4, so this has to be used
    try {
      //GraphicsEnvironment ge =
      //  GraphicsEnvironment.getLocalGraphicsEnvironment();
      Class geClass = Class.forName("java.awt.GraphicsEnvironment");
      Method glgeMethod =
        geClass.getMethod("getLocalGraphicsEnvironment", null);
      Object ge = glgeMethod.invoke(null, null);

      Method gafMethod = geClass.getMethod("getAllFonts", null);
      Font fonts[] = (Font[]) gafMethod.invoke(ge, null); //ge.getAllFonts();
      String list[] = new String[fonts.length];
      for (int i = 0; i < list.length; i++) {
        list[i] = fonts[i].getName();
      }
      return list;

    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Error inside PFont.list()");
    }
  }
}
