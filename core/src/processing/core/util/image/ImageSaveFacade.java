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
import processing.core.PGraphics;
import processing.core.util.image.save.*;
import processing.core.util.io.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Facade to load PImages from various sources.
 */
public class ImageSaveFacade {

  private static final AtomicReference<ImageSaveFacade> instance = new AtomicReference<>(null);

  private final Map<String, ImageSaveStrategy> saveStrategies;
  private final ImageSaveStrategy defaultImageSaveStrategy;

  /**
   * Get a shared instance of this singleton.
   *
   * @return Shared instance of ImageSaveFacade.
   */
  public static ImageSaveFacade get() {
    instance.compareAndSet(null, new ImageSaveFacade());
    return instance.get();
  }

  /**
   * Private hidden constructor requiring clients to use get().
   */
  private ImageSaveFacade() {
    saveStrategies = new HashMap<>();

    ImageSaveStrategy imageWriterImageSaveStrategy = new ImageWriterImageSaveStrategy();
    for (String format : javax.imageio.ImageIO.getWriterFormatNames()) {
      saveStrategies.put(format.toLowerCase(), imageWriterImageSaveStrategy);
    }

    ImageSaveStrategy tgaImageSaveStrategy = new TgaImageSaveStrategy();
    saveStrategies.put("tga", tgaImageSaveStrategy);

    ImageSaveStrategy tiffImageSaveStrategy = new TiffImageSaveStrategy();
    saveStrategies.put("tiff", tiffImageSaveStrategy);

    defaultImageSaveStrategy = new TiffNakedFilenameImageSaveStrategy();
  }

  /**
   * Save a raw representation of pixel values to a file given that file's path.
   *
   * @param pixels The raw representation of the image to save.
   * @param pixelWidth Width of the image in pixels.
   * @param pixelheight Height of the image in pixels.
   * @param format Format corresponding to value in PConstants like PConstants.ARGB.
   * @param filename The path at which the file should be saved like "test/path/output.png".
   */
  public boolean save(int[] pixels, int pixelWidth, int pixelHeight, int format, String filename) {
    return save(
        pixels,
        pixelWidth,
        pixelHeight,
        format,
        filename,
        null
    );
  }

  /**
   * Save a raw representation of pixel values to a file given that file's path.
   *
   * @param pixels The raw representation of the image to save.
   * @param pixelWidth Width of the image in pixels.
   * @param pixelheight Height of the image in pixels.
   * @param format Format corresponding to value in PConstants like PConstants.ARGB.
   * @param filename The path at which the file should be saved like "test/path/output.png".
   * @param pApplet The applet through which files should be saved when using sketch relative paths.
   *    Can pass null if using absolute paths.
   */
  public boolean save(int[] pixels, int pixelWidth, int pixelHeight, int format, String filename,
      PApplet pApplet) {

    filename = preparePath(pApplet, filename);

    String extension = PathUtil.parseExtension(filename);

    ImageSaveStrategy imageSaveStrategy = saveStrategies.getOrDefault(
        extension,
        defaultImageSaveStrategy
    );

    try {
      return imageSaveStrategy.save(
          pixels,
          pixelWidth,
          pixelHeight,
          format,
          filename
      );
    } catch (IOException e) {
      System.err.println("Error while saving image.");
      e.printStackTrace();
      return false;
    } catch (SaveImageException e) {
      PGraphics.showException(e.getMessage());
      return false;
    }

  }

  /**
   * Ensure that the path is ready so that a file can be saved.
   *
   * @param pApplet The applet through which files should be saved when using sketch relative paths.
   *    Can pass null if using absolute paths.
   * @param filename The filename that will be written and for which a path needs to be prepared.
   * @return Completed path useable for writing like a file path that has been made relative to
   *    the sketch folder.
   */
  private String preparePath(PApplet pApplet, String filename) {
    if (pApplet != null) {
      return pApplet.savePath(filename);
    } else {
      File file = new File(filename);
      if (file.isAbsolute()) {
        // make sure that the intermediate folders have been created
        PathUtil.createPath(file);
        return filename;
      } else {
        String msg =
            "PImage.save() requires an absolute path. " +
                "Use createImage(), or pass savePath() to save().";

        throw new SaveImageException(msg);
      }
    }
  }

}
