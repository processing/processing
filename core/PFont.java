/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PFont - font object for text rendering
  Part of the Processing project - http://processing.org

  Copyright (c) 2004 Ben Fry & Casey Reas
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


/*
  awful ascii (non)art for how this works
  |
  |                   height is the full used height of the image
  |
  |   ..XX..       }
  |   ..XX..       }
  |   ......       }
  |   XXXX..       }  topExtent (top y is baseline - topExtent)
  |   ..XX..       }
  |   ..XX..       }  dotted areas are where the image data
  |   ..XX..       }  is actually located for the character
  +---XXXXXX----   }  (it extends to the right & down for pow of 2 textures)
  |
  ^^^^ leftExtent (amount to move over before drawing the image

  ^^^^^^^^^^^^^^ setWidth (width displaced by char)
*/

public class PFont implements PConstants {

  //int firstChar = 33; // always
  public int charCount;
  public PImage images[];

  // image width, a power of 2
  // note! these will always be the same
  public int twidth, theight;
  // float versions of the above
  //float twidthf, theightf;

  // formerly iwidthf, iheightf.. but that's wrong
  // actually should be mbox, the font size
  float fwidth, fheight;

  // mbox is just the font size (i.e. 48 for most vlw fonts)
  public int mbox2; // next power of 2 over the max image size
  public int mbox;  // actual "font size" of source font

  public int value[];  // char code
  public int height[]; // height of the bitmap data
  public int width[];  // width of bitmap data
  public int setWidth[];  // width displaced by the char
  public int topExtent[];  // offset for the top
  public int leftExtent[];  // offset for the left

  public int ascent;
  public int descent;

  // scaling, for convenience
  public float size;
  public float leading;
  public int align;
  public int space;

  int ascii[];  // quick lookup for the ascii chars
  boolean cached;

  // used by the text() functions to avoid over-allocation of memory
  private char textBuffer[] = new char[8 * 1024];
  private char widthBuffer[] = new char[8 * 1024];


  public PFont() { }  // for PFontAI subclass and font builder


  public PFont(InputStream input) throws IOException {
    DataInputStream is = new DataInputStream(input);

    // number of character images stored in this font
    charCount = is.readInt();

    // bit count is ignored since this is always 8
    int numBits = is.readInt();

    // this was formerly ignored, now it's the actual font size
    mbox = is.readInt();
    // this was formerly mboxY, the one that was used
    // this will make new fonts downward compatible
    mbox2 = is.readInt();

    fwidth = mbox;
    fheight = mbox;

    // size for image ("texture") is next power of 2
    // over the font size. for most vlw fonts, the size is 48
    // so the next power of 2 is 64.
    // double-check to make sure that mbox2 is a power of 2
    // there was a bug in the old font generator that broke this
    mbox2 = (int) Math.pow(2, Math.ceil(Math.log(mbox2) / Math.log(2)));
    // size for the texture is stored in the font
    twidth = theight = mbox2;

    ascent  = is.readInt();  // formerly baseHt (zero/ignored)
    descent = is.readInt();  // formerly ignored struct padding

    //System.out.println("found mbox = " + mbox);
    //System.out.println("found ascent/descent = " + ascent + " " + descent);

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
      //int pixels[] = new int[64 * 64];
      int pixels[] = new int[twidth * theight];
      //images[i] = new PImage(pixels, 64, 64, ALPHA);
      images[i] = new PImage(pixels, twidth, theight, ALPHA);
      //images[i] = new PImage(pixels, twidth, theight, RGBA);
      int bitmapSize = height[i] * width[i];

      byte temp[] = new byte[bitmapSize];
      is.readFully(temp);

      // convert the bitmap to an alpha channel
      int w = width[i];
      int h = height[i];
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          int valu = temp[y*w + x] & 0xff;
          //images[i].pixels[y * twidth + x] = valu;

          images[i].pixels[y * twidth + x] =
            (valu << 24) | 0xFFFFFF;
            //0xFFFFFF00 | valu;
            //(valu << 24) | (valu << 16) | (valu << 8) | valu;
          //0x8040ff40;
            //(valu << 24) | (valu << 16) | (valu << 8) | valu;
          //System.out.print((images[i].pixels[y*64+x] > 128) ? "*" : ".");
        }
        //System.out.println();
      }
      //System.out.println();
    }
    cached = false;

    resetSize();
    //resetLeading(); // ??
    space = OBJECT_SPACE;
    align = ALIGN_LEFT;
  }

    //static boolean isSpace(int c) {
    //return (Character.isWhitespace((char) c) ||
    //      (c == '\u00A0') || (c == '\u2007') || (c == '\u202F'));
    //}


  public void write(OutputStream output) throws IOException {
    DataOutputStream os = new DataOutputStream(output);

    os.writeInt(charCount);
    os.writeInt(8);       // numBits
    os.writeInt(mbox);    // formerly mboxX (was 64, now 48)
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
      //int bitmapSize = height[i] * width[i];
      //byte bitmap[] = new byte[bitmapSize];

      for (int y = 0; y < height[i]; y++) {
        for (int x = 0; x < width[i]; x++) {
          //os.write(images[i].pixels[y * width[i] + x] & 0xff);
          os.write(images[i].pixels[y * mbox2 + x] & 0xff);
        }
      }
    }
    os.flush();
    os.close();  // can/should i do this?
  }


  /**
   * Get index for the char (convert from unicode to bagel charset).
   * @return index into arrays or -1 if not found
   */
  public int index(char c) {
    // degenerate case, but the find function will have trouble
    // if there are somehow zero chars in the lookup
    if (value.length == 0) return -1;

    // these chars required in all fonts
    //if ((c >= 33) && (c <= 126)) {
    //return c - 33;
    //}
    // quicker lookup for the ascii fellers
    if (c < 128) return ascii[c];

    // some other unicode char, hunt it out
    return index_hunt(c, 0, value.length-1);
  }


  // whups, this used the p5 charset rather than what was inside the font
  // meaning that old fonts would crash.. fixed for 0069

  private int index_hunt(int c, int start, int stop) {
    //System.err.println("checking between " + start + " and " + stop);
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


  public void space(int which) {
    this.space = which;
    if (space == SCREEN_SPACE) {
      resetSize();
      //resetLeading();
    }
  }


  public void align(int which) {
    this.align = which;
  }


  public float kern(char a, char b) {
    return 0;  // * size, but since zero..
  }


  public void resetSize() {
    //size = 12;
    size = mbox;  // default size for the font
    resetLeading();  // has to happen with the resize
  }


  public void size(float isize) {
    size = isize;
  }


  public void resetLeading() {
    // by trial & error, this seems close to illustrator
    leading = (ascent() + descent()) * 1.275f;
  }


  public void leading(float ileading) {
    leading = ileading;
  }


  public float ascent() {
    return ((float)ascent / fheight) * size;
  }


  public float descent() {
    return ((float)descent / fheight) * size;
  }


  public float width(char c) {
    if (c == 32) return width('i');

    int cc = index(c);
    if (cc == -1) return 0;

    return ((float)setWidth[cc] / fwidth) * size;
  }


  public float width(String str) {
    int length = str.length();
    if (length > widthBuffer.length) {
      widthBuffer = new char[length + 10];
    }
    str.getChars(0, length, widthBuffer, 0);

    float wide = 0;
    //float pwide = 0;
    int index = 0;
    int start = 0;

    while (index < length) {
      if (widthBuffer[index] == '\n') {
        wide = Math.max(wide, calcWidth(widthBuffer, start, index));
        start = index+1;
      }
      index++;
    }
    //System.out.println(start + " " + length + " " + index);
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


  public void text(char c, float x, float y, PGraphics parent) {
    text(c, x, y, 0, parent);
  }


  public void text(char c, float x, float y, float z, PGraphics parent) {
    //if (!valid) return;
    //if (!exists(c)) return;

    // eventually replace this with a table
    // to convert the > 127 coded chars
    //int glyph = c - 33;
    int glyph = index(c);
    if (glyph == -1) return;

    if (!cached) {
      // cache on first run, to ensure a graphics context exists
      parent.cache(images);
      cached = true;
    }

    if (space == OBJECT_SPACE) {
      float high    = (float) height[glyph]     / fheight;
      float bwidth  = (float) width[glyph]      / fwidth;
      float lextent = (float) leftExtent[glyph] / fwidth;
      float textent = (float) topExtent[glyph]  / fheight;

      int savedTextureMode = parent.textureMode;
      boolean savedStroke = parent.stroke;

      parent.textureMode = IMAGE_SPACE;
      //parent.drawing_text = true;
      parent.stroke = false;

      float x1 = x + lextent * size;
      float y1 = y - textent * size;
      float x2 = x1 + bwidth * size;
      float y2 = y1 + high * size;

      // this code was moved here (instead of using parent.image)
      // because now images use tint() for their coloring, which
      // internally is kind of a hack because it temporarily sets
      // the fill color to the tint values when drawing text.
      // rather than doubling up the hack with this hack, the code
      // is just included here instead.

      //System.out.println(x1 + " " + y1 + " " + x2 + " " + y2);

      parent.beginShape(QUADS);
      parent.texture(images[glyph]);
      parent.vertex(x1, y1, z, 0, 0);
      parent.vertex(x1, y2, z, 0, height[glyph]);
      parent.vertex(x2, y2, z, width[glyph], height[glyph]);
      parent.vertex(x2, y1, z, width[glyph], 0);
      parent.endShape();

      parent.textureMode = savedTextureMode;
      //parent.drawing_text = false;
      parent.stroke = savedStroke;

    } else {  // SCREEN_SPACE
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
        //System.out.println("x " + xx + " " + x0 + " " + w0);
        xx = 0;
      }
      if (yy < 0) {
        y0 -= yy;
        h0 += yy;
        //System.out.println("y " + yy + " " + y0 + " " + h0);
        yy = 0;
      }
      if (xx + w0 > parent.width) {
        //System.out.println("wide " + x0 + " " + w0);
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
        y += leading;
      }
      index++;
    }
    if (start < length) {
      textLine(start, index, x, y, z, parent);
    }
  }


  private void textLine(int start, int stop,
                        float x, float y, float z,
                        PGraphics parent) {
    //float startX = x;
    //int index = 0;
    //char previous = 0;

    if (align == ALIGN_CENTER) {
      x -= calcWidth(textBuffer, start, stop) / 2f;

    } else if (align == ALIGN_RIGHT) {
      x -= calcWidth(textBuffer, start, stop);
    }

    for (int index = start; index < stop; index++) {
      text(textBuffer[index], x, y, z, parent);
      x += width(textBuffer[index]);
    }
  }


  /**
   * Same as below, just without a z coordinate.
   */
  public void text(String str, float x, float y,
                   float w, float h, PGraphics parent) {
    text(str, x, y, 0, w, h, parent);
  }


  /**
   * Draw text in a box that is constrained to a particular size.
   * The current rectMode() determines what the coordinates mean
   * (whether x1/y1/x2/y2 or x/y/w/h).
   *
   * Note that the x,y coords of the start of the box
   * will align with the *ascent* of the text, not the baseline,
   * as is the case for the other text() functions.
   */
  public void text(String str, float boxX1, float boxY1, float boxZ,
                   float boxX2, float boxY2, PGraphics parent) {
    float spaceWidth = width(' ');
    float runningX = boxX1;
    float currentY = boxY1;
    float boxWidth = boxX2 - boxX1;
    //float right = x + w;

    float lineX = boxX1;
    if (align == ALIGN_CENTER) {
      lineX = lineX + boxWidth/2f;
    } else if (align == ALIGN_RIGHT) {
      lineX = boxX2;
    }

    // ala illustrator, the text itself must fit inside the box
    currentY += ascent();
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
      if ((textBuffer[index] == ' ') ||
          (index == length-1)) {
        // boundary of a word
        float wordWidth = calcWidth(textBuffer, wordStart, index);
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
                return;
              }
              wordWidth = calcWidth(textBuffer, wordStart, index);
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
          currentY += leading;
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
        currentY += leading;
        if (currentY > boxY2) return;  // box is now full
      }
      index++;
    }
    if ((lineStart < length) &&
        (lineStart != index)) {  // if line is not empty
      //System.out.println("line not empty " +
      //                 new String(textBuffer, lineStart, index));
      textLine(lineStart, index, lineX, currentY, boxZ, parent);
    }
  }


  // .................................................................


  /**
   * Draw SCREEN_SPACE text on its left edge.
   * This method is incomplete and should not be used.
   */
  public void ltext(String str, float x, float y, PGraphics parent) {
    float startY = y;
    int index = 0;
    char previous = 0;

    int length = str.length();
    if (length > textBuffer.length) {
      textBuffer = new char[length + 10];
    }
    str.getChars(0, length, textBuffer, 0);

    while (index < length) {
      if (textBuffer[index] == '\n') {
        y = startY;
        x += leading;
        previous = 0;

      } else {
        ltext(textBuffer[index], x, y, parent);
        y -= width(textBuffer[index]);
        if (previous != 0)
          y -= kern(previous, textBuffer[index]);
        previous = textBuffer[index];
      }
      index++;
    }
  }


  /**
   * Draw SCREEN_SPACE text on its left edge.
   * This method is incomplete and should not be used.
   */
  public void ltext(char c, float x, float y, PGraphics parent) {
    int glyph = index(c);
    if (glyph == -1) return;

    // top-lefthand corner of the char
    int sx = (int) x - topExtent[glyph];
    int sy = (int) y - leftExtent[glyph];

    // boundary of the character's pixel buffer to copy
    int px = 0;
    int py = 0;
    int pw = width[glyph];
    int ph = height[glyph];

    // if the character is off the screen
    if ((sx >= parent.width) ||         // top of letter past width
        (sy - pw >= parent.height) ||
        (sy + pw < 0) ||
        (sx + ph < 0)) return;

    if (sx < 0) {  // if starting x is off screen
      py -= sx;
      ph += sx;
      sx = 0;
    }
    if (sx + ph >= parent.width) {
      ph -= ((sx + ph) - parent.width);
    }

    if (sy < pw) {
      //int extra = pw - sy;
      pw -= -1 + pw - sy;
      //px -= sy;
      //pw += sy;
      //sy = 0;
    }
    if (sy >= parent.height) {  // off bottom edge
      int extra = 1 + sy - parent.height;
      pw -= extra;
      px += extra;
      sy -= extra;
      //pw -= ((sy + pw) - parent.height);
    }

    int fr = parent.fillRi;
    int fg = parent.fillGi;
    int fb = parent.fillBi;
    int fa = parent.fillAi;

    int pixels1[] = images[glyph].pixels;
    int pixels2[] = parent.pixels;

    // loop over the source pixels in the character image
    // row & col is the row and column of the source image
    // (but they become col & row in the target image)
    for (int row = py; row < py + ph; row++) {
      for (int col = px; col < px + pw; col++) {
        int a1 = (fa * pixels1[row * twidth + col]) >> 8;
        int a2 = a1 ^ 0xff;
        int p1 = pixels1[row * width[glyph] + col];

        try {
          int index = (sy + px-col)*parent.width + (sx+row-py);
          int p2 = pixels2[index];

          pixels2[index] =
            (0xff000000 |
             (((a1 * fr + a2 * ((p2 >> 16) & 0xff)) & 0xff00) << 8) |
             (( a1 * fg + a2 * ((p2 >>  8) & 0xff)) & 0xff00) |
             (( a1 * fb + a2 * ( p2        & 0xff)) >> 8));
        } catch (ArrayIndexOutOfBoundsException e) {
          System.out.println("out of bounds " + sy + " " + px + " " + col);
        }
      }
    }
  }


  // .................................................................


  /**
   * Draw SCREEN_SPACE text on its right edge.
   * This method is incomplete and should not be used.
   */
  public void rtext(String str, float x, float y, PGraphics parent) {
    float startY = y;
    int index = 0;
    char previous = 0;

    int length = str.length();
    if (length > textBuffer.length) {
      textBuffer = new char[length + 10];
    }
    str.getChars(0, length, textBuffer, 0);

    while (index < length) {
      if (textBuffer[index] == '\n') {
        y = startY;
        x += leading;
        previous = 0;

      } else {
        rtext(textBuffer[index], x, y, parent);
        y += width(textBuffer[index]);
        if (previous != 0)
          y += kern(previous, textBuffer[index]);
        previous = textBuffer[index];
      }
      index++;
    }
  }


  /**
   * Draw SCREEN_SPACE text on its right edge.
   * This method is incomplete and should not be used.
   */
  public void rtext(char c, float x, float y, PGraphics parent) {
    int glyph = index(c);
    if (glyph == -1) return;

    // starting point on the screen
    int sx = (int) x + topExtent[glyph];
    int sy = (int) y + leftExtent[glyph];

    // boundary of the character's pixel buffer to copy
    int px = 0;
    int py = 0;
    int pw = width[glyph];
    int ph = height[glyph];

    // if the character is off the screen
    if ((sx - ph >= parent.width) || (sy >= parent.height) ||
        (sy + pw < 0) || (sx < 0)) return;

    // off the left of screen, cut off bottom of letter
    if (sx < ph) {
      //x0 -= xx;  // chop that amount off of the image area to be copied
      //w0 += xx;  // and reduce the width by that (negative) amount
      //py0 -= xx;  // if x = -3, cut off 3 pixels from the bottom
      //ph0 += xx;
      ph -= (ph - sx) - 1;
      //sx = 0;
    }
    // off the right of the screen, cut off top of the letter
    if (sx >= parent.width) {
      int extra = sx - (parent.width-1);
      py += extra;
      ph -= extra;
      //sx = parent.width-1;
    }
    // off the top, cut off left edge of letter
    if (sy < 0) {
      int extra = -sy;
      px += extra;
      pw -= extra;
      sy = 0;
    }
    // off the bottom, cut off right edge of letter
    if (sy + pw >= parent.height-1) {
      int extra = (sy + pw) - parent.height;
      pw -= extra;
    }

    int fr = parent.fillRi;
    int fg = parent.fillGi;
    int fb = parent.fillBi;
    int fa = parent.fillAi;

    int fpixels[] = images[glyph].pixels;
    int spixels[] = parent.pixels;

    // loop over the source pixels in the character image
    // row & col is the row and column of the source image
    // (but they become col & row in the target image)
    for (int row = py; row < py + ph; row++) {
      for (int col = px; col < px + pw; col++) {
        int a1 = (fa * fpixels[row * twidth + col]) >> 8;
        int a2 = a1 ^ 0xff;
        int p1 = fpixels[row * width[glyph] + col];

        try {
          //int index = (yy + x0-col)*parent.width + (xx+row-y0);
          //int index = (sy + px-col)*parent.width + (sx+row-py);
          int index = (sy + px+col)*parent.width + (sx-row);
          int p2 = spixels[index];

          // x coord is backwards
          spixels[index] =
            (0xff000000 |
             (((a1 * fr + a2 * ((p2 >> 16) & 0xff)) & 0xff00) << 8) |
             (( a1 * fg + a2 * ((p2 >>  8) & 0xff)) & 0xff00) |
             (( a1 * fb + a2 * ( p2        & 0xff)) >> 8));
        } catch (ArrayIndexOutOfBoundsException e) {
          System.out.println("out of bounds " + sy + " " + px + " " + col);
        }
      }
    }
  }


  // ....................................................................


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


  public PFont(String name, int size) {
    this(new Font(name, Font.PLAIN, size), false, true);
  }

  public PFont(String name, int size, boolean smooth) {
    this(new Font(name, Font.PLAIN, size), false, smooth);
  }

  public PFont(Font font, boolean all, boolean smooth) {
    try {
      this.charCount = all ? 65536 : charset.length;
      this.mbox = font.getSize();

      fwidth = fheight = mbox;

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
      char c = all ? (char)i : charset[i];

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
      g.drawString(String.valueOf(c), mbox, mbox * 2);

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
          int value = 255 - (samples[y * mbox3 + x] & 0xff);
          //int value = 255 - raster.getSample(x, y, 0);
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
      return;
    }
  }
}
