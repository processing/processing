/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  BFont - font object for text rendering
  Part of the Processing project - http://processing.org

  Copyright (c) 2001-04 Massachusetts Institute of Technology 
  (Except where noted that the author is not Ben Fry)

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

import java.io.*;
import java.util.*;


// value[] could be used to build a char to byte mapping table
// as the font is loaded..
// when generating, use the native char mapping. 

public class PFont implements PConstants {

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


  //int firstChar = 33; // always
  int charCount;
  PImage images[];

  // image width, a power of 2
  // note! these will always be the same
  int iwidth, iheight; 
  // float versions of the above
  float iwidthf, iheightf;

  // mbox is just the font size (i.e. 48 for most vlw fonts)
  int mbox;

  int value[];  // char code
  int height[]; // height of the bitmap data
  int width[];  // width of bitmap data
  int setWidth[];  // width displaced by the char
  int topExtent[];  // offset for the top
  int leftExtent[];  // offset for the left

  // scaling, for convenience
  float size; 
  float leading;

  boolean cached;


  public PFont() { }  // for PFontAI subclass and font builder


  // can this throw an exception instead? 
  /*
  public PFont(String filename, PApplet parent) throws IOException {
    //this.parent = parent;
    //this.valid = false;

    String lower = filename.toLowerCase();
    if (lower.endsWith(".vlw")) {
      read(parent.openStream(filename));

    } else if (lower.endsWith(".vlw.gz")) {
      read(new GZIPInputStream(parent.openStream(filename)));

    } else {
      throw new IOException("don't know what type of file that is");
    }
    cached = false;
    size();
  }
  */


  /*
  public PFont(InputStream input) throws IOException {
    read(input);
    //cached = false;
    //size();
  }
  */

  public void write(OutputStream output) throws IOException {
    DataOutputStream os = new DataOutputStream(output);

    os.writeInt(charCount); 
    os.writeInt(8);     // numBits
    os.writeInt(mbox);  // mboxX (font size)
    os.writeInt(mbox);  // mboxY (font size)
    os.writeInt(0);     // baseHt, ignored
    os.writeInt(0);     // struct padding for c version

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
          os.write(images[i].pixels[y * width[i] + x] & 0xff);
        }
      }
    }
    os.flush();
    os.close();  // can/should i do this?
  }


  //private void load_vlw_font(String filename) throws IOException {
  //public void read(InputStream input) throws IOException {
  public PFont(InputStream input) throws IOException {
    DataInputStream is = new DataInputStream(input);

    charCount   = is.readInt();
    int numBits = is.readInt();
    int mboxX   = is.readInt();  // not used, just fontsize (48)
    int mboxY   = is.readInt();  // also just fontsize (48)

    // only store this one for leading calc
    mbox = mboxY;  

    // size for image ("texture") is next power of 2
    // over the font size. for most vlw fonts, the size is 48
    // so the next power of 2 is 64. 
    iwidth = (int) 
      Math.pow(2, Math.ceil(Math.log(mboxX) / Math.log(2)));
    iheight = (int) 
      Math.pow(2, Math.ceil(Math.log(mboxY) / Math.log(2)));

    iwidthf = (float) iwidth;
    iheightf = (float) iheight;

    // font size is 48, so default leading is 48 * 1.2
    // this is same as what illustrator uses for the default
    //defaultLeading = ((float)mboxY / iheightf) * 1.2f;

    int baseHt = is.readInt(); // zero, ignored
    is.readInt(); // ignore 4 for struct padding

    // allocate enough space for the character info
    value       = new int[charCount];
    height      = new int[charCount];
    width       = new int[charCount];
    setWidth    = new int[charCount];
    topExtent   = new int[charCount];
    leftExtent  = new int[charCount];

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
    }

    images = new PImage[charCount];
    for (int i = 0; i < charCount; i++) {
      //int pixels[] = new int[64 * 64];
      int pixels[] = new int[iwidth * iheight];
      //images[i] = new PImage(pixels, 64, 64, ALPHA);
      images[i] = new PImage(pixels, iwidth, iheight, ALPHA);
      int bitmapSize = height[i] * width[i];

      byte temp[] = new byte[bitmapSize];
      is.readFully(temp);

      // convert the bitmap to an alpha channel
      int w = width[i];
      int h = height[i];
      for (int x = 0; x < w; x++) {
        for (int y = 0; y < h; y++) {
          int valu = temp[y*w + x] & 0xff;
          //images[i].pixels[y*64 + x] = valu;
          images[i].pixels[y * iwidth + x] = valu;
          // the following makes javagl more happy.. 
          // not sure what's going on
          //(valu << 24) | (valu << 16) | (valu << 8) | valu; //0xffffff;
          //System.out.print((images[i].pixels[y*64+x] > 128) ? "*" : ".");
        }
        //System.out.println();
      }
      //System.out.println();
    }
    cached = false;
    size();
  }


  /**
   * Get index for the char (convert from unicode to bagel charset).
   * @return index into arrays or -1 if not found
   */
  public int index(char c) {
    // these chars required in all fonts
    if ((c >= 33) && (c <= 126)) {
      return c - 33;
    }
    // some other unicode char, hunt it out
    //return index_hunt(c, 0, charset.length-1);
    return index_hunt(c, 0, value.length-1);
  }


  // whups, this used the p5 charset rather than what was inside the font
  // meaning that old fonts would crash.. fixed for 0069

  private int index_hunt(int c, int start, int stop) {
    //System.out.println("checking between " + start + " and " + stop);
    int pivot = (start + stop) / 2;

    // if this is the char, then return it
    //if (c == charset[pivot]) return pivot;
    if (c == value[pivot]) return pivot;

    // char doesn't exist, otherwise would have been the pivot
    //if (start == stop) return -1; 
    if (start >= stop) return -1;

    // if it's in the lower half, continue searching that 
    //if (c < charset[pivot]) return index_hunt(c, start, pivot-1);
    if (c < value[pivot]) return index_hunt(c, start, pivot-1);

    // if it's in the upper half, continue there
    return index_hunt(c, pivot+1, stop);
  }


  float kern(char a, char b) { 
    return 0;  // * size, but since zero..
  }


  public void size() {
    size = 12;
  }


  public void size(float isize) {
    size = isize;
    //leading();
  }


  public void leading() {
    leading = size * ((float)mbox / iheightf) * 1.2f;
  }


  public void leading(float ileading) {
    leading = ileading;
  }


  // supposedly this should be ok even in SCREEN_SPACE mode
  // since the applet will set the 'size' of the font to iwidth
  // (though this prolly breaks any sort of 'height' measurements)
  public float width(char c) {
    if (c == 32) return width('i');

    int cc = index(c);
    if (cc == -1) return 0;

    return ((float)setWidth[cc] / iwidthf) * size;
  }


  public float width(String string) {
    //if (!valid) return 0;
    float wide = 0;
    float pwide = 0;
    char previous = 0;

    char s[] = string.toCharArray();
    for (int i = 0; i < s.length; i++) {
      if (s[i] == '\n') {
        if (wide > pwide) pwide = wide;
        wide = 0;
        previous = 0;

      } else {
        wide += width(s[i]);
        if (previous != 0) {
          wide += kern(previous, s[i]);
        }
        previous = s[i];
      }
    }
    return (pwide > wide) ? pwide : wide;
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

    if (parent.text_space == OBJECT_SPACE) {
      float high    = (float) height[glyph]     / iheightf;
      float bwidth  = (float) width[glyph]      / iwidthf;
      float lextent = (float) leftExtent[glyph] / iwidthf;
      float textent = (float) topExtent[glyph]  / iheightf;

      int savedTextureMode = parent.texture_mode;
      //boolean savedSmooth = parent.smooth;
      boolean savedStroke = parent._stroke;

      parent.texture_mode = IMAGE_SPACE;
      //parent.smooth = true;
      parent.drawing_text = true;
      parent._stroke = false;

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

      parent.texture_mode = savedTextureMode;
      //parent.smooth = savedSmooth;
      parent.drawing_text = false;
      parent._stroke = savedStroke;

    } else {  // SCREEN_SPACE
      int xx = (int) x + leftExtent[glyph];;
      int yy = (int) y - topExtent[glyph];

      //int x1 = xx + leftExtent[glyph];
      //int y1 = yy - topExtent[glyph];
      //int x2 = x1 + width[glyph];
      //int y2 = y1 + height[glyph];

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

      //int index1 = y0 * iwidth; //0;
      //int index2 = 0;

      //for (int row = 0; row < height[glyph]; row++) {
      for (int row = y0; row < y0 + h0; row++) {
        //for (int col = 0; col < width[glyph]; col++) {
        for (int col = x0; col < x0 + w0; col++) {
          int a1 = (fa * pixels1[row * iwidth + col]) >> 8;
          //System.out.println(index1 + col);
          //int a1 = (fa * pixels1[index1 + col]) >> 8;
          int a2 = a1 ^ 0xff;
          int p1 = pixels1[row * width[glyph] + col];
          int p2 = pixels2[(yy + row-y0)*parent.width + (xx+col-x0)];

          pixels2[(yy + row-y0)*parent.width + xx+col-x0] = 
            (0xff000000 | 
             (((a1 * fr + a2 * ((p2 >> 16) & 0xff)) & 0xff00) << 8) |
             (( a1 * fg + a2 * ((p2 >>  8) & 0xff)) & 0xff00) |
             (( a1 * fb + a2 * ( p2        & 0xff)) >> 8));
        }
        //index1 += iwidth;
      }
    }
  }

  /*
  public final static int _blend(int p1, int p2, int a2) {
    // scale alpha by alpha of incoming pixel
    a2 = (a2 * (p2 >>> 24)) >> 8;

    int a1 = a2 ^ 0xff;
    int r = (a1 * ((p1 >> 16) & 0xff) + a2 * ((p2 >> 16) & 0xff)) & 0xff00;
    int g = (a1 * ((p1 >>  8) & 0xff) + a2 * ((p2 >>  8) & 0xff)) & 0xff00;
    int b = (a1 * ( p1        & 0xff) + a2 * ( p2        & 0xff)) >> 8;

    return 0xff000000 | (r << 8) | g | b;
  }
  */


  private char c[] = new char[8192];

  public void text(String str, float x, float y, PGraphics parent) {
    text(str, x, y, 0, parent);
  }

  public void text(String str, float x, float y, float z, PGraphics parent) {
    float startX = x;
    int index = 0;
    char previous = 0;

    int length = str.length();
    if (length > c.length) {
      c = new char[length + 10];
    }
    str.getChars(0, length, c, 0);

    while (index < length) {
      if (c[index] == '\n') {
        x = startX;
        y += leading;
        previous = 0;
      } else {
        text(c[index], x, y, z, parent);
        x += width(c[index]);
        if (previous != 0)
          x += kern(previous, c[index]);
        previous = c[index];
      }
      index++;
    }
  }
}
