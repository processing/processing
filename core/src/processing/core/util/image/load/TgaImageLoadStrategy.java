/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-18 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core.util.image.load;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;

import java.io.IOException;
import java.io.InputStream;


/**
 * Strategy for loading a RLE-compressed TGA image.
 */
public class TgaImageLoadStrategy implements ImageLoadStrategy {

  @Override
  public PImage load(PApplet pApplet, String path, String extension) {
    try {
      PImage image = loadNoCatch(pApplet, path);
      return image;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Targa image loader for RLE-compressed TGA files which throws exceptions.
   * <p>
   * Rewritten for 0115 to read/write RLE-encoded targa images.
   * For 0125, non-RLE encoded images are now supported, along with
   * images whose y-order is reversed (which is standard for TGA files).
   * <p>
   * A version of this function is in MovieMaker.java. Any fixes here
   * should be applied over in MovieMaker as well.
   * <p>
   * Known issue with RLE encoding and odd behavior in some apps:
   * https://github.com/processing/processing/issues/2096
   * Please help!
   */
  private PImage loadNoCatch(PApplet pApplet, String filename) throws IOException {
    InputStream is = pApplet.createInput(filename);
    if (is == null) return null;

    byte header[] = new byte[18];
    int offset = 0;
    do {
      int count = is.read(header, offset, header.length - offset);
      if (count == -1) return null;
      offset += count;
    } while (offset < 18);

    /*
      header[2] image type code
      2  (0x02) - Uncompressed, RGB images.
      3  (0x03) - Uncompressed, black and white images.
      10 (0x0A) - Run-length encoded RGB images.
      11 (0x0B) - Compressed, black and white images. (grayscale?)

      header[16] is the bit depth (8, 24, 32)

      header[17] image descriptor (packed bits)
      0x20 is 32 = origin upper-left
      0x28 is 32 + 8 = origin upper-left + 32 bits

        7  6  5  4  3  2  1  0
      128 64 32 16  8  4  2  1
    */

    int format = 0;

    if (((header[2] == 3) || (header[2] == 11)) &&  // B&W, plus RLE or not
        (header[16] == 8) &&  // 8 bits
        ((header[17] == 0x8) || (header[17] == 0x28))) {  // origin, 32 bit
      format = PConstants.ALPHA;

    } else if (((header[2] == 2) || (header[2] == 10)) &&  // RGB, RLE or not
        (header[16] == 24) &&  // 24 bits
        ((header[17] == 0x20) || (header[17] == 0))) {  // origin
      format = PConstants.RGB;

    } else if (((header[2] == 2) || (header[2] == 10)) &&
        (header[16] == 32) &&
        ((header[17] == 0x8) || (header[17] == 0x28))) {  // origin, 32
      format = PConstants.ARGB;
    }

    if (format == 0) {
      System.err.println("Unknown .tga file format for " + filename);
      //" (" + header[2] + " " +
      //(header[16] & 0xff) + " " +
      //hex(header[17], 2) + ")");
      return null;
    }

    int w = ((header[13] & 0xff) << 8) + (header[12] & 0xff);
    int h = ((header[15] & 0xff) << 8) + (header[14] & 0xff);
    PImage outgoing = pApplet.createImage(w, h, format);

    // where "reversed" means upper-left corner (normal for most of
    // the modernized world, but "reversed" for the tga spec)
    //boolean reversed = (header[17] & 0x20) != 0;
    // https://github.com/processing/processing/issues/1682
    boolean reversed = (header[17] & 0x20) == 0;

    if ((header[2] == 2) || (header[2] == 3)) {  // not RLE encoded
      if (reversed) {
        int index = (h-1) * w;
        switch (format) {
          case PConstants.ALPHA:
            for (int y = h-1; y >= 0; y--) {
              for (int x = 0; x < w; x++) {
                outgoing.pixels[index + x] = is.read();
              }
              index -= w;
            }
            break;
          case PConstants.RGB:
            for (int y = h-1; y >= 0; y--) {
              for (int x = 0; x < w; x++) {
                outgoing.pixels[index + x] =
                    is.read() | (is.read() << 8) | (is.read() << 16) |
                        0xff000000;
              }
              index -= w;
            }
            break;
          case PConstants.ARGB:
            for (int y = h-1; y >= 0; y--) {
              for (int x = 0; x < w; x++) {
                outgoing.pixels[index + x] =
                    is.read() | (is.read() << 8) | (is.read() << 16) |
                        (is.read() << 24);
              }
              index -= w;
            }
        }
      } else {  // not reversed
        int count = w * h;
        switch (format) {
          case PConstants.ALPHA:
            for (int i = 0; i < count; i++) {
              outgoing.pixels[i] = is.read();
            }
            break;
          case PConstants.RGB:
            for (int i = 0; i < count; i++) {
              outgoing.pixels[i] =
                  is.read() | (is.read() << 8) | (is.read() << 16) |
                      0xff000000;
            }
            break;
          case PConstants.ARGB:
            for (int i = 0; i < count; i++) {
              outgoing.pixels[i] =
                  is.read() | (is.read() << 8) | (is.read() << 16) |
                      (is.read() << 24);
            }
            break;
        }
      }

    } else {  // header[2] is 10 or 11
      int index = 0;
      int px[] = outgoing.pixels;

      while (index < px.length) {
        int num = is.read();
        boolean isRLE = (num & 0x80) != 0;
        if (isRLE) {
          num -= 127;  // (num & 0x7F) + 1
          int pixel = 0;
          switch (format) {
            case PConstants.ALPHA:
              pixel = is.read();
              break;
            case PConstants.RGB:
              pixel = 0xFF000000 |
                  is.read() | (is.read() << 8) | (is.read() << 16);
              //(is.read() << 16) | (is.read() << 8) | is.read();
              break;
            case PConstants.ARGB:
              pixel = is.read() |
                  (is.read() << 8) | (is.read() << 16) | (is.read() << 24);
              break;
          }
          for (int i = 0; i < num; i++) {
            px[index++] = pixel;
            if (index == px.length) break;
          }
        } else {  // write up to 127 bytes as uncompressed
          num += 1;
          switch (format) {
            case PConstants.ALPHA:
              for (int i = 0; i < num; i++) {
                px[index++] = is.read();
              }
              break;
            case PConstants.RGB:
              for (int i = 0; i < num; i++) {
                px[index++] = 0xFF000000 |
                    is.read() | (is.read() << 8) | (is.read() << 16);
                //(is.read() << 16) | (is.read() << 8) | is.read();
              }
              break;
            case PConstants.ARGB:
              for (int i = 0; i < num; i++) {
                px[index++] = is.read() | //(is.read() << 24) |
                    (is.read() << 8) | (is.read() << 16) | (is.read() << 24);
                //(is.read() << 16) | (is.read() << 8) | is.read();
              }
              break;
          }
        }
      }

      if (!reversed) {
        int[] temp = new int[w];
        for (int y = 0; y < h/2; y++) {
          int z = (h-1) - y;
          System.arraycopy(px, y*w, temp, 0, w);
          System.arraycopy(px, z*w, px, y*w, w);
          System.arraycopy(temp, 0, px, z*w, w);
        }
      }
    }
    is.close();
    return outgoing;
  }

}
