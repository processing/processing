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
import processing.core.util.image.constants.TifConstants;


/**
 * Strategy for loading a tif image.
 */
public class TiffImageLoadStrategy implements ImageLoadStrategy {

  @Override
  public PImage load(PApplet pApplet, String path, String extension) {
    byte bytes[] = pApplet.loadBytes(path);
    PImage image =  (bytes == null) ? null : loadFromBytes(bytes);
    return image;
  }

  /**
   * Load the tif image from bytes.
   *
   * @param tiff The bytes of the tif image.
   * @return PImage created from the provided bytes.
   */
  private PImage loadFromBytes(byte[] tiff) {
    if ((tiff[42] != tiff[102]) ||  // width/height in both places
        (tiff[43] != tiff[103])) {
      System.err.println(TifConstants.TIFF_ERROR);
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
      System.err.println(TifConstants.TIFF_ERROR + " (" + width + ", " + height +")");
      return null;
    }

    // check the rest of the header
    for (int i = 0; i < TifConstants.TIFF_HEADER.length; i++) {
      if ((i == 30) || (i == 31) || (i == 42) || (i == 43) ||
          (i == 102) || (i == 103) ||
          (i == 114) || (i == 115) || (i == 116) || (i == 117)) continue;

      if (tiff[i] != TifConstants.TIFF_HEADER[i]) {
        System.err.println(TifConstants.TIFF_ERROR + " (" + i + ")");
        return null;
      }
    }

    PImage outgoing = new PImage(width, height, PConstants.RGB);
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

}
