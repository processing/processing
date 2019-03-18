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

import org.junit.Assert;
import org.mockito.Mockito;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.util.image.ImageLoadFacade;

import java.awt.*;
import java.io.ByteArrayInputStream;
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
   * Get the full path to a test file.
   *
   * @param pathName The path to the file.
   * @return The full absolute path to the file.
   */
  public static String getFullPath(String pathName) {
    return getFile(fixStringPath(pathName)).getAbsolutePath();
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

    Mockito.when(testApplet.createInput(Mockito.anyString())).then(
        (invocation) -> new ByteArrayInputStream(
            getBytes(invocation.getArguments()[0].toString())
        )
    );

    Mockito.when(testApplet.dataPath(Mockito.anyString())).then(
        (invocation) -> getFullPath(invocation.getArguments()[0].toString())
    );

    Mockito.when(testApplet.sketchPath(Mockito.anyString())).then(
        (invocation) -> getFullPath(invocation.getArguments()[0].toString())
    );

    Mockito.when(testApplet.createImage(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).then(
        (invocation) -> {
          PImage image = new PImage(
              (int) invocation.getArguments()[0],
              (int) invocation.getArguments()[1],
              (int) invocation.getArguments()[2]
          );
          image.parent = testApplet;  // make save() work
          return image;
        }
    );

    Mockito.when(testApplet.sketchPath()).thenReturn("");

    return testApplet;
  }

  /**
   * Check that an pattern image has expected values.
   *
   * @param results The PImage to test.
   */
  public static void assertPImagePattern(PImage results) {
    Assert.assertEquals(7, results.pixelWidth);
    Assert.assertEquals(5, results.pixelHeight);
    Assert.assertEquals(7, results.width);
    Assert.assertEquals(5, results.height);

    int valueBlack = results.get(0, 0);
    int valueBlue = results.get(2, 2);

    Assert.assertEquals(Color.BLACK, new Color(valueBlack));
    Assert.assertEquals(new Color(52, 0, 254), new Color(valueBlue));
  }

  /**
   * Check that an image with a dot has expected values.
   *
   * @param results The PImage to test.
   */
  public static void assertPImageDot(PImage results) {
    Assert.assertEquals(5, results.pixelWidth);
    Assert.assertEquals(7, results.pixelHeight);
    Assert.assertEquals(5, results.width);
    Assert.assertEquals(7, results.height);

    Assert.assertEquals(new Color(26, 26, 26), new Color(results.get(2, 2)));
  }

  /**
   * Check that an image with a dot has expected values.
   *
   * @param results The PImage to test.
   */
  public static void assertPImageDotFlip(PImage results) {
    // Not sure if this "expected" but it is compatible with old behavior so maintaining
    Assert.assertEquals(5, results.pixelWidth);
    Assert.assertEquals(7, results.pixelHeight);
    Assert.assertEquals(5, results.width);
    Assert.assertEquals(7, results.height);

    Assert.assertEquals(new Color(26, 26, 26), new Color(results.get(2, 4)));
  }


  /**
   * Generate test pixels for writing.
   *
   * @return Get test pixels roughly matching above image pattern.
   */
  public static int[] generateTestPixels() {
    return generateTestPixels(7, 5);
  }

  /**
   * Generate test pixels for writing.
   *
   * @return Get test pixels roughly matching above image pattern.
   */
  public static int[] generateTestPixels(int width, int height) {
    int[] pixels = new int[width*height];
    pixels[7 * 0 + 0] = Color.BLACK.getRGB();
    pixels[7 * 0 + 2] = Color.BLACK.getRGB();
    pixels[7 * 0 + 4] = Color.BLACK.getRGB();
    pixels[7 * 2 + 0] = Color.BLACK.getRGB();
    pixels[7 * 2 + 2] = (new Color(52, 0, 254)).getRGB();
    pixels[7 * 2 + 4] = Color.BLACK.getRGB();
    pixels[7 * 4 + 0] = Color.BLACK.getRGB();
    pixels[7 * 4 + 2] = Color.BLACK.getRGB();
    pixels[7 * 4 + 4] = Color.BLACK.getRGB();

    return pixels;
  }

  /**
   * Check a saved file that should contain the image pattern above.
   *
   * @param path The path of the file to be checked.
   */
  public static void checkSavedFile(String path) {
    PApplet testApplet = CommonInputTestUtil.getFakePApplet();

    PImage results = ImageLoadFacade.get().loadFromFile(
        testApplet,
        path
    );

    assertPImagePattern(results);
  }

}
