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


/**
 * Interface for strategies to load different image formats and through different methods.
 */
public interface ImageLoadStrategy {

  /**
   * Load an image.
   *
   * @param pApplet The PApplet on whose behalf an image is being loaded.
   * @param path The path to the file like "subdirectory/file.png". Note that paths without
   *    extensions are supported and the extension is not read off this path but instead must be
   *    specified in the extension parameter.
   * @param extension The extension of the file to open.
   * @return The PImage loaded from the given path.
   */
  PImage load(PApplet pApplet, String path, String extension);

}
