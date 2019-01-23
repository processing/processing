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
import processing.core.PImage;
import processing.core.util.io.InputFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Use Java 1.4 ImageIO methods to load an image.
 */
public class ImageIoImageLoadStrategy implements ImageLoadStrategy {

  private AtomicReference<String[]> cachedLoadImageFormats;

  public ImageIoImageLoadStrategy() {
    cachedLoadImageFormats = new AtomicReference<>(null);
  }

  /**
   * Load an image after checking that its format is supported.
   *
   * @param pApplet The PApplet on whose behalf an image is being loaded. If null, cannot use sketch
   *    relative file paths.
   * @param path The path to the file like "subdirectory/file.png". Note that paths without
   *    extensions are supported and the extension is not read off this path but instead must be
   *    specified in the extension parameter.
   * @param extension The extension of the file to open.
   * @return The newly loaded image.
   */
  @Override
  public PImage load(PApplet pApplet, String path, String extension) {
    String[] loadImageFormats = getLoadImageFormats();

    if (loadImageFormats != null) {
      for (int i = 0; i < loadImageFormats.length; i++) {
        if (extension.equals(loadImageFormats[i])) {
          return loadImageIOInner(pApplet, path);
        }
      }
    }

    // failed, could not load image after all those attempts
    System.err.println("Could not find a method to load " + path);
    return null;
  }

  /**
   * Get the list of supported image formats.
   *
   * @return List of image formats like "png" that are supported by this strategy.
   */
  private String[] getLoadImageFormats() {
    cachedLoadImageFormats.compareAndSet(null, ImageIO.getReaderFormatNames());
    return cachedLoadImageFormats.get();
  }

  /**
   * Load an image without checking that its format is supported.
   *
   * @param pApplet The PApplet on whose behalf an image is being loaded. If null, cannot use sketch
   *    relative file paths.
   * @param path The path to the file like "subdirectory/file.png". Note that paths without
   *    extensions are supported and the extension is not read off this path but instead must be
   *    specified in the extension parameter.
   * @return The newly loaded image.
   */
  private PImage loadImageIOInner(PApplet pApplet, String filename) {
    InputStream stream = InputFactory.createInput(pApplet, filename);
    if (stream == null) {
      System.err.println("The image " + filename + " could not be found.");
      return null;
    }

    try {
      BufferedImage bi = ImageIO.read(stream);

      int width = bi.getWidth();
      int height = bi.getHeight();
      int[] pixels = new int[width * height];

      bi.getRGB(0, 0, width, height, pixels, 0, width);

      // check the alpha for this image
      // was gonna call getType() on the image to see if RGB or ARGB,
      // but it's not actually useful, since gif images will come through
      // as TYPE_BYTE_INDEXED, which means it'll still have to check for
      // the transparency. also, would have to iterate through all the other
      // types and guess whether alpha was in there, so.. just gonna stick
      // with the old method.
      PImage outgoing = new PImage(width, height, pixels, true, pApplet);

      stream.close();
      // return the image
      return outgoing;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

}
