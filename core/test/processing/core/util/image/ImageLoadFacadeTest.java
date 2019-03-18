/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2019 The Processing Foundation

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

import org.junit.Test;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.util.common.CommonInputTestUtil;

import java.io.IOException;


public class ImageLoadFacadeTest {

  @Test
  public void testLoadWithExtension() throws IOException {
    PApplet testApplet = CommonInputTestUtil.getFakePApplet();

    PImage results = ImageLoadFacade.get().loadFromFile(
        testApplet,
        "resource-test/test.png",
        "png"
    );

    CommonInputTestUtil.assertPImagePattern(results);
  }

  @Test
  public void testNonFallbackTga() throws IOException {
    PApplet testApplet = CommonInputTestUtil.getFakePApplet();

    PImage results = ImageLoadFacade.get().loadFromFile(
        testApplet,
        "resource-test/test.tga"
    );

    CommonInputTestUtil.assertPImageDotFlip(results);
  }

  @Test
  public void testNonFallbackTiff() throws IOException {
    PApplet testApplet = CommonInputTestUtil.getFakePApplet();

    PImage results = ImageLoadFacade.get().loadFromFile(
        testApplet,
        "resource-test/test.tiff"
    );

    CommonInputTestUtil.assertPImageDot(results);
  }

  @Test
  public void testLoadWithoutExtension() throws IOException {
    PApplet testApplet = CommonInputTestUtil.getFakePApplet();

    PImage results = ImageLoadFacade.get().loadFromFile(
        testApplet,
        "resource-test/test.png"
    );

    CommonInputTestUtil.assertPImagePattern(results);
  }

}
