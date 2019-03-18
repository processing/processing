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

package processing.core.util.image;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.util.image.load.*;
import processing.core.util.io.PathUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Utility for loading images either from file system or string encoding using a set of strategies.
 */
public class ImageLoadFacade {

  private static final AtomicReference<ImageLoadFacade> instance = new AtomicReference<>(null);

  private static final String PREFIX_BASE64_STRING_IMAGE = "data:image";
  private static final String PREFIX_FILE_PATH = "file://";

  private final Map<String, ImageLoadStrategy> loadStrategies;
  private final ImageLoadStrategy defaultImageLoadStrategy;

  /**
   * Get a shared instance of this singleton.
   *
   * @return Shared instance of this singleton.
   */
  public static ImageLoadFacade get() {
    instance.compareAndSet(null, new ImageLoadFacade());
    return instance.get();
  }

  /**
   * Hidden constructor. Clients should use get().
   */
  private ImageLoadFacade() {
    loadStrategies = new HashMap<>();

    loadStrategies.put("base64", new Base64StringImageLoadStrategy());

    loadStrategies.put("tga", new TgaImageLoadStrategy());

    ImageLoadStrategy tifImageLoadStrategy = new TiffImageLoadStrategy();
    loadStrategies.put("tif", tifImageLoadStrategy);
    loadStrategies.put("tiff", tifImageLoadStrategy);

    ImageLoadStrategy awtImageLoadStrategy = new AwtImageLoadStrategy();
    defaultImageLoadStrategy = new ImageIoImageLoadStrategy();

    ImageLoadStrategy awtFallbackStrategy = new FallbackImageLoadStrategy(
        awtImageLoadStrategy,
        defaultImageLoadStrategy
    );
    loadStrategies.put("jpg", awtFallbackStrategy);
    loadStrategies.put("jpeg", awtFallbackStrategy);
    loadStrategies.put("gif", awtFallbackStrategy);
    loadStrategies.put("png", awtFallbackStrategy);
    loadStrategies.put("unknown", awtFallbackStrategy);
  }

  /**
   * Load an image embedded within an SVG string.
   *
   * @param pApplet The PApplet on whose behalf an SVG is being parsed. This must be given so that
   *                image can be retrieved in the case of sketch relative file.
   * @param svgImageStr The SVG string to load which can be data:image or file://.
   * @return The image loaded as a PImage.
   */
  public PImage loadFromSvg(PApplet pApplet, String svgImageStr) {
    if (svgImageStr == null) {
      return null;
    }

    if (svgImageStr.startsWith(PREFIX_BASE64_STRING_IMAGE)) {
      String[] parts = svgImageStr.split(";base64,");
      String extension = parts[0].substring(11);
      String encodedData = parts[1];
      return loadStrategies.get("base64").load(pApplet, encodedData, extension);
    } else if (svgImageStr.startsWith(PREFIX_FILE_PATH)) {
      String filePath = svgImageStr.substring(PREFIX_FILE_PATH.length());
      return loadFromFile(pApplet, filePath);
    } else {
      return null;
    }
  }

  /**
   * Load an image from a file.
   *
   * @param pApplet The PApplet through which the image should be retrieved in the case of sketch
   *     relative file (data folder for example).
   * @param path The path to the file to be opened.
   * @return The image loaded.
   */
  public PImage loadFromFile(PApplet pApplet, String path) {
    return loadFromFile(pApplet, path, null);
  }

  /**
   * Load an image from a file using the given file extension.
   *
   * @param pApplet The PApplet through which the image should be retrieved in the case of sketch
   *     relative file (data folder for example).
   * @param path The path to the file to be opened.
   * @param extension The extension with which the image should be loaded like "png".
   * @return The image loaded.
   */
  public PImage loadFromFile(PApplet pApplet, String path, String extension) {
    if (extension == null) {
      extension = PathUtil.parseExtension(path);
    }

    // just in case. them users will try anything!
    extension = PathUtil.cleanExtension(extension);

    // Find strategy for loading
    ImageLoadStrategy imageLoadStrategy = loadStrategies.getOrDefault(
        extension,
        defaultImageLoadStrategy
    );

    // Load image
    PImage resultImage = imageLoadStrategy.load(pApplet, path, extension);

    // Report error or return
    if (resultImage == null) {
      System.err.println("Could not find a method to load " + path);
      return null;
    } else {
      return resultImage;
    }
  }

}
