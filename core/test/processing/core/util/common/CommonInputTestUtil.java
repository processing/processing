/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.core.util.common;

import org.mockito.Mockito;
import processing.core.PApplet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


/**
 * Utilities for testing input logic.
 */
public class CommonInputTestUtil {

  /**
   * Load a testing file.
   *
   * @param pathName The path to the testing file.
   * @return File to the testing file.
   */
  public static File getFile(String pathName) {
    File targetFile = new File(fixStringPath(pathName));

    if (!targetFile.isFile()) {
      targetFile = new File(fixStringPath("../core/" + pathName));
    }

    return targetFile;
  }

  /**
   * Convert a path to be usable within the host OS.
   *
   * @param pathName The path like "path/to/file".
   * @return The localized path like "path/to/file" or "path\to\file" depending on OS.
   */
  public static String fixStringPath(String pathName) {
    return pathName.replace("/", File.separator);
  }

  /**
   * Get the bytes from a file given that file's path.
   *
   * @param pathName The path to be loaded.
   * @return The path from which bytes should be read.
   */
  public static byte[] getBytes(String pathName) throws IOException {
    String targetPathName = getFile(pathName).getPath();
    return Files.readAllBytes(Paths.get(targetPathName));
  }

  /**
   * Create a fake PApplet for use in testing.
   *
   * @return Newly created PApplet.
   */
  public static PApplet getFakePApplet() {
    PApplet testApplet = Mockito.mock(PApplet.class);
    Mockito.when(testApplet.loadBytes(Mockito.anyString())).then(
        (invocation) -> getBytes(invocation.getArguments()[0].toString())
    );
    return testApplet;
  }

}
