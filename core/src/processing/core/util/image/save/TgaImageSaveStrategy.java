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

package processing.core.util.image.save;

import processing.core.PConstants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Strategy for creating tga (targa32) images.
 */
public class TgaImageSaveStrategy implements ImageSaveStrategy {

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
  @Override
  public boolean save(int[] pixels, int pixelWidth, int pixelHeight, int format,
      String filename) throws FileNotFoundException {

    OutputStream output = ImageSaveUtil.createForFile(filename);

    byte header[] = new byte[18];

    if (format == PConstants.ALPHA) {  // save ALPHA images as 8bit grayscale
      header[2] = 0x0B;
      header[16] = 0x08;
      header[17] = 0x28;

    } else if (format == PConstants.RGB) {
      header[2] = 0x0A;
      header[16] = 24;
      header[17] = 0x20;

    } else if (format == PConstants.ARGB) {
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
      if (format == PConstants.ALPHA) {
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
            if (format == PConstants.ARGB) output.write(col >>> 24 & 0xff);

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
            if (format == PConstants.ARGB) {
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
      output.close();
      return true;

    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

}
