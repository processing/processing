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

package processing.core.util.io;

import java.io.File;


/**
 * Convenience functions for managing file paths.
 */
public class PathUtil {
  private static final String UNKNOWN_FILE_EXTENSION = "unknown";

  /**
   * Determine the extension of a file given in a path.
   *
   * @param path The path like directory/subdirectory/image.png.
   * @return The extension of the file at path like "png" or "unknown" if could not be determined.
   */
  public static String parseExtension(String path) {
    String lower = path.toLowerCase();
    String extension;

    int dot = path.lastIndexOf('.');
    if (dot == -1) {
      extension = UNKNOWN_FILE_EXTENSION;  // no extension found

    } else {
      extension = lower.substring(dot + 1);

      // check for, and strip any parameters on the url, i.e.
      // filename.jpg?blah=blah&something=that
      int question = extension.indexOf('?');
      if (question != -1) {
        extension = extension.substring(0, question);
      }
    }

    return extension;
  }

  /**
   * Clean up irregularities like leading period or capitalization from an extension string.
   *
   * @param extension The extension to clean up like ".PNG".
   * @return The cleaned up extension like "png".
   */
  public static String cleanExtension(String extension) {
    extension = extension == null ? UNKNOWN_FILE_EXTENSION : extension.toLowerCase();

    if (extension.startsWith(".")) {
      extension = extension.substring(1);
    }

    return extension;
  }

  /**
   * Create the directories so that files can be created at a path.
   *
   * @param path The path like folder1/folder2/image.png. In this example, folder1 and folder2 would
   *    both be created if not already existing.
   */
  public static void createPath(String path) {
    createPath(new File(path));
  }

  /**
   * Create the directories so that files can be created at a path.
   *
   * @param file The file describing the path that should be created. A path of
   *    folder1/folder2/image.png would cause folder1 and folder2 to be created if not already
   *    existing.
   */
  public static void createPath(File file) {
    try {
      String parent = file.getParent();
      if (parent != null) {
        File unit = new File(parent);
        if (!unit.exists()) unit.mkdirs();
      }
    } catch (SecurityException se) {
      System.err.println("You don't have permissions to create " +
          file.getAbsolutePath());
    }
  }
}
