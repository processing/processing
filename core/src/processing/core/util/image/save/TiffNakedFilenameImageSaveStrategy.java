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

import java.io.IOException;


/**
 * Strategy for saving a Tiff and appending ".tif" to the filename.
 */
public class TiffNakedFilenameImageSaveStrategy implements ImageSaveStrategy {

  private final ImageSaveStrategy tiffImageSaveStrategy;

  /**
   * Create a new Tiff save strategy where ".tif" is appended to the filename.
   */
  public TiffNakedFilenameImageSaveStrategy() {
    tiffImageSaveStrategy = new TiffImageSaveStrategy();
  }

  @Override
  public boolean save(int[] pixels, int pixelWidth, int pixelHeight, int format,
      String filename) throws IOException {

    filename += ".tif";
    return tiffImageSaveStrategy.save(
        pixels,
        pixelWidth,
        pixelHeight,
        format,
        filename
    );
  }
}
