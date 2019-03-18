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

package processing.core.util.image.load;

import org.junit.Test;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.util.common.CommonInputTestUtil;

import java.io.IOException;


public class TiffImageLoadStrategyTest {

  @Test
  public void testLoad() throws IOException {
    PApplet testApplet = CommonInputTestUtil.getFakePApplet();

    TiffImageLoadStrategy strategy = new TiffImageLoadStrategy();
    PImage results = strategy.load(testApplet, "resource-test/test.tiff", "tiff");

    CommonInputTestUtil.assertPImageDot(results);
  }

}
